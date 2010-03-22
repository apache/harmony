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
 * @author Dmitry A. Durnev, Pavel Dolgov
 */
package org.apache.harmony.awt.wtk.windows;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.nativebridge.Int16Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;
import org.apache.harmony.awt.wtk.NativeEvent;
import org.apache.harmony.awt.wtk.NativeEventQueue;
import org.apache.harmony.awt.wtk.NativeWindow;
import org.apache.harmony.misc.accessors.AccessorFactory;
import org.apache.harmony.misc.accessors.ObjectAccessor;

/**
 * Handler of Windows messages
 */
public class WinEventQueue extends NativeEventQueue {
    
    static final NativeBridge bridge = NativeBridge.getInstance();
    static final Win32 win32 = Win32.getInstance();
    static final ObjectAccessor objAccessor = AccessorFactory.getObjectAccessor();

    /**
     * Invisible auxiliary window for service messages
     */
    private final long javaWindow;
    /**
     * Theme handles currently open
     */
    private final ThemeMap themeMap = new ThemeMap();

    /**
     * The message being processed
     */
    private final Win32.MSG lastMsg = win32.createMSG(false);

    final int dispatchThreadID = win32.GetCurrentThreadId();
    final WinSystemProperties systemProperties;

    /**
     * The last keyboard message
     */
    private final Win32.MSG charMsg = win32.createMSG(false);
    /**
     * Accumulated typed characters
     */
    private final StringBuffer lastTranslation = new StringBuffer();
    /**
     * Recently typed character
     */
    private char lastChar;
    /**
     * Keycodes and characters stored 
     * until WM_KEYUP or WM_SYSKEYUP message is received 
     */
    private final HashMap<Integer, Character> keyCodeToChar = new HashMap<Integer, Character>();

    final WinWindowFactory factory;

    private long utcOffset = -1;
    /**
     * Return value for the Windows message being processed
     */
    private final long[] result = new long[1];

    public static final int WM_PERFORM_TASK = WindowsDefs.WM_USER + 1;
    public static final int WM_PERFORM_LATER = WindowsDefs.WM_USER + 2;

    private final LinkedList<Preprocessor> preprocessors = new LinkedList<Preprocessor>();

    /**
     * Initialize message pump, create auxiliary window for service messages 
     * @param systemProperties
     */
    public WinEventQueue(WinSystemProperties systemProperties) {
        this.systemProperties = systemProperties;

        WindowProcHandler.registerCallback();
        factory = new WinWindowFactory(this);

        javaWindow = createJavaWindow();
    }

    public void addPreprocessor(Preprocessor preprocessor) {
        preprocessors.add(preprocessor);
    }

    /**
     * Wait for next Windows messages. The re-entrant calls to windowProc()
     * may occur while waiting
     * @return false when WM_QUIT received
     */
    @Override
    public boolean waitEvent() {
        win32.GetMessageW(lastMsg, 0, 0, 0);

        return lastMsg.get_message() != WindowsDefs.WM_QUIT;
    }

    /**
     * Post the service message to the auxiliary window to ensure
     * the method waitEvent() will leave the waiting state
     */
    @Override
    public void awake() {
        if (win32.GetCurrentThreadId() != dispatchThreadID) {
            win32.PostThreadMessageW(dispatchThreadID, WinEvent.WM_AWAKE, 0, 0);
        }
    }

    /**
     * Handle the service message posted to event dispatch thread
     * @param msg - the Windows message
     */
    private void processThreadMessage(Win32.MSG msg) {
        handleEvent(0, msg.get_message(), msg.get_wParam(), msg.get_lParam());
    }
    
