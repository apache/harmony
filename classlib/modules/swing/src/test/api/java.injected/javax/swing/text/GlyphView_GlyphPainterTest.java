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
 * @author Alexey A. Ivanov
 */
package javax.swing.text;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import javax.swing.BasicSwingTestCase;
import javax.swing.SwingConstants;
import javax.swing.text.Position.Bias;

/**
 * Tests how GlyphView uses GlyphPainter.
 *
 */
public class GlyphView_GlyphPainterTest extends BasicSwingTestCase {
    private static final float ACCURACY = 0.00001f;

    private GlyphView view;

    private StyledDocument doc;

    private Element root;

    private Element leaf;

    private Rectangle shape;

    private static ByteArrayOutputStream stream;

    private static PrintStream out;

    private static final String TEXT = "012345";

    private static final float DESCENT = 6;

    private static final float ASCENT = -30;

    private static final float HEIGHT = 20;

    private static class Painter extends GlyphView.GlyphPainter {
        @Override
        public float getSpan(GlyphView v, int p0, int p1, TabExpander e, float x) {
            out.println("getSpan(" + v + ", " + p0 + ", " + p1 + ", " + e + ", " + x + ")");
            return 0.125f;
        }

        @Override
        public float getHeight(GlyphView v) {
            out.println("getHeight(" + v + ")");
            return HEIGHT;
        }

        @Override
        public float getAscent(GlyphView v) {
            out.println("getAscent(" + v + ")");
            return ASCENT;
        }

        @Override
        public float getDescent(GlyphView v) {
            out.println("getDescent(" + v + ")");
            return DESCENT;
        }

        @Override
        public void paint(GlyphView v, Graphics g, Shape a, int p0, int p1) {
            out.println("paint(" + v + ", " + a + ", " + p0 + ", " + p1 + ")");
        }

        @Override
        public Shape modelToView(GlyphView v, int pos, Bias bias, Shape a)
                throws BadLocationException {
            out.println("modelToView(" + v + ", " + pos + ", " + bias + ", " + a + ")");
            return null;
        }

        @Override
        public int viewToModel(GlyphView v, float x, float y, Shape a, Bias[] biasRet) {
            out.println("viewToModel(" + v + ", " + x + ", " + y + ", " + a + ")");
            return 0;
        }

        @Override
        public int getBoundedPosition(GlyphView v, int p0, float x, float len) {
            out.println("getBoundedPosition(" + v + ", " + p0 + ", " + x + ", " + len + ")");
            return 0;
        }

        @Override
        public int getNextVisualPositionFrom(GlyphView v, int pos, Bias b, Shape a,
                int direction, Bias[] biasRet) throws BadLocationException {
            out.println("getNextVisualPositionFrom(" + v + ", " + pos + ", " + b + ", " + a
                    + ", " + direction + ")");
            int result = super.getNextVisualPositionFrom(v, pos, b, a, direction, biasRet);
            out.println("--> result " + result);
            return result;
        }

        @Override
        public String toString() {
            return "GlyphView_GlyphPainterTest.Painter";
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        stream = new ByteArrayOutputStream();
        out = new PrintStream(stream);
        doc = new DefaultStyledDocument();
        root = doc.getDefaultRootElement();
        doc.insertString(0, TEXT, null);
        doc.setCharacterAttributes(0, TEXT.length() - 1, SimpleAttributeSet.EMPTY, false);
        leaf = root.getElement(0).getElement(0);
        view = new GlyphView(leaf) {
            @Override
            public String toString() {
                return "thisTest.view";
            }
        };
        shape = new Rectangle(23, 41, 523, 671);
        view.setGlyphPainter(new Painter());
        //        System.out.println(">>> " + getName());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        //        System.out.print(stream.toString());
        //        System.out.println("<<< " + getName() + "\n\n");
    }

    public void testGetPreferredSpan() {
        view.getPreferredSpan(View.X_AXIS);
        assertEquals("getSpan(thisTest.view, 0, 5, null, 0.0)\n", getFilteredString());
        stream.reset();
        view.getPreferredSpan(View.Y_AXIS);
        assertEquals("getHeight(thisTest.view)\n", getFilteredString());
    }

    public void testGetPartialSpan() {
        view.getPartialSpan(2, 4);
        assertEquals("getSpan(thisTest.view, 2, 4, null, 0.0)\n", getFilteredString());
    }

    public void testGetTabbedSpan() {
        view.getTabbedSpan(1.5f, null);
        assertEquals("getSpan(thisTest.view, 0, 5, null, 1.5)\n", getFilteredString());
    }

    public void testGetTabExpander() {
        assertNull(view.getTabExpander());
        assertEquals("", stream.toString());
    }

    public void testModelToView() throws BadLocationException {
        view.modelToView(2, new Rectangle(13, 21, 500, 200), Bias.Forward);
        assertEquals("modelToView(thisTest.view, 2, Forward, "
                + "java.awt.Rectangle[x=13,y=21,width=500,height=200])\n", getFilteredString());
    }

    public void testViewToModel() {
        Bias[] bias = new Bias[1];
        view.viewToModel(17, 16, shape, bias);
        assertEquals("viewToModel(thisTest.view, 17.0, 16.0, "
                + "java.awt.Rectangle[x=23,y=41,width=523,height=671])\n", getFilteredString());
        assertNull(bias[0]);
    }

    public void testGetNextVisualPositionFrom() throws BadLocationException {
        Bias[] bias = new Bias[1];
        view.getNextVisualPositionFrom(3, Bias.Forward, shape, SwingConstants.EAST, bias);
        assertEquals("getNextVisualPositionFrom(thisTest.view, 3, Forward, "
                + "java.awt.Rectangle[x=23,y=41,width=523,height=671]," + " 3)\n"
                + "--> result 4\n", getFilteredString());
        assertSame(Bias.Forward, bias[0]);
    }

    public void testGetAlignment() {
        assertEquals(0.5f, view.getAlignment(View.X_AXIS), ACCURACY);
        assertEquals("", stream.toString());
        assertEquals((HEIGHT - DESCENT) / HEIGHT, view.getAlignment(View.Y_AXIS), ACCURACY);
        assertEquals("getHeight(thisTest.view)\n" + "getDescent(thisTest.view)\n"
                + (!isHarmony() ? "getAscent(thisTest.view)\n" : ""), getFilteredString());
    }

    public void testGetBreakWeight() {
        view.getBreakWeight(View.X_AXIS, shape.x + 5.2f, 16);
        assertEquals("getBoundedPosition(thisTest.view, 0, 28.2, 16.0)\n", getFilteredString());
    }

    public void testPaint() {
        view.paint(createTestGraphics(), shape);
        assertEquals("paint(thisTest.view, "
                + "java.awt.Rectangle[x=23,y=41,width=523,height=671], " + "0, 5)\n",
                getFilteredString());
    }

    private String getFilteredString() {
        return stream.toString().replaceAll("\r", "");
    }
}
