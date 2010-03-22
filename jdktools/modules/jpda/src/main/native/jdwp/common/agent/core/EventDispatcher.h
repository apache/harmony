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
 * EventDispatcher.h
 *
 * Produces JDWP event packets and writes them to the transport.
 * Operates in a separate agent thread.
 */

#ifndef _EVENT_DISPATCHER_H_
#define _EVENT_DISPATCHER_H_

#include "Util.h"
#include "jni.h"
#include "jvmti.h"
#include "jdwp.h"

#include "AgentBase.h"
#include "AgentMonitor.h"
#include "PacketParser.h"
#include "CommandHandler.h"


namespace jdwp {
    /**
     * The given class provides a separate thread that dispatches all event packets, 
     * suspends threads on events and performs deferred method invocation.
     */
    class EventDispatcher : public AgentBase {

    public:

        /**
         * A constructor.
         * Creates a new instance.
         * 
         * @param limit - maximum even queue length
         */
        EventDispatcher(size_t limit = 1024);

        /**
         * A destructor.
         * Destroys the given instance.
         */
        ~EventDispatcher() {}

        /**
         * Initializes this events before starting the thread.
         * 
         * @param jni - the JNI interface pointer
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void Init(JNIEnv *jni);

        /**
         * Starts the given thread.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        int Start(JNIEnv *jni);

        /**
         * Resets all data.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void Reset(JNIEnv *jni);

        /**
         * Stops the given thread.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void Stop(JNIEnv *jni);

        /**
         * Cleans all data after thread completion.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void Clean(JNIEnv *jni);

        /**
         * Turns on the flag to hold all events.
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void HoldEvents();

        /**
         * Turns off the flag to hold all events.
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void ReleaseEvents();

        /**
         * Informs that new session started.
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void NewSession();

        /**
         * Generates new ID for the event packet.
         */
        jint NewId() { return m_idCount++; }

        /**
         * Sends the event packet and suspends thread(s) according to suspend 
         * policy.
         *
         * @param jni       - the JNI interface pointer
         * @param ec        - the pointer to EventComposer
         * @param eventKind - the JDWP event kind
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void PostEventSet(JNIEnv *jni, EventComposer *ec, jdwpEventKind eventKind);

        /**
         * Suspends thread(s) after method invocation according to invocation 
         * options.
         *
         * @param jni     - the JNI interface pointer
         * @param handler - the pointer to SpecialAsyncCommandHandler
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        int PostInvokeSuspend(JNIEnv *jni, SpecialAsyncCommandHandler* handler);

        /**
         * Executes all handlers of methods that should be invoked on the 
         * specified thread.
         *
         * @param jni    - the JNI interface pointer
         * @param thread - the thread in which methods should be invoked
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void ExecuteInvokeMethodHandlers(JNIEnv *jni, jthread thread);

    protected:

        /**
         * Starts the given thread.
         *
         * @param jvmti - the JVMTI interface pointer
         * @param jni   - the JNI interface pointer
         * @param arg   - the function argument
         */
        static void JNICALL
            StartFunction(jvmtiEnv* jvmti, JNIEnv* jni, void* arg);

        /**
         * Performs the thread algorithm.
         *
         * @param jni - the JNI interface pointer
         */
        void Run(JNIEnv *jni);

        /**
         * Sends the event set and suspends thread(s) according to suspend 
         * policy.
         *
         * @param jni - the JNI interface pointer
         * @param ec  - the pointer to EventComposer
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        int SuspendOnEvent(JNIEnv* jni, EventComposer *ec);

        /**
         * Event queue type.
         */
        //typedef queue<EventComposer*,
        //    deque<EventComposer*, AgentAllocator<EventComposer*> > > EventQueue;
        typedef JDWPQueue EventQueue;

        /**
         * Event queue with event packets to be sent.
         */
        EventQueue m_eventQueue;

        /**
         * Limit for events in <code>m_eventQueue</code>.
         */
        size_t m_queueLimit;

        /**
         * Counter for event-packet IDs.
         */
        jint m_idCount;

        /**
         * Monitor for <code>m_eventQueue</code>.
         */
        AgentMonitor* m_queueMonitor;

        /**
         * Monitor for synchronization of events sending.
         */
        AgentMonitor* m_waitMonitor;

        /**
         * Monitor for synchronization of methods invocation.
         */
        AgentMonitor* m_invokeMonitor;

        /**
         * Monitor for synchronization of the thread end.
         */
        AgentMonitor* m_completeMonitor;

        /**
         * Flag to hold all event packets in <code>m_eventQueue</code>.
         */
        bool volatile m_holdFlag;

        /**
         * Flag to stop the thread run.
         */
        bool volatile m_stopFlag;

        /**
         * Flag to reset all data.
         */
        bool volatile m_resetFlag;

        /**
         * Associated thread object.
         */
        jthread m_threadObject;

    };

}

#endif // _EVENT_DISPATCHER_H_
