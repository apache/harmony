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
import java.awt.geom.Arc2D;
import java.awt.geom.ShapeTestCase;

public class Arc2DTest extends ShapeTestCase {

    final double DELTA = 0.001;
    final double s2 = 2.0 / Math.sqrt(2.0);
    final double s3 = 3.0 / Math.sqrt(2.0);

    // Test getStartPoint()/getEndPoint()
    double angles[][] = new double[][] {
            // Angle, point
            // 90s angels
            {   0,   5, 5},
            {  90,   3, 2},
            { 180,   1, 5},
            { 270,   3, 8},
            { 360,   5, 5},
            { 450,   3, 2},
            { -90,   3, 8},
            {-180,   1, 5},
            {-270,   3, 2},
            {-360,   5, 5},
            {-450,   3, 8},
            // 45s angle
            {  45,   3 + s2, 5 - s3},
            { 135,   3 - s2, 5 - s3},
            { 225,   3 - s2, 5 + s3},
            { 315,   3 + s2, 5 + s3}
    };

    // Test containsAngle(double)
    double[][] arcs = new double[][] {
            // Start, extent, angle, contains

            {   0,  10,    5,  1},
            {   0,  10,   15,  0},
            {   0,  10,  115,  0},
            {   0,  10,  365,  1},
            {   0,  10,  725,  1},
            {   0,  10, -355,  1},
            {   0,  10, -715,  1},

            {  10, 370,    0,  1},
            {  10, 370,   90,  1},
            {  10, 370,  999,  1},
            {  10, 370, -999,  1},

            {  135, -50,  84,  0},
            {  135, -50,  85,  1},
            {  135, -50, 135,  1},
            {  135, -50, 136,  0},

            { -135, -50, -186,  0},
            { -135, -50, -185,  1},
            { -135, -50, -135,  1},
            { -135, -50, -134,  0},

            {  435, 30,  74,  0},
            {  435, 30,  75,  1},
            {  435, 30, 105,  1},
            {  435, 30, 106,  0},
            {  435, 30, 800,  1},

            { -435, 30, -76,  0},
            { -435, 30, -75,  1},
            { -435, 30, -45,  1},
            { -435, 30, -44,  0},
            { -435, 30, -770, 1},

            {200, 350, 20, 1}


    };

    // Test getBounds()
    double bounds[][] = new double[][] {
            // Type, start, extent, bounds
            {Arc2D.PIE,   0,  90,   3, 2, 5, 5},
            {Arc2D.CHORD, 0,  90,   3, 2, 5, 5},
            {Arc2D.OPEN,  0,  90,   3, 2, 5, 5},

            {Arc2D.PIE,   0,  45,   3, 5 - s3, 5, 5},
            {Arc2D.CHORD, 0,  45,   3 + s2, 5 - s3, 5, 5},
            {Arc2D.OPEN,  0,  45,   3 + s2, 5 - s3, 5, 5},

            {Arc2D.PIE,   45, 90,   3 - s2, 2, 3 + s2, 5},
            {Arc2D.CHORD, 45, 90,   3 - s2, 2, 3 + s2, 5 - s3},
            {Arc2D.OPEN,  45, 90,   3 - s2, 2, 3 + s2, 5 - s3},

            {Arc2D.PIE,   135, 180, 1, 5 - s3, 3 + s2, 8},
            {Arc2D.CHORD, 135, 180, 1, 5 - s3, 3 + s2, 8},
            {Arc2D.OPEN,  135, 180, 1, 5 - s3, 3 + s2, 8},

            {Arc2D.PIE,   225, 90,  3 - s2, 5, 3 + s2, 8},
            {Arc2D.CHORD, 225, 90,  3 - s2, 5 + s3, 3 + s2, 8},
            {Arc2D.OPEN,  225, 90,  3 - s2, 5 + s3, 3 + s2, 8}
    };

    // Test SetAngleStart(Point2D)
    double points1[][] = new double[][] {
            // X, Y, angle
            { 5, 5,   0},
            { 5, 3,  45},
            { 3, 3,  90},
            { 1, 3, 135},
            { 1, 5, 180},
            { 1, 7, 225},
            { 3, 7, 270},
            { 5, 7, 315}
    };

