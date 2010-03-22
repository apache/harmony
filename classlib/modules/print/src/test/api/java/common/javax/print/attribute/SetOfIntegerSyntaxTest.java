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

import javax.print.attribute.SetOfIntegerSyntax;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.NumberUpSupported;
import javax.print.attribute.standard.PageRanges;

import junit.framework.TestCase;

public class SetOfIntegerSyntaxTest extends TestCase {

    static{
        System.out.println("SetOfIntegerSyntax testing...");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SetOfIntegerSyntaxTest.class);
    }

    SetOfIntegerSyntax set1, set2;
    int[][] arr1, arr2;

    /*
     * setOfIntegerSyntax(String str) constructor testing. 
     */
    public final void testSetOfIntegerSyntax() {
        set1 = new setOfIntegerSyntax(
                " 16-37, 100:30, 17-50, 1000-1848, 1-2, 2147");
        set1 = new setOfIntegerSyntax("0");
        set1 = new setOfIntegerSyntax(
                "100       :       30,                  4");
        set1 = new setOfIntegerSyntax("000-1848");
        set1 = new setOfIntegerSyntax("");

        try {
            set1 = new setOfIntegerSyntax("17-50 1000-160");
            fail("IllegalArgumentException wasn't trown when expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            set1 = new setOfIntegerSyntax("-16:12");
            fail("IllegalArgumentException wasn't trown when expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            set1 = new setOfIntegerSyntax("a");
            fail("IllegalArgumentException wasn't trown when expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            //fails in "some" environment
            set1 = new setOfIntegerSyntax("19836475376");
            fail("IllegalArgumentException wasn't trown when expected");
        } catch (IllegalArgumentException e) {
        }

        set1 = new setOfIntegerSyntax(
                " 16-37, 100:30, 17:50, 1000-1848, 1-2, 214748364, 3");
        //System.out.println(set1.toString());
        arr1 = set1.getMembers();
        arr2 = new int[][] {{1,3}, {16,50}, {1000,1848}, {214748364, 214748364}};
        if (arr1.length == arr2.length) {
            for (int i=0; i<arr1.length; i++) {
                if ((arr1[i][0] != arr2[i][0]) || (arr1[i][1] != arr2[i][1])) {
                    fail("Wrong cannonical array");
                }
            }
        } else {
            fail("Wrong cannonical array");
        }

        set1 = new setOfIntegerSyntax(" ");
        arr1 = set1.getMembers();
        String str = null;
        set2 = new setOfIntegerSyntax(str);
        arr2 = set2.getMembers();
        if (arr1.length != arr2.length || arr1.length != 0 ) {
            fail("IllegalArgumentException wasn't trown when expected");
        }
        set1 = new setOfIntegerSyntax("15-2");
        assertEquals(0, set1.getMembers().length);

        try {
            set1 = new setOfIntegerSyntax("1-2-3");
            fail("IllegalArgumentException wasn't trown when expected");
        } catch (IllegalArgumentException e) {
        }

    }

    /*
     * setOfIntegerSyntax(int member) constructor testing. 
     */
    public final void testSetOfIntegerSyntax1() {
        try {
            set1 = new setOfIntegerSyntax(-1);
            fail("IllegalArgumentException wasn't trown when expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            set1 = new setOfIntegerSyntax(0);
            set2 = new setOfIntegerSyntax(1000);
            assertEquals(0, set1.getMembers()[0][0]);
            assertEquals(1000, set2.getMembers()[0][0]);
        } catch (IllegalArgumentException e) {
            fail("Unexpected exception " + e);
        }
    }

    /*
     * setOfIntegerSyntax(int[][] members) constructor testing. 
     * Tests that .
     */
    public final void testSetOfIntegerSyntax2() {
        try {
            int[] arr = null;
            arr1 = new int[][] {{9}, arr};
            set1 = new setOfIntegerSyntax(arr1);
            fail("NullPointerException wasn't trown when expected");
        } catch (NullPointerException e) {
        }
        try {
            arr1 = new int[][] {{9}, {4,5,6}};
            set1 = new setOfIntegerSyntax(arr1);
            fail("IllegalArgumentException wasn't trown when expected");
        } catch (IllegalArgumentException e) {
        }
        try {
            arr1 = new int[][] {{-9}, {4}};
            set1 = new setOfIntegerSyntax(arr1);
            fail("IllegalArgumentException wasn't trown when expected");
        } catch (IllegalArgumentException e) {
        }

        arr1 = new int[][] {{1,5}, {10}, {7,7}, {5,6}, {8,9}, {15,14}};
        set1 = new setOfIntegerSyntax(arr1);
        //System.out.println(set1.toString());
        assertEquals(1, set1.getMembers()[0][0]);
        assertEquals(10, set1.getMembers()[0][1]);

        arr1 = new int[][] {{15,14}};
        set1 = new setOfIntegerSyntax(arr1);
        assertEquals(0, set1.getMembers().length);

    }

    /*
     * setOfIntegerSyntax(int lowerBound, int upperBound) constructor testing. 
     */
    public final void testSetOfIntegerSyntax3() {
        try {
            set1 = new setOfIntegerSyntax(10, 10);
            assertEquals(10, set1.getMembers()[0][0]);
            assertEquals(10, set1.getMembers()[0][1]);
            set2 = new setOfIntegerSyntax(10, 1);
            assertEquals(0, set2.getMembers().length);
        } catch (IllegalArgumentException e) {
            fail("Unexpected exception occurred " + e);
        }
        try {
            arr1 = new int[][] {{-9}, {4}};
            set1 = new setOfIntegerSyntax(arr1);
            fail("IllegalArgumentException wasn't trown when expected");
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * setOfIntegerSyntax(String string) constructor testing. 
     */
    public final void testSetOfIntegerSyntax4() {
        String str = null;
        set1 = new setOfIntegerSyntax(str);
        assertTrue(0 == set1.getMembers().length);
    }

    /*
     * contains(int x) method testing. 
     */
    public final void testContains() {
        set1 = new setOfIntegerSyntax("0:8");
        assertTrue(set1.contains(0));
        assertTrue(set1.contains(8));
        assertFalse(set1.contains(9));
        assertFalse(set1.contains(-1));
    }

    /*
     * contains(IntegerSyntax attribute) method testing. 
     */
    public final void testContains1() {
        IntegerSyntax att1 = new Copies(10);
        IntegerSyntax att2 = new Copies(1);
        set1 = new setOfIntegerSyntax("10, 5-8");
        assertTrue(set1.contains(att1));
        assertFalse(set1.contains(att2));
    }

    /*
     * equals(Object object) method testing. 
     */
    public final void testEquals() {
        arr1 = new int[][] {{0,1}, {5}};
        set1 = new setOfIntegerSyntax(arr1);
        set2 = new setOfIntegerSyntax("0, 1, 5");
        assertTrue(set1.equals(set2));

        set1 = new PageRanges("2, 3-2");
        set2 = new PageRanges(2);
        assertTrue(set1.equals(set2));

        set1 = new NumberUpSupported(2, 10);
        set2 = new NumberUpSupported(2);
        assertFalse(set1.equals(set2));

        set1 = new setOfIntegerSyntax("100-1000");
        set2 = null;
        assertFalse(set1.equals(set2));
    }

    /*
     * getMembers() method testing. 
     */
    public final void testGetMembers() {
        set1 = new setOfIntegerSyntax("10, 5-8");
        arr1 = set1.getMembers();
        assertEquals(5, arr1[0][0]);
        assertEquals(8, arr1[0][1]);
        assertEquals(10, arr1[1][0]);
        assertEquals(10, arr1[1][1]);
    }

    /*
     * hashCode() method testing. 
     * Tests that hash code is the sum of the lower and upper bounds of 
     * the ranges in the canonical array form, or 0 for an empty set.
     */
    public final void testHashCode() {
        set1 = new setOfIntegerSyntax("0, 1, 5, 10-15");
        assertEquals(36, set1.hashCode());
        set1 = new setOfIntegerSyntax(" ");
        assertEquals(0, set1.hashCode());
        set1 = new setOfIntegerSyntax("5");
        assertEquals(10, set1.hashCode());
    }

    /*
     * next() method testing.
     */
    public final void testNext() {
        set1 = new setOfIntegerSyntax(" 5-8, 10, 25, 100-130, 250");
        assertEquals(10, set1.next(8));
        assertEquals(5, set1.next(1));
        assertEquals(-1, set1.next(250));
    }


    /*
     * Auxiliary class
     */
    public class setOfIntegerSyntax extends SetOfIntegerSyntax {

        public setOfIntegerSyntax(int lowerBound, int upperBound) {
            super(lowerBound, upperBound);
        }

        public setOfIntegerSyntax(String str) {
            super(str);
        }

        public setOfIntegerSyntax(int i) {
            super(i);
        }

        public setOfIntegerSyntax(int[][] arr) {
            super(arr);
        }
    }

}
