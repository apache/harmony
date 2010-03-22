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

import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.nativebridge.Int16Pointer;
import org.apache.harmony.awt.nativebridge.windows.Callback;
import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;
import org.apache.harmony.awt.wtk.NativeEventThread;



/**
 * Helper that registers native window class and forwards WindowProc calls to
 * appropriate WinWTK object. This allows running concurrent event dispatch
 * threads
 */
public class WindowProcHandler implements Callback.Handler {

    static final Win32 win32 = Win32.getInstance();

    private static WindowProcHandler instance;

    static final String windowClassName = "org.apache.harmony.awt.wtk.window"; //$NON-NLS-1$

    private final long windowProcPtr;

    public static void registerCallback() {
        if (instance != null) {
            return;
        }

        instance = new WindowProcHandler();
    }

    private WindowProcHandler() {
        windowProcPtr = Callback.registerCallback(this);
        registerWindowClass(windowClassName, windowProcPtr);
    }

    public long windowProc(long hwnd, int msg, long wParam, long lParam) {
        NativeEventThread thread = (NativeEventThread)Thread.currentThread();
        WinWTK wtk = (WinWTK)thread.getWTK();
        if (wtk == null || wtk.getWinEventQueue() == null) {
            return win32.DefWindowProcW(hwnd, msg, wParam, lParam);
        }
        return wtk.getWinEventQueue().windowProc(hwnd, msg, wParam, lParam);
    }

    public static void registerWindowClass(String className, long windowProc) {

        Int16Pointer namePtr = WinEventQueue.bridge.createInt16Pointer(
                className, false);

        Win32.WNDCLASSEXW wndCls = win32.createWNDCLASSEXW(false);

        wndCls.set_cbSize(wndCls.size());
        wndCls.set_style(WindowsDefs.CS_OWNDC);
        wndCls.set_lpfnWndProc(windowProc);
        wndCls.set_hCursor(0);
        wndCls.set_lpszClassName(namePtr);

        short classAtom = win32.RegisterClassExW(wndCls);
        int winError = win32.GetLastError();
        namePtr.unlock();

        if (classAtom == 0) {
            // awt.1A=Failed to register window class {0} GetLastError returned {1}
            throw new InternalError(Messages.getString("awt.1A", //$NON-NLS-1$
                    className, winError));
        }
    }

    public static void registerWindowClass(String className) {
        registerCallback();
        registerWindowClass(className, instance.windowProcPtr);
    }
}