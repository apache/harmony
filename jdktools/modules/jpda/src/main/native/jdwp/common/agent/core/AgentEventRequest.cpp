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
 * @author Pavel N. Vyssotski
 */
// AgentEventRequest.cpp

#include <string.h>
#include "AgentEventRequest.h"
#include "RequestManager.h"
#include "Log.h"

using namespace jdwp;

AgentEventRequest::AgentEventRequest(jdwpEventKind kind,
        jdwpSuspendPolicy suspend, jint modCount)
    throw(AgentException)
{
    m_requestId = 0;
    m_eventKind = kind;
    m_suspendPolicy = suspend;
    m_modifierCount = modCount;
    m_modifiers = 0;
    m_isExpired = false;
    if (modCount != 0) {
        m_modifiers = reinterpret_cast<RequestModifier**>
            (GetMemoryManager().Allocate(sizeof(RequestModifier*)*modCount JDWP_FILE_LINE));
        memset(m_modifiers, 0, sizeof(RequestModifier*)*modCount);
    }
}

AgentEventRequest::~AgentEventRequest() throw()
{
    for (jint i = 0; i < m_modifierCount; i++) {
        delete m_modifiers[i];
    }
    if (m_modifiers != 0) {
        GetMemoryManager().Free(m_modifiers JDWP_FILE_LINE);
    }
}

bool AgentEventRequest::ApplyModifiers(JNIEnv *jni, EventInfo &eInfo)
    throw(AgentException)
{
    for (jint i = 0; i < m_modifierCount; i++) {
        if (!m_modifiers[i]->Apply(jni, eInfo)) {
            return false;
        }
        if ((m_modifiers[i])->GetKind() == JDWP_MODIFIER_COUNT) {
            // once the count reaches 0, the event becomes expired
            m_isExpired = true;
        }
    }
    return true;
}

jthread AgentEventRequest::GetThread() const throw()
{
    for (jint i = 0; i < m_modifierCount; i++) {
        if ((m_modifiers[i])->GetKind() == JDWP_MODIFIER_THREAD_ONLY) {
            return (reinterpret_cast<ThreadOnlyModifier*>
                (m_modifiers[i]))->GetThread();
        }
    }
    return 0;
}

FieldOnlyModifier* AgentEventRequest::GetField() const throw()
{
    for (jint i = 0; i < m_modifierCount; i++) {
        if ((m_modifiers[i])->GetKind() == JDWP_MODIFIER_FIELD_ONLY) {
            return reinterpret_cast<FieldOnlyModifier*>(m_modifiers[i]);
        }
    }
    return 0;
}

LocationOnlyModifier* AgentEventRequest::GetLocation() const throw()
{
    for (jint i = 0; i < m_modifierCount; i++) {
        if ((m_modifiers[i])->GetKind() == JDWP_MODIFIER_LOCATION_ONLY) {
            return reinterpret_cast<LocationOnlyModifier*>(m_modifiers[i]);
        }
    }
    return 0;
}


//-----------------------------------------------------------------------------
// StepRequest
//-----------------------------------------------------------------------------

StepRequest::~StepRequest() throw()
{
    ControlSingleStep(false);
    JNIEnv *jni = GetJniEnv();
    if (m_framePopRequest != 0) {
        GetRequestManager().DeleteRequest(jni, m_framePopRequest);
    }
    if (m_methodEntryRequest != 0) {
        GetRequestManager().DeleteRequest(jni, m_methodEntryRequest);
    }
    jni->DeleteGlobalRef(m_thread);
}

jint StepRequest::GetCurrentLine() throw()
{
    jint lineNumber = -1;
    if (m_size == JDWP_STEP_LINE) {
        jmethodID method;
        jlocation location;
        jvmtiError err;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetFrameLocation(m_thread, 0,
            &method, &location));
        if (err == JVMTI_ERROR_NONE && location != -1) {
            jint cnt;
            jvmtiLineNumberEntry* table = 0;
            JVMTI_TRACE(err, GetJvmtiEnv()->GetLineNumberTable(method,
                &cnt, &table));
            JvmtiAutoFree jafTable(table);
            if (err == JVMTI_ERROR_NONE && cnt > 0) {
                jint i = 1;
                while (i < cnt && location >= table[i].start_location) {
                    i++;
                }
                lineNumber = table[i-1].line_number;
            }
        }
    }
    return lineNumber;
}

void StepRequest::ControlSingleStep(bool enable) throw()
{
    JDWP_TRACE_EVENT("control Step: "<< (enable ? "on" : "off")
        << ", thread=" << m_thread);
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->SetEventNotificationMode(
        (enable) ? JVMTI_ENABLE : JVMTI_DISABLE,
        JVMTI_EVENT_SINGLE_STEP, m_thread));
    m_isActive = enable;
}

void StepRequest::Restore() throw(AgentException) {
    JDWP_TRACE_EVENT("Restore stepRequest: " << (m_isActive ? "on" : "off"));
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->SetEventNotificationMode(
        (m_isActive) ? JVMTI_ENABLE : JVMTI_DISABLE,
        JVMTI_EVENT_SINGLE_STEP, m_thread));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
}

bool StepRequest::IsClassApplicable(JNIEnv* jni, EventInfo &eInfo) throw()
{
    for (jint i = 0; i < m_modifierCount; i++) {
        switch ((m_modifiers[i])->GetKind()) {
        case JDWP_MODIFIER_CLASS_ONLY:
        case JDWP_MODIFIER_CLASS_MATCH:
        case JDWP_MODIFIER_CLASS_EXCLUDE:
            if (!m_modifiers[i]->Apply(jni, eInfo)) {
                return false;
            }
            break;
        case JDWP_MODIFIER_COUNT:
            return true;
        default:
            break;
        }
    }
    return true;
}

