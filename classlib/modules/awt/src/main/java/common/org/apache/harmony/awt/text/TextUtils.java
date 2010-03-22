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
 * @author Evgeniya G. Maenkova, Alexey A. Ivanov
 */

package org.apache.harmony.awt.text;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.Format.Field;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.SwingConstants;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DateFormatter;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabExpander;
import javax.swing.text.View;
import javax.swing.text.Position.Bias;

import org.apache.harmony.awt.ComponentInternals;
import org.apache.harmony.awt.internal.nls.Messages;


public class TextUtils {
    public TextUtils() {
    }
    static final Position.Bias backward = Position.Bias.Backward;

    static final Position.Bias forward = Position.Bias.Forward;

    /**
     * Sets new value to dot or mark of the given component caret
     * depending on <code>isMovingCaret</code> value
     */
    public static final void changeCaretPosition(
        final TextKit textKit,
        final int newPos,
        final boolean isMovingCaret) {

        TextCaret caret = textKit.getCaret();

        Point pt = caret.getMagicCaretPosition();
        if (isMovingCaret) {
            caret.moveDot(newPos, Position.Bias.Forward);
        } else {
            caret.setDot(newPos, Position.Bias.Forward);
        }
        caret.setMagicCaretPosition(pt);
    }

//  TODO remove this method as duplicate


     /**
      * Sets new value to dot or mark of the given component caret
      * depending on <code>isMovingCaret</code> value
      */
     public static final void changeCaretPosition(
         final TextKit textKit, final int newPos,
         final boolean isMovingCaret, final Position.Bias newBias) {

         TextCaret caret = textKit.getCaret();
         Point pt = caret.getMagicCaretPosition();
         if (isMovingCaret) {
             caret.moveDot(newPos, newBias);
         } else {
             caret.setDot(newPos, newBias);
         }
         //TODO in original TextAction this is after not here
         //(after getCaretMagicPosition).
         //Probably there is mistake
         caret.setMagicCaretPosition(pt);
     }


    public static final int getNextWord(final Document doc, final int pos)
        throws BadLocationException {

        BreakIterator bi = BreakIterator.getWordInstance();
        int length = doc.getLength();
        if (pos < 0 || pos >= length) {
            // awt.2F=No more words
            throwException(Messages.getString("awt.2F"), pos); //$NON-NLS-1$
        }
        String content = null;

        content = doc.getText(0, doc.getLength());
        bi.setText(content);

        int iteratorNextWord = bi.following(pos);
        while (iteratorNextWord < length
               && Character.isWhitespace(content.charAt(iteratorNextWord))) {
            iteratorNextWord = bi.following(iteratorNextWord);
        }
        if (iteratorNextWord == length) {
            // awt.2F=No more words
            throwException(Messages.getString("awt.2F"), pos); //$NON-NLS-1$
        }
        return iteratorNextWord;
    }

    public static final int getPreviousWord(final Document doc, final int pos)
        throws BadLocationException  {

        BreakIterator bi = BreakIterator.getWordInstance();
        int length = doc.getLength();
        if (pos < 0 || pos > length) {
            // awt.2F=No more words
            throwException(Messages.getString("awt.2F"), pos); //$NON-NLS-1$
        }
        String content = null;

        content = doc.getText(0, doc.getLength());
        bi.setText(content);

        int iteratorPrevWord = bi.preceding(pos);
        while (iteratorPrevWord > 0
                && ((content.charAt(iteratorPrevWord) == ' '
                    || content.charAt(iteratorPrevWord) == '\n')
                    || content.charAt(iteratorPrevWord) == '\t')) {
            iteratorPrevWord = bi.preceding(iteratorPrevWord);
        }
        return iteratorPrevWord == -1 ? 0 : iteratorPrevWord;
    }

    public static final boolean isBidirectional(final Document document) {
        if (!(document instanceof AbstractDocument)) {
            return false;
        }
        Element bidiRoot = ((AbstractDocument)document).getBidiRootElement();
        return (bidiRoot != null && bidiRoot.getElementCount() >= 2);
    }

    static final boolean isLTR(final Element element) {
        return isLTR(StyleConstants.getBidiLevel(element.getAttributes()));
    }

    public static final boolean isLTR(final int level) {
        return (level & 1) == 0;
    }

    static final boolean isRTL(final int level) {
        return (level & 1) == 1;
    }

