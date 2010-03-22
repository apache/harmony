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
#ifndef _NCAI_TYPES_H_
#define _NCAI_TYPES_H_

#include <stddef.h>
#include "jvmti_types.h"

#ifdef __cplusplus
extern "C"
{
#endif

    struct _ncai;
    struct ncaiEnv_struct;

#ifdef __cplusplus
    typedef ncaiEnv_struct ncaiEnv;
#else
    typedef const struct _ncai* ncaiEnv;
#endif


    typedef enum { //TBD - work on NCAI errors
        // Universal errors
        NCAI_ERROR_NONE = 0,
        NCAI_ERROR_NULL_POINTER = 100,
        NCAI_ERROR_ILLEGAL_ARGUMENT = 103,
        NCAI_ERROR_OUT_OF_MEMORY = 110,
        NCAI_ERROR_ACCESS_DENIED = 111,
        NCAI_ERROR_UNATTACHED_THREAD = 115,
        NCAI_ERROR_INVALID_ENVIRONMENT = 116,
        NCAI_ERROR_INTERNAL = 113,
        // function specific
        NCAI_ERROR_INVALID_THREAD = 10,
        NCAI_ERROR_THREAD_NOT_SUSPENDED = 13,
        NCAI_ERROR_THREAD_SUSPENDED = 14,
        NCAI_ERROR_THREAD_NOT_ALIVE = 15,
        NCAI_ERROR_INVALID_METHOD = 23,
        NCAI_ERROR_INVALID_LOCATION = 24,
        NCAI_ERROR_DUPLICATE = 40,
        NCAI_ERROR_NOT_FOUND = 41,
        NCAI_ERROR_NOT_AVAILABLE = 98,
        NCAI_ERROR_INTERPRETER_USED = 201,
        NCAI_ERROR_NOT_COMPILED = 211,
        NCAI_ERROR_INVALID_ADDRESS = 212,
        NCAI_ERROR_INVALID_MODULE = 213,
    } ncaiError;

    typedef enum {
        NCAI_MODULE_JNI_LIBRARY,
        NCAI_MODULE_VM_INTERNAL,
        NCAI_MODULE_OTHER
    } ncaiModuleKind;

    typedef enum {
        NCAI_SEGMENT_UNKNOWN,
        NCAI_SEGMENT_CODE,
        NCAI_SEGMENT_DATA
    } ncaiSegmentKind;

    typedef struct {
        ncaiSegmentKind kind;
        void* base_address;
        size_t size;
    } ncaiSegmentInfo;

    typedef struct {
        ncaiModuleKind kind;
        char* name;
        char* filename;
        ncaiSegmentInfo* segments;
        size_t segment_count;
    } ncaiModuleInfo;

    struct _ncaiModule;
    typedef _ncaiModule* ncaiModule;

    struct _ncaiThread;
    typedef _ncaiThread* ncaiThread;

    typedef enum {
        NCAI_THREAD_JAVA,
        NCAI_THREAD_VM_INTERNAL,
        NCAI_THREAD_OTHER
    } ncaiThreadKind;

    typedef struct {
        ncaiThreadKind kind;
        char* name;
    } ncaiThreadInfo;


    typedef struct {
        jint java_frame_depth;
        void* pc_address;
        void* return_address;
        void* frame_address;
        void* stack_address;
    } ncaiFrameInfo;


    typedef struct {
        char* name;
        jint size;
    } ncaiRegisterInfo;


    typedef struct {
        char* name;
    } ncaiSignalInfo;


    typedef struct {
    } ncaiCapabilities;


    typedef void (JNICALL * ncaiThreadStart)
        (ncaiEnv* env, ncaiThread thread);
    typedef void (JNICALL * ncaiThreadEnd)
        (ncaiEnv* env, ncaiThread thread);
    typedef void (JNICALL * ncaiBreakpoint)
        (ncaiEnv *env, ncaiThread thread, void *addr);
    typedef void (JNICALL * ncaiStep)
        (ncaiEnv *env, ncaiThread thread, void *addr);
    typedef void (JNICALL * ncaiWatchpoint)
        (ncaiEnv *env, ncaiThread thread, void *code_addr, void *data_addr);
    typedef void (JNICALL * ncaiMethodEntry)
        (ncaiEnv* env, ncaiThread thread, void* addr);
    typedef void (JNICALL * ncaiMethodExit)
        (ncaiEnv* env, ncaiThread thread, void* addr);
    typedef void (JNICALL * ncaiFramePop)
        (ncaiEnv* env, ncaiThread thread, void* addr);
    typedef void (JNICALL * ncaiSignal)
        (ncaiEnv *env, ncaiThread thread, void *addr, jint signal, jboolean is_internal, jboolean* is_handled);
    typedef void (JNICALL * ncaiException)
        (ncaiEnv *env, ncaiThread thread, void *addr, void *exception);
    typedef void (JNICALL * ncaiModuleLoad)
        (ncaiEnv *env, ncaiThread thread, ncaiModule module);
    typedef void (JNICALL * ncaiModuleUnload)
        (ncaiEnv *env, ncaiThread thread, ncaiModule module);
    typedef void (JNICALL * ncaiConsoleInput)
        (ncaiEnv *env, char **message);
    typedef void (JNICALL * ncaiConsoleOutput)
        (ncaiEnv *env, char *message);
    typedef void (JNICALL * ncaiDebugMessage)
        (ncaiEnv *env, char *message);

    typedef enum {
        NCAI_ENABLE = 1,
        NCAI_DISABLE = 0
    } ncaiEventMode;

    typedef enum {
        NCAI_MIN_EVENT_TYPE_VAL = 1,
        NCAI_EVENT_THREAD_START = NCAI_MIN_EVENT_TYPE_VAL,
        NCAI_EVENT_THREAD_END,
        NCAI_EVENT_BREAKPOINT,
        NCAI_EVENT_STEP,
        NCAI_EVENT_WATCHPOINT,
        NCAI_EVENT_METHOD_ENTRY,
        NCAI_EVENT_METHOD_EXIT,
        NCAI_EVENT_FRAME_POP,
        NCAI_EVENT_SIGNAL,
        NCAI_EVENT_EXCEPTION,
        NCAI_EVENT_MODULE_LOAD,
        NCAI_EVENT_MODULE_UNLOAD,
        NCAI_EVENT_CONSOLE_INPUT,
        NCAI_EVENT_CONSOLE_OUTPUT,
        NCAI_EVENT_DEBUG_OUTPUT,
        NCAI_MAX_EVENT_TYPE_VAL = NCAI_EVENT_DEBUG_OUTPUT
    } ncaiEventKind;

    typedef struct
    {
        ncaiThreadStart ThreadStart;
        ncaiThreadEnd ThreadEnd;
        ncaiBreakpoint Breakpoint;
        ncaiStep Step;
        ncaiWatchpoint Watchpoint;
        ncaiMethodEntry MethodEntry;
        ncaiMethodExit MethodExit;
        ncaiFramePop FramePop;
        ncaiSignal Signal;
        ncaiException Exception;
        ncaiModuleLoad ModuleLoad;
        ncaiModuleUnload ModuleUnload;
        ncaiConsoleInput ConsoleInput;
        ncaiConsoleOutput ConsoleOutput;
        ncaiDebugMessage DebugMessage;
    }ncaiEventCallbacks;

    typedef enum {
        NCAI_WATCHPOINT_READ,
        NCAI_WATCHPOINT_WRITE,
        NCAI_WATCHPOINT_ACCESS
    } ncaiWatchpointMode;

    typedef enum {
        NCAI_STEP_OFF,
        NCAI_STEP_INTO,
        NCAI_STEP_OVER,
        NCAI_STEP_OUT
    } ncaiStepMode;


#ifdef __cplusplus
}               /* extern "C" { */
#endif

#endif /* _NCAI_TYPES_H_ */
