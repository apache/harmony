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
import java.awt.Color;
import java.awt.Container;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowListener;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicToolBarUI.DragWindow;

public class BasicToolBarUITest extends SwingTestCase {
    private class TestBasicToolBarUI extends BasicToolBarUI {
        public Border rolloverBorder;

        public Border nonRolloverBorder;

        public Container floatingWindow;

        public boolean setFloatingCalled;

        public boolean setFloatingBParam;

        private boolean dragToCalled;

        @Override
        protected Border createRolloverBorder() {
            rolloverBorder = super.createRolloverBorder();
            return rolloverBorder;
        }

        @Override
        protected Border createNonRolloverBorder() {
            nonRolloverBorder = super.createNonRolloverBorder();
            return nonRolloverBorder;
        }

        @Override
        protected RootPaneContainer createFloatingWindow(final JToolBar toolbar) {
            RootPaneContainer result = super.createFloatingWindow(toolbar);
            if (result instanceof Container) {
                floatingWindow = (Container) result;
            }
            return result;
        }

        @Override
        public void setFloating(final boolean b, final Point p) {
            setFloatingCalled = true;
            setFloatingBParam = b;
            super.setFloating(b, p);
        }

        @Override
        protected void dragTo(final Point position, final Point origin) {
            dragToCalled = true;
            super.dragTo(position, origin);
        }
    }

    private class TestJButton extends JButton {
        private static final long serialVersionUID = 1L;

        @Override
        public void requestFocus() {
            BasicToolBarUITest.this.ui.focusedCompIndex = toolBar.getComponentIndex(this);
            super.requestFocus();
        }
    }

    private JToolBar toolBar;

    private TestBasicToolBarUI ui;

    private JButton b;

    private JLabel label;

    private JFrame frame;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        toolBar = new JToolBar();
        ui = new TestBasicToolBarUI();
        toolBar.setUI(ui);
        b = new JButton();
        label = new JLabel();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public BasicToolBarUITest(final String name) {
        super(name);
    }

