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
 * @author Pavel Dolgov
 */
package org.apache.harmony.awt.wtk;

import junit.framework.TestCase;

public class ShutdownWatchdogTest extends TestCase {
    
    private ShutdownWatchdog wd;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ShutdownWatchdogTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        wd = new ShutdownWatchdog();
    }
    
    public void testSetWindowListEmptyTrue() {
        intro();
        wd.setWindowListEmpty(true);
        sleep();
        assertFalse(isShutdownThreadRunning());
        outro();
    }
    
    public void testSetWindowListEmptyFalse() {
        intro();
        wd.setWindowListEmpty(false);
        assertTrue(isShutdownThreadRunning());
        outro();
    }

    public void testSetAwtQueueEmptyTrue() {
        intro();
        wd.setAwtQueueEmpty(true);
        sleep();
        assertFalse(isShutdownThreadRunning());
        outro();
    }
    
    public void testSetAwtQueueEmptyFalse() {
        intro();
        wd.setAwtQueueEmpty(false);
        assertTrue(isShutdownThreadRunning());
        outro();
    }

    public void testSetNativeQueueEmptyTrue() {
        intro();
        wd.setNativeQueueEmpty(true);
        sleep();
        assertFalse(isShutdownThreadRunning());
        outro();
    }
    
    public void testSetNativeQueueEmptyFalse() {
        intro();
        wd.setNativeQueueEmpty(false);
        assertTrue(isShutdownThreadRunning());
        outro();
    }

    public void testStartThenForceShutdown() {
        intro();
        outro();
    }
    
    private void intro() {
        assertFalse(isShutdownThreadRunning());
        wd.start();
        assertTrue(isShutdownThreadRunning());
    }
    
    private void outro() {
        wd.forceShutdown();
        sleep();
        assertFalse(isShutdownThreadRunning());
    }
    
    private boolean isShutdownThreadRunning() {
        ThreadGroup g = Thread.currentThread().getThreadGroup();
        Thread[] threads = new Thread[g.activeCount() + 1];
        while(true) {
            int actualCount = g.enumerate(threads);
            if (actualCount < threads.length) {
                break;
            }
            threads = new Thread[actualCount + 1];
        }
        for (Thread element : threads) {
            if ((element != null) && 
                    element.getName().equals("AWT-Shutdown")) {
                return true;
            }
        }
        return false;
    }
    
    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
