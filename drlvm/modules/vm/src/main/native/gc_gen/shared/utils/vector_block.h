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
 
#ifndef _VECTOR_BLOCK_H_
#define _VECTOR_BLOCK_H_

typedef struct Vector_Block{
  void* next; /* point to next block */
  POINTER_SIZE_INT* head;  /* point to the first filled entry */
  POINTER_SIZE_INT* tail;  /* point to the entry after the last filled one */
  POINTER_SIZE_INT* heap_end;   /* point to heap_end of the block (right after the last entry) */
  POINTER_SIZE_INT entries[1];
}Vector_Block;


/* this size better be 2's power */
#define VECTOR_BLOCK_DATA_SIZE_BYTES (2*KB)

#define VECTOR_BLOCK_HEADER_SIZE_BYTES ((POINTER_SIZE_INT)((Vector_Block*)0)->entries)
#define VECTOR_BLOCK_ENTRY_NUM ((VECTOR_BLOCK_DATA_SIZE_BYTES - VECTOR_BLOCK_HEADER_SIZE_BYTES) >> BIT_SHIFT_TO_BYTES_OF_POINTER_SIZE_INT )
#define VECTOR_BLOCK_LOW_MASK ((POINTER_SIZE_INT)(VECTOR_BLOCK_DATA_SIZE_BYTES - 1))
#define VECTOR_BLOCK_HIGH_MASK (~VECTOR_BLOCK_LOW_MASK)
#define VECTOR_BLOCK_HEADER(addr) ((Vector_Block *)((POINTER_SIZE_INT)(addr) & VECTOR_BLOCK_HIGH_MASK))

inline void vector_block_init(Vector_Block* block, unsigned int size)
{
  block->heap_end = (POINTER_SIZE_INT*)((POINTER_SIZE_INT)block + size);
  block->head = (POINTER_SIZE_INT*)block->entries;
  block->tail = (POINTER_SIZE_INT*)block->entries;
  memset(block->entries, 0, (POINTER_SIZE_INT)block->heap_end - (POINTER_SIZE_INT)block->entries);
  return;  
}

inline unsigned int vector_block_entry_count(Vector_Block* block)
{ return (unsigned int)(block->tail - block->head); }

inline Boolean vector_block_is_full(Vector_Block* block)
{ return block->tail == block->heap_end; }

/*
inline Boolean vector_block_is_empty(Vector_Block* block)
{ return block->tail == block->head; }

inline Boolean vector_block_is_full(Vector_Block* block)
{ return (block->tail - block->entries) == VECTOR_BLOCK_ENTRY_NUM; }
*/

inline Boolean vector_block_is_empty(Vector_Block* block)
{ return block->tail == block->head; }

inline void vector_block_add_entry(Vector_Block* block, POINTER_SIZE_INT value)
{
#ifdef _DEBUG 
  assert(value && !*(block->tail));
#endif

  *(block->tail++) = value; 
}

inline POINTER_SIZE_INT vector_block_get_entry(Vector_Block* block)
{
#ifdef _DEBUG
  assert(!vector_block_is_empty(block));
#endif

  POINTER_SIZE_INT value = *(block->head++);

#ifdef _DEBUG
  assert(value);
#endif
  return value;
}
inline void vector_block_set_at_index(Vector_Block* block, unsigned int index, POINTER_SIZE_INT value)
{
#ifdef _DEBUG 
  assert(index < VECTOR_BLOCK_ENTRY_NUM);
#endif
  block->entries[index] = value;
}
inline POINTER_SIZE_INT vector_block_get_at_index(Vector_Block* block, unsigned int index)
{
#ifdef _DEBUG 
  assert(index < VECTOR_BLOCK_ENTRY_NUM);
#endif
  return block->entries[index];
}

#define VECTOR_BLOCK_SHARE_BIT 0x01
#define VECTOR_BLOCK_FULL_BIT  0x02
#define VECTOR_BLOCK_SHARE_ID_SHIFT 0x05
#define VECTOR_BLOCK_EXCLUSIVE_ID 0xFFff
inline Boolean vector_block_is_shared(Vector_Block* block)
{
  return (Boolean)((POINTER_SIZE_INT)block->next & VECTOR_BLOCK_SHARE_BIT);
}

inline Boolean vector_block_is_set_full(Vector_Block* block)
{
  return (Boolean)((POINTER_SIZE_INT)block->next & VECTOR_BLOCK_FULL_BIT);
}

inline Boolean vector_block_set_full(Vector_Block* block)
{
  POINTER_SIZE_INT old_shared_var = (POINTER_SIZE_INT) block->next;
  POINTER_SIZE_INT new_shared_var = old_shared_var | VECTOR_BLOCK_FULL_BIT;

  while(TRUE){
    POINTER_SIZE_INT old_var = (POINTER_SIZE_INT)atomic_casptr((volatile void **)& block->next ,(void*) new_shared_var,(void*) old_shared_var);
    if(old_var == old_shared_var) return TRUE;
    old_shared_var = (POINTER_SIZE_INT) block->next;
    new_shared_var = old_shared_var | VECTOR_BLOCK_FULL_BIT;
  }
  assert(0);
  return FALSE;

}

