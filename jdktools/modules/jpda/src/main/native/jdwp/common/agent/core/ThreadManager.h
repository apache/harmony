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
 * @author Vitaly A. Provodin, Pavel N. Vyssotski
 */

/**
 * @file
 * ThreadManager.h
 *
 */

#ifndef _THREAD_MANAGER_H_
#define _THREAD_MANAGER_H_

#include <vector>

#include "jni.h"
#include "jvmti.h"
#include "AgentBase.h"
#include "AgentException.h"
#include "AgentMonitor.h"
#include "AgentAllocator.h"
#include "CommandHandler.h"

namespace jdwp {

    class ThreadInfo;

    typedef vector<ThreadInfo*, AgentAllocator<ThreadInfo*> > ThreadInfoList;

    typedef vector<SpecialAsyncCommandHandler*,
        AgentAllocator<SpecialAsyncCommandHandler*> > ExecList;

    typedef ExecList::iterator ExecListIterator;

    /**
     * Manages list of known threads, provides agent specific thread info,
     * such as debug, suspend count, pending step, invoke, etc.
     * Provides services to support stop, interrupt, suspend/resume.
     */
    class ThreadManager : public AgentBase {

    public:

        /**
         * Constructs a new <code>ThreadManager</code> object.
         */
        ThreadManager() throw() : 
          m_thrdmgrMonitor(0), 
          m_execMonitor(0),
          m_stepMonitor(0),
          m_popFramesMonitor(0),
          m_popFramesThread(0)
        {}

        ~ThreadManager();

        /**
         * Initializes <code>ThreadManager</code> object.
         *
         * @param jni - the JNI interface pointer
         */
        void Init(JNIEnv *jni) throw();

        /**
         * Cleans the <code>ThreadManager</code> object.
         *
         * @param jni - the JNI interface pointer
         */
        void Clean(JNIEnv *jni) throw();

        /**
         * Reinitializes <code>ThreadManager</code> object.
         * All suspended threads resumed as many times as necessary and the
         * internal list is cleaned.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception AgentException is thrown in case of a 
         *            universal error, with the corresponded error code.
         */
        void Reset(JNIEnv *jni) throw(AgentException);

        /**
         * Creates a new agent thread and starts the specified function in it.
         * thread.
         *
         * @param jni      - the JNI interface pointer
         * @param proc     - the start function
         * @param arg      - the argument to the start function
         * @param priority - the priority of the started thread
         * @param name     - the default parameter; if defined, then the created
         *                   thread has a specified name
         * @param name     - the default parameter; if defined, then the created
         *                   thread is associated with this object
         *
         * @return Returns the <code>jthread</code> object associated with started 
         * thread.
         *
         * @exception OutOfMemoryException is thrown if the system 
         *            runs out of memory.
         * @exception AgentException JVMTI_ERROR_INVALID_PRIORITY 
         *            is thrown if the <code>priority</code> has a wrong value.
         * @exception AgentException JVMTI_ERROR_NULL_POINTER is thrown if
         *            <code>proc</code> is <code>NULL</code>.  
         * @exception InternalErrorException is thrown in any other cases.
         */
        jthread RunAgentThread(JNIEnv *jni, jvmtiStartFunction proc,
                                const void *arg, jint priority,
                                const char *name = 0,
                                jthread thread = 0) throw(AgentException);

        /**
         * Adds an information about the thread parameter into
         * the internal list.
         *
         * @param jni            - the JNI interface pointer
         * @param thread         - the thread to be added
         * @param isAgentThread  - if the added thread is an agent's one, then
         *                         <code>TRUE</code> is passed, otherwise 
         *                         <code>FALSE</code>
         * @param isOnEvent      - whether the thread was suspended on event
         *
         * @return Pointer to the new record in the thread list.
         *
         * @exception OutOfMemoryException is thrown no more memory
         *            is available for allocation.
         */
        ThreadInfo* AddThread(JNIEnv *jni, jthread thread,
                    bool isAgentThread = false, bool isOnEvent = false) throw(AgentException);
        /**
         * Suspends the specified thread.
         * If the specified thread is an agent thread, calling this method 
         * is ineffective.
         * If the suspend count is equal to <code>0</code> then the thread will
         * be suspended, otherwise the suspend count will be incremented.
         *
         * @param jni        - the JNI interface pointer
         * @param thread the - thread to be suspended
         * @param isOnEvent  - <code>TRUE</code> if suspended on event
         *
         * @exception AgentException(JVMTI_ERROR_INVALID_THREAD) 
         *            is thrown if the thread is an agent thread or not a thread 
         *            object.
         * @exception AgentException(JVMTI_ERROR_THREAD_NOT_ALIVE) 
         *            is thrown if thread has not been started or is dead.
         */
        void Suspend(JNIEnv *jni, jthread thread, bool isOnEvent = false) throw(AgentException);

