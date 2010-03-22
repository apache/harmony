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

package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleText;
import javax.accessibility.AccessibleValue;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.TreeUI;
import javax.swing.text.Position;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.apache.harmony.luni.util.NotImplementedException;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>JTree</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JTree extends JComponent implements Scrollable, Accessible {
    private static final long serialVersionUID = -3884445419090632712L;

    protected class AccessibleJTree extends AccessibleJComponent implements
            AccessibleSelection, TreeSelectionListener, TreeModelListener,
            TreeExpansionListener {
        private static final long serialVersionUID = -8714565563782619758L;

        protected class AccessibleJTreeNode extends AccessibleContext implements Accessible,
                AccessibleComponent, AccessibleSelection, AccessibleAction {
                
            private Accessible accessibleParent;
            
            public AccessibleJTreeNode(JTree t, TreePath p, Accessible ap)
                    throws NotImplementedException {
                throw new NotImplementedException();
            }

            public AccessibleContext getAccessibleContext() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public String getAccessibleName() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public void setAccessibleName(String s) throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public String getAccessibleDescription() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public void setAccessibleDescription(String s) throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public AccessibleRole getAccessibleRole() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public AccessibleStateSet getAccessibleStateSet() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public Accessible getAccessibleParent() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public int getAccessibleIndexInParent() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public int getAccessibleChildrenCount() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public Accessible getAccessibleChild(int i) throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public Locale getLocale() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public void addPropertyChangeListener(PropertyChangeListener l)
                    throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public void removePropertyChangeListener(PropertyChangeListener l)
                    throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public AccessibleAction getAccessibleAction() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public AccessibleComponent getAccessibleComponent() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public AccessibleSelection getAccessibleSelection() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public AccessibleText getAccessibleText() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public AccessibleValue getAccessibleValue() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Color getBackground() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setBackground(Color c) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Color getForeground() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setForeground(Color c) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Cursor getCursor() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setCursor(Cursor c) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Font getFont() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setFont(Font f) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public FontMetrics getFontMetrics(Font f) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public boolean isEnabled() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setEnabled(boolean b) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public boolean isVisible() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setVisible(boolean b) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public boolean isShowing() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public boolean contains(Point p) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Point getLocationOnScreen() throws NotImplementedException {
                throw new NotImplementedException();
            }

            protected Point getLocationInJTree() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Point getLocation() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setLocation(Point p) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Rectangle getBounds() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setBounds(Rectangle r) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Dimension getSize() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setSize(Dimension d) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Accessible getAccessibleAt(Point p) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public boolean isFocusTraversable() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void requestFocus() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void addFocusListener(FocusListener l) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void removeFocusListener(FocusListener l) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public int getAccessibleSelectionCount() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Accessible getAccessibleSelection(int i) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public boolean isAccessibleChildSelected(int i) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void addAccessibleSelection(int i) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void removeAccessibleSelection(int i) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void clearAccessibleSelection() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void selectAllAccessibleSelection() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public int getAccessibleActionCount() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public String getAccessibleActionDescription(int i) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public boolean doAccessibleAction(int i) throws NotImplementedException {
                throw new NotImplementedException();
            }
        }

        public AccessibleJTree() {
            super();
        }

        public void valueChanged(TreeSelectionEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void fireVisibleDataPropertyChange() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void treeNodesChanged(TreeModelEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void treeNodesInserted(TreeModelEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void treeNodesRemoved(TreeModelEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void treeStructureChanged(TreeModelEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void treeCollapsed(TreeExpansionEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void treeExpanded(TreeExpansionEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleRole getAccessibleRole() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public Accessible getAccessibleAt(Point p) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public int getAccessibleChildrenCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public Accessible getAccessibleChild(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public int getAccessibleIndexInParent() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleSelection getAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleSelectionCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Accessible getAccessibleSelection(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public boolean isAccessibleChildSelected(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void addAccessibleSelection(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void removeAccessibleSelection(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void clearAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void selectAllAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }
    }

    public static class DynamicUtilTreeNode extends DefaultMutableTreeNode {
        private static final long serialVersionUID = -2795134038906279615L;

        protected boolean hasChildren;

        protected Object childValue;

        protected boolean loadedChildren;

        public static void createChildren(DefaultMutableTreeNode parent, Object children) {
            loadChildren(parent, children);
        }

        public DynamicUtilTreeNode(Object value, Object children) {
            super(value, false);
            childValue = children;
            loadedChildren = !(children instanceof Object[]) && !(children instanceof Vector)
                    && !(children instanceof Hashtable);
            setAllowsChildren(!loadedChildren);
        }

        @Override
        public boolean isLeaf() {
            return !getAllowsChildren();
        }

        @Override
        public int getChildCount() {
            loadChildrenIfRequired();
            return super.getChildCount();
        }

        protected void loadChildren() {
            loadedChildren = true;
            loadChildren(this, childValue);
        }

        @Override
        public TreeNode getChildAt(int index) {
            loadChildrenIfRequired();
            return super.getChildAt(index);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Enumeration children() {
            loadChildrenIfRequired();
            return super.children();
        }

        private void loadChildrenIfRequired() {
            if (!loadedChildren) {
                loadChildren();
            }
        }

        private static void loadChildren(DefaultMutableTreeNode node, Object nodeChildren) {
            boolean hasChildren = false;
            if (nodeChildren instanceof Object[]) {
                Object[] children = (Object[]) nodeChildren;
                for (Object element : children) {
                    node.add(new DynamicUtilTreeNode(element, element));
                    hasChildren = true;
                }
            } else if (nodeChildren instanceof Vector) {
                for (Iterator<?> it = ((Vector) nodeChildren).iterator(); it.hasNext();) {
                    Object child = it.next();
                    node.add(new DynamicUtilTreeNode(child, child));
                    hasChildren = true;
                }
            } else if (nodeChildren instanceof Hashtable) {
                for (Iterator<?> it = ((Hashtable) nodeChildren).keySet().iterator(); it
                        .hasNext();) {
                    Object child = it.next();
                    node.add(new DynamicUtilTreeNode(child, child));
                    hasChildren = true;
                }
            }
            if (hasChildren) {
                node.setAllowsChildren(true);
            }
        }
    }

    protected static class EmptySelectionModel extends DefaultTreeSelectionModel {
        private static final long serialVersionUID = -2866787372484669512L;

        protected static final EmptySelectionModel sharedInstance = new EmptySelectionModel();

        public static EmptySelectionModel sharedInstance() {
            return sharedInstance;
        }

        @Override
        public void setSelectionPaths(TreePath[] pPaths) {
        }

        @Override
        public void addSelectionPaths(TreePath[] paths) {
        }

        @Override
        public void removeSelectionPaths(TreePath[] paths) {
        }
    }

    protected class TreeModelHandler implements TreeModelListener {
        public void treeNodesChanged(TreeModelEvent e) {
        }

        public void treeNodesInserted(TreeModelEvent e) {
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            TreePath parentPath = e.getTreePath();
            Object[] children = e.getChildren();
            if (parentPath == null || children == null) {
                return;
            }
            for (Object element : children) {
                TreePath childPath = parentPath.pathByAddingChild(element);
                removeDescendantToggledPaths(getDescendantToggledPaths(childPath));
            }
            removeDescendantSelectedPaths(parentPath, false);
        }

        public void treeStructureChanged(TreeModelEvent e) {
            TreePath parentPath = e.getTreePath();
            if (parentPath == null) {
                return;
            }
            Object parentPathToggleStatus = togglePaths.get(parentPath);
            removeDescendantToggledPaths(getDescendantToggledPaths(parentPath));
            togglePaths.put(parentPath, parentPathToggleStatus);
            removeDescendantSelectedPaths(parentPath, false);
        }
    }

    protected class TreeSelectionRedirector implements Serializable, TreeSelectionListener {
        private static final long serialVersionUID = -5457497600720267892L;

        public void valueChanged(TreeSelectionEvent e) {
            fireValueChanged((TreeSelectionEvent) e.cloneWithSource(JTree.this));
        }
    }

    public static final String CELL_RENDERER_PROPERTY = "cellRenderer";

    public static final String TREE_MODEL_PROPERTY = "model";

    public static final String ROOT_VISIBLE_PROPERTY = "rootVisible";

    public static final String SHOWS_ROOT_HANDLES_PROPERTY = "showsRootHandles";

    public static final String ROW_HEIGHT_PROPERTY = "rowHeight";

    public static final String CELL_EDITOR_PROPERTY = "cellEditor";

    public static final String EDITABLE_PROPERTY = "editable";

    public static final String LARGE_MODEL_PROPERTY = "largeModel";

    public static final String SELECTION_MODEL_PROPERTY = "selectionModel";

    public static final String VISIBLE_ROW_COUNT_PROPERTY = "visibleRowCount";

    public static final String INVOKES_STOP_CELL_EDITING_PROPERTY = "invokesStopCellEditing";

    public static final String SCROLLS_ON_EXPAND_PROPERTY = "scrollsOnExpand";

    public static final String TOGGLE_CLICK_COUNT_PROPERTY = "toggleClickCount";

    public static final String LEAD_SELECTION_PATH_PROPERTY = "leadSelectionPath";

    public static final String ANCHOR_SELECTION_PATH_PROPERTY = "anchorSelectionPath";

    public static final String EXPANDS_SELECTED_PATHS_PROPERTY = "expandsSelectedPaths";

    protected transient TreeModel treeModel;

    protected transient TreeSelectionModel selectionModel;

    protected boolean rootVisible;

    protected transient TreeCellRenderer cellRenderer;

    protected int rowHeight;

    protected boolean showsRootHandles;

    protected transient TreeSelectionRedirector selectionRedirector;

    protected transient TreeCellEditor cellEditor;

    protected boolean editable;

    protected boolean largeModel;

    protected int visibleRowCount;

    protected boolean invokesStopCellEditing;

    protected boolean scrollsOnExpand;

    protected int toggleClickCount;

    protected transient TreeModelListener treeModelListener = createTreeModelListener();

    private boolean expandsSelectedPaths;

    private boolean dragEnabled;

    private TreePath leadSelectionPath;

    private TreePath anchorSelectionPath;

    private final Map<TreePath, Object> togglePaths = new HashMap<TreePath, Object>();

    private static final String UI_CLASS_ID = "TreeUI";

    public JTree() {
        this(getDefaultTreeModel());
    }

    public JTree(Object[] value) {
        this(createTreeModel(value));
        setRootVisible(false);
        setShowsRootHandles(true);
    }

    public JTree(Vector<?> value) {
        this(createTreeModel(value));
        setRootVisible(false);
        setShowsRootHandles(true);
    }

    public JTree(Hashtable<?, ?> value) {
        this(createTreeModel(value));
        setRootVisible(false);
        setShowsRootHandles(true);
    }

    public JTree(TreeNode root) {
        this(new DefaultTreeModel(root));
    }

    public JTree(TreeNode root, boolean asksAllowsChildren) {
        this(new DefaultTreeModel(root));
        ((DefaultTreeModel) getModel()).setAsksAllowsChildren(asksAllowsChildren);
    }

    public JTree(TreeModel model) {
        setModel(model);
        selectionModel = new DefaultTreeSelectionModel();
        updateUI();
    }

    public TreeUI getUI() {
        return (TreeUI) ui;
    }

    public void setUI(TreeUI ui) {
        super.setUI(ui);
    }

    @Override
    public void updateUI() {
        setUI((TreeUI) UIManager.getUI(this));
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public TreeCellRenderer getCellRenderer() {
        return cellRenderer;
    }

    public void setCellRenderer(TreeCellRenderer renderer) {
        TreeCellRenderer oldValue = cellRenderer;
        cellRenderer = renderer;
        firePropertyChange(CELL_RENDERER_PROPERTY, oldValue, renderer);
    }

    public void setEditable(boolean flag) {
        boolean oldValue = editable;
        editable = flag;
        firePropertyChange(EDITABLE_PROPERTY, oldValue, flag);
    }

    public boolean isEditable() {
        return editable;
    }

    public void setCellEditor(TreeCellEditor editor) {
        TreeCellEditor oldValue = cellEditor;
        cellEditor = editor;
        firePropertyChange(CELL_EDITOR_PROPERTY, oldValue, editor);
    }

    public TreeCellEditor getCellEditor() {
        return cellEditor;
    }

    public TreeModel getModel() {
        return treeModel;
    }

    public void setModel(TreeModel model) {
        TreeModel oldValue = treeModel;
        if (treeModel != null) {
            treeModel.removeTreeModelListener(treeModelListener);
        }
        clearToggledPaths();
        if (getSelectionModel() != null) {
            clearSelection();
        }
        treeModel = model;
        if (treeModel != null) {
            treeModel.addTreeModelListener(treeModelListener);
            Object root = treeModel.getRoot();
            if (root != null && !treeModel.isLeaf(root)) {
                togglePaths.put(new TreePath(root), Boolean.TRUE);
            }
        }
        firePropertyChange(TREE_MODEL_PROPERTY, oldValue, model);
    }

    public boolean isRootVisible() {
        return rootVisible;
    }

    public void setRootVisible(boolean visible) {
        boolean oldValue = rootVisible;
        rootVisible = visible;
        firePropertyChange(ROOT_VISIBLE_PROPERTY, oldValue, visible);
    }

    public void setShowsRootHandles(boolean showHandles) {
        boolean oldValue = showsRootHandles;
        showsRootHandles = showHandles;
        firePropertyChange(SHOWS_ROOT_HANDLES_PROPERTY, oldValue, showHandles);
    }

    public boolean getShowsRootHandles() {
        return showsRootHandles;
    }

    public void setRowHeight(int height) {
        int oldValue = rowHeight;
        rowHeight = height;
        firePropertyChange(ROW_HEIGHT_PROPERTY, oldValue, height);
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public boolean isFixedRowHeight() {
        return getRowHeight() > 0;
    }

    public void setLargeModel(boolean large) {
        boolean oldValue = largeModel;
        largeModel = large;
        firePropertyChange(LARGE_MODEL_PROPERTY, oldValue, large);
    }

    public boolean isLargeModel() {
        return largeModel;
    }

    public void setInvokesStopCellEditing(boolean invokesStop) {
        boolean oldValue = invokesStopCellEditing;
        invokesStopCellEditing = invokesStop;
        firePropertyChange(INVOKES_STOP_CELL_EDITING_PROPERTY, oldValue, invokesStop);
    }

    public boolean getInvokesStopCellEditing() {
        return invokesStopCellEditing;
    }

    public void setScrollsOnExpand(boolean scroll) {
        boolean oldValue = scrollsOnExpand;
        scrollsOnExpand = scroll;
        firePropertyChange(SCROLLS_ON_EXPAND_PROPERTY, oldValue, scroll);
    }

    public boolean getScrollsOnExpand() {
        return scrollsOnExpand;
    }

    public void setToggleClickCount(int clickCount) {
        int oldValue = toggleClickCount;
        toggleClickCount = clickCount;
        firePropertyChange(TOGGLE_CLICK_COUNT_PROPERTY, oldValue, toggleClickCount);
    }

    public int getToggleClickCount() {
        return toggleClickCount;
    }

    public void setExpandsSelectedPaths(boolean expand) {
        boolean oldValue = expandsSelectedPaths;
        expandsSelectedPaths = expand;
        firePropertyChange(EXPANDS_SELECTED_PATHS_PROPERTY, oldValue, expand);
    }

    public boolean getExpandsSelectedPaths() {
        return expandsSelectedPaths;
    }

    public void setDragEnabled(boolean enabled) {
        if (enabled && GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        dragEnabled = enabled;
    }

    public boolean getDragEnabled() {
        return dragEnabled;
    }

    public boolean isPathEditable(TreePath path) {
        return isEditable();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (event == null) {
            return null;
        }
        TreePath path = getPathForLocation(event.getX(), event.getY());
        if (path != null) {
            Object node = path.getLastPathComponent();
            boolean isLeaf = getModel().isLeaf(node);
            Component renderer = getCellRenderer().getTreeCellRendererComponent(this, node,
                    isPathSelected(path), isExpanded(path), isLeaf, getRowForPath(path),
                    path.equals(getLeadSelectionPath()));
            if (renderer instanceof JComponent) {
                return ((JComponent) renderer).getToolTipText(SwingUtilities.convertMouseEvent(
                        this, event, renderer));
            }
        }
        return super.getToolTipText(event);
    }

    public String convertValueToText(Object value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
        return value != null ? value.toString() : "";
    }

    public int getRowCount() {
        return getUI().getRowCount(this);
    }

    public void setSelectionPath(TreePath path) {
        getSelectionModel().setSelectionPath(path);
    }

    public void setSelectionPaths(TreePath[] paths) {
        getSelectionModel().setSelectionPaths(paths);
    }

    public void setLeadSelectionPath(TreePath path) {
        TreePath oldValue = leadSelectionPath;
        leadSelectionPath = path;
        firePropertyChange(LEAD_SELECTION_PATH_PROPERTY, oldValue, path);
    }

    public void setAnchorSelectionPath(TreePath path) {
        TreePath oldValue = anchorSelectionPath;
        anchorSelectionPath = path;
        firePropertyChange(ANCHOR_SELECTION_PATH_PROPERTY, oldValue, path);
    }

    public void setSelectionRow(int row) {
        setSelectionRows(new int[] { row });
    }

    public void setSelectionRows(int[] rows) {
        getSelectionModel().setSelectionPaths(rowsToPaths(rows));
    }

    public void addSelectionPath(TreePath path) {
        getSelectionModel().addSelectionPath(path);
    }

    public void addSelectionPaths(TreePath[] paths) {
        getSelectionModel().addSelectionPaths(paths);
    }

    public void addSelectionRow(int row) {
        addSelectionRows(new int[] { row });
    }

    public void addSelectionRows(int[] rows) {
        getSelectionModel().addSelectionPaths(rowsToPaths(rows));
    }

    public Object getLastSelectedPathComponent() {
        TreePath selectionPath = getSelectionPath();
        return selectionPath != null ? selectionPath.getLastPathComponent() : null;
    }

    public TreePath getLeadSelectionPath() {
        return leadSelectionPath;
    }

    public TreePath getAnchorSelectionPath() {
        return anchorSelectionPath;
    }

    public TreePath getSelectionPath() {
        return getSelectionModel().getSelectionPath();
    }

    public TreePath[] getSelectionPaths() {
        return getSelectionModel().getSelectionPaths();
    }

    public int[] getSelectionRows() {
        return getSelectionModel().getSelectionRows();
    }

    public int getSelectionCount() {
        return getSelectionModel().getSelectionCount();
    }

    public int getMinSelectionRow() {
        return getSelectionModel().getMinSelectionRow();
    }

    public int getMaxSelectionRow() {
        return getSelectionModel().getMaxSelectionRow();
    }

    public int getLeadSelectionRow() {
        return getRowForPath(getLeadSelectionPath());
    }

    public boolean isPathSelected(TreePath path) {
        return getSelectionModel().isPathSelected(path);
    }

    public boolean isRowSelected(int row) {
        return getSelectionModel().isRowSelected(row);
    }

    public Enumeration<TreePath> getExpandedDescendants(TreePath parent) {
        final Enumeration<TreePath> toggled = getDescendantToggledPaths(parent);
        
        if (toggled == null || !isExpanded(parent)) {
             return null;
        } 

        return new Enumeration<TreePath>() {
            private TreePath nextElement = getNextExpandedPath();

            public TreePath nextElement() {
                if (nextElement == null) {
                    throw new NoSuchElementException(Messages.getString("swing.4B")); //$NON-NLS-1$
                }
                TreePath currentValue = nextElement;
                nextElement = getNextExpandedPath();
                return currentValue;
            }

            public boolean hasMoreElements() {
                return nextElement != null;
            }

            private TreePath getNextExpandedPath() {
                while (toggled.hasMoreElements()) {
                    TreePath nextPath = toggled.nextElement();                    

                    if (isExpanded(nextPath)) {
                        return nextPath;
                    }
                }
                return null;
            }
        };
    }

    public boolean hasBeenExpanded(TreePath path) {
        return togglePaths.containsKey(path);
    }

    public boolean isExpanded(TreePath path) {
        Boolean state = (Boolean) togglePaths.get(path);
        if (state == null || !state.booleanValue()) {
            return false;
        }
        TreePath parentPath = path.getParentPath();
        return parentPath == null || isExpanded(parentPath);
    }

    public boolean isExpanded(int row) {
        return isExpanded(getPathForRow(row));
    }

    public boolean isCollapsed(TreePath path) {
        return !isExpanded(path);
    }

    public boolean isCollapsed(int row) {
        return !isExpanded(row);
    }

    public void makeVisible(TreePath path) {
        expandPath(path.getParentPath());
    }

    public boolean isVisible(TreePath path) {
        return getRowForPath(path) >= 0;
    }

    public Rectangle getPathBounds(TreePath path) {
        return getUI().getPathBounds(this, path);
    }

    public Rectangle getRowBounds(int row) {
        return getPathBounds(getPathForRow(row));
    }

    public void scrollPathToVisible(TreePath path) {
        if (path == null) {
            return;
        }
        makeVisible(path);
        Rectangle pathBounds = getPathBounds(path);
        if (pathBounds != null) {
            scrollRectToVisible(pathBounds);
        }
    }

    public void scrollRowToVisible(int row) {
        scrollPathToVisible(getPathForRow(row));
    }

    public TreePath getPathForRow(int row) {
        return getUI().getPathForRow(this, row);
    }

    public int getRowForPath(TreePath path) {
        return getUI().getRowForPath(this, path);
    }

    public void expandPath(TreePath path) {
        if (path == null || getModel() == null) {
            return;
        }
        if (getModel().isLeaf(path.getLastPathComponent())) {
            return;
        }
        setExpandedState(path, true);
    }

    public void expandRow(int row) {
        expandPath(getPathForRow(row));
    }

    public void collapsePath(TreePath path) {
        setExpandedState(path, false);
    }

    public void collapseRow(int row) {
        collapsePath(getPathForRow(row));
    }

    public TreePath getPathForLocation(int x, int y) {
        TreePath closestPath = getClosestPathForLocation(x, y);
        if (closestPath == null) {
            return null;
        }
        Rectangle pathBounds = getPathBounds(closestPath);
        return pathBounds.contains(x, y) ? closestPath : null;
    }

    public int getRowForLocation(int x, int y) {
        return getRowForPath(getPathForLocation(x, y));
    }

    public TreePath getClosestPathForLocation(int x, int y) {
        return getUI().getClosestPathForLocation(this, x, y);
    }

    public int getClosestRowForLocation(int x, int y) {
        return getRowForPath(getClosestPathForLocation(x, y));
    }

    public boolean isEditing() {
        return getUI().isEditing(this);
    }

    public boolean stopEditing() {
        return getUI().stopEditing(this);
    }

    public void cancelEditing() {
        getUI().cancelEditing(this);
    }

    public void startEditingAtPath(TreePath path) {
        getUI().startEditingAtPath(this, path);
    }

    public TreePath getEditingPath() {
        return getUI().getEditingPath(this);
    }

    public void setSelectionModel(TreeSelectionModel model) {
        TreeSelectionModel oldValue = selectionModel;
        selectionModel.removeTreeSelectionListener(selectionRedirector);
        selectionModel = model != null ? model : EmptySelectionModel.sharedInstance();
        if (selectionRedirector != null) {
            selectionModel.addTreeSelectionListener(selectionRedirector);
        }
        firePropertyChange(SELECTION_MODEL_PROPERTY, oldValue, selectionModel);
    }

    public TreeSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setSelectionInterval(int index0, int index1) {
        clearSelection();
        addSelectionInterval(index0, index1);
    }

    public void addSelectionInterval(int index0, int index1) {
        addSelectionPaths(getPathBetweenRows(index0, index1));
    }

    public void removeSelectionInterval(int index0, int index1) {
        removeSelectionPaths(getPathBetweenRows(index0, index1));
    }

    public void removeSelectionPath(TreePath path) {
        getSelectionModel().removeSelectionPath(path);
    }

    public void removeSelectionPaths(TreePath[] paths) {
        getSelectionModel().removeSelectionPaths(paths);
    }

    public void removeSelectionRow(int row) {
        removeSelectionPath(getPathForRow(row));
    }

    public void removeSelectionRows(int[] rows) {
        removeSelectionPaths(rowsToPaths(rows));
    }

    public void clearSelection() {
        getSelectionModel().clearSelection();
    }

    public boolean isSelectionEmpty() {
        return getSelectionModel().isSelectionEmpty();
    }

    public void addTreeExpansionListener(TreeExpansionListener l) {
        listenerList.add(TreeExpansionListener.class, l);
    }

    public void removeTreeExpansionListener(TreeExpansionListener l) {
        listenerList.remove(TreeExpansionListener.class, l);
    }

    public TreeExpansionListener[] getTreeExpansionListeners() {
        return listenerList.getListeners(TreeExpansionListener.class);
    }

    public void addTreeWillExpandListener(TreeWillExpandListener l) {
        listenerList.add(TreeWillExpandListener.class, l);
    }

    public void removeTreeWillExpandListener(TreeWillExpandListener l) {
        listenerList.remove(TreeWillExpandListener.class, l);
    }

    public TreeWillExpandListener[] getTreeWillExpandListeners() {
        return listenerList.getListeners(TreeWillExpandListener.class);
    }

    public void fireTreeExpanded(TreePath path) {
        TreeExpansionListener[] listeners = getTreeExpansionListeners();
        if (listeners.length == 0) {
            return;
        }
        TreeExpansionEvent event = new TreeExpansionEvent(this, path);
        for (TreeExpansionListener element : listeners) {
            element.treeExpanded(event);
        }
    }

    public void fireTreeCollapsed(TreePath path) {
        TreeExpansionListener[] listeners = getTreeExpansionListeners();
        if (listeners.length == 0) {
            return;
        }
        TreeExpansionEvent event = new TreeExpansionEvent(this, path);
        for (TreeExpansionListener element : listeners) {
            element.treeCollapsed(event);
        }
    }

    public void fireTreeWillExpand(TreePath path) throws ExpandVetoException {
        TreeWillExpandListener[] listeners = getTreeWillExpandListeners();
        if (listeners.length == 0) {
            return;
        }
        TreeExpansionEvent event = new TreeExpansionEvent(this, path);
        for (TreeWillExpandListener element : listeners) {
            element.treeWillExpand(event);
        }
    }

    public void fireTreeWillCollapse(TreePath path) throws ExpandVetoException {
        TreeWillExpandListener[] listeners = getTreeWillExpandListeners();
        if (listeners.length == 0) {
            return;
        }
        TreeExpansionEvent event = new TreeExpansionEvent(this, path);
        for (TreeWillExpandListener element : listeners) {
            element.treeWillCollapse(event);
        }
    }

    public void addTreeSelectionListener(TreeSelectionListener l) {
        if (selectionRedirector == null) {
            selectionRedirector = new TreeSelectionRedirector();
            selectionModel.addTreeSelectionListener(selectionRedirector);
        }
        listenerList.add(TreeSelectionListener.class, l);
    }

    public void removeTreeSelectionListener(TreeSelectionListener l) {
        listenerList.remove(TreeSelectionListener.class, l);
    }

    public TreeSelectionListener[] getTreeSelectionListeners() {
        return listenerList.getListeners(TreeSelectionListener.class);
    }

    protected void fireValueChanged(TreeSelectionEvent e) {
        TreeSelectionListener[] listeners = getTreeSelectionListeners();
        for (TreeSelectionListener element : listeners) {
            element.valueChanged(e);
        }
    }

    public void treeDidChange() {
        revalidate();
        repaint();
    }

    public void setVisibleRowCount(int count) {
        int oldValue = visibleRowCount;
        visibleRowCount = count;
        firePropertyChange(VISIBLE_ROW_COUNT_PROPERTY, oldValue, count);
    }

    public int getVisibleRowCount() {
        return visibleRowCount;
    }

    public TreePath getNextMatch(String prefix, int startingRow, Position.Bias bias) {
        if (prefix == null) {
            throw new IllegalArgumentException(Messages.getString("swing.4C")); //$NON-NLS-1$
        }
        if (startingRow < 0 || startingRow >= getRowCount()) {
            throw new IllegalArgumentException(Messages.getString("swing.2D")); //$NON-NLS-1$
        }
        if (bias == Position.Bias.Forward) {
            int rowCount = getRowCount();
            for (int i = startingRow; i < rowCount; i++) {
                TreePath path = getPathForRow(i);
                if (pathMatches(prefix, path, i)) {
                    return path;
                }
            }
            for (int i = 0; i < startingRow; i++) {
                TreePath path = getPathForRow(i);
                if (pathMatches(prefix, path, i)) {
                    return path;
                }
            }
        } else {
            for (int i = startingRow; i >= 0; i--) {
                TreePath path = getPathForRow(i);
                if (pathMatches(prefix, path, i)) {
                    return path;
                }
            }
            for (int i = getRowCount() - 1; i > startingRow; i--) {
                TreePath path = getPathForRow(i);
                if (pathMatches(prefix, path, i)) {
                    return path;
                }
            }
        }
        return null;
    }

    public Dimension getPreferredScrollableViewportSize() {
        int width = getPreferredSize().width;
        int height;
        if (isFixedRowHeight()) {
            height = getRowHeight();
        } else {
            Rectangle rootBounds = getModel() != null ? getPathBounds(new TreePath(getModel()
                    .getRoot())) : null;
            height = rootBounds != null ? rootBounds.height : 16;
        }
        return new Dimension(width, getVisibleRowCount() * height);
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return 4;
        }
        TreePath closestPath = getClosestPathForLocation(visibleRect.x, visibleRect.y);
        if (closestPath == null) {
            return 0;
        }
        Rectangle pathBounds = getPathBounds(closestPath);
        if (direction >= 0) {
            return pathBounds.y + pathBounds.height - visibleRect.y;
        }
        int increment = visibleRect.y - pathBounds.y;
        if (increment > 0) {
            return increment;
        }
        int row = getRowForPath(closestPath);
        if (row == 0) {
            return 0;
        }
        pathBounds = getRowBounds(row - 1);
        return pathBounds.height;
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
    }

    public boolean getScrollableTracksViewportWidth() {
        Component parent = getParent();
        if (!(parent instanceof JViewport)) {
            return false;
        }
        return parent.getSize().width > getPreferredSize().width;
    }

    public boolean getScrollableTracksViewportHeight() {
        Component parent = getParent();
        if (!(parent instanceof JViewport)) {
            return false;
        }
        return parent.getSize().height > getPreferredSize().height;
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJTree();
        }
        return accessibleContext;
    }

    protected void setExpandedState(TreePath path, boolean state) {
        doSetExpandedState(path, state);
        if (!state) {
            if (removeDescendantSelectedPaths(path, false)) {
                addSelectionPath(path);
            }
        }
        getSelectionModel().resetRowSelection();
    }

    protected Enumeration<TreePath> getDescendantToggledPaths(final TreePath parent) {
        if (parent == null) {
            return null;
        }
        final Iterator<TreePath> toggled = (new HashSet<TreePath>(togglePaths.keySet()))
                .iterator();
        return new Enumeration<TreePath>() {
            private TreePath nextElement = getNextDescendPath();

            public TreePath nextElement() {
                if (nextElement == null) {
                    throw new NoSuchElementException(Messages.getString("swing.4B")); //$NON-NLS-1$
                }
                TreePath currentValue = nextElement;
                nextElement = getNextDescendPath();
                return currentValue;
            }

            public boolean hasMoreElements() {
                return nextElement != null;
            }

            private TreePath getNextDescendPath() {
                while (toggled.hasNext()) {
                    TreePath nextPath = toggled.next();
                    if (parent.isDescendant(nextPath)) {
                        return nextPath;
                    }
                }
                return null;
            }
        };
    }

    protected void removeDescendantToggledPaths(Enumeration<TreePath> toRemove) {
        if (toRemove == null) {
            return;
        }
        while (toRemove.hasMoreElements()) {
            togglePaths.remove(toRemove.nextElement());
        }
    }

    protected void clearToggledPaths() {
        togglePaths.clear();
    }

    protected TreeModelListener createTreeModelListener() {
        return new TreeModelHandler();
    }

    protected boolean removeDescendantSelectedPaths(TreePath path, boolean includePath) {
        if (path == null) {
            return false;
        }
        TreePath[] selectedPaths = getSelectionPaths();
        if (selectedPaths == null) {
            return false;
        }
        List<TreePath> toRemove = new LinkedList<TreePath>();
        for (TreePath selectedPath : selectedPaths) {
            if (path.isDescendant(selectedPath) && (includePath || !path.equals(selectedPath))) {
                toRemove.add(selectedPath);
            }
        }
        if (toRemove.isEmpty()) {
            return false;
        }
        removeSelectionPaths(toRemove.toArray(new TreePath[toRemove.size()]));
        return true;
    }

    protected static TreeModel getDefaultTreeModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("JTree");
        DefaultMutableTreeNode colorsNode = new DefaultMutableTreeNode("towns");
        colorsNode.add(new DefaultMutableTreeNode("Saint-Petersburg"));
        colorsNode.add(new DefaultMutableTreeNode("New-York"));
        colorsNode.add(new DefaultMutableTreeNode("Munchen"));
        colorsNode.add(new DefaultMutableTreeNode("Oslo"));
        root.add(colorsNode);
        DefaultMutableTreeNode sportsNode = new DefaultMutableTreeNode("animals");
        sportsNode.add(new DefaultMutableTreeNode("dog"));
        sportsNode.add(new DefaultMutableTreeNode("tiger"));
        sportsNode.add(new DefaultMutableTreeNode("wolf"));
        sportsNode.add(new DefaultMutableTreeNode("bear"));
        root.add(sportsNode);
        DefaultMutableTreeNode foodNode = new DefaultMutableTreeNode("computers");
        foodNode.add(new DefaultMutableTreeNode("notebook"));
        foodNode.add(new DefaultMutableTreeNode("desktop"));
        foodNode.add(new DefaultMutableTreeNode("server"));
        foodNode.add(new DefaultMutableTreeNode("mainframe"));
        root.add(foodNode);
        return new DefaultTreeModel(root);
    }

    protected static TreeModel createTreeModel(Object value) {
        return new DefaultTreeModel(new DynamicUtilTreeNode("root", value));
    }

    protected TreePath[] getPathBetweenRows(int index0, int index1) {
        int minRow = Math.max(Math.min(index0, index1), 0);
        int maxRow = Math.min(Math.max(index0, index1), getRowCount() - 1);
        if (minRow > maxRow) {
            return null;
        }
        TreePath[] paths = new TreePath[maxRow - minRow + 1];
        for (int i = minRow; i <= maxRow; i++) {
            paths[i - minRow] = getPathForRow(i);
        }
        return paths;
    }

    private void doSetExpandedState(TreePath path, boolean state) {
        if (path == null) {
            return;
        }
        doSetExpandedState(path.getParentPath(), true);
        if (isExpanded(path) == state) {
            return;
        }
        try {
            if (state) {
                fireTreeWillExpand(path);
            } else {
                fireTreeWillCollapse(path);
            }
        } catch (ExpandVetoException e) {
            return;
        }
        togglePaths.put(path, Boolean.valueOf(state));
        if (state) {
            fireTreeExpanded(path);
        } else {
            fireTreeCollapsed(path);
        }
    }

    private TreePath[] rowsToPaths(int[] rows) {
        if (rows == null || rows.length == 0) {
            return new TreePath[0];
        }
        List<TreePath> paths = new ArrayList<TreePath>();
        for (int row : rows) {
            TreePath path = getPathForRow(row);
            if (path != null) {
                paths.add(path);
            }
        }
        return paths.toArray(new TreePath[paths.size()]);
    }

    private boolean pathMatches(String prefix, TreePath path, int row) {
        if (path == null) {
            return false;
        }
        boolean isLeaf = getModel() != null && getModel().isLeaf(path.getLastPathComponent());
        boolean isFocused = path.equals(getLeadSelectionPath());
        String value = convertValueToText(path.getLastPathComponent(), isPathSelected(path),
                isExpanded(path), isLeaf, row, isFocused);
        return value != null && value.toUpperCase().startsWith(prefix.toUpperCase());
    }
}
