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

import java.awt.Color;
import java.awt.Insets;
import javax.swing.BasicSwingTestCase;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.LabelUI;

public class DefaultTableCellRendererTest extends BasicSwingTestCase {
    private DefaultTableCellRenderer renderer;

    public DefaultTableCellRendererTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        renderer = new DefaultTableCellRenderer();
    }

    @Override
    protected void tearDown() throws Exception {
        renderer = null;
    }

    public void testDefaultTableCellRenderer() throws Exception {
        assertTrue(DefaultTableCellRenderer.noFocusBorder instanceof EmptyBorder);
        assertEquals(new Insets(1, 1, 1, 1), DefaultTableCellRenderer.noFocusBorder
                .getBorderInsets(null));
    }

    public void testSetForegroundBackground() throws Exception {
        PropertyChangeController controller = new PropertyChangeController();
        renderer.addPropertyChangeListener(controller);
        renderer.setForeground(Color.RED);
        assertEquals(Color.RED, renderer.getForeground());
        assertFalse(controller.isChanged());
        renderer.setBackground(Color.GREEN);
        assertEquals(Color.GREEN, renderer.getBackground());
        assertFalse(controller.isChanged());
        renderer.setBackground(null);
        renderer.setForeground(null);
        assertNull(renderer.getForeground());
        assertNull(renderer.getBackground());
        JTable table = new JTable();
        table.setBackground(Color.BLUE);
        table.setForeground(Color.RED);
        table.setSelectionBackground(Color.MAGENTA);
        table.setSelectionForeground(Color.CYAN);
        assertNull(renderer.getForeground());
        assertNull(renderer.getBackground());
        assertSame(renderer, renderer.getTableCellRendererComponent(table, null, false, false,
                0, 0));
        assertEquals(table.getBackground(), renderer.getBackground());
        assertEquals(table.getForeground(), renderer.getForeground());
        renderer.setForeground(Color.GRAY);
        renderer.setBackground(Color.YELLOW);
        renderer.getTableCellRendererComponent(table, null, false, false, 0, 0);
        if (isHarmony()) {
            assertEquals(table.getBackground(), renderer.getBackground());
            assertEquals(table.getForeground(), renderer.getForeground());
        } else {
            assertEquals(Color.YELLOW, renderer.getBackground());
            assertEquals(Color.GRAY, renderer.getForeground());
        }
        renderer.getTableCellRendererComponent(table, null, true, false, 0, 0);
        assertEquals(table.getSelectionBackground(), renderer.getBackground());
        assertEquals(table.getSelectionForeground(), renderer.getForeground());
    }

    public void testUpdateUI() throws Exception {
        ComponentUI ui = renderer.getUI();
        assertTrue(ui instanceof LabelUI);
        renderer.updateUI();
        assertSame(ui, renderer.getUI());
    }

    public void testGetTableCellRendererComponent() throws Exception {
        JTable table = new JTable();
        table.setBackground(Color.BLUE);
        table.setForeground(Color.RED);
        table.setSelectionBackground(Color.MAGENTA);
        table.setSelectionForeground(Color.CYAN);
        assertSame(renderer, renderer.getTableCellRendererComponent(table, null, false, false,
                0, 0));
        assertEquals("", renderer.getText());
        assertSame(DefaultTableCellRenderer.noFocusBorder, renderer.getBorder());
        assertEquals(table.getBackground(), renderer.getBackground());
        assertEquals(table.getForeground(), renderer.getForeground());
        assertEquals(table.getFont(), renderer.getFont());
        assertSame(renderer, renderer.getTableCellRendererComponent(table, "any value", false,
                true, -1, -1));
        assertEquals("any value", renderer.getText());
        assertSame(UIManager.getBorder("Table.focusCellHighlightBorder"), renderer.getBorder());
        assertEquals(UIManager.getColor("Table.focusCellBackground"), renderer.getBackground());
        assertEquals(UIManager.getColor("Table.focusCellForeground"), renderer.getForeground());
        assertEquals(table.getFont(), renderer.getFont());
        assertSame(renderer, renderer.getTableCellRendererComponent(table, "any value", true,
                false, 0, 0));
        assertEquals("any value", renderer.getText());
        assertSame(DefaultTableCellRenderer.noFocusBorder, renderer.getBorder());
        assertEquals(table.getSelectionBackground(), renderer.getBackground());
        assertEquals(table.getSelectionForeground(), renderer.getForeground());
        assertEquals(table.getFont(), renderer.getFont());
        assertSame(renderer, renderer.getTableCellRendererComponent(table, "any value", true,
                true, -1, -1));
        assertEquals("any value", renderer.getText());
        assertSame(UIManager.getBorder("Table.focusCellHighlightBorder"), renderer.getBorder());
        assertEquals(table.getSelectionBackground(), renderer.getBackground());
        assertEquals(table.getSelectionForeground(), renderer.getForeground());
        assertEquals(table.getFont(), renderer.getFont());
        table.addColumn(new TableColumn());
        assertSame(renderer, renderer.getTableCellRendererComponent(table, "any value", true,
                true, 0, 0));
        assertEquals("any value", renderer.getText());
        assertSame(UIManager.getBorder("Table.focusCellHighlightBorder"), renderer.getBorder());
        assertEquals(table.getSelectionBackground(), renderer.getBackground());
        assertEquals(table.getSelectionForeground(), renderer.getForeground());
        assertEquals(table.getFont(), renderer.getFont());
        table.setModel(new DefaultTableModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(final int row, final int column) {
                return false;
            }
        });
        table.addColumn(new TableColumn());
        assertSame(renderer, renderer.getTableCellRendererComponent(table, "any value", true,
                true, 0, 0));
        assertEquals("any value", renderer.getText());
        assertSame(UIManager.getBorder("Table.focusCellHighlightBorder"), renderer.getBorder());
        assertEquals(table.getSelectionBackground(), renderer.getBackground());
        assertEquals(table.getSelectionForeground(), renderer.getForeground());
        assertEquals(table.getFont(), renderer.getFont());

        // regression test for HARMONY-1721
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        Color c = renderer.getBackground();
        assertNull(c);
    }

    public void testIsOpaque() throws Exception {
        assertTrue(renderer.isOpaque());
    }

    public void testFirePropertyChange() throws Exception {
        PropertyChangeController controller = new PropertyChangeController();
        renderer.addPropertyChangeListener(controller);
        renderer.firePropertyChange("any", "a", "b");
        assertFalse(controller.isChanged());
        renderer.firePropertyChange("any", true, false);
        assertFalse(controller.isChanged());
    }

    public void testSetText() throws Exception {
        assertEquals("", renderer.getText());
        renderer.setValue(null);
        assertEquals("", renderer.getText());
        renderer.setValue("any");
        assertEquals("any", renderer.getText());
        renderer.setValue(new Integer("4"));
        assertEquals("4", renderer.getText());
    }

    public void testGetName() {
        DefaultTableCellRenderer cr = new DefaultTableCellRenderer();
        assertNull(cr.getName());
    }
}
