 /*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * Includes and defines for the Shared Memory Transport module
 */

#ifndef _SHAREDMEMTRANSPORT_H
#define _SHAREDMEMTRANSPORT_H

#include <windows.h>

#include "jni.h"
#include "jdwpTransport.h"
#include "LastTransportError.h"

#define DEFAULT_ADDRESS_NAME "sharedmem"
#define MUTEX_SUFFIX "mutex"
#define ACCEPT_EVENT_SUFFIX "acceptEvent"
#define ATTACH_EVENT_SUFFIX "attachEvent"
#define INIT_MEM_SIZE 4096
#define NAME_SIZE 75
#define JDWP_HANDSHAKE "JDWP-Handshake"

static jdwpTransportError JNICALL ShMemTran_GetCapabilities(jdwpTransportEnv* env, JDWPTransportCapabilities* capabilitiesPtr);
static jdwpTransportError JNICALL ShMemTran_Attach(jdwpTransportEnv* env, const char* address, jlong attachTimeout, jlong handshakeTimeout);
static jdwpTransportError JNICALL ShMemTran_StartListening(jdwpTransportEnv* env, const char* address, char** actualAddress);
static jdwpTransportError JNICALL ShMemTran_StopListening(jdwpTransportEnv* env);
static jdwpTransportError JNICALL ShMemTran_Accept(jdwpTransportEnv* env, jlong acceptTimeout, jlong handshakeTimeout);
static jboolean JNICALL ShMemTran_IsOpen(jdwpTransportEnv* env);
static jdwpTransportError JNICALL ShMemTran_Close(jdwpTransportEnv* env);
static jdwpTransportError JNICALL ShMemTran_ReadPacket(jdwpTransportEnv* env, jdwpPacket* packet);
static jdwpTransportError JNICALL ShMemTran_WritePacket(jdwpTransportEnv* env, const jdwpPacket* packet);
static jdwpTransportError JNICALL ShMemTran_GetLastError(jdwpTransportEnv* env, char** message);
extern "C" JNIEXPORT jint JNICALL jdwpTransport_OnLoad(JavaVM *vm, jdwpTransportCallback* callback, jint version, jdwpTransportEnv** env);
extern "C" JNIEXPORT void JNICALL jdwpTransport_UnLoad(jdwpTransportEnv** env);

/* This structure is shared between VMs */
typedef struct SharedMemInit_struct {
    char mutexName[NAME_SIZE];
    char acceptEventName[NAME_SIZE];
    char attachEventName[NAME_SIZE];
    jboolean isListening;
    jboolean isAccepted;
    jint reserved1;  // TODO: not sure what this field is for
    jint acceptPid;
    jint reserved2;  // TODO: not sure what this field is for
    jint attachPid;
} SharedMemInit;

/* Contains VM local variables */
typedef struct LocalMemInit_struct {
    HANDLE initHandle;
    HANDLE mutexHandle;
    HANDLE acceptEventHandle;
    HANDLE attachEventHandle;    
} LocalMemInit;

struct internalEnv {
    JavaVM *jvm;                    // the JNI invocation interface, provided 
                                    // by the agent 
    void* (*alloc)(jint numBytes);  // function for allocating an area of memory, 
                                    // provided by the agent 
    void (*free)(void *buffer);     // function for deallocating an area of memory, 
                                    // provided by the agent
    LocalMemInit *localInit;        // Shared memory initialization structure
    LastTransportError *lastError;  // pointer to the last transport error
};

#endif /* _SHAREDMEMTRANSPORT_H */
