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

import java.io.ObjectStreamException;

import junit.framework.TestCase;

public class EnumSyntaxTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(EnumSyntaxTest.class);
    }

    static {
        System.out.println("EnumSyntax testing...");
    }

    enumSyntax es;
    ExtendEnumSyntax ees;

    /*
     * clone() method testing.
     */
    public void testClone() {
        es = new enumSyntax(10);
        Object es2 = es.clone();
        assertEquals(es2, es);
    }


    /*
     * getEnumValueTable() method testing. 
     * Tests that getEnumValueTable() returns null as a default.
     */
    public void testGetEnumValueTable() {
        es = new enumSyntax(10);
        EnumSyntax[] esyntax = es.getEnumValueTableEx();
        assertNull(esyntax);
    }


    /*
     * getOffset() method testing. 
     * Tests that getOffset() returns 0 as a default.
     */
    public void testGetOffset() {
        es = new enumSyntax(10);
        int i = es.getOffsetEx();
        assertEquals(0, i);
    }


    /*
     * getStringTable() method testing. 
     * Tests that GetStringTable() returns null as a default.
     */
    public void testGetStringTable() {
        es = new enumSyntax(10);
        String[] str = es.getStringTableEx();
        assertNull(str);
    }


    /*
     * getValue() method testing. 
     * Tests that getValue() returns this enumeration value's integer value.
     */
    public void testGetValue() {
        es = new enumSyntax(10);
        int i = es.getValue();
        assertEquals(10, i);
     }


    /*
     * hashCode() method testing. 
     * Tests that hash code is this enumeration value's integer value.
     */
    public void testHashCode() {
        es = new enumSyntax(0);
        int i = es.hashCode();
        assertEquals(0, i);
    }

    /*
     * readResolve() method testing. 
     */
    public void testReadResolve() {
        ees = new ExtendEnumSyntax(1);
        try {
            ees.readResolveEx();
            fail("readResolve() doesn't throw InvalidObjectException if" +
                    "enumeration value table is null");
        } catch (ObjectStreamException e) {
            //System.out.println(e);
        }
    }

    /*
     * readResolve() method testing. 
     */
    public void testReadResolve1() {
        ees = new ExtendEnumSyntax(3);
        try {
            ees.readResolveEx();
            fail("readResolve() doesn't throw InvalidObjectException if" +
                    "integer value doesn't have corresponding enumeration value");
        } catch (ObjectStreamException e) {
            //System.out.println(e);
        }
    }

    /*
     * readResolve() method testing. 
     */
    public void testReadResolve2() {
        ees = new ExtendEnumSyntax(2);
        try {
            ees.readResolveEx();
            fail("readResolve() doesn't throw InvalidObjectException if" +
                "enumeration value is null");
        } catch (ObjectStreamException e) {
            //System.out.println(e);
        }
    }

    /*
     * toString() method testing. 
     * Tests that if there is no string value corresponding to this enumeration
     * value toString() returns string contains this enumeration value.
     */
    public void testToString() {
        ees = new ExtendEnumSyntax(2);
        assertEquals("2", ees.toString());
        es = new enumSyntax(10);
        assertEquals("10", es.toString());
    }

    /*
     * Auxiliary class
     */
    protected class enumSyntax extends EnumSyntax {

        public enumSyntax(int value) {
            super(value);
        }

        public EnumSyntax[] getEnumValueTableEx() {
            return getEnumValueTable();
        }

        public String[] getStringTableEx() {
            return getStringTable();
        }

        public int getOffsetEx() {
            return getOffset();
        }
    }


}
