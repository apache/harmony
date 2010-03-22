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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import javax.swing.plaf.TextUI;

import org.apache.harmony.awt.text.TextKit;
import org.apache.harmony.awt.text.TextUtils;

import org.apache.harmony.x.swing.internal.nls.Messages;



public class DefaultHighlighter extends LayeredHighlighter {

    // This vector contains all highlights
    private final HighlightList  highlights = new HighlightList();;

    // Defines paint procedure
    private boolean        drawsLayeredHighlights;

    // JTextComponent bound to the highlighter
    private JTextComponent component;

    private TextKit textKit;

    /**
     * Provides more convinient interface for DefaultHighlighter
     */
    private class HighlightList extends ArrayList {

        public HighlightImpl getElem(final Object obj) {
            int index = indexOf(obj);
            return index >= 0 ? (HighlightImpl)get(index) : null;
        }

        public HighlightImpl getElem(final int index) {
            return (HighlightImpl)get(index);
        }
    }

    /**
     * Stores all Highlights information.
     *
     */
    private class HighlightImpl implements Highlight {
        // highlight start position
        private Position startPosition;

        // highlight end position
        private Position endPosition;

        // This variable is used for painting. This is true, if
        // Highlight painter is instanceof
        // LayeredHighlight.LayeredHighlightPainter,otherwise - false.
        private final boolean instanceLayerPainter;

        // Defines paint order for highlight.
        // Equals DefaultHighlighter.drawsLayeredHighlight, when Highlight
        // is added.
        private final boolean drawsLayered;

        // Highlight painter
        private final Highlighter.HighlightPainter painter;

        /**
         * New highlight constructor. Sets startPosition, endPosition,
         * drawsLayered, instanceLayerPainter and bounds. Parameters: p0 - start
         * position, p1 - end position, pnt - painter, r - bounds
         */
        HighlightImpl(final Position start, final Position end,
                  final HighlightPainter pnt) {
            startPosition = start;
            endPosition = end;
            painter = pnt;
            instanceLayerPainter = (pnt instanceof LayerPainter);
            drawsLayered = getDrawsLayeredHighlights();
        }

        /**
         * Changes highlight. Changes startPosition, endPosition, bounds.
         */
        public void changeHighlight(final Position start,
                                    final Position end) {
            startPosition = start;
            endPosition = end;
        }

        /**
         * Returns the endPosition offset
         */
        public int getEndOffset() {
            return endPosition.getOffset();
        }

        /**
         * Returns the painter
         */
        public Highlighter.HighlightPainter getPainter() {
            return painter;
        }

        /**
         * Returns the startPosition Offset
         */
        public int getStartOffset() {
            return startPosition.getOffset();
        }

        final boolean needSimplePaint() {
            return !drawsLayered || !instanceLayerPainter;
        }
    }

