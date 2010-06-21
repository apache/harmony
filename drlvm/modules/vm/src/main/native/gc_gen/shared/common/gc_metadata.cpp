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
 * @author Xiao-Feng Li, 2006/10/25
 */

#include "gc_metadata.h"
#include "interior_pointer.h"
#include "../finalizer_weakref/finalizer_weakref.h"
#include "gc_block.h"
#include "compressed_ref.h"
#include "../utils/sync_stack.h"
#include "../gen/gen.h"
#include "../verify/verify_live_heap.h"

#define GC_METADATA_SIZE_BYTES (1*MB)
#define GC_METADATA_EXTEND_SIZE_BYTES (1*MB)

#define METADATA_BLOCK_SIZE_BYTES VECTOR_BLOCK_DATA_SIZE_BYTES

GC_Metadata gc_metadata;
unsigned int rootset_type;

void gc_metadata_initialize(GC* gc)
{
  /* FIXME:: since we use a list to arrange the root sets and tasks, we can
     dynamically alloc space for metadata. 
     We just don't have this dynamic support at the moment. */
  TRACE2("gc.process", "GC: GC metadata init ...\n");
  unsigned int seg_size = GC_METADATA_SIZE_BYTES + METADATA_BLOCK_SIZE_BYTES;
  void* metadata = STD_MALLOC(seg_size);
  memset(metadata, 0, seg_size);
  gc_metadata.segments[0] = metadata;
  metadata = (void*)round_up_to_size((POINTER_SIZE_INT)metadata, METADATA_BLOCK_SIZE_BYTES);
  gc_metadata.num_alloc_segs = 1;

  unsigned int i=0;       
  unsigned int num_blocks =  GC_METADATA_SIZE_BYTES/METADATA_BLOCK_SIZE_BYTES;
  for(i=0; i<num_blocks; i++){
    Vector_Block* block = (Vector_Block*)((POINTER_SIZE_INT)metadata + i*METADATA_BLOCK_SIZE_BYTES);
    vector_block_init(block, METADATA_BLOCK_SIZE_BYTES);
  }
  
  /* part of the metadata space is used for trace_stack */
  unsigned num_tasks = num_blocks >> 1;
  gc_metadata.free_task_pool = sync_pool_create();
  for(i=0; i<num_tasks; i++){
    Vector_Block *block = (Vector_Block*)((POINTER_SIZE_INT)metadata + i*METADATA_BLOCK_SIZE_BYTES);
    vector_stack_init((Vector_Block*)block);
    pool_put_entry(gc_metadata.free_task_pool, (void*)block); 
  }
  gc_metadata.mark_task_pool = sync_pool_create();

  /* the other part is used for root sets (including rem sets) */
  gc_metadata.free_set_pool = sync_pool_create();
  /* initialize free rootset pool so that mutators can use them */  
  for(; i<num_blocks; i++){
    POINTER_SIZE_INT block = (POINTER_SIZE_INT)metadata + i*METADATA_BLOCK_SIZE_BYTES;
    pool_put_entry(gc_metadata.free_set_pool, (void*)block); 
  }

  gc_metadata.gc_rootset_pool = sync_pool_create();
  //gc_metadata.gc_verifier_rootset_pool = sync_pool_create();
  gc_metadata.gc_uncompressed_rootset_pool = sync_pool_create();
  gc_metadata.mutator_remset_pool = sync_pool_create();
  gc_metadata.collector_remset_pool = sync_pool_create();
  gc_metadata.collector_repset_pool = sync_pool_create();
  gc_metadata.gc_dirty_set_pool = sync_pool_create();
  gc_metadata.weakroot_pool = sync_pool_create();
#ifdef USE_32BITS_HASHCODE  
  gc_metadata.collector_hashcode_pool = sync_pool_create();
#endif
 
  gc->metadata = &gc_metadata; 
  return;  
}

