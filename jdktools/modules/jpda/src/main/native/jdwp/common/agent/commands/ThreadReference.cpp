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
#include "ThreadReference.h"
#include "PacketParser.h"
#include "ThreadManager.h"
#include "VirtualMachine.h"
#include "MemoryManager.h"
#include "ClassManager.h"

using namespace jdwp;
using namespace ThreadReference;

//-----------------------------------------------------------------------------
//NameHandler------------------------------------------------------------------

void
ThreadReference::NameHandler::Execute(JNIEnv *jni) throw (AgentException)
{
    jvmtiThreadInfo info;
    info.name = 0;
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    JDWP_TRACE_DATA("Name: received: threadID=" << thrd);

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(thrd, &info));
    JvmtiAutoFree destroyer(info.name);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    JDWP_TRACE_DATA("Name: send: name=" << JDWP_CHECK_NULL(info.name));
    m_cmdParser->reply.WriteString(info.name);
}

//-----------------------------------------------------------------------------
//SuspendHandler----------------------------------------------------------------

void
ThreadReference::SuspendHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);

    JDWP_TRACE_DATA("Suspend: suspend: threadID=" << thrd);
    GetThreadManager().Suspend(jni, thrd);
}

//-----------------------------------------------------------------------------
//ResumeHandler----------------------------------------------------------------

void
ThreadReference::ResumeHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);

    JDWP_TRACE_DATA("Resume: resume: threadID=" << thrd);
    GetThreadManager().Resume(jni, thrd);
}

//-----------------------------------------------------------------------------
//StatusHandler----------------------------------------------------------------

void
ThreadReference::StatusHandler::Execute(JNIEnv *jni) throw (AgentException)
{
    jint thread_state;

    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    JDWP_TRACE_DATA("Status: received: threadID=" << thrd);

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadState(thrd, &thread_state));
    JDWP_TRACE_DATA("Status: threadState=" << hex << thread_state);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    jint ret_value;
    jint const THREAD_STATE_SLEEPING =
        JVMTI_THREAD_STATE_SLEEPING | JVMTI_THREAD_STATE_ALIVE;

    if ( (thread_state & THREAD_STATE_SLEEPING) == THREAD_STATE_SLEEPING ) {
        ret_value = JDWP_THREAD_STATUS_SLEEPING;
    } else {
        switch (thread_state & JVMTI_JAVA_LANG_THREAD_STATE_MASK)
        {
        case JVMTI_JAVA_LANG_THREAD_STATE_TERMINATED:
            ret_value = JDWP_THREAD_STATUS_ZOMBIE;
            break;
        case JVMTI_JAVA_LANG_THREAD_STATE_RUNNABLE:
            ret_value = JDWP_THREAD_STATUS_RUNNING;
            break;
        case JVMTI_JAVA_LANG_THREAD_STATE_BLOCKED:
            ret_value = JDWP_THREAD_STATUS_MONITOR;
            break;
        case JVMTI_JAVA_LANG_THREAD_STATE_WAITING:
        case JVMTI_JAVA_LANG_THREAD_STATE_TIMED_WAITING:
            ret_value = JDWP_THREAD_STATUS_WAIT;
            break;
        default:
            JDWP_TRACE_DATA("Status: bad Java thread state: " 
                << hex << thread_state);
            throw InternalErrorException();
        }
    }
    m_cmdParser->reply.WriteInt(ret_value);
    if (thread_state & JVMTI_THREAD_STATE_SUSPENDED)
        m_cmdParser->reply.WriteInt(JDWP_SUSPEND_STATUS_SUSPENDED);
    else
        m_cmdParser->reply.WriteInt(0);
    JDWP_TRACE_DATA("Status: send: status=" << ret_value);
}

//-----------------------------------------------------------------------------
//ThreadGroupHandler-----------------------------------------------------------

void 
ThreadReference::ThreadGroupHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jvmtiThreadInfo info;
    info.name = 0;

    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    JDWP_TRACE_DATA("ThreadGroup: received: threadID=" << thrd);

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(thrd, &info));
    JvmtiAutoFree destroyer(info.name);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    JDWP_TRACE_DATA("ThreadGroup: send: threadGroupID=" << info.thread_group);
    m_cmdParser->reply.WriteThreadGroupID(jni, info.thread_group);
}

//-----------------------------------------------------------------------------
//FramesHandler----------------------------------------------------------------

