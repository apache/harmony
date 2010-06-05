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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.IllegalComponentStateException;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicInternalFrameUI;

@SuppressWarnings("serial")
public class JInternalFrameTest extends SwingTestCase {
    /*
     * This class is used to test that some methods were called.
     */
    static private class TestInternalFrame extends JInternalFrame {
        public static boolean createRootPaneCalled = false;

        public static boolean setRootPaneCalled = false;

        public static boolean disposeCalled = false;

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
            disposeCalled = false;
        }

        @Override
        public void dispose() {
            disposeCalled = true;
            super.dispose();
        }
    }

    /*
     * This class is used to test that some property is a constrained property
     */
    private class MyVetoableChangeListener implements VetoableChangeListener {
        protected String name;

        MyVetoableChangeListener(final String name) {
            this.name = name;
        }

        public void vetoableChange(final PropertyChangeEvent evt) throws PropertyVetoException {
            if (evt.getPropertyName() == name) {
                throw new PropertyVetoException("", evt);
            }
        }
    }

    private class MyInternalFrameListener implements InternalFrameListener {
        public int state;

        private static final int opened = 1;

        private static final int closing = 2;

        private static final int closed = 4;

        private static final int iconified = 8;

        private static final int deiconified = 16;

        private static final int activated = 32;

        private static final int deactivated = 64;

        MyInternalFrameListener() {
            state = 0;
        }

        public void internalFrameOpened(final InternalFrameEvent e) {
            state |= opened;
        }

        public void internalFrameClosing(final InternalFrameEvent e) {
            state |= closing;
        }

        public void internalFrameClosed(final InternalFrameEvent e) {
            state |= closed;
        }

        public void internalFrameIconified(final InternalFrameEvent e) {
            state |= iconified;
        }

        public void internalFrameDeiconified(final InternalFrameEvent e) {
            state |= deiconified;
        }

        public void internalFrameActivated(final InternalFrameEvent e) {
            state |= activated;
        }

        public void internalFrameDeactivated(final InternalFrameEvent e) {
            state |= deactivated;
        }

        public boolean openedFired() {
            return (state & opened) != 0;
        }

        public boolean closingFired() {
            return (state & closing) != 0;
        }

        public boolean closedFired() {
            return (state & closed) != 0;
        }

        public boolean iconifiedFired() {
            return (state & iconified) != 0;
        }

        public boolean deiconifiedFired() {
            return (state & deiconified) != 0;
        }

        public boolean activatedFired() {
            return (state & activated) != 0;
        }

        public boolean deactivatedFired() {
            return (state & deactivated) != 0;
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

    private JInternalFrame frame;

    // is used in tests where frame.isShowing() must be true
    private JFrame rootFrame;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JInternalFrame();
        TestInternalFrame.initStaticVars();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        if (rootFrame != null) {
            rootFrame.dispose();
            rootFrame = null;
        }
        super.tearDown();
    }

    /**
     * Constructor for JInternalFrameTest.
     * @param name
     */
    public JInternalFrameTest(final String name) {
        super(name);
    }

    /*
     * Class under test for void reshape(int, int, int, int).
     * Actually, reshape() seems to be obsolete, we'll test setBounds()
     * instead.
     */
    public void testReshape() {
        final Point location = new Point(20, 21);
        final int width = 22;
        final int height = 23;
        frame.setBounds(location.x, location.y, width, height);
        assertTrue("location is set", frame.getLocation().equals(location));
        assertTrue("width is set", frame.getWidth() == width);
        assertTrue("height is set", frame.getHeight() == height);
        // Note: could test that the component was re-layouted
    }

    /*
     * Class under test for void updateUI()
     */
    public void testUpdateUI() {
        frame.updateUI();
        ComponentUI ui1 = frame.getUI();
        ComponentUI ui2 = UIManager.getUI(frame);
        // at least names of classes must be the same
        assertEquals(ui2.getClass().getName(), ui1.getClass().getName());
    }

    /*
     * Checks correctness of the internal frame after constructor.
     */
    protected void checkJInternalFrameCorrectness(final String title, final boolean resizable,
            final boolean closable, final boolean maximizable, final boolean iconifiable) {
        assertTrue("title is set", frame.getTitle() == title);
        assertTrue("resizable is set", frame.isResizable() == resizable);
        assertTrue("closable is set", frame.isClosable() == closable);
        assertTrue("maximizable is set", frame.isMaximizable() == maximizable);
        assertTrue("iconifiable is set", frame.isIconifiable() == iconifiable);
        assertFalse("is visible by default", frame.isVisible());
        assertTrue("rootPane != null", frame.getRootPane() != null);
        assertTrue("locale is set", frame.getLocale() == JComponent.getDefaultLocale());
        assertTrue("", frame.getRootPane().getWindowDecorationStyle() == JRootPane.NONE);
        assertTrue("", frame.getBackground() == frame.getContentPane().getBackground());
        assertTrue("ui != null", frame.getUI() != null);
        assertTrue("is focus cycle root", frame.isFocusCycleRoot());
        assertTrue("glassPane is visible", frame.getGlassPane().isVisible());
        // test that defaultFocusTraversalPolicy is set
        assertTrue("focusTraversalPolicy is set", frame.isFocusTraversalPolicySet());
        assertTrue("focusTraversalPolicy is set correctly",
                frame.getFocusTraversalPolicy() == KeyboardFocusManager
                        .getCurrentKeyboardFocusManager().getDefaultFocusTraversalPolicy());
        assertTrue(frame.isFocusCycleRoot());
        assertFalse(frame.isFocusTraversalPolicyProvider());
    }

    /*
     * Class under test for void JInternalFrame()
     */
    public void testJInternalFrame() {
        frame = new JInternalFrame();
        checkJInternalFrameCorrectness("", false, false, false, false);
    }

    /*
     * Class under test for void doDefaultCloseAction()
     */
    public void testDoDefaultCloseAction() {
        TestInternalFrame frame = new TestInternalFrame();
        MyInternalFrameListener listener = new MyInternalFrameListener();
        frame.addInternalFrameListener(listener);
        frame.setVisible(true);
        // test DO_NOTHING_ON_CLOSE operation
        TestInternalFrame.initStaticVars();
        listener.state = 0;
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.doDefaultCloseAction();
        assertTrue("INTERNAL_FRAME_CLOSING was fired", listener.closingFired());
        assertFalse("dispose() was not called", TestInternalFrame.disposeCalled);
        assertTrue("setVisible(false) was not called", frame.isVisible());
        // test DISPOSE_ON_CLOSE operation
        TestInternalFrame.initStaticVars();
        listener.state = 0;
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.doDefaultCloseAction();
        assertTrue("dispose() was called", TestInternalFrame.disposeCalled);
        assertTrue("INTERNAL_FRAME_CLOSING was fired", listener.closingFired());
        // test HIDE_ON_CLOSE operation
        TestInternalFrame.initStaticVars();
        listener.state = 0;
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        frame.doDefaultCloseAction();
        assertFalse("dispose() was not called", TestInternalFrame.disposeCalled);
        assertFalse("setVisible(false) was called", frame.isVisible());
        assertTrue("INTERNAL_FRAME_CLOSING was fired", listener.closingFired());
    }

    /*
     * Class under test for void pack()
     */
    public void testPack() {
        final JComponent comp1 = new JPanel();
        final JComponent comp2 = new JPanel();
        comp2.setPreferredSize(new Dimension(60, 20));
        final Component comp3 = new JPanel();
        frame.getContentPane().add(comp1, BorderLayout.NORTH);
        frame.getContentPane().add(comp2, BorderLayout.SOUTH);
        frame.getContentPane().add(comp3, BorderLayout.CENTER);
        createAndShowRootFrame();
        frame.pack();
        assertTrue("size is set to preferred size", frame.getRootPane().getSize().equals(
                frame.getRootPane().getPreferredSize()));
    }

    /*
     * Class under test for void moveToBack()
     */
    public void testMoveToBack() {
        // test without JDesktopPane set
        frame.moveToBack();
        // test with JDesktopPane set
        JDesktopPane desktop = new JDesktopPane();
        desktop.add(frame);
        desktop.add(new JInternalFrame());
        desktop.add(new JInternalFrame());
        assertTrue("frame index is 0", desktop.getIndexOf(frame) == 0);
        frame.moveToBack();
        assertTrue("frame index is 2", desktop.getIndexOf(frame) == 2);
        // test with different layer
        frame.setLayer(1);
        assertTrue("frame index is 0", desktop.getIndexOf(frame) == 0);
        frame.moveToBack();
        assertTrue("frame index is 0", desktop.getIndexOf(frame) == 0);
    }

    /*
     * Class under test for void moveToFront()
     */
    public void testMoveToFront() {
        // test without JDesktopPane set
        frame.moveToFront();
        // test with JDesktopPane set
        //JDesktopPane desktop = new JDesktopPane();
        JLayeredPane desktop = new JLayeredPane();
        desktop.add(new JInternalFrame());
        desktop.add(new JInternalFrame());
        desktop.add(frame);
        assertTrue("frame index is 2", desktop.getIndexOf(frame) == 2);
        frame.moveToFront();
        assertTrue("frame index is 0", desktop.getIndexOf(frame) == 0);
        // test with different layer
        frame.setLayer(-1);
        assertTrue("frame index is 2", desktop.getIndexOf(frame) == 2);
        frame.moveToFront();
        assertTrue("frame index is 2", desktop.getIndexOf(frame) == 2);
    }

    /*
     * Class under test for void toBack()
     */
    public void testToBack() {
        // test without JDesktopPane set
        frame.toBack();
        // test with JDesktopPane set
        JDesktopPane desktop = new JDesktopPane();
        desktop.add(frame);
        desktop.add(new JInternalFrame());
        desktop.add(new JInternalFrame());
        assertTrue("frame index is 0", desktop.getIndexOf(frame) == 0);
        frame.toBack();
        assertTrue("frame index is 2", desktop.getIndexOf(frame) == 2);
        // test with different layer
        frame.setLayer(1);
        assertTrue("frame index is 0", desktop.getIndexOf(frame) == 0);
        frame.toBack();
        assertTrue("frame index is 0", desktop.getIndexOf(frame) == 0);
    }

    /*
     * Class under test for void toFront()
     */
    public void testToFront() {
        // test without JDesktopPane set
        frame.toFront();
        // test with JDesktopPane set
        JLayeredPane desktop = new JLayeredPane();
        desktop.add(new JInternalFrame());
        desktop.add(new JInternalFrame());
        desktop.add(frame);
        assertTrue("frame index is 2", desktop.getIndexOf(frame) == 2);
        frame.toFront();
        assertTrue("frame index is 0", desktop.getIndexOf(frame) == 0);
        // test with different layer
        frame.setLayer(-1);
        assertTrue("frame index is 2", desktop.getIndexOf(frame) == 2);
        frame.toFront();
        assertTrue("frame index is 2", desktop.getIndexOf(frame) == 2);
    }

    /*
     * Class under test for void fireInternalFrameEvent(int)
     */
    public void testFireInternalFrameEvent() {
        TestInternalFrame frame = new TestInternalFrame();
        MyInternalFrameListener l = new MyInternalFrameListener();
        frame.addInternalFrameListener(l);
        l.state = 0;
        frame.fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_OPENED);
        assertTrue("INTERNAL_FRAME_OPENED was fired", l.openedFired());
        l.state = 0;
        frame.fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSING);
        assertTrue("INTERNAL_FRAME_CLOSING was fired", l.closingFired());
        l.state = 0;
        frame.fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSED);
        assertTrue("INTERNAL_FRAME_CLOSED was fired", l.closedFired());
        l.state = 0;
        frame.fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_ICONIFIED);
        assertTrue("INTERNAL_FRAME_ICONIFIED was fired", l.iconifiedFired());
        l.state = 0;
        frame.fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED);
        assertTrue("INTERNAL_FRAME_DEICONIFIED was fired", l.deiconifiedFired());
        l.state = 0;
        frame.fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_ACTIVATED);
        assertTrue("INTERNAL_FRAME_ACTIVATED was fired", l.activatedFired());
        l.state = 0;
        frame.fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED);
        assertTrue("INTERNAL_FRAME_DEACTIVATED was fired", l.deactivatedFired());
        l.state = 0;
        frame.fireInternalFrameEvent(101); // test invalid value
        assertTrue("nothing happens", l.state == 0);
    }

    /*
     * Class under test for
     *     void setDefaultCloseOperation(int operation)
     *     int getDefaultCloseOperation()
     */
    public void testSetGetDefaultCloseOperation() {
        // default value is JInternalFrame.DISPOSE_ON_CLOSE
        assertEquals(WindowConstants.DISPOSE_ON_CLOSE, frame.getDefaultCloseOperation());
        // test setting valid value
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        frame.addPropertyChangeListener("defaultCloseOperation", listener);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        assertEquals(WindowConstants.DO_NOTHING_ON_CLOSE, frame.getDefaultCloseOperation());
        assertFalse("defaultCloseOperation is not a bound property", listener.ok);
        // test setting invalid value
        boolean ok = false;
        try {
            frame.setDefaultCloseOperation(101); // invalid value
        } catch (IllegalArgumentException e) {
            ok = true;
        } finally {
            assertFalse("no exception", ok);
            assertTrue("Invalid value is set", frame.getDefaultCloseOperation() == 101);
        }
    }

    /*
     * Class under test for
     *     void setLayer(int)
     *     int getLayer()
     */
    public void testSetGetLayer() {
        assertTrue("default level is 0", frame.getLayer() == 0);
        // test setLayer() without JDesktopPane set
        frame.setLayer(1);
        assertTrue("layer is set", frame.getLayer() == 1);
        // tes setLayer with JLayeredPane set
        JLayeredPane desktop = new JLayeredPane();
        desktop.add(frame);
        desktop.add(new JInternalFrame());
        assertTrue("frame index is 0", desktop.getIndexOf(frame) == 0);
        frame.setLayer(-1);
        assertTrue("frame index is 1", desktop.getIndexOf(frame) == 1);
    }

    /*
     * Class under test for void setLayer(Integer)
     */
    public void testSetLayerInteger() {
        // test setLayer() without JDesktopPane set
        frame.setLayer(new Integer(1));
        assertTrue("layer is set", frame.getLayer() == 1);
        // tes setLayer with JDesktopPane set
        JDesktopPane desktop = new JDesktopPane();
        desktop.add(frame);
        desktop.add(new JInternalFrame());
        assertTrue("frame index is 0", desktop.getIndexOf(frame) == 0);
        frame.setLayer(new Integer(-1));
        assertTrue("frame index is 1", desktop.getIndexOf(frame) == 1);
    }

    /*
     * Class under test for
     *     void setClosable(boolean)
     *     boolean isClosable()
     */
    public void testSetIsClosable() {
        assertFalse("closable is false by default", frame.isClosable());
        frame.setClosable(true);
        assertTrue("closable is set", frame.isClosable());
    }

    /*
     * Class under test for
     *     void setClosed(boolean)
     *     boolean isClosed()
     */
    public void testSetIsClosed() {
        MyInternalFrameListener frameListener = new MyInternalFrameListener();
        frame.addInternalFrameListener(frameListener);
        MyPropertyChangeListener l = new MyPropertyChangeListener();
        frame.addPropertyChangeListener(JInternalFrame.IS_CLOSED_PROPERTY, l);
        //try {
        //    SwingUtilities.invokeAndWait(new Runnable() {
        //        public void run() {
        createAndShowRootFrame();
        //        }
        //    });
        //} catch (Exception e) {
        //    assertFalse("exception", true);
        //}
        assertFalse("false by default", frame.isClosed());
        // test that this is a constrained property
        boolean thrown = false;
        l.ok = false;
        MyVetoableChangeListener vetoableListener = new MyVetoableChangeListener(
                JInternalFrame.IS_CLOSED_PROPERTY);
        frame.addVetoableChangeListener(vetoableListener);
        try {
            frame.setSelected(true);
            frameListener.state = 0;
            frame.setClosed(true);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertTrue("exception is thrown", thrown);
            assertFalse("isClosed is not set", frame.isClosed());
            assertTrue("CLOSING fired", frameListener.closingFired());
            assertFalse("CLOSED not fired", frameListener.closedFired());
            assertTrue("frame is visible", frame.isVisible());
            assertTrue("frame is selected", frame.isSelected());
            assertFalse("property change was not fired", l.ok);
            assertTrue("not removed", rootFrame.isAncestorOf(frame));
        }
        // test that this a a bound property
        thrown = false;
        l.ok = false;
        frame.removeVetoableChangeListener(vetoableListener);
        try {
            frameListener.state = 0;
            frame.setClosed(true);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertFalse("exception is no thrown", thrown);
            assertTrue("isClosed is set", frame.isClosed());
            assertTrue("CLOSING fired", frameListener.closingFired());
            assertTrue("CLOSED fired", frameListener.closedFired());
            assertFalse("frame is visible", frame.isVisible());
            assertFalse("frame is selected", frame.isSelected());
            assertTrue("property change was not fired", l.ok);
            assertFalse("removed", rootFrame.isAncestorOf(frame));
        }
        // test setting to try second time
        thrown = false;
        l.ok = false;
        try {
            frameListener.state = 0;
            frame.setClosed(true);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertFalse("exception is no thrown", thrown);
            assertFalse("CLOSING not fired", frameListener.closingFired());
            assertFalse("CLOSED not fired", frameListener.closedFired());
        }
    }

    /*
     * Class under test for
     *     void setIcon(boolean)
     *     boolean isIcon()
     */
    public void testSetIsIcon() {
        JDesktopPane desktop = new JDesktopPane();
        desktop.add(frame);
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        frame.addPropertyChangeListener(JInternalFrame.IS_ICON_PROPERTY, listener);
        MyInternalFrameListener frameListener = new MyInternalFrameListener();
        frame.addInternalFrameListener(frameListener);
        boolean thrown;
        assertFalse("isIcon is false by default", frame.isIcon());
        // test correct set to true
        thrown = false;
        frameListener.state = 0;
        try {
            frame.setIcon(true);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertFalse("exception is not thrown", thrown);
            assertTrue("isIcon is set", frame.isIcon());
            assertTrue("isIcon is a bound property", listener.ok);
            assertTrue("INTERNAL_FRAME_ICONIFIED event", frameListener.iconifiedFired());
        }
        // test correct set to false
        thrown = false;
        frameListener.state = 0;
        try {
            frame.setIcon(false);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertFalse("exception is not thrown", thrown);
            assertFalse("isIcon is set", frame.isIcon());
            assertTrue("INTERNAL_FRAME_DEICONIFIED event", frameListener.deiconifiedFired());
        }
        // test set to false when it is false already
        thrown = false;
        frameListener.state = 0;
        try {
            frame.setIcon(false);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertFalse("exception is not thrown", thrown);
            assertFalse("isIcon is set", frame.isIcon());
            assertTrue("no events", frameListener.state == 0);
        }
        // test that this is a constrained property
        thrown = false;
        frameListener.state = 0;
        frame.addVetoableChangeListener(new MyVetoableChangeListener(
                JInternalFrame.IS_ICON_PROPERTY));
        try {
            frame.setIcon(true);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertTrue("exception is thrown", thrown);
            assertFalse("isIcon is not set", frame.isIcon());
            assertTrue("no events", frameListener.state == 0);
        }
    }

    /*
     * Class under test for
     *     void setIconifiable(boolean)
     *     boolean isIconifiable()
     */
    public void testSetIsIconifiable() {
        assertFalse("iconable is false by default", frame.isIconifiable());
        frame.setIconifiable(true);
        assertTrue("iconable is set", frame.isIconifiable());
    }

    /*
     * Class under test for
     *     void setMaximizable(boolean)
     *     boolean isMaximizable()
     */
    public void testSetIsMaximizable() {
        assertFalse("maximizable is false by default", frame.isMaximizable());
        frame.setMaximizable(true);
        assertTrue("maximizable is set", frame.isMaximizable());
    }

    /*
     * Class under test for
     *     void setMaximum(boolean)
     *     boolean isMaximum()
     */
    public void testSetIsMaximum() {
        JDesktopPane desktop = new JDesktopPane();
        desktop.add(frame);
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        frame.addPropertyChangeListener(JInternalFrame.IS_MAXIMUM_PROPERTY, listener);
        boolean thrown;
        assertFalse("isMaximum is false by default", frame.isMaximum());
        // test correct set to true
        thrown = false;
        try {
            frame.setMaximum(true);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertFalse("exception is not thrown", thrown);
            assertTrue("isMaximum is set", frame.isMaximum());
            assertTrue("isMaximum is a bound property", listener.ok);
        }
        // test correct set to false
        thrown = false;
        try {
            frame.setMaximum(false);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertFalse("exception is not thrown", thrown);
            assertFalse("isMaximum is set", frame.isMaximum());
        }
        // test set to false when it is false already
        thrown = false;
        try {
            frame.setMaximum(false);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertFalse("exception is not thrown", thrown);
            assertFalse("isMaximum is set", frame.isMaximum());
        }
        // test that this is a constrained property
        thrown = false;
        frame.addVetoableChangeListener(new MyVetoableChangeListener(
                JInternalFrame.IS_MAXIMUM_PROPERTY));
        try {
            frame.setMaximum(true);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertTrue("exception is thrown", thrown);
            assertFalse("isIcon is not set", frame.isMaximum());
        }
    }

    /*
     * Class under test for
     *     void setResizable(boolean)
     *     boolean isResizable()
     */
    public void testSetIsResizable() {
        assertFalse("resizable is false by default", frame.isResizable());
        frame.setResizable(true);
        assertTrue("resizable is set", frame.isResizable());
    }

    /*
     * Class under test for
     *     void setRootPaneCheckingEnabled(boolean enabled)
     *     boolean isRootPaneCheckingEnabled()
     */
    public void testSetIsRootPaneCheckingEnabled() {
        TestInternalFrame frame = new TestInternalFrame();
        assertTrue("rootPaneCheckingEnabled is true by default", frame
                .isRootPaneCheckingEnabled());
        frame.setRootPaneCheckingEnabled(false);
        assertFalse("rootPaneCheckingEnabled is set to false", frame
                .isRootPaneCheckingEnabled());
    }

    /*
     * Class under test for
     *     void setSelected(boolean)
     *     boolean isSelected()
     */
    public void testSetIsSelected() {
        createAndShowRootFrame();
        rootFrame.getContentPane().add(new JInternalFrame()); // to make 'frame' not selectable by default
        //rootFrame.getContentPane().add(frame);
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        frame.addPropertyChangeListener(JInternalFrame.IS_SELECTED_PROPERTY, listener);
        MyInternalFrameListener frameListener = new MyInternalFrameListener();
        frame.addInternalFrameListener(frameListener);
        boolean thrown;
        assertFalse("isSelected is false by default", frame.isSelected());
        // test set to true when the internal frame is invisible
        thrown = false;
        frame.setVisible(false);
        try {
            frame.setSelected(true);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertFalse("exception is not thrown", thrown);
            assertFalse("isSelected is not set", frame.isSelected());
        }
        // test correct set to true
        thrown = false;
        frame.setVisible(true);
        try {
            frame.setSelected(false);
        } catch (PropertyVetoException e) {
        }
        frameListener.state = 0;
        listener.ok = false;
        assertFalse("isSelected is false", frame.isSelected());
        try {
            frame.setSelected(true);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertFalse("exception is not thrown", thrown);
            assertTrue("isSelected is set", frame.isSelected());
            assertTrue("isSelected is a bound property", listener.ok);
            assertTrue("event fired", frameListener.activatedFired());
            assertFalse("glassPane is invisible", frame.getGlassPane().isVisible());
        }
        // test set to false when the internal frame is invisible
        thrown = false;
        frame.setVisible(false);
        assertTrue("isSelected is true", frame.isSelected());
        try {
            frame.setSelected(false);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertFalse("exception is not thrown", thrown);
            assertFalse("isSelected is set", frame.isSelected());
        }
        // test correct set to false
        thrown = false;
        frame.setVisible(true);
        assertTrue("isSelected is true", frame.isSelected());
        frameListener.state = 0;
        try {
            frame.setSelected(false);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertFalse("exception is not thrown", thrown);
            assertFalse("isSelected is set", frame.isSelected());
            assertTrue("isSelected is a bound property", listener.ok);
            assertTrue("event fired", frameListener.deactivatedFired());
            assertTrue("glassPane is visible", frame.getGlassPane().isVisible());
        }
        // test set to false when it is false already
        thrown = false;
        try {
            frame.setSelected(false);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertFalse("exception is not thrown", thrown);
            assertFalse("isSelected is set", frame.isSelected());
        }
        // test that this is a constrained property
        thrown = false;
        MyVetoableChangeListener vetoableListener = new MyVetoableChangeListener(
                JInternalFrame.IS_SELECTED_PROPERTY);
        frame.addVetoableChangeListener(vetoableListener);
        try {
            frame.setSelected(true);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertTrue("exception is thrown", thrown);
            assertFalse("isSelected is not set", frame.isSelected());
            frame.removeVetoableChangeListener(vetoableListener);
        }
        // test set to true when it is iconified
        thrown = false;
        try {
            frame.setIcon(true);
            frame.setSelected(true);
        } catch (PropertyVetoException e) {
            thrown = true;
        } finally {
            assertFalse("exception is not thrown", thrown);
            assertTrue("isIcon is set", frame.isIcon());
            assertTrue("isSelected is set", frame.isSelected());
        }
    }

    /*
     * Class under test for void JInternalFrame(String, boolean, boolean, boolean, boolean)
     */
    public void testJInternalFrameStringbooleanbooleanbooleanboolean() {
        final String title = "Test title";
        frame = new JInternalFrame(title, true, true, true, true);
        checkJInternalFrameCorrectness(title, true, true, true, true);
        frame = new JInternalFrame(title, true, true, true, false);
        checkJInternalFrameCorrectness(title, true, true, true, false);
        frame = new JInternalFrame(title, true, true, false, true);
        checkJInternalFrameCorrectness(title, true, true, false, true);
    }

    /*
     * Class under test for void JInternalFrame(String, boolean, boolean, boolean)
     */
    public void testJInternalFrameStringbooleanbooleanboolean() {
        final String title = "Test title";
        frame = new JInternalFrame(title, true, true, true);
        checkJInternalFrameCorrectness(title, true, true, true, false);
        frame = new JInternalFrame(title, false, false, true);
        checkJInternalFrameCorrectness(title, false, false, true, false);
        frame = new JInternalFrame(title, false, true, false);
        checkJInternalFrameCorrectness(title, false, true, false, false);
    }

    /*
     * Class under test for void JInternalFrame(String, boolean, boolean)
     */
    public void testJInternalFrameStringbooleanboolean() {
        final String title = "Test title";
        frame = new JInternalFrame(title, true, true);
        checkJInternalFrameCorrectness(title, true, true, false, false);
        frame = new JInternalFrame(title, false, true);
        checkJInternalFrameCorrectness(title, false, true, false, false);
        frame = new JInternalFrame(title, true, false);
        checkJInternalFrameCorrectness(title, true, false, false, false);
    }

    /*
     * Class under test for void JInternalFrame(String, boolean)
     */
    public void testJInternalFrameStringboolean() {
        final String title = "Test title";
        frame = new JInternalFrame(title, false);
        checkJInternalFrameCorrectness(title, false, false, false, false);
        frame = new JInternalFrame(title, true);
        checkJInternalFrameCorrectness(title, true, false, false, false);
    }

    /*
     * Class under test for void JInternalFrame(String)
     */
    public void testJInternalFrameString() {
        final String title = "Test title";
        frame = new JInternalFrame(title);
        checkJInternalFrameCorrectness(title, false, false, false, false);
    }

    /*
     * Class under test for void addImpl(Component, Object, int)
     */
    public void testAddImpl() {
        TestInternalFrame frame = new TestInternalFrame();
        JComponent comp = new JPanel();
        // rootPaneCheckingEnabled is true, no exception since 1.5
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
        // rootPaneCheckingEnabled is false, no exception
        frame.setRootPaneCheckingEnabled(false);
        ok = false;
        try {
            frame.addImpl(comp, null, 0);
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception", ok);
            assertTrue("the component is added to the frame", comp.getParent() == frame);
            assertTrue("index of the component is 0", frame.getComponent(0) == comp);
        }
    }

    /*
     * Class under test for void
     *     setUI(InternalFrameUI)
     *     InternalFrameUI getUI()
     */
    public void testSetGetUI() {
        BasicInternalFrameUI ui = new BasicInternalFrameUI(frame);
        frame.setUI(ui);
        assertTrue(frame.getUI() == ui);
    }

    /*
     * Returns true if array contains obj
     */
    protected boolean contains(final Object[] array, final Object obj) {
        boolean ok = false;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == obj) {
                ok = true;
                break;
            }
        }
        return ok;
    }

    /*
     * Class under test for void
     *     void addInternalFrameListener(InternalFrameListener)
     *     removeInternalFrameListener(InternalFrameListener)
     *     InternalFrameListener[] getInternalFrameListeners()
     */
    public void testAddRemoveGetInternalFrameListener() {
        InternalFrameListener l = new MyInternalFrameListener();
        frame.addInternalFrameListener(l);
        InternalFrameListener[] listeners = frame.getInternalFrameListeners();
        assertTrue("listener was added", contains(listeners, l));
        frame.removeInternalFrameListener(l);
        listeners = frame.getInternalFrameListeners();
        assertFalse("listener was removed", contains(listeners, l));
    }

    /*
     * Class under test for
     *     void setRootPane(JRootPane)
     *     JRootPane getRootPane()
     */
    public void testSetGetRootPane() {
        TestInternalFrame frame = new TestInternalFrame();
        assertTrue("setRootPane() is called from the constructor",
                TestInternalFrame.setRootPaneCalled);
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        frame.addPropertyChangeListener(JInternalFrame.ROOT_PANE_PROPERTY, listener);
        JRootPane root = new JRootPane();
        frame.setRootPane(root);
        assertTrue(frame.getRootPane() == root);
        assertTrue("rootPane is a bound property", listener.ok);
        // test setting rootPane to null
        frame.setRootPane(null);
        assertNull(frame.getRootPane());
        assertTrue("rootPane is not removed from the container", frame.getComponentCount() == 1);
    }

    /*
     * Class under test for JRootPane createRootPane()
     */
    public void testCreateRootPane() {
        TestInternalFrame frame = new TestInternalFrame();
        assertTrue("createRootPane() is called from the constructor",
                TestInternalFrame.createRootPaneCalled);
        JRootPane root = frame.createRootPane();
        assertTrue("createRootPane() cannot return null", root != null);
    }

    /*
     * Class under test for
     *     void setMenuBar(JMenuBar)
     *     JMenuBar getMenuBar()
     */
    @SuppressWarnings("deprecation")
    public void testSetGetMenuBar() {
        assertNull(frame.getMenuBar());
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        frame.addPropertyChangeListener(JInternalFrame.MENU_BAR_PROPERTY, listener);
        JMenuBar menuBar = new JMenuBar();
        frame.setMenuBar(menuBar);
        assertTrue(frame.getMenuBar() == menuBar);
        assertTrue("menuBar is a bound property", listener.ok);
        frame.setMenuBar(null);
        assertNull(frame.getMenuBar());
    }

    /*
     * Class under test for
     *     void setJMenuBar(JMenuBar)
     *     JMenuBar getJMenuBar()
     */
    public void testSetGetJMenuBar() {
        assertNull(frame.getJMenuBar());
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        frame.addPropertyChangeListener(JInternalFrame.MENU_BAR_PROPERTY, listener);
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        assertTrue(frame.getJMenuBar() == menuBar);
        assertTrue("menuBar is a bound property", listener.ok);
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
        frame.addPropertyChangeListener(JInternalFrame.LAYERED_PANE_PROPERTY, listener);
        JLayeredPane pane = new JLayeredPane();
        frame.setLayeredPane(pane);
        assertTrue(frame.getLayeredPane() == pane);
        assertTrue("layeredPane is a bound property", listener.ok);
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
     * Class under test for void
     *     setDesktopIcon(JDesktopIcon)
     *     JDesktopIcon getDesktopIcon()
     */
    public void testSetGetDesktopIcon() {
        assertTrue("desktopIcon is not null by default", frame.getDesktopIcon() != null);
        JInternalFrame.JDesktopIcon icon = new JInternalFrame.JDesktopIcon(frame);
        assertTrue("desktopIcon is not set", frame.getDesktopIcon() != icon);
        frame.setDesktopIcon(icon);
        assertTrue("desktopIcon is set", frame.getDesktopIcon() == icon);
        frame.setDesktopIcon(null);
        assertNull("desktopIcon is null", frame.getDesktopIcon());
    }

    /*
     * Class under test for JDesktopPane getDesktopPane()
     */
    public void testGetDesktopPane() throws NullPointerException {
        // no desktopPane
        assertNull("desktopPane is null by default", frame.getDesktopPane());
        //JInternalFrame.JDesktopIcon icon = new JInternalFrame.JDesktopIcon(frame);
        //frame.setDesktopIcon(icon);
        JDesktopPane desktop = new JDesktopPane();
        // frame is added to desktopPane
        desktop.add(frame);
        assertTrue("desktopPane is set", frame.getDesktopPane() == desktop);
        // frame is removed from desktopPane
        desktop.remove(frame);
        assertNull("desktopPane is null", frame.getDesktopPane());
        // icon is added to desktoPane
        desktop.add(frame.getDesktopIcon());
        assertTrue("desktopPane is set", frame.getDesktopPane() == desktop);
        // icon is removed from desktopPane, desktopIcon == null
        // default implementation crashes here
        //desktop.remove(frame.getDesktopIcon());
        //frame.setDesktopIcon(null);
        //assertNull("desktopPane is null", frame.getDesktopPane());

        JInternalFrame jf = new JInternalFrame();
        JInternalFrame.JDesktopIcon fc = new JInternalFrame.JDesktopIcon(jf);
        fc.setInternalFrame(null);
        assertNull(fc.getDesktopPane());              
    }

    /*
     * Class under test for
     *     void setFrameIcon(Icon)
     *     Icon getFrameIcon()
     */
    public void testSetGetFrameIcon() {
        Icon icon = new ImageIcon();
        assertTrue("frameIcon is not null by default", frame.getFrameIcon() != null);
        frame.setFrameIcon(icon);
        assertTrue("frameIcon is set", frame.getFrameIcon() == icon);
        frame.setFrameIcon(null);
        assertNull("frameIcon is set to null", frame.getFrameIcon());
    }

    /*
     * Class under test for AccessibleContext getAccessibleContext()
     */
    public void testGetAccessibleContext() {
        AccessibleContext c = frame.getAccessibleContext();
        assertTrue("instanceof AccessibleJInternalFrame",
                c instanceof JInternalFrame.AccessibleJInternalFrame);
        // test getAccessibleName()
        assertTrue("AccessibleName is ok", c.getAccessibleName() == "");
        frame.setTitle("aa");
        assertTrue("AccessibleName is ok", c.getAccessibleName() == "aa");
        // test getAccessibleRole()
        assertTrue("AccessibleRole ok", c.getAccessibleRole() == AccessibleRole.INTERNAL_FRAME);
        // test getAccessibleValue()
        assertTrue("AccessibleValue ok", c.getAccessibleValue() == c);
        // test setCurrentAccessibleValue(), getCurrentAccessibleValue()
        AccessibleValue value = c.getAccessibleValue();
        assertTrue("currentAccessibleValue == 0",
                value.getCurrentAccessibleValue().intValue() == 0);
        Integer currentAccessibleValue = new Integer(4);
        boolean set = value.setCurrentAccessibleValue(currentAccessibleValue);
        assertTrue("setCurrentAccessibleValue returns true", set);
        set = value.setCurrentAccessibleValue(new Float(5));
        assertTrue("setCurrentAccessibleValue returns true", set);
        assertTrue("currentAccessibleValue == 5",
                value.getCurrentAccessibleValue().intValue() == 5);
        assertTrue("the object is not the same",
                value.getCurrentAccessibleValue() != currentAccessibleValue);
        set = value.setCurrentAccessibleValue(null);
        assertFalse("setCurrentAccessibleValue returns false", set);
        // test getMinimumAccessibleValue()
        assertTrue("minimumAccessibleValue ok",
                value.getMinimumAccessibleValue().intValue() == Integer.MIN_VALUE);
        // test getMaximumAccessibleValue()
        assertTrue("maximumAccessibleValue ok",
                value.getMaximumAccessibleValue().intValue() == Integer.MAX_VALUE);
        // test other methods
        assertNull("AccessibleDescription is ok", c.getAccessibleDescription());
        assertTrue("AccessibleChildrenCount == 1", c.getAccessibleChildrenCount() == 1);
    }

    /*
     * Class under test for
     *     void setTitle(String)
     *     String getTitle()
     */
    public void testSetGetTitle() {
        final String title = "Test title";
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        frame.addPropertyChangeListener(JInternalFrame.TITLE_PROPERTY, listener);
        assertTrue("Default title is an empty title", frame.getTitle() == "");
        // test setting the correct title
        frame.setTitle(title);
        assertTrue("Title is set correctly", frame.getTitle() == title);
        assertTrue("title is a bound property", listener.ok);
        // test setting null title
        frame.setTitle(null);
        assertNull("Title is set to null", frame.getTitle());
    }

    /*
     * Class under test for String paramString()
     */
    public void testParamString() {
        TestInternalFrame frame = new TestInternalFrame();
        assertTrue("paramString() cannot return null", frame.paramString() != null);
    }

    /*
     * Class under test for String getUIClassID()
     */
    public void testGetUIClassID() {
        assertTrue("InternalFrameUI" == frame.getUIClassID());
    }

    /*
     * Class under test for
     *     void setNormalBounds(Rectangle)
     *     Rectangle getNormalBounds()
     */
    public void testSetGetNormalBounds() {
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        frame.addPropertyChangeListener(listener);
        JDesktopPane desktopPane = new JDesktopPane();
        desktopPane.add(frame);
        Rectangle normal = new Rectangle(100, 150); // normal bounds
        Rectangle bounds = new Rectangle(150, 200); // bounds
        frame.setBounds(bounds);
        assertTrue("normalBounds is not null by default", frame.getNormalBounds() != null);
        assertFalse("normalBounds is not affected by setBounds()",
                frame.getNormalBounds() == bounds);
        frame.setNormalBounds(normal);
        assertFalse("bounds is not affected by setNormalBounds()", frame.getBounds() == normal);
        // test getNormalBounds() when isMaximum == false
        try {
            frame.setMaximum(false);
        } catch (PropertyVetoException e) {
            assertTrue("no exception should be thrown", false);
        }
        assertTrue("normalBounds is set", frame.getNormalBounds() == normal);
        assertFalse("normalBounds is not a bound property", listener.ok);
        // test getNormalBounds() when isMaximum == true
        try {
            frame.setMaximum(true);
        } catch (PropertyVetoException e) {
            assertTrue("no exception should be thrown", false);
        }
        assertTrue("normalBounds is set", frame.getNormalBounds().equals(bounds));
        assertFalse("normalBounds is set", frame.getNormalBounds() == bounds);
    }

    /*
     * Class under test for void setLayout(LayoutManager)
     */
    public void testSetLayout() {
        TestInternalFrame frame = new TestInternalFrame();
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
     * Class under test for void paintComponent(Graphics)
     */
    public void testPaintComponent() {
        // Note: painting code, cannot test
    }

    /*
     * Class under test for
     *     void setContentPane(Container)
     *     Container getContentPane()
     */
    public void testSetGetContentPane() {
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        frame.addPropertyChangeListener(JInternalFrame.CONTENT_PANE_PROPERTY, listener);
        JPanel pane = new JPanel();
        frame.setContentPane(pane);
        assertTrue(frame.getContentPane() == pane);
        assertTrue("contentPane is a bound property", listener.ok);
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
        frame.addPropertyChangeListener(JInternalFrame.GLASS_PANE_PROPERTY, listener);
        JPanel pane = new JPanel();
        frame.setGlassPane(pane);
        assertTrue(frame.getGlassPane() == pane);
        assertTrue("glassPane is a bound property", listener.ok);
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
        JComponent comp = new JPanel();
        frame.getContentPane().add(comp);
        assertTrue("label is in contentPane", frame.isAncestorOf(comp));
        frame.remove(comp);
        assertFalse("label is removed from contentPane", frame.isAncestorOf(comp));
        ((JPanel) frame.getGlassPane()).add(comp);
        frame.remove(comp);
        assertTrue("label is not removed from glassPane", frame.isAncestorOf(comp));
        // test removing from JInternalFrame
        frame.setRootPaneCheckingEnabled(false);
        frame.add(comp);
        assertTrue("added", comp.getParent() == frame);
        frame.remove(comp);
        assertTrue("removed", comp.getParent() != frame);
        // test removing null
        boolean ok = false;
        try {
            frame.remove((Component) null);
        } catch (NullPointerException e) {
            ok = true;
        } finally {
            assertTrue("exception", ok);
        }
        // test removing rootPane
        assertTrue(frame.isAncestorOf(frame.getRootPane()));
        frame.remove(frame.getRootPane());
        // rootPane is removed from the container
        assertFalse(frame.isAncestorOf(frame.getRootPane()));
        // but getRootPane() still returns it
        assertTrue(frame.getRootPane() != null);
    }

    /*
     * Creates and shows rootFrame. This method is used when JInternalFrame
     * need to be selected (isSelected() == true) for testing purposes.
     */
    protected void createAndShowRootFrame() {
        frame.setSize(70, 100);
        rootFrame = new JFrame();
        JDesktopPane desktop = new JDesktopPane();
        rootFrame.setContentPane(desktop);
        rootFrame.getContentPane().add(frame);
        rootFrame.setSize(100, 200);
        frame.setVisible(true);
        rootFrame.setVisible(true);
    }

    /*
     * Thread safe function to make the internal frame selected
     * or deselected
     */
    protected void setSelectedFrame(final JInternalFrame frame, final boolean selected) {
        //try {
        //    SwingUtilities.invokeAndWait(new Runnable() {
        //        public void run() {
        try {
            frame.setSelected(selected);
        } catch (PropertyVetoException e) {
        }
        //        }
        //    });
        //} catch (Exception e) {
        //    assertFalse("exception", true);
        //}
    }

    /*
     * Class under test for String getWarningString()
     */
    public void testGetWarningString() {
        assertNull("getWarningString() always returns null", frame.getWarningString());
    }

    /*
     * Class under test for
     *     void setFocusCycleRoot(boolean)
     *     boolean isFocusCycleRoot()
     */
    public void testFocusCycleRoot() {
        assertTrue("isFocusCycleRoot is always true", frame.isFocusCycleRoot());
        frame.setFocusCycleRoot(false);
        assertTrue("isFocusCycleRoot is always true", frame.isFocusCycleRoot());
        frame.setFocusCycleRoot(true);
        assertTrue("isFocusCycleRoot is always true", frame.isFocusCycleRoot());
    }

    /*
     * Class under test for Container getFocusCycleRootAncestor()
     */
    public void testGetFocusCycleRootAncestor() {
        assertNull("always returns null", frame.getFocusCycleRootAncestor());
    }

    /*
     * Class under test for void hide()
     */
    public void testHide() {
        frame.setVisible(true);
        frame.hide();
        assertFalse("frame is hided", frame.isVisible());
        if (isHarmony()) {
            assertFalse("icon is hided", frame.getDesktopIcon().isVisible());
        }
    }

    /*
     * Class under test for void show()
     */
    public void testShow() {
        MyInternalFrameListener l = new MyInternalFrameListener();
        frame.addInternalFrameListener(l);
        frame.show();
        assertTrue("INTERNAL_FRAME_OPENED was fired", l.openedFired());
        assertTrue("frame is visible", frame.isVisible());
        assertTrue("icon is visible", frame.getDesktopIcon().isVisible());
        assertFalse("is selected", frame.isSelected());
        // test show() the first time
        frame = new JInternalFrame();
        frame.addInternalFrameListener(l);
        l.state = 0;
        createAndShowRootFrame();
        assertTrue("INTERNAL_FRAME_OPENED was fired", l.openedFired());
        // test show() the second time
        frame.dispose();
        JInternalFrame frame2 = new JInternalFrame("frame2");
        frame2.setSize(new Dimension(50, 60));
        rootFrame.getContentPane().add(frame2);
        frame2.setVisible(true);
        rootFrame.getContentPane().add(frame);
        l.state = 0;
        frame.show();
        assertFalse("INTERNAL_FRAME_OPENED was not fired", l.openedFired());
        assertTrue("moved to the front", ((JLayeredPane) rootFrame.getContentPane())
                .getIndexOf(frame) == 0);
        assertTrue("is visible", frame.isVisible());
        assertTrue("is selected", frame.isSelected());
        // test when the frame is already shown
        frame.show();
        assertFalse("INTERNAL_FRAME_OPENED was not fired", l.openedFired());
        // test show() when the internal frame is iconified
        try {
            frame.setIcon(true);
        } catch (PropertyVetoException e) {
        }
        frame.setVisible(false);
        frame.show();
    }

    /*
     * Class under test for void dispose()
     */
    public void testDispose() {
        createAndShowRootFrame();
        MyInternalFrameListener l = new MyInternalFrameListener();
        frame.addInternalFrameListener(l);
        // test when the internal frame is visible
        l.state = 0;
        MyPropertyChangeListener l2 = new MyPropertyChangeListener();
        frame.addPropertyChangeListener(JInternalFrame.IS_CLOSED_PROPERTY, l2);
        frame.dispose();
        assertTrue("isClosed property change fired", l2.ok);
        assertFalse("is visible", frame.isVisible());
        assertFalse("is selected", frame.isSelected());
        assertTrue("is closed", frame.isClosed());
        assertTrue("INTERNAL_FRAME_CLOSED was fired", l.closedFired());
        assertFalse("INTERNAL_FRAME_CLOSING was not fired", l.closingFired());
        assertFalse("removed from the containter", rootFrame.isAncestorOf(frame));
        // test already disposed internal frame
        l.state = 0;
        frame.dispose();
        if (isHarmony()) {
            assertFalse("INTERNAL_FRAME_CLOSED was not fired", l.closedFired());
        }
    }

    public void testSetBounds() throws Exception {
        // Regression for HARMONY-1801
        final Marker validateMarker = new Marker();
        final Marker revalidateMarker = new Marker();
        final JComponent frame = new JInternalFrame() {
            @Override
            public void validate() {
                validateMarker.setOccurred();
                super.validate();
            }

            @Override
            public void revalidate() {
                revalidateMarker.setOccurred();
                super.revalidate();
            }
        };
        validateMarker.reset();
        revalidateMarker.reset();
        frame.setBounds(0, 0, 50, 500);
        assertFalse(revalidateMarker.isOccurred());
        assertTrue(validateMarker.isOccurred());
    }
}
