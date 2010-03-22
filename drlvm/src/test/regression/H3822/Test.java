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

package org.apache.harmony.drlvm.tests.regression.h3822;
import junit.framework.*;

public class Test extends TestCase {
    public static void main(String[] args) {
        (new Test()).test();
    }

    public void test() {
        try {
            if (!(new ThreadTest()).test()) {
                fail();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            fail();
        }
    }
}

class ThreadTest extends Thread {
    public boolean test() {
        final int MAX_THREADS = 10000;
        int i = 0;
        try {
            for (i = 0; i < MAX_THREADS; i++) {
                ThreadTest t = new ThreadTest();
                t.start();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        } finally {
            System.out.println("Created threads: " + i);
        }
        return (i == MAX_THREADS);
    }
    
    public void run() {
        System.out.print("*");
    }
}
