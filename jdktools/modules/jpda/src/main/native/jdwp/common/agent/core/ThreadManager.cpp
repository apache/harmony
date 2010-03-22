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
#include "ThreadManager.h"
#include "Log.h"
#include "ClassManager.h"
#include "ObjectManager.h"
#include "EventDispatcher.h"
#include "RequestManager.h"
#include "ExceptionManager.h"

using namespace jdwp;

namespace jdwp {

    /**
     * Contains information about Java threads only
     */
    class JavaThreadInfo : public AgentBase{
    public:
        jthread m_thread;
        bool    m_hasStepped;
        JavaThreadInfo(JNIEnv *jni, jthread thrd)
        {
            m_thread = jni->NewGlobalRef(thrd);
            if (m_thread == 0) {
                JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Unable to allocate new global ref for JavaThreadInfo"));
            }
            m_hasStepped = false;
        }
        void Clean(JNIEnv *jni)
        {
            jni->DeleteGlobalRef(m_thread);
        }
    };

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
         * Whether thread is alive
         */
        bool    m_isAlive;

        /**
         * A constructor.
         * Creates new instance.
         * @param jni the JNI interface pointer.
         * @param thrd java thread.
         * @param isAgentThrd indicates whether thread is internal agent's or traget VM's.
         * @param isOnEvent on event thread suspension.
         */
        ThreadInfo(JNIEnv *jni, jthread thrd, bool isAgentThrd = false, bool isOnEvent = false)
        {
            m_thread = jni->NewGlobalRef(thrd);
            if (m_thread == 0) {
                JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Unable to allocate new global ref for ThreadInfo"));
            }
            m_isAgentThread = isAgentThrd;
            m_isOnEvent = isOnEvent;
            m_suspendCount = 0;
            m_threadName = 0;
            m_isAlive = 1;
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
        for (; result.hasNext();) {
            ThreadInfo* element= result.getNext();
            if (element != 0 && 
                    jni->IsSameObject((element)->m_thread, thread) == JNI_TRUE){
		    // TODO revert this JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "FindThreadInfo : agent thread=%p, name=%s, oldCount=%d, isOnEvent=%d", (element)->m_thread, JDWP_CHECK_NULL(((element)->m_thread)->m_threadName), ((element)->m_thread)->m_suspendCount, ((element)->m_thread)->m_isOnEvent));
                break;
            }
        }
    }

    /**
     * Looks for corresponding Java thread info in the Java thread 
     * info list 
     * @param jni - the JNI interface pointer
     * @param javathrdInfoList - pointer to the Java thread info 
     *                         list
     * @param thread - the Java thread
     * @param result - iterator for the Java thread info list
     */
    void FindJavaThreadInfo(JNIEnv *jni, JavaThreadInfoList *javathrdInfoList, jthread thread,
        JavaThreadInfoList::iterator &result)
    {
        for (; result.hasNext();) {
            JavaThreadInfo* element= result.getNext();
            if (element != 0 && 
                    jni->IsSameObject((element)->m_thread, thread) == JNI_TRUE){
                break;
            }
        }
    }

}// namespace jdwp

//-----------------------------------------------------------------------------
//ThreadManager----------------------------------------------------------------

ThreadManager::~ThreadManager()
{
    /* FIXME - Temporary comment out for to avoid shutdown crash
    JDWP_ASSERT(m_thrdmgrMonitor == 0);    
    JDWP_ASSERT(m_execMonitor == 0);*/
}
        
//-----------------------------------------------------------------------------

void
ThreadManager::Init(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Init(%p)", jni));

    JDWP_ASSERT(m_javathrdmgrMonitor == 0);
    JDWP_ASSERT(m_thrdmgrMonitor == 0);

    m_execMonitor = new AgentMonitor("_jdwp_ThreadManager_execMonitor");
    m_javathrdmgrMonitor = new AgentMonitor("_jdwp_ThreadManager_javathrdmgrMonitor");
    m_thrdmgrMonitor = new AgentMonitor("_jdwp_ThreadManager_thrdmgrMonitor");
    
    m_stepMonitor = new AgentMonitor("_jdwp_ThreadManager_stepMonitor");
    m_popFramesMonitor = new AgentMonitor("_jdwp_ThreadManager_popFramesMonitor");
    m_stepMonitorReleased = false;
    m_popFramesMonitorReleased = false;

}

//-----------------------------------------------------------------------------

