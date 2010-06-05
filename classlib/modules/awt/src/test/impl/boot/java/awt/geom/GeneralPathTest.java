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
import java.awt.Shape;
import java.awt.Tools;

public class GeneralPathTest extends ShapeTestCase {

    // Test append
    float[][][][] shapes1 = new float[][][][] {
            // Source shape, shape to append, result shape, flag connect
            // Null means empty GeneralPath
            {null, null, null, {{0}}},
            {null, null, null, {{1}}},

            {null, {{PathIterator.SEG_MOVETO, 1, 2}}, {{PathIterator.SEG_MOVETO, 1, 2}}, {{0}}},
            {null, {{PathIterator.SEG_MOVETO, 1, 2}}, {{PathIterator.SEG_MOVETO, 1, 2}}, {{1}}},

            {{{PathIterator.SEG_MOVETO, 1, 2}}, null, {{PathIterator.SEG_MOVETO, 1, 2}}, {{0}}},
            {{{PathIterator.SEG_MOVETO, 1, 2}}, null, {{PathIterator.SEG_MOVETO, 1, 2}}, {{1}}},

            {{{PathIterator.SEG_MOVETO, 1, 2}},

             {{PathIterator.SEG_MOVETO, 5, 6},
              {PathIterator.SEG_LINETO, 7, 8}},

             {{PathIterator.SEG_MOVETO, 1, 2},
              {PathIterator.SEG_MOVETO, 5, 6},
              {PathIterator.SEG_LINETO, 7, 8}},
             {{0}}},

            {{{PathIterator.SEG_MOVETO, 1, 2}},

             {{PathIterator.SEG_MOVETO, 5, 6},
              {PathIterator.SEG_LINETO, 7, 8}},

             {{PathIterator.SEG_MOVETO, 1, 2},
              {PathIterator.SEG_LINETO, 5, 6},
              {PathIterator.SEG_LINETO, 7, 8}},
             {{1}}},

            {{{PathIterator.SEG_MOVETO, 1, 2},
              {PathIterator.SEG_LINETO, 3, 4}},

             {{PathIterator.SEG_MOVETO, 5, 6},
              {PathIterator.SEG_LINETO, 7, 8}},

             {{PathIterator.SEG_MOVETO, 1, 2},
              {PathIterator.SEG_LINETO, 3, 4},
              {PathIterator.SEG_MOVETO, 5, 6},
              {PathIterator.SEG_LINETO, 7, 8}},
             {{0}}},

            {{{PathIterator.SEG_MOVETO, 1, 2},
              {PathIterator.SEG_LINETO, 3, 4}},

             {{PathIterator.SEG_MOVETO, 5, 6},
              {PathIterator.SEG_LINETO, 7, 8}},

             {{PathIterator.SEG_MOVETO, 1, 2},
              {PathIterator.SEG_LINETO, 3, 4},
              {PathIterator.SEG_LINETO, 5, 6},
              {PathIterator.SEG_LINETO, 7, 8}},
             {{1}}}
    };

    // Test transform/createTransformedShape
    float[][][][] shapes2 = new float[][][][] {
            // Source path, transform matrix, result path
             {{{PathIterator.SEG_MOVETO, 1, 2},
               {PathIterator.SEG_LINETO, 3, 4},
               {PathIterator.SEG_LINETO, 5, 6}},
              {{2, 0, 0, 2, 0, 0}},
              {{PathIterator.SEG_MOVETO, 2, 4},
               {PathIterator.SEG_LINETO, 6, 8},
               {PathIterator.SEG_LINETO, 10, 12}}}
    };

    // Test getBounds/getBounds2D
    float[][][][] bounds = new float[][][][] {
            // Source path, boundary
            {null, {{0, 0, 0, 0}}},

            {{{PathIterator.SEG_MOVETO, 1, 2}},
             {{1, 2, 0, 0}}},

             {{{PathIterator.SEG_MOVETO, 1, 2},
               {PathIterator.SEG_LINETO, 3, 4}},
              {{1, 2, 2, 2}}},

             {{{PathIterator.SEG_MOVETO, 1, 2},
               {PathIterator.SEG_LINETO, 3, 4},
               {PathIterator.SEG_LINETO, 5, 1}},
              {{1, 1, 4, 3}}}

    };

