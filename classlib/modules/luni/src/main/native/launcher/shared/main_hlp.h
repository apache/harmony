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

#if !defined(MAINHLP_H)
#define MAINHLP_H
#if defined(__cplusplus)
extern "C"
{
#endif
#include "hycomp.h"

extern HY_CFUNC int main_get_executable_name PROTOTYPE((char *argv0, char **exeName));
extern HY_CFUNC void *main_mem_allocate_memory PROTOTYPE((int byteAmount));
extern HY_CFUNC void main_mem_free_memory PROTOTYPE((void *memoryPointer));
extern HY_CFUNC int main_open_port_library PROTOTYPE((UDATA * descriptor));
extern HY_CFUNC int main_close_port_library PROTOTYPE((UDATA descriptor));
extern HY_CFUNC UDATA main_lookup_name PROTOTYPE((UDATA descriptor, char *name, UDATA * func));

#if defined(__cplusplus)
}
#endif
#endif                          /* MAINHLP_H */
