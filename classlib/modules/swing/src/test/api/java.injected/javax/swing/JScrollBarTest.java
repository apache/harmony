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

import java.awt.Adjustable;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Vector;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;

public class JScrollBarTest extends SwingTestCase {
    private JScrollBar bar;

    private List<String> testList;

    @Override
    public void setUp() {
        bar = new JScrollBar();
        testList = new Vector<String>();
        bar.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent e) {
                testList.add("1");
            }
        });
    }

    @Override
    protected void tearDown() throws Exception {
        bar = null;
        testList = null;
    }

    public void testConstructor() throws Exception {
        try {         
            new JScrollBar(2000, 512, 128, 16, 10001); 
            fail("IllegalArgumentException should be thrown");
        } catch (NullPointerException npe) {    
            fail("NPE should not be thrown");            
        } catch (IllegalArgumentException iae) {
            // PASSED
        }
    }

    public void testGetAccessibleContext() throws Exception {
        JScrollBar.AccessibleJScrollBar accJScrollBar = (JScrollBar.AccessibleJScrollBar) bar
                .getAccessibleContext();
        assertNotNull(accJScrollBar);
        assertTrue(bar.getAccessibleContext() == bar.getAccessibleContext());
        assertTrue(accJScrollBar.getAccessibleValue() == accJScrollBar.getAccessibleValue());
        assertTrue(bar.getAccessibleContext() == accJScrollBar.getAccessibleValue());
        assertEquals(new Integer(bar.getValue()), accJScrollBar.getCurrentAccessibleValue());
        accJScrollBar.setCurrentAccessibleValue(new Float(1));
        assertEquals(new Integer(1), accJScrollBar.getCurrentAccessibleValue());
        assertEquals(new Integer(bar.getValue()), accJScrollBar.getCurrentAccessibleValue());
        bar.setMinimum(20);
        assertEquals(new Integer(20), accJScrollBar.getMinimumAccessibleValue());
        assertEquals(new Integer(bar.getMinimum()), accJScrollBar.getMinimumAccessibleValue());
        bar.setMaximum(345);
        assertEquals(new Integer(335), accJScrollBar.getMaximumAccessibleValue());
        bar.setVisibleAmount(20);
        assertEquals(new Integer(325), accJScrollBar.getMaximumAccessibleValue());
        assertTrue(accJScrollBar.getAccessibleRole() == AccessibleRole.SCROLL_BAR);
        assertEquals(5, accJScrollBar.getAccessibleStateSet().toArray().length);
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.ENABLED));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.FOCUSABLE));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.VISIBLE));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.OPAQUE));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.VERTICAL));
        bar.setVisible(false);
        assertEquals(4, accJScrollBar.getAccessibleStateSet().toArray().length);
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.ENABLED));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.FOCUSABLE));
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.VISIBLE));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.OPAQUE));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.VERTICAL));
        bar.setOrientation(Adjustable.HORIZONTAL);
        assertEquals(4, accJScrollBar.getAccessibleStateSet().toArray().length);
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.ENABLED));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.FOCUSABLE));
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.VISIBLE));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.OPAQUE));
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.VERTICAL));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.HORIZONTAL));
        bar.setEnabled(false);
        assertEquals(3, accJScrollBar.getAccessibleStateSet().toArray().length);
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.ENABLED));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.FOCUSABLE));
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.VISIBLE));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.OPAQUE));
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.VERTICAL));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.HORIZONTAL));
        bar.setOpaque(false);
        assertEquals(2, accJScrollBar.getAccessibleStateSet().toArray().length);
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.ENABLED));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.FOCUSABLE));
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.VISIBLE));
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.OPAQUE));
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.VERTICAL));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.HORIZONTAL));
        bar.setFocusable(false);
        assertEquals(1, accJScrollBar.getAccessibleStateSet().toArray().length);
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.ENABLED));
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.FOCUSABLE));
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.VISIBLE));
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.OPAQUE));
        assertFalse(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.VERTICAL));
        assertTrue(accJScrollBar.getAccessibleStateSet().contains(AccessibleState.HORIZONTAL));
    }

    public void testGetOrientation() throws Exception {
        assertEquals(SwingConstants.VERTICAL, bar.getOrientation());
        bar.setOrientation(SwingConstants.HORIZONTAL);
        assertEquals(SwingConstants.HORIZONTAL, bar.getOrientation());
        try {
            bar.setOrientation(200);
            fail("illegal argument exception shal be thrown");
        } catch (IllegalArgumentException e) {
        }
        assertEquals(1, testList.size());
    }

    public void testGetValue() throws Exception {
        assertEquals(0, bar.getValue());
        assertEquals(bar.getModel().getValue(), bar.getValue());
        bar.getModel().setValue(50);
        assertEquals(50, bar.getValue());
        assertEquals(bar.getModel().getValue(), bar.getValue());
        bar.setValue(180);
        assertEquals(90, bar.getValue());
        assertEquals(bar.getModel().getValue(), bar.getValue());
        assertEquals(0, testList.size());
    }

    public void testGetModel() throws Exception {
        assertEquals(0, testList.size());
        bar.setModel(new DefaultBoundedRangeModel());
        assertEquals(1, testList.size());
    }

    public void testGetMinimum() throws Exception {
        bar.setMinimum(-4);
        assertEquals(-4, bar.getMinimum());
        bar.setMinimum(200);
        assertEquals(200, bar.getMinimum());
        assertEquals(200, bar.getModel().getMinimum());
        assertEquals(0, testList.size());
    }

    public void testGetMaximum() throws Exception {
        bar.setMaximum(-4);
        assertEquals(-4, bar.getMaximum());
        bar.setMaximum(200);
        assertEquals(200, bar.getMaximum());
        assertEquals(200, bar.getModel().getMaximum());
        assertEquals(0, testList.size());
    }

    public void testGetValueIsAdjusting() throws Exception {
        assertFalse(bar.getValueIsAdjusting());
        bar.setValueIsAdjusting(true);
        assertTrue(bar.getValueIsAdjusting());
        assertEquals(0, testList.size());
    }

    public void testGetUnitIncrement() throws Exception {
        assertEquals(1, bar.getUnitIncrement());
        propertyChangeController = new PropertyChangeController();
        bar.addPropertyChangeListener(propertyChangeController);
        bar.setUnitIncrement(23);
        assertEquals(23, bar.getUnitIncrement());
        assertTrue(propertyChangeController.isChanged("unitIncrement"));
        propertyChangeController.reset();
        bar.setUnitIncrement(23);
        assertFalse(propertyChangeController.isChanged("unitIncrement"));
        assertEquals(23, bar.getUnitIncrement(-1));
        assertEquals(23, bar.getUnitIncrement(1));
        assertEquals(23, bar.getUnitIncrement(-134));
    }

    public void testGetBlockIncrement() throws Exception {
        assertEquals(10, bar.getBlockIncrement());
        bar = new JScrollBar(Adjustable.VERTICAL, 0, 32, 0, 150);
        assertEquals(32, bar.getBlockIncrement());
        assertEquals(bar.getModel().getExtent(), bar.getBlockIncrement());
        propertyChangeController = new PropertyChangeController();
        bar.addPropertyChangeListener(propertyChangeController);
        bar.setBlockIncrement(40);
        assertTrue(propertyChangeController.isChanged("blockIncrement"));
        propertyChangeController.reset();
        bar.setBlockIncrement(40);
        assertFalse(propertyChangeController.isChanged("blockIncrement"));
        assertEquals(40, bar.getBlockIncrement());
        assertEquals(32, bar.getModel().getExtent());
        bar.setValues(0, 50, 0, 200);
        assertEquals(40, bar.getBlockIncrement());
        assertEquals(50, bar.getModel().getExtent());
        assertEquals(0, testList.size());
        assertEquals(40, bar.getBlockIncrement(1));
        assertEquals(40, bar.getBlockIncrement(-1));
        assertEquals(40, bar.getBlockIncrement(241));
    }

    public void testGetVisibleAmount() throws Exception {
        assertEquals(10, bar.getVisibleAmount());
        bar.setVisibleAmount(20);
        assertEquals(20, bar.getVisibleAmount());
        assertEquals(20, bar.getModel().getExtent());
        assertEquals(0, testList.size());
    }

    public void testAdjustmentListener() throws Exception {
        final List<String> test = new Vector<String>();
        bar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(final AdjustmentEvent e) {
                test.add("1");
            }
        });
        assertEquals(0, test.size());
        bar.setValue(300);
        assertEquals(1, test.size());
    }

    public void testGetAdjustmentListeners() throws Exception {
        assertEquals(0, bar.getAdjustmentListeners().length);
        AdjustmentListener l = new AdjustmentListener() {
            public void adjustmentValueChanged(final AdjustmentEvent e) {
            }
        };
        bar.addAdjustmentListener(l);
        assertEquals(1, bar.getAdjustmentListeners().length);
        bar.removeAdjustmentListener(l);
        assertEquals(0, bar.getAdjustmentListeners().length);
    }
}
