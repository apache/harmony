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
 * @author Michael Danilov
 */
package org.apache.harmony.awt.wtk.linux;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.gl.Utils;
import org.apache.harmony.awt.gl.linux.XVolatileImage;
import org.apache.harmony.awt.gl.linux.XGraphicsConfiguration;
import org.apache.harmony.awt.nativebridge.CLongPointer;
import org.apache.harmony.awt.nativebridge.Int32Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.PointerPointer;
import org.apache.harmony.awt.nativebridge.VoidPointer;
import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.X11Defs;
import org.apache.harmony.awt.wtk.CreationParams;
import org.apache.harmony.awt.wtk.NativeWindow;


class LinuxWindow implements NativeWindow {

    private static final int DEFAULT_MASK = X11Defs.StructureNotifyMask
            | X11Defs.ExposureMask | X11Defs.FocusChangeMask
            | X11Defs.KeyPressMask | X11Defs.KeyReleaseMask
            | X11Defs.ButtonPressMask | X11Defs.ButtonReleaseMask
            | X11Defs.PointerMotionMask | X11Defs.OwnerGrabButtonMask
            | X11Defs.LeaveWindowMask | X11Defs.EnterWindowMask
            | X11Defs.PropertyChangeMask;

    private static final int DISABLED_MASK = X11Defs.ExposureMask | X11Defs.StructureNotifyMask
            | X11Defs.PropertyChangeMask | X11Defs.FocusChangeMask;

    private static final int MOUSE_GRAB_MASK = X11Defs.PointerMotionMask
            | X11Defs.ButtonPressMask | X11Defs.ButtonReleaseMask;

    private final static Insets DEFAULT_INSETS =
        new Insets(25, 5, 5, 5); // initial "guessed" insets

    // Window size limits
    public final static int MIN_WINDOW_WIDTH = 1;
    public final static int MAX_WINDOW_WIDTH = Short.MAX_VALUE;
    public final static int MIN_WINDOW_HEIGHT = 1;
    public final static int MAX_WINDOW_HEIGHT = Short.MAX_VALUE;

    static boolean manualGrabActive = false;
    static boolean autoGrabActive = false;
    static LinuxWindow grabWindow = null;

    private final boolean child;

    private final long display;

    private final long windowID;

    private long parentID;

    private boolean visible;

    private boolean enabled;

    private boolean resizable;

    private boolean popup;

    private Dimension requestedSize;

    private Rectangle windowRect;

    private static final X11 x11 = X11.getInstance();
    private static final NativeBridge bridge = NativeBridge.getInstance();

    private final LinuxWindowFactory factory;

    private final WindowManager wm;

    boolean moved, resized;

    private boolean iconified = false;
    private boolean maximized = false;
    private int currentState = 0;
    boolean alwaysOnTop = false;

    Insets savedInsets = new Insets(0, 0, 0, 0);

    private boolean undecorated = true;

    private boolean focusable = true;

    private Insets oldInsets;

    private boolean packed;

    private ContentWindow contentWindow;

    private boolean insetsChanged;

