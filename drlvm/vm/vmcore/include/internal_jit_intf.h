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
 * @author Intel, Alexei Fedotov
 */  



#ifndef _INTERNAL_JIT_INTF_H_
#define _INTERNAL_JIT_INTF_H_

#include "open/types.h"
#include "open/rt_helpers.h"

NativeCodePtr rth_get_lil_helper(VM_RT_SUPPORT f);
VMEXPORT char *gen_setup_j2n_frame(char *s);
VMEXPORT char *gen_pop_j2n_frame(char *s);

#endif // _INTERNAL_JIT_INTF_H_

