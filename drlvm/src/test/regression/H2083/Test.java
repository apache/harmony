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

package org.apache.harmony.drlvm.tests.regression.h2083;

import junit.framework.TestCase;

public class Test extends TestCase {
    static final int N_THREADS = 100;
    static final int N_ITERS = 500;

    public void test() throws Exception {
        Thread threads[] = new Thread[N_THREADS];

        for (int i = 0; i < N_THREADS; i++)
            threads[i] = new TestThread();

        System.out.println("START");
        for (int i = 0; i < N_THREADS; i++)
            threads[i].start();

        System.out.println("JOIN");
        for (int i = 0; i < N_THREADS; i++)
            threads[i].join();

        System.out.println("PASSED");
    }
}

class TestThread extends Thread {
    public void run() {
        for (int i = 0; i < Test.N_ITERS; i++) {
            try {
                new Missed();
            } catch (Throwable e) {}
        }
    }
}

class Missed {
}
