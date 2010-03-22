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
 * @author Anton Avtamonov
 */

package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;


public class BasicSplitPaneDivider extends Container implements PropertyChangeListener {
    protected class DividerLayout implements LayoutManager {
        public void layoutContainer(final Container c) {
            if (splitPane.isOneTouchExpandable()) {
                Insets insets = getInsets();
                int buttonSize = ONE_TOUCH_SIZE;
                int startPos = ONE_TOUCH_OFFSET;
                if (ONE_TOUCH_SIZE + ONE_TOUCH_OFFSET > dividerSize) {
                    if (ONE_TOUCH_SIZE < dividerSize) {
                        buttonSize = dividerSize;
                        startPos = 0;
                    }
                }

                if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                    leftButton.setBounds(startPos + insets.left, ONE_TOUCH_OFFSET + insets.top, buttonSize, buttonSize);
                    rightButton.setBounds(startPos + insets.left, buttonSize + 2 * ONE_TOUCH_OFFSET + insets.top, buttonSize, buttonSize);
                } else {
                    leftButton.setBounds(ONE_TOUCH_OFFSET + insets.left, startPos + insets.top, buttonSize, buttonSize);
                    rightButton.setBounds(buttonSize + 2 * ONE_TOUCH_OFFSET + insets.left, startPos + insets.top, buttonSize, buttonSize);
                }
            }
        }

        public Dimension minimumLayoutSize(final Container c) {
            return null;
        }

        public Dimension preferredLayoutSize(final Container c) {
            return null;
        }

        public void removeLayoutComponent(final Component c) {

        }

