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
 * @author Roman I. Chernyatchik
 */
package javax.swing.plaf.basic;

import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.util.EventObject;

import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.ViewFactory;

import org.apache.harmony.x.swing.StringConstants;

public class BasicTextPaneUI extends BasicEditorPaneUI implements ViewFactory {
    private static final String propertyPrefix = "TextPane";

    public static ComponentUI createUI(final JComponent c) {
        return new BasicTextPaneUI();
    }

    protected void propertyChange(final PropertyChangeEvent e) {
        super.propertyChange(e);

        final String propName = e.getPropertyName();

        if (e.getNewValue() == null) {
            return;
        }

        if (StringConstants.FONT_PROPERTY_CHANGED.equals(propName)) {
            updateFontAttributes(getDefaultStyle(e), (Font) e.getNewValue());
        } else if (StringConstants.FOREGROUND_PROPERTY_CHANGED.equals(propName)) {
            getDefaultStyle(e).addAttribute(StyleConstants.Foreground,
                                            e.getNewValue());
        } else if (StringConstants.TEXT_COMPONENT_DOCUMENT_PROPERTY.equals(propName)) {
            setDefaultStyle(e);
        }
    }

    private Style getDefaultStyle(final EventObject e) {
        return ((JTextPane) e.getSource()).getStyledDocument()
               .getStyle(StyleContext.DEFAULT_STYLE);
    }

    private void setDefaultStyle(final PropertyChangeEvent e) {
        final JTextPane pane = (JTextPane)e.getSource();
        if (pane.getStyledDocument() == null) {
            return;
        }

        final Font f = pane.getFont();
        if (f == null) {
            return;
        }

        final Style style = getDefaultStyle(e);

        updateFontAttributes(style, f);
        style.addAttribute(StyleConstants.Foreground, pane.getForeground());
   }

    protected  String getPropertyPrefix() {
        return propertyPrefix;
    }


    private void updateFontAttributes(final Style style, final Font f) {
        style.addAttribute(StyleConstants.Italic, Boolean.valueOf(f.isItalic()));
        style.addAttribute(StyleConstants.Bold, Boolean.valueOf(f.isBold()));
        style.addAttribute(StyleConstants.FontFamily, f.getName());
        style.addAttribute(StyleConstants.FontSize, new Integer(f.getSize()));
    }
}
