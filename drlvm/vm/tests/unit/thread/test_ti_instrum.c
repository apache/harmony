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
#include <ti_thread.h>

static hysem_t mon_enter;

/**
 * Test jthread_get_all_threads()
 * Test jthread_get_thread_count()
 */
int test_jthread_get_all_threads(void)
{
    tested_thread_sturct_t *tts;
    jint all_threads_count;
    jint thread_count;
    jint initial_thread_count;
    jint initial_all_threads_count;
    jthread *threads = NULL;
    int i;
    JNIEnv * jni_env = jthread_get_JNI_env(jthread_self());

    tf_assert_same(jthread_get_thread_count(&initial_thread_count), TM_ERROR_NONE);
    tf_assert_same(jthread_get_all_threads(&threads, &initial_all_threads_count), TM_ERROR_NONE);

    // Initialize tts structures
    tested_threads_init(TTS_INIT_COMMON_MONITOR);

    tf_assert_same(jthread_get_thread_count(&thread_count), TM_ERROR_NONE);
    tf_assert_same(jthread_get_all_threads(&threads, &all_threads_count), TM_ERROR_NONE);
    tf_assert_same(thread_count, initial_thread_count);
    tf_assert_same(all_threads_count, initial_all_threads_count);
    tf_assert_not_null(threads);

    i = 0;
    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        tts->attrs.proc = default_run_for_test;
        tts->attrs.arg = tts;
        tf_assert_same(jthread_create_with_function(jni_env, tts->java_thread, &tts->attrs), TM_ERROR_NONE);
        tested_thread_wait_started(tts);
        tts->native_thread = jthread_get_native_thread(tts->java_thread);
        check_tested_thread_phase(tts, TT_PHASE_RUNNING);
        tf_assert_same(jthread_get_thread_count(&thread_count), TM_ERROR_NONE);
        tf_assert_same(jthread_get_all_threads(&threads, &all_threads_count), TM_ERROR_NONE);
        i++;
        tf_assert_same(thread_count, i + initial_thread_count);
        tf_assert_same(all_threads_count, i + initial_all_threads_count);
        compare_threads(threads, i, 0);
    }

    // Terminate all threads (not needed here) and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_get_all_threads

/*
 * Test jthread_get_blocked_count()
 */
void JNICALL run_for_test_jthread_get_blocked_count(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

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
} // run_for_test_jthread_get_blocked_count

int test_jthread_get_blocked_count(void)
{
    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *critical_tts;
    int i;
    int waiting_on_monitor_nmb;

    tf_assert_same(hysem_create(&mon_enter, 0, 1), TM_ERROR_NONE);

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_get_blocked_count);

    for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++){
        int cycles = MAX_TIME_TO_WAIT / CLICK_TIME_MSEC;

        tf_assert_same(hysem_wait(mon_enter), TM_ERROR_NONE);

        critical_tts = NULL;
        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)){
            if (tts->phase == TT_PHASE_IN_CRITICAL_SECTON){
                tf_assert(critical_tts == NULL);
                critical_tts = tts;
            }
        }

        waiting_on_monitor_nmb = 0;
        while ((MAX_TESTED_THREAD_NUMBER - i > waiting_on_monitor_nmb + 1) && (cycles-- > 0)) {
            tf_assert_same(jthread_get_blocked_count(&waiting_on_monitor_nmb), TM_ERROR_NONE);
            sleep_a_click();
        }
        if (cycles < 0){
            tf_fail("Wrong number waiting on monitor threads");
        }
        tested_thread_send_stop_request(critical_tts);
        tested_thread_wait_ended(critical_tts);
        check_tested_thread_phase(critical_tts, TT_PHASE_DEAD);
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_get_blocked_count

/*
 * Test jthread_get_deadlocked_threads()
 */
void JNICALL run_for_test_jthread_get_deadlocked_threads(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args)
{
    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    IDATA status = TM_ERROR_NONE;

    tts->phase =  TT_PHASE_WAITING_ON_MONITOR;
    status = jthread_monitor_enter(tts->monitor);

    tts->phase = TT_PHASE_RUNNING;
    tested_thread_started(tts);
    tested_thread_wait_for_stop_request(tts);
    tts->phase =  TT_PHASE_WAITING_ON_MONITOR;
    status = jthread_monitor_enter(
        get_tts(MAX_TESTED_THREAD_NUMBER - (tts->my_index + 1))->monitor);
    tts->phase = status == TM_ERROR_NONE ? TT_PHASE_DEAD : TT_PHASE_ERROR;
    tested_thread_ended(tts);
} // run_for_test_jthread_get_deadlocked_threads

