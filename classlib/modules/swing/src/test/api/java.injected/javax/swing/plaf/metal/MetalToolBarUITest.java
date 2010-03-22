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
package javax.swing.plaf.metal;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ContainerListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.SwingTestCase;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;

public class MetalToolBarUITest extends SwingTestCase {
    private class TestMetalToolBarUI extends MetalToolBarUI {
        public class TestMetalDockingListener extends MetalDockingListener {
            TestMetalDockingListener(final JToolBar toolBar) {
                super(toolBar);
            }

            boolean isDragging() {
                return isDragging;
            }

            Point getOrigin() {
                return origin;
            }
        }

        public Border nonRolloverBorder;

        private boolean dragToCalled;

        @Override
        protected PropertyChangeListener createRolloverListener() {
            return new MetalRolloverListener();
        }

        @Override
        protected ContainerListener createContainerListener() {
            return new MetalContainerListener();
        }

        public Point getDragWindowOffset() {
            return dragWindow.getOffset();
        }

        @Override
        protected Border createNonRolloverBorder() {
            nonRolloverBorder = super.createNonRolloverBorder();
            return nonRolloverBorder;
        }

        @Override
        protected void dragTo(final Point position, final Point origin) {
            dragToCalled = true;
            super.dragTo(position, origin);
        }
    }

    private JFrame frame;

    private JToolBar toolBar;

    private MetalToolBarUI ui;

    private JButton b;

    private JLabel label;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        b = new JButton();
        label = new JLabel();
        toolBar = new JToolBar();
        ui = new MetalToolBarUI();
        toolBar.setUI(ui);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public MetalToolBarUITest(final String name) {
        super(name);
    }

    public void testUpdate() {
        // painting code, cannot test
    }

    public void testUninstallUI() {
        // nothing to test
    }

    public void testInstallUI() {
        // nothing to test
    }

    public void testCreateUI() {
        ComponentUI ui1 = MetalToolBarUI.createUI(toolBar);
        assertTrue(ui1 instanceof MetalToolBarUI);
        ComponentUI ui2 = MetalToolBarUI.createUI(toolBar);
        assertNotSame(ui1, ui2);
    }

    public void testCreateDockingListener() {
        assertTrue(ui.createDockingListener() instanceof MetalToolBarUI.MetalDockingListener);
    }

    public void testCreateRolloverBorder() {
        assertNotNull(ui.createRolloverBorder());
        if (isHarmony()) {
            assertTrue(ui.createRolloverBorder() instanceof UIResource);
        }
    }

    public void testCreateNonRolloverBorder() {
        assertNotNull(ui.createNonRolloverBorder());
        if (isHarmony()) {
            assertTrue(ui.createNonRolloverBorder() instanceof UIResource);
        }
    }

    public void testInstallListeners() {
        TestMetalToolBarUI ui = new TestMetalToolBarUI();
        toolBar.setUI(ui);
        assertTrue(Arrays.asList(toolBar.getContainerListeners()).contains(ui.contListener));
        assertTrue(Arrays.asList(toolBar.getPropertyChangeListeners()).contains(
                ui.rolloverListener));
        assertFalse(Arrays.asList(toolBar.getPropertyChangeListeners("JToolBar.isRollover"))
                .contains(ui.rolloverListener));
    }

    public void testUninstallListeners() {
        TestMetalToolBarUI ui = new TestMetalToolBarUI();
        toolBar.setUI(ui);
        ui.uninstallListeners();
        assertFalse(Arrays.asList(toolBar.getContainerListeners()).contains(ui.contListener));
        assertFalse(Arrays.asList(toolBar.getPropertyChangeListeners()).contains(
                ui.rolloverListener));
    }

    public void testSetBorderToNonRollover() {
        TestMetalToolBarUI ui = new TestMetalToolBarUI();
        toolBar.setUI(ui);
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

    public void testMetalToolBarUI() {
        // nothing to test
    }

    public void testCreateContainerListener() {
        assertNull(ui.createContainerListener());
    }

    public void testCreateRolloverListener() {
        assertNull(ui.createRolloverListener());
    }

    public void testSetDragOffset() {
        TestMetalToolBarUI ui = new TestMetalToolBarUI();
        toolBar.setUI(ui);
        Point offset = new Point(1, 2);
        ui.setDragOffset(offset);
        assertEquals(offset, ui.getDragWindowOffset());
    }

    public void testMetalDockingListener() {
        TestMetalToolBarUI ui = new TestMetalToolBarUI();
        toolBar.setUI(ui);
        toolBar.add(b);
        createAndShowFrame();
        TestMetalToolBarUI.TestMetalDockingListener l = ui.new TestMetalDockingListener(toolBar);
        MouseEvent e = new MouseEvent(toolBar, MouseEvent.MOUSE_PRESSED, 0, 0, toolBar
                .getWidth(), toolBar.getHeight(), 0, false);
        l.mousePressed(e);
        e = new MouseEvent(toolBar, MouseEvent.MOUSE_DRAGGED, 0, 0, toolBar.getWidth(), toolBar
                .getHeight(), 0, false);
        l.mouseDragged(e);
        // false because drag should occur only on bumps
        assertFalse(l.isDragging());
        assertFalse(ui.dragToCalled);
        Point p = new Point(4, 8);
        e = new MouseEvent(toolBar, MouseEvent.MOUSE_PRESSED, 0, 0, p.x, p.y, 0, false);
        l.mousePressed(e);
        e = new MouseEvent(toolBar, MouseEvent.MOUSE_DRAGGED, 0, 0, p.x, p.y, 0, false);
        l.mouseDragged(e);
        assertTrue(l.isDragging());
        assertTrue(ui.dragToCalled);
    }

    private void createAndShowFrame() {
        frame = new JFrame();
        frame.setSize(90, 90);
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);
        frame.setVisible(true);
    }
}
