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
 * @author Dmitry A. Durnev, Denis M. Kishenko
 */
package java.awt;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class RectangleTest extends SerializeTestCase {

    static { 
        SERIALIZATION_TEST = true;
    }
    
    Rectangle r;

    public RectangleTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        r = new Rectangle(1, 2, 3, 4);
    }

    @Override
    protected void tearDown() throws Exception {
        r = null;
        super.tearDown();
    }

    public void testCreate1() {
        assertEquals(new Rectangle(0, 0, 0, 0), new Rectangle());
    }

    public void testCreate2() {
        assertEquals(new Rectangle(0, 0, 5, 6), new Rectangle(5, 6));
    }

    public void testCreate3() {
        assertEquals(new Rectangle(0, 0, 5, 6), new Rectangle(new Dimension(5, 6)));
    }

    public void testCreate4() {
        assertEquals(new Rectangle(5, 6, 0, 0), new Rectangle(new Point(5, 6)));
    }

    public void testCreate5() {
        assertEquals(new Rectangle(5, 6, 7, 8), new Rectangle(new Point(5, 6), new Dimension(7, 8)));
    }

    public void testCreate6() {
        assertEquals(new Rectangle(5, 6, 7, 8), new Rectangle(new Rectangle(5, 6, 7, 8)));
    }

    public void testGetX() {
        assertEquals(1, (int)r.getX());
    }

    public void testGetY() {
        assertEquals(2, (int)r.getY());
    }

    public void testGetWidth() {
        assertEquals(3, (int)r.getWidth());
    }

    public void testGetHeight() {
        assertEquals(4, (int)r.getHeight());
     }

    public void testIsEmpty() {
        assertTrue(!new Rectangle(1, 2, 3, 4).isEmpty());
        assertTrue(new Rectangle(1, 2, -3, 4).isEmpty());
        assertTrue(new Rectangle(1, 2, 3, -4).isEmpty());
        assertTrue(new Rectangle(1, 2, -3, -4).isEmpty());
    }

    public void testEquals() {
        assertTrue(r.equals(new Rectangle(1, 2, 3, 4)));
        assertTrue(!r.equals(new Rectangle(0, 2, 3, 4)));
        assertTrue(!r.equals(new Rectangle(1, 0, 3, 4)));
        assertTrue(!r.equals(new Rectangle(1, 2, 0, 4)));
        assertTrue(!r.equals(new Rectangle(1, 2, 3, 0)));
    }

    public void testToString() {
        assertEquals("java.awt.Rectangle[x=1,y=2,width=3,height=4]", r.toString());
    }

    public void testGetSize() {
        assertEquals(new Dimension(3, 4), r.getSize());
    }

    public void testSetSize1() {
        r.setSize(new Dimension(5, 6));
        assertEquals(new Dimension(5, 6), r.getSize());
    }

    public void testSetSize2() {
        r.setSize(5, 6);
        assertEquals(new Dimension(5, 6), r.getSize());
    }

    public void testGetLocation() {
        assertEquals(new Point(1, 2), r.getLocation());
    }

    public void testSetLocation1() {
        r.setLocation(5, 6);
        assertEquals(new Point(5, 6), r.getLocation());
    }

    public void testSetLocation2() {
        r.setLocation(new Point(5, 6));
        assertEquals(new Point(5, 6), r.getLocation());
    }

    @SuppressWarnings("deprecation")
    public void testMove() {
        r.move(5, 6);
        assertEquals(new Point(5, 6), r.getLocation());
    }

    public void testSetRect() {
        r.setRect(5, 6, 7, 8);
        assertEquals(new Rectangle(5, 6, 7, 8), r);
        r.setRect(9.3, 10.7, 11.5, 12.5);
        assertEquals(new Rectangle(9, 10, 12, 14), r);
        r.setRect(13.2, 14.7, 15.3, 16.4);
        assertEquals(new Rectangle(13, 14, 16, 18), r);
    }

    @SuppressWarnings("deprecation")
    public void testResize() {
        r.resize(5, 6);
        assertEquals(new Rectangle(1, 2, 5, 6), r);
    }

    @SuppressWarnings("deprecation")
    public void testReshape() {
        r.reshape(5, 6, 7, 8);
        assertEquals(new Rectangle(5, 6, 7, 8), r);
    }

    public void testGetBounds() {
        assertEquals(new Rectangle(1, 2, 3, 4), r.getBounds());
    }

    public void testGetBounds2D() {
        assertEquals(new Rectangle(1, 2, 3, 4), r.getBounds2D());
    }

    public void setBounds1() {
        r.setBounds(5, 6, 7, 8);
        assertEquals(new Rectangle(5, 6, 7, 8), r.getBounds());
    }

    public void setBounds2() {
        r.setBounds(new Rectangle(5, 6, 7, 8));
        assertEquals(new Rectangle(5, 6, 7, 8), r.getBounds());
    }

    public void testGrow() {
        r.grow(0, 0);
        assertEquals(new Rectangle(1, 2, 3, 4), r);
        r.grow(1, 1);
        assertEquals(new Rectangle(0, 1, 5, 6), r);
        r.grow(-1, -1);
        assertEquals(new Rectangle(1, 2, 3, 4), r);
    }

    public void testTranslate(){
        r.translate(0, 0);
        assertEquals(new Point(1, 2), r.getLocation());
        r.translate(5, 6);
        assertEquals(new Point(6, 8), r.getLocation());
        r.translate(-5, -6);
        assertEquals(new Point(1, 2), r.getLocation());
    }

    public void testAddPoint1() {
        r.add(0, 0);
        assertEquals(new Rectangle(0, 0, 4, 6), r);
        r.add(1, 1);
        assertEquals(new Rectangle(0, 0, 4, 6), r);
        r.add(7, 8);
        assertEquals(new Rectangle(0, 0, 7, 8), r);
        r.add(5, 9);
        assertEquals(new Rectangle(0, 0, 7, 9), r);
        r.add(9, 6);
        assertEquals(new Rectangle(0, 0, 9, 9), r);
    }

    public void testAddPoint2() {
        r.add(new Point(0, 0));
        assertEquals(new Rectangle(0, 0, 4, 6), r);
        r.add(new Point(1, 1));
        assertEquals(new Rectangle(0, 0, 4, 6), r);
        r.add(new Point(7, 8));
        assertEquals(new Rectangle(0, 0, 7, 8), r);
        r.add(new Point(5, 9));
        assertEquals(new Rectangle(0, 0, 7, 9), r);
        r.add(new Point(9, 6));
        assertEquals(new Rectangle(0, 0, 9, 9), r);
    }

    public void testAddRect() {
        r.add(new Rectangle(1, 2, 3, 4)); // The same
        assertEquals(new Rectangle(1, 2, 3, 4), r);
        r.add(new Rectangle(2, 3, 2, 3)); // Inside
        assertEquals(new Rectangle(1, 2, 3, 4), r);
        r.add(new Rectangle(0, 0, 1, 2)); // Outside
        assertEquals(new Rectangle(0, 0, 4, 6), r);
        r.add(new Rectangle(2, 3, 3, 4)); // Intersect
        assertEquals(new Rectangle(0, 0, 5, 7), r);
        r.add(new Rectangle(-1, -1, 6, 8)); // Cover
        assertEquals(new Rectangle(-1, -1, 6, 8), r);
    }

    public void testContainsPoint1() {
        assertTrue(r.contains(2, 3));
        assertTrue(!r.contains(0, 0));
        assertTrue(!r.contains(0, 5));
        assertTrue(!r.contains(6, 5));
        assertTrue(!r.contains(6, 0));
    }

    public void testContainsPoint2() {
        assertTrue(r.contains(new Point(2, 3)));
        assertTrue(!r.contains(new Point(0, 0)));
        assertTrue(!r.contains(new Point(0, 5)));
        assertTrue(!r.contains(new Point(6, 5)));
        assertTrue(!r.contains(new Point(6, 0)));
    }

    @SuppressWarnings("deprecation")
    public void testInside() {
        assertTrue(r.inside(2, 3));
        assertTrue(!r.inside(0, 0));
        assertTrue(!r.inside(0, 7));
        assertTrue(!r.inside(5, 7));
        assertTrue(!r.inside(5, 0));
    }

    public void testContainsRect1() {
        assertTrue(!r.contains(0, 0, 0, 0)); // Empty
        assertTrue(r.contains(1, 2, 3, 4));  // The same
        assertTrue(r.contains(2, 3, 1, 2));  // Inside
        assertTrue(!r.contains(5, 7, 1, 2)); // Outside
        assertTrue(!r.contains(2, 3, 5, 6)); // Intersect
        assertTrue(!r.contains(0, 0, 5, 6)); // Cover
    }

    public void testContainsRect2() {
        assertTrue(!r.contains(new Rectangle(0, 0, 0, 0))); // Empty
        assertTrue(r.contains(new Rectangle(1, 2, 3, 4)));  // The same
        assertTrue(r.contains(new Rectangle(2, 3, 1, 2)));  // Inside
        assertTrue(!r.contains(new Rectangle(5, 7, 1, 2))); // Outside
        assertTrue(!r.contains(new Rectangle(2, 3, 5, 6))); // Intersect
        assertTrue(!r.contains(new Rectangle(0, 0, 5, 6))); // Cover
    }

    public void testIntersects() {
        assertTrue(!r.intersects(0, 0, 0, 0)); // Empty
        assertTrue(r.intersects(1, 2, 3, 4));  // The same
        assertTrue(r.intersects(2, 3, 1, 2));  // Inside
        assertTrue(!r.intersects(5, 7, 1, 2)); // Outside
        assertTrue(r.intersects(2, 3, 5, 6));  // Intersect
        assertTrue(r.intersects(0, 0, 5, 6));  // Cover
    }

    public void testIntersection() {
        assertEquals(new Rectangle(1, 2, -1, -2), r.intersection(new Rectangle()));           // Empty
        assertEquals(new Rectangle(1, 2, 3, 4),   r.intersection(new Rectangle(1, 2, 3, 4))); // The same
        assertEquals(new Rectangle(2, 3, 1, 2),   r.intersection(new Rectangle(2, 3, 1, 2))); // Inside
        assertEquals(new Rectangle(5, 7, -1, -1), r.intersection(new Rectangle(5, 7, 1, 2))); // Outside
        assertEquals(new Rectangle(2, 3, 2, 3),   r.intersection(new Rectangle(2, 3, 5, 6))); // Intersect
        assertEquals(new Rectangle(1, 2, 3, 4),   r.intersection(new Rectangle(0, 0, 5, 6))); // Cover
    }

    public void testCreateIntersection1() {
//      assertEquals(r.createIntersection(new Rectangle()), new Rectangle());                     // Empty
        assertEquals(new Rectangle(1, 2, 3, 4),   r.createIntersection(new Rectangle(1, 2, 3, 4))); // The same
        assertEquals(new Rectangle(2, 3, 1, 2),   r.createIntersection(new Rectangle(2, 3, 1, 2))); // Inside
        assertEquals(new Rectangle(5, 7, -1, -1), r.createIntersection(new Rectangle(5, 7, 1, 2))); // Outside
        assertEquals(new Rectangle(2, 3, 2, 3),   r.createIntersection(new Rectangle(2, 3, 5, 6))); // Intersect
        assertEquals(new Rectangle(1, 2, 3, 4),   r.createIntersection(new Rectangle(0, 0, 5, 6))); // Cover
    }

    public void testCreateIntersection2() {
//      assertEquals(r.createIntersection(new Rectangle2D.Double()), new Rectangle2D.Double());                     // Empty
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4),   r.createIntersection(new Rectangle2D.Double(1, 2, 3, 4))); // The same
        assertEquals(new Rectangle2D.Double(2, 3, 1, 2),   r.createIntersection(new Rectangle2D.Double(2, 3, 1, 2))); // Inside
        assertEquals(new Rectangle2D.Double(5, 7, -1, -1), r.createIntersection(new Rectangle2D.Double(5, 7, 1, 2)));           // Outside
        assertEquals(new Rectangle2D.Double(2, 3, 2, 3),   r.createIntersection(new Rectangle2D.Double(2, 3, 5, 6))); // Intersect
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4),   r.createIntersection(new Rectangle2D.Double(0, 0, 5, 6))); // Cover
    }

    public void testOutcode() {
        assertEquals(Rectangle2D.OUT_LEFT | Rectangle2D.OUT_TOP,     r.outcode(0, 0));
        assertEquals(Rectangle2D.OUT_TOP,                          r.outcode(2, 0));
        assertEquals(Rectangle2D.OUT_RIGHT | Rectangle2D.OUT_TOP,    r.outcode(5, 0));
        assertEquals(Rectangle2D.OUT_RIGHT,                        r.outcode(5, 3));
        assertEquals(Rectangle2D.OUT_RIGHT | Rectangle2D.OUT_BOTTOM, r.outcode(5, 7));
        assertEquals(Rectangle2D.OUT_BOTTOM,                       r.outcode(2, 7));
        assertEquals(Rectangle2D.OUT_LEFT | Rectangle2D.OUT_BOTTOM,  r.outcode(0, 7));
        assertEquals(Rectangle2D.OUT_LEFT,                         r.outcode(0, 3));
        assertEquals(0, r.outcode(2, 3));
    }

    public void testUnion() {
//      assertEquals(r.createUnion(new Rectangle()), new Rectangle(1, 2, 3, 4));           // Empty
        assertEquals(new Rectangle(1, 2, 3, 4), r.createUnion(new Rectangle(1, 2, 3, 4))); // The same
        assertEquals(new Rectangle(1, 2, 3, 4), r.createUnion(new Rectangle(2, 3, 1, 2))); // Inside
        assertEquals(new Rectangle(1, 2, 5, 7), r.createUnion(new Rectangle(5, 7, 1, 2))); // Outside
        assertEquals(new Rectangle(1, 2, 6, 7), r.createUnion(new Rectangle(2, 3, 5, 6))); // Intersect
        assertEquals(new Rectangle(0, 0, 5, 6), r.createUnion(new Rectangle(0, 0, 5, 6))); // Cover
    }

    public void testCreateUnion() {
//      assertEquals(r.createUnion(new Rectangle2D.Double()), new Rectangle2D.Double(1, 2, 3, 4));           // Empty
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4), r.createUnion(new Rectangle2D.Double(1, 2, 3, 4))); // The same
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4), r.createUnion(new Rectangle2D.Double(2, 3, 1, 2))); // Inside
        assertEquals(new Rectangle2D.Double(1, 2, 5, 7), r.createUnion(new Rectangle2D.Double(5, 7, 1, 2))); // Outside
        assertEquals(new Rectangle2D.Double(1, 2, 6, 7), r.createUnion(new Rectangle2D.Double(2, 3, 5, 6))); // Intersect
        assertEquals(new Rectangle2D.Double(0, 0, 5, 6), r.createUnion(new Rectangle2D.Double(0, 0, 5, 6))); // Cover
    }

    public void testSerializeRead1() {
        checkRead(new Rectangle());
    }

    public void testSerializeRead2() {
        checkRead(new Rectangle(1, 2));
    }

    public void testSerializeRead3() {
        checkRead(new Rectangle(1, 2, 3, 4));
    }

    public void testSerializeWrite1() {
        checkWrite(new Rectangle());
    }

    public void testSerializeWrite2() {
        checkWrite(new Rectangle(1, 2));
    }

    public void testSerializeWrite3() {
        checkWrite(new Rectangle(1, 2, 3, 4));
    }

    public void createSerializeTemplates() {
        saveSerialized(new Rectangle());
        saveSerialized(new Rectangle(1, 2));
        saveSerialized(new Rectangle(1, 2, 3, 4));
    }

    public static void main(String[] args) {
//        new RectangleTest("").createSerializeTemplates();
        junit.textui.TestRunner.run(RectangleTest.class);
    }

}


