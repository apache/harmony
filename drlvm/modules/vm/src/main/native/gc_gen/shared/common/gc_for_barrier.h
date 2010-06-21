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

#ifndef _GC_FOR_BARRIER_H_
#define _GC_FOR_BARRIER_H_

#include "../jni/java_support.h"

extern volatile unsigned int write_barrier_function;

enum Write_Barrier_Function{
  WB_REM_NIL           = 0x00,
  WB_REM_SOURCE_OBJ    = 0x01,
  WB_REM_SOURCE_REF    = 0x02,
  WB_REM_OLD_VAR       = 0x03,
  WB_REM_NEW_VAR       = 0x04,
  WB_REM_OBJ_SNAPSHOT  = 0x05,
  WB_CON_DEBUG = 0x06
};

inline void gc_set_barrier_function(unsigned int wb_function)
{
  write_barrier_function = wb_function;
}

#endif /* _GC_FOR_BARRIER_H_ */