        /**
         * Resumes the specified thread.
         * If the specified thread is an agent thread, calling this method has
         * no effect.
         * If the suspend count is equal to <code>0</code> then the thread will
         * continue to execute, otherwise the suspend count will be decremented.
         *
         * @param jni    - the JNI interface pointer
         * @param thread - the thread to be resumed
         *
         * @exception AgentException(JVMTI_ERROR_INVALID_THREAD)
         *            is thrown if the thread is an agent thread or not a thread 
         *            object.
         * @exception AgentException(JVMTI_ERROR_THREAD_NOT_ALIVE) 
         *            is thrown if thread has not been started or is dead.
         */
        void Resume(JNIEnv *jni, jthread thread) throw(AgentException);

        /**
         * Suspends all non-agent threads.
         *
         * @param jni           - the JNI interface pointer
         * @param threadOnEvent - thread suspended on event or NULL
         *
         * @exception AgentException is thrown in case an universal 
         *            error, with the corresponded error code.
         */
        void SuspendAll(JNIEnv *jni, jthread threadOnEvent = 0) throw(AgentException);

        /**
         * Resumes all non-agent threads.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception AgentException is thrown in case an universal 
         *             error, with the corresponded error code.
         */
        void ResumeAll(JNIEnv *jni)                 throw(AgentException);

        /**
         * Interrupts the specified thread.
         * 
         * @param jni    - the JNI interface pointer
         * @param thread - the thread to be interrupted
         *
         * @exception AgentException(JVMTI_ERROR_INVALID_THREAD)
         *            is thrown if the thread is not a thread object.
         * @exception AgentException(JVMTI_ERROR_THREAD_NOT_ALIVE) 
         *            is thrown if thread has not been started or is dead.
         */
        void Interrupt(JNIEnv *jni, jthread thread) throw(AgentException);

        /**
         * Stops the specified thread.
         * 
         * @param jni       - the JNI interface pointer
         * @param thread    - the thread to be interrupted
         * @param throwable - the asynchronous exception object sent to the thread
         *
         * @exception AgentException(JVMTI_ERROR_INVALID_THREAD) 
         *            is thrown if the thread is not a thread object.
         * @exception AgentException(JVMTI_ERROR_THREAD_NOT_ALIVE) 
         *            is thrown if thread has not been started or is dead.
         * @exception AgentException(JVMTI_ERROR_INVALID_OBJECT) 
         *            is thrown if exception is not an object.
         */
        void Stop(JNIEnv *jni, jthread thread, jobject throwable)
            throw(AgentException);
        
        /**
         * Join the specified thread.
         * 
         * @param jni       - the JNI interface pointer
         * @param thread    - the thread to be joined
         * 
         * @exception AgentException is thrown in case an universal 
         *            error, with the corresponded error code.
         */
        void Join(JNIEnv *jni, jthread thread);

        /**
         * Checks if the specified thread is an agent thread.
         *
         * @param jni    - the JNI interface pointer
         * @param thread - the thread to be checked
         *
         * @return Returns <code>TRUE</code> if the thread is an
         *         agent thread, otherwise <code>FALSE</code>.
         */
        bool IsAgentThread(JNIEnv *jni, jthread thread);

        /**
         * Returns a suspend count for the specified thread.
         *
         * @param jni    - the JNI interface pointer
         * @param thread - the thread to be checked
         *
         * @return Returns a suspend count for the specified thread.
         */
        jint GetSuspendCount(JNIEnv *jni, jthread thread);

        /**
         * Checks if the specified thread is suspended on event.
         *
         * @param jni  - the JNI interface pointer
         * @param thrd - the thread to be checked
         *
         * @return Returns <code>TRUE</code> if the thread is suspended on 
         *         event, otherwise <code>FALSE</code>.
         *
         * @exception AgentException(JVMTI_ERROR_INVALID_THREAD)
         *            is thrown if the thread is not a thread object.
         */
        bool IsSuspendedOnEvent(JNIEnv *jni, jthread thrd) throw (AgentException);

        /**
         * Checks if the specified thread is really suspended.
         *
         * @param thrd - the thread to be checked
         *
         * @return Returns <code>TRUE</code> if the thread is suspended,
         *         otherwise <code>FALSE</code>.
         *
         * @exception AgentException(JVMTI_ERROR_INVALID_THREAD) 
         *            is thrown if the thread is not a thread object.
         */
        bool IsSuspended(jthread thrd) throw (AgentException);

