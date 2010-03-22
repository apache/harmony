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
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;

public class JDialogTest extends SwingTestCase {
    /*
     * This class is used to test that some methods were called.
     */
    private static class TestDialog extends JDialog {
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

    private JDialog dialog;

    /*
     * Constructor
     */
    public JDialogTest(final String name) {
        super(name);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dialog = new JDialog();
        TestDialog.initStaticVars();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (dialog.isDisplayable()) {
            dialog.dispose();
        }
    }

    /*
     * Auxiliary method to check JDialog correctness after constructor's call.
     */
    protected void checkDialogCorrectness(final JDialog dialog, final String title,
            final boolean modal) {
        assertFalse("JDialog is invisible by default", dialog.isVisible());
        assertTrue("locale is set", dialog.getLocale() == JComponent.getDefaultLocale());
        assertTrue("owner is not null", dialog.getOwner() != null);
        assertTrue("isModal is set", dialog.isModal() == modal);
        assertTrue("title is set", dialog.getTitle() == title);
    }

    /*
     * Class under test for void JDialog()
     */
    public void testJDialog() {
        dialog = new JDialog();
        // title == null, isModal() == false
        checkDialogCorrectness(dialog, null, false);
    }

    /*
     * Class under test for void dialogInit()
     */
    public void testDialogInit() {
        TestDialog dialog = new TestDialog();
        assertTrue("onwer is not null", dialog.getOwner() != null);
        assertTrue("rootPaneCheckingEnabled is true", dialog.isRootPaneCheckingEnabled());
        assertTrue("layout is not null", dialog.getLayout() != null);
        assertTrue("rootPane is not null", dialog.getRootPane() != null);
        assertTrue("locale is set", dialog.getLocale() == JComponent.getDefaultLocale());
        assertFalse("defaultLookAndFeelDecorated is false", JDialog
                .isDefaultLookAndFeelDecorated());
        assertFalse("isUndecorated is false", dialog.isUndecorated());
        assertTrue("rootPane.windowDecorationStyle is NONE", dialog.getRootPane()
                .getWindowDecorationStyle() == JRootPane.NONE);
        // test that defaultFocusTraversalPolicy is set
        //dialog.setFocusTraversalPolicy(null);
        //dialog.dialogInit();
        assertTrue("focusTraversalPolicy is set correctly",
                dialog.getFocusTraversalPolicy() == KeyboardFocusManager
                        .getCurrentKeyboardFocusManager().getDefaultFocusTraversalPolicy());
        assertTrue("focusTraversalPolicy is set", dialog.isFocusTraversalPolicySet());
        assertTrue(dialog.isFocusCycleRoot());
        assertFalse(dialog.isFocusTraversalPolicyProvider());
        JDialog.setDefaultLookAndFeelDecorated(true);
        dialog.dialogInit();
        assertTrue("isUndecorated is true", dialog.isUndecorated());
        assertTrue("rootPane.windowDecorationStyle is PLAIN_DIALOG", dialog.getRootPane()
                .getWindowDecorationStyle() == JRootPane.PLAIN_DIALOG);
        // restore default value
        JDialog.setDefaultLookAndFeelDecorated(false);
    }

    /*
     * Class under test for
     *     void setDefaultCloseOperation(int operation)
     *     int getDefaultCloseOperation()
     */
    public void testSetGetDefaultCloseOperation() {
        // default value is JDialog.HIDE_ON_CLOSE
        assertEquals(WindowConstants.HIDE_ON_CLOSE, dialog.getDefaultCloseOperation());
        // test setting valid value
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        dialog.addPropertyChangeListener("defaultCloseOperation", listener);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        assertEquals(WindowConstants.DISPOSE_ON_CLOSE, dialog.getDefaultCloseOperation());
        // it is not a bound property
        assertFalse("defaultCloseOperation is a bound property", listener.ok);
        // test setting invalid value
        boolean ok = false;
        try {
            dialog.setDefaultCloseOperation(101); // invalid value
        } catch (IllegalArgumentException e) {
            ok = true;
        } finally {
            // exception is not thrown
            assertFalse("exception was not thrown", ok);
            //  is is set
            assertTrue("invalid value is set", dialog.getDefaultCloseOperation() == 101);
        }
    }

    /*
     * Class under test for
     *     static void setDefaultLookAndFeelDecorated(boolean defaultLookAndFeelDecorated)
     *     static boolean isDefaultLookAndFeelDecorated()
     */
    public void testSetIsDefaultLookAndFeelDecorated() {
        // test for default value
        assertFalse(JDialog.isDefaultLookAndFeelDecorated());
        JDialog.setDefaultLookAndFeelDecorated(true);
        assertTrue(JDialog.isDefaultLookAndFeelDecorated());
        // restore default value
        JDialog.setDefaultLookAndFeelDecorated(false);
    }

