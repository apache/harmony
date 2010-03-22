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
package org.apache.harmony.awt.wtk.windows;

import java.awt.Point;

import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;
import org.apache.harmony.awt.wtk.NativeMouseInfo;

/**
 * Implementation of NativeMouseInfo for Windows platform.
 */
public class WinMouseInfo implements NativeMouseInfo {

    private static final Win32 win32 = Win32.getInstance();

    /**
     * @see org.apache.harmony.awt.wtk.NativeMouseInfo#getLocation()
     */
    public Point getLocation() {
        Win32.POINT pt = WinEventQueue.win32.createPOINT(false);
        win32.GetCursorPos(pt);
        return new Point(pt.get_x(), pt.get_y());
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeMouseInfo#getNumberOfButtons()
     */
    public int getNumberOfButtons() {
        int nButtons = win32.GetSystemMetrics(WindowsDefs.SM_CMOUSEBUTTONS);
        return ((nButtons > 0) ? nButtons : -1);
    }

}
