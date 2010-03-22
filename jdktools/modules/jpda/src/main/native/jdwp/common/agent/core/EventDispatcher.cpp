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
#include "EventDispatcher.h"
#include "ThreadManager.h"
#include "OptionParser.h"
#include "PacketDispatcher.h"
#include "ExceptionManager.h"
#include "Log.h"

using namespace jdwp;

EventDispatcher::EventDispatcher(size_t limit) {
    JDWP_ASSERT(limit > 0);
    m_idCount = 0;
    m_queueMonitor = 0;
    m_waitMonitor = 0;
    m_invokeMonitor = 0;
    m_completeMonitor = 0;
    m_threadObject = 0;
    m_stopFlag = true;
    m_holdFlag = false;
    m_resetFlag = false;
    m_queueLimit = limit;
}

void EventDispatcher::Run(JNIEnv* jni) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Run(%p)", jni));

    MonitorAutoLock malCM(m_completeMonitor JDWP_FILE_LINE);
    
    while (!m_stopFlag) {
        jni->PushLocalFrame(32);
        EventComposer *ec;
    
        // get next event from queue
        {
            MonitorAutoLock lock(m_queueMonitor JDWP_FILE_LINE);

            while (m_holdFlag || m_eventQueue.empty()) {
                m_queueMonitor->Wait();
                if (m_stopFlag) break; // break event waiting loop
            }
            if (m_stopFlag) break; // break events handling loop

            ec = m_eventQueue.front();
            m_eventQueue.pop();
            m_queueMonitor->NotifyAll();
        }

        // send event and suspend thread according to suspend policy
        int ret = SuspendOnEvent(jni, ec);
        if (ret != JDWP_ERROR_NONE) {
            AgentException aex = GetExceptionManager().GetLastException();
            JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Exception in EventDispatcher thread: %s", aex.GetExceptionMessage(jni)));

            // reset current session
            JDWP_TRACE(LOG_RELEASE, (LOG_PROG_FL, "Run: reset session after exception"));
            int ret = GetPacketDispatcher().ResetAll(jni);
            if (ret != JDWP_ERROR_NONE) {
                AgentException aex = GetExceptionManager().GetLastException();
                JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Exception in PacketDispatcher::ResetAll(): %s", aex.GetExceptionMessage(jni)));
            } 
            break;
        }
        jni->PopLocalFrame(NULL);
    }

        // release completion monitor and wait forever until VM kills this thread
        // TODO: remove this workaround to prevent from resource leak
	// This is the old completion mechanism fixed in HARMONY-5019
//        m_completeMonitor->Wait(0);
}

void JNICALL
EventDispatcher::StartFunction(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "StartFunction(%p,%p,%p)", jvmti, jni, arg));

    (reinterpret_cast<EventDispatcher*>(arg))->Run(jni);
}

void EventDispatcher::Init(JNIEnv *jni) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Init(%p)", jni));

    m_queueMonitor = new AgentMonitor("_jdwp_EventDispatcher_queueMonitor");
    m_waitMonitor = new AgentMonitor("_jdwp_EventDispatcher_waitMonitor");
    m_invokeMonitor = new AgentMonitor("_jdwp_EventDispatcher_invokeMonitor");
    m_completeMonitor = new AgentMonitor("_jdwp_EventDispatcher_completeMonitor");
    m_stopFlag = false;
    m_holdFlag = true;
}

int EventDispatcher::Start(JNIEnv *jni) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Start(%p)", jni));

    jthread thread = GetThreadManager().RunAgentThread(jni, StartFunction, this,
        JVMTI_THREAD_MAX_PRIORITY, "_jdwp_EventDispatcher");
    if (thread == 0) {
        return JDWP_ERROR_INTERNAL;
    }
    m_threadObject = jni->NewGlobalRef(thread);
    return JDWP_ERROR_NONE;
}

void EventDispatcher::Reset(JNIEnv *jni) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Reset(%p)", jni));

    m_resetFlag = true;

    // dispose all remaining events in queue
    if (m_queueMonitor != 0) {
        MonitorAutoLock lock(m_queueMonitor JDWP_FILE_LINE);

        while(!m_eventQueue.empty()) {
            EventComposer *ec = m_eventQueue.front();
            m_eventQueue.pop();
            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "Reset -- delete event set: packet=%p", ec));
            ec->Reset(jni);
            delete ec;
        }

        m_holdFlag = true;
    }
    
    // release all treads waiting for suspending by event
    if (m_waitMonitor != 0) {
        MonitorAutoLock lock(m_waitMonitor JDWP_FILE_LINE);
        m_waitMonitor->NotifyAll();
    }

    // release all treads waiting for invoke method
    if (m_invokeMonitor != 0) {
        MonitorAutoLock lock(m_invokeMonitor JDWP_FILE_LINE);
        m_invokeMonitor->NotifyAll();
    }
}

