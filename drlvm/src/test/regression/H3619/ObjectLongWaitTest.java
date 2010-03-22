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

package org.apache.harmony.drlvm.tests.regression.h3619;

import junit.framework.TestCase;

public class ObjectLongWaitTest extends TestCase {
    
    public static void main(String args[]) throws Exception {
       (new ObjectLongWaitTest()).test();
    }

    public void test() throws Exception {
        TestThread testThread = new TestThread();
        System.out.println("TestThread is starting...");
        testThread.start();

        Thread.sleep(2000); // give time to TestThread to behave
        
        if (testThread.finished) {
            testThread.join();
            fail("TestThread is returned from Object.wait(Long.MAX_VALUE)");
        } else {
            System.out.println("TestThread expectedly hanged up. Interrupting...");
            testThread.interrupt();
        }
    }
}

class TestThread extends Thread {

    public volatile boolean finished = false;

    public void run() {
        try {
            System.out.println("TestThread started");
            String lock = "lock";
            synchronized (lock) {
                lock.wait(Long.MAX_VALUE);
            }
            finished = true;
            System.out.println("Object.wait(Long.MAX_VALUE) exited in couple of seconds");
        } catch (InterruptedException e) {
        }
    }
}
