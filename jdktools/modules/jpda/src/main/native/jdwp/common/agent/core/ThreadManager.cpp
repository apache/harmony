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
#include <algorithm>

#include "Log.h"
#include "ThreadManager.h"
#include "ClassManager.h"
#include "ObjectManager.h"
#include "EventDispatcher.h"
#include "RequestManager.h"

using namespace jdwp;

namespace jdwp {

    /**
     * The structure containing thread information about
     * internal agent threads and suspended java threads.
     */
    class ThreadInfo : public AgentBase {

    public:

        /**
         * Thread suspend count.
         */
        jint    m_suspendCount;

        /**
         * Java thread.
         */
        jthread m_thread;

        /**
         * Thread name.
         */
        char*   m_threadName;

        /**
         * Whether internal agent is working thread.
         */
        bool    m_isAgentThread;

        /**
         * Whether thread was suspended on event.
         */
        bool    m_isOnEvent;

        /**
         * A constructor.
         * Creates new instance.
         * @param jni the JNI interface pointer.
         * @param thrd java thread.
         * @param isAgentThrd indicates whether thread is internal agent's or traget VM's.
         * @param isOnEvent on event thread suspension.
         */
        ThreadInfo(JNIEnv *jni, jthread thrd, bool isAgentThrd = false, bool isOnEvent = false)
            throw (OutOfMemoryException)
        {
            m_thread = jni->NewGlobalRef(thrd);
            if (m_thread == 0) throw OutOfMemoryException();
            m_isAgentThread = isAgentThrd;
            m_isOnEvent = isOnEvent;
            m_suspendCount = 0;
            m_threadName = 0;
        }

        /**
         * Cleanups global reference to java thread.
         * @param jni the JNI interface pointer.
         */
        void Clean(JNIEnv *jni)
        {
            jni->DeleteGlobalRef(m_thread);
        }

    }; //ThreadInfo

    //-------------------------------------------------------------------------

    /**
     * Looks for corresponding thread info in the thread info list.
     * @param jni the JNI interface pointer.
     * @param thrdInfoList pointer to the thread info list.
     * @param thread the given java thread.
     * @param result found position in the thread info list.
     */
    void FindThreadInfo(JNIEnv *jni, ThreadInfoList *thrdInfoList, jthread thread,
        ThreadInfoList::iterator &result)
    {
        for (result = thrdInfoList->begin(); result != thrdInfoList->end(); result++) {
            if (*result != 0 && 
                    jni->IsSameObject((*result)->m_thread, thread) == JNI_TRUE)
                break;
        }
    }

}// namespace jdwp

//-----------------------------------------------------------------------------
//ThreadManager----------------------------------------------------------------

ThreadManager::~ThreadManager()
{
    JDWP_ASSERT(m_thrdmgrMonitor == 0);
    JDWP_ASSERT(m_execMonitor == 0);
}
        
//-----------------------------------------------------------------------------

void
ThreadManager::Init(JNIEnv *jni) throw()
{
    JDWP_TRACE_ENTRY("Init(" << jni << ')');

    JDWP_ASSERT(m_thrdmgrMonitor == 0);

    m_execMonitor = new AgentMonitor("_jdwp_ThreadManager_execMonitor");
    m_thrdmgrMonitor = new AgentMonitor("_jdwp_ThreadManager_thrdmgrMonitor");
    
    m_stepMonitor = new AgentMonitor("_jdwp_ThreadManager_stepMonitor");
    m_popFramesMonitor = new AgentMonitor("_jdwp_ThreadManager_popFramesMonitor");
    m_stepMonitorReleased = false;
    m_popFramesMonitorReleased = false;

}

//-----------------------------------------------------------------------------

void
ThreadManager::Clean(JNIEnv *jni) throw()
{
    JDWP_TRACE_ENTRY("Clean(" << jni << ')');

    if(m_execMonitor !=0) {
        delete m_execMonitor;
        m_execMonitor = 0;
    }

    if(m_thrdmgrMonitor != 0) {
        delete m_thrdmgrMonitor;
        m_thrdmgrMonitor = 0;
    }

    if (m_stepMonitor != 0) {
        delete m_stepMonitor;
        m_stepMonitor = 0;
    }

    if (m_popFramesMonitor != 0) {
        delete m_popFramesMonitor;
        m_popFramesMonitor = 0;
    }
}

//-----------------------------------------------------------------------------

