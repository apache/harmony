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
#ifndef _DLL_JIT_INTF_H_
#define _DLL_JIT_INTF_H_

#include <string.h>
#include "jit_intf_cpp.h"
#include "jit_export_jpda.h"
#include <apr_dso.h>

class Dll_JIT: public JIT {

public:

    Dll_JIT();

    Dll_JIT(const char *dll_filename);

    ~Dll_JIT() { 
        if (_deinit != NULL) _deinit(this); 
        if (pool != NULL) apr_pool_destroy(pool);
        //the library itself is unloaded by the pool destructor
    }

    void
    next_command_line_argument(const char *option,
                               const char *arg
                               )
    {
        if (_next_command_line_argument != NULL)
            _next_command_line_argument(this, option, arg);
    }

    JIT_Result 
    compile_method_with_params(Compile_Handle            compilation,
                               Method_Handle             method,
                               OpenMethodExecutionParams flags
                               )
    {
        if (_compile_method_with_params == NULL)
            return JIT_FAILURE;
        return _compile_method_with_params(this, compilation, method, flags);
    }

    void
    unwind_stack_frame(Method_Handle       method,
                       JitFrameContext*  context
                       )
    {
        _unwind_stack_frame(this, method, context);
    }


    void 
    get_root_set_from_stack_frame(Method_Handle          method,
                                  GC_Enumeration_Handle  enum_handle,
                                  const JitFrameContext*     context
                                  )
    {
        _get_root_set_from_stack_frame(this, method, enum_handle, context);
    }

    void 
    get_root_set_for_thread_dump(Method_Handle          method,
                                  GC_Enumeration_Handle  enum_handle,
                                  const JitFrameContext*     context
                                  )
    {
        _get_root_set_for_thread_dump(this, method, enum_handle, context);
    }

    U_32
    get_inline_depth(InlineInfoPtr  ptr,
                     U_32         offset)
    {
        if (_get_inline_depth != NULL) {
            return _get_inline_depth(this, ptr, offset);
        }
        return 0;
    }

    Method_Handle
    get_inlined_method(InlineInfoPtr  ptr,
                     U_32         offset,
                     U_32         inline_depth)
    {
        if (_get_inlined_method != NULL) {
            return _get_inlined_method(this, ptr, offset, inline_depth);
        }
        return NULL;
    }

    uint16
    get_inlined_bc(InlineInfoPtr  ptr,
                     U_32         offset,
                     U_32         inline_depth)
    {
        if (_get_inlined_bc != NULL) {
            return _get_inlined_bc(this, ptr, offset, inline_depth);
        }
        return 0;
    }

    void
    fix_handler_context(Method_Handle      method,
                        JitFrameContext* context
                        )
    {
        _fix_handler_context(this, method, context);
    }


    void *
    get_address_of_this(Method_Handle            method,
                        const JitFrameContext* context
                        )
    {
        return (void *)_get_address_of_this(this, method, context);
    }
    
    Boolean
        is_soe_area(Method_Handle            method,
        const JitFrameContext* context
        )
    {
        if (_is_soe_area != 0) {
            return (Boolean)_is_soe_area(this, method, context);
        }
        return 0;
        
    }

    Boolean
    recompiled_method_callback(Method_Handle  recompiled_method,
                               void          *callback_data)
    {
        if (_recompiled_method_callback != NULL) {
            return _recompiled_method_callback(this, recompiled_method, callback_data);
        }
        return FALSE;
    }

    Boolean 
    supports_compressed_references()
    {
        if (_supports_compressed_references != NULL) {
            return _supports_compressed_references(this);
        }
        return FALSE;
    }

    void
    execute_method(jmethodID method, jvalue *return_value, jvalue *args) {
        _execute_method(this, method, return_value, args);
    }

    OpenExeJpdaError get_bc_location_for_native(Method_Handle method,
        NativeCodePtr native_pc, uint16 *bc_pc)
    {
        if (_get_bc_location_for_native == NULL) 
            return EXE_ERROR_UNSUPPORTED;
        else
            return _get_bc_location_for_native(this, method,  native_pc, bc_pc);
    }  

