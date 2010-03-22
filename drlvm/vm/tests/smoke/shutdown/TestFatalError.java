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

public class TestFatalError {
    private static Object sync = new Object();

    static {
        System.loadLibrary("TestFatalError");
    }

    public static native void sendFatalError();

    public static void main(String[] args) {
        synchronized (sync) {
            Thread permanentThread = new PermanentThread();
            permanentThread.start();
            Thread hook = new HookThread();
            Runtime.getRuntime().addShutdownHook(hook);
            sendFatalError();
            System.out.println("FAILED");
        }
    }

    static class PermanentThread extends Thread {
        public void run() {
            synchronized (sync) {
                System.out.println("FAILED");
            }                
        }
        
    }

    static class HookThread extends Thread {
        public void run() {
           System.out.println("FAILED");
        }
    }
}