void
ThreadManager::Clean(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Clean(%p)", jni));

    /* Temporarily commented out to workaround shutdown crash
    if(m_execMonitor !=0) {
        delete m_execMonitor;
        m_execMonitor = 0;
    }

    if(m_thrdmgrMonitor != 0) {
        delete m_thrdmgrMonitor;
        m_thrdmgrMonitor = 0;
    }*/

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

int
ThreadManager::Reset(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Reset(%p)", jni));

    // delete not yet started but registered handlers for method invoke
    if (m_execMonitor != 0) {
        MonitorAutoLock lock(m_execMonitor JDWP_FILE_LINE);
        ClearExecList(jni);
    }

    // resume all not internal threads and remove them from list
    if (m_thrdmgrMonitor != 0) {
        MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);
        int ret = ClearThreadList(jni);
        JDWP_CHECK_RETURN(ret);
    }

    // reset flags and thread variable that are used in PopFrames process
    {
        m_stepMonitorReleased = false;
        m_popFramesMonitorReleased = false;
        m_popFramesThread = 0;
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------

int
ThreadManager::ClearThreadList(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "ClearThreadList(%p)", jni));

    jthread packetDispatcher;
    jthread eventDispatcher;
    
    for (ThreadInfoList::iterator iter = m_threadInfoList.begin(); iter.hasNext();) {
        ThreadInfo* element = iter.getNext();
        if (element == NULL) continue;
        if (!(element)->m_isAgentThread) {
            JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "Reset: resume thread=%p, name=%s", (element)->m_thread, JDWP_CHECK_NULL((element)->m_threadName)));
            // resume thread
            GetObjectManager().DeleteFrameIDs(jni, (element)->m_thread);
            jvmtiError err;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->ResumeThread((element)->m_thread));
            // remove from list and destroy
            iter.remove();
            (element)->Clean(jni);
            delete element;
            element = NULL;
        } else {
            // get thread name 
            jvmtiError err;
            jvmtiThreadInfo info;

            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo((element)->m_thread, &info));
            JvmtiAutoFree jafInfoName(info.name);
            if (err != JVMTI_ERROR_NONE) {
                AgentException ex(err);
                JDWP_SET_EXCEPTION(ex);
                return err;
            }
            char* threadName = info.name;
            
            // determine whether this agent thread should be retained
            if( strcmp( threadName, "_jdwp_PacketDispatcher") == 0){
                JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "find packet dispatcher thread=%p, name=%s", (element)->m_thread, JDWP_CHECK_NULL((element)->m_threadName)));
                packetDispatcher = (element)->m_thread;
            }else if (  strcmp( threadName, "_jdwp_EventDispatcher") == 0){
                JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "find event dispatcher thread=%p, name=%s", (element)->m_thread, JDWP_CHECK_NULL((element)->m_threadName)));
                eventDispatcher = (element)->m_thread;
            }
        }
    }
    m_threadInfoList.clear();
    
    // Background agent threads should be added back to the list
    AddThread(jni, packetDispatcher, true);
    AddThread(jni, eventDispatcher, true);

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------

void
ThreadManager::ClearExecList(JNIEnv* jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "ClearExecList(%p)", jni));

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
                               
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "RunAgentThread(%p,%p,%p,%d,%s,%p)", jni, proc, arg, priority, JDWP_CHECK_NULL(name), thread));

    if (thread == 0) {
        thread = CreateAgentThread(jni, name);
    }

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->RunAgentThread(thread, proc, arg, priority));
    if (err != JVMTI_ERROR_NONE) {
    	AgentException ex(err);
        JDWP_SET_EXCEPTION(ex);
    	return 0;
    }

    return thread;
}

//-----------------------------------------------------------------------------

jthread
ThreadManager::CreateAgentThread(JNIEnv *jni, const char *name)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "CreateAgentThread(%p,%s)", jni, JDWP_CHECK_NULL(name)));

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
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "AddThread(%p,%p,%s,%s)", jni, thread, (isAgentThread?"TRUE":"FALSE"), (isOnEvent?"TRUE":"FALSE")));

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    jint place = -1,found = -1;
    ThreadInfoList::iterator result = m_threadInfoList.begin();
    for (;result.hasNext();) {
        ThreadInfo* element = result.getNext();
        if (element == NULL) {
            // save pointer to empty slot
            place = result.getIndex();
        } else if (jni->IsSameObject((element)->m_thread, thread) == JNI_TRUE) {
            found = result.getIndex();
            // thread found
            break;
        }
    }

    // not found
    if  (found == -1)
    {
        ThreadInfo *thrdinf = new ThreadInfo(jni, thread, isAgentThread, isOnEvent);

#ifndef NDEBUG
        // save thread name for debugging purpose
        if (JDWP_TRACE_ENABLED(LOG_KIND_THREAD)) {
            jvmtiError err;
            jvmtiThreadInfo info;

            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
            if (err == JVMTI_ERROR_NONE) {
                thrdinf->m_threadName = info.name;
                JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "AddThread: add thread=%p, name=%s", thread, JDWP_CHECK_NULL(thrdinf->m_threadName)));
            }
        }        
#endif // NDEBUG        

        if (place !=-1) {
            m_threadInfoList.insert(place,thrdinf);
        } else {
            m_threadInfoList.push_back(thrdinf);
        }

        return thrdinf;
    }

    return result.getCurrent();
}

//----------------------------------------------------------------------------

void ThreadManager::RemoveThread(JNIEnv *jni, jthread thread) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "RemoveThread(%p,%p)", jni, thread));

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    ThreadInfoList::iterator result = m_threadInfoList.begin();
    for (;result.hasNext();) {
        ThreadInfo* element = result.getNext();
	if (element != NULL && jni->IsSameObject((element)->m_thread, thread) == JNI_TRUE) {
	    // thread found, remove from list and destroy
	    m_threadInfoList.remove(result.getIndex());
	    (element)->Clean(jni);
	    delete element;
	    element = 0;
	    JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "RemoveThread: add thread=%p", thread));
	    break;
	}
    }
}


//-----------------------------------------------------------------------------

/**
 * Add a Java thread to the internal Java thread info list
 * @param jni - the JNI interface pointer
 * @param thread - the Java thread to add
 * 
 * @return JavaThreadInfo* - the Java thread info structure 
 *                          created for this thread
 */
