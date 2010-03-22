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
 * @author Vadim L. Bogdanov
 */

package org.apache.harmony.x.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * This class helps to implement dragging/resising of components
 * with a mouse.
 * For example, it is used in <code>BasicInternalFrameUI.BorderListener</code>.
 *
 */
public class ComponentDragImplHelper implements SwingConstants {
    /**
     * This field is used in resizing. It shows which area is considered
     * as a corner and resizement is made in diagonal direction.
     */
    // Note: the value is hardcoded;
    // the same value is hardcoded in MetalBorders.InternalFrameBorder
    public static final int FRAME_CORNER_SIZE = 15;

    /**
     * This value shows that there is no resizement, only dragging.
     */
    public static final int RESIZE_NONE = 0;

    /*
     * Maps directions from Rectangle to directions from SwingConstants.
     */
    private static final int[] decodeResizeDirTable = {
        0,                                          RESIZE_NONE,
        Rectangle.OUT_BOTTOM,                       SOUTH,
        Rectangle.OUT_RIGHT,                        EAST,
        Rectangle.OUT_LEFT,                         WEST,
        Rectangle.OUT_TOP,                          NORTH,
        Rectangle.OUT_TOP | Rectangle.OUT_LEFT,     NORTH_WEST,
        Rectangle.OUT_TOP | Rectangle.OUT_RIGHT,    NORTH_EAST,
        Rectangle.OUT_BOTTOM | Rectangle.OUT_LEFT,  SOUTH_WEST,
        Rectangle.OUT_BOTTOM | Rectangle.OUT_RIGHT, SOUTH_EAST
    };

    /*
     * Maps directions to cursor types.
     */
    private static final int[] ResizeDirToCursorTypeTable = {
        RESIZE_NONE, Cursor.DEFAULT_CURSOR,
        SOUTH,       Cursor.S_RESIZE_CURSOR,
        EAST,        Cursor.E_RESIZE_CURSOR,
        WEST,        Cursor.W_RESIZE_CURSOR,
        NORTH,       Cursor.N_RESIZE_CURSOR,
        NORTH_WEST,  Cursor.NW_RESIZE_CURSOR,
        NORTH_EAST,  Cursor.NE_RESIZE_CURSOR,
        SOUTH_WEST,  Cursor.SW_RESIZE_CURSOR,
        SOUTH_EAST,  Cursor.SE_RESIZE_CURSOR
    };

    /*
     * The coordinates where the mouse was pressed.
     */
    private Point start;

    /*
     * The component to be resized/moved.
     */
    private Component comp;

    /*
     * The parent of the resized/moved component. It can be null if
     * the component is a window.
     */
    private Container parent;

    /*
     * The size of the parent.
     */
    private Dimension parentSize;

    /*
     * Becomes true if the component is being dragged or being resized.
     */
    private boolean dragging;

    /*
     * The rectangle of available mouse dragging.
     */
    private Rectangle dragArea = new Rectangle();

    /*
     * The bounds of the frame prior to resize/drag.
     */
    private Rectangle oldBounds;

    /*
     * Shows the direction of resizing
     */
    private int resizeDirection = RESIZE_NONE;

    /**
     * Starts dragging operation. This function is usually called from
     * {@link java.awt.event.MouseListener#mousePressed(MouseEvent)} function.
     *
     * @param e the mouse event
     * @param comp the component to be dragged
     * @param parent the parent of the component being dragged
     */
    public final void beginDragging(final MouseEvent e,
                                    final JComponent comp,
                                    final Container parent) {
        beginOperationImpl(e, comp, parent, RESIZE_NONE);
    }

    /**
     * Starts resizing operation. This function is usually called from
     * {@link java.awt.event.MouseListener#mousePressed(MouseEvent)} function.
     *
     * @param e the mouse event
     * @param comp the component to be resized
     * @param parent the parent of the component being resized
     */
    public final void beginResizing(final MouseEvent e,
                                    final JComponent comp,
                                    final Container parent) {
        beginOperationImpl(e, comp, parent, getResizeDirection(e, comp));
    }

