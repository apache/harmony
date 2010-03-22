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
package java.awt.geom;

import java.awt.Point;
import java.awt.Rectangle;

public class Rectangle2DTest extends ShapeTestCase {

    // Test intersectLine
    public static int lines[][] = new int[][] {
            // Parallel left
            { 0,  0,  0,  2,    0},
            {-1,  4, -1,  6,    0},
            { 0,  5,  0,  8,    0},
            { 0,  7,  0, 10,    0},
            // Parallel right
            { 5,  0,  5,  2,    0},
            { 6,  4,  6,  6,    0},
            { 5,  5,  5,  8,    0},
            { 5,  7,  5, 10,    0},
            // Parallel top
            { 0,  0,  2,  0,    0},
            { 0,  0,  1,  0,    0},
            { 2,  0,  5,  0,    0},
            { 4,  0,  6,  0,    0},
            // Parallel bottom
            { 0,  7,  2,  7,    0},
            { 0,  7,  1,  7,    0},
            { 2,  7,  5,  7,    0},
            { 4,  7,  6,  7,    0},
            // Corner TL
            { 0,  2, -1,  3,    0},
            { 0,  2,  1,  1,    0},
            { 1,  1,  2,  1,    0},
            {-9,  9,  9, -9,    0},
            // Corner TR
            { 3,  0,  4,  1,    0},
            { 3,  0,  5,  2,    0},
            { 5,  2,  6,  4,    0},
            {-9, -9, 19,  9,    0},
            // Corner BR
            { 4,  7,  6, 10,    0},
            { 5,  8,  6,  6,    0},
            { 6,  7,  8, 10,    0},
            { 0, 11, 11,  0,    0},
            // Corner BL
            { 0,  6, -2,  1,    0},
            { 0,  6,  1,  8,    0},
            {-1,  8,  3, 10,    0},
            {-9,  0,  9, 19,    0},
            // Touch left
            {-1,  1,  1,  2,    1},
            { 0,  3,  1,  4,    1},
            // Touch bottom
            { 0,  5,  1,  6,    1},
            { 1,  8,  3,  6,    1},
            // Touch right
            { 7,  6,  4,  6,    1},
            { 6,  4,  4,  3,    1},
            // Touch top
            { 6,  0,  4,  2,    1},
            { 4,  0,  2,  2,    1},
            // On the left side
            { 1,  0,  1,  1,    0}, // up
            { 1,  0,  1,  3,    1},
            { 1,  3,  1,  5,    1}, // inside
            { 1,  4,  1,  7,    1},
            { 1,  7,  1,  9,    0}, // down
            { 1,  0,  1,  9,    1}, // cover
            // On the right side
            { 4,  0,  4,  1,    0}, // up
            { 4,  0,  4,  3,    1},
            { 4,  3,  4,  5,    1}, // inside
            { 4,  4,  4,  7,    1},
            { 4,  7,  4,  9,    0}, // down
            { 4,  0,  4,  9,    1}, // cover
            // On the top
            {-1,  2,  0,  2,    0},
            { 0,  2,  2,  2,    1},
            { 2,  2,  3,  2,    1},
            { 3,  2,  6,  2,    1},
            { 5,  2,  8,  2,    0},
            { 0,  2,  7,  2,    1},
            // On the bottom
            {-1,  6,  0,  6,    0},
            { 0,  6,  2,  6,    1},
            { 2,  6,  3,  6,    1},
            { 3,  6,  6,  6,    1},
            { 5,  6,  8,  6,    0},
            { 0,  6,  7,  6,    1}
            // Intersect vertical
    };

    Rectangle2D.Double r;