JavaThreadInfo* 
ThreadManager::AddJavaThread(JNIEnv *jni, jthread thread)
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "AddJavaThread(%p,%p)", jni, thread));

    MonitorAutoLock lock(m_javathrdmgrMonitor JDWP_FILE_LINE);

    jint place = -1,found = -1;
    JavaThreadInfoList::iterator result = m_javaThreadInfoList.begin();
    for (;result.hasNext();) {
        JavaThreadInfo* element = result.getNext();
        if (element == NULL) {
            // save pointer to empty slot
            place = result.getIndex();
        } else if (jni->IsSameObject((element)->m_thread, thread) == JNI_TRUE) {
            found = result.getIndex();
            // thread found
            break;
        }
    }

    // not found
    if  (found == -1)
    {
        JavaThreadInfo *thrdinf = new JavaThreadInfo(jni, thread);

#ifndef NDEBUG
        // save thread name for debugging purpose
        if (JDWP_TRACE_ENABLED(LOG_KIND_THREAD)) {
            jvmtiError err;
            jvmtiThreadInfo info;

            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
            JvmtiAutoFree jafInfoName(info.name);
            if (err == JVMTI_ERROR_NONE) {
                JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "AddJavaThread: add thread=%p, name=%s", thread, JDWP_CHECK_NULL(info.name)));
            }
        }        
#endif // NDEBUG        

        if (place !=-1) {
            m_javaThreadInfoList.insert(place,thrdinf);
        } else {
            m_javaThreadInfoList.push_back(thrdinf);
        }

        return thrdinf;
    }

    return result.getCurrent();
}


//----------------------------------------------------------------------------

/**
 * Remove a Java thread from the list 
 *  
 * @param jni - the JNI interface pointer 
 * @param thread - the Java thread to remove 
 */

void ThreadManager::RemoveJavaThread(JNIEnv *jni, jthread thread) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "RemoveJavaThread(%p,%p)", jni, thread));

    MonitorAutoLock lock(m_javathrdmgrMonitor JDWP_FILE_LINE);

    JavaThreadInfoList::iterator result = m_javaThreadInfoList.begin();
    for (;result.hasNext();) {
        JavaThreadInfo* element = result.getNext();
        if (element != NULL && jni->IsSameObject((element)->m_thread, thread) == JNI_TRUE) {
            // thread found, remove from list and destroy
            m_javaThreadInfoList.remove(result.getIndex());
            (element)->Clean(jni);
    	    delete element;
    	    element = 0;
    	    JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "RemoveJavaThread: add thread=%p", thread));
    	    break;
    	}
    }
}


//-----------------------------------------------------------------------------

int
ThreadManager::InternalSuspend(JNIEnv *jni, jthread thread, bool ignoreInternal, bool isOnEvent) 
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "InternalSuspend(%p,%p,%s,%s)", jni, thread, (ignoreInternal?"TRUE":"FALSE"), (isOnEvent?"TRUE":"FALSE")));

    // find thread
    jint place = -1,found = -1;
    ThreadInfoList::iterator result = m_threadInfoList.begin();
    for (;result.hasNext(); ){
	ThreadInfo* element = result.getNext();

        if (element == NULL) {
            // save pointer to empty slot
            place = result.getIndex();
        } else if (jni->IsSameObject((element)->m_thread, thread) == JNI_TRUE) {
	    found = result.getIndex();
            // thread found
            break;
        }
    }

    if  (found == -1)
    {
        // not in list -> suspend and add to list
        jvmtiError err;

    	jvmtiThreadInfo jvmtiInfo;
    
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &jvmtiInfo));
        JvmtiAutoFree jafInfoName(jvmtiInfo.name);
    	if (err != JVMTI_ERROR_NONE) {
            AgentException ex(err);
            JDWP_SET_EXCEPTION(ex);
            return err;
        }

        // ignore agent threads for AsyncCommandHandler
        if (strncmp(jvmtiInfo.name, "_jdwp_", 6) == 0) {
            JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "InternalSuspend: ignore agent thread=%p, name=%s",
                       thread, jvmtiInfo.name));
            return JDWP_ERROR_NONE;
        }

        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SuspendThread(thread));

        JDWP_ASSERT(err != JVMTI_ERROR_THREAD_SUSPENDED);

        if (err != JVMTI_ERROR_NONE && err != JVMTI_ERROR_THREAD_NOT_ALIVE){
            JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "InternalSuspend: suspend error: %d", err));
            AgentException ex(err);
            JDWP_SET_EXCEPTION(ex);
            return err;
        }

        // add thread
        ThreadInfo *thrdinf = new ThreadInfo(jni, thread, false, isOnEvent);
        thrdinf->m_suspendCount = 1 ;

        if (err == JVMTI_ERROR_THREAD_NOT_ALIVE){
            thrdinf->m_isAlive = 0 ;
        }

        if (place != -1) {
           m_threadInfoList.insert(place,thrdinf);
        } else {
           m_threadInfoList.push_back(thrdinf);
        }

#ifndef NDEBUG
        // save thread name for debugging purpose
        if (JDWP_TRACE_ENABLED(LOG_KIND_THREAD)) {
            jvmtiError err;
            jvmtiThreadInfo info;

            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
//            JvmtiAutoFree jafInfoName(info.name);
            if (err == JVMTI_ERROR_NONE) {
                thrdinf->m_threadName = info.name;
            }
        }
