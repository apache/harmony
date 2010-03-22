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
#include "EventRequest.h"
#include "PacketParser.h"
#include "RequestManager.h"
#include "Log.h"
#include "ExceptionManager.h"


using namespace jdwp;
using namespace EventRequest;

//-----------------------------------------------------------------------------
//Set--------------------------------------------------------------------------

int
EventRequest::SetHandler::Execute(JNIEnv *jni) 
{
    jdwpEventKind eventKind = (jdwpEventKind) m_cmdParser->command.ReadByte();
    jdwpSuspendPolicy suspendPolicy =
        (jdwpSuspendPolicy) m_cmdParser->command.ReadByte();
    jint modCount = m_cmdParser->command.ReadInt();
    jdwpError jdwpErr;

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: event=%s, eventKind=%d, suspendPolicy=%d, modCount=%d",
                    GetRequestManager().GetEventKindName(eventKind), eventKind, suspendPolicy, modCount));

    AgentEventRequest* request = (eventKind == JDWP_EVENT_SINGLE_STEP) ?
        new StepRequest(suspendPolicy, modCount) :
        new AgentEventRequest(eventKind, suspendPolicy, modCount);

    for (jint i = 0; i < modCount; i++) {
        RequestModifier* modifier;
        jbyte modifierByte = m_cmdParser->command.ReadByte();
        switch (modifierByte) {
        case JDWP_MODIFIER_COUNT:
            {
                jint n = m_cmdParser->command.ReadInt();
                if (n <= 0) {
                    AgentException e(JDWP_ERROR_INVALID_COUNT);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_INVALID_COUNT;
                }
                modifier = new CountModifier(n);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: modifier=COUNT, count=%d", n));
                break;
            }

        case JDWP_MODIFIER_CONDITIONAL:
            {
                jint id = m_cmdParser->command.ReadInt();
                modifier = new ConditionalModifier(id);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: modifier=CONDITIONAL, exprID=%d", id));
                break;
            }

        case JDWP_MODIFIER_THREAD_ONLY:
            {
                if (eventKind == JDWP_EVENT_CLASS_UNLOAD ||
                    eventKind == JDWP_EVENT_VM_DEATH)
                {
                    AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_ILLEGAL_ARGUMENT;
                }
                jthread thread = m_cmdParser->command.ReadThreadID(jni);
#ifndef NDEBUG                    
                if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
                    jvmtiError err;
                    jvmtiThreadInfo threadInfo;
                    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &threadInfo));
                    JvmtiAutoFree jafInfoName(threadInfo.name);
                    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: modifier=THREAD_ONLY, thread=%p, name=%s", thread, threadInfo.name));
                }
#endif
                if (thread == 0) {
                    AgentException e(JDWP_ERROR_INVALID_THREAD);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_INVALID_THREAD;
                }
                modifier = new ThreadOnlyModifier(jni, thread);
                break;
            }

        case JDWP_MODIFIER_CLASS_ONLY:
            {
                if (eventKind == JDWP_EVENT_CLASS_UNLOAD ||
                    eventKind == JDWP_EVENT_THREAD_START ||
                    eventKind == JDWP_EVENT_THREAD_END ||
                    eventKind == JDWP_EVENT_VM_DEATH)
                {
                    AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_ILLEGAL_ARGUMENT;
                }
                jclass cls = m_cmdParser->command.ReadReferenceTypeID(jni);
#ifndef NDEBUG  
                if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
                    jvmtiError err;
                    char* signature = 0;
                    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(cls, &signature, 0));
                    JvmtiAutoFree afs(signature);
                    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: modifier=CLASS_ONLY, refTypeID=%p, signature=%s", cls, JDWP_CHECK_NULL(signature)));
                }
