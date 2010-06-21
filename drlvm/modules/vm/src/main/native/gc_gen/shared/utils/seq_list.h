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
 
#ifndef _SEQ_LIST_H_
#define _SEQ_LIST_H_

#include "vector_block.h"

typedef struct List_Node{
  List_Node* next;  
}List_Node;

typedef struct Seq_List{
  List_Node* head;
  List_Node* end;
  List_Node* curr;
#ifdef _DEBUG
  unsigned int node_num;
#endif
}Seq_List;

inline Seq_List* seq_list_create()
{
  unsigned int size = sizeof(Seq_List);
  Seq_List* seq_list = (Seq_List*)STD_MALLOC(size);
  memset(seq_list, 0, size);
  
  //List Head
  size = sizeof(List_Node);
  List_Node* lnode = (List_Node*)STD_MALLOC(size);
  seq_list->head = seq_list->end = lnode;
  lnode->next = lnode;

  return seq_list;
}

inline void seq_list_destruct(Seq_List* seq_list)
{ 
  STD_FREE(seq_list->head);
  STD_FREE(seq_list); 
}

inline Boolean seq_list_add(Seq_List* seq_list, List_Node* node)
{
#ifdef _DEBUG
  seq_list->node_num ++;
#endif
  seq_list->end ->next = node;
  seq_list->end = node;
  node->next = seq_list->head;
  return TRUE;
}

inline void seq_list_iterate_init(Seq_List* seq_list)
{
  seq_list->curr = seq_list->head->next;
}

inline void seq_list_iterate_init_after_node(Seq_List* seq_list, List_Node* begin)
{
  seq_list->curr = begin->next;
}

inline List_Node* seq_list_iterate_next(Seq_List* seq_list)
{
  if(seq_list->curr !=  seq_list->head){
    List_Node* ret_node = seq_list->curr; 
    seq_list->curr =seq_list->curr->next;
    return ret_node; 
  }
  return NULL;
}

inline Boolean seq_list_has_next(Seq_List* seq_list)
{
  return seq_list->curr != seq_list->head;
}

inline List_Node* seq_list_end_node(Seq_List* seq_list)
{ return seq_list->end; }

inline List_Node* seq_list_lookup_prev_node(Seq_List* seq_list, List_Node* node)
{
  List_Node* prev_node = seq_list->head;
  seq_list_iterate_init(seq_list);
  while(seq_list_has_next(seq_list)){
    List_Node* curr_node = seq_list_iterate_next(seq_list);
    if( node == curr_node ) return prev_node;
    prev_node = curr_node;
  }
  return NULL;
}

inline Boolean seq_list_remove(Seq_List* seq_list, List_Node* node)
{
  List_Node* prev_node = seq_list_lookup_prev_node(seq_list, node);
  if(prev_node==NULL) return FALSE; //need assertion here.
  prev_node->next = node->next;
#ifdef _DEBUG
  seq_list->node_num --;
#endif
  if(seq_list->end == node) seq_list->end = prev_node;
  return TRUE;
}

inline void seq_list_clear(Seq_List* seq_list)
{
  seq_list->end = seq_list->head;
  seq_list->curr = seq_list->head;
  List_Node* head = seq_list->head;
  head->next = seq_list->head;
#ifdef _DEBUG
  seq_list->node_num = 0;
#endif
}

#ifdef _DEBUG
inline unsigned int seq_list_size(Seq_List* seq_list)
{
  return seq_list->node_num;
}
#endif
#endif //_SEQ_LIST_H_
