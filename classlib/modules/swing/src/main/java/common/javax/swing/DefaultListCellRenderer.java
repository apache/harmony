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

import java.awt.Component;
import java.io.Serializable;
import javax.swing.border.Border;

/**
 * <p>
 * <i>DefaultListCellRenderer</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class DefaultListCellRenderer extends JLabel implements ListCellRenderer, Serializable {
    private static final long serialVersionUID = -4095659446023979489L;

    public static class UIResource extends DefaultListCellRenderer implements
            javax.swing.plaf.UIResource {
        private static final long serialVersionUID = 5748603813962368116L;
    }

    protected static Border noFocusBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);

    public DefaultListCellRenderer() {
        setBorder(noFocusBorder);
        setHorizontalAlignment(SwingConstants.LEADING);
        setOpaque(true);
    }

    public Component getListCellRendererComponent(JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
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
        setEnabled(list.isEnabled());
        setBorder(cellHasFocus ? UIManager.getBorder("List.focusCellHighlightBorder")
                : noFocusBorder);
        setComponentOrientation(list.getComponentOrientation());
        return this;
    }

    @Override
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }

    @Override
    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
    }

    @Override
    public void firePropertyChange(String propertyName, char oldValue, char newValue) {
    }

    @Override
    public void firePropertyChange(String propertyName, double oldValue, double newValue) {
    }

    @Override
    public void firePropertyChange(String propertyName, float oldValue, float newValue) {
    }

    @Override
    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
    }

    @Override
    public void firePropertyChange(String propertyName, long oldValue, long newValue) {
    }

    @Override
    public void firePropertyChange(String propertyName, short oldValue, short newValue) {
    }

    @Override
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    }
}
