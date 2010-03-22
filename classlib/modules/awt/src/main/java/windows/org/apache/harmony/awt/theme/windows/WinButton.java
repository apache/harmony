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
package org.apache.harmony.awt.theme.windows;

import java.awt.Dimension;
import java.awt.Graphics;

import org.apache.harmony.awt.state.ButtonState;

import org.apache.harmony.awt.nativebridge.windows.WindowsConsts;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;

/**
 * WinButton
 */
public class WinButton extends WinStyle {

    public static void drawBackground(Graphics g, ButtonState s, WinTheme t) {

        Dimension size = s.getSize();
        WinThemeGraphics wgr = new WinThemeGraphics(g);

        if (t.isXpThemeActive()) {
            wgr.setTheme(t.getXpTheme("Button")); //$NON-NLS-1$
            wgr.fillBackground(size, s.getBackground(), false);
            wgr.drawXpBackground(size, WindowsConsts.BP_PUSHBUTTON,
                    getXpState(s));
        } else {
            wgr.drawClassicBackground(size, WindowsDefs.DFC_BUTTON,
                    getClassicState(s));
        }
        if (s.isFocused()) {
            wgr.drawFocusRect(size, 3);
        }
        wgr.dispose();
    }

    private static int getXpState(ButtonState s) {
        if (!s.isEnabled()) {
            return WindowsConsts.PBS_DISABLED;
        } else if (s.isPressed()) {
            return WindowsConsts.PBS_PRESSED;
        } else if (s.isFocused()) {
            return WindowsConsts.PBS_DEFAULTED;
        }
        return WindowsConsts.PBS_NORMAL;
    }

    private static int getClassicState(ButtonState s) {
        int state = WindowsDefs.DFCS_BUTTONPUSH;
        if (!s.isEnabled()) {
            state |= WindowsDefs.DFCS_INACTIVE;
        } else if (s.isPressed()) {
            state |= WindowsDefs.DFCS_PUSHED;
        }
        return state;
    }
}
