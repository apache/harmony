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


package org.apache.harmony.drlvm.tests.regression.h4265;
import junit.framework.*;

public class Test extends TestCase {

//1
    void _testRec1() {
        _testRec1();
    }

    public void testRec1() {
        try {
            _testRec1();
        } catch (StackOverflowError e) {
            return;
        }
        fail("No SOE was thrown!");
    }



//2
    Object lock2 = new Object();
    void _testSyncRec1() {
        synchronized(lock2) {
            _testSyncRec1();
        }
    }

    public void testSyncRec1() {
        try {
            _testSyncRec1();
        } catch (StackOverflowError e) {
            return;
        }
        fail("No SOE was thrown!");
    }


//3
    Object lock3 = new Object();
    void _testSyncRec2() {
        try {
            synchronized(lock3) {
                _testSyncRec2();            
            }
        } catch (Error r) {
            //ignore
        }
    }

    public void testSyncRec2() throws Exception {
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                _testSyncRec2();                
            }            
        });
        t1.start();
        t1.join();

        Thread t2 = new Thread(new Runnable() {
            public void run() {
                _testSyncRec2();                
            }            
        });
        t2.start();
        t2.join();
        //pass if no hang            
    }


//4
    int i1=0;
    void _testRec2() {
        try { 
            i1++;
            _testRec2();            
        } finally {
            i1--;
        }
    }

    public void testRec2() {
        i1=0;
        try {
            _testRec2();
        } catch (StackOverflowError e) {
            assertEquals(0, i1);
            return;
        }
        fail("No SOE was thrown!");
            
    }

//5
    public void testRec3() {
        i1=0;
        try {
            _testRec2();
        } catch (Throwable e) {
            assertEquals(0, i1);
            return;
        }
        fail("No Throwable was thrown!");
            
    }

//6
    public void testRec4() {
        i1=0;
        try {
            _testRec2();
        } catch (Error e) {
            assertEquals(0, i1);
            return;
        }
        fail("No Error was thrown!");
            
    }
    

}
