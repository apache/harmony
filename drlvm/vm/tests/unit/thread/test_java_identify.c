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
#include "thread_manager.h"


/*
 * Test jthread_self(...)
 */
void JNICALL run_for_test_jthread_self(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args){

    tested_thread_sturct_t * tts = (tested_thread_sturct_t *) args;
    
    tts->phase = TT_PHASE_RUNNING;
    tested_thread_started(tts);
    tested_thread_wait_for_stop_request(tts);
    tts->phase = jthread_self()->object == tts->java_thread->object ? TT_PHASE_DEAD : TT_PHASE_ERROR;
    tested_thread_ended(tts);
}

int test_jthread_self(void) {

    tested_thread_sturct_t *tts;

    // Initialize tts structures and run all tested threads
    tested_threads_run(run_for_test_jthread_self);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        tested_thread_send_stop_request(tts);
        tested_thread_wait_ended(tts);
        check_tested_thread_phase(tts, TT_PHASE_DEAD);
    }
    // Terminate all threads (not needed here) and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
}

/*
 * Test jthread_get_id(...)
 */
int test_jthread_get_id(void) {

    tested_thread_sturct_t *tts;
    jlong id;

    // Initialize tts structures and run all tested threads
    tested_threads_run(default_run_for_test);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        id = jthread_get_id(tts->java_thread);
        tf_assert_same(jthread_get_thread(id)->object, tts->java_thread->object);
        //tf_assert_same(hythread_detach(tts->java_thread), TM_ERROR_NONE);
        //tf_assert(jthread_get_id(tts->java_thread) == 0);
    }
    // Terminate all threads (not needed here) and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
}
/*
 * Test jthread_get_thread(...)
 */
int test_jthread_get_thread(void) {

    return test_jthread_get_id();
}

TEST_LIST_START
    TEST(test_jthread_self)
    TEST(test_jthread_get_id)
    TEST(test_jthread_get_thread)
TEST_LIST_END;
