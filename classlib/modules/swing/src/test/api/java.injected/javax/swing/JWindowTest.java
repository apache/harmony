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
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

public class JWindowTest extends SwingTestCase {
    /*
     * This class is used to test protected methods
     */
    static private class TestWindow extends JWindow {
        private static final long serialVersionUID = 1L;

        public static boolean createRootPaneCalled = false;

        public static boolean setRootPaneCalled = false;

        @Override
        public JRootPane createRootPane() {
            createRootPaneCalled = true;
            return super.createRootPane();
        }

        @Override
        public void setRootPane(final JRootPane root) {
            setRootPaneCalled = true;
            super.setRootPane(root);
        }

        @Override
        public void setRootPaneCheckingEnabled(final boolean enabled) {
            super.setRootPaneCheckingEnabled(enabled);
        }

        @Override
        public boolean isRootPaneCheckingEnabled() {
            return super.isRootPaneCheckingEnabled();
        }

        @Override
        public void addImpl(final Component comp, final Object constraints, final int index) {
            super.addImpl(comp, constraints, index);
        }

        public static void initStaticVars() {
            createRootPaneCalled = false;
            setRootPaneCalled = false;
        }

        @Override
        public String paramString() {
            return super.paramString();
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

    private JWindow window;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        window = new JWindow();
        TestWindow.initStaticVars();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Constructor for JWindowTest.
     * @param name
     */
    public JWindowTest(final String name) {
        super(name);
    }

    /*
     * Class under test for void JWindow()
     */
    public void testJWindow() {
        window = new JWindow();
        assertTrue("owner is not null", window.getOwner() != null);
        assertFalse("JWindow is invisible by default", window.isVisible());
        assertTrue(window.getLocale() == JComponent.getDefaultLocale());
        assertFalse("window is not focusable", window.isFocusableWindow());
        assertTrue(window.getContentPane().getLayout() instanceof BorderLayout);
    }

    /*
     * Class under test for void windowInit()
     */
    public void testWindowInit() {
        TestWindow window = new TestWindow();
        assertTrue("rootPaneCheckingEnabled is true", window.isRootPaneCheckingEnabled());
        assertTrue("layout is not null", window.getLayout() != null);
        assertTrue("rootPane is not null", window.getRootPane() != null);
        assertTrue("locale is set", window.getLocale() == JComponent.getDefaultLocale());
        assertTrue("rootPane.windowDecorationStyle is NONE", window.getRootPane()
                .getWindowDecorationStyle() == JRootPane.NONE);
        // test that defaultFocusTraversalPolicy is set
        assertTrue("focusTraversalPolicy is set correctly",
                window.getFocusTraversalPolicy() == KeyboardFocusManager
                        .getCurrentKeyboardFocusManager().getDefaultFocusTraversalPolicy());
        assertTrue("focusTraversalPolicy is set", window.isFocusTraversalPolicySet());
        assertTrue(window.isFocusCycleRoot());
        assertFalse(window.isFocusTraversalPolicyProvider());
    }

    /*
     * Class under test for
     *     void setRootPaneCheckingEnabled(boolean enabled)
     *     boolean isRootPaneCheckingEnabled()
     */
    public void testSetIsRootPaneCheckingEnabled() {
        TestWindow window = new TestWindow();
        assertTrue("rootPaneCheckingEnabled is true by default", window
                .isRootPaneCheckingEnabled());
        window.setRootPaneCheckingEnabled(false);
        assertFalse("rootPaneCheckingEnabled is set to false", window
                .isRootPaneCheckingEnabled());
    }

    /*
     * Class under test for void JWindow(Window, GraphicsConfiguration)
     */
    public void testJWindowWindowGraphicsConfiguration() {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        Window owner = new JWindow();
        // test with valid owner and valid gc
        // would be nice to test non-default gc here
        window = new JWindow(owner, gc);
        assertTrue("owner is set", window.getOwner() == owner);
        assertTrue(window.getGraphicsConfiguration() == gc);
        assertFalse("JWindow is invisible by default", window.isVisible());
        assertTrue(window.getLocale() == JComponent.getDefaultLocale());
        assertFalse("window is not focusable", window.isFocusableWindow());
        // test with valid owner and gc == null
        window = new JWindow(owner, (GraphicsConfiguration) null);
        assertTrue("owner is set", window.getOwner() == owner);
        assertTrue(window.getGraphicsConfiguration() == gc);
        assertFalse("JWindow is invisible by default", window.isVisible());
        assertTrue(window.getLocale() == JComponent.getDefaultLocale());
        assertFalse("window is not focusable", window.isFocusableWindow());
        // test with owner == null and valid gc
        window = new JWindow(null, gc);
        assertTrue("owner is not null", window.getOwner() != null);
        assertTrue(window.getGraphicsConfiguration() == gc);
        assertFalse("JWindow is invisible by default", window.isVisible());
        assertTrue(window.getLocale() == JComponent.getDefaultLocale());
        assertFalse("window is not focusable", window.isFocusableWindow());
        // test with owner == null and gc == null
        window = new JWindow(null, null);
        assertTrue("owner is not null", window.getOwner() != null);
        assertTrue(window.getGraphicsConfiguration() == window.getOwner()
                .getGraphicsConfiguration());
        assertFalse("JWindow is invisible by default", window.isVisible());
        assertTrue(window.getLocale() == JComponent.getDefaultLocale());
        assertFalse("window is not focusable", window.isFocusableWindow());
    }

    /*
     * Class under test for void JWindow(Window)
     */
    public void testJWindowWindow() {
        Window owner = new JWindow();
        window = new JWindow(owner);
        // test with the correct owner
        assertTrue("owner is set", window.getOwner() == owner);
        assertFalse("JWindow is invisible by default", window.isVisible());
        assertTrue(window.getLocale() == JComponent.getDefaultLocale());
        assertFalse("window is not focusable", window.isFocusableWindow());
        // test with owner = null
        window = new JWindow((Window) null);
        assertTrue("owner is not null", window.getOwner() != null);
        assertFalse("JWindow is invisible by default", window.isVisible());
        assertTrue(window.getLocale() == JComponent.getDefaultLocale());
        assertFalse("window is not focusable", window.isFocusableWindow());
    }

    /*
     * Class under test for void JWindow(GraphicsConfiguration)
     */
    public void testJWindowGraphicsConfiguration() {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        // test with valid gc
        // would be nice to test non-default gc here
        window = new JWindow(gc);
        assertTrue("owner is not null", window.getOwner() != null);
        assertFalse("JWindow is invisible by default", window.isVisible());
        assertTrue(window.getLocale() == JComponent.getDefaultLocale());
        assertFalse("window is not focusable", window.isFocusableWindow());
        assertTrue(window.getGraphicsConfiguration() == gc);
        // test with gc == null
        window = new JWindow((GraphicsConfiguration) null);
        assertTrue("owner is not null", window.getOwner() != null);
        assertFalse("JWindow is invisible by default", window.isVisible());
        assertTrue(window.getLocale() == JComponent.getDefaultLocale());
        assertFalse("window is not focusable", window.isFocusableWindow());
        assertTrue(window.getGraphicsConfiguration() == gc);
    }

    /*
     * Class under test for void JWindow(Frame)
     */
    public void testJWindowFrame() {
        Frame owner = new Frame();
        window = new JWindow(owner);
        // test with the correct owner
        assertTrue("owner is set", window.getOwner() == owner);
        assertFalse("JWindow is invisible by default", window.isVisible());
        assertTrue(window.getLocale() == JComponent.getDefaultLocale());
        assertFalse("window is not focusable", window.isFocusableWindow());
        // test with owner = null
        window = new JWindow((Frame) null);
        assertTrue("owner is not null", window.getOwner() != null);
        assertFalse("JWindow is invisible by default", window.isVisible());
        assertTrue(window.getLocale() == JComponent.getDefaultLocale());
        assertFalse("window is not focusable", window.isFocusableWindow());
    }

    /*
     * Class under test for void addImpl(Component, Object, int)
     */
    public void testAddImpl() {
        TestWindow window = new TestWindow();
        JComponent comp = new JPanel();
        // rootPaneCheckingEnabled is true, no exception since 1.5
        window.setRootPaneCheckingEnabled(true);
        boolean ok = false;
        try {
            window.addImpl(comp, null, 0);
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception", ok);
            assertTrue("The component is added to contentPane", comp.getParent() == window
                    .getContentPane());
        }
        // rootPaneCheckingEnabled is false, no exception
        window.setRootPaneCheckingEnabled(false);
        ok = false;
        try {
            window.addImpl(comp, null, 0);
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception", ok);
            assertTrue("the component is added to JWindow", comp.getParent() == window);
            assertTrue("index of the component is 0", window.getComponent(0) == comp);
        }
    }

    /*
     * Class under test for
     *     void setRootPane(JRootPane)
     *     JRootPane getRootPane()
     */
    public void testSetGetRootPane() {
        TestWindow window = new TestWindow();
        assertTrue("setRootPane() is called from the constructor", TestWindow.setRootPaneCalled);
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        window.addPropertyChangeListener("rootPane", listener);
        JRootPane root = new JRootPane();
        window.setRootPane(root);
        assertTrue(window.getRootPane() == root);
        assertFalse("rootPane is not a bound property", listener.ok);
        // test setting rootPane to null
        window.setRootPane(null);
        assertNull(window.getRootPane());
        assertTrue("rootPane is removed from the container", window.getComponentCount() == 0);
    }

    /*
     * Class under test for JRootPane createRootPane()
     */
    public void testCreateRootPane() {
        TestWindow frame = new TestWindow();
        assertTrue("createRootPane() is called from the constructor",
                TestWindow.createRootPaneCalled);
        JRootPane root = frame.createRootPane();
        assertTrue("createRootPane() cannot return null", root != null);
    }

    /*
     * Class under test for
     *     void setLayeredPane(JLayeredPane)
     *     JLayeredPane getLayeredPane()
     */
    public void testSetGetLayeredPane() {
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        window.addPropertyChangeListener("layeredPane", listener);
        JLayeredPane pane = new JLayeredPane();
        window.setLayeredPane(pane);
        assertTrue(window.getLayeredPane() == pane);
        assertFalse("layeredPane is not a bound property", listener.ok);
        // test throwing exception if the parameter is null
        boolean ok = false;
        try {
            window.setLayeredPane(null);
        } catch (IllegalComponentStateException e) {
            ok = true;
        } finally {
            assertTrue(ok);
        }
        // layeredPane cannot be null, even after setLayeredPane(null)
        assertTrue(window.getLayeredPane() != null);
        // setLayeredPane() method is not called by the constructor
        // (seems that there is an error in docs)
    }

    /*
     * Class under test for AccessibleContext getAccessibleContext()
     */
    public void testGetAccessibleContext() {
        AccessibleContext c = window.getAccessibleContext();
        assertTrue("instance of AccessibleJWindow", c instanceof JWindow.AccessibleJWindow);
        assertTrue("AccessibleRole is ok", c.getAccessibleRole() == AccessibleRole.WINDOW);
        assertNull("AccessibleName is ok", c.getAccessibleName());
        assertNull("AccessibleDescription is ok", c.getAccessibleDescription());
        assertTrue("AccessibleChildrenCount == 1", c.getAccessibleChildrenCount() == 1);
    }

    /*
     * Class under test for String paramString()
     */
    public void testParamString() {
        TestWindow window = new TestWindow();
        assertTrue("paramString() cannot return null", window.paramString() != null);
    }

    /*
     * Class under test for void setLayout(LayoutManager)
     */
    public void testSetLayout() {
        TestWindow window = new TestWindow();
        LayoutManager contentLayout = window.getContentPane().getLayout();
        LayoutManager frameLayout = window.getLayout();
        // rootPaneCheckingEnabled is true, no exception since 1.5
        window.setRootPaneCheckingEnabled(true);
        boolean ok = false;
        try {
            window.setLayout(new FlowLayout());
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception since 1.5", ok);
            assertTrue("contentPane layout is changed",
                    window.getContentPane().getLayout() != contentLayout);
            assertTrue("Window layout shouldn't be changed", window.getLayout() == frameLayout);
            window.getContentPane().setLayout(contentLayout);
        }
        // rootPaneCheckingEnabled is false
        window.setRootPaneCheckingEnabled(false);
        ok = false;
        try {
            window.setLayout(new FlowLayout());
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception", ok);
            assertTrue("contentPane layout shouldn't be changed", window.getContentPane()
                    .getLayout() == contentLayout);
            assertTrue("Window layout is changed)", window.getLayout() != frameLayout);
        }
    }

    /*
     * Class under test for void update(Graphics)
     */
    public void testUpdate() {
        // Note: painting code, cannot test
    }

    /*
     * Class under test for
     *     void setContentPane(Container)
     *     Container getContentPane()
     */
    public void testSetGetContentPane() {
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        window.addPropertyChangeListener("contentPane", listener);
        JPanel pane = new JPanel();
        window.setContentPane(pane);
        assertTrue(window.getContentPane() == pane);
        assertFalse("contentPane is not a bound property", listener.ok);
        // test throwing exception if the parameter is null
        boolean ok = false;
        try {
            window.setContentPane(null);
        } catch (IllegalComponentStateException e) {
            ok = true;
        } finally {
            assertTrue(ok);
        }
        // contentPane cannot be null, even after setContentPane(null)
        assertTrue(window.getContentPane() != null);
        // setContentPane() method is not called by the constructor
        // (seems that there is an error in docs)
    }

    /*
     * Class under test for
     *     void setGlassPane(Component)
     *     Component getGlassPane()
     */
    public void testSetGetGlassPane() {
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        window.addPropertyChangeListener("glassPane", listener);
        JPanel pane = new JPanel();
        window.setGlassPane(pane);
        assertTrue(window.getGlassPane() == pane);
        assertFalse("glassPane is not a bound property", listener.ok);
        // test throwing exception if the parameter is null
        boolean ok = false;
        try {
            window.setGlassPane(null);
        } catch (NullPointerException e) {
            ok = true;
        } finally {
            assertTrue(ok);
        }
        // glassPane cannot be null, even after setGlassPane(null)
        assertTrue(window.getGlassPane() != null);
        // setGlassPane() method is not called by the constructor
        // (seems that there is an error in docs)
    }

    /*
     * Class under test for void remove(Component)
     */
    public void testRemove() {
        JComponent comp = new JPanel();
        window.getContentPane().add(comp);
        assertTrue("label is in contentPane", window.isAncestorOf(comp));
        window.remove(comp);
        assertFalse("label is removed from contentPane", window.isAncestorOf(comp));
        ((JPanel) window.getGlassPane()).add(comp);
        window.remove(comp);
        assertTrue("label is not removed from glassPane", window.isAncestorOf(comp));
        // test removing directly from the container
        window.setRootPaneCheckingEnabled(false);
        window.add(comp, BorderLayout.EAST);
        assertTrue("added", comp.getParent() == window);
        window.remove(comp);
        assertTrue("not removed", comp.getParent() == window);
        // test removing null
        //        boolean ok = false;
        //        try {
        //            window.remove((Component)null);
        //        } catch (NullPointerException e) {
        //            ok = true;
        //        } finally {
        //            assertTrue("exception", ok);
        //        }
        // test removing rootPane
        assertTrue(window.isAncestorOf(window.getRootPane()));
        window.remove(window.getRootPane());
        // rootPane is removed from the container
        assertFalse(window.isAncestorOf(window.getRootPane()));
        // but getRootPane() still returns it
        assertTrue(window.getRootPane() != null);
    }
}
