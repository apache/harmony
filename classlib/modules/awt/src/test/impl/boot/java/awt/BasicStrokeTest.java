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
package java.awt;

import junit.framework.TestCase;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Tools;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Arrays;
import java.net.URL;

public class BasicStrokeTest extends TestCase {

    boolean OUTPUT = System.getProperty("TEST_OUTPUT") != null;
    final double SHAPE_DELTA = 0.01;

    String shapePath, outputPath;
    BasicStroke stroke;
    Shape srcShape, dstShape;
    
    public BasicStrokeTest(String name) {
        super(name);
        
        String classPath = "../resources/shapes/" + Tools.getClasstPath(this.getClass());
        URL url = ClassLoader.getSystemClassLoader().getResource(classPath);

        assertNotNull("Path not found " + classPath, url);
        shapePath = url.getPath();
        outputPath = shapePath + "output/";
    }

    public void testCreate() {
        BasicStroke bs = new BasicStroke();
        assertNotNull(bs);
        assertEquals(bs, new BasicStroke(1.0f));
        assertEquals(bs, new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        assertEquals(bs, new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f));
        assertEquals(bs, new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f));

        assertEquals(1.0f, bs.getLineWidth(), 0.0f);
        assertEquals(BasicStroke.CAP_SQUARE, bs.getEndCap());
        assertEquals(BasicStroke.JOIN_MITER, bs.getLineJoin());
        assertEquals(10.0f, bs.getMiterLimit(), 0.0f);
        assertNull(bs.getDashArray());
        assertEquals(0.0f, bs.getDashPhase(), 0.0f);
    }

    public final void testGetLineWidth() {
        BasicStroke bs = new BasicStroke(10.0f);
        assertEquals(10.0f, bs.getLineWidth(), 0.0f);
    }

    public final void testGetEndCap() {
        BasicStroke bs = new BasicStroke(10.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        assertEquals(BasicStroke.CAP_ROUND, bs.getEndCap());
    }

    public final void testGetLineJoin() {
        BasicStroke bs = new BasicStroke(10.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        assertEquals(BasicStroke.JOIN_ROUND, bs.getLineJoin());
    }

    public final void testGetMiterLimit() {
        BasicStroke bs = new BasicStroke(10.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 20.0f);
        assertEquals(20.0f, bs.getMiterLimit(), 0.0f);
    }

    public final void testGetDashArray() {
        float dash[] = new float[] {10.0f, 20.f, 30.f};
        BasicStroke bs = new BasicStroke(10.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 20.0f, dash, 0.0f);
        assertTrue(Arrays.equals(bs.getDashArray(), dash));
    }

    public final void testGetDashPhase() {
        float dash[] = new float[] {10.0f, 20.f, 30.f};
        BasicStroke bs = new BasicStroke(10.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 20.0f, dash, 5.0f);
        assertEquals(5.0f, bs.getDashPhase(), 0.0f);
    }

    BasicStroke createSampleStroke() {
        return new BasicStroke(
                10.0f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                20.0f,
                new float[] {10.0f, 20.f, 30.f},
                5.0f);
    }

    BasicStroke[] createStrokeArray() {
        float dash[] = createSampleStroke().getDashArray();
        float dash2[] = new float[] {10.0f, 15.f, 30.f};
        return new BasicStroke[] {
            new BasicStroke(9.0f,  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 20.0f, dash,  5.0f),
            new BasicStroke(10.0f, BasicStroke.CAP_BUTT,  BasicStroke.JOIN_ROUND, 20.0f, dash,  5.0f),
            new BasicStroke(10.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 20.0f, dash,  5.0f),
            new BasicStroke(10.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 30.0f, dash,  5.0f),
            new BasicStroke(10.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 20.0f, dash2, 5.0f),
            new BasicStroke(10.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 20.0f, dash,  15.0f)
        };
    }

    public final void testEquals() {
        BasicStroke bs = createSampleStroke();
        assertTrue("Object isn't equal itself", bs.equals(bs));
        BasicStroke bsa[] = createStrokeArray();
        for (BasicStroke element : bsa) {
            assertTrue("Different objects are equal", !bs.equals(element));
        }
    }

    public final void testHashCode() {
        BasicStroke bs = createSampleStroke();
        assertTrue("Hash code isn't equal for the same object", bs.hashCode() == bs.hashCode());
        BasicStroke bsa[] = createStrokeArray();
        for (BasicStroke element : bsa) {
            assertTrue("Different objects have the same hash code", bs.hashCode() != element.hashCode());
        }
    }

    public void testCreateStrokedShape() {
        File path = new File(shapePath);
        String test[] = path.list();

        if (test == null) {
            fail("Golden files folder is empty " + path.getAbsolutePath());
        } else {
            System.out.println("Golden files folder " + path.getAbsolutePath());
        }

        for (String element : test) {
            if (element.indexOf("_d") != -1 && element.indexOf("#JAVA") == -1) {
                check(path.getAbsolutePath() + File.separator + element);
            }
        }
    }

    void check(String fileName) {
        int a = fileName.indexOf("_d");
        int b = fileName.indexOf(".shape");
        String strokeDesc = fileName.substring(a + 2, b);
        String srcName = fileName.substring(0, a) + fileName.substring(b);
//        System.out.println(SHAPE_PATH + Tools.File.extractFileName(fileName));

        BasicStroke bs = createStroke(strokeDesc);
        Shape src = Tools.Shape.load(srcName);
        Shape dstExpected = Tools.Shape.load(fileName);

        Shape dstActual = bs.createStrokedShape(src);

        String srcFile = Tools.File.extractFileName(srcName);
        String testFile = Tools.File.extractFileName(fileName);

        if (OUTPUT) {
            Tools.Shape.save(dstActual, outputPath + Tools.File.changeExt(testFile, " #ACTUAL.shape"));
            createImage(src,  outputPath + Tools.File.changeExt(srcFile, " #SOURCE.jpeg"), "SOURCE");
            createImage(dstActual, outputPath + Tools.File.changeExt(testFile, " #ACTUAL.jpeg"), "ACTUAL");
            createImage(dstExpected,  outputPath + Tools.File.changeExt(testFile, " #EXPECTED.jpeg"), "EXPECTED");
        }

        assertTrue("Non equal shape " + fileName,
                Tools.PathIterator.equals(
                        dstExpected.getPathIterator(null),
                        dstActual.getPathIterator(null),
                        SHAPE_DELTA));
    }

    BasicStroke createStroke(String text) {
        BasicStroke bs = new BasicStroke();
        float width = bs.getLineWidth();
        int cap = bs.getEndCap();
        int join = bs.getLineJoin();
        float miter = bs.getMiterLimit();
        float dash[] = bs.getDashArray();
        float dashPhase = bs.getDashPhase();

        int dashCount = 0;
        int state = 0;
        // 0 - width
        // 1 - cap
        // 2 - join
        // 3 - miter
        // 4 - dash
        // 5 - phase
        // 6 - completed

        try {
            StreamTokenizer t = new StreamTokenizer(new StringReader(text));
            t.ordinaryChar('R');
            t.ordinaryChar('S');
            t.ordinaryChar('B');
            t.ordinaryChar('M');
            while(t.nextToken() != StreamTokenizer.TT_EOF) {
                switch(t.ttype) {
                case StreamTokenizer.TT_NUMBER:
                    switch(state) {
                    case 0: // width
                        width = (float)t.nval;
                        state++;
                        break;
                    case 3:
                        miter =(float)t.nval;
                        state++;
                        break;
                    case 4: // dash
                        dash[dashCount++] = (float)t.nval;
                        break;
                    case 5: // phase
                        dashPhase = (float)t.nval;
                        state++;
                        break;
                    }
                    break;
                case '(':
                    dash = new float[10];
                    break;
                case ')':
                    float tmp[] = new float[dashCount];
                    System.arraycopy(dash, 0, tmp, 0, dashCount);
                    dash = tmp;
                    state++;
                    break;
                default:
                    switch(state) {
                    case 1: // cap
                        switch(t.ttype) {
                        case 'B':
                            cap = BasicStroke.CAP_BUTT;
                            break;
                        case 'R':
                            cap = BasicStroke.CAP_ROUND;
                            break;
                        case 'S':
                            cap = BasicStroke.CAP_SQUARE;
                            break;
                        }
                        state++;
                        break;
                    case 2: // join
                        switch(t.ttype) {
                        case 'B':
                            join = BasicStroke.JOIN_BEVEL;
                            state += 2;
                            break;
                        case 'M':
                            join = BasicStroke.JOIN_MITER;
                            state++;
                            break;
                        case 'R':
                            join = BasicStroke.JOIN_ROUND;
                            state += 2;
                            break;
                        }
                        break;
                    }
                    break;
                }
            }
        } catch (IOException e) {
            fail("Can't extract stroke description " + text);
        }

        return new BasicStroke(width, cap, join, miter, dash, dashPhase);
    }

    void createImage(Shape s, String fileName, String caption) {
        BufferedImage img = Tools.Shape.createImage(s, null, Color.gray, Color.lightGray);
        Graphics g = img.getGraphics();
        g.setColor(Color.green);
        g.drawString(caption, 10, 20);
        Tools.BufferedImage.save(img, fileName);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BasicStrokeTest.class);
    }

}
