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

#include "gc_common.h"
#include "collection_scheduler.h"
#include "concurrent_collection_scheduler.h"
#include "gc_concurrent.h"

void collection_scheduler_initialize(GC* gc)
{
  if(gc_is_specify_con_gc()) con_collection_scheduler_initialize(gc);
  return;
}
void collection_scheduler_destruct(GC* gc)
{
  if(gc_is_specify_con_gc()) con_collection_scheduler_destruct(gc);
  return;
}


Boolean gc_sched_collection(GC* gc, unsigned int gc_cause)
{
  /*collection scheduler only schedules concurrent collection now.*/
  if(GC_CAUSE_CONCURRENT_GC == gc_cause){
    assert(gc_is_specify_con_gc());
    return gc_con_perform_collection( gc );
  }else{
    return FALSE;
  }
}


