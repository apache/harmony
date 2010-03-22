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

/*
 * Test test_jthread_get_jvmti_state_1
 *
 *  called function                     tested state
 *
 *  tested_threads_init()               NEW   (state == 0)
 *  hythread_create()                   ALIVE | RUNNABLE
 *  jthread_interrupt()                 ALIVE | RUNNABLE | INTERRUPTED
 *  jthread_clear_interrupted()         ALIVE | RUNNABLE
 *  tested_thread_send_stop_request()   TERMINATED
 */
void JNICALL run_for_test_jthread_get_jvmti_state_1(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;

    tts->phase = TT_PHASE_RUNNING;
    tested_thread_started(tts);
    while(tested_thread_wait_for_stop_request_timed(tts, SLEEP_TIME) == TM_ERROR_TIMEOUT){
        hythread_yield();
    }
    tts->phase = TT_PHASE_DEAD;
    tested_thread_ended(tts);
} // run_for_test_jthread_get_jvmti_state_1

int test_jthread_get_jvmti_state_1(void) {

    tested_thread_sturct_t *tts;
    int state;
    int ref_state;
    int count;

    // Initialize tts structures and run all tested threads
    tested_threads_init(TTS_INIT_COMMON_MONITOR);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        tf_assert_same(jthread_get_jvmti_state(tts->java_thread, &state), TM_ERROR_NONE);
        ref_state = 0;   // it's a new thread
        log_info("thread %d state = %08x (%08x) - NEW", tts->my_index, state, ref_state);
        tf_assert_same(state, ref_state);
    }

    // Run all tested threads
    tested_threads_run_common(run_for_test_jthread_get_jvmti_state_1);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        tested_thread_wait_running(tts);
        tf_assert_same(jthread_get_jvmti_state(tts->java_thread, &state), TM_ERROR_NONE);
        ref_state = JVMTI_THREAD_STATE_RUNNABLE | JVMTI_THREAD_STATE_ALIVE;
        log_info("thread %d state = %08x (%08x) - ALIVE|RUNNABLE", tts->my_index, state, ref_state);
        tf_assert((state & ref_state) != 0 && "thread is not RUNNABLE or ALIVE");

        tf_assert_same(jthread_interrupt(tts->java_thread), TM_ERROR_NONE);
        tf_assert_same(jthread_get_jvmti_state(tts->java_thread, &state), TM_ERROR_NONE);
        ref_state |= JVMTI_THREAD_STATE_INTERRUPTED;
        log_info("thread %d state = %08x (%08x) - ALIVE|RUNNABLE|INTERRUPTED", tts->my_index, state, ref_state);
        tf_assert_same(state, ref_state);

        tf_assert_same(jthread_clear_interrupted(tts->java_thread), TM_ERROR_INTERRUPT);
        tf_assert_same(jthread_get_jvmti_state(tts->java_thread, &state), TM_ERROR_NONE);
        ref_state &= ~JVMTI_THREAD_STATE_INTERRUPTED;
        log_info("thread %d state = %08x (%08x) - ALIVE|RUNNABLE", tts->my_index, state, ref_state);
        tf_assert((state & ref_state) != 0 && "thread is not RUNNABLE or ALIVE");

        tested_thread_send_stop_request(tts);
        tested_thread_wait_ended(tts);
        check_tested_thread_phase(tts, TT_PHASE_DEAD);
        count = 0;
        while (hythread_is_alive(tts->native_thread)) {
            // waiting when the thread goes to terminate state
            hythread_sleep(SLEEP_TIME);
            if (tts->phase == TT_PHASE_ERROR || ++count > (MAX_TIME_TO_WAIT/SLEEP_TIME)) {
                tf_fail("thread failed to change state on TERMINATED");
            }
        }
        tf_assert_same(jthread_get_jvmti_state(tts->java_thread, &state), TM_ERROR_NONE);
        ref_state = JVMTI_THREAD_STATE_TERMINATED;
        log_info("thread %d state = %08x (%08x) - TERMINATED", tts->my_index, state, ref_state);
        tf_assert_same(state, ref_state);
    }

    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_get_jvmti_state_1

/**
 * Test test_jthread_get_jvmti_state_2
 *
 *  called function                     tested state
 *
 *  jthread_monitor_enter()             ALIVE | BLOCKED
 *  jthread_monitor_wait()              ALIVE | WAITING | OBJECT_WAIT | INDEFINITELY
 *  jthread_monitor_notify_all()        ALIVE | RUNNABLE
 */
