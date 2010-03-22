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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.font.LineMetrics;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.text.GlyphView.GlyphPainter;
import javax.swing.text.Position.Bias;
import javax.swing.text.TextInterval.TextIntervalPainter;

import org.apache.harmony.awt.text.TextKit;
import org.apache.harmony.awt.text.TextUtils;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * Default GlyphPainter which is used when a GlyphView needs a painter but
 * none was set.
 */
final class DefaultGlyphPainter extends GlyphPainter {

    private final class TextPainter implements TextIntervalPainter {
        final GlyphView view;
        final Rectangle bounds;
        final Font font;
        final boolean advanced;

        TextPainter(final GlyphView view,
                    final Rectangle bounds, final Font font,
                    final boolean advanced) {
            this.view = view;
            this.bounds = bounds;
            this.font = font;
            this.advanced = advanced;
        }

        public int paintSelected(final Graphics g,
                                 final int start, final int end,
                                 final int x, final int y)
            throws BadLocationException {

            return drawSelectedText(this, g, start, end, x, y);
        }

        public int paintUnselected(final Graphics g,
                                   final int start, final int end,
                                   final int x, final int y)
            throws BadLocationException {

            return drawUnselectedText(this, g, start, end, x, y);
        }

        public int paintComposed(final Graphics g,
                                 final int start, final int end,
                                 final int x, final int y)
            throws BadLocationException {

            if (true) {
                throw new UnsupportedOperationException(Messages.getString("swing.87")); //$NON-NLS-1$
            }
            return x;
        }
    }

    private static GlyphPainter defaultPainter;

    public float getSpan(final GlyphView v,
                         final int startOffset, final int endOffset,
                         final TabExpander tabExpander, final float x) {
        final Segment text = v.getText(startOffset, endOffset);
        final FontMetrics metrics = getFontMetrics(v);
        return TextUtils.getTabbedTextWidth(text, metrics, (int)x, tabExpander,
                                            startOffset);
    }

    public float getHeight(final GlyphView v) {
        return getFontMetrics(v).getHeight();
    }

    public float getAscent(final GlyphView v) {
        return getFontMetrics(v).getAscent();
    }

    public float getDescent(final GlyphView v) {
        return getFontMetrics(v).getDescent();
    }

    public void paint(final GlyphView v, final Graphics g, final Shape alloc,
                      final int startOffset, final int endOffset) {
        final TextKit textKit = v.getTextKit();
        textKit.paintLayeredHighlights(g, startOffset, endOffset, alloc, v);

        final Rectangle bounds = alloc.getBounds();
        final Font font = v.getFont();
        final TextPainter painter = new TextPainter(v, bounds, font,
                                                    g instanceof Graphics2D
                                                    && v.isUnderline()
                                                       || v.isStrikeThrough());
        final UnselectedTextInterval ui =
            new UnselectedTextInterval(v.getStartOffset(), v.getEndOffset(),
                                       painter);
        final SelectedTextInterval si =
            new SelectedTextInterval(textKit.getSelectionStart(),
                                     textKit.getSelectionEnd(), painter);
        final List intervals = Arrays.asList(ui.dissect(si));

        Color oldColor = g.getColor();
        Font oldFont = g.getFont();

        g.setFont(font);
        final FontMetrics metrics = getFontMetrics(v, font);
        try {
            int x = bounds.x;
            final int y = bounds.y + metrics.getAscent();
            for (Iterator it = intervals.iterator(); it.hasNext();) {
                TextInterval interval = (TextInterval)it.next();
                x = interval.paint(g, x, y);
            }
        } catch (BadLocationException e) { }

        g.setFont(oldFont);
        g.setColor(oldColor);
    }

    public Shape modelToView(final GlyphView v,
                             final int offset, final Bias bias,
                             final Shape alloc)
        throws BadLocationException {

        if (offset < v.getStartOffset() || offset > v.getEndOffset()) {
            throw new BadLocationException(Messages.getString("swing.88", offset), offset); //$NON-NLS-1$
        }

        final Segment text = v.getText(v.getStartOffset(), offset);
        final FontMetrics metrics = getFontMetrics(v);
        final Rectangle bounds = alloc.getBounds();
        return new Rectangle(TextUtils.getTabbedTextWidth(text, metrics,
                                                          bounds.x,
                                                          v.getTabExpander(),
                                                          offset) + bounds.x,
                             bounds.y, 0, metrics.getHeight());
    }

