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

//
// VM Internal Native Interface
//



#ifndef _INI_H
#define _INI_H

#include "open/em.h"
#include "jni_types.h"

#ifdef __cplusplus
extern "C" {
#endif

VMEXPORT void
JIT_execute_method_default(JIT_Handle jit, 
                           jmethodID meth,
                           jvalue    *return_value,
                           jvalue    *args);

VMEXPORT void
vm_execute_java_method_array(jmethodID method,
                             jvalue *result,
                             jvalue *args);

#ifdef __cplusplus
}
#endif

#endif /* _INI_H */
