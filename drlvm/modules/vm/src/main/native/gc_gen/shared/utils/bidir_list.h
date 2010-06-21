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
 * @author Ji Qi, 2006/10/05
 */

#ifndef _BIDIR_LIST_H_
#define _BIDIR_LIST_H_

typedef struct Bidir_List{
  unsigned int zero;
  Bidir_List* next;
  Bidir_List* prev;
}Bidir_List;

inline Bidir_List* bidir_list_add_item(Bidir_List* head, Bidir_List* item)
{
  item->next = head->next;
  item->prev = head;
  head->next->prev = item;
  head->next = item;
  return head;
}

inline Bidir_List* bidir_list_remove_item(Bidir_List* item)
{
  item->prev->next = item->next;
  item->next->prev = item->prev;
  item->next = item->prev = item;
  return item;
}

#endif /* _BIDIR_LIST_H_ */
