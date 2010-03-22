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

static hysem_t mon_enter;

/**
 * Monitor TODO:
 *
 * - Init monitor and not init
 * - jthread_monitor_exit() without jthread_monitor_enter()
 */

/**
 * Test jthread_monitor_try_enter()
 */
void JNICALL run_for_test_jthread_monitor_try_enter(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args)
{
    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;

    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    tested_thread_started(tts);
    // Begin critical section
    status = jthread_monitor_try_enter(monitor);
    while (status == TM_ERROR_EBUSY){
        status = jthread_monitor_try_enter(monitor);
        sleep_a_click();
    }
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
    } else {
        tts->phase = TT_PHASE_IN_CRITICAL_SECTON;
    }
    hysem_set(mon_enter, 1);
    tested_thread_wait_for_stop_request(tts);
    status = jthread_monitor_exit(monitor);
    // End critical section
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
    } else {
        tts->phase = TT_PHASE_DEAD;
    }
    tested_thread_ended(tts);
} // run_for_test_jthread_monitor_try_enter

int test_jthread_monitor_try_enter(void)
{
    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *critical_tts;
    int i;
    int waiting_on_monitor_nmb;

    tf_assert_same(hysem_create(&mon_enter, 0, 1), TM_ERROR_NONE);

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_monitor_try_enter);

    for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++) {
        waiting_on_monitor_nmb = 0;
        critical_tts = NULL;

        tf_assert_same(hysem_wait(mon_enter), TM_ERROR_NONE);

        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)) {
            if (tts->phase == TT_PHASE_IN_CRITICAL_SECTON) {
                // error if two threads in critical section
                tf_assert(critical_tts == NULL);
                critical_tts = tts;
            } else if (tts->phase == TT_PHASE_WAITING_ON_MONITOR) {
                waiting_on_monitor_nmb++;
            }
        }
        tf_assert(critical_tts); // thread in critical section found
        if (MAX_TESTED_THREAD_NUMBER - i != waiting_on_monitor_nmb + 1){
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
} // test_jthread_monitor_try_enter

/**
 * Test jthread_monitor_notify_all()
 * Test jthread_monitor_notify()
 */
void JNICALL run_for_test_jthread_monitor_notify(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args)
{
    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;

    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    status = jthread_monitor_enter(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
        tested_thread_started(tts);
        tested_thread_ended(tts);
        return;
    }
    // Begin critical section
    tts->phase = TT_PHASE_WAITING_ON_WAIT;
    tested_thread_started(tts);
    status = jthread_monitor_wait(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
    } else {
        tts->phase = TT_PHASE_IN_CRITICAL_SECTON;
    }
    hysem_set(mon_enter, 1);
    tested_thread_wait_for_stop_request(tts);
    // Exit critical section
    status = jthread_monitor_exit(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
    } else {
        tts->phase = TT_PHASE_DEAD;
    }
    tested_thread_ended(tts);
} // run_for_test_jthread_monitor_notify

