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

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.SwingTestCase;

public class BasicTabbedPaneUITest extends SwingTestCase {
    private JTabbedPane tabbed;

    private BasicTabbedPaneUI ui;

    private JFrame frame;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tabbed = new JTabbedPane() {
            private static final long serialVersionUID = 1L;

            @Override
            public FontMetrics getFontMetrics(Font f) {
                return BasicTabbedPaneUITest.this.getFontMetrics(f);
            }
        };
        ui = new BasicTabbedPaneUI();
        tabbed.setUI(ui);
        tabbed.addTab("tab1", new JLabel());
        tabbed.setIconAt(0, new ImageIcon());
        tabbed.setDisabledIconAt(0, new ImageIcon());
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

    public BasicTabbedPaneUITest(final String name) {
        super(name);
    }

    /*
     * Class under test for ComponentUI createUI(JComponent)
     */
    public void testCreateUI() {
        ComponentUI ui1 = BasicTabbedPaneUI.createUI(tabbed);
        assertTrue(ui1 instanceof BasicTabbedPaneUI);
        ComponentUI ui2 = BasicTabbedPaneUI.createUI(tabbed);
        assertNotSame(ui1, ui2);
    }

    public void testRotateInsets() {
        Insets insets = new Insets(1, 2, 3, 4);
        Insets rotated = new Insets(0, 0, 0, 0);
        BasicTabbedPaneUI.rotateInsets(insets, rotated, SwingConstants.LEFT);
        assertEquals(new Insets(2, 1, 4, 3), rotated);
        BasicTabbedPaneUI.rotateInsets(insets, rotated, SwingConstants.BOTTOM);
        assertEquals(new Insets(3, 2, 1, 4), rotated);
        BasicTabbedPaneUI.rotateInsets(insets, rotated, SwingConstants.RIGHT);
        assertEquals(new Insets(2, 3, 4, 1), rotated);
    }

    public void testBasicTabbedPaneUI() {
        // does nothing
    }

    public void testAssureRectsCreated() {
        ui.assureRectsCreated(5);
        assertEquals(5, ui.rects.length);
        assertNotNull(ui.rects[4]);
    }

    public void testCalculateMaxTabHeight() {
        int tabPlacement = tabbed.getTabPlacement();
        int fontHeight = tabbed.getFontMetrics(tabbed.getFont()).getHeight();
        int height1 = ui.calculateTabHeight(tabPlacement, 0, fontHeight);
        int height2 = ui.calculateTabHeight(tabPlacement, 1, fontHeight);
        assertEquals(Math.max(height1, height2), ui.calculateMaxTabHeight(tabPlacement));
    }

    public void testCalculateMaxTabWidth() {
        int tabPlacement = tabbed.getTabPlacement();
        FontMetrics fm = tabbed.getFontMetrics(tabbed.getFont());
        int w1 = ui.calculateTabWidth(tabPlacement, 0, fm);
        int w2 = ui.calculateTabWidth(tabPlacement, 1, fm);
        assertEquals(Math.max(w1, w2), ui.calculateMaxTabWidth(tabPlacement));
    }

    public void testCalculateTabAreaHeight() {
        int tabPlacement = SwingConstants.TOP;
        assertEquals(14, ui.calculateTabAreaHeight(tabPlacement, 1, 10));
        assertEquals(22, ui.calculateTabAreaHeight(tabPlacement, 2, 10));
        assertEquals(30, ui.calculateTabAreaHeight(tabPlacement, 3, 10));
    }

    public void testCalculateTabAreaWidth() {
        int tabPlacement = SwingConstants.LEFT;
        assertEquals(14, ui.calculateTabAreaWidth(tabPlacement, 1, 10));
        assertEquals(22, ui.calculateTabAreaWidth(tabPlacement, 2, 10));
        assertEquals(30, ui.calculateTabAreaWidth(tabPlacement, 3, 10));
    }

