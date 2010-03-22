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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import javax.swing.BasicSwingTestCase;
import javax.swing.SwingConstants;
import javax.swing.text.GlyphView.GlyphPainter;
import javax.swing.text.Position.Bias;

/**
 * Tests GlyphView class.
 *
 */
public class GlyphViewTest extends BasicSwingTestCase {
    protected static class EmptyPainter extends GlyphPainter {
        @Override
        public float getSpan(GlyphView v, int p0, int p1, TabExpander e, float x) {
            return 0;
        }

        @Override
        public float getHeight(GlyphView v) {
            return 0;
        }

        @Override
        public float getAscent(GlyphView v) {
            return 0;
        }

        @Override
        public float getDescent(GlyphView v) {
            return 0;
        }

        @Override
        public void paint(GlyphView v, Graphics g, Shape a, int p0, int p1) {
        }

        @Override
        public Shape modelToView(GlyphView v, int pos, Bias bias, Shape a)
                throws BadLocationException {
            return null;
        }

        @Override
        public int viewToModel(GlyphView v, float x, float y, Shape a, Bias[] biasRet) {
            return 0;
        }

        @Override
        public int getBoundedPosition(GlyphView v, int p0, float x, float len) {
            return 0;
        }

        @Override
        public String toString() {
            return "GlyphViewTest.EmptyPainter";
        }
    }

    private static final float ACCURACY = 0.00001f;

    private GlyphView view;

    private StyledDocument doc;

    private Element root;

    private Element leaf;