int test_jthread_monitor_notify_all(void)
{
    int i;
    int count;
    int waiting_on_wait_nmb;
    jobject monitor;
    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *critical_tts;

    tf_assert_same(hysem_create(&mon_enter, 0, 1), TM_ERROR_NONE);

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_monitor_notify);

    monitor = get_tts(0)->monitor;

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)) {
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

    log_info("Notify all tested threads");
    tf_assert_same(jthread_monitor_enter(monitor), TM_ERROR_NONE);
    tf_assert_same(jthread_monitor_notify_all(monitor), TM_ERROR_NONE);
    tf_assert_same(jthread_monitor_exit(monitor), TM_ERROR_NONE);

    for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++) {
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
        if (MAX_TESTED_THREAD_NUMBER - i != waiting_on_wait_nmb + 1){
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
} // test_jthread_monitor_notify_all

int test_jthread_monitor_notify(void)
{
    int i;
    int count;
    int waiting_on_wait_nmb;
    jobject monitor;
    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *critical_tts;

    tf_assert_same(hysem_create(&mon_enter, 0, 1), TM_ERROR_NONE);

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_monitor_notify);

    monitor = get_tts(0)->monitor;

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)) {
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

    for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++){
        waiting_on_wait_nmb = 0;
        critical_tts = NULL;

        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)){
            tf_assert(tts->phase != TT_PHASE_IN_CRITICAL_SECTON);
        }

        log_info("Notify monitor");
        tf_assert_same(jthread_monitor_enter(monitor), TM_ERROR_NONE);
        tf_assert_same(jthread_monitor_notify(monitor), TM_ERROR_NONE);
        tf_assert_same(jthread_monitor_exit(monitor), TM_ERROR_NONE);

        tf_assert_same(hysem_wait(mon_enter), TM_ERROR_NONE);

        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)) {
            if (tts->phase == TT_PHASE_IN_CRITICAL_SECTON) {
                // error if two threads in critical section
                tf_assert(critical_tts == NULL);
                critical_tts = tts;
            } else if (tts->phase == TT_PHASE_WAITING_ON_WAIT) {
                waiting_on_wait_nmb++;
            }
        }
        tf_assert(critical_tts); // thread in critical section found
        if (MAX_TESTED_THREAD_NUMBER - i != waiting_on_wait_nmb + 1){
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
} // test_jthread_monitor_notify

/**
 * Test jthread_monitor_wait()
 */
void JNICALL run_for_test_jthread_monitor_wait(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args)
{
    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;

    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    status = jthread_monitor_enter(monitor);
    if (status != TM_ERROR_NONE){
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
        tested_thread_started(tts);
        tested_thread_ended(tts);
        return;
    }
    tts->phase = TT_PHASE_WAITING_ON_WAIT;
    tested_thread_started(tts);
    status = jthread_monitor_wait(monitor);
    status = jthread_monitor_exit(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
    } else {
        tts->phase = TT_PHASE_DEAD;
    }
    tested_thread_ended(tts);
} // run_for_test_jthread_monitor_wait

int test_jthread_monitor_wait(void)
{
    int count;
    tested_thread_sturct_t *tts;
    jobject monitor;

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_monitor_wait);

    monitor = get_tts(0)->monitor;

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

    log_info("Notify all threads");
    tf_assert_same(jthread_monitor_enter(monitor), TM_ERROR_NONE);
    tf_assert_same(jthread_monitor_notify_all(monitor), TM_ERROR_NONE);
    tf_assert_same(jthread_monitor_exit(monitor), TM_ERROR_NONE);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        tested_thread_wait_ended(tts);
        check_tested_thread_phase(tts, TT_PHASE_DEAD);
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
}  // test_jthread_monitor_wait

/*
 * Test jthread_monitor_wait_interrupt()
 */
void JNICALL run_for_test_jthread_monitor_wait_interrupt(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args)
{
    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;

    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    status = jthread_monitor_enter(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
        tested_thread_started(tts);
        tested_thread_ended(tts);
        return;
    }
    tts->phase = TT_PHASE_WAITING_ON_WAIT;
    tested_thread_started(tts);
    status = jthread_monitor_wait(monitor);
    if (status != TM_ERROR_INTERRUPT) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_INTERRUPT);
        tts->phase = TT_PHASE_ERROR;
        jthread_monitor_exit(monitor);
        tested_thread_ended(tts);
        return;
    }
    // Exit critical section
    status = jthread_monitor_exit(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
    } else {
        tts->phase = TT_PHASE_DEAD;
    }
    tested_thread_ended(tts);
} // run_for_test_jthread_monitor_wait_interrupt

int test_jthread_monitor_wait_interrupt(void)
{
    int i;
    int count;
    int waiting_on_wait_nmb;
    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *waiting_tts;

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_monitor_wait_interrupt);

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

    for (i = 0; i < MAX_TESTED_THREAD_NUMBER + 1; i++) {
        waiting_on_wait_nmb = 0;
        waiting_tts = NULL;

        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)){
            if (tts->phase == TT_PHASE_WAITING_ON_WAIT) {
                waiting_tts = tts;
                waiting_on_wait_nmb++;
            } else {
                check_tested_thread_phase(tts, TT_PHASE_DEAD);
            }
        }
        tf_assert_same(MAX_TESTED_THREAD_NUMBER - i, waiting_on_wait_nmb);
        if (waiting_tts) {
            log_info("Interrupt thread %d", waiting_tts->my_index);
            tf_assert_same(jthread_interrupt(waiting_tts->java_thread), TM_ERROR_NONE);
            tested_thread_wait_ended(waiting_tts);
            check_tested_thread_phase(waiting_tts, TT_PHASE_DEAD);
        }
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_monitor_wait_interrupt

