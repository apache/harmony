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
 
#ifndef _HASHCODE_H_
#define _HASHCODE_H_

#include "gc_common.h"
#include "../utils/vector_block.h"
#include "../utils/seq_list.h"

#define HASHCODE_MASK         0x1C

#define HASHCODE_SET_BIT      0x04
#define HASHCODE_ATTACHED_BIT 0x08
#define HASHCODE_BUFFERED_BIT 0x10

#define HASHCODE_EXTENDED_VT_BIT 0x02

enum Hashcode_Kind{
  HASHCODE_UNSET            = 0x0,
  HASHCODE_SET_UNALLOCATED  = HASHCODE_SET_BIT,
  HASHCODE_SET_ATTACHED     = HASHCODE_SET_BIT | HASHCODE_ATTACHED_BIT,
  HASHCODE_SET_BUFFERED     = HASHCODE_SET_BIT | HASHCODE_BUFFERED_BIT
};

inline Boolean obj_is_sethash_in_vt(Partial_Reveal_Object* p_obj){
  return (((VT_SIZE_INT)obj_get_vt_raw(p_obj) & HASHCODE_EXTENDED_VT_BIT) != 0);
}

inline void obj_sethash_in_vt(Partial_Reveal_Object* p_obj){
  VT vt = obj_get_vt_raw(p_obj);
  obj_set_vt(p_obj,(VT)((VT_SIZE_INT)vt | HASHCODE_EXTENDED_VT_BIT));
}

inline Boolean hashcode_is_set(Partial_Reveal_Object* p_obj)
{ 
  Obj_Info_Type obj_info = get_obj_info_raw(p_obj);
  return ((obj_info & HASHCODE_SET_BIT) != 0);
}

inline Boolean hashcode_is_attached(Partial_Reveal_Object* p_obj)
{ 
  Obj_Info_Type obj_info = get_obj_info_raw(p_obj);
  return ((obj_info & HASHCODE_ATTACHED_BIT) != 0);
}

inline Boolean hashcode_is_buffered(Partial_Reveal_Object* p_obj)
{
  Obj_Info_Type obj_info = get_obj_info_raw(p_obj);
  return ((obj_info & HASHCODE_BUFFERED_BIT) != 0);
}

inline int hashcode_gen(void* addr)
{ return (int)(((POINTER_SIZE_INT)addr) >> 2); }

typedef struct Hashcode_Buf{
  Seq_List* list;
  POINTER_SIZE_INT* checkpoint;
  SpinLock lock;
}Hashcode_Buf;

extern GC_Metadata gc_metadata;
Vector_Block* free_set_pool_get_entry(GC_Metadata *metadata);
void free_set_pool_put_entry(Vector_Block* block, GC_Metadata *metadata);

inline void hashcode_buf_set_checkpoint(Hashcode_Buf* hashcode_buf)
{ hashcode_buf->checkpoint = vector_block_get_last_entry((Vector_Block*)hashcode_buf->list->end); }

inline Hashcode_Buf* hashcode_buf_create()
{
  Hashcode_Buf* hashcode_buf = (Hashcode_Buf*) STD_MALLOC(sizeof(Hashcode_Buf));
  memset(hashcode_buf, 0, sizeof(Hashcode_Buf));
  hashcode_buf->list = seq_list_create();
  return hashcode_buf;
}

inline void hashcode_buf_remove(Hashcode_Buf* hashcode_buf, Vector_Block* block)
{
  Seq_List* list = hashcode_buf->list; 
  seq_list_remove(list, (List_Node*) block);
  vector_block_clear(block);
  free_set_pool_put_entry(block, &gc_metadata);
}

inline void hashcode_buf_clear(Hashcode_Buf* hashcode_buf)
{
  //push vector block back to free list
  Seq_List* list = hashcode_buf->list; 
  seq_list_iterate_init(list);
  
  while(seq_list_has_next(list)){
    Vector_Block* curr_block = (Vector_Block*)seq_list_iterate_next(list);;
    vector_block_clear(curr_block);
    free_set_pool_put_entry(curr_block, &gc_metadata);
  }
  seq_list_clear(list);
  return;
}

