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

#ifndef INTERIOR_POINTER_H
#define INTERIOR_POINTER_H 

#include "gc_common.h"

void add_root_set_entry_interior_pointer(void **slot, int offset, Boolean is_pinned);
void gc_copy_interior_pointer_table_to_rootset();
void update_rootset_interior_pointer();
void gc_reset_interior_pointer_table();

#endif //INTERIOR_POINTER_H
