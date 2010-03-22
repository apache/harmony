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

import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public abstract class AbstractTableModel implements TableModel, Serializable {
    protected EventListenerList listenerList = new EventListenerList();

    public String getColumnName(final int column) {
        StringBuilder result = new StringBuilder();
        int rest = column;
        do {
            int remainder = rest % 26;
            result.append(indexToLetter(remainder));
            rest = rest / 26 - 1;
            if (rest == 0) {
                result.append(indexToLetter(0));
            }
        } while (rest > 0);

        return result.reverse().toString();
    }

    public int findColumn(final String columnName) {
        int result = -1;
        for (int i = 0; i < columnName.length(); i++) {
            int index = letterToIndex(columnName.charAt(i));
            if (index == -1) {
                return -1;
            }
            result = 26 * (result + 1) + index;
        }
        return result < getColumnCount() ? result : -1;
    }

    public Class<?> getColumnClass(final int columnIndex) {
        return Object.class;
    }

    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return false;
    }

    public void setValueAt(final Object value, final int rowIndex, final int columnIndex) {

    }

    public void addTableModelListener(final TableModelListener l) {
        listenerList.add(TableModelListener.class, l);
    }

    public void removeTableModelListener(final TableModelListener l) {
        listenerList.remove(TableModelListener.class, l);
    }

    public TableModelListener[] getTableModelListeners() {
        return (TableModelListener[])listenerList.getListeners(TableModelListener.class);
    }

    public <T extends EventListener> T[] getListeners(final Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }


    public void fireTableDataChanged() {
        TableModelListener[] listeners = getTableModelListeners();
        if (listeners.length == 0) {
            return;
        }

        fireTableChanged(new TableModelEvent(this));
    }

    public void fireTableStructureChanged() {
        TableModelListener[] listeners = getTableModelListeners();
        if (listeners.length == 0) {
            return;
        }

        fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }

    public void fireTableRowsInserted(final int firstRow, final int lastRow) {
        TableModelListener[] listeners = getTableModelListeners();
        if (listeners.length == 0) {
            return;
        }

        fireTableChanged(new TableModelEvent(this, firstRow, lastRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
    }

    public void fireTableRowsUpdated(final int firstRow, final int lastRow) {
        TableModelListener[] listeners = getTableModelListeners();
        if (listeners.length == 0) {
            return;
        }

        fireTableChanged(new TableModelEvent(this, firstRow, lastRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE));
    }

    public void fireTableRowsDeleted(final int firstRow, final int lastRow) {
        TableModelListener[] listeners = getTableModelListeners();
        if (listeners.length == 0) {
            return;
        }

        fireTableChanged(new TableModelEvent(this, firstRow, lastRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
    }

    public void fireTableCellUpdated(final int row, final int column) {
        TableModelListener[] listeners = getTableModelListeners();
        if (listeners.length == 0) {
            return;
        }

        fireTableChanged(new TableModelEvent(this, row, row, column, TableModelEvent.UPDATE));
    }

    public void fireTableChanged(final TableModelEvent e) {
        TableModelListener[] listeners = getTableModelListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].tableChanged(e);
        }
    }


    private char indexToLetter(final int index) {
        return (char)('A' + index);
    }

    private int letterToIndex(final char letter) {
        int result = letter - 'A';
        return (result >= 0 && result < 26) ? result : -1;
    }
}
