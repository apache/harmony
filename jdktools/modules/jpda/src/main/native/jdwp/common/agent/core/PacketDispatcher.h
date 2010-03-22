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
 * @author Vitaly A. Provodin
 */

/**
 * @file
 * PacketDispatcher.h
 *
 * Reads command packets from the transport, wraps and passes them to
 * CommandDispatcher.
 * Operates in a separate agent thread.
 */

#ifndef _PACKET_DISPATCHER_H_
#define _PACKET_DISPATCHER_H_

#include "AgentBase.h"
#include "AgentException.h"
#include "PacketParser.h"
#include "CommandDispatcher.h"


namespace jdwp {

    class AgentMonitor;

    /**
     * Unwraps JDWP commands and passes them to <code>CommandDispatcher</code>
     * for executing.
     * The JDWP agent has only one object of <code>PacketDispatcher</code> that
     * is started at the startup routine in a separate thread.
     * <code>PacketDispatcher</code> reads command packets from the active
     * JDWP transport via <code>TransportManager</code>, unwraps them and passes
     * to <code>CommandDispatcher</code>. In case <code>CommandDispatcher</code>
     * can not execute the obtained JDWP command, <code>PacketDispatcher</code>
     * composes the corresponding reply packet, according to the JDWP 
     * specification, and sends it back to the JDWP transport.
     * 
     * @see CommandDispatcher
     * @see TransportManager
     */
    class PacketDispatcher : public AgentBase
    {
    public:

        /**
         * Constructs a new <code>PacketDispatcher</code> object.
         */
        PacketDispatcher() throw();

        /**
         * Initializes the <code>PacketDispatcher</code>'s thread.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void Init(JNIEnv *jni) throw(AgentException);

        /**
         * Starts the <code>PacketDispatcher</code>'s thread.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void Start(JNIEnv *jni) throw(AgentException);

        /**
         * Stops the <code>PacketDispatcher</code>'s thread.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void Stop(JNIEnv *jni) throw(AgentException);

        /**
         * Resets the <code>PacketDispatcher</code> object.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void Reset(JNIEnv *jni) throw(AgentException);

        /**
         * Cleans the <code>PacketDispatcher</code> object.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void Clean(JNIEnv *jni) throw(AgentException);

        /**
         * Resets all agent modules.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
        void ResetAll(JNIEnv *jni) throw(AgentException);

        /**
         * Shutdowns all agent modules.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception If any error occurs, <code>AgentException</code> is thrown.
         */
//        void ShutdownAll(JNIEnv *jni) throw(AgentException);

        /**
         * Determines if the <code>PacketDispatcher</code> object is already
         * started.
         */
        bool IsProcessed() const {return m_isProcessed;}

    protected:

        /**
         * Main execution function of the dispatcher processing
         * commands from the debugger.
         *
         * @param jni - the JNI interface pointer
         */
        void Run(JNIEnv *jni);

        /**
         * Starts <code>PacketDispatcher</code> thread.
         * The given function is passed as the <code>proc</code> parameter of
         * <code>ThreadManager::RunAgentThread</code>. The <code>arg</code>
         * parameter must be a pointer to the agent's
         * <code>PacketDisptacher</code> object.
         *
         * @param jvmti - the JVMTI interface pointer
         * @param jni   - the JNI interface pointer
         * @param arg   - the agent's <code>PacketDisptacher</code> object pointer
         */
        static void JNICALL
            StartFunction(jvmtiEnv* jvmti, JNIEnv* jni, void* arg);

    private:
        volatile bool       m_isProcessed;
        volatile bool       m_isRunning;
        CommandParser       m_cmdParser;
        CommandDispatcher   m_cmdDispatcher;
        AgentMonitor*       m_completionMonitor;
        AgentMonitor*       m_executionMonitor;
        jthread             m_threadObject;

    };//class PacketDispatcher

}//jdwp

#endif //_PACKET_DISPATCHER_H_
