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
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.text.AttributedString;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.Position.Bias;
import javax.swing.text.TextInterval.TextIntervalPainter;

import org.apache.harmony.awt.text.ComposedTextParams;
import org.apache.harmony.awt.text.TextKit;
import org.apache.harmony.awt.text.TextUtils;

public abstract class View implements SwingConstants {

    private class TextPainter implements TextIntervalPainter {
        private final TextPaintParams tpp;

        TextPainter(final TextPaintParams tpp) {
            this.tpp = tpp;
        }

        public int paintSelected(final Graphics g,
                                 final int start, final int end,
                                 final int x, final int y)
            throws BadLocationException {

            return drawSelectedText(g, x, y, start, end);
        }

        public int paintUnselected(final Graphics g,
                                   final int start, final int end,
                                   final int x, final int y)
            throws BadLocationException {

            return drawUnselectedText(g, x, y, start, end);
        }

        public int paintComposed(final Graphics g,
                                 final int start, final int end,
                                 final int x, final int y)
            throws BadLocationException {

            return drawComposedText(g, tpp.color, tpp.composedText, x, y);
        }
    }

    public static final int BadBreakWeight = 0;
    public static final int GoodBreakWeight = 1000;
    public static final int ExcellentBreakWeight = 2000;
    public static final int ForcedBreakWeight = 3000;

    public static final int X_AXIS = 0;
    public static final int Y_AXIS = 1;

    static final float ALIGN_LEFT   = 0.0f;
    static final float ALIGN_CENTER = 0.5f;
    static final float ALIGN_RIGHT  = 1.0f;

    Component component;

    private Element element;
    private View    parent;

    public View(final Element element) {
        this.element = element;
    }

    public abstract float getPreferredSpan(final int axis);

    public abstract Shape modelToView(final int pos, final Shape shape,
                                      final Bias bias)
        throws BadLocationException;

    public abstract int viewToModel(final float x,
                                    final float y,
                                    final Shape shape,
                                    final Bias[] biasReturn);

    public abstract void paint(final Graphics g, final Shape shape);

    public View breakView(final int axis, final int offset, final float pos,
                          final float len) {
        return this;
    }

    public View createFragment(final int startOffset, final int endOffset) {
        return this;
    }

    public float getAlignment(final int axis) {
        return ALIGN_CENTER;
    }

    public AttributeSet getAttributes() {
        return getElement().getAttributes();
    }

    public int getBreakWeight(final int axis, final float pos,
                              final float len) {
        return getPreferredSpan(axis) < len ? GoodBreakWeight : BadBreakWeight;
    }

    public Shape getChildAllocation(final int index, final Shape shape) {
        return null;
    }

    public Container getContainer() {
        return getParent() != null ? getParent().getContainer() : null;
    }

    public Document getDocument() {
        return getElement().getDocument();
    }

    public Element getElement() {
        return element;
    }

    public int getStartOffset() {
        return getElement().getStartOffset();
    }

    public int getEndOffset() {
        return getElement().getEndOffset();
    }

    public Graphics getGraphics() {
        return getComponent().getGraphics();
    }

    /**
     * If view is not resizable, returns <code>getPreferredSpan(axis)</code>.
     * Otherwise (resizable), returns 0.
     */
    public float getMinimumSpan(final int axis) {
        return getResizeWeight(axis) <= 0 ? getPreferredSpan(axis) : 0;
    }

    /**
     * If view is not resizable, returns <code>getPreferredSpan(axis)</code>.
     * Otherwise (resizable), returns Integer.MAX_VALUE.
     */
    public float getMaximumSpan(final int axis) {
        return getResizeWeight(axis) <= 0 ? getPreferredSpan(axis)
                                          : Integer.MAX_VALUE;
    }

    public int getNextVisualPositionFrom(final int pos,
                                         final Bias bias,
                                         final Shape shape,
                                         final int direction,
                                         final Bias[] biasRet)
        throws BadLocationException {

        return TextUtils.getNextVisualPositionFrom(getTextKit(), this,
                                                   pos, bias, shape,
                                                   direction, biasRet);
    }

    public View getParent() {
        return parent;
    }

    public int getResizeWeight(final int axis) {
        return 0;
    }

    public String getToolTipText(final float x, final float y,
                                 final Shape shape) {
        final int index = getViewIndex(x, y, shape);
        if (index == -1) {
            return null;
        }

        final View view = getView(index);
        return view != null 
               ? view.getToolTipText(x, y, getChildAllocation(index, shape)) 
               : null;
    }

