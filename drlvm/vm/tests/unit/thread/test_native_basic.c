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
#include "thread_manager.h"


typedef struct proc_param {
    hythread_group_t group;
    UDATA stacksize;
    jint priority;
    hylatch_t start;
    hylatch_t end;
} proc_param;


static int start_proc(void *args) {
    proc_param *attrs = (proc_param *)args;
    tf_assert_same(hythread_get_priority(hythread_self()), attrs->priority);
    tf_assert_same(hythread_self()->group, attrs->group);
    hylatch_count_down(attrs->end);
    return 0;
} // start_proc

static int start_proc_empty(void *args) {
    proc_param *attrs = (proc_param *)args;
    hylatch_count_down(attrs->start);
    hylatch_wait(attrs->end);
    return 0;
} // start_proc_empty


int test_hythread_self_base(void) {
    hythread_t thread;

    // check that this thread is attached to VM
    tf_assert(thread = hythread_self());

    return 0;
} // test_hythread_self_base

/*
* Test tm_create(..)
*/
int test_hythread_create(void) {
    proc_param *args;
    hythread_t thread;
    IDATA status;

    args = (proc_param*)calloc(1, sizeof(proc_param));
    tf_assert(args && "alloc proc_params failed");

    status = hythread_group_create(&args->group);
    tf_assert(status == TM_ERROR_NONE && "thread group creation failed");

    status = hylatch_create(&args->end, 1);
    tf_assert(status == TM_ERROR_NONE && "latch creation failed");

    args->priority  = 1;

    thread = (hythread_t)calloc(1, hythread_get_struct_size());
    assert(thread);
    status = hythread_create_ex(thread, args->group, 1024000, 1, NULL,
        (hythread_entrypoint_t)start_proc, (void*)args);
    tf_assert(status == TM_ERROR_NONE && "thread creation failed");

    // Wait util tested thread have finished.
    status = hylatch_wait(args->end);
    tf_assert(status == TM_ERROR_NONE && "thread finished failed");

    status = hylatch_destroy(args->end);
    tf_assert(status == TM_ERROR_NONE && "latch destroy failed");

    hythread_sleep(SLEEP_TIME);
    hythread_group_release(args->group);

    free(args);

    return TEST_PASSED;
}

/**
* Waits until count of running threads in specified group
* reaches 'count' or less
*/
static void wait_for_all_treads_are_terminated(hythread_group_t group, int count)
{
    int max_tries = 1000; // Maximum count of iterations

    while (max_tries--)
    {
        int n = 0;
        hythread_t thread;

        hythread_iterator_t iterator = hythread_iterator_create(group);

        while(hythread_iterator_has_next(iterator)) {
            thread = hythread_iterator_next(&iterator);

            if (!hythread_is_terminated(thread))
                ++n;
        }

        hythread_iterator_release(&iterator);

        if (n <= count)
            break;

        hythread_yield();
    }

    // 0.1s to let system threads finish their work
    hythread_sleep(100);
} // wait_for_all_treads_are_terminated

int test_hythread_iterator(void) {
    int i;
    const int n = 100;
    IDATA status;
    proc_param *args;
    hythread_t thread = NULL;
    hythread_iterator_t iterator;

    args = (proc_param*)calloc(1, sizeof(proc_param));
    tf_assert(args && "alloc proc_params failed");

    status = hythread_group_create(&args->group);
    tf_assert(status == TM_ERROR_NONE && "create group failed");

    status = hylatch_create(&args->start, n);
    tf_assert(status == TM_ERROR_NONE && "latch creation failed");

    status = hylatch_create(&args->end, 1);
    tf_assert(status == TM_ERROR_NONE && "latch creation failed");

    for (i = 0; i < n; i++) {
        thread = (hythread_t)calloc(1, hythread_get_struct_size());
        assert(thread);
        status = hythread_create_ex(thread, args->group, 0, 0, NULL,
            (hythread_entrypoint_t)start_proc_empty, (void*)args);
        tf_assert(status == TM_ERROR_NONE && "test thread creation failed");
    }

    // Wait util all threads have started.
    status = hylatch_wait(args->start);
    tf_assert(status == TM_ERROR_NONE && "start waiting failed");

    iterator = hythread_iterator_create(args->group);
    tf_assert(iterator && "interator creation failed");

    printf ("iterator size: %d\n", (int)hythread_iterator_size(iterator));
    tf_assert(hythread_iterator_size(iterator) == n && "iterator size");

    i = 0;
    while(hythread_iterator_has_next(iterator)) {
        i++;
        thread = hythread_iterator_next(&iterator);
        tf_assert(hythread_is_alive(thread) && "thread is not alive!");
    }

    tf_assert(i == n && "wrong number of tested threads");

    status = hythread_iterator_release(&iterator);
    tf_assert(status == TM_ERROR_NONE && "release iterator failed");

    // Notify all threads
    status = hylatch_count_down(args->end);
    tf_assert(status == TM_ERROR_NONE && "all threads notify failed");

    wait_for_all_treads_are_terminated(args->group, i - n);

    status = hylatch_destroy(args->start);
    tf_assert(status == TM_ERROR_NONE && "start latch destroy failed");

    status = hylatch_destroy(args->end);
    tf_assert(status == TM_ERROR_NONE && "end latch destroy failed");

    hythread_sleep(SLEEP_TIME);
    hythread_group_release(args->group);

    free(args);

    return TEST_PASSED;
} // test_hythread_iterator

