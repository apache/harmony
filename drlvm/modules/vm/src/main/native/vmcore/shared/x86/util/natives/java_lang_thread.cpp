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
 * @author Intel, Evgueni Brevnov
 */  

#include <assert.h>

//MVM
#include <iostream>

using namespace std;


#ifndef PLATFORM_POSIX
#include "vm_process.h"
#endif

#include "environment.h"
#include "open/types.h"
#include "open/vm_util.h"
#include "object_layout.h"
#include "Class.h"
#include "vm_threads.h"

#include "thread_generic.h"

void setup_floating_point_state(int *p_old_floating_point_state)
{
    int old_floating_point_state = 0;
#ifdef PLATFORM_NT
    int floating_point_temp = 0;
    __asm {
        // the below set the significand to 53 bits for doubles
        // this is "non-strict" mode
        // you should call cleanup_floating_point_state (see below)
        // before entering Java Code
        fnstcw WORD PTR [old_floating_point_state]
        fnstcw WORD PTR [floating_point_temp]
        and DWORD PTR[floating_point_temp], 0feffH
        or DWORD PTR [floating_point_temp], 0200H
        fldcw WORD PTR [floating_point_temp] 
    }
#endif
    *p_old_floating_point_state = old_floating_point_state;
}


void cleanup_floating_point_state(int UNREF old_floating_point_state)
{
#ifdef PLATFORM_NT
    // the below restores floating point state to "strict" mode
     __asm {
        fldcw WORD PTR [old_floating_point_state] 
    }
#endif
}