    OpenExeJpdaError get_native_location_for_bc(Method_Handle method,
        uint16 bc_pc, NativeCodePtr *native_pc)
    {
        if (_get_native_location_for_bc == NULL) 
            return EXE_ERROR_UNSUPPORTED;
        else
            return _get_native_location_for_bc(this, method, bc_pc, native_pc);
    }  

    OpenExeJpdaError get_local_var(Method_Handle method, const JitFrameContext *context,
        uint16 var_num, VM_Data_Type var_type, void *value_ptr)
    {
        if (_get_local_var == NULL) 
            return EXE_ERROR_UNSUPPORTED;
        else
            return _get_local_var(this, method, context, var_num, var_type, value_ptr);
    }  

    OpenExeJpdaError set_local_var(Method_Handle method, const JitFrameContext *context,
        uint16 var_num, VM_Data_Type var_type, void *value_ptr)
    {
        if (_set_local_var == NULL) 
            return EXE_ERROR_UNSUPPORTED;
        else
            return _set_local_var(this, method, context, var_num, var_type, value_ptr);
    }  

    apr_dso_handle_t* get_lib_handle() const {return lib_handle;}

private:
    void (*_deinit)(JIT_Handle jit);

    void
    (*_next_command_line_argument)(JIT_Handle jit,
                                   const char *option,
                                   const char *arg
                                   );

    JIT_Result 
    (*_compile_method_with_params)(JIT_Handle jit,
                                   Compile_Handle            compilation,
                                   Method_Handle             method,
                                   OpenMethodExecutionParams flags
                                   );

    void
    (*_unwind_stack_frame)(JIT_Handle         jit,
                           Method_Handle      method,
                           JitFrameContext*   context
                           );

    void 
    (*_get_root_set_from_stack_frame)(JIT_Handle              jit,
                                      Method_Handle           method,
                                      GC_Enumeration_Handle   enum_handle,
                                      const JitFrameContext*  context
                                      );

    void 
    (*_get_root_set_for_thread_dump)(JIT_Handle              jit,
                                      Method_Handle           method,
                                      GC_Enumeration_Handle   enum_handle,
                                      const JitFrameContext*  context
                                      );
    
    U_32
    (*_get_inline_depth)(
                        JIT_Handle jit,                    
                        InlineInfoPtr  ptr, 
                        U_32         offset);

    Method_Handle
    (*_get_inlined_method)(JIT_Handle jit,
                            InlineInfoPtr  ptr, 
                            U_32         offset,
                            U_32         inline_depth);

    uint16
    (*_get_inlined_bc)(JIT_Handle jit,
                            InlineInfoPtr  ptr,
                            U_32         offset,
                            U_32         inline_depth);

    void
    (*_fix_handler_context)(JIT_Handle         jit,
                            Method_Handle      method,
                            JitFrameContext*   context
                            );

    void *
    (*_get_address_of_this)(JIT_Handle               jit,
                            Method_Handle            method,
                            const JitFrameContext* context
                            );

    Boolean
        (*_is_soe_area)(JIT_Handle              jit,
        Method_Handle                           method,
        const JitFrameContext*                  context
        );

    Boolean
    (*_recompiled_method_callback)(JIT_Handle jit,
                                   Method_Handle  recompiled_method, 
                                   void          *callback_data);

    Boolean 
    (*_supports_compressed_references)(JIT_Handle jit);

    void
    (*_execute_method) (JIT_Handle jit, jmethodID method, jvalue *return_value, jvalue *args);
    
    OpenExeJpdaError 
        (*_get_bc_location_for_native)(JIT_Handle jit, Method_Handle  method,
        NativeCodePtr   native_pc, uint16 *bc_pc);

    OpenExeJpdaError 
        (*_get_native_location_for_bc)(JIT_Handle jit, Method_Handle  method,
        uint16 bc_pc, NativeCodePtr   *native_pc);

    OpenExeJpdaError
        (*_get_local_var)(JIT_Handle jit, Method_Handle method, const JitFrameContext *context,
        uint16 var_num, VM_Data_Type var_type, void *value_ptr);

    OpenExeJpdaError
        (*_set_local_var)(JIT_Handle jit, Method_Handle method, const JitFrameContext *context,
        uint16 var_num, VM_Data_Type var_type, void *value_ptr);

    const char *jit_dll_filename;
    apr_pool_t *pool;
    apr_dso_handle_t *lib_handle;
};



#endif
