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
 * Created on 25.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ThreadGroupReference;

import org.apache.harmony.jpda.tests.framework.DebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.framework.LogWriter;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;


/**
 * The class specifies debuggee for <code>org.apache.harmony.jpda.tests.jdwp.ThreadGroupReference.NameTest</code>
 * and <code>org.apache.harmony.jpda.tests.jdwp.ThreadGroupReference.ParentTest</code>.
 * This debuggee is started as follow:
 * <ol>
 *      <li>the group <code>PARENT_GROUP</code> is created
 *      <li>the group <code>CHILD_GROUP</code> is created as a child of
 *          the <code>PARENT_GROUP</code>  
 *      <li>the tested thread <code>TESTED_THREAD</code> is started so this
 *          thread belongs to the <code>CHILD_GROUP</code>.
 * </ol>
 */
public class NameDebuggee extends SyncDebuggee {

    public static final String PARENT_GROUP  = "ParentGroup";
    public static final String CHILD_GROUP   = "ChildGroup";
    public static final String TESTED_THREAD = "TestedThread";
    
    static Object waitForStart = new Object();
    static Object waitForFinish = new Object();
    
    public void run() {
        ThreadGroup thrdGroupParent = new ThreadGroup(PARENT_GROUP);
        ThreadGroup thrdGroupChild = new ThreadGroup(thrdGroupParent, CHILD_GROUP);
        DebuggeeThread thrd = new DebuggeeThread(thrdGroupChild, TESTED_THREAD,
                logWriter, synchronizer); 
        
        synchronized(waitForStart){
            thrd.start();
            try {
                waitForStart.wait();
            } catch (InterruptedException e) {
                
            }
        }

        synchronized(waitForFinish){
            logWriter.println("thread is finished");
        }
    }

    class DebuggeeThread extends Thread {

        LogWriter logWriter;
        DebuggeeSynchronizer synchronizer;

        public DebuggeeThread(ThreadGroup thrdGroup, String name,
                LogWriter logWriter, DebuggeeSynchronizer synchronizer) {
            super(thrdGroup, name);
            this.logWriter = logWriter;
            this.synchronizer = synchronizer;
        }

        public void run() {

            synchronized(NameDebuggee.waitForFinish){

                synchronized(NameDebuggee.waitForStart){

                    NameDebuggee.waitForStart.notifyAll();

                    logWriter.println(getName() +  ": started");
                    synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);

                    logWriter.println(getName() +  ": wait for SGNL_CONTINUE");
                    synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
                    logWriter.println(getName() +  ": finished");
                }
            }
        }
    }

    public static void main(String [] args) {
        runDebuggee(NameDebuggee.class);
    }
}