inline void hashcode_buf_destory(Hashcode_Buf* hashcode_buf)
{
  Seq_List* list = hashcode_buf->list; 
  hashcode_buf_clear(hashcode_buf);
  seq_list_destruct(list);
  STD_FREE((void*)hashcode_buf);
}

inline void hashcode_buf_init(Hashcode_Buf* hashcode_buf)
{
  Seq_List* list = hashcode_buf->list; 
#ifdef _DEBUG
  seq_list_iterate_init(list);
  assert(!seq_list_has_next(list));
#endif
  Vector_Block* free_block = free_set_pool_get_entry(&gc_metadata);
  seq_list_add(list, (List_Node*)free_block);
  hashcode_buf_set_checkpoint(hashcode_buf);
  return;
}

inline int hashcode_buf_lookup(Partial_Reveal_Object* p_obj,Hashcode_Buf* hashcode_buf)
{
  POINTER_SIZE_INT obj_addr = (POINTER_SIZE_INT)p_obj;
  lock(hashcode_buf->lock);
  Seq_List* list = hashcode_buf->list; 
  seq_list_iterate_init(list);
  while(seq_list_has_next(list)){
    Vector_Block* curr_block = (Vector_Block*)seq_list_iterate_next(list); 
    POINTER_SIZE_INT *iter = vector_block_iterator_init(curr_block);
    
    while(!vector_block_iterator_end(curr_block, iter)){  
      POINTER_SIZE_INT addr = (POINTER_SIZE_INT)*iter;
      if(obj_addr != addr){
        iter = vector_block_iterator_advance(curr_block, iter);
        iter = vector_block_iterator_advance(curr_block, iter);
      }else{
        iter = vector_block_iterator_advance(curr_block, iter);
        POINTER_SIZE_INT hashcode = (POINTER_SIZE_INT)*iter;
        iter = vector_block_iterator_advance(curr_block, iter);
        unlock(hashcode_buf->lock);
        return (int)hashcode;
      }
    }
  }
  assert(0);
  unlock(hashcode_buf->lock);
  return 0;
}

inline void hashcode_buf_add(Partial_Reveal_Object* p_obj, I_32 hashcode, Hashcode_Buf* hashcode_buf)
{
  Seq_List* list = hashcode_buf->list; 
  Vector_Block* tail_block = (Vector_Block*)seq_list_end_node(list);
  vector_block_add_entry(tail_block, (POINTER_SIZE_INT) p_obj);
  POINTER_SIZE_INT hashcode_var = 0;
  hashcode_var = (POINTER_SIZE_INT)hashcode;
  vector_block_add_entry(tail_block, hashcode_var);

  if(!vector_block_is_full(tail_block)) return;
  
  tail_block = free_set_pool_get_entry(&gc_metadata);
  seq_list_add(list, (List_Node*)tail_block);
  return;
}

inline void hashcode_buf_update(Partial_Reveal_Object* p_obj, I_32 hashcode, Hashcode_Buf* hashcode_buf)
{
  POINTER_SIZE_INT obj_addr = (POINTER_SIZE_INT)p_obj;
  lock(hashcode_buf->lock);
  Seq_List* list = hashcode_buf->list; 
  seq_list_iterate_init(list);
  while(seq_list_has_next(list)){
    Vector_Block* curr_block = (Vector_Block*)seq_list_iterate_next(list); 
    POINTER_SIZE_INT *iter = vector_block_iterator_init(curr_block);
    
    while(!vector_block_iterator_end(curr_block, iter)){  
      POINTER_SIZE_INT addr = (POINTER_SIZE_INT)*iter;
      if(obj_addr != addr){
        iter = vector_block_iterator_advance(curr_block, iter);
        iter = vector_block_iterator_advance(curr_block, iter);
      }else{
        iter = vector_block_iterator_advance(curr_block, iter);
        *iter = (POINTER_SIZE_INT)hashcode;
        iter = vector_block_iterator_advance(curr_block, iter);
        unlock(hashcode_buf->lock);
        return;
      }
    }
  }
  unlock(hashcode_buf->lock);
  hashcode_buf_add(p_obj, hashcode, hashcode_buf);
}

