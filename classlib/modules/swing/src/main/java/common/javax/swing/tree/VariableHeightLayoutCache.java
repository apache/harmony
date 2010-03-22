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

import javax.swing.event.TreeModelEvent;

public class VariableHeightLayoutCache extends AbstractLayoutCache {
    public void setRootVisible(final boolean rootVisible) {
        this.rootVisible = rootVisible;
        if (getStateRoot() != null) {
            getStateRoot().invalidateSubtree();
        }
    }

    public void setRowHeight(final int rowHeight) {
        super.setRowHeight(rowHeight);
        if (getStateRoot() != null) {
            getStateRoot().invalidateSubtree();
        }
    }

    public void setNodeDimensions(final NodeDimensions nd) {
        super.setNodeDimensions(nd);
        if (getStateRoot() != null) {
            getStateRoot().invalidateSubtree();
        }
    }

    public void setExpandedState(final TreePath path, final boolean isExpanded) {
        setExpandedStateImpl(path, isExpanded);
    }

    public boolean getExpandedState(final TreePath path) {
        return getExpandedStateImpl(path);
    }

    public Rectangle getBounds(final TreePath path, final Rectangle placeIn) {
        if (!isRoot(path) && !isVisible(path)) {
            return null;
        }

        if (isFixedRowHeight()) {
            return getFixedHeightBoundsImpl(path, placeIn);
        }

        if (getNodeDimensions() == null) {
            return new Rectangle();
        }


        if (isRoot(path)) {
            Rectangle result = getNodeDimensions(getStateRoot().getModelNode(), isRootVisible() ? 0 : -1,
                                                 0, getStateRoot().isExpanded(), placeIn);
            result.y = 0;
            return result;
        }


        VariableHeightStateNode parentNode = (VariableHeightStateNode)getStateNodeForPath(path.getParentPath());
        StateNode node = parentNode.getChild(path.getLastPathComponent());
        Rectangle result = getNodeDimensions(path.getLastPathComponent(), getRowForPath(path),
                                             getPathDepth(path), node != null && node.isExpanded(), placeIn);
        result.y = 0;

        Object currentModelNode = path.getLastPathComponent();
        while (parentNode != null) {
            if (!parentNode.isRoot() || isRootVisible()) {
                result.y += parentNode.getHeight();
            }
            if (parentNode.isExpanded()) {
                int modelIndex = parentNode.getModelIndexOfChild(currentModelNode);
                for (int i = 0; i < modelIndex; i++) {
                    result.y += parentNode.getChildrenHeights()[i];
                }
            }
            currentModelNode = parentNode.getModelNode();
            parentNode = (VariableHeightStateNode)parentNode.getParent();
        }

        return result;
    }

    public TreePath getPathForRow(final int row) {
        if (row < 0 || row >= getRowCount()) {
            return null;
        }

        if (isRootVisible() && row == 0) {
            return getStateRoot().getModelPath();
        }

        VariableHeightStateNode parent = (VariableHeightStateNode)getStateRoot();

        while(true) {
            if (parent.getChildCount() == 0) {
                int modelIndex = row - parent.getRow() - 1;
                Object modelChild = parent.getModelChildNode(modelIndex);
                return parent.getModelPath().pathByAddingChild(modelChild);
            }

            for (int i = 0; i < parent.getChildCount(); i++) {
                VariableHeightStateNode childNode = (VariableHeightStateNode)parent.get(i);
                if (childNode.getRow() == row) {
                    return childNode.getModelPath();
                }
                if (childNode.getRow() > row) {
                    int modelChildIndex = parent.getModelIndexOfChild(childNode.getModelNode());
                    int modelIndex = modelChildIndex - (childNode.getRow() - row);
                    Object modelChild = parent.getModelChildNode(modelIndex);
                    return parent.getModelPath().pathByAddingChild(modelChild);
                }
                if (childNode.getRow() < row && childNode.getRow() + childNode.getTotalChildrenCount() >= row) {
                    parent = childNode;
                    break;
                }
                if (i == parent.getChildCount() - 1) {
                    int modelChildIndex = parent.getModelIndexOfChild(childNode.getModelNode());
                    int modelIndex = modelChildIndex + row - (childNode.getRow() + childNode.getTotalChildrenCount());
                    Object modelChild = parent.getModelChildNode(modelIndex);
                    return parent.getModelPath().pathByAddingChild(modelChild);
                }
            }
        }
    }

    public int getRowForPath(final TreePath path) {
        if (!isVisible(path)) {
            return -1;
        }

        VariableHeightStateNode correspondingNode = (VariableHeightStateNode)getStateNodeForPath(path);
        if (correspondingNode != null) {
            return correspondingNode.getRow();
        }

        VariableHeightStateNode parent = (VariableHeightStateNode)getStateNodeForPath(path.getParentPath());

        int modelIndex = parent.getModelIndexOfChild(path.getLastPathComponent());
        int rowIncrement = 0;
        for (int i = 0; i < modelIndex; i++) {
            rowIncrement++;

            Object modelNode = parent.getModelChildNode(i);
            StateNode childNode = parent.getChild(modelNode);
            if (childNode != null) {
                rowIncrement += childNode.getTotalChildrenCount();
            }
        }
        return parent.getRow() + rowIncrement + 1;
    }

    public int getRowCount() {
        return getRowCountImpl();
    }

    public void invalidatePathBounds(final TreePath path) {
        StateNode node = getStateNodeForPath(path);
        if (node == null) {
            node = getStateNodeForPath(path.getParentPath());
        }

        if (node != null) {
            node.invalidateTreePartBelow();
        }
    }

