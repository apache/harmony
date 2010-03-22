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
import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

public class MetalThemeTest extends SwingTestCase {
    private TestMetalTheme metalTheme;

    @Override
    public void setUp() {
        metalTheme = new TestMetalTheme();
    }

    @Override
    public void tearDown() {
        metalTheme = null;
    }

    public void testGetWindowTitleInactiveForeground() {
        assertSame(metalTheme.getWindowTitleInactiveForeground(), metalTheme.getBlackPublic());
    }

    public void testGetWindowTitleInactiveBackground() {
        assertEquals(metalTheme.getSecondary3(), metalTheme.getWindowTitleInactiveBackground());
    }

    public void testGetWindowTitleForeground() {
        assertSame(metalTheme.getWindowTitleForeground(), metalTheme.getBlackPublic());
    }

    public void testGetWindowTitleBackground() {
        assertEquals(metalTheme.getPrimary3(), metalTheme.getWindowTitleBackground());
    }

    public void testGetWindowBackground() {
        assertSame(metalTheme.getWindowBackground(), metalTheme.getWhitePublic());
    }

    public void testGetWhite() {
        assertEquals(new ColorUIResource(255, 255, 255), metalTheme.getWhitePublic());
    }

    public void testGetUserTextColor() {
        assertSame(metalTheme.getUserTextColor(), metalTheme.getBlackPublic());
    }

    public void testGetTextHighlightColor() {
        assertEquals(metalTheme.getPrimary3(), metalTheme.getTextHighlightColor());
    }

    public void testGetSystemTextColor() {
        assertSame(metalTheme.getSystemTextColor(), metalTheme.getBlackPublic());
    }

    public void testGetSeparatorForeground() {
        assertEquals(metalTheme.getPrimary1(), metalTheme.getSeparatorForeground());
    }

    public void testGetSeparatorBackground() {
        assertSame(metalTheme.getSeparatorBackground(), metalTheme.getWhitePublic());
    }

    public void testGetPrimaryControlShadow() {
        assertEquals(metalTheme.getPrimary2(), metalTheme.getPrimaryControlShadow());
    }

    public void testGetPrimaryControlInfo() {
        assertSame(metalTheme.getPrimaryControlInfo(), metalTheme.getBlackPublic());
    }

    public void testGetPrimaryControlHighlight() {
        assertSame(metalTheme.getPrimaryControlHighlight(), metalTheme.getWhitePublic());
    }

    public void testGetPrimaryControlDarkShadow() {
        assertEquals(metalTheme.getPrimary1(), metalTheme.getPrimaryControlDarkShadow());
    }

    public void testGetPrimaryControl() {
        assertEquals(metalTheme.getPrimary3(), metalTheme.getPrimaryControl());
    }

    public void testGetMenuSelectedForeground() {
        assertSame(metalTheme.getMenuSelectedForeground(), metalTheme.getBlackPublic());
    }

    public void testGetMenuSelectedBackground() {
        assertEquals(metalTheme.getPrimary2(), metalTheme.getMenuSelectedBackground());
    }

    public void testGetMenuForeground() {
        assertSame(metalTheme.getMenuForeground(), metalTheme.getBlackPublic());
    }

    public void testGetMenuDisabledForeground() {
        assertEquals(metalTheme.getSecondary2(), metalTheme.getMenuDisabledForeground());
    }

    public void testGetMenuBackground() {
        assertEquals(metalTheme.getSecondary3(), metalTheme.getMenuBackground());
    }

    public void testGetInactiveSystemTextColor() {
        assertEquals(metalTheme.getSecondary2(), metalTheme.getInactiveSystemTextColor());
    }

    public void testGetInactiveControlTextColor() {
        assertEquals(metalTheme.getSecondary2(), metalTheme.getInactiveControlTextColor());
    }

    public void testGetHighlightedTextColor() {
        assertSame(metalTheme.getHighlightedTextColor(), metalTheme.getBlackPublic());
    }

    public void testGetFocusColor() {
        assertEquals(metalTheme.getPrimary2(), metalTheme.getFocusColor());
    }

    public void testGetDesktopColor() {
        assertEquals(metalTheme.getPrimary2(), metalTheme.getDesktopColor());
    }

    public void testGetControlTextColor() {
        assertSame(metalTheme.getControlTextColor(), metalTheme.getBlackPublic());
    }

    public void testGetControlShadow() {
        assertEquals(metalTheme.getSecondary2(), metalTheme.getControlShadow());
    }

    public void testGetControlInfo() {
        assertSame(metalTheme.getControlInfo(), metalTheme.getBlackPublic());
    }

    public void testGetControlHighlight() {
        assertSame(metalTheme.getControlHighlight(), metalTheme.getWhitePublic());
    }

    public void testGetControlDisabled() {
        assertEquals(metalTheme.getSecondary2(), metalTheme.getControlDisabled());
    }

    public void testGetControlDarkShadow() {
        assertEquals(metalTheme.getSecondary1(), metalTheme.getControlDarkShadow());
    }

    public void testGetControl() {
        assertEquals(metalTheme.getSecondary3(), metalTheme.getControl());
    }

    public void testGetBlack() {
        assertEquals(new ColorUIResource(0, 0, 0), metalTheme.getBlackPublic());
    }

    public void testGetAcceleratorSelectedForeground() {
        assertSame(metalTheme.getAcceleratorSelectedForeground(), metalTheme.getBlackPublic());
    }

    public void testGetAcceleratorForeground() {
        assertEquals(metalTheme.getPrimary1(), metalTheme.getAcceleratorForeground());
    }

    public void testAddCustomEntriesToTable() {
        UIDefaults defs = new UIDefaults();
        metalTheme.addCustomEntriesToTable(defs);
        assertEquals(0, defs.size());
        metalTheme.addCustomEntriesToTable(null);
    }
}

class TestMetalTheme extends MetalTheme {
    public ColorUIResource getWhitePublic() {
        return getWhite();
    }

    public ColorUIResource getBlackPublic() {
        return getBlack();
    }

    @Override
    public String getName() {
        return "name";
    }

    @Override
    protected ColorUIResource getPrimary1() {
        return new ColorUIResource(1, 1, 1);
    }

    @Override
    protected ColorUIResource getPrimary2() {
        return new ColorUIResource(1, 1, 2);
    }

    @Override
    protected ColorUIResource getPrimary3() {
        return new ColorUIResource(1, 1, 3);
    }

    @Override
    protected ColorUIResource getSecondary1() {
        return new ColorUIResource(1, 1, 4);
    }

    @Override
    protected ColorUIResource getSecondary2() {
        return new ColorUIResource(1, 1, 5);
    }

    @Override
    protected ColorUIResource getSecondary3() {
        return new ColorUIResource(1, 1, 6);
    }

    @Override
    public FontUIResource getControlTextFont() {
        return null;
    }

    @Override
    public FontUIResource getMenuTextFont() {
        return null;
    }

    @Override
    public FontUIResource getSubTextFont() {
        return null;
    }

    @Override
    public FontUIResource getSystemTextFont() {
        return null;
    }

    @Override
    public FontUIResource getUserTextFont() {
        return null;
    }

    @Override
    public FontUIResource getWindowTitleFont() {
        return null;
    }
}
