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
 */
package org.apache.harmony.awt.wtk.windows;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;
import org.apache.harmony.awt.wtk.CreationParams;
import org.apache.harmony.awt.wtk.NativeWindow;
import org.apache.harmony.awt.wtk.WindowFactory;


public final class WinWindowFactory implements WindowFactory {

    static final Win32 win32 = Win32.getInstance();
    static final NativeBridge bridge = NativeBridge.getInstance();

    private final Map<Long, WinWindow> hwnd2winMap = new HashMap<Long, WinWindow>();
    private CreationParams creationParams = null;

    final WinEventQueue eventQueue;

    // for WinEvent
    long lastHwndUnderPointer = 0;

    final MouseGrab mouseGrab = new MouseGrab();

    WinWindowFactory(WinEventQueue eq) {
        eventQueue = eq;
        eventQueue.addPreprocessor(mouseGrab);
    }

    public NativeWindow createWindow(final CreationParams p) {
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                String title = (p.name != null) ? p.name : ""; //$NON-NLS-1$
                Rectangle rect = new Rectangle(p.x, p.y, p.w, p.h);
                int style = getStyle(p);
                int styleEx = getStyleEx(p);

                if (p.locationByPlatform) {
                    rect.x = rect.y = WindowsDefs.CW_USEDEFAULT;
                }
                if (p.parentId != 0 && p.child) {
                    Insets insets = getInsets(p.parentId);
                    rect.x -= insets.left;
                    rect.y -= insets.top;
                }

                creationParams = p;
                long hwnd = win32.CreateWindowExW(styleEx,
                        WindowProcHandler.windowClassName, title, style,
                        rect.x, rect.y, rect.width, rect.height,
                        p.parentId, 0, 0, null);

                creationParams = null;
                NativeWindow win = getWindowById(hwnd);
                assert win != null;

                returnValue = win;
            }
        };
        eventQueue.performTask(task);
        return (WinWindow) task.returnValue;
    }

    public NativeWindow getWindowById(long id) {
        return hwnd2winMap.get(new Long(id));
    }

    /**
     * @param id - HWND of Windows window
     * @return WinWindow object representing Windows window,
     * or null if given HWND doesn't belong to WTK
     */
    public WinWindow getWinWindowById(long id) {
        return hwnd2winMap.get(new Long(id));
    }

    /**
     * @param p - point on the screen
     * @return window whose client area contains p
     */
    public NativeWindow getWindowFromPoint(Point p) {
        Win32.POINT pt = win32.createPOINT(false);
        pt.set_x(p.x);
        pt.set_y(p.y);
        long hwnd = win32.WindowFromPoint(pt);
        WinWindow wnd = getWinWindowById(hwnd);
        return ((wnd != null) && wnd.contains(p)) ? wnd : null;
    }

    void onCreateWindow(long hwnd) {
        WinWindow win = new WinWindow(hwnd, this, creationParams);
        hwnd2winMap.put(new Long(hwnd), win);
        win.postCreate(creationParams);
    }

    boolean isWindowBeingCreated() {
        return creationParams != null;
    }

    void remove(WinWindow win) {
        hwnd2winMap.remove(win);
    }

    public boolean isWindowStateSupported(int state) {
        switch (state) {
        case Frame.NORMAL:
        case Frame.ICONIFIED:
        case Frame.MAXIMIZED_BOTH:
            return true;
        default:
            return false;
        }
    }

    private static int getStyle(CreationParams p) {
        int style = WindowsDefs.WS_CLIPCHILDREN|WindowsDefs.WS_CLIPSIBLINGS;
        style |= p.child ? WindowsDefs.WS_CHILD : WindowsDefs.WS_POPUP;

        switch (p.decorType) {
            case CreationParams.DECOR_TYPE_FRAME:
                style |= WindowsDefs.WS_SYSMENU | WindowsDefs.WS_MINIMIZEBOX;
                if (!p.undecorated) {
                    style |= WindowsDefs.WS_CAPTION | WindowsDefs.WS_BORDER |
                            WindowsDefs.WS_MAXIMIZEBOX | WindowsDefs.WS_SIZEBOX;                 }
                break;
            case CreationParams.DECOR_TYPE_DIALOG:
                style |= WindowsDefs.WS_SYSMENU;
                if (!p.undecorated) {
                    style |= WindowsDefs.WS_CAPTION | WindowsDefs.WS_BORDER | WindowsDefs.WS_SIZEBOX;
                }
                break;
            case CreationParams.DECOR_TYPE_POPUP:
                break;
            case CreationParams.DECOR_TYPE_UNDECOR:
                break;
        }

        if (p.visible) {
            style |= WindowsDefs.WS_VISIBLE;
        }
        if (p.disabled) {
            style |= WindowsDefs.WS_DISABLED;
        }
        if (p.iconified) {
            style |= WindowsDefs.WS_MINIMIZE;
        }
        if (p.maximizedState == p.MAXIMIZED) {
            style |= WindowsDefs.WS_MAXIMIZE;
        }
        return style;
    }

    private static int getStyleEx(CreationParams p) {
        int styleEx = 0;
        if (p.topmost) {
            styleEx |= WindowsDefs.WS_EX_TOPMOST;
        }

        switch(p.decorType) {
            case CreationParams.DECOR_TYPE_FRAME:
                styleEx |= WindowsDefs.WS_EX_APPWINDOW; // Show in taskbar
                break;
            case CreationParams.DECOR_TYPE_POPUP:
                styleEx |= WindowsDefs.WS_EX_NOACTIVATE;
                break;
        }

        return styleEx;
    }

    static final class MouseGrab implements WinEventQueue.Preprocessor {
        private boolean manualGrabActive = false;
        private boolean autoGrabActive = false;
        private long grabHwnd = 0;
        private boolean releasing = false;

        static final long mouseMask = (~WindowsDefs.MK_ALT &
                                       ~WindowsDefs.MK_CONTROL &
                                       ~WindowsDefs.MK_SHIFT);
        /**
         * @see org.apache.harmony.awt.wtk.windows.WinEventQueue.Preprocessor#preprocess(long, int, long, long, long[])
         */
        public boolean preprocess(long hwnd, int msg,
                long wParam, long lParam, long[] result) {

            switch(msg) {
            case WindowsDefs.WM_LBUTTONDOWN:
            case WindowsDefs.WM_RBUTTONDOWN:
            case WindowsDefs.WM_MBUTTONDOWN:
            case WindowsDefs.WM_XBUTTONDOWN:
                onMouseButtonDown(hwnd, wParam & mouseMask);
                break;
            case WindowsDefs.WM_LBUTTONUP:
            case WindowsDefs.WM_RBUTTONUP:
            case WindowsDefs.WM_MBUTTONUP:
            case WindowsDefs.WM_XBUTTONUP:
                onMouseButtonUp(wParam & mouseMask);
                break;
            case WindowsDefs.WM_CAPTURECHANGED:
                if (onCaptureChanged(lParam)) {
                    result[0] = 0;
                    return true;
                }
                break;
            case WindowsDefs.WM_MOUSEMOVE:
                onMouseMove(wParam & mouseMask);
                break;
            case WindowsDefs.WM_ACTIVATE:
                if ((wParam & 0xFFFF) == WindowsDefs.WA_ACTIVE) {
                    onActivate();
                }
            }
            return false;
        }

        boolean isMouseGrabbed() {
            return grabHwnd != 0;
        }

        void startAutoGrab(long hwnd) {
            autoGrabActive = true;
            if (grabHwnd == 0) {
                grabImpl(hwnd);
            }
        }

        void startManualGrab(long hwnd) {
            manualGrabActive = true;
            grabImpl(hwnd);
        }

        void endManualGrab() {
            manualGrabActive = false;

            if (!autoGrabActive) {
                ungrabImpl();
            }
        }

        void endAutoGrab() {
            autoGrabActive = false;
            if (!manualGrabActive && grabHwnd != 0) {
                ungrabImpl();
            }
        }

        void restoreAutoGrab() {
            long hwnd = win32.GetActiveWindow();
            if (hwnd != 0) {
                startAutoGrab(hwnd);
            }
        }

        void grabCanceled() {
            autoGrabActive = manualGrabActive = false;
            grabHwnd = 0;
        }

        private void grabImpl(long hwnd) {
            grabHwnd = hwnd;
            win32.SetCapture(hwnd);
        }

        private void ungrabImpl() {
            releasing = true;
            win32.ReleaseCapture();
            releasing = false;
            grabHwnd = 0;
        }


        private void onMouseButtonUp(long mouseState) {
            if (mouseState == 0) {
                endAutoGrab();
            }
        }

        /**
         * Preprocess mouse capture event
         * @param otherHwnd - the event parameter
         * @return true if capture change event shouldn't be processed
         */
        private boolean onCaptureChanged(long otherHwnd) {
            if (otherHwnd == 0) {
                grabCanceled();
            }
            return releasing;
        }

        private void onMouseButtonDown(long hwnd, long mouseState) {
            // Check that only one mouse button is pressed at the moment
            if (mouseState == WindowsDefs.MK_LBUTTON ||
                mouseState == WindowsDefs.MK_MBUTTON ||
                mouseState == WindowsDefs.MK_RBUTTON ||
                mouseState == WindowsDefs.MK_XBUTTON1 ||
                mouseState == WindowsDefs.MK_XBUTTON2) {

                startAutoGrab(hwnd);
            }

        }

        private void onMouseMove(long mouseState) {
            if ((mouseState != 0) && (grabHwnd == 0)) {
                restoreAutoGrab();
            }
        }

        private void onActivate() {
            if (isKeyPressed(WindowsDefs.VK_LBUTTON) ||
                    isKeyPressed(WindowsDefs.VK_MBUTTON) ||
                    isKeyPressed(WindowsDefs.VK_RBUTTON) ||
                    isKeyPressed(WindowsDefs.VK_XBUTTON1) ||
                    isKeyPressed(WindowsDefs.VK_XBUTTON2)) {
                restoreAutoGrab();
            }
        }

        private boolean isKeyPressed(int vKey) {
            return (win32.GetKeyState(vKey) & 0x80) != 0;
        }
    }

    /**
     * @see org.apache.harmony.awt.wtk.WindowFactory#attachWindow(long)
     */
    public NativeWindow attachWindow(long nativeWindowId) {
        return new WinWindow(nativeWindowId, this);
    }

    /**
     * @see org.apache.harmony.awt.wtk.WindowFactory#setCaretPosition(int, int)
     */
    public void setCaretPosition(final int x, final int y) {
        WinEventQueue.Task task = new WinEventQueue.Task () {
           @Override
        public void perform() {
                win32.SetCaretPos(x, y);
            }
        };
        eventQueue.performTask(task);
    }

    /**
     * @see org.apache.harmony.awt.wtk.WindowFactory#getWindowSizeById(long)
     */
    public Dimension getWindowSizeById(long hwnd) {

        Win32.RECT nativeRect = win32.createRECT(false);
        win32.GetWindowRect(hwnd, nativeRect);

        Dimension size = new Dimension();
        size.width = nativeRect.get_right() - nativeRect.get_left();
        size.height = nativeRect.get_bottom() - nativeRect.get_top();
        return size;
    }

    /**
     * Obtain the window's native decorations size
     * @param hwnd - window handle
     * @return the insets. Never returns null.
     */
    Insets getInsets(long hwnd) {
        Insets insets = new Insets(0, 0, 0, 0);
        if ((getWindowStyle(hwnd)& WindowsDefs.WS_ICONIC) != 0) {
            return insets;
        }

        Win32.RECT rect = win32.createRECT(false);
        win32.GetClientRect(hwnd, rect);
        int width = rect.get_right();
        int height = rect.get_bottom();

        win32.GetWindowRect(hwnd, rect);

        Win32.POINT topLeft = win32.createPOINT(false);
        topLeft.set_x(rect.get_left());
        topLeft.set_y(rect.get_top());
        win32.MapWindowPoints(0, hwnd, topLeft, 1);

        Win32.POINT bottomRight = win32.createPOINT(false);
        bottomRight.set_x(rect.get_right());
        bottomRight.set_y(rect.get_bottom());
        win32.MapWindowPoints(0, hwnd, bottomRight, 1);

        insets.left = -topLeft.get_x();
        insets.right = bottomRight.get_x() - width;
        insets.top = -topLeft.get_y();
        insets.bottom = bottomRight.get_y() - height;

        return insets;
    }

    int getWindowStyle(long hwnd) {
        return win32.GetWindowLongW(hwnd, WindowsDefs.GWL_STYLE);
    }

    void setWindowStyle(long hwnd, int style) {
        win32.SetWindowLongW(hwnd, WindowsDefs.GWL_STYLE, style);
    }

    int getWindowExStyle(long hwnd) {
        return win32.GetWindowLongW(hwnd, WindowsDefs.GWL_EXSTYLE);
    }

    void setWindowExStyle(long hwnd, int style) {
        win32.SetWindowLongW(hwnd, WindowsDefs.GWL_EXSTYLE, style);
    }

    boolean isChild(long hwnd) {
        return ((getWindowStyle(hwnd) & WindowsDefs.WS_CHILD) != 0);
    }

    /**
     * Get window size and position relative to its parent
     * @param hwnd - the window handle
     * @return window bounds
     */
    Rectangle getWindowBounds(long hwnd) {

        Win32.RECT nativeRect = win32.createRECT(false);
        win32.GetWindowRect(hwnd, nativeRect);

        Rectangle rect = getRectBounds(nativeRect);

        long hwndParent = win32.GetParent(hwnd);
        if (hwndParent != 0 && isChild(hwnd)) {
            win32.GetWindowRect(hwndParent, nativeRect);
            rect.translate(-nativeRect.get_left(), -nativeRect.get_top());
        }
        return rect;
    }

    Rectangle getRectBounds(Win32.RECT nativeRect) {
        Rectangle rect = new Rectangle();
        rect.x = nativeRect.get_left();
        rect.y = nativeRect.get_top();
        rect.width = nativeRect.get_right() - rect.x;
        rect.height = nativeRect.get_bottom() - rect.y;

        return rect;
    }


}
