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
import java.awt.Shape;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Position.Bias;

import org.apache.harmony.awt.text.TextUtils;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class ParagraphView extends FlowView implements TabExpander {
    private static final int DEFAULT_TAB = 72;
    private static final int MIN_TEXT_CHUNK = 10;
    private static final char[] TABS = new char[] {'\t'};
    private static final char[] TABS_DECIMAL = new char[] {'\t', '.'};

    private class Row extends BoxView {
        private int lineSpace;

        public Row(final Element element) {
            super(element, X_AXIS);
        }

        public int getStartOffset() {
            if (getViewCount() > 0) {
                return getView(0).getStartOffset();
            }
            return super.getStartOffset();
        }

        public int getEndOffset() {
            if (getViewCount() > 0) {
                return getView(getViewCount() - 1).getEndOffset();
            }
            return super.getEndOffset();
        }

        public AttributeSet getAttributes() {
            final View parent = getParent();
            return parent != null ? parent.getAttributes() : null;
        }
        
        protected void loadChildren(final ViewFactory factory) {
        }

        protected SizeRequirements
            calculateMajorAxisRequirements(final int axis,
                                           final SizeRequirements r) {

            SizeRequirements result =
                super.calculateMajorAxisRequirements(axis, r);
            result.alignment = getAlignByJustification();
            return result;
        }

        protected SizeRequirements
            calculateMinorAxisRequirements(final int axis,
                                           final SizeRequirements r) {

            SizeRequirements result = baselineRequirements(axis, r);
            lineSpace = (int)(result.preferred * lineSpacing);
            return result;
        }

        protected void layoutMinorAxis(final int targetSpan, final int axis,
                                       final int[] offsets, final int[] spans) {
            baselineLayout(targetSpan, axis, offsets, spans);
        }

        protected short getLeftInset() {
            if (ParagraphView.this.isAllocationValid()
                && ParagraphView.this.getViewCount() > 0
                && this == ParagraphView.this.getView(0)) {

                return (short)(super.getLeftInset() + firstLineIndent);
            }
            return super.getLeftInset();
        }

        protected short getBottomInset() {
            return (short)(super.getBottomInset() + lineSpace);
        }

        private float getAlignByJustification() {
            switch (justification) {
            case StyleConstants.ALIGN_LEFT:
                return ALIGN_LEFT;
            case StyleConstants.ALIGN_CENTER:
                return ALIGN_CENTER;
            case StyleConstants.ALIGN_RIGHT:
                return ALIGN_RIGHT;
            default:
                return ALIGN_CENTER;
            }
        }
    }

    protected int firstLineIndent;
    private int justification;
    private float lineSpacing;

    private int tabBase;

    public ParagraphView(final Element element) {
        super(element, Y_AXIS);
        setPropertiesFromAttributes();
    }

    public int getFlowStart(final int rowIndex) {
        int result = super.getFlowStart(rowIndex);
        if (rowIndex == 0) {
            result += firstLineIndent;
        }
        View child = getView(rowIndex);
        if (child instanceof CompositeView) {
            result += ((CompositeView)child).getLeftInset();
        }
        return result;
    }

    public int getFlowSpan(final int rowIndex) {
        int result = super.getFlowSpan(rowIndex);
        if (rowIndex == 0) {
            result -= firstLineIndent;
        }
        View child = getView(rowIndex);
        if (child instanceof CompositeView) {
            CompositeView cv = (CompositeView)child;
            result -= cv.getLeftInset();
            result -= cv.getRightInset();
        }
        return result;
    }

    public float getAlignment(final int axis) {
        if (axis == Y_AXIS && getViewCount() > 0) {
            return (getSpan(Y_AXIS, 0) / 2) / getPreferredSpan(Y_AXIS);
        }
        return super.getAlignment(axis);
    }

    public void paint(final Graphics g,
                      final Shape a) {
        tabBase = a.getBounds().x + getLeftInset();
        super.paint(g, a);
    }

    public float nextTabStop(final float x,
                             final int tabOffset) {
        final float base = getTabBase();
        final float basedX = x > base ? x - base : 0;
        final TabSet tabSet = getTabSet();
        if (tabSet == null) {
            return base + ((int)(basedX / DEFAULT_TAB) + 1) * DEFAULT_TAB;
        }

        final TabStop tabStop = tabSet.getTabAfter(basedX);
        if (tabStop == null) {
            return base + ((int)(basedX / DEFAULT_TAB) + 1) * DEFAULT_TAB;
        }

        final int align = tabStop.getAlignment();
        if (align == TabStop.ALIGN_LEFT || align == TabStop.ALIGN_BAR) {
            return base + tabStop.getPosition();
        }

        int nextTab =
            findOffsetToCharactersInString(align == TabStop.ALIGN_DECIMAL
                                           ? TABS_DECIMAL : TABS,
                                           tabOffset + 1);
        if (nextTab == -1) {
            nextTab = getEndOffset();
        }

        float partSpan = getPartialSize(tabOffset + 1, nextTab);
        if (align == TabStop.ALIGN_CENTER) {
            return base + tabStop.getPosition() - partSpan / 2;
        }
        return base + tabStop.getPosition() - partSpan;
    }

    public View breakView(final int axis,
                          final float len,
                          final Shape a) {
        return this;
    }

    public int getBreakWeight(final int axis,
                              final float len) {
        return BadBreakWeight;
    }

    public void changedUpdate(final DocumentEvent changes,
                              final Shape a,
                              final ViewFactory f) {
        preferenceChanged(null, true, true);
        setPropertiesFromAttributes();
        super.changedUpdate(changes, a, f);
    }

    protected View createRow() {
        return new Row(getElement());
    }

    protected void setJustification(final int j) {
        justification = j;
    }

    protected void setLineSpacing(final float ls) {
        lineSpacing = ls;
    }

    protected void setFirstLineIndent(final float fi) {
        firstLineIndent = (int)fi;
    }

    protected void setPropertiesFromAttributes() {
        final AttributeSet attrs = getAttributes();
        setJustification(StyleConstants.getAlignment(attrs));
        setFirstLineIndent(StyleConstants.getFirstLineIndent(attrs));
        setLineSpacing(StyleConstants.getLineSpacing(attrs));
        setParagraphInsets(attrs);
    }

    protected int getLayoutViewCount() {
        return layoutPool.getViewCount();
    }

    protected View getLayoutView(final int index) {
        return layoutPool.getView(index);
    }

    /**
     * This method does nothing and is not supposed to be called.
     * The functionality described for this method in the API Specification
     * is equivalent to that of
     * @link FlowView.FlowStrategy#adjustRow(FlowView, int, int, int)
     */
    protected void adjustRow(final Row r, final int desiredSpan, final int x) {
    }

    protected int getClosestPositionTo(final int pos,
                                       final Bias b,
                                       final Shape a,
                                       final int direction,
                                       final Bias[] biasRet,
                                       final int rowIndex,
                                       final int x)
                                throws BadLocationException {
        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
    }

    protected float getPartialSize(final int startOffset,
                                   final int endOffset) {
        float result = 0;
        int index = layoutPool.getViewIndex(startOffset, Bias.Forward);
        if (index == -1) {
            return 0;
        }

        View child;
        int childStart;
        int childEnd;
        do {
            child = getLayoutView(index);
            childStart = child.getStartOffset();
            childEnd = child.getEndOffset();
            if (startOffset <= childStart && childEnd <= endOffset) {
                result += child.getPreferredSpan(X_AXIS);
            } else if (!(child instanceof TabableView)) {
                return 0;
            } else {
                TabableView tv = (TabableView)child;
                result +=
                    tv.getPartialSpan(childStart > startOffset ? childStart
                                                               : startOffset,
                                            childEnd < endOffset ? childEnd
                                                                 : endOffset);
            }
        } while (childEnd < endOffset && ++index < getLayoutViewCount());

        return result;
    }

    protected int findOffsetToCharactersInString(final char[] string,
                                                 final int start) {
        final Segment text = new Segment();
        text.setPartialReturn(true);
        int offset = start;
        final int limit = getEndOffset();
        final Document doc = getDocument();
        while (offset < limit) {
            try {
                doc.getText(offset, Math.min(MIN_TEXT_CHUNK,
                                             limit - offset), text);
            } catch (BadLocationException e) {
                e.printStackTrace();
                return -1;
            }

            char c = text.first();
            while (c != Segment.DONE) {
                for (int i = 0; i < string.length; i++) {
                    if (c == string[i]) {
                        return offset + text.getIndex() - text.getBeginIndex();
                    }
                }
                c = text.next();
            }
            offset += text.count;
        }
        return -1;
    }

    protected int getNextNorthSouthVisualPositionFrom(final int pos,
                                                      final Bias b,
                                                      final Shape a,
                                                      final int direction,
                                                      final Bias[] biasRet)
                                               throws BadLocationException {
        if (true) {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
        return TextUtils.getNextVisualPositionFrom(getTextKit(), this,
                                                   pos, b, a,
                                                   direction, biasRet);
    }

    protected boolean flipEastAndWestAtEnds(final int position,
                                            final Bias bias) {
        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
    }

    protected TabSet getTabSet() {
        return StyleConstants.getTabSet(getAttributes());
    }

    protected float getTabBase() {
        return tabBase;
    }
}