void gc_metadata_destruct(GC* gc)
{
  TRACE2("gc.process", "GC: GC metadata destruct ...");
  GC_Metadata* metadata = gc->metadata;
  sync_pool_destruct(metadata->free_task_pool);
  sync_pool_destruct(metadata->mark_task_pool);
  
  sync_pool_destruct(metadata->free_set_pool);
  sync_pool_destruct(metadata->gc_rootset_pool);
  sync_pool_destruct(metadata->gc_uncompressed_rootset_pool);
  sync_pool_destruct(metadata->mutator_remset_pool);
  sync_pool_destruct(metadata->collector_remset_pool);
  sync_pool_destruct(metadata->collector_repset_pool);
  sync_pool_destruct(metadata->gc_dirty_set_pool);
  sync_pool_destruct(metadata->weakroot_pool);
#ifdef USE_32BITS_HASHCODE  
  sync_pool_destruct(metadata->collector_hashcode_pool);
#endif

  for(unsigned int i=0; i<metadata->num_alloc_segs; i++){
    assert(metadata->segments[i]);
    STD_FREE(metadata->segments[i]);
  }
  
  gc->metadata = NULL;  
}

Vector_Block* gc_metadata_extend(Pool* pool)
{  
  GC_Metadata *metadata = &gc_metadata;
  lock(metadata->alloc_lock);
  Vector_Block* block = pool_get_entry(pool);
  if( block ){
    unlock(metadata->alloc_lock);
    return block;
  }
 
  unsigned int num_alloced = metadata->num_alloc_segs;
  if(num_alloced == GC_METADATA_SEGMENT_NUM){
    LDIE(78, "GC: Run out GC metadata, please give it more segments!");
  }

  unsigned int seg_size =  GC_METADATA_EXTEND_SIZE_BYTES + METADATA_BLOCK_SIZE_BYTES;
  void *new_segment = STD_MALLOC(seg_size);
  memset(new_segment, 0, seg_size);
  metadata->segments[num_alloced] = new_segment;
  new_segment = (void*)round_up_to_size((POINTER_SIZE_INT)new_segment, METADATA_BLOCK_SIZE_BYTES);
  metadata->num_alloc_segs = num_alloced + 1;
  
  unsigned int num_blocks =  GC_METADATA_EXTEND_SIZE_BYTES/METADATA_BLOCK_SIZE_BYTES;

  unsigned int i=0;
  for(i=0; i<num_blocks; i++){
    Vector_Block* block = (Vector_Block*)((POINTER_SIZE_INT)new_segment + i*METADATA_BLOCK_SIZE_BYTES);
    vector_block_init(block, METADATA_BLOCK_SIZE_BYTES);
    assert(vector_block_is_empty(block));
  }

  if( pool == gc_metadata.free_task_pool){  
    for(i=0; i<num_blocks; i++){
      Vector_Block *block = (Vector_Block *)((POINTER_SIZE_INT)new_segment + i*METADATA_BLOCK_SIZE_BYTES);
      vector_stack_init(block);
      pool_put_entry(gc_metadata.free_task_pool, (void*)block);
    }
  
  }else{ 
    assert( pool == gc_metadata.free_set_pool );
    for(i=0; i<num_blocks; i++){
      POINTER_SIZE_INT block = (POINTER_SIZE_INT)new_segment + i*METADATA_BLOCK_SIZE_BYTES;    
      pool_put_entry(gc_metadata.free_set_pool, (void*)block); 
    }
  }
  
  block = pool_get_entry(pool);
  unlock(metadata->alloc_lock);

  return block;
}

