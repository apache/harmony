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
#include "ThreadGroupReference.h"
#include "VirtualMachine.h"

#include "PacketParser.h"
#include "ThreadManager.h"
#include "Log.h"
#include "ExceptionManager.h"

using namespace jdwp;
using namespace ThreadGroupReference;

//-----------------------------------------------------------------------------
//NameHandler------------------------------------------------------------------

int
ThreadGroupReference::NameHandler::Execute(JNIEnv *jni)
{
    jvmtiThreadGroupInfo info;
    info.name = 0;

    jthreadGroup threadGroupID = m_cmdParser->command.ReadThreadGroupID(jni);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Name: received: threadGroupID=%p", threadGroupID));

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadGroupInfo(threadGroupID, &info));

    JvmtiAutoFree dobj(info.name);

    JDWP_ASSERT(err != JVMTI_ERROR_NULL_POINTER);

    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
	    JDWP_SET_EXCEPTION(e);
        return err;
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Name: send: name=%s", info.name));
    m_cmdParser->reply.WriteString(info.name);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//ParentHandler----------------------------------------------------------------

int
ThreadGroupReference::ParentHandler::Execute(JNIEnv *jni)
{
    jvmtiThreadGroupInfo info;
    info.name = 0;

    jthreadGroup threadGroupID = m_cmdParser->command.ReadThreadGroupID(jni);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Parent: received: threadGroupID=%p", threadGroupID));

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadGroupInfo(threadGroupID, &info));

    JvmtiAutoFree dobj(info.name);

    JDWP_ASSERT(err != JVMTI_ERROR_NULL_POINTER);

    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
	    JDWP_SET_EXCEPTION(e);
        return err;
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Parent: send: name=%s, parent=%p", info.name, info.parent));
    m_cmdParser->reply.WriteThreadGroupID(jni, info.parent);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//ChildrenHandler--------------------------------------------------------------

int
ThreadGroupReference::ChildrenHandler::Execute(JNIEnv *jni)
{
    jint totalThreadCount, threadCount = 0;
    jint groupCount;
    jthread* threads = 0;
    jthreadGroup* groups = 0;

    jthreadGroup threadGroupID = m_cmdParser->command.ReadThreadGroupID(jni);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Children: received: threadGroupID=%p", threadGroupID));

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadGroupChildren(threadGroupID,
        &totalThreadCount, &threads, &groupCount, &groups));

    JvmtiAutoFree dobjt(threads);
    JvmtiAutoFree dobjg(groups);

    JDWP_ASSERT(err != JVMTI_ERROR_NULL_POINTER);

    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
	    JDWP_SET_EXCEPTION(e);
        return err;
    }

    ThreadManager& thrdMgr = GetThreadManager();

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Children: threadGroupID=%p, totalThreadCount=%d, thread=%p, groupCount=%d, groups=%p", 
                    threadGroupID, totalThreadCount, threads, groupCount, groups));

    int i;

    // filter out agent threads
    for (i = 0; i < totalThreadCount; i++) {
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
            jvmtiThreadInfo info;
            info.name = 0;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(threads[i], &info));
            JvmtiAutoFree jafInfoName(info.name);
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Children: thread#=%d, name=%s, isAgent=%s", 
                            i, JDWP_CHECK_NULL(info.name), ((thrdMgr.IsAgentThread(jni, threads[i]))?"TRUE":"FALSE")));
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
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Clildren: send: threadCount=%d, groupCount=%d", threadCount, groupCount));

        for (i = 0; i < threadCount; i++) {
            jvmtiThreadInfo info;
            info.name = 0;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(threads[i], &info));
            JvmtiAutoFree jafInfoName(info.name);

            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Clildren: send: thread#=%d, threadID=%p, name=%s", i, threads[i], JDWP_CHECK_NULL(info.name)));
        }

        for (i = 0; i < groupCount; i++) {
            jvmtiThreadGroupInfo info;
            info.name = 0;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadGroupInfo(groups[i], &info));
            JvmtiAutoFree jafInfoName(info.name);

            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Clildren: send: group#%d, groupID=%p, name=%s", i, groups[i], JDWP_CHECK_NULL(info.name)));
        }
    }
#endif

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
