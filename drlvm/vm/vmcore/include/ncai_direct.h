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

#ifndef _NCAI_DIRECT_H_
#define _NCAI_DIRECT_H_

#include "jvmti_direct.h"
#include "lock_manager.h"
#include "ncai.h"
#include "open/hythread.h"


struct ncaiEventThread
{
    ncaiThread thread;
    ncaiEventThread* next;
};

// ncaiThread=(_ncaiThread*) will be used as hythread_t=(HyThread*)
struct _ncaiThread
{
    int dummy; // The structure will never be allocated
};

struct _ncaiModule{
    ncaiModuleInfo* info;
    _ncaiModule* next;
    bool isAlive;
};

class VMBreakInterface;

struct NCAIEnv
{
    const _ncai *functions;
    TIEnv *ti_env;
    Lock_Manager* env_lock;
    ncaiModule modules;
    ncaiEventCallbacks event_table;
    VMBreakInterface* brpt_intf;

    bool global_events[NCAI_MAX_EVENT_TYPE_VAL - NCAI_MIN_EVENT_TYPE_VAL + 1];
    ncaiEventThread *event_threads[NCAI_MAX_EVENT_TYPE_VAL - NCAI_MIN_EVENT_TYPE_VAL + 1];

    /**
     * Returns pointer to a callback function that was set by SetEventCallbacks
     * If no callback was set, this function returns NULL, in this case
     * no event should be sent.
     */
    void *get_event_callback(ncaiEventKind event_type)
    {
        return ((void **)&event_table)[event_type - NCAI_MIN_EVENT_TYPE_VAL];
    }

};

