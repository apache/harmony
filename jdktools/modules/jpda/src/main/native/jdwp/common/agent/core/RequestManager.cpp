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
#include "RequestManager.h"
#include "ThreadManager.h"
#include "EventDispatcher.h"
#include "PacketParser.h"
#include "ClassManager.h"
#include "OptionParser.h"
#include "Log.h"
#include "AgentManager.h"
#include "ExceptionManager.h"

using namespace jdwp;

/**
 * Enables combining METHOD_EXIT events with other collocated events (METHOD_ENTRY, BREAKPOINT, SINGLE_STEP);
 * Disabled by default to preserve compatibility with RI behavior.
 */
static bool ENABLE_COMBINED_METHOD_EXIT_EVENT = false;

RequestManager::RequestManager()
    : m_requestIdCount(0)
    , m_requestMonitor(0) 
    , m_combinedEventsMonitor(0) 
{}

RequestManager::~RequestManager() 
{}

void RequestManager::Init(JNIEnv* jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Init(%p)", jni));

    m_requestMonitor = new AgentMonitor("_jdwp_RequestManager_requestMonitor");
    m_combinedEventsMonitor = new AgentMonitor("_jdwp_RequestManager_combinedEventsMonitor");
	m_exceptionMonitor = new AgentMonitor("_jdwp_RequestManager_exceptionMonitor");
    m_requestIdCount = 1;
}

void RequestManager::Clean(JNIEnv* jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Clean(%p)", jni));

    if (m_requestMonitor != 0){
        {
            MonitorAutoLock lock(m_requestMonitor JDWP_FILE_LINE);
        }
        delete m_requestMonitor;
        m_requestMonitor = 0;
    }
    m_requestIdCount = 0;

    if (m_combinedEventsMonitor != 0){
        {
            MonitorAutoLock lock(m_combinedEventsMonitor JDWP_FILE_LINE);
        }
        delete m_combinedEventsMonitor;
        m_combinedEventsMonitor = 0;
    }

	if (m_exceptionMonitor != 0){
        {
            MonitorAutoLock lock(m_exceptionMonitor JDWP_FILE_LINE);
        }
        delete m_exceptionMonitor;
        m_exceptionMonitor = 0;
    }
}

void RequestManager::Reset(JNIEnv* jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Reset(%p)", jni));

    if (m_requestMonitor != 0) {
        DeleteAllRequests(jni, JDWP_EVENT_SINGLE_STEP);
        DeleteAllRequests(jni, JDWP_EVENT_BREAKPOINT);
        DeleteAllRequests(jni, JDWP_EVENT_FRAME_POP);
        DeleteAllRequests(jni, JDWP_EVENT_EXCEPTION);
        DeleteAllRequests(jni, JDWP_EVENT_USER_DEFINED);
        DeleteAllRequests(jni, JDWP_EVENT_THREAD_START);
        DeleteAllRequests(jni, JDWP_EVENT_THREAD_END);
        DeleteAllRequests(jni, JDWP_EVENT_CLASS_PREPARE);
        DeleteAllRequests(jni, JDWP_EVENT_CLASS_UNLOAD);
        DeleteAllRequests(jni, JDWP_EVENT_CLASS_LOAD);
        DeleteAllRequests(jni, JDWP_EVENT_FIELD_ACCESS);
        DeleteAllRequests(jni, JDWP_EVENT_FIELD_MODIFICATION);
        DeleteAllRequests(jni, JDWP_EVENT_EXCEPTION_CATCH);
        DeleteAllRequests(jni, JDWP_EVENT_METHOD_ENTRY);
        DeleteAllRequests(jni, JDWP_EVENT_METHOD_EXIT);
        DeleteAllRequests(jni, JDWP_EVENT_VM_DEATH);
        // New events for Java 6
        DeleteAllRequests(jni, JDWP_EVENT_METHOD_EXIT_WITH_RETURN_VALUE);   
        DeleteAllRequests(jni, JDWP_EVENT_MONITOR_CONTENDED_ENTER); 
        DeleteAllRequests(jni, JDWP_EVENT_MONITOR_CONTENDED_ENTERED); 
        DeleteAllRequests(jni, JDWP_EVENT_MONITOR_WAIT);     
        DeleteAllRequests(jni, JDWP_EVENT_MONITOR_WAITED);
        {
            MonitorAutoLock lock(m_requestMonitor JDWP_FILE_LINE);
            m_requestIdCount = 1;
        }
    }

    if (m_combinedEventsMonitor != 0) {
        DeleteAllCombinedEventsInfo(jni);
    }
}

int RequestManager::ControlBreakpoint(JNIEnv* jni,
        AgentEventRequest* request, bool enable)
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "ControlBreakpoint(%p,%p,%s)", jni, request, (enable?"TRUE":"FALSE")));

    LocationOnlyModifier* lom = request->GetLocation();
    if (lom == 0) {
        AgentException ex(JDWP_ERROR_INTERNAL);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_INTERNAL;
    }
    jclass cls = lom->GetClass();
    jmethodID method = lom->GetMethod();
    jlocation location = lom->GetLocation();
    bool found = false;
    RequestList& rl = GetRequestList(request->GetEventKind());
    for (RequestListIterator i = rl.begin(); i.hasNext();) {
        AgentEventRequest* req = i.getNext();
        LocationOnlyModifier* m = req->GetLocation();
        if (m != 0 && method == m->GetMethod() &&
            location == m->GetLocation() &&
            JNI_TRUE == jni->IsSameObject(cls, m->GetClass()))
        {
            found = true;
            break;
        }
    }
    if (!found) {
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "ControlBreakpoint: breakpoint %s, loc=%lld", (enable ? "set" : "clear"), location));
        jvmtiError err;
        if (enable) {
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetBreakpoint(method, location));

            jint threadsCount;
            jthread* threads = 0;

            jvmtiError err;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetAllThreads(&threadsCount,
                                                          &threads));

            JvmtiAutoFree dobj(threads);
            if (err != JVMTI_ERROR_NONE){
                AgentException e(err);
                JDWP_SET_EXCEPTION(e);
                return err;
            }
            for (int i = 0; i < threadsCount; i++) {
              
                if (GetThreadManager().IsAgentThread(jni, threads[i]) ||
                    !GetThreadManager().IsSuspended(threads[i]) ||
                    !GetThreadManager().HasStepped(jni, threads[i]))
                    goto clean_up_thread_ref;

                jlocation threadLocation;
                jmethodID threadMethod;
                JVMTI_TRACE(LOG_DEBUG, err,
                            GetJvmtiEnv()->GetFrameLocation(threads[i], 0,
                                                            &threadMethod,
                                                            &threadLocation));
                if (err != JVMTI_ERROR_NONE)
                    goto clean_up_thread_ref;

                if (method != threadMethod || location != threadLocation)
                    goto clean_up_thread_ref;

                {
                    EventInfo eInfo;
                    memset(&eInfo, 0, sizeof(eInfo));
                    eInfo.kind = JDWP_EVENT_BREAKPOINT;
                    eInfo.thread = threads[i];
                    eInfo.cls = cls;
                    eInfo.method = method;
                    eInfo.location = location;
                
                    CombinedEventsInfo::CombinedEventsKind combinedKind = CombinedEventsInfo::COMBINED_EVENT_BREAKPOINT;
                    CombinedEventsInfo* combinedEvents = new CombinedEventsInfo();
                    int ret = combinedEvents->Init(jni, eInfo);
                    JDWP_CHECK_RETURN(ret);

                    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
                    CombinedEventsInfo::CombinedEventsList* events = 
                        &combinedEvents->m_combinedEventsLists[combinedKind];
                    events->ignored++;
                    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "Creating predicted event for breakpoint set at current location:  location=%lld, kind=%d, thread=%p method=%p",
                               location, combinedKind, threads[i], method));
                    GetRequestManager().AddCombinedEventsInfo(jni, combinedEvents);
                }

            clean_up_thread_ref:
                jni->DeleteLocalRef(threads[i]);
            }
            
        } else {
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->ClearBreakpoint(method, location));
        }
        if (err != JVMTI_ERROR_NONE) {
            AgentException ex(err);
            JDWP_SET_EXCEPTION(ex);
            return err;
        }

#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
            char* name = 0;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(method, &name, 0, 0));
            JvmtiAutoFree af(name);
            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "ControlBreakpoint: request: method=%s location=%lld enable=%s", name, location, (enable?"TRUE":"FALSE")));
        }
#endif // NDEBUG

    }

    return JDWP_ERROR_NONE;
}

int RequestManager::ControlWatchpoint(JNIEnv* jni,
        AgentEventRequest* request, bool enable)
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "ControlWatchpoint(%p,%p,%s)", jni, request, (enable?"TRUE":"FALSE")));

    FieldOnlyModifier *fom = request->GetField();
    if (fom == 0) {
        AgentException ex(JDWP_ERROR_INTERNAL);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_INTERNAL;
    }
    jclass cls = fom->GetClass();
    jfieldID field = fom->GetField();
    bool found = false;
    RequestList& rl = GetRequestList(request->GetEventKind());
    for (RequestListIterator i = rl.begin(); i.hasNext();) {
        AgentEventRequest* req = i.getNext();
        FieldOnlyModifier *m = req->GetField();
        if (m != 0 && field == m->GetField() &&
            JNI_TRUE == jni->IsSameObject(cls, m->GetClass()))
        {
            found = true;
            break;
        }
    }
    if (!found) {
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "ControlWatchpoint: watchpoint %s[%d] %s, field=%d",
            GetEventKindName(request->GetEventKind()),
            request->GetEventKind(), (enable ? "set" : "clear"), field));
        jvmtiError err;
        if (request->GetEventKind() == JDWP_EVENT_FIELD_ACCESS) {
            if (enable) {
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetFieldAccessWatch(cls, field));
            } else {
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->ClearFieldAccessWatch(cls, field));
            }
        } else if (request->GetEventKind() == JDWP_EVENT_FIELD_MODIFICATION) {
            if (enable) {
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetFieldModificationWatch(cls, field));
            } else {
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->ClearFieldModificationWatch(cls, field));
            }
        } else {
    	    AgentException ex(JDWP_ERROR_INTERNAL);
            JDWP_SET_EXCEPTION(ex);
            return JDWP_ERROR_INTERNAL;
        }
        if (err != JVMTI_ERROR_NONE) {
            AgentException ex(err);
            JDWP_SET_EXCEPTION(ex);
            return err;
        }
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
            char* name = 0;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFieldName(cls, field, &name, 0, 0));
            JvmtiAutoFree af(name);
            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "ControlBreakpoint: request: field=%s kind=%d enable=%s", name, request->GetEventKind(), (enable?"TRUE":"FALSE")));
        }
#endif // NDEBUG
    }

    return JDWP_ERROR_NONE;
}

jint RequestManager::ControlClassUnload(JNIEnv* jni, AgentEventRequest* request, bool enable) 
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "ControlClassUnload(%p,%p,%s)", jni, request, (enable?"TRUE":"FALSE")));

    if (GetAgentEnv()->extensionEventClassUnload != 0) {
        jvmtiError err;
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "ControlClassUnload: class unload callback [%d] %s",
            request->GetEventKind(), (enable ? "set" : "clear")));
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetExtensionEventCallback(
                GetAgentEnv()->extensionEventClassUnload->extension_event_index, 
                (enable ? reinterpret_cast<jvmtiExtensionEvent>(HandleClassUnload) : 0)));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Error calling SetExtensionEventCallback: %d", err));
            return 0;
        }
        return GetAgentEnv()->extensionEventClassUnload->extension_event_index;
    }
    return 0;
}

