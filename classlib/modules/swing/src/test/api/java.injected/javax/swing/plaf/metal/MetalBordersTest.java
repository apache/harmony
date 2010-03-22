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

import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;
import javax.swing.border.AbstractBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.metal.MetalBorders.ButtonBorder;
import javax.swing.plaf.metal.MetalBorders.MenuBarBorder;
import javax.swing.plaf.metal.MetalBorders.MenuItemBorder;
import javax.swing.plaf.metal.MetalBorders.PopupMenuBorder;

public class MetalBordersTest extends SwingTestCase {
    public void testButtonBorderClass() {
        MetalBorders.ButtonBorder border = new MetalBorders.ButtonBorder();
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(3, 3, 3, 3), border.getBorderInsets(newJComponent()));
        assertSame(ButtonBorder.borderInsets, border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    public void testFlush3DBorderClass() {
        MetalBorders.Flush3DBorder border = new MetalBorders.Flush3DBorder();
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(2, 2, 2, 2), border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    public void testInternalFrameBorderClass() {
        MetalBorders.InternalFrameBorder border = new MetalBorders.InternalFrameBorder();
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(5, 5, 5, 5), border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    public void testFrameBorderClass() {
        MetalBorders.FrameBorder border = new MetalBorders.FrameBorder();
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(5, 5, 5, 5), border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    public void testMenuBarBorderClass() {
        MetalBorders.MenuBarBorder border = new MetalBorders.MenuBarBorder();
        assertFalse(border.isBorderOpaque());
        if (isHarmony()) {
            assertEquals(new Insets(1, 0, 1, 0), border.getBorderInsets(newJComponent()));
            assertSame(MenuBarBorder.borderInsets, border.getBorderInsets(newJComponent()));
        } else {
            assertEquals(new Insets(1, 0, 1, 0), MenuBarBorder.borderInsets);
            assertNotSame(border.getBorderInsets(newJComponent()), border
                    .getBorderInsets(newJComponent()));
            assertEquals(new Insets(0, 0, 2, 0), border.getBorderInsets(newJComponent()));
            assertEquals(border.getBorderInsets(newJComponent()), border.getBorderInsets(
                    newJComponent(), new Insets(0, 0, 0, 0)));
        }
        checkInsets(border);
    }

    public void testMenuItemBorder() {
        MetalBorders.MenuItemBorder border = new MetalBorders.MenuItemBorder();
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(2, 2, 2, 2), MenuItemBorder.borderInsets);
        assertTrue(MenuItemBorder.borderInsets == border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    public void testOptionDialogBorderClass() {
        MetalBorders.OptionDialogBorder border = new MetalBorders.OptionDialogBorder();
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(3, 3, 3, 3), border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    public void testPaletteBorderClass() {
        MetalBorders.PaletteBorder border = new MetalBorders.PaletteBorder();
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(1, 1, 1, 1), border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    public void testPopupMenuBorderClass() {
        MetalBorders.PopupMenuBorder border = new MetalBorders.PopupMenuBorder();
        assertFalse(border.isBorderOpaque());
        if (isHarmony()) {
            assertEquals(new Insets(2, 2, 1, 1), PopupMenuBorder.borderInsets);
        } else {
            assertEquals(new Insets(3, 1, 2, 1), PopupMenuBorder.borderInsets);
        }
        assertTrue(PopupMenuBorder.borderInsets == border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    public void testRolloverButtonBorderClass() {
        MetalBorders.RolloverButtonBorder border = new MetalBorders.RolloverButtonBorder();
        assertFalse(border.isBorderOpaque());
        assertTrue(ButtonBorder.borderInsets == border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    public void testScrollPaneBorderClass() {
        MetalBorders.ScrollPaneBorder border = new MetalBorders.ScrollPaneBorder();
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(1, 1, 2, 2), border.getBorderInsets(newJComponent()));
        assertSame(border.getBorderInsets(newJComponent()), border
                .getBorderInsets(newJComponent()));
        Insets ins = new Insets(0, 1, 0, 2);
        border.getBorderInsets(newJComponent(), ins);
        assertEquals(ins, new Insets(0, 0, 0, 0));
    }

    public void testTableHeaderBorderClass() {
        MetalBorders.TableHeaderBorder border = new MetalBorders.TableHeaderBorder();
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(2, 2, 2, 0), border.editorBorderInsets);
        assertTrue(border.editorBorderInsets == border.getBorderInsets(newJComponent()));
        Insets ins = new Insets(0, 1, 0, 2);
        border.getBorderInsets(newJComponent(), ins);
        assertEquals(ins, new Insets(0, 0, 0, 0));
    }

    public void testTextFieldBorderClass() {
        MetalBorders.TextFieldBorder border = new MetalBorders.TextFieldBorder();
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(2, 2, 2, 2), border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    public void testToggleButtonBorderClass() {
        MetalBorders.ToggleButtonBorder border = new MetalBorders.ToggleButtonBorder();
        assertFalse(border.isBorderOpaque());
        assertTrue(ButtonBorder.borderInsets == border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    public void testToolBarBorderClass() {
        JToolBar testToolBar = new JToolBar();
        MetalBorders.ToolBarBorder border = new MetalBorders.ToolBarBorder();
        assertFalse(border.isBorderOpaque());
        if (isHarmony()) {
            assertEquals(new Insets(2, 16, 2, 2), border.getBorderInsets(testToolBar));
        } else {
            assertEquals(new Insets(1, 16, 3, 2), border.getBorderInsets(testToolBar));
        }
        testToolBar.setOrientation(SwingConstants.VERTICAL);
        if (isHarmony()) {
            assertEquals(new Insets(16, 2, 2, 2), border.getBorderInsets(testToolBar));
        } else {
            assertEquals(new Insets(16, 2, 3, 2), border.getBorderInsets(testToolBar));
        }
        testToolBar.setFloatable(false);
        if (isHarmony()) {
            assertEquals(new Insets(2, 2, 2, 2), border.getBorderInsets(testToolBar));
        } else {
            assertEquals(new Insets(1, 2, 3, 2), border.getBorderInsets(testToolBar));
        }
        testToolBar.setOrientation(SwingConstants.HORIZONTAL);
        if (isHarmony()) {
            assertEquals(new Insets(2, 2, 2, 2), border.getBorderInsets(testToolBar));
        } else {
            assertEquals(new Insets(1, 2, 3, 2), border.getBorderInsets(testToolBar));
        }
        try {
            border.getBorderInsets(new JButton());
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
        checkInsets(border, testToolBar);
    }

    public void testGetToggleButtonBorder() {
        JComponent testButton = newJComponent();
        assertTrue(MetalBorders.getToggleButtonBorder() instanceof BorderUIResource.CompoundBorderUIResource);
        BorderUIResource.CompoundBorderUIResource border = (BorderUIResource.CompoundBorderUIResource) MetalBorders
                .getToggleButtonBorder();
        assertTrue(border.getInsideBorder() instanceof BasicBorders.MarginBorder);
        BasicBorders.MarginBorder insideBorder = (BasicBorders.MarginBorder) border
                .getInsideBorder();
        assertTrue(border.getOutsideBorder() instanceof MetalBorders.ToggleButtonBorder);
        MetalBorders.ToggleButtonBorder outsideBorder = (MetalBorders.ToggleButtonBorder) border
                .getOutsideBorder();
        //        assertEquals(new Insets(2, 14, 2, 14), insideBorder.getBorderInsets(testButton));
        assertEquals(new Insets(3, 3, 3, 3), outsideBorder.getBorderInsets(testButton));
        assertFalse(insideBorder.isBorderOpaque());
        assertFalse(outsideBorder.isBorderOpaque());
    }

    public void testGetTextFieldBorder() {
        JComponent testButton = newJComponent();
        assertTrue(MetalBorders.getTextFieldBorder() instanceof BorderUIResource.CompoundBorderUIResource);
        BorderUIResource.CompoundBorderUIResource border = (BorderUIResource.CompoundBorderUIResource) MetalBorders
                .getTextFieldBorder();
        assertTrue(border.getInsideBorder() instanceof BasicBorders.MarginBorder);
        BasicBorders.MarginBorder insideBorder = (BasicBorders.MarginBorder) border
                .getInsideBorder();
        assertTrue(border.getOutsideBorder() instanceof MetalBorders.TextFieldBorder);
        MetalBorders.TextFieldBorder outsideBorder = (MetalBorders.TextFieldBorder) border
                .getOutsideBorder();
        //        assertEquals(new Insets(2, 14, 2, 14), insideBorder.getBorderInsets(testButton));
        assertEquals(new Insets(2, 2, 2, 2), outsideBorder.getBorderInsets(testButton));
        assertFalse(insideBorder.isBorderOpaque());
        assertFalse(outsideBorder.isBorderOpaque());
    }

    public void testGetTextBorder() {
        JComponent testButton = newJComponent();
        assertTrue(MetalBorders.getTextBorder() instanceof BorderUIResource.CompoundBorderUIResource);
        BorderUIResource.CompoundBorderUIResource border = (BorderUIResource.CompoundBorderUIResource) MetalBorders
                .getTextBorder();
        assertTrue(border.getInsideBorder() instanceof BasicBorders.MarginBorder);
        BasicBorders.MarginBorder insideBorder = (BasicBorders.MarginBorder) border
                .getInsideBorder();
        assertTrue(border.getOutsideBorder() instanceof MetalBorders.Flush3DBorder);
        MetalBorders.Flush3DBorder outsideBorder = (MetalBorders.Flush3DBorder) border
                .getOutsideBorder();
        //        assertEquals(new Insets(2, 14, 2, 14), insideBorder.getBorderInsets(testButton));
        assertEquals(new Insets(2, 2, 2, 2), outsideBorder.getBorderInsets(testButton));
        assertFalse(insideBorder.isBorderOpaque());
        assertFalse(outsideBorder.isBorderOpaque());
    }

    public void testGetDesktopIconBorder() {
        JComponent testButton = newJComponent();
        assertTrue(MetalBorders.getDesktopIconBorder() instanceof BorderUIResource.CompoundBorderUIResource);
        BorderUIResource.CompoundBorderUIResource border = (BorderUIResource.CompoundBorderUIResource) MetalBorders
                .getDesktopIconBorder();
        assertTrue(border.getInsideBorder() instanceof MatteBorder);
        MatteBorder insideBorder = (MatteBorder) border.getInsideBorder();
        assertTrue(border.getOutsideBorder() instanceof LineBorder);
        LineBorder outsideBorder = (LineBorder) border.getOutsideBorder();
        assertEquals(new Insets(2, 2, 1, 2), insideBorder.getBorderInsets(testButton));
        assertEquals(new Insets(1, 1, 1, 1), outsideBorder.getBorderInsets(testButton));
        assertTrue(insideBorder.isBorderOpaque());
        assertEquals(new ColorUIResource(238, 238, 238), insideBorder.getMatteColor());
        assertFalse(outsideBorder.getRoundedCorners());
        assertTrue(outsideBorder.isBorderOpaque());
        assertEquals(new ColorUIResource(122, 138, 153), outsideBorder.getLineColor());
        assertEquals(1, outsideBorder.getThickness());
    }

    public void testGetButtonBorder() {
        JComponent testButton = newJComponent();
        assertTrue(MetalBorders.getButtonBorder() instanceof BorderUIResource.CompoundBorderUIResource);
        BorderUIResource.CompoundBorderUIResource border = (BorderUIResource.CompoundBorderUIResource) MetalBorders
                .getButtonBorder();
        assertTrue(border.getInsideBorder() instanceof BasicBorders.MarginBorder);
        assertTrue(border.getOutsideBorder() instanceof MetalBorders.ButtonBorder);
        MetalBorders.ButtonBorder outsideBorder = (MetalBorders.ButtonBorder) border
                .getOutsideBorder();
        //        assertEquals(new Insets(2, 14, 2, 14), insideBorder.getBorderInsets(testButton));
        assertEquals(new Insets(3, 3, 3, 3), outsideBorder.getBorderInsets(testButton));
        assertTrue(ButtonBorder.borderInsets == outsideBorder.getBorderInsets(testButton));
        assertFalse(outsideBorder.isBorderOpaque());
    }

    public void testDialogBorder() throws Exception {
        MetalBorders.DialogBorder border = new MetalBorders.DialogBorder();
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(5, 5, 5, 5), border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    public void testQuestionDialogBorder() throws Exception {
        MetalBorders.DialogBorder border = new MetalBorders.DialogBorder();
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(5, 5, 5, 5), border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    public void testWarningDialogBorder() throws Exception {
        MetalBorders.DialogBorder border = new MetalBorders.DialogBorder();
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(5, 5, 5, 5), border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    public void testErrorDialogBorder() throws Exception {
        MetalBorders.DialogBorder border = new MetalBorders.DialogBorder();
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(5, 5, 5, 5), border.getBorderInsets(newJComponent()));
        checkInsets(border);
    }

    private void checkInsets(final AbstractBorder border) {
        checkInsets(border, newJComponent());
    }

    private void checkInsets(final AbstractBorder border, final JComponent c) {
        Insets ins = new Insets(100, 100, 100, 100);
        border.getBorderInsets(c, ins);
        assertEquals(border.getBorderInsets(c), ins);
    }

    private JComponent newJComponent() {
        return new JComponent() {
            private static final long serialVersionUID = 1L;
        };
    }
}