    public static final int getTabbedTextWidth(final Segment s,
                                               final FontMetrics fm,
                                               final int x,
                                               final TabExpander t,
                                               final int pos) {
        return getTabbedTextEnd(s, fm, x, t, pos, false, null, 0) - x;
    }

    public static final int getTabbedTextOffset(final Segment s,
                                                final FontMetrics fm,
                                                final int start,
                                                final int end,
                                                final TabExpander t,
                                                final int pos) {
        return getTabbedTextOffset(s, fm, start, end, t, pos, true);
    }

    /**
     * If round equals false it needs that symbol is placed completely. If
     * round equals true it needs that more than half of the symbol is placed.
     */
    public static final int getTabbedTextOffset(final Segment s,
                                                final FontMetrics fm,
                                                final int start,
                                                final int end,
                                                final TabExpander t,
                                                final int pos,
                                                final boolean round) {
        String str = ""; //$NON-NLS-1$
        int segmentOffset = pos - s.getBeginIndex();
        boolean isTab = false;
        boolean isNullTabExpander = (t == null);
        int currentEnd = start < 0 ? 0 : start;
        int x1 = start < 0 ? start : 0;
        int currentIndex = 0;
        int prevEnd = currentEnd;
        int tabEnd = currentEnd;
        for (char c = s.first(); c != CharacterIterator.DONE; c = s.next()) {
            isTab = (c == '\t');
            if (isTab && !isNullTabExpander) {
                tabEnd = (int)t.nextTabStop(currentEnd, s.getIndex()
                        + segmentOffset);
                str = ""; //$NON-NLS-1$
            } else {
                str += (isTab) ? ' ' : c;
                isTab = false;
            }
            int tmpEnd = tabEnd + x1;
            currentEnd = isTab ?  tmpEnd: tmpEnd + fm.stringWidth(str);
            int delta = (round) ? (currentEnd - prevEnd) / 2 : 0;
            if (currentEnd > end + delta) {
                break;
            }
            currentIndex++;
            prevEnd = currentEnd;
        }
        return currentIndex;
    }

    public static final int drawTabbedText(final Segment s, final int x,
                                           final int y, final Graphics g,
            final TabExpander t, final int pos) {
        return getTabbedTextEnd(s, g.getFontMetrics(), x, t, pos, true, g, y);
    }

    public static final int drawComposedText(final TextKit textKit,
                                      final AttributedString text,
                                      final Graphics g,
                                      final int x,
                                      final int y) {
        text.addAttribute(TextAttribute.FONT, textKit.getComponent().getFont());
        final AttributedCharacterIterator iterator = text.getIterator();

        g.drawString(iterator, x, y);

        final int width = g.getFontMetrics().getStringBounds(iterator,
                iterator.getBeginIndex(), iterator.getEndIndex(),
                g).getBounds().width;
        return width + x;
    }

    public static final int getBreakLocation(final Segment s,
                                             final FontMetrics fm,
                                             final int start,
                                             final int end,
                                             final TabExpander t,
                                             final int pos) {
        int offset = s.offset;
        int index = TextUtils.getTabbedTextOffset(s, fm, start, end, t, pos,
                false);
        int fullIndex = offset +  index;

        BreakIterator bi = BreakIterator.getWordInstance();
        bi.setText(s);
        if (bi.last() <= fullIndex) {
            return bi.last() - offset;
        }
        if (bi.isBoundary(fullIndex)) {
            return Character.isWhitespace(s.array[fullIndex])
                   ? index + 1 : index;
        }
        int prev = bi.preceding(fullIndex);
        if (prev == bi.first()) {
            return index;
        }
        return prev - offset;
    }

    public static final int getRowStart(final TextKit tk, final int pos)
            throws BadLocationException {
        Dimension d = tk.getVisibleRect().getSize();
        if (d != null && (d.height == 0 || d.width == 0)) {
            return -1;
        }
        int length = tk.getDocument().getLength();
        if (pos < 0 || pos > length) {
            // awt.2A=Position not represented by view
            throwException(Messages.getString("awt.2A"), pos); //$NON-NLS-1$
        }
        int y = tk.modelToView(pos).y;
        Rectangle tmp = null;
        for (int i = pos - 1; i >= 0; i--) {
            tmp = tk.modelToView(i);
            if (tmp.y < y) {
                return i + 1;
            }
        }
        return 0;
    }
    public static final int getWordEnd(final TextKit tk, final int pos)
        throws BadLocationException {

        Document doc = tk.getDocument();
        BreakIterator bi = BreakIterator.getWordInstance();
        int length = doc.getLength();
        if (pos < 0 || pos > length) {
            // awt.2B=No word at {0}
            throwException(Messages.getString("awt.2B", pos), pos); //$NON-NLS-1$
        }
        String content = doc.getText(0, doc.getLength());
        bi.setText(content);
        return (pos < bi.last()) ? bi.following(pos) : pos;
   }

