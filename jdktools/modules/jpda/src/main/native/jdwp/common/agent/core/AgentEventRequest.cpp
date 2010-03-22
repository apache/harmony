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
#include "AgentEventRequest.h"
#include "RequestManager.h"
#include "Log.h"

#include <string.h>

#if defined(ZOS)
#define _XOPEN_SOURCE  500
#include <unistd.h>
#endif

using namespace jdwp;

AgentEventRequest::AgentEventRequest(jdwpEventKind kind,
        jdwpSuspendPolicy suspend, jint modCount)
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

AgentEventRequest::~AgentEventRequest()
{
    for (jint i = 0; i < m_modifierCount; i++) {
        delete m_modifiers[i];
    }
    if (m_modifiers != 0) {
        GetMemoryManager().Free(m_modifiers JDWP_FILE_LINE);
    }
}

bool AgentEventRequest::ApplyModifiers(JNIEnv *jni, EventInfo &eInfo)

{
    JDWP_TRACE_ENTRY(LOG_DEBUG, (LOG_FUNC_FL, "ApplyModifiers(%p, ...)", jni));
    
    for (jint i = 0; i < m_modifierCount; i++) {
        JDWP_TRACE(LOG_DEBUG, (LOG_EVENT_FL, "ApplyModifiers: index=%d, modifier_kind=%d", i, (m_modifiers[i])->GetKind()));
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

jthread AgentEventRequest::GetThread() const
{
    for (jint i = 0; i < m_modifierCount; i++) {
        if ((m_modifiers[i])->GetKind() == JDWP_MODIFIER_THREAD_ONLY) {
            return (reinterpret_cast<ThreadOnlyModifier*>
                (m_modifiers[i]))->GetThread();
        }
    }
    return 0;
}

FieldOnlyModifier* AgentEventRequest::GetField() const
{
    for (jint i = 0; i < m_modifierCount; i++) {
        if ((m_modifiers[i])->GetKind() == JDWP_MODIFIER_FIELD_ONLY) {
            return reinterpret_cast<FieldOnlyModifier*>(m_modifiers[i]);
        }
    }
    return 0;
}

LocationOnlyModifier* AgentEventRequest::GetLocation() const
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

StepRequest::~StepRequest()
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

jint StepRequest::GetCurrentLine()
{
    jint lineNumber = -1;
    char* sourceDebugExtension = 0;
    char* default_stratum;
    char* stratum;
    jmethodID method;
    jlocation location;
    jvmtiError err;

    if (m_size != JDWP_STEP_LINE)
        return -1;
    
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFrameLocation(m_thread, 0,
                                                     &method, &location));
    if (err == JVMTI_ERROR_NONE && location != -1) {
        jint cnt;
        jvmtiLineNumberEntry* table = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetLineNumberTable(method,
                                                           &cnt, &table));
        JvmtiAutoFree jafTable(table);
        if (err == JVMTI_ERROR_NONE && cnt > 0) {
            jint i = 1;
            while (i < cnt && location >= table[i].start_location) {
                i++;
            }
            lineNumber = table[i-1].line_number;
        }
    } else {
        return -1;
    }

    default_stratum = AgentBase::GetDefaultStratum();
    if (default_stratum != NULL && strcmp(default_stratum, "Java") == 0 )
        return lineNumber;

    jclass jvmClass;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodDeclaringClass(method, &jvmClass));
    if (err != JVMTI_ERROR_NONE)
        return -1;

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetSourceDebugExtension(jvmClass,
        &sourceDebugExtension));
    if (err != JVMTI_ERROR_NONE)
        return lineNumber;

    JvmtiAutoFree autoFreeDebugExtension(sourceDebugExtension);            

#ifdef ZOS
    /* Make sure we pass EBCDIC strings to zOS system functions */
    __atoe(sourceDebugExtension);
    if (default_stratum != NULL) {
        // Copy the string so we only convert the local version to EBCDIC
        char *temp = (char*)GetMemoryManager().Allocate(strlen(default_stratum)+1 JDWP_FILE_LINE);
        strcpy(temp, default_stratum);
        default_stratum = temp;
        __atoe(default_stratum);
    }
    // This is ok to do here since JvmtiAutoFree checks if the pointer is NULL
    // before freeing
    JvmtiAutoFree autoFreeDefaultStratum(default_stratum); 
#pragma convlit(suspend)
#endif /* ZOS */

    char *tok = strtok(sourceDebugExtension, "\n");
    if (tok == NULL) return -1;
    tok = strtok(NULL, "\n");
    if (tok == NULL) return -1;
    tok = strtok(NULL, "\n"); /* This is the preferred stratum for this class */
    if (tok == NULL) return -1;
    if ( ( default_stratum == NULL || strlen(default_stratum) == 0 ) &&
         strcmp(tok, "Java") == 0)
        return lineNumber;

    stratum = ( default_stratum == NULL || strlen(default_stratum) == 0 ) ?
                tok : default_stratum;

    while( ( tok = strtok(NULL, "\n") ) ) {
        if (strlen(tok) >= 2) {
            while (tok[0] == '*' && tok[1] == 'S' && tok[2] == ' ') {
                tok++; tok++;
                while (tok[0] == ' ' && tok[0] != 0) tok++; // skip spaces
                if (strcmp(stratum, tok) == 0) {
                    // this is the stratum that is required
                    tok = strtok(NULL, "\n");
                    if (tok == NULL) return -1;
                    // parse until we find another stratum section or 
                    // the end token
                    while (!(tok[0] == '*' && ( tok[1] == 'S'
                                                || tok[1] == 'E' ))) {
                        if (strlen(tok) >= 2 &&
                            tok[0] == '*' && tok[1] == 'L' && tok[2] == '\0') {
                            // parse line info section
                            do {
                                tok = strtok(NULL, "\n");
                                if (tok == NULL) return -1;
                                if (tok[0] >= '0' && tok[0] <= '9') {
                                    long int in_start = strtol(tok, &tok, 10);
                                    long int in_len = 1;
                                    long int out_start;
                                    long int out_len = 1;
                                    if (tok[0] == '#') {
                                        long ret;
                                        tok++;
                                         // ignore file id - capture return value to satisfy compiler
                                        ret = strtol(tok, &tok, 10);
                                    }
                                    if (tok[0] == ',') {
                                        tok++;
                                        in_len = strtol(tok, &tok, 10);
                                    }
                                    if (tok[0] != ':') {
                                        continue;
                                    }
                                    tok++;
                                    out_start = strtol(tok, &tok, 10);
                                    if (tok[0] == ',') {
                                        tok++;
                                        out_len = strtol(tok, &tok, 10);
                                    }
                                    if (lineNumber >= out_start 
                                        && lineNumber < out_start+(out_len
                                                                   *in_len)) {
                                        return in_start
                                            +(lineNumber-out_start)/out_len;
                                    }
                                }
                            } while (tok[0] != '*');
                            return -1;
                        }
                        tok = strtok(NULL, "\n");
                        if (tok == NULL) return -1;
                    }
                }
            }
        }
    }