int test_jthread_get_deadlocked_threads(void)
{
    tested_thread_sturct_t * tts;
    jthread *thread_list;
    int dead_list_count;
    jthread *dead_list;
    int count;

    // Initialize tts structures and run all tested threads
    tested_threads_run_with_different_monitors(run_for_test_jthread_get_deadlocked_threads);

    thread_list =
        (jthread*)calloc(MAX_TESTED_THREAD_NUMBER, sizeof(jthread*));
    tf_assert(thread_list && "failed to alloc memory");

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)) {
        thread_list[tts->my_index] = tts->java_thread;
    }
    tf_assert_same(jthread_get_deadlocked_threads(thread_list, MAX_TESTED_THREAD_NUMBER,
                                                  &dead_list, &dead_list_count), TM_ERROR_NONE);
    tf_assert_same(dead_list_count, 0);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)) {
        tested_thread_send_stop_request(tts);
    }

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)) {
        count = 0;
        if ((MAX_TESTED_THREAD_NUMBER % 2)
            && (tts->my_index == MAX_TESTED_THREAD_NUMBER / 2))
        {
            while (hythread_is_alive(tts->native_thread)) {
                // wait until the state is changed
                hythread_sleep(SLEEP_TIME);
                if (tts->phase == TT_PHASE_ERROR || ++count > (MAX_TIME_TO_WAIT/SLEEP_TIME)) {
                    tf_fail("thread failed to change state on WAITING");
                }
            }
            log_info("Thread %d is dead", tts->my_index);
            check_tested_thread_phase(tts, TT_PHASE_DEAD);
            thread_list[tts->my_index] = jthread_self();
        } else {
            while (!hythread_is_blocked_on_monitor_enter(tts->native_thread)) {
                // wait until the state is changed
                hythread_sleep(SLEEP_TIME);
                if (tts->phase == TT_PHASE_ERROR || ++count > (MAX_TIME_TO_WAIT/SLEEP_TIME)) {
                    tf_fail("thread failed to change state on WAITING");
                }
            }
            log_info("Thread %d is blocked on monitor", tts->my_index);
            check_tested_thread_phase(tts, TT_PHASE_WAITING_ON_MONITOR);
        }
    }

    tf_assert_same(jthread_get_deadlocked_threads(thread_list, MAX_TESTED_THREAD_NUMBER,
        &dead_list, &dead_list_count), TM_ERROR_NONE);
    log_info("Deadlocked thread numbre is %d", dead_list_count);
    tf_assert_same(dead_list_count,
        ((MAX_TESTED_THREAD_NUMBER % 2)
        ? MAX_TESTED_THREAD_NUMBER - 1 : MAX_TESTED_THREAD_NUMBER));

    count = 0;
    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)) {
        if ((MAX_TESTED_THREAD_NUMBER % 2)
            && (tts->my_index == MAX_TESTED_THREAD_NUMBER / 2))
        {
            continue;
        } else {
            tf_assert_same(dead_list[count++], tts->java_thread);
            log_info("Thread %d is deadlocked", tts->my_index);
            hythread_set_state(tts->native_thread, TM_THREAD_STATE_TERMINATED);
            hythread_cancel(tts->native_thread);
        }
    }

     // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_get_deadlocked_threads

/**
 * Test jthread_get_waited_count()
 */
void JNICALL run_for_test_jthread_get_waited_count(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args)
{
    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;

    tts->phase = TT_PHASE_RUNNING;
    tested_thread_started(tts);
    // Enter critical section
    status = jthread_monitor_enter(monitor);
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_WAITING_ON_WAIT : TT_PHASE_ERROR);
    // Wait on monitor
    status = jthread_monitor_wait(monitor);
    if (status != TM_ERROR_NONE){
        tts->phase = TT_PHASE_ERROR;
        jthread_monitor_exit(monitor);
        tested_thread_ended(tts);
        return;
    }
    // Exit critical section
    status = jthread_monitor_exit(monitor);

    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_DEAD : TT_PHASE_ERROR);
    tested_thread_ended(tts);
} // run_for_test_jthread_get_waited_count

int test_jthread_get_waited_count(void)
{
    int count;
    int waiting_nmb;
    jobject monitor;
    tested_thread_sturct_t * tts;

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_get_waited_count);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        count = 0;
        while (!hythread_is_waiting(tts->native_thread)) {
            // wait until the state is changed
            hythread_sleep(SLEEP_TIME);
            if (tts->phase == TT_PHASE_ERROR || ++count > (MAX_TIME_TO_WAIT/SLEEP_TIME)) {
                tf_fail("thread failed to change state on WAITING");
            }
        }
        check_tested_thread_phase(tts, TT_PHASE_WAITING_ON_WAIT);
        log_info("Thread %d is waiting.", tts->my_index);
    }

    tf_assert_same(jthread_get_waited_count(&waiting_nmb), TM_ERROR_NONE);
    log_info("Waiting threads count is %d", waiting_nmb);
    tf_assert_same(waiting_nmb, MAX_TESTED_THREAD_NUMBER);

    log_info("Release all threads");
    monitor = get_tts(0)->monitor;
    tf_assert_same(jthread_monitor_enter(monitor), TM_ERROR_NONE);
    tf_assert_same(jthread_monitor_notify_all(monitor), TM_ERROR_NONE);
    tf_assert_same(jthread_monitor_exit(monitor), TM_ERROR_NONE);

    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_get_waited_count

TEST_LIST_START
    TEST(test_jthread_get_all_threads)
    TEST(test_jthread_get_blocked_count)
    TEST(test_jthread_get_deadlocked_threads)
    TEST(test_jthread_get_waited_count)
TEST_LIST_END;
