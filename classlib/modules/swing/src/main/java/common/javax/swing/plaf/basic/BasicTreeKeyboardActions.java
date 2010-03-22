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
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import org.apache.harmony.x.swing.Utilities;


final class BasicTreeKeyboardActions {
    private static abstract class TreeAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            JTree tree = (JTree)e.getSource();
            if (tree.getRowCount() == 0) {
                return;
            }

            actionPerformed(tree);
        }

        public abstract void actionPerformed(JTree tree);

        protected TreePath getPreviousPath(final JTree tree, final int currentRow) {
            if (currentRow == -1) {
                return tree.getPathForRow(tree.getRowCount() - 1);
            }
            return currentRow > 0 ? tree.getPathForRow(currentRow - 1)
                                  : tree.getPathForRow(0);
        }

        protected TreePath getNextPath(final JTree tree, final int currentRow) {
            if (currentRow == -1) {
                return tree.getPathForRow(0);
            }
            return currentRow < tree.getRowCount() - 1 ? tree.getPathForRow(currentRow + 1)
                                                       : tree.getPathForRow(tree.getRowCount() - 1);
        }

        protected void extendSelection(final JTree tree, final TreePath toPath) {
            TreePath anchorPath = tree.getAnchorSelectionPath();
            if (anchorPath == null) {
                tree.setSelectionPath(toPath);
            } else {
                int anchorRow = tree.getRowForPath(anchorPath);
                int leadRow = tree.getRowForPath(toPath);
                tree.setSelectionInterval(anchorRow, leadRow);
                tree.setAnchorSelectionPath(anchorPath);
                tree.setLeadSelectionPath(toPath);
            }
            tree.scrollPathToVisible(toPath);
        }

        protected boolean isLeaf(final JTree tree, final TreePath path) {
            return tree.getModel().isLeaf(path.getLastPathComponent());
        }

        protected TreePath getLastPath(final JTree tree) {
            return tree.getPathForRow(tree.getRowCount() - 1);
        }

        protected TreePath getUpPath(final JTree tree) {
            Rectangle visibleRect = tree.getVisibleRect();
            TreePath result = tree.getClosestPathForLocation(visibleRect.x, visibleRect.y + 1);
            TreePath leadPath = tree.getLeadSelectionPath();
            if (result.equals(leadPath)) {
                result = tree.getClosestPathForLocation(visibleRect.x, visibleRect.y - visibleRect.height);
            }

            return result;
        }

        protected TreePath getDownPath(final JTree tree) {
            Rectangle visibleRect = tree.getVisibleRect();
            TreePath result = tree.getClosestPathForLocation(visibleRect.x, visibleRect.y + visibleRect.height - 1);
            TreePath leadPath = tree.getLeadSelectionPath();
            if (result.equals(leadPath)) {
                result = tree.getClosestPathForLocation(visibleRect.x, visibleRect.y + 2 * visibleRect.height);
            }

            return result;
        }
    }

    private static abstract class ChangeLeadAction extends TreeAction {
        public void actionPerformed(final JTree tree) {
            changeLeadActionPerformed(tree, getNewLeadPath(tree));
        }

        protected void changeLeadActionPerformed(final JTree tree, final TreePath newLeadPath) {
            TreePath oldLeadPath = tree.getLeadSelectionPath();
            tree.setLeadSelectionPath(newLeadPath);
            tree.scrollPathToVisible(newLeadPath);
            tree.repaint(tree.getPathBounds(oldLeadPath));
            tree.repaint(tree.getPathBounds(newLeadPath));
        }

        protected abstract TreePath getNewLeadPath(JTree tree);
    }

    private static abstract class PreserveLeadAnchorAction extends TreeAction {
        public void actionPerformed(final JTree tree) {
            TreePath leadPath = tree.getLeadSelectionPath();
            TreePath anchorPath = tree.getAnchorSelectionPath();
            preserveActionPerformed(tree);
            if (leadPath != null) {
                tree.setLeadSelectionPath(leadPath);
            }
            if (anchorPath != null) {
                tree.setAnchorSelectionPath(anchorPath);
            }
        }

        protected abstract void preserveActionPerformed(JTree tree);
    }


    private static AbstractAction startEditingAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            TreePath path = tree.getLeadSelectionPath();
            if (path == null) {
                return;
            }

            tree.startEditingAtPath(path);
        }
    };
    static AbstractAction cancelAction = new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
            JTree tree = (JTree)e.getSource();
            tree.cancelEditing();
        }
    };
    static AbstractAction selectPreviousAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            int leadRow = tree.getLeadSelectionRow();
            TreePath prevPath = getPreviousPath(tree, leadRow);
            tree.setSelectionPath(prevPath);
            tree.scrollPathToVisible(prevPath);
        }
    };
    private static AbstractAction selectPreviousExtendSelectionAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            TreePath prevPath = getPreviousPath(tree, tree.getLeadSelectionRow());
            extendSelection(tree, prevPath);
        }
    };
    static AbstractAction selectNextAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            int leadRow = tree.getLeadSelectionRow();
            TreePath nextPath = getNextPath(tree, leadRow);
            tree.setSelectionPath(nextPath);
            tree.scrollPathToVisible(nextPath);
        }
    };
    private static AbstractAction selectNextExtendSelectionAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            TreePath nextPath = getNextPath(tree, tree.getLeadSelectionRow());
            extendSelection(tree, nextPath);
        }
    };
    static AbstractAction selectChildAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            TreePath leadPath = tree.getLeadSelectionPath();
            if (leadPath == null) {
                tree.setSelectionRow(0);
                tree.scrollRowToVisible(0);
                return;
            }

            if (!isLeaf(tree, leadPath) && !tree.isExpanded(leadPath)) {
                tree.expandPath(leadPath);
                return;
            }

            TreePath nextPath = getNextPath(tree, tree.getLeadSelectionRow());
            tree.setSelectionPath(nextPath);
            tree.scrollPathToVisible(nextPath);
        }
    };
    static AbstractAction selectParentAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            TreePath leadPath = tree.getLeadSelectionPath();
            if (leadPath == null) {
                TreePath lastPath = getLastPath(tree);
                tree.setSelectionPath(lastPath);
                tree.scrollPathToVisible(lastPath);
                return;
            }

            if (!isLeaf(tree, leadPath) && tree.isExpanded(leadPath)) {
                tree.collapsePath(leadPath);
                return;
            }

            TreePath prevPath = leadPath.getParentPath();
            if (!tree.isVisible(prevPath)) {
                return;
            }
            tree.setSelectionPath(prevPath);
            tree.scrollPathToVisible(prevPath);
        }
    };
    static AbstractAction selectFirstAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            tree.setSelectionPath(tree.getPathForRow(0));
        }
    };
    private static AbstractAction selectFirstExtendSelectionAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            TreePath firstPath = tree.getPathForRow(0);
            extendSelection(tree, firstPath);
        }
    };
    static AbstractAction selectLastAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            tree.setSelectionPath(getLastPath(tree));
        }
    };
    private static AbstractAction selectLastExtendSelectionAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            extendSelection(tree, getLastPath(tree));
        }
    };
    private static AbstractAction selectAllAction = new PreserveLeadAnchorAction() {
        public void preserveActionPerformed(final JTree tree) {
            tree.setSelectionInterval(0, tree.getRowCount() - 1);
        }
    };
    private static AbstractAction clearSelectionAction = new PreserveLeadAnchorAction() {
        public void preserveActionPerformed(final JTree tree) {
            tree.clearSelection();
        }
    };
    private static AbstractAction toggleSelectionPreserveAnchorAction = new PreserveLeadAnchorAction() {
        public void preserveActionPerformed(final JTree tree) {
            TreePath leadPath = tree.getLeadSelectionPath();
            if (leadPath == null) {
                return;
            }

            if (tree.isPathSelected(leadPath)) {
                tree.removeSelectionPath(leadPath);
            } else {
                tree.addSelectionPath(leadPath);
            }
        }
    };
    private static AbstractAction moveSelectionToAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            TreePath leadPath = tree.getLeadSelectionPath();
            if (leadPath == null) {
                return;
            }
            tree.setSelectionPath(leadPath);
        }
    };
    private static AbstractAction extendSelectionAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            TreePath leadPath = tree.getLeadSelectionPath();
            if (leadPath == null) {
                return;
            }
            extendSelection(tree, leadPath);
        }
    };
    private static AbstractAction selectFirstChangeLeadAction = new ChangeLeadAction() {
        protected TreePath getNewLeadPath(final JTree tree) {
            return tree.getPathForRow(0);
        }
    };
    private static AbstractAction selectLastChangeLeadAction = new ChangeLeadAction() {
        protected TreePath getNewLeadPath(final JTree tree) {
            return getLastPath(tree);
        }
    };
    private static AbstractAction selectPreviousChangeLeadAction = new ChangeLeadAction() {
        protected TreePath getNewLeadPath(final JTree tree) {
            return getPreviousPath(tree, tree.getLeadSelectionRow());
        }
    };
    private static AbstractAction selectNextChangeLeadAction = new ChangeLeadAction() {
        protected TreePath getNewLeadPath(final JTree tree) {
            return getNextPath(tree, tree.getLeadSelectionRow());
        }
    };

    static AbstractAction scrollUpChangeSelectionAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            TreePath upPath = getUpPath(tree);
            tree.setSelectionPath(upPath);
            tree.scrollPathToVisible(upPath);
        }
    };
    private static AbstractAction scrollUpExtendSelectionAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            TreePath upPath = getUpPath(tree);
            extendSelection(tree, upPath);
            tree.scrollPathToVisible(upPath);
        }
    };
    private static AbstractAction scrollUpChangeLeadAction = new ChangeLeadAction() {
        protected TreePath getNewLeadPath(final JTree tree) {
            return getUpPath(tree);
        }
    };
    static AbstractAction scrollDownChangeSelectionAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            TreePath downPath = getDownPath(tree);
            tree.setSelectionPath(downPath);
            tree.scrollPathToVisible(downPath);
        }
    };
    private static AbstractAction scrollDownExtendSelectionAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            TreePath downPath = getDownPath(tree);
            extendSelection(tree, downPath);
            tree.scrollPathToVisible(downPath);
        }
    };
    private static AbstractAction scrollDownChangeLeadAction = new ChangeLeadAction() {
        protected TreePath getNewLeadPath(final JTree tree) {
            return getDownPath(tree);
        }
    };
    private static AbstractAction scrollLeftAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            Rectangle visibleRect = tree.getVisibleRect();
            int increment = tree.getScrollableUnitIncrement(visibleRect, SwingConstants.HORIZONTAL, -1);
            visibleRect.translate(-increment, 0);
            tree.scrollRectToVisible(visibleRect);
        }
    };
    private static AbstractAction scrollRightAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            Rectangle visibleRect = tree.getVisibleRect();
            int increment = tree.getScrollableUnitIncrement(visibleRect, SwingConstants.HORIZONTAL, 1);
            visibleRect.translate(increment, 0);
            tree.scrollRectToVisible(visibleRect);
        }
    };
    private static AbstractAction expandAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            int leadRow = tree.getLeadSelectionRow();
            tree.expandRow(leadRow);
        }
    };
    private static AbstractAction collapseAction = new TreeAction() {
        public void actionPerformed(final JTree tree) {
            int leadRow = tree.getLeadSelectionRow();
            tree.collapseRow(leadRow);
        }
    };


    public static void installKeyboardActions(final JTree tree) {
        Utilities.installKeyboardActions(tree, JComponent.WHEN_FOCUSED, "Tree.focusInputMap", "Tree.focusInputMap.RightToLeft");
        Utilities.installKeyboardActions(tree, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "Tree.ancestorInputMap", null);

        tree.getActionMap().put("copy", TransferHandler.getCopyAction());
        tree.getActionMap().put("paste", TransferHandler.getPasteAction());
        tree.getActionMap().put("cut", TransferHandler.getCutAction());

        tree.getActionMap().put("selectPrevious", selectPreviousAction);
        tree.getActionMap().put("selectPreviousExtendSelection", selectPreviousExtendSelectionAction);
        tree.getActionMap().put("selectNext", selectNextAction);
        tree.getActionMap().put("selectNextExtendSelection", selectNextExtendSelectionAction);
        tree.getActionMap().put("selectChild", selectChildAction);
        tree.getActionMap().put("selectParent", selectParentAction);

        tree.getActionMap().put("selectFirst", selectFirstAction);
        tree.getActionMap().put("selectFirstExtendSelection", selectFirstExtendSelectionAction);
        tree.getActionMap().put("selectLast", selectLastAction);
        tree.getActionMap().put("selectLastExtendSelection", selectLastExtendSelectionAction);

        tree.getActionMap().put("selectAll", selectAllAction);
        tree.getActionMap().put("clearSelection", clearSelectionAction);

        tree.getActionMap().put("toggleSelectionPreserveAnchor", toggleSelectionPreserveAnchorAction);
        tree.getActionMap().put("extendSelection", extendSelectionAction);
        tree.getActionMap().put("moveSelectionTo", moveSelectionToAction);

        tree.getActionMap().put("selectFirstChangeLead", selectFirstChangeLeadAction);
        tree.getActionMap().put("selectLastChangeLead", selectLastChangeLeadAction);
        tree.getActionMap().put("selectPreviousChangeLead", selectPreviousChangeLeadAction);
        tree.getActionMap().put("selectNextChangeLead", selectNextChangeLeadAction);

        tree.getActionMap().put("scrollUpChangeSelection", scrollUpChangeSelectionAction);
        tree.getActionMap().put("scrollUpExtendSelection", scrollUpExtendSelectionAction);
        tree.getActionMap().put("scrollUpChangeLead", scrollUpChangeLeadAction);
        tree.getActionMap().put("scrollDownChangeSelection", scrollDownChangeSelectionAction);
        tree.getActionMap().put("scrollDownExtendSelection", scrollDownExtendSelectionAction);
        tree.getActionMap().put("scrollDownChangeLead", scrollDownChangeLeadAction);

        tree.getActionMap().put("scrollLeft", scrollLeftAction);
        tree.getActionMap().put("scrollRight", scrollRightAction);

        tree.getActionMap().put("startEditing", startEditingAction);
        tree.getActionMap().put("cancel", cancelAction);

        tree.getActionMap().put("expand", expandAction);
        tree.getActionMap().put("collapse", collapseAction);
    }

    public static void uninstallKeyboardActions(final JTree tree) {
        Utilities.uninstallKeyboardActions(tree, JComponent.WHEN_FOCUSED);
        Utilities.uninstallKeyboardActions(tree, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }
}
