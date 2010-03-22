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

/**
 * Tries to start at least 100 threads simultaneously.
 */
public class Threads extends Thread {

    // do not try to create more than 1024 threads,
    // as our GC can serve only that much
    static final int thread_count = 700;
    static boolean wait = true;

    public static void main (String[] args) {
        int num = 0;
        Thread threads[] = new Thread[thread_count];
        try {
            for (num = 0; num < thread_count; num++) {
                threads[num] = new Threads();
                threads[num].start();
            }
        } catch (Throwable e) {
            System.out.println("\n" + num + " threads creation resulted in " + e);
        }
        System.gc();
        synchronized (Threads.class) {
            wait = false;
            Threads.class.notifyAll();
        }
        for (int i = 0; i < num; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) { /* ignore */ }
        }
        if (num >= 100) {
            System.out.println("PASSED, " + num + " threads created and started");
        } else {
            System.out.println("FAILED, insufficient threads: " + num);
        }
    }

    public Threads () {
        setDaemon(true);
    }

    public void run () {
        trace(".");
        synchronized (this.getClass()) {
            try { 
                if (!wait) return;
                this.getClass().wait();
            } catch (InterruptedException e) {}
        }
    }

    public static void trace(Object o) {
        System.out.print(o);
        System.out.flush();
    }
}
