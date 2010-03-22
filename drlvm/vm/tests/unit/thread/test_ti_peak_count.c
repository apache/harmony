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

/**
 * Test jthread_get_peak_thread_count()
 * Test jthread_reset_peak_thread_count()
 */
void JNICALL run_for_test_get_reset_peak_count(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args)
{

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    IDATA status;
    int num = 0;
    
    tts->peak_count = 0;
    tts->phase = TT_PHASE_RUNNING;
    tested_thread_started(tts);
    while(tested_thread_wait_for_stop_request_timed(tts, SLEEP_TIME) == TM_ERROR_TIMEOUT) {
        ++num;
    }
    status = jthread_get_peak_thread_count(&tts->peak_count);
    tts->phase = (status == TM_ERROR_NONE ? TT_PHASE_DEAD : TT_PHASE_ERROR);
    tested_thread_ended(tts);
} // run_for_test_get_reset_peak_count

int test_get_get_reset_peak_count(void)
{
    jint peak_count;
    tested_thread_sturct_t *tts;

    tf_assert_same(jthread_reset_peak_thread_count(), TM_ERROR_NONE);
    tf_assert_same(jthread_get_peak_thread_count(&peak_count), TM_ERROR_NONE);
    log_info("Initial peak_count = %d", peak_count);

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_get_reset_peak_count);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)) {
        while(tts->phase != TT_PHASE_RUNNING) {
            hythread_sleep(SLEEP_TIME);
        }
        check_tested_thread_phase(tts, TT_PHASE_RUNNING);
        tested_thread_send_stop_request(tts);
        tested_thread_wait_ended(tts);
        check_tested_thread_phase(tts, TT_PHASE_DEAD);
        log_info("Thread %d peak_count = %d", tts->my_index, tts->peak_count);
        tf_assert(tts->peak_count > peak_count);
        tf_assert_same(jthread_reset_peak_thread_count(), TM_ERROR_NONE);
    }

    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_get_get_reset_peak_count

TEST_LIST_START
    TEST(test_get_get_reset_peak_count)
TEST_LIST_END;
