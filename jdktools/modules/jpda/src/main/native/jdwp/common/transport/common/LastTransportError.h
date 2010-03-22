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
/**
 * @file
 * LastTransportError.h
 *
 */

#ifndef _LASTTRANSPORTERROR_H
#define _LASTTRANSPORTERROR_H
#if defined(ZOS)
#define _XOPEN_SOURCE  500
#endif

#include "jni.h"
#include "jvmti.h"
#include "hythread.h"
#include "jdwpTransport.h"
typedef hythread_t ThreadId_t;

/**
 * The given class is a container for message and status code of the last 
 * transport error.
 */
class LastTransportError
{
public:
    /**
     * A constructor.
     *
     * @param messagePtr  - the pointer to the string with diagnostics for the 
     *                      last failed operation 
     * @param errorStatus - the error status for the last failed operation 
     * @param alloc       - the pointer to the function allocating the memory 
     *                      area 
     * @param free        - the pointer to the function deallocating the memory 
     *                      area 
     */
    LastTransportError(JavaVM *jvm, const char* messagePtr, int errorStatus, 
        void* (*alloc)(jint numBytes), void (*free)(void *buffer));

    /**
     * A destructor.
     */
    ~LastTransportError();

    /**
     * Inserts new Error data for the current thread.
     *
     * @param messagePtr  - the pointer to the string with diagnostics for the 
     *                      last failed operation
     * @param errorStatus - the error status for the last failed operation
     *
     * @return Returns JDWPTRANSPORT_ERROR_OUT_OF_MEMORY if out of memory, 
     *         otherwise JDWPTRANSPORT_ERROR_NONE.
     */
    jdwpTransportError insertError(const char* messagePtr, int errorStatus);

    /**
     * Adds a message prefix for the current thread.
     *
     * @param prefixPtr - the pointer to the string with the message prefix
     *
     * @return Returns JDWPTRANSPORT_ERROR_OUT_OF_MEMORY if out of memory, 
     *         otherwise JDWPTRANSPORT_ERROR_NONE.
     */
    jdwpTransportError addErrorMessagePrefix(const char* prefixPtr);

    /**
     * Returns the pointer to the string with diagnostics for the last failed 
     * operation in the current thread.
     *
     * @return Returns the pointer to the string with diagnostics for the last 
     *         failed operation.
     */
    char* GetLastErrorMessage();

    /**
     * Returns the error status for the last failed operation in the current 
     * thread.
     *
     * @return Returns the error status for the last failed operation.
     */
    int GetLastErrorStatus();

    void* operator new(size_t bytes, void* (*alloc)(jint numBytes), void (*free)(void *buffer));
    void operator delete(void* address);
    void operator delete(void* address, void* (*alloc)(jint numBytes), void (*free)(void *buffer));

private:
    JavaVM *m_jvm;
    ThreadId_t m_treadId;             // the thread Id
    const char* m_lastErrorMessage;   // diagnostics for the last failed operation 
    const char* m_lastErrorMessagePrefix;   // diagnostics prefix for the last failed operation  
    int m_lastErrorStatus;            // the error status for the last failed operation 
    LastTransportError* m_next;       // pointer to the next LastTransportError
    void* (*m_alloc)(jint numBytes);  // function allocating the memory area, provided by the agent 
    static void (*m_free)(void *buffer);     // function deallocating the memory area, provided by the agent

};

#endif // _LASTTRANSPORTERROR_H
