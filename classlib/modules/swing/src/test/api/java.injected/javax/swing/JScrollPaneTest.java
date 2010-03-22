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
 * @author Anton Avtamonov, Sergey Burlak
 */
package javax.swing;

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import javax.swing.border.Border;
import javax.swing.plaf.ScrollPaneUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;

public class JScrollPaneTest extends SwingTestCase {
    private JScrollPane pane;

    public JScrollPaneTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        pane = new JScrollPane();
        propertyChangeController = new PropertyChangeController();
        pane.addPropertyChangeListener(propertyChangeController);
    }

    @Override
    protected void tearDown() throws Exception {
        pane = null;
    }

    public void testJScrollPane() throws Exception {
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                pane.verticalScrollBarPolicy);
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED,
                pane.horizontalScrollBarPolicy);
        assertNotNull(pane.viewport);
        assertNull(pane.viewport.getView());
        assertTrue(pane.verticalScrollBar instanceof JScrollPane.ScrollBar);
        assertEquals(Adjustable.VERTICAL, pane.verticalScrollBar.getOrientation());
        assertTrue(pane.horizontalScrollBar instanceof JScrollPane.ScrollBar);
        assertEquals(Adjustable.HORIZONTAL, pane.horizontalScrollBar.getOrientation());
        assertNull(pane.rowHeader);
        assertNull(pane.columnHeader);
        assertNull(pane.lowerLeft);
        assertNull(pane.lowerRight);
        assertNull(pane.upperLeft);
        assertNull(pane.upperRight);
        assertEquals(3, pane.getComponentCount());
        assertEquals(pane.viewport, pane.getComponent(0));
        assertEquals(pane.verticalScrollBar, pane.getComponent(1));
        assertEquals(pane.horizontalScrollBar, pane.getComponent(2));
        Component view = new JButton();
        pane = new JScrollPane(view, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        assertEquals(view, pane.viewport.getView());
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, pane.verticalScrollBarPolicy);
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS,
                pane.horizontalScrollBarPolicy);
        view = new JButton();
        pane = new JScrollPane(view);
        assertEquals(view, pane.viewport.getView());
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                pane.verticalScrollBarPolicy);
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED,
                pane.horizontalScrollBarPolicy);
        pane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        assertNull(pane.viewport.getView());
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                pane.verticalScrollBarPolicy);
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
                pane.horizontalScrollBarPolicy);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new JScrollPane(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            }
        });
    }

    public void testGetSetUpdateUI() throws Exception {
        assertNotNull(pane.getUI());
        ScrollPaneUI ui = new BasicScrollPaneUI();
        pane.setUI(ui);
        assertEquals(ui, pane.getUI());
        pane.updateUI();
        assertNotSame(ui, pane.getUI());
    }

    public void testGetUICalssID() throws Exception {
        assertEquals("ScrollPaneUI", pane.getUIClassID());
    }

    public void testSetLayout() throws Exception {
        assertTrue(pane.getLayout() instanceof ScrollPaneLayout);
        TestLayout layout = new TestLayout();
        pane.setLayout(layout);
        assertEquals(layout, pane.getLayout());
        assertEquals(pane, layout.getSyncScrollPane());
        pane.setLayout(null);
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                pane.setLayout(new LayoutManager() {
                    public void addLayoutComponent(final String name, final Component comp) {
                    }

                    public void layoutContainer(final Container parent) {
                    }

                    public Dimension minimumLayoutSize(final Container parent) {
                        return null;
                    }

                    public Dimension preferredLayoutSize(final Container parent) {
                        return null;
                    }

                    public void removeLayoutComponent(final Component comp) {
                    }
                });
            }

            @Override
            public Class<ClassCastException> expectedExceptionClass() {
                return ClassCastException.class;
            }
        });
    }

    public void testIsValidRoot() throws Exception {
        assertTrue(pane.isValidateRoot());
    }

    public void testGetSetVerticalScrollBarPolicy() throws Exception {
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, pane
                .getVerticalScrollBarPolicy());
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        assertTrue(propertyChangeController.isChanged("verticalScrollBarPolicy"));
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, pane
                .getVerticalScrollBarPolicy());
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, pane
                .getVerticalScrollBarPolicy());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                pane.setVerticalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            }
        });
    }

    public void testGetSetHorizontalScrollBarPolicy() throws Exception {
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED, pane
                .getHorizontalScrollBarPolicy());
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        assertTrue(propertyChangeController.isChanged("horizontalScrollBarPolicy"));
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS, pane
                .getHorizontalScrollBarPolicy());
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, pane
                .getHorizontalScrollBarPolicy());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            }
        });
    }

    public void testGetSetViewportBorder() throws Exception {
        assertNull(pane.getViewportBorder());
        Border border = BorderFactory.createEmptyBorder();
        pane.setViewportBorder(border);
        assertEquals(border, pane.getViewportBorder());
        assertTrue(propertyChangeController.isChanged("viewportBorder"));
    }

    public void testGetViewportBorderBounds() throws Exception {
        pane.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 20));
        pane.setBounds(200, 200, 40, 60);
        assertEquals(new Rectangle(10, 5, 40 - 10 - 20, 60 - 5 - 15), pane
                .getViewportBorderBounds());
        pane.setColumnHeader(new JViewport());
        pane.getColumnHeader().setBounds(10, 20, 20, 50);
        assertEquals(new Rectangle(10, 5 + 50, 40 - 10 - 20, 60 - 5 - 15 - 50), pane
                .getViewportBorderBounds());
        pane.setRowHeader(new JViewport());
        pane.getRowHeader().setBounds(10, 20, 20, 30);
        assertEquals(new Rectangle(10 + 20, 5 + 50, 40 - 10 - 20 - 20, 60 - 5 - 15 - 50), pane
                .getViewportBorderBounds());
        pane.getVerticalScrollBar().setBounds(20, 10, 30, 10);
        assertEquals(new Rectangle(10 + 20, 5 + 50, 40 - 10 - 20 - 20 - 30, 60 - 5 - 15 - 50),
                pane.getViewportBorderBounds());
        pane.getHorizontalScrollBar().setBounds(20, 10, 30, 50);
        assertEquals(new Rectangle(10 + 20, 5 + 50, 40 - 10 - 20 - 20 - 30, 60 - 5 - 15 - 50
                - 50), pane.getViewportBorderBounds());
    }

    public void testCreateHorizontalScrollBar() throws Exception {
        JScrollBar scrollBar = pane.createHorizontalScrollBar();
        assertTrue(scrollBar instanceof JScrollPane.ScrollBar);
        assertEquals(Adjustable.HORIZONTAL, scrollBar.getOrientation());
    }

    public void testCreateVerticalScrollBar() throws Exception {
        JScrollBar scrollBar = pane.createVerticalScrollBar();
        assertTrue(scrollBar instanceof JScrollPane.ScrollBar);
        assertEquals(Adjustable.VERTICAL, scrollBar.getOrientation());
    }

    public void testGetSetHorizontalScrollBar() throws Exception {
        assertTrue(pane.getHorizontalScrollBar() instanceof JScrollPane.ScrollBar);
        JScrollBar sb = new JScrollBar(Adjustable.VERTICAL);
        pane.setHorizontalScrollBar(sb);
        assertEquals(sb, pane.getHorizontalScrollBar());
        assertTrue(propertyChangeController.isChanged("horizontalScrollBar"));
        assertEquals(Adjustable.VERTICAL, sb.getOrientation());
    }

    public void testGetSetVerticalScrollBar() throws Exception {
        assertTrue(pane.getVerticalScrollBar() instanceof JScrollPane.ScrollBar);
        JScrollBar sb = new JScrollBar(Adjustable.HORIZONTAL);
        pane.setVerticalScrollBar(sb);
        assertEquals(sb, pane.getVerticalScrollBar());
        assertTrue(propertyChangeController.isChanged("verticalScrollBar"));
        assertEquals(Adjustable.HORIZONTAL, sb.getOrientation());
    }

    public void testCreateViewport() throws Exception {
        assertNull(pane.createViewport().getView());
    }

    public void testGetSetViewport() throws Exception {
        assertNotNull(pane.getViewport());
        JViewport viewport = new JViewport();
        pane.setViewport(viewport);
        assertEquals(viewport, pane.getViewport());
        assertEquals(3, pane.getComponentCount());
        assertEquals(viewport, pane.getComponent(2));
        JViewport newViewport = new JViewport();
        pane.setViewport(newViewport);
        assertEquals(newViewport, pane.getViewport());
        assertEquals(3, pane.getComponentCount());
        assertEquals(newViewport, pane.getComponent(2));
        assertTrue(propertyChangeController.isChanged("viewport"));
        pane.setViewport(null);
        assertNull(pane.getViewport());
        assertEquals(2, pane.getComponentCount());
        assertTrue(propertyChangeController.isChanged("viewport"));
        propertyChangeController.reset();
        pane.setViewport(null);
        assertNull(pane.getViewport());
        assertEquals(2, pane.getComponentCount());
        assertTrue(propertyChangeController.isChanged("viewport"));
        propertyChangeController.reset();
        pane.setViewport(newViewport);
        assertEquals(3, pane.getComponentCount());
        assertTrue(propertyChangeController.isChanged("viewport"));
        pane.remove(newViewport);
        propertyChangeController.reset();
        pane.setViewport(newViewport);
        assertEquals(3, pane.getComponentCount());
        assertFalse(propertyChangeController.isChanged("viewport"));
    }

    public void testSetViewportView() throws Exception {
        assertNull(pane.getViewport().getView());
        Component c = new JButton();
        pane.setViewportView(c);
        assertEquals(c, pane.getViewport().getView());
        pane.setViewport(null);
        pane.setViewportView(c);
        assertEquals(c, pane.getViewport().getView());
        assertTrue(propertyChangeController.isChanged("viewport"));
    }

    public void testGetSetRowHeader() throws Exception {
        assertNull(pane.getRowHeader());
        JViewport rowHeader = new JViewport();
        pane.setRowHeader(rowHeader);
        assertEquals(rowHeader, pane.getRowHeader());
        assertTrue(propertyChangeController.isChanged("rowHeader"));
        assertEquals(4, pane.getComponentCount());
        assertEquals(rowHeader, pane.getComponent(3));
        JViewport newRowHeader = new JViewport();
        pane.setRowHeader(newRowHeader);
        assertEquals(newRowHeader, pane.getRowHeader());
        assertEquals(4, pane.getComponentCount());
        assertEquals(newRowHeader, pane.getComponent(3));
        pane.setRowHeader(null);
        assertNull(pane.getRowHeader());
        assertEquals(3, pane.getComponentCount());
    }

    public void testSetRowHeaderView() throws Exception {
        assertNull(pane.getRowHeader());
        Component c = new JButton();
        pane.setRowHeaderView(c);
        assertEquals(c, pane.getRowHeader().getView());
        assertTrue(propertyChangeController.isChanged("rowHeader"));
    }

    public void testGetSetColumnHeader() throws Exception {
        assertNull(pane.getColumnHeader());
        JViewport columnHeader = new JViewport();
        pane.setColumnHeader(columnHeader);
        assertEquals(columnHeader, pane.getColumnHeader());
        assertTrue(propertyChangeController.isChanged("columnHeader"));
        assertEquals(4, pane.getComponentCount());
        assertEquals(columnHeader, pane.getComponent(3));
        JViewport newColumnHeader = new JViewport();
        pane.setColumnHeader(newColumnHeader);
        assertEquals(newColumnHeader, pane.getColumnHeader());
        assertEquals(4, pane.getComponentCount());
        assertEquals(newColumnHeader, pane.getComponent(3));
        pane.setColumnHeader(null);
        assertNull(pane.getColumnHeader());
        assertEquals(3, pane.getComponentCount());
    }

    public void testSetColumnHeaderView() throws Exception {
        assertNull(pane.getColumnHeader());
        Component c = new JButton();
        pane.setColumnHeaderView(c);
        assertEquals(c, pane.getColumnHeader().getView());
        assertTrue(propertyChangeController.isChanged("columnHeader"));
    }

    public void testGetSetCorner() throws Exception {
        assertNull(pane.getCorner(ScrollPaneConstants.LOWER_LEFT_CORNER));
        assertNull(pane.getCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER));
        assertNull(pane.getCorner(ScrollPaneConstants.UPPER_LEFT_CORNER));
        assertNull(pane.getCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER));
        assertNull(pane.getCorner(ScrollPaneConstants.LOWER_LEADING_CORNER));
        assertNull(pane.getCorner(ScrollPaneConstants.LOWER_TRAILING_CORNER));
        assertNull(pane.getCorner(ScrollPaneConstants.UPPER_LEADING_CORNER));
        assertNull(pane.getCorner(ScrollPaneConstants.UPPER_TRAILING_CORNER));
        assertEquals(3, pane.getComponentCount());
        Component lowerLeft = new JButton();
        Component lowerRight = new JButton();
        Component upperLeft = new JButton();
        Component upperRight = new JButton();
        Component lowerLeading = new JButton();
        Component lowerTrailing = new JButton();
        Component upperLeading = new JButton();
        Component upperTrailing = new JButton();
        pane.setCorner(ScrollPaneConstants.LOWER_LEFT_CORNER, lowerLeft);
        assertTrue(propertyChangeController.isChanged(ScrollPaneConstants.LOWER_LEFT_CORNER));
        assertEquals(4, pane.getComponentCount());
        propertyChangeController.reset();
        pane.setCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER, lowerRight);
        assertTrue(propertyChangeController.isChanged(ScrollPaneConstants.LOWER_RIGHT_CORNER));
        assertEquals(5, pane.getComponentCount());
        propertyChangeController.reset();
        pane.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, upperLeft);
        assertTrue(propertyChangeController.isChanged(ScrollPaneConstants.UPPER_LEFT_CORNER));
        assertEquals(6, pane.getComponentCount());
        propertyChangeController.reset();
        pane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, upperRight);
        assertTrue(propertyChangeController.isChanged(ScrollPaneConstants.UPPER_RIGHT_CORNER));
        assertEquals(7, pane.getComponentCount());
        assertEquals(lowerLeft, pane.getCorner(ScrollPaneConstants.LOWER_LEFT_CORNER));
        assertEquals(lowerRight, pane.getCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER));
        assertEquals(upperLeft, pane.getCorner(ScrollPaneConstants.UPPER_LEFT_CORNER));
        assertEquals(upperRight, pane.getCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER));
        propertyChangeController.reset();
        pane.setCorner(ScrollPaneConstants.LOWER_LEADING_CORNER, lowerLeading);
        assertTrue(propertyChangeController.isChanged(ScrollPaneConstants.LOWER_LEFT_CORNER));
        assertEquals(7, pane.getComponentCount());
        propertyChangeController.reset();
        pane.setCorner(ScrollPaneConstants.LOWER_TRAILING_CORNER, lowerTrailing);
        assertTrue(propertyChangeController.isChanged(ScrollPaneConstants.LOWER_RIGHT_CORNER));
        assertEquals(7, pane.getComponentCount());
        propertyChangeController.reset();
        pane.setCorner(ScrollPaneConstants.UPPER_LEADING_CORNER, upperLeading);
        assertTrue(propertyChangeController.isChanged(ScrollPaneConstants.UPPER_LEFT_CORNER));
        assertEquals(7, pane.getComponentCount());
        propertyChangeController.reset();
        pane.setCorner(ScrollPaneConstants.UPPER_TRAILING_CORNER, upperTrailing);
        assertTrue(propertyChangeController.isChanged(ScrollPaneConstants.UPPER_RIGHT_CORNER));
        assertEquals(7, pane.getComponentCount());
        assertEquals(lowerLeading, pane.getCorner(ScrollPaneConstants.LOWER_LEFT_CORNER));
        assertEquals(lowerTrailing, pane.getCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER));
        assertEquals(upperLeading, pane.getCorner(ScrollPaneConstants.UPPER_LEFT_CORNER));
        assertEquals(upperTrailing, pane.getCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                pane.setCorner("anything", null);
            }
        });
        pane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, new JButton());
        assertEquals(7, pane.getComponentCount());
        pane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, null);
        assertEquals(6, pane.getComponentCount());
        JButton b = new JButton();
        propertyChangeController.reset();
        pane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, b);
        assertTrue(propertyChangeController.isChanged(ScrollPaneConstants.UPPER_RIGHT_CORNER));
        assertEquals(7, pane.getComponentCount());
        propertyChangeController.reset();
        pane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, b);
        assertFalse(propertyChangeController.isChanged(ScrollPaneConstants.UPPER_RIGHT_CORNER));
        assertEquals(7, pane.getComponentCount());
        propertyChangeController.reset();
        pane.remove(6);
        pane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, b);
        assertFalse(propertyChangeController.isChanged(ScrollPaneConstants.UPPER_RIGHT_CORNER));
        assertEquals(7, pane.getComponentCount());
    }

    public void testSetComponentOrientation() throws Exception {
        assertTrue(pane.getComponentOrientation().isLeftToRight());
        assertTrue(pane.getVerticalScrollBar().getComponentOrientation().isLeftToRight());
        assertTrue(pane.getHorizontalScrollBar().getComponentOrientation().isLeftToRight());
        pane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        assertFalse(pane.getComponentOrientation().isLeftToRight());
        assertFalse(pane.getVerticalScrollBar().getComponentOrientation().isLeftToRight());
        assertFalse(pane.getHorizontalScrollBar().getComponentOrientation().isLeftToRight());
    }

    public void testSetIsWhellScrollingEnabled() throws Exception {
        assertTrue(pane.isWheelScrollingEnabled());
        pane.setWheelScrollingEnabled(false);
        assertFalse(pane.isWheelScrollingEnabled());
        assertTrue(propertyChangeController.isChanged("wheelScrollingEnabled"));
    }

    public void testGetAccessibleContext() throws Exception {
        assertTrue(pane.getAccessibleContext() instanceof JScrollPane.AccessibleJScrollPane);
    }

    public void testIsOpaque() throws Exception {
        assertTrue(pane.isOpaque());
    }

    private class TestLayout extends ScrollPaneLayout {
        private static final long serialVersionUID = 1L;

        private JScrollPane syncScrollPane;

        @Override
        public void syncWithScrollPane(final JScrollPane sp) {
            super.syncWithScrollPane(sp);
            syncScrollPane = sp;
        }

        public JScrollPane getSyncScrollPane() {
            return syncScrollPane;
        }
    }
}
