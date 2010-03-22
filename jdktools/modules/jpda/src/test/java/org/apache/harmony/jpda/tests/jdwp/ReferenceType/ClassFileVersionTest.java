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

package org.apache.harmony.jpda.tests.jdwp.ReferenceType;

import java.io.DataInputStream;
import java.io.FileInputStream;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;

public class ClassFileVersionTest extends JDWPSyncTestCase {

	static final int testStatusPassed = 0;

	static final int testStatusFailed = -1;

	static final String thisCommandName = "ReferenceType.ClassFileVersion command";

	static final String debuggeeSignature = "Lorg/apache/harmony/jpda/tests/jdwp/ReferenceType/ClassFileVersionDebuggee;";

	static final String debuggeeClass = "org/apache/harmony/jpda/tests/jdwp/ReferenceType/ClassFileVersionDebuggee.class";

	@Override
	protected String getDebuggeeClassName() {
		return "org.apache.harmony.jpda.tests.jdwp.ReferenceType.ClassFileVersionDebuggee";
	}

	/**
	 * This testcase exercises ReferenceType.ClassFileVersion command. <BR>
	 * The test starts ClassFileVersionDebuggee class, requests referenceTypeId
	 * for this class by VirtualMachine.ClassesBySignature command, then
	 * performs ReferenceType.ClassFileVersion command and checks that returned
	 * majorVersion and minorVersion are equal to expected values.
	 */
	public void testClassFileVersion001() {
		String thisTestName = "testClassFileVersion001";
		logWriter.println("==> " + thisTestName + " for " + thisCommandName
				+ ": START...");
		synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

		long refTypeID = getClassIDBySignature(debuggeeSignature);

		logWriter.println("=> Debuggee class = " + getDebuggeeClassName());
		logWriter.println("=> referenceTypeID for Debuggee class = "
				+ refTypeID);
		logWriter.println("=> CHECK: send " + thisCommandName
				+ " and check reply...");

		CommandPacket classFileVersionCommand = new CommandPacket(
				JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
				JDWPCommands.ReferenceTypeCommandSet.ClassFileVersionCommand);
		classFileVersionCommand.setNextValueAsReferenceTypeID(refTypeID);

		ReplyPacket classFileVersionReply = debuggeeWrapper.vmMirror
				.performCommand(classFileVersionCommand);
		classFileVersionCommand = null;
		checkReplyPacket(classFileVersionReply, thisCommandName);

		int majorVersion = classFileVersionReply.getNextValueAsInt();
		int minorVersion = classFileVersionReply.getNextValueAsInt();

		int expectedMinorVersion = -1;
		int expectedMajorVersion = -1;
		try {
			DataInputStream in = new DataInputStream(new FileInputStream(
					debuggeeClass));
			int magic = in.readInt();
			if (magic != 0xcafebabe) {
				printErrorAndFail(debuggeeClass + " is not a valid class!");
			}
			expectedMinorVersion = in.readUnsignedShort();
			expectedMajorVersion = in.readUnsignedShort();
			in.close();
		} catch (Exception e) {
			printErrorAndFail(thisCommandName + "has error in reading target class file!");
		}
		
		assertEquals(thisCommandName + "returned invalid majorVersion,", expectedMajorVersion, majorVersion, null, null);
		assertEquals(thisCommandName + "returned invalid minorVersion,", expectedMinorVersion, minorVersion, null, null);
		
		logWriter.println("=> CHECK: PASSED: expected majorVersion and minorVersion are returned:");
	    logWriter.println("=> majorVersion = " + majorVersion);
	    logWriter.println("=> minorVersion = " + minorVersion);

	    synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
	    logWriter.println("==> " + thisTestName + " for " + thisCommandName + ": FINISH");

	    assertAllDataRead(classFileVersionReply);

	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(ClassFileVersionTest.class);
	}
}
