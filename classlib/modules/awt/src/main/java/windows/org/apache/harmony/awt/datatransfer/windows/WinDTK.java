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
 * @author Michael Danilov, Pavel Dolgov
 */
package org.apache.harmony.awt.datatransfer.windows;

import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.dnd.peer.DropTargetContextPeer;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.harmony.awt.datatransfer.DTK;
import org.apache.harmony.awt.datatransfer.NativeClipboard;
import org.apache.harmony.awt.nativebridge.windows.Callback;
import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.awt.nativebridge.windows.WinDataTransfer;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;
import org.apache.harmony.awt.wtk.NativeEventQueue.Task;
import org.apache.harmony.awt.wtk.windows.WindowProcHandler;
import org.apache.harmony.misc.accessors.AccessorFactory;
import org.apache.harmony.misc.accessors.ObjectAccessor;

public final class WinDTK extends DTK implements Callback.Handler {
    
    private static final Win32 win32 = Win32.getInstance();
    private static final ObjectAccessor objAccessor = AccessController.doPrivileged(new PrivilegedAction<ObjectAccessor>() {
                                                        public ObjectAccessor run() {
                                                            return AccessorFactory.getObjectAccessor();
                                                        }
                                                    });    
    
    private static final int WM_TASK = WindowsDefs.WM_USER + 1;
    
    private long dataTransferWindow;
    private long windowProc;
    private static final String windowClass = 
        "org.apache.harmony.awt.datatransfer.window"; //$NON-NLS-1$

    @Override
    protected NativeClipboard newNativeClipboard() {
        return new WinClipboard();
    }

    @Override
    protected NativeClipboard newNativeSelection() {
        return null;
    }

    @Override
    public void initDragAndDrop() {
        WinDataTransfer.init();
        
        if (windowProc != 0) {
            return;
        }
        windowProc = Callback.registerCallbackDataTransfer(this);
        WindowProcHandler.registerWindowClass(windowClass, windowProc);
        dataTransferWindow = win32.CreateWindowExW(0, windowClass,
                windowClass, 0, 0, 0, 0, 0, // style, x, y, w, h 
                WindowsDefs.HWND_MESSAGE, 0, 0, null);
    }
    
    @Override
    public void runEventLoop() {
        Win32.MSG msg = win32.createMSG(false);
        while (win32.GetMessageW(msg, 0, 0, 0) != 0) {
            win32.DispatchMessageW(msg);
        }
    }

    @Override
    public DropTargetContextPeer createDropTargetContextPeer(
            DropTargetContext context) {
        return new WinDropTarget(this, context);
    }

    @Override
    public DragSourceContextPeer createDragSourceContextPeer(
            DragGestureEvent dge) {
        return new WinDragSource();
    }
    
    @Override
    public String getDefaultCharset() {
        return "utf-16le"; //$NON-NLS-1$
    }

    public long windowProc(long hwnd, int msg, long wParam, long lParam) {
        if (hwnd == dataTransferWindow && msg == WM_TASK) {
            Task t = (Task)objAccessor.getObjectFromReference(lParam);
            t.perform();
            return 0;
        }
        return win32.DefWindowProcW(hwnd, msg, wParam, lParam);
    }

    public void performTask(Task task) {
        long ref = objAccessor.getGlobalReference(task);
        win32.SendMessageW(dataTransferWindow, WM_TASK, 0, ref);
        objAccessor.releaseGlobalReference(ref);
    }
}
