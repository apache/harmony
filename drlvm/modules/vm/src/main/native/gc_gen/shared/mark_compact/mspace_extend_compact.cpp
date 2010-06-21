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

#include "mspace_collect_compact.h"
#include "../trace_forward/fspace.h"
#include "../los/lspace.h"
#include "../finalizer_weakref/finalizer_weakref.h"
#include "../gen/gen.h"
#include "../common/fix_repointed_refs.h"
#include "../common/interior_pointer.h"

static volatile Block *mos_first_new_block = NULL;
static volatile Block *nos_first_free_block = NULL;
static volatile Block *first_block_to_move = NULL;

Boolean mos_extended = FALSE;

static void set_first_and_end_block_to_move(Collector *collector, unsigned int mem_changed_size)
{
  GC_Gen *gc_gen = (GC_Gen *)collector->gc;
  Space *nos = gc_gen->nos;
  
  assert (!(mem_changed_size % SPACE_ALLOC_UNIT));
  
  unsigned int mos_added_block_num = mem_changed_size >> GC_BLOCK_SHIFT_COUNT;    // block number needing moving
  first_block_to_move = nos_first_free_block - mos_added_block_num;
  Block *nos_start_block = (Block*)space_heap_start(nos);
  if(first_block_to_move < nos_start_block)
    first_block_to_move = nos_start_block;
}

static POINTER_SIZE_INT nspace_shrink(Fspace *nspace)
{
  void *committed_nos_end = (void *)((POINTER_SIZE_INT)space_heap_start((Space *)nspace) + nspace->committed_heap_size);
  
  POINTER_SIZE_INT nos_used_size = (POINTER_SIZE_INT)nos_first_free_block - (POINTER_SIZE_INT)nspace->heap_start;
  POINTER_SIZE_INT nos_free_size = (POINTER_SIZE_INT)committed_nos_end - (POINTER_SIZE_INT)nos_first_free_block;
  POINTER_SIZE_INT decommit_size = (nos_used_size <= nos_free_size) ? nos_used_size : nos_free_size;
  assert(decommit_size);
  
  void *decommit_base = (void *)((POINTER_SIZE_INT)committed_nos_end - decommit_size);
  decommit_base = (void *)round_down_to_size((POINTER_SIZE_INT)decommit_base, SPACE_ALLOC_UNIT);
  if(decommit_base < (void *)nos_first_free_block)
    decommit_base = (void *)((POINTER_SIZE_INT)decommit_base + SPACE_ALLOC_UNIT);
  decommit_size = (POINTER_SIZE_INT)committed_nos_end - (POINTER_SIZE_INT)decommit_base;
  assert(decommit_size && !(decommit_size % SPACE_ALLOC_UNIT));
  
  Boolean result = vm_decommit_mem(decommit_base, decommit_size);
  assert(result == TRUE);
  
  nspace->committed_heap_size = (POINTER_SIZE_INT)decommit_base - (POINTER_SIZE_INT)nspace->heap_start;
  nspace->num_managed_blocks = (unsigned int)(nspace->committed_heap_size >> GC_BLOCK_SHIFT_COUNT);
  
  Block_Header *new_last_block = (Block_Header *)&nspace->blocks[nspace->num_managed_blocks - 1];
  nspace->ceiling_block_idx = new_last_block->block_idx;
  new_last_block->next = NULL;
  
  return decommit_size;
}

static void link_mspace_extended_blocks(Mspace *mspace, Fspace *nspace)
{
  Block_Header *old_last_mos_block = (Block_Header *)(mos_first_new_block -1);
  old_last_mos_block->next = (Block_Header *)mos_first_new_block;
  void *new_committed_mos_end = (void *)((POINTER_SIZE_INT)space_heap_start((Space *)mspace) + mspace->committed_heap_size); 
  Block_Header *new_last_mos_block = (Block_Header *)((Block *)new_committed_mos_end -1);
  new_last_mos_block->next = (Block_Header *)space_heap_start((Space *)nspace);
}

static Block *mspace_extend_without_link(Mspace *mspace, Fspace *nspace, unsigned int commit_size)
{
  assert(commit_size && !(commit_size % GC_BLOCK_SIZE_BYTES));
  
  void *committed_mos_end = (void *)((POINTER_SIZE_INT)space_heap_start((Space *)mspace) + mspace->committed_heap_size);
  void *commit_base = committed_mos_end;
  assert(!((POINTER_SIZE_INT)committed_mos_end % SPACE_ALLOC_UNIT));
  
  void *result = vm_commit_mem(commit_base, commit_size);
  assert(result == commit_base);
  
  void *new_end = (void *)((POINTER_SIZE_INT)commit_base + commit_size);
  mspace->committed_heap_size = (POINTER_SIZE_INT)new_end - (POINTER_SIZE_INT)mspace->heap_start;
  
  /* init the grown blocks */
  Block_Header *block = (Block_Header *)commit_base;
  Block_Header *last_block = (Block_Header *)((Block *)block -1);
  unsigned int start_idx = last_block->block_idx + 1;
  unsigned int i;
  for(i=0; block < (Block_Header *)new_end; i++){
    block_init(block);
    block->block_idx = start_idx + i;
    if(i != 0) last_block->next = block;
    last_block = block;
    block = (Block_Header *)((Block *)block + 1);
  }
  last_block->next = NULL;
  mspace->ceiling_block_idx = last_block->block_idx;
  mspace->num_managed_blocks = (unsigned int)(mspace->committed_heap_size >> GC_BLOCK_SHIFT_COUNT);
  
  return (Block *)commit_base;
}