   public static final int getWordStart(final TextKit tk, final int pos)
        throws BadLocationException {
        Document doc = tk.getDocument();
        BreakIterator bi = BreakIterator.getWordInstance();
        int length = doc.getLength();
        if (pos < 0 || pos > length) {
            // awt.2B=No word at {0}
            throwException(Messages.getString("awt.2B", pos), pos); //$NON-NLS-1$
        }
        String content = null;

        content = doc.getText(0, doc.getLength());
        bi.setText(content);
        int iteratorWordStart = pos;
        if (pos < length - 1) {
            iteratorWordStart = bi.preceding(pos + 1);
        } else {
             bi.last();
             iteratorWordStart = bi.previous();
        }
        return iteratorWordStart;
   }

    /**
     * Finds row above, calculates modelToView for all the positions from this view
     * and selects the closest one.
     */
    public static final int getPositionAbove(final TextKit textKit,
                                             final int p, final int x)
            throws BadLocationException {
        int p0 = getRowStart(textKit, p);
        if (p0 <= 0) {
            return -1;
        }
        int end = p0 - 1;
        int offset = end;
        int dy = 0;
        Rectangle rect = textKit.modelToView(end);
        int lastY = rect.y;
        int dx = Math.abs(rect.x - x);
        int index = end;
        do { if (index != end) {
                rect = textKit.modelToView(index);
             }
             dy = rect.y - lastY;
             int locDiff = Math.abs(rect.x - x);
             if (locDiff <= dx && dy == 0) {
                 dx = locDiff;
                 offset = index;
             }
             index--;
             lastY = rect.y;
        } while(dx >= 1 && dy == 0 && index >= 0);
        return offset;
    }

    /**
     * Finds row below, calculates modelToView for all the positions from this view
     * and selects the closest one.
     */
    public static final int getPositionBelow(final TextKit textKit,
                                             final int p, final int x)
            throws BadLocationException {
        int p0 = getRowEnd(textKit, p);
        if (p0 == -1) {
            return -1;
        }
        int length = textKit.getDocument().getLength();
        if (p0 == length) {
            return p;
        }
        int start = p0 + 1;
        int offset = p0 + 1;
        int dy = 0;
        Rectangle rect = textKit.modelToView(start);
        int lastY = rect.y;
        int dx = Math.abs(rect.x - x);
        int index = start;
        do { rect = textKit.modelToView(index);
             dy = rect.y - lastY;
             int locDiff = Math.abs(rect.x - x);
             if (locDiff < dx && dy == 0) {
                 dx = locDiff;
                 offset = index;
             }
             index++;
             lastY = rect.y;
        } while(index <= length && dy == 0 && dx >= 1);

        return offset;
    }

    public static final TextKit getTextKit(final Component c) {
        return ComponentInternals.getComponentInternals().getTextKit(c);
    }

    public static final TextFieldKit getTextFieldKit(final Component c) {
        return ComponentInternals.getComponentInternals().getTextFieldKit(c);
    }

    public static final int getRowEnd(final TextKit tk, final int pos)
            throws BadLocationException {
        Dimension d = tk.getVisibleRect().getSize();
        if (d != null && (d.height == 0 || d.width == 0)) {
            return -1;
        }
        int length = tk.getDocument().getLength();
        if (pos < 0 || pos > length) {
            // awt.2A=Position not represented by view
            throwException(Messages.getString("awt.2A"), pos); //$NON-NLS-1$
        }

        int y = tk.modelToView(pos).y;
        Rectangle r = null;
        for (int i = pos + 1; i <= length; i++) {
            r = tk.modelToView(i);
            if (r.y > y) {
                return i - 1;
            }
        }
        return length;
    }

    //TODO Probably here is a bug:
    //Why NORTH?
    //Where bias?
    public static final void setCurrentPositionAsMagic(final TextKit textKit) {
        TextCaret caret = textKit.getCaret();
        int newPos = caret.getDot();
        try {
            Point pt = textKit.modelToView(newPos,
                                           caret.getDotBias())
                                           .getLocation();
            caret.setMagicCaretPosition(newPos, SwingConstants.NORTH, pt);
        } catch (BadLocationException e) {
        }
    }

