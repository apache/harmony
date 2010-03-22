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
package javax.swing.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;

public class DefaultTableCellRenderer extends JLabel implements TableCellRenderer {
    public static class UIResource extends DefaultTableCellRenderer implements javax.swing.plaf.UIResource {
    }

    protected static Border noFocusBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);
    private Border focusBorder;
    private Color focusCellBackground;
    private Color focusCellForeground;

    public DefaultTableCellRenderer() {
        updateUI();
        setName(null);
    }

    public void setForeground(final Color c) {
        super.setForeground(c);
    }

    public void setBackground(final Color c) {
        super.setBackground(c);
    }

    public void updateUI() {        
        super.updateUI();
        setBackground(null);
        setForeground(null);
        focusBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
        focusCellBackground = UIManager.getColor("Table.focusCellBackground");
        focusCellForeground = UIManager.getColor("Table.focusCellForeground");
    }

    public Component getTableCellRendererComponent(final JTable table,
                                                   final Object value,
                                                   final boolean isSelected,
                                                   final boolean hasFocus,
                                                   final int row,
                                                   final int column) {

        setValue(value);
        setFont(table.getFont());
        if (hasFocus) {
            setBorder(focusBorder);
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else if (table.isCellEditable(row, column)) {
                setBackground(focusCellBackground);
                setForeground(focusCellForeground);
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
        } else {
            setBorder(noFocusBorder);
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
        }

        return this;
    }

    public boolean isOpaque() {
        return true;
    }

    public void invalidate() {
    }

    public void validate() {
    }

    public void revalidate() {
    }

    public void repaint(final long tm, final int x, final int y, final int width, final int height) {
    }

    public void repaint(final Rectangle r) {
    }

    public void repaint() {
    }

    public void firePropertyChange(final String propertyName, final boolean oldValue, final boolean newValue) {
    }


    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
    }

    protected void setValue(final Object value) {
        setText(value != null ? value.toString() : "");
    }
}