void JNICALL run_for_test_jthread_get_jvmti_state_2(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;

    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    tested_thread_started(tts);
    status = jthread_monitor_enter(monitor);
    if (status != TM_ERROR_NONE){
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
        tested_thread_ended(tts);
        return;
    }
    // Begin critical section
    tts->phase = TT_PHASE_WAITING_ON_WAIT;
    status = jthread_monitor_wait(monitor);
    if (status != TM_ERROR_NONE){
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
        jthread_monitor_exit(monitor);
        return;
    }
    tts->phase = TT_PHASE_RUNNING;
    // Exit critical section
    status = jthread_monitor_exit(monitor);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
    }
    while(tested_thread_wait_for_stop_request_timed(tts, SLEEP_TIME) == TM_ERROR_TIMEOUT){
        hythread_yield();
    }
    tts->phase = TT_PHASE_DEAD;
    tested_thread_ended(tts);
} // run_for_test_jthread_get_jvmti_state_2

int test_jthread_get_jvmti_state_2(void)
{
    tested_thread_sturct_t *tts;
    jobject monitor;
    int state;
    int ref_state;
    int count;

    // Initialize tts structures and run all tested threads
    tested_threads_init(TTS_INIT_COMMON_MONITOR);

    // Lock monitor
    monitor = get_tts(0)->monitor;
    tf_assert_same(jthread_monitor_enter(monitor), TM_ERROR_NONE);

    // Run all tested threads
    tested_threads_run_common(run_for_test_jthread_get_jvmti_state_2);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        check_tested_thread_phase(tts, TT_PHASE_WAITING_ON_MONITOR);
        count = 0;
        while (!hythread_is_blocked_on_monitor_enter(tts->native_thread)) {
            // wait until the state is changed
            hythread_sleep(SLEEP_TIME);
            if (tts->phase == TT_PHASE_ERROR || ++count > (MAX_TIME_TO_WAIT/SLEEP_TIME)) {
                tf_fail("thread failed to change state on BLOCKED_ON_MONITOR_ENTER");
            }
        }
        tf_assert_same(jthread_get_jvmti_state(tts->java_thread, &state), TM_ERROR_NONE);
        ref_state = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER;
        log_info("thread %d state = %08x (%08x) - ALIVE|BLOCKED", tts->my_index, state, ref_state);
        tf_assert_same(state, ref_state);
    }

    // Release monitor
    tf_assert_same(jthread_monitor_exit(monitor), TM_ERROR_NONE);

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
        tf_assert_same(jthread_get_jvmti_state(tts->java_thread, &state), TM_ERROR_NONE);
        ref_state = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_WAITING_INDEFINITELY
            | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_IN_OBJECT_WAIT;
        log_info("thread %d state = %08x (%08x) - ALIVE|WAITING|OBJECT_WAIT|INDEFINITELY",
            tts->my_index, state, ref_state);
        tf_assert_same(state, ref_state);
    }

    // Notify tested threads
    tf_assert_same(jthread_monitor_enter(monitor), TM_ERROR_NONE);
    tf_assert_same(jthread_monitor_notify_all(monitor), TM_ERROR_NONE);
    tf_assert_same(jthread_monitor_exit(monitor), TM_ERROR_NONE);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        count = 0;
        while (!hythread_is_runnable(tts->native_thread)) {
            // wait until the state is changed
            hythread_sleep(SLEEP_TIME);
            if (tts->phase == TT_PHASE_ERROR || ++count > (MAX_TIME_TO_WAIT/SLEEP_TIME)) {
                tf_fail("thread failed to change state on RUNNABLE");
            }
        }
        tf_assert_same(jthread_monitor_enter(monitor), TM_ERROR_NONE);
        tf_assert_same(jthread_monitor_exit(monitor), TM_ERROR_NONE);
        check_tested_thread_phase(tts, TT_PHASE_RUNNING);
        tf_assert_same(jthread_get_jvmti_state(tts->java_thread, &state), TM_ERROR_NONE);
        ref_state = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_RUNNABLE;
        log_info("thread %d state = %08x (%08x) - ALIVE|RUNNABLE",
            tts->my_index, state, ref_state);
        tf_assert_same(state, ref_state);
    }

    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_get_jvmti_state_2

/*
 * Test test_jthread_get_jvmti_state_3
 *
 *  called function                     tested state
 *
 *  jthread_monitor_wait(m, n)          ALIVE | WAITING | OBJECT_WAIT | WITH_TIMEOUT
 *  tested_thread_ended()               TERMINATED
 */
