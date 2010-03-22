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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class CellRendererPaneTest extends SwingTestCase {
    private CellRendererPane pane;

    public CellRendererPaneTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        pane = new CellRendererPane();
    }

    @Override
    protected void tearDown() throws Exception {
        pane = null;
    }

    public void testCellRendererPane() throws Exception {
        assertNull(pane.accessibleContext);
    }

    public void testAddImpl() throws Exception {
        JComponent c1 = new JPanel();
        JComponent c2 = new JPanel();
        JComponent c3 = new JPanel();
        assertEquals(0, pane.getComponentCount());
        pane.add(c1);
        assertEquals(1, pane.getComponentCount());
        assertSame(c1, pane.getComponent(0));
        pane.add(c2);
        assertEquals(2, pane.getComponentCount());
        assertSame(c1, pane.getComponent(0));
        assertSame(c2, pane.getComponent(1));
        pane.add(c3);
        assertEquals(3, pane.getComponentCount());
        assertSame(c1, pane.getComponent(0));
        assertSame(c2, pane.getComponent(1));
        assertSame(c3, pane.getComponent(2));
        pane.add(c1);
        assertEquals(3, pane.getComponentCount());
        assertSame(c1, pane.getComponent(0));
        assertSame(c2, pane.getComponent(1));
        assertSame(c3, pane.getComponent(2));
    }

    public void testGetAccessibleContext() throws Exception {
        assertNull(pane.accessibleContext);
        assertNotNull(pane.getAccessibleContext());
        assertEquals(pane.accessibleContext, pane.getAccessibleContext());
    }

    public void testInvalidate() throws Exception {
        final Marker parentInvalidation = new Marker();
        JPanel parent = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            public void invalidate() {
                super.invalidate();
                parentInvalidation.setOccurred();
            }
        };
        final Marker childInvalidation = new Marker();
        JPanel child = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            public void invalidate() {
                super.invalidate();
                childInvalidation.setOccurred();
            }
        };
        parent.add(pane);
        pane.add(child);
        parent.validate();
        parentInvalidation.setOccurred(false);
        childInvalidation.setOccurred(false);
        child.invalidate();
        assertTrue(childInvalidation.isOccurred());
        assertFalse(parentInvalidation.isOccurred());
    }

    public void testPaintComponent() throws Exception {
        final List<Rectangle> boundsChanges = new ArrayList<Rectangle>();
        final Marker dbMarker = new Marker();
        final Marker validationMarker = new Marker();
        JPanel component = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            public void setBounds(final int x, final int y, final int w, final int h) {
                boundsChanges.add(new Rectangle(x, y, w, h));
                super.setBounds(x, y, w, h);
            }

            @Override
            public void validate() {
                validationMarker.setOccurred();
                super.validate();
            }

            @Override
            public void paint(final Graphics g) {
                super.paint(g);
                dbMarker.setOccurred(isDoubleBuffered());
            }
        };
        component.setDoubleBuffered(true);
        component.invalidate();
        Graphics g = createTestGraphics();
        g.setClip(0, 0, 100, 100);
        pane.paintComponent(g, component, new JPanel(), 20, 30, 40, 50);
        assertEquals(pane, component.getParent());
        assertFalse(dbMarker.isOccurred());
        assertTrue(component.isDoubleBuffered());
        assertEquals(2, boundsChanges.size());
        assertEquals(new Rectangle(20, 30, 40, 50), boundsChanges.get(0));
        assertEquals(new Rectangle(-40, -50, 0, 0), boundsChanges.get(1));
        assertEquals(new Rectangle(-40, -50, 0, 0), component.getBounds());
        assertFalse(validationMarker.isOccurred());
        pane.paintComponent(g, component, new JPanel(), 20, 30, 40, 50, true);
        assertTrue(validationMarker.isOccurred());
    }

    public void testPaintComponent1() {
        CellRendererPane c = new CellRendererPane();
        try {
            c.paintComponent(null, null, null, 0, 0, 0, 0, false);
            assertTrue(true);
        } catch (NullPointerException e) {
            fail(e + " - was thrown");
        }
    }
}
