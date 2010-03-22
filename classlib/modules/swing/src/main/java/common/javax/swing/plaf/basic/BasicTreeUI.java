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
 * @author Anton Avtamonov, Sergey Burlak
 */
package javax.swing.plaf.basic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.CellRendererPane;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.MouseInputListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TreeUI;
import javax.swing.text.Position;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.FixedHeightLayoutCache;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.VariableHeightLayoutCache;

import org.apache.harmony.x.swing.TreeCommons;
import org.apache.harmony.x.swing.Utilities;


public class BasicTreeUI extends TreeUI {

    public class CellEditorHandler implements CellEditorListener {
        public void editingCanceled(final ChangeEvent e) {
            completeEditing(false, false, false);
        }

        public void editingStopped(final ChangeEvent e) {
            completeEditing(false, false, true);
        }
    };

    public class ComponentHandler extends ComponentAdapter implements ActionListener {
        protected Timer timer;
        protected JScrollBar scrollBar;

        public void componentMoved(final ComponentEvent e) {
            JScrollPane pane = getScrollPane();
            if (pane == null) {
                return;
            }

            if (pane.getHorizontalScrollBar().getValueIsAdjusting()) {
                scrollBar = pane.getHorizontalScrollBar();
            } else if (pane.getVerticalScrollBar().getValueIsAdjusting()) {
                scrollBar = pane.getVerticalScrollBar();
            } else {
                return;
            }
            startTimer();
        }

        protected void startTimer() {
            if (timer == null) {
                timer = new Timer(200, this);
                timer.setRepeats(false);
            }
            timer.restart();
        }

        protected JScrollPane getScrollPane() {
            Container parent = tree.getParent();
            if (!(parent instanceof JViewport)) {
                return null;
            }
            parent = parent.getParent();
            return parent instanceof JScrollPane ? (JScrollPane)parent : null;
        }

        public void actionPerformed(final ActionEvent e) {
            if (scrollBar.getValueIsAdjusting()) {
                timer.restart();
            } else {
                timer.stop();
                updateSize();
            }
        }
    };

    public class FocusHandler implements FocusListener {
        public void focusGained(final FocusEvent e) {
            repaintFocusedRow();
        }

        public void focusLost(final FocusEvent e) {
            repaintFocusedRow();
        }


        private void repaintFocusedRow() {
            TreePath focusedPath = tree.getLeadSelectionPath();
            if (focusedPath == null) {
                return;
            }
            tree.repaint(getPathBounds(tree, focusedPath));
        }
    };

    public class KeyHandler extends KeyAdapter {
        protected Action repeatKeyAction;
        protected boolean isKeyDown;

