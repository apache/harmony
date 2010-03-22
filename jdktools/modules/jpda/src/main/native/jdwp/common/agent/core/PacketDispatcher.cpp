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
#include "PacketDispatcher.h"
#include "TransportManager.h"
#include "AgentException.h"
#include "ThreadManager.h"
#include "EventDispatcher.h"
#include "ObjectManager.h"
#include "ClassManager.h"
#include "RequestManager.h"
#include "OptionParser.h"

using namespace jdwp;

//-----------------------------------------------------------------------------

PacketDispatcher::PacketDispatcher() throw()
    :AgentBase()
{
    m_isProcessed = false;
    m_completionMonitor = 0;
    m_executionMonitor = 0;
    m_threadObject = 0;
}

//-----------------------------------------------------------------------------

void
PacketDispatcher::Init(JNIEnv *jni) throw(AgentException)
{
    JDWP_TRACE_ENTRY("Init(" << jni << ")");

    m_completionMonitor = new AgentMonitor("_agent_Packet_Dispatcher_completion");
    m_executionMonitor = new AgentMonitor("_agent_Packet_Dispatcher_execution");
}

void
PacketDispatcher::Start(JNIEnv *jni) throw(AgentException)
{
    JDWP_TRACE_ENTRY("Start(" << jni << ")");

    JDWP_ASSERT(!m_isProcessed);

    try
    {
        m_threadObject = jni->NewGlobalRef(GetThreadManager().RunAgentThread(jni, StartFunction, this,
            JVMTI_THREAD_MAX_PRIORITY, "_jdwp_PacketDispatcher"));
    }
    catch (const AgentException& e)
    {
        JDWP_ASSERT(e.ErrCode() != JDWP_ERROR_NULL_POINTER);
        JDWP_ASSERT(e.ErrCode() != JDWP_ERROR_INVALID_PRIORITY);

        throw e;
    }

}

//-----------------------------------------------------------------------------

void
PacketDispatcher::Run(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY("Run(" << jni << ")");

    try {
        MonitorAutoLock lock(m_completionMonitor JDWP_FILE_LINE);
        TransportManager &transport = GetTransportManager();
        
        try
        {
            // run multiplle sessions in a loop
            for (; ;)
            {
                // connect for new session
                JDWP_TRACE_PROG("Run: start new session");
                try
                {
                    transport.Connect();
                }
                catch (const TransportException& e)
                {
                    JDWP_TRACE_PROG("Run: Exception in connection: "
                                    << e.what() << " [" << e.ErrCode() 
                                    << "/" << e.TransportErrorCode() << "]");
                    if (!IsDead()) {
                        JDWP_DIE(e.what() << " [" << e.ErrCode() << "/"
                                    << e.TransportErrorCode() << "]: " 
                                    << GetTransportManager().GetLastTransportError());
                    }
                    break;
                }
        
                // start session and execute commands
                try
                {
                    // inform that new session started
                    GetEventDispatcher().NewSession();
        
                    // add internal request for automatic VMDeath event with no modifiers
                    GetRequestManager().AddInternalRequest(jni,
                        new AgentEventRequest(JDWP_EVENT_VM_DEATH, JDWP_SUSPEND_NONE));
        
                    // release events
                    GetEventDispatcher().ReleaseEvents();
        
                    // read and execute commands
                    m_isProcessed = true;
                    while (m_isProcessed)
                    {
                        // read command
                        try {
                            JDWP_TRACE_PROG("Run: handle next command");
                            m_cmdParser.ReadCommand();
                            if (m_cmdParser.command.GetLength() == 0)
                            break;
                        }
                        catch (const TransportException& e)
                        {
                            JDWP_TRACE_PROG("Run: Exception in reading command: " 
                                << e.what() << " [" << e.ErrCode() 
                                << "/" << e.TransportErrorCode() << "]");
                            if (m_isProcessed && !IsDead())
                            {
                                char* msg = GetTransportManager().GetLastTransportError();
                                AgentAutoFree af(msg JDWP_FILE_LINE);
        
                                if (e.TransportErrorCode() == JDWPTRANSPORT_ERROR_OUT_OF_MEMORY) {
                                    JDWP_DIE(e.what() << " [" << e.ErrCode() << "/"
                                                << e.TransportErrorCode() << "]: " << msg);
                                } else {
                                    JDWP_ERROR(e.what() << " [" << e.ErrCode() << "/"
                                                << e.TransportErrorCode() << "]: " << msg);
                                }
                            }
                            break;
                        }
        
                        // execute command and prevent from reset while execution
                        {
                            MonitorAutoLock lock(m_executionMonitor JDWP_FILE_LINE);
                            m_cmdDispatcher.ExecCommand(jni, &m_cmdParser);
                        }
                    }
                }
                catch (const AgentException& e)
                {
                    JDWP_TRACE_PROG("Run: Exception in executing command: "
                                    << e.what() << " [" << e.ErrCode() << "]");
                    if (!IsDead()) {
                        JDWP_ERROR(e.what() << " [" << e.ErrCode() << "]");
                    }
                }
        
                // reset all modules after session finished
                JDWP_TRACE_PROG("Run: reset session");
                ResetAll(jni);
        
                // no more sessions if VMDeath event occured
                if (IsDead()) {
                    JDWP_TRACE_PROG("Run: VM is dead -> shutdown");
                    break;
                }
        
                // no more sessions in attach mode
                if (!GetOptionParser().GetServer()) {
                    JDWP_TRACE_PROG("Run: attach mode -> shutdown");
                    break;
                }
            }
        }
        catch (const AgentException& e)
        {
            JDWP_TRACE_PROG("Run: Exception in PacketDispatcher: "
                            << e.what() << " [" << e.ErrCode() << "]");
            if (!IsDead()) {
                JDWP_DIE(e.what() << " [" << e.ErrCode() << "]"); 
            }
        }
        
        // stop also EventDispatcher thread
        try 
        {
            JDWP_TRACE_PROG("Run: stop EventDispatcher");
            GetEventDispatcher().Stop(jni);
        }
        catch (const AgentException& e)
        {
            // just report an error, cannot do anything else
            JDWP_ERROR("Exception in stopping EventDispatcher: "
                            << e.what() << " [" << e.ErrCode() << "]");
        }

        // release completion monitor and wait forever until VM kills this thread
        // TODO: remove this workaround to prevent from resource leak
	// This is the old completion mechanism fixed in HARMONY-5019
//        m_completionMonitor->Wait(0);
    }
    catch (const AgentException& e)
    {
        // just report an error, cannot do anything else
        JDWP_ERROR("Exception in PacketDispatcher synchronization: "
                        << e.what() << " [" << e.ErrCode() << "]");
    }
}