    /**
     * If the component document is an instance of AbstractDocument then its
     * {@link AbstractDocument#getParagraphElement(int)} method is called.
     * Otherwise {@link Document#getDefaultRootElement()} is used to search
     * paragraph element.
     */
    public static final Element getParagraphElement(final Document doc,
                                                    final int p) {
        if (doc instanceof AbstractDocument) {
            AbstractDocument abstrDoc = (AbstractDocument) doc;
            abstrDoc.readLock();
            Element elem = null;
            int length = 0;
            boolean incorrectPosition = false;
            try {
                length = doc.getLength();
                incorrectPosition = (p < 0 || p > length)
                        && (doc instanceof PlainDocument);
                if (!incorrectPosition) {
                    elem = abstrDoc.getParagraphElement(p);
                }
            } finally {
                abstrDoc.readUnlock();
            }
            return elem;
        }

        Element root = doc.getDefaultRootElement();
        int index = root.getElementIndex(p);
        return index == -1 ? null : root.getElement(index);
    }

    /*
     * Returns end x-coordinate of tabbed text. Uses by drawTabbedText and
     * getTabbedTextWidth.
     */
    private static int getTabbedTextEnd(final Segment s, final FontMetrics fm,
                                        final int x, final TabExpander t,
                                        final int pos, final boolean needDraw,
                                        final Graphics g, final int y) {
        int x1 = x < 0 ? x : 0;
        int res = x < 0 ? 0 : x;
        String buffer = s.toString();
        int tabIndex = buffer.indexOf("\t"); //$NON-NLS-1$
        int currentIndex = pos - s.getBeginIndex() + s.offset;
        if (t == null) {
            String buf =  buffer.replaceAll("\t", " "); //$NON-NLS-1$ //$NON-NLS-2$
            drawString(buf, needDraw, g, x, y);
            return fm.stringWidth(buf) + x;
        }
        String substr = null;
        int lastTabIndex = -1;
        while (tabIndex >= 0) {
            substr = buffer.substring(lastTabIndex + 1, tabIndex);
            drawString(substr, needDraw, g, res + x1, y);
            res = (int)t.nextTabStop(res + fm.stringWidth(substr),
                                     tabIndex + currentIndex);
            lastTabIndex = tabIndex;
            tabIndex = buffer.indexOf("\t", tabIndex + 1); //$NON-NLS-1$
        }
        int tmp = res + x1;
        substr = buffer.substring(lastTabIndex + 1, buffer.length());
        drawString(substr, needDraw, g, tmp, y);
        return (tabIndex >= 0) ? tmp : tmp + fm.stringWidth(substr);
    }

    private static void drawString(final String text, final boolean needDraw,
                                   final Graphics g,
                                   final int x, final int y) {
        if (needDraw) {
            g.drawString(text, x, y);
        }
    }

    public static final void readLock(final Document document) {
        if (document instanceof AbstractDocument) {
            ((AbstractDocument)document).readLock();
        }
    }

    public static final void readUnlock(final Document document) {
        if (document instanceof AbstractDocument) {
            ((AbstractDocument)document).readUnlock();
        }
    }

    private static void throwException(final String s, final int i)
        throws BadLocationException {
        throw new BadLocationException(s, i);
    }

    //---NextVisualPosition---------------------------------------------
//  for a stub (getNexVisualPositionFrom)
    public static final int getNextVisualPositionFrom(
                                               final TextKit textKit,
                                               final View v, final int pos,
                                               final Position.Bias bias,
                                               final Shape shape,
                                               final int direction,
                                               final Position.Bias[] biasRet)
            throws BadLocationException {
        int length = v.getDocument().getLength();
        if (pos < 0 || pos > length) {
            // awt.2C=Invalid position: {0}
            throwException(Messages.getString("awt.2C", pos), pos); //$NON-NLS-1$
        }
        biasRet[0] = Position.Bias.Forward;
        if (direction == SwingConstants.WEST
                || direction == SwingConstants.EAST) {
            return getNextVisualPosition(v, pos, bias, direction, biasRet);
        }
        Point pt = textKit.getCaret().getMagicCaretPosition();
        if (direction == SwingConstants.NORTH) {
            return TextUtils.getPositionAbove(textKit, pos,
                                              pt != null ? pt.x
                    : v.modelToView(pos, shape, bias).getBounds().x);
        } else if (direction == SwingConstants.SOUTH) {
            return TextUtils.getPositionBelow(textKit, pos, pt != null ? pt.x
                    : v.modelToView(pos, shape, bias).getBounds().x);
        }

        // awt.2D=Invalid direction
        throw new IllegalArgumentException(Messages.getString("awt.2D")); //$NON-NLS-1$
    }