#ifdef __cplusplus
extern "C" {
#endif

    ncaiError JNICALL ncaiGetAllLoadedModules(ncaiEnv* env,
        jint* count_ptr,
        ncaiModule** modules_ptr);

    ncaiError JNICALL ncaiGetModuleInfo(ncaiEnv* env,
        ncaiModule module,
        ncaiModuleInfo* info_ptr);

    ncaiError JNICALL ncaiGetModuleClassLoader(ncaiEnv* env,
        ncaiModule module,
        jobject* classloader_ptr);

    ncaiError JNICALL ncaiIsMethodCompiled(ncaiEnv* env,
        jmethodID method,
        jboolean* is_compiled_ptr);

    ncaiError JNICALL ncaiGetMethodLocation(ncaiEnv* env,
        jmethodID method,
        void** address_ptr,
        size_t* size_ptr);

    ncaiError JNICALL ncaiFindJavaMethod(ncaiEnv* env,
        void* address,
        jmethodID* method_ptr);

    ncaiError JNICALL ncaiGetBytcodeLocation(ncaiEnv* env,
        void* address,
        jmethodID* method,
        jlocation* location_ptr);

    ncaiError JNICALL ncaiGetNativeLocation(ncaiEnv* env,
        jmethodID method,
        jlocation location,
        void** address_ptr);

    ncaiError JNICALL ncaiGetAllThreads(ncaiEnv* env,
        jint* count_ptr,
        ncaiThread** threads_ptr);

    ncaiError JNICALL ncaiGetThreadInfo(ncaiEnv* env,
        ncaiThread thread,
        ncaiThreadInfo* info_ptr);

    ncaiError JNICALL ncaiGetThreadHandle(ncaiEnv* env,
        jthread thread,
        ncaiThread* thread_ptr);

    ncaiError JNICALL ncaiGetThreadObject(ncaiEnv* env,
        ncaiThread thread,
        jthread* thread_ptr);

    ncaiError JNICALL ncaiSuspendThread(ncaiEnv* env,
        ncaiThread thread);

    ncaiError JNICALL ncaiResumeThread(ncaiEnv* env,
        ncaiThread thread);

    ncaiError JNICALL ncaiTerminateThread(ncaiEnv* env,
        ncaiThread thread);

    ncaiError JNICALL ncaiGetThreadState(ncaiEnv* env,
        ncaiThread thread,
        jint* state_ptr);

    ncaiError JNICALL ncaiGetFrameCount(ncaiEnv* env,
        ncaiThread thread,
        jint* count_ptr);

    ncaiError JNICALL ncaiGetStackTrace(ncaiEnv* env,
        ncaiThread thread,
        jint depth,
        ncaiFrameInfo* frame_buffer,
        jint* count_ptr);

    ncaiError JNICALL ncaiGetRegisterCount(ncaiEnv* env,
        jint* count_ptr);

    ncaiError JNICALL ncaiGetRegisterInfo(ncaiEnv* env,
        jint reg_number,
        ncaiRegisterInfo* info_ptr);

    ncaiError JNICALL ncaiGetRegisterValue(ncaiEnv* env,
        ncaiThread thread,
        jint reg_number,
        void* buf);

    ncaiError JNICALL ncaiSetRegisterValue(ncaiEnv* env,
        ncaiThread thread,
        jint reg_number,
        void* buf);

    ncaiError JNICALL ncaiReadMemory(ncaiEnv* env,
        void* addr,
        size_t size,
        void* buf);

    ncaiError JNICALL ncaiWriteMemory(ncaiEnv* env,
        void* addr,
        size_t size,
        void* buf);

    ncaiError JNICALL ncaiGetSignalCount(ncaiEnv* env,
        jint* count_ptr);

    ncaiError JNICALL ncaiGetSignalInfo(ncaiEnv* env,
        jint signal,
        ncaiSignalInfo* info_ptr);

    ncaiError JNICALL ncaiGetJvmtiEnv(ncaiEnv* env,
        jvmtiEnv** jvmti_env_ptr);

    ncaiError JNICALL ncaiGetVersion(ncaiEnv* env,
        jint* version_ptr);

    ncaiError JNICALL ncaiGetErrorName(ncaiEnv* env,
        ncaiError err,
        const char** name_ptr);

    ncaiError JNICALL ncaiGetPotentialCapabilities(ncaiEnv* env,
        ncaiCapabilities* caps_ptr);

    ncaiError JNICALL ncaiGetCapabilities(ncaiEnv* env,
        ncaiCapabilities* caps_ptr);

    ncaiError JNICALL ncaiAddCapabilities(ncaiEnv* env,
        ncaiCapabilities* caps_ptr);

    ncaiError JNICALL ncaiRelinquishCapabilities(ncaiEnv* env,
        ncaiCapabilities* caps_ptr);

    ncaiError JNICALL ncaiGetEventCallbacks(ncaiEnv* env,
        ncaiEventCallbacks* callbacks,
        size_t size);

    ncaiError JNICALL ncaiSetEventCallbacks(ncaiEnv* env,
        ncaiEventCallbacks* callbacks,
        size_t size);

    ncaiError JNICALL ncaiSetEventNotificationMode(ncaiEnv* env,
        ncaiEventMode mode,
        ncaiEventKind event,
        ncaiThread thread);

    ncaiError JNICALL ncaiSetBreakpoint(ncaiEnv* env,
        void* code_addr);

    ncaiError JNICALL ncaiClearBreakpoint(ncaiEnv* env,
        void* code_addr);

    ncaiError JNICALL ncaiSetWatchpoint(ncaiEnv* env,
        void* data_addr,
        size_t len,
        ncaiWatchpointMode mode);

    ncaiError JNICALL ncaiClearWatchpoint(ncaiEnv* env,
        void* data_addr);

    ncaiError JNICALL ncaiSetStepMode(ncaiEnv* env,
        ncaiThread thread,
        ncaiStepMode mode);

    ncaiError JNICALL ncaiNotifyFramePop(ncaiEnv* env,
        ncaiThread thread,
        void* frame_address);

/*    void JNICALL ncaiStep(ncaiEnv* env,
        ncaiThread thread,
        void* addr);

    void JNICALL ncaiBreakpoint(ncaiEnv* env,
        ncaiThread thread,
        void* addr);

    void JNICALL ncaiWatchpoint(ncaiEnv* env,
        ncaiThread thread,
        void* code_addr,
        void* data_addr);

    void JNICALL ncaiSignal(ncaiEnv* env,
        ncaiThread thread,
        void* addr,
        jint signal,
        jboolean is_internal,
        jboolean* is_handled);

    void JNICALL ncaiException(ncaiEnv* env,
        ncaiThread thread,
        void* addr,
        void* exception);

    void JNICALL ncaiModuleLoad(ncaiEnv* env,
        ncaiThread thread,
        ncaiModule module);

    void JNICALL ncaiModuleUnload(ncaiEnv* env,
        ncaiThread thread,
        ncaiModule module);

    void JNICALL ncaiMethodEntry(ncaiEnv* env,
        ncaiThread thread,
        void* addr);

    void JNICALL ncaiMethodExit(ncaiEnv* env,
        ncaiThread thread,
        void* addr);

    void JNICALL ncaiFramePop(ncaiEnv* env,
        ncaiThread thread,
        void* addr);

    void JNICALL ncaiConsoleInput(ncaiEnv* env, char** message);

    void JNICALL ncaiConsoleOutput(ncaiEnv* env, char* message);

    void JNICALL ncaiDebugMessage(ncaiEnv* env, char* message);*/

#ifdef __cplusplus
}
#endif
#endif /* _NCAI_DIRECT_H_ */