    LinuxWindow(LinuxWindowFactory factory, CreationParams p) {

        display = factory.getDisplay();
        visible = p.visible;
        enabled = !p.disabled;
        resizable = p.resizable;
        this.factory = factory;
        wm = factory.wm;
        child = p.child;
        popup = (p.decorType == CreationParams.DECOR_TYPE_POPUP);
        undecorated = (p.decorType == CreationParams.DECOR_TYPE_UNDECOR)
                || p.undecorated || popup;
        if (p.maximizedState == p.MAXIMIZED) {
            currentState |= Frame.MAXIMIZED_BOTH;
        }
        if (p.iconified) {
            currentState |= Frame.ICONIFIED;
        }

        parentID = p.child ? p.parentId : x11.XDefaultRootWindow(display);
        long ownerID = p.child ? 0 : p.parentId;
        //X can't create windows with zero size
        p.w = adjustedWidth(p.w);
        p.h = adjustedHeight(p.h);
        if (child) {
            windowRect = new Rectangle(p.x, p.y, p.w, p.h);
        }

        XGraphicsConfiguration xgcfg =
                (XGraphicsConfiguration) GraphicsEnvironment.
                getLocalGraphicsEnvironment().getDefaultScreenDevice().
                getDefaultConfiguration();

        X11.XSetWindowAttributes setAttrs = x11.createXSetWindowAttributes(false);
        setAttrs.set_colormap(xgcfg.getXColormap());

        windowID = x11.XCreateWindow(
                display, parentID,
                p.x, p.y, p.w, p.h, 0,
                xgcfg.getDepth(),
                X11Defs.InputOutput,
                xgcfg.getVisual(),
                (long) (X11Defs.CWColormap),
                setAttrs
        );

        /*
        windowID = x11.XCreateWindow(display, parentID, p.x, p.y, p.w, p.h, 0,
                                     X11Defs.CopyFromParent,
                                     X11Defs.InputOutput,
                                     X11Defs.CopyFromParent, 0, 0);
                                     */
        String title = (p.name != null) ? p.name : ""; //$NON-NLS-1$
        x11.Xutf8SetWMProperties(display, windowID, title, title, null, 0,
                null, null, null);
        x11.XSelectInput(display, windowID, enabled ? DEFAULT_MASK : DISABLED_MASK);

        if (!popup) {
            if (!child && ownerID != 0) {
                x11.XSetTransientForHint(display, windowID, ownerID);
            }
            wm.setDecorType(windowID, p.decorType, p.undecorated);
            wm.setInputAllowed(windowID, !p.disabled);
            wm.setWMProtocols(windowID);
        } else {
            setPopupAttributes();
        }

        if (child) {
            //windowRect = new Rectangle(p.x, p.y, p.w, p.h);
            x11.XLowerWindow(display, windowID);
        } else {
            if (!popup) {
                wm.setResizableHint(windowID, resizable, p.x, p.y, p.w, p.h);
                // Override the window position set by Window Manager
                windowRect = new Rectangle(0, 0, p.w, p.h);
                setBounds(p.x, p.y, p.w, p.h, 0);
            } else {
                windowRect = new Rectangle(p.x, p.y, p.w, p.h);
            }
            //A Client whose window has not yet been mapped can request of the
            //Window Manager an estimate of the frame extents it will be given
            //upon mapping
            if (!child && !undecorated) {
                savedInsets = DEFAULT_INSETS;
                wm.requestFrameExtents(windowID);
            }
            x11.XRaiseWindow(display, windowID);
        }
        if (visible) {
            x11.XMapWindow(display, windowID);
        }
    }

    // for EmbeddedWindow
    LinuxWindow(long nativeWindowId, LinuxWindowFactory factory) {

        this.factory = factory;
        windowID = nativeWindowId;
        display = factory.getDisplay();
        visible = true;
        enabled = true;
        resizable = false;
        undecorated = true;
        wm = factory.wm;
        child = true;
        popup = false;
    }

    public void setVisible(boolean v) {
        if (visible != v) {
            visible = v;
            if (visible) {
                if (!isChild()) {
                    // update state hints before the window is mapped:
                    // wm removes the property upon window withdrawal
                    setState(currentState);
                    setAlwaysOnTop(alwaysOnTop);
                }
                x11.XMapWindow(display, windowID);
            } else {
                x11.XWithdrawWindow(display, windowID, factory.getScreen());
            }
        }
        x11.XFlush(display);
    }

    public long getId() {
        return windowID;
    }

    Rectangle onBoundsChange(Rectangle newBounds) {

        moved = resized = false;
        Rectangle oldRect = (Rectangle) windowRect.clone();

        windowRect.setBounds(newBounds);

        if (!resizable && requestedSize != null &&
                newBounds.width == requestedSize.width && newBounds.height == requestedSize.height) {
            wm.setResizableHint(windowID, false, newBounds.x, newBounds.y, newBounds.width, newBounds.height);
            requestedSize = null;
        }
        Point loc = windowRect.getLocation(), oldLoc = oldRect.getLocation();
        if (!loc.equals(oldLoc)) {
            moved = true;
        }
        Dimension size = windowRect.getSize(), oldSize = oldRect.getSize();
        if (!size.equals(oldSize)) {
            resized = true;
        }
        return oldRect;
    }