void
ThreadManager::Reset(JNIEnv *jni) throw(AgentException)
{
    JDWP_TRACE_ENTRY("Reset(" << jni << ')');

    // delete not yet started but registered handlers for method invoke
    if (m_execMonitor != 0) {
        MonitorAutoLock lock(m_execMonitor JDWP_FILE_LINE);
        ClearExecList(jni);
    }

    // resume all not internal threads and remove them from list
    if (m_thrdmgrMonitor != 0) {
        MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);
        ClearThreadList(jni);
    }

    // reset flags and thread variable that are used in PopFrames process
    {
        m_stepMonitorReleased = false;
        m_popFramesMonitorReleased = false;
        m_popFramesThread = 0;
    }
}

//-----------------------------------------------------------------------------

void
ThreadManager::ClearThreadList(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY("ClearThreadList(" << jni << ')');

    for (ThreadInfoList::iterator iter = m_threadInfoList.begin(); iter != m_threadInfoList.end(); iter++) {
        if (*iter == 0) continue;
        if (!(*iter)->m_isAgentThread) {
            JDWP_TRACE_THREAD("Reset: resume thread=" << (*iter)->m_thread 
                << ", name=" << JDWP_CHECK_NULL((*iter)->m_threadName));
            // resume thread
            GetObjectManager().DeleteFrameIDs(jni, (*iter)->m_thread);
            jvmtiError err;
            JVMTI_TRACE(err, GetJvmtiEnv()->ResumeThread((*iter)->m_thread));
            // remove from list and destroy
            (*iter)->Clean(jni);
            delete *iter;
            *iter = 0;
        }
    }
    m_threadInfoList.clear();
}

//-----------------------------------------------------------------------------

void
ThreadManager::ClearExecList(JNIEnv* jni) throw()
{
    JDWP_TRACE_ENTRY("ClearExecList(" << jni << ')');

    while (!m_execList.empty()) {
        SpecialAsyncCommandHandler* handler = m_execList.back();
        m_execList.pop_back();
        delete handler;
    }
}

//-----------------------------------------------------------------------------

jthread
ThreadManager::RunAgentThread(JNIEnv *jni, jvmtiStartFunction proc,
                                const void *arg, jint priority,
                                const char *name, jthread thread) 
                                throw(AgentException)
{
    JDWP_TRACE_ENTRY("RunAgentThread(" << jni << ',' << proc 
            << ',' << arg << ',' << priority 
                        << ',' << JDWP_CHECK_NULL(name) << ')');

    if (thread == 0) {
        thread = CreateAgentThread(jni, name);
    }

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->RunAgentThread(thread, proc, arg, priority));

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    return thread;
}

//-----------------------------------------------------------------------------

jthread
ThreadManager::CreateAgentThread(JNIEnv *jni, const char *name) throw(AgentException)
{
    JDWP_TRACE_ENTRY("CreateAgentThread(" << jni 
                        << ',' << JDWP_CHECK_NULL(name) << ')');

    jthread thrd;
    ClassManager &clsMgr = GetClassManager();

    jclass klass = clsMgr.GetThreadClass();

    jmethodID methodID;
    if (name == 0)
        methodID = jni->GetMethodID(klass, "<init>", "()V");
    else
        methodID = jni->GetMethodID(klass, "<init>", "(Ljava/lang/String;)V");

    clsMgr.CheckOnException(jni);

    if (name == 0)
        thrd = jni->NewObject(klass, methodID);
    else
    {
        jstring threadName = jni->NewStringUTF(name);
        clsMgr.CheckOnException(jni);
        thrd = jni->NewObject(klass, methodID, threadName);
    }

    clsMgr.CheckOnException(jni);

    AddThread(jni, thrd, true);

    return thrd;
}

//-----------------------------------------------------------------------------