void JNICALL run_for_test_jthread_get_jvmti_state_3(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;

    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    tested_thread_started(tts);

    // Begin critical section
    status = jthread_monitor_enter(monitor);
    if (status != TM_ERROR_NONE){
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
        tested_thread_ended(tts);
        return;
    }
    tts->phase = TT_PHASE_WAITING_ON_WAIT;
    status = jthread_monitor_timed_wait(monitor, MAX_TIME_TO_WAIT, 0);
    if (status != TM_ERROR_NONE) {
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
} // run_for_test_jthread_get_jvmti_state_3

int test_jthread_get_jvmti_state_3(void) {

    tested_thread_sturct_t *tts;
    int state;
    int ref_state;
    int count;
    jobject monitor;

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_get_jvmti_state_3);

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
        tf_assert_same(jthread_get_jvmti_state(tts->java_thread, &state), TM_ERROR_NONE);
        ref_state = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT
            | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_IN_OBJECT_WAIT;
        log_info("thread %d state = %08x (%08x) - ALIVE|WAITING|OBJECT_WAIT|TIMEOUT",
            tts->my_index, state, ref_state);
        tf_assert_same(state, ref_state);
    }

    // Notify tested threads
    log_info("Notify tested threads to end");
    tf_assert_same(jthread_monitor_enter(monitor), TM_ERROR_NONE);
    tf_assert_same(jthread_monitor_notify_all(monitor), TM_ERROR_NONE);
    tf_assert_same(jthread_monitor_exit(monitor), TM_ERROR_NONE);

    ref_state = JVMTI_THREAD_STATE_TERMINATED;
    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        while (hythread_is_alive(tts->native_thread)) {
            // waiting when the thread goes to terminate state
            hythread_sleep(SLEEP_TIME);
            if (tts->phase == TT_PHASE_ERROR || ++count > (MAX_TIME_TO_WAIT/SLEEP_TIME)) {
                log_info("Test thread phase: %d", tts->phase);
                tf_fail("thread failed to change state on TERMINATED");
            }
        }
        check_tested_thread_phase(tts, TT_PHASE_DEAD);
        tf_assert_same(jthread_get_jvmti_state(tts->java_thread, &state), TM_ERROR_NONE);
        log_info("thread %d state = %08x (%08x) - TERMINATED",
            tts->my_index, state, ref_state);
        tf_assert_same(state, ref_state);
    }

    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_get_jvmti_state_3

/**
 * Test test_jthread_get_jvmti_state_4
 *
 * 1. Run threads
 * 2. Wait 20 times more then 1 thread does
 * 3. Expected TERMINATED state for all threads
 */
void JNICALL run_for_test_jthread_get_jvmti_state_4(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    jobject monitor = tts->monitor;
    IDATA status;

    tts->phase = TT_PHASE_WAITING_ON_MONITOR;
    tested_thread_started(tts);
    status = jthread_monitor_enter(monitor);
    if (status != TM_ERROR_NONE){
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
        tested_thread_ended(tts);
        return;
    }
    // Begin critical section
    tts->phase = TT_PHASE_WAITING_ON_WAIT;
    status = jthread_monitor_timed_wait(monitor, CLICK_TIME_MSEC, 0);
    if (status != TM_ERROR_TIMEOUT){
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
        tested_thread_ended(tts);
        return;
    }
    status = jthread_monitor_exit(monitor);
    // Exit critical section
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
    } else {
        tts->phase = TT_PHASE_DEAD;
    }
    tested_thread_ended(tts);
} // run_for_test_jthread_get_jvmti_state_4

int test_jthread_get_jvmti_state_4(void)
{
    tested_thread_sturct_t *tts;
    int state;
    int ref_state;
    int count;

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_get_jvmti_state_4);

    // Wait for all threads wait timeout
    jthread_sleep(20 * CLICK_TIME_MSEC * MAX_TESTED_THREAD_NUMBER, 0);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        check_tested_thread_phase(tts, TT_PHASE_DEAD);
    }

    ref_state = JVMTI_THREAD_STATE_TERMINATED;
    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        count = 0;
        while (hythread_is_alive(tts->native_thread)) {
            // waiting when the thread goes to terminate state
            hythread_sleep(SLEEP_TIME);
            if (tts->phase == TT_PHASE_ERROR || ++count > (MAX_TIME_TO_WAIT/SLEEP_TIME)) {
                log_info("Test thread phase: %d", tts->phase);
                tf_fail("thread failed to change state on TERMINATED");
            }
        }
        tf_assert_same(jthread_get_jvmti_state(tts->java_thread, &state), TM_ERROR_NONE);
        log_info("thread %d state = %08x (%08x) - TERMINATED",
            tts->my_index, state, ref_state);
        tf_assert_same(state, ref_state);
    }

    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_get_jvmti_state_4

/**
 * Test test_jthread_get_jvmti_state_5
 *
 * 1. Run threads
 * 2. Check sleeping state
 * 3. Interrupt sleep
 * 4. Check TERMINATE state
 */
