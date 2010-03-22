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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalButtonUI;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class UIManagerTest extends SwingTestCase {
    Locale locale = Locale.US;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        removePropertyChangeListeners();
    }

    public void testPut() {
        UIManager.put("1", "1v");
        assertEquals("1v", UIManager.get("1"));
        assertEquals("1v", UIManager.getDefaults().get("1"));
        assertNull(UIManager.getLookAndFeelDefaults().get("1"));
        assertNull(UIManager.getLookAndFeel().getDefaults().get("1"));
    }

    public void testGet() {
        UIManager.put("1", "1v");
        assertEquals("1v", UIManager.get("1"));
        assertEquals("1v", UIManager.get("1", locale));
        assertEquals("Close", UIManager.get("InternalFrame.closeButtonToolTip", Locale.US));
    }

    public void testGetBorder() {
        Object result = UIManager.get("Menu.border");
        assertNotNull(result);
        assertTrue(result instanceof Border);
        result = UIManager.getBorder("Menu.border");
        assertNotNull(result);
        assertNotNull(UIManager.getBorder("Menu.border", locale));
    }

    public void testGetInt() {
        Object result = UIManager.get("SplitPane.dividerSize");
        assertTrue(result instanceof Integer);
        int num = UIManager.getInt("SplitPane.dividerSize");
        assertTrue(num > 0);
        assertTrue(UIManager.getInt("SplitPane.dividerSize", locale) > 0);
    }

    public void testGetColor() {
        Object result = UIManager.get("InternalFrame.borderColor");
        assertNotNull(result);
        assertTrue(result instanceof Color);
        result = UIManager.getColor("InternalFrame.borderColor");
        assertNotNull(result);
        assertNotNull(UIManager.getColor("InternalFrame.borderColor", locale));
    }

    public void testGetInsets() {
        Object result = UIManager.get("Button.margin");
        assertNotNull(result);
        assertTrue(result instanceof Insets);
        result = UIManager.getInsets("Button.margin");
        assertNotNull(result);
        assertNotNull(UIManager.getInsets("Button.margin", locale));
    }

    public void testGetFont() {
        Object result = UIManager.get("TextPane.font");
        assertNotNull(result);
        assertTrue(result instanceof Font);
        result = UIManager.getFont("TextPane.font");
        assertNotNull(result);
        assertNotNull(UIManager.getFont("TextPane.font", locale));
    }

    public void testGetBoolean() {
        Object result = UIManager.get("MenuItem.borderPainted");
        assertNotNull(result);
        assertTrue(result instanceof Boolean);
        boolean b = UIManager.getBoolean("MenuItem.borderPainted");
        assertTrue(b);
        assertTrue(UIManager.getBoolean("MenuItem.borderPainted", locale));
    }

    public void testGetString() {
        Object result = UIManager.get("TabbedPaneUI");
        assertNotNull(result);
        assertTrue(result instanceof String);
        result = UIManager.getString("TabbedPaneUI");
        assertNotNull(result);
        assertNotNull(UIManager.getString("TabbedPaneUI", locale));
    }

    public void testGetDimension() {
        Object result = UIManager.get("Spinner.arrowButtonSize");
        assertNotNull(result);
        assertTrue(result instanceof Dimension);
        result = UIManager.getDimension("Spinner.arrowButtonSize");
        assertNotNull(result);
        assertNotNull(UIManager.getDimension("Spinner.arrowButtonSize", locale));
    }

    public void testGetIcon() {
        Object result = UIManager.get("Menu.arrowIcon");
        assertNotNull(result);
        assertTrue(result instanceof Icon);
        result = UIManager.getIcon("Menu.arrowIcon");
        assertNotNull(result);
        assertNotNull(UIManager.getIcon("Menu.arrowIcon", locale));
    }

    public void testGetUI() throws Exception {
        UIManager.setLookAndFeel(new MetalLookAndFeel());
        JComponent c = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getUIClassID() {
                return "ButtonUI";
            }
        };
        ComponentUI ui = UIManager.getUI(c);
        assertTrue(ui instanceof MetalButtonUI);
    }

    public void testGetCrossPlatformLookAndFeelClassName() {
        assertEquals("javax.swing.plaf.metal.MetalLookAndFeel", UIManager
                .getCrossPlatformLookAndFeelClassName());
    }

    public void testGetSystemLookAndFeelClassName() {
        if (System.getProperty("os.name").indexOf("Linux") > 0) {
            assertEquals("javax.swing.plaf.metal.MetalLookAndFeel", UIManager
                    .getSystemLookAndFeelClassName());
        }
    }

    public void testGetDefaults() {
        assertNotNull(UIManager.getLookAndFeel());
        assertNotNull(UIManager.getDefaults());
        assertNotNull(UIManager.getLookAndFeelDefaults());
    }

    public void testSetUnsupportedLF() throws Exception {
        LookAndFeel laf = UIManager.getLookAndFeel();
        LookAndFeel lf = createUnsupportedLF();
        try {
            UIManager.setLookAndFeel(lf);
            fail("UnsupportedLookAndFeelException shall be thrown");
        } catch (UnsupportedLookAndFeelException e) {
            assertTrue(e.getMessage().indexOf("not supported on this platform") > 0);
        }
        UIManager.setLookAndFeel(laf);
    }

    public void testSetLookAndFeel() throws Exception {
        LookAndFeel laf = UIManager.getLookAndFeel();
        propertyChangeController = new PropertyChangeController();
        UIManager.addPropertyChangeListener(propertyChangeController);
        UIManager.setLookAndFeel(new MetalLookAndFeel());
        assertTrue(propertyChangeController.isChanged("lookAndFeel"));
        assertTrue(UIManager.getLookAndFeel() instanceof MetalLookAndFeel);
        UIManager.setLookAndFeel(laf);
        UIManager.removePropertyChangeListener(propertyChangeController);
    }

    public void testLookAndFeelInfo() {
        LookAndFeelInfo lfInfo = new LookAndFeelInfo("Metal",
                "javax.swing.plaf.metal.MetalLookAndFeel");
        assertEquals("Metal", lfInfo.getName());
        assertEquals("javax.swing.plaf.metal.MetalLookAndFeel", lfInfo.getClassName());
        assertEquals(
                "javax.swing.UIManager$LookAndFeelInfo[Metal javax.swing.plaf.metal.MetalLookAndFeel]",
                lfInfo.toString());
    }

    public void testPropertyChangeListeners() throws Exception {
        LookAndFeel laf = UIManager.getLookAndFeel();
        UIManager.setLookAndFeel(new MetalLookAndFeel());
        propertyChangeController = new PropertyChangeController();
        UIManager.addPropertyChangeListener(propertyChangeController);
        assertEquals(1, UIManager.getPropertyChangeListeners().length);
        assertFalse(propertyChangeController.isChanged());
        assertEquals(0, UIManager.getDefaults().getPropertyChangeListeners().length);
        assertFalse(propertyChangeController.isChanged());
        UIManager.getDefaults();
        assertFalse(propertyChangeController.isChanged());
        UIManager.put("1", "1v");
        assertFalse(propertyChangeController.isChanged());
        MetalLookAndFeel metalLookAndFeel = new MetalLookAndFeel();
        UIManager.setLookAndFeel(metalLookAndFeel);
        assertTrue(propertyChangeController.isChanged("lookAndFeel"));
        propertyChangeController.reset();
        UIManager.setLookAndFeel(metalLookAndFeel);
        assertFalse(propertyChangeController.isChanged());
        UIManager.removePropertyChangeListener(propertyChangeController);
        assertEquals(0, UIManager.getPropertyChangeListeners().length);
        UIManager.setLookAndFeel(laf);
    }

    public void testSetInstalledLFs() {
        LookAndFeelInfo[] previousValues = UIManager.getInstalledLookAndFeels();
        UIManager.setInstalledLookAndFeels(new LookAndFeelInfo[] {});
        UIManager.installLookAndFeel("new", "newClass");
        assertEquals(1, UIManager.getInstalledLookAndFeels().length);
        assertEquals("new", UIManager.getInstalledLookAndFeels()[0].getName());
        assertEquals("newClass", UIManager.getInstalledLookAndFeels()[0].getClassName());
        UIManager.setInstalledLookAndFeels(new LookAndFeelInfo[] {});
        LookAndFeelInfo lfInfo0 = new LookAndFeelInfo("Metal",
                "javax.swing.plaf.metal.MetalLookAndFeel");
        LookAndFeelInfo lfInfo1 = new LookAndFeelInfo("-", "1");
        UIManager.installLookAndFeel(lfInfo0);
        assertEquals(1, UIManager.getInstalledLookAndFeels().length);
        assertEquals(lfInfo0.getName(), UIManager.getInstalledLookAndFeels()[0].getName());
        assertEquals(lfInfo0.getClassName(), UIManager.getInstalledLookAndFeels()[0]
                .getClassName());
        UIManager.setInstalledLookAndFeels(new LookAndFeelInfo[] { lfInfo0, lfInfo1 });
        assertEquals(2, UIManager.getInstalledLookAndFeels().length);
        UIManager.setInstalledLookAndFeels(previousValues);
    }

    public void testGetInstalledLFs() throws Exception {
        LookAndFeelInfo[] installedLookAndFeels = UIManager.getInstalledLookAndFeels();
        if (isHarmony()) {
            assertEquals(1, installedLookAndFeels.length);
        } else {
            assertEquals(3, installedLookAndFeels.length);
        }
        boolean foundMetalLF = false;
        for (int i = 0; i < installedLookAndFeels.length; i++) {
            if ("Metal".equals(installedLookAndFeels[i].getName())) {
                foundMetalLF = true;
            }
        }
        assertTrue(foundMetalLF);
    }
    
    public void testAuxillaryLafs() {

        MetalLookAndFeel auxLaf1 = new MetalLookAndFeel();
        MetalLookAndFeel auxLaf2 = new MetalLookAndFeel();

        assertEquals(UIManager.getAuxiliaryLookAndFeels().length, 0);
        UIManager.addAuxiliaryLookAndFeel(auxLaf1);
        assertEquals(UIManager.getAuxiliaryLookAndFeels().length, 1);
        UIManager.addAuxiliaryLookAndFeel(auxLaf1);
        assertEquals(UIManager.getAuxiliaryLookAndFeels().length, 1);
        UIManager.addAuxiliaryLookAndFeel(auxLaf2);
        assertEquals(UIManager.getAuxiliaryLookAndFeels().length, 2);
        UIManager.addAuxiliaryLookAndFeel(createUnsupportedLF());
        assertEquals(UIManager.getAuxiliaryLookAndFeels().length, 2);

        assertTrue(UIManager.removeAuxiliaryLookAndFeel(auxLaf1));
        assertEquals(UIManager.getAuxiliaryLookAndFeels().length, 1);
        assertFalse(UIManager.removeAuxiliaryLookAndFeel(auxLaf1));
        assertFalse(UIManager
                .removeAuxiliaryLookAndFeel(new MetalLookAndFeel()));
        assertEquals(UIManager.getAuxiliaryLookAndFeels().length, 1);
        assertTrue(UIManager.removeAuxiliaryLookAndFeel(auxLaf2));
        assertEquals(UIManager.getAuxiliaryLookAndFeels().length, 0);

    }

    private LookAndFeel createUnsupportedLF() {
        return new LookAndFeel() {
            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public String getID() {
                return null;
            }

            @Override
            public String getName() {
                return null;
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
    }

    private static void removePropertyChangeListeners() {
        PropertyChangeListener[] array = UIManager.getPropertyChangeListeners();
        for (int i = 0; i < array.length; i++) {
            UIManager.removePropertyChangeListener(array[i]);
        }
    }
}
