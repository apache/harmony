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
 * Created on 11.04.2005
 */
package org.apache.harmony.jpda.tests.jdwp.Events;

import java.io.File;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * Debuggee for FieldAccessTest and FieldModified unit tests.
 * Provides access and modification of testIntField field.
 */
public class FieldModification002Debuggee extends SyncDebuggee {

    public static void main(String[] args) {
        runDebuggee(FieldModification002Debuggee.class);
    }

    public boolean testBoolField = false;
    public byte testByteField = 0;
    public char testCharField = 0;
    public short testShortField = 0;
    public int testIntField = 0;
    public long testLongField = 0;
    public float testFloatField = 0;
    public double testDoubleField = 0;
    public Object testObjectField = null;
    public Object testThreadField = null;
    public Object testThreadGroupField = null;
    public Object testClassField = null;
    public Object testClassLoaderField = null;
    public Object testStringField = null;
    public int [] testIntArrayField = null;
    public String [] testStringArrayField = null;
    public Object [] testObjectArrayField = null;

    /**
     * This debuggee accesses all class fields and synchronizes with debugger.
     */
    public void run() {

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        logWriter.println("FieldDebuggee started");

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testBoolField = true;
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testByteField = 'k';
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testCharField = 'Q';
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testShortField = 127;
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testIntField = -1001;
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testLongField = 4000000000000000L;
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testFloatField = 100*100*100;
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testDoubleField = -1.1/100000.999999;
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testObjectField = new File("none");

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testThreadField = new Thread("myThread001");
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testThreadGroupField = new ThreadGroup("MyThreadGroup_000001");
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testClassField = System.out.getClass();
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testClassLoaderField = this.getClass().getClassLoader();
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testStringField = "String=Strong=Strung";
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testIntArrayField = new int [] { 1, 2, 3, };
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testStringArrayField = new String[] { "abc", "xyz", };
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        testObjectArrayField = new Object[] { null, null, null, new Long(99), "objobjobj", };
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("FieldDebuggee finished");
    }
}
