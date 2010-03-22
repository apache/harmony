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
 * Raw monitor TODO:
 *
 * - Init raw monitor and not init
 * - jthread_raw_monitor_exit() without jthread_raw_monitor_enter()
 */

/**
 * Test jthread_raw_monitor_create()
 * Test jthread_raw_monitor_destroy()
 */
int test_jthread_raw_monitor_create_destroy(void)
{
    IDATA status;
    jrawMonitorID raw_monitor;

    status = jthread_raw_monitor_create(&raw_monitor);
    if (status != TM_ERROR_NONE) {
        return TEST_FAILED;
    }
    status = jthread_raw_monitor_destroy(raw_monitor);
    if (status != TM_ERROR_NONE) {
        return TEST_FAILED;
    }
    return TEST_PASSED;
} // test_jthread_raw_monitor_create_destroy

/**
 * Test jthread_raw_monitor_enter()
 * Test jthread_raw_monitor_exit()
 */
void JNICALL run_for_test_jthread_raw_monitor_enter_exit(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jrawMonitorID monitor = tts->raw_monitor;
    IDATA status;

    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    tested_thread_started(tts);
    status = jthread_raw_monitor_enter(monitor);

    // Begin critical section
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_IN_CRITICAL_SECTON : TT_PHASE_ERROR);
    hysem_set(mon_enter, 1);
    tested_thread_wait_for_stop_request(tts);
    status = jthread_raw_monitor_exit(monitor);
    // End critical section
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_DEAD : TT_PHASE_ERROR);
    tested_thread_ended(tts);
} // run_for_test_jthread_raw_monitor_enter_exit

int test_jthread_raw_monitor_enter_exit(void) {

    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *critical_tts;
    int i;
    int waiting_on_monitor_nmb;

    tf_assert_same(hysem_create(&mon_enter, 0, 1), TM_ERROR_NONE);

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_raw_monitor_enter_exit);

    for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++) {
        waiting_on_monitor_nmb = 0;
        critical_tts = NULL;

        tf_assert_same(hysem_wait(mon_enter), TM_ERROR_NONE);

        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)){
            if (tts->phase == TT_PHASE_IN_CRITICAL_SECTON) {
                tf_assert(critical_tts == NULL); // error if two threads in critical section
                critical_tts = tts;
            } else if (tts->phase == TT_PHASE_WAITING_ON_MONITOR) {
                waiting_on_monitor_nmb++;
            }
        }
        tf_assert(critical_tts); // thread in critical section found
        if (MAX_TESTED_THREAD_NUMBER - waiting_on_monitor_nmb - i != 1) {
            tf_fail("Wrong number waiting on monitor threads");
        }
        log_info("Thread %d grabbed monitor", critical_tts->my_index);
        tested_thread_send_stop_request(critical_tts);
        tested_thread_wait_ended(critical_tts);
        check_tested_thread_phase(critical_tts, TT_PHASE_DEAD);
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    tf_assert_same(hysem_destroy(mon_enter), TM_ERROR_NONE);

    return TEST_PASSED;
} // test_jthread_raw_monitor_enter_exit

/**
 * Test jthread_raw_wait()
 * Test jthread_raw_notify()
 */
void JNICALL run_for_test_jthread_raw_wait_notify(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jrawMonitorID monitor = tts->raw_monitor;
    IDATA status;
    int64 msec = 1000000;

    status = jthread_raw_monitor_enter(monitor);
    if (status != TM_ERROR_NONE){
        tts->phase = TT_PHASE_ERROR;
        tested_thread_ended(tts);
        return;
    }
    // Begin critical section
    tts->phase = TT_PHASE_WAITING_ON_WAIT;
    tested_thread_started(tts);
    status = jthread_raw_monitor_wait(monitor, msec);
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_IN_CRITICAL_SECTON : TT_PHASE_ERROR);
    hysem_set(mon_enter, 1);
    tested_thread_wait_for_stop_request(tts);
    status = jthread_raw_monitor_exit(monitor);
    // End critical section
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_DEAD : TT_PHASE_ERROR);
    tested_thread_ended(tts);
} // run_for_test_jthread_raw_wait_notify

