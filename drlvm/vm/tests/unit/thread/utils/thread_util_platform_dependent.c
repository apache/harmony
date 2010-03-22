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

#include <assert.h>
#include "thread_unit_test_utils.h"

#ifdef _WIN32
#include <windows.h>

create_java_vm_func test_get_java_vm_ptr(void)
{
    HANDLE lib;
    FARPROC func;

    lib = LoadLibrary("harmonyvm.dll");
    assert(lib);
    func = GetProcAddress(lib, "JNI_CreateJavaVM");
    assert(func);

    return (create_java_vm_func)func;
}
#else
#include <dlfcn.h>

create_java_vm_func test_get_java_vm_ptr(void)
{
    void *lib, *func;

    lib = dlopen("libharmonyvm.so", RTLD_NOW);
    assert(lib);
    func = dlsym(lib, "JNI_CreateJavaVM");
    assert(func);

    return (create_java_vm_func)func;
}
#endif
