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

public class InterruptWait {
    static boolean success = true;

    public static void main(String[] args) {
        final Lock lock = new Lock();
        final Thread[] pool = new Thread[10];

        for (int i = 0; i < pool.length; i++) {
            pool[i] = new Thread() {
                public void run() {
                    lock.lock();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.out.println(this.toString() + " interrupted while working!");
                        success = false;
                    }
                    lock.unlock();
                }
            };
        }
        for (int i = 0; i < pool.length; i++) {
            pool[i].start();
        }
        for (int i = 0; i < pool.length; i++) {
            try {
                pool[i].join();
            } catch (InterruptedException e) {
                System.out.println(pool[i].toString() + " interrupted while joining!");
                success = false;
            }
        }
        System.out.println(success ? "PASS" : "FAIL");
    }

    static class Lock {
        final java.util.List<Thread> waitQueue = new java.util.LinkedList<Thread>();
        Thread owner;

        public void lock() {
            final Thread currentThread = Thread.currentThread();

            synchronized (this) {
                if (owner == currentThread) {
                    return;
                }
                if (owner == null) {
                    owner = currentThread;
                    return;
                }
                waitQueue.add(currentThread);

                try {
                    wait();
                } catch (InterruptedException e) {
                    // Expected, ignore.
                    // System.out.println(Thread.currentThread().toString() + " interrupted while waiting!");
                }
            }
        }

        public void unlock() {
            synchronized (this) {
                if (owner == null) {
                    System.out.println("ERROR at " + Thread.currentThread() + ": Can\'t unlock not locked resource");
                    success = false;
                }
                if (owner != Thread.currentThread()) {
                    System.out.println("ERROR at " + Thread.currentThread() + ": Not owner can\'t unlock resource");
                    success = false;
                }
                while (!waitQueue.isEmpty()) {
                    final Thread nextThread = waitQueue.remove(0);

                    if (!nextThread.isInterrupted()) {
                        owner = nextThread;
                        nextThread.interrupt();
                        return;
                    }
                }
                owner = null;
            }
        }
    }
}
