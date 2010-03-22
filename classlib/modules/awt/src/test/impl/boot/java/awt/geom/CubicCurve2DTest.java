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

public class CubicCurve2DTest extends ShapeTestCase {

    static final double ROOT_DELTA = 0.0001;

    // Test subdivide()
    double[][][] curves1 = new double[][][] {
            // Curve, left subdivision, right subdivision
            {{1, 2, 3, 4, 5, 6, 7, 8}, {1, 2, 2, 3, 3, 4, 4, 5}, {4, 5, 5, 6, 6, 7, 7, 8}},
            {{1, 1, 3, 7, 9, 5, 7, 3}, {1, 1, 2, 4, 4, 5, 5.5, 5}, {5.5, 5, 7, 5, 8, 4, 7, 3}}
    };

    double[][][] curves2 = new double[][][] {
            // Curve, left subdivision, right subdivision
            // First number in array means offset
            {{-3, 8, 7, 1, 2, 3, 4, 5, 6, 7, 8}, {-1, 1, 2, 2, 3, 3, 4, 4, 5}, {4, 5, 5, 6, 6, 7, 7, 8}},
            {{1, 1, 3, 7, 9, 5, 7, 3}, {-2, 9, 1, 1, 2, 4, 4, 5, 5.5, 5}, {-3, 8, 8, 5.5, 5, 7, 5, 8, 4, 7, 3}}
    };

    // Test solveQuaratic
    double[][][] equations = new double[][][] {
            // Equation (c, b, a), number of roots, roots
            {{-8, 12, -6, 1},  {1, 2}},
            {{-12, 16, -7, 1},  {2, 3, 2}},
            {{-24, 26, -9, 1}, {3, 2, 4, 3}}
    };

    // Test getFlatness
    double[][] curves3 = new double[][] {
            // Curve, flatnnes^2
            {1, 2, 3, 4, 5, 6, 7, 8,   0},
            {1, 2, 2, 4, 5, 1, 6, 2,   4},
            {1, 2, 2, 4, 5, -1, 6, 2,  9}
    };

    // Test getBounds
    public static double[][][] bounds = new double[][][] {
            {{1, 2, 3, 4, 5, 6, 7, 8}, {1, 2, 6, 6}},
            {{1, 1, 3, 7, 9, 5, 7, 3}, {1, 1, 8, 6}},
            {{3, 1, 1, 2, 5, 4, 4, 5}, {1, 1, 4, 4}}
    };

    CubicCurve2D c;

