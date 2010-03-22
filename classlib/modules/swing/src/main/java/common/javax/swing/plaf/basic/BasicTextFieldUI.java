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

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.FieldView;
import javax.swing.text.View;

import org.apache.harmony.x.swing.StringConstants;


public class BasicTextFieldUI extends BasicTextUI {
    static final String FIELD_VIEW_I18N_CLASS =
        "javax.swing.text.FieldViewI18N";
    static String propertyPrefix = "TextField";

    public BasicTextFieldUI() {
        super();
    }

    public View create(final Element elem) {
        if (elem == null) {
            return null;
        }
        Document doc = elem.getDocument();
        Boolean i18n = (Boolean)doc.getProperty(StringConstants.BIDI_PROPERTY);
        if (i18n.booleanValue()) {
            return (View)AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    try {
                        Class cls = Class.forName(FIELD_VIEW_I18N_CLASS);
                        Constructor constructor =
                            cls.getConstructor(new Class[] {Element.class});
                        constructor.setAccessible(true);
                        return constructor.newInstance(new Object[] {elem});
                    } catch (Exception e) {
                        return null;
                    }
                }
            });
        }
        return new FieldView(elem);
    }

    public static ComponentUI createUI(final JComponent c) {
        return new BasicTextFieldUI();
    }

    protected  String getPropertyPrefix() {
        return propertyPrefix;
    }

    public void installUI(final JComponent c) {
        super.installUI(c);
    }

    private void updateBackgroundColor() {
        String property = getComponent().isEditable()
            ? ".background" : ".inactiveBackground";
        Color color = UIManager.getColor(addPrefix(property));
        getComponent().setBackground(color);
    }

    protected  void propertyChange(final PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if ("horizontalAlignment".equals(propertyName)) {
            getComponent().repaint();
        } else  if (org.apache.harmony.x.swing.StringConstants
                    .EDITABLE_PROPERTY_CHANGED.equals(evt.getPropertyName())) {
            updateBackgroundColor();
        }
        super.propertyChange(evt);
    }

    final void updateFocusTraversalKeys() {

    }
}