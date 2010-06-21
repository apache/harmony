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

#ifndef _SSPACE_CHUNK_H_
#define _SSPACE_CHUNK_H_

#ifdef USE_32BITS_HASHCODE
#include "hashcode.h"
#endif

#include "wspace.h"

#define SSPACE_USE_FASTDIV
#ifdef SSPACE_USE_FASTDIV
#if 0
#define massert(x) do { if(!(x)) { printf("%s:%s, , Assertion %s failed\n", __FILE__, __LINE__, #x); exit(1); }} while(0) 
#else
#define massert(x)
#endif
#define MAX_SLOT_SIZE 1024
#define MAX_SLOT_SIZE_AFTER_SHIFTING 31
#define MAX_ADDR_OFFSET (16 * 1024)
#define INIT_ALLOC_SIZE (128 * 1024)

extern unsigned int *shift_table;
extern unsigned short *compact_table[MAX_SLOT_SIZE_AFTER_SHIFTING+1];
extern unsigned int mask[MAX_SLOT_SIZE_AFTER_SHIFTING];
extern void fastdiv_init(void);

inline int fastdiv_div(int x, int y)
{
  massert(x % y == 0);
  massert(0 <= y && y <= 1024);
  massert(y % 4 == 0);
  massert(y <= 128 || y % 8 == 0);
  massert(y <= 256 || y % 128 == 0);
  massert(x <= (1 << 16));

  int s = shift_table[y];
  massert(s >= 2);
  x >>= s;
  y >>= s;

  massert(x >= 0 && x <= 16 * (1 << 10));
  massert(y <= 32 && y % 2);

  return (int)compact_table[y][x & mask[y]];
}
#else
#define fastdiv_div(x,y) ((x) / (y))
#define fastdiv_init() ((void)0)
#endif

enum Chunk_Status {
  CHUNK_NIL = 0,
  CHUNK_FREE = 0x1,
  CHUNK_FRESH = 0x2,
  CHUNK_NORMAL = 0x10,
  CHUNK_ABNORMAL = 0x20,
  CHUNK_NEED_ZEROING = 0x100,
  CHUNK_TO_MERGE = 0x200,
  CHUNK_IN_USE = 0x400, /* just keep info for now, not used */
  CHUNK_USED = 0x800, /* just keep info for now, not used */
  CHUNK_MERGED = 0x1000
};

typedef volatile POINTER_SIZE_INT Chunk_Status_t;

typedef struct Chunk_Header_Basic {
  Chunk_Header_Basic *next;
  Chunk_Header_Basic *prev;
  Chunk_Status_t status;
  Chunk_Header_Basic *adj_next;  // adjacent next chunk
  Chunk_Header_Basic *adj_prev;  // adjacent previous chunk, for merging continuous free chunks
} Chunk_Header_Basic;

typedef struct Chunk_Header {
  /* Beginning of Chunk_Header_Basic */
  Chunk_Header *next;           /* pointing to the next pfc in the pfc pool */
  Chunk_Header *prev;           /* pointing to the prev pfc in the pfc pool */
  Chunk_Status_t status;
  Chunk_Header_Basic *adj_next;  // adjacent next chunk
  Chunk_Header_Basic *adj_prev;  // adjacent previous chunk, for merging continuous free chunks
  /* End of Chunk_Header_Basic */
  void *base;
  unsigned int slot_size;
  unsigned int slot_num;
  unsigned int slot_index;      /* the index of which is the first free slot in this chunk */
  unsigned int alloc_num;       /* the index of which is the first free slot in this chunk */
#ifdef USE_32BITS_HASHCODE
  Hashcode_Buf* hashcode_buf; /*hash code entry list*/
#endif  
  POINTER_SIZE_INT table[1];
} Chunk_Header;


#define NORMAL_CHUNK_SHIFT_COUNT    16
#define NORMAL_CHUNK_SIZE_BYTES     (1 << NORMAL_CHUNK_SHIFT_COUNT)
#define NORMAL_CHUNK_LOW_MASK       ((POINTER_SIZE_INT)(NORMAL_CHUNK_SIZE_BYTES - 1))
#define NORMAL_CHUNK_HIGH_MASK      (~NORMAL_CHUNK_LOW_MASK)
#define NORMAL_CHUNK_HEADER(addr)   ((Chunk_Header*)((POINTER_SIZE_INT)(addr) & NORMAL_CHUNK_HIGH_MASK))
#define ABNORMAL_CHUNK_HEADER(addr) ((Chunk_Header*)((POINTER_SIZE_INT)addr & CHUNK_GRANULARITY_HIGH_MASK))

