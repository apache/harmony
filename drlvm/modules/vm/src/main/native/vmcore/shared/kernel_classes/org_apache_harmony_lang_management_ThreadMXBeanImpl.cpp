/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Andrey Yakushev
 */

/**
 * @file org_apache_harmony_lang_management_ThreadMXBeanImpl.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of
 * org.apache.harmony.lang.management.ThreadMXBeanImpl class.
 */

#include <jni.h>
#include <cxxlog.h>
#include "exceptions.h"
#include "environment.h"
#include "java_lang_System.h"
#include "org_apache_harmony_lang_management_ThreadMXBeanImpl.h"
#include "jthread.h"

/* Native methods */

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.findMonitorDeadlockedThreadsImpl()[J
 */
JNIEXPORT jlongArray JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_findMonitorDeadlockedThreadsImpl
(JNIEnv *jenv_ext, jobject)
{
    TRACE2("management", "findMonitorDeadlockedThreadsImpl invocation");
    JNIEnv_Internal *jenv = (JNIEnv_Internal *)jenv_ext;
    jthread* threads;
    jthread* dead_threads;
    jint count;
    jint dead_count;
    jmethodID mid;
    jlongArray array;

    IDATA UNUSED status = jthread_get_all_threads(&threads, &count);
    assert(!status);

    status = jthread_get_deadlocked_threads(threads, count, &dead_threads, &dead_count);
    assert(!status);

    if (dead_count == 0){
        return NULL;
    }

    jlong* ids = (jlong*)malloc(sizeof(jlong)* dead_count);
    assert(ids);

    jclass cl = jenv->FindClass("java/lang/Thread");
    if (jenv->ExceptionCheck()) goto cleanup;

    mid = jenv->GetMethodID(cl, "getId","()J");
    if (jenv->ExceptionCheck()) goto cleanup;

    for (int i = 0; i < dead_count; i++){
        ids[i] = jenv->CallLongMethod(dead_threads[i], mid);
        if (jenv->ExceptionCheck()) goto cleanup;
    }
    

    array = jenv->NewLongArray(dead_count);
    if (jenv->ExceptionCheck()) goto cleanup;

    jenv->SetLongArrayRegion(array, 0, dead_count, ids);
    if (jenv->ExceptionCheck()) goto cleanup;

cleanup:
    free(threads);
    free(dead_threads);
    free(ids);

    return array;
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.getAllThreadIdsImpl()[J
 */
JNIEXPORT jlongArray JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getAllThreadIdsImpl
(JNIEnv *jenv_ext, jobject)
{
    TRACE2("management", "getAllThreadIdsImpl invocation");
    JNIEnv_Internal *jenv = (JNIEnv_Internal *)jenv_ext;
    jthread* threads;
    jint count;
    jlongArray array;
    jclass cl;
    jmethodID mid;
    jmethodID m_get_state;
    jclass cls;
    jfieldID field_terminated;
    jobject state_terminated;
    int ids_count = 0;

    IDATA UNUSED status = jthread_get_all_threads(&threads, &count);
    assert(!status);

    jlong* ids = (jlong*)malloc(sizeof(jlong)* count);
    assert(ids);

    cl =jenv->FindClass("java/lang/Thread");
    if (jenv->ExceptionCheck()) goto cleanup;

    mid = jenv->GetMethodID(cl, "getId","()J");
    if (jenv->ExceptionCheck()) goto cleanup;

    m_get_state = jenv->GetMethodID(cl, "getState","()Ljava/lang/Thread$State;");
    if (jenv->ExceptionCheck()) goto cleanup;

    cls =jenv->FindClass("java/lang/Thread$State");
    if (jenv->ExceptionCheck()) goto cleanup;

    field_terminated = jenv->GetStaticFieldID(cls, "TERMINATED", "Ljava/lang/Thread$State;");
    if (jenv->ExceptionCheck()) goto cleanup;

    state_terminated = jenv->GetStaticObjectField(cls, field_terminated);
    if (jenv->ExceptionCheck()) goto cleanup;

    for (int i = 0; i < count; i++){
        jthread thread_i = threads[i];

        jobject state = jenv->CallObjectMethod(thread_i, m_get_state);
        if (jenv->ExceptionCheck()) goto cleanup;

        jboolean is_terminated = jenv->IsSameObject(state, state_terminated);
        if (jenv->ExceptionCheck()) goto cleanup;

        if (!is_terminated){
            ids[ids_count++] = jenv->CallLongMethod(thread_i, mid);
            if (jenv->ExceptionCheck()) goto cleanup;
        }
    }

    array = jenv->NewLongArray(ids_count);
    if (jenv->ExceptionCheck()) goto cleanup;

    jenv->SetLongArrayRegion(array, 0, ids_count, ids);
    if (jenv->ExceptionCheck()) goto cleanup;

cleanup:
    free(threads);
    free(ids);

    return array;
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.getDaemonThreadCountImpl()I
 */
JNIEXPORT jint JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getDaemonThreadCountImpl(JNIEnv *jenv, jobject)
{
    jthread* threads;
    jint count;
    jint daemon_count = 0;
    jclass cl;
    jmethodID id;

    TRACE2("management", "getDaemonThreadCountImpl invocation");
    IDATA UNUSED status = jthread_get_all_threads(&threads, &count);
    assert(!status);

    cl = jenv->FindClass("java/lang/Thread");
    if (jenv->ExceptionCheck()) goto cleanup;
    id = jenv->GetMethodID(cl, "isDaemon","()Z");
    if (jenv->ExceptionCheck()) goto cleanup;

    for (int i = 0; i < count; i++){
        int is_daemon = jenv->CallBooleanMethod(threads[i], id);
        if (jenv->ExceptionCheck()) goto cleanup;
        if (is_daemon){
            daemon_count++;
        }
    }
cleanup:
    free(threads);

    return daemon_count;
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.getPeakThreadCountImpl()I
 */
JNIEXPORT jint JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getPeakThreadCountImpl(JNIEnv *, jobject)
{
    jint count = 0;
    TRACE2("management", "getPeakThreadCountImpl invocation");
    IDATA UNUSED status = jthread_get_peak_thread_count(&count);
    assert(!status);
    return count;
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.getThreadCountImpl()I
 */
JNIEXPORT jint JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getThreadCountImpl(JNIEnv *, jobject)
{
    jint count;
    TRACE2("management", "getThreadCountImpl invocation");
    IDATA UNUSED status = jthread_get_thread_count(&count);
    assert(!status);
    return count;
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.getThreadCpuTimeImpl(J)J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getThreadCpuTimeImpl
(JNIEnv * jenv_ext, jobject obj, jlong thread_id)
{
    JNIEnv_Internal *jenv = (JNIEnv_Internal *)jenv_ext;
    jlong nanos;
    TRACE2("management", "getThreadCpuTimeImpl invocation");
    jthread thread = Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getThreadByIdImpl(jenv_ext, obj, thread_id);
    if (jenv->ExceptionCheck()) return 0;
    if (! thread){
         return -1;
    }
    IDATA UNUSED status = jthread_get_thread_cpu_time(thread, &nanos);
    assert(status == TM_ERROR_NONE);
    return nanos;
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.getThreadByIdImpl(J)Ljava/lang/Thread;
 */
JNIEXPORT jobject JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getThreadByIdImpl(
    JNIEnv * jenv_ext,
    jobject,
    jlong thread_id)
{
    TRACE2("management", "getThreadByIdImpl invocation");

    JNIEnv_Internal *jenv = (JNIEnv_Internal *)jenv_ext;
    jthread* threads;
    jint count;
    jlong id;
    jobject res = NULL;
    jclass cl;
    jmethodID jmid;
    jclass cls;
    jmethodID m_get_state;
    jfieldID field_terminated;
    jobject state_terminated;

    IDATA UNUSED status = jthread_get_all_threads(&threads, &count);
    assert(!status);

    cl =jenv->FindClass("java/lang/Thread");
    if (jenv->ExceptionCheck()) goto cleanup;

    jmid = jenv->GetMethodID(cl, "getId","()J");
    if (jenv->ExceptionCheck()) goto cleanup;

    cls =jenv->FindClass("java/lang/Thread$State");
    if (jenv->ExceptionCheck()) goto cleanup;

    m_get_state = jenv->GetMethodID(cl, "getState", "()Ljava/lang/Thread$State;");
    if (jenv->ExceptionCheck()) goto cleanup;

    field_terminated = jenv->GetStaticFieldID(cls, "TERMINATED", "Ljava/lang/Thread$State;");
    if (jenv->ExceptionCheck()) goto cleanup;

    state_terminated = jenv->GetStaticObjectField(cls, field_terminated);
    if (jenv->ExceptionCheck()) goto cleanup;

    for (int i = 0; i < count; i++){
        jthread thread_i = threads[i];

        id = jenv->CallLongMethod(thread_i, jmid);
        if (jenv->ExceptionCheck()) goto cleanup;
        
        if (id == thread_id){
            jobject state = jenv->CallObjectMethod(thread_i, m_get_state);
            if (jenv->ExceptionCheck()) goto cleanup;

            jboolean is_terminated = jenv->IsSameObject(state, state_terminated);
            if (jenv->ExceptionCheck()) goto cleanup;

            if (!is_terminated){
                res = jenv->NewGlobalRef(thread_i);
            }
            break;
        }
    }

cleanup:
    free(threads);

    return res;
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.getObjectThreadIsBlockedOnImpl(Ljava/lang/Thread;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getObjectThreadIsBlockedOnImpl(JNIEnv *, jobject,
                                                                                        jobject thread)
{
    jobject monitor;
    TRACE2("management", "getObjectThreadIsBlockedOnImpl invocation");
    IDATA UNUSED status = jthread_get_contended_monitor(thread, &monitor);
    assert(!status);
    if (monitor){
        return monitor;
    }
    status = jthread_get_wait_monitor(thread, &monitor);
    assert(!status);
    return monitor;
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.getThreadOwningObjectImpl(Ljava/lang/Object;)Ljava/lang/Thread;
 */
JNIEXPORT jobject JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getThreadOwningObjectImpl(JNIEnv *, jobject,
                                                                                   jobject monitor)
{
    jthread lock_owner;
    TRACE2("management", "getThreadOwningObjectImpl invocation");
    IDATA UNUSED status = jthread_get_lock_owner(monitor, &lock_owner);
    assert(!status);
    return lock_owner;
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.isSuspendedImpl(Ljava/lang/Thread;)Z
 */
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_isSuspendedImpl(JNIEnv *, jobject,
                                                                         jobject thread)
{
    jint thread_state;
    TRACE2("management", "ThreadMXBeanImpl_isSuspendedImpl invocation");
    IDATA UNUSED status = jthread_get_jvmti_state(thread, &thread_state);
    assert(status == TM_ERROR_NONE);
    return ((thread_state & TM_THREAD_STATE_SUSPENDED) != 0);
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.getThreadWaitedCountImpl(Ljava/lang/Thread;)J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getThreadWaitedCountImpl(JNIEnv *, jobject,
                                                                                  jobject thread)
{
    TRACE2("management", "getThreadWaitedCountImpl invocation");
    return jthread_get_thread_waited_times_count(thread);
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.getThreadWaitedTimeImpl(Ljava/lang/Thread;)J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getThreadWaitedTimeImpl(JNIEnv *, jobject,
                                                                                 jobject thread)
{
    jlong nanos;
    TRACE2("management", "getThreadWaitedCountImpl invocation");
    IDATA UNUSED status = jthread_get_thread_waited_time(thread, &nanos);
    assert(status == TM_ERROR_NONE);
    return nanos;
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.getThreadBlockedTimeImpl(Ljava/lang/Thread;)J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getThreadBlockedTimeImpl(JNIEnv *, jobject,
                                                                                  jobject thread)
{
    jlong nanos;
    TRACE2("management", "getThreadBlockedTimeImpl invocation");
    IDATA UNUSED status = jthread_get_thread_blocked_time(thread, &nanos);
    assert(status == TM_ERROR_NONE);
    return nanos;
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.getThreadBlockedCountImpl(Ljava/lang/Thread;)J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getThreadBlockedCountImpl(JNIEnv *, jobject,
                                                                                   jobject thread)
{
    TRACE2("management", "getThreadBlockedCountImpl invocation");
    return jthread_get_thread_blocked_times_count(thread);
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.createThreadInfoImpl(JLjava/lang/String;Ljava/lang/Thread$State;ZZJJJJLjava/lang/String;JLjava/lang/String;[Ljava/lang/StackTraceElement;)Ljava/lang/management/ThreadInfo;
 */
JNIEXPORT jobject JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_createThreadInfoImpl(
    JNIEnv *jenv_ext,
    jobject ,
    jlong threadIdVal,
    jstring threadNameVal,
    jobject threadStateVal,
    jboolean suspendedVal,
    jboolean inNativeVal,
    jlong blockedCountVal,
    jlong blockedTimeVal,
    jlong waitedCountVal,
    jlong waitedTimeVal,
    jstring lockNameVal,
    jlong lockOwnerIdVal,
    jstring lockOwnerNameVal,
    jobjectArray stackTraceVal)
{
    JNIEnv_Internal *jenv = (JNIEnv_Internal *)jenv_ext;

    TRACE2("management", "createThreadInfoImpl invocation");
    jclass threadInfoClazz =jenv->FindClass("java/lang/management/ThreadInfo");
    if (jenv->ExceptionCheck()) return NULL;
    jmethodID threadInfoClazzConstructor = jenv->GetMethodID(threadInfoClazz, "<init>",
        "(JLjava/lang/String;Ljava/lang/Thread$State;ZZJJJJLjava/lang/String;"
        "JLjava/lang/String;[Ljava/lang/StackTraceElement;)V");
    if (jenv->ExceptionCheck()) return NULL;

    jobject threadInfo = jenv->NewObject(
        threadInfoClazz,
        threadInfoClazzConstructor,
        threadIdVal,
        threadNameVal,
        threadStateVal,
        suspendedVal,
        inNativeVal,
        blockedCountVal,
        blockedTimeVal,
        waitedCountVal,
        waitedTimeVal,
        lockNameVal,
        lockOwnerIdVal,
        lockOwnerNameVal,
        stackTraceVal);

    return threadInfo;
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.getThreadUserTimeImpl(J)J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getThreadUserTimeImpl
(JNIEnv * jenv_ext, jobject obj, jlong threadId)
{
    TRACE2("management", "getThreadUserTimeImpl invocation");
    JNIEnv_Internal *jenv = (JNIEnv_Internal *)jenv_ext;
    jlong nanos;
    jthread thread = Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getThreadByIdImpl(jenv_ext, obj, threadId);
    if (jenv->ExceptionCheck()) return 0;
    if (! thread){
         return -1;
    }
    IDATA UNUSED status = jthread_get_thread_user_cpu_time(thread, &nanos);
    assert(status == TM_ERROR_NONE);
    return nanos;
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.getTotalStartedThreadCountImpl()J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_getTotalStartedThreadCountImpl(JNIEnv *, jobject)
{
    jint count;
    TRACE2("management", "getTotalStartedThreadCountImpl invocation");
    IDATA UNUSED status = jthread_get_total_started_thread_count(&count);
    assert(status == TM_ERROR_NONE);
    return count;
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.isCurrentThreadCpuTimeSupportedImpl()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_isCurrentThreadCpuTimeSupportedImpl(JNIEnv *, jobject)
{
    TRACE2("management", "isCurrentThreadCpuTimeSupportedImpl invocation");
    return jthread_is_current_thread_cpu_time_supported();
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.isThreadContentionMonitoringEnabledImpl()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_isThreadContentionMonitoringEnabledImpl(JNIEnv *, jobject)
{
    TRACE2("management", "isThreadContentionMonitoringEnabledImpl invocation");
    return jthread_is_thread_contention_monitoring_enabled();
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.isThreadContentionMonitoringSupportedImpl()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_isThreadContentionMonitoringSupportedImpl(JNIEnv *, jobject)
{
    TRACE2("management", "isThreadContentionMonitoringSupportedImpl invocation");
    return jthread_is_thread_contention_monitoring_supported();
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.isThreadCpuTimeEnabledImpl()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_isThreadCpuTimeEnabledImpl(JNIEnv *, jobject)
{
    TRACE2("management", "isThreadCpuTimeEnabledImpl invocation");
    return jthread_is_thread_cpu_time_enabled();
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.isThreadCpuTimeSupportedImpl()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_isThreadCpuTimeSupportedImpl(JNIEnv *, jobject)
{
    TRACE2("management", "isThreadCpuTimeSupportedImpl invocation");
    return jthread_is_thread_cpu_time_supported();
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.resetPeakThreadCountImpl()V
 */
JNIEXPORT void JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_resetPeakThreadCountImpl(JNIEnv *, jobject)
{
    TRACE2("management", "resetPeakThreadCountImpl invocation");
    IDATA UNUSED status = jthread_reset_peak_thread_count();
    assert(status == TM_ERROR_NONE);
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.setThreadContentionMonitoringEnabledImpl(Z)V
 */
JNIEXPORT void JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_setThreadContentionMonitoringEnabledImpl(
    JNIEnv *,
    jobject,
    jboolean new_value)
{
    // TODO implement this method stub correctly
    TRACE2("management", "setThreadContentionMonitoringEnabledImpl invocation");
    jthread_set_thread_contention_monitoring_enabled(new_value);
};

/*
 * Method: org.apache.harmony.lang.management.ThreadMXBeanImpl.setThreadCpuTimeEnabledImpl(Z)V
 */
JNIEXPORT void JNICALL
Java_org_apache_harmony_lang_management_ThreadMXBeanImpl_setThreadCpuTimeEnabledImpl(JNIEnv *, jobject,
                                                                                     jboolean new_value)
{
    TRACE2("management", "setThreadCpuTimeEnabledImpl invocation");
    jthread_set_thread_cpu_time_enabled(new_value);
};



