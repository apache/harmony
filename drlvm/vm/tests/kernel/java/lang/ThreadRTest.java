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
 * @author Elena Semukhina
 */
package java.lang;

import junit.framework.TestCase;

public class ThreadRTest extends TestCase {

    private class ThreadRunning extends Thread {
        volatile boolean stopWork = false;
        int i = 0;

        ThreadRunning() {
            super();
        }
        
        ThreadRunning(ThreadGroup g, String name) {
            super(g, name);
        }
        public void run () {
            while (!stopWork) {
                i++;
            }
        }
    }

    public void testGetThreadGroupDeadThread() {
        ThreadRunning t = new ThreadRunning();
        t.start();
        t.stopWork = true;
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();          
        }
        try {
            t.getThreadGroup();
            t.getThreadGroup();
        } catch (NullPointerException e) {
            fail("NullPointerException has been thrown");
        }
    }
    /*
     * Regression test for Harmony-917
     */
    public void testJoinJI() throws Exception {
        Thread th = new Thread(); 

        long mls = 688204075024689866L; 
        int nn = -10000; 
        try { 
            th.join(mls, nn); 
            fail("1: test failed"); 
        } catch (IllegalArgumentException e) { 
            //expected
        } 

        mls = -1000000000000L; 
        nn = 90000; 
        try { 
            th.join(mls, nn); 
            fail("2: test failed"); 
        } catch (IllegalArgumentException e) { 
            //expected
        } 

        mls = 10000000000000L; 
        nn = 1000001; 
        try { 
            th.join(mls, nn); 
            fail("3: Test failed"); 
        } catch (IllegalArgumentException e) { 
            //expected
        } 
    }    

    static class TT implements Runnable {
        public volatile boolean started = false;
        public volatile boolean finished = false;
        public void run() { 
            started = true; 
            try{ 
                synchronized(this) { 
                    while (true){ 
                    } 
                } 
            } finally { 
                finished = true; 
            } 
        } 
    } 

    /*
     * Regression test for Harmony-3116
     */
    public void testStopFinally() throws Exception {
        TT tt = new TT();
        Thread t = new Thread(tt); 
        t.start(); 
        while(!tt.started){ 
        } 
        t.stop(); 
        
        int count = 300;
        while(!tt.finished && count-- > 0 ){
            Thread.sleep(100);
        }          
        assertTrue(tt.finished);
    } 

}