        public void addLayoutComponent(final String constraint, final Component c) {

        }
    }

    protected class DragController {
        private final int initialMousePosition;

        protected DragController(final MouseEvent e) {
            initialMousePosition = positionForMouseEvent(e);
            if (!splitPane.isContinuousLayout()) {
                prepareForDragging();
            }
        }

        protected boolean isValid() {
            return true;
        }

        protected int positionForMouseEvent(final MouseEvent e) {
            return e.getX();
        }

        protected int getNeededLocation(final int x, final int y) {
            return x;
        }

        protected void continueDrag(final int x, final int y) {
            int location = calculateDividerLocation(x, y);
            if (splitPane.isContinuousLayout()) {
                splitPane.setDividerLocation(location);
            } else {
                dragDividerTo(location);
            }
        }

        protected void continueDrag(final MouseEvent e) {
            continueDrag(e.getX(), e.getY());
        }

        protected void completeDrag(final int x, final int y) {
            if (!splitPane.isContinuousLayout()) {
                int location = calculateDividerLocation(x, y);
                finishDraggingTo(location);
            }
        }

        protected void completeDrag(final MouseEvent e) {
            completeDrag(e.getX(), e.getY());
        }


        private int calculateDividerLocation(final int x, final int y) {
            int mousePosition = getNeededLocation(x, y);
            int delta = mousePosition - initialMousePosition;
            int dividerLocation = splitPane.getDividerLocation() + delta;

            if (splitPane.getMaximumDividerLocation() < splitPane.getMinimumDividerLocation()) {
                return splitPane.getDividerLocation();
            }
            
            if (dividerLocation >= splitPane.getMinimumDividerLocation()
                && dividerLocation <= splitPane.getMaximumDividerLocation()) {

                return dividerLocation;
            } else if (dividerLocation >= splitPane.getMaximumDividerLocation()) {
                return splitPane.getMaximumDividerLocation();
            } else {
                return splitPane.getMinimumDividerLocation();
            }
        }
    }


    protected class VerticalDragController extends DragController {
        protected VerticalDragController(final MouseEvent e) {
            super(e);
        }

        protected int getNeededLocation(final int x, final int y) {
            return y;
        }

        protected int positionForMouseEvent(final MouseEvent e) {
            return e.getY();
        }
    }

    protected class MouseHandler extends MouseAdapter implements MouseMotionListener {
        public void mousePressed(final MouseEvent e) {
            if (dragger == null) {
                dragger = orientation == JSplitPane.HORIZONTAL_SPLIT ? new DragController(e)
                                                                     : new VerticalDragController(e);
            }
        }

        public void mouseReleased(final MouseEvent e) {
            if (dragger != null) {
                dragger.completeDrag(e);
            }
            dragger = null;
        }

        public void mouseDragged(final MouseEvent e) {
            if (dragger != null) {
                dragger.continueDrag(e);
            }
        }

        public void mouseMoved(final MouseEvent e) {
        }

        public void mouseEntered(final MouseEvent e) {
            setMouseOver(true);
        }

        public void mouseExited(final MouseEvent e) {
            setMouseOver(false);
        }
    }

    private class ArrowButton extends JButton {
        private final int direction;

        public ArrowButton(final int direction) {
            this.direction = direction;
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            setOpaque(false);
            setFocusable(false);
        }

        public void paint(final Graphics g) {
            paint3DArrow(g, direction, getWidth(), Color.DARK_GRAY, Color.WHITE, getModel().isArmed());
        }

        private void paint3DArrow(final Graphics g,
                final int direction, final int size,
                final Color shadow, final Color highlight,
                final boolean armed) {

            final int halfHeight = (size + 1) / 2;
            final int height = halfHeight * 2;
            final int width = halfHeight;

            final int[] heights = new int[] {0, halfHeight - 1, height - 2};
            final int[] lWidths = new int[] {width - 1, 0, width - 1};
            final int[] rWidths = new int[] {0, width - 1, 0};
            int[] px = null;
            int[] py = null;
            int[] pxHighlight = null;
            int[] pyHighlight = null;
            switch (direction) {
            case SwingConstants.NORTH:
                px = heights;
                py = lWidths;
                break;
            case SwingConstants.SOUTH:
                px = heights;
                py = rWidths;
                break;
            case SwingConstants.WEST:
            case SwingConstants.LEFT:
                px = lWidths;
                py = heights;
                break;
            case SwingConstants.EAST:
            case SwingConstants.RIGHT:
                px = rWidths;
                py = heights;
                break;
            default:
                assert false : "incorrect direction";
            }

            pxHighlight = new int[] { px[0] + 1, px[1] + 1, px[2] + 1 };
            pyHighlight = new int[] { py[0] + 1, py[1] + 1, py[2] + 1 };
            final Color oldColor = g.getColor();

            g.setColor(getBackground());
            g.fillPolygon(px, py, 3);
            g.setColor(highlight);
            g.drawPolygon(pxHighlight, pyHighlight, 3);
            g.setColor(shadow);
            g.drawPolygon(px, py, 3);
            if (armed) {
                g.setColor(shadow);
                g.fillPolygon(px, py, 3);
            }

            g.setColor(oldColor);
        }
    }

    protected static final int ONE_TOUCH_OFFSET = 2;
    protected static final int ONE_TOUCH_SIZE = 6;

    protected JButton leftButton;
    protected JButton rightButton;
    protected DragController dragger;
    protected BasicSplitPaneUI splitPaneUI;
    protected int dividerSize;
    protected Component hiddenDivider;
    protected JSplitPane splitPane;
    protected MouseHandler mouseHandler;
    protected int orientation;

    private Border border;
    private boolean mouseIsOver;


    public BasicSplitPaneDivider(final BasicSplitPaneUI ui) {
        splitPaneUI = ui;
        splitPane = ui.splitPane;
        orientation = splitPane.getOrientation();
        mouseHandler = new MouseHandler();

        setLayout(new DividerLayout());
        splitPane.addPropertyChangeListener(this);
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        updateCursor();
        
        if (Utilities.isUIResource(getBorder())) {
            setBorder(UIManager.getBorder("SplitPaneDivider.border"));
        }
    }

    public void setBasicSplitPaneUI(final BasicSplitPaneUI ui) {
        splitPaneUI = ui;
        if (ui != null) {
            splitPane = ui.splitPane;
            orientation = splitPane.getOrientation();
        } else {
            splitPane = null;
        }
    }

    public BasicSplitPaneUI getBasicSplitPaneUI() {
        return splitPaneUI;
    }

    public void setDividerSize(final int size) {
        dividerSize = size;
    }

    public int getDividerSize() {
        return dividerSize;
    }

    public void setBorder(final Border border) {
        this.border = border;
    }

    public Border getBorder() {
        return border;
    }

    public Insets getInsets() {
        return border != null ? border.getBorderInsets(this) : super.getInsets();
    }



    public boolean isMouseOver() {
        return mouseIsOver;
    }

    public Dimension getPreferredSize() {
        if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
            return new Dimension(dividerSize, 1);
        } else {
            return new Dimension(1, dividerSize);
        }

    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public void propertyChange(final PropertyChangeEvent e) {;
        if (JSplitPane.ORIENTATION_PROPERTY.equals(e.getPropertyName())) {
            orientation = splitPane.getOrientation();
            updateCursor();
            updateButtons();
        } else if (JSplitPane.ONE_TOUCH_EXPANDABLE_PROPERTY.equals(e.getPropertyName())) {
            oneTouchExpandableChanged();
        } else if (StringConstants.ENABLED_PROPERTY_CHANGED.equals(e.getPropertyName())) {
            boolean isEnabled = ((Boolean)e.getNewValue()).booleanValue();
            if (isEnabled) {
                addMouseListener(mouseHandler);
                addMouseMotionListener(mouseHandler);
            } else {
                removeMouseListener(mouseHandler);
                removeMouseMotionListener(mouseHandler);
            }
            enableButtons(isEnabled);
        }
        invalidate();
        validate();
    }

    public void paint(final Graphics g) {
        super.paint(g);
        if (splitPane.isFocusOwner()) {
            g.setColor(UIManager.getColor("SplitPane.darkShadow"));
            g.drawRect(0, 0, getWidth() - 2, getHeight() - 2);
        }
        if (getBorder() != null) {
            getBorder().paintBorder(this, g, 0, 0, getWidth(), getHeight());
        }
    }

    protected JButton createLeftOneTouchButton() {
        JButton result = new ArrowButton(orientation == JSplitPane.HORIZONTAL_SPLIT ? SwingConstants.WEST : SwingConstants.NORTH);
        result.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                Insets insets = splitPane.getInsets();
                int oldLocation = splitPaneUI.getDividerLocation(splitPane);
                int newLocation;
                if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                    if (oldLocation == insets.left) {
                        return;
                    } else if (oldLocation == splitPane.getWidth() - insets.right - splitPane.getDividerSize()) {
                        newLocation = splitPane.getLastDividerLocation();
                    } else {
                        newLocation = insets.left;
                    }
                } else {
                    if (oldLocation == insets.top) {
                        return;
                    } else if (oldLocation == splitPane.getHeight() - insets.bottom - splitPane.getDividerSize()) {
                        newLocation = splitPane.getLastDividerLocation();
                    } else {
                        newLocation = insets.top;
                    }
                }

                splitPane.setDividerLocation(newLocation);
            }
        });

        return result;
    }

    protected JButton createRightOneTouchButton() {
        JButton result = new ArrowButton(orientation == JSplitPane.HORIZONTAL_SPLIT ? SwingConstants.EAST : SwingConstants.SOUTH);
        result.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                Insets insets = splitPane.getInsets();
                int oldLocation = splitPaneUI.getDividerLocation(splitPane);
                int newLocation;
                if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                    if (oldLocation == splitPane.getWidth() - insets.right - splitPane.getDividerSize()) {
                        return;
                    } else if (oldLocation == insets.left) {
                        newLocation = splitPane.getLastDividerLocation();
                    } else {
                        newLocation = splitPane.getWidth() - insets.right - splitPane.getDividerSize();
                    }
                } else {
                    if (oldLocation == splitPane.getHeight() - insets.bottom - splitPane.getDividerSize()) {
                        return;
                    } else if (oldLocation == insets.top) {
                        newLocation = splitPane.getLastDividerLocation();
                    } else {
                        newLocation = splitPane.getHeight() - insets.bottom - splitPane.getDividerSize();
                    }
                }

                splitPane.setDividerLocation(newLocation);
            }
        });

        return result;
    }

    protected void oneTouchExpandableChanged() {
        if (leftButton != null) {
            remove(leftButton);
        }
        if (rightButton != null) {
            remove(rightButton);
        }
        if (splitPane.isOneTouchExpandable()) {
            leftButton = createLeftOneTouchButton();
            rightButton = createRightOneTouchButton();
            add(leftButton);
            add(rightButton);
        }
    }

    protected void prepareForDragging() {
        splitPaneUI.startDragging();
    }

    protected void dragDividerTo(final int location) {
        splitPaneUI.dragDividerTo(location);
    }

    protected void finishDraggingTo(final int location) {
        splitPaneUI.finishDraggingTo(location);
    }

    protected void setMouseOver(final boolean mouseOver) {
        mouseIsOver = mouseOver;
    }

    private void updateCursor() {
        if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
            setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
        } else {
            setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
        }
    }

    private void updateButtons() {
        if (leftButton != null) {
            remove(leftButton);
            leftButton = createLeftOneTouchButton();
            add(leftButton);
        }
        if (rightButton != null) {
            remove(rightButton);
            rightButton = createRightOneTouchButton();
            add(rightButton);
        }
    }

    private void enableButtons(final boolean enable) {
        if (leftButton != null) {
            leftButton.setEnabled(enable);
        }
        if (rightButton != null) {
            rightButton.setEnabled(enable);
        }
    }
}
