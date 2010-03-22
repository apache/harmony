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

package javax.print.attribute;

import java.util.Date;

import junit.framework.TestCase;

public class DateTimeSyntaxTest extends TestCase {


    public static void main(String[] args) {
        junit.textui.TestRunner.run(DateTimeSyntaxTest.class);
    }

    static {
        System.out.println("DateTimeSyntax testing...");
    }

    private DateTimeSyntax dts1, dts2;
    private Date date;


    /*
     * DateTimeSyntax(Date date) constructor testing. 
     */
    public final void testDateTimeSyntax() {

        date = null;
        try {
            dts1 = new dateTimeSyntax(date);
            fail("DateTimeSyntax(Date date) doesn't throw " +
                    "NullPointerException if date is null");
        } catch (NullPointerException e) {

        }
    }

    /*
     * hashCode() method testing. Tests if two object are equal they must have
     * the same hash code.
     */
    public final void testHashCode() {
        date = new Date((long) Math.pow(10, 12));
        dts1 = new dateTimeSyntax(date);
        dts2 = new dateTimeSyntax(date);
        assertTrue(dts1.hashCode() == dts2.hashCode());
    }

    /*
     * hashCode() method testing. Tests if two object aren't equal they should
     * have different hash codes.
     */
    public final void testHashCode1() {
        date = new Date((long) Math.pow(10, 12));
        dts1 = new dateTimeSyntax(date);
        date = new Date();
        dts2 = new dateTimeSyntax(date);
        assertFalse(dts1.hashCode() == dts2.hashCode());
    }

    /*
     * equals(Object object) method testing. Tests if two DateTimeSyntax 
     * objects aren't equal equals() return false.
     */
    public final void testEquals() {

        date = new Date((long) Math.pow(10, 12));
        dts1 = new dateTimeSyntax(date);
        date = new Date((long) Math.pow(10, 12)-1);
        dts2 = new dateTimeSyntax(date);
        assertFalse(dts1.equals(dts2));


        dts2 = null;
        assertFalse(dts1.equals(dts2));
    }

    /*
     * equals(Object object) method testing. Tests if two DateTimeSyntax 
     * objects are equal equals() return true.
     */
    public final void testEquals1() {

        date = new Date();
        dts1 = new dateTimeSyntax(date);
        dts2 = new dateTimeSyntax(date);
        assertTrue(dts1.equals(dts2));
    }

    /*
     * getValue() method testing.
     */
    public final void testGetValue() {
        date = new Date();
        dts1 = new dateTimeSyntax(date);
        assertEquals(date, dts1.getValue());

        date = new Date((long) Math.pow(10, 12));
        dts2 = new dateTimeSyntax(date);
        assertEquals(date, dts2.getValue());
    }

    /*
     * Auxiliary class
     */
    public class dateTimeSyntax extends DateTimeSyntax {

        public dateTimeSyntax(Date value) {
            super(value);
        }
    }


}
