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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.InputMapUIResource;
import javax.swing.plaf.basic.BasicLookAndFeel;

public class UIDefaultsTest extends SwingTestCase {
    UIDefaults uiDefaults;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        uiDefaults = new UIDefaults();
    }

    @Override
    public void tearDown() throws Exception {
        uiDefaults = null;
        super.tearDown();
    }

    public void testGetBoolean() {
        uiDefaults.put("TRUE", Boolean.TRUE);
        assertEquals(1, uiDefaults.size());
        assertEquals(Boolean.TRUE.booleanValue(), uiDefaults.getBoolean("TRUE"));
        uiDefaults.put("TRUE", "a");
        uiDefaults.getBoolean("TRUE");
    }

    public void testGetInt() {
        Integer num = new Integer("10");
        uiDefaults.put("1", num);
        assertEquals(1, uiDefaults.size());
        assertEquals(num.intValue(), uiDefaults.getInt("1"));
        uiDefaults.put("1", "a");
        uiDefaults.getInt("1");
    }

    public void testGetString() {
        String str = "10";
        uiDefaults.put("string", str);
        assertEquals(1, uiDefaults.size());
        assertEquals(str, uiDefaults.getString("string"));
    }

    public void testGetIcon() {
        Icon ic = new Icon() {
            public int getIconHeight() {
                return 0;
            }

            public int getIconWidth() {
                return 0;
            }

            public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            }
        };
        uiDefaults.put("ic", ic);
        assertEquals(1, uiDefaults.size());
        assertEquals(ic, uiDefaults.getIcon("ic"));
    }

    public void testGetBorder() {
        Border b = new Border() {
            public void paintBorder(final Component c, final Graphics g, final int x,
                    final int y, final int w, final int h) {
            }

            public Insets getBorderInsets(final Component c) {
                return null;
            }

            public boolean isBorderOpaque() {
                return false;
            }
        };
        uiDefaults.put("b", b);
        assertEquals(1, uiDefaults.size());
        assertEquals(b, uiDefaults.getBorder("b"));
    }

    public void testGetInsets() {
        Insets ins = new Insets(0, 0, 0, 0);
        uiDefaults.put("ins", ins);
        assertEquals(1, uiDefaults.size());
        assertEquals(ins, uiDefaults.getInsets("ins"));
    }

    public void testGetFont() {
        Font f = new Font("Arial", Font.BOLD, 12);
        uiDefaults.put("f", f);
        assertEquals(1, uiDefaults.size());
        assertEquals(f, uiDefaults.getFont("f"));
    }

    public void testGetDimension() {
        Dimension dim = new Dimension();
        uiDefaults.put("d", dim);
        assertEquals(1, uiDefaults.size());
        assertEquals(dim, uiDefaults.getDimension("d"));
    }

    public void testGetColor() {
        Color c = Color.RED;
        uiDefaults.put("c", c);
        assertEquals(1, uiDefaults.size());
        assertEquals(c, uiDefaults.getColor("c"));
    }

    public void testProxyLazyValue() {
        uiDefaults.put("1", new UIDefaults.ProxyLazyValue("java.lang.String",
                new Object[] { "aaa" }));
        propertyChangeController = new PropertyChangeController();
        uiDefaults.addPropertyChangeListener(propertyChangeController);
        assertEquals(1, uiDefaults.size());
        assertEquals("aaa", uiDefaults.get("1"));
        assertEquals(1, uiDefaults.size());
        assertFalse(propertyChangeController.isChanged());
        uiDefaults.put("2", new UIDefaults.ProxyLazyValue("java.lang.String"));
        assertEquals(2, uiDefaults.size());
        assertEquals("aaa", uiDefaults.get("1"));
        assertEquals("", uiDefaults.get("2"));
        assertNull(uiDefaults.getIcon("2"));
        uiDefaults.put("3", new UIDefaults.ProxyLazyValue("java.lang.Integer", "decode",
                new String[] { "43" }));
        assertEquals(3, uiDefaults.size());
        assertEquals("aaa", uiDefaults.get("1"));
        assertEquals("", uiDefaults.get("2"));
        assertEquals(new Integer("43"), uiDefaults.get("3"));
        assertEquals(43, uiDefaults.getInt("3"));
        uiDefaults.put("4", new UIDefaults.ProxyLazyValue("java.lang.String", "toUpperCase"));
        assertEquals(4, uiDefaults.size());
        assertEquals("aaa", uiDefaults.get("1"));
        assertEquals("", uiDefaults.get("2"));
        assertEquals(new Integer("43"), uiDefaults.get("3"));
        assertNull(uiDefaults.get("4"));
    }

    public void testProxyLazyValueWithThreadClassLoader() throws Exception {
        // Regression test for HARMONY-3420

        final ClassLoader classLoader = new ArrayClassLoader();

        class ThreadGetUIClass extends Thread {
            private UIDefaults.ProxyLazyValue plv =
                    new UIDefaults.ProxyLazyValue("MyLabelUI");
            private Object result;

            public ThreadGetUIClass() {
                setContextClassLoader(classLoader);
            }

            public void run() {
                result = plv.createValue(null);
            }

            public Object getResult() {
                while (true) {
                    try {
                        join();
                        return result;
                    } catch (InterruptedException e) {
                        // Ignored.
                    }
                }
            }
        };

        ThreadGetUIClass thread = new ThreadGetUIClass();
        thread.start();
        Object result = thread.getResult();
        assertNotNull(result);
        assertEquals(result.getClass(), classLoader.loadClass("MyLabelUI"));
    }

    public void testGetUI() {
        setBasicLF();
        final String uiClassName = "javax.swing.plaf.basic.BasicButtonUI";
        uiDefaults.put("ButtonUI", uiClassName);
        assertNull(uiDefaults.get(uiClassName));
        JButton button = new JButton();
        assertTrue(uiDefaults.getUI(button).getClass().getName().equals(uiClassName));
        Object classObj = uiDefaults.get(uiClassName);
        assertNotNull(classObj);
        Object methodObj = uiDefaults.get(classObj);
        assertNotNull(methodObj);
    }

    public void testGetUIWithClassLoader() throws Exception {
        // Regression test for HARMONY-3385

        uiDefaults.put("LabelUI", "MyLabelUI");
        Class<? extends JComponent> cls = (Class<? extends JComponent>)
                new ArrayClassLoader().loadClass("MyJLabel");
        JComponent component = cls.newInstance();
        assertNotNull(uiDefaults.getUI(component));
    }

    public void testGetUIClass() throws ClassNotFoundException {
        setBasicLF();
        final String uiClassName = "javax.swing.plaf.basic.BasicButtonUI";
        uiDefaults.put("ButtonUI", uiClassName);
        assertEquals(Class.forName(uiClassName), uiDefaults.getUIClass("ButtonUI"));
    }

    public void testGetUIClassWithClassLoader() throws Exception {
        setBasicLF();
        final String uiClassName = "javax.swing.plaf.basic.BasicButtonUI";
        uiDefaults.put("ButtonUI", uiClassName);
        assertEquals(Class.forName(uiClassName), uiDefaults.getUIClass("ButtonUI", null));
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try {
                    assertEquals(Class.forName(uiClassName), uiDefaults.getUIClass("ButtonUI",
                            new URLClassLoader(new URL[0])));
                } catch (ClassNotFoundException e) {
                }
                return null;
            }
        });
    }

    public void testGetUIClassWithThreadClassLoader() throws Exception {
        // Regression test for HARMONY-3398

        final ClassLoader classLoader = new ArrayClassLoader();

        class ThreadGetUIClass extends Thread {
            private UIDefaults uid = new UIDefaults(
                    new Object[] { "LabelUI", "MyLabelUI" });
            private Class result;

            public ThreadGetUIClass() {
                setContextClassLoader(classLoader);
            }

            public void run() {
                result = uid.getUIClass("LabelUI");
            }

            public Class getResult() {
                while (true) {
                    try {
                        join();
                        return result;
                    } catch (InterruptedException e) {
                        // Ignored.
                    }
                }
            }
        };

        ThreadGetUIClass thread = new ThreadGetUIClass();
        thread.start();
        Class result = thread.getResult();
        assertNotNull(result);
        assertEquals(result, classLoader.loadClass("MyLabelUI"));
    }

    public void testGetValueFromResourceBundle() {
        String bundleName = "javax.swing.TestBundle";
        assertNull(uiDefaults.get("OptionPane.okButtonText"));
        propertyChangeController = new PropertyChangeController();
        uiDefaults.addPropertyChangeListener(propertyChangeController);
        uiDefaults.addResourceBundle(bundleName);
        assertFalse(propertyChangeController.isChanged());
        ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
        assertEquals(bundle.getString("OptionPane.okButtonText"), uiDefaults
                .get("OptionPane.okButtonText"));
        propertyChangeController.reset();
        uiDefaults.removeResourceBundle(bundleName);
        assertFalse(propertyChangeController.isChanged());
    }

    public void testInputMap() {
        uiDefaults.put("1", new UIDefaults.LazyInputMap(new Object[] { "1", "11", "2", "22",
                "3", "33", "4", "44" }));
        assertTrue(uiDefaults.get("1") instanceof InputMapUIResource);
        InputMapUIResource map = (InputMapUIResource) uiDefaults.get("1");
        assertEquals(4, map.allKeys().length);
    }

    public void testCreateUIDefaultsTable() {
        assertEquals(0, uiDefaults.size());
        UIDefaults uid = new UIDefaults(new Object[] { "1", "1v", "2", "2v", "3", "3v", "4",
                "4v" });
        assertEquals(4, uid.size());
        assertEquals("1v", uid.get("1"));
        assertEquals("3v", uid.getString("3"));
        uiDefaults.put("key", null);
        assertNull(uiDefaults.get("key"));
    }

    public void testErrorGetUI() {
        final PrintStream orig = System.err;
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final PrintStream errStream = new PrintStream(byteStream);
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                System.setErr(errStream);
                return null;
            }
        });
        uiDefaults.put("ButtonUI", "plaf.windows.WWWWWinButtonUI");
        JButton button = new JButton();
        uiDefaults.getUI(button);
        String error = byteStream.toString();
        assertTrue(error.indexOf("failed") > 0);
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                System.setErr(orig);
                return null;
            }
        });
    }

    public void testLocale() {
        assertTrue(uiDefaults.getDefaultLocale() == Locale.getDefault());
        propertyChangeController = new PropertyChangeController();
        uiDefaults.addPropertyChangeListener(propertyChangeController);
        uiDefaults.setDefaultLocale(Locale.UK);
        assertFalse(propertyChangeController.isChanged());
        String bundleName = "javax.swing.TestBundle";
        assertNull(uiDefaults.get("OptionPane.titleText"));
        uiDefaults.addResourceBundle(bundleName);
        assertEquals(ResourceBundle.getBundle(bundleName, Locale.GERMAN).getString(
                "OptionPane.titleText"), uiDefaults.getString("OptionPane.titleText",
                Locale.GERMAN));
        assertEquals(ResourceBundle.getBundle(bundleName, Locale.ITALIAN).getString(
                "OptionPane.titleText"), uiDefaults.getString("OptionPane.titleText",
                Locale.ITALIAN));
        assertEquals(ResourceBundle.getBundle(bundleName, Locale.KOREAN).getString(
                "OptionPane.titleText"), uiDefaults.getString("OptionPane.titleText",
                Locale.KOREAN));
    }

    public void testPropertyListeners() {
        final List<Object> count = new Vector<Object>();
        PropertyChangeListener listener = new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent pce) {
                count.add(new Object());
            }
        };
        uiDefaults.put("1", "1v");
        assertEquals(0, uiDefaults.getPropertyChangeListeners().length);
        uiDefaults.addPropertyChangeListener(listener);
        assertEquals(1, uiDefaults.getPropertyChangeListeners().length);
        assertEquals(0, count.size());
        uiDefaults.put("2", "2v");
        assertEquals(1, count.size());
        uiDefaults.put("2", "2v");
        assertEquals(1, count.size());
        uiDefaults.removePropertyChangeListener(listener);
        assertEquals(0, uiDefaults.getPropertyChangeListeners().length);
    }

    public void testPutDefaultsChangeEvent() {
        propertyChangeController = new PropertyChangeController();
        uiDefaults.addPropertyChangeListener(propertyChangeController);
        uiDefaults.putDefaults(new Object[] { "1", "11", "2", "22", "3", "33", "4", "44" });
        assertTrue(propertyChangeController.isChanged("UIDefaults"));
    }

    public void testCreateProxyLazyValueWithUIResourceAsParameter() {
        uiDefaults.put("1", new UIDefaults.ProxyLazyValue(
                "javax.swing.plaf.BorderUIResource$LineBorderUIResource",
                new Object[] { new ColorUIResource(255, 255, 0) }));
        assertEquals(1, uiDefaults.size());
        assertNotNull(uiDefaults.get("1"));
        assertTrue(uiDefaults.get("1") instanceof Border);
    }

    public void testPut() throws Exception {
        propertyChangeController = new PropertyChangeController();
        uiDefaults.addPropertyChangeListener(propertyChangeController);
        assertNull(uiDefaults.put("1", "val1"));
        assertEquals(1, uiDefaults.size());
        assertTrue(propertyChangeController.isChanged("1"));
        propertyChangeController.reset();
        assertNull(uiDefaults.put("2", "val2"));
        assertEquals(2, uiDefaults.size());
        assertTrue(propertyChangeController.isChanged("2"));
        propertyChangeController.reset();
        assertEquals("val2", uiDefaults.put("2", "val2"));
        assertEquals(2, uiDefaults.size());
        assertFalse(propertyChangeController.isChanged("2"));
        propertyChangeController.reset();
        assertEquals("val2", uiDefaults.put("2", null));
        assertTrue(propertyChangeController.isChanged("2"));
        assertEquals(1, uiDefaults.size());
        propertyChangeController.reset();
        uiDefaults.put("nokey", null);
        assertTrue(propertyChangeController.isChanged("nokey"));
        assertEquals(1, uiDefaults.size());
    }

    private void setBasicLF() {
        try {
            UIManager.setLookAndFeel(new BasicLookAndFeel() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isNativeLookAndFeel() {
                    return true;
                }

                @Override
                public boolean isSupportedLookAndFeel() {
                    return true;
                }

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
            });
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }

    /**
     * Special classloader for classloader-based tests.
     */
    private static class ArrayClassLoader extends ClassLoader {

        private static Hashtable<String, byte[]> table =
                new Hashtable<String, byte[]>();

        static {
            table.put("MyJLabel", new byte[] {
                /*
                 * public class MyJLabel extends JLabel {
                 *     public String getUIClassID() {
                 *         return "LabelUI";
                 *     }
                 * }
                 */
                -54, -2, -70, -66, 0, 0, 0, 49, 0, 17, 10, 0, 4, 0, 13, 8,
                0, 14, 7, 0, 15, 7, 0, 16, 1, 0, 6, 60, 105, 110, 105, 116,
                62, 1, 0, 3, 40, 41, 86, 1, 0, 4, 67, 111, 100, 101, 1, 0,
                15, 76, 105, 110, 101, 78, 117, 109, 98, 101, 114, 84, 97,
                98, 108, 101, 1, 0, 12, 103, 101, 116, 85, 73, 67, 108, 97,
                115, 115, 73, 68, 1, 0, 20, 40, 41, 76, 106, 97, 118, 97,
                47, 108, 97, 110, 103, 47, 83, 116, 114, 105, 110, 103, 59,
                1, 0, 10, 83, 111, 117, 114, 99, 101, 70, 105, 108, 101, 1,
                0, 13, 77, 121, 74, 76, 97, 98, 101, 108, 46, 106, 97, 118,
                97, 12, 0, 5, 0, 6, 1, 0, 7, 76, 97, 98, 101, 108, 85, 73,
                1, 0, 8, 77, 121, 74, 76, 97, 98, 101, 108, 1, 0, 18, 106,
                97, 118, 97, 120, 47, 115, 119, 105, 110, 103, 47, 74, 76,
                97, 98, 101, 108, 0, 33, 0, 3, 0, 4, 0, 0, 0, 0, 0, 2, 0,
                1, 0, 5, 0, 6, 0, 1, 0, 7, 0, 0, 0, 29, 0, 1, 0, 1, 0, 0,
                0, 5, 42, -73, 0, 1, -79, 0, 0, 0, 1, 0, 8, 0, 0, 0, 6, 0,
                1, 0, 0, 0, 3, 0, 1, 0, 9, 0, 10, 0, 1, 0, 7, 0, 0, 0, 27,
                0, 1, 0, 1, 0, 0, 0, 3, 18, 2, -80, 0, 0, 0, 1, 0, 8, 0, 0,
                0, 6, 0, 1, 0, 0, 0, 5, 0, 1, 0, 11, 0, 0, 0, 2, 0, 12 });

            table.put("MyLabelUI", new byte[] {
                /*
                 * public class MyLabelUI extends LabelUI {
                 *     public static ComponentUI createUI(JComponent c) {
                 *         return new MyLabelUI();
                 *     }
                 * }
                 */
                -54, -2, -70, -66, 0, 0, 0, 49, 0, 16, 10, 0, 4, 0, 13, 7,
                0, 14, 10, 0, 2, 0, 13, 7, 0, 15, 1, 0, 6, 60, 105, 110,
                105, 116, 62, 1, 0, 3, 40, 41, 86, 1, 0, 4, 67, 111, 100,
                101, 1, 0, 15, 76, 105, 110, 101, 78, 117, 109, 98, 101,
                114, 84, 97, 98, 108, 101, 1, 0, 8, 99, 114, 101, 97, 116,
                101, 85, 73, 1, 0, 56, 40, 76, 106, 97, 118, 97, 120, 47,
                115, 119, 105, 110, 103, 47, 74, 67, 111, 109, 112, 111,
                110, 101, 110, 116, 59, 41, 76, 106, 97, 118, 97, 120, 47,
                115, 119, 105, 110, 103, 47, 112, 108, 97, 102, 47, 67,
                111, 109, 112, 111, 110, 101, 110, 116, 85, 73, 59, 1, 0,
                10, 83, 111, 117, 114, 99, 101, 70, 105, 108, 101, 1, 0,
                14, 77, 121, 76, 97, 98, 101, 108, 85, 73, 46, 106, 97,
                118, 97, 12, 0, 5, 0, 6, 1, 0, 9, 77, 121, 76, 97, 98, 101,
                108, 85, 73, 1, 0, 24, 106, 97, 118, 97, 120, 47, 115, 119,
                105, 110, 103, 47, 112, 108, 97, 102, 47, 76, 97, 98, 101,
                108, 85, 73, 0, 33, 0, 2, 0, 4, 0, 0, 0, 0, 0, 2, 0, 1, 0,
                5, 0, 6, 0, 1, 0, 7, 0, 0, 0, 29, 0, 1, 0, 1, 0, 0, 0, 5,
                42, -73, 0, 1, -79, 0, 0, 0, 1, 0, 8, 0, 0, 0, 6, 0, 1, 0,
                0, 0, 5, 0, 9, 0, 9, 0, 10, 0, 1, 0, 7, 0, 0, 0, 32, 0, 2,
                0, 1, 0, 0, 0, 8, -69, 0, 2, 89, -73, 0, 3, -80, 0, 0, 0,
                1, 0, 8, 0, 0, 0, 6, 0, 1, 0, 0, 0, 7, 0, 1, 0, 11, 0, 0,
                0, 2, 0, 12 });
            }

        protected Class findClass(String name) throws ClassNotFoundException {
            byte[] bytes = table.get(name);

            if (bytes == null) {
                throw new ClassNotFoundException(name);
            }
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
