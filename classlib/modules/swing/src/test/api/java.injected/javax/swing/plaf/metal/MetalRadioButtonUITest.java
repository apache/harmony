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
 * Created on 29.04.2005

 */
package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.IconUIResource;
import junit.framework.TestCase;

public class MetalRadioButtonUITest extends TestCase {
    PublicMetalRadioButtonUI publicUI;

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(MetalRadioButtonUITest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        publicUI = new PublicMetalRadioButtonUI();
    }

    public void testCreateUI() {
        assertTrue("created UI is not null", null != MetalRadioButtonUI.createUI(new JButton()));
        assertTrue("created UI is of the proper class",
                MetalRadioButtonUI.createUI(null) instanceof MetalRadioButtonUI);
        assertTrue("created UI is not shared",
                MetalRadioButtonUI.createUI(null) == MetalRadioButtonUI.createUI(null));
    }

    class PublicMetalRadioButtonUI extends MetalRadioButtonUI {
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

    public void testUninstallDefaults() {
        JRadioButton button = new JRadioButton();
        UIManager.put("RadioButton.disabledText", new ColorUIResource(Color.red));
        UIManager.put("RadioButton.focus", new ColorUIResource(Color.yellow));
        UIManager.put("RadioButton.select", new ColorUIResource(Color.green));
        UIManager.put("RadioButton.foreground", new ColorUIResource(Color.cyan));
        UIManager.put("RadioButton.background", new ColorUIResource(Color.blue));
        Font font = new FontUIResource(button.getFont().deriveFont(100f));
        UIManager.put("RadioButton.font", font);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.put("RadioButton.border", border);
        Icon icon = new IconUIResource(new ImageIcon(new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB)));
        UIManager.put("RadioButton.icon", icon);
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
        assertEquals("icon", icon, publicUI.getDefaultIcon());
    }

    public void testInstallDefaults() {
        JRadioButton button = new JRadioButton();
        UIManager.put("RadioButton.disabledText", new ColorUIResource(Color.red));
        UIManager.put("RadioButton.focus", new ColorUIResource(Color.yellow));
        UIManager.put("RadioButton.select", new ColorUIResource(Color.green));
        UIManager.put("RadioButton.foreground", new ColorUIResource(Color.cyan));
        UIManager.put("RadioButton.background", new ColorUIResource(Color.blue));
        Font font = new FontUIResource(button.getFont().deriveFont(100f));
        UIManager.put("RadioButton.font", font);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.put("RadioButton.border", border);
        Icon icon = new IconUIResource(new ImageIcon(new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB)));
        UIManager.put("RadioButton.icon", icon);
        button.setUI(publicUI);
        publicUI.installDefaults(button);
        assertEquals(Color.blue, button.getBackground());
        assertEquals(Color.cyan, button.getForeground());
        assertEquals("SelectedColor ", Color.green, publicUI.getSelectColor());
        assertEquals("focusColor ", Color.yellow, publicUI.getFocusColor());
        assertEquals("disabledTextColor ", Color.red, publicUI.getDisabledTextColor());
        assertEquals("font", font, button.getFont());
        assertEquals("border", border, button.getBorder());
        assertEquals("icon", icon, publicUI.getDefaultIcon());
    }

    public void testGetSelectColor() {
        assertNull("SelectedColor is null", publicUI.getSelectColor());
        publicUI.setSelectColor(Color.WHITE);
        assertEquals("SelectedColor ", Color.WHITE, publicUI.getSelectColor());
    }

    public void testGetDisabledTextColor() {
        assertNull("DisabledTextColor is null", publicUI.getDisabledTextColor());
        publicUI.setDisabledTextColor(Color.WHITE);
        assertEquals("DisabledTextColor ", Color.WHITE, publicUI.getDisabledTextColor());
    }

    public void testGetFocusColor() {
        assertNull("FocusColor is null", publicUI.getFocusColor());
        publicUI.setFocusColor(Color.WHITE);
        assertEquals("FocusColor ", Color.WHITE, publicUI.getFocusColor());
    }
}
