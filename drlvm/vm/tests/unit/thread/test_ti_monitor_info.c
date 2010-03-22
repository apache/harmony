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


#include <stdio.h>
#include "testframe.h"
#include "thread_unit_test_utils.h"
#include <jthread.h>
#include <open/hythread_ext.h>
#include <ti_thread.h>

hysem_t mon_enter;

/*
 * Test jthread_get_contended_monitor(...)
 */
void JNICALL run_for_test_jthread_get_contended_monitor(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;
    
    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    tested_thread_started(tts);
    status = jthread_monitor_enter(monitor);
    // Begin critical section
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_IN_CRITICAL_SECTON : TT_PHASE_ERROR);
    hysem_set(mon_enter, 1);
    tested_thread_wait_for_stop_request(tts);
    // End critical section
    status = jthread_monitor_exit(monitor);
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_DEAD : TT_PHASE_ERROR);
    tested_thread_ended(tts);
}

int test_jthread_get_contended_monitor(void) {

    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *critical_tts = NULL;
    jobject contended_monitor;
    int i;

    hysem_create(&mon_enter, 0, 1);

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_get_contended_monitor);
    
    for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++){
        critical_tts = NULL;
        
        hysem_wait(mon_enter);

        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)){
            while(tts->phase == TT_PHASE_NONE) {
                // thread is not started yet
                hythread_yield();
            }
            if (tts->phase == TT_PHASE_IN_CRITICAL_SECTON){
                tf_assert_same(jthread_get_contended_monitor(tts->java_thread,
                    &contended_monitor), TM_ERROR_NONE);
                tf_assert_null(contended_monitor);
                tf_assert_null(critical_tts);
                critical_tts = tts;
            } else if (tts->phase != TT_PHASE_DEAD) {
                check_tested_thread_phase(tts, TT_PHASE_WAITING_ON_MONITOR);
                // This can't be guaranteed
                //tf_assert(vm_objects_are_equal(contended_monitor, tts->monitor));
            }
        }
        tested_thread_send_stop_request(critical_tts);
        tested_thread_wait_ended(critical_tts);
        check_tested_thread_phase(critical_tts, TT_PHASE_DEAD);
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
}

/*
 * Test jthread_holds_lock(...)
 */
void JNICALL run_for_test_jthread_holds_lock(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;
    
    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    tested_thread_started(tts);
    status = jthread_monitor_enter(monitor);
    // Begin critical section
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_IN_CRITICAL_SECTON : TT_PHASE_ERROR);
    hysem_set(mon_enter, 1);
    tested_thread_wait_for_stop_request(tts);
    status = jthread_monitor_exit(monitor);
    // End critical section
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_DEAD : TT_PHASE_ERROR);
    tested_thread_ended(tts);
}

int test_jthread_holds_lock(void) {

    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *critical_tts = NULL;
    int blocked_count;
    int i;

    hysem_create(&mon_enter, 0, 1);

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_holds_lock);
    

    for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++){
        blocked_count = 0;
        critical_tts = NULL;

        hysem_wait(mon_enter);

        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)){
            while(tts->phase == TT_PHASE_NONE) {
                // thread is not started yet
                hythread_yield();
            }
            if (tts->phase == TT_PHASE_IN_CRITICAL_SECTON){
                tf_assert(jthread_holds_lock(tts->java_thread, tts->monitor) > 0);
                tf_assert_null(critical_tts);
                critical_tts = tts;
            } else if (tts->phase != TT_PHASE_DEAD){
                check_tested_thread_phase(tts, TT_PHASE_WAITING_ON_MONITOR);
                tf_assert(jthread_holds_lock(tts->java_thread, tts->monitor) == 0);
                if (tts->phase == TT_PHASE_WAITING_ON_MONITOR){
                    blocked_count++;
                }
            }
        }
        tf_assert(critical_tts); // thread in critical section found
        tf_assert_same(blocked_count, MAX_TESTED_THREAD_NUMBER - i - 1);
        tested_thread_send_stop_request(critical_tts);
        tested_thread_wait_ended(critical_tts);
        check_tested_thread_phase(critical_tts, TT_PHASE_DEAD);
    }

    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
}

