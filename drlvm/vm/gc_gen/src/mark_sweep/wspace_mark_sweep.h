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

#ifndef _SSPACE_MARK_SWEEP_H_
#define _SSPACE_MARK_SWEEP_H_

#include "wspace_chunk.h"
#include "wspace_verify.h"
#include "../thread/conclctor.h"

#define PFC_REUSABLE_RATIO 0.1
#define WSPACE_COMPACT_RATIO 0.06

inline Boolean chunk_is_reusable(Chunk_Header *chunk)
{ return (float)(chunk->slot_num-chunk->alloc_num)/chunk->slot_num > PFC_REUSABLE_RATIO; }

struct Conclctor;

#define OBJ_ALLOC_BIT_IN_TABLE  0x01
#define OBJ_BLACK_BIT_IN_TABLE  0x02
#define OBJ_GRAY_BIT_IN_TABLE   0x04
#define OBJ_COLOR_BIT_IN_TABLE  0x06
#define OBJ_DIRTY_BIT_IN_TABLE  0x08

enum Obj_Color {
  OBJ_COLOR_BLUE =  0x0,
  OBJ_COLOR_WHITE = OBJ_ALLOC_BIT_IN_TABLE,
  OBJ_COLOR_GRAY =  OBJ_GRAY_BIT_IN_TABLE,
  OBJ_COLOR_BLACK = OBJ_BLACK_BIT_IN_TABLE,
  OBJ_COLOR_MASK =  OBJ_COLOR_BIT_IN_TABLE
};

#ifdef POINTER64
  //#define BLACK_MASK_IN_TABLE  ((POINTER_SIZE_INT)0xAAAAAAAAAAAAAAAA)
  #define MARK_MASK_IN_TABLE  ((POINTER_SIZE_INT)0x2222222222222222)
  #define FLIP_COLOR_MASK_IN_TABLE  ((POINTER_SIZE_INT)0x3333333333333333)
  //#define DIRY_MASK_IN_TABLE   ((POINTER_SIZE_INT)0x4444444444444444)
#else
  #define MARK_MASK_IN_TABLE  ((POINTER_SIZE_INT)0x22222222)
  #define FLIP_COLOR_MASK_IN_TABLE  ((POINTER_SIZE_INT)0x33333333)
  //#define DIRY_MASK_IN_TABLE   ((POINTER_SIZE_INT)0x44444444)
#endif

extern volatile POINTER_SIZE_INT cur_alloc_color;
extern volatile POINTER_SIZE_INT cur_mark_gray_color;
extern volatile POINTER_SIZE_INT cur_mark_black_color;
extern volatile POINTER_SIZE_INT cur_alloc_mask;
extern volatile POINTER_SIZE_INT cur_mark_mask;

inline Boolean is_super_obj(Partial_Reveal_Object *obj)
{
  //return get_obj_info_raw(obj) & SUPER_OBJ_MASK;/*
  if(vm_object_size(obj) > SUPER_OBJ_THRESHOLD){
    return TRUE;
  } else {
    return FALSE;
  }
}

FORCE_INLINE POINTER_SIZE_INT *get_color_word_in_table(Partial_Reveal_Object *obj, unsigned int &index_in_word)
{
  Chunk_Header *chunk;
  unsigned int index;
  
  if(is_super_obj(obj)){
    chunk = ABNORMAL_CHUNK_HEADER(obj);
    index = 0;
  } else {
    chunk = NORMAL_CHUNK_HEADER(obj);
    index = slot_addr_to_index(chunk, obj);
  }
  //unsigned int word_index = index / SLOT_NUM_PER_WORD_IN_TABLE;
  //index_in_word = COLOR_BITS_PER_OBJ * (index % SLOT_NUM_PER_WORD_IN_TABLE);
  unsigned int word_index = index >> SLOT_NUM_PER_WORD_SHIT;
  index_in_word = COLOR_BITS_PER_OBJ * (index & (((unsigned int)(SLOT_NUM_PER_WORD_IN_TABLE-1))));
  
  return &chunk->table[word_index];
}

