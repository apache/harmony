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
 
#include "verifier_metadata.h"
#include "verifier_common.h"

#define GC_VERIFIER_METADATA_SIZE_BYTES (4*MB)
#define GC_VERIFIER_METADATA_EXTEND_SIZE_BYTES (4*MB)

#define GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES VECTOR_BLOCK_DATA_SIZE_BYTES

Heap_Verifier_Metadata* verifier_metadata;

void verifier_metadata_initialize(Heap_Verifier* heap_verifier)
{
  Heap_Verifier_Metadata* heap_verifier_metadata = (Heap_Verifier_Metadata* )STD_MALLOC(sizeof(Heap_Verifier_Metadata));
  assert(heap_verifier_metadata);
  memset(heap_verifier_metadata, 0, sizeof(Heap_Verifier_Metadata));
  
  unsigned int seg_size = GC_VERIFIER_METADATA_SIZE_BYTES + GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES;
  void* metadata = STD_MALLOC(seg_size);
  assert(metadata);
  memset(metadata, 0, seg_size);
  heap_verifier_metadata->segments[0] = metadata;
  metadata = (void*)round_up_to_size((POINTER_SIZE_INT)metadata, GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES);
  heap_verifier_metadata->num_alloc_segs = 1;
  
  unsigned int i = 0;
  unsigned int num_blocks = GC_VERIFIER_METADATA_SIZE_BYTES/GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES;
  for(i=0; i<num_blocks; i++){
    Vector_Block* block = (Vector_Block*)((POINTER_SIZE_INT)metadata + i*GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES);
    vector_block_init(block, GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES);
  }
  
  unsigned num_tasks = num_blocks>>1;
  heap_verifier_metadata->free_task_pool = sync_pool_create();
  for(i=0; i<num_tasks; i++){
    Vector_Block *block = (Vector_Block*)((POINTER_SIZE_INT)metadata + i*GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES);
    vector_stack_init((Vector_Block*)block);
    pool_put_entry(heap_verifier_metadata->free_task_pool, (void*)block); 
  }
  
  heap_verifier_metadata->free_set_pool = sync_pool_create();
  for(; i<num_blocks; i++){
    POINTER_SIZE_INT block = (POINTER_SIZE_INT)metadata + i*GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES;    
    pool_put_entry(heap_verifier_metadata->free_set_pool, (void*)block); 
  }

  heap_verifier_metadata->mark_task_pool = sync_pool_create();
  heap_verifier_metadata->root_set_pool = sync_pool_create();
  heap_verifier_metadata->objects_pool_before_gc  = sync_pool_create();
  heap_verifier_metadata->objects_pool_after_gc     = sync_pool_create();
  heap_verifier_metadata->resurrect_objects_pool_before_gc  = sync_pool_create();
  heap_verifier_metadata->resurrect_objects_pool_after_gc      = sync_pool_create();
  heap_verifier_metadata->new_objects_pool  = sync_pool_create();  
  heap_verifier_metadata->hashcode_pool_before_gc = sync_pool_create();
  heap_verifier_metadata->hashcode_pool_after_gc = sync_pool_create();
  heap_verifier_metadata->obj_with_fin_pool= sync_pool_create();
  heap_verifier_metadata->finalizable_obj_pool= sync_pool_create();

  verifier_metadata = heap_verifier_metadata;
  heap_verifier->heap_verifier_metadata = heap_verifier_metadata;
  return;
}

void gc_verifier_metadata_destruct(Heap_Verifier* heap_verifier)
{
  Heap_Verifier_Metadata* metadata = heap_verifier->heap_verifier_metadata;
  
  sync_pool_destruct(metadata->free_task_pool);
  sync_pool_destruct(metadata->free_set_pool);

  sync_pool_destruct(metadata->mark_task_pool);
  sync_pool_destruct(metadata->root_set_pool); 
  sync_pool_destruct(metadata->objects_pool_before_gc);
  sync_pool_destruct(metadata->objects_pool_after_gc);
  sync_pool_destruct(metadata->resurrect_objects_pool_before_gc);
  sync_pool_destruct(metadata->resurrect_objects_pool_after_gc);
  sync_pool_destruct(metadata->new_objects_pool);  
  sync_pool_destruct(metadata->hashcode_pool_before_gc);
  sync_pool_destruct(metadata->hashcode_pool_after_gc);
  
  sync_pool_destruct(metadata->obj_with_fin_pool);
  sync_pool_destruct(metadata->finalizable_obj_pool);

  for(unsigned int i=0; i<metadata->num_alloc_segs; i++){
    assert(metadata->segments[i]);
    STD_FREE(metadata->segments[i]);
  }
  STD_FREE( heap_verifier->heap_verifier_metadata);
  heap_verifier->heap_verifier_metadata = NULL;
}

