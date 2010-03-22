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
 * Created on 14.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.Method;

import java.io.UnsupportedEncodingException;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;



/**
 * JDWP Unit test for Method.Bytecodes command.
 */
public class BytecodesTest extends JDWPMethodTestCase {
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(BytecodesTest.class);
    }

    /**
     * This testcase exercises Method.Bytecodes command.
     * <BR>It runs MethodDebuggee. Gets methods with ReferenceType.Methods command,
     * prints it's bytecodes received with Method.Bytecodes command.
     */
    public void testBytecodesTest001() throws UnsupportedEncodingException {
        logWriter.println("testBytecodesTest001 started");
        
        //check capability, relevant for this test
        logWriter.println("=> Check capability: canGetBytecodes");
        debuggeeWrapper.vmMirror.capabilities();
        boolean isCapability = debuggeeWrapper.vmMirror.targetVMCapabilities.canGetBytecodes;
        if (!isCapability) {
            logWriter.println("##WARNING: this VM doesn't possess capability: canGetBytecodes");
            return;
        }
        
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);

        long classID = getClassIDBySignature("L"+getDebuggeeClassName().replace('.', '/')+";");

        MethodInfo[] methodsInfo = jdwpGetMethodsInfo(classID);
        assertFalse("Invalid number of methods", methodsInfo.length == 0);

        for (int i = 0; i < methodsInfo.length; i++) {
            logWriter.println(methodsInfo[i].toString());
           
            // get variable table for this class
            CommandPacket packet = new CommandPacket(
                    JDWPCommands.MethodCommandSet.CommandSetID,
                    JDWPCommands.MethodCommandSet.BytecodesCommand);
            packet.setNextValueAsClassID(classID);
            packet.setNextValueAsMethodID(methodsInfo[i].getMethodID());
            ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(reply, "Method::Bytecodes command");

            int bytes = reply.getNextValueAsInt();
            logWriter.println("bytes = " + bytes);
            
            byte[] bytecode = new byte[bytes];
            for (int j = 0; j < bytes; j++) {
                bytecode[j] = reply.getNextValueAsByte();
            }

            logWriter.println("Bytecode=" + new String(bytecode));
        }

        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }
}
