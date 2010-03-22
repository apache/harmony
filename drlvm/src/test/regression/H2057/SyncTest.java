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


package org.apache.harmony.drlvm.tests.regression.h2057;

import junit.framework.TestCase;

public class SyncTest extends TestCase {


    public void testMain() {
        Waiter w = new Waiter();
        w.start();         
        while(!w.passed){}
    }

}

class Stopper extends Thread {
    Waiter w;
    Stopper(Waiter w) {this.w=w;}

    public void run() {
//        System.out.println("stopper started..");
        try {sleep(1000);} catch (Exception e) {e.printStackTrace();}
//        System.out.println("stopping..");
        w.finish();
//        System.out.println("stopped..");
    }
}

class Waiter extends Thread {
    boolean done = false;
    boolean passed = false;

    synchronized void finish(){
//        System.out.println("inside finish()..");
        done = true;
    }

    public void run() {
//        System.out.println("waiter started..");
        new Stopper(this).start();
        int i=0;
        while(!done) {
            synchronized(this) {
                i++;
            }
        }
        passed = true;
//        System.out.println("passed!");
    }
}
