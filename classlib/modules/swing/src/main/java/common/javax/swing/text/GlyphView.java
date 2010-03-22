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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Shape;
import java.text.BreakIterator;
import java.text.CharacterIterator;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Position.Bias;

public class GlyphView extends View implements TabableView, Cloneable {
    public abstract static class GlyphPainter {
        public abstract float getSpan(GlyphView v,
                                      int startOffset, int endOffset,
                                      TabExpander tabExpander, float x);
        public abstract float getHeight(GlyphView v);
        public abstract float getAscent(GlyphView v);
        public abstract float getDescent(GlyphView v);

        public abstract void paint(GlyphView v, Graphics g, Shape alloc,
                                   int startOffset, int endOffset);

        public abstract Shape modelToView(GlyphView v,
                                          int offset, Bias bias,
                                          Shape alloc)
                                          throws BadLocationException;
        public abstract int viewToModel(GlyphView v,
                                        float x, float y,
                                        Shape alloc,
                                        Bias[] biasReturn);
        public abstract int getBoundedPosition(GlyphView v, int startOffset,
                                               float x, float len);

        public GlyphPainter getPainter(final GlyphView v,
                                       final int startOffset,
                                       final int endOffset) {
            return this;
        }

        public int getNextVisualPositionFrom(final GlyphView v,
                                             final int offset, final Bias bias,
                                             final Shape alloc,
                                             final int direction,
                                             final Bias[] biasReturn)
            throws BadLocationException {

            switch (direction) {
            case EAST:
                if (offset < v.getEndOffset() - 1) {
                    biasReturn[0] = Bias.Forward;
                    return offset + 1;
                }
                return -1;

            case WEST:
                if (offset > v.getStartOffset()) {
                    biasReturn[0] = Bias.Forward;
                    return offset - 1;
                }
                return -1;

            case NORTH:
            case SOUTH:
                return -1;

            default:
                assert false : "Unknown direction. Valid values are "
                               + "EAST, WEST, NORTH, SOUTH";
                return -1;
            }
        }
    }

    private GlyphPainter painter;
    private int partOffset;
    private int partLength;
    private TabExpander tabExpander;

    public GlyphView(final Element element) {
        super(element);
    }

    public void setGlyphPainter(final GlyphPainter p) {
        painter = p;
    }

    public GlyphPainter getGlyphPainter() {
        return painter;
    }

    public float getAlignment(final int axis) {
        if (axis == X_AXIS) {
            return super.getAlignment(axis);
        }

        checkPainter();
        final GlyphPainter p = getGlyphPainter();
        final float height = p.getHeight(this);

        return (height - p.getDescent(this)) / height;
    }

    public float getPreferredSpan(final int axis) {
        checkPainter();
        return axis == X_AXIS
               ? getGlyphPainter().getSpan(this,
                                           getStartOffset(), getEndOffset(),
                                           getTabExpander(), 0)
               : getGlyphPainter().getHeight(this);
    }

    public float getPartialSpan(final int startOffset, final int endOffset) {
        checkPainter();
        return getGlyphPainter().getSpan(this, startOffset, endOffset, null, 0);
    }

    public float getTabbedSpan(final float x, final TabExpander tabExpander) {
        checkPainter();
        return getGlyphPainter().getSpan(this,
                                         getStartOffset(), getEndOffset(),
                                         tabExpander, x);
    }

    public TabExpander getTabExpander() {
        return tabExpander;
    }

    public void setParent(final View parent) {
        if (parent != getParent()) {
            View p = parent;
            while (p != null && !(p instanceof TabExpander)) {
                p = p.getParent();
            }
            tabExpander = p != null ? (TabExpander)p : null;
        }
        super.setParent(parent);
    }

    public Segment getText(final int startOffset, final int endOffset) {
        final Segment text = new Segment();
        try {
            getDocument().getText(startOffset, endOffset - startOffset, text);
        } catch (BadLocationException e) {
            throw new Error(e);
        }
        return text;
    }

    public int getStartOffset() {
        int result = super.getStartOffset();
        if (isPart()) {
            result += partOffset;
        }
        return result;
    }

    public int getEndOffset() {
        return isPart() ? super.getStartOffset() + partOffset + partLength
                        : super.getEndOffset();
    }

    public void paint(final Graphics g, final Shape alloc) {
        checkPainter();
        getGlyphPainter().paint(this, g, alloc,
                                getStartOffset(), getEndOffset());
    }

    public Shape modelToView(final int offset, final Shape alloc,
                             final Bias bias) throws BadLocationException {
        checkPainter();
        return getGlyphPainter().modelToView(this, offset, bias, alloc);
    }

    public int viewToModel(final float x, final float y,
                           final Shape alloc, final Bias[] biasReturn) {
        checkPainter();
        return getGlyphPainter().viewToModel(this, x, y, alloc, biasReturn);
    }

    public int getNextVisualPositionFrom(final int offset, final Bias bias,
                                         final Shape alloc, final int direction,
                                         final Bias[] biasReturn)
        throws BadLocationException {

        return getGlyphPainter().getNextVisualPositionFrom(this, offset, bias,
                                                           alloc, direction,
                                                           biasReturn);
    }

