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
package org.apache.harmony.awt.wtk.windows;

import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.PaintEvent;
import java.awt.event.WindowEvent;
import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.nativebridge.Int8Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;
import org.apache.harmony.awt.wtk.NativeEvent;

final class WinEvent extends NativeEvent implements WindowsDefs {
    static final int WM_AWAKE = WM_USER;

    static final Win32 win32 = Win32.getInstance();
    static final NativeBridge bridge = NativeBridge.getInstance();

    private static final long sizeofRECT = win32.createRECT(0).size();

    // missing const from header "tmschema.h"
    private static final int WM_THEMECHANGED = 0x031A;

    private Rectangle clipRect;
    private MultiRectArea clipRgn;
    private int msg;

    private long wParam;
    private long lParam;

    private char lastChar;

    private final WinWindowFactory factory;

    @Override
    public boolean getTrigger() {
        return ((eventId == MouseEvent.MOUSE_RELEASED)
                && (mouseButton == MouseEvent.BUTTON3));
    }

    @Override
    public MultiRectArea getClipRects() {
        if (clipRgn == null) {
            Insets insets = factory.getInsets(windowId);
            int dx = insets.left;
            int dy = insets.top;

            long hRgn = win32.CreateRectRgn(0, 0, 0, 0);
            int type = win32.GetUpdateRgn(windowId, hRgn, 0);
            long rgnToValidate = type != NULLREGION ? hRgn : 0;
            win32.ValidateRgn(windowId, rgnToValidate);

            Win32.RECT nativeRect = win32.createRECT(false);
            win32.GetRgnBox(hRgn, nativeRect);
            clipRect = factory.getRectBounds(nativeRect);
            clipRect.translate(dx, dy);

            switch (type) {
            case COMPLEXREGION: {
                clipRgn = decodeComplexRgn(hRgn, dx, dy);
                break;
            }
            case SIMPLEREGION:
                clipRgn = new MultiRectArea(clipRect);
                break;
            case NULLREGION:
                clipRgn = new MultiRectArea();
                break;
            }
            win32.DeleteObject(hRgn);
        }
        return clipRgn;
    }

    @Override
    public Rectangle getClipBounds() {
        if (clipRect == null) {
            getClipRects();
        }
        return clipRect;
    }

    @Override
    public Rectangle getWindowRect() {
        if (windowRect == null) {
            windowRect = factory.getWindowBounds(windowId);
        }
        return windowRect;
    }

    @Override
    public Insets getInsets() {
        return factory.getInsets(windowId);
    }

    @Override
    public char getLastChar() {
        return lastChar;
    }

    WinEvent(long hwnd, int msg, long wParam, long lParam, long lastTime, 
             StringBuffer lastTranslation, char lastChar, WinWindowFactory factory)
    {
        this.factory = factory;
        this.windowId = hwnd;
        this.msg = msg;
        this.wParam = wParam;
        this.lParam = lParam;
        this.time = lastTime;
        keyInfo.setKeyChars(lastTranslation);
        if (keyInfo.keyChars.length() == 0) {
            keyInfo.keyChars.append(KeyEvent.CHAR_UNDEFINED);
        }
        this.lastChar = lastChar;
        eventId = ID_PLATFORM;

        if (hwnd == factory.eventQueue.getJavaWindow()) {
            if ((msg == WM_THEMECHANGED) || (msg == WM_SYSCOLORCHANGE)
                    || (msg == WM_SETTINGCHANGE)) {
                // TODO: handle this                

                if (msg == WM_THEMECHANGED) {
                    factory.eventQueue.getThemeMap().refresh();
                    factory.eventQueue.systemProperties.getXPTheme(null);
                }
                eventId = ID_THEME_CHANGED;
            }
        } else if (((msg >= WM_MOUSEFIRST) && (msg <= WM_MOUSELAST))
                || (msg == WM_MOUSELEAVE)) {
            processMouse(msg);
        }else if (msg == WM_CHAR) {
            processChar();
        } else if (msg == WM_IME_KEYDOWN) {
            processKey(msg);
        } else if ((msg >= WM_KEYFIRST) && (msg <= WM_KEYLAST)) {
            processKey(msg);
        } else if ((msg == WM_KILLFOCUS) || (msg == WM_SETFOCUS)) {
            processFocus(msg);
        } else if (msg == WM_PAINT) {
            eventId = PaintEvent.PAINT;
            processPaint();
        } else if (msg == WM_SIZE) {
            processSize();
        } else if (msg == WM_MOVE) {
            processMove();
        } else if (msg == WM_CLOSE) {
            eventId = WindowEvent.WINDOW_CLOSING;
        } else if (msg == WM_MOUSEACTIVATE) {
            processActivation();
        } else if (msg == WM_CAPTURECHANGED) {
            if (lParam == 0) {
                eventId = ID_MOUSE_GRAB_CANCELED;
            }
        } else if (msg == WM_CREATE) {
            eventId = ID_CREATED;
        } else if (msg == WM_SHOWWINDOW) {
            processShowWindow();
        } else if (msg == WM_STYLECHANGED) {
            processStyleChanged();
        } else if (msg == WM_THEMECHANGED) {
            processThemeChanged();
        } else if (msg == WM_INPUTLANGCHANGEREQUEST) {
            WinIM.onInputLangChange(lParam);
        }
    }
    