    public void setBounds(int x, int y, int w, int h, int boundsMask) {


        int newWidth = adjustedWidth(w);
        int newHeight = adjustedHeight(h);

        windowRect = getBounds(); // TODO: check if windowRect field can be removed
        boolean willMove = (x != windowRect.x || y != windowRect.y) &&
                (boundsMask & BOUNDS_NOMOVE) == 0;
        boolean willResize = (newWidth != windowRect.width || newHeight != windowRect.height) &&
                (boundsMask & BOUNDS_NOSIZE) == 0;

        if (!willMove && !willResize) {
            return;
        }

        toFront();

        if (willMove) {
            x11.XMoveWindow(display, windowID, x, y);
        }
        if (willResize) {
            //            if (!resizable) {
            requestedSize = new Dimension(newWidth, newHeight);
            //            }
            int wasOverrideRedirect = -1;
            if (!child && !resizable) {
                // allow program(not user) resizing of a non-resizable window
                X11.XWindowAttributes winAttr = x11.createXWindowAttributes(false);
                x11.XGetWindowAttributes(display, windowID, winAttr);
                wasOverrideRedirect = winAttr.get_override_redirect();
                setOverrideRedirect(1);
            }
            x11.XResizeWindow(display, windowID, newWidth, newHeight);

            if (wasOverrideRedirect >= 0) {
                setOverrideRedirect(wasOverrideRedirect);
            }

        }

        x11.XFlush(display);
    }

    private void setOverrideRedirect(int on) {
        X11.XSetWindowAttributes winAttr = x11.createXSetWindowAttributes(true);
        winAttr.set_override_redirect(on);
        long mask = X11Defs.CWOverrideRedirect;
        x11.XChangeWindowAttributes(display, windowID, mask, winAttr);
    }

    /**
     * Query window size and position from X server
     */
    public Rectangle getBounds() {
        Int32Pointer x = bridge.createInt32Pointer(1, false);
        Int32Pointer y = bridge.createInt32Pointer(1, false);
        Int32Pointer w = bridge.createInt32Pointer(1, false);
        Int32Pointer h = bridge.createInt32Pointer(1, false);
        CLongPointer root = bridge.createCLongPointer(1, false);
        Int32Pointer border = bridge.createInt32Pointer(1, false);
        Int32Pointer depth = bridge.createInt32Pointer(1, false);

        x11.XGetGeometry(display, windowID, root, x, y, w, h, border, depth);
        long rootID = root.get(0);
        if (!child || (this instanceof ContentWindow)) {
            CLongPointer childWindow = bridge.createCLongPointer(1, false);
            x11.XTranslateCoordinates(display, getParentID(), rootID,
                                      x.get(0), y.get(0), x, y, childWindow);
        }
        Rectangle r = new Rectangle(x.get(0), y.get(0), w.get(0), h.get(0));

        return r;
    }

