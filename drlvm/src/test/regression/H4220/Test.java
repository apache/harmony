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

package org.apache.harmony.drlvm.tests.regression.h4220;

import junit.framework.TestCase;

public class Test extends TestCase {

    public static void main(String args[]) {
        (new Test()).test();
    }

    Object waitForStart = new Object();
    Object waitForInterrupt = new Object();
    Object waitForFinish = new Object();
    Thread notifyThread = new NotifierThread();
    Thread interruptThread = new InterrupterThread();

    public void test() {
        synchronized(waitForStart){
            notifyThread.start();
            try {
                waitForStart.wait();
            } catch (InterruptedException e) {
            }
        }
        synchronized(waitForFinish){
        }
    }

    class NotifierThread extends Thread {
        public void run() {
            synchronized(waitForFinish){
                synchronized(waitForStart){
                    waitForStart.notifyAll();
                    interruptThread.start();
                    // If comment next line test will not hang
                    Thread.yield();
                    try {
                        waitForStart.wait();
                    } catch (InterruptedException e) {
                        System.out.println("Expected " + e);
                        return;
                    }
                    assertTrue("waitForStart missed interrupt", true);
                }
            }
        }
    }

    class InterrupterThread extends Thread {
        public void run() {
            notifyThread.interrupt();
        }
    }
}
