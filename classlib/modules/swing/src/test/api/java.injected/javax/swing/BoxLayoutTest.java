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

public class BoxLayoutTest extends SwingTestCase {
    protected BoxLayout layout = null;

    public void testBoxLayout() {
        Container container = new JPanel();
        boolean thrown = false;
        String text = null;
        try {
            layout = new BoxLayout(container, BoxLayout.LINE_AXIS);
        } catch (AWTError e) {
            thrown = true;
        }
        assertFalse("No exception thrown", thrown);
        thrown = false;
        text = null;
        try {
            layout = new BoxLayout(container, 300);
        } catch (AWTError e) {
            thrown = true;
            text = e.getMessage();
        }
        assertTrue("AWTError exception thrown", thrown);
        assertEquals(text, "Invalid axis");
        thrown = false;
        text = null;
        try {
            layout = new BoxLayout(null, BoxLayout.Y_AXIS);
        } catch (AWTError e) {
            thrown = true;
        }
        assertFalse("No exception thrown", thrown);
    }

    /*
     * this method is not used by this class so ther's no need to test it
     */
    public void testAddLayoutComponentComponentObject() {
    }

    public void testPreferredLayoutSize() {
        JComponent container1 = new JPanel();
        JComponent container2 = new JPanel();
        JComponent component11 = new JPanel();
        component11.setPreferredSize(new Dimension(41, 26));
        JComponent component21 = new JPanel();
        component21.setPreferredSize(new Dimension(48, 26));
        JComponent component31 = new JPanel();
        component31.setPreferredSize(new Dimension(55, 26));
        JComponent component41 = new JPanel();
        component41.setPreferredSize(new Dimension(62, 26));
        JComponent component12 = new JPanel();
        component12.setPreferredSize(new Dimension(62, 26));
        JComponent component22 = new JPanel();
        component22.setPreferredSize(new Dimension(55, 26));
        JComponent component32 = new JPanel();
        component32.setPreferredSize(new Dimension(48, 26));
        JComponent component42 = new JPanel();
        component42.setPreferredSize(new Dimension(41, 26));
        BoxLayout layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        BoxLayout layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        container1.add(component11);
        container1.add(component21);
        container2.add(component12);
        container2.add(component22);
        assertEquals("1 Sizes coinside:", new Dimension(89, 26), layout1
                .preferredLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(62, 52), layout2
                .preferredLayoutSize(container2));
        component11.setPreferredSize(new Dimension(50, 50));
        component21.setPreferredSize(new Dimension(70, 150));
        component31.setPreferredSize(new Dimension(90, 120));
        component41.setPreferredSize(new Dimension(80, 90));
        component12.setPreferredSize(new Dimension(50, 50));
        component22.setPreferredSize(new Dimension(70, 150));
        component32.setPreferredSize(new Dimension(90, 120));
        component42.setPreferredSize(new Dimension(80, 90));
        layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        assertEquals("1 Sizes coinside:", new Dimension(120, 150), layout1
                .preferredLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(70, 200), layout2
                .preferredLayoutSize(container2));
        container1.add(component31);
        container2.add(component32);
        layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        assertEquals("1 Sizes coinside:", new Dimension(210, 150), layout1
                .preferredLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(90, 320), layout2
                .preferredLayoutSize(container2));
        container1.add(component41);
        container2.add(component42);
        layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        container1.setBorder(new EmptyBorder(20, 20, 20, 20));
        container2.setBorder(new EmptyBorder(20, 20, 20, 20));
        assertEquals("1 Sizes coinside:", new Dimension(330, 190), layout1
                .preferredLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(130, 450), layout2
                .preferredLayoutSize(container2));
    }

    public void testLayoutInvisibleChild() {
        JComponent container1 = new JPanel();
        JComponent component1 = new JPanel();
        component1.setPreferredSize(new Dimension(41, 26));
        component1.setVisible(false);
        container1.add(component1);
        BoxLayout layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        assertEquals(new Dimension(), layout1.preferredLayoutSize(container1));
    }

