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
 * Created on 21.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

public class NestedTypesDebuggee extends SyncDebuggee {
    
    static interface StatInterf_1 {
        static interface StatInterf_1_1 {
        }
    }
    
    static class StatClass_1 {
        static class StatClass_1_1 {
        }
        StatClass_1_1 statClass_1_2_obj = new StatClass_1_1();
    }

    class NonStatClass_1 implements StatInterf_1 {
        class NonStatClass_1_1 implements StatInterf_1_1 {
        }
        NonStatClass_1_1 nonStatClass_1_2_obj = new NonStatClass_1_1();
    }

    void method_1() {
        Object obj1 = new Object() {
        };
        logWriter.println("--> Debuggee: DUMP{" + obj1 + "}");
    }

    public void run() {
        logWriter.println("--> Debuggee: NestedTypesDebuggee: START");
        StatClass_1 stat_Class_1_Obj = new StatClass_1();
        NonStatClass_1 nonStat_Class_1_Obj = new NonStatClass_1();
        method_1();
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("--> Debuggee: DUMP{" + 
                stat_Class_1_Obj + nonStat_Class_1_Obj + "}");
        logWriter.println("--> Debuggee: NestedTypesDebuggee: FINISH");
    }

    public static void main(String [] args) {
        runDebuggee(NestedTypesDebuggee.class);
    }

}