#endif // NDEBUG

        JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "InternalSuspend: suspend thread=%p, name=%s, oldCount=%d, isOnEvent=%s", 
                          thread, JDWP_CHECK_NULL(thrdinf->m_threadName), thrdinf->m_suspendCount, (thrdinf->m_isOnEvent?"TRUE":"FALSE")));

    } else if ((m_threadInfoList.getIndexof(found))->m_isAgentThread) {
        // agent thread -> error (if not ignored)
	ThreadInfo* element = m_threadInfoList.getIndexof(found);
        JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "InternalSuspend: ignore agent thread=%p, name=%s, oldCount=%d, isOnEvent=%s",
                          thread, JDWP_CHECK_NULL((element)->m_threadName), (element)->m_suspendCount, ((element)->m_isOnEvent?"TRUE":"FALSE")));
        if (!ignoreInternal) {
            AgentException ex(JVMTI_ERROR_INVALID_THREAD);
            JDWP_SET_EXCEPTION(ex);
            return JVMTI_ERROR_INVALID_THREAD;
        }
    } else {
        // already in list -> just increase suspend count
	ThreadInfo* element = m_threadInfoList.getIndexof(found);
        JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "InternalSuspend: increase count thread=%p, name=%s, oldCount=%d, isOnEvent=%s",
                          thread, JDWP_CHECK_NULL((element)->m_threadName), element->m_suspendCount, ((element)->m_isOnEvent?"TRUE":"FALSE")));
        JDWP_ASSERT((element)->m_suspendCount > 0);
        (element)->m_suspendCount++;
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------

int
ThreadManager::Suspend(JNIEnv *jni, jthread thread, bool isOnEvent)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Suspend(%p,%p,%s)", jni, thread, (isOnEvent?"TRUE":"FALSE")));

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);
    int ret = InternalSuspend(jni, thread, false, isOnEvent);
    return ret;
}

//-----------------------------------------------------------------------------

int
ThreadManager::InternalResume(JNIEnv *jni, jthread thread, bool ignoreInternal)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "InternalResume(%p,%p)", jni, thread));

    ThreadInfoList::iterator result= m_threadInfoList.begin();
    FindThreadInfo(jni, &m_threadInfoList, thread, result);
    if  (!result.hasCurrent()) {
        return JDWP_ERROR_NONE;
    }
    ThreadInfo* info = result.getCurrent();
    if (info->m_isAgentThread) {
        // agent thread -> error (if not ignored)
        JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "InternalResume: ignore agent thread=%p, name=%s, oldCount=%d, isOnEvent=%s",
                          thread, JDWP_CHECK_NULL((info)->m_threadName), (info)->m_suspendCount, ((info)->m_isOnEvent?"TRUE":"FALSE")));
        if (!ignoreInternal) {
            AgentException ex(JVMTI_ERROR_INVALID_THREAD);
            JDWP_SET_EXCEPTION(ex);
            return JVMTI_ERROR_INVALID_THREAD;
        }
    } else if (info->m_suspendCount == 1) {
        // count == 1 -> resume thread if it is alive
        GetObjectManager().DeleteFrameIDs(jni, thread);
        if (info->m_isAlive == 1 ){
             jvmtiError err;
             JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->ResumeThread(thread));   
     	     JDWP_ASSERT(err != JVMTI_ERROR_THREAD_NOT_SUSPENDED);

             if (err != JVMTI_ERROR_NONE) {
                 AgentException ex(err);
                 JDWP_SET_EXCEPTION(ex);
                 return err;
             }
        }


        JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "InternalResume: resume thread=%p, name=%s, oldCount=%d, isOnEvent=%s, isAlive=%s",
                          thread, JDWP_CHECK_NULL((info)->m_threadName), (info)->m_suspendCount, ((info)->m_isOnEvent?"TRUE":"FALSE"), 
                          ((info)->m_isAlive?"TRUE":"FALSE")));
            
        // remove from list and destroy
        m_threadInfoList.remove(result.getIndex());
        (info)->Clean(jni);
        delete info;
        info = 0;
    } else {
        // count > 1 -> just decrease count
        JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "InternalResume: decrease count thread=%p, name=%s, oldCount=%d, isOnEvent=%s",
                          thread, JDWP_CHECK_NULL((info)->m_threadName), (info)->m_suspendCount, ((info)->m_isOnEvent?"TRUE":"FALSE")));
        (info)->m_suspendCount--;
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------

int
ThreadManager::Resume(JNIEnv *jni, jthread thread)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Resume(%p,%p)", jni, thread));

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);
    int ret = InternalResume(jni, thread, false);
    return ret;
}

//-----------------------------------------------------------------------------

