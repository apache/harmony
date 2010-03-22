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
#include <malloc.h>
#include <open/hythread_ext.h>
#include <apr_atomic.h>
#include <open/types.h>
#include "port_mutex.h"
#include "thread_manager.h"
#include "testframe.h"
#include "thread_unit_test_utils.h"

#define CHECK_NUMBER 100
#define CHECK_TIME_WAIT 1000

#define RAND() (rand() % 1000)
#define CHECK_RAND(percentage) ((rand() % 100) < (((percentage)+1)))

#define trace(x)            printf(x); fflush(stdout)
#define tf_exp_assert(x)    if (!(x)) { failed = 1; tf_assert(x); }
#define tf_exp_assert_v(x)  if (!(x)) { failed = 1; tf_assert_v(x); }

static IDATA test_native_thread_create(hythread_t new_thread,
    hythread_entrypoint_t func, void *args);
static IDATA test_java_thread_create(jobject* new_thread,
    jvmtiStartFunction func, void *args);
static void test_dump_thread_data(jobject thread);
static void test_target_heap_access();
static void test_requestor_heap_access();
static void JNICALL test_thread_proc(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args);
static IDATA HYTHREAD_PROC test_gc_request_thread_proc(void *args);
static void JNICALL test_java_request_thread_proc(jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *args);
static U_32 test_waste_time(U_32 count);

static hylatch_t wait_threads;
static hylatch_t start;
static osmutex_t gc_lock;
static osmutex_t suspend_lock;
static char stop = 0;
static char failed = 0;
static uint64 cycle_count = 0;
static int hang_count;

int test_function(void)
{
    int index;
    uint64 tmp_cycle;
    char buf[1024];
    IDATA status;
    hythread_t native_threads;
    jobject *java_threads;
    int number;

    srand((unsigned)time(NULL));
    test_init();

    number = 1 + GC_REQUEST_THREAD_COUNT + JAVA_REQUEST_THREAD_COUNT;
    if (GC_REQUEST_THREAD_COUNT) {
        native_threads = (hythread_t)alloca(GC_REQUEST_THREAD_COUNT * sizeof(HyThread));
        memset(native_threads, 0, GC_REQUEST_THREAD_COUNT * sizeof(HyThread));
    }
    java_threads = (jobject*)alloca((1 + JAVA_REQUEST_THREAD_COUNT) * sizeof(jobject*));
    memset(java_threads, 0, (1 + JAVA_REQUEST_THREAD_COUNT) * sizeof(jobject*));

    status = hylatch_create(&wait_threads, number);
    tf_exp_assert(status == TM_ERROR_NONE);
    status = hylatch_create(&start, 1);
    tf_exp_assert(status == TM_ERROR_NONE);
    status = port_mutex_create(&gc_lock, APR_THREAD_MUTEX_NESTED);
    tf_exp_assert(status == TM_ERROR_NONE);

    // start tested thread
    status = test_java_thread_create(&java_threads[0],
        test_thread_proc, NULL);
    tf_exp_assert(status == TM_ERROR_NONE);

    // start gc_request threads
    for (index = 0; index < GC_REQUEST_THREAD_COUNT; index++) {
        status = test_native_thread_create(&native_threads[index],
            test_gc_request_thread_proc, java_threads[0]);
        tf_exp_assert(status == TM_ERROR_NONE);
    }

    // start java_request threads
    for (index = 1; index < JAVA_REQUEST_THREAD_COUNT + 1; index++) {
        status = test_java_thread_create(&java_threads[index],
            test_java_request_thread_proc, java_threads[0]);
        tf_exp_assert(status == TM_ERROR_NONE);
    }

    // Wait util all threads have started.
    status = hylatch_wait(wait_threads);
    tf_exp_assert(status == TM_ERROR_NONE);
    hythread_sleep(100);

    // Start testing
    trace("TEST start!\n");
    status = hylatch_count_down(start);
    tf_exp_assert(status == TM_ERROR_NONE);

    // checkpoints
    tmp_cycle = cycle_count;
    for (index = 0, hang_count = 0; index < CHECK_NUMBER; index++) {
        // wait a bit
        hythread_sleep(CHECK_TIME_WAIT);

        if (tmp_cycle == cycle_count) {
            hang_count++;
            index--;
            if (hang_count > 10) {
                sprintf(buf, "F - %d\nTEST HANGED!\n", index);
                trace(buf);
                trace("Tested thread data:\n");
                test_dump_thread_data(java_threads[0]);
                return TEST_FAILED;
            }
        } else {
            tmp_cycle = cycle_count;
            hang_count = 0;
        }

        if (failed) {
            sprintf(buf, "F - %d\nTEST FAILED!\n", index);
            trace(buf);
            test_dump_thread_data(java_threads[0]);
            return TEST_FAILED;
        } else if (hang_count > 2) {
            trace("?");
        } else if (!hang_count) {
            trace(".");
        }
        if ((index % 10) == 0) {
            trace("\n");
        }
    }
    trace("\n\n");

    // Stop testing, wait util all threads have finished.
    stop = 1;

    // Waiting gc_request threads
    trace("\nWaiting native threads finish...\n");
    for (index = 0; index < GC_REQUEST_THREAD_COUNT; index++) {
        test_thread_join(&native_threads[index], index);
    }
    trace("done.\n");

    // Waiting java_request threads
    trace("Waiting java threads finish...\n");
    for (index = 0; index < JAVA_REQUEST_THREAD_COUNT + 1; index++) {
        test_thread_join(jthread_get_native_thread(java_threads[index]), index);
    }
    trace("done.\n");

    sprintf(buf, "\nTEST PASSED!\ncheck number = %d\n"
        "cycle_count = %" FMT64 "d\n", CHECK_NUMBER, cycle_count);
    trace(buf);
    return TEST_PASSED;
} // test_function

