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
#include "ThreadReference.h"
#include "PacketParser.h"
#include "ThreadManager.h"
#include "VirtualMachine.h"
#include "MemoryManager.h"
#include "ClassManager.h"
#include "ExceptionManager.h"


using namespace jdwp;
using namespace ThreadReference;

//-----------------------------------------------------------------------------
//NameHandler------------------------------------------------------------------

int
ThreadReference::NameHandler::Execute(JNIEnv *jni) 
{
    jvmtiThreadInfo info;
    info.name = 0;
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Name: received: threadID=%p", thrd));

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thrd, &info));
    JvmtiAutoFree destroyer(info.name);

    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Name: send: name=%s", JDWP_CHECK_NULL(info.name)));
    m_cmdParser->reply.WriteString(info.name);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//SuspendHandler----------------------------------------------------------------

int
ThreadReference::SuspendHandler::Execute(JNIEnv *jni) 
{
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Suspend: suspend: threadID=%p", thrd));
    int ret = GetThreadManager().Suspend(jni, thrd);

    return ret;
}

//-----------------------------------------------------------------------------
//ResumeHandler----------------------------------------------------------------

int
ThreadReference::ResumeHandler::Execute(JNIEnv *jni) 
{
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Resume: resume: threadID=%p", thrd));
    int ret = GetThreadManager().Resume(jni, thrd);

    return ret;
}

//-----------------------------------------------------------------------------
//StatusHandler----------------------------------------------------------------

int
ThreadReference::StatusHandler::Execute(JNIEnv *jni) 
{
    jint thread_state;

    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Status: received: threadID=%p", thrd));

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadState(thrd, &thread_state));
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Status: threadState=%x", thread_state));

    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    jint ret_value;
    jint const THREAD_STATE_SLEEPING =
        JVMTI_THREAD_STATE_SLEEPING | JVMTI_THREAD_STATE_ALIVE;

    if ( (thread_state & THREAD_STATE_SLEEPING) == THREAD_STATE_SLEEPING ) {
        ret_value = JDWP_THREAD_STATUS_SLEEPING;
    } else {
        switch (thread_state & (JVMTI_JAVA_LANG_THREAD_STATE_MASK))
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
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Status: bad Java thread state: %x", thread_state));
            AgentException e(JDWP_ERROR_INTERNAL);
			JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_INTERNAL;
        }
    }
    m_cmdParser->reply.WriteInt(ret_value);
    if (thread_state & JVMTI_THREAD_STATE_SUSPENDED)
        m_cmdParser->reply.WriteInt(JDWP_SUSPEND_STATUS_SUSPENDED);
    else
        m_cmdParser->reply.WriteInt(0);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Status: send: status=%d", ret_value));

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//ThreadGroupHandler-----------------------------------------------------------

int 
ThreadReference::ThreadGroupHandler::Execute(JNIEnv *jni) 
{
    jvmtiThreadInfo info;
    info.name = 0;

    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ThreadGroup: received: threadID=%p", thrd));

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thrd, &info));
    JvmtiAutoFree destroyer(info.name);

    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ThreadGroup: send: threadGroupID=%p", info.thread_group));
    m_cmdParser->reply.WriteThreadGroupID(jni, info.thread_group);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//FramesHandler----------------------------------------------------------------