int RequestManager::ControlEvent(JNIEnv* jni,
        AgentEventRequest* request, bool enable)
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "ControlEvent(%p,%p,%s)", jni, request, (enable?"TRUE":"FALSE")));

    int ret;
    jvmtiEvent eventType;
    bool nullThreadForSetEventNotificationMode = false;
    switch (request->GetEventKind()) {
    case JDWP_EVENT_SINGLE_STEP:
        // manually controlled inside StepRequest
        //eventType = JVMTI_EVENT_SINGLE_STEP;
        //break;
        return JDWP_ERROR_NONE;
    case JDWP_EVENT_BREAKPOINT:
        eventType = JVMTI_EVENT_BREAKPOINT;
        ret = ControlBreakpoint(jni, request, enable);
        JDWP_CHECK_RETURN(ret);
        break;
    case JDWP_EVENT_FRAME_POP:
        eventType = JVMTI_EVENT_FRAME_POP;
        break;
    case JDWP_EVENT_EXCEPTION:
        eventType = JVMTI_EVENT_EXCEPTION;
        break;
    case JDWP_EVENT_CLASS_PREPARE:
        eventType = JVMTI_EVENT_CLASS_PREPARE;
        break;
    case JDWP_EVENT_CLASS_LOAD:
        eventType = JVMTI_EVENT_CLASS_LOAD;
        break;
    case JDWP_EVENT_CLASS_UNLOAD:
        eventType = static_cast<jvmtiEvent>(
            ControlClassUnload(jni, request, enable));
        // avoid standard event enable/disable technique
        return JDWP_ERROR_NONE;
    case JDWP_EVENT_FIELD_ACCESS:
        eventType = JVMTI_EVENT_FIELD_ACCESS;
        ret = ControlWatchpoint(jni, request, enable);
        JDWP_CHECK_RETURN(ret);
        break;
    case JDWP_EVENT_FIELD_MODIFICATION:
        eventType = JVMTI_EVENT_FIELD_MODIFICATION;
        ret = ControlWatchpoint(jni, request, enable);
        JDWP_CHECK_RETURN(ret);
        break;
    case JDWP_EVENT_EXCEPTION_CATCH:
        eventType = JVMTI_EVENT_EXCEPTION_CATCH;
        break;
    case JDWP_EVENT_METHOD_ENTRY:
        eventType = JVMTI_EVENT_METHOD_ENTRY;
        break;
    case JDWP_EVENT_METHOD_EXIT:
        eventType = JVMTI_EVENT_METHOD_EXIT;
        break;
    case JDWP_EVENT_THREAD_START:
        eventType = JVMTI_EVENT_THREAD_START;
        nullThreadForSetEventNotificationMode = true;
        break;
    case JDWP_EVENT_THREAD_END:
        eventType = JVMTI_EVENT_THREAD_END;
        nullThreadForSetEventNotificationMode = true;
        break;
    // New events for Java 6
    case JDWP_EVENT_METHOD_EXIT_WITH_RETURN_VALUE:
        eventType = JVMTI_EVENT_METHOD_EXIT;
        break;
    case JDWP_EVENT_MONITOR_CONTENDED_ENTER:
        eventType = JVMTI_EVENT_MONITOR_CONTENDED_ENTER;
        break;
    case JDWP_EVENT_MONITOR_CONTENDED_ENTERED:
        eventType = JVMTI_EVENT_MONITOR_CONTENDED_ENTERED;
        break;
    case JDWP_EVENT_MONITOR_WAIT:
        eventType = JVMTI_EVENT_MONITOR_WAIT;
        break;
    case JDWP_EVENT_MONITOR_WAITED:
        eventType = JVMTI_EVENT_MONITOR_WAITED;
        break;
    default:
        return JDWP_ERROR_NONE;
    }

    jthread thread = request->GetThread();
    RequestList& rl = GetRequestList(request->GetEventKind());

    RequestListIterator i = rl.begin();
    for (; i.hasNext();) {
        if (nullThreadForSetEventNotificationMode) {
            //
            // SetEventNotificationMode() for some events must be called with
            // jthread = 0, even if we need request only for specified thread.
            // Thus, if there is already any request for such events 
            // it is for all threads and SetEventNotificationMode() should not 
            // be called. 
            //
            return JDWP_ERROR_NONE;
        }
        AgentEventRequest* req = i.getNext();
	if ( req != NULL){
        if (JNI_TRUE == jni->IsSameObject(thread, req->GetThread())) {
            // there is similar request, so do nothing
            return JDWP_ERROR_NONE;
        }
	}
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "ControlEvent: request %s[%d] %s, thread=%p", GetEventKindName(request->GetEventKind()),
        request->GetEventKind(), (enable ? "on" : "off"), thread));
    jvmtiError err;
    if (nullThreadForSetEventNotificationMode) {
        //
        // SetEventNotificationMode() for some events must be called with
        // jthread = 0, even if we need request only for specified thread.
        // Thus, if request is for such event, SetEventNotificationMode() 
        // should be called with jthread = 0 and generated events will be
        // filtered later 
        //
        thread = 0;
    }
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetEventNotificationMode(
        (enable) ? JVMTI_ENABLE : JVMTI_DISABLE, eventType, thread));
    if (err != JVMTI_ERROR_NONE &&
        (err != JVMTI_ERROR_THREAD_NOT_ALIVE || enable))
    {
        AgentException ex(err);
        JDWP_SET_EXCEPTION(ex);
	    return err;
    }

    return JDWP_ERROR_NONE;
}

RequestList& RequestManager::GetRequestList(jdwpEventKind kind)
   
{
    switch (kind) {
    case JDWP_EVENT_SINGLE_STEP:
        return m_singleStepRequests;
    case JDWP_EVENT_BREAKPOINT:
        return m_breakpointRequests;
    case JDWP_EVENT_FRAME_POP:
        return m_framePopRequests;
    case JDWP_EVENT_EXCEPTION:
        return m_exceptionRequests;
    case JDWP_EVENT_USER_DEFINED:
        return m_userDefinedRequests;
    case JDWP_EVENT_THREAD_START:
        return m_threadStartRequests;
    case JDWP_EVENT_THREAD_END:
        return m_threadEndRequests;
    case JDWP_EVENT_CLASS_PREPARE:
        return m_classPrepareRequests;
    case JDWP_EVENT_CLASS_UNLOAD:
        return m_classUnloadRequests;
    case JDWP_EVENT_CLASS_LOAD:
        return m_classLoadRequests;
    case JDWP_EVENT_FIELD_ACCESS:
        return m_fieldAccessRequests;
    case JDWP_EVENT_FIELD_MODIFICATION:
        return m_fieldModificationRequests;
    case JDWP_EVENT_EXCEPTION_CATCH:
        return m_exceptionCatchRequests;
    case JDWP_EVENT_METHOD_ENTRY:
        return m_methodEntryRequests;
    case JDWP_EVENT_METHOD_EXIT:
        return m_methodExitRequests;
    case JDWP_EVENT_VM_DEATH:
        return m_vmDeathRequests;
	case JDWP_EVENT_VM_START:
		return m_vmStartRequests;
    // New events for Java 6
    case JDWP_EVENT_METHOD_EXIT_WITH_RETURN_VALUE:
        return m_methodExitWithReturnValueRequests;
    case JDWP_EVENT_MONITOR_CONTENDED_ENTER:
        return m_monitorContendedEnterRequests;
    case JDWP_EVENT_MONITOR_CONTENDED_ENTERED:
       return m_monitorContendedEnteredRequests;
    case JDWP_EVENT_MONITOR_WAIT:
        return  m_monitorWaitRequests;
    case JDWP_EVENT_MONITOR_WAITED:
        return m_monitorWaitedRequests;
    default:
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "Error: Invalid event type: %d", kind));
        return *(new RequestList());
    }    
}

int RequestManager::AddInternalRequest(JNIEnv* jni,
        AgentEventRequest* request)
   
{
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "AddInternalRequest: event=%s[%d], modCount=%d, policy=%d",
        GetEventKindName(request->GetEventKind()), request->GetEventKind(), request->GetModifierCount(), request->GetSuspendPolicy()));
    JDWP_ASSERT(m_requestIdCount > 0);
    RequestList& rl = GetRequestList(request->GetEventKind());
    MonitorAutoLock lock(m_requestMonitor JDWP_FILE_LINE);
    int ret = ControlEvent(jni, request, true);
    JDWP_CHECK_RETURN(ret);
    rl.push_back(request);

    return JDWP_ERROR_NONE;
}

int RequestManager::EnableInternalStepRequest(JNIEnv* jni, jthread thread)
{
    jvmtiError err;
    
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        char* threadName = 0;
        jvmtiThreadInfo threadInfo;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &threadInfo));
        threadName = threadInfo.name;
        JvmtiAutoFree af(threadName);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "EnableInternalStepRequest: thread=%s", JDWP_CHECK_NULL(threadName)));
    }
#endif // NDEBUG
    
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_SINGLE_STEP, thread));
    if (err != JVMTI_ERROR_NONE) {
        AgentException ex(err);
        JDWP_SET_EXCEPTION(ex);
        return err;
    }

    return JDWP_ERROR_NONE;
}

int RequestManager::DisableInternalStepRequest(JNIEnv* jni, jthread thread)
{
    jvmtiError err;
    
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
            char* threadName = 0;
            jvmtiThreadInfo threadInfo;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &threadInfo));
            threadName = threadInfo.name;
            JvmtiAutoFree af(threadName);
            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "DisableInternalStepRequest: thread=%s", JDWP_CHECK_NULL(threadName)));
        }
#endif // NDEBUG
    
    StepRequest* stepRequest = FindStepRequest(jni, thread);
    if (stepRequest != 0) {
        int ret = stepRequest->Restore();
        JDWP_CHECK_RETURN(ret);
    } else {
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_SINGLE_STEP, thread));
        if (err != JVMTI_ERROR_NONE) {
            AgentException ex(err);
            JDWP_SET_EXCEPTION(ex);
            return err;
        }
    }

    return JDWP_ERROR_NONE;
}

AgentMonitor* RequestManager::GetExceptionMonitor()
{
    return m_exceptionMonitor;
}

RequestID RequestManager::AddRequest(JNIEnv* jni, AgentEventRequest* request)
   
{
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "AddRequest: event=%s[%d], req=%d, modCount=%d, policy=%d",
        GetEventKindName(request->GetEventKind()), request->GetEventKind(), m_requestIdCount, request->GetModifierCount(), request->GetSuspendPolicy()));
    JDWP_ASSERT(m_requestIdCount > 0);
    RequestList& rl = GetRequestList(request->GetEventKind());
    MonitorAutoLock lock(m_requestMonitor JDWP_FILE_LINE);
    int ret = ControlEvent(jni, request, true);
    if (ret != JDWP_ERROR_NONE) {
        return 0;
    }
    int id = m_requestIdCount++;
    request->SetRequestId(id);
    rl.push_back(request);
    return id;
}

int RequestManager::DeleteRequest(JNIEnv* jni,
         jdwpEventKind kind, RequestID id)
    
{
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "DeleteRequest: event=%s[%d], req=%d", GetEventKindName(kind), kind, id));
    RequestList& rl = GetRequestList(kind);
    MonitorAutoLock lock(m_requestMonitor JDWP_FILE_LINE);

	for (RequestListIterator i = rl.begin(); i.hasNext();) {
	    AgentEventRequest* req = i.getNext();
	    if (id == req->GetRequestId()) {
    		rl.erase(i);
    		int ret = ControlEvent(jni, req, false);
            delete req;
            JDWP_CHECK_RETURN(ret);    			    
    		break;
	    }
	}

    return JDWP_ERROR_NONE;
}

int RequestManager::DeleteRequest(JNIEnv* jni, AgentEventRequest* request)
    
