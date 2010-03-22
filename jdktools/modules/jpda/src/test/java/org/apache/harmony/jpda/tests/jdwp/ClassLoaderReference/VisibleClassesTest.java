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
 * @author Viacheslav G. Rybalov
 */

/**
 * Created on 05.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ClassLoaderReference;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;


/**
 * JDWP unit test for ClassLoaderReference.VisibleClasses command.
 */
public class VisibleClassesTest extends JDWPSyncTestCase {

    /**
     * Returns full name of debuggee class which is used by this test.
     * @return full name of debuggee class.
     */
    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";
    }

    /**
     * This testcase exercises ClassLoaderReference.VisibleClasses command.
     * <BR>Starts <A HREF="../share/debuggee/HelloWorld.html">HelloWorld</A> debuggee. 
     * <BR>Then the following statements are checked: 
     * <BR>It is expected:
     * <BR>&nbsp;&nbsp; - number of reference types has non-zero value;
     * <BR>&nbsp;&nbsp; - refTypeTag takes one of the TypeTag constants: CLASS, INTERFACE, ARRAY;
     * <BR>&nbsp;&nbsp; - All data were read;
     */
    public void testVisibleClasses001() {
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ClassLoaderReferenceCommandSet.CommandSetID,
                JDWPCommands.ClassLoaderReferenceCommandSet.VisibleClassesCommand);
        packet.setNextValueAsClassLoaderID(0);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ClassLoaderReference::VisibleClasses command");

        byte refTypeTag;
        String refTypeTagName;
        long typeID;
        String msgLine;

        int classes = reply.getNextValueAsInt();
        logWriter.println("Number of reference types = " + classes);
        assertTrue("Invalid returned number of reference types = " + classes, classes > 0);

        int printBound_1 = classes;
        int printBound_2 = 0;
        if (classes > 50) {
            printBound_1 = 5;
            printBound_2 = classes - 5;
        }
        for (int i = 0; i < classes; i++) {
            refTypeTag = reply.getNextValueAsByte();
            refTypeTagName = JDWPConstants.TypeTag.getName(refTypeTag);
            msgLine = "" + i + ".  refTypeTag = " + refTypeTagName + "(" + refTypeTag + ")";
            typeID = reply.getNextValueAsReferenceTypeID();
            msgLine = msgLine + ";  referenceTypeID = " + typeID;
            if ( (i < printBound_1) || (i >= printBound_2) ) {
                logWriter.println(msgLine);
                if ( i == (printBound_1 - 1) ) {
                    logWriter.println("...\n...\n...");
                }
            }
            assertTrue("Unexpected reference TagType:<" + refTypeTag + "(" + refTypeTagName + ")>",
                    refTypeTag == JDWPConstants.TypeTag.ARRAY
                    || refTypeTag == JDWPConstants.TypeTag.CLASS
                    || refTypeTag == JDWPConstants.TypeTag.INTERFACE);
        }

        assertAllDataRead(reply);
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * Starts this test by junit.textui.TestRunner.run() method.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(VisibleClassesTest.class);
    }
}