int
ThreadReference::FramesHandler::Execute(JNIEnv *jni)
{
    jvmtiEnv *jvmti = GetJvmtiEnv();

    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    
    if (!GetThreadManager().IsSuspended(thrd)){
        AgentException e(JVMTI_ERROR_THREAD_NOT_SUSPENDED);
		JDWP_SET_EXCEPTION(e);
        return JVMTI_ERROR_THREAD_NOT_SUSPENDED;
    }
    
    jint startFrame = m_cmdParser->command.ReadInt();
    jint length = m_cmdParser->command.ReadInt();
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Frames: received: threadID=%p, startFrame=%d, length=%d", thrd, startFrame, length));

    jint frameCount;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetFrameCount(thrd, &frameCount));


    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    if (length == -1) {
        length = frameCount - startFrame;
    }

    if (length == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Frames: frameCount=%d, startFrame=%d, length=%d", frameCount, startFrame, length));
        m_cmdParser->reply.WriteInt(0);
        return JDWP_ERROR_NONE;
    }

    if (startFrame >= frameCount || startFrame < 0){
        AgentException e(JDWP_ERROR_INVALID_INDEX);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_INDEX;
    }

    jint maxFrame = startFrame + length;
    if ( (length < 0) || (maxFrame > frameCount) ) {
        AgentException e(JDWP_ERROR_INVALID_LENGTH);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_LENGTH;
    }

    jvmtiFrameInfo *frame_buffer = 
        reinterpret_cast<jvmtiFrameInfo*>(GetMemoryManager().Allocate(
                        sizeof(jvmtiFrameInfo)*frameCount JDWP_FILE_LINE));
    AgentAutoFree destroyer(frame_buffer JDWP_FILE_LINE);

    jint count;
    JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetStackTrace(thrd, 0, frameCount, 
                                                 frame_buffer, &count));
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    JDWP_ASSERT(count == frameCount);

    m_cmdParser->reply.WriteInt(length);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Frames: frameCount=%d, startFrame=%d, length=%d, maxFrame=%d", frameCount, startFrame, length, maxFrame));

    jclass declaring_class;
    jdwpTypeTag typeTag;

    for (jint j = startFrame; j < maxFrame; j++) {
        m_cmdParser->reply.WriteFrameID(jni, thrd, j, frameCount);

        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetMethodDeclaringClass(frame_buffer[j].method,
            &declaring_class));
        if (err != JVMTI_ERROR_NONE) {
            AgentException e(err);
		    JDWP_SET_EXCEPTION(e);
            return err;
        }

        typeTag = GetClassManager().GetJdwpTypeTag(declaring_class);

#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
            jvmtiThreadInfo info;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thrd, &info));
            JvmtiAutoFree jafInfoName(info.name);
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Frames: send: frame#=%d, threadName=%s, loc=%lld, methodID=%p, classID=%p, typeTag=%d", 
                            j, info.name, frame_buffer[j].location, frame_buffer[j].method, declaring_class, typeTag));
        }
#endif
        
        // we should return 0 as the methodID for obsolete method.

        jvmtiCapabilities caps;
        memset(&caps, 0, sizeof(caps));

        JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetCapabilities(&caps));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Unable to get capabilities: %d", err));
            return err;
        }
       // When selective debug is enabled, HCR is not allowed. So we need to check if HCR is disabled first before invoking IsMethodObsolete.
       // If so, just return the methodID directly.
        if(caps.can_redefine_classes != 1) {
            m_cmdParser->reply.WriteLocation(jni, typeTag,
                        declaring_class, frame_buffer[j].method,
                        frame_buffer[j].location);
        }else {
            jboolean isObsolete;
            JVMTI_TRACE(LOG_DEBUG, err, jvmti->IsMethodObsolete(frame_buffer[j].method, &isObsolete));
            if (err != JVMTI_ERROR_NONE) {
                JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Frames: IsMethodObsolete return error code: %d", err));
                return err;
            }
            if(isObsolete) {
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Frames: the method is obsolete: frame#=%d, loc=%lld, methodID=%p, classID=%p, typeTag=%d",
                            j, frame_buffer[j].location, frame_buffer[j].method, declaring_class, typeTag));
                m_cmdParser->reply.WriteLocation(jni, typeTag,
                            declaring_class, 0,
                            frame_buffer[j].location);
            }else {
                m_cmdParser->reply.WriteLocation(jni, typeTag,
                        declaring_class, frame_buffer[j].method,
                        frame_buffer[j].location);
            }
        }
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//FrameCountHandler------------------------------------------------------------

int
ThreadReference::FrameCountHandler::Execute(JNIEnv *jni) 
{
    jint count;

    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "FrameCount: received: threadID=%p", thrd));

    if (!GetThreadManager().IsSuspended(thrd)){
        AgentException e(JVMTI_ERROR_THREAD_NOT_SUSPENDED);
		JDWP_SET_EXCEPTION(e);
        return JVMTI_ERROR_THREAD_NOT_SUSPENDED;
    }

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFrameCount(thrd, &count));

    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    m_cmdParser->reply.WriteInt(count);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "FrameCount: send: count=%d", count));

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//OwnedMonitorsHandler---------------------------------------------------------

