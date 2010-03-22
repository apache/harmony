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

import java.awt.Point;
import java.awt.Rectangle;

public class Line2DTest extends PathIteratorTestCase {

    final static double DELTA = 0.001;

    double points[][] = new double[][] {
            // X, Y, CCW, seg distance^2, line distance^2
            // On the right
            { 0, 0,   1,  5,    0.5},
            { 1, 1,   1,  1,    0.5},
            { 2, 2,   1,  0.5,  0.5},
            { 3, 3,   1,  0.5,  0.5},
            { 4, 4,   1,  1,    0.5},
            { 5, 5,   1,  5,    0.5},
            // On the left
            {-1, 1,  -1,  5,    0.5},
            { 0, 2,  -1,  1,    0.5},
            { 1, 3,  -1,  0.5,  0.5},
            { 2, 4,  -1,  0.5,  0.5},
            { 3, 5,  -1,  1,    0.5},
            { 4, 6,  -1,  5,    0.5},
            // Online behind
            { 0, 1,  -1,  2,    0},
            {-1, 0,  -1,  8,    0},
            // Online forward
            { 4, 5,   1,  2,    0},
            { 5, 6,   1,  8,    0},
            // Inside
            { 1, 2,   0,  0,    0},
            { 2, 3,   0,  0,    0},
            { 3, 4,   0,  0,    0}
    };

    int[][] lines = new int[][] {
            // X1, Y1, X2, Y2, intersects
            // Parallel on the right
            { 0, 0, 3, 3,   0},
            { 1, 1, 2, 2,   0},
            { 3, 3, 5, 5,   0},
            // Parallel on the left
            {-2, 0, 0, 2,   0},
            { 0, 2, 2, 4,   0},
            { 3, 5, 5, 7,   0},
            // Ortogonal behind
            { 1, 0, 0, 1,   0},
            { 1, 0, -1, 2,  0},
            { 0, 1, -1, 2,  0},
            // Ortogonal forward
            { 3, 6, 5, 4,   0},
            { 3, 6, 4, 5,   0},
            { 4, 5, 5, 4,   0},
            // Ortogonal on the right
            { 2, 2, 3, 1,   0},
            { 3, 3, 4, 2,   0},
            // Ortogonal on the left
            { 1, 3, 0, 4,   0},
            { 2, 4, 1, 5,   0},
            // Doesn't intersect
            { 0, 0, 10, 3,  0},
            { 3, 3, 6, 4,   0},
            // Intersects
            { 2, 0, 2, 5,   1},
            { 3, 2, 0, 4,   1},
            // Online
            {-1, 0, 0, 1,   0},
            { 4, 5, 5, 6,   0},
            { 0, 1, 1, 2,   1}, // touch start
            { 0, 1, 2, 3,   1},
            { 0, 1, 3, 4,   1},
            { 0, 1, 5, 6,   1}, // cover
            { 1, 2, 3, 4,   1}, // the same
            { 3, 4, 4, 5,   1}, // touch end
            { 2, 3, 4, 5,   1},
            // Touch
            { 1, 0, 1, 2,   1},
            { 0, 2, 1, 2,   1},
            { 2, 0, 2, 3,   1},
            { 0, 3, 3, 3,   1},
            { 3, 0, 3, 4,   1},
            { 0, 4, 3, 4,   1},
            { 0, 0, 0, 1,   0},
            {-1, 1, 0, 1,   0},
            { 4, 0, 4, 5,   0},
            { 0, 5, 4, 5,   0}
    };

    public static int[][][] bounds = new int[][][] {
            {{1, 2, 3, 4}, {1, 2, 2, 2}},
            {{1, 2, 0, 0}, {0, 0, 1, 2}},
            {{1, 2, 0, 3}, {0, 2, 1, 1}},
            {{1, 2, 3, 0}, {1, 0, 2, 2}}
    };

    Line2D l;

