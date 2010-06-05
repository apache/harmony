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
 * @author Alexander T. Simbirtsev
 * Created on 22.12.2004

 */
package javax.swing;

import java.awt.AWTError;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.border.EmptyBorder;

public class OverlayLayoutTest extends SwingTestCase {
    protected OverlayLayout layout = null;

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(OverlayLayoutTest.class);
    }

    public void testOverlayLayout() {
        Container container = new JPanel();
        boolean thrown = false;
        try {
            layout = new OverlayLayout(container);
        } catch (AWTError e) {
            thrown = true;
        }
        assertFalse("No exception thrown", thrown);
        thrown = false;
        try {
            layout = new OverlayLayout(null);
        } catch (AWTError e) {
            thrown = true;
        }
        assertFalse("No exception thrown", thrown);
    }

    /*
     * Class under test for void addLayoutComponent(Component, Object)
     */
    public void testAddLayoutComponentComponentObject() {
        Container container = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        JComponent component4 = new JPanel();
        OverlayLayout layout = new OverlayLayout(container);
        container.add(component1);
        container.add(component2);
        component1.setMinimumSize(new Dimension(50, 50));
        component2.setMinimumSize(new Dimension(70, 80));
        component3.setMinimumSize(new Dimension(90, 70));
        component4.setMinimumSize(new Dimension(80, 120));
        assertEquals("Sizes ", new Dimension(70, 80), layout.minimumLayoutSize(container));
        container.add(component3);
        assertEquals("Sizes ", new Dimension(70, 80), layout.minimumLayoutSize(container));
        layout.addLayoutComponent(component3, null);
        assertEquals("Sizes ", new Dimension(90, 80), layout.minimumLayoutSize(container));
        container.add(component4);
        assertEquals("Sizes ", new Dimension(90, 80), layout.minimumLayoutSize(container));
        layout.addLayoutComponent(component4, null);
        assertEquals("Sizes ", new Dimension(90, 120), layout.minimumLayoutSize(container));
    }

    public void testPreferredLayoutSize() {
        JComponent container = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        JComponent component4 = new JPanel();
        component1.setPreferredSize(new Dimension(41, 26));
        component2.setPreferredSize(new Dimension(48, 26));
        component3.setPreferredSize(new Dimension(55, 26));
        component4.setPreferredSize(new Dimension(62, 26));
        layout = new OverlayLayout(container);
        container.add(component1);
        container.add(component2);
        assertEquals("Preferred size: ", new Dimension(48, 26), layout
                .preferredLayoutSize(container));
        component1.setPreferredSize(new Dimension(50, 50));
        component2.setPreferredSize(new Dimension(70, 120));
        component3.setPreferredSize(new Dimension(90, 150));
        component4.setPreferredSize(new Dimension(80, 90));
        layout = new OverlayLayout(container);
        assertEquals("Preferred size: ", new Dimension(70, 120), layout
                .preferredLayoutSize(container));
        container.add(component3);
        layout = new OverlayLayout(container);
        assertEquals("Preferred size: ", new Dimension(90, 150), layout
                .preferredLayoutSize(container));
        container.add(component4);
        layout = new OverlayLayout(container);
        assertEquals("Preferred size: ", new Dimension(90, 150), layout
                .preferredLayoutSize(container));
        container = new JPanel();
        container.add(component1);
        container.add(component2);
        component1.setAlignmentX(0.75f);
        component2.setAlignmentY(0.75f);
        component3.setAlignmentX(0.25f);
        component4.setAlignmentY(0.25f);
        layout = new OverlayLayout(container);
        assertEquals("Preferred size: ", new Dimension(72, 120), layout
                .preferredLayoutSize(container));
        container.add(component3);
        layout = new OverlayLayout(container);
        assertEquals("Preferred size: ", new Dimension(105, 165), layout
                .preferredLayoutSize(container));
        container.add(component4);
        layout = new OverlayLayout(container);
        container.setBorder(new EmptyBorder(20, 20, 20, 20));
        assertEquals("Preferred size: ", new Dimension(148, 205), layout
                .preferredLayoutSize(container));
    }

    public void testMinimumLayoutSize() {
        JComponent container = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        JComponent component4 = new JPanel();
        component1.setMinimumSize(new Dimension(41, 26));
        component2.setMinimumSize(new Dimension(48, 26));
        component3.setMinimumSize(new Dimension(55, 26));
        component4.setMinimumSize(new Dimension(62, 26));
        layout = new OverlayLayout(container);
        container.add(component1);
        container.add(component2);
        assertEquals("Minimum size: ", new Dimension(48, 26), layout
                .minimumLayoutSize(container));
        component1.setMinimumSize(new Dimension(50, 50));
        component2.setMinimumSize(new Dimension(70, 120));
        component3.setMinimumSize(new Dimension(90, 150));
        component4.setMinimumSize(new Dimension(80, 90));
        layout = new OverlayLayout(container);
        assertEquals("Minimum size: ", new Dimension(70, 120), layout
                .minimumLayoutSize(container));
        container.add(component3);
        layout = new OverlayLayout(container);
        assertEquals("Minimum size: ", new Dimension(90, 150), layout
                .minimumLayoutSize(container));
        container.add(component4);
        layout = new OverlayLayout(container);
        assertEquals("Minimum size: ", new Dimension(90, 150), layout
                .minimumLayoutSize(container));
        container = new JPanel();
        container.add(component1);
        container.add(component2);
        component1.setAlignmentX(0.75f);
        component2.setAlignmentY(0.75f);
        component3.setAlignmentX(0.25f);
        component4.setAlignmentY(0.25f);
        layout = new OverlayLayout(container);
        assertEquals("Minimum size: ", new Dimension(72, 120), layout
                .minimumLayoutSize(container));
        container.add(component3);
        layout = new OverlayLayout(container);
        assertEquals("Minimum size: ", new Dimension(105, 165), layout
                .minimumLayoutSize(container));
        container.add(component4);
        layout = new OverlayLayout(container);
        container.setBorder(new EmptyBorder(20, 20, 20, 20));
        assertEquals("Minimum size: ", new Dimension(148, 205), layout
                .minimumLayoutSize(container));
    }

    public void testMaximumLayoutSize() {
        JComponent container = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        JComponent component4 = new JPanel();
        component1.setMaximumSize(new Dimension(41, 26));
        component2.setMaximumSize(new Dimension(48, 26));
        component3.setMaximumSize(new Dimension(55, 26));
        component4.setMaximumSize(new Dimension(62, 26));
        layout = new OverlayLayout(container);
        container.add(component1);
        container.add(component2);
        assertEquals("Maximum size: ", new Dimension(48, 26), layout
                .maximumLayoutSize(container));
        component1.setMaximumSize(new Dimension(50, 50));
        component2.setMaximumSize(new Dimension(70, 120));
        component3.setMaximumSize(new Dimension(90, 150));
        component4.setMaximumSize(new Dimension(80, 90));
        layout = new OverlayLayout(container);
        assertEquals("Maximum size: ", new Dimension(70, 120), layout
                .maximumLayoutSize(container));
        container.add(component3);
        layout = new OverlayLayout(container);
        assertEquals("Maximum size: ", new Dimension(90, 150), layout
                .maximumLayoutSize(container));
        container.add(component4);
        layout = new OverlayLayout(container);
        assertEquals("Maximum size: ", new Dimension(90, 150), layout
                .maximumLayoutSize(container));
        container = new JPanel();
        container.add(component1);
        container.add(component2);
        component1.setAlignmentX(0.75f);
        component2.setAlignmentY(0.75f);
        component3.setAlignmentX(0.25f);
        component4.setAlignmentY(0.25f);
        layout = new OverlayLayout(container);
        assertEquals("Maximum size: ", new Dimension(72, 120), layout
                .maximumLayoutSize(container));
        container.add(component3);
        layout = new OverlayLayout(container);
        assertEquals("Maximum size: ", new Dimension(105, 165), layout
                .maximumLayoutSize(container));
        container.add(component4);
        container.setBorder(new EmptyBorder(20, 20, 20, 20));
        layout = new OverlayLayout(container);
        assertEquals("Maximum size: ", new Dimension(148, 205), layout
                .maximumLayoutSize(container));
    }

    /*
     * Class under test for void addLayoutComponent(String, Component)
     */
    public void testAddLayoutComponentStringComponent() {
        Container container = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        JComponent component4 = new JPanel();
        component1.setMinimumSize(new Dimension(34, 26));
        component2.setMinimumSize(new Dimension(34, 26));
        component3.setMinimumSize(new Dimension(34, 26));
        component4.setMinimumSize(new Dimension(34, 26));
        component1.setPreferredSize(new Dimension(34, 26));
        component2.setPreferredSize(new Dimension(34, 26));
        component3.setPreferredSize(new Dimension(34, 26));
        component4.setPreferredSize(new Dimension(34, 26));
        component1.setMaximumSize(new Dimension(34, 26));
        component2.setMaximumSize(new Dimension(34, 26));
        component3.setMaximumSize(new Dimension(34, 26));
        component4.setMaximumSize(new Dimension(34, 26));
        OverlayLayout layout = new OverlayLayout(container);
        container.add(component1);
        container.add(component2);
        component1.setMinimumSize(new Dimension(50, 50));
        component2.setMinimumSize(new Dimension(70, 80));
        component3.setMinimumSize(new Dimension(90, 70));
        component4.setMinimumSize(new Dimension(80, 120));
        assertEquals("Sizes ", new Dimension(70, 80), layout.minimumLayoutSize(container));
        container.add(component3);
        assertEquals("Sizes ", new Dimension(70, 80), layout.minimumLayoutSize(container));
        layout.addLayoutComponent("name", component3);
        assertEquals("Sizes ", new Dimension(90, 80), layout.minimumLayoutSize(container));
        container.add(component4);
        assertEquals("Sizes ", new Dimension(90, 80), layout.minimumLayoutSize(container));
        layout.addLayoutComponent("name", component4);
        assertEquals("Sizes ", new Dimension(90, 120), layout.minimumLayoutSize(container));
    }

    public void testLayoutContainer1() {
        JComponent container = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        JComponent component4 = new JPanel();
        component4.setMinimumSize(new Dimension(41, 26));
        component3.setMinimumSize(new Dimension(48, 26));
        component2.setMinimumSize(new Dimension(55, 26));
        component1.setMinimumSize(new Dimension(62, 26));
        component4.setPreferredSize(new Dimension(41, 26));
        component3.setPreferredSize(new Dimension(48, 26));
        component2.setPreferredSize(new Dimension(55, 26));
        component1.setPreferredSize(new Dimension(62, 26));
        component4.setMaximumSize(new Dimension(41, 26));
        component3.setMaximumSize(new Dimension(48, 26));
        component2.setMaximumSize(new Dimension(55, 26));
        component1.setMaximumSize(new Dimension(62, 26));
        layout = new OverlayLayout(container);
        container.setLayout(layout);
        container.setBorder(new EmptyBorder(20, 20, 20, 20));
        container.add(component1);
        container.add(component2);
        container.add(component3);
        container.add(component4);
        component1.setAlignmentX(0);
        component2.setAlignmentX(0.2f);
        component3.setAlignmentX(0.3f);
        component4.setAlignmentX(1);
        component1.setAlignmentY(0);
        component2.setAlignmentY(0.2f);
        component3.setAlignmentY(0.3f);
        component4.setAlignmentY(1);
        JDialog window = new JDialog();
        window.getContentPane().add(container);
        window.pack();
        assertEquals("Container's minimum requirements", new Dimension(143, 92), layout
                .minimumLayoutSize(container));
        assertEquals("Container's preferred requirements", new Dimension(143, 92), layout
                .preferredLayoutSize(container));
        assertEquals("Container's maximum requirements", new Dimension(143, 92), layout
                .maximumLayoutSize(container));
        assertEquals("Component1 location ", new Point(61, 46), component1.getLocation());
        assertEquals("Component2 location ", new Point(50, 41), component2.getLocation());
        assertEquals("Component3 location ", new Point(47, 39), component3.getLocation());
        assertEquals("Component4 location ", new Point(20, 20), component4.getLocation());
    }

    @SuppressWarnings("deprecation")
    public void testLayoutContainer2() {
        JWindow window = new JWindow();
        JComponent panel = new JPanel();
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        JComponent panel3 = new JPanel();
        panel1.setPreferredSize(new Dimension(100, 100));
        panel2.setPreferredSize(new Dimension(100, 100));
        panel3.setPreferredSize(new Dimension(100, 100));
        panel1.setMaximumSize(new Dimension(100, 100));
        panel2.setMaximumSize(new Dimension(100, 100));
        panel3.setMaximumSize(new Dimension(100, 100));
        panel1.setMinimumSize(new Dimension(100, 100));
        panel2.setMinimumSize(new Dimension(100, 100));
        panel3.setMinimumSize(new Dimension(100, 100));
        panel.setPreferredSize(new Dimension(300, 300));
        panel.setLayout(new OverlayLayout(panel));
        panel1.setAlignmentX(1.0f);
        panel2.setAlignmentX(0.3f);
        panel3.setAlignmentX(0.0f);
        panel1.setAlignmentY(1.0f);
        panel2.setAlignmentY(0.3f);
        panel3.setAlignmentY(0.0f);
        panel.add(panel1);
        panel.add(panel2);
        panel.add(panel3);
        window.setSize(150, 150);
        window.getContentPane().add(panel);
        window.show();
        assertEquals("component's location ", new Point(0, 0), panel1.getLocation());
        assertEquals("component's location ", new Point(45, 45), panel2.getLocation());
        assertEquals("component's location ", new Point(75, 75), panel3.getLocation());
    }

    public void testInvalidateLayout() {
        Container container = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        JComponent component4 = new JPanel();
        OverlayLayout layout = new OverlayLayout(container);
        container.add(component1);
        container.add(component2);
        component1.setMinimumSize(new Dimension(50, 50));
        component2.setMinimumSize(new Dimension(70, 80));
        component3.setMinimumSize(new Dimension(90, 70));
        component4.setMinimumSize(new Dimension(80, 120));
        assertEquals("Sizes ", new Dimension(70, 80), layout.minimumLayoutSize(container));
        container.add(component3);
        layout.invalidateLayout(container);
        assertEquals("Sizes ", new Dimension(90, 80), layout.minimumLayoutSize(container));
        container.add(component4);
        layout.invalidateLayout(container);
        assertEquals("Sizes ", new Dimension(90, 120), layout.minimumLayoutSize(container));
    }

    public void testGetLayoutAlignmentY() {
        Container container = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        JComponent component4 = new JPanel();
        component1.setPreferredSize(new Dimension(10, 10));
        component2.setPreferredSize(new Dimension(10, 10));
        component3.setPreferredSize(new Dimension(10, 10));
        component4.setPreferredSize(new Dimension(10, 10));
        component1.setMinimumSize(new Dimension(10, 10));
        component2.setMinimumSize(new Dimension(10, 10));
        component3.setMinimumSize(new Dimension(10, 10));
        component4.setMinimumSize(new Dimension(10, 10));
        component1.setMaximumSize(new Dimension(10, 10));
        component2.setMaximumSize(new Dimension(10, 10));
        component3.setMaximumSize(new Dimension(10, 10));
        component4.setMaximumSize(new Dimension(10, 10));
        OverlayLayout layout = new OverlayLayout(container);
        container.add(component1);
        container.add(component2);
        float value1 = 0.02f;
        float value2 = 0.47f;
        float value3 = 0.51f;
        float value4 = 0.94f;
        component1.setAlignmentY(value1);
        component2.setAlignmentY(value2);
        component3.setAlignmentY(value3);
        component4.setAlignmentY(value4);
        assertEquals("Alignment ", 0.2857143f, layout.getLayoutAlignmentY(container), 1e-5f);
        container.add(component3);
        layout = new OverlayLayout(container);
        assertEquals("Alignment ", 0.333333f, layout.getLayoutAlignmentY(container), 1e-5f);
        container.add(component4);
        layout = new OverlayLayout(container);
        assertEquals("Alignment ", 0.4736842f, layout.getLayoutAlignmentY(container), 1e-5f);
    }

    public void testGetLayoutAlignmentX() {
        Container container = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        JComponent component4 = new JPanel();
        component1.setMinimumSize(new Dimension(34, 26));
        component2.setMinimumSize(new Dimension(34, 26));
        component3.setMinimumSize(new Dimension(34, 26));
        component4.setMinimumSize(new Dimension(34, 26));
        component1.setPreferredSize(new Dimension(34, 26));
        component2.setPreferredSize(new Dimension(34, 26));
        component3.setPreferredSize(new Dimension(34, 26));
        component4.setPreferredSize(new Dimension(34, 26));
        component1.setMaximumSize(new Dimension(34, 26));
        component2.setMaximumSize(new Dimension(34, 26));
        component3.setMaximumSize(new Dimension(34, 26));
        component4.setMaximumSize(new Dimension(34, 26));
        OverlayLayout layout = new OverlayLayout(container);
        container.add(component1);
        container.add(component2);
        float value1 = 0.02f;
        float value2 = 0.47f;
        float value3 = 0.51f;
        float value4 = 0.94f;
        component1.setAlignmentX(value1);
        component2.setAlignmentX(value2);
        component3.setAlignmentX(value3);
        component4.setAlignmentX(value4);
        assertEquals("Alignment ", 0.306122f, layout.getLayoutAlignmentX(container), 1e-5f);
        container.add(component3);
        layout = new OverlayLayout(container);
        assertEquals("Alignment ", 0.333333f, layout.getLayoutAlignmentX(container), 1e-5f);
        container.add(component4);
        layout = new OverlayLayout(container);
        assertEquals("Alignment ", 0.4769230f, layout.getLayoutAlignmentX(container), 1e-5f);
    }

    public void testRemoveLayoutComponent() {
        Container container = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        JComponent component4 = new JPanel();
        OverlayLayout layout = new OverlayLayout(container);
        container.add(component1);
        container.add(component2);
        container.add(component3);
        container.add(component4);
        component1.setMinimumSize(new Dimension(50, 50));
        component2.setMinimumSize(new Dimension(70, 80));
        component3.setMinimumSize(new Dimension(90, 70));
        component4.setMinimumSize(new Dimension(80, 120));
        assertEquals("Sizes ", new Dimension(90, 120), layout.minimumLayoutSize(container));
        container.remove(component4);
        assertEquals("Sizes ", new Dimension(90, 120), layout.minimumLayoutSize(container));
        container.add(component4);
        layout.removeLayoutComponent(component4);
        container.remove(component4);
        assertEquals("Sizes ", new Dimension(90, 80), layout.minimumLayoutSize(container));
        container.remove(component3);
        assertEquals("Sizes ", new Dimension(90, 80), layout.minimumLayoutSize(container));
        container.add(component3);
        layout.removeLayoutComponent(component3);
        container.remove(component3);
        assertEquals("Sizes ", new Dimension(70, 80), layout.minimumLayoutSize(container));
    }

    // Layout sharing testcases and so on
    public void testSharingLayout() {
        Container container = new JPanel();
        layout = new OverlayLayout(container);
        boolean thrown = false;
        String text = null;
        try {
            layout.getLayoutAlignmentY(new JPanel());
        } catch (AWTError e) {
            thrown = true;
            text = e.getMessage();
        }
        assertTrue("AWTError exception thrown", thrown);
        assertEquals(text, "OverlayLayout can't be shared");
        thrown = false;
        text = null;
        try {
            layout.getLayoutAlignmentX(new JPanel());
        } catch (AWTError e) {
            thrown = true;
            text = e.getMessage();
        }
        assertTrue("AWTError exception thrown", thrown);
        assertEquals(text, "OverlayLayout can't be shared");
        thrown = false;
        text = null;
        try {
            layout.invalidateLayout(new JPanel());
        } catch (AWTError e) {
            thrown = true;
            text = e.getMessage();
        }
        assertTrue("AWTError exception thrown", thrown);
        assertEquals(text, "OverlayLayout can't be shared");
        thrown = false;
        text = null;
        try {
            layout.maximumLayoutSize(new JPanel());
        } catch (AWTError e) {
            thrown = true;
            text = e.getMessage();
        }
        assertTrue("AWTError exception thrown", thrown);
        assertEquals(text, "OverlayLayout can't be shared");
        thrown = false;
        text = null;
        try {
            layout.layoutContainer(new JPanel());
        } catch (AWTError e) {
            thrown = true;
            text = e.getMessage();
        }
        assertTrue("AWTError exception thrown", thrown);
        assertEquals(text, "OverlayLayout can't be shared");
        thrown = false;
        text = null;
        try {
            layout.preferredLayoutSize(new JPanel());
        } catch (AWTError e) {
            thrown = true;
            text = e.getMessage();
        }
        assertTrue("AWTError exception thrown", thrown);
        assertEquals(text, "OverlayLayout can't be shared");
        thrown = false;
        text = null;
        try {
            layout.minimumLayoutSize(new JPanel());
        } catch (AWTError e) {
            thrown = true;
            text = e.getMessage();
        }
        assertTrue("AWTError exception thrown", thrown);
        assertEquals(text, "OverlayLayout can't be shared");
    }

    public void testWriteObject() throws IOException {
        Container container = new JPanel();
        OverlayLayout layout1 = new OverlayLayout(container);
        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(fo);
        so.writeObject(layout1);
        so.flush();
    }

    public void testReadObject() throws ClassNotFoundException, IOException {
        Container container = new Panel();
        OverlayLayout layout1 = new OverlayLayout(container);
        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(fo);
        so.writeObject(layout1);
        so.flush();
        InputStream fi = new ByteArrayInputStream(fo.toByteArray());
        ObjectInputStream si = new ObjectInputStream(fi);
        OverlayLayout resurrectedLayout = (OverlayLayout) si.readObject();
        assertNotNull(resurrectedLayout);
    }
}
