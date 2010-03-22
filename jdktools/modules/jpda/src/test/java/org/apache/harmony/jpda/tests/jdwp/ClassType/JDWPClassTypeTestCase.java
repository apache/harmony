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
 * Created on 11.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.ClassType;

import org.apache.harmony.jpda.tests.framework.jdwp.CommandPacket;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPCommands;
import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;
import org.apache.harmony.jpda.tests.framework.jdwp.ReplyPacket;
import org.apache.harmony.jpda.tests.jdwp.share.JDWPSyncTestCase;


/**
 * Super class of some JDWP unit tests for JDWP ClassType command set.
 */
public class JDWPClassTypeTestCase extends JDWPSyncTestCase {

    /**
     * Returns full name of debuggee class which is used by
     * testcases in this test.
     * @return full name of debuggee class.
     */
    protected String getDebuggeeClassName() {
        return "org.apache.harmony.jpda.tests.jdwp.ClassType.ClassTypeDebuggee";
    }

    /**
     * Returns signature of debuggee class which is used by
     * testcases in this test.
     * @return signature of debuggee class.
     */
    protected String getDebuggeeSignature() {
      return "Lorg/apache/harmony/jpda/tests/jdwp/ClassType/ClassTypeDebuggee;";
    }

    class FieldInfo {
        private long fieldID;
        private String name;
        private String signature;
        private int modBits;

        /**
         * @return Returns the fieldID.
         */
        public long getFieldID() {
            return fieldID;
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

        public FieldInfo(long fieldID, String name, String signature,
                int modBits) {
            super();
            this.fieldID = fieldID;
            this.name = name;
            this.signature = signature;
            this.modBits = modBits;
        }

        public String toString() {
            return "fieldID=" + fieldID + "; name='" + name + "'; signature='" + signature
            + "'; modbits=" + modBits;
        }

    }

    /**
     * Returns for specified class array with information about fields of this class.
     * <BR>Each element of array contains: 
     * <BR>Field ID, Field name, Field signature, Field modifier bit flags; 
     * @param refType - ReferenceTypeID, defining class.
     * @return array with information about fields.
     */
    protected FieldInfo[] jdwpGetFields(long refType) {
        CommandPacket packet = new CommandPacket(
                JDWPCommands.ReferenceTypeCommandSet.CommandSetID,
                JDWPCommands.ReferenceTypeCommandSet.FieldsCommand);
        packet.setNextValueAsReferenceTypeID(refType);
        
        ReplyPacket reply = debuggeeWrapper.vmMirror.performCommand(packet);
        assertTrue(reply.getErrorCode() == JDWPConstants.Error.NONE);
        
        int declared = reply.getNextValueAsInt();
        FieldInfo[] fields = new FieldInfo[declared];
        for (int i = 0; i < declared; i++) {
            fields[i] =
                new FieldInfo(reply.getNextValueAsFieldID(),
                              reply.getNextValueAsString(),
                              reply.getNextValueAsString(),
                              reply.getNextValueAsInt());
        }
        return fields;
    }

}