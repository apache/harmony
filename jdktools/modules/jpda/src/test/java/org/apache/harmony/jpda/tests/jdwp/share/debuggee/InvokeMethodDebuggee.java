/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Vitaly A. Provodin
 */

/**
 * Created on 10.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.share.debuggee;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * This class provides common debuggee class for InvokeMethod tests. 
 *  
 */
public class InvokeMethodDebuggee extends SyncDebuggee {

    public class testClass1 {
        public testClass1() {
            logWriter.println("constructor testClass1 invoked");
        }
    }

    public int testMethod1(boolean needThrow) throws Throwable {
        logWriter.println("method testClass1 invoked");
        if (needThrow) {
            throw new Throwable("test exception");
        }
        return 123;
    }

    public static int testMethod2(boolean needThrow) throws Throwable {
        if (needThrow) {
            throw new Throwable("test exception");
        }
        return 234;
    }

    static int checkInt = 1;
    static int[] checkIntArray = {1, 2};
    static int[][] checkIntArray2 = {{123}, {23, 34}, {2, 4, 6, 8}};
    static String checkString = "text 1";
    static String[] checkStringArray = {"text 2"};
    static String[][] checkStringArray2 = {{"text 3"}, {"text 4"}};
    static testClass checkClass = new testClass();
    static testClass[] checkClassArray = {new testClass()};
    static testClass[][] checkClassArray2 = {{new testClass()}, {new testClass()}};

    public static String testMethod3(int i, int[] ai, int[][] aai,
            String s, String[] as, String[][] aas,
            testClass tc, testClass[] atc, testClass[][] aatc) {
        return "qwerty";
    }

    void execMethod() {
        logWriter.println("InvokeMethodDebuggee.execMethod()");
    }

    public void run() {
        Class c = null;
        try {
            c = Class.forName("org.apache.harmony.jpda.tests.jdwp.share.debuggee.InvokeMethodDebuggee$testClass1");
            c = Class.forName("org.apache.harmony.jpda.tests.jdwp.share.debuggee.testClass2");
            c = Class.forName("org.apache.harmony.jpda.tests.jdwp.share.debuggee.testClass3");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        logWriter.println("InvokeMethodDebuggee");
//        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        synchronizer.receiveMessageWithoutException("org.apache.harmony.jpda.tests.jdwp.share.debuggee.InvokeMethodDebuggee(#1)");
        execMethod();
//        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        synchronizer.receiveMessageWithoutException("org.apache.harmony.jpda.tests.jdwp.share.debuggee.InvokeMethodDebuggee(#2)");
        logWriter.println("DUMP{" + c + "}");
    }

    public static void main(String[] args) {
        runDebuggee(InvokeMethodDebuggee.class);
    }
}

class testClass {
}

class testClass2 {
    public testClass2(boolean needThrow) throws Throwable {
        if (needThrow) {
            throw new Throwable("test exception");
        }
    }

    public int testMethod3(boolean needThrow) throws Throwable {
        if (needThrow) {
            throw new Throwable("test exception");
        }
        return 345;
    }
}

class testClass3 extends testClass2 {
    public testClass3() throws Throwable {
        super(false);
    }

    public int testMethod3(boolean needThrow) throws Throwable {
        if (needThrow) {
            throw new Throwable("test exception");
        }
        return 456;
    }
}

