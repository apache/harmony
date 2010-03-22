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

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;


public class JDWPMethodTestCase extends JDWPSyncTestCase {

    protected String getDebuggeeClassName() {
        return MethodDebuggee.class.getName();
    }

    static class MethodInfo {
        private long methodID;
        private String name;
        private String signature;
        private int modBits;

        public MethodInfo(long methodID, String name, String signature,
                int modBits) {
            super();
            this.methodID = methodID;
            this.name = name;
            this.signature = signature;
            this.modBits = modBits;
        }

        /**
         * @return Returns the methodID.
         */
        public long getMethodID() {
            return methodID;
        }
        /**
         * @return Returns the modBits.
         */
        public int getModBits() {
            return modBits;
        }
        /**
         * @return Returns the name.
         */
        public String getName() {
            return name;
        }
        /**
         * @return Returns the signature.
         */
        public String getSignature() {
            return signature;
        }
        public String toString() {
            return ""+methodID+" "+name+" "+signature+" "+modBits;
        }
        
    }

    protected MethodInfo[] jdwpGetMethodsInfo(long classID) {
        
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.MethodsCommand);
        packet.setNextValueAsClassID(classID);
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);

        assertTrue(reply.getErrorCode() == JDWPConstants.Error.NONE);
        int declared = reply.getNextValueAsInt();
        
        MethodInfo[] methodsInfo = new MethodInfo[declared];
        for (int i = 0; i < declared; i++) {
            methodsInfo[i] = new MethodInfo(
                reply.getNextValueAsMethodID(),
                reply.getNextValueAsString(),
                reply.getNextValueAsString(),
                reply.getNextValueAsInt()
            );
        }
        return methodsInfo;
    }
}