#define MAX_SLOT_INDEX 0xFFffFFff
//#define COLOR_BITS_PER_OBJ              4   // should be powers of 2
#define COLOR_BITS_PER_OBJ_SHIT     2 // COLOR_BITS_PER_OBJ = 1 << COLOR_BITS_PER_OBJ_SHIT
#define COLOR_BITS_PER_OBJ          (1<<COLOR_BITS_PER_OBJ_SHIT)

//#define SLOT_NUM_PER_WORD_IN_TABLE  (BITS_PER_WORD /COLOR_BITS_PER_OBJ)
#define SLOT_NUM_PER_WORD_SHIT      (BIT_SHIFT_TO_BITS_PER_WORD - COLOR_BITS_PER_OBJ_SHIT)
#define SLOT_NUM_PER_WORD_IN_TABLE  (1<<SLOT_NUM_PER_WORD_SHIT)


/* Two equations:
 * 1. CHUNK_HEADER_VARS_SIZE_BYTES + NORMAL_CHUNK_TABLE_SIZE_BYTES + slot_size*NORMAL_CHUNK_SLOT_NUM = NORMAL_CHUNK_SIZE_BYTES
 * 2. (BITS_PER_BYTE * NORMAL_CHUNK_TABLE_SIZE_BYTES)/COLOR_BITS_PER_OBJ >= NORMAL_CHUNK_SLOT_NUM
 * ===>
 * NORMAL_CHUNK_SLOT_NUM <= BITS_PER_BYTE*(NORMAL_CHUNK_SIZE_BYTES - CHUNK_HEADER_VARS_SIZE_BYTES) / (BITS_PER_BYTE*slot_size + COLOR_BITS_PER_OBJ)
 * ===>
 * NORMAL_CHUNK_SLOT_NUM = BITS_PER_BYTE*(NORMAL_CHUNK_SIZE_BYTES - CHUNK_HEADER_VARS_SIZE_BYTES) / (BITS_PER_BYTE*slot_size + COLOR_BITS_PER_OBJ)
 */

#define CHUNK_HEADER_VARS_SIZE_BYTES      ((POINTER_SIZE_INT)&(((Chunk_Header*)0)->table))
#define NORMAL_CHUNK_SLOT_AREA_SIZE_BITS  (BITS_PER_BYTE * (NORMAL_CHUNK_SIZE_BYTES - CHUNK_HEADER_VARS_SIZE_BYTES))
#define SIZE_BITS_PER_SLOT(chunk)         (BITS_PER_BYTE * chunk->slot_size + COLOR_BITS_PER_OBJ)

#define NORMAL_CHUNK_SLOT_NUM(chunk)          (NORMAL_CHUNK_SLOT_AREA_SIZE_BITS / SIZE_BITS_PER_SLOT(chunk))
#define NORMAL_CHUNK_TABLE_SIZE_BYTES(chunk)  (((NORMAL_CHUNK_SLOT_NUM(chunk) + SLOT_NUM_PER_WORD_IN_TABLE-1) / SLOT_NUM_PER_WORD_IN_TABLE) * BYTES_PER_WORD)
#define NORMAL_CHUNK_HEADER_SIZE_BYTES(chunk) (CHUNK_HEADER_VARS_SIZE_BYTES + NORMAL_CHUNK_TABLE_SIZE_BYTES(chunk))

#define NORMAL_CHUNK_BASE(chunk)    ((void*)((POINTER_SIZE_INT)(chunk) + NORMAL_CHUNK_HEADER_SIZE_BYTES(chunk)))
#define ABNORMAL_CHUNK_BASE(chunk)  ((void*)((POINTER_SIZE_INT)(chunk) + sizeof(Chunk_Header)))

#define CHUNK_END(chunk)  ((chunk)->adj_next)
#define CHUNK_SIZE(chunk) ((POINTER_SIZE_INT)chunk->adj_next - (POINTER_SIZE_INT)chunk)

