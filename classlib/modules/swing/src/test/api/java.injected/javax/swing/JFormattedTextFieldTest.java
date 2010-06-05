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
 * @author Evgeniya G. Maenkova
 */
package javax.swing;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.InternationalFormatter;
import javax.swing.text.NumberFormatter;
import javax.swing.text.TextAction;

public class JFormattedTextFieldTest extends SwingTestCase {
    JFrame jf;

    DbgFormattedField tf;

    boolean bWasException;

    String message;

    PropertyChangeListenerImpl listener = new PropertyChangeListenerImpl();

    private FocusEvent FOCUS_LOST;

    private FocusEvent FOCUS_GAINED;

    class PropertyChangeListenerImpl implements PropertyChangeListener {
        String name;

        Object oldValue;

        Object newValue;

        String interestingPropertyName;

        public void propertyChange(final PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if (propertyName.equals(interestingPropertyName)) {
                name = e.getPropertyName();
                oldValue = e.getOldValue();
                newValue = e.getNewValue();
            }
        }

        final void setInterestingPropertyName(final String propertyName) {
            interestingPropertyName = propertyName;
        }
    }

    class DbgFormattedField extends JFormattedTextField {
        private static final long serialVersionUID = 1L;

        private boolean wasCallCommitEdit;

        @Override
        public void commitEdit() throws ParseException {
            wasCallCommitEdit = true;
            super.commitEdit();
        }

        boolean wasCallCommitEdit() {
            boolean was = wasCallCommitEdit;
            wasCallCommitEdit = false;
            return was;
        }
    };

    class DbgFormatter extends DefaultFormatter {
        private static final long serialVersionUID = 1L;

        boolean wasCallInstall;

        boolean wasCallUninstall;

        @Override
        public void install(final JFormattedTextField ftf) {
            wasCallInstall = true;
            super.install(ftf);
        }

        @Override
        public void uninstall() {
            wasCallUninstall = true;
            super.uninstall();
        }

        boolean wasCallInstall() {
            boolean result = wasCallInstall;
            wasCallInstall = false;
            return result;
        }

        boolean wasCallUninstall() {
            boolean result = wasCallUninstall;
            wasCallUninstall = false;
            return result;
        }
    }

    class FTF extends JFormattedTextField {
        private static final long serialVersionUID = 1L;

        boolean hasFocus;

        public FTF(final boolean hasFocus) {
            setHasFocus(hasFocus);
        }

        @Override
        public boolean hasFocus() {
            return hasFocus;
        }

        public void setHasFocus(final boolean hasFocus) {
            this.hasFocus = hasFocus;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jf = new JFrame();
        tf = new DbgFormattedField();
        tf.addPropertyChangeListener(listener);
        initFocusEvent();
        jf.getContentPane().add(tf);
        jf.setSize(200, 300);
        jf.pack();
        bWasException = false;
        message = null;
    }

    private void initFocusEvent() {
        if (FOCUS_GAINED == null || FOCUS_LOST == null) {
            FOCUS_GAINED = new FocusEvent(tf, FocusEvent.FOCUS_GAINED);
            FOCUS_LOST = new FocusEvent(tf, FocusEvent.FOCUS_LOST);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        jf.dispose();
    }

    private void checkEvent(final String name, final Object oldValue, final Object newValue) {
        assertEquals(name, listener.name);
        assertEquals(oldValue, listener.oldValue);
        assertEquals(newValue, listener.newValue);
    }

    public void testJFormattedTextFieldObject() {
        Object value = Color.RED;
        JFormattedTextField tf1 = new JFormattedTextField(value);
        assertEquals(value, tf1.getValue());
        assertEquals(JFormattedTextField.COMMIT_OR_REVERT, tf1.getFocusLostBehavior());
        assertTrue(tf1.getFormatter() instanceof DefaultFormatter);
        assertTrue(tf1.getFormatterFactory() instanceof DefaultFormatterFactory);
        DefaultFormatterFactory factory = (DefaultFormatterFactory) tf1.getFormatterFactory();
        assertTrue(factory.getDefaultFormatter() instanceof DefaultFormatter);
        assertNull(factory.getEditFormatter());
        assertNull(factory.getDisplayFormatter());
        assertNull(factory.getNullFormatter());
    }

    public void testJFormattedTextFieldObject_NullToString() {
        final Object value = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };
        final JFormattedTextField ftf = new JFormattedTextField(value);
        assertEquals("", ftf.getText());
    }