int test_jthread_raw_notify(void)
{
    int i;
    int count;
    int waiting_on_wait_nmb;
    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *critical_tts;
    jrawMonitorID monitor;

    tf_assert_same(hysem_create(&mon_enter, 0, 1), TM_ERROR_NONE);

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_raw_wait_notify);

    monitor = get_tts(0)->raw_monitor;

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        count = 0;
        while (tts->phase != TT_PHASE_WAITING_ON_WAIT) {
            // wait until the state is changed
            hythread_sleep(SLEEP_TIME);
            if (tts->phase == TT_PHASE_ERROR || ++count > (MAX_TIME_TO_WAIT/SLEEP_TIME)) {
                tf_fail("thread failed to change state on WAITING");
            }
        }
        check_tested_thread_phase(tts, TT_PHASE_WAITING_ON_WAIT);
        log_info("Thread %d is waiting.", tts->my_index);
    }

    for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++) {
        waiting_on_wait_nmb = 0;
        critical_tts = NULL;

        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)){
            tf_assert(tts->phase != TT_PHASE_IN_CRITICAL_SECTON);
        }
        log_info("Notify tested threads");
        tf_assert_same(jthread_raw_monitor_enter(monitor), TM_ERROR_NONE);
        tf_assert_same(jthread_raw_monitor_notify(monitor), TM_ERROR_NONE);
        tf_assert_same(jthread_raw_monitor_exit(monitor), TM_ERROR_NONE);

        tf_assert_same(hysem_wait(mon_enter), TM_ERROR_NONE);

        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)){
            if (tts->phase == TT_PHASE_IN_CRITICAL_SECTON){
                tf_assert(critical_tts == NULL); // error if two threads in critical section
                critical_tts = tts;
            } else if (tts->phase == TT_PHASE_WAITING_ON_WAIT){
                waiting_on_wait_nmb++;
            }
        }
        tf_assert(critical_tts); // thread in critical section found
        if (MAX_TESTED_THREAD_NUMBER - waiting_on_wait_nmb - i != 1){
            tf_fail("Wrong number waiting on monitor threads");
        }
        log_info("Thread %d was notified", critical_tts->my_index);
        tested_thread_send_stop_request(critical_tts);
        tested_thread_wait_ended(critical_tts);
        check_tested_thread_phase(critical_tts, TT_PHASE_DEAD);
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    tf_assert_same(hysem_destroy(mon_enter), TM_ERROR_NONE);

    return TEST_PASSED;
} // test_jthread_raw_notify

/**
 * Test jthread_raw_wait()
 * Test jthread_raw_notify_all()
 */