{
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "DeleteRequest: event=%s[%d], req=%d", GetEventKindName(request->GetEventKind()), request->GetEventKind(), request->GetRequestId()));
    RequestList& rl = GetRequestList(request->GetEventKind());
    MonitorAutoLock lock(m_requestMonitor JDWP_FILE_LINE);

	for (RequestListIterator i = rl.begin(); i.hasNext();) {
	    AgentEventRequest* req = i.getNext();
	    if (req == request) {
    		rl.erase(i);
    		int ret = ControlEvent(jni, req, false);
            delete req;
            JDWP_CHECK_RETURN(ret);    		
    		break;
	    }
	}

    return JDWP_ERROR_NONE;
}

void RequestManager::DeleteAllBreakpoints(JNIEnv* jni)
   
{
    DeleteAllRequests(jni, JDWP_EVENT_BREAKPOINT);
}

void RequestManager::DeleteAllRequests(JNIEnv* jni, jdwpEventKind eventKind)
{
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "DeleteAllRequests: event=%s[%d]", GetEventKindName(eventKind), eventKind));
    RequestList& rl = GetRequestList(eventKind);
    MonitorAutoLock lock(m_requestMonitor JDWP_FILE_LINE);
    while (!rl.empty()) {
        AgentEventRequest* req = rl.back();
        rl.pop_back();
        int ret = ControlEvent(jni, req, false);
        if (ret != JDWP_ERROR_NONE) {
            AgentException aex = GetExceptionManager().GetLastException();
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Error calling ControlEvent: %s", aex.GetExceptionMessage(jni)));
            return;
        }
        if(req != 0)
            delete req;
    }

}

const char*
RequestManager::GetEventKindName(jdwpEventKind kind) const
{
    switch (kind) {
    case JDWP_EVENT_SINGLE_STEP:
        return "SINGLE_STEP";
    case JDWP_EVENT_BREAKPOINT:
        return "BREAKPOINT";
    case JDWP_EVENT_FRAME_POP:
        return "FRAME_POP";
    case JDWP_EVENT_EXCEPTION:
        return "EXCEPTION";
    case JDWP_EVENT_USER_DEFINED:
        return "USER_DEFINED";
    case JDWP_EVENT_THREAD_START:
        return "THREAD_START";
    case JDWP_EVENT_THREAD_END:
        return "THREAD_END";
    case JDWP_EVENT_CLASS_PREPARE:
        return "CLASS_PREPARE";
    case JDWP_EVENT_CLASS_UNLOAD:
        return "CLASS_UNLOAD";
    case JDWP_EVENT_CLASS_LOAD:
        return "CLASS_LOAD";
    case JDWP_EVENT_FIELD_ACCESS:
        return "FIELD_ACCESS";
    case JDWP_EVENT_FIELD_MODIFICATION:
        return "FIELD_MODIFICATION";
    case JDWP_EVENT_EXCEPTION_CATCH:
        return "EXCEPTION_CATCH";
    case JDWP_EVENT_METHOD_ENTRY:
        return "METHOD_ENTRY";
    case JDWP_EVENT_METHOD_EXIT:
        return "METHOD_EXIT";
    case JDWP_EVENT_VM_DEATH:
        return "VM_DEATH";
    // New events for Java 6
    case JDWP_EVENT_METHOD_EXIT_WITH_RETURN_VALUE:
        return "METHOD_EXIT_WITH_RETURN_VALUE";
    case JDWP_EVENT_MONITOR_CONTENDED_ENTER:
        return "MONITOR_CONTENDED_ENTER";
    case JDWP_EVENT_MONITOR_CONTENDED_ENTERED:
        return "MONITOR_CONTENDED_ENTERED";
    case JDWP_EVENT_MONITOR_WAIT:
        return "MONITOR_WAIT";
    case JDWP_EVENT_MONITOR_WAITED:
        return "MONITOR_WAITED";
    default:
        return "UNKNOWN";
    }
}

StepRequest* RequestManager::FindStepRequest(JNIEnv* jni, jthread thread)
   
{
    RequestList& rl = GetRequestList(JDWP_EVENT_SINGLE_STEP);
    MonitorAutoLock lock(m_requestMonitor JDWP_FILE_LINE);
    for (RequestListIterator i = rl.begin(); i.hasNext();) {
        StepRequest* req = reinterpret_cast<StepRequest*> (i.getNext());
        if (JNI_TRUE == jni->IsSameObject(thread, req->GetThread())) {
            return req;
        }
    }
    return 0;
}

void RequestManager::DeleteStepRequest(JNIEnv* jni, jthread thread)
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "DeleteStepRequest(%p,%p)", jni, thread));

    RequestList& rl = GetRequestList(JDWP_EVENT_SINGLE_STEP);
    MonitorAutoLock lock(m_requestMonitor JDWP_FILE_LINE);
    for (RequestListIterator i = rl.begin(); i.hasNext();) {
        StepRequest* req = reinterpret_cast<StepRequest*> (i.getNext());
        if (JNI_TRUE == jni->IsSameObject(thread, req->GetThread())) {
            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "DeleteStepRequest: req=%d", req->GetRequestId()));
            rl.erase(i);
            delete req;
            break;
        }
    }
}

// extract filtered RequestID(s) into list
void RequestManager::GenerateEvents(JNIEnv* jni, EventInfo &eInfo,
        jint &eventCount, RequestID* &eventList, jdwpSuspendPolicy &sp)
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "GenerateEvents(%p, ...)", jni));

    RequestList& rl = GetRequestList(eInfo.kind);
    MonitorAutoLock lock(m_requestMonitor JDWP_FILE_LINE);
    eventList = reinterpret_cast<RequestID*>
        (GetMemoryManager().Allocate(sizeof(RequestID)*rl.size() JDWP_FILE_LINE));
    for (RequestListIterator i = rl.begin(); i.hasNext();) {
        AgentEventRequest* req = i.getNext();
        if (req->GetModifierCount() <= 0 || req->ApplyModifiers(jni, eInfo)) {
            if (req->GetRequestId() == 0 &&
                eInfo.kind == JDWP_EVENT_METHOD_ENTRY)
            {
                StepRequest* step = FindStepRequest(jni, eInfo.thread);
                if (step != 0) {
                    step->OnMethodEntry(jni, eInfo);
                }
            } else {
                JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "GenerateEvents: event #%d: kind=%s, req=%d%s",
                                 eventCount, GetEventKindName(eInfo.kind), req->GetRequestId(), (req->IsExpired() ? " (expired)" : "")));
                if (sp == JDWP_SUSPEND_NONE) {
                    sp = req->GetSuspendPolicy();
                } else if (sp == JDWP_SUSPEND_EVENT_THREAD &&
                           req->GetSuspendPolicy() == JDWP_SUSPEND_ALL) {
                    sp = JDWP_SUSPEND_ALL;
                }
                eventList[eventCount++] = req->GetRequestId();
            }
            if (req->IsExpired()) {
                rl.erase(i);
                int ret = ControlEvent(jni, req, false);
                delete req;
                if (ret != JDWP_ERROR_NONE) {
                    AgentException aex = GetExceptionManager().GetLastException();
                    JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Error calling ControlEvent: %s", aex.GetExceptionMessage(jni)));
                    return;
                }
                // Warning: compatible with the previous version
                i.backwards();
                continue;
            }
        }
    }
}

// --------------------- begin of combined events support ---------------------

CombinedEventsInfo::CombinedEventsInfo()
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "CombinedEventsInfo::CombinedEventsInfo()"));

    // initialize empty event lists
    for (int i = 0; i < COMBINED_EVENT_COUNT; i++) {
        m_combinedEventsLists[i].list = 0;
        m_combinedEventsLists[i].count = 0;
        m_combinedEventsLists[i].ignored = 0;
    }
}

CombinedEventsInfo::~CombinedEventsInfo() 
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "CombinedEventsInfo::~CombinedEventsInfo()"));

    // destroy event lists
    for (int i = 0; i < COMBINED_EVENT_COUNT; i++) {
        if (m_combinedEventsLists[i].list != 0) {
            GetMemoryManager().Free(m_combinedEventsLists[i].list JDWP_FILE_LINE);
        };
    }
}

int CombinedEventsInfo::Init(JNIEnv *jni, EventInfo &eInfo) 
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "CombinedEventsInfo::SetEventInfo(%p,%p)", jni, &eInfo));

    // store info about initial event
    m_eInfo = eInfo;
    // create global references to be used during grouping events
    if (m_eInfo.thread != 0) {
        m_eInfo.thread = jni->NewGlobalRef(eInfo.thread); 
        if (m_eInfo.thread == 0) {
            AgentException ex(JDWP_ERROR_OUT_OF_MEMORY);
	        JDWP_SET_EXCEPTION(ex);
            return JDWP_ERROR_OUT_OF_MEMORY;
        }
    }
    if (m_eInfo.cls != 0) {
        m_eInfo.cls = (jclass)jni->NewGlobalRef(eInfo.cls); 
        if (m_eInfo.cls == 0) {
            AgentException ex(JDWP_ERROR_OUT_OF_MEMORY);
            JDWP_SET_EXCEPTION(ex);
            return JDWP_ERROR_OUT_OF_MEMORY;
        }
    }

    return JDWP_ERROR_NONE;
}

void CombinedEventsInfo::Clean(JNIEnv *jni) 
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "CombinedEventsInfo::Clean(%p)", jni));
    if (m_eInfo.cls != 0) {
        jni->DeleteGlobalRef(m_eInfo.cls);
        m_eInfo.cls = 0;
    }
    if (m_eInfo.thread != 0) {
        jni->DeleteGlobalRef(m_eInfo.thread);
        m_eInfo.thread = 0;
    }
}

jint CombinedEventsInfo::GetEventsCount() const 
{
    jint count = 0;   
    for (int i = 0; i < COMBINED_EVENT_COUNT; i++) {
        count += m_combinedEventsLists[i].count;
    }
    return count;
}

int CombinedEventsInfo::GetIgnoredCallbacksCount() const 
{
    jint count = 0;   
    for (int i = 0; i < COMBINED_EVENT_COUNT; i++) {
        count += m_combinedEventsLists[i].ignored;
    }
    return count;
}

void CombinedEventsInfo::CountOccuredCallback(CombinedEventsKind combinedKind)
{
    if (m_combinedEventsLists[combinedKind].ignored > 0) {
        m_combinedEventsLists[combinedKind].ignored--;
    }
}

static bool isSameLocation(JNIEnv *jni, EventInfo& eInfo1, EventInfo eInfo2) {
    return (eInfo1.location == eInfo2.location) && (eInfo1.method == eInfo2.method);
}

CombinedEventsInfoList::iterator RequestManager::FindCombinedEventsInfo(JNIEnv *jni, jthread thread) 
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "FindCombinedEventsInfo(%p)", jni));
    MonitorAutoLock lock(m_combinedEventsMonitor JDWP_FILE_LINE);
    CombinedEventsInfoList::iterator p = m_combinedEventsInfoList.begin();
    for (; p.hasNext();) {
	CombinedEventsInfo* element = p.getNext();
        if (element != NULL && jni->IsSameObject((element)->m_eInfo.thread, thread)) {
            break;
        }
    }
    return p;
}

void RequestManager::AddCombinedEventsInfo(JNIEnv *jni, CombinedEventsInfo* info) 
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "AddCombinedEventsInfo(%p)", jni));
    MonitorAutoLock lock(m_combinedEventsMonitor JDWP_FILE_LINE);
    for (CombinedEventsInfoList::iterator p = m_combinedEventsInfoList.begin(); 
                                    p.hasNext();) {
	CombinedEventsInfo* element = p.getNext();
        if (element == NULL) {
            element = info;
            return;
        }
    }
    m_combinedEventsInfoList.push_back(info);
}