#define NUM_ALIGNED_FREE_CHUNK_BUCKET   (HYPER_OBJ_THRESHOLD >> NORMAL_CHUNK_SHIFT_COUNT)
#define NUM_UNALIGNED_FREE_CHUNK_BUCKET (HYPER_OBJ_THRESHOLD >> CHUNK_GRANULARITY_BITS)

inline void *slot_index_to_addr(Chunk_Header *chunk, unsigned int index)
{ return (void*)((POINTER_SIZE_INT)chunk->base + chunk->slot_size * index); }

inline unsigned int slot_addr_to_index(Chunk_Header *chunk, void *addr)
{ return (unsigned int)fastdiv_div(((POINTER_SIZE_INT)addr - (POINTER_SIZE_INT)chunk->base) , chunk->slot_size); }

typedef struct Free_Chunk {
  /* Beginning of Chunk_Header_Basic */
  Free_Chunk *next;             /* pointing to the next free Free_Chunk */
  Free_Chunk *prev;             /* pointing to the prev free Free_Chunk */
  Chunk_Status_t status;
  Chunk_Header_Basic *adj_next;  // adjacent next chunk
  Chunk_Header_Basic *adj_prev;  // adjacent previous chunk, for merging continuous free chunks
  /* End of Chunk_Header_Basic */
} Free_Chunk;

typedef struct Free_Chunk_List {
  Free_Chunk *head;  /* get new free chunk from head */
  Free_Chunk *tail;  /* put free chunk to tail */
  unsigned int chunk_num;
  SpinLock lock;
} Free_Chunk_List;

/*
typedef union Chunk{
  Chunk_Header   header;
  Free_Chunk     free_chunk;
  unsigned char  raw_bytes[NORMAL_CHUNK_SIZE_BYTES];
} Chunk;
*/

inline Boolean is_free_chunk_merged(Free_Chunk* free_chunk)
{
  assert(free_chunk->status & CHUNK_FREE);
  return (Boolean)(free_chunk->status & CHUNK_MERGED);
}

inline void free_chunk_list_init(Free_Chunk_List *list)
{
  list->head = NULL;
  list->tail = NULL;
  list->chunk_num = 0;
  list->lock = FREE_LOCK;
}

inline void free_chunk_list_clear(Free_Chunk_List *list)
{
  list->head = NULL;
  list->tail = NULL;
  list->chunk_num = 0;
  assert(list->lock == FREE_LOCK);
}

inline void free_chunk_list_add_tail(Free_Chunk_List *list, Free_Chunk *chunk)
{
  chunk->next = NULL;
  if(list->head) {
    list->tail->next = chunk;
    chunk->prev = list->tail;
    list->tail = chunk;
  } else {
    chunk->prev = NULL;
    list->head = list->tail = chunk;
  }
  list->chunk_num++;
}

inline void free_chunk_list_add_head(Free_Chunk_List *list, Free_Chunk *chunk)
{
  chunk->prev = NULL;
  if(list->head) {
    list->head->prev = chunk;
    chunk->next = list->head;
    list->head = chunk;
  } else {
    chunk->next = NULL;
    list->head = list->tail = chunk;
  }
  list->chunk_num++;
}


inline void free_list_detach_chunk(Free_Chunk_List *list, Free_Chunk *chunk)
{
  if(chunk->prev)
    chunk->prev->next = chunk->next;
  else  // chunk is the head
    list->head = chunk->next;
  
  if(chunk->next)
    chunk->next->prev = chunk->prev;
  else
    list->tail = chunk->prev;
  --list->chunk_num;
}

inline Boolean chunk_is_in_list(Free_Chunk_List *from_list, Free_Chunk *chunk)
{
  Free_Chunk *pro_chunk = from_list->head;
  while(pro_chunk) {
    if(pro_chunk == chunk)
	  return TRUE;
    pro_chunk = pro_chunk->next;
  }
  return FALSE; 
}


inline void move_free_chunks_between_lists(Free_Chunk_List *to_list, Free_Chunk_List *from_list)
{
  if(to_list->tail){
    to_list->head->prev = from_list->tail;
  } else {
    to_list->tail = from_list->tail;
  }
  if(from_list->head){
    from_list->tail->next = to_list->head;
    to_list->head = from_list->head;
  }
  //to_list->chunk_num += from_list->chunk_num;
  from_list->head = NULL;
  from_list->tail = NULL;
  from_list->chunk_num = 0;
}

