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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TableHeaderUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.harmony.x.swing.Utilities;


public class BasicTableHeaderUI extends TableHeaderUI {
    public class MouseInputHandler implements MouseInputListener {
        private final Cursor HEADER_RESIZING_CURSOR = new Cursor(Cursor.E_RESIZE_CURSOR);

        private Cursor originalHeaderCursor;
        private int initialMousePosition;
        private int initialColumnWidth;

        public void mouseClicked(final MouseEvent e) {
        }

        public void mousePressed(final MouseEvent e) {
            initialMousePosition = e.getX();
            TableColumn processedColumn = getResizingColumn(e);
            if (processedColumn != null) {
                header.setResizingColumn(processedColumn);
                initialColumnWidth = processedColumn.getWidth();
                return;
            }
            processedColumn = getReorderingColumn(e);
            if (processedColumn != null) {
                header.setDraggedColumn(processedColumn);
                dragColumn(e);
            }
        }

        public void mouseReleased(final MouseEvent e) {
            if (header.getResizingColumn() != null) {
                header.setResizingColumn(null);
                header.setCursor(originalHeaderCursor);
            } else if (header.getDraggedColumn() != null) {
                header.setDraggedDistance(0);
                int draggingColumnIndex = getColumnIndex(header.getDraggedColumn());
                header.getColumnModel().moveColumn(draggingColumnIndex, draggingColumnIndex);
                header.setDraggedColumn(null);
            }
        }


        public void mouseMoved(final MouseEvent e) {
            updateCursor(e);
        }

        public void mouseDragged(final MouseEvent e) {
            if (header.getResizingColumn() != null) {
                int increment;
                if (header.getTable().getComponentOrientation().isLeftToRight()) {
                    increment = e.getX() - initialMousePosition;
                } else {
                    increment = initialMousePosition - e.getX();
                }
                header.setCursor(HEADER_RESIZING_CURSOR);
                header.getResizingColumn().setWidth(initialColumnWidth + increment);
            } else if (header.getDraggedColumn() != null) {
                dragColumn(e);
            }
        }

        public void mouseEntered(final MouseEvent e) {
            if (header != null) {
                updateCursor(e);
            }
        }

        public void mouseExited(final MouseEvent e) {
            if (header != null) {
                header.setCursor(originalHeaderCursor);
            }
        }


        private void dragColumn(final MouseEvent e) {
            int increment = e.getX() - initialMousePosition;
            int draggingColumnIndex = getColumnIndex(header.getDraggedColumn());
            Rectangle draggingColumnRect = header.getHeaderRect(draggingColumnIndex);
            if (increment > 0) {
                int draggingFront = draggingColumnRect.x + draggingColumnRect.width + increment;
                int swappingColumnIndex = header.columnAtPoint(new Point(draggingFront, 0));
                if (swappingColumnIndex == -1) {
                    swappingColumnIndex = header.getColumnModel().getColumnCount() - 1;
                }
                Rectangle swappingColumnRect = header.getHeaderRect(swappingColumnIndex);
                int swappingColumnCenter = swappingColumnRect.x + swappingColumnRect.width / 2;
                if (draggingColumnIndex != swappingColumnIndex && draggingFront >= swappingColumnCenter) {
                    int distance = increment - swappingColumnRect.width;
                    header.setDraggedDistance(distance);
                    initialMousePosition = e.getX() - distance;
                    header.getColumnModel().moveColumn(draggingColumnIndex, swappingColumnIndex);
                } else {
                    header.setDraggedDistance(increment);
                    header.getColumnModel().moveColumn(draggingColumnIndex, draggingColumnIndex);
                }
            } else {
                int draggingFront = draggingColumnRect.x + increment;
                int swappingColumnIndex = header.columnAtPoint(new Point(draggingFront, 0));
                if (swappingColumnIndex == -1) {
                    swappingColumnIndex = 0;
                }
                Rectangle swappingColumnRect = header.getHeaderRect(swappingColumnIndex);
                int swappingColumnCenter = swappingColumnRect.x + swappingColumnRect.width / 2;
                if (draggingColumnIndex != swappingColumnIndex && draggingFront <= swappingColumnCenter) {
                    int distance = swappingColumnRect.width + increment;
                    header.setDraggedDistance(distance);
                    initialMousePosition = e.getX() - distance;
                    header.getColumnModel().moveColumn(draggingColumnIndex, swappingColumnIndex);
                } else {
                    header.setDraggedDistance(increment);
                    header.getColumnModel().moveColumn(draggingColumnIndex, draggingColumnIndex);
                }
            }
        }