/*
 * Test jthread_monitor_timed_wait()
 */
void JNICALL run_for_test_jthread_monitor_timed_wait(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args)
{
    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;

    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    status = jthread_monitor_enter(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
        tested_thread_started(tts);
        tested_thread_ended(tts);
        return;
    }
    tts->phase = TT_PHASE_WAITING_ON_WAIT;
    tested_thread_started(tts);
    status = jthread_monitor_timed_wait(monitor, 10 * CLICK_TIME_MSEC * MAX_TESTED_THREAD_NUMBER, 0);
    if (status != TM_ERROR_NONE){
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
        jthread_monitor_exit(monitor);
        tested_thread_ended(tts);
        return;
    }
    // Exit critical section
    status = jthread_monitor_exit(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
    } else {
        tts->phase = TT_PHASE_DEAD;
    }
    tested_thread_ended(tts);
} // run_for_test_jthread_monitor_timed_wait

int test_jthread_monitor_timed_wait(void)
{
    int count;
    tested_thread_sturct_t *tts;
    jobject monitor;

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_monitor_timed_wait);

    monitor = get_tts(0)->monitor;

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

    log_info("Notify all threads");
    tf_assert_same(jthread_monitor_enter(monitor), TM_ERROR_NONE);
    tf_assert_same(jthread_monitor_notify_all(monitor), TM_ERROR_NONE);
    tf_assert_same(jthread_monitor_exit(monitor), TM_ERROR_NONE);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        tested_thread_wait_ended(tts);
        check_tested_thread_phase(tts, TT_PHASE_DEAD);
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_monitor_timed_wait

/*
 * Test jthread_monitor_timed_wait()
 */
void JNICALL run_for_test_jthread_monitor_timed_wait_timeout(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args)
{
    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;

    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    status = jthread_monitor_enter(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
        tested_thread_started(tts);
        tested_thread_ended(tts);
        return;
    }
    tts->phase = TT_PHASE_WAITING_ON_WAIT;
    tested_thread_started(tts);
    status = jthread_monitor_timed_wait(monitor, 10 * CLICK_TIME_MSEC * MAX_TESTED_THREAD_NUMBER, 0);
    if (status != TM_ERROR_TIMEOUT){
        tts->phase = TT_PHASE_ERROR;
        tested_thread_ended(tts);
        return;
    }
    // Exit critical section
    status = jthread_monitor_exit(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
    } else {
        tts->phase = TT_PHASE_DEAD;
    }
    tested_thread_ended(tts);
} // run_for_test_jthread_monitor_timed_wait_timeout

int test_jthread_monitor_timed_wait_timeout(void)
{
    int count;
    tested_thread_sturct_t *tts;
    jobject monitor;

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_monitor_timed_wait_timeout);

    monitor = get_tts(0)->monitor;

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

    // Wait for all threads wait timeout
    hythread_sleep(20 * CLICK_TIME_MSEC * MAX_TESTED_THREAD_NUMBER);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        //tested_thread_wait_dead(tts);
        check_tested_thread_phase(tts, TT_PHASE_DEAD);
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_monitor_timed_wait_timeout

/*
 * Test jthread_monitor_timed_wait()
 */
