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
package org.apache.harmony.awt.theme.windows;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.SystemColor;

import org.apache.harmony.awt.state.ChoiceState;

import org.apache.harmony.awt.nativebridge.windows.WindowsConsts;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;


/**
 * WinChoice
 */
public class WinChoice extends WinStyle {

    public static void drawBackground(Graphics g, ChoiceState s, WinTheme t) {
        Dimension size = s.getSize();

        WinThemeGraphics wgr = new WinThemeGraphics(g);

        if (t.isXpThemeActive()) {

            wgr.setTheme(t.getXpTheme("Edit")); //$NON-NLS-1$
            wgr.drawXpBackground(size,
                    WindowsConsts.EP_EDITTEXT, WindowsConsts.ETS_NORMAL);

            wgr.setTheme(t.getXpTheme("Combobox")); //$NON-NLS-1$
            Rectangle r = s.getButtonBounds();
            r.grow(1, 1);
            wgr.drawXpBackground(r,
                    WindowsConsts.CP_DROPDOWNBUTTON, getXpButtonState(s));


        } else {
            Color c = s.isBackgroundSet() ? s.getBackground() : SystemColor.window;
            g.setColor(c);
            wgr.fillBackground(1, 1, size.width-2, size.height-2, c, true);
            wgr.drawEdge(size, WindowsDefs.EDGE_SUNKEN);
            wgr.drawClassicBackground(s.getButtonBounds(), DFC_SCROLL,
                    getClassicButtonState(s));
        }

        if (s.isFocused()) {
            Rectangle r = s.getTextBounds();
            r.grow(1, 0);
            g.setColor(SystemColor.textHighlight);
            g.fillRect(r.x, r.y, r.width, r.height);
            wgr.drawFocusRect(r, 0);
        }

        wgr.dispose();
    }

    private static int getXpButtonState(ChoiceState s) {
        if (s.isPressed()) {
            return WindowsConsts.CBXS_PRESSED;
        }
        if (!s.isEnabled()) {
            return WindowsConsts.CBXS_DISABLED;
        }
        return WindowsConsts.CBXS_NORMAL;
    }

    private static int getClassicButtonState(ChoiceState s) {
        int state = DFCS_SCROLLCOMBOBOX;
        if (!s.isEnabled()) {
            state |= DFCS_INACTIVE;
        }
        if (s.isPressed()) {
            state |= DFCS_PUSHED;
        }
        return state;
    }
}