static void mspace_block_iter_init_for_extension(Mspace *mspace, Block_Header *start_block)
{
  mspace->block_iterator = start_block;
}

static Block_Header *mspace_block_iter_next_for_extension(Mspace *mspace, Block_Header *end_block)
{
  Block_Header *cur_block = (Block_Header *)mspace->block_iterator;
  
  while(cur_block && cur_block < end_block){
    Block_Header *next_block = cur_block->next;

    Block_Header *temp = (Block_Header *)atomic_casptr((volatile void **)&mspace->block_iterator, next_block, cur_block);
    if(temp != cur_block){
      cur_block = (Block_Header*)mspace->block_iterator;
      continue;
    }
    return cur_block;
  }
  /* run out space blocks */
  return NULL;  
}

inline void object_refix_ref_slots(Partial_Reveal_Object* p_obj, void *start_address, void *end_address, unsigned int addr_diff)
{
  if( !object_has_ref_field(p_obj) ) return;
  
    /* scan array object */
  if (object_is_array(p_obj)) {
    Partial_Reveal_Array* array = (Partial_Reveal_Array*)p_obj;
    assert(!obj_is_primitive_array(p_obj));
    
    I_32 array_length = array->array_len;
    REF* p_refs = (REF*)((POINTER_SIZE_INT)array + (int)array_first_element_offset(array));

    for (int i = 0; i < array_length; i++) {
      REF* p_ref = p_refs + i;
      Partial_Reveal_Object*  p_element = read_slot(p_ref);
      if((p_element > start_address) && (p_element < end_address))
        write_slot(p_ref, (Partial_Reveal_Object*)((POINTER_SIZE_INT)p_element - addr_diff));
    }
    return;
  }

  /* scan non-array object */
  unsigned int num_refs = object_ref_field_num(p_obj);
  int *ref_iterator = object_ref_iterator_init(p_obj);
            
  for(unsigned int i=0; i<num_refs; i++){
    REF* p_ref = object_ref_iterator_get(ref_iterator+i, p_obj);        
    Partial_Reveal_Object*  p_element = read_slot(p_ref);
    if((p_element > start_address) && (p_element < end_address))
      write_slot(p_ref, (Partial_Reveal_Object*)((POINTER_SIZE_INT)p_element - addr_diff));
  }

  return;
}

static void mspace_refix_repointed_refs(Collector *collector, Mspace* mspace, void *start_address, void *end_address, unsigned int addr_diff)
{
  Block_Header *mspace_first_free_block = (Block_Header *)&mspace->blocks[mspace->free_block_idx - mspace->first_block_idx];
  
  while(Block_Header *block = mspace_block_iter_next_for_extension(mspace, mspace_first_free_block)){
    Partial_Reveal_Object *p_obj = (Partial_Reveal_Object *)block->base;
    Partial_Reveal_Object *block_end = (Partial_Reveal_Object *)block->new_free;   // new_free or free depends on whether reset is done or not
    while(p_obj < block_end){
      object_refix_ref_slots(p_obj, start_address, end_address, addr_diff);
      p_obj = obj_end(p_obj);
      if(obj_vt_is_to_next_obj(p_obj))
        p_obj = obj_get_next_obj_from_vt(p_obj);
    }
#ifdef USE_32BITS_HASHCODE
    /*repoint the p_obj in hashcode_table in the moved block.*/
    if(((void*)block) >= start_address && ((void*)block) <= end_address){      
      hashcode_buf_refresh_all(block->hashcode_buf, (POINTER_SIZE_INT)addr_diff);
    }
#endif
  }
}

static void lspace_refix_repointed_refs(Collector* collector, Lspace* lspace, void *start_address, void *end_address, unsigned int addr_diff)
{
  unsigned int start_pos = 0;
  Partial_Reveal_Object* p_obj = lspace_get_first_marked_object(lspace, &start_pos);
  while( p_obj){
    assert(obj_is_marked_in_vt(p_obj));
    object_refix_ref_slots(p_obj, start_address, end_address, addr_diff);
    p_obj = lspace_get_next_marked_object(lspace, &start_pos);
  }
}