        private TableColumn getResizingColumn(final MouseEvent e) {
            if (!header.getResizingAllowed()) {
                return null;
            }

            int column = header.columnAtPoint(e.getPoint());
            if (column == -1) {
                return null;
            }

            Rectangle columnBounds = header.getHeaderRect(column);
            if (header.getTable().getComponentOrientation().isLeftToRight()) {
                if (column == 0 && columnBounds.x + MOUSE_TOLERANCE > e.getX()) {
                    return null;
                }

                if (columnBounds.x + MOUSE_TOLERANCE > e.getX()) {
                    TableColumn result = header.getColumnModel().getColumn(column - 1);
                    return result.getResizable() ? result : null;
                }
                if (columnBounds.x + columnBounds.width - MOUSE_TOLERANCE < e.getX()) {
                    TableColumn result = header.getColumnModel().getColumn(column);
                    return result.getResizable() ? result : null;
                }
            } else {
                if (column == 0 && columnBounds.x  + columnBounds.width - MOUSE_TOLERANCE < e.getX()) {
                    return null;
                }

                if (columnBounds.x  + columnBounds.width - MOUSE_TOLERANCE < e.getX()) {
                    TableColumn result = header.getColumnModel().getColumn(column - 1);
                    return result.getResizable() ? result : null;
                }
                if (columnBounds.x + MOUSE_TOLERANCE > e.getX()) {
                    TableColumn result = header.getColumnModel().getColumn(column);
                    return result.getResizable() ? result : null;
                }
            }

            return null;
        }

        private TableColumn getReorderingColumn(final MouseEvent e) {
            if (!header.getReorderingAllowed()) {
                return null;
            }

            int column = header.columnAtPoint(e.getPoint());
            if (column == -1) {
                return null;
            }

            return header.getColumnModel().getColumn(column);
        }

        private void updateCursor(final MouseEvent e) {
            if (e == null || e.getButton() > 0 || e.getModifiersEx() > 0) {
                return;
            }

            if (header.getCursor() != HEADER_RESIZING_CURSOR) {
                originalHeaderCursor = header.getCursor();
            }
            header.setCursor(getResizingColumn(e) != null && header.getDraggedColumn() == null ? HEADER_RESIZING_CURSOR : originalHeaderCursor);
        }
    }

    protected JTableHeader header;
    protected CellRendererPane rendererPane;
    protected MouseInputListener mouseInputListener;

    private static final int MOUSE_TOLERANCE = 3;

    private static final SizeInfo MINIMUM_WIDTH_INFO = new SizeInfo() {
        public int getWidth(final TableColumn column) {
            return column.getMinWidth();
        }

        public int getHeight(final TableColumn column, final JTableHeader header, final int columnIndex) {
            Component renderingComponent = getRenderingComponent(column, header, columnIndex);
            return renderingComponent != null ? renderingComponent.getMinimumSize().height : 0;
        }
    };
    private static final SizeInfo MAXIMUM_WIDTH_INFO = new SizeInfo() {
        public int getWidth(final TableColumn column) {
            return column.getMaxWidth();
        }

        public int getHeight(final TableColumn column, final JTableHeader header, final int columnIndex) {
            Component renderingComponent = getRenderingComponent(column, header, columnIndex);
            return renderingComponent != null ? renderingComponent.getMaximumSize().height : 0;
        }
    };
    private static final SizeInfo PREFERRED_WIDTH_INFO = new SizeInfo() {
        public int getWidth(final TableColumn column) {
            return column.getPreferredWidth();
        }

        public int getHeight(final TableColumn column, final JTableHeader header, final int columnIndex) {
            Component renderingComponent = getRenderingComponent(column, header, columnIndex);
            return renderingComponent != null ? renderingComponent.getPreferredSize().height : 0;
        }
    };


    public static ComponentUI createUI(final JComponent c) {
        return new BasicTableHeaderUI();
    }