    /**
     * Translate key code to typed character for keyboard messages,
     * do nothing for all other messages
     * @param msg - the Windows message to translate
     */
    private void translateMessage(Win32.MSG msg) {
        if (win32.TranslateMessage(msg) == 0) {
            return;
        }

        lastChar = KeyEvent.CHAR_UNDEFINED;
        lastTranslation.setLength(0);

        switch (msg.get_message()) {
        case WindowsDefs.WM_KEYDOWN:
            peekChar(msg, WindowsDefs.WM_CHAR, WindowsDefs.WM_DEADCHAR);
            break;
        case WindowsDefs.WM_SYSKEYDOWN:
            peekChar(msg, WindowsDefs.WM_SYSCHAR, WindowsDefs.WM_SYSDEADCHAR);
            break;
        default:
            peekChar(msg, WindowsDefs.WM_CHAR, WindowsDefs.WM_DEADCHAR);
        }
    }

    /**
     * Determine typed character for WM_KEYDOWN or WM_SYSKEYDOWN message,
     * and store the translation for decoding of subsequent
     * WM_KEYUP or WM_SYSKEYUP messages
     * @param msg - message to find the typed character for
     * @param min - the low bound of range of messages to peek
     * @param max - the high bound of range of messages to peek
     */
    private void peekChar(Win32.MSG msg, int min, int max) {
        while (win32.PeekMessageW(charMsg, msg.get_hwnd(),
                min, max, WindowsDefs.PM_REMOVE) != 0) {

            int msgId = charMsg.get_message();
            int vKey = (int)msg.get_wParam();
            lastChar = (char)charMsg.get_wParam();

            if (msgId == WindowsDefs.WM_CHAR || msgId == WindowsDefs.WM_SYSCHAR) {
                lastTranslation.append(lastChar);
            }

            keyCodeToChar.put(new Integer(vKey), new Character(lastChar));
        }
    }

    /**
     * The callback called by Windows framework to handle the messages
     * @param hwnd - window handle
     * @param msg - message code
     * @param wParam - first message-dependent parameter
     * @param lParam - second message-dependent parameter
     * @return - message-dependent value
     */
    public long windowProc(long hwnd, int msg, long wParam, long lParam) {
        if ((utcOffset < 0) && (lastMsg.get_time() != 0l)) {
            utcOffset = System.currentTimeMillis() - lastMsg.get_time();
        }

        if (preProcessMessage(hwnd, msg,  wParam, lParam, result)) {
            return result[0];
        }
        if (handleEvent(hwnd, msg,  wParam, lParam)) {
            return 0;
        }

        if (msg == WindowsDefs.WM_MOUSEACTIVATE) {
            return WindowsDefs.MA_NOACTIVATE;
        }
        return win32.DefWindowProcW(hwnd, msg, wParam, lParam);
    }

    private boolean handleEvent(long hwnd, int msg, long wParam, long lParam) {
        if (msg == WindowsDefs.WM_KEYUP || msg == WindowsDefs.WM_SYSKEYUP) {
            Character ch = keyCodeToChar.remove(new Integer((int)wParam));
            if (ch != null) {
                lastChar = ch.charValue();
            } else {
                lastChar = KeyEvent.CHAR_UNDEFINED;
            }
        }

        WinEvent event = new WinEvent(hwnd, msg, wParam,
                lParam, utcOffset + lastMsg.get_time(),
                lastTranslation, lastChar, factory);
        if (event.getEventId() != NativeEvent.ID_PLATFORM) {
            addEvent(event);
            return !isKeyUpOrKeyDownMessage(msg);
        }
        return false;
    }

    private boolean isKeyUpOrKeyDownMessage(int msg) {
        return msg == WindowsDefs.WM_KEYDOWN 
            || msg == WindowsDefs.WM_KEYUP 
            || msg == WindowsDefs.WM_SYSKEYDOWN 
            || msg == WindowsDefs.WM_SYSKEYUP;
    }

