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
 * Created on 13.07.2005
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
 * JDWP Unit test for ReferenceType.GetValues command for fields with null value.
 */
public class GetValues005Test extends JDWPSyncTestCase {

    static final String thisCommandName = "ReferenceType.GetValues command";
    static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/GetValues005Debuggee;";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.GetValues005Debuggee";
    }

    /**
     * This testcase exercises ReferenceType.GetValues command for fields with null value.
     * <BR>The test starts GetValues005Debuggee class and performs
     * ReferenceType.GetValues command for fields of different 
     * referenceTypes with value=null for all fields.
     * <BR>The test expects the all returned values should be represented by expected
     * JDWP tag with null value.
     */
    public void testGetValues005() {
        String thisTestName = "testGetValues005";
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long debuggeeRefTypeID = getClassIDBySignature(debuggeeSignature);

        logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
        logWriter.println("=> referenceTypeID for Debuggee class = " + debuggeeRefTypeID);

        String checkedFieldNames[] = {
                "intArrayField",
                "objectArrayField",
                "objectField",
                "stringField",
                "threadField",
                "threadGroupField",
                "classField",
                "classLoaderField",
        };

        long checkedFieldIDs[] = checkFields(debuggeeRefTypeID, checkedFieldNames);
        int checkedFieldsNumber = checkedFieldNames.length;

        logWriter.println
        ("=> CHECK: send " + thisCommandName 
        + " for Debuggee class for fields of different referenceTypes with with null values...");

        CommandPacket checkedCommand = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.GetValuesCommand);
        checkedCommand.setNextValueAsObjectID(debuggeeRefTypeID);
        checkedCommand.setNextValueAsInt(checkedFieldsNumber);
        for (int i = 0; i < checkedFieldsNumber; i++) {
            checkedCommand.setNextValueAsFieldID(checkedFieldIDs[i]);
        }
        ReplyPacket checkedReply =
            debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;

        checkReplyPacket(checkedReply, thisCommandName);
        
        int returnedValuesNumber = checkedReply.getNextValueAsInt();
        logWriter.println("=> Returned values number = " + returnedValuesNumber);
        if ( checkedFieldsNumber != returnedValuesNumber ) {
            logWriter.println
            ("\n## FAILURE: ReferenceType::GetValues command returned unexpected number of values:");
            logWriter.println("## Expected number = " + checkedFieldsNumber);
            logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
            assertTrue(false);
        }
        byte expectedFieldTags[] = {
                JDWPConstants.Tag.ARRAY_TAG,
                JDWPConstants.Tag.ARRAY_TAG,
                JDWPConstants.Tag.OBJECT_TAG,
                JDWPConstants.Tag.STRING_TAG,
                JDWPConstants.Tag.THREAD_TAG,
                JDWPConstants.Tag.THREAD_GROUP_TAG,
                JDWPConstants.Tag.CLASS_OBJECT_TAG,
                JDWPConstants.Tag.CLASS_LOADER_TAG,
        };
        logWriter.println("=> CHECK for returned values...");
        for (int i=0; i < returnedValuesNumber; i++) {
            Value fieldValue = checkedReply.getNextValueAsValue();
            byte fieldTag = fieldValue.getTag();
            logWriter.println
            ("\n=> Check for returned value for field: " + checkedFieldNames[i] + " ...");
            logWriter.println("=> Returned value tag = " + fieldTag 
                + "(" + JDWPConstants.Tag.getName(fieldTag) + ")");
            if ( (fieldTag != expectedFieldTags[i]) && (fieldTag != JDWPConstants.Tag.OBJECT_TAG) ) {
                logWriter.println("\n## FAILURE:  Unexpected value tag is returned");
                logWriter.println("## Expected value tag = " + expectedFieldTags[i]
                + "(" + JDWPConstants.Tag.getName(expectedFieldTags[i]) + ")"
                + " or = " + JDWPConstants.Tag.OBJECT_TAG
                + "(" + JDWPConstants.Tag.getName(JDWPConstants.Tag.OBJECT_TAG) + ")");
                //testStatus = testStatusFailed;
                fail("Unexpected value tag is returned");
            }
            long objectIDValue = fieldValue.getLongValue();
            logWriter.println("=> ObjectId value = " + objectIDValue);
            assertEquals("Invalid objectID value is returned,", 0, objectIDValue);
        }

        assertAllDataRead(checkedReply);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": OK.");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GetValues005Test.class);
    }
}
