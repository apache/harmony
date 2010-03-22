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
 * Created on 16.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * The class specifies debuggee for <code>org.apache.harmony.jpda.tests.jdwp.ThreadReference.FramesTest</code>.
 * It recursively invokes the method <code>FramesDebuggee.recursiveMethod</code>
 * thereby the specified depth <code>FramesDebuggee.DEPTH</code> of recursion
 * is reached. 
 */
public class FramesDebuggee extends SyncDebuggee {

    public final static int DEPTH = 10; 
    public final static String METHOD_NAME = "recursiveMethod";
    public volatile static String THREAD_NAME = "TBD";

    private int depthCount = 0; 

    private void recursiveMethod() {
        depthCount++;

        if (depthCount < DEPTH) {
            logWriter.println("\tinvoke tested method: depth=" + depthCount);
            recursiveMethod();
        }

        if (depthCount == DEPTH) {
            logWriter.println("\tsending SGNL_READY signal");
            synchronizer.sendMessage(THREAD_NAME);
            synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        }
        depthCount--;
    }

    public void run() {
        THREAD_NAME = Thread.currentThread().getName();
        recursiveMethod();
    }

    public static void main(String[] args) {
        runDebuggee(FramesDebuggee.class);
    }
}
