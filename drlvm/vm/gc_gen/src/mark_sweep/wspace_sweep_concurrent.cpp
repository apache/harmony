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

#include "wspace.h"
#include "wspace_chunk.h"
#include "wspace_mark_sweep.h"
#include "gc_ms.h"
#include "../thread/conclctor.h"
#include "../gen/gen.h"


static void wspace_check_free_list_chunks(Free_Chunk_List* free_list)
{
  Free_Chunk* chunk = free_list->head;
  while(chunk ){
    assert(!(chunk->status & (CHUNK_TO_MERGE |CHUNK_MERGED) ));
    chunk = chunk->next;
  }
}

static void wspace_check_free_chunks_status(Wspace* wspace)
{
  unsigned int i;
  
  for(i = NUM_ALIGNED_FREE_CHUNK_BUCKET; i--;)
    wspace_check_free_list_chunks(&wspace->aligned_free_chunk_lists[i]);

  for(i = NUM_UNALIGNED_FREE_CHUNK_BUCKET; i--;)
    wspace_check_free_list_chunks(&wspace->unaligned_free_chunk_lists[i]);

  wspace_check_free_list_chunks(wspace->hyper_free_chunk_list);

}

inline static void check_list(Free_Chunk_List *chunk_list)
{
	Free_Chunk *chunk = chunk_list->head;
	unsigned int count = 0;
	while(chunk) {
      count++;
	  chunk = chunk->next;
	}
	assert( count == chunk_list->chunk_num );
}

inline static void collector_add_free_chunk(Conclctor *sweeper, Free_Chunk *chunk)
{
  Free_Chunk_List *list = sweeper->free_chunk_list;
  
  chunk->status = CHUNK_FREE | CHUNK_TO_MERGE;
  chunk->next = list->head;
  chunk->prev = NULL;
  if(list->head)
    list->head->prev = chunk;
  else
    list->tail = chunk;
  list->head = chunk;
  list->chunk_num++;
}

static void collector_sweep_normal_chunk_con(Conclctor *sweeper, Wspace *wspace, Chunk_Header *chunk)
{
  unsigned int slot_num = chunk->slot_num;
  unsigned int live_num = 0;
  unsigned int first_free_word_index = MAX_SLOT_INDEX;
  POINTER_SIZE_INT *table = chunk->table;
  
  unsigned int index_word_num = (slot_num + SLOT_NUM_PER_WORD_IN_TABLE - 1) / SLOT_NUM_PER_WORD_IN_TABLE;
  for(unsigned int i=0; i<index_word_num; ++i){
    
    table[i] &= cur_alloc_mask;
    unsigned int live_num_in_word = (table[i] == cur_alloc_mask) ? SLOT_NUM_PER_WORD_IN_TABLE : word_set_bit_num(table[i]);
    live_num += live_num_in_word;
    
    /* for concurrent sweeping, sweeping and allocation are performed concurrently. so we can not just count the current live obj*/
    
    if((first_free_word_index == MAX_SLOT_INDEX) && (live_num_in_word < SLOT_NUM_PER_WORD_IN_TABLE)){
      first_free_word_index = i;
      pfc_set_slot_index((Chunk_Header*)chunk, first_free_word_index, cur_alloc_color);
    }
  }
  assert(live_num <= slot_num);
  sweeper->live_obj_size += live_num * chunk->slot_size;
  sweeper->live_obj_num += live_num;

  if(!live_num){  /* all objects in this chunk are dead */
    collector_add_free_chunk(sweeper, (Free_Chunk*)chunk);
   } else {
    chunk->alloc_num = live_num;
   if(!chunk_is_reusable(chunk)){  /* most objects in this chunk are swept, add chunk to pfc list*/
    wspace_reg_unreusable_normal_chunk(wspace, chunk);
   } else {  /* most objects in this chunk are swept, add chunk to pfc list*/
    wspace_put_pfc_backup(wspace, chunk);
   }
  }
}

static inline void collector_sweep_abnormal_chunk_con(Conclctor *sweeper, Wspace *wspace, Chunk_Header *chunk)
{
  assert(chunk->status == (CHUNK_ABNORMAL | CHUNK_USED));
  POINTER_SIZE_INT *table = chunk->table;
  table[0] &= cur_alloc_mask;
  if(!table[0]){    
    collector_add_free_chunk(sweeper, (Free_Chunk*)chunk);
  }
  else {
    wspace_reg_live_abnormal_chunk(wspace, chunk);
    sweeper->live_obj_size += CHUNK_SIZE(chunk);
    sweeper->live_obj_num++;
  }
}