    GeneralPath g;

    public GeneralPathTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        g = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreate1() {
        assertEquals(new GeneralPath(), new GeneralPath(GeneralPath.WIND_NON_ZERO), 0.0);
        assertEquals(new GeneralPath(), new GeneralPath(GeneralPath.WIND_NON_ZERO), 0.0f);
    }

    public void testCreate2() {
        assertEquals(new GeneralPath(GeneralPath.WIND_NON_ZERO, 20), new GeneralPath(GeneralPath.WIND_NON_ZERO), 0.0);
        assertEquals(new GeneralPath(GeneralPath.WIND_NON_ZERO, 20), new GeneralPath(GeneralPath.WIND_NON_ZERO), 0.0f);
    }

    public void testCreate3() {
        g.moveTo(5, 6);
        g.lineTo(7, 8);
        assertEquals(g, new GeneralPath(g), 0.0);
        assertEquals(g, new GeneralPath(g), 0.0f);
    }

    public void testConstants() {
        assertEquals("WIND_EVEN_ODD", PathIterator.WIND_EVEN_ODD, GeneralPath.WIND_EVEN_ODD);
        assertEquals("WIND_NON_ZERO", PathIterator.WIND_NON_ZERO, GeneralPath.WIND_NON_ZERO);
    }

    public void testGetWindingRule() {
        assertEquals("Rule", GeneralPath.WIND_EVEN_ODD, g.getWindingRule());
    }

    public void testSetWindingRule() {
        g.setWindingRule(GeneralPath.WIND_NON_ZERO);
        assertEquals("Rule", GeneralPath.WIND_NON_ZERO, g.getWindingRule());
        g.setWindingRule(GeneralPath.WIND_EVEN_ODD);
        assertEquals("Rule", GeneralPath.WIND_EVEN_ODD, g.getWindingRule());
        try {
            g.setWindingRule(-1);
            fail("Expected IllegalArgumentException");
        } catch(IllegalArgumentException e) {
            // IT'S OK
        }
    }

    public void testIllegalPathStateException() {
        try {
            g.lineTo(10, 20);
            fail("GeneralPath.lineTo() should throw exception IllegalPathStateException");
        } catch (IllegalPathStateException e) {
        }
        try {
            g.quadTo(10, 20, 30, 40);
            fail("GeneralPath.quadTo() should throw exception IllegalPathStateException");
        } catch (IllegalPathStateException e) {
        }
        try {
            g.curveTo(10, 20, 30, 40, 50, 60);
            fail("GeneralPath.curveTo() should throw exception IllegalPathStateException");
        } catch (IllegalPathStateException e) {
        }
        try {
            g.closePath();
            fail("GeneralPath.closePath() should throw exception IllegalPathStateException");
        } catch (IllegalPathStateException e) {
        }
    }

    public void testMoveToDouble() {
        g.moveTo(10, 20);
        PathIterator p = g.getPathIterator(null);
        checkPathMove(p, true, 10, 20, 0.0);
    }

    public void testMoveToFloat() {
        g.moveTo(10, 20);
        PathIterator p = g.getPathIterator(null);
        checkPathMove(p, true, 10, 20, 0.0f);
    }

    public void testLineToDouble() {
        g.moveTo(10, 20);
        g.lineTo(30, 40);
        PathIterator p = g.getPathIterator(null);
        checkPathMove(p, false, 10, 20, 0.0);
        checkPathLine(p, true, 30, 40, 0.0);
    }

    public void testLineToFloat() {
        g.moveTo(10, 20);
        g.lineTo(30, 40);
        PathIterator p = g.getPathIterator(null);
        checkPathMove(p, false, 10, 20, 0.0f);
        checkPathLine(p, true, 30, 40, 0.0f);
    }