    private static final String TEXT = "012345";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        root = doc.getDefaultRootElement();
        doc.insertString(0, TEXT, null);
        doc.setCharacterAttributes(0, TEXT.length() - 1, SimpleAttributeSet.EMPTY, false);
        leaf = root.getElement(0).getElement(0);
        view = new GlyphView(leaf);
    }

    public void testGlyphView() {
        view = new GlyphView(root);
        assertSame(root, view.getElement());
        assertNull(view.getGlyphPainter());
    }

    @SuppressWarnings("deprecation")
    public void testGetPreferredSpan() {
        assertNull(view.getGlyphPainter());
        Font font = view.getFont();
        FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
        assertEquals(metrics.stringWidth(TEXT.substring(view.getStartOffset(), view
                .getEndOffset())), view.getPreferredSpan(View.X_AXIS), ACCURACY);
        assertEquals(metrics.getHeight(), view.getPreferredSpan(View.Y_AXIS), ACCURACY);
        assertNotNull(view.getGlyphPainter());
    }

    public void testGetPreferredSpanCustomPainter() {
        view.setGlyphPainter(new EmptyPainter() {
            @Override
            public float getSpan(GlyphView v, int p0, int p1, TabExpander e, float x) {
                return (p1 - p0) * 0.4261f;
            }

            @Override
            public float getHeight(GlyphView v) {
                return 0.524f;
            }
        });
        assertEquals((view.getEndOffset() - view.getStartOffset()) * 0.4261f, view
                .getPreferredSpan(View.X_AXIS), ACCURACY);
        assertEquals(0.524f, view.getPreferredSpan(View.Y_AXIS), ACCURACY);
    }

    public void testGetStartOffset() {
        assertEquals(leaf.getStartOffset(), view.getStartOffset());
    }

    public void testGetEndOffset() {
        assertEquals(leaf.getEndOffset(), view.getEndOffset());
    }

    public void testSetGetGlyphPainter() {
        assertNull(view.getGlyphPainter());
        GlyphPainter painter = new EmptyPainter();
        view.setGlyphPainter(painter);
        assertSame(painter, view.getGlyphPainter());
    }

    public void testGetText() {
        Segment docText = view.getText(0, doc.getLength() + 1);
        assertEquals(TEXT + "\n", docText.toString());
        Segment text = view.getText(2, leaf.getEndOffset());
        assertNotSame(docText, text);
        assertEquals(TEXT.substring(2, TEXT.length() - 1), text.toString());
        try {
            view.getText(3, 2).toString();
            fail("an Error is expected");
        } catch (Error e) {
            //            System.out.println(e.getMessage());
        }
    }

    public void testGetTabExpander() throws BadLocationException {
        assertNull(view.getTabExpander());
        View plainView = new ParagraphView(root.getElement(0));
        assertTrue(plainView instanceof TabExpander);
        plainView.replace(0, 0, new View[] { view });
        assertTrue(view.getParent() instanceof TabExpander);
        if (isHarmony()) {
            assertSame(plainView, view.getTabExpander());
        } else {
            assertNull(view.getTabExpander());
        }
        doc.insertString(1, "\t", null);
        if (isHarmony()) {
            assertSame(plainView, view.getTabExpander());
        } else {
            assertNull(view.getTabExpander());
        }
    }

    @SuppressWarnings("deprecation")
    public void testGetTabbedSpan() throws BadLocationException {
        final Font font = view.getFont();
        final FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
        TabExpander expander = new TabExpander() {
            public float nextTabStop(float x, int tabOffset) {
                return (x / 10 + 1) * 10;
            }
        };
        final String leafText = view.getText(view.getStartOffset(), view.getEndOffset())
                .toString();
        assertEquals(metrics.stringWidth(leafText), (int) view.getTabbedSpan(0, expander));
        doc.insertString(2, "\t", null); // leafText doesn't contain tab
        final int beforeTab = metrics.stringWidth(leafText.substring(0, 2));
        assertEquals(metrics.stringWidth(leafText.substring(2))
                + (int) expander.nextTabStop(beforeTab, 2), (int) view.getTabbedSpan(0,
                expander));
    }

    @SuppressWarnings("deprecation")
    public void testGetPartialSpan() {
        final Font font = view.getFont();
        final FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
        final String leafText = view.getText(view.getStartOffset(), view.getEndOffset())
                .toString();
        assertEquals(metrics.stringWidth(leafText), (int) view.getPartialSpan(view
                .getStartOffset(), view.getEndOffset()));
        final String subText = leafText.substring(2, 4);
        assertEquals(metrics.stringWidth(subText), (int) view.getPartialSpan(2, 4));
    }

    public void testCheckPainter() {
        assertNull(view.getGlyphPainter());
        view.checkPainter();
        assertNotNull(view.getGlyphPainter());
        GlyphView other = new GlyphView(root);
        other.checkPainter();
        assertSame(view.getGlyphPainter(), other.getGlyphPainter());
    }

    /**
     * Shape modelToView(int, Shape, Bias)
     * This assures a default GlyphPainter is created.
     */
    public void testModelToView() throws BadLocationException {
        assertNull(view.getGlyphPainter());
        view.modelToView(0, new Rectangle(), Bias.Forward);
        assertNotNull(view.getGlyphPainter());
    }

    /**
     * int viewToModel(float, float, Shape, Bias[])
     * This assures a default GlyphPainter is created.
     */
    public void testViewToModel() {
        assertNull(view.getGlyphPainter());
        view.viewToModel(0, 0, new Rectangle(), new Bias[1]);
        assertNotNull(view.getGlyphPainter());
    }

    /**
     * This assures a default GlyphPainter is created.
     */
    public void testGetNextVisualPositionFrom() throws BadLocationException {
        assertNull(view.getGlyphPainter());
        try {
            view.getNextVisualPositionFrom(0, Bias.Forward, new Rectangle(),
                    SwingConstants.EAST, new Bias[1]);
            fail("No default painter is installed when this method is called");
        } catch (NullPointerException e) {
        }
        view.setGlyphPainter(new EmptyPainter());
        assertNotNull(view.getGlyphPainter());
        // Causes no exception
        view.getNextVisualPositionFrom(0, Bias.Forward, new Rectangle(), SwingConstants.EAST,
                new Bias[1]);
    }
}
