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
 * @author Anton V. Karnachuk
 */

/**
 * Created on 09.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ArrayReference;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * Common debuggee of JDWP unit tests for JDWP ArrayReference command set:
 * GetValuesTest, LengthTest, SetValuesTest.
 *  
 */
public class ArrayReferenceDebuggee extends SyncDebuggee {
    
    static int[] intArray = new int[10];
    static String[] strArray = new String[8]; 
    static Integer intField = new Integer(-1);

    static Thread[] threadArray = { new Thread() }; 
    static ThreadGroup[] threadGroupArray = { new ThreadGroup("name") }; 
    static Class[] classArray = { ArrayReferenceDebuggee.class }; 
    static ClassLoader[] ClassLoaderArray = { ArrayReferenceDebuggee.class.getClassLoader() }; 
    static MyThread[] myThreadArray = { new MyThread() }; 
    static Object[][] objectArrayArray = { threadArray }; 
    
    static long[] longArray = new long[10];
    static byte[] byteArray = new byte[10];
    
    static {
        for (int i=0; i<intArray.length; i++) {
            intArray[i] = i;
            byteArray[i] = (byte)i;
            longArray[i] = i;
        }
        
        for (int i=0; i<strArray.length; i++) {
            strArray[i] = ""+i;
        }
    }

    public void run() {
        logWriter.println("-- ArrayReferenceDebuggee: STARTED ...");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("-- ArrayReferenceDebuggee: FINISHing ...");
    }

    /**
     * Starts ArrayReferenceDebuggee with help of runDebuggee() method 
     * from <A HREF="../../share/Debuggee.html">Debuggee</A> super class.
     *  
     */
    public static void main(String [] args) {
        runDebuggee(ArrayReferenceDebuggee.class);
    }

}

class MyThread extends Thread {}