    public Insets updateInsets() {
        Insets insets = new Insets(0, 0, 0, 0);
        if (!child && !undecorated) {
            Int32Pointer x = bridge.createInt32Pointer(1, false);
            Int32Pointer y = bridge.createInt32Pointer(1, false);
            Int32Pointer w = bridge.createInt32Pointer(1, false);
            Int32Pointer h = bridge.createInt32Pointer(1, false);
            CLongPointer root = bridge.createCLongPointer(1, false);
            Int32Pointer border = bridge.createInt32Pointer(1, false);
            Int32Pointer depth = bridge.createInt32Pointer(1, false);

            x11.XGetGeometry(display, windowID, root, x, y, w, h, border, depth);
            long rootID = root.get(0);
            long frameID = getFrameID(windowID, rootID);

            if (frameID != 0 && frameID != rootID && parentID == rootID) {
                int width = w.get(0), height = h.get(0);
                CLongPointer childWindow = bridge.createCLongPointer(1, false);

                //get window coordinates relative to WM's frame
                x11.XTranslateCoordinates(display, getParentID(), frameID,
                                        x.get(0), y.get(0), x, y, childWindow);
                insets.top = y.get(0);
                insets.left = x.get(0);
                x11.XGetGeometry(display, frameID, root, x, y, w, h, border, depth);
                int frameW = w.get(0);
                int frameH = h.get(0);
                insets.right = frameW - width - insets.left;
                insets.bottom = frameH - height - insets.top;
            } else {
                // Suggested insets, just to have some
                insets.top = 25;
                insets.bottom = insets.left = insets.right = 5;
            }
        }
        setInsets(insets);
        return (Insets) insets.clone();
    }

    public Insets getInsets() {
        return (Insets) savedInsets.clone();
    }

    public void setEnabled(boolean value) {
        if (enabled != value) {
            enabled = value;
            x11.XSelectInput(display, windowID, enabled ? DEFAULT_MASK : DISABLED_MASK);
            setInputAllowed(value);
        }
        x11.XFlush(display);
    }

    public void dispose() {
        factory.onWindowDispose(windowID);
        x11.XDestroyWindow(display, windowID);
        x11.XFlush(display);
    }

    public void placeAfter(NativeWindow w) {
        if (w == null) {
            toFront();
            return;
        }
        CLongPointer params = bridge.createCLongPointer(2, false);
        params.set(0, w.getId());
        params.set(1, windowID);
        x11.XRestackWindows(display, params, 2);
        x11.XFlush(display);
    }

    public void toFront() {
        x11.XRaiseWindow(display, windowID);
        x11.XFlush(display);
    }

    public void toBack() {
        x11.XLowerWindow(display, windowID);
        x11.XFlush(display);
    }

    Point queryPointer() {
        CLongPointer rootReturned = bridge.createCLongPointer(1, false);
        CLongPointer childReturned = bridge.createCLongPointer(1, false);
        Int32Pointer rootX = bridge.createInt32Pointer(1, false);
        Int32Pointer rootY = bridge.createInt32Pointer(1, false);
        Int32Pointer windowX = bridge.createInt32Pointer(1, false);
        Int32Pointer windowY = bridge.createInt32Pointer(1, false);
        Int32Pointer mask = bridge.createInt32Pointer(1, false);

        x11.XQueryPointer(display, windowID,
                rootReturned, childReturned,
                rootX, rootY, windowX,  windowY,
                mask);

        return new Point(windowX.get(0), windowY.get(0));
    }

    public boolean setFocus(boolean focus) {

        if (!isFocusable()) {
            return false;
        }
        // avoid focus request loops:
        if (wm.getFocusedWindow() == getId()) {
            return true;
        }

        boolean isViewable = (getMapState() == X11Defs.IsViewable);
        // try to set input focus only if window is mapped
        // [and all ancestor windows are mapped]
        // , clear focus otherwise
        boolean setFocus = isViewable && focus;
        long id = setFocus ? windowID : X11Defs.None;

        x11.XSetInputFocus(display, id, X11Defs.RevertToParent,
                           X11Defs.CurrentTime);

        x11.XFlush(display);
        return setFocus;
    }

    int getMapState() {
        X11.XWindowAttributes winAttr = x11.createXWindowAttributes(false);
        x11.XGetWindowAttributes(display, windowID, winAttr);

        return winAttr.get_map_state();
    }

    public void setTitle(String title) {
        if (title == null) {
            title = ""; //$NON-NLS-1$
        }
        x11.Xutf8SetWMProperties(display, windowID, title, title, null, 0,
                null, null, null);
        x11.XFlush(display);
    }

