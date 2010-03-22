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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.text;

import java.awt.FontMetrics;
import java.awt.Graphics;

import org.apache.harmony.awt.text.TextUtils;

public class Utilities {
    public static final int drawTabbedText(final Segment s, final int x,
                                           final int y, final Graphics g,
            final TabExpander t, final int pos) {
        return TextUtils.drawTabbedText(s, x, y, g, t, pos);
    }

    /**
     * Uses BreakIterator.
     */
    public static final int getBreakLocation(final Segment s,
                                             final FontMetrics fm,
                                             final int start,
                                             final int end,
                                             final TabExpander t,
                                             final int pos) {
        return TextUtils.getBreakLocation(s, fm, start, end, t, pos);
    }

    public static final int getNextWord(final JTextComponent c, final int pos)
            throws BadLocationException {
        return TextUtils.getNextWord(c.getDocument(), pos);
    }

    /**
     * If the components document is instanceof AbstractDocument uses by its
     * method(getParagraphElement). Otherwise, uses Document.getDefaultRoot for
     * search.
     */
    public static final Element getParagraphElement(final JTextComponent c,
                                                    final int p) {
        return TextUtils.getParagraphElement(c.getDocument(), p);
    }

    /**
     * Finds row above, calculates modelToView for all position from this view
     * and selects the best (the closest).
     */
    public static final int getPositionAbove(final JTextComponent c,
                                             final int p, final int x)
            throws BadLocationException {
        return TextUtils.getPositionAbove(TextUtils.getTextKit(c), p, x);
    }

    /**
     * Finds row below, calculates modelToView for all position from this view
     * and selects the best (the closest).
     */
    public static final int getPositionBelow(final JTextComponent c,
                                             final int p, final int x)
            throws BadLocationException {
        return TextUtils.getPositionBelow(TextUtils.getTextKit(c), p, x);
    }

    public static final int getPreviousWord(final JTextComponent c,
                                            final int pos)
            throws BadLocationException {
        return TextUtils.getPreviousWord(c.getDocument(), pos);
    }

    public static final int getRowEnd(final JTextComponent c, final int pos)
            throws BadLocationException {
        return TextUtils.getRowEnd(TextUtils.getTextKit(c), pos);
    }

    public static final int getRowStart(final JTextComponent c, final int pos)
            throws BadLocationException {
        return TextUtils.getRowStart(TextUtils.getTextKit(c), pos);
    }



    public static final int getTabbedTextOffset(final Segment s,
                                                final FontMetrics fm,
                                                final int start,
                                                final int end,
                                                final TabExpander t,
                                                final int pos) {
        return TextUtils.getTabbedTextOffset(s, fm, start, end, t, pos, true);
    }

    /**
     * If round equals false, it needs that symbol is placed completely. If
     * round equals true, it needs that more than half of the symbol is placed.
     */
    public static final int getTabbedTextOffset(final Segment s,
                                                final FontMetrics fm,
                                                final int start,
                                                final int end,
                                                final TabExpander t,
                                                final int pos,
                                                final boolean round) {
        return TextUtils.getTabbedTextOffset(s, fm, start, end, t, pos, round);
    }

    public static final int getTabbedTextWidth(final Segment s,
                                               final FontMetrics fm,
                                               final int x,
                                               final TabExpander t,
                                               final int pos) {
        return TextUtils.getTabbedTextWidth(s, fm, x, t, pos);
    }

    /**
     * If pos < 0 or pos > length of the document BadLocationException will be
     * thrown
     */
    public static final int getWordEnd(final JTextComponent c, final int pos)
            throws BadLocationException {
        return TextUtils.getWordEnd(TextUtils.getTextKit(c), pos);
    }

    /**
     * If pos < 0 or pos > length of the document BadLocationException will be
     * thrown.
     */
    public static final int getWordStart(final JTextComponent c, final int pos)
            throws BadLocationException {
        return TextUtils.getWordStart(TextUtils.getTextKit(c), pos);
    }
}