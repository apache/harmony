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
package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.BasicSwingTestCase;
import javax.swing.CellRendererPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class BasicTableUITest extends BasicSwingTestCase {
    private BasicTableUI ui;

    private FocusEvent focusEvent = new FocusEvent(new JTable(), 0);

    private KeyEvent keyEvent = new KeyEvent(new JTable(), 0, 0, 0, 0);

    private MouseWheelEvent mouseWheelEvent =
            new MouseWheelEvent(new Label(),
                    0, 0, 0, 0, 0, 0, false, 0,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 0);

    public BasicTableUITest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        ui = new BasicTableUI();
    }

    @Override
    protected void tearDown() throws Exception {
        ui = null;
    }

    public void testBasicTableUI() throws Exception {
        assertNull(ui.table);
        assertNull(ui.rendererPane);
        assertNull(ui.focusListener);
        assertNull(ui.keyListener);
        assertNull(ui.mouseInputListener);
    }

    public void testCreateKeyListener() throws Exception {
        assertNull(ui.createKeyListener());
    }

    public void testCreateFocusListener() throws Exception {
        assertTrue(ui.createFocusListener() instanceof BasicTableUI.FocusHandler);
        assertNotSame(ui.createFocusListener(), ui.createFocusListener());
        assertNull(ui.focusListener);
    }

    public void testCreateMouseInputListener() throws Exception {
        assertTrue(ui.createMouseInputListener() instanceof BasicTableUI.MouseInputHandler);
        assertNotSame(ui.createMouseInputListener(), ui.createMouseInputListener());
        assertNull(ui.mouseInputListener);
    }

    public void testCreateUI() throws Exception {
        assertSame(BasicTableUI.createUI(null).getClass(), BasicTableUI.class);
        assertNotSame(BasicTableUI.createUI(null), BasicTableUI.createUI(null));
    }

    public void testInstallUI() throws Exception {
        JTable table = new JTable();
        ui.installUI(table);
        assertSame(table, ui.table);
        assertNotNull(ui.rendererPane);
    }

    public void testUninstallUI() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.uninstallUI(new JTable());
            }
        });
        JTable table = new JTable();
        ui.installUI(table);
        ui.uninstallUI(null);
        assertNull(ui.table);
        assertNull(ui.rendererPane);
    }

    public void testGetMinimumMaximumPreferredSize() throws Exception {
        JTable table = new JTable();
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.getMinimumSize(new JTable());
            }
        });
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.getMaximumSize(new JTable());
            }
        });
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.getPreferredSize(new JTable());
            }
        });
        ui.table = table;
        assertEquals(new Dimension(), ui.getMinimumSize(null));
        TableColumn column1 = new TableColumn();
        column1.setMinWidth(20);
        column1.setPreferredWidth(50);
        column1.setMaxWidth(100);
        table.addColumn(column1);
        assertEquals(new Dimension(20, 0), ui.getMinimumSize(null));
        assertEquals(new Dimension(100, 0), ui.getMaximumSize(null));
        assertEquals(new Dimension(50, 0), ui.getPreferredSize(null));
        TableColumn column2 = new TableColumn();
        column2.setMinWidth(10);
        column2.setPreferredWidth(20);
        column2.setMaxWidth(40);
        table.addColumn(column2);
        assertEquals(new Dimension(30, 0), ui.getMinimumSize(null));
        assertEquals(new Dimension(140, 0), ui.getMaximumSize(null));
        assertEquals(new Dimension(70, 0), ui.getPreferredSize(null));
        table.setRowHeight(30);
        ((DefaultTableModel) table.getModel()).addRow(new Object[] { "1" });
        assertEquals(new Dimension(30, 30), ui.getMinimumSize(null));
        assertEquals(new Dimension(140, 30), ui.getMaximumSize(null));
        assertEquals(new Dimension(70, 30), ui.getPreferredSize(null));
        ((DefaultTableModel) table.getModel()).addRow(new Object[] { "2", "2" });
        table.setRowHeight(1, 20);
        assertEquals(new Dimension(30, 50), ui.getMinimumSize(null));
        assertEquals(new Dimension(140, 50), ui.getMaximumSize(null));
        assertEquals(new Dimension(70, 50), ui.getPreferredSize(null));
    }

    public void testPaint() throws Exception {
        ui.table = new JTable();
        DefaultTableModel model = (DefaultTableModel) ui.table.getModel();
        model.addColumn("column1");
        model.addRow(new Object[] { "1" });
        ui.rendererPane = new CellRendererPane();
        Graphics g = createTestGraphics();
        g.setClip(0, 0, 100, 100);
        ui.paint(g, null);
    }

    public void testPaint_Null() throws Exception {
        try {
            // Regression test for HARMONY-1776
            ui.paint(null, null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }

        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.paint(createTestGraphics(), new JTable());
            }
        });
    }

    public void testInstallDefaults() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.installDefaults();
            }
        });
        ui.table = new JTable();
        ui.installDefaults();
        assertSame(UIManager.getFont("Table.font"), ui.table.getFont());
        assertSame(UIManager.getColor("Table.gridColor"), ui.table.getGridColor());
        assertSame(UIManager.getColor("Table.foreground"), ui.table.getForeground());
        assertSame(UIManager.getColor("Table.background"), ui.table.getBackground());
        assertSame(UIManager.getColor("Table.selectionForeground"), ui.table
                .getSelectionForeground());
        assertSame(UIManager.getColor("Table.selectionBackground"), ui.table
                .getSelectionBackground());
    }

    public void testInstallKeyboardActions() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.installKeyboardActions();
            }
        });
    }

    public void testUninstallDefaults() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.uninstallDefaults();
            }
        });
    }

    public void testUninstallKeyboardActions() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.uninstallKeyboardActions();
            }
        });
    }

    public void testInstallListeners() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.installListeners();
            }
        });
    }

    public void testUninstallListeners() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.uninstallListeners();
            }
        });
    }

    public void testFocusHandlerFocusGained() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.new FocusHandler().focusGained(focusEvent);
            }
        });
    }

    public void testFocusHandlerFocusLost() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.new FocusHandler().focusLost(focusEvent);
            }
        });
    }

    public void testKeyHandlerKeyPressed() throws Exception {
        // Regression test for HARMONY-2613
        // Make sure it throws no exceptions
        ui.new KeyHandler().keyPressed(keyEvent);
    }

    public void testKeyHandlerKeyReleased() throws Exception {
        // Regression test for HARMONY-2613
        // Make sure it throws no exceptions
        ui.new KeyHandler().keyReleased(keyEvent);
    }

    public void testFocusHandlerKeyTyped() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.new KeyHandler().keyTyped(keyEvent);
            }
        });
    }

    public void testMouseInputHandlerMouseClicked() throws Exception {
        // Regression test for HARMONY-2613
        // Make sure it throws no exceptions
        ui.new MouseInputHandler().mouseClicked(mouseWheelEvent);
    }

    public void testMouseInputHandlerMouseEntered() throws Exception {
        // Regression test for HARMONY-2613
        // Make sure it throws no exceptions
        ui.new MouseInputHandler().mouseEntered(mouseWheelEvent);
    }

    public void testMouseInputHandlerMouseExited() throws Exception {
        // Regression test for HARMONY-2613
        // Make sure it throws no exceptions
        ui.new MouseInputHandler().mouseExited(mouseWheelEvent);
    }

    public void testMouseInputHandlerMousePressed() throws Exception {
        // Regression test for HARMONY-2613
        // Make sure it throws no exceptions
        ui.new MouseInputHandler().mousePressed(mouseWheelEvent);
    }

    public void testMouseInputHandlerMouseReleased() throws Exception {
        // Regression test for HARMONY-2613
        // Make sure it throws no exceptions
        ui.new MouseInputHandler().mouseReleased(mouseWheelEvent);
    }

    public void testMouseInputHandlerMouseDragged() throws Exception {
        // Regression test for HARMONY-2613
        // Make sure it throws no exceptions
        ui.new MouseInputHandler().mouseDragged(mouseWheelEvent);
    }

    public void testMouseInputHandlerMouseMoved() throws Exception {
        // Regression test for HARMONY-2613
        // Make sure it throws no exceptions
        ui.new MouseInputHandler().mouseMoved(mouseWheelEvent);
    }
}
