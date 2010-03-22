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
#include <open/hythread_ext.h>
#include "testframe.h"
#include "thread_unit_test_utils.h"
#include "thread_manager.h"

int started_thread_count;
int start_proc(void *args);

int test_hythread_thread_suspend(void){
    void **args; 
    hythread_t thread = NULL;
    hythread_thin_monitor_t lock;
    hythread_thin_monitor_t monitor;
    IDATA status;
    int i;

    // create monitors
    status = hythread_thin_monitor_create(&lock);
    tf_assert_same(status, TM_ERROR_NONE);
    status = hythread_thin_monitor_create(&monitor);
    tf_assert_same(status, TM_ERROR_NONE);

    // alloc and set thread start procedure args
    args = (void**)calloc(3, sizeof(void*));
    args[0] = &lock;
    args[1] = &monitor;
    args[2] = 0;

    // create thread
    hythread_suspend_disable();
    status = hythread_thin_monitor_enter(&lock);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_enable();

    status = hythread_create(&thread, 0, 0, 0,
        (hythread_entrypoint_t)start_proc, args);
    tf_assert_same(status, TM_ERROR_NONE);

    // waiting start of tested thread
    hythread_suspend_disable();
    status = hythread_thin_monitor_wait(&lock);
    tf_assert_same(status, TM_ERROR_NONE);

    status = hythread_thin_monitor_exit(&lock);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_enable();

    // suspend tested thread
    status = hythread_suspend_other(thread);
    tf_assert_same(status, TM_ERROR_NONE);

    // notify tested thread
    hythread_suspend_disable();
    status = hythread_thin_monitor_enter(&monitor);
    tf_assert_same(status, TM_ERROR_NONE);
    status = hythread_thin_monitor_notify_all(&monitor);
    tf_assert_same(status, TM_ERROR_NONE);
    status = hythread_thin_monitor_exit(&monitor);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_enable();

    // check tested argument
    for(i = 0; i < 1000; i++) {
        tf_assert_same(args[2], 0);
        hythread_sleep(1);
    }

    // resume thread
    hythread_resume(thread);

    test_thread_join(thread, 1);
    
    tf_assert_same((IDATA)args[2], 1);

    return 0;
}

#define THREAD_COUNT 10

int test_hythread_thread_suspend_all(void)
{
    void **args; 
    hythread_t thread_list[THREAD_COUNT];
    hythread_thin_monitor_t lock;
    hythread_thin_monitor_t monitor;
    IDATA status;
    int i;

    // create monitors
    status = hythread_thin_monitor_create(&monitor);
    tf_assert_same(status, TM_ERROR_NONE);
    status = hythread_thin_monitor_create(&lock);
    tf_assert_same(status, TM_ERROR_NONE);

    // alloc and set thread start procedure args
    args = (void**)calloc(3, sizeof(void*));
    args[0] = &lock;
    args[1] = &monitor;
    args[2] = 0;

    // create threads
    hythread_suspend_disable();
    status = hythread_thin_monitor_enter(&lock);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_enable();

    started_thread_count = 0;
    for(i = 0; i < THREAD_COUNT; i++) {
        thread_list[i] = NULL;
        status = hythread_create(&thread_list[i], 0, 0, 0,
            (hythread_entrypoint_t)start_proc, args);
        tf_assert_same(status, TM_ERROR_NONE);
        log_info("%d thread is started", i + 1);
    } 

    // waiting start of tested thread
    hythread_suspend_disable();
    while (started_thread_count < 10) {
        status = hythread_thin_monitor_wait(&lock);
        tf_assert_same(status, TM_ERROR_NONE);
    }

    status = hythread_thin_monitor_exit(&lock);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_enable();

    // suspend tested thread
    status = hythread_suspend_all(NULL, ((HyThread_public*)hythread_self())->group);
    tf_assert_same(status, TM_ERROR_NONE);
    log_info("all threads are suspended");

    // notify tested threads
    hythread_suspend_disable();
    status = hythread_thin_monitor_enter(&monitor);
    tf_assert_same(status, TM_ERROR_NONE);
    status = hythread_thin_monitor_notify_all(&monitor);
    tf_assert_same(status, TM_ERROR_NONE);
    status = hythread_thin_monitor_exit(&monitor);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_enable();
    log_info("notify all suspended threads");

    // check tested argument
    for(i = 0; i < 1000; i++) {
        tf_assert_same(args[2], 0);
        hythread_sleep(1);
    }

    // resume thread
    status = hythread_resume_all(((HyThread_public*)hythread_self())->group);
    tf_assert_same(status, TM_ERROR_NONE);
    log_info("resume all suspended threads");

    for(i = 0; i < THREAD_COUNT; i++) {
        test_thread_join(thread_list[i], i);
        log_info("%d thread is terminated", i + 1);
    }

    tf_assert_same((IDATA)args[2], THREAD_COUNT);

    return 0;
}

int start_proc(void *args)
{
    hythread_thin_monitor_t *lock_p = (hythread_thin_monitor_t*)((void**)args)[0];
    hythread_thin_monitor_t *monitor_p = (hythread_thin_monitor_t*)((void**)args)[1];
    IDATA *ret =  (IDATA*)&(((void**)args)[2]);
    IDATA status;

    // wait to start
    hythread_suspend_disable();

    status = hythread_thin_monitor_enter(monitor_p);
    if (status != TM_ERROR_NONE) {
        hythread_suspend_enable();
        tf_assert_same(status, TM_ERROR_NONE);
    }

    // notify main thread about thread start
    status = hythread_thin_monitor_enter(lock_p);
    if (status != TM_ERROR_NONE) {
        hythread_suspend_enable();
        tf_assert_same(status, TM_ERROR_NONE);
    }
    started_thread_count++;
    status = hythread_thin_monitor_notify(lock_p);
    if (status != TM_ERROR_NONE) {
        hythread_suspend_enable();
        tf_assert_same(status, TM_ERROR_NONE);
    }
    status = hythread_thin_monitor_exit(lock_p);
    if (status != TM_ERROR_NONE) {
        hythread_suspend_enable();
        tf_assert_same(status, TM_ERROR_NONE);
    }

    // fall to infinite wait
    status = hythread_thin_monitor_wait(monitor_p);
    if (status != TM_ERROR_NONE) {
        hythread_suspend_enable();
        tf_assert_same(status, TM_ERROR_NONE);
    }

    (*ret)++;

    status = hythread_thin_monitor_exit(monitor_p);
    if (status != TM_ERROR_NONE) {
        hythread_suspend_enable();
        tf_assert_same(status, TM_ERROR_NONE);
    }
    hythread_suspend_enable();

    return 0;
}

TEST_LIST_START
    TEST(test_hythread_thread_suspend)
    TEST(test_hythread_thread_suspend_all)
TEST_LIST_END;
