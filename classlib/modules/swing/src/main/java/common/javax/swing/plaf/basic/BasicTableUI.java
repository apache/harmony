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
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TableUI;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class BasicTableUI extends TableUI {
    public class FocusHandler implements FocusListener {
        public void focusGained(final FocusEvent e) {
            repaintFocusedCell();
        }

        public void focusLost(final FocusEvent e) {
            repaintFocusedCell();
        }

        private void repaintFocusedCell() {
            int focusedRow = table.getSelectionModel().getLeadSelectionIndex();
            int focusedColumn = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();

            if (focusedRow != -1 && focusedColumn != -1) {
                table.repaint(table.getCellRect(focusedRow, focusedColumn, false));
            }
        }
    }

    public class KeyHandler implements KeyListener {
        public void keyPressed(final KeyEvent e) {
        }

        public void keyReleased(final KeyEvent e) {
        }

        public void keyTyped(final KeyEvent e) {
            if (table.getActionForKeyStroke(KeyStroke.getKeyStrokeForEvent(e)) != null) {
                return;
            }

            if (!table.isEditing()) {
                int currentRow = table.getSelectionModel().getLeadSelectionIndex();
                int currentColumn = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
                if (currentRow == -1 || currentColumn == -1) {
                    return;
                }
                if (!table.editCellAt(currentRow, currentColumn, e)) {
                    return;
                }
            }

            if (table.isEditing()) {
                e.consume();
                KeyEvent editorEvent = new KeyEvent(table.getEditorComponent(), e.getID(), e.getWhen(), e.getModifiers(), e.getKeyCode(), e.getKeyChar());
                table.getEditorComponent().dispatchEvent(editorEvent);
            }
        }
    }

    public class MouseInputHandler implements MouseInputListener {
        private DnDMouseHelper dndhelper = new DnDMouseHelper(table);

        public void mouseClicked(final MouseEvent e) {
        }

        public void mouseEntered(final MouseEvent e) {
        }

        public void mouseExited(final MouseEvent e) {
        }

        public void mousePressed(final MouseEvent e) {
            if (table == null || !table.isEnabled()) {
                return;
            }

            table.requestFocus();

            int eventRow = table.rowAtPoint(e.getPoint());
            if (eventRow == -1) {
                return;
            }

            int eventColumn = table.columnAtPoint(e.getPoint());
            if (eventColumn == -1) {
                return;
            }

            dndhelper.mousePressed(e, table.getDragEnabled(),
                                   eventRow != -1 && eventColumn != -1,
                                   table.isCellSelected(eventRow, eventColumn));

            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }

            if (!table.getDragEnabled()) {
                table.getSelectionModel().setValueIsAdjusting(true);
                table.getColumnModel().getSelectionModel().setValueIsAdjusting(true);
                if (table.editCellAt(eventRow, eventColumn, e)) {
                    if (table.getCellEditor(eventRow, eventColumn).shouldSelectCell(e)) {
                        changeSelection(e);
                    }
                    forwardEventToEditor(e);
                } else {
                    changeSelection(e);
                }
            } else if (!table.isCellSelected(eventRow, eventColumn)) {
                changeSelection(e);
            }
        }

        public void mouseReleased(final MouseEvent e) {
            if (table == null || !table.isEnabled()) {
                return;
            }

            dndhelper.mouseReleased(e);
            if (table.isEditing()) {
                forwardEventToEditor(e);
            }
            if (dndhelper.shouldProcessOnRelease()) {
                changeSelection(e);
            }
            table.getSelectionModel().setValueIsAdjusting(false);
            table.getColumnModel().getSelectionModel().setValueIsAdjusting(false);
        }

        public void mouseDragged(final MouseEvent e) {
            if (table == null || !table.isEnabled()) {
                return;
            }

            dndhelper.mouseDragged(e);

            if (SwingUtilities.isLeftMouseButton(e)
                && !dndhelper.isDndStarted()) {

                table.getSelectionModel().setValueIsAdjusting(true);
                table.getColumnModel().getSelectionModel().setValueIsAdjusting(true);

                changeSelection(e.getPoint(), false, true);
            }
        }

        public void mouseMoved(final MouseEvent e) {
        }


        private void changeSelection(final Point p, final boolean toggle, final boolean extend) {
            int clickRow = table.rowAtPoint(p);
            if (clickRow == -1) {
                return;
            }

            int clickColumn = table.columnAtPoint(p);
            if (clickColumn == -1) {
                return;
            }

            table.changeSelection(clickRow, clickColumn, toggle, extend);

        }

        private void changeSelection(final MouseEvent e) {
            boolean toggle = false;
            boolean extend = false;
            if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                toggle = true;
            }
            if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
                extend = true;
            }

            changeSelection(e.getPoint(), toggle, extend);
        }

        private void forwardEventToEditor(final MouseEvent e) {
            table.getEditorComponent().dispatchEvent(SwingUtilities.convertMouseEvent(table, e, table.getEditorComponent()));
        }
    }

    private class TableTransferHandler extends TransferHandler {
        private final String lineSeparator = System.getProperty("line.separator");

        public int getSourceActions(final JComponent c) {
            return COPY;
        }

        protected Transferable createTransferable(final JComponent c) {
            if (table.getSelectedColumnCount() == 0 || table.getSelectedRowCount() == 0) {
                return null;
            }

            StringBuilder content = new StringBuilder();
            int rowMinSelectionIndex = table.getSelectionModel().getMinSelectionIndex();
            int rowMaxSelectionIndex = table.getSelectionModel().getMaxSelectionIndex();
            int colMinSelectionIndex = table.getColumnModel().getSelectionModel().getMinSelectionIndex();
            int colMaxSelectionIndex = table.getColumnModel().getSelectionModel().getMaxSelectionIndex();
            for (int i = rowMinSelectionIndex; i <= rowMaxSelectionIndex; i++) {
                for (int j = colMinSelectionIndex; j <= colMaxSelectionIndex; j++) {
                    if (table.getSelectionModel().isSelectedIndex(i) && table.getColumnModel().getSelectionModel().isSelectedIndex(j)) {
                        String value = table.getValueAt(i, j) == null ? "\t" : table.getValueAt(i, j).toString() + "\t";
                        content.append(value);
                    }
                }
                if (i < colMaxSelectionIndex) {
                    content.append(lineSeparator);
                }
            }

            return new StringSelection(content.toString());
        }
    }



    protected JTable table;
    protected CellRendererPane rendererPane;
    protected KeyListener keyListener;
    protected FocusListener focusListener;
    protected MouseInputListener mouseInputListener;

    private static final WidthInfo MINIMUM_WIDTH = new WidthInfo() {
        public int getWidth(final TableColumn column) {
            return column.getMinWidth();
        }
    };
    private static final WidthInfo MAXIMUM_WIDTH = new WidthInfo() {
        public int getWidth(final TableColumn column) {
            return column.getMaxWidth();
        }
    };
    private static final WidthInfo PREFERRED_WIDTH = new WidthInfo() {
        public int getWidth(final TableColumn column) {
            return column.getPreferredWidth();
        }
    };


    public static ComponentUI createUI(final JComponent c) {
        return new BasicTableUI();
    }

    public void installUI(final JComponent c) {
        table = (JTable)c;
        rendererPane = new CellRendererPane();
        rendererPane.setVisible(false);
        table.add(rendererPane);

        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(final JComponent c) {
        uninstallKeyboardActions();
        uninstallListeners();
        uninstallDefaults();

        rendererPane = null;
        table = null;
    }

    public Dimension getMinimumSize(final JComponent c) {
        return getSize(MINIMUM_WIDTH);
    }

    public Dimension getPreferredSize(final JComponent c) {
        return getSize(PREFERRED_WIDTH);
    }

    public Dimension getMaximumSize(final JComponent c) {
        return getSize(MAXIMUM_WIDTH);
    }

    public void paint(final Graphics g, final JComponent c) {
        if (g == null) {
            throw new NullPointerException(Messages.getString("swing.03","context")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (table.getColumnCount() == 0 || table.getRowCount() == 0) {
            return;
        }

        Color oldColor = g.getColor();
        paintCells(g);
        g.setColor(oldColor);
    }


    protected void installDefaults() {
        LookAndFeel.installColorsAndFont(table, "Table.background", "Table.foreground", "Table.font");
        if (Utilities.isUIResource(table.getSelectionBackground())) {
            table.setSelectionBackground(UIManager.getColor("Table.selectionBackground"));
        }
        if (Utilities.isUIResource(table.getSelectionForeground())) {
            table.setSelectionForeground(UIManager.getColor("Table.selectionForeground"));
        }
        if (Utilities.isUIResource(table.getGridColor())) {
            table.setGridColor(UIManager.getColor("Table.gridColor"));
        }

        LookAndFeel.installProperty(table, "opaque", Boolean.TRUE);

        table.setTransferHandler(new TableTransferHandler());
    }

    protected void uninstallDefaults() {
        Utilities.uninstallColorsAndFont(table);

        table.setTransferHandler(null);
    }

    protected void installListeners() {
        keyListener = createKeyListener();
        if (keyListener != null) {
            table.addKeyListener(keyListener);
        }

        focusListener = createFocusListener();
        if (focusListener != null) {
            table.addFocusListener(focusListener);
        }

        mouseInputListener = createMouseInputListener();
        if (mouseInputListener != null) {
            table.addMouseListener(mouseInputListener);
            table.addMouseMotionListener(mouseInputListener);
        }
    }

    protected void uninstallListeners() {
        table.removeKeyListener(keyListener);
        table.removeFocusListener(focusListener);
        table.removeMouseListener(mouseInputListener);
        table.removeMouseMotionListener(mouseInputListener);
    }

    protected void installKeyboardActions() {
        BasicTableKeyboardActions.installKeyboardActions(table);

        Set forwardSet = new HashSet();
        forwardSet.add(KeyStroke.getKeyStroke("ctrl pressed TAB"));
        table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardSet);

        Set backwardSet = new HashSet();
        forwardSet.add(KeyStroke.getKeyStroke("shift ctrl pressed TAB"));
        table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backwardSet);
    }

    protected void uninstallKeyboardActions() {
        BasicTableKeyboardActions.uninstallKeyboardActions(table);
        table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
    }

    protected KeyListener createKeyListener() {
        return null;
    }

    protected FocusListener createFocusListener() {
        return new FocusHandler();
    }

    protected MouseInputListener createMouseInputListener() {
        return new MouseInputHandler();
    }

    private void paintCells(final Graphics g) {
        Rectangle clipBounds = g.getClipBounds();
        int draggedColumn = -1;
        if (table.getTableHeader() != null && table.getTableHeader().getDraggedColumn() != null) {
            draggedColumn = getColumnIndex(table.getTableHeader().getDraggedColumn());
        }
        for (int column = 0; column < table.getColumnCount(); column++) {
            if (column != draggedColumn) {
                for (int row = 0; row < table.getRowCount(); row++) {
                    Rectangle cellRect = table.getCellRect(row, column, false);
                    Rectangle gridRect = table.getCellRect(row, column, true);
                    if (clipBounds == null || clipBounds.intersects(gridRect)) {
                        paintCell(g, row, column, cellRect, gridRect);
                        paintGrid(g, row, column, gridRect);
                    }
                }
            }
        }
        if (draggedColumn != -1) {
            for (int row = 0; row < table.getRowCount(); row++) {
                Rectangle cellRect = table.getCellRect(row, draggedColumn, false);
                Rectangle gridRect = table.getCellRect(row, draggedColumn, true);

                paintBackgroundUnderDraggedCell(g, gridRect);
                cellRect.translate(table.getTableHeader().getDraggedDistance(), 0);
                gridRect.translate(table.getTableHeader().getDraggedDistance(), 0);
                if (clipBounds == null || clipBounds.intersects(gridRect)) {
                    paintCell(g, row, draggedColumn, cellRect, gridRect);
                    paintGrid(g, row, draggedColumn, gridRect);
                }
            }
        }
    }

    private void paintCell(final Graphics g, final int row, final int column, final Rectangle cellRect, final Rectangle gridRect) {
        boolean isFocused = table.isFocusOwner() && table.getSelectionModel().getLeadSelectionIndex() == row && table.getColumnModel().getSelectionModel().getLeadSelectionIndex() == column;
        Component renderingComponent = table.getCellRenderer(row, column).getTableCellRendererComponent(table, table.getValueAt(row, column), table.isCellSelected(row, column), isFocused, row, column);
        g.setColor(table.getBackground());
        g.fillRect(gridRect.x, gridRect.y, gridRect.width, gridRect.height);
        rendererPane.paintComponent(g, renderingComponent, table, cellRect);
    }

    private void paintGrid(final Graphics g, final int row, final int column, final Rectangle gridRect) {
        if (!table.getShowHorizontalLines() && !table.getShowVerticalLines()) {
            return;
        }

        g.setColor(table.getGridColor());
        if (table.getShowHorizontalLines()) {
          g.drawLine(gridRect.x - 1, gridRect.y - 1, gridRect.x + gridRect.width - 1, gridRect.y - 1);
          g.drawLine(gridRect.x - 1, gridRect.y + gridRect.height - 1, gridRect.x + gridRect.width - 1, gridRect.y + gridRect.height - 1);
        }
        if (table.getShowVerticalLines()) {
          g.drawLine(gridRect.x - 1, gridRect.y - 1, gridRect.x - 1, gridRect.y + gridRect.height - 1);
          g.drawLine(gridRect.x + gridRect.width - 1, gridRect.y - 1, gridRect.x + gridRect.width - 1, gridRect.y + gridRect.height - 1);
        }
    }

    private void paintBackgroundUnderDraggedCell(final Graphics g, final Rectangle gridRect) {
        g.setColor(table.getParent().getBackground());
        g.fillRect(gridRect.x, gridRect.y, gridRect.width, gridRect.height);
    }

    private int getColumnIndex(final TableColumn column) {
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            if (table.getColumnModel().getColumn(i) == column) {
                return i;
            }
        }
        return table.getColumnModel().getColumnIndex(column.getIdentifier());
    }


    private Dimension getSize(final WidthInfo info) {
        return new Dimension(getWidth(info), getHeight());
    }

    private int getHeight() {
        int result = 0;
        for (int i = 0; i < table.getRowCount(); i++) {
            result += table.getRowHeight(i);
        }

        return result;
    }

    private int getWidth(final WidthInfo info) {
        int result = 0;
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            result += info.getWidth(columnModel.getColumn(i));
        }

        return result;
    }

    private interface WidthInfo {
        int getWidth(final TableColumn column);
    }
}