int
ThreadManager::SuspendAll(JNIEnv *jni, jthread threadOnEvent)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "SuspendAll(%p,%p)", jni, threadOnEvent));

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    jint count;
    jthread *threads = 0;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetAllThreads(&count, &threads));
    if (err != JVMTI_ERROR_NONE) {
        AgentException ex(err);
        JDWP_SET_EXCEPTION(ex);
        return err;
    }

    JvmtiAutoFree dobj(threads);

    for (jint i = 0; i < count; i++)
    {
        JDWP_ASSERT(threads[i] != 0);
        // suspend thread (ignoring internal)
        bool isOnEvent = (threadOnEvent != 0 && jni->IsSameObject(threadOnEvent, threads[i]));
        int ret = InternalSuspend(jni, threads[i], true, isOnEvent);
        if (ret != JDWP_ERROR_NONE)
        {
            jvmtiError errTemp = (jvmtiError)ret;

            JDWP_ASSERT(errTemp != JVMTI_ERROR_THREAD_NOT_SUSPENDED);

            if (errTemp != JVMTI_ERROR_NONE &&
                errTemp != JVMTI_ERROR_THREAD_NOT_ALIVE &&
                errTemp != JVMTI_ERROR_INVALID_THREAD)
            {
                return errTemp;
            }
        }
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------

int
ThreadManager::ResumeAll(JNIEnv *jni)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "ResumeAll(%p)", jni));

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    // resume all not internal threads and remove from list
    for (ThreadInfoList::iterator iter = m_threadInfoList.begin(); iter.hasNext();) {
	
        ThreadInfo* element = iter.getNext();
	
        if (element == NULL) continue;

        if (!(element)->m_isAgentThread) {
            // check suspend count
            JDWP_ASSERT((element)->m_suspendCount > 0);
            if ((element)->m_suspendCount == 1) {
                // resume application thread
                JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "ResumeAll: resume thread=%p, name=%s, oldCount=%d, isOnEvent=%s, isAlive=%s",
                          (element)->m_thread, JDWP_CHECK_NULL((element)->m_threadName), (element)->m_suspendCount, 
                           ((element)->m_isOnEvent?"TRUE":"FALSE"), ((element)->m_isAlive?"TRUE":"FALSE")));
                    

                // destroy stack frame IDs
                GetObjectManager().DeleteFrameIDs(jni, (element)->m_thread);

                // resume thread
                // resume thread if it is still alive
                if ((element)->m_isAlive == 1 ){
                    jvmtiError err;
                    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->ResumeThread((element)->m_thread));
                
                    JDWP_ASSERT(err != JVMTI_ERROR_THREAD_NOT_SUSPENDED);
                    JDWP_ASSERT(err != JVMTI_ERROR_INVALID_TYPESTATE);
                    JDWP_ASSERT(err != JVMTI_ERROR_INVALID_THREAD);
                    JDWP_ASSERT(err != JVMTI_ERROR_THREAD_NOT_ALIVE);
                    if (err != JVMTI_ERROR_NONE) {
                        AgentException ex(err);
                        JDWP_SET_EXCEPTION(ex);
                        return err;
                    }
                }
                // remove from list and destroy
                (element)->Clean(jni);
                // set the slot to null
                m_threadInfoList.insertNULL(iter.getIndex());
                delete element;
            } else {
                // just decrease suspend count
                JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "ResumeAll: decrease count thread=%p, name=%s, oldCount=%d, isOnEvent=%s",
                          (element)->m_thread, JDWP_CHECK_NULL((element)->m_threadName), (element)->m_suspendCount, 
                           ((element)->m_isOnEvent?"TRUE":"FALSE")));
                (element)->m_suspendCount--;
            }
        } else {
            // ignore agent thread
            JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "ResumeAll: ignore agent thread=%p, name=%s, oldCount=%d, isOnEvent=%s",
                          (element)->m_thread, JDWP_CHECK_NULL((element)->m_threadName), (element)->m_suspendCount, 
                          ((element)->m_isOnEvent?"TRUE":"FALSE")));
        }
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------

int
ThreadManager::Interrupt(JNIEnv *jni, jthread thread)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Interrupt(%p,%p)", jni, thread));

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->InterruptThread(thread));

    if (err != JVMTI_ERROR_NONE) {
        AgentException ex(err);
        JDWP_SET_EXCEPTION(ex);
    }
    return err;
}

//-----------------------------------------------------------------------------

int
ThreadManager::Stop(JNIEnv *jni, jthread thread, jobject throwable)
                       
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Stop(%p,%p,%p)", jni, thread, throwable));

    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->StopThread(thread, throwable));
    
    if (err != JVMTI_ERROR_NONE) {
        AgentException ex(err);
        JDWP_SET_EXCEPTION(ex);
    }
    return err;
}

//-----------------------------------------------------------------------------

void
ThreadManager::Join(JNIEnv *jni, jthread thread)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Join(%p,%p)", jni, thread));

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
    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    bool ret_value = false;

    ThreadInfoList::iterator result = m_threadInfoList.begin();
    FindThreadInfo(jni, &m_threadInfoList, thread, result);

    // found
    if  (result.hasCurrent())
        ret_value = (result.getCurrent())->m_isAgentThread;

    return ret_value;
}

//-----------------------------------------------------------------------------

/**
 * Set a boolean for the specified thread indicating if the last 
 * action was a single step 
 *  
 * @param jni - the JNI interface pointer
 * @param thread - the Java thread
 * @param hasStepped - indicates if the last action was a single 
 *                   step
 */
void 
ThreadManager::SetHasStepped(JNIEnv *jni, jthread thread, bool hasStepped) {
    MonitorAutoLock lock(m_javathrdmgrMonitor JDWP_FILE_LINE);
    JavaThreadInfoList::iterator result = m_javaThreadInfoList.begin();
    FindJavaThreadInfo(jni, &m_javaThreadInfoList, thread, result);

    jvmtiThreadInfo info;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
    JvmtiAutoFree jafInfoName(info.name);

    // If we have found a matching entry, set it's value
    if  (result.hasCurrent()) {
            (result.getCurrent())->m_hasStepped = hasStepped;
    }
}

