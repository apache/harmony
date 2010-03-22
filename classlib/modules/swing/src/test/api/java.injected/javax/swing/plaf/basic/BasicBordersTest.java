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
 * @author Alexander T. Simbirtsev
 */
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;

public class BasicBordersTest extends SwingTestCase {
    private Color shadow;

    private Color darkShadow;

    private Color highlight;

    private Color lightHighlight;

    @Override
    public void setUp() {
        shadow = new Color(0, 0, 0);
        darkShadow = new Color(0, 0, 1);
        highlight = new Color(0, 0, 2);
        lightHighlight = new Color(0, 0, 3);
    }

    @Override
    public void tearDown() {
        shadow = null;
        darkShadow = null;
        highlight = null;
        lightHighlight = null;
    }

    public void testButtonBorderClass() {
        BasicBorders.ButtonBorder border = new BasicBorders.ButtonBorder(shadow, darkShadow,
                highlight, lightHighlight);
        assertEquals(shadow, border.shadow);
        assertEquals(darkShadow, border.darkShadow);
        assertEquals(highlight, border.highlight);
        assertEquals(lightHighlight, border.lightHighlight);
        checkInsets(border, new Insets(2, 3, 3, 3));
    }

    public void testFieldBorderClass() {
        BasicBorders.FieldBorder border = new BasicBorders.FieldBorder(shadow, darkShadow,
                highlight, lightHighlight);
        assertEquals(shadow, border.shadow);
        assertEquals(darkShadow, border.darkShadow);
        assertEquals(highlight, border.highlight);
        assertEquals(lightHighlight, border.lightHighlight);
        checkInsets(border, new Insets(2, 2, 2, 2));
    }

    public void testMarginBorderClass() {
        BasicBorders.MarginBorder border = new BasicBorders.MarginBorder();
        checkInsets(border, new Insets(0, 0, 0, 0));
        assertEquals(UIManager.get("CheckBox.margin"), border.getBorderInsets(new JCheckBox()));
        assertEquals(UIManager.get("Button.margin"), border.getBorderInsets(new JButton()));
        assertEquals(UIManager.get("CheckBoxMenuItem.margin"), border
                .getBorderInsets(new JCheckBoxMenuItem()));
        assertEquals(UIManager.get("EditorPane.margin"), border
                .getBorderInsets(new JEditorPane()));
        assertEquals(UIManager.get("FormattedTextField.margin"), border
                .getBorderInsets(new JFormattedTextField()));
        assertEquals(UIManager.get("Menu.margin"), border.getBorderInsets(new JMenu()));
        assertEquals(UIManager.get("MenuItem.margin"), border.getBorderInsets(new JMenuItem()));
        assertEquals(UIManager.get("PasswordField.margin"), border
                .getBorderInsets(new JPasswordField()));
        assertEquals(UIManager.get("RadioButton.margin"), border
                .getBorderInsets(new JRadioButton()));
        assertEquals(UIManager.get("RadioButtonMenuItem.margin"), border
                .getBorderInsets(new JRadioButtonMenuItem()));
        assertEquals(UIManager.get("TextArea.margin"), border.getBorderInsets(new JTextArea()));
        assertEquals(UIManager.get("TextField.margin"), border
                .getBorderInsets(new JTextField()));
        assertEquals(UIManager.get("TextPane.margin"), border.getBorderInsets(new JTextPane()));
        assertEquals(UIManager.get("ToggleButton.margin"), border
                .getBorderInsets(new JToggleButton()));
    }

    public void testMenuBarBorderClass() {
        BasicBorders.MenuBarBorder border = new BasicBorders.MenuBarBorder(highlight, shadow);
        checkInsets(border, new Insets(0, 0, 2, 0));
    }

    public void testRadioButtonBorderClass() {
        BasicBorders.RadioButtonBorder border = new BasicBorders.RadioButtonBorder(shadow,
                darkShadow, highlight, lightHighlight);
        checkCustomColors(border);
        checkInsets(border, new Insets(2, 2, 2, 2));
    }

    public void testRolloverButtonBorderClass() {
        BasicBorders.RolloverButtonBorder border = new BasicBorders.RolloverButtonBorder(
                shadow, darkShadow, highlight, lightHighlight);
        checkCustomColors(border);
        checkInsets(border, new Insets(2, 3, 3, 3));
    }

    public void testSplitPaneBorderClass() {
        BasicBorders.SplitPaneBorder border = new BasicBorders.SplitPaneBorder(highlight,
                shadow);
        assertEquals(highlight, border.highlight);
        assertEquals(shadow, border.shadow);
        assertEquals(new Insets(1, 1, 1, 1), border.getBorderInsets(newJComponent()));
        assertTrue(border.isBorderOpaque());
    }

    public void testToggleButtonBorderClass() {
        BasicBorders.ToggleButtonBorder border = new BasicBorders.ToggleButtonBorder(shadow,
                darkShadow, highlight, lightHighlight);
        checkCustomColors(border);
        checkInsets(border, new Insets(2, 2, 2, 2));
    }

