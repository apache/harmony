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
package javax.swing.tree;

import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.event.TreeModelEvent;

public abstract class AbstractLayoutCache implements RowMapper {
    public abstract static class NodeDimensions {
        public abstract Rectangle getNodeDimensions(Object value,
                                                    int row,
                                                    int depth,
                                                    boolean expanded,
                                                    Rectangle bounds);
    }

    class StateNode {
        private final StateNode parent;
        private final TreePath path;
        private final List children = new LinkedList();
        private boolean expanded = true;
        protected int totalChildrenCount;
        private Object[] modelChildren;

        private boolean isValid;

        public StateNode(final StateNode parent, final TreePath path) {
            this.parent = parent;
            this.path = path;
        }

        public boolean isRoot() {
            return getParent() == null;
        }

        public void add(final StateNode child, final int index) {
            synchronized (AbstractLayoutCache.this) {
                children.add(index, child);
                child.invalidateTreePartBelow();
            }
        }

        public void add(final StateNode child) {
            synchronized (AbstractLayoutCache.this) {
                add(child, children.size());
                child.invalidateTreePartBelow();
            }
        }

        public void remove(final int index) {
            synchronized (AbstractLayoutCache.this) {
                children.remove(index);
                invalidateTreePartBelow();
            }
        }

        public void remove(final StateNode child) {
            synchronized (AbstractLayoutCache.this) {
                children.remove(child);
                invalidateTreePartBelow();
            }
        }

        public void removeAll() {
            synchronized (AbstractLayoutCache.this) {
                children.clear();
                invalidateTreePartBelow();
            }
        }

        public StateNode getParent() {
            return parent;
        }

        public Object getModelNode() {
            return path.getLastPathComponent();
        }

        public TreePath getModelPath() {
            return path;
        }

        public Object getModelChildNode(final int index) {
            return getModelChildren()[index];
        }

        public int getModelChildCount() {
            return getModelChildren().length;
        }

        public int getModelIndexOfChild(final Object modelChild) {
            if (modelChild == null) {
                return -1;
            }

            Object[] mChildren = getModelChildren();
            for (int i = 0; i < mChildren.length; i++) {
                if (modelChild.equals(mChildren[i])) {
                    return i;
                }
            }
            return -1;
        }

        public Object[] getModelChildren() {
            if (modelChildren == null) {
                int childCount = getModel().getChildCount(getModelNode());
                modelChildren = new Object[childCount];
                for (int i = 0; i < childCount; i++) {
                    modelChildren[i] = getModel().getChild(getModelNode(), i);
                }
            }
            return modelChildren;
        }

        public int getChildCount() {
            synchronized (AbstractLayoutCache.this) {
                return unsyncGetChildCount();
            }
        }

        public boolean isLeaf() {
            synchronized (AbstractLayoutCache.this) {
                return unsyncIsLeaf();
            }
        }

        public void setExpanded() {
            expanded = true;
            invalidateTreePartBelow();
        }

        public void setCollapsed() {
            expanded = false;
            invalidateTreePartBelow();
        }

        public boolean isExpanded() {
            return expanded;
        }

        public List children() {
            synchronized (AbstractLayoutCache.this) {
                return children;
            }
        }

        public StateNode get(final int index) {
            synchronized (AbstractLayoutCache.this) {
                return unsyncGet(index);
            }
        }

        public StateNode getChild(final Object modelNode) {
            synchronized (AbstractLayoutCache.this) {
                for (Iterator it = children.iterator(); it.hasNext();) {
                    StateNode stateNode = (StateNode)it.next();
                    if (stateNode.getModelNode().equals(modelNode)) {
                        return stateNode;
                    }
                }

                return null;
            }
        }