static void gc_update_repointed_sets(GC* gc, Pool* pool, Boolean double_fix)
{
  GC_Metadata* metadata = gc->metadata;
  
  pool_iterator_init(pool);
  Vector_Block* root_set = pool_iterator_next(pool);

  while(root_set){
    POINTER_SIZE_INT* iter = vector_block_iterator_init(root_set);
    while(!vector_block_iterator_end(root_set,iter)){
      REF* p_ref = (REF* )*iter;
      iter = vector_block_iterator_advance(root_set,iter);

      Partial_Reveal_Object* p_obj = read_slot(p_ref);
      if( collect_is_compact_move()){ /* move-compact uses offset table */
          /*This condition is removed because we do los sliding compaction at every major compaction after add los minor sweep.*/
          //if(obj_is_moved(p_obj)) 
          /*Fixme: los_boundery ruined the modularity of gc_common.h*/
        if( gc_has_los() && p_obj < los_boundary){
            p_obj = obj_get_fw_in_oi(p_obj);
        }else{ /* this is the case with unique move_compact */
            p_obj = obj_get_fw_in_table(p_obj);
        }

        write_slot(p_ref, p_obj);

      }else{ /* this is the case of non-move-compact major collection, such as slide-compact and mark-sweep */
        if(obj_is_fw_in_oi(p_obj)){
          /* Condition obj_is_moved(p_obj) is for preventing mistaking previous mark bit of large obj as fw bit when fallback happens.
           * Because until fallback happens, perhaps the large obj hasn't been marked. So its mark bit remains as the last time.
           * This condition is removed because we do los sliding compaction at every major compaction after add los minor sweep.
           * In major collection condition obj_is_fw_in_oi(p_obj) can be omitted,
           * since those which can be scanned in MOS & NOS must have been set fw bit in oi.
           */
          p_obj = obj_get_fw_in_oi(p_obj);
          assert(address_belongs_to_gc_heap(p_obj, gc));
          /* Only major collection in MS Gen GC might need double_fix.
           * Double fixing happens when both forwarding and compaction happen.
           */
          if(double_fix && obj_is_fw_in_oi(p_obj)){
            p_obj = obj_get_fw_in_oi(p_obj);
            assert(address_belongs_to_gc_heap(p_obj, gc));
          }
          write_slot(p_ref, p_obj);
        } /* obj is forwarded */
      } /* collect is not move-compact */
        
    } /* while root_set has entry */
    root_set = pool_iterator_next(pool);
  } /* while pool has root_set */
  
  return;
}

void gc_fix_rootset(Collector* collector, Boolean double_fix)
{
  GC* gc = collector->gc;

  gc_update_weak_roots(gc, double_fix);

  /* ALGO_MINOR doesn't need rootset update, but need reset */
  if( !collect_is_minor()){
    gc_update_repointed_sets(gc, gc->metadata->gc_rootset_pool, double_fix);
#ifndef BUILD_IN_REFERENT
    gc_update_finref_repointed_refs(gc, double_fix);
#endif
  }

#ifdef COMPRESS_REFERENCE
  gc_fix_uncompressed_rootset(gc);
#endif


  update_rootset_interior_pointer();
  /* it was pointing to the last root_set entry in gc_rootset_pool (before rem_sets). */
  //gc->root_set = NULL;
  
  return;
}