    private static Element getElementByPosition(final Element rootElement,
                                                final int pos) {
        int index = rootElement.getElementIndex(pos);
        return rootElement.getElement(index);
    }

    private static int getNextVisualPosition(final View v, final int pos,
                                             final Position.Bias b0,
            final int direction, final Position.Bias[] biasRet) {
        boolean toWest = (direction == SwingConstants.WEST);
        Document document = v.getDocument();
        int length = document.getLength();
        if (!isBidirectional(document)) {
            return getTrivialVisualPosition(toWest, pos, b0, length, biasRet,
                                            true);
        }
        Element bidiRoot = ((AbstractDocument)document).getBidiRootElement();
        Element elem = getElementByPosition(bidiRoot, pos);
        boolean isLTR = isLTR(elem);
        int start = elem.getStartOffset();
        int end = elem.getEndOffset() - 1;

        int posInNeighboringElement = toWest ? Math.max(start - 1, 0)
                                             : Math.min(end + 1, length);
        Element neighboringElement = getElementByPosition(bidiRoot,
                                                       posInNeighboringElement);

        Element paragraph = getElementByPosition(document
                                                 .getDefaultRootElement(), pos);
        int startParagraph = paragraph.getStartOffset();
        int endParagraph = paragraph.getEndOffset() - 1;

        boolean nextIsLTR = isLTR(getElementByPosition(bidiRoot,
                                                       Math.min(endParagraph
                                                                + 1,
                                                                length)));

        int result = checkBoundaryCondition(pos, b0, biasRet,
                                            neighboringElement,
                                            isLTR, toWest, start,
                                            length, startParagraph,
                                            endParagraph, nextIsLTR);

        return result >= 0 ? result
                : getBidiVisualPosition(start, end, neighboringElement, pos, b0,
                                     biasRet, length, toWest, isLTR);
    }

    private static int checkBoundaryCondition(final int pos,
                                                    final Position.Bias b0,
                                                    final Position.Bias[] biasRet,
                                                    final Element neighboringElement,
                                                    final boolean isLTR,
                                                    final boolean toWest,
                                                    final int start,
                                                    final int length,
                                                    final int startParagraph,
                                                    final int endParagraph,
                                                    final boolean nextIsLTR) {

    if (toWest && isLTR) {
        if (pos == startParagraph) {
             return Math.max(0, pos - 1);
        } else if (neighboringElement.getStartOffset() == startParagraph
            && pos == start  && b0 == backward) {
             return Math.max(0, startParagraph - 1);
        }
    } else if (!toWest) {
        if (b0 == forward && pos == length) {
            biasRet[0] = b0;
            return pos;
        }
        if (b0 == forward && pos == endParagraph) {
                if (nextIsLTR) {
                    return pos + 1;
                }
                biasRet[0] = backward;
                return neighboringElement.getEndOffset();
            }
        }
        return -1;
    }

    static final int getBidiVisualPosition(final int start,
                                           final int end,
                                           final Element neighbouringElement,
                                           final int pos,
                                           final Position.Bias b0,
                                           final Position.Bias[] biasRet,
                                           final int length,
                                           final boolean toWest,
                                           final boolean isLTR) {
        boolean direction = toWest ^ isLTR;
        if (pos == end && direction && b0 == forward) {
            biasRet[0] = backward;
            return pos + 1;
        } else if (pos == start + 1 && pos <= end && !direction) {
            return pos - 1;
        } else if (pos == start) {
            if (direction) {
                return (b0 == forward) ? pos + 1 : pos - 1;
            }
            biasRet[0] = b0;
            return neighbouringElement.getStartOffset();
        } else {
            return getTrivialVisualPosition(toWest, pos, b0, length, biasRet, isLTR);
        }
    }

    private static int getTrivialVisualPosition(final boolean toWest,
                                                final int pos,
                                                final Position.Bias bias,
                                                final int docLength,
                                                final Position.Bias[] biasRet,
                                                final boolean isLTR) {
        boolean condition = (toWest && isLTR) || (!toWest && !isLTR);
        return condition ? Math.max(pos - 1, 0) : Math.min(pos + 1, docLength);
    }