FORCE_INLINE POINTER_SIZE_INT *get_color_word_in_table(Partial_Reveal_Object *obj, unsigned int &index_in_word, unsigned int size)
{
  Chunk_Header *chunk;
  unsigned int index;
  
  if(size > SUPER_OBJ_THRESHOLD){
    chunk = ABNORMAL_CHUNK_HEADER(obj);
    index = 0;
  } else {
    chunk = NORMAL_CHUNK_HEADER(obj);
    index = slot_addr_to_index(chunk, obj);
  }
  unsigned int word_index = index >> SLOT_NUM_PER_WORD_SHIT;
  index_in_word = COLOR_BITS_PER_OBJ * (index & (((unsigned int)(SLOT_NUM_PER_WORD_IN_TABLE-1))));
  
  return &chunk->table[word_index];
}

#if 0
/* Accurate marking: TRUE stands for being marked by this collector, and FALSE for another collector */
inline Boolean obj_mark_in_table(Partial_Reveal_Object *obj)
{
  volatile POINTER_SIZE_INT *p_color_word;
  unsigned int index_in_word;
  p_color_word = get_color_word_in_table(obj, index_in_word);
  assert(p_color_word);
  
  POINTER_SIZE_INT color_bits_mask = ~(OBJ_COLOR_MASK << index_in_word);
  POINTER_SIZE_INT mark_color = cur_mark_color << index_in_word;
  
  POINTER_SIZE_INT old_word = *p_color_word;
  POINTER_SIZE_INT new_word = (old_word & color_bits_mask) | mark_color;
  while(new_word != old_word) {
    POINTER_SIZE_INT temp = (POINTER_SIZE_INT)atomic_casptr((volatile void**)p_color_word, (void*)new_word, (void*)old_word);
    if(temp == old_word){
#ifdef SSPACE_VERIFY
#ifndef SSPACE_VERIFY_FINREF
      assert(obj_is_marked_in_vt(obj));
#endif
      obj_unmark_in_vt(obj);
      wspace_record_mark(obj, vm_object_size(obj));
#endif
      return TRUE;
    }
    old_word = *p_color_word;
    new_word = (old_word & color_bits_mask) | mark_color;
  }
  
  return FALSE;
}

#endif

FORCE_INLINE Boolean obj_is_mark_gray_in_table(Partial_Reveal_Object *obj)
{
  POINTER_SIZE_INT *p_color_word;
  unsigned int index_in_word;
  p_color_word = get_color_word_in_table(obj, index_in_word);
  POINTER_SIZE_INT current_word = *p_color_word;
  POINTER_SIZE_INT mark_gray_color = cur_mark_gray_color << index_in_word;

  if(current_word & mark_gray_color)
    return TRUE;
  else
    return FALSE;
}

Boolean obj_is_mark_black_in_table(Partial_Reveal_Object *obj);

FORCE_INLINE Boolean obj_is_mark_black_in_table(Partial_Reveal_Object *obj, unsigned int size)
{
  POINTER_SIZE_INT *p_color_word;
  unsigned int index_in_word;
  p_color_word = get_color_word_in_table(obj, index_in_word, size);
  POINTER_SIZE_INT current_word = *p_color_word;
  POINTER_SIZE_INT mark_black_color = cur_mark_black_color << index_in_word;
  
  if(current_word & mark_black_color)
    return TRUE;
  else
    return FALSE;
  
}

//just debugging for root set size
FORCE_INLINE Boolean obj_mark_gray_in_table(Partial_Reveal_Object *obj, volatile unsigned int *slot_size)
{
  volatile POINTER_SIZE_INT *p_color_word;
  Chunk_Header *chunk;
  unsigned int slot_index;
  
  if(is_super_obj(obj)){
    chunk = ABNORMAL_CHUNK_HEADER(obj);
    slot_index = 0;
  } else {
    chunk = NORMAL_CHUNK_HEADER(obj);
    slot_index = slot_addr_to_index(chunk, obj);
  }
  
  unsigned int word_index = slot_index >> SLOT_NUM_PER_WORD_SHIT;
  unsigned int index_in_word = COLOR_BITS_PER_OBJ * (slot_index & (((unsigned int)(SLOT_NUM_PER_WORD_IN_TABLE-1))));
  p_color_word = &chunk->table[word_index];
  
  assert(p_color_word);
  
  //POINTER_SIZE_INT color_bits_mask = ~(OBJ_COLOR_MASK << index_in_word);
  POINTER_SIZE_INT mark_color = cur_mark_gray_color << index_in_word;
  
  POINTER_SIZE_INT old_word = *p_color_word;  
  if(old_word & mark_color) return FALSE; /*already marked gray*/

  apr_atomic_add32(slot_size, chunk->slot_size);
  
  //POINTER_SIZE_INT new_word = (old_word & color_bits_mask) | mark_color;
  POINTER_SIZE_INT new_word = old_word | mark_color;
  while(new_word != old_word) {
    POINTER_SIZE_INT temp = (POINTER_SIZE_INT)atomic_casptr((volatile void**)p_color_word, (void*)new_word, (void*)old_word);
    if(temp == old_word){
      return TRUE; /*returning true does not mean it's marked by this thread. */
    }
    old_word = *p_color_word;
    if(old_word & mark_color) return FALSE; /*already marked gray*/
    
    //new_word = (old_word & color_bits_mask) | mark_color;
    new_word = old_word | mark_color;
  }
  
  return FALSE;
}