void gc_set_rootset(GC* gc)
{
  GC_Metadata* metadata = gc->metadata;
  Pool* gc_rootset_pool = metadata->gc_rootset_pool;
  Pool* mutator_remset_pool = metadata->mutator_remset_pool;
  Pool* collector_remset_pool = metadata->collector_remset_pool;
  Pool* free_set_pool = metadata->free_set_pool;

  Vector_Block* root_set = NULL;
#ifdef COMPRESS_REFERENCE  
  gc_set_uncompressed_rootset(gc);
#endif
  /* put back last rootset block */
  pool_put_entry(gc_rootset_pool, gc->root_set);
  
  /* we only reset gc->root_set here for non gen mode, because we need it to remember the border
     between root_set and rem_set in gc_rootset_pool for gen mode. This is useful when a minor
     gen collection falls back to compaction, we can clear all the blocks in 
     gc_rootset_pool after the entry pointed by gc->root_set. So we clear this value
     only after we know we are not going to fallback. */
    // gc->root_set = NULL;

  if(vector_block_is_empty(gc->weakroot_set))
    pool_put_entry(free_set_pool, gc->weakroot_set);
  else
    pool_put_entry(metadata->weakroot_pool, gc->weakroot_set);
  gc->weakroot_set = NULL;
  
  if(!gc_is_gen_mode()) return;

  /* put back last remset block of each mutator */
  Mutator *mutator = gc->mutator_list;
  while (mutator) {
    pool_put_entry(mutator_remset_pool, mutator->rem_set);
    mutator->rem_set = NULL;
    mutator = mutator->next;
  }
  
  assert( collect_is_major_normal() || collect_is_minor());
  if( collect_is_major_normal() ){
    /* all the remsets are useless now */
    /* clean and put back mutator remsets */
#ifdef USE_REM_SLOTS  
    root_set = pool_get_entry( mutator_remset_pool );
    while(root_set){
        vector_block_clear(root_set);
        pool_put_entry(free_set_pool, root_set);
        root_set = pool_get_entry( mutator_remset_pool );
    }
#else
    Vector_Block* rem_set = pool_get_entry(mutator_remset_pool);
    
    while(rem_set){
      POINTER_SIZE_INT* iter = vector_block_iterator_init(rem_set);
      while(!vector_block_iterator_end(rem_set,iter)){
        Partial_Reveal_Object* p_obj_holding_ref = (Partial_Reveal_Object*)*iter;
        iter = vector_block_iterator_advance(rem_set,iter);
        
        assert( !obj_belongs_to_nos(p_obj_holding_ref));
        assert( obj_is_remembered(p_obj_holding_ref));
        obj_clear_rem_bit(p_obj_holding_ref);
      } 
      vector_block_clear(rem_set);
      pool_put_entry(free_set_pool, rem_set);      
      rem_set = pool_get_entry(metadata->mutator_remset_pool);
    }
    
#endif /* ifdef USE_REM_SLOTS else */  

    /* clean and put back collector remsets */  
    root_set = pool_get_entry( collector_remset_pool );
    while(root_set){
        vector_block_clear(root_set);
        pool_put_entry(free_set_pool, root_set);
        root_set = pool_get_entry( collector_remset_pool );
    }

  }else{ /* generational ALGO_MINOR */

    /* all the remsets are put into the shared pool */
#ifdef USE_REM_SLOTS
    root_set = pool_get_entry( mutator_remset_pool );
    while(root_set){
        pool_put_entry(gc_rootset_pool, root_set);
        root_set = pool_get_entry( mutator_remset_pool );
    }
#else /* USE_REM_OBJS */
    /* scan mutator remembered objects, and put the p_refs to collector_remset_pool if they
       hold references to NOS. The pool will be moved to rootset_pool next. */
    
    void allocator_object_write_barrier(Partial_Reveal_Object* p_object, Collector* allocator); 
    /* temporarily use collector[0]'s rem_set for the moving. Hope to be parallelized in future. */
    Collector* collector = gc->collectors[0];
    collector->rem_set = free_set_pool_get_entry(metadata);

    Vector_Block* rem_set = pool_get_entry(mutator_remset_pool);
    
    while(rem_set){
      POINTER_SIZE_INT* iter = vector_block_iterator_init(rem_set);
      while(!vector_block_iterator_end(rem_set,iter)){
        Partial_Reveal_Object* p_obj_holding_ref = (Partial_Reveal_Object*)*iter;
        iter = vector_block_iterator_advance(rem_set,iter);
        
        assert( !obj_belongs_to_nos(p_obj_holding_ref));
        assert( obj_is_remembered(p_obj_holding_ref));
        obj_clear_rem_bit(p_obj_holding_ref);
        allocator_object_write_barrier(p_obj_holding_ref, collector);  
      } 
      vector_block_clear(rem_set);
      pool_put_entry(free_set_pool, rem_set);      
      rem_set = pool_get_entry(metadata->mutator_remset_pool);
    }
    
   pool_put_entry(collector_remset_pool, collector->rem_set);
   collector->rem_set = NULL;
    
#endif /* ifdef USE_REM_SLOTS else */
  
    /* put back collector remsets */  
    root_set = pool_get_entry( collector_remset_pool );
    while(root_set){
        pool_put_entry(gc_rootset_pool, root_set);
        root_set = pool_get_entry( collector_remset_pool );
    }
    
  }
  
  return;

}

void gc_reset_rootset(GC* gc)
{
  assert(pool_is_empty(gc_metadata.gc_rootset_pool));
  ///TODO: check the statements below  assert(gc->root_set == NULL); 
  if(gc->root_set != NULL) gc->root_set = NULL; 
  gc->root_set = free_set_pool_get_entry(&gc_metadata);
  assert(vector_block_is_empty(gc->root_set));

  assert(pool_is_empty(gc_metadata.weakroot_pool));
  assert(gc->weakroot_set == NULL);
  gc->weakroot_set = free_set_pool_get_entry(&gc_metadata);
  assert(vector_block_is_empty(gc->weakroot_set));

#ifdef COMPRESS_REFERENCE
  assert(pool_is_empty(gc_metadata.gc_uncompressed_rootset_pool));
  assert(gc->uncompressed_root_set == NULL);
  gc->uncompressed_root_set = free_set_pool_get_entry(&gc_metadata);
  assert(vector_block_is_empty(gc->uncompressed_root_set));
#endif

  return;
}