void EventDispatcher::Stop(JNIEnv *jni) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Stop(%p)", jni));

    // let thread loop to finish
    {
        MonitorAutoLock lock(m_queueMonitor JDWP_FILE_LINE);
        m_stopFlag = true;
        m_holdFlag = false;
        m_queueMonitor->NotifyAll();
    }

    // wait for loop finished
    {
        MonitorAutoLock lock(m_completeMonitor JDWP_FILE_LINE);
    } 

    // wait for thread finished
    GetThreadManager().Join(jni, m_threadObject);
    jni->DeleteGlobalRef(m_threadObject);
    m_threadObject = NULL;
}

void EventDispatcher::Clean(JNIEnv *jni) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Clean(%p)", jni));
    
    // The following code is just a workaround for known problem
    // in reset and clean-up procedure to release threads which
    // may still wait on monitors.
    // TODO: improve reset and clean-up procedure
    {
        // release all treads waiting for suspending by event
        if (m_waitMonitor != 0) {
            MonitorAutoLock lock(m_waitMonitor JDWP_FILE_LINE);
            m_waitMonitor->NotifyAll();
        }
        
        // release all treads waiting for invoke method
        if (m_invokeMonitor != 0) {
            MonitorAutoLock lock(m_invokeMonitor JDWP_FILE_LINE);
            m_invokeMonitor->NotifyAll();
        }
    }

    // delete monitors

    /* Commented out as a workaround for shutdown crash - TODO apply correct behaviour to free these locks */
    /*if (m_queueMonitor != 0) {
        delete m_queueMonitor;
        m_queueMonitor = 0;
    }
    if (m_waitMonitor != 0){
        delete m_waitMonitor;
        m_waitMonitor = 0;
    }
    if (m_invokeMonitor != 0){
        delete m_invokeMonitor;
        m_invokeMonitor = 0;
    }*/

    // do not delete m_completeMonitor because thread is waiting on it
    // TODO: remove this workaround to prevent from resource leak
    // This is the old completion mechanism fixed in HARMONY-5019
    if (m_completeMonitor != 0){
        delete m_completeMonitor;
        m_completeMonitor = 0;
    }

    // clean counter for packet id

    m_idCount = 0;
}

void EventDispatcher::HoldEvents() {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HoldEvents()"));

    MonitorAutoLock lock(m_queueMonitor JDWP_FILE_LINE);
    m_holdFlag = true;
}

void EventDispatcher::ReleaseEvents() {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "ReleaseEvents()"));

    MonitorAutoLock lock(m_queueMonitor JDWP_FILE_LINE);
    m_holdFlag = false;
    m_queueMonitor->NotifyAll();
}

void EventDispatcher::NewSession() {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "NewSession()"));

    m_resetFlag = false;
    m_holdFlag = true;
}

int EventDispatcher::PostInvokeSuspend(JNIEnv *jni, SpecialAsyncCommandHandler* handler) 
    
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "PostInvokeSuspend(%p,%p)", jni, handler));

    MonitorAutoLock lock(m_invokeMonitor JDWP_FILE_LINE);
    jthread thread = handler->GetThread();

    int ret;
    char* threadName = 0;
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
            jvmtiError err;
            jvmtiThreadInfo threadInfo;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &threadInfo));
            threadName = threadInfo.name;
        }