        public StateNode addChild(final Object modelChildNode) {
            synchronized (AbstractLayoutCache.this) {
                int childModelIndex = getModelIndexOfChild(modelChildNode);
                int insertionIndex;
                for (insertionIndex = 0; insertionIndex < children.size(); insertionIndex++) {
                    StateNode childStateNode = (StateNode)children.get(insertionIndex);
                    int childModelIndexForStateNode = getModelIndexOfChild(childStateNode.getModelNode());
                    if (childModelIndexForStateNode == childModelIndex) {
                        return childStateNode;
                    }
                    if (childModelIndexForStateNode > childModelIndex) {
                        break;
                    }
                }
                StateNode newChildStateNode = createStateNode(this, getModelPath().pathByAddingChild(modelChildNode));
                add(newChildStateNode, insertionIndex);

                return newChildStateNode;
            }
        }

        public StateNode getNextSibling() {
            synchronized (AbstractLayoutCache.this) {
                if (getParent() == null) {
                    return null;
                }
                int index = getParent().children().indexOf(this);
                return index + 1 < getParent().getChildCount() ? getParent().get(index + 1)
                        : null;
            }
        }

        public int getTotalChildrenCount() {
            validate();
            return totalChildrenCount;
        }

        public boolean isValid() {
            return isValid;
        }

        public void validate() {
            synchronized (AbstractLayoutCache.this) {
                if (isValid) {
                    return;
                }

                // validation should go hierarchically, from the top to leaves
                // therefore a parent shoul dbe validated first
                if (getParent() != null) {
                    getParent().validate();
                }

                // validation of the parent can cause children validation.
                // In this case we don't need to validate this children again
                if (isValid) {
                    return;
                }

                // important to be BEFORE validateData() to get rid of potential cycling
                isValid = true;
                validateData();
            }
        }

        public String toString() {
            return getModelNode().toString();
        }

        public void invalidate() {
            synchronized (AbstractLayoutCache.this) {
                isValid = false;
                resetCachedData();
                if (parent != null) {
                    parent.invalidate();
                }
            }
        }

        public void invalidateSubtree() {
            invalidate();
            synchronized (AbstractLayoutCache.this) {
                for (int i = 0; i < unsyncGetChildCount(); i++) {
                    unsyncInvalidateSubtree(unsyncGet(i));
                }
            }
        }
        
        public void invalidateTreePartBelow() {
            synchronized (AbstractLayoutCache.this) {
                unsyncInvalidateTreePartBelow(this);
            }
        }

        protected void validateData() {
        }

        private void unsyncInvalidateTreePartBelow(final StateNode node) {
            node.invalidate();
            
            if (!node.unsyncIsLeaf()) {
                unsyncInvalidateTreePartBelow(node.unsyncGet(0));
            } else {
                StateNode currentNode = node;
                while(currentNode != null) {
                    StateNode sibling = currentNode.getNextSibling();
                    if (sibling != null) {
                        unsyncInvalidateTreePartBelow(sibling);
                        break;
                    }
                    currentNode = currentNode.getParent();
                }
            }
        }

        private boolean unsyncIsLeaf() {
            return children.size() == 0;
        }

        private void resetCachedData() {
            modelChildren = null;
        }

        private void unsyncInvalidateSubtree(final StateNode root) {
            root.invalidate();
            for (int i = 0; i < root.unsyncGetChildCount(); i++) {
                unsyncInvalidateSubtree(root.unsyncGet(i));
            }
        }

        private StateNode unsyncGet(final int index) {
            return (StateNode)children.get(index);
        }

        private int unsyncGetChildCount() {
            return children.size();
        }
    }

    protected NodeDimensions nodeDimensions;
    protected TreeModel treeModel;
    protected TreeSelectionModel treeSelectionModel;
    protected boolean rootVisible;
    protected int rowHeight;

    private StateNode stateRoot;

    public abstract boolean isExpanded(final TreePath path);
    public abstract Rectangle getBounds(TreePath path, Rectangle placeIn);
    public abstract TreePath getPathForRow(int row);
    public abstract int getRowForPath(TreePath path);
    public abstract TreePath getPathClosestTo(int x, int y);
    public abstract Enumeration<TreePath> getVisiblePathsFrom(TreePath path);
    public abstract int getVisibleChildCount(TreePath path);
    public abstract void setExpandedState(TreePath path, boolean isExpanded);
    public abstract boolean getExpandedState(TreePath path);
    public abstract int getRowCount();
    public abstract void invalidateSizes();
    public abstract void invalidatePathBounds(TreePath path);
    public abstract void treeNodesChanged(TreeModelEvent e);
    public abstract void treeNodesInserted(TreeModelEvent e);
    public abstract void treeNodesRemoved(TreeModelEvent e);
    public abstract void treeStructureChanged(TreeModelEvent e);