    public void testGetToggleButtonBorder() {
        JComponent testButton = newJComponent();
        assertTrue(BasicBorders.getToggleButtonBorder() instanceof BorderUIResource.CompoundBorderUIResource);
        BorderUIResource.CompoundBorderUIResource border = (BorderUIResource.CompoundBorderUIResource) BasicBorders
                .getToggleButtonBorder();
        assertTrue(border.getInsideBorder() instanceof BasicBorders.MarginBorder);
        BasicBorders.MarginBorder insideBorder = (BasicBorders.MarginBorder) border
                .getInsideBorder();
        assertTrue(border.getOutsideBorder() instanceof BasicBorders.ToggleButtonBorder);
        BasicBorders.ToggleButtonBorder outsideBorder = (BasicBorders.ToggleButtonBorder) border
                .getOutsideBorder();
        checkButtonBorder(outsideBorder);
        assertEquals(new Insets(0, 0, 0, 0), insideBorder.getBorderInsets(testButton));
        assertFalse(insideBorder.isBorderOpaque());
    }

    public void testGetTextFieldBorder() {
        JComponent testButton = newJComponent();
        assertTrue(BasicBorders.getTextFieldBorder() instanceof BasicBorders.FieldBorder);
        BasicBorders.FieldBorder border = (BasicBorders.FieldBorder) BasicBorders
                .getTextFieldBorder();
        if (isHarmony()) {
            assertEquals("shadow", new ColorUIResource(Color.GRAY), border.shadow);
            assertEquals("darkShadow", new ColorUIResource(Color.DARK_GRAY), border.darkShadow);
            assertEquals("highlight", new ColorUIResource(Color.WHITE), border.highlight);
            assertEquals("lightHighlight", new ColorUIResource(Color.WHITE),
                    border.lightHighlight);
        }
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(2, 2, 2, 2), border.getBorderInsets(testButton));
    }

    public void testGetSplitPaneDividerBorder() {
        JComponent c = new JComponent() {
            private static final long serialVersionUID = 1L;
        };
        Border border = BasicBorders.getSplitPaneDividerBorder();
        assertTrue(border.isBorderOpaque());
        assertEquals(new Insets(1, 1, 1, 1), border.getBorderInsets(c));
    }

    public void testGetSplitPaneBorder() {
        JComponent testButton = newJComponent();
        assertTrue(BasicBorders.getSplitPaneBorder() instanceof BasicBorders.SplitPaneBorder);
        BasicBorders.SplitPaneBorder border = (BasicBorders.SplitPaneBorder) BasicBorders
                .getSplitPaneBorder();
        assertTrue(border.isBorderOpaque());
        assertEquals(new Insets(1, 1, 1, 1), border.getBorderInsets(testButton));
        if (isHarmony()) {
            assertEquals(new ColorUIResource(Color.WHITE), border.highlight);
            assertEquals(new ColorUIResource(Color.GRAY), border.shadow);
        }
    }

    public void testGetRadioButtonBorder() {
        JComponent testButton = newJComponent();
        assertTrue(BasicBorders.getRadioButtonBorder() instanceof BorderUIResource.CompoundBorderUIResource);
        BorderUIResource.CompoundBorderUIResource border = (BorderUIResource.CompoundBorderUIResource) BasicBorders
                .getRadioButtonBorder();
        assertTrue(border.getInsideBorder() instanceof BasicBorders.MarginBorder);
        BasicBorders.MarginBorder insideBorder = (BasicBorders.MarginBorder) border
                .getInsideBorder();
        assertTrue(border.getOutsideBorder() instanceof BasicBorders.RadioButtonBorder);
        BasicBorders.RadioButtonBorder outsideBorder = (BasicBorders.RadioButtonBorder) border
                .getOutsideBorder();
        assertEquals(new Insets(0, 0, 0, 0), insideBorder.getBorderInsets(testButton));
        assertFalse(insideBorder.isBorderOpaque());
        checkButtonBorder(outsideBorder);
    }

    public void testGetProgressBarBorder() {
        JComponent testButton = newJComponent();
        assertTrue(BasicBorders.getProgressBarBorder() instanceof BorderUIResource.LineBorderUIResource);
        BorderUIResource.LineBorderUIResource border = (BorderUIResource.LineBorderUIResource) BasicBorders
                .getProgressBarBorder();
        assertFalse(border.getRoundedCorners());
        assertTrue(border.isBorderOpaque());
        assertEquals(new Insets(2, 2, 2, 2), border.getBorderInsets(testButton));
        assertEquals(new Color(0, 255, 0), border.getLineColor());
        assertEquals(2, border.getThickness());
    }

    public void testGetMenuBarBorder() {
        JComponent testButton = newJComponent();
        assertTrue(BasicBorders.getMenuBarBorder() instanceof BasicBorders.MenuBarBorder);
        BasicBorders.MenuBarBorder border = (BasicBorders.MenuBarBorder) BasicBorders
                .getMenuBarBorder();
        assertEquals(new Insets(0, 0, 2, 0), border.getBorderInsets(testButton));
        assertFalse(border.isBorderOpaque());
    }

    public void testGetInternalFrameBorder() {
        JComponent testButton = newJComponent();
        assertTrue(BasicBorders.getInternalFrameBorder() instanceof BorderUIResource.CompoundBorderUIResource);
        BorderUIResource.CompoundBorderUIResource border = (BorderUIResource.CompoundBorderUIResource) BasicBorders
                .getInternalFrameBorder();
        assertTrue(border.getInsideBorder() instanceof LineBorder);
        LineBorder insideBorder = (LineBorder) border.getInsideBorder();
        assertTrue(border.getOutsideBorder() instanceof BevelBorder);
        BevelBorder outsideBorder = (BevelBorder) border.getOutsideBorder();
        assertEquals(new Insets(1, 1, 1, 1), insideBorder.getBorderInsets(testButton));
        assertEquals(new Insets(2, 2, 2, 2), outsideBorder.getBorderInsets(testButton));
        assertFalse(insideBorder.getRoundedCorners());
        assertEquals(1, insideBorder.getThickness());
        assertTrue(insideBorder.isBorderOpaque());
        assertEquals(0, outsideBorder.getBevelType());
        if (isHarmony()) {
            assertEquals(new ColorUIResource(Color.LIGHT_GRAY), insideBorder.getLineColor());
            assertEquals(new ColorUIResource(Color.GRAY), outsideBorder.getShadowInnerColor());
            assertEquals(new ColorUIResource(Color.DARK_GRAY), outsideBorder
                    .getShadowOuterColor());
            assertEquals(new ColorUIResource(Color.WHITE), outsideBorder
                    .getHighlightOuterColor());
            assertEquals(new ColorUIResource(Color.WHITE), outsideBorder
                    .getHighlightInnerColor());
        }
    }

    public void testGetButtonBorder() {
        JComponent testButton = newJComponent();
        assertTrue(BasicBorders.getButtonBorder() instanceof BorderUIResource.CompoundBorderUIResource);
        BorderUIResource.CompoundBorderUIResource border = (BorderUIResource.CompoundBorderUIResource) BasicBorders
                .getButtonBorder();
        assertTrue(border.getInsideBorder() instanceof BasicBorders.MarginBorder);
        BasicBorders.MarginBorder insideBorder = (BasicBorders.MarginBorder) border
                .getInsideBorder();
        assertTrue(border.getOutsideBorder() instanceof BasicBorders.ButtonBorder);
        BasicBorders.ButtonBorder outsideBorder = (BasicBorders.ButtonBorder) border
                .getOutsideBorder();
        checkColors(outsideBorder);
        assertEquals(new Insets(0, 0, 0, 0), insideBorder.getBorderInsets(testButton));
        assertEquals(new Insets(2, 3, 3, 3), outsideBorder.getBorderInsets(testButton));
    }

    private void checkButtonBorder(final BasicBorders.ButtonBorder border) {
        checkColors(border);
        assertFalse(border.isBorderOpaque());
        assertEquals(new Insets(2, 2, 2, 2), border.getBorderInsets(newJComponent()));
    }

    private void checkColors(final BasicBorders.ButtonBorder border) {
        if (isHarmony()) {
            assertEquals("shadow", new ColorUIResource(Color.GRAY), border.shadow);
            assertEquals("darkShadow", new ColorUIResource(Color.DARK_GRAY), border.darkShadow);
            assertEquals("highlight", new ColorUIResource(Color.WHITE), border.highlight);
            assertEquals("lightHighlight", new ColorUIResource(Color.WHITE),
                    border.lightHighlight);
        }
        JComponent button = newJComponent();
        assertFalse("insets are not shared", border.getBorderInsets(button) == border
                .getBorderInsets(button));
    }

    private void checkCustomColors(final BasicBorders.ButtonBorder border) {
        assertEquals("shadow", shadow, border.shadow);
        assertEquals("darkShadow", darkShadow, border.darkShadow);
        assertEquals("highlight", highlight, border.highlight);
        assertEquals("lightHighlight", lightHighlight, border.lightHighlight);
        JComponent button = newJComponent();
        assertFalse("insets are not shared", border.getBorderInsets(button) == border
                .getBorderInsets(button));
    }

    private void checkInsets(final AbstractBorder border, final Insets testInsets) {
        assertEquals(testInsets, border.getBorderInsets(newJComponent()));
        assertFalse(border.isBorderOpaque());
        Insets insets = new Insets(0, 0, 0, 0);
        border.getBorderInsets(newJComponent(), insets);
        assertEquals(border.getBorderInsets(newJComponent()), insets);
        JComponent button = newJComponent();
        assertFalse(border.getBorderInsets(button) == border.getBorderInsets(button));
    }

    private JComponent newJComponent() {
        return new JComponent() {
            private static final long serialVersionUID = 1L;
        };
    }
}
