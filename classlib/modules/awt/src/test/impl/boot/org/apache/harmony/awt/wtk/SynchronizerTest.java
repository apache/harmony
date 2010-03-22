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
/**
 * @author Michael Danilov
 */
package org.apache.harmony.awt.wtk;

import junit.framework.TestCase;

public class SynchronizerTest extends TestCase {

    class ConcreteSynchronizer extends Synchronizer
    {
        protected boolean isDispatchThread() {
            return false;
        }
    }

    Synchronizer s;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SynchronizerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        s = new ConcreteSynchronizer();
    }

    public void testLockUnlock() {
        s.lock();
        s.lock();
        s.unlock();
        s.unlock();

        boolean unlocked = false;
        try {
            s.unlock();
        } catch (Throwable t) {
            unlocked = true;
        }
        assertTrue(unlocked);
    }

    public void testStoreStateAndUnlockAllLockAndRestoreState() {
        boolean free = false;
        try {
            s.storeStateAndFree();
        } catch (Throwable t) {
            free = true;
        }
        assertTrue(free);

        s.lock();
        s.lock();
        s.storeStateAndFree();

        boolean secondTime = false;
        try {
            s.storeStateAndFree();
        } catch (Throwable t) {
            secondTime = true;
        }
        assertTrue(secondTime);

        boolean locked = false;
        try {
            s.lock();
            s.lockAndRestoreState();
        } catch (Throwable t) {
            locked = true;
            s.unlock();
        }
        assertTrue(locked);

        s.lockAndRestoreState();

        boolean notStored = false;
        try {
            s.lockAndRestoreState();
        } catch (Throwable t) {
            notStored = true;
        }
        assertTrue(notStored);

        s.unlock();
        s.unlock();
    }

    // Regression test for HARMONY-3601
    public void testHarmony_3601() throws Exception {
        final Thread[] threads = new Thread[10];
        final boolean[] errFlag = { false };

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread() {
                public void run() {
                    try {
                        java.awt.Toolkit.getDefaultToolkit().createImage(
                                new java.net.URL("file://any/thing"));
                    } catch (Throwable t) {
                        errFlag[0] = true;
                        fail(t.getMessage());
                    }
                }
            };
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }

        assertFalse("Exception in child thread", errFlag[0]);
    }
}
