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

import javax.swing.SwingTestCase;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

public class MetalLookAndFeelTest extends SwingTestCase {
    private DefaultMetalTheme defaultMetalTheme;

    @Override
    public void setUp() {
        defaultMetalTheme = new DefaultMetalTheme();
    }

    @Override
    public void tearDown() {
        defaultMetalTheme = null;
    }

    public void testSize() throws Exception {
        if (isHarmony()) {
            assertEquals(559, new MetalLookAndFeel().getDefaults().size());
        }
    }

    public void testGetAcceleratorForeground() {
        checkColor(MetalLookAndFeel.getAcceleratorForeground(), defaultMetalTheme
                .getAcceleratorForeground());
    }

    public void testGetAcceleratorSelectedForeground() {
        checkColor(MetalLookAndFeel.getAcceleratorSelectedForeground(), defaultMetalTheme
                .getAcceleratorSelectedForeground());
    }

    public void testGetBlack() {
        checkColor(MetalLookAndFeel.getBlack(), defaultMetalTheme.getBlack());
    }

    public void testGetControl() {
        checkColor(MetalLookAndFeel.getControl(), defaultMetalTheme.getControl());
    }

    public void testGetControlDarkShadow() {
        checkColor(MetalLookAndFeel.getControlDarkShadow(), defaultMetalTheme
                .getControlDarkShadow());
    }

    public void testGetControlDisabled() {
        checkColor(MetalLookAndFeel.getControlDisabled(), defaultMetalTheme
                .getControlDisabled());
    }

    public void testGetControlHighlight() {
        checkColor(MetalLookAndFeel.getControlHighlight(), defaultMetalTheme
                .getControlHighlight());
    }

    public void testGetControlInfo() {
        checkColor(MetalLookAndFeel.getControlInfo(), defaultMetalTheme.getControlInfo());
    }

    public void testGetControlShadow() {
        checkColor(MetalLookAndFeel.getControlShadow(), defaultMetalTheme.getControlShadow());
    }

    public void testGetControlTextColor() {
        checkColor(MetalLookAndFeel.getControlTextColor(), defaultMetalTheme
                .getControlTextColor());
    }

    public void testGetControlTextFont() {
        FontUIResource font = MetalLookAndFeel.getControlTextFont();
        assertNotNull(font);
        assertEquals(defaultMetalTheme.getControlTextFont(), font);
    }

    public void testGetDeskTopColor() {
        checkColor(MetalLookAndFeel.getDesktopColor(), defaultMetalTheme.getDesktopColor());
    }

    public void testGetFocusColor() {
        checkColor(MetalLookAndFeel.getFocusColor(), defaultMetalTheme.getFocusColor());
    }

    public void testGetHighlightedTextColor() {
        checkColor(MetalLookAndFeel.getHighlightedTextColor(), defaultMetalTheme
                .getHighlightedTextColor());
    }

    public void testGetInactiveControlTextColor() {
        checkColor(MetalLookAndFeel.getInactiveControlTextColor(), defaultMetalTheme
                .getInactiveControlTextColor());
    }

    public void testGetInactiveSystemTextColor() {
        checkColor(MetalLookAndFeel.getInactiveSystemTextColor(), defaultMetalTheme
                .getInactiveSystemTextColor());
    }

    public void testGetMenuBackground() {
        checkColor(MetalLookAndFeel.getMenuBackground(), defaultMetalTheme.getMenuBackground());
    }

    public void testGetMenuDisabledBackground() {
        checkColor(MetalLookAndFeel.getMenuDisabledForeground(), defaultMetalTheme
                .getMenuDisabledForeground());
    }

    public void testGetMenuForeground() {
        checkColor(MetalLookAndFeel.getMenuForeground(), defaultMetalTheme.getMenuForeground());
    }

    public void testGetMenuSelectedBackground() {
        checkColor(MetalLookAndFeel.getMenuSelectedBackground(), defaultMetalTheme
                .getMenuSelectedBackground());
    }

    public void testGetMenuSelectedForeground() {
        checkColor(MetalLookAndFeel.getMenuSelectedForeground(), defaultMetalTheme
                .getMenuSelectedForeground());
    }

    public void testGetMenuTextFont() {
        FontUIResource font = MetalLookAndFeel.getMenuTextFont();
        assertNotNull(font);
        assertEquals(defaultMetalTheme.getMenuTextFont(), font);
    }

    public void testGetName() {
        assertNotNull(new MetalLookAndFeel().getName());
    }

