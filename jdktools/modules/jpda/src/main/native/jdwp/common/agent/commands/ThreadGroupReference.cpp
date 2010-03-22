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
#include "ThreadGroupReference.h"
#include "VirtualMachine.h"

#include "PacketParser.h"
#include "ThreadManager.h"
#include "Log.h"

using namespace jdwp;
using namespace ThreadGroupReference;

//-----------------------------------------------------------------------------
//NameHandler------------------------------------------------------------------

void
ThreadGroupReference::NameHandler::Execute(JNIEnv *jni) throw (AgentException)
{
    jvmtiThreadGroupInfo info;
    info.name = 0;

    jthreadGroup threadGroupID = m_cmdParser->command.ReadThreadGroupID(jni);
    JDWP_TRACE_DATA("Name: received: threadGroupID=" << threadGroupID);

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadGroupInfo(threadGroupID, &info));

    JvmtiAutoFree dobj(info.name);

    JDWP_ASSERT(err != JVMTI_ERROR_NULL_POINTER);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    JDWP_TRACE_DATA("Name: send: name=" << info.name);
    m_cmdParser->reply.WriteString(info.name);
}

//-----------------------------------------------------------------------------
//ParentHandler----------------------------------------------------------------

void
ThreadGroupReference::ParentHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jvmtiThreadGroupInfo info;
    info.name = 0;

    jthreadGroup threadGroupID = m_cmdParser->command.ReadThreadGroupID(jni);
    JDWP_TRACE_DATA("Parent: received: threadGroupID=" << threadGroupID);

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadGroupInfo(threadGroupID, &info));

    JvmtiAutoFree dobj(info.name);

    JDWP_ASSERT(err != JVMTI_ERROR_NULL_POINTER);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    JDWP_TRACE_DATA("Parent: send: name=" << info.name << ", parent=" << info.parent);
    m_cmdParser->reply.WriteThreadGroupID(jni, info.parent);
}

//-----------------------------------------------------------------------------
//ChildrenHandler--------------------------------------------------------------

void
ThreadGroupReference::ChildrenHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jint totalThreadCount, threadCount = 0;
    jint groupCount;
    jthread* threads = 0;
    jthreadGroup* groups = 0;

    jthreadGroup threadGroupID = m_cmdParser->command.ReadThreadGroupID(jni);
    JDWP_TRACE_DATA("Children: received: threadGroupID=" << threadGroupID); 

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadGroupChildren(threadGroupID,
        &totalThreadCount, &threads, &groupCount, &groups));

    JvmtiAutoFree dobjt(threads);
    JvmtiAutoFree dobjg(groups);

    JDWP_ASSERT(err != JVMTI_ERROR_NULL_POINTER);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    ThreadManager& thrdMgr = GetThreadManager();

    JDWP_TRACE_DATA("Children: threadGroupID=" << threadGroupID 
        << ", totalThreadCount=" << totalThreadCount 
        << ", threads=" << threads 
        << ", groupCount=" << groupCount 
        << ", groups=" << groups);

    int i;

    // filter out agent threads
    for (i = 0; i < totalThreadCount; i++) {
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
            jvmtiThreadInfo info;
            info.name = 0;
            JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(threads[i], &info));
            JvmtiAutoFree jafInfoName(info.name);
            JDWP_TRACE_DATA("Children: thread#=" << i
                << ", name= " << JDWP_CHECK_NULL(info.name)
                << ", isAgent=" << (thrdMgr.IsAgentThread(jni, threads[i])));
        }
#endif
        if (!thrdMgr.IsAgentThread(jni, threads[i])) {
            threads[threadCount] = threads[i];
            threadCount++;
        }
    }

    m_cmdParser->reply.WriteInt(threadCount);
    for (i = 0; i < threadCount; i++) {
        m_cmdParser->reply.WriteThreadID(jni, threads[i]);
    }

    m_cmdParser->reply.WriteInt(groupCount);
    for (i = 0; i < groupCount; i++) {
        m_cmdParser->reply.WriteThreadGroupID(jni, groups[i]);
    }

#ifndef NDEBUG    
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        int i;
        JDWP_TRACE_DATA("Clildren: send: threadCount=" << threadCount 
            << ", groupCount=" << groupCount);

        for (i = 0; i < threadCount; i++) {
            jvmtiThreadInfo info;
            info.name = 0;
            JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(threads[i], &info));
            JvmtiAutoFree jafInfoName(info.name);

            JDWP_TRACE_DATA("Clildren: send: thread#=" << i
            << ", threadID=" << threads[i]
            << ", name=" << JDWP_CHECK_NULL(info.name));
        }

        for (i = 0; i < groupCount; i++) {
            jvmtiThreadGroupInfo info;
            info.name = 0;
            JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadGroupInfo(groups[i], &info));
            JvmtiAutoFree jafInfoName(info.name);

            JDWP_TRACE_DATA("Clildren: send: group#" << i 
            << ", groupID=" << groups[i]
            << ", name=" << JDWP_CHECK_NULL(info.name));
        }
    }
#endif
}

//-----------------------------------------------------------------------------
