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
 * @author Dmitry A. Durnev
 */
package org.apache.harmony.awt;

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Implements scrolling behavior for any container which
 * has up to 1 child and up to 2 scrollbars[and implements
 * Scrollable interface], for example ScrollPane.
 * Listens to adjustment events and mouse wheel events
 * and updates the scrollable object in response.
 * @see org.apache.harmony.awt.Scrollable
 */
public class ScrollStateController extends ComponentAdapter implements
        AdjustmentListener, MouseWheelListener {

    private static final int HSCROLLBAR_HEIGHT = 16;

    private static final int VSCROLLBAR_WIDTH = 16;

    private final Scrollable scrollable;

    private final Component component;

    private final Adjustable hAdj;

    private final Adjustable vAdj;

    public ScrollStateController(Scrollable scrollable) {
        this.scrollable = scrollable;
        component = scrollable.getComponent();
        hAdj = scrollable.getHAdjustable();
        vAdj = scrollable.getVAdjustable();
    }

    /**
     * Scrolls vertically or horizontally(if scrollable has only
     * horizontal scrollbar) on mouse wheel move
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        int type = e.getScrollType();
        int unitType = MouseWheelEvent.WHEEL_UNIT_SCROLL;
        Adjustable adj = vAdj;
        if (!isAdjNeeded(vAdj) && isAdjNeeded(hAdj)) {
            // scroll horizontally if only horiz scrollbar
            // is present
            adj = hAdj;
        }
        int incrSize = (type == unitType ? adj.getUnitIncrement() : adj
                .getBlockIncrement());
        int scrollAmount = e.getUnitsToScroll() * incrSize;

        adj.setValue(adj.getValue() + scrollAmount);
    }

    /**
     * Scrolls component on adjustment events from
     * scrollbars, repaints scrollable viewport
     */
    public void adjustmentValueChanged(AdjustmentEvent e) {
        // set scrolled component's position here:
        Adjustable srcAdj = e.getAdjustable();
        int val = e.getValue();

        Insets ins = scrollable.getInsets();
        Point loc = scrollable.getLocation();
        if (srcAdj == hAdj) {
            loc.x = ins.left - val;
        }
        if (srcAdj == vAdj) {
            loc.y = ins.top - val;
        }
        scrollable.setLocation(loc);
        int hSize = scrollable.getWidth() - getHrzInsets(ins);
        int vSize = scrollable.getHeight() - getVrtInsets(ins);
        Rectangle r = new Rectangle(ins.left, ins.top, hSize, vSize);
        scrollable.doRepaint(r); // repaint only client area
    }

    /**
     * Recalculates internal scrollable layout
     * on component resize
     */
    @Override
    public void componentResized(ComponentEvent e) {
        if (!component.isDisplayable()) {
            return;
        }
        scrollable.doRepaint();
    }

    /**
     * Adds vertical scrollbar width to the specified horizontal
     * insets
     * @param insets
     * @return sum of left & right insets & vertical scrollbar width
     */
    private int getHrzInsets(Insets insets) {
        return (insets != null) ?
            insets.left + insets.right + scrollable.getAdjustableWidth() : 0;
    }
    
    /**
     * Adds horizontal scrollbar height to the specified vertical
     * insets
     * @param insets
     * @return sum of top & bottom insets & horizontal scrollbar height
     */
    private int getVrtInsets(Insets insets) {
        return (insets != null) ?
            insets.top + insets.bottom + scrollable.getAdjustableHeight() : 0;
    }

    /**
     * Sets scrollbars bounds, values & increments
     */
    public void layoutScrollbars() {
        boolean hAdjNeeded = isAdjNeeded(hAdj);
        boolean vAdjNeeded = isAdjNeeded(vAdj);
        Insets insets = scrollable.getInsets();
        Dimension size = scrollable.getSize();

        setAdjBounds(hAdjNeeded, vAdjNeeded);

        if (hAdj != null) {
            int hSize = scrollable.getWidth() - getHrzInsets(insets);
            int hGap = insets.left + insets.right;
            int hVis = getVisFromSize(hSize, hGap);
            int newHMax = Math.max(size.width, hVis);
            scrollable.setAdjustableSizes(hAdj, hVis, 0, newHMax);
            hAdj.setBlockIncrement(getBlockIncrFromVis(hVis));
        }

        if (vAdj != null) {
            int vSize = scrollable.getHeight() - getVrtInsets(insets);
            int vGap = insets.bottom + insets.top;
            int vVis = getVisFromSize(vSize, vGap);
            int newVMax = Math.max(size.height, vVis);
            scrollable.setAdjustableSizes(vAdj, vVis, 0, newVMax);
            vAdj.setBlockIncrement(getBlockIncrFromVis(vVis));
        }
    }
    
    /**
     * Calculates & sets scrollbars bounds
     * 
     * @param hAdjNeeded true if it's necessary to layout horizontal scrollbar
     * @param vAdjNeeded true if it's necessary to layout vertical scrollbar
     */
    private void setAdjBounds(boolean hAdjNeeded, boolean vAdjNeeded) {
        Rectangle hRect = new Rectangle();
        Rectangle vRect = new Rectangle();
        int spHeight = scrollable.getHeight();
        int spWidth = scrollable.getWidth();
        Insets ins = scrollable.getInsets();

        if (hAdjNeeded) {
            hRect.height = HSCROLLBAR_HEIGHT;
            int vWidth = vAdjNeeded ? VSCROLLBAR_WIDTH : 0;
            hRect.width = spWidth - vWidth - (ins.left + ins.right);
            hRect.y = spHeight - hRect.height - ins.bottom;
            hRect.x = ins.left;
        }
        scrollable.setAdjustableBounds(hAdj, hRect);
        if (vAdjNeeded) {
            int hHeight = hAdjNeeded ? HSCROLLBAR_HEIGHT : 0;
            vRect.height = spHeight - hHeight - (ins.top + ins.bottom);
            vRect.width = VSCROLLBAR_WIDTH;
            vRect.x = spWidth - vRect.width - ins.right;
            vRect.y = ins.top;
        }
        scrollable.setAdjustableBounds(vAdj, vRect);
    }

    /**
     * Calculates block increment given visible amount of scrollbar
     * @param vis visible amount
     * @return block increment size
     */
    private int getBlockIncrFromVis(int vis) {
        return Math.max(1, vis * 9 / 10);
    }

    /**
     * Calculates visible amount given size(length) of scrollbar's
     * viewport
     * @param size viewport size
     * @param gap insets(without scrollbar area) size
     * @return visible amount
     */
    private int getVisFromSize(int size, int gap) {
        return Math.max(1, size - gap);
    }

    /**
     * Determines whether a specified adjustable should
     * be displayed using scrollbar display policy
     * @param adj scrollbar
     * @return true if scrollbar should be displayed, false otherwise
     */
    private boolean isAdjNeeded(Adjustable adj) {
        if (adj == null) {
            return false;
        }
        switch (scrollable.getAdjustableMode(adj)) {
            case Scrollable.ALWAYS:
                return true;
            case Scrollable.NEVER:
                return false;
            case Scrollable.AS_NEEDED:
                return calculateNeeded(adj);
            case Scrollable.HORIZONTAL_ONLY:
                return adj.getOrientation() == Adjustable.HORIZONTAL;
            case Scrollable.VERTICAL_ONLY:
                return adj.getOrientation() == Adjustable.VERTICAL;
        }
        return true;
    }

    /**
     * Only for Scrollable.AS_NEEDED scrollbar display policy:
     *  determines if the specified scrollbar should be visible.
     *  Scrollbar is shown only if child component can be scrolled in
     *  corresponding direction, i. e. the size of the child exceeds
     *  container size, and there's enough space for
     *  scrollbar itself.
     * @param adj scrollbar
     * @return true if scrollbar is needed, false otherwise
     */
    private boolean calculateNeeded(Adjustable adj) {
        Insets ins = scrollable.getInsets();
        Component comp = scrollable.getComponent();
        final int GAP = ins.left + ins.right;
        boolean isVertical = (adj.getOrientation() == Adjustable.VERTICAL);
        Dimension viewSize = comp.getSize();
        viewSize.width -= getHrzInsets(ins);
        viewSize.height -= getVrtInsets(ins);
        int viewWidth = viewSize.width;
        int viewHeight = viewSize.height;
        int spOtherSize = isVertical ? viewWidth : viewHeight;
        int spSize = isVertical ? viewHeight : viewWidth;

        int compSize = 0;
        int adjSize = (isVertical ? scrollable.getAdjustableWidth()
                                 : scrollable.getAdjustableHeight());
        Dimension prefSize = scrollable.getSize();
        compSize = isVertical ? prefSize.height : prefSize.width;
        return ((spSize < compSize) && (spOtherSize > adjSize + GAP));
    }
}