    /*
     * Class under test for
     *     void setRootPaneCheckingEnabled(boolean enabled)
     *     boolean isRootPaneCheckingEnabled()
     */
    public void testSetIsRootPaneCheckingEnabled() {
        TestDialog dialog = new TestDialog();
        assertTrue("rootPaneCheckingEnabled is true by default", dialog
                .isRootPaneCheckingEnabled());
        dialog.setRootPaneCheckingEnabled(false);
        assertFalse("rootPaneCheckingEnabled is set to false", dialog
                .isRootPaneCheckingEnabled());
    }

    /*
     * Class under test for void JDialog(Frame, String, boolean, GraphicsConfiguration)
     */
    public void testJDialogFrameStringbooleanGraphicsConfiguration() {
        Frame owner = new Frame();
        final GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        // test with corrent owner, correct title, modal == false, correct gc
        dialog = new JDialog(owner, "Test JDialog", false, gc);
        // title is set, isModal() == false
        checkDialogCorrectness(dialog, "Test JDialog", false);
        assertTrue("owner is set", dialog.getOwner() == owner);
        assertTrue("gc is set", dialog.getGraphicsConfiguration() == gc);
        // test with corrent owner, correct title, modal == false, incorrect gc
        dialog = new JDialog(owner, "Test JDialog", false, null);
        // title is set, isModal() == false
        checkDialogCorrectness(dialog, "Test JDialog", false);
        assertTrue("owner is set", dialog.getOwner() == owner);
        assertTrue("gc is set from the owner", dialog.getGraphicsConfiguration() == dialog
                .getOwner().getGraphicsConfiguration());
        // test with null owner, correct title, modal == false, incorrect gc
        dialog = new JDialog((Frame) null, "Test JDialog", false, null);
        // title is set, isModal() == false
        checkDialogCorrectness(dialog, "Test JDialog", false);
        assertTrue("owner is set", dialog.getOwner() != null);
        // this case is not described in docs, but gc definitely can't be null
        assertTrue("gc is set", dialog.getGraphicsConfiguration() != null);
    }

    /*
     * Class under test for void JDialog(Dialog, String, boolean, GraphicsConfiguration)
     */
    public void testJDialogDialogStringbooleanGraphicsConfiguration() {
        Dialog owner = new Dialog(new Frame());
        final GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        // test with corrent owner, correct title, modal == true, correct gc
        dialog = new JDialog(owner, "Test JDialog", true, gc);
        // title is set, isModal() == true
        checkDialogCorrectness(dialog, "Test JDialog", true);
        assertTrue("owner is set", dialog.getOwner() == owner);
        assertTrue("gc is set", dialog.getGraphicsConfiguration() == gc);
        // test with corrent owner, correct title, modal == false, incorrect gc
        dialog = new JDialog(owner, "Test JDialog", false, null);
        // title is set, isModal() == false
        checkDialogCorrectness(dialog, "Test JDialog", false);
        assertTrue("owner is set", dialog.getOwner() == owner);
        assertTrue("gc is set from the owner", dialog.getGraphicsConfiguration() == dialog
                .getOwner().getGraphicsConfiguration());
    }

    /*
     * Class under test for void JDialog(Frame, String, boolean)
     */
    public void testJDialogFrameStringboolean() {
        Frame owner = new Frame();
        // test with corrent owner, correct title, modal == false
        dialog = new JDialog(owner, "Test JDialog", false);
        // title is set, isModal() == false
        checkDialogCorrectness(dialog, "Test JDialog", false);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // test with corrent owner, correct title, modal == true
        dialog = new JDialog(owner, "Test JDialog", true);
        // title is set, isModal() == true
        checkDialogCorrectness(dialog, "Test JDialog", true);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // test with corrent owner, incorrect title, modal == true
        dialog = new JDialog(owner, null, true);
        // title is not set, isModal() == true
        checkDialogCorrectness(dialog, null, true);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // test with incorrent owner, correct title, modal == true
        dialog = new JDialog((Frame) null, "Test JDialog", true);
        checkDialogCorrectness(dialog, "Test JDialog", true);
    }

