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

package org.apache.harmony.jpda.tests.jdwp.ThreadReference;

import org.apache.harmony.jpda.tests.framework.DebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.framework.LogWriter;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * The class specifies debuggee for
 * <code>org.apache.harmony.jpda.tests.jdwp.ThreadReference.ForceEarlyReturnTest</code>.
 * This debuggee starts the tested thread which name is supplied by test and
 * synchronized with test via the <code>SGNL_READY</code> and
 * <code>SGNL_CONTINUE</code> signals.
 */
public class ForceEarlyReturnDebuggee extends SyncDebuggee {

    public static String threadName;

    static boolean isFuncVoidBreak = true;

    static TestObject testObj = new TestObject();

    public static final String THREAD_VOID = "THREAD_VOID";

    public static final String THREAD_OBJECT = "THREAD_OBJECT";

    public static final String THREAD_INT = "THREAD_INT";

    public static final String THREAD_SHORT = "THREAD_SHORT";

    public static final String THREAD_BYTE = "THREAD_BYTE";

    public static final String THREAD_CHAR = "THREAD_CHAR";

    public static final String THREAD_BOOLEAN = "THREAD_BOOLEAN";

    public static final String THREAD_LONG = "THREAD_LONG";

    public static final String THREAD_FLOAT = "THREAD_FLOAT";

    public static final String THREAD_DOUBLE = "THREAD_DOUBLE";

    public static boolean condition = true;

    static Object waitForStart = new Object();

    static Object waitForFinish = new Object();

    public void run() {
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        threadName = synchronizer.receiveMessage();
        DebuggeeThread thrd = new DebuggeeThread(threadName, logWriter,
                synchronizer);
        synchronized (waitForStart) {
            thrd.start();
            try {
                waitForStart.wait();
            } catch (InterruptedException e) {

            }
        }
        synchronized (waitForFinish) {
            logWriter.println("thread is finished");
        }

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    public Object func_Object() {
        logWriter.println("In func_Object");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        while (condition)
            ;
        return new Object();
    }

    public int func_Int() {
        logWriter.println("In func_Int");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        while (condition)
            ;
        return -1;
    }

    public short func_Short() {
        logWriter.println("In func_Short");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        while (condition)
            ;
        return -1;
    }

    public byte func_Byte() {
        logWriter.println("In func_Byte");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        while (condition)
            ;
        return -1;
    }

    public char func_Char() {
        logWriter.println("In func_Char");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        while (condition)
            ;
        return 'Z';
    }

    public boolean func_Boolean() {
        logWriter.println("In func_Boolean");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        while (condition)
            ;
        return false;
    }

    public long func_Long() {
        logWriter.println("In func_Long");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        while (condition)
            ;
        return -1;
    }

    public float func_Float() {
        logWriter.println("In func_Float");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        while (condition)
            ;
        return -1;
    }

    public double func_Double() {
        logWriter.println("In func_Double");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        while (condition)
            ;
        return -1;
    }

    public void func_Void() {
        logWriter.println("In func_Void");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        while (condition)
            ;
        isFuncVoidBreak = false;
        return;
    }

    class DebuggeeThread extends Thread {

        LogWriter logWriter;

        DebuggeeSynchronizer synchronizer;

        public DebuggeeThread(String name, LogWriter logWriter,
                DebuggeeSynchronizer synchronizer) {
            super(name);
            this.logWriter = logWriter;
            this.synchronizer = synchronizer;
        }

        public void run() {

            synchronized (ForceEarlyReturnDebuggee.waitForFinish) {

                synchronized (ForceEarlyReturnDebuggee.waitForStart) {

                    ForceEarlyReturnDebuggee.waitForStart.notifyAll();

                    logWriter.println(getName() + ": started");

                    if (getName().equals(THREAD_OBJECT)) {
                        Object result = func_Object();
                        logWriter.println(getName() + ": " + "Object");
                        if (result instanceof TestObject) {
                            synchronizer.sendMessage("TRUE");
                        } else {
                            synchronizer.sendMessage("FALSE");
                        }
                        logWriter
                                .println(getName() + ": func_Object returned.");

                    } else if (getName().equals(THREAD_INT)) {
                        int result = func_Int();
                        logWriter.println(getName() + ": " + result);
                        synchronizer
                                .sendMessage(new Integer(result).toString());
                        logWriter.println(getName() + ": func_Int returned.");
                    } else if (getName().equals(THREAD_SHORT)) {
                        short result = func_Short();
                        logWriter.println(getName() + ": " + result);
                        synchronizer
                                .sendMessage(new Integer(result).toString());
                        logWriter.println(getName() + ": func_Short returned.");
                    } else if (getName().equals(THREAD_BYTE)) {
                        byte result = func_Byte();
                        logWriter.println(getName() + ": " + result);
                        synchronizer
                                .sendMessage(new Integer(result).toString());
                        logWriter.println(getName() + ": func_Byte returned.");
                    } else if (getName().equals(THREAD_CHAR)) {
                        char result = func_Char();
                        logWriter.println(getName() + ": " + result);
                        synchronizer.sendMessage(new Character(result)
                                .toString());
                        logWriter.println(getName() + ": func_Char returned.");
                    } else if (getName().equals(THREAD_BOOLEAN)) {
                        Boolean result = func_Boolean();
                        logWriter.println(getName() + ": " + result);
                        synchronizer
                                .sendMessage(new Boolean(result).toString());
                        logWriter.println(getName()
                                + ": func_Boolean returned.");
                    } else if (getName().equals(THREAD_LONG)) {
                        long result = func_Long();
                        logWriter.println(getName() + ": " + result);
                        synchronizer.sendMessage(new Long(result).toString());
                        logWriter.println(getName() + ": func_Long returned.");
                    } else if (getName().equals(THREAD_FLOAT)) {
                        float result = func_Float();
                        logWriter.println(getName() + ": " + result);
                        synchronizer.sendMessage(new Float(result).toString());
                        logWriter.println(getName() + ": func_Float returned.");
                    } else if (getName().equals(THREAD_DOUBLE)) {
                        double result = func_Double();
                        logWriter.println(getName() + ": " + result);
                        synchronizer.sendMessage(new Double(result).toString());
                        logWriter
                                .println(getName() + ": func_Double returned.");
                    } else if (getName().equals(THREAD_VOID)) {
                        func_Void();
                        logWriter.println(getName() + ": " + "void");
                        if (isFuncVoidBreak) {
                            synchronizer.sendMessage("TRUE");
                        } else {
                            synchronizer.sendMessage("FALSE");
                        }
                        logWriter.println(getName() + ": func_Void returned.");
                    } else {
                        logWriter.println(getName() + ": no func is called.");
                        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
                        synchronizer.receiveMessage("ThreadExit");
                    }

                    logWriter.println(getName() + ": finished");

                }
            }
        }
    }

    public static void main(String[] args) {
        runDebuggee(ForceEarlyReturnDebuggee.class);
    }

}

class TestObject {

}