    public void testJFormattedTextFieldAbstractFormatter() {
        InternationalFormatter formatter = new InternationalFormatter();
        JFormattedTextField tf1 = new JFormattedTextField(formatter);
        assertNull(tf1.getValue());
        assertEquals(JFormattedTextField.COMMIT_OR_REVERT, tf1.getFocusLostBehavior());
        assertEquals(formatter, tf1.getFormatter());
        assertTrue(tf1.getFormatterFactory() instanceof DefaultFormatterFactory);
        DefaultFormatterFactory factory = (DefaultFormatterFactory) tf1.getFormatterFactory();
        assertNull(factory.getDisplayFormatter());
        assertNull(factory.getEditFormatter());
        assertEquals(formatter, factory.getDefaultFormatter());
        assertNull(factory.getNullFormatter());
    }

    public void testJFormattedTextFieldFormat() {
        Format format = new SimpleDateFormat();
        InternationalFormatter formatter;
        DefaultFormatterFactory factory;
        JFormattedTextField tf1 = new JFormattedTextField(format);
        assertNull(tf1.getValue());
        assertEquals(JFormattedTextField.COMMIT_OR_REVERT, tf1.getFocusLostBehavior());
        assertTrue(tf1.getFormatter() instanceof DateFormatter);
        formatter = (InternationalFormatter) tf1.getFormatter();
        assertEquals(format, formatter.getFormat());
        assertTrue(tf1.getFormatterFactory() instanceof DefaultFormatterFactory);
        factory = (DefaultFormatterFactory) tf1.getFormatterFactory();
        assertEquals(formatter, factory.getDefaultFormatter());
        format = new MessageFormat("");
        tf1 = new JFormattedTextField(format);
        assertTrue(tf1.getFormatter() instanceof InternationalFormatter);
        formatter = (InternationalFormatter) tf1.getFormatter();
        assertEquals(format, formatter.getFormat());
        factory = (DefaultFormatterFactory) tf1.getFormatterFactory();
        assertEquals(formatter, factory.getDefaultFormatter());
        format = new DecimalFormat();
        tf1 = new JFormattedTextField(format);
        assertTrue(tf1.getFormatter() instanceof NumberFormatter);
        formatter = (InternationalFormatter) tf1.getFormatter();
        assertEquals(format, formatter.getFormat());
        factory = (DefaultFormatterFactory) tf1.getFormatterFactory();
        assertEquals(formatter, factory.getDefaultFormatter());
    }

    public void testJFormattedTextFieldAbstractFormatterFactory() {
        JFormattedTextField.AbstractFormatterFactory factory = new DefaultFormatterFactory();
        JFormattedTextField tf1 = new JFormattedTextField(factory);
        assertNull(tf1.getValue());
        assertEquals(JFormattedTextField.COMMIT_OR_REVERT, tf1.getFocusLostBehavior());
        assertNull(tf1.getFormatter());
        assertEquals(factory, tf1.getFormatterFactory());
    }

    public void testJFormattedTextFieldAbstractFormatterFactoryObject() {
        Object value = Color.RED;
        JFormattedTextField.AbstractFormatterFactory factory = new DefaultFormatterFactory();
        JFormattedTextField tf1 = new JFormattedTextField(factory, value);
        assertEquals(value, tf1.getValue());
        assertEquals(JFormattedTextField.COMMIT_OR_REVERT, tf1.getFocusLostBehavior());
        assertNull(tf1.getFormatter());
        assertEquals(factory, tf1.getFormatterFactory());
    }

    public void testSetGetValue() {
        String propertyName = "value";
        listener.setInterestingPropertyName(propertyName);
        assertNull(tf.getValue());
        Object value = Color.RED;
        tf.setValue(value);
        assertEquals(value, tf.getValue());
        checkEvent(propertyName, null, value);
        value = "just value";
        tf.setValue(value);
        assertEquals(value, tf.getText());
        assertEquals(value, tf.getValue());
        checkEvent(propertyName, Color.RED, value);
    }

