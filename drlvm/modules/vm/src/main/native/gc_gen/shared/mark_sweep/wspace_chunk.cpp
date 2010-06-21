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
#include "gc_ms.h"
#include "wspace.h"
#include "wspace_chunk.h"
#include "wspace_verify.h"

/* PFC stands for partially free chunk */
static Size_Segment *size_segments[SIZE_SEGMENT_NUM];
static Pool **pfc_pools[SIZE_SEGMENT_NUM];
static Pool **pfc_pools_backup[SIZE_SEGMENT_NUM];
static Boolean  *pfc_steal_flags[SIZE_SEGMENT_NUM];

static Free_Chunk_List  aligned_free_chunk_lists[NUM_ALIGNED_FREE_CHUNK_BUCKET];
static Free_Chunk_List  unaligned_free_chunk_lists[NUM_UNALIGNED_FREE_CHUNK_BUCKET];
static Free_Chunk_List  hyper_free_chunk_list;

Free_Chunk_List *get_hyper_free_chunk_list() {
	return &hyper_free_chunk_list;
}

static void init_size_segment(Size_Segment *seg, unsigned int size_min, unsigned int size_max, unsigned int gran_shift_bits, Boolean local_alloc)
{
  seg->size_min = size_min;
  seg->size_max = size_max;
  seg->local_alloc = local_alloc;
  seg->chunk_num = (seg->size_max - seg->size_min) >> gran_shift_bits;
  seg->gran_shift_bits = gran_shift_bits;
  seg->granularity = (POINTER_SIZE_INT)(1 << gran_shift_bits);
  seg->gran_low_mask = seg->granularity - 1;
  seg->gran_high_mask = ~seg->gran_low_mask;
}

void wspace_init_chunks(Wspace *wspace)
{
  unsigned int i, j;
  
  /* Init size segments */
  Size_Segment *size_seg_start = (Size_Segment*)STD_MALLOC(sizeof(Size_Segment) * SIZE_SEGMENT_NUM);
  for(i = SIZE_SEGMENT_NUM; i--;){
    size_segments[i] = size_seg_start + i;
    size_segments[i]->seg_index = i;
  }
  init_size_segment(size_segments[0], 0, MEDIUM_OBJ_THRESHOLD, SMALL_GRANULARITY_BITS, SMALL_IS_LOCAL_ALLOC);
  init_size_segment(size_segments[1], MEDIUM_OBJ_THRESHOLD, LARGE_OBJ_THRESHOLD, MEDIUM_GRANULARITY_BITS, MEDIUM_IS_LOCAL_ALLOC);
  init_size_segment(size_segments[2], LARGE_OBJ_THRESHOLD, SUPER_OBJ_THRESHOLD, LARGE_GRANULARITY_BITS, LARGE_IS_LOCAL_ALLOC);
  
  /* Init partially free chunk pools */
  for(i = SIZE_SEGMENT_NUM; i--;){
    pfc_pools[i] = (Pool**)STD_MALLOC(sizeof(Pool*) * size_segments[i]->chunk_num);
    pfc_pools_backup[i] = (Pool**)STD_MALLOC(sizeof(Pool*) * size_segments[i]->chunk_num);
    pfc_steal_flags[i] = (Boolean*)STD_MALLOC(sizeof(Boolean) * size_segments[i]->chunk_num);
    for(j=size_segments[i]->chunk_num; j--;){
      pfc_pools[i][j] = sync_pool_create();
      pfc_pools_backup[i][j] = sync_pool_create();
      pfc_steal_flags[i][j] = FALSE;
    }
  }
  
  /* Init aligned free chunk lists */
  for(i = NUM_ALIGNED_FREE_CHUNK_BUCKET; i--;)
    free_chunk_list_init(&aligned_free_chunk_lists[i]);
  
  /* Init nonaligned free chunk lists */
  for(i = NUM_UNALIGNED_FREE_CHUNK_BUCKET; i--;)
    free_chunk_list_init(&unaligned_free_chunk_lists[i]);
  
  /* Init super free chunk lists */
  free_chunk_list_init(&hyper_free_chunk_list);
  
  wspace->size_segments = size_segments;
  wspace->pfc_pools = pfc_pools;
  wspace->pfc_pools_backup = pfc_pools_backup;
  wspace->used_chunk_pool = sync_pool_create();
  wspace->unreusable_normal_chunk_pool = sync_pool_create();
  wspace->live_abnormal_chunk_pool = sync_pool_create();
  wspace->aligned_free_chunk_lists = aligned_free_chunk_lists;
  wspace->unaligned_free_chunk_lists = unaligned_free_chunk_lists;
  wspace->hyper_free_chunk_list = &hyper_free_chunk_list;
  
  /* Init the first free chunk: from heap start to heap end */
  Free_Chunk *free_chunk = (Free_Chunk*)wspace->heap_start;
  free_chunk->adj_next = (Chunk_Header_Basic*)wspace->heap_end;
  POINTER_SIZE_INT chunk_size = wspace->reserved_heap_size;
  assert(chunk_size > CHUNK_GRANULARITY && !(chunk_size % CHUNK_GRANULARITY));
  wspace_put_free_chunk(wspace, free_chunk);
}