ThreadInfo* 
ThreadManager::AddThread(JNIEnv *jni, jthread thread, bool isAgentThread, bool isOnEvent)
    throw(AgentException)
{
    JDWP_TRACE_ENTRY("AddThread(" << jni << ',' << thread 
        << ',' << isAgentThread << ',' << isOnEvent << ')');

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    ThreadInfoList::iterator place = m_threadInfoList.end();
    ThreadInfoList::iterator result;
    for (result = m_threadInfoList.begin(); 
                               result != m_threadInfoList.end(); result++) {
        if (*result == 0) {
            // save pointer to empty slot
            place = result;
        } else if (jni->IsSameObject((*result)->m_thread, thread) == JNI_TRUE) {
            // thread found
            break;
        }
    }

    // not found
    if  (result == m_threadInfoList.end())
    {
        ThreadInfo *thrdinf = new ThreadInfo(jni, thread, isAgentThread, isOnEvent);

#ifndef NDEBUG
        // save thread name for debugging purpose
        if (JDWP_TRACE_ENABLED(LOG_KIND_THREAD)) {
            jvmtiError err;
            jvmtiThreadInfo info;

            JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
            if (err != JVMTI_ERROR_NONE)
                throw AgentException(err);

            thrdinf->m_threadName = info.name;
        }
#endif // NDEBUG

        JDWP_TRACE_THREAD("AddThread: add thread=" << thread 
            << ", name=" << JDWP_CHECK_NULL(thrdinf->m_threadName));

        if (place != m_threadInfoList.end()) {
            *place = thrdinf;
        } else {
            m_threadInfoList.push_back(thrdinf);
        }

        return thrdinf;
    }

    return *result;
}

//-----------------------------------------------------------------------------

void
ThreadManager::InternalSuspend(JNIEnv *jni, jthread thread, bool ignoreInternal, bool isOnEvent) 
    throw(AgentException)
{
    JDWP_TRACE_ENTRY("InternalSuspend(" << jni << ',' << thread 
        << ',' << ignoreInternal << ',' << isOnEvent << ')');

    // find thread
    ThreadInfoList::iterator place = m_threadInfoList.end();
    ThreadInfoList::iterator result;
    for (result = m_threadInfoList.begin(); 
                               result != m_threadInfoList.end(); result++) {
        if (*result == 0) {
            // save pointer to empty slot
            place = result;
        } else if (jni->IsSameObject((*result)->m_thread, thread) == JNI_TRUE) {
            // thread found
            break;
        }
    }
    
    if  (result == m_threadInfoList.end())
    {
        // not in list -> suspend and add to list
        jvmtiError err;

        JVMTI_TRACE(err, GetJvmtiEnv()->SuspendThread(thread));

        JDWP_ASSERT(err != JVMTI_ERROR_THREAD_SUSPENDED);

        if (err != JVMTI_ERROR_NONE)
            throw AgentException(err);

        // add thread
        ThreadInfo *thrdinf = new ThreadInfo(jni, thread, false, isOnEvent);
        thrdinf->m_suspendCount = 1 ;
        if (place != m_threadInfoList.end()) {
            *place = thrdinf;
        } else {
            m_threadInfoList.push_back(thrdinf);
        }

#ifndef NDEBUG
        // save thread name for debugging purpose
        if (JDWP_TRACE_ENABLED(LOG_KIND_THREAD)) {
            jvmtiError err;
            jvmtiThreadInfo info;

            JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
            if (err != JVMTI_ERROR_NONE)
                throw AgentException(err);

            thrdinf->m_threadName = info.name;
        }
#endif // NDEBUG

        JDWP_TRACE_THREAD("InternalSuspend: suspend thread=" << thread 
            << ", name=" << JDWP_CHECK_NULL(thrdinf->m_threadName)
            << ", oldCount=" << thrdinf->m_suspendCount
            << ", isOnEvent=" << thrdinf->m_isOnEvent);

    } else if ((*result)->m_isAgentThread) {
        // agent thread -> error (if not ignored)
        JDWP_TRACE_THREAD("InternalSuspend: ignore agent thread=" << thread 
            << ", name=" << JDWP_CHECK_NULL((*result)->m_threadName)
            << ", oldCount=" << (*result)->m_suspendCount
            << ", isOnEvent=" << (*result)->m_isOnEvent);
        if (!ignoreInternal) {
            throw AgentException(JVMTI_ERROR_INVALID_THREAD);
        }
    } else {
        // already in list -> just increase suspend count
        JDWP_TRACE_THREAD("InternalSuspend: increase count thread=" << thread 
            << ", name=" << JDWP_CHECK_NULL((*result)->m_threadName)
            << ", oldCount=" << (*result)->m_suspendCount
            << ", isOnEvent=" << (*result)->m_isOnEvent);
        JDWP_ASSERT((*result)->m_suspendCount > 0);
        (*result)->m_suspendCount++;
    }
}

//-----------------------------------------------------------------------------

void
ThreadManager::Suspend(JNIEnv *jni, jthread thread, bool isOnEvent) throw(AgentException)
{
    JDWP_TRACE_ENTRY("Suspend(" << jni << ',' << thread << '.' << isOnEvent << ')');

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);
    InternalSuspend(jni, thread, false, isOnEvent);
}

