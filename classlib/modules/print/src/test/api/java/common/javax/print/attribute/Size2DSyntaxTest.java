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



public class Size2DSyntaxTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Size2DSyntaxTest.class);
    }

    static {
        System.out.println("Size2DSyntax testing...");
    }


    size2DSyntax ss1, ss2;


    /*
     * Size2DSyntax(float x, float y, int units) constructor testing. 
     */
    public final void testSize2DSyntax() {

        ss1 = new size2DSyntax(0.1f, 22.22f, Size2DSyntax.MM);
        assertTrue(100 == ss1.getXMicrometersEx());
        assertTrue(22220 == ss1.getYMicrometersEx());

        try {
            ss1 = new size2DSyntax(-0.01f, 700.45f, Size2DSyntax.MM);
            fail("Size2DSyntax(float x, float y, int units) " +
                    "doesn't throw IllegalArgumentException x<0");
        } catch (IllegalArgumentException e) {}

        try {
            ss1 = new size2DSyntax(0.1f, -700.45f, Size2DSyntax.MM);
            fail("Size2DSyntax(float x, float y, int units) " +
                    "doesn't throw IllegalArgumentException y<0");
        } catch (IllegalArgumentException e) {}

        try {
            ss1 = new size2DSyntax(0.1f, 0.1f, 0);
            fail("Size2DSyntax(float x, float y, int units) " +
                    "doesn't throw IllegalArgumentException units<1");
        } catch (IllegalArgumentException e) {}

    }

    /*
     * Size2DSyntax(int x, int y, int units) constructor testing. 
     */
    public final void testSize2DSyntax1() {

        ss1 = new size2DSyntax(1, 10, Size2DSyntax.INCH);
        assertTrue(25400 == ss1.getXMicrometersEx());
        assertTrue(254000 == ss1.getYMicrometersEx());

        try {
            ss1 = new size2DSyntax(-2, 1, Size2DSyntax.MM);
            fail("Size2DSyntax(float x, float y, int units) " +
                    "doesn't throw IllegalArgumentException x<0");
        } catch (IllegalArgumentException e) {}

        try {
            ss1 = new size2DSyntax(1, -1, Size2DSyntax.MM);
            fail("Size2DSyntax(float x, float y, int units) " +
                    "doesn't throw IllegalArgumentException y<0");
        } catch (IllegalArgumentException e) {}

        try {
            ss1 = new size2DSyntax(1, 1, -26);
            fail("Size2DSyntax(float x, float y, int units) " +
                    "doesn't throw IllegalArgumentException units<1");
        } catch (IllegalArgumentException e) {}
    }

    /*
     * hashCode() method testing. 
     */
    public final void testHashCode() {

        ss1 = new size2DSyntax(10, 10, 2);
        ss2 = new size2DSyntax(1, 1, 20);
        assertEquals(ss1.hashCode(), ss2.hashCode());

        ss1 = new size2DSyntax(10, 10, 2);
        ss2 = new size2DSyntax(10, 10, 20);
        assertFalse(ss1.hashCode() == ss2.hashCode());

    }

    /*
     * equals(Object object) method testing. 
     */
    public final void testEqualsObject() {
        ss1 = new size2DSyntax(10, 10, 2);
        ss2 = new size2DSyntax(1, 1, 20);
        assertTrue(ss1.equals(ss2));

        ss1 = new size2DSyntax(10, 10, 2);
        ss2 = new size2DSyntax(10, 10, 20);
        assertFalse(ss1.equals(ss2));

        ss2 = null;
        assertFalse(ss1.equals(ss2));
    }

    /*
     * getSize(int units) method testing. 
     */
    public final void testGetSize() {
        ss1 = new size2DSyntax(10.2f, 100f, 2);
        float[] size = ss1.getSize(2);
        assertEquals(10.2, size[0], 1);
        assertEquals(100, size[1], 1);
        try {
            ss1.getSize(0);
            fail("getSize(int units) " +
                    "doesn't throw IllegalArgumentException units<1");
        } catch (IllegalArgumentException e) {}
    }

    /*
     * getX(int units) method testing. 
     */
    public final void testGetX() {
        ss1 = new size2DSyntax(500.f, 800f, 1);
        assertEquals(62.5f, ss1.getX(8), 0.0001);

        ss1 = new size2DSyntax(105f, 800f, 10);
        assertEquals(5.25f, ss1.getX(200), 0.0001);

        try {
            ss1.getX(0);
            fail("getX(int units) " +
                    "doesn't throw IllegalArgumentException units<1");
        } catch (IllegalArgumentException e) {}
    }

    /*
     * getY(int units) method testing. 
     */
    public final void testGetY() {
        ss1 = new size2DSyntax(500, 700, 1);
        assertEquals(17.5, ss1.getY(40), 0.0001);

        ss1 = new size2DSyntax(500, 700, 10);
        assertEquals(583.3333, ss1.getY(12), 0.0001);

        try {
            ss1.getY(-44);
            fail("getY(int units) " +
                    "doesn't throw IllegalArgumentException units<1");
        } catch (IllegalArgumentException e) {}
    }

    /*
     * getXMicrometers() method testing. 
     */
    public final void testGetXMicrometers() {
        ss1 = new size2DSyntax(500.3f, 700.45f, 1);
        assertEquals(500, ss1.getXMicrometersEx());

        ss1 = new size2DSyntax(500.08f, 700.5f, 10);
        assertEquals(5001, ss1.getXMicrometersEx());
    }

    /*
     * getYMicrometers() method testing. 
     */
    public final void testGetYMicrometers() {
        ss1 = new size2DSyntax(500.3f, 700.45f, 1);
        assertEquals(700, ss1.getYMicrometersEx());

        ss1 = new size2DSyntax(500.3f, 700.07f, 10);
        assertEquals(7001, ss1.getYMicrometersEx());
    }

    /*
     * toString(int units, String unitsName) method testing. 
     */
    public final void testToStringintString() {
        size2DSyntax s = new  size2DSyntax(500f, 50f, 10);
        //System.out.println(s.toString(20, "mm"));
        try {
            s.toString(-1, "");
            fail("toString(int units, String unitsName) " +
                    "doesn't throw IllegalArgumentException units<1");
        } catch (IllegalArgumentException e) {}
    }


    /*
     * Auxiliary class
     */
    public class size2DSyntax extends  Size2DSyntax {

        public size2DSyntax(float a, float b, int c) {
            super(a, b, c);
        }

        public size2DSyntax(int a, int b, int c) {
            super(a, b, c);
        }

        protected int getXMicrometersEx() {
            return getXMicrometers();
        }

        protected int getYMicrometersEx() {
            return getYMicrometers();
        }
    }


}