    /*
     * Class under test for void JDialog(Frame, String)
     */
    public void testJDialogFrameString() {
        Frame owner = new Frame();
        // test with corrent owner, correct title
        dialog = new JDialog(owner, "Test JDialog");
        // title is set, isModal() == false
        checkDialogCorrectness(dialog, "Test JDialog", false);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // test with corrent owner, incorrect title
        dialog = new JDialog(owner, null);
        // title is not set, isModal() == false
        checkDialogCorrectness(dialog, null, false);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // test with incorrent owner, correct title
        dialog = new JDialog((Frame) null, "Test JDialog");
        checkDialogCorrectness(dialog, "Test JDialog", false);
    }

    /*
     * Class under test for void JDialog(Dialog, String, boolean)
     */
    public void testJDialogDialogStringboolean() {
        Dialog owner = new Dialog(new Frame());
        // test with corrent owner, correct title, modal == false
        dialog = new JDialog(owner, "Test JDialog", false);
        // title is set, isModal() == false
        checkDialogCorrectness(dialog, "Test JDialog", false);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // test with corrent owner, correct title, modal == true
        dialog = new JDialog(owner, "Test JDialog", true);
        // title not set, isModal() == true
        checkDialogCorrectness(dialog, "Test JDialog", true);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // test with corrent owner, incorrect title, modal == true
        dialog = new JDialog(owner, null, true);
        // title is not set, isModal() == true
        checkDialogCorrectness(dialog, null, true);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // owner cannot be null in this case
    }

    /*
     * Class under test for void JDialog(Dialog, String)
     */
    public void testJDialogDialogString() {
        Dialog owner = new Dialog(new Frame());
        // test with corrent owner, correct title
        dialog = new JDialog(owner, "Test JDialog");
        // title is set, isModal() == false
        checkDialogCorrectness(dialog, "Test JDialog", false);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // test with corrent owner, incorrect title
        dialog = new JDialog(owner, null);
        // title is not set, isModal() == false
        checkDialogCorrectness(dialog, null, false);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // owner cannot be null in this case
    }

    /*
     * Class under test for void JDialog(Frame, boolean)
     */
    public void testJDialogFrameboolean() {
        Frame owner = new Frame();
        // test with corrent owner, modal == false
        dialog = new JDialog(owner, false);
        // title == null, isModal() == false
        checkDialogCorrectness(dialog, null, false);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // test with corrent owner, modal == true
        dialog = new JDialog(owner, true);
        // title == null, isModal() == false
        checkDialogCorrectness(dialog, null, true);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // test with incorrect owner
        dialog = new JDialog((Frame) null, true);
        // title == null, isModal() == true
        checkDialogCorrectness(dialog, null, true);
    }

    /*
     * Class under test for void JDialog(Frame)
     */
    public void testJDialogFrame() {
        Frame owner = new Frame();
        // test with corrent owner
        dialog = new JDialog(owner);
        // title == null, isModal() == false
        checkDialogCorrectness(dialog, null, false);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // test with incorrect owner
        dialog = new JDialog((Frame) null);
        // title == null, isModal() == false
        checkDialogCorrectness(dialog, null, false);
    }

    /*
     * Class under test for void JDialog(Dialog, boolean)
     */
    public void testJDialogDialogboolean() {
        Dialog owner = new Dialog(new Frame());
        // test with corrent owner, modal == false
        dialog = new JDialog(owner, false);
        // title == null, isModal() == false
        checkDialogCorrectness(dialog, null, false);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // test with corrent owner, modal == true
        dialog = new JDialog(owner, true);
        // title == null, isModal() == true
        checkDialogCorrectness(dialog, null, true);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // owner cannot be null in this case
    }

    /*
     * Class under test for void JDialog(Dialog)
     */
    public void testJDialogDialog() {
        Dialog owner = new Dialog(new Frame());
        // test with corrent owner
        dialog = new JDialog(owner);
        // title == null, isModal() == false
        checkDialogCorrectness(dialog, null, false);
        assertTrue("owner is set", dialog.getOwner() == owner);
        // owner cannot be null in this case
    }

    /*
     * Class under test for void addImpl(Component, Object, int)
     */
    public void testAddImpl() {
        TestDialog dialog = new TestDialog();
        JComponent comp = new JPanel();
        // rootPaneCheckingEnabled is true, no exception since 1.5
        dialog.setRootPaneCheckingEnabled(true);
        boolean ok = false;
        try {
            dialog.addImpl(comp, null, 0);
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception", ok);
            assertTrue("The component is added to contentPane", comp.getParent() == dialog
                    .getContentPane());
        }
        // rootPaneCheckingEnabled is false, no exception
        dialog.setRootPaneCheckingEnabled(false);
        ok = false;
        try {
            dialog.addImpl(comp, null, 0);
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception", ok);
            assertTrue("the component is added to JWindow", comp.getParent() == dialog);
            assertTrue("index of the component is 0", dialog.getComponent(0) == comp);
        }
    }

