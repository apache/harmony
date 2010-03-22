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
 * Created on 21.12.2004

 */
package javax.swing;

import java.awt.AWTError;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.accessibility.AccessibleContext;

public class BoxTest extends SwingTestCase {
    /*
     * Class under test for AccessibleContext getAccessibleContext()
     */
    public void testGetAccessibleContext() {
        Component box = new Box(BoxLayout.LINE_AXIS);
        AccessibleContext accessible = box.getAccessibleContext();
        assertEquals("Accessible context is correct ", Box.AccessibleBox.class, accessible
                .getClass());
    }

    /*
     * Class under test for void setLayout(LayoutManager)
     */
    public void testSetLayoutLayoutManager() {
        Box box = new Box(BoxLayout.X_AXIS);
        boolean thrown = false;
        try {
            box.setLayout(new GridLayout(3, 3));
        } catch (AWTError err) {
            thrown = true;
        }
        assertTrue("Exception is thrown ", thrown);
    }

    public void testCreateRigidArea() throws NullPointerException {
        Dimension size = new Dimension(100, 100);
        Component box = Box.createRigidArea(size);
        assertEquals("Minimum size initialized ", size, box.getMinimumSize());
        assertEquals("Preferred size initialized ", size, box.getPreferredSize());
        assertEquals("Maximum size initialized ", size, box.getMaximumSize());
        assertFalse("Opaqueness initialized ", box.isOpaque());

        Box.createRigidArea(null);             
    }

    public void testCreateVerticalBox() {
        Box box = Box.createVerticalBox();
        assertFalse("Opaqueness initialized ", box.isOpaque());
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        panel1.setMinimumSize(new Dimension(10, 10));
        panel2.setMinimumSize(new Dimension(10, 10));
        panel1.setPreferredSize(new Dimension(100, 200));
        panel2.setPreferredSize(new Dimension(1000, 2000));
        box.add(panel1);
        box.add(panel2);
        assertEquals("Minimum size ", new Dimension(10, 20), box.getMinimumSize());
        assertEquals("Preferred size ", new Dimension(1000, 2200), box.getPreferredSize());
        assertEquals("Maximum size ", new Dimension(Short.MAX_VALUE, 2 * Short.MAX_VALUE), box
                .getMaximumSize());
    }

    public void testCreateHorizontalBox() {
        Box box = Box.createHorizontalBox();
        assertFalse("Opaqueness initialized ", box.isOpaque());
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        panel1.setMinimumSize(new Dimension(10, 10));
        panel2.setMinimumSize(new Dimension(10, 10));
        panel1.setPreferredSize(new Dimension(100, 200));
        panel2.setPreferredSize(new Dimension(1000, 2000));
        box.add(panel1);
        box.add(panel2);
        assertEquals("Minimum size ", new Dimension(20, 10), box.getMinimumSize());
        assertEquals("Preferred size ", new Dimension(1100, 2000), box.getPreferredSize());
        assertEquals("Maximum size ", new Dimension(2 * Short.MAX_VALUE, Short.MAX_VALUE), box
                .getMaximumSize());
    }

    public void testCreateVerticalStrut() {
        int height = 100;
        Dimension size = new Dimension(0, height);
        Dimension maxSize = new Dimension(Short.MAX_VALUE, height);
        Component box = Box.createVerticalStrut(height);
        assertEquals("Minimum size initialized correctly ", size, box.getMinimumSize());
        assertEquals("Preferred size initialized correctly ", size, box.getPreferredSize());
        assertEquals("Maximum size initialized correctly ", maxSize, box.getMaximumSize());
        assertFalse("Opaqueness initialized correctly", box.isOpaque());
    }

    public void testCreateHorizontalStrut() {
        int width = 100;
        Dimension size = new Dimension(width, 0);
        Dimension maxSize = new Dimension(width, Short.MAX_VALUE);
        Component box = Box.createHorizontalStrut(width);
        assertEquals("Minimum size initialized correctly ", size, box.getMinimumSize());
        assertEquals("Preferred size initialized correctly ", size, box.getPreferredSize());
        assertEquals("Maximum size initialized correctly ", maxSize, box.getMaximumSize());
        assertFalse("Opaqueness initialized correctly", box.isOpaque());
    }

    public void testCreateVerticalGlue() {
        Dimension nullSize = new Dimension(0, 0);
        Dimension maximumSize = new Dimension(0, Short.MAX_VALUE);
        Component box = Box.createVerticalGlue();
        assertEquals("Minimum size initialized correctly ", nullSize, box.getMinimumSize());
        assertEquals("Preferred size initialized correctly ", nullSize, box.getPreferredSize());
        assertEquals("Maximum size initialized correctly ", maximumSize, box.getMaximumSize());
        assertFalse("Opaqueness initialized correctly", box.isOpaque());
    }

    public void testCreateHorizontalGlue() {
        Dimension nullSize = new Dimension(0, 0);
        Dimension maximumSize = new Dimension(Short.MAX_VALUE, 0);
        Component box = Box.createHorizontalGlue();
        assertEquals("Minimum size initialized correctly ", nullSize, box.getMinimumSize());
        assertEquals("Preferred size initialized correctly ", nullSize, box.getPreferredSize());
        assertEquals("Maximum size initialized correctly ", maximumSize, box.getMaximumSize());
        assertFalse("Opaqueness initialized correctly", box.isOpaque());
    }

    public void testCreateGlue() {
        Dimension nullSize = new Dimension(0, 0);
        Dimension maximumSize = new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
        Component box = Box.createGlue();
        assertEquals("Minimum size initialized correctly ", nullSize, box.getMinimumSize());
        assertEquals("Preferred size initialized correctly ", nullSize, box.getPreferredSize());
        assertEquals("Maximum size initialized correctly ", maximumSize, box.getMaximumSize());
        assertFalse("Opaqueness initialized correctly", box.isOpaque());
    }
}