    public void testMinimumLayoutSize() {
        JComponent container1 = new JPanel();
        JComponent container2 = new JPanel();
        JComponent component11 = new JPanel();
        component11.setMinimumSize(new Dimension(41, 26));
        JComponent component21 = new JPanel();
        component21.setMinimumSize(new Dimension(48, 26));
        JComponent component31 = new JPanel();
        component31.setMinimumSize(new Dimension(55, 26));
        JComponent component41 = new JPanel();
        component41.setMinimumSize(new Dimension(62, 26));
        JComponent component12 = new JPanel();
        component12.setMinimumSize(new Dimension(62, 26));
        JComponent component22 = new JPanel();
        component22.setMinimumSize(new Dimension(55, 26));
        JComponent component32 = new JPanel();
        component32.setMinimumSize(new Dimension(48, 26));
        JComponent component42 = new JPanel();
        component42.setMinimumSize(new Dimension(41, 26));
        BoxLayout layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        BoxLayout layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        container1.add(component11);
        container1.add(component21);
        container2.add(component12);
        container2.add(component22);
        assertEquals("1 Sizes coinside:", new Dimension(89, 26), layout1
                .minimumLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(62, 52), layout2
                .minimumLayoutSize(container2));
        component11.setMinimumSize(new Dimension(50, 50));
        component21.setMinimumSize(new Dimension(70, 150));
        component31.setMinimumSize(new Dimension(90, 120));
        component41.setMinimumSize(new Dimension(80, 90));
        component12.setMinimumSize(new Dimension(50, 50));
        component22.setMinimumSize(new Dimension(70, 150));
        component32.setMinimumSize(new Dimension(90, 120));
        component42.setMinimumSize(new Dimension(80, 90));
        layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        assertEquals("1 Sizes coinside:", new Dimension(120, 150), layout1
                .minimumLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(70, 200), layout2
                .minimumLayoutSize(container2));
        container1.add(component31);
        container2.add(component32);
        layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        assertEquals("1 Sizes coinside:", new Dimension(210, 150), layout1
                .minimumLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(90, 320), layout2
                .minimumLayoutSize(container2));
        container1.add(component41);
        container2.add(component42);
        container1.setBorder(new EmptyBorder(20, 20, 20, 20));
        container2.setBorder(new EmptyBorder(20, 20, 20, 20));
        layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        assertEquals("1 Sizes coinside:", new Dimension(330, 190), layout1
                .minimumLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(130, 450), layout2
                .minimumLayoutSize(container2));
    }

    public void testMaximumLayoutSize1() {
        JComponent container1 = new JPanel();
        JComponent container2 = new JPanel();
        JComponent component11 = new JPanel();
        component11.setMaximumSize(new Dimension(41, 26));
        JComponent component21 = new JPanel();
        component21.setMaximumSize(new Dimension(48, 26));
        JComponent component31 = new JPanel();
        component31.setMaximumSize(new Dimension(55, 26));
        JComponent component41 = new JPanel();
        component41.setMaximumSize(new Dimension(62, 26));
        JComponent component12 = new JPanel();
        component12.setMaximumSize(new Dimension(62, 26));
        JComponent component22 = new JPanel();
        component22.setMaximumSize(new Dimension(55, 26));
        JComponent component32 = new JPanel();
        component32.setMaximumSize(new Dimension(48, 26));
        JComponent component42 = new JPanel();
        component42.setMaximumSize(new Dimension(41, 26));
        BoxLayout layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        BoxLayout layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        container1.add(component11);
        container1.add(component21);
        container2.add(component12);
        container2.add(component22);
        assertEquals("1 Sizes coinside:", new Dimension(89, 26), layout1
                .maximumLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(62, 52), layout2
                .maximumLayoutSize(container2));
        component11.setMaximumSize(new Dimension(50, 50));
        component21.setMaximumSize(new Dimension(70, 150));
        component31.setMaximumSize(new Dimension(90, 120));
        component41.setMaximumSize(new Dimension(80, 90));
        component12.setMaximumSize(new Dimension(50, 50));
        component22.setMaximumSize(new Dimension(70, 150));
        component32.setMaximumSize(new Dimension(90, 120));
        component42.setMaximumSize(new Dimension(80, 90));
        layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        assertEquals("1 Sizes coinside:", new Dimension(120, 150), layout1
                .maximumLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(70, 200), layout2
                .maximumLayoutSize(container2));
        container1.add(component31);
        container2.add(component32);
        layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        assertEquals("1 Sizes coinside:", new Dimension(210, 150), layout1
                .maximumLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(90, 320), layout2
                .maximumLayoutSize(container2));
        container1.add(component41);
        container2.add(component42);
        layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        container1.setBorder(new EmptyBorder(20, 20, 20, 20));
        container2.setBorder(new EmptyBorder(20, 20, 20, 20));
        assertEquals("1 Sizes coinside:", new Dimension(330, 190), layout1
                .maximumLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(130, 450), layout2
                .maximumLayoutSize(container2));
    }

