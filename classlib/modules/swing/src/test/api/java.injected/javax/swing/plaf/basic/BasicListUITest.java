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
 * @author Anton Avtamonov
 */
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.SwingTestCase;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

public class BasicListUITest extends SwingTestCase {
    private JList list;

    private BasicListUI ui;

    private Object defaultBackground;

    private Object defaultForeground;

    private Object defaultFont;

    private Object defaultRenderer;

    private Object defaultBorder;

    public BasicListUITest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ui = new BasicListUI();
        list = new JList();
        defaultBackground = UIManager.get("List.background");
        defaultForeground = UIManager.get("List.foreground");
        defaultFont = UIManager.get("List.font");
        defaultRenderer = UIManager.get("List.cellRenderer");
        defaultBorder = UIManager.get("List.border");
    }

    @Override
    protected void tearDown() throws Exception {
        ui = null;
        list = null;
        UIManager.put("List.background", defaultBackground);
        UIManager.put("List.foreground", defaultForeground);
        UIManager.put("List.font", defaultFont);
        UIManager.put("List.cellRenderer", defaultRenderer);
        UIManager.put("List.border", defaultBorder);
        super.tearDown();
    }

    public void testBasicListUI() throws Exception {
        assertEquals(-1, ui.cellHeight);
        assertEquals(-1, ui.cellWidth);
        assertNull(ui.cellHeights);
        assertNull(ui.rendererPane);
        assertNull(ui.list);
        assertEquals(1, ui.updateLayoutStateNeeded);
    }

    public void testBasicListUI_FocusListener() throws Exception {
        assertNull(ui.focusListener);
        int listenersCount = list.getFocusListeners().length;
        ui.installUI(list);
        if (isHarmony()) {
            assertTrue(ui.focusListener instanceof BasicListUI.FocusHandler);
        }
        assertTrue(Arrays.asList(list.getFocusListeners()).contains(ui.focusListener));
        assertEquals(listenersCount + 1, list.getFocusListeners().length);
        ui.uninstallUI(list);
        assertNull(ui.focusListener);
        assertEquals(listenersCount, list.getFocusListeners().length);
    }

    public void testBasicListUI_ListDataHandler() throws Exception {
    }

    public void testConvertRowToY() throws Exception {
        ui.installUI(list);
        assertEquals(-1, ui.convertRowToY(0));
        assertEquals(-1, ui.convertRowToY(-1));
        list.setListData(new Object[] { "a", "b" });
        ui.installUI(list);
        list.setFixedCellHeight(20);
        ui.maybeUpdateLayoutState();
        assertEquals(-1, ui.convertRowToY(-1));
        assertEquals(0, ui.convertRowToY(0));
        assertEquals(20, ui.convertRowToY(1));
    }

    public void testConvertYToRow() throws Exception {
        ui.installUI(list);
        assertEquals(-1, ui.convertYToRow(0));
        list.setListData(new Object[] { "a", "b" });
        list.setFixedCellHeight(10);
        ui.maybeUpdateLayoutState();
        assertEquals(0, ui.convertYToRow(0));
        assertEquals(0, ui.convertYToRow(5));
        assertEquals(1, ui.convertYToRow(11));
        assertEquals(1, ui.convertYToRow(19));
    }

    public void testCreateFocusListener() throws Exception {
        if (isHarmony()) {
            assertTrue(ui.createFocusListener() instanceof BasicListUI.FocusHandler);
        }
    }

    public void testCreateListDataListener() throws Exception {
        if (isHarmony()) {
            assertTrue(ui.createListDataListener() instanceof BasicListUI.ListDataHandler);
        }
    }

    public void testCreateListSelectionListener() throws Exception {
        if (isHarmony()) {
            assertTrue(ui.createListSelectionListener() instanceof BasicListUI.ListSelectionHandler);
        }
    }

    public void testCreateMouseInputListener() throws Exception {
        if (isHarmony()) {
            assertTrue(ui.createMouseInputListener() instanceof BasicListUI.MouseInputHandler);
        }
    }

    public void testCreatePropertyChangeListener() throws Exception {
        if (isHarmony()) {
            assertTrue(ui.createPropertyChangeListener() instanceof BasicListUI.PropertyChangeHandler);
        }
    }

    public void testCreateUI() throws Exception {
        BasicListUI newUI1 = (BasicListUI) BasicListUI.createUI(list);
        BasicListUI newUI2 = (BasicListUI) BasicListUI.createUI(list);
        assertNotSame(newUI1, newUI2);
    }

    public void testGetCellBounds() throws Exception {
        ui.installUI(list);
        assertNull(ui.getCellBounds(list, 0, 0));
        assertNull(ui.getCellBounds(list, -1, -1));
        list.setListData(new Object[] { "a", "b" });
        list.setFixedCellHeight(10);
        list.setFixedCellWidth(20);
        list.setSize(100, 100);
        assertEquals(new Rectangle(0, 0, 100, 10), ui.getCellBounds(list, 0, 0));
        assertEquals(new Rectangle(0, 10, 100, 10), ui.getCellBounds(list, 1, 1));
        assertEquals(new Rectangle(0, 0, 100, 20), ui.getCellBounds(list, 0, 1));
        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        assertEquals(new Rectangle(0, 0, 20, 10), ui.getCellBounds(list, 0, 0));
        assertEquals(new Rectangle(0, 10, 20, 10), ui.getCellBounds(list, 1, 1));
        assertEquals(new Rectangle(0, 0, 20, 20), ui.getCellBounds(list, 1, 0));
        list.setBorder(BorderFactory.createEmptyBorder(10, 5, 20, 7));
        assertEquals(new Rectangle(5, 10, 20, 10), ui.getCellBounds(list, 0, 0));
        list.setLayoutOrientation(JList.VERTICAL);
        assertEquals(new Rectangle(5, 10, 100 - 5 - 7, 20), ui.getCellBounds(list, 0, 1));
    }

    public void testGetCellBounds_Null() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                ui.getCellBounds(null, -1, 9);
            }
        });
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                ui.getCellBounds(null, 1, 9);
            }
        });
    }

    public void testGetMaximumSize() throws Exception {
        ui.installUI(list);
        assertEquals(ui.getPreferredSize(list), ui.getMaximumSize(list));
    }

    public void testGetMinimumSize() throws Exception {
        ui.installUI(list);
        assertEquals(ui.getPreferredSize(list), ui.getMinimumSize(list));
    }

    public void testGetPreferredSize() throws Exception {
        ui.installUI(list);
        list.setSize(100, 100);
        assertEquals(new Dimension(0, 0), ui.getPreferredSize(list));
        list.setListData(new Object[] { "a", "bbb" });
        Component renderer = new DefaultListCellRenderer().getListCellRendererComponent(list,
                "bbb", 1, false, false);
        assertEquals(new Dimension(renderer.getPreferredSize().width, 2 * renderer
                .getPreferredSize().height), ui.getPreferredSize(list));
        list.setFixedCellHeight(20);
        list.setFixedCellWidth(30);
        assertEquals(new Dimension(30, 40), ui.getPreferredSize(list));
        list.setVisibleRowCount(1);
        assertEquals(new Dimension(30, 40), ui.getPreferredSize(list));
        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        assertEquals(new Dimension(60, 20), ui.getPreferredSize(list));
    }

    public void testGetRowHeight() throws Exception {
        assertEquals(-1, ui.getRowHeight(-1));
        ui.installUI(list);
        assertEquals(-1, ui.getRowHeight(0));
        list.setListData(new Object[] { "a", "bbb" });
        ui.maybeUpdateLayoutState();
        Component renderer = new DefaultListCellRenderer().getListCellRendererComponent(list,
                "bbb", 1, false, false);
        assertEquals(renderer.getPreferredSize().height, ui.getRowHeight(0));
        assertEquals(renderer.getPreferredSize().height, ui.getRowHeight(1));
        assertEquals(-1, ui.getRowHeight(2));
        list.setFixedCellHeight(30);
        ui.maybeUpdateLayoutState();
        assertEquals(30, ui.getRowHeight(0));
    }

    public void testIndexToLocation() throws Exception {
        ui.installUI(list);
        assertNull(ui.indexToLocation(list, -1));
        assertNull(ui.indexToLocation(list, 0));
        list.setListData(new Object[] { "a", "bbb" });
        Component renderer = new DefaultListCellRenderer().getListCellRendererComponent(list,
                "bbb", 1, false, false);
        assertEquals(new Point(0, 0), ui.indexToLocation(list, 0));
        assertEquals(new Point(0, renderer.getPreferredSize().height), ui.indexToLocation(list,
                1));
        list.setVisibleRowCount(1);
        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        assertEquals(new Point(0, 0), ui.indexToLocation(list, 0));
        assertEquals(new Point(renderer.getPreferredSize().width, 0), ui.indexToLocation(list,
                1));
        assertNull(ui.indexToLocation(list, 2));
    }

    public void testInstallDefaults() throws Exception {
        UIManager.getDefaults().put("List.background", new ColorUIResource(Color.red));
        UIManager.getDefaults().put("List.foreground", new ColorUIResource(Color.yellow));
        Font font = new FontUIResource(list.getFont().deriveFont(100f));
        UIManager.getDefaults().put("List.font", font);
        DefaultListCellRenderer renderer = new DefaultListCellRenderer.UIResource();
        UIManager.getDefaults().put("List.cellRenderer", renderer);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.getDefaults().put("List.border", border);
        list.setUI(ui);
        ui.installDefaults();
        assertEquals(Color.red, list.getBackground());
        assertEquals(Color.yellow, list.getForeground());
        assertEquals(font, list.getFont());
        assertEquals(renderer, list.getCellRenderer());
        assertEquals(border, list.getBorder());
    }

    public void testUninstallDefaults() throws Exception {
        UIManager.getDefaults().put("List.background", new ColorUIResource(Color.red));
        UIManager.getDefaults().put("List.foreground", new ColorUIResource(Color.yellow));
        Font font = new FontUIResource(list.getFont().deriveFont(100f));
        UIManager.getDefaults().put("List.font", font);
        DefaultListCellRenderer renderer = new DefaultListCellRenderer.UIResource();
        UIManager.getDefaults().put("List.cellRenderer", renderer);
        Border border = new BorderUIResource(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.getDefaults().put("List.border", border);
        list.setUI(ui);
        ui.installDefaults();
        ui.uninstallDefaults();
        assertNull(list.getBackground());
        assertNull(list.getForeground());
        assertNull(list.getFont());
        assertNull(list.getCellRenderer());
        assertNull(list.getBorder());
        UIManager.getDefaults().put("List.background", Color.red);
        list.setUI(ui);
        ui.uninstallDefaults();
        if (isHarmony()) {
            assertNull(list.getBackground());
        }
        assertNull(list.getForeground());
        assertNull(list.getFont());
        assertNull(list.getCellRenderer());
        assertNull(list.getBorder());
    }

    public void testInstallKeyboardActions() throws Exception {
        list.setUI(ui);
        assertNotNull(SwingUtilities.getUIInputMap(list, JComponent.WHEN_FOCUSED));
    }

    public void testUninstallKeyboardActions() throws Exception {
        list.setUI(ui);
        ui.uninstallKeyboardActions();
        assertNull(SwingUtilities.getUIInputMap(list, JComponent.WHEN_FOCUSED));
    }

    public void testInstallListeners() throws Exception {
        list.setUI(ui);
        assertNotNull(ui.focusListener);
        assertTrue(list.getFocusListeners().length > 0);
        assertNotNull(ui.listDataListener);
        assertNotNull(ui.mouseInputListener);
        assertTrue(list.getMouseListeners().length > 0);
        assertTrue(list.getMouseMotionListeners().length > 0);
        assertNotNull(ui.propertyChangeListener);
        assertTrue(list.getPropertyChangeListeners().length > 0);
    }

    public void testUninstallListeners() throws Exception {
        list.setUI(ui);
        int focusListenersCount = list.getFocusListeners().length;
        int mouseListenersCount = list.getMouseListeners().length;
        int mouseMotionListenersCount = list.getMouseMotionListeners().length;
        int propertyChangeListenersCount = list.getPropertyChangeListeners().length;
        ui.uninstallListeners();
        assertNull(ui.focusListener);
        assertTrue(focusListenersCount > list.getFocusListeners().length);
        assertNull(ui.listDataListener);
        assertNull(ui.mouseInputListener);
        assertTrue(mouseListenersCount > list.getMouseListeners().length);
        assertTrue(mouseMotionListenersCount > list.getMouseMotionListeners().length);
        assertNull(ui.propertyChangeListener);
        assertTrue(propertyChangeListenersCount > list.getPropertyChangeListeners().length);
    }

    public void testInstallUI() throws Exception {
        ui.installUI(list);
        assertNotNull(list.getBackground());
        assertNotNull(SwingUtilities.getUIInputMap(list, JComponent.WHEN_FOCUSED));
        assertNotNull(ui.rendererPane);
        assertFalse(ui.rendererPane.isVisible());
        assertEquals(2, list.getComponentCount());
    }

    public void testUninstallUI() throws Exception {
        list.setUI(ui);
        ui.uninstallUI(list);
        if (isHarmony()) {
            assertNull(list.getBackground());
        }
        assertNull(SwingUtilities.getUIInputMap(list, JComponent.WHEN_FOCUSED));
        assertEquals(0, list.getComponentCount());
    }

    public void testLocationToIndex() throws Exception {
        ui.installUI(list);
        assertEquals(-1, ui.locationToIndex(list, new Point(3, 3)));
        list.setListData(new Object[] { "aa", "bb" });
        assertEquals(0, ui.locationToIndex(list, new Point(3, 3)));
        assertEquals(1, ui.locationToIndex(list, new Point(3, 25)));
        assertEquals(0, ui.locationToIndex(list, new Point(70, 3)));
        assertEquals(1, ui.locationToIndex(list, new Point(70, 25)));
        list.setVisibleRowCount(1);
        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        assertEquals(0, ui.locationToIndex(list, new Point(3, 3)));
        assertEquals(0, ui.locationToIndex(list, new Point(3, 25)));
        assertEquals(1, ui.locationToIndex(list, new Point(50, 3)));
        assertEquals(1, ui.locationToIndex(list, new Point(50, 25)));

        try {     
            BasicListUI localBasicListUI = new BasicListUI();
            javax.swing.JList localJList = new javax.swing.JList();
            localBasicListUI.locationToIndex(localJList, null); 
            fail("NPE should be thrown");
        } catch (NullPointerException npe) {    
            // PASSED          
        }
    }

    public void testMaybeUpdateLayoutState() throws Exception {
        ui.installUI(list);
        assertTrue(ui.updateLayoutStateNeeded != 0);
        ui.maybeUpdateLayoutState();
        assertEquals(0, ui.updateLayoutStateNeeded);
    }

    //TODO
    public void testPaint() throws Exception {
    }

    //TODO
    public void testPaintCell() throws Exception {
    }

    public void testUpdateLayoutState() throws Exception {
        ui.installUI(list);
        assertEquals(-1, ui.cellHeight);
        assertEquals(-1, ui.cellWidth);
        assertNull(ui.cellHeights);
        list.setSize(100, 100);
        ui.maybeUpdateLayoutState();
        assertEquals(-1, ui.cellHeight);
        assertEquals(-1, ui.cellWidth);
        assertNotNull(ui.cellHeights);
        assertEquals(0, ui.cellHeights.length);
        list.setListData(new Object[] { "aa" });
        ui.maybeUpdateLayoutState();
        assertEquals(-1, ui.cellHeight);
        assertTrue(ui.cellWidth > 0);
        assertNotNull(ui.cellHeights);
        assertEquals(1, ui.cellHeights.length);
        assertTrue(ui.cellHeights[0] > 0);
        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        ui.maybeUpdateLayoutState();
        assertTrue(ui.cellHeight > 0);
        assertTrue(ui.cellWidth > 0);
        assertNull(ui.cellHeights);
    }

    public void testSelectNextPreviousIndex() throws Exception {
        ui.installUI(list);
        list.setListData(new Object[] { "1", "2", "3" });
        assertTrue(list.isSelectionEmpty());
        list.setSelectedIndex(0);
        ui.selectNextIndex();
        assertFalse(list.isSelectedIndex(0));
        assertTrue(list.isSelectedIndex(1));
        assertFalse(list.isSelectedIndex(2));
        ui.selectNextIndex();
        assertFalse(list.isSelectedIndex(0));
        assertFalse(list.isSelectedIndex(1));
        assertTrue(list.isSelectedIndex(2));
        ui.selectNextIndex();
        assertFalse(list.isSelectedIndex(0));
        assertFalse(list.isSelectedIndex(1));
        assertTrue(list.isSelectedIndex(2));
        ui.selectPreviousIndex();
        assertFalse(list.isSelectedIndex(0));
        assertTrue(list.isSelectedIndex(1));
        assertFalse(list.isSelectedIndex(2));
        ui.selectPreviousIndex();
        assertTrue(list.isSelectedIndex(0));
        assertFalse(list.isSelectedIndex(1));
        assertFalse(list.isSelectedIndex(2));
        ui.selectPreviousIndex();
        assertTrue(list.isSelectedIndex(0));
        assertFalse(list.isSelectedIndex(1));
        assertFalse(list.isSelectedIndex(2));
        list.clearSelection();
        ui.selectNextIndex();
        assertTrue(list.isSelectedIndex(0));
        assertFalse(list.isSelectedIndex(1));
        assertFalse(list.isSelectedIndex(2));
        list.clearSelection();
        ui.selectPreviousIndex();
        assertTrue(list.isSelectionEmpty());
        list.setSelectionInterval(0, 1);
        ui.selectNextIndex();
        assertFalse(list.isSelectedIndex(0));
        assertTrue(list.isSelectedIndex(1));
        assertFalse(list.isSelectedIndex(2));
        list.setSelectionInterval(1, 0);
        ui.selectNextIndex();
        assertFalse(list.isSelectedIndex(0));
        assertTrue(list.isSelectedIndex(1));
        assertFalse(list.isSelectedIndex(2));
        list.setSelectedIndex(1);
        list.setSelectionInterval(0, 0);
        ui.selectNextIndex();
        assertFalse(list.isSelectedIndex(0));
        assertTrue(list.isSelectedIndex(1));
        assertFalse(list.isSelectedIndex(2));
        list.setSelectionInterval(0, 1);
        ui.selectPreviousIndex();
        assertTrue(list.isSelectedIndex(0));
        assertTrue(list.isSelectedIndex(1));
        assertFalse(list.isSelectedIndex(2));
        list.setSelectionInterval(2, 1);
        ui.selectPreviousIndex();
        assertTrue(list.isSelectedIndex(0));
        assertFalse(list.isSelectedIndex(1));
        assertFalse(list.isSelectedIndex(2));
    }
    
    /**
     * Regression test for HARMONY-2653 
     * */
    public void testRGetPreferredSize() { 
        try { 
            BasicListUI bl = new BasicListUI(); 
            bl.getPreferredSize(new JFileChooser() ); 
            fail("No NPE thrown"); 
        } catch (NullPointerException e) { 
            //expected 
        }
    } 

}
