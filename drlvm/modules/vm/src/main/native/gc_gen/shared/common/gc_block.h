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

/**
 * @author Xiao-Feng Li, 2006/10/05
 */

#ifndef _BLOCK_H_
#define _BLOCK_H_

#include "gc_common.h"
#ifdef USE_32BITS_HASHCODE
#include "hashcode.h"
#endif

#define GC_BLOCK_SHIFT_COUNT 15
#define GC_BLOCK_SIZE_BYTES (1 << GC_BLOCK_SHIFT_COUNT)

enum Block_Status {
  BLOCK_NIL = 0,
  BLOCK_FREE = 0x1,
  BLOCK_IN_USE = 0x2,
  BLOCK_USED = 0x4,
  BLOCK_IN_COMPACT = 0x8,
  BLOCK_COMPACTED = 0x10,
  BLOCK_TARGET = 0x20,
  BLOCK_DEST = 0x40
};

typedef struct Block_Header {
  void* base;                       
  void* free;                       
  void* ceiling;                    
  void* new_free; /* used only during compaction */
  unsigned int block_idx;           
  unsigned int num_multi_block; /* number of blocks in large block */
  volatile unsigned int status;

  volatile unsigned int num_live_objs; /* for verification debugging */
  
  /* following three fields are used only in parallel sliding compaction */
  volatile unsigned int dest_counter;
  Partial_Reveal_Object* src;
  Partial_Reveal_Object* next_src;

#ifdef USE_32BITS_HASHCODE
  Hashcode_Buf* hashcode_buf; /*hash code entry list*/
#endif
  Block_Header* next;
  POINTER_SIZE_INT table[1]; /* entry num == OFFSET_TABLE_SIZE_WORDS */
}Block_Header;

typedef union Block{
    Block_Header header;
    unsigned char raw_bytes[GC_BLOCK_SIZE_BYTES];
}Block;

#define GC_BLOCK_HEADER_VARS_SIZE_BYTES (POINTER_SIZE_INT)&(((Block_Header*)0)->table)

#define SECTOR_SIZE_SHIFT_COUNT  8
#define SECTOR_SIZE_BYTES        (1 << SECTOR_SIZE_SHIFT_COUNT)
#define SECTOR_SIZE_WORDS        (SECTOR_SIZE_BYTES >> BIT_SHIFT_TO_BYTES_PER_WORD)
/* one offset_table word maps to one SECTOR_SIZE_WORDS sector */

/* BlockSize - OffsetTableSize*SECTOR_SIZE_WORDS = HeaderVarsSize + OffsetTableSize
   => OffsetTableSize = (BlockSize - HeaderVars)/(SECTOR_SIZE_WORDS+1) */
#define OFFSET_TABLE_COMPUTE_DIVISOR       (SECTOR_SIZE_WORDS + 1)
#define OFFSET_TABLE_COMPUTED_SIZE_BYTE ((GC_BLOCK_SIZE_BYTES-GC_BLOCK_HEADER_VARS_SIZE_BYTES)/OFFSET_TABLE_COMPUTE_DIVISOR + 1)
#define OFFSET_TABLE_SIZE_BYTES ((OFFSET_TABLE_COMPUTED_SIZE_BYTE + MASK_OF_BYTES_PER_WORD)&~MASK_OF_BYTES_PER_WORD)
#define OFFSET_TABLE_SIZE_WORDS (OFFSET_TABLE_SIZE_BYTES >> BIT_SHIFT_TO_BYTES_PER_WORD)
#define OBJECT_INDEX_TO_OFFSET_TABLE(p_obj)   (ADDRESS_OFFSET_IN_BLOCK_BODY(p_obj) >> SECTOR_SIZE_SHIFT_COUNT)

#define GC_BLOCK_BODY_ALIGNMENT ((GC_OBJECT_ALIGNMENT<8) ? 8 : GC_OBJECT_ALIGNMENT)
#define GC_BLOCK_BODY_ALIGN_MASK (GC_BLOCK_BODY_ALIGNMENT-1)

#define GC_BLOCK_HEADER_SIZE_BYTES ((OFFSET_TABLE_SIZE_BYTES + GC_BLOCK_HEADER_VARS_SIZE_BYTES  + GC_BLOCK_BODY_ALIGN_MASK ) & (~GC_BLOCK_BODY_ALIGN_MASK))

#define GC_BLOCK_BODY_SIZE_BYTES (GC_BLOCK_SIZE_BYTES - GC_BLOCK_HEADER_SIZE_BYTES)
#define GC_BLOCK_BODY(block) ((void*)((POINTER_SIZE_INT)(block) + GC_BLOCK_HEADER_SIZE_BYTES))
/*LOS_Shrink: We have some fake block headers when trying to compute mos object target, 
so we must have some other methods to compute block end.*/
//#define GC_BLOCK_END(block) ((void*)((POINTER_SIZE_INT)(block) + GC_BLOCK_SIZE_BYTES))
#define GC_BLOCK_END(block) (((Block_Header*)(block))->ceiling)

