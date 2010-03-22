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

package thread;

public class SmallStackThreadTest implements Runnable
{
    private static final int MAX_THREADS = 4 * 1024;
    private static final int STACK_SIZE = 256 * 1024;

    private static int started = 0;
    private static int finished = 0;
    private static Object awake = new Object();
    private static boolean awaked = false;

    public static void main(String [] args) {
        Thread[] threads = new Thread[MAX_THREADS];
        for (int i = 0; i < MAX_THREADS; i++) {
            threads[i] = new Thread( null,
                                 new SmallStackThreadTest(),
                                 "Test Thread " + i,
                                 STACK_SIZE);
            threads[i].start();

            if ((i%100) == 0) {
                System.out.println("Start Thread " + i);
            }
        }

        synchronized(awake){
            awaked = true;
            awake.notifyAll();
        }

        for (int i = 0; i < MAX_THREADS; i++) {
            try {
                threads[i].join();
            } catch (Throwable th) {
                System.out.println("Was caught " + th);
            }
        }

        synchronized(SmallStackThreadTest.class) {
            if (started != MAX_THREADS) {
                System.out.println("FAILED");
            } else if (finished != MAX_THREADS) {
                System.out.println("FAILED");
            } else {
                System.out.println("PASSED");
            }
        }
    }

    public void run() {
        synchronized(SmallStackThreadTest.class) {
            SmallStackThreadTest.started++;
        }

        try {
            synchronized(awake){
                if (!awaked) {
                    awake.wait();
                }
            }
        } catch (Throwable th) {
            System.out.println("Was caught " + th);
        }

        synchronized(SmallStackThreadTest.class) {
            SmallStackThreadTest.finished++;
        }
    }
} 

