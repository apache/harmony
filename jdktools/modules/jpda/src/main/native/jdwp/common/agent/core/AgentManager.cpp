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
 * @author Aleksander V. Budniy
 */

#include "AgentManager.h"
#include "ClassManager.h"
#include "ObjectManager.h"
#include "OptionParser.h"
#include "ThreadManager.h"
#include "RequestManager.h"
#include "TransportManager.h"
#include "PacketDispatcher.h"
#include "EventDispatcher.h"

using namespace jdwp;

void AgentManager::Init(jvmtiEnv *jvmti, JNIEnv *jni) throw (AgentException)
{
    JDWP_TRACE_ENTRY("Init(" << jvmti << "," << jni << ")");

    JDWP_TRACE_PROG("Init: init agent modules and load transport");

    AgentBase::SetIsDead(false);
    AgentBase::GetClassManager().Init(jni);
    AgentBase::GetObjectManager().Init(jni);
    AgentBase::GetThreadManager().Init(jni);
    AgentBase::GetRequestManager().Init(jni);
    AgentBase::GetEventDispatcher().Init(jni);
    AgentBase::GetPacketDispatcher().Init(jni);

    char* javaLibraryPath = 0;
    jvmtiError err;
    JVMTI_TRACE(err,
        jvmti->GetSystemProperty("java.library.path", &javaLibraryPath));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_INFO("Unable to get system property: java.library.path")
    }

    JvmtiAutoFree afv(javaLibraryPath);
    AgentBase::GetTransportManager().Init(
        AgentBase::GetOptionParser().GetTransport(),
        javaLibraryPath);

} 

void AgentManager::Start(jvmtiEnv *jvmti, JNIEnv *jni) throw (AgentException)
{
    JDWP_TRACE_ENTRY("Start(" << jvmti << "," << jni << ")");

    JDWP_TRACE_PROG("Start: prepare connection and start all agent threads");

    // prepare transport connection
    AgentBase::GetTransportManager().PrepareConnection(
        AgentBase::GetOptionParser().GetAddress(),
        AgentBase::GetOptionParser().GetServer(),
        AgentBase::GetOptionParser().GetTimeout(),
        AgentBase::GetOptionParser().GetTimeout()
    );

    // launch debugger if required and disable initial handling of EXCEPTION event
    const char* launch = AgentBase::GetOptionParser().GetLaunch();
    if (launch != 0) {
        AgentBase::GetTransportManager().Launch(launch);
        DisableInitialExceptionCatch(jvmti, jni);
    }

    // start agent threads
    AgentBase::GetEventDispatcher().Start(jni);
    AgentBase::GetPacketDispatcher().Start(jni);
    SetStarted(true);
}

void 
AgentManager::Stop(JNIEnv *jni) throw(AgentException)
{
    JDWP_TRACE_ENTRY("Stop(" << jni << ")");

    // stop PacketDispatcher and EventDispatcher threads, and reset all modules
    JDWP_TRACE_PROG("Stop: stop all agent threads");
    GetPacketDispatcher().Stop(jni);
}

void 
AgentManager::Clean(JNIEnv *jni) throw(AgentException)
{
    // trace entry before cleaning LogManager
    {
        JDWP_TRACE_ENTRY("Clean(" << jni << ")");

        // clean all modules
        JDWP_TRACE_PROG("Clean: clean agent modules");
        GetPacketDispatcher().Clean(jni);
        GetThreadManager().Clean(jni);
        GetRequestManager().Clean(jni);
        GetEventDispatcher().Clean(jni);
        GetObjectManager().Clean(jni);
        GetClassManager().Clean(jni);

        // delete extensionEventClassUnload if any
        jvmtiExtensionEventInfo* ext = GetAgentEnv()->extensionEventClassUnload;
        if (ext != 0) {
            jvmtiError err;
            JVMTI_TRACE(err, GetJvmtiEnv()->Deallocate(
                reinterpret_cast<unsigned char*>(ext->id)));
            JVMTI_TRACE(err, GetJvmtiEnv()->Deallocate(
                reinterpret_cast<unsigned char*>(ext->short_description)));
            if (ext->params != 0) {
                for (int j = 0; j < ext->param_count; j++) {
                    JVMTI_TRACE(err, GetJvmtiEnv()->Deallocate(
                        reinterpret_cast<unsigned char*>(ext->params[j].name)));
                }
                JVMTI_TRACE(err, GetJvmtiEnv()->Deallocate(
                    reinterpret_cast<unsigned char*>(ext->params)));
            }
            GetMemoryManager().Free(ext JDWP_FILE_LINE);
        }
    }

    // clean LogManager and close log
    GetLogManager().Clean();
}

void AgentManager::EnableInitialExceptionCatch(jvmtiEnv *jvmti, JNIEnv *jni) throw (AgentException)
{
    JDWP_TRACE_PROG("EnableInitialExceptionCatch");

    jvmtiError err;
    JVMTI_TRACE(err, jvmti->SetEventNotificationMode(
         JVMTI_ENABLE , JVMTI_EVENT_EXCEPTION, 0));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
}

void AgentManager::DisableInitialExceptionCatch(jvmtiEnv *jvmti, JNIEnv *jni) throw (AgentException)
{
    JDWP_TRACE_PROG("DisableInitialExceptionCatch");

    jvmtiError err;
    JVMTI_TRACE(err, jvmti->SetEventNotificationMode(
         JVMTI_DISABLE, JVMTI_EVENT_EXCEPTION, 0));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
}
