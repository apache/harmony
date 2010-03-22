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
 * JDWP Unit test for Method.VariableTable command.
 */
public class VariableTableTest extends JDWPMethodTestCase {
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(VariableTableTest.class);
    }

    /**
     * This testcase exercises Method.VariableTable command.
     * <BR>It runs MethodDebuggee, receives methods of debuggee. 
     * For each received method sends Method.VariableTable command
     * and prints returned VariableTable.
     */
    public void testVariableTableTest001() throws UnsupportedEncodingException {
        logWriter.println("testVariableTableTest001 started");
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        
        long classID = getClassIDBySignature("L"+getDebuggeeClassName().replace('.', '/')+";");

        MethodInfo[] methodsInfo = jdwpGetMethodsInfo(classID);
        assertFalse("Invalid number of methods: 0", methodsInfo.length == 0);
        
        for (int i = 0; i < methodsInfo.length; i++) {
            logWriter.println(methodsInfo[i].toString());
            
            // get variable table for this class
            CommandPacket packet = new CommandPacket(
                    JDWPCommands.MethodCommandSet.CommandSetID,
                    JDWPCommands.MethodCommandSet.VariableTableCommand);
            packet.setNextValueAsClassID(classID);
            packet.setNextValueAsMethodID(methodsInfo[i].getMethodID());
            ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
            checkReplyPacket(reply, "Method::VariableTable command");

            int argCnt = reply.getNextValueAsInt();
            logWriter.println("argCnt = "+argCnt);
            int slots = reply.getNextValueAsInt();
            logWriter.println("slots = "+slots);
            for (int j = 0; j < slots; j++) {
                long codeIndex = reply.getNextValueAsLong();
                logWriter.println("codeIndex = "+codeIndex);
                String name = reply.getNextValueAsString();
                logWriter.println("name = "+name);
                String signature = reply.getNextValueAsString();
                logWriter.println("signature = "+signature);
                int length = reply.getNextValueAsInt();
                logWriter.println("length = "+length);
                int slot = reply.getNextValueAsInt();
                logWriter.println("slot = "+slot);
            }
            
        }
        
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
    }
}
