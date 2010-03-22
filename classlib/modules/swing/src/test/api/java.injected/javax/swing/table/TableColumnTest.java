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
import java.awt.Dimension;
import javax.swing.BasicSwingTestCase;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;

public class TableColumnTest extends BasicSwingTestCase {
    private TableColumn column;

    public TableColumnTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        column = new TableColumn();
        propertyChangeController = new PropertyChangeController();
        column.addPropertyChangeListener(propertyChangeController);
    }

    @Override
    protected void tearDown() throws Exception {
        column = null;
        propertyChangeController = null;
    }

    @SuppressWarnings("deprecation")
    public void testTableColumn() throws Exception {
        assertEquals(0, column.modelIndex);
        assertEquals(75, column.width);
        assertEquals(75, column.getPreferredWidth());
        assertEquals(15, column.minWidth);
        assertEquals(Integer.MAX_VALUE, column.maxWidth);
        assertEquals(0, column.resizedPostingDisableCount);
        assertNull(column.cellRenderer);
        assertNull(column.cellEditor);
        assertNull(column.headerValue);
        assertNull(column.headerRenderer);
        assertNull(column.identifier);
        column = new TableColumn(10);
        assertEquals(10, column.modelIndex);
        column = new TableColumn(10, 20);
        assertEquals(10, column.modelIndex);
        assertEquals(20, column.width);
        assertEquals(20, column.getPreferredWidth());
        TableCellRenderer renderer = new DefaultTableCellRenderer();
        TableCellEditor editor = new DefaultCellEditor(new JCheckBox());
        column = new TableColumn(10, 20, renderer, editor);
        assertEquals(10, column.modelIndex);
        assertEquals(20, column.width);
        assertEquals(20, column.getPreferredWidth());
        assertEquals(renderer, column.cellRenderer);
        assertEquals(editor, column.cellEditor);
    }

    public void testGetSetModelIndex() throws Exception {
        assertEquals(0, column.getModelIndex());
        column.setModelIndex(5);
        assertEquals(5, column.getModelIndex());
        assertTrue(propertyChangeController.isChanged("modelIndex"));
    }

    public void testGetSetIdentifier() throws Exception {
        assertNull(column.getIdentifier());
        column.setHeaderValue("Header");
        assertEquals("Header", column.getIdentifier());
        assertNull(column.identifier);
        propertyChangeController.reset();
        column.setIdentifier("Any");
        assertEquals("Any", column.getIdentifier());
        assertTrue(propertyChangeController.isChanged("identifier"));
    }

    public void testGetSetHeaderValue() throws Exception {
        assertNull(column.getHeaderValue());
        column.setHeaderValue("Any");
        assertEquals("Any", column.getHeaderValue());
        assertTrue(propertyChangeController.isChanged("headerValue"));
    }

    public void testGetSetHeaderRenderer() throws Exception {
        assertNull(column.getHeaderRenderer());
        TableCellRenderer renderer = new DefaultTableCellRenderer();
        column.setHeaderRenderer(renderer);
        assertEquals(renderer, column.getHeaderRenderer());
        assertTrue(propertyChangeController.isChanged("headerRenderer"));
    }

    public void testGetSetCellRenderer() throws Exception {
        assertNull(column.getCellRenderer());
        TableCellRenderer renderer = new DefaultTableCellRenderer();
        column.setCellRenderer(renderer);
        assertEquals(renderer, column.getCellRenderer());
        assertTrue(propertyChangeController.isChanged("cellRenderer"));
    }

    public void testGetSetCellEditor() throws Exception {
        assertNull(column.getCellEditor());
        TableCellEditor editor = new DefaultCellEditor(new JCheckBox());
        column.setCellEditor(editor);
        assertEquals(editor, column.getCellEditor());
        assertTrue(propertyChangeController.isChanged("cellEditor"));
    }

    public void testGetSetWidth() throws Exception {
        assertEquals(75, column.getWidth());
        column.setWidth(20);
        assertEquals(20, column.getWidth());
        assertTrue(propertyChangeController.isChanged("width"));
        column.setMaxWidth(100);
        column.setWidth(10);
        assertEquals(15, column.getWidth());
        column.setWidth(150);
        assertEquals(100, column.getWidth());
        assertEquals(75, column.getPreferredWidth());
    }

    public void testGetSetPreferredWidth() throws Exception {
        assertEquals(75, column.getPreferredWidth());
        column.setPreferredWidth(50);
        assertEquals(50, column.getPreferredWidth());
        assertTrue(propertyChangeController.isChanged("preferredWidth"));
        column.setMaxWidth(100);
        column.setPreferredWidth(10);
        assertEquals(15, column.getPreferredWidth());
        column.setPreferredWidth(150);
        assertEquals(100, column.getPreferredWidth());
        assertEquals(75, column.getWidth());
    }

    public void testGetSetMinWidth() throws Exception {
        assertEquals(15, column.getMinWidth());
        column.setMinWidth(50);
        assertEquals(50, column.getMinWidth());
        assertTrue(propertyChangeController.isChanged("minWidth"));
        column.setMinWidth(100);
        assertEquals(100, column.getWidth());
        assertEquals(100, column.getPreferredWidth());
        column.setMinWidth(10);
        assertEquals(100, column.getWidth());
        assertEquals(100, column.getPreferredWidth());
    }

    public void testGetSetMaxWidth() throws Exception {
        assertEquals(Integer.MAX_VALUE, column.getMaxWidth());
        column.setMaxWidth(100);
        assertEquals(100, column.getMaxWidth());
        assertTrue(propertyChangeController.isChanged("maxWidth"));
        column.setMaxWidth(50);
        assertEquals(50, column.getWidth());
        assertEquals(50, column.getPreferredWidth());
        column.setMinWidth(100);
        assertEquals(100, column.getMinWidth());
        assertEquals(50, column.getWidth());
        assertEquals(50, column.getPreferredWidth());
    }

    public void testGetSetResizable() throws Exception {
        assertTrue(column.getResizable());
        column.setResizable(false);
        assertFalse(column.getResizable());
        assertTrue(propertyChangeController.isChanged("isResizable"));
    }

    public void testSizeWidthToFit() throws Exception {
        column.sizeWidthToFit();
        assertEquals(15, column.getMinWidth());
        assertEquals(75, column.getWidth());
        assertEquals(75, column.getPreferredWidth());
        assertEquals(Integer.MAX_VALUE, column.getMaxWidth());
        DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) column
                .createDefaultHeaderRenderer();
        renderer.setMaximumSize(new Dimension(600, 200));
        renderer.setPreferredSize(new Dimension(400, 100));
        renderer.setMinimumSize(new Dimension(200, 50));
        column.setHeaderRenderer(renderer);
        column.sizeWidthToFit();
        assertEquals(200, column.getMinWidth());
        assertEquals(400, column.getWidth());
        assertEquals(400, column.getPreferredWidth());
        assertEquals(600, column.getMaxWidth());
        column.setHeaderRenderer(column.createDefaultHeaderRenderer());
        column.sizeWidthToFit();
        // specifics of our JLabel's size calculation processing: we calculate component size including
        // their border insets for all the components in the same manner.
        // Phenomena caused by inconsistent definition of border insets so that
        // different Border.getBorderInsets() methods return different results.
        if (isHarmony()) {
            assertEquals(2, column.getMinWidth());
            assertEquals(2, column.getWidth());
            assertEquals(2, column.getPreferredWidth());
            assertEquals(2, column.getMaxWidth());
        } else {
            assertEquals(0, column.getMinWidth());
            assertEquals(0, column.getWidth());
            assertEquals(0, column.getPreferredWidth());
            assertEquals(0, column.getMaxWidth());
        }
        column.setHeaderValue("Any");
        column.sizeWidthToFit();
        Component defaultRenderingComponent = column.createDefaultHeaderRenderer()
                .getTableCellRendererComponent(null, "Any", false, false, 0, 0);
        assertEquals(defaultRenderingComponent.getMinimumSize().width, column.getMinWidth());
        assertEquals(defaultRenderingComponent.getPreferredSize().width, column.getWidth());
        assertEquals(defaultRenderingComponent.getPreferredSize().width, column
                .getPreferredWidth());
        assertEquals(defaultRenderingComponent.getMaximumSize().width, column.getMaxWidth());
        column.setHeaderRenderer(column.createDefaultHeaderRenderer());
        column.setHeaderValue(null);
        defaultRenderingComponent = column.createDefaultHeaderRenderer()
                .getTableCellRendererComponent(null, "Any", false, false, 0, 0);
        assertEquals(defaultRenderingComponent.getMinimumSize().width, column.getMinWidth());
        assertEquals(defaultRenderingComponent.getPreferredSize().width, column.getWidth());
        assertEquals(defaultRenderingComponent.getPreferredSize().width, column
                .getPreferredWidth());
        assertEquals(defaultRenderingComponent.getMaximumSize().width, column.getMaxWidth());
    }

    @SuppressWarnings("deprecation")
    public void testEnableDisableResizedPosting() throws Exception {
        assertEquals(0, column.resizedPostingDisableCount);
        column.disableResizedPosting();
        assertEquals(1, column.resizedPostingDisableCount);
        assertFalse(propertyChangeController.isChanged());
        column.disableResizedPosting();
        assertEquals(2, column.resizedPostingDisableCount);
        column.disableResizedPosting();
        assertEquals(3, column.resizedPostingDisableCount);
        column.enableResizedPosting();
        assertEquals(2, column.resizedPostingDisableCount);
        assertFalse(propertyChangeController.isChanged());
        column.enableResizedPosting();
        assertEquals(1, column.resizedPostingDisableCount);
        column.enableResizedPosting();
        assertEquals(0, column.resizedPostingDisableCount);
        column.enableResizedPosting();
        assertEquals(-1, column.resizedPostingDisableCount);
    }

    public void testAddRemoveGetPropertyChangeListener() throws Exception {
        assertEquals(1, column.getPropertyChangeListeners().length);
        assertEquals(propertyChangeController, column.getPropertyChangeListeners()[0]);
        column.removePropertyChangeListener(propertyChangeController);
        assertEquals(0, column.getPropertyChangeListeners().length);
    }

    public void testCreateDefaultHeaderRenderer() throws Exception {
        assertTrue(column.createDefaultHeaderRenderer() instanceof DefaultTableCellRenderer);
        assertNotSame(column.createDefaultHeaderRenderer(), column
                .createDefaultHeaderRenderer());
    }
}
