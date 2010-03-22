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
 * @author Sergey Burlak
 */

package javax.swing.plaf.metal;

import java.awt.Font;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

public class DefaultMetalTheme extends MetalTheme {
    private static ColorUIResource primaryColor1 = new ColorUIResource(0x20, 0x50, 0x80);
    private static ColorUIResource primaryColor2 = new ColorUIResource(0x50, 0x80, 0xC0);
    private static ColorUIResource primaryColor3 = new ColorUIResource(0xC0, 0xE0, 0xFF);
    private static ColorUIResource secondaryColor1 = new ColorUIResource(0x40, 0x40, 0x40);
    private static ColorUIResource secondaryColor2 = new ColorUIResource(0x80, 0x80, 0x80);
    private static ColorUIResource secondaryColor3 = new ColorUIResource(0xC0, 0xC0, 0xC0);

    private FontUIResource systemFont;
    private FontUIResource smallFont;
    private FontUIResource controlFont;
    private FontUIResource userFont;

    public FontUIResource getUserTextFont() {
        if (userFont == null) {
            userFont = getFont("swing.plaf.metal.userFont", new FontUIResource("Dialog", FontUIResource.PLAIN, 12));
        }

        return userFont;
    }

    public FontUIResource getSystemTextFont() {
        if (systemFont == null) {
            systemFont = getFont("swing.plaf.metal.systemFont", new FontUIResource("Dialog", FontUIResource.PLAIN, 12));
        }

        return systemFont;
    }


    public FontUIResource getSubTextFont() {
        if (smallFont == null) {
            smallFont = getFont("swing.plaf.metal.smallFont", new FontUIResource("Dialog", FontUIResource.PLAIN, 10));
        }

        return smallFont;
    }

    public FontUIResource getControlTextFont() {
        if (controlFont == null) {
            controlFont = getFont("swing.plaf.metal.controlFont", new FontUIResource("Dialog", FontUIResource.BOLD, 12));
        }

        return controlFont;
    }

    public FontUIResource getWindowTitleFont() {
        return getControlTextFont();
    }

    public FontUIResource getMenuTextFont() {
        return getControlTextFont();
    }

    protected ColorUIResource getSecondary3() {
        return secondaryColor3;
    }

    protected ColorUIResource getSecondary2() {
        return secondaryColor2;
    }

    protected ColorUIResource getSecondary1() {
        return secondaryColor1;
    }

    protected ColorUIResource getPrimary3() {
        return primaryColor3;
    }

    protected ColorUIResource getPrimary2() {
        return primaryColor2;
    }

    protected ColorUIResource getPrimary1() {
        return primaryColor1;
    }

    public String getName() {
        return "Steel";
    }

    private FontUIResource getFont(final String propertyName, final FontUIResource defaultFont) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null) {
            return new FontUIResource(Font.decode(propertyValue));
        } else {
            return defaultFont;
        }
    }
}

