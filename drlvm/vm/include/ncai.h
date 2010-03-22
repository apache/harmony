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
 * @author Intel, Petr Ivanov
 */
#ifndef _NCAI_H_
#define _NCAI_H_

#include "ncai_types.h"
#include "jni_types.h"


/*
 * Supported NCAI versions
 */
#define NCAI_VERSION_MASK_MAJOR 0x0FFF0000
#define NCAI_VERSION_MASK_MINOR 0x0000FF00
#define NCAI_VERSION_MASK_MICRO 0x000000FF
#define NCAI_VERSION_SHIFT_MAJOR 16
#define NCAI_VERSION_SHIFT_MINOR 8
#define NCAI_VERSION_SHIFT_MICRO 0

#define NCAI_VERSION_MAJOR 1
#define NCAI_VERSION_MINOR 0
#define NCAI_VERSION_MICRO 3

#define NCAI_VERSION_1_0 \
    ((NCAI_VERSION_MAJOR << NCAI_VERSION_SHIFT_MAJOR) | \
     (NCAI_VERSION_MINOR << NCAI_VERSION_SHIFT_MINOR) | \
     (NCAI_VERSION_MICRO << NCAI_VERSION_SHIFT_MICRO))

#define NCAI_VERSION NCAI_VERSION_1_0


struct _ncai
{
    ncaiError (JNICALL *GetAllLoadedModules) (ncaiEnv* env,
        jint* count_ptr,
        ncaiModule** modules_ptr);

    ncaiError (JNICALL *GetModuleInfo) (ncaiEnv* env,
        ncaiModule module,
        ncaiModuleInfo* info_ptr);

    ncaiError (JNICALL *GetModuleClassLoader) (ncaiEnv* env,
        ncaiModule module,
        jobject* classloader_ptr);

    ncaiError (JNICALL *IsMethodCompiled) (ncaiEnv* env,
        jmethodID method,
        jboolean* is_compiled_ptr);

    ncaiError (JNICALL *GetMethodLocation) (ncaiEnv* env,
        jmethodID method,
        void** address_ptr,
        size_t* size_ptr);

    ncaiError (JNICALL *FindJavaMethod) (ncaiEnv* env,
        void* address,
        jmethodID* method_ptr);

    ncaiError (JNICALL *GetBytcodeLocation) (ncaiEnv* env,
        void* address,
        jmethodID* method,
        jlocation* location_ptr);

    ncaiError (JNICALL *GetNativeLocation) (ncaiEnv* env,
        jmethodID method,
        jlocation location,
        void** address_ptr);

    ncaiError (JNICALL *GetAllThreads) (ncaiEnv* env,
        jint* count_ptr,
        ncaiThread** threads_ptr);

    ncaiError (JNICALL *GetThreadInfo) (ncaiEnv* env,
        ncaiThread thread,
        ncaiThreadInfo* info_ptr);

    ncaiError (JNICALL *GetThreadHandle) (ncaiEnv* env,
        jthread thread,
        ncaiThread* thread_ptr);

    ncaiError (JNICALL *GetThreadObject) (ncaiEnv* env,
        ncaiThread thread,
        jthread* thread_ptr);

    ncaiError (JNICALL *SuspendThread) (ncaiEnv* env,
        ncaiThread thread);

    ncaiError (JNICALL *ResumeThread) (ncaiEnv* env,
        ncaiThread thread);

    ncaiError (JNICALL *TerminateThread) (ncaiEnv* env,
        ncaiThread thread);

    ncaiError (JNICALL *GetThreadState) (ncaiEnv* env,
        ncaiThread thread,
        jint* state_ptr);

    ncaiError (JNICALL *GetFrameCount) (ncaiEnv* env,
        ncaiThread thread,
        jint* count_ptr);

    ncaiError (JNICALL *GetStackTrace) (ncaiEnv* env,
        ncaiThread thread,
        jint depth,
        ncaiFrameInfo* frame_buffer,
        jint* count_ptr);

    ncaiError (JNICALL *GetRegisterCount) (ncaiEnv* env,
        jint* count_ptr);

    ncaiError (JNICALL *GetRegisterInfo) (ncaiEnv* env,
        jint reg_number,
        ncaiRegisterInfo* info_ptr);

    ncaiError (JNICALL *GetRegisterValue) (ncaiEnv* env,
        ncaiThread thread,
        jint reg_number,
        void* buf);

    ncaiError (JNICALL *SetRegisterValue) (ncaiEnv* env,
        ncaiThread thread,
        jint reg_number,
        void* buf);

    ncaiError (JNICALL *ReadMemory) (ncaiEnv* env,
        void* addr,
        size_t size,
        void* buf);

    ncaiError (JNICALL *WriteMemory) (ncaiEnv* env,
        void* addr,
        size_t size,
        void* buf);