static IDATA test_native_thread_create(hythread_t new_thread,
                                       hythread_entrypoint_t func,
                                       void *args)
{
    IDATA status;

    status = hythread_create_ex((hythread_t)new_thread,
        NULL, 0, 0, NULL, func, args);
    if (status != TM_ERROR_NONE) {
        return status;
    }
    return status;
} // test_thread_create

static IDATA test_java_thread_create(jobject * new_thread,
                                     jvmtiStartFunction func,
                                     void *args)
{
    IDATA status;
    JNIEnv * jni_env;
    struct jthread_start_proc_data start_data = {0};

    jni_env = jthread_get_JNI_env(jthread_self());
    *new_thread = new_jobject_thread(jni_env);

    start_data.proc = func;
    start_data.arg = args;

    status = jthread_create(jni_env, *new_thread, &start_data);
    if (status != TM_ERROR_NONE) {
        return status;
    }
    return status;
} // test_thread_create

static volatile int test_heap_ptr_1;
static volatile int test_heap_ptr_2;

static void test_target_heap_access()
{
    char buf[1024];

    if (test_heap_ptr_1 != test_heap_ptr_2) {
        sprintf(buf, "\n********** FAILED! ************\ntarget: test_heap_ptr_1 = %d, "
            "test_heap_ptr_2 = %d\n********** FAILED! ************\n",
            test_heap_ptr_1, test_heap_ptr_2);
        trace(buf);
        failed = 1;
        return;
    }
    if (test_heap_ptr_1 == 0) {
        test_heap_ptr_1++;
        test_heap_ptr_2++;
    } else {
        test_heap_ptr_1--;
        test_heap_ptr_2--;
    }
} // test_target_heap_access

static void test_requestor_heap_access()
{
    char buf[1024];

    if (test_heap_ptr_1 != test_heap_ptr_2) {
        sprintf(buf, "\n********** FAILED! ************\nrequestor: test_heap_ptr_1 = %d, "
            "test_heap_ptr_2 = %d\n********** FAILED! ************\n",
            test_heap_ptr_1, test_heap_ptr_2);
        trace(buf);
        failed = 1;
        return;
    }
    if (test_heap_ptr_2 == 0) {
        test_heap_ptr_2++;
        test_heap_ptr_1++;
    } else {
        test_heap_ptr_2--;
        test_heap_ptr_1--;
    }
} // test_requestor_heap_access

/**
 *  There is a presentation how the tests works:
 *
 *  ...
 *  disable_count = 0
 *              <--- safe point     condition is var1 == var2
 *  disable_count = 1
 *  var1 = 0
 *  var2 = 0
 *  disable_count = 0
 *              <--- safe point     condition is var1 == var2
 *  disable_count = 1
 *  var1 = 1
 *  var2 = 1
 *  disable_count = 0
 *              <--- safe point     condition is var1 == var2
 *  disable_count = 1
 *  ...
 */
static void JNICALL test_thread_proc(jvmtiEnv * jvmti_env,
                                     JNIEnv * jni_env,
                                     void *args)
{
    U_32 xx = 0;
    IDATA status;
    hythread_t self = hythread_self();

    status = hylatch_count_down(wait_threads);
    tf_exp_assert_v(status == TM_ERROR_NONE);

    trace("Tested thread is started\n");

    status = hylatch_wait(start);
    tf_exp_assert_v(status == TM_ERROR_NONE);

    while (!stop && !failed) {
        test_suspend_disable();
        test_target_heap_access();
        xx += test_waste_time(RAND());

        cycle_count++;

        // BB polling is called about 30% of the time
        if (CHECK_RAND(30) && self->request) {
            test_safe_point();
            test_target_heap_access();
            xx += test_waste_time(RAND());
        }

        test_suspend_enable();
        xx += test_waste_time(RAND());
    }

    trace("Tested thread is finished\n");

    return;
} // test_thread_proc

