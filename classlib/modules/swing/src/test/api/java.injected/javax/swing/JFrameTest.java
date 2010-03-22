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
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.security.Permission;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import org.apache.harmony.x.swing.StringConstants;

public class JFrameTest extends SwingTestCase {
    /*
     * This class is used to test that some methods were called.
     */
    private static class TestFrame extends JFrame {
        private static final long serialVersionUID = 1L;

        public static boolean createRootPaneCalled = false;

        public static boolean setRootPaneCalled = false;

        public boolean disposeCalled = false;

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

        public static void initStaticVars() {
            createRootPaneCalled = false;
            setRootPaneCalled = false;
        }

        @Override
        public void dispose() {
            disposeCalled = true;
            super.dispose();
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

    private JFrame frame;

    public JFrameTest(final String name) {
        super(name);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JFrame();
        TestFrame.initStaticVars();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (frame.isDisplayable()) {
            frame.dispose();
        }
    }

    /*
     * Class under test for void JFrame()
     */
    public void testJFrame() {
        frame = new JFrame();
        assertEquals("title is empty", "", frame.getTitle());
        assertFalse("JFrame is invisible by default", frame.isVisible());
        assertTrue(frame.getLocale() == JComponent.getDefaultLocale());
        // how to test throwing of HeadlessException
        // when GraphicsEnvironment.isHeadless() returns true
        // it is not critical because the exception is actually thrown by Frame() constructor
    }

    public void testFrameInit() {
        TestFrame frame = new TestFrame();
        assertTrue("rootPaneCheckingEnabled is true", frame.isRootPaneCheckingEnabled());
        assertTrue("layout is not null", frame.getLayout() != null);
        assertTrue("rootPane is not null", frame.getRootPane() != null);
        assertTrue("locale is set", frame.getLocale() == JComponent.getDefaultLocale());
        assertTrue("background is set", frame.getBackground() == frame.getContentPane()
                .getBackground());
        assertFalse("defaultLookAndFeelDecorated is false", JFrame
                .isDefaultLookAndFeelDecorated());
        assertFalse("isUndecorated is false", frame.isUndecorated());
        assertTrue("rootPane.windowDecorationStyle is NONE", frame.getRootPane()
                .getWindowDecorationStyle() == JRootPane.NONE);
        // test that defaultFocusTraversalPolicy is set
        //frame.setFocusTraversalPolicy(null);
        //frame.frameInit();
        assertTrue("focusTraversalPolicy is set correctly",
                frame.getFocusTraversalPolicy() == KeyboardFocusManager
                        .getCurrentKeyboardFocusManager().getDefaultFocusTraversalPolicy());
        assertTrue("focusTraversalPolicy is set", frame.isFocusTraversalPolicySet());
        assertTrue(frame.isFocusCycleRoot());
        assertFalse(frame.isFocusTraversalPolicyProvider());
        JFrame.setDefaultLookAndFeelDecorated(true);
        frame.frameInit();
        assertTrue("isUndecorated is true", frame.isUndecorated());
        assertTrue("rootPane.windowDecorationStyle is FRAME", frame.getRootPane()
                .getWindowDecorationStyle() == JRootPane.FRAME);
        // restore default value
        JFrame.setDefaultLookAndFeelDecorated(false);
    }

    /*
     * Class under test for
     *     void setDefaultCloseOperation(int operation)
     *     int getDefaultCloseOperation()
     */
    public void testSetGetDefaultCloseOperation() {
        // default value is JFrame.HIDE_ON_CLOSE
        assertEquals(WindowConstants.HIDE_ON_CLOSE, frame.getDefaultCloseOperation());
        // test setting valid value
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        frame.addPropertyChangeListener("defaultCloseOperation", listener);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        assertEquals(WindowConstants.DISPOSE_ON_CLOSE, frame.getDefaultCloseOperation());
        assertTrue("defaultCloseOperation is a bound property", listener.ok);
        // test setting invalid value
        boolean ok = false;
        try {
            frame.setDefaultCloseOperation(101); // invalid value
        } catch (IllegalArgumentException e) {
            ok = true;
        } finally {
            assertTrue(ok);
        }
        // if JFrame.EXIT_ON_CLOSE has been specified and the SecurityManager
        // will not allow the caller to invoke System.exit then SecurityException is thrown
        class MySecurityManager extends SecurityManager {
            @Override
            public void checkExit(final int status) {
                // exit is not allowed
                throw new SecurityException();
            }

            @Override
            public void checkPermission(final Permission perm) {
                // allow changing the security manager
            }
        }
        MySecurityManager sm = new MySecurityManager();
        SecurityManager oldSM = System.getSecurityManager();
        System.setSecurityManager(sm);
        ok = false;
        try {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        } catch (SecurityException e) {
            ok = true;
        } finally {
            assertTrue("", ok);
            System.setSecurityManager(oldSM);
        }
    }

    /*
     * Class under test for
     *     static void setDefaultLookAndFeelDecorated(boolean defaultLookAndFeelDecorated)
     *     static boolean isDefaultLookAndFeelDecorated()
     */
    public void testSetIsDefaultLookAndFeelDecorated() {
        // test for default value
        assertFalse(JFrame.isDefaultLookAndFeelDecorated());
        JFrame.setDefaultLookAndFeelDecorated(true);
        assertTrue(JFrame.isDefaultLookAndFeelDecorated());
        // restore default value
        JFrame.setDefaultLookAndFeelDecorated(false);
    }

    /*
     * Class under test for
     *     void setRootPaneCheckingEnabled(boolean enabled)
     *     boolean isRootPaneCheckingEnabled()
     */
    public void testSetIsRootPaneCheckingEnabled() {
        TestFrame frame = new TestFrame();
        assertTrue("rootPaneCheckingEnabled is true by default", frame
                .isRootPaneCheckingEnabled());
        frame.setRootPaneCheckingEnabled(false);
        assertFalse("rootPaneCheckingEnabled is set to false", frame
                .isRootPaneCheckingEnabled());
    }

    /*
     * Class under test for void JFrame(String, GraphicsConfiguration)
     */
    public void testJFrameStringGraphicsConfiguration() {
        final String title = "Test frame.";
        final GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        // test with valid title, valid gc
        // would be nice to test non-default gc here
        frame = new JFrame(title, gc);
        assertEquals("Title is set properly", title, frame.getTitle());
        assertFalse("JFrame is invisible by default", frame.isVisible());
        assertTrue(frame.getLocale() == JComponent.getDefaultLocale());
        assertTrue(frame.getGraphicsConfiguration() == gc);
        frame = new JFrame(null, null);
        assertNull("null instead of title can be used", frame.getTitle());
        assertFalse("JFrame is invisible by default", frame.isVisible());
        assertTrue(frame.getLocale() == JComponent.getDefaultLocale());
        assertTrue(frame.getGraphicsConfiguration() == gc);
        // how to test throwing of HeadlessException
        // when GraphicsEnvironment.isHeadless() returns true
        // it is not critical because the exception is actually thrown by Frame() constructor
    }

    /*
     * Class under test for void JFrame(String)
     */
    public void testJFrameString() {
        final String title = "Test frame.";
        // test with valid title
        frame = new JFrame(title);
        assertEquals("Title is set properly", title, frame.getTitle());
        assertFalse("JFrame is invisible by default", frame.isVisible());
        assertTrue(frame.getLocale() == JComponent.getDefaultLocale());
        frame = new JFrame((String) null);
        assertNull("null instead of title can be used", frame.getTitle());
        assertFalse("JFrame is invisible by default", frame.isVisible());
        assertTrue(frame.getLocale() == JComponent.getDefaultLocale());
        // how to test throwing of HeadlessException
        // when GraphicsEnvironment.isHeadless() returns true
        // it is not critical because the exception is actually thrown by Frame() constructor
    }

    /*
     * Class under test for void JFrame(GraphicsConfiguration)
     */
    public void testJFrameGraphicsConfiguration() {
        final GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        // test with valid gc
        // would be nice to test non-default gc here
        frame = new JFrame(gc);
        assertEquals("title is empty", "", frame.getTitle());
        assertFalse("JFrame is invisible by default", frame.isVisible());
        assertTrue(frame.getLocale() == JComponent.getDefaultLocale());
        assertTrue(frame.getGraphicsConfiguration() == gc);
        frame = new JFrame((GraphicsConfiguration) null);
        assertEquals("title is empty", "", frame.getTitle());
        assertFalse("JFrame is invisible by default", frame.isVisible());
        assertTrue(frame.getLocale() == JComponent.getDefaultLocale());
        assertTrue(frame.getGraphicsConfiguration() == gc);
        // how to test throwing of HeadlessException
        // when GraphicsEnvironment.isHeadless() returns true
        // it is not critical because the exception is actually thrown by Frame() constructor
    }

    /*
     * Class under test for void addImpl(Component, Object, int)
     */
    public void testAddImpl() {
        JComponent comp = new JPanel();
        // rootPaneCheckingEnabled is true, exception must be thrown
        frame.setRootPaneCheckingEnabled(true);
        boolean ok = false;
        try {
            frame.addImpl(comp, null, 0);
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception", ok);
            assertTrue("The component is added to contentPane", comp.getParent() == frame
                    .getContentPane());
        }
        // rootPaneCheckingEnabled is false, exception may not be thrown
        frame.setRootPaneCheckingEnabled(false);
        ok = false;
        try {
            frame.addImpl(comp, null, 0);
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception", ok);
            assertTrue("the component is added to JWindow", comp.getParent() == frame);
            assertTrue("index of the component is 0", frame.getComponent(0) == comp);
        }
    }

    /*
     * Class under test for
     *     void setRootPane(JRootPane)
     *     JRootPane getRootPane()
     */
    public void testSetGetRootPane() {
        TestFrame frame = new TestFrame();
        assertTrue("setRootPane() is called from the constructor", TestFrame.setRootPaneCalled);
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        frame.addPropertyChangeListener("rootPane", listener);
        JRootPane root = new JRootPane();
        frame.setRootPane(root);
        assertTrue(frame.getRootPane() == root);
        assertFalse("rootPane is not a bound property", listener.ok);
        // test setting rootPane to null
        frame.setRootPane(null);
        assertNull(frame.getRootPane());
        assertTrue("rootPane is removed from the container", frame.getComponentCount() == 0);
    }

    /*
     * Class under test for JRootPane createRootPane()
     */
    public void testCreateRootPane() {
        TestFrame frame = new TestFrame();
        assertTrue("createRootPane() is called from the constructor",
                TestFrame.createRootPaneCalled);
        JRootPane root = frame.createRootPane();
        assertTrue("createRootPane() cannot return null", root != null);
    }

    /*
     * Class under test for
     *     void setJMenuBar(JMenuBar)
     *     JMenuBar getJMenuBar()
     */
    public void testSetGetJMenuBar() {
        assertNull(frame.getJMenuBar());
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        assertTrue(frame.getJMenuBar() == menuBar);
        frame.setJMenuBar(null);
        assertNull(frame.getJMenuBar());
    }

    /*
     * Class under test for
     *     void setLayeredPane(JLayeredPane)
     *     JLayeredPane getLayeredPane()
     */
    public void testSetGetLayeredPane() {
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        frame.addPropertyChangeListener("layeredPane", listener);
        JLayeredPane pane = new JLayeredPane();
        frame.setLayeredPane(pane);
        assertTrue(frame.getLayeredPane() == pane);
        assertFalse("layeredPane is not a bound property", listener.ok);
        // test throwing exception if the parameter is null
        boolean ok = false;
        try {
            frame.setLayeredPane(null);
        } catch (IllegalComponentStateException e) {
            ok = true;
        } finally {
            assertTrue(ok);
        }
        // layeredPane cannot be null, even after setLayeredPane(null)
        assertTrue(frame.getLayeredPane() != null);
        // setLayeredPane() method is not called by the constructor
        // (seems that there is an error in docs)
    }

    /*
     * Class under test for AccessibleContext getAccessibleContext()
     */
    public void testGetAccessibleContext() {
        AccessibleContext c = frame.getAccessibleContext();
        assertTrue("class is ok", c instanceof JFrame.AccessibleJFrame);
        assertTrue("AccessibleRole is ok", c.getAccessibleRole() == AccessibleRole.FRAME);
        assertNull("AccessibleDescription is ok", c.getAccessibleDescription());
        assertTrue("AccessibleChildrenCount == 1", c.getAccessibleChildrenCount() == 1);
        // test getAccessibleName()
        assertTrue("AccessibleName is ok", c.getAccessibleName() == "");
        frame.setTitle("aa");
        assertTrue("AccessibleName is ok", c.getAccessibleName() == "aa");
        // test getAccessibleStateSet()
        AccessibleState[] states = c.getAccessibleStateSet().toArray();
        assertTrue("more than 2 states", states.length > 2);
        frame.setVisible(true);
        states = c.getAccessibleStateSet().toArray();
        assertTrue("more than 4 states", states.length > 4);
    }

    /*
     * Class under test for String paramString()
     */
    public void testParamString() {
        TestFrame frame = new TestFrame();
        assertTrue("paramString() cannot return null", frame.paramString() != null);
    }

    /*
     * Class under test for void processWindowEvent(WindowEvent)
     */
    public void testProcessWindowEvent() {
        TestFrame frame = new TestFrame();
        frame.setVisible(true);
        WindowEvent e = new WindowEvent(frame, WindowEvent.WINDOW_CLOSING);
        // test DO_NOTHING_ON_CLOSE
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.disposeCalled = false;
        frame.processWindowEvent(e);
        assertFalse("didn't call dispose()", frame.disposeCalled);
        assertTrue("is visible", frame.isVisible());
        // test HIDE_ON_CLOSE
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        frame.disposeCalled = false;
        frame.processWindowEvent(e);
        assertFalse("didn't call dispose()", frame.disposeCalled);
        assertFalse("is not visible", frame.isVisible());
        // test DISPOSE_ON_CLOSE
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.disposeCalled = false;
        frame.setVisible(true);
        frame.processWindowEvent(e);
        assertTrue("called dispose()", frame.disposeCalled);
        assertFalse("is not visible", frame.isVisible());
        // could test EXIT_ON_CLOSE but it's rather hard
    }

    /*
     * Class under test for void setLayout(LayoutManager)
     */
    public void testSetLayout() {
        TestFrame frame = new TestFrame();
        LayoutManager contentLayout = frame.getContentPane().getLayout();
        LayoutManager frameLayout = frame.getLayout();
        // rootPaneCheckingEnabled is true, no exception since 1.5
        frame.setRootPaneCheckingEnabled(true);
        boolean ok = false;
        try {
            frame.setLayout(new FlowLayout());
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception since 1.5", ok);
            assertTrue("contentPane layout is changed",
                    frame.getContentPane().getLayout() != contentLayout);
            assertTrue("Frame layout shouldn't be changed", frame.getLayout() == frameLayout);
            frame.getContentPane().setLayout(contentLayout);
        }
        // rootPaneCheckingEnabled is false
        frame.setRootPaneCheckingEnabled(false);
        ok = false;
        try {
            frame.setLayout(new FlowLayout());
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception", ok);
            assertTrue("contentPane layout shouldn't be changed", frame.getContentPane()
                    .getLayout() == contentLayout);
            assertTrue("Frame layout is changed", frame.getLayout() != frameLayout);
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
        frame.addPropertyChangeListener("contentPane", listener);
        JPanel pane = new JPanel();
        frame.setContentPane(pane);
        assertTrue(frame.getContentPane() == pane);
        assertFalse("contentPane is not a bound property", listener.ok);
        // test throwing exception if the parameter is null
        boolean ok = false;
        try {
            frame.setContentPane(null);
        } catch (IllegalComponentStateException e) {
            ok = true;
        } finally {
            assertTrue(ok);
        }
        // contentPane cannot be null, even after setContentPane(null)
        assertTrue(frame.getContentPane() != null);
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
        frame.addPropertyChangeListener("glassPane", listener);
        JPanel pane = new JPanel();
        frame.setGlassPane(pane);
        assertTrue(frame.getGlassPane() == pane);
        assertFalse("glassPane is not a bound property", listener.ok);
        // test throwing exception if the parameter is null
        boolean ok = false;
        try {
            frame.setGlassPane(null);
        } catch (NullPointerException e) {
            ok = true;
        } finally {
            assertTrue(ok);
        }
        // glassPane cannot be null, even after setGlassPane(null)
        assertTrue(frame.getGlassPane() != null);
        // setGlassPane() method is not called by the constructor
        // (seems that there is an error in docs)
    }

    /*
     * Class under test for void remove(Component)
     */
    public void testRemove() {
        TestFrame frame = new TestFrame();
        JComponent comp = new JPanel();
        frame.getContentPane().add(comp);
        assertTrue("label is in contentPane", frame.isAncestorOf(comp));
        frame.remove(comp);
        assertFalse("label is removed from contentPane", frame.isAncestorOf(comp));
        ((JPanel) frame.getGlassPane()).add(comp);
        frame.remove(comp);
        assertTrue("label is not removed from glassPane", frame.isAncestorOf(comp));
        // test removing from JFrame
        frame.setRootPaneCheckingEnabled(false);
        frame.add(comp, BorderLayout.EAST);
        assertTrue("added", comp.getParent() == frame);
        frame.remove(comp);
        assertTrue("not removed", comp.getParent() == frame);
        // test removing null
        //        boolean ok = false;
        //        try {
        //            frame.remove((Component)null);
        //        } catch (NullPointerException e) {
        //            ok = true;
        //        } finally {
        //            assertTrue("exception", ok);
        //        }
        // test removing rootPane
        assertTrue(frame.isAncestorOf(frame.getRootPane()));
        frame.remove(frame.getRootPane());
        // rootPane is removed from the container
        assertFalse(frame.isAncestorOf(frame.getRootPane()));
        // but getRootPane() still returns it
        assertTrue(frame.getRootPane() != null);
    }

    /*
     * Class under test for void setIconImage(Image image)
     */
    public void testSetIconImage() {
        Image image = new BufferedImage(5, 5, BufferedImage.TYPE_BYTE_INDEXED);
        PropertyChangeController cont = new PropertyChangeController();
        frame.addPropertyChangeListener(cont);
        frame.setIconImage(image);
        assertEquals(image, frame.getIconImage());
        assertTrue(cont.isChanged(StringConstants.ICON_IMAGE_PROPERTY));
    }
}
