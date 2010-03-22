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
 * @author Pavel Rebriy, Ilya Berezhniuk
 */  

/**
 * @file Describes the interface for native library processing 
 *       which is used by class support, kernal class natives and JNI.
 */

#ifndef _NATIVES_SUPPORT_LIB_H_
#define _NATIVES_SUPPORT_LIB_H_

#include <apr_dso.h>
#include "vm_core_types.h"
#include "String_Pool.h"
#include "jni_types.h"

/**
 * Native library handle typedef declaration.
 */
typedef struct apr_dso_handle_t * NativeLibraryHandle;

/**
 * Native library status typedef declaration.
 */
typedef apr_status_t NativeLoadStatus;

/**
 * Native library information declaration.
 */
struct NativeLibInfo
{
    const String*       name;
    NativeLibraryHandle handle;
    NativeLibInfo*      next;
};

/**
 * Native libraries list typedef declaration.
 */
typedef NativeLibInfo* NativeLibraryList;

/**
 * Initializes natives_support module. Caller must provide thread safety.
 *
 * @return Returns JNI_OK if initialized successfully.
 */
jint
natives_init();

/**
 * Cleanups natives_support module. Cleans all remaining libraries.
 */
void
natives_cleanup();

/**
 * Function loads native library with a given name.
 *
 * @param library_name - name of library
 * @param just_loaded  - is set when this library was not loaded before this call
 * @param pstatus      - pointer to status variable
 *
 * @return Loaded native library handle.
 */
NativeLibraryHandle
natives_load_library(const char* library_name, bool* just_loaded,
                     NativeLoadStatus* pstatus);

/**
 * Function unloads native library.
 *
 * @param library_handle - native library to be unloaded
 */
void
natives_unload_library(NativeLibraryHandle library_handle);

/**
 * Function looks for loaded native library with a given name.
 *
 * @param library_name - searching native library name
 *
 * @return <code>TRUE</code> if search is success, otherwise - <code>FALSE</code>.
 */
bool
natives_is_library_loaded(const char* library_name);

/**
 * Function looks for method with a given name and descriptor in a given native library.
 *
 * @param library_handle - native library handle
 * @param class_name     - name of class
 * @param method_name    - name of method
 * @param method_desc    - descriptor of method
 *
 * @return Found native function pointer.
 */
GenericFunctionPointer
natives_lookup_method( NativeLibraryList libraries,
                       const char* class_name,
                       const char* method_name,
                       const char* method_desc);

/**
 * Function returns detailed error description.
 *
 * @param error          - error code
 * @param buf            - string buffer
 * @param buflen         - buffer size
 *
 * @return Found native function pointer.
 */
void
natives_describe_error(NativeLoadStatus error, char* buf, size_t buflen);


/**
 * Function detects if module is JNI library
 *
 * @param libname        - library name (full or relative)
 *
 * @return true if specified library was loaded already by natives support
 */
//
bool natives_is_library_loaded_slow(const char* libname);

#endif // _NATIVES_SUPPORT_LIB_H_