static void wspace_sweep_chunk_con(Wspace* wspace, Conclctor* sweeper, Chunk_Header_Basic* chunk)
{  
  if(chunk->status & CHUNK_NORMAL){   /* chunk is used as a normal sized obj chunk */
    assert(chunk->status == (CHUNK_NORMAL | CHUNK_USED));
    collector_sweep_normal_chunk_con(sweeper, wspace, (Chunk_Header*)chunk);
  } else {  /* chunk is used as a super obj chunk */
    assert(chunk->status == (CHUNK_ABNORMAL | CHUNK_USED));
    collector_sweep_abnormal_chunk_con(sweeper, wspace, (Chunk_Header*)chunk);
  }
}

//used in last sweeper and final stw reset
Free_Chunk_List merged_free_chunk_list; 
Free_Chunk_List free_chunk_list_from_sweepers;
Free_Chunk_List global_free_chunk_list;

static Free_Chunk_List* wspace_collect_free_chunks_from_sweepers(GC *gc)
{
  Free_Chunk_List* free_chunk_list = &free_chunk_list_from_sweepers;
  assert(free_chunk_list);
  free_chunk_list_init(free_chunk_list);
  
  for( unsigned int i=0; i<gc->num_conclctors; i++ ) {
    Conclctor *conclctor = gc->conclctors[i];
    if( conclctor->role != CONCLCTOR_ROLE_SWEEPER )
      continue;
    Free_Chunk_List *list = conclctor->free_chunk_list;
    move_free_chunks_between_lists(free_chunk_list, list);
  }
  return free_chunk_list;
}


static void wspace_reset_free_list_chunks(Free_Chunk_List* free_list)
{
  Free_Chunk* chunk = free_list->head;
  while(chunk ){
    assert(chunk->status & CHUNK_FREE);
    chunk->status = CHUNK_FREE;
    chunk = chunk->next;
  }
}

static void wspace_reset_free_list_chunks(Free_Chunk_List* free_list, Chunk_Status_t status)
{
  Free_Chunk* chunk = free_list->head;
  while(chunk ){
    assert(chunk->status & CHUNK_FREE);
    chunk->status = status;
    chunk = chunk->next;
  }
}

static unsigned int get_to_merge_length(Free_Chunk_List *free_list) 
{
	Free_Chunk* chunk = free_list->head;
	unsigned int counter = 0;
	while(chunk) {
		if(chunk->status&CHUNK_MERGED) {
           return counter;
		}
		counter++;
		chunk = chunk->next;
	}
	return counter;
}

static unsigned int get_length(Free_Chunk_List *free_list) 
{
	Free_Chunk* chunk = free_list->head;
	unsigned int counter = 0;
	while(chunk) {
		counter++;
		chunk = chunk->next;
	}
	return counter;
}

static void wspace_merge_free_list(Wspace* wspace, Free_Chunk_List *free_list)
{
  int64 merge_start = time_now();
  Free_Chunk *wspace_ceiling = (Free_Chunk*)space_heap_end((Space*)wspace);
  Free_Chunk *chunk = free_list->head;
  while(chunk && !(chunk->status &CHUNK_MERGED)) {

    free_list->head = chunk->next;
	free_list->chunk_num--;
    if(free_list->head)
      free_list->head->prev = NULL;
    /* Check if the back adjcent chunks are free */
    Free_Chunk *back_chunk = (Free_Chunk*)chunk->adj_next;
    while(back_chunk < wspace_ceiling && (back_chunk->status & (CHUNK_TO_MERGE|CHUNK_MERGED))) {
      assert(chunk < back_chunk);
      /* Remove back_chunk from list */
      free_list_detach_chunk(free_list, back_chunk);  
      back_chunk = (Free_Chunk*)back_chunk->adj_next;
      chunk->adj_next = (Chunk_Header_Basic*)back_chunk;
    }
    if(back_chunk < wspace_ceiling)
      back_chunk->adj_prev = (Chunk_Header_Basic*)chunk;

    //INFO2("gc.con.info", "the iteration merges [" << counter << "] chunks, to merge length=" << get_to_merge_length(free_list));
    chunk->status = CHUNK_FREE | CHUNK_MERGED;
    free_chunk_list_add_tail(free_list, chunk);
    chunk = free_list->head;
  }
  //INFO2("gc.con.info", "after "<< counter <<" mergings, chunks num [" << get_length(free_list) << "], time=" << (time_now()-merge_start) << " us");
}
 

