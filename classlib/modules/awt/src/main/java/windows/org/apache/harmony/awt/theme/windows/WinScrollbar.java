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

import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;
import org.apache.harmony.awt.state.ScrollbarState;


/**
 * WinScrollbar
 */
public class WinScrollbar extends WinStyle {

    public static void draw(Graphics g, ScrollbarState s, WinTheme t) {
        WinThemeGraphics wgr = new WinThemeGraphics(g);
        if (t.isXpThemeActive()) {
            wgr.setTheme(t.getXpTheme("Scrollbar")); //$NON-NLS-1$
            drawXp(wgr, s);
        } else {
            drawClassic(wgr, s);
        }
        wgr.dispose();
    }

    static void drawXp(WinThemeGraphics wgr, ScrollbarState s) {
        if (s.isVertical()) {
            drawXpVertical(wgr, s);
        } else {
            drawXpHorzontal(wgr, s);
        }
    }

    static void drawXpVertical(WinThemeGraphics wgr, ScrollbarState s) {
        boolean enabled = s.isEnabled();

        int state = enabled ? SCRBS_NORMAL : SCRBS_DISABLED;
        if (s.getHighlight() == ScrollbarState.DECREASE_HIGHLIGHT) {
            state = SCRBS_PRESSED;
        }
        wgr.drawXpBackground(getLowerTrackBounds(s), SBP_LOWERTRACKVERT, state);

        state = enabled ? SCRBS_NORMAL : SCRBS_DISABLED;
        if (s.getHighlight() == ScrollbarState.INCREASE_HIGHLIGHT) {
            state = SCRBS_PRESSED;
        }
        wgr.drawXpBackground(getUpperTrackBounds(s), SBP_UPPERTRACKVERT, state);

        state = s.isEnabled() ?
                (s.isSliderPressed() ? SCRBS_PRESSED : SCRBS_NORMAL)
                : SCRBS_DISABLED;
        wgr.drawXpBackground(s.getSliderRect(), SBP_THUMBBTNVERT, state);
        wgr.drawXpBackground(s.getSliderRect(), SBP_GRIPPERVERT, 0);

        state = enabled ?
                (s.isDecreasePressed() ? ABS_UPPRESSED : ABS_UPNORMAL)
                : ABS_UPDISABLED;
        wgr.drawXpBackground(s.getDecreaseRect(), SBP_ARROWBTN, state);

        state = enabled ?
                (s.isIncreasePressed() ? ABS_DOWNPRESSED : ABS_DOWNNORMAL)
                : ABS_DOWNDISABLED;
        wgr.drawXpBackground(s.getIncreaseRect(), SBP_ARROWBTN, state);
    }

    static void drawXpHorzontal(WinThemeGraphics wgr, ScrollbarState s) {
        boolean enabled = s.isEnabled();

        int state = enabled ? SCRBS_NORMAL : SCRBS_DISABLED;
        if (s.getHighlight() == ScrollbarState.DECREASE_HIGHLIGHT) {
            state = SCRBS_PRESSED;
        }
        wgr.drawXpBackground(getLowerTrackBounds(s), SBP_LOWERTRACKHORZ, state);

        state = enabled ? SCRBS_NORMAL : SCRBS_DISABLED;
        if (s.getHighlight() == ScrollbarState.INCREASE_HIGHLIGHT) {
            state = SCRBS_PRESSED;
        }
        wgr.drawXpBackground(getUpperTrackBounds(s), SBP_UPPERTRACKHORZ, state);

        state = s.isEnabled() ?
                (s.isSliderPressed() ? SCRBS_PRESSED : SCRBS_NORMAL)
                : SCRBS_DISABLED;
        wgr.drawXpBackground(s.getSliderRect(), SBP_THUMBBTNHORZ, state);
        wgr.drawXpBackground(s.getSliderRect(), SBP_GRIPPERHORZ, state);

        state = enabled ?
                (s.isDecreasePressed() ? ABS_LEFTPRESSED : ABS_LEFTNORMAL)
                : ABS_LEFTDISABLED;
        wgr.drawXpBackground(s.getDecreaseRect(), SBP_ARROWBTN, state);

        state = enabled ?
                (s.isIncreasePressed() ? ABS_RIGHTPRESSED : ABS_RIGHTNORMAL)
                : ABS_RIGHTDISABLED;
        wgr.drawXpBackground(s.getIncreaseRect(), SBP_ARROWBTN, state);
    }