#define GC_BLOCK_LOW_MASK ((POINTER_SIZE_INT)(GC_BLOCK_SIZE_BYTES - 1))
#define GC_BLOCK_HIGH_MASK (~GC_BLOCK_LOW_MASK)
#define GC_BLOCK_HEADER(addr) ((Block_Header *)((POINTER_SIZE_INT)(addr) & GC_BLOCK_HIGH_MASK))
#define GC_BLOCK_INDEX(addr) ((unsigned int)(GC_BLOCK_HEADER(addr)->block_idx))
#define GC_BLOCK_INDEX_FROM(heap_start, addr) ((unsigned int)(((POINTER_SIZE_INT)(addr)-(POINTER_SIZE_INT)(heap_start)) >> GC_BLOCK_SHIFT_COUNT))

#define ADDRESS_OFFSET_TO_BLOCK_HEADER(addr) ((unsigned int)((POINTER_SIZE_INT)addr&GC_BLOCK_LOW_MASK))
#define ADDRESS_OFFSET_IN_BLOCK_BODY(addr) ((unsigned int)(ADDRESS_OFFSET_TO_BLOCK_HEADER(addr)- GC_BLOCK_HEADER_SIZE_BYTES))

#define NUM_BLOCKS_IN_LARGE_BLOCK_FOR_SIZE(size) ((unsigned int)(((size)+ GC_BLOCK_HEADER_SIZE_BYTES + GC_BLOCK_SIZE_BYTES - 1)>>GC_BLOCK_SHIFT_COUNT))

inline void block_init(Block_Header* block)
{
  block->free = (void*)((POINTER_SIZE_INT)block + GC_BLOCK_HEADER_SIZE_BYTES);
  block->ceiling = (void*)((POINTER_SIZE_INT)block + GC_BLOCK_SIZE_BYTES); 
  block->base = block->free;
  block->new_free = block->free;
  block->num_multi_block = 0;
  block->status = BLOCK_FREE;
  block->dest_counter = 0;
  block->src = NULL;
  block->next_src = NULL;
#ifdef USE_32BITS_HASHCODE
  block->hashcode_buf = hashcode_buf_create(); 
#endif
}

inline void block_reset(Block_Header* block)
{
  if(block->status == BLOCK_FREE) return;
  block->src = NULL;
  block->next_src = NULL;
  assert(!block->dest_counter);
  block->status = BLOCK_FREE; 
  block->free = block->base;
}

inline void block_destruct(Block_Header* block)
{
#ifdef USE_32BITS_HASHCODE
  hashcode_buf_destory(block->hashcode_buf);
#endif
}

inline Partial_Reveal_Object *obj_end(Partial_Reveal_Object *obj)
{
#ifdef USE_32BITS_HASHCODE
  assert(vm_object_size(obj) != 0);
  unsigned int hash_extend_size 
    = (hashcode_is_attached(obj))?GC_OBJECT_ALIGNMENT:0;
  return (Partial_Reveal_Object *)((POINTER_SIZE_INT)obj + vm_object_size(obj) + hash_extend_size);
#else
  return (Partial_Reveal_Object *)((POINTER_SIZE_INT)obj + vm_object_size(obj));
#endif
}

#ifdef USE_32BITS_HASHCODE
inline Partial_Reveal_Object *obj_end_extend(Partial_Reveal_Object *obj) 
{
  assert(vm_object_size(obj) != 0);
  unsigned int hash_extend_size 
    = (obj_is_sethash_in_vt(obj))?GC_OBJECT_ALIGNMENT:0;
  return (Partial_Reveal_Object *)((POINTER_SIZE_INT)obj + vm_object_size(obj) + hash_extend_size);
}
#endif

/*FIXME: We use native pointer and put into oi, assuming VT and OI takes two POINTER_SIZE_INTs. 
         This is untrue if:
         1. compressed_ref && compressed_vt (vt and oi: 32bit. this can be true when heap<4G in 64bit machine)
         Fortunately, current real design gives both vt (padded) and oi 64 bit even in this case.
         
         This is true in 64bit machine if either vt or oi is not compressed: 
         2. compressed_ref && ! compressed_vt (vt: 64bit, oi: 64bit)
         3. !compressed_ref && ! compressed_vt (vt: 64bit, oi: 64bit)
         4. !compressed_ref && compressed_vt (with padded vt) (vt: 64bit, oi: 64bit) 

  When compressed_ref is true, REF is 32bit. It doesn't work in case of 2.         
  So we always use native pointer for both. 
  If case 1 design is changed in future to use 32bit for both, we need reconsider.
  
  Use REF here has another implication. Since it's a random number, can have 1 in LSBs, which might be confused as FWD_BIT or MARK_BIT.
  We need ensure that, if we use REF, we never check this prefetch pointer for FWD_BIT or MARK_BIT.
  
*/

