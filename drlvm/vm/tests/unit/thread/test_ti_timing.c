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

void JNICALL run_for_helper_get_timing(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args)
{
    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    int num = 0;
    jobject monitor = tts->monitor;
    IDATA status;

    tts->phase = TT_PHASE_RUNNING;
    tested_thread_started(tts);
    while(tested_thread_wait_for_stop_request_timed(tts, 1) == TM_ERROR_TIMEOUT) {
        status = jthread_monitor_enter(monitor);
        if (status != TM_ERROR_NONE){
            tts->phase = TT_PHASE_ERROR;
            tested_thread_ended(tts);
            return;
        }
        status = jthread_monitor_timed_wait(monitor, 0, 100);
        if (status != TM_ERROR_TIMEOUT) {
            tts->phase = TT_PHASE_ERROR;
            jthread_monitor_exit(monitor);
            break;
        }
        status = jthread_monitor_exit(monitor);
        if (status != TM_ERROR_NONE) {
            tts->phase = TT_PHASE_ERROR;
            break;
        }
        ++num;
        hythread_yield();
    }
    log_info("Thread %d cycle times: %d", tts->my_index, num);
    tts->phase = TT_PHASE_DEAD;
    tested_thread_ended(tts);
} // run_for_helper_get_timing

int test_jthread_get_timing(void)
{
    tested_thread_sturct_t *tts;
    jlong cpu_time;
    jlong user_cpu_time;
    jlong blocked_time;
    jlong waited_time;

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_helper_get_timing);

    hythread_sleep(MAX_TIME_TO_WAIT/20);
    
    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){

        tf_assert_same(jthread_get_thread_cpu_time(tts->java_thread, &cpu_time), TM_ERROR_NONE);
        tf_assert_same(jthread_get_thread_user_cpu_time(tts->java_thread, &user_cpu_time), TM_ERROR_NONE);
        tf_assert_same(jthread_get_thread_blocked_time(tts->java_thread, &blocked_time), TM_ERROR_NONE);
        tf_assert_same(jthread_get_thread_waited_time(tts->java_thread, &waited_time), TM_ERROR_NONE);
        

        log_info("Thread %d:", tts->my_index);
        if (cpu_time/1000000) {
            log_info("cpu_time = %.1f s", (float)cpu_time/1000000);
        } else if(cpu_time/1000) {
            log_info("cpu_time = %.1f ms", (float)cpu_time/1000);
        } else {
            log_info("cpu_time = %d ns", cpu_time);
        }

        if (user_cpu_time/1000000) {
            log_info("user_cpu_time = %.1f s", (float)user_cpu_time/1000000);
        } else if(user_cpu_time/1000) {
            log_info("user_cpu_time = %.1f ms", (float)user_cpu_time/1000);
        } else {
            log_info("user_cpu_time = %d ns", user_cpu_time);
        }

        if (blocked_time/1000000) {
            log_info("blocked_time = %.1f s", (float)blocked_time/1000000);
        } else if (blocked_time/1000) {
            log_info("blocked_time = %.1f ms", (float)blocked_time/1000);
        } else {
            log_info("blocked_time = %d ns", blocked_time);
        }

        if (waited_time/1000000) {
            log_info("waited_time = %.1f s\n", (float)waited_time/1000000);
        } else if (waited_time/1000) {
            log_info("waited_time = %.1f ms\n", (float)waited_time/1000);
        } else {
            log_info("waited_time = %d ns\n", waited_time);
        }
    }

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)) {
        tested_thread_send_stop_request(tts);
        tested_thread_wait_ended(tts);
        check_tested_thread_phase(tts, TT_PHASE_DEAD);
    }

    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
} // test_jthread_get_timing

TEST_LIST_START
    TEST(test_jthread_get_timing)
TEST_LIST_END;
