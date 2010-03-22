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
 * JDWP Unit test for ReferenceType.GetValues command for field from another class.
 */
public class GetValues004Test extends JDWPSyncTestCase {

    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/GetValues004Debuggee;";
    static final String anotherClassSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/RFGetValues004AnotherClass;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.GetValues004Debuggee";
    }

    /**
     * This testcase exercises ReferenceType.GetValues command for field from another class.
     * <BR>The test starts GetValues004Debuggee and checks that
     * ReferenceType.GetValues command runs correctly for field declaring
     * in some another class than passed to GetValues command ReferenceTypeID which is
     * not assignable from that another class.
     * <BR>Test expects that INVALID_FIELDID error is returned.
     */
    public void testGetValues004() {
        String thisTestName = "testGetValues004";
        logWriter.println("==> " + thisTestName + " for ReferenceType.GetValues command: START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        logWriter.println
        ("\n=> Get anotherClassRefTypeID for checkedClass class = RFGetValues003AnotherClass...");
        long anotherClassRefTypeID = 0;
        try {
            anotherClassRefTypeID = debuggeeWrapper.vmMirror.getClassID(anotherClassSignature);
        } catch ( Throwable thrown) {
            logWriter.println("## FAILURE: Can not get anotherClassRefTypeID:");
            logWriter.println("## Exception: " + thrown);
            fail("Can not get anotherClassRefTypeID, Exception: " + thrown);
        }
        if ( anotherClassRefTypeID == -1 ) {
            logWriter.println("## FAILURE: Can not get debuggeeRefTypeID for given signature!");
            logWriter.println("## Signature = |" + anotherClassSignature + "|");
            fail("Can not get debuggeeRefTypeID for given signature:<" + anotherClassSignature + ">");
        }
        logWriter.println("=> anotherClassRefTypeID = " + anotherClassRefTypeID);

        logWriter.println
        ("\n=> Get anotherClassCheckedFieldID for field of anotherClass...");
        String anotherClassCheckedFieldName = "anotherClassStaticIntVar";
        long anotherClassCheckedFieldID = 0;
        try {
            anotherClassCheckedFieldID
                = debuggeeWrapper.vmMirror.getFieldID(anotherClassRefTypeID, anotherClassCheckedFieldName);
        } catch ( Throwable thrown) {
            logWriter.println("## FAILURE: Can not get anotherClassCheckedFieldID:");
            logWriter.println("## Exception: " + thrown);
            fail("Can not get anotherClassCheckedFieldID, Exception: " + thrown);
        }
        logWriter.println("=> superClassCheckedFieldID = " + anotherClassCheckedFieldID);

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
        ("\n=> CHECK ReferenceType::GetValues command for debuggeeRefTypeID, anotherClassCheckedFieldID...");
        logWriter.println
        ("=> 'INVALID_FIELDID' error is expected as anotherClassCheckedField is not field of debuggee class!");
        CommandPacket getValuesCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        getValuesCommand.setNextValueAsReferenceTypeID(debuggeeRefTypeID);
        getValuesCommand.setNextValueAsInt(1);
        getValuesCommand.setNextValueAsFieldID(anotherClassCheckedFieldID);
        ReplyPacket getValuesReply = debuggeeWrapper.vmMirror.performCommand(getValuesCommand);
        short errorCode = getValuesReply.getErrorCode();
        if ( errorCode != JDWPConstants.Error.NONE ) {
            checkReplyPacket(getValuesReply, "ReferenceType::GetValues command", JDWPConstants.Error.INVALID_FIELDID);
            logWriter.println("=> CHECK PASSED: Expected error (INVALID_FIELDID) is returned");
            synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
            return;
        }
        logWriter.println
        ("## FAILURE: ReferenceType::GetValues command does NOT return expected error - INVALID_FIELDID");

        // next is only for extra info
        //int returnedValuesNumber =
            getValuesReply.getNextValueAsInt();
        Value fieldValue = getValuesReply.getNextValueAsValue();
        byte fieldTag = fieldValue.getTag();
        logWriter.println("## Returned value tag = " + fieldTag 
            + "(" + JDWPConstants.Tag.getName(fieldTag) + ")");
        if ( fieldTag == JDWPConstants.Tag.INT_TAG ) {
            int intValue = fieldValue.getIntValue();
            logWriter.println("## Returned value = " + intValue);
        }
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        fail("ReferenceType::GetValues command does NOT return expected error - INVALID_FIELDID");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GetValues004Test.class);
    }
}
