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

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.text.Document;

import org.apache.harmony.awt.ComponentInternals;
import org.apache.harmony.x.swing.StringConstants;


public class JPasswordField extends JTextField {
    protected class AccessibleJPasswordField extends AccessibleJTextField {
        public AccessibleRole getAccessibleRole() {
             return AccessibleRole.PASSWORD_TEXT;
        }
    }

    final class PasswordTextKit extends TextFieldKitImpl {
        public boolean echoCharIsSet() {
            return JPasswordField.this.echoCharIsSet();
        }

        public char getEchoChar() {
            return JPasswordField.this.getEchoChar();
        }
    }

    private static final String uiClassID = "PasswordFieldUI";
    private static final char zeroEcho = '\0';
    private char echoChar = '*';

    public JPasswordField() {
        this(null, null, 0);
    }

    public JPasswordField(final String text, final int columns) {
        this(null, text, columns);
    }

    public JPasswordField(final String text) {
        this(null, text, 0);
    }

    public JPasswordField(final int columns) {
        this(null, null, columns);
    }

    public JPasswordField(final Document document,
                          final String text,
                          final int columns) {
        super(document, text, columns);
        installTextKit();
    }

    void installTextKit() {
        ComponentInternals.getComponentInternals()
            .setTextFieldKit(this, new PasswordTextKit());
    }

    public void copy() {
        UIManager.getLookAndFeel().provideErrorFeedback(this);
    }

    public void cut() {
        UIManager.getLookAndFeel().provideErrorFeedback(this);
    }

    public boolean echoCharIsSet() {
        return echoChar != zeroEcho;
    }

    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJPasswordField();
        }
        return accessibleContext;
    }

    public char getEchoChar() {
        return echoChar;
    }

    public String getUIClassID() {
        return uiClassID;
    }

    public char[] getPassword() {
        return getText().toCharArray();
    }

    public void setEchoChar(final char echoChar) {
        char oldValue = this.echoChar;
        this.echoChar = echoChar;
        firePropertyChange(StringConstants.PASSWORD_FIELD_ECHO_CHAR_PROPERTY,
                           oldValue, echoChar);
    }
}