    public Line2DTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        l = new Line2D.Double(1, 2, 3, 4);
    }

    @Override
    protected void tearDown() throws Exception {
        l = null;
        super.tearDown();
    }

    public void testSetLine1() {
        l.setLine(new Point(5, 6), new Point(7, 8));
        assertEquals(new Line2D.Double(5, 6, 7, 8), l);
    }

    public void testSetLine2() {
        l.setLine(new Line2D.Double(5, 6, 7, 8));
        assertEquals(new Line2D.Double(5, 6, 7, 8), l);
    }

    public void testGetBounds(){
        for (int[][] element : bounds) {
            assertEquals(
                    new Rectangle(
                            element[1][0],
                            element[1][1],
                            element[1][2],
                            element[1][3]),
                    new Line2D.Double(
                            element[0][0],
                            element[0][1],
                            element[0][2],
                            element[0][3]).getBounds());
        }
    }

    public void testClone() {
        assertEquals(l, (Line2D)l.clone());
    }

    public void testRelativeCCW1() {
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.relativeCCW(" + x + "," + y + ") failed",
                    (int)element[2],
                    Line2D.relativeCCW(l.getX1(), l.getY1(), l.getX2(), l.getY2(), x, y)
            );
        }
    }

    public void testRelativeCCW2() {
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.relativeCCW(" + x + "," + y + ") failed",
                    (int)element[2],
                    l.relativeCCW(x, y)
            );
        }
    }

    public void testRelativeCCW3() {
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.relativeCCW(" + x + "," + y + ") failed",
                    (int)element[2],
                    l.relativeCCW(new Point(x, y))
            );
        }
    }

    public void testLinesIntersect(){
        for (int[] element : lines) {
            int x1 = element[0];
            int y1 = element[1];
            int x2 = element[2];
            int y2 = element[3];
            assertEquals(
                    "Line2D.intersectsLine(" + x1 + "," + y1 + "," + x2 + "," + y2 + ") failed",
                    element[4] == 1,
                    Line2D.linesIntersect(l.getX1(), l.getY1(), l.getX2(), l.getY2(), x1, y1, x2, y2)
            );
        }
    }

    public void testIntersectsLine1(){
        for (int[] element : lines) {
            int x1 = element[0];
            int y1 = element[1];
            int x2 = element[2];
            int y2 = element[3];
            assertEquals(
                    "Line2D.intersectsLine(" + x1 + "," + y1 + "," + x2 + "," + y2 + ") failed",
                    element[4] == 1,
                    l.intersectsLine(x1, y1, x2, y2)
            );
        }
    }

    public void testIntersectsLine2(){
        for (int[] element : lines) {
            int x1 = element[0];
            int y1 = element[1];
            int x2 = element[2];
            int y2 = element[3];
            assertEquals(
                    "Line2D.intersectsLine(" + x1 + "," + y1 + "," + x2 + "," + y2 + ") failed",
                    element[4] == 1,
                    l.intersectsLine(new Line2D.Double(x1, y1, x2, y2))
            );
        }
    }

    public void testPtSegDistSq1(){
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.ptSegDistSq(" + x + "," + y + ") failed",
                    element[3],
                    Line2D.ptSegDistSq(l.getX1(), l.getY1(), l.getX2(), l.getY2(), x, y),
                    DELTA
            );
        }
    }

    public void testPtSegDistSq2(){
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.ptSegDistSq(" + x + "," + y + ") failed",
                    element[3],
                    l.ptSegDistSq(x, y),
                    DELTA
            );
        }
    }

    public void testPtSegDistSq3(){
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.ptSegDistSq(" + x + "," + y + ") failed",
                    element[3],
                    l.ptSegDistSq(new Point(x, y)),
                    DELTA
            );
        }
    }

    public void testPtSegDist1(){
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.ptSegDist(" + x + "," + y + ") failed",
                    Math.sqrt(element[3]),
                    Line2D.ptSegDist(l.getX1(), l.getY1(), l.getX2(), l.getY2(), x, y),
                    DELTA
            );
        }
    }

    public void testPtSegDist2(){
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.ptSegDist(" + x + "," + y + ") failed",
                    Math.sqrt(element[3]),
                    l.ptSegDist(x, y),
                    DELTA
            );
        }
    }

    public void testPtSegDist3(){
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.ptSegDist(" + x + "," + y + ") failed",
                    Math.sqrt(element[3]),
                    l.ptSegDist(new Point(x, y)),
                    DELTA
            );
        }
    }

    public void testPtLineDistSq1(){
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.ptLineDistSq(" + x + "," + y + ") failed",
                    element[4],
                    Line2D.ptLineDistSq(l.getX1(), l.getY1(), l.getX2(), l.getY2(), x, y),
                    DELTA
            );
        }
    }

    public void testPtLineDistSq2(){
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.ptLineDistSq(" + x + "," + y + ") failed",
                    element[4],
                    l.ptLineDistSq(x, y),
                    DELTA
            );
        }
    }

    public void testPtLineDistSq3(){
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.ptLineDistSq(" + x + "," + y + ") failed",
                    element[4],
                    l.ptLineDistSq(new Point(x, y)),
                    DELTA
            );
        }
    }

    public void testPtLineDist1(){
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.ptLineDist(" + x + "," + y + ") failed",
                    Math.sqrt(element[4]),
                    Line2D.ptLineDist(l.getX1(), l.getY1(), l.getX2(), l.getY2(), x, y),
                    DELTA
            );
        }
    }

    public void testPtLineDist2(){
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.ptLineDist(" + x + "," + y + ") failed",
                    Math.sqrt(element[4]),
                    l.ptLineDist(x, y),
                    DELTA
            );
        }
    }

    public void testPtLineDist3(){
        for (double[] element : points) {
            int x = (int)element[0];
            int y = (int)element[1];
            assertEquals(
                    "Line2D.ptLineDist(" + x + "," + y + ") failed",
                    Math.sqrt(element[4]),
                    l.ptLineDist(new Point(x, y)),
                    DELTA
            );
        }
    }

    public void testContainsPoint(){
        // Always returns false
        assertFalse(l.contains(0, 0));
        assertFalse(l.contains(1, 2));
        assertFalse(l.contains(3, 4));
    }

    public void testContainsPoint2(){
        // Always returns false
        assertFalse(l.contains(new Point(0, 0)));
        assertFalse(l.contains(new Point(1, 2)));
        assertFalse(l.contains(new Point(3, 4)));
    }

    public void testContainsRect(){
        // Always returns false
        assertFalse(l.contains(1, 2, 3, 4));
    }

    public void testContainsRect2(){
        // Always returns false
        assertFalse(l.contains(new Rectangle(1, 2, 3, 4)));
    }

    public void testIntersects1(){
        for(int i = 0; i < Rectangle2DTest.lines.length; i++) {
            int x1 = Rectangle2DTest.lines[i][0];
            int y1 = Rectangle2DTest.lines[i][1];
            int x2 = Rectangle2DTest.lines[i][2];
            int y2 = Rectangle2DTest.lines[i][3];
            assertEquals(
                    "Line(" + x1 + "," + y1 + "," + x2 + "," + y2 + ") intersects Rectangle(1,2,3,4) #" + i,
                    Rectangle2DTest.lines[i][4] == 1,
                    new Line2D.Double(x1, y1, x2, y2).intersects(1, 2, 3, 4));
        }
    }

    public void testIntersects2(){
        for(int i = 0; i < Rectangle2DTest.lines.length; i++) {
            int x1 = Rectangle2DTest.lines[i][0];
            int y1 = Rectangle2DTest.lines[i][1];
            int x2 = Rectangle2DTest.lines[i][2];
            int y2 = Rectangle2DTest.lines[i][3];
            assertEquals(
                    "Line(" + x1 + "," + y1 + "," + x2 + "," + y2 + ") intersects Rectangle(1,2,3,4) #" + i,
                    Rectangle2DTest.lines[i][4] == 1,
                    new Line2D.Double(x1, y1, x2, y2).intersects(new Rectangle(1, 2, 3, 4)));
        }
    }

    void checkPathIteratorDouble(PathIterator p, double[] values) {
        checkPathRule(p, PathIterator.WIND_NON_ZERO);
        checkPathMove(p, false, values[0], values[1], 0.0);
        checkPathLine(p, true, values[2], values[3], 0.0);
    }

    void checkPathIteratorFloat(PathIterator p, float[] values) {
        checkPathRule(p, PathIterator.WIND_NON_ZERO);
        checkPathMove(p, false, values[0], values[1], 0.0f);
        checkPathLine(p, true, values[2], values[3], 0.0f);
    }

    public void testGetPathIteratorDouble() {
        checkPathIteratorDouble(
                l.getPathIterator(null),
                new double[]{1, 2, 3, 4});
    }

    public void testGetPathIteratorFloat() {
        checkPathIteratorFloat(
                l.getPathIterator(null),
                new float[]{1, 2, 3, 4});
    }

    public void testGetPathIteratorDoubleFlat() {
        checkPathIteratorDouble(
                l.getPathIterator(null, 2),
                new double[]{1, 2, 3, 4});
    }

    public void testGetPathIteratorFloatFlat() {
        checkPathIteratorFloat(
                l.getPathIterator(null, 4),
                new float[]{1, 2, 3, 4});
    }

    public void testGetPathIteratorDoubleAffine() {
        checkPathIteratorDouble(
                l.getPathIterator(AffineTransform.getTranslateInstance(2, 1)),
                new double[]{3, 3, 5, 5});
    }

    public void testGetPathIteratorFloatAffine() {
        checkPathIteratorFloat(
                l.getPathIterator(AffineTransform.getTranslateInstance(2, 1)),
                new float[]{3, 3, 5, 5});
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Line2DTest.class);
    }

}