    public int getPreferredHeight() {
        if (getStateRoot() == null) {
            return 0;
        }

        if (isFixedRowHeight()) {
            return getRowCount() * getRowHeight();
        }

        VariableHeightStateNode root = (VariableHeightStateNode)getStateRoot();
        return root.getTotalChildrenHeight()
               + (isRootVisible() ? root.getHeight() : 0);
    }

    public int getPreferredWidth(final Rectangle bounds) {
        return super.getPreferredWidth(bounds);
    }

    public TreePath getPathClosestTo(final int x, final int y) {
        if (getStateRoot() == null) {
            return null;
        }

        if (isFixedRowHeight()) {
            return getFixedHeightPathClosestToImpl(x, y);
        }

        if (!getStateRoot().isExpanded()) {
            return isRootVisible() ? getStateRoot().getModelPath() : null;
        }

        int cummulativeHeight = 0;
        VariableHeightStateNode currentParent = (VariableHeightStateNode)getStateRoot();
        if (isRootVisible()) {
            if (currentParent.getHeight() > y) {
                return currentParent.getModelPath();
            }
            cummulativeHeight += currentParent.getHeight();
        }

        while (true) {
            int modelChildCount = currentParent.getModelChildCount();
            if (modelChildCount == 0) {
                return currentParent.getModelPath();
            }
            for (int i = 0; i < modelChildCount; i++) {
                if (cummulativeHeight + currentParent.getChildrenHeights()[i] > y || i + 1 == modelChildCount) {
                    Object modelChild = currentParent.getModelChildNode(i);
                    VariableHeightStateNode childNode = (VariableHeightStateNode)currentParent.getChild(modelChild);
                    if (childNode != null) {
                        currentParent = childNode;
                        if (cummulativeHeight + currentParent.getHeight() > y
                            || !currentParent.isExpanded()) {

                            return currentParent.getModelPath();
                        }
                        cummulativeHeight += currentParent.getHeight();
                        break;
                    }
                    return currentParent.getModelPath().pathByAddingChild(modelChild);
                }
                cummulativeHeight += currentParent.getChildrenHeights()[i];
            }
        }
    }

    public Enumeration<TreePath> getVisiblePathsFrom(final TreePath path) {
        return getVisiblePathsFromImpl(path);
    }

    public int getVisibleChildCount(final TreePath path) {
        return getVisibleChildCountImpl(path);
    }

    public void invalidateSizes() {
        if (getStateRoot() != null) {
            getStateRoot().invalidateSubtree();
        }
    }

    public boolean isExpanded(final TreePath path) {
        return getExpandedState(path);
    }

    public void treeNodesChanged(final TreeModelEvent e) {
        treeNodesChangedImpl(e);
    }

    public void treeNodesInserted(final TreeModelEvent e) {
        treeNodesInsertedImpl(e);
    }

    public void treeNodesRemoved(final TreeModelEvent e) {
        treeNodesRemovedImpl(e);
    }

    public void treeStructureChanged(final TreeModelEvent e) {
        treeStructureChangedImpl(e);
    }


    class VariableHeightStateNode extends StateNode {
        private int totalChildrenHeight;
        private int height;
        private int[] childrenHeights;

        private int row;

        public VariableHeightStateNode(final StateNode parent, final TreePath path) {
            super(parent, path);
        }

        public int getTotalChildrenHeight() {
            validate();
            return totalChildrenHeight;
        }

        public int[] getChildrenHeights() {
            validate();
            return childrenHeights;
        }

        public int getHeight() {
            validate();
            return height;
        }

        public int getRow() {
            validate();
            return row;
        }

        protected void validateData() {
            super.validateData();
            if (getParent() == null) {
                row = isRootVisible() ? 0 : -1;
            }
            Rectangle bounds = getNodeDimensions(getModelNode(), row, getPathDepth(getModelPath()), isExpanded(), null);
            height = bounds != null ? bounds.height : 0;

            if (!isExpanded()) {
                totalChildrenCount = 0;
                totalChildrenHeight = 0;

                if (childrenHeights == null || childrenHeights.length != 0) {
                    childrenHeights = new int[0];
                }
                return;
            }

            int modelChildCount = getModelChildCount();
            int childRow = row;

            totalChildrenCount = 0;
            totalChildrenHeight = 0;

            if (childrenHeights == null || childrenHeights.length != modelChildCount) {
                childrenHeights = new int[modelChildCount];
            }
            for (int i = 0; i < modelChildCount; i++) {
                Object modelChild = getModelChildNode(i);
                VariableHeightStateNode stateChild = (VariableHeightStateNode)getChild(modelChild);
                childRow++;
                if (stateChild != null) {
                    stateChild.row = childRow;
                    childrenHeights[i] = stateChild.getHeight() + stateChild.getTotalChildrenHeight();
                    totalChildrenCount += stateChild.getTotalChildrenCount();

                    childRow += stateChild.getTotalChildrenCount();
                } else {
                    bounds = getNodeDimensions(modelChild, childRow, getPathDepth(getModelPath()) + 1, false, bounds);
                    childrenHeights[i] = bounds != null ? bounds.height : 0;
                }
                totalChildrenCount++;
                totalChildrenHeight += childrenHeights[i];
            }
        }
    }

    StateNode createStateNode(final StateNode parent, final TreePath path) {
        return new VariableHeightStateNode(parent, path);
    }
}
