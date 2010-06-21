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

#include "../gen/gen.h"

static void blocked_space_clear_info(Blocked_Space* space)
{  
  for(unsigned int i=0; i < space->num_managed_blocks; i++){
    Block_Header* block = (Block_Header*)&(space->blocks[i]);
    block->num_live_objs = 0;
  }
  return;
}

void verifier_cleanup_block_info(GC* gc)
{
  Blocked_Space* space = (Blocked_Space*)gc_get_mos((GC_Gen*)gc);
  blocked_space_clear_info(space);  

  space = (Blocked_Space*)gc_get_nos((GC_Gen*)gc);
  blocked_space_clear_info(space);  

  return;
}