    public void setResizable(boolean value) {
        if (value != resizable) {
            resizable = value;
            wm.setResizableHint(windowID, resizable, windowRect.x, windowRect.y,
                                requestedSize.width, requestedSize.height);
        }
        x11.XFlush(display);
    }

    /**
     * Start manual mouse grab
     */
    public void grabMouse() {
        manualGrabActive = true;
        grabWindow = this;

        x11.XGrabPointer(display, windowID, X11Defs.True, MOUSE_GRAB_MASK,
                X11Defs.GrabModeAsync, X11Defs.GrabModeAsync,
                X11Defs.None, X11Defs.None, X11Defs.CurrentTime);
        x11.XFlush(display);
    }

    /**
     * End manual mouse grab. If automatic grab is still active,
     * the ungrab is postponed until all mouse buttons are released.
     */
    public void ungrabMouse() {
        manualGrabActive = false;

        if (!autoGrabActive) {
            ungrabMouseImpl();
        }
    }

    /**
     * Actually end the mouse grab
     */
    private void ungrabMouseImpl() {
        x11.XUngrabPointer(display, X11Defs.CurrentTime);
        x11.XFlush(display);

        grabWindow = null;
    }

    /**
     * Notify that mouse button was pressed. If there was no manual grab
     * the automatic mouse grab is activated implicitly. Cancel manual grab
     * if it was active and mouse was clicked outside of application windows
     *
     * @param winId - window that got the event
     * @param x - x mouse position in window coordinates
     * @param y - x mouse position in window coordinates
     * @return false if explicit grab was canceled; true otherwise
     */
    static boolean startAutoGrab(long winId, int x, int y) {
        // If the click was outside of any LinuxWindow it was reported to grabWindow
        if (grabWindow != null && grabWindow.getId() == winId &&
                !grabWindow.isInside(winId, x, y)) {

            autoGrabActive = false;
            grabWindow.ungrabMouseImpl();
            return false;
        }

        autoGrabActive = true;
        return true;
    }

    /**
     * Notify that all mouse buttons were released and automatic grab has to be ended.
     * If the manual ungrab was postponed, do it now.
     */
    static void endAutoGrab() {
        autoGrabActive = false;
        if (!manualGrabActive && grabWindow != null) {
            grabWindow.ungrabMouseImpl();
        }
    }

    private boolean isInside(long winId, int x, int y) {
        return (winId == windowID && x >= 0 && x < windowRect.width &&
                y >= 0 && y < windowRect.height);
    }

    private void setPopupAttributes() {
        X11.XSetWindowAttributes winAttr = x11.createXSetWindowAttributes(false);
        winAttr.set_override_redirect(1);
        winAttr.set_save_under(1);
        long mask = X11Defs.CWOverrideRedirect | X11Defs.CWSaveUnder;
        x11.XChangeWindowAttributes(display, windowID, mask, winAttr);
    }

    boolean isInputAllowed() {
        return wm.isInputAllowed(windowID);
    }

    void setInputAllowed(boolean value) {
        wm.setInputAllowed(windowID, value);
    }

    boolean isChild() {
        return child;
    }

    static final int adjustedWidth(int width) {
        return Math.min(Math.max(MIN_WINDOW_WIDTH, width), MAX_WINDOW_WIDTH);
    }

    static final int adjustedHeight(int height) {
        return Math.min(Math.max(MIN_WINDOW_HEIGHT, height), MAX_WINDOW_HEIGHT);
    }

    public void setFocusable(boolean value) {
        setInputAllowed(value);
        focusable = value;
    }

    public boolean isFocusable() {
        return focusable;//isInputAllowed();
    }

    /**
     * @return actual native parent of a window
     * (for a top-level this maybe a WM frame window)
     */
    long getParentID() {
        return getParentID(windowID);
    }

    long getFrameID() {
        return getFrameID(windowID, factory.getRootWindow());
    }

