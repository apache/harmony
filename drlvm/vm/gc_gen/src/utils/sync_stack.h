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
 
#ifndef _SYNC_STACK_H_
#define _SYNC_STACK_H_

#include "vector_block.h"

#define SYNC_STACK_VERSION_MASK_SHIFT 10
#define SYNC_STACK_VERSION_MASK ((1 << SYNC_STACK_VERSION_MASK_SHIFT) - 1)

typedef struct Node{
  Node* next;  
}Node;

/*
 * ATTENTION: only for reference
 * Perhaps in some platforms compilers compile this struct in a way different from what we expect
 * GCC requires to specify "packed" attribute
#ifdef __linux__
typedef struct Stack_Top{
  POINTER_SIZE_INT version: SYNC_STACK_VERSION_MASK_SHIFT;
  POINTER_SIZE_INT entry: (BITS_OF_POINTER_SIZE_INT-SYNC_STACK_VERSION_MASK_SHIFT);
}Stack_Top __attribute__((packed));
#else
typedef struct Stack_Top{
  POINTER_SIZE_INT version: SYNC_STACK_VERSION_MASK_SHIFT;
  POINTER_SIZE_INT entry: (BITS_OF_POINTER_SIZE_INT-SYNC_STACK_VERSION_MASK_SHIFT);
}Stack_Top;
#endif
 */

typedef POINTER_SIZE_INT Stack_Top;

typedef struct Sync_Stack{
  Stack_Top top; /* pointing to the first filled entry */
  Node* cur; /* pointing to the current accessed entry, only for iterator */
}Sync_Stack;

#define stack_top_get_entry(top) ((Node*)((*(POINTER_SIZE_INT*)&(top)) & ~SYNC_STACK_VERSION_MASK))
/* The alternative way: (Node*)(top.entry<<SYNC_STACK_VERSION_MASK_SHIFT) */
#define stack_top_get_version(top) ((*(POINTER_SIZE_INT*)&(top)) & SYNC_STACK_VERSION_MASK)
/* The alternative way: (top.version) */
#define stack_top_contruct(entry, version) ((POINTER_SIZE_INT)(entry) | (version))
#define stack_top_get_next_version(top) ((stack_top_get_version(top) + 1) & SYNC_STACK_VERSION_MASK)

inline Sync_Stack* sync_stack_init()
{
  unsigned int size = sizeof(Sync_Stack);
  Sync_Stack* stack = (Sync_Stack*)STD_MALLOC(size);
  memset(stack, 0, size);
  stack->cur = NULL;
  POINTER_SIZE_INT temp_top = 0;
  stack->top = *(Stack_Top*)&temp_top;
  return stack;
}

inline void sync_stack_destruct(Sync_Stack* stack)
{
  STD_FREE(stack);
  return;  
}

inline void sync_stack_iterate_init(Sync_Stack* stack)
{
  stack->cur = stack_top_get_entry(stack->top);
  return;
}

inline Node* sync_stack_iterate_next(Sync_Stack* stack)
{
  Node* entry = stack->cur;
  while ( entry != NULL ){
    Node* new_entry = entry->next;
    Node* temp = (Node*)atomic_casptr((volatile void**)&stack->cur, new_entry, entry);
    if(temp == entry){ /* got it */  
      return entry;
    }
    entry = stack->cur;
  }
  return NULL;
}

inline Node* sync_stack_pop(Sync_Stack* stack)
{
  Stack_Top cur_top = stack->top;
  Node* top_entry = stack_top_get_entry(cur_top);
  POINTER_SIZE_INT version = stack_top_get_version(cur_top);
  
  while( top_entry != NULL ){
    POINTER_SIZE_INT temp = stack_top_contruct(top_entry->next, version);
    temp = (POINTER_SIZE_INT)atomic_casptr((volatile void**)&stack->top, (void*)temp, (void*)cur_top);
    if(temp == *(POINTER_SIZE_INT*)&cur_top){ // got it  
      top_entry->next = NULL;
      return top_entry;
    }
    cur_top = stack->top;
    top_entry = stack_top_get_entry(cur_top);
    version = stack_top_get_version(cur_top);
  }  
  return 0;
}

inline Boolean sync_stack_push(Sync_Stack* stack, Node* node)
{
  Stack_Top cur_top = stack->top;
  node->next = stack_top_get_entry(cur_top);
  POINTER_SIZE_INT new_version = stack_top_get_next_version(cur_top);
  POINTER_SIZE_INT temp = stack_top_contruct(node, new_version);
 
  while( TRUE ){
    temp = (POINTER_SIZE_INT)atomic_casptr((volatile void**)&stack->top, (void*)temp, (void*)cur_top);
    if(temp == *(POINTER_SIZE_INT*)&cur_top){ // got it   
      return TRUE;
    }
    cur_top = stack->top;
    node->next = stack_top_get_entry(cur_top);
    new_version = stack_top_get_next_version(cur_top);
    temp = stack_top_contruct(node, new_version);
  }
  // never comes here 
  return FALSE;
}

/* it does not matter whether this is atomic or not, because
   it is only invoked when there is no contention or only for rough idea */
inline Boolean sync_stack_is_empty(Sync_Stack* stack)
{
  return (stack_top_get_entry(stack->top) == NULL);
}

inline void sync_stack_empty(Sync_Stack* stack)
{
  stack->top = (Stack_Top)NULL;
  stack->cur = NULL;
}

inline unsigned int sync_stack_size(Sync_Stack* stack)
{
  unsigned int entry_count = 0;
  
  sync_stack_iterate_init(stack);
  while(sync_stack_iterate_next(stack)){
    ++entry_count;
  }

  return entry_count;
}

#endif /* _SYNC_STACK_H_ */
