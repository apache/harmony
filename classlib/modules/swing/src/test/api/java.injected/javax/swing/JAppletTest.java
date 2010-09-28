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

package javax.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.IllegalComponentStateException;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

import junit.framework.Test;
import junit.framework.TestSuite;

public class JAppletTest extends SwingTestCase {
    /*
     * This class is used to test protected methods
     */
    private static class TestApplet extends JApplet {
        public static boolean createRootPaneCalled = false;
        public static boolean setRootPaneCalled = false;

        public JRootPane createRootPane() {
            createRootPaneCalled = true;
            return super.createRootPane();
        }

        public void setRootPane(final JRootPane root) {
            setRootPaneCalled = true;
            super.setRootPane(root);
        }

        public void setRootPaneCheckingEnabled(final boolean enabled) {
            super.setRootPaneCheckingEnabled(enabled);
        }

        public boolean isRootPaneCheckingEnabled() {
            return super.isRootPaneCheckingEnabled();
        }

        public void addImpl(final Component comp, final Object constraints, final int index) {
            super.addImpl(comp, constraints, index);
        }

        public static void initStaticVars() {
            createRootPaneCalled = false;
            setRootPaneCalled = false;
        }

        public String paramString() {
            return super.paramString();
        }
    }

    private JApplet applet;

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

