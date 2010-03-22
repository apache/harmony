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
 * @author Denis M. Kishenko
 */
package java.awt.geom;

import java.awt.Rectangle;

import junit.framework.TestCase;

public class Rectangle2DDoubleTest extends TestCase {

    Rectangle2D.Double r;

    public Rectangle2DDoubleTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        r = new Rectangle2D.Double(1, 2, 3, 4);
    }

    @Override
    protected void tearDown() throws Exception {
        r = null;
        super.tearDown();
    }

    public void testCreate() {
        assertEquals(new Rectangle2D.Double(0, 0, 0, 0), new Rectangle2D.Double());
    }

    public void testGetX() {
        assertEquals(1.0f, r.getX(), 0.0f);
    }

    public void testGetY() {
        assertEquals(2.0f, r.getY(), 0.0f);
    }

    public void testGetWidth() {
        assertEquals(3.0f, r.getWidth(), 0.0f);
    }

    public void testGetHeight() {
        assertEquals(4.0f, r.getHeight(), 0.0f);
    }

    public void testIsEmpty() {
        assertFalse(r.isEmpty());
        assertTrue(new Rectangle2D.Double(1, 2, -1, 1).isEmpty());
        assertTrue(new Rectangle2D.Double(3, 4, 1, -1).isEmpty());
        assertTrue(new Rectangle2D.Double(3, 4, 1, 0).isEmpty());
        assertTrue(new Rectangle2D.Double(5, 6, -1, -1).isEmpty());
    }

    public void testSetRect1() {
        r.setRect(5.0, 6.0, 7.0, 8.0);
        assertEquals(new Rectangle2D.Double(5, 6, 7, 8), r);
    }

    public void testSetRect2() {
        r.setRect(5.0f, 6.0f, 7.0f, 8.0f);
        assertEquals(new Rectangle2D.Double(5, 6, 7, 8), r);
    }

    public void testSetRect3() {
        r.setRect(new Rectangle(5, 6, 7, 8));
        assertEquals(new Rectangle2D.Double(5, 6, 7, 8), r);
    }

    public void testOutcode() {
        assertEquals(Rectangle2D.OUT_LEFT | Rectangle2D.OUT_TOP,     r.outcode(0, 0));
        assertEquals(Rectangle2D.OUT_TOP,                            r.outcode(2, 0));
        assertEquals(Rectangle2D.OUT_RIGHT | Rectangle2D.OUT_TOP,    r.outcode(5, 0));
        assertEquals(Rectangle2D.OUT_RIGHT,                          r.outcode(5, 3));
        assertEquals(Rectangle2D.OUT_RIGHT | Rectangle2D.OUT_BOTTOM, r.outcode(5, 7));
        assertEquals(Rectangle2D.OUT_BOTTOM,                         r.outcode(2, 7));
        assertEquals(Rectangle2D.OUT_LEFT | Rectangle2D.OUT_BOTTOM,  r.outcode(0, 7));
        assertEquals(Rectangle2D.OUT_LEFT,                           r.outcode(0, 3));
        assertEquals(0, r.outcode(2, 3));
    }

    public void testGetBounds2D() {
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4), r.getBounds2D());
    }

    public void testToString() {
        assertEquals("java.awt.geom.Rectangle2D$Double[x=1.0,y=2.0,width=3.0,height=4.0]", r.toString());
    }

    public void testCreateIntersection() {
        assertEquals(new Rectangle2D.Float(1, 2, -1, -2),  r.createIntersection(new Rectangle2D.Float()));            // Empty
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4),   r.createIntersection(new Rectangle2D.Double(1, 2, 3, 4))); // The same
        assertEquals(new Rectangle2D.Float(2, 3, 1, 2),    r.createIntersection(new Rectangle2D.Float(2, 3, 1, 2)));  // Inside
        assertEquals(new Rectangle2D.Double(5, 7, -1, -1), r.createIntersection(new Rectangle2D.Double(5, 7, 1, 2))); // Outside
        assertEquals(new Rectangle2D.Float(2, 3, 2, 3),    r.createIntersection(new Rectangle2D.Float(2, 3, 5, 6)));  // Intersect
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4),   r.createIntersection(new Rectangle2D.Double(0, 0, 5, 6))); // Cover
    }

    public void testCreateUnion() {
//      assertEquals(r.createUnion(new Rectangle2D.Double()), new Rectangle2D.Double(1, 2, 3, 4));           // Empty
        assertEquals(new Rectangle2D.Float(1, 2, 3, 4), r.createUnion(new Rectangle2D.Float(1, 2, 3, 4)));   // The same
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4), r.createUnion(new Rectangle2D.Double(2, 3, 1, 2))); // Inside
        assertEquals(new Rectangle2D.Float(1, 2, 5, 7), r.createUnion(new Rectangle2D.Float(5, 7, 1, 2)));   // Outside
        assertEquals(new Rectangle2D.Double(1, 2, 6, 7), r.createUnion(new Rectangle2D.Double(2, 3, 5, 6))); // Intersect
        assertEquals(new Rectangle2D.Float(0, 0, 5, 6), r.createUnion(new Rectangle2D.Float(0, 0, 5, 6)));   // Cover
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Rectangle2DDoubleTest.class);
    }

}