void gc_clear_rootset(GC* gc)
{
  gc_reset_interior_pointer_table();
  gc_set_pool_clear(gc->metadata->gc_rootset_pool);
  gc_set_pool_clear(gc->metadata->weakroot_pool);
#ifdef COMPRESS_REFERENCE
  gc_set_pool_clear(gc->metadata->gc_uncompressed_rootset_pool);
#endif
  gc->root_set = NULL;
}


void gc_clear_remset(GC* gc)
{
  /* this function clears all the remset before fallback */
  assert(collect_is_fallback());
  
  /* rootset pool has some entries that are actually remset, because all the remsets are put into rootset pool 
     before the collection. gc->root_set is a pointer pointing to the boundary between remset and rootset in the pool */
  assert(gc->root_set != NULL);
  Pool* pool = gc_metadata.gc_rootset_pool;    
  Vector_Block* rem_set = pool_get_entry(pool);
  while(rem_set != gc->root_set){
    vector_block_clear(rem_set);
    pool_put_entry(gc_metadata.free_set_pool, rem_set);
    rem_set = pool_get_entry(pool);
  }
 
  assert(rem_set == gc->root_set);
  /* put back root set */
  pool_put_entry(pool, rem_set);
  
  /* put back last remset block of each collector (saved in the minor collection before fallback) */  
  unsigned int num_active_collectors = gc->num_active_collectors;
  pool = gc_metadata.collector_remset_pool;
  for(unsigned int i=0; i<num_active_collectors; i++)
  {
    Collector* collector = gc->collectors[i];
    assert(collector->rem_set != NULL);
    pool_put_entry(pool, collector->rem_set);
    collector->rem_set = NULL;
  }
  
  /* cleanup remset pool */  
  rem_set = pool_get_entry(pool);
  while(rem_set){
    vector_block_clear(rem_set);
    pool_put_entry(gc_metadata.free_set_pool, rem_set);
    rem_set = pool_get_entry(pool);
  }
    
  return;
} 

