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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Vector;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class DefaultTableColumnModel implements TableColumnModel, PropertyChangeListener, ListSelectionListener, Serializable {
    protected Vector<TableColumn> tableColumns = new Vector<TableColumn>();
    protected ListSelectionModel selectionModel;
    protected int columnMargin = 1;
    protected EventListenerList listenerList = new EventListenerList();
    protected transient ChangeEvent changeEvent;
    protected boolean columnSelectionAllowed;
    protected int totalColumnWidth = -1;

    public DefaultTableColumnModel() {
        selectionModel = createSelectionModel();
        selectionModel.addListSelectionListener(this);
        alignSelectionModelToColumns();
    }

    public void addColumn(final TableColumn column) {
        if (column == null) {
            throw new IllegalArgumentException(Messages.getString("swing.78")); //$NON-NLS-1$
        }
        tableColumns.add(column);
        totalColumnWidth = -1;
        column.addPropertyChangeListener(this);
        alignSelectionModelToColumns();
        fireColumnAdded(new TableColumnModelEvent(this, tableColumns.size() - 2 >= 0 ? tableColumns.size() - 2 : tableColumns.size() - 1, tableColumns.size() - 1));
    }

    public void removeColumn(final TableColumn column) {
        int index = tableColumns.indexOf(column);
        if (tableColumns.remove(column)) {
            totalColumnWidth = -1;
            column.removePropertyChangeListener(this);
        }
        alignSelectionModelToColumns();
        fireColumnRemoved(new TableColumnModelEvent(this, index, index));
    }

    public void moveColumn(final int columnIndex, final int newIndex) {
        if (columnIndex < 0 || columnIndex >= getColumnCount()
            || newIndex < 0 || newIndex > getColumnCount()) {

            throw new IllegalArgumentException(Messages.getString("swing.79")); //$NON-NLS-1$
        }

        if (columnIndex != newIndex) {
            TableColumn firstColumn = getColumn(columnIndex);
            tableColumns.remove(columnIndex);
            tableColumns.add(newIndex, firstColumn);

            boolean oldIsSelected = selectionModel.isSelectedIndex(columnIndex);
            selectionModel.removeIndexInterval(columnIndex, columnIndex);
            selectionModel.insertIndexInterval(newIndex, 1, true);
            if (oldIsSelected) {
                selectionModel.addSelectionInterval(newIndex, newIndex);
            } else {
                selectionModel.removeSelectionInterval(newIndex, newIndex);
            }
        }

        fireColumnMoved(new TableColumnModelEvent(this, columnIndex, newIndex));
    }

    public void setColumnMargin(final int margin) {
        if (columnMargin != margin) {
            columnMargin = margin;
            fireColumnMarginChanged();
        }
    }

    public int getColumnMargin() {
        return columnMargin;
    }

    public int getColumnCount() {
        return tableColumns.size();
    }

    public Enumeration<TableColumn> getColumns() {
        return Collections.enumeration(tableColumns);
    }

    public int getColumnIndex(final Object identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException(Messages.getString("swing.7A")); //$NON-NLS-1$
        }

        for (int i = 0; i < getColumnCount(); i++) {
            TableColumn next = getColumn(i);
            if (identifier.equals(next.getIdentifier())) {
                return i;
            }
        }

        throw new IllegalArgumentException(Messages.getString("swing.7B")); //$NON-NLS-1$
    }

    public TableColumn getColumn(final int columnIndex) {
        return (TableColumn)tableColumns.get(columnIndex);
    }

    public int getColumnIndexAtX(final int x) {
        int cumulativeWidth = 0;
        for (int i = 0; i < getColumnCount(); i++) {
            int width = getColumn(i).width;
            if (cumulativeWidth <= x && cumulativeWidth + width > x) {
                return i;
            }
            cumulativeWidth += width;
        }

        return -1;
    }

    public int getTotalColumnWidth() {
        if (totalColumnWidth == -1) {
            recalcWidthCache();
        }

        return totalColumnWidth;
    }

    public void setSelectionModel(final ListSelectionModel model) {
        if (model == null) {
            throw new IllegalArgumentException(Messages.getString("swing.7C")); //$NON-NLS-1$
        }
        selectionModel.removeListSelectionListener(this);

        selectionModel = model;
        selectionModel.addListSelectionListener(this);
        alignSelectionModelToColumns();
    }

    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setColumnSelectionAllowed(final boolean allowed) {
        columnSelectionAllowed = allowed;
    }

    public boolean getColumnSelectionAllowed() {
        return columnSelectionAllowed;
    }

    public int[] getSelectedColumns() {
        int[] result = new int[getSelectedColumnCount()];
        int index = 0;
        for (int i = 0; i < getColumnCount(); i++) {
            if (selectionModel.isSelectedIndex(i)) {
                result[index++] = i;
            }
        }

        return result;
    }

    public int getSelectedColumnCount() {
        if (selectionModel.isSelectionEmpty()) {
            return 0;
        }

        int result = 0;
        for (int i = 0; i < getColumnCount(); i++) {
            if (selectionModel.isSelectedIndex(i)) {
                result++;
            }
        }

        return result;
    }

    public void addColumnModelListener(final TableColumnModelListener listener) {
        listenerList.add(TableColumnModelListener.class, listener);
    }

    public void removeColumnModelListener(final TableColumnModelListener listener) {
        listenerList.remove(TableColumnModelListener.class, listener);
    }

    public TableColumnModelListener[] getColumnModelListeners() {
        return (TableColumnModelListener[])listenerList.getListeners(TableColumnModelListener.class);
    }

    public <T extends EventListener> T[] getListeners(final Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    public void propertyChange(final PropertyChangeEvent e) {
        if (TableColumn.WIDTH_PROPERTY.equals(e.getPropertyName())
            || TableColumn.PREFERRED_WIDTH_PROPERTY.equals(e.getPropertyName())) {

            totalColumnWidth = -1;
            fireColumnMarginChanged();
        }
    }

    public void valueChanged(final ListSelectionEvent e) {
        fireColumnSelectionChanged(e);
    }


    protected void fireColumnAdded(final TableColumnModelEvent e) {
        TableColumnModelListener[] listeners = getColumnModelListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].columnAdded(e);
        }
    }

    protected void fireColumnRemoved(final TableColumnModelEvent e) {
        TableColumnModelListener[] listeners = getColumnModelListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].columnRemoved(e);
        }
    }

    protected void fireColumnMoved(final TableColumnModelEvent e) {
        TableColumnModelListener[] listeners = getColumnModelListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].columnMoved(e);
        }
    }

    protected void fireColumnSelectionChanged(final ListSelectionEvent e) {
        TableColumnModelListener[] listeners = getColumnModelListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].columnSelectionChanged(e);
        }
    }

    protected void fireColumnMarginChanged() {
        TableColumnModelListener[] listeners = getColumnModelListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].columnMarginChanged(getChangeEvent());
        }
    }

    protected ListSelectionModel createSelectionModel() {
        return new DefaultListSelectionModel();
    }

    protected void recalcWidthCache() {
        if (getColumnCount() == 0) {
            totalColumnWidth = -1;
        }
        totalColumnWidth = 0;
        for (int i = 0; i < getColumnCount(); i++) {
            totalColumnWidth += getColumn(i).width;
        }
    }


    private ChangeEvent getChangeEvent() {
        if (changeEvent == null) {
            changeEvent = new ChangeEvent(this);
        }

        return changeEvent;
    }

    private void alignSelectionModelToColumns() {
        if (getColumnCount() == 0) {
            if (selectionModel.getAnchorSelectionIndex() >= 0) {
                selectionModel.setValueIsAdjusting(true);
                selectionModel.setAnchorSelectionIndex(-1);
                selectionModel.setLeadSelectionIndex(-1);
                selectionModel.setValueIsAdjusting(false);
            }
        } else if (selectionModel.getLeadSelectionIndex() < 0) {
            selectionModel.removeSelectionInterval(0, 0);
        }
    }
}
