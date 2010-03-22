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
package javax.swing.plaf.basic;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.PasswordView;
import javax.swing.text.View;

import org.apache.harmony.x.swing.StringConstants;


public class BasicPasswordFieldUI extends BasicTextFieldUI {
    private static final String propertyPrefix = "PasswordField";
    public BasicPasswordFieldUI() {
    }

    public View create(final Element element) {
        if (echoCharIsSet() || !getI18nProperty()) {
            return new PasswordView(element);
        } else {
            return (View)AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    try {
                        Class cls = Class.forName(FIELD_VIEW_I18N_CLASS);
                        Constructor constructor =
                            cls.getConstructor(new Class[] {Element.class});
                        constructor.setAccessible(true);
                        return constructor.newInstance(new Object[] {element});
                    } catch (Exception e) {
                        return null;
                    }
                }
            });
        }
    }

    public static ComponentUI createUI(final JComponent c) {
        return new BasicPasswordFieldUI();
    }

    protected String getPropertyPrefix() {
        return propertyPrefix;
    }

    void propertyChangeImpl(final PropertyChangeEvent e) {
        if (!StringConstants.PASSWORD_FIELD_ECHO_CHAR_PROPERTY.equals(e
            .getPropertyName())) {
            return;
        }
        if (getI18nProperty() && (isZeroChar(e.getNewValue())
                || isZeroChar(e.getOldValue()))) {
            modelChanged();
        } else {
            getComponent().repaint();
        }
    }

    private final boolean isZeroChar(final Object value) {
        return (value instanceof Character)
           && ((Character)value).charValue() == '\0';
    }

    private final boolean echoCharIsSet() {
        JTextComponent component = getComponent();
        return (component instanceof JPasswordField)
            && ((JPasswordField)component).echoCharIsSet();
    }
}