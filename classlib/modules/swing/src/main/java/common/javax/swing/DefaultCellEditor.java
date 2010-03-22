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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.EventObject;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.TreeCellEditor;
import org.apache.harmony.x.swing.StringConstants;

/**
 * <p>
 * <i>DefaultCellEditor</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class DefaultCellEditor extends AbstractCellEditor implements TableCellEditor,
        TreeCellEditor {
    private static final long serialVersionUID = 3564035141373880027L;

    protected class EditorDelegate implements ActionListener, ItemListener, Serializable {
        private static final long serialVersionUID = -1953114537286696317L;

        protected Object value;

        public Object getCellEditorValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public boolean isCellEditable(EventObject event) {
            if (event instanceof MouseEvent) {
                return ((MouseEvent) event).getClickCount() >= clickCountToStart;
            }
            return true;
        }

        public boolean shouldSelectCell(EventObject event) {
            return true;
        }

        public boolean startCellEditing(EventObject event) {
            return true;
        }

        public boolean stopCellEditing() {
            fireEditingStopped();
            return true;
        }

        public void cancelCellEditing() {
            fireEditingCanceled();
        }

        public void actionPerformed(ActionEvent e) {
            DefaultCellEditor.this.stopCellEditing();
        }

        public void itemStateChanged(ItemEvent e) {
            DefaultCellEditor.this.stopCellEditing();
        }
    }

    protected JComponent editorComponent;

    protected EditorDelegate delegate;

    protected int clickCountToStart;

    public DefaultCellEditor(final JTextField textField) {
        editorComponent = textField;
        delegate = new EditorDelegate() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getCellEditorValue() {
                return textField.getText();
            }

            @Override
            public void setValue(Object value) {
                textField.setText(value != null ? value.toString() : null);
            }
        };
        textField.addActionListener(delegate);
        setClickCountToStart(2);
    }

    public DefaultCellEditor(final JCheckBox checkBox) {
        editorComponent = checkBox;
        delegate = new EditorDelegate() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getCellEditorValue() {
                return Boolean.valueOf(checkBox.isSelected());
            }

            @Override
            public void setValue(Object value) {
                checkBox.setSelected(value != null ? Boolean.parseBoolean(value.toString())
                        : false);
            }
        };
        checkBox.setFocusable(false);
        checkBox.addActionListener(delegate);
        setClickCountToStart(1);
    }

    public DefaultCellEditor(final JComboBox comboBox) {
        editorComponent = comboBox;
        delegate = new EditorDelegate() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getCellEditorValue() {
                return comboBox.getSelectedItem();
            }

            @Override
            public void setValue(Object value) {
                comboBox.setSelectedItem(value);
            }
        };
        comboBox.addActionListener(delegate);
        setClickCountToStart(1);
        comboBox.putClientProperty(StringConstants.IS_TABLE_EDITOR, Boolean.TRUE);
    }

    public Component getComponent() {
        return editorComponent;
    }

    public void setClickCountToStart(int count) {
        clickCountToStart = count;
    }

    public int getClickCountToStart() {
        return clickCountToStart;
    }

    public Object getCellEditorValue() {
        return delegate.getCellEditorValue();
    }

    @Override
    public boolean isCellEditable(EventObject event) {
        return delegate.isCellEditable(event);
    }

    @Override
    public boolean shouldSelectCell(EventObject event) {
        return delegate.shouldSelectCell(event);
    }

    @Override
    public boolean stopCellEditing() {
        return delegate.stopCellEditing();
    }

    @Override
    public void cancelCellEditing() {
        delegate.cancelCellEditing();
    }

    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected,
            boolean expanded, boolean leaf, int row) {
        delegate.setValue(value);
        return getComponent();
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        delegate.setValue(value);
        return getComponent();
    }
}
