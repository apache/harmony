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

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

public class MetalTabbedPaneUITest extends SwingTestCase {
    private class TestMetalTabbedPaneUI extends MetalTabbedPaneUI {
        @Override
        protected FontMetrics getFontMetrics() {
            return super.getFontMetrics();
        }

        @Override
        protected int calculateTabWidth(final int tabPlacement, final int tabIndex,
                final FontMetrics fm) {
            return super.calculateTabWidth(tabPlacement, tabIndex, fm);
        }

        @Override
        protected int calculateTabHeight(final int tabPlacement, final int tabIndex,
                final int fontHeight) {
            return super.calculateTabHeight(tabPlacement, tabIndex, fontHeight);
        }

        @Override
        protected int getRunForTab(final int tabCount, final int tabIndex) {
            return super.getRunForTab(tabCount, tabIndex);
        }
    }

    private JTabbedPane tabbed;

    private TestMetalTabbedPaneUI ui;

    private JFrame frame;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tabbed = new JTabbedPane();
        ui = new TestMetalTabbedPaneUI();
        tabbed.setUI(ui);
        tabbed.addTab("tab1", new JLabel());
        tabbed.setIconAt(0,
                new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)));
        tabbed.setDisabledIconAt(0, new ImageIcon(new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB)));
        tabbed.addTab("tabtab2", new JLabel());
        FontMetrics fm = ui.getFontMetrics();
        tabbed.setSize(ui.calculateTabWidth(tabbed.getTabPlacement(), 0, fm)
                + ui.calculateTabWidth(tabbed.getTabPlacement(), 1, fm) + 10, 100);
        tabbed.doLayout();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public MetalTabbedPaneUITest(final String name) {
        super(name);
    }

    public void testUpdate() {
        // Note: painting code, cannot test
    }

    public void testPaint() {
        // Note: painting code, cannot test
    }

    public void testCreateUI() {
        ComponentUI ui = MetalTabbedPaneUI.createUI(tabbed);
        assertTrue(ui instanceof MetalTabbedPaneUI);
    }

    public void testCalculateMaxTabHeight() {
        int tabPlacement = tabbed.getTabPlacement();
        int fontHeight = tabbed.getFontMetrics(tabbed.getFont()).getHeight();
        int height1 = ui.calculateTabHeight(tabPlacement, 0, fontHeight);
        int height2 = ui.calculateTabHeight(tabPlacement, 1, fontHeight);
        assertEquals(Math.max(height1, height2), ui.calculateMaxTabHeight(tabPlacement));
    }

    public void testCreateLayoutManager() {
        assertTrue(ui.createLayoutManager() instanceof MetalTabbedPaneUI.TabbedPaneLayout);
    }

    public void testGetTabLabelShiftX() {
        assertEquals(0, ui.getTabLabelShiftX(SwingConstants.TOP, 0, true));
        assertEquals(0, ui.getTabLabelShiftX(SwingConstants.TOP, 1, false));
    }

    public void testGetTabLabelShiftY() {
        assertEquals(0, ui.getTabLabelShiftY(SwingConstants.TOP, 0, true));
        assertEquals(0, ui.getTabLabelShiftY(SwingConstants.TOP, 1, false));
    }

    public void testGetTabRunOverlay() {
        assertTrue(ui.getTabRunOverlay(SwingConstants.TOP) >= 0);
        assertTrue(ui.getTabRunOverlay(SwingConstants.LEFT) >= 0);
        assertTrue(ui.getTabRunOverlay(SwingConstants.RIGHT) >= 0);
        assertTrue(ui.getTabRunOverlay(SwingConstants.BOTTOM) >= 0);
    }

    public void testInstallDefaults() {
        ui.minTabWidth = 1;
        ui.selectColor = null;
        ui.selectHighlight = null;
        ui.tabAreaBackground = null;
        ui.installDefaults();
        assertEquals(1, ui.minTabWidth);
        assertEquals(UIManager.get("TabbedPane.selected"), ui.selectColor);
        assertEquals(UIManager.get("TabbedPane.selectHighlight"), ui.selectHighlight);
        assertEquals(UIManager.get("TabbedPane.tabAreaBackground"), ui.tabAreaBackground);
    }

    public void testPaintContentBorderBottomEdge() {
        // Note: painting code, cannot test
    }

    public void testPaintContentBorderLeftEdge() {
        // Note: painting code, cannot test
    }

    public void testPaintContentBorderRightEdge() {
        // Note: painting code, cannot test
    }

    public void testPaintContentBorderTopEdge() {
        // Note: painting code, cannot test
    }

    public void testPaintFocusIndicator() {
        // Note: painting code, cannot test
    }

    public void testPaintTabBackground() {
        // Note: painting code, cannot test
    }

    public void testPaintTabBorder() {
        // Note: painting code, cannot test
    }

    public void testShouldPadTabRun() {
        assertFalse(ui.shouldPadTabRun(SwingConstants.TOP, 0));
        create3TabRuns();
        assertFalse(ui.shouldPadTabRun(SwingConstants.TOP, 2));
        assertTrue(ui.shouldPadTabRun(SwingConstants.TOP, 1));
        assertTrue(ui.shouldPadTabRun(SwingConstants.TOP, 0));
    }

    public void testMetalTabbedPaneUI() {
        assertEquals(40, ui.minTabWidth);
    }

    public void testGetColorForGap() {
        assertEquals(tabbed.getBackground(), ui.getColorForGap(0, 0, 0));
        create3TabRuns();
        tabbed.setBackgroundAt(2, Color.green);
        tabbed.setBackgroundAt(0, Color.red);
        tabbed.setSelectedIndex(3);
        int tabCount = tabbed.getTabCount();
        Rectangle tabBounds = ui.getTabBounds(tabbed, 0);
        assertEquals(tabbed.getBackgroundAt(2), ui.getColorForGap(ui.getRunForTab(tabCount, 0),
                tabBounds.x, tabBounds.y));
        tabbed.setSelectedIndex(2);
        assertEquals(ui.selectColor, ui.getColorForGap(ui.getRunForTab(tabCount, 0),
                tabBounds.x, tabBounds.y));
    }

    public void testPaintHighlightBelowTab() {
        // Note: painting code, cannot test
    }

    public void testPaintBottomTabBorder() {
        // Note: painting code, cannot test
    }

    public void testPaintLeftTabBorder() {
        // Note: painting code, cannot test
    }

    public void testPaintRightTabBorder() {
        // Note: painting code, cannot test
    }

    public void testPaintTopTabBorder() {
        // Note: painting code, cannot test
    }

    public void testShouldFillGap() {
        assertFalse(ui.shouldFillGap(0, 0, 0, 0));
        create3TabRuns();
        int tabCount = tabbed.getTabCount();
        Rectangle tabBounds = ui.getTabBounds(tabbed, 0);
        assertTrue(ui.shouldFillGap(ui.getRunForTab(tabCount, 0), 0, tabBounds.x, tabBounds.y));
        tabBounds = ui.getTabBounds(tabbed, tabCount - 1);
        assertFalse(ui.shouldFillGap(ui.getRunForTab(tabCount, tabCount - 1), tabCount - 1,
                tabBounds.x, tabBounds.y));
    }

    public void testShouldRotateTabRuns() {
        assertFalse(ui.shouldRotateTabRuns(SwingConstants.TOP, 0));
        assertFalse(ui.shouldRotateTabRuns(SwingConstants.TOP, 1));
    }

    private void create3TabRuns() {
        tabbed.add("tab4", new JLabel());
        tabbed.add("tabtabtabtab5", new JLabel());
        tabbed.getLayout().layoutContainer(tabbed);
        assertEquals("initialized incorrectly", 3, tabbed.getTabRunCount());
    }
}
