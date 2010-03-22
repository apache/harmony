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

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.SwingPropertyChangeSupport;

public class TableColumn implements Serializable {
    public static final String COLUMN_WIDTH_PROPERTY = "columWidth";
    public static final String HEADER_VALUE_PROPERTY = "headerValue";
    public static final String HEADER_RENDERER_PROPERTY = "headerRenderer";
    public static final String CELL_RENDERER_PROPERTY = "cellRenderer";

    static final String WIDTH_PROPERTY = "width";
    static final String PREFERRED_WIDTH_PROPERTY = "preferredWidth";
    private static final String MODEL_INDEX_PROPERTY = "modelIndex";
    private static final String IDENTIFIER_PROPERTY = "identifier";
    private static final String CELL_EDITOR_PROPERTY = "cellEditor";
    private static final String MIN_WIDTH_PROPERTY = "minWidth";
    private static final String MAX_WIDTH_PROPERTY = "maxWidth";
    private static final String IS_RESIZABLE_PROPERTY = "isResizable";

    protected int modelIndex;
    protected Object identifier;
    protected int width;
    protected int minWidth = 15;
    protected int maxWidth = Integer.MAX_VALUE;
    protected TableCellRenderer headerRenderer;
    protected TableCellRenderer cellRenderer;
    protected TableCellEditor cellEditor;
    protected Object headerValue;
    protected boolean isResizable = true;
    /**
     * @deprecated
     */
    protected transient int resizedPostingDisableCount;

    private int preferredWidth;
    private SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this);


    public TableColumn() {
        this(0, 75, null, null);
    }

    public TableColumn(final int modelIndex) {
        this(modelIndex, 75, null, null);
    }

    public TableColumn(final int modelIndex, final int width) {
        this(modelIndex, width, null, null);
    }

    public TableColumn(final int modelIndex, final int width, final TableCellRenderer cellRenderer, final TableCellEditor cellEditor) {
        this.modelIndex = modelIndex;
        this.width = width;
        this.preferredWidth = width;
        this.cellRenderer = cellRenderer;
        this.cellEditor = cellEditor;
    }

    public void setModelIndex(final int modelIndex) {
        int oldValue = this.modelIndex;
        this.modelIndex = modelIndex;
        propertyChangeSupport.firePropertyChange(MODEL_INDEX_PROPERTY, oldValue, modelIndex);
    }

    public int getModelIndex() {
        return modelIndex;
    }

    public void setIdentifier(final Object identifier) {
        Object oldValue = this.identifier;
        this.identifier = identifier;
        propertyChangeSupport.firePropertyChange(IDENTIFIER_PROPERTY, oldValue, identifier);
    }

    public Object getIdentifier() {
        return identifier != null ? identifier : getHeaderValue();
    }

    public void setHeaderValue(final Object headerValue) {
        Object oldValue = this.headerValue;
        this.headerValue = headerValue;
        propertyChangeSupport.firePropertyChange(HEADER_VALUE_PROPERTY, oldValue, headerValue);
    }

    public Object getHeaderValue() {
        return headerValue;
    }

    public void setHeaderRenderer(final TableCellRenderer headerRenderer) {
        Object oldValue = this.headerRenderer;
        this.headerRenderer = headerRenderer;
        propertyChangeSupport.firePropertyChange(HEADER_RENDERER_PROPERTY, oldValue, headerRenderer);
    }

    public TableCellRenderer getHeaderRenderer() {
        return headerRenderer;
    }

    public void setCellRenderer(final TableCellRenderer cellRenderer) {
        Object oldValue = this.cellRenderer;
        this.cellRenderer = cellRenderer;
        propertyChangeSupport.firePropertyChange(CELL_RENDERER_PROPERTY, oldValue, cellRenderer);
    }

    public TableCellRenderer getCellRenderer() {
        return cellRenderer;
    }

    public void setCellEditor(final TableCellEditor cellEditor) {
        Object oldValue = this.cellEditor;
        this.cellEditor = cellEditor;
        propertyChangeSupport.firePropertyChange(CELL_EDITOR_PROPERTY, oldValue, cellEditor);
    }

    public TableCellEditor getCellEditor() {
        return cellEditor;
    }

    public void setWidth(final int width) {
        int oldValue = this.width;
        this.width = width;
        adjustWidths();

        propertyChangeSupport.firePropertyChange(WIDTH_PROPERTY, oldValue, this.width);
    }

    public int getWidth() {
        return width;
    }

    public void setPreferredWidth(final int preferredWidth) {
        int oldValue = this.preferredWidth;
        this.preferredWidth = preferredWidth;
        adjustWidths();

        propertyChangeSupport.firePropertyChange(PREFERRED_WIDTH_PROPERTY, oldValue, this.preferredWidth);
    }

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setMinWidth(final int minWidth) {
        int oldValue = this.minWidth;
        this.minWidth = minWidth;
        adjustWidths();

        propertyChangeSupport.firePropertyChange(MIN_WIDTH_PROPERTY, oldValue, this.minWidth);
    }

    public int getMinWidth() {
        return minWidth;
    }

    public void setMaxWidth(final int maxWidth) {
        int oldValue = this.maxWidth;
        this.maxWidth = maxWidth;
        adjustWidths();

        propertyChangeSupport.firePropertyChange(MAX_WIDTH_PROPERTY, oldValue, this.maxWidth);
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setResizable(final boolean isResizable) {
        boolean oldValue = this.isResizable;
        this.isResizable = isResizable;

        propertyChangeSupport.firePropertyChange(IS_RESIZABLE_PROPERTY, oldValue, isResizable);
    }

    public boolean getResizable() {
        return isResizable;
    }

    public void sizeWidthToFit() {
        if (headerRenderer != null) {
            Component renderingComponent = headerRenderer.getTableCellRendererComponent(null, getHeaderValue(), false, false, 0, 0);
            maxWidth = renderingComponent.getMaximumSize().width;
            minWidth = renderingComponent.getMinimumSize().width;
            preferredWidth = renderingComponent.getPreferredSize().width;
            width = preferredWidth;
        }
    }

    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return propertyChangeSupport.getPropertyChangeListeners();
    }

    /**
     * @deprecated
     */
    public void disableResizedPosting() {
        resizedPostingDisableCount++;
    }

    /**
     * @deprecated
     */
    public void enableResizedPosting() {
        resizedPostingDisableCount--;
    }

    protected TableCellRenderer createDefaultHeaderRenderer() {
        return new DefaultTableCellRenderer() {
            private JTable defaultTable;
            private Border cellBorder = UIManager.getBorder("TableHeader.cellBorder");

            public Component getTableCellRendererComponent(final JTable table,
                                                           final Object value,
                                                           final boolean isSelected,
                                                           final boolean hasFocus,
                                                           final int row,
                                                           final int column) {
                JComponent result = (JComponent)super.getTableCellRendererComponent(table != null ? table : getDefaultTable(), value, isSelected, hasFocus, row, column);
                result.setBorder(cellBorder);

                return result;
            }

            private JTable getDefaultTable() {
                if (defaultTable == null) {
                    defaultTable = new JTable();
                }

                return defaultTable;
            }
        };
    }

    private void adjustWidths() {
        if (width < minWidth) {
            width = minWidth;
        }
        if (width > maxWidth) {
            width = maxWidth;
        }

        if (preferredWidth < minWidth) {
            preferredWidth = minWidth;
        }
        if (preferredWidth > maxWidth) {
            preferredWidth = maxWidth;
        }
    }
}
