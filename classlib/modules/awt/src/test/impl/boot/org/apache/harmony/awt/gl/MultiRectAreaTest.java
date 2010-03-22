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
package org.apache.harmony.awt.gl;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Tools;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;

import org.apache.harmony.awt.gl.MultiRectArea;


public class MultiRectAreaTest extends MultiRectAreaTestCase {

    static int MAX_ERR_COUNT = 5;
    static int STEP = 3;

    MultiRectArea area;

    OperationTest opIntersect = new OperationTest.Intersect();
    OperationTest opUnion = new OperationTest.Union();
    OperationTest opSubtract = new OperationTest.Subtract();

    static { 
        SERIALIZATION_TEST = false;
    }
    
    public MultiRectAreaTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        area = new MultiRectArea();
    }

    @Override
    protected void tearDown() throws Exception {
        area = null;
        super.tearDown();
    }

    void fillRect(int[] buf, int width, Rectangle rect, int inc) {
        for(int x = rect.x; x < rect.x + rect.width; x++) {
            for(int y = rect.y; y < rect.y + rect.height; y++) {
                buf[x + y * width] += inc;
            }
        }
    }

    void fillMultiRectArea(int[] buf, int width, MultiRectArea area, int inc) {
        Rectangle[] rect = area.getRectangles();
        for (Rectangle element : rect) {
            fillRect(buf, width, element, inc);
        }
    }

    static abstract class OperationTest {

        static class Intersect extends OperationTest {

            @Override
            String getName() {
                return "Intersect";
            }

            @Override
            MultiRectArea getResult(MultiRectArea mra1, MultiRectArea mra2) {
                return MultiRectArea.intersect(mra1, mra2);
            }

            @Override
            boolean isValid(int[] buf, int index) {
                return
                    buf[index] == 0 ||
                    buf[index] == 1 ||
                    buf[index] == 2 ||
                    buf[index] == 7;
            }

        }

        static class Union extends OperationTest {

            @Override
            String getName() {
                return "Union";
            }

            @Override
            MultiRectArea getResult(MultiRectArea mra1, MultiRectArea mra2) {
                return MultiRectArea.union(mra1, mra2);
            }

            @Override
            boolean isValid(int[] buf, int index) {
                return
                    buf[index] == 0 ||
                    buf[index] == 5 ||
                    buf[index] == 6 ||
                    buf[index] == 7;
            }

        }

        static class Subtract extends OperationTest {

            @Override
            String getName() {
                return "Subtract";
            }

            @Override
            MultiRectArea getResult(MultiRectArea mra1, MultiRectArea mra2) {
                return MultiRectArea.subtract(mra1, mra2);
            }

            @Override
            boolean isValid(int[] buf, int index) {
                return
                    buf[index] == 0 ||
                    buf[index] == 2 ||
                    buf[index] == 3 ||
                    buf[index] == 5;
            }

        }

        abstract String getName();
        abstract MultiRectArea getResult(MultiRectArea mra1, MultiRectArea mra2);
        abstract boolean isValid(int[] buf, int index);
    }

    void check(OperationTest op, MultiRectArea area1, MultiRectArea area2, String name) {

        int[] color = new int[] {
                Color.white.getRGB(),  // 0
                Color.green.getRGB(),  // 1
                Color.blue.getRGB(),   // 2
                Color.yellow.getRGB(), // 3
                Color.black.getRGB(),  // 4
                Color.red.getRGB(),    // 5
                Color.red.getRGB(),    // 6
                Color.red.getRGB()     // 7
            };

        Rectangle bounds1 = area1.getBounds();
        Rectangle bounds2 = area2.getBounds();
        int width = bounds1.width + bounds2.width + 10;
        int height = bounds1.height + bounds2.height + 10;

        area1.translate(-bounds1.x, -bounds1.y);
        area2.translate(bounds1.width - bounds2.x + 5, bounds1.height - bounds2.y + 5);

        int bufWidth = width + bounds1.width;
        int bufHeight = height + bounds1.height;

        int errCount = 0;

    outer:
        for(int j = 0; j < height; j += STEP) {
            for(int i = 0; i < width; i += STEP) {
                BufferedImage img = new BufferedImage(bufWidth, bufHeight, BufferedImage.TYPE_INT_RGB);
                int[] buf = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();

                MultiRectArea area3 = op.getResult(area1, area2);
                fillMultiRectArea(buf, bufWidth, area1, 1);
                fillMultiRectArea(buf, bufWidth, area2, 2);
                fillMultiRectArea(buf, bufWidth, area3, 4);

                boolean error = false;

                for(int k = 0; k < buf.length; k++) {
                    if (!error) {
                        error = !op.isValid(buf, k);
                    }

                    if (buf[k] > 7) {
                        buf[k] = Color.black.getRGB();
                    } else {
                        buf[k] = color[buf[k]];
                    }
                }

                if (error) {
                    errCount++;
                    Tools.BufferedImage.saveIcon(img, outputPath + name + "(" + i + "," + j + ").ico");
                    if (errCount > MAX_ERR_COUNT) {
                        break outer;
                    }
                }

                if (i + STEP < width) {
                    area1.translate(STEP, 0);
                } else {
                    area1.translate(-i, STEP);
                }
            }
        }

        if (errCount > 0) {
            fail(op.getName() + " failed");
        }

    }

    void check(OperationTest op, String name1, String name2) {
        check(
                op,
                Tools.MultiRectArea.load(shapePath + name1 + ".rect"),
                Tools.MultiRectArea.load(shapePath + name2 + ".rect"),
                op.getName() + "_" + name1 + "VS" + name2);
    }

    public void testIntersect() {
        check(opIntersect, "star", "twist");
        check(opIntersect, "chess", "star");
        check(opIntersect, "double", "rect");
/*
        check(
                new OperationTest.Intersect(),
                Tools.MultiRectArea.load(shapePath + "star.rect"),
                Tools.MultiRectArea.load(shapePath + "twist.rect"),
                "int_startVStwist");
        check(
                new OperationTest.Intersect(),
                Tools.MultiRectArea.load(shapePath + "chess.rect"),
                Tools.MultiRectArea.load(shapePath + "star.rect"),
                "int_chessVSstart");
*/
    }

    public void testUnion() {
        check(opUnion, "star", "twist");
        check(opUnion, "chess", "star");
        check(opUnion, "double", "rect");
/*
        check(
                new OperationTest.Union(),
                Tools.MultiRectArea.load(shapePath + "star.rect"),
                Tools.MultiRectArea.load(shapePath + "twist.rect"),
                "union_startVStwist");
        check(
                new OperationTest.Union(),
                Tools.MultiRectArea.load(shapePath + "chess.rect"),
                Tools.MultiRectArea.load(shapePath + "star.rect"),
                "union_chessVSstart");
        check(
                new OperationTest.Union(),
                Tools.MultiRectArea.load(shapePath + "double.rect"),
                Tools.MultiRectArea.load(shapePath + "rect.rect"),
                "union_chessVSstart");
*/
    }

    public void testSubtract() {
        check(opSubtract, "star", "twist");
        check(opSubtract, "chess", "star");
        check(opSubtract, "double", "rect");
/*
        check(
                new OperationTest.Subtract(),
                Tools.MultiRectArea.load(shapePath + "star.rect"),
                Tools.MultiRectArea.load(shapePath + "twist.rect"),
                "sub_startVStwist");
        check(
                new OperationTest.Subtract(),
                Tools.MultiRectArea.load(shapePath + "chess.rect"),
                Tools.MultiRectArea.load(shapePath + "star.rect"),
                "sub_chessVSstart");
        check(
                new OperationTest.Subtract(),
                Tools.MultiRectArea.load(shapePath + "double.rect"),
                Tools.MultiRectArea.load(shapePath + "rect.rect"),
                "sub_doubleVSrect");
                        */
    }

    Rectangle[] createRects(int[][] rect) {
        Rectangle[] r = new Rectangle[rect.length];
        for(int i = 0; i < rect.length; i++) {
            r[i] = new Rectangle(
                    rect[i][0],
                    rect[i][1],
                    rect[i][2] - rect[i][0] + 1,
                    rect[i][3] - rect[i][1] + 1);
        }
        return r;
    }

    ArrayList<Rectangle> createList(int[][] rect) {
        ArrayList<Rectangle> r = new ArrayList<Rectangle>();
        for(int i = 0; i < rect.length; i++) {
            r.add(i, new Rectangle(
                    rect[i][0],
                    rect[i][1],
                    rect[i][2] - rect[i][0] + 1,
                    rect[i][3] - rect[i][1] + 1));
        }
        return r;
    }

    public void testCreateIntersected() {
        int[][] initial = new int[][] {
                {2, 2, 4, 3},
                {4, 3, 5, 4}
        };
        int[][] expected = new int[][] {
                {2, 2, 4, 2},
                {2, 3, 3, 3},
                {4, 3, 5, 4}
        };
        assertEquals(
                createRects(expected),
                new MultiRectArea(createRects(initial)).getRectangles());
        assertEquals(
                createRects(expected),
                new MultiRectArea(createList(initial)).getRectangles());
    }

    public void testCreateNonintersected() {
        int[][] initial = new int[][] {
                {2, 2, 4, 2},
                {4, 3, 5, 4}
        };
        assertEquals(
                createRects(initial),
                new MultiRectArea(createRects(initial)).getRectangles());
        assertEquals(
                createRects(initial),
                new MultiRectArea(createList(initial)).getRectangles());
    }

    public void testIntersectEmpty() {
        MultiRectArea mra = new MultiRectArea(new Rectangle(10, 10, 30, 40));
        MultiRectArea empty = new MultiRectArea();

        assertEquals(empty, MultiRectArea.intersect(mra, empty));
        assertEquals(empty, MultiRectArea.intersect(empty, mra));
        assertEquals(empty, MultiRectArea.intersect(mra, null));
        assertEquals(empty, MultiRectArea.intersect(null, mra));

        assertEquals(empty, MultiRectArea.intersect(empty, empty));
        assertEquals(empty, MultiRectArea.intersect(null, empty));
        assertEquals(empty, MultiRectArea.intersect(empty, null));
        assertEquals(empty, MultiRectArea.intersect(null, null));
    }

    public void testUnionEmpty() {
        MultiRectArea mra = new MultiRectArea(new Rectangle(10, 10, 30, 40));
        MultiRectArea empty = new MultiRectArea();

        assertEquals(mra, MultiRectArea.union(mra, empty));
        assertEquals(mra, MultiRectArea.union(empty, mra));
        assertEquals(mra, MultiRectArea.union(mra, null));
        assertEquals(mra, MultiRectArea.union(null, mra));

        assertEquals(empty, MultiRectArea.union(empty, empty));
        assertEquals(empty, MultiRectArea.union(null, empty));
        assertEquals(empty, MultiRectArea.union(empty, null));
        assertEquals(empty, MultiRectArea.union(null, null));
    }

    public void testSubtractEmpty() {
        MultiRectArea mra = new MultiRectArea(new Rectangle(10, 10, 30, 40));
        MultiRectArea empty = new MultiRectArea();

        assertEquals(mra, MultiRectArea.subtract(mra, empty));
        assertEquals(empty, MultiRectArea.subtract(empty, mra));
        assertEquals(mra, MultiRectArea.subtract(mra, null));
        assertEquals(empty, MultiRectArea.subtract(null, mra));

        assertEquals(empty, MultiRectArea.subtract(empty, empty));
        assertEquals(empty, MultiRectArea.subtract(null, empty));
        assertEquals(empty, MultiRectArea.subtract(empty, null));
        assertEquals(empty, MultiRectArea.subtract(null, null));
    }

    void checkPathIteratorDouble(PathIterator p, double[] values) {
        checkPathRule(p, PathIterator.WIND_NON_ZERO);
        for(int i = 0; i < values.length;) {
            int j = i % 8;
            if (j == 0) {
                checkPathMove(p, false, values[i++], values[i++], 0.0);
            } else if (j < 6) {
                checkPathLine(p, false, values[i++], values[i++], 0.0);
            } else {
                checkPathLine(p, false, values[i++], values[i++], 0.0);
                checkPathClose(p, i >= values.length);
            }
        }
        checkPathDone(p, true);
    }

    void checkPathIteratorFloat(PathIterator p, float[] values) {
        checkPathRule(p, PathIterator.WIND_NON_ZERO);
        for(int i= 0; i < values.length;) {
            int j = i % 8;
            if (j == 0) {
                checkPathMove(p, false, values[i++], values[i++], 0.0f);
            } else if (j < 6) {
                checkPathLine(p, false, values[i++], values[i++], 0.0f);
            } else {
                checkPathLine(p, false, values[i++], values[i++], 0.0f);
                checkPathClose(p, i >= values.length);
            }
        }
        checkPathDone(p, true);
    }

    public void testGetPathIteratorDouble() {
        MultiRectArea mra = new MultiRectArea();
        checkPathIteratorDouble(
                mra.getPathIterator(null),
                new double[]{});

        mra = new MultiRectArea(1, 2, 4, 6);
        checkPathIteratorDouble(
                mra.getPathIterator(null),
                new double[]{1, 2, 4, 2, 4, 6, 1, 6});

        mra.addRect(4, 1, 6, 4);
        checkPathIteratorDouble(
                mra.getPathIterator(null),
                new double[]{
                        1, 2, 4, 2, 4, 6, 1, 6,
                        4, 1, 6, 1, 6, 4, 4, 4});

        checkPathIteratorDouble(
                mra.getPathIterator(null, 0),
                new double[]{
                        1, 2, 4, 2, 4, 6, 1, 6,
                        4, 1, 6, 1, 6, 4, 4, 4});

        checkPathIteratorDouble(
                mra.getPathIterator(AffineTransform.getTranslateInstance(1, 2)),
                new double[]{
                        2, 4, 5, 4, 5, 8, 2, 8,
                        5, 3, 7, 3, 7, 6, 5, 6});
    }

    public void testGetPathIteratorFloat() {
        MultiRectArea mra = new MultiRectArea();
        checkPathIteratorFloat(
                mra.getPathIterator(null),
                new float[]{});

        mra = new MultiRectArea(1, 2, 4, 6);
        checkPathIteratorFloat(
                mra.getPathIterator(null),
                new float[]{1, 2, 4, 2, 4, 6, 1, 6});

        mra.addRect(4, 1, 6, 4);
        checkPathIteratorFloat(
                mra.getPathIterator(null),
                new float[]{
                        1, 2, 4, 2, 4, 6, 1, 6,
                        4, 1, 6, 1, 6, 4, 4, 4});
    }


/*

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

*/
    public static void main(String[] args) {
        junit.textui.TestRunner.run(MultiRectAreaTest.class);
    }

}