FORCE_INLINE Boolean obj_mark_gray_in_table(Partial_Reveal_Object *obj)
{
  volatile POINTER_SIZE_INT *p_color_word;
  unsigned int index_in_word;
  p_color_word = get_color_word_in_table(obj, index_in_word);
  assert(p_color_word);
  
  //POINTER_SIZE_INT color_bits_mask = ~(OBJ_COLOR_MASK << index_in_word);
  POINTER_SIZE_INT mark_color = cur_mark_gray_color << index_in_word;
  
  POINTER_SIZE_INT old_word = *p_color_word;  
  if(old_word & mark_color) return FALSE; /*already marked gray*/
  
  //POINTER_SIZE_INT new_word = (old_word & color_bits_mask) | mark_color;
  POINTER_SIZE_INT new_word = old_word | mark_color;
  while(new_word != old_word) {
    POINTER_SIZE_INT temp = (POINTER_SIZE_INT)atomic_casptr((volatile void**)p_color_word, (void*)new_word, (void*)old_word);
    if(temp == old_word){
      return TRUE; /*returning true does not mean it's marked by this thread. */
    }
    old_word = *p_color_word;
    if(old_word & mark_color) return FALSE; /*already marked gray*/
    
    //new_word = (old_word & color_bits_mask) | mark_color;
    new_word = old_word | mark_color;
  }
  
  return FALSE;
}

FORCE_INLINE Boolean obj_mark_black_in_table(Partial_Reveal_Object *obj)
{
  //assert(obj_is_mark_in_table(obj));
  volatile POINTER_SIZE_INT *p_color_word;
  unsigned int index_in_word;
  p_color_word = get_color_word_in_table(obj, index_in_word);
  assert(p_color_word);
  
  //POINTER_SIZE_INT color_bits_mask = ~(OBJ_COLOR_MASK << index_in_word);
  POINTER_SIZE_INT mark_black_color = cur_mark_black_color << index_in_word;
  
  POINTER_SIZE_INT old_word = *p_color_word;  
  if(old_word & mark_black_color) return FALSE; /*already marked black*/
  
  POINTER_SIZE_INT new_word = old_word | mark_black_color;
  while(new_word != old_word) {
    POINTER_SIZE_INT temp = (POINTER_SIZE_INT)atomic_casptr((volatile void**)p_color_word, (void*)new_word, (void*)old_word);
    if(temp == old_word){
      return TRUE; /*returning true does not mean it's marked by this thread. */
    }
    old_word = *p_color_word;
    if(old_word & mark_black_color) return FALSE; /*already marked black*/
    
    new_word = old_word | mark_black_color;
  }
  
  return FALSE;

}


FORCE_INLINE Boolean obj_mark_black_in_table(Partial_Reveal_Object *obj, unsigned int size)
{
  //assert(obj_is_mark_in_table(obj));
  volatile POINTER_SIZE_INT *p_color_word;
  unsigned int index_in_word;
  p_color_word = get_color_word_in_table(obj, index_in_word, size);
  assert(p_color_word);
  
  //POINTER_SIZE_INT color_bits_mask = ~(OBJ_COLOR_MASK << index_in_word);
  POINTER_SIZE_INT mark_black_color = (OBJ_DIRTY_BIT_IN_TABLE|cur_mark_black_color) << index_in_word;   //just debugging, to mark new object
  
  POINTER_SIZE_INT old_word = *p_color_word;  
  if(old_word & mark_black_color) return FALSE; /*already marked black*/
  
  POINTER_SIZE_INT new_word = old_word | mark_black_color;
  while(new_word != old_word) {
    POINTER_SIZE_INT temp = (POINTER_SIZE_INT)atomic_casptr((volatile void**)p_color_word, (void*)new_word, (void*)old_word);
    if(temp == old_word){
      return TRUE; /*returning true does not mean it's marked by this thread. */
    }
    old_word = *p_color_word;
    if(old_word & mark_black_color) return FALSE; /*already marked black*/
    
    new_word = old_word | mark_black_color;
  }
  
  return FALSE;

}

