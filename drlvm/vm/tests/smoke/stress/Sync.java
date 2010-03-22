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
* @author Salikh Zakirov
*/  

package stress;

public class Sync extends Thread {

    final static int number_of_threads = 200;
    final static int iterations = 10000;

    static Thread threads[];
    static volatile int waiting;

    public static void main(String[] args) {
        threads = new Thread[number_of_threads];
        waiting = 0;

        for (int i = 0; i < number_of_threads; i++) {
            threads[i] = new Sync(i);
            threads[i].setDaemon(true);
        }

        for (int i = 0; i < number_of_threads; i++) {
            threads[i].start();
        }

        try { Thread.sleep(10000); } catch (InterruptedException e) {}

        for (int t = 0; t < number_of_threads; t++) {
            synchronized (threads[t]) {
                threads[t].notifyAll();
            }
        }

        System.out.println("PASSED");
    }

    int n;
    public Sync(int n) {
        this.n = n;
    }

    public void run() {
        for (int i = n; i < iterations; i++) {
            int t = i % threads.length;
            synchronized (threads[t]) {
                threads[t].notifyAll();
            }
            try { Thread.sleep(i%2); } catch (InterruptedException e) {}
            if (waiting < number_of_threads/2) {
                synchronized (threads[t]) {
                    waiting++;
                    try { threads[t].wait(); } catch (InterruptedException e) {}
                    waiting--;
                }
            }
            if (0 == n && i % 1000 == 0) {
                System.out.print("."); System.out.flush();
            }
        }

    }
}
