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
 * @author Anton Avtamonov
 */
package org.apache.harmony.x.swing;

import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;

public class ExtendedListCellRenderer extends DefaultListCellRenderer {
    private static final String SUB_ELEMENT_INDENT = "    ";

    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {

        if (value instanceof ExtendedListElement) {
            ExtendedListElement extendedElement = (ExtendedListElement)value;
            JComponent result = (JComponent)super.getListCellRendererComponent(list, value, index,
                                                      isSelected && extendedElement.isChoosable(),
                                                      cellHasFocus && extendedElement.isChoosable());
            tuneRenderer(result, extendedElement);
            return result;
        } else {
            return super.getListCellRendererComponent(list, value, index,
                                                        isSelected, cellHasFocus);
        }
    }

    protected void tuneRenderer(final JComponent renderingComponent, final ExtendedListElement element) {
        Font font = element.getFont();
        if (font != null) {
            renderingComponent.setFont(font);
        }
        if (element.getIndentationLevel() > 0) {
            JLabel labelRenderer = (JLabel)renderingComponent;
            labelRenderer.setText(createIndentation(element.getIndentationLevel()) + labelRenderer.getText());
        }
        if (!element.isEnabled()) {
            renderingComponent.setEnabled(false);
        }
        renderingComponent.setToolTipText(element.getToolTipText());
    }


    private static String createIndentation(final int indentationLevel) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < indentationLevel; i++) {
            result.append(SUB_ELEMENT_INDENT);
        }

        return result.toString();
    }
}