//#include <hash_set>
/* FIXME:: should better move to verifier dir */
extern Boolean verify_live_heap;
void gc_metadata_verify(GC* gc, Boolean is_before_gc)
{
  GC_Metadata* metadata = gc->metadata;
  assert(pool_is_empty(metadata->gc_rootset_pool));
  assert(pool_is_empty(metadata->collector_repset_pool));
  assert(pool_is_empty(metadata->mark_task_pool));
  
  if(!is_before_gc || !gc_is_gen_mode()){
    assert(pool_is_empty(metadata->mutator_remset_pool));
  }else if(gc_is_gen_mode() && verify_live_heap ){
    unsigned int remset_size = pool_size(metadata->mutator_remset_pool);
    printf("Size of mutator remset pool %s: %d\n", is_before_gc?"before GC":"after GC", remset_size);   
/*  
      using namespace stdext;
      hash_set<Partial_Reveal_Object**> pref_hash;
      unsigned int num_rem_slots = 0;
      unsigned int num_ref_to_nos = 0;

      pool_iterator_init(metadata->mutator_remset_pool);
      Vector_Block* rem_set = pool_iterator_next(metadata->mutator_remset_pool);
      while(rem_set){
        POINTER_SIZE_INT* iter = vector_block_iterator_init(rem_set);
        while(!vector_block_iterator_end(rem_set,iter)){
          Partial_Reveal_Object** p_ref = (Partial_Reveal_Object **)*iter;
          iter = vector_block_iterator_advance(rem_set,iter);

          pref_hash.insert(p_ref);
          num_rem_slots ++; 
#ifdef USE_REM_SLOTS
          Partial_Reveal_Object *p_obj = *p_ref;
          if( p_obj && addr_belongs_to_nos(p_obj))
            num_ref_to_nos++;
#endif
          if(addr_belongs_to_nos(p_ref)){
            printf("wrong remset value!!!\n");
          }
        } 
        rem_set = pool_iterator_next(metadata->mutator_remset_pool);
      }
      printf("pref hashset size is %d\n", pref_hash.size());
      printf("Num of rem slots: %d, refs to NOS: %d\n", num_rem_slots, num_ref_to_nos);
*/
  }

  if(!gc_is_gen_mode()){
    assert(pool_is_empty(metadata->collector_remset_pool));
  }else if( verify_live_heap){
    unsigned int remset_size = pool_size(metadata->collector_remset_pool);
    printf("Size of collector remset pool %s: %d\n", is_before_gc?"before GC":"after GC", remset_size);
/*    
    if(!is_before_gc){ 
 
      using namespace stdext;
      hash_set<Partial_Reveal_Object**> pref_hash;
      
      unsigned int num_rem_slots = 0;
      pool_iterator_init(metadata->collector_remset_pool);
      Vector_Block* rem_set = pool_iterator_next(metadata->collector_remset_pool);
      while(rem_set){
        POINTER_SIZE_INT* iter = vector_block_iterator_init(rem_set);
        while(!vector_block_iterator_end(rem_set,iter)){
          Partial_Reveal_Object** p_ref = (Partial_Reveal_Object **)*iter;
          iter = vector_block_iterator_advance(rem_set,iter);

          pref_hash.insert(p_ref);
          num_rem_slots ++; 
          Partial_Reveal_Object *p_obj = *p_ref;
          assert( obj_is_survivor(p_obj));
          assert( addr_belongs_to_nos(p_obj) && !addr_belongs_to_nos(p_ref));
          if( !obj_is_survivor(p_obj) || !addr_belongs_to_nos(p_obj) || addr_belongs_to_nos(p_ref)){
            printf("wrong remset value!!!\n");
          }
        } 
        rem_set = pool_iterator_next(metadata->collector_remset_pool);
      }
      printf("pref hashset size is %d\n", pref_hash.size());
      printf("Num of rem slots: %d\n", num_rem_slots);
  
    }
*/
  }/* if verify_live_heap */

  if(verify_live_heap ){
    unsigned int free_pool_size = pool_size(metadata->free_set_pool);
    printf("Size of free pool %s: %d\n\n\n", is_before_gc?"before GC":"after GC", free_pool_size); 
  }
  
  return;  
}

#ifdef _DEBUG
Boolean obj_is_mark_black_in_table(Partial_Reveal_Object* p_obj);
#endif

void analyze_bad_obj(Partial_Reveal_Object *p_obj);
void gc_reset_dirty_set(GC* gc)
{
  GC_Metadata* metadata = gc->metadata;

  Mutator *mutator = gc->mutator_list;
  while (mutator) {
    Vector_Block* local_dirty_set = mutator->dirty_set;
    if(!vector_block_is_empty(local_dirty_set)) {
      POINTER_SIZE_INT* iter = vector_block_iterator_init(local_dirty_set);
      while(!vector_block_iterator_end(local_dirty_set,iter)){
        Partial_Reveal_Object* p_obj = (Partial_Reveal_Object*) *iter;
        iter = vector_block_iterator_advance(local_dirty_set, iter);
	    analyze_bad_obj(p_obj);
      }
      RAISE_ERROR;
      vector_block_clear(mutator->dirty_set);
    }
    mutator = mutator->next;
  }

  /*reset global dirty set pool*/
  Pool* global_dirty_set_pool = metadata->gc_dirty_set_pool;

  if(!pool_is_empty(global_dirty_set_pool)){
    Vector_Block* dirty_set = pool_get_entry(global_dirty_set_pool);
    while(dirty_set != NULL) {
      if(!vector_block_is_empty(dirty_set)){
      /*
      POINTER_SIZE_INT* iter = vector_block_iterator_init(dirty_set);
      while(!vector_block_iterator_end(dirty_set,iter)){
        Partial_Reveal_Object* p_obj = (Partial_Reveal_Object*) *iter;
        iter = vector_block_iterator_advance(dirty_set, iter);
	 analyze_bad_obj(p_obj);
      }*/
      RAISE_ERROR;
        vector_block_clear(dirty_set);
        pool_put_entry(metadata->free_set_pool,dirty_set);
      } else {
         pool_put_entry(metadata->free_set_pool,dirty_set);
      }
    dirty_set = pool_get_entry(global_dirty_set_pool);
  }  
 }
}