/*
 * Test jthread_get_lock_owner(...)
 */
void JNICALL run_for_test_jthread_get_lock_owner(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;
    
    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    tested_thread_started(tts);
    status = jthread_monitor_enter(monitor);
    // Begin critical section
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_IN_CRITICAL_SECTON : TT_PHASE_ERROR);
    hysem_set(mon_enter, 1);
    tested_thread_wait_for_stop_request(tts);
    status = jthread_monitor_exit(monitor);
    // End critical section
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_DEAD : TT_PHASE_ERROR);
    tested_thread_ended(tts);
}

int test_jthread_get_lock_owner(void) {

    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *critical_tts = NULL;
    jthread lock_owner = NULL;
    int blocked_count;
    int i;

    hysem_create(&mon_enter, 0 , 1);

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_get_lock_owner);
    
    for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++){
        blocked_count = 0;
        critical_tts = NULL;

        hysem_wait(mon_enter);

        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)){
            while(tts->phase == TT_PHASE_NONE) {
                // thread is not started yet
                hythread_yield();
            }
            if (tts->phase == TT_PHASE_IN_CRITICAL_SECTON){
                tf_assert_null(critical_tts);
                critical_tts = tts;
            } else if (tts->phase == TT_PHASE_WAITING_ON_MONITOR){
                blocked_count++;
            }
        }
        tf_assert(critical_tts); // thread in critical section found
        tf_assert_same(blocked_count, MAX_TESTED_THREAD_NUMBER - i - 1);
        tf_assert_same(jthread_get_lock_owner(critical_tts->monitor, &lock_owner), TM_ERROR_NONE);
        tf_assert(lock_owner);
        tf_assert_same(critical_tts->java_thread->object, lock_owner->object);
        tested_thread_send_stop_request(critical_tts);
        tested_thread_wait_ended(critical_tts);
        check_tested_thread_phase(critical_tts, TT_PHASE_DEAD);
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
}

/*
 * Test jthread_get_owned_monitors(...)
 */
void JNICALL run_for_test_jthread_get_owned_monitors(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;
    
    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    tested_thread_started(tts);
    status = jthread_monitor_enter(monitor);
    // Begin critical section
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_IN_CRITICAL_SECTON : TT_PHASE_ERROR);
    hysem_set(mon_enter, 1);
    tested_thread_wait_for_stop_request(tts);
    status = jthread_monitor_exit(monitor);
    // End critical section
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_DEAD : TT_PHASE_ERROR);
    tested_thread_ended(tts);
}

int test_jthread_get_owned_monitors(void) {

    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *critical_tts;
    int i;
    jint owned_monitors_count;
    jobject *owned_monitors = NULL;

    hysem_create(&mon_enter, 0, 1);

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_get_owned_monitors);
    
    for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++){
        critical_tts = NULL;

        hysem_wait(mon_enter);

        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)) {
            while(tts->phase == TT_PHASE_NONE) {
                // thread is not started yet
                hythread_yield();
            }
            if (tts->phase == TT_PHASE_IN_CRITICAL_SECTON){
                tf_assert_same(jthread_get_owned_monitors (tts->java_thread,
                    &owned_monitors_count, &owned_monitors), TM_ERROR_NONE);
                tf_assert(critical_tts == NULL);
                critical_tts = tts;
                tf_assert_same(owned_monitors_count, 1);
                tf_assert_same(owned_monitors[0]->object, tts->monitor->object);
            } else if (tts->phase == TT_PHASE_WAITING_ON_MONITOR){
                tf_assert_same(jthread_get_owned_monitors (tts->java_thread,
                    &owned_monitors_count, &owned_monitors), TM_ERROR_NONE);
                tf_assert_same(owned_monitors_count, 0);
            }
        }
        tf_assert(critical_tts);
        tested_thread_send_stop_request(critical_tts);
        tested_thread_wait_ended(critical_tts);
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
}

TEST_LIST_START
    TEST(test_jthread_get_contended_monitor)
    TEST(test_jthread_holds_lock)
    TEST(test_jthread_get_lock_owner)
    TEST(test_jthread_get_owned_monitors)
TEST_LIST_END;