static void pfc_pool_set_steal_flag(Pool *pool, unsigned int steal_threshold, Boolean &steal_flag)
{
  Chunk_Header *chunk = (Chunk_Header*)pool_get_entry(pool);
  while(chunk){
    steal_threshold--;
    if(!steal_threshold)
      break;
    chunk = chunk->next;
  }
  steal_flag = steal_threshold ? FALSE : TRUE;
}

void wspace_clear_chunk_list(Wspace* wspace)
{
  unsigned int i, j;
  GC* gc = wspace->gc;
  unsigned int collector_num = gc->num_collectors;
  unsigned int steal_threshold = collector_num << PFC_STEAL_THRESHOLD;
  
  Pool*** pfc_pools = wspace->pfc_pools;
  for(i = SIZE_SEGMENT_NUM; i--;){
    for(j = size_segments[i]->chunk_num; j--;){
      Pool *pool = pfc_pools[i][j];
      pfc_pool_set_steal_flag(pool, steal_threshold, pfc_steal_flags[i][j]);
      pool_empty(pool);
    }
  }
  
  Pool*** pfc_pools_backup = wspace->pfc_pools_backup;
  for(i = SIZE_SEGMENT_NUM; i--;){
    for(j = size_segments[i]->chunk_num; j--;){
      Pool *pool = pfc_pools_backup[i][j];
      pfc_pool_set_steal_flag(pool, steal_threshold, pfc_steal_flags[i][j]);
      pool_empty(pool);
    }
  }
  
  for(i=NUM_ALIGNED_FREE_CHUNK_BUCKET; i--;)
    free_chunk_list_clear(&aligned_free_chunk_lists[i]);
  
  for(i=NUM_UNALIGNED_FREE_CHUNK_BUCKET; i--;)
    free_chunk_list_clear(&unaligned_free_chunk_lists[i]);
  
  free_chunk_list_clear(&hyper_free_chunk_list);
}

/* Simply put the free chunk to the according list
 * Don't merge continuous free chunks
 * The merging job is executed in merging phase
 */
static void list_put_free_chunk(Free_Chunk_List *list, Free_Chunk *chunk)
{
  chunk->status = CHUNK_FREE;
  chunk->prev = NULL;

  lock(list->lock);
  chunk->next = list->head;
  if(list->head)
    list->head->prev = chunk;
  list->head = chunk;
  if(!list->tail)
    list->tail = chunk;
  assert(list->chunk_num < ~((unsigned int)0));
  ++list->chunk_num;
  unlock(list->lock);
}

static void list_put_free_chunk_to_head(Free_Chunk_List *list, Free_Chunk *chunk)
{
  
  chunk->status = CHUNK_FREE;
  chunk->prev = NULL;
  chunk->next = NULL;

  lock(list->lock);
  chunk->next = list->head;
  if(list->head)
    list->head->prev = chunk;
  list->head = chunk;
  if(!list->tail)
    list->tail = chunk;
  assert(list->chunk_num < ~((unsigned int)0));
  ++list->chunk_num;
  unlock(list->lock);
}

static void list_put_free_chunk_to_tail(Free_Chunk_List *list, Free_Chunk *chunk)
{
  //modified for concurrent sweep.
  //chunk->status = CHUNK_FREE;
  chunk->prev = NULL;
  chunk->next = NULL;

  lock(list->lock);
  chunk->prev = list->tail;
  if(list->head)
    list->tail->next = chunk;
  list->tail = chunk;
  if(!list->head)
    list->head = chunk;
  assert(list->chunk_num < ~((unsigned int)0));
  ++list->chunk_num;
  unlock(list->lock);
}



/* The difference between this and the above normal one is this func needn't hold the list lock.
 * This is for the calling in partitioning chunk functions.
 * Please refer to the comments of wspace_get_hyper_free_chunk().
 */
static void list_put_hyper_free_chunk(Free_Chunk_List *list, Free_Chunk *chunk)
{
  chunk->status = CHUNK_FREE;
  chunk->prev = NULL;

  /* lock(list->lock);
   * the list lock must have been held like in getting a free chunk and partitioning it
   * or needn't be held like in wspace initialization and the merging phase
   */
  chunk->next = list->head;
  if(list->head)
    list->head->prev = chunk;
  list->head = chunk;
  if(!list->tail)
    list->tail = chunk;
  assert(list->chunk_num < ~((unsigned int)0));
  ++list->chunk_num;
  //unlock(list->lock);
}

static void list_put_hyper_free_chunk_to_head(Free_Chunk_List *list, Free_Chunk *chunk)
{
  chunk->status = CHUNK_FREE;
  chunk->prev = NULL;

  chunk->next = list->head;
  if(list->head)
    list->head->prev = chunk;
  list->head = chunk;
  if(!list->tail)
    list->tail = chunk;
  assert(list->chunk_num < ~((unsigned int)0));
  ++list->chunk_num;
}

