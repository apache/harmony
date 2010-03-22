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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class DefaultMutableTreeNode implements Cloneable, MutableTreeNode, Serializable {
    public static final Enumeration<TreeNode> EMPTY_ENUMERATION = new Vector<TreeNode>().elements();

    protected MutableTreeNode parent;
    protected Vector children;
    protected transient Object userObject;
    protected boolean allowsChildren;

    public DefaultMutableTreeNode() {
        this(null);
    }

    public DefaultMutableTreeNode(final Object userObject) {
        this(userObject, true);
    }

    public DefaultMutableTreeNode(final Object userObject, final boolean allowsChildren) {
        setUserObject(userObject);
        setAllowsChildren(allowsChildren);
    }

    public void insert(final MutableTreeNode child, final int childIndex) {
        if (!getAllowsChildren()) {
            throw new IllegalStateException(Messages.getString("swing.A6")); //$NON-NLS-1$
        }
        if (child == null || isNodeAncestor(child)) {
            throw new IllegalArgumentException(Messages.getString("swing.A7")); //$NON-NLS-1$
        }

        if (child.getParent() instanceof MutableTreeNode) {
            ((MutableTreeNode)child.getParent()).remove(child);
        }
        child.setParent(this);
        getChildren().insertElementAt(child, childIndex);
    }

    public void remove(final int childIndex) {
        MutableTreeNode child = (MutableTreeNode)getChildren().remove(childIndex);
        child.setParent(null);
    }

    public void setParent(final MutableTreeNode parent) {
        this.parent = parent;
    }

    public TreeNode getParent() {
        return parent;
    }

    public TreeNode getChildAt(final int index) {
        return (TreeNode)getChildren().get(index);
    }

    public int getChildCount() {
        return children != null ? children.size() : 0;
    }

    public int getIndex(final TreeNode child) {
        if (child == null) {
            throw new IllegalArgumentException(Messages.getString("swing.A8")); //$NON-NLS-1$
        }

        return children != null ? children.indexOf(child) : -1;
    }

    public Enumeration children() {
        return children != null ? children.elements() : EMPTY_ENUMERATION;
    }

    public void setAllowsChildren(final boolean allows) {
        allowsChildren = allows;
        if (!allowsChildren && children != null) {
            children.clear();
        }
    }

    public boolean getAllowsChildren() {
        return allowsChildren;
    }

    public void setUserObject(final Object userObject) {
        this.userObject = userObject;
    }

    public Object getUserObject() {
        return userObject;
    }

    public void removeFromParent() {
        if (parent != null) {
            parent.remove(this);
        }
    }

    public void remove(final MutableTreeNode child) {
        int index = -1;
        if (child == null || children == null || (index = children.indexOf(child)) == -1) {
            throw new IllegalArgumentException(Messages.getString("swing.A9")); //$NON-NLS-1$
        }
        remove(index);
    }

    public void removeAllChildren() {
        if (children == null) {
            return;
        }
        for (Iterator it = children.iterator(); it.hasNext(); ) {
            MutableTreeNode child = (MutableTreeNode)it.next();
            child.setParent(null);
            it.remove();
        }
    }

    public void add(final MutableTreeNode child) {
        insert(child, getChildCount() - (isNodeChild(child) ? 1 : 0));
    }

    public boolean isNodeAncestor(final TreeNode anotherNode) {
        if (anotherNode == null) {
            return false;
        }
        TreeNode currentParent = this;
        while (currentParent != null) {
            if (currentParent == anotherNode) {
                return true;
            }
            currentParent = currentParent.getParent();
        }

        return false;
    }

    public boolean isNodeDescendant(final DefaultMutableTreeNode anotherNode) {
        return anotherNode != null ? anotherNode.isNodeAncestor(this) : false;
    }

    public TreeNode getSharedAncestor(final DefaultMutableTreeNode anotherNode) {
        TreeNode currentParent = anotherNode;
        while (currentParent != null) {
            if (isNodeAncestor(currentParent)) {
                return currentParent;
            }

            currentParent = currentParent.getParent();
        }

        return null;
    }

    public boolean isNodeRelated(final DefaultMutableTreeNode node) {
        return getSharedAncestor(node) != null;
    }

    public int getDepth() {
        if (children == null || children.size() == 0) {
            return 0;
        }
        int childrenDepth = 0;
        for (Iterator it = children.iterator(); it.hasNext(); ) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)it.next();
            int childDepth = child.getDepth();
            if (childDepth > childrenDepth) {
                childrenDepth = childDepth;
            }
        }
        return childrenDepth + 1;
    }

    public int getLevel() {
        int result = 0;
        TreeNode currentParent = getParent();
        while (currentParent != null) {
            currentParent = currentParent.getParent();
            result++;
        }

        return result;
    }

    public TreeNode[] getPath() {
        return getPathToRoot(this, 0);
    }

    public Object[] getUserObjectPath() {
        TreeNode[] path = getPath();
        Object[] result = new Object[path.length];
        for (int i = 0; i < path.length; i++) {
            result[i] = ((DefaultMutableTreeNode)path[i]).getUserObject();
        }

        return result;
    }

    public TreeNode getRoot() {
        TreeNode currentNode = this;
        while (currentNode.getParent() != null) {
            currentNode = currentNode.getParent();
        }

        return currentNode;
    }

    public boolean isRoot() {
        return getParent() == null;
    }

    public DefaultMutableTreeNode getNextNode() {
        if (getRoot() == null) {
            return null;
        }
        Enumeration preorderEnum = preorderEnumeration(getRoot());
        while (preorderEnum.hasMoreElements()) {
            TreeNode curNode = (TreeNode)preorderEnum.nextElement();
            if (curNode == this) {
                return preorderEnum.hasMoreElements() ? (DefaultMutableTreeNode)preorderEnum.nextElement() : null;
            }
        }

        return null;
    }

    public DefaultMutableTreeNode getPreviousNode() {
        if (getParent() == null) {
            return null;
        }
        Enumeration preorderEnum = ((DefaultMutableTreeNode)getParent()).preorderEnumeration();
        DefaultMutableTreeNode previousNode = null;
        while (preorderEnum.hasMoreElements()) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)preorderEnum.nextElement();
            if (currentNode == this) {
                break;
            }
            previousNode = currentNode;
        }

        return previousNode;
    }

    public Enumeration preorderEnumeration() {
        return preorderEnumeration(this);
    }

    public Enumeration postorderEnumeration() {
        return depthFirstEnumeration(this);
    }

    public Enumeration breadthFirstEnumeration() {
        return new Enumeration() {
            private Enumeration children;
            private List nextLevelChildren;

            public Object nextElement() {
                if (nextLevelChildren == null) {
                    nextLevelChildren = new LinkedList();
                    nextLevelChildren.add(children());
                    return DefaultMutableTreeNode.this;
                }

                if (children == null || !children.hasMoreElements()) {
                    if (nextLevelChildren.isEmpty()) {
                        throw new NoSuchElementException(Messages.getString("swing.AA")); //$NON-NLS-1$
                    } else {
                        children = (Enumeration)nextLevelChildren.remove(0);
                    }
                }

                TreeNode result = (TreeNode)children.nextElement();
                if (result.getChildCount() > 0) {
                    nextLevelChildren.add(result.children());
                }

                return result;
            }

            public boolean hasMoreElements() {
                return nextLevelChildren == null || children != null && children.hasMoreElements();
            }
        };
    }

    public Enumeration depthFirstEnumeration() {
        return postorderEnumeration();
    }

    public Enumeration pathFromAncestorEnumeration(final TreeNode ancestor) {
        if (!isNodeAncestor(ancestor)) {
            throw new IllegalArgumentException(Messages.getString("swing.AB")); //$NON-NLS-1$
        }

        return new Enumeration() {
            private TreeNode previousAncestor;

            public Object nextElement() {
                if (previousAncestor == null) {
                    previousAncestor = ancestor;
                    return ancestor;
                }

                if (previousAncestor == DefaultMutableTreeNode.this) {
                    throw new NoSuchElementException(Messages.getString("swing.AA")); //$NON-NLS-1$
                }
                TreeNode nextNode = DefaultMutableTreeNode.this;
                while (nextNode.getParent() != previousAncestor) {
                    nextNode = nextNode.getParent();
                }
                previousAncestor = nextNode;

                return previousAncestor;
            }

            public boolean hasMoreElements() {
                return previousAncestor != DefaultMutableTreeNode.this;
            }
        };
    }

    public boolean isNodeChild(final TreeNode child) {
        return child != null && children != null ? children.contains(child) : false;
    }

    public TreeNode getFirstChild() {
        if (children == null || children.isEmpty()) {
            throw new NoSuchElementException(Messages.getString("swing.AC")); //$NON-NLS-1$
        }

        return (TreeNode)children.get(0);
    }

    public TreeNode getLastChild() {
        if (children == null || children.isEmpty()) {
            throw new NoSuchElementException(Messages.getString("swing.AC")); //$NON-NLS-1$
        }

        return (TreeNode)children.get(children.size() - 1);
    }

    public TreeNode getChildAfter(final TreeNode child) {
        int index = -1;
        if (child == null || (index = getIndex(child)) == -1) {
            throw new IllegalArgumentException(Messages.getString("swing.AD")); //$NON-NLS-1$
        }

        return index + 1 < getChildCount() ? getChildAt(index + 1) : null;
    }

    public TreeNode getChildBefore(final TreeNode child) {
        int index = -1;
        if (child == null || (index = getIndex(child)) == -1) {
            throw new IllegalArgumentException(Messages.getString("swing.AD")); //$NON-NLS-1$
        }

        return index > 0 ? getChildAt(index - 1) : null;
    }

    public boolean isNodeSibling(final TreeNode sibling) {
        if (sibling == null) {
            return false;
        }

        if (sibling == this) {
            return true;
        }

        return getParent() != null && getParent() == sibling.getParent();
    }

    public int getSiblingCount() {
        if (getParent() == null) {
            return 1;
        }

        Enumeration children = getParent().children();
        int result = 0;
        while(children.hasMoreElements()) {
            children.nextElement();
            result++;
        }

        return result;
    }

    public DefaultMutableTreeNode getNextSibling() {
        if (getParent() == null) {
            return null;
        }

        return (DefaultMutableTreeNode)((DefaultMutableTreeNode)getParent()).getChildAfter(this);
    }

    public DefaultMutableTreeNode getPreviousSibling() {
        if (getParent() == null) {
            return null;
        }

        return (DefaultMutableTreeNode)((DefaultMutableTreeNode)getParent()).getChildBefore(this);
    }

    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    public DefaultMutableTreeNode getFirstLeaf() {
        TreeNode curNode = this;
        while (!curNode.isLeaf() && curNode.getChildCount() > 0) {
            curNode = curNode.getChildAt(0);
        }

        return (DefaultMutableTreeNode)curNode;
    }

    public DefaultMutableTreeNode getLastLeaf() {
        TreeNode curNode = this;
        while (!curNode.isLeaf() && curNode.getChildCount() > 0) {
            curNode = curNode.getChildAt(curNode.getChildCount() - 1);
        }

        return (DefaultMutableTreeNode)curNode;
    }

    public DefaultMutableTreeNode getNextLeaf() {
        if (getRoot() == null) {
            return null;
        }

        boolean nodeFound = false;
        Enumeration depthEnum = depthFirstEnumeration(getRoot());
        while (depthEnum.hasMoreElements()) {
            TreeNode nextNode = (TreeNode)depthEnum.nextElement();
            if (nodeFound && nextNode.isLeaf()) {
                return (DefaultMutableTreeNode)nextNode;
            }
            if (nextNode == this) {
                nodeFound = true;
            }
        }

        return null;
    }

    public DefaultMutableTreeNode getPreviousLeaf() {
        if (getRoot() == null) {
            return null;
        }

        TreeNode previousLeaf = null;
        Enumeration preEnum = preorderEnumeration(getRoot());
        while (preEnum.hasMoreElements()) {
            TreeNode nextNode = (TreeNode)preEnum.nextElement();
            if (nextNode == this) {
                return (DefaultMutableTreeNode)previousLeaf;
            }
            if (nextNode.isLeaf()) {
                previousLeaf = nextNode;
            }
        }

        return null;
    }

    public int getLeafCount() {
        int result = 0;
        Enumeration preEnum = preorderEnumeration();
        while(preEnum.hasMoreElements()) {
            DefaultMutableTreeNode nextNode = (DefaultMutableTreeNode)preEnum.nextElement();
            if (nextNode.isLeaf()) {
                result++;
            }
        }

        return result;
    }

    public String toString() {
        return getUserObject() != null ? getUserObject().toString() : null;
    }

    public Object clone() {
        return new DefaultMutableTreeNode(getUserObject());
    }


    protected TreeNode[] getPathToRoot(final TreeNode node, final int depth) {
        if (node == null) {
            return new TreeNode[depth];
        }
        TreeNode[] result = getPathToRoot(node.getParent(), depth + 1);
        result[result.length - 1 - depth] = node;

        return result;
    }


    private Vector getChildren() {
        if (children == null) {
            children = new Vector();
        }

        return children;
    }


    private static Enumeration preorderEnumeration(final TreeNode root) {
        return new Enumeration() {
            private Enumeration children;
            private Enumeration subChildren;

            public Object nextElement() {
                if (children == null) {
                    children = root.children();
                    return root;
                }

                if (subChildren != null && subChildren.hasMoreElements()) {
                    return subChildren.nextElement();
                }
                if (children.hasMoreElements()) {
                    subChildren = preorderEnumeration((TreeNode)children.nextElement());
                    return subChildren.nextElement();
                }

                throw new NoSuchElementException(Messages.getString("swing.AA")); //$NON-NLS-1$
            }

            public boolean hasMoreElements() {
                return children == null || children.hasMoreElements()
                       || subChildren != null && subChildren.hasMoreElements();
            }
        };
    }

    private static Enumeration depthFirstEnumeration(final TreeNode root) {
        return new Enumeration() {
            private Enumeration children = root.children();
            private Enumeration subChildren;

            public Object nextElement() {
                if (subChildren != null && subChildren.hasMoreElements()) {
                    return subChildren.nextElement();
                }

                if (children != null && children.hasMoreElements()) {
                    subChildren = depthFirstEnumeration((TreeNode)children.nextElement());
                    return subChildren.nextElement();
                } else if (children != null) {
                    children = null;
                    return root;
                } else {
                    throw new NoSuchElementException(Messages.getString("swing.AA")); //$NON-NLS-1$
                }
            }

            public boolean hasMoreElements() {
                return children != null;
            }
        };
    }
}