/* Padding the last index word in table to facilitate allocation */
inline void chunk_pad_last_index_word(Chunk_Header *chunk, POINTER_SIZE_INT alloc_mask)
{
  unsigned int ceiling_index_in_last_word = (chunk->slot_num * COLOR_BITS_PER_OBJ) % BITS_PER_WORD;
  if(!ceiling_index_in_last_word)
    return;
  POINTER_SIZE_INT padding_mask = ~((1 << ceiling_index_in_last_word) - 1);
  padding_mask &= alloc_mask;
  unsigned int last_word_index = (chunk->slot_num-1) / SLOT_NUM_PER_WORD_IN_TABLE;
  chunk->table[last_word_index] |= padding_mask;
}

inline void chunk_pad_last_index_word_concurrent(Chunk_Header *chunk, POINTER_SIZE_INT alloc_mask)
{
  unsigned int ceiling_index_in_last_word = (chunk->slot_num * COLOR_BITS_PER_OBJ) % BITS_PER_WORD;
  if(!ceiling_index_in_last_word)
    return;
  POINTER_SIZE_INT padding_mask = ~((1 << ceiling_index_in_last_word) - 1);
  padding_mask &= alloc_mask;
  unsigned int last_word_index = (chunk->slot_num-1) / SLOT_NUM_PER_WORD_IN_TABLE;
  POINTER_SIZE_INT old_word = chunk->table[last_word_index];
  POINTER_SIZE_INT new_word = old_word | padding_mask;
  while (old_word == new_word){
    POINTER_SIZE_INT temp = (POINTER_SIZE_INT)atomic_casptr((volatile void **) &chunk->table[last_word_index],(void*)new_word ,(void*)old_word );
    if(temp == old_word)
      return;
    old_word = chunk->table[last_word_index];
    new_word = old_word | padding_mask;
  }
}



/* Depadding the last index word in table to facilitate allocation */
inline void chunk_depad_last_index_word(Chunk_Header *chunk)
{
  unsigned int ceiling_index_in_last_word = (chunk->slot_num * COLOR_BITS_PER_OBJ) % BITS_PER_WORD;
  if(!ceiling_index_in_last_word)
    return;
  POINTER_SIZE_INT depadding_mask = (1 << ceiling_index_in_last_word) - 1;
  unsigned int last_word_index = (chunk->slot_num-1) / SLOT_NUM_PER_WORD_IN_TABLE;
  chunk->table[last_word_index] &= depadding_mask;
}

extern volatile POINTER_SIZE_INT cur_alloc_mask;
/* Used for allocating a fixed-size chunk from free area lists */
inline void normal_chunk_init(Chunk_Header *chunk, unsigned int slot_size)
{
  //Modified this assertion for concurrent sweep
  //assert(chunk->status == CHUNK_FREE);
  assert(chunk->status & CHUNK_FREE);
  assert(CHUNK_SIZE(chunk) == NORMAL_CHUNK_SIZE_BYTES);
  
  chunk->next = NULL;
  chunk->status = CHUNK_FRESH | CHUNK_NORMAL | CHUNK_NEED_ZEROING;
  chunk->slot_size = slot_size;
  chunk->slot_num = NORMAL_CHUNK_SLOT_NUM(chunk);
  chunk->slot_index = 0;
  chunk->alloc_num = 0;
  chunk->base = NORMAL_CHUNK_BASE(chunk);
#ifdef USE_32BITS_HASHCODE
  chunk->hashcode_buf = NULL;
#endif
  memset(chunk->table, 0, NORMAL_CHUNK_TABLE_SIZE_BYTES(chunk));//memset table
  //chunk_pad_last_index_word(chunk, cur_alloc_mask);
  fastdiv_init();
}