void RequestManager::DeleteCombinedEventsInfo(JNIEnv *jni, CombinedEventsInfoList::iterator p) 
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "DeleteCombinedEventsInfo(%p)", jni));
    MonitorAutoLock lock(m_combinedEventsMonitor JDWP_FILE_LINE);
    CombinedEventsInfo* element = p.getCurrent();
    if (element != NULL) {
	p.remove();
        (element)->Clean(jni);
        delete element;
        element = NULL;
    }
}

void RequestManager::DeleteAllCombinedEventsInfo(JNIEnv *jni) 
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "DeleteAllCombinedEventsInfo(%p)", jni));
    MonitorAutoLock lock(m_combinedEventsMonitor JDWP_FILE_LINE);
    for (CombinedEventsInfoList::iterator p = m_combinedEventsInfoList.begin(); 
                                        p.hasNext();) {
        CombinedEventsInfo* element = p.getNext();
        if (element != NULL) {
            p.remove();
            (element)->Clean(jni);
            delete element;
            element = NULL;
            return;
        }
    }
}

bool RequestManager::IsPredictedCombinedEvent(JNIEnv *jni, EventInfo& eInfo, 
        CombinedEventsInfo::CombinedEventsKind combinedKind)
   
{
            MonitorAutoLock lock(m_combinedEventsMonitor JDWP_FILE_LINE);
            CombinedEventsInfoList::iterator p = 
                    GetRequestManager().FindCombinedEventsInfo(jni, eInfo.thread);

            // check if no combined events info stored for this thread 
            //   -> not ignore this event
            if (!p.hasCurrent()) {
                JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "CheckCombinedEvent: no stored combined events for same location: kind=%d method=%p loc=%lld",
                        combinedKind, eInfo.method, eInfo.location));
                return false;
            }

            CombinedEventsInfo* element = p.getCurrent();

            // check if stored combined events info is for different location 
            //  -> delete info and not ignore this event
            if (!isSameLocation(jni, eInfo, (element)->m_eInfo)) 
            {
                JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "CheckCombinedEvent: delete old combined events for different location: kind=%d method=%p loc=%lld",
                        combinedKind, (element)->m_eInfo.method, (element)->m_eInfo.location));
                GetRequestManager().DeleteCombinedEventsInfo(jni, p);
                JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "CheckCombinedEvent: handle combined events for new location: kind=%d method=%p loc=%lld",
                        combinedKind,  eInfo.method, eInfo.location));
                return false;
            }

            // found conbined events info for this location
            //   -> ignore this event, decrease number of ignored callbacks, and delete info if necessary
            {
                // delete combined event info if no more callbacks to ignore
                if ((element)->GetIgnoredCallbacksCount() <= 0) {
                    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "CheckCombinedEvent: delete handled combined events for same location: kind=%d method=%p loc=%lld",
                        combinedKind,  eInfo.method, eInfo.location));
                    GetRequestManager().DeleteCombinedEventsInfo(jni, p);
                    if (eInfo.kind == JDWP_EVENT_BREAKPOINT) {
                        return false;
                    }
                }

                JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "CheckCombinedEvent: ignore predicted combined event for same location: kind=%d method=%p loc=%lld",
                        combinedKind, eInfo.method, eInfo.location));
                (element)->CountOccuredCallback(combinedKind);
                return true;
            } 
}

EventComposer* RequestManager::CombineEvents(JNIEnv* jni, 
        CombinedEventsInfo* combEventsInfo, jdwpSuspendPolicy sp) 
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "CombineEvents(%p,%p)", jni, combEventsInfo));

    jdwpTypeTag typeTag = GetClassManager().GetJdwpTypeTag(combEventsInfo->m_eInfo.cls);
    EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);

    int combinedEventsCount = combEventsInfo->GetEventsCount();
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "CombineEvents: events=%d METHOD_ENTRY=%d SINGLE_STEP=%d BREAKPOINT=%d METHOD_EXIT=%d ignored=%d",
            combinedEventsCount,
            combEventsInfo->m_combinedEventsLists[CombinedEventsInfo::COMBINED_EVENT_METHOD_ENTRY].count,
            combEventsInfo->m_combinedEventsLists[CombinedEventsInfo::COMBINED_EVENT_SINGLE_STEP].count,
            combEventsInfo->m_combinedEventsLists[CombinedEventsInfo::COMBINED_EVENT_BREAKPOINT].count,
            combEventsInfo->m_combinedEventsLists[CombinedEventsInfo::COMBINED_EVENT_METHOD_EXIT].count,
            combEventsInfo->GetIgnoredCallbacksCount()));
    ec->event.WriteInt(combinedEventsCount);
    
    CombinedEventsInfo::CombinedEventsKind combinedKind = CombinedEventsInfo::COMBINED_EVENT_METHOD_ENTRY;
    for (jint i = 0; i < combEventsInfo->m_combinedEventsLists[combinedKind].count; i++) {
        ec->event.WriteByte(JDWP_EVENT_METHOD_ENTRY);
        ec->event.WriteInt(combEventsInfo->m_combinedEventsLists[combinedKind].list[i]);
        ec->WriteThread(jni, combEventsInfo->m_eInfo.thread);
        ec->event.WriteLocation(jni,
            typeTag, combEventsInfo->m_eInfo.cls, combEventsInfo->m_eInfo.method, combEventsInfo->m_eInfo.location);
    }
    
    combinedKind = CombinedEventsInfo::COMBINED_EVENT_SINGLE_STEP;
    for (jint i = 0; i < combEventsInfo->m_combinedEventsLists[combinedKind].count; i++) {
        ec->event.WriteByte(JDWP_EVENT_SINGLE_STEP);
        ec->event.WriteInt(combEventsInfo->m_combinedEventsLists[combinedKind].list[i]);
        ec->WriteThread(jni, combEventsInfo->m_eInfo.thread);
        ec->event.WriteLocation(jni,
            typeTag, combEventsInfo->m_eInfo.cls, combEventsInfo->m_eInfo.method, combEventsInfo->m_eInfo.location);
    }

    combinedKind = CombinedEventsInfo::COMBINED_EVENT_BREAKPOINT;
    for (jint i = 0; i < combEventsInfo->m_combinedEventsLists[combinedKind].count; i++) {
        ec->event.WriteByte(JDWP_EVENT_BREAKPOINT);
        ec->event.WriteInt(combEventsInfo->m_combinedEventsLists[combinedKind].list[i]);
        ec->WriteThread(jni, combEventsInfo->m_eInfo.thread);
        ec->event.WriteLocation(jni,
            typeTag, combEventsInfo->m_eInfo.cls, combEventsInfo->m_eInfo.method, combEventsInfo->m_eInfo.location);
    }

    combinedKind = CombinedEventsInfo::COMBINED_EVENT_METHOD_EXIT;
    for (jint i = 0; i < combEventsInfo->m_combinedEventsLists[combinedKind].count; i++) {
        ec->event.WriteByte(JDWP_EVENT_METHOD_EXIT);
        ec->event.WriteInt(combEventsInfo->m_combinedEventsLists[combinedKind].list[i]);
        ec->WriteThread(jni, combEventsInfo->m_eInfo.thread);
        ec->event.WriteLocation(jni,
            typeTag, combEventsInfo->m_eInfo.cls, combEventsInfo->m_eInfo.method, combEventsInfo->m_eInfo.location);
    } 
    return ec;
}

bool RequestManager::IsMethodExitLocation(JNIEnv* jni, EventInfo& eInfo) 
   
{
    jvmtiError err;
    jlocation start_location;
    jlocation end_location;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodLocation(eInfo.method, &start_location, &end_location));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Error calling GetMethodLocation: %d", err));
        return false;
    }    
    bool isExit = (end_location == eInfo.location);
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "IsMethodExitLocation: isExit=%s, location=%lld, start=%lld, end=%lld",
                     (isExit?"TRUE":"FALSE"), eInfo.location, start_location, end_location));
    return isExit;
}

jdwpTag RequestManager::MethodReturnType(jvmtiEnv *env, jmethodID method)
{
    char *signature;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, env->GetMethodName(method, NULL, &signature, NULL));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "Error calling GetMethodName: %d", err));
    }
    AgentAutoFree aafSignature(signature JDWP_FILE_LINE);

     char *returnType = strchr(signature, ')') + 1;
     if (*returnType == 'V') {
         return JDWP_TAG_VOID;
     } else if (*returnType == '[') {
         return JDWP_TAG_ARRAY;
     } else if (*returnType == 'B') {
         return JDWP_TAG_BYTE;
     } else if (*returnType == 'C') {
         return JDWP_TAG_CHAR;
     }else if (*returnType == 'F') {
         return  JDWP_TAG_FLOAT;
     }else if (*returnType == 'D') {
         return JDWP_TAG_DOUBLE;
     }else if (*returnType == 'I') {
         return JDWP_TAG_INT;
     }else if (*returnType == 'J') {
         return JDWP_TAG_LONG;
     }else if (*returnType == 'S') {
         return JDWP_TAG_SHORT;
     }else if (*returnType == 'Z') {
        return JDWP_TAG_BOOLEAN;
    } else {
        return JDWP_TAG_OBJECT;
    }
}

// --------------------- end of combined events support -----------------------

//-----------------------------------------------------------------------------
// event callbacks
//-----------------------------------------------------------------------------

void JNICALL RequestManager::HandleVMInit(jvmtiEnv *jvmti, JNIEnv *jni, jthread thread)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleVMInit(%p,%p,%p)", jvmti, jni, thread));

    EventInfo eInfo;
    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_VM_INIT;

    jint eventCount = 0;
    RequestID *eventList = 0;

    jdwpSuspendPolicy sp = 
        GetOptionParser().GetSuspend() ? JDWP_SUSPEND_ALL : JDWP_SUSPEND_NONE;
    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);
    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    // post generated events
    if (eventCount > 0) {
        EventComposer *ec = 
            new EventComposer(GetEventDispatcher().NewId(),
                JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_VM_INIT);
            ec->event.WriteInt(eventList[i]);
            ec->WriteThread(jni, thread);
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "VMInit: post set of %d event", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_VM_INIT);
    } else {
        EventComposer *ec = 
            new EventComposer(GetEventDispatcher().NewId(),
                JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(1);
        ec->event.WriteByte(JDWP_EVENT_VM_INIT);
        ec->event.WriteInt(0);
        ec->WriteThread(jni, thread);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "VMInit: post single JDWP_EVENT_VM_INIT event"));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_VM_INIT);
    }
}

void JNICALL RequestManager::HandleVMDeath(jvmtiEnv* jvmti, JNIEnv* jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleVMDeath(%p,%p)", jvmti, jni));

    EventInfo eInfo;
    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_VM_DEATH;

    jint eventCount = 0;
    RequestID *eventList = 0;
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);
    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    // for VM_DEATH event use SUSPEND_POLICY_ALL for any suspension
    if (sp != JDWP_SUSPEND_NONE) {
        sp = JDWP_SUSPEND_ALL;
    }

    // post generated events
    if (eventCount > 0) {
        EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_VM_DEATH);
            ec->event.WriteInt(eventList[i]);
        }
        ec->SetAutoDeathEvent(true);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "VMDeath: post set of %d events", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_VM_DEATH);
    }
}

