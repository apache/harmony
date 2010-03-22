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
 * Test jthread_get_java_thread(...)
 */
int test_jthread_get_java_thread(void) {

    tested_thread_sturct_t *tts;
    hythread_t native_thread;

    // Initialize tts structures and run all tested threads
    tested_threads_run(default_run_for_test);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        native_thread = jthread_get_native_thread(tts->java_thread);
        tf_assert_same(jthread_get_java_thread(native_thread)->object, tts->java_thread->object);
    }

    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
}

/*
 * Test jthread_get_native_thread(...)
 */
int test_jthread_get_native_thread(void) {

    tested_thread_sturct_t *tts;
    hythread_t native_thread;

    // Initialize tts structures and run all tested threads
    tested_threads_run(default_run_for_test);

    reset_tested_thread_iterator(&tts);
    while(next_tested_thread(&tts)){
        native_thread = jthread_get_native_thread(tts->java_thread);
        tf_assert_same(jthread_get_java_thread(native_thread)->object, tts->java_thread->object);
    }

    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
}

TEST_LIST_START
    TEST(test_jthread_get_java_thread)
    TEST(test_jthread_get_native_thread)
TEST_LIST_END;
