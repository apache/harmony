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
* @author Intel, Leviev Ilia
*/  

/**
* @file
* Provides explicit markers for a code region where potentially unsafe thread operations are performed,
* useful for thread analyzing tools. For example, they allows to ignore false alerts on hand-crafted
* synchronization mechanisms or "allowed" race conditions that are known/proven to not affect correctness of execution.
* Normally they do not affect behavior of code.
*/

#ifdef ITC
#include <libittnotify.h>
#else
#define __itt_obj_mode_set(prop, state)
#endif

/**
* Marks the beginning of code region where thread unsafe operations are performed.
* The unsafe area should be closed by UNSAFE_REGION_END.
*/
#define UNSAFE_REGION_START __itt_obj_mode_set(__itt_obj_prop_ignore, __itt_obj_state_set);

/**
* Marks the end of code region where thread unsafe operation are performed.
*/
#define UNSAFE_REGION_END __itt_obj_mode_set(__itt_obj_prop_ignore, __itt_obj_state_clr);

