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

package org.apache.harmony.drlvm.tests.regression.h3894;

import junit.framework.TestCase;
import java.util.HashSet;

/**
 * Test case for GetThreadGroupChildren() jvmti function.
 * Checks that the function may report more than 10 child theads.
 * Checks that the function reports the same thread group children that were
 * added by java application.
 * Checks that not started threads are not reported.
 * Checks that grand children are not reported.
 */
public class ThreadGroupChildren extends TestCase {

    static final int CHILD_THREADS = 15;
    static final int NOT_STARTED_CHILD_THREADS = 2;
    static final int CHILD_GROUPS = 15;
    static final int EMPTY_CHILD_GROUPS = 2;
    static final int GRAND_CHILD_THREADS = 2;
    static final int GRAND_CHILD_GROUPS = 2;

    public static void main(String args[]) {
        (new ThreadGroupChildren()).test();
    }

    public void test() {
        // this collection should hold references to created Thead and
        // ThreadGroup objects to avoid their elimination by garbage collector
        HashSet<Object> set = new HashSet<Object>();

        ThreadGroup testGroup = new ThreadGroup("test group");

        Thread[] childThreads = new Thread[CHILD_THREADS];

        for (int i = 0; i < CHILD_THREADS; i++) {
            Thread thread = new TestThread(testGroup, "child thread " + i);
            childThreads[i] = thread;
            set.add(thread);
            thread.start();
        }

        for (int i = 0; i < NOT_STARTED_CHILD_THREADS; i++) {
            Thread thread = new TestThread(testGroup,
                    "not started child thread " + i);
            set.add(thread);
        }

        ThreadGroup[] childGroups = new ThreadGroup[CHILD_GROUPS +
                EMPTY_CHILD_GROUPS];
        int childGroupsIndex = 0;

        for (int i = 0; i < EMPTY_CHILD_GROUPS; i++) {
            ThreadGroup group = new ThreadGroup(testGroup, "empty child group "
                     + i);
            childGroups[childGroupsIndex ++] = group;
            set.add(group);
        }

        for (int i = 0; i < CHILD_GROUPS; i++) {
            ThreadGroup group = new ThreadGroup(testGroup, "child group " + i);
            childGroups[childGroupsIndex ++] = group;
            set.add(group);

            for (int j = 0; j < GRAND_CHILD_THREADS; j++) {
                Thread thread = new TestThread(group, "grand child thread "
                         + j);
                set.add(thread);
                thread.start();
            }

            for (int j = 0; j < GRAND_CHILD_GROUPS; j++) {
                ThreadGroup subGroup = new ThreadGroup(group,
                         "empty grand child group " + j);
                set.add(subGroup);
            }
        }

        try {
            System.err.println("[Java]: Throwing an exception");
            // pass execution to the agent
            throw new InvokeAgentException(testGroup, childThreads,
                     childGroups);
        } catch (Exception e) {
            System.err.println("[Java]: Exception caught");
        }

        synchronized (TestThread.class) {
            try {
                TestThread.class.notifyAll();
            } catch (Throwable exc) {
                exc.printStackTrace();
            }
        }

        System.err.println("[Java]: test done");
        assertTrue(Status.status);
    }
}

class TestThread extends Thread {

    public TestThread(ThreadGroup group, String name) {
        super(group, name);
    }

    public void run() {
        System.err.println("Thread started: " + getName());

        synchronized (TestThread.class) {
            try {
                TestThread.class.wait();
            } catch (Throwable exc) {
                exc.printStackTrace();
            }
        }

        System.err.println("Thread finished: " + getName());
    }
}

class InvokeAgentException extends Exception {

    ThreadGroup testGroup;
    Thread[] childThreads;
    ThreadGroup[] childGroups;

    InvokeAgentException(ThreadGroup group, Thread[] threads,
             ThreadGroup[] groups) {
        testGroup = group;
        childThreads = threads;
        childGroups = groups;
    }
}

class Status {
    /** the field should be modified by jvmti agent to determine test result. */
    public static boolean status = false;
}
