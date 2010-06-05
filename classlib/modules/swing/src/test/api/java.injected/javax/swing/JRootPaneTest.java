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
 * @author Vadim L. Bogdanov
 */
package javax.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRootPaneUI;

public class JRootPaneTest extends SwingTestCase {
    /*
     * This class overload protected methods with public methods
     */
    private class TestRootPane extends JRootPane {
        private static final long serialVersionUID = 1L;

        @Override
        public String paramString() {
            return super.paramString();
        }

        @Override
        public Container createContentPane() {
            return super.createContentPane();
        }

        @Override
        public Component createGlassPane() {
            return super.createGlassPane();
        }

        @Override
        public JLayeredPane createLayeredPane() {
            return super.createLayeredPane();
        }

        @Override
        public LayoutManager createRootLayout() {
            return super.createRootLayout();
        }
    }

    /*
     * This class is used to test that some property is (or is not) a bound property
     */
    private class MyPropertyChangeListener implements PropertyChangeListener {
        public boolean ok;

        MyPropertyChangeListener() {
            ok = false;
        }

        public void propertyChange(final PropertyChangeEvent e) {
            ok = true;
        }
    }

    private JRootPane rootPane;

    public JRootPaneTest(final String name) {
        super(name);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        rootPane = new JRootPane();
        JFrame.setDefaultLookAndFeelDecorated(false);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for String getUIClassID()
     */
    public void testGetUIClassID() {
        assertEquals("RootPaneUI", rootPane.getUIClassID());
    }

    @SuppressWarnings("deprecation")
    public void testSetGetMenuBar() {
        assertNull(rootPane.getMenuBar());
        JMenuBar menuBar = new JMenuBar();
        rootPane.setMenuBar(menuBar);
        assertTrue(rootPane.getMenuBar() == menuBar);
        rootPane.setMenuBar(null);
        assertNull(rootPane.getMenuBar());
    }

    public void testJRootPaneConstructor() {
        assertTrue(rootPane.getContentPane() != null);
        assertTrue(rootPane.getLayeredPane() != null);
        assertTrue(rootPane.getGlassPane() != null);
        assertNull(rootPane.getDefaultButton());
    }

    public void testSetGetContentPane() {
        JPanel contentPane = new JPanel();
        contentPane.setOpaque(true);
        rootPane.setContentPane(contentPane);
        assertTrue(contentPane == rootPane.getContentPane());
    }

    public void testSetGetLayeredPane() {
        JLayeredPane pane = new JLayeredPane();
        rootPane.setLayeredPane(pane);
        assertTrue(pane == rootPane.getLayeredPane());
        boolean thrown = false;
        try {
            rootPane.setLayeredPane(null);
        } catch (IllegalComponentStateException e) {
            thrown = true;
        } finally {
            assertTrue(thrown);
        }
        assertTrue(rootPane.getLayeredPane() != null);
    }

    public void testSetGetGlassPane() {
        JPanel pane = new JPanel();
        pane.setVisible(false);
        rootPane.setGlassPane(pane);
        assertTrue(pane == rootPane.getGlassPane());
        boolean thrown = false;
        try {
            rootPane.setGlassPane(null);
        } catch (NullPointerException e) {
            thrown = true;
        } finally {
            assertTrue(thrown);
        }
        assertTrue(rootPane.getGlassPane() != null);
    }

    public void testSetGetJMenuBar() {
        assertNull(rootPane.getJMenuBar());
        JMenuBar menuBar = new JMenuBar();
        rootPane.setJMenuBar(menuBar);
        assertTrue(rootPane.getJMenuBar() == menuBar);
        rootPane.setJMenuBar(null);
        assertNull(rootPane.getJMenuBar());
    }

    public void testSetGetUI() {
        BasicRootPaneUI ui = new BasicRootPaneUI();
        rootPane.setUI(ui);
        assertTrue(rootPane.getUI() == ui);
    }

    public void testUpdateUI() {
        rootPane.updateUI();
        ComponentUI ui1 = rootPane.getUI();
        ComponentUI ui2 = UIManager.getUI(rootPane);
        // at least names of classes must be the same
        assertEquals(ui2.getClass().getName(), ui1.getClass().getName());
    }

    public void testSetGetWindowDecorationStyle() {
        // rootPane must be inside window in this test
        JFrame frame = new JFrame();
        rootPane = frame.getRootPane();
        assertEquals(JRootPane.NONE, rootPane.getWindowDecorationStyle());
        int newStyle = JRootPane.FRAME;
        rootPane.setWindowDecorationStyle(newStyle);
        assertEquals(newStyle, rootPane.getWindowDecorationStyle());
        // test for invalid style - an exception must be thrown
        boolean ok = false;
        try {
            rootPane.setWindowDecorationStyle(101);
        } catch (IllegalArgumentException e) {
            ok = true;
        } finally {
            assertTrue(ok);
        }
        // test that this is a bound property
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        rootPane.addPropertyChangeListener("windowDecorationStyle", listener);
        assertFalse(listener.ok);
        rootPane.setWindowDecorationStyle(JRootPane.ERROR_DIALOG);
        assertTrue(listener.ok);
        frame.dispose();
    }

    public void testAddImpl() {
        JPanel pane = new JPanel();
        // setGlassPane() calls addImpl(), which enshures that glass pane
        // has index 0
        rootPane.setGlassPane(pane);
        // glass pane must always have index 0
        assertTrue(rootPane.getComponent(0) == pane);
        pane = new JPanel();
        rootPane.add(pane, 0);
        // not a glass pane, cannot have index 0
        assertFalse(rootPane.getComponent(0) == pane);
    }

    public void testIsValidateRoot() {
        assertTrue(rootPane.isValidateRoot());
    }

    public void testIsOptimizedDrawingEnabled() {
        rootPane.getGlassPane().setVisible(false);
        assertTrue(rootPane.isOptimizedDrawingEnabled());
        rootPane.getGlassPane().setVisible(true);
        assertFalse(rootPane.isOptimizedDrawingEnabled());
    }

    public void testParamString() {
        TestRootPane pane = new TestRootPane();
        assertTrue(pane.paramString() != null);
    }

    public void testCreateRootLayout() {
        TestRootPane pane = new TestRootPane();
        LayoutManager layout = pane.createRootLayout();
        assertTrue(layout != null);
        assertTrue(layout instanceof JRootPane.RootLayout);
    }

    public void testSetGetDefaultButton() {
        assertNull(rootPane.getDefaultButton());
        JButton button = new JButton();
        rootPane.setDefaultButton(button);
        assertTrue(rootPane.getDefaultButton() == button);
        rootPane.setDefaultButton(null);
        assertNull(rootPane.getDefaultButton());
        // test that this is a bound property
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        rootPane.addPropertyChangeListener("defaultButton", listener);
        assertFalse(listener.ok);
        rootPane.setDefaultButton(button);
        assertTrue(listener.ok);
        rootPane.setDefaultButton(null);
        button.setDefaultCapable(false);
        rootPane.setDefaultButton(button);
        assertSame(button, rootPane.getDefaultButton());
    }

    /*
     * Test inner class JRootPane.RootLayout
     */
    //
    public void testRootLayout() {
        final Dimension base = new Dimension(640, 480);
        rootPane.setSize(base);
        rootPane.getLayout().layoutContainer(rootPane);
        // test without menu
        assertEquals(new Rectangle(0, 0, base.width, base.height), rootPane.getGlassPane()
                .getBounds());
        assertEquals(new Rectangle(0, 0, base.width, base.height), rootPane.getLayeredPane()
                .getBounds());
        assertEquals(new Rectangle(0, 0, base.width, base.height), rootPane.getContentPane()
                .getBounds());
        // test with menu
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new JMenu("Menu"));
        rootPane.setJMenuBar(menuBar);
        rootPane.getLayout().layoutContainer(rootPane);
        assertEquals(new Rectangle(0, 0, base.width, base.height), rootPane.getGlassPane()
                .getBounds());
        assertEquals(new Rectangle(0, 0, base.width, base.height), rootPane.getLayeredPane()
                .getBounds());
        assertEquals(new Rectangle(0, menuBar.getHeight(), base.width, base.height
                - menuBar.getHeight()), rootPane.getContentPane().getBounds());
        assertEquals(new Rectangle(0, 0, base.width, menuBar.getHeight()), rootPane
                .getJMenuBar().getBounds());
        // test with menu and border
        Border border = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        rootPane.setBorder(border);
        rootPane.getLayout().layoutContainer(rootPane);
        Insets insets = border.getBorderInsets(menuBar);
        int insetsWidth = insets.left + insets.right;
        int insetsHeight = insets.top + insets.bottom;
        assertEquals(new Rectangle(insets.left, insets.top, base.width - insetsWidth,
                base.height - insetsHeight), rootPane.getGlassPane().getBounds());
        assertEquals(new Rectangle(insets.left, insets.top, base.width - insetsWidth,
                base.height - insetsHeight), rootPane.getLayeredPane().getBounds());
        assertEquals(new Rectangle(0, menuBar.getHeight(), base.width - insetsWidth,
                base.height - insetsHeight - menuBar.getHeight()), rootPane.getContentPane()
                .getBounds());
        assertEquals(new Rectangle(0, 0, base.width - insetsWidth, menuBar.getHeight()),
                rootPane.getJMenuBar().getBounds());
    }