#endif

                if (cls == 0) {
                    AgentException e(JDWP_ERROR_INVALID_CLASS);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_INVALID_CLASS;
                }
                modifier = new ClassOnlyModifier(jni, cls);
                break;
            }

        case JDWP_MODIFIER_CLASS_MATCH:
            {
                if (eventKind == JDWP_EVENT_THREAD_START ||
                    eventKind == JDWP_EVENT_THREAD_END ||
                    eventKind == JDWP_EVENT_VM_DEATH)
                {
                    AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_ILLEGAL_ARGUMENT;
                }
                char* pattern = m_cmdParser->command.ReadStringNoFree();
                JDWP_ASSERT(pattern != 0);
                // replace '.' with '/' to be matched with signature
                for (char* p = pattern; *p != '\0'; p++) {
                    if (*p == '.') {
                        *p = '/';
                    }
                }
                modifier = new ClassMatchModifier(pattern);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: modifier=CLASS_MATCH, classPattern=%s", JDWP_CHECK_NULL(pattern)));
                break;
            }

        case JDWP_MODIFIER_CLASS_EXCLUDE:
            {
                if (eventKind == JDWP_EVENT_THREAD_START ||
                    eventKind == JDWP_EVENT_THREAD_END ||
                    eventKind == JDWP_EVENT_VM_DEATH)
                {
                    AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_ILLEGAL_ARGUMENT;
                }
                char* pattern = m_cmdParser->command.ReadStringNoFree();
                JDWP_ASSERT(pattern != 0);
                // replace '.' with '/' to be matched with signature
                for (char* p = pattern; *p != '\0'; p++) {
                    if (*p == '.') {
                        *p = '/';
                    }
                }
                modifier = new ClassExcludeModifier(pattern);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: modifier=CLASS_EXCLUDE, classPattern=%s", JDWP_CHECK_NULL(pattern)));
                break;
            }

        case JDWP_MODIFIER_LOCATION_ONLY:
            {
                if (eventKind != JDWP_EVENT_BREAKPOINT &&
                    eventKind != JDWP_EVENT_FIELD_ACCESS &&
                    eventKind != JDWP_EVENT_FIELD_MODIFICATION &&
                    eventKind != JDWP_EVENT_SINGLE_STEP &&
                    eventKind != JDWP_EVENT_EXCEPTION)
                {
                    AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_ILLEGAL_ARGUMENT;
                }
                m_cmdParser->command.ReadByte(); // typeTag
                jclass cls = m_cmdParser->command.ReadReferenceTypeID(jni);
                if (cls == 0) {
                    AgentException e(JDWP_ERROR_INVALID_CLASS);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_INVALID_CLASS;
                }

                jmethodID method = m_cmdParser->command.ReadMethodID(jni);
                jlocation loc = m_cmdParser->command.ReadLong();

#ifndef NDEBUG                    
                if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
                    jvmtiError err;
                    char* methodName = 0;
                    char* methodSignature = 0;
                    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(method, &methodName, &methodSignature, 0));
                    JvmtiAutoFree afs(methodSignature);
                    JvmtiAutoFree afn(methodName);
                    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: modifier=LOCATION_ONLY, loc=%lld, method=%p, name=%s, signature=%s", 
                                    loc, method, JDWP_CHECK_NULL(methodName), JDWP_CHECK_NULL(methodSignature)));
                }
#endif                      
                modifier = new LocationOnlyModifier(jni, cls, method, loc);
                break;
            }

        case JDWP_MODIFIER_EXCEPTION_ONLY:
            {
                if (eventKind != JDWP_EVENT_EXCEPTION) {
                    AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_ILLEGAL_ARGUMENT;
                }
                jclass cls = m_cmdParser->command.ReadReferenceTypeIDOrNull(jni);
                jdwpErr = JDWP_LAST_ERROR_CODE;
                if (jdwpErr != JDWP_ERROR_NONE) {
                    delete request;
                    return jdwpErr;
                }

                jboolean caught = m_cmdParser->command.ReadBoolean();
                jboolean uncaught = m_cmdParser->command.ReadBoolean();

#ifndef NDEBUG                    
                if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
                    jvmtiError err;
                    char* signature = 0;
                    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(cls, &signature, 0));
                    JvmtiAutoFree afs(signature);
                    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: modifier=EXCEPTION_ONLY, refTypeID=%p, signature=%s, caught=%s, uncaught=%s", 
                                    cls, JDWP_CHECK_NULL(signature), (caught?"TRUE":"FALSE"), (uncaught?"TRUE":"FALSE")));
                }
#endif
                modifier = new ExceptionOnlyModifier(jni, cls,
                    (caught == JNI_TRUE), (uncaught == JNI_TRUE));
                break;
            }

        case JDWP_MODIFIER_FIELD_ONLY:
            {
                if (eventKind != JDWP_EVENT_FIELD_ACCESS &&
                    eventKind != JDWP_EVENT_FIELD_MODIFICATION)
                {
                    AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_ILLEGAL_ARGUMENT;
                }
                jclass cls = m_cmdParser->command.ReadReferenceTypeID(jni);
                if (cls == 0) {
                    AgentException e(JDWP_ERROR_INVALID_CLASS);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_INVALID_CLASS;
                }
                jfieldID field = 
                m_cmdParser->command.ReadFieldID(jni);
#ifndef NDEBUG                    
                if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
                    jvmtiError err;
                    char* signature = 0;
                    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(cls, &signature, 0));
                    JvmtiAutoFree afs(signature);
                    char* fieldName = 0;
                    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFieldName(cls, field, &fieldName, 0, 0));
                    JvmtiAutoFree afn(fieldName);
                    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: modifier=FIELD_ONLY, refTypeId=%p, signature=%s, fieldName=%s",
                                    cls, JDWP_CHECK_NULL(signature), JDWP_CHECK_NULL(fieldName)));
                }