    public int viewToModel(final GlyphView v,
                           final float x, final float y,
                           final Shape alloc, final Bias[] biasReturn) {
        biasReturn[0] = Bias.Forward;

        final Rectangle bounds = alloc.getBounds();
        if (x < bounds.x || y < bounds.y) {
            return v.getStartOffset();
        }
        if (x > bounds.x + bounds.width || y > bounds.y + bounds.height) {
            return v.getEndOffset() - 1;
        }
        final Segment text = v.getText(v.getStartOffset(), v.getEndOffset());
        final FontMetrics fm = getFontMetrics(v);
        return v.getStartOffset()
               + TextUtils.getTabbedTextOffset(text, fm, bounds.x, (int)x,
                                               v.getTabExpander(),
                                               v.getStartOffset());
    }

    public int getBoundedPosition(final GlyphView v,
                                  final int startOffset,
                                  final float x, final float len) {
        final Segment text = v.getText(startOffset, v.getEndOffset());
        final FontMetrics fm = getFontMetrics(v);
        return startOffset
               + TextUtils.getTabbedTextOffset(text, fm, (int)x, (int)(x + len),
                                               v.getTabExpander(),
                                               startOffset, false);
    }

    static GlyphPainter getDefaultPainter() {
        if (defaultPainter == null) {
            defaultPainter = new DefaultGlyphPainter();
        }
        return defaultPainter;
    }

    static FontMetrics getFontMetrics(final GlyphView v) {
        return getFontMetrics(v, v.getFont());
    }

    static FontMetrics getFontMetrics(final GlyphView v, final Font font) {
        Component c = v.getComponent();
        return c != null ? c.getFontMetrics(font)
                         : Toolkit.getDefaultToolkit().getFontMetrics(font);
    }

    int drawText(final GlyphView v, final Graphics g,
                 final int start, final int end,
                 final int x, final int y) throws BadLocationException {
        return TextUtils.drawTabbedText(v.getText(start, end), x, y,
                                        g, v.getTabExpander(), start);
    }

    int drawSelectedText(final TextPainter tp, final Graphics g,
                         final int start, final int end,
                         final int x, final int y) throws BadLocationException {
        g.setColor(tp.view.getTextKit().getSelectedTextColor());
        final int result = drawText(tp.view, g, start, end, x, y);
        if (tp.advanced) {
            paintAdvancedStyles(tp.view, start, end, (Graphics2D)g, tp.font,
                                tp.view.modelToView(start, Bias.Forward,
                                                    end, Bias.Backward,
                                                    tp.bounds).getBounds());
        }
        return result;
    }

    int drawUnselectedText(final TextPainter tp, final Graphics g,
                           final int start, final int end,
                           final int x, final int y)
        throws BadLocationException {

        Rectangle r = null;
        try {
            r = tp.view.modelToView(start, Bias.Forward,
                              end, Bias.Backward, tp.bounds).getBounds();
        } catch (BadLocationException e) { }

        Color back = tp.view.getBackground();
        if (back != null && r != null) {
            g.setColor(back);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        g.setColor(tp.view.getForeground());
        final int result = drawText(tp.view, g, start, end, x, y);
        if (tp.advanced) {
            paintAdvancedStyles(tp.view, start, end, (Graphics2D)g, tp.font, r);
        }
        return result;
    }

    private void paintAdvancedStyles(final GlyphView v,
                                     final int start, final int end,
                                     final Graphics2D g,
                                     final Font font, final Rectangle bounds) {
        final Segment text = v.getText(start, end);
        final LineMetrics lm = font.getLineMetrics(text.array, text.offset,
                                                   text.offset + text.count,
                                                   g.getFontRenderContext());
        int baseline = bounds.y + Math.round(lm.getAscent());

        if (v.isStrikeThrough()) {
            int offset = Math.round(lm.getStrikethroughOffset()) + baseline;
            BasicStroke stroke =
                new BasicStroke(lm.getStrikethroughThickness());
            Stroke oldStroke = g.getStroke();
            g.setStroke(stroke);
            g.drawLine(bounds.x, offset, bounds.x + bounds.width, offset);
            g.setStroke(oldStroke);
        }

        if (v.isUnderline()) {
            int offset = Math.round(lm.getUnderlineOffset()) + baseline;
            BasicStroke stroke = new BasicStroke(lm.getUnderlineThickness());
            Stroke oldStroke = g.getStroke();
            g.setStroke(stroke);
            g.drawLine(bounds.x, offset, bounds.x + bounds.width, offset);
            g.setStroke(oldStroke);
        }
    }
}
