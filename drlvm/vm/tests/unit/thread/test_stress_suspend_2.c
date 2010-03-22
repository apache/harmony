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

#include <port_barriers.h>
#include <port_sysinfo.h>
#include <open/hythread.h>
#include <open/hythread_ext.h>

////////////////////////
// Tested functions:
////////////////////////
#define test_suspend_disable    hythread_suspend_disable    // not changed
#define test_suspend_enable     hythread_suspend_enable     // not changed
#define test_gc_suspend         hythread_suspend_other      // not changed
#define test_gc_resume          hythread_resume             // not changed
#define test_java_suspend       jthread_suspend             // not changed
#define test_java_resume        jthread_resume              // not changed
#define test_safe_point         hythread_safe_point         // not changed

#define test_function test_java_suspend_stress_2

static int GC_REQUEST_THREAD_COUNT;
static int JAVA_REQUEST_THREAD_COUNT;

hy_inline void test_hythread_suspend_disable()
{
    register hythread_t thread = hythread_self();

    // Check that current thread is in default thread group.
    // Justification: GC suspends and enumerates threads from
    // default group only.
    assert(((HyThread_public *)thread)->group == get_java_thread_group());

    ((HyThread_public *)thread)->disable_count++;
    //port_rw_barrier();

    if (thread->request && thread->disable_count == 1) {
        // enter to safe point if suspend request was set
        // and suspend disable was made a moment ago
        // (it's a point of entry to the unsafe region)
        hythread_safe_point_other(thread);
    }
    return;
} // test_hythread_suspend_disable

static void test_init()
{
    int proc_number = port_CPUs_number();
    GC_REQUEST_THREAD_COUNT = 0;
    JAVA_REQUEST_THREAD_COUNT = (proc_number * 3 + 1) / 2;
} // test_init

// include test environment
#include "test_stress_suspend.h"

TEST_LIST_START
    TEST(test_java_suspend_stress_2)
TEST_LIST_END;