        private StringBuffer searchPrefix = new StringBuffer();
        private Timer searchTimer = new Timer(1000, new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                resetSearch();
            }
        });

        public void keyTyped(final KeyEvent e) {
            int rowCount = tree.getRowCount();
            if (rowCount == 0 
                || (e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0
                || (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                
                return;
            }

            int startRow = tree.getLeadSelectionRow() + (searchPrefix.length() == 0 ? 1 : 0);
            if (startRow >= rowCount || startRow == -1) {
                startRow = 0;
            }

            searchPrefix.append(e.getKeyChar());

            TreePath nextPath = tree.getNextMatch(searchPrefix.toString(), startRow, Position.Bias.Forward);
            if (nextPath == null) {
                if (searchPrefix.length() > 1) {
                    resetSearch();
                    searchPrefix.append(e.getKeyChar());
                    startRow++;
                    if (startRow >= rowCount) {
                        startRow = 0;
                    }
                    nextPath = tree.getNextMatch(searchPrefix.toString(), startRow, Position.Bias.Forward);
                }
            }
            if (nextPath != null) {
                searchTimer.stop();
                tree.setSelectionPath(nextPath);
                searchTimer.restart();
            } else {
                resetSearch();
            }
        }

        public void keyPressed(final KeyEvent e) {
        }

        public void keyReleased(final KeyEvent e) {
        }

        private void resetSearch() {
            searchPrefix.delete(0, searchPrefix.length());
            searchTimer.stop();
        }
    };

    public class MouseHandler extends MouseAdapter implements MouseMotionListener {
        private DnDMouseHelper dndhelper = new DnDMouseHelper(tree);

        public void mouseDragged(final MouseEvent e) {
            if (!tree.isEnabled()) {
                return;
            }
            dndhelper.mouseDragged(e);
        }

        public void mousePressed(final MouseEvent e) {
            if (!tree.isEnabled()) {
                return;
            }

            tree.requestFocus();

            TreePath path = tree.getPathForLocation(e.getX(), e.getY());
            dndhelper.mousePressed(e, tree.getDragEnabled(), path != null,
                                   tree.isPathSelected(path));

            if (!tree.getDragEnabled()) {
                startEditing(path, e);
                processSelectionAndExpansion(path, e);
            } else if (path == null) {
                processSelectionAndExpansion(path, e);
            } else if (!tree.isPathSelected(path)) {
                selectPathForEvent(path, e);
            }
        }

        public void mouseReleased(final MouseEvent e) {
            if (!tree.isEnabled()) {
                return;
            }

            if (!tree.getDragEnabled()) {
                return;
            }
            dndhelper.mouseReleased(e);
            if (dndhelper.shouldProcessOnRelease()) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    selectPathForEvent(path, e);
                }
            }
        }

        public void mouseMoved(final MouseEvent e) {
        }


        private void processSelectionAndExpansion(final TreePath path, final MouseEvent e) {
            if (path != null) {
                selectPathForEvent(path, e);
            } else {
                TreePath closestPath = tree.getClosestPathForLocation(e.getX(), e.getY());
                if (closestPath != null) {
                    checkForClickInExpandControl(closestPath, e.getX(), e.getY());
                }
            }
        }
    };

    // It is not clear where and how it should be used. Looks like some part of DnD support.
    public class MouseInputHandler implements MouseInputListener {
        protected Component source;
        protected Component destination;

        public MouseInputHandler(final Component source, final Component destination, final MouseEvent event) {
        }
        public void mouseClicked(final MouseEvent e) {
        }
        public void mouseEntered(final MouseEvent e) {
        }
        public void mouseExited(final MouseEvent e) {
        }
        public void mousePressed(final MouseEvent e) {
        }
        public void mouseReleased(final MouseEvent e) {
        }
        public void mouseDragged(final MouseEvent e) {
        }
        public void mouseMoved(final MouseEvent e) {
        }
        protected void removeFromSource() {
        }
    };

    public class NodeDimensionsHandler extends AbstractLayoutCache.NodeDimensions {
        public Rectangle getNodeDimensions(final Object value, final int row, final int depth,
                                           final boolean expanded, final Rectangle bounds) {

            if (value == null) {
                return bounds;
            }

            Rectangle result = bounds == null ? new Rectangle() : bounds;

            if (isEditing(tree) && editingRow == row) {
                result.setSize(editingComponent.getSize());
            } else {
                Component c = getCellRenderer().getTreeCellRendererComponent(tree, value, false,
                                                                             expanded, tree.getModel().isLeaf(value), row,
                                                                             false);
                result.setSize(c.getPreferredSize());
            }

            int rowX = tree.getComponentOrientation().isLeftToRight()
                            ? getRowX(row, depth)
                            : tree.getWidth() - getRowX(row, depth) - result.width;

            result.setLocation(rowX, result.y);

            return result;
        }

        protected int getRowX(final int row, final int depth) {
            return BasicTreeUI.this.getRowX(row, depth);
        }
    };

    public class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent e) {
            String changedProperty = e.getPropertyName();
            if (JTree.CELL_RENDERER_PROPERTY.equals(changedProperty)) {
                setCellRenderer((TreeCellRenderer)e.getNewValue());
            } else if (JTree.CELL_EDITOR_PROPERTY.equals(changedProperty)) {
                setCellEditor((TreeCellEditor)e.getNewValue());
            } else if (JTree.TREE_MODEL_PROPERTY.equals(changedProperty)) {
                setModel((TreeModel)e.getNewValue());
            } else if (JTree.ROOT_VISIBLE_PROPERTY.equals(changedProperty)) {
                setRootVisible(((Boolean)e.getNewValue()).booleanValue());
            } else if (JTree.SHOWS_ROOT_HANDLES_PROPERTY.equals(changedProperty)) {
                setShowsRootHandles(((Boolean)e.getNewValue()).booleanValue());
            } else if (JTree.ROW_HEIGHT_PROPERTY.equals(changedProperty)) {
                setRowHeight(((Integer)e.getNewValue()).intValue());
            } else if (JTree.EDITABLE_PROPERTY.equals(changedProperty)) {
                setEditable(((Boolean)e.getNewValue()).booleanValue());
            } else if (JTree.LARGE_MODEL_PROPERTY.equals(changedProperty)) {
                setLargeModel(((Boolean)e.getNewValue()).booleanValue());
            } else if (JTree.SELECTION_MODEL_PROPERTY.equals(changedProperty)) {
                setSelectionModel((TreeSelectionModel)e.getNewValue());
            } else if (JTree.LEAD_SELECTION_PATH_PROPERTY.equals(changedProperty)) {
                TreePath leadPath = (TreePath)e.getNewValue();
                lastSelectedRow = leadPath != null ? tree.getRowForPath(leadPath) : -1;
            }
        }
    };

    // In our implementation it's not required
    public class SelectionModelPropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent event) {
        }
    };

    public class TreeExpansionHandler implements TreeExpansionListener {
        public void treeCollapsed(final TreeExpansionEvent e) {
            pathWasCollapsed(e.getPath());
            resetDrawingCache();
        }

        public void treeExpanded(final TreeExpansionEvent e) {
            pathWasExpanded(e.getPath());
            resetDrawingCache();
        }
    };

    public class TreeCancelEditingAction extends AbstractAction {
        public TreeCancelEditingAction(final String name) {
            super(name);
        }

        public void actionPerformed(final ActionEvent e) {
            BasicTreeKeyboardActions.cancelAction.actionPerformed(e);
        }

        public boolean isEnabled() {
            return BasicTreeKeyboardActions.cancelAction.isEnabled();
        }
    };

    public class TreeHomeAction extends AbstractAction {
        protected int direction;

        public TreeHomeAction(final int direction, final String name) {
            super(name);
            this.direction = direction;
        }

        public void actionPerformed(final ActionEvent e) {
            if (direction > 0) {
                BasicTreeKeyboardActions.selectLastAction.actionPerformed(e);
            } else {
                BasicTreeKeyboardActions.selectFirstAction.actionPerformed(e);
            }
        }

        public boolean isEnabled() {
            return direction > 0 ? BasicTreeKeyboardActions.selectLastAction.isEnabled()
                                 : BasicTreeKeyboardActions.selectFirstAction.isEnabled();
        }
    };

    public class TreeIncrementAction extends AbstractAction {
        protected int direction;

        public TreeIncrementAction(final int direction, final String name) {
            super(name);
            this.direction = direction;
        }

        public void actionPerformed(final ActionEvent e) {
            if (direction > 0) {
                BasicTreeKeyboardActions.selectNextAction.actionPerformed(e);
            } else {
                BasicTreeKeyboardActions.selectPreviousAction.actionPerformed(e);
            }
        }

        public boolean isEnabled() {
            return direction > 0 ? BasicTreeKeyboardActions.selectNextAction.isEnabled()
                                 : BasicTreeKeyboardActions.selectPreviousAction.isEnabled();
        }
    };

    public class TreePageAction extends AbstractAction {
        protected int direction;

        public TreePageAction(final int direction, final String name) {
            super(name);
            this.direction = direction;
        }

        public void actionPerformed(final ActionEvent e) {
            if (direction > 0) {
                BasicTreeKeyboardActions.scrollDownChangeSelectionAction.actionPerformed(e);
            } else {
                BasicTreeKeyboardActions.scrollUpChangeSelectionAction.actionPerformed(e);
            }
        }

        public boolean isEnabled() {
            return direction > 0 ? BasicTreeKeyboardActions.scrollDownChangeSelectionAction.isEnabled()
                                 : BasicTreeKeyboardActions.scrollUpChangeSelectionAction.isEnabled();
        }
    };

    public class TreeTraverseAction extends AbstractAction {
        protected int direction;

        public TreeTraverseAction(final int direction, final String name) {
            super(name);
            this.direction = direction;
        }
        public void actionPerformed(final ActionEvent e) {
            if (direction > 0) {
                BasicTreeKeyboardActions.selectChildAction.actionPerformed(e);
            } else {
                BasicTreeKeyboardActions.selectParentAction.actionPerformed(e);
            }
        }

        public boolean isEnabled() {
            return direction > 0 ? BasicTreeKeyboardActions.selectChildAction.isEnabled()
                                 : BasicTreeKeyboardActions.selectParentAction.isEnabled();
        }
    };

    public class TreeToggleAction extends AbstractAction {
        public TreeToggleAction(final String name) {
            super(name);
        }

        public void actionPerformed(final ActionEvent e) {
            TreePath path = tree.getLeadSelectionPath();
            if (path == null) {
                return;
            }

            toggleExpandState(path);
        }

        public boolean isEnabled() {
            return tree.getLeadSelectionPath() != null;
        }
    };


    public class TreeSelectionHandler implements TreeSelectionListener {
        public void valueChanged(final TreeSelectionEvent e) {
            completeEditing();
            TreePath path = e.getNewLeadSelectionPath();
            TreePath oldLeadPath = tree.getLeadSelectionPath();
            tree.setAnchorSelectionPath(path);
            tree.setLeadSelectionPath(path);
            if (tree.getExpandsSelectedPaths() && path != null) {
                tree.makeVisible(path);
            }

            repaintPaths(e.getPaths());
            repaintPath(oldLeadPath);
            repaintPath(path);
        }

        private void repaintPaths(final TreePath[] paths) {
            if (paths == null || paths.length == 0) {
                return;
            }
            for (int i = 0; i < paths.length; i++) {
                repaintPath(paths[i]);
            }
        }

        private void repaintPath(final TreePath path) {
            if (path == null) {
                return;
            }
            Rectangle pathBounds = tree.getPathBounds(path);
            if (pathBounds != null) {
                tree.repaint(pathBounds);
            }
        }
    };

    public class TreeModelHandler implements TreeModelListener {
        public void treeNodesChanged(final TreeModelEvent e) {
            treeState.treeNodesChanged(e);
            updateTreeSize(e);
        }

        public void treeNodesInserted(final TreeModelEvent e) {
            treeState.treeNodesInserted(e);
            updateTreeSize(e);
        }

        public void treeNodesRemoved(final TreeModelEvent e) {
            treeState.treeNodesRemoved(e);
            updateTreeSize(e);
        }

        public void treeStructureChanged(final TreeModelEvent e) {
            treeState.treeStructureChanged(e);
            updateTreeSize(e);
        }

        private void updateTreeSize(final TreeModelEvent e) {
            TreePath eventRootPath = e.getTreePath();
            if (eventRootPath == null) {
                return;
            }
            if (tree.isVisible(eventRootPath)) {
                updateSize();
            }
            resetDrawingCache();
            tree.repaint();
        }
    };

    private class TreeTransferHandler extends TransferHandler {
        private final String lineSeparator = System.getProperty("line.separator");

        public int getSourceActions(final JComponent c) {
            return COPY;
        }

        protected Transferable createTransferable(final JComponent c) {
            if (tree.getSelectionCount() == 0) {
                return null;
            }

            StringBuilder content = new StringBuilder();
            TreePath[] selectionPaths = tree.getSelectionPaths();
            for (int i = 0; i < selectionPaths.length; i++) {
                content.append(selectionPaths[i].getLastPathComponent());
                if (i < selectionPaths.length - 1) {
                    content.append(lineSeparator);
                }
            }
            return new StringSelection(content.toString());
        }
    }

    protected transient Icon collapsedIcon;
    protected transient Icon expandedIcon;
    protected Dimension preferredMinSize;
    protected JTree tree;
    protected transient TreeCellRenderer currentCellRenderer;
    protected transient TreeCellEditor cellEditor;
    protected CellRendererPane rendererPane;
    protected Dimension preferredSize;
    protected AbstractLayoutCache treeState;
    protected Hashtable<javax.swing.tree.TreePath, java.lang.Boolean> drawingCache;
    protected AbstractLayoutCache.NodeDimensions nodeDimensions;
    protected TreeModel treeModel;
    protected TreeSelectionModel treeSelectionModel;
    protected Component editingComponent;
    protected TreePath editingPath;

    protected int leftChildIndent;
    protected int rightChildIndent;
    protected int totalChildIndent;
    protected int lastSelectedRow;
    protected int depthOffset;
    protected int editingRow;

    protected boolean createdRenderer;
    protected boolean createdCellEditor;
    // Is not used in our implementation
    protected boolean stopEditingInCompleteEditing;
    protected boolean validCachedPreferredSize;
    protected boolean editorHasDifferentSize;
    protected boolean largeModel;

    private Color hashColor;
    private int rowHeight;
    private PropertyChangeListener propertyChangeHandler;
    private boolean isRootVisible;
    private boolean isEditable;
    private boolean showsRootHandles;
    private KeyListener keyHandler;
    private MouseListener mouseHandler;
    private FocusListener focusHandler;
    private TreeExpansionListener expansionHandler;
    private PropertyChangeListener selectionModelPropertyChangeHandler;
    private TreeModelListener modelHandler;
    private TreeSelectionListener selectionHandler;
    private CellEditorListener cellEditorHandler;
    private ComponentListener componentHandler;

    private TreeCommons.PaintTreeContext paintContext = new TreeCommons.PaintTreeContext() {
        public JTree getTree() {
            return tree;
        }

        public Hashtable getDrawingCache() {
            return drawingCache;
        }

        public AbstractLayoutCache getLayoutCache() {
            return treeState;
        }

        public boolean paintHandles() {
            return true;
        }

        public boolean paintHorizontalSeparators() {
            return false;
        }

        public boolean isEditing(final TreePath path) {
            return BasicTreeUI.this.isEditing(tree) && editingPath.equals(path);
        }

        public void paintVerticalPartOfLeg(final Graphics g, final Rectangle clipBounds, final Insets insets, final TreePath path) {
            BasicTreeUI.this.paintVerticalPartOfLeg(g, clipBounds, insets, path);
        }

        public void paintHorizontalPartOfLeg(final Graphics g, final Rectangle clipBounds, final Insets insets, final Rectangle bounds, final TreePath path, final int row, final boolean isExpanded, final boolean hasBeenExpanded, final boolean isLeaf) {
            BasicTreeUI.this.paintHorizontalPartOfLeg(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
        }

        public void paintExpandControl(final Graphics g, final Rectangle clipBounds, final Insets insets, final Rectangle bounds, final TreePath path, final int row, final boolean isExpanded, final boolean hasBeenExpanded, final boolean isLeaf) {
            BasicTreeUI.this.paintExpandControl(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
        }

        public void paintRow(final Graphics g, final Rectangle clipBounds, final Insets insets, final Rectangle bounds, final TreePath path, final int row, final boolean isExpanded, final boolean hasBeenExpanded, final boolean isLeaf) {
            BasicTreeUI.this.paintRow(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
        }

        public void paintHorizontalSeparators(final Graphics g, final JComponent tree) {
        }
    };


    public static ComponentUI createUI(final JComponent c) {
        return new BasicTreeUI();
    }

    protected Color getHashColor() {
        return hashColor;
    }

    protected void setHashColor(final Color color) {
        hashColor = color;
    }

    public void setLeftChildIndent(final int newIndent) {
        leftChildIndent = newIndent;
        totalChildIndent = leftChildIndent + rightChildIndent;
        updateSize();
    }

    public int getLeftChildIndent() {
        return leftChildIndent;
    }

    public void setRightChildIndent(final int newIndent) {
        rightChildIndent = newIndent;
        totalChildIndent = rightChildIndent + leftChildIndent;
        updateSize();
    }

    public int getRightChildIndent() {
        return rightChildIndent;
    }

    public void setExpandedIcon(final Icon newIcon) {
        expandedIcon = newIcon;
    }

    public Icon getExpandedIcon() {
        return expandedIcon;
    }

    public void setCollapsedIcon(final Icon newIcon) {
        collapsedIcon = newIcon;
    }

    public Icon getCollapsedIcon() {
        return collapsedIcon;
    }

    protected void setLargeModel(final boolean b) {
        boolean fixedHeightUsed = useFixedHeightLayoutCache();
        largeModel = b;

        if (fixedHeightUsed != useFixedHeightLayoutCache()) {
            updateComponentHandler();
            treeState = createLayoutCache();
            configureLayoutCache();
            updateSize();
        }
    }

    protected boolean isLargeModel() {
        return largeModel;
    }

    protected void setRowHeight(final int rowHeight) {
        boolean fixedHeightUsed = useFixedHeightLayoutCache();
        this.rowHeight = tree.getRowHeight();
        if (fixedHeightUsed != useFixedHeightLayoutCache()) {
            updateComponentHandler();
            treeState = createLayoutCache();
            configureLayoutCache();
        } else {
            treeState.setRowHeight(rowHeight);
        }
        updateSize();
    }

    protected int getRowHeight() {
        return rowHeight;
    }

    protected void setCellRenderer(final TreeCellRenderer renderer) {
        completeEditing();
        updateRenderer();
        if (createdCellEditor) {
            tree.setCellEditor(null);
        }
        updateSize();
    }

    protected TreeCellRenderer getCellRenderer() {
        return currentCellRenderer;
    }

    protected void setModel(final TreeModel model) {
        if (treeModel != null) {
            treeModel.removeTreeModelListener(modelHandler);
        }

        treeModel = model;
        if (treeModel != null) {
            treeModel.addTreeModelListener(modelHandler);
        }

        treeState.setModel(treeModel);
        updateSize();
        resetDrawingCache();
    }

    protected TreeModel getModel() {
        return treeModel;
    }

    protected void setRootVisible(final boolean visible) {
        treeState.setRootVisible(visible);
        isRootVisible = tree.isRootVisible();

        updateDepthOffset();
        updateSize();
    }

    protected boolean isRootVisible() {
        return isRootVisible;
    }

    protected void setShowsRootHandles(final boolean showRoot) {
        showsRootHandles = tree.getShowsRootHandles();
        updateDepthOffset();
        updateSize();
    }

    protected boolean getShowsRootHandles() {
        return showsRootHandles;
    }

    protected void setCellEditor(final TreeCellEditor editor) {
        completeEditing();
        updateCellEditor();
    }

    protected TreeCellEditor getCellEditor() {
        return tree.getCellEditor();
    }

    protected void setEditable(final boolean editable) {
        isEditable = tree.isEditable();
        completeEditing();
        updateCellEditor();
    }

    protected boolean isEditable() {
        return isEditable;
    }

    protected void setSelectionModel(final TreeSelectionModel newSelectionModel) {
        if (treeSelectionModel != null) {
            treeSelectionModel.removePropertyChangeListener(selectionModelPropertyChangeHandler);
            treeSelectionModel.removeTreeSelectionListener(selectionHandler);
            treeSelectionModel.setRowMapper(null);

            treeSelectionModel.clearSelection();
        }
        treeSelectionModel = newSelectionModel;
        if (treeSelectionModel != null) {
            treeSelectionModel.addPropertyChangeListener(selectionModelPropertyChangeHandler);
            treeSelectionModel.addTreeSelectionListener(selectionHandler);
            treeSelectionModel.setRowMapper(treeState);
        }
        resetDrawingCache();
        tree.repaint();
    }

    protected TreeSelectionModel getSelectionModel() {
        return treeSelectionModel;
    }

    public Rectangle getPathBounds(final JTree tree, final TreePath path) {
        Rectangle result = treeState.getBounds(path, null);
        if (result == null) {
            return null;
        }

        Insets insets = tree.getInsets();
        if (tree.getComponentOrientation().isLeftToRight()) {
            result.x += insets.left;
        } else {
            result.x -= insets.right;
        }
        result.y += insets.top;

        return result;
    }

    public TreePath getPathForRow(final JTree tree, final int row) {
        return treeState.getPathForRow(row);
    }

    public int getRowForPath(final JTree tree, final TreePath path) {
        return treeState.getRowForPath(path);
    }

    public int getRowCount(final JTree tree) {
        return treeState.getRowCount();
    }

    public TreePath getClosestPathForLocation(final JTree tree, final int x, final int y) {
        Insets insets = tree.getInsets();
        return treeState.getPathClosestTo(x - insets.left, y - insets.top);
    }

    public boolean isEditing(final JTree tree) {
        return editingComponent != null;
    }

    public boolean stopEditing(final JTree tree) {
        if (!isEditing(tree)) {
            return false;
        }
        boolean result = getCellEditor().stopCellEditing();
        completeEditing(false, false, true);

        return result;
    }

    public void cancelEditing(final JTree tree) {
        completeEditing(false, true, false);
    }

    public void startEditingAtPath(final JTree tree, final TreePath path) {
        startEditing(path, null);
    }

    public TreePath getEditingPath(final JTree tree) {
        return editingPath;
    }

    public void installUI(final JComponent c) {
        tree = (JTree)c;
        prepareForUIInstall();

        setSelectionModel(tree.getSelectionModel());

        installDefaults();
        installComponents();
        installListeners();
        installKeyboardActions();

        completeUIInstall();
    }

    public void uninstallUI(final JComponent c) {
        prepareForUIUninstall();

        uninstallKeyboardActions();
        uninstallListeners();
        uninstallComponents();
        uninstallDefaults();

        treeSelectionModel = null;
        treeModel = null;

        tree.setCellRenderer(null);
        tree.setCellEditor(null);

        completeUIUninstall();
    }

    public void paint(final Graphics g, final JComponent c) {
        TreeCommons.paintTree(g, paintContext);
    }

    public void setPreferredMinSize(final Dimension newSize) {
        preferredMinSize = newSize;
    }

    public Dimension getPreferredMinSize() {
        return preferredMinSize != null ? (Dimension)preferredMinSize.clone() : null;
    }

    public Dimension getPreferredSize(final JComponent c) {
        return getPreferredSize(c, true);
    }

    public Dimension getPreferredSize(final JComponent c, final boolean checkConsistancy) {
        if (!validCachedPreferredSize) {
            updateCachedPreferredSize();
        }
        return (Dimension)preferredSize.clone();
    }

    public Dimension getMinimumSize(final JComponent c) {
        Dimension minPrefSize = getPreferredMinSize();
        return minPrefSize != null ? (Dimension)minPrefSize.clone() : new Dimension();
    }

    public Dimension getMaximumSize(final JComponent c) {
        Dimension prefSize = getPreferredSize(c);
        return prefSize != null ? (Dimension)prefSize.clone() : null;
    }


    protected void prepareForUIInstall() {
        preferredSize = new Dimension();
        drawingCache = new Hashtable<javax.swing.tree.TreePath, java.lang.Boolean>();

        treeState = createLayoutCache();
        nodeDimensions = createNodeDimensions();
        rendererPane = createCellRendererPane();
        rendererPane.setVisible(false);

        setModel(tree.getModel());
    }

    protected void prepareForUIUninstall() {
    }

    protected void completeUIInstall() {
        isEditable = tree.isEditable();
        largeModel = tree.isLargeModel();
        isRootVisible = tree.isRootVisible();
        rowHeight = tree.getRowHeight();
        showsRootHandles = tree.getShowsRootHandles();

        updateRenderer();
        updateCellEditor();
        updateDepthOffset();
        configureLayoutCache();
        updateSize();
    }

    protected void completeUIUninstall() {
        drawingCache = null;
        treeModel = null;

        rendererPane = null;
        treeState = null;
        nodeDimensions = null;
    }

    protected void installDefaults() {
        stopEditingInCompleteEditing = true;
        lastSelectedRow = -1;

        LookAndFeel.installProperty(tree, "rowHeight", UIManager.get("Tree.rowHeight"));
        LookAndFeel.installProperty(tree, "rootVisible", Boolean.TRUE);
        LookAndFeel.installProperty(tree, "scrollsOnExpand", UIManager.get("Tree.scrollsOnExpand"));
        LookAndFeel.installProperty(tree, "expandsSelectedPaths", Boolean.TRUE);
        LookAndFeel.installProperty(tree, "toggleClickCount", new Integer(2));
        LookAndFeel.installProperty(tree, "visibleRowCount", new Integer(20));

        setHashColor(UIManager.getColor("Tree.hash"));
        setLeftChildIndent(UIManager.getInt("Tree.leftChildIndent"));
        setRightChildIndent(UIManager.getInt("Tree.rightChildIndent"));

        LookAndFeel.installColorsAndFont(tree, "Tree.background", "Tree.foreground", "Tree.font");
        LookAndFeel.installProperty(tree, "opaque", Boolean.TRUE);

        setCollapsedIcon(UIManager.getIcon("Tree.collapsedIcon"));
        setExpandedIcon(UIManager.getIcon("Tree.expandedIcon"));

        tree.setTransferHandler(new TreeTransferHandler());
    }

    protected void uninstallDefaults() {
        Utilities.uninstallColorsAndFont(tree);
        tree.setTransferHandler(null);
    }

    protected void installListeners() {
        propertyChangeHandler = createPropertyChangeListener();
        tree.addPropertyChangeListener(propertyChangeHandler);

        keyHandler = createKeyListener();
        tree.addKeyListener(keyHandler);

        mouseHandler = createMouseListener();
        tree.addMouseListener(mouseHandler);
        if (mouseHandler instanceof MouseMotionListener) {
            tree.addMouseMotionListener((MouseMotionListener)mouseHandler);
        }

        focusHandler = createFocusListener();
        tree.addFocusListener(focusHandler);

        expansionHandler = createTreeExpansionListener();
        tree.addTreeExpansionListener(expansionHandler);

        modelHandler = createTreeModelListener();
        if (treeModel != null) {
            treeModel.addTreeModelListener(modelHandler);
        }

        selectionModelPropertyChangeHandler = createSelectionModelPropertyChangeListener();
        selectionHandler = createTreeSelectionListener();
        if (treeSelectionModel != null) {
            treeSelectionModel.addPropertyChangeListener(selectionModelPropertyChangeHandler);
            treeSelectionModel.addTreeSelectionListener(selectionHandler);
        }

        cellEditorHandler = createCellEditorListener();
    }

    protected void uninstallListeners() {
        tree.removePropertyChangeListener(propertyChangeHandler);
        tree.removeKeyListener(keyHandler);
        tree.removeMouseListener(mouseHandler);
        if (mouseHandler instanceof MouseMotionListener) {
            tree.removeMouseMotionListener((MouseMotionListener)mouseHandler);
        }
        tree.removeFocusListener(focusHandler);
        tree.removeTreeExpansionListener(expansionHandler);
        if (treeModel != null) {
            treeModel.removeTreeModelListener(modelHandler);
        }
        if (treeSelectionModel != null) {
            treeSelectionModel.removePropertyChangeListener(selectionModelPropertyChangeHandler);
            treeSelectionModel.removeTreeSelectionListener(selectionHandler);
        }

        propertyChangeHandler = null;
        keyHandler = null;
        mouseHandler = null;
        focusHandler = null;
        expansionHandler = null;
        modelHandler = null;
        selectionModelPropertyChangeHandler = null;
        selectionHandler = null;
        cellEditorHandler = null;
    }

    protected void installComponents() {
        tree.add(rendererPane);
    }

    protected void uninstallComponents() {
        tree.remove(rendererPane);
    }

    protected void installKeyboardActions() {
        BasicTreeKeyboardActions.installKeyboardActions(tree);
    }

    protected void uninstallKeyboardActions() {
        BasicTreeKeyboardActions.uninstallKeyboardActions(tree);
    }

    protected AbstractLayoutCache.NodeDimensions createNodeDimensions() {
        return new NodeDimensionsHandler();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    protected MouseListener createMouseListener() {
        return new MouseHandler();
    }

    protected FocusListener createFocusListener() {
        return new FocusHandler();
    }

    protected KeyListener createKeyListener() {
        return new KeyHandler();
    }

    protected PropertyChangeListener createSelectionModelPropertyChangeListener() {
        return new SelectionModelPropertyChangeHandler();
    }

    protected TreeSelectionListener createTreeSelectionListener() {
        return new TreeSelectionHandler();
    }

    protected CellEditorListener createCellEditorListener() {
        return new CellEditorHandler();
    }

    protected ComponentListener createComponentListener() {
        return new ComponentHandler();
    }

    protected TreeExpansionListener createTreeExpansionListener() {
        return new TreeExpansionHandler();
    }

    protected AbstractLayoutCache createLayoutCache() {
        return rowHeight > 0 && largeModel ? (AbstractLayoutCache)new FixedHeightLayoutCache()
                                           : (AbstractLayoutCache)new VariableHeightLayoutCache();
    }

    protected CellRendererPane createCellRendererPane() {
        return new CellRendererPane();
    }

    protected TreeCellEditor createDefaultCellEditor() {
        DefaultTreeCellRenderer renderer = currentCellRenderer instanceof DefaultTreeCellRenderer
            ? (DefaultTreeCellRenderer)currentCellRenderer
            : null;

        return new DefaultTreeCellEditor(tree, renderer);
    }

    protected TreeCellRenderer createDefaultCellRenderer() {
        return new DefaultTreeCellRenderer();
    }

    protected TreeModelListener createTreeModelListener() {
        return new TreeModelHandler();
    }

    protected void paintRow(final Graphics g, final Rectangle clipBounds, final Insets insets,
                            final Rectangle bounds, final TreePath path, final int row,
                            final boolean isExpanded, final boolean hasBeenExpanded,
                            final boolean isLeaf) {

        Component c = getCellRenderer().getTreeCellRendererComponent(tree, path.getLastPathComponent(),
                                                                     treeSelectionModel.isPathSelected(path),
                                                                     isExpanded, isLeaf, row, tree.isFocusOwner() && path.equals(tree.getLeadSelectionPath()));
        rendererPane.paintComponent(g, c, tree, bounds);
    }

    protected void paintExpandControl(final Graphics g, final Rectangle clipBounds, final Insets insets,
                                      final Rectangle bounds, final TreePath path, final int row,
                                      final boolean isExpanded, final boolean hasBeenExpanded,
                                      final boolean isLeaf) {

        if (!shouldPaintExpandControl(path, row, isExpanded, hasBeenExpanded, isLeaf)) {
            return;
        }

        Rectangle pathBounds = tree.getPathBounds(path);
        int x = tree.getComponentOrientation().isLeftToRight()
                ? pathBounds.x - rightChildIndent
                : pathBounds.x + pathBounds.width  + rightChildIndent;
        int y = pathBounds.y + pathBounds.height / 2;
        if (isExpanded) {
            drawCentered(tree, g, getExpandedIcon(), x, y);
        } else {
            drawCentered(tree, g, getCollapsedIcon(), x, y);
        }
    }

    protected void drawCentered(final Component c, final Graphics g, final Icon icon, final int x, final int y) {
        if (icon == null) {
            return;
        }

        icon.paintIcon(c, g, x - icon.getIconWidth() / 2, y - icon.getIconHeight() / 2);
    }

    protected void drawDashedHorizontalLine(final Graphics g, final int y, final int x1, final int x2) {
        if (!(g instanceof Graphics2D)) {
            paintHorizontalLine(g, tree, y, x1, x2);
            return;
        }

        drawDashedLine((Graphics2D)g, x1, x2, y, y);
    }

    protected void drawDashedVerticalLine(final Graphics g, final int x, final int y1, final int y2) {
        if (!(g instanceof Graphics2D)) {
            paintVerticalLine(g, tree, x, y1, y2);
            return;
        }

        drawDashedLine((Graphics2D)g, x, x, y1, y2);
    }

    protected void paintVerticalLine(final Graphics g, final JComponent c, final int x, final int top, final int bottom) {
        g.drawLine(x, top, x, bottom);
    }

    protected void paintHorizontalLine(final Graphics g, final JComponent c, final int y, final int left, final int right) {
        g.drawLine(left, y, right, y);
    }

    protected void paintVerticalPartOfLeg(final Graphics g, final Rectangle clipBounds,
                                          final Insets insets, final TreePath path) {

        if (!tree.isRootVisible() && !tree.getShowsRootHandles() && isRootPath(path)) {
            return;
        }

        g.setColor(getHashColor());

        int offset = getRowX(tree.getRowForPath(path), path.getPathCount() - 1) + leftChildIndent;
        int x = tree.getComponentOrientation().isLeftToRight()
                   ? insets.left + offset
                   : tree.getWidth() - insets.right - offset;

        TreePath pathToLastVisibleChild = getLastChildPath(path);

        Rectangle pathBounds;
        int top;
        if (tree.getShowsRootHandles() && !tree.isRootVisible() && isRootPath(path)) {
            pathBounds = tree.getPathBounds(getFirstChildPath(path));
            top = pathBounds.y + pathBounds.height / 2;
        } else {
            pathBounds = tree.getPathBounds(path);
            top = pathBounds.y + pathBounds.height + getVerticalLegBuffer();
        }

        Rectangle pathToLastVisibleChildBounds = tree.getPathBounds(pathToLastVisibleChild);
        if (pathToLastVisibleChildBounds != null) {
            int bottom = pathToLastVisibleChildBounds.y + pathToLastVisibleChildBounds.height / 2;
            paintVerticalLine(g, tree, x, top, bottom);
        }
    }

    protected void paintHorizontalPartOfLeg(final Graphics g, final Rectangle clipBounds, final Insets insets,
                                            final Rectangle bounds, final TreePath path, final int row,
                                            final boolean isExpanded, final boolean hasBeenExpanded,
                                            final boolean isLeaf) {

        if (!tree.isRootVisible() && !tree.getShowsRootHandles() && path.getPathCount() == 2
            || isRootPath(path) && !tree.getShowsRootHandles()) {

            return;
        }

        g.setColor(getHashColor());

        int left;
        int right;
        int offset = getRowX(row, path.getPathCount() - 1) - rightChildIndent;
        if (tree.getComponentOrientation().isLeftToRight()) {
            left = insets.left + offset;
            right = bounds.x - getHorizontalLegBuffer();
        } else {
            left = tree.getWidth() - offset - insets.right;
            right = bounds.x + bounds.width + getHorizontalLegBuffer();
        }
        int y = bounds.y + bounds.height / 2;

        paintHorizontalLine(g, tree, y, left, right);
    }

    protected boolean shouldPaintExpandControl(final TreePath path, final int row, final boolean isExpanded,
                                               final boolean hasBeenExpanded, final boolean isLeaf) {

        return !isLeaf && path.getPathCount() > (tree.getShowsRootHandles() ? 0 : (isRootVisible() ? 1 : 2));
    }

    protected int getVerticalLegBuffer() {
        return 0;
    }

    protected int getHorizontalLegBuffer() {
        return 0;
    }

    protected int getRowX(final int row, final int depth) {
        return totalChildIndent * depthOffset + totalChildIndent * depth;
    }

    protected void updateLayoutCacheExpandedNodes() {
        if (treeModel != null && treeModel.getRoot() != null) {
            updateExpandedDescendants(new TreePath(treeModel.getRoot()));
        }
    }

    protected void updateExpandedDescendants(final TreePath path) {
        Enumeration expandedPaths = tree.getExpandedDescendants(path);
        if (expandedPaths == null) {
            return;
        }
        while (expandedPaths.hasMoreElements()) {
            TreePath expandedPath = (TreePath)expandedPaths.nextElement();
            treeState.setExpandedState(expandedPath, true);
        }
    }

    protected TreePath getLastChildPath(final TreePath parent) {
        if (treeModel.isLeaf(parent.getLastPathComponent())) {
            return null;
        }
        int childCount = treeModel.getChildCount(parent.getLastPathComponent());
        if (childCount == 0) {
            return null;
        }

        return parent.pathByAddingChild(treeModel.getChild(parent.getLastPathComponent(), childCount - 1));
    }

    protected void updateDepthOffset() {
        depthOffset = 0;
        if (!isRootVisible()) {
            depthOffset--;
        }
        if (getShowsRootHandles()) {
            depthOffset++;
        }
    }

    protected void updateCellEditor() {
        if (!isEditable()) {
            return;
        }
        completeEditing();
        if (cellEditor != null) {
            cellEditor.removeCellEditorListener(cellEditorHandler);
        }
        cellEditor = tree.getCellEditor();
        if (cellEditor == null) {
            tree.setCellEditor(createDefaultCellEditor());
            createdCellEditor = true;
            return;
        }
        createdCellEditor = false;
        if (cellEditor != null) {
            cellEditor.addCellEditorListener(cellEditorHandler);
        }
    }

    protected void updateRenderer() {
        currentCellRenderer = tree.getCellRenderer();
        if (currentCellRenderer == null) {
            tree.setCellRenderer(createDefaultCellRenderer());
            createdRenderer = true;
        } else {
            createdRenderer = false;
        }
    }

    protected void configureLayoutCache() {
        treeState.setModel(treeModel);
        treeState.setSelectionModel(treeSelectionModel);
        treeState.setNodeDimensions(nodeDimensions);
        treeState.setRootVisible(isRootVisible);
        treeState.setRowHeight(rowHeight);
        treeState.invalidateSizes();
        updateLayoutCacheExpandedNodes();
    }

    protected void updateSize() {
        validCachedPreferredSize = false;
        tree.treeDidChange();
    }

    protected void updateCachedPreferredSize() {
        Insets insets = tree.getInsets();
        int preferredWidth;
        if (tree.getComponentOrientation().isLeftToRight()) {
            preferredWidth = treeState.getPreferredWidth(null);
        } else {
            preferredWidth = calculatePreferredWidthForRTL();
        }
        preferredSize = new Dimension(preferredWidth + insets.left + insets.right,
                                      treeState.getPreferredHeight() + insets.top + insets.bottom);
        validCachedPreferredSize = true;
    }

    protected void pathWasExpanded(final TreePath path) {
        treeState.setExpandedState(path, true);
        updateExpandedDescendants(path);
        updateSize();

        if (tree.getScrollsOnExpand() && treeModel.getChildCount(path.getLastPathComponent()) > 0) {
            Object firstChild = treeModel.getChild(path.getLastPathComponent(), 0);
            Object lastChild = treeModel.getChild(path.getLastPathComponent(), treeModel.getChildCount(path.getLastPathComponent()) - 1);

            int firstRow = tree.getRowForPath(path.pathByAddingChild(firstChild));
            int lastRow = tree.getRowForPath(path.pathByAddingChild(lastChild));
            ensureRowsAreVisible(firstRow, lastRow);
        }
    }

    protected void pathWasCollapsed(final TreePath path) {
        treeState.setExpandedState(path, false);
        updateSize();
    }

    protected void ensureRowsAreVisible(final int beginRow, final int endRow) {
        Rectangle beginRowBounds = tree.getRowBounds(beginRow);
        Rectangle endRowBounds = tree.getRowBounds(endRow);
        if (beginRowBounds == null && endRowBounds == null) {
            return;
        }

        if (beginRowBounds == null) {
            tree.scrollRectToVisible(endRowBounds);
        } else if (endRowBounds == null) {
            tree.scrollRectToVisible(beginRowBounds);
        } else {
            tree.scrollRectToVisible(beginRowBounds.union(endRowBounds));
        }
    }

    protected void completeEditing() {
        completeEditing(tree.getInvokesStopCellEditing(), !tree.getInvokesStopCellEditing(), false);
    }

    protected void completeEditing(final boolean messageStop, final boolean messageCancel, final boolean messageTree) {
        if (!isEditing(tree)) {
            return;
        }

        if (messageStop) {
            getCellEditor().stopCellEditing();
        }
        if (messageCancel) {
            getCellEditor().cancelCellEditing();
        }
        if (messageTree) {
            treeModel.valueForPathChanged(editingPath, getCellEditor().getCellEditorValue());
        }

        if (!isEditing(tree)) {
            return;
        }
        
        tree.remove(editingComponent);
        editingComponent = null;
        editingPath = null;

        tree.repaint();
        tree.requestFocus();
    }

    protected boolean startEditing(final TreePath path, final MouseEvent event) {
        if (!isEditable()) {
            return false;
        }
        completeEditing();

        if (path == null) {
            return false;
        }

        if (!getCellEditor().isCellEditable(event)) {
            return false;
        }

        if (getCellEditor().shouldSelectCell(event)) {
            tree.setSelectionPath(path);
        }

        Rectangle bounds = tree.getPathBounds(path);
        if (bounds == null) {
            return false;
        }
        int width = bounds.width;

        // TODO: should be another behavior. Should take this information from some cache.
        editorHasDifferentSize = true;
        if (editorHasDifferentSize) {
            bounds.width += 10;
            if (bounds.width < 100) {
                bounds.width = 100;
            }
        }

        if (!tree.getComponentOrientation().isLeftToRight()) {
            bounds.x = bounds.x - (bounds.width - width);
        }

        editingPath = path;
        editingRow = tree.getRowForPath(path);
        editingComponent = cellEditor.getTreeCellEditorComponent(tree, editingPath.getLastPathComponent(), tree.isPathSelected(editingPath), tree.isExpanded(editingPath), treeModel.isLeaf(editingPath.getLastPathComponent()), editingRow);
        tree.add(editingComponent);
        editingComponent.setBounds(bounds);

        editingComponent.validate();
        editingComponent.repaint();
        editingComponent.requestFocus();

        return true;
    }

    protected void checkForClickInExpandControl(final TreePath path, final int mouseX, final int mouseY) {
        if (isLocationInExpandControl(path, mouseX, mouseY)) {
            handleExpandControlClick(path, mouseX, mouseY);
        }
    }

    protected boolean isLocationInExpandControl(final TreePath path, final int mouseX, final int mouseY) {
        if (treeModel == null || path == null || treeModel.isLeaf(path.getLastPathComponent())) {
            return false;
        }
        if (!getShowsRootHandles()) {
            if (isRootVisible() && path.getPathCount() == 1
                || !isRootVisible() && path.getPathCount() <= 2) {

                return false;
            }
        }
        Rectangle pathBounds = tree.getPathBounds(path);
        if (mouseY < pathBounds.y || mouseY > pathBounds.y + pathBounds.height) {
            return false;
        }

        int expandIconWidth = expandedIcon != null ? expandedIcon.getIconWidth() : 8;
        int startExpandZone;
        int endExpandZone;
        if (tree.getComponentOrientation().isLeftToRight()) {
            startExpandZone = pathBounds.x - rightChildIndent - expandIconWidth / 2;
            endExpandZone = startExpandZone + expandIconWidth;
        } else {
            startExpandZone = pathBounds.x + pathBounds.width + rightChildIndent - expandIconWidth / 2;
            endExpandZone = startExpandZone + expandIconWidth;
        }

        return mouseX >= startExpandZone && mouseX <= endExpandZone;
    }

    protected void handleExpandControlClick(final TreePath path, final int mouseX, final int mouseY) {
        toggleExpandState(path);
    }

    protected void toggleExpandState(final TreePath path) {
        if (treeModel.isLeaf(path.getLastPathComponent())) {
            return;
        }

        if (tree.isExpanded(path)) {
            tree.collapsePath(path);
        } else {
            tree.expandPath(path);
        }
    }

    protected boolean isToggleSelectionEvent(final MouseEvent event) {
        return SwingUtilities.isLeftMouseButton(event) && ((event.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0);
    }

    protected boolean isMultiSelectEvent(final MouseEvent event) {
        return SwingUtilities.isLeftMouseButton(event) && ((event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0);
    }

    protected boolean isToggleEvent(final MouseEvent event) {
        return SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == tree.getToggleClickCount();
    }

    protected void selectPathForEvent(final TreePath path, final MouseEvent event) {
        if (isToggleSelectionEvent(event)) {
            TreePath anchorPath = tree.getAnchorSelectionPath();
            if (tree.isPathSelected(path)) {
               tree.removeSelectionPath(path);
            } else {
                tree.addSelectionPath(path);
            }
            tree.setLeadSelectionPath(path);
            tree.setAnchorSelectionPath(anchorPath);
        } else if (isMultiSelectEvent(event)) {
            TreePath anchorPath = tree.getAnchorSelectionPath();
            if (anchorPath == null) {
                tree.setSelectionPath(path);
            } else {
                int anchorRow = tree.getRowForPath(anchorPath);
                int leadRow = tree.getRowForPath(path);
                tree.setSelectionInterval(anchorRow, leadRow);
                tree.setAnchorSelectionPath(anchorPath);
                tree.setLeadSelectionPath(path);
            }
        } else if (SwingUtilities.isLeftMouseButton(event)) {
            tree.setSelectionPath(path);
            if (isToggleEvent(event)) {
                toggleExpandState(path);
            }
        }
    }

    protected boolean isLeaf(final int row) {
        TreePath path = tree.getPathForRow(row);
        if (path == null) {
            return true;
        }
        return treeModel.isLeaf(path.getLastPathComponent());
    }


    private boolean isRootPath(final TreePath path) {
        return path.getPathCount() == 1 && path.getLastPathComponent().equals(treeModel.getRoot());
    }

    private TreePath getFirstChildPath(final TreePath parent) {
        if (treeModel.isLeaf(parent.getLastPathComponent())) {
            return null;
        }

        return parent.pathByAddingChild(treeModel.getChild(parent.getLastPathComponent(), 0));
    }

    private boolean useFixedHeightLayoutCache() {
        return rowHeight > 0 && largeModel;
    }

    private void updateComponentHandler() {
        if (useFixedHeightLayoutCache()) {
            if (componentHandler == null) {
                componentHandler = createComponentListener();
            }
            tree.addComponentListener(componentHandler);
        } else {
            tree.removeComponentListener(componentHandler);
        }
    }

    private void drawDashedLine(final Graphics2D g, final int x1, final int x2, final int y1, final int y2) {
        Stroke oldStroke = g.getStroke();

        g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {1}, 0));
        g.drawLine(x1, y1, x2, y2);

        g.setStroke(oldStroke);
    }

    private void resetDrawingCache() {
        drawingCache.clear();
    }

    private int calculatePreferredWidthForRTL() {
        int result = 0;
        TreePath startPath = new TreePath(treeModel.getRoot());
        TreePath endPath = null;

        Enumeration paths = treeState.getVisiblePathsFrom(startPath);
        if (paths == null) {
            return result;
        } else {
            int currentWidth = tree.getWidth();
            while(paths.hasMoreElements()) {
                TreePath path = (TreePath)paths.nextElement();

                if (isRootPath(path) && !isRootVisible()) {
                    continue;
                }

                Rectangle pathBounds = tree.getPathBounds(path);
                if (pathBounds == null) {
                    continue;
                }

                int width = currentWidth - pathBounds.x;
                if (result < width) {
                    result = width;
                }

                if (path.equals(endPath)) {
                    break;
                }
            }
        }

        return result;
    }
}