    public static Test suite() {
        TestSuite suite = new TestSuite(JAppletTest.class);
        return suite;
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        applet = new JApplet();
        TestApplet.initStaticVars();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Constructor for JAppletTest.
     * @param name
     */
    public JAppletTest(final String name) {
        super(name);
    }

    /*
     * Class under test for void JApplet()
     */
    public void testJApplet() {
        TestApplet applet = new TestApplet();

        assertTrue("rootPaneCheckingEnabled is true", applet.isRootPaneCheckingEnabled());

        assertTrue("layout is not null", applet.getLayout() != null);
        assertTrue("layout is BorderLayout", applet.getLayout() instanceof BorderLayout);

        assertTrue("rootPane is not null", applet.getRootPane() != null);

        assertTrue("locale is set", applet.getLocale() == JComponent.getDefaultLocale());

        assertTrue("background is set", applet.isBackgroundSet());
        assertTrue("background is set to white", applet.getBackground() == java.awt.Color.white);

        assertTrue("rootPane.windowDecorationStyle is NONE",
                applet.getRootPane().getWindowDecorationStyle() == JRootPane.NONE);

        // test that defaultFocusTraversalPolicy is set
        assertTrue("focusTraversalPolicy is set",
                   applet.isFocusTraversalPolicySet());
        assertTrue("focusTraversalPolicy is set correctly",
                applet.getFocusTraversalPolicy() == KeyboardFocusManager.
                    getCurrentKeyboardFocusManager().getDefaultFocusTraversalPolicy());
        assertFalse(applet.isFocusCycleRoot());
        assertTrue(applet.isFocusTraversalPolicyProvider());
    }

    /*
     * Class under test for
     *     void setRootPaneCheckingEnabled(boolean enabled)
     *     boolean isRootPaneCheckingEnabled()
     */
    public void testSetIsRootPaneCheckingEnabled() {
        TestApplet applet = new TestApplet();

        assertTrue("rootPaneCheckingEnabled is true by default", applet.isRootPaneCheckingEnabled());

        applet.setRootPaneCheckingEnabled(false);
        assertFalse("rootPaneCheckingEnabled is set to false", applet.isRootPaneCheckingEnabled());
    }

    /*
     * Class under test for void addImpl(Component, Object, int)
     */
    public void testAddImpl() {
        TestApplet applet = new TestApplet();
        JComponent comp = new JPanel();

        // rootPaneCheckingEnabled is true, no exception since 1.5
        applet.setRootPaneCheckingEnabled(true);
        boolean ok = false;
        try {
            applet.addImpl(comp, null, 0);
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception", ok);
            assertTrue("The component is added to contentPane",
                       comp.getParent() == applet.getContentPane());
        }

        // rootPaneCheckingEnabled is false, no exception
        applet.setRootPaneCheckingEnabled(false);
        ok = false;
        try {
            applet.addImpl(comp, null, 0);
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception", ok);
            assertTrue("the component is added to JWindow",
                       comp.getParent() == applet);
            assertTrue("index of the component is 0",
                       applet.getComponent(0) == comp);
        }
    }

    /*
     * Class under test for
     *     void setRootPane(JRootPane)
     *     JRootPane getRootPane()
     */
    public void testSetGetRootPane() {
        TestApplet applet = new TestApplet();
        assertTrue("setRootPane() is called from the constructor",
                TestApplet.setRootPaneCalled);

        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        applet.addPropertyChangeListener("rootPane", listener);
        JRootPane root = new JRootPane();
        applet.setRootPane(root);
        assertTrue(applet.getRootPane() == root);
        assertFalse("rootPane is not a bound property", listener.ok);

        // test setting rootPane to null
        applet.setRootPane(null);
        assertNull(applet.getRootPane());
        assertTrue("rootPane is removed from the container", applet.getComponentCount() == 0);
    }

    /*
     * Class under test for JRootPane createRootPane()
     */
    public void testCreateRootPane() {
        TestApplet applet = new TestApplet();
        assertTrue("createRootPane() is called from the constructor",
                TestApplet.createRootPaneCalled);

        JRootPane root = applet.createRootPane();
        assertTrue("createRootPane() cannot return null", root != null);
    }

    /*
     * Class under test for
     *     void setJMenuBar(JMenuBar)
     *     JMenuBar getJMenuBar()
     */
    public void testSetGetJMenuBarJMenuBar() {
        assertNull(applet.getJMenuBar());

        JMenuBar menuBar = new JMenuBar();
        applet.setJMenuBar(menuBar);
        assertTrue(applet.getJMenuBar() == menuBar);

        applet.setJMenuBar(null);
        assertNull(applet.getJMenuBar());
    }

    /*
     * Class under test for
     *     void setLayeredPane(JLayeredPane)
     *     JLayeredPane getLayeredPane()
     */
    public void testSetGetLayeredPane() {
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        applet.addPropertyChangeListener("layeredPane", listener);

        JLayeredPane pane = new JLayeredPane();
        applet.setLayeredPane(pane);
        assertTrue(applet.getLayeredPane() == pane);
        assertFalse("layeredPane is not a bound property", listener.ok);

        // test throwing exception if the parameter is null
        boolean ok = false;
        try {
            applet.setLayeredPane(null);
        } catch (IllegalComponentStateException e) {
            ok = true;
        } finally {
            assertTrue(ok);
        }
        // layeredPane cannot be null, even after setLayeredPane(null)
        assertTrue(applet.getLayeredPane() != null);

        // setLayeredPane() method is not called by the constructor
        // (seems that there is an error in docs)
    }

    /*
     * Class under test for AccessibleContext getAccessibleContext()
     */
    public void testGetAccessibleContext() {
        AccessibleContext c = applet.getAccessibleContext();

        assertTrue("instance of AccessibleJApplet",
                   c instanceof JApplet.AccessibleJApplet);
        assertTrue("AccessibleRole is ok",
                c.getAccessibleRole() == AccessibleRole.FRAME);
        assertNull("AccessibleName is ok", c.getAccessibleName());
        assertNull("AccessibleDescription is ok",
                c.getAccessibleDescription());
        assertTrue("AccessibleChildrenCount == 1",
                   c.getAccessibleChildrenCount() == 1);
    }

    /*
     * Class under test for String paramString()
     */
    public void testParamString() {
        TestApplet applet = new TestApplet();
        assertTrue("paramString() cannot return null", applet.paramString() != null);
    }

    /*
     * Class under test for void setLayout(LayoutManager)
     */
    public void testSetLayout() {
        TestApplet applet = new TestApplet();
        LayoutManager contentLayout = applet.getContentPane().getLayout();
        LayoutManager appletLayout = applet.getLayout();

        // rootPaneCheckingEnabled is true, no exception since 1.5
        applet.setRootPaneCheckingEnabled(true);
        boolean ok = false;
        try {
            applet.setLayout(new FlowLayout());
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception since 1.5", ok);
            assertTrue("contentPane layout is changed",
                       applet.getContentPane().getLayout() != contentLayout);
            assertTrue("Applet layout shouldn't be changed",
                       applet.getLayout() == appletLayout);
            applet.getContentPane().setLayout(contentLayout);
        }

        // rootPaneCheckingEnabled is false, exception may not be thrown
        applet.setRootPaneCheckingEnabled(false);
        ok = false;
        try {
            applet.setLayout(new FlowLayout());
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception", ok);
            assertTrue("contentPane layout shouldn't be changed",
                       applet.getContentPane().getLayout() == contentLayout);
            assertTrue("Applet layout is changed",
                       applet.getLayout() != appletLayout);
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
        applet.addPropertyChangeListener("contentPane", listener);

        JPanel pane = new JPanel();
        applet.setContentPane(pane);
        assertTrue(applet.getContentPane() == pane);
        assertFalse("contentPane is not a bound property", listener.ok);

        // test throwing exception if the parameter is null
        boolean ok = false;
        try {
            applet.setContentPane(null);
        } catch (IllegalComponentStateException e) {
            ok = true;
        } finally {
            assertTrue(ok);
        }
        // contentPane cannot be null, even after setContentPane(null)
        assertTrue(applet.getContentPane() != null);

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
        applet.addPropertyChangeListener("glassPane", listener);

        JPanel pane = new JPanel();
        applet.setGlassPane(pane);
        assertTrue(applet.getGlassPane() == pane);
        assertFalse("glassPane is not a bound property", listener.ok);

        // test throwing exception if the parameter is null
        boolean ok = false;
        try {
            applet.setGlassPane(null);
        } catch (NullPointerException e) {
            ok = true;
        } finally {
            assertTrue(ok);
        }
        // glassPane cannot be null, even after setGlassPane(null)
        assertTrue(applet.getGlassPane() != null);

        // setGlassPane() method is not called by the constructor
        // (seems that there is an error in docs)
    }

    /*
     * Class under test for void remove(Component)
     */
    public void testRemove() {
        JComponent comp = new JPanel();
        applet.getContentPane().add(comp);
        assertTrue("label is in contentPane", applet.isAncestorOf(comp));
        applet.remove(comp);
        assertFalse("label is removed from contentPane", applet.isAncestorOf(comp));

        ((JPanel)applet.getGlassPane()).add(comp);
        applet.remove(comp);
        assertTrue("label is not removed from glassPane", applet.isAncestorOf(comp));

        // test removing directly from the container
        applet.setRootPaneCheckingEnabled(false);
        applet.add(comp, BorderLayout.EAST);
        assertTrue("added", comp.getParent() == applet);
        applet.remove(comp);
        assertTrue("not removed", comp.getParent() == applet);

        // test removing rootPane
        assertTrue(applet.isAncestorOf(applet.getRootPane()));
        applet.remove(applet.getRootPane());
        // rootPane is removed from the container
        assertFalse(applet.isAncestorOf(applet.getRootPane()));
        // but getRootPane() still returns it
        assertTrue(applet.getRootPane() != null);
    }
}