    /**
     * Call the message preprocessors, handle the Windows messages that
     * should not be passed to the default Windows handler 
     *  
     * @param hwnd - window handle
     * @param msg - message code
     * @param wParam - first message-dependent parameter
     * @param lParam - second message-dependent parameter
     * @param result - result[0] is the return value of the message
     * @return - false if the default Windows handler should be called; 
     */
    private boolean preProcessMessage(long hwnd, int msg, long wParam, long lParam, long[] result) {
        if (msg == WM_PERFORM_TASK && hwnd == javaWindow) {
            Task t = (Task)objAccessor.getObjectFromReference(lParam);
            t.perform();
            return true;
        }
        
        if (msg == WM_PERFORM_LATER && hwnd == javaWindow) {
            Task t = (Task)objAccessor.getObjectFromReference(lParam);
            t.perform();
            objAccessor.releaseGlobalReference(lParam);
            return true;
        }
        for (Iterator<Preprocessor> i = preprocessors.iterator(); i.hasNext(); ) {
            if (i.next().preprocess(hwnd, msg, wParam, lParam, result)) {
                return true;
            }
        }

        switch (msg) {
        case WindowsDefs.WM_CREATE:
            factory.onCreateWindow(hwnd);
            break;

        case WindowsDefs.WM_NCACTIVATE:
            // prevent non-focusable window's non-client area from being
            // changed to indicate its active state
            long hwndOther = lParam;
            NativeWindow wnd = factory.getWindowById(hwnd);
            NativeWindow otherWnd = factory.getWindowById(hwndOther);
            if (wParam != 0 && !wnd.isFocusable() ||
                    wParam == 0 && wnd.isFocusable() &&
                    otherWnd != null && !otherWnd.isFocusable() ) {

                result[0] = 0;
                return true;
            }
            break;
            
        case WindowsDefs.WM_ACTIVATE:
            if (wParam == WindowsDefs.WA_ACTIVE) {
                // while activation of Frame/Dialog
                // [actually focus transfer to focusProxy] is in progress
                // skip all focus events related to windows being activated/deactivated,
                // such spurious events are sometimes generated by Win32
                WinWindow ownd = (WinWindow) factory.getWindowById(lParam);
                long hOwner = lParam;
                // find nearest decorated ancestor window of
                // the window being deactivated
                while ((ownd != null) && ownd.undecorated) {
                    hOwner = win32.GetParent(hOwner);
                    ownd = (WinWindow) factory.getWindowById(hOwner);
                }
                // cancel focus only if
                // this is the window found
                if ((ownd != null) && (hOwner == hwnd)) {
                    result[0] = 0;
                    return true;
                }
            }
            break;

        case WindowsDefs.WM_SETCURSOR:
            short htCode = (short) lParam;
            if (htCode == WindowsDefs.HTCLIENT) {
                result[0] = 0;
                return true;
            }
            break;

        case WindowsDefs.WM_GETMINMAXINFO:
            if (processGetMinMaxInfo(hwnd, lParam)) {
                result[0] = 0;
                return true;
            }
            break;

        case WindowsDefs.WM_ERASEBKGND:
            result[0] = 1;
            return true;

        case WindowsDefs.WM_SYSCOMMAND:
            // suppress system menu activation on F10 key
            if ( (wParam & 0xFFF0) == WindowsDefs.SC_KEYMENU) {
                result[0] = 0;
                return true;
            }
            break;

        case WindowsDefs.WM_SYSCOLORCHANGE:
            result[0] = 0;
            if (hwnd == javaWindow) {
                systemProperties.resetSystemColors();
            }
            break;
            
        case WindowsDefs.WM_SETTINGCHANGE:
            systemProperties.processSettingChange(wParam);
            break;
        case WindowsDefs.WM_IME_STARTCOMPOSITION:            
            return WinIM.onStartComposition(hwnd);
        case WindowsDefs.WM_IME_SETCONTEXT:
            if (wParam != 0l) {
                return WinIM.onActivateContext(hwnd);
            }
            break;
        case WindowsDefs.WM_IME_COMPOSITION:
            WinIM.onComposition(hwnd, lParam);
            break;
        }

        
        return false;
    }

    public ThemeMap getThemeMap() {
        return themeMap;
    }

