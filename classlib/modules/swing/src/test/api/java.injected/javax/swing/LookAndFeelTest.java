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
package javax.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.lang.reflect.Method;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentInputMapUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InputMapUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

public class LookAndFeelTest extends SwingTestCase {
    LookAndFeel lf;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        lf = new LookAndFeel() {
            @Override
            public String getDescription() {
                return "description";
            }

            @Override
            public String getID() {
                return "id";
            }

            @Override
            public String getName() {
                return "name";
            }

            @Override
            public boolean isNativeLookAndFeel() {
                return false;
            }

            @Override
            public boolean isSupportedLookAndFeel() {
                return false;
            }
        };
        timeoutDelay = 5 * DEFAULT_TIMEOUT_DELAY;
    }

    @Override
    public void tearDown() throws Exception {
        lf = null;
        super.tearDown();
    }

    public void testToString() {
        assertEquals("[" + lf.getDescription() + " - " + lf.getClass().getName() + "]", lf
                .toString());
    }

    public void testMakeComponentInputMap() {
        Object[] keys = new Object[] { "SPACE", "pressed" };
        JButton button = new JButton();
        ComponentInputMap componentInputMap = LookAndFeel.makeComponentInputMap(button, keys);
        assertTrue(componentInputMap instanceof ComponentInputMapUIResource);
        assertTrue(componentInputMap.getComponent() instanceof JButton);
        assertEquals(button, componentInputMap.getComponent());
        assertEquals(1, componentInputMap.size());
    }

    public void testMakeIcon() {
        Object icon = LookAndFeel.makeIcon(lf.getClass(), "empty_path");
        assertNull(((UIDefaults.LazyValue) icon).createValue(new UIDefaults()));
        lf = new MetalLookAndFeel();
        icon = LookAndFeel.makeIcon(MetalLookAndFeel.class, "icons/TreeLeaf.gif");
        assertTrue(icon instanceof UIDefaults.LazyValue);
    }

    public void testMakeInputMap() {
        Object[] keys = new Object[] { "SPACE", "pressed" };
        InputMap inputMap = LookAndFeel.makeInputMap(keys);
        assertTrue(inputMap instanceof InputMapUIResource);
        assertEquals(1, inputMap.size());
    }

    public void testMakeKeyBindings() {
        Object[] binds = { "UP", DefaultEditorKit.beepAction, "DOWN",
                DefaultEditorKit.beginWordAction, "TAB", DefaultEditorKit.beginWordAction };
        JTextComponent.KeyBinding[] b = LookAndFeel.makeKeyBindings(binds);
        assertEquals(3, b.length);
        assertEquals(DefaultEditorKit.beepAction, b[0].actionName);
        assertEquals(DefaultEditorKit.beginWordAction, b[1].actionName);
    }

    public void testMakeKeyBindingsFromErrorArray() {
        Object[] binds = { "UP", DefaultEditorKit.beepAction, "DOWN" };
        try {
            LookAndFeel.makeKeyBindings(binds);
            fail("shall throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    public void testGetDefaults() {
        UIDefaults defaults = lf.getDefaults();
        assertNull(defaults);
    }

    public void testInstallColors() {
        lf = new MetalLookAndFeel();
        JLabel label = new JLabel();
        LookAndFeel.installColors(label, "Tree.selectionBackground", "TextPane.background");
        UIDefaults defaults = lf.getDefaults();
        assertNotNull(defaults);
        assertEquals(UIManager.get("Tree.selectionBackground"), label.getBackground());
        assertEquals(UIManager.get("TextPane.background"), label.getForeground());
    }

    public void testInstallErrorColors() {
        lf = new MetalLookAndFeel();
        JLabel label = new JLabel();
        LookAndFeel.installColors(label, "bbb", "fff");
        assertNull(label.getForeground());
        assertNull(label.getBackground());
    }

    public void testInstallColorAndFonts() {
        lf = new MetalLookAndFeel();
        JLabel label = new JLabel();
        LookAndFeel.installColorsAndFont(label, "Tree.selectionBackground",
                "TextPane.background", "CheckBox.font");
        UIDefaults defaults = lf.getDefaults();
        assertNotNull(defaults);
        assertEquals(UIManager.get("Tree.selectionBackground"), label.getBackground());
        assertEquals(UIManager.get("TextPane.background"), label.getForeground());
        assertEquals(UIManager.get("CheckBox.font"), label.getFont());
    }

    public void testInstallUninstallBorder() throws NullPointerException {
        lf = new MetalLookAndFeel();
        JPanel p = new JPanel();
        UIDefaults defaults = lf.getDefaults();
        assertNotNull(defaults);
        try {
            LookAndFeel.installBorder(null, "Menu.border");
            fail("NullPointerException shall be thrown");
        } catch (NullPointerException e) {
        }
        LookAndFeel.installBorder(p, "Menu.border");
        assertEquals(UIManager.get("Menu.border"), p.getBorder());
        LookAndFeel.uninstallBorder(p);
        assertNull(p.getBorder());

        BasicMenuBarUIExt m = 
            new BasicMenuBarUIExt();
        m.uninstallDefaults(); 
    }

    class BasicMenuBarUIExt extends javax.swing.plaf.basic.BasicMenuBarUI {
        public void uninstallDefaults() {
            super.uninstallDefaults();
        }
    }

    public void testLoadKeyBindings() {
        InputMap map = new InputMap();
        Object[] binds = { "SPACE", DefaultEditorKit.beepAction,
                KeyStroke.getKeyStroke("DOWN"), DefaultEditorKit.beginWordAction };
        try {
            LookAndFeel.loadKeyBindings(null, binds);
            fail("NullPointerException shall be thrown");
        } catch (NullPointerException e) {
        }
        LookAndFeel.loadKeyBindings(map, null);
        assertEquals(0, map.size());
        LookAndFeel.loadKeyBindings(map, binds);
        assertEquals(2, map.size());
    }

    public void testGetDesktopPropertyValue() throws Exception {
        Method setProperty = Toolkit.class.getDeclaredMethod("setDesktopProperty", new Class[] {
                String.class, Object.class });
        setProperty.setAccessible(true);
        final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        setProperty
                .invoke(defaultToolkit, new Object[] { "win.icon.hspacing", new Integer(1) });
        setProperty.invoke(defaultToolkit, new Object[] { "win.frame.captionFont",
                new Font("arial", Font.BOLD, 10) });
        setProperty.invoke(defaultToolkit, new Object[] { "win.frame.activeCaptionColor",
                Color.red });
        assertTrue(LookAndFeel.getDesktopPropertyValue("win.icon.hspacing", "") instanceof Integer);
        assertTrue(LookAndFeel.getDesktopPropertyValue("win.frame.captionFont", "") instanceof FontUIResource);
        Object desktopPropertyValue = LookAndFeel.getDesktopPropertyValue(
                "win.frame.activeCaptionColor", "default");
        assertTrue(desktopPropertyValue instanceof ColorUIResource);
        assertTrue(desktopPropertyValue instanceof ColorUIResource);
        assertEquals(defaultToolkit.getDesktopProperty("win.frame.activeCaptionColor"),
                desktopPropertyValue);
        assertEquals("default", LookAndFeel.getDesktopPropertyValue(
                "win.frame.activeCaptionColor???", "default"));
    }

    public void testGetSupportsWindowsDecorations() {
        assertFalse(lf.getSupportsWindowDecorations());
    }

    public void testInstallProperty() throws Exception {
        if (!isHarmony()) {
            return;
        }
        JComponent comp1 = new JPanel();
        JButton comp2 = new JButton();
        LookAndFeel.installProperty(comp1, "opaque", Boolean.TRUE);
        assertTrue("opaque", comp1.isOpaque());
        LookAndFeel.installProperty(comp1, "opaque", Boolean.FALSE);
        assertFalse("opaque", comp1.isOpaque());
        comp1.setOpaque(true);
        LookAndFeel.installProperty(comp1, "opaque", Boolean.FALSE);
        assertTrue("opaque", comp1.isOpaque());
        LookAndFeel.installProperty(comp2, "opaque", Boolean.TRUE);
        assertTrue("opaque", comp2.isOpaque());
        LookAndFeel.installProperty(comp2, "opaque", Boolean.FALSE);
        assertFalse("opaque", comp2.isOpaque());
        comp1.setOpaque(true);
        LookAndFeel.installProperty(comp2, "opaque", Boolean.FALSE);
        assertFalse("opaque", comp2.isOpaque());
        try {
            LookAndFeel.installProperty(comp1, "iconTextGap", Boolean.TRUE);
            fail("IllegalArgumentException shall be thrown");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            LookAndFeel.installProperty(comp2, "iconTextGap", Boolean.TRUE);
            fail("ClassCastException shall be thrown");
        } catch (ClassCastException e) {
            // expected
        }
        LookAndFeel.installProperty(comp2, "iconTextGap", new Integer(0));
        assertEquals("iconTextGap", 0, comp2.getIconTextGap());
        LookAndFeel.installProperty(comp2, "iconTextGap", new Integer(120));
        assertEquals("iconTextGap", 120, comp2.getIconTextGap());
        comp2.setIconTextGap(300);
        LookAndFeel.installProperty(comp2, "iconTextGap", new Integer(120));
        assertEquals("iconTextGap", 300, comp2.getIconTextGap());
    }
}