void JNICALL RequestManager::HandleClassPrepare(jvmtiEnv* jvmti, JNIEnv* jni,
        jthread thread, jclass cls)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleClassPrepare(%p,%p,%p,%p)", jvmti, jni, thread, cls));

    bool isAgent = GetThreadManager().IsAgentThread(jni, thread);
    jvmtiError err;
    EventInfo eInfo;
    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_CLASS_PREPARE;
    eInfo.thread = thread;
    eInfo.cls = cls;

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls,
        &eInfo.signature, 0));
    JvmtiAutoFree jafSignature(eInfo.signature);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in CLASS_PREPARE: %d", err));
        return;
    }

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "CLASS_PREPARE event: class=%s thread=%s",
            JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    jint eventCount = 0;
    RequestID *eventList = 0;
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;

    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);

    eInfo.thread = isAgent ? 0 : thread;
    sp = isAgent ? JDWP_SUSPEND_NONE : sp;

    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    // post generated events
    if (eventCount > 0) {
        jdwpTypeTag typeTag = GetClassManager().GetJdwpTypeTag(cls);
        jint status = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassStatus(cls, &status));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in CLASS_PREPARE: %d", err));
            return;
        }
        EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_CLASS_PREPARE);
            ec->event.WriteInt(eventList[i]);
            ec->WriteThread(jni, thread);
            ec->event.WriteByte((jbyte)typeTag);
            ec->event.WriteReferenceTypeID(jni, cls);
            ec->event.WriteString(eInfo.signature);
            ec->event.WriteInt(status);
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "ClassPrepare: post set of %d events", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_CLASS_PREPARE);
    }
}

//void JNICALL RequestManager::HandleClassUnload(jvmtiEnv* jvmti, ...)
void JNICALL RequestManager::HandleClassUnload(jvmtiEnv* jvmti, JNIEnv* jni,
        jthread thread, jclass cls)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleClassUnload(%p,%p,%p,%p)", jvmti, jni, thread, cls));
    
    jvmtiError err;
    EventInfo eInfo;
    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_CLASS_UNLOAD;
    eInfo.thread = thread;
    eInfo.cls = cls;

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls,
        &eInfo.signature, 0));
    JvmtiAutoFree jafSignature(eInfo.signature);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in CLASS_UNLOAD: %d", err));
        return;
    }

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "CLASS_UNLOAD event: class=%s thread=%s",
            JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    jint eventCount = 0;
    RequestID *eventList = 0;
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);
    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    bool isAgent = GetThreadManager().IsAgentThread(jni, thread);
    if (isAgent) {
        eInfo.thread = 0;
        sp = JDWP_SUSPEND_NONE;
    }

    // post generated events
    if (eventCount > 0) {
        jdwpTypeTag typeTag = GetClassManager().GetJdwpTypeTag(cls);
        jint status = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassStatus(cls, &status));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in CLASS_UNLOAD: %d", err));
            return;
        }
        EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_CLASS_UNLOAD);
            ec->event.WriteInt(eventList[i]);
            ec->WriteThread(jni, thread);
            ec->event.WriteByte((jbyte)typeTag);
            ec->event.WriteReferenceTypeID(jni, cls);
            ec->event.WriteString(eInfo.signature);
            ec->event.WriteInt(status);
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleClassUnload: post set of %d events", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_CLASS_UNLOAD);
    }
}

void JNICALL RequestManager::HandleThreadEnd(jvmtiEnv* jvmti, JNIEnv* jni,
        jthread thread)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleThreadEnd(%p,%p,%p)", jvmti, jni, thread));

    if (GetThreadManager().IsAgentThread(jni, thread)) {
        return;
    }

    GetRequestManager().DeleteStepRequest(jni, thread);
    EventInfo eInfo;
    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_THREAD_END;
    eInfo.thread = thread;

    // Remove this Java thread from our list
    GetThreadManager().RemoveJavaThread(jni, thread);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        jvmtiError err;
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "THREAD_END event: thread=%s", JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    jint eventCount = 0;
    RequestID *eventList = 0;
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);
    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    // post generated events
    if (eventCount > 0) {
        EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_THREAD_END);
            ec->event.WriteInt(eventList[i]);
            ec->WriteThread(jni, thread);
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "ThreadEnd: post set of %d events", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_THREAD_END);
    }
}

void JNICALL RequestManager::HandleThreadStart(jvmtiEnv* jvmti, JNIEnv* jni,
        jthread thread)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleThreadStart(%p,%p,%p)", jvmti, jni, thread));

    if (GetThreadManager().IsAgentThread(jni, thread)) {
        return;
    }

    EventInfo eInfo;
    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_THREAD_START;
    eInfo.thread = thread;

    // Add this new Java thread to the list in ThreadManager
    GetThreadManager().AddJavaThread(jni, thread);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        jvmtiError err;
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "THREAD_START event: thread=%s", JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    jint eventCount = 0;
    RequestID *eventList = 0;
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);
    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    // post generated events
    if (eventCount > 0) {
        EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_THREAD_START);
            ec->event.WriteInt(eventList[i]);
            ec->WriteThread(jni, thread);
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "ThreadStart: post set of %d events", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_THREAD_START);
    }
}

void JNICALL RequestManager::HandleBreakpoint(jvmtiEnv* jvmti, JNIEnv* jni,
        jthread thread, jmethodID method, jlocation location)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleBreakpoint(%p,%p,%p,%p,%lld)", jvmti, jni, thread, method, location));
    
    // if is popFrames process, ignore event
    if (GetThreadManager().IsPopFramesProcess(jni, thread)) {
        return;
    }

    // if occured in agent thread, ignore event
    if (GetThreadManager().IsAgentThread(jni, thread)) {
        return;
    }

    jvmtiError err;
    EventInfo eInfo;
    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_BREAKPOINT;
    eInfo.thread = thread;
    eInfo.method = method;
    eInfo.location = location;
    CombinedEventsInfo::CombinedEventsKind combinedKind = CombinedEventsInfo::COMBINED_EVENT_BREAKPOINT;

    // if this combined event was already prediced, ignore event
    if (GetRequestManager().IsPredictedCombinedEvent(jni, eInfo, combinedKind)) {
        return;
    }

    // We have stopped due to a breakpoint, so set hasStepped for this thread to false
    GetThreadManager().SetHasStepped(jni, thread, false);

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodDeclaringClass(method, &eInfo.cls));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in BREAKPOINT: %d", err));
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls, &eInfo.signature, 0));
    JvmtiAutoFree jafSignature(eInfo.signature);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in BREAKPOINT: %d", err));
        return;
    }

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        char* name = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(eInfo.method, &name, 0, 0));
        JvmtiAutoFree af(name);
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "BREAKPOINT event: class=%s, method=%s, location=%lld, thread=%s",
                         JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(name), eInfo.location, JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    // create new info about combined events for this location
    CombinedEventsInfo* combinedEvents = new CombinedEventsInfo();
    int ret = combinedEvents->Init(jni, eInfo);
    if (ret != JDWP_ERROR_NONE) {
        AgentException aex = AgentBase::GetExceptionManager().GetLastException();
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in BREAKPOINT: %s", aex.GetExceptionMessage(jni)));
        return;
    }
    
    // generate BREAKPOINT events according to existing requests
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
    CombinedEventsInfo::CombinedEventsList* events = 
        &combinedEvents->m_combinedEventsLists[combinedKind];
    GetRequestManager().GenerateEvents(jni, eInfo, events->count, events->list, sp);
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleBreakpoint: BREAKPOINT events: count=%d, suspendPolicy=%d, location=%lld",
            events->count, sp, combinedEvents->m_eInfo.location));

    // if no BREAKPOINT events then return from callback
    if (events->count <= 0) {
        combinedEvents->Clean(jni);
        delete combinedEvents;
        combinedEvents = 0;
        return;
    }

    // check if extra combined events should be generated later: METHOD_EXIT
    {
        // check for METHOD_EXIT events
        if (ENABLE_COMBINED_METHOD_EXIT_EVENT) {
            if (GetRequestManager().IsMethodExitLocation(jni, eInfo)) {
                combinedKind = CombinedEventsInfo::COMBINED_EVENT_METHOD_EXIT;
                events = &combinedEvents->m_combinedEventsLists[combinedKind];
                eInfo.kind = JDWP_EVENT_METHOD_EXIT;
                // generate extra events
                GetRequestManager().GenerateEvents(jni, eInfo, events->count, events->list, sp);
                JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleBreakpoint: METHOD_EXIT events: count=%d, suspendPolicy=%d, location=%lld", 
                                 events->count, sp, combinedEvents->m_eInfo.location));
                // check if corresponding callback should be ignored
                if (events->count > 0) {
                    events->ignored = 1;
                }
            }
        }
    }

    // post all generated events
    EventComposer *ec = GetRequestManager().CombineEvents(jni, combinedEvents, sp);
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleBreakpoint: post set of %d", combinedEvents->GetEventsCount()));
    GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_BREAKPOINT);

    // store info about combined events if other callbacks should be ignored
    if (combinedEvents->GetIgnoredCallbacksCount() > 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleBreakpoint: store combined events for new location: method=%p loc=%lld", eInfo.method, eInfo.location));
        GetRequestManager().AddCombinedEventsInfo(jni, combinedEvents);
    } else {
        combinedEvents->Clean(jni);
        delete combinedEvents;
        combinedEvents = 0;
    }
}