#endif // NDEBUG
    JvmtiAutoFree af(threadName);

    // wait for thread to complete method invocation and ready for suspension
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "PostInvokeSuspend -- wait for method invoked: thread=%p, name=%s", thread, JDWP_CHECK_NULL(threadName)));
    while (!handler->IsInvoked()) {
        m_invokeMonitor->Wait();
        if (m_resetFlag) {
            return JDWP_ERROR_NONE;
        }
    }

    // suspend single thread or all threads accodring to invocation options
    if ((handler->GetOptions() & JDWP_INVOKE_SINGLE_THREADED) == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "PostInvokeSuspend -- suspend all after method invoke: thread=%p, name=%s", thread, JDWP_CHECK_NULL(threadName)));
        ret = GetThreadManager().SuspendAll(jni, handler->GetThread());
        JDWP_CHECK_RETURN(ret);
    } else {
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "PostInvokeSuspend -- suspend after method invoke: thread=%p, name=%s", thread, JDWP_CHECK_NULL(threadName)));
        ret = GetThreadManager().Suspend(jni, handler->GetThread(), true);
        JDWP_CHECK_RETURN(ret);
    }

    // release thread after suspension
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "SuspendOnEvent -- release after method invoke: thread=%p, name=%s", thread, JDWP_CHECK_NULL(threadName)));
    handler->SetReleased(true);
    m_invokeMonitor->NotifyAll();

    return JDWP_ERROR_NONE;
}

int EventDispatcher::SuspendOnEvent(JNIEnv* jni, EventComposer *ec)
   
{
    JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "SuspendOnEvent -- send event set: id=%d, policy=%d", ec->event.GetId(), ec->GetSuspendPolicy()));
    int ret = 0;
    if (ec->GetSuspendPolicy() == JDWP_SUSPEND_NONE && !ec->IsAutoDeathEvent()) {
        // thread is not waiting for suspension
        ret = ec->WriteEvent(jni);
        JDWP_CHECK_RETURN(ret);
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "SuspendOnEvent -- delete event set: packet=%p", ec));
        ec->Reset(jni);
        delete ec;
    } else {
        MonitorAutoLock lock(m_waitMonitor JDWP_FILE_LINE);
        jthread thread = ec->GetThread();

        char* threadName = 0;
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
            jvmtiError err;
            jvmtiThreadInfo threadInfo;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &threadInfo));
            threadName = threadInfo.name;
        }
#endif // NDEBUG
        JvmtiAutoFree af(threadName);

        // wait for thread to reach suspension point
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "SuspendOnEvent -- wait for thread on event: thread=%p, name=%s", thread, JDWP_CHECK_NULL(threadName)));

        while (!ec->IsWaiting()) {
            m_waitMonitor->Wait();
            if (m_resetFlag) {
                return JDWP_ERROR_NONE;
            }
        }

        // suspend corresponding threads if necessary
        if (ec->GetSuspendPolicy() == JDWP_SUSPEND_ALL) {
            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "SuspendOnEvent -- suspend all threads on event: thread=%p, name=%s", thread, JDWP_CHECK_NULL(threadName)));
            ret = GetThreadManager().SuspendAll(jni, thread);
            JDWP_CHECK_RETURN(ret);
        } else if (ec->GetSuspendPolicy() == JDWP_SUSPEND_EVENT_THREAD) {
            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "SuspendOnEvent -- suspend thread on event: thread=%p, name=%s", thread, JDWP_CHECK_NULL(threadName)));
            JDWP_ASSERT(thread != 0);
            ret = GetThreadManager().Suspend(jni, thread, true);
            JDWP_CHECK_RETURN(ret);
        }

        // send event packet
        ret = ec->WriteEvent(jni);
        JDWP_CHECK_RETURN(ret);

        // release thread on suspension point
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "SuspendOnEvent -- release thread on event: thread=%p, name=%s", thread, JDWP_CHECK_NULL(threadName)));
        ec->SetReleased(true);
        m_waitMonitor->NotifyAll();
    }

    return JDWP_ERROR_NONE;
}

void EventDispatcher::PostEventSet(JNIEnv *jni, EventComposer *ec, jdwpEventKind eventKind)
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "PostEventSet(%p,%p,%d)", jni, ec, eventKind));

    if (m_stopFlag) {
        return;
    }

    jdwpSuspendPolicy suspendPolicy = ec->GetSuspendPolicy();
    bool isAutoDeathEvent = ec->IsAutoDeathEvent();

    // put event packet into queue
    {
        MonitorAutoLock lock(m_queueMonitor JDWP_FILE_LINE);
        while (m_eventQueue.size() > (jint)m_queueLimit) {
            m_queueMonitor->Wait();
            if (m_resetFlag) {
                JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "PostEventSet -- delete event set: packet=%p, evenKind=%d", ec, eventKind));
                ec->Reset(jni);
                delete ec;
                return;
            }
        }
        m_eventQueue.push(ec);
        m_queueMonitor->NotifyAll();
    }

    // if thread should be suspended
    if (suspendPolicy != JDWP_SUSPEND_NONE || isAutoDeathEvent) {

        jthread thread = ec->GetThread();

        char* threadName = 0;
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
            jvmtiError err;
            jvmtiThreadInfo threadInfo;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &threadInfo));
            threadName = threadInfo.name;
        }