    /**
     * @return actual native parent of a window
     * (for a top-level this maybe a WM frame window)
     */
    private long getParentID(long windowID) {
        if (windowID == 0) {
            return 0;
        }

        CLongPointer root = bridge.createCLongPointer(1, false);
        CLongPointer parent = bridge.createCLongPointer(1, false);
        PointerPointer children = bridge.createPointerPointer(1, true);
        Int32Pointer count = bridge.createInt32Pointer(1, false);

        if (x11.XQueryTree(display, windowID, root, parent, children, count) != 0) {
            final VoidPointer data = children.get(0);
            
            if (data != null) {
                x11.XFree(data);
            }
            
            return parent.get(0);
        }
        
        return 0;
    }

    private long getFrameID(long windowId, long rootID) {
        long frameID = getParentID(windowId);

        if (frameID != 0 && frameID != rootID) {

            while (getParentID(frameID) != rootID) {
                frameID = getParentID(frameID);
            }

        }
        return frameID != rootID ? frameID : 0;
    }


    /**
     * @return display of the current window
     */
    long getDisplay() {
        return display;
    }
    /**
     * @return saved requested window bounds
     */
    Rectangle getWindowRect() {
        return windowRect;
    }

    LinuxWindow getParent() {
        if (factory.validWindowId(parentID)) {
            return (LinuxWindow) factory.getWindowById(parentID);
        }
        return null;
    }

    boolean isIconified() {
        return iconified;
    }

    private int setBits(int n, int bits, boolean set) {
        return (set ? (n | bits) : (n & ~bits));
    }

    void setIconified(boolean iconified) {
        this.iconified = iconified;
        currentState = setBits(currentState, Frame.ICONIFIED, iconified);
    }

    void setMaximized(boolean maximized) {
        this.maximized = maximized;
        currentState = setBits(currentState, Frame.MAXIMIZED_BOTH, maximized);
    }

    boolean isMaximized() {
        return maximized;
    }

    int getState() {
        int state = 0;

        if (maximized) {
            state |= Frame.MAXIMIZED_BOTH;
        }
        if (iconified) {
            state |= Frame.ICONIFIED;
        }

        return state;
    }

    int getCurrentState() {
        return currentState;
    }

