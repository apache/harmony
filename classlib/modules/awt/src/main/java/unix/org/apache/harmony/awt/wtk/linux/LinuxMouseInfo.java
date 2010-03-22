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
 * @author Dmitry A. Durnev
 */
package org.apache.harmony.awt.wtk.linux;

import java.awt.Point;

import org.apache.harmony.awt.nativebridge.CLongPointer;
import org.apache.harmony.awt.nativebridge.Int32Pointer;
import org.apache.harmony.awt.nativebridge.Int8Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.wtk.NativeMouseInfo;


/**
 * Implementation of NativeMouseInfo for X11 platform.
 */
public class LinuxMouseInfo implements NativeMouseInfo {

    private static final X11 x11 = X11.getInstance();
    private static final NativeBridge bridge = NativeBridge.getInstance();
    final long display;
    final int screen;

    LinuxMouseInfo(LinuxWindowFactory factory) {
        display = factory.getDisplay();
        screen = factory.getScreen();
    }
    /**
     * @see org.apache.harmony.awt.wtk.NativeMouseInfo#getLocation()
     */
    public Point getLocation() {
        CLongPointer rootReturned = bridge.createCLongPointer(1, false);
        CLongPointer childReturned = bridge.createCLongPointer(1, false);
        Int32Pointer rootX = bridge.createInt32Pointer(1, false);
        Int32Pointer rootY = bridge.createInt32Pointer(1, false);
        Int32Pointer windowX = bridge.createInt32Pointer(1, false);
        Int32Pointer windowY = bridge.createInt32Pointer(1, false);
        Int32Pointer mask = bridge.createInt32Pointer(1, false);

        long windowID = x11.XRootWindow(display, screen);

        x11.XQueryPointer(display, windowID,
                          rootReturned, childReturned,
                          rootX, rootY, windowX,  windowY,
                          mask);

        return new Point(rootX.get(0), rootY.get(0));
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeMouseInfo#getNumberOfButtons()
     */
    public int getNumberOfButtons() {
        int buttonCount = 1; // wild guess
        Int8Pointer mapping = bridge.createInt8Pointer(buttonCount, false);
        buttonCount = x11.XGetPointerMapping(display, mapping, buttonCount);
        return ((buttonCount > 0) ? buttonCount : -1);
    }

}