inline void hashcode_buf_refresh_all(Hashcode_Buf* hashcode_buf, POINTER_SIZE_INT dist)
{
  Seq_List* list = hashcode_buf->list; 
  seq_list_iterate_init(list);
  while(seq_list_has_next(list)){
    Vector_Block* curr_block = (Vector_Block*)seq_list_iterate_next(list);;
    POINTER_SIZE_INT *iter = vector_block_iterator_init(curr_block);
    while(!vector_block_iterator_end(curr_block, iter)){
      POINTER_SIZE_INT addr = (POINTER_SIZE_INT)*iter;
      *iter = addr - dist;
      iter =vector_block_iterator_advance(curr_block, iter);
      iter =vector_block_iterator_advance(curr_block, iter);
    }
  }
  return;
}

inline void hashcode_buf_rollback_new_entry(Hashcode_Buf* hashcode_buf)
{
  Vector_Block* first_block = VECTOR_BLOCK_HEADER(hashcode_buf->checkpoint);
  POINTER_SIZE_INT* iter = hashcode_buf->checkpoint;
  while(!vector_block_iterator_end(first_block, iter)){
    Partial_Reveal_Object* p_obj = (Partial_Reveal_Object*)*iter;
    Obj_Info_Type oi = get_obj_info_raw(p_obj);
    set_obj_info(p_obj, oi & ~HASHCODE_BUFFERED_BIT); 
    iter =vector_block_iterator_advance(first_block, iter);
    iter =vector_block_iterator_advance(first_block, iter);
  }
  first_block->tail = hashcode_buf->checkpoint;

  Seq_List* list = hashcode_buf->list; 
  seq_list_iterate_init_after_node(list, (List_Node*)first_block);
  while(seq_list_has_next(list)){
    Vector_Block* curr_block = (Vector_Block*)seq_list_iterate_next(list);;
    iter = vector_block_iterator_init(curr_block);
    while(!vector_block_iterator_end(curr_block, iter)){
      Partial_Reveal_Object* p_obj = (Partial_Reveal_Object*)*iter;
      Obj_Info_Type oi = get_obj_info_raw(p_obj);
      set_obj_info(p_obj, oi & ~HASHCODE_BUFFERED_BIT); 
      iter =vector_block_iterator_advance(curr_block, iter);
      iter =vector_block_iterator_advance(curr_block, iter);
    }
    hashcode_buf_remove(hashcode_buf, curr_block);
  } 
  return;
}

inline void hashcode_buf_transfer_new_entry(Hashcode_Buf* old_buf, Hashcode_Buf* new_buf)
{
  hashcode_buf_set_checkpoint(new_buf);

  Vector_Block* first_block = VECTOR_BLOCK_HEADER(old_buf->checkpoint);
  POINTER_SIZE_INT* iter = old_buf->checkpoint;
  while(!vector_block_iterator_end(first_block, iter)){
    Partial_Reveal_Object* p_obj = (Partial_Reveal_Object*)*iter;

    iter =vector_block_iterator_advance(first_block, iter);
    POINTER_SIZE_INT hashcode = (POINTER_SIZE_INT)*iter;
    iter =vector_block_iterator_advance(first_block, iter);
    hashcode_buf_add(p_obj, (I_32)hashcode, new_buf);
  }
  first_block->tail = old_buf->checkpoint;

  Seq_List* list = old_buf->list; 
  seq_list_iterate_init_after_node(list, (List_Node*)first_block);
  while(seq_list_has_next(list)){
    Vector_Block* curr_block = (Vector_Block*)seq_list_iterate_next(list);;
    iter = vector_block_iterator_init(curr_block);
    while(!vector_block_iterator_end(curr_block, iter)){
      Partial_Reveal_Object* p_obj = (Partial_Reveal_Object*)*iter;
      iter =vector_block_iterator_advance(curr_block, iter);
      POINTER_SIZE_INT hashcode = (POINTER_SIZE_INT)*iter;
      iter =vector_block_iterator_advance(curr_block, iter);

      hashcode_buf_add(p_obj, (I_32)hashcode, new_buf);
    }
    hashcode_buf_remove(old_buf, curr_block);
  } 
  return;
}