//-----------------------------------------------------------------------------

void
ThreadManager::InternalResume(JNIEnv *jni, jthread thread, bool ignoreInternal) throw(AgentException)
{
    JDWP_TRACE_ENTRY("InternalResume(" << jni << ',' << thread << ')');

    ThreadInfoList::iterator result;
    FindThreadInfo(jni, &m_threadInfoList, thread, result);
    
    if  (result == m_threadInfoList.end()) {
        // thread not yet suspended -> ignore
    } else if ((*result)->m_isAgentThread) {
        // agent thread -> error (if not ignored)
        JDWP_TRACE_THREAD("InternalResume: ignore agent thread=" << thread 
            << ", name=" << JDWP_CHECK_NULL((*result)->m_threadName)
            << ", oldCount=" << (*result)->m_suspendCount
            << ", isOnEvent=" << (*result)->m_isOnEvent);
        if (!ignoreInternal) {
            throw AgentException(JVMTI_ERROR_INVALID_THREAD);
        }
    } else if ((*result)->m_suspendCount == 1) {
        // count == 1 -> resume thread
        GetObjectManager().DeleteFrameIDs(jni, thread);

        jvmtiError err;
        JVMTI_TRACE(err, GetJvmtiEnv()->ResumeThread(thread));

        JDWP_ASSERT(err != JVMTI_ERROR_THREAD_NOT_SUSPENDED);

        if (err != JVMTI_ERROR_NONE)
            throw AgentException(err);

        JDWP_TRACE_THREAD("InternalResume: resume thread=" << thread 
            << ", name=" << JDWP_CHECK_NULL((*result)->m_threadName)
            << ", oldCount=" << (*result)->m_suspendCount
            << ", isOnEvent=" << (*result)->m_isOnEvent);

        // remove from list and destroy
        (*result)->Clean(jni);
        delete *result;
        *result = 0;
    } else {
        // count > 1 -> just decrease count
        JDWP_TRACE_THREAD("InternalResume: decrease count thread=" << thread 
            << ", name=" << JDWP_CHECK_NULL((*result)->m_threadName)
            << ", oldCount=" << (*result)->m_suspendCount
            << ", isOnEvent=" << (*result)->m_isOnEvent);
        (*result)->m_suspendCount--;
    }
}

//-----------------------------------------------------------------------------

void
ThreadManager::Resume(JNIEnv *jni, jthread thread) throw(AgentException)
{
    JDWP_TRACE_ENTRY("Resume(" << jni << ',' << thread << ')');

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);
    InternalResume(jni, thread, false);
}

//-----------------------------------------------------------------------------

void
ThreadManager::SuspendAll(JNIEnv *jni, jthread threadOnEvent) throw(AgentException)
{
    JDWP_TRACE_ENTRY("SuspendAll(" << jni << ',' << threadOnEvent << ')');

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    jint count;
    jthread *threads = 0;

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetAllThreads(&count, &threads));
    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);
    JvmtiAutoFree dobj(threads);

    for (jint i = 0; i < count; i++)
    {
        JDWP_ASSERT(threads[i] != 0);
        try
        {
            // suspend thread (ignoring internal)
            bool isOnEvent = (threadOnEvent != 0 && jni->IsSameObject(threadOnEvent, threads[i]));
            InternalSuspend(jni, threads[i], true, isOnEvent);
        }
        catch (const AgentException &e)
        {
            jvmtiError errTemp = (jvmtiError)e.ErrCode();

            JDWP_ASSERT(errTemp != JVMTI_ERROR_THREAD_NOT_SUSPENDED);

            if (errTemp != JVMTI_ERROR_NONE &&
                errTemp != JVMTI_ERROR_THREAD_NOT_ALIVE &&
                errTemp != JVMTI_ERROR_INVALID_THREAD)
            {
                err = errTemp;
            }
        }
    }

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);
}

//-----------------------------------------------------------------------------