    public void testInstallUI() {
        ui = new TestBasicToolBarUI();
        ui.installUI(toolBar);
        assertSame(ui.toolBar, toolBar);
        assertEquals(UIManager.getColor("ToolBar.dockingForeground"), ui.dockingBorderColor);
        assertTrue(Arrays.asList(toolBar.getContainerListeners()).contains(
                ui.toolBarContListener));
        assertSame(UIManager.get("ToolBar.ancestorInputMap"), SwingUtilities.getUIInputMap(
                toolBar, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
    }

    public void testUninstallUI() {
        ui.uninstallUI(toolBar);
        assertNull(toolBar.getBorder());
        assertFalse(Arrays.asList(toolBar.getContainerListeners()).contains(
                ui.toolBarContListener));
        assertNull(SwingUtilities.getUIInputMap(toolBar,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
    }

    public void testUninstallUIWhenFloating() {
        prepareToTestFloating();
        Point p = new Point(1, 2);
        ui.dragTo(p, p);
        ui.setFloatingLocation(p.x, p.y);
        ui.setFloating(true, null);
        ui.uninstallUI(toolBar);
        assertFalse(ui.isFloating());
        assertTrue(frame.isAncestorOf(toolBar));
        assertNull(ui.dragWindow);
    }

    public void testCreateUI() {
        ComponentUI ui1 = BasicToolBarUI.createUI(toolBar);
        assertTrue(ui1 instanceof BasicToolBarUI);
        ComponentUI ui2 = BasicToolBarUI.createUI(toolBar);
        assertNotSame(ui1, ui2);
    }

    public void testBasicToolBarUI() {
        ui = new TestBasicToolBarUI();
        assertEquals(BorderLayout.NORTH, ui.constraintBeforeFloating);
        assertEquals(-1, ui.focusedCompIndex);
    }

    public void testCanDock() {
        JPanel panel = new JPanel();
        panel.add(toolBar);
        panel.setSize(400, 300);
        panel.doLayout();
        assertTrue(ui.canDock(panel, new Point(panel.getWidth() / 2, toolBar.getHeight() - 1)));
        assertTrue(ui.canDock(panel, new Point(panel.getWidth() / 2, panel.getHeight()
                - (toolBar.getHeight() - 1))));
        assertTrue(ui.canDock(panel, new Point(toolBar.getHeight() - 1, panel.getHeight() / 2)));
        assertTrue(ui.canDock(panel, new Point(panel.getWidth() - (toolBar.getHeight() - 1),
                panel.getHeight() / 2)));
        assertFalse(ui.canDock(panel, new Point(toolBar.getHeight() + 1,
                toolBar.getHeight() + 1)));
        assertFalse(ui.canDock(panel, new Point(panel.getWidth(), panel.getHeight())));
    }

    public void testCreateDockingListener() {
        assertNotNull(ui.createDockingListener());
    }

    public void testCreateDragWindow() {
        DragWindow dragWindow = ui.createDragWindow(toolBar);
        assertNotNull(ui.createDragWindow(toolBar));
        assertNotNull(dragWindow.getOwner());
    }

    public void testCreateFloatingFrame() {
        toolBar.setName("The toolbar");
        JFrame floatingFrame = ui.createFloatingFrame(toolBar);
        assertEquals("The toolbar", floatingFrame.getTitle());
        assertFalse(floatingFrame.isAncestorOf(toolBar));
        assertTrue(floatingFrame.getWindowListeners().length > 0);
        assertFalse(floatingFrame.isResizable());
    }

    public void testCreateFloatingWindow() {
        toolBar.setName("The toolbar");
        RootPaneContainer floatingWindow = ui.createFloatingWindow(toolBar);
        assertTrue(floatingWindow instanceof JDialog);
        JDialog floatingFrame = (JDialog) floatingWindow;
        assertFalse(floatingFrame.isAncestorOf(toolBar));
        assertEquals("The toolbar", floatingFrame.getTitle());
        assertTrue(floatingFrame.getWindowListeners().length > 0);
    }

    public void testCreateFrameListener() {
        assertTrue(ui.createFrameListener() instanceof BasicToolBarUI.FrameListener);
    }

    public void testCreateNonRolloverBorder() {
        assertNotNull(ui.createRolloverBorder());
        if (isHarmony()) {
            assertTrue(ui.createRolloverBorder() instanceof UIResource);
        }
    }

    public void testCreatePropertyListener() {
        assertNotNull(ui.createPropertyListener());
    }

    public void testCreateRolloverBorder() {
        assertNotNull(ui.createRolloverBorder());
        if (isHarmony()) {
            assertTrue(ui.createRolloverBorder() instanceof UIResource);
        }
    }

    public void testCreateToolBarContListener() {
        assertNotNull(ui.createToolBarContListener());
    }

    public void testCreateToolBarFocusListener() {
        assertNotNull(ui.createToolBarFocusListener());
    }

    public void testDragTo() {
        prepareToTestFloating();
        Point origin = new Point(1, 2);
        Point position1 = frame.getLocation();
        position1.translate(frame.getWidth() + 1, 0);
        toolBar.setFloatable(false);
        ui.dragTo(position1, origin);
        assertNull(ui.dragWindow);
        toolBar.setFloatable(true);
        ui.dragTo(position1, origin);
        assertNotNull(ui.dragWindow);
        assertTrue(ui.dragWindow.isVisible());
        Point dragWindowPosition1 = ui.dragWindow.getLocation();
        Point position2 = new Point(position1);
        position2.translate(1, 2);
        ui.dragTo(position2, origin);
        Point dragWindowPosition2 = ui.dragWindow.getLocation();
        position2.translate(-position1.x, -position1.y);
        dragWindowPosition2.translate(-dragWindowPosition1.x, -dragWindowPosition1.y);
        assertEquals(position2, dragWindowPosition2);
    }

    public void testFloatAt() {
        prepareToTestFloating();
        Point origin = new Point(1, 2);
        Point position1 = frame.getLocation();
        position1.translate(frame.getWidth() + 1, 0);
        toolBar.setFloatable(false);
        ui.dragTo(position1, origin);
        ui.floatAt(position1, origin);
        assertNull(ui.floatingWindow);
        toolBar.setFloatable(true);
        ui.dragTo(position1, origin);
        ui.floatAt(position1, origin);
        assertFalse(ui.dragWindow.isVisible());
        assertTrue(ui.isFloating());
        assertTrue(ui.floatingWindow.isVisible());
        assertTrue(ui.floatingWindow.isAncestorOf(toolBar));
    }

    public void testSetGetDockingColor() {
        assertSame(ui.getDockingColor(), ui.dockingColor);
        Color c = Color.RED;
        ui.setDockingColor(c);
        assertSame(c, ui.dockingColor);
        assertSame(ui.getDockingColor(), c);
    }

    public void testSetGetFloatingColor() {
        assertSame(ui.getFloatingColor(), ui.floatingColor);
        Color c = Color.RED;
        ui.setFloatingColor(c);
        assertSame(c, ui.floatingColor);
        assertSame(ui.getFloatingColor(), c);
    }

    public void testInstallComponents() {
        toolBar.add(b);
        ui.uninstallComponents();
        int compCount = toolBar.getComponentCount();
        ui.installComponents();
        assertEquals(compCount, toolBar.getComponentCount());
    }

    public void testUninstallComponents() {
        toolBar.add(b);
        int compCount = toolBar.getComponentCount();
        ui.uninstallComponents();
        assertEquals(compCount, toolBar.getComponentCount());
    }

    public void testInstallDefaults() {
        ui = new TestBasicToolBarUI();
        ui.toolBar = toolBar;
        toolBar.add(b);
        b.setBorder(null);
        ui.installDefaults();
        assertTrue(toolBar.getForeground() instanceof UIResource);
        assertTrue(toolBar.getForeground() instanceof UIResource);
        assertTrue(toolBar.getFont() instanceof UIResource);
        assertTrue(toolBar.getBorder() instanceof UIResource);
        assertEquals(UIManager.getColor("ToolBar.dockingForeground"), ui.dockingBorderColor);
        assertEquals(UIManager.getColor("ToolBar.dockingBackground"), ui.dockingColor);
        assertEquals(UIManager.getColor("ToolBar.floatingForeground"), ui.floatingBorderColor);
        assertEquals(UIManager.getColor("ToolBar.floatingBackground"), ui.floatingColor);
        assertTrue(toolBar.isOpaque());
        if (isHarmony()) {
            assertTrue(b.getBorder() instanceof UIResource);
        }
    }

    public void testUninstallDefaults() {
        b.setBorder(null);
        toolBar.add(b);
        ui.uninstallDefaults();
        assertNull(toolBar.getBorder());
        assertNull(b.getBorder());
        assertTrue(toolBar.isOpaque());
    }

    public void testInstallKeyboardActions() {
        ui.uninstallKeyboardActions();
        ui.installKeyboardActions();
        assertSame(UIManager.get("ToolBar.ancestorInputMap"), SwingUtilities.getUIInputMap(
                toolBar, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
        assertNotNull(SwingUtilities.getUIActionMap(toolBar));
    }

    public void testUninstallKeyboardActions() {
        ui.uninstallKeyboardActions();
        assertNull(SwingUtilities.getUIInputMap(toolBar,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
        assertNull(SwingUtilities.getUIActionMap(toolBar));
    }

    public void testInstallListeners() {
        ui.uninstallListeners();
        toolBar.add(b);
        ui.installListeners();
        assertTrue(Arrays.asList(toolBar.getContainerListeners()).contains(
                ui.toolBarContListener));
        assertTrue(Arrays.asList(toolBar.getPropertyChangeListeners()).contains(
                ui.propertyListener));
        assertTrue(Arrays.asList(toolBar.getMouseListeners()).contains(ui.dockingListener));
        assertTrue(Arrays.asList(toolBar.getMouseMotionListeners())
                .contains(ui.dockingListener));
        assertFalse(Arrays.asList(toolBar.getFocusListeners())
                .contains(ui.toolBarFocusListener));
        assertTrue(Arrays.asList(b.getFocusListeners()).contains(ui.toolBarFocusListener));
    }

    public void testUninstallListeners() {
        toolBar.add(b);
        ui.uninstallListeners();
        assertFalse(Arrays.asList(toolBar.getContainerListeners()).contains(
                ui.toolBarContListener));
        assertFalse(Arrays.asList(toolBar.getPropertyChangeListeners()).contains(
                ui.propertyListener));
        assertFalse(Arrays.asList(toolBar.getMouseListeners()).contains(ui.dockingListener));
        assertFalse(Arrays.asList(toolBar.getMouseMotionListeners()).contains(
                ui.dockingListener));
        assertFalse(Arrays.asList(toolBar.getFocusListeners())
                .contains(ui.toolBarFocusListener));
        assertFalse(Arrays.asList(b.getFocusListeners()).contains(ui.toolBarFocusListener));
    }

    public void testInstallNonRolloverBorders() {
        toolBar.add(b);
        toolBar.add(label);
        b.setBorder(null);
        label.setBorder(null);
        ui.installNonRolloverBorders(toolBar);
        assertSame(ui.nonRolloverBorder, b.getBorder());
        assertNull(label.getBorder());
    }

    public void testInstallNormalBorders() {
        Border bBorder = b.getBorder();
        Border labelBorder = label.getBorder();
        toolBar.add(b);
        toolBar.add(label);
        ui.installNormalBorders(toolBar);
        assertSame(bBorder, b.getBorder());
        assertSame(labelBorder, label.getBorder());
    }

    public void testInstallRolloverBorders() {
        toolBar.add(b);
        toolBar.add(label);
        b.setBorder(null);
        label.setBorder(null);
        ui.installRolloverBorders(toolBar);
        assertSame(ui.rolloverBorder, b.getBorder());
        assertNull(label.getBorder());
    }

    public void testNavigateFocusedComp() {
        toolBar.add(new TestJButton());
        toolBar.add(new TestJButton());
        toolBar.add(new TestJButton());
        ui.focusedCompIndex = 0;
        ui.navigateFocusedComp(SwingConstants.EAST);
        assertEquals(1, ui.focusedCompIndex);
        ui.navigateFocusedComp(SwingConstants.WEST);
        assertEquals(0, ui.focusedCompIndex);
        ui.navigateFocusedComp(SwingConstants.NORTH);
        assertEquals(2, ui.focusedCompIndex);
        ui.navigateFocusedComp(SwingConstants.SOUTH);
        assertEquals(0, ui.focusedCompIndex);
    }

    public void testPaintDragWindow() {
        // Note: painting code, cannot test
    }

    public void testSetBorderToNonRollover() {
        ui.setBorderToNonRollover(b);
        assertSame(ui.nonRolloverBorder, b.getBorder());
        assertFalse(b.isRolloverEnabled());
        Border border = new EmptyBorder(new Insets(0, 0, 0, 0));
        b.setBorder(border);
        ui.setBorderToNonRollover(b);
        assertSame(border, b.getBorder());
        ui.setBorderToNonRollover(label);
        assertNull(label.getBorder());
        ui.setBorderToNonRollover(null);
    }

    public void testSetBorderToNormal() {
        Border oldBorder = b.getBorder();
        boolean oldRolloverEnabled = b.isRolloverEnabled();
        ui.setBorderToRollover(b);
        if (isHarmony()) {
            ui.setBorderToNonRollover(b);
        }
        ui.setBorderToNormal(b);
        assertSame(oldBorder, b.getBorder());
        assertEquals(oldRolloverEnabled, b.isRolloverEnabled());
        ui.setBorderToRollover(b);
        b.setBorder(new BorderUIResource.LineBorderUIResource(Color.RED));
        ui.setBorderToNormal(b);
        assertSame(oldBorder, b.getBorder());
    }

    public void testSetBorderToRollover() {
        b.setRolloverEnabled(false);
        ui.setBorderToRollover(b);
        assertSame(ui.rolloverBorder, b.getBorder());
        assertTrue(b.isRolloverEnabled());
        Border border = new EmptyBorder(new Insets(0, 0, 0, 0));
        b.setBorder(border);
        ui.setBorderToRollover(b);
        assertSame(border, b.getBorder());
        ui.setBorderToRollover(label);
        assertNull(label.getBorder());
        ui.setBorderToRollover(null);
    }

    public void testSetIsFloating() {
        prepareToTestFloating();
        Point origin = new Point(1, 2);
        Point position1 = frame.getLocation();
        position1.translate(frame.getWidth() + 1, 0);
        ui.dragTo(position1, origin);
        Container parent = toolBar.getParent();
        Object constraint = ((BorderLayout) parent.getLayout()).getConstraints(toolBar);
        ui.setFloatingLocation(position1.x, position1.y);
        ui.setFloating(true, null);
        assertTrue(ui.isFloating());
        if (isHarmony()) {
            Point offset = SwingUtilities.convertPoint(toolBar, ui.dragWindow.getOffset(),
                    SwingUtilities.getWindowAncestor(toolBar));
            position1.translate(-offset.x, -offset.y);
        }
        assertEquals(position1, ui.floatingWindow.getLocation());
        assertTrue(ui.floatingWindow.isAncestorOf(toolBar));
        assertTrue(ui.floatingWindow.isVisible());
        ui.setFloating(false, null);
        assertFalse(ui.isFloating());
        assertFalse(ui.floatingWindow.isVisible());
        assertSame(parent, toolBar.getParent());
        assertEquals(constraint, ((BorderLayout) parent.getLayout()).getConstraints(toolBar));
        ui.setFloating(false, new Point(1, frame.getHeight() / 2));
        assertFalse(ui.isFloating());
        assertFalse(ui.floatingWindow.isVisible());
        assertSame(parent, toolBar.getParent());
        assertEquals(BorderLayout.WEST, ((BorderLayout) parent.getLayout())
                .getConstraints(toolBar));
    }

    public void testSetFloatingLocation() {
        prepareToTestFloating();
        Point origin = new Point(100, 200);
        Point position1 = frame.getLocation();
        position1.translate(frame.getWidth() + 1, 0);
        ui.dragTo(position1, origin);
        Point p = new Point(100, 200);
        ui.setFloatingLocation(p.x, p.y);
        ui.setFloating(true, null);
        if (isHarmony()) {
            Point offset = SwingUtilities.convertPoint(toolBar, ui.dragWindow.getOffset(),
                    SwingUtilities.getWindowAncestor(toolBar));
            p.translate(-offset.x, -offset.y);
        }
        assertEquals(p, ui.floatingWindow.getLocation());
    }

    public void testSetOrientation() {
        ui.setOrientation(SwingConstants.VERTICAL);
        assertEquals(SwingConstants.VERTICAL, toolBar.getOrientation());
        ui.setOrientation(SwingConstants.HORIZONTAL);
        assertEquals(SwingConstants.HORIZONTAL, toolBar.getOrientation());
    }

    public void testSetIsRolloverBorders() {
        ui.setRolloverBorders(false);
        toolBar.add(b);
        toolBar.add(label);
        b.setBorder(null);
        label.setBorder(null);
        ui.setRolloverBorders(true);
        assertSame(ui.rolloverBorder, b.getBorder());
        assertNull(label.getBorder());
        assertTrue(ui.isRolloverBorders());
        b.setBorder(null);
        ui.setRolloverBorders(false);
        assertSame(ui.nonRolloverBorder, b.getBorder());
        assertFalse(ui.isRolloverBorders());
    }

    public void testDockingListener() {
        createAndShowFrame();
        BasicToolBarUI.DockingListener l = ui.new DockingListener(toolBar);
        MouseEvent e = new MouseEvent(toolBar, MouseEvent.MOUSE_DRAGGED, 0, 0, 0, 0, 0, false);
        l.mouseDragged(e);
        assertTrue(l.isDragging);
        assertFalse(new Point(0, 0).equals(l.origin));
        assertTrue(ui.dragToCalled);
        e = new MouseEvent(toolBar, MouseEvent.MOUSE_DRAGGED, 0, 0, 0, 0, 1, false);
        l.mouseReleased(e);
        assertFalse(l.isDragging);
        assertTrue(ui.setFloatingCalled);
    }

    public void testFrameListener() {
        prepareToTestFloating();
        WindowListener l = ui.createFrameListener();
        ui.dragTo(new Point(1, 1), new Point(0, 0));
        ui.floatAt(new Point(1, 1), new Point(0, 0));
        l.windowClosing(null);
        assertTrue(ui.setFloatingCalled);
        if (isHarmony()) {
            assertFalse(ui.setFloatingBParam);
        }
    }

    public void testPropertyListener() {
        toolBar.setRollover(false);
        toolBar.add(b);
        toolBar.setRollover(true);
        assertSame(ui.rolloverBorder, b.getBorder());
        toolBar.setRollover(false);
        assertSame(ui.nonRolloverBorder, b.getBorder());
    }

    public void testToolBarContListener() {
        ContainerListener l = ui.createToolBarContListener();
        Border border = b.getBorder();
        ContainerEvent e = new ContainerEvent(toolBar, ContainerEvent.COMPONENT_ADDED, b);
        l.componentAdded(e);
        assertNotSame(border, b.getBorder());
        e = new ContainerEvent(toolBar, ContainerEvent.COMPONENT_REMOVED, b);
        l.componentRemoved(e);
        assertSame(border, b.getBorder());
    }

    public void testToolBarFocusListener() {
        toolBar.add(b);
        FocusListener l = ui.createToolBarFocusListener();
        FocusEvent e = new FocusEvent(b, FocusEvent.FOCUS_GAINED);
        l.focusGained(e);
        assertEquals(toolBar.getComponentIndex(b), ui.focusedCompIndex);
    }

    private void createAndShowFrame() {
        frame = new JFrame();
        frame.setSize(90, 90);
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);
        frame.setVisible(true);
    }

    private void prepareToTestFloating() {
        createAndShowFrame();
        toolBar.add(b);
        toolBar.setFloatable(true);
    }
}