/* Used for allocating a chunk for large object from free area lists */
inline void abnormal_chunk_init(Chunk_Header *chunk, unsigned int chunk_size, unsigned int obj_size)
{
  //Modified this assertion for concurrent sweep
  //assert(chunk->status == CHUNK_FREE);
  assert(chunk->status & CHUNK_FREE);
  assert(CHUNK_SIZE(chunk) == chunk_size);
  
  chunk->next = NULL;
  chunk->status = CHUNK_ABNORMAL;
  chunk->slot_size = obj_size;
  chunk->slot_num = 1;
  chunk->slot_index = 0;
  chunk->base = ABNORMAL_CHUNK_BASE(chunk);
#ifdef USE_32BITS_HASHCODE
  chunk->hashcode_buf = NULL;
#endif
}


#ifdef POINTER64
  #define GC_OBJECT_ALIGNMENT_BITS    3
#else
  #define GC_OBJECT_ALIGNMENT_BITS    2
#endif

#define MEDIUM_OBJ_THRESHOLD  (128)
#define LARGE_OBJ_THRESHOLD   (256)
#define SUPER_OBJ_THRESHOLD   (1024)
#define HYPER_OBJ_THRESHOLD   (128*KB)

#define SMALL_GRANULARITY_BITS  (GC_OBJECT_ALIGNMENT_BITS)
#define MEDIUM_GRANULARITY_BITS (SMALL_GRANULARITY_BITS + 1)
#define LARGE_GRANULARITY_BITS  7
#define CHUNK_GRANULARITY_BITS  10

#define CHUNK_GRANULARITY       (1 << CHUNK_GRANULARITY_BITS)
#define CHUNK_GRANULARITY_LOW_MASK    ((POINTER_SIZE_INT)(CHUNK_GRANULARITY-1))
#define CHUNK_GRANULARITY_HIGH_MASK   (~CHUNK_GRANULARITY_LOW_MASK)

#define SMALL_IS_LOCAL_ALLOC   TRUE
#define MEDIUM_IS_LOCAL_ALLOC  TRUE
#define LARGE_IS_LOCAL_ALLOC  FALSE

#define NORMAL_SIZE_ROUNDUP(size, seg)  (((size) + seg->granularity-1) & seg->gran_high_mask)
#define SUPER_OBJ_TOTAL_SIZE(size)  (sizeof(Chunk_Header) + (size))
#define SUPER_SIZE_ROUNDUP(size)    ((SUPER_OBJ_TOTAL_SIZE(size) + CHUNK_GRANULARITY-1) & CHUNK_GRANULARITY_HIGH_MASK)

#define NORMAL_SIZE_TO_INDEX(size, seg) ((((size)-(seg)->size_min) >> (seg)->gran_shift_bits) - 1)
#define ALIGNED_CHUNK_SIZE_TO_INDEX(size)     (((size) >> NORMAL_CHUNK_SHIFT_COUNT) - 1)
#define UNALIGNED_CHUNK_SIZE_TO_INDEX(size)   (((size) >> CHUNK_GRANULARITY_BITS) - 1)

#define NORMAL_INDEX_TO_SIZE(index, seg)  ((((index) + 1) << (seg)->gran_shift_bits) + (seg)->size_min)
#define ALIGNED_CHUNK_INDEX_TO_SIZE(index)    (((index) + 1) << NORMAL_CHUNK_SHIFT_COUNT)
#define UNALIGNED_CHUNK_INDEX_TO_SIZE(index)  (((index) + 1) << CHUNK_GRANULARITY_BITS)

#define SUPER_OBJ_MASK ((Obj_Info_Type)0x20)  /* the 4th bit in obj info */

//#define WSPACE_CONCURRENT_GC_STATS

#ifdef WSPACE_CONCURRENT_GC_STATS
#define NEW_OBJ_MASK ((Obj_Info_Type)0x40)
#endif

#define PFC_STEAL_NUM   3
#define PFC_STEAL_THRESHOLD   3


#define SIZE_SEGMENT_NUM  3

typedef struct Size_Segment {
  unsigned int size_min;
  unsigned int size_max;
  unsigned int seg_index;
  Boolean local_alloc;
  unsigned int chunk_num;
  unsigned int gran_shift_bits;
  POINTER_SIZE_INT granularity;
  POINTER_SIZE_INT gran_low_mask;
  POINTER_SIZE_INT gran_high_mask;
} Size_Segment;