    public void testQuadToDouble() {
        g.moveTo(10, 20);
        g.quadTo(30, 40, 50, 60);
        PathIterator p = g.getPathIterator(null);
        checkPathMove(p, false, 10, 20, 0.0);
        checkPathQuad(p, true, 30, 40, 50, 60, 0.0);
    }

    public void testQuadToFloat() {
        g.moveTo(10, 20);
        g.quadTo(30, 40, 50, 60);
        PathIterator p = g.getPathIterator(null);
        checkPathMove(p, false, 10, 20, 0.0f);
        checkPathQuad(p, true, 30, 40, 50, 60, 0.0f);
    }

    public void testCurveToDouble() {
        g.moveTo(10, 20);
        g.curveTo(30, 40, 50, 60, 70, 80);
        PathIterator p = g.getPathIterator(null);
        checkPathMove(p, false, 10, 20, 0.0);
        checkPathCubic(p, true, 30, 40, 50, 60, 70, 80, 0.0);
    }

    public void testCurveToFloat() {
        g.moveTo(10, 20);
        g.curveTo(30, 40, 50, 60, 70, 80);
        PathIterator p = g.getPathIterator(null);
        checkPathMove(p, false, 10, 20, 0.0f);
        checkPathCubic(p, true, 30, 40, 50, 60, 70, 80, 0.0f);
    }

    public void testClosePathDouble() {
        g.moveTo(10, 20);
        g.closePath();
        PathIterator p = g.getPathIterator(null);
        checkPathMove(p, false, 10, 20, 0.0);
        checkPathClose(p, true);
    }

    public void testClosePathFloat() {
        g.moveTo(10, 20);
        g.closePath();
        PathIterator p = g.getPathIterator(null);
        checkPathMove(p, false, 10, 20, 0.0f);
        checkPathClose(p, true);
    }

    public void testClosePath2() {
        g.moveTo(10, 20);
        g.closePath();
        g.closePath();
        PathIterator p = g.getPathIterator(null);
        checkPathMove(p, false, 10, 20, 0.0);
        checkPathClose(p, true);
    }

    public void testClosePath3() {
        g.moveTo(10, 20);
        g.lineTo(30, 40);
        g.closePath();
        g.closePath();
        PathIterator p = g.getPathIterator(null);
        checkPathMove(p, false, 10, 20, 0.0);
        checkPathLine(p, false, 30, 40, 0.0);
        checkPathClose(p, true);
    }

