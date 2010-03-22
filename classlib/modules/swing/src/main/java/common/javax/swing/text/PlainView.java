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

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentEvent.EventType;

import org.apache.harmony.awt.text.TextKit;
import org.apache.harmony.awt.text.TextUtils;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class PlainView extends View implements TabExpander {
    protected FontMetrics metrics;
    final TextPaintParams paintParams = new TextPaintParams(this);
    private Element widestLine;
    private int widestLineWidth;

    /**
     * It is assumed that <code>element</code> is default root of document
     * this view represents (PlainDocument).
     */
    public PlainView(final Element element) {
        super(element);
    }

    public float getPreferredSpan(final int axis) {
        conditionalUpdateMetrics();

        switch (axis) {
        case X_AXIS:
            return getLineWidth(widestLine);

        case Y_AXIS:
            return metrics.getHeight() * getElement().getElementCount();

        default:
            throw new IllegalArgumentException(Messages.getString("swing.00", axis)); //$NON-NLS-1$
        }
    }

    public void insertUpdate(final DocumentEvent event, final Shape shape,
                             final ViewFactory factory) {
        updateDamage(event, shape, factory);
    }

    public void removeUpdate(final DocumentEvent event, final Shape shape,
                             final ViewFactory factory) {
        updateDamage(event, shape, factory);
    }

    public void changedUpdate(final DocumentEvent event, final Shape shape,
                              final ViewFactory factory) {
        updateDamage(event, shape, factory);
    }

    /**
     * Returns the rectangle where the caret will be painted. This rectangle
     * is positioned between characters (at position <code>pos</code>).
     * Its width is 1, and height is the height of the line (of the font used).
     * <p>For more information about position see
     * http://java.sun.com/products/jfc/tsc/articles/text/element_buffer/
     * in the heading <strong>The Position Interface</strong>.)
     */
    public Shape modelToView(final int pos, final Shape shape,
                             final Position.Bias bias)
        throws BadLocationException {

        if (pos < 0 || pos > getDocument().getLength() + 1) {
            throw new BadLocationException(Messages.getString("swing.98"), pos); //$NON-NLS-1$
        }

        final int lineNo = getElement().getElementIndex(pos);
        final Element line = getElement().getElement(lineNo);

        getDocument().getText(line.getStartOffset(),
                              pos - line.getStartOffset(), getLineBuffer());
        final Rectangle bounds = shape.getBounds();
        return new Rectangle(
                TextUtils.getTabbedTextWidth(getLineBuffer(), metrics,
                                             bounds.x, this, pos) + bounds.x,
                bounds.y + metrics.getHeight() * lineNo,
                1, metrics.getHeight());
    }

    public int viewToModel(final float x, final float y,
                           final Shape shape,
                           final Position.Bias[] biasReturn) {
        biasReturn[0] = Position.Bias.Forward;

        final Rectangle bounds = shape.getBounds();

        if (y >= getPreferredSpan(Y_AXIS) + bounds.y) {
            return getDocument().getLength();
        }
        if (y < bounds.y) {
            return 0;
        }

        final int lineNo = (int)(y - bounds.y) / metrics.getHeight();

        final Element line  = getElement().getElement(lineNo);
        final int     start = line.getStartOffset();
        final int     end   = line.getEndOffset() - 1;
        try {
            getDocument().getText(start, end - start, getLineBuffer());
        } catch (final BadLocationException e) { }

        return start + TextUtils.getTabbedTextOffset(getLineBuffer(), metrics,
                                                     bounds.x,
                                                     (int)Math.max(x, bounds.x),
                                                     this, start);
    }

    public float nextTabStop(final float x, final int tabOffset) {
        conditionalUpdateMetrics();
        return paintParams.nextTabStop(x);
    }

    public void paint(final Graphics g, final Shape shape) {
        final Rectangle bounds = shape.getBounds();
        int y = bounds.y + metrics.getAscent();

        paintParams.updateFields();

        final Rectangle clipBounds = g.getClipBounds();
        final int height = metrics.getHeight();

        final TextKit textKit = getTextKit();
        for (int i = 0; i < getElement().getElementCount(); i++, y += height) {
            if (!lineToRect(shape, i).intersects(clipBounds)) {
                continue;
            }

            if (textKit != null) {
                Element line = getElement().getElement(i);
                textKit.paintLayeredHighlights(g, line.getStartOffset(),
                                               line.getEndOffset() - 1,
                                               shape, this);
            }
            drawLine(i, g, bounds.x, y);
        }
    }

    public void setSize(final float width, final float height) {
        conditionalUpdateMetrics();
    }

    protected void drawLine(final int lineNo, final Graphics g,
                            final int x, final int y) {
        final Element line = getElement().getElement(lineNo);
        drawLine(paintParams,
                 line.getStartOffset(), line.getEndOffset() - 1,
                 g, x, y);
    }

    protected int drawSelectedText(final Graphics g,
                                   final int x, final int y,
                                   final int start, final int end)
        throws BadLocationException {

        return drawText(g, paintParams.selColor, paintParams,
                        x, y, start, end);
    }

    protected int drawUnselectedText(final Graphics g,
                                     final int x, final int y,
                                     final int start, final int end)
        throws BadLocationException {

        return drawText(g, paintParams.color, paintParams,
                        x, y, start, end);
    }

    protected final Segment getLineBuffer() {
        return paintParams.buffer;
    }

    protected int getTabSize() {
        return paintParams.getTabSize();
    }

    protected Rectangle lineToRect(final Shape shape, final int lineNo) {
        conditionalUpdateMetrics();

        int height = metrics.getHeight();
        Rectangle bounds = shape.getBounds();
        return new Rectangle(bounds.x, bounds.y + height * lineNo,
                             bounds.width, height);
    }

    protected void damageLineRange(final int startLine,
                                   final int endLine,
                                   final Shape shape,
                                   final Component component) {
        Rectangle lineRect;
        for (int i = startLine; i <= endLine; i++) {
            lineRect = lineToRect(shape, i);
            component.repaint(lineRect.x, lineRect.y,
                              lineRect.width, lineRect.height);
        }
    }

    protected void updateDamage(final DocumentEvent event, final Shape shape,
                                final ViewFactory factory) {
        if (shape == null) {
            return;
        }

        if (metrics == null) {
            updateMetrics();
            preferenceChanged(null, true, true);
            return;
        }

        final ElementChange change = event.getChange(getElement());

        if (event.getType() == EventType.INSERT) {
            updateDamageOnInsert(event, change, shape);
        } else {
            updateDamageOnRemove(event, change, shape);
        }
    }

    protected void updateMetrics() {
        paintParams.updateMetrics();
        metrics = paintParams.metrics;

        updateWidestLine();
    }

    final void conditionalUpdateMetrics() {
        if (paintParams.areMetricsValid()) {
            updateMetrics();
        }
    }

    private void updateWidestLine() {
        widestLine = getElement().getElement(0);
        widestLineWidth = getLineWidth(widestLine);

        updateWidestLine(1, getElement().getElementCount() - 1);
    }

    private void updateWidestLine(final int start, final int end) {
        for (int i = start; i <= end; i++) {
            int w = getLineWidth(i);
            if (w > widestLineWidth) {
                widestLineWidth = w;
                widestLine = getElement().getElement(i);
            }
        }
    }

    private int getLineWidth(final Element line) {
        try {
            getDocument().getText(line.getStartOffset(),
                                  line.getEndOffset()
                                  - line.getStartOffset() - 1,
                                  getLineBuffer());
        } catch (final BadLocationException e) { }

        return TextUtils.getTabbedTextWidth(getLineBuffer(), metrics, 0,
                                            this, line.getStartOffset());
    }

    private int getLineWidth(final int lineNo) {
        return getLineWidth(getElement().getElement(lineNo));
    }

    private void updateDamageOnInsert(final DocumentEvent event,
                                      final ElementChange change,
                                      final Shape shape) {
        boolean linesAdded = change != null;
        int start = linesAdded
                    ? change.getIndex()
                    : getElement().getElementIndex(event.getOffset());
        int length = linesAdded ? change.getChildrenAdded().length - 1 : 0;
        int width = widestLineWidth;
        if (widestLine.getEndOffset() < event.getOffset()
            || widestLine.getStartOffset() > event.getOffset()
                                             + event.getLength()) {
            // The previous longest line was not affected
            updateWidestLine(start, start + length);
        } else {
            updateWidestLine();
        }
        preferenceChanged(null, widestLineWidth != width, linesAdded);
        damageLineRange(start,
                        linesAdded ? getElement().getElementCount() - 1 : start,
                        shape, getComponent());
    }

    private void updateDamageOnRemove(final DocumentEvent event,
                                      final ElementChange change,
                                      final Shape shape) {
        int width = widestLineWidth;
        if (change != null) {
            updateWidestLine();
            preferenceChanged(null, widestLineWidth != width, true);
            getComponent().repaint();
        } else {
            int lineNo = getElement().getElementIndex(event.getOffset());
            Element line = getElement().getElement(lineNo);
            if (widestLine == line) {
                updateWidestLine();
                preferenceChanged(null, widestLineWidth != width, false);
            }
            damageLineRange(lineNo, lineNo, shape, getComponent());
        }
    }

}