    private void beginOperationImpl(final MouseEvent e,
                                    final Component comp,
                                    final Container parent,
                                    final int resizeDirection) {
        this.comp = comp;
        this.parent = parent;
        parentSize = getParentSize(parent);
        oldBounds = comp.getBounds(oldBounds);
        start = getPointInParentCoordinates(e, false);
        this.resizeDirection = resizeDirection;
        setDragArea(e);
        dragging = true;
    }

    /**
     * Starts dragging or resizing operation.  This function is usually
     * called from {@link java.awt.event.MouseListener#mousePressed(MouseEvent)}
     * function.
     *
     * @param e the mouse event
     * @param window the window to be resized/moved
     * @param rootPane the <code>rootPane</code> of the window
     */
    public final void mousePressed(final MouseEvent e,
            final Window window,
            final JRootPane rootPane) {
        beginOperationImpl(e, window, null, getResizeDirection(e, rootPane));
    }

    /**
     * This function is called when the mouse is dragged during
     * resizing/moving operation. It receives the <code>MOUSE_DRAGGED</code>
     * event and returns the new bounds of the component being dragged/resized.
     *
     * @param e the <code>MOUSE_DRAGGED</code> event
     *
     * @return the new bounds of the component being dragged/resized
     */
    public final Rectangle mouseDragged(final MouseEvent e) {
        if (resizeDirection == RESIZE_NONE) {
            Point p = keepPointInsideOfDragArea(e);
            p.translate(-start.x, -start.y);
            Rectangle r = oldBounds.getBounds();
            r.translate(p.x, p.y);
            return r;
        }

        return getNewBounds(e);
    }

    /**
     * Ends resizing or dragging operation. This function is usually called from
     * {@link java.awt.event.MouseListener#mouseReleased(MouseEvent)} function.
     *
     * @param e the mouse event
     */
    public final void endDraggingOrResizing(final MouseEvent e) {
        dragging = false;
    }

    /**
     * Shows if the component is currently being dragged/resized.
     *
     * @return <code>true</code> if the component is being dragged/resized;
     *         otherwise <code>false</code> is returned;
     */
    public final boolean isDragging() {
        return dragging;
    }

    private Point getPointInParentCoordinates(final MouseEvent e,
                                              final boolean useMouseInfo) {
        if (parent != null) {
            return SwingUtilities.convertPoint(
                    e.getComponent(), e.getPoint(), parent);
        }

        Point p = useMouseInfo
                ? Utilities.getMousePointerScreenLocation()
                : null;
        if (p == null) {
            p = e.getPoint();
            SwingUtilities.convertPointToScreen(p, e.getComponent());
        }

        return p;
    }