#endif // NDEBUG
        JvmtiAutoFree af(threadName);
        
        // wait on suspension point
        {
            MonitorAutoLock lock(m_waitMonitor JDWP_FILE_LINE);
            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "PostEventSet -- wait for release on event: thread=%p, name=%s, eventKind=%d", thread, JDWP_CHECK_NULL(threadName), eventKind));

            // notify that thread is waiting on suspension point
            ec->SetWaiting(true);
            m_waitMonitor->NotifyAll();

            // wait for thread to be released after suspension
            while (!ec->IsReleased()) {
                m_waitMonitor->Wait();
                if (m_resetFlag || m_stopFlag) {
                    return;
                }
            }

            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "PostEventSet -- released on event: thread=%p, name=%s, eventKind=%d", thread, JDWP_CHECK_NULL(threadName), eventKind));
        }
       
	if (GetThreadManager().IsSuspended(thread)) {
	    jvmtiError error;
	    JVMTI_TRACE(LOG_DEBUG, error, GetJvmtiEnv()->ResumeThread(thread));
	    if (error == JVMTI_ERROR_NONE) {
		JVMTI_TRACE(LOG_DEBUG, error, GetJvmtiEnv()->SuspendThread(thread));
	    }
            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "PostEventSet -- Running suspended thread: thread=%p, name=%s, eventKind=%d", thread, threadName, eventKind));
	}

        // execute all registered InvokeMethod handlers sequentially
        if (thread != 0 && suspendPolicy != JDWP_SUSPEND_NONE) {
            ExecuteInvokeMethodHandlers(jni, thread);
        }
        
        // delete event packet
        JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "PostEventSet -- delete event set: packet=%p", ec));
        ec->Reset(jni);
        delete ec;
    }
}

void EventDispatcher::ExecuteInvokeMethodHandlers(JNIEnv *jni, jthread thread)
{
    // if reset process, don't invoke handlers
    if (m_resetFlag) {
        return;
    }
    
    char* threadName = 0;
#ifndef NDEBUG
    jvmtiError err;
    if (JDWP_TRACE_ENABLED(LOG_KIND_EVENT)) {
        jvmtiThreadInfo threadInfo;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &threadInfo));
        threadName = threadInfo.name;
    }
#endif // NDEBUG

    JvmtiAutoFree af(threadName);
    
    // if PopFrame process, don't invoke handlers
    if (!GetThreadManager().IsPopFramesProcess(jni, thread)) {
        SpecialAsyncCommandHandler* handler;
        while ((handler =
            GetThreadManager().FindInvokeHandler(jni, thread)) != 0)
        {
            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "ExecuteInvokeMethodHandlers -- invoke method: thread=%p, threadName=%s, handler=%p", thread, JDWP_CHECK_NULL(threadName), handler));
            handler->ExecuteDeferredInvoke(jni);

            MonitorAutoLock invokeMonitorLock(m_invokeMonitor JDWP_FILE_LINE);

            // notify that thread is waiting on suspension point after method invocation
            handler->SetInvoked(true);
            m_invokeMonitor->NotifyAll();

            // wait on suspension point after method invocation
            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "ExecuteInvokeMethodHandlers -- wait for released on event: thread=%p, threadName=%s, handler=%p", thread, JDWP_CHECK_NULL(threadName), handler));
            while (!handler->IsReleased()) {
                m_invokeMonitor->Wait();
                if (m_resetFlag) {
                    return;
                }
            }

	    if (GetThreadManager().IsSuspended(thread)) {
		jvmtiError error;
		JVMTI_TRACE(LOG_DEBUG, error, GetJvmtiEnv()->ResumeThread(thread));
		if (error == JVMTI_ERROR_NONE) {
		    JVMTI_TRACE(LOG_DEBUG, error, GetJvmtiEnv()->SuspendThread(thread));
		}
                JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "ExecuteInvokeMethodHandlers -- Running suspended thread: thread=%p, name=%s", thread, threadName));
	    }

            JDWP_TRACE(LOG_RELEASE, (LOG_EVENT_FL, "ExecuteInvokeMethodHandlers -- released on event: thread=%p, threadName=%s, handler=%p", thread, JDWP_CHECK_NULL(threadName), handler));
        }
    }
}
