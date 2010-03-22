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
import java.awt.Rectangle;
import java.awt.Toolkit;
import javax.swing.BasicSwingTestCase;
import javax.swing.SwingConstants;
import javax.swing.text.GlyphView.GlyphPainter;
import javax.swing.text.Position.Bias;
import junit.framework.TestCase;

/**
 * Tests default implementation of GlyphPainter abstract class.
 *
 */
public class DefaultGlyphPainterTest extends TestCase {
    private GlyphPainter painter;

    private GlyphView view;

    private StyledDocument doc;

    private Element root;

    private Element leaf;

    private Rectangle alloc;

    private Font font;

    private FontMetrics metrics;

    private static final String FULL_TEXT = "this text to check how view breaks";

    //   0123456789012345678901234567890123
    private static final int startOffset = 5;

    private static final int endOffset = 28;

    private static final int length = endOffset - startOffset;

    private static final String LEAF_TEXT = FULL_TEXT.substring(startOffset, endOffset);

    @SuppressWarnings("deprecation")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        root = doc.getDefaultRootElement();
        doc.insertString(0, FULL_TEXT, null);
        doc.setCharacterAttributes(startOffset, length, SimpleAttributeSet.EMPTY, false);
        leaf = root.getElement(0).getElement(1);
        view = new GlyphView(leaf);
        view.checkPainter();
        painter = view.getGlyphPainter();
        font = view.getFont();
        metrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
        alloc = new Rectangle(12, 21, 129, 17);
        alloc.width = getX(length - 1) - 1 - alloc.x;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetSpan() {
        assertEquals(metrics.stringWidth(LEAF_TEXT), (int) painter.getSpan(view, startOffset,
                endOffset, null, 0));
        final int end = LEAF_TEXT.indexOf(' ') + 1;
        assertEquals(metrics.stringWidth(LEAF_TEXT.substring(0, end)), (int) painter.getSpan(
                view, startOffset, startOffset + end, null, 0));
    }

    public void testGetHeight() {
        assertEquals(metrics.getHeight(), (int) painter.getHeight(view));
    }

    public void testGetAscent() {
        assertEquals(metrics.getAscent(), (int) painter.getAscent(view));
    }

    public void testGetDescent() {
        assertEquals(metrics.getDescent(), (int) painter.getDescent(view));
    }

    public void testModelToView() throws BadLocationException {
        assertEquals(new Rectangle(alloc.x, alloc.y, 0, metrics.getHeight()), painter
                .modelToView(view, startOffset, Bias.Forward, alloc));
        assertEquals(new Rectangle(alloc.x, alloc.y, 0, metrics.getHeight()), painter
                .modelToView(view, startOffset, Bias.Backward, alloc));
        assertEquals(new Rectangle(getX(1), alloc.y, 0, metrics.getHeight()), painter
                .modelToView(view, startOffset + 1, Bias.Forward, alloc));
        assertEquals(new Rectangle(getX(2), alloc.y, 0, metrics.getHeight()), painter
                .modelToView(view, startOffset + 2, Bias.Forward, alloc));
        assertEquals(new Rectangle(getX(length - 1), alloc.y, 0, metrics.getHeight()), painter
                .modelToView(view, endOffset - 1, Bias.Forward, alloc));
        assertTrue(getX(length - 1) > alloc.x + alloc.width);
        assertEquals(new Rectangle(BasicSwingTestCase.isHarmony() ? getX(length) : alloc.x
                + alloc.width, alloc.y, 0, metrics.getHeight()), painter.modelToView(view,
                endOffset, Bias.Forward, alloc));
    }

    public void testViewToModel() {
        Bias[] bias = new Bias[1];
        assertEquals(startOffset, painter.viewToModel(view, alloc.x - 1, alloc.y, alloc, bias));
        assertSame(Bias.Forward, bias[0]);
        assertEquals(startOffset, painter.viewToModel(view, alloc.x, alloc.y - 1, alloc, bias));
        assertSame(Bias.Forward, bias[0]);
        assertEquals(startOffset, painter.viewToModel(view, alloc.x, alloc.y, alloc, bias));
        assertSame(Bias.Forward, bias[0]);
        assertEquals(startOffset + 1, painter.viewToModel(view, getX(1), alloc.y, alloc, bias));
        assertSame(Bias.Forward, bias[0]);
        assertEquals(startOffset + 2, painter.viewToModel(view, getX(2), alloc.y, alloc, bias));
        assertSame(Bias.Forward, bias[0]);
        assertEquals(endOffset - 1, painter.viewToModel(view, getX(length - 1), alloc.y, alloc,
                bias));
        assertSame(Bias.Forward, bias[0]);
        assertEquals(endOffset - 1, painter.viewToModel(view, getX(length), alloc.y, alloc,
                bias));
        assertSame(Bias.Forward, bias[0]);
        assertEquals(endOffset - 1, painter.viewToModel(view, getX(length) + 100, alloc.y,
                alloc, bias));
        assertSame(Bias.Forward, bias[0]);
        assertEquals(endOffset - 1, painter.viewToModel(view, getX(length), alloc.y
                + alloc.height + 10, alloc, bias));
        assertSame(Bias.Forward, bias[0]);
    }