        /**
         * Registers instance of <code>SpecialAsyncCommandHandler</code> for 
         * the deferred invocation of handler's method 
         * <code>ExecuteDeferredFunc()</code> in the specified thread, 
         * and resumes thread(s) according to invocation options.
         *
         * @param jni     - the JNI interface pointer
         * @param handler - <code>SpecialAsyncCommandHandler</code> instance 
         *                  reference
         *
         * @exception AgentException(JVMTI_ERROR_THREAD_NOT_SUSPENDED)
         *            is thrown if the specified thread has not been suspended by 
         *            an event.
         */
        void RegisterInvokeHandler(JNIEnv *jni, SpecialAsyncCommandHandler* handler)
            throw (AgentException);

        /**
         * Finds an instance of <code>SpecialAsyncCommandHandler</code> from 
         * registered for the given thread to invoke the handler's method 
         * <code>ExecuteDeferredFunc()<code>.
         *
         * @param jni     - the JNI interface pointer
         * @param thread  - thread corresponding to invoke handler
         *
         * @return The first handler instance found or NULL.
         *
         * @exception AgentException is thrown if any error occurs.
         */
        SpecialAsyncCommandHandler* FindInvokeHandler(JNIEnv *jni,
            jthread thread) throw (AgentException);

        /**
         * Used for internal purposes. Handles <code>Step Event</code>, 
         * during <code>PopFrame</code> process on specified thread.
         *
         * @param jni      - the JNI interface pointer
         * @param thread   - thread in which <code>SingleStep</code> was made
         * @param method   - method in which <code>SingleStep</code> was made
         * @param location - location in which <code>SingleStep</code> was made
         *
         * @exception AgentException is thrown if any error occurs.
         */
        void HandleInternalSingleStep(JNIEnv* jni, jthread thread, jmethodID method, jlocation location) throw(AgentException);

        /**
         * Pops <code>framesToPop</code> number of frames on the specified thread.
         *
         * @param jni         - the JNI interface pointer
         * @param framesToPop - number of frames to pop
         * @param thread      - thread which frames are popped
         *
         * @exception AgentException is thrown if any error occurs.
         */
        void PerformPopFrames(JNIEnv* jni, jint framesToPop, jthread thread)
            throw(AgentException);
 
        /**
         * Checks if <code>PopFrames</code> command is executed on the specified 
         * thread.
         *
         * @param jni    - the JNI interface pointer
         * @param thread - the thread to check
         */
        jboolean IsPopFramesProcess(JNIEnv* jni, jthread thread);

    private:

        /**
         * List of suspended threads and agent threads.
         */
        ThreadInfoList  m_threadInfoList;
        
        /**
         * Monitor for <code>m_threadInfoList</code>.
         */
        AgentMonitor    *m_thrdmgrMonitor;

        /**
         * List of registered handlers for deferred method invocation.
         */
        ExecList m_execList;
        
        /**
         * Monitor for <code>m_execList</code>.
         */
        AgentMonitor* m_execMonitor;

        /**
         * Thread, on which the <code>PopFrame</code> command is executed.
         */
        volatile jthread m_popFramesThread;


        AgentMonitor* m_stepMonitor;
        AgentMonitor* m_popFramesMonitor;
        
        volatile bool m_popFramesMonitorReleased;
        volatile bool m_stepMonitorReleased;
          
        /**
         * Checks native frames among <code>numberOfFrames</code> of specified thread.
         *
         * @param thread         - hread to check
         * @param numberOfFrames - number of frames to check
         *
         * @exception AgentException(JDWP_ERROR_NATIVE_METHOD)
         *            is thrown if native frame is found.
         */
        void CheckNativeFrameExistence(jthread thread, jint numberOfFrames)
            throw(AgentException);
        
        /**
         * Creates a new agent thread and adds it to the list.
         *
         * @param jni        - the JNI interface pointer
         * @param threadName - the default parameter, if defined then the created
         *                     thread has specified name
         *
         * @return Returns the <code>jthread</code> object that holds the created
         *         thread.
         *
         * @exception OutOfMemoryException is thrown if the system 
         *            runs out of memory.
         * @exception InternalErrorException is thrown in any other
         *            cases.
         */
        jthread CreateAgentThread(JNIEnv *jni, const char *name = 0)
            throw(AgentException);

        // synchronized
        void ClearExecList(JNIEnv* jni) throw();

        // synchronized
        void ClearThreadList(JNIEnv *jni);

        // not synchronized
        void InternalResume(JNIEnv *jni, jthread thread, bool ignoreInternal) throw(AgentException);

        // not synchronized
        void InternalSuspend(JNIEnv *jni, jthread thread, bool ignoreInternal, bool isOnEvent = false) throw(AgentException);

    };//class ThreadManager

}//jdwp

#endif //_THREAD_MANAGER_H_