    @Override
    public String toString() {
        return "hwnd=0x" + Long.toHexString(windowId) + ", msg=0x" + Integer.toHexString(msg); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void processChar() {
        keyInfo.keyChars.setLength(0);
        keyInfo.keyChars.append((char)wParam);
        eventId = KeyEvent.KEY_PRESSED;
    }

    private void processThemeChanged() {
        eventId = ID_THEME_CHANGED;
    }

    private void processStyleChanged() {
        if ((wParam & GWL_STYLE) != 0) {
            Win32.STYLESTRUCT ss = win32.createSTYLESTRUCT(lParam);
            int oldStyle = ss.get_styleOld();
            int newStyle = ss.get_styleNew();

            if ((oldStyle & WS_SIZEBOX) != (newStyle & WS_SIZEBOX)) {
                eventId = ID_INSETS_CHANGED;
                // Update native decorations
                int flags = SWP_DRAWFRAME|SWP_NOMOVE|SWP_NOSIZE|SWP_NOZORDER|SWP_NOACTIVATE;
                win32.SetWindowPos(windowId, 0, 0, 0, 0, 0, flags);
            }
        }
    }

    private void processShowWindow() {
        WinWindow win = factory.getWinWindowById(windowId);
        if (wParam != 0 && !win.child) {
            eventId = ID_INSETS_CHANGED;
        }
    }

    private void processSize() {
        WinWindow win = factory.getWinWindowById(windowId);

        if (wParam == SIZE_RESTORED) {
            if (!factory.isWindowBeingCreated()) {
                if (win.isIconified()) {
                    win.setIconified(false);
                    windowState = Frame.NORMAL;
                }
                if (win.isMaximized()) {
                    win.setMaximized(false);
                    windowState = Frame.NORMAL;
                }
            }
            eventId = ID_BOUNDS_CHANGED;
        } else if (wParam == SIZE_MAXIMIZED) {
            win.setIconified(false);
            win.setMaximized(true);
            windowState = Frame.MAXIMIZED_BOTH;
            eventId = ID_BOUNDS_CHANGED;
        } else if (wParam == SIZE_MINIMIZED) {
            win.setIconified(true);
            windowState = Frame.ICONIFIED;
            if (win.isMaximized()) {
                windowState |= Frame.MAXIMIZED_BOTH;
            }
            eventId = WindowEvent.WINDOW_STATE_CHANGED;
        }
    }

    private void processMove() {
        WinWindow win = factory.getWinWindowById(windowId);

        if (win32.IsIconic(windowId) == 0) {
            if (!win.isIconified()) {
                eventId = ComponentEvent.COMPONENT_MOVED;
            } else {
                eventId = ID_INSETS_CHANGED;
            }
        }
    }

    private void processFocus(int msg) {
        eventId = (msg == WM_SETFOCUS) ?
                FocusEvent.FOCUS_GAINED : FocusEvent.FOCUS_LOST;
        otherWindowId = wParam;
    }

    private void processActivation() {
        //translate activation events into focus events only for top-levels
        WinWindow wnd = factory.getWinWindowById(windowId);
        if (wnd.isFocusable()) {
            if(!wnd.child) {
                //set focus on top-level
                win32.SetFocus(windowId);
            }
            else {
                //activate top-level ancestor
                long ancestorHwnd = win32.GetAncestor(windowId, GA_ROOT);
                if (win32.GetActiveWindow() != ancestorHwnd) {
                    win32.SetActiveWindow(ancestorHwnd);
                }

                // check for embedded window, pass focus into it
                if (factory.getWinWindowById(ancestorHwnd) == null) {
                    for (long curHwnd = windowId; curHwnd != 0; ) {
                        long parentHwnd = win32.GetParent(curHwnd);
                        if (factory.getWinWindowById(parentHwnd) == null) {
                            win32.SetFocus(curHwnd);
                            break;
                        }
                        curHwnd = parentHwnd;
                    }
                }

            }
        }

    }
    
    private void processPaint() {
        getClipRects();
    }

    private MultiRectArea decodeComplexRgn(long hRgn, int dx, int dy) {
        int nBytes = win32.GetRegionData(hRgn, 0, 0);
        Int8Pointer rgnDataPtr = bridge.createInt8Pointer(nBytes, false);

        win32.GetRegionData(hRgn, nBytes, rgnDataPtr.lock());
        rgnDataPtr.unlock();
        Win32.RGNDATA rgnData = win32.createRGNDATA(rgnDataPtr);
        Win32.RGNDATAHEADER rdh = rgnData.get_rdh();
        Int8Pointer bufferPtr = rgnData.get_Buffer();

        int nCount = rdh.get_nCount();
        Rectangle rgn[] = new Rectangle[nCount];

        long rawBufferPtr = bufferPtr.lock();
        for (int i = 0; i < nCount; i++) {
            Win32.RECT nr = win32.createRECT(rawBufferPtr + i * sizeofRECT);
            Rectangle rect = factory.getRectBounds(nr);
            rect.translate(dx, dy);
            rgn[i] = rect;
        }
        bufferPtr.unlock();

        return new MultiRectArea(rgn);
    }

    private void processKey(int msg) {
        if ((msg == WM_KEYDOWN) || (msg == WM_SYSKEYDOWN) || (msg == WM_IME_KEYDOWN)) {
            eventId = KeyEvent.KEY_PRESSED;
        } else if ((msg == WM_KEYUP) || (msg == WM_SYSKEYUP)) {
            eventId = KeyEvent.KEY_RELEASED;
        }

        modifiers |= translateKeyModifiers();
        keyInfo.keyLocation = computeKeyLocation();
        keyInfo.vKey = translateVKey();
    }

    private int translateVKey() {
        int vKey = (int) wParam;

        switch (vKey) {
        case VK_RETURN:
            vKey = KeyEvent.VK_ENTER;
            break;
        case VK_LWIN:
        case VK_RWIN:
        case VK_APPS:
            vKey = KeyEvent.VK_UNDEFINED;
            break;
        case VK_INSERT:
            vKey = KeyEvent.VK_INSERT;
            break;
        case VK_DELETE:
            vKey = KeyEvent.VK_DELETE;
            break;
        case VK_OEM_1:
            vKey = KeyEvent.VK_SEMICOLON;
            break;
        case VK_OEM_2:
            vKey = KeyEvent.VK_SLASH;
            break;
        case VK_OEM_4:
            vKey = KeyEvent.VK_OPEN_BRACKET;
            break;
        case VK_OEM_5:
            vKey = KeyEvent.VK_BACK_SLASH;
            break;
        case VK_OEM_6:
            vKey = KeyEvent.VK_CLOSE_BRACKET;
            break;
        case VK_OEM_COMMA:
            vKey = KeyEvent.VK_COMMA;
            break;
        case VK_OEM_PERIOD:
            vKey = KeyEvent.VK_PERIOD;
            break;
        case VK_OEM_MINUS:
            vKey = KeyEvent.VK_MINUS;
            break;
        case VK_OEM_PLUS:
            vKey = KeyEvent.VK_EQUALS;
            break;
        }
        return vKey;
    }

    private int translateKeyModifiers() {
        int modifiers = 0;
        modifiers |= ((win32.GetKeyState(VK_SHIFT) & 0x80) != 0) ?
                InputEvent.SHIFT_DOWN_MASK : 0;
        modifiers |= ((win32.GetKeyState(VK_CONTROL) & 0x80) != 0) ?
                InputEvent.CTRL_DOWN_MASK : 0;
        modifiers |= ((win32.GetKeyState(VK_MENU) & 0x80) != 0) ?
                InputEvent.ALT_DOWN_MASK : 0;
        modifiers |= ((win32.GetKeyState(VK_LBUTTON) & 0x80) != 0) ?
                InputEvent.BUTTON1_DOWN_MASK : 0;
        modifiers |= ((win32.GetKeyState(VK_MBUTTON) & 0x80) != 0) ?
                InputEvent.BUTTON2_DOWN_MASK : 0;
        modifiers |= ((win32.GetKeyState(VK_RBUTTON) & 0x80) != 0) ?
                InputEvent.BUTTON3_DOWN_MASK : 0;
        return modifiers;
    }

    private int computeKeyLocation() {
        int winVKey = (int) wParam;

        if ((winVKey == VK_MENU) || (winVKey == VK_CONTROL)) {
            return ((lParam & 0x1000000) > 0) ?
                    KeyEvent.KEY_LOCATION_RIGHT :
                    KeyEvent.KEY_LOCATION_LEFT;
        }
        if (winVKey == VK_SHIFT) {
            if (((((int) lParam) >> 16) & 0xff) == win32.MapVirtualKeyW(VK_SHIFT, 0)) {
                return KeyEvent.KEY_LOCATION_LEFT;
            }
            return KeyEvent.KEY_LOCATION_RIGHT;
        }
        if (((winVKey >= VK_NUMPAD0) && (winVKey <= VK_NUMPAD9))
                || (winVKey == VK_NUMLOCK) || (winVKey == VK_DECIMAL)
                || (winVKey == VK_DIVIDE) || (winVKey == VK_MULTIPLY)
                || (winVKey == VK_SUBTRACT) || (winVKey == VK_ADD)
                || (winVKey == VK_CLEAR)) {
            return KeyEvent.KEY_LOCATION_NUMPAD;
        }
        if ((winVKey == VK_HOME) || (winVKey == VK_END)
                || (winVKey == VK_UP) || (winVKey == VK_DOWN)
                || (winVKey == VK_LEFT) || (winVKey == VK_RIGHT)
                || (winVKey == VK_PRIOR) || (winVKey == VK_NEXT)
                || (winVKey == VK_INSERT) || (winVKey == VK_DELETE)) {
            return ((lParam & 0x1000000) > 0) ?
                    KeyEvent.KEY_LOCATION_STANDARD :
                    KeyEvent.KEY_LOCATION_NUMPAD;
        }
        if (winVKey == VK_RETURN) {
            return ((lParam & 0x1000000) > 0) ?
                    KeyEvent.KEY_LOCATION_NUMPAD :
                    KeyEvent.KEY_LOCATION_STANDARD;
        }
        return KeyEvent.KEY_LOCATION_STANDARD;
    }

    private void processMouse(int msg) {
        if (msg == WM_MOUSELEAVE) {
            Win32.POINT nativePoint = win32.createPOINT(false);
            win32.GetCursorPos(nativePoint);
            screenPos = new Point(nativePoint.get_x(), nativePoint.get_y());
            eventId = MouseEvent.MOUSE_EXITED;
            modifiers |= translateKeyModifiers();
            factory.lastHwndUnderPointer = 0;
        } else {
            setMouseEventIDAndButton();
            if (eventId == MouseEvent.MOUSE_WHEEL) {
                wheelRotation = computeMouseWheelRotation();
                screenPos = computeMouseWheelScreenPosition();
            } else {
                screenPos = computeMouseScreenPosition();
            }
            modifiers |= translateMouseModifiers();

            if (factory.lastHwndUnderPointer != windowId) {
                WinWindow win = factory.getWinWindowById(windowId);

                if (win.contains(screenPos)) {
                    win.trackMouseEvent();
                    factory.lastHwndUnderPointer = windowId;
                }
            }
            if (factory.mouseGrab.isMouseGrabbed()) {
                Win32.POINT nativePoint = win32.createPOINT(false);
                nativePoint.set_x(screenPos.x);
                nativePoint.set_y(screenPos.y);
                long newHwnd = win32.WindowFromPoint(nativePoint);
                if (factory.getWinWindowById(newHwnd) != null) {
                    windowId = newHwnd;
                }
            }
        }
        localPos = computeMouseLocalPosition();
    }

    private int computeMouseWheelRotation() {
        return -((short) ((wParam >> 16) & 0xffff)) / 120;
    }

    private Point computeMouseLocalPosition() {
        Win32.RECT nativeRect = win32.createRECT(false);
        win32.GetWindowRect(windowId, nativeRect);
        int x = screenPos.x - nativeRect.get_left();
        int y = screenPos.y - nativeRect.get_top();
        return new Point(x, y);
    }

    private Point computeMouseScreenPosition() {
        if (msg == WM_MOUSEMOVE) {
            Win32.MSG tempMsg = win32.createMSG(false);
            while (win32.PeekMessageW(
                    tempMsg, 0, WM_MOUSEMOVE, WM_MOUSEMOVE, PM_NOREMOVE) != 0)
            {
                if (tempMsg.get_hwnd() == windowId) {
                    win32.PeekMessageW(
                            tempMsg, windowId, WM_MOUSEMOVE, WM_MOUSEMOVE, PM_REMOVE);
                    lParam = tempMsg.get_lParam();
                } else {
                    break;
                }
            }
        }

        int x = (short) (lParam & 0xffff);
        int y = (short) ((lParam >> 16) & 0xffff);

        Insets insets = factory.getInsets(windowId);
        x += insets.left;
        y += insets.top;

        Win32.RECT nativeRect = win32.createRECT(false);
        win32.GetWindowRect(windowId, nativeRect);
        x += nativeRect.get_left();
        y += nativeRect.get_top();
        return new Point(x, y);
    }

    private Point computeMouseWheelScreenPosition() {
        int x = (short) (lParam & 0xffff);
        int y = (short) ((lParam >> 16) & 0xffff);
        return new Point(x, y);
    }

    private void setMouseEventIDAndButton() {
        switch (msg) {
        case WM_LBUTTONDOWN:
            eventId = MouseEvent.MOUSE_PRESSED;
            mouseButton = MouseEvent.BUTTON1;
            break;
        case WM_LBUTTONUP:
            eventId = MouseEvent.MOUSE_RELEASED;
            mouseButton = MouseEvent.BUTTON1;
            break;
        case WM_RBUTTONDOWN:
            eventId = MouseEvent.MOUSE_PRESSED;
            mouseButton = MouseEvent.BUTTON3;
            break;
        case WM_RBUTTONUP:
            eventId = MouseEvent.MOUSE_RELEASED;
            mouseButton = MouseEvent.BUTTON3;
            break;
        case WM_MBUTTONDOWN:
            eventId = MouseEvent.MOUSE_PRESSED;
            mouseButton = MouseEvent.BUTTON2;
            break;
        case WM_MBUTTONUP:
            eventId = MouseEvent.MOUSE_RELEASED;
            mouseButton = MouseEvent.BUTTON2;
            break;
        case WM_MOUSEMOVE:
            eventId = ((wParam & (MK_LBUTTON | MK_RBUTTON | MK_MBUTTON)) != 0) ?
                    MouseEvent.MOUSE_DRAGGED : MouseEvent.MOUSE_MOVED;
            break;
        case WM_MOUSEWHEEL:
            eventId = MouseEvent.MOUSE_WHEEL;
            break;
        }
    }

    private int translateMouseModifiers() {
        int modifiers = 0;
        modifiers |= (wParam & MK_SHIFT) != 0 ? InputEvent.SHIFT_DOWN_MASK : 0;
        modifiers |= (wParam & MK_CONTROL) != 0 ? InputEvent.CTRL_DOWN_MASK : 0;
        modifiers |= ((win32.GetKeyState(VK_MENU) & 0x80) != 0) ?
                InputEvent.ALT_DOWN_MASK : 0;

        modifiers |= (wParam & MK_LBUTTON) != 0 ? InputEvent.BUTTON1_DOWN_MASK : 0;
        modifiers |= (wParam & MK_MBUTTON) != 0 ? InputEvent.BUTTON2_DOWN_MASK : 0;
        modifiers |= (wParam & MK_RBUTTON) != 0 ? InputEvent.BUTTON3_DOWN_MASK : 0;
        return modifiers;
    }
}
