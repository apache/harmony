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
* @author Alexander T. Simbirtsev
*/
package javax.swing.tree;

import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import org.apache.harmony.x.swing.TreeCommons;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class DefaultTreeModel implements TreeModel, Serializable {

    protected boolean asksAllowsChildren;
    protected EventListenerList listenerList = new EventListenerList();
    protected TreeNode root;

    public DefaultTreeModel(final TreeNode root) {
        this.root = root;
    }

    public DefaultTreeModel(final TreeNode root,
                            final boolean asksAllowsChildren) {
        this.root = root;
        this.asksAllowsChildren = asksAllowsChildren;
    }

    public void setAsksAllowsChildren(final boolean asksAllowsChildren) {
        this.asksAllowsChildren = asksAllowsChildren;
    }

    public boolean asksAllowsChildren() {
        return asksAllowsChildren;
    }

    public void addTreeModelListener(final TreeModelListener l) {
        listenerList.add(TreeModelListener.class, l);
    }

    public void removeTreeModelListener(final TreeModelListener l) {
        listenerList.remove(TreeModelListener.class, l);
    }

    public TreeModelListener[] getTreeModelListeners() {
        return (TreeModelListener[])listenerList.getListeners(TreeModelListener.class);
    }

    public <T extends EventListener> T[] getListeners(final Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    public void setRoot(final TreeNode root) {
        this.root = root;
        if (root != null) {
            nodeStructureChanged(root);
        } else {
            fireRootChangedToNull(this);
        }
    }

    public Object getRoot() {
        return root;
    }

    public Object getChild(final Object parent, final int i) {
        return ((TreeNode)parent).getChildAt(i);
    }

    public int getChildCount(final Object node) {
        return ((TreeNode)node).getChildCount();
    }

    public int getIndexOfChild(final Object parent, final Object child) {
        if (parent == null || child == null) {
            return -1;
        }

        TreeNode parentNode = (TreeNode)parent;
        int numChildren = parentNode.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            if (child.equals(parentNode.getChildAt(i))) {
                return i;
            }
        }

        return -1;
    }

    public boolean isLeaf(final Object node) {
        return asksAllowsChildren ? !((TreeNode)node).getAllowsChildren() :
                                    (((TreeNode)node).getChildCount() == 0);
    }

    public void valueForPathChanged(final TreePath path, final Object value) {
        MutableTreeNode node = (MutableTreeNode)path.getLastPathComponent();
        node.setUserObject(value);
        nodeChanged(node);
    }

    public void reload() {
        reload(root);
    }

    public void reload(final TreeNode node) {
        nodeStructureChanged(node);
    }

    public void insertNodeInto(final MutableTreeNode newChild,
                               final MutableTreeNode parent,
                               final int index) {

        if (newChild == null) {
            throw new IllegalArgumentException(Messages.getString("swing.03","new child")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        parent.insert(newChild, index);
        nodesWereInserted(parent,  new int[] {index});
    }

    public void removeNodeFromParent(final MutableTreeNode node) {
        MutableTreeNode parent = (MutableTreeNode)node.getParent();
        if (parent == null) {
            throw new IllegalArgumentException(Messages.getString("swing.AF")); //$NON-NLS-1$
        }
        int index = parent.getIndex(node);
        parent.remove(node);
        nodesWereRemoved(parent, new int[] {index}, new Object[] {node});
    }

    public void nodeChanged(final TreeNode node) {
        if (node == root) {
            nodesChanged(node, null);
            return;
        }
        if (node == null) {
            return;
        }
        final TreeNode parent = node.getParent();
        if (parent == null) {
            return;
        }
        nodesChanged(parent, new int[] {getIndexOfChild(parent, node)});
    }

    public void nodesChanged(final TreeNode node,
                             final int[] childIndices) {
        if (node == null || node != root && Utilities.isEmptyArray(childIndices)) {
            return;
        }

        fireTreeNodesChanged(this, getPathToRoot(node), childIndices,
                             getNodeChildren(node, childIndices));
    }

    public void nodesWereInserted(final TreeNode node,
                                  final int[] childIndices) {
        if (node == null || Utilities.isEmptyArray(childIndices)) {
            return;
        }

        fireTreeNodesInserted(this, getPathToRoot(node), childIndices,
                              getNodeChildren(node, childIndices));
    }

    public void nodesWereRemoved(final TreeNode node,
                                 final int[] childIndices,
                                 final Object[] removedChildren) {
        if (node == null || Utilities.isEmptyArray(childIndices)) {
            return;
        }
        fireTreeNodesRemoved(this, getPathToRoot(node), childIndices, removedChildren);
    }

    public void nodeStructureChanged(final TreeNode node) {
        if (node == null) {
            return;
        }
        fireTreeStructureChanged(this, getPathToRoot(node), null, null);
    }

    public TreeNode[] getPathToRoot(final TreeNode aNode) {
        if (aNode == null) {
            return new TreeNode[0];
        }

        return getPathToRoot(aNode, 0);
    }

    protected TreeNode[] getPathToRoot(final TreeNode aNode,
                                       final int depth) {
        return TreeCommons.getPathToAncestor(aNode, root, depth);
    }

    protected void fireTreeNodesChanged(final Object source,
                                        final Object[] path,
                                        final int[] childIndices,
                                        final Object[] children) {
        TreeModelListener[] listeners = getTreeModelListeners();
        if (Utilities.isEmptyArray(listeners)) {
            return;
        }

        TreeModelEvent event = new TreeModelEvent(source, path, childIndices, children);
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].treeNodesChanged(event);
        }
    }

    protected void fireTreeNodesInserted(final Object source,
                                         final Object[] path,
                                         final int[] childIndices,
                                         final Object[] children) {
        TreeModelListener[] listeners = getTreeModelListeners();
        if (Utilities.isEmptyArray(listeners)) {
            return;
        }

        TreeModelEvent event = new TreeModelEvent(source, path, childIndices, children);
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].treeNodesInserted(event);
        }
    }

    protected void fireTreeNodesRemoved(final Object source,
                                        final Object[] path,
                                        final int[] childIndices,
                                        final Object[] children) {

        TreeModelListener[] listeners = getTreeModelListeners();
        if (Utilities.isEmptyArray(listeners)) {
            return;
        }

        TreeModelEvent event = new TreeModelEvent(source, path, childIndices, children);
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].treeNodesRemoved(event);
        }
    }

    protected void fireTreeStructureChanged(final Object source,
                                            final Object[] path,
                                            final int[] childIndices,
                                            final Object[] children) {
        TreeModelListener[] listeners = getTreeModelListeners();
        if (Utilities.isEmptyArray(listeners)) {
            return;
        }

        TreeModelEvent event = new TreeModelEvent(source, path, childIndices, children);
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].treeStructureChanged(event);
        }
    }

    private void fireRootChangedToNull(final Object source) {
        TreeModelListener[] listeners = getTreeModelListeners();
        if (Utilities.isEmptyArray(listeners)) {
            return;
        }

        TreeModelEvent event = new TreeModelEvent(source, (TreePath)null);
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].treeStructureChanged(event);
        }
    }

    private Object[] getNodeChildren(final TreeNode node, final int[] childIndices) {
        if (childIndices == null) {
            return null;
        }

        Object[] result = new Object[childIndices.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = node.getChildAt(childIndices[i]);
        }
        return result;
    }

}
