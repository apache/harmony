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
 * Created on 20.05.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.Value;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP Unit test for ReferenceType.GetValues command for field of super class.
 */
public class GetValues003Test extends JDWPSyncTestCase {

    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/GetValues003Debuggee;";
    static final String chekedClassSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/RFGetValues003CheckedClass;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.GetValues003Debuggee";
    }

    /**
     * This testcase exercises ReferenceType.GetValues command for field of super class.
     * <BR>The test starts GetValues003Debuggee and checks that
     * ReferenceType.GetValues command runs correctly for field declaring
     * in super class of passed to GetValues command ReferenceTypeID.
     * <BR>Test checks that expected value of this field is returned.
     */
    public void testGetValues003() {
        String thisTestName = "testGetValues003";
        logWriter.println("==> " + thisTestName + " for ReferenceType.GetValues command: START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        logWriter.println
        ("\n=> Get debuggeeRefTypeID for debuggee class = " + getDebuggeeClassName() + "...");
        long debuggeeRefTypeID = 0;
        try {
            debuggeeRefTypeID = debuggeeWrapper.vmMirror.getClassID(debuggeeSignature);
        } catch ( Throwable thrown) {
            logWriter.println("## FAILURE: Can not get debuggeeRefTypeID:");
            logWriter.println("## Exception: " + thrown);
            fail("Can not get debuggeeRefTypeID, Exception: " + thrown);
        }
        if ( debuggeeRefTypeID == -1 ) {
            logWriter.println("## FAILURE: Can not get debuggeeRefTypeID for given signature!");
            logWriter.println("## Signature = |" + debuggeeSignature + "|");
            fail("Can not get debuggeeRefTypeID for given signature:<" + debuggeeSignature + ">");
        }
        logWriter.println("=> debuggeeRefTypeID = " + debuggeeRefTypeID);

        logWriter.println
        ("\n=> Get superClassCheckedFieldID for field of debuggee class...");
        String superClassCheckedFieldName = "superClassStaticIntVar";
        long superClassCheckedFieldID = 0;
        try {
            superClassCheckedFieldID
                = debuggeeWrapper.vmMirror.getFieldID(debuggeeRefTypeID, superClassCheckedFieldName);
        } catch ( Throwable thrown) {
            logWriter.println("## FAILURE: Can not get superClassCheckedFieldID:");
            logWriter.println("## Exception: " + thrown);
            fail("Can not get superClassCheckedFieldID, Exception: " + thrown);
        }
        logWriter.println("=> superClassCheckedFieldID = " + superClassCheckedFieldID);

        logWriter.println
        ("\n=> Get chekedClassRefTypeID for chekedClass class = RFGetValues003CheckedClass...");
        long chekedClassRefTypeID = 0;
        try {
            chekedClassRefTypeID = debuggeeWrapper.vmMirror.getClassID(chekedClassSignature);
        } catch ( Throwable thrown) {
            logWriter.println("## FAILURE: Can not get chekedClassRefTypeID:");
            logWriter.println("## Exception: " + thrown);
            fail(" Can not get chekedClassRefTypeID, Exception: " + thrown);
        }
        if ( chekedClassRefTypeID == -1 ) {
            logWriter.println("## FAILURE: Can not get chekedClassRefTypeID for given signature!");
            logWriter.println("## Signature = |" + chekedClassSignature + "|");
            fail("Can not get chekedClassRefTypeID for given signature:<" + chekedClassSignature + ">");
        }
        logWriter.println("=> chekedClassRefTypeID = " + chekedClassRefTypeID);

        logWriter.println
        ("\n=> CHECK ReferenceType::GetValues command for chekedClassRefTypeID, superClassCheckedFieldID...");
        CommandPacket getValuesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        getValuesCommand.setNextValueAsReferenceTypeID(chekedClassRefTypeID);
        getValuesCommand.setNextValueAsInt(1);
        getValuesCommand.setNextValueAsFieldID(superClassCheckedFieldID);
        ReplyPacket getValuesReply = debuggeeWrapper.vmMirror.performCommand(getValuesCommand);
        checkReplyPacket(getValuesReply, "ReferenceType::GetValues command");
        
        //int returnedValuesNumber =
            getValuesReply.getNextValueAsInt();
        Value fieldValue = getValuesReply.getNextValueAsValue();
        byte fieldTag = fieldValue.getTag();
        logWriter.println("=> Returned value tag = " + fieldTag 
            + "(" + JDWPConstants.Tag.getName(fieldTag) + ")");
        assertEquals("Invalid value tag is returned,", JDWPConstants.Tag.INT_TAG, fieldTag
                , JDWPConstants.Tag.getName(JDWPConstants.Tag.INT_TAG)
                , JDWPConstants.Tag.getName(fieldTag));
        
        int intValue = fieldValue.getIntValue();
        logWriter.println("=> Returned value = " + intValue);
        // here expected value = 99 (staticIntField)
        int expectedIntValue = 99;
        assertEquals("Invalid int value,", expectedIntValue, intValue);

        assertAllDataRead(getValuesReply);

        logWriter.println("=> CHECK PASSED: Expected value is returned!");

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for ReferenceType::GetValues command: FINISH");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GetValues003Test.class);
    }
}