void gc_prepare_dirty_set(GC* gc)
{
  GC_Metadata* metadata = gc->metadata;
  Pool* gc_dirty_set_pool = metadata->gc_dirty_set_pool;

  lock(gc->mutator_list_lock);
  
  Mutator *mutator = gc->mutator_list;
  while (mutator) {
    if( vector_block_is_empty(mutator->dirty_set) ) {
	  mutator = mutator->next;
	  continue; 
    }
    //FIXME: temproray solution for mostly concurrent.
    //lock(mutator->dirty_set_lock);
    pool_put_entry(gc_dirty_set_pool, mutator->dirty_set);
    mutator->dirty_set = free_set_pool_get_entry(metadata);    
    //unlock(mutator->dirty_set_lock);
    mutator = mutator->next;
  }
  unlock(gc->mutator_list_lock);
}
void gc_copy_local_dirty_set_to_global(GC *gc)
{

  GC_Metadata* metadata = gc->metadata;
  if(!pool_is_empty(metadata->gc_dirty_set_pool)) //only when the global dirty is empty
  	return;
  
  Pool* gc_dirty_set_pool = metadata->gc_dirty_set_pool;
  Vector_Block* dirty_copy = free_set_pool_get_entry(metadata);
  unsigned int i = 0;
  Vector_Block* local_dirty_set = NULL;
  
  lock(gc->mutator_list_lock);
  Mutator *mutator = gc->mutator_list;
  
  while (mutator) { 
    lock(mutator->dirty_set_lock);
    local_dirty_set = mutator->dirty_set;
    if( vector_block_is_empty(local_dirty_set) ) {
	  unlock(mutator->dirty_set_lock);
	  mutator = mutator->next;
	  continue; 
    }
    unsigned int dirty_set_size = vector_block_entry_count(local_dirty_set);
    for(i=0; i<dirty_set_size; i++) {
	POINTER_SIZE_INT p_obj = vector_block_get_entry(local_dirty_set);
	vector_block_add_entry(dirty_copy, p_obj);
	if(vector_block_is_full(dirty_copy)) {
	   pool_put_entry(gc_dirty_set_pool, dirty_copy);
	   dirty_copy = free_set_pool_get_entry(metadata); 
	 }
    }
    unlock(mutator->dirty_set_lock);
    mutator = mutator->next;
  }
  unlock(gc->mutator_list_lock);
  if( !vector_block_is_empty(dirty_copy) )
    pool_put_entry(gc_dirty_set_pool, dirty_copy);
  else
    free_set_pool_put_entry(dirty_copy, metadata);
}

void gc_clear_dirty_set(GC* gc)
{
  gc_prepare_dirty_set(gc);

  GC_Metadata* metadata = gc->metadata;

  Vector_Block* dirty_set = pool_get_entry(metadata->gc_dirty_set_pool);

  while(dirty_set){
    vector_block_clear(dirty_set);
    pool_put_entry(metadata->free_set_pool, dirty_set);
    dirty_set = pool_get_entry(metadata->gc_dirty_set_pool);
  }
}

void free_set_pool_put_entry(Vector_Block* block, GC_Metadata *metadata)
{
  if(!vector_block_is_empty(block))
  	RAISE_ERROR;
  pool_put_entry(metadata->free_set_pool, block); 
}


void gc_reset_collectors_rem_set(GC *gc) 
{  
  /* put back last remset block of each collector (saved in last collection) */  
  GC_Metadata* metadata = gc->metadata;
  unsigned int num_active_collectors = gc->num_active_collectors;
  for(unsigned int i=0; i<num_active_collectors; i++)
  {
    Collector* collector = gc->collectors[i];
    /* 1. in the first time GC, rem_set is NULL. 2. it should be NULL when NOS is forwarding_all */
    if(collector->rem_set == NULL) continue;
    pool_put_entry(metadata->collector_remset_pool, collector->rem_set);
    collector->rem_set = NULL;
  }
}


