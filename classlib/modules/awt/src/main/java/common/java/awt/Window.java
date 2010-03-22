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

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.awt.im.InputContext;
import java.awt.image.BufferStrategy;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

import org.apache.harmony.awt.AWTPermissionCollection;
import org.apache.harmony.awt.FieldsAccessor;
import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.im.InputMethodContext;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.wtk.NativeWindow;
import org.apache.harmony.luni.util.NotImplementedException;

public class Window extends Container implements Accessible {
    private static final long serialVersionUID = 4497834738069338734L;

    private final AWTListenerList<WindowFocusListener> windowFocusListeners = new AWTListenerList<WindowFocusListener>(
            this);

    private final AWTListenerList<WindowListener> windowListeners = new AWTListenerList<WindowListener>(
            this);

    private final AWTListenerList<WindowStateListener> windowStateListeners = new AWTListenerList<WindowStateListener>(
            this);

    private final ArrayList<Window> ownedWindows = new ArrayList<Window>();

    private transient Component focusOwner;

    private boolean focusableWindowState = true;// By default, all Windows have
                                                // a focusable Window state of
                                                // true

    private Insets nativeInsets = new Insets(0, 0, 0, 0);

    /** Security warning for non-secure windows */
    private final String warningString;

    // Properties of Frame and Dialog
    private String title;

    private boolean resizable;

    private boolean undecorated;

    private boolean alwaysOnTop;

    boolean locationByPlatform;

    /** The window is popup menu or tooltip (for internal use) */
    private boolean popup;

    /**
     * Focus proxy native window actually has native focus when this
     * Window(Frame) is active, but some other (owned) Window is focused
     */
    private transient NativeWindow focusProxy;

    /**
     * Component which has requested focus last
     */
    private transient Component requestedFocus;

    private final transient GraphicsConfiguration graphicsConfiguration;

    private boolean opened;

    private boolean disposed;

    boolean painted;

    private transient InputContext inputContext;