FORCE_INLINE Boolean obj_mark_black_in_table(Partial_Reveal_Object *obj, Conclctor* marker)
{
 // assert(obj_is_mark_in_table(obj));
  volatile POINTER_SIZE_INT *p_color_word;
  Chunk_Header *chunk;
  unsigned int slot_index;
  unsigned int obj_ocuppied_size = 0;
  
  if(is_super_obj(obj)){
    chunk = ABNORMAL_CHUNK_HEADER(obj);
    slot_index = 0;
    obj_ocuppied_size = CHUNK_SIZE(chunk);
  } else {
    chunk = NORMAL_CHUNK_HEADER(obj);
    slot_index = slot_addr_to_index(chunk, obj);
    obj_ocuppied_size = chunk->slot_size;
  }
  
  unsigned int word_index = slot_index >> SLOT_NUM_PER_WORD_SHIT;
  unsigned int index_in_word = COLOR_BITS_PER_OBJ * (slot_index & (((unsigned int)(SLOT_NUM_PER_WORD_IN_TABLE-1))));
  p_color_word = &chunk->table[word_index];
  
  assert(p_color_word);
  
  //POINTER_SIZE_INT color_bits_mask = ~(OBJ_COLOR_MASK << index_in_word);
  POINTER_SIZE_INT mark_black_color = cur_mark_black_color << index_in_word;
  
  POINTER_SIZE_INT old_word = *p_color_word;
  if(obj_is_mark_black_in_table(obj)) return FALSE; /*already marked black*/

  marker->live_obj_num++;
  marker->live_obj_size+=obj_ocuppied_size;
  
  POINTER_SIZE_INT new_word = old_word | mark_black_color;
  while(new_word != old_word) {
    POINTER_SIZE_INT temp = (POINTER_SIZE_INT)atomic_casptr((volatile void**)p_color_word, (void*)new_word, (void*)old_word);
    if(temp == old_word){
      return TRUE; /*returning true does not mean it's marked by this thread. */
    }
    old_word = *p_color_word;
    if(obj_is_mark_black_in_table(obj)) return FALSE; /*already marked black*/
    
    new_word = old_word | mark_black_color;
  }
  
  return FALSE;
}

static volatile unsigned int mutator_marked = 0;

FORCE_INLINE Boolean obj_mark_black_in_table(Partial_Reveal_Object *obj, Mutator *mutator)
{
 // assert(obj_is_mark_in_table(obj));
  volatile POINTER_SIZE_INT *p_color_word;
  Chunk_Header *chunk;
  unsigned int slot_index;
  unsigned int obj_size = 0;
  
  if(is_super_obj(obj)){
    chunk = ABNORMAL_CHUNK_HEADER(obj);
    slot_index = 0;
    obj_size = CHUNK_SIZE(chunk);
  } else {
    chunk = NORMAL_CHUNK_HEADER(obj);
    slot_index = slot_addr_to_index(chunk, obj);
    obj_size = chunk->slot_size;
  }

  unsigned int word_index = slot_index >> SLOT_NUM_PER_WORD_SHIT;
  unsigned int index_in_word = COLOR_BITS_PER_OBJ * (slot_index & (((unsigned int)(SLOT_NUM_PER_WORD_IN_TABLE-1))));
  p_color_word = &chunk->table[word_index];
  assert(p_color_word);
  
  //POINTER_SIZE_INT color_bits_mask = ~(OBJ_COLOR_MASK << index_in_word);
  POINTER_SIZE_INT mark_black_color = cur_mark_black_color << index_in_word;
  
  POINTER_SIZE_INT old_word = *p_color_word;
  if(obj_is_mark_black_in_table(obj)) return FALSE; /*already marked black*/

  //mutator->new_obj_size += vm_object_size(obj);
  mutator->write_barrier_marked_size += obj_size;
  
  POINTER_SIZE_INT new_word = old_word | mark_black_color;
  while(new_word != old_word) {
    POINTER_SIZE_INT temp = (POINTER_SIZE_INT)atomic_casptr((volatile void**)p_color_word, (void*)new_word, (void*)old_word);
    if(temp == old_word){
      return TRUE; /*returning true does not mean it's marked by this thread. */
    }
    old_word = *p_color_word;
    if(obj_is_mark_black_in_table(obj)) return FALSE; /*already marked black*/
    
    new_word = old_word | mark_black_color;
  }
  
  return FALSE;
}

