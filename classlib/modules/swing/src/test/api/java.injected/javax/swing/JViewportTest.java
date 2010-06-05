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
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Vector;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JViewport.ViewListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicViewportUI;

public class JViewportTest extends SwingTestCase {
    private JViewport port;

    private JList list;

    private TestListener listener;

    private JFrame frame;

    @Override
    public void setUp() {
        port = new JViewport();
        listener = new TestListener();
    }

    @Override
    public void tearDown() {
        port = null;
        listener = null;
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public void testAddRemoveChangeListener() {
        final List<String> test = new Vector<String>();
        assertEquals(0, port.getChangeListeners().length);
        ChangeListener listener = new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                test.add("");
            }
        };
        port.addChangeListener(listener);
        assertEquals(1, port.getChangeListeners().length);
        assertEquals(listener, port.getChangeListeners()[0]);
        assertEquals(0, test.size());
        port.fireStateChanged();
        assertEquals(1, test.size());
        port.removeChangeListener(listener);
        assertEquals(0, port.getChangeListeners().length);
    }

    public void testAddRemovePropertyChangeListener() {
        final List<String> test = new Vector<String>();
        assertEquals(0, port.getPropertyChangeListeners().length);
        PropertyChangeListener listener = new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent e) {
                test.add("");
            }
        };
        port.addPropertyChangeListener(listener);
        assertEquals(1, port.getPropertyChangeListeners().length);
        assertEquals(listener, port.getPropertyChangeListeners()[0]);
        assertEquals(0, test.size());
        port.setBackground(Color.CYAN);
        assertEquals(1, test.size());
        port.removePropertyChangeListener(listener);
        assertEquals(0, port.getPropertyChangeListeners().length);
    }

    public void testGetExtentSize() {
        assertEquals(new Dimension(0, 0), port.getExtentSize());
        Dimension d = new Dimension(3, 5);
        port.setExtentSize(d);
        assertEquals(d, port.getExtentSize());
        assertFalse(d == port.getExtentSize());
    }

    public void testGetInsets() {
        assertEquals(new Insets(0, 0, 0, 0), port.getInsets());
        assertEquals(new Insets(0, 0, 0, 0), port.getInsets(new Insets(1, 2, 3, 4)));
    }

    public void testGetScrollMode() {
        assertEquals(1, port.getScrollMode());
        port.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
        assertEquals(2, port.getScrollMode());
        port.setScrollMode(JViewport.BLIT_SCROLL_MODE);
        assertEquals(1, port.getScrollMode());
        port.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        assertEquals(0, port.getScrollMode());
        propertyChangeController = new PropertyChangeController();
        port.addPropertyChangeListener(propertyChangeController);
        port.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        assertEquals(0, port.getScrollMode());
        assertFalse(propertyChangeController.isChanged());
    }

    public void testGetUI() {
        assertNotNull(port.getUI());
        assertEquals("ViewportUI", port.getUIClassID());
    }

    public void testGetView() {
        assertNull(port.getView());
        JLabel l = new JLabel();
        port.add(l);
        assertEquals(l, port.getView());
        JPanel panel = new JPanel();
        port.add(panel);
        assertEquals(panel, port.getView());
    }

    public void testGetSetViewPosition() {
        JLabel l = new JLabel();
        l.setPreferredSize(new Dimension(200, 200));
        port.add(l);
        port.addChangeListener(listener);
        assertEquals(new Point(0, 0), port.getViewPosition());
        Point point = new Point(2, 6);
        port.setViewPosition(point);
        assertNotNull(listener.getEvent());
        assertEquals(port, listener.getEvent().getSource());
        assertEquals(point, port.getViewPosition());
        listener.reset();
        port.setViewPosition(new Point(2, 6));
        assertNull(listener.getEvent());
    }

    public void testGetViewRect() {
        JLabel l = new JLabel();
        l.setPreferredSize(new Dimension(20, 40));
        port.add(l);
        assertEquals(new Rectangle(0, 0, 0, 0), port.getViewRect());
    }

    public void testGetSetViewSize() {
        JLabel l = new JLabel();
        port.add(l);
        Dimension dimension = new Dimension(20, 50);
        l.setPreferredSize(dimension);
        assertEquals(dimension, port.getViewSize());
        dimension = new Dimension(20, 20);
        port.setViewSize(dimension);
        assertEquals(dimension, port.getViewSize());
        assertEquals(dimension, port.getView().getSize());
        dimension = new Dimension(100, 50);
        l.setSize(dimension);
        assertEquals(dimension, port.getViewSize());
    }

    public void testSetBorder() {
        try {
            port.setBorder(BorderFactory.createEmptyBorder());
            fail("must throw an exception");
        } catch (IllegalArgumentException e) {
        }
        assertNull(port.getBorder());
        port.setBorder(null);
        assertNull(port.getBorder());
    }

    public void testIsOptimizedDrawingEnabled() {
        assertFalse(port.isOptimizedDrawingEnabled());
    }

    public void testParamString() {
        assertTrue(port.paramString().indexOf(",isViewSizeSet=") > 0);
        assertTrue(port.paramString().indexOf(",lastPaintPosition=") > 0);
        assertTrue(port.paramString().indexOf(",scrollUnderway=") > 0);
    }

    @SuppressWarnings("deprecation")
    public void testScrollRectToVisible() throws Exception {
        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(300, 300));
        label.setBackground(Color.RED);
        label.setOpaque(true);
        final JScrollPane pane = new JScrollPane(label);
        //        pane.setPreferredSize(new Dimension(118, 118));
        pane.setPreferredSize(new Dimension(120, 119));
        frame = new JFrame();
        frame.getContentPane().add(pane);
        frame.pack();
        frame.show();
        port = pane.getViewport();
        assertEquals(new Dimension(100, 100), port.getExtentSize());
        port.setViewPosition(new Point(0, 0));
        port.scrollRectToVisible(new Rectangle(50, 50, 100, 100));
        assertEquals(new Point(50, 50), port.getViewPosition());
        port.setViewPosition(new Point(0, 0));
        port.scrollRectToVisible(new Rectangle(50, 50, 50, 50));
        assertEquals(new Point(0, 0), port.getViewPosition());
        port.setViewPosition(new Point(0, 0));
        port.scrollRectToVisible(new Rectangle(50, 50, 60, 60));
        assertEquals(new Point(10, 10), port.getViewPosition());
        port.setViewPosition(new Point(0, 0));
        port.scrollRectToVisible(new Rectangle(0, 0, 60, 60));
        assertEquals(new Point(0, 0), port.getViewPosition());
        port.setViewPosition(new Point(100, 100));
        port.scrollRectToVisible(new Rectangle(0, 0, 60, 60));
        assertEquals(new Point(100, 100), port.getViewPosition());
        port.setViewPosition(new Point(100, 100));
        port.scrollRectToVisible(new Rectangle(50, 50, 60, 60));
        assertEquals(new Point(110, 110), port.getViewPosition());
        port.setViewPosition(new Point(100, 100));
        port.scrollRectToVisible(new Rectangle(-50, -50, 60, 60));
        assertEquals(new Point(50, 50), port.getViewPosition());
        port.setViewPosition(new Point(100, 100));
        port.scrollRectToVisible(new Rectangle(-50, -50, 130, 130));
        assertEquals(new Point(80, 80), port.getViewPosition());
        port.setViewPosition(new Point(100, 100));
        port.scrollRectToVisible(new Rectangle(-10, -10, 130, 130));
        assertEquals(new Point(100, 100), port.getViewPosition());
        port.setViewPosition(new Point(100, 100));
        port.scrollRectToVisible(new Rectangle(-50, -50, 200, 200));
        assertEquals(new Point(100, 100), port.getViewPosition());
        port.setViewPosition(new Point(0, 0));
        port.scrollRectToVisible(new Rectangle(1000, 1000, 100, 100));
        assertEquals(new Point(200, 200), port.getViewPosition());
        port.scrollRectToVisible(new Rectangle(1000, 1000, 100, 100));
        assertEquals(new Point(200, 200), port.getViewPosition());
        port.setViewPosition(new Point(0, 0));
        port.scrollRectToVisible(new Rectangle(-1000, 1000, 100, 100));
        assertEquals(new Point(0, 200), port.getViewPosition());
        port.scrollRectToVisible(new Rectangle(-1000, 1000, 100, 100));
        assertEquals(new Point(0, 200), port.getViewPosition());
        port.setViewPosition(new Point(0, 0));
        port.scrollRectToVisible(new Rectangle(-1000, -1000, 100, 100));
        assertEquals(new Point(0, 0), port.getViewPosition());
        port.scrollRectToVisible(new Rectangle(-1000, -1000, 100, 100));
        assertEquals(new Point(0, 0), port.getViewPosition());
    }

    public void testSetUI() {
        assertEquals("ViewportUI", port.getUIClassID());
        assertTrue(port.getUI() instanceof BasicViewportUI);
    }

    public void testToViewCoordinates() {
        JLabel l = new JLabel();
        Dimension dimension = new Dimension(20, 50);
        l.setPreferredSize(dimension);
        port.add(l);
        Dimension dim = new Dimension(1000, 200);
        assertEquals(dim, port.toViewCoordinates(dim));
        Point point = new Point(1000, 200);
        assertEquals(point, port.toViewCoordinates(point));
    }

    public void testCreateLayoutManager() {
        JLabel l = new JLabel();
        Dimension dimension = new Dimension(20, 50);
        l.setPreferredSize(dimension);
        port.add(l);
        assertTrue(port.createLayoutManager() instanceof ViewportLayout);
        assertTrue((port.getComponent(0) == l));
        assertTrue((port.getView() == l));
    }

    public void testCreateViewListener() {
        ViewListener viewListener = port.createViewListener();
        assertNotNull(viewListener);
    }

    public void testGetAccessibleContext() {
        AccessibleContext accessibleContext = port.getAccessibleContext();
        assertNotNull(accessibleContext);
        assertTrue(AccessibleRole.VIEWPORT == accessibleContext.getAccessibleRole());
    }

    public void testSetView() {
        port.setView(null);
        JComponent comp = newJComponent();
        port.setView(comp);
        assertEquals(1, port.getComponentCount());
        assertEquals(comp, port.getView());
        assertEquals(comp, port.getComponent(0));
        port.setView(new JButton());
        assertEquals(1, port.getComponentCount());
    }

    public void testEnsureIndexIsVisible() throws Exception {
        list = new JList(new Object[] { "a", "b", "c" });
        JScrollPane scroller = insertListToFrame();
        assertNotNull(scroller);
        Rectangle bounds = list.getCellBounds(1, 1);
        assertFalse(list.getVisibleRect().contains(bounds));
        list.ensureIndexIsVisible(1);
        assertTrue(list.getVisibleRect().contains(bounds));
    }

    public void testIsOpaque() throws Exception {
        assertTrue(port.isOpaque());
    }

    private JScrollPane insertListToFrame() {
        return insertListToFrame(25);
    }

    private JScrollPane insertListToFrame(final int preferredHeight) {
        frame = new JFrame();
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

    private JComponent newJComponent() {
        return new JComponent() {
            private static final long serialVersionUID = 1L;
        };
    }

    private class TestListener implements ChangeListener {
        private ChangeEvent event;

        public void stateChanged(final ChangeEvent e) {
            event = e;
        }

        public ChangeEvent getEvent() {
            return event;
        }

        public void reset() {
            event = null;
        }
    }
}
