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
package javax.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.plaf.TableUI;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

@SuppressWarnings("serial")
public class JTableTest extends BasicSwingTestCase {
    private JTable table;

    public JTableTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        table = new JTable();
        propertyChangeController = new PropertyChangeController();
        table.addPropertyChangeListener(propertyChangeController);
    }

    @Override
    protected void tearDown() throws Exception {
        table = null;
    }

    public void testJTable() throws Exception {
        assertSame(DefaultTableModel.class, table.dataModel.getClass());
        assertSame(DefaultTableColumnModel.class, table.columnModel.getClass());
        assertSame(DefaultListSelectionModel.class, table.selectionModel.getClass());
        assertSame(JTableHeader.class, table.tableHeader.getClass());
        assertEquals(3, table.defaultEditorsByColumnClass.size());
        assertEquals(8, table.defaultRenderersByColumnClass.size());
        DefaultTableModel model = new DefaultTableModel();
        table = new JTable(model);
        assertSame(model, table.dataModel);
        assertSame(DefaultTableColumnModel.class, table.columnModel.getClass());
        assertSame(DefaultListSelectionModel.class, table.selectionModel.getClass());
        DefaultTableColumnModel columnModel = new DefaultTableColumnModel();
        table = new JTable(model, columnModel);
        assertSame(model, table.dataModel);
        assertSame(columnModel, table.columnModel);
        assertSame(DefaultListSelectionModel.class, table.selectionModel.getClass());
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        table = new JTable(model, columnModel, selectionModel);
        assertSame(model, table.dataModel);
        assertSame(columnModel, table.columnModel);
        assertSame(selectionModel, table.selectionModel);
        table = new JTable(null, columnModel, null);
        assertSame(DefaultTableModel.class, table.dataModel.getClass());
        assertSame(columnModel, table.columnModel);
        assertSame(DefaultListSelectionModel.class, table.selectionModel.getClass());
        table = new JTable(3, 4);
        assertSame(DefaultTableModel.class, table.dataModel.getClass());
        assertSame(DefaultTableColumnModel.class, table.columnModel.getClass());
        assertSame(DefaultListSelectionModel.class, table.selectionModel.getClass());
        assertEquals(3, table.dataModel.getRowCount());
        assertEquals(4, table.dataModel.getColumnCount());
        assertEquals(4, table.columnModel.getColumnCount());
    }

    public void testAddNotify() throws Exception {
        final Marker marker = new Marker();
        new JTable() {
            @Override
            protected void configureEnclosingScrollPane() {
                marker.setOccurred();
            }
        }.addNotify();
        assertTrue(marker.isOccurred());
    }

    public void testRemoveNotify() throws Exception {
        final Marker marker = new Marker();
        new JTable() {
            @Override
            protected void unconfigureEnclosingScrollPane() {
                marker.setOccurred();
            }
        }.removeNotify();
        assertTrue(marker.isOccurred());
    }

    public void testConfigureUnconfigureEnclosingScrollPane() throws Exception {
        JScrollPane pane = new JScrollPane();
        pane.setViewportView(table);
        assertNull(pane.getColumnHeader());
        table.addNotify();
        assertNotNull(pane.getColumnHeader());
        assertSame(table.getTableHeader(), pane.getColumnHeader().getView());
        table.removeNotify();
        assertNull(pane.getColumnHeader().getView());
    }

    public void testGetSetTableHeader() throws Exception {
        assertNotNull(table.getTableHeader());
        assertEquals(table.tableHeader, table.getTableHeader());
        assertEquals(table, table.getTableHeader().getTable());
        assertEquals(table.getColumnModel(), table.getTableHeader().getColumnModel());
        JTableHeader oldValue = table.getTableHeader();
        JTableHeader header = new JTableHeader();
        table.setTableHeader(header);
        assertEquals(table.tableHeader, table.getTableHeader());
        assertEquals(table, table.getTableHeader().getTable());
        assertNull(oldValue.getTable());
        assertNotSame(table.getColumnModel(), table.getTableHeader().getColumnModel());
        assertTrue(propertyChangeController.isChanged("tableHeader"));
        table.setTableHeader(null);
        assertNull(table.getTableHeader());
    }

    public void testGetSetRowHeight() throws Exception {
        assertEquals(16, table.rowHeight);
        assertEquals(16, table.getRowHeight());
        table.setRowHeight(30);
        assertEquals(30, table.getRowHeight());
        assertEquals(30, table.getRowHeight(10));
        assertTrue(propertyChangeController.isChanged("rowHeight"));
        propertyChangeController.reset();
        table.setRowHeight(10, 50);
        assertEquals(30, table.getRowHeight());
        if (isHarmony()) {
            assertEquals(30, table.getRowHeight(10));
        } else {
            assertEquals(0, table.getRowHeight(10));
        }
        assertFalse(propertyChangeController.isChanged("rowHeight"));
        table = new JTable(3, 4);
        table.setRowHeight(2, 50);
        assertEquals(16, table.getRowHeight());
        assertEquals(16, table.getRowHeight(0));
        assertEquals(50, table.getRowHeight(2));
        if (isHarmony()) {
            assertEquals(16, table.getRowHeight(10));
        } else {
            assertEquals(0, table.getRowHeight(10));
        }
        table.setRowHeight(20, 50);
        if (isHarmony()) {
            assertEquals(16, table.getRowHeight(20));
        } else {
            assertEquals(0, table.getRowHeight(20));
        }
        table.setRowHeight(3, 25);
        if (isHarmony()) {
            assertEquals(16, table.getRowHeight(3));
        } else {
            assertEquals(0, table.getRowHeight(3));
        }
        table.setRowHeight(40);
        ((DefaultTableModel) table.getModel()).addRow(new Object[] { "3", "3" });
        assertEquals(40, table.getRowHeight(3));
        table.setRowHeight(3, 25);
        assertEquals(25, table.getRowHeight(3));
        ((DefaultTableModel) table.getModel()).removeRow(3);
        ((DefaultTableModel) table.getModel()).addRow(new Object[] { "3", "3" });
        if (isHarmony()) {
            assertEquals(25, table.getRowHeight(3));
        } else {
            assertEquals(40, table.getRowHeight(3));
        }
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.setRowHeight(0);
            }
        });
    }

    public void testGetSetRowMargin() throws Exception {
        assertEquals(1, table.getRowMargin());
        assertEquals(1, table.getIntercellSpacing().height);
        table.setRowMargin(10);
        assertEquals(10, table.getRowMargin());
        assertEquals(10, table.getIntercellSpacing().height);
        assertTrue(propertyChangeController.isChanged("rowMargin"));
        table.setRowMargin(-5);
        assertEquals(-5, table.getRowMargin());
        assertEquals(-5, table.getIntercellSpacing().height);
    }

    public void testGetSetIntercellSpacing() throws Exception {
        assertEquals(new Dimension(1, 1), table.getIntercellSpacing());
        assertNotSame(table.getIntercellSpacing(), table.getIntercellSpacing());
        table.setRowMargin(10);
        assertEquals(new Dimension(1, 10), table.getIntercellSpacing());
        table.getColumnModel().setColumnMargin(5);
        assertEquals(new Dimension(5, 10), table.getIntercellSpacing());
        propertyChangeController.reset();
        Dimension spacing = new Dimension(3, 4);
        table.setIntercellSpacing(spacing);
        assertNotSame(spacing, table.getIntercellSpacing());
        assertEquals(spacing, table.getIntercellSpacing());
        assertEquals(4, table.getRowMargin());
        assertEquals(3, table.getColumnModel().getColumnMargin());
        assertTrue(propertyChangeController.isChanged("rowMargin"));
    }

    public void testGetSetGridColor() throws Exception {
        assertEquals(UIManager.getColor("Table.gridColor"), table.getGridColor());
        table.setGridColor(Color.RED);
        assertEquals(Color.RED, table.getGridColor());
        assertTrue(propertyChangeController.isChanged("gridColor"));
    }

    public void testSetShowGrid() throws Exception {
        assertTrue(table.getShowHorizontalLines());
        assertTrue(table.getShowVerticalLines());
        table.setShowGrid(false);
        assertFalse(table.getShowHorizontalLines());
        assertFalse(table.getShowVerticalLines());
        assertTrue(propertyChangeController.isChanged("showHorizontalLines"));
        assertTrue(propertyChangeController.isChanged("showVerticalLines"));
        table.setShowGrid(true);
        assertTrue(table.getShowHorizontalLines());
        assertTrue(table.getShowVerticalLines());
    }

    public void testGetSetShowHorizontalLines() throws Exception {
        assertTrue(table.getShowHorizontalLines());
        table.setShowHorizontalLines(false);
        assertFalse(table.getShowHorizontalLines());
        assertTrue(propertyChangeController.isChanged("showHorizontalLines"));
        table.setShowHorizontalLines(true);
        assertTrue(table.getShowHorizontalLines());
    }

    public void testGetSetShowVerticalLines() throws Exception {
        assertTrue(table.getShowVerticalLines());
        table.setShowVerticalLines(false);
        assertFalse(table.getShowVerticalLines());
        assertTrue(propertyChangeController.isChanged("showVerticalLines"));
        table.setShowVerticalLines(true);
        assertTrue(table.getShowVerticalLines());
    }

    public void testGetSetAutoResizeMode() throws Exception {
        assertEquals(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS, table.getAutoResizeMode());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        assertEquals(JTable.AUTO_RESIZE_ALL_COLUMNS, table.getAutoResizeMode());
        assertTrue(propertyChangeController.isChanged("autoResizeMode"));
        propertyChangeController.reset();
        table.setAutoResizeMode(20);
        assertEquals(JTable.AUTO_RESIZE_ALL_COLUMNS, table.getAutoResizeMode());
        assertFalse(propertyChangeController.isChanged());
    }

    public void testGetSetAutoCreateColumnsFromModel() throws Exception {
        assertTrue(table.getAutoCreateColumnsFromModel());
        table.setAutoCreateColumnsFromModel(false);
        assertFalse(table.getAutoCreateColumnsFromModel());
        assertTrue(propertyChangeController.isChanged("autoCreateColumnsFromModel"));
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addColumn("column1");
        assertEquals(0, table.getColumnModel().getColumnCount());
        table.setAutoCreateColumnsFromModel(true);
        assertEquals(1, table.getColumnModel().getColumnCount());
        model.addColumn("column2");
        assertEquals(2, table.getColumnModel().getColumnCount());
        table.setAutoCreateColumnsFromModel(false);
        model.addColumn("column3");
        assertEquals(2, table.getColumnModel().getColumnCount());
    }

    public void testCreateDefaultColumnsFromModel() throws Exception {
        TableColumnModel columnModel = table.getColumnModel();
        assertEquals(0, columnModel.getColumnCount());
        TableColumn modelColumn1 = new TableColumn();
        modelColumn1.setIdentifier("modelColumn1");
        columnModel.addColumn(modelColumn1);
        assertEquals(1, columnModel.getColumnCount());
        table.setAutoCreateColumnsFromModel(false);
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addColumn("column1");
        model.addColumn("column2");
        assertEquals(1, columnModel.getColumnCount());
        table.createDefaultColumnsFromModel();
        assertEquals(2, columnModel.getColumnCount());
        assertEquals("column1", columnModel.getColumn(0).getIdentifier());
        assertEquals("column2", columnModel.getColumn(1).getIdentifier());
        model.addColumn("column3");
        model.addColumn("column4");
        assertEquals(2, columnModel.getColumnCount());
        table.setAutoCreateColumnsFromModel(true);
        assertEquals(4, columnModel.getColumnCount());
    }

    public void testGetSetDefaultRenderer() throws Exception {
        assertEquals(8, table.defaultRenderersByColumnClass.size());
        assertNotNull(table.getDefaultRenderer(String.class));
        assertSame(table.defaultRenderersByColumnClass.get(Object.class), table
                .getDefaultRenderer(String.class));
        DefaultTableCellRenderer stringRenderer = new DefaultTableCellRenderer();
        table.setDefaultRenderer(String.class, stringRenderer);
        DefaultTableCellRenderer objectRenderer = new DefaultTableCellRenderer();
        table.setDefaultRenderer(Object.class, objectRenderer);
        assertEquals(9, table.defaultRenderersByColumnClass.size());
        assertSame(stringRenderer, table.getDefaultRenderer(String.class));
        assertSame(objectRenderer, table.getDefaultRenderer(JTable.class));
        table.setDefaultRenderer(Object.class, null);
        assertEquals(8, table.defaultRenderersByColumnClass.size());
        assertNull(table.getDefaultRenderer(JTable.class));
        DefaultTableCellRenderer actionRenderer = new DefaultTableCellRenderer();
        table.setDefaultRenderer(Action.class, actionRenderer);
        assertSame(actionRenderer, table.getDefaultRenderer(Action.class));
        assertNull(table.getDefaultRenderer(AbstractAction.class));
    }

    public void testGetSetDefaultEditor() throws Exception {
        assertEquals(3, table.defaultEditorsByColumnClass.size());
        assertNotNull(table.getDefaultEditor(String.class));
        assertSame(table.defaultEditorsByColumnClass.get(Object.class), table
                .getDefaultEditor(String.class));
        DefaultCellEditor componentEditor = new DefaultCellEditor(new JTextField());
        table.setDefaultEditor(JComponent.class, componentEditor);
        DefaultCellEditor booleanEditor = new DefaultCellEditor(new JCheckBox());
        table.setDefaultEditor(Boolean.class, booleanEditor);
        assertEquals(4, table.defaultEditorsByColumnClass.size());
        assertSame(componentEditor, table.getDefaultEditor(JTable.class));
        assertSame(booleanEditor, table.getDefaultEditor(Boolean.class));
        table.setDefaultEditor(Boolean.class, null);
        assertEquals(3, table.defaultEditorsByColumnClass.size());
        assertNotNull(table.getDefaultEditor(Boolean.class));
        assertNotNull(table.getDefaultEditor(Math.class));
    }

    public void testGetSetDragEnabled() throws Exception {
        assertFalse(table.getDragEnabled());
        table.setDragEnabled(true);
        assertTrue(table.getDragEnabled());
    }

    public void testSetSelectionMode() throws Exception {
        assertEquals(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, table.getSelectionModel()
                .getSelectionMode());
        assertEquals(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, table.getColumnModel()
                .getSelectionModel().getSelectionMode());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        assertEquals(ListSelectionModel.SINGLE_SELECTION, table.getSelectionModel()
                .getSelectionMode());
        assertEquals(ListSelectionModel.SINGLE_SELECTION, table.getColumnModel()
                .getSelectionModel().getSelectionMode());
        assertFalse(propertyChangeController.isChanged());
    }

    public void testGetSetRowSelectionAllowed() throws Exception {
        assertTrue(table.getRowSelectionAllowed());
        table.setRowSelectionAllowed(false);
        assertFalse(table.getRowSelectionAllowed());
        assertTrue(propertyChangeController.isChanged("rowSelectionAllowed"));
    }

    public void testGetSetColumnSelectionAllowed() throws Exception {
        assertFalse(table.getColumnSelectionAllowed());
        assertFalse(table.getColumnModel().getColumnSelectionAllowed());
        table.setColumnSelectionAllowed(true);
        assertTrue(table.getColumnSelectionAllowed());
        assertTrue(table.getColumnModel().getColumnSelectionAllowed());
        assertTrue(propertyChangeController.isChanged("columnSelectionAllowed"));
    }

    public void testGetSetCellSelectionEnabled() throws Exception {
        assertFalse(table.getCellSelectionEnabled());
        assertFalse(table.cellSelectionEnabled);
        table.setCellSelectionEnabled(false);
        assertFalse(table.cellSelectionEnabled);
        assertFalse(table.getCellSelectionEnabled());
        assertFalse(table.getRowSelectionAllowed());
        assertFalse(table.getColumnSelectionAllowed());
        assertFalse(propertyChangeController.isChanged("cellSelectionEnabled"));
        propertyChangeController.reset();
        table.setCellSelectionEnabled(true);
        assertTrue(table.cellSelectionEnabled);
        assertTrue(table.getCellSelectionEnabled());
        assertTrue(table.getRowSelectionAllowed());
        assertTrue(table.getColumnSelectionAllowed());
        assertTrue(propertyChangeController.isChanged("cellSelectionEnabled"));
        table.setRowSelectionAllowed(false);
        assertFalse(table.getCellSelectionEnabled());
        assertTrue(table.cellSelectionEnabled);
    }

    public void testSelectAllClearSelection() throws Exception {
        table = new JTable(3, 4);
        assertEquals(0, getSelectedIndices(table.getSelectionModel()).length);
        assertEquals(0, getSelectedIndices(table.getColumnModel().getSelectionModel()).length);
        table.selectAll();
        assertEquals(3, getSelectedIndices(table.getSelectionModel()).length);
        assertEquals(4, getSelectedIndices(table.getColumnModel().getSelectionModel()).length);
        table.clearSelection();
        assertEquals(0, getSelectedIndices(table.getSelectionModel()).length);
        assertEquals(0, getSelectedIndices(table.getColumnModel().getSelectionModel()).length);
        table.setCellSelectionEnabled(false);
        table.selectAll();
        assertEquals(3, getSelectedIndices(table.getSelectionModel()).length);
        assertEquals(4, getSelectedIndices(table.getColumnModel().getSelectionModel()).length);
    }

    public void testSetRowSelectionInterval() throws Exception {
        table = new JTable(3, 4);
        assertEquals(0, getSelectedIndices(table.getSelectionModel()).length);
        table.setRowSelectionInterval(1, 2);
        assertEquals(2, getSelectedIndices(table.getSelectionModel()).length);
        assertEquals(1, getSelectedIndices(table.getSelectionModel())[0]);
        assertEquals(2, getSelectedIndices(table.getSelectionModel())[1]);
        table.clearSelection();
        table.setRowSelectionAllowed(false);
        table.setRowSelectionInterval(1, 1);
        assertEquals(1, getSelectedIndices(table.getSelectionModel()).length);
        assertEquals(1, getSelectedIndices(table.getSelectionModel())[0]);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.setRowSelectionInterval(0, 3);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.setRowSelectionInterval(-1, 1);
            }
        });
    }

    public void testSetColumnSelectionInterval() throws Exception {
        table = new JTable(3, 4);
        assertEquals(0, getSelectedIndices(table.getColumnModel().getSelectionModel()).length);
        table.setColumnSelectionInterval(1, 2);
        assertEquals(2, getSelectedIndices(table.getColumnModel().getSelectionModel()).length);
        assertEquals(1, getSelectedIndices(table.getColumnModel().getSelectionModel())[0]);
        assertEquals(2, getSelectedIndices(table.getColumnModel().getSelectionModel())[1]);
        table.clearSelection();
        table.setColumnSelectionAllowed(false);
        table.setColumnSelectionInterval(1, 1);
        assertEquals(1, getSelectedIndices(table.getColumnModel().getSelectionModel()).length);
        assertEquals(1, getSelectedIndices(table.getColumnModel().getSelectionModel())[0]);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.setColumnSelectionInterval(0, 4);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.setColumnSelectionInterval(-1, 1);
            }
        });
    }

    public void testAddRemoveRowSelectionInterval() throws Exception {
        table = new JTable(3, 4);
        assertEquals(0, getSelectedIndices(table.getSelectionModel()).length);
        table.addRowSelectionInterval(0, 0);
        assertEquals(1, getSelectedIndices(table.getSelectionModel()).length);
        assertEquals(0, getSelectedIndices(table.getSelectionModel())[0]);
        table.addRowSelectionInterval(2, 2);
        assertEquals(2, getSelectedIndices(table.getSelectionModel()).length);
        assertEquals(0, getSelectedIndices(table.getSelectionModel())[0]);
        assertEquals(2, getSelectedIndices(table.getSelectionModel())[1]);
        table.removeRowSelectionInterval(2, 1);
        assertEquals(1, getSelectedIndices(table.getSelectionModel()).length);
        assertEquals(0, getSelectedIndices(table.getSelectionModel())[0]);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.addRowSelectionInterval(4, 4);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.addRowSelectionInterval(-1, 1);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.removeRowSelectionInterval(-1, 1);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.removeRowSelectionInterval(4, 4);
            }
        });
    }

    public void testAddRemoveColumnSelectionInterval() throws Exception {
        table = new JTable(3, 4);
        assertEquals(0, getSelectedIndices(table.getColumnModel().getSelectionModel()).length);
        table.addColumnSelectionInterval(0, 0);
        assertEquals(1, getSelectedIndices(table.getColumnModel().getSelectionModel()).length);
        assertEquals(0, getSelectedIndices(table.getColumnModel().getSelectionModel())[0]);
        table.addColumnSelectionInterval(2, 3);
        assertEquals(3, getSelectedIndices(table.getColumnModel().getSelectionModel()).length);
        assertEquals(0, getSelectedIndices(table.getColumnModel().getSelectionModel())[0]);
        assertEquals(2, getSelectedIndices(table.getColumnModel().getSelectionModel())[1]);
        assertEquals(3, getSelectedIndices(table.getColumnModel().getSelectionModel())[2]);
        table.removeColumnSelectionInterval(3, 3);
        assertEquals(2, getSelectedIndices(table.getColumnModel().getSelectionModel()).length);
        assertEquals(0, getSelectedIndices(table.getColumnModel().getSelectionModel())[0]);
        assertEquals(2, getSelectedIndices(table.getColumnModel().getSelectionModel())[1]);
        table.removeColumnSelectionInterval(1, 2);
        assertEquals(1, getSelectedIndices(table.getColumnModel().getSelectionModel()).length);
        assertEquals(0, getSelectedIndices(table.getColumnModel().getSelectionModel())[0]);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.addColumnSelectionInterval(4, 4);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.addColumnSelectionInterval(-1, 1);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.removeColumnSelectionInterval(4, 4);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.removeColumnSelectionInterval(-1, 1);
            }
        });
    }

    public void testGetSelectedRow() throws Exception {
        table = new JTable(3, 4);
        assertEquals(-1, table.getSelectedRow());
        table.setRowSelectionInterval(1, 1);
        assertEquals(1, table.getSelectedRow());
        table.addRowSelectionInterval(2, 2);
        assertEquals(1, table.getSelectedRow());
    }

    public void testGetSelectedColumn() throws Exception {
        table = new JTable(3, 4);
        assertEquals(-1, table.getSelectedColumn());
        table.setColumnSelectionInterval(1, 1);
        assertEquals(1, table.getSelectedColumn());
        table.addColumnSelectionInterval(2, 3);
        assertEquals(1, table.getSelectedColumn());
    }

    public void testGetSelectedRows() throws Exception {
        table = new JTable(3, 4);
        assertEquals(0, table.getSelectedRows().length);
        table.setRowSelectionInterval(1, 1);
        assertEquals(1, table.getSelectedRows().length);
        assertEquals(1, table.getSelectedRows()[0]);
        table.addRowSelectionInterval(2, 2);
        assertEquals(2, table.getSelectedRows().length);
        assertEquals(1, table.getSelectedRows()[0]);
        assertEquals(2, table.getSelectedRows()[1]);
    }

    public void testGetSelectedColumns() throws Exception {
        table = new JTable(3, 4);
        assertEquals(0, table.getSelectedColumns().length);
        table.setColumnSelectionInterval(0, 1);
        assertEquals(2, table.getSelectedColumns().length);
        assertEquals(0, table.getSelectedColumns()[0]);
        assertEquals(1, table.getSelectedColumns()[1]);
        table.addColumnSelectionInterval(3, 3);
        assertEquals(3, table.getSelectedColumns().length);
        assertEquals(0, table.getSelectedColumns()[0]);
        assertEquals(1, table.getSelectedColumns()[1]);
        assertEquals(3, table.getSelectedColumns()[2]);
    }

    public void testGetSelectedRowCount() throws Exception {
        table = new JTable(3, 4);
        assertEquals(0, table.getSelectedRowCount());
        table.setRowSelectionInterval(1, 1);
        assertEquals(1, table.getSelectedRowCount());
        table.addRowSelectionInterval(2, 2);
        assertEquals(2, table.getSelectedRowCount());
    }

    public void testGetSelectedColumnCount() throws Exception {
        table = new JTable(3, 4);
        assertEquals(0, table.getSelectedColumnCount());
        table.setColumnSelectionInterval(0, 1);
        assertEquals(2, table.getSelectedColumnCount());
        table.addColumnSelectionInterval(3, 3);
        assertEquals(3, table.getSelectedColumnCount());
    }

    public void testIsRowSelected() throws Exception {
        table = new JTable(3, 4);
        assertFalse(table.isRowSelected(0));
        assertFalse(table.isRowSelected(1));
        assertFalse(table.isRowSelected(2));
        table.addRowSelectionInterval(0, 1);
        assertTrue(table.isRowSelected(0));
        assertTrue(table.isRowSelected(1));
        assertFalse(table.isRowSelected(2));
        assertFalse(table.isRowSelected(-1));
        assertFalse(table.isRowSelected(4));
    }

    public void testIsColumnSelected() throws Exception {
        table = new JTable(3, 4);
        assertFalse(table.isColumnSelected(0));
        assertFalse(table.isColumnSelected(1));
        assertFalse(table.isColumnSelected(2));
        assertFalse(table.isColumnSelected(3));
        table.addColumnSelectionInterval(0, 1);
        table.addColumnSelectionInterval(3, 3);
        assertTrue(table.isColumnSelected(0));
        assertTrue(table.isColumnSelected(1));
        assertFalse(table.isColumnSelected(2));
        assertTrue(table.isColumnSelected(3));
        assertFalse(table.isColumnSelected(-1));
        assertFalse(table.isColumnSelected(5));
    }

    public void testIsCellSelected() throws Exception {
        table = new JTable(3, 4);
        assertTrue(table.getRowSelectionAllowed());
        assertFalse(table.getColumnSelectionAllowed());
        assertFalse(table.isCellSelected(0, 0));
        assertFalse(table.isCellSelected(0, 1));
        assertFalse(table.isCellSelected(0, 2));
        assertFalse(table.isCellSelected(0, 3));
        assertFalse(table.isCellSelected(1, 0));
        assertFalse(table.isCellSelected(1, 1));
        assertFalse(table.isCellSelected(1, 2));
        assertFalse(table.isCellSelected(1, 3));
        assertFalse(table.isCellSelected(2, 0));
        assertFalse(table.isCellSelected(2, 1));
        assertFalse(table.isCellSelected(2, 2));
        assertFalse(table.isCellSelected(2, 3));
        table.addRowSelectionInterval(0, 0);
        table.addRowSelectionInterval(2, 2);
        assertTrue(table.isCellSelected(0, 0));
        assertTrue(table.isCellSelected(0, 1));
        assertTrue(table.isCellSelected(0, 2));
        assertTrue(table.isCellSelected(0, 3));
        assertFalse(table.isCellSelected(1, 0));
        assertFalse(table.isCellSelected(1, 1));
        assertFalse(table.isCellSelected(1, 2));
        assertFalse(table.isCellSelected(1, 3));
        assertTrue(table.isCellSelected(2, 0));
        assertTrue(table.isCellSelected(2, 1));
        assertTrue(table.isCellSelected(2, 2));
        assertTrue(table.isCellSelected(2, 3));
        table.setCellSelectionEnabled(true);
        assertFalse(table.isCellSelected(0, 0));
        assertFalse(table.isCellSelected(0, 1));
        assertFalse(table.isCellSelected(0, 2));
        assertFalse(table.isCellSelected(0, 3));
        assertFalse(table.isCellSelected(1, 0));
        assertFalse(table.isCellSelected(1, 1));
        assertFalse(table.isCellSelected(1, 2));
        assertFalse(table.isCellSelected(1, 3));
        assertFalse(table.isCellSelected(2, 0));
        assertFalse(table.isCellSelected(2, 1));
        assertFalse(table.isCellSelected(2, 2));
        assertFalse(table.isCellSelected(2, 3));
        table.addColumnSelectionInterval(1, 1);
        table.addColumnSelectionInterval(3, 3);
        assertFalse(table.isCellSelected(0, 0));
        assertTrue(table.isCellSelected(0, 1));
        assertFalse(table.isCellSelected(0, 2));
        assertTrue(table.isCellSelected(0, 3));
        assertFalse(table.isCellSelected(1, 0));
        assertFalse(table.isCellSelected(1, 1));
        assertFalse(table.isCellSelected(1, 2));
        assertFalse(table.isCellSelected(1, 3));
        assertFalse(table.isCellSelected(2, 0));
        assertTrue(table.isCellSelected(2, 1));
        assertFalse(table.isCellSelected(2, 2));
        assertTrue(table.isCellSelected(2, 3));
        table.setCellSelectionEnabled(false);
        assertFalse(table.isCellSelected(0, 0));
        assertFalse(table.isCellSelected(0, 1));
        assertFalse(table.isCellSelected(0, 2));
        assertFalse(table.isCellSelected(0, 3));
        assertFalse(table.isCellSelected(1, 0));
        assertFalse(table.isCellSelected(1, 1));
        assertFalse(table.isCellSelected(1, 2));
        assertFalse(table.isCellSelected(1, 3));
        assertFalse(table.isCellSelected(2, 0));
        assertFalse(table.isCellSelected(2, 1));
        assertFalse(table.isCellSelected(2, 2));
        assertFalse(table.isCellSelected(2, 3));
        table.setColumnSelectionAllowed(true);
        table.setRowSelectionAllowed(false);
        assertFalse(table.isCellSelected(0, 0));
        assertTrue(table.isCellSelected(0, 1));
        assertFalse(table.isCellSelected(0, 2));
        assertTrue(table.isCellSelected(0, 3));
        assertFalse(table.isCellSelected(1, 0));
        assertTrue(table.isCellSelected(1, 1));
        assertFalse(table.isCellSelected(1, 2));
        assertTrue(table.isCellSelected(1, 3));
        assertFalse(table.isCellSelected(2, 0));
        assertTrue(table.isCellSelected(2, 1));
        assertFalse(table.isCellSelected(2, 2));
        assertTrue(table.isCellSelected(2, 3));
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);
        assertTrue(table.isCellSelected(0, 0));
        assertTrue(table.isCellSelected(0, 1));
        assertTrue(table.isCellSelected(0, 2));
        assertTrue(table.isCellSelected(0, 3));
        assertFalse(table.isCellSelected(1, 0));
        assertFalse(table.isCellSelected(1, 1));
        assertFalse(table.isCellSelected(1, 2));
        assertFalse(table.isCellSelected(1, 3));
        assertTrue(table.isCellSelected(2, 0));
        assertTrue(table.isCellSelected(2, 1));
        assertTrue(table.isCellSelected(2, 2));
        assertTrue(table.isCellSelected(2, 3));
        table.setCellSelectionEnabled(true);
        assertFalse(table.isCellSelected(-1, 0));
        assertFalse(table.isCellSelected(0, -1));
        assertFalse(table.isCellSelected(0, 5));
        assertFalse(table.isCellSelected(4, 2));
    }

    public void testChangeSelection() throws Exception {
        table = new JTable(3, 4);
        table.setCellSelectionEnabled(true);
        table.changeSelection(1, 1, true, false);
        assertTrue(table.isCellSelected(1, 1));
        assertFalse(table.isCellSelected(1, 2));
        assertFalse(table.isCellSelected(2, 1));
        assertFalse(table.isCellSelected(2, 2));
        table.changeSelection(2, 2, true, false);
        assertTrue(table.isCellSelected(1, 1));
        assertTrue(table.isCellSelected(1, 2));
        assertTrue(table.isCellSelected(2, 1));
        assertTrue(table.isCellSelected(2, 2));
        assertEquals(2, table.getSelectionModel().getAnchorSelectionIndex());
        assertEquals(2, table.getColumnModel().getSelectionModel().getAnchorSelectionIndex());
        assertEquals(2, table.getSelectionModel().getLeadSelectionIndex());
        assertEquals(2, table.getColumnModel().getSelectionModel().getLeadSelectionIndex());
        table.changeSelection(2, 3, true, true);
        assertTrue(table.isCellSelected(1, 1));
        assertTrue(table.isCellSelected(1, 2));
        assertTrue(table.isCellSelected(2, 1));
        assertTrue(table.isCellSelected(2, 2));
        assertFalse(table.isCellSelected(2, 4));
        assertEquals(2, table.getSelectionModel().getLeadSelectionIndex());
        assertEquals(2, table.getColumnModel().getSelectionModel().getLeadSelectionIndex());
        assertEquals(2, table.getSelectionModel().getAnchorSelectionIndex());
        assertEquals(3, table.getColumnModel().getSelectionModel().getAnchorSelectionIndex());
        table.changeSelection(0, 3, false, true);
        assertFalse(table.isCellSelected(1, 1));
        assertFalse(table.isCellSelected(1, 2));
        assertFalse(table.isCellSelected(2, 1));
        assertFalse(table.isCellSelected(2, 2));
        assertTrue(table.isCellSelected(1, 3));
        assertTrue(table.isCellSelected(1, 3));
        assertTrue(table.isCellSelected(2, 3));
        assertTrue(table.isCellSelected(2, 3));
        assertFalse(table.isCellSelected(0, 4));
        assertFalse(table.isCellSelected(1, 4));
        assertFalse(table.isCellSelected(2, 4));
        table.changeSelection(1, 2, false, true);
        assertFalse(table.isCellSelected(1, 1));
        assertTrue(table.isCellSelected(1, 2));
        table.changeSelection(1, 1, true, false);
        assertTrue(table.isCellSelected(1, 1));
        table.changeSelection(1, 1, true, false);
        assertFalse(table.isCellSelected(1, 1));
        table.changeSelection(2, 3, false, false);
        assertTrue(table.isCellSelected(2, 3));
        assertEquals(1, table.getSelectedRowCount());
        assertEquals(1, table.getSelectedColumnCount());
    }

    public void testGetSetSelectionForegroundBackground() throws Exception {
        assertEquals(UIManager.getColor("Table.selectionForeground"), table
                .getSelectionForeground());
        assertEquals(UIManager.getColor("Table.selectionBackground"), table
                .getSelectionBackground());
        table.setSelectionForeground(Color.BLUE);
        assertTrue(propertyChangeController.isChanged("selectionForeground"));
        propertyChangeController.reset();
        table.setSelectionBackground(Color.RED);
        assertTrue(propertyChangeController.isChanged("selectionBackground"));
        assertEquals(Color.BLUE, table.getSelectionForeground());
        assertEquals(Color.RED, table.getSelectionBackground());
    }

    public void testGetColumn() throws Exception {
        table = new JTable(3, 4);
        assertNotNull(table.getColumn("A"));
        assertNotNull(table.getColumn("B"));
        assertNotNull(table.getColumn("C"));
        assertNotNull(table.getColumn("D"));
        table.getColumn("C").setIdentifier("ANY");
        assertNotNull(table.getColumn("ANY"));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                assertNull(table.getColumn("C"));
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                assertNull(table.getColumn("E"));
            }
        });
    }

    public void testConvertColumnIndexToModel() throws Exception {
        assertEquals(-5, table.convertColumnIndexToModel(-5));
        table = new JTable(3, 4);
        assertEquals(0, table.convertColumnIndexToModel(0));
        assertEquals(1, table.convertColumnIndexToModel(1));
        assertEquals(2, table.convertColumnIndexToModel(2));
        assertEquals(3, table.convertColumnIndexToModel(3));
        table.moveColumn(0, 2);
        assertEquals(1, table.convertColumnIndexToModel(0));
        assertEquals(2, table.convertColumnIndexToModel(1));
        assertEquals(0, table.convertColumnIndexToModel(2));
        assertEquals(3, table.convertColumnIndexToModel(3));
        table.getColumnModel().getColumn(0).setModelIndex(-5);
        table.getColumnModel().getColumn(1).setModelIndex(-5);
        table.getColumnModel().getColumn(2).setModelIndex(-5);
        table.getColumnModel().getColumn(3).setModelIndex(-5);
        assertEquals(-5, table.convertColumnIndexToModel(0));
        assertEquals(-5, table.convertColumnIndexToModel(1));
        assertEquals(-5, table.convertColumnIndexToModel(2));
        assertEquals(-5, table.convertColumnIndexToModel(3));
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.convertColumnIndexToModel(100);
            }
        });
    }

    public void testConvertColumnIndexToView() throws Exception {
        assertEquals(-5, table.convertColumnIndexToView(-5));
        assertEquals(-1, table.convertColumnIndexToView(10));
        table = new JTable(3, 4);
        assertEquals(0, table.convertColumnIndexToView(0));
        assertEquals(1, table.convertColumnIndexToView(1));
        assertEquals(2, table.convertColumnIndexToView(2));
        assertEquals(3, table.convertColumnIndexToView(3));
        table.moveColumn(0, 2);
        assertEquals(2, table.convertColumnIndexToView(0));
        assertEquals(0, table.convertColumnIndexToView(1));
        assertEquals(1, table.convertColumnIndexToView(2));
        assertEquals(3, table.convertColumnIndexToView(3));
        table.getColumnModel().getColumn(0).setModelIndex(-5);
        table.getColumnModel().getColumn(1).setModelIndex(-5);
        table.getColumnModel().getColumn(2).setModelIndex(-5);
        table.getColumnModel().getColumn(3).setModelIndex(-5);
        assertEquals(-1, table.convertColumnIndexToView(0));
        assertEquals(-1, table.convertColumnIndexToView(1));
        assertEquals(-1, table.convertColumnIndexToView(2));
        assertEquals(-1, table.convertColumnIndexToView(3));
    }

    public void testGetRowCount() throws Exception {
        assertEquals(0, table.getRowCount());
        table = new JTable(3, 4);
        assertEquals(3, table.getRowCount());
        table = new JTable(new DefaultTableModel() {
            @Override
            public int getRowCount() {
                return 10;
            }
        });
        assertEquals(10, table.getRowCount());
    }

    public void testGetColumnCount() throws Exception {
        assertEquals(0, table.getColumnCount());
        table = new JTable(3, 4);
        assertEquals(4, table.getColumnCount());
        table = new JTable(new DefaultTableModel() {
            @Override
            public int getColumnCount() {
                return 10;
            }
        });
        assertEquals(10, table.getColumnCount());
    }

    public void testGetColumnName() throws Exception {
        table = new JTable(3, 4);
        assertEquals("A", table.getColumnName(0));
        assertEquals("B", table.getColumnName(1));
        assertEquals("C", table.getColumnName(2));
        assertEquals("D", table.getColumnName(3));
        table.moveColumn(0, 2);
        assertEquals("B", table.getColumnName(0));
        assertEquals("C", table.getColumnName(1));
        assertEquals("A", table.getColumnName(2));
        assertEquals("D", table.getColumnName(3));
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.getColumnName(-1);
            }
        });
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.getColumnName(100);
            }
        });
    }

    public void testGetColumnClass() throws Exception {
        table = new JTable(new DefaultTableModel(3, 4) {
            @Override
            public Class<?> getColumnClass(final int columnIndex) {
                return columnIndex < 2 ? Object.class : String.class;
            }
        });
        assertEquals(Object.class, table.getColumnClass(0));
        assertEquals(Object.class, table.getColumnClass(1));
        assertEquals(String.class, table.getColumnClass(2));
        assertEquals(String.class, table.getColumnClass(3));
        table.moveColumn(0, 2);
        assertEquals(Object.class, table.getColumnClass(0));
        assertEquals(String.class, table.getColumnClass(1));
        assertEquals(Object.class, table.getColumnClass(2));
        assertEquals(String.class, table.getColumnClass(3));
        assertEquals(Object.class, table.getColumnClass(-1));
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.getColumnName(100);
            }
        });
    }

    public void testGetSetvalueAt() throws Exception {
        table = new JTable(3, 4);
        assertNull(table.getValueAt(0, 0));
        assertNull(table.getValueAt(0, 1));
        assertNull(table.getValueAt(0, 2));
        assertNull(table.getValueAt(0, 3));
        table.setValueAt("a", 0, 0);
        table.setValueAt("b", 0, 1);
        table.setValueAt("c", 0, 2);
        table.setValueAt("d", 0, 3);
        assertEquals("a", table.getValueAt(0, 0));
        assertEquals("b", table.getValueAt(0, 1));
        assertEquals("c", table.getValueAt(0, 2));
        assertEquals("d", table.getValueAt(0, 3));
        table.moveColumn(0, 2);
        assertEquals("b", table.getValueAt(0, 0));
        assertEquals("c", table.getValueAt(0, 1));
        assertEquals("a", table.getValueAt(0, 2));
        assertEquals("d", table.getValueAt(0, 3));
        table.setValueAt("another a", 0, 2);
        assertEquals("another a", table.getValueAt(0, 2));
    }

    public void testIsCellEditable() throws Exception {
        table = new JTable(new DefaultTableModel(3, 4) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return column < 2;
            }
        });
        assertTrue(table.isCellEditable(0, 0));
        assertTrue(table.isCellEditable(0, 1));
        assertFalse(table.isCellEditable(0, 2));
        assertFalse(table.isCellEditable(0, 3));
        table.moveColumn(0, 2);
        assertTrue(table.isCellEditable(0, 0));
        assertFalse(table.isCellEditable(0, 1));
        assertTrue(table.isCellEditable(0, 2));
        assertFalse(table.isCellEditable(0, 3));
        assertTrue(table.isCellEditable(100, -1));
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.isCellEditable(0, 100);
            }
        });
    }

    public void testAddColumn() throws Exception {
        assertEquals(0, table.getColumnCount());
        TableColumn column1 = new TableColumn(0);
        column1.setIdentifier("column1");
        table.addColumn(column1);
        assertEquals(1, table.getColumnCount());
        assertEquals(0, table.getModel().getColumnCount());
        assertSame(column1, table.getColumn("column1"));
        assertEquals(0, table.getColumnModel().getColumnIndex("column1"));
        TableColumn column2 = new TableColumn(1);
        table.addColumn(column2);
        assertEquals(2, table.getColumnCount());
        assertSame(column2, table.getColumn("B"));
        assertEquals(1, table.getColumnModel().getColumnIndex("B"));
        TableColumn column3 = new TableColumn(20);
        table.addColumn(column3);
        assertEquals(3, table.getColumnCount());
        assertSame(column3, table.getColumn("U"));
        assertEquals(2, table.getColumnModel().getColumnIndex("U"));
        TableColumn column31 = new TableColumn(20);
        table.addColumn(column31);
        assertEquals(4, table.getColumnCount());
        assertSame(column3, table.getColumn("U"));
        assertEquals(2, table.getColumnModel().getColumnIndex("U"));
        assertEquals(2, table.getColumnModel().getColumnIndex("U"));
        assertEquals("U", table.getColumnModel().getColumn(2).getIdentifier());
        assertEquals("U", table.getColumnModel().getColumn(3).getIdentifier());
        TableColumn column5 = new TableColumn();
        column5.setIdentifier("column1");
        assertNull(column5.getHeaderValue());
        table.addColumn(column5);
        assertEquals("A", column5.getHeaderValue());
        TableColumn column6 = new TableColumn();
        column6.setIdentifier("column1");
        column6.setHeaderValue("header value");
        table.addColumn(column6);
        assertEquals("header value", column6.getHeaderValue());
    }

    public void testRemoveColumn() throws Exception {
        assertEquals(0, table.getColumnCount());
        TableColumn column1 = new TableColumn(0);
        column1.setIdentifier("column1");
        table.addColumn(column1);
        assertEquals(1, table.getColumnCount());
        assertSame(column1, table.getColumn("column1"));
        assertEquals(0, table.getColumnModel().getColumnIndex("column1"));
        TableColumn column2 = new TableColumn(1);
        table.addColumn(column2);
        assertEquals(2, table.getColumnCount());
        assertSame(column2, table.getColumn("B"));
        assertEquals(1, table.getColumnModel().getColumnIndex("B"));
        table.removeColumn(column1);
        assertEquals(1, table.getColumnCount());
        table.removeColumn(new TableColumn(1));
        assertEquals(1, table.getColumnCount());
    }

    public void testMoveColumn() throws Exception {
        TableColumn column1 = new TableColumn(0);
        table.addColumn(column1);
        assertEquals(0, table.getColumnModel().getColumnIndex("A"));
        TableColumn column2 = new TableColumn(1);
        table.addColumn(column2);
        assertEquals(1, table.getColumnModel().getColumnIndex("B"));
        table.moveColumn(0, 1);
        assertEquals(0, table.getColumnModel().getColumnIndex("B"));
        assertEquals(1, table.getColumnModel().getColumnIndex("A"));
    }

    public void testColumnAtPoint() throws Exception {
        assertEquals(-1, table.columnAtPoint(new Point(10, 10)));
        assertEquals(-1, table.columnAtPoint(new Point(-10, 10)));
        table.addColumn(new TableColumn(0, 20));
        table.addColumn(new TableColumn(1, 30));
        assertEquals(0, table.columnAtPoint(new Point(10, 100)));
        assertEquals(0, table.columnAtPoint(new Point(19, 100)));
        assertEquals(1, table.columnAtPoint(new Point(20, 100)));
        assertEquals(1, table.columnAtPoint(new Point(49, 100)));
        table.moveColumn(0, 1);
        assertEquals(0, table.columnAtPoint(new Point(10, 100)));
        assertEquals(0, table.columnAtPoint(new Point(29, 100)));
        assertEquals(1, table.columnAtPoint(new Point(30, 100)));
        assertEquals(1, table.columnAtPoint(new Point(49, 100)));
    }

    public void testRowAtPoint() throws Exception {
        assertEquals(-1, table.rowAtPoint(new Point(10, 10)));
        assertEquals(-1, table.rowAtPoint(new Point(10, -10)));
        table = new JTable(3, 4);
        table.setRowHeight(10);
        table.setRowHeight(1, 30);
        assertEquals(0, table.rowAtPoint(new Point(1000, 0)));
        assertEquals(0, table.rowAtPoint(new Point(1000, 9)));
        assertEquals(1, table.rowAtPoint(new Point(1000, 10)));
        assertEquals(1, table.rowAtPoint(new Point(1000, 39)));
        assertEquals(2, table.rowAtPoint(new Point(1000, 40)));
        assertEquals(2, table.rowAtPoint(new Point(1000, 49)));
        if (isHarmony()) {
            assertEquals(-1, table.rowAtPoint(new Point(10, -100)));
        } else {
            assertEquals(0, table.rowAtPoint(new Point(10, -100)));
        }
        assertEquals(-1, table.rowAtPoint(new Point(10, 50)));
    }

    public void testGetCellRect() throws Exception {
        assertEquals(new Rectangle(), table.getCellRect(0, 0, true));
        assertEquals(new Rectangle(), table.getCellRect(0, 0, false));
        assertEquals(new Rectangle(), table.getCellRect(10, 10, true));
        assertEquals(new Rectangle(), table.getCellRect(-10, -10, true));
        table = new JTable(3, 4);
        table.setIntercellSpacing(new Dimension(12, 10));
        assertEquals(new Rectangle(0, 0, 75, 16), table.getCellRect(0, 0, true));
        assertEquals(new Rectangle(6, 5, 63, 6), table.getCellRect(0, 0, false));
        assertEquals(new Rectangle(75, 16, 75, 16), table.getCellRect(1, 1, true));
        assertEquals(new Rectangle(81, 21, 63, 6), table.getCellRect(1, 1, false));
        assertEquals(new Rectangle(225, 32, 75, 16), table.getCellRect(2, 3, true));
        assertEquals(new Rectangle(231, 37, 63, 6), table.getCellRect(2, 3, false));
        assertEquals(new Rectangle(0, 32, 0, 16), table.getCellRect(2, 4, true));
        assertEquals(new Rectangle(0, 32, 0, 16), table.getCellRect(2, 4, false));
        assertEquals(new Rectangle(0, 32, 0, 16), table.getCellRect(2, -1, true));
        assertEquals(new Rectangle(0, 32, 0, 16), table.getCellRect(2, -1, false));
        assertEquals(new Rectangle(225, 0, 75, 0), table.getCellRect(3, 3, true));
        assertEquals(new Rectangle(225, 0, 75, 0), table.getCellRect(3, 3, false));
        assertEquals(new Rectangle(225, 0, 75, 0), table.getCellRect(-1, 3, true));
        assertEquals(new Rectangle(225, 0, 75, 0), table.getCellRect(-1, 3, false));
    }

    public void testGetToolTipText() throws Exception {
        assertNull(table.getToolTipText(new MouseEvent(table, MouseEvent.MOUSE_ENTERED, 0, 0,
                0, 0, 0, false)));
        table = new JTable(3, 4) {
            @Override
            public TableCellRenderer getCellRenderer(final int row, final int column) {
                DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) getDefaultRenderer(Object.class);
                renderer.setToolTipText("[" + row + ", " + column + "]");
                return renderer;
            }
        };
        assertNull(table.getToolTipText(new MouseEvent(table, MouseEvent.MOUSE_ENTERED, 0, 0,
                -10, -10, 0, false)));
        assertNull(table.getToolTipText(new MouseEvent(table, MouseEvent.MOUSE_ENTERED, 0, 0,
                800, 20, 0, false)));
        assertEquals("[0, 0]", table.getToolTipText(new MouseEvent(table,
                MouseEvent.MOUSE_ENTERED, 0, 0, 0, 0, 0, false)));
        assertEquals("[0, 0]", table.getToolTipText(new MouseEvent(table,
                MouseEvent.MOUSE_ENTERED, 0, 0, 30, 10, 0, false)));
        assertEquals("[1, 0]", table.getToolTipText(new MouseEvent(table,
                MouseEvent.MOUSE_ENTERED, 0, 0, 30, 20, 0, false)));
        assertEquals("[1, 1]", table.getToolTipText(new MouseEvent(table,
                MouseEvent.MOUSE_ENTERED, 0, 0, 80, 20, 0, false)));
    }

    public void testGetSetSurrendersFocusOnKeystroke() throws Exception {
        assertFalse(table.getSurrendersFocusOnKeystroke());
        table.setSurrendersFocusOnKeystroke(true);
        assertTrue(table.getSurrendersFocusOnKeystroke());
        assertFalse(propertyChangeController.isChanged());
    }

    @SuppressWarnings("deprecation")
    public void testEditCellAt() throws Exception {
        assertFalse(table.editCellAt(0, 0));
        assertFalse(table.isEditing());
        assertEquals(-1, table.getEditingRow());
        assertEquals(-1, table.getEditingColumn());
        assertNull(table.getCellEditor());
        assertNull(table.getEditorComponent());
        table = new JTable(3, 4);
        assertTrue(table.editCellAt(0, 0));
        assertTrue(table.isEditing());
        assertNotNull(table.getEditorComponent());
        assertEquals(0, table.getEditingRow());
        assertEquals(0, table.getEditingColumn());
        assertTrue(table.editCellAt(1, 2, new KeyEvent(table, KeyEvent.KEY_PRESSED, 0, 0, 0)));
        assertTrue(table.isEditing());
        assertNotNull(table.getEditorComponent());
        assertEquals(1, table.getEditingRow());
        assertEquals(2, table.getEditingColumn());
        assertNotNull(table.getCellEditor());
        assertFalse(table.editCellAt(0, 0, new MouseEvent(table, MouseEvent.MOUSE_ENTERED, 0,
                0, 0, 0, 0, false)));
        assertFalse(table.isEditing());
        assertEquals(-1, table.getEditingRow());
        assertEquals(-1, table.getEditingColumn());
        assertNull(table.getCellEditor());
        assertNull(table.getEditorComponent());
        table = new JTable(new DefaultTableModel() {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return false;
            }
        });
        assertFalse(table.editCellAt(1, 2));
        assertFalse(table.isEditing());
    }

    public void testIsEditing() throws Exception {
        assertFalse(table.isEditing());
        table.cellEditor = new DefaultCellEditor(new JCheckBox());
        assertTrue(table.isEditing());
    }

    public void testGetEditingComponent() throws Exception {
        assertNull(table.getEditorComponent());
        table.editorComp = new JLabel();
        assertEquals(table.editorComp, table.getEditorComponent());
    }

    public void testGetSetEditingRowColumn() throws Exception {
        assertEquals(-1, table.getEditingRow());
        assertEquals(-1, table.getEditingColumn());
        table.editingRow = 25;
        table.editingColumn = -5;
        assertEquals(25, table.getEditingRow());
        assertEquals(-5, table.getEditingColumn());
        table.setEditingRow(15);
        assertEquals(15, table.getEditingRow());
        assertFalse(propertyChangeController.isChanged());
    }

    public void testGetSetUpdateUI() throws Exception {
        assertTrue(table.getUI() instanceof BasicTableUI);
        TableUI ui = new BasicTableUI();
        table.setUI(ui);
        assertSame(ui, table.getUI());
        table.updateUI();
        assertNotSame(ui, table.getUI());
    }

    public void testGetUIClassID() throws Exception {
        assertEquals("TableUI", table.getUIClassID());
    }

    public void testGetSetModel() throws Exception {
        DefaultTableModel oldModel = (DefaultTableModel) table.getModel();
        assertNotNull(oldModel);
        assertEquals(1, oldModel.getTableModelListeners().length);
        assertEquals(table, oldModel.getTableModelListeners()[0]);
        DefaultTableModel model = new DefaultTableModel(3, 4);
        table.setModel(model);
        assertEquals(0, oldModel.getTableModelListeners().length);
        assertEquals(1, model.getTableModelListeners().length);
        assertEquals(4, table.getColumnModel().getColumnCount());
        assertTrue(propertyChangeController.isChanged("model"));
        table.setAutoCreateColumnsFromModel(false);
        table.setModel(new DefaultTableModel(1, 2));
        assertEquals(4, table.getColumnModel().getColumnCount());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.setModel(null);
            }
        });
    }

    public void testGetSetColumnModel() throws Exception {
        table = new JTable(3, 4);
        DefaultTableColumnModel oldModel = (DefaultTableColumnModel) table.getColumnModel();
        assertNotNull(oldModel);
        assertEquals(2, oldModel.getColumnModelListeners().length);
        assertEquals(table.getTableHeader(), oldModel.getColumnModelListeners()[0]);
        assertEquals(table, oldModel.getColumnModelListeners()[1]);
        DefaultTableColumnModel model = new DefaultTableColumnModel();
        table.setColumnModel(model);
        assertEquals(0, oldModel.getColumnModelListeners().length);
        assertEquals(2, model.getColumnModelListeners().length);
        assertEquals(0, table.getColumnModel().getColumnCount());
        assertFalse(propertyChangeController.isChanged());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.setColumnModel(null);
            }
        });
    }

    public void testGetSetSelectionModel() throws Exception {
        DefaultListSelectionModel oldModel = (DefaultListSelectionModel) table
                .getSelectionModel();
        assertNotNull(oldModel);
        assertEquals(1, oldModel.getListSelectionListeners().length);
        assertEquals(table, oldModel.getListSelectionListeners()[0]);
        DefaultListSelectionModel model = new DefaultListSelectionModel();
        table.setSelectionModel(model);
        assertEquals(0, oldModel.getListSelectionListeners().length);
        assertEquals(1, model.getListSelectionListeners().length);
        assertTrue(propertyChangeController.isChanged("selectionModel"));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.setSelectionModel(null);
            }
        });
    }

    public void testEditingCanceled() throws Exception {
        table = new JTable(3, 4);
        table.editCellAt(0, 0);
        assertTrue(table.isEditing());
        assertNotNull(table.getCellEditor());
        assertNotNull(table.getEditorComponent());
        assertEquals(0, table.getEditingRow());
        assertEquals(0, table.getEditingColumn());
        table.setCellEditor(new DefaultCellEditor((JTextField) table.getEditorComponent()));
        ((JTextField) table.getEditorComponent()).setText("new value");
        table.editingCanceled(null);
        assertFalse(table.isEditing());
        assertNull(table.getValueAt(0, 0));
    }

    public void testEditingStoppped() throws Exception {
        table = new JTable(3, 4);
        table.editCellAt(0, 0);
        assertTrue(table.isEditing());
        assertNotNull(table.getCellEditor());
        assertNotNull(table.getEditorComponent());
        assertEquals(0, table.getEditingRow());
        assertEquals(0, table.getEditingColumn());
        table.setCellEditor(new DefaultCellEditor((JTextField) table.getEditorComponent()));
        ((JTextField) table.getEditorComponent()).setText("new value");
        table.editingStopped(null);
        assertFalse(table.isEditing());
        assertEquals("new value", table.getValueAt(0, 0));
    }

    public void testGetSetPreferredScrollableViewportSize() throws Exception {
        assertEquals(new Dimension(450, 400), table.getPreferredScrollableViewportSize());
        table = new JTable(3, 4);
        assertEquals(new Dimension(450, 400), table.getPreferredScrollableViewportSize());
        table = new JTable(300, 400);
        assertEquals(new Dimension(450, 400), table.getPreferredScrollableViewportSize());
        table.setBorder(BorderFactory.createLineBorder(Color.RED, 20));
        assertEquals(new Dimension(450, 400), table.getPreferredScrollableViewportSize());
        Dimension size = new Dimension(200, 300);
        table.setPreferredScrollableViewportSize(size);
        assertSame(size, table.getPreferredScrollableViewportSize());
        assertFalse(propertyChangeController.isChanged());
    }

    //TODO
    public void testGetScrollableUnitIncrement() throws Exception {
    }

    //TODO
    public void testGetScrollableBlockIncrement() throws Exception {
    }

    public void testGetScrollableTracksViewportWidth() throws Exception {
        assertTrue(table.getScrollableTracksViewportWidth());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        assertFalse(table.getScrollableTracksViewportWidth());
    }

    public void testGetScrollableTracksViewportHeight() throws Exception {
        assertFalse(table.getScrollableTracksViewportHeight());
    }

    public void testCreateDefaultRenderers() throws Exception {
        assertEquals(8, table.defaultRenderersByColumnClass.size());
        table.defaultRenderersByColumnClass = null;
        table.createDefaultRenderers();
        assertEquals(8, table.defaultRenderersByColumnClass.size());
    }

    public void testCreateDefaultEditors() throws Exception {
        assertEquals(3, table.defaultEditorsByColumnClass.size());
        table.defaultEditorsByColumnClass = null;
        table.createDefaultEditors();
        assertEquals(3, table.defaultEditorsByColumnClass.size());
    }

    public void testInitializeLocalVars() throws Exception {
        table.rowMargin = 0;
        table.tableHeader = null;
        table.rowHeight = 0;
        table.showHorizontalLines = false;
        table.showVerticalLines = false;
        table.autoResizeMode = 100;
        table.preferredViewportSize = null;
        table.rowSelectionAllowed = false;
        table.defaultEditorsByColumnClass = null;
        table.defaultRenderersByColumnClass = null;
        table.initializeLocalVars();
        assertEquals(1, table.rowMargin);
        assertNotNull(table.tableHeader);
        assertEquals(16, table.rowHeight);
        assertTrue(table.showHorizontalLines);
        assertTrue(table.showVerticalLines);
        assertEquals(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS, table.autoResizeMode);
        assertEquals(new Dimension(450, 400), table.preferredViewportSize);
        assertTrue(table.rowSelectionAllowed);
        assertEquals(3, table.defaultEditorsByColumnClass.size());
        assertEquals(8, table.defaultRenderersByColumnClass.size());
    }

    public void testCreateDefaultDataModel() throws Exception {
        assertSame(DefaultTableModel.class, table.createDefaultDataModel().getClass());
        assertNotSame(table.createDefaultDataModel(), table.createDefaultDataModel());
    }

    public void testCreateDefaultColumnModel() throws Exception {
        assertSame(DefaultTableColumnModel.class, table.createDefaultColumnModel().getClass());
        assertNotSame(table.createDefaultColumnModel(), table.createDefaultColumnModel());
    }

    public void testCreateDefaultSelectionModel() throws Exception {
        assertSame(DefaultListSelectionModel.class, table.createDefaultSelectionModel()
                .getClass());
        assertNotSame(table.createDefaultSelectionModel(), table.createDefaultSelectionModel());
    }

    public void testCreateDefaultTableHeader() throws Exception {
        assertSame(JTableHeader.class, table.createDefaultTableHeader().getClass());
        assertNotSame(table.createDefaultTableHeader(), table.createDefaultTableHeader());
    }

    public void testGetSetCellEditor() throws Exception {
        assertNull(table.getCellEditor());
        TableCellEditor editor = new DefaultCellEditor(new JTextField());
        table.setCellEditor(editor);
        assertSame(editor, table.getCellEditor());
    }

    public void testGetCellRenderer() throws Exception {
        table = new JTable(3, 4);
        assertEquals(table.defaultRenderersByColumnClass.get(Object.class), table
                .getCellRenderer(0, 0));
        TableCellRenderer renderer = new DefaultTableCellRenderer();
        table.getColumnModel().getColumn(0).setCellRenderer(renderer);
        assertEquals(renderer, table.getCellRenderer(0, 0));
        assertEquals(renderer, table.getCellRenderer(1, 0));
        assertEquals(renderer, table.getCellRenderer(2, 0));
        table.moveColumn(0, 2);
        assertEquals(renderer, table.getCellRenderer(0, 2));
        assertEquals(renderer, table.getCellRenderer(1, 2));
        assertEquals(renderer, table.getCellRenderer(2, 2));
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.getCellRenderer(100, 100);
            }
        });
    }

    public void testPrepareRenderer() throws Exception {
        table = new JTable(3, 4);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        assertSame(renderer, table.prepareRenderer(renderer, 0, 0));
        assertEquals(table.getBackground(), renderer.getBackground());
        assertEquals(table.getForeground(), renderer.getForeground());
        table.getSelectionModel().setSelectionInterval(0, 0);
        table.prepareRenderer(renderer, 0, 0);
        assertEquals(table.getSelectionBackground(), renderer.getBackground());
        assertEquals(table.getSelectionForeground(), renderer.getForeground());
    }

    public void testPrepareEditor() throws Exception {
        table = new JTable(3, 4);
        JTextField editorComponent = new JTextField();
        DefaultCellEditor editor = new DefaultCellEditor(editorComponent);
        table.setCellEditor(editor);
        table.editorComp = editorComponent;
        assertSame(editorComponent, table.prepareEditor(editor, 0, 0));
        assertNull(editorComponent.getParent());
        assertEquals(new Rectangle(), editorComponent.getBounds());
    }

    public void testGetCellEditor() throws Exception {
        table = new JTable(3, 4);
        assertEquals(table.defaultEditorsByColumnClass.get(Object.class), table.getCellEditor(
                0, 0));
        TableCellEditor editor = new DefaultCellEditor(new JTextField());
        table.getColumnModel().getColumn(0).setCellEditor(editor);
        assertEquals(editor, table.getCellEditor(0, 0));
        assertEquals(editor, table.getCellEditor(1, 0));
        assertEquals(editor, table.getCellEditor(2, 0));
        table.moveColumn(0, 2);
        assertEquals(editor, table.getCellEditor(0, 2));
        assertEquals(editor, table.getCellEditor(1, 2));
        assertEquals(editor, table.getCellEditor(2, 2));
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                table.getCellEditor(100, 100);
            }
        });
    }

    public void testRemoveEditor() throws Exception {
        table.removeEditor();
        int childrenCount = table.getComponentCount();
        table.setCellEditor(new DefaultCellEditor(new JTextField()));
        table.editorComp = new JTextField();
        table.add(table.editorComp);
        table.setEditingColumn(10);
        table.setEditingRow(10);
        assertEquals(childrenCount + 1, table.getComponentCount());
        table.removeEditor();
        assertNull(table.getCellEditor());
        assertNull(table.getEditorComponent());
        assertEquals(childrenCount, table.getComponentCount());
        assertEquals(-1, table.getEditingColumn());
        assertEquals(-1, table.getEditingRow());
    }

    private int[] getSelectedIndices(final ListSelectionModel selModel) {
        int count = 0;
        for (int i = selModel.getMinSelectionIndex(); i <= selModel.getMaxSelectionIndex(); i++) {
            if (selModel.isSelectedIndex(i)) {
                count++;
            }
        }
        int[] result = new int[count];
        count = 0;
        for (int i = selModel.getMinSelectionIndex(); i <= selModel.getMaxSelectionIndex(); i++) {
            if (selModel.isSelectedIndex(i)) {
                result[count++] = i;
            }
        }
        return result;
    }
}