/**
 * Return an indication of whether the last action on the 
 * specified Java thread was a single step. 
 *  
 * @param jni - the JNI interface pointer
 * @param thread - the Java thread to query
 * 
 * @return bool - indicates if the last action was a single step
 */
bool 
ThreadManager::HasStepped(JNIEnv *jni, jthread thread) {
    MonitorAutoLock lock(m_javathrdmgrMonitor JDWP_FILE_LINE);
    JavaThreadInfoList::iterator result = m_javaThreadInfoList.begin();
    FindJavaThreadInfo(jni, &m_javaThreadInfoList, thread, result);

    jvmtiThreadInfo info;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &info));
    JvmtiAutoFree jafInfoName(info.name);

    // If we have found a matching entry, return it's value
    if  (result.hasCurrent()) {
            return (result.getCurrent())->m_hasStepped;
    }

    return false;
}

//-----------------------------------------------------------------------------

jint
ThreadManager::GetSuspendCount(JNIEnv *jni, jthread thread)
{
    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    jint ret_value = 0;

    ThreadInfoList::iterator result = m_threadInfoList.begin();
    FindThreadInfo(jni, &m_threadInfoList, thread, result);
    
    // found
    if  (result.hasCurrent())
        ret_value = (result.getCurrent())->m_suspendCount;

    return ret_value;
}

//-----------------------------------------------------------------------------

bool
ThreadManager::IsSuspendedOnEvent(JNIEnv *jni, jthread thrd)
{
    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    bool ret_value = false;

    ThreadInfoList::iterator result = m_threadInfoList.begin();
    FindThreadInfo(jni, &m_threadInfoList, thrd, result);
    
    // found
    if  (result.hasCurrent())
        ret_value = (result.getCurrent())->m_isOnEvent;

    return ret_value;
}

//-----------------------------------------------------------------------------

bool
ThreadManager::IsSuspended(jthread thrd)
{
    jint thread_state;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadState(thrd, &thread_state));

    JDWP_ASSERT(err != JVMTI_ERROR_NULL_POINTER);
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error calling GetThreadState: %d", err));
        return false;
    }

    return (thread_state & JVMTI_THREAD_STATE_SUSPENDED) != 0;
}

//-----------------------------------------------------------------------------

int
ThreadManager::CheckThreadStatus(JNIEnv *jni, jthread thrd)
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "CheckThreadStatus(%p)", thrd));
    // check the thread status
    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);
	
    // check whether the reference is a thread reference
    jclass threadClass = GetClassManager().GetThreadClass();
    if( !jni->IsInstanceOf(thrd, threadClass) )
    {
        JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "## CheckThreadStatus: thread reference is not a valid thread reference "));
        AgentException ex(JVMTI_ERROR_INVALID_THREAD);
        JDWP_SET_EXCEPTION(ex);
        return JVMTI_ERROR_INVALID_THREAD;
    }
    
	// check in the suspend thread list
    bool isSuspened = false;
    ThreadInfoList::iterator result = m_threadInfoList.begin();
    FindThreadInfo(jni, &m_threadInfoList, thrd, result);
    // not found
    if  (!result.hasCurrent())
    {
        JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "## CheckThreadStatus: thread is not in suspended thread list: %p", thrd));
        AgentException ex(JVMTI_ERROR_THREAD_NOT_SUSPENDED);
        JDWP_SET_EXCEPTION(ex);
        return JVMTI_ERROR_THREAD_NOT_SUSPENDED;
    }

    jint thread_state;
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadState(thrd, &thread_state));

    if (err != JVMTI_ERROR_NONE) {
        AgentException ex(err);
        JDWP_SET_EXCEPTION(ex);
        return err;
    }

    JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "CheckThreadStatus: check thread status: %d", thread_state));
    if( thread_state & JVMTI_THREAD_STATE_TERMINATED) {
        AgentException ex(JVMTI_ERROR_INVALID_THREAD);
        JDWP_SET_EXCEPTION(ex);
        return JVMTI_ERROR_INVALID_THREAD;
    }

    if( !(thread_state & JVMTI_THREAD_STATE_SUSPENDED) ) {
        AgentException ex(JVMTI_ERROR_THREAD_NOT_SUSPENDED);
        JDWP_SET_EXCEPTION(ex);
        return JVMTI_ERROR_THREAD_NOT_SUSPENDED;
    }

    return JDWP_ERROR_NONE;
}

//-----------------------------------------------------------------------------

