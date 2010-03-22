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


/*
 * @author esayapin
 */

public class PrinterStateReasonTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PrinterStateReasonTest.class);
    }
    
    static {
        System.out.println("PrinterStateReason testing...");
    }


    printerStateReason reason;


    /*
     * JobStateReason constructor testing.
     */
    public final void testFinishings() {

        reason = new printerStateReason(0);
        assertEquals(0, reason.getValue());

        reason = new printerStateReason(300);
        assertEquals(300, reason.getValue());
        assertEquals("300", reason.toString());
    }

    /*
     * getEnumValueTable(), getStringTable() methods testing.
     */
    public final void testGetStringTable() {

        int quantity = 0;
        reason = new printerStateReason(1);
        String[] str = reason.getStringTableEx();
        EnumSyntax[] table = reason.getEnumValueTableEx();
        assertEquals(str.length, table.length);
        assertEquals(33, table.length);

        //Tests that StringTable isn't changed for PrinterStateReason
        str = reason.getStringTableEx();
        str[1] = "reason1";
        //System.out.println(reason.getStringTable()[1]);
        assertFalse(reason.getStringTableEx()[1].equals("reason1"));
    }

    /*
     * getCategory() method testing.
     */
    public final void testGetCategory() {
        PrinterStateReason r = PrinterStateReason.DEVELOPER_LOW;
        assertEquals(PrinterStateReason.class, r.getCategory());
    }

    /*
     * getName() method testing.
     */
    public final void testGetName() {
        PrinterStateReason r = PrinterStateReason.COVER_OPEN;
        assertEquals("printer-state-reason", r.getName());
    }


    /*
     * Auxiliary class
     */
    public class printerStateReason extends PrinterStateReason {

        public printerStateReason(int value) {
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