static inline Free_Chunk_List * gc_collect_global_free_chunk_list(Wspace *wspace, GC *gc)
{
  
  free_chunk_list_init(&global_free_chunk_list);
  Free_Chunk_List *global_free_list = &global_free_chunk_list;
  unsigned int i;
  for(i = NUM_ALIGNED_FREE_CHUNK_BUCKET; i--;)
    move_free_chunks_between_lists(global_free_list, &wspace->aligned_free_chunk_lists[i]);
  for(i = NUM_UNALIGNED_FREE_CHUNK_BUCKET; i--;)
    move_free_chunks_between_lists(global_free_list, &wspace->unaligned_free_chunk_lists[i]);

  move_free_chunks_between_lists(global_free_list, wspace->hyper_free_chunk_list);
  move_free_chunks_between_lists(global_free_list, &free_chunk_list_from_sweepers);
  
  wspace_reset_free_list_chunks(global_free_list, CHUNK_FREE|CHUNK_TO_MERGE);
  
  return global_free_list;
}

//final remerge in a STW manner, this can reduce the lock of merging global free list
void gc_merge_free_list_global(GC *gc) {
  Wspace *wspace = gc_get_wspace(gc);
  int64 start_merge = time_now();
  
  Free_Chunk_List *global_free_list = gc_collect_global_free_chunk_list(wspace, gc);
  wspace_merge_free_list(wspace, global_free_list);
  wspace_reset_free_list_chunks(global_free_list);
  
  //put to global list
  Free_Chunk *chunk = global_free_list->head;
  while(chunk) {
     global_free_list->head = chunk->next;
     if(global_free_list->head)
      global_free_list->head->prev = NULL;
     wspace_put_free_chunk(wspace, chunk);
     chunk = global_free_list->head;
  }
  //INFO2("gc.merge", "[merge global] time=" << (time_now()-start_merge) << " us" );
  
}


static void allocator_sweep_local_chunks(Allocator *allocator)
{
  Wspace *wspace = gc_get_wspace(allocator->gc);
  Size_Segment **size_segs = wspace->size_segments;
  Chunk_Header ***local_chunks = allocator->local_chunks;
  
  for(unsigned int i = SIZE_SEGMENT_NUM; i--;){
    if(!size_segs[i]->local_alloc){
      assert(!local_chunks[i]);
      continue;
    }
    Chunk_Header **chunks = local_chunks[i];
    assert(chunks);
    for(unsigned int j = size_segs[i]->chunk_num; j--;){
      if(chunks[j]){
        unsigned int slot_num = chunks[j]->slot_num;
        POINTER_SIZE_INT *table = chunks[j]->table;
        
        unsigned int index_word_num = (slot_num + SLOT_NUM_PER_WORD_IN_TABLE - 1) / SLOT_NUM_PER_WORD_IN_TABLE;
        for(unsigned int i=0; i<index_word_num; ++i){
          //atomic sweep.
          POINTER_SIZE_INT old_word = table[i];
          POINTER_SIZE_INT new_word = old_word & cur_alloc_mask;
          while(old_word != new_word){
            POINTER_SIZE_INT temp = (POINTER_SIZE_INT)atomic_casptr((volatile void**) &table[i],(void*) new_word,(void*) old_word);
            if(temp == old_word){
              break;
            }
            old_word = table[i];
            new_word = old_word & cur_alloc_mask;
          }
        }
      }
    }
  }
}


static void gc_sweep_mutator_local_chunks(GC *gc)
{
  lock(gc->mutator_list_lock);     // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
  /* release local chunks of each mutator in unique mark-sweep GC */
  Mutator *mutator = gc->mutator_list;
  while(mutator){
    wait_mutator_signal(mutator, HSIG_MUTATOR_SAFE);
    allocator_sweep_local_chunks((Allocator*)mutator);
    mutator = mutator->next;
  }
  unlock(gc->mutator_list_lock);
}

static void gc_wait_mutator_signal(GC *gc, unsigned int handshake_signal)
{
  lock(gc->mutator_list_lock);     // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

  /* release local chunks of each mutator in unique mark-sweep GC */
  Mutator *mutator = gc->mutator_list;
  while(mutator){
    wait_mutator_signal(mutator, handshake_signal);
    mutator = mutator->next;
  }

  unlock(gc->mutator_list_lock);
}


static volatile unsigned int num_sweeping_collectors = 0;

/*Concurrent Sweep:  
   The mark bit and alloc bit is exchanged before entering this function. 
   This function is to clear the mark bit and merge the free chunks concurrently.   
  */