static void list_put_hyper_free_chunk_to_tail(Free_Chunk_List *list, Free_Chunk *chunk)
{
  //modified for concurrent sweep.
  //chunk->status = CHUNK_FREE;
  chunk->next = NULL;

  chunk->prev = list->tail;
  if(list->tail)
    list->tail->next = chunk;
  list->tail = chunk;
  if(!list->head)
    list->head = chunk;
  assert(list->chunk_num < ~((unsigned int)0));
  ++list->chunk_num;
}



static Free_Chunk *free_list_get_head(Free_Chunk_List *list)
{
  lock(list->lock);
  Free_Chunk *chunk = list->head;
  if(chunk){
    list->head = chunk->next;
    if(list->head)
      list->head->prev = NULL;
    else
      list->tail = NULL;
    assert(list->chunk_num);
    --list->chunk_num;
    //assert(chunk->status == CHUNK_FREE);
    assert(chunk->status & CHUNK_FREE);
  }
  unlock(list->lock);
  return chunk;
}

void wspace_put_free_chunk(Wspace *wspace, Free_Chunk *chunk)
{
  POINTER_SIZE_INT chunk_size = CHUNK_SIZE(chunk);
  assert(!(chunk_size % CHUNK_GRANULARITY));
  
  if(chunk_size > HYPER_OBJ_THRESHOLD)
    list_put_hyper_free_chunk(wspace->hyper_free_chunk_list, chunk);
  else if(!((POINTER_SIZE_INT)chunk & NORMAL_CHUNK_LOW_MASK) && !(chunk_size & NORMAL_CHUNK_LOW_MASK))
    list_put_free_chunk(&wspace->aligned_free_chunk_lists[ALIGNED_CHUNK_SIZE_TO_INDEX(chunk_size)], chunk);
  else
    list_put_free_chunk(&wspace->unaligned_free_chunk_lists[UNALIGNED_CHUNK_SIZE_TO_INDEX(chunk_size)], chunk);
}

void wspace_put_free_chunk_to_head(Wspace *wspace, Free_Chunk *chunk)
{
  POINTER_SIZE_INT chunk_size = CHUNK_SIZE(chunk);
  assert(!(chunk_size % CHUNK_GRANULARITY));
  
  if(chunk_size > HYPER_OBJ_THRESHOLD){
    lock(wspace->hyper_free_chunk_list->lock);
    list_put_hyper_free_chunk_to_head(wspace->hyper_free_chunk_list, chunk);
    unlock(wspace->hyper_free_chunk_list->lock);
  }else if(!((POINTER_SIZE_INT)chunk & NORMAL_CHUNK_LOW_MASK) && !(chunk_size & NORMAL_CHUNK_LOW_MASK))
    list_put_free_chunk_to_head(&wspace->aligned_free_chunk_lists[ALIGNED_CHUNK_SIZE_TO_INDEX(chunk_size)], chunk);
  else
    list_put_free_chunk_to_head(&wspace->unaligned_free_chunk_lists[UNALIGNED_CHUNK_SIZE_TO_INDEX(chunk_size)], chunk);

}

void wspace_put_free_chunk_to_tail(Wspace *wspace, Free_Chunk *chunk)
{
  POINTER_SIZE_INT chunk_size = CHUNK_SIZE(chunk);
  assert(!(chunk_size % CHUNK_GRANULARITY));

  Free_Chunk_List *free_list = NULL;
  if(chunk_size > HYPER_OBJ_THRESHOLD){
    free_list = wspace->hyper_free_chunk_list;
    lock(free_list->lock);
    list_put_hyper_free_chunk_to_tail(wspace->hyper_free_chunk_list, chunk);
    unlock(free_list->lock);
  }else if(!((POINTER_SIZE_INT)chunk & NORMAL_CHUNK_LOW_MASK) && !(chunk_size & NORMAL_CHUNK_LOW_MASK)) {
    free_list = &wspace->aligned_free_chunk_lists[ALIGNED_CHUNK_SIZE_TO_INDEX(chunk_size)];
    list_put_free_chunk_to_tail(free_list, chunk);
  } else {
    free_list = &wspace->unaligned_free_chunk_lists[UNALIGNED_CHUNK_SIZE_TO_INDEX(chunk_size)];
    list_put_free_chunk_to_tail(free_list, chunk);
  }

}

static inline Free_Chunk *partition_normal_free_chunk(Wspace *wspace, Free_Chunk *chunk)
{
  assert(CHUNK_SIZE(chunk) > NORMAL_CHUNK_SIZE_BYTES);
  
  Chunk_Header_Basic *adj_next = chunk->adj_next;
  Free_Chunk *normal_chunk = (Free_Chunk*)(((POINTER_SIZE_INT)chunk + NORMAL_CHUNK_SIZE_BYTES-1) & NORMAL_CHUNK_HIGH_MASK);
  
  if(chunk != normal_chunk){
    assert(chunk < normal_chunk);
    chunk->adj_next = (Chunk_Header_Basic*)normal_chunk;
    wspace_put_free_chunk(wspace, chunk);
  }
  normal_chunk->adj_next = (Chunk_Header_Basic*)((POINTER_SIZE_INT)normal_chunk + NORMAL_CHUNK_SIZE_BYTES);
  if(normal_chunk->adj_next != adj_next){
    assert(normal_chunk->adj_next < adj_next);
    Free_Chunk *back_chunk = (Free_Chunk*)normal_chunk->adj_next;
    back_chunk->adj_next = adj_next;
    wspace_put_free_chunk(wspace, back_chunk);
  }
  
  normal_chunk->status = CHUNK_FREE;
  return normal_chunk;
}

