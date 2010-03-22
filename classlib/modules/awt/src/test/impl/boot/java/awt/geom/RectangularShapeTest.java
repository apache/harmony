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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import junit.framework.TestCase;

// Don't test abstract methods getX(), getY(), getWidth(), getHeight(), isEmpty() and setFrame()

public class RectangularShapeTest extends TestCase {

    Rectangle r;

    public RectangularShapeTest(String name) {
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

    public void testGetMinX() {
        assertEquals(1.0, r.getMinX(), 0.0);
    }

    public void testGetMinY() {
        assertEquals(2.0, r.getMinY(), 0.0);
    }

    public void testGetMaxX() {
        assertEquals(4.0, r.getMaxX(), 0.0);
    }

    public void testGetMaxY() {
        assertEquals(6.0, r.getMaxY(), 0.0);
    }

    public void testGetCenterX() {
        assertEquals(2.5, r.getCenterX(), 0.0);
    }

    public void testGetCenterY() {
        assertEquals(4.0, r.getCenterY(), 0.0);
    }

    public void testGetFrame() {
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4), r.getFrame());
    }

    public void testGetBounds() {
        assertEquals(new Rectangle(1, 2, 3, 4), r.getBounds());
        assertEquals(
                new Rectangle(1, 2, 4, 6),
                new Rectangle2D.Double(1.3, 2.7, 3.2, 4.5).getBounds());
    }

    public void testClone() {
        assertEquals(r, r.clone());
    }

    public void testContainsPoint() {
        // Just check it works
        assertTrue(r.contains(new Point(2, 4)));
        assertTrue(!r.contains(new Point(0, 0)));
    }

    public void testContainsRect() {
        // Just check it works
        assertTrue(r.contains(new Rectangle(2, 3, 1, 2)));  // inside
        assertTrue(!r.contains(new Rectangle(0, 1, 2, 3))); // intersects
        assertTrue(!r.contains(new Rectangle(5, 6, 7, 8))); // outside
        assertTrue(!r.contains(new Rectangle(0, 0, 8, 9))); // cover
    }

    public void testIntersectsRect() {
        // Just check it works
        assertTrue(r.intersects(new Rectangle(2, 3, 1, 2)));  // inside
        assertTrue(r.intersects(new Rectangle(0, 1, 2, 3)));  // intersects
        assertTrue(!r.intersects(new Rectangle(5, 6, 7, 8))); // outside
        assertTrue(r.intersects(new Rectangle(0, 0, 8, 9)));  // cover
    }

    public void testGetPathIterator() {
        // Just check it works
        assertTrue(new Arc2D.Double(1, 2, 3, 4, 5, 6, 0).getPathIterator(null, 0) instanceof FlatteningPathIterator);
    }

    public void testSetFrame1() {
        r.setFrame(5.0, 6.0, 7.0, 8.0);
        assertEquals(new Rectangle(5, 6, 7, 8), r);
    }

    public void testSetFrame2() {
        r.setFrame(new Point(5, 6), new Dimension(7, 8));
        assertEquals(new Rectangle(5, 6, 7, 8), r);
    }

    public void testSetFrame3() {
        r.setFrame(new Rectangle(5, 6, 7, 8));
        assertEquals(new Rectangle(5, 6, 7, 8), r);
    }

    public void testSetFrameFromCenterXY1() {
        r.setFrameFromCenter(5.0, 6.0, 7.0, 9.0);
        assertEquals(new Rectangle(3, 3, 4, 6), r);
    }

    public void testSetFrameFromCenterXY2() {
        r.setFrameFromCenter(5.0, 6.0, 7.0, 3.0);
        assertEquals(new Rectangle(3, 3, 4, 6), r);
    }

    public void testSetFrameFromCenterXY3() {
        r.setFrameFromCenter(5.0, 6.0, 3.0, 3.0);
        assertEquals(new Rectangle(3, 3, 4, 6), r);
    }

    public void testSetFrameFromCenterXY4() {
        r.setFrameFromCenter(5.0, 6.0, 3.0, 9.0);
        assertEquals(new Rectangle(3, 3, 4, 6), r);
    }

    public void testSetFrameFromCenterPP1() {
        r.setFrameFromCenter(new Point(5, 6), new Point(7, 9));
        assertEquals(new Rectangle(3, 3, 4, 6), r);
    }

    public void testSetFrameFromCenterPP2() {
        r.setFrameFromCenter(new Point(5, 6), new Point(7, 3));
        assertEquals(new Rectangle(3, 3, 4, 6), r);
    }

    public void testSetFrameFromCenterPP3() {
        r.setFrameFromCenter(new Point(5, 6), new Point(3, 3));
        assertEquals(new Rectangle(3, 3, 4, 6), r);
    }

    public void testSetFrameFromCenterPP4() {
        r.setFrameFromCenter(new Point(5, 6), new Point(3, 9));
        assertEquals(new Rectangle(3, 3, 4, 6), r);
    }

    public void testSetFrameFromDiagonalXY1() {
        r.setFrameFromDiagonal(5.0, 6.0, 7.0, 9.0);
        assertEquals(new Rectangle(5, 6, 2, 3), r);
    }

    public void testSetFrameFromDiagonalXY2() {
        r.setFrameFromDiagonal(5.0, 6.0, 7.0, 3.0);
        assertEquals(new Rectangle(5, 3, 2, 3), r);
    }

    public void testSetFrameFromDiagonalXY3() {
        r.setFrameFromDiagonal(5.0, 6.0, 3.0, 3.0);
        assertEquals(new Rectangle(3, 3, 2, 3), r);
    }

    public void testSetFrameFromDiagonalXY4() {
        r.setFrameFromDiagonal(5.0, 6.0, 3.0, 9.0);
        assertEquals(new Rectangle(3, 6, 2, 3), r);
    }

    public void testSetFrameFromDiagonalPP1() {
        r.setFrameFromDiagonal(new Point(5, 6), new Point(7, 9));
        assertEquals(new Rectangle(5, 6, 2, 3), r);
    }

    public void testSetFrameFromDiagonalPP2() {
        r.setFrameFromDiagonal(new Point(5, 6), new Point(7, 3));
        assertEquals(new Rectangle(5, 3, 2, 3), r);
    }

    public void testSetFrameFromDiagonalPP3() {
        r.setFrameFromDiagonal(new Point(5, 6), new Point(3, 3));
        assertEquals(new Rectangle(3, 3, 2, 3), r);
    }

    public void testSetFrameFromDiagonalPP4() {
        r.setFrameFromDiagonal(new Point(5, 6), new Point(3, 9));
        assertEquals(new Rectangle(3, 6, 2, 3), r);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(RectangularShapeTest.class);
    }

}