    public void testMaximumLayoutSize2() {
        Container container1 = new JPanel();
        Container container2 = new JPanel();
        JComponent component11 = new JPanel();
        JComponent component21 = new JPanel();
        JComponent component12 = new JPanel();
        JComponent component22 = new JPanel();
        BoxLayout layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        BoxLayout layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        container1.add(component11);
        container1.add(component21);
        container2.add(component12);
        container2.add(component22);
        assertEquals("Maximum size 1: ", new Dimension(2 * Short.MAX_VALUE, Short.MAX_VALUE),
                layout1.maximumLayoutSize(container1));
        assertEquals("Maximum size 2: ", new Dimension(Short.MAX_VALUE, 2 * Short.MAX_VALUE),
                layout2.maximumLayoutSize(container2));
    }

    /*
     * this method is not used by this class so ther's no need to test it
     */
    public void testAddLayoutComponentStringComponent() {
    }

    public void testLayoutContainerVertical() {
        JComponent container = new JPanel();
        JComponent component1 = new JPanel();
        component1.setMinimumSize(new Dimension(62, 26));
        component1.setPreferredSize(new Dimension(62, 26));
        component1.setMaximumSize(new Dimension(62, 26));
        JComponent component2 = new JPanel();
        component2.setMinimumSize(new Dimension(55, 26));
        component2.setPreferredSize(new Dimension(55, 26));
        component2.setMaximumSize(new Dimension(55, 26));
        JComponent component3 = new JPanel();
        component3.setMinimumSize(new Dimension(48, 26));
        component3.setPreferredSize(new Dimension(48, 26));
        component3.setMaximumSize(new Dimension(48, 26));
        JComponent component4 = new JPanel();
        component4.setMinimumSize(new Dimension(41, 26));
        component4.setPreferredSize(new Dimension(41, 26));
        component4.setMaximumSize(new Dimension(41, 26));
        BoxLayout layout = new BoxLayout(container, BoxLayout.Y_AXIS);
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
        JFrame window2 = new JFrame();
        window2.getContentPane().add(container);
        window2.pack();
        assertEquals("Container's minimum requirements", new Dimension(143, 144), layout
                .minimumLayoutSize(container));
        assertEquals("Container's preferred requirements", new Dimension(143, 144), layout
                .preferredLayoutSize(container));
        assertEquals("Container's maximum requirements", new Dimension(143, 144), layout
                .maximumLayoutSize(container));
        assertEquals("Component1 location ", new Point(61, 20), component1.getLocation());
        assertEquals("Component2 location ", new Point(50, 46), component2.getLocation());
        assertEquals("Component3 location ", new Point(47, 72), component3.getLocation());
        assertEquals("Component4 location ", new Point(20, 98), component4.getLocation());
        window2.dispose();
    }

