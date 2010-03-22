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
package javax.swing;

import java.awt.Canvas;
import java.awt.Component;
import java.util.Hashtable;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

public class JLayeredPaneTest extends SwingTestCase {
    /*
     * components to add to the container
     * they are supposed to have next order:
     *         layer 5     |     4     |   1
     * --------------------------------------
     * index | 0   1   2   |   3   4   |   5
     * comp  |5_0 5_1 5_2  |  4_0 4_1  |  1_0
     * --------------------------------------
     */
    private static Component c5_0;

    private static Component c5_1;

    private static Component c5_2;

    private static Component c4_0;

    private static Component c4_1;

    private static Component c1_0;

    private static int i4_0 = 3;

    /*
     * This class overload protected methods with public methods
     */
    private class TestLayeredPane extends JLayeredPane {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unchecked")
        public Hashtable getGetComponentToLayer() {
            return getComponentToLayer();
        }

        @Override
        public Integer getObjectForLayer(final int layer) {
            return super.getObjectForLayer(layer);
        }

        @Override
        public int insertIndexForLayer(final int layer, final int position) {
            return super.insertIndexForLayer(layer, position);
        }

        @Override
        public String paramString() {
            return super.paramString();
        }
    }

    private JLayeredPane layered;
    static {
        c5_0 = new JPanel();
        c5_1 = new JPanel();
        c5_2 = new JPanel();
        c4_0 = new JPanel();
        c4_1 = new JPanel();
        c1_0 = new JPanel();
    }