    // Test setAngles(double, double, double, double)
    double points2[][] = new double[][] {
            // X1, Y1, X2, Y2, start, extent
            // Full arc
            { 5, 5, 5, 5,    0, 360},
            { 0, 2, 0, 2,  135, 360},
            { 1, 7, 1, 7,  225, 360},
            { 5, 7, 5, 7,  315, 360},

            { 5, 3, 1, 3,   45,  90},
            { 5, 3, 5, 7,   45, 270},

            { 1, 3, 5, 7,  135, 180}
    };

    // Test setArcByTangent(double, double, double, double)
    double tangent[][] = new double[][] {
            // X1, Y1, X2, Y2, X3, Y3, radius, centerX, centerY, start, extent
            // Quarter sectors
            { 0, 5,   0, 0,   5, 0,  3,        3, 3, 180, 270},
            { 5, 0,   0, 0,   0, 5,  3,        3, 3,  90,  90},

            {-5, 0,   0, 0,   0, 5,  3,       -3, 3,  90, 270},
            { 0, 5,   0, 0,  -5, 0,  3,       -3, 3,   0,  90},

            { 6, 7,   1, 2,  -4, 7,  2 * s2,   1, 6,  45,  90},
            {-4, 7,   1, 2,   6, 7,  2 * s2,   1, 6, 135, 270}
    };

    Arc2D.Double a;

    public Arc2DTest(String name) {
        super(name);
//        filterImage = createFilter("^(arc).*([.]ico)$", "(.*)((affine)|(flat)|(bounds))(.*)");
        filterShape = createFilter("^(arc).*([.]shape)$", null);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        a = new Arc2D.Double(1, 2, 4, 6, 45, 240 - 45, Arc2D.PIE);
    }

    @Override
    protected void tearDown() throws Exception {
        a = null;
        super.tearDown();
    }

    public void testGetArcType() {
        assertEquals("Arc type", Arc2D.PIE, a.getArcType());
    }