    //-------TextField Methods
    public static final Shape getFieldViewAllocation(final View v,
                                              final TextFieldKit tfk,
                                              final Shape shape,
                                              final ComponentOrientation
                                              orientation) {
        if (tfk == null || shape == null) {
            return null;
        }

        Rectangle bounds = shape.getBounds();
        int prefWidth = (int)v.getPreferredSpan(View.X_AXIS);
        int height = (int)v.getPreferredSpan(View.Y_AXIS);
        int diff = bounds.width - prefWidth;
        int alignment = tfk.getHorizontalAlignment();
        boolean toLeft = isToLeft(orientation, alignment);
        int offset = 0;
        if (alignment == SwingConstants.CENTER) {
            offset = diff / 2;
        } else {
            offset = toLeft ? 0 : diff;
        }
        int extent = bounds.width - 1;
        int max = Math.max(extent, prefWidth);
        BoundedRangeModel brm = tfk.getHorizontalVisibility();
        int value = Math.min(brm.getValue(), max - extent);
        brm.setRangeProperties(value, extent, brm.getMinimum(), max, false);

        int x = (diff < 0 ? -value + tfk.getInsets().left : bounds.x + offset);
        int y = (bounds.height - height) / 2 + bounds.y;
        if (!toLeft) {
            x--;
        }
        return new Rectangle(x, y, prefWidth + 1, height);
    }

    private static boolean isToLeft(final ComponentOrientation orientation,
                                    final int alignment) {
        boolean isRTL = !orientation.isLeftToRight();
        return isRTL && alignment == SwingConstants.TRAILING
               || !isRTL && alignment == SwingConstants.LEADING
               || alignment == SwingConstants.LEFT;
    }

    //-------Highlight painting

    public static Shape paintLayer(final Graphics g, final int p0, final int p1,
                            final Shape shape, final Color color,
                            final View view, final boolean fill) {
        if (shape == null) {
            return null;
        }
        Shape result = null;
        try {
            result = view.modelToView(Math.min(p0, p1),
                                                   Position.Bias.Forward,
                                                   Math.max(p0, p1),
                                                   Position.Bias.Backward,
                                                   shape);
            Rectangle bounds = result.getBounds();
            g.setColor(color);
            if (fill) {
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            } else {
                g.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
            }
        } catch (final BadLocationException e) {
        }
        return result;
    }

    //------IM support
    public static ComposedTextParams getComposedTextParams(final TextKit tk) {
        Document doc = tk.getDocument();
        Object currentProperty = tk.getDocument()
           .getProperty(PropertyNames.COMPOSED_TEXT_PROPERTY);
        if (!(currentProperty instanceof ComposedTextParams)) {
           ComposedTextParams result = new ComposedTextParams(doc);
           int caretPosition = tk.getCaret().getDot();
           result.setComposedTextStart(caretPosition);
           result.setLastCommittedTextStart(caretPosition);
           return result;
        }
        return (ComposedTextParams)currentProperty;
    }
    public static void processIMEvent(final InputMethodListener listener,
                                 final InputMethodEvent e) {
        if (e.getID() == InputMethodEvent.CARET_POSITION_CHANGED) {
            listener.caretPositionChanged(e);
        }
        if (e.getID() == InputMethodEvent.INPUT_METHOD_TEXT_CHANGED) {
            listener.inputMethodTextChanged(e);
        }
    }

    //----highlighter
    public static final Rectangle getBoundsByOffsets(final TextKit tk,
                                         final int p0, final int p1) {
        Rectangle r0 = null;
        Rectangle r1 = null;
        Rectangle rect = new Rectangle();
        if (tk == null) {
            return null;
        }
        try {
            r0 = tk.modelToView(p0, Position.Bias.Forward);
            r1 = tk.modelToView(p1, Position.Bias.Forward);
        } catch (final BadLocationException e) {
        }
        if (r0 == null || r1 == null) {
            return null;
        }
        if (r0.y == r1.y) {
            rect.x = Math.min(r0.x, r1.x);
            rect.y = r0.y;
            rect.width = Math.max(r0.x, r1.x) - rect.x + 1;
            rect.height = Math.max(r0.height, r1.height);
            return rect;
        }
        Rectangle visibleRect = tk.getVisibleRect();
        rect.x = visibleRect.x;
        rect.y = Math.min(r0.y, r1.y);
        rect.width = visibleRect.width;
        rect.height = Math.max(r0.y, r1.y) - rect.y
                      + Math.max(r0.height, r1.height);
        return rect;
    }