int 
ThreadManager::RegisterInvokeHandler(JNIEnv *jni, SpecialAsyncCommandHandler* handler)
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "RegisterInvokeHandler(%p,%p)", jni, handler));

    JDWP_ASSERT(handler->GetThread() != 0);

    // check if thread is suspended on event
    MonitorAutoLock lock(m_thrdmgrMonitor JDWP_FILE_LINE);

    ThreadInfoList::iterator result = m_threadInfoList.begin();
    FindThreadInfo(jni, &m_threadInfoList, handler->GetThread(), result);

    if  (!result.hasCurrent() || !(result.getCurrent())->m_isOnEvent) {
        AgentException ex(JDWP_ERROR_THREAD_NOT_SUSPENDED);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_THREAD_NOT_SUSPENDED;
    }

    ThreadInfo* info = result.getCurrent();
    JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "RegisterInvokeHandler: handler=%p, thread=%p, name=%s, options=%d",
                      handler, (info)->m_thread, JDWP_CHECK_NULL((info)->m_threadName), handler->GetOptions()));

    {
        MonitorAutoLock lock(m_execMonitor JDWP_FILE_LINE);
        m_execList.push_back(handler);
    }

    int ret;
    if ((handler->GetOptions() & JDWP_INVOKE_SINGLE_THREADED) == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "RegisterInvokeHandler -- resume all before method invoke: thread=%p", handler->GetThread()));
        ret = ResumeAll(jni);
    } else {
        JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "RegisterInvokeHandler -- resume before method invoke: thread=%p", handler->GetThread()));
        ret = Resume(jni, handler->GetThread());
    }

    return ret;
}

SpecialAsyncCommandHandler*
ThreadManager::FindInvokeHandler(JNIEnv* jni, jthread thread)
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "FindInvokeHandler(%p,%p)", jni, thread));

    MonitorAutoLock lock(m_execMonitor JDWP_FILE_LINE);
    for (ExecListIterator i = m_execList.begin(); i.hasNext();) {
        SpecialAsyncCommandHandler* handler = i.getNext();
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
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "HandleInternalSingleStep(%p,%p,&p,%lld)", jni, thread, method, location));

    //char* threadName = 0;
    char* methodName = 0;
#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_THREAD)) {
        jvmtiError err;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(method, &methodName, 0, 0));
        
        // don't invoke GetThreadInfo() because it may issue extra STEP events
        // jvmtiThreadInfo threadInfo;
        // JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &threadInfo));
        // threadName = threadInfo.name;
    }
#endif // NDEBUG
    //JvmtiAutoFree threadNameAutoFree(threadName);
    JvmtiAutoFree methodNameAutoFree(methodName);

    {
        MonitorAutoLock stepMonitorLock(m_stepMonitor JDWP_FILE_LINE);
        {
            MonitorAutoLock popFramesMonitorLock(m_popFramesMonitor JDWP_FILE_LINE);
            // notify that thread is waiting on suspension point
            m_popFramesMonitorReleased = true;
            m_popFramesMonitor->NotifyAll();

            JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "HandleInternalSingleStep: thread on suspention point: thread=%p, method=%s, location=%lld",
                              thread, JDWP_CHECK_NULL(methodName), location));
        }
        // wait for suspend on suspention point
        m_stepMonitorReleased = false;
        while (!m_stepMonitorReleased) {
            m_stepMonitor->Wait();
        }
        JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "HandleInternalSingleStep: thread resumed: thread=%p, method=%s, location=%lld",
                              thread, JDWP_CHECK_NULL(methodName), location));
    }

    // execute registered MethodInvoke hadlers if needed
    GetEventDispatcher().ExecuteInvokeMethodHandlers(jni, thread);
}

int ThreadManager::PerformPopFrames(JNIEnv* jni, jint framesToPop, jthread thread) 
   
{
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "PerformPopFrames(%p,%d,%p)", jni, framesToPop, thread));

    MonitorAutoLock thrdmgrMonitorLock(m_thrdmgrMonitor JDWP_FILE_LINE);
    int ret;

    // thread should be suspended
    if (!GetThreadManager().IsSuspended(thread)) {
        AgentException ex(JDWP_ERROR_THREAD_NOT_SUSPENDED);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_THREAD_NOT_SUSPENDED;
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
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetThreadInfo(thread, &threadInfo));
            threadName = threadInfo.name;
        }
