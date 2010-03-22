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

import org.apache.harmony.x.swing.internal.nls.Messages;

public class FixedHeightLayoutCache extends AbstractLayoutCache {
    public FixedHeightLayoutCache() {
        setRowHeight(1);
    }

    public void setRowHeight(final int rowHeight) {
        if (rowHeight <= 0) {
            throw new IllegalArgumentException(Messages.getString("swing.97")); //$NON-NLS-1$
        }
        super.setRowHeight(rowHeight);
    }

    public int getRowCount() {
        return getRowCountImpl();
    }

    public void invalidatePathBounds(final TreePath path) {
    }

    public void invalidateSizes() {
        if (getStateRoot() != null) {
            getStateRoot().invalidateSubtree();
        }
    }

    public boolean isExpanded(final TreePath path) {
        return getExpandedState(path);
    }

    public Rectangle getBounds(final TreePath path, final Rectangle placeIn) {
        if (nodeDimensions == null
            || !isRoot(path) && !isVisible(path)) {

            return null;
        }

        return getFixedHeightBoundsImpl(path, placeIn);
    }

    public TreePath getPathForRow(final int row) {
        if (row < 0 || row >= getRowCount()) {
            return null;
        }

        int currentRow = isRootVisible() ? 0 : -1;
        StateNode parent = getStateRoot();
        while(true) {
            if (currentRow == row) {
                return parent.getModelPath();
            }
            if (parent.isLeaf()) {
                Object modelChildNode = parent.getModelChildNode(row - currentRow - 1);
                return parent.getModelPath().pathByAddingChild(modelChildNode);
            }

            int previousModelIndex = 0;
            currentRow++;
            for (int i = 0; i < parent.getChildCount(); i++) {
                StateNode child = parent.get(i);
                int childModelIndex = parent.getModelIndexOfChild(child.getModelNode());
                int rowIncrement = childModelIndex - previousModelIndex;
                previousModelIndex = childModelIndex;
                if (row < currentRow + rowIncrement) {
                    Object modelChildNode = parent.getModelChildNode(childModelIndex - (currentRow + rowIncrement - row));
                    return parent.getModelPath().pathByAddingChild(modelChildNode);
                }
                if (row == currentRow + rowIncrement) {
                    return child.getModelPath();
                }
                currentRow += rowIncrement;
                if (row > currentRow && row <= currentRow + child.getTotalChildrenCount()) {
                    parent = child;
                    break;
                }

                currentRow += child.getTotalChildrenCount();

                if (i + 1 == parent.getChildCount()) {
                    Object modelChildNode = parent.getModelChildNode(childModelIndex + row - currentRow);
                    return parent.getModelPath().pathByAddingChild(modelChildNode);
                }
            }
        }
    }

    public int getRowForPath(final TreePath path) {
        if (!isVisible(path)) {
            return -1;
        }

        int result = -1;
        StateNode correspondingNode = getStateNodeForPath(path);
        StateNode parent = correspondingNode != null ? correspondingNode.getParent()
                                                     : getStateNodeForPath(path.getParentPath());
        Object modelNode = path.getLastPathComponent();

        while (parent != null) {
            int modelIndex = parent.getModelIndexOfChild(modelNode);
            for (int i = 0; i < parent.getChildCount(); i++) {
                StateNode sibling = parent.get(i);
                int siblingModelIndex = parent.getModelIndexOfChild(sibling.getModelNode());
                if (siblingModelIndex >= modelIndex) {
                    break;
                }
                result += sibling.getTotalChildrenCount();
            }
            result += modelIndex + 1;

            modelNode = parent.getModelNode();
            parent = parent.getParent();
        }

        return isRootVisible() ? result + 1 : result;
    }

    public TreePath getPathClosestTo(final int x, final int y) {
        return getFixedHeightPathClosestToImpl(x, y);
    }

    public int getVisibleChildCount(final TreePath path) {
        return getVisibleChildCountImpl(path);
    }

    public Enumeration<TreePath> getVisiblePathsFrom(final TreePath path) {
        return getVisiblePathsFromImpl(path);
    }

    public void setExpandedState(final TreePath path, final boolean isExpanded) {
        setExpandedStateImpl(path, isExpanded);
    }

    public boolean getExpandedState(final TreePath path) {
        return getExpandedStateImpl(path);
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


    class FixedHeightStateNode extends StateNode {
        public FixedHeightStateNode(final StateNode parent, final TreePath path) {
            super(parent, path);
        }

        public void invalidateTreePartBelow() {
            invalidate();
        }

        protected void validateData() {
            super.validateData();
            if (!isExpanded()) {
                totalChildrenCount = 0;
                return;
            }

            totalChildrenCount = getModelChildCount();
            for (int i = 0; i < getChildCount(); i++) {
                StateNode child = get(i);
                totalChildrenCount += child.getTotalChildrenCount();
            }
        }
    }

    StateNode createStateNode(final StateNode parent, final TreePath path) {
        return new FixedHeightStateNode(parent, path);
    }
}
