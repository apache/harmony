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
 * @author Pavel Pervov
 */  
package classloader;

/**
 * Test loading from multiple threads.
 */
public class StressLoader extends Thread {
    final static int COUNT = 250; // VM has a thread limit
    static StressLoader loader[] = new StressLoader[COUNT];

    public static void main(String[] s) {
        int i;

        for (i = 0; i < COUNT; i++) {
            loader[i] = new StressLoader();
            loader[i].start();
        }
        for (i = 0; i < COUNT; i++) {
            try {
                loader[i].join();
            } catch (InterruptedException ie) {
                System.out.println(ie);
                ie.printStackTrace();
                passed = false;
            }
        }
        if (passed) {
            System.out.println("pass");
        }
    }

    static boolean passed = true;

    public void run() {
        LogLoader ll = new LogLoader();
        try {
            ll.loadClass("classloader.LogLoader");
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            passed = false;
        }
    }

}