    GeneralPath createPath(float[][] segments) {
        GeneralPath p = new GeneralPath();
        if (segments != null) {
            for (float[] element : segments) {
                switch((int)element[0]) {
                case PathIterator.SEG_MOVETO:
                    p.moveTo(element[1], element[2]);
                    break;
                case PathIterator.SEG_LINETO:
                    p.lineTo(element[1], element[2]);
                    break;
                case PathIterator.SEG_QUADTO:
                    p.quadTo(element[1], element[2], element[3], element[4]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    p.curveTo(element[1], element[2], element[3], element[4], element[5], element[6]);
                    break;
                case PathIterator.SEG_CLOSE:
                    p.closePath();
                    break;
                }
            }
        }
        return p;
    }

    public void testAppendShape() {
        for (float[][][] element : shapes1) {
            GeneralPath src1 = createPath(element[0]);
            GeneralPath src2 = createPath(element[1]);
            GeneralPath dst = createPath(element[2]);
            boolean connect = element[3][0][0] == 1;
            src1.append(src2, connect);
            assertEquals(dst, src1, 0.0);
            assertEquals(dst, src1, 0.0f);
        }
    }

    public void testAppendPath() {
        for (float[][][] element : shapes1) {
            GeneralPath src1 = createPath(element[0]);
            GeneralPath src2 = createPath(element[1]);
            GeneralPath dst = createPath(element[2]);
            boolean connect = element[3][0][0] == 1;
            src1.append(src2.getPathIterator(null), connect);
            assertEquals(dst, src1, 0.0);
            assertEquals(dst, src1, 0.0f);
        }
    }

    public void testGetCurrentPoint() {
        assertNull(g.getCurrentPoint());
        g.moveTo(10, 20);
        assertEquals(new Point2D.Float(10, 20), g.getCurrentPoint());
        g.lineTo(30, 40);
        assertEquals(new Point2D.Float(30, 40), g.getCurrentPoint());
        g.quadTo(50, 60, 70, 80);
        assertEquals(new Point2D.Float(70, 80), g.getCurrentPoint());
        g.curveTo(90, 100, 110, 120, 130, 140);
        assertEquals(new Point2D.Float(130, 140), g.getCurrentPoint());
        g.closePath();
        assertEquals(new Point2D.Float(10, 20), g.getCurrentPoint());
        g.moveTo(150, 160);
        assertEquals(new Point2D.Float(150, 160), g.getCurrentPoint());
        g.lineTo(170, 180);
        assertEquals(new Point2D.Float(170, 180), g.getCurrentPoint());
        g.closePath();
        assertEquals(new Point2D.Float(150, 160), g.getCurrentPoint());
    }

    public void testReset1() {
        g.moveTo(10, 20);
        g.lineTo(30, 40);
        g.closePath();
        g.quadTo(50, 60, 70, 80);
        g.reset();
        PathIterator p = g.getPathIterator(null);
        assertTrue(p.isDone());
    }

    public void testReset2() {
        g.moveTo(10, 20);
        g.lineTo(30, 40);
        g.closePath();
        g.quadTo(50, 60, 70, 80);
        g.reset();
        g.moveTo(10, 20);
        g.curveTo(60, 70, 80, 90, 100, 110);
        PathIterator p = g.getPathIterator(null);
        checkPathMove(p, false, 10, 20, 0.0);
        checkPathCubic(p, true, 60, 70, 80, 90, 100, 110, 0.0);
    }

    public void testTransform() {
        for (float[][][] element : shapes2) {
            GeneralPath src = createPath(element[0]);
            GeneralPath dst = createPath(element[2]);
            AffineTransform t = new AffineTransform(element[1][0]);
            src.transform(t);
            assertEquals(dst, src, 0.0);
            assertEquals(dst, src, 0.0f);
        }
    }

    public void testCreateTransformedShape() {
        for (float[][][] element : shapes2) {
            GeneralPath src = createPath(element[0]);
            Shape dst1 = createPath(element[2]);
            AffineTransform t = new AffineTransform(element[1][0]);
            Shape dst2 = src.createTransformedShape(t);
            assertTrue(Tools.Shape.equals(dst1, dst2, 0.0));
            assertTrue(Tools.Shape.equals(dst1, dst2, 0.0f));
        }
    }

    public void testGetBounds2D() {
        for (float[][][] element : bounds) {
            GeneralPath src = createPath(element[0]);
            Rectangle2D bound = new Rectangle2D.Float(
                    element[1][0][0],
                    element[1][0][1],
                    element[1][0][2],
                    element[1][0][3]);
            assertEquals(bound, src.getBounds2D(), 0.0);
        }
    }

    public void testGetBounds() {
        for (float[][][] element : bounds) {
            GeneralPath src = createPath(element[0]);
            Rectangle2D bound = new Rectangle(
                    (int)element[1][0][0],
                    (int)element[1][0][1],
                    (int)element[1][0][2],
                    (int)element[1][0][3]);
            assertEquals(bound, src.getBounds(), 0.0);
        }
    }

    public void testClone() {
        assertEquals(g, (GeneralPath)g.clone(), 0.0);
        g.moveTo(10, 20);
        g.lineTo(30, 40);
        assertEquals(g, (GeneralPath)g.clone(), 0.0);
        g.quadTo(30, 40, 50, 60);
        assertEquals(g, (GeneralPath)g.clone(), 0.0);
        g.closePath();
        assertEquals(g, (GeneralPath)g.clone(), 0.0);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GeneralPathTest.class);
    }

}
