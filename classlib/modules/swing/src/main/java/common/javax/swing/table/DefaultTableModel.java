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

import java.util.Vector;

import javax.swing.event.TableModelEvent;

public class DefaultTableModel extends AbstractTableModel {
    protected Vector dataVector;
    protected Vector columnIdentifiers;

    public DefaultTableModel() {
        setDataVector(new Vector(), new Vector());
    }

    public DefaultTableModel(final int rowCount, final int columnCount) {
        setDataVector(new Vector(), new Vector());
        setColumnCount(columnCount);
        setRowCount(rowCount);
    }

    public DefaultTableModel(final Vector columnNames, final int rowCount) {
        setDataVector(new Vector(), columnNames);
        setRowCount(rowCount);
    }

    public DefaultTableModel(final Object[] columnNames, final int rowCount) {
        this(convertToVector(columnNames), rowCount);
    }

    public DefaultTableModel(final Vector data, final Vector columnNames) {
        setDataVector(data, columnNames);
    }

    public DefaultTableModel(final Object[][] data, final Object[] columnNames) {
        setDataVector(data, columnNames);
    }

    public Vector getDataVector() {
        return dataVector;
    }

    public void setDataVector(final Vector data, final Vector identifiers) {
        columnIdentifiers = identifiers != null ? identifiers : new Vector() ;
        dataVector = data != null ? data : new Vector();
        alignRowsLengthAndCount();
        for (int i = 0; i < getRowCount(); i++) {
            Vector row = (Vector)dataVector.get(i);
            for (int j = 0; j < getColumnCount(); j++) {
                Vector dataRow = (Vector)data.get(i);
                if (dataRow.size() < j) {
                    row.setElementAt(dataRow.get(j), i);
                }
            }
        }

        fireTableStructureChanged();
    }

    public void setDataVector(final Object[][] dataVector, final Object[] columnIdentifiers) {
        setDataVector(convertToVector(dataVector), convertToVector(columnIdentifiers));
    }

    public void newDataAvailable(final TableModelEvent e) {
        fireTableChanged(e);
    }

    public void newRowsAdded(final TableModelEvent e) {
        if (e.getFirstRow() >=0 && e.getLastRow() >= e.getFirstRow()) {
            for (int i = e.getFirstRow(); i <= e.getLastRow(); i++) {
                ((Vector)dataVector.get(i)).setSize(getColumnCount());
            }
        }
        fireTableChanged(e);
    }

    public void rowsRemoved(final TableModelEvent e) {
        fireTableChanged(e);
    }

    public void setNumRows(final int rowCount) {
        setRowCount(rowCount);
    }

    public void setRowCount(final int rowCount) {
        int oldSize = dataVector.size();
        if (oldSize < rowCount) {
            for (int i = oldSize; i < rowCount; i++) {
                dataVector.add(new Vector());
            }
            newRowsAdded(new TableModelEvent(this, oldSize, rowCount - 1, TableModelEvent.HEADER_ROW, TableModelEvent.INSERT));
        } else if (oldSize > rowCount) {
            dataVector.setSize(rowCount);
            rowsRemoved(new TableModelEvent(this, rowCount, oldSize - 1, TableModelEvent.HEADER_ROW, TableModelEvent.DELETE));
        }
    }

    public void addRow(final Vector rowData) {
        dataVector.add(rowData != null ? rowData : new Vector());
        newRowsAdded(new TableModelEvent(this, dataVector.size() - 1, dataVector.size() - 1, TableModelEvent.HEADER_ROW, TableModelEvent.INSERT));
    }

    public void addRow(final Object[] rowData) {
        addRow(convertToVector(rowData));
    }

    public void insertRow(final int row, final Vector rowData) {
        dataVector.add(row, rowData != null ? rowData : new Vector());
        newRowsAdded(new TableModelEvent(this, row, row, TableModelEvent.HEADER_ROW, TableModelEvent.INSERT));
    }

    public void insertRow(final int row, final Object[] rowData) {
        insertRow(row, convertToVector(rowData));
    }

    public void moveRow(final int start, final int end, final int to) {
        int intervalLength = end - start + 1;
        if (start < to) {
            for (int i = 0; i < intervalLength; i++) {
                Object row = dataVector.remove(start);
                dataVector.add(to + intervalLength - 1, row);
            }
            fireTableRowsUpdated(start, to + intervalLength - 1);
        } else {
            for (int i = 0; i < intervalLength; i++) {
                Object row = dataVector.remove(start + i);
                dataVector.add(to + i, row);
            }
            fireTableRowsUpdated(to, end);
        }
    }

    public void removeRow(final int row) {
        dataVector.remove(row);
        rowsRemoved(new TableModelEvent(this, row, row, TableModelEvent.HEADER_ROW, TableModelEvent.DELETE));
    }

    public void setColumnIdentifiers(final Vector identifiers) {
        columnIdentifiers = identifiers;
        alignRowsLengthAndCount();
        fireTableStructureChanged();
    }

    public void setColumnIdentifiers(final Object[] identifiers) {
        setColumnIdentifiers(convertToVector(identifiers));
    }

    public void setColumnCount(final int columnCount) {
        columnIdentifiers.setSize(columnCount);
        alignRowsLengthAndCount();
        fireTableStructureChanged();
    }

    public void addColumn(final Object columnName) {
        addColumn(columnName, (Vector)null);
    }

    public void addColumn(final Object columnName, final Vector columnData) {
        columnIdentifiers.add(columnName);
        alignRowsLengthAndCount();

        if (columnData != null) {
            for (int i = 0; i < dataVector.size(); i++) {
                Vector row = (Vector)dataVector.get(i);
                if (columnData.size() > i) {
                    row.setElementAt(columnData.get(i), columnIdentifiers.size() - 1);
                } else {
                    break;
                }
            }
        }
        fireTableStructureChanged();
    }

    public void addColumn(final Object columnName, final Object[] columnData) {
        addColumn(columnName, convertToVector(columnData));
    }

    public int getRowCount() {
        return dataVector.size();
    }

    public int getColumnCount() {
        return columnIdentifiers.size();
    }

    public String getColumnName(final int column) {
        return columnIdentifiers.size() > column && columnIdentifiers.get(column) != null ? columnIdentifiers.get(column).toString() : super.getColumnName(column);
    }

    public boolean isCellEditable(final int row, final int column) {
        return true;
    }

    public Object getValueAt(final int row, final int column) {
        return ((Vector)dataVector.get(row)).get(column);
    }

    public void setValueAt(final Object value, final int row, final int column) {
        ((Vector)dataVector.get(row)).setElementAt(value, column);
        fireTableCellUpdated(row, column);
    }


    protected static Vector convertToVector(final Object[] data) {
        if (data == null) {
            return null;
        }
        Vector result = new Vector();
        for (int i = 0; i < data.length; i++) {
            result.add(data[i]);
        }

        return result;
    }

    protected static Vector convertToVector(final Object[][] data) {
        if (data == null) {
            return null;
        }
        Vector result = new Vector();
        for (int i = 0; i < data.length; i++) {
            result.add(convertToVector(data[i]));
        }

        return result;
    }


    private void alignRowsLengthAndCount() {
        for (int i = 0; i < getRowCount(); i++) {
            if (dataVector.size() == i) {
                dataVector.add(new Vector());
            }
            Vector next = (Vector)dataVector.get(i);
            if (next == null) {
                next = new Vector();
                dataVector.setElementAt(next, i);
            }
            next.setSize(getColumnCount());
        }
    }
}
