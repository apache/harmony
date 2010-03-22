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

/*
 * Test jthread_suspend(...)
 * Test jthread_resume(...)
  */
void JNICALL run_for_test_jthread_suspend_resume(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    IDATA status;
    
    tts->phase = TT_PHASE_RUNNING;
    tested_thread_started(tts);    
    do {
        hythread_safe_point();
        status = tested_thread_wait_for_stop_request_timed(tts, SLEEP_TIME);
        hythread_suspend_disable();
        hythread_suspend_enable();
    } while (status == TM_ERROR_TIMEOUT);
    tts->phase = status == TM_ERROR_NONE ? TT_PHASE_DEAD : TT_PHASE_ERROR;
    tested_thread_ended(tts);
}

int test_jthread_suspend_resume(void) {

    tested_thread_sturct_t *tts;
    tested_thread_sturct_t *switch_tts;

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_suspend_resume);

    switch_tts = get_tts(0);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        tested_thread_wait_running(tts);
    }

    tf_assert_same((jthread_suspend(switch_tts->java_thread)), TM_ERROR_NONE);
    
    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        tested_thread_send_stop_request(tts);
    }
 
    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        if (tts != switch_tts) {
            tested_thread_wait_ended(tts);
            check_tested_thread_phase(tts, TT_PHASE_DEAD);
        } else {
            check_tested_thread_phase(tts, TT_PHASE_RUNNING);
        }
    }

    tf_assert_same(jthread_resume(switch_tts->java_thread), TM_ERROR_NONE);
    tested_thread_wait_ended(switch_tts);
    check_tested_thread_phase(switch_tts, TT_PHASE_DEAD);

    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
}
/*
 * Test jthread_suspend_all(...)
 * Test jthread_resume_all(...)
 */
int test_jthread_suspend_all_resume_all(void) {

    tested_thread_sturct_t * tts;
    jthread all_threads[MAX_TESTED_THREAD_NUMBER];
    jvmtiError results[MAX_TESTED_THREAD_NUMBER];
    int i = 0;

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_suspend_resume);

    // Test that all threads are running
    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        tested_thread_wait_running(tts);
        all_threads[i] = tts->java_thread;
        results[i] = (jvmtiError)(TM_ERROR_NONE + 1);
        i++;
    }
    tf_assert_same(jthread_suspend_all(results, MAX_TESTED_THREAD_NUMBER, all_threads), TM_ERROR_NONE);
    // Test that all threads are suspended
    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        tested_thread_send_stop_request(tts);
    }
    i = 0;
    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        check_tested_thread_phase(tts, TT_PHASE_RUNNING);
        tf_assert_same(results[i], TM_ERROR_NONE);
        results[i] = (jvmtiError)(TM_ERROR_NONE + 1);
        i++;
    }
    tf_assert_same(jthread_resume_all(results, MAX_TESTED_THREAD_NUMBER, all_threads), TM_ERROR_NONE);
    // Test that all threads are running
    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        tested_thread_wait_ended(tts);
        check_tested_thread_phase(tts, TT_PHASE_DEAD);        
    }
    // Terminate all threads and clear tts structures
    tested_threads_destroy();
    return TEST_PASSED;
}

TEST_LIST_START
    TEST(test_jthread_suspend_resume)
    TEST(test_jthread_suspend_all_resume_all)
TEST_LIST_END;
