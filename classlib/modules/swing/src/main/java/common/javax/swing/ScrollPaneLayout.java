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
 * @author Sergey Burlak
 */

package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.io.Serializable;

import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class ScrollPaneLayout implements Serializable, LayoutManager, ScrollPaneConstants {

    public static class UIResource extends ScrollPaneLayout implements javax.swing.plaf.UIResource {
    }

    protected JViewport viewport;
    protected JScrollBar vsb;
    protected JScrollBar hsb;
    protected JViewport rowHead;
    protected JViewport colHead;
    protected Component lowerLeft;
    protected Component lowerRight;
    protected Component upperLeft;
    protected Component upperRight;
    protected int vsbPolicy = VERTICAL_SCROLLBAR_AS_NEEDED;
    protected int hsbPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED;

    protected Component addSingletonComponent(final Component oldC, final Component newC) {
        if (oldC == null) {
            return newC;
        }

        Container parent = oldC.getParent();
        if (parent != null) {
            parent.remove(oldC);
        }

        return newC;
    }

    public void addLayoutComponent(final String s, final Component c) {
        if (VIEWPORT.equals(s)) {
            viewport = (JViewport)addSingletonComponent(viewport, c);
        } else if (VERTICAL_SCROLLBAR.equals(s)) {
            vsb = (JScrollBar)addSingletonComponent(vsb, c);
        } else if (HORIZONTAL_SCROLLBAR.equals(s)) {
            hsb = (JScrollBar)addSingletonComponent(hsb, c);
        } else if (ROW_HEADER.equals(s)) {
            rowHead = (JViewport)addSingletonComponent(rowHead, c);
        } else if (COLUMN_HEADER.equals(s)) {
            colHead = (JViewport)addSingletonComponent(colHead, c);
        } else if (LOWER_LEFT_CORNER.equals(s)) {
            lowerLeft = addSingletonComponent(lowerLeft, c);
        } else if (LOWER_RIGHT_CORNER.equals(s)) {
            lowerRight = addSingletonComponent(lowerRight, c);
        } else if (UPPER_LEFT_CORNER.equals(s)) {
            upperLeft = addSingletonComponent(upperLeft, c);
        } else if (UPPER_RIGHT_CORNER.equals(s)) {
            upperRight = addSingletonComponent(upperRight, c);
        }
    }

    public void removeLayoutComponent(final Component c) {
        if (viewport == c) {
            viewport = null;
        }
        if (vsb == c) {
            vsb = null;
        }
        if (hsb == c) {
            hsb = null;
        }
        if (rowHead == c) {
            rowHead = null;
        }
        if (colHead == c) {
            colHead = null;
        }
        if (lowerLeft == c) {
            lowerLeft = null;
        }
        if (lowerRight == c) {
            lowerRight = null;
        }
        if (upperLeft == c) {
            upperLeft = null;
        }
        if (upperRight == c) {
            upperRight = null;
        }
    }

    public int getVerticalScrollBarPolicy() {
        return vsbPolicy;
    }

    public void setVerticalScrollBarPolicy(final int x) {
        if (x != ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED &&
            x != ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS && 
            x != ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {
            throw new IllegalArgumentException(Messages.getString("swing.02","verticalScrollBarPolicy")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        vsbPolicy = x;
    }

    public int getHorizontalScrollBarPolicy() {
        return hsbPolicy;
    }

    public void setHorizontalScrollBarPolicy(final int x) {
        if (x != ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED &&
            x != ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER && 
            x != ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS) {
            throw new IllegalArgumentException(Messages.getString("swing.02","horizontalScrollBarPolicy")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        hsbPolicy = x;
    }

    public JViewport getViewport() {
        return viewport;
    }

    public JScrollBar getHorizontalScrollBar() {
        return hsb;
    }

    public JScrollBar getVerticalScrollBar() {
        return vsb;
    }

    public JViewport getRowHeader() {
        return rowHead;
    }

    public JViewport getColumnHeader() {
        return colHead;
    }

    public Component getCorner(final String key) {
        if (LOWER_LEFT_CORNER.equals(key)) {
            return lowerLeft;
        }
        if (UPPER_LEFT_CORNER.equals(key)) {
            return upperLeft;
        }
        if (LOWER_RIGHT_CORNER.equals(key)) {
            return lowerRight;
        }
        if (UPPER_RIGHT_CORNER.equals(key)) {
            return upperRight;
        }

        return null;
    }

    public Dimension preferredLayoutSize(final Container parent) {
        JScrollPane pane = (JScrollPane)parent;

        int rowHeadWidth = (rowHead == null) ? 0 : rowHead.getPreferredSize().width;
        int viewportWidth = (viewport == null) ? 0 : viewport.getPreferredSize().width;
        int viewportBorderLeft = (pane.getViewportBorder() == null) ? 0 : pane.getViewportBorder().getBorderInsets(pane).left;
        int viewportBorderRight = (pane.getViewportBorder() == null) ? 0 : pane.getViewportBorder().getBorderInsets(pane).right;
        int width = viewportWidth + rowHeadWidth
                    + (vsb == null ? 0 : vsb.getBounds().width)
                    + pane.getInsets().right + pane.getInsets().left
                    + viewportBorderLeft
                    + viewportBorderRight;

        int viewportHeight = (viewport == null) ? 0 : viewport.getPreferredSize().height;
        int colHeadHeight = (colHead == null) ? 0 : colHead.getPreferredSize().height;
        int viewportBorderTop = (pane.getViewportBorder() == null) ? 0 : pane.getViewportBorder().getBorderInsets(pane).top;
        int viewportBorderBottom = (pane.getViewportBorder() == null) ? 0 : pane.getViewportBorder().getBorderInsets(pane).bottom;
        int height = viewportHeight + colHeadHeight
                     + (hsb == null ? 0 : hsb.getBounds().height)
                     + pane.getInsets().top + pane.getInsets().bottom
                     + viewportBorderTop
                     + viewportBorderBottom;

        return new Dimension(width, height);
    }

    public Dimension minimumLayoutSize(final Container parent) {
        JScrollPane pane = (JScrollPane)parent;

        if (pane == null) {
            return new Dimension(0, 0);
        }

        int rowHeadWidth = (rowHead == null) ? 0 : rowHead.getMinimumSize().width;
        int viewportWidth = (viewport == null) ? 0 : viewport.getMinimumSize().width;
        int viewportBorderLeft = (pane.getViewportBorder() == null) ? 0 : pane.getViewportBorder().getBorderInsets(pane).left;
        int viewportBorderRight = (pane.getViewportBorder() == null) ? 0 : pane.getViewportBorder().getBorderInsets(pane).right;
        int vsbWidth = ((pane.getVerticalScrollBarPolicy() == VERTICAL_SCROLLBAR_NEVER) ? 0 : pane.getVerticalScrollBar().getMinimumSize().width);
        int width = viewportWidth + rowHeadWidth
                    + vsbWidth
                    + pane.getInsets().right + pane.getInsets().left
                    + viewportBorderLeft
                    + viewportBorderRight;

        int viewportHeight = (viewport == null) ? 0 : viewport.getMinimumSize().height;
        int colHeadHeight = (colHead == null) ? 0 : colHead.getMinimumSize().height;
        int viewportBorderTop = (pane.getViewportBorder() == null) ? 0 : pane.getViewportBorder().getBorderInsets(pane).top;
        int viewportBorderBottom = (pane.getViewportBorder() == null) ? 0 : pane.getViewportBorder().getBorderInsets(pane).bottom;
        int hsbHeight = ((pane.getVerticalScrollBarPolicy() == HORIZONTAL_SCROLLBAR_NEVER) ? 0 : pane.getHorizontalScrollBar().getMinimumSize().height);
        int height = viewportHeight + colHeadHeight
                     + hsbHeight
                     + pane.getInsets().top + pane.getInsets().bottom
                     + viewportBorderTop
                     + viewportBorderBottom;

        return new Dimension(width, height);
    }

    public void layoutContainer(final Container parent) {
        JScrollPane pane = (JScrollPane)parent;

        Insets scrollPaneInsets = pane.getInsets();

        boolean isVerticalSBVisible = isVerticalScrollBarVisible(pane);
        if (vsb != null) {
            vsb.setVisible(isVerticalSBVisible);
            if (!isVerticalSBVisible) {
                vsb.setBounds(0, 0, 0, 0);
            }
        }
        boolean isHorizontalSBVisible = isHorizontalScrollBarVisible(pane);
        if (hsb != null) {
            hsb.setVisible(isHorizontalSBVisible);
            if (!isHorizontalSBVisible) {
                hsb.setBounds(0, 0, 0, 0);
            }
        }

        int verticalSBWidth = isVerticalSBVisible && vsb != null ? vsb.getPreferredSize().width : 0;
        int horizontalSBHeight = isHorizontalSBVisible && hsb != null ? hsb.getPreferredSize().height : 0;

        int rowHeadWidth = (rowHead != null && rowHead.isVisible()) ? rowHead.getPreferredSize().width : 0;
        int colHeadHeight = (colHead != null && colHead.isVisible()) ? colHead.getPreferredSize().height : 0;

        int rowHeadHeight = pane.getSize().height - scrollPaneInsets.top - scrollPaneInsets.bottom
            - colHeadHeight - horizontalSBHeight;
        int colHeadWidth = pane.getSize().width - scrollPaneInsets.left - scrollPaneInsets.right
            - rowHeadWidth - verticalSBWidth;

        setRowHeadBounds(pane, rowHeadWidth, rowHeadHeight);
        setColHeadBounds(pane, verticalSBWidth, rowHeadWidth, colHeadHeight, colHeadWidth);

        setVSBBounds(pane, verticalSBWidth, horizontalSBHeight, colHeadHeight);
        setHSBBounds(pane, verticalSBWidth, horizontalSBHeight, rowHeadWidth);

        setViewportBounds(pane, verticalSBWidth, horizontalSBHeight, rowHeadWidth, colHeadHeight);

        setUpperLeftBounds(pane, horizontalSBHeight, rowHeadWidth, colHeadHeight);
        setLowerLeftBounds(pane, horizontalSBHeight, rowHeadWidth, colHeadHeight, rowHeadHeight);
        setUpperRightBounds(pane, verticalSBWidth, rowHeadWidth, colHeadHeight);
        setLowerRightBounds(pane, verticalSBWidth, horizontalSBHeight, rowHeadWidth, colHeadHeight, rowHeadHeight);
    }

    @Deprecated
    public Rectangle getViewportBorderBounds(final JScrollPane scrollpane) {
        return scrollpane.getViewportBorderBounds();
    }

    public void syncWithScrollPane(final JScrollPane sp) {
        viewport = sp.getViewport();
        rowHead = sp.getRowHeader();
        colHead = sp.getColumnHeader();
        vsb = sp.getVerticalScrollBar();
        hsb = sp.getHorizontalScrollBar();
        lowerLeft = sp.getCorner(LOWER_LEFT_CORNER);
        lowerRight = sp.getCorner(LOWER_RIGHT_CORNER);
        upperLeft = sp.getCorner(UPPER_LEFT_CORNER);
        upperRight = sp.getCorner(UPPER_RIGHT_CORNER);
        vsbPolicy = sp.getVerticalScrollBarPolicy();
        hsbPolicy = sp.getHorizontalScrollBarPolicy();
    }

    private boolean isHorizontalScrollBarVisible(final JScrollPane pane) {
        if (getHorizontalScrollBarPolicy() == HORIZONTAL_SCROLLBAR_ALWAYS) {
            return true;
        } else if (getHorizontalScrollBarPolicy() == HORIZONTAL_SCROLLBAR_NEVER || viewport.getView() == null) {
            return false;
        }
        if (viewport.getView() instanceof Scrollable && ((Scrollable)viewport.getView()).getScrollableTracksViewportWidth()) {
            return false;
        }

        Insets scrollPaneInsets = pane.getInsets();

        int rowHeadWidth = (rowHead != null && rowHead.isVisible()) ? rowHead.getPreferredSize().width : 0;
        int vsbWidth = vsb.isVisible() ? vsb.getWidth() : 0;
        int viewportWidth = pane.getSize().width - scrollPaneInsets.left - scrollPaneInsets.right
                            - rowHeadWidth - vsbWidth;

        Border viewportBorder = pane.getViewportBorder();
        if (viewportBorder != null) {
            Insets viewportBorderInsets = viewportBorder.getBorderInsets(viewport);
            viewportWidth -= viewportBorderInsets.left + viewportBorderInsets.right;
        }

        return viewport.getView().getPreferredSize().width > viewportWidth;
    }

    private boolean isVerticalScrollBarVisible(final JScrollPane pane) {
        if (getVerticalScrollBarPolicy() == VERTICAL_SCROLLBAR_ALWAYS) {
            return true;
        } else if (getVerticalScrollBarPolicy() == VERTICAL_SCROLLBAR_NEVER || viewport == null || viewport.getView() == null) {
            return false;
        }
        if (viewport.getView() instanceof Scrollable && ((Scrollable)viewport.getView()).getScrollableTracksViewportHeight()) {
            return false;
        }

        Insets scrollPaneInsets = pane.getInsets();

        int colHeadHeight = (colHead != null && colHead.isVisible()) ? colHead.getPreferredSize().height : 0;
        int hsbHeight = hsb.isVisible() ? hsb.getHeight() : 0;
        int viewportHeight = pane.getSize().height - scrollPaneInsets.top - scrollPaneInsets.bottom
                             - colHeadHeight - hsbHeight;

        Border viewportBorder = pane.getViewportBorder();
        if (viewportBorder != null) {
            Insets viewportBorderInsets = viewportBorder.getBorderInsets(viewport);
            viewportHeight -= viewportBorderInsets.top + viewportBorderInsets.bottom;
        }

        return viewport.getView().getPreferredSize().height > viewportHeight;
    }

    private void setViewportBounds(final JScrollPane pane, final int verticalSBWidth, final int horizontalSBHeight,
            final int rowHeadWidth, final int colHeadHeight) {

        Insets scrollPaneInsets = pane.getInsets();
        boolean leftToRight = pane.getComponentOrientation().isLeftToRight();
        int viewportX;
        int viewportY = scrollPaneInsets.top + colHeadHeight;
        int viewportWidth = pane.getSize().width - scrollPaneInsets.left - scrollPaneInsets.right - verticalSBWidth
                            - rowHeadWidth;
        int viewportHeight = pane.getSize().height - scrollPaneInsets.top - scrollPaneInsets.bottom - colHeadHeight
                            - horizontalSBHeight;
        if (leftToRight) {
            viewportX = scrollPaneInsets.left + rowHeadWidth;
        } else {
            viewportX = scrollPaneInsets.left + verticalSBWidth;
        }
        if (pane.getViewportBorder() != null) {
            Insets viewportBorderInsets = pane.getViewportBorder().getBorderInsets(viewport);
            viewportWidth -= viewportBorderInsets.left + viewportBorderInsets.right;
            viewportHeight -= viewportBorderInsets.top + viewportBorderInsets.bottom;
            viewportX += viewportBorderInsets.left;
            viewportY += viewportBorderInsets.top;
        }
        viewportWidth = viewportWidth > 0 ? viewportWidth : 0;
        viewportHeight = viewportHeight > 0 ? viewportHeight : 0;

        Rectangle newBounds = new Rectangle(viewportX, viewportY, viewportWidth, viewportHeight);

        if (!newBounds.equals(viewport.getBounds())) {
            viewport.setLocation(viewportX, viewportY);
            viewport.setExtentSize(new Dimension(viewportWidth, viewportHeight));
        }
    }

    private void setHSBBounds(final JScrollPane pane, final int verticalSBWidth,
            final int horizontalSBHeight, final int rowHeadWidth) {

        if (hsb == null) {
            return;
        }

        Insets scrollPaneInsets = pane.getInsets();
        boolean leftToRight = pane.getComponentOrientation().isLeftToRight();

        int hsbX;
        if (leftToRight) {
            hsbX = scrollPaneInsets.left + rowHeadWidth;
        } else {
            hsbX = scrollPaneInsets.left + verticalSBWidth;
        }
        int hsbY = pane.getSize().height - scrollPaneInsets.bottom - horizontalSBHeight;
        int horizontalSBWidth = pane.getWidth() - scrollPaneInsets.left - scrollPaneInsets.right - rowHeadWidth - verticalSBWidth;
        hsb.setBounds(hsbX, hsbY, horizontalSBWidth, horizontalSBHeight);
    }

    private void setVSBBounds(final JScrollPane pane,
            final int verticalSBWidth, final int horizontalSBHeight, final int colHeadHeight) {

        if (vsb == null) {
            return;
        }

        Insets scrollPaneInsets = pane.getInsets();
        boolean leftToRight = pane.getComponentOrientation().isLeftToRight();

        int vsbX;
        if (leftToRight) {
            vsbX = pane.getSize().width - scrollPaneInsets.right - verticalSBWidth;
        } else {
            vsbX = scrollPaneInsets.left;
        }
        int vsbY = scrollPaneInsets.top + colHeadHeight;
        int verticalSBHeight = pane.getHeight() - scrollPaneInsets.top - scrollPaneInsets.bottom
                               - horizontalSBHeight - colHeadHeight;
        vsb.setBounds(vsbX, vsbY, verticalSBWidth, verticalSBHeight);
    }

    private void setColHeadBounds(final JScrollPane pane, final int verticalSBWidth, final int rowHeadWidth,
            final int colHeadHeight, final int colHeadWidth) {

        if (colHead == null || !colHead.isVisible()) {
            return;
        }

        Insets scrollPaneInsets = pane.getInsets();
        boolean leftToRight = pane.getComponentOrientation().isLeftToRight();

        int colHeadX = (leftToRight)
                       ? scrollPaneInsets.left + rowHeadWidth
                       : scrollPaneInsets.left + verticalSBWidth;
        int colHeadY = scrollPaneInsets.top;

        colHead.setBounds(colHeadX, colHeadY, colHeadWidth, colHeadHeight);
    }

    private void setRowHeadBounds(final JScrollPane pane, final int rowHeadWidth, final int rowHeadHeight) {
        if (rowHead == null || !rowHead.isVisible()) {
            return;
        }

        Insets scrollPaneInsets = pane.getInsets();
        boolean leftToRight = pane.getComponentOrientation().isLeftToRight();

        int rowHeadX = (leftToRight)
                       ? scrollPaneInsets.left
                       : pane.getSize().width - scrollPaneInsets.right - rowHead.getPreferredSize().width;
        int rowHeadY = (colHead != null && colHead.isVisible())
                       ? scrollPaneInsets.top + colHead.getPreferredSize().height
                       : scrollPaneInsets.top;

        rowHead.setBounds(rowHeadX, rowHeadY, rowHeadWidth, rowHeadHeight);
    }

    private void setLowerRightBounds(final JScrollPane pane, final int verticalSBWidth, final int horizontalSBHeight,
            final int rowHeadWidth, final int colHeadHeight, final int rowHeadHeight) {

        Insets scrollPaneInsets = pane.getInsets();
        boolean leftToRight = pane.getComponentOrientation().isLeftToRight();

        if (lowerRight != null) {
            if (((vsb.isVisible() && leftToRight) || (rowHead != null && rowHead.isVisible() && !leftToRight))
                    && hsb.isVisible()) {
                int lowerRigthX = leftToRight ? vsb.getX() : rowHead.getX();
                int lowerRightWidth = leftToRight ? verticalSBWidth : rowHeadWidth;
                lowerRight.setBounds(lowerRigthX, scrollPaneInsets.top + colHeadHeight + rowHeadHeight,
                                     lowerRightWidth, horizontalSBHeight);
            } else {
                lowerRight.setBounds(0, 0, 0, 0);
            }
        }
    }

    private void setUpperRightBounds(final JScrollPane pane, final int verticalSBWidth, final int rowHeadWidth, final int colHeadHeight) {
        Insets scrollPaneInsets = pane.getInsets();
        boolean leftToRight = pane.getComponentOrientation().isLeftToRight();

        if (upperRight != null) {
            if (colHead != null && colHead.isVisible() && ((vsb.isVisible() && leftToRight)
                    || (rowHead != null && rowHead.isVisible() && !leftToRight))) {
                int upperRightWidth = leftToRight ? verticalSBWidth : rowHeadWidth;
                int upperRightX = leftToRight
                        ? pane.getWidth() - scrollPaneInsets.right - verticalSBWidth
                        : pane.getWidth() - scrollPaneInsets.right - rowHeadWidth;
                upperRight.setBounds(upperRightX, scrollPaneInsets.top,
                                     upperRightWidth, colHeadHeight);
            } else {
                upperRight.setBounds(0, 0, 0, 0);
            }
        }
    }

    private void setLowerLeftBounds(final JScrollPane pane, final int horizontalSBHeight, final int rowHeadWidth, final int colHeadHeight, final int rowHeadHeight) {
        Insets scrollPaneInsets = pane.getInsets();
        boolean leftToRight = pane.getComponentOrientation().isLeftToRight();

        if (lowerLeft != null) {
            if (((rowHead != null && rowHead.isVisible() && leftToRight) || (vsb.isVisible() && !leftToRight))
                    && hsb.isVisible()) {
                int lowerLeftWidth = leftToRight ? rowHeadWidth : horizontalSBHeight;
                lowerLeft.setBounds(scrollPaneInsets.left, scrollPaneInsets.top + colHeadHeight + rowHeadHeight,
                                    lowerLeftWidth, horizontalSBHeight);
            } else {
                lowerLeft.setBounds(0, 0, 0, 0);
            }
        }
    }

    private void setUpperLeftBounds(final JScrollPane pane, final int horizontalSBHeight, final int rowHeadWidth, final int colHeadHeight) {
        Insets scrollPaneInsets = pane.getInsets();
        boolean leftToRight = pane.getComponentOrientation().isLeftToRight();

        if (upperLeft != null) {
            if (colHead != null && colHead.isVisible()
                    && ((rowHead != null && rowHead.isVisible() && leftToRight) || (vsb.isVisible() && !leftToRight))) {
                int upperLeftWidth = leftToRight ? rowHeadWidth : horizontalSBHeight;
                upperLeft.setBounds(scrollPaneInsets.left, scrollPaneInsets.top, upperLeftWidth, colHeadHeight);
            } else {
                upperLeft.setBounds(0, 0, 0, 0);
            }
        }
    }
}
