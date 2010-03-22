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

import java.beans.PropertyChangeEvent;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JSpinner.DateEditor;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JSpinner.ListEditor;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.SpinnerUI;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

public class JSpinnerTest extends BasicSwingTestCase {
    private JSpinner spinner;

    private SpinnerModel abstractModel;

    private ChangeController chl;

    @Override
    public void setUp() {
        spinner = new JSpinner();
        propertyChangeController = new PropertyChangeController();
        spinner.addPropertyChangeListener(propertyChangeController);
        chl = new ChangeController();
        spinner.addChangeListener(chl);
        abstractModel = new SpinnerModel() {
            public void addChangeListener(ChangeListener l) {
            }

            public Object getNextValue() {
                return null;
            }

            public Object getPreviousValue() {
                return null;
            }

            public Object getValue() {
                return null;
            }

            public void removeChangeListener(ChangeListener l) {
            }

            public void setValue(Object value) {
            }
        };
    }

    @Override
    public void tearDown() {
        spinner = null;
        abstractModel = null;
        propertyChangeController = null;
        chl = null;
    }

    public void testJSpinner() {
        assertTrue(spinner.getModel() instanceof SpinnerNumberModel);
        assertTrue(spinner.getEditor() instanceof JSpinner.NumberEditor);
        assertTrue(Arrays.asList(spinner.getChangeListeners()).contains(spinner.getEditor()));
    }

    public void testSetGetUpdateUI() {
        assertEquals(spinner.getUIClassID(), "SpinnerUI");
        SpinnerUI defaultUI = spinner.getUI();
        assertNotNull(defaultUI);
        SpinnerUI ui = new SpinnerUI() {
        };
        spinner.setUI(ui);
        assertEquals(ui, spinner.getUI());
        assertTrue(propertyChangeController.isChanged("UI"));
        propertyChangeController.reset();
        spinner.updateUI();
        assertNotSame(ui, spinner.getUI());
        assertTrue(propertyChangeController.isChanged("UI"));
    }