Vector_Block* gc_verifier_metadata_extend(Pool* pool, Boolean is_set_pool)
{
  /*add a slot to pool point back to verifier_metadata, then we do not need the global var verifer_metadata*/
  lock(verifier_metadata->alloc_lock);
  Vector_Block* block = pool_get_entry(pool);
  if( block ){
    unlock(verifier_metadata->alloc_lock);
    return block;
  }
  
  unsigned int num_alloced = verifier_metadata->num_alloc_segs;
  if(num_alloced == METADATA_SEGMENT_NUM){
    printf("Run out GC metadata, please give it more segments!\n");
    exit(0);
  }
  unsigned int seg_size =  GC_VERIFIER_METADATA_EXTEND_SIZE_BYTES + GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES;
  void *new_segment = STD_MALLOC(seg_size);
  assert(new_segment);
  memset(new_segment, 0, seg_size);
  verifier_metadata->segments[num_alloced] = new_segment;
  new_segment = (void*)round_up_to_size((POINTER_SIZE_INT)new_segment, GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES);
  verifier_metadata->num_alloc_segs = num_alloced + 1;
  
  unsigned int num_blocks =  GC_VERIFIER_METADATA_EXTEND_SIZE_BYTES/GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES;
  
  unsigned int i=0;
  for(i=0; i<num_blocks; i++){
    Vector_Block* block = (Vector_Block*)((POINTER_SIZE_INT)new_segment + i*GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES);
    vector_block_init(block, GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES);
    assert(vector_block_is_empty(block));
  }
  
  if(is_set_pool){
    for(i=0; i<num_blocks; i++){
      POINTER_SIZE_INT block = (POINTER_SIZE_INT)new_segment + i*GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES;    
      pool_put_entry(pool, (void*)block); 
    }
  }else{
    for(i=0; i<num_blocks; i++){
      Vector_Block *block = (Vector_Block *)((POINTER_SIZE_INT)new_segment + i*GC_VERIFIER_METADATA_BLOCK_SIZE_BYTES);
      vector_stack_init(block);
      pool_put_entry(pool, (void*)block);
    }
  }

  block = pool_get_entry(pool);
  unlock(verifier_metadata->alloc_lock);
  return block;
}

void verifier_clear_pool(Pool* working_pool, Pool* free_pool, Boolean is_vector_stack)
{
  Vector_Block* working_block = pool_get_entry(working_pool);
  while(working_block){
    if(is_vector_stack) vector_stack_clear(working_block);
    else vector_block_clear(working_block);
    pool_put_entry(free_pool, working_block);
    working_block = pool_get_entry(working_pool);
  }
}

void verifier_remove_pool(Pool* working_pool, Pool* free_pool, Boolean is_vector_stack)
{
  verifier_clear_pool(working_pool, free_pool, is_vector_stack);
  sync_pool_destruct(working_pool);
}

void verifier_copy_pool_reverse_order(Pool* dest_pool, Pool* source_pool)
{
  pool_iterator_init(source_pool);
  Vector_Block* dest_set = verifier_free_set_pool_get_entry(verifier_metadata->free_set_pool);
  
  while(Vector_Block *source_set = pool_iterator_next(source_pool)){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(source_set);
    while( !vector_block_iterator_end(source_set, iter)){
      assert(!vector_block_is_full(dest_set));
      vector_block_add_entry(dest_set, *iter);
      iter = vector_block_iterator_advance(source_set, iter);
    }
    pool_put_entry(dest_pool, dest_set);
    dest_set = verifier_free_set_pool_get_entry(verifier_metadata->free_set_pool);
  }
  return ;
}

/*copy dest pool to source pool, ignore NULL slot*/
void verifier_copy_pool(Pool* dest_pool, Pool* source_pool)
{
  Pool* temp_pool = sync_pool_create();
  
  Vector_Block* dest_set = verifier_free_set_pool_get_entry(verifier_metadata->free_set_pool);
  pool_iterator_init(source_pool);
  while(Vector_Block *source_set = pool_iterator_next(source_pool)){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(source_set);
    while( !vector_block_iterator_end(source_set, iter)){
      assert(!vector_block_is_full(dest_set));
      if(*iter)  vector_block_add_entry(dest_set, *iter);
      iter = vector_block_iterator_advance(source_set, iter);
    }
    pool_put_entry(temp_pool, dest_set);
    dest_set = verifier_free_set_pool_get_entry(verifier_metadata->free_set_pool);
  }
  
  dest_set = NULL;
  pool_iterator_init(temp_pool);
  while(dest_set = pool_iterator_next(temp_pool)){
    pool_put_entry(dest_pool, dest_set);
  }
  
  sync_pool_destruct(temp_pool);
  return;
}
