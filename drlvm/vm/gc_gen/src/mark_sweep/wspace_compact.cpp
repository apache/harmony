/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#include "wspace_chunk.h"
#include "wspace_alloc.h"
#include "wspace_mark_sweep.h"
#include "wspace_verify.h"

#ifdef USE_32BITS_HASHCODE
#include "../common/hashcode.h"
#endif
 

#define PFC_SORT_NUM  8

void wspace_decide_compaction_need(Wspace *wspace)
{
  POINTER_SIZE_INT free_mem_size = free_mem_in_wspace(wspace, FALSE);
  float free_mem_ratio = (float)free_mem_size / wspace->committed_heap_size;

#ifdef USE_UNIQUE_MARK_SWEEP_GC
  if( gc_con_is_in_STW(wspace->gc) && (free_mem_ratio > WSPACE_COMPACT_RATIO) && (wspace->gc->cause != GC_CAUSE_RUNTIME_FORCE_GC)){
#else
  if(collect_is_major()){
#endif
    wspace->need_compact = wspace->move_object = TRUE;
  } else {
    wspace->need_compact = wspace->move_object = FALSE;
  }
}

static inline void sorted_chunk_bucket_add_entry(Chunk_Header **head, Chunk_Header **tail, Chunk_Header *chunk)
{
  chunk->prev = NULL; /* Field adj_prev is used as prev */
  
  if(!*head){
    assert(!*tail);
    chunk->next = NULL;
    *head = *tail = chunk;
    return;
  }
  
  assert(*tail);
  chunk->next = *head;
  (*head)->prev = chunk;
  *head = chunk;
}

/* One assumption: pfc_pool is not empty */
static Boolean pfc_pool_roughly_sort(Pool *pfc_pool, Chunk_Header **least_free_chunk, Chunk_Header **most_free_chunk)
{
  Chunk_Header *bucket_head[PFC_SORT_NUM];  /* Sorted chunk buckets' heads */
  Chunk_Header *bucket_tail[PFC_SORT_NUM];  /* Sorted chunk buckets' tails */
  unsigned int slot_num;
  unsigned int chunk_num = 0;
  unsigned int slot_alloc_num = 0;
  
  /* Init buckets' heads and tails */
  memset(bucket_head, 0, sizeof(Chunk_Header*) * PFC_SORT_NUM);
  memset(bucket_tail, 0, sizeof(Chunk_Header*) * PFC_SORT_NUM);
  
  /* Roughly sort chunks in pfc_pool */
  pool_iterator_init(pfc_pool);
  Chunk_Header *chunk = (Chunk_Header*)pool_iterator_next(pfc_pool);
  if(chunk) slot_num = chunk->slot_num;
  while(chunk){
    ++chunk_num;
    assert(chunk->alloc_num);
    slot_alloc_num += chunk->alloc_num;
    Chunk_Header *next_chunk = chunk->next;
    unsigned int bucket_index = (chunk->alloc_num*PFC_SORT_NUM-1) / slot_num;
    assert(bucket_index < PFC_SORT_NUM);
    sorted_chunk_bucket_add_entry(&bucket_head[bucket_index], &bucket_tail[bucket_index], chunk);
    chunk = next_chunk;
  }
  
  /* Empty the pfc pool because some chunks in this pool will be free after compaction */
  pool_empty(pfc_pool);
  
  /* If we can't get a free chunk after compaction, there is no need to compact.
   * This condition includes that the chunk num in pfc pool is equal to 1, in which case there is also no need to compact
   */
  if(slot_num*(chunk_num-1) <= slot_alloc_num){
    for(unsigned int i = 0; i < PFC_SORT_NUM; i++){
      Chunk_Header *chunk = bucket_head[i];
      while(chunk){
        Chunk_Header *next_chunk = chunk->next;
        pool_put_entry(pfc_pool, chunk);
        chunk = next_chunk;
      }
    }
    return FALSE;
  }
  
  /* Link the sorted chunk buckets into one single ordered bidirectional list */
  Chunk_Header *head = NULL;
  Chunk_Header *tail = NULL;
  for(unsigned int i = PFC_SORT_NUM; i--;){
    assert((head && tail) || (!head && !tail));
    assert((bucket_head[i] && bucket_tail[i]) || (!bucket_head[i] && !bucket_tail[i]));
    if(!bucket_head[i]) continue;
    if(!tail){
      head = bucket_head[i];
      tail = bucket_tail[i];
    } else {
      tail->next = bucket_head[i];
      bucket_head[i]->prev = tail;
      tail = bucket_tail[i];
    }
  }
  
  assert(head && tail);
  *least_free_chunk = head;
  *most_free_chunk = tail;
  
  return TRUE;
}

static inline Chunk_Header *get_least_free_chunk(Chunk_Header **least_free_chunk, Chunk_Header **most_free_chunk)
{
  if(!*least_free_chunk){
    assert(!*most_free_chunk);
    return NULL;
  }
  Chunk_Header *result = *least_free_chunk;
  *least_free_chunk = (*least_free_chunk)->next;
  if(*least_free_chunk)
    (*least_free_chunk)->prev = NULL;
  else
    *most_free_chunk = NULL;
  return result;
}
static inline Chunk_Header *get_most_free_chunk(Chunk_Header **least_free_chunk, Chunk_Header **most_free_chunk)
{
  if(!*most_free_chunk){
    assert(!*least_free_chunk);
    return NULL;
  }
  Chunk_Header *result = *most_free_chunk;
  *most_free_chunk = (*most_free_chunk)->prev;
  if(*most_free_chunk)
    (*most_free_chunk)->next = NULL;
  else
    *least_free_chunk = NULL;
  assert(!result->next);
  return result;
}

static inline void move_obj_between_chunks(Wspace *wspace, Chunk_Header **dest_ptr, Chunk_Header *src)
{
  Chunk_Header *dest = *dest_ptr;
  assert(dest->slot_size == src->slot_size);
  
  unsigned int slot_size = dest->slot_size;
  unsigned int alloc_num = src->alloc_num;
  assert(alloc_num);
  
#ifdef USE_32BITS_HASHCODE
  Hashcode_Buf*  old_hashcode_buf = src->hashcode_buf;
  Hashcode_Buf* new_hashcode_buf = dest->hashcode_buf;
#endif

  while(alloc_num && dest){
    Partial_Reveal_Object *p_obj = next_alloc_slot_in_chunk(src);
    Partial_Reveal_Object *target = (Partial_Reveal_Object *)alloc_in_chunk(dest);

    if(dest->slot_index == MAX_SLOT_INDEX){
      dest->status = CHUNK_USED | CHUNK_NORMAL;      
      wspace_reg_used_chunk(wspace,dest);
      dest = NULL;
    }
    
    assert(p_obj && target);
    memcpy(target, p_obj, slot_size);

#ifdef USE_32BITS_HASHCODE
  if(hashcode_is_set(p_obj)){
    int hashcode;
    if(new_hashcode_buf == NULL) {
      new_hashcode_buf = hashcode_buf_create();
      hashcode_buf_init(new_hashcode_buf);
      dest->hashcode_buf = new_hashcode_buf;
    }
    if(hashcode_is_buffered(p_obj)){
      /*already buffered objects;*/
      hashcode = hashcode_buf_lookup(p_obj, old_hashcode_buf);
      hashcode_buf_update(target, hashcode, new_hashcode_buf);
    }else{
      /*objects need buffering.*/
      hashcode = hashcode_gen(p_obj);
      hashcode_buf_update(target, hashcode, new_hashcode_buf);
      Obj_Info_Type oi = get_obj_info_raw(target);
      set_obj_info(target, oi | HASHCODE_BUFFERED_BIT);
    }
  }
#endif


#ifdef SSPACE_VERIFY
    wspace_modify_mark_in_compact(target, p_obj, slot_size);
#endif
    obj_set_fw_in_oi(p_obj, target);
    --alloc_num;
  }

#ifdef USE_32BITS_HASHCODE
  if(alloc_num == 0) {
    if(old_hashcode_buf) hashcode_buf_destory(old_hashcode_buf);
    src->hashcode_buf = NULL;
  }
#endif

  
  /* dest might be set to NULL, so we use *dest_ptr here */
  assert((*dest_ptr)->alloc_num <= (*dest_ptr)->slot_num);
  src->alloc_num = alloc_num;
  if(!dest){
    assert((*dest_ptr)->alloc_num == (*dest_ptr)->slot_num);
    *dest_ptr = NULL;
    clear_free_slot_in_table(src->table, src->slot_index);
  }
}

void wspace_compact(Collector *collector, Wspace *wspace)
{
  Chunk_Header *least_free_chunk, *most_free_chunk;
  Pool *pfc_pool = wspace_grab_next_pfc_pool(wspace);
  
  for(; pfc_pool; pfc_pool = wspace_grab_next_pfc_pool(wspace)){
    if(pool_is_empty(pfc_pool)) continue;
    Boolean pfc_pool_need_compact = pfc_pool_roughly_sort(pfc_pool, &least_free_chunk, &most_free_chunk);
    if(!pfc_pool_need_compact) continue;
    
    Chunk_Header *dest = get_least_free_chunk(&least_free_chunk, &most_free_chunk);
    Chunk_Header *src = get_most_free_chunk(&least_free_chunk, &most_free_chunk);
    Boolean src_is_new = TRUE;
    while(dest && src){
      if(src_is_new)
        src->slot_index = 0;
      //chunk_depad_last_index_word(src);
      move_obj_between_chunks(wspace, &dest, src);
      if(!dest)
        dest = get_least_free_chunk(&least_free_chunk, &most_free_chunk);
      if(!src->alloc_num){
        collector_add_free_chunk(collector, (Free_Chunk*)src);
        src = get_most_free_chunk(&least_free_chunk, &most_free_chunk);
        src_is_new = TRUE;
      } else {
        src_is_new = FALSE;
      }
    }
    
    /* Rebuild the pfc_pool */
    if(dest)
      wspace_put_pfc(wspace, dest);
    if(src){
      //chunk_pad_last_index_word(src, cur_alloc_mask);
      pfc_reset_slot_index(src);
      wspace_put_pfc(wspace, src);
    }
  }
}