void
ThreadManager::ResumeAll(JNIEnv *jni) throw(AgentException)
{
    JDWP_TRACE_ENTRY("ResumeAll(" << jni << ')');

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    // resume all not internal threads and remove from list
    for (ThreadInfoList::iterator iter = m_threadInfoList.begin(); iter != m_threadInfoList.end(); iter++) {

        if (*iter == 0) continue;

        if (!(*iter)->m_isAgentThread) {
            // check suspend count
            JDWP_ASSERT((*iter)->m_suspendCount > 0);
            if ((*iter)->m_suspendCount == 1) {

                // resume application thread
                JDWP_TRACE_THREAD("ResumeAll: resume thread=" << (*iter)->m_thread 
                    << ", name=" << JDWP_CHECK_NULL((*iter)->m_threadName)
                    << ", oldCount=" << (*iter)->m_suspendCount
                    << ", isOnEvent=" << (*iter)->m_isOnEvent);

                // destroy stack frame IDs
                GetObjectManager().DeleteFrameIDs(jni, (*iter)->m_thread);

                // resume thread
                jvmtiError err;
                JVMTI_TRACE(err, GetJvmtiEnv()->ResumeThread((*iter)->m_thread));

                JDWP_ASSERT(err != JVMTI_ERROR_THREAD_NOT_SUSPENDED);
                JDWP_ASSERT(err != JVMTI_ERROR_INVALID_TYPESTATE);
                JDWP_ASSERT(err != JVMTI_ERROR_INVALID_THREAD);
                JDWP_ASSERT(err != JVMTI_ERROR_THREAD_NOT_ALIVE);

                if (err != JVMTI_ERROR_NONE)
                    throw AgentException(err);

                // remove from list and destroy
                (*iter)->Clean(jni);
                delete *iter;
                *iter = 0;
            } else {
                // just decrease suspend count
                JDWP_TRACE_THREAD("ResumeAll: decrease count thread=" << (*iter)->m_thread 
                    << ", name=" << JDWP_CHECK_NULL((*iter)->m_threadName)
                    << ", oldCount=" << (*iter)->m_suspendCount
                    << ", isOnEvent=" << (*iter)->m_isOnEvent);
                (*iter)->m_suspendCount--;
            }
        } else {
            // ignore agent thread
            JDWP_TRACE_THREAD("ResumeAll: ignore agent thread=" << (*iter)->m_thread 
                << ", name=" << JDWP_CHECK_NULL((*iter)->m_threadName)
                << ", oldCount=" << (*iter)->m_suspendCount
                << ", isOnEvent=" << (*iter)->m_isOnEvent);
        }
    }
}

//-----------------------------------------------------------------------------

void
ThreadManager::Interrupt(JNIEnv *jni, jthread thread) throw(AgentException)
{
    JDWP_TRACE_ENTRY("Interrupt(" << jni << ',' << thread << ')');

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->InterruptThread(thread));

    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);
}

//-----------------------------------------------------------------------------

void
ThreadManager::Stop(JNIEnv *jni, jthread thread, jobject throwable)
                        throw(AgentException)
{
    JDWP_TRACE_ENTRY("Stop(" << jni << ',' << thread << ',' << throwable << ')');

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->StopThread(thread, throwable));
    
    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);
}

//-----------------------------------------------------------------------------

void
ThreadManager::Join(JNIEnv *jni, jthread thread)
{
    JDWP_TRACE_ENTRY("Join(" << jni << ',' << thread << ')');

    ClassManager &clsMgr = GetClassManager();
    jclass threadClass = clsMgr.GetThreadClass();

    jmethodID joinMethodID = jni->GetMethodID(threadClass, "join", "()V");
    clsMgr.CheckOnException(jni);
    JDWP_ASSERT(joinMethodID != NULL);

    jni->CallVoidMethod(thread, joinMethodID);
    clsMgr.CheckOnException(jni);
}

//-----------------------------------------------------------------------------

bool
ThreadManager::IsAgentThread(JNIEnv *jni, jthread thread)
{
    //JDWP_TRACE_ENTRY("IsAgentThread(" << jni << ',' << thread << ')');

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    bool ret_value = false;

    ThreadInfoList::iterator result;
    FindThreadInfo(jni, &m_threadInfoList, thread, result);
    
    // found
    if  (result != m_threadInfoList.end())
        ret_value = (*result)->m_isAgentThread;

    return ret_value;
}

//-----------------------------------------------------------------------------

jint
ThreadManager::GetSuspendCount(JNIEnv *jni, jthread thread)
{
    //JDWP_TRACE_ENTRY("GetSuspendCount(" << jni << ',' << thread << ')');

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    jint ret_value = 0;

    ThreadInfoList::iterator result;
    FindThreadInfo(jni, &m_threadInfoList, thread, result);
    
    // found
    if  (result != m_threadInfoList.end())
        ret_value = (*result)->m_suspendCount;

    return ret_value;
}

//-----------------------------------------------------------------------------