inline Size_Segment *wspace_get_size_seg(Wspace *wspace, unsigned int size)
{
  Size_Segment **size_segs = wspace->size_segments;
  
  unsigned int seg_index = 0;
  for(; seg_index < SIZE_SEGMENT_NUM; ++seg_index)
    if(size <= size_segs[seg_index]->size_max) break;
  assert(seg_index < SIZE_SEGMENT_NUM);
  assert(size_segs[seg_index]->seg_index == seg_index);
  return size_segs[seg_index];
}

inline Chunk_Header *wspace_get_pfc(Wspace *wspace, unsigned int seg_index, unsigned int index)
{
  /*1. Search PFC pool*/
  Pool *pfc_pool = wspace->pfc_pools[seg_index][index];
  Chunk_Header *chunk = (Chunk_Header*)pool_get_entry(pfc_pool);

  /*2. If in concurrent sweeping phase, search PFC backup pool*/
  if(!chunk && in_con_sweeping_phase(wspace->gc)){
    pfc_pool = wspace->pfc_pools_backup[seg_index][index];
    chunk = (Chunk_Header*)pool_get_entry(pfc_pool);
  }
  assert(!chunk || chunk->status == (CHUNK_NORMAL | CHUNK_NEED_ZEROING));
  return chunk;
}

inline void wspace_put_pfc(Wspace *wspace, Chunk_Header *chunk)
{
  unsigned int size = chunk->slot_size;
  assert(chunk && (size <= SUPER_OBJ_THRESHOLD));
  assert(chunk->base && chunk->alloc_num);
  assert(chunk->alloc_num < chunk->slot_num);
  assert(chunk->slot_index < chunk->slot_num);
  
  Size_Segment **size_segs = wspace->size_segments;
  chunk->status = CHUNK_NORMAL | CHUNK_NEED_ZEROING;
  
  for(unsigned int i = 0; i < SIZE_SEGMENT_NUM; ++i){
    if(size > size_segs[i]->size_max) continue;
    assert(!(size & size_segs[i]->gran_low_mask));
    assert(size > size_segs[i]->size_min);
    unsigned int index = NORMAL_SIZE_TO_INDEX(size, size_segs[i]);
    Pool *pfc_pool;
    pfc_pool = wspace->pfc_pools[i][index];
    pool_put_entry(pfc_pool, chunk);
    return;
  }
}

inline void wspace_put_pfc_backup(Wspace *wspace, Chunk_Header *chunk)
{
  unsigned int size = chunk->slot_size;
  assert(chunk && (size <= SUPER_OBJ_THRESHOLD));
  assert(chunk->base && chunk->alloc_num);
  assert(chunk->alloc_num < chunk->slot_num);
  assert(chunk->slot_index < chunk->slot_num);
  
  Size_Segment **size_segs = wspace->size_segments;
  chunk->status = CHUNK_NORMAL | CHUNK_NEED_ZEROING;
  
  for(unsigned int i = 0; i < SIZE_SEGMENT_NUM; ++i){
    if(size > size_segs[i]->size_max) continue;
    assert(!(size & size_segs[i]->gran_low_mask));
    assert(size > size_segs[i]->size_min);
    unsigned int index = NORMAL_SIZE_TO_INDEX(size, size_segs[i]);
    //FIXME: concurrent sweep
    Pool *pfc_pool;
    pfc_pool = wspace->pfc_pools_backup[i][index];
    pool_put_entry(pfc_pool, chunk);
    return;
  }
}


/*Function for concurrent sweeping. In concurrent sweeping, PFC chunks are put back to pfc_pools_backup
    instead of pfc_pools. */
inline void wspace_backup_pfc(Wspace *wspace, Chunk_Header *chunk)
{
  unsigned int size = chunk->slot_size;
  assert(chunk->base && chunk->alloc_num);
  assert(chunk && (size <= SUPER_OBJ_THRESHOLD));
  assert(chunk->slot_index < chunk->slot_num);
  
  Size_Segment **size_segs = wspace->size_segments;
  chunk->status = CHUNK_NORMAL | CHUNK_NEED_ZEROING;
  
  for(unsigned int i = 0; i < SIZE_SEGMENT_NUM; ++i){
    if(size > size_segs[i]->size_max) continue;
    assert(!(size & size_segs[i]->gran_low_mask));
    assert(size > size_segs[i]->size_min);
    unsigned int index = NORMAL_SIZE_TO_INDEX(size, size_segs[i]);
    Pool *pfc_pool = wspace->pfc_pools_backup[i][index];
    pool_put_entry(pfc_pool, chunk);
    return;
  }
}