/* Partition the free chunk to two free chunks:
 * the first one's size is chunk_size
 * the second will be inserted into free chunk list according to its size
 */
static inline Free_Chunk *partition_abnormal_free_chunk(Wspace *wspace,Free_Chunk *chunk, unsigned int chunk_size)
{
  assert(CHUNK_SIZE(chunk) > chunk_size);
  
  Free_Chunk *new_chunk = (Free_Chunk*)((POINTER_SIZE_INT)chunk->adj_next - chunk_size);
  assert(chunk < new_chunk);
  
  new_chunk->adj_next = chunk->adj_next;
  chunk->adj_next = (Chunk_Header_Basic*)new_chunk;
  wspace_put_free_chunk(wspace, chunk);
  new_chunk->status = CHUNK_FREE;
  return new_chunk;
}

Free_Chunk *wspace_get_normal_free_chunk(Wspace *wspace)
{
  Free_Chunk_List *aligned_lists = wspace->aligned_free_chunk_lists;
  Free_Chunk_List *unaligned_lists = wspace->unaligned_free_chunk_lists;
  Free_Chunk_List *list = NULL;
  Free_Chunk *chunk = NULL;
  
  /* Search in aligned chunk lists first */
  unsigned int index = 0;
  while(index < NUM_ALIGNED_FREE_CHUNK_BUCKET){
    list = &aligned_lists[index];
    if(list->head)
      chunk = free_list_get_head(list);
    if(chunk){
      if(CHUNK_SIZE(chunk) > NORMAL_CHUNK_SIZE_BYTES)
        chunk = partition_normal_free_chunk(wspace, chunk);
      //zeroing_free_chunk(chunk);
      return chunk;
    }
    index++;
  }
  assert(!chunk);
  
  /* Search in unaligned chunk lists with larger chunk.
     (NORMAL_CHUNK_SIZE_BYTES + (NORMAL_CHUNK_SIZE_BYTES-CHUNK_GRANULARITY))
     is the smallest size which can guarantee the chunk includes a normal chunk.
  */
  index = UNALIGNED_CHUNK_SIZE_TO_INDEX((NORMAL_CHUNK_SIZE_BYTES<<1) - CHUNK_GRANULARITY);
  while(index < NUM_UNALIGNED_FREE_CHUNK_BUCKET){
    list = &unaligned_lists[index];
    if(list->head)
      chunk = free_list_get_head(list);
    if(chunk){
      chunk = partition_normal_free_chunk(wspace, chunk);
      assert(!((POINTER_SIZE_INT)chunk & NORMAL_CHUNK_LOW_MASK));
      //zeroing_free_chunk(chunk);
      return chunk;
    }
    index++;
  }
  assert(!chunk);
  
  /* search in the hyper free chunk list */

  chunk = wspace_get_hyper_free_chunk(wspace, NORMAL_CHUNK_SIZE_BYTES, TRUE);
  assert(!((POINTER_SIZE_INT)chunk & NORMAL_CHUNK_LOW_MASK));
  /*if(chunk == NULL )
  INFO2("gc.wspace", "return from hyper free chunk list");*/
  return chunk;
}

Free_Chunk *wspace_get_abnormal_free_chunk(Wspace *wspace, unsigned int chunk_size)
{
  assert(chunk_size > CHUNK_GRANULARITY);
  assert(!(chunk_size % CHUNK_GRANULARITY));
  assert(chunk_size <= HYPER_OBJ_THRESHOLD);
  
  Free_Chunk_List *unaligned_lists = wspace->unaligned_free_chunk_lists;
  Free_Chunk_List *list = NULL;
  Free_Chunk *chunk = NULL;
  unsigned int index = 0;
  
  /* Search in the list with chunk size of multiple chunk_size */
  unsigned int search_size = chunk_size;
  while(search_size <= HYPER_OBJ_THRESHOLD){
    index = UNALIGNED_CHUNK_SIZE_TO_INDEX(search_size);
    list = &unaligned_lists[index];
    if(list->head)
      chunk = free_list_get_head(list);
    if(chunk){
      if(search_size > chunk_size)
        chunk = partition_abnormal_free_chunk(wspace, chunk, chunk_size);
      zeroing_free_chunk(chunk);
      return chunk;
    }
    search_size += chunk_size;
  }
  assert(!chunk);
  
  /* search in the hyper free chunk list */
  chunk = wspace_get_hyper_free_chunk(wspace, chunk_size, FALSE);
  if(chunk) return chunk;
  
  /* Search again in abnormal chunk lists */
  index = UNALIGNED_CHUNK_SIZE_TO_INDEX(chunk_size);
  while(index < NUM_UNALIGNED_FREE_CHUNK_BUCKET){
    list = &unaligned_lists[index];
    if(list->head)
      chunk = free_list_get_head(list);
    if(chunk){
      if(index > UNALIGNED_CHUNK_SIZE_TO_INDEX(chunk_size))
        chunk = partition_abnormal_free_chunk(wspace, chunk, chunk_size);
      zeroing_free_chunk(chunk);
      return chunk;
    }
    ++index;
  }
  
  return chunk;
}