    public void testGetId() {
        assertNotNull(new MetalLookAndFeel().getID());
    }

    public void testGetPrimaryControl() {
        checkColor(MetalLookAndFeel.getPrimaryControl(), defaultMetalTheme.getPrimaryControl());
    }

    public void testGetPrimaryControlDarkShadow() {
        checkColor(MetalLookAndFeel.getPrimaryControlDarkShadow(), defaultMetalTheme
                .getPrimaryControlDarkShadow());
    }

    public void testGetPrimaryControlHighlight() {
        checkColor(MetalLookAndFeel.getPrimaryControlHighlight(), defaultMetalTheme
                .getPrimaryControlHighlight());
    }

    public void testGetPrimaryControlInfo() {
        checkColor(MetalLookAndFeel.getPrimaryControlInfo(), defaultMetalTheme
                .getPrimaryControlInfo());
    }

    public void testGetPrimaryControlShadow() {
        checkColor(MetalLookAndFeel.getPrimaryControlShadow(), defaultMetalTheme
                .getPrimaryControlShadow());
    }

    public void testGetSeparatorBackground() {
        checkColor(MetalLookAndFeel.getSeparatorBackground(), defaultMetalTheme
                .getSeparatorBackground());
    }

    public void testGetSeparatorForeground() {
        checkColor(MetalLookAndFeel.getSeparatorForeground(), defaultMetalTheme
                .getSeparatorForeground());
    }

    public void testSubTextFont() {
        FontUIResource font = MetalLookAndFeel.getSubTextFont();
        assertNotNull(font);
        assertEquals(defaultMetalTheme.getSubTextFont(), font);
    }

    public void testGetSupportWindowDecorations() {
        assertTrue(new MetalLookAndFeel().getSupportsWindowDecorations());
    }

    public void testGetSystemTestColor() {
        checkColor(MetalLookAndFeel.getSystemTextColor(), defaultMetalTheme
                .getSystemTextColor());
    }

    public void testSystemTextFont() {
        FontUIResource font = MetalLookAndFeel.getSystemTextFont();
        assertNotNull(font);
        assertEquals(defaultMetalTheme.getSystemTextFont(), font);
    }

    public void testGetTextHighlightColor() {
        checkColor(MetalLookAndFeel.getTextHighlightColor(), defaultMetalTheme
                .getTextHighlightColor());
    }

    public void testUserTextFont() {
        FontUIResource font = MetalLookAndFeel.getUserTextFont();
        assertNotNull(font);
        assertEquals(defaultMetalTheme.getUserTextFont(), font);
    }

    public void testGetUserTextColor() {
        checkColor(MetalLookAndFeel.getUserTextColor(), defaultMetalTheme.getUserTextColor());
    }

    public void testGetWhite() {
        checkColor(MetalLookAndFeel.getWhite(), defaultMetalTheme.getWhite());
    }

    public void testGetWindowBackground() {
        checkColor(MetalLookAndFeel.getWindowBackground(), defaultMetalTheme
                .getWindowBackground());
    }

    public void testGetWindowTitleBackground() {
        checkColor(MetalLookAndFeel.getWindowTitleBackground(), defaultMetalTheme
                .getWindowTitleBackground());
    }

    public void testGetWindowTitleFont() {
        FontUIResource font = MetalLookAndFeel.getWindowTitleFont();
        assertNotNull(font);
        assertEquals(defaultMetalTheme.getWindowTitleFont(), font);
    }

    public void testGetWindowTitleForeground() {
        checkColor(MetalLookAndFeel.getWindowTitleForeground(), defaultMetalTheme
                .getWindowTitleForeground());
    }

    public void testGetWindowTitleInactiveBackground() {
        checkColor(MetalLookAndFeel.getWindowTitleInactiveBackground(), defaultMetalTheme
                .getWindowTitleInactiveBackground());
    }

    public void testGetWindowTitleInactiveForeground() {
        checkColor(MetalLookAndFeel.getWindowTitleInactiveForeground(), defaultMetalTheme
                .getWindowTitleInactiveForeground());
    }

    public void testIsNativeLookAndFeel() {
        assertFalse(new MetalLookAndFeel().isNativeLookAndFeel());
    }

    public void testIsSupportedLookAndFeel() {
        assertTrue(new MetalLookAndFeel().isSupportedLookAndFeel());
    }

    private void checkColor(final ColorUIResource color, final ColorUIResource themeColor) {
        assertNotNull(color);
        assertSame(color, themeColor);
    }
}