    public void testLayoutContainerHorizontal() {
        JComponent container = new JPanel();
        JComponent component1 = new JPanel();
        component1.setMinimumSize(new Dimension(41, 26));
        component1.setPreferredSize(new Dimension(41, 26));
        component1.setMaximumSize(new Dimension(41, 26));
        JComponent component2 = new JPanel();
        component2.setMinimumSize(new Dimension(48, 26));
        component2.setPreferredSize(new Dimension(48, 26));
        component2.setMaximumSize(new Dimension(48, 26));
        JComponent component3 = new JPanel();
        component3.setMinimumSize(new Dimension(55, 26));
        component3.setPreferredSize(new Dimension(55, 26));
        component3.setMaximumSize(new Dimension(55, 26));
        JComponent component4 = new JPanel();
        component4.setMinimumSize(new Dimension(62, 26));
        component4.setPreferredSize(new Dimension(62, 26));
        component4.setMaximumSize(new Dimension(62, 26));
        BoxLayout layout = new BoxLayout(container, BoxLayout.X_AXIS);
        container.setLayout(layout);
        container.setBorder(new EmptyBorder(20, 20, 20, 20));
        container.add(component1);
        container.add(component2);
        container.add(component3);
        container.add(component4);
        component1.setPreferredSize(new Dimension(70, 150));
        component2.setPreferredSize(new Dimension(70, 150));
        component3.setPreferredSize(new Dimension(90, 120));
        component4.setPreferredSize(new Dimension(80, 90));
        component1.setAlignmentY(0);
        component2.setAlignmentY(0.2f);
        component3.setAlignmentY(0.3f);
        component4.setAlignmentY(1);
        JFrame window = new JFrame();
        window.getContentPane().add(container);
        window.pack();
        assertEquals("Container's minimum requirements", new Dimension(246, 92), layout
                .minimumLayoutSize(container));
        assertEquals("Container's preferred requirements", new Dimension(350, 280), layout
                .preferredLayoutSize(container));
        assertEquals("Container's maximum requirements", new Dimension(246, 92), layout
                .maximumLayoutSize(container));
        assertEquals("Component1 - Locations coinside:", new Point(20, 140), component1
                .getLocation());
        assertEquals("Component2 - Locations coinside:", new Point(61, 135), component2
                .getLocation());
        assertEquals("Component3 - Locations coinside:", new Point(109, 133), component3
                .getLocation());
        assertEquals("Component4 - Locations coinside:", new Point(164, 114), component4
                .getLocation());
        window.dispose();
    }

    public void testInvalidateLayout() {
        Container container1 = new MyPanel();
        Container container2 = new MyPanel();
        JComponent component11 = new JPanel();
        JComponent component21 = new JPanel();
        JComponent component31 = new JPanel();
        JComponent component41 = new JPanel();
        JComponent component12 = new JPanel();
        JComponent component22 = new JPanel();
        JComponent component32 = new JPanel();
        JComponent component42 = new JPanel();
        BoxLayout layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        BoxLayout layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        container1.add(component11);
        container1.add(component21);
        container2.add(component21);
        container2.add(component22);
        component11.setMinimumSize(new Dimension(50, 50));
        component21.setMinimumSize(new Dimension(70, 150));
        component31.setMinimumSize(new Dimension(90, 120));
        component41.setMinimumSize(new Dimension(80, 90));
        component12.setMinimumSize(new Dimension(50, 50));
        component22.setMinimumSize(new Dimension(70, 150));
        component32.setMinimumSize(new Dimension(90, 120));
        component42.setMinimumSize(new Dimension(80, 90));
        assertEquals("1 Sizes coinside:", new Dimension(50, 50), layout1
                .minimumLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(70, 300), layout2
                .minimumLayoutSize(container2));
        container1.add(component31);
        container2.add(component32);
        layout1.invalidateLayout(container1);
        layout2.invalidateLayout(container2);
        assertEquals("1 Sizes coinside:", new Dimension(140, 120), layout1
                .minimumLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(90, 420), layout2
                .minimumLayoutSize(container2));
        container1.add(component41);
        container2.add(component42);
        layout1.invalidateLayout(container1);
        layout2.invalidateLayout(container2);
        assertEquals("1 Sizes coinside:", new Dimension(220, 120), layout1
                .minimumLayoutSize(container1));
        assertEquals("2 Sizes coinside:", new Dimension(90, 510), layout2
                .minimumLayoutSize(container2));
    }

