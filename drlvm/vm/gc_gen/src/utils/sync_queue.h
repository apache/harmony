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

#ifndef _SYNC_QUEUE_H_
#define _SYNC_QUEUE_H_

/* an implementation of MSQ. FIXME:: only work in 32-bit machine */

struct Queue_Node;

typedef struct Queue_Link{
  struct Queue_Node* ptr;
  unsigned int count;
}Queue_Link;

typedef struct Queue_Node{
  __declspec(align(8))
  Queue_Link next; /* must be aligned to 8Byte*/
  unsigned int* value;
}Queue_Node;

typedef struct Sync_Queue{
  __declspec(align(8)) 
  Queue_Link head; /* must be aligned to 8Byte*/
  Queue_Link tail; 
}Sync_Queue;

inline Queue_Node * new_queue_node()
{
  Queue_Node* p_node = malloc(sizeof(Queue_Node));
  assert( (unsigned int)node%8 == 0 );
  return p_node;
}

inline void free_queue_node(Queue_Node* node)
{   free( node ); }

inline void sync_queue_init(Sync_Queue *queue)
{
  Queue_Node *node = new_queue_node();
  node->next.ptr = NULL;
  node->next.count = 0;
  queue->head.ptr = queue->tail.ptr = node;
  queue->head.count = queue->tail.count = 0;
  return;
}

#define QLINK_PTR(x) ((unsigned long long*)&(x))
#define QLINK_VAL(x) (*(QLINK_PTR(x)))

inline void sync_queue_push(Sync_Queue* queue, unsigned int* value)
{
  Queue_Link tail, next, tmp1, tmp2;
  Queue_Node* node = new_queue_node();
  node->value = value;
  node->next.ptr = NULL;
  while(TRUE){
    QLINK_VAL(tail) = QLINK_VAL(queue->tail);
    QLINK_VAL(next) = QLINK_VAL(tail.ptr->next);
    if( QLINK_VAL(tail) == QLINK_VAL(queue->tail)){
      if( next.ptr==NULL ){
        tmp1.ptr = node;
        tmp1.count = next.count + 1;
        node->next.count = tmp1.count; 
        QLINK_VAL(tmp2) = atomic_cas64(QLINK_PTR(tail.ptr->next), QLINK_VAL(next), QLINK_VAL(tmp1))
        if( QLINK_VAL(tmp1) == QLINK_VAL(tmp2))
          break;
      }else{
        tmp1.ptr = next.ptr;
        tmp1.count = tail.count + 1;
        atomic_cas64(QLINK_PTR(queue->tail), QLINK_VAL(tail), QLINK_VAL(tmp1));
      }
    }
  }
  tmp1.ptr = node;
  tmp1.count = tail.count + 1;
  atomic_cas64(QLINK_PTR(queue->tail), QLINK_VAL(tail), QLINK_VAL(tmp1));
  return;
}

Boolean sync_queue_pull(Sync_Queue* queue, unsigned int * pvalue)
{
  Queue_Link head, tail, next, tmp1, tmp2;
  while(TRUE){
    QLINK_VAL(head) = QLINK_VAL(queue->head);
    QLINK_VAL(tail) = QLINK_VAL(queue->tail);
    QLINK_VAL(next) = QLINK_VAL(head.ptr->next);

    if( QLINK_VAL(head) == QLINK_VAL(queue->head)){
      if( head.ptr== tail.ptr )
        if( next.ptr == NULL )
          return FALSE;
        else{
          tmp1.ptr = next.ptr;
          tmp1.count = tail.count+1;
          atomic_cas64(QLINK_PTR(queue->tail), QLINK_VAL(tail), QLINK_VAL(tmp1));
        }
      else{
        *pvalue = next.ptr->value;
        tmp1.ptr = next.ptr;
        tmp1.count = head.count+1;
        QLINK_VAL(tmp2) =  atomic_cas64(QLINK_PTR(queue->head), QLINK_VAL(head), QLINK_VAL(tmp1));
        if( QLINK_VAL(tmp2) == QLINK_VAL(tmp1))
          break;
      }
    }
  }
  free( head.ptr );
  return TRUE;
}
  
#endif /* _SYNC_QUEUE_H_ */