    ncaiError (JNICALL *GetSignalCount) (ncaiEnv* env,
        jint* count_ptr);

    ncaiError (JNICALL *GetSignalInfo) (ncaiEnv* env,
        jint signal,
        ncaiSignalInfo* info_ptr);

    ncaiError (JNICALL *GetJvmtiEnv) (ncaiEnv* env,
        jvmtiEnv** jvmti_env_ptr);

    ncaiError (JNICALL *GetVersion) (ncaiEnv* env,
        jint* version_ptr);

    ncaiError (JNICALL *GetErrorName) (ncaiEnv* env,
        ncaiError err,
        const char** name_ptr);

    ncaiError (JNICALL *GetPotentialCapabilities) (ncaiEnv* env,
        ncaiCapabilities* caps_ptr);

    ncaiError (JNICALL *GetCapabilities) (ncaiEnv* env,
        ncaiCapabilities* caps_ptr);

    ncaiError (JNICALL *AddCapabilities) (ncaiEnv* env,
        ncaiCapabilities* caps_ptr);

    ncaiError (JNICALL *RelinquishCapabilities) (ncaiEnv* env,
        ncaiCapabilities* caps_ptr);

    ncaiError (JNICALL *GetEventCallbacks) (ncaiEnv* env,
        ncaiEventCallbacks* callbacks,
        size_t size);

    ncaiError (JNICALL *SetEventCallbacks) (ncaiEnv* env,
        ncaiEventCallbacks* callbacks,
        size_t size);

    ncaiError (JNICALL *SetEventNotificationMode) (ncaiEnv* env,
        ncaiEventMode mode,
        ncaiEventKind event,
        ncaiThread thread);

    ncaiError (JNICALL *SetBreakpoint) (ncaiEnv* env,
        void* code_addr);

    ncaiError (JNICALL *ClearBreakpoint) (ncaiEnv* env,
        void* code_addr);

    ncaiError (JNICALL *SetWatchpoint) (ncaiEnv* env,
        void* data_addr,
        size_t len,
        ncaiWatchpointMode mode);

    ncaiError (JNICALL *ClearWatchpoint) (ncaiEnv* env,
        void* data_addr);

    ncaiError (JNICALL *SetStepMode) (ncaiEnv* env,
        ncaiThread thread,
        ncaiStepMode mode);

    ncaiError (JNICALL *NotifyFramePop) (ncaiEnv* env,
        ncaiThread thread,
        void* frame_address);
};

struct ncaiEnv_struct
{
    const struct _ncai* funcs;

#ifdef __cplusplus

    ncaiError GetAllLoadedModules (jint* count_ptr, ncaiModule** modules_ptr)
    {
        return funcs->GetAllLoadedModules (this, count_ptr, modules_ptr);
    }

    ncaiError GetModuleInfo (ncaiModule module, ncaiModuleInfo* info_ptr)
    {
        return funcs->GetModuleInfo (this, module, info_ptr);
    }

    ncaiError GetModuleClassLoader (ncaiModule module, jobject* classloader_ptr)
    {
        return funcs->GetModuleClassLoader (this, module, classloader_ptr);
    }

    ncaiError IsMethodCompiled (jmethodID method, jboolean* is_compiled_ptr)
    {
        return funcs->IsMethodCompiled (this, method, is_compiled_ptr);
    }

    ncaiError GetMethodLocation (jmethodID method, void** address_ptr, size_t* size_ptr)
    {
        return funcs->GetMethodLocation (this, method, address_ptr, size_ptr);
    }

    ncaiError FindJavaMethod (void* address, jmethodID* method_ptr)
    {
        return funcs->FindJavaMethod (this, address, method_ptr);
    }

    ncaiError GetBytcodeLocation (void* address, jmethodID* method, jlocation* location_ptr)
    {
        return funcs->GetBytcodeLocation (this, address, method, location_ptr);
    }

    ncaiError GetNativeLocation (jmethodID method, jlocation location, void** address_ptr)
    {
        return funcs->GetNativeLocation (this, method, location, address_ptr);
    }

    ncaiError GetAllThreads (jint* count_ptr, ncaiThread** threads_ptr)
    {
        return funcs->GetAllThreads (this, count_ptr, threads_ptr);
    }

    ncaiError GetThreadInfo (ncaiThread thread, ncaiThreadInfo* info_ptr)
    {
        return funcs->GetThreadInfo (this, thread, info_ptr);
    }

    ncaiError GetThreadHandle (jthread thread, ncaiThread* thread_ptr)
    {
        return funcs->GetThreadHandle (this, thread, thread_ptr);
    }

    ncaiError GetThreadObject (ncaiThread thread, jthread* thread_ptr)
    {
        return funcs->GetThreadObject (this, thread, thread_ptr);
    }

    ncaiError SuspendThread (ncaiThread thread)
    {
        return funcs->SuspendThread (this, thread);
    }