    public int getBreakWeight(final int axis,
                              final float x, final float len) {
        if (axis == Y_AXIS) {
            return super.getBreakWeight(axis, x, len);
        }

        checkPainter();
        int breakOffset = getGlyphPainter().getBoundedPosition(this,
                                                               getStartOffset(),
                                                               x, len);
        if (breakOffset == getStartOffset()) {
            return BadBreakWeight;
        }
        final Segment text = getText(getStartOffset(), breakOffset);
        boolean hasWhiteSpace;
        char c = text.last();
        do {
            hasWhiteSpace = Character.isWhitespace(c);
            c = text.previous();
        } while (c != Segment.DONE && !hasWhiteSpace);
        return hasWhiteSpace ? ExcellentBreakWeight : GoodBreakWeight;
    }

    public View breakView(final int axis, final int startOffset,
                          final float x, final float len) {
        if (axis == Y_AXIS) {
            return super.breakView(axis, startOffset, x, len);
        }
        final BreakIterator bi = BreakIterator.getWordInstance();
        final Segment text = getText(startOffset, getEndOffset());
        bi.setText(text);

        checkPainter();
        final GlyphPainter p = getGlyphPainter();

        float width = 0;
        float fragmentWidth;
        int prev;
        int curr = bi.first() - text.offset;
        int next;
        int ws = -1;
        boolean whitespace;
        do {
            prev = curr;
            next = bi.next();
            curr = next != BreakIterator.DONE ? next - text.offset
                                              : getEndOffset() - startOffset;
            fragmentWidth = p.getSpan(this,
                                      startOffset + prev, startOffset + curr,
                                      getTabExpander(), x + width);
            whitespace = isWhitespace(getText(startOffset + prev,
                                              startOffset + curr));
            if (whitespace) {
                ws = curr;
            }
            width += fragmentWidth;
        } while (width <= len && next != BreakIterator.DONE);

        int length;
        if (ws == -1) {
            length = p.getBoundedPosition(this, startOffset, x, len)
                     - startOffset;
            if (length == 0) {
                // The view cannot be broken at this point
                return createFragment(getStartOffset(), getEndOffset());
            }
        } else {
            length = whitespace ? curr : prev;
        }

        if (startOffset == getStartOffset()
            && getStartOffset() + length == getEndOffset()) {

            return this;
        }
        return createFragment(startOffset, startOffset + length);
    }

    public View createFragment(final int startOffset, final int endOffset) {
        GlyphView result = (GlyphView)clone();
        result.partOffset = startOffset - getElement().getStartOffset();
        result.partLength = endOffset - startOffset;
        return result;
    }

    public Color getForeground() {
        final StyledDocument doc = getStyledDocument();
        if (doc != null) {
            return doc.getForeground(getAttributes());
        }

        final Component component = getComponent();
        return component != null ? component.getForeground() : null;
    }

    public Color getBackground() {
        if (!getAttributes().isDefined(StyleConstants.Background)) {
            return null;
        }

        final StyledDocument doc = getStyledDocument();
        return doc != null ? doc.getBackground(getAttributes()) : null;
    }

    public Font getFont() {
        final StyledDocument doc = getStyledDocument();
        if (doc != null) {
            return doc.getFont(getAttributes());
        }

        final Component component = getComponent();
        return component != null ? component.getFont() : null;
    }

    public boolean isUnderline() {
        return StyleConstants.isUnderline(getAttributes());
    }

    public boolean isStrikeThrough() {
        return StyleConstants.isStrikeThrough(getAttributes());
    }

    public boolean isSubscript() {
        return StyleConstants.isSubscript(getAttributes());
    }

    public boolean isSuperscript() {
        return StyleConstants.isSuperscript(getAttributes());
    }

    public void insertUpdate(final DocumentEvent event, final Shape alloc,
                             final ViewFactory factory) {
        preferenceChanged(null, true, false);
    }

    public void removeUpdate(final DocumentEvent event, final Shape alloc,
                             final ViewFactory factory) {
        preferenceChanged(null, true, false);
    }

    public void changedUpdate(final DocumentEvent event, final Shape alloc,
                              final ViewFactory factory) {
        preferenceChanged(null, true, true);
    }

    protected void checkPainter() {
        if (getGlyphPainter() == null) {
            setGlyphPainter(DefaultGlyphPainter.getDefaultPainter());
        }
    }

    protected final Object clone() {
        Object result = null;
        try {
            result = super.clone();
        } catch (CloneNotSupportedException e) {}
        return result;
    }

    private StyledDocument getStyledDocument() {
        Document doc = getDocument();
        return doc instanceof StyledDocument ? (StyledDocument)doc : null;
    }

    private boolean isPart() {
        return partOffset != 0 || partLength != 0;
    }

    private static boolean isWhitespace(final CharacterIterator text) {
        boolean result;
        char c = text.first();
        do {
            result = Character.isWhitespace(c);
            c = text.next();
        } while (result && c != CharacterIterator.DONE);
        return result;
    }
}
