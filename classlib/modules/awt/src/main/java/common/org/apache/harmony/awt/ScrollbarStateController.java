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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.state.ScrollbarState;


/**
 * ScrollbarStateController.
 * Implements Scrollbar behavior. Responds to
 * input, focus, component events and updates
 * scrollbar state. Repaints scrollbar when necessary,
 * fires [adjustment] events.
 */
public abstract class ScrollbarStateController implements MouseListener,
        MouseMotionListener, FocusListener, KeyListener, ComponentListener {

    private static final long DELAY = 100l; //animation delay in ms
    // delay between mouse press event and scroll start:
    private static final long START_DELAY = 250l;

    private final ScrollbarState state;
    private final SimpleButton incr, decr;
    private final Slider slider;
    private boolean pressed;
    private boolean focused;
    
    /**
     * store last scroll type(see AdjustmentEvent)  to be able
     * to get it when firing adjustment event 
     */
    private int type = -1;
    
    /**
     * Which part of scrollbar track is highlighted
     * while block scrolling by mouse press on track
     * @see ScrollbarState
     */
    private int highlight = ScrollbarState.NO_HIGHLIGHT;
    
    private ScrollTask unitTask;
    private BlockScrollTask blockTask;
    int curMouseX, curMouseY; //for dragging
    private Point dragPoint; //for dragging

    public ScrollbarStateController(ScrollbarState state) {
        this.state = state;
        incr = new SimpleButton(state.getIncreaseRect());
        decr = new SimpleButton(state.getDecreaseRect());
        slider = new Slider(state.getSliderRect());
        unitTask = new ScrollTask(1);
        blockTask = new BlockScrollTask(1);
    }

    /**
     * Scrollbar Arrow button behavior:
     * handles mouse events 
     */
    class SimpleButton {
        Rectangle rect;
        boolean pressed;
        boolean mouseInside;

        SimpleButton(Rectangle r) {
            rect = r;
        }

        boolean mousePressed(Point pt) {
            if (rect == null) {
                return false;
            }

            mouseInside = rect.contains(pt);
            if (mouseInside) {
                pressed = true;
                repaint();
            }

            return mouseInside;
        }

        boolean mouseReleased(Point pt) {
            if (rect == null) {
                return false;
            }

            // there's no mouse capture, so
            // have to handle releases manually:
            boolean wasPressed = pressed;
            if (wasPressed) {
                pressed = false;
                repaint();
            }

            mouseInside = rect.contains(pt);
            return wasPressed && mouseInside;
        }

        boolean mouseDragged(Point pt) {

            boolean oldInside = mouseInside;
            mouseInside = rect.contains(pt);
            if (pressed && (oldInside != mouseInside)) {
                repaint();
            }
            return mouseInside;
        }

        void repaint() {
            ScrollbarStateController.this.repaint(rect);
        }

        boolean isPressed() {
            return (pressed && mouseInside);
        }

    }

    /**
     * Scrollbar Thumb[scroll box] behavior:
     * handles mouse drags
     */
    class Slider extends SimpleButton {
        boolean dragged;

        Slider(Rectangle r) {
            super(r);
        }

        boolean isDragged() {
            return dragged;
        }

        @Override
        boolean mouseReleased(Point pt) {
           boolean result = super.mouseReleased(pt);

            if (dragged) {
                dragged = false;
            }
            return result;
        }

        @Override
        boolean mouseDragged(Point pt) {
            boolean result = super.mouseDragged(pt);

            if (pressed) {
                dragged = true;
                setValueOnDragging(pt);
                curMouseX = pt.x;
                curMouseY = pt.y;
            }
            return result;
        }
    }
    
    /**
     * Timer task which scrolls by unit
     * on every tick.
     */
    class ScrollTask implements Runnable {

        /**
         * scroll direction(up or down)
         */
        int dir;
        
        /**
         * overall running time
         */
        long runTime;
        
        private PeriodicTimer timer;
        
        /**
         * Delay before scroll starts
         */
        private long startDelay;
        
        ScrollTask(int dir) {
            setDir(dir);
            timer = new PeriodicTimer(DELAY, this);
        }

        public void run() {
            runTime += timer.getPeriod();
            if (runTime < startDelay) {
                return;
            }
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    scroll();
                }

            });
        }

        void setDir(int dir) {
            this.dir = dir;
        }

        void scroll() {
            scrollByUnit(dir);
        }

        PeriodicTimer getTimer() {
            return timer;
        }

        void start(int dir, long delay) {
            setDir(dir);
            startDelay = delay;
            runTime = 0;
            timer.start();

        }
    }

    /**
     * Timer task which scrolls by block
     * on every tick.
     */
    class BlockScrollTask extends ScrollTask {

        BlockScrollTask(int dir) {
            super(dir);
        }

        @Override
        void scroll() {
            int dir = getBlockDir(new Point(curMouseX, curMouseY));
            if (this.dir != dir) {
                getTimer().stop();
                return;
            }
            scrollByBlock(dir);
        }
    }

    public void mouseClicked(MouseEvent e) {
        // ignored

    }

    public void mouseEntered(MouseEvent e) {
        // ignored

    }

    public void mouseExited(MouseEvent e) {
        // ignored

    }

    public void mousePressed(MouseEvent me) {
        if (me.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        if (!focused) {
            requestFocus();
        }
        Point pt = getPoint(me);
        curMouseX = pt.x;
        curMouseY = pt.y;
        boolean incrPressed = incr.mousePressed(pt);
        boolean decrPressed = decr.mousePressed(pt);
        slider.mousePressed(pt);
        if (incrPressed || decrPressed) {
            int dir = (incrPressed ? 1 : -1);
            scrollByUnit(dir);
            // start timer
            unitTask.start(dir, START_DELAY);
        } else {
            pressed = true;
            int dir = getBlockDir(pt);
            if (dir != 0) {
                scrollByBlock(dir);
                blockTask.start(dir, START_DELAY);
            } else {
                Point thumbLoc = state.getSliderRect().getLocation();
                dragPoint = new Point(pt.x - thumbLoc.x, pt.y - thumbLoc.y);
            }
            repaint();
        }

    }

    /**
     * Gets direction of block scroll
     * started by mouse press on scrollbar
     * track
     * @param pt point where mouse was pressed
     * in scrollbar coordinates
     * @return 1 for scroll down/right, -1 for
     * up/left, 0 - no scroll
     */
    private int getBlockDir(Point pt) {
        Rectangle sliderRect = state.getSliderRect();
        if ((sliderRect == null) || sliderRect.isEmpty()) {
            return 0;
        }
        Rectangle upperRect = state.getUpperTrackBounds();
        Rectangle lowerRect = state.getLowerTrackBounds();

        if (lowerRect.contains(pt)) {
            highlight = ScrollbarState.INCREASE_HIGHLIGHT;
            return 1;
        } else if (upperRect.contains(pt)) {
            highlight = ScrollbarState.DECREASE_HIGHLIGHT;
            return -1;
        } else {
            highlight = ScrollbarState.NO_HIGHLIGHT;
            type = AdjustmentEvent.TRACK;
            repaint(state.getTrackBounds());
        }
        return 0;
    }

    public void mouseReleased(MouseEvent me) {
        if (me.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        Point pt = getPoint(me);
        boolean incrReleased = incr.mouseReleased(pt);
        boolean decrReleased = decr.mouseReleased(pt);
        slider.mouseReleased(pt);
        if (!(decrReleased || incrReleased)){
            pressed = false;
            type = -1;
            highlight = ScrollbarState.NO_HIGHLIGHT;
            repaint();
        }
        // stop timer
        blockTask.getTimer().stop();
        unitTask.getTimer().stop();

    }

    public void mouseDragged(MouseEvent e) {
        Point pt = getPoint(e);
        switchTimerOnDrag(pt, incr, 1);
        switchTimerOnDrag(pt, decr, -1);
        slider.mouseDragged(pt);
    }

    /**
     * Translates component coordinates into
     * scrollbar coordinates
     * @param e coordinates of this mouse event are translated
     * @return point in scrollbar coordinates
     */
    private Point getPoint(MouseEvent e) {
        Point pt = e.getPoint().getLocation();
        Point loc = state.getLocation();
        pt.translate(-loc.x, -loc.y);
        return pt;
    }

    /**
     * Starts/stops unit scrolling on mouse drag
     * in/out of arrow button
     * @param pt current mouse position
     * @param b arrow button
     * @param dir scroll direction
     */
    void switchTimerOnDrag(Point pt, SimpleButton b, int dir) {
        boolean wasPressed = b.isPressed();
        b.mouseDragged(pt);
        boolean isPressed = b.isPressed();
        if (isPressed != wasPressed) {
            if (isPressed) {
                unitTask.start(dir, 0l);
            } else {
                unitTask.getTimer().stop();
            }
        }
    }

    protected abstract void repaint(Rectangle r);

    protected abstract void repaint();

    protected abstract void requestFocus();

    public void mouseMoved(MouseEvent e) {
        // ignored

    }

    public void focusGained(FocusEvent e) {
        focused = true;
        repaint();

    }

    public void focusLost(FocusEvent e) {
        focused = false;
        repaint();
    }

    public void keyPressed(KeyEvent e) {
        // awt.54=Key event for unfocused component
        assert focused : Messages.getString("awt.54"); //$NON-NLS-1$
        int keyCode = e.getKeyCode();
        switch (keyCode) {
        case KeyEvent.VK_UP:
        case KeyEvent.VK_LEFT:
            scrollByUnit(-1);
            break;
        case KeyEvent.VK_DOWN:
        case KeyEvent.VK_RIGHT:
            scrollByUnit(1);
            break;
        case KeyEvent.VK_PAGE_UP:
            scrollByBlock(-1);
            break;
        case KeyEvent.VK_PAGE_DOWN:
            scrollByBlock(1);
            break;
        case KeyEvent.VK_HOME:
            setAdjMinMaxValue(false);
            break;
        case KeyEvent.VK_END:
            setAdjMinMaxValue(true);
            break;

        }
    }

    public void keyReleased(KeyEvent e) {
        // ignored
    }

    public void keyTyped(KeyEvent e) {
        // ignored
    }

    public void componentHidden(ComponentEvent e) {
        // ignored

    }

    public void componentMoved(ComponentEvent e) {
        // ignored

    }

    public void componentResized(ComponentEvent e) {
        // call repaint() to calculate internal layout
        repaint();
    }

    public void componentShown(ComponentEvent e) {
        // ignored

    }
    public boolean isIncreasePressed() {
        return incr.isPressed();
    }

    public boolean isDecreasePressed() {
        return decr.isPressed();
    }

    public boolean isSliderDragged() {
        return slider.isDragged();
    }

    protected abstract void fireEvent();

    public final boolean isPressed() {
        return pressed;
    }

    public final int getType() {
        return type;
    }

    /**
     * Sets scrollbar value on
     * thumb dragging by mouse.
     * Repaints scrollbar & fires events
     * when necessary.
     * @param pt current mouse position
     */
    public void setValueOnDragging(final Point pt) {

        int extent = state.getSliderSize();
        int viewSize = state.getScrollSize();
        int availableScrollingSize = viewSize - extent;
        int thumbSize = getThumbSize();
        int availableTrackSize = state.getTrackSize() - thumbSize;
        Rectangle trackRect = state.getTrackBounds();
        int offset = getOffset(pt, trackRect.x + dragPoint.x,
                               trackRect.y + dragPoint.y);

        float fVal = 0.0f;
        if (availableTrackSize != 0) {
            fVal = (float)offset * availableScrollingSize / availableTrackSize;
        }
        int val = Math.round(fVal);
        boolean repaint = false;
        if (offset != 0) {

            Rectangle oldRect = (Rectangle) state.getSliderRect().clone();
            Rectangle r = new Rectangle(oldRect);

            if (state.isVertical()) {
                r.y = pt.y - dragPoint.y;
            } else {
                r.x = pt.x - dragPoint.x;
            }
            if (trackRect.contains(r)) {
                state.setSliderRect(r);
                Rectangle paintRect = r.union(oldRect);
                repaint(paintRect);
            } else {
                repaint = true;
            }
        }

        Adjustable adj = state.getAdjustable();
        int oldVal = adj.getValue();
        state.setValue(AdjustmentEvent.TRACK, val);
        if (oldVal != adj.getValue()) {
            fireEvent();
            if (repaint) {
                repaint();
            }
        }

    }

    /**
     * @return width/height of horizontal/vertical
     * scrollbar thumb
     */
    private int getThumbSize() {
        Dimension sliderSize = state.getSliderRect().getSize();
        return (state.isVertical() ? sliderSize.height : sliderSize.width);
    }

    /**
     * Gets distance in points between specified point
     * and current mouse position
     * @param pt new position coordinates
     * @param currentMouseX current mouse x coordinate
     * @param currentMouseY current mouse y coordinate
     * @return distance in pixels from pt to (currentMouseX, currentMouseY)
     */
    int getOffset(final Point pt, final int currentMouseX,
                  final int currentMouseY) {
        boolean vertical = state.isVertical();
        int newPos = vertical ? pt.y : pt.x;
        int oldPos = vertical ? currentMouseY : currentMouseX;
        int offset = newPos - oldPos;
        if (!vertical && !state.getComponentOrientation().isLeftToRight()) {
            offset = -offset;
        }
        return offset;
    }

    /**
     * Changes scrollbar value on increment.
     * Fires adjustment event on value change.
     * @param increment change amount
     */
    private void updateAdjValue(final int increment) {
        Adjustable adj = state.getAdjustable();
        int oldVal = adj.getValue();
        state.setValue(type, oldVal + increment);
        if (oldVal != adj.getValue()){
            fireEvent();
        }
    }

    /**
     * Scrolls by block up(left) if dir is &lt; 0
     * or down(right) if dir is &gt; 0.
     * @param dir direction
     */
    void scrollByBlock(final int dir) {
        type = (dir > 0 ? AdjustmentEvent.BLOCK_INCREMENT
                       : AdjustmentEvent.BLOCK_DECREMENT);
        updateAdjValue(state.getAdjustable().getBlockIncrement() * dir);
    }
    
    /**
     * Scrolls by unit up(left) if dir is &lt; 0
     * or down(right) if dir is &gt; 0.
     * @param dir direction
     */
    void scrollByUnit(final int dir) {
        type = (dir > 0 ? AdjustmentEvent.UNIT_INCREMENT
                        : AdjustmentEvent.UNIT_DECREMENT);
        updateAdjValue(state.getAdjustable().getUnitIncrement() * dir);
    }

    /**
     * Scrolls to scrollbar maximum or minimum value
     * @param max scroll to maximum if true, scroll to minimum
     * otherwise
     */
    void setAdjMinMaxValue(boolean max) {
        Adjustable adj = state.getAdjustable();
        int newVal = max ? adj.getMaximum() : adj.getMinimum();
        int oldVal = adj.getValue();
        adj.setValue(newVal);
        if (oldVal != newVal) {
            type = AdjustmentEvent.TRACK;
            fireEvent();
        }
    }

    public int getHighlight() {
        return highlight;
    }
}