//-----------------------------------------------------------------------------

void 
PacketDispatcher::Stop(JNIEnv *jni) throw(AgentException)
{
    JDWP_TRACE_ENTRY("Stop()");

    // cause thread loop to break
    m_isProcessed = false;
    
    // close transport first, but not while executing current command
    JDWP_TRACE_PROG("Stop: close agent connection");
    if (m_executionMonitor != 0) {
        MonitorAutoLock lock(m_executionMonitor JDWP_FILE_LINE);
        GetTransportManager().Clean();
    }

    // wait for loop finished and EventDispatcher is also stopped
    {
        MonitorAutoLock lock(m_completionMonitor JDWP_FILE_LINE);
    }

    // wait for thread finished
    GetThreadManager().Join(jni, m_threadObject);
    jni->DeleteGlobalRef(m_threadObject);
    m_threadObject = NULL;
}

void 
PacketDispatcher::Clean(JNIEnv *jni) throw(AgentException)
{
    JDWP_TRACE_ENTRY("Clean(" << jni << ')');

    JDWP_TRACE_PROG("Clean: clean internal data");

    // do not delete m_completionMonitor because thread is waiting on it
    // TODO: remove this workaround to prevent from resource leak
    // This is the old completion mechanism fixed in HARMONY-5019
    if (m_completionMonitor != 0) {
        delete m_completionMonitor;
        m_completionMonitor = 0;
    }

    if (m_executionMonitor != 0) {
        delete m_executionMonitor;
        m_executionMonitor = 0;
    }
}

void 
PacketDispatcher::Reset(JNIEnv *jni) throw(AgentException)
{ 
    JDWP_TRACE_ENTRY("Reset(" << jni << ')');

    // cause thread loop to break
    JDWP_TRACE_PROG("Reset: reset session");
    m_isProcessed = false; 
}

void 
PacketDispatcher::ResetAll(JNIEnv *jni) throw(AgentException)
{
    JDWP_TRACE_ENTRY("ResetAll(" << jni << ")");

    // reset all modules, but not while executing current command 
    if (m_executionMonitor != 0) {
        MonitorAutoLock lock(m_executionMonitor JDWP_FILE_LINE);

        JDWP_TRACE_PROG("ResetAll: reset all modules");

        GetThreadManager().Reset(jni);
        GetRequestManager().Reset(jni);
        GetEventDispatcher().Reset(jni);
        GetTransportManager().Reset();
        GetPacketDispatcher().Reset(jni);
        GetClassManager().Reset(jni);
        GetObjectManager().Reset(jni);
    }
}

//-----------------------------------------------------------------------------

void JNICALL
PacketDispatcher::StartFunction(jvmtiEnv* jvmti_env, JNIEnv* jni, void* arg)
{
    JDWP_TRACE_ENTRY("StartFunction(" << jvmti_env << "," << jni << "," << arg << ")");

    (reinterpret_cast<PacketDispatcher *>(arg))->Run(jni);
}
