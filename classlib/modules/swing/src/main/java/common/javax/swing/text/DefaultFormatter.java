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
package javax.swing.text;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.ParseException;
import javax.swing.JFormattedTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.DocumentFilter.FilterBypass;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>DefaultFormatter</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class DefaultFormatter extends JFormattedTextField.AbstractFormatter
    implements Cloneable, Serializable {

    private static final long serialVersionUID = 4759164676455607130L;

    private boolean commitsOnValidEdit;
    private boolean allowsInvalid = true;
    private boolean overwriteMode = true;
    private Class valueClass;
    private DocumentFilter documentFilter;

    private class DocumentFilterImpl extends DocumentFilter {

        @Override
        public void insertString(final FilterBypass filterBypass,
                                 final int offset,
                                 final String string,
                                 final AttributeSet attrs)
            throws BadLocationException {
            if (overwriteMode) {
                int length = getMaxLengthToRemove(filterBypass, offset,
                                                  string.length());
                replaceImpl(filterBypass, offset, length, string, attrs);
            } else {
                insertStringImpl(filterBypass, offset, string, attrs);
            }
        }

        @Override
        public void remove(final FilterBypass filterBypass,
                           final int offset, final int length)
                throws BadLocationException {
            removeImpl(filterBypass, offset, length);
        }

        @Override
        public void replace(final FilterBypass filterBypass,
                            final int offset, final int length,
                            final String text,
                            final AttributeSet attrs)
            throws BadLocationException {
            if (overwriteMode) {
                int strLength = getMaxLengthToRemove(filterBypass, offset,
                        Math.max(length, text != null ? text.length() : 0));
                replaceImpl(filterBypass, offset, strLength, text, attrs);
            } else {
                replaceImpl(filterBypass, offset, length, text, attrs);
            }
        }
    }


    void insertStringImpl(final FilterBypass filterBypass,
                             final int offset,
                             final String string,
                             final AttributeSet attrs)
        throws BadLocationException {
        if (needEdit(0, offset, 0, string)) {
            filterBypass.insertString(offset, string, attrs);
        }
    }

    void removeImpl(final FilterBypass filterBypass,
                       final int offset, final int length)
            throws BadLocationException {
        if (needEdit(1, offset, length, null)) {
            filterBypass.remove(offset, length);
        }
    }

    void replaceImpl(final FilterBypass filterBypass,
                        final int offset, final int length,
                        final String text,
                        final AttributeSet attrs) throws BadLocationException {
        if (needEdit(2, offset, length, text)) {
            filterBypass.replace(offset, length, text, attrs);
        }
    }

    final int getMaxLengthToRemove(final FilterBypass filterBypass,
                                     final int offset,
                                     final int length) {
        return Math.min(length,
                        filterBypass.getDocument().getLength() - offset);

    }

    private String removeString(final String string,
                                final int offset,
                                final int length) {
         int stringLength = string.length();
         int start = Math.min(stringLength, offset);
         int minLength = Math.min(stringLength - start, length);
         return string.substring(0, start)
            + string.substring(start + minLength,
                               stringLength);
    }

    private String insertString(final String string,
                                final int offset,
                                final String insertedString) {
        int stringLength = string.length();
        return offset <= stringLength
            ? string.substring(0, offset) + insertedString
                    + string.substring(offset, stringLength)
            : string;
    }

    private String replaceString(final String string,
                                 final int offset,
                                 final int length,
                                 final String replacement) {
        int stringLength = string.length();
        int start = Math.min(stringLength, offset);
        int minLength = Math.min(stringLength - start, offset);
        return string.substring(0, start) + replacement
            +  string.substring(start + minLength, stringLength);
    }

    boolean needEdit(final int operationId,
                                 final int offset,
                                 final int length,
                                 final String text) {
        String supposedText = getFormattedTextField().getText();
        if (allowsInvalid) {
            return true;
        }
        switch (operationId) {
        case 0:
            supposedText = insertString(supposedText, offset, text);
            break;
        case 1:
            supposedText = removeString(supposedText, offset, length);
            break;
        case 2:
            supposedText = replaceString(supposedText, offset, length, text);
            break;
        default:
            break;
        }
        Object value = null;
        boolean result = false;
        try {
            value = stringToValue(supposedText);
            result = value != null;
            valueToString(value);
            result = true;
        } catch (Exception e) {
        }
        return allowsInvalid || result;
    }

    public DefaultFormatter() {

    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public boolean getAllowsInvalid() {
        return allowsInvalid;
    }

    public boolean getCommitsOnValidEdit() {
        return commitsOnValidEdit;
    }

    @Override
    protected DocumentFilter getDocumentFilter() {
        if (documentFilter == null) {
            documentFilter = new DocumentFilterImpl();
        }
        return documentFilter;
    }

    public boolean getOverwriteMode() {
        return overwriteMode;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }

    public void setAllowsInvalid(final boolean allowsInvalid) {
        this.allowsInvalid = allowsInvalid;
    }

    public void setCommitsOnValidEdit(final boolean commitsOnValidEdit) {
        this.commitsOnValidEdit = commitsOnValidEdit;
    }

    @Override
    protected void setEditValid(final boolean isEditValid) {
        super.setEditValid(isEditValid);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                checkCommitCondition();
                formatValue();
        }
        });
    }

    public void setOverwriteMode(final boolean overwriteMode) {
        this.overwriteMode = overwriteMode;
    }

    public void setValueClass(final Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    private Object stringToValue(final String string, final Class valueClass) {
        return AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                Constructor constructor = null;
                try {
                    constructor = valueClass.getConstructor(new Class[]
                                                   {String.class});
                } catch (NoSuchMethodException e) {
                    return string;
                }
                Object result = null;
                try {
                     constructor.setAccessible(true);
                     result = constructor.newInstance(new Object[] {string});
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                } catch (InstantiationException e) {
                }
                return result;
             }
         });
    }

    @Override
    public Object stringToValue(final String string) throws ParseException {
        final Class valueClass = (this.valueClass != null) ? this.valueClass
                : getTextFieldValueClass();
        if (valueClass == null || string == null) {
            return string;
        }
       Object result = stringToValue(string, valueClass);
       if (result == null) {
           throw new ParseException(Messages.getString("swing.86"), 0); //$NON-NLS-1$
       }
       return result;
    }

    @Override
    public String valueToString(final Object value) throws ParseException {
        return value != null ? value.toString() : "";
    }

    private Class getTextFieldValueClass() {
        JFormattedTextField textField = getFormattedTextField();
        if (textField == null) {
            return null;
        }
        Object value = getFormattedTextField().getValue();
        return value != null ? value.getClass() : null;
    }

    private void checkCommitCondition() {
        JFormattedTextField textField = getFormattedTextField();
        if (textField.isEditValid() && commitsOnValidEdit) {
            try {
                 textField.setValue(stringToValue(textField.getText()));
            } catch (ParseException e) {
            }
        }
    }

    private void formatValue() {
        if (allowsInvalid) {
            return;
        }
        JFormattedTextField textField = getFormattedTextField();
        String text = textField.getText();
        String formattedText = getFormattedText(text);
        if (!formattedText.equals(text)) {
            int caret = textField.getCaretPosition();
            textField.setText(formattedText);
            textField.setCaretPosition(Math.min(formattedText.length(), caret));
        }
    }

    String getFormattedText(final String text) {
        Object value = null;
        try {
            value = stringToValue(text);
        } catch (ParseException e) {
        }
        return (value != null) ? text : "";
    }
}