    public static class DefaultHighlightPainter extends
            LayerPainter {
        private final Color color;

        public DefaultHighlightPainter(final Color c) {
            color = c;
        }

        /**
         * @return color, defined by constructor parameter.
         */
        public Color getColor() {
            return color;
        }

        /**
         * It supposes, that p0 and p1 have Position.Bias.Forward bias.
         * Calculates appropriate rectangles by call TextUI.modelToView. If some
         * rectangles equals null, do nothing.
         *
         */
        public void paint(final Graphics g, final int p0, final int p1,
                          final Shape shape, final JTextComponent jtc) {
            TextUI textUI = jtc.getUI();
            if (textUI == null) {
                return;
            }

            int start = Math.min(p0, p1);
            int end = Math.max(p0, p1);
            Rectangle startRect = null;
            Rectangle endRect = null;
            Rectangle shapeBounds = shape.getBounds();

            try {
                startRect = textUI.modelToView(jtc, start,
                                               Position.Bias.Forward);
                endRect = textUI.modelToView(jtc, end, Position.Bias.Backward);
            } catch (final BadLocationException e) {
            }

            if (startRect == null || endRect == null) {
                return;
            }
            int startRectMaxY = startRect.y + startRect.height;
            boolean isLineGap = (startRectMaxY < endRect.y);
            boolean isDifferentLine = (startRectMaxY <= endRect.y);
            g.setColor(getRealColor(jtc));
            //It need to add 1 to width and height as that requires fillRect
            if (isLineGap) {
                g.fillRect(shapeBounds.x,
                           startRectMaxY,
                           shapeBounds.width + 1,
                           endRect.y - startRectMaxY + 1);
            }

            if (isDifferentLine) {
                g.fillRect(startRect.x,
                           startRect.y,
                           shapeBounds.width - startRect.x + 1 + shapeBounds.x,
                           startRect.height + 1);
                g.fillRect(shapeBounds.x, endRect.y,
                           endRect.x - shapeBounds.x + 1,
                           endRect.height + 1);
            } else {
                g.fillRect(Math.min(startRect.x, endRect.x),
                           startRect.y,
                           Math.abs(endRect.x - startRect.x + 1),
                           startRect.height + 1);

            }
        }

        /**
         * It supposes, that start has Position.Bias.Forward bias, end has
         * Position.Bias.Backward bias. Calculates appropriate rectangles by
         * call view.modelToView. If some rectangles equals null, do nothing.
         * This method is called by DefaultHighlighter paint method, when
         * painter is instance of LayeredHighlighter.LayeredHighlightPainter and
         * drawsLayered of the current Highlight equals true.
         */
        public Shape paintLayer(final Graphics g, final int start,
                                final int end,
                                final Shape shape, final JTextComponent jtc,
                                final View view) {
            return TextUtils.paintLayer(g, start, end, shape, getRealColor(jtc),
                                        view, true);
        }

        private Color getRealColor(final JTextComponent c) {
            return color == null ? c.getSelectionColor() : color;
        }
    }

    /**
     * DefaultHighlighter.DefaultHighlighterPainter with color equal to null
     */
    public static final LayerPainter DefaultPainter =
        new DefaultHighlightPainter(null);

    /**
     * Calls setDrawLayeredHighlights(true). Initializes internal vector
     * variable to store highlights in future.
     */
    public DefaultHighlighter() {
        setDrawsLayeredHighlights(true);
    }

    public Object addHighlight(final int start, final int end,
                               final Highlighter.HighlightPainter pnt)
            throws BadLocationException {
        checkBoundaries(start, end);
        Highlight h = new HighlightImpl(createPosition(start),
                                        createPosition(end), pnt);
        highlights.add(h);
        repaintComponent(getBoundsByOffsets(start, end));
        return h;
    }

    public void changeHighlight(final Object obj, final int start,
                                final int end)
            throws BadLocationException {
        checkBoundaries(start, end);

        HighlightImpl h = highlights.getElem(obj);
        if (h == null) {
            return;
        }

        int oldStart = h.getStartOffset();
        int oldEnd = h.getEndOffset();
        h.changeHighlight(createPosition(start), createPosition(end));
        updateHighlights(oldStart, oldEnd, start, end);
    }

    public void deinstall(final JTextComponent c) {
    }

    public boolean getDrawsLayeredHighlights() {
        return drawsLayeredHighlights;
    }

    public Highlighter.Highlight[] getHighlights() {
        return (Highlighter.Highlight[])highlights
                .toArray(new Highlighter.Highlight[highlights.size()]);
    }

    public void install(final JTextComponent c) {
        component = c;
        removeAllHighlights();
        textKit = TextUtils.getTextKit(c);
    }

    /**
     * Looks trough highlights vector (down) to calls paint of Highlight
     * painter. Method paint of highlight painter is called, if the Highlight is
     * added at getDrawsLayeredHighlights() equals false or the Highlght painter
     * doesn't instance of LayeredHighlighter.LayeredHighlightPainter.
     */

    public void paint(final Graphics g) {
        Rectangle r = TextUtils.getEditorRect(component);
        for (int i = 0; i < highlights.size(); i++) {
            HighlightImpl hElem = highlights.getElem(i);
            int start = hElem.getStartOffset();
            int end = hElem.getEndOffset();
            if (hElem.needSimplePaint()) {
                hElem.getPainter().paint(g, start, end, r, component);
            }
        }
    }