    public void installUI(final JComponent c) {
        header = (JTableHeader)c;
        rendererPane = new CellRendererPane();
        rendererPane.setVisible(false);
        header.add(rendererPane);

        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(final JComponent c) {
        uninstallKeyboardActions();
        uninstallListeners();
        uninstallDefaults();

        rendererPane = null;
        header = null;
    }

    public void paint(final Graphics g, final JComponent c) {
        Rectangle clipRect = g.getClipBounds();
        for (int i = 0; i < header.getColumnModel().getColumnCount(); i++) {
            TableColumn column = header.getColumnModel().getColumn(i);
            Rectangle columnRect = header.getHeaderRect(i);
            if (header.getDraggedColumn() != column) {
                paintColumn(g, column, clipRect, columnRect, i);
            }
        }

        if (header.getDraggedColumn() != null) {
            int draggedIndex = getColumnIndex(header.getDraggedColumn());
            Rectangle columnRect = header.getHeaderRect(draggedIndex);
            paintBackgroundUnderDraggedCell(g, columnRect);
            columnRect.translate(header.getDraggedDistance(), 0);
            paintColumn(g, header.getDraggedColumn(), clipRect, columnRect, draggedIndex);
        }
    }

    public Dimension getMinimumSize(final JComponent c) {
        return getColumnSize(MINIMUM_WIDTH_INFO);
    }

    public Dimension getMaximumSize(final JComponent c) {
        return getColumnSize(MAXIMUM_WIDTH_INFO);
    }

    public Dimension getPreferredSize(final JComponent c) {
        return getColumnSize(PREFERRED_WIDTH_INFO);
    }


    protected void installDefaults() {
        LookAndFeel.installColorsAndFont(header, "TableHeader.background", "TableHeader.foreground", "TableHeader.font");

        LookAndFeel.installProperty(header, "opaque", Boolean.TRUE);
    }

    protected void uninstallDefaults() {
        if (header != null) {
            Utilities.uninstallColorsAndFont(header);
        }
    }

    protected void installListeners() {
        mouseInputListener = createMouseInputListener();
        if (mouseInputListener != null) {
            header.addMouseListener(mouseInputListener);
            header.addMouseMotionListener(mouseInputListener);
        }
    }

    protected void uninstallListeners() {
        header.removeMouseListener(mouseInputListener);
        header.removeMouseMotionListener(mouseInputListener);
        mouseInputListener = null;
    }

    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected MouseInputListener createMouseInputListener() {
        return new MouseInputHandler();
    }


    private void paintColumn(final Graphics g, final TableColumn column, final Rectangle clipRect, final Rectangle columnRect, final int columnIndex) {
        if (clipRect != null && !clipRect.intersects(columnRect)) {
            return;
        }

        TableCellRenderer renderer = column.getHeaderRenderer() != null ? column.getHeaderRenderer()
                                                                        : header.getDefaultRenderer();

        Component renderingComponent = renderer.getTableCellRendererComponent(header.getTable(), column.getHeaderValue(), false, false, -1, columnIndex);
        rendererPane.paintComponent(g, renderingComponent, header, columnRect);
    }

    private void paintBackgroundUnderDraggedCell(final Graphics g, final Rectangle columnRect) {
        g.setColor(header.getParent().getBackground());
        g.fillRect(columnRect.x, columnRect.y, columnRect.width, columnRect.height);
    }

    private Dimension getColumnSize(final SizeInfo info) {
        int width = 0;
        int height = 0;
        TableColumnModel model = header.getColumnModel();
        for (int i = 0; i < model.getColumnCount(); i++) {
            TableColumn column = model.getColumn(i);
            width += info.getWidth(column);
            int columnHeight = info.getHeight(column, header, i);
            if (height < columnHeight) {
                height = columnHeight;
            }
        }

        return new Dimension(width, height);
    }

    private int getColumnIndex(final TableColumn column) {
        for (int i = 0; i < header.getColumnModel().getColumnCount(); i++) {
            if (header.getColumnModel().getColumn(i) == column) {
                return i;
            }
        }
        return -1;
    }

    private static abstract class SizeInfo {
        protected Component getRenderingComponent(final TableColumn column, final JTableHeader header, final int columnIndex) {
            if (column.getHeaderValue() == null) {
                return null;
            }

            TableCellRenderer renderer = column.getHeaderRenderer() != null ? column.getHeaderRenderer()
                                                                          : header.getDefaultRenderer();

            return renderer.getTableCellRendererComponent(header.getTable(), column.getHeaderValue(), false, false, -1, columnIndex);
        }

        public abstract int getWidth(TableColumn column);
        public abstract int getHeight(TableColumn column, JTableHeader header, int columnIndex);
    }
}
