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
 * @author Pavel Dolgov
 *
 */

package java.awt;

import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.wtk.NativeWindow;

/**
 * Non-component window for displaying menus and drop-down lists
 */
abstract class PopupBox {

    NativeWindow nativeWindow;

    final Dimension size = new Dimension();
    final Point location = new Point();
    private Window owner;
    private PopupBox parent;
    private PopupBox activeChild;
    private boolean visible;
    private ModalContext modalContext;

    final Toolkit toolkit = Toolkit.getDefaultToolkit();

    PopupBox() {
    }

    void addNotify() {
        if (nativeWindow == null) {
            nativeWindow = toolkit.createPopupNativeWindow(this);
        }
    }

    void removeNotify() {
        if (nativeWindow != null) {
            toolkit.removePopupNativeWindow(nativeWindow);
            NativeWindow temp = nativeWindow;
            nativeWindow = null;
            temp.dispose();
        }
    }

    Dimension getSize() {
        return new Dimension(size);
    }

    int getHeight() {
        return getSize().height;
    }

    Point getLocation() {
        return new Point(location);
    }

    Point getScreenLocation() {
        return new Point(location);
    }

    boolean contains(Point p) {
        Rectangle r = new Rectangle(getScreenLocation(), getSize());
        return r.contains(p);
    }

    Window getOwner() {
        return owner;
    }

    PopupBox getParent() {
        return parent;
    }

    void setParent(PopupBox p) {
        parent = p;
    }

    final PopupBox getActiveChild() {
        return activeChild;
    }

    final NativeWindow getNativeWindow() {
        return nativeWindow;
    }

    final boolean isVisible() {
        return visible;
    }

    boolean isMenu() {
        return false;
    }

    final void setModal(boolean modal) {
        if (modal) {
            if (modalContext == null) {
                modalContext = new ModalContext();
            }
        } else {
            if (modalContext != null) {
                if (modalContext.isModalLoopRunning()) {
                    modalContext.endModalLoop();
                }
                modalContext = null;
            }
        }
    }

    final boolean isModal() {
        return modalContext != null;
    }

    boolean isMenuBar() {
        return false;
    }

    Rectangle calculateBounds() {
        return new Rectangle(location, size);
    }

    void paint(MultiRectArea clipArea) {
        if (nativeWindow != null) {
            Graphics gr = getGraphics(clipArea);
            if (gr != null) {
                paint(gr);
            }
            gr.dispose();
        }
    }

    abstract void paint(Graphics gr);

    /**
     * Mouse events handler
     * @param eventId - one of the MouseEvent.MOUSE_* constants
     * @param where - mouse location
     * @param mouseButton - mouse button that was pressed or released
     * @param when - event time
     * @param modifiers - input event modifiers
     */
    abstract void onMouseEvent(int eventId, Point where, int mouseButton,
            long when, int modifiers, int wheelRotation);

    /**
     * Keyboard events handler
     * @param eventId - one of the KeyEvent.KEY_* constants
     * @param vKey - the key code
     * @param when - event time
     * @param modifiers - input event modifiers
     */
    abstract void onKeyEvent(int eventId, int vKey, long when, int modifiers);

    /**
     * Determine if the pop-up box should be hidden if the mouse button was 
     * pressed at the <code>start</code> position 
     * and released at the <code>end</code> position
     * @param start - the location where mouse button was pressed
     * @param end - the location where mouse button was released
     * @return true if the pop-up box should be hidden; false otherwise 
     */
    boolean closeOnUngrab(Point start, Point end) {
        return true;
    }

    /**
     * Pass the keyboard event to the topmost active pop-up box
     * @param eventId - one of the KeyEvent.KEY_* constants
     * @param vKey - the key code
     * @param when - event time
     * @param modifiers - input event modifiers
     */
    final void dispatchKeyEvent(int eventId, int vKey, long when, int modifiers) {
        if (activeChild != null) {
            activeChild.dispatchKeyEvent(eventId, vKey, when, modifiers);
        } else {
            onKeyEvent(eventId, vKey, when, modifiers);
        }
    }

    void setDefaultCursor() {
        Cursor.getDefaultCursor().getNativeCursor().setCursor(nativeWindow.getId());
    }

    private void show(int x, int y, int w, int h) {
        visible = true;
        addNotify();

        location.setLocation(x, y);
        size.setSize(w, h);
        nativeWindow.setBounds(x, y, w, h, 0);
        nativeWindow.setVisible(true);
        setDefaultCursor();
    }

    void show(Frame owner) {
        this.owner = owner;

        Rectangle bounds = calculateBounds();
        show(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    void show(Point location, Dimension size, Window owner) {
        this.owner = owner;
        if (parent != null) {
            parent.activeChild = this;
        }
        show(location.x, location.y, size.width, size.height);

        toolkit.dispatcher.popupDispatcher.activate(this);
        if (modalContext != null) {
            modalContext.runModalLoop();
        }
    }

    void hide() {
        if (!visible) {
            return;
        }
        visible = false;

        if (activeChild != null) {
            activeChild.hide();
        }

        if (nativeWindow != null) {
            nativeWindow.setVisible(false);
        }

        if (parent != null) {
            parent.activeChild = null;
        }

        if (modalContext != null) {
            modalContext.endModalLoop();
        }

        toolkit.dispatcher.popupDispatcher.deactivate(this);
    }

    Graphics getGraphics(MultiRectArea clip) {
        Dimension size = getSize();
        Graphics gr = toolkit.getGraphicsFactory().getGraphics2D(nativeWindow, 0, 0, size.width, size.height);
        if (gr != null && clip != null) {
            gr.setClip(clip);
        }
        return gr;
    }

}
