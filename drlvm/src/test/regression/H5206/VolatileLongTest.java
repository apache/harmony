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

package org.apache.harmony.drlvm.tests.regression.h5206;

import junit.framework.TestCase;

public class VolatileLongTest extends TestCase {
    private class Interrupter extends Thread {
        Thread int_th;
        long timeout;
        Interrupter(Thread int_th, long timeout) {
            this.int_th = int_th;
            this.timeout = timeout;
        }
        public void run() {
            try {
                Thread.sleep(timeout);
            } catch(Throwable ex) {
            }
            int_th.interrupt();
        }
    }
    long lo;
    long hi;
    volatile long v;

    public static void main(String[] args) {
        new VolatileLongTest().test();
    }

    public void test() {
        lo = 0x00000000FFFFFFFFL;
        hi = 0xFFFFFFFF00000000L;
        v = hi;

        Thread t1 = new Thread() {
            public void run() {
                while (!isInterrupted()) {
                    v = lo;
                    v = hi;
                }
            }
        };
        Thread t2 = new Thread() {
            public void run() {
                while (!isInterrupted()) {
                    v = hi;
                    v = lo;
                }
            }
        };

        boolean passed = true;
        long v_copy = 0;

        t1.start();
        t2.start();
        new Interrupter(Thread.currentThread(), 10000).start();

        while(!Thread.currentThread().interrupted()) {
            Thread.yield();
            v_copy = v;
            if(v_copy != lo && v_copy != hi) {
                passed = false;
                break;
            }
        }

        t1.interrupt();
        t2.interrupt();

        assertTrue("" + v_copy, passed);

        System.out.println("PASSED");
    }
}