void JNICALL run_for_test_jthread_get_jvmti_state_5(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    IDATA status;

    tts->phase = TT_PHASE_SLEEPING;
    tested_thread_started(tts);
    status = jthread_sleep(MAX_TIME_TO_WAIT, 0);
    if (status != TM_ERROR_INTERRUPT) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_INTERRUPT);
        tts->phase = TT_PHASE_ERROR;
    } else {
        tts->phase = TT_PHASE_DEAD;
    }
    tested_thread_ended(tts);
} // run_for_test_jthread_get_jvmti_state_5

int test_jthread_get_jvmti_state_5(void)
{
    tested_thread_sturct_t *tts;
    int state;
    int ref_state;
    int count;

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_get_jvmti_state_5);

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
        check_tested_thread_phase(tts, TT_PHASE_SLEEPING);
        tf_assert_same(jthread_get_jvmti_state(tts->java_thread, &state), TM_ERROR_NONE);
        ref_state = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT
            | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_SLEEPING;
        log_info("thread %d state = %08x (%08x) - ALIVE|WAITING|SLEEPING|TIMEOUT",
            tts->my_index, state, ref_state);
        tf_assert_same(state, ref_state);
        tf_assert_same(jthread_interrupt(tts->java_thread), TM_ERROR_NONE);
    }

    ref_state = JVMTI_THREAD_STATE_TERMINATED;
    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        count = 0;
        while (hythread_is_alive(tts->native_thread)) {
            // waiting when the thread goes to terminate state
            hythread_sleep(SLEEP_TIME);
            if (tts->phase == TT_PHASE_ERROR || ++count > (MAX_TIME_TO_WAIT/SLEEP_TIME)) {
                log_info("Test thread phase: %d", tts->phase);
                tf_fail("thread failed to change state on TERMINATED");
            }
        }
        check_tested_thread_phase(tts, TT_PHASE_DEAD);
        tf_assert_same(jthread_get_jvmti_state(tts->java_thread, &state), TM_ERROR_NONE);
        log_info("thread %d state = %08x (%08x) - TERMINATED",
            tts->my_index, state, ref_state);
        tf_assert_same(state, ref_state);
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_get_jvmti_state_5

/**
 * Test test_jthread_get_jvmti_state_6
 *
 * 1. Run threads
 * 2. Wait 20 times more then 1 thread does
 * 3. Expected TERMINATED state for all threads
 */
void JNICALL run_for_test_jthread_get_jvmti_state_6(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    IDATA status;

    tts->phase = TT_PHASE_SLEEPING;
    tested_thread_started(tts);
    status = hythread_sleep(CLICK_TIME_MSEC);
    if (status != TM_ERROR_NONE) {
        log_info("Test status is %d, but expected %d", status, TM_ERROR_NONE);
        tts->phase = TT_PHASE_ERROR;
    } else {
        tts->phase = TT_PHASE_DEAD;
    }
    tested_thread_ended(tts);
} // run_for_test_jthread_get_jvmti_state_6

int test_jthread_get_jvmti_state_6(void)
{
    int count;
    int state;
    int ref_state;
    tested_thread_sturct_t *tts;

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_get_jvmti_state_6);

    // Wait for all threads wait timeout
    jthread_sleep(20 * CLICK_TIME_MSEC * MAX_TESTED_THREAD_NUMBER, 0);

    ref_state = JVMTI_THREAD_STATE_TERMINATED;
    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        check_tested_thread_phase(tts, TT_PHASE_DEAD);
        count = 0;
        while (hythread_is_alive(tts->native_thread)) {
            // waiting when the thread goes to terminate state
            hythread_sleep(SLEEP_TIME);
            if (tts->phase == TT_PHASE_ERROR || ++count > (MAX_TIME_TO_WAIT/SLEEP_TIME)) {
                tf_fail("thread failed to change state on TERMINATED");
            }
        }
        tf_assert_same(jthread_get_jvmti_state(tts->java_thread, &state), TM_ERROR_NONE);
        log_info("thread %d state = %08x (%08x) - TERMINATED",
            tts->my_index, state, ref_state);
        tf_assert_same(state, ref_state);
    }

    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_get_jvmti_state_6

TEST_LIST_START
    TEST(test_jthread_get_jvmti_state_1)
    TEST(test_jthread_get_jvmti_state_2)
    TEST(test_jthread_get_jvmti_state_3)
    TEST(test_jthread_get_jvmti_state_4)
    TEST(test_jthread_get_jvmti_state_5)
    TEST(test_jthread_get_jvmti_state_6)
TEST_LIST_END;
