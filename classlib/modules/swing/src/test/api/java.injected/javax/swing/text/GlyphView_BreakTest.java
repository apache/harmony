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
import java.awt.Toolkit;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AbstractDocument.LeafElement;
import javax.swing.text.GlyphViewTest.EmptyPainter;
import junit.framework.TestCase;

/**
 * Tests breaking behavior of GlyphView.
 *
 */
public class GlyphView_BreakTest extends TestCase {
    private GlyphView view;

    private StyledDocument doc;

    private Element root;

    private Element leaf;

    private Font font;

    private FontMetrics metrics;

    private float width;

    private static final String FULL_TEXT = "this text to check how view breaks";

    //   0123456789012345678901234567890123
    private static final int startOffset = 5;

    private static final int endOffset = 28;

    private static final int length = endOffset - startOffset;

    private static final String LEAF_TEXT = FULL_TEXT.substring(startOffset, endOffset);

    private static final int X_AXIS = View.X_AXIS;

    private static final int Y_AXIS = View.Y_AXIS;

    private static final int BAD = View.BadBreakWeight;

    private static final int GOOD = View.GoodBreakWeight;

    private static final int EXCELLENT = View.ExcellentBreakWeight;

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
        font = view.getFont();
        metrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
    }

    /**
     * Breaks the view along X at an invalid offset
     * (not in the range of the view).
     */
    public void testBreakViewX_IncorrectOffset() {
        View part;
        // The text in view will be measured as if the view started at offset 0
        width = metrics.stringWidth(FULL_TEXT.substring(0, 1));
        part = view.breakView(X_AXIS, 0, 0.0f, width);
        assertNotSame(view, part);
        assertSame(view.getElement(), part.getElement());
        assertEquals(0, part.getStartOffset());
        assertEquals(1, part.getEndOffset());
        assertEquals(5, view.getStartOffset()); // leaf.get{Start, End}Offset
        assertEquals(28, view.getEndOffset());
        part = view.breakView(X_AXIS, 0, 0.0f, width - 0.01f);
        assertNotSame(view, part);
        assertEquals(view.getStartOffset(), part.getStartOffset());
        assertEquals(view.getEndOffset(), part.getEndOffset());
        // Measuring will start at offset 1
        width = metrics.stringWidth(FULL_TEXT.substring(1, 2));
        part = view.breakView(X_AXIS, 1, 0.0f, width);
        assertNotSame(view, part);
        assertEquals(1, part.getStartOffset());
        assertEquals(2, part.getEndOffset());
        part = view.breakView(X_AXIS, 1, 0.0f, width - 0.01f);
        assertNotSame(view, part);
        assertEquals(view.getStartOffset(), part.getStartOffset());
        assertEquals(view.getEndOffset(), part.getEndOffset());
        width = metrics.stringWidth(FULL_TEXT.substring(0, 2));
        part = view.breakView(X_AXIS, 0, 0.0f, width);
        assertEquals(0, part.getStartOffset());
        assertEquals(2, part.getEndOffset());
        width = metrics.stringWidth(FULL_TEXT.substring(1, 3));
        part = view.breakView(X_AXIS, 1, 0.0f, width - 0.01f);
        assertEquals(1, part.getStartOffset());
        assertEquals(2, part.getEndOffset());
        width = metrics.stringWidth(FULL_TEXT.substring(0, 8));
        part = view.breakView(X_AXIS, 0, 0.0f, width);
        assertEquals(0, part.getStartOffset());
        assertEquals(5, part.getEndOffset());
        width = metrics.stringWidth(FULL_TEXT.substring(1, 9));
        part = view.breakView(X_AXIS, 1, 0.0f, width - 0.01f);
        assertEquals(1, part.getStartOffset());
        assertEquals(5, part.getEndOffset());
    }

    /**
     * Breaks the view along X at a correct offset
     * (in the range of the view).
     */
    public void testBreakViewX_CorrectOffset() {
        View part;
        width = metrics.stringWidth(LEAF_TEXT.substring(0, 1));
        part = view.breakView(X_AXIS, startOffset, 0.0f, width);
        assertNotSame(view, part);
        assertSame(view.getElement(), part.getElement());
        assertEquals(5, part.getStartOffset());
        assertEquals(6, part.getEndOffset());
        assertEquals(5, view.getStartOffset()); // leaf.get{Start, End}Offset
        assertEquals(28, view.getEndOffset());
        part = view.breakView(X_AXIS, startOffset, 0.0f, width - 0.01f);
        assertEquals(5, part.getStartOffset());
        assertEquals(28, part.getEndOffset());
        // Measuring of text will start at startOffset + 1
        width = metrics.stringWidth(LEAF_TEXT.substring(1, 2));
        part = view.breakView(X_AXIS, startOffset + 1, 0.0f, width);
        assertEquals(6, part.getStartOffset());
        assertEquals(7, part.getEndOffset());
        part = view.breakView(X_AXIS, startOffset + 1, 0.0f, width - 0.01f);
        assertEquals(5, part.getStartOffset());
        assertEquals(28, part.getEndOffset());
        width = metrics.stringWidth(LEAF_TEXT.substring(0, 2));
        part = view.breakView(X_AXIS, startOffset, 0.0f, width);
        assertEquals(5, part.getStartOffset());
        assertEquals(7, part.getEndOffset());
        width = metrics.stringWidth(LEAF_TEXT.substring(1, 3));
        part = view.breakView(X_AXIS, startOffset + 1, 0.0f, width - 0.01f);
        assertEquals(6, part.getStartOffset());
        assertEquals(7, part.getEndOffset());
        width = metrics.stringWidth(LEAF_TEXT.substring(0, 8));
        part = view.breakView(X_AXIS, startOffset, 0.0f, width);
        assertEquals(5, part.getStartOffset());
        assertEquals(13, part.getEndOffset());
        width = metrics.stringWidth(LEAF_TEXT.substring(1, 9));
        part = view.breakView(X_AXIS, startOffset + 1, 0.0f, width - 0.01f);
        assertEquals(6, part.getStartOffset());
        assertEquals(13, part.getEndOffset());
    }

    /**
     * Shows the start and end offsets are updated when text is inserted
     * before the start of the part this view represents.
     */
    public void testBreakViewX_ElementChangeBefore() throws BadLocationException {
        View part;
        width = metrics.stringWidth(LEAF_TEXT.substring(0, 8));
        part = view.breakView(X_AXIS, startOffset + 1, 0.0f, width - 0.01f);
        assertEquals(6, part.getStartOffset());
        assertEquals(13, part.getEndOffset());
        doc.insertString(5, "^^^", null);
        assertEquals(9, part.getStartOffset());
        assertEquals(16, part.getEndOffset());
        DocumentEvent event = createEvent(5, 3, EventType.INSERT);
        part.insertUpdate(event, null, null);
        assertEquals(9, part.getStartOffset());
        assertEquals(16, part.getEndOffset());
    }

    /**
     * Shows the start and end offsets are not updated when text is inserted
     * in the middle of the part of the element this view represents.
     */
    public void testBreakViewX_ElementChangeMiddle() throws BadLocationException {
        View part;
        width = metrics.stringWidth(LEAF_TEXT.substring(0, 8));
        part = view.breakView(X_AXIS, startOffset + 1, 0.0f, width - 0.01f);
        assertEquals(6, part.getStartOffset());
        assertEquals(13, part.getEndOffset());
        doc.insertString(7, "^^^", null);
        assertEquals(6, part.getStartOffset());
        assertEquals(13, part.getEndOffset());
        DocumentEvent event = createEvent(5, 3, EventType.INSERT);
        part.insertUpdate(event, null, null);
        assertEquals(6, part.getStartOffset());
        assertEquals(13, part.getEndOffset());
    }

    /**
     * Breakes the view along Y.
     */
    public void testBreakViewY() {
        View part;
        final int offset = leaf.getStartOffset();
        width = metrics.getHeight();
        part = view.breakView(Y_AXIS, offset, 0.0f, width);
        assertSame(view, part);
        assertEquals(5, part.getStartOffset());
        assertEquals(28, part.getEndOffset());
        part = view.breakView(Y_AXIS, offset, 0.0f, width - 0.01f);
        assertSame(view, part);
        part = view.breakView(Y_AXIS, offset, 0.0f, width + 0.01f);
        assertSame(view, part);
        part = view.breakView(Y_AXIS, offset, 0.0f, width * 2);
        assertSame(view, part);
    }

    /**
     * Tests <code>breakView</code> uses <code>createFragment</code> to
     * produce the part.
     */
    public void testBreakViewCreate() throws Exception {
        view = new GlyphView(root) {
            @Override
            public View createFragment(int startOffset, int endOffset) {
                final Element part = ((AbstractDocument) doc).new LeafElement(null, null,
                        startOffset, endOffset);
                return new LabelView(part);
            };
        };
        final float width = metrics.stringWidth(FULL_TEXT.substring(0, 2));
        final View fragment = view.breakView(X_AXIS, 0, 0, width);
        assertTrue(fragment instanceof LabelView);
        assertFalse(view instanceof LabelView);
        assertEquals(view.getStartOffset(), fragment.getStartOffset());
        assertEquals(view.getStartOffset() + 2, fragment.getEndOffset());
        assertTrue(fragment.getElement() instanceof LeafElement);
        assertTrue(view.getElement() instanceof BranchElement);
    }

    public void testCreateFragment() {
        View fragment;
        fragment = view.createFragment(1, 3);
        assertEquals(1, fragment.getStartOffset());
        assertEquals(3, fragment.getEndOffset());
        assertSame(view.getElement(), fragment.getElement());
        assertTrue(fragment instanceof GlyphView);
        fragment = view.createFragment(5, 7);
        assertEquals(5, fragment.getStartOffset());
        assertEquals(7, fragment.getEndOffset());
        assertSame(view.getElement(), fragment.getElement());
        fragment = view.createFragment(startOffset, endOffset);
        assertNotSame(view, fragment);
        assertEquals(view.getStartOffset(), fragment.getStartOffset());
        assertEquals(view.getEndOffset(), fragment.getEndOffset());
        assertSame(view.getElement(), fragment.getElement());
        fragment = view.createFragment(startOffset + length / 4, endOffset - length / 4);
        assertEquals(startOffset + length / 4, fragment.getStartOffset());
        assertEquals(endOffset - length / 4, fragment.getEndOffset());
        assertSame(view.getElement(), fragment.getElement());
        fragment = fragment.createFragment(fragment.getStartOffset() + 2, fragment
                .getEndOffset() - 1);
        assertEquals(startOffset + length / 4 + 2, fragment.getStartOffset());
        assertEquals(endOffset - length / 4 - 1, fragment.getEndOffset());
        assertSame(view.getElement(), fragment.getElement());
    }

    public void testGetBreakWeightX() {
        width = metrics.stringWidth(LEAF_TEXT.substring(0, 1));
        assertEquals(GOOD, view.getBreakWeight(X_AXIS, 0.0f, width));
        assertEquals(BAD, view.getBreakWeight(X_AXIS, 0.0f, width - 0.01f));
        assertEquals(GOOD, view.getBreakWeight(X_AXIS, width, width));
        assertEquals(BAD, view.getBreakWeight(X_AXIS, width, width - 0.01f));
        assertEquals(GOOD, view.getBreakWeight(X_AXIS, width * 2, width));
        assertEquals(BAD, view.getBreakWeight(X_AXIS, width * 2, width - 0.01f));
        assertEquals(GOOD, view.getBreakWeight(X_AXIS, width * 3, width));
        assertEquals(BAD, view.getBreakWeight(X_AXIS, width * 3, width - 0.01f));
        width = metrics.stringWidth(LEAF_TEXT.substring(0, 2));
        assertEquals(GOOD, view.getBreakWeight(X_AXIS, 0.0f, width));
        assertEquals(GOOD, view.getBreakWeight(X_AXIS, 0.0f, width - 0.01f));
        width = metrics.stringWidth(LEAF_TEXT.substring(0, 5));
        assertEquals(EXCELLENT, view.getBreakWeight(X_AXIS, 0.0f, width));
        assertEquals(GOOD, view.getBreakWeight(X_AXIS, 0.0f, width - 0.01f));
        width = metrics.stringWidth(LEAF_TEXT.substring(0, 6));
        assertEquals(EXCELLENT, view.getBreakWeight(X_AXIS, 0.0f, width));
        assertEquals(EXCELLENT, view.getBreakWeight(X_AXIS, 0.0f, width - 0.01f));
    }

    public void testGetBreakWeightY() {
        width = metrics.getHeight();
        assertEquals(BAD, view.getBreakWeight(Y_AXIS, 0.0f, width));
        assertEquals(BAD, view.getBreakWeight(Y_AXIS, 0.0f, width - 0.01f));
        assertEquals(BAD, view.getBreakWeight(Y_AXIS, width, width));
        assertEquals(BAD, view.getBreakWeight(Y_AXIS, width, width - 0.01f));
        assertEquals(GOOD, view.getBreakWeight(Y_AXIS, 0.0f, width + 0.01f));
    }

    public void testClone() {
        view.setGlyphPainter(new EmptyPainter());
        GlyphView clone = (GlyphView) view.clone();
        assertEquals(view.getStartOffset(), clone.getStartOffset());
        assertEquals(view.getEndOffset(), clone.getEndOffset());
        assertSame(view.getElement(), clone.getElement());
        assertSame(view.getGlyphPainter(), clone.getGlyphPainter());
    }

    private DocumentEvent createEvent(final int offset, final int length, final EventType type) {
        return ((AbstractDocument) doc).new DefaultDocumentEvent(offset, length, type);
    }
}