int
ThreadReference::OwnedMonitorsHandler::Execute(JNIEnv *jni) 
{
    jint count;
    jobject* owned_monitors = 0;
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "OwnedMonitors: received: threadID=%p", thrd));

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetOwnedMonitorInfo(thrd, &count,
        &owned_monitors));
    JvmtiAutoFree destroyer(owned_monitors);

    JDWP_ASSERT(err != JVMTI_ERROR_NULL_POINTER);

    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "OwnedMonitors: send: monitors=%d", count));
    m_cmdParser->reply.WriteInt(count);
    for (int i = 0; i < count; i++)
    {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "OwnedMonitors: send: monitor#=%d, objectID=%p", i, owned_monitors[i]));
        m_cmdParser->reply.WriteTaggedObjectID(jni, owned_monitors[i]);
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//OwnedMonitorsHandler---------------------------------------------------------

int
ThreadReference::CurrentContendedMonitorHandler::Execute(JNIEnv *jni) 
{
    jobject monitor;
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);\
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "CurrentContendedMonitor: received: threadID=%p", thrd));
                                                                           
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetCurrentContendedMonitor(thrd, &monitor));

    JDWP_ASSERT(err != JVMTI_ERROR_NULL_POINTER);

    if (err != JVMTI_ERROR_NONE){
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "CurrentContendedMonitor: send: monitor=%p", monitor));
    m_cmdParser->reply.WriteTaggedObjectID(jni, monitor);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//StopHandler------------------------------------------------------------------

int
ThreadReference::StopHandler::Execute(JNIEnv *jni) 
{
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    jobject excp = m_cmdParser->command.ReadObjectID(jni);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Stop: stop: threadID=%p throwableID=%p", thrd, excp));
    int ret = GetThreadManager().Stop(jni, thrd, excp);

    return ret;
}

//-----------------------------------------------------------------------------
//InterruptHandler-------------------------------------------------------------

int
ThreadReference::InterruptHandler::Execute(JNIEnv *jni) 
{
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Interrupt: interrupt: threadID=%p", thrd));
    int ret = GetThreadManager().Interrupt(jni, thrd);

    return ret;
}

//-----------------------------------------------------------------------------
//SuspendCountHandler----------------------------------------------------------

int
ThreadReference::SuspendCountHandler::Execute(JNIEnv *jni)
{
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SuspendCount: received: threadID=%p", thrd));
    jint count = GetThreadManager().GetSuspendCount(jni, thrd);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SuspendCount: send: count=%d", count));
    m_cmdParser->reply.WriteInt(count);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//OwnedMonitorsStackDepthInfoHandler-------------------------------------------

int
ThreadReference::OwnedMonitorsStackDepthInfoHandler::Execute(JNIEnv *jni)
{
    // Read thread id from OwnedMonitorsStackDepthInfoHandler command
    // Bug fix, if thread id is invalid, error code reply should be returned
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    if(thrd == 0){
        AgentException aex = GetExceptionManager().GetLastException();
        jdwpError err = aex.ErrCode();
        AgentException ex(err);
        JDWP_SET_EXCEPTION(ex);
        return err;
	}
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "OwnedMonitorsStackDepthInfo: received: threadID=%p", thrd));
    
    // If the thread is not in the suspended list or is terminated,
    // throw INVALID_THREAD exception or THREAD_NOT_SUSPENDED
    int ret = GetThreadManager().CheckThreadStatus(jni, thrd);
    JDWP_CHECK_RETURN(ret);

    // Invoke jvmti function to attain the expected monitor data
    jvmtiError err;
    jint count;
    jvmtiMonitorStackDepthInfo* pMonitorInfos;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetOwnedMonitorStackDepthInfo(thrd, &count, &pMonitorInfos));
    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY, JVMTI_ERROR_INVALID_THREAD
        // JVMTI_ERROR_THREAD_NOT_ALIVE, JVMTI_ERROR_NULL_POINTER 
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    
    // Must release memeory manually
    JvmtiAutoFree af(pMonitorInfos);

    // Write monitor count to reply package
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "OwnedMonitorsStackDepthInfo: received: monitor count=%d", count));
    m_cmdParser->reply.WriteInt(count);
    
    // Write each monitor and its stack depth to reply package 
    for (int i =0; i < count; i++){
        // Attain monitor and its stack depth from returned data.
        jobject monitor = pMonitorInfos[i].monitor;
        m_cmdParser->reply.WriteTaggedObjectID(jni, monitor);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "OwnedMonitorsStackDepthInfo: received: monitor object=%p", monitor));
        
        jint stack_depth = pMonitorInfos[i].stack_depth;
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "OwnedMonitorsStackDepthInfo: received: monitor stack depth=%d", stack_depth));
        m_cmdParser->reply.WriteInt(stack_depth);
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//ForceEarlyReturnHandler------------------------------------------------------