    ncaiError ResumeThread (ncaiThread thread)
    {
        return funcs->ResumeThread (this, thread);
    }

    ncaiError TerminateThread (ncaiThread thread)
    {
        return funcs->TerminateThread (this, thread);
    }

    ncaiError GetThreadState (ncaiThread thread, jint* state_ptr)
    {
        return funcs->GetThreadState (this, thread, state_ptr);
    }

    ncaiError GetFrameCount (ncaiThread thread, jint* count_ptr)
    {
        return funcs->GetFrameCount (this, thread, count_ptr);
    }

    ncaiError GetStackTrace (ncaiThread thread, jint depth, ncaiFrameInfo* frame_buffer, jint* count_ptr)
    {
        return funcs->GetStackTrace (this, thread, depth, frame_buffer, count_ptr);
    }

    ncaiError GetRegisterCount (jint* count_ptr)
    {
        return funcs->GetRegisterCount (this, count_ptr);
    }

    ncaiError GetRegisterInfo (jint reg_number, ncaiRegisterInfo* info_ptr)
    {
        return funcs->GetRegisterInfo (this, reg_number, info_ptr);
    }

    ncaiError GetRegisterValue (ncaiThread thread, jint reg_number, void* buf)
    {
        return funcs->GetRegisterValue (this, thread, reg_number, buf);
    }

    ncaiError SetRegisterValue (ncaiThread thread, jint reg_number, void* buf)
    {
        return funcs->SetRegisterValue (this, thread, reg_number, buf);
    }

    ncaiError ReadMemory (void* addr, size_t size, void* buf)
    {
        return funcs->ReadMemory (this, addr, size, buf);
    }

    ncaiError WriteMemory (void* addr, size_t size, void* buf)
    {
        return funcs->WriteMemory (this, addr, size, buf);
    }

    ncaiError GetSignalCount (jint* count_ptr)
    {
        return funcs->GetSignalCount (this, count_ptr);
    }

    ncaiError GetSignalInfo (jint signal, ncaiSignalInfo* info_ptr)
    {
        return funcs->GetSignalInfo (this, signal, info_ptr);
    }

    ncaiError GetJvmtiEnv (jvmtiEnv** jvmti_env_ptr)
    {
        return funcs->GetJvmtiEnv (this, jvmti_env_ptr);
    }

    ncaiError GetVersion (jint* version_ptr)
    {
        return funcs->GetVersion (this, version_ptr);
    }

    ncaiError GetErrorName (ncaiError err, const char** name_ptr)
    {
        return funcs->GetErrorName (this, err, name_ptr);
    }

    ncaiError GetPotentialCapabilities (ncaiCapabilities* caps_ptr)
    {
        return funcs->GetPotentialCapabilities (this, caps_ptr);
    }

    ncaiError GetCapabilities (ncaiCapabilities* caps_ptr)
    {
        return funcs->GetCapabilities (this, caps_ptr);
    }

    ncaiError AddCapabilities (ncaiCapabilities* caps_ptr)
    {
        return funcs->AddCapabilities (this, caps_ptr);
    }

    ncaiError RelinquishCapabilities (ncaiCapabilities* caps_ptr)
    {
        return funcs->RelinquishCapabilities (this, caps_ptr);
    }

    ncaiError GetEventCallbacks (ncaiEventCallbacks* callbacks, size_t size)
    {
        return funcs->GetEventCallbacks (this, callbacks, size);
    }

    ncaiError SetEventCallbacks (ncaiEventCallbacks* callbacks, size_t size)
    {
        return funcs->SetEventCallbacks (this, callbacks, size);
    }

    ncaiError SetEventNotificationMode (ncaiEventMode mode, ncaiEventKind event, ncaiThread thread)
    {
        return funcs->SetEventNotificationMode (this, mode, event, thread);
    }

    ncaiError SetBreakpoint (void* code_addr)
    {
        return funcs->SetBreakpoint (this, code_addr);
    }

    ncaiError ClearBreakpoint (void* code_addr)
    {
        return funcs->ClearBreakpoint (this, code_addr);
    }

    ncaiError SetWatchpoint (void* data_addr, size_t len, ncaiWatchpointMode mode)
    {
        return funcs->SetWatchpoint (this, data_addr, len, mode);
    }

    ncaiError ClearWatchpoint (void* data_addr)
    {
        return funcs->ClearWatchpoint (this, data_addr);
    }

    ncaiError SetStepMode (ncaiThread thread, ncaiStepMode mode)
    {
        return funcs->SetStepMode (this, thread, mode);
    }

    ncaiError NotifyFramePop (ncaiThread thread, void* frame_address)
    {
        return funcs->NotifyFramePop (this, thread, frame_address);
    }

#endif
};


#endif /* _NCAI_H_ */
