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
 * @author Dennis Ushakov
 */
package javax.swing;

import java.util.Arrays;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleValue;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ProgressBarUI;
import org.apache.harmony.x.swing.StringConstants;

public class JProgressBarTest extends BasicSwingTestCase {
    private JProgressBar progressBar;

    private DefaultBoundedRangeModel model;

    private ChangeController controller;

    @Override
    public void setUp() {
        propertyChangeController = new PropertyChangeController();
        model = new DefaultBoundedRangeModel(0, 0, 0, 256);
        controller = new ChangeController();
        progressBar = new JProgressBar(model);
        progressBar.addChangeListener(controller);
        progressBar.addPropertyChangeListener(propertyChangeController);
    }

    @Override
    public void tearDown() {
        propertyChangeController = null;
        progressBar = null;
        model = null;
        controller = null;
    }

    public void testJProgressBar() {
        progressBar = new JProgressBar();
        assertEquals(SwingConstants.HORIZONTAL, progressBar.orientation);
        assertNull(progressBar.progressString);
        assertFalse(progressBar.paintString);
        assertTrue(progressBar.paintBorder);
        assertFalse(progressBar.isIndeterminate());
        assertNotNull(progressBar.getUI());
        assertNotNull(progressBar.changeListener);
        progressBar = new JProgressBar(model);
        assertSame(model, progressBar.model);
        assertNotNull(progressBar.getUI());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() {
                progressBar = new JProgressBar(SwingConstants.HORIZONTAL
                        + SwingConstants.VERTICAL + 1);
            }
        });
        progressBar = new JProgressBar(1, 23);
        assertNotNull(progressBar.getUI());
        assertEquals(1, progressBar.getMinimum());
        assertEquals(23, progressBar.getMaximum());
        assertEquals(1, progressBar.getValue());
    }

    public void testSetGetOrientation() {
        progressBar.setOrientation(SwingConstants.VERTICAL);
        assertEquals(SwingConstants.VERTICAL, progressBar.getOrientation());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() {
                progressBar.setOrientation(SwingConstants.HORIZONTAL + SwingConstants.VERTICAL
                        + 1);
            }
        });
        assertTrue(propertyChangeController.isChanged(StringConstants.ORIENTATION));
    }

    public void testSetGetModel() {
        DefaultBoundedRangeModel newModel = new DefaultBoundedRangeModel(1, 0, 1, 12);
        progressBar.setModel(newModel);
        assertSame(newModel, progressBar.getModel());
        assertTrue(Arrays.asList(newModel.getChangeListeners()).contains(
                progressBar.changeListener));
        progressBar.setModel(null);
        assertNull(progressBar.getModel());
    }

    public void testProgressString() {
        assertFalse(progressBar.isStringPainted());
        String string = "someDisplayString";
        progressBar.setString(string);
        assertSame(string, progressBar.getString());
        assertTrue(propertyChangeController.isChanged("string"));
        progressBar.setStringPainted(true);
        assertTrue(progressBar.isStringPainted());
        assertTrue(propertyChangeController.isChanged("stringPainted"));
    }

    public void testSetIsIndeterminate() {
        assertFalse(progressBar.isIndeterminate());
        progressBar.setIndeterminate(true);
        assertTrue(progressBar.isIndeterminate());
        assertTrue(propertyChangeController.isChanged("indeterminate"));
    }

    public void testAccessibleContext() {
        progressBar.add(new JProgressBar());
        AccessibleContext accessibleContext = progressBar.getAccessibleContext();
        assertNull(accessibleContext.getAccessibleName());
        assertNull(accessibleContext.getAccessibleDescription());
        assertNull(accessibleContext.getAccessibleAction());
        assertSame(accessibleContext.getAccessibleValue(), accessibleContext);
        assertTrue(accessibleContext.getAccessibleStateSet().contains(
                AccessibleState.HORIZONTAL));
        AccessibleValue value = (AccessibleValue) accessibleContext;
        int currentValue = (progressBar.getMinimum() + progressBar.getMaximum()) / 2;
        progressBar.setValue(currentValue);
        assertTrue(controller.isChanged());
        assertEquals(new Integer(currentValue), value.getCurrentAccessibleValue());
        assertEquals(new Integer(progressBar.getMaximum()), value.getMaximumAccessibleValue());
        assertEquals(new Integer(progressBar.getMinimum()), value.getMinimumAccessibleValue());
        value.setCurrentAccessibleValue(new Integer(currentValue + 1));
        assertTrue(controller.isChanged());
        assertEquals(currentValue + 1, progressBar.getValue());
        assertEquals(new Integer(currentValue + 1), value.getCurrentAccessibleValue());
        assertTrue(value.setCurrentAccessibleValue(new Integer(currentValue + 1)));
    }

    public void testSetGetUpdateUI() {
        assertEquals("ProgressBarUI", progressBar.getUIClassID());
        ProgressBarUI defaultUI = progressBar.getUI();
        assertNotNull(defaultUI);
        ProgressBarUI ui = new ProgressBarUI() {
        };
        progressBar.setUI(ui);
        assertEquals(ui, progressBar.getUI());
        progressBar.updateUI();
        assertNotSame(ui, progressBar.getUI());
    }

    public void testSetGetValue() {
        int currentValue = (progressBar.getMinimum() + progressBar.getMaximum()) / 2;
        progressBar.setValue(currentValue);
        assertEquals(currentValue, progressBar.getValue());
        assertTrue(controller.isChanged());
        progressBar.setValue(progressBar.getMaximum() + 10);
        assertEquals(progressBar.getMaximum(), progressBar.getValue());
    }

    public void testSetGetMinimum() {
        progressBar.setMinimum(1);
        assertEquals(1, progressBar.getMinimum());
        assertTrue(controller.isChanged());
    }

    public void testSetGetMaximum() {
        progressBar.setMaximum(257);
        assertEquals(257, progressBar.getMaximum());
        assertTrue(controller.isChanged());
    }

    public void testSetIsBorderPainted() {
        assertTrue(progressBar.isBorderPainted());
        progressBar.setBorderPainted(false);
        assertFalse(progressBar.isBorderPainted());
        assertTrue(propertyChangeController.isChanged("borderPainted"));
    }

    public void testGetPercentComplete() {
        controller.setVerbose(false);
        progressBar.setMinimum(25);
        int range = progressBar.getMaximum() - progressBar.getMinimum();
        int min = progressBar.getMinimum();
        for (int i = progressBar.getMinimum(); i < progressBar.getMaximum() + 1; i++) {
            progressBar.setValue(i);
            assertEquals(1. * (i - min) / (range), progressBar.getPercentComplete(),
                    0.0000000001);
        }
        progressBar.setMinimum(progressBar.getMaximum());
        assertTrue(Double.isNaN(progressBar.getPercentComplete()));
    }

    public void testCreateChangeListener() {
        ChangeListener listener = progressBar.createChangeListener();
        assertNotNull(listener);
    }

    public void testAddRemoveGetChangeListener() {
        assertEquals(2, progressBar.getChangeListeners().length);
        assertTrue(Arrays.asList(progressBar.getChangeListeners()).contains(controller));
        ChangeListener listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
            }
        };
        progressBar.addChangeListener(listener);
        assertTrue(Arrays.asList(progressBar.getChangeListeners()).contains(listener));
        progressBar.removeChangeListener(controller);
        progressBar.removeChangeListener(listener);
        assertEquals(1, progressBar.getChangeListeners().length);
    }

    public void testChangeEvent() {
        progressBar.setValue(95);
        assertSame(progressBar, progressBar.changeEvent.getSource());
        assertTrue(controller.isChanged());
    }
}
