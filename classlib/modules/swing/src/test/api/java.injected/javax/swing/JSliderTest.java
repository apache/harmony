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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

public class JSliderTest extends SwingTestCase {
    private JSlider slider;

    @Override
    protected void setUp() throws Exception {
        slider = new JSlider();
    }

    @Override
    protected void tearDown() throws Exception {
        slider = null;
    }

    public void testNewJSlider() throws Exception {
        BoundedRangeModel m = new DefaultBoundedRangeModel(2, 10, 0, 50);
        m.setValueIsAdjusting(true);
        slider = new JSlider(m);
        assertTrue(m == slider.getModel());

        try { // Regression test for HARMONY-2535
            new JSlider(2);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testCreateChangeListener() throws Exception {
        assertNotNull(slider.createChangeListener());
        assertFalse(slider.createChangeListener() == slider.createChangeListener());
    }

    public void testSetGetLabelTable() throws Exception {
        assertNull(slider.getLabelTable());
        Dictionary<String, String> labelTable = new Hashtable<String, String>();
        labelTable.put("a", "b");
        propertyChangeController = new PropertyChangeController();
        slider.addPropertyChangeListener(propertyChangeController);
        slider.setLabelTable(labelTable);
        assertTrue(propertyChangeController.isChanged("labelTable"));
        assertEquals(labelTable, slider.getLabelTable());
        labelTable.put("c", "d");
        assertEquals(labelTable, slider.getLabelTable());
        propertyChangeController.reset();
        slider.setLabelTable(labelTable);
        assertFalse(propertyChangeController.isChanged("labelTable"));
    }

    public void testSetGetInverted() throws Exception {
        assertFalse(slider.getInverted());
        propertyChangeController = new PropertyChangeController();
        slider.addPropertyChangeListener(propertyChangeController);
        slider.setInverted(true);
        assertTrue(propertyChangeController.isChanged("inverted"));
        assertTrue(slider.getInverted());
        propertyChangeController.reset();
        slider.setInverted(true);
        assertFalse(propertyChangeController.isChanged("inverted"));
    }

    public void testSetGetMajorTickSpacing() throws Exception {
        assertEquals(0, slider.getMajorTickSpacing());
        propertyChangeController = new PropertyChangeController();
        slider.addPropertyChangeListener(propertyChangeController);
        slider.setMajorTickSpacing(22);
        assertTrue(propertyChangeController.isChanged("majorTickSpacing"));
        assertEquals(22, slider.getMajorTickSpacing());
        propertyChangeController.reset();
        slider.setMajorTickSpacing(22);
        assertFalse(propertyChangeController.isChanged("majorTickSpacing"));
    }

    public void testSetGetMinorTickSpacing() throws Exception {
        assertEquals(0, slider.getMinorTickSpacing());
        propertyChangeController = new PropertyChangeController();
        slider.addPropertyChangeListener(propertyChangeController);
        slider.setMinorTickSpacing(22);
        assertTrue(propertyChangeController.isChanged("minorTickSpacing"));
        assertEquals(22, slider.getMinorTickSpacing());
        propertyChangeController.reset();
        slider.setMinorTickSpacing(22);
        assertFalse(propertyChangeController.isChanged("minorTickSpacing"));
    }

    public void testSetGetSnapToTicks() throws Exception {
        assertFalse(slider.getSnapToTicks());
        propertyChangeController = new PropertyChangeController();
        slider.addPropertyChangeListener(propertyChangeController);
        slider.setSnapToTicks(true);
        assertTrue(propertyChangeController.isChanged("snapToTicks"));
        assertTrue(slider.getSnapToTicks());
        propertyChangeController.reset();
        slider.setSnapToTicks(true);
        assertFalse(propertyChangeController.isChanged("snapToTicks"));
    }

    public void testSetGetPaintTicks() throws Exception {
        assertFalse(slider.getPaintTicks());
        propertyChangeController = new PropertyChangeController();
        slider.addPropertyChangeListener(propertyChangeController);
        slider.setPaintTicks(true);
        assertTrue(propertyChangeController.isChanged("paintTicks"));
        assertTrue(slider.getPaintTicks());
        propertyChangeController.reset();
        slider.setPaintTicks(true);
        assertFalse(propertyChangeController.isChanged("paintTicks"));
    }

    public void testSetGetPaintTrack() throws Exception {
        assertTrue(slider.getPaintTrack());
        propertyChangeController = new PropertyChangeController();
        slider.addPropertyChangeListener(propertyChangeController);
        slider.setPaintTrack(false);
        assertTrue(propertyChangeController.isChanged("paintTrack"));
        assertFalse(slider.getPaintTrack());
        propertyChangeController.reset();
        slider.setPaintTrack(false);
        assertFalse(propertyChangeController.isChanged("paintTrack"));
    }

    public void testSetGetOrientation() throws Exception {
        try {
            slider.setOrientation(20);
            fail("IllegalArgumentException shall be thrown");
        } catch (IllegalArgumentException e) {
        }
        assertEquals(SwingConstants.HORIZONTAL, slider.getOrientation());
        propertyChangeController = new PropertyChangeController();
        slider.addPropertyChangeListener(propertyChangeController);
        slider.setOrientation(SwingConstants.VERTICAL);
        assertTrue(propertyChangeController.isChanged("orientation"));
        assertEquals(SwingConstants.VERTICAL, slider.getOrientation());
    }

    public void testSetGetModel() throws Exception {
        assertNotNull(slider.getModel());
        propertyChangeController = new PropertyChangeController();
        slider.addPropertyChangeListener(propertyChangeController);
        slider.setModel(new DefaultBoundedRangeModel());
        assertTrue(propertyChangeController.isChanged("model"));
    }

    public void testSetPaintLabels() throws Exception {
        assertFalse(slider.getPaintLabels());
        propertyChangeController = new PropertyChangeController();
        slider.addPropertyChangeListener(propertyChangeController);
        slider.setPaintLabels(true);
        assertEquals(0, slider.getMajorTickSpacing());
        assertTrue(propertyChangeController.isChanged("paintLabels"));
        assertTrue(slider.getPaintLabels());
        propertyChangeController.reset();
        slider.setPaintLabels(true);
        assertFalse(propertyChangeController.isChanged("paintLabels"));
    }

    public void testCreateStandardLabels() throws Exception {
        try {
            slider.createStandardLabels(-1, 0);
            fail("IllegalArgumentException shall be thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            slider.createStandardLabels(1, -1);
            fail("IllegalArgumentException shall be thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            slider.createStandardLabels(1, 101);
            fail("IllegalArgumentException shall be thrown");
        } catch (IllegalArgumentException e) {
        }
        Hashtable<?, ?> t = slider.createStandardLabels(2, 0);
        assertNotNull(t);
        Enumeration<?> enumeration = t.keys();
        while (enumeration.hasMoreElements()) {
            Integer key = (Integer) enumeration.nextElement();
            String text = ((JLabel) t.get(key)).getText();
            assertEquals(key.toString(), text);
        }
    }

    public void testGetUI() throws Exception {
        assertNotNull(slider.getUI());
        assertTrue(slider.getUI() == slider.getUI());
    }
}
