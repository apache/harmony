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

package java.awt;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import org.apache.harmony.awt.ChoiceStyle;
import org.apache.harmony.awt.PeriodicTimer;
import org.apache.harmony.awt.ScrollStateController;
import org.apache.harmony.awt.Scrollable;
import org.apache.harmony.awt.ScrollbarStateController;
import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.state.ListState;

/**
 * Helper class: popup window containing list of
 * Choice items, implements popup and scrolling list behavior
 * of Choice component
 */
class ChoicePopupBox extends PopupBox {
    private final Choice choice;

    /**
     * how many items to scroll on PgUp/PgDn
     */
    final static int PAGE_SIZE = 8;

    /**
     * how many ticks to skip when scroll speed is minimum
     */
    final static int MAX_SKIP = 12;

    int selectedItem;

    int focusedItem;

    /**
     * fields for dragging/scrolling behavior implementation
     */
    private int scrollLocation;

    transient ScrollPaneAdjustable vAdj;

    Scrollable scrollable;

    ScrollStateController scrollController;

    ScrollbarStateController scrollbarController;

    boolean scroll;

    private boolean scrollDragged;

    /**
     * fields for scrolling animation
     */
    private final PeriodicTimer dragScrollTimer;

    private int dragScrollSpeed;

    private Runnable dragScrollHandler;

    private int nSkip; // scroll every nSkip ticks

    private final ChoiceListState listState;

    private final ChoiceStyle style;

    /**
     * List state implementation
     */
    class ChoiceListState extends Choice.ComponentState implements ListState {
        ChoiceListState() {
            choice.super();
        }

        public Rectangle getItemBounds(int idx) {
            return ChoicePopupBox.this.getItemBounds(idx);
        }

        public Rectangle getClient() {
            Rectangle clientRect = ChoicePopupBox.this.getBounds();
            if (scroll) {
                clientRect.width -= scrollable.getAdjustableWidth();
            }
            clientRect.grow(-1, -1);
            return clientRect;
        }

        public int getItemCount() {
            return choice.getItemCount();
        }

        public boolean isSelected(int idx) {
            return (selectedItem == idx);
        }

        public String getItem(int idx) {
            return choice.getItem(idx);
        }

        public int getCurrentIndex() {
            return focusedItem;
        }

        @Override
        public Dimension getSize() {
            return ChoicePopupBox.this.getSize();
        }
    }

    /**
     * Scrolling behavior implementation
     */
    class ChoiceScrollable implements Scrollable {
        public Adjustable getVAdjustable() {
            return vAdj;
        }

        public Adjustable getHAdjustable() {
            return null;
        }

        public Insets getInsets() {
            return Choice.INSETS;
        }

        public Point getLocation() {
            return new Point(0, scrollLocation);
        }

        public void setLocation(Point p) {
            scrollLocation = p.y;
        }

        public Component getComponent() {
            return choice;
        }

        public Dimension getSize() {
            return new Dimension(getWidth(), choice.getItemHeight() * choice.getItemCount());
        }

        public void doRepaint() {
            repaint();
        }

        public int getAdjustableWidth() {
            return vAdj.getBounds().width;
        }

        public int getAdjustableHeight() {
            return 0;
        }

        public void setAdjustableSizes(Adjustable adj, int vis, int min, int max) {
            vAdj.setSizes(vis, min, max);
        }

        public int getAdjustableMode(Adjustable adj) {
            return Scrollable.VERTICAL_ONLY;
        }

        public void setAdjustableBounds(Adjustable adj, Rectangle r) {
            vAdj.setBounds(r);
        }

        public int getWidth() {
            return ChoicePopupBox.this.getWidth();
        }

        public int getHeight() {
            return ChoicePopupBox.this.getHeight();
        }

        public void doRepaint(Rectangle r) {
            repaint(r);
        }
    }

    public ChoicePopupBox(Choice choice) {
        scrollLocation = 0;
        this.choice = choice;
        style = choice.popupStyle;
        vAdj = new ScrollPaneAdjustable(choice, Adjustable.VERTICAL) {
            private static final long serialVersionUID = 2280561825998835980L;

            @Override
            void repaintComponent(Rectangle r) {
                if (isVisible()) {
                    repaint(r);
                }
            }
        };
        scrollable = new ChoiceScrollable();
        listState = new ChoiceListState();
        scrollController = new ScrollStateController(scrollable);
        scrollbarController = vAdj.getStateController();
        vAdj.addAdjustmentListener(scrollController);
        nSkip = MAX_SKIP;
        dragScrollTimer = new PeriodicTimer(25l, getAsyncHandler(getDragScrollHandler()));
    }

    public int getWidth() {
        return getSize().width;
    }

