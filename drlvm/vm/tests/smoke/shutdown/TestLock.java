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

package shutdown;

public class TestLock {
    private static Object start = new Object();
    private static Object sync = new Object();

    public static void main(String[] args) {
        synchronized (start) {
            try {
                Thread worker = new WorkerThread();
                worker.setDaemon(true);
                worker.start();
                start.wait();
                Thread locked = new LockedThread();
                locked.setDaemon(true);
                locked.start();
                start.wait();
            } catch (InterruptedException e) {
                System.out.println("FAILED");
            }
        }
        System.out.println("PASSED");
    }

    static class WorkerThread extends Thread {
        public void run() {
            synchronized (sync) {
                synchronized (start) {
                    start.notify();
                }
                while (true) {
                    Thread.yield();
                }
            }
        }
    }

    static class LockedThread extends Thread {
        public void run() {
            synchronized (start) {
                start.notify();
            }
            synchronized (sync) {
                System.out.println("FAILED");
            }
            System.out.println("FAILED");
        }
    }

}