    public static final void setNativeCaretPosition(final Rectangle rect,
                                                    final Component comp) {
        ComponentInternals.getComponentInternals()
             .setCaretPos(comp, rect.x, rect.y);
    }

    //-----clipboard operations (Perhaps, that's temporary solution.
    public static final void copy(final TextKit textKit) {
        exportToClipboard(textKit, getSystemClipboard(), ActionNames.COPY);
    }

    public static final void cut(final TextKit textKit) {
        exportToClipboard(textKit, getSystemClipboard(), ActionNames.MOVE);
    }

    public static final void paste(final TextKit textKit) {
        TextUtils.importData(textKit, getSystemClipboard().getContents(null));
    }

    public static final void exportToClipboard(final TextKit textKit,
                                               final Clipboard clipboard,
                                               final int action) {
        if (textKit == null) {
            return;
        }
        int realAction = (action & (getSourceActions(textKit)));
        Transferable transferable = TextUtils.createTransferable(textKit);
        if (realAction > 0 && transferable != null) {
            clipboard.setContents(transferable, null);
        }
        exportDone(textKit, transferable, realAction);
    }

    public static final Transferable createTransferable(final TextKit textKit) {
        String text = textKit.getSelectedText();
        return text != null ? new StringSelection(text) : null;
    }

    public static final void exportDone(final TextKit textKit,
                                        final Transferable transferable,
                                        final int action) {
       if (textKit != null  && (action & ActionNames.MOVE) > 0) {
           textKit.replaceSelectedText(""); //$NON-NLS-1$
       }
    }

