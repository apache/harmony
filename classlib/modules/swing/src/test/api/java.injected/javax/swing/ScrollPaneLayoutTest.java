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
 * @author Sergey Burlak
 */
package javax.swing;

import java.awt.Dimension;

public class ScrollPaneLayoutTest extends SwingTestCase {
    private ScrollPaneLayout layout;

    private JScrollPane pane;

    private JLabel label;

    @Override
    protected void setUp() throws Exception {
        label = new JLabel();
        label.setPreferredSize(new Dimension(500, 500));
        pane = new JScrollPane(label);
        layout = (ScrollPaneLayout) pane.getLayout();
    }

    @Override
    protected void tearDown() throws Exception {
        layout = null;
        pane = null;
        label = null;
    }

    public void testGetPreferredLayoutSize() throws Exception {
        layout.colHead = new JViewport();
        layout.colHead.setPreferredSize(new Dimension(100, 30));
        layout.rowHead = new JViewport();
        layout.rowHead.setPreferredSize(new Dimension(50, 20));
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        pane.setBorder(BorderFactory.createEmptyBorder(51, 101, 151, 202));
        pane.setViewportBorder(BorderFactory.createEmptyBorder(51, 101, 151, 202));
        int width = layout.viewport.getPreferredSize().width
                + layout.rowHead.getPreferredSize().width
                + (layout.vsb == null ? 0 : layout.vsb.getBounds().width)
                + pane.getInsets().right + pane.getInsets().left + 101 + 202;
        int height = layout.viewport.getPreferredSize().height
                + layout.colHead.getPreferredSize().height
                + (layout.hsb == null ? 0 : layout.hsb.getBounds().height)
                + pane.getInsets().top + pane.getInsets().bottom + 51 + 151;
        assertEquals(width, layout.preferredLayoutSize(pane).width);
        assertEquals(height, layout.preferredLayoutSize(pane).height);
        try {
            layout.preferredLayoutSize(new JButton());
            fail("Class cast exception shall be thrown");
        } catch (ClassCastException e) {
        }
        //regression for HARMONY-1735
        try {
        	layout.preferredLayoutSize(null);
        	fail("No expected exception");
        }catch (NullPointerException e) {
        //expected
        }
      }

