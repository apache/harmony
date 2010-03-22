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

package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleEditableText;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleText;
import javax.accessibility.AccessibleValue;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.SpinnerUI;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NumberFormatter;
import org.apache.harmony.luni.util.NotImplementedException;
import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>JSpinner</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JSpinner extends JComponent implements Accessible {
    private static final long serialVersionUID = 5455034942343575490L;

    protected class AccessibleJSpinner extends JComponent.AccessibleJComponent implements
            AccessibleValue, AccessibleAction, AccessibleText, AccessibleEditableText,
            ChangeListener {
        private static final long serialVersionUID = -8871493856204319541L;

        protected AccessibleJSpinner() {
        }

        public void stateChanged(ChangeEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleRole getAccessibleRole() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public int getAccessibleChildrenCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public Accessible getAccessibleChild(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleAction getAccessibleAction() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleText getAccessibleText() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleValue getAccessibleValue() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Number getCurrentAccessibleValue() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public boolean setCurrentAccessibleValue(Number n) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Number getMinimumAccessibleValue() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Number getMaximumAccessibleValue() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleActionCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getAccessibleActionDescription(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public boolean doAccessibleAction(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getIndexAtPoint(Point p) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Rectangle getCharacterBounds(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getCharCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getCaretPosition() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getAtIndex(int part, int index) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getAfterIndex(int part, int index) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getBeforeIndex(int part, int index) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public AttributeSet getCharacterAttribute(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getSelectionStart() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getSelectionEnd() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getSelectedText() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void setTextContents(String s) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void insertTextAtIndex(int index, String s) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getTextRange(int startIndex, int endIndex) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void delete(int startIndex, int endIndex) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void cut(int startIndex, int endIndex) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void paste(int startIndex) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void replaceText(int startIndex, int endIndex, String s)
                throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void selectText(int startIndex, int endIndex) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void setAttributes(int startIndex, int endIndex, AttributeSet as)
                throws NotImplementedException {
            throw new NotImplementedException();
        }
    }

    public static class DefaultEditor extends JPanel implements ChangeListener,
            PropertyChangeListener, LayoutManager {
        private static final long serialVersionUID = 6977154593437159148L;

        private JFormattedTextField text;

        private JSpinner spinner;

        public DefaultEditor(JSpinner spinner) {
            this.spinner = spinner;
            setLayout(this);
            spinner.addChangeListener(this);
            text = new JFormattedTextField();
            text.setEditable(false);
            text.setValue(spinner.getModel().getValue());
            text.addPropertyChangeListener(this);
            text.getActionMap().put("increment", disabledAction);
            text.getActionMap().put("decrement", disabledAction);
            add(text);
        }

        public JSpinner getSpinner() {
            return spinner;
        }

        public JFormattedTextField getTextField() {
            return text;
        }

        public void stateChanged(ChangeEvent e) {
            if (this != spinner.getEditor()) {
                return;
            }
            text.setValue(spinner.getValue());
        }

        public void propertyChange(PropertyChangeEvent e) {
            if (e.getSource() != text || this != spinner.getEditor()) {
                return;
            }
            if (StringConstants.VALUE_PROPERTY_NAME.equals(e.getPropertyName())) {
                try {
                    spinner.setValue(e.getNewValue());
                } catch (IllegalArgumentException ex) {
                    text.setValue(e.getOldValue());
                }
            }
        }

        public void addLayoutComponent(String name, Component child) {
        }

        public void removeLayoutComponent(Component child) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            return Utilities.addInsets(text.getPreferredSize(), parent.getInsets());
        }

        public Dimension minimumLayoutSize(Container parent) {
            return Utilities.addInsets(text.getMinimumSize(), parent.getInsets());
        }

        public void layoutContainer(Container parent) {
            text.setBounds(0, 0, parent.getWidth(), parent.getHeight());
        }

        public void commitEdit() throws ParseException {
            text.commitEdit();
        }

        public void dismiss(JSpinner spinner) {
            spinner.removeChangeListener(this);
        }
    }

    @SuppressWarnings("unchecked")
    private static class SpinnerDateFormatter extends DateFormatter {
        private static final long serialVersionUID = 1L;

        private SpinnerDateModel model;

        public SpinnerDateFormatter(final SimpleDateFormat format, final SpinnerDateModel model) {
            super(format);
            this.model = model;
        }

        @Override
        public void setMaximum(Comparable max) {
            super.setMaximum(max);
            model.setEnd(max);
        }

        @Override
        public void setMinimum(Comparable min) {
            super.setMinimum(min);
            model.setStart(min);
        }

        @Override
        public Comparable getMaximum() {
            Comparable max = model.getEnd();
            super.setMaximum(max);
            return max;
        }

        @Override
        public Comparable getMinimum() {
            Comparable min = model.getStart();
            super.setMinimum(min);
            return min;
        }
    }

    public static class DateEditor extends DefaultEditor {
        private static final long serialVersionUID = 1L;

        private SimpleDateFormat format;

        public DateEditor(JSpinner spinner) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerDateModel)) {
                throw new IllegalArgumentException(Messages.getString("swing.2C","SpinnerDateModel")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            format = new SimpleDateFormat();
            initTextField();
        }

        public DateEditor(JSpinner spinner, String dateFormatPattern) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerDateModel)) {
                throw new IllegalArgumentException(Messages.getString("swing.2C","SpinnerDateModel")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            format = new SimpleDateFormat(dateFormatPattern);
            initTextField();
        }

        public SimpleDateFormat getFormat() {
            return format;
        }

        public SpinnerDateModel getModel() {
            return (SpinnerDateModel) this.getSpinner().getModel();
        }

        private void initTextField() {
            SpinnerDateFormatter formatter = new SpinnerDateFormatter(format, getModel());
            JFormattedTextField textField = getTextField();
            textField.setFormatterFactory(new DefaultFormatterFactory(formatter));
            textField.setEditable(true);
        }
    }

    private static class SpinnerListFormatter extends AbstractFormatter {
        private static final long serialVersionUID = 1L;

        private class ListFilter extends DocumentFilter {
            private JFormattedTextField textField;

            public ListFilter(JFormattedTextField textField) {
                this.textField = textField;
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String text,
                    AttributeSet attrs) throws BadLocationException {
                super.insertString(fb, offset, text, attrs);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text,
                    AttributeSet attrs) throws BadLocationException {
                String str = textField.getText().substring(0, offset) + text;
                String replace = findElementText(str);
                if (!"".equals(replace)) {
                    fb.replace(0, textField.getText().length(), replace, attrs);
                    textField.setCaretPosition(offset + text.length());
                    textField.moveCaretPosition(textField.getText().length());
                } else {
                    super.replace(fb, offset, length, text, attrs);
                }
            }

            private String findElementText(String text) {
                Object findElement = findElement(text);
                if (findElement == null) {
                    return "";
                }
                String result = findElement.toString();
                return (result.indexOf(text) == 0) ? result : "";
            }
        }

        private SpinnerListModel model;

        private ListFilter filter;

        public SpinnerListFormatter(SpinnerListModel model, JFormattedTextField textField) {
            this.model = model;
            filter = new ListFilter(textField);
        }

        @Override
        public Object stringToValue(String text) throws ParseException {
            return ("".equals(text)) ? null : findElement(text);
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            return (value == null ? "" : value.toString());
        }

        @Override
        protected DocumentFilter getDocumentFilter() {
            return filter;
        }

        private Object findElement(String text) {
            List<?> modelList = model.getList();
            for (int i = 0; i < modelList.size(); i++) {
                Object obj = modelList.get(i);
                if (obj != null && obj.toString().indexOf(text) == 0) {
                    return obj;
                }
            }
            return modelList.get(0);
        }
    }

    public static class ListEditor extends DefaultEditor {
        private static final long serialVersionUID = 1L;

        public ListEditor(JSpinner spinner) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerListModel)) {
                throw new IllegalArgumentException(Messages.getString("swing.2C","SpinnerListModel")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            SpinnerListFormatter formatter = new SpinnerListFormatter(this.getModel(), this
                    .getTextField());
            JFormattedTextField textField = this.getTextField();
            textField.setFormatterFactory(new DefaultFormatterFactory(formatter));
            textField.setEditable(true);
        }

        public SpinnerListModel getModel() {
            return (SpinnerListModel) this.getSpinner().getModel();
        }
    }

    @SuppressWarnings("unchecked")
    private static class SpinnerNumberFormatter extends NumberFormatter {
        private static final long serialVersionUID = 1L;

        private SpinnerNumberModel model;

        public SpinnerNumberFormatter(DecimalFormat format, SpinnerNumberModel model) {
            super(format);
            this.model = model;
            setValueClass(model.getValue().getClass());
        }

        @Override
        public void setMaximum(Comparable max) {
            super.setMaximum(max);
            model.setMaximum(max);
        }

        @Override
        public void setMinimum(Comparable min) {
            super.setMinimum(min);
            model.setMinimum(min);
        }

        @Override
        public Comparable getMaximum() {
            Comparable max = model.getMaximum();
            super.setMaximum(max);
            return max;
        }

        @Override
        public Comparable getMinimum() {
            Comparable min = model.getMinimum();
            super.setMinimum(min);
            return min;
        }
    }

    public static class NumberEditor extends DefaultEditor {
        private static final long serialVersionUID = 1L;

        private DecimalFormat format;

        public NumberEditor(JSpinner spinner) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerNumberModel)) {
                throw new IllegalArgumentException(Messages.getString("swing.2C","SpinnerNumberModel")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            format = new DecimalFormat();
            initTextField();
        }

        public NumberEditor(JSpinner spinner, String decimalFormatPattern) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerNumberModel)) {
                throw new IllegalArgumentException(Messages.getString("swing.2C","SpinnerNumberModel")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            format = new DecimalFormat(decimalFormatPattern);
            initTextField();
        }

        public DecimalFormat getFormat() {
            return format;
        }

        public SpinnerNumberModel getModel() {
            return (SpinnerNumberModel) this.getSpinner().getModel();
        }

        private void initTextField() {
            SpinnerNumberFormatter numberFormatter = new SpinnerNumberFormatter(format, this
                    .getModel());
            JFormattedTextField textField = this.getTextField();
            textField.setFormatterFactory(new DefaultFormatterFactory(numberFormatter));
            textField.setHorizontalAlignment(SwingConstants.RIGHT);
            textField.setEditable(true);
        }
    }

    private class ModelChangeListener implements ChangeListener, Serializable {
        private static final long serialVersionUID = 1L;

        public void stateChanged(ChangeEvent e) {
            fireStateChanged();
        }
    }

    private static Action disabledAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    };

    private static final String UI_CLASS_ID = "SpinnerUI";

    private SpinnerModel model;

    private boolean editorSet;

    private JComponent editor;

    private ChangeListener changeListener = new ModelChangeListener();

    private ChangeEvent changeEvent;

    public JSpinner(SpinnerModel model) {
        this.model = model;
        model.addChangeListener(changeListener);
        editor = createEditor(model);
        updateUI();
    }

    public JSpinner() {
        this(new SpinnerNumberModel());
    }

    public SpinnerUI getUI() {
        return (SpinnerUI) ui;
    }

    public void setUI(SpinnerUI ui) {
        super.setUI(ui);
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    @Override
    public void updateUI() {
        setUI((SpinnerUI) UIManager.getUI(this));
    }

    protected JComponent createEditor(SpinnerModel model) {
        if (model instanceof SpinnerNumberModel) {
            return new NumberEditor(this);
        }
        if (model instanceof SpinnerDateModel) {
            return new DateEditor(this);
        }
        if (model instanceof SpinnerListModel) {
            return new ListEditor(this);
        }
        return new DefaultEditor(this);
    }

    public void setModel(SpinnerModel model) {
        if (model == null) {
            throw new IllegalArgumentException(Messages.getString("swing.2F")); //$NON-NLS-1$
        }
        SpinnerModel oldModel = this.model;
        oldModel.removeChangeListener(changeListener);
        this.model = model;
        model.addChangeListener(changeListener);
        firePropertyChange(StringConstants.MODEL_PROPERTY_CHANGED, oldModel, model);
        if (!editorSet) {
            setEditor(createEditor(model));
            editorSet = false;
        }
    }

    public SpinnerModel getModel() {
        return model;
    }

    public Object getValue() {
        return model.getValue();
    }

    public void setValue(Object value) {
        model.setValue(value);
    }

    public Object getNextValue() {
        return model.getNextValue();
    }

    public Object getPreviousValue() {
        return model.getPreviousValue();
    }

    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }

    public ChangeListener[] getChangeListeners() {
        return getListeners(ChangeListener.class);
    }

    protected void fireStateChanged() {
        if (changeEvent == null) {
            changeEvent = new ChangeEvent(this);
        }
        ChangeListener[] listeners = getChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].stateChanged(changeEvent);
        }
    }

    public void setEditor(JComponent editor) {
        if (editor == null) {
            throw new IllegalArgumentException(Messages.getString("swing.30")); //$NON-NLS-1$
        }
        JComponent oldEditor = this.editor;
        if (oldEditor == editor) {
            return;
        }
        if (oldEditor instanceof DefaultEditor) {
            DefaultEditor def = (DefaultEditor) oldEditor;
            def.dismiss(this);
        }
        this.editor = editor;
        editorSet = true;
        firePropertyChange(StringConstants.EDITOR_PROPERTY_CHANGED, oldEditor, editor);
    }

    public JComponent getEditor() {
        return editor;
    }

    public void commitEdit() throws ParseException {
        if (editor instanceof DefaultEditor) {
            ((DefaultEditor) editor).commitEdit();
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJSpinner();
        }
        return accessibleContext;
    }
}