    /*
     * Class under test for
     *     void setRootPane(JRootPane)
     *     JRootPane getRootPane()
     */
    public void testSetGetRootPane() {
        TestDialog dialog = new TestDialog();
        assertTrue("setRootPane() is called from the constructor", TestDialog.setRootPaneCalled);
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        dialog.addPropertyChangeListener("rootPane", listener);
        JRootPane root = new JRootPane();
        dialog.setRootPane(root);
        assertTrue(dialog.getRootPane() == root);
        assertFalse("rootPane is not a bound property", listener.ok);
        // test setting rootPane to null
        dialog.setRootPane(null);
        assertNull(dialog.getRootPane());
        assertTrue("rootPane is removed from the container", dialog.getComponentCount() == 0);
    }

    /*
     * Class under test for JRootPane createRootPane()
     */
    public void testCreateRootPane() {
        TestDialog dialog = new TestDialog();
        assertTrue("createRootPane() is called from the constructor",
                TestDialog.createRootPaneCalled);
        JRootPane root = dialog.createRootPane();
        assertTrue("createRootPane() cannot return null", root != null);
    }

    /*
     * Class under test for
     *     void setJMenuBar(JMenuBar)
     *     JMenuBar getJMenuBar()
     */
    public void testSetGetJMenuBarJMenuBar() {
        assertNull(dialog.getJMenuBar());
        JMenuBar menuBar = new JMenuBar();
        dialog.setJMenuBar(menuBar);
        assertTrue(dialog.getJMenuBar() == menuBar);
        dialog.setJMenuBar(null);
        assertNull(dialog.getJMenuBar());
    }

    /*
     * Class under test for
     *     void setLayeredPane(JLayeredPane)
     *     JLayeredPane getLayeredPane()
     */
    public void testSetGetLayeredPane() {
        MyPropertyChangeListener listener = new MyPropertyChangeListener();
        dialog.addPropertyChangeListener("layeredPane", listener);
        JLayeredPane pane = new JLayeredPane();
        dialog.setLayeredPane(pane);
        assertTrue(dialog.getLayeredPane() == pane);
        assertFalse("layeredPane is not a bound property", listener.ok);
        // test throwing exception if the parameter is null
        boolean ok = false;
        try {
            dialog.setLayeredPane(null);
        } catch (IllegalComponentStateException e) {
            ok = true;
        } finally {
            assertTrue(ok);
        }
        // layeredPane cannot be null, even after setLayeredPane(null)
        assertTrue(dialog.getLayeredPane() != null);
        // setLayeredPane() method is not called by the constructor
        // (seems that there is an error in docs)
    }

    /*
     * Class under test for AccessibleContext getAccessibleContext()
     */
    public void testGetAccessibleContext() {
        AccessibleContext c = dialog.getAccessibleContext();
        assertTrue("class is ok", c instanceof JDialog.AccessibleJDialog);
        assertTrue("AccessibleRole is ok", c.getAccessibleRole() == AccessibleRole.DIALOG);
        assertNull("AccessibleDescription is ok", c.getAccessibleDescription());
        assertTrue("AccessibleChildrenCount == 1", c.getAccessibleChildrenCount() == 1);
        // test getAccessibleName()
        assertNull("AccessibleName is ok", c.getAccessibleName());
        dialog.setTitle("aa");
        assertTrue("AccessibleName is ok", c.getAccessibleName() == "aa");
        // test getAccessibleStateSet()
        AccessibleState[] states = c.getAccessibleStateSet().toArray();
        assertTrue("more than 2 states", states.length > 2);
        dialog.setVisible(true);
        states = c.getAccessibleStateSet().toArray();
        assertTrue("more than 4 states", states.length > 4);
    }

    /*
     * Class under test for String paramString()
     */
    public void testParamString() {
        TestDialog dialog = new TestDialog();
        assertTrue("paramString() cannot return null", dialog.paramString() != null);
    }

