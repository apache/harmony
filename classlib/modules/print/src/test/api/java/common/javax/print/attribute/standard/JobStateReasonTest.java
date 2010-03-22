/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/** 
 * @author Elena V. Sayapina 
 */ 

package javax.print.attribute.standard;

import javax.print.attribute.EnumSyntax;

import junit.framework.TestCase;

public class JobStateReasonTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(JobStateReasonTest.class);
    }

    static {
        System.out.println("JobStateReason testing...");
    }

    jobStateReason reason;


    /*
     * JobStateReason constructor testing.
     */
    public final void testFinishings() {

        reason = new jobStateReason(20);
        assertEquals(20, reason.getValue());

        reason = new jobStateReason(40);
        assertEquals(40, reason.getValue());
        assertEquals("40", reason.toString());
    }

    /*
     * getEnumValueTable(), getStringTable() methods testing.
     */
    public final void testGetStringTable() {

        int quantity = 0;
        reason = new jobStateReason(5);
        String[] str = reason.getStringTableEx();
        EnumSyntax[] table = reason.getEnumValueTableEx();
        assertEquals(str.length, table.length);
        assertTrue(29 == table.length);

        //Tests that StringTable isn't changed for JobStateReason
        reason = new jobStateReason(1);
        str = reason.getStringTableEx();
        str[1] = "reason1";
        //System.out.println(reason.getStringTable()[1]);
        assertFalse(reason.getStringTableEx()[1].equals("reason1"));
    }

    /*
     * getCategory() method testing.
     */
    public final void testGetCategory() {
        JobStateReason jsreason = JobStateReason.ABORTED_BY_SYSTEM;
        assertEquals(JobStateReason.class, jsreason.getCategory());
    }

    /*
     * getName() method testing.
     */
    public final void testGetName() {
        JobStateReason jsreason = JobStateReason.UNSUPPORTED_DOCUMENT_FORMAT;
        assertEquals("job-state-reason", jsreason.getName());
    }


    /*
     * Auxiliary class
     */
    public class jobStateReason extends JobStateReason {

        public jobStateReason(int value) {
            super(value);
        }

        public String[] getStringTableEx() {
            return getStringTable();
        }

        public EnumSyntax[] getEnumValueTableEx() {
            return getEnumValueTable();
        }
    }


}