    private static Rectangle getUpperTrackBounds(ScrollbarState s) {
        Rectangle slider = s.getSliderRect();
        Rectangle button = s.getIncreaseRect();
        Rectangle track = new Rectangle();
        Dimension size = s.getSize();
        if (s.isVertical()) {
            track.width = size.width;
            track.y = button.y;
            track.height = slider.y - track.y;
        } else {
            track.height = size.height;
            track.x = button.x;
            track.width = slider.x - track.x;
        }
        return track;
    }

    private static Rectangle getLowerTrackBounds(ScrollbarState s) {
        Rectangle slider = s.getSliderRect();
        Rectangle button = s.getDecreaseRect();
        Rectangle track = new Rectangle();
        Dimension size = s.getSize();
        if (s.isVertical()) {
            track.width = size.width;
            track.y = slider.y + slider.height;
            track.height = button.y - track.y;
        } else {
            track.height = size.height;
            track.x = slider.x + slider.width;
            track.width = button.x - track.x;
        }
        return track;
    }

    static void drawClassic(WinThemeGraphics wgr, ScrollbarState s) {
        boolean enabled = s.isEnabled();
        fillClassicBackground(wgr, getUpperTrackBounds(s),
                s.getHighlight() == ScrollbarState.INCREASE_HIGHLIGHT);
        fillClassicBackground(wgr, getLowerTrackBounds(s),
                s.getHighlight() == ScrollbarState.DECREASE_HIGHLIGHT);

        int state = WindowsDefs.DFCS_BUTTONPUSH
            | getClassicButtonState(enabled, s.isSliderPressed());

        wgr.drawClassicBackground(s.getSliderRect(),
                WindowsDefs.DFC_BUTTON, state);

        int dir = s.isVertical() ? WindowsDefs.DFCS_SCROLLUP
                                 : WindowsDefs.DFCS_SCROLLLEFT;
        state = dir | getClassicButtonState(enabled, s.isDecreasePressed());
        wgr.drawClassicBackground(s.getDecreaseRect(),
                WindowsDefs.DFC_SCROLL, state);

        dir = s.isVertical() ? WindowsDefs.DFCS_SCROLLDOWN
                             : WindowsDefs.DFCS_SCROLLRIGHT;
        state = dir | getClassicButtonState(enabled, s.isIncreasePressed());
        wgr.drawClassicBackground(s.getIncreaseRect(),
                WindowsDefs.DFC_SCROLL, state);

        if (s.isFocused()) {
            drawFocus(wgr, s);
        }
    }

    private static void fillClassicBackground(WinThemeGraphics wgr,
                                              Rectangle r, boolean highlight) {
        int sysColor1, sysColor2;
        if (highlight) {
            sysColor1 = WindowsDefs.COLOR_3DDKSHADOW;
            sysColor2 = WindowsDefs.COLOR_WINDOWTEXT;
        } else {
            sysColor1 = WindowsDefs.COLOR_WINDOW;
            sysColor2 = WindowsDefs.COLOR_BTNFACE;
        }
        wgr.fillHatchedSysColorRect(r, sysColor1, sysColor2);
    }

    private static void drawFocus(WinThemeGraphics wgr, ScrollbarState s) {
        Rectangle sliderRect = s.getSliderRect();
        if ((sliderRect == null) || sliderRect.isEmpty()) {
            return;
        }
        Rectangle focusRect = new Rectangle(sliderRect);
        focusRect.grow(-2, -2);
        wgr.fillHatchedSysColorRect(focusRect,
                WindowsDefs.COLOR_3DDKSHADOW, WindowsDefs.COLOR_BTNFACE);
    }
}