inline Boolean vector_block_set_shared(Vector_Block* block, unsigned int share_id)
{
  POINTER_SIZE_INT new_shared_var = (POINTER_SIZE_INT)((share_id << VECTOR_BLOCK_SHARE_ID_SHIFT) | VECTOR_BLOCK_SHARE_BIT);
  POINTER_SIZE_INT old_shared_var = (POINTER_SIZE_INT) block->next;
  if(old_shared_var != 0) return FALSE;

  while(TRUE){
    POINTER_SIZE_INT old_var = (POINTER_SIZE_INT)atomic_casptr((volatile void **)& block->next ,(void*) new_shared_var,(void*) old_shared_var);
    if(old_var == old_shared_var) return TRUE;
    old_shared_var = (POINTER_SIZE_INT) block->next;
    if(old_shared_var != 0) return FALSE;
  }    
}

inline Boolean vector_block_not_full_set_unshared(Vector_Block* block)
{
  POINTER_SIZE_INT new_shared_var = (POINTER_SIZE_INT) 0;
  POINTER_SIZE_INT old_shared_var = (POINTER_SIZE_INT) block->next;

  if(old_shared_var & VECTOR_BLOCK_FULL_BIT) return FALSE;

  while(TRUE){
    POINTER_SIZE_INT old_var = (POINTER_SIZE_INT)atomic_casptr((volatile void **)& block->next ,(void*) new_shared_var,(void*) old_shared_var);
    if(old_var == old_shared_var) return TRUE;
    old_shared_var = (POINTER_SIZE_INT) block->next;
    if(old_shared_var & VECTOR_BLOCK_FULL_BIT) return FALSE;    
  }
  assert(0);
  return FALSE;
}

inline Boolean vector_block_set_exclusive(Vector_Block* block)
{
  POINTER_SIZE_INT new_shared_var = (POINTER_SIZE_INT)((VECTOR_BLOCK_EXCLUSIVE_ID << VECTOR_BLOCK_SHARE_ID_SHIFT) | VECTOR_BLOCK_SHARE_BIT);
  POINTER_SIZE_INT old_shared_var = (POINTER_SIZE_INT) block->next;
  if(old_shared_var & VECTOR_BLOCK_SHARE_BIT) return FALSE;

  while(TRUE){
    POINTER_SIZE_INT old_var = (POINTER_SIZE_INT)atomic_casptr((volatile void **)& block->next ,(void*) new_shared_var,(void*) old_shared_var);
    if(old_var == old_shared_var) return TRUE;
    old_shared_var = (POINTER_SIZE_INT) block->next;
    if(old_shared_var & VECTOR_BLOCK_SHARE_BIT) return FALSE;
  }
}

inline void vector_block_clear(Vector_Block* block)
{
  block->head = (POINTER_SIZE_INT*)block->entries;
  block->tail = (POINTER_SIZE_INT*)block->entries;
#ifdef _DEBUG
  memset(block->entries, 0, (POINTER_SIZE_INT)block->heap_end - (POINTER_SIZE_INT)block->entries);
#endif
}

inline void vector_block_set_zero(Vector_Block* block)
{
  block->head = (POINTER_SIZE_INT*)block->entries;
  block->tail = (POINTER_SIZE_INT*)block->entries;
  memset(block->entries, 0, (POINTER_SIZE_INT)block->heap_end - (POINTER_SIZE_INT)block->entries);
}

/* Below is for sequential local access */
inline POINTER_SIZE_INT* vector_block_iterator_init(Vector_Block* block)
{  return block->head;  }

inline POINTER_SIZE_INT* vector_block_iterator_advance(Vector_Block* block, POINTER_SIZE_INT* iter)
{  return ++iter; }

inline Boolean vector_block_iterator_end(Vector_Block* block, POINTER_SIZE_INT* iter)
{  return iter == block->tail; }

inline POINTER_SIZE_INT* vector_block_get_last_entry(Vector_Block* block)
{ return block->tail; }

/* Below is to use Vector_Block as stack (for trace-forwarding DFS order ) */
inline void vector_stack_init(Vector_Block* block)
{ 
  block->tail = block->heap_end;
  block->head = block->heap_end;  
}

inline void vector_stack_clear(Vector_Block* block)
{
  vector_stack_init(block);
#ifdef _DEBUG
  memset(block->entries, 0, (POINTER_SIZE_INT)block->heap_end - (POINTER_SIZE_INT)block->entries);
#endif
}

inline Boolean vector_stack_is_empty(Vector_Block* block)
{  return (block->head == block->tail); }

/*
inline Boolean vector_stack_is_empty(Vector_Block* block)
{ return (block->head - block->entries) == VECTOR_BLOCK_ENTRY_NUM; }
*/

inline Boolean vector_stack_is_full(Vector_Block* block)
{  return (block->head == block->entries); }

inline void vector_stack_push(Vector_Block* block, POINTER_SIZE_INT value)
{ 
  block->head--;
#ifdef _DEBUG
  assert(value && !*(block->head));
#endif
  *(block->head) = value;
}

inline POINTER_SIZE_INT vector_stack_pop(Vector_Block* block)
{   
  POINTER_SIZE_INT value = *block->head;
#ifdef _DEBUG
  *block->head = 0;
#endif
  block->head++;
  return value;
}

inline POINTER_SIZE_INT vector_stack_read(Vector_Block* block, int idx)
{
	return block->head[idx];
}

inline void vector_block_integrity_check(Vector_Block* block)
{
  POINTER_SIZE_INT* iter = vector_block_iterator_init(block);
  while(!vector_block_iterator_end(block, iter)){
    assert(*iter);
    iter = vector_block_iterator_advance(block, iter);
  }    
  return;
}

#endif /* #ifndef _VECTOR_BLOCK_H_ */
