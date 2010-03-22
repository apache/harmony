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
 * @author Vitaly A. Provodin, Pavel N. Vyssotski
 */
#include "EventRequest.h"
#include "PacketParser.h"
#include "RequestManager.h"
#include "Log.h"

using namespace jdwp;
using namespace EventRequest;

//-----------------------------------------------------------------------------
//Set--------------------------------------------------------------------------

void
EventRequest::SetHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jdwpEventKind eventKind = (jdwpEventKind) m_cmdParser->command.ReadByte();
    jdwpSuspendPolicy suspendPolicy =
        (jdwpSuspendPolicy) m_cmdParser->command.ReadByte();
    jint modCount = m_cmdParser->command.ReadInt();

    JDWP_TRACE_DATA("Set: event="
        << GetRequestManager().GetEventKindName(eventKind) 
        << ", eventKind=" << eventKind 
        << ", suspendPolicy=" << suspendPolicy
        << ", modCount=" << modCount);

    AgentEventRequest* request = (eventKind == JDWP_EVENT_SINGLE_STEP) ?
        new StepRequest(suspendPolicy, modCount) :
        new AgentEventRequest(eventKind, suspendPolicy, modCount);

    try {
        for (jint i = 0; i < modCount; i++) {
            RequestModifier* modifier;
            jbyte modifierByte = m_cmdParser->command.ReadByte();
            switch (modifierByte) {
            case JDWP_MODIFIER_COUNT:
                {
                    jint n = m_cmdParser->command.ReadInt();
                    if (n <= 0) {
                        throw AgentException(JDWP_ERROR_INVALID_COUNT);
                    }
                    modifier = new CountModifier(n);
                    JDWP_TRACE_DATA("Set: modifier=COUNT, count=" << n);
                    break;
                }

            case JDWP_MODIFIER_CONDITIONAL:
                {
                    jint id = m_cmdParser->command.ReadInt();
                    modifier = new ConditionalModifier(id);
                    JDWP_TRACE_DATA("Set: modifier=CONDITIONAL, exprID=" << id);
                    break;
                }

            case JDWP_MODIFIER_THREAD_ONLY:
                {
                    if (eventKind == JDWP_EVENT_CLASS_UNLOAD ||
                        eventKind == JDWP_EVENT_VM_DEATH)
                    {
                        throw IllegalArgumentException();
                    }
                    jthread thread = m_cmdParser->command.ReadThreadID(jni);
#ifndef NDEBUG                    
                    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
                        jvmtiError err;
                        jvmtiThreadInfo threadInfo;
                        JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(thread, &threadInfo));
                        JDWP_TRACE_DATA("Set: modifier=THREAD_ONLY, thread=" << thread 
                            << ", name=" << threadInfo.name);
                    }
#endif
                    if (thread == 0) {
                        throw AgentException(JDWP_ERROR_INVALID_THREAD);
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
                        throw IllegalArgumentException();
                    }
                    jclass cls = m_cmdParser->command.ReadReferenceTypeID(jni);
#ifndef NDEBUG  
                    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
                        jvmtiError err;
                        char* signature = 0;
                        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(cls, &signature, 0));
                        JvmtiAutoFree afs(signature);
                        JDWP_TRACE_DATA("Set: modifier=CLASS_ONLY, refTypeID=" << cls
                            << ", signature=" << JDWP_CHECK_NULL(signature));
                    }
#endif

                    if (cls == 0) {
                        throw AgentException(JDWP_ERROR_INVALID_CLASS);
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
                        throw IllegalArgumentException();
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
                    JDWP_TRACE_DATA("Set: modifier=CLASS_MATCH, classPattern=" 
                        << JDWP_CHECK_NULL(pattern));
                    break;
                }

            case JDWP_MODIFIER_CLASS_EXCLUDE:
                {
                    if (eventKind == JDWP_EVENT_THREAD_START ||
                        eventKind == JDWP_EVENT_THREAD_END ||
                        eventKind == JDWP_EVENT_VM_DEATH)
                    {
                        throw IllegalArgumentException();
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
                    JDWP_TRACE_DATA("Set: modifier=CLASS_EXCLUDE, classPattern=" 
                        << JDWP_CHECK_NULL(pattern));
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
                        throw IllegalArgumentException();
                    }
                    m_cmdParser->command.ReadByte(); // typeTag
                    jclass cls = m_cmdParser->command.ReadReferenceTypeID(jni);
                    jmethodID method = m_cmdParser->command.ReadMethodID(jni);
                    jlocation loc = m_cmdParser->command.ReadLong();

#ifndef NDEBUG                    
                    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
                        jvmtiError err;
                        char* methodName = 0;
                        char* methodSignature = 0;
                        JVMTI_TRACE(err, GetJvmtiEnv()->GetMethodName(method, &methodName, &methodSignature, 0));
                        JvmtiAutoFree afs(methodSignature);
                        JvmtiAutoFree afn(methodName);
                        JDWP_TRACE_DATA("Set: modifier=LOCATION_ONLY, loc=" << loc
                            << ", method=" << method
                            << ", name=" << JDWP_CHECK_NULL(methodName)
                            << ", signature=" << JDWP_CHECK_NULL(methodSignature));
                    }