bool
ThreadManager::IsSuspendedOnEvent(JNIEnv *jni, jthread thrd) throw (AgentException)
{
    //JDWP_TRACE_ENTRY("IsSuspendedOnEvent(" << thrd << ')');

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    bool ret_value = false;

    ThreadInfoList::iterator result;
    FindThreadInfo(jni, &m_threadInfoList, thrd, result);
    
    // found
    if  (result != m_threadInfoList.end())
        ret_value = (*result)->m_isOnEvent;

    return ret_value;
}

//-----------------------------------------------------------------------------

bool
ThreadManager::IsSuspended(jthread thrd) throw (AgentException)
{
    //JDWP_TRACE_ENTRY("IsSuspended(" << thrd << ')');

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    jint thread_state;
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadState(thrd, &thread_state));

    JDWP_ASSERT(err != JVMTI_ERROR_NULL_POINTER);
    if (err != JVMTI_ERROR_NONE)
        throw AgentException(err);

    return (thread_state & JVMTI_THREAD_STATE_SUSPENDED) != 0;
}

//-----------------------------------------------------------------------------

void 
ThreadManager::RegisterInvokeHandler(JNIEnv *jni, SpecialAsyncCommandHandler* handler)
    throw (AgentException)
{
    JDWP_TRACE_ENTRY("RegisterInvokeHandler(" << jni << ',' << handler << ')');

    JDWP_ASSERT(handler->GetThread() != 0);

    // check if thread is suspended on event
    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    ThreadInfoList::iterator result;
    FindThreadInfo(jni, &m_threadInfoList, handler->GetThread(), result);

    if  (result == m_threadInfoList.end() || !(*result)->m_isOnEvent)
        throw AgentException(JDWP_ERROR_THREAD_NOT_SUSPENDED);

    JDWP_TRACE_THREAD("RegisterInvokeHandler: handler=" << handler
        << ", thread=" << (*result)->m_thread 
        << ", name=" << JDWP_CHECK_NULL((*result)->m_threadName)
        << ", options=" << handler->GetOptions());

    {
        MonitorAutoLock lock(m_execMonitor JDWP_FILE_LINE);
        m_execList.push_back(handler);
    }

    if ((handler->GetOptions() & JDWP_INVOKE_SINGLE_THREADED) == 0) {
        JDWP_TRACE_THREAD("RegisterInvokeHandler -- resume all before method invoke: thread=" 
            << handler->GetThread());
        ResumeAll(jni);
    } else {
        JDWP_TRACE_THREAD("RegisterInvokeHandler -- resume before method invoke: thread=" 
            << handler->GetThread());
        Resume(jni, handler->GetThread());
    }
}

SpecialAsyncCommandHandler*
ThreadManager::FindInvokeHandler(JNIEnv* jni, jthread thread)
    throw (AgentException)
{
    JDWP_TRACE_ENTRY("FindInvokeHandler(" << jni << ',' << thread << ')');

    MonitorAutoLock lock(m_execMonitor JDWP_FILE_LINE);
    for (ExecListIterator i = m_execList.begin(); i != m_execList.end(); i++) {
        SpecialAsyncCommandHandler* handler = *i;
        if (jni->IsSameObject(thread, handler->GetThread())) {
            m_execList.erase(i);
            return handler;
        }
    }
    return 0;
}

jboolean ThreadManager::IsPopFramesProcess(JNIEnv* jni, jthread thread)
{
    if (m_popFramesThread == 0) {
        return false;
    } else {
        return (jni->IsSameObject(thread, m_popFramesThread));
    }
}

void ThreadManager::HandleInternalSingleStep(JNIEnv* jni, jthread thread, 
    jmethodID method, jlocation location) 
    throw(AgentException)
{
    JDWP_TRACE_ENTRY("HandleInternalSingleStep(" << jni << ',' << thread << ',' 
        << method << ',' << location << ')');

    char* threadName = 0;
    char* methodName = 0;
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_THREAD)) {
        jvmtiError err;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetMethodName(method, &methodName, 0, 0));
        
        // don't invoke GetThreadInfo() because it may issue extra STEP events
        // jvmtiThreadInfo threadInfo;
        // JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(thread, &threadInfo));
        // threadName = threadInfo.name;
    }
