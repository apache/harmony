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
package javax.swing.plaf.basic;

import java.awt.Canvas;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.SwingTestCase;

public class BasicSplitPaneUITest extends SwingTestCase {
    private JSplitPane splitPane;

    private BasicSplitPaneUI ui;

    @Override
    protected void setUp() throws Exception {
        splitPane = new JSplitPane();
        ui = new BasicSplitPaneUI();
        splitPane.setUI(ui);
        splitPane.setSize(new Dimension(2000, 1000));
    }

    @Override
    protected void tearDown() throws Exception {
        splitPane = null;
        ui = null;
    }

    public void testPreferredLayoutSize() throws Exception {
        splitPane.setBorder(BorderFactory.createEmptyBorder(5, 6, 7, 8));
        ui.layoutManager.layoutContainer(splitPane);
        int width = splitPane.getInsets().left + splitPane.getInsets().right
                + ui.layoutManager.components[0].getPreferredSize().width
                + ui.layoutManager.components[1].getPreferredSize().width
                + ui.layoutManager.components[2].getPreferredSize().width;
        int height = splitPane.getInsets().top + splitPane.getInsets().bottom
                + ui.layoutManager.components[0].getPreferredSize().height;
        assertEquals(new Dimension(width, height), ui.layoutManager
                .preferredLayoutSize(splitPane));
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        width = splitPane.getInsets().left + splitPane.getInsets().right
                + ui.layoutManager.components[1].getPreferredSize().width;
        height = splitPane.getInsets().top + splitPane.getInsets().bottom
                + ui.layoutManager.components[0].getPreferredSize().height
                + ui.layoutManager.components[1].getPreferredSize().height
                + ui.layoutManager.components[2].getPreferredSize().height;
        assertEquals(new Dimension(width, height), ui.layoutManager
                .preferredLayoutSize(splitPane));
    }

    public void testMinLayoutSize() throws Exception {
        splitPane.setBorder(BorderFactory.createEmptyBorder(5, 6, 7, 8));
        ui.layoutManager.layoutContainer(splitPane);
        int width = splitPane.getInsets().left + splitPane.getInsets().right
                + ui.layoutManager.components[0].getMinimumSize().width
                + ui.layoutManager.components[1].getMinimumSize().width
                + ui.layoutManager.components[2].getMinimumSize().width;
        int height = splitPane.getInsets().top + splitPane.getInsets().bottom
                + ui.layoutManager.components[0].getMinimumSize().height;
        assertEquals(new Dimension(width, height), ui.layoutManager
                .minimumLayoutSize(splitPane));
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        width = splitPane.getInsets().left + splitPane.getInsets().right
                + ui.layoutManager.components[1].getMinimumSize().width;
        height = splitPane.getInsets().top + splitPane.getInsets().bottom
                + ui.layoutManager.components[0].getMinimumSize().height
                + ui.layoutManager.components[1].getMinimumSize().height
                + ui.layoutManager.components[2].getMinimumSize().height;
        assertEquals(new Dimension(width, height), ui.layoutManager
                .minimumLayoutSize(splitPane));
    }

    public void testCreateUI() throws Exception {
        assertNotNull(BasicSplitPaneUI.createUI(splitPane));
        assertFalse(BasicSplitPaneUI.createUI(splitPane) == BasicSplitPaneUI
                .createUI(splitPane));
    }

    public void testCreatePropertyHandler() throws Exception {
        assertNotNull(ui.createPropertyChangeListener());
        if (isHarmony()) {
            assertFalse(ui.createPropertyChangeListener() == ui.createPropertyChangeListener());
        }
    }

    public void testCreateFocusHandler() throws Exception {
        assertNotNull(ui.createFocusListener());
        if (isHarmony()) {
            assertFalse(ui.createFocusListener() == ui.createFocusListener());
        }
    }

    public void testSetOrientation() throws Exception {
        propertyChangeController = new PropertyChangeController();
        splitPane.addPropertyChangeListener(propertyChangeController);
        ui = (BasicSplitPaneUI) splitPane.getUI();
        assertEquals(JSplitPane.HORIZONTAL_SPLIT, splitPane.getOrientation());
        assertEquals(JSplitPane.HORIZONTAL_SPLIT, ui.getOrientation());
        ui.setOrientation(JSplitPane.VERTICAL_SPLIT);
        assertEquals(JSplitPane.VERTICAL_SPLIT, ui.getOrientation());
        assertFalse(JSplitPane.VERTICAL_SPLIT == splitPane.getOrientation());
        ui.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        propertyChangeController.reset();
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        assertTrue(propertyChangeController.isChanged("orientation"));
        assertEquals(JSplitPane.VERTICAL_SPLIT, ui.getOrientation());
        assertEquals(JSplitPane.VERTICAL_SPLIT, splitPane.getOrientation());
    }