static void gc_reupdate_repointed_sets(GC* gc, Pool* pool, void *start_address, void *end_address, unsigned int addr_diff)
{
  GC_Metadata *metadata = gc->metadata;
  assert(mos_extended);
  
  pool_iterator_init(pool);

  while(Vector_Block *root_set = pool_iterator_next(pool)){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(root_set);
    while(!vector_block_iterator_end(root_set,iter)){
      Partial_Reveal_Object **p_ref = (Partial_Reveal_Object **)*iter;
      iter = vector_block_iterator_advance(root_set,iter);

      Partial_Reveal_Object *p_obj = *p_ref;
      if((p_obj > start_address) && (p_obj < end_address))
        *p_ref = (Partial_Reveal_Object*)((POINTER_SIZE_INT)p_obj - addr_diff);
    }
  }
}

static void gc_refix_rootset(Collector *collector, void *start_address, void *end_address, unsigned int addr_diff)
{
  GC *gc = collector->gc;  
  GC_Metadata *metadata = gc->metadata;

  /* only for ALGO_MAJOR and ALGO_MAJOR_FALLBACK */
  assert(mos_extended);
  
  gc_reupdate_repointed_sets(gc, metadata->gc_rootset_pool, start_address, end_address, addr_diff);

#ifdef COMPRESS_REFERENCE
  gc_fix_uncompressed_rootset(gc);
#endif
  
#ifndef BUILD_IN_REFERENT
  gc_update_finref_repointed_refs(gc, FALSE);
#endif
  gc_reupdate_repointed_sets(gc, gc->metadata->weakroot_pool, start_address, end_address, addr_diff);

  update_rootset_interior_pointer();
}

static void move_compacted_blocks_to_mspace(Collector *collector, unsigned int addr_diff)
{
  GC_Gen *gc_gen = (GC_Gen *)collector->gc;
  Mspace *mspace = (Mspace *)gc_gen->mos;
  
  while(Block_Header *block = mspace_block_iter_next_for_extension(mspace, (Block_Header *)nos_first_free_block)){
    Partial_Reveal_Object *p_obj = (Partial_Reveal_Object *)block->base;
    void *src_base = (void *)block->base;
    void *block_end = block->new_free;   // new_free or free depends on whether reset is done or not
    POINTER_SIZE_INT size = (POINTER_SIZE_INT)block_end - (POINTER_SIZE_INT)src_base;
    Block_Header *dest_block = GC_BLOCK_HEADER((void *)((POINTER_SIZE_INT)src_base - addr_diff));
    memmove(dest_block->base, src_base, size);
    dest_block->new_free = (void *)((POINTER_SIZE_INT)block_end - addr_diff);
  }
}

static volatile unsigned int num_space_changing_collectors = 0;

#ifndef STATIC_NOS_MAPPING

/* FIXME:: this is a sequential process, the atomic parallel constructs should be removed. 
   Better to call this function in the sequential region of last phase. */
void mspace_extend_compact(Collector *collector)
{
  GC_Gen *gc_gen = (GC_Gen *)collector->gc;
  Blocked_Space *mspace = (Blocked_Space *)gc_gen->mos;
  Blocked_Space *nspace = (Blocked_Space *)gc_gen->nos;

  /*For_LOS adaptive: when doing ALGO_MAJOR_EXTEND, mspace->survive_ratio should not be updated in gc_decide_next_collect( )*/
  mos_extended = TRUE;
  
  unsigned int num_active_collectors = gc_gen->num_active_collectors;
  unsigned int old_num;
  atomic_cas32( &num_space_changing_collectors, 0, num_active_collectors + 1);
  old_num = atomic_inc32(&num_space_changing_collectors);
  if( ++old_num == num_active_collectors ){
     if(NOS_SIZE) /* when NOS_SIZE is speficied, it can't be shrunk. */
       LWARN(59, "GC: collector[{0}]: MOS is overflowed, have to reduce NOS size." << ((POINTER_SIZE_INT)collector->thread_handle));
     Block *old_nos_boundary = nspace->blocks;
     nos_boundary = &mspace->blocks[mspace->free_block_idx - mspace->first_block_idx];
     if(nspace->num_managed_blocks != 0) /* FIXME:: why can it be 0 here?? Because mspace extend can't happen is NOS is zero size */
       assert(nos_boundary > old_nos_boundary);
     POINTER_SIZE_INT mem_change_size = ((Block *)nos_boundary - old_nos_boundary) << GC_BLOCK_SHIFT_COUNT;
     nspace->heap_start = nos_boundary;
     nspace->blocks = (Block *)nos_boundary;
     nspace->committed_heap_size -= mem_change_size;
     nspace->num_managed_blocks = (unsigned int)(nspace->committed_heap_size >> GC_BLOCK_SHIFT_COUNT);
     nspace->num_total_blocks = nspace->num_managed_blocks;
     nspace->first_block_idx = mspace->free_block_idx;
     nspace->free_block_idx = nspace->first_block_idx;
     
     mspace->heap_end = nos_boundary;
     mspace->committed_heap_size += mem_change_size;
     mspace->num_managed_blocks = (unsigned int)(mspace->committed_heap_size >> GC_BLOCK_SHIFT_COUNT);
     mspace->num_total_blocks = mspace->num_managed_blocks;
     mspace->ceiling_block_idx = mspace->free_block_idx - 1;

     num_space_changing_collectors ++;
  }
  while(num_space_changing_collectors != num_active_collectors + 1);
}

