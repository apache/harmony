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
 * Created on 30.03.2005
 */
package org.apache.harmony.jpda.tests.jdwp.VirtualMachine;



import java.io.*;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

/**
 * JDWP Unit test for VirtualMachine.RedefineClasses command.
 */
public class RedefineClassesTest extends JDWPSyncTestCase {

    static final int testStatusPassed = 0;
    static final int testStatusFailed = -1;
    static final String thisCommandName = "VirtualMachine::RedefineClasses command";
    static final String checkedClassSignature
        = "Lorg/apache/harmony/jpda/tests/jdwp/VirtualMachine/RedefineClass_Debuggee;";
    static final String byteCodeToRedefineFile = "RedefineByteCode_Debuggee001";

    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.VirtualMachine.RedefineClassesDebuggee";
    }

    File findNewClassByteCode() {
        File foundFile = null;
        String nameSeparator = File.separator;
        String pathSeparator = File.pathSeparator;
        String byteCodeFileNameSuffix = "org" + nameSeparator + "apache" + nameSeparator
            + "harmony" + nameSeparator + "jpda" + nameSeparator + "tests" + nameSeparator
            + "jdwp" + nameSeparator + "VirtualMachine"
            + nameSeparator + byteCodeToRedefineFile;
        String byteCodeFileName = null;
        String classPaths = System.getProperty("java.class.path");
        int begPos = 0;
        int classPathsLength = classPaths.length();;

        for (int i = 0; i <= classPathsLength; i++) {
            if ( i == classPathsLength ) {
                if ( begPos == i ) {
                 break;   
                }
            } else {
                if ( ! pathSeparator.equals(classPaths.substring(i,i+1))) {
                    continue;
                }
                if ( begPos == i ) {
                    begPos++;
                    continue;
                }
            }
            byteCodeFileName = classPaths.substring(begPos,i);
            if ( ! nameSeparator.equals(classPaths.substring(i-1,i)) ) {
                byteCodeFileName = byteCodeFileName + nameSeparator;
            }
            byteCodeFileName = byteCodeFileName + byteCodeFileNameSuffix;
            foundFile = new File(byteCodeFileName);
            if ( foundFile.exists() ) {
                break;
            }
            foundFile = null;
            begPos = i+1;
        }
       return foundFile;
    }

    /**
     * This testcase exercises VirtualMachine.RedefineClasses command.
     * <BR>At first the test starts RedefineClassesDebuggee which invokes
     * the 'testMethod()' of RedefineClass_Debuggee class and prints the string
     * returned by this method before redefining.
     * <BR> Then the test performs VirtualMachine.RedefineClasses command
     * for RedefineClass_Debuggee class - the 'testMethod()' is redefined.
     * Next, the debuggee invokes the 'testMethod()' again and it is expected
     * that the method returns another resulting string.
     * The test checks that this resulting string is expected string.
     */
    public void testRedefineClasses001() {
        String thisTestName = "testClassObject001";
        
        //check capability, relevant for this test
        logWriter.println("=> Check capability: canRedefineClasses");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canRedefineClasses;
        if (!isCapability) {
            logWriter.println("##WARNING: this VM doesn't possess capability: canRedefineClasses");
            return;
        }
        
        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": START...");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        File newClassByteCodeFile = findNewClassByteCode();
        if ( newClassByteCodeFile == null ) {
            logWriter.println
            ("===> Can NOT find out byte code file for redefine:");
            logWriter.println
            ("===> File name = " + byteCodeToRedefineFile);
            logWriter.println
            ("===> Test can NOT be run!");
            logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
            return;
        }

        logWriter.println
        ("\n=> Send VirtualMachine::ClassesBySignature command and and get checked class referenceTypeID...");
        logWriter.println("=> checkedClassSignature = " + checkedClassSignature);
        CommandPacket classesBySignatureCommand = new CommandPacket(
            JDWPCommands.VirtualMachineCommandSet.CommandSetID,
            JDWPCommands.VirtualMachineCommandSet.ClassesBySignatureCommand);
        classesBySignatureCommand.setNextValueAsString(checkedClassSignature);
        
        ReplyPacket classesBySignatureReply = debuggeeWrapper.vmMirror.performCommand(classesBySignatureCommand);
        classesBySignatureCommand = null;

        checkReplyPacket(classesBySignatureReply, "VirtualMachine::ClassesBySignature command");

        int returnedClassesNumber = classesBySignatureReply.getNextValueAsInt();
        logWriter.println("=> ReturnedClassesNumber = " + returnedClassesNumber);  
        if ( returnedClassesNumber != 1 ) {
            // Number of returned reference types - is NOt used here
            printErrorAndFail("Unexpected number of classes is returned: " +
                    returnedClassesNumber +
                    ", Expected number = 1"); 
        }

        classesBySignatureReply.getNextValueAsByte();
        // refTypeTag of class - is NOt used here

        long refTypeID = classesBySignatureReply.getNextValueAsReferenceTypeID();
        classesBySignatureReply = null;

        logWriter.println("=> Checked class referenceTypeID = " + refTypeID);

        logWriter.println("\n=> Preparing info for " + thisCommandName);
        logWriter.println
        ("=> File name with new class byte code to redefine = " + byteCodeToRedefineFile);
        FileInputStream newClassByteCodeFileInputStream = null;
        try {
            newClassByteCodeFileInputStream = new FileInputStream(newClassByteCodeFile);
        } catch (Throwable thrown) {
            logWriter.println
            ("===> Can NOT create FileInputStream for byte code file:");
            logWriter.println("===> File name = " + byteCodeToRedefineFile);
            logWriter.println("===> Exception is thrown: " + thrown);
            logWriter.println("===> Test can NOT be run!");
            logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
            return;
        }
        int newClassByteCodeSize = 0;
        try {
            newClassByteCodeSize = (int)newClassByteCodeFileInputStream.skip(Long.MAX_VALUE);
        } catch (Throwable thrown) {
            logWriter.println
            ("===> Can NOT do FileInputStream.skip() to the end of file:");
            logWriter.println("===> File name = " + byteCodeToRedefineFile);
            logWriter.println("===> Exception is thrown: " + thrown);
            logWriter.println("===> Test can NOT be run!");
            logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
            return;
        }
        logWriter.println("=> newClassByteCodeSize = " + newClassByteCodeSize);
        try {
            newClassByteCodeFileInputStream.close();
        } catch (Throwable thrown) {
            logWriter.println
            ("===> WARNING: Can NOT close FileInputStream for byte code file:");
            logWriter.println("===> File name = " + byteCodeToRedefineFile);
            logWriter.println("===> Exception is thrown: " + thrown);
        }
        newClassByteCodeFileInputStream = null;
        
        try {
            newClassByteCodeFileInputStream = new FileInputStream(newClassByteCodeFile);
        } catch (Throwable thrown) {
            logWriter.println
            ("===> Can NOT re-create FileInputStream for byte code file:");
            logWriter.println("===> File name = " + byteCodeToRedefineFile);
            logWriter.println("===> Exception is thrown: " + thrown);
            logWriter.println("===> Test can NOT be run!");
            logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
            return;
        }

        CommandPacket checkedCommand = new CommandPacket(
            JDWPCommands.VirtualMachineCommandSet.CommandSetID,
            JDWPCommands.VirtualMachineCommandSet.RedefineClassesCommand);
        checkedCommand.setNextValueAsInt(1); // number of classes to redefine
        checkedCommand.setNextValueAsReferenceTypeID(refTypeID);
        checkedCommand.setNextValueAsInt(newClassByteCodeSize);
        int writtenBytes = 0;
        int currentByte = 0;
        while ( true ) {
            try {
                currentByte = newClassByteCodeFileInputStream.read();
            } catch (Throwable thrown) {
                logWriter.println
                ("===> Can NOT read current byte from byte code file:");
                logWriter.println("===> File name = " + byteCodeToRedefineFile);
                logWriter.println("===> Byte number = " + writtenBytes);
                logWriter.println("===> Exception is thrown: " + thrown);
                logWriter.println("===> Test can NOT be run!");
                logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
                return;
            }
            if ( currentByte == -1 ) { // EOF is reached
               break;
            }
            checkedCommand.setNextValueAsByte((byte)currentByte);
            writtenBytes++;
        }
        logWriter.println("=> Number of written bytes as new class file = " + writtenBytes);
        if ( newClassByteCodeSize != writtenBytes ) {
            logWriter.println("===> WARNING: Number of written bytes != newClassByteCodeSize");
            logWriter.println("===> Test can NOT be run!");
            logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
            return;
        }
        
        logWriter.println("\n=> Send " + thisCommandName + " and check reply...");
        
        ReplyPacket checkedReply = debuggeeWrapper.vmMirror.performCommand(checkedCommand);
        checkedCommand = null;
        int[] expectedErrors = {
            JDWPConstants.Error.NOT_IMPLEMENTED,   
        };
        short errorCode = checkedReply.getErrorCode();
        if ( errorCode != JDWPConstants.Error.NONE ) {
            if ( errorCode != JDWPConstants.Error.UNSUPPORTED_VERSION ) {
                finalSyncMessage = JPDADebuggeeSynchronizer.SGNL_CONTINUE;
                printErrorAndFail(
                    "## WARNING: A class file for redefine has a version number not supported by this VM" + 
                    "\n## File name with byte code = " + byteCodeToRedefineFile +
                    "\n## It should be re-created");
            }
            boolean expectedErr = false;
            for (int i=0; i < expectedErrors.length; i++) {
                if ( errorCode == expectedErrors[i] ) {
                    expectedErr = true;
                    break;
                }
            }
            if ( expectedErr ) {
                logWriter.println("=> " +  thisCommandName 
                        + " returns expected ERROR = " + errorCode 
                        + "(" + JDWPConstants.Error.getName(errorCode) + ")");
                logWriter.println
                    ("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
                return;
            } else {
                finalSyncMessage = JPDADebuggeeSynchronizer.SGNL_CONTINUE;
                printErrorAndFail(thisCommandName 
                    + " returns unexpected ERROR = " + errorCode 
                    + "(" + JDWPConstants.Error.getName(errorCode) + ")");
            }
        }
        logWriter.println("=> " +  thisCommandName + " returns reply without any error"); 

        assertAllDataRead(checkedReply);

        logWriter.println("\n=> Send Debuggee signal to continue and execute redefined testMethod");
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);

        String testMethodResult = synchronizer.receiveMessage();
        logWriter.println("=> Redefined testMethod result = \"" + testMethodResult + "\"");
        if ( testMethodResult.equals("testMethod_Result_After_Redefine") ) {
            logWriter.println("=> OK - it is expected result");
        } else {
            printErrorAndFail("it is NOT expected result" +
                    "\n Expected result = \"testMethod_Result_After_Redefine\"");
        }

        logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(RedefineClassesTest.class);
    }
}