    public void testSetGetFormatter() {
        String propertyName = "textFormatter";
        listener.setInterestingPropertyName(propertyName);
        assertNull(tf.getFormatter());
        DbgFormatter formatter = new DbgFormatter();
        tf.setFormatter(formatter);
        assertTrue(formatter.wasCallInstall());
        assertEquals(formatter, tf.getFormatter());
        checkEvent(propertyName, null, formatter);
        DbgFormatter formatter1 = new DbgFormatter();
        tf.setFormatter(formatter1);
        assertTrue(formatter1.wasCallInstall());
        assertTrue(formatter.wasCallUninstall());
        assertEquals(formatter1, tf.getFormatter());
        checkEvent(propertyName, formatter, formatter1);
    }

    public void testSetGetFocusLostBehavior() {
        assertEquals(JFormattedTextField.COMMIT_OR_REVERT, tf.getFocusLostBehavior());
        tf.setFocusLostBehavior(JFormattedTextField.COMMIT);
        assertEquals(JFormattedTextField.COMMIT, tf.getFocusLostBehavior());
        tf.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        assertEquals(JFormattedTextField.COMMIT_OR_REVERT, tf.getFocusLostBehavior());
        tf.setFocusLostBehavior(JFormattedTextField.REVERT);
        assertEquals(JFormattedTextField.REVERT, tf.getFocusLostBehavior());
        tf.setFocusLostBehavior(JFormattedTextField.PERSIST);
        assertEquals(JFormattedTextField.PERSIST, tf.getFocusLostBehavior());
        try {
            tf.setFocusLostBehavior(-2);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        checkException("setFocusLostBehavior must be one of: " + "JFormattedTextField.COMMIT, "
                + "JFormattedTextField.COMMIT_OR_REVERT, " + "JFormattedTextField.PERSIST "
                + "or JFormattedTextField.REVERT");
        try {
            tf.setFocusLostBehavior(4);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        checkException("setFocusLostBehavior must be one of: " + "JFormattedTextField.COMMIT, "
                + "JFormattedTextField.COMMIT_OR_REVERT, " + "JFormattedTextField.PERSIST "
                + "or JFormattedTextField.REVERT");
    }

    public void testSetGetFormatterFactory() {
        String propertyName = "formatterFactory";
        listener.setInterestingPropertyName(propertyName);
        assertNull(tf.getFormatterFactory());
        DefaultFormatterFactory factory = new DefaultFormatterFactory();
        tf.setFormatterFactory(factory);
        assertEquals(factory, tf.getFormatterFactory());
        checkEvent(propertyName, null, factory);
    }

    public void testProcessFocusEventFocusEvent() {
        Long value = new Long(56);
        tf.setValue(new Integer(345));
        tf.setFocusLostBehavior(JFormattedTextField.COMMIT);
        assertFalse(tf.wasCallCommitEdit());
        tf.setText("667");
        tf.processFocusEvent(FOCUS_LOST);
        assertTrue(tf.wasCallCommitEdit());
        assertEquals("667", tf.getText());
        assertEquals(new Long(667), tf.getValue());
        tf.setText("34ft");
        tf.processFocusEvent(FOCUS_LOST);
        assertEquals("34", tf.getText());
        assertTrue(tf.wasCallCommitEdit());
        assertEquals(new Long(34), tf.getValue());
        tf.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        tf.setText("667");
        tf.processFocusEvent(FOCUS_LOST);
        assertTrue(tf.wasCallCommitEdit());
        assertEquals("667", tf.getText());
        assertEquals(new Long(667), tf.getValue());
        tf.setText("56&");
        tf.processFocusEvent(FOCUS_LOST);
        assertTrue(tf.wasCallCommitEdit());
        assertEquals("56", tf.getText());
        assertEquals(value, tf.getValue());
        tf.setFocusLostBehavior(JFormattedTextField.REVERT);
        tf.setText("667");
        tf.processFocusEvent(FOCUS_LOST);
        assertFalse(tf.wasCallCommitEdit());
        assertEquals("56", tf.getText());
        assertEquals(value, tf.getValue());
        tf.setText("323rtft");
        tf.processFocusEvent(FOCUS_LOST);
        assertFalse(tf.wasCallCommitEdit());
        assertEquals("56", tf.getText());
        assertEquals(value, tf.getValue());
        tf.setFocusLostBehavior(JFormattedTextField.PERSIST);
        tf.setText("667");
        tf.processFocusEvent(FOCUS_LOST);
        assertFalse(tf.wasCallCommitEdit());
        assertEquals("667", tf.getText());
        assertEquals(value, tf.getValue());
        tf.setText("67ft");
        tf.processFocusEvent(FOCUS_LOST);
        assertFalse(tf.wasCallCommitEdit());
        assertEquals("67ft", tf.getText());
        assertEquals(value, tf.getValue());
    }

    public void testSetDocumentDocument() {
    }

    public void testGetActions() {
        Action[] actions = tf.getActions();
        Action[] defaultActions = new DefaultEditorKit().getActions();
        assertEquals(defaultActions.length + 2, actions.length);
        Action cancellAction = null;
        Action commitAction = null;
        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            String name = (String) action.getValue(Action.NAME);
            if ("notify-field-accept".equals(name)) {
                commitAction = action;
                continue;
            } else if ("reset-field-edit".equals(name)) {
                cancellAction = action;
                continue;
            } else {
                boolean fromDefaultAction = false;
                for (int j = 0; j < defaultActions.length; j++) {
                    if (defaultActions[j].equals(action)) {
                        fromDefaultAction = true;
                        break;
                    }
                }
                assertTrue(fromDefaultAction);
            }
        }
        assertTrue(cancellAction instanceof TextAction);
        assertTrue(commitAction instanceof TextAction);
        //TODO check commit & cancel actions
    }

    public void testCommitEdit() {
        tf.setFormatter(new NumberFormatter());
        assertNull(tf.getValue());
        tf.setText("678");
        try {
            tf.commitEdit();
        } catch (ParseException e) {
            assertTrue("Unexpected exception: ", false);
        }
        assertEquals(new Long(678), tf.getValue());
    }

    public void testIsEditValid() {
        String propertyName = "editValid";
        listener.setInterestingPropertyName(propertyName);
        tf.setValue(new Integer(90000000));
        String text = "5323";
        tf.setText(text);
        assertTrue(tf.isEditValid());
        checkEvent(propertyName, Boolean.FALSE, Boolean.TRUE);
        text = "valid or invalid?";
        tf.setText(text);
        assertFalse(tf.isEditValid());
        assertEquals(text, tf.getText());
        checkEvent(propertyName, Boolean.TRUE, Boolean.FALSE);
    }

    public void testConstants() {
        assertEquals(0, JFormattedTextField.COMMIT);
        assertEquals(1, JFormattedTextField.COMMIT_OR_REVERT);
        assertEquals(2, JFormattedTextField.REVERT);
        assertEquals(3, JFormattedTextField.PERSIST);
    }

    public void testGetUIClassID() {
        assertEquals("FormattedTextFieldUI", tf.getUIClassID());
    }

    private void checkException(final String text) {
        assertTrue(bWasException);
        assertEquals(text, message);
        bWasException = false;
        message = null;
    }

    private DefaultFormatterFactory getFactoryIfDefault(
            final JFormattedTextField.AbstractFormatterFactory factory) {
        assertTrue(factory instanceof DefaultFormatterFactory);
        return (DefaultFormatterFactory) factory;
    }

    private void checkDefaultFormatter(final DefaultFormatterFactory factory) {
        assertTrue(factory.getDefaultFormatter() instanceof DefaultFormatter);
        assertNull(factory.getDisplayFormatter());
        assertNull(factory.getEditFormatter());
        assertNull(factory.getNullFormatter());
    }

    public void testCreateFormattersFactory() {
        DefaultFormatterFactory factory;
        tf.setValue(new Integer(34));
        factory = getFactoryIfDefault(tf.getFormatterFactory());
        assertTrue(factory.getDefaultFormatter() instanceof NumberFormatter);
        //TODO: check if factory.getDefaultFormatter() should be same to factory.getDisplayFormatter()
        // or factory.getEditFormatter().
        assertNull(factory.getNullFormatter());
        tf.setFormatterFactory(null);
        tf.setValue(new Date());
        factory = getFactoryIfDefault(tf.getFormatterFactory());
        assertTrue(factory.getDefaultFormatter() instanceof DateFormatter);
        assertNull(factory.getDisplayFormatter());
        assertNull(factory.getEditFormatter());
        assertNull(factory.getNullFormatter());
        tf.setFormatterFactory(null);
        tf.setValue("sdffsdf");
        factory = getFactoryIfDefault(tf.getFormatterFactory());
        checkDefaultFormatter(factory);
        tf.setFormatterFactory(null);
        tf.setValue(Color.RED);
        factory = getFactoryIfDefault(tf.getFormatterFactory());
        checkDefaultFormatter(factory);
    }
}
