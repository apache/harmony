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

#include "ti_thread.h"
#include "testframe.h"
#include "thread_unit_test_utils.h"

#define TEST_TLS_KEY (TM_THREAD_QUANTITY_OF_PREDEFINED_TLS_KEYS + 1)

/*
 * Test test_jthread_set_and_get_local_storage
 */
int test_jthread_set_and_get_local_storage(void)
{
    void * data;
    hythread_t hythread;
    tested_thread_sturct_t *tts;

    // Initialize tts structures and run all tested threads
    tested_threads_run(default_run_for_test);
    
    reset_tested_thread_iterator(&tts);
    while (next_tested_thread(&tts)) {
        hythread = jthread_get_native_thread(tts->java_thread);
        tf_assert(hythread);
        tf_assert_same(hythread_tls_set(hythread, TEST_TLS_KEY, tts),
                       TM_ERROR_NONE);
    }
    reset_tested_thread_iterator(&tts);
    while (next_tested_thread(&tts)) {
        hythread = jthread_get_native_thread(tts->java_thread);
        tf_assert(hythread);
        data = hythread_tls_get(hythread, TEST_TLS_KEY);
        tf_assert_same(data, tts);
    }

    // Terminate all threads and clear tts structures
    tested_threads_destroy();

    return TEST_PASSED;
}

TEST_LIST_START
    TEST(test_jthread_set_and_get_local_storage)
TEST_LIST_END;