void JNICALL RequestManager::HandleException(jvmtiEnv* jvmti, JNIEnv* jni,
        jthread thread, jmethodID method, jlocation location,
        jobject exception, jmethodID catch_method, jlocation catch_location)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleException(%p,%p,%p,%p,%lld,%p,%p,%lld)", jvmti, jni, thread, method, location, exception, catch_method, catch_location));

    MonitorAutoLock lock(GetRequestManager().GetExceptionMonitor() JDWP_FILE_LINE);
    int ret;
    jvmtiError err;
    jclass exceptionClass = 0;
    AgentEventRequest* exceptionRequest = 0;

    // if agent was not initialized and this exception is expected, initialize agent
    if (!GetAgentManager().IsStarted()) {
        JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "HandleException: initial exception caught"));

        // if invocation option onuncaught=y is set, check that exception is uncaught, otherwise return
        if (GetOptionParser().GetOnuncaught() != 0) {
            if (catch_location != 0) {
                JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "HandleException: ignore caught exception"));
                return;
            }
        }

        // if invocation option onthrow=y is set, check that exception class is expected, otherwise return
        if (GetOptionParser().GetOnthrow() != 0) {

            char* expectedExceptionName = const_cast<char*>(GetOptionParser().GetOnthrow());
            if (expectedExceptionName != 0) {

                char* exceptionSignature = 0;
                exceptionClass = jni->GetObjectClass(exception);

                JVMTI_TRACE(LOG_DEBUG, err, jvmti->GetClassSignature(exceptionClass, &exceptionSignature, 0)); 
                if (err != JVMTI_ERROR_NONE) {
                    JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in EXCEPTION: %d", err));
                    return;
                }
                JvmtiAutoFree jafSignature(exceptionSignature);

                char* exceptionName = GetClassManager().GetClassName(exceptionSignature);
                if (0 == exceptionName) {
                    AgentException aex = GetExceptionManager().GetLastException();
                    err = (jvmtiError)aex.ErrCode();        
                    JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "HandleException: jvmti method Allocate() failed, error code is %d", err));
                    return;
                }
                JvmtiAutoFree jafName(exceptionName);

                JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "HandleException: exception: class=%s, signature=%s", exceptionName, exceptionSignature));

                // compare exception class taking into account similar '/' and '.' delimiters
                int i;
                for (i = 0; ; i++) {
                    if (expectedExceptionName[i] != exceptionName[i]) {
                        if ((expectedExceptionName[i] == '.' && exceptionName[i] == '/')
                                || (expectedExceptionName[i] == '/' && exceptionName[i] == '.')) {
                             continue;
                        }
                        // ignore not matched exception
                        return;
                    }
                    if (expectedExceptionName[i] == '\0') {
                        // matched exception found
                        break;
                    }
                }
            }
        }

        // disable catching initial exception and start agent
        JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "HandleException: start agent"));
        ret = GetAgentManager().DisableInitialExceptionCatch(jvmti, jni);
        if (ret != JDWP_ERROR_NONE) {
            AgentException aex = AgentBase::GetExceptionManager().GetLastException();
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in EXCEPTION: %s",  aex.GetExceptionMessage(jni)));
            return;
        }
        GetAgentManager().Start(jvmti, jni);

        // check if VM should be suspended on initial EXCEPTION event
        bool needSuspend = GetOptionParser().GetSuspend();
        if (needSuspend) {
            // add internal EXCEPTION request
            exceptionRequest = new AgentEventRequest(JDWP_EVENT_EXCEPTION, JDWP_SUSPEND_ALL);
            ret = GetRequestManager().AddInternalRequest(jni, exceptionRequest);
            if (ret != JDWP_ERROR_NONE) {
                AgentException aex = AgentBase::GetExceptionManager().GetLastException();
                JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in EXCEPTION: %s", aex.GetExceptionMessage(jni)));
                return;
            }
        } else {
            return;
        }
    }

    // must be non-agent thread
    if (GetThreadManager().IsAgentThread(jni, thread)) {
        return;
    }

    EventInfo eInfo;
    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_EXCEPTION;
    eInfo.thread = thread;
    eInfo.method = method;
    eInfo.location = location;

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodDeclaringClass(method,
        &eInfo.cls));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in EXCEPTION: %d", err));
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls,
        &eInfo.signature, 0));
    JvmtiAutoFree jafSignature(eInfo.signature);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in EXCEPTION: %d", err));
        return;
    }

    if (exceptionClass != 0) {
        eInfo.auxClass = exceptionClass;
    } else {
        eInfo.auxClass = jni->GetObjectClass(exception);
    }
    JDWP_ASSERT(eInfo.auxClass != 0);

    if (catch_method != 0) {
        eInfo.caught = true;
    }

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        char* name = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(eInfo.method, &name, 0, 0));
        JvmtiAutoFree af(name);
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "EXCEPTION event: class=%s method=%s location=%lld caught=%s, thread=%s",
                         JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(name), eInfo.location, (eInfo.caught?"TRUE":"FALSE"), JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    jint eventCount = 0;
    RequestID *eventList = 0;
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);
    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    // post generated events
    if (eventCount > 0) {
        jdwpTypeTag typeTag = GetClassManager().GetJdwpTypeTag(eInfo.cls);
        jclass catchCls = 0;
        jdwpTypeTag catchTypeTag = JDWP_TYPE_TAG_CLASS;
        if (catch_method != 0) {
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodDeclaringClass(catch_method,
                &catchCls));
            if (err != JVMTI_ERROR_NONE) {
                JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in EXCEPTION: %d", err));
                return;
            }
            catchTypeTag = GetClassManager().GetJdwpTypeTag(catchCls);
        }
        EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_EXCEPTION);
            ec->event.WriteInt(eventList[i]);
            ec->WriteThread(jni, thread);
            ec->event.WriteLocation(jni,
                typeTag, eInfo.cls, method, location);
            ec->event.WriteTaggedObjectID(jni, exception);
            ec->event.WriteLocation(jni,
                catchTypeTag, catchCls, catch_method, catch_location);
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "Exception: post set of %d events", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_EXCEPTION);
    }
    // delete internal EXCEPTION request
    if (exceptionRequest != 0) {
        GetRequestManager().DeleteRequest(jni, exceptionRequest);
    }
}

void JNICALL RequestManager::HandleMethodEntry(jvmtiEnv* jvmti, JNIEnv* jni,
        jthread thread, jmethodID method)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleMethodEntry(%p,%p,%p,%p)", jvmti, jni, thread, method));
    
    // if is popFrames process, ignore event
    if (GetThreadManager().IsPopFramesProcess(jni, thread)) {
        return;
    }

    // if occured in agent thread, ignore event
    if (GetThreadManager().IsAgentThread(jni, thread)) {
        return;
    }

    jvmtiError err;
    EventInfo eInfo;
    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_METHOD_ENTRY;
    eInfo.thread = thread;
    CombinedEventsInfo::CombinedEventsKind combinedKind = CombinedEventsInfo::COMBINED_EVENT_METHOD_ENTRY;

    // if this combined event was already prediced, ignore event
    if (GetRequestManager().IsPredictedCombinedEvent(jni, eInfo, combinedKind)) {
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodDeclaringClass(method,
        &eInfo.cls));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in METHOD_ENTRY: %d", err));
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls,
        &eInfo.signature, 0));
    JvmtiAutoFree jafSignature(eInfo.signature);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in METHOD_ENTRY: %d", err));
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFrameLocation(thread, 0,
        &eInfo.method, &eInfo.location));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in METHOD_ENTRY: %d", err));
        return;
    }
    JDWP_ASSERT(method == eInfo.method);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        char* name = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(eInfo.method, &name, 0, 0));
        JvmtiAutoFree af(name);
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "METHOD_ENTRY event: class=%p method=%s loc=%lld thread=%s",
            JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(name), eInfo.location, JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    // create new info about combined events for this location
    CombinedEventsInfo* combinedEvents = new CombinedEventsInfo();
    int ret = combinedEvents->Init(jni, eInfo);
    if (ret != JDWP_ERROR_NONE) {
        AgentException aex = AgentBase::GetExceptionManager().GetLastException();
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in METHOD_ENTRY: %s", aex.GetExceptionMessage(jni)));
        return;
    }
    
    // generate METHOD_ENTRY events according to existing requests
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
    CombinedEventsInfo::CombinedEventsList* events = 
        &combinedEvents->m_combinedEventsLists[combinedKind];
    GetRequestManager().GenerateEvents(jni, eInfo, events->count, events->list, sp);
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleMethodEntry: METHOD_ENTRY events: count=%d, suspendPolicy=%d, location=%lld",
            events->count, sp, combinedEvents->m_eInfo.location));

    // if no METHOD_ENTRY events then return from callback
    if (events->count <= 0) {
        combinedEvents->Clean(jni);
        delete combinedEvents;
        combinedEvents = 0;
        return;
    }

    // check if extra combined events should be generated: SINGLE_STEP, BREAKPOINT, NETHOD_EXIT
    {
        // check for SINGLE_STEP events
        {
            combinedKind = CombinedEventsInfo::COMBINED_EVENT_SINGLE_STEP;
            events = &combinedEvents->m_combinedEventsLists[combinedKind];
            eInfo.kind = JDWP_EVENT_SINGLE_STEP;
            // generate extra events
            GetRequestManager().GenerateEvents(jni, eInfo, events->count, events->list, sp);
            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleMethodEntry: SINGLE_STEP events: count=%d, suspendPolicy=%d, location=%lld",
                             events->count, sp, combinedEvents->m_eInfo.location));
            // check if corresponding callback should be ignored
            if (events->count > 0) {
                events->ignored = 1;
            }
        }

        // check for BREAKPOINT events
        {
            combinedKind = CombinedEventsInfo::COMBINED_EVENT_BREAKPOINT;
            events = &combinedEvents->m_combinedEventsLists[combinedKind];
            eInfo.kind = JDWP_EVENT_BREAKPOINT;
            // generate extra events
            GetRequestManager().GenerateEvents(jni, eInfo, events->count, events->list, sp);
            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleMethodEntry: BREAKPOINT events: count=%d, suspendPolicy=%d, location=%lld",
                             events->count, sp, combinedEvents->m_eInfo.location));
            // check if corresponding callback should be ignored
            if (events->count > 0) {
                events->ignored = 1;
            }
        }

        // check for METHOD_EXIT events
        if (ENABLE_COMBINED_METHOD_EXIT_EVENT) {
            if (GetRequestManager().IsMethodExitLocation(jni, eInfo)) {
                combinedKind = CombinedEventsInfo::COMBINED_EVENT_METHOD_EXIT;
                events = &combinedEvents->m_combinedEventsLists[combinedKind];
                eInfo.kind = JDWP_EVENT_METHOD_EXIT;
                // generate extra events
                GetRequestManager().GenerateEvents(jni, eInfo, events->count, events->list, sp);
                JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleMethodEntry: METHOD_EXIT events: count=%d, suspendPolicy=%d, location=%lld",
                                 events->count, sp, combinedEvents->m_eInfo.location));
                // check if corresponding callback should be ignored
                if (events->count > 0) {
                    events->ignored = 1;
                }
            }
        }
    }

    // post all generated events
    EventComposer *ec = GetRequestManager().CombineEvents(jni, combinedEvents, sp);
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleBreakpoint: post set of %d", combinedEvents->GetEventsCount()));
    GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_METHOD_ENTRY);

    // store info about combined events if other callbacks should be ignored
    if (combinedEvents->GetIgnoredCallbacksCount() > 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleMethodEntry: store combined events for new location: method=%p, loc=%lld",
                         eInfo.method, eInfo.location));
        GetRequestManager().AddCombinedEventsInfo(jni, combinedEvents);
    } else {
        combinedEvents->Clean(jni);
        delete combinedEvents;
        combinedEvents = 0;
    }
}

void JNICALL RequestManager::HandleMethodExit(jvmtiEnv* jvmti, JNIEnv* jni,
        jthread thread, jmethodID method, jboolean was_popped_by_exception,
        jvalue return_value)
{
    HandleMethodExitWithoutReturnValue(jvmti, jni, thread, method, was_popped_by_exception, return_value);
    HandleMethodExitWithReturnValue(jvmti, jni, thread, method, was_popped_by_exception, return_value);
}

void JNICALL RequestManager::HandleMethodExitWithoutReturnValue(jvmtiEnv* jvmti, JNIEnv* jni,
        jthread thread, jmethodID method, jboolean was_popped_by_exception,
        jvalue return_value)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleMethodExit(%p,%p,%p,%p,%d,%p)", jvmti, jni, thread, method, was_popped_by_exception, &return_value));

    // must be non-agent thread
    if (GetThreadManager().IsAgentThread(jni, thread)) {
        return;
    }

    // Method exit events are not generated if the method terminates with a thrown exception. 
    if (was_popped_by_exception) {
        return;
    }

    jvmtiError err;
    EventInfo eInfo;
    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_METHOD_EXIT;
    eInfo.thread = thread;
    CombinedEventsInfo::CombinedEventsKind combinedKind = CombinedEventsInfo::COMBINED_EVENT_METHOD_EXIT;

    if (ENABLE_COMBINED_METHOD_EXIT_EVENT) {
        // if this combined event was already prediced, ignore event
        if (GetRequestManager().IsPredictedCombinedEvent(jni, eInfo, combinedKind)) {
            return;
        }
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodDeclaringClass(method,
        &eInfo.cls));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in METHOD_EXIT: %d", err));
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls,
        &eInfo.signature, 0));
    JvmtiAutoFree jafSignature(eInfo.signature);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in METHOD_EXIT: %d", err));
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFrameLocation(thread, 0,
        &eInfo.method, &eInfo.location));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in METHOD_EXIT: %d", err));
        return;
    }
    JDWP_ASSERT(method == eInfo.method);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        char* name = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(eInfo.method, &name, 0, 0));
        JvmtiAutoFree af(name);
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "METHOD_EXIT event: class=%s method=%s loc=%lld thread=%s",
            JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(name), eInfo.location, JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    // there are no combined events to be generated after METHOD_EXIT event

    jint eventCount = 0;
    RequestID *eventList = 0;
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);
    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    // post generated events
    if (eventCount > 0) {
        jdwpTypeTag typeTag = GetClassManager().GetJdwpTypeTag(eInfo.cls);
        EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_METHOD_EXIT);
            ec->event.WriteInt(eventList[i]);
            ec->WriteThread(jni, thread);
            ec->event.WriteLocation(jni,
                typeTag, eInfo.cls, method, eInfo.location);
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "MethodExit: post set of %d events", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_METHOD_EXIT);
    }
}

