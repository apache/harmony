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
 * @author Anatoly F. Bondarenko
 */

/**
 * Created on 24.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class InterfacesDebuggee extends SyncDebuggee {

    public void run() {
        CheckedClass_Interfaces001 checkedClass = new CheckedClass_Interfaces001();
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        logWriter.println("--> Debuggee: InterfacesDebuggee...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("--> Debuggee: DUMP{" + checkedClass + "}");
    }

    public static void main(String [] args) {
        runDebuggee(InterfacesDebuggee.class);
    }

}

interface Interface_1_1_Interfaces001 {}

interface Interface_1_Interfaces001 extends Interface_1_1_Interfaces001 {}

interface Interface_2_1_Interfaces001 {}

interface Interface_2_Interfaces001 extends Interface_2_1_Interfaces001 {}

class CheckedClass_Interfaces001 implements Interface_1_Interfaces001, Interface_2_Interfaces001 {}