    public void testGetBoundedPosition() {
        // No char can fit; the same offset is returned
        assertEquals(startOffset + 1, painter.getBoundedPosition(view, startOffset + 1,
                alloc.x, 1));
        // width is the width of the first char
        float width = metrics.stringWidth(LEAF_TEXT.substring(0, 1));
        assertEquals(startOffset, painter.getBoundedPosition(view, startOffset, alloc.x,
                width - 0.01f));
        assertEquals(startOffset + 1, painter.getBoundedPosition(view, startOffset, alloc.x,
                width));
        assertEquals(startOffset + 1, painter.getBoundedPosition(view, startOffset, alloc.x,
                width + 0.01f));
        // width includes two chars
        width = metrics.stringWidth(LEAF_TEXT.substring(0, 2));
        assertEquals(startOffset + 2, painter.getBoundedPosition(view, startOffset, alloc.x,
                width));
        // Two chars (the second and the third) starting not at the beginning
        // of the view
        width = metrics.stringWidth(LEAF_TEXT.substring(1, 3));
        assertEquals(startOffset + 3, painter.getBoundedPosition(view, startOffset + 1,
                alloc.x, width));
        // Can't return offset greater than view.getEndOffset()
        assertEquals(endOffset, painter.getBoundedPosition(view, endOffset, alloc.x, width));
    }

    public void testGetPainter() {
        assertSame(painter, painter.getPainter(view, startOffset, endOffset));
        assertSame(painter, painter.getPainter(new GlyphView(leaf), startOffset, endOffset));
        assertSame(painter, painter.getPainter(view, 0, 4));
        assertSame(painter, painter.getPainter(view, endOffset + 5, endOffset + 10));
    }

    public void testGetNextVisualPositionFromWest() throws BadLocationException {
        final boolean isHarmony = BasicSwingTestCase.isHarmony();
        Bias[] bias = new Bias[1];
        assertEquals(-1, painter.getNextVisualPositionFrom(view, startOffset, Bias.Forward,
                alloc, SwingConstants.WEST, bias));
        assertNull(bias[0]);
        assertEquals(isHarmony ? -1 : startOffset - 2, painter.getNextVisualPositionFrom(view,
                startOffset - 1, Bias.Forward, alloc, SwingConstants.WEST, bias));
        if (isHarmony) {
            assertNull(bias[0]);
        } else {
            assertSame(Bias.Forward, bias[0]);
            bias[0] = null;
        }
        assertEquals(startOffset, painter.getNextVisualPositionFrom(view, startOffset + 1,
                Bias.Forward, alloc, SwingConstants.WEST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
        assertEquals(startOffset + 1, painter.getNextVisualPositionFrom(view, startOffset + 2,
                Bias.Forward, alloc, SwingConstants.WEST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
    }

    public void testGetNextVisualPositionFromEast() throws BadLocationException {
        final boolean isHarmony = BasicSwingTestCase.isHarmony();
        Bias[] bias = new Bias[1];
        assertEquals(-1, painter.getNextVisualPositionFrom(view, endOffset, Bias.Forward,
                alloc, SwingConstants.EAST, bias));
        assertNull(bias[0]);
        assertEquals(isHarmony ? -1 : endOffset + 2, painter.getNextVisualPositionFrom(view,
                endOffset + 1, Bias.Forward, alloc, SwingConstants.EAST, bias));
        if (isHarmony) {
            assertNull(bias[0]);
        } else {
            assertSame(Bias.Forward, bias[0]);
            bias[0] = null;
        }
        assertEquals(-1, painter.getNextVisualPositionFrom(view, endOffset - 1, Bias.Forward,
                alloc, SwingConstants.EAST, bias));
        assertNull(bias[0]);
        assertEquals(endOffset - 1, painter.getNextVisualPositionFrom(view, endOffset - 2,
                Bias.Forward, alloc, SwingConstants.EAST, bias));
        assertSame(Bias.Forward, bias[0]);
        bias[0] = null;
    }

    public void testGetNextVisualPositionFromNorth() throws BadLocationException {
        Bias[] bias = new Bias[1];
        assertEquals(-1, painter.getNextVisualPositionFrom(view, startOffset, Bias.Forward,
                alloc, SwingConstants.NORTH, bias));
        assertNull(bias[0]);
        assertEquals(-1, painter.getNextVisualPositionFrom(view, startOffset - 1, Bias.Forward,
                alloc, SwingConstants.NORTH, bias));
        assertNull(bias[0]);
        assertEquals(-1, painter.getNextVisualPositionFrom(view, startOffset + 1, Bias.Forward,
                alloc, SwingConstants.NORTH, bias));
        assertNull(bias[0]);
        assertEquals(-1, painter.getNextVisualPositionFrom(view, startOffset + 2, Bias.Forward,
                alloc, SwingConstants.NORTH, bias));
        assertNull(bias[0]);
    }

    public void testGetNextVisualPositionFromSouth() throws BadLocationException {
        Bias[] bias = new Bias[1];
        assertEquals(-1, painter.getNextVisualPositionFrom(view, endOffset, Bias.Forward,
                alloc, SwingConstants.SOUTH, bias));
        assertNull(bias[0]);
        assertEquals(-1, painter.getNextVisualPositionFrom(view, endOffset + 1, Bias.Forward,
                alloc, SwingConstants.SOUTH, bias));
        assertNull(bias[0]);
        assertEquals(-1, painter.getNextVisualPositionFrom(view, endOffset - 1, Bias.Forward,
                alloc, SwingConstants.SOUTH, bias));
        assertNull(bias[0]);
        assertEquals(-1, painter.getNextVisualPositionFrom(view, endOffset - 2, Bias.Forward,
                alloc, SwingConstants.SOUTH, bias));
        assertNull(bias[0]);
    }

    private int getX(final int end) {
        return metrics.stringWidth(LEAF_TEXT.substring(0, end)) + alloc.x;
    }
}
