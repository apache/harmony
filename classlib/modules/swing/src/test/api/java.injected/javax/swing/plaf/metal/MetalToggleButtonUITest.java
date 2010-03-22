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
 * Created on 22.02.2005

 */
package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Font;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicToggleButtonUITest;

public class MetalToggleButtonUITest extends BasicToggleButtonUITest {
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(MetalToggleButtonUITest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void testCreateUI() {
        assertTrue("created UI is not null", null != MetalToggleButtonUI
                .createUI(new JButton()));
        assertTrue("created UI is of the proper class",
                MetalToggleButtonUI.createUI(null) instanceof MetalToggleButtonUI);
        assertTrue("created UI is not shared",
                MetalToggleButtonUI.createUI(null) == MetalToggleButtonUI.createUI(null));
    }

    public void testPaintFocus() {
    }

    public void testPaintTextGraphicsJComponentRectangleString() {
    }

    public void testPaintButtonPressed() {
    }

    public void testMetalButtonUI() {
    }

    class PublicMetalToggleButtonUI extends MetalToggleButtonUI {
        public void setDisabledTextColor(final Color color) {
            disabledTextColor = color;
        }

        @Override
        public Color getDisabledTextColor() {
            return super.getDisabledTextColor();
        }

        public void setFocusColor(final Color color) {
            focusColor = color;
        }

        @Override
        public Color getFocusColor() {
            return super.getFocusColor();
        }

        public void setSelectColor(final Color color) {
            selectColor = color;
        }

        @Override
        public Color getSelectColor() {
            return super.getSelectColor();
        }

        @Override
        public void uninstallDefaults(final AbstractButton b) {
            super.uninstallDefaults(b);
        }
    }

    public void testGetDisabledTextColor() {
        PublicMetalToggleButtonUI publicUI = new PublicMetalToggleButtonUI();
        assertNull("DisabledTextColor is null", publicUI.getDisabledTextColor());
        publicUI.setDisabledTextColor(Color.WHITE);
        assertEquals("DisabledTextColor ", Color.WHITE, publicUI.getDisabledTextColor());
    }

    public void testGetFocusColor() {
        PublicMetalToggleButtonUI publicUI = new PublicMetalToggleButtonUI();
        assertNull("FocusColor is null", publicUI.getFocusColor());
        publicUI.setFocusColor(Color.WHITE);
        assertEquals("FocusColor ", Color.WHITE, publicUI.getFocusColor());
    }

    public void testGetSelectColor() {
        PublicMetalToggleButtonUI publicUI = new PublicMetalToggleButtonUI();
        assertNull("SelectedColor is null", publicUI.getSelectColor());
        publicUI.setSelectColor(Color.WHITE);
        assertEquals("SelectedColor ", Color.WHITE, publicUI.getSelectColor());
    }

    public void testInstallDefaults() throws Exception {
        PublicMetalToggleButtonUI publicUI = new PublicMetalToggleButtonUI();
        JToggleButton button = new JToggleButton();
        UIManager.put("ToggleButton.disabledText", new ColorUIResource(Color.red));
        UIManager.put("ToggleButton.focus", new ColorUIResource(Color.yellow));
        UIManager.put("ToggleButton.select", new ColorUIResource(Color.green));
        UIManager.put("ToggleButton.foreground", new ColorUIResource(Color.cyan));
        UIManager.put("ToggleButton.background", new ColorUIResource(Color.blue));
        Font font = new FontUIResource(button.getFont().deriveFont(100f));
        UIManager.put("ToggleButton.font", font);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.put("ToggleButton.border", border);
        button.setUI(publicUI);
        publicUI.installDefaults(button);
        assertEquals(Color.blue, button.getBackground());
        assertEquals(Color.cyan, button.getForeground());
        assertEquals("SelectedColor ", Color.green, publicUI.getSelectColor());
        assertEquals("focusColor ", Color.yellow, publicUI.getFocusColor());
        assertEquals("disabledTextColor ", Color.red, publicUI.getDisabledTextColor());
        assertEquals(font, button.getFont());
        assertEquals(border, button.getBorder());
    }

    public void testUninstallDefaults() throws Exception {
        PublicMetalToggleButtonUI publicUI = new PublicMetalToggleButtonUI();
        JToggleButton button = new JToggleButton();
        UIManager.put("ToggleButton.disabledText", new ColorUIResource(Color.red));
        UIManager.put("ToggleButton.focus", new ColorUIResource(Color.yellow));
        UIManager.put("ToggleButton.select", new ColorUIResource(Color.green));
        UIManager.put("ToggleButton.foreground", new ColorUIResource(Color.cyan));
        UIManager.put("ToggleButton.background", new ColorUIResource(Color.blue));
        Font font = new FontUIResource(button.getFont().deriveFont(100f));
        UIManager.put("ToggleButton.font", font);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.put("ToggleButton.border", border);
        button.setUI(publicUI);
        publicUI.installDefaults(button);
        publicUI.uninstallDefaults(button);
        assertEquals(Color.blue, button.getBackground());
        assertEquals(Color.cyan, button.getForeground());
        assertEquals("SelectedColor ", Color.green, publicUI.getSelectColor());
        assertEquals("focusColor ", Color.yellow, publicUI.getFocusColor());
        assertEquals("disabledTextColor ", Color.red, publicUI.getDisabledTextColor());
        assertEquals("font", font, button.getFont());
        assertNull("border", button.getBorder());
    }
}
