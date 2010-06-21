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
 * @author Intel, Mikhail Y. Fursov
 *
 */

#ifndef PLATFORM_POSIX
#include <crtdbg.h>
#endif

#include "Type.h"
#include "Jitrino.h"
#include "VMInterface.h"
#include "EMInterface.h"
#include "Log.h"
#include "PMF.h"
#include "CompilationContext.h"
#include "JITInstanceContext.h"
#include "RuntimeInterface.h"

#include "jit_export.h"
#include "jit_export_jpda.h"
#include "open/types.h"

#define LOG_DOMAIN "jitrino"
#include "cxxlog.h"

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include "port_threadunsafe.h"

#if !defined(_IPF_) // No .JET on IPF yet
    #define USE_FAST_PATH
#endif

#ifdef USE_FAST_PATH
    #include "../../jet/jet.h"
#endif

namespace Jitrino {

//
// exported DLL functions for the DRL VM
//
////////////////////////////////////////////////////////
// Optional functions that don't have to be provided.
////////////////////////////////////////////////////////

#ifdef USE_FAST_PATH
static bool isJET(JIT_Handle jit)
{
    JITInstanceContext* jitContext = Jitrino::getJITInstanceContext(jit);
    return jitContext->isJet();
}
#endif


// Called once at the end of the constructor.
extern "C"
JITEXPORT void
JIT_init(JIT_Handle jit, const char* name, vm_adaptor_t adaptor)
{
    std::string initMessage = std::string("Initializing Jitrino.") + name + " -> ";
    std::string mode = "OPT";
#ifdef USE_FAST_PATH
    if (JITInstanceContext::isNameReservedForJet(name)) mode = "JET";
#endif 
    initMessage = initMessage + mode + " compiler mode";
    INFO(initMessage.c_str());

    VMInterface::setVmAdapter(adaptor);

    Jitrino::Init(jit, name);

#ifdef USE_FAST_PATH
    Jet::setup(jit, name);
#endif
}

// Called once at the end of the destructor.
extern "C"
JITEXPORT void
JIT_deinit(JIT_Handle jit)
{
#ifdef USE_FAST_PATH
    Jet::cleanup();
#endif
    Jitrino::DeInit(jit);
}

extern "C"
JITEXPORT void
JIT_next_command_line_argument(JIT_Handle jit, const char *name,
                               const char *arg)
{
#ifdef USE_FAST_PATH
    Jet::cmd_line_arg(jit, name, arg);
#endif
}

extern "C"
JITEXPORT void
JIT_set_profile_access_interface(JIT_Handle jit, EM_Handle em,
                                 EM_ProfileAccessInterface* pc_interface)
{
    JITInstanceContext* jitContext = Jitrino::getJITInstanceContext(jit);
    MemoryManager& mm = Jitrino::getGlobalMM();
    ProfilingInterface* pi = new (mm) ProfilingInterface(em, jit, pc_interface);
    jitContext->setProfilingInterface(pi);
}


//Optional
extern "C"
JITEXPORT bool
JIT_enable_profiling(JIT_Handle jit, PC_Handle pc, EM_JIT_PC_Role role)
{
    JITInstanceContext* jitContext = Jitrino::getJITInstanceContext(jit);
    ProfilingInterface* pi = jitContext->getProfilingInterface();
    return pi->enableProfiling(pc, role == EM_JIT_PROFILE_ROLE_GEN ? 
                                JITProfilingRole_GEN: JITProfilingRole_USE);
}

extern "C"
JITEXPORT void
JIT_profile_notification_callback(JIT_Handle jit, PC_Handle pc, Method_Handle mh) 
{
    JITInstanceContext* jitContext = Jitrino::getJITInstanceContext(jit);
    if (jitContext->isJet()) {
#ifdef USE_FAST_PATH
        Jet::rt_profile_notification_callback(jit, pc, mh);
#endif
    } //opt does not support counters patching today.
}

// Called if JIT registered itself to be notified when the method is
// recompiled
// Returns TRUE if any code was modified and FALSE otherwise
extern "C"
JITEXPORT Boolean
JIT_recompiled_method_callback(JIT_Handle jit,
                               Method_Handle recompiled_method,
                               void *callback_data)
{
    MethodDesc methodDesc(recompiled_method, NULL);
    bool res = Jitrino::getRuntimeInterface()->recompiledMethodEvent(&methodDesc,callback_data);
    return (res ? TRUE : FALSE);
}


////////////////////////////////////////////////////////
// Required functions.
////////////////////////////////////////////////////////

extern "C"
JITEXPORT JIT_Result
JIT_compile_method_with_params(JIT_Handle jit, Compile_Handle compilation,
                               Method_Handle method_handle,
                               OpenMethodExecutionParams compilation_params)
{
    MemoryManager memManager("JIT_compile_method.memManager");
    JITInstanceContext* jitContext = Jitrino::getJITInstanceContext(jit);
    assert(jitContext!= NULL);

    TypeManager typeManager(memManager); typeManager.init();
    CompilationInterface  compilationInterface(compilation, method_handle, jit,
            memManager, compilation_params, NULL, typeManager);
    CompilationContext cs(memManager, &compilationInterface, jitContext);
    compilationInterface.setCompilationContext(&cs);

    static int method_seqnb = 0;
    UNSAFE_REGION_START
    // Non-atomic increment of compiled method counter,
    // may affect accuracy of JIT logging but don't affect JIT functionality
    int current_nb = method_seqnb++;
    UNSAFE_REGION_END
    MethodDesc* md = compilationInterface.getMethodToCompile();
    const char* methodTypeName = md->getParentType()->getName();
    const char* methodName = md->getName();
    const char* methodSig  = md->getSignatureString();
    PMF::Pipeline* pipep = jitContext->getPMF().selectPipeline(methodTypeName, methodName, methodSig);
    cs.setPipeline((HPipeline*)pipep);
    LogStreams::current(jitContext).beginMethod(methodTypeName, methodName, methodSig, method_seqnb);
    Str pipename = pipep->getName();
    LogStream& info = LogStream::log(LogStream::INFO, (HPipeline*)pipep);
    if (info.isEnabled()) {
        info << "<" << current_nb << "\t"
             << jitContext->getJITName() << "." << pipename
             << "\tstart "
             << methodTypeName << "." << methodName << methodSig
             << "\tbyte code size=" <<  md->getByteCodeSize()
             << std::endl;
    }
#ifdef _DEBUG
    Jitrino::incCompilationRecursionLevel();
#endif

    JIT_Result result;
 
#ifdef USE_FAST_PATH
    if (isJET(jit))
        result = Jet::compile_with_params(jit, compilation, method_handle,
                                        compilation_params);
    else 
#endif  // USE_FAST_PATH

        result = Jitrino::CompileMethod(&cs) ? JIT_SUCCESS : JIT_FAILURE;

#ifdef _DEBUG
    Jitrino::decCompilationRecursionLevel();
#endif

    if (info.isEnabled()) {
        info << current_nb << ">\t"
             << jitContext->getJITName() << "." << pipename
             << "\t  end ";
             //<< methodTypeName << "." << methodName << methodSig;

        if (result == JIT_SUCCESS) {
            unsigned size = md->getCodeBlockSize(0);
            U_8*    start = size ? md->getCodeBlockAddress(0) : 0;
            info << "\tnative code size=" << size
                 << " code range=[" << (void*)start << "," << (void*)(start+size) << "]";
        }
        else
             info << "\tFAILURE";

        info << std::endl;
    }

    LogStreams::current(jitContext).endMethod();
    return result;
}

extern "C"
JITEXPORT OpenMethodExecutionParams JIT_get_exe_capabilities (JIT_Handle jit)
{
#ifdef USE_FAST_PATH
    if (isJET(jit)) {
        return Jet::get_exe_capabilities();
    }
#endif  // USE_FAST_PATH
    
    static const OpenMethodExecutionParams compilation_capabilities = {
        false, // exe_notify_method_entry
        false, // exe_notify_method_exit
        false, // exe_notify_field_access
        false, // exe_notify_field_modification
        false, // exe_notify_exception_throw
        false, // exe_notify_exception_catch
        false, // exe_notify_monitor_enter
        false, // exe_notify_monitor_exit
        false, // exe_notify_contended_monitor_enter
        false, // exe_notify_contended_monitor_exit
        false, // exe_do_method_inlining
        false, // exe_do_code_mapping
        false, // exe_do_local_var_mapping
        false, // exe_insert_write_barriers
        false, // exe_provide_access_to_this
        false, // exe_restore_context_after_unwind
        false, // exe_notify_compiled_method_load
    };
    return compilation_capabilities;
}

extern "C"
JITEXPORT void
JIT_unwind_stack_frame(JIT_Handle jit, Method_Handle method,
                       ::JitFrameContext *context)
{
#ifdef _DEBUG
    if(Log::cat_rt()->isEnabled())
        Log::cat_rt()->out() << "UNWIND_STACK_FRAME(" << method << ")" << ::std::endl;
#endif

#ifdef USE_FAST_PATH
    if (isJET(jit)) {
        Jet::rt_unwind(jit, method, context);
        return;
    }
#endif
    MethodDesc methodDesc(method, jit);
    Jitrino::getRuntimeInterface()->unwindStack(&methodDesc, context, context->is_ip_past == FALSE);
}

extern "C"
JITEXPORT void
JIT_get_root_set_from_stack_frame(JIT_Handle jit, Method_Handle method,
                                  GC_Enumeration_Handle enum_handle,
                                  ::JitFrameContext *context)
{
#ifdef _DEBUG
    if(Log::cat_rt()->isEnabled())
        Log::cat_rt()->out() << "GET_ROOT_SET_FROM_STACK_FRAME(" << method << ")" << ::std::endl;
#endif

#ifdef USE_FAST_PATH
    if (isJET(jit)) {
        Jet::rt_enum(jit, method, enum_handle, context);
        return;
    }
#endif

    MethodDesc methodDesc(method, jit);
    GCInterface gcInterface(enum_handle);
    Jitrino::getRuntimeInterface()->getGCRootSet(&methodDesc, &gcInterface, context,
                          context->is_ip_past == FALSE);
}

extern "C"
JITEXPORT U_32
JIT_get_inline_depth(JIT_Handle jit, InlineInfoPtr ptr, U_32 offset)
{
    if (Log::cat_rt()->isEnabled()) {
        Log::cat_rt()->out() << "GET_INLINE_DEPTH()" << ::std::endl;
    }
    return Jitrino::getRuntimeInterface()->getInlineDepth(ptr, offset);
}

extern "C"
JITEXPORT Method_Handle
JIT_get_inlined_method(JIT_Handle jit, InlineInfoPtr ptr, U_32 offset,
                       U_32 inline_depth)
{
    if (Log::cat_rt()->isEnabled()) {
        Log::cat_rt()->out() << "GET_INLINED_METHOD()" << ::std::endl;
    }
    if (0 == inline_depth) {
        assert(0 && "misused inline_depth, must be [1..max_depth]");
        return 0;
    }
    return Jitrino::getRuntimeInterface()->getInlinedMethod(ptr, offset, inline_depth);
}

extern "C"
JITEXPORT uint16
JIT_get_inlined_bc(JIT_Handle jit, InlineInfoPtr ptr, U_32 offset, U_32 inline_depth)
{
    if (Log::cat_rt()->isEnabled()) {
        Log::cat_rt()->out() << "GET_INLINED_BC()" << ::std::endl;
    }
    if (0 == inline_depth) {
        assert(0 && "misused inline_depth, must be [1..max_depth]");
        return 0;
    }
    return Jitrino::getRuntimeInterface()->getInlinedBc(ptr, offset, inline_depth);
}

extern "C"
JITEXPORT void
JIT_fix_handler_context(JIT_Handle jit, Method_Handle method,
                        ::JitFrameContext *context)
{
#ifdef _DEBUG
    if(Log::cat_rt()->isEnabled())
        Log::cat_rt()->out() << "FIX_HANDLER_CONTEXT(" << method << ")" << ::std::endl;
#endif

#ifdef USE_FAST_PATH
    if (isJET(jit)) {
        Jet::rt_fix_handler_context(jit, method, context);
        return;
    }
#endif

    MethodDesc methodDesc(method, jit);
    Jitrino::getRuntimeInterface()->fixHandlerContext(&methodDesc, context,
                               context->is_ip_past == FALSE);
}

extern "C"
JITEXPORT Boolean
JIT_is_soe_area
(JIT_Handle jit, Method_Handle method,
                        const ::JitFrameContext   *context)
{
#ifdef USE_FAST_PATH
    if (isJET(jit)) {
        return Jet::rt_is_soe_area(jit, method, context);
    }
#endif
    MethodDesc methodDesc(method, jit);
    return Jitrino::getRuntimeInterface()->isSOEArea(&methodDesc, context, context->is_ip_past == FALSE);
}


extern "C"
JITEXPORT void *
JIT_get_address_of_this(JIT_Handle jit, Method_Handle method,
                        const ::JitFrameContext   *context)
{
#ifdef USE_FAST_PATH
    if (isJET(jit)) {
        return Jet::rt_get_address_of_this(jit, method, context);
    }
#endif
    MethodDesc methodDesc(method, jit);
    return Jitrino::getRuntimeInterface()->getAddressOfThis(&methodDesc, context,
                                     context->is_ip_past == FALSE);
}

extern "C"
JITEXPORT Boolean
JIT_supports_compressed_references(JIT_Handle jit)
{
#ifdef _EM64T_
        return true;
#else
        return false;
#endif
}

extern "C"
JITEXPORT void
JIT_get_root_set_for_thread_dump(JIT_Handle jit, Method_Handle method,
                                 GC_Enumeration_Handle enum_handle,
                                 ::JitFrameContext *context)
{
    if(Log::cat_rt()->isEnabled()) {
        Log::cat_rt()->out() << "GET_ROOT_SET_FROM_STACK_FRAME(" << method << ")" << ::std::endl;
    }
    MethodDesc methodDesc(method, jit);
    ThreadDumpEnumerator gcInterface;
    Jitrino::getRuntimeInterface()->getGCRootSet(&methodDesc, &gcInterface, context,
                          context->is_ip_past == FALSE);
}


////////////////////////////////////////////////////////
// JVMTI support functions.
////////////////////////////////////////////////////////

extern "C"
JITEXPORT OpenExeJpdaError
get_native_location_for_bc(JIT_Handle jit, Method_Handle method,
                           uint16  bc_pc, NativeCodePtr  *native_pc)
{
#ifdef USE_FAST_PATH
    if (isJET(jit)) {
        Jet::rt_bc2native(jit, method, bc_pc, native_pc);
        return EXE_ERROR_NONE;
    }
#endif

    MethodDesc methDesc(method, jit);
    POINTER_SIZE_INT* ncAddr = (POINTER_SIZE_INT*) native_pc;

    if (Jitrino::getRuntimeInterface()->getNativeLocationForBc(&methDesc, bc_pc, ncAddr)) {
        return EXE_ERROR_NONE;
    }
    return EXE_ERROR_UNSUPPORTED;
}

extern "C"
JITEXPORT OpenExeJpdaError
get_bc_location_for_native(JIT_Handle jit, Method_Handle method,
                           NativeCodePtr native_pc, uint16 *bc_pc)
{
#ifdef USE_FAST_PATH
    if (isJET(jit)) {
        Jet::rt_native2bc(jit, method, native_pc, bc_pc);
        return EXE_ERROR_NONE;
    }
#endif

    MethodDesc methDesc(method, jit);
    POINTER_SIZE_INT ncAddr = (POINTER_SIZE_INT) native_pc;
    if (Jitrino::getRuntimeInterface()->getBcLocationForNative(&methDesc, ncAddr, bc_pc)) {
        return EXE_ERROR_NONE;
    }
    return EXE_ERROR_INVALID_LOCATION;
}

extern "C"
JITEXPORT ::OpenExeJpdaError
get_local_var(JIT_Handle jit, Method_Handle method,
              const ::JitFrameContext *context, uint16  var_num,
              VM_Data_Type var_type, void *value_ptr)
{
#ifdef USE_FAST_PATH
    if (isJET(jit)) {
        return Jet::rt_get_local_var(jit, method, context, var_num, var_type,
                                     value_ptr);
    }
#endif
    return EXE_ERROR_UNSUPPORTED;
}

extern "C"
JITEXPORT ::OpenExeJpdaError
set_local_var(JIT_Handle jit, Method_Handle method,
              const ::JitFrameContext *context, uint16 var_num,
              VM_Data_Type var_type, void *value_ptr)
{
#ifdef USE_FAST_PATH
    if (isJET(jit)) {
        return Jet::rt_set_local_var(jit, method, context, var_num, var_type,
                                     value_ptr);
    }
#endif
    return EXE_ERROR_UNSUPPORTED;
}

} //namespace Jitrino