    private Dimension getParentSize(final Component parent) {
        return parent != null
            ? parent.getSize()
            : new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    /*
     * Calculates and sets dragArea (when starting the movement/resizement).
     * "draggingRect" determines bounds in which the mouse cursor can be
     * moved. It depends on desktop pane size (the mouse cursor cannot
     * leave it), internal frame's location and its minimum size
     * (the size of the internal frame cannot be less than its minimum
     * size).
     */
    private void setDragArea(final MouseEvent e) {
        if (resizeDirection == RESIZE_NONE) {
            if (parent != null) {
                Insets insets = parent.getInsets();
                parent.getBounds(dragArea);
                dragArea.setRect(insets.left, insets.top,
                                 dragArea.width - insets.right - insets.left,
                                 dragArea.height - insets.top - insets.bottom);
            } else {
                dragArea.setBounds(0, 0, parentSize.width, parentSize.height);
            }
            return;
        }

        Dimension minSize = comp.getMinimumSize();
        int dX = comp.getWidth() - minSize.width;
        int dY = comp.getHeight() - minSize.height;

        // the result is the minimum rectangle that contains p1, p2
        Point p1 = e.getPoint();
        Point p2 = start.getLocation();

        switch (resizeDirection) {
        case NORTH_WEST:
        case NORTH:
        case WEST:
            p2.translate(dX, dY);
            break;

        case NORTH_EAST:
            p1.translate(parentSize.width - comp.getWidth(), 0);
            p2.translate(-dX, dY);
            break;

        case SOUTH_EAST:
        case SOUTH:
        case EAST:
            p1.translate(parentSize.width - comp.getWidth(),
                         parentSize.height - comp.getHeight());
            p2.translate(-dX, -dY);
            break;

        default: // SOUTH_WEST
            p1.translate(0, parentSize.height - comp.getHeight());
            p2.translate(dX, -dY);
        }

        // construct the minumim rectangle that contains both p1, p2
        dragArea.setLocation(p1);
        dragArea.setSize(0, 0);
        dragArea.add(p2);
    }

    /*
     * This method is used when resizing the frame.
     * Calculates the new size of the frame.
     */
    private Rectangle getNewBounds(final MouseEvent e) {
        Rectangle bounds = new Rectangle();

        Point p = keepPointInsideOfDragArea(e);

        int x = (p.x - start.x);
        int y = (p.y - start.y);

        int dX = 0;
        int dY = 0;
        int dWidth = 0;
        int dHeight = 0;

        switch (resizeDirection) {
        case NORTH:
        case NORTH_EAST:
        case NORTH_WEST:
            dY = y;
            dHeight = -y;
            break;
        }

        switch (resizeDirection) {
        case SOUTH:
        case SOUTH_EAST:
        case SOUTH_WEST:
            dHeight = y;
            break;
        }

        switch (resizeDirection) {
        case EAST:
        case SOUTH_EAST:
        case NORTH_EAST:
            dWidth = x;
            break;
        }

        switch (resizeDirection) {
        case WEST:
        case SOUTH_WEST:
        case NORTH_WEST:
            dX = x;
            dWidth = -x;
            break;
        }

        bounds.setLocation(oldBounds.x + dX, oldBounds.y + dY);
        bounds.width = oldBounds.width + dWidth;
        bounds.height = oldBounds.height + dHeight;

        return bounds;
    }

    private Point keepPointInsideOfDragArea(final MouseEvent e) {
        Point p = getPointInParentCoordinates(e, true);
        p.x = Math.max(dragArea.x, p.x);
        p.y = Math.max(dragArea.y, p.y);
        p.x = Math.min(dragArea.x + dragArea.width, p.x);
        p.y = Math.min(dragArea.y + dragArea.height, p.y);
        return p;
    }

    /*
     * Finds the key in the array that consists of pairs: key, value.
     * If the key is found, returns the corresponding value; returns
     * 0 if the key is not found.
     */
    private static int findValueInArray(final int[] array, final int key) {
        for (int i = 0; i < array.length; i += 2) {
            if (array[i] == key) {
                return array[i + 1];
            }
        }
        return -1;
    }

    /*
     * Decodes the direction from Rectangle direction to
     * one of the SwingConstants direction constants.
     */
    private static int decodeResizeDirection(final int rawDirection) {
        int result = findValueInArray(decodeResizeDirTable, rawDirection);
        if (result == -1) {
            assert false : "invalid direction";
            return RESIZE_NONE;
        }

        return result;
    }

    /**
     * @param e the mouse event
     * @param comp the resizing component
     *
     * @return the direction of the resizement.
     */
    public static final int getResizeDirection(final MouseEvent e,
            final JComponent comp) {
        Point p = SwingUtilities.convertPoint(
                (Component) e.getSource(), e.getPoint(), comp);

        Rectangle inner = SwingUtilities.calculateInnerArea(comp, null);
        if (inner.contains(p)) {
            // the point is inside of the frame and
            // the border doesn't contain it
            return RESIZE_NONE;
        }

        if (!SwingUtilities.getLocalBounds(comp).contains(p)) {
            // the point is outside of the frame;
            // this can occur when the mouse button is released
            return RESIZE_NONE;
        }

        inner.grow(-FRAME_CORNER_SIZE, -FRAME_CORNER_SIZE);
        if (inner.width < 0) {
            inner.width = 1;
        }
        if (inner.height < 0) {
            inner.height = 1;
        }
        return decodeResizeDirection(inner.outcode(p));
    }

    /**
     * This function is used to determine the cursor that shows the resize
     * direction on the border of the component.
     *
     * @param e the mouse event
     * @param comp the component
     */
    public static final Cursor getUpdatedCursor(final MouseEvent e,
            final JComponent comp) {
        int cursorType = findValueInArray(ResizeDirToCursorTypeTable,
                                          getResizeDirection(e, comp));

        if (cursorType == -1) {
            assert false : "invalid direction";
            cursorType = Cursor.DEFAULT_CURSOR;
        }

        return Cursor.getPredefinedCursor(cursorType);
    }
}