    public void testSetContinuousLayout() throws Exception {
        propertyChangeController = new PropertyChangeController();
        splitPane.addPropertyChangeListener(propertyChangeController);
        assertFalse(splitPane.isContinuousLayout());
        assertFalse(ui.isContinuousLayout());
        ui.setContinuousLayout(true);
        assertTrue(ui.isContinuousLayout());
        assertFalse(splitPane.isContinuousLayout());
        ui.setContinuousLayout(false);
        assertFalse(ui.isContinuousLayout());
        assertFalse(splitPane.isContinuousLayout());
        propertyChangeController.reset();
        splitPane.setContinuousLayout(true);
        assertTrue(propertyChangeController.isChanged("continuousLayout"));
        assertTrue(ui.isContinuousLayout());
        assertTrue(splitPane.isContinuousLayout());
    }

    public void testSetLastDragLocation() throws Exception {
        assertEquals(-1, ui.getLastDragLocation());
        ui.setLastDragLocation(40);
        assertEquals(40, ui.getLastDragLocation());
    }

    // Regression for HARMONY-2771
    public void testGetLastDragLocation() throws Exception {
        assertEquals(0, new BasicSplitPaneUI().getLastDragLocation());
    }

    public void testGetDivider() throws Exception {
        assertNotNull(ui.getDivider());
        assertNull(new BasicSplitPaneUI().getDivider());
    }

    public void testGetSplitPane() throws Exception {
        assertNotNull(ui.getSplitPane());
        assertTrue(splitPane == ui.getSplitPane());
        assertNull(new BasicSplitPaneUI().getSplitPane());
    }

    public void testCreateDefaultNonContinuousLayoutDivider() throws Exception {
        assertTrue(ui.createDefaultNonContinuousLayoutDivider() instanceof Canvas);
    }

    public void testCreateDefaultDivider() throws Exception {
        assertNotNull(ui.createDefaultDivider());
    }

    public void testSetNonContinuousLayoutDivider() throws Exception {
        Component component = new Component() {
            private static final long serialVersionUID = 1L;
        };
        ui.setNonContinuousLayoutDivider(component);
        assertTrue(component == ui.getNonContinuousLayoutDivider());
        assertFalse(ui.layoutManager.components[0] == component);
        assertFalse(ui.layoutManager.components[1] == component);
        assertFalse(ui.layoutManager.components[2] == component);
        assertTrue(ui.nonContinuousLayoutDivider == component);
    }

    public void testAddComponentToLayout() throws Exception {
        assertEquals(3, ui.layoutManager.components.length);
        JViewport viewport = new JViewport();
        ui.layoutManager.addLayoutComponent(JSplitPane.BOTTOM, viewport);
        assertFalse(ui.layoutManager.components[0] == viewport);
        assertTrue(ui.layoutManager.components[1] == viewport);
        assertFalse(ui.layoutManager.components[2] == viewport);
        ui.layoutManager.addLayoutComponent(JSplitPane.BOTTOM, new JButton());
        assertFalse(ui.layoutManager.components[1] == viewport);
        ui.layoutManager.addLayoutComponent(JSplitPane.RIGHT, viewport);
        assertFalse(ui.layoutManager.components[0] == viewport);
        assertTrue(ui.layoutManager.components[1] == viewport);
        assertFalse(ui.layoutManager.components[2] == viewport);
        ui.layoutManager.addLayoutComponent(JSplitPane.BOTTOM, new JButton());
        ui.layoutManager.addLayoutComponent(JSplitPane.TOP, viewport);
        assertTrue(ui.layoutManager.components[0] == viewport);
        assertFalse(ui.layoutManager.components[1] == viewport);
        assertFalse(ui.layoutManager.components[2] == viewport);
        ui.layoutManager.addLayoutComponent(JSplitPane.TOP, new JButton());
        assertFalse(ui.layoutManager.components[0] == viewport);
        ui.layoutManager.addLayoutComponent(JSplitPane.LEFT, viewport);
        assertTrue(ui.layoutManager.components[0] == viewport);
        assertFalse(ui.layoutManager.components[1] == viewport);
        assertFalse(ui.layoutManager.components[2] == viewport);
        ui.layoutManager.removeLayoutComponent(new JButton());
        assertNotNull(ui.layoutManager.components[0]);
        assertNotNull(ui.layoutManager.components[1]);
        assertNotNull(ui.layoutManager.components[2]);
        ui.layoutManager.removeLayoutComponent(ui.layoutManager.components[2]);
        assertNotNull(ui.layoutManager.components[0]);
        assertNotNull(ui.layoutManager.components[1]);
        assertNull(ui.layoutManager.components[2]);
        ui.layoutManager.removeLayoutComponent(ui.layoutManager.components[1]);
        assertNotNull(ui.layoutManager.components[0]);
        assertNull(ui.layoutManager.components[1]);
        assertNull(ui.layoutManager.components[2]);
        ui.layoutManager.removeLayoutComponent(ui.layoutManager.components[0]);
        assertNull(ui.layoutManager.components[0]);
        assertNull(ui.layoutManager.components[1]);
        assertNull(ui.layoutManager.components[2]);
        try {
            ui.layoutManager.addLayoutComponent("a", viewport);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetComponentPreferredSize() throws Exception {
        assertEquals(ui.layoutManager.components[0].getPreferredSize().width, ui.layoutManager
                .getPreferredSizeOfComponent(ui.layoutManager.components[0]));
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        assertEquals(ui.layoutManager.components[0].getPreferredSize().height, ui.layoutManager
                .getPreferredSizeOfComponent(ui.layoutManager.components[0]));
    }

    public void testGetComponentSize() throws Exception {
        ui.layoutManager.layoutContainer(splitPane);
        assertEquals(ui.layoutManager.components[0].getSize().width, ui.layoutManager
                .getSizeOfComponent(ui.layoutManager.components[0]));
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        assertEquals(ui.layoutManager.components[0].getSize().height, ui.layoutManager
                .getSizeOfComponent(ui.layoutManager.components[0]));
    }

    public void testGetAvailableSize() throws Exception {
        splitPane.setSize(1000, 2000);
        splitPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 20));
        assertEquals(splitPane.getSize().width - 10 - 20, ui.layoutManager.getAvailableSize(
                splitPane.getSize(), splitPane.getInsets()));
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        assertEquals(splitPane.getSize().height - 5 - 15, ui.layoutManager.getAvailableSize(
                splitPane.getSize(), splitPane.getInsets()));
    }