#endif                      
                    modifier = new LocationOnlyModifier(jni, cls, method, loc);
                    break;
                }

            case JDWP_MODIFIER_EXCEPTION_ONLY:
                {
                    if (eventKind != JDWP_EVENT_EXCEPTION) {
                        throw IllegalArgumentException();
                    }
                    jclass cls = m_cmdParser->command.ReadReferenceTypeIDOrNull(jni);
                    jboolean caught = m_cmdParser->command.ReadBoolean();
                    jboolean uncaught = m_cmdParser->command.ReadBoolean();

#ifndef NDEBUG                    
                    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
                        jvmtiError err;
                        char* signature = 0;
                        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(cls, &signature, 0));
                        JvmtiAutoFree afs(signature);
                        JDWP_TRACE_DATA("Set: modifier=EXCEPTION_ONLY, refTypeID=" << cls
                            << ", signature=" << JDWP_CHECK_NULL(signature)
                            << ", caught=" << (int)caught
                            << ", uncaught=" << (int)uncaught);
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
                        throw IllegalArgumentException();
                    }
                    jclass cls = m_cmdParser->command.ReadReferenceTypeID(jni);
                                        if (cls == 0) {
                        throw AgentException(JDWP_ERROR_INVALID_CLASS);
                    }
                    jfieldID field = 
                    m_cmdParser->command.ReadFieldID(jni);
#ifndef NDEBUG                    
                    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
                        jvmtiError err;
                        char* signature = 0;
                        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(cls, &signature, 0));
                        JvmtiAutoFree afs(signature);
                        char* fieldName = 0;
                        JVMTI_TRACE(err, GetJvmtiEnv()->GetFieldName(cls, field, &fieldName, 0, 0));
                        JvmtiAutoFree afn(fieldName);
                        JDWP_TRACE_DATA("Set: modifier=FIELD_ONLY, refTypeId=" << cls
                            << ", signature=" << JDWP_CHECK_NULL(signature)
                            << ", fieldName=" << JDWP_CHECK_NULL(fieldName));
                    }
#endif
                    modifier = new FieldOnlyModifier(jni, cls, field);
                    break;
                }

            case JDWP_MODIFIER_STEP:
                {
                    if (eventKind != JDWP_EVENT_SINGLE_STEP) {
                        throw IllegalArgumentException();
                    }
                    jthread thread = m_cmdParser->command.ReadThreadID(jni);
                    jint size = m_cmdParser->command.ReadInt();
                    jint depth = m_cmdParser->command.ReadInt();

#ifndef NDEBUG                    
                    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
                        jvmtiError err;
                        jvmtiThreadInfo threadInfo;
                        JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(thread, &threadInfo));
                        JvmtiAutoFree aftn(threadInfo.name);
                        JDWP_TRACE_DATA("Set: modifier=STEP, thread=" << thread
                            << ", name=" << JDWP_CHECK_NULL(threadInfo.name)
                            << ", size=" << size
                            << ", depth=" << depth);
                    }
#endif

                    if (thread == 0) {
                        throw AgentException(JDWP_ERROR_INVALID_THREAD);
                    }
                    reinterpret_cast<StepRequest*>
                        (request)->Init(jni, thread, size, depth);
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
                        throw IllegalArgumentException();
                    }

                    jobject instance = m_cmdParser->command.ReadObjectID(jni);
                    JDWP_TRACE_DATA("Set: modifier=INSTANCE_ONLY, instance=" << instance);

                    modifier = new InstanceOnlyModifier(jni, instance);
                    break;
                }

            default:
                JDWP_TRACE_DATA("Set: bad modifier: " << modifierByte);
                throw IllegalArgumentException();

            }
            request->AddModifier(modifier, i);
        }
    } catch (AgentException& e) {
        delete request;
        throw e;
    }

    RequestID id = GetRequestManager().AddRequest(jni, request);
    JDWP_TRACE_DATA("Set: send: requestId=" << id);
    m_cmdParser->reply.WriteInt(id);
}

//-----------------------------------------------------------------------------
//Clear------------------------------------------------------------------------

void
EventRequest::ClearHandler::Execute(JNIEnv *jni)
    throw(AgentException)
{
    jdwpEventKind eventKind = (jdwpEventKind) m_cmdParser->command.ReadByte();
    RequestID id = m_cmdParser->command.ReadInt();
    JDWP_TRACE_DATA("Clear: event="
        << GetRequestManager().GetEventKindName(eventKind) 
        << ", eventKind=" << eventKind << ", requestId=" << id);
    GetRequestManager().DeleteRequest(jni, eventKind, id);
}

//-----------------------------------------------------------------------------
//ClearAllBreakpoints----------------------------------------------------------

void
EventRequest::ClearAllBreakpointsHandler::Execute(JNIEnv *jni)
    throw(AgentException)
{
    JDWP_TRACE_DATA("ClearAllBreakpoints");
    GetRequestManager().DeleteAllBreakpoints(jni);
}

//-----------------------------------------------------------------------------
