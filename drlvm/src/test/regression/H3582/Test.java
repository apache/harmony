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


package org.apache.harmony.drlvm.tests.regression.h3582;
import junit.framework.*;

public class Test extends TestCase {
    public static void main(String[] args) {
        (new Test()).test();
    }

    public void test() {
        try {
            (new MegaJoin()).test();
        } catch (Throwable e) {
            e.printStackTrace();
            fail();
        }
    }
}

class MegaJoin implements Runnable {
    static int number_of_bosses = 10;
    static int worker_threads_per_boss = 1000;
    static int number_of_workers_created = 0;

    public static void main(String[] args) {
        (new Test()).test();
    }

    public void test() throws InterruptedException {
        Thread[] boss_threads = new Thread[number_of_bosses];
        int ii = 0;
        for (ii = 0; ii < boss_threads.length; ii++) {
            boss_threads[ii] = new Thread(new MegaJoin());
            System.out.println("H3582: " +  boss_threads[ii]);
            boss_threads[ii].start();
        }

        for (ii = 0; ii < boss_threads.length; ii++) {
            try {
                boss_threads[ii].join();
                System.out.println("H3582: " +  boss_threads[ii]
                        + " - finished!");
            } catch (InterruptedException e) {
                throw e;
            }
        }
        System.out.println("H3582: PASSED!");
    }

    public void run() {
        Thread [] wta = new Thread[worker_threads_per_boss];
        for (int ii = 0; ii < wta.length; ii++) {
            wta[ii] = new Thread() {
                public void run () {
                    //intentionally empty, only testing thread join
                }
            };
            wta[ii].start();
        }

        for (int ii = 0; ii < wta.length; ii++) {
            try {
                wta[ii].join();
            }
            catch (InterruptedException e) {
                System.out.println("Working thread exeption:");
                e.printStackTrace();
            }
        }
    }
}
