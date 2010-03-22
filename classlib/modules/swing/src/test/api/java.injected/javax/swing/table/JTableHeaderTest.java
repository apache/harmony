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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import javax.swing.BasicSwingTestCase;
import javax.swing.JTable;
import javax.swing.plaf.TableHeaderUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTableHeaderUI;

public class JTableHeaderTest extends BasicSwingTestCase {
    private JTableHeader header;

    public JTableHeaderTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        header = new JTableHeader();
        propertyChangeController = new PropertyChangeController();
        header.addPropertyChangeListener(propertyChangeController);
    }

    @Override
    protected void tearDown() throws Exception {
        header = null;
    }

    public void testJTableHeader() throws Exception {
        assertTrue(header.columnModel instanceof DefaultTableColumnModel);
        header = new JTableHeader(null);
        assertTrue(header.columnModel instanceof DefaultTableColumnModel);
        TableColumnModel cm = new DefaultTableColumnModel();
        header = new JTableHeader(cm);
        assertSame(cm, header.columnModel);
    }

    public void testGetSetTable() throws Exception {
        assertNull(header.table);
        assertNull(header.getTable());
        JTable table = new JTable();
        header.setTable(table);
        assertSame(table, header.getTable());
        assertTrue(propertyChangeController.isChanged("table"));
    }

    public void testGetSetReorderingAllowed() throws Exception {
        assertTrue(header.getReorderingAllowed());
        header.setReorderingAllowed(false);
        assertFalse(header.getReorderingAllowed());
        assertTrue(propertyChangeController.isChanged("reorderingAllowed"));
    }

    public void testGetSetResizingAllowed() throws Exception {
        assertTrue(header.getResizingAllowed());
        header.setResizingAllowed(false);
        assertFalse(header.getResizingAllowed());
        assertTrue(propertyChangeController.isChanged("resizingAllowed"));
    }

    public void testGetSetDraggedColumn() throws Exception {
        assertNull(header.getDraggedColumn());
        TableColumn column = new TableColumn();
        header.setDraggedColumn(column);
        assertEquals(column, header.getDraggedColumn());
        assertFalse(propertyChangeController.isChanged());
    }

    public void testGetSetDraggedDistance() throws Exception {
        assertEquals(0, header.getDraggedDistance());
        header.setDraggedDistance(10);
        assertEquals(10, header.getDraggedDistance());
        header.setDraggedDistance(-10);
        assertEquals(-10, header.getDraggedDistance());
        assertFalse(propertyChangeController.isChanged());
    }

    public void testGetSetResizingColumn() throws Exception {
        assertNull(header.getResizingColumn());
        TableColumn column = new TableColumn();
        header.setResizingColumn(column);
        assertEquals(column, header.getResizingColumn());
        assertFalse(propertyChangeController.isChanged());
    }

    public void testGetSetUpdateTableInRealTime() throws Exception {
        assertTrue(header.getUpdateTableInRealTime());
        header.setUpdateTableInRealTime(false);
        assertFalse(header.getUpdateTableInRealTime());
        assertFalse(propertyChangeController.isChanged());
    }

    public void testGetSetDefaultRenderer() throws Exception {
        assertTrue(header.getDefaultRenderer() instanceof DefaultTableCellRenderer);
        assertTrue(header.getDefaultRenderer() instanceof UIResource);
        TableCellRenderer renderer = new DefaultTableCellRenderer();
        header.setDefaultRenderer(renderer);
        assertSame(renderer, header.getDefaultRenderer());
        assertFalse(propertyChangeController.isChanged());
        header.setDefaultRenderer(null);
        assertNull(header.getDefaultRenderer());
    }

    public void testColumnAtPoint() throws Exception {
        assertEquals(-1, header.columnAtPoint(new Point(10, 10)));
        header.columnModel.addColumn(new TableColumn(0, 20));
        header.columnModel.addColumn(new TableColumn(0, 30));
        assertEquals(0, header.columnAtPoint(new Point(10, 1000)));
        assertEquals(0, header.columnAtPoint(new Point(19, 1000)));
        assertEquals(1, header.columnAtPoint(new Point(30, 1000)));
    }

    public void testGetHeaderRect() throws Exception {
        assertEquals(new Rectangle(), header.getHeaderRect(10));
        header.columnModel.addColumn(new TableColumn(0, 20));
        assertEquals(new Rectangle(0, 0, 20, 0), header.getHeaderRect(0));
        assertEquals(new Rectangle(), header.getHeaderRect(1));
        header.columnModel.addColumn(new TableColumn(0, 30));
        assertEquals(new Rectangle(0, 0, 20, 0), header.getHeaderRect(0));
        assertEquals(new Rectangle(20, 0, 30, 0), header.getHeaderRect(1));
        assertEquals(new Rectangle(), header.getHeaderRect(2));
        assertNotSame(header.getHeaderRect(2), header.getHeaderRect(2));
        header.setBounds(10, 10, 100, 30);
        assertEquals(new Rectangle(0, 0, 20, 30), header.getHeaderRect(0));
    }

    public void testGetToolTipText() throws Exception {
        assertNull(header.getToolTipText(new MouseEvent(header, 0, 0, 0, 0, 0, 0, false)));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setToolTipText("tooltip");
        assertNull(header.getToolTipText(new MouseEvent(header, 0, 0, 0, 0, 0, 0, false)));
        header.columnModel.addColumn(new TableColumn(0, 20));
        assertEquals("tooltip", header.getToolTipText(new MouseEvent(header, 0, 0, 0, 0, 100,
                0, false)));
        assertNull(header.getToolTipText(new MouseEvent(header, 0, 0, 0, 25, 0, 0, false)));
        header.columnModel.addColumn(new TableColumn(0, 20));
        assertEquals("tooltip", header.getToolTipText(new MouseEvent(header, 0, 0, 0, 0, 100,
                0, false)));
        assertEquals("tooltip", header.getToolTipText(new MouseEvent(header, 0, 0, 0, 25, 100,
                0, false)));
        DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) header.columnModel
                .getColumn(0).createDefaultHeaderRenderer();
        renderer.setToolTipText("column tooltip");
        header.columnModel.getColumn(0).setHeaderRenderer(renderer);
        assertEquals("column tooltip", header.getToolTipText(new MouseEvent(header, 0, 0, 0, 0,
                100, 0, false)));
        assertEquals("tooltip", header.getToolTipText(new MouseEvent(header, 0, 0, 0, 25, 100,
                0, false)));
    }

    public void testGetSetUpdateUI() throws Exception {
        assertNotNull(header.getUI());
        TableHeaderUI ui = new BasicTableHeaderUI();
        header.setUI(ui);
        assertSame(ui, header.getUI());
        header.updateUI();
        assertNotNull(header.getUI());
        assertNotSame(ui, header.getUI());
    }

    public void testGetSetColumnModel() throws Exception {
        assertTrue(header.getColumnModel() instanceof DefaultTableColumnModel);
        assertTrue(Arrays.asList(
                ((DefaultTableColumnModel) header.getColumnModel()).getColumnModelListeners())
                .contains(header));
        DefaultTableColumnModel model = new DefaultTableColumnModel();
        header.setColumnModel(model);
        assertSame(model, header.getColumnModel());
        assertTrue(Arrays.asList(model.getColumnModelListeners()).contains(header));
        assertTrue(propertyChangeController.isChanged("columnModel"));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                header.setColumnModel(null);
            }
        });
    }

    public void testCreateDefaultColumnModel() throws Exception {
        assertTrue(header.createDefaultColumnModel().getClass() == DefaultTableColumnModel.class);
        assertNotSame(header.createDefaultColumnModel(), header.createDefaultColumnModel());
    }

    public void testCreateDefaultRenderer() throws Exception {
        assertTrue(header.createDefaultRenderer() instanceof DefaultTableCellRenderer);
        assertTrue(header.createDefaultRenderer() instanceof UIResource);
        assertNotSame(header.createDefaultRenderer(), header.createDefaultRenderer());
    }

    public void testInitializeLocalVars() throws Exception {
        assertTrue(header.getReorderingAllowed());
        assertTrue(header.getResizingAllowed());
        assertTrue(header.getUpdateTableInRealTime());
        assertNotNull(header.getDefaultRenderer());
        assertNull(header.getResizingColumn());
        assertNull(header.getDraggedColumn());
        assertEquals(0, header.getDraggedDistance());
        assertNull(header.getTable());
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setUpdateTableInRealTime(false);
        header.setDefaultRenderer(null);
        header.setResizingColumn(new TableColumn());
        header.setDraggedColumn(new TableColumn());
        header.setDraggedDistance(10);
        header.setTable(new JTable());
        header.initializeLocalVars();
        assertTrue(header.getReorderingAllowed());
        assertTrue(header.getResizingAllowed());
        assertTrue(header.getUpdateTableInRealTime());
        assertNotNull(header.getDefaultRenderer());
        assertNull(header.getResizingColumn());
        assertNull(header.getDraggedColumn());
        assertEquals(0, header.getDraggedDistance());
        assertNull(header.getTable());
    }

    public void testGetAccessibleContext() throws Exception {
        assertTrue(header.getAccessibleContext() instanceof JTableHeader.AccessibleJTableHeader);
    }
}