    public View getView(final int index) {
        return null;
    }

    public int getViewCount() {
        return 0;
    }

    public ViewFactory getViewFactory() {
        return getParent() != null ? getParent().getViewFactory() : null;
    }

    public int getViewIndex(final float x, final float y, final Shape shape) {
        if (shape == null) {
            return -1;
        }

        final int count = getViewCount();
        for (int i = 0; i < count; i++) {
            final Shape childAllocation = getChildAllocation(i, shape);
            if (childAllocation != null && childAllocation.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    public int getViewIndex(final int pos, final Bias bias) {
        return -1;
    }

    public void changedUpdate(final DocumentEvent event, final Shape shape,
                              final ViewFactory factory) {
        updateView(event, shape, factory);
    }

    public void insertUpdate(final DocumentEvent event, final Shape shape,
                             final ViewFactory factory) {
        updateView(event, shape, factory);
    }

    public void removeUpdate(final DocumentEvent event, final Shape shape,
                             final ViewFactory factory) {
        updateView(event, shape, factory);
    }

    public boolean isVisible() {
        return true;
    }

    public void preferenceChanged(final View child,
                                  final boolean width,
                                  final boolean height) {
        if (getParent() != null) {
            getParent().preferenceChanged(this, width, height);
        }
    }

    public void append(final View view) {
        replace(getViewCount(), 0, new View[] {view});
    }

    public void insert(final int index, final View view) {
        replace(index, 0, new View[] {view});
    }

    public void remove(final int index) {
        replace(index, 1, null);
    }

    public void removeAll() {
        replace(0, getViewCount(), null);
    }

    public void replace(final int index, final int length, final View[] views) {
    }

    public void setParent(final View parent) {
        this.parent = parent;
        if (parent == null) {
            for (int i = 0; i < getViewCount(); i++) {
                getView(i).setParent(null);
            }
        }
    }

    public void setSize(final float width, final float height) {
    }

    public Shape modelToView(final int p1, final Bias b1,
                             final int p2, final Bias b2,
                             final Shape shape) throws BadLocationException {
        Rectangle r1 = modelToView(p1, shape, b1).getBounds();
        Rectangle r2 = modelToView(p2, shape, b2).getBounds();
        return r1.union(r2);
    }

    /**
     * @deprecated
     */
    public Shape modelToView(final int pos, final Shape shape)
        throws BadLocationException {

        return modelToView(pos, shape, Bias.Forward);
    }

    /**
     * @deprecated
     */
    public int viewToModel(final float x, final float y, final Shape shape) {
        return viewToModel(x, y, shape, new Bias[] {Bias.Forward});
    }

    /**
     * The event is forwarded to all child views that lie in the range of
     * the change, i.e. <code>event.getOffset()</code> up to
     * <code>(event.getOffset() + event.getLength())</code>.
     * <p>
     * If <code>event.getOffset()</code> is boundary of children, then
     * the previous child is included to the update range.
     * <p>
     * If <code>change</code> is not <code>null</code>, children that
     * have just been added by <code>updateChildren</code> are excluded
     * from the update range.
     * <p>
     * <code>change</code> can be <code>null</code> if the element this view
     * represents has not changed, or if its children represent portions of
     * elements.
     *
     * @param change is always <code>null</code>
     *               if <code>updateChildren</code> returned <code>false</code>,
     *               otherwise it has the value returned by
     *               <code>event.getChange(getElement())</code>.
     */
    protected void forwardUpdate(final DocumentEvent.ElementChange change,
                                 final DocumentEvent event,
                                 final Shape shape,
                                 final ViewFactory factory) {
        final int offset = event.getOffset();
        int start = getViewIndex(offset, Bias.Forward);
        if (start < 0) {
            start = 0;
        }
        int end = event.getType() == EventType.REMOVE
                  ? start
                  : getViewIndex(offset + event.getLength(), Bias.Forward);
        if (end < 0) {
            end = getViewCount() - 1;
        }

        if (start > 0 && getView(start - 1).getEndOffset() == offset) {
            --start;
        }

        if (change != null) {
            end -= change.getChildrenAdded().length;
        }

        for (int i = start; i <= end; i++) {
            forwardUpdateToView(getView(i), event,
                                getChildAllocation(i, shape), factory);
        }
    }

    protected void forwardUpdateToView(final View view,
                                       final DocumentEvent event,
                                       final Shape shape,
                                       final ViewFactory factory) {
        final DocumentEvent.EventType type = event.getType();

        if (type == DocumentEvent.EventType.INSERT) {
            view.insertUpdate(event, shape, factory);
        } else if (type == DocumentEvent.EventType.REMOVE) {
            view.removeUpdate(event, shape, factory);
        } else if (type == DocumentEvent.EventType.CHANGE) {
            view.changedUpdate(event, shape, factory);
        }
    }

    protected boolean updateChildren(final DocumentEvent.ElementChange change,
                                     final DocumentEvent event,
                                     final ViewFactory factory) {
        final Element[] added = change.getChildrenAdded();
        View[] views = null;
        if (added != null && added.length > 0) {
            views = new View[added.length];
            for (int i = 0; i < added.length; i++) {
                views[i] = factory.create(added[i]);
            }
        }
        replace(change.getIndex(), change.getChildrenRemoved().length, views);
        return true;
    }

    protected void updateLayout(final DocumentEvent.ElementChange change,
                                final DocumentEvent event, final Shape shape) {
        if (change != null) {
            preferenceChanged(null, true, true);
            final Component c = getComponent();
            if (c != null) {
                c.repaint();
            }
        }
    }

    final int drawComposedText(final Graphics g, final Color color,
                               final ComposedTextParams composedTextParams,
                               final int x, final int y) {
        final Color oldColor = g.getColor();
        g.setColor(color);
        AttributedString text = composedTextParams.getComposedText();
        int result = text == null
                     ? x
                     : TextUtils.drawComposedText(getTextKit(), text, g, x, y);
        g.setColor(oldColor);
        return result;
    }

    void drawLine(final TextPaintParams tpp,
                  final int start, final int end,
                  final Graphics g, final int x, final int y) {

        TextPainter painter = new TextPainter(tpp);
        UnselectedTextInterval ui =
                new UnselectedTextInterval(start, end, painter);
        SelectedTextInterval si =
                new SelectedTextInterval(tpp.selStart, tpp.selEnd, painter);

        TextInterval[] uisi = ui.dissect(si);
        List intervals;

        if (tpp.composedText != null) {
            intervals = new LinkedList();
            ComposedTextInterval ci =
                new ComposedTextInterval(tpp.composedStart, tpp.composedEnd,
                                         painter);
            for (int i = 0; i < uisi.length; i++) {
                TextInterval[] uisici = uisi[i].dissect(ci);
                for (int j = 0; j < uisici.length; j++) {
                    if (!intervals.contains(uisici[j])) {
                        intervals.add(uisici[j]);
                    }
                }
            }
        } else {
            intervals = Arrays.asList(uisi);
        }

        try {
            int nextX = x;
            for (Iterator it = intervals.iterator(); it.hasNext();) {
                TextInterval interval = (TextInterval)it.next();
                nextX = interval.paint(g, nextX, y);
            }
        } catch (BadLocationException e) { }
    }

    int drawSelectedText(final Graphics g,
                         final int x, final int y,
                         final int start, final int end)
        throws BadLocationException {

        return 0;
    }

    final int drawText(final Graphics g, final Color color,
                       final TextPaintParams tpp,
                       final int x, final int y,
                       final int start, final int end)
        throws BadLocationException {

        getDocument().getText(start, end - start, tpp.buffer);

        final Color oldColor = g.getColor();
        g.setColor(color);
        int result = TextUtils.drawTabbedText(tpp.buffer, x, y, g,
                                              (TabExpander)tpp.view, start);
        g.setColor(oldColor);
        return result;

    }

    int drawUnselectedText(final Graphics g,
                         final int x, final int y,
                         final int start, final int end)
        throws BadLocationException {

        return 0;
    }

    Component getComponent() {
        final Component comp = getContainer();
        if (comp == null) {
            return getParent() != null ? getParent().getComponent() : null;
        } else {
            return comp;
        }
    }

    final TextKit getTextKit() {
        final Component c = getComponent();
        return c != null ? TextUtils.getTextKit(getComponent()) : null;
    }

    private void updateView(final DocumentEvent event, final Shape shape,
                            final ViewFactory factory) {
        if (getViewCount() == 0) {
            return;
        }

        ElementChange change = event.getChange(getElement());
        if (change != null && !updateChildren(change, event, factory)) {
            // updateChildren returned false, then forwardUpdate and
            // updateLayout must be passed null as change despite
            // change is not null
            change = null;
        }

        forwardUpdate(change, event, shape, factory);
        updateLayout(change, event, shape);
    }
}