void JNICALL RequestManager::HandleMethodExitWithReturnValue(jvmtiEnv* jvmti, JNIEnv* jni,
        jthread thread, jmethodID method, jboolean was_popped_by_exception,
        jvalue return_value)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleMethodExitWithReturnValue(%p,%p,%p,%p,%d,%p)", jvmti, jni, thread, method, was_popped_by_exception, &return_value));

    // must be non-agent thread
    if (GetThreadManager().IsAgentThread(jni, thread)) {
        return;
    }
    // Method exit events are not generated if the method terminates with a thrown exception. 
    if (was_popped_by_exception) {
        return;
    }

    jvmtiError err;
    EventInfo eInfo;
    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_METHOD_EXIT_WITH_RETURN_VALUE;
    eInfo.thread = thread;
    CombinedEventsInfo::CombinedEventsKind combinedKind = CombinedEventsInfo::COMBINED_EVENT_METHOD_EXIT;

    if (ENABLE_COMBINED_METHOD_EXIT_EVENT) {
        // if this combined event was already prediced, ignore event
        if (GetRequestManager().IsPredictedCombinedEvent(jni, eInfo, combinedKind)) {
            return;
        }
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodDeclaringClass(method,
        &eInfo.cls));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in METHOD_EXIT_WITH_RETURN_VALUE: %d", err));
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls,
        &eInfo.signature, 0));
    JvmtiAutoFree jafSignature(eInfo.signature);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in METHOD_EXIT_WITH_RETURN_VALUE: %d", err));
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFrameLocation(thread, 0,
        &eInfo.method, &eInfo.location));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in METHOD_EXIT_WITH_RETURN_VALUE: %d", err));
        return;
    }
    JDWP_ASSERT(method == eInfo.method);

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        char* name = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(eInfo.method, &name, 0, 0));
        JvmtiAutoFree af(name);
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "METHOD_EXIT_WITH_RETURN_VALUE event: class=%s method=%s loc=%lld thread=%s",
                         JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(name), eInfo.location, JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    // there are no combined events to be generated after METHOD_EXIT_WITH_RETURN_VALUE event

    jint eventCount = 0;
    RequestID *eventList = 0;
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);
    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    // post generated events
    if (eventCount > 0) {
        jdwpTypeTag typeTag = GetClassManager().GetJdwpTypeTag(eInfo.cls);
        EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_METHOD_EXIT_WITH_RETURN_VALUE);
            ec->event.WriteInt(eventList[i]);
            ec->WriteThread(jni, thread);
            ec->event.WriteLocation(jni,
                typeTag, eInfo.cls, method, eInfo.location);
            ec->event.WriteValue(jni, MethodReturnType(GetJvmtiEnv(), method), return_value);
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "MethodExitWithReturnValue : post set of %d events", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_METHOD_EXIT_WITH_RETURN_VALUE);
    }
}

void JNICALL RequestManager::HandleFieldAccess(jvmtiEnv* jvmti, JNIEnv* jni,
        jthread thread, jmethodID method, jlocation location,
        jclass field_class, jobject object, jfieldID field)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleFieldAccess(%p,%p,%p,%p,%lld,%p,%p,%p)",
                     jvmti, jni, thread, method, location, field_class, object, field));

    // must be non-agent thread
    if (GetThreadManager().IsAgentThread(jni, thread)) {
        return;
    }

    jvmtiError err;
    EventInfo eInfo;
    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_FIELD_ACCESS;
    eInfo.thread = thread;
    eInfo.method = method;
    eInfo.location = location;
    eInfo.field = field;
    eInfo.instance = object;
    eInfo.auxClass = field_class;

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodDeclaringClass(method,
        &eInfo.cls));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in FIELD_ACCESS: %d", err));
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls,
        &eInfo.signature, 0));
    JvmtiAutoFree jafSignature(eInfo.signature);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in FIELD_ACCESS: %d", err));
        return;
    }

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        char* fieldName = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(eInfo.method, &fieldName, 0, 0));
        JvmtiAutoFree affn(fieldName);
        char* methodName = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFieldName(field_class, field, &fieldName, 0, 0));
        JvmtiAutoFree afmn(methodName);
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "FIELD_ACCESS event: class=%s method=%s loc=%lld field=%s thread=%s",
            JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(methodName), eInfo.location, JDWP_CHECK_NULL(fieldName), JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    jint eventCount = 0;
    RequestID *eventList = 0;
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);
    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    // post generated events
    if (eventCount > 0) {
        jdwpTypeTag typeTag = GetClassManager().GetJdwpTypeTag(eInfo.cls);
        jdwpTypeTag fieldTypeTag =
            GetClassManager().GetJdwpTypeTag(field_class);
        EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_FIELD_ACCESS);
            ec->event.WriteInt(eventList[i]);
            ec->WriteThread(jni, thread);
            ec->event.WriteLocation(jni,
                typeTag, eInfo.cls, method, location);
            ec->event.WriteByte((jbyte)fieldTypeTag);
            ec->event.WriteReferenceTypeID(jni, field_class);
            ec->event.WriteFieldID(jni, field);
            ec->event.WriteTaggedObjectID(jni, object);
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "FieldAccess: post set of %d events", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_FIELD_ACCESS);
    }
}

void JNICALL RequestManager::HandleFieldModification(jvmtiEnv* jvmti,
        JNIEnv* jni, jthread thread, jmethodID method, jlocation location,
        jclass field_class, jobject object, jfieldID field,
        char value_sig, jvalue value)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleFieldModification(%p,%p,%p,%p,%lld,%p,%p,%p,%c,%p)", jvmti, jni, thread,
                     method, location, field_class, object, field, value_sig, &value));

    // must be non-agent thread
    if (GetThreadManager().IsAgentThread(jni, thread)) {
        return;
    }

    jvmtiError err;
    EventInfo eInfo;
    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_FIELD_MODIFICATION;
    eInfo.thread = thread;
    eInfo.method = method;
    eInfo.location = location;
    eInfo.field = field;
    eInfo.instance = object;
    eInfo.auxClass = field_class;

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodDeclaringClass(method,
        &eInfo.cls));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in FIELD_MODIFICATION: %d", err));
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls,
        &eInfo.signature, 0));
    JvmtiAutoFree jafSignature(eInfo.signature);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in FIELD_MODIFICATION: %d", err));
        return;
    }

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        char* fieldName = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(eInfo.method, &fieldName, 0, 0));
        JvmtiAutoFree affn(fieldName);
        char* methodName = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFieldName(field_class, field, &fieldName, 0, 0));
        JvmtiAutoFree afmn(methodName);
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "FIELD_MODIFICATION event: class=%s method=%s loc=%lld field=%s thread=%s",
            JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(methodName), eInfo.location, JDWP_CHECK_NULL(fieldName), JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    jint eventCount = 0;
    RequestID *eventList = 0;
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);
    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    // post generated events
    if (eventCount > 0) {
        jdwpTypeTag typeTag = GetClassManager().GetJdwpTypeTag(eInfo.cls);
        jdwpTypeTag fieldTypeTag =
            GetClassManager().GetJdwpTypeTag(field_class);
        EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_FIELD_MODIFICATION);
            ec->event.WriteInt(eventList[i]);
            ec->WriteThread(jni, thread);
            ec->event.WriteLocation(jni,
                typeTag, eInfo.cls, method, location);
            ec->event.WriteByte((jbyte)fieldTypeTag);
            ec->event.WriteReferenceTypeID(jni, field_class);
            ec->event.WriteFieldID(jni, field);
            ec->event.WriteTaggedObjectID(jni, object);
            jdwpTag valueTag = static_cast<jdwpTag>(value_sig);
            if (valueTag == JDWP_TAG_OBJECT) {
                valueTag = GetClassManager().GetJdwpTag(jni, value.l);
            }
            ec->event.WriteValue(jni, valueTag, value);
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "FieldModification: post set of %d events", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_FIELD_MODIFICATION);
    }
}

void JNICALL RequestManager::HandleSingleStep(jvmtiEnv* jvmti, JNIEnv* jni,
        jthread thread, jmethodID method, jlocation location)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleSingleStep(%p,%p,%p,%p,%lld)", jvmti, jni, thread, method, location));

    // if is popFrames process, invoke internal handler of step event
    if (GetThreadManager().IsPopFramesProcess(jni, thread)) {
        GetThreadManager().HandleInternalSingleStep(jni, thread, method, location);
        return;
    }

    // if in agent thread, ignore event
    if (GetThreadManager().IsAgentThread(jni, thread)) {
        return;
    }

    jvmtiError err;
    EventInfo eInfo;
    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_SINGLE_STEP;
    eInfo.thread = thread;
    eInfo.method = method;
    eInfo.location = location;
    CombinedEventsInfo::CombinedEventsKind combinedKind = CombinedEventsInfo::COMBINED_EVENT_SINGLE_STEP;

    // if this combined event was already prediced, ignore event
    if (GetRequestManager().IsPredictedCombinedEvent(jni, eInfo, combinedKind)) {
        return;
    }

    // We have stopped due to a single step, so set hasStepped for this thread to true
    GetThreadManager().SetHasStepped(jni, thread, true);
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodDeclaringClass(method,
        &eInfo.cls));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in SINGLE_STEP: %d", err));
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls,
        &eInfo.signature, 0));
    JvmtiAutoFree jafSignature(eInfo.signature);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in SINGLE_STEP: %d", err));
        return;
    }

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        char* name = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(eInfo.method, &name, 0, 0));
        JvmtiAutoFree af(name);
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "SINGLE_STEP event: class=%s method=%s loc=%lld thread=%s",
                         JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(name), eInfo.location, JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    // create new info about combined events for this location
    CombinedEventsInfo* combinedEvents = new CombinedEventsInfo();
    int ret = combinedEvents->Init(jni, eInfo);
    if (ret != JDWP_ERROR_NONE) {
        AgentException aex = AgentBase::GetExceptionManager().GetLastException();
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in SINGLE_STEP: %s", aex.GetExceptionMessage(jni)));
        return;
    }
    
    // generate SINGLE_STEP events according to existing requests
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
    CombinedEventsInfo::CombinedEventsList* events = 
        &combinedEvents->m_combinedEventsLists[combinedKind];
    GetRequestManager().GenerateEvents(jni, eInfo, events->count, events->list, sp);
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleSingleStep: SINGLE_STEP events: count=%d, suspendPolicy=%d, location=%lld",
            events->count, sp, combinedEvents->m_eInfo.location));

    // if no SINGLE_STEP events then return from callback
    if (events->count <= 0) {
        combinedEvents->Clean(jni);
        delete combinedEvents;
        combinedEvents = 0;
        return;
    }

    // check if extra combined events should be generated: BREAKPOINT, METHOD_EXIT
    {
        // check for BREAKPOINT events
        {
            combinedKind = CombinedEventsInfo::COMBINED_EVENT_BREAKPOINT;
            events = &combinedEvents->m_combinedEventsLists[combinedKind];
            eInfo.kind = JDWP_EVENT_BREAKPOINT;
            // generate extra events
            GetRequestManager().GenerateEvents(jni, eInfo, events->count, events->list, sp);
            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleSingleStep: BREAKPOINT events:count=%d, suspendPolicy=%d, location=%lld",
                             events->count, sp, combinedEvents->m_eInfo.location));
            // check if corresponding callback should be ignored
            if (events->count > 0) {
                events->ignored = 1;
            }
        }

        // check for METHOD_EXIT events
        if (ENABLE_COMBINED_METHOD_EXIT_EVENT) {
            if (GetRequestManager().IsMethodExitLocation(jni, eInfo)) {
                combinedKind = CombinedEventsInfo::COMBINED_EVENT_METHOD_EXIT;
                events = &combinedEvents->m_combinedEventsLists[combinedKind];
                eInfo.kind = JDWP_EVENT_METHOD_EXIT;
                // generate extra events
                GetRequestManager().GenerateEvents(jni, eInfo, events->count, events->list, sp);
                JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleSingleStep: METHOD_EXIT events:count=%d, suspendPolicy=%d, location=%lld",
                                 events->count, sp, combinedEvents->m_eInfo.location));
                // check if corresponding callback should be ignored
                if (events->count > 0) {
                    events->ignored = 1;
                }
            }
        }
    }

    // post all generated events
    EventComposer *ec = GetRequestManager().CombineEvents(jni, combinedEvents, sp);
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleSingleStep: post set of %d", combinedEvents->GetEventsCount()));
    GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_SINGLE_STEP);

    // store info about combined events if other callbacks should be ignored
    if (combinedEvents->GetIgnoredCallbacksCount() > 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "HandleSingleStep: store combined events for new location: method=%p loc=%lld", eInfo.method, eInfo.location));
        GetRequestManager().AddCombinedEventsInfo(jni, combinedEvents);
    } else {
        combinedEvents->Clean(jni);
        delete combinedEvents;
        combinedEvents = 0;
    }
}