    /**     
     * @return action to be performed on scrolling by mouse drag:
     * action just highlights("focuses") next/previous item
     */
    private Runnable getDragScrollHandler() {
        if (dragScrollHandler == null) {
            dragScrollHandler = new Runnable() {
                int n;

                public void run() {
                    if (n++ % nSkip == 0) {
                        int selItem = focusedItem
                                + (dragScrollSpeed / Math.abs(dragScrollSpeed));
                        selectItem(selItem, false);
                        vAdj.setValue(vAdj.getValue() + dragScrollSpeed);
                    }
                }
            };
        }
        return dragScrollHandler;
    }

    /**
     * Makes handler asynchronous
     * @param handler any Runnable
     * @return asynchronous Runnable: just invokes <code> handler.run()</code>
     *  on event dispatch thread
     */
    private Runnable getAsyncHandler(final Runnable handler) {
        return new Runnable() {
            public void run() {
                EventQueue.invokeLater(handler);
            }
        };
    }

    /**
     * Paints popup list and vertical scrollbar(if necessary)
     */
    @Override
    void paint(Graphics g) {
        toolkit.theme.drawList(g, listState, true);
        if (scroll) {
            Rectangle r = vAdj.getBounds();
            Rectangle oldClip = g.getClipBounds();
            g.setClip(r.x, r.y, r.width, r.height);
            vAdj.prepaint(g);
            g.setClip(oldClip);
        }
    }

    /**
     * Gets item bounds inside popup list
     * @param pos item index
     * @return item bounds rectangle relative to
     * popup list window origin
     */
    Rectangle getItemBounds(int pos) {
        int itemHeight = choice.getItemHeight();
        Point p = new Point(0, pos * itemHeight);
        Rectangle itemRect = new Rectangle(p, new Dimension(getWidth(), itemHeight));
        itemRect.translate(0, scrollLocation);
        return itemRect;
    }

    /**
     * Calculates popup window screen bounds
     * using choice style and location on screen
     * @return list popup window bounds in screen coordinates
     */
    Rectangle calcBounds() {
        scroll = false;
        int itemHeight = choice.getItemHeight();
        int choiceWidth = choice.getWidth();
        int itemWidth = style.getPopupWidth(choiceWidth);
        int count = choice.getItemCount();
        if (count > PAGE_SIZE) {
            count = PAGE_SIZE;
            scroll = true;
        }
        int height = count * itemHeight;
        Rectangle screenBounds = choice.getGraphicsConfiguration().getBounds();
        Point screenLoc = choice.getLocationOnScreen();
        int x = style.getPopupX(screenLoc.x, itemWidth, choiceWidth, screenBounds.width);
        int y = calcY(screenLoc.y, height, screenBounds.height);
        return new Rectangle(x, y, itemWidth, height);
    }

    /**
     * Places list popup window below or above Choice component
     * @param y Choice component screen y-coordinate
     * @param height list height 
     * @param screenHeight height of entire screen
     * @return y screen coordinate of list popup window
     */
    int calcY(int y, int height, int screenHeight) {
        int h = choice.getHeight();
        y += h; // popup is below choice
        int maxHeight = screenHeight - y;
        if (height > maxHeight) {
            // popup is above choice
            y -= height + h;
        }
        return y;
    }

    void show() {
        Rectangle r = calcBounds();
        selectedItem = choice.getSelectedIndex();
        show(r.getLocation(), r.getSize(), choice.getWindowAncestor());
        if (scroll) {
            vAdj.setUnitIncrement(choice.getItemHeight());
            vAdj.setBlockIncrement(vAdj.getUnitIncrement() * PAGE_SIZE);
            scrollController.layoutScrollbars();
            makeFirst(choice.selectedIndex);
        }
        getNativeWindow().setAlwaysOnTop(true);
    }

    /**
     * @return current popup window bounds
     */
    Rectangle getBounds() {
        return new Rectangle(new Point(), getSize());
    }

    @Override
    void onKeyEvent(int eventId, int vKey, long when, int modifiers) {
        if (eventId == KeyEvent.KEY_PRESSED) {
            switch (vKey) {
                case KeyEvent.VK_ESCAPE:
                    hide();
                    break;
                case KeyEvent.VK_ENTER:
                    hide();
                    choice.selectAndFire(selectedItem);
                    break;
            }
        }
    }

    /**
     * Moves selection <code>incr</code> items up/down from current selected
     * item, scrolls list to the new selected item
     */
    void changeSelection(int incr) {
        int newIndex = choice.getValidIndex(selectedItem + incr);
        makeFirst(newIndex);
        selectItem(newIndex);
    }