FORCE_INLINE Boolean obj_dirty_in_table(Partial_Reveal_Object *obj)
{
  volatile POINTER_SIZE_INT *p_color_word;
  unsigned int index_in_word;
  p_color_word = get_color_word_in_table(obj, index_in_word);
  assert(p_color_word);
  
  POINTER_SIZE_INT obj_dirty_bit_in_word = OBJ_DIRTY_BIT_IN_TABLE<< index_in_word;
  
  POINTER_SIZE_INT old_word = *p_color_word;
  if(old_word & obj_dirty_bit_in_word) return FALSE; 
  
  POINTER_SIZE_INT new_word = old_word | obj_dirty_bit_in_word;
  while(new_word != old_word) {
    POINTER_SIZE_INT temp = (POINTER_SIZE_INT)atomic_casptr((volatile void**)p_color_word, (void*)new_word, (void*)old_word);
    if(temp == old_word){
      return TRUE; /*returning true does not mean it's marked by this thread. */
    }
    old_word = *p_color_word;
    if(old_word & obj_dirty_bit_in_word) return FALSE; 
    
    new_word = old_word | obj_dirty_bit_in_word;
  }
  
  return FALSE;
}

FORCE_INLINE Boolean obj_is_dirty_in_table(Partial_Reveal_Object *obj)
{
  POINTER_SIZE_INT *p_color_word;
  unsigned int index_in_word;
  p_color_word = get_color_word_in_table(obj, index_in_word);
  POINTER_SIZE_INT current_word = *p_color_word;
  POINTER_SIZE_INT obj_dirty_bit_in_word = OBJ_DIRTY_BIT_IN_TABLE<< index_in_word;
  

  if(current_word & obj_dirty_bit_in_word)
    return TRUE;
  else
    return FALSE;
}

FORCE_INLINE Boolean obj_clear_mark_in_table(Partial_Reveal_Object *obj, Conclctor *marker)
{
   volatile POINTER_SIZE_INT *p_color_word;
  Chunk_Header *chunk;
  unsigned int slot_index;
  
  if(is_super_obj(obj)){
    chunk = ABNORMAL_CHUNK_HEADER(obj);
    slot_index = 0;
  } else {
    chunk = NORMAL_CHUNK_HEADER(obj);
    slot_index = slot_addr_to_index(chunk, obj);
  }

  unsigned int word_index = slot_index >> SLOT_NUM_PER_WORD_SHIT;
  unsigned int index_in_word = COLOR_BITS_PER_OBJ * (slot_index & (((unsigned int)(SLOT_NUM_PER_WORD_IN_TABLE-1))));
  p_color_word = &chunk->table[word_index];
  assert(p_color_word);

  if(obj_is_mark_black_in_table(obj)) {
     marker->live_obj_num--;
     marker->live_obj_size-=chunk->slot_size; 
  }
  
  //POINTER_SIZE_INT color_bits_mask = ~(OBJ_COLOR_MASK << index_in_word);
  POINTER_SIZE_INT mark_color = (cur_mark_black_color|cur_mark_gray_color) << index_in_word;
  POINTER_SIZE_INT clear_mask = ~mark_color;

  POINTER_SIZE_INT old_word = *p_color_word;  
  
  POINTER_SIZE_INT new_word = old_word & clear_mask;
  while(new_word != old_word) {
    POINTER_SIZE_INT temp = (POINTER_SIZE_INT)atomic_casptr((volatile void**)p_color_word, (void*)new_word, (void*)old_word);
    if(temp == old_word){
      return TRUE; /*returning true does not mean it's marked by this thread. */
    }
    old_word = *p_color_word;
    //if(old_word & clear_mask) return FALSE; /*already marked black*/
    
    new_word = old_word & clear_mask;
  }
  
  return FALSE;

}

