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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.harmony.awt.nativebridge.CLongPointer;
import org.apache.harmony.awt.nativebridge.Int32Pointer;
import org.apache.harmony.awt.nativebridge.Int8Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.X11Defs;
import org.apache.harmony.awt.wtk.CreationParams;
import org.apache.harmony.awt.wtk.NativeWindow;
import org.apache.harmony.awt.wtk.WindowFactory;


public final class LinuxWindowFactory implements WindowFactory {

    private static final X11 x11 = X11.getInstance();
    private static final NativeBridge bridge = NativeBridge.getInstance();

    private final XServerConnection xConnection = XServerConnection.getInstance();
    private final long display = xConnection.getDisplay();
    private final int screen = xConnection.getScreen();
    final WindowManager wm;

    private final long javaWindow;
    private final LinuxWindowMap allWindows = new LinuxWindowMap();

    /**
     * Returns current root window id.
     * @return root window id
     */
    long getRootWindow() {
        return x11.XRootWindow(display, screen);
    }

    public long getDisplay() {
        return display;
    }

    public int getScreen() {
        return screen;
    }

    public long getDisplayImpl() {
        return display;
    }

    public int getScreenImpl() {
        return screen;
    }

    public LinuxWindowFactory() {
        javaWindow = x11.XCreateSimpleWindow(display, x11.XDefaultRootWindow(display),
                0, 0, 1, 1, 0, 0, 0);
        x11.XSelectInput(display, javaWindow, X11Defs.StructureNotifyMask);

        long rootWindow = getRootWindow();
        X11.XWindowAttributes attributes = x11.createXWindowAttributes(false);
        x11.XGetWindowAttributes(display, rootWindow, attributes);
        x11.XSelectInput(
            display,
            rootWindow,
            attributes.get_your_event_mask() | X11Defs.StructureNotifyMask);

        wm = new WindowManager(this);
    }

    public NativeWindow createWindow(CreationParams p) {
        LinuxWindow lw = new LinuxWindow(this, p);
        allWindows.put(lw);
        if (!lw.isChild() && !lw.isUndecorated()) {
            p.child = true;
            p.parentId = lw.getId();
            p.x = 0;
            p.y = 0;
            ContentWindow cw = new ContentWindow(this, p);
            allWindows.put(cw);
            lw.setContentWindow(cw);
            lw = cw;
        }
        x11.XFlush(display);
        return lw;
    }

    public NativeWindow getWindowById(long id) {
        return allWindows.get(id);
    }

    boolean validWindowId(long id) {
        return allWindows.contains(id);
    }

    void onWindowDispose(long windowID) {
        allWindows.remove(windowID);
    }

    public String getAtomName(long atom) {
        long atomNamePtr = x11.XGetAtomName(display, atom);
        Int8Pointer rawName = bridge.createInt8Pointer(atomNamePtr);
        String atomName = rawName.getStringUTF();
        x11.XFree(atomNamePtr);

        return atomName;
    }

    public long internAtom(String name) {
        return x11.XInternAtom(display, name, 0);
    }

    /**
     * @see org.apache.harmony.awt.wtk.WindowFactory#getWindowFromPoint(java.awt.Point)
     */
    public NativeWindow getWindowFromPoint(Point p) {
        long rootID = getRootWindow();
        long childID = rootID;
        Int32Pointer x = bridge.createInt32Pointer(1, false);
        Int32Pointer y = bridge.createInt32Pointer(1, false);
        CLongPointer childWindow = bridge.createCLongPointer(1, false);

        //recursevily ask for child containing p
        //until the deepest child is found
        //or until our top-level window is found
        while (childID != X11Defs.None) {
            x11.XTranslateCoordinates(display, rootID, childID,
                    p.x, p.y, x, y, childWindow);
            long nextID = childWindow.get(0);
            // avoid endless loop
            if (childID == nextID) {
                return null;
            }
            childID = nextID;
            if (validWindowId(childID)) {
                break;
            }
        }

        LinuxWindow win = ((childID != 0) ? (LinuxWindow) getWindowById(childID)
                                         : null);
        if (win != null) {
            LinuxWindow content = win.getContentWindow();
            if ((content != null) && validWindowId(content.getId())) {
                return content;
            }
        }
        return win;

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

    public NativeWindow attachWindow(long nativeWindowId) {
        return new LinuxWindow(nativeWindowId, this);
    }

    public void setCaretPosition(int x, int y) {
    }

    /**
     * @see org.apache.harmony.awt.wtk.WindowFactory#getWindowSizeById(long)
     */
    public Dimension getWindowSizeById(long id) {
        Int32Pointer x = bridge.createInt32Pointer(1, false);
        Int32Pointer y = bridge.createInt32Pointer(1, false);
        Int32Pointer w = bridge.createInt32Pointer(1, false);
        Int32Pointer h = bridge.createInt32Pointer(1, false);
        CLongPointer root = bridge.createCLongPointer(1, false);
        Int32Pointer border = bridge.createInt32Pointer(1, false);
        Int32Pointer depth = bridge.createInt32Pointer(1, false);

        x11.XGetGeometry(display, id, root, x, y, w, h, border, depth);
        return new Dimension(w.get(0), h.get(0));
    }

    public long getJavaWindow() {
        return javaWindow;
    }

}