    public static final boolean importData(final TextKit textKit,
                                        final Transferable t) {

        if (t == null) {
            return false;
        }
        DataFlavor[] flavors = t.getTransferDataFlavors();
        DataFlavor flavor = null;
        for (DataFlavor element : flavors) {
            flavor = element;
            if (String.class.isAssignableFrom(flavor.getRepresentationClass())) {
                break;
            }
            flavor = null;
        }
        if (flavor != null) {
            try {
                String text = (String) t.getTransferData(flavor);
                textKit.replaceSelectedText(text);
                return true;
            } catch (UnsupportedFlavorException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    public static final Clipboard getSystemClipboard() {
        return Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    public static int getSourceActions(final TextKit textKit) {
        if (textKit != null
            && !"javax.swing.JPasswordField".equals(textKit.getClass())) { //$NON-NLS-1$
            return (textKit.isEditable()) ? ActionNames.COPY_OR_MOVE
                    : ActionNames.COPY;

        }
        return ActionNames.NONE;
    }

    public static Rectangle scrollRectToVisible(Rectangle viewRect, Rectangle r) {
        Rectangle retVal = (Rectangle)viewRect.clone();
        retVal.x = - retVal.x;
        retVal.y = - retVal.y;

        int dx;
        int dy;

        if (r.x > 0) {
            if (r.x + r.width > viewRect.width) {
                int dx2 = r.x + r.width - viewRect.width;
                dx = Math.min(r.x, dx2);
            } else {
                dx = 0;
            }
        } else if (r.x < 0) {
            if (r.x + r.width < viewRect.width) {
                int dx2 = r.x + r.width - viewRect.width;
                dx = Math.max(r.x, dx2);
            } else {
                dx = 0;
            }
        } else {
            dx = 0;
        }

        if (r.y > 0) {
            if (r.y + r.height > viewRect.height) {
                int dy2 = r.y + r.height - viewRect.height;
                dy = Math.min(r.y, dy2);
            } else {
                dy = 0;
            }
        } else if (r.y < 0) {
            if (r.y + r.height < viewRect.height) {
                int dy2 = r.y + r.height - viewRect.height;
                dy = Math.max(r.y, dy2);
            } else {
                dy = 0;
            }
        } else {
            dy = 0;
        }

        if (dx != 0 || dy != 0) {
            int x = retVal.x + dx;
            int y = retVal.y + dy;

            retVal.x = x;
            retVal.y = y;
        }

        return retVal;
    }

    public static Rectangle getEditorRect(final JComponent component) {
        if (component == null) {
            return null;
        }
        Insets insets = component.getInsets();
        int left = 0;
        int top = 0;
        if (insets != null) {
            left = insets.left;
            top = insets.top;
        }
        Dimension r = component.getSize();
        return r.width == 0 || r.height == 0 ? null
               : new Rectangle(left, top, r.width - getHrzInsets(insets),
                               r.height - getVrtInsets(insets));
    }

    public static int getHrzInsets(final Insets insets) {
        return (insets != null) ? insets.left + insets.right : 0;
    }

    public static int getVrtInsets(final Insets insets) {
        return (insets != null) ? insets.top + insets.bottom : 0;
    }

    public static void setCharacterAttributes(final AttributeSet attr,
                                              final boolean replace,
                                              final JEditorPane editorPane,
                                              final StyledDocument doc,
                                              final MutableAttributeSet
                                                  inputAttrs) {

        final int selectionStart = editorPane.getSelectionStart();
        final int selectionEnd = editorPane.getSelectionEnd();
        doc.setCharacterAttributes(selectionStart,
                                   selectionEnd - selectionStart,
                                   attr, replace);
        if (selectionStart == selectionEnd) {
            if (replace) {
                inputAttrs.removeAttributes(inputAttrs.getAttributeNames());
            }
            inputAttrs.addAttributes(attr);
        }
    }

    public static void setParagraphAttributes(final AttributeSet attr,
                                              final boolean replace,
                                              final JEditorPane editorPane,
                                              final StyledDocument doc) {
        final int selectStart = editorPane.getSelectionStart();
        int intervalLength = Math.max (editorPane.getSelectionEnd()
                                       - selectStart, 1);
        doc.setParagraphAttributes(selectStart, intervalLength, attr, replace);
    }

    public static int getCalendarField(final JFormattedTextField textField) {
        DateFormatter formatter = (DateFormatter)textField.getFormatter();
        Field[] fields = formatter.getFields(textField.getCaretPosition());

        for (int i = textField.getCaretPosition(); fields.length == 0 || i < 0; i--) {
            fields = formatter.getFields(i);
        }
        if (fields.length == 0) {
            int length = textField.getText().length();
            for (int i = textField.getCaretPosition(); fields.length == 0 || i > length; i++) {
                fields = formatter.getFields(i);
            }
        }
        return ((DateFormat.Field)fields[0]).getCalendarField();
    }

    public static void selectCalendarField(final JFormattedTextField textField,
                                           final int calendarField) {
        boolean selecting = false;
        int length = textField.getText().length();
        for (int i = 0; i <= length; i++) {
            if (!selecting) {
                textField.setCaretPosition(i);
            } else {
                textField.moveCaretPosition(i);
            }
            if(getCalendarField(textField) == calendarField) {
                selecting = true;
            } else {
                if (selecting) {
                    textField.moveCaretPosition(i - 1);
                    return;
                }
            }
        }
    }

    public static Object getNextValue(final Date value,
                                      final int calendarField,
                                      final Comparable<Date> end) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(value);
        calendar.add(calendarField, 1);
        Date result = calendar.getTime();
        return (end == null)? result
                : (end.compareTo(result) < 0) ? null : result;
    }

    public static Object getPreviousValue(final Date value,
                                          final int calendarField,
                                          final Comparable<Date> start) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(value);
        calendar.add(calendarField, -1);
        Date result = calendar.getTime();
        return (start == null)? result
                : (start.compareTo(result) > 0) ? null : result;
    }


    /**
     * Transforms position at the document model coordinate space to the
     * coordinate space of the corresponding icon or component view.
     */
    public static Shape modelToIconOrComponentView(final View view, final int pos,
                                    final Shape shape, final Bias bias)
        throws BadLocationException {

        TextUtils.isPositionValid(view, pos);

        final Rectangle bounds = shape.getBounds();
        final int x = (pos == view.getStartOffset() ? bounds.x
                                                    : bounds.x + bounds.width);

        return new Rectangle(x, bounds.y, 0, bounds.height);
    }

    /**
     * Throws BadLocationException if the position does not represent a
     * valid location in the associated document element.
     */
    public static void isPositionValid(final View view, final int pos)
        throws BadLocationException {

        if (pos < view.getStartOffset() || pos > view.getEndOffset()) {
            // awt.2E={0} not in range {1},{2}
            throw new BadLocationException(Messages.getString("awt.2E", //$NON-NLS-1$
                    new Object[] { pos, view.getStartOffset(),
                            view.getEndOffset() }), pos);
        }
    }
}
