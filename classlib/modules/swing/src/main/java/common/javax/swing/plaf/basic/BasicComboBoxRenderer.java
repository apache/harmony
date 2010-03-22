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

package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;

import org.apache.harmony.x.swing.Utilities;


public class BasicComboBoxRenderer extends JLabel implements ListCellRenderer {

    public static class UIResource extends BasicComboBoxRenderer implements javax.swing.plaf.UIResource {
    }

    protected static Border noFocusBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);

    public BasicComboBoxRenderer() {
        setBorder(noFocusBorder);
        setHorizontalAlignment(JLabel.LEADING);
    }

    public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
        setText(value != null ? value.toString() : null);
        if (isSelected) {
            setForeground(list.getSelectionForeground());
            setBackground(list.getSelectionBackground());
        } else {
            setForeground(list.getForeground());
            setBackground(list.getBackground());
        }

        setOpaque(true);
        setFont(list.getFont());
        setBorder(noFocusBorder);
        setComponentOrientation(list.getComponentOrientation());

        return this;
    }

    public Dimension getPreferredSize() {
        Dimension result;
        if (Utilities.isEmptyString(getText())) {
            setText(" ");
            result = super.getPreferredSize();
            setText("");
        } else {
            result = super.getPreferredSize();
        }

        return result;
    }
}