    public Rectangle2DTest(String name) {
        super(name);
//        filterImage = createFilter("^(rect).*([.]ico)$", "(.*)((affine)|(flat)|(bounds))(.*)");
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

    public void testSetRect() {
        r.setRect(new Rectangle2D.Double(5, 6, 7, 8));
        assertEquals(new Rectangle2D.Double(5, 6, 7, 8), r);
    }

    public void testSetFrame() {
        r.setFrame(5.0, 6.0, 7.0, 8.0);
        assertEquals(new Rectangle2D.Double(5, 6, 7, 8), r);
    }

    public void testGetBounds2D() {
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4), r.getBounds2D());
    }

    public void testOutcode() {
        // Just check it works
        assertEquals(Rectangle2D.OUT_LEFT | Rectangle2D.OUT_TOP,     r.outcode(0, 0));
        assertEquals(0, r.outcode(new Point2D.Double(2, 3)));
    }

    public void testEquals() {
        assertTrue(r.equals(new Rectangle(1, 2, 3, 4)));
        assertTrue(!r.equals(new Rectangle(0, 2, 3, 4)));
        assertTrue(!r.equals(new Rectangle(1, 0, 3, 4)));
        assertTrue(!r.equals(new Rectangle(1, 2, 0, 4)));
        assertTrue(!r.equals(new Rectangle(1, 2, 3, 0)));
    }

    public void testHashCode() {
        assertTrue(r.hashCode() == new Rectangle(1, 2, 3, 4).hashCode());
        assertTrue(r.hashCode() != new Rectangle(0, 2, 3, 4).hashCode());
        assertTrue(r.hashCode() != new Rectangle(1, 0, 3, 4).hashCode());
        assertTrue(r.hashCode() != new Rectangle(1, 2, 0, 4).hashCode());
        assertTrue(r.hashCode() != new Rectangle(1, 2, 3, 0).hashCode());
    }

    public void testIntersectsLine1() {
        Rectangle2D rr = new Rectangle2D.Double(1, 2, 3, 4);
        for (int[] element : lines) {
            int x1 = element[0];
            int y1 = element[1];
            int x2 = element[2];
            int y2 = element[3];
            assertEquals(
                    "Rectangle2D.intersectsLine(" + x1 + "," + y1 + "," + x2 + "," + y2 + ") failed",
                    element[4] == 1,
                    rr.intersectsLine(x1, y1, x2, y2));
        }
    }

    public void testIntersectsLine2() {
        Rectangle2D rr = new Rectangle2D.Double(1, 2, 3, 4);
        for (int[] element : lines) {
            int x1 = element[0];
            int y1 = element[1];
            int x2 = element[2];
            int y2 = element[3];
            assertEquals(
                    "Rectangle2D.intersectsLine(" + x1 + "," + y1 + "," + x2 + "," + y2 + ") failed",
                    element[4] == 1,
                    rr.intersectsLine(new Line2D.Double(x1, y1, x2, y2)));
        }
    }

    public void testIntersect() {
        Rectangle dst = new Rectangle();
        Rectangle2D.intersect(r, new Rectangle(1, 2, 3, 4), dst);
        assertEquals(new Rectangle(1, 2, 3, 4), dst); // The same
        Rectangle2D.intersect(r, new Rectangle(2, 3, 1, 2), dst);
        assertEquals(new Rectangle(2, 3, 1, 2), dst); // Inside
        Rectangle2D.intersect(r, new Rectangle(5, 7, 1, 2), dst);
        assertEquals(new Rectangle(5, 7, -1, -1), dst); // Outside
        Rectangle2D.intersect(r, new Rectangle(2, 3, 5, 6), dst);
        assertEquals(new Rectangle(2, 3, 2, 3), dst); // Intersect
        Rectangle2D.intersect(r, new Rectangle(0, 0, 5, 6), dst);
        assertEquals(new Rectangle(1, 2, 3, 4), dst); // Cover
    }

    public void testUnion() {
        Rectangle dst = new Rectangle();
        Rectangle2D.union(r, new Rectangle(1, 2, 3, 4), dst);
        assertEquals(new Rectangle(1, 2, 3, 4), dst); // The same
        Rectangle2D.union(r, new Rectangle(2, 3, 1, 2), dst);
        assertEquals(new Rectangle(1, 2, 3, 4), dst); // Inside
        Rectangle2D.union(r, new Rectangle(5, 7, 1, 2), dst);
        assertEquals(new Rectangle(1, 2, 5, 7), dst); // Outside
        Rectangle2D.union(r, new Rectangle(2, 3, 5, 6), dst);
        assertEquals(new Rectangle(1, 2, 6, 7), dst); // Intersect
        Rectangle2D.union(r, new Rectangle(0, 0, 5, 6), dst);
        assertEquals(new Rectangle(0, 0, 5, 6), dst); // Cover
    }

    public void testAdd1() {
        r.add(1.0, 1.0);
        assertEquals(new Rectangle2D.Double(1, 1, 3, 5), r);
        r.add(7.0, 8.0);
        assertEquals(new Rectangle2D.Double(1, 1, 6, 7), r);
        r.add(5.0, 9.0);
        assertEquals(new Rectangle2D.Double(1, 1, 6, 8), r);
        r.add(9.0, 6.0);
        assertEquals(new Rectangle2D.Double(1, 1, 8, 8), r);
        r.add(0.0, 0.0);
        assertEquals(new Rectangle2D.Double(0, 0, 9, 9), r);
    }

    public void testAdd2() {
        r.add(new Point(0, 0));
        assertEquals(new Rectangle2D.Double(0, 0, 4, 6), r);
        r.add(new Point(1, 1));
        assertEquals(new Rectangle2D.Double(0, 0, 4, 6), r);
        r.add(new Point(7, 8));
        assertEquals(new Rectangle2D.Double(0, 0, 7, 8), r);
        r.add(new Point(5, 9));
        assertEquals(new Rectangle2D.Double(0, 0, 7, 9), r);
        r.add(new Point(9, 6));
        assertEquals(new Rectangle2D.Double(0, 0, 9, 9), r);
    }

    public void testAdd3() {
        r.add(new Rectangle(1, 2, 3, 4)); // The same
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4), r);
        r.add(new Rectangle(2, 3, 2, 3)); // Inside
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4), r);
        r.add(new Rectangle2D.Double(0, 0, 1, 2)); // Outside
        assertEquals(new Rectangle2D.Double(0, 0, 4, 6), r);
        r.add(new Rectangle(2, 3, 3, 4)); // Intersect
        assertEquals(new Rectangle2D.Double(0, 0, 5, 7), r);
        r.add(new Rectangle(-1, -1, 6, 8)); // Cover
        assertEquals(new Rectangle2D.Double(-1, -1, 6, 8), r);
    }

    void checkPathIteratorDouble(PathIterator p, double[] values) {
        checkPathRule(p, PathIterator.WIND_NON_ZERO);
        checkPathMove(p, false, values[0], values[1], 0.0);
        checkPathLine(p, false, values[2], values[3], 0.0);
        checkPathLine(p, false, values[4], values[5], 0.0);
        checkPathLine(p, false, values[6], values[7], 0.0);
        checkPathLine(p, false, values[0], values[1], 0.0);
        checkPathClose(p, true);
    }

    void checkPathIteratorFloat(PathIterator p, float[] values) {
        checkPathRule(p, PathIterator.WIND_NON_ZERO);
        checkPathMove(p, false, values[0], values[1], 0.0f);
        checkPathLine(p, false, values[2], values[3], 0.0f);
        checkPathLine(p, false, values[4], values[5], 0.0f);
        checkPathLine(p, false, values[6], values[7], 0.0f);
        checkPathLine(p, false, values[0], values[1], 0.0f);
        checkPathClose(p, true);
    }

    public void testGetPathIteratorDouble() {
        checkPathIteratorDouble(
                new Rectangle(1, 2, 0, 0).getPathIterator(null),
                new double[]{1, 2, 1, 2, 1, 2, 1, 2});
        checkPathIteratorDouble(
                r.getPathIterator(null),
                new double[]{1, 2, 4, 2, 4, 6, 1, 6});
    }

    public void testGetPathIteratorFloat() {
        checkPathIteratorFloat(
                r.getPathIterator(null),
                new float[]{1, 2, 4, 2, 4, 6, 1, 6});
    }

    public void testGetPathIteratorDoubleFlat() {
        checkPathIteratorDouble(
                r.getPathIterator(null, 2),
                new double[]{1, 2, 4, 2, 4, 6, 1, 6});
    }

    public void testGetPathIteratorFloatFlat() {
        checkPathIteratorFloat(
                r.getPathIterator(null, 5),
                new float[]{1, 2, 4, 2, 4, 6, 1, 6});
    }

    public void testGetPathIteratorDoubleAffine() {
        checkPathIteratorDouble(
                r.getPathIterator(AffineTransform.getTranslateInstance(3, 1)),
                new double[]{4, 3, 7, 3, 7, 7, 4, 7});
    }

    public void testGetPathIteratorFloatAffine() {
        checkPathIteratorFloat(
                r.getPathIterator(AffineTransform.getTranslateInstance(3, 1)),
                new float[]{4, 3, 7, 3, 7, 7, 4, 7});
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Rectangle2DTest.class);
    }

}