    // Layout sharing testcases and so on
    public void testSharingLayout() {
        layout = new BoxLayout(new JPanel(), BoxLayout.LINE_AXIS);
        try {
            layout.getLayoutAlignmentY(new JPanel());
            fail("Exception must be thrown");
        } catch (AWTError e) {
        }
        try {
            layout.getLayoutAlignmentX(new JPanel());
            fail("Exception must be thrown");
        } catch (AWTError e) {
        }
        try {
            layout.invalidateLayout(new JPanel());
            fail("Exception must be thrown");
        } catch (AWTError e) {
        }
        try {
            layout.maximumLayoutSize(new JPanel());
            fail("Exception must be thrown");
        } catch (AWTError e) {
        }
        try {
            layout.layoutContainer(new JPanel());
            fail("Exception must be thrown");
        } catch (AWTError e) {
        }
        try {
            layout.preferredLayoutSize(new JPanel());
            fail("Exception must be thrown");
        } catch (AWTError e) {
        }
        try {
            layout.minimumLayoutSize(new JPanel());
            fail("Exception must be thrown");
        } catch (AWTError e) {
        }
    }

    class MyPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        MyPanel() {
            setPreferredSize(new Dimension(10, 10));
            setMinimumSize(new Dimension(10, 10));
            setMaximumSize(new Dimension(10, 10));
        }
    }

    public void testGetLayoutAlignmentY() {
        Container container1 = new MyPanel();
        Container container2 = new MyPanel();
        JComponent component11 = new MyPanel();
        JComponent component21 = new MyPanel();
        JComponent component31 = new MyPanel();
        JComponent component41 = new MyPanel();
        JComponent component12 = new MyPanel();
        JComponent component22 = new MyPanel();
        JComponent component32 = new MyPanel();
        JComponent component42 = new MyPanel();
        BoxLayout layout1 = new BoxLayout(container1, BoxLayout.Y_AXIS);
        BoxLayout layout2 = new BoxLayout(container2, BoxLayout.X_AXIS);
        container1.add(component11);
        container1.add(component21);
        container2.add(component12);
        container2.add(component22);
        float value1 = 0.02f;
        float value2 = 0.47f;
        float value3 = 0.51f;
        float value4 = 0.94f;
        float defaultValue = 0.5f;
        component11.setAlignmentY(value1);
        component21.setAlignmentY(value2);
        component31.setAlignmentY(value3);
        component41.setAlignmentY(value4);
        component12.setAlignmentY(value1);
        component22.setAlignmentY(value2);
        component32.setAlignmentY(value3);
        component42.setAlignmentY(value4);
        assertEquals("Alignments coinside:", 0.2857143,
                layout2.getLayoutAlignmentY(container2), 1e-5f);
        assertEquals("Alignments coinside:", defaultValue, layout1
                .getLayoutAlignmentY(container1), 1e-5f);
        container1.add(component31);
        container2.add(component32);
        layout1 = new BoxLayout(container1, BoxLayout.Y_AXIS);
        layout2 = new BoxLayout(container2, BoxLayout.X_AXIS);
        assertEquals("Alignments coinside:", 0.33333334, layout2
                .getLayoutAlignmentY(container2), 1e-5f);
        assertEquals("Alignments coinside:", defaultValue, layout1
                .getLayoutAlignmentY(container1), 1e-5f);
        container1.add(component41);
        container2.add(component42);
        layout1 = new BoxLayout(container1, BoxLayout.Y_AXIS);
        layout2 = new BoxLayout(container2, BoxLayout.X_AXIS);
        assertEquals("Alignments coinside:", 0.47368422, layout2
                .getLayoutAlignmentY(container2), 1e-5f);
        assertEquals("Alignments coinside:", defaultValue, layout1
                .getLayoutAlignmentY(container1), 1e-5f);
    }

    public void testGetLayoutAlignmentX() {
        Container container1 = new JPanel();
        Container container2 = new JPanel();
        JComponent component11 = new JPanel();
        JComponent component21 = new JPanel();
        JComponent component31 = new JPanel();
        JComponent component41 = new JPanel();
        JComponent component12 = new JPanel();
        JComponent component22 = new JPanel();
        JComponent component32 = new JPanel();
        JComponent component42 = new JPanel();
        component11.setMinimumSize(new Dimension(34, 10));
        component11.setPreferredSize(new Dimension(34, 10));
        component11.setMaximumSize(new Dimension(34, 10));
        component21.setMinimumSize(new Dimension(34, 10));
        component21.setPreferredSize(new Dimension(34, 10));
        component21.setMaximumSize(new Dimension(34, 10));
        component31.setMinimumSize(new Dimension(34, 10));
        component31.setPreferredSize(new Dimension(34, 10));
        component31.setMaximumSize(new Dimension(34, 10));
        component41.setMinimumSize(new Dimension(34, 10));
        component41.setPreferredSize(new Dimension(34, 10));
        component41.setMaximumSize(new Dimension(34, 10));
        component12.setMinimumSize(new Dimension(34, 10));
        component12.setPreferredSize(new Dimension(34, 10));
        component12.setMaximumSize(new Dimension(34, 10));
        component22.setMinimumSize(new Dimension(34, 10));
        component22.setPreferredSize(new Dimension(34, 10));
        component22.setMaximumSize(new Dimension(34, 10));
        component32.setMinimumSize(new Dimension(34, 10));
        component32.setPreferredSize(new Dimension(34, 10));
        component32.setMaximumSize(new Dimension(34, 10));
        component42.setMinimumSize(new Dimension(34, 10));
        component42.setPreferredSize(new Dimension(34, 10));
        component42.setMaximumSize(new Dimension(34, 10));
        BoxLayout layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        BoxLayout layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        container1.add(component11);
        container1.add(component21);
        container2.add(component12);
        container2.add(component22);
        float value1 = 0.02f;
        float value2 = 0.47f;
        float value3 = 0.51f;
        float value4 = 0.94f;
        float defaultValue = 0.5f;
        component11.setAlignmentX(value1);
        component21.setAlignmentX(value2);
        component31.setAlignmentX(value3);
        component41.setAlignmentX(value4);
        component12.setAlignmentX(value1);
        component22.setAlignmentX(value2);
        component32.setAlignmentX(value3);
        component42.setAlignmentX(value4);
        assertEquals("Alignments coinside:", defaultValue, layout1
                .getLayoutAlignmentX(container1), 1e-5f);
        assertEquals("Alignments coinside:", 0.30612245, layout2
                .getLayoutAlignmentX(container2), 1e-5f);
        container1.add(component31);
        container2.add(component32);
        layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        assertEquals("Alignments coinside:", defaultValue, layout1
                .getLayoutAlignmentX(container1), 1e-5f);
        assertEquals("Alignments coinside:", 0.33333334, layout2
                .getLayoutAlignmentX(container2), 1e-5f);
        container1.add(component41);
        container2.add(component42);
        layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        assertEquals("Alignments coinside:", defaultValue, layout1
                .getLayoutAlignmentX(container1), 1e-5f);
        assertEquals("Alignments coinside:", 0.47692308, layout2
                .getLayoutAlignmentX(container2), 1e-5f);
    }

    /*
     * this method is not used by this class so ther's no need to test it
     */
    public void testRemoveLayoutComponent() {
    }

    public void testWriteObject() throws IOException {
        Container container1 = new Panel();
        Container container2 = new Panel();
        BoxLayout layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        BoxLayout layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(fo);
        so.writeObject(layout1);
        so.flush();
        fo = new ByteArrayOutputStream();
        so = new ObjectOutputStream(fo);
        so.writeObject(layout2);
        so.flush();
    }

    public void testReadObject() throws IOException, ClassNotFoundException {
        Container container1 = new Panel();
        Container container2 = new Panel();
        BoxLayout layout1 = new BoxLayout(container1, BoxLayout.X_AXIS);
        BoxLayout layout2 = new BoxLayout(container2, BoxLayout.Y_AXIS);
        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(fo);
        so.writeObject(layout1);
        so.flush();
        InputStream fi = new ByteArrayInputStream(fo.toByteArray());
        ObjectInputStream si = new ObjectInputStream(fi);
        BoxLayout resurrectedLayout = (BoxLayout) si.readObject();
        assertNotNull(resurrectedLayout);
        fo = new ByteArrayOutputStream();
        so = new ObjectOutputStream(fo);
        so.writeObject(layout2);
        so.flush();
        fi = new ByteArrayInputStream(fo.toByteArray());
        si = new ObjectInputStream(fi);
        resurrectedLayout = (BoxLayout) si.readObject();
        assertNotNull(resurrectedLayout);
    }
}