#endif // NDEBUG
    JvmtiAutoFree threadNameAutoFree(threadName);
    JvmtiAutoFree methodNameAutoFree(methodName);

    {
        MonitorAutoLock stepMonitorLock(m_stepMonitor JDWP_FILE_LINE);
        {
            MonitorAutoLock popFramesMonitorLock(m_popFramesMonitor JDWP_FILE_LINE);
            // notify that thread is waiting on suspension point
            m_popFramesMonitorReleased = true;
            m_popFramesMonitor->NotifyAll();

            JDWP_TRACE_THREAD("HandleInternalSingleStep: thread on suspention point:"
                // << " thread=" << JDWP_CHECK_NULL(threadName) 
                << " thread=" << thread 
                << ",method=" << JDWP_CHECK_NULL(methodName) 
                << ",location=" << location);    
        }
        // wait for suspend on suspention point
        m_stepMonitorReleased = false;
        while (!m_stepMonitorReleased) {
            m_stepMonitor->Wait();
        }
        JDWP_TRACE_THREAD("HandleInternalSingleStep: thread resumed:"
            // << " thread=" << JDWP_CHECK_NULL(threadName) 
            << " thread=" << thread 
            << ",method=" << JDWP_CHECK_NULL(methodName) 
            << ",location=" << location);    
    }

    // execute registered MethodInvoke hadlers if needed
    GetEventDispatcher().ExecuteInvokeMethodHandlers(jni, thread);
}

void ThreadManager::PerformPopFrames(JNIEnv* jni, jint framesToPop, jthread thread) 
    throw(AgentException)
{
    JDWP_TRACE_ENTRY("PerformPopFrames(" << jni << ',' << framesToPop << ',' << thread << ')');

    MonitorAutoLock thrdmgrMonitorLock(m_thrdmgrMonitor JDWP_FILE_LINE);
    
    // thread should be suspended
    if (!GetThreadManager().IsSuspended(thread)) {
        throw AgentException(JDWP_ERROR_THREAD_NOT_SUSPENDED);
    }

    // The following check is disabled, because JVMTI and JDWP specifications 
    // are unclear if PopFrames should be invoked only for thread on an event.
    // Current algorithm supports PopFrames command for any suspended thread.
    /*
    if (!GetThreadManager().IsSuspendedOnEvent(jni, thread)) {
        throw AgentException(JDWP_ERROR_THREAD_NOT_SUSPENDED);
    }
    */

    jvmtiError err;
    char* threadName = 0;
#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_THREAD)) {
            jvmtiThreadInfo threadInfo;
            JVMTI_TRACE(err, GetJvmtiEnv()->GetThreadInfo(thread, &threadInfo));
            threadName = threadInfo.name;
        }