#endif // NDEBUG
    
    // The following check is just for sure.
    // This algorithm supposes that there are no invoke method handlers registered.
    // Otherwise, active invoke handlers could break popFrame process.
    {
        MonitorAutoLock lockExecList(m_execMonitor JDWP_FILE_LINE);
        if (!m_execList.empty()) {
            AgentException ex(JDWP_ERROR_THREAD_NOT_SUSPENDED);
            JDWP_SET_EXCEPTION(ex);
            return JDWP_ERROR_THREAD_NOT_SUSPENDED;
        }
    }

    jint frameCount;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFrameCount(thread, &frameCount));
    if (err != JVMTI_ERROR_NONE) {
        AgentException ex(err);
        JDWP_SET_EXCEPTION(ex);
        return err;
    }
    if (frameCount <= framesToPop) {
        AgentException ex(JDWP_ERROR_INVALID_FRAMEID);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_INVALID_FRAMEID;
    }

    // if there is native frame, pop frame can't be performed
    ret = CheckNativeFrameExistence(thread, framesToPop);
    JDWP_CHECK_RETURN(ret);

    MonitorAutoLock popFramesMonitorLock(m_popFramesMonitor JDWP_FILE_LINE);
    m_popFramesThread = thread;

    // enabling step request on thread where PopFrame command is performed
    JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "PerformPopFrames: enable internal step: thread=%s", JDWP_CHECK_NULL(threadName)));
    ret = GetRequestManager().EnableInternalStepRequest(jni, m_popFramesThread);
    JDWP_CHECK_RETURN(ret);

    // cycle where topmost frames are popped one after one
    for (int i = 0; i < framesToPop; i++) {
        // pop topmost frame, thread is already suspended on event
        JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "PerformPopFrames: pop: frame#=%d, thread=%s", i, JDWP_CHECK_NULL(threadName)));
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->PopFrame(m_popFramesThread));
        if (err != JVMTI_ERROR_NONE) {
            AgentException ex(err);
            JDWP_SET_EXCEPTION(ex);
            JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "PerformPopFrames: disable internal step: thread=%s", JDWP_CHECK_NULL(threadName)));
            GetRequestManager().DisableInternalStepRequest(jni, m_popFramesThread);
            m_popFramesThread = 0;
            return err;
        }

        // resume thread
        JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "PerformPopFrames: resume: thread=%s", JDWP_CHECK_NULL(threadName)));
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->ResumeThread(m_popFramesThread));
        JDWP_ASSERT(err != JVMTI_ERROR_THREAD_NOT_SUSPENDED);
        if (err != JVMTI_ERROR_NONE) {
            AgentException ex(err);
            JDWP_SET_EXCEPTION(ex);
            JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "PerformPopFrames: disable internal step: thread=%s", JDWP_CHECK_NULL(threadName)));
            GetRequestManager().DisableInternalStepRequest(jni, m_popFramesThread);
            m_popFramesThread = 0;
            return err;
        }
        
        // wait for thread to achieve suspention point in InternalSingleStep handler
        JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "PerformPopFrames: wait for step: thread=%s", JDWP_CHECK_NULL(threadName)));
        m_popFramesMonitorReleased = false;
        while (!m_popFramesMonitorReleased) {
            m_popFramesMonitor->Wait();
        }

        {
            // suspend thread on suspention point
            MonitorAutoLock stepMonitorLock(m_stepMonitor JDWP_FILE_LINE);
            JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "PerformPopFrames: suspend: thread=%s", JDWP_CHECK_NULL(threadName)));
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SuspendThread(m_popFramesThread));
            JDWP_ASSERT(err != JVMTI_ERROR_THREAD_SUSPENDED);
            if (err != JVMTI_ERROR_NONE) {
                AgentException ex(err);
                JDWP_SET_EXCEPTION(ex);
                JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "PerformPopFrames: disable internal step: thread=%s", JDWP_CHECK_NULL(threadName)));
                GetRequestManager().DisableInternalStepRequest(jni, m_popFramesThread);
                m_popFramesThread = 0;
                return err;
            }
    
            // notify suspended thread on suspention point
            m_stepMonitorReleased = true;
            m_stepMonitor->NotifyAll();
            JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "PerformPopFrames: notify: thread=%s", JDWP_CHECK_NULL(threadName)));
        }

    }
    GetObjectManager().DeleteFrameIDs(jni, m_popFramesThread);
    
    JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "PerformPopFrames: disable internal step: thread=%s", JDWP_CHECK_NULL(threadName)));
    ret = GetRequestManager().DisableInternalStepRequest(jni, m_popFramesThread);        
    m_popFramesThread = 0;
    JDWP_CHECK_RETURN(ret);

    return JDWP_ERROR_NONE;
}

int ThreadManager::CheckNativeFrameExistence(jthread thread, jint framesToPop) 
   
{
    jvmtiFrameInfo* frames = static_cast<jvmtiFrameInfo*>
        (GetMemoryManager().Allocate((framesToPop+1) 
            * sizeof(jvmtiFrameInfo) JDWP_FILE_LINE));
    AgentAutoFree af(frames JDWP_FILE_LINE);
    
    jint count;
    jvmtiError err;
    // check (framesToPop+1) frames, because both the called 
    // and calling methods must be non-native
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetStackTrace(thread, 0, 
        (framesToPop+1), frames, &count));
    if (err != JVMTI_ERROR_NONE) {
        AgentException ex(err);
        JDWP_SET_EXCEPTION(ex);
        return err;
    }
    JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "CheckNativeFrameExistence: FramesToCheck=%d", count));
    
    jboolean isNative = false;
    for (int i = 0; i < count; i++) {
        jmethodID methodID = frames[i].method;
        
        jvmtiError err;
        char* methodName = 0;
        char* classSignature = 0;

#ifndef NDEBUG
        if (JDWP_TRACE_ENABLED(LOG_KIND_THREAD)) {
            jclass clazz = 0;
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodName(methodID, &methodName, 0, 0));
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodDeclaringClass(methodID, &clazz));
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(clazz, &classSignature, 0));
            
            JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "CheckNativeFrameExistence: method=%s, class=%s", JDWP_CHECK_NULL(methodName), JDWP_CHECK_NULL(classSignature)));
        }
#endif // NDEBUG

        JvmtiAutoFree methodNameAutoFree(methodName);
        JvmtiAutoFree classSignatureAutoFree(classSignature);
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->IsMethodNative(methodID, &isNative));
        if (err != JVMTI_ERROR_NONE) {
            AgentException ex(err);
            JDWP_SET_EXCEPTION(ex);
            return err;
        }
        if (isNative) {
            JDWP_TRACE(LOG_RELEASE, (LOG_THREAD_FL, "CheckNativeFrameExistence: method=%s, class=%s is native", JDWP_CHECK_NULL(methodName), JDWP_CHECK_NULL(classSignature)));
            AgentException ex(JDWP_ERROR_NATIVE_METHOD);
            JDWP_SET_EXCEPTION(ex);
            return JDWP_ERROR_NATIVE_METHOD;
        }
    }

    return JDWP_ERROR_NONE;
}