    /*
     * Class under test for void processWindowEvent(WindowEvent)
     */
    public void testProcessWindowEvent() {
        TestDialog dialog = new TestDialog();
        dialog.setVisible(true);
        WindowEvent e = new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING);
        // test DO_NOTHING_ON_CLOSE
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.disposeCalled = false;
        dialog.processWindowEvent(e);
        assertFalse("didn't call dispose()", dialog.disposeCalled);
        assertTrue("is visible", dialog.isVisible());
        // test HIDE_ON_CLOSE
        dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        dialog.disposeCalled = false;
        dialog.processWindowEvent(e);
        assertFalse("didn't call dispose()", dialog.disposeCalled);
        assertFalse("is not visible", dialog.isVisible());
        // test DISPOSE_ON_CLOSE
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.disposeCalled = false;
        dialog.setVisible(true);
        dialog.processWindowEvent(e);
        assertTrue("called dispose()", dialog.disposeCalled);
        assertFalse("is not visible", dialog.isVisible());
    }

    /*
     * Class under test for void setLayout(LayoutManager)
     */
    public void testSetLayout() {
        TestDialog dialog = new TestDialog();
        LayoutManager contentLayout = dialog.getContentPane().getLayout();
        LayoutManager dialogLayout = dialog.getLayout();
        // rootPaneCheckingEnabled is true, no exception since 1.5
        dialog.setRootPaneCheckingEnabled(true);
        boolean ok = false;
        try {
            dialog.setLayout(new FlowLayout());
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception since 1.5", ok);
            assertTrue("contentPane layout is changed",
                    dialog.getContentPane().getLayout() != contentLayout);
            assertTrue("Dialog layout shouldn't be changed", dialog.getLayout() == dialogLayout);
            dialog.getContentPane().setLayout(contentLayout);
        }
        // rootPaneCheckingEnabled is false
        dialog.setRootPaneCheckingEnabled(false);
        ok = false;
        try {
            dialog.setLayout(new FlowLayout());
        } catch (Error e) {
            ok = true;
        } finally {
            assertFalse("no exception", ok);
            assertTrue("contentPane layout shouldn't be changed", dialog.getContentPane()
                    .getLayout() == contentLayout);
            assertTrue("Dialog layout is changed", dialog.getLayout() != dialogLayout);
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
        dialog.addPropertyChangeListener("contentPane", listener);
        JPanel pane = new JPanel();
        dialog.setContentPane(pane);
        assertTrue(dialog.getContentPane() == pane);
        assertFalse("contentPane is not a bound property", listener.ok);
        // test throwing exception if the parameter is null
        boolean ok = false;
        try {
            dialog.setContentPane(null);
        } catch (IllegalComponentStateException e) {
            ok = true;
        } finally {
            assertTrue(ok);
        }
        // contentPane cannot be null, even after setContentPane(null)
        assertTrue(dialog.getContentPane() != null);
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
        dialog.addPropertyChangeListener("glassPane", listener);
        JPanel pane = new JPanel();
        dialog.setGlassPane(pane);
        assertTrue(dialog.getGlassPane() == pane);
        assertFalse("glassPane is not a bound property", listener.ok);
        // test throwing exception if the parameter is null
        boolean ok = false;
        try {
            dialog.setGlassPane(null);
        } catch (NullPointerException e) {
            ok = true;
        } finally {
            assertTrue(ok);
        }
        // glassPane cannot be null, even after setGlassPane(null)
        assertTrue(dialog.getGlassPane() != null);
        // setGlassPane() method is not called by the constructor
        // (seems that there is an error in docs)
    }

    /*
     * Class under test for void remove(Component)
     */
    public void testRemove() {
        JComponent comp = new JPanel();
        dialog.getContentPane().add(comp);
        assertTrue("added to contentPane", dialog.isAncestorOf(comp));
        dialog.remove(comp);
        assertFalse("removed from contentPane", dialog.isAncestorOf(comp));
        ((JPanel) dialog.getGlassPane()).add(comp);
        dialog.remove(comp);
        assertTrue("not removed from glassPane", dialog.isAncestorOf(comp));
        // test removing directly from the container
        dialog.setRootPaneCheckingEnabled(false);
        dialog.add(comp, BorderLayout.EAST);
        assertTrue("added", comp.getParent() == dialog);
        dialog.remove(comp);
        assertTrue("not removed", comp.getParent() == dialog);
        // test removing null
        //        boolean ok = false;
        //        try {
        //            dialog.remove((Component)null);
        //        } catch (NullPointerException e) {
        //            ok = true;
        //        } finally {
        //            assertTrue("exception", ok);
        //        }
        // test removing rootPane
        assertTrue(dialog.isAncestorOf(dialog.getRootPane()));
        dialog.remove(dialog.getRootPane());
        // rootPane is removed from the container
        assertFalse(dialog.isAncestorOf(dialog.getRootPane()));
        // but getRootPane() still returns it
        assertTrue(dialog.getRootPane() != null);
    }
}