    public void testSetGetModel() {
        SpinnerNumberModel newModel = new SpinnerNumberModel(0, -1, 1, 1);
        spinner.setModel(newModel);
        assertSame(newModel, spinner.getModel());
        assertTrue(propertyChangeController.isChanged(StringConstants.MODEL_PROPERTY_CHANGED));
        assertTrue(propertyChangeController.isChanged(StringConstants.EDITOR_PROPERTY_CHANGED));
        assertEquals(1, newModel.getChangeListeners().length);
        spinner.setEditor(new JButton());
        propertyChangeController.reset();
        spinner.setModel(new SpinnerNumberModel(0, -2, 2, 1));
        assertFalse(propertyChangeController.isChanged(StringConstants.EDITOR_PROPERTY_CHANGED));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                spinner.setModel(null);
            }
        });
        if (isHarmony()) {
            SpinnerNumberModel old = (SpinnerNumberModel) spinner.getModel();
            spinner.setModel(new SpinnerDateModel());
            assertEquals(old.getChangeListeners().length, 0);
        }
    }

    public void testSetGetValue() {
        spinner.setValue(new Integer(10));
        assertTrue(chl.isChanged());
        assertEquals(spinner.getModel().getValue(), spinner.getValue());
    }

    public void testGetPreviousNextValue() {
        assertEquals(spinner.getNextValue(), spinner.getModel().getNextValue());
        assertEquals(spinner.getPreviousValue(), spinner.getModel().getPreviousValue());
    }

    public void testAddRemoveGetChangeListener() {
        assertEquals(2, spinner.getChangeListeners().length);
        assertTrue(Arrays.asList(spinner.getChangeListeners()).contains(chl));
        ChangeListener listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
            }
        };
        spinner.addChangeListener(listener);
        assertTrue(Arrays.asList(spinner.getChangeListeners()).contains(listener));
        spinner.removeChangeListener(chl);
        spinner.removeChangeListener(listener);
        assertEquals(1, spinner.getChangeListeners().length);
    }

    public void testFireStateChanged() {
        spinner.getModel().setValue(new Integer(10));
        assertTrue(chl.isChanged());
    }

    public void testSetGetEditor() throws Exception {
        JComponent oldEditor = spinner.getEditor();
        assertNotNull(oldEditor);
        JComponent editor = new JProgressBar();
        spinner.setEditor(editor);
        assertFalse(Arrays.asList(spinner.getChangeListeners()).contains(editor));
        assertTrue(propertyChangeController.isChanged(StringConstants.EDITOR_PROPERTY_CHANGED));
        assertSame(editor, spinner.getEditor());
        editor = new JSpinner.NumberEditor(spinner);
        spinner.setEditor(editor);
        assertTrue(Arrays.asList(spinner.getChangeListeners()).contains(editor));
        assertTrue(propertyChangeController.isChanged(StringConstants.EDITOR_PROPERTY_CHANGED));
        assertSame(editor, spinner.getEditor());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                spinner.setEditor(null);
            }
        });
        spinner = new JSpinner();
        spinner.setEditor(oldEditor);
        assertFalse(Arrays.asList(spinner.getChangeListeners()).contains(oldEditor));
        spinner = new JSpinner();
        oldEditor = spinner.getEditor();
        spinner.setEditor(oldEditor);
        spinner.setModel(new SpinnerDateModel());
        assertNotSame(spinner.getEditor(), oldEditor);
        spinner = new JSpinner();
        oldEditor = spinner.getEditor();
        spinner.setEditor(oldEditor);
        spinner.setModel(new SpinnerDateModel());
        assertNotSame(spinner.getEditor(), oldEditor);
    }

    public void testCreateEditor() {
        SpinnerModel model = new SpinnerNumberModel();
        spinner.setModel(model);
        assertTrue(spinner.createEditor(model) instanceof JSpinner.NumberEditor);
        model = new SpinnerDateModel();
        spinner.setModel(model);
        assertTrue(spinner.createEditor(model) instanceof JSpinner.DateEditor);
        model = new SpinnerListModel();
        spinner.setModel(model);
        assertTrue(spinner.createEditor(model) instanceof JSpinner.ListEditor);
        assertTrue(spinner.createEditor(abstractModel) instanceof JSpinner.DefaultEditor);
    }

    public void testGetAccessibleContext() {
    }

    public void testDefaultEditor_DefaultEditor() {
        DefaultEditor defaultEditor = (DefaultEditor) spinner.createEditor(abstractModel);
        spinner.setEditor(defaultEditor);
        assertTrue(Arrays.asList(defaultEditor.getTextField().getPropertyChangeListeners())
                .contains(defaultEditor));
        assertFalse(defaultEditor.getTextField().isEditable());
        assertSame(defaultEditor.getTextField(), defaultEditor.getComponent(0));
        JFormattedTextField textField = ((DefaultEditor) spinner.getEditor()).getTextField();
        assertSame(textField.getActionForKeyStroke(KeyStroke.getKeyStroke("DOWN")), textField
                .getActionForKeyStroke(KeyStroke.getKeyStroke("UP")));
        textField.setFormatterFactory(new DefaultFormatterFactory(null));
        textField.setValue("TEST");
        assertEquals(new Integer(0), textField.getValue());
    }

    public void testDefaultEditor_propertyChange() throws Exception {
        DefaultEditor defaultEditor = (DefaultEditor) spinner.getEditor();
        PropertyChangeController pcc = new PropertyChangeController();
        ChangeController modelController = new ChangeController();
        defaultEditor.getTextField().addPropertyChangeListener(pcc);
        spinner.getModel().addChangeListener(modelController);
        defaultEditor.getTextField().setText("15");
        defaultEditor.commitEdit();
        assertTrue(pcc.isChanged());
        assertTrue(modelController.isChanged());
        modelController.reset();
        defaultEditor.getTextField().removePropertyChangeListener(defaultEditor);
        defaultEditor.getTextField().setText("18");
        defaultEditor.commitEdit();
        defaultEditor.getTextField().addPropertyChangeListener(defaultEditor);
        defaultEditor.propertyChange(new PropertyChangeEvent(defaultEditor.getTextField(),
                "value", defaultEditor.getTextField().getValue(), new Integer(10)));
        assertTrue(modelController.isChanged());
        modelController.reset();
        defaultEditor.getTextField().removePropertyChangeListener(defaultEditor);
        defaultEditor.getTextField().setText("58");
        defaultEditor.commitEdit();
        defaultEditor.getTextField().addPropertyChangeListener(defaultEditor);
        defaultEditor.propertyChange(new PropertyChangeEvent(new Integer(10), "value",
                new Integer(13), new Integer(10)));
        assertFalse(modelController.isChanged());
    }

    public void testDefaultEditor_stateChange() {
        DefaultEditor defaultEditor = (DefaultEditor) spinner.getEditor();
        PropertyChangeController pcc = new PropertyChangeController();
        defaultEditor.getTextField().addPropertyChangeListener(pcc);
        spinner.setValue(new Integer(159));
        assertTrue(pcc.isChanged());
    }

    public void testDefaultEditor_LayoutSizes() {
        DefaultEditor defaultEditor = (DefaultEditor) spinner.createEditor(abstractModel);
        spinner.setEditor(defaultEditor);
        assertEquals(defaultEditor.minimumLayoutSize(spinner), Utilities.addInsets(
                defaultEditor.getTextField().getMinimumSize(), spinner.getInsets()));
        assertEquals(defaultEditor.preferredLayoutSize(spinner), Utilities.addInsets(
                defaultEditor.getTextField().getPreferredSize(), spinner.getInsets()));
    }

    public void testNumberEditor_NumberEditor() {
        spinner.getModel().setValue(new Integer(5));
        NumberEditor numEditor = new NumberEditor(spinner);
        spinner.setEditor(numEditor);
        assertTrue(numEditor.getTextField().isEditable());
        assertTrue(numEditor.getTextField().getFormatter() instanceof NumberFormatter);
        assertEquals(numEditor.getTextField().getValue(), new Integer(5));
        assertSame(((NumberFormatter) numEditor.getTextField().getFormatter()).getValueClass(),
                Integer.class);
        assertNull(((NumberFormatter) numEditor.getTextField().getFormatter()).getMinimum());
        assertNull(((NumberFormatter) numEditor.getTextField().getFormatter()).getMaximum());
        assertTrue(numEditor.getFormat().equals(new DecimalFormat()));
        spinner.setModel(abstractModel);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new JSpinner.NumberEditor(spinner);
            }
        });
    }

    public void testNumberEditor_formatter() {
        NumberEditor numEditor = new NumberEditor(spinner);
        spinner.setEditor(numEditor);
        final Integer max1 = new Integer(777);
        NumberFormatter numberFormatter = ((NumberFormatter) numEditor.getTextField()
                .getFormatter());
        numberFormatter.setMaximum(max1);
        assertSame(numberFormatter.getMaximum(), max1);
        assertSame(numEditor.getModel().getMaximum(), max1);
        final Integer max2 = new Integer(555);
        numEditor.getModel().setMaximum(max2);
        assertSame(numberFormatter.getMaximum(), max2);
        assertSame(numEditor.getModel().getMaximum(), max2);
        SpinnerNumberModel old = (SpinnerNumberModel) spinner.getModel();
        spinner.setModel(abstractModel);
        final Integer max3 = new Integer(333);
        old.setMaximum(max3);
        assertSame(((NumberFormatter) ((NumberEditor) spinner.getEditor()).getTextField()
                .getFormatter()).getMaximum(), max3);
    }

    public void testNumberEditor_getModel() {
        NumberEditor numEditor = new NumberEditor(spinner);
        spinner.setEditor(numEditor);
        assertSame(numEditor.getModel(), spinner.getModel());
        spinner.setModel(abstractModel);
        testExceptionalCase(new ExceptionalCase(null, ClassCastException.class) {
            @Override
            public void exceptionalAction() throws Exception {
                ((NumberEditor) spinner.getEditor()).getModel();
            }
        });
    }

    public void testDateEditor_DateEditor() {
        spinner.setModel(new SpinnerDateModel());
        DateEditor dateEditor = (DateEditor) spinner.getEditor();
        spinner.setEditor(dateEditor);
        assertTrue(dateEditor.getTextField().isEditable());
        assertTrue(dateEditor.getTextField().getFormatter() instanceof DateFormatter);
        assertNull(((DateFormatter) dateEditor.getTextField().getFormatter()).getMinimum());
        assertNull(((DateFormatter) dateEditor.getTextField().getFormatter()).getMaximum());
        assertTrue(dateEditor.getFormat().equals(new SimpleDateFormat()));
        spinner.setModel(abstractModel);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new JSpinner.DateEditor(spinner);
            }
        });
    }

    public void testDateEditor_formatter() {
        spinner.setModel(new SpinnerDateModel());
        DateEditor dateEditor = new DateEditor(spinner);
        spinner.setEditor(dateEditor);
        final Date date1 = new Date(777);
        DateFormatter dateFormatter = ((DateFormatter) dateEditor.getTextField().getFormatter());
        dateFormatter.setMaximum(date1);
        assertSame(dateFormatter.getMaximum(), date1);
        assertSame(dateEditor.getModel().getEnd(), date1);
        final Date date2 = new Date(555);
        dateEditor.getModel().setEnd(date2);
        assertSame(dateFormatter.getMaximum(), date2);
        assertSame(dateEditor.getModel().getEnd(), date2);
        SpinnerDateModel old = (SpinnerDateModel) spinner.getModel();
        spinner.setModel(abstractModel);
        final Date date3 = new Date(555);
        old.setEnd(date3);
        assertEquals(((DateFormatter) ((DateEditor) spinner.getEditor()).getTextField()
                .getFormatter()).getMaximum(), date3);
    }

    public void testListEditor_ListEditor() {
        Object[] values = { "arrline1", "arrline2", "text", new Integer(33), spinner };
        spinner.setModel(new SpinnerListModel(values));
        ListEditor listEditor = new ListEditor(spinner);
        spinner.setEditor(listEditor);
        assertTrue(listEditor.getTextField().isEditable());
        spinner.setModel(abstractModel);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new JSpinner.ListEditor(spinner);
            }
        });
    }

    public void testListEditor_formatter() throws Exception {
        JComponent comp = new JButton();
        Object[] values = { "arrline1", "arrline2", "text", new Integer(33), comp };
        spinner.setModel(new SpinnerListModel(values));
        ListEditor listEditor = new ListEditor(spinner);
        spinner.setEditor(listEditor);
        AbstractFormatter formatter = ((ListEditor) spinner.getEditor()).getTextField()
                .getFormatter();
        assertEquals(formatter.valueToString(null), "");
        assertEquals(formatter.valueToString(new Integer(33)), "33");
        assertEquals(formatter.stringToValue("text"), "text");
    }
}
