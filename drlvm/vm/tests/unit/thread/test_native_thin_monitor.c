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

int start_proc(void *);
/*
 * Test hythread_thin_monitor_try_enter
 */
int test_hythread_thin_monitor_try_enter(void){
    IDATA status;
    hythread_thin_monitor_t lockword_ptr;
    status = hythread_thin_monitor_create(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_disable();
    status = hythread_thin_monitor_try_enter(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);

    status = hythread_thin_monitor_exit(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_enable();
    return 0;
}

/*
 * Test hythread_thin_monitor_enter
 */
int test_hythread_thin_monitor_enter(void){
    IDATA status;
    hythread_thin_monitor_t lockword_ptr;
    status = hythread_thin_monitor_create(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_disable();
    status = hythread_thin_monitor_enter(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);

    status = hythread_thin_monitor_exit(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_enable();
    return 0;
}

/*
 * Test hythread_thin_monitor timed wait timeout
 */
int test_hythread_thin_monitor_wait_timed(void){
    IDATA status;
    hythread_thin_monitor_t lockword_ptr;
    status = hythread_thin_monitor_create(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_disable();

    status = hythread_thin_monitor_enter(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);

    status = hythread_thin_monitor_wait_timed(&lockword_ptr, 1000,100);
    tf_assert_same(status, TM_ERROR_TIMEOUT);
    hythread_suspend_enable();

    return 0;
}

/*
 * Test hythread_thin_monitor timed wait returns TM_ERROR_ILLEGAL_STATE
 */
int test_hythread_thin_monitor_wait_timed_illegal(void){
    IDATA status;
    hythread_thin_monitor_t lockword_ptr;
    status = hythread_thin_monitor_create(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);

    status = hythread_thin_monitor_wait_timed(&lockword_ptr, 1000,100);
    tf_assert_same(status, TM_ERROR_ILLEGAL_STATE);

    return 0;
}
/*
 * Test hythread_thin_monitor fat unlock
 */
int test_hythread_thin_monitor_fat_unlock(void){
    IDATA status;
    hythread_thin_monitor_t lockword_ptr;
    status = hythread_thin_monitor_create(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_disable();

    status = hythread_thin_monitor_enter(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);

    status = hythread_thin_monitor_wait_timed(&lockword_ptr, 1000,100);
    printf("status: %d\n", (int)status);
    tf_assert_same(status, TM_ERROR_TIMEOUT);

    status = hythread_thin_monitor_exit(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_enable();
    return 0;
}

int start_proc(void *args);
int test_hythread_thin_monitor_enter_contended(void){
    void **args; 
    hythread_t thread = NULL;
    hythread_thin_monitor_t lockword_ptr;
    IDATA status;
    int i;

    args = (void**)calloc(3, sizeof(void *));
    
    status = hythread_thin_monitor_create(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_disable();

    status = hythread_thin_monitor_enter(&lockword_ptr);
    hythread_suspend_enable();
    tf_assert_same(status, TM_ERROR_NONE);

    args[0] = &lockword_ptr;
    args[1] = 0;
    status = hythread_create(&thread, 0, 0, 0,
        (hythread_entrypoint_t)start_proc, args);
    tf_assert_same(status, TM_ERROR_NONE);
    for(i = 0; i < 100000; i++) {
        tf_assert_same(args[1], 0);
    }
    hythread_suspend_disable();
    status = hythread_thin_monitor_exit(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);
    hythread_suspend_enable();
    test_thread_join(thread, 1);
    
    tf_assert_same((IDATA)args[1], 1);
    hythread_suspend_disable();
    status = hythread_thin_monitor_enter(&lockword_ptr);
    tf_assert_same(status, TM_ERROR_NONE);
    args[1] = 0;
    hythread_suspend_enable();    
    status = hythread_create_ex(thread, NULL, 0, 0, NULL,
        (hythread_entrypoint_t)start_proc, args);
    tf_assert_same(status, TM_ERROR_NONE);
    for(i = 0; i < 100000; i++) {
        tf_assert_same(args[1], 0);
    }
    hythread_suspend_disable();    
    status = hythread_thin_monitor_exit(&lockword_ptr);
    hythread_suspend_enable();
    tf_assert_same(status, TM_ERROR_NONE);

    test_thread_join(thread, 1);
    
    tf_assert_same((IDATA)args[1], 1);

    return 0;
}

int start_proc(void *args) {
    hythread_thin_monitor_t *lockword_ptr = (hythread_thin_monitor_t*)((void**)args)[0];
    IDATA *ret =  (IDATA*)args+1;
    IDATA status;
    hythread_suspend_disable();    
    status = hythread_thin_monitor_enter(lockword_ptr);
    hythread_suspend_enable();
    tf_assert_same(status, TM_ERROR_NONE);
    *ret =1;
    hythread_suspend_disable();
    status = hythread_thin_monitor_exit(lockword_ptr);
    hythread_suspend_enable();
    tf_assert_same(status, TM_ERROR_NONE);
    return 0;
}

TEST_LIST_START
    TEST(test_hythread_thin_monitor_enter)
    TEST(test_hythread_thin_monitor_try_enter)
    TEST(test_hythread_thin_monitor_wait_timed_illegal)
    TEST(test_hythread_thin_monitor_wait_timed)
    TEST(test_hythread_thin_monitor_fat_unlock)
    TEST(test_hythread_thin_monitor_enter_contended)
TEST_LIST_END;
