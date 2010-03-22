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

import junit.framework.TestCase;

public class MediaPrintableAreaTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MediaPrintableAreaTest.class);
    }

    static {
        System.out.println("MediaPrintableArea testing...");
    }

    MediaPrintableArea mpa1, mpa2;

    /*
     * MediaPrintableArea(int x, int y, int width, int height, 
     * int units)  constructors testing.
     */
    public void testMediaPrintableArea() {
        try {
            mpa1 = new MediaPrintableArea(20, 20, 100, 100, 0);
            fail("Constructor doesn't throw IllegalArgumentException if " +
                    "units < 1");
        } catch (IllegalArgumentException e) {

        }
        try {
            mpa1 = new MediaPrintableArea(-2, 2, 1, 1, 1);
            fail("Constructor doesn't throw IllegalArgumentException if " +
                    "x < 0");
        } catch (IllegalArgumentException e) {

        }
    }

    /*
     * MediaPrintableArea(float x, float y, float width, float height, 
     * int units)  constructors testing.
     */
    public void testMediaPrintableArea1() {

         mpa1 = new MediaPrintableArea(0.4f, 0.5f, 9.9f, 3.4f, 1);
         assertEquals(0.0f, mpa1.getX(1), 0.001f);
         assertEquals(1.0f, mpa1.getY(1), 0.001f);
         assertEquals(3.0f, mpa1.getHeight(1), 0.001f);
         assertEquals(10.0f, mpa1.getWidth(1), 0.001f);
    }

    /*
     * hashCode() method testing. Tests if two object is the same they must 
     * have the same hashCodes.
     */
    public void testHashCode() {
        mpa1 = new MediaPrintableArea(20, 20, 100, 100, MediaPrintableArea.INCH);
        mpa2 = new MediaPrintableArea(20, 20, 100, 100, MediaPrintableArea.INCH);
        assertEquals(mpa1.hashCode(), mpa2.hashCode());
    }

    /*
     * hashCode() method testing. Tests if two object is different they should 
     * have different hashCodes.
     */
    public void testHashCode1() {
        mpa1 = new MediaPrintableArea(20, 20, 100, 100, MediaPrintableArea.INCH);
        mpa2 = new MediaPrintableArea(20, 20, 100, 100, MediaPrintableArea.MM);
        assertFalse(mpa1.hashCode() == mpa2.hashCode());
    }

    /*
     * equals(Object object) method testing.
     */
    public void testEqualsObject() {

        mpa1 = new MediaPrintableArea(1, 1, 1, 1, MediaPrintableArea.INCH);
        mpa2 = new MediaPrintableArea(25400, 25400, 25400, 25400, 1);
        assertTrue(mpa1.equals(mpa2));

        mpa1 = new MediaPrintableArea(20, 20, 10, 10, MediaPrintableArea.INCH);
        mpa2 = new MediaPrintableArea(20, 20, 10, 10, MediaPrintableArea.MM);
        assertFalse(mpa1.equals(mpa2));

        mpa2 = null;
        assertFalse(mpa1.equals(mpa2));
    }

    /*
     * getCategory() method testing.
     */
    public void testGetCategory() {
        mpa1 = new MediaPrintableArea(1, 1, 1, 1, 1);
        assertEquals(MediaPrintableArea.class, mpa1.getCategory());
    }

    /*
     * getName() method testing.
     */
    public void testGetName() {
        mpa1 = new MediaPrintableArea(1, 1, 1, 1, 1);
        assertEquals("media-printable-area", mpa1.getName());
    }

    /*
     * getX(), getY(), getHeight(), getHeight() methods testing.
     */
    public void testGetX() {
        mpa1 = new MediaPrintableArea(1, 100, 1, 1, 1);
        assertEquals(0.01f, mpa1.getX(100), 0.01f);
        assertEquals(1.0f, mpa1.getY(100), 0.01f);
        assertEquals(0.333f, mpa1.getWidth(3), 0.001f);
        assertEquals(0.05f, mpa1.getHeight(20), 0.01f);
    }


    /*
     * toString() method checking.
     */
    public void testToString() {

        mpa1 = new MediaPrintableArea(20.0f, 20.0f, 100.0f, 100.0f, 254000);
        //System.out.println(mpa1.toString(254000, "inch"));

        mpa1 = new MediaPrintableArea(20,20,100,100, 1000);
        //System.out.println(mpa1.toString());
    }

}
