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
#include "PacketDispatcher.h"
#include "TransportManager.h"
#include "AgentException.h"
#include "ThreadManager.h"
#include "EventDispatcher.h"
#include "ObjectManager.h"
#include "ClassManager.h"
#include "RequestManager.h"
#include "OptionParser.h"
#include "ExceptionManager.h"

using namespace jdwp;

//-----------------------------------------------------------------------------

PacketDispatcher::PacketDispatcher()
    :AgentBase()
{
    m_isProcessed = false;
    m_completionMonitor = 0;
    m_executionMonitor = 0;
    m_threadObject = 0;
}

//-----------------------------------------------------------------------------

void
PacketDispatcher::Init(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Init(%p)", jni));

    m_completionMonitor = new AgentMonitor("_agent_Packet_Dispatcher_completion");
    m_executionMonitor = new AgentMonitor("_agent_Packet_Dispatcher_execution");
}

int
PacketDispatcher::Start(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Start(%p)", jni));

    JDWP_ASSERT(!m_isProcessed);

    jthread thread = GetThreadManager().RunAgentThread(jni, StartFunction, this,
                                         JVMTI_THREAD_MAX_PRIORITY, "_jdwp_PacketDispatcher");
    if (thread == 0) {
        return JDWP_ERROR_INTERNAL;
    }
    m_threadObject = jni->NewGlobalRef(thread);
    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------

void
PacketDispatcher::Run(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Run(%p)", jni));

    int ret;
    MonitorAutoLock lock(m_completionMonitor JDWP_FILE_LINE);
    TransportManager &transport = GetTransportManager();
    
    // run multiplle sessions in a loop
    for (; ;)
    {
        // connect for new session
        JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Run: start new session"));
        ret = transport.Connect();
        if (ret != JDWP_ERROR_NONE) {
            AgentException aex = GetExceptionManager().GetLastException();
            JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Run: Exception in connection: %s", aex.GetExceptionMessage(jni)));
            if (!IsDead()) {
                JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Run: Exception in connection: %s", aex.GetExceptionMessage(jni)));
                /* In server case, attempt to reaccept while the VM is alive */
                if (AgentBase::GetOptionParser().GetServer()) {
                    ret = ResetAll(jni);
                    if (ret != JDWP_ERROR_NONE) {
                        AgentException aex = GetExceptionManager().GetLastException();
                        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error calling ResetAll(): %s", aex.GetExceptionMessage(jni)));
                        if (!IsDead()) {                                            
                            ::exit(1);
                        }
                        break;
                    }
                    continue;
                }
                ::exit(1);
            }
            break;
        }

        // inform that new session started
        GetEventDispatcher().NewSession();

        // add internal request for automatic VMDeath event with no modifiers
        ret = GetRequestManager().AddInternalRequest(jni,
            new AgentEventRequest(JDWP_EVENT_VM_DEATH, JDWP_SUSPEND_NONE));
        if (ret != JDWP_ERROR_NONE) {
            AgentException aex = GetExceptionManager().GetLastException();
             if (!IsDead()) {
                JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Run: Exception in executing command: %s", aex.GetExceptionMessage(jni)));
            }
            goto reset;
        }

        // start session and execute commands
        // release events
        GetEventDispatcher().ReleaseEvents();

        // read and execute commands
        m_isProcessed = true;
        while (m_isProcessed)
        {
            // read command
            JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Run: handle next command"));
            int ret = m_cmdParser.ReadCommand();
            if (ret != JDWP_ERROR_NONE) {
                if (m_isProcessed && !IsDead()) {
                    AgentException aex = GetExceptionManager().GetLastException();
                    JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Run: Exception in reading command: %s", aex.GetExceptionMessage(jni)));
                    if (aex.ErrCode() == JDWP_ERROR_OUT_OF_MEMORY
                         || aex.TransportErrCode() == JDWPTRANSPORT_ERROR_OUT_OF_MEMORY) {  
                        ::exit(1);
                    }
                }
                break;
            }

            if (m_cmdParser.command.GetLength() == 0)
                break;

            // execute command and prevent from reset while execution
            {
                MonitorAutoLock lock(m_executionMonitor JDWP_FILE_LINE);
                ret = m_cmdDispatcher.ExecCommand(jni, &m_cmdParser);
                if (ret != JDWP_ERROR_NONE) {
                    AgentException aex = GetExceptionManager().GetLastException();
                    JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Run: Exception in executing command: %s", aex.GetExceptionMessage(jni)));
                    break;
                }
            }
        }

reset:
        // reset all modules after session finished
        JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Run: reset session"));
        ResetAll(jni);
        if (ret != JDWP_ERROR_NONE) {
            AgentException aex = GetExceptionManager().GetLastException();
            JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error calling ResetAll(): %s", aex.GetExceptionMessage(jni)));
            if (!IsDead()) {                                            
                ::exit(1);
            }
            break;
        }

        // no more sessions if VMDeath event occured
        if (IsDead()) {
            JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Run: VM is dead -> shutdown"));
            break;
        }

        // no more sessions in attach mode
        if (!GetOptionParser().GetServer()) {
            JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Run: attach mode -> shutdown"));
            break;
        }
    }
    
    // stop also EventDispatcher thread
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Run: stop EventDispatcher"));
    GetEventDispatcher().Stop(jni);

        // release completion monitor and wait forever until VM kills this thread
        // TODO: remove this workaround to prevent from resource leak
	// This is the old completion mechanism fixed in HARMONY-5019
//        m_completionMonitor->Wait(0);

}

//-----------------------------------------------------------------------------

void 
PacketDispatcher::Stop(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Stop()"));

    // cause thread loop to break
    m_isProcessed = false;
    
    // close transport first, but not while executing current command
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Stop: close agent connection"));
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
PacketDispatcher::Clean(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Clean(%p)", jni));

    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Clean: clean internal data"));

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
PacketDispatcher::Reset(JNIEnv *jni)
{ 
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Reset(%p)", jni));

    // cause thread loop to break
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Reset: reset session"));
    m_isProcessed = false; 
}

int 
PacketDispatcher::ResetAll(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "ResetAll(%p)", jni));

    // reset all modules, but not while executing current command 
    if (m_executionMonitor != 0) {
        MonitorAutoLock lock(m_executionMonitor JDWP_FILE_LINE);

        JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "ResetAll: reset all modules"));

        int ret;
        m_cmdParser.Reset(jni);
        ret = GetThreadManager().Reset(jni);
        JDWP_CHECK_RETURN(ret);

        GetRequestManager().Reset(jni);
        GetEventDispatcher().Reset(jni);

        ret = GetTransportManager().Reset();
        JDWP_CHECK_RETURN(ret);

        GetPacketDispatcher().Reset(jni);
        GetClassManager().Reset(jni);
        GetObjectManager().Reset(jni);
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------

void JNICALL
PacketDispatcher::StartFunction(jvmtiEnv* jvmti_env, JNIEnv* jni, void* arg)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "StartFunction(%p,%p,%p)", jvmti_env, jni, arg));

    (reinterpret_cast<PacketDispatcher *>(arg))->Run(jni);
}