#endif // NDEBUG
    JvmtiAutoFree af(threadName);
    
    // The following check is just for sure.
    // This algorithm supposes that there are no invoke method handlers registered.
    // Otherwise, active invoke handlers could break popFrame process.
    {
        MonitorAutoLock lockExecList(m_execMonitor JDWP_FILE_LINE);
        if (!m_execList.empty()) {
            throw AgentException(JDWP_ERROR_THREAD_NOT_SUSPENDED);
        }
    }

    jint frameCount;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetFrameCount(thread, &frameCount));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
    if (frameCount <= framesToPop) {
        throw AgentException(JDWP_ERROR_INVALID_FRAMEID);
    }

    // if there is native frame, pop frame can't be performed
    CheckNativeFrameExistence(thread, framesToPop);

    MonitorAutoLock popFramesMonitorLock(m_popFramesMonitor JDWP_FILE_LINE);
    try {
        m_popFramesThread = thread;

        // enabling step request on thread where PopFrame command is performed
        JDWP_TRACE_THREAD("PerformPopFrames: enable internal step: thread=" 
            << JDWP_CHECK_NULL(threadName));
        GetRequestManager().EnableInternalStepRequest(jni, m_popFramesThread);

        // cycle where topmost frames are popped one after one
        for (int i = 0; i < framesToPop; i++) {
            // pop topmost frame, thread is already suspended on event
            JDWP_TRACE_THREAD("PerformPopFrames: pop: frame#=" << i 
                << ", thread=" << JDWP_CHECK_NULL(threadName));
            JVMTI_TRACE(err, GetJvmtiEnv()->PopFrame(m_popFramesThread));
            if (err != JVMTI_ERROR_NONE) {
                throw AgentException(err);
            }

            // resume thread
            JDWP_TRACE_THREAD("PerformPopFrames: resume: thread=" 
                << JDWP_CHECK_NULL(threadName));
            JVMTI_TRACE(err, GetJvmtiEnv()->ResumeThread(m_popFramesThread));
            JDWP_ASSERT(err != JVMTI_ERROR_THREAD_NOT_SUSPENDED);
            if (err != JVMTI_ERROR_NONE)
                throw AgentException(err);
            
            // wait for thread to achieve suspention point in InternalSingleStep handler
            JDWP_TRACE_THREAD("PerformPopFrames: wait for step: thread=" 
                << JDWP_CHECK_NULL(threadName));
            m_popFramesMonitorReleased = false;
            while (!m_popFramesMonitorReleased) {
                m_popFramesMonitor->Wait();
            }
            
            {
                // suspend thread on suspention point
                MonitorAutoLock stepMonitorLock(m_stepMonitor JDWP_FILE_LINE);
                JDWP_TRACE_THREAD("PerformPopFrames: suspend: thread=" 
                    << JDWP_CHECK_NULL(threadName));
                JVMTI_TRACE(err, GetJvmtiEnv()->SuspendThread(m_popFramesThread));
                JDWP_ASSERT(err != JVMTI_ERROR_THREAD_SUSPENDED);
                if (err != JVMTI_ERROR_NONE)
                    throw AgentException(err);

                // notify suspended thread on suspention point
                m_stepMonitorReleased = true;
                m_stepMonitor->NotifyAll();
                JDWP_TRACE_THREAD("PerformPopFrames: notify: thread=" 
                    << JDWP_CHECK_NULL(threadName));
            }
        }
        GetObjectManager().DeleteFrameIDs(jni, m_popFramesThread);
        
        JDWP_TRACE_THREAD("PerformPopFrames: disable internal step: thread=" 
            << JDWP_CHECK_NULL(threadName));
        GetRequestManager().DisableInternalStepRequest(jni, m_popFramesThread);
        
        m_popFramesThread = 0;
    } catch (AgentException& e) {
        JDWP_INFO("JDWP error: " << e.what() << " [" << e.ErrCode() << "]");
        JDWP_TRACE_THREAD("PerformPopFrames: disable internal step: thread=" 
            << JDWP_CHECK_NULL(threadName));
        GetRequestManager().DisableInternalStepRequest(jni, m_popFramesThread);
        m_popFramesThread = 0;
        throw(e);
    }
}

void ThreadManager::CheckNativeFrameExistence(jthread thread, jint framesToPop) 
    throw(AgentException)
{
    jvmtiFrameInfo* frames = static_cast<jvmtiFrameInfo*>
        (GetMemoryManager().Allocate((framesToPop+1) 
            * sizeof(jvmtiFrameInfo) JDWP_FILE_LINE));
    AgentAutoFree af(frames JDWP_FILE_LINE);
    
    jint count;
    jvmtiError err;
    // check (framesToPop+1) frames, because both the called 
    // and calling methods must be non-native
    JVMTI_TRACE(err, GetJvmtiEnv()->GetStackTrace(thread, 0, 
        (framesToPop+1), frames, &count));
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }
    JDWP_TRACE_THREAD("CheckNativeFrameExistence: FramesToCheck=" << count);
    
    jboolean isNative = false;
    for (int i = 0; i < count; i++) {
        jmethodID methodID = frames[i].method;
        
        jvmtiError err;
        char* methodName = 0;
        char* classSignature = 0;

#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_THREAD)) {
            jclass clazz = 0;
            JVMTI_TRACE(err, GetJvmtiEnv()->GetMethodName(methodID, &methodName, 0, 0));
            JVMTI_TRACE(err, GetJvmtiEnv()->GetMethodDeclaringClass(methodID, &clazz));
            JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(clazz, &classSignature, 0));
            
            JDWP_TRACE_THREAD("CheckNativeFrameExistence: method=" 
                << JDWP_CHECK_NULL(methodName) 
                << ", class=" << JDWP_CHECK_NULL(classSignature));
        }
#endif // NDEBUG

        JvmtiAutoFree methodNameAutoFree(methodName);
        JvmtiAutoFree classSignatureAutoFree(classSignature);
        JVMTI_TRACE(err, GetJvmtiEnv()->IsMethodNative(methodID, &isNative));
        if (err != JVMTI_ERROR_NONE) {
            throw AgentException(err);
        }
        if (isNative) {
            JDWP_TRACE_THREAD("CheckNativeFrameExistence: method=" 
                << JDWP_CHECK_NULL(methodName) 
                << ", class=" << JDWP_CHECK_NULL(classSignature) 
                << " is native");
            throw AgentException(JDWP_ERROR_NATIVE_METHOD);
        }
    }
}