int test_jthread_raw_notify_all(void)
{
    int i;
    int count;
    int waiting_on_wait_nmb;
    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *critical_tts;
    jrawMonitorID monitor;

    tf_assert_same(hysem_create(&mon_enter, 0, 1), TM_ERROR_NONE);

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_raw_wait_notify);

    monitor = get_tts(0)->raw_monitor;

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        count = 0;
        while (tts->phase != TT_PHASE_WAITING_ON_WAIT) {
            // wait until the state is changed
            hythread_sleep(SLEEP_TIME);
            if (tts->phase == TT_PHASE_ERROR || ++count > (MAX_TIME_TO_WAIT/SLEEP_TIME)) {
                tf_fail("thread failed to change state on WAITING");
            }
        }
        check_tested_thread_phase(tts, TT_PHASE_WAITING_ON_WAIT);
        log_info("Thread %d is waiting.", tts->my_index);
    }

    log_info("Notify all tested threads");
    tf_assert_same(jthread_raw_monitor_enter(monitor), TM_ERROR_NONE);
    tf_assert_same(jthread_raw_monitor_notify_all(monitor), TM_ERROR_NONE);
    tf_assert_same(jthread_raw_monitor_exit(monitor), TM_ERROR_NONE);

    for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++){

        waiting_on_wait_nmb = 0;
        critical_tts = NULL;

        tf_assert_same(hysem_wait(mon_enter), TM_ERROR_NONE);

        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)){
            if (tts->phase == TT_PHASE_IN_CRITICAL_SECTON){
                tf_assert(critical_tts == NULL); // error if two threads in critical section
                critical_tts = tts;
            } else if (tts->phase == TT_PHASE_WAITING_ON_WAIT){
                waiting_on_wait_nmb++;
            }
        }
        tf_assert(critical_tts); // thread in critical section found
        if (MAX_TESTED_THREAD_NUMBER - waiting_on_wait_nmb - i != 1){
            tf_fail("Wrong number waiting on monitor threads");
        }
        log_info("Thread %d was notified", critical_tts->my_index);
        tested_thread_send_stop_request(critical_tts);
        tested_thread_wait_ended(critical_tts);
        check_tested_thread_phase(critical_tts, TT_PHASE_DEAD);
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    tf_assert_same(hysem_destroy(mon_enter), TM_ERROR_NONE);

    return TEST_PASSED;
} // test_jthread_raw_notify_all

void JNICALL run_for_test_jthread_raw_monitor_try_enter(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args)
{
    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jrawMonitorID monitor = tts->raw_monitor;
    IDATA status;

    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    tested_thread_started(tts);
    status = jthread_raw_monitor_try_enter(monitor);
    while (status == TM_ERROR_EBUSY){
        status = jthread_raw_monitor_try_enter(monitor);
        sleep_a_click();
    }
    // Begin critical section
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_IN_CRITICAL_SECTON : TT_PHASE_ERROR);
    hysem_set(mon_enter, 1);
    tested_thread_wait_for_stop_request(tts);
    status = jthread_raw_monitor_exit(monitor);
    // End critical section
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_DEAD : TT_PHASE_ERROR);
    tested_thread_ended(tts);
} // run_for_test_jthread_raw_monitor_try_enter

int test_jthread_raw_monitor_try_enter(void)
{
    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *critical_tts;
    int i;
    int waiting_on_monitor_nmb;

    tf_assert_same(hysem_create(&mon_enter, 0, 1), TM_ERROR_NONE);

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_raw_monitor_try_enter);

    for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++){

        waiting_on_monitor_nmb = 0;
        critical_tts = NULL;

        tf_assert_same(hysem_wait(mon_enter), TM_ERROR_NONE);

        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)){
            if (tts->phase == TT_PHASE_IN_CRITICAL_SECTON){
                tf_assert(critical_tts == NULL); // error if two threads in critical section
                critical_tts = tts;
            } else if (tts->phase == TT_PHASE_WAITING_ON_MONITOR){
                waiting_on_monitor_nmb++;
            }
        }
        tf_assert(critical_tts); // thread in critical section found
        if (MAX_TESTED_THREAD_NUMBER - waiting_on_monitor_nmb - i != 1){
            tf_fail("Wrong number waiting on monitor threads");
        }
        log_info("Thread %d grabbed monitor", critical_tts->my_index);
        tested_thread_send_stop_request(critical_tts);
        tested_thread_wait_ended(critical_tts);
        check_tested_thread_phase(critical_tts, TT_PHASE_DEAD);
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    tf_assert_same(hysem_destroy(mon_enter), TM_ERROR_NONE);

    return TEST_PASSED;
} // test_jthread_raw_monitor_try_enter

TEST_LIST_START
    TEST(test_jthread_raw_monitor_create_destroy)
    TEST(test_jthread_raw_monitor_enter_exit)
    TEST(test_jthread_raw_monitor_try_enter)
    TEST(test_jthread_raw_notify)
    TEST(test_jthread_raw_notify_all)
TEST_LIST_END;
