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
package org.apache.harmony.awt.wtk.linux;

import java.awt.Font;
import java.awt.SystemColor;
import java.awt.font.TextAttribute;
import java.awt.im.InputMethodHighlight;
import java.util.Map;

import org.apache.harmony.awt.wtk.SystemProperties;


/**
 * LinuxSystemProperties
 */

public class LinuxSystemProperties implements SystemProperties {

    final LinuxWindowFactory factory;

    LinuxSystemProperties(LinuxWindowFactory factory) {
        this.factory = factory;
    }

    public int getSystemColorARGB(int index) {
        switch(index) {
        // Grey-blue
        case SystemColor.ACTIVE_CAPTION:
            return 0xff336699;
        case SystemColor.ACTIVE_CAPTION_BORDER:
            return 0xffcccccc;
        case SystemColor.ACTIVE_CAPTION_TEXT:
            return 0xffffffff;
        // Light grey
        case SystemColor.CONTROL:
            return 0xffdddddd;
        // Dark grey
        case SystemColor.CONTROL_DK_SHADOW:
            return 0xff777777;
        // Almost white
        case SystemColor.CONTROL_HIGHLIGHT:
            return 0xffeeeeee;
        // White
        case SystemColor.CONTROL_LT_HIGHLIGHT:
            return 0xffffffff;
        // Grey
        case SystemColor.CONTROL_SHADOW:
            return 0xff999999;
        // Black
        case SystemColor.CONTROL_TEXT:
            return 0xff000000;
        // Dark blue
        case SystemColor.DESKTOP:
            return 0xff224466;
        // Another grey
        case SystemColor.INACTIVE_CAPTION:
            return 0xff888888;
        // Grey
        case SystemColor.INACTIVE_CAPTION_BORDER:
            return 0xffcccccc;
        // Light grey
        case SystemColor.INACTIVE_CAPTION_TEXT:
            return 0xffdddddd;
        // Almost white
        case SystemColor.INFO:
            return 0xffeeeeee;
        case SystemColor.INFO_TEXT:
            return 0xff222222;
        // Light grey
        case SystemColor.MENU:
            return 0xffdddddd;
        // Black
        case SystemColor.MENU_TEXT:
            return 0xff000000;
        // Grey
        case SystemColor.SCROLLBAR:
            return 0xffcccccc;
        // White
        case SystemColor.TEXT:
            return 0xffffffff;
        // Grey blue
        case SystemColor.TEXT_HIGHLIGHT:
            return 0xff336699;
        // White
        case SystemColor.TEXT_HIGHLIGHT_TEXT:
            return 0xffffffff;
        // Almost white
        case SystemColor.TEXT_INACTIVE_TEXT:
            return 0xffdddddd;
        // Black
        case SystemColor.TEXT_TEXT:
            return 0xff000000;
        // White
        case SystemColor.WINDOW:
            return 0xffffffff;
        // Black
        case SystemColor.WINDOW_BORDER:
            return 0xff000000;
        // Black
        case SystemColor.WINDOW_TEXT:
            return 0xff000000;
        }
        // Black
        return 0xFF000000;
    }

    public Font getDefaultFont() {
        // Default font parameters are described 
        // in java.awt.Font specification
        return new Font("Dialog", Font.PLAIN, 12); //$NON-NLS-1$
    }

    public void init(Map desktopProperties) {
    }

    public void mapInputMethodHighlight(InputMethodHighlight highlight, Map map) {
        TextAttribute key = TextAttribute.SWAP_COLORS;
        if (highlight.isSelected()) {
            map.put(key, TextAttribute.SWAP_COLORS_ON);
            return;
        }
        switch(highlight.getState()) {
        case InputMethodHighlight.RAW_TEXT:
            key = TextAttribute.WEIGHT;
            map.put(key, TextAttribute.WEIGHT_BOLD);
            break;
        case InputMethodHighlight.CONVERTED_TEXT:
            key = TextAttribute.INPUT_METHOD_UNDERLINE;
            map.put(key, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
            break;
        }
    }
}
