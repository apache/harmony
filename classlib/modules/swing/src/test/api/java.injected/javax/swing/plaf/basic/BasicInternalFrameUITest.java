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
package javax.swing.plaf.basic;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import javax.swing.BorderFactory;
import javax.swing.DesktopManager;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.SwingTestCase;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;

public class BasicInternalFrameUITest extends SwingTestCase {

    private class MyJInternalFrame extends JInternalFrame {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean isRootPaneCheckingEnabled() {
            return false;
        }
    }

    private MyJInternalFrame frame;

    private BasicInternalFrameUI ui;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new MyJInternalFrame();
        ui = new BasicInternalFrameUI(frame);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        UIManager.getDefaults().remove("InternalFrame.windowBindings");
    }

    private boolean belongs(final Object o, final Object[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == o) {
                return true;
            }
        }
        return false;
    }

    /*
     * Class under test for void createInternalFrameListener()
     */
    public void testCreateInternalFrameListener() {
        // cannot test createInternalFrameListener() directly, because
        // it only sets BasicInternalFrameUI.internalFrameListener field
        // which is private
        frame.setUI(ui);
        ui.uninstallKeyboardActions();
        int listenersCount = frame.getInternalFrameListeners().length;
        ui.installKeyboardActions();
        assertTrue("internalFrameListener was installed",
                frame.getInternalFrameListeners().length == listenersCount + 1);
    }

    /*
     * Class under test for
     *   void installComponents()
     *   void uninstallComponents()
     */
    public void testInstallUninstallComponents() {
        ui.frame = frame;
        ui.installComponents();
        assertTrue("titlePane installed", belongs(ui.titlePane, frame.getComponents()));
        assertTrue("northPane != null", ui.getNorthPane() != null);
        // cannot call uninstallComponents() directly
        ui.uninstallComponents();
        assertFalse("titlePane uninstalled", belongs(ui.titlePane, frame.getComponents()));
        assertNull("northPane == null", ui.getNorthPane());
    }

    /*
     * Class is under test for void installDefaults()
     */
    public void testInstallDefaults() {
        frame.setBackground(null);
        frame.setBorder(null);
        frame.setFrameIcon(null);
        frame.setLayout(null);
        ui.frame = frame;
        ui.installDefaults();
        assertTrue("background", frame.getBackground() != null);
        assertTrue("opaque", frame.isOpaque());
        assertTrue("border", frame.getBorder() != null);
        assertTrue("frameIcon", frame.getFrameIcon() != null);
        assertTrue("layout", frame.getLayout() != null);
        assertTrue("layout", ui.internalFrameLayout == frame.getLayout());
    }

    /*
     * Class is under test for void uninstallDefaults()
     */
    public void testUninstallDefaults() {
        frame.setUI(ui);
        // test general uninstallDefaults()
        ui.uninstallDefaults();
        assertTrue("background", frame.getBackground() != null);
        assertNull("border", frame.getBorder());
        assertNull("frameIcon", frame.getFrameIcon());
        assertNull("layout", frame.getLayout());
        assertNull("layout", ui.internalFrameLayout);
        // test uninstallDefaults() with user-set values
        frame.setBorder(BorderFactory.createEmptyBorder());
        frame.setLayout(new BorderLayout());
        ui.uninstallDefaults();
        assertTrue("background", frame.getBorder() != null);
        assertNull("layout", frame.getLayout());
    }

    /*
     * Class is under test for void installKeyboardActions()
     */
    public void testInstallKeyboardActions() {
        UIManager.getDefaults().put("InternalFrame.windowBindings",
                new Object[] { "shift ESCAPE", "showSystemMenu" });
        frame.setUI(ui);
        ui.uninstallKeyboardActions();
        int listenersCount = frame.getInternalFrameListeners().length;
        assertNull("no UIActionMap", SwingUtilities.getUIActionMap(frame));
        ui.installKeyboardActions();
        assertTrue("UIActionMap installed", SwingUtilities.getUIActionMap(frame) != null);
        assertTrue("internalFrameListener istalled",
                frame.getInternalFrameListeners().length == listenersCount + 1);
        assertNull("inputMap not installed", SwingUtilities.getUIInputMap(frame,
                JComponent.WHEN_IN_FOCUSED_WINDOW));
    }

    /*
     * Class is under test for void uninstallKeyboardActions()
     */
    public void testUninstallKeyboardActions() {
        UIManager.getDefaults().put("InternalFrame.windowBindings",
                new Object[] { "shift ESCAPE", "showSystemMenu" });
        frame.setUI(ui);
        ui.setupMenuOpenKey();
        int listenersCount = frame.getInternalFrameListeners().length;
        assertTrue("UIActionMap installed", SwingUtilities.getUIActionMap(frame) != null);
        assertTrue("inputMap installed", SwingUtilities.getUIInputMap(frame,
                JComponent.WHEN_IN_FOCUSED_WINDOW) != null);
        ui.uninstallKeyboardActions();
        assertNull("no UIActionMap", SwingUtilities.getUIActionMap(frame));
        assertTrue("internalFrameListener unistalled",
                frame.getInternalFrameListeners().length == listenersCount - 1);
        assertNull("inputMap uninstalled", SwingUtilities.getUIInputMap(frame,
                JComponent.WHEN_IN_FOCUSED_WINDOW));
    }

    /*
     * Class under test for void installListeners()
     */
    public void testInstallListeners() {
        JFrame f = new JFrame();
        f.setSize(50, 100);
        f.getContentPane().add(frame);
        f.setVisible(true);
        frame.setUI(ui);
        // glassPaneDispatcher
        assertTrue("glassPaneDispatcher != null", ui.glassPaneDispatcher != null);
        assertTrue("glassPaneDispatcher is mouse listener", belongs(ui.glassPaneDispatcher,
                frame.getGlassPane().getMouseListeners()));
        assertTrue("glassPaneDispatcher is mouse motion listener", belongs(
                ui.glassPaneDispatcher, frame.getGlassPane().getMouseMotionListeners()));
        // borderListener
        assertTrue("borderListener != null", ui.borderListener != null);
        assertTrue("borderListener is mouse listener", belongs(ui.borderListener, frame
                .getMouseListeners()));
        assertTrue("borderListener is mouse motion listener", belongs(ui.borderListener, frame
                .getMouseMotionListeners()));
        // propertyChangeListener
        assertTrue("propertyChangeListener != null", ui.propertyChangeListener != null);
        assertTrue("propertyChangeListener installed", belongs(ui.propertyChangeListener, frame
                .getPropertyChangeListeners()));
        // componentListener
        assertTrue("componentListener != null", ui.componentListener != null);
        assertTrue("componentListener installed", belongs(ui.componentListener, frame
                .getParent().getComponentListeners()));
        f.dispose();
    }

    /*
     * Class under test for void uninstallListeners()
     */
    public void testUninstallListeners() {
        JFrame f = new JFrame();
        f.setSize(50, 100);
        f.getContentPane().add(frame);
        f.setVisible(true);
        frame.setUI(ui);
        ui.uninstallListeners();
        // glassPaneDispatcher
        assertFalse("glassPaneDispatcher is not mouse listener", belongs(
                ui.glassPaneDispatcher, frame.getGlassPane().getMouseListeners()));
        assertFalse("glassPaneDispatcher is not mouse motion listener", belongs(
                ui.glassPaneDispatcher, frame.getGlassPane().getMouseMotionListeners()));
        // borderListener
        assertFalse("borderListener is not mouse listener", belongs(ui.borderListener, frame
                .getMouseListeners()));
        assertFalse("borderListener is not mouse motion listener", belongs(ui.borderListener,
                frame.getMouseMotionListeners()));
        // propertyChangeListener
        assertFalse("propertyChangeListener uninstalled", belongs(ui.propertyChangeListener,
                frame.getPropertyChangeListeners()));
        // componentListener
        assertFalse("componentListener uninstalled", belongs(ui.componentListener, frame
                .getComponentListeners()));
        f.dispose();
    }

    /*
     * Class under test for void setupMenuCloseKey()
     */
    public void testSetupMenuCloseKey() {
        // the function does nothing, just check that it doesn't crash
        ui.setupMenuCloseKey();
    }

    /*
     * Class under test for void setupMenuOpenKey()
     */
    public void testSetupMenuOpenKey() {
        frame.setUI(ui);
        assertNull("inputMap not installed", SwingUtilities.getUIInputMap(frame,
                JComponent.WHEN_IN_FOCUSED_WINDOW));
        // "InternalFrame.windowBindings" is empty - inputMap is not installed
        final String key = "InternalFrame.windowBindings";
        if (UIManager.get(key) == null) {
            ui.setupMenuOpenKey();
            assertNull("inputMap not installed", SwingUtilities.getUIInputMap(frame,
                    JComponent.WHEN_IN_FOCUSED_WINDOW));
        }
        // "InternalFrame.windowBindings" is not empty - inputMap is installed
        UIManager.getDefaults().put(key, new Object[] { "shift ESCAPE", "showSystemMenu" });
        ui.setupMenuOpenKey();
        final InputMap uiInputMap = SwingUtilities.getUIInputMap(frame,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertNotNull("inputMap installed", uiInputMap);
        assertEquals(1, uiInputMap.allKeys().length);
        
        // Regression test for HARMONY-2709
        try {
            new BasicInternalFrameUI(null).setupMenuOpenKey();
            fail("NPE expected");
        } catch (NullPointerException npe) {
            // PASSED
        } 
    }

    /*
     * Class under test for void BasicInternalFrameUI(JInternalFrame)
     */
    public void testBasicInternalFrameUI() {
        ui = new BasicInternalFrameUI(frame);
        assertTrue("frame is not set", ui.frame != frame);
        assertTrue("ui is not installed", frame.getUI() != ui);
    }

    /*
     * Class under test for MouseInputAdapter createBorderListener(JInternalFrame)
     */
    public void testCreateBorderListener() {
        MouseInputAdapter listener1 = ui.createBorderListener(frame);
        assertTrue("not null", listener1 != null);
        assertTrue("instanceof BorderListener",
                listener1 instanceof BasicInternalFrameUI.BorderListener);
        MouseInputAdapter listener2 = ui.createBorderListener(frame);
        assertTrue("new instance", listener1 != listener2);
    }

    /*
     * Class under test for JComponent createWestPane(JInternalFrame)
     */
    public void testCreateWestPane() {
        frame.setUI(ui);
        assertNull("null", ui.createWestPane(frame));
        assertNull("null", ui.createWestPane(null));
    }

    /*
     * Class under test for JComponent createSouthPane(JInternalFrame)
     */
    public void testCreateSouthPane() {
        frame.setUI(ui);
        assertNull("null", ui.createSouthPane(frame));
        assertNull("null", ui.createSouthPane(null));
    }

    /*
     * Class under test for JComponent createNorthPane(JInternalFrame)
     */
    public void testCreateNorthPane() {
        JComponent comp = ui.createNorthPane(frame);
        assertTrue("title pane", comp instanceof BasicInternalFrameTitlePane);
        assertFalse("doesn't belong", belongs(comp, frame.getComponents()));
        assertTrue("northPane", ui.getNorthPane() != comp);
        assertTrue("== titlePane", comp == ui.titlePane);
        JComponent comp2 = ui.createNorthPane(frame);
        assertTrue("new object", comp != comp2);
        assertTrue("northPane", ui.getNorthPane() != comp2);
        
        try { 
            BasicInternalFrameUIExt f = new BasicInternalFrameUIExt(null);
            f.createNorthPane(null);
            fail("NPE should be thrown");
        } catch (NullPointerException npe) {              
            // PASSED            
        }
    }

    class BasicInternalFrameUIExt extends BasicInternalFrameUI {
        BasicInternalFrameUIExt (JInternalFrame c) {
            super(c);
        }
        
        public JComponent createNorthPane(JInternalFrame c) {
            return super.createNorthPane(c);
        }
    } 

    /*
     * Class under test for JComponent createEastPane(JInternalFrame)
     */
    public void testCreateEastPane() {
        frame.setUI(ui);
        assertNull("null", ui.createEastPane(frame));
        assertNull("null", ui.createEastPane(null));
    }

    /*
     * Class under test for void replacePane(JComponent, JComponent)
     */
    public void testReplacePane() {
        frame.setUI(ui);
        JComponent comp = new JButton("OK");
        // ordinary replace
        ui.replacePane(ui.getNorthPane(), comp);
        assertFalse("old pane removed", belongs(ui.getNorthPane(), frame.getComponents()));
        assertFalse("mouse listener removed", belongs(ui.borderListener, ui.getNorthPane()
                .getMouseListeners()));
        assertFalse("mouse motion listener removed", belongs(ui.borderListener, ui
                .getNorthPane().getMouseMotionListeners()));
        assertTrue("new pane added", belongs(comp, frame.getComponents()));
        assertTrue("mouse listener istalled", belongs(ui.borderListener, comp
                .getMouseListeners()));
        assertTrue("mouse motion listener istalled", belongs(ui.borderListener, comp
                .getMouseMotionListeners()));
        // replace to null
        ui.replacePane(comp, null);
        assertFalse("new pane removed", belongs(comp, frame.getComponents()));
        assertFalse("mouse listener removed", belongs(ui.borderListener, comp
                .getMouseListeners()));
        assertFalse("mouse motion listener removed", belongs(ui.borderListener, comp
                .getMouseMotionListeners()));
    }

    /*
     * Class under test for Dimension getPreferredSize(JComponent)
     */
    public void testGetPreferredSize() {
        frame.setPreferredSize(new Dimension(200, 200));
        Dimension standardSize = new Dimension(100, 100);
        // ui is not installed into the frame
        assertTrue("standard size", ui.getPreferredSize(frame).equals(standardSize));
        // ui.getPreferredSize(null) crashes with NullPointerException
        ui.installUI(frame);
        // ui is installed into the frame
        assertTrue("size ok", ui.getPreferredSize(frame).equals(
                ui.internalFrameLayout.preferredLayoutSize(frame)));
        assertTrue("standard size", ui.getPreferredSize(null).equals(standardSize));
        assertTrue("standard size", ui.getPreferredSize(new JInternalFrame()).equals(
                standardSize));
    }

    /*
     * Class under test for Dimension getMinimumSize(JComponent)
     */
    public void testGetMinimumSize() {
        frame.setMinimumSize(new Dimension(200, 200));
        Dimension standardSize = new Dimension(0, 0);
        // ui is not installed into the frame
        assertTrue("standard size", ui.getMinimumSize(frame).equals(standardSize));
        // ui.getMinimumSize(null) crashes with NullPointerException
        ui.installUI(frame);
        // ui is installed into the frame
        assertTrue("size ok", ui.getMinimumSize(frame).equals(
                ui.internalFrameLayout.minimumLayoutSize(frame)));
        assertTrue("standard size", ui.getMinimumSize(null).equals(standardSize));
        assertTrue("standard size", ui.getMinimumSize(new JInternalFrame())
                .equals(standardSize));
    }

    /*
     * Class under test for Dimension getMaximumSize(JComponent)
     */
    public void testGetMaximumSize() {
        ui.installUI(frame);
        // ui is installed into the frame
        Dimension size = ui.getMaximumSize(frame);
        assertTrue(size.width >= Short.MAX_VALUE);
        assertTrue(size.height >= Short.MAX_VALUE);
        size = ui.getMaximumSize(null);
        assertTrue(size.width >= Short.MAX_VALUE);
        assertTrue(size.height >= Short.MAX_VALUE);
    }

    /*
     * Class under test for MouseInputListener createGlassPaneDispatcher()
     */
    public void testCreateGlassPaneDispatcher() {
        MouseInputListener listener1 = ui.createGlassPaneDispatcher();
        assertTrue("not null", listener1 != null);
        assertNotSame(listener1, ui.glassPaneDispatcher);
    }

    /*
     * Class under test for void minimizeFrame(JInternalFrame)
     */
    public void testMinimizeFrame() {
        Dimension size = new Dimension(10, 20);
        frame.setUI(ui);
        frame.setSize(size);
        frame.setMaximizable(true);
        JDesktopPane desktop = new JDesktopPane();
        desktop.setSize(new Dimension(100, 200));
        desktop.add(frame);
        try {
            frame.setMaximum(true);
        } catch (PropertyVetoException e) {
            assertTrue("exception", false);
        }
        assertTrue("size changed", frame.getSize().equals(desktop.getSize()));
        frame.setMaximizable(false);
        ui.minimizeFrame(frame);
        assertFalse("minimized", frame.getSize().equals(desktop.getSize()));
    }

    /*
     * Class under test for void maximizeFrame(JInternalFrame)
     */
    public void testMaximizeFrame() {
        frame.setUI(ui);
        JDesktopPane desktop = new JDesktopPane();
        desktop.setSize(new Dimension(100, 200));
        desktop.add(frame);
        ui.maximizeFrame(frame);
        assertTrue("maximized", frame.getSize().equals(desktop.getSize()));
    }

    /*
     * Class under test for void iconifyFrame(JInternalFrame)
     */
    public void testIconifyFrame() {
        frame.setUI(ui);
        JDesktopPane desktop = new JDesktopPane();
        desktop.setSize(new Dimension(100, 200));
        desktop.add(frame);
        ui.iconifyFrame(frame);
        assertTrue("iconified", desktop.isAncestorOf(frame.getDesktopIcon()));
    }

    /*
     * Class under test for void deiconifyFrame(JInternalFrame)
     */
    public void testDeiconifyFrame() {
        frame.setUI(ui);
        JDesktopPane desktop = new JDesktopPane();
        desktop.setSize(new Dimension(100, 200));
        desktop.add(frame);
        frame.setIconifiable(true);
        try {
            frame.setIcon(true);
        } catch (PropertyVetoException e) {
            assertTrue("exception", false);
        }
        assertTrue("iconified", desktop.isAncestorOf(frame.getDesktopIcon()));
        frame.setIconifiable(false);
        ui.deiconifyFrame(frame);
        assertTrue("deiconified", desktop.isAncestorOf(frame));
    }

    /*
     * Class under test for void activateFrame(JInternalFrame)
     */
    public void testActivateFrame() {
        frame.setUI(ui);
        JDesktopPane desktop = new JDesktopPane();
        desktop.setSize(new Dimension(100, 200));
        desktop.add(frame);
        assertTrue("not selected", desktop.getSelectedFrame() != frame);
        ui.activateFrame(frame);
        assertTrue("activated", desktop.getSelectedFrame() == frame);
    }

    /*
     * Class under test for void deactivateFrame(JInternalFrame)
     */
    public void testDeactivateFrame() {
        frame.setUI(ui);
        JDesktopPane desktop = new JDesktopPane();
        desktop.setSize(new Dimension(100, 200));
        desktop.add(frame);
        ui.activateFrame(frame);
        assertTrue("activated", desktop.getSelectedFrame() == frame);
        ui.deactivateFrame(frame);
        assertTrue("deactivated", desktop.getSelectedFrame() != frame);
    }

    /*
     * Class under test for void closeFrame(JInternalFrame)
     */
    public void testCloseFrame() {
        frame.setUI(ui);
        JDesktopPane desktop = new JDesktopPane();
        desktop.setSize(new Dimension(100, 200));
        desktop.add(frame);
        ui.closeFrame(frame);
        assertFalse("frame removed", desktop.isAncestorOf(frame));
        assertFalse("desktop icon removed", desktop.isAncestorOf(frame.getDesktopIcon()));
    }

    /*
     * Class under test for
     *     void setWestPane(JComponent)
     *     JComponent getWestPane()
     */
    public void testSetGetWestPane() {
        frame.setUI(ui);
        JButton comp = new JButton("ok");
        // null by default
        assertNull("null by default", ui.getWestPane());
        // general setting
        ui.setWestPane(comp);
        assertTrue("was set", ui.getWestPane() == comp);
        assertTrue("field was set", ui.westPane == comp);
        assertFalse("not added", belongs(comp, frame.getComponents()));
        // setting to null
        ui.setWestPane(null);
        assertNull("was set", ui.getWestPane());
    }

    /*
     * Class under test for
     *     void setSouthPane(JComponent)
     *     JComponent getSouthPane()
     */
    public void testSetGetSouthPane() {
        frame.setUI(ui);
        JButton comp = new JButton("ok");
        // null by default
        assertNull("null by default", ui.getSouthPane());
        // general setting
        ui.setSouthPane(comp);
        assertTrue("was set", ui.getSouthPane() == comp);
        assertTrue("field was set", ui.southPane == comp);
        assertFalse("not added", belongs(comp, frame.getComponents()));
        // setting to null
        ui.setSouthPane(null);
        assertNull("was set", ui.getSouthPane());
    }

    /*
     * Class under test for
     *     void setNorthPane(JComponent)
     *     JComponent getNorthPane()
     */
    public void testSetGetNorthPane() {
        frame.setUI(ui);
        JComponent comp = new JButton("ok");
        // not null by default
        assertTrue("not null by default", ui.getNorthPane() != null);
        // general setting
        ui.setNorthPane(comp);
        assertTrue("was set", ui.getNorthPane() == comp);
        assertTrue("field was set", ui.northPane == comp);
        assertTrue("added", belongs(comp, frame.getComponents()));
        // setting to null
        ui.setNorthPane(null);
        assertNull("was set", ui.getNorthPane());
    }

    /*
     * Class under test for
     *     void setEastPane(JComponent)
     *     JComponent getEastPane()
     */
    public void testSetGetEastPane() {
        frame.setUI(ui);
        JButton comp = new JButton("ok");
        // null by default
        assertNull("null by default", ui.getEastPane());
        // general setting
        ui.setEastPane(comp);
        assertTrue("was set", ui.getEastPane() == comp);
        assertTrue("field was set", ui.eastPane == comp);
        assertFalse("not added", belongs(comp, frame.getComponents()));
        // setting to null
        ui.setEastPane(null);
        assertNull("was set", ui.getEastPane());
    }

    /*
     * Class under test for ComponentUI createUI(JComponent)
     */
    public void testCreateUI() {
        ComponentUI ui1 = BasicInternalFrameUI.createUI(frame);
        ComponentUI ui2 = BasicInternalFrameUI.createUI(frame);
        assertTrue("not null", ui1 != null);
        assertTrue("statefull", ui1 != ui2);
    }

    /*
     * Class under test for void installUI(JComponent)
     */
    public void testInstallUI() {
        frame.setBorder(null);
        ui.installUI(frame);
        // we'll check only some key points
        assertTrue("installed titlePane", belongs(ui.titlePane, frame.getComponents()));
        assertTrue("installed border", frame.getBorder() != null);
        assertTrue("borderListener != null", ui.borderListener != null);
        assertTrue("installed borderListener", belongs(ui.borderListener, frame
                .getMouseListeners()));
    }

    /*
     * Class under test for void uninstallUI(JComponent)
     */
    public void testUninstallUI() {
        ui.installUI(frame);
        ui.uninstallUI(frame);
        // we'll check only some key points
        assertFalse("uninstalled titlePane", belongs(ui.titlePane, frame.getComponents()));
        assertFalse("uninstalled border", frame.getBorder() != null);
        assertFalse("uninstalled borderListener", belongs(ui.borderListener, frame
                .getMouseListeners()));
    }

    /*
     * Class under test for void installMouseHandlers(JComponent)
     */
    public void testInstallMouseHandlers() {
        ui.borderListener = ui.createBorderListener(frame);
        // install to frame
        ui.installMouseHandlers(frame);
        assertTrue("mouseListener in frame", belongs(ui.borderListener, frame
                .getMouseListeners()));
        assertTrue("mouseMotionListener in frame", belongs(ui.borderListener, frame
                .getMouseMotionListeners()));
        // install to titlePane
        ui.titlePane = new BasicInternalFrameTitlePane(frame);
        ui.installMouseHandlers(ui.titlePane);
        assertTrue("mouseListener in titlePane", belongs(ui.borderListener, ui.titlePane
                .getMouseListeners()));
        assertTrue("mouseMotionListener in titlePane", belongs(ui.borderListener, ui.titlePane
                .getMouseMotionListeners()));
        //        JInternalFrame frame = new JInternalFrame();
        //        ui.frame = frame;
        //        System.out.println(frame.getMouseListeners().length);
        //        ui.installMouseHandlers(frame);
        //        System.out.println(frame.getMouseListeners().length);
    }

    /*
     * Class under test for void deinstallMouseHandlers(JComponent)
     */
    public void testDeinstallMouseHandlers() {
        ui.borderListener = ui.createBorderListener(frame);
        ui.installMouseHandlers(frame);
        ui.titlePane = new BasicInternalFrameTitlePane(frame);
        ui.installMouseHandlers(ui.titlePane);
        // deinstall from frame
        ui.deinstallMouseHandlers(frame);
        assertFalse("deinstalled mouseListener from frame", belongs(ui.borderListener, frame
                .getMouseListeners()));
        assertFalse("deinstalled mouseMotionListener from  frame", belongs(ui.borderListener,
                frame.getMouseMotionListeners()));
        // deinstall from titlePane
        ui.deinstallMouseHandlers(ui.titlePane);
        assertFalse("deinstalled mouseListener from titlePane", belongs(ui.borderListener,
                ui.titlePane.getMouseListeners()));
        assertFalse("deinstalled mouseMotionListener from titlePane", belongs(
                ui.borderListener, ui.titlePane.getMouseMotionListeners()));
    }

    /*
     * Class under test for DesktopManager getDesktopManager()
     */
    public void testGetDesktopManager() {
        frame.setUI(ui);
        // no desktop pane, the default desktop manager is created
        DesktopManager manager1 = ui.getDesktopManager();
        assertTrue("not null", manager1 != null);
        DesktopManager manager2 = ui.getDesktopManager();
        assertTrue("the same object", manager1 == manager2);
        assertNull("no desktop pane", frame.getDesktopPane());
        // the desktop pane is set
        JDesktopPane desktop = new JDesktopPane();
        desktop.add(frame);
        manager2 = ui.getDesktopManager();
        assertTrue("not null", manager2 != null);
        assertTrue("is taken from desktop pane", manager2 == frame.getDesktopPane()
                .getDesktopManager());
        assertTrue("another object", manager2 != manager1);
    }

    /*
     * Class under test for DesktopManager createDesktopManager()
     */
    public void testCreateDesktopManager() {
        DesktopManager manager1 = ui.createDesktopManager();
        assertTrue("not null", manager1 != null);
        DesktopManager manager2 = ui.createDesktopManager();
        assertTrue("other object", manager1 != manager2);
    }

    /*
     * Class under test for PropertyChangeListener createPropertyChangeListener()
     */
    public void testCreatePropertyChangeListener() {
        PropertyChangeListener listener1 = ui.createPropertyChangeListener();
        assertTrue("not null", listener1 != null);
        PropertyChangeListener listener2 = ui.createPropertyChangeListener();
        assertTrue("the same object", listener1 == listener2);
        //assertTrue("is saved", ui.propertyChangeListener == listener1);
    }

    /*
     * Class under test for ComponentListener createComponentListener()
     */
    public void testCreateComponentListener() {
        ComponentListener listener1 = ui.createComponentListener();
        assertTrue("not null", listener1 != null);
        assertNotSame(listener1, ui.componentListener);
    }

    /*
     * Class under test for LayoutManager createLayoutManager()
     */
    public void testCreateLayoutManager() {
        LayoutManager layout = ui.createLayoutManager();
        assertTrue("not null", layout != null);
        if (isHarmony()) {
            assertTrue("instanceof InternalFrameLayout",
                    layout instanceof BasicInternalFrameUI.InternalFrameLayout);
        }
        assertNotSame(layout, ui.internalFrameLayout);
    }

    /*
     * Class under test for
     *     void setKeyBindingRegistered()
     *     boolean isKeyBindingRegistered()
     */
    public void testSetIsKeyBindingRegistered() {
        frame.setUI(ui);
        assertFalse("false by default", ui.isKeyBindingRegistered());
        ui.setKeyBindingRegistered(true);
        assertTrue("set to true", ui.isKeyBindingRegistered());
        ui.setKeyBindingRegistered(false);
        assertFalse("set to false", ui.isKeyBindingRegistered());
    }

    /*
     * Class under test for
     *     void setKeyBindingActive()
     *     boolean isKeyBindingActive()
     */
    public void testSetIsKeyBindingActive() {
        frame.setUI(ui);
        assertFalse("false by default", ui.isKeyBindingActive());
        ui.setKeyBindingActive(true);
        assertTrue("set to true", ui.isKeyBindingActive());
        ui.setKeyBindingActive(false);
        assertFalse("set to false", ui.isKeyBindingActive());
    }

    public void testBorderListener() {
        // the documentation is empty
    }

    public void testBasicInternalFrameListener() {
        // the documentation is empty
    }

    public void testComponentHandler() {
        // the documentation is empty
    }

    public void testGlassPaneDispatcher() {
        // the documentation is empty
    }

    public void testInternalFramePropertyChangeListener() {
        // the documentation is empty
    }
}
