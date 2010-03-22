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
#define LOG_DOMAIN "vm.core"
#include "cxxlog.h"

#include "Class.h"
#include "dll_jit_intf.h"
#include "ini.h"
#include "open/gc.h"
#include "compile.h"


Dll_JIT::Dll_JIT(const char *dll_filename) :
_deinit(NULL),
_next_command_line_argument(NULL),
_compile_method_with_params(NULL),
_unwind_stack_frame(NULL),
_get_root_set_from_stack_frame(NULL),
_get_root_set_for_thread_dump(NULL),
_fix_handler_context(NULL),
_get_address_of_this(NULL),
_recompiled_method_callback(NULL),
_execute_method(NULL),
_get_bc_location_for_native(NULL),
_get_native_location_for_bc(NULL),
_get_local_var(NULL),
_set_local_var(NULL),
jit_dll_filename(NULL),
lib_handle(NULL)
{
    apr_status_t stat;
    char buf[1024];
    apr_pool_create(&pool, 0);
    assert(pool);
    apr_dso_handle_t *handle;
    if ((stat = apr_dso_load(&handle, dll_filename, pool)) != APR_SUCCESS)
    {
        LWARN(28, "Failure to open JIT dll {0} {1}" << dll_filename << stat);
        printf("apr code: %s\n", apr_dso_error(handle, buf, 1024));
        return;
    }

    lib_handle = handle;
    jit_dll_filename = dll_filename;
    apr_dso_handle_sym_t fn;

#define GET_FUNCTION(fn, handle, name) \
    if (apr_dso_sym(&fn, handle, name) != APR_SUCCESS) {\
    LWARN(29, "Couldn't load dll {0}: missing entry point {1}" << dll_filename << name); \
    return; \
    }

#define GET_OPTIONAL_FUNCTION(fn, handle, name) \
    if (apr_dso_sym(&fn, handle, name) != APR_SUCCESS) {\
        fn = 0;\
    }

    GET_OPTIONAL_FUNCTION(fn, handle, "JIT_deinit");
    _deinit = (void (*)(JIT_Handle)) fn;


    GET_OPTIONAL_FUNCTION(fn, handle, "JIT_next_command_line_argument");
    _next_command_line_argument = (void (*)(JIT_Handle, const char *, const char *)) fn;

    GET_OPTIONAL_FUNCTION(fn, handle, "JIT_recompiled_method_callback");
    _recompiled_method_callback = (Boolean (*)(JIT_Handle, Method_Handle, void *)) fn;

    GET_OPTIONAL_FUNCTION(fn, handle, "JIT_get_inline_depth");
    _get_inline_depth = (U_32 (*)(JIT_Handle, InlineInfoPtr, U_32)) fn;

    GET_OPTIONAL_FUNCTION(fn, handle, "JIT_get_inlined_method");
    _get_inlined_method = (Method_Handle (*)(JIT_Handle, InlineInfoPtr, U_32, U_32)) fn;

    GET_OPTIONAL_FUNCTION(fn, handle, "JIT_get_inlined_bc");
    _get_inlined_bc = (uint16 (*)(JIT_Handle, InlineInfoPtr, U_32, U_32)) fn;

    GET_FUNCTION(fn, handle, "JIT_unwind_stack_frame");
    _unwind_stack_frame = (void (*)(JIT_Handle, Method_Handle, JitFrameContext *)) fn;

    GET_FUNCTION(fn, handle, "JIT_get_root_set_from_stack_frame");
    _get_root_set_from_stack_frame = (void (*)(JIT_Handle, Method_Handle, GC_Enumeration_Handle, const JitFrameContext *)) fn;

    GET_FUNCTION(fn, handle, "JIT_get_root_set_for_thread_dump");
    _get_root_set_for_thread_dump = (void (*)(JIT_Handle, Method_Handle, GC_Enumeration_Handle, const JitFrameContext *)) fn;

    GET_FUNCTION(fn, handle, "JIT_fix_handler_context");
    _fix_handler_context = (void (*)(JIT_Handle, Method_Handle, JitFrameContext *)) fn;

    GET_FUNCTION(fn, handle, "JIT_get_address_of_this");
    _get_address_of_this = (void * (*)(JIT_Handle, Method_Handle, const JitFrameContext *)) fn;

    GET_OPTIONAL_FUNCTION(fn, handle, "JIT_is_soe_area");
    _is_soe_area = (Boolean (*)(JIT_Handle, Method_Handle, const JitFrameContext *)) fn;

    GET_FUNCTION(fn, handle, "JIT_compile_method_with_params");
    _compile_method_with_params = (JIT_Result (*)(JIT_Handle, Compile_Handle, Method_Handle, OpenMethodExecutionParams)) fn;

    GET_OPTIONAL_FUNCTION(fn, handle, "JIT_supports_compressed_references");
    _supports_compressed_references = (Boolean (*)(JIT_Handle)) fn;

    GET_OPTIONAL_FUNCTION(fn, handle, "JIT_execute_method");
    _execute_method = (void(*)(JIT_Handle, jmethodID, jvalue*, jvalue*)) fn;

    GET_OPTIONAL_FUNCTION(fn, handle, "get_bc_location_for_native");
    _get_bc_location_for_native = (OpenExeJpdaError(*)(JIT_Handle, Method_Handle, NativeCodePtr , uint16*)) fn;

    GET_OPTIONAL_FUNCTION(fn, handle, "get_native_location_for_bc");
    _get_native_location_for_bc = (OpenExeJpdaError(*)(JIT_Handle, Method_Handle, uint16, NativeCodePtr*)) fn;

    GET_OPTIONAL_FUNCTION(fn, handle, "get_local_var");
    _get_local_var = (OpenExeJpdaError(*)(JIT_Handle, Method_Handle, const JitFrameContext *,
        uint16, VM_Data_Type, void*)) fn;

    GET_OPTIONAL_FUNCTION(fn, handle, "set_local_var");
    _set_local_var = (OpenExeJpdaError(*)(JIT_Handle, Method_Handle, const JitFrameContext *,
        uint16, VM_Data_Type, void*)) fn;

    if (!_execute_method) {
        _execute_method = JIT_execute_method_default;
    }
}
