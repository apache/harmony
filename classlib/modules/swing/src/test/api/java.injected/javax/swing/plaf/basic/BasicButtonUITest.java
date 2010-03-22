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
 * Created on 16.02.2005

 */
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.KeyStroke;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InputMapUIResource;

public class BasicButtonUITest extends SwingTestCase {
    protected BasicButtonUI ui;

    protected InputMap uiInputMap;

    Border previousBorder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ui = new BasicButtonUI();
        previousBorder = UIManager.getBorder("Button.border");
        uiInputMap = new InputMapUIResource();
        uiInputMap.put(KeyStroke.getKeyStroke("SPACE"), "pressed");
        uiInputMap.put(KeyStroke.getKeyStroke("released SPACE"), "released");
        uiInputMap.put(KeyStroke.getKeyStroke("ENTER"), "pressed");
        uiInputMap.put(KeyStroke.getKeyStroke("released ENTER"), "released");
    }

    @Override
    protected void tearDown() throws Exception {
        UIManager.put("Button.border", previousBorder);
        super.tearDown();
    }

    public void testGetPreferredSize() {
        Font font = new FontUIResource(new Font("serif", Font.PLAIN, 24));
        UIManager.put("Button.font", font);
        JButton button1 = new JButton();
        JButton button2 = new JButton("text");
        JButton button3 = new JButton("text");
        JButton button4 = new JButton();
        JButton button5 = new JButton("text");
        JButton button6 = new JButton("text");
        button3.setBorder(new EmptyBorder(10, 10, 10, 10));
        button6.setBorder(null);
        button4.setIcon(new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB)));
        button5.setIcon(new ImageIcon(new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB)));
        int horInsets = button1.getInsets().left + button1.getInsets().right;
        int vertInsets = button1.getInsets().top + button1.getInsets().bottom;
        int textWidth = button1.getFontMetrics(button1.getFont()).stringWidth("text");
        int textHeight = button1.getFontMetrics(button1.getFont()).getHeight();
        assertEquals("PreferredSize", new Dimension(horInsets, vertInsets), ui
                .getPreferredSize(button1));
        assertEquals("PreferredSize", new Dimension(horInsets + textWidth, vertInsets
                + textHeight), ui.getPreferredSize(button2));
        assertEquals("PreferredSize", new Dimension(horInsets
                + button4.getIcon().getIconWidth(), vertInsets
                + button4.getIcon().getIconHeight()), ui.getPreferredSize(button4));
        int height = vertInsets + Math.max(button5.getIcon().getIconHeight(), textHeight);
        int width = horInsets + textWidth + button5.getIcon().getIconWidth()
                + button5.getIconTextGap();
        assertEquals("PreferredSize", new Dimension(width, height), ui
                .getPreferredSize(button5));
        horInsets = button3.getInsets().left + button3.getInsets().right;
        vertInsets = button3.getInsets().top + button3.getInsets().bottom;
        assertEquals("PreferredSize", new Dimension(horInsets + textWidth, vertInsets
                + textHeight), ui.getPreferredSize(button3));
        horInsets = button6.getInsets().left + button6.getInsets().right;
        vertInsets = button6.getInsets().top + button6.getInsets().bottom;
        assertEquals("PreferredSize", new Dimension(horInsets + textWidth, vertInsets
                + textHeight), ui.getPreferredSize(button6));
    }

    public void testGetMinimumSize() {
        Font font = new FontUIResource(new Font("serif", Font.PLAIN, 24));
        UIManager.put("Button.font", font);
        JButton button1 = new JButton();
        JButton button2 = new JButton("text");
        JButton button3 = new JButton("text");
        JButton button4 = new JButton();
        JButton button5 = new JButton("text");
        button3.setBorder(new EmptyBorder(10, 10, 10, 10));
        button4.setIcon(new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB)));
        button5.setIcon(new ImageIcon(new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB)));
        int horInsets = button1.getInsets().left + button1.getInsets().right;
        int vertInsets = button1.getInsets().top + button1.getInsets().bottom;
        int textWidth = button1.getFontMetrics(button1.getFont()).stringWidth("text");
        int textHeight = button1.getFontMetrics(button1.getFont()).getHeight();
        assertEquals("MinimumSize", new Dimension(horInsets, vertInsets), ui
                .getMinimumSize(button1));
        assertEquals("MinimumSize", new Dimension(horInsets + textWidth, vertInsets
                + textHeight), ui.getMinimumSize(button2));
        assertEquals("MinimumSize", new Dimension(horInsets + button4.getIcon().getIconWidth(),
                vertInsets + button4.getIcon().getIconHeight()), ui.getMinimumSize(button4));
        int height = vertInsets + Math.max(button5.getIcon().getIconHeight(), textHeight);
        int width = horInsets + textWidth + button5.getIcon().getIconWidth()
                + button5.getIconTextGap();
        assertEquals("MinimumSize", new Dimension(width, height), ui.getMinimumSize(button5));
        horInsets = button3.getInsets().left + button3.getInsets().right;
        vertInsets = button3.getInsets().top + button3.getInsets().bottom;
        assertEquals("MinimumSize", new Dimension(horInsets + textWidth, vertInsets
                + textHeight), ui.getMinimumSize(button3));
    }

    public void testGetMaximumSize() {
        Font font = new FontUIResource(new Font("serif", Font.PLAIN, 24));
        UIManager.put("Button.font", font);
        JButton button1 = new JButton();
        JButton button2 = new JButton("text");
        JButton button3 = new JButton("text");
        JButton button4 = new JButton();
        JButton button5 = new JButton("text");
        button3.setBorder(new EmptyBorder(10, 10, 10, 10));
        button4.setIcon(new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB)));
        button5.setIcon(new ImageIcon(new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB)));
        int horInsets = button1.getInsets().left + button1.getInsets().right;
        int vertInsets = button1.getInsets().top + button1.getInsets().bottom;
        int textWidth = button1.getFontMetrics(button1.getFont()).stringWidth("text");
        int textHeight = button1.getFontMetrics(button1.getFont()).getHeight();
        assertEquals("MaximumSize", new Dimension(horInsets, vertInsets), ui
                .getMaximumSize(button1));
        assertEquals("MaximumSize", new Dimension(horInsets + textWidth, vertInsets
                + textHeight), ui.getMaximumSize(button2));
        assertEquals("MaximumSize", new Dimension(horInsets + button4.getIcon().getIconWidth(),
                vertInsets + button4.getIcon().getIconHeight()), ui.getMaximumSize(button4));
        int height = vertInsets + Math.max(button5.getIcon().getIconHeight(), textHeight);
        int width = horInsets + textWidth + button5.getIcon().getIconWidth()
                + button5.getIconTextGap();
        assertEquals("MaximumSize", new Dimension(width, height), ui.getMaximumSize(button5));
        horInsets = button3.getInsets().left + button3.getInsets().right;
        vertInsets = button3.getInsets().top + button3.getInsets().bottom;
        assertEquals("MaximumSize", new Dimension(horInsets + textWidth, vertInsets
                + textHeight), ui.getMaximumSize(button3));
    }

    /**
     * uninstallUI() and installUI() are being tested here
     */
    public void testInstallUninstallUI() {
        UIManager.put("Button.focusInputMap", uiInputMap);
        JButton button = new JButton();
        button.setUI(ui);
        assertTrue(
                "there is one property listener",
                button.getPropertyChangeListeners().length == 1
                        && button.getPropertyChangeListeners()[0] instanceof BasicButtonListener);
        assertTrue("there is border", button.getBorder() != null);
        assertTrue("there is one change listener", button.getChangeListeners().length == 1
                && button.getChangeListeners()[0] instanceof BasicButtonListener);
        assertEquals(4, button.getRegisteredKeyStrokes().length);
        ui.uninstallUI(button);
        assertTrue("there are no property listeners",
                button.getPropertyChangeListeners().length == 0);
        assertNull("there ain't no border", button.getBorder());
        assertTrue("there are no change listeners", button.getChangeListeners().length == 0);
        assertTrue("no RegisteredKeyStrokes installed",
                button.getRegisteredKeyStrokes().length == 0);
        assertTrue("opaque", button.isOpaque());
        button.setOpaque(false);
        ui.installUI(button);
        assertTrue(
                "there is one property listener",
                button.getPropertyChangeListeners().length == 1
                        && button.getPropertyChangeListeners()[0] instanceof BasicButtonListener);
        assertTrue("there is border", button.getBorder() != null);
        assertTrue("there is one change listener", button.getChangeListeners().length == 1
                && button.getChangeListeners()[0] instanceof BasicButtonListener);
        assertEquals(4, button.getRegisteredKeyStrokes().length);
        if (isHarmony()) {
            assertFalse(button.isOpaque());
        }
    }

    public void testInstallUninstallUI2() {
        JButton button = new JButton();
        BasicButtonUI buttonUI = new BasicButtonUI();
        assertTrue("opaque", button.isOpaque());
        UIManager.put("Button.background", new ColorUIResource(Color.red));
        UIManager.put("Button.foreground", new ColorUIResource(Color.yellow));
        Font font = new FontUIResource(button.getFont().deriveFont(100f));
        UIManager.put("Button.font", font);
        Border border2 = new BorderUIResource(BorderFactory.createEmptyBorder());
        UIManager.put("Button.border", border2);
        button.setOpaque(false);
        UIManager.put("Button.textIconGap", new Integer(100));
        button.setUI(buttonUI);
        assertEquals(Color.red, button.getBackground());
        assertEquals(Color.yellow, button.getForeground());
        assertEquals(border2, button.getBorder());
        assertEquals(font, button.getFont());
        if (isHarmony()) {
            assertFalse(button.isOpaque());
        }
        Border border1 = BorderFactory.createEmptyBorder();
        button.setBorder(border1);
        button.setUI(buttonUI);
        buttonUI.installUI(button);
        assertEquals(border1, button.getBorder());
        buttonUI.uninstallUI(button);
        if (isHarmony()) {
            assertNull(button.getBackground());
            assertNull(button.getForeground());
        }
        assertNotNull(button.getBorder());
        UIManager.put("Button.background", Color.red);
        button.setUI(buttonUI);
        buttonUI.uninstallUI(button);
        if (!isHarmony()) {
            assertEquals(Color.red, button.getBackground());
            assertNotNull(button.getForeground());
        } else {
            assertNull(button.getBackground());
            assertNull(button.getForeground());
        }
        assertEquals(border1, button.getBorder());
    }

    public void testCreateUI() {
        assertNotNull("created UI is not null", BasicButtonUI.createUI(new JButton()));
        assertTrue("created UI is of the proper class",
                BasicButtonUI.createUI(null) instanceof BasicButtonUI);
        assertSame("created UI is of unique", BasicButtonUI.createUI(null), BasicButtonUI
                .createUI(null));
    }

    public void testCreateButtonListener() {
        JButton button1 = new JButton();
        JButton button2 = new JButton();
        BasicButtonListener res1 = ui.createButtonListener(button1);
        BasicButtonListener res2 = ui.createButtonListener(button2);
        BasicButtonListener res3 = ui.createButtonListener(button2);
        assertNotNull("listener created", res1);
        assertNotNull("listener created", res2);
        assertNotNull("listener created", res3);
        assertTrue("created listeners are unique", res2 != res1);
        assertTrue("created listeners are unique", res2 != res3);
    }

    /**
     * uninstallListeners() and installListeners() are being tested here
     */
    public void testInstallUninstallListeners() {
        JButton button = new JButton();
        button.setUI(ui);
        ui.uninstallListeners(button);
        assertEquals("PropertyChangeListeners", 0, button.getPropertyChangeListeners().length);
        assertEquals("ChangeListeners", 0, button.getChangeListeners().length);
        ui.installListeners(button);
        assertEquals("PropertyChangeListeners", 1, button.getPropertyChangeListeners().length);
        assertEquals("ChangeListeners", 1, button.getChangeListeners().length);
        assertTrue("listeners", button.getPropertyChangeListeners()[0].equals(button
                .getChangeListeners()[0]));
        ui.uninstallListeners(button);
        assertEquals("PropertyChangeListeners", 0, button.getPropertyChangeListeners().length);
        assertEquals("ChangeListeners", 0, button.getChangeListeners().length);
    }

    boolean findKeyStroke(final KeyStroke[] strokes, final KeyStroke stroke) {
        for (int i = 0; i < strokes.length; i++) {
            if (strokes[i].equals(stroke)) {
                return true;
            }
        }
        return false;
    }

    /**
     * uninstallKeyboardActions() and installKeyboardActions() are being tested here
     */
    public void testInstallUninstallKeyboardActions() {
        JButton button = new JButton();
        button.setUI(ui);
        UIManager.put("Button.focusInputMap", uiInputMap);
        ui.uninstallKeyboardActions(button);
        assertEquals("RegisteredKeyStrokes", 0, button.getRegisteredKeyStrokes().length);
        ui.installKeyboardActions(button);
        assertEquals("RegisteredKeyStrokes", 4, button.getRegisteredKeyStrokes().length);
        ui.uninstallKeyboardActions(button);
        assertEquals("RegisteredKeyStrokes", 0, button.getRegisteredKeyStrokes().length);
    }

    /**
     * uninstallDefaults() and installDefaults() are being tested here
     */
    public void testInstallUninstallDefaults() {
        JButton button = new JButton();
        button.setUI(ui);
        ui.uninstallDefaults(button);
        assertNull("border", button.getBorder());
        if (!isHarmony()) {
            assertNotNull("font", button.getFont());
            assertNotNull("margin", button.getFont());
        } else {
            assertNull("font", button.getFont());
            assertNull("margin", button.getFont());
        }
        UIManager.put("Button.textIconGap", new Integer(100));
        UIManager.put("Button.textShiftOffset", new Integer(1000));
        button.setOpaque(false);
        ui.installDefaults(button);
        assertNotNull(button.getBorder());
        if (isHarmony()) {
            assertFalse(button.isOpaque());
        }
        assertEquals(100, ui.getDefaultTextIconGap(button));
        assertTrue(100 != button.getIconTextGap());
        assertEquals(1000, ui.defaultTextShiftOffset);
        Border border = new EmptyBorder(1, 1, 1, 1);
        button.setBorder(border);
        ui.uninstallDefaults(button);
        assertEquals("border ", border, button.getBorder());
    }

    public void testGetDefaultTextIconGap() {
        JButton button = new JButton("text");
        assertEquals("DefaultTextIconGap", 0, ui.getDefaultTextIconGap(button));
        assertEquals("DefaultTextIconGap", 0, ui.getDefaultTextIconGap(null));
    }

    public void testGetPropertyPrefix() {
        assertEquals("prefix", "Button.", ui.getPropertyPrefix());
    }

    /**
     * The test verifies that getTextShiftOffset methods returns
     * Button.textShiftOffset property if the setTextShiftOffset method is
     * called and returns 0 if the clearTextShiftOffset method is called
     */
    @SuppressWarnings( { "boxing", "nls" })
    public void testTextShiftOffest() {

        int oldTextShiftOffset = UIManager.getInt("Button.textShiftOffset");

        UIManager.put("Button.textShiftOffset", 5);

        BasicButtonUI currentUI = new BasicButtonUI();
        currentUI.installUI(new JButton());

        assertEquals(currentUI.getTextShiftOffset(), 0);
        currentUI.setTextShiftOffset();
        assertEquals(currentUI.getTextShiftOffset(), 5);
        currentUI.clearTextShiftOffset();
        assertEquals(currentUI.getTextShiftOffset(), 0);

        UIManager.put("Button.textShiftOffset", oldTextShiftOffset);
    }
}
