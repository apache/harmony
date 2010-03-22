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

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeomTestCase;


public class AffineTransformTest extends GeomTestCase {

    static { 
        SERIALIZATION_TEST = true;
    }
    
    final static double ROTATION_DELTA = 1E-5;

    double[][] type = new double[][]{

            { 1,  0,  0,  1,  0,  0, AffineTransform.TYPE_IDENTITY},
            { 1,  0,  0,  1,  2,  4, AffineTransform.TYPE_TRANSLATION},
            { 1,  0,  0,  1,  2,  0, AffineTransform.TYPE_TRANSLATION},
            { 1,  0,  0,  1,  0,  4, AffineTransform.TYPE_TRANSLATION},
            { 1,  0,  0,  1, -2, -4, AffineTransform.TYPE_TRANSLATION},
            { 3,  0,  0,  3,  0,  0, AffineTransform.TYPE_UNIFORM_SCALE},
            {-3,  0,  0, -3,  0,  0, AffineTransform.TYPE_UNIFORM_SCALE |
                                     AffineTransform.TYPE_QUADRANT_ROTATION},
            { 3,  0,  0,  5,  0,  0, AffineTransform.TYPE_GENERAL_SCALE},
            {-3,  0,  0, -5,  0,  0, AffineTransform.TYPE_GENERAL_SCALE |
                                     AffineTransform.TYPE_QUADRANT_ROTATION},
            { 0,  1, -1,  0,  0,  0, AffineTransform.TYPE_QUADRANT_ROTATION},
            { 0, -1,  1,  0,  0,  0, AffineTransform.TYPE_QUADRANT_ROTATION},
            {-1,  0,  0, -1,  0,  0, AffineTransform.TYPE_QUADRANT_ROTATION},

            { 0,  1,  1,  0,  0,  0, AffineTransform.TYPE_FLIP |
                                     AffineTransform.TYPE_QUADRANT_ROTATION},
            { 0, -1, -1,  0,  0,  0, AffineTransform.TYPE_FLIP |
                                     AffineTransform.TYPE_QUADRANT_ROTATION},
            {-1,  0,  0,  1,  3,  4, AffineTransform.TYPE_FLIP |
                                     AffineTransform.TYPE_QUADRANT_ROTATION |
                                     AffineTransform.TYPE_TRANSLATION},
            { 0,  2,  2,  0,  0,  0, AffineTransform.TYPE_FLIP |
                                     AffineTransform.TYPE_QUADRANT_ROTATION |
                                     AffineTransform.TYPE_UNIFORM_SCALE},
            { 0,  3,  2,  0,  0,  0, AffineTransform.TYPE_FLIP |
                                     AffineTransform.TYPE_QUADRANT_ROTATION |
                                     AffineTransform.TYPE_GENERAL_SCALE},
    };

    // Check concatenate/preConcatenate
    double[][][] matrix = new double[][][] {
            {{1, 0, 0, 1, 0, 0}, {1, 2, 3, 4, 5, 6}, {1, 2, 3, 4, 5, 6}},
            {{2, 3, 4, 5, 6, 7}, {1, 0, 1, 0, 1, 0}, {5, 0, 9, 0, 14, 0}},
            {{2, 3, 4, 5, 6, 7}, {0, 1, 0, 1, 0, 1}, {0, 5, 0, 9, 0, 14}},
            {{2, 3, 4, 5, 6, 7}, {7, 6, 5, 4, 3, 2}, {29, 24, 53, 44, 80, 66}},
    };

    // Check createInvers
    double[][][] invers = new double[][][] {
            {{1, 0, 0, 1, 0, 0}, {1, 0, 0, 1, 0, 0}},
            {{0, 1, -1, 0, 0, 0}, {0, -1, 1, 0, 0, 0}},
            {{1, 1, 1, 1, 0, 0}, null},
            {{4, 6, 3, 5, 1, 2}, {2.5, -3, -1.5, 2, 0.5, -1}},
            {{0, 1, -2, 0, 3, 4}, {0, -0.5, 1, 0, -4, 1.5}}
    };

    // Check transform/inversTransform/deltaTransform
    float[][][] points = new float[][][] {
             // Matrix
            {{0, 1, -2, 0, 3, 4},
             // Source point, transform point, invers point, delta point
             {0, 0,  3, 4,  -4, 1.5f,   0, 0},
             {1, 1,  1, 5,  -3,  1,  -2, 1}},

    };