    public void testResetPreferredSizes() throws Exception {
        if (isHarmony()) {
            assertEquals(0, ui.getDividerLocation(splitPane));
            ui.resetToPreferredSizes(splitPane);
            assertEquals(splitPane.getLeftComponent().getPreferredSize().width, ui
                    .getDividerLocation(splitPane));
        }
    }

    public void testMinMaxDividerLocation() throws Exception {
        splitPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 20));
        assertEquals(splitPane.getLeftComponent().getPreferredSize().width
                + splitPane.getInsets().left, ui.getMinimumDividerLocation(splitPane));
        assertEquals(splitPane.getWidth() - splitPane.getDividerSize()
                - splitPane.getRightComponent().getMinimumSize().width
                - splitPane.getInsets().right, ui.getMaximumDividerLocation(splitPane));
    }

    public void testSetDividerLocation() throws Exception {
        ui.setDividerLocation(splitPane, 230);
        assertEquals(-1, splitPane.getDividerLocation());
        assertEquals(0, ui.getDividerLocation(splitPane));
        splitPane.setDividerLocation(20);
        assertEquals(20, splitPane.getDividerLocation());
        assertEquals(0, ui.getDividerLocation(splitPane));
        ui.layoutManager.layoutContainer(splitPane);
        assertEquals(20, ui.getDividerLocation(splitPane));
    }

    public void testGetDividerLocation() throws Exception {
        try { // Regression test for HARMONY-2661
            ui.getDividerLocation(null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    public void testInitialLocation() throws Exception {
        splitPane.setSize(1000, 2000);
        splitPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 20));
        assertEquals(splitPane.getInsets().left, ui.layoutManager.getInitialLocation(splitPane
                .getInsets()));
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        assertEquals(splitPane.getInsets().top, ui.layoutManager.getInitialLocation(splitPane
                .getInsets()));
    }

    public void testUpdateComponents() throws Exception {
        ui.layoutManager.components[0] = null;
        ui.layoutManager.components[1] = null;
        ui.layoutManager.components[2] = null;
        ui.layoutManager.updateComponents();
        assertEquals(splitPane.getLeftComponent(), ui.layoutManager.components[0]);
        assertEquals(splitPane.getRightComponent(), ui.layoutManager.components[1]);
        assertEquals(ui.getDivider(), ui.layoutManager.components[2]);
    }

    public void testSetComponentToSize() throws Exception {
        JButton b = new JButton();
        b.setSize(new Dimension(20, 30));
        int size = 5;
        int location = 15;
        int cW = 50;
        int cH = 40;
        Insets insets = new Insets(1, 2, 3, 4);
        ui.layoutManager.setComponentToSize(b, size, location, insets, new Dimension(cW, cH));
        assertEquals(
                new Rectangle(location, insets.top, size, cH - insets.top - insets.bottom), b
                        .getBounds());
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        ui.layoutManager.setComponentToSize(b, size, location, insets, new Dimension(cW, cH));
        assertEquals(
                new Rectangle(insets.left, location, cW - insets.left - insets.right, size), b
                        .getBounds());
    }

    public void testGetSizes() { // Regression test for HARMONY-2767
        ui = new BasicSplitPaneUI();
        JComponent component = new JComponent() {};
        assertEquals(new Dimension(0, 0), ui.getPreferredSize(component));
        assertEquals(new Dimension(0, 0), ui.getPreferredSize(null));
        assertEquals(new Dimension(0, 0), ui.getMinimumSize(component));
        assertEquals(new Dimension(0, 0), ui.getMinimumSize(null));
        assertEquals(new Dimension(0, 0), ui.getMaximumSize(component));
        assertEquals(new Dimension(0, 0), ui.getMaximumSize(null));
    }
}