inline void obj_set_prefetched_next_pointer(Partial_Reveal_Object* p_obj, Partial_Reveal_Object* raw_prefetched_next)
{
  Partial_Reveal_Object** p_ref = (Partial_Reveal_Object**)p_obj + 1;
  *p_ref = raw_prefetched_next;

/* see comments above. 
  REF* p_ref = (REF*)p_obj + 1;
  write_slot(p_ref, raw_prefetched_next);
*/
}

inline  Partial_Reveal_Object* obj_get_prefetched_next_pointer(Partial_Reveal_Object* p_obj)
{
  assert(p_obj);  
  Partial_Reveal_Object** p_ref = (Partial_Reveal_Object**)p_obj + 1;
  return *p_ref;

/* see comments above. 
  REF* p_ref = (REF*)p_obj + 1;
  return read_slot(p_ref);
*/
}

inline Partial_Reveal_Object *next_marked_obj_in_block(Partial_Reveal_Object *cur_obj, Partial_Reveal_Object *block_end)
{
  while(cur_obj < block_end){
    if( obj_is_marked_in_vt(cur_obj))
      return cur_obj;
    if( obj_vt_is_to_next_obj(cur_obj))
      cur_obj = obj_get_next_obj_from_vt(cur_obj);
    else
      cur_obj = obj_end(cur_obj);
    PREFETCH( ((POINTER_SIZE_INT) cur_obj) + 64);
  }
  
  return NULL;
}

inline Partial_Reveal_Object* obj_get_fw_in_table(Partial_Reveal_Object *p_obj)
{
  /* only for inter-sector compaction */
  unsigned int index    = OBJECT_INDEX_TO_OFFSET_TABLE(p_obj);
  Block_Header *curr_block = GC_BLOCK_HEADER(p_obj);
  Partial_Reveal_Object* new_addr = (Partial_Reveal_Object *)(((POINTER_SIZE_INT)p_obj) - curr_block->table[index]);
  return new_addr;
}

inline void block_clear_table(Block_Header* block)
{
  POINTER_SIZE_INT* table = block->table;
  memset(table, 0, OFFSET_TABLE_SIZE_BYTES);
  return;
}

#ifdef USE_32BITS_HASHCODE
Partial_Reveal_Object* block_get_first_marked_object_extend(Block_Header* block, void** start_pos);
#endif

Partial_Reveal_Object* block_get_first_marked_object(Block_Header* block, void** start_pos);
Partial_Reveal_Object* block_get_next_marked_object(Block_Header* block, void** start_pos);
Partial_Reveal_Object *block_get_first_marked_obj_prefetch_next(Block_Header *block, void **start_pos);
Partial_Reveal_Object *block_get_first_marked_obj_after_prefetch(Block_Header *block, void **start_pos);
Partial_Reveal_Object *block_get_next_marked_obj_prefetch_next(Block_Header *block, void **start_pos);
Partial_Reveal_Object *block_get_next_marked_obj_after_prefetch(Block_Header *block, void **start_pos);


/* <-- blocked_space hashcode_buf ops */
#ifdef USE_32BITS_HASHCODE  
inline Hashcode_Buf* block_set_hashcode_buf(Block_Header *block, Hashcode_Buf* new_hashcode_buf)
{
  Hashcode_Buf* old_hashcode_buf = block->hashcode_buf;
  block->hashcode_buf = new_hashcode_buf;
  return old_hashcode_buf;
}

inline void block_swap_hashcode_buf(Block_Header *block, Hashcode_Buf** new_ptr, Hashcode_Buf** old_ptr)
{
  Hashcode_Buf*  temp = block_set_hashcode_buf(block, *new_ptr);
  *old_ptr = *new_ptr;
  *new_ptr = temp;
  hashcode_buf_init(*new_ptr);
}

inline Hashcode_Buf* block_get_hashcode_buf(Block_Header *block)
{ return block->hashcode_buf; }

inline int obj_lookup_hashcode_in_buf(Partial_Reveal_Object *p_obj)
{
  Hashcode_Buf* hashcode_buf = block_get_hashcode_buf(GC_BLOCK_HEADER(p_obj));
  return hashcode_buf_lookup(p_obj,hashcode_buf);
}

#endif //#ifdef USE_32BITS_HASHCODE  
/* blocked_space hashcode_buf ops --> */

#endif //#ifndef _BLOCK_H_