    public void testDefaultLayout() throws Exception {
        ScrollPaneLayout l = new ScrollPaneLayout();
        assertNull(l.colHead);
        assertNull(l.lowerLeft);
        assertNull(l.lowerRight);
        assertNull(l.upperLeft);
        assertNull(l.upperRight);
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, l.vsbPolicy);
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED, l.hsbPolicy);
    }

    public void testSetHorizontalPolicy() throws Exception {
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED, layout
                .getHorizontalScrollBarPolicy());
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        layout.syncWithScrollPane(pane);
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS, layout
                .getHorizontalScrollBarPolicy());
        layout.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, layout
                .getHorizontalScrollBarPolicy());
        // regression 1 for HARMONY-1737
        try{
            layout.setHorizontalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            fail("No expected IllegalArgumentException");
        }catch(IllegalArgumentException e){
         //expected 
        }
    }

    public void testSetVerticalPolicy() throws Exception {
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, layout
                .getVerticalScrollBarPolicy());
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        layout.syncWithScrollPane(pane);
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, layout
                .getVerticalScrollBarPolicy());
        layout.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, layout
                .getVerticalScrollBarPolicy());
        // regression 2 for HARMONY-1737
        try{
            layout.setVerticalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            fail("No expected IllegalArgumentException");
        }catch(IllegalArgumentException e){
        //expected 
        } 
    }

    public void testGetViewport() throws Exception {
        assertEquals(layout.viewport, layout.getViewport());
        layout.viewport = null;
        assertNull(layout.getViewport());
    }

    public void testGetHorizontalScrollbar() throws Exception {
        assertEquals(layout.hsb, layout.getHorizontalScrollBar());
        layout.hsb = null;
        assertNull(layout.getHorizontalScrollBar());
    }

    public void testGetVerticalScrollbar() throws Exception {
        assertEquals(layout.vsb, layout.getVerticalScrollBar());
        layout.vsb = null;
        assertNull(layout.getVerticalScrollBar());
    }

    public void testGetCorner() throws Exception {
        JButton lowerLeftButton = new JButton();
        JButton upperLeftButton = new JButton();
        JButton lowerRightButton = new JButton();
        JButton upperRightButton = new JButton();
        layout.lowerLeft = lowerLeftButton;
        layout.upperLeft = upperLeftButton;
        layout.lowerRight = lowerRightButton;
        layout.upperRight = upperRightButton;
        assertEquals(lowerLeftButton, layout.getCorner(ScrollPaneConstants.LOWER_LEFT_CORNER));
        assertEquals(upperLeftButton, layout.getCorner(ScrollPaneConstants.UPPER_LEFT_CORNER));
        assertEquals(lowerRightButton, layout.getCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER));
        assertEquals(upperRightButton, layout.getCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER));
        assertNull(layout.getCorner("something"));
    }

    public void testGetRowHeader() throws Exception {
        assertNull(layout.getRowHeader());
        layout.rowHead = new JViewport();
        assertEquals(layout.rowHead, layout.getRowHeader());
    }

    public void testGetColumnHeader() throws Exception {
        assertNull(layout.getColumnHeader());
        layout.colHead = new JViewport();
        assertEquals(layout.colHead, layout.getColumnHeader());
    }

    public void testSyncWithScrollPane() throws Exception {
        ScrollPaneLayout l = new ScrollPaneLayout();
        assertNull(l.viewport);
        assertNull(l.rowHead);
        assertNull(l.colHead);
        assertNull(l.lowerLeft);
        assertNull(l.lowerRight);
        assertNull(l.upperLeft);
        assertNull(l.upperRight);
        assertNull(l.hsb);
        assertNull(l.vsb);
        l.syncWithScrollPane(pane);
        assertEquals(pane.getViewport(), l.viewport);
        assertEquals(pane.getRowHeader(), l.rowHead);
        assertEquals(pane.getColumnHeader(), l.colHead);
        assertEquals(pane.getHorizontalScrollBar(), l.hsb);
        assertEquals(pane.getVerticalScrollBar(), l.vsb);
        assertEquals(pane.getCorner(ScrollPaneConstants.LOWER_LEFT_CORNER), l.lowerLeft);
        assertEquals(pane.getCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER), l.lowerRight);
        assertEquals(pane.getCorner(ScrollPaneConstants.UPPER_LEFT_CORNER), l.upperLeft);
        assertEquals(pane.getCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER), l.upperRight);
        assertEquals(pane.getHorizontalScrollBarPolicy(), l.getHorizontalScrollBarPolicy());
        assertEquals(pane.getVerticalScrollBarPolicy(), l.getVerticalScrollBarPolicy());
        try {
            layout.syncWithScrollPane(null);
            fail("NPE shall be thrown");
        } catch (NullPointerException e) {
        }
    }

    public void testGetViewportBorderBounds() throws Exception {
        assertEquals(pane.getViewportBorderBounds(), layout.getViewportBorderBounds(pane));
    }

    public void testRemoveLayoutComponent() throws Exception {
        assertNotNull(layout.viewport);
        layout.removeLayoutComponent(layout.viewport);
        assertNull(layout.viewport);
        assertNotNull(layout.vsb);
        layout.removeLayoutComponent(layout.vsb);
        assertNull(layout.vsb);
        assertNotNull(layout.hsb);
        layout.removeLayoutComponent(layout.hsb);
        assertNull(layout.hsb);
        layout.rowHead = new JViewport();
        assertNotNull(layout.rowHead);
        layout.removeLayoutComponent(layout.rowHead);
        assertNull(layout.rowHead);
        layout.colHead = new JViewport();
        assertNotNull(layout.colHead);
        layout.removeLayoutComponent(layout.colHead);
        assertNull(layout.colHead);
        layout.lowerLeft = new JButton();
        assertNotNull(layout.lowerLeft);
        layout.removeLayoutComponent(layout.lowerLeft);
        assertNull(layout.lowerLeft);
        layout.lowerRight = new JButton();
        assertNotNull(layout.lowerRight);
        layout.removeLayoutComponent(layout.lowerRight);
        assertNull(layout.lowerRight);
        layout.upperLeft = new JButton();
        assertNotNull(layout.upperLeft);
        layout.removeLayoutComponent(layout.upperLeft);
        assertNull(layout.upperLeft);
        layout.upperRight = new JButton();
        assertNotNull(layout.upperRight);
        layout.removeLayoutComponent(layout.upperRight);
        assertNull(layout.upperRight);
    }

    public void testAddSingletonLayoutComponent() throws Exception {
        JButton newButton = new JButton();
        JButton button = new JButton();
        pane.setCorner(ScrollPaneConstants.LOWER_LEFT_CORNER, button);
        int componentCount = pane.getComponentCount();
        assertEquals(newButton, layout.addSingletonComponent(button, newButton));
        assertEquals(componentCount - 1, pane.getComponentCount());
        pane.setCorner(ScrollPaneConstants.LOWER_LEFT_CORNER, button);
        componentCount = pane.getComponentCount();
        assertNull(layout.addSingletonComponent(button, null));
        assertEquals(componentCount - 1, pane.getComponentCount());
    }

    public void testMinimumLayoutSize() throws Exception {
        layout.colHead = new JViewport();
        layout.colHead.setPreferredSize(new Dimension(100, 30));
        layout.rowHead = new JViewport();
        layout.rowHead.setPreferredSize(new Dimension(50, 20));
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        pane.setBorder(BorderFactory.createEmptyBorder(51, 101, 151, 202));
        pane.setViewportBorder(BorderFactory.createEmptyBorder(51, 101, 151, 202));
        int width = layout.viewport.getMinimumSize().width
                + layout.rowHead.getMinimumSize().width
                + pane.getVerticalScrollBar().getMinimumSize().width + pane.getInsets().right
                + pane.getInsets().left + 101 + 202;
        int height = layout.viewport.getMinimumSize().height
                + layout.colHead.getMinimumSize().height
                + pane.getHorizontalScrollBar().getMinimumSize().height + pane.getInsets().top
                + pane.getInsets().bottom + 51 + 151;
        assertEquals(new Dimension(width, height), layout.minimumLayoutSize(pane));
    }
}
