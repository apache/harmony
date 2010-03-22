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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.event.MouseWheelEvent;
import javax.swing.BasicSwingTestCase;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import org.apache.harmony.x.swing.Utilities;

public class BasicTableHeaderUITest extends BasicSwingTestCase {

    private BasicTableHeaderUI ui;

    private MouseWheelEvent mouseWheelEvent =
            new MouseWheelEvent(new Label(),
                    0, 0, 0, 0, 0, 0, false, 0,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 0);

    public BasicTableHeaderUITest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        ui = new BasicTableHeaderUI();
    }

    @Override
    protected void tearDown() throws Exception {
        ui = null;
    }

    public void testCreateMouseInputListener() throws Exception {
        assertSame(BasicTableHeaderUI.MouseInputHandler.class, ui.createMouseInputListener()
                .getClass());
        assertNotSame(ui.createMouseInputListener(), ui.createMouseInputListener());
    }

    public void testCreate() throws Exception {
        assertSame(BasicTableHeaderUI.class, BasicTableHeaderUI.createUI(null).getClass());
        assertNotSame(BasicTableHeaderUI.createUI(null), BasicTableHeaderUI.createUI(null));
        assertNull(((BasicTableHeaderUI) BasicTableHeaderUI.createUI(new JTableHeader())).header);
    }

    public void testInstallUninstallUI() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.uninstallUI(new JTable());
            }
        });
        assertNull(ui.header);
        assertNull(ui.rendererPane);
        JTableHeader header = new JTableHeader();
        ui.installUI(header);
        assertSame(header, ui.header);
        assertNotNull(ui.rendererPane);
        assertTrue(ui.header.isOpaque());
        ui.uninstallUI(null);
        assertNull(ui.header);
        assertNull(ui.rendererPane);
    }

    public void testInstallUninstallDefaults() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.installDefaults();
            }
        });

        // Regression test for HARMONY-2613
        // Make sure it throws no exceptions
        ui.uninstallDefaults();

        ui.header = new JTableHeader();
        ui.header.setForeground(new ColorUIResource(Color.RED));
        ui.header.setBackground(new ColorUIResource(Color.BLUE));
        ui.header.setFont(new FontUIResource(new Font("any", Font.BOLD, 20)));
        ui.installDefaults();
        assertEquals(UIManager.getColor("TableHeader.background"), ui.header.getBackground());
        assertEquals(UIManager.getColor("TableHeader.foreground"), ui.header.getForeground());
        assertEquals(UIManager.getFont("TableHeader.font"), ui.header.getFont());
        ui.uninstallDefaults();
        if (isHarmony()) {
            assertNull(ui.header.getForeground());
            assertNull(ui.header.getBackground());
            assertNull(ui.header.getFont());
        }
    }

    public void testInstallUninstallListeners() throws Exception {
        assertNull(ui.mouseInputListener);

        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.installListeners();
            }
        });
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.uninstallListeners();
            }
        });
        ui.header = new JTableHeader();
        int mouseListenersCount = ui.header.getMouseListeners().length;
        int mouseMotionListenersCount = ui.header.getMouseMotionListeners().length;
        ui.installListeners();
        assertEquals(mouseListenersCount + 1, ui.header.getMouseListeners().length);
        assertEquals(mouseMotionListenersCount + 1, ui.header.getMouseMotionListeners().length);
        assertNotNull(ui.mouseInputListener);
        ui.uninstallListeners();
        assertEquals(mouseListenersCount, ui.header.getMouseListeners().length);
        assertEquals(mouseMotionListenersCount, ui.header.getMouseMotionListeners().length);
        assertNull(ui.mouseInputListener);
    }

    public void testInstallUninstallKeyboardActions() throws Exception {
        ui.header = new JTableHeader();
        ui.installKeyboardActions();
        assertTrue(Utilities.isEmptyArray(ui.header.getActionMap().allKeys()));
    }

    public void testInstallKeyboardActions() throws Exception {
        // Regression test for HARMONY-2613
        // Make sure it throws no exceptions
        ui.installKeyboardActions();
    }

    public void testUninstallKeyboardActions() throws Exception {
        // Regression test for HARMONY-2613
        // Make sure it throws no exceptions
        ui.uninstallKeyboardActions();
    }

    public void testPaint() throws Exception {
        final Graphics g = createTestGraphics();

        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.paint(g, new JTable());
            }
        });
        ui.installUI(new JTableHeader());
        ui.header.getColumnModel().addColumn(new TableColumn());
        g.setClip(0, 0, 100, 100);
        ui.paint(g, null);
    }

    public void testGetMinimumMaximumPreferredSize() throws Exception {
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
        ui.header = new JTableHeader();
        assertEquals(new Dimension(), ui.getMinimumSize(null));
        assertEquals(new Dimension(), ui.getMaximumSize(null));
        assertEquals(new Dimension(), ui.getPreferredSize(null));
        TableColumn column1 = new TableColumn(0, 30);
        column1.setMinWidth(20);
        column1.setMaxWidth(60);
        ui.header.getColumnModel().addColumn(column1);
        assertEquals(new Dimension(20, 0), ui.getMinimumSize(null));
        assertEquals(new Dimension(60, 0), ui.getMaximumSize(null));
        assertEquals(new Dimension(30, 0), ui.getPreferredSize(null));
        TableColumn column2 = new TableColumn(0, 40);
        column2.setMinWidth(30);
        column2.setMaxWidth(100);
        ui.header.getColumnModel().addColumn(column2);
        assertEquals(new Dimension(50, 0), ui.getMinimumSize(null));
        assertEquals(new Dimension(160, 0), ui.getMaximumSize(null));
        assertEquals(new Dimension(70, 0), ui.getPreferredSize(null));
        column1.setHeaderValue("any");
        Component renderingComponent = ui.header.getDefaultRenderer()
                .getTableCellRendererComponent(null, "any", false, false, 0, 0);
        assertEquals(new Dimension(50, renderingComponent.getMinimumSize().height), ui
                .getMinimumSize(null));
        assertEquals(new Dimension(160, renderingComponent.getMaximumSize().height), ui
                .getMaximumSize(null));
        assertEquals(new Dimension(70, renderingComponent.getPreferredSize().height), ui
                .getPreferredSize(null));
    }

    public void testMouseInputHandlerMouseEnteredNull() throws Exception {
        // Regression test for HARMONY-1777
        new BasicTableHeaderUI().new MouseInputHandler().mouseEntered(null);
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
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.new MouseInputHandler().mousePressed(mouseWheelEvent);
            }
        });
    }

    public void testMouseInputHandlerMouseReleased() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.new MouseInputHandler().mouseReleased(mouseWheelEvent);
            }
        });
    }

    public void testMouseInputHandlerMouseDragged() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.new MouseInputHandler().mouseDragged(mouseWheelEvent);
            }
        });
    }

    public void testMouseInputHandlerMouseMoved() throws Exception {
        testExceptionalCase(new NullPointerCase() {
            // Regression test for HARMONY-2613
            @Override
            public void exceptionalAction() throws Exception {
                ui.new MouseInputHandler().mouseMoved(mouseWheelEvent);
            }
        });
    }
}
