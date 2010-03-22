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
package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ListUI;
import javax.swing.text.Position;

public class JListTest extends SwingTestCase {
    private JList list;

    private TestPropertyChangeListener changeListener;

    private JFrame frame;

    public JListTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        list = new JList(new Object[] { "a", "b", "c" });
        list.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public FontMetrics getFontMetrics(Font f) {
                return JListTest.this.getFontMetrics(f, 10, 10);
            }
        });
        changeListener = new TestPropertyChangeListener();
        list.addPropertyChangeListener(changeListener);
        frame = new JFrame();
    }

    @Override
    protected void tearDown() throws Exception {
        list = null;
        changeListener = null;
        frame = null;
        super.tearDown();
    }

    public void testJList() throws Exception {
        list = new JList();
        assertNotNull(list.getModel());
        assertTrue(list.getSelectionModel() instanceof DefaultListSelectionModel);
        assertNotNull(list.getUI());
        assertTrue(list.isOpaque());
        ListModel testModel = new ListModel() {
            public void addListDataListener(final ListDataListener l) {
            }

            public Object getElementAt(final int index) {
                return null;
            }

            public int getSize() {
                return 0;
            }

            public void removeListDataListener(final ListDataListener l) {
            }
        };
        list = new JList(testModel);
        assertEquals(testModel, list.getModel());
        assertTrue(list.getSelectionModel() instanceof DefaultListSelectionModel);
        assertNotNull(list.getUI());
        list = new JList(new Object[] { "a", "b" });
        assertNotNull(list.getModel());
        assertTrue(list.getSelectionModel() instanceof DefaultListSelectionModel);
        assertNotNull(list.getUI());
        list = new JList(new Vector<Object>());
        assertNotNull(list.getModel());
        assertTrue(list.getSelectionModel() instanceof DefaultListSelectionModel);
        assertNotNull(list.getUI());
    }

    public void testAddGetRemoveListSelectionListener() throws Exception {
        assertEquals(0, list.getListSelectionListeners().length);
        TestListSelectionListener l = new TestListSelectionListener();
        list.addListSelectionListener(l);
        list.addListSelectionListener(new TestListSelectionListener());
        list.addListSelectionListener(new TestListSelectionListener());
        assertEquals(3, list.getListSelectionListeners().length);
        list.removeListSelectionListener(l);
        assertEquals(2, list.getListSelectionListeners().length);
    }

    public void testAddSelectionInterval() throws Exception {
        assertTrue(list.isSelectionEmpty());
        assertTrue(list.getSelectionModel().isSelectionEmpty());
        list.addSelectionInterval(1, 2);
        assertFalse(list.isSelectionEmpty());
        assertFalse(list.getSelectionModel().isSelectionEmpty());
        try {
            // Regression for HARMONY-1965
            list.addSelectionInterval(1, -2);
            fail("IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            // Regression for HARMONY-1965
            list.addSelectionInterval(-2, 2);
            fail("IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testClearSelection() throws Exception {
        assertTrue(list.isSelectionEmpty());
        assertTrue(list.getSelectionModel().isSelectionEmpty());
        list.setSelectedIndex(1);
        assertFalse(list.isSelectionEmpty());
        assertFalse(list.getSelectionModel().isSelectionEmpty());
        list.clearSelection();
        assertTrue(list.isSelectionEmpty());
        assertTrue(list.getSelectionModel().isSelectionEmpty());
    }

    public void testCreateSelectionModel() throws Exception {
        assertTrue(new TestList().createSelectionModel() instanceof DefaultListSelectionModel);
    }

    public void testEnsureIndexIsVisible() throws Exception {
        JScrollPane scroller = insertListToFrame();
        assertNotNull(scroller);
        Rectangle bounds = list.getCellBounds(1, 1);
        assertFalse(list.getVisibleRect().contains(bounds));
        list.ensureIndexIsVisible(1);
        assertTrue(list.getVisibleRect().contains(bounds));
        list = new JList();
        assertEquals(0, list.getModel().getSize());
        list.ensureIndexIsVisible(0);
        list.ensureIndexIsVisible(10);
        list.ensureIndexIsVisible(-10);
        list.setListData(new String[] { "1", "2" });
        list.ensureIndexIsVisible(0);
        list.ensureIndexIsVisible(10);
        list.ensureIndexIsVisible(-10);
    }

    public void testFireSelectionValueChanged() throws Exception {
        TestListSelectionListener l1 = new TestListSelectionListener();
        TestListSelectionListener l2 = new TestListSelectionListener();
        TestListSelectionListener l3 = new TestListSelectionListener();
        TestList list = new TestList();
        list.addListSelectionListener(l1);
        list.addListSelectionListener(l2);
        list.getSelectionModel().addListSelectionListener(l3);
        list.fireSelectionValueChanged(2, 5, true);
        assertNotNull(l1.getEvent());
        assertNotNull(l2.getEvent());
        assertNull(l3.getEvent());
        assertEquals(2, l1.getEvent().getFirstIndex());
        assertEquals(5, l1.getEvent().getLastIndex());
        assertTrue(l1.getEvent().getValueIsAdjusting());
        if (isHarmony()) {
            l1.reset();
            l2.reset();
            l3.reset();
            list.setSelectedIndex(1);
            assertNotNull(l1.getEvent());
            assertNotNull(l2.getEvent());
            assertNotNull(l3.getEvent());
            assertEquals(list, l1.getEvent().getSource());
            assertEquals(1, l1.getEvent().getFirstIndex());
            assertEquals(1, l1.getEvent().getLastIndex());
            assertFalse(l1.getEvent().getValueIsAdjusting());
            assertEquals(list.getSelectionModel(), l3.getEvent().getSource());
            assertEquals(1, l3.getEvent().getFirstIndex());
            assertEquals(1, l3.getEvent().getLastIndex());
            assertFalse(l3.getEvent().getValueIsAdjusting());
        }
    }

    public void testGetAccssibleContext() throws Exception {
        assertTrue(list.getAccessibleContext() instanceof JList.AccessibleJList);
    }

    public void testGetAnchorSelectionIndex() throws Exception {
        list.addSelectionInterval(2, 1);
        assertEquals(2, list.getAnchorSelectionIndex());
        assertEquals(2, list.getSelectionModel().getAnchorSelectionIndex());
    }

    public void testGetCellBounds() throws Exception {
        list.setUI(new ListUI() {
            @Override
            public Point indexToLocation(final JList arg0, final int arg1) {
                return null;
            }

            @Override
            public int locationToIndex(final JList arg0, final Point arg1) {
                return 0;
            }

            @Override
            public Rectangle getCellBounds(final JList arg0, final int arg1, final int arg2) {
                return new Rectangle(10, 20, 30, 40);
            }
        });
        assertEquals(new Rectangle(10, 20, 30, 40), list.getCellBounds(0, 0));
    }

    public void testGetSetCellRenderer() throws Exception {
        assertTrue(list.getCellRenderer() instanceof DefaultListCellRenderer);
        ListCellRenderer testRenderer = new ListCellRenderer() {
            public Component getListCellRendererComponent(final JList list, final Object value,
                    final int index, final boolean isSelected, final boolean cellHasFocus) {
                return null;
            }
        };
        list.setCellRenderer(testRenderer);
        assertEquals(testRenderer, list.getCellRenderer());
    }

    public void testGetSetDragEnabled() throws Exception {
        assertFalse(list.getDragEnabled());
        list.setDragEnabled(true);
        assertTrue(list.getDragEnabled());
    }

    public void testGetFirstVisibleIndex() throws Exception {
        JScrollPane scroller = insertListToFrame();
        Rectangle bounds = list.getCellBounds(1, 1);
        assertNotNull(bounds);
        assertEquals(0, list.getFirstVisibleIndex());
        scroller.getVerticalScrollBar().setValue(scroller.getVerticalScrollBar().getMaximum());
        assertEquals(1, list.getFirstVisibleIndex());
    }

    public void testGetSetFixedCellHeight() throws Exception {
        assertEquals(-1, list.getFixedCellHeight());
        list.setFixedCellHeight(10);
        assertEquals(10, list.getFixedCellHeight());
        assertTrue(changeListener.isChanged("fixedCellHeight"));
    }

    public void testGetSetFixedCellWidth() throws Exception {
        assertEquals(-1, list.getFixedCellWidth());
        list.setFixedCellWidth(10);
        assertEquals(10, list.getFixedCellWidth());
        assertTrue(changeListener.isChanged("fixedCellWidth"));
    }

    public void testGetLastVisibleIndex() throws Exception {
        JScrollPane scroller = insertListToFrame();
        Rectangle bounds = list.getCellBounds(1, 1);
        assertNotNull(bounds);
        assertEquals(1, list.getLastVisibleIndex());
        scroller.getVerticalScrollBar().setValue(scroller.getVerticalScrollBar().getMaximum());
        assertEquals(2, list.getLastVisibleIndex());
    }

    public void testGetSetLayoutOrientation() throws Exception {
        assertEquals(JList.VERTICAL, list.getLayoutOrientation());
        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        assertEquals(JList.VERTICAL_WRAP, list.getLayoutOrientation());
        assertTrue(changeListener.isChanged("layoutOrientation"));
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        assertEquals(JList.HORIZONTAL_WRAP, list.getLayoutOrientation());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() {
                list.setLayoutOrientation(10);
            }
        });
    }

    public void testGetLeadSelectionIndex() throws Exception {
        list.addSelectionInterval(2, 1);
        assertEquals(1, list.getLeadSelectionIndex());
        assertEquals(1, list.getSelectionModel().getLeadSelectionIndex());
    }

    public void testGetMaxSelectionIndex() throws Exception {
        list.addSelectionInterval(2, 1);
        assertEquals(2, list.getMaxSelectionIndex());
        assertEquals(2, list.getSelectionModel().getMaxSelectionIndex());
    }

    public void testGetMinSelectionIndex() throws Exception {
        list.addSelectionInterval(2, 1);
        assertEquals(1, list.getMinSelectionIndex());
        assertEquals(1, list.getSelectionModel().getMinSelectionIndex());
    }

    public void testGetSetModel() throws Exception {
        assertNotNull(list.getModel());
        ListModel m = new DefaultListModel();
        list.setModel(m);
        assertEquals(m, list.getModel());
        assertTrue(changeListener.isChanged("model"));
    }

    public void testGetNextMatch() throws Exception {
        list = new JList(new Object[] { "a1", "b1", "c1", "a2", "B2", "c2" });
        assertEquals(0, list.getNextMatch("a", 0, Position.Bias.Forward));
        assertEquals(3, list.getNextMatch("a", 1, Position.Bias.Forward));
        assertEquals(0, list.getNextMatch("a", 4, Position.Bias.Forward));
        assertEquals(1, list.getNextMatch("B", 1, Position.Bias.Backward));
        assertEquals(1, list.getNextMatch("b", 3, Position.Bias.Backward));
        assertEquals(4, list.getNextMatch("b", 5, Position.Bias.Backward));
        assertEquals(5, list.getNextMatch("c", 1, Position.Bias.Backward));
        assertEquals(-1, list.getNextMatch("d", 1, Position.Bias.Backward));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() {
                list.getNextMatch("a", -1, Position.Bias.Forward);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() {
                list.getNextMatch("a", 10, Position.Bias.Forward);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() {
                list.getNextMatch(null, 1, Position.Bias.Forward);
            }
        });
    }

    public void testGetPreferredScrollableViewportSize() throws Exception {
        list.setFixedCellHeight(10);
        list.setFixedCellWidth(100);
        list.setVisibleRowCount(5);
        assertEquals(new Dimension(100, 50), list.getPreferredScrollableViewportSize());
        list.setFixedCellWidth(-1);
        list.setListData(new Object[] { "a", "bbb", "cc" });
        int expectedWidth = list.getCellRenderer().getListCellRendererComponent(list, "bbb", 1,
                false, false).getPreferredSize().width;
        assertEquals(new Dimension(expectedWidth, 50), list
                .getPreferredScrollableViewportSize());
        list.setFixedCellHeight(-1);
        int expectedHeight = list.getCellRenderer().getListCellRendererComponent(list, "bbb",
                1, false, false).getPreferredSize().height * 5;
        assertEquals(new Dimension(expectedWidth, expectedHeight), list
                .getPreferredScrollableViewportSize());
        list.setListData(new Object[] {});
        assertEquals(new Dimension(256, 16 * 5), list.getPreferredScrollableViewportSize());
    }

    public void testGetSetPrototypeCellValue() throws Exception {
        assertNull(list.getPrototypeCellValue());
        list.setFixedCellHeight(1);
        list.setFixedCellWidth(1);
        changeListener.reset();
        list.setPrototypeCellValue("abcdef");
        assertEquals("abcdef", list.getPrototypeCellValue());
        assertTrue(changeListener.isChanged("prototypeCellValue"));
        assertEquals(1, changeListener.getNumberOfChanges());
        assertTrue(list.getFixedCellHeight() > 10);
        assertTrue(list.getFixedCellWidth() > 10);
    }

    //TODO
    public void testGetScrollableBlockIncrement() throws Exception {
        list.setListData(new Object[] { "a", "b", "c", "d", "e", "f", "g", "h" });
        JScrollPane scroller = insertListToFrame(50);
        int lastVisibleIndex = list.getLastVisibleIndex();
        assertEquals(3, lastVisibleIndex);
        int rowHeight = list.getCellBounds(lastVisibleIndex, lastVisibleIndex).height;
        assertEquals(rowHeight * lastVisibleIndex, list.getScrollableBlockIncrement(list
                .getVisibleRect(), SwingConstants.VERTICAL, 1));
        scroller.getVerticalScrollBar().setValue(
                scroller.getVerticalScrollBar().getValue() + rowHeight * 2);
        assertEquals(2, list.getFirstVisibleIndex());
        //        scroller.getVerticalScrollBar().setValue(columnWidth * 3);
        //        assertEquals(3, list.getFirstVisibleIndex());
        //        assertEquals(columnWidth * 2, list.getScrollableBlockIncrement(list.getVisibleRect(), SwingConstants.VERTICAL, -1));
        //        scroller.getVerticalScrollBar().setValue(scroller.getVerticalScrollBar().getValue() - columnWidth * 2);
        //        assertEquals(1, list.getFirstVisibleIndex());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() {
                list.getScrollableBlockIncrement(null, SwingConstants.VERTICAL, 1);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() {
                list.getScrollableBlockIncrement(list.getVisibleRect(), 10, 1);
            }
        });
    }

    public void testGetScrollableTracksViewportHeight() throws Exception {
        JScrollPane scroller = insertListToFrame();
        assertNotNull(scroller);
        list.setPreferredSize(new Dimension(1000, 10));
        assertTrue(list.getScrollableTracksViewportHeight());
        list.setPreferredSize(new Dimension(1000, 1000));
        assertFalse(list.getScrollableTracksViewportHeight());
        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        assertFalse(list.getScrollableTracksViewportHeight());
        list.setVisibleRowCount(0);
        assertTrue(list.getScrollableTracksViewportHeight());
        list.setVisibleRowCount(-1);
        assertTrue(list.getScrollableTracksViewportHeight());
    }

    public void testGetScrollableTracksViewportWidth() throws Exception {
        JScrollPane scroller = insertListToFrame();
        assertNotNull(scroller);
        list.setPreferredSize(new Dimension(10, 1000));
        assertTrue(list.getScrollableTracksViewportWidth());
        list.setPreferredSize(new Dimension(1000, 1000));
        assertFalse(list.getScrollableTracksViewportWidth());
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        assertFalse(list.getScrollableTracksViewportWidth());
        list.setVisibleRowCount(0);
        assertTrue(list.getScrollableTracksViewportWidth());
        list.setVisibleRowCount(-1);
        assertTrue(list.getScrollableTracksViewportWidth());
    }

    public void testGetScrollableUnitIncrement() throws Exception {
        JScrollPane scroller = insertListToFrame();
        assertEquals(0, list.getFirstVisibleIndex());
        int rowHeight = list.getCellBounds(0, 0).height;
        assertEquals(rowHeight, list.getScrollableUnitIncrement(list.getVisibleRect(),
                SwingConstants.VERTICAL, 1));
        assertEquals(0, list.getScrollableUnitIncrement(list.getVisibleRect(),
                SwingConstants.VERTICAL, -1));
        scroller.getVerticalScrollBar().setValue(5);
        assertEquals(rowHeight - 5, list.getScrollableUnitIncrement(list.getVisibleRect(),
                SwingConstants.VERTICAL, 1));
        assertEquals(5, list.getScrollableUnitIncrement(list.getVisibleRect(),
                SwingConstants.VERTICAL, -1));
        scroller.getVerticalScrollBar().setValue(rowHeight);
        assertEquals(rowHeight, list.getScrollableUnitIncrement(list.getVisibleRect(),
                SwingConstants.VERTICAL, 1));
        assertEquals(rowHeight, list.getScrollableUnitIncrement(list.getVisibleRect(),
                SwingConstants.VERTICAL, -1));
        assertEquals(list.getFont().getSize(), list.getScrollableUnitIncrement(list
                .getVisibleRect(), SwingConstants.HORIZONTAL, 1));
        assertEquals(list.getFont().getSize(), list.getScrollableUnitIncrement(list
                .getVisibleRect(), SwingConstants.HORIZONTAL, -1));
        list.setFont(list.getFont().deriveFont(100f));
        assertEquals(100, list.getScrollableUnitIncrement(list.getVisibleRect(),
                SwingConstants.HORIZONTAL, 1));
        assertEquals(100, list.getScrollableUnitIncrement(list.getVisibleRect(),
                SwingConstants.HORIZONTAL, -1));
        frame.setFont(null);
        frame.getContentPane().setFont(null);
        scroller.setFont(null);
        scroller.getViewport().setFont(null);
        list.setFont(null);
        assertNull(list.getFont());
        assertEquals(1, list.getScrollableUnitIncrement(list.getVisibleRect(),
                SwingConstants.HORIZONTAL, 1));
        assertEquals(1, list.getScrollableUnitIncrement(list.getVisibleRect(),
                SwingConstants.HORIZONTAL, -1));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() {
                list.getScrollableUnitIncrement(null, SwingConstants.VERTICAL, 1);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() {
                list.getScrollableUnitIncrement(list.getVisibleRect(), 10, 1);
            }
        });
    }

    public void testGetSetSelectedIndex() throws Exception {
        assertEquals(-1, list.getSelectedIndex());
        list.setSelectedIndex(2);
        assertEquals(2, list.getSelectedIndex());
        assertTrue(list.getSelectionModel().isSelectedIndex(2));
        list.clearSelection();
        assertEquals(-1, list.getSelectedIndex());
    }

    public void testGetSetSelectedIndices() throws Exception {
        assertEquals(0, list.getSelectedIndices().length);
        list.setSelectedIndices(new int[] { 0, 2 });
        assertEquals(2, list.getSelectedIndices().length);
        assertEquals(0, list.getSelectedIndices()[0]);
        assertEquals(2, list.getSelectedIndices()[1]);
        assertTrue(list.getSelectionModel().isSelectedIndex(0));
        assertFalse(list.getSelectionModel().isSelectedIndex(1));
        assertTrue(list.getSelectionModel().isSelectedIndex(2));
    }

    public void testGetSetSelectedValue() throws Exception {
        assertNull(list.getSelectedValue());
        list.setSelectedIndex(1);
        assertEquals("b", list.getSelectedValue());
        list.setSelectedValue("c", true);
        assertEquals("c", list.getSelectedValue());
        assertEquals(2, list.getSelectedIndex());
        assertEquals(1, list.getSelectedIndices().length);
        assertEquals(2, list.getSelectedIndices()[0]);
        list.setSelectedValue("d", true);
        assertEquals("c", list.getSelectedValue());
    }

    public void testGetSelectedValues() throws Exception {
        assertEquals(0, list.getSelectedValues().length);
        list.setSelectedIndex(1);
        assertEquals(1, list.getSelectedValues().length);
        list.addSelectionInterval(1, 2);
        assertEquals(2, list.getSelectedValues().length);
        assertEquals("b", list.getSelectedValues()[0]);
        assertEquals("c", list.getSelectedValues()[1]);
        list.setSelectedValue("a", true);
        assertEquals(1, list.getSelectedValues().length);
        assertEquals("a", list.getSelectedValues()[0]);
    }

    public void testGetSetSelectionBackground() throws Exception {
        assertNotNull(list.getSelectionBackground());
        list.setSelectionBackground(Color.red);
        assertEquals(Color.red, list.getSelectionBackground());
        assertTrue(changeListener.isChanged("selectionBackground"));
    }

    public void testGetSetSelectionForeground() throws Exception {
        assertNotNull(list.getSelectionForeground());
        list.setSelectionForeground(Color.red);
        assertEquals(Color.red, list.getSelectionForeground());
        assertTrue(changeListener.isChanged("selectionForeground"));
    }

    public void testGetSetSelectionMode() throws Exception {
        assertEquals(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, list.getSelectionMode());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        assertEquals(ListSelectionModel.SINGLE_SELECTION, list.getSelectionMode());
        assertEquals(ListSelectionModel.SINGLE_SELECTION, list.getSelectionModel()
                .getSelectionMode());
    }

    public void testGetSetSelectionModel() throws Exception {
        assertTrue(list.getSelectionModel() instanceof DefaultListSelectionModel);
        ListSelectionModel model = new DefaultListSelectionModel();
        list.setSelectionModel(model);
        assertEquals(model, list.getSelectionModel());
        assertTrue(changeListener.isChanged("selectionModel"));
    }

    public void testGetToolTipText() throws Exception {
        insertListToFrame();
        assertNull(list.getToolTipText());
        list.setToolTipText("list tooltip");
        assertEquals("list tooltip", list.getToolTipText());
        assertEquals("list tooltip", list.getToolTipText(new MouseEvent(list,
                MouseEvent.MOUSE_ENTERED, EventQueue.getMostRecentEventTime(), 0, 5, 5, 0,
                false)));
        list.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(final JList list, final Object value,
                    final int index, final boolean isSelected, final boolean cellHasFocus) {
                JLabel result = (JLabel) super.getListCellRendererComponent(list, value, index,
                        isSelected, cellHasFocus);
                result.setToolTipText("item tooltip");
                return result;
            }
        });
        assertEquals("item tooltip", list.getToolTipText(new MouseEvent(list,
                MouseEvent.MOUSE_ENTERED, EventQueue.getMostRecentEventTime(), 0, 5, 5, 0,
                false)));
    }

    public void testGetSetUpdateUI() throws Exception {
        ListUI ui = new ListUI() {
            @Override
            public Point indexToLocation(final JList arg0, final int arg1) {
                return null;
            }

            @Override
            public int locationToIndex(final JList arg0, final Point arg1) {
                return 0;
            }

            @Override
            public Rectangle getCellBounds(final JList arg0, final int arg1, final int arg2) {
                return null;
            }
        };
        list.setUI(ui);
        assertEquals(ui, list.getUI());
        list.updateUI();
        assertNotSame(ui, list.getUI());
    }

    public void testGetUICalssID() throws Exception {
        assertEquals("ListUI", list.getUIClassID());
    }

    public void testGetSetValueIsAdjusting() throws Exception {
        assertFalse(list.getValueIsAdjusting());
        list.setValueIsAdjusting(true);
        assertTrue(list.getValueIsAdjusting());
        assertTrue(list.getSelectionModel().getValueIsAdjusting());
    }

    public void testGetSetVisibleRowCount() throws Exception {
        assertEquals(8, list.getVisibleRowCount());
        list.setVisibleRowCount(10);
        assertEquals(10, list.getVisibleRowCount());
        assertTrue(changeListener.isChanged("visibleRowCount"));
        changeListener.reset();
        list.setVisibleRowCount(-2);
        assertEquals(0, list.getVisibleRowCount());
        assertTrue(changeListener.isChanged("visibleRowCount"));
    }

    public void testIndexToLocation() throws Exception {
        ListUI ui = new ListUI() {
            @Override
            public Point indexToLocation(final JList arg0, final int arg1) {
                return new Point(10, 20);
            }

            @Override
            public int locationToIndex(final JList arg0, final Point arg1) {
                return 0;
            }

            @Override
            public Rectangle getCellBounds(final JList arg0, final int arg1, final int arg2) {
                return null;
            }
        };
        list.setUI(ui);
        assertEquals(new Point(10, 20), list.indexToLocation(0));
    }

    public void testIsSelectedIndex() throws Exception {
        assertFalse(list.isSelectedIndex(0));
        list.setSelectedIndex(0);
        assertTrue(list.isSelectedIndex(0));
        assertTrue(list.getSelectionModel().isSelectedIndex(0));
    }

    public void testIsSelectionEmpty() throws Exception {
        assertTrue(list.isSelectionEmpty());
        list.setSelectedIndex(1);
        assertFalse(list.isSelectionEmpty());
        assertFalse(list.getSelectionModel().isSelectionEmpty());
    }

    public void testLocationToIndex() throws Exception {
        ListUI ui = new ListUI() {
            @Override
            public Point indexToLocation(final JList arg0, final int arg1) {
                return null;
            }

            @Override
            public int locationToIndex(final JList arg0, final Point arg1) {
                return 30;
            }

            @Override
            public Rectangle getCellBounds(final JList arg0, final int arg1, final int arg2) {
                return null;
            }
        };
        list.setUI(ui);
        assertEquals(30, list.locationToIndex(null));
    }

    public void testRemoveSelectionInterval() throws Exception {
        assertTrue(list.isSelectionEmpty());
        assertTrue(list.getSelectionModel().isSelectionEmpty());
        list.addSelectionInterval(0, 2);
        assertFalse(list.isSelectionEmpty());
        assertFalse(list.getSelectionModel().isSelectionEmpty());
        list.removeSelectionInterval(0, 1);
        assertFalse(list.isSelectedIndex(0));
        assertFalse(list.isSelectedIndex(1));
        assertFalse(list.getSelectionModel().isSelectedIndex(0));
        assertFalse(list.getSelectionModel().isSelectedIndex(1));
    }

    public void testSetListData() throws Exception {
        ListModel model = list.getModel();
        assertEquals(3, model.getSize());
        list.setListData(new Object[] { "1", "2" });
        assertTrue(changeListener.isChanged("model"));
        assertNotSame(model, list.getModel());
        assertEquals(2, list.getModel().getSize());
        model = list.getModel();
        changeListener.reset();
        Vector<String> data = new Vector<String>();
        data.add("x");
        data.add("y");
        data.add("z");
        list.setListData(data);
        assertTrue(changeListener.isChanged("model"));
        assertNotSame(model, list.getModel());
        assertEquals(3, list.getModel().getSize());
    }

    private JScrollPane insertListToFrame() {
        return insertListToFrame(25);
    }

    private JScrollPane insertListToFrame(final int preferredHeight) {
        frame.setLocation(100, 100);
        JScrollPane result = new JScrollPane(list) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, preferredHeight);
            }
        };
        frame.getContentPane().add(result);
        frame.pack();
        return result;
    }

    private class TestListSelectionListener implements ListSelectionListener {
        private ListSelectionEvent event;

        public void valueChanged(final ListSelectionEvent event) {
            this.event = event;
        }

        public ListSelectionEvent getEvent() {
            return event;
        }

        public void reset() {
            event = null;
        }
    }

    private class TestPropertyChangeListener implements PropertyChangeListener {
        private List<String> changedNames = new ArrayList<String>();

        public void propertyChange(final PropertyChangeEvent event) {
            changedNames.add(event.getPropertyName());
        }

        public void reset() {
            changedNames.clear();
        }

        public boolean isChanged(final String name) {
            return changedNames.contains(name);
        }

        public boolean isChanged() {
            return !changedNames.isEmpty();
        }

        public int getNumberOfChanges() {
            return changedNames.size();
        }

        public List<String> getAllChangedNames() {
            return changedNames;
        }

        @Override
        public String toString() {
            return "Changed: " + changedNames;
        }
    }

    private class TestList extends JList {
        private static final long serialVersionUID = 1L;

        @Override
        public ListSelectionModel createSelectionModel() {
            return super.createSelectionModel();
        }

        @Override
        public void fireSelectionValueChanged(final int firstIndex, final int lastIndex,
                final boolean isAdjusting) {
            super.fireSelectionValueChanged(firstIndex, lastIndex, isAdjusting);
        }
    }
}
