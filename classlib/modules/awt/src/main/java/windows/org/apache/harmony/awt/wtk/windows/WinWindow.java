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
 * @author Michael Danilov, Dmitry A. Durnev
 */
package org.apache.harmony.awt.wtk.windows;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;

import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.gl.Surface;
import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.awt.nativebridge.windows.WinManagement;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;
import org.apache.harmony.awt.wtk.CreationParams;
import org.apache.harmony.awt.wtk.NativeWindow;

final class WinWindow implements NativeWindow {

    static final Win32 win32 = Win32.getInstance();
    Surface surface;

    private final WinWindowFactory factory;

    final long hwnd;
    boolean focusable;

    final boolean undecorated;
    final boolean child;
    final boolean popup;

    private boolean iconified;
    private boolean maximized;

    Rectangle maximizedBounds;

    WinWindow(long hwnd, WinWindowFactory factory, CreationParams cp) {
        this.hwnd = hwnd;
        this.factory = factory;

        focusable = true;
        if (cp != null) {
            child = cp.child;
            iconified = cp.iconified;
            maximized = (cp.maximizedState == cp.MAXIMIZED);
            popup = (cp.decorType == CreationParams.DECOR_TYPE_POPUP);
            undecorated = (cp.decorType == CreationParams.DECOR_TYPE_UNDECOR) ||
                    cp.undecorated || popup;
            focusable = (cp.decorType != CreationParams.DECOR_TYPE_POPUP);
        } else {
            undecorated = false;
            child = false;
            popup = false;
        }
    }

    // for EmbeddedWindow
    WinWindow(long hwnd, WinWindowFactory factory) {
        this.hwnd = hwnd;
        this.factory = factory;

        focusable = true;
        child = true;
        popup = false;
        iconified = false;
        maximized = false;
        undecorated = true;
    }

    void postCreate(CreationParams cp) {
        if (cp == null) {
            return;
        }
        if (!cp.resizable && !cp.child) {
            modifyStyle(WindowsDefs.WS_SIZEBOX|WindowsDefs.WS_MAXIMIZEBOX, false);
        }
    }

    boolean trackMouseEvent() {
        Win32.TRACKMOUSEEVENT tme = win32.createTRACKMOUSEEVENT(false);
        tme.set_cbSize(tme.size());
        tme.set_dwFlags(WindowsDefs.TME_LEAVE);
        tme.set_hwndTrack(hwnd);
        int result = win32.TrackMouseEvent(tme);
        return result != 0;
    }