    public void setNodeDimensions(final NodeDimensions nd) {
        nodeDimensions = nd;
    }

    public NodeDimensions getNodeDimensions() {
        return nodeDimensions;
    }

    public void setModel(final TreeModel model) {
        treeModel = model;
        resetRoot(model);
    }

    public TreeModel getModel() {
        return treeModel;
    }

    public void setRootVisible(final boolean rootVisible) {
        this.rootVisible = rootVisible;
    }

    public boolean isRootVisible() {
        return rootVisible;
    }

    public void setRowHeight(final int rowHeight) {
        this.rowHeight = rowHeight;
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public void setSelectionModel(final TreeSelectionModel selectionModel) {
        if (treeSelectionModel != null) {
            treeSelectionModel.setRowMapper(null);
        }
        treeSelectionModel = selectionModel;
        if (treeSelectionModel != null) {
            treeSelectionModel.setRowMapper(this);
        }
    }

    public TreeSelectionModel getSelectionModel() {
        return treeSelectionModel;
    }

    public int getPreferredHeight() {
        return getRowCount() * rowHeight;
    }

    public int getPreferredWidth(final Rectangle bounds) {
        if (getNodeDimensions() == null || getStateRoot() == null) {
            return 0;
        }

        TreePath startPath;
        TreePath endPath;
        if (bounds != null) {
            startPath = getPathClosestTo(bounds.x, bounds.y);
            endPath = getPathClosestTo(bounds.x, bounds.y + bounds.height - 1);
        } else {
            startPath = getStateRoot().getModelPath();
            endPath = null;
        }

        Enumeration paths = getVisiblePathsFromImpl(startPath);
        if (paths == null) {
            return 0;
        }

        int result = 0;
        while(paths.hasMoreElements()) {
            TreePath path = (TreePath)paths.nextElement();

            if (isRoot(path) && !isRootVisible()) {
                continue;
            }

            Rectangle pathBounds = getBounds(path, null);
            if (pathBounds == null) {
                continue;
            }

            int width = pathBounds.x + pathBounds.width;
            if (result < width) {
                result = width;
            }

            if (path.equals(endPath)) {
                break;
            }
        }

        return result;
    }

    public int[] getRowsForPaths(final TreePath[] paths) {
        if (paths == null) {
            return new int[0];
        }

        int result[] = new int[paths.length];
        for (int i = 0; i < paths.length; i++) {
            result[i] = getRowForPath(paths[i]);
        }

        return result;
    }

    protected Rectangle getNodeDimensions(final Object value,
                                          final int row,
                                          final int depth,
                                          final boolean expanded,
                                          final Rectangle placeIn) {

        return getNodeDimensions() != null ? getNodeDimensions().getNodeDimensions(value, row, depth, expanded, placeIn)
                                           : null;
    }

    protected boolean isFixedRowHeight() {
        return getRowHeight() > 0;
    }



    boolean isVisible(final TreePath path) {
        if (!isModelPath(path)) {
            return false;
        }
        if (isRoot(path)) {
            return isRootVisible();
        }

        if (!stateRoot.isExpanded()) {
            return false;
        }
        StateNode nextNode = stateRoot;
        for (int i = 1; i < path.getPathCount() - 1; i++) {
            Object pathComponent = path.getPathComponent(i);
            nextNode = nextNode.getChild(pathComponent);
            if (nextNode == null || !nextNode.isExpanded()) {
                return false;
            }
        }

        return nextNode.getModelIndexOfChild(path.getLastPathComponent()) != -1;
    }

    StateNode getStateRoot() {
        return stateRoot;
    }

    boolean isRoot(final TreePath path) {
        return getStateRoot() != null && path != null && path.equals(getStateRoot().getModelPath());
    }

    StateNode getStateNodeForPath(final TreePath path) {
        if (stateRoot == null || !isModelPath(path)) {
            return null;
        }

        StateNode result = stateRoot;
        for (int i = 1; i < path.getPathCount(); i++) {
            Object pathComponent = path.getPathComponent(i);
            result = result.getChild(pathComponent);
            if (result == null) {
                return null;
            }
        }

        return result;
    }

    int getVisibleChildCountImpl(final TreePath path) {
        StateNode correspondingNode = getVisibleStateNodeForPath(path);
        return correspondingNode != null ? correspondingNode.getTotalChildrenCount() : 0;
    }

    Enumeration<TreePath> getVisiblePathsFromImpl(final TreePath path) {
        if (!isModelPath(path) || !isRoot(path) && !isVisible(path)) {
            return null;
        }

        return new Enumeration<TreePath>() {
            private TreePath currentPath = path;

            public TreePath nextElement() {
                if (currentPath == null) {
                    throw new NoSuchElementException();
                }

                TreePath result = currentPath;

                TreePath childPath = getFirstChildPath(currentPath);
                if (childPath != null) {
                    currentPath = childPath;
                } else {
                    currentPath = getNextSiblingPathInHierarchy(currentPath);
                }

                return result;
            }

            public boolean hasMoreElements() {
                return currentPath != null;
            }

            private TreePath getFirstChildPath(final TreePath path) {
                if (!isExpanded(path)) {
                    return null;
                }

                StateNode node = getStateNodeForPath(path);
                if (node.getModelChildCount() == 0) {
                    return null;
                }

                Object child = node.getModelChildNode(0);
                return path.pathByAddingChild(child);
            }

            private TreePath getNextSiblingPathInHierarchy(final TreePath path) {
                TreePath currentPath = path;
                while(true) {
                    TreePath siblingPath = getNextSiblingPath(currentPath);
                    if (siblingPath != null) {
                        return siblingPath;
                    }
                    currentPath = currentPath.getParentPath();
                    if (currentPath == null) {
                        return null;
                    }
                }
            }

            private TreePath getNextSiblingPath(final TreePath path) {
                TreePath parentPath = path.getParentPath();
                if (parentPath == null) {
                    return null;
                }
                StateNode parentNode = getStateNodeForPath(parentPath);
                int curIndex = parentNode.getModelIndexOfChild(path.getLastPathComponent());
                int childCount = parentNode.getModelChildCount();
                if (curIndex + 1 < childCount) {
                    Object sibling = parentNode.getModelChildNode(curIndex + 1);
                    return parentPath.pathByAddingChild(sibling);
                }
                return null;
            }
        };
    }

    void setExpandedStateImpl(final TreePath path, final boolean isExpanded) {
        if (isExpanded) {
            expandPath(path);
        } else {
            collapsePath(path);
        }
    }

    boolean getExpandedStateImpl(final TreePath path) {
        StateNode node = getVisibleStateNodeForPath(path);
        return node != null && node.isExpanded();
    }

    int getRowCountImpl() {
        return getStateRoot() != null ? getStateRoot().getTotalChildrenCount()
                                        + (isRootVisible() ? 1 : 0)
                                      : 0;
    }

    Rectangle getFixedHeightBoundsImpl(final TreePath path, final Rectangle placeIn) {
        int row = getRowForPath(path);

        Rectangle result;
        if (nodeDimensions != null) {
            StateNode node = getStateNodeForPath(path);
            result = getNodeDimensions(path.getLastPathComponent(), row,
                                       getPathDepth(path), node != null && node.isExpanded(), placeIn);
        } else {
            result = placeIn != null ? placeIn : new Rectangle();
        }
        result.y = row * rowHeight;
        result.height = rowHeight;

        return result;
    }

    TreePath getFixedHeightPathClosestToImpl(final int x, final int y) {
        int rowCount = getRowCount();
        if (rowCount == 0) {
            return null;
        }

        int row = Math.round(y / rowHeight);
        if (row < 0) {
            row = 0;
        } else if (row >= rowCount) {
            row = rowCount - 1;
        }

        return getPathForRow(row);
    }

    int getPathDepth(final TreePath path) {
        return path.getPathCount() - 1;
    }

    void treeNodesChangedImpl(final TreeModelEvent e) {
        StateNode changeRootNode = getStateNodeForPath(e.getTreePath());
        if (changeRootNode == null) {
            changeRootNode = getStateNodeForPath(e.getTreePath().getParentPath());
        }
        if (changeRootNode == null) {
            return;
        }
        changeRootNode.invalidate();
    }

    void treeNodesInsertedImpl(final TreeModelEvent e) {
        StateNode insertRootNode = getStateNodeForPath(e.getTreePath());
        if (insertRootNode != null) {
            insertRootNode.invalidateTreePartBelow();
            resetRowSelection();
        }
    }

    void treeNodesRemovedImpl(final TreeModelEvent e) {
        StateNode removeRootNode = getStateNodeForPath(e.getTreePath());
        if (removeRootNode == null) {
            removeRootNode = getStateNodeForPath(e.getTreePath().getParentPath());
            if (removeRootNode == null) {
                return;
            }
            removeRootNode.invalidateTreePartBelow();
            resetRowSelection();
            return;
        }

        for (int i = 0; i < e.getChildren().length; i++) {
            StateNode node = removeRootNode.getChild(e.getChildren()[i]);
            if (node != null) {
                removeRootNode.remove(node);
            }
        }
        removeRootNode.invalidateTreePartBelow();
        resetRowSelection();
    }

    void treeStructureChangedImpl(final TreeModelEvent e) {
        if (stateRoot.getModelNode() != treeModel.getRoot()) {
            resetRoot(treeModel);
        }
        TreePath path = e.getTreePath();
        StateNode node = getStateNodeForPath(path);
        if (node == null) {
            return;
        }
        node.removeAll();
        resetRowSelection();
    }

    StateNode createStateNode(final StateNode parent, final TreePath path) {
        return new StateNode(parent, path);
    }


    private void expandPath(final TreePath path) {
        if (!isModelPath(path)) {
            return;
        }

        StateNode nextNode = stateRoot;
        stateRoot.setExpanded();
        for (int i = 1; i < path.getPathCount(); i++) {
            Object pathComponent = path.getPathComponent(i);
            if (!getModel().isLeaf(pathComponent)) {
                nextNode = nextNode.addChild(pathComponent);
                nextNode.setExpanded();
            }
        }
    }

    private void collapsePath(final TreePath path) {
        StateNode stateNode = getStateNodeForPath(path);
        if (stateNode == null) {
            return;
        }

        if (stateNode.isLeaf()
            && !stateNode.isRoot()) {

            stateNode.getParent().remove(stateNode);
            return;
        }
        stateNode.setCollapsed();
    }

    private StateNode getVisibleStateNodeForPath(final TreePath path) {
        if (stateRoot == null || !isModelPath(path)) {
            return null;
        }

        StateNode result = stateRoot;
        for (int i = 1; i < path.getPathCount(); i++) {
            if (!result.isExpanded()) {
                return null;
            }
            Object pathComponent = path.getPathComponent(i);
            result = result.getChild(pathComponent);
            if (result == null) {
                return null;
            }
        }

        return result;
    }

    private boolean isModelPath(final TreePath path) {
        return getStateRoot() != null && path != null && getStateRoot().getModelPath().isDescendant(path);
    }

    private void resetRowSelection() {
        if (getSelectionModel() != null) {
            getSelectionModel().resetRowSelection();
        }
    }

    private void resetRoot(final TreeModel model) {
        if (model != null && model.getRoot() != null) {
            stateRoot = createStateNode(null, new TreePath(model.getRoot()));
            if (model.isLeaf(model.getRoot())) {
                stateRoot.setCollapsed();
            }
        } else {
            stateRoot = null;
        }
    }
}
