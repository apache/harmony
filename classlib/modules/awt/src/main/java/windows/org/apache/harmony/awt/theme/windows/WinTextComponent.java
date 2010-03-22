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
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.SystemColor;

import org.apache.harmony.awt.nativebridge.windows.WindowsConsts;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;
import org.apache.harmony.awt.state.TextComponentState;


public class WinTextComponent extends WinStyle {

    public static void drawBackground(Graphics g, TextComponentState s,
                                      WinTheme t) {
        
        Color c = (s.isEnabled() ? s.getBackground() : SystemColor.control);

        WinThemeGraphics wgr = new WinThemeGraphics(g);

        if (t.isXpThemeActive()) {

            wgr.setTheme(t.getXpTheme("Edit")); //$NON-NLS-1$
            int flags = (s.isEnabled() ? WindowsConsts.ETS_NORMAL :
                         WindowsConsts.ETS_DISABLED);
            wgr.drawXpBackground(s.getSize(),
                    WindowsConsts.EP_EDITTEXT, flags);
            if (s.isEnabled() && s.isBackgroundSet()) {
                fill(s, c, wgr);
            }

        } else {
            g.setColor(c);
            fill(s, c, wgr);
            wgr.drawEdge(s.getSize(), WindowsDefs.EDGE_SUNKEN);
        }
        wgr.dispose();
    }

    private static void fill(TextComponentState s, Color c, WinThemeGraphics wgr) {
        Dimension size = s.getSize();
        Rectangle client = s.getClient();
        Insets ins = s.getInsets();
        int x = client.x + client.width;
        int y = client.y + client.height;
        int w = size.width - x;
        int h = size.height - y;
        wgr.fillBackground(client, c, true);
        // fill areas outside of client area &&
        // scrollbars
        wgr.fillBackground(x, y, w, h, c, true);
        wgr.fillBackground(0, size.height - ins.bottom, 
                           size.width, ins.bottom, c, true);
        wgr.fillBackground(size.width - ins.right, 0, 
                           ins.right, size.height, c, true);
    }
}
