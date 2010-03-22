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

import junit.framework.TestCase;


public class ResolutionSyntaxTest extends TestCase {

    static {
        System.out.println("ResolutionSyntax testing...");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ResolutionSyntaxTest.class);
    }

    resolutionSyntax rs1, rs2;

    /*
     * ResolutionSyntax(int crossFeedResolution, int feedResolution, 
     * int units) constructor testing. 
     */
    public final void testResolutionSyntax() {
        try {
            rs1 = new resolutionSyntax(1, 1, 0);
            fail("ResolutionSyntax(int crossFeedResolution, " +
                    "int feedResolution, int units) doesn't throw " +
                    "IllegalArgumentException if units < 1");
        } catch (IllegalArgumentException e) {}

        try {
            rs1 = new resolutionSyntax(-1, 1, 1);
            fail("ResolutionSyntax(int crossFeedResolution, " +
                    "int feedResolution, int units) doesn't throw " +
                    "IllegalArgumentException if " +
                    "crossFeedResolution < 1");
        } catch (IllegalArgumentException e) {}
        try {
            rs1 = new resolutionSyntax(1, 0, 10);
            fail("ResolutionSyntax(int crossFeedResolution, " +
                    "int feedResolution, int units) doesn't throw " +
                    "IllegalArgumentException if " +
                    "or feedResolution < 1");
        } catch (IllegalArgumentException e) {}

        rs1 = new resolutionSyntax(10001, 1507, resolutionSyntax.DPI);
        assertEquals(10001, rs1.getCrossFeedResolution(resolutionSyntax.DPI));
        assertEquals(1507, rs1.getFeedResolution(resolutionSyntax.DPI));
    }

    /*
     * hashCode() method testing. Tests if two object aren't equal they should
     * have different hash codes.
     */
    public final void testHashCode() {
        rs1 = new resolutionSyntax(4444, 333, resolutionSyntax.DPCM);
        rs2 = new resolutionSyntax(333, 444, resolutionSyntax.DPCM);
        assertTrue(rs1.hashCode() != rs2.hashCode());
    }

    /*
     * hashCode() method testing. Tests if two object are equal they must have
     * the same hash code.
     */
    public final void testHashCode1() {
        rs1 = new resolutionSyntax(1, 1, resolutionSyntax.DPCM);
        rs2 = new resolutionSyntax(1, 1, resolutionSyntax.DPCM);
        assertTrue(rs1.hashCode() == rs2.hashCode());
    }

    /*
     * equals(Object object) method testing. Tests if two object are equal 
     * they must have the same hash code.
     */
    public final void testEqualsObject() {
        rs1 = new resolutionSyntax(1, 1, resolutionSyntax.DPCM);
        rs2 = new resolutionSyntax(1, 1, resolutionSyntax.DPCM);
        assertTrue(rs1.equals(rs2));

        rs1 = new resolutionSyntax(1, 1, resolutionSyntax.DPCM);
        rs2 = new resolutionSyntax(1, 1, resolutionSyntax.DPI);
        assertFalse(rs1.equals(rs2));

        rs1 = new resolutionSyntax(1000, 2000, resolutionSyntax.DPCM);
        rs2 = new resolutionSyntax(1000, 2000, resolutionSyntax.DPCM);
        assertTrue(rs1.equals(rs2));

        rs1 = new resolutionSyntax(1000, 2000, resolutionSyntax.DPI);
        rs2 = new resolutionSyntax(1000, 2000-1, resolutionSyntax.DPCM);
        assertFalse(rs1.equals(rs2));

        rs2 = null;
        assertFalse("equals(object o) returns true if o is null",
                    rs1.equals(rs2));
    }

    public final void testGetCrossFeedResolution() {
        rs1 = new resolutionSyntax(1000, 2000, 1);
        assertEquals(1000, rs1.getCrossFeedResolution(1));
        try {
            rs1.getCrossFeedResolution(0);
            fail("getCrossFeedResolution(int units) doesn't throw " +
                    "IllegalArgumentException if units < 1");
        } catch (IllegalArgumentException e) {}
    }

    public final void testGetCrossFeedResolutionDphi() {
        rs1 = new resolutionSyntax(1000, 2000, 3);
        assertEquals(3000, rs1.getCrossFeedResolutionDphiEx());
        rs1 = new resolutionSyntax(1000, 2000, 1);
        assertEquals("bad rounding", 333, rs1.getCrossFeedResolution(3));
    }

    public final void testGetFeedResolution() {
        rs1 = new resolutionSyntax(1000, 2000, 1);
        assertEquals(2000, rs1.getFeedResolution(1));
        try {
            rs1.getFeedResolution(-100);
            fail("getFeedResolution(int units) doesn't throw " +
                    "IllegalArgumentException if units < 1");
        } catch (IllegalArgumentException e) {}
    }

    public final void testGetFeedResolutionDphi() {
        rs1 = new resolutionSyntax(1000, 2000, 3);
        assertEquals(6000, rs1.getFeedResolutionDphiEx());
        rs1 = new resolutionSyntax(1000, 500, 1);
        assertEquals("bad rounding", 63, rs1.getFeedResolution(8));
    }

    public final void testGetResolution() {
        rs1 = new resolutionSyntax(1000, 2000, resolutionSyntax.DPCM);
        assertEquals(1000, rs1.getResolution(resolutionSyntax.DPCM)[0]);
        assertEquals(2000, rs1.getResolution(resolutionSyntax.DPCM)[1]);
        try {
            rs1.getResolution(0);
            fail("getResolution(int units) doesn't throw " +
                    "IllegalArgumentException if units < 1");
        } catch (IllegalArgumentException e) {}
    }


    public final void testLessThanOrEquals() {
        rs1 = new resolutionSyntax(1000, 1000, 1);
        rs2 = new resolutionSyntax(999, 1000, 1);
        assertTrue(rs2.lessThanOrEquals(rs1));

        rs1 = new resolutionSyntax(1, 1, resolutionSyntax.DPCM);
        rs2 = new resolutionSyntax(1, 2, resolutionSyntax.DPCM);
        assertFalse(rs2.lessThanOrEquals(rs1));

        try {
            rs2 = null;
            rs1.lessThanOrEquals(rs2);
            fail("lessThanOrEquals(ResolutionSyntax other) " +
                    "doesn't throw NullPointerException " +
                    "if other is null");
        } catch (NullPointerException e) {}
    }


    public final void testToString() {
        rs1 = new resolutionSyntax(500, 50, 10);
        assertEquals("5000x500 dphi", rs1.toString());
        //System.out.println(rs1.toString());
    }


    public final void testToStringintString() {
        rs1 = new resolutionSyntax(1024, 1034, 254);
        assertEquals("1024x1034 dpcm", rs1.toString(254, "dpcm"));
        //System.out.println(rs1.toString(254, "dpcm"));
    }

    /*
     * Auxiliary class
     */
    public class resolutionSyntax extends ResolutionSyntax {

        public  resolutionSyntax(int a, int b, int c) {
            super(a, b, c);
        }

        public int getCrossFeedResolutionDphiEx() {
            return getCrossFeedResolutionDphi();
        }

        public int getFeedResolutionDphiEx() {
            return getFeedResolutionDphi();
        }
    }

}