    /**
     * Look through highlights vector (up) to calls paintLayer of Highlight
     * painter. Method paintLayer of Highlight painter is called, if
     * getStartOffet() and getEndOffset() of the Highlight corresponds to view,
     * painter is instance of LayeredHighlight.LayeredHightPainter, and the
     * Highlight is added at getDrawsLayeredHighlights() equals true.
     *
     */
    public void paintLayeredHighlights(final Graphics g, final int p0,
                                       final int p1, final Shape viewBounds,
                                       final JTextComponent editor,
                                       final View view) {

        Document doc = editor.getDocument();
        int length = doc.getLength();

        for (int i = highlights.size() - 1; i >= 0; i--) {
            HighlightImpl hElem = highlights.getElem(i);
            int start = hElem.getStartOffset();
            int end = hElem.getEndOffset();

            if (hElem.needSimplePaint()
                || end > length || start > p1 || end < p0) {
                continue;
            }
            ((LayerPainter)(hElem.getPainter()))
                    .paintLayer(g, Math.max(p0, start), Math.min(p1, end),
                                viewBounds, editor, view);
        }
    }

    /**
     * Calls repaint of component with parameters corresponding to bounds of
     * deleted highlight (for each highlight).
     */
    public void removeAllHighlights() {
        while (!highlights.isEmpty()) {
            HighlightImpl hElem = highlights.getElem(0);
            Rectangle highlightBounds = getBoundsByHighlight(hElem);
            highlights.remove(0);
            repaintComponent(highlightBounds);
        }
    }

    /**
     * Calls repaint of component with parameters corresponding to bounds of
     * deleted highlight.
     *
     */
    public void removeHighlight(final Object obj) {
        Highlight highlight = highlights.getElem(obj);
        if (highlight != null) {
            Rectangle highlightBounds = getBoundsByHighlight(highlight);
            highlights.remove(highlight);
            repaintComponent(highlightBounds);
        }
    }

    /**
     * Defines paint order for Highlight is added at this setting. If b equals
     * false, added highlight will be painted only by call paint method of
     * highlight painter. Otherwise, if added highlighter is instance of
     * LayeredHighlighter.LayeredHighlightPainter, method paintLayer of
     * HighlightPainter will be used.
     *
     * @param b
     */
    public void setDrawsLayeredHighlights(final boolean b) {
        drawsLayeredHighlights = b;
    }

    private void checkBoundaries(final int start, final int end)
       throws BadLocationException {
        if (start < 0) {
            throw new BadLocationException(Messages.getString("swing.89", start), end); //$NON-NLS-1$
        }
        if (end < start) {
            throw new BadLocationException(Messages.getString("swing.89", start), end); //$NON-NLS-1$
        }
    }

    /**
     * Return Position by Offset, if document of component bound to this
     * instance of current Highlight throws BadLocationException - returns null.
     */

    private Position createPosition(final int offset)
        throws BadLocationException {
        return component.getDocument().createPosition(offset);
    }

    private void updateHighlights(final int oldStart, final int oldEnd,
                                final int newStart, final int newEnd) {
        if (oldEnd < newStart || oldStart > oldEnd) {
             repaintComponent(oldStart, oldEnd, newStart, newEnd);
        } else {
             repaintComponent(Math.min(oldStart, newStart),
                              Math.max(oldStart, newStart),
                              Math.min(oldEnd, newEnd),
                              Math.max(oldEnd, newEnd));
        }
    }

    private void repaintComponent(final int p0, final int p1,
                                  final int p2, final int p3) {
        repaintComponent(getBoundsByOffsets(p0, p1));
        repaintComponent(getBoundsByOffsets(p2, p3));
    }

    private void repaintComponent(final Rectangle r) {
        if (r != null) {
            component.repaint(r.x, r.y, r.width + 1, r.height + 1);
        }
    }

    private Rectangle getBoundsByHighlight(final Highlight highlight) {
        return TextUtils.getBoundsByOffsets(textKit,
                                            highlight.getStartOffset(),
                                            highlight.getEndOffset());
    }

    private Rectangle getBoundsByOffsets(final int p0, final int p1) {
        return TextUtils.getBoundsByOffsets(textKit, p0, p1);
    }

}