#ifdef ZOS
#pragma convlit(resume)
#endif /* ZOS */

    return -1;
}

void StepRequest::ControlSingleStep(bool enable)
{
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "control Step: %s, thread=%p", (enable ? "on" : "off"), m_thread));
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetEventNotificationMode(
        (enable) ? JVMTI_ENABLE : JVMTI_DISABLE,
        JVMTI_EVENT_SINGLE_STEP, m_thread));
    m_isActive = enable;
}

int StepRequest::Restore() {
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "Restore stepRequest: %s", (m_isActive ? "on" : "off")));
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetEventNotificationMode(
        (m_isActive) ? JVMTI_ENABLE : JVMTI_DISABLE,
        JVMTI_EVENT_SINGLE_STEP, m_thread));
    if (err != JVMTI_ERROR_NONE) {
        AgentException ex = AgentException(err);
    	JDWP_SET_EXCEPTION(ex);
        return err;
    }

    return JDWP_ERROR_NONE;
}

bool StepRequest::IsClassApplicable(JNIEnv* jni, EventInfo &eInfo)
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

int StepRequest::OnFramePop(JNIEnv *jni)
{
    JDWP_ASSERT(m_framePopRequest != 0);

    int ret;
    if (m_depth == JDWP_STEP_OVER ||
        m_depth == JDWP_STEP_OUT ||
        m_methodEntryRequest != 0)
    {
        ControlSingleStep(true);
        if (m_methodEntryRequest != 0) {
            ret = GetRequestManager().DeleteRequest(jni, m_methodEntryRequest);
            JDWP_CHECK_RETURN(ret);
            m_methodEntryRequest = 0;
        }
    }

    return JDWP_ERROR_NONE;
}

void StepRequest::OnMethodEntry(JNIEnv *jni, EventInfo &eInfo)
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

int StepRequest::Init(JNIEnv *jni, jthread thread, jint size, jint depth)
{
    m_thread = jni->NewGlobalRef(thread);
    if (m_thread == 0) {
    	AgentException ex(JDWP_ERROR_OUT_OF_MEMORY);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_OUT_OF_MEMORY;
    }
    m_size = size;
    m_depth = depth;

    if (m_depth != JDWP_STEP_INTO || m_size != JDWP_STEP_MIN) {
        jvmtiError err;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFrameCount(m_thread, &m_frameCount));
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
        int ret = GetRequestManager().AddInternalRequest(jni, m_framePopRequest);
        JDWP_CHECK_RETURN(ret);
        jvmtiError err;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->NotifyFramePop(m_thread, 0));
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

    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "step start: size=%d, depth=%d, frame=%d, line=%d", m_size, m_depth, m_frameCount, m_lineNumber));
    return JDWP_ERROR_NONE;
}

bool StepRequest::ApplyModifiers(JNIEnv *jni, EventInfo &eInfo)
{
    JDWP_ASSERT(eInfo.thread != 0);

    if (JNI_FALSE == jni->IsSameObject(eInfo.thread, m_thread)) {
        return false;
    }

    jint currentCount = 0;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFrameCount(m_thread, &currentCount));
    if (err != JVMTI_ERROR_NONE) {
        return false;
    }

    jint currentLine = 0;
    if (m_size == JDWP_STEP_LINE) {
        currentLine = GetCurrentLine();
    }

    if (currentCount < m_frameCount) {
        // method exit
        m_frameCount = currentCount;
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
                int ret = GetRequestManager().AddInternalRequest(
                        jni, m_methodEntryRequest);
                if (ret != JDWP_ERROR_NONE) {
                    AgentException aex = AgentBase::GetExceptionManager().GetLastException();
                    JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error adding internal request: %s", aex.GetExceptionMessage(jni)));
                }
            }
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->NotifyFramePop(m_thread, 0));
            if (err == JVMTI_ERROR_OPAQUE_FRAME) {
                m_isNative = true;
            }
            return false;
        }
    } else { // currentCount == m_frameCount
        // check against line
        if (m_size == JDWP_STEP_LINE && currentLine == m_lineNumber) {
            return false;
        }
    }
    
    if (currentLine == -1) {
        return false;
    }

    m_frameCount = currentCount;
    m_lineNumber = currentLine;

    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "step: frame=%d, line=%d", m_frameCount, m_lineNumber));
    return AgentEventRequest::ApplyModifiers(jni, eInfo);
}