    /**
     * Scrolls list to make item the first visible item
     * @param idx index of item to scroll to(make visible)
     */
    void makeFirst(int idx) {
        if (scroll) {
            vAdj.setValue(getItemBounds(idx).y - scrollLocation);
        }
    }

    /**
     * Scrolls list to specified y position
     */
    void dragScroll(int y) {
        if (!scroll) {
            return;
        }
        int h = getHeight();
        if ((y >= 0) && (y < h)) {
            dragScrollTimer.stop();
            return;
        }
        int itemHeight = choice.getItemHeight();
        int dy = ((y < 0) ? -y : (y - h));
        nSkip = Math.max(1, MAX_SKIP - 5 * dy / itemHeight);
        dragScrollSpeed = itemHeight * (y / Math.abs(y));
        dragScrollTimer.start();
    }

    /**
     * Handles all mouse events on popup window
     */
    @Override
    void onMouseEvent(int eventId, Point where, int mouseButton, long when, int modifiers,
            int wheelRotation) {
        if (scroll && (eventId == MouseEvent.MOUSE_WHEEL)) {
            processMouseWheel(where, when, modifiers, wheelRotation);
            return;
        }
        if (scroll && (vAdj.getBounds().contains(where) || scrollDragged)) {
            processMouseEvent(eventId, where, mouseButton, when, modifiers);
            return;
        }
        boolean button1 = (mouseButton == MouseEvent.BUTTON1);
        int y = where.y;
        int index = (y - scrollLocation) / choice.getItemHeight();
        switch (eventId) {
            case MouseEvent.MOUSE_PRESSED:
                break;
            case MouseEvent.MOUSE_DRAGGED:
                // scroll on mouse drag
                if ((modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0) {
                    dragScroll(y);
                }
            case MouseEvent.MOUSE_MOVED:
                if ((index < 0) || (index >= choice.getItemCount()) || (index == selectedItem)) {
                    return;
                }
                boolean select = getBounds().contains(where);
                if ((y >= 0) && (y < getHeight())) {
                    selectItem(index, select); //hot track
                }
                break;
            case MouseEvent.MOUSE_RELEASED:
                if (button1) {
                    hide();
                    choice.selectAndFire(index);
                }
                break;
        }
    }

    /**
     * Mouse wheel event processing by vertical scrollbar
     */
    private void processMouseWheel(Point where, long when, int modifiers, int wheelRotation) {
        MouseWheelEvent mwe = new MouseWheelEvent(choice, MouseEvent.MOUSE_WHEEL, when,
                modifiers, where.x, where.y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1,
                wheelRotation);
        scrollController.mouseWheelMoved(mwe);
    }

    /**
     * Mouse event processing by vertical scrollbar
     */
    private void processMouseEvent(int id, Point where, int button, long when, int modifiers) {
        MouseEvent me = new MouseEvent(choice, id, when, modifiers, where.x, where.y, 0, false,
                button);
        switch (id) {
            case MouseEvent.MOUSE_PRESSED:
                scrollDragged = true;
                scrollbarController.mousePressed(me);
                break;
            case MouseEvent.MOUSE_RELEASED:
                scrollDragged = false;
                scrollbarController.mouseReleased(me);
                break;
            case MouseEvent.MOUSE_DRAGGED:
                scrollbarController.mouseDragged(me);
                break;
        }
    }

    /**
     * Selects/focuses item and repaints popup window
     * @param index item to be focused/selected
     * @param highlight item is highlighted(selected) if true,
     * only focused otherwise
     */
    private void selectItem(int index, boolean highlight) {
        Rectangle oldRect = listState.getItemBounds(focusedItem);
        if (focusedItem != selectedItem) {
            oldRect = oldRect.union(listState.getItemBounds(selectedItem));
        }
        focusedItem = index;
        selectedItem = (highlight ? focusedItem : -1);
        Rectangle itemRect = listState.getItemBounds(index);
        Rectangle paintRect = itemRect.union(oldRect);
        paintRect.grow(0, 2);
        if (scroll) {
            paintRect.width -= vAdj.getBounds().width;
        }
        repaint(paintRect);
    }

    private void selectItem(int index) {
        selectItem(index, true);
    }

    void repaint(final Rectangle r) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                paint(new MultiRectArea(r));
            }
        });
    }

    void repaint() {
        repaint(null);
    }

    @Override
    boolean closeOnUngrab(Point start, Point end) {
        if (!getBounds().contains(end)) {
            // close on mouse ungrab only
            // if grab started not above
            // the scrollbar
            return !vAdj.getBounds().contains(start);
        }
        return true;
    }

    @Override
    void hide() {
        // stop timer before closing:
        if (scroll && dragScrollTimer.isRunning()) {
            dragScrollTimer.stop();
        }
        super.hide();
    }
}
