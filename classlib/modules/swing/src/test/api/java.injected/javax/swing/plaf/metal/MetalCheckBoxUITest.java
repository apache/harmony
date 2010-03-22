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
import javax.swing.JCheckBox;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.IconUIResource;

public class MetalCheckBoxUITest extends MetalRadioButtonUITest {
    PublicMetalCheckBoxUI publicUI;

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(MetalCheckBoxUITest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        publicUI = new PublicMetalCheckBoxUI();
    }

    @Override
    public void testCreateUI() {
        assertTrue("created UI is not null", null != MetalCheckBoxUI.createUI(new JButton()));
        assertTrue("created UI is of the proper class",
                MetalCheckBoxUI.createUI(null) instanceof MetalCheckBoxUI);
        assertTrue("created UI is not shared",
                MetalCheckBoxUI.createUI(null) == MetalCheckBoxUI.createUI(null));
    }

    class PublicMetalCheckBoxUI extends MetalCheckBoxUI {
        @Override
        public String getPropertyPrefix() {
            return super.getPropertyPrefix();
        }

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

    @Override
    public void testUninstallDefaults() {
        JCheckBox button = new JCheckBox();
        UIManager.put("CheckBox.disabledText", new ColorUIResource(Color.red));
        UIManager.put("CheckBox.focus", new ColorUIResource(Color.yellow));
        UIManager.put("CheckBox.select", new ColorUIResource(Color.green));
        UIManager.put("CheckBox.foreground", new ColorUIResource(Color.cyan));
        UIManager.put("CheckBox.background", new ColorUIResource(Color.blue));
        Font font = new FontUIResource(button.getFont().deriveFont(100f));
        UIManager.put("CheckBox.font", font);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.put("CheckBox.border", border);
        Icon icon = new IconUIResource(new ImageIcon(new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB)));
        UIManager.put("CheckBox.icon", icon);
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

    @Override
    public void testInstallDefaults() {
        JCheckBox button = new JCheckBox();
        UIManager.put("CheckBox.disabledText", new ColorUIResource(Color.red));
        UIManager.put("CheckBox.focus", new ColorUIResource(Color.yellow));
        UIManager.put("CheckBox.select", new ColorUIResource(Color.green));
        UIManager.put("CheckBox.foreground", new ColorUIResource(Color.cyan));
        UIManager.put("CheckBox.background", new ColorUIResource(Color.blue));
        Font font = new FontUIResource(button.getFont().deriveFont(100f));
        UIManager.put("CheckBox.font", font);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.put("CheckBox.border", border);
        Icon icon = new IconUIResource(new ImageIcon(new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB)));
        UIManager.put("CheckBox.icon", icon);
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

    public void testGetPropertyPrefix() {
        assertEquals("prefix ", "CheckBox.", publicUI.getPropertyPrefix());
    }
}