    public void setState(int state) {
        currentState = state;
        if (iconified) {
            if ((state & Frame.ICONIFIED) == 0) {
                x11.XMapRaised(display, windowID);
            }
        } else {
            if ((state & Frame.ICONIFIED) > 0) {
                x11.XIconifyWindow(display, windowID, factory.getScreen());
            }
        }

        if ((maximized && ((state & Frame.MAXIMIZED_BOTH) == 0)) ||
                (!maximized && ((state & Frame.MAXIMIZED_BOTH) > 0)))
        {
            long[] states = new long[] {wm.NET_WM_STATE_MAXIMIZED_HORZ,
                                        wm.NET_WM_STATE_MAXIMIZED_VERT};
            wm.changeWindowState(this, maximized ? 0 : 1, states);
        }
        x11.XFlush(display);
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setIconImage(java.awt.Image)
     */
    public void setIconImage(Image image) {
        long pixmap = 0;
        if (image != null) {
            BufferedImage bufImg = Utils.getBufferedImage(image);
            if (bufImg == null) {
                return;
            }
            pixmap = createPixmap(image);
        }
        wm.setIcon(windowID, pixmap, 0);
    }

    private long createPixmap(Image image) {
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        XVolatileImage xvi = (XVolatileImage) GraphicsEnvironment.
            getLocalGraphicsEnvironment().getDefaultScreenDevice().
            getDefaultConfiguration().createCompatibleVolatileImage(w, h);
        Graphics g = xvi.getGraphics();
        g.setColor(Color.lightGray);
        g.fillRect(0, 0, w, h);
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return xvi.getPixmap();
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setMaximizedBounds(java.awt.Rectangle)
     */
    public void setMaximizedBounds(Rectangle bounds) {
        final int MAX = Integer.MAX_VALUE;
        if (bounds == null) {
            return;
        }
        int w = bounds.width;
        int h = bounds.height;
        int screen = factory.getScreen();

        if (w == MAX) {
            w = x11.XDisplayWidth(display, screen);
        }

        if (h == MAX) {
            h = x11.XDisplayHeight(display, screen);
        }
        //TODO: call adjustRect(bounds, 0, -1) to decrease requested from WM
        //size on insets size
        wm.setResizableHint(windowID, true, true,
                            bounds.x, bounds.y,
                            w, h);

    }

    boolean isUndecorated() {
        return undecorated;
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#getScreenPos()
     */
    public Point getScreenPos() {

        Int32Pointer x = bridge.createInt32Pointer(1, false);
        Int32Pointer y = bridge.createInt32Pointer(1, false);
        Int32Pointer w = bridge.createInt32Pointer(1, false);
        Int32Pointer h = bridge.createInt32Pointer(1, false);
        CLongPointer root = bridge.createCLongPointer(1, false);
        Int32Pointer border = bridge.createInt32Pointer(1, false);
        Int32Pointer depth = bridge.createInt32Pointer(1, false);

        x11.XGetGeometry(display, windowID, root, x, y, w, h, border, depth);
        long rootID = root.get(0);

        CLongPointer childWindow = bridge.createCLongPointer(1, false);
        x11.XTranslateCoordinates(display, getParentID(), rootID,
                x.get(0), y.get(0), x, y, childWindow);

        Point pos = new Point(x.get(0), y.get(0));
        return pos;
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setAlwaysOnTop(boolean)
     */
    public void setAlwaysOnTop(boolean value) {
        wm.changeWindowState(this, value ? 1 : 0,
                             new long[] { wm.NET_WM_STATE_ABOVE,
                                         wm.NET_WM_STATE_STAYS_ON_TOP, 0l });
        alwaysOnTop = value;

    }

    void setInsets(Insets newInsets) {
        if ((newInsets == null) || newInsets.equals(savedInsets)) {
            return;
        }
        oldInsets = (insetsChanged ? savedInsets : new Insets(0, 0, 0, 0));
        insetsChanged = true;
        savedInsets = (Insets) newInsets.clone();

        if (contentWindow != null) {
//            Rectangle contentRect = contentWindow.getBounds();
            x11.XMoveWindow(getDisplay(), contentWindow.getId(),
                    -newInsets.left, -newInsets.top);

            Rectangle rect = getBounds();

            int dx = savedInsets.left - oldInsets.left;
            int dy = savedInsets.top - oldInsets.top;
            int dw = dx + (savedInsets.right - oldInsets.right);
            int dh = dy + (savedInsets.bottom - oldInsets.bottom);

            if (contentWindow.isPacked()) {
                // don't adjust client area size if
                // the window is packed
                int w = adjustedWidth(/*contentRect*/rect.width + dw);
                int h = adjustedHeight(/*contentRect*/rect.height + dh);
                x11.XResizeWindow(display, contentWindow.getId(), w, h);
                contentWindow.setPacked(false);
            } else {
                // adjust client area size to match
                // new insets
                int newWidth = adjustedWidth(rect.width - dw);
                int newHeight = adjustedHeight(rect.height - dh);
                 // update WM hints to resize "frame" window
                wm.setResizableHint(windowID, resizable, rect.x, rect.y,
                        newWidth, newHeight);
                x11.XResizeWindow(display, windowID, newWidth, newHeight);
            }

            x11.XFlush(display);

        }
    }

    /**
     * @return Returns the contentWindow.
     */
    ContentWindow getContentWindow() {
        return contentWindow;
    }
    /**
     * @param contentWindow The contentWindow to set.
     */
    void setContentWindow(ContentWindow contentWindow) {
        this.contentWindow = contentWindow;
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setPacked(boolean)
     */
    public void setPacked(boolean packed) {
        this.packed = true;
    }

    /**
     * @return Returns the packed flag.
     */
    boolean isPacked() {
        return packed;
    }

    public MultiRectArea getObscuredRegion(Rectangle part) {
        return null;
    }

    public void setIMStyle() {
    }
}
