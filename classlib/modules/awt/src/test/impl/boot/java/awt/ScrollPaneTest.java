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
 * @author Dmitry A. Durnev
 */
package java.awt;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import junit.framework.TestCase;

/**
 * ScrollPaneTest
 */
public class ScrollPaneTest extends TestCase {

    ScrollPane scrollPane;
    boolean eventProcessed;
    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scrollPane = new ScrollPane();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testGetAccessibleContext() {
        //TODO Implement getAccessibleContext().
    }

    public final void testDoLayout() {
        Component comp = new Button();
        comp.setLocation(100, 100);
        //comp.setSize(1000, 1000);
        scrollPane.add(comp);
        scrollPane.doLayout();
        assertEquals(scrollPane.getSize(), comp.getSize());
        assertEquals(new Point(), comp.getLocation());
        Dimension size = new Dimension(50, 1000);
        comp.setPreferredSize(size);
        scrollPane.doLayout();
        size = new Dimension(100, 1000);
        assertEquals(size, comp.getSize());

        size = new Dimension(160, 200);
        comp.setPreferredSize(size);
        scrollPane.doLayout();
        scrollPane.setScrollPosition(50, 75);
        // If the new preferred size of the child causes the current scroll
        // position to be invalid, the scroll position
        // is set to the closest valid position.
        size = new Dimension(110, 120);
        comp.setPreferredSize(size);
        scrollPane.doLayout();

        assertEquals(size, comp.getSize());
        int viewHeight = scrollPane.getViewportSize().height;
        int viewWidth = scrollPane.getViewportSize().width;
        Point pos = new Point(size.width - viewWidth,
                              size.height - viewHeight);
        assertEquals(pos, scrollPane.getScrollPosition());
    }

    public final void testParamString() {
        String str = scrollPane.paramString();
        assertEquals(0, str.indexOf("scrollpane"));
        assertTrue(str.indexOf("ScrollPosition=(0,0)") > 0);
        assertTrue(str.indexOf("Insets=(0,0,0,0)") > 0);
        assertTrue(str.indexOf("ScrollbarDisplayPolicy=as-needed") > 0);
        assertTrue(str.indexOf("wheelScrollingEnabled=true") > 0);
    }

    public final void testProcessMouseWheelEvent() {
        eventProcessed = false;
        scrollPane.addMouseWheelListener(new MouseWheelListener() {

            public void mouseWheelMoved(MouseWheelEvent arg0) {
                eventProcessed = true;
            }

        });

        scrollPane.processEvent(new MouseWheelEvent(scrollPane,
                                              MouseEvent.MOUSE_WHEEL,
                                              0l, 0, 100, 200, 0, false,
                                              MouseWheelEvent.WHEEL_BLOCK_SCROLL,
                                              10, 10));
        assertTrue(eventProcessed);
    }

    public final void testAddImpl() {
        assertEquals(0, scrollPane.getComponentCount());
        Component c = new Button();
        scrollPane.add(c);
        assertEquals(1, scrollPane.getComponentCount());
        assertSame(c, scrollPane.getComponent(0));
        scrollPane.add(c = new Checkbox());
        assertEquals(1, scrollPane.getComponentCount());
        assertSame(c, scrollPane.getComponent(0));
        boolean iae = false;
        try {
            scrollPane.add(c = new Label(), 2);
        } catch (IllegalArgumentException e) {
            iae = true;
        }
        assertTrue(iae);
        iae = false;
        scrollPane.add(c = new Label(), 0);
        assertEquals(1, scrollPane.getComponentCount());
        assertSame(c, scrollPane.getComponent(0));

    }

    public final void testSetLayout() {
        assertNull(scrollPane.getLayout());
        boolean error = false;
        try {
            scrollPane.setLayout(new BorderLayout());
        } catch (AWTError err) {
            error = true;
        }
        assertTrue(error);
        assertNull(scrollPane.getLayout());
    }

    /*
     * Class under test for void ScrollPane()
     */
    public final void testScrollPane() {
        assertNotNull(scrollPane);
        Dimension defaultSize = new Dimension(100, 100);
        assertEquals(defaultSize, scrollPane.getSize());
        assertEquals(defaultSize, scrollPane.getMinimumSize());
        assertEquals(defaultSize, scrollPane.getPreferredSize());
    }

