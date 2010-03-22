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
import javax.swing.JList;
import javax.swing.TransferHandler;

import org.apache.harmony.x.swing.Utilities;


final class BasicListKeyboardActions {
    private static AbstractAction selectPreviousRowAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();

            int previousIndex = getPreviousRow(list);
            if (previousIndex == -1) {
                return;
            }

            list.setSelectedIndex(previousIndex);
            list.ensureIndexIsVisible(previousIndex);
        }
    };

    private static AbstractAction selectPreviousRowChangeLeadAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            if (!(list.getSelectionModel() instanceof DefaultListSelectionModel)) {
                return;
            }

            int previousIndex = getPreviousRow(list);
            if (previousIndex == -1) {
                return;
            }

            ((DefaultListSelectionModel)list.getSelectionModel()).moveLeadSelectionIndex(previousIndex);
            list.ensureIndexIsVisible(previousIndex);
        }
    };

    private static AbstractAction selectPreviousRowExtendSelectionAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();

            int previousIndex = getPreviousRow(list);
            if (previousIndex == -1) {
                return;
            }

            if (list.isSelectionEmpty()) {
                list.setSelectedIndex(previousIndex);
            } else {
                list.setValueIsAdjusting(true);
                list.clearSelection();
                list.addSelectionInterval(list.getAnchorSelectionIndex(), previousIndex);
                list.setValueIsAdjusting(false);
                list.ensureIndexIsVisible(previousIndex);
            }
        }
    };

    private static AbstractAction selectNextRowAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            int nextIndex = getNextRow(list);
            if (nextIndex == -1) {
                return;
            }

            list.setSelectedIndex(nextIndex);
            list.ensureIndexIsVisible(nextIndex);
        }
    };

    private static AbstractAction selectNextRowChangeLeadAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            if (!(list.getSelectionModel() instanceof DefaultListSelectionModel)) {
                return;
            }

            int nextIndex = getNextRow(list);
            if (nextIndex == -1) {
                return;
            }

            ((DefaultListSelectionModel)list.getSelectionModel()).moveLeadSelectionIndex(nextIndex);
            list.ensureIndexIsVisible(nextIndex);
        }
    };

    private static AbstractAction selectNextRowExtendSelectionAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            int nextIndex = getNextRow(list);
            if (nextIndex == -1) {
                return;
            }

            if (list.isSelectionEmpty()) {
                list.setSelectedIndex(nextIndex);
            } else {
                list.setValueIsAdjusting(true);
                list.clearSelection();
                list.addSelectionInterval(list.getAnchorSelectionIndex(), nextIndex);
                list.setValueIsAdjusting(false);
                list.ensureIndexIsVisible(nextIndex);
            }
        }
    };

    private static AbstractAction selectPreviousColumnAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            int previousIndex = getPreviousColumn(list);
            if (previousIndex == -1) {
                return;
            }

            list.setSelectedIndex(previousIndex);
            list.ensureIndexIsVisible(previousIndex);
        }
    };

    private static AbstractAction selectPreviousColumnChangeLeadAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            if (!(list.getSelectionModel() instanceof DefaultListSelectionModel)) {
                return;
            }

            int previousIndex = getPreviousColumn(list);
            if (previousIndex == -1) {
                return;
            }

            ((DefaultListSelectionModel)list.getSelectionModel()).moveLeadSelectionIndex(previousIndex);
            list.ensureIndexIsVisible(previousIndex);
        }
    };

    private static AbstractAction selectPreviousColumnExtendSelectionAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();

            int previousIndex = getPreviousColumn(list);
            if (previousIndex == -1) {
                return;
            }

            list.setValueIsAdjusting(true);
            list.clearSelection();
            list.addSelectionInterval(list.getAnchorSelectionIndex(), previousIndex);
            list.setValueIsAdjusting(false);
            list.ensureIndexIsVisible(previousIndex);
        }
    };

    private static AbstractAction selectNextColumnAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();

            int nextIndex = getNextColumn(list);
            if (nextIndex == -1) {
                return;
            }

            list.setSelectedIndex(nextIndex);
            list.ensureIndexIsVisible(nextIndex);
        }
    };

    private static AbstractAction selectNextColumnChangeLeadAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            if (!(list.getSelectionModel() instanceof DefaultListSelectionModel)) {
                return;
            }

            int nextIndex = getNextColumn(list);
            if (nextIndex == -1) {
                return;
            }

            ((DefaultListSelectionModel)list.getSelectionModel()).moveLeadSelectionIndex(nextIndex);
            list.ensureIndexIsVisible(nextIndex);
        }
    };

    private static AbstractAction selectNextColumnExtendSelectionAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();

            int nextIndex = getNextColumn(list);
            if (nextIndex == -1) {
                return;
            }

            list.setValueIsAdjusting(true);
            list.clearSelection();
            list.addSelectionInterval(list.getAnchorSelectionIndex(), nextIndex);
            list.setValueIsAdjusting(false);
            list.ensureIndexIsVisible(nextIndex);
        }
    };

    private static AbstractAction selectFirstRowAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            if (list.getModel().getSize() == 0) {
                return;
            }

            int candidateIndex = 0;
            BasicListUI ui = (BasicListUI)list.getUI();
            if (ui.extendedSupportEnabled) {
                while (candidateIndex < list.getModel().getSize()) {
                    if (ui.isChoosable(candidateIndex)) {
                        break;
                    }
                    candidateIndex++;
                }
                if (candidateIndex < list.getModel().getSize()) {
                    return;
                }
            }
            list.setSelectedIndex(candidateIndex);
            list.ensureIndexIsVisible(candidateIndex);
        }
    };

    private static AbstractAction selectFirstRowChangeLeadAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            if (list.getModel().getSize() == 0) {
                return;
            }
            if (!(list.getSelectionModel() instanceof DefaultListSelectionModel)) {
                return;
            }

            ((DefaultListSelectionModel)list.getSelectionModel()).moveLeadSelectionIndex(0);
            list.ensureIndexIsVisible(0);
        }
    };

    private static AbstractAction selectFirstRowExtendSelectionAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            if (list.getModel().getSize() == 0) {
                return;
            }

            int beginIndex = list.getAnchorSelectionIndex();
            if (beginIndex != -1) {
                list.setValueIsAdjusting(true);
                list.clearSelection();
                list.addSelectionInterval(beginIndex, 0);
                list.setValueIsAdjusting(false);
            }

            list.ensureIndexIsVisible(0);
        }
    };

    private static AbstractAction selectLastRowAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            if (list.getModel().getSize() == 0) {
                return;
            }

            int candidateIndex = list.getModel().getSize() - 1;
            BasicListUI ui = (BasicListUI)list.getUI();
            if (ui.extendedSupportEnabled) {
                while (candidateIndex >= 0) {
                    if (ui.isChoosable(candidateIndex)) {
                        break;
                    }
                    candidateIndex--;
                }
                if (candidateIndex == -1) {
                    return;
                }
            }

            list.setSelectedIndex(candidateIndex);
            list.ensureIndexIsVisible(candidateIndex);
        }
    };

    private static AbstractAction selectLastRowChangeLeadAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            if (list.getModel().getSize() == 0) {
                return;
            }
            if (!(list.getSelectionModel() instanceof DefaultListSelectionModel)) {
                return;
            }

            int lastIndex = list.getModel().getSize() - 1;
            ((DefaultListSelectionModel)list.getSelectionModel()).moveLeadSelectionIndex(lastIndex);
            list.ensureIndexIsVisible(lastIndex);
        }
    };

    private static AbstractAction selectLastRowExtendSelectionAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            if (list.getModel().getSize() == 0) {
                return;
            }

            int lastIndex = list.getModel().getSize() - 1;
            int beginIndex = list.getAnchorSelectionIndex();
            if (beginIndex != -1) {
                list.setValueIsAdjusting(true);
                list.clearSelection();
                list.addSelectionInterval(beginIndex, lastIndex);
                list.setValueIsAdjusting(false);
            }

            list.ensureIndexIsVisible(lastIndex);
        }
    };

    private static AbstractAction scrollUpAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();

            int upIndex = getScrollUpIndex(list);
            if (upIndex == -1) {
                return;
            }

            list.setSelectedIndex(upIndex);
            list.ensureIndexIsVisible(upIndex);
        }
    };

    private static AbstractAction scrollUpChangeLeadAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            if (!(list.getSelectionModel() instanceof DefaultListSelectionModel)) {
                return;
            }

            int upIndex = getScrollUpIndex(list);
            if (upIndex == -1) {
                return;
            }

            ((DefaultListSelectionModel)list.getSelectionModel()).moveLeadSelectionIndex(upIndex);
            list.ensureIndexIsVisible(upIndex);
        }
    };

    private static AbstractAction scrollUpExtendSelectionAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();

            int upIndex = getScrollUpIndex(list);
            if (upIndex == -1) {
                return;
            }

            int beginIndex = list.getAnchorSelectionIndex();
            if (beginIndex != -1) {
                list.setValueIsAdjusting(true);
                list.clearSelection();
                list.addSelectionInterval(beginIndex, upIndex);
                list.setValueIsAdjusting(false);
            }

            list.ensureIndexIsVisible(upIndex);
        }
    };

    private static AbstractAction scrollDownAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();

            int downIndex = getScrollDownIndex(list);
            if (downIndex == -1) {
                return;
            }

            list.setSelectedIndex(downIndex);
            list.ensureIndexIsVisible(downIndex);
        }
    };

    private static AbstractAction scrollDownChangeLeadAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            if (!(list.getSelectionModel() instanceof DefaultListSelectionModel)) {
                return;
            }

            int downIndex = getScrollDownIndex(list);
            if (downIndex == -1) {
                return;
            }

            ((DefaultListSelectionModel)list.getSelectionModel()).moveLeadSelectionIndex(downIndex);
            list.ensureIndexIsVisible(downIndex);
        }
    };

    private static AbstractAction scrollDownExtendSelectionAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();

            int downIndex = getScrollDownIndex(list);
            if (downIndex == -1) {
                return;
            }

            int beginIndex = list.getAnchorSelectionIndex();
            if (beginIndex != -1) {
                list.setValueIsAdjusting(true);
                list.clearSelection();
                list.addSelectionInterval(beginIndex, downIndex);
                list.setValueIsAdjusting(false);
            }

            list.ensureIndexIsVisible(downIndex);
        }
    };

    private static AbstractAction selectAllAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            if (list.getModel().getSize() == 0) {
                return;
            }

            list.setSelectionInterval(0, list.getModel().getSize() - 1);
        }
    };

    private static AbstractAction clearSelectionAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            if (list.getModel().getSize() == 0) {
                return;
            }

            list.clearSelection();
        }
    };

    private static AbstractAction addToSelectionAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();

            int addIndex = list.getLeadSelectionIndex();
            if (addIndex == -1) {
                return;
            }

            list.addSelectionInterval(addIndex, addIndex);
        }
    };

    private static AbstractAction toggleAndAnchorAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();

            int toggleIndex = list.getLeadSelectionIndex();
            if (toggleIndex == -1) {
                return;
            }

            if (list.isSelectedIndex(toggleIndex)) {
                list.removeSelectionInterval(toggleIndex, toggleIndex);
            } else {
                list.addSelectionInterval(toggleIndex, toggleIndex);
            }
            list.getSelectionModel().setAnchorSelectionIndex(toggleIndex);
        }
    };

    private static AbstractAction moveSelectionToAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();

            int moveIndex = list.getLeadSelectionIndex();
            if (moveIndex == -1) {
                return;
            }

            list.setSelectedIndex(moveIndex);
        }
    };

    private static AbstractAction extendToAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JList list = (JList)e.getSource();
            int extendIndex = list.getLeadSelectionIndex();
            if (extendIndex == -1) {
                return;
            }

            list.setValueIsAdjusting(true);
            list.clearSelection();
            list.addSelectionInterval(list.getAnchorSelectionIndex(), extendIndex);
            list.setValueIsAdjusting(false);
        }
    };


    public static void installKeyboardActions(final JList list) {
        Utilities.installKeyboardActions(list, JComponent.WHEN_FOCUSED, "List.focusInputMap", "List.focusInputMap.RightToLeft");

        list.getActionMap().put("selectPreviousRow", selectPreviousRowAction);
        list.getActionMap().put("selectNextRow", selectNextRowAction);
        list.getActionMap().put("selectPreviousRowExtendSelection", selectPreviousRowExtendSelectionAction);
        list.getActionMap().put("selectNextRowExtendSelection", selectNextRowExtendSelectionAction);

        list.getActionMap().put("selectPreviousRowChangeLead", selectPreviousRowChangeLeadAction);
        list.getActionMap().put("selectNextRowChangeLead", selectNextRowChangeLeadAction);
        list.getActionMap().put("selectPreviousColumnChangeLead", selectPreviousColumnChangeLeadAction);
        list.getActionMap().put("selectNextColumnChangeLead", selectNextColumnChangeLeadAction);

        list.getActionMap().put("selectPreviousColumn", selectPreviousColumnAction);
        list.getActionMap().put("selectNextColumn", selectNextColumnAction);
        list.getActionMap().put("selectPreviousColumnExtendSelection", selectPreviousColumnExtendSelectionAction);
        list.getActionMap().put("selectNextColumnExtendSelection", selectNextColumnExtendSelectionAction);

        list.getActionMap().put("selectFirstRow", selectFirstRowAction);
        list.getActionMap().put("selectFirstRowExtendSelection", selectFirstRowExtendSelectionAction);
        list.getActionMap().put("selectLastRow", selectLastRowAction);
        list.getActionMap().put("selectLastRowExtendSelection", selectLastRowExtendSelectionAction);

        list.getActionMap().put("selectLastRowChangeLead", selectLastRowChangeLeadAction);
        list.getActionMap().put("selectFirstRowChangeLead", selectFirstRowChangeLeadAction);

        list.getActionMap().put("scrollUp", scrollUpAction);
        list.getActionMap().put("scrollUpExtendSelection", scrollUpExtendSelectionAction);
        list.getActionMap().put("scrollDown", scrollDownAction);
        list.getActionMap().put("scrollDownExtendSelection", scrollDownExtendSelectionAction);

        list.getActionMap().put("scrollUpChangeLead", scrollUpChangeLeadAction);
        list.getActionMap().put("scrollDownChangeLead", scrollDownChangeLeadAction);

        list.getActionMap().put("selectAll", selectAllAction);
        list.getActionMap().put("clearSelection", clearSelectionAction);

        list.getActionMap().put("addToSelection", addToSelectionAction);
        list.getActionMap().put("toggleAndAnchor", toggleAndAnchorAction);
        list.getActionMap().put("moveSelectionTo", moveSelectionToAction);
        list.getActionMap().put("extendTo", extendToAction);

        list.getActionMap().put("copy", TransferHandler.getCopyAction());
        list.getActionMap().put("paste", TransferHandler.getPasteAction());
        list.getActionMap().put("cut", TransferHandler.getCutAction());
    }

    public static void uninstallKeyboardActions(final JList list) {
        Utilities.uninstallKeyboardActions(list, JComponent.WHEN_FOCUSED);
    }


    private static int getNextRow(final JList list) {
        if (list.getModel().getSize() == 0) {
            return -1;
        }

        BasicListUI ui = (BasicListUI)list.getUI();


        int currectSelection = list.getLeadSelectionIndex();
        if (currectSelection == -1 || currectSelection >= list.getModel().getSize()) {
            currectSelection = 0;
            if (!ui.extendedSupportEnabled || ui.isChoosable(currectSelection)) {
                return currectSelection;
            }
        }

        ui.maybeUpdateLayoutState();
        BasicListUI.LayoutStrategy strategy = ui.layouter.getLayoutStrategy();

        int candidateIndex = currectSelection;
        while(candidateIndex < list.getModel().getSize()) {
            int selectedRow = strategy.getRow(candidateIndex);
            int selectedColumn = strategy.getColumn(candidateIndex);
            if (selectedRow < strategy.getRowCount() - 1 && strategy.getIndex(selectedRow + 1, selectedColumn) < list.getModel().getSize()) {
                candidateIndex = strategy.getIndex(selectedRow + 1, selectedColumn);
            } else if (list.getLayoutOrientation() == JList.VERTICAL_WRAP && selectedColumn < strategy.getColumnCount() - 1 && strategy.getIndex(0, selectedColumn + 1) < list.getModel().getSize()) {
                candidateIndex = strategy.getIndex(0, selectedColumn + 1);
            } else {
                return -1;
            }

            if (ui.isChoosable(candidateIndex)) {
                return candidateIndex;
            }
        }
        
        return -1;
    }

    private static int getPreviousRow(final JList list) {
        if (list.getModel().getSize() == 0) {
            return -1;
        }

        BasicListUI ui = (BasicListUI)list.getUI();
        int currectSelection = list.getLeadSelectionIndex();
        if (currectSelection == -1 || currectSelection >= list.getModel().getSize()) {
            currectSelection = list.getModel().getSize() - 1;
            if (!ui.extendedSupportEnabled || ui.isChoosable(currectSelection)) {
                return currectSelection;
            }
        }

        ui.maybeUpdateLayoutState();
        BasicListUI.LayoutStrategy strategy = ui.layouter.getLayoutStrategy();

        int candidateIndex = currectSelection;
        while(candidateIndex >= 0) {
            int selectedRow = strategy.getRow(candidateIndex);
            int selectedColumn = strategy.getColumn(candidateIndex);

            if (selectedRow > 0) {
                candidateIndex = strategy.getIndex(selectedRow - 1, selectedColumn);
            } else if (list.getLayoutOrientation() == JList.VERTICAL_WRAP && selectedColumn > 0) {
                candidateIndex = strategy.getIndex(strategy.getRowCount() - 1, selectedColumn - 1);
            } else {
                return -1;
            }

            if (ui.isChoosable(candidateIndex)) {
                return candidateIndex;
            }
        }

        return -1;
    }

    private static int getNextColumn(final JList list) {
        if (list.getModel().getSize() == 0) {
            return -1;
        }

        int currectSelection = list.getLeadSelectionIndex();
        if (currectSelection == -1 || currectSelection >= list.getModel().getSize()) {
            list.setSelectedIndex(0);
            return -1;
        }

        BasicListUI ui = (BasicListUI)list.getUI();
        ui.maybeUpdateLayoutState();
        BasicListUI.LayoutStrategy strategy = ui.layouter.getLayoutStrategy();

        int candidateIndex = currectSelection;
        while(candidateIndex < list.getModel().getSize()) {
            int selectedRow = strategy.getRow(candidateIndex);
            int selectedColumn = strategy.getColumn(candidateIndex);
            if (selectedColumn < strategy.getColumnCount() - 1) {
                if (strategy.getIndex(selectedRow, selectedColumn + 1) < list.getModel().getSize()) {
                    candidateIndex = strategy.getIndex(selectedRow, selectedColumn + 1);
                } else {
                    candidateIndex = list.getModel().getSize() - 1;
                }
            } else if (list.getLayoutOrientation() == JList.VERTICAL_WRAP && selectedRow < strategy.getRowCount() - 1 && strategy.getIndex(selectedRow + 1, selectedColumn) < list.getModel().getSize()) {
                return strategy.getIndex(selectedRow + 1, selectedColumn);
            } else {
                return -1;
            }
            
            if (ui.isChoosable(candidateIndex)) {
                return candidateIndex;
            }
        }

        return -1;
    }

    private static int getPreviousColumn(final JList list) {
        if (list.getModel().getSize() == 0) {
            return -1;
        }

        int currectSelection = list.getLeadSelectionIndex();
        if (currectSelection == -1 || currectSelection >= list.getModel().getSize()) {
            list.setSelectedIndex(list.getModel().getSize() - 1);
            return -1;
        }

        BasicListUI ui = (BasicListUI)list.getUI();
        ui.maybeUpdateLayoutState();
        BasicListUI.LayoutStrategy strategy = ui.layouter.getLayoutStrategy();

        int candidateIndex = currectSelection;
        while(candidateIndex >= 0) {
            int selectedRow = strategy.getRow(candidateIndex);
            int selectedColumn = strategy.getColumn(candidateIndex);
            if (selectedColumn > 0) {
                candidateIndex = strategy.getIndex(selectedRow, selectedColumn - 1);
            } else if (list.getLayoutOrientation() == JList.VERTICAL_WRAP && selectedRow > 0) {
                candidateIndex = strategy.getIndex(selectedRow - 1, selectedColumn);
            } else {
                return -1;
            }

            if (ui.isChoosable(candidateIndex)) {
                return candidateIndex;
             }
        }

        return -1;
    }

    private static int getScrollDownIndex(final JList list) {
        if (list.getModel().getSize() == 0) {
            return -1;
        }

        int candidateIndex;
        int currentSelection = list.getLeadSelectionIndex();
        int lastVisible = list.getLastVisibleIndex();
        if (lastVisible != currentSelection || lastVisible == list.getModel().getSize() - 1) {
            candidateIndex = lastVisible;
        } else {
            Rectangle visibleRect = list.getVisibleRect();
            int i = lastVisible + 1;
            while (i < list.getModel().getSize() - 1 && list.getCellBounds(lastVisible + 1, i).height < visibleRect.height) {
                i++;
            }

            candidateIndex = i;
        }

        BasicListUI ui = (BasicListUI)list.getUI();
        if (!ui.extendedSupportEnabled) {
            return candidateIndex;
        }
        for (int i = candidateIndex; i < list.getModel().getSize(); i++) {
            if (ui.isChoosable(i)) {
                return i;
            }
        }
        for (int i = candidateIndex; i > currentSelection; i--) {
            if (ui.isChoosable(i)) {
                return i;
            }
        }

        return -1;
    }

    private static int getScrollUpIndex(final JList list) {
        if (list.getModel().getSize() == 0) {
            return -1;
        }

        int candidateIndex;
        int currentSelection = list.getLeadSelectionIndex();

        int firstVisible = list.getFirstVisibleIndex();
        if (firstVisible != currentSelection || firstVisible == 0) {
            candidateIndex = firstVisible;
        } else {
            Rectangle visibleRect = list.getVisibleRect();
            int i = firstVisible - 1;
            while (i > 0 && list.getCellBounds(firstVisible - 1, i).height < visibleRect.height) {
                i--;
            }

            candidateIndex = i;
        }
        
        BasicListUI ui = (BasicListUI)list.getUI();
        if (!ui.extendedSupportEnabled) {
            return candidateIndex;
        }
        for (int i = candidateIndex; i >= 0; i--) {
            if (ui.isChoosable(i)) {
                return i;
            }
        }
        for (int i = candidateIndex; i < currentSelection; i++) {
            if (ui.isChoosable(i)) {
                return i;
            }
        }

        return -1;
    }
}
