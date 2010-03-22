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

import java.util.Arrays;
import java.util.Vector;
import javax.swing.event.TableModelEvent;

@SuppressWarnings("unchecked")
public class DefaultTableModelTest extends BasicSwingTableTestCase {
    private DefaultTableModel model;

    public DefaultTableModelTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        model = new DefaultTableModel();
    }

    @Override
    protected void tearDown() throws Exception {
        model = null;
    }

    public void testDefaultTableModel() throws Exception {
        assertNotNull(model.dataVector);
        assertNotNull(model.columnIdentifiers);
        assertEquals(0, model.dataVector.size());
        assertEquals(0, model.columnIdentifiers.size());
        model = new DefaultTableModel(3, 4);
        assertEquals(4, model.columnIdentifiers.size());
        assertNull(model.columnIdentifiers.get(0));
        assertNull(model.columnIdentifiers.get(3));
        assertEquals(3, model.dataVector.size());
        assertTrue(model.dataVector.get(0) instanceof Vector);
        assertTrue(model.dataVector.get(2) instanceof Vector);
        assertEquals(4, ((Vector) model.dataVector.get(0)).size());
        assertEquals(4, ((Vector) model.dataVector.get(2)).size());
        assertNull(((Vector) model.dataVector.get(0)).get(0));
        assertNull(((Vector) model.dataVector.get(2)).get(3));
        model = new DefaultTableModel(new Object[] { "col1", "col2", "col3" }, 4);
        assertEquals(3, model.columnIdentifiers.size());
        assertEquals("col2", model.columnIdentifiers.get(1));
        assertEquals(4, model.dataVector.size());
        Vector columnNames = new Vector(Arrays.asList(new Object[] { "col1", "col2", "col3" }));
        model = new DefaultTableModel(columnNames, 4);
        assertTrue(columnNames == model.columnIdentifiers);
        assertEquals(3, model.columnIdentifiers.size());
        assertEquals("col2", model.columnIdentifiers.get(1));
        assertEquals(4, model.dataVector.size());
        Vector dataVector = new Vector();
        dataVector
                .add(new Vector(Arrays.asList(new Object[] { "data11", "data12", "data13" })));
        dataVector
                .add(new Vector(Arrays.asList(new Object[] { "data21", "data22", "data23" })));
        model = new DefaultTableModel(dataVector, new Vector(Arrays.asList(new Object[] {
                "col1", "col2", "col3" })));
        assertTrue(dataVector == model.dataVector);
        assertEquals(3, model.columnIdentifiers.size());
        assertEquals(2, model.dataVector.size());
        model = new DefaultTableModel(null, new Vector(Arrays.asList(new Object[] { "col1",
                "col2", "col3" })));
        assertNotNull(model.dataVector);
        assertEquals(0, model.dataVector.size());
        model = new DefaultTableModel(dataVector, null);
        assertNotNull(model.columnIdentifiers);
        assertEquals(0, model.columnIdentifiers.size());
        assertEquals(2, model.dataVector.size());
        assertEquals(0, ((Vector) model.dataVector.get(0)).size());
        model = new DefaultTableModel(new Object[][] { { "data11", "data12", "data13" },
                { "data21", "data22", "data23" } }, new Object[] { "col1", "col2", "col3" });
        assertEquals(3, model.columnIdentifiers.size());
        assertEquals(2, model.dataVector.size());
    }

    public void testGetSetDataVector() throws Exception {
        Vector dataVector = new Vector();
        dataVector
                .add(new Vector(Arrays.asList(new Object[] { "data11", "data12", "data13" })));
        dataVector
                .add(new Vector(Arrays.asList(new Object[] { "data21", "data22", "data23" })));
        model = new DefaultTableModel(dataVector, new Vector(Arrays.asList(new Object[] {
                "col1", "col2", "col3" })));
        assertTrue(dataVector == model.getDataVector());
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        dataVector = new Vector();
        dataVector.add(new Vector(Arrays.asList(new Object[] { "value11", "value12" })));
        dataVector.add(new Vector(Arrays
                .asList(new Object[] { "value21", "value22", "value23" })));
        dataVector.add(new Vector(Arrays.asList(new Object[] { "value31" })));
        model.setDataVector(dataVector, new Vector(Arrays
                .asList(new Object[] { "col1", "col2" })));
        assertTrue(dataVector == model.getDataVector());
        assertTrue(dataVector == model.dataVector);
        assertEquals(2, model.columnIdentifiers.size());
        assertEquals(2, ((Vector) model.getDataVector().get(0)).size());
        assertEquals(2, ((Vector) model.getDataVector().get(1)).size());
        assertEquals(2, ((Vector) model.getDataVector().get(2)).size());
        assertEquals("value22", ((Vector) model.getDataVector().get(1)).get(1));
        assertNull(((Vector) model.getDataVector().get(2)).get(1));
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getFirstRow());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
        listener.reset();
        model.addTableModelListener(listener);
        Object[][] dataArray = new Object[][] { new Object[] { "value11", "value12" },
                new Object[] { "value21", "value22", "value23" }, new Object[] { "value31" } };
        model.setDataVector(dataArray, new Object[] { "col1", "col2" });
        assertEquals(2, model.columnIdentifiers.size());
        assertEquals(2, ((Vector) model.dataVector.get(0)).size());
        assertEquals(2, ((Vector) model.dataVector.get(1)).size());
        assertEquals(2, ((Vector) model.dataVector.get(2)).size());
        assertEquals("value22", ((Vector) model.dataVector.get(1)).get(1));
        assertNull(((Vector) model.dataVector.get(2)).get(1));
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getFirstRow());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
    }

    public void testNewDataAvailable() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        TableModelEvent event = new TableModelEvent(model);
        model.newDataAvailable(event);
        assertTrue(listener.eventOccured());
        assertEquals(event, listener.getEvent());
    }

    public void testNewRowsAdded() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.columnIdentifiers = new Vector(Arrays.asList(new Object[] { "col1", "col2" }));
        model.dataVector = new Vector();
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value11", "value12" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value21", "value22",
                "value23" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value31" })));
        TableModelEvent event = new TableModelEvent(model, 1, 2, TableModelEvent.HEADER_ROW,
                TableModelEvent.DELETE);
        model.newRowsAdded(event);
        assertEquals(2, model.columnIdentifiers.size());
        assertEquals(2, ((Vector) model.getDataVector().get(0)).size());
        assertEquals(2, ((Vector) model.getDataVector().get(1)).size());
        assertEquals(2, ((Vector) model.getDataVector().get(2)).size());
        assertTrue(listener.eventOccured());
        assertEquals(event, listener.getEvent());
    }

    public void testRowsRemoved() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        TableModelEvent event = new TableModelEvent(model);
        model.rowsRemoved(event);
        assertTrue(listener.eventOccured());
        assertEquals(event, listener.getEvent());
    }

    public void testSetNumRowsSetGetRowCount() throws Exception {
        assertEquals(0, model.getRowCount());
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.columnIdentifiers = new Vector(Arrays.asList(new Object[] { "col1", "col2" }));
        model.dataVector = new Vector();
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value11", "value12" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value21", "value22",
                "value23" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value31" })));
        assertEquals(3, model.getRowCount());
        listener.reset();
        model.setRowCount(5);
        assertEquals(5, model.getDataVector().size());
        assertEquals(5, model.getRowCount());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(3, listener.getEvent().getFirstRow());
        assertEquals(4, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.INSERT, listener.getEvent().getType());
        assertEquals(3, ((Vector) model.dataVector.get(1)).size());
        assertEquals(1, ((Vector) model.dataVector.get(2)).size());
        assertEquals(2, ((Vector) model.dataVector.get(3)).size());
        assertEquals(2, ((Vector) model.dataVector.get(4)).size());
        listener.reset();
        model.setRowCount(5);
        assertEquals(5, model.getDataVector().size());
        assertFalse(listener.eventOccured());
        listener.reset();
        model.setNumRows(1);
        assertEquals(1, model.getDataVector().size());
        assertEquals(1, model.getRowCount());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(1, listener.getEvent().getFirstRow());
        assertEquals(4, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.DELETE, listener.getEvent().getType());
    }

    public void testAddRow() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.columnIdentifiers = new Vector(Arrays.asList(new Object[] { "col1", "col2" }));
        model.dataVector = new Vector();
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value11", "value12" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value21", "value22",
                "value23" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value31" })));
        listener.reset();
        model.addRow((Vector) null);
        assertEquals(4, model.getDataVector().size());
        assertEquals(3, ((Vector) model.dataVector.get(1)).size());
        assertEquals(1, ((Vector) model.dataVector.get(2)).size());
        assertEquals(2, ((Vector) model.dataVector.get(3)).size());
        assertNull(((Vector) model.dataVector.get(3)).get(0));
        assertNull(((Vector) model.dataVector.get(3)).get(1));
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(3, listener.getEvent().getFirstRow());
        assertEquals(3, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.INSERT, listener.getEvent().getType());
        listener.reset();
        model.addRow(new Object[] { "a" });
        assertEquals(5, model.getDataVector().size());
        assertEquals(3, ((Vector) model.dataVector.get(1)).size());
        assertEquals(1, ((Vector) model.dataVector.get(2)).size());
        assertEquals(2, ((Vector) model.dataVector.get(3)).size());
        assertEquals(2, ((Vector) model.dataVector.get(4)).size());
        assertEquals("a", ((Vector) model.dataVector.get(4)).get(0));
        assertNull(((Vector) model.dataVector.get(4)).get(1));
    }

    public void testInsertRow() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.columnIdentifiers = new Vector(Arrays.asList(new Object[] { "col1", "col2" }));
        model.dataVector = new Vector();
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value11", "value12" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value21", "value22",
                "value23" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value31" })));
        listener.reset();
        model.insertRow(1, (Vector) null);
        assertEquals(4, model.getDataVector().size());
        assertEquals(2, ((Vector) model.dataVector.get(0)).size());
        assertEquals(2, ((Vector) model.dataVector.get(1)).size());
        assertEquals(3, ((Vector) model.dataVector.get(2)).size());
        assertEquals(1, ((Vector) model.dataVector.get(3)).size());
        assertNull(((Vector) model.dataVector.get(1)).get(0));
        assertNull(((Vector) model.dataVector.get(1)).get(1));
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(1, listener.getEvent().getFirstRow());
        assertEquals(1, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.INSERT, listener.getEvent().getType());
        listener.reset();
        model.insertRow(0, new Object[] { "a", "b", "c" });
        assertEquals(5, model.getDataVector().size());
        assertEquals(2, ((Vector) model.dataVector.get(0)).size());
        assertEquals(2, ((Vector) model.dataVector.get(1)).size());
        assertEquals(2, ((Vector) model.dataVector.get(2)).size());
        assertEquals(3, ((Vector) model.dataVector.get(3)).size());
        assertEquals(1, ((Vector) model.dataVector.get(4)).size());
        assertEquals("a", ((Vector) model.dataVector.get(0)).get(0));
        assertEquals("b", ((Vector) model.dataVector.get(0)).get(1));
        testExceptionalCase(new ArrayIndexOutOfBoundsExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.insertRow(6, new Object[] { "a", "b", "c" });
            }
        });
    }

    public void testMoveRow() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.columnIdentifiers = new Vector(Arrays.asList(new Object[] { "col1", "col2" }));
        model.dataVector = new Vector();
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value11" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value21", "value22" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value31", "value32",
                "value33" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value41", "value42",
                "value43", "value44" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value51", "value52",
                "value53", "value54", "value55" })));
        listener.reset();
        model.moveRow(0, 1, 1);
        assertEquals(5, model.getDataVector().size());
        assertEquals(3, ((Vector) model.dataVector.get(0)).size());
        assertEquals(1, ((Vector) model.dataVector.get(1)).size());
        assertEquals(2, ((Vector) model.dataVector.get(2)).size());
        assertEquals(4, ((Vector) model.dataVector.get(3)).size());
        assertEquals(5, ((Vector) model.dataVector.get(4)).size());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(0, listener.getEvent().getFirstRow());
        assertEquals(2, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
        listener.reset();
        model.moveRow(1, 1, 2);
        assertEquals(5, model.getDataVector().size());
        assertEquals(3, ((Vector) model.dataVector.get(0)).size());
        assertEquals(2, ((Vector) model.dataVector.get(1)).size());
        assertEquals(1, ((Vector) model.dataVector.get(2)).size());
        assertEquals(4, ((Vector) model.dataVector.get(3)).size());
        assertEquals(5, ((Vector) model.dataVector.get(4)).size());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(1, listener.getEvent().getFirstRow());
        assertEquals(2, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
        listener.reset();
        model.moveRow(2, 2, 0);
        assertEquals(5, model.getDataVector().size());
        assertEquals(1, ((Vector) model.dataVector.get(0)).size());
        assertEquals(3, ((Vector) model.dataVector.get(1)).size());
        assertEquals(2, ((Vector) model.dataVector.get(2)).size());
        assertEquals(4, ((Vector) model.dataVector.get(3)).size());
        assertEquals(5, ((Vector) model.dataVector.get(4)).size());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(0, listener.getEvent().getFirstRow());
        assertEquals(2, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
        listener.reset();
        model.moveRow(0, 3, 1);
        assertEquals(5, model.getDataVector().size());
        assertEquals(5, ((Vector) model.dataVector.get(0)).size());
        assertEquals(1, ((Vector) model.dataVector.get(1)).size());
        assertEquals(3, ((Vector) model.dataVector.get(2)).size());
        assertEquals(2, ((Vector) model.dataVector.get(3)).size());
        assertEquals(4, ((Vector) model.dataVector.get(4)).size());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(0, listener.getEvent().getFirstRow());
        assertEquals(4, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
        listener.reset();
        model.moveRow(2, 4, 1);
        assertEquals(5, model.getDataVector().size());
        assertEquals(5, ((Vector) model.dataVector.get(0)).size());
        assertEquals(3, ((Vector) model.dataVector.get(1)).size());
        assertEquals(2, ((Vector) model.dataVector.get(2)).size());
        assertEquals(4, ((Vector) model.dataVector.get(3)).size());
        assertEquals(1, ((Vector) model.dataVector.get(4)).size());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(1, listener.getEvent().getFirstRow());
        assertEquals(4, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
        testExceptionalCase(new ArrayIndexOutOfBoundsExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.moveRow(-1, 2, 0);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.moveRow(0, 3, 2);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.moveRow(1, 2, 4);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.moveRow(1, 7, 0);
            }
        });
    }

    public void testRemoveRow() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.columnIdentifiers = new Vector(Arrays.asList(new Object[] { "col1", "col2" }));
        model.dataVector = new Vector();
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value11", "value12" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value21", "value22",
                "value23" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value31" })));
        listener.reset();
        model.removeRow(1);
        assertEquals(2, model.getDataVector().size());
        assertEquals(2, ((Vector) model.dataVector.get(0)).size());
        assertEquals(1, ((Vector) model.dataVector.get(1)).size());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(1, listener.getEvent().getFirstRow());
        assertEquals(1, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.DELETE, listener.getEvent().getType());
        testExceptionalCase(new ArrayIndexOutOfBoundsExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.removeRow(-1);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.removeRow(3);
            }
        });
    }

    public void testSetColumnIdentifiers() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.columnIdentifiers = new Vector(Arrays.asList(new Object[] { "col1", "col2" }));
        model.dataVector = new Vector();
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value11", "value12" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value21", "value22",
                "value23" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value31" })));
        listener.reset();
        model.setColumnIdentifiers(new Vector(Arrays.asList(new Object[] { "col1", "col2" })));
        assertEquals(2, model.columnIdentifiers.size());
        assertEquals(3, model.getDataVector().size());
        assertEquals(2, ((Vector) model.dataVector.get(0)).size());
        assertEquals(2, ((Vector) model.dataVector.get(1)).size());
        assertEquals(2, ((Vector) model.dataVector.get(2)).size());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getFirstRow());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
        listener.reset();
        model.setColumnIdentifiers(new Vector(Arrays.asList(new Object[] { "col1", "col2",
                "col3" })));
        assertEquals(3, model.columnIdentifiers.size());
        assertEquals(3, model.getDataVector().size());
        assertEquals(3, ((Vector) model.dataVector.get(0)).size());
        assertEquals(3, ((Vector) model.dataVector.get(1)).size());
        assertEquals(3, ((Vector) model.dataVector.get(2)).size());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getFirstRow());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
        listener.reset();
        model.setColumnIdentifiers(new Object[] { "col1" });
        assertEquals(1, model.columnIdentifiers.size());
        assertEquals(3, model.getDataVector().size());
        assertEquals(1, ((Vector) model.dataVector.get(0)).size());
        assertEquals(1, ((Vector) model.dataVector.get(1)).size());
        assertEquals(1, ((Vector) model.dataVector.get(2)).size());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getFirstRow());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
        listener.reset();
        model.setColumnIdentifiers(new Object[] { "col1" });
        assertEquals(1, model.columnIdentifiers.size());
        assertEquals(3, model.getDataVector().size());
        assertEquals(1, ((Vector) model.dataVector.get(0)).size());
        assertEquals(1, ((Vector) model.dataVector.get(1)).size());
        assertEquals(1, ((Vector) model.dataVector.get(2)).size());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getFirstRow());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
    }

    public void testGetSetColumnCount() throws Exception {
        assertEquals(0, model.getColumnCount());
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.columnIdentifiers = new Vector(Arrays.asList(new Object[] { "col1", "col2" }));
        model.dataVector = new Vector();
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value11", "value12" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value21", "value22",
                "value23" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value31" })));
        listener.reset();
        model.setColumnCount(2);
        assertEquals(2, model.columnIdentifiers.size());
        assertEquals(2, model.getColumnCount());
        assertEquals(3, model.getDataVector().size());
        assertEquals(2, ((Vector) model.dataVector.get(0)).size());
        assertEquals(2, ((Vector) model.dataVector.get(1)).size());
        assertEquals(2, ((Vector) model.dataVector.get(2)).size());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getFirstRow());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
        listener.reset();
        model.setColumnCount(3);
        assertEquals(3, model.columnIdentifiers.size());
        assertEquals(3, model.getColumnCount());
        assertEquals(3, model.getDataVector().size());
        assertEquals(3, ((Vector) model.dataVector.get(0)).size());
        assertEquals(3, ((Vector) model.dataVector.get(1)).size());
        assertEquals(3, ((Vector) model.dataVector.get(2)).size());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getFirstRow());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
        listener.reset();
        model.setColumnCount(1);
        assertEquals(1, model.columnIdentifiers.size());
        assertEquals(1, model.getColumnCount());
        assertEquals(3, model.getDataVector().size());
        assertEquals(1, ((Vector) model.dataVector.get(0)).size());
        assertEquals(1, ((Vector) model.dataVector.get(1)).size());
        assertEquals(1, ((Vector) model.dataVector.get(2)).size());
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getFirstRow());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
    }

    public void testAddColumn() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.columnIdentifiers = new Vector(Arrays.asList(new Object[] { "col1", "col2" }));
        model.dataVector = new Vector();
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value11", "value12" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value21", "value22",
                "value23" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value31" })));
        listener.reset();
        model.addColumn(null, new Object[] { "val13", "val23" });
        assertEquals(3, model.columnIdentifiers.size());
        assertEquals(3, model.getDataVector().size());
        assertEquals(3, ((Vector) model.dataVector.get(0)).size());
        assertEquals(3, ((Vector) model.dataVector.get(1)).size());
        assertEquals(3, ((Vector) model.dataVector.get(2)).size());
        assertNull(model.columnIdentifiers.get(2));
        assertEquals("val13", ((Vector) model.dataVector.get(0)).get(2));
        assertEquals("val23", ((Vector) model.dataVector.get(1)).get(2));
        assertNull(((Vector) model.dataVector.get(2)).get(2));
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getFirstRow());
        assertEquals(TableModelEvent.HEADER_ROW, listener.getEvent().getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
        listener.reset();
        model.addColumn("col4");
        assertEquals(4, model.columnIdentifiers.size());
        assertEquals(3, model.getDataVector().size());
        assertEquals(4, ((Vector) model.dataVector.get(0)).size());
        assertEquals(4, ((Vector) model.dataVector.get(1)).size());
        assertEquals(4, ((Vector) model.dataVector.get(2)).size());
        assertEquals("col4", model.columnIdentifiers.get(3));
        assertTrue(listener.eventOccured());
    }

    public void testGetColumnName() throws Exception {
        assertEquals("A", model.getColumnName(0));
        assertEquals("B", model.getColumnName(1));
        model.setColumnCount(2);
        assertEquals("A", model.getColumnName(0));
        assertEquals("B", model.getColumnName(1));
        model.setColumnIdentifiers(new Object[] { "col1", null, "col3", new Integer(4) });
        assertEquals("col1", model.getColumnName(0));
        assertEquals("B", model.getColumnName(1));
        assertEquals("col3", model.getColumnName(2));
        assertEquals("4", model.getColumnName(3));
        assertEquals("E", model.getColumnName(4));
        testExceptionalCase(new ArrayIndexOutOfBoundsExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.getColumnName(-1);
            }
        });
    }

    public void testIsCellEditable() throws Exception {
        assertTrue(model.isCellEditable(2, 4));
    }

    public void testGetSetValueAt() throws Exception {
        TestTableModelListener listener = new TestTableModelListener();
        model.addTableModelListener(listener);
        model.columnIdentifiers = new Vector(Arrays.asList(new Object[] { "col1", "col2" }));
        model.dataVector = new Vector();
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value11", "value12" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value21", "value22",
                "value23" })));
        model.dataVector.add(new Vector(Arrays.asList(new Object[] { "value31" })));
        assertEquals("value22", model.getValueAt(1, 1));
        assertEquals("value31", model.getValueAt(2, 0));
        testExceptionalCase(new ArrayIndexOutOfBoundsExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.getValueAt(2, 1);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                model.setValueAt("value32", 2, 1);
            }
        });
        listener.reset();
        model.setValueAt(new Integer(3), 1, 0);
        assertEquals(new Integer(3), model.getValueAt(1, 0));
        assertTrue(listener.eventOccured());
        assertEquals(model, listener.getEvent().getSource());
        assertEquals(1, listener.getEvent().getFirstRow());
        assertEquals(1, listener.getEvent().getLastRow());
        assertEquals(0, listener.getEvent().getColumn());
        assertEquals(TableModelEvent.UPDATE, listener.getEvent().getType());
    }

    public void testConvertToVector() throws Exception {
        Object[] array = new Object[] { "1", new Integer(6), new String[] { "31", "32" } };
        assertEquals(new Vector(Arrays.asList(array)), DefaultTableModel.convertToVector(array));
        Object[][] arrayOfArray = new Object[][] { new Object[] { "1" },
                new Object[] { new Integer(6), new Character('c') },
                new String[] { "31", "32" } };
        Vector expected = new Vector();
        expected.add(Arrays.asList(new Object[] { "1" }));
        expected.add(Arrays.asList(new Object[] { new Integer(6), new Character('c') }));
        expected.add(Arrays.asList(new String[] { "31", "32" }));
        assertEquals(expected, DefaultTableModel.convertToVector(arrayOfArray));
    }
}