void JNICALL RequestManager::HandleFramePop(jvmtiEnv* jvmti, JNIEnv* jni,
        jthread thread, jmethodID method, jboolean was_popped_by_exception)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleFramePop(%p,%p,%p,%p,%d)", jvmti, jni, thread, method, was_popped_by_exception));

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        jvmtiError err;
        EventInfo eInfo;
        memset(&eInfo, 0, sizeof(eInfo));
        eInfo.kind = JDWP_EVENT_METHOD_EXIT;
        eInfo.thread = thread;
    
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodDeclaringClass(method,
            &eInfo.cls));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in FRAME_POP calling GetMethodDeclaringClass: %d", err));
            return;
        }
    
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls,
            &eInfo.signature, 0));
        JvmtiAutoFree jafSignature(eInfo.signature);
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in FRAME_POP calling GetClassSignature: %d", err));
            return;
        }
    
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFrameLocation(thread, 0,
            &eInfo.method, &eInfo.location));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in FRAME_POP calling GetFrameLocation: %d", err));
            return;
        }
        JDWP_ASSERT(method == eInfo.method);

        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        char* name = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(eInfo.method, &name, 0, 0));
        JvmtiAutoFree af(name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "FRAME_POP event: class=%s method=%s loc=%lld by_exception=%d thread=%s",
            JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(name), eInfo.location, was_popped_by_exception, JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    StepRequest* step = GetRequestManager().FindStepRequest(jni, thread);
    if (step != 0) {
        int ret = step->OnFramePop(jni);
        if (ret != JDWP_ERROR_NONE) {
            AgentException aex = GetExceptionManager().GetLastException();
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in FRAME_POP: %s", aex.GetExceptionMessage(jni)));
            return;
        }
    }
}

//-----------------------------------------------------------------------------
// New event callbacks for Java 6
//-----------------------------------------------------------------------------

void JNICALL RequestManager::HandleMonitorWait(jvmtiEnv *jvmti, JNIEnv* jni, 
            jthread thread, jobject object, jlong timeout)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleMonitorWait(%p,%p,%p,%p,%lld)", jvmti, jni, thread, object, timeout));
    
    bool isAgent = GetThreadManager().IsAgentThread(jni, thread);

    jvmtiError err;
    EventInfo eInfo;

    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_MONITOR_WAIT;
    eInfo.thread = thread;
    eInfo.cls = jni->GetObjectClass(object);

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls,
        &eInfo.signature, 0));
    JvmtiAutoFree jafSignature(eInfo.signature);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in MONITOR_WAIT: %d", err));
        return;
    }

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "MONITOR_WAIT event:  monitor object class=%s thread=%s", JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFrameLocation(thread, 0,
        &eInfo.method, &eInfo.location));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in MONITOR_WAIT: %d", err));
        return;
    }

    jint eventCount = 0;
    RequestID *eventList = 0;
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;

    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);

    eInfo.thread = isAgent ? 0 : thread;
    sp = isAgent ? JDWP_SUSPEND_NONE : sp;

    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    // post generated events
    if (eventCount > 0) {
        jdwpTypeTag typeTag = GetClassManager().GetJdwpTypeTag(eInfo.cls);
        jint status = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassStatus(eInfo.cls, &status));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in MONITOR_WAIT: %d", err));
            return;
        }
        EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_MONITOR_WAIT);
            ec->event.WriteInt(eventList[i]);
            ec->WriteThread(jni, thread);
            ec->event.WriteTaggedObjectID(jni, object);
            ec->event.WriteLocation(jni,
                typeTag, eInfo.cls, eInfo.method, eInfo.location);
            ec->event.WriteLong(timeout);
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "MonitorWait: post set of %d events", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_MONITOR_WAIT);
    }
}

void JNICALL RequestManager::HandleMonitorWaited(jvmtiEnv *jvmti, JNIEnv* jni, 
            jthread thread, jobject object, jboolean timed_out)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleMonitorWaited(%p,%p,%p,%p,%d)", jvmti, jni, thread, object, timed_out));
    
    bool isAgent = GetThreadManager().IsAgentThread(jni, thread);

    jvmtiError err;
    EventInfo eInfo;

    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_MONITOR_WAITED;
    eInfo.thread = thread;
    eInfo.cls = jni->GetObjectClass(object);

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls,
        &eInfo.signature, 0));
    JvmtiAutoFree jafSignature(eInfo.signature);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in MONITOR_WAITED: %d", err));
        return;
    }

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "MONITOR_WAITED event: monitor object class=%s thread=%s", JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFrameLocation(thread, 0,
        &eInfo.method, &eInfo.location));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in MONITOR_WAITED: %d", err));
        return;
    }

    jint eventCount = 0;
    RequestID *eventList = 0;
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;

    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);

    eInfo.thread = isAgent ? 0 : thread;
    sp = isAgent ? JDWP_SUSPEND_NONE : sp;

    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    // post generated events
    if (eventCount > 0) {
        jdwpTypeTag typeTag = GetClassManager().GetJdwpTypeTag(eInfo.cls);
        jint status = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassStatus(eInfo.cls, &status));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in MONITOR_WAITED: %d", err));
            return;
        }
        EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_MONITOR_WAITED);
            ec->event.WriteInt(eventList[i]);
            ec->WriteThread(jni, thread);
            ec->event.WriteTaggedObjectID(jni, object);
            ec->event.WriteLocation(jni,
                typeTag, eInfo.cls, eInfo.method, eInfo.location);
            ec->event.WriteBoolean(timed_out);
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "MonitorWait: post set of %d events", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_MONITOR_WAITED);
    }
}

void JNICALL RequestManager::HandleMonitorContendedEnter(jvmtiEnv *jvmti, JNIEnv* jni,
            jthread thread, jobject object)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleMonitorContendedEnter(%p,%p,%p,%p)", jvmti, jni, thread, object));
    
    bool isAgent = GetThreadManager().IsAgentThread(jni, thread);
    jvmtiError err;
    EventInfo eInfo;

    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_MONITOR_CONTENDED_ENTER;
    eInfo.thread = thread;

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFrameLocation(thread, 0,
        &eInfo.method, &eInfo.location));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in MONITOR_CONTENDED_ENTER: %d", err));
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodDeclaringClass(eInfo.method,
        &eInfo.cls));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in MONITOR_CONTENDED_ENTER: %d", err));
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls,
        &eInfo.signature, 0));
    JvmtiAutoFree jafSignature(eInfo.signature);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in MONITOR_CONTENDED_ENTER: %d", err));
        return;
    }

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "MONITOR_CONTENDED_ENTER event: monitor object class=%s thread=%s", JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    jint eventCount = 0;
    RequestID *eventList = 0;
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);
    eInfo.thread = isAgent ? 0 : thread;
    sp = isAgent ? JDWP_SUSPEND_NONE : sp;

    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    // post generated events
    if (eventCount > 0) {
        jdwpTypeTag typeTag = GetClassManager().GetJdwpTypeTag(eInfo.cls);
        jint status = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassStatus(eInfo.cls, &status));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in MONITOR_CONTENDED_ENTER: %d", err));
            return;
        }
        EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_MONITOR_CONTENDED_ENTER);
            ec->event.WriteInt(eventList[i]);
            ec->WriteThread(jni, thread);
            ec->event.WriteTaggedObjectID(jni, object);
            ec->event.WriteLocation(jni,
                typeTag, eInfo.cls, eInfo.method, eInfo.location);
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "MonitorContendedEnter: post set of %d events", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_MONITOR_CONTENDED_ENTER);
    }
}

void JNICALL RequestManager::HandleMonitorContendedEntered(jvmtiEnv *jvmti, JNIEnv* jni,
            jthread thread, jobject object)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleMonitorContendedEntered(%p,%p,%p,%p)", jvmti, jni, thread, object));
    
    bool isAgent = GetThreadManager().IsAgentThread(jni, thread);
    jvmtiError err;
    EventInfo eInfo;

    memset(&eInfo, 0, sizeof(eInfo));
    eInfo.kind = JDWP_EVENT_MONITOR_CONTENDED_ENTERED;
    eInfo.thread = thread;

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFrameLocation(thread, 0,
        &eInfo.method, &eInfo.location));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in MONITOR_CONTENDED_ENTERED: %d", err));
        return;
    }

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodDeclaringClass(eInfo.method,
        &eInfo.cls));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in MONITOR_CONTENDED_ENTERED: %d", err));
        return;
    }	

    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(eInfo.cls,
        &eInfo.signature, 0));
    JvmtiAutoFree jafSignature(eInfo.signature);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in MONITOR_CONTENDED_ENTERED: %d", err));
        return;
    }

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        jvmtiThreadInfo info;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
        JvmtiAutoFree jafInfoName(info.name);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "MONITOR_CONTENDED_ENTERED event: monitor object class=%s thread=%s", JDWP_CHECK_NULL(eInfo.signature), JDWP_CHECK_NULL(info.name)));
    }
#endif // NDEBUG

    jint eventCount = 0;
    RequestID *eventList = 0;
    jdwpSuspendPolicy sp = JDWP_SUSPEND_NONE;
    GetRequestManager().GenerateEvents(jni, eInfo, eventCount, eventList, sp);
    eInfo.thread = isAgent ? 0 : thread;
    sp = isAgent ? JDWP_SUSPEND_NONE : sp;

    AgentAutoFree aafEL(eventList JDWP_FILE_LINE);

    // post generated events
    if (eventCount > 0) {
        jdwpTypeTag typeTag = GetClassManager().GetJdwpTypeTag(eInfo.cls);
        jint status = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassStatus(eInfo.cls, &status));
        if (err != JVMTI_ERROR_NONE) {
            JDWP_TRACE(LOG_RELEASE, (LOG_INFO_FL, "JDWP error in MONITOR_CONTENDED_ENTERED: %d", err));
            return;
        }
        EventComposer *ec = new EventComposer(GetEventDispatcher().NewId(),
            JDWP_COMMAND_SET_EVENT, JDWP_COMMAND_E_COMPOSITE, sp);
        ec->event.WriteInt(eventCount);
        for (jint i = 0; i < eventCount; i++) {
            ec->event.WriteByte(JDWP_EVENT_MONITOR_CONTENDED_ENTERED);
            ec->event.WriteInt(eventList[i]);
            ec->WriteThread(jni, thread);
            ec->event.WriteTaggedObjectID(jni, object);
            ec->event.WriteLocation(jni,
                typeTag, eInfo.cls, eInfo.method, eInfo.location);
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "MonitorContendedEntered: post set of %d events", eventCount));
        GetEventDispatcher().PostEventSet(jni, ec, JDWP_EVENT_MONITOR_CONTENDED_ENTERED);
    }
}
