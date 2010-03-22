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

import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;

import org.apache.harmony.x.swing.Utilities;


final class BasicTableKeyboardActions {
    private static abstract class AbstractTableAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            JTable table = (JTable)e.getSource();
            if (table.getRowCount() == 0 || table.getColumnCount() == 0) {
                return;
            }

            processTable(table);
        }

        protected abstract void processTable(final JTable table);
    }

    private static abstract class AbstractRowColumnAction extends AbstractTableAction {
        protected abstract void processRowColumn(final JTable table, final int currentRow, final int currentColumn);
    }

    private static abstract class LeadRowColumnAction extends AbstractRowColumnAction {
        protected void processTable(final JTable table) {
            if (!stopEditing(table)) {
                return;
            }
            int currentRow = table.getSelectionModel().getLeadSelectionIndex();
            int currentColumn = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
            processRowColumn(table, currentRow, currentColumn);
        }

        private boolean stopEditing(final JTable table) {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
                if (table.isEditing()) {
                    return false;
                }
            }

            return true;
        }
    }

    private static abstract class EnablebableLeadRowColumnAction extends LeadRowColumnAction {
        final JTable table;

        public EnablebableLeadRowColumnAction(final JTable table) {
            this.table = table;
        }

        public boolean isEnabled() {
            return isEnabled(table, table.getSelectionModel().getLeadSelectionIndex(),
                             table.getColumnModel().getSelectionModel().getLeadSelectionIndex());
        }

        public abstract boolean isEnabled(final JTable table, final int currentRow, final int currentColumn);
    }

    private static AbstractAction selectNextColumnAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentColumn + 1 < table.getColumnCount()) {
                table.changeSelection(currentRow, currentColumn + 1, false, false);
            }
        }
    };
    private static AbstractAction selectNextColumnChangeLeadAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentColumn + 1 < table.getColumnCount() && (table.getColumnModel().getSelectionModel() instanceof DefaultListSelectionModel)) {
                ((DefaultListSelectionModel)table.getColumnModel().getSelectionModel()).moveLeadSelectionIndex(currentColumn + 1);
                ensureCellIsVisible(table, currentRow, currentColumn + 1);
            }
        }
    };
    private static AbstractAction selectPreviousColumnAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentColumn > 0) {
                table.changeSelection(currentRow, currentColumn - 1, false, false);
            }
        }
    };
    private static AbstractAction selectPreviousColumnChangeLeadAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentColumn > 0 && (table.getColumnModel().getSelectionModel() instanceof DefaultListSelectionModel)) {
                ((DefaultListSelectionModel)table.getColumnModel().getSelectionModel()).moveLeadSelectionIndex(currentColumn - 1);
                ensureCellIsVisible(table, currentRow, currentColumn - 1);
            }
        }
    };
    private static AbstractAction selectNextRowAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentRow + 1 < table.getRowCount()) {
                table.changeSelection(currentRow + 1, currentColumn, false, false);
            }
        }
    };
    private static AbstractAction selectNextRowChangeLeadAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentRow + 1 < table.getRowCount() && (table.getSelectionModel() instanceof DefaultListSelectionModel)) {
                ((DefaultListSelectionModel)table.getSelectionModel()).moveLeadSelectionIndex(currentRow + 1);
                ensureCellIsVisible(table, currentRow + 1, currentColumn);
            }
        }
    };
    private static AbstractAction selectPreviousRowAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentRow > 0) {
                table.changeSelection(currentRow - 1, currentColumn, false, false);
            }
        }
    };
    private static AbstractAction selectPreviousRowChangeLeadAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentRow > 0 && (table.getSelectionModel() instanceof DefaultListSelectionModel)) {
                ((DefaultListSelectionModel)table.getSelectionModel()).moveLeadSelectionIndex(currentRow - 1);
                ensureCellIsVisible(table, currentRow - 1, currentColumn);
            }
        }
    };

    private static AbstractAction selectNextColumnExtendSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentColumn + 1 < table.getColumnCount()) {
                table.changeSelection(currentRow, currentColumn + 1, false, true);
            }
        }
    };
    private static AbstractAction selectPreviousColumnExtendSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentColumn > 0) {
                table.changeSelection(currentRow, currentColumn - 1, false, true);
            }
        }
    };
    private static AbstractAction selectNextRowExtendSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentRow + 1 < table.getRowCount()) {
                table.changeSelection(currentRow + 1, currentColumn, false, true);
            }
        }
    };
    private static AbstractAction selectPreviousRowExtendSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentRow > 0) {
                table.changeSelection(currentRow - 1, currentColumn, false, true);
            }
        }
    };

    private static AbstractAction selectFirstColumnAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            table.changeSelection(currentRow, 0, false, false);
        }
    };
    private static AbstractAction selectLastColumnAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            table.changeSelection(currentRow, table.getColumnCount() - 1, false, false);
        }
    };
    private static AbstractAction selectFirstRowAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            table.changeSelection(0, currentColumn, false, false);
        }
    };
    private static AbstractAction selectLastRowAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            table.changeSelection(table.getRowCount() - 1, currentColumn, false, false);
        }
    };

    private static AbstractAction selectFirstColumnExtendSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            table.changeSelection(currentRow, 0, false, true);
        }
    };
    private static AbstractAction selectLastColumnExtendSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            table.changeSelection(currentRow, table.getColumnCount() - 1, false, true);
        }
    };
    private static AbstractAction selectFirstRowExtendSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            table.changeSelection(0, currentColumn, false, true);
        }
    };
    private static AbstractAction selectLastRowExtendSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            table.changeSelection(table.getRowCount() - 1, currentColumn, false, true);
        }
    };
    private static AbstractAction toggleAndAnchorAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentRow >= 0 && currentColumn >=0) {
                table.changeSelection(currentRow, currentColumn, true, false);
                table.getSelectionModel().setAnchorSelectionIndex(currentRow);
                table.getColumnModel().getSelectionModel().setAnchorSelectionIndex(currentColumn);
            }
        }
    };
    private static AbstractAction moveSelectionToAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentRow >= 0 && currentColumn >=0) {
                table.changeSelection(currentRow, currentColumn, false, false);
            }
        }
    };
    private static AbstractAction extendToAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (currentRow >= 0 && currentColumn >=0) {
                table.changeSelection(currentRow, currentColumn, false, true);
            }
        }
    };


    private static abstract class SelectRowColumnCellAction extends LeadRowColumnAction {
        protected abstract int[] nextCellCoords(final int[] cell, final int minRow, final int maxRow,
                                                final int minColumn, final int maxColumn);

        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            ListSelectionModel rowSelectionModel = table.getSelectionModel();
            ListSelectionModel colSelectionModel = table.getColumnModel().getSelectionModel();
            if (rowSelectionModel.getMinSelectionIndex() == rowSelectionModel.getMaxSelectionIndex()
                && colSelectionModel.getMinSelectionIndex() == colSelectionModel.getMaxSelectionIndex()) {

                int[] currentCell = new int[] { currentRow, currentColumn };
                currentCell = nextCellCoords(currentCell, 0, table.getRowCount() - 1, 0, table.getColumnCount() - 1);
                table.changeSelection(currentCell[0], currentCell[1], false, false);
            } else {
                int[] currentCell = new int[] { currentRow, currentColumn };
                int rowMinSelectionIndex;
                int rowMaxSelectionIndex;
                int colMinSelectionIndex;
                int colMaxSelectionIndex;
                
                if (table.getRowSelectionAllowed() && table.getColumnSelectionAllowed()) {
                    rowMinSelectionIndex = rowSelectionModel.getMinSelectionIndex();
                    rowMaxSelectionIndex = rowSelectionModel.getMaxSelectionIndex();
                    colMinSelectionIndex = colSelectionModel.getMinSelectionIndex();
                    colMaxSelectionIndex = colSelectionModel.getMaxSelectionIndex();
                } else if (table.getRowSelectionAllowed() && !table.getColumnSelectionAllowed()) {
                    rowMinSelectionIndex = rowSelectionModel.getMinSelectionIndex();
                    rowMaxSelectionIndex = rowSelectionModel.getMaxSelectionIndex();
                    colMinSelectionIndex = 0;
                    colMaxSelectionIndex = table.getColumnCount() - 1;
                } else if (!table.getRowSelectionAllowed() && table.getColumnSelectionAllowed()) {
                    rowMinSelectionIndex = 0;
                    rowMaxSelectionIndex = table.getRowCount() - 1;
                    colMinSelectionIndex = colSelectionModel.getMinSelectionIndex();
                    colMaxSelectionIndex = colSelectionModel.getMaxSelectionIndex();
                } else {
                    rowMinSelectionIndex = 0;
                    rowMaxSelectionIndex = table.getRowCount() - 1;
                    colMinSelectionIndex = 0;
                    colMaxSelectionIndex = table.getColumnCount() - 1;
                }
                
                do {
                    currentCell = nextCellCoords(currentCell, rowMinSelectionIndex, rowMaxSelectionIndex,
                                                 colMinSelectionIndex, colMaxSelectionIndex);
                    
                    if (!table.getRowSelectionAllowed() && !table.getColumnSelectionAllowed()) {
                        break;
                    }
                } while (!table.isCellSelected(currentCell[0], currentCell[1]));
                
                colSelectionModel.addSelectionInterval(currentCell[1], currentCell[1]);
                rowSelectionModel.addSelectionInterval(currentCell[0], currentCell[0]);
            }
        }
    }

    private static AbstractAction selectNextColumnCellAction = new SelectRowColumnCellAction() {
        protected int[] nextCellCoords(final int[] cell, final int minRow, final int maxRow, final int minColumn, final int maxColumn) {
            if (cell[1] + 1 <= maxColumn) {
                cell[1]++;
            } else {
                cell[1] = minColumn;
                if (cell[0] + 1 <= maxRow) {
                    cell[0]++;
                } else {
                    cell[0] = minRow;
                }
            }

            return cell;
        }
    };
    private static AbstractAction selectPreviousColumnCellAction = new SelectRowColumnCellAction() {
        protected int[] nextCellCoords(final int[] cell, final int minRow, final int maxRow, final int minColumn, final int maxColumn) {
            if (cell[1] > minColumn) {
                cell[1]--;
            } else {
                cell[1] = maxColumn;
                if (cell[0] > minRow) {
                    cell[0]--;
                } else {
                    cell[0] = maxRow;
                }
            }

            return cell;
        }
    };
    private static AbstractAction selectNextRowCellAction = new SelectRowColumnCellAction() {
        protected int[] nextCellCoords(final int[] cell, final int minRow, final int maxRow, final int minColumn, final int maxColumn) {
            if (cell[0] + 1 <= maxRow) {
                cell[0]++;
            } else {
                cell[0] = minRow;
                if (cell[1] + 1 <= maxColumn) {
                    cell[1]++;
                } else {
                    cell[1] = minColumn;
                }
            }

            return cell;
        }
    };
    private static AbstractAction selectPreviousRowCellAction = new SelectRowColumnCellAction() {
        protected int[] nextCellCoords(final int[] cell, final int minRow, final int maxRow, final int minColumn, final int maxColumn) {
            if (cell[0] > minRow) {
                cell[0]--;
            } else {
                cell[0] = maxRow;
                if (cell[1] > minColumn) {
                    cell[1]--;
                } else {
                    cell[1] = maxColumn;
                }
            }

            return cell;
        }
    };

    private static AbstractAction selectAllAction = new AbstractTableAction() {
        protected void processTable(final JTable table) {
            if (table.isEditing()) {
                table.getCellEditor().cancelCellEditing();
            }
            table.selectAll();
        }
    };
    private static AbstractAction clearSelectionAction = new AbstractTableAction() {
        protected void processTable(final JTable table) {
            if (table.isEditing()) {
                table.getCellEditor().cancelCellEditing();
            }
            table.clearSelection();
        }
    };

    private static AbstractAction startEditingAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            } else if (table.editCellAt(currentRow, currentColumn)) {
                table.getEditorComponent().requestFocus();
            }
        }
    };
    private static AbstractAction cancelAction = new AbstractTableAction() {
        protected void processTable(final JTable table) {
            if (table.isEditing()) {
                table.getCellEditor().cancelCellEditing();
            }
        }
    };

    private static AbstractAction scrollUpChangeSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            int scrollUpIndex = getScrollUpIndex(table);
            if (scrollUpIndex > 0) {
                table.changeSelection(scrollUpIndex, currentColumn, false, false);
            } else {
                table.changeSelection(0, currentColumn, false, false);
            }
        }
    };
    private static AbstractAction scrollDownChangeSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            int scrollDownIndex = getScrollDownIndex(table);
            if (scrollDownIndex < table.getRowCount()) {
                table.changeSelection(scrollDownIndex, currentColumn, false, false);
            } else {
                table.changeSelection(table.getRowCount() - 1, currentColumn, false, false);
            }
        }
    };
    private static AbstractAction scrollRightChangeSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            int scrollRightIndex = getScrollRightIndex(table);
            if (scrollRightIndex < table.getColumnCount()) {
                table.changeSelection(currentRow, scrollRightIndex, false, false);
            } else {
                table.changeSelection(currentRow, table.getColumnCount() - 1, false, false);
            }
        }
    };
    private static AbstractAction scrollLeftChangeSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            int scrollLeftIndex = getScrollLeftIndex(table);
            if (scrollLeftIndex > 0) {
                table.changeSelection(currentRow, scrollLeftIndex, false, false);
            } else {
                table.changeSelection(currentRow, 0, false, false);
            }
        }
    };

    private static AbstractAction scrollUpExtendSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            int scrollUpIndex = getScrollUpIndex(table);
            System.err.println("scrollup: " + scrollUpIndex);
            if (scrollUpIndex > 0) {
                table.changeSelection(scrollUpIndex, currentColumn, false, true);
            } else {
                table.changeSelection(0, currentColumn, false, true);
            }
        }
    };
    private static AbstractAction scrollDownExtendSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            int scrollDownIndex = getScrollDownIndex(table);
            if (scrollDownIndex < table.getRowCount()) {
                table.changeSelection(scrollDownIndex, currentColumn, false, true);
            } else {
                table.changeSelection(table.getRowCount() - 1, currentColumn, false, true);
            }
        }
    };
    private static AbstractAction scrollRightExtendSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            int scrollRightIndex = getScrollRightIndex(table);
            if (scrollRightIndex < table.getColumnCount()) {
                table.changeSelection(currentRow, scrollRightIndex, false, true);
            } else {
                table.changeSelection(currentRow, table.getColumnCount() - 1, false, true);
            }
        }
    };
    private static AbstractAction scrollLeftExtendSelectionAction = new LeadRowColumnAction() {
        protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
            int scrollLeftIndex = getScrollLeftIndex(table);
            if (scrollLeftIndex > 0) {
                table.changeSelection(currentRow, scrollLeftIndex, false, true);
            } else {
                table.changeSelection(currentRow, 0, false, true);
            }
        }
    };


    public static void installKeyboardActions(final JTable table) {
        Utilities.installKeyboardActions(table, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "Table.ancestorInputMap", "Table.ancestorInputMap.RightToLeft");

        table.getActionMap().put("copy", TransferHandler.getCopyAction());
        table.getActionMap().put("paste", TransferHandler.getPasteAction());
        table.getActionMap().put("cut", TransferHandler.getCutAction());

        table.getActionMap().put("selectNextColumn", selectNextColumnAction);
        table.getActionMap().put("selectPreviousColumn", selectPreviousColumnAction);
        table.getActionMap().put("selectNextRow", selectNextRowAction);
        table.getActionMap().put("selectPreviousRow", selectPreviousRowAction);
        
        table.getActionMap().put("selectPreviousRowChangeLead", selectPreviousRowChangeLeadAction);
        table.getActionMap().put("selectNextRowChangeLead", selectNextRowChangeLeadAction);
        table.getActionMap().put("selectPreviousColumnChangeLead", selectPreviousColumnChangeLeadAction);
        table.getActionMap().put("selectNextColumnChangeLead", selectNextColumnChangeLeadAction);

        table.getActionMap().put("selectNextColumnExtendSelection", selectNextColumnExtendSelectionAction);
        table.getActionMap().put("selectPreviousColumnExtendSelection", selectPreviousColumnExtendSelectionAction);
        table.getActionMap().put("selectNextRowExtendSelection", selectNextRowExtendSelectionAction);
        table.getActionMap().put("selectPreviousRowExtendSelection", selectPreviousRowExtendSelectionAction);

        table.getActionMap().put("scrollUpChangeSelection", scrollUpChangeSelectionAction);
        table.getActionMap().put("scrollDownChangeSelection", scrollDownChangeSelectionAction);
        table.getActionMap().put("scrollUpExtendSelection", scrollUpExtendSelectionAction);
        table.getActionMap().put("scrollDownExtendSelection", scrollDownExtendSelectionAction);
        table.getActionMap().put("scrollLeftChangeSelection", scrollLeftChangeSelectionAction);
        table.getActionMap().put("scrollRightChangeSelection", scrollRightChangeSelectionAction);
        table.getActionMap().put("scrollLeftExtendSelection", scrollLeftExtendSelectionAction);
        table.getActionMap().put("scrollRightExtendSelection", scrollRightExtendSelectionAction);

        table.getActionMap().put("selectFirstColumn", selectFirstColumnAction);
        table.getActionMap().put("selectLastColumn", selectLastColumnAction);
        table.getActionMap().put("selectFirstRow", selectFirstRowAction);
        table.getActionMap().put("selectLastRow", selectLastRowAction);

        table.getActionMap().put("selectFirstColumnExtendSelection", selectFirstColumnExtendSelectionAction);
        table.getActionMap().put("selectLastColumnExtendSelection", selectLastColumnExtendSelectionAction);
        table.getActionMap().put("selectFirstRowExtendSelection", selectFirstRowExtendSelectionAction);
        table.getActionMap().put("selectLastRowExtendSelection", selectLastRowExtendSelectionAction);

        table.getActionMap().put("selectNextColumnCell", selectNextColumnCellAction);
        table.getActionMap().put("selectPreviousColumnCell", selectPreviousColumnCellAction);
        table.getActionMap().put("selectNextRowCell", selectNextRowCellAction);
        table.getActionMap().put("selectPreviousRowCell", selectPreviousRowCellAction);

        table.getActionMap().put("selectAll", selectAllAction);
        table.getActionMap().put("clearSelection", clearSelectionAction);

        table.getActionMap().put("addToSelection", new EnablebableLeadRowColumnAction(table) {
            protected void processRowColumn(final JTable table, final int currentRow, final int currentColumn) {
                if (currentRow >= 0 && currentColumn >=0) {
                    table.addRowSelectionInterval(currentRow, currentRow);
                    table.addColumnSelectionInterval(currentColumn, currentColumn);
                }
            }
            public boolean isEnabled(final JTable table, final int currentRow, final int currentColumn) {
                return !table.isCellSelected(currentRow, currentColumn);
            }
        });

        table.getActionMap().put("toggleAndAnchor", toggleAndAnchorAction);
        table.getActionMap().put("moveSelectionTo", moveSelectionToAction);
        table.getActionMap().put("extendTo", extendToAction);

        table.getActionMap().put("startEditing", startEditingAction);
        table.getActionMap().put("cancel", cancelAction);
    }

    public static void uninstallKeyboardActions(final JTable table) {
        Utilities.uninstallKeyboardActions(table, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private static int getFirstVisibleTableRowIndex(final JTable table) {
        Rectangle visibleRect = table.getVisibleRect();
        if (visibleRect.isEmpty()) {
            return -1;
        }

        int currentColumn = table.getColumnModel().getSelectionModel().getAnchorSelectionIndex();

        for (int i = 0; i < table.getRowCount(); i++) {
            Rectangle bounds = table.getCellRect(i, currentColumn, true);
            if (bounds.intersects(visibleRect)) {
                return i;
            }
        }

        return -1;
    }

    private static int getLastVisibleTableRowIndex(final JTable table) {
        Rectangle visibleRect = table.getVisibleRect();
        if (visibleRect.isEmpty()) {
            return -1;
        }

        int currentColumn = table.getColumnModel().getSelectionModel().getAnchorSelectionIndex();

        for (int i = table.getRowCount() - 1; i >= 0; i--) {
            Rectangle bounds = table.getCellRect(i, currentColumn, true);
            if (bounds.intersects(visibleRect)) {
                return i;
            }
        }

        return -1;
    }

    private static int getRightmostVisibleTableRowIndex(final JTable table) {
        Rectangle visibleRect = table.getVisibleRect();
        if (visibleRect.isEmpty()) {
            return -1;
        }

        int currentRow = table.getSelectionModel().getAnchorSelectionIndex();

        for (int i = table.getColumnCount() - 1; i >= 0; i--) {
            Rectangle bounds = table.getCellRect(currentRow, i, true);
            if (bounds.intersects(visibleRect)) {
                return i;
            }
        }

        return -1;
    }

    private static int getLeftmostVisibleTableRowIndex(final JTable table) {
        Rectangle visibleRect = table.getVisibleRect();
        if (visibleRect.isEmpty()) {
            return -1;
        }

        int currentRow = table.getSelectionModel().getAnchorSelectionIndex();

        for (int i = 0; i < table.getColumnCount(); i++) {
            Rectangle bounds = table.getCellRect(currentRow, i, true);
            if (bounds.intersects(visibleRect)) {
                return i;
            }
        }

        return -1;
    }

    private static int getScrollDownIndex(final JTable table) {
        if (table.getRowCount() == 0) {
            return -1;
        }

        int currentSelection = table.getSelectionModel().getLeadSelectionIndex();
        int lastVisible = getLastVisibleTableRowIndex(table);
        if (lastVisible != currentSelection || lastVisible == table.getRowCount() - 1) {
            return lastVisible;
        } else {
            Rectangle visibleRect = table.getVisibleRect();
            int currentColumn = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
            int i = lastVisible + 1;
            int cellsHeight = table.getCellRect(i, currentColumn, true).height;
            while (i < table.getRowCount() - 1 && cellsHeight < visibleRect.height) {
                i++;
                cellsHeight += table.getCellRect(i, currentColumn, true).height;
            }

            return i;
        }
    }

    private static int getScrollUpIndex(final JTable table) {
        if (table.getRowCount() == 0) {
            return -1;
        }

        int currentSelection = table.getSelectionModel().getLeadSelectionIndex();
        int firstVisible = getFirstVisibleTableRowIndex(table);
        if (firstVisible != currentSelection || firstVisible == table.getRowCount() - 1) {
            return firstVisible;
        } else {
            Rectangle visibleRect = table.getVisibleRect();
            int currentColumn = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
            int i = firstVisible - 1;
            int cellsHeight = table.getCellRect(i, currentColumn, true).height;
            while (i > 0 && cellsHeight < visibleRect.height) {
                i--;
                cellsHeight += table.getCellRect(i, currentColumn, true).height;
            }

            return i;
        }
    }

    private static int getScrollRightIndex(final JTable table) {
        if (table.getColumnCount() == 0) {
            return -1;
        }

        int currentSelection = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
        int lastVisible = getRightmostVisibleTableRowIndex(table);
        if (lastVisible != currentSelection || lastVisible == table.getColumnCount() - 1) {
            return lastVisible;
        } else {
            Rectangle visibleRect = table.getVisibleRect();
            int currentRow = table.getSelectionModel().getLeadSelectionIndex();
            int i = lastVisible + 1;
            int cellsWidth = table.getCellRect(currentRow, i, true).width;
            while (i < table.getColumnCount() - 1 && cellsWidth < visibleRect.width) {
                i++;
                cellsWidth += table.getCellRect(currentRow, i, true).width;
            }

            return i;
        }
    }

    private static int getScrollLeftIndex(final JTable table) {
        if (table.getRowCount() == 0) {
            return -1;
        }

        int currentSelection = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
        int firstVisible = getLeftmostVisibleTableRowIndex(table);
        if (firstVisible != currentSelection || firstVisible == table.getColumnCount() - 1) {
            return firstVisible;
        } else {
            Rectangle visibleRect = table.getVisibleRect();
            int currentRow = table.getSelectionModel().getLeadSelectionIndex();
            int i = firstVisible - 1;
            int cellsWidth = table.getCellRect(currentRow, i, true).width;
            while (i > 0 && cellsWidth < visibleRect.width) {
                i--;
                cellsWidth += table.getCellRect(currentRow, i, true).width;
            }

            return i;
        }
    }

    private static void ensureCellIsVisible(final JTable table, final int row, final int column) {
        table.scrollRectToVisible(table.getCellRect(row, column, true));
     }
}
