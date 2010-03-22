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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.TextUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.InternationalFormatter;
import javax.swing.text.JTextComponent;
import javax.swing.text.NavigationFilter;
import javax.swing.text.NumberFormatter;
import javax.swing.text.TextAction;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>JFormattedTextField</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JFormattedTextField extends JTextField {
    private static final long serialVersionUID = 7685569367944517634L;

    public abstract static class AbstractFormatter implements Serializable {
        private JFormattedTextField textField;

        private class ActionMapFormattersResource extends ActionMap {
            private static final long serialVersionUID = 1L;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            AbstractFormatter formatter = (AbstractFormatter) super.clone();
            formatter.install(null);
            return formatter;
        }

        protected Action[] getActions() {
            return null;
        }

        protected DocumentFilter getDocumentFilter() {
            return null;
        }

        protected JFormattedTextField getFormattedTextField() {
            return textField;
        }

        protected NavigationFilter getNavigationFilter() {
            return null;
        }

        public void install(final JFormattedTextField ftf) {
            if (textField != null) {
                removeFormattersActionMap();
                setNavigationFilter(null);
                setDocumentFilter(null);
            }
            textField = ftf;
            if (ftf == null) {
                return;
            }
            setActionMap(getActions());
            setNavigationFilter(getNavigationFilter());
            setDocumentFilter(getDocumentFilter());
            updateInternalValue();
            updateText();
        }

        private void updateText() {
            String result = "";
            try {
                result = valueToString(textField.getValue());
            } catch (ParseException e) {
                setEditValid(false);
            }
            textField.setText(result);
        }

        protected void invalidEdit() {
            textField.invalidEdit();
        }

        protected void setEditValid(final boolean isEditValid) {
            textField.setEditValid(isEditValid);
        }

        public abstract Object stringToValue(final String text) throws ParseException;

        public void uninstall() {
            removeFormattersActionMap();
            setNavigationFilter(null);
            setDocumentFilter(null);
        }

        public abstract String valueToString(final Object value) throws ParseException;

        private ActionMap createActionMap(final Action[] actions) {
            ActionMap result = new ActionMapFormattersResource();
            for (int i = 0; i < actions.length; i++) {
                Action action = actions[i];
                result.put(action.getValue(Action.NAME), action);
            }
            return result;
        }

        private void setActionMap(final Action[] actions) {
            if (actions == null) {
                return;
            }
            ActionMap formattersActionMap = createActionMap(actions);
            ActionMap actionMap = textField.getActionMap();
            ActionMap parent = actionMap.getParent();
            while (!(parent instanceof UIResource)) {
                actionMap = parent;
                parent = actionMap.getParent();
            }
            actionMap.setParent(formattersActionMap);
            formattersActionMap.setParent(parent);
        }

        private void removeFormattersActionMap() {
            ActionMap actionMap = textField.getActionMap();
            ActionMap parent = actionMap.getParent();
            while (!(parent instanceof ActionMapFormattersResource)) {
                actionMap = parent;
                parent = actionMap.getParent();
                if (parent instanceof UIResource) {
                    return;
                }
            }
            actionMap.setParent(parent.getParent());
            parent.setParent(null);
        }

        private void setDocumentFilter(final DocumentFilter filter) {
            Document doc = textField.getDocument();
            if (doc instanceof AbstractDocument) {
                ((AbstractDocument) doc).setDocumentFilter(filter);
            }
        }

        private void setNavigationFilter(final NavigationFilter filter) {
            textField.setNavigationFilter(filter);
        }

        private void updateInternalValue() {
            String text = textField.getText();
            boolean result = false;
            try {
                stringToValue(text);
                result = true;
            } catch (ParseException e) {
            }
            if (!result) {
                invalidEdit();
            }
            setEditValid(result);
        }
    }

    public abstract static class AbstractFormatterFactory {
        public abstract AbstractFormatter getFormatter(final JFormattedTextField tf);
    }

    static class CommitAction extends NotifyAction {
        private static final long serialVersionUID = 1L;

        public CommitAction(final String name) {
            super(name);
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            JTextComponent source = getTextComponent(e);
            if (source instanceof JFormattedTextField) {
                JFormattedTextField tf = (JFormattedTextField) source;
                if (tf.commitText()) {
                    tf.revertValue();
                }
            }
            super.actionPerformed(e);
        }

        @Override
        public boolean isEnabled() {
            final JTextComponent focused = getFocusedComponent();
            return focused instanceof JFormattedTextField;
        }
    }

    private static class CancelAction extends TextAction {
        private static final long serialVersionUID = 1L;

        public CancelAction(final String name) {
            super(name);
        }

        public void actionPerformed(final ActionEvent e) {
            JTextComponent source = getTextComponent(e);
            if (source instanceof JFormattedTextField) {
                JFormattedTextField tf = (JFormattedTextField) source;
                tf.revertValue();
            }
        }
    }

    public static final int COMMIT = 0;

    public static final int COMMIT_OR_REVERT = 1;

    public static final int PERSIST = 3;

    public static final int REVERT = 2;

    private static final String UI_CLASS_ID = "FormattedTextFieldUI";

    private static final String FORMATTER_FACTORY_PROPERTY_NAME = "formatterFactory";

    private static final String TEXT_FORMATTER_PROPERTY_NAME = "textFormatter";

    private static final String VALUE_PROPERTY_NAME = "value";

    private static final String EDIT_VALID_PROPERTY_NAME = "editValid";

    private static final String COMMIT_ACTION_NAME = "notify-field-accept";

    private static final String CANCEL_ACTION_NAME = "reset-field-edit";

    private static final TextAction COMMIT_ACTION = new CommitAction(COMMIT_ACTION_NAME);

    private static final TextAction CANCEL_ACTION = new CancelAction(CANCEL_ACTION_NAME);

    private static Action[] actions;

    private AbstractFormatterFactory factory;

    private Object value;

    private int focusLostBehaviour = COMMIT_OR_REVERT;

    private AbstractFormatter formatter;

    private boolean isEditValid;

    private DocumentListenerImpl docListener = new DocumentListenerImpl();

    private String lastSuccessfullyCommittedText;

    private class DocumentListenerImpl implements DocumentListener {
        public void changedUpdate(final DocumentEvent e) {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        changedUpdate(e);
                    }
                });
                return;
            }
            updateFormatterInternalValue();
        }

        public void insertUpdate(final DocumentEvent e) {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        insertUpdate(e);
                    }
                });
                return;
            }
            updateFormatterInternalValue();
        }

        public void removeUpdate(final DocumentEvent e) {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        removeUpdate(e);
                    }
                });
                return;
            }
            updateFormatterInternalValue();
        }
    }

    private void updateFormatterInternalValue() {
        if (formatter != null) {
            formatter.updateInternalValue();
        }
    }

    public JFormattedTextField() {
    }

    public JFormattedTextField(final AbstractFormatterFactory factory) {
        this.factory = factory;
    }

    public JFormattedTextField(final Format format) {
        setFormatter(createFormatter(format));
        setFormatterFactory(createFactory(formatter));
    }

    public JFormattedTextField(final AbstractFormatter formatter) {
        setFormatter(formatter);
        setFormatterFactory(createFactory(formatter));
    }

    public JFormattedTextField(final AbstractFormatterFactory factory, final Object value) {
        setFormatterFactory(factory);
        setValue(value);
    }

    public JFormattedTextField(final Object value) {
        setValue(value);
    }

    public void commitEdit() throws ParseException {
        String text = getText();
        if (formatter != null) {
            setValue(formatter.stringToValue(text));
        }
        lastSuccessfullyCommittedText = text;
    }

    @Override
    public Action[] getActions() {
        if (actions == null) {
            Action[] editorKitActions = ((TextUI) ui).getEditorKit(this).getActions();
            int length = editorKitActions.length;
            actions = new Action[length + 2];
            System.arraycopy(editorKitActions, 0, actions, 0, length);
            actions[length] = COMMIT_ACTION;
            actions[length + 1] = CANCEL_ACTION;
        }
        return actions.clone();
    }

    public int getFocusLostBehavior() {
        return focusLostBehaviour;
    }

    public AbstractFormatter getFormatter() {
        return formatter;
    }

    public AbstractFormatterFactory getFormatterFactory() {
        return factory;
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public Object getValue() {
        return value;
    }

    protected void invalidEdit() {
        Toolkit.getDefaultToolkit().beep();
    }

    public boolean isEditValid() {
        return isEditValid;
    }

    @Override
    protected void processFocusEvent(final FocusEvent event) {
        if (event.getID() == FocusEvent.FOCUS_LOST && !event.isTemporary()) {
            switch (getFocusLostBehavior()) {
                case REVERT:
                    revertValue();
                    break;
                case COMMIT:
                    if (commitText()) {
                        revertValue();
                    }
                    break;
                case COMMIT_OR_REVERT:
                    if (!commitText()) {
                        revertValue();
                    } else {
                        revertValue();
                    }
                    break;
                default:
                    break;
            }
        }
        if (!changeQueryFactoryPolicy()) {
            updateFormatter(false);
        }
        super.processFocusEvent(event);
    }

    private boolean changeQueryFactoryPolicy() {
        int focusLostBehaviour = getFocusLostBehavior();
        return (focusLostBehaviour == PERSIST || focusLostBehaviour == COMMIT_OR_REVERT)
                && lastSuccessfullyCommittedText != getText();
    }

    @Override
    public void setDocument(final Document doc) {
        Document oldValue = getDocument();
        if (oldValue != null) {
            oldValue.removeDocumentListener(docListener);
        }
        if (doc != null) {
            initDocumentListener();
            doc.addDocumentListener(docListener);
        }
        super.setDocument(doc);
        if (formatter != null) {
            formatter.install(this);
        }
    }

    public void setFocusLostBehavior(final int behavior) {
        if (behavior != COMMIT && behavior != COMMIT_OR_REVERT && behavior != PERSIST
                && behavior != REVERT) {
            throw new IllegalArgumentException("setFocusLostBehavior" + Messages.getString("swing.13") //$NON-NLS-1$ //$NON-NLS-2$
                    + "JFormattedTextField.COMMIT, " + "JFormattedTextField.COMMIT_OR_" //$NON-NLS-1$ //$NON-NLS-2$
                    + "REVERT, " + "JFormattedTextField.PERSIST " //$NON-NLS-1$ //$NON-NLS-2$
                    + "or JFormattedTextField.REVERT"); //$NON-NLS-1$
        }
        focusLostBehaviour = behavior;
    }

    /**
     * A PropertyChange event ("textFormatter") is propagated to each listener
     */
    protected void setFormatter(final AbstractFormatter formatter) {
        Object oldValue = this.formatter;
        if (this.formatter != null) {
            this.formatter.uninstall();
        }
        if (formatter != null) {
            formatter.install(this);
        }
        this.formatter = formatter;
        firePropertyChange(TEXT_FORMATTER_PROPERTY_NAME, oldValue, formatter);
    }

    /**
     * A PropertyChange event ("formattedFactory") is propagated to each
     * listener.
     */
    public void setFormatterFactory(final AbstractFormatterFactory factory) {
        Object oldValue = this.factory;
        this.factory = factory;
        updateFormatter(true);
        firePropertyChange(FORMATTER_FACTORY_PROPERTY_NAME, oldValue, factory);
    }

    /**
     * A PropertyChange event ("value") is propagated to each listener.
     */
    public void setValue(final Object value) {
        Object oldValue = this.value;
        this.value = value;
        updateFactory();
        updateFormatter(true);
        revertValue();
        firePropertyChange(VALUE_PROPERTY_NAME, oldValue, value);
    }

    private AbstractFormatterFactory createFactory(final Object value) {
        DefaultFormatterFactory factory = new DefaultFormatterFactory();
        if (value instanceof Number) {
            factory.setDefaultFormatter(new NumberFormatter());
        } else if (value instanceof Date) {
            factory.setDefaultFormatter(new DateFormatter());
        } else {
            factory.setDefaultFormatter(new DefaultFormatter());
        }
        return factory;
    }

    private AbstractFormatterFactory createFactory(final AbstractFormatter formatter) {
        DefaultFormatterFactory factory = new DefaultFormatterFactory();
        factory.setDefaultFormatter(formatter);
        return factory;
    }

    private AbstractFormatter createFormatter(final Format format) {
        if (format instanceof DateFormat) {
            return new DateFormatter((DateFormat) format);
        } else if (format instanceof NumberFormat) {
            return new NumberFormatter((NumberFormat) format);
        } else {
            return new InternationalFormatter(format);
        }
    }

    private boolean revertValue() {
        if (formatter == null) {
            return false;
        }
        String newText = null;
        try {
            newText = formatter.valueToString(value);
        } catch (ParseException e) {
        }
        if (newText == null) {
            return false;
        }
        if (!newText.equals(getText())) {
            setText(newText);
        }
        return true;
    }

    private boolean commitText() {
        boolean result = false;
        try {
            commitEdit();
            result = true;
        } catch (ParseException e) {
        }
        return result;
    }

    private void setEditValid(final boolean isEditValid) {
        boolean oldValue = this.isEditValid;
        this.isEditValid = isEditValid;
        firePropertyChange(EDIT_VALID_PROPERTY_NAME, oldValue, isEditValid);
    }

    private void updateFactory() {
        if (factory == null) {
            factory = createFactory(getValue());
        }
    }

    private void updateFormatter(final boolean conditionalUpdate) {
        if (factory == null) {
            return;
        }
        AbstractFormatter formatter = factory.getFormatter(this);
        if (!conditionalUpdate || this.formatter != formatter) {
            setFormatter(factory.getFormatter(this));
        }
    }

    private void initDocumentListener() {
        if (docListener == null) {
            docListener = new DocumentListenerImpl();
        }
    }
}