static IDATA HYTHREAD_PROC test_gc_request_thread_proc(void *args)
{
    char buf[1024];
    uint64 count;
    U_32 xx = 0;
    IDATA status;
    hythread_t test_thread = jthread_get_native_thread((jobject)args);

    // Notify main thread about start
    status = hylatch_count_down(wait_threads);
    tf_exp_assert(status == TM_ERROR_NONE);
    trace("GC request thread is started\n");

    // Wait all thread start
    status = hylatch_wait(start);
    tf_exp_assert(status == TM_ERROR_NONE);

    while (!stop && !failed) {
        status = test_gc_suspend(test_thread);
        tf_exp_assert(status == TM_ERROR_NONE);

        count = cycle_count;

        status = port_mutex_lock(&gc_lock);
        tf_exp_assert(status == TM_ERROR_NONE);

        test_requestor_heap_access();
        tf_exp_assert(status == TM_ERROR_NONE);

        status = port_mutex_unlock(&gc_lock);
        tf_exp_assert(status == TM_ERROR_NONE);

        xx += test_waste_time(RAND());

        // check thread is suspended
        if (count != cycle_count) {
            sprintf(buf, "FAILED: thread is not suspended! %" FMT64 "d != %" FMT64 "d\n",
                count, cycle_count);
            trace(buf);
            failed = 1;
        }

        test_gc_resume(test_thread);
        xx += test_waste_time(RAND());
        if (hang_count) {
            // to avoid hang due to multiple requests.
            hythread_sleep(CHECK_TIME_WAIT * hang_count / 50);
        }
    }

    trace("GC request thread is finished\n");

    return 0;
} // test_gc_request_thread_proc

static void JNICALL test_java_request_thread_proc(jvmtiEnv * jvmti_env,
                                                  JNIEnv * jni_env,
                                                  void *args)
{
    U_32 xx = 0;
    IDATA status;
    jobject test_thread = (jobject)args;

    // Notify main thread about start
    status = hylatch_count_down(wait_threads);
    tf_exp_assert_v(status == TM_ERROR_NONE);
    trace("JAVA request thread is started\n");

    // Wait all thread start
    status = hylatch_wait(start);
    tf_exp_assert_v(status == TM_ERROR_NONE);

    while (!stop && !failed) {
        status = test_java_suspend(test_thread);
        tf_exp_assert_v(status == TM_ERROR_NONE);

        xx += test_waste_time(RAND());

        status = test_java_suspend(test_thread);
        tf_exp_assert_v(status == TM_ERROR_NONE);

        xx += test_waste_time(RAND());

        test_java_resume(test_thread);

        xx += test_waste_time(RAND());

        if (hang_count) {
            // to avoid hang due to multiple requests.
            hythread_sleep(CHECK_TIME_WAIT * hang_count / 50);
        }
    }

    trace("JAVA request thread is finished\n");

    return;
} // test_java_request_thread_proc

// make it static to prevent compiler to optimize
// reading/writing of this variable
static U_32 waste_time_int;

static U_32 test_waste_time(U_32 count)
{
    for (; count; count--) {
        waste_time_int = waste_time_int * rand();
        waste_time_int += rand();
    }

    // yield happend about 10% of the time
    if (CHECK_RAND(10)) {
        hythread_yield();
    }
    return waste_time_int;
} // test_waste_time

static void test_dump_thread_data(jobject java_thread)
{
    char buf[1024];
    vm_thread_t vm_thread = jthread_get_vm_thread_from_java(java_thread);
    hythread_t native_thread = (hythread_t)vm_thread;

    sprintf(buf, "\nvm_thread: %p\n"
        "\tsuspend_flag: %d,\tshould be 0\n"
        "hy_thread: %p\n"
        "\trequest          : %d,\tshould be 0\n"
        "\tsuspend_count    : %d,\tshould be 0\n"
        "\tdisable_count    : %d,\tshould be 0\n"
        "\tstate & SUSPENDED: %x,\tshould be 0\n\n",
        vm_thread,
        vm_thread->suspend_flag,
        native_thread,
        native_thread->request,
        native_thread->suspend_count,
        native_thread->disable_count,
        native_thread->state & TM_THREAD_STATE_SUSPENDED);
    trace(buf);
} // test_dump_thread_data