    public CubicCurve2DTest(String name) {
        super(name);
//        filterImage = createFilter("^(cubic).*([.]ico)$", "(.*)((affine)|(flat)|(bounds))(.*)");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        c = new CubicCurve2D.Double(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Override
    protected void tearDown() throws Exception {
        c = null;
        super.tearDown();
    }

    public void testSetCurve1() {
        double coords[] = new double[] {7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17};
        c.setCurve(coords, 0);
        assertEquals(new CubicCurve2D.Double(7, 8, 9, 10, 11, 12, 13, 14), c, 0.0);
        c.setCurve(coords, 3);
        assertEquals(new CubicCurve2D.Double(10, 11, 12, 13, 14, 15, 16, 17), c, 0.0);
    }

    public void testSetCurve2() {
        Point2D[] points = new Point[] {
                new Point(7, 8),
                new Point(9, 10),
                new Point(11, 12),
                new Point(13, 14),
                new Point(15, 16),
                new Point(17, 18)};
        c.setCurve(points, 0);
        assertEquals(new CubicCurve2D.Double(7, 8, 9, 10, 11, 12, 13, 14), c, 0.0);
        c.setCurve(points, 2);
        assertEquals(new CubicCurve2D.Double(11, 12, 13, 14, 15, 16, 17, 18), c, 0.0);
    }

    public void testSetCurve3() {
        c.setCurve(new Point(7, 8), new Point(9, 10), new Point(11, 12), new Point(13, 14));
        assertEquals(new CubicCurve2D.Double(7, 8, 9, 10, 11, 12, 13, 14), c, 0.0);
    }

    public void testSetCurve4() {
        c.setCurve(new CubicCurve2D.Double(7, 8, 9, 10, 11, 12, 13, 14));
        assertEquals(new CubicCurve2D.Double(7, 8, 9, 10, 11, 12, 13, 14), c, 0.0);
    }

    public void testGetFlatnessSq1() {
        for (double[] element : curves3) {
            CubicCurve2D curve = new CubicCurve2D.Double();
            curve.setCurve(element, 0);
            assertEquals(cubicToStr(curve), element[8], curve.getFlatnessSq(), 0.0);
        }
    }

    public void testGetFlatnessSq2() {
        for (double[] element : curves3) {
            CubicCurve2D curve = new CubicCurve2D.Double();
            curve.setCurve(element, 0);
            assertEquals(
                    cubicToStr(curve),
                    element[8],
                    CubicCurve2D.getFlatnessSq(
                            element[0],
                            element[1],
                            element[2],
                            element[3],
                            element[4],
                            element[5],
                            element[6],
                            element[7]), 0.0);
        }
    }

    public void testGetFlatnessSq3() {
        for (double[] element : curves3) {
            CubicCurve2D curve = new CubicCurve2D.Double();
            curve.setCurve(element, 0);
            assertEquals(
                    cubicToStr(curve),
                    element[8],
                    CubicCurve2D.getFlatnessSq(element, 0), 0.0);
        }
    }

    public void testGetFlatness1() {
        for (double[] element : curves3) {
            CubicCurve2D curve = new CubicCurve2D.Double();
            curve.setCurve(element, 0);
            assertEquals(cubicToStr(curve), Math.sqrt(element[8]), curve.getFlatness(), 0.0);
        }
    }

    public void testGetFlatness2() {
        for (double[] element : curves3) {
            CubicCurve2D curve = new CubicCurve2D.Double();
            curve.setCurve(element, 0);
            assertEquals(
                    cubicToStr(curve),
                    Math.sqrt(element[8]),
                    CubicCurve2D.getFlatness(
                            element[0],
                            element[1],
                            element[2],
                            element[3],
                            element[4],
                            element[5],
                            element[6],
                            element[7]), 0.0);
        }
    }

    public void testGetFlatness3() {
        for (double[] element : curves3) {
            CubicCurve2D curve = new CubicCurve2D.Double();
            curve.setCurve(element, 0);
            assertEquals(
                    cubicToStr(curve),
                    Math.sqrt(element[8]),
                    CubicCurve2D.getFlatness(element, 0), 0.0);
        }
    }

    public void testSubdivide1() {
        for (double[][] element : curves1) {
            CubicCurve2D src1 = new CubicCurve2D.Double();
            CubicCurve2D left1 = new CubicCurve2D.Double();
            CubicCurve2D right1 = new CubicCurve2D.Double();
            src1.setCurve(element[0], 0);
            left1.setCurve(element[1], 0);
            right1.setCurve(element[2], 0);

            CubicCurve2D src2 = new CubicCurve2D.Double();
            CubicCurve2D left2 = new CubicCurve2D.Double();
            CubicCurve2D right2 = new CubicCurve2D.Double();
            src2.setCurve(element[0], 0);
            CubicCurve2D.subdivide(src2, left2, right2);

            assertEquals(src1, src2, 0.0);
            assertEquals(left1, left2, 0.0);
            assertEquals(right1, right2, 0.0);
        }
    }

    public void testSubdivide2() {
        for (double[][] element : curves2) {
            int srcOff = element[0][0] < 0.0 ? (int)-element[0][0] : 0;
            int leftOff = element[1][0] < 0.0 ? (int)-element[1][0] : 0;
            int rightOff = element[2][0] < 0.0 ? (int)-element[2][0] : 0;

            double[] src = new double[20];
            double[] left = new double[20];
            double[] right = new double[20];

            System.arraycopy(src, 0, element[0], 0, element[0].length);
            System.arraycopy(left, 0, element[1], 0, element[1].length);
            System.arraycopy(right, 0, element[2], 0, element[2].length);

            CubicCurve2D.subdivide(src, srcOff, left, leftOff, right, rightOff);

            assertEquals(element[0], src, element[0].length, 0.0);
            assertEquals(element[1], left, element[1].length, 0.0);
            assertEquals(element[2], right, element[2].length, 0.0);
        }
    }

    public void testSolveCubic1() {
        for (double[][] element : equations) {
            double eqn[] = new double[4];
            System.arraycopy(element[0], 0, eqn, 0, 4);
            String seqn =
                "[" + doubleToStr(eqn[0]) + "," +
                doubleToStr(eqn[1]) + "," +
                doubleToStr(eqn[2]) + "," +
                doubleToStr(eqn[3]) + "]";
            int roots = CubicCurve2D.solveCubic(eqn);
            assertEquals(seqn + " roots count", (int)element[1][0], roots);
            for(int j = 0; j < roots; j++) {
                assertEquals(seqn + " root(" + j + ")", element[1][1 + j], eqn[j], ROOT_DELTA);
            }
        }
    }

    public void testSolveCubic2() {
        for (double[][] element : equations) {
            double res[] = new double[3];
            double eqn[] = new double[4];
            System.arraycopy(element[0], 0, eqn, 0, 4);
            String seqn =
                "[" + doubleToStr(eqn[0]) + "," +
                doubleToStr(eqn[1]) + "," +
                doubleToStr(eqn[2]) + "," +
                doubleToStr(eqn[3]) + "]";
            int roots = CubicCurve2D.solveCubic(eqn, res);
            assertEquals(seqn + " roots count", (int)element[1][0], roots);
            assertEquals(element[0], eqn, 4, 0.0);
            for(int j = 0; j < roots; j++) {
                assertEquals(seqn + " root(" + j + ")", element[1][1 + j], res[j], ROOT_DELTA);
            }
        }
    }

    public void testClone() {
        assertEquals(c, (CubicCurve2D)c.clone(), 0.0);
    }

    public void testGetBounds() {
        for (double[][] element : bounds) {
            CubicCurve2D curve = new CubicCurve2D.Double();
            curve.setCurve(element[0], 0);
            assertEquals(
                    cubicToStr(curve),
                    new Rectangle(
                            (int)element[1][0],
                            (int)element[1][1],
                            (int)element[1][2],
                            (int)element[1][3]),
                    curve.getBounds());
        }
    }

    void checkPathIteratorDouble(PathIterator p, double[] values) {
        checkPathRule(p, PathIterator.WIND_NON_ZERO);
        checkPathMove(p, false, values[0], values[1], 0.0);
        checkPathCubic(p, true, values[2], values[3], values[4], values[5], values[6], values[7], 0.0);
    }

    void checkPathIteratorFloat(PathIterator p, float[] values) {
        checkPathRule(p, PathIterator.WIND_NON_ZERO);
        checkPathMove(p, false, values[0], values[1], 0.0f);
        checkPathCubic(p, true, values[2], values[3], values[4], values[5], values[6], values[7], 0.0f);
    }

    public void testGetPathIteratorDouble() {
        checkPathIteratorDouble(
                c.getPathIterator(null),
                new double[]{1, 2, 3, 4, 5, 6, 7, 8});
    }

    public void testGetPathIteratorFloat() {
        checkPathIteratorFloat(
                c.getPathIterator(null),
                new float[]{1, 2, 3, 4, 5, 6, 7, 8});
    }

    public void testGetPathIteratorDoubleFlat() {
        PathIterator p = new CubicCurve2D.Double(1, 1, 3, 7, 9, 5, 7, 3).getPathIterator(null, 2);
        checkPathRule(p, PathIterator.WIND_NON_ZERO);
        checkPathMove(p, false, 1, 1, 0.0);
        checkPathLine(p, false, 5.5, 5, 0.0);
        checkPathLine(p, true, 7, 3, 0.0);
    }

    public void testGetPathIteratorFloatFlat() {
        PathIterator p = new CubicCurve2D.Double(1, 1, 3, 7, 9, 5, 7, 3).getPathIterator(null, 2);
        checkPathRule(p, PathIterator.WIND_NON_ZERO);
        checkPathMove(p, false, 1, 1, 0.0f);
        checkPathLine(p, false, 5.5, 5, 0.0f);
        checkPathLine(p, true, 7, 3, 0.0f);
    }

    public void testGetPathIteratorDoubleAffine() {
        checkPathIteratorDouble(
                c.getPathIterator(AffineTransform.getTranslateInstance(2, 1)),
                new double[]{3, 3, 5, 5, 7, 7, 9, 9});
    }

    public void testGetPathIteratorFloatAffine() {
        checkPathIteratorFloat(
                c.getPathIterator(AffineTransform.getTranslateInstance(2, 1)),
                new float[]{3, 3, 5, 5, 7, 7, 9, 9});
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CubicCurve2DTest.class);
    }

}
