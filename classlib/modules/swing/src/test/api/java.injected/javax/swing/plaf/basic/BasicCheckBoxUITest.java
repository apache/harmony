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
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JScrollBar;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

public class BasicCheckBoxUITest extends SwingTestCase {
    public class MyBasicCheckBoxUI extends BasicCheckBoxUI {
        @Override
        public String getPropertyPrefix() {
            return super.getPropertyPrefix();
        }

        @Override
        public void installDefaults(final AbstractButton b) {
            super.installDefaults(b);
        }

        @Override
        public void uninstallDefaults(final AbstractButton b) {
            super.uninstallDefaults(b);
        }

        @Override
        public int getTextShiftOffset() {
            return super.getTextShiftOffset();
        }

        public void setIcon(final Icon newIcon) {
            icon = newIcon;
        }

        public void setTextIconGap(final int gap) {
            defaultTextIconGap = gap;
        }

        public void setTextShiftOffset(final int offset) {
            defaultTextShiftOffset = offset;
        }
    };

    public MyBasicCheckBoxUI ui = null;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ui = new MyBasicCheckBoxUI();
    }

    public void testGetPreferredSize() {
        Font font = new FontUIResource(new Font("serif", Font.PLAIN, 24));
        UIManager.put("CheckBox.font", font);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(11, 11, 11, 11));
        UIManager.put("CheckBox.border", border);
        JCheckBox button1 = new JCheckBox();
        JCheckBox button2 = new JCheckBox("text");
        JCheckBox button3 = new JCheckBox("text");
        JCheckBox button6 = new JCheckBox("text");
        button3.setBorder(new EmptyBorder(10, 10, 10, 10));
        button6.setBorder(null);
        int iconH = 20;
        int iconW = 30;
        ui.setIcon(new ImageIcon(new BufferedImage(iconW, iconH, BufferedImage.TYPE_INT_ARGB)));
        // following parameters are not being used by UI
        ui.setTextIconGap(100);
        ui.setTextShiftOffset(33);
        int horInsets = button1.getInsets().left + button1.getInsets().right;
        int vertInsets = button1.getInsets().top + button1.getInsets().bottom;
        int textWidth = button1.getFontMetrics(button1.getFont()).stringWidth("text");
        int textHeight = button1.getFontMetrics(button1.getFont()).getHeight();
        assertEquals("PreferredSize", new Dimension(horInsets + iconW, vertInsets + iconH), ui
                .getPreferredSize(button1));
        assertEquals("PreferredSize", new Dimension(horInsets + iconW + textWidth
                + button1.getIconTextGap(), vertInsets + Math.max(iconH, textHeight)), ui
                .getPreferredSize(button2));
        horInsets = button3.getInsets().left + button3.getInsets().right;
        vertInsets = button3.getInsets().top + button3.getInsets().bottom;
        assertEquals("PreferredSize", new Dimension(horInsets + iconW + textWidth
                + button1.getIconTextGap(), vertInsets + Math.max(iconH, textHeight)), ui
                .getPreferredSize(button3));
        horInsets = button6.getInsets().left + button6.getInsets().right;
        vertInsets = button6.getInsets().top + button6.getInsets().bottom;
        assertEquals("PreferredSize", new Dimension(horInsets + iconW + textWidth
                + button1.getIconTextGap(), vertInsets + Math.max(iconH, textHeight)), ui
                .getPreferredSize(button6));

        // regression test for HARMONY-2605
        assertNull(new BasicCheckBoxUI().getMaximumSize(new JScrollBar()));
    }

    public void testCreateUI() {
        assertTrue("created UI is not null", null != BasicCheckBoxUI.createUI(new JButton()));
        assertTrue("created UI is of the proper class",
                BasicCheckBoxUI.createUI(null) instanceof BasicCheckBoxUI);
        assertTrue("created UI is not unique",
                BasicCheckBoxUI.createUI(null) == BasicCheckBoxUI.createUI(null));
    }

    public void testUninstallDefaults() {
        JCheckBox button = new JCheckBox();
        UIManager.put("CheckBox.foreground", new ColorUIResource(Color.cyan));
        UIManager.put("CheckBox.background", new ColorUIResource(Color.blue));
        assertNotNull("font is installed", button.getFont());
        Font font = new FontUIResource(button.getFont().deriveFont(100f));
        UIManager.put("CheckBox.font", font);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.put("CheckBox.border", border);
        button.setUI(ui);
        ui.installDefaults(button);
        ui.uninstallDefaults(button);
        assertEquals("background", Color.blue, button.getBackground());
        assertEquals("foreground", Color.cyan, button.getForeground());
        assertEquals("font", font, button.getFont());
        assertNull("border", button.getBorder());
    }

    public void testInstallDefaults() {
        JCheckBox button = new JCheckBox();
        UIManager.put("CheckBox.foreground", new ColorUIResource(Color.cyan));
        UIManager.put("CheckBox.background", new ColorUIResource(Color.blue));
        assertNotNull("font is installed", button.getFont());
        Font font = new FontUIResource(button.getFont().deriveFont(100f));
        UIManager.put("CheckBox.font", font);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.put("CheckBox.border", border);
        button.setUI(ui);
        ui.installDefaults(button);
        assertEquals("background", Color.blue, button.getBackground());
        assertEquals("foreground", Color.cyan, button.getForeground());
        assertEquals("font", font, button.getFont());
        assertEquals("border", border, button.getBorder());
    }

    public void testGetPropertyPrefix() {
        assertEquals("prefix ", "CheckBox.", ui.getPropertyPrefix());
    }

    public void testGetDefaultIcon() {
        assertNull("icon", ui.getDefaultIcon());
        Icon icon = new ImageIcon();
        ui.setIcon(icon);
        assertEquals("icon", icon, ui.getDefaultIcon());
    }
}