void JNICALL run_for_test_jthread_monitor_timed_wait_interrupt(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args)
{
    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;

    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    status = jthread_monitor_enter(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
        tested_thread_started(tts);
        tested_thread_ended(tts);
        return;
    }
    tts->phase = TT_PHASE_WAITING_ON_WAIT;
    tested_thread_started(tts);
    status = jthread_monitor_timed_wait(monitor, 100 * CLICK_TIME_MSEC * MAX_TESTED_THREAD_NUMBER, 0);
    if (status != TM_ERROR_INTERRUPT) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_INTERRUPT);
        tts->phase = TT_PHASE_ERROR;
        jthread_monitor_exit(monitor);
        tested_thread_ended(tts);
        return;
    }
    // Exit critical section
    status = jthread_monitor_exit(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
    } else {
        tts->phase = TT_PHASE_DEAD;
    }
    tested_thread_ended(tts);
} // run_for_test_jthread_monitor_timed_wait_interrupt

int test_jthread_monitor_timed_wait_interrupt(void)
{
    int i;
    int count;
    int waiting_on_wait_nmb;
    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *waiting_tts;

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_monitor_timed_wait_interrupt);

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

    for (i = 0; i < MAX_TESTED_THREAD_NUMBER + 1; i++) {
        waiting_on_wait_nmb = 0;
        waiting_tts = NULL;

        reset_tested_thread_iterator(&tts);
        while(next_tested_thread(&tts)){
            if (tts->phase == TT_PHASE_WAITING_ON_WAIT) {
                waiting_tts = tts;
                waiting_on_wait_nmb++;
            } else {
                check_tested_thread_phase(tts, TT_PHASE_DEAD);
            }
        }
        tf_assert_same(MAX_TESTED_THREAD_NUMBER - i, waiting_on_wait_nmb);
        if (waiting_tts) {
            log_info("Interrupt thread %d", waiting_tts->my_index);
            tf_assert_same(jthread_interrupt(waiting_tts->java_thread), TM_ERROR_NONE);
            tested_thread_wait_ended(waiting_tts);
            check_tested_thread_phase(waiting_tts, TT_PHASE_DEAD);
        }
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_monitor_timed_wait_interrupt

/**
 * Test jthread_monitor_enter()
 * Test jthread_monitor_exit()
 */
void JNICALL run_for_test_jthread_monitor_enter_exit(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args)
{
    IDATA status;
    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;

    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    tested_thread_started(tts);
    // Begin critical section
    status = jthread_monitor_enter(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
    } else {
        tts->phase = TT_PHASE_IN_CRITICAL_SECTON;
    }
    hysem_set(mon_enter, 1);
    tested_thread_wait_for_stop_request(tts);
    // End critical section
    status = jthread_monitor_exit(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
    } else {
        tts->phase = TT_PHASE_DEAD;
    }
    tested_thread_ended(tts);
} // run_for_test_jthread_monitor_enter_exit

int test_jthread_monitor_enter_exit(void)
{
    int i;
    int waiting_on_monitor_nmb;
    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *critical_tts;

    tf_assert_same(hysem_create(&mon_enter, 0, 1), TM_ERROR_NONE);

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_monitor_enter_exit);

    for (i = 0; i < MAX_TESTED_THREAD_NUMBER; i++) {
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
        if (MAX_TESTED_THREAD_NUMBER - i != waiting_on_monitor_nmb + 1){
            tf_fail("Wrong number waiting on monitor threads");
        }
        log_info("Thread %d grabbed the monitor", critical_tts->my_index);
        tested_thread_send_stop_request(critical_tts);
        tested_thread_wait_ended(critical_tts);
        check_tested_thread_phase(critical_tts, TT_PHASE_DEAD);
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    tf_assert_same(hysem_destroy(mon_enter), TM_ERROR_NONE);

    return TEST_PASSED;
} // test_jthread_monitor_enter_exit

TEST_LIST_START
    TEST(test_jthread_monitor_enter_exit)
    TEST(test_jthread_monitor_try_enter)
    TEST(test_jthread_monitor_notify)
    TEST(test_jthread_monitor_notify_all)
    TEST(test_jthread_monitor_wait)
    TEST(test_jthread_monitor_wait_interrupt)
    TEST(test_jthread_monitor_timed_wait)
    TEST(test_jthread_monitor_timed_wait_timeout)
    TEST(test_jthread_monitor_timed_wait_interrupt)
TEST_LIST_END;