Free_Chunk *wspace_get_hyper_free_chunk(Wspace *wspace, unsigned int chunk_size, Boolean is_normal_chunk)
{
  assert(chunk_size >= CHUNK_GRANULARITY);
  assert(!(chunk_size % CHUNK_GRANULARITY));
  
  Free_Chunk_List *list = wspace->hyper_free_chunk_list;
  lock(list->lock);
  
  Free_Chunk *prev_chunk = NULL;
  Free_Chunk *chunk = list->head;
  /*
  if( chunk == NULL  )
  INFO2("gc.wspace", "NO free hyper chunk now!!!" );
  */
  while(chunk){
    
    if(CHUNK_SIZE(chunk) >= chunk_size){
      Free_Chunk *next_chunk = chunk->next;
      if(prev_chunk)
        prev_chunk->next = next_chunk;
      else
        list->head = next_chunk;
      if(next_chunk)
        next_chunk->prev = prev_chunk;
      else
        list->tail = prev_chunk;
      break;
    } else {
      //INFO2("gc.wspace", "check chunk with SIZE "<<CHUNK_SIZE(chunk) << " ,not enough" );
    }
    prev_chunk = chunk;
    chunk = chunk->next;
  }
  
  /* unlock(list->lock);
   * We move this unlock to the end of this func for the following reason.
   * A case might occur that two allocator are asking for a hyper chunk at the same time,
   * and there is only one chunk in the list and it can satify the requirements of both of them.
   * If allocator 1 gets the list lock first, it will get the unique chunk and releases the lock here.
   * And then allocator 2 holds the list lock after allocator 1 releases it,
   * it will found there is no hyper chunk in the list and return NULL.
   * In fact the unique hyper chunk is large enough.
   * If allocator 1 chops down one piece and put back the rest into the list, allocator 2 will be satisfied.
   * So we will get a wrong info here if we release the lock here, which makes us invoke GC much earlier than needed.
   */
  
  if(chunk){
    if(is_normal_chunk)
      chunk = partition_normal_free_chunk(wspace, chunk);
    else if(CHUNK_SIZE(chunk) > chunk_size)
      chunk = partition_abnormal_free_chunk(wspace, chunk, chunk_size);
    if(!is_normal_chunk)
      zeroing_free_chunk(chunk);
  }
  
  unlock(list->lock);
  
  return chunk;
}

void wspace_collect_free_chunks_to_list(Wspace *wspace, Free_Chunk_List *list)
{
  unsigned int i;
  
  for(i = NUM_ALIGNED_FREE_CHUNK_BUCKET; i--;)
    move_free_chunks_between_lists(list, &wspace->aligned_free_chunk_lists[i]);
  
  for(i = NUM_UNALIGNED_FREE_CHUNK_BUCKET; i--;)
    move_free_chunks_between_lists(list, &wspace->unaligned_free_chunk_lists[i]);
  
  move_free_chunks_between_lists(list, wspace->hyper_free_chunk_list);
  
  Free_Chunk *chunk = list->head;
  while(chunk){
    chunk->status = CHUNK_FREE | CHUNK_TO_MERGE;
    chunk = chunk->next;
  }
}


typedef struct PFC_Pool_Iterator {
  volatile unsigned int seg_index;
  volatile unsigned int chunk_index;
  SpinLock lock;
} PFC_Pool_Iterator;

static PFC_Pool_Iterator pfc_pool_iterator;

void wspace_init_pfc_pool_iterator(Wspace *wspace)
{
  assert(pfc_pool_iterator.lock == FREE_LOCK);
  pfc_pool_iterator.seg_index = 0;
  pfc_pool_iterator.chunk_index = 0;
}

void wspace_exchange_pfc_pool(Wspace *wspace)
{
  Pool*** empty_pfc_pools = wspace->pfc_pools;
  wspace->pfc_pools = wspace->pfc_pools_backup;
  wspace->pfc_pools_backup = empty_pfc_pools;
}

Pool *wspace_grab_next_pfc_pool(Wspace *wspace)
{
  Pool*** pfc_pools = wspace->pfc_pools;
  Pool *pfc_pool = NULL;
  
  lock(pfc_pool_iterator.lock);
  for(; pfc_pool_iterator.seg_index < SIZE_SEGMENT_NUM; ++pfc_pool_iterator.seg_index){
    for(; pfc_pool_iterator.chunk_index < size_segments[pfc_pool_iterator.seg_index]->chunk_num; ++pfc_pool_iterator.chunk_index){
      pfc_pool = pfc_pools[pfc_pool_iterator.seg_index][pfc_pool_iterator.chunk_index];
      ++pfc_pool_iterator.chunk_index;
      unlock(pfc_pool_iterator.lock);
      return pfc_pool;
    }
    pfc_pool_iterator.chunk_index = 0;
  }
  unlock(pfc_pool_iterator.lock);
  
  return NULL;
}

