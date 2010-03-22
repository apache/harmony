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
 * @author Alexei Fedotov, Salikh Zakirov
 */  
package thread;

/**
 * @keyword
 */
public class ThreadInterrupt implements Runnable {

    public static boolean doSpin = true;

    public void run() {
        boolean state = true;
        try {
            int s = 0;
            while (doSpin) {
                s++;
            }
            state = false;
            Thread.sleep(3000);
            System.out.println("fail");
        } catch (InterruptedException e) {
            if (state) {
              System.out.println("fail, interrupt should happen in sleep() call");
            } else {
              System.out.println("pass");
            }
        }
    }

    public static void main (String[] args) {
        Thread x = new Thread(new ThreadInterrupt());
        x.start();
        try { Thread.sleep(500); } catch (Exception e) { System.out.println("fail, " + e); }
        System.out.println("interrupting...");
        x.interrupt();
        try { Thread.sleep(500); } catch (Exception e) { System.out.println("fail, " + e); }
        doSpin = false;
        try { x.join(); } catch (Exception e) { System.out.println("fail, " + e); }
    }
}