int
ThreadReference::ForceEarlyReturnHandler::Execute(JNIEnv *jni)
{
    // Read thread id from ForceEarlyReturnHandler command
    jthread thrd = m_cmdParser->command.ReadThreadID(jni);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ForceEarlyReturn Command: received: threadID = %p", thrd));

    // If the thread is not suspended, throw exception
    if (!GetThreadManager().IsSuspended(thrd)) {
        AgentException e(JDWP_ERROR_THREAD_NOT_SUSPENDED);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_THREAD_NOT_SUSPENDED;
    }

    // Attain return value type from the command
    jdwpTaggedValue taggedValue = m_cmdParser->command.ReadValue(jni);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ForceEarlyReturn Command: received value type: %d", taggedValue.tag));
    
    // Invoke relevant jvmti function according to return value's type
    jvmtiError err = JVMTI_ERROR_NONE;      
    switch(taggedValue.tag){
        case JDWP_TAG_OBJECT:
        case JDWP_TAG_ARRAY:
        case JDWP_TAG_STRING:
        case JDWP_TAG_THREAD:
        case JDWP_TAG_THREAD_GROUP:
        case JDWP_TAG_CLASS_LOADER:
        case JDWP_TAG_CLASS_OBJECT:{
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ForceEarlyReturn Command: jobject return value: %p", taggedValue.value.l));
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->ForceEarlyReturnObject(thrd, taggedValue.value.l));
            break;
        }
        case JDWP_TAG_BOOLEAN:
        case JDWP_TAG_BYTE:
        case JDWP_TAG_CHAR:
        case JDWP_TAG_SHORT:
        case JDWP_TAG_INT:{
            jint ivalue = 0;
            switch (taggedValue.tag) {
                case JDWP_TAG_BOOLEAN:
                    ivalue = static_cast<jint>(taggedValue.value.z);
                    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ForceEarlyReturn Command: value=(boolean)%d", taggedValue.value.z));
                    break;
                case JDWP_TAG_BYTE:
                    ivalue = static_cast<jint>(taggedValue.value.b);
                    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ForceEarlyReturn Command: value=(byte)%d", taggedValue.value.b));
                    break;
                case JDWP_TAG_CHAR:
                    ivalue = static_cast<jint>(taggedValue.value.c);
                    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ForceEarlyReturn Command: value=(char)%d", taggedValue.value.c));
                    break;
                case JDWP_TAG_SHORT:
                    ivalue = static_cast<jint>(taggedValue.value.s);
                    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ForceEarlyReturn Command: value=(short)%d", taggedValue.value.s));
                    break;
                case JDWP_TAG_INT:
                    ivalue = taggedValue.value.i;
                    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ForceEarlyReturn Command: value=(int)%d", taggedValue.value.i));
                    break;
            }
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->ForceEarlyReturnInt(thrd, ivalue));
            break;
        }
        case JDWP_TAG_LONG:{
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ForceEarlyReturn Command: jlong return value:%lld", taggedValue.value.j));
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->ForceEarlyReturnLong(thrd, taggedValue.value.j));
            break;
        }
        case JDWP_TAG_FLOAT:{
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ForceEarlyReturn Command: jfloat return value:%f", taggedValue.value.f));
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->ForceEarlyReturnFloat(thrd, taggedValue.value.f));
            break;
            
        }
        case JDWP_TAG_DOUBLE:{
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ForceEarlyReturn Command: jdouble return value:%Lf", taggedValue.value.d));
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->ForceEarlyReturnDouble(thrd, taggedValue.value.d));
            break;
        }
        case JDWP_TAG_VOID:{
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ForceEarlyReturn Command: void return value"));
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->ForceEarlyReturnVoid(thrd));
            break;
        }
        default:{
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ForceEarlyReturn Command: Value's type is not supported %d", taggedValue.tag));
            AgentException e(JDWP_ERROR_INVALID_TAG);
			JDWP_SET_EXCEPTION(e);
            return JDWP_ERROR_INVALID_TAG;
        }
    }
    
    if (err != JVMTI_ERROR_NONE) {
        AgentException e(err);
		JDWP_SET_EXCEPTION(e);
        return err;
    }
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ForceEarlyReturn Command finished."));

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
