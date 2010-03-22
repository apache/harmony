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
import java.awt.Rectangle;

import org.apache.harmony.awt.state.CheckboxState;

import org.apache.harmony.awt.nativebridge.windows.WindowsConsts;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;

/**
 * WinCheckbox
 */
public class WinCheckbox extends WinStyle {

    static final int CB_SIZE = 13;
    static final int ABS_MARGIN = 4;

    public static void drawBackground(Graphics g,
            CheckboxState s, Rectangle focusRect, WinTheme t) {

        Dimension size = s.getSize();
        int y = size.height / 2 - CB_SIZE / 2;
        Rectangle bounds = new Rectangle(ABS_MARGIN, y, CB_SIZE, CB_SIZE);

        g.setColor(s.getBackground());
        g.fillRect(0, 0, size.width, size.height);

        WinThemeGraphics wgr = new WinThemeGraphics(g);
        wgr.fillBackground(size, s.getBackground(), true);

        if (t.isXpThemeActive()) {
            wgr.setTheme(t.getXpTheme("Button")); //$NON-NLS-1$
            wgr.drawXpBackground(bounds, getXpType(s), getXpState(s));
        } else {
            wgr.drawClassicBackground(bounds, WindowsDefs.DFC_BUTTON,
                    getClassicState(s));
        }
        if (s.isFocused()) {
            wgr.drawFocusRect(focusRect, -1);
        }
        wgr.dispose();
    }

    private static int getXpType(CheckboxState s) {
        return s.isInGroup() ?
                WindowsConsts.BP_RADIOBUTTON : WindowsConsts.BP_CHECKBOX;
    }

    private static int getXpState(CheckboxState s) {
        if (s.isChecked()) {
            if (s.isPressed()) {
                return WindowsConsts.CBS_CHECKEDPRESSED;
            }
            if (!s.isEnabled()) {
                return WindowsConsts.CBS_CHECKEDDISABLED;
            }
            return WindowsConsts.CBS_CHECKEDNORMAL;
        }
        if (s.isPressed()) {
            return WindowsConsts.CBS_UNCHECKEDPRESSED;
        }
        if (!s.isEnabled()) {
            return WindowsConsts.CBS_UNCHECKEDDISABLED;
        }
        return WindowsConsts.CBS_UNCHECKEDNORMAL;
    }

    private static int getClassicState(CheckboxState s) {
        int state = s.isInGroup() ?
                WindowsDefs.DFCS_BUTTONRADIO : WindowsDefs.DFCS_BUTTONCHECK;
        if (s.isPressed()) {
            state |= WindowsDefs.DFCS_PUSHED;
        }
        if (s.isChecked()) {
            state |= WindowsDefs.DFCS_CHECKED;
        }
        if (!s.isEnabled()) {
            state |= WindowsDefs.DFCS_INACTIVE;
        }
        return state;
    }
}