void StepRequest::OnFramePop(JNIEnv *jni)
    throw(AgentException)
{
    JDWP_ASSERT(m_framePopRequest != 0);

    jint currentCount;
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetFrameCount(m_thread, &currentCount));
    if (err != JVMTI_ERROR_NONE) {
        currentCount = -1;
    }

    if (m_depth == JDWP_STEP_OVER ||
        (m_depth == JDWP_STEP_OUT && currentCount <= m_frameCount) ||
        (m_methodEntryRequest != 0 && currentCount-1 <= m_frameCount))
    {
        ControlSingleStep(true);
        if (m_methodEntryRequest != 0) {
            GetRequestManager().DeleteRequest(jni, m_methodEntryRequest);
            m_methodEntryRequest = 0;
        }
    }
}

void StepRequest::OnMethodEntry(JNIEnv *jni, EventInfo &eInfo)
    throw(AgentException)
{
    JDWP_ASSERT(m_methodEntryRequest != 0);
    JDWP_ASSERT(m_depth == JDWP_STEP_INTO);

    if ((m_size == JDWP_STEP_MIN || GetCurrentLine() != -1) &&
        IsClassApplicable(jni, eInfo))
    {
        ControlSingleStep(true);
        m_methodEntryRequest->SetExpired(true);
        m_methodEntryRequest = 0;
    }
}

void StepRequest::Init(JNIEnv *jni, jthread thread, jint size, jint depth)
    throw(AgentException)
{
    m_thread = jni->NewGlobalRef(thread);
    if (m_thread == 0) {
        throw OutOfMemoryException();
    }
    m_size = size;
    m_depth = depth;

    if (m_depth != JDWP_STEP_INTO || m_size != JDWP_STEP_MIN) {
        jvmtiError err;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetFrameCount(m_thread, &m_frameCount));
        if (err != JVMTI_ERROR_NONE) {
            m_frameCount = -1;
        }
        if (m_size == JDWP_STEP_LINE) {
            m_lineNumber = GetCurrentLine();
        }
    }

    if (m_depth == JDWP_STEP_INTO || m_frameCount > 0) {
        // add internal FramePop event request for the thread
        m_framePopRequest =
            new AgentEventRequest(JDWP_EVENT_FRAME_POP, JDWP_SUSPEND_NONE, 1);
        m_framePopRequest->AddModifier(new ThreadOnlyModifier(jni, thread), 0);
        GetRequestManager().AddInternalRequest(jni, m_framePopRequest);
        jvmtiError err;
        JVMTI_TRACE(err, GetJvmtiEnv()->NotifyFramePop(m_thread, 0));
        if (err == JVMTI_ERROR_OPAQUE_FRAME) {
            m_isNative = true;
        }
    }

    if (m_depth == JDWP_STEP_INTO ||
        (m_depth == JDWP_STEP_OUT && m_frameCount > 0 && m_isNative) ||
        (m_depth == JDWP_STEP_OVER && m_frameCount > 0 &&
         (m_size == JDWP_STEP_MIN || m_isNative || m_lineNumber != -1)))
    {
        ControlSingleStep(true);
    }

    JDWP_TRACE_EVENT("step start: size=" << m_size << ", depth=" << m_depth
        << ", frame=" << m_frameCount << ", line=" << m_lineNumber);
}

bool StepRequest::ApplyModifiers(JNIEnv *jni, EventInfo &eInfo)
    throw(AgentException)
{
    JDWP_ASSERT(eInfo.thread != 0);

    if (JNI_FALSE == jni->IsSameObject(eInfo.thread, m_thread)) {
        return false;
    }

    jint currentCount = 0;
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetFrameCount(m_thread, &currentCount));
    if (err != JVMTI_ERROR_NONE) {
        return false;
    }

    jint currentLine = 0;
    if (m_size == JDWP_STEP_LINE) {
        currentLine = GetCurrentLine();
    }

    if (currentCount < m_frameCount) {
        // method exit
    } else if (currentCount > m_frameCount) {
        // method entry
        if (m_depth != JDWP_STEP_INTO || !IsClassApplicable(jni, eInfo)) {
            ControlSingleStep(false);
            if (m_depth == JDWP_STEP_INTO) {
                // add internal MethodEntry event request for the thread
                m_methodEntryRequest = new AgentEventRequest(
                    JDWP_EVENT_METHOD_ENTRY, JDWP_SUSPEND_NONE, 1);
                m_methodEntryRequest->AddModifier(
                    new ThreadOnlyModifier(jni, m_thread), 0);
                GetRequestManager().AddInternalRequest(
                    jni, m_methodEntryRequest);
            }
            JVMTI_TRACE(err, GetJvmtiEnv()->NotifyFramePop(m_thread, 0));
            if (err == JVMTI_ERROR_OPAQUE_FRAME) {
                m_isNative = true;
            }
            return false;
        }
    } else { // currentCount == m_frameCount
        // check against line
        if (m_size == JDWP_STEP_LINE && currentLine == m_lineNumber && currentLine != -1) {
            return false;
        }
    }

    m_frameCount = currentCount;
    m_lineNumber = currentLine;

    JDWP_TRACE_EVENT("step: frame=" << m_frameCount << ", line=" << m_lineNumber);
    return AgentEventRequest::ApplyModifiers(jni, eInfo);
}
