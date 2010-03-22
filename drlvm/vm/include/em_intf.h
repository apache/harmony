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
 * @author Mikhail Y. Fursov
 */  
#ifndef _EM_IMPORT_H
#define _EM_IMPORT_H

#include "open/types.h"
#include "open/em.h"
#include <apr_dso.h>

#ifdef __cplusplus
extern "C" {
#endif

VMEXPORT JIT_Handle vm_load_jit(const char* file_name, apr_dso_handle_t** handle);

#ifdef __cplusplus
}
#endif

#endif // _EM_IMPORT_H