#define min_value(x, y) (((x) < (y)) ? (x) : (y))

Chunk_Header *wspace_steal_pfc(Wspace *wspace, unsigned int seg_index, unsigned int index)
{
  Size_Segment *size_seg = wspace->size_segments[seg_index];
  Chunk_Header *pfc = NULL;
  unsigned int max_index = min_value(index + PFC_STEAL_NUM + 1, size_seg->chunk_num);
  ++index;
  for(; index < max_index; ++index){
    if(!pfc_steal_flags[seg_index][index]) continue;
    pfc = wspace_get_pfc(wspace, seg_index, index);
    if(pfc) return pfc;
  }
  return NULL;
}

static POINTER_SIZE_INT free_mem_in_used_chunk_list(Wspace *wspace, Boolean show_chunk_info)
{
  POINTER_SIZE_INT used_chunk_size = 0;
  POINTER_SIZE_INT used_chunk_num  = 0;
  POINTER_SIZE_INT free_mem_size   = 0;
  Pool* used_chunk_pool = wspace->used_chunk_pool;
  if(used_chunk_pool) {
    pool_iterator_init(used_chunk_pool);
    Chunk_Header* used_chunk = (Chunk_Header*)pool_iterator_next(used_chunk_pool);
    while(used_chunk != NULL){
      used_chunk_num ++;
      used_chunk_size += CHUNK_SIZE(used_chunk);
      if(used_chunk->status & CHUNK_NORMAL) {
        free_mem_size += (used_chunk->slot_num - used_chunk->alloc_num) * used_chunk->slot_size;
      }
      used_chunk = (Chunk_Header*)pool_iterator_next(used_chunk_pool);
    }
  }
#ifdef SSPACE_CHUNK_INFO
      if(show_chunk_info)
        printf("Used chunk num: %d\tTotal Size: %d\tFragmentation Ratio: %f\n", used_chunk_num, used_chunk_size, (float)free_mem_size/used_chunk_size);
#endif
  return free_mem_size;
}

static POINTER_SIZE_INT free_mem_in_pfc_pools(Wspace *wspace, Boolean show_chunk_info)
{
  Size_Segment **size_segs = wspace->size_segments;
  Pool ***pfc_pools = wspace->pfc_pools;
  POINTER_SIZE_INT free_mem_size = 0;
  POINTER_SIZE_INT total_pfc_size = 0;
  
  for(unsigned int i = 0; i < SIZE_SEGMENT_NUM; ++i){
    for(unsigned int j = 0; j < size_segs[i]->chunk_num; ++j){
      Pool *pfc_pool = pfc_pools[i][j];
      if(pool_is_empty(pfc_pool))
        continue;
      pool_iterator_init(pfc_pool);
      Chunk_Header *chunk = (Chunk_Header*)pool_iterator_next(pfc_pool);
      assert(chunk);
      unsigned int slot_num = chunk->slot_num;
      unsigned int chunk_num = 0;
      unsigned int alloc_num = 0;
      while(chunk){
        assert(chunk->slot_num == slot_num);
        ++chunk_num;
        alloc_num += chunk->alloc_num;
        chunk = (Chunk_Header*)pool_iterator_next(pfc_pool);
      }
      unsigned int total_slot_num = slot_num * chunk_num;
      assert(alloc_num < total_slot_num);
#ifdef SSPACE_CHUNK_INFO
      if(show_chunk_info)
        printf("Size: %x\tchunk num: %d\tLive Ratio: %f\n", NORMAL_INDEX_TO_SIZE(j, size_segs[i]), chunk_num, (float)alloc_num/total_slot_num);
#endif
      free_mem_size += NORMAL_INDEX_TO_SIZE(j, size_segs[i]) * (total_slot_num-alloc_num);
      total_pfc_size += NORMAL_INDEX_TO_SIZE(j, size_segs[i]) * total_slot_num;
      assert(free_mem_size < wspace->committed_heap_size);
    }
  }

#ifdef SSPACE_CHUNK_INFO
      if(show_chunk_info)
        printf("Total PFC pool Size: %d\tFragmentation Ratio: %f\n", total_pfc_size,  (float)free_mem_size/total_pfc_size);
#endif
  
  return free_mem_size;
}

static POINTER_SIZE_INT free_mem_in_free_lists(Wspace *wspace, Free_Chunk_List *lists, unsigned int list_num, Boolean show_chunk_info)
{
  POINTER_SIZE_INT free_mem_size = 0;
  
  for(unsigned int index = 0; index < list_num; ++index){
    Free_Chunk *chunk = lists[index].head;
    if(!chunk) continue;
    POINTER_SIZE_INT chunk_size = CHUNK_SIZE(chunk);
    assert(chunk_size <= HYPER_OBJ_THRESHOLD);
    unsigned int chunk_num = 0;
    while(chunk){
      assert(CHUNK_SIZE(chunk) == chunk_size);
      ++chunk_num;
      chunk = chunk->next;
    }
    free_mem_size += chunk_size * chunk_num;
    assert(free_mem_size < wspace->committed_heap_size);
#ifdef SSPACE_CHUNK_INFO
    if(show_chunk_info)
      printf("Free Size: %x\tnum: %d\n", chunk_size, chunk_num);
#endif
  }

#ifdef SSPACE_CHUNK_INFO
    if(show_chunk_info)
      printf("Total Size in FreeList: %d\n", free_mem_size);
#endif
  
  return free_mem_size;
}