    /*
     * Class under test for void ScrollPane(int)
     */
    public final void testScrollPaneint() {
        int policy = -1;
        boolean iae = false;
        scrollPane = null;
        try {
            scrollPane = new ScrollPane(policy);
        } catch (IllegalArgumentException ex) {
            iae = true;
        }
        assertTrue(iae);
        assertNull(scrollPane);
        scrollPane = new ScrollPane(policy = ScrollPane.SCROLLBARS_ALWAYS);
        assertNotNull(scrollPane);
        assertEquals(policy, scrollPane.getScrollbarDisplayPolicy());
        assertTrue(scrollPane.isWheelScrollingEnabled());
        scrollPane = new ScrollPane(policy = ScrollPane.SCROLLBARS_NEVER);
        assertEquals(policy, scrollPane.getScrollbarDisplayPolicy());
        assertTrue(scrollPane.isWheelScrollingEnabled());
        assertEquals(new Dimension(100, 100), scrollPane.getSize());
    }

    public final void testEventTypeEnabled() {
        assertTrue(scrollPane.eventTypeEnabled(MouseEvent.MOUSE_WHEEL));
        assertFalse(scrollPane.eventTypeEnabled(MouseEvent.MOUSE_PRESSED));
    }

    public final void testGetHAdjustable() {
        assertTrue(scrollPane.getHAdjustable() instanceof ScrollPaneAdjustable);
    }

    public final void testGetHScrollbarHeight() {
        assertEquals(0, scrollPane.getHScrollbarHeight());
    }

    public final void testGetScrollPosition() {
        boolean npe = false;
        try {
            scrollPane.getScrollPosition();
        } catch (NullPointerException e) {
            npe = true;
        }
        assertTrue(npe);
        scrollPane.add(new Button());
        assertEquals(new Point(), scrollPane.getScrollPosition());
    }

    public final void testGetScrollbarDisplayPolicy() {
        assertEquals(ScrollPane.SCROLLBARS_AS_NEEDED,
                     scrollPane.getScrollbarDisplayPolicy());
    }

    public final void testGetVAdjustable() {
        assertTrue(scrollPane.getVAdjustable() instanceof ScrollPaneAdjustable);
    }

    public final void testGetVScrollbarWidth() {
        assertEquals(0, scrollPane.getVScrollbarWidth());
    }

    public final void testGetViewportSize() {
        Dimension size = scrollPane.getSize();
        Insets ins = scrollPane.getInsets();
        Dimension viewSize = size.getSize();
        viewSize.width -= ins.left + ins.right;
        viewSize.height -= ins.top + ins.bottom;
        assertEquals(viewSize, scrollPane.getViewportSize());
    }

    public final void testIsWheelScrollingEnabled() {
        assertTrue(scrollPane.isWheelScrollingEnabled());
    }

    /*
     * Class under test for void setScrollPosition(Point)
     */
    public final void testSetScrollPositionPoint() {
        boolean npe = false;
        try {
            scrollPane.setScrollPosition(new Point());
        } catch (NullPointerException e) {
            npe = true;
        }
        assertTrue(npe);
        Component c = new Button();
        Dimension prefSize = new Dimension(500, 300);
        c.setPreferredSize(prefSize);
        scrollPane.add(c);
        scrollPane.doLayout();
        Point pos = new Point(250, 150);
        scrollPane.setScrollPosition(pos);
        assertEquals(pos, scrollPane.getScrollPosition());
        scrollPane.setScrollPosition(pos = new Point(-10, 15));
        pos.x = 0;
        assertEquals(pos, scrollPane.getScrollPosition());
        scrollPane.setScrollPosition(pos = new Point(13, 250));
        int vis = scrollPane.getViewportSize().height;
        pos.y = c.getSize().height - vis;
        assertEquals(pos, scrollPane.getScrollPosition());
    }

    public final void testSetWheelScrollingEnabled() {
        scrollPane.setWheelScrollingEnabled(false);
        assertFalse(scrollPane.isWheelScrollingEnabled());
        assertFalse(scrollPane.eventTypeEnabled(MouseEvent.MOUSE_WHEEL));
        scrollPane.setWheelScrollingEnabled(true);
        assertTrue(scrollPane.isWheelScrollingEnabled());
        assertTrue(scrollPane.eventTypeEnabled(MouseEvent.MOUSE_WHEEL));
    }

}