inline void wspace_rebuild_chunk_chain(Wspace *wspace)
{
  Chunk_Header_Basic *wspace_ceiling = (Chunk_Header_Basic*)space_heap_end((Space*)wspace);
  Chunk_Header_Basic *prev_chunk = (Chunk_Header_Basic*)space_heap_start((Space*)wspace);
  Chunk_Header_Basic *chunk = prev_chunk->adj_next;
  prev_chunk->adj_prev = NULL;
  
  while(chunk < wspace_ceiling){
    chunk->adj_prev = prev_chunk;
    prev_chunk = chunk;
    chunk = chunk->adj_next;
  }
}

inline Chunk_Header_Basic* chunk_pool_get_chunk(Pool* chunk_pool)
{
  return (Chunk_Header_Basic*)pool_get_entry(chunk_pool);
}

inline void chunk_pool_put_chunk(Pool* chunk_pool, Chunk_Header* chunk)
{
  pool_put_entry(chunk_pool,chunk);
  return;
}

inline void wspace_reg_used_chunk(Wspace* wspace, Chunk_Header* chunk)
{
  pool_put_entry(wspace->used_chunk_pool, chunk);
  return;
}

inline void wspace_clear_used_chunk_pool(Wspace* wspace)
{
  pool_empty(wspace->used_chunk_pool);
  return;
}

inline void wspace_reg_unreusable_normal_chunk(Wspace* wspace, Chunk_Header* chunk)
{
  pool_put_entry(wspace->unreusable_normal_chunk_pool, chunk);
  return;
}

inline Chunk_Header*  wspace_get_unreusable_normal_chunk(Wspace* wspace)
{
  return (Chunk_Header*)pool_get_entry(wspace->unreusable_normal_chunk_pool);
}

inline void wspace_reg_live_abnormal_chunk(Wspace* wspace, Chunk_Header* chunk)
{
  pool_put_entry(wspace->live_abnormal_chunk_pool, chunk);
  return;
}

inline Chunk_Header* wspace_get_live_abnormal_chunk(Wspace* wspace)
{
  return (Chunk_Header*)pool_get_entry(wspace->live_abnormal_chunk_pool);
}

extern void wspace_init_chunks(Wspace *wspace);
extern void wspace_clear_chunk_list(Wspace* wspace);

extern void wspace_put_free_chunk(Wspace *wspace, Free_Chunk *chunk);
void wspace_put_free_chunk_to_head(Wspace *wspace, Free_Chunk *chunk);
void wspace_put_free_chunk_to_tail(Wspace *wspace, Free_Chunk *chunk);

extern Free_Chunk *wspace_get_normal_free_chunk(Wspace *wspace);
extern Free_Chunk *wspace_get_abnormal_free_chunk(Wspace *wspace, unsigned int chunk_size);
extern Free_Chunk *wspace_get_hyper_free_chunk(Wspace *wspace, unsigned int chunk_size, Boolean is_normal_chunk);

extern void wspace_init_pfc_pool_iterator(Wspace *wspace);
extern Pool *wspace_grab_next_pfc_pool(Wspace *wspace);
void wspace_exchange_pfc_pool(Wspace *wspace);

extern Chunk_Header *wspace_steal_pfc(Wspace *wspace, unsigned int index);

extern POINTER_SIZE_INT free_mem_in_wspace(Wspace *wspace, Boolean show_chunk_info);

extern void zeroing_free_chunk(Free_Chunk *chunk);

extern void gc_clear_collector_local_chunks(GC *gc);

extern void wspace_collect_free_chunks_to_list(Wspace *wspace, Free_Chunk_List *list);

#ifdef USE_32BITS_HASHCODE  
inline int obj_lookup_hashcode_in_chunk_buf(Partial_Reveal_Object *p_obj)
{
  Hashcode_Buf* hashcode_buf = NORMAL_CHUNK_HEADER(p_obj)->hashcode_buf;
  return hashcode_buf_lookup(p_obj,hashcode_buf);
}
#endif

#endif //#ifndef _SSPACE_CHUNK_H_