void
ThreadReference::FramesHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jvmtiEnv *jvmti = GetJvmtiEnv();

    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    
    if (!GetThreadManager().IsSuspended(thrd))
        throw AgentException(JVMTI_ERROR_THREAD_NOT_SUSPENDED);
    
    jint startFrame = m_cmdParser->command.ReadInt();
    jint length = m_cmdParser->command.ReadInt();
    JDWP_TRACE_DATA("Frames: received: threadID=" << thrd 
        << ", startFrame=" << startFrame 
        << ", length=" << length);

    jint frameCount;
    jvmtiError err;
    JVMTI_TRACE(err, jvmti->GetFrameCount(thrd, &frameCount));


    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    if (length == -1) {
        length = frameCount - startFrame;
    }

    if (length == 0) {
        JDWP_TRACE_DATA("Frames: frameCount=" << frameCount 
            << ", startFrame=" << startFrame 
            << ", length=" << length);
        m_cmdParser->reply.WriteInt(0);
        return;
    }

    if (startFrame >= frameCount || startFrame < 0)
        throw AgentException(JDWP_ERROR_INVALID_INDEX);

    jint maxFrame = startFrame + length;
    if ( (length < 0) || (maxFrame > frameCount) ) {
        throw AgentException(JDWP_ERROR_INVALID_LENGTH);
    }

    jvmtiFrameInfo *frame_buffer = 
        reinterpret_cast<jvmtiFrameInfo*>(GetMemoryManager().Allocate(
                        sizeof(jvmtiFrameInfo)*frameCount JDWP_FILE_LINE));
    AgentAutoFree destroyer(frame_buffer JDWP_FILE_LINE);

    jint count;
    JVMTI_TRACE(err, jvmti->GetStackTrace(thrd, 0, frameCount, 
                                                 frame_buffer, &count));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
    JDWP_ASSERT(count == frameCount);

    m_cmdParser->reply.WriteInt(length);

    JDWP_TRACE_DATA("Frames: frameCount=" << frameCount 
        << ", startFrame=" << startFrame 
        << ", length=" << length 
        << ", maxFrame=" << maxFrame);

    jclass declaring_class;
    jdwpTypeTag typeTag;

    for (jint j = startFrame; j < maxFrame; j++) {
        m_cmdParser->reply.WriteFrameID(jni, thrd, j, frameCount);

        JVMTI_TRACE(err, jvmti->GetMethodDeclaringClass(frame_buffer[j].method,
            &declaring_class));
        if (err != JVMTI_ERROR_NONE) {
            throw AgentException(err);
        }

        typeTag = GetClassManager().GetJdwpTypeTag(declaring_class);

#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
            jvmtiThreadInfo info;
            JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(thrd, &info));
            JDWP_TRACE_DATA("Frames: send: frame#=" << j 
                << ", threadName=" << info.name 
                << ", loc=" << frame_buffer[j].location 
                << ", methodID=" << frame_buffer[j].method 
                << ", classID=" << declaring_class
                << ", typeTag=" << typeTag);
        }
#endif

        m_cmdParser->reply.WriteLocation(jni, typeTag,
                        declaring_class, frame_buffer[j].method,
                        frame_buffer[j].location);
    }
}

//-----------------------------------------------------------------------------
//FrameCountHandler------------------------------------------------------------

void
ThreadReference::FrameCountHandler::Execute(JNIEnv *jni) throw (AgentException)
{
    jint count;

    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    JDWP_TRACE_DATA("FrameCount: received: threadID=" << thrd);

    if (!GetThreadManager().IsSuspended(thrd))
        throw AgentException(JVMTI_ERROR_THREAD_NOT_SUSPENDED);

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetFrameCount(thrd, &count));

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    m_cmdParser->reply.WriteInt(count);
    JDWP_TRACE_DATA("FrameCount: send: count=" << count);
}

//-----------------------------------------------------------------------------
//OwnedMonitorsHandler---------------------------------------------------------

void
ThreadReference::OwnedMonitorsHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jint count;
    jobject* owned_monitors = 0;
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    JDWP_TRACE_DATA("OwnedMonitors: received: threadID=" << thrd);

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetOwnedMonitorInfo(thrd, &count,
        &owned_monitors));
    JvmtiAutoFree destroyer(owned_monitors);

    JDWP_ASSERT(err != JVMTI_ERROR_NULL_POINTER);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    JDWP_TRACE_DATA("OwnedMonitors: send: monitors=" << count);
    m_cmdParser->reply.WriteInt(count);
    for (int i = 0; i < count; i++)
    {
        JDWP_TRACE_DATA("OwnedMonitors: send: monitor#=" << i
            << ", objectID=" << owned_monitors[i]);
        m_cmdParser->reply.WriteTaggedObjectID(jni, owned_monitors[i]);
    }
}

//-----------------------------------------------------------------------------
//OwnedMonitorsHandler---------------------------------------------------------

void
ThreadReference::CurrentContendedMonitorHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jobject monitor;
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);\
    JDWP_TRACE_DATA("CurrentContendedMonitor: received: threadID=" << thrd);
                                                                           
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetCurrentContendedMonitor(thrd, &monitor));

    JDWP_ASSERT(err != JVMTI_ERROR_NULL_POINTER);

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    JDWP_TRACE_DATA("CurrentContendedMonitor: send: monitor=" << monitor);
    m_cmdParser->reply.WriteTaggedObjectID(jni, monitor);
}

//-----------------------------------------------------------------------------
//StopHandler------------------------------------------------------------------

void
ThreadReference::StopHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    jobject excp = m_cmdParser->command.ReadObjectID(jni);

    JDWP_TRACE_DATA("Stop: stop: threadID=" << thrd 
        << ", throwableID=" << excp);
    GetThreadManager().Stop(jni, thrd, excp);
}

//-----------------------------------------------------------------------------
//InterruptHandler-------------------------------------------------------------

void
ThreadReference::InterruptHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);

    JDWP_TRACE_DATA("Interrupt: interrupt: threadID=" << thrd);
    GetThreadManager().Interrupt(jni, thrd);
}

//-----------------------------------------------------------------------------
//SuspendCountHandler----------------------------------------------------------

void
ThreadReference::SuspendCountHandler::Execute(JNIEnv *jni)
    throw (AgentException)
{
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    JDWP_TRACE_DATA("SuspendCount: received: threadID=" << thrd);
    jint count = GetThreadManager().GetSuspendCount(jni, thrd);

    JDWP_TRACE_DATA("SuspendCount: send: count=" << count);
    m_cmdParser->reply.WriteInt(count);
}

//-----------------------------------------------------------------------------