static POINTER_SIZE_INT free_mem_in_hyper_free_list(Wspace *wspace, Boolean show_chunk_info)
{
  POINTER_SIZE_INT free_mem_size = 0;
  
  Free_Chunk_List *list = wspace->hyper_free_chunk_list;
  Free_Chunk *chunk = list->head;
  while(chunk){
#ifdef SSPACE_CHUNK_INFO
    if(show_chunk_info)
      printf("Size: %x\n", CHUNK_SIZE(chunk));
#endif
    free_mem_size += CHUNK_SIZE(chunk);
    assert(free_mem_size <= wspace->committed_heap_size);
    chunk = chunk->next;
  }

#ifdef SSPACE_CHUNK_INFO
    if(show_chunk_info)
      printf("Total Size in HyperFreeList: %d\n", free_mem_size);
#endif
  
  return free_mem_size;
}

POINTER_SIZE_INT free_mem_in_wspace(Wspace *wspace, Boolean show_chunk_info)
{
  POINTER_SIZE_INT free_mem_size = 0;

#ifdef SSPACE_CHUNK_INFO
  if(show_chunk_info)
    printf("\n\nPFC INFO:\n\n");
#endif
  free_mem_size += free_mem_in_pfc_pools(wspace, show_chunk_info);

#ifdef SSPACE_CHUNK_INFO
  if(show_chunk_info)
    printf("\n\nALIGNED FREE CHUNK INFO:\n\n");
#endif
  free_mem_size += free_mem_in_free_lists(wspace, aligned_free_chunk_lists, NUM_ALIGNED_FREE_CHUNK_BUCKET, show_chunk_info);

#ifdef SSPACE_CHUNK_INFO
  if(show_chunk_info)
    printf("\n\nUNALIGNED FREE CHUNK INFO:\n\n");
#endif
  free_mem_size += free_mem_in_free_lists(wspace, unaligned_free_chunk_lists, NUM_UNALIGNED_FREE_CHUNK_BUCKET, show_chunk_info);

#ifdef SSPACE_CHUNK_INFO
  if(show_chunk_info)
    printf("\n\nSUPER FREE CHUNK INFO:\n\n");
#endif
  free_mem_size += free_mem_in_hyper_free_list(wspace, show_chunk_info);
  
  return free_mem_size;
}


#ifdef SSPACE_CHUNK_INFO
void wspace_chunks_info(Wspace *wspace, Boolean show_info)
{
  if(!show_info) return;
  
  POINTER_SIZE_INT free_mem_size = free_mem_in_wspace(wspace, TRUE);
  free_mem_size += free_mem_in_used_chunk_list(wspace, TRUE);
  
  float free_mem_ratio = (float)free_mem_size / wspace->committed_heap_size;
  printf("\n\nFree mem ratio: %f\n\n", free_mem_ratio);
}
#endif


/* Because this computation doesn't use lock, its result is not accurate. And it is enough. */
POINTER_SIZE_INT wspace_free_memory_size(Wspace *wspace)
{
  POINTER_SIZE_INT free_size = 0;
  
  vm_gc_lock_enum();
  /*
  for(unsigned int i=NUM_ALIGNED_FREE_CHUNK_BUCKET; i--;)
    free_size += NORMAL_CHUNK_SIZE_BYTES * (i+1) * wspace->aligned_free_chunk_lists[i].chunk_num;
  
  for(unsigned int i=NUM_UNALIGNED_FREE_CHUNK_BUCKET; i--;)
    free_size += CHUNK_GRANULARITY * (i+1) * wspace->unaligned_free_chunk_lists[i].chunk_num;
  
  Free_Chunk *hyper_chunk = wspace->hyper_free_chunk_list->head;
  while(hyper_chunk){
    free_size += CHUNK_SIZE(hyper_chunk);
    hyper_chunk = hyper_chunk->next;
  }
  */
  free_size = free_mem_in_wspace(wspace, FALSE);
  vm_gc_unlock_enum();
  
  return free_size;
}


#ifdef SSPACE_ALLOC_INFO

#define MEDIUM_THRESHOLD 256
#define LARGE_THRESHOLD (1024)
#define SUPER_THRESHOLD (6*KB)
#define HYPER_THRESHOLD (64*KB)

#define SMALL_OBJ_ARRAY_NUM  (MEDIUM_THRESHOLD >> 2)
#define MEDIUM_OBJ_ARRAY_NUM (LARGE_THRESHOLD >> 4)
#define LARGE_OBJ_ARRAY_NUM  (SUPER_THRESHOLD >> 6)
#define SUPER_OBJ_ARRAY_NUM  (HYPER_THRESHOLD >> 10)