    /**
     * create invisible auxlitary window to listen to the service messages
     * @return HWND of created window
     */
    private static long createJavaWindow() {
        long hwnd = win32.CreateWindowExW(0,
                WindowProcHandler.windowClassName, "JavaWindow", //$NON-NLS-1$
                0, 0, 0, 0, 0, 0, 0, 0, null);
        int winError = win32.GetLastError();

        if (hwnd == 0) {
            // awt.1C=Failure to create JavaWindow GetLastError returned {0}
            throw new InternalError(Messages.getString("awt.1C", //$NON-NLS-1$
                    winError));
        }

        return hwnd;
    }

    /**
     * Apply window's maximized bounds
     * @param hwnd
     */
    private boolean processGetMinMaxInfo(long hwnd, long ptrMinMaxInfo) {
        WinWindow win = factory.getWinWindowById(hwnd);
        if (win == null) {
            return false;
        }
        Rectangle maxBounds = win.maximizedBounds;
        final int MAX = Integer.MAX_VALUE;
        if (maxBounds == null) {
            return false;
        }
        Win32.MINMAXINFO info = win32.createMINMAXINFO(ptrMinMaxInfo);

        if (maxBounds.width < MAX) {
            info.get_ptMaxSize().set_x(maxBounds.width);
        }
        if (maxBounds.height < MAX) {
            info.get_ptMaxSize().set_y(maxBounds.height);
        }
        if (maxBounds.x < MAX) {
            info.get_ptMaxPosition().set_x(maxBounds.x);
        }
        if (maxBounds.y < MAX) {
            info.get_ptMaxPosition().set_y(maxBounds.y);
        }
        return true;
    }

    @Override
    public long getJavaWindow() {
        return javaWindow;
    }

    /**
     * Interface for message pre-processing
     */
    public interface Preprocessor {

        public boolean preprocess(long hwnd, int msg, long wParam, long lParam, long[] result);

    }

    /**
     * The Windows theme handles currently open, and their names
     */
    public class ThemeMap {
        private final HashMap<String, Long> themes = new HashMap<String, Long>();
        private boolean enabled;

        ThemeMap() {
            enabled = (win32.IsThemeActive() != 0);
        }

        public long get(String name) {
            Long obj = themes.get(name);
            if (obj != null) {
                return obj.longValue();
            }
            long hTheme = open(name);
            themes.put(name, new Long(hTheme));
            return hTheme;
        }

        private long open(String name) {
            Int16Pointer namePtr = bridge.createInt16Pointer(name, false);
            long hTheme = win32.OpenThemeData(javaWindow, namePtr.lock());
            namePtr.unlock();
            return hTheme;
        }

        public boolean isEnabled() {
            return enabled;
        }

        void refresh() {
            enabled = (win32.IsThemeActive() != 0);

            for (Iterator<Map.Entry<String, Long>> it = themes.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, Long> e = it.next();
                long hTheme = e.getValue().longValue();
                if (hTheme != 0) {
                    win32.CloseThemeData(hTheme);
                }
                if (enabled) {
                    hTheme = open(e.getKey());
                } else {
                    hTheme = 0;
                }
                e.setValue(new Long(hTheme));
            }
        }
    }

    @Override
    public void dispatchEvent() {
        if (lastMsg.get_hwnd() == 0) {
            processThreadMessage(lastMsg);
        }
        
        translateMessage(lastMsg);
        win32.DispatchMessageW(lastMsg);
    }

    @Override
    public void performTask(Task task) {
        long ref = objAccessor.getGlobalReference(task);
        win32.SendMessageW(javaWindow, WM_PERFORM_TASK, 0, ref);
        objAccessor.releaseGlobalReference(ref);
    }

    @Override
    public void performLater(Task task) {
        long ref = objAccessor.getGlobalReference(task);
        win32.PostMessageW(javaWindow, WM_PERFORM_LATER, 0, ref);
    }
}