    public void testCalculateTabHeight() {
        if (!isHarmony()) {
            return;
        }
        tabbed.setSelectedIndex(0);
        assertEquals(27, ui.calculateTabHeight(tabbed.getTabPlacement(), 0, 20));
        tabbed.setSelectedIndex(1);
        assertEquals(27, ui.calculateTabHeight(tabbed.getTabPlacement(), 0, 20));
    }

    public void testCalculateTabWidth() {
        final FontMetrics fm = tabbed.getFontMetrics(tabbed.getFont());
        final int tabIndex = 0;
        final int textWidth = fm.stringWidth(tabbed.getTitleAt(tabIndex));
        assertEquals(textWidth + 24, ui.calculateTabWidth(tabbed.getTabPlacement(), tabIndex,
                fm));
    }

    public void testCreateChangeListener() {
        assertTrue(ui.createChangeListener() instanceof BasicTabbedPaneUI.TabSelectionHandler);
    }

    public void testCreateFocusListener() {
        assertTrue(ui.createFocusListener() instanceof BasicTabbedPaneUI.FocusHandler);
    }

    public void testCreateLayoutManager() {
        assertTrue(ui.createLayoutManager() instanceof BasicTabbedPaneUI.TabbedPaneLayout);
        tabbed.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        assertFalse(ui.createLayoutManager().getClass() == BasicTabbedPaneUI.TabbedPaneLayout.class);
    }

    public void testCreateMouseListener() {
        assertTrue(ui.createMouseListener() instanceof BasicTabbedPaneUI.MouseHandler);
    }

    public void testCreatePropertyChangeListener() {
        PropertyChangeListener l = ui.createPropertyChangeListener();
        assertTrue(l instanceof BasicTabbedPaneUI.PropertyChangeHandler);
    }