    /*
     * Class under test for void addNotify()
     */
    public void testAddNotify() {
        // Note: how to test?
    }

    /*
     * Class under test for void removeNotify()
     */
    public void testRemoveNotify() {
        // Note: how to test?
    }

    /*
     * Class under test for Container createContentPane()
     */
    public void testCreateContentPane() {
        TestRootPane root = new TestRootPane();
        JComponent content = (JComponent) root.createContentPane();
        assertTrue(content != null);
        assertTrue(content.isOpaque());
        assertTrue(content.getLayout() instanceof BorderLayout);
    }

    /*
     * Class under test for Container createGlassPane()
     */
    public void testCreateGlassPane() {
        TestRootPane root = new TestRootPane();
        JComponent glass = (JComponent) root.createGlassPane();
        assertTrue(glass != null);
        assertFalse(glass.isVisible());
        // there is nothing about default opacity in the docs,
        // but it really must be false
        assertFalse(glass.isOpaque());
    }

    /*
     * Class under test for Container createLayeredPane()
     */
    public void testCreateLayeredPane() {
        TestRootPane root = new TestRootPane();
        JLayeredPane layered = root.createLayeredPane();
        assertTrue(layered != null);
    }

    /*
     * Class under test for AccessibleContext getAccessibleContext()
     */
    public void testGetAccessibleContext() {
        AccessibleContext c = rootPane.getAccessibleContext();
        assertTrue("instanceof AccessibleJRootPane", c instanceof JRootPane.AccessibleJRootPane);
        assertTrue("AccessibleRole is ok", c.getAccessibleRole() == AccessibleRole.ROOT_PANE);
        assertNull("AccessibleName is ok", c.getAccessibleName());
        assertNull("AccessibleDescription is ok", c.getAccessibleDescription());
        rootPane.add(new JPanel());
        rootPane.getLayeredPane().add(new JPanel());
        //System.out.println(c.getAccessibleChildrenCount());
        //System.out.println(c.getAccessibleChild(0));
        assertTrue("AccessibleChildrenCount == 1", c.getAccessibleChildrenCount() == 1);
        assertTrue("AccessibleChild(0) == contentPane", c.getAccessibleChild(0) == rootPane
                .getContentPane());
    }
}