    public void setVisible(final boolean visible) {
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                int cmd;
                if (visible) {
                    cmd = iconified ? WindowsDefs.SW_SHOWMINNOACTIVE : WindowsDefs.SW_SHOW;
                } else {
                    cmd = WindowsDefs.SW_HIDE;
                }

                win32.ShowWindow(hwnd, cmd);
            }
        };
        factory.eventQueue.performTask(task);
    }

    public void setBounds(final int x, final int y, final int w, final int h, final int boundsMask) {
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                if (iconified || maximized) {
                    setNotNormalBounds(x, y, w, h, boundsMask);
                } else {
                    setNormalBounds(x, y, w, h, boundsMask);
                }
            }
        };
        factory.eventQueue.performTask(task);
    }

    private void setNotNormalBounds(int x, int y, int w, int h, int boundsMask) {
        Win32.WINDOWPLACEMENT placement = win32.createWINDOWPLACEMENT(false);
        Win32.RECT rect = placement.get_rcNormalPosition();

        win32.GetWindowPlacement(hwnd, placement);
        placement.set_length(placement.size());
        placement.set_showCmd(iconified? WindowsDefs.SW_MINIMIZE : WindowsDefs.SW_SHOWNORMAL);

        if ((boundsMask & BOUNDS_NOMOVE) == 0) {
            int oldX = rect.get_left();
            int oldY = rect.get_top();

            rect.set_left(x);
            rect.set_top(y);
            rect.set_right(rect.get_right() + (x - oldX));
            rect.set_bottom(rect.get_bottom() + (y - oldY));
        }
        if ((boundsMask & BOUNDS_NOSIZE) == 0) {
            rect.set_right(rect.get_left() + w);
            rect.set_bottom(rect.get_top() + h);
        }

        win32.SetWindowPlacement(hwnd, placement);
    }

    private void setNormalBounds(int x, int y, int w, int h, int boundsMask) {
        Rectangle bounds = getBounds();

        if ((bounds.x ==x) && (bounds.y == y) && (bounds.width == w) && (bounds.height == h)) {
            win32.UpdateWindow(hwnd);
        } else {
            int flags = WindowsDefs.SWP_NOZORDER | WindowsDefs.SWP_NOACTIVATE;
            int dx = 0;
            int dy = 0;

            if ((boundsMask & BOUNDS_NOMOVE) != 0) {
                flags |= WindowsDefs.SWP_NOMOVE;
            }
            if ((boundsMask & BOUNDS_NOSIZE) != 0) {
                flags |= WindowsDefs.SWP_NOSIZE;
            }
            if (child) {
                long hwndParent = win32.GetParent(hwnd);
                Insets insets = factory.getInsets(hwndParent);
                dx = insets.left;
                dy = insets.top;
                flags |= WindowsDefs.SWP_NOCOPYBITS;
            }

            win32.SetWindowPos(hwnd, 0, x - dx, y - dy, w, h, flags);
        }
    }

    public Rectangle getBounds() {
        return factory.getWindowBounds(hwnd);
    }

    public long getId() {
        return hwnd;
    }

    public Insets getInsets() {
        return factory.getInsets(hwnd);
    }

    public void setEnabled(final boolean value) {
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                win32.EnableWindow(hwnd, value ? 1 : 0);
            }
        };
        factory.eventQueue.performTask(task);
    }

    public void dispose() {
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                factory.remove(WinWindow.this);
                win32.DestroyWindow(hwnd);
            }
        };
        factory.eventQueue.performTask(task);
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#placeAfter(org.apache.harmony.awt.wtk.NativeWindow)
     */
    public void placeAfter(final NativeWindow w) {
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                long hwndPrev = (w == null ? WindowsDefs.HWND_TOP : w.getId());
                int flags = WindowsDefs.SWP_NOMOVE | WindowsDefs.SWP_NOSIZE;
                win32.SetWindowPos(hwnd, hwndPrev, 0, 0, 0, 0, flags);
            }
        };
        factory.eventQueue.performTask(task);
    }

    public void toFront() {
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                int flags = WindowsDefs.SWP_NOMOVE | WindowsDefs.SWP_NOSIZE;
                win32.SetWindowPos(hwnd, WindowsDefs.HWND_TOP, 0, 0, 0, 0, flags);
                win32.SetForegroundWindow(hwnd);
            }
        };
        factory.eventQueue.performTask(task);
    }

    public void toBack() {
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                int flags = WindowsDefs.SWP_NOMOVE | WindowsDefs.SWP_NOSIZE;
                win32.SetWindowPos(hwnd, WindowsDefs.HWND_BOTTOM, 0, 0, 0, 0, flags);
            }
        };
        factory.eventQueue.performTask(task);
    }

    public boolean setFocus(final boolean focus) {
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                long res = win32.SetFocus(focus ? hwnd : 0);
                returnValue = new Boolean(res != 0);
            }
        };
        factory.eventQueue.performTask(task);
        return ((Boolean) task.returnValue).booleanValue();
    }

    public void setTitle(final String title) {
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                win32.SetWindowTextW(hwnd, title);
            }
        };
        factory.eventQueue.performTask(task);
    }

    public void setResizable(final boolean value) {
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                modifyStyle(WindowsDefs.WS_SIZEBOX|WindowsDefs.WS_MAXIMIZEBOX, value);
            }
        };
        factory.eventQueue.performTask(task);
    }

    public void grabMouse() {
        factory.mouseGrab.startManualGrab(hwnd);
    }

    public void ungrabMouse() {
        factory.mouseGrab.endManualGrab();
    }

    /**
     * Modify window style
     * @param styleBits - style bits to add or remove
     * @param value - if true set bits, remove otherwise
     */
    private void modifyStyle(int styleBits, boolean value) {
        int style = factory.getWindowStyle(hwnd);
        int newStyle = style;
        if (value) {
            newStyle |= styleBits;
        } else {
            newStyle &= ~styleBits;
        }
        if (style != newStyle) {
            factory.setWindowStyle(hwnd, newStyle);
        }
    }

    public void setFocusable(boolean value) {
        focusable = value;
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#isFocusable()
     */
    public boolean isFocusable() {
        return focusable;
    }
    /**
     * Checks if pos is inside the client area
     * @param pos - point to check (in screen coordinates)
     * @return true if client area contains pos
     */
    boolean contains(Point pos) {
        Win32.POINT pt = win32.createPOINT(false);
        pt.set_x(pos.x);
        pt.set_y(pos.y);
        win32.ScreenToClient(hwnd, pt);
        int x = pt.get_x();
        int y = pt.get_y();

        Win32.RECT nativeRect = win32.createRECT(false);
        win32.GetClientRect(hwnd, nativeRect);
        Rectangle clientRect = factory.getRectBounds(nativeRect);

        return clientRect.contains(x, y);
    }

    boolean isIconified() {
        return iconified;
    }

    void setIconified(boolean iconified) {
        this.iconified = iconified;
    }

    void setMaximized(boolean maximized) {
        this.maximized = maximized;
    }

    boolean isMaximized() {
        return maximized;
    }

    public void setState(final int state) {
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                if (state == Frame.NORMAL) {
                    win32.ShowWindow(hwnd, WindowsDefs.SW_SHOWNORMAL);
                } else if (state == Frame.MAXIMIZED_BOTH) {
                    win32.ShowWindow(hwnd, WindowsDefs.SW_MAXIMIZE);
                } else {
                    Win32.WINDOWPLACEMENT placement = win32.createWINDOWPLACEMENT(false);

                    if (!iconified) {
                        win32.ShowWindow(hwnd, WindowsDefs.SW_MINIMIZE);
                    }
                    win32.GetWindowPlacement(hwnd, placement);
                    if ((state & Frame.MAXIMIZED_BOTH) > 0) {
                        maximized = true;
                        placement.set_flags(placement.get_flags() | WindowsDefs.WPF_RESTORETOMAXIMIZED);
                    } else {
                        maximized = false;
                        placement.set_flags(placement.get_flags() & ~WindowsDefs.WPF_RESTORETOMAXIMIZED);
                    }
                    win32.SetWindowPlacement(hwnd, placement);
                }
            }
        };
        factory.eventQueue.performTask(task);
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setIconImage(java.awt.Image)
     */
    public void setIconImage(final Image image) {
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                long hIcon = 0;
                if (image != null) {
                    hIcon = WinIcons.createIcon(true, image, 0, 0);
                }
                win32.SendMessageW(hwnd, WindowsDefs.WM_SETICON, WindowsDefs.ICON_BIG, hIcon);
                win32.SendMessageW(hwnd, WindowsDefs.WM_SETICON, WindowsDefs.ICON_SMALL, hIcon);
            }
        };
        factory.eventQueue.performTask(task);

    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setMaximizedBounds(java.awt.Rectangle)
     */
    public void setMaximizedBounds(Rectangle bounds) {
        if (bounds == null) {
            return;
        }
        maximizedBounds = new Rectangle(bounds);
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#getScreenPos()
     */
    public Point getScreenPos() {
        Win32.RECT nativeRect = win32.createRECT(false);
        win32.GetWindowRect(hwnd, nativeRect);

        Point pos = new Point(nativeRect.get_left(), nativeRect.get_top());
        return pos;
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeWindow#setAlwaysOnTop(boolean)
     */
    public void setAlwaysOnTop(final boolean value) {
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                int hwndInsertAfter = (value ? WindowsDefs.HWND_TOPMOST : WindowsDefs.HWND_NOTOPMOST);
                int flags = WindowsDefs.SWP_NOMOVE | WindowsDefs.SWP_NOSIZE |
                        WindowsDefs.SWP_NOACTIVATE;
                win32.SetWindowPos(hwnd, hwndInsertAfter, 0, 0, 0, 0, flags);
            }
        };
        factory.eventQueue.performTask(task);
    }

    public void setPacked(boolean packed) {
        // nothing to do
    }

    public MultiRectArea getObscuredRegion(Rectangle part) {
        return WinManagement.getObscuredRegion(hwnd, part);
    }

    public void setIMStyle() {
        // set small title bar:
        factory.setWindowExStyle(getId(), WindowsDefs.WS_EX_PALETTEWINDOW);
        // remove system menu & buttons:
        modifyStyle(WindowsDefs.WS_SYSMENU, false);        
    }
}