inline void hashcode_buf_refresh_new_entry(Hashcode_Buf* hashcode_buf, POINTER_SIZE_INT dist)
{
  Vector_Block* first_block = VECTOR_BLOCK_HEADER(hashcode_buf->checkpoint);
  POINTER_SIZE_INT* iter = hashcode_buf->checkpoint;
  while(!vector_block_iterator_end(first_block, iter)){
    POINTER_SIZE_INT addr = (POINTER_SIZE_INT)*iter;
    *iter = addr - dist;

    iter =vector_block_iterator_advance(first_block, iter);
    iter =vector_block_iterator_advance(first_block, iter);
  }

  Seq_List* list = hashcode_buf->list; 
  seq_list_iterate_init_after_node(list, (List_Node*)first_block);
  while(seq_list_has_next(list)){
    Vector_Block* curr_block = (Vector_Block*)seq_list_iterate_next(list);;
    iter = vector_block_iterator_init(curr_block);
    while(!vector_block_iterator_end(curr_block, iter)){
      POINTER_SIZE_INT addr = (POINTER_SIZE_INT)*iter;
      *iter = addr - dist;

      iter =vector_block_iterator_advance(curr_block, iter);
      iter =vector_block_iterator_advance(curr_block, iter);
    }
  } 
  hashcode_buf_set_checkpoint(hashcode_buf);
  return;
}

int obj_lookup_hashcode_in_buf(Partial_Reveal_Object *p_obj);
int obj_lookup_hashcode_in_chunk_buf(Partial_Reveal_Object *p_obj);

inline int hashcode_lookup(Partial_Reveal_Object* p_obj,Obj_Info_Type obj_info)
{
  int hash;
  if(hashcode_is_attached(p_obj)){
    int offset = vm_object_size(p_obj);
    unsigned char* pos = (unsigned char *)p_obj;
    hash = *(int*) (pos + offset);
  }else if(hashcode_is_buffered(p_obj)){
#ifdef  USE_UNIQUE_MARK_SWEEP_GC
    hash = obj_lookup_hashcode_in_chunk_buf(p_obj);
#else
    hash = obj_lookup_hashcode_in_buf(p_obj);
#endif
  }
  return hash;
}

inline void precompute_hashcode_extend_size(Partial_Reveal_Object* p_obj, void* targ_addr,
                                               unsigned int * obj_size_precompute)
{
  if(hashcode_is_set(p_obj) && !hashcode_is_attached(p_obj)){ 
    if((POINTER_SIZE_INT)targ_addr != (POINTER_SIZE_INT)p_obj)
        *obj_size_precompute += GC_OBJECT_ALIGNMENT;
  }
}

inline Obj_Info_Type forward_obj_attach_hashcode(Partial_Reveal_Object* p_targ_obj, Partial_Reveal_Object* p_obj,
                                                    Obj_Info_Type oi, unsigned int p_obj_size)
{
    oi  |= HASHCODE_ATTACHED_BIT;
    *(int *)(((char*)p_targ_obj) + p_obj_size) = hashcode_gen(p_obj);
    return oi;
}

Obj_Info_Type slide_compact_process_hashcode(Partial_Reveal_Object* p_obj, void* dest_addr, 
                                                unsigned int* p_obj_size, Collector* collector, 
                                                Hashcode_Buf* old_buf, Hashcode_Buf* new_buf);

void move_compact_process_hashcode(Partial_Reveal_Object* p_obj,Hashcode_Buf* old_buf,  
                                           Hashcode_Buf* new_buf);

#endif //_HASHCODE_H_