    protected class AccessibleAWTWindow extends AccessibleAWTContainer {
        private static final long serialVersionUID = 4215068635060671780L;

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            toolkit.lockAWT();
            try {
                AccessibleStateSet set = super.getAccessibleStateSet();
                if (isFocused()) {
                    set.add(AccessibleState.ACTIVE);
                }
                if (isResizable()) {
                    set.add(AccessibleState.RESIZABLE);
                }
                return set;
            } finally {
                toolkit.unlockAWT();
            }
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            toolkit.lockAWT();
            try {
                return AccessibleRole.WINDOW;
            } finally {
                toolkit.unlockAWT();
            }
        }
    }

    public Window(Window owner) {
        this(owner, null);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    private void addWindow(Window window) {
        ownedWindows.add(window);
    }

    public Window(Window owner, GraphicsConfiguration gc) {
        toolkit.lockAWT();
        try {
            if (!(this instanceof Frame) && !(this instanceof EmbeddedWindow)) {
                if (owner == null) {
                    // awt.125=null owner window
                    throw new IllegalArgumentException(Messages.getString("awt.125")); //$NON-NLS-1$
                }
                owner.addWindow(this);
            }
            parent = owner; // window's parent is the same as owner(by spec)
            graphicsConfiguration = getGraphicsConfiguration(gc);
            warningString = getWarningStringImpl();
            super.setLayout(new BorderLayout());
            if (owner == null) {
                setBackground(getDefaultBackground());
                setForeground(getDefaultForeground());
            }
            visible = false;
            focusCycleRoot = true; // FIXME
            // Top-levels initialize their focus traversal policies
            // using the context default policy.
            // The context default policy is established by
            // using KeyboardFocusManager.setDefaultFocusTraversalPolicy().
            setFocusTraversalPolicy(KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .getDefaultFocusTraversalPolicy());
            redrawManager = new RedrawManager(this);
            cursor = Cursor.getDefaultCursor(); // for Window cursor is always
                                                // set(non-null)
        } finally {
            toolkit.unlockAWT();
        }
    }

    NativeWindow getFocusProxy() {
        return focusProxy;
    }

    public Window(Frame owner) {
        this((Window) owner);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        // do nothing
    }

    @Override
    public void addNotify() {
        toolkit.lockAWT();
        try {
            super.addNotify();
            focusProxy = toolkit.createFocusProxyNativeWindow(this);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void removeNotify() {
        toolkit.lockAWT();
        try {
            disposeFocusProxy();
            super.removeNotify();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        toolkit.lockAWT();
        try {
            return super.getAccessibleContext();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public Toolkit getToolkit() {
        return toolkit;
    }

    @Override
    public void setCursor(Cursor cursor) {
        toolkit.lockAWT();
        try {
            // for Window cursor is always set(non-null)
            super.setCursor(cursor != null ? cursor : Cursor.getDefaultCursor());
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        toolkit.lockAWT();
        try {
            super.addPropertyChangeListener(listener);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        toolkit.lockAWT();
        try {
            super.addPropertyChangeListener(propertyName, listener);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void createBufferStrategy(int a0) throws NotImplementedException {
        throw new NotImplementedException();
    }

    public void createBufferStrategy(int a0, BufferCapabilities a1) throws AWTException, NotImplementedException {
        throw new NotImplementedException();
    }

    public void dispose() {
        toolkit.lockAWT();
        try {
            if (!disposed) {
                prepare4HierarchyChange();
                hide();
                disposeOwnedWindows();
                mapToDisplay(false);
                disposed = true;
                opened = false;
                disposeInputContext();
                finishHierarchyChange(this, parent, 0);
                postEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSED));
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    private void disposeInputContext() {
        // only default windows input contexts are disposed
        // custom input contexts returned by
        // overridden getInputContext() are not!
        if (inputContext != null) {
            inputContext.dispose();
        }
    }

    /**
     * Remove focus proxy native window from map which is stored in Toolkit
     */
    private void disposeFocusProxy() {
        if (focusProxy != null) {
            toolkit.removeFocusProxyNativeWindow(focusProxy);
            focusProxy = null;
        }
    }

    /**
     * dispose all owned windows explicitly to remove them from Toolkit's map
     */
    private void disposeOwnedWindows() {
        for (int i = 0; i < ownedWindows.size(); i++) {
            Window win = ownedWindows.get(i);
            if (win != null) {
                win.dispose();
            }
        }
    }

    public BufferStrategy getBufferStrategy() throws NotImplementedException {
        throw new NotImplementedException();
    }

    @Override
    public final Container getFocusCycleRootAncestor() {
        toolkit.lockAWT();
        try {
            // Always returns null because Windows have no ancestors
            return null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Component getFocusOwner() {
        toolkit.lockAWT();
        try {
            return isFocused() ? focusOwner : null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
        // why override?
        toolkit.lockAWT();
        try {
            return super.getFocusTraversalKeys(id);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public GraphicsConfiguration getGraphicsConfiguration() {
        toolkit.lockAWT();
        try {
            if (graphicsConfiguration != null) {
                return graphicsConfiguration;
            } else if (parent != null) {
                return parent.getGraphicsConfiguration();
            } else {
                return GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice().getDefaultConfiguration();
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public InputContext getInputContext() {
        toolkit.lockAWT();
        try {
            if (inputContext == null) {
                inputContext = InputContext.getInstance();
            }
            return inputContext;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public Locale getLocale() {
        toolkit.lockAWT();
        try {
            return super.getLocale();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    @Override
    public void hide() {
        toolkit.lockAWT();
        try {
            super.hide();
            painted = false;
            // hide all owned windows explicitly:
            for (int i = 0; i < ownedWindows.size(); i++) {
                Window w = ownedWindows.get(i);
                if (w != null) {
                    w.hide();
                }
            }
            notifyInputMethod(null);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public final boolean isFocusCycleRoot() {
        toolkit.lockAWT();
        try {
            // Every Window is, by default, a "focus cycle root".
            return true;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public final boolean isFocusableWindow() {
        toolkit.lockAWT();
        try {
            return getFocusableWindowState()
                    && (isActivateable() || getFrameDialogOwner().isShowing()
                            && focusTraversalCycleNotEmpty());
        } finally {
            toolkit.unlockAWT();
        }
    }

    final boolean isActivateable() {
        return (this instanceof Frame) || (this instanceof Dialog)
                || (this instanceof EmbeddedWindow);
    }

    private boolean focusTraversalCycleNotEmpty() {
        return getFocusTraversalPolicy().getFirstComponent(this) != null;
    }

    /**
     * Gets the nearest ancestor "activateable" window which is typically Frame
     * or Dialog
     */
    Window getFrameDialogOwner() {
        for (Window o = this;; o = (Window) o.parent) {
            if ((o == null) || o.isActivateable()) {
                return o;
            }
        }
    }

    @Override
    public boolean isShowing() {
        toolkit.lockAWT();
        try {
            return (isVisible() && isDisplayable());
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    @Override
    public boolean postEvent(Event evt) {
        toolkit.lockAWT();
        try {
            // do not propagate event to parent(owner) window:
            return handleEvent(evt);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    @Override
    public void show() {
        toolkit.lockAWT();
        try {
            if (opened) {
                if (isVisible()) {
                    toFront();
                    return;
                }
            } else {
                disposed = false;
            }
            
            if (getFont() == null) {
                setFont(Font.DEFAULT_FONT);
            }
            
            super.show();
            toFront();
            if (!opened) {
                opened = true;
                postEvent(new WindowEvent(this, WindowEvent.WINDOW_OPENED));
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Component getMostRecentFocusOwner() {
        toolkit.lockAWT();
        try {
            // if the Window has never been focused, focus should be set to the
            // Window's initial Component to focus
            return (focusOwner != null) && (focusOwner != this) ? focusOwner
                    : (isFocusableWindow() ? getFocusTraversalPolicy()
                            .getInitialComponent(this) : null);
        } finally {
            toolkit.unlockAWT();
        }
    }

    void setFocusOwner(Component owner) {
        focusOwner = owner;
    }

    @Override
    public final void setFocusCycleRoot(boolean value) {
        toolkit.lockAWT();
        try {
            // Does nothing because Windows must always be roots of a focus
            // traversal cycle.
            // The passed-in value is ignored.
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void toFront() {
        toolkit.lockAWT();
        try {
            NativeWindow win = getNativeWindow();
            if (win != null) {
                win.toFront();
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void applyResourceBundle(ResourceBundle rb) {
        toolkit.lockAWT();
        try {
            applyComponentOrientation(ComponentOrientation.getOrientation(rb));
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void applyResourceBundle(String rbName) {
        toolkit.lockAWT();
        try {
            applyResourceBundle(ResourceBundle.getBundle(rbName));
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean getFocusableWindowState() {
        toolkit.lockAWT();
        try {
            return focusableWindowState;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Window[] getOwnedWindows() {
        toolkit.lockAWT();
        try {
            return ownedWindows.toArray(new Window[0]);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Window getOwner() {
        toolkit.lockAWT();
        try {
            return (Window) parent;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public final String getWarningString() {
        return warningString;
    }

    private final String getWarningStringImpl() {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return null;
        }
        if (sm.checkTopLevelWindow(this)) {
            return null;
        }

        return org.apache.harmony.awt.Utils.getSystemProperty(
                        "awt.appletWarning", "Warning: Java window"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public boolean isActive() {
        toolkit.lockAWT();
        try {
            return KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() == this;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isFocused() {
        toolkit.lockAWT();
        try {
            return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow() == this;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void pack() {
        toolkit.lockAWT();
        try {
            if (getFont() == null) {
                setFont(Font.DEFAULT_FONT);
            }
            
            if ((parent != null) && !parent.isDisplayable()) {
                parent.mapToDisplay(true);
            }
            if (!isDisplayable()) {
                mapToDisplay(true);
            }
            setSize(getPreferredSize());
            validate();
            getNativeWindow().setPacked(true);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public final boolean isAlwaysOnTop() {
        toolkit.lockAWT();
        try {
            return alwaysOnTop;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public final void setAlwaysOnTop(boolean alwaysOnTop) throws SecurityException {
        boolean wasAlwaysOnTop;
        toolkit.lockAWT();
        try {
            if (this.alwaysOnTop == alwaysOnTop) {
                return;
            }
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(AWTPermissionCollection.SET_WINDOW_ALWAYS_ON_TOP_PERMISSION);
            }
            wasAlwaysOnTop = this.alwaysOnTop;
            this.alwaysOnTop = alwaysOnTop;
            NativeWindow win = getNativeWindow();
            if (win != null) {
                win.setAlwaysOnTop(alwaysOnTop);
            }
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("alwaysOnTop", wasAlwaysOnTop, alwaysOnTop); //$NON-NLS-1$
    }

    /**
     * Called by AWT in response to native event "insets changed" on this Window
     * 
     * @param insets new native insets
     */
    void setNativeInsets(Insets insets) {
        if (this.nativeInsets.equals(insets)) {
            return;
        }
        nativeInsets = (Insets) insets.clone();
        validateMenuBar();
        invalidate();
        validate();
    }

    void validateMenuBar() {
        // do nothing, override in Frame to do useful work
    }

    @Override
    Insets getNativeInsets() {
        return (Insets) nativeInsets.clone();
    }

    @Override
    void setBounds(int x, int y, int w, int h, int bMask, boolean updateBehavior) {
        boolean resized = ((w != this.w) || (h != this.h));
        super.setBounds(x, y, w, h, bMask, updateBehavior);
        if (visible && resized && !updateBehavior) {
            validate();
        }
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        locationByPlatform = false;
        super.setBounds(x, y, width, height);
    }

    public void setLocationByPlatform(boolean byPlatform) {
        toolkit.lockAWT();
        try {
            if (byPlatform && visible && behaviour.isDisplayable()) {
                // awt.126=Window is showing
                throw new IllegalComponentStateException(Messages.getString("awt.126")); //$NON-NLS-1$
            }
            locationByPlatform = byPlatform;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isLocationByPlatform() {
        toolkit.lockAWT();
        try {
            if (visible && behaviour.isDisplayable()) {
                return false;
            }
            return locationByPlatform;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setFocusableWindowState(boolean state) {
        boolean oldState;
        toolkit.lockAWT();
        try {
            oldState = focusableWindowState;
            focusableWindowState = state;
            // call cb here to make window natively non-focusable
            NativeWindow win = getNativeWindow();
            if (win != null) {
                win.setFocusable(state);
            }
            if (!state) {
                moveFocusToOwner();
            }
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("focusableWindowState", oldState, focusableWindowState); //$NON-NLS-1$
    }

    /**
     * If this is a focused window then attempt to focus the most recently
     * focused Component of this Window's owner or clear global focus owner if
     * attempt fails
     */
    private void moveFocusToOwner() {
        if (isFocused()) {
            Component compToFocus = null;
            for (Window wnd = getOwner(); wnd != null && compToFocus == null; wnd = wnd
                    .getOwner()) {
                compToFocus = wnd.getMostRecentFocusOwner();
                if (compToFocus != null && !compToFocus.requestFocusImpl(false, true, false)) {
                    compToFocus = null;
                }
            }
            if (compToFocus == null) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
            }
        }
    }

    public void setLocationRelativeTo(Component c) {
        toolkit.lockAWT();
        try {
            Rectangle screenRect = getGraphicsConfiguration().getBounds();
            int minX = screenRect.x;
            int minY = screenRect.y;
            int maxX = minX + screenRect.width - 1;
            int maxY = minY + screenRect.height;
            int centerX = (minX + maxX) / 2;
            int centerY = (minY + maxY) / 2;
            int x = centerX;
            int y = centerY;
            // if comp is null or not showing, then set location
            // relative to "component" of 1-pixel size located
            // at the center of the screen
            Point loc = new Point(centerX, centerY);
            int compX = loc.x;
            Dimension compSize = new Dimension();
            if ((c != null) && c.isShowing()) {
                loc = c.getLocationOnScreen();
                compX = loc.x;
                compSize = c.getSize();
            }
            // first get center coords:
            loc.translate(compSize.width / 2, compSize.height / 2);
            // now get upper-left corner coords:
            int w = getWidth(), h = getHeight();
            loc.translate(-w / 2, -h / 2);
            // check if screenRect contains new window
            // bounds rectangle and if not - change location
            // of window to fit into screenRect
            x = Math.max(loc.x, minX);
            y = Math.max(loc.y, minY);
            int right = x + w, bottom = y + h;
            if (right > maxX) {
                x -= right - maxX;
            }
            if (bottom > maxY) {
                y -= bottom - maxY;
                // If the bottom of the component is offscreen,
                // the window is placed to the side of the Component
                // that is closest to the center of the screen.
                int compRight = compX + compSize.width;
                int distRight = Math.abs(compRight - centerX);
                int distLeft = Math.abs(centerX - compX);
                x = ((distRight < distLeft) ? compRight : (compX - w));
                x = Math.max(x, minX);
                right = x + w;
                if (right > maxX) {
                    x -= right - maxX;
                }
            }
            setLocation(x, y);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void toBack() {
        toolkit.lockAWT();
        try {
            NativeWindow win = getNativeWindow();
            if (win != null) {
                win.toBack();
            }
            // TODO?: reset the focused Window(this or any of owners) to the
            // top-most Window in the VM
        } finally {
            toolkit.unlockAWT();
        }
    }

    boolean isResizable() {
        return resizable;
    }

    void setResizable(boolean resizable) {
        if (this.resizable == resizable) {
            return;
        }
        this.resizable = resizable;
        NativeWindow win = getNativeWindow();
        if (win != null && !undecorated && !popup) {
            win.setResizable(resizable);
        }
    }

    boolean isUndecorated() {
        return undecorated;
    }

    void setUndecorated(boolean undecorated) {
        if (this.undecorated == undecorated) {
            return;
        }
        if (isDisplayable()) {
            // awt.127=Cannot change the decorations while the window is visible
            throw new IllegalComponentStateException(Messages.getString("awt.127")); //$NON-NLS-1$
        }
        this.undecorated = undecorated;
    }

    boolean isPopup() {
        return popup;
    }

    void setPopup(boolean popup) {
        if (isDisplayable()) {
            // awt.127=Cannot change the decorations while the window is visible
            throw new IllegalComponentStateException(Messages.getString("awt.127")); //$NON-NLS-1$
        }
        this.popup = popup;
    }

    String getTitle() {
        return title;
    }

    /**
     * Set title for Frame or Dialog<br>
     * It does lockAWT() properly so there's no need to synchronize the calls of
     * this method
     * 
     * @param title - value to set
     */
    void setTitle(String title) {
        String oldTitle = this.title;
        toolkit.lockAWT();
        try {
            this.title = (title == null) ? "" : title; //$NON-NLS-1$
            NativeWindow win = getNativeWindow();
            if (win != null) {
                win.setTitle(title);
            }
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("title", oldTitle, title); //$NON-NLS-1$
    }

    @Override
    RedrawManager getRedrawManager() {
        return redrawManager;
    }

    void redrawAll() {
        if (redrawManager.redrawAll()) {
            painted = true;
        }
    }

    void setRequestedFocus(Component component) {
        requestedFocus = component;
    }

    Component getRequestedFocus() {
        return requestedFocus;
    }

    @Override
    AccessibleContext createAccessibleContext() {
        return new AccessibleAWTWindow();
    }

    /**
     * Gets the default Cursor if Window is disabled even if Cursor is
     * explicitly set
     * 
     * @return actual Cursor to be displayed
     * @see Component.getRealCursor()
     */
    @Override
    Cursor getRealCursor() {
        return isEnabled() ? getCursor() : Cursor.getDefaultCursor();
    }

    @Override
    String autoName() {
        int number = toolkit.autoNumber.nextWindow++;
        return "window" + Integer.toString(number); //$NON-NLS-1$
    }

    public void addWindowFocusListener(WindowFocusListener l) {
        windowFocusListeners.addUserListener(l);
    }

    public void addWindowListener(WindowListener l) {
        windowListeners.addUserListener(l);
    }

    public void addWindowStateListener(WindowStateListener l) {
        windowStateListeners.addUserListener(l);
    }

    public WindowFocusListener[] getWindowFocusListeners() {
        return windowFocusListeners.getUserListeners(new WindowFocusListener[0]);
    }

    public WindowListener[] getWindowListeners() {
        return windowListeners.getUserListeners(new WindowListener[0]);
    }

    public WindowStateListener[] getWindowStateListeners() {
        return windowStateListeners.getUserListeners(new WindowStateListener[0]);
    }

    public void removeWindowFocusListener(WindowFocusListener l) {
        windowFocusListeners.removeUserListener(l);
    }

    public void removeWindowListener(WindowListener l) {
        windowListeners.removeUserListener(l);
    }

    public void removeWindowStateListener(WindowStateListener l) {
        windowStateListeners.removeUserListener(l);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (WindowFocusListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getWindowFocusListeners();
        } else if (WindowStateListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getWindowStateListeners();
        } else if (WindowListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getWindowListeners();
        } else {
            return super.getListeners(listenerType);
        }
    }

    @Override
    protected void processEvent(AWTEvent e) {
        long eventMask = toolkit.eventTypeLookup.getEventMask(e);
        if (eventMask == AWTEvent.WINDOW_EVENT_MASK) {
            processWindowEvent((WindowEvent) e);
        } else if (eventMask == AWTEvent.WINDOW_STATE_EVENT_MASK) {
            processWindowStateEvent((WindowEvent) e);
        } else if (eventMask == AWTEvent.WINDOW_FOCUS_EVENT_MASK) {
            processWindowFocusEvent((WindowEvent) e);
        } else {
            super.processEvent(e);
        }
    }

    protected void processWindowEvent(WindowEvent e) {
        for (Iterator<?> i = windowListeners.getUserIterator(); i.hasNext();) {
            WindowListener listener = (WindowListener) i.next();
            switch (e.getID()) {
                case WindowEvent.WINDOW_ACTIVATED:
                    listener.windowActivated(e);
                    break;
                case WindowEvent.WINDOW_CLOSED:
                    listener.windowClosed(e);
                    break;
                case WindowEvent.WINDOW_CLOSING:
                    listener.windowClosing(e);
                    break;
                case WindowEvent.WINDOW_DEACTIVATED:
                    listener.windowDeactivated(e);
                    break;
                case WindowEvent.WINDOW_DEICONIFIED:
                    listener.windowDeiconified(e);
                    break;
                case WindowEvent.WINDOW_ICONIFIED:
                    listener.windowIconified(e);
                    break;
                case WindowEvent.WINDOW_OPENED:
                    listener.windowOpened(e);
                    break;
            }
        }
    }

    protected void processWindowFocusEvent(WindowEvent e) {
        for (Iterator<?> i = windowFocusListeners.getUserIterator(); i.hasNext();) {
            WindowFocusListener listener = (WindowFocusListener) i.next();
            switch (e.getID()) {
                case WindowEvent.WINDOW_GAINED_FOCUS:
                    listener.windowGainedFocus(e);
                    break;
                case WindowEvent.WINDOW_LOST_FOCUS:
                    listener.windowLostFocus(e);
                    break;
            }
        }
    }

    protected void processWindowStateEvent(WindowEvent e) {
        for (Iterator<?> i = windowStateListeners.getUserIterator(); i.hasNext();) {
            WindowStateListener listener = (WindowStateListener) i.next();
            switch (e.getID()) {
                case WindowEvent.WINDOW_STATE_CHANGED:
                    listener.windowStateChanged(e);
                    break;
            }
        }
    }

    @Override
    void moveFocusOnHide() {
        // let native system move focus itself
        // if native focused window is the same as
        // java focused window
        if (!isActivateable()) {
            super.moveFocusOnHide();
        }
    }

    @Override
    ComponentBehavior createBehavior() {
        return new HWBehavior(this);
    }

    private GraphicsConfiguration getGraphicsConfiguration(GraphicsConfiguration gc) {
        if (gc == null) {
            Toolkit.checkHeadless();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
        } else if (GraphicsEnvironment.isHeadless()) {
            // awt.128=Graphics environment is headless
            throw new IllegalArgumentException(Messages.getString("awt.128")); //$NON-NLS-1$
        }
        if (gc.getDevice().getType() != GraphicsDevice.TYPE_RASTER_SCREEN) {
            // awt.129=Not a screen device
            throw new IllegalArgumentException(Messages.getString("awt.129")); //$NON-NLS-1$
        }
        return gc;
    }

    @Override
    Color getDefaultBackground() {
        return SystemColor.window;
    }

    @Override
    Color getDefaultForeground() {
        return SystemColor.windowText;
    }

    @Override
    boolean isPrepainter() {
        return true;
    }

    @Override
    void prepaint(Graphics g) {
        Color back = getBackground();
        if (back == null) {
            back = getDefaultBackground();
        }
        g.setColor(back);
        Insets ins = getNativeInsets();
        g.fillRect(ins.left, ins.top, w - ins.right - ins.left, h - ins.bottom - ins.top);
    }

    /**
     * Called immediately after native window has been created. Updates native
     * window state & properties to make them correspond to Java Window
     * state/properties
     */
    @Override
    void nativeWindowCreated(NativeWindow win) {
        win.setFocusable(getFocusableWindowState());
        nativeInsets = win.getInsets();
        win.setAlwaysOnTop(isAlwaysOnTop());
        win.setIconImage(getIconImage());
    }

    /**
     * Returns icon image of the owner frame. This method is overridden as
     * public in the class Frame
     */
    Image getIconImage() {
        toolkit.lockAWT();
        try {
            for (Container c = parent; c != null; c = c.parent) {
                if (c instanceof Frame) {
                    return ((Frame) c).getIconImage();
                }
            }
            return null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    MultiRectArea getObscuredRegion(Rectangle part) {
        if (!visible || behaviour.getNativeWindow() == null) {
            return null;
        }
        Insets ins = getNativeInsets();
        Rectangle r = new Rectangle(ins.left, ins.top, w - ins.left - ins.right, h - ins.top
                - ins.bottom);
        if (part != null) {
            r = r.intersection(part);
        }
        if (r.isEmpty()) {
            return null;
        }
        return behaviour.getNativeWindow().getObscuredRegion(r);
    }

    private void readObject(ObjectInputStream stream) throws IOException,
            ClassNotFoundException {
        stream.defaultReadObject();
        FieldsAccessor accessor = new FieldsAccessor(Window.class, this);
        accessor.set("graphicsConfiguration", getGraphicsConfiguration(null)); //$NON-NLS-1$
        visible = false;
        redrawManager = new RedrawManager(this);
    }

    @Override
    void notifyInputMethod(Rectangle bounds) {
        InputContext ic = getInputContext();
        if (ic instanceof InputMethodContext) {
            ((InputMethodContext) ic).notifyClientWindowChange(bounds);
        }
    }
}
