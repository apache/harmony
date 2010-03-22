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
import java.util.Enumeration;
import java.util.EventObject;
import javax.swing.BasicSwingTestCase;
import javax.swing.DefaultListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

public class DefaultTableColumnModelTest extends BasicSwingTestCase {
    private DefaultTableColumnModel model;

    public DefaultTableColumnModelTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        model = new DefaultTableColumnModel();
    }

    @Override
    protected void tearDown() throws Exception {
        model = null;
    }

    public void testDefaultTableColumnModel() throws Exception {
        assertNotNull(model.tableColumns);
        assertEquals(0, model.tableColumns.size());
        assertTrue(model.selectionModel instanceof DefaultListSelectionModel);
        assertEquals(1, ((DefaultListSelectionModel) model.selectionModel)
                .getListSelectionListeners().length);
        assertEquals(model, ((DefaultListSelectionModel) model.selectionModel)
                .getListSelectionListeners()[0]);
        assertEquals(1, model.columnMargin);
        assertNotNull(model.listenerList);
        assertNull(model.changeEvent);
        assertFalse(model.columnSelectionAllowed);
        assertEquals(-1, model.totalColumnWidth);
    }

    public void testAddRemoveMoveColumn() throws Exception {
        TestTableColumnModelListener listener = new TestTableColumnModelListener();
        model.addColumnModelListener(listener);
        TableColumn column1 = new TableColumn();
        model.addColumn(column1);
        assertEquals(1, model.getColumnCount());
        assertEquals(0, column1.getModelIndex());
        assertTrue(listener.eventOccured());
        assertEquals(TestTableColumnModelListener.COLUMN_ADDED, listener.getEventType());
        assertEquals(model, ((TableColumnModelEvent) listener.getEvent()).getSource());
        assertEquals(0, ((TableColumnModelEvent) listener.getEvent()).getFromIndex());
        assertEquals(0, ((TableColumnModelEvent) listener.getEvent()).getToIndex());
        assertEquals(1, column1.getPropertyChangeListeners().length);
        assertEquals(model, column1.getPropertyChangeListeners()[0]);
        listener.reset();
        TableColumn column2 = new TableColumn();
        model.addColumn(column2);
        assertEquals(2, model.getColumnCount());
        assertEquals(0, column2.getModelIndex());
        assertTrue(listener.eventOccured());
        assertEquals(TestTableColumnModelListener.COLUMN_ADDED, listener.getEventType());
        assertEquals(model, ((TableColumnModelEvent) listener.getEvent()).getSource());
        assertEquals(0, ((TableColumnModelEvent) listener.getEvent()).getFromIndex());
        assertEquals(1, ((TableColumnModelEvent) listener.getEvent()).getToIndex());
        listener.reset();
        model.removeColumn(column1);
        assertEquals(1, model.getColumnCount());
        assertEquals(column2, model.getColumn(0));
        assertTrue(listener.eventOccured());
        assertEquals(TestTableColumnModelListener.COLUMN_REMOVED, listener.getEventType());
        assertEquals(model, ((TableColumnModelEvent) listener.getEvent()).getSource());
        assertEquals(0, ((TableColumnModelEvent) listener.getEvent()).getFromIndex());
        assertEquals(0, ((TableColumnModelEvent) listener.getEvent()).getToIndex());
        assertEquals(0, column1.getPropertyChangeListeners().length);
        listener.reset();
        model.addColumn(column1);
        assertEquals(column2, model.getColumn(0));
        assertEquals(column1, model.getColumn(1));
        assertTrue(listener.eventOccured());
        assertEquals(TestTableColumnModelListener.COLUMN_ADDED, listener.getEventType());
        assertEquals(model, ((TableColumnModelEvent) listener.getEvent()).getSource());
        assertEquals(0, ((TableColumnModelEvent) listener.getEvent()).getFromIndex());
        assertEquals(1, ((TableColumnModelEvent) listener.getEvent()).getToIndex());
        listener.reset();
        model.moveColumn(0, 1);
        assertEquals(column1, model.getColumn(0));
        assertEquals(column2, model.getColumn(1));
        assertTrue(listener.eventOccured());
        assertEquals(TestTableColumnModelListener.COLUMN_MOVED, listener.getEventType());
        assertEquals(model, ((TableColumnModelEvent) listener.getEvent()).getSource());
        assertEquals(0, ((TableColumnModelEvent) listener.getEvent()).getFromIndex());
        assertEquals(1, ((TableColumnModelEvent) listener.getEvent()).getToIndex());
        listener.reset();
        model.moveColumn(1, 0);
        assertEquals(column2, model.getColumn(0));
        assertEquals(column1, model.getColumn(1));
        assertTrue(listener.eventOccured());
        assertEquals(TestTableColumnModelListener.COLUMN_MOVED, listener.getEventType());
        assertEquals(model, ((TableColumnModelEvent) listener.getEvent()).getSource());
        assertEquals(1, ((TableColumnModelEvent) listener.getEvent()).getFromIndex());
        assertEquals(0, ((TableColumnModelEvent) listener.getEvent()).getToIndex());
        listener.reset();
        model.moveColumn(0, 0);
        assertEquals(column2, model.getColumn(0));
        assertEquals(column1, model.getColumn(1));
        assertTrue(listener.eventOccured());
        model.removeColumn(null);
        assertEquals(2, model.getColumnCount());
        assertTrue(listener.eventOccured());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.addColumn(null);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.moveColumn(5, 1);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.moveColumn(1, -1);
            }
        });
    }

    public void testGetSetColumnMargin() throws Exception {
        TestTableColumnModelListener listener = new TestTableColumnModelListener();
        model.addColumnModelListener(listener);
        assertNull(model.changeEvent);
        assertEquals(1, model.getColumnMargin());
        model.setColumnMargin(10);
        assertEquals(10, model.getColumnMargin());
        assertTrue(listener.eventOccured());
        assertEquals(TestTableColumnModelListener.MARGIN_CHANGED, listener.getEventType());
        assertEquals(model, ((ChangeEvent) listener.getEvent()).getSource());
        assertNotNull(model.changeEvent);
        model.setColumnMargin(-1);
        assertEquals(-1, model.getColumnMargin());
    }

    public void testGetColumnCount() throws Exception {
        assertEquals(0, model.getColumnCount());
        model.addColumn(new TableColumn());
        assertEquals(1, model.getColumnCount());
        model.addColumn(new TableColumn());
        assertEquals(2, model.getColumnCount());
    }

    public void testGetColumns() throws Exception {
        Enumeration<?> columns = model.getColumns();
        assertNotNull(columns);
        assertFalse(columns.hasMoreElements());
        TableColumn column = new TableColumn();
        model.addColumn(column);
        columns = model.getColumns();
        assertNotNull(columns);
        assertTrue(columns.hasMoreElements());
        assertEquals(column, columns.nextElement());
        assertFalse(columns.hasMoreElements());
    }

    public void testGetColumnIndex() throws Exception {
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.getColumnIndex(null);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.getColumnIndex("tag1");
            }
        });
        TableColumn column1 = new TableColumn();
        TableColumn column2 = new TableColumn();
        TableColumn column3 = new TableColumn();
        TableColumn column4 = new TableColumn();
        column2.setIdentifier("tag1");
        column3.setIdentifier("tag2");
        column4.setIdentifier("tag1");
        model.addColumn(column1);
        model.addColumn(column2);
        model.addColumn(column3);
        model.addColumn(column4);
        assertEquals(1, model.getColumnIndex("tag1"));
        assertEquals(2, model.getColumnIndex("tag2"));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.getColumnIndex("tag3");
            }
        });
    }

    public void testGetColumn() throws Exception {
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.getColumn(0);
            }
        });
        TableColumn column1 = new TableColumn();
        TableColumn column2 = new TableColumn();
        TableColumn column3 = new TableColumn();
        TableColumn column4 = new TableColumn();
        model.addColumn(column1);
        model.addColumn(column2);
        model.addColumn(column3);
        model.addColumn(column4);
        assertEquals(column1, model.getColumn(0));
        assertEquals(column2, model.getColumn(1));
        assertEquals(column3, model.getColumn(2));
        assertEquals(column4, model.getColumn(3));
    }

    public void testGetColumnIndexAtX() throws Exception {
        assertEquals(-1, model.getColumnIndexAtX(0));
        model.addColumn(new TableColumn(0, 10));
        model.addColumn(new TableColumn(0, 20));
        model.addColumn(new TableColumn(0, 30));
        model.addColumn(new TableColumn(0, 40));
        assertEquals(0, model.getColumnIndexAtX(0));
        assertEquals(0, model.getColumnIndexAtX(9));
        assertEquals(1, model.getColumnIndexAtX(10));
        assertEquals(1, model.getColumnIndexAtX(29));
        assertEquals(2, model.getColumnIndexAtX(30));
        assertEquals(2, model.getColumnIndexAtX(59));
        assertEquals(3, model.getColumnIndexAtX(60));
        assertEquals(3, model.getColumnIndexAtX(99));
        assertEquals(-1, model.getColumnIndexAtX(100));
        assertEquals(-1, model.getColumnIndexAtX(-5));
    }

    public void testGetTotalColumnWidth() throws Exception {
        assertEquals(0, model.getTotalColumnWidth());
        model.addColumn(new TableColumn(0, 10));
        model.addColumn(new TableColumn(0, 20));
        model.addColumn(new TableColumn(0, 30));
        model.addColumn(new TableColumn(0, 40));
        assertEquals(100, model.getTotalColumnWidth());
    }

    public void testGetSetSelectionModel() throws Exception {
        assertTrue(model.getSelectionModel() instanceof DefaultListSelectionModel);
        DefaultListSelectionModel oldModel = (DefaultListSelectionModel) model.selectionModel;
        assertEquals(1, oldModel.getListSelectionListeners().length);
        assertEquals(model, oldModel.getListSelectionListeners()[0]);
        DefaultListSelectionModel newModel = new DefaultListSelectionModel();
        model.setSelectionModel(newModel);
        assertEquals(newModel, model.getSelectionModel());
        assertEquals(1, newModel.getListSelectionListeners().length);
        assertEquals(model, newModel.getListSelectionListeners()[0]);
        assertEquals(0, oldModel.getListSelectionListeners().length);
    }

    public void testGetSetColumnSelectionAllowed() throws Exception {
        TestTableColumnModelListener listener = new TestTableColumnModelListener();
        TestListSelectionListener selectionListener = new TestListSelectionListener();
        model.addColumnModelListener(listener);
        model.getSelectionModel().addListSelectionListener(selectionListener);
        assertFalse(model.getColumnSelectionAllowed());
        model.setColumnSelectionAllowed(true);
        assertTrue(model.getColumnSelectionAllowed());
        assertFalse(listener.eventOccured());
        assertFalse(selectionListener.eventOccured());
    }

    public void testGetSelectedColumns() throws Exception {
        assertEquals(0, model.getSelectedColumns().length);
        model.addColumn(new TableColumn());
        model.addColumn(new TableColumn());
        model.addColumn(new TableColumn());
        model.addColumn(new TableColumn());
        model.addColumn(new TableColumn());
        assertEquals(0, model.getSelectedColumns().length);
        model.getSelectionModel().setSelectionInterval(1, 1);
        model.getSelectionModel().addSelectionInterval(3, 4);
        int[] selectedIndices = model.getSelectedColumns();
        assertEquals(3, selectedIndices.length);
        assertEquals(1, selectedIndices[0]);
        assertEquals(3, selectedIndices[1]);
        assertEquals(4, selectedIndices[2]);
    }

    public void testGetSelectedColumnCount() throws Exception {
        assertEquals(0, model.getSelectedColumnCount());
        model.addColumn(new TableColumn());
        model.addColumn(new TableColumn());
        model.addColumn(new TableColumn());
        model.addColumn(new TableColumn());
        model.addColumn(new TableColumn());
        assertEquals(0, model.getSelectedColumnCount());
        model.getSelectionModel().setSelectionInterval(1, 1);
        model.getSelectionModel().addSelectionInterval(3, 4);
        assertEquals(3, model.getSelectedColumnCount());
    }

    public void testAddRemoveGetTableColumnModelListener() throws Exception {
        assertEquals(0, model.getColumnModelListeners().length);
        TableColumnModelListener listener = new TestTableColumnModelListener();
        model.addColumnModelListener(listener);
        model.addColumnModelListener(new TestTableColumnModelListener());
        assertEquals(2, model.getColumnModelListeners().length);
        model.removeColumnModelListener(listener);
        assertEquals(1, model.getColumnModelListeners().length);
    }

    public void testgetListeners() throws Exception {
        assertEquals(0, model.getListeners(TableColumnModelListener.class).length);
        model.addColumnModelListener(new TestTableColumnModelListener());
        assertEquals(1, model.getListeners(TableColumnModelListener.class).length);
    }

    public void testPropertyChange() throws Exception {
        TableColumn column = new TableColumn();
        model.addColumn(column);
        assertEquals(-1, model.totalColumnWidth);
        assertEquals(75, model.getTotalColumnWidth());
        column.setWidth(25);
        assertEquals(-1, model.totalColumnWidth);
        assertEquals(25, model.getTotalColumnWidth());
        model.totalColumnWidth = 10;
        model.propertyChange(new PropertyChangeEvent("source", TableColumn.WIDTH_PROPERTY, "a",
                "b"));
        assertEquals(-1, model.totalColumnWidth);
        model.totalColumnWidth = 10;
        model.propertyChange(new PropertyChangeEvent("source",
                TableColumn.PREFERRED_WIDTH_PROPERTY, "a", "b"));
        assertEquals(-1, model.totalColumnWidth);
    }

    public void testValueChanged() throws Exception {
        TestTableColumnModelListener listener = new TestTableColumnModelListener();
        model.addColumnModelListener(listener);
        model.getSelectionModel().setSelectionInterval(1, 2);
        assertTrue(listener.eventOccured());
        assertEquals(TestTableColumnModelListener.SELECTION_CHANGED, listener.getEventType());
        assertEquals(model.getSelectionModel(), listener.getEvent().getSource());
        assertEquals(1, ((ListSelectionEvent) listener.getEvent()).getFirstIndex());
        assertEquals(2, ((ListSelectionEvent) listener.getEvent()).getLastIndex());
        listener.reset();
        ListSelectionEvent event = new ListSelectionEvent("source", 4, 7, true);
        model.valueChanged(event);
        assertTrue(listener.eventOccured());
        assertEquals(TestTableColumnModelListener.SELECTION_CHANGED, listener.getEventType());
        assertEquals("source", listener.getEvent().getSource());
        assertEquals(4, ((ListSelectionEvent) listener.getEvent()).getFirstIndex());
        assertEquals(7, ((ListSelectionEvent) listener.getEvent()).getLastIndex());
    }

    public void testRecalcWidthCache() throws Exception {
        assertEquals(-1, model.totalColumnWidth);
        assertEquals(0, model.getTotalColumnWidth());
        assertEquals(0, model.totalColumnWidth);
        model.addColumn(new TableColumn());
        assertEquals(-1, model.totalColumnWidth);
        model.recalcWidthCache();
        assertEquals(75, model.totalColumnWidth);
        model.removeColumn(model.getColumn(0));
        assertEquals(-1, model.totalColumnWidth);
        model.addColumn(new TableColumn());
        model.addColumn(new TableColumn());
        model.totalColumnWidth = -1;
        model.moveColumn(0, 1);
        assertEquals(-1, model.totalColumnWidth);
    }

    private class TestTableColumnModelListener implements TableColumnModelListener {
        public static final int COLUMN_ADDED = 0;

        public static final int COLUMN_REMOVED = 1;

        public static final int COLUMN_MOVED = 2;

        public static final int MARGIN_CHANGED = 3;

        public static final int SELECTION_CHANGED = 4;

        private EventObject event;

        private int eventType = -1;

        public void columnMarginChanged(final ChangeEvent e) {
            event = e;
            eventType = MARGIN_CHANGED;
        }

        public void columnSelectionChanged(final ListSelectionEvent e) {
            event = e;
            eventType = SELECTION_CHANGED;
        }

        public void columnAdded(final TableColumnModelEvent e) {
            event = e;
            eventType = COLUMN_ADDED;
        }

        public void columnMoved(final TableColumnModelEvent e) {
            event = e;
            eventType = COLUMN_MOVED;
        }

        public void columnRemoved(final TableColumnModelEvent e) {
            event = e;
            eventType = COLUMN_REMOVED;
        }

        public EventObject getEvent() {
            return event;
        }

        public int getEventType() {
            return eventType;
        }

        public boolean eventOccured() {
            return eventType != -1;
        }

        public void reset() {
            event = null;
            eventType = -1;
        }
    }

    private class TestListSelectionListener implements ListSelectionListener {
        private ListSelectionEvent event;

        public void valueChanged(ListSelectionEvent e) {
            event = e;
        }

        public ListSelectionEvent getEvent() {
            return event;
        }

        public boolean eventOccured() {
            return event != null;
        }

        public void reset() {
            event = null;
        }
    }
}
