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

/**
 * Tests that VM doen't hang on shutdown if a daemon thread is on Object.wait()
 */
public class TestDaemonOnWait {
    private static Object start = new Object();
    private static Object sync = new Object();

    public static void main(String[] args) {
        synchronized (start) {
            try {
                Thread worker = new WorkerThread();
                worker.setDaemon(true);
                worker.start();
                start.wait();
            } catch (InterruptedException e) {
                System.out.println("FAILED");
            }

            System.out.println("PASSED");
        }
    }

    static class WorkerThread extends Thread {
        private int recursion = 0;

        static {
            System.loadLibrary("TestDaemonOnWait");
        }


        public native void callJNI();

        public void calledFromJNI() throws InterruptedException {
            if (recursion < 30) {
                 ++recursion;
                 run();
            }

            // when desired stack frame count is achieved
            synchronized (sync) {
                synchronized (start) {
                    // release main thread in order to initiate VM shutdown
                    start.notify();
                }

                // wait here forever.
                // actually this whait() will be interrupted by VM shutdown
                // process with the exception.
                sync.wait();
            }
        }

        public void run() {
            // recursively calls JNI method which calls java method in order
            // to create a number of M2n and java frames on stack.
            callJNI();
        }
    }
}