#endif
                modifier = new FieldOnlyModifier(jni, cls, field);
                break;
            }

        case JDWP_MODIFIER_STEP:
            {
                if (eventKind != JDWP_EVENT_SINGLE_STEP) {
                    AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_ILLEGAL_ARGUMENT;
                }
                jthread thread = m_cmdParser->command.ReadThreadID(jni);
                jint size = m_cmdParser->command.ReadInt();
                jint depth = m_cmdParser->command.ReadInt();

#ifndef NDEBUG                    
                if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
                    jvmtiError err;
                    jvmtiThreadInfo threadInfo;
                    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &threadInfo));
                    JvmtiAutoFree aftn(threadInfo.name);
                    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: modifier=STEP, thread=%p, name=%s, size=%d, depth=%d", 
                                    thread, JDWP_CHECK_NULL(threadInfo.name), size, depth));
                }
#endif

                if (thread == 0) {
                    AgentException e(JDWP_ERROR_INVALID_THREAD);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_INVALID_THREAD;
                }
                int ret  = (reinterpret_cast<StepRequest*>
                    (request))->Init(jni, thread, size, depth);
                if (ret != JDWP_ERROR_NONE) {
                    delete request;
                    return ret;
                }
                modifier = new StepModifier(jni, thread, size, depth);
                break;
            }

        case JDWP_MODIFIER_INSTANCE_ONLY:
            {
                if (eventKind == JDWP_EVENT_CLASS_PREPARE ||
                    eventKind == JDWP_EVENT_CLASS_UNLOAD ||
                    eventKind == JDWP_EVENT_THREAD_START ||
                    eventKind == JDWP_EVENT_THREAD_END ||
                    eventKind == JDWP_EVENT_VM_DEATH)
                {
                    AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_ILLEGAL_ARGUMENT;
                }

                jobject instance = m_cmdParser->command.ReadObjectID(jni);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: modifier=INSTANCE_ONLY, instance=%p", instance));

                modifier = new InstanceOnlyModifier(jni, instance);
                break;
            }

        // New modifier kind  for Java 6
        case JDWP_MODIFIER_SOURCE_NAME_MATCH:
            {
                if (eventKind != JDWP_EVENT_CLASS_PREPARE )
                {
                    AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
                    JDWP_SET_EXCEPTION(e);
                    delete request;
                    return JDWP_ERROR_ILLEGAL_ARGUMENT;
                }

                char* pattern = m_cmdParser->command.ReadStringNoFree();
                JDWP_ASSERT(pattern != 0);

                modifier = new SourceNameMatchModifier(pattern);
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: modifier=SOURCE_NAME_MATCH, classPattern=%s", JDWP_CHECK_NULL(pattern)));
                break;
            }
        default:
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: bad modifier: %d", modifierByte));
            AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
            JDWP_SET_EXCEPTION(e);
            delete request;
            return JDWP_ERROR_ILLEGAL_ARGUMENT;

        }
        request->AddModifier(modifier, i);
    }

    RequestID id = GetRequestManager().AddRequest(jni, request);
    if (id == 0) {
        return JDWP_ERROR_INTERNAL;
    }
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Set: send: requestId=%d", id));
    m_cmdParser->reply.WriteInt(id);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//Clear------------------------------------------------------------------------

int
EventRequest::ClearHandler::Execute(JNIEnv *jni)
{
    jdwpEventKind eventKind = (jdwpEventKind) m_cmdParser->command.ReadByte();
    RequestID id = m_cmdParser->command.ReadInt();
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Clear: event=%s, eventKind=%d, requestId=%d",
                    GetRequestManager().GetEventKindName(eventKind), eventKind, id));
    int ret = GetRequestManager().DeleteRequest(jni, eventKind, id);
    JDWP_CHECK_RETURN(ret);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
//ClearAllBreakpoints----------------------------------------------------------

int
EventRequest::ClearAllBreakpointsHandler::Execute(JNIEnv *jni)
{
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ClearAllBreakpoints"));
    GetRequestManager().DeleteAllBreakpoints(jni);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------