void wspace_sweep_concurrent(Conclctor* sweeper)
{
  GC *gc = sweeper->gc;
  
  Wspace *wspace = gc_get_wspace(gc);

  sweeper->live_obj_size = 0;
  sweeper->live_obj_num = 0;

  Pool* used_chunk_pool = wspace->used_chunk_pool;

  Chunk_Header_Basic* chunk_to_sweep;
  
  /*1. Grab chunks from used list, sweep the chunk and push back to PFC backup list & free list.*/
  chunk_to_sweep = chunk_pool_get_chunk(used_chunk_pool);
  while(chunk_to_sweep != NULL){
    wspace_sweep_chunk_con(wspace, sweeper, chunk_to_sweep);
    chunk_to_sweep = chunk_pool_get_chunk(used_chunk_pool);
  }

  /*2. Grab chunks from PFC list, sweep the chunk and push back to PFC backup list & free list.*/
  Pool* pfc_pool = wspace_grab_next_pfc_pool(wspace);
  while(pfc_pool != NULL){
    if(!pool_is_empty(pfc_pool)){
      /*sweep the chunks in pfc_pool. push back to pfc backup list*/
      chunk_to_sweep = chunk_pool_get_chunk(pfc_pool);
      while(chunk_to_sweep != NULL){
        assert(chunk_to_sweep->status == (CHUNK_NORMAL | CHUNK_NEED_ZEROING));
        chunk_to_sweep->status = CHUNK_NORMAL | CHUNK_USED;
        wspace_sweep_chunk_con(wspace, sweeper, chunk_to_sweep);
        chunk_to_sweep = chunk_pool_get_chunk(pfc_pool);
      }
    }
    /*grab more pfc pools*/
    pfc_pool = wspace_grab_next_pfc_pool(wspace);
  }

}


//final work should be done by the last sweeper
void wspace_last_sweeper_work( Conclctor *last_sweeper ) {

  GC *gc = last_sweeper->gc;
  Wspace *wspace = gc_get_wspace(gc);
  Chunk_Header_Basic* chunk_to_sweep;
  Pool* used_chunk_pool = wspace->used_chunk_pool;

  /* all but one sweeper finishes its job*/
  state_transformation( gc, GC_CON_SWEEPING, GC_CON_SWEEP_DONE );
	
  /*3. Check the local chunk of mutator*/
  gc_sweep_mutator_local_chunks(wspace->gc);
  
    /*4. Sweep gloabl alloc normal chunks again*/
    gc_set_sweep_global_normal_chunk();
    gc_wait_mutator_signal(wspace->gc, HSIG_MUTATOR_SAFE);
    wspace_init_pfc_pool_iterator(wspace);
    Pool* pfc_pool = wspace_grab_next_pfc_pool(wspace);
    while(pfc_pool != NULL){
      if(!pool_is_empty(pfc_pool)){
        chunk_to_sweep = chunk_pool_get_chunk(pfc_pool);
        while(chunk_to_sweep != NULL){
          assert(chunk_to_sweep->status == (CHUNK_NORMAL | CHUNK_NEED_ZEROING));
          chunk_to_sweep->status = CHUNK_NORMAL | CHUNK_USED;
          wspace_sweep_chunk_con(wspace, last_sweeper, chunk_to_sweep);
          chunk_to_sweep = chunk_pool_get_chunk(pfc_pool);
        }
      }
      /*grab more pfc pools*/
      pfc_pool = wspace_grab_next_pfc_pool(wspace);
    }

    /*5. Check the used list again.*/
    chunk_to_sweep = chunk_pool_get_chunk(used_chunk_pool);
    while(chunk_to_sweep != NULL){
      wspace_sweep_chunk_con(wspace, last_sweeper, chunk_to_sweep);
      chunk_to_sweep = chunk_pool_get_chunk(used_chunk_pool);
    }

    /*6. Switch the PFC backup list to PFC list.*/
    wspace_exchange_pfc_pool(wspace);
    
    gc_unset_sweep_global_normal_chunk();

    /*7. Put back live abnormal chunk and normal unreusable chunk*/
    Chunk_Header* used_abnormal_chunk = wspace_get_live_abnormal_chunk(wspace);
    while(used_abnormal_chunk){      
      used_abnormal_chunk->status = CHUNK_USED | CHUNK_ABNORMAL;
      wspace_reg_used_chunk(wspace,used_abnormal_chunk);
      used_abnormal_chunk = wspace_get_live_abnormal_chunk(wspace);
    }
    pool_empty(wspace->live_abnormal_chunk_pool);

    Chunk_Header* unreusable_normal_chunk = wspace_get_unreusable_normal_chunk(wspace);
    while(unreusable_normal_chunk){  
      unreusable_normal_chunk->status = CHUNK_USED | CHUNK_NORMAL;
      wspace_reg_used_chunk(wspace,unreusable_normal_chunk);
      unreusable_normal_chunk = wspace_get_unreusable_normal_chunk(wspace);
    }
    pool_empty(wspace->unreusable_normal_chunk_pool);

    /*8. Merge free chunks from sweepers*/
   Free_Chunk_List *free_list_from_sweeper = wspace_collect_free_chunks_from_sweepers(gc);
   wspace_merge_free_list(wspace, free_list_from_sweeper);
     
  /* last sweeper will transform the state to before_finish */
  state_transformation( gc, GC_CON_SWEEP_DONE, GC_CON_BEFORE_FINISH );
}






