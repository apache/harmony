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
#include "AgentManager.h"
#include "ClassManager.h"
#include "ObjectManager.h"
#include "OptionParser.h"
#include "ThreadManager.h"
#include "RequestManager.h"
#include "TransportManager.h"
#include "PacketDispatcher.h"
#include "EventDispatcher.h"
#include "ExceptionManager.h"

using namespace jdwp;

int AgentManager::Init(jvmtiEnv *jvmti, JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Init(%p,%p)", jvmti, jni));

    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Init: init agent modules and load transport"));

    int ret;
    AgentBase::SetIsDead(false);
    ret = AgentBase::GetClassManager().Init(jni);
    JDWP_CHECK_RETURN(ret);
    AgentBase::GetObjectManager().Init(jni);
    AgentBase::GetThreadManager().Init(jni);
    AgentBase::GetRequestManager().Init(jni);
    AgentBase::GetEventDispatcher().Init(jni);
    AgentBase::GetPacketDispatcher().Init(jni);

    char* javaLibraryPath = 0;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err,
        jvmti->GetSystemProperty("java.library.path", &javaLibraryPath));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Unable to get system property: java.library.path"));
    }

    JvmtiAutoFree afv(javaLibraryPath);
    ret = AgentBase::GetTransportManager().Init(
        AgentBase::GetOptionParser().GetTransport(),
        javaLibraryPath);
    JDWP_CHECK_RETURN(ret);

    return JDWP_ERROR_NONE;
} 

int AgentManager::Start(jvmtiEnv *jvmti, JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Start(%p,%p)", jvmti, jni));

    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Start: prepare connection and start all agent threads"));

    // prepare transport connection
    int ret = AgentBase::GetTransportManager().PrepareConnection(
        AgentBase::GetOptionParser().GetAddress(),
        AgentBase::GetOptionParser().GetServer(),
        AgentBase::GetOptionParser().GetTimeout(),
        AgentBase::GetOptionParser().GetTimeout()
    );
    JDWP_CHECK_RETURN(ret);

    // launch debugger if required and disable initial handling of EXCEPTION event
    const char* launch = AgentBase::GetOptionParser().GetLaunch();
    if (launch != 0) {
        ret = AgentBase::GetTransportManager().Launch(launch);
        JDWP_CHECK_RETURN(ret);
        ret = DisableInitialExceptionCatch(jvmti, jni);
        JDWP_CHECK_RETURN(ret);
    }

    // start agent threads
    ret = AgentBase::GetEventDispatcher().Start(jni);
    JDWP_CHECK_RETURN(ret);
    ret = AgentBase::GetPacketDispatcher().Start(jni);
    JDWP_CHECK_RETURN(ret);
    SetStarted(true);

    return JDWP_ERROR_NONE;
}

void 
AgentManager::Stop(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Stop(%p)", jni));

    // stop PacketDispatcher and EventDispatcher threads, and reset all modules
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Stop: stop all agent threads"));
    GetPacketDispatcher().Stop(jni);
}

void 
AgentManager::Clean(JNIEnv *jni)
{
    // trace entry before cleaning LogManager
    {
        JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Clean(%p)", jni));

        // clean all modules
        JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Clean: clean agent modules"));
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
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->Deallocate(
                reinterpret_cast<unsigned char*>(ext->id)));
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->Deallocate(
                reinterpret_cast<unsigned char*>(ext->short_description)));
            if (ext->params != 0) {
                for (int j = 0; j < ext->param_count; j++) {
                    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->Deallocate(
                        reinterpret_cast<unsigned char*>(ext->params[j].name)));
                }
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->Deallocate(
                    reinterpret_cast<unsigned char*>(ext->params)));
            }
            GetMemoryManager().Free(ext JDWP_FILE_LINE);
        }
    }

    // clean LogManager and close log
    GetLogManager().Clean();
    GetExceptionManager().Clean();
}

int AgentManager::EnableInitialExceptionCatch(jvmtiEnv *jvmti, JNIEnv *jni)
{
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "EnableInitialExceptionCatch"));

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->SetEventNotificationMode(
         JVMTI_ENABLE , JVMTI_EVENT_EXCEPTION, 0));
    if (err != JVMTI_ERROR_NONE) {
        AgentException ex(err);
        JDWP_SET_EXCEPTION(ex);
        return err;
    }

    return JDWP_ERROR_NONE;
}

int AgentManager::DisableInitialExceptionCatch(jvmtiEnv *jvmti, JNIEnv *jni)
{
    JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "DisableInitialExceptionCatch"));

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->SetEventNotificationMode(
         JVMTI_DISABLE, JVMTI_EVENT_EXCEPTION, 0));
    if (err != JVMTI_ERROR_NONE) {
        AgentException ex(err);
        JDWP_SET_EXCEPTION(ex);
        return err;
    }

    return JDWP_ERROR_NONE;
}