    public void testSetArcType() {
        a.setArcType(Arc2D.CHORD);
        assertEquals("Arc type", Arc2D.CHORD, a.getArcType());
        
        // Regression for HARMONY-1403
        try {
            // Invalid type
            a.setArcType(17);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testGetStartPoint() {
        for (double[] element : angles) {
            double angle = element[0];
            assertEquals(
                    "[angle=" + doubleToStr(angle) + "] Start point",
                    new Point2D.Double(element[1], element[2]),
                    new Arc2D.Double(1, 2, 4, 6, angle, 10, Arc2D.PIE).getStartPoint(),
                    DELTA);
        }
    }

    public void testGetEndPoint() {
        for (double[] element : angles) {
            double angle = element[0];
            assertEquals(
                    "[angle=" + doubleToStr(angle) + "] End point",
                    new Point2D.Double(element[1], element[2]),
                    new Arc2D.Double(1, 2, 4, 6, 10, angle - 10, Arc2D.PIE).getEndPoint(),
                    DELTA);
        }
    }

    public void testGetBounds2D() {
        for (double[] element : bounds) {
            int type = (int)element[0];
            double start = element[1];
            double extent = element[2];
            assertEquals(
                    "[start=" + doubleToStr(start) +
                    ",extent=" + doubleToStr(extent) +
                    ",type=" + type + "] Bounds",
                    new Rectangle2D.Double(
                            element[3],
                            element[4],
                            element[5] - element[3],
                            element[6] - element[4]),
                    new Arc2D.Double(1, 2, 4, 6, start, extent, type).getBounds2D(),
                    DELTA);
        }
    }

    public void testContainsAngle() {
        for (double[] element : arcs) {
            double start = element[0];
            double extent = element[1];
            double angle = element[2];
            assertEquals(
                    "[start=" + doubleToStr(start) + ",extent=" + extent + "] containsAngle(" + angle + ")",
                    element[3] == 1,
                    new Arc2D.Double(1, 2, 4, 6, start, extent, Arc2D.OPEN).containsAngle(angle));
        }
    }

    public void testSetFrame() {
        int type = a.getArcType();
        double start = a.getAngleStart();
        double extent = a.getAngleExtent();
        a.setFrame(7, 8, 9, 10);
        assertEquals(new Arc2D.Double(7, 8, 9, 10, start, extent, type), a);
    }

    public void testSetArc1() {
        a.setArc(new Point(7, 8), new Dimension(9, 10), 11, 12, Arc2D.CHORD);
        assertEquals(new Arc2D.Double(7, 8, 9, 10, 11, 12, Arc2D.CHORD), a);
    }

    public void testSetArc2() {
        a.setArc(new Rectangle(7, 8, 9, 10), 11, 12, Arc2D.CHORD);
        assertEquals(new Arc2D.Double(7, 8, 9, 10, 11, 12, Arc2D.CHORD), a);
    }

    public void testSetArc3() {
        a.setArc(new Arc2D.Double(7, 8, 9, 10, 11, 12, Arc2D.CHORD));
        assertEquals(new Arc2D.Double(7, 8, 9, 10, 11, 12, Arc2D.CHORD), a);
    }

    public void testSetArcByCenter() {
        a.setArcByCenter(7, 8, 3, 11, 12, Arc2D.CHORD);
        assertEquals(new Arc2D.Double(4, 5, 6, 6, 11, 12, Arc2D.CHORD), a);
    }

    public void testSetArcByTangent() {
        for (double[] element : tangent) {
            double x1 = element[0];
            double y1 = element[1];
            double x2 = element[2];
            double y2 = element[3];
            double x3 = element[4];
            double y3 = element[5];
            double radius = element[6];
            a.setArcByTangent(
                    new Point2D.Double(x1, y1),
                    new Point2D.Double(x2, y2),
                    new Point2D.Double(x3, y3),
                    radius);
            Arc2D a2 = new Arc2D.Double();
            a2.setArcByCenter(element[7], element[8], radius, element[9], element[10], a.getArcType());
            assertEquals(
                    "Arc2d.setArcByTangent((" +
                    doubleToStr(x1) + "," +
                    doubleToStr(y1) + "),(" +
                    doubleToStr(x2) +"," +
                    doubleToStr(y2) + "),(" +
                    doubleToStr(x3) + "," +
                    doubleToStr(y3) +")," +
                    doubleToStr(radius) + ") failed",
                    a2,
                    a,
                    DELTA);
        }
    }

    public void testSetAngleStart() {
        for (double[] element : points1) {
            double x = element[0];
            double y = element[1];
            double angle = element[2];
            a.setAngleStart(new Point2D.Double(x, y));
            assertEquals(
                    "Arc2d.setAngleStart(" + x + "," + y + ") failed",
                    angle,
                    a.getAngleStart(),
                    DELTA);
        }
    }

    public void testSetAngels1() {
        for (double[] element : points2) {
            double x1 = element[0];
            double y1 = element[1];
            double x2 = element[2];
            double y2 = element[3];
            double start = element[4];
            double extent = element[5];
            a.setAngles(x1, y1, x2, y2);
            assertEquals(
                    "Arc2d.setAngles(" + x1 + "," + y1 + "," + x2 + "," + y2 + ") failed. Start angle",
                    start,
                    a.getAngleStart(),
                    DELTA);
            assertEquals(
                    "Arc2d.setAngles(" + x1 + "," + y1 + "," + x2 + "," + y2 + ") failed. Extent angle",
                    extent,
                    a.getAngleExtent(),
                    DELTA);
        }
    }

    public void testSetAngels2() {
        for (double[] element : points2) {
            double x1 = element[0];
            double y1 = element[1];
            double x2 = element[2];
            double y2 = element[3];
            double start = element[4];
            double extent = element[5];
            a.setAngles(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
            assertEquals(
                    "Arc2d.setAngles(" + x1 + "," + y1 + "," + x2 + "," + y2 + ") failed. Start angle",
                    start,
                    a.getAngleStart(),
                    DELTA);
            assertEquals(
                    "Arc2d.setAngles(" + x1 + "," + y1 + "," + x2 + "," + y2 + ") failed. Extent angle",
                    extent,
                    a.getAngleExtent(),
                    DELTA);
        }
    }

    public void testGetPathIteratorEmpty() {
        // Regression test HARMONY-1585
        Arc2D a = new Arc2D.Double();
        PathIterator p = a.getPathIterator(null);
        checkPathMove(p, true, 0, 0, 0.0);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(Arc2DTest.class);
    }

}
