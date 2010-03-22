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
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Position.Bias;

import org.apache.harmony.awt.text.TextKit;
import org.apache.harmony.awt.text.TextUtils;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class WrappedPlainView extends BoxView implements TabExpander {

    private class LineView extends View {

        private ArrayList breaks = new ArrayList();
        private int wrappedWidth;

        public LineView(final Element element) {
            super(element);
        }

        public float getPreferredSpan(final int axis) {
            final int width = getWidth();
            if (axis == X_AXIS) {
                return width > 0 ? width : getTextWidth(getStartOffset(),
                                                        getEndOffset());
            }

            if (wrappedWidth != width) {
                wrapLines();
                wrappedWidth = width;
            }
            return getLineCount() * paintParams.metrics.getHeight();
        }

        public void insertUpdate(final DocumentEvent event, final Shape shape,
                                 final ViewFactory factory) {
            updateView(shape);
            super.insertUpdate(event, shape, factory);
        }

        public Shape modelToView(final int pos, final Shape shape,
                                 final Bias bias)
            throws BadLocationException {

            final int lineStart = getStartOffset();
            int start = lineStart;
            int end   = getEndOffset() - 1;

            if (pos < start || pos > end) {
                throw new BadLocationException(Messages.getString("swing.98"), pos); //$NON-NLS-1$
            }

            int lineNo = 0;
            for (int i = 0; i < breaks.size(); i++) {
                int breakPos = ((Integer)breaks.get(i)).intValue() + lineStart;
                if (pos < breakPos
                    || bias == Bias.Backward && pos == breakPos) {

                    end = breakPos;
                    break;
                }
                start = breakPos;
                ++lineNo;
            }

            Rectangle bounds = shape.getBounds();
            getDocument().getText(start, pos - start, getLineBuffer());
            Rectangle result = new Rectangle(
                    TextUtils.getTabbedTextWidth(getLineBuffer(),
                                                 paintParams.metrics,
                                                 bounds.x,
                                                 WrappedPlainView.this, pos)
                    + bounds.x, bounds.y
                    + lineNo * paintParams.metrics.getHeight(),
                    1, paintParams.metrics.getHeight());
            return result;
        }

        public void paint(final Graphics g, final Shape shape) {
            final int lineStart = getStartOffset();
            int start = lineStart;
            Rectangle bounds = shape.getBounds();
            int y = bounds.y + paintParams.metrics.getHeight()
                    - paintParams.metrics.getDescent();

            if (breaks.size() > 0) {
                for (int i = 0; i < breaks.size(); i++) {
                    int endPos = ((Integer)breaks.get(i)).intValue()
                                 + lineStart;
                    paintLine(start, endPos, g, shape, bounds.x, y, false);
                    start = endPos;
                    y += paintParams.metrics.getHeight();
                }
            }
            paintLine(start, getEndOffset(), g, shape, bounds.x, y, true);
        }

        public void removeUpdate(final DocumentEvent event, final Shape shape,
                                 final ViewFactory factory) {
            updateView(shape);
            super.removeUpdate(event, shape, factory);
        }

        public int viewToModel(final float x, final float y,
                               final Shape shape, final Bias[] biasReturn) {
            biasReturn[0] = Position.Bias.Forward;

            final Rectangle bounds = shape.getBounds();
            final int lineHeight = paintParams.metrics.getHeight();

            final int lineNo = ((int)y - bounds.y) / lineHeight;

            if (lineNo > breaks.size()) {
                return getEndOffset() - 1;
            } else {
                final Element  line = getElement();
                final int lineStart = line.getStartOffset();
                final int start = lineNo > 0 && breaks.size() > 0
                                  ? ((Integer)breaks.get(lineNo - 1)).intValue()
                                    + lineStart
                                  : lineStart;
                final int end   = lineNo < breaks.size()
                                  ? ((Integer)breaks.get(lineNo)).intValue()
                                    + lineStart
                                  : line.getEndOffset() - 1;

                try {
                    getDocument().getText(start, end - start, getLineBuffer());
                } catch (final BadLocationException e) { }

                return start
                       + TextUtils.getTabbedTextOffset(getLineBuffer(),
                                                       paintParams.metrics,
                                                       bounds.x,
                                                      (int)x,
                                                      WrappedPlainView.this,
                                                      start);
            }
        }

        private int getLineCount() {
            return breaks.size() + 1;
        }

        private int getTextWidth(final int start, final int end) {
            try {
                getDocument().getText(start, end - start, getLineBuffer());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            return TextUtils.getTabbedTextWidth(getLineBuffer(),
                                                paintParams.metrics, 0,
                                                WrappedPlainView.this, start);

        }

        private void paintHilite(final int start, final int end,
                                 final Graphics g, final Shape shape) {
            TextKit textKit = getTextKit();
            if (textKit != null) {
                textKit.paintLayeredHighlights(g, start, end, shape, this);
            }
        }

        private void paintLine(final int start, final int end,
                               final Graphics g, final Shape shape,
                               final int x, final int y,
                               final boolean chopDrawing) {
            int drawEnd = end;
            if (chopDrawing) {
                --drawEnd;
            }
            paintHilite(start, drawEnd, g, shape);

            WrappedPlainView.this.drawLine(start, drawEnd, g, x, y);
        }

        private void updateView(final Shape shape) {
            final int lineCount = getLineCount();
            final boolean widthValid = getWidth() > 0;
            if (widthValid) {
                wrapLines();
            }
            final boolean widthChanged = !widthValid;
            final boolean heightChanged = lineCount != getLineCount();
            if (widthChanged || heightChanged) {
                preferenceChanged(this, widthChanged, heightChanged);
            } else if (shape != null) {
                Component c = getComponent();
                if (c != null) {
                    Rectangle bounds = shape.getBounds();
                    getComponent().repaint(bounds.x, bounds.y,
                                           bounds.width, bounds.height);
                }
            }
        }

        private void wrapLines() {
            final Element line = getElement();
            final int lineStart = line.getStartOffset();
            int start = lineStart;
            int end   = line.getEndOffset() - 1;

            breaks.clear();

            final int width = getWidth();
            int textWidth = getTextWidth(start, end);
            while (textWidth > width) {
                int breakPos = calculateBreakPosition(start, end);
                breaks.add(new Integer(breakPos - lineStart));
                start = breakPos;
                textWidth = getTextWidth(start, end);
            }
        }

    }

    final ViewFactory viewFactory = new ViewFactory() {
        public View create(final Element element) {
            return new LineView(element);
        }
    };

    private final TextPaintParams paintParams = new TextPaintParams(this);

    private boolean wordWrap;

    public WrappedPlainView(final Element element) {
        this(element, false);
    }

    public WrappedPlainView(final Element element, final boolean wordWrap) {
        super(element, Y_AXIS);
        this.wordWrap = wordWrap;
    }

    public void changedUpdate(final DocumentEvent event,
                              final Shape shape,
                              final ViewFactory factory) {
        super.changedUpdate(event, shape, viewFactory);
    }

    public float getMaximumSpan(final int axis) {
        paintParams.conditionalUpdateMetrics();
        return super.getMaximumSpan(axis);
    }

    public float getMinimumSpan(final int axis) {
        paintParams.conditionalUpdateMetrics();
        return super.getMinimumSpan(axis);
    }

    public float getPreferredSpan(final int axis) {
        paintParams.conditionalUpdateMetrics();
        return super.getPreferredSpan(axis);
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }

    public void insertUpdate(final DocumentEvent event,
                             final Shape shape,
                             final ViewFactory factory) {
        super.insertUpdate(event, shape, viewFactory);
    }

    public float nextTabStop(final float x, final int tabOffset) {
        return paintParams.nextTabStop(x);
    }

    public void paint(final Graphics g, final Shape shape) {
        paintParams.updateFields();
        super.paint(g, shape);
    }

    public void removeUpdate(final DocumentEvent event,
                             final Shape shape,
                             final ViewFactory factory) {
        super.removeUpdate(event, shape, viewFactory);
    }

    public void setSize(final float width, final float height) {
        if (getWidth() != width - getLeftInset() - getRightInset()) {
            preferenceChanged(null, true, true);
        }
        paintParams.conditionalUpdateMetrics();
        super.setSize(width, height);
    }

    protected int calculateBreakPosition(final int start, final int end) {
        paintParams.conditionalUpdateMetrics();

        try {
            getDocument().getText(start, end - start, paintParams.buffer);
        } catch (BadLocationException e) { }

        int offset;
        if (wordWrap) {
            offset = TextUtils.getBreakLocation(paintParams.buffer,
                                                paintParams.metrics,
                                                0, getWidth(),
                                                this, start);
        } else {
            offset = TextUtils.getTabbedTextOffset(paintParams.buffer,
                                                   paintParams.metrics,
                                                   0, getWidth(),
                                                   this, start,
                                                   false);
        }

        return offset + start;
    }

    protected void drawLine(final int start, final int end,
                            final Graphics g, final int x, final int y) {
        drawLine(paintParams, start, end, g, x, y);
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

    protected void loadChildren(final ViewFactory factory) {
        super.loadChildren(viewFactory);
    }
}
