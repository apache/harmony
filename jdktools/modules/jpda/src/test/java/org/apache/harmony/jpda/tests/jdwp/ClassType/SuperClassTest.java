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
 * Created on 09.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ClassType;

import java.io.UnsupportedEncodingException;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;



/**
 * JDWP unit test for ClassType.SuperClass command.
 * Contains three testcases: testSuperClass001, testSuperClass002, testSuperClass003.
 */
public class SuperClassTest extends JDWPClassTypeTestCase {

    /**
     * Starts this test by junit.textui.TestRunner.run() method.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(SuperClassTest.class);
    }

    private ReplyPacket jdwpGetSuperClassReply(long classID, int errorExpected) {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ClassTypeCommandSet.CommandSetID,
                JDWPCommands.ClassTypeCommandSet.SuperclassCommand);
        packet.setNextValueAsClassID(classID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        checkReplyPacket(reply, "ClassType.Superclass command", errorExpected);
        return reply;
    }

    private void asserSuperClassReplyIsValid(ReplyPacket reply, String expectedSignature) {
        assertTrue(reply.getErrorCode() == JDWPConstants.Error.NONE);
        long superClassID = reply.getNextValueAsClassID();
        logWriter.println("superClassID=" + superClassID);
        if (superClassID == 0) {
            // for superclass of Object expectedSignature is null
            assertNull
            ("ClassType::Superclass command returned invalid expectedSignature that must be null",
                    expectedSignature);
        } else {
            String signature = getClassSignature(superClassID);
            logWriter.println("Signature: "+signature);
            assertString("ClassType::Superclass command returned invalid signature,",
                    expectedSignature, signature);
        }
    }

    /**
     * This testcase exercises ClassType.Superclass command.
     * <BR>Starts <A HREF="ClassTypeDebuggee.html">ClassTypeDebuggee</A>. 
     * <BR>Then does the following checks: 
     * <BR>&nbsp;&nbsp; - superclass for java.lang.String is java.lang.Object;
     * <BR>&nbsp;&nbsp; - superclass for array of Objects is java.lang.Object;
     * <BR>&nbsp;&nbsp; - superclass for primitive array is java.lang.Object;
     * <BR>&nbsp;&nbsp; - superclass for <A HREF="ClassTypeDebuggee.html">ClassTypeDebuggee</A>
     * class is <A HREF="../../share/SyncDebuggee.html">SyncDebuggee</A> class.;
     */
    public void testSuperClass001() throws UnsupportedEncodingException {
        logWriter.println("testSuperClassTest001 started");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        
        // check that superclass for java.lang.String is java.lang.Object
        {
            // test with String[] class
            long classID = getClassIDBySignature("Ljava/lang/String;");

            ReplyPacket reply = jdwpGetSuperClassReply(classID, JDWPConstants.Error.NONE);
            // complare returned signature with superclass signature
            asserSuperClassReplyIsValid(reply, "Ljava/lang/Object;");
        }

        // check that superclass for array is java.lang.Object
        {
            // test with String[] class
            long classID = getClassIDBySignature("[Ljava/lang/String;");

            ReplyPacket reply = jdwpGetSuperClassReply(classID, JDWPConstants.Error.NONE);
            // complare returned signature with superclass signature
            asserSuperClassReplyIsValid(reply, "Ljava/lang/Object;");
        }

        // check that superclass for primitive array is java.lang.Object
        {
            // test with int[] class
            long classID = getClassIDBySignature("[I");

            ReplyPacket reply = jdwpGetSuperClassReply(classID, JDWPConstants.Error.NONE);
            // complare returned signature with superclass signature
            asserSuperClassReplyIsValid(reply, "Ljava/lang/Object;");
        }

        // check that superclass for Debuggee is SyncDebuggee
        {
            long classID = getClassIDBySignature(getDebuggeeSignature());

            ReplyPacket reply = jdwpGetSuperClassReply(classID, JDWPConstants.Error.NONE);
            // complare returned signature with superclass signature
            asserSuperClassReplyIsValid(reply, "Lorg/apache/harmony/jpda/tests/share/SyncDebuggee;");
        }

        // check that there is no superclass for java.lang.Object
        {
            // test with java.lang.Object class
            long classID = getClassIDBySignature("Ljava/lang/Object;");

            ReplyPacket reply = jdwpGetSuperClassReply(classID, JDWPConstants.Error.NONE);
            // complare returned signature with superclass signature 
            // (expects null for this case) 
            asserSuperClassReplyIsValid(reply, null);
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * This testcase exercises ClassType.Superclass command.
     * <BR>Starts <A HREF="ClassTypeDebuggee.html">ClassTypeDebuggee</A>. 
     * <BR>Then does the following checks: 
     * <BR>&nbsp;&nbsp; - there is no superclass for interface;
     * <BR>&nbsp;&nbsp; - INVALID_OBJECT is returned if classID is non-existent;
     * <BR>&nbsp;&nbsp; - INVALID_OBJECT is returned if instead of classID FieldID is passed;
     */
    public void testSuperClass002() throws UnsupportedEncodingException {
        logWriter.println("testSuperClassTest002 started");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // check that there is no superclass for interface objects
        {
            long interfaceID = getClassIDBySignature("Ljava/lang/Cloneable;");

            ReplyPacket reply = jdwpGetSuperClassReply(interfaceID, JDWPConstants.Error.NONE);
            // compare returned signature with superclass signature
            // (null for interfaces)
            asserSuperClassReplyIsValid(reply, null);
        }

        // check that INVALID_OBJECT returns if classID is non-existent
        {
            jdwpGetSuperClassReply(10000
                , JDWPConstants.Error.INVALID_OBJECT);
        }

        // check that reply error code is INVALID_OBJECT for a FieldID Out Data
        {
            long classID = getClassIDBySignature(getDebuggeeSignature());

            FieldInfo[] fields = jdwpGetFields(classID);
            // assert stringID is not null
            assertTrue("Invalid fields.length: 0", fields.length > 0);
            // test with the first field
            
            jdwpGetSuperClassReply(fields[0].getFieldID()
                , JDWPConstants.Error.INVALID_OBJECT);
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }

    /**
     * This testcase exercises ClassType.Superclass command.
     * <BR>Starts <A HREF="ClassTypeDebuggee.html">ClassTypeDebuggee</A>. 
     * <BR>Then does the following check: 
     * <BR>&nbsp;&nbsp; - INVALID_CLASS is returned if instead of classID ObjectID is passed;
     */
    public void testSuperClass003() throws UnsupportedEncodingException {
        logWriter.println("testSuperClassTest003 started");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        // check that reply error code is INVALID_CLASS for a StringID Out Data
        {
            long stringID = createString("Some test string");
            // assert stringID is not null
            assertFalse("Invalid stringID: 0", stringID == 0);
            jdwpGetSuperClassReply(stringID, JDWPConstants.Error.INVALID_CLASS);
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }
}