    /*
     * add some components to layered
     */
    private void addComponents() {
        layered.add(c5_0, new Integer(5), -1);
        layered.add(c5_1, new Integer(5), -1);
        layered.add(c5_2, new Integer(5), -1);
        layered.add(c4_0, new Integer(4), -1);
        layered.add(c4_1, new Integer(4), -1);
        layered.add(c1_0, new Integer(1), -1);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        layered = new JLayeredPane();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Constructor for JLayeredPaneTest.
     * @param arg0
     */
    public JLayeredPaneTest(final String arg0) {
        super(arg0);
    }

    /*
     * Class under test for void remove(int)
     */
    public void testRemoveint() {
        addComponents();
        layered.remove(i4_0);
        assertFalse(layered.isAncestorOf(c4_0));
        assertEquals(5, layered.getComponentCount());
        // remove() with incorrect index throws ArrayIndexOutOfBoundsException
    }

    /*
     * Class under test for void removeAll()
     */
    public void testRemoveAll() {
        addComponents();
        Component comp = new Canvas();
        layered.setLayer(comp, 5);
        // comp is not in JLayeredPane
        assertTrue("not empty", layered.getComponentCount() != 0);
        assertFalse("componentToLayer is not empty", layered.getComponentToLayer().isEmpty());
        layered.removeAll();
        assertTrue("empty", layered.getComponentCount() == 0);
        assertFalse("componentToLayer is not empty", layered.getComponentToLayer().isEmpty());
        // comp is in JLayeredPane
        layered.add(comp);
        layered.removeAll();
        assertTrue("componentToLayer is empty", layered.getComponentToLayer().isEmpty());
    }

    public void testIsOptimizedDrawingEnabled() {
        // no components
        assertTrue(layered.isOptimizedDrawingEnabled());
        // 1 component
        layered.add(new JPanel());
        assertTrue(layered.isOptimizedDrawingEnabled());
        // many components
        addComponents();
        assertFalse(layered.isOptimizedDrawingEnabled());
    }

    public void testHighestLayer() {
        assertEquals(0, layered.highestLayer());
        addComponents();
        assertEquals(5, layered.highestLayer());
    }

    public void testLowestLayer() {
        assertEquals(0, layered.lowestLayer());
        addComponents();
        assertEquals(1, layered.lowestLayer());
    }

    public void testJLayeredPane() {
        // nothing to test
    }

    public void testGetComponentCountInLayer() {
        assertEquals(0, layered.getComponentCountInLayer(5));
        addComponents();
        assertEquals(3, layered.getComponentCountInLayer(5));
        assertEquals(2, layered.getComponentCountInLayer(4));
        assertEquals(1, layered.getComponentCountInLayer(1));
        assertEquals(0, layered.getComponentCountInLayer(6));
        assertEquals(0, layered.getComponentCountInLayer(2));
    }

    /*
     * Class under test for AccessibleContext getAccessibleContext()
     */
    public void testGetAccessibleContext() {
        AccessibleContext c = layered.getAccessibleContext();
        assertTrue("instanceof AccessibleJLayeredPane",
                c instanceof JLayeredPane.AccessibleJLayeredPane);
        assertTrue("AccessibleRole is ok", c.getAccessibleRole() == AccessibleRole.LAYERED_PANE);
        assertNull("AccessibleName is ok", c.getAccessibleName());
        assertNull("AccessibleDescription is ok", c.getAccessibleDescription());
        assertTrue("AccessibleChildrenCount == 0", c.getAccessibleChildrenCount() == 0);
    }

    /*
     * Class under test for void paint(Graphics)
     */
    public void testPaint() {
        // Note: cannot test paint code here
    }

    /*
     * Class under test for void setLayer(Component, int, int)
     */
    public void testSetLayerComponentintint() {
        Component jcomp = new JPanel();
        Component comp = new Canvas();
        // test setLayer() for JComponent outside the container
        layered.setLayer(jcomp, 4, 0);
        assertEquals(4, layered.getLayer(jcomp));
        assertEquals(4, JLayeredPane.getLayer((JComponent) jcomp));
        assertFalse(layered.isAncestorOf(jcomp));
        // test setLayer() for Component outside the container
        layered.setLayer(comp, 4, 5);
        assertEquals(4, layered.getLayer(comp));
        assertFalse(layered.isAncestorOf(comp));
        addComponents();
        // to the start of existing layer
        layered.setLayer(c5_2, 1, 0);
        assertEquals(1, layered.getLayer(c5_2));
        assertEquals(4, layered.getIndexOf(c5_2));
        // in the middle of existing layer
        layered.setLayer(c5_2, 5, 1);
        assertEquals(5, layered.getLayer(c5_2));
        assertEquals(1, layered.getIndexOf(c5_2));
        // to the same position
        layered.setLayer(c1_0, 1, 1);
        assertEquals(1, layered.getLayer(c1_0));
        assertEquals(5, layered.getIndexOf(c1_0));
        // to the same index with different layer
        layered.removeAll();
        layered.add(c1_0);
        layered.setLayer(c1_0, 5, 0);
        assertEquals(5, layered.getLayer(c1_0));
    }

    /*
     * Class under test for void setPosition(Component, int)
     */
    public void testSetPositionComponentint() {
        Component comp = new JPanel();
        layered.setPosition(comp, 1);
        assertEquals(0, layered.getComponentCount());
        addComponents();
        layered.setPosition(c4_0, -1);
        assertEquals(4, layered.getIndexOf(c4_0));
        layered.setPosition(c4_0, 0);
        assertEquals(3, layered.getIndexOf(c4_0));
        layered.setPosition(c1_0, 0);
        assertEquals(5, layered.getIndexOf(c1_0));
        layered.setPosition(c1_0, -1);
        assertEquals(5, layered.getIndexOf(c1_0));
        layered.setPosition(c5_2, 100);
        assertEquals(2, layered.getIndexOf(c5_2));
        layered.setPosition(c5_0, 100);
        assertEquals(2, layered.getIndexOf(c5_0));
        layered.setPosition(c5_0, 1);
        assertEquals(1, layered.getIndexOf(c5_0));
    }

    /*
     * Class under test for void setLayer(Component, int)
     */
    public void testSetLayerComponentint() {
        Component jcomp = new JPanel();
        Component comp = new Canvas();
        // test setLayer() for JComponent outside the container
        layered.setLayer(jcomp, 4);
        assertEquals(4, layered.getLayer(jcomp));
        assertEquals(4, JLayeredPane.getLayer((JComponent) jcomp));
        assertFalse(layered.isAncestorOf(jcomp));
        // test setLayer() for Component outside the container
        layered.setLayer(comp, 4);
        assertEquals(4, layered.getLayer(comp));
        assertFalse(layered.isAncestorOf(comp));
        addComponents();
        layered.setLayer(c4_0, 3);
        assertEquals(4, layered.getIndexOf(c4_0));
        assertEquals(3, layered.getLayer(c4_0));
        layered.setLayer(c5_1, 1);
        assertEquals(5, layered.getIndexOf(c5_1));
        assertEquals(1, layered.getLayer(c5_1));
    }

    /*
     * Class under test for void moveToFront(Component)
     */
    public void testMoveToFrontComponent() {
        addComponents();
        layered.moveToFront(c4_1);
        assertEquals(3, layered.getIndexOf(c4_1));
        layered.moveToFront(c5_2);
        assertEquals(0, layered.getIndexOf(c5_2));
        layered.moveToFront(c1_0);
        assertEquals(5, layered.getIndexOf(c1_0));
        // moveToFront() does nothing if the component is not from the containter
        layered.moveToFront(new JPanel());
        assertEquals(6, layered.getComponentCount());
    }

    /*
     * Class under test for void moveToBack(Component)
     */
    public void testMoveToBackComponent() {
        addComponents();
        layered.moveToBack(c4_0);
        assertEquals(4, layered.getIndexOf(c4_0));
        layered.moveToBack(c5_0);
        assertEquals(2, layered.getIndexOf(c5_0));
        layered.moveToBack(c1_0);
        assertEquals(5, layered.getIndexOf(c1_0));
        // moveToBack() does nothing if the component is not from the containter
        layered.moveToBack(new JPanel());
        assertEquals(6, layered.getComponentCount());
        try { // Regression test for HARMONY-2279
            layered.moveToBack(null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /*
     * Class under test for int getPosition(Component)
     */
    public void testGetPositionComponent() {
        addComponents();
        assertEquals(0, layered.getPosition(c5_0));
        assertEquals(1, layered.getPosition(c5_1));
        assertEquals(2, layered.getPosition(c5_2));
        assertEquals(0, layered.getPosition(c4_0));
        assertEquals(1, layered.getPosition(c4_1));
        assertEquals(0, layered.getPosition(c1_0));
        assertEquals(-1, layered.getPosition(new JPanel()));
    }

    /*
     * Class under test for int getLayer(Component)
     */
    public void testGetLayerComponent() {
        // getLayer() for JComponent that doesn't exist in the container
        Component c = new JPanel();
        assertEquals(0, layered.getLayer(c));
        // getLayer() for JComponent that doesn't exist in the container
        // but has layer in the property
        JLayeredPane.putLayer((JComponent) c, 5);
        assertEquals(5, layered.getLayer(c));
        // getLayer() for Component that doesn't exist in the container
        c = new Canvas();
        assertEquals(0, layered.getLayer(c));
        // getLayer() for Component from the containter
        layered.add(c, new Integer(3));
        assertEquals(3, layered.getLayer(c));
        // getLayer() for Component removed from the containter
        layered.remove(c);
        assertEquals(0, layered.getLayer(c));
        // layered.getLayer((Component)null) - throws NPE
    }

    /*
     * Class under test for int getIndexOf(Component)
     */
    public void testGetIndexOfComponent() {
        Component comp = new JPanel();
        assertEquals(-1, layered.getIndexOf(comp));
        addComponents();
        assertEquals(-1, layered.getIndexOf(comp));
        assertEquals(0, layered.getIndexOf(c5_0));
        assertEquals(1, layered.getIndexOf(c5_1));
        assertEquals(2, layered.getIndexOf(c5_2));
        assertEquals(3, layered.getIndexOf(c4_0));
        assertEquals(4, layered.getIndexOf(c4_1));
        assertEquals(5, layered.getIndexOf(c1_0));
    }

    private boolean belongs(final Component c, final Component[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == c) {
                return true;
            }
        }
        return false;
    }

    /*
     * Class under test for Component[] getComponentsInLayer(int)
     */
    public void testGetComponentsInLayerint() {
        assertEquals(0, layered.getComponentsInLayer(1).length);
        addComponents();
        Component[] components;
        components = layered.getComponentsInLayer(5);
        assertEquals(3, components.length);
        assertTrue(belongs(c5_0, components));
        assertTrue(belongs(c5_1, components));
        assertTrue(belongs(c5_2, components));
        components = layered.getComponentsInLayer(4);
        assertEquals(2, components.length);
        assertTrue(belongs(c4_0, components));
        assertTrue(belongs(c4_1, components));
        components = layered.getComponentsInLayer(1);
        assertEquals(1, components.length);
        assertTrue(belongs(c1_0, components));
        assertEquals(0, layered.getComponentsInLayer(2).length);
    }

    /*
     * Class under test for javax.swing.JLayeredPane getLayeredPaneAbove(Component)
     */
    public void testGetLayeredPaneAboveComponent() {
        // can't use c5_0 and others because they have
        // previous JLayeredPane as their parent
        assertNull(JLayeredPane.getLayeredPaneAbove(new JPanel()));
        assertNull(JLayeredPane.getLayeredPaneAbove(null));
        addComponents();
        assertTrue(JLayeredPane.getLayeredPaneAbove(c5_0) == layered);
    }

    /*
     * Class under test for int getLayer(JComponent)
     */
    public void testPutGetLayerJComponent() {
        JComponent c = new JPanel();
        assertEquals(0, JLayeredPane.getLayer(c));
        JLayeredPane.putLayer(c, 5);
        assertEquals(5, JLayeredPane.getLayer(c));
    }

    /*
     * Class under test for int addImpl(Component comp, Object constraints, int index)
     */
    public void testAddImpl() {
        JComponent comp = new JPanel();
        addComponents();
        // test add() with string instead of layer
        // should be added to layer 0
        JComponent comp2 = new JPanel();
        layered.add(comp2, new Integer(0));
        layered.add(comp, "str", 0);
        assertEquals(0, JLayeredPane.getLayer(comp));
        assertEquals(6, layered.getIndexOf(comp));
        layered.remove(comp);
        layered.add(comp, "str", -1);
        assertEquals(0, JLayeredPane.getLayer(comp));
        assertEquals(7, layered.getIndexOf(comp));
        layered.remove(comp);
        layered.remove(comp2);
        // test if add() sets "layer" property of the added component
        layered.add(comp, new Integer(4));
        assertEquals(4, JLayeredPane.getLayer(comp));
        assertEquals(5, layered.getIndexOf(comp));
        layered.remove(comp);
        // test add() to the bottom of the layer
        layered.add(comp, new Integer(4), -1);
        assertEquals(5, layered.getIndexOf(comp));
        layered.remove(comp);
        // test add() on the top of the layer
        layered.add(comp, new Integer(4), 0);
        assertEquals(3, layered.getIndexOf(comp));
        layered.remove(comp);
        // test add() to the inner of the layer
        layered.add(comp, new Integer(4), 1);
        assertEquals(4, layered.getIndexOf(comp));
        layered.remove(comp);
        // test add() to the invalid position of the layer
        layered.add(comp, new Integer(4), 100);
        assertEquals(5, layered.getIndexOf(comp));
        layered.remove(comp);
        // add component that already in container: the component must be removed first
        layered.add(comp, new Integer(4), 0);
        layered.add(comp, new Integer(3), 0);
        assertEquals(7, layered.getComponentCount());
        assertEquals(5, layered.getIndexOf(comp));
        layered.remove(comp);
        // test add() with layer set in JComponent
        JLayeredPane.putLayer(comp, 5);
        layered.add(comp);
        assertEquals(3, layered.getIndexOf(comp));
    }

    /*
     * Class under test for Hashtable getComponentToLayer()
     */
    public void testGetComponentToLayer() {
        TestLayeredPane pane = new TestLayeredPane();
        assertTrue(pane.getGetComponentToLayer() != null);
    }

    /*
     * Class under test for Integer getObjectForLayer(int layer)
     */
    public void testGetObjectForLayer() {
        TestLayeredPane pane = new TestLayeredPane();
        assertEquals(5, pane.getObjectForLayer(5).intValue());
    }

    /*
     * Class under test for int insertIndexForLayer(int layer, int position)
     */
    public void testInsertIndexForLayer() {
        // it is tested in testAddImpl()
    }

    /*
     * Class under test for String paramString()
     */
    public void testParamString() {
        TestLayeredPane pane = new TestLayeredPane();
        assertTrue(pane.paramString() != null);
    }
}
