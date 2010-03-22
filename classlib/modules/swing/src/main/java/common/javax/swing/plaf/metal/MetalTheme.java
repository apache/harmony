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

import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

public abstract class MetalTheme {

    private static final ColorUIResource blackColor = new ColorUIResource(0, 0, 0);
    private static final ColorUIResource whiteColor = new ColorUIResource(255, 255, 255);

    /**
     */
    public MetalTheme() {
    }

    /**
     * Return the theme name
     * @return String name
     */
    public abstract String getName();

    /**
     * Return window title font
     * @return FontUIResource result
     */
    public abstract FontUIResource getWindowTitleFont();

    /**
     * Return user text font
     * @return FontUIResource result
     */
    public abstract FontUIResource getUserTextFont();

    /**
     * Return system text font
     * @return FontUIResource result
     */
    public abstract FontUIResource getSystemTextFont();

    /**
     * Return sub text font
     * @return FontUIResource result
     */
    public abstract FontUIResource getSubTextFont();

    /**
     * Return menu text font
     * @return FontUIResource result
     */
    public abstract FontUIResource getMenuTextFont();

    /**
     * Return control text font
     * @return FontUIResource result
     */
    public abstract FontUIResource getControlTextFont();

    protected abstract ColorUIResource getPrimary3();
    protected abstract ColorUIResource getPrimary2();
    protected abstract ColorUIResource getPrimary1();
    protected abstract ColorUIResource getSecondary3();
    protected abstract ColorUIResource getSecondary2();
    protected abstract ColorUIResource getSecondary1();

    /**
     * Return window title inactive foreground color
     * @return ColorUIResource result
     */
    public ColorUIResource getWindowTitleInactiveForeground() {
        return getBlack();
    }

    /**
     * Return window title inactive background color
     * @return ColorUIResource result
     */
    public ColorUIResource getWindowTitleInactiveBackground() {
        return getSecondary3();
    }

    /**
     * Return window title foreground color
     * @return ColorUIResource result
     */
    public ColorUIResource getWindowTitleForeground() {
        return getBlack();
    }

    /**
     * Return window title background color
     * @return ColorUIResource result
     */
    public ColorUIResource getWindowTitleBackground() {
        return getPrimary3();
    }

    /**
     * Return window background color
     * @return ColorUIResource result
     */
    public ColorUIResource getWindowBackground() {
        return getWhite();
    }

    /**
     * Return white color
     * @return ColorUIResource result
     */
    protected ColorUIResource getWhite() {
        return whiteColor;
    }

    /**
     * Return user text color
     * @return ColorUIResource result
     */
    public ColorUIResource getUserTextColor() {
        return getBlack();
    }

    /**
     * Return user text color
     * @return ColorUIResource result
     */
    public ColorUIResource getTextHighlightColor() {
        return getPrimary3();
    }

    /**
     * Return system text color
     * @return ColorUIResource result
     */
    public ColorUIResource getSystemTextColor() {
        return getBlack();
    }

    /**
     * Return separator foreground color
     * @return ColorUIResource result
     */
    public ColorUIResource getSeparatorForeground() {
        return getPrimary1();
    }

    /**
     * Return separator background color
     * @return ColorUIResource result
     */
    public ColorUIResource getSeparatorBackground() {
        return getWhite();
    }

    /**
     * Return primary control shadow color
     * @return ColorUIResource result
     */
    public ColorUIResource getPrimaryControlShadow() {
        return getPrimary2();
    }

    /**
     * Return primary control info color
     * @return ColorUIResource result
     */
    public ColorUIResource getPrimaryControlInfo() {
        return getBlack();
    }

    /**
     * Return primary control highlight color
     * @return ColorUIResource result
     */
    public ColorUIResource getPrimaryControlHighlight() {
        return getWhite();
    }

    /**
     * Return primary control dark shadow color
     * @return ColorUIResource result
     */
    public ColorUIResource getPrimaryControlDarkShadow() {
        return getPrimary1();
    }

    /**
     * Return primary control color
     * @return ColorUIResource result
     */
    public ColorUIResource getPrimaryControl() {
        return getPrimary3();
    }

    /**
     * Return menu selected foreground color
     * @return ColorUIResource result
     */
    public ColorUIResource getMenuSelectedForeground() {
        return getBlack();
    }

    /**
     * Return menu selected background color
     * @return ColorUIResource result
     */
    public ColorUIResource getMenuSelectedBackground() {
        return getPrimary2();
    }

    /**
     * Return menu foreground color
     * @return ColorUIResource result
     */
    public ColorUIResource getMenuForeground() {
        return getBlack();
    }

    /**
     * Return menu disabled foreground color
     * @return ColorUIResource result
     */
    public ColorUIResource getMenuDisabledForeground() {
        return getSecondary2();
    }

    /**
     * Return menu background color
     * @return ColorUIResource result
     */
    public ColorUIResource getMenuBackground() {
        return getSecondary3();
    }

    /**
     * Return inactive system text color
     * @return ColorUIResource result
     */
    public ColorUIResource getInactiveSystemTextColor() {
        return getSecondary2();
    }

    /**
     * Return inactive control text color
     * @return ColorUIResource result
     */
    public ColorUIResource getInactiveControlTextColor() {
        return getSecondary2();
    }

    /**
     * Return highlighted text color
     * @return ColorUIResource result
     */
    public ColorUIResource getHighlightedTextColor() {
        return getBlack();
    }

    /**
     * Return focus color
     * @return ColorUIResource result
     */
    public ColorUIResource getFocusColor() {
        return getPrimary2();
    }

    /**
     * Return desktop color
     * @return ColorUIResource result
     */
    public ColorUIResource getDesktopColor() {
        return getPrimary2();
    }

    /**
     * Return control text color
     * @return ColorUIResource result
     */
    public ColorUIResource getControlTextColor() {
        return getBlack();
    }

    /**
     * Return control shadow color
     * @return ColorUIResource result
     */
    public ColorUIResource getControlShadow() {
        return getSecondary2();
    }

    /**
     * Return control info color
     * @return ColorUIResource result
     */
    public ColorUIResource getControlInfo() {
        return getBlack();
    }

    /**
     * Return control highlight color
     * @return ColorUIResource result
     */
    public ColorUIResource getControlHighlight() {
        return getWhite();
    }

    /**
     * Return control disabled color
     * @return ColorUIResource result
     */
    public ColorUIResource getControlDisabled() {
        return getSecondary2();
    }

    /**
     * Return control dark shadow color
     * @return ColorUIResource result
     */
    public ColorUIResource getControlDarkShadow() {
        return getSecondary1();
    }

    /**
     * Return control color
     * @return ColorUIResource result
     */
    public ColorUIResource getControl() {
        return getSecondary3();
    }

    /**
     * Return black color
     * @return ColorUIResource result
     */
    protected ColorUIResource getBlack() {
        return blackColor;
    }

    /**
     * Return selected accelerator color
     * @return ColorUIResource result
     */
    public ColorUIResource getAcceleratorSelectedForeground() {
        return getBlack();
    }

    /**
     * Return accelerator foreground color
     * @return ColorUIResource result
     */
    public ColorUIResource getAcceleratorForeground() {
        return getPrimary1();
    }

    /**
     * Add custom entries to the defaults table
     * @param uiDefs defaults table
     */
    public void addCustomEntriesToTable(final UIDefaults uiDefs) {
    }
}