    public void testCreateScrollButton() {
        JButton b = ui.createScrollButton(SwingConstants.NORTH);
        assertTrue(b instanceof UIResource);
        assertFalse(b.isFocusable());
        assertTrue(b.getWidth() > 5);
        assertTrue(b.getHeight() > 5);

        try {     
            new BasicTabbedPaneUI() {
                public JButton createScrollButton(int direction) {
                    return super.createScrollButton(direction);
                }
            }.createScrollButton(80);
            
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {    
            // PASSED          
        }
    }

    public void testExpandTabRunsArray() {
        int oldLength = ui.tabRuns.length;
        ui.expandTabRunsArray();
        assertTrue(ui.tabRuns.length > oldLength);
    }

    public void testGetContentBorderInsets() {
        assertEquals(ui.contentBorderInsets, ui.getContentBorderInsets(SwingConstants.TOP));
        assertSame(ui.contentBorderInsets, ui.getContentBorderInsets(SwingConstants.TOP));
        assertEquals(ui.contentBorderInsets, ui.getContentBorderInsets(SwingConstants.LEFT));
        assertEquals(ui.contentBorderInsets, ui.getContentBorderInsets(SwingConstants.BOTTOM));
        assertEquals(ui.contentBorderInsets, ui.getContentBorderInsets(SwingConstants.RIGHT));
    }

    public void testGetFocusIndex() {
        showTabPane();
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .getFocusOwner();
        int expected = focusOwner != tabbed ? -1 : tabbed.getSelectedIndex();
        assertEquals(expected, ui.getFocusIndex());
    }

    public void testGetFontMetrics() {
        tabbed = new JTabbedPane();
        tabbed.setUI(ui);
        assertSame(tabbed.getFontMetrics(tabbed.getFont()), ui.getFontMetrics());
    }

    public void testGetIconForTab() {
        tabbed.setEnabledAt(0, true);
        assertSame(tabbed.getIconAt(0), ui.getIconForTab(0));
        tabbed.setEnabledAt(0, false);
        assertSame(tabbed.getDisabledIconAt(0), ui.getIconForTab(0));
        tabbed.setDisabledIconAt(0, null);
        assertNull(ui.getIconForTab(0));
    }

    public void testGetMaximumSize() {
        assertNull(ui.getMaximumSize(tabbed));
    }

    public void testGetMinimumSize() {
        assertNull(ui.getMinimumSize(tabbed));
    }

    public void testGetNextTabIndex() {
        assertEquals(1, ui.getNextTabIndex(0));
        assertEquals(0, ui.getNextTabIndex(tabbed.getTabCount() - 1));
    }

    public void testGetPreviousTabIndex() {
        assertEquals(tabbed.getTabCount() - 1, ui.getPreviousTabIndex(0));
        assertEquals(0, ui.getPreviousTabIndex(1));
    }

    public void testGetNextTabIndexInRun() {
        create2TabRuns();
        int tabCount = tabbed.getTabCount();
        assertEquals(1, ui.getNextTabIndexInRun(tabCount, 0));
        assertEquals(0, ui.getNextTabIndexInRun(tabCount, 1));
        assertEquals(2, ui.getNextTabIndexInRun(tabCount, 2));
    }

    public void testGetNextTabRun() {
        create3TabRuns();
        assertEquals(1, ui.getNextTabRun(0));
        assertEquals(0, ui.getNextTabRun(2));
    }

    public void testGetPreviousTabIndexInRun() {
        create2TabRuns();
        int tabCount = tabbed.getTabCount();
        assertEquals(1, ui.getPreviousTabIndexInRun(tabCount, 0));
        assertEquals(0, ui.getPreviousTabIndexInRun(tabCount, 1));
        assertEquals(2, ui.getPreviousTabIndexInRun(tabCount, 2));
    }

    public void testGetPreviousTabRun() {
        create3TabRuns();
        assertEquals(2, ui.getPreviousTabRun(0));
        assertEquals(0, ui.getPreviousTabRun(1));
    }

    public void testGetRunForTab() {
        assertEquals(0, ui.getRunForTab(tabbed.getTabCount(), 1));
    }

    public void testGetSelectedTabPadInsets() {
        Insets rotatedInsets = new Insets(0, 0, 0, 0);
        BasicTabbedPaneUI.rotateInsets(ui.selectedTabPadInsets, rotatedInsets,
                SwingConstants.LEFT);
        assertEquals(rotatedInsets, ui.getSelectedTabPadInsets(SwingConstants.LEFT));
        tabbed.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        assertEquals(rotatedInsets, ui.getSelectedTabPadInsets(SwingConstants.LEFT));
    }

    public void testGetTabAreaInsets() {
        ui.tabAreaInsets = new Insets(1, 2, 3, 4);
        assertEquals(ui.tabAreaInsets, ui.getTabAreaInsets(SwingConstants.TOP));
        assertEquals(new Insets(2, 1, 4, 3), ui.getTabAreaInsets(SwingConstants.LEFT));
        assertEquals(new Insets(3, 2, 1, 4), ui.getTabAreaInsets(SwingConstants.BOTTOM));
        assertEquals(new Insets(2, 3, 4, 1), ui.getTabAreaInsets(SwingConstants.RIGHT));
        assertNotSame(ui.tabAreaInsets, ui.getTabAreaInsets(SwingConstants.TOP));
    }

    public void testGetTabBoundsintRectangle() {
        tabbed.setSize(220, 100);
        tabbed.doLayout();
        Rectangle r = new Rectangle();
        Rectangle rc = ui.getTabBounds(0, r);
        assertSame(r, rc);
        assertEquals(ui.rects[0], r);
        assertTrue(r.x >= 0 && r.x <= 10);
        assertTrue(r.y >= 0 && r.y <= 10);
        assertTrue(r.width < 80 && r.width > 0);
        assertTrue(r.height < 50 && r.height > 0);
    }

    public void testGetTabBoundsJTabbedPaneint() {
        tabbed.setSize(220, 100);
        tabbed.doLayout();
        Rectangle r1 = ui.getTabBounds(tabbed, 0);
        Rectangle r2 = new Rectangle();
        ui.getTabBounds(0, r2);
        assertEquals(r2, r1);
    }

    public void testGetTabInsets() {
        tabbed.setSelectedIndex(0);
        assertSame(ui.tabInsets, ui.getTabInsets(SwingConstants.LEFT, 0));
        tabbed.setSelectedIndex(1);
        assertSame(ui.tabInsets, ui.getTabInsets(SwingConstants.LEFT, 0));
        assertSame(ui.tabInsets, ui.getTabInsets(SwingConstants.TOP, 1));
        assertSame(ui.tabInsets, ui.getTabInsets(SwingConstants.RIGHT, 1));
        assertSame(ui.tabInsets, ui.getTabInsets(SwingConstants.BOTTOM, 1));
    }

    public void testGetTabLabelShiftX() {
        if (!isHarmony()) {
            return;
        }
        tabbed.setSelectedIndex(0);
        tabbed.doLayout();
        assertEquals(-1, ui.getTabLabelShiftX(SwingConstants.TOP, 0, true));
        assertEquals(-1, ui.getTabLabelShiftX(SwingConstants.LEFT, 0, true));
        assertEquals(1, ui.getTabLabelShiftX(SwingConstants.RIGHT, 0, true));
        assertEquals(-1, ui.getTabLabelShiftX(SwingConstants.BOTTOM, 0, true));
        assertEquals(-1, ui.getTabLabelShiftX(SwingConstants.TOP, 0, false));
        assertEquals(1, ui.getTabLabelShiftX(SwingConstants.LEFT, 0, false));
        assertEquals(-1, ui.getTabLabelShiftX(SwingConstants.RIGHT, 0, false));
        assertEquals(-1, ui.getTabLabelShiftX(SwingConstants.BOTTOM, 0, false));
    }

    public void testGetTabLabelShiftY() {
        assertEquals(-1, ui.getTabLabelShiftY(SwingConstants.TOP, 0, true));
        assertEquals(1, ui.getTabLabelShiftY(SwingConstants.LEFT, 0, true));
        assertEquals(1, ui.getTabLabelShiftY(SwingConstants.RIGHT, 0, true));
        assertEquals(1, ui.getTabLabelShiftY(SwingConstants.BOTTOM, 0, true));
        assertEquals(1, ui.getTabLabelShiftY(SwingConstants.TOP, 0, false));
        assertEquals(1, ui.getTabLabelShiftY(SwingConstants.LEFT, 0, false));
        assertEquals(1, ui.getTabLabelShiftY(SwingConstants.RIGHT, 0, false));
        assertEquals(-1, ui.getTabLabelShiftY(SwingConstants.BOTTOM, 0, false));
    }

    /*
     * Class under test for int getTabRunCount(JTabbedPane)
     */
    public void testGetTabRunCount() {
        create2TabRuns();
        assertEquals(2, ui.getTabRunCount(tabbed));
        tabbed.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        assertEquals(1, ui.getTabRunCount(tabbed));
    }

    public void testGetTabRunIndent() {
        assertEquals(0, ui.getTabRunIndent(SwingConstants.LEFT, 0));
        assertEquals(0, ui.getTabRunIndent(SwingConstants.TOP, 1));
        assertEquals(0, ui.getTabRunIndent(SwingConstants.BOTTOM, 1));
        assertEquals(0, ui.getTabRunIndent(SwingConstants.RIGHT, 1));
    }

    public void testGetTabRunOffset() {
        // the documentation is empty
    }

    public void testGetTabRunOverlay() {
        assertEquals(ui.tabRunOverlay, ui.getTabRunOverlay(SwingConstants.LEFT));
        assertEquals(ui.tabRunOverlay, ui.getTabRunOverlay(SwingConstants.TOP));
    }

    public void testGetTextViewForTab() {
        assertNull(ui.getTextViewForTab(0));
        //TODO HTML styled text is not supported
    }

    public void testSetGetVisibleComponent() {
        assertSame(tabbed.getSelectedComponent(), ui.getVisibleComponent());
        assertTrue(ui.getVisibleComponent().isVisible());
        JComponent comp = new JLabel("label");
        Component oldVisible = ui.getVisibleComponent();
        ui.setVisibleComponent(comp);
        assertSame(comp, ui.getVisibleComponent());
        assertNotSame(comp, tabbed.getSelectedComponent());
        assertFalse(oldVisible.isVisible());
        assertTrue(comp.isVisible());
        assertFalse(tabbed.isAncestorOf(comp));
        ui.setVisibleComponent(null);
        assertNull(ui.getVisibleComponent());
    }

    public void testInstallUninstallComponents() {
        int count = tabbed.getComponentCount();
        ui.uninstallComponents();
        assertEquals(count, tabbed.getComponentCount());
        ui.installComponents();
        assertEquals(count, tabbed.getComponentCount());
        tabbed.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        ui.uninstallComponents();
        count = tabbed.getComponentCount();
        ui.installComponents();
        if (isHarmony()) {
            assertEquals(count + 2, tabbed.getComponentCount());
        }
        ui.uninstallComponents();
        assertEquals(count, tabbed.getComponentCount());
    }

    public void testInstallDefaults() {
        tabbed.setBackgroundAt(0, null);
        tabbed.setBackground(null);
        tabbed.setForeground(null);
        tabbed.setLayout(null);
        tabbed.setFont(null);
        ui.installDefaults();
        assertTrue(tabbed.getBackground() instanceof UIResource);
        assertTrue(tabbed.getForeground() instanceof UIResource);
        assertTrue(tabbed.getFont() instanceof UIResource);
        assertTrue(tabbed.getBackgroundAt(0) instanceof UIResource);
        assertNull(tabbed.getLayout());
    }

    public void testUninstallDefaults() {
        // nothing to test
    }

    public void testInstallUninstallKeyboardActions() {
        ui.uninstallKeyboardActions();
        ui.installKeyboardActions();
        assertSame(UIManager.get("TabbedPane.focusInputMap"), SwingUtilities.getUIInputMap(
                tabbed, JComponent.WHEN_FOCUSED));
        assertSame(UIManager.get("TabbedPane.ancestorInputMap"), SwingUtilities.getUIInputMap(
                tabbed, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
        assertNotNull(SwingUtilities.getUIActionMap(tabbed));
        ui.uninstallKeyboardActions();
        assertNull(SwingUtilities.getUIInputMap(tabbed, JComponent.WHEN_FOCUSED));
        assertNull(SwingUtilities.getUIInputMap(tabbed,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
        assertNull(SwingUtilities.getUIActionMap(tabbed));
    }

    public void testInstallUninstallListeners() {
        ui.uninstallListeners();
        ui.installListeners();
        assertTrue(Arrays.asList(tabbed.getPropertyChangeListeners()).contains(
                ui.propertyChangeListener));
        assertTrue(Arrays.asList(tabbed.getMouseListeners()).contains(ui.mouseListener));
        assertTrue(Arrays.asList(tabbed.getFocusListeners()).contains(ui.focusListener));
        assertTrue(Arrays.asList(tabbed.getChangeListeners()).contains(ui.tabChangeListener));
        ui.uninstallListeners();
        assertFalse(Arrays.asList(tabbed.getPropertyChangeListeners()).contains(
                ui.propertyChangeListener));
        assertFalse(Arrays.asList(tabbed.getMouseListeners()).contains(ui.mouseListener));
        assertFalse(Arrays.asList(tabbed.getFocusListeners()).contains(ui.focusListener));
        assertFalse(Arrays.asList(tabbed.getChangeListeners()).contains(ui.tabChangeListener));
    }

    public void testInstallUI() {
        tabbed.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        ui.uninstallListeners();
        ui.uninstallComponents();
        ui.uninstallKeyboardActions();
        int compCount = tabbed.getComponentCount();
        tabbed.setLayout(null);
        tabbed.setBackground(null);
        ui.installUI(tabbed);
        assertNotNull(tabbed.getLayout());
        assertTrue("called installDefaults()", tabbed.getBackground() instanceof UIResource);
        assertTrue("called installComponents()", tabbed.getComponentCount() > compCount);
        if (isHarmony()) {
            assertTrue("called installListeners()", Arrays.asList(tabbed.getMouseListeners())
                    .contains(ui.mouseListener));
        }
        assertSame("called installKeyboardActions()",
                UIManager.get("TabbedPane.focusInputMap"), SwingUtilities.getUIInputMap(tabbed,
                        JComponent.WHEN_FOCUSED));
    }

    public void testUninstallUI() {
        tabbed.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        int compCount = tabbed.getComponentCount();
        ui.uninstallUI(tabbed);
        assertNull(tabbed.getLayout());
        assertTrue("called uninstallComponents()", tabbed.getComponentCount() < compCount);
        assertFalse("called uninstallListeners()", Arrays.asList(tabbed.getMouseListeners())
                .contains(ui.mouseListener));
        assertNull("called uninstallKeyboardActions()", SwingUtilities.getUIInputMap(tabbed,
                JComponent.WHEN_FOCUSED));
    }

    public void testLastTabInRun() {
        int lastTabIndex = tabbed.getTabCount() - 1;
        assertEquals(lastTabIndex, ui.lastTabInRun(tabbed.getTabCount(), 0));
    }

    public void testLayoutLabel() {
        // no documentation
    }

    public void testNavigateSelectedTab() {
        create2TabRuns();
        tabbed.setSelectedIndex(0);
        ui.navigateSelectedTab(SwingConstants.EAST);
        assertEquals(ui.getNextTabIndexInRun(tabbed.getTabCount(), 0), tabbed
                .getSelectedIndex());
        tabbed.setEnabledAt(0, false);
        ui.navigateSelectedTab(SwingConstants.WEST);
        assertEquals(1, tabbed.getSelectedIndex());
        tabbed.setEnabledAt(0, true);
        ui.navigateSelectedTab(SwingConstants.NORTH);
        assertEquals(2, tabbed.getSelectedIndex());
        if (isHarmony()) {
            ui.navigateSelectedTab(SwingConstants.SOUTH);
            assertEquals(1, tabbed.getSelectedIndex());
        }
    }

    public void testPaintGraphicsJComponent() {
        BasicTabbedPaneUI localBasicTabbedPaneUI = new BasicTabbedPaneUI(); 
        localBasicTabbedPaneUI.installUI(new JTabbedPane());
        try { 
            localBasicTabbedPaneUI.paint(null, new JToolBar()); 
            fail("NPE is not thrown"); 
        } catch (NullPointerException e) {
            // PASSED
        } 
    }

    public void testPaintContentBorder() {
        // Note: painting code, cannot test
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

    public void testPaintIcon() {
        // Note: painting code, cannot test
    }

    public void testPaintTab() {
        // Note: painting code, cannot test
    }

    public void testPaintTabArea() {
        // Note: painting code, cannot test
    }

    public void testPaintTabBackground() {
        // Note: painting code, cannot test
    }

    public void testPaintTabBorder() {
        // Note: painting code, cannot test
    }

    public void testPaintText() {
        // Note: painting code, cannot test
    }

    public void testSelectAdjacentRunTab() {
        create3TabRuns();
        tabbed.setSelectedIndex(0);
        int tabRunOffset = ui.getTabRunOffset(tabbed.getTabPlacement(), tabbed.getTabCount(),
                0, true);
        ui.selectAdjacentRunTab(tabbed.getTabPlacement(), 0, tabRunOffset);
        assertEquals(3, tabbed.getSelectedIndex());
    }

    public void testSelectNextTab() {
        create2TabRuns();
        int initIndex = 0;
        tabbed.setSelectedIndex(initIndex);
        ui.selectNextTab(initIndex);
        assertEquals(ui.getNextTabIndex(initIndex), tabbed.getSelectedIndex());
    }

    public void testSelectPreviousTab() {
        create2TabRuns();
        int initIndex = 0;
        tabbed.setSelectedIndex(initIndex);
        ui.selectPreviousTab(initIndex);
        assertEquals(ui.getPreviousTabIndex(initIndex), tabbed.getSelectedIndex());
    }

    public void testSelectNextTabInRun() {
        create2TabRuns();
        int initIndex = 0;
        tabbed.setSelectedIndex(initIndex);
        ui.selectNextTabInRun(initIndex);
        assertEquals(ui.getNextTabIndexInRun(tabbed.getTabCount(), initIndex), tabbed
                .getSelectedIndex());
    }

    public void testSelectPreviousTabInRun() {
        create2TabRuns();
        int initIndex = 0;
        tabbed.setSelectedIndex(initIndex);
        ui.selectPreviousTabInRun(initIndex);
        assertEquals(ui.getPreviousTabIndexInRun(tabbed.getTabCount(), initIndex), tabbed
                .getSelectedIndex());
    }

    public void testSetGetRolloverTab() {
        assertEquals(-1, ui.getRolloverTab());
        ui.setRolloverTab(1);
        assertEquals(1, ui.getRolloverTab());
        ui.setRolloverTab(5);
        assertEquals(5, ui.getRolloverTab());
    }

    public void testShouldPadTabRun() {
        assertFalse(ui.shouldPadTabRun(tabbed.getTabPlacement(), 0));
        create2TabRuns();
        assertTrue(ui.shouldPadTabRun(tabbed.getTabPlacement(), 0));
        assertTrue(ui.shouldPadTabRun(tabbed.getTabPlacement(), 1));
        assertTrue(ui.shouldPadTabRun(tabbed.getTabPlacement(), 2));
    }

    public void testShouldRotateTabRuns() {
        assertTrue(ui.shouldRotateTabRuns(tabbed.getTabPlacement()));
    }

    /*
     * Class under test for int tabForCoordinate(JTabbedPane, int, int)
     */
    public void testTabForCoordinate() {
        tabbed.setSize(40, 30);
        tabbed.doLayout();
        Rectangle tabBounds = ui.getTabBounds(tabbed, 0);
        assertEquals(0, ui.tabForCoordinate(null, tabBounds.x, tabBounds.y));
        assertEquals(-1, ui.tabForCoordinate(null, tabBounds.x - 1, tabBounds.y - 1));
        tabBounds = ui.getTabBounds(tabbed, 1);
        assertEquals(1, ui.tabForCoordinate(null, tabBounds.x + 1, tabBounds.y + 1));
    }

    private void create2TabRuns() {
        tabbed.addTab("tab3", new JLabel());
        FontMetrics fm = ui.getFontMetrics();
        tabbed.setSize(ui.calculateTabWidth(tabbed.getTabPlacement(), 0, fm)
                + ui.calculateTabWidth(tabbed.getTabPlacement(), 1, fm) + 10, 100);
        tabbed.getLayout().layoutContainer(tabbed);
        assertEquals("initialized incorrectly", 2, tabbed.getTabRunCount());
    }

    private void create3TabRuns() {
        tabbed.add("tab4", new JLabel());
        tabbed.add("tabtabtabtab5", new JLabel());
        tabbed.getLayout().layoutContainer(tabbed);
        assertEquals("initialized incorrectly", 3, tabbed.getTabRunCount());
    }

    private void showTabPane() {
        frame = new JFrame();
        frame.getContentPane().add(tabbed);
        frame.pack();
        frame.setVisible(true);
    }
}