    double[][] equal = new double[][] {
            {1, 2, 3, 4, 5, 6},
            {0, 2, 3, 4, 5, 6},
            {1, 0, 3, 4, 5, 6},
            {1, 2, 0, 4, 5, 6},
            {1, 2, 3, 0, 5, 6},
            {1, 2, 3, 4, 0, 6},
            {1, 2, 3, 4, 5, 0}
    };

    AffineTransform t;

    public AffineTransformTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        t = new AffineTransform(2, 3, 4, 5, 6, 7);
    }

    @Override
    protected void tearDown() throws Exception {
        t = null;
        super.tearDown();
    }

    void assertEquals(int type, double[] matrix, double delta, AffineTransform t) {
        double[] actual = new double[matrix.length];
        t.getMatrix(actual);
        assertEquals(matrixToStr(matrix) + " Type", type, t.getType());
        assertEquals("Matrix", matrix, actual, matrix.length, delta);
    }

    public void testCreate1() {
        assertEquals(
                AffineTransform.TYPE_IDENTITY,
                new double[]{1, 0, 0, 1, 0, 0},
                0.0,
                new AffineTransform());
    }

    public void testCreate2() {
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{2, 3, 4, 5, 6, 7},
                0.0,
                new AffineTransform(2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f));
    }

    public void testCreate3() {
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{2, 3, 4, 5, 6, 7},
                0.0,
                new AffineTransform(2.0, 3.0, 4.0, 5.0, 6.0, 7.0));
    }

    public void testCreate4() {
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{2, 3, 4, 5, 6, 7},
                0.0,
                new AffineTransform(new float[]{2, 3, 4, 5, 6, 7}));
    }

    public void testCreate5() {
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{2, 3, 4, 5, 6, 7},
                0.0,
                new AffineTransform(new double[]{2, 3, 4, 5, 6, 7}));
    }

    public void testCreate6() {
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{2, 3, 4, 5, 6, 7},
                0.0,
                new AffineTransform(new AffineTransform(2, 3, 4, 5, 6, 7)));
    }

    String matrixToStr(double[] matrix) {
        return "[" +
            doubleToStr(matrix[0]) + "," +
            doubleToStr(matrix[1]) + "," +
            doubleToStr(matrix[2]) + "," +
            doubleToStr(matrix[3]) + "," +
            doubleToStr(matrix[4]) + "," +
            doubleToStr(matrix[5]) + "]";
    }

    boolean typeHas(int type, int flag) {
        return (type & flag) == flag;
    }

    String typeToStr(int type) {
        String s = "";
        if (type == AffineTransform.TYPE_IDENTITY) {
            s += "IDENTITY,";
        } else {
            if (typeHas(type, AffineTransform.TYPE_FLIP)) {
                s += "FLIP,";
            }
            if (typeHas(type, AffineTransform.TYPE_GENERAL_ROTATION)) {
                s += "GEN_ROTATION,";
            }
            if (typeHas(type, AffineTransform.TYPE_GENERAL_SCALE)) {
                s += "GEN_SCALE,";
            }
            if (typeHas(type, AffineTransform.TYPE_GENERAL_TRANSFORM)) {
                s += "GEN_TRANSFORM,";
            }
            if (typeHas(type, AffineTransform.TYPE_QUADRANT_ROTATION)) {
                s += "QUAD_ROTATION,";
            }
            if (typeHas(type, AffineTransform.TYPE_TRANSLATION)) {
                s += "TRANSLATION,";
            }
            if (typeHas(type, AffineTransform.TYPE_UNIFORM_SCALE)) {
                s += "UN_SCALE,";
            }
            int all =
                AffineTransform.TYPE_FLIP |
                AffineTransform.TYPE_GENERAL_ROTATION |
                AffineTransform.TYPE_GENERAL_SCALE |
                AffineTransform.TYPE_GENERAL_TRANSFORM |
                AffineTransform.TYPE_IDENTITY |
                AffineTransform.TYPE_QUADRANT_ROTATION |
                AffineTransform.TYPE_TRANSLATION |
                AffineTransform.TYPE_UNIFORM_SCALE;
            if ((type & ~all) != 0) {
                s += Integer.toString(type & ~all) + ",";
            }
        }
        return Integer.toString(type);
    }

    public void testGetType() {
        for (double[] element : type) {
            assertEquals(
                    matrixToStr(element) + " Type",
                    (int)element[6],
                    new AffineTransform(element).getType());
        }
    }

    public void testGetScaleX() {
        assertEquals(2.0, t.getScaleX(), 0.0);
    }

    public void testGetScaleY() {
        assertEquals(5.0, t.getScaleY(), 0.0);
    }

    public void testGetShearX() {
        assertEquals(4.0, t.getShearX(), 0.0);
    }

    public void testGetShearY() {
        assertEquals(3.0, t.getShearY(), 0.0);
    }

    public void testGetTranslateX() {
        assertEquals(6.0, t.getTranslateX(), 0.0);
    }

    public void testGetTranslateY() {
        assertEquals(7.0, t.getTranslateY(), 0.0);
    }

    public void testIsEdentity() {
        assertFalse(t.isIdentity());
        assertTrue(new AffineTransform(1, 0, 0, 1, 0, 0).isIdentity());
    }

    public void testGetMatrix() {
        double[] matrix = new double[]{0, 0, 0, 0};
        t.getMatrix(matrix);
        assertEquals(new double[]{2, 3, 4, 5}, matrix, 4, 0.0);
        matrix = new double[]{0, 0, 0, 0, 0, 0};
        t.getMatrix(matrix);
        assertEquals(new double[]{2, 3, 4, 5, 6, 7}, matrix, 6, 0.0);
    }

    public void testGetDeterminant() {
        assertEquals(-2, t.getDeterminant(), 0.0);
    }

    public void testSetTransform() {
        t.setTransform(8, 9, 10, 11, 12, 13);
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{8, 9, 10, 11, 12, 13},
                0.0,
                t);
    }

    public void testSetTransform2() {
        t.setTransform(new AffineTransform(8, 9, 10, 11, 12, 13));
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{8, 9, 10, 11, 12, 13},
                0.0,
                t);
    }

    public void testSetToIdentity() {
        t.setToIdentity();
        assertEquals(
                AffineTransform.TYPE_IDENTITY,
                new double[]{1, 0, 0, 1, 0, 0},
                0.0,
                t);
    }

    public void testSetToTranslation() {
        t.setToTranslation(8, 9);
        assertEquals(
                AffineTransform.TYPE_TRANSLATION,
                new double[]{1, 0, 0, 1, 8, 9},
                0.0,
                t);
        t.setToTranslation(0, 0);
        assertEquals(
                AffineTransform.TYPE_IDENTITY,
                new double[]{1, 0, 0, 1, 0, 0},
                0.0,
                t);
    }

    public void testSetToScale() {
        t.setToScale(8, 9);
        assertEquals(
                AffineTransform.TYPE_GENERAL_SCALE,
                new double[]{8, 0, 0, 9, 0, 0},
                0.0,
                t);
        t.setToScale(1, 1);
        assertEquals(
                AffineTransform.TYPE_IDENTITY,
                new double[]{1, 0, 0, 1, 0, 0},
                0.0,
                t);
    }

    public void testSetToShear() {
        t.setToShear(8, 9);
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{1, 9, 8, 1, 0, 0},
                0.0,
                t);
        t.setToShear(0, 0);
        assertEquals(
                AffineTransform.TYPE_IDENTITY,
                new double[]{1, 0, 0, 1, 0, 0},
                0.0,
                t);
    }

    public void testSetToRotation1() {
        t.setToRotation(Math.PI * 0.5);
        assertEquals(
                AffineTransform.TYPE_QUADRANT_ROTATION,
                new double[]{0, 1, -1, 0, 0, 0},
                ROTATION_DELTA,
                t);
        t.setToRotation(Math.PI * 1.5);
        assertEquals(
                AffineTransform.TYPE_QUADRANT_ROTATION,
                new double[]{0, -1, 1, 0, 0, 0},
                ROTATION_DELTA,
                t);
        t.setToRotation(0);
        assertEquals(
                AffineTransform.TYPE_IDENTITY,
                new double[]{1, 0, 0, 1, 0, 0},
                ROTATION_DELTA,
                t);
        t.setToRotation(Math.PI * 2.0);
        assertEquals(
                AffineTransform.TYPE_IDENTITY,
                new double[]{1, 0, 0, 1, 0, 0},
                ROTATION_DELTA,
                t);
    }

    public void testSetToRotation2() {
        t.setToRotation(0, 8, 9);
        assertEquals(
                AffineTransform.TYPE_IDENTITY,
                new double[]{1, 0, 0, 1, 0, 0},
                ROTATION_DELTA,
                t);
        t.setToRotation(Math.PI * 0.5, 8, 9);
        assertEquals(
                AffineTransform.TYPE_TRANSLATION |
                AffineTransform.TYPE_QUADRANT_ROTATION,
                new double[]{0, 1, -1, 0, 17, 1},
                ROTATION_DELTA,
                t);
        t.setToRotation(Math.PI * 0.5, 0, 0);
        assertEquals(
                AffineTransform.TYPE_QUADRANT_ROTATION,
                new double[]{0, 1, -1, 0, 0, 0},
                ROTATION_DELTA,
                t);
    }

    public void testGetTranslateInstance() {
        assertEquals(
                AffineTransform.TYPE_TRANSLATION,
                new double[]{1, 0, 0, 1, 8, 9},
                0.0,
                AffineTransform.getTranslateInstance(8, 9));
        assertEquals(
                AffineTransform.TYPE_IDENTITY,
                new double[]{1, 0, 0, 1, 0, 0},
                0.0,
                AffineTransform.getTranslateInstance(0, 0));
    }

    public void testGetScaleInstance() {
        assertEquals(
                AffineTransform.TYPE_GENERAL_SCALE,
                new double[]{8, 0, 0, 9, 0, 0},
                0.0,
                AffineTransform.getScaleInstance(8, 9));
        assertEquals(
                AffineTransform.TYPE_IDENTITY,
                new double[]{1, 0, 0, 1, 0, 0},
                0.0,
                AffineTransform.getScaleInstance(1, 1));
    }

    public void testGetShearInstance() {
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{1, 9, 8, 1, 0, 0},
                0.0,
                AffineTransform.getShearInstance(8, 9));
        assertEquals(
                AffineTransform.TYPE_IDENTITY,
                new double[]{1, 0, 0, 1, 0, 0},
                0.0,
                AffineTransform.getShearInstance(0, 0));
    }

    public void testGetRotateInstance1() {
        assertEquals(
                AffineTransform.TYPE_QUADRANT_ROTATION,
                new double[]{0, 1, -1, 0, 0, 0},
                ROTATION_DELTA,
                AffineTransform.getRotateInstance(Math.PI * 0.5));
        assertEquals(
                AffineTransform.TYPE_QUADRANT_ROTATION,
                new double[]{0, -1, 1, 0, 0, 0},
                ROTATION_DELTA,
                AffineTransform.getRotateInstance(Math.PI * 1.5));
        assertEquals(
                AffineTransform.TYPE_IDENTITY,
                new double[]{1, 0, 0, 1, 0, 0},
                ROTATION_DELTA,
                AffineTransform.getRotateInstance(0));
        assertEquals(
                AffineTransform.TYPE_IDENTITY,
                new double[]{1, 0, 0, 1, 0, 0},
                ROTATION_DELTA,
                AffineTransform.getRotateInstance(Math.PI * 2.0));
    }

    public void testGetRotateInstance2() {
        assertEquals(
                AffineTransform.TYPE_IDENTITY,
                new double[]{1, 0, 0, 1, 0, 0},
                ROTATION_DELTA,
                AffineTransform.getRotateInstance(0, 8, 9));
        assertEquals(
                AffineTransform.TYPE_TRANSLATION |
                AffineTransform.TYPE_QUADRANT_ROTATION,
                new double[]{0, 1, -1, 0, 17, 1},
                ROTATION_DELTA,
                AffineTransform.getRotateInstance(Math.PI * 0.5, 8, 9));
        assertEquals(
                AffineTransform.TYPE_QUADRANT_ROTATION,
                new double[]{0, 1, -1, 0, 0, 0},
                ROTATION_DELTA,
                AffineTransform.getRotateInstance(Math.PI * 0.5, 0, 0));
    }

    public void testTranslate() {
        t.translate(0, 0);
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{2, 3, 4, 5, 6, 7},
                0.0,
                t);
        t.translate(8, 9);
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{2, 3, 4, 5, 58, 76},
                0.0,
                t);
    }

    public void testScale() {
        t.scale(1, 1);
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{2, 3, 4, 5, 6, 7},
                0.0,
                t);
        t.scale(2, 3);
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{4, 6, 12, 15, 6, 7},
                0.0,
                t);
    }

    public void testShear() {
        t.shear(0, 0);
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{2, 3, 4, 5, 6, 7},
                0.0,
                t);
        t.shear(3, 2);
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{10, 13, 10, 14, 6, 7},
                0.0,
                t);
    }

    public void testRotate1() {
        t.rotate(0);
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{2, 3, 4, 5, 6, 7},
                0.0,
                t);
        t.rotate(Math.PI * 2.0);
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{2, 3, 4, 5, 6, 7},
                0.0,
                t);
        t.rotate(Math.PI * 0.5);
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{4, 5, -2, -3, 6, 7},
                0.0,
                t);
    }

    public void testRotate2() {
        t.rotate(0, 8, 9);
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{2, 3, 4, 5, 6, 7},
                0.0,
                t);
        t.rotate(Math.PI * 2.0, 8, 9);
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{2, 3, 4, 5, 6, 7},
                0.0,
                t);
        t.rotate(Math.PI * 1.5, 8, 9);
        assertEquals(
                AffineTransform.TYPE_GENERAL_TRANSFORM,
                new double[]{-4, -5, 2, 3, 72, 89},
//                new double[]{3, -2, 5, -4, 6, 11},
                0.0,
                t);
    }

    public void testConcatenate() {
        for (double[][] element : matrix) {
            AffineTransform a = new AffineTransform(element[0]);
            AffineTransform b = new AffineTransform(element[1]);
            AffineTransform c = new AffineTransform(element[2]);
            b.concatenate(a);
            assertEquals(c, b);
        }
    }

    public void testPreConcatenate() {
        for (double[][] element : matrix) {
            AffineTransform a = new AffineTransform(element[0]);
            AffineTransform b = new AffineTransform(element[1]);
            AffineTransform c = new AffineTransform(element[2]);
            a.preConcatenate(b);
            assertEquals(c, a);
        }
    }

    public void testCreateInvers() {
        for (double[][] element : invers) {
            try {
                AffineTransform at = new AffineTransform(element[0]);
                AffineTransform it = at.createInverse();
                if (element[1] == null) {
                    fail(at + " Expected exception NoninvertibleTransformException");
                } else {
                    assertEquals(new AffineTransform(element[1]), it);
                }
                at.concatenate(it);
                assertTrue(at.isIdentity());
            } catch(NoninvertibleTransformException e) {
                if (element[1] != null) {
                    fail(e.toString());
                }
            }
        }
    }

    public void testTransformPoint() {
        for (float[][] element : points) {
            AffineTransform at = new AffineTransform(element[0]);
            for(int j = 1; j < element.length; j++) {
                float x1 = element[j][0];
                float y1 = element[j][1];
                float x2 = element[j][2];
                float y2 = element[j][3];

                assertEquals(
                        new Point2D.Double(x2, y2),
                        at.transform(new Point2D.Double(x1, y1), null));
                assertEquals(
                        new Point2D.Float(x2, y2),
                        at.transform(new Point2D.Float(x1, y1), null));

                Point2D dst = new Point2D.Double();
                assertEquals(
                        new Point2D.Double(x2, y2),
                        at.transform(new Point2D.Double(x1, y1), dst));
                assertEquals(
                        new Point2D.Double(x2, y2),
                        dst);
            }
        }
    }

    public void testTransformPointArray() {
        AffineTransform at = new AffineTransform(0, 1, -2, 0, 3, 4);
        Point2D[] src = new Point2D[]{
                null,
                new Point2D.Double(0, 0),
                new Point2D.Float(1, 1),
                null};
        Point2D[] dst = new Point2D[4];
        at.transform(src, 1, dst, 2, 2);
        assertEquals(new Point2D.Double(3, 4), dst[2]);
        assertEquals(new Point2D.Float(1, 5), dst[3]);
    }

    public void testTransformPointArrayBad() {
        // Regression test HARMONY-1405
        
        AffineTransform at = new AffineTransform();
        try {
            at.transform(
                    new Point2D[] { null, null, null, null },
                    0,
                    new Point2D[] { null, null, null, null },
                    -1,
                    1);
            fail("Expected NPE");
        } catch (NullPointerException e) {
            // expected
        }
        
        try {
            at.transform(
                    new Point2D[] { null, null, null, null },
                    1,
                    new Point2D[] { null, null, null, null },
                    10,
                    1);
            fail("Expected NPE");
        } catch (NullPointerException e) {
            // expected
        }        
    }
    
    public void testTransformArray2() {
        AffineTransform at = new AffineTransform(0, 1, -2, 0, 3, 4);
        double[] src = new double[]{0, 0, 0, 0, 0, 0, 1, 1, 0, 0};
        double[] dst = new double[6];
        double[] expected = new double[]{0, 0, 3, 4, 1, 5};
        at.transform(src, 4, dst, 2, 2);
        assertEquals(expected, dst, 6, 0.0);
    }
    
    public void testTransformArray3() {
        AffineTransform at = new AffineTransform(0, 1, -2, 0, 3, 4);
        float[] src = new float[]{0, 0, 0, 0, 0, 0, 1, 1, 0, 0};
        float[] dst = new float[6];
        float[] expected = new float[]{0, 0, 3, 4, 1, 5};
        at.transform(src, 4, dst, 2, 2);
        assertEquals(expected, dst, 6, 0.0f);
    }

    public void testTransformArray4() {
        AffineTransform at = new AffineTransform(0, 1, -2, 0, 3, 4);
        float[] src = new float[]{0, 0, 0, 0, 0, 0, 1, 1, 0, 0};
        double[] dst = new double[6];
        double[] expected = new double[]{0, 0, 3, 4, 1, 5};
        at.transform(src, 4, dst, 2, 2);
        assertEquals(expected, dst, 6, 0.0);
    }

    public void testTransformArray5() {
        AffineTransform at = new AffineTransform(0, 1, -2, 0, 3, 4);
        double[] src = new double[]{0, 0, 0, 0, 0, 0, 1, 1, 0, 0};
        float[] dst = new float[6];
        float[] expected = new float[]{0, 0, 3, 4, 1, 5};
        at.transform(src, 4, dst, 2, 2);
        assertEquals(expected, dst, 6, 0.0f);
    }
    
    public void testTransformArrayOverlap1() {
        // Regresion test HARMONY-1603
        AffineTransform at = AffineTransform.getTranslateInstance(2, 3);
        float[] src = new float[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
        float[] expected = new float[]{1, 2, 3, 4, 3, 5, 5, 7, 7, 9, 9, 11, 13, 14};
        at.transform(src, 0, src, 4, 4);
        assertEquals(expected, src, src.length, 0);
    }

    public void testTransformArrayOverlap2() {
        // Regresion test HARMONY-1603
        AffineTransform at = AffineTransform.getTranslateInstance(2, 3);
        double[] src = new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
        double[] expected = new double[]{1, 2, 3, 4, 3, 5, 5, 7, 7, 9, 9, 11, 13, 14};
        at.transform(src, 0, src, 4, 4);
        assertEquals(expected, src, src.length, 0);
    }

    public void testDeltaTransform1() {
        for (float[][] element : points) {
            AffineTransform at = new AffineTransform(element[0]);
            for(int j = 1; j < element.length; j++) {
                float x1 = element[j][0];
                float y1 = element[j][1];
                float x2 = element[j][6];
                float y2 = element[j][7];

                assertEquals(
                        new Point2D.Double(x2, y2),
                        at.deltaTransform(new Point2D.Double(x1, y1), null));
                assertEquals(
                        new Point2D.Float(x2, y2),
                        at.deltaTransform(new Point2D.Float(x1, y1), null));

                Point2D dst = new Point2D.Double();
                assertEquals(
                        new Point2D.Double(x2, y2),
                        at.deltaTransform(new Point2D.Double(x1, y1), dst));
                assertEquals(
                        new Point2D.Double(x2, y2),
                        dst);
            }
        }
    }

    public void testDeltaTransform2() {
        AffineTransform at = new AffineTransform(0, 1, -2, 0, 3, 4);
        double[] src = new double[]{0, 0, 0, 0, 0, 0, 1, 1, 0, 0};
        double[] dst = new double[6];
        double[] expected = new double[]{0, 0, 0, 0, -2, 1};
        at.deltaTransform(src, 4, dst, 2, 2);
        assertEquals(expected, dst, 6, 0.0);
    }

    public void testInversTransform1() {
        try {
            new AffineTransform(1, 1, 1, 1, 1, 1).inverseTransform(new Point2D.Double(), null);
            fail("Expected exception NoninvertibleTransformException");
        } catch(NoninvertibleTransformException e) {
        }

        for (float[][] element : points) {
            AffineTransform at = new AffineTransform(element[0]);
            for(int j = 1; j < element.length; j++) {
                float x1 = element[j][0];
                float y1 = element[j][1];
                float x2 = element[j][4];
                float y2 = element[j][5];

                try {
                    assertEquals(
                            new Point2D.Double(x2, y2),
                            at.inverseTransform(new Point2D.Double(x1, y1), null));
                    assertEquals(
                            new Point2D.Float(x2, y2),
                            at.inverseTransform(new Point2D.Float(x1, y1), null));

                    Point2D dst = new Point2D.Double();
                    assertEquals(
                            new Point2D.Double(x2, y2),
                            at.inverseTransform(new Point2D.Double(x1, y1), dst));
                    assertEquals(
                            new Point2D.Double(x2, y2),
                            dst);
                } catch(NoninvertibleTransformException e) {
                    fail(e.toString());
                }
            }
        }
    }

    public void testInversTransform2() {
        AffineTransform at = new AffineTransform(0, 1, -2, 0, 3, 4);
        double[] src = new double[]{0, 0, 0, 0, 0, 0, 1, 1, 0, 0};
        double[] dst = new double[6];
        double[] expected = new double[]{0, 0, -4, 1.5, -3, 1};
        try {
            at.inverseTransform(src, 4, dst, 2, 2);
            assertEquals(expected, dst, 6, 0.0);
        } catch(NoninvertibleTransformException e) {
            fail(e.toString());
        }
    }

    public void testCreateTransformedShape() {
        AffineTransform at = new AffineTransform(0, 1, -2, 0, 3, 4);
        Shape actual = at.createTransformedShape(new Line2D.Double(1, 2, 3, 4));
        GeneralPath expected = new GeneralPath();
        expected.moveTo(-1, 5);
        expected.lineTo(-5, 7);
        assertEquals(expected.getPathIterator(null), actual.getPathIterator(null), 0.0);
    }

    public void testEquals() {
        for(int i = 0; i < equal.length; i++) {
            AffineTransform t1 = new AffineTransform(equal[i]);
            for(int j = 0; j < equal.length; j++){
                AffineTransform t2 = new AffineTransform(equal[j]);
                if (i == j) {
                    assertTrue(t1.equals(t2));
                } else {
                    assertFalse(t1.equals(t2));
                }
            }
        }
    }

    public void testHashCode() {
        for(int i = 0; i < equal.length; i++) {
            AffineTransform t1 = new AffineTransform(equal[i]);
            for(int j = 0; j < equal.length; j++){
                AffineTransform t2 = new AffineTransform(equal[j]);
                if (i == j) {
                    assertTrue("HashCode " + t1 + " " + t2, t1.hashCode() == t2.hashCode());
                } else {
                    assertTrue("HashCode " + t1 + " " + t2, t1.hashCode() != t2.hashCode());
                }
            }
        }
    }

    public void testClone() {
        assertEquals(t, t.clone());
    }

    public void testToString() {
        assertEquals(
                "java.awt.geom.AffineTransform[[2.0, 4.0, 6.0], [3.0, 5.0, 7.0]]",
                t.toString());
    }

    @Override
    public String objToStr(Object obj) {
        double[] m = new double[6];
        ((AffineTransform)obj).getMatrix(m);
        return
            obj.getClass().getName() + "[[" +
            m[0] + ", " + m[2] + ", " + m[4] + "], [" +
            m[1] + ", " + m[3] + ", " + m[5] + "]]";
    }

    public void testSerializeRead1() {
        checkRead(new AffineTransform());
    }

    public void testSerializeRead2() {
        checkRead(new AffineTransform(1, 2, 3, 4, 5, 6));
    }

    public void testSerializeWrite1() {
        checkWrite(new AffineTransform());
    }

    public void testSerializeWrite2() {
        checkWrite(new AffineTransform(1, 2, 3, 4, 5, 6));
    }

    public void createSerializeTemplates() {
        saveSerialized(new AffineTransform());
        saveSerialized(new AffineTransform(1, 2, 3, 4, 5, 6));
    }

    public static void main(String[] args) {
//        new AffineTransformTest("").createSerializeTemplates();
        junit.textui.TestRunner.run(AffineTransformTest.class);
    }

}
