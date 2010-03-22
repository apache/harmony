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
 * @author Aleksander V. Budniy
 */

/**
 * Created on 8.7.2005
 */
package org.apache.harmony.jpda.tests.jdwp.MultiSession;

import org.apache.harmony.jpda.tests.framework.TestOptions;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPUnitDebuggeeWrapper;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for verifying invalidating of classObjectID after re-connection.
 */
public class ClassObjectIDTest extends JDWPSyncTestCase {

    private String DEBUGGEE_SIGNATURE = "Lorg/apache/harmony/jpda/tests/jdwp/MultiSession/MultiSessionDebuggee;";

    //private String METHOD_NAME = "printWord";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.MultiSession.MultiSessionDebuggee";
    }

    /**
     * This testcase verifies invalidating of classObjectID after re-connection.
     * <BR>It runs multiSessionDebuggee, gets classID and classObjectID, re-connects
     * and tries to get refTypeID with ObjectReference.ReferenceType command
     * using old classObjectID. 
     * <BR>It is expected that INVALID_OBJECT error is returned by command.
     */
    public void testClassObjectID001() {

        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long classID = debuggeeWrapper.vmMirror.getClassID(DEBUGGEE_SIGNATURE);
        long classObjectID = debuggeeWrapper.vmMirror.getClassObjectId(classID);

        logWriter.println("");
        logWriter.println("=> CLOSE CONNECTION..");
        closeConnection();
        logWriter.println("=> CONNECTION CLOSED");

        logWriter.println("");
        logWriter.println("=> OPEN NEW CONNECTION..");
        openConnection();
        logWriter.println("=> CONNECTION OPENED");

        logWriter.println("=> Trying to get classID using old classObjectID");
        boolean success = false;
        try {
            //long requestID = 
                debuggeeWrapper.vmMirror.getReferenceType(classObjectID);
        } catch (Exception e) {
            logWriter.println("=> TEST PASSED, because INVALID_OBJECT exception was occurred");
            success = true;
        }

        if (!success) {
            logWriter.println("=> TEST FAILED, because INVALID_OBJECT exception was not occurred");
            fail("INVALID_OBJECT exception was not occurred");
        }

        //resuming debuggee
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    protected void beforeDebuggeeStart(JDWPUnitDebuggeeWrapper debuggeeWrapper) {
        settings.setAttachConnectorKind();
        if (settings.getTransportAddress() == null) {
            settings.setTransportAddress(TestOptions.DEFAULT_ATTACHING_ADDRESS);
        }
        logWriter.println("ATTACH connector kind");
        super.beforeDebuggeeStart(debuggeeWrapper);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ClassObjectIDTest.class);
    }
}
