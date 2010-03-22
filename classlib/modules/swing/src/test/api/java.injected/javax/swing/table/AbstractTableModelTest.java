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

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class AbstractTableModelTest extends BasicSwingTableTestCase {
    private AbstractTableModel model;

    public AbstractTableModelTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        model = new AbstractTableModel() {
            private static final long serialVersionUID = 1L;

            public int getRowCount() {
                return 5;
            }

            public int getColumnCount() {
                return 20000;
            }

            public Object getValueAt(final int rowIndex, final int columnIndex) {
                return "value at [" + rowIndex + "," + columnIndex + "]";
            }
        };
    }

    public void testAbstractTableModel() throws Exception {
        assertNotNull(model.listenerList);
        assertEquals(0, model.listenerList.getListenerCount());
    }

    public void testGetColumnName() throws Exception {
        assertEquals("A", model.getColumnName(0));
        assertEquals("B", model.getColumnName(1));
        assertEquals("C", model.getColumnName(2));
        assertEquals("Z", model.getColumnName(25));
        assertEquals("AA", model.getColumnName(26));
        assertEquals("AK", model.getColumnName(36));
        assertEquals("SU", model.getColumnName(514));
        assertEquals("AAJ", model.getColumnName(711));
        assertEquals("ATS", model.getColumnName(1214));
        assertEquals("BFG", model.getColumnName(1514));
        assertEquals("ABVO", model.getColumnName(19514));
    }

    public void testFindColumn() throws Exception {
        assertEquals(-1, model.findColumn("a"));
        assertEquals(-1, model.findColumn("Ba"));
        assertEquals(0, model.findColumn("A"));
        assertEquals(514, model.findColumn("SU"));
        assertEquals(711, model.findColumn("AAJ"));
        assertEquals(1514, model.findColumn("BFG"));
        assertEquals(19514, model.findColumn("ABVO"));
        assertEquals(-1, model.findColumn("BBVO"));
    }

    public void testGetColumnClass() throws Exception {
        assertEquals(Object.class, model.getColumnClass(555));
    }

    public void testIsCellEditable() throws Exception {
        assertFalse(model.isCellEditable(2, 4));
        assertFalse(model.isCellEditable(20, 40000));
    }

    public void testSetValueAt() throws Exception {
        model.setValueAt("any", 0, 0);
        model.setValueAt("any", 10000, 10000);
        assertNotSame("any", model.getValueAt(0, 0));
    }

    public void testAddRemoveGetTableModelListener() throws Exception {
        assertEquals(0, model.getTableModelListeners().length);
        assertEquals(0, model.getListeners(TableModelListener.class).length);
        TableModelListener listener1 = new TestTableModelListener();
        TableModelListener listener2 = new TestTableModelListener();
        model.addTableModelListener(listener1);
        assertEquals(1, model.getTableModelListeners().length);
        assertEquals(1, model.getListeners(TableModelListener.class).length);
        model.addTableModelListener(listener2);
        assertEquals(2, model.getTableModelListeners().length);
        assertEquals(2, model.getListeners(TableModelListener.class).length);
        model.removeTableModelListener(listener1);
        assertEquals(1, model.getTableModelListeners().length);
        assertEquals(1, model.getListeners(TableModelListener.class).length);
    }

    public void testFireTableDataChanged() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.fireTableDataChanged();
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(0, listener.getEvent().getFirstRow());
        assertEquals(Integer.MAX_VALUE, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
    }

    public void testFireTableStructureChanged() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.fireTableStructureChanged();
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getFirstRow());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
    }

    public void testFireTableRowsInserted() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.fireTableRowsInserted(5, 40000);
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(5, listener.getEvent().getFirstRow());
        assertEquals(40000, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.INSERT, listener.getEvent().getType());
    }

    public void testFireTableRowsUpdated() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.fireTableRowsUpdated(5, 40000);
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(5, listener.getEvent().getFirstRow());
        assertEquals(40000, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
    }

    public void testFireTableRowsDeleted() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.fireTableRowsDeleted(5, 40000);
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(5, listener.getEvent().getFirstRow());
        assertEquals(40000, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.DELETE, listener.getEvent().getType());
    }

    public void testFireTableCellUpdated() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.fireTableCellUpdated(5, 50);
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(5, listener.getEvent().getFirstRow());
        assertEquals(5, listener.getEvent().getLastRow());
        assertEquals(50, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
    }

    public void testFireTableChanged() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        TableModelEvent event = new TableModelEvent(model, -5, 4, 3, TableModelEvent.INSERT);
        model.fireTableChanged(event);
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(-5, listener.getEvent().getFirstRow());
        assertEquals(4, listener.getEvent().getLastRow());
        assertEquals(3, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.INSERT, listener.getEvent().getType());
    }
}
