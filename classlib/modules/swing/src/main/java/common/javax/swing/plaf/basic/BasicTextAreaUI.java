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
package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainView;
import javax.swing.text.View;
import javax.swing.text.WrappedPlainView;

import org.apache.harmony.x.swing.StringConstants;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class BasicTextAreaUI extends BasicTextUI {
    private static final String PLAIN_VIEW_I18N_CLASS =
        "javax.swing.text.PlainViewI18N";
    private static final String propertyPrefix = "TextArea";

    public BasicTextAreaUI() {
        super();
    }

    public static ComponentUI createUI(final JComponent c) {
        return new BasicTextAreaUI();
    }

    @Override
    public View create(final Element element) {
        Document doc = element.getDocument();
        Boolean i18n = (Boolean)doc.getProperty(StringConstants.BIDI_PROPERTY);
        if (i18n.booleanValue()) {
            return AccessController.doPrivileged(new PrivilegedAction<View>() {
                public View run() {
                    try {
                        Class cls = Class.forName(PLAIN_VIEW_I18N_CLASS);
                        Constructor constructor =
                            cls.getConstructor(new Class[] {Element.class});
                        constructor.setAccessible(true);
                        return (View)constructor.newInstance(new Object[] {element});
                    } catch (Exception e) {
                        return null;
                    }
                }
            });
        }

        JTextComponent comp = getComponent();
        boolean lineWrap = false;
        boolean wordWrap = false;
        if (comp instanceof JTextArea) {
            JTextArea c = (JTextArea)getComponent();
            lineWrap = c.getLineWrap();
            wordWrap = c.getWrapStyleWord();
        }
        if (lineWrap) {
            return new WrappedPlainView(element, wordWrap);
        }

        return new PlainView(element);

    }

    @Override
    public Dimension getMinimumSize(final JComponent c) {
        if (!(c instanceof JTextComponent)) {
            throw new IllegalArgumentException(Messages.getString("swing.77")); //$NON-NLS-1$
        }
        Dimension dim = super.getMinimumSize(c);
        Caret caret = ((JTextComponent)c).getCaret();
        if (caret != null && caret instanceof DefaultCaret) {
            dim.width += ((DefaultCaret)caret).width;
        }
        return dim;
    }

    @Override
    public Dimension getPreferredSize(final JComponent c) {
        if (!(c instanceof JTextComponent)) {
            throw new IllegalArgumentException(Messages.getString("swing.77")); //$NON-NLS-1$
        }
        Dimension dim = super.getPreferredSize(c);

        Caret caret = ((JTextComponent)c).getCaret();
        if (caret != null && caret instanceof DefaultCaret) {
            dim.width += ((DefaultCaret)caret).width;
        }
        return dim;
    }

    @Override
    protected String getPropertyPrefix() {
        return propertyPrefix;
    }


    @Override
    protected void propertyChange(final PropertyChangeEvent e) {
        String name = e.getPropertyName();
        if (StringConstants.TEXT_COMPONENT_LINE_WRAP_PROPERTY.equals(name)
                || StringConstants
                .TEXT_COMPONENT_WRAP_STYLE_WORD_PROPERTY.equals(name)) {
            modelChanged();
        }
    }
}
