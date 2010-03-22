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

import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.NumberUp;
import javax.print.attribute.standard.PagesPerMinute;

import junit.framework.TestCase;

public class IntegerSyntaxTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IntegerSyntaxTest.class);
    }


    static {
        System.out.println("IntegerSyntax testing...");
    }


    IntegerSyntax is1, is2;


    /*
     * IntegerSyntax(int value) constructor testing. 
     */
    public final void testIntegerSyntax_Int() {
        is1 = new integerSyntax(300);
        assertEquals(300, is1.getValue());
    }

    /*
     * IntegerSyntax(int value, int lowerBound, int upperBound) constructor testing. 
     */
    public final void testIntegerSyntax_IntIntInt() {
        try {
            is1 = new integerSyntax(-1, 1, 1);
            fail("Constructor IntegerSyntax(int value, int lowerBound, int upperBound) " +
                    "doesn't throw IllegalArgumentException " +
                    "if value is less than lowerBound");
        } catch (IllegalArgumentException e) {

        }

        try {
            is1 = new integerSyntax(2, 1, 1);
            fail("Constructor IntegerSyntax(int value, int lowerBound, int upperBound) " +
                    "doesn't throw IllegalArgumentException " +
                    "if value is greater than upperBound");
        } catch (IllegalArgumentException e) {

        }

        is1 = new integerSyntax(300, 1, 400);
        assertEquals(300, is1.getValue());
    }

    /*
     * hashCode() method testing. Tests if two object aren't equal they should
     * have different hash codes.
     */
    public final void testHashCode() {
        is1 = new integerSyntax(1000);
        is2 = new integerSyntax(1000-1);
        assertTrue(is2.hashCode() == 999);
        assertTrue(is1.hashCode() != is2.hashCode());
    }

    /*
     * hashCode() method testing. Tests if two object are equal they must have
     * the same hash code.
     */
    public final void testHashCode1() {
        is1 = new integerSyntax(1, 1, 10);
        is2 = new integerSyntax(1, 1, 15);
        assertTrue(is1.hashCode() == 1);
        assertTrue(is1.hashCode() == is2.hashCode());
    }

    /*
     * hashCode() method testing. Tests that hash code is just this integer 
     * attribute's integer value.
     */
    public final void testHashCode2() {
        is1 = new Copies(5);
        is2 = new NumberUp(5);
        assertTrue(is1.hashCode() == 5);
        assertTrue(is2.hashCode() == 5);
        assertTrue(is1.hashCode() == is2.hashCode());
    }

    /*
     * equals(Object object) method testing. Tests if two IntegerSyntax 
     * objects are equal equals() return true.
     */
    public final void testEquals() {
        is1 = new Copies(99);
        is2 = new Copies(99);
        assertTrue(is1.equals(is2));
    }

    /*
     * equals(Object object) method testing. Tests if two IntegerSyntax 
     * objects aren't equal equals() return false.
     */
    public final void testEquals1() {
        is1 = new Copies(99);
        is2 = new NumberUp(99);
        assertFalse(is1.equals(is2));

        is2 = null;
        assertFalse(is1.equals(is2));
    }

    /*
     * getValue() method testing. 
     */
    public final void testGetValue() {
        is1 = new PagesPerMinute(30);
        assertTrue(is1.getValue() == 30);
    }

    /*
     * Auxiliary class
     */
    protected class integerSyntax extends IntegerSyntax {

        public  integerSyntax(int value) {
            super(value);
        }

        public  integerSyntax(int value, int lowerBound, int upperBound) {
            super(value, lowerBound, upperBound);
        }
    }

}