FORCE_INLINE Boolean obj_clear_dirty_in_table(Partial_Reveal_Object *obj)
{
  volatile POINTER_SIZE_INT *p_color_word;
  unsigned int index_in_word;
  p_color_word = get_color_word_in_table(obj, index_in_word);
  assert(p_color_word);
  
  //POINTER_SIZE_INT color_bits_mask = ~(OBJ_COLOR_MASK << index_in_word);
  POINTER_SIZE_INT mark_color = OBJ_DIRTY_BIT_IN_TABLE << index_in_word;
  POINTER_SIZE_INT clear_mask = ~mark_color;

  POINTER_SIZE_INT old_word = *p_color_word;  
  
  POINTER_SIZE_INT new_word = old_word & clear_mask;
  while(new_word != old_word) {
    POINTER_SIZE_INT temp = (POINTER_SIZE_INT)atomic_casptr((volatile void**)p_color_word, (void*)new_word, (void*)old_word);
    if(temp == old_word){
      return TRUE; /*returning true does not mean it's marked by this thread. */
    }
    old_word = *p_color_word;
    //if(old_word & clear_mask) return FALSE; /*already marked black*/
    
    new_word = old_word & clear_mask;
  }
  
  return FALSE;

}

FORCE_INLINE Boolean obj_is_alloc_in_color_table(Partial_Reveal_Object *obj)
{
  POINTER_SIZE_INT *p_color_word;
  unsigned int index_in_word;
  p_color_word = get_color_word_in_table(obj, index_in_word);
  POINTER_SIZE_INT current_word = *p_color_word;
  POINTER_SIZE_INT obj_alloc_color_bit_in_word = cur_alloc_color << index_in_word;
  
  return (Boolean)(current_word & obj_alloc_color_bit_in_word);
}

FORCE_INLINE Boolean obj_need_take_snapshot(Partial_Reveal_Object *obj)
{
  return !obj_is_mark_black_in_table(obj) && !obj_is_dirty_in_table(obj); 
}

FORCE_INLINE Boolean obj_need_remember(Partial_Reveal_Object *obj)
{
  return (obj_is_mark_gray_in_table(obj) || obj_is_mark_black_in_table(obj)) && !obj_is_dirty_in_table(obj); 
}

FORCE_INLINE Boolean obj_need_remember_oldvar(Partial_Reveal_Object *obj)
{
  return !obj_is_mark_gray_in_table(obj) && !obj_is_mark_black_in_table(obj); 
}

inline void collector_add_free_chunk(Collector *collector, Free_Chunk *chunk)
{
  Free_Chunk_List *list = collector->free_chunk_list;
  
  chunk->status = CHUNK_FREE | CHUNK_TO_MERGE;
  chunk->next = list->head;
  chunk->prev = NULL;
  if(list->head)
    list->head->prev = chunk;
  else
    list->tail = chunk;
  list->head = chunk;
}


inline unsigned int word_set_bit_num(POINTER_SIZE_INT word)
{
  unsigned int count = 0;
  
  while(word){
    word &= word - 1;
    ++count;
  }
  return count;
}

inline void ops_color_flip(void)
{
  POINTER_SIZE_INT temp = cur_alloc_color;
  cur_alloc_color = cur_mark_black_color; //can not use mark = alloc, otherwise some obj alloc when swapping may be lost
  cur_mark_black_color = temp;
  cur_alloc_mask = (~cur_alloc_mask) & FLIP_COLOR_MASK_IN_TABLE;
  cur_mark_mask = (~cur_mark_mask) & FLIP_COLOR_MASK_IN_TABLE;
  TRACE2("gc.con","color bit flips");
}

extern void wspace_mark_scan(Collector *collector, Wspace *wspace);
extern void wspace_fallback_mark_scan(Collector *collector, Wspace *wspace);
extern void gc_init_chunk_for_sweep(GC *gc, Wspace *wspace);
extern void wspace_sweep(Collector *collector, Wspace *wspace);
extern void wspace_compact(Collector *collector, Wspace *wspace);
extern void wspace_merge_free_chunks(GC *gc, Wspace *wspace);
extern void wspace_remerge_free_chunks(GC *gc, Wspace *wspace);
extern Chunk_Header_Basic *wspace_grab_next_chunk(Wspace *wspace, Chunk_Header_Basic *volatile *shared_next_chunk, Boolean need_construct);

extern void pfc_set_slot_index(Chunk_Header *chunk, unsigned int first_free_word_index, POINTER_SIZE_INT alloc_color);
extern void pfc_reset_slot_index(Chunk_Header *chunk);

#endif // _SSPACE_MARK_SWEEP_H_