volatile unsigned int small_obj_num[SMALL_OBJ_ARRAY_NUM];
volatile unsigned int medium_obj_num[MEDIUM_OBJ_ARRAY_NUM];
volatile unsigned int large_obj_num[LARGE_OBJ_ARRAY_NUM];
volatile unsigned int super_obj_num[SUPER_OBJ_ARRAY_NUM];
volatile unsigned int hyper_obj_num;

void wspace_alloc_info(unsigned int size)
{
  if(size <= MEDIUM_THRESHOLD)
    atomic_inc32(&small_obj_num[(size>>2)-1]);
  else if(size <= LARGE_THRESHOLD)
    atomic_inc32(&medium_obj_num[(size>>4)-1]);
  else if(size <= SUPER_THRESHOLD)
    atomic_inc32(&large_obj_num[(size>>6)-1]);
  else if(size <= HYPER_THRESHOLD)
    atomic_inc32(&super_obj_num[(size>>10)-1]);
  else
    atomic_inc32(&hyper_obj_num);
}

void wspace_alloc_info_summary(void)
{
  unsigned int i;
  
  printf("\n\nNORMAL OBJ\n\n");
  for(i = 0; i < SMALL_OBJ_ARRAY_NUM; i++){
    printf("Size: %x\tnum: %d\n", (i+1)<<2, small_obj_num[i]);
    small_obj_num[i] = 0;
  }
  
  i = ((MEDIUM_THRESHOLD + (1<<4))>>4) - 1;
  for(; i < MEDIUM_OBJ_ARRAY_NUM; i++){
    printf("Size: %x\tnum: %d\n", (i+1)<<4, medium_obj_num[i]);
    medium_obj_num[i] = 0;
  }
  
  i = ((LARGE_THRESHOLD + (1<<6))>>6) - 1;
  for(; i < LARGE_OBJ_ARRAY_NUM; i++){
    printf("Size: %x\tnum: %d\n", (i+1)<<6, large_obj_num[i]);
    large_obj_num[i] = 0;
  }
  
  i = ((SUPER_THRESHOLD + (1<<10))>>10) - 1;
  for(; i < SUPER_OBJ_ARRAY_NUM; i++){
    printf("Size: %x\tnum: %d\n", (i+1)<<10, super_obj_num[i]);
    super_obj_num[i] = 0;
  }
  
  printf("\n\nHYPER OBJ\n\n");
  printf("num: %d\n", hyper_obj_num);
  hyper_obj_num = 0;
}

#endif

#ifdef SSPACE_USE_FASTDIV

static int total_malloc_bytes = 0;
static char *cur_free_ptr = NULL;
static int cur_free_bytes = 0;

void *malloc_wrapper(int size)
{
  massert(size > 0);
  if(!cur_free_ptr) {
    cur_free_bytes = INIT_ALLOC_SIZE;
    cur_free_ptr = (char*) STD_MALLOC(cur_free_bytes);
  }
  
  massert(cur_free_bytes >= size);
  
  total_malloc_bytes += size;
  cur_free_bytes -= size;
  
  void * ret = cur_free_ptr;
  cur_free_ptr += size;
  return ret;
}

void free_wrapper(int size)
{
  massert(size > 0);
  massert(cur_free_ptr);
  massert(total_malloc_bytes >= size);
  cur_free_bytes += size;
  total_malloc_bytes -= size;
  cur_free_ptr -= size;
}

unsigned int *shift_table;
unsigned short *compact_table[MAX_SLOT_SIZE_AFTER_SHIFTING+1];
unsigned int mask[MAX_SLOT_SIZE_AFTER_SHIFTING];
static int already_inited = 0;
void fastdiv_init()
{
  if(already_inited) return;
  already_inited = 1;
  
  int i;
  int shift_table_size = (MAX_SLOT_SIZE + 1) * sizeof shift_table[0];
  shift_table = (unsigned int *)malloc_wrapper(shift_table_size);
  memset(shift_table, 0x00, shift_table_size) ;
  for(i = MAX_SLOT_SIZE + 1;i--;) {
    shift_table[i] = 0;
    int v = i;
    while(v && !(v & 1)) {
      v >>= 1;
      shift_table[i]++;
    }
  }

  memset(compact_table, 0x00, sizeof compact_table);
  memset(mask, 0x00, sizeof mask);
  for(i = 1;i < 32;i += 2) {
    int cur = 1;
    unsigned short *p = NULL;
    while(1) {
      p = (unsigned short*)malloc_wrapper(cur * sizeof p[0]);
      memset(p, 0xff, cur * sizeof p[0]);
      int j;
      for(j = 0; j <= MAX_ADDR_OFFSET;j += i) {
        int pos = j & (cur - 1);
        if(p[pos] == 0xffff) {
          p[pos] = j / i;
        }else {
          break;
        }
      }
      if(j <= MAX_ADDR_OFFSET) {
        free_wrapper(cur * sizeof p[0]);
        cur <<= 1;
        p = NULL;
      }else {
        break;
      }
    }
    massert(p);
    mask[i] = cur - 1;
    while(cur && p[cur - 1] == 0xffff) {
      free_wrapper(sizeof p[0]);
      cur--;
    }
    compact_table[i] = p;
  }
}

#endif