#else
static volatile unsigned int num_recomputing_collectors = 0;
static volatile unsigned int num_refixing_collectors = 0;
static volatile unsigned int num_moving_collectors = 0;
static volatile unsigned int num_restoring_collectors = 0;

void mspace_extend_compact(Collector *collector)
{
  GC_Gen *gc_gen = (GC_Gen *)collector->gc;
  Mspace *mspace = gc_gen->mos;
  Fspace *nspace = gc_gen->nos;
  Lspace *lspace = gc_gen->los;

  /*For_LOS adaptive: when doing ALGO_MAJOR_EXTEND, mspace->survive_ratio should not be updated in gc_decide_next_collect()*/
  mos_extended = TRUE;
  
  unsigned int num_active_collectors = gc_gen->num_active_collectors;
  unsigned int old_num;
  
  Block *nos_first_block = nspace->blocks;
  nos_first_free_block = &mspace->blocks[mspace->free_block_idx - mspace->first_block_idx];
  assert(nos_first_free_block > nos_first_block);
  
  while(nos_first_free_block > nos_first_block){
    
    atomic_cas32( &num_space_changing_collectors, 0, num_active_collectors + 1);
    old_num = atomic_inc32(&num_space_changing_collectors);
    if( old_num == 0 ){
      unsigned int mem_changed_size = nspace_shrink(nspace);
      mos_first_new_block = mspace_extend_without_link(mspace, nspace, mem_changed_size);
      
      set_first_and_end_block_to_move(collector, mem_changed_size);
      //mspace_block_iter_init_for_extension(mspace, (Block_Header *)first_block_to_move);
      mspace_block_iter_init_for_extension(mspace, (Block_Header *)mspace->blocks);
      
      num_space_changing_collectors++;
    }
    while(num_space_changing_collectors != num_active_collectors + 1);

    atomic_cas32( &num_refixing_collectors, 0, num_active_collectors+1);
    
    mspace_refix_repointed_refs(collector, mspace, (void *)first_block_to_move, (void *)nos_first_free_block, (first_block_to_move - mos_first_new_block) << GC_BLOCK_SHIFT_COUNT);
    
    old_num = atomic_inc32(&num_refixing_collectors);
    if( ++old_num == num_active_collectors ){
      /* init the iterator: prepare for refixing */
      lspace_refix_repointed_refs(collector, lspace, (void *)first_block_to_move, (void *)nos_first_free_block, (first_block_to_move - mos_first_new_block) << GC_BLOCK_SHIFT_COUNT);
      gc_refix_rootset(collector, (void *)first_block_to_move, (void *)nos_first_free_block, (first_block_to_move - mos_first_new_block) << GC_BLOCK_SHIFT_COUNT);
      link_mspace_extended_blocks(mspace, nspace);
      mspace_block_iter_init_for_extension(mspace, (Block_Header *)first_block_to_move);
      num_refixing_collectors++;
    }
    while(num_refixing_collectors != num_active_collectors + 1);
    
    
    atomic_cas32( &num_moving_collectors, 0, num_active_collectors+1);
    
    move_compacted_blocks_to_mspace(collector, (first_block_to_move - mos_first_new_block) << GC_BLOCK_SHIFT_COUNT);
    
    old_num = atomic_inc32(&num_moving_collectors);
    if( ++old_num == num_active_collectors ){
      if(first_block_to_move == nos_first_block) {
        void *new_committed_mos_end = (void *)((unsigned int)space_heap_start((Space *)mspace) + mspace->committed_heap_size); 
        Block_Header *new_last_mos_block = (Block_Header *)((Block *)new_committed_mos_end -1);
        mspace->free_block_idx = new_last_mos_block->block_idx + 1;
      }else{
        mspace->free_block_idx = ((Block_Header*)first_block_to_move)->block_idx;
      }
      nos_first_free_block =first_block_to_move;
      num_moving_collectors++;
    }
    while(num_moving_collectors != num_active_collectors + 1);
  }
}
#endif