int test_hythread_iterator_default(void) {
    int i;
    const int n = 100;
    IDATA status;
    proc_param *args;
    hythread_t thread = NULL;
    hythread_iterator_t iterator;

    args = (proc_param*)calloc(1, sizeof(proc_param));
    tf_assert(args && "alloc proc_params failed");

    status = hylatch_create(&args->start, n);
    tf_assert(status == TM_ERROR_NONE && "latch creation failed");

    status = hylatch_create(&args->end, 1);
    tf_assert(status == TM_ERROR_NONE && "latch creation failed");

    for (i = 0; i < n; i++) {
        status = hythread_create(NULL, 0, 0, 0,
            (hythread_entrypoint_t)start_proc_empty, (void*)args);
        tf_assert(status == TM_ERROR_NONE && "test thread creation failed");
    }

    // Wait util all threads have started.
    status = hylatch_wait(args->start);
    tf_assert(status == TM_ERROR_NONE && "start waiting failed");

    iterator = hythread_iterator_create(args->group);
    tf_assert(iterator && "interator creation failed");

    printf ("default group iterator size: %d\n", (int)hythread_iterator_size(iterator));
    tf_assert(hythread_iterator_size(iterator) >= n && "iterator size");

    i = 0;
    while(hythread_iterator_has_next(iterator)) {
        thread = hythread_iterator_next(&iterator);
        if (hythread_is_alive(thread)) {
            i++;
        }
    }
    tf_assert(i >= n && "wrong number of tested threads");

    status = hythread_iterator_release(&iterator);
    tf_assert(status == TM_ERROR_NONE && "release iterator failed");

    // Notify all threads
    status = hylatch_count_down(args->end);
    tf_assert(status == TM_ERROR_NONE && "all threads notify failed");

    wait_for_all_treads_are_terminated(NULL, i - n);

    status = hylatch_destroy(args->start);
    tf_assert(status == TM_ERROR_NONE && "start latch destroy failed");

    status = hylatch_destroy(args->end);
    tf_assert(status == TM_ERROR_NONE && "end latch destroy failed");

    free(args);

    return TEST_PASSED;
} // test_hythread_iterator_default

/*
* Test tm_create(..)
*/
int test_hythread_create_many(void){
    int i;
    const int n = 10;
    IDATA status;
    proc_param *args;
    hythread_t thread;

    args = (proc_param*)calloc(1, sizeof(proc_param));
    tf_assert(args && "alloc proc_params failed");

    status = hythread_group_create(&args->group);
    tf_assert(status == TM_ERROR_NONE && "create group failed");

    status = hylatch_create(&args->start, n);
    tf_assert(status == TM_ERROR_NONE && "latch creation failed");

    status = hylatch_create(&args->end, 1);
    tf_assert(status == TM_ERROR_NONE && "latch creation failed");

    for (i = 0; i < n; i++) {
        thread = (hythread_t)calloc(1, hythread_get_struct_size());
        assert(thread);
        status = hythread_create_ex(thread, args->group, 0, 0, NULL,
            (hythread_entrypoint_t)start_proc_empty, (void*)args);
        tf_assert(status == TM_ERROR_NONE && "test thread creation failed");
    }

    // Wait util all threads have started.
    status = hylatch_wait(args->start);
    tf_assert(status == TM_ERROR_NONE && "start waiting failed");

    // huck to get group_count from hythread_group_t structure
    i = hythread_iterator_size((hythread_iterator_t)thread);
    tf_assert(i == n && "incorrect threads count");

    // Notify all threads
    status = hylatch_count_down(args->end);
    tf_assert(status == TM_ERROR_NONE && "all threads notify failed");

    wait_for_all_treads_are_terminated(args->group, i - n);

    status = hylatch_destroy(args->start);
    tf_assert(status == TM_ERROR_NONE && "start latch destroy failed");

    status = hylatch_destroy(args->end);
    tf_assert(status == TM_ERROR_NONE && "end latch destroy failed");

    hythread_sleep(SLEEP_TIME);
    hythread_group_release(args->group);

    free(args);

    return TEST_PASSED;
} // test_hythread_create_many

TEST_LIST_START
    TEST(test_hythread_self_base)
    TEST(test_hythread_create)
    TEST(test_hythread_create_many)
    TEST(test_hythread_iterator)
    TEST(test_hythread_iterator_default)
TEST_LIST_END;
