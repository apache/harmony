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

import java.util.Enumeration;
import java.util.NoSuchElementException;
import javax.swing.BasicSwingTestCase;

public class DefaultMutableTreeNodeTest extends BasicSwingTestCase {
    private DefaultMutableTreeNode node;

    public DefaultMutableTreeNodeTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        node = new DefaultMutableTreeNode();
    }

    @Override
    protected void tearDown() throws Exception {
        node = null;
    }

    public void testEMPTY_ENUMERATION() throws Exception {
        assertFalse(DefaultMutableTreeNode.EMPTY_ENUMERATION.hasMoreElements());
    }

    public void testDefaultMutableTreeNode() throws Exception {
        assertNull(node.parent);
        assertNull(node.children);
        assertEquals(0, node.getChildCount());
        assertNull(node.userObject);
        assertTrue(node.allowsChildren);
        node = new DefaultMutableTreeNode("user object");
        assertNull(node.parent);
        assertNull(node.children);
        assertEquals(0, node.getChildCount());
        assertEquals("user object", node.userObject);
        assertTrue(node.allowsChildren);
        node = new DefaultMutableTreeNode("user object", false);
        assertNull(node.parent);
        assertNull(node.children);
        assertEquals(0, node.getChildCount());
        assertEquals("user object", node.userObject);
        assertFalse(node.allowsChildren);
    }

    public void testInsert() throws Exception {
        DefaultMutableTreeNode root1 = new DefaultMutableTreeNode();
        final DefaultMutableTreeNode root2 = new DefaultMutableTreeNode();
        final DefaultMutableTreeNode insertingChild = new DefaultMutableTreeNode();
        root1.add(insertingChild);
        assertEquals(1, root1.getChildCount());
        assertSame(root1, insertingChild.getParent());
        root2.add(new DefaultMutableTreeNode());
        root2.add(new DefaultMutableTreeNode());
        assertEquals(2, root2.getChildCount());
        root2.insert(insertingChild, 1);
        assertEquals(3, root2.getChildCount());
        assertEquals(0, root1.getChildCount());
        assertSame(root2, insertingChild.getParent());
        assertSame(insertingChild, root2.getChildAt(1));
        root2.insert(insertingChild, 0);
        assertEquals(3, root2.getChildCount());
        assertSame(root2, insertingChild.getParent());
        assertSame(insertingChild, root2.getChildAt(0));
        root2.insert(insertingChild, 2);
        assertEquals(3, root2.getChildCount());
        assertSame(root2, insertingChild.getParent());
        assertSame(insertingChild, root2.getChildAt(2));
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() throws Exception {
                root2.insert(insertingChild, 3);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new DefaultMutableTreeNode().insert(new DefaultMutableTreeNode(), 2);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new DefaultMutableTreeNode().insert(new DefaultMutableTreeNode(), -1);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new DefaultMutableTreeNode().insert(null, 0);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                DefaultMutableTreeNode root = new DefaultMutableTreeNode();
                DefaultMutableTreeNode child = new DefaultMutableTreeNode();
                root.insert(child, 0);
                child.insert(root, 0);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                DefaultMutableTreeNode root = new DefaultMutableTreeNode();
                root.insert(root, 0);
            }
        });
        testExceptionalCase(new IllegalStateCase() {
            @Override
            public void exceptionalAction() throws Exception {
                DefaultMutableTreeNode root = new DefaultMutableTreeNode();
                root.setAllowsChildren(false);
                root.insert(new DefaultMutableTreeNode(), 0);
            }
        });
    }

    public void testRemove() throws Exception {
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        node.add(child);
        node.add(new DefaultMutableTreeNode());
        assertEquals(2, node.getChildCount());
        node.remove(1);
        assertEquals(1, node.getChildCount());
        node.remove(child);
        assertEquals(0, node.getChildCount());
        assertNull(child.getParent());
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() throws Exception {
                node.remove(0);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                node.remove(null);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                node.remove(new DefaultMutableTreeNode());
            }
        });
    }

    public void testGetSetParent() throws Exception {
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        node.add(child);
        assertSame(node, child.getParent());
        assertEquals(1, node.getChildCount());
        DefaultMutableTreeNode parent = new DefaultMutableTreeNode();
        child.setParent(parent);
        assertSame(parent, child.getParent());
        assertEquals(1, node.getChildCount());
        assertEquals(child, node.getChildAt(0));
        assertEquals(0, parent.getChildCount());
    }

    public void testGetChildAt() throws Exception {
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode();
        root.add(child1);
        root.add(child2);
        assertSame(child1, root.getChildAt(0));
        assertSame(child2, root.getChildAt(1));
        DefaultMutableTreeNode child3 = new DefaultMutableTreeNode();
        root.insert(child3, 1);
        assertSame(child1, root.getChildAt(0));
        assertSame(child3, root.getChildAt(1));
        assertSame(child2, root.getChildAt(2));
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() throws Exception {
                root.getChildAt(3);
            }
        });
        testExceptionalCase(new ArrayIndexOutOfBoundsCase() {
            @Override
            public void exceptionalAction() throws Exception {
                root.getChildAt(-1);
            }
        });
    }

    public void testGetChildCount() throws Exception {
        assertEquals(0, node.getChildCount());
        node.add(new DefaultMutableTreeNode());
        assertEquals(1, node.getChildCount());
        node.add(new DefaultMutableTreeNode());
        assertEquals(2, node.getChildCount());
        node.remove(0);
        assertEquals(1, node.getChildCount());
    }

    public void testGetIndex() throws Exception {
        assertEquals(-1, node.getIndex(new DefaultMutableTreeNode()));
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        node.add(new DefaultMutableTreeNode());
        node.add(child);
        node.add(new DefaultMutableTreeNode());
        assertEquals(1, node.getIndex(child));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                node.getIndex(null);
            }
        });
    }

    public void testChildren() throws Exception {
        assertSame(DefaultMutableTreeNode.EMPTY_ENUMERATION, node.children());
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode();
        node.add(child1);
        node.add(child2);
        Enumeration<?> children = node.children();
        assertSame(child1, children.nextElement());
        assertSame(child2, children.nextElement());
    }

    public void testGetSetAllowsChildren() throws Exception {
        assertTrue(node.getAllowsChildren());
        node.add(new DefaultMutableTreeNode());
        node.add(new DefaultMutableTreeNode());
        node.setAllowsChildren(false);
        assertFalse(node.getAllowsChildren());
        assertEquals(0, node.getChildCount());
        assertNotNull(node.children);
        assertTrue(node.children.isEmpty());
        node.setAllowsChildren(true);
        assertTrue(node.getAllowsChildren());
        assertEquals(0, node.getChildCount());
        node.add(new DefaultMutableTreeNode());
        node.setAllowsChildren(true);
        assertEquals(1, node.getChildCount());
    }

    public void testGetSetUserObject() throws Exception {
        assertNull(node.getUserObject());
        Object user = new Object();
        node.setUserObject(user);
        assertSame(user, node.getUserObject());
    }

    public void testRemoveFromParent() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        root.add(node);
        node.add(new DefaultMutableTreeNode());
        assertSame(root, node.getParent());
        assertEquals(1, node.getChildCount());
        node.removeFromParent();
        assertNull(node.getParent());
        assertEquals(1, node.getChildCount());
        assertEquals(0, root.getChildCount());
    }

    public void testRemoveAllChildren() throws Exception {
        node.removeAllChildren();
        assertEquals(0, node.getChildCount());
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode();
        node.add(child1);
        node.add(child2);
        assertEquals(2, node.getChildCount());
        node.removeAllChildren();
        assertEquals(0, node.getChildCount());
        assertNull(child1.getParent());
        assertNull(child2.getParent());
    }

    public void testAdd() throws Exception {
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode();
        node.add(child1);
        assertSame(child1, node.getChildAt(0));
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode();
        node.add(child2);
        assertSame(child2, node.getChildAt(1));
        assertEquals(2, node.getChildCount());
        node.add(child1);
        assertEquals(2, node.getChildCount());
        assertSame(child2, node.getChildAt(0));
        assertSame(child1, node.getChildAt(1));
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        root.add(child1);
        assertEquals(1, node.getChildCount());
        assertEquals(1, root.getChildCount());
        assertEquals(root, child1.getParent());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                node.add(null);
            }
        });
        testExceptionalCase(new IllegalStateCase() {
            @Override
            public void exceptionalAction() throws Exception {
                node.setAllowsChildren(false);
                node.add(new DefaultMutableTreeNode());
            }
        });
    }

    public void testIsNodeAncestor() throws Exception {
        assertFalse(node.isNodeAncestor(null));
        assertFalse(node.isNodeAncestor(new DefaultMutableTreeNode()));
        assertTrue(node.isNodeAncestor(node));
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode();
        node.add(child);
        child.add(childChild);
        assertTrue(childChild.isNodeAncestor(child));
        assertTrue(child.isNodeAncestor(node));
        assertTrue(childChild.isNodeAncestor(node));
        child.setParent(null);
        assertFalse(child.isNodeAncestor(node));
    }

    public void testIsNodeDescendant() throws Exception {
        assertFalse(node.isNodeDescendant(null));
        assertFalse(node.isNodeDescendant(new DefaultMutableTreeNode()));
        assertTrue(node.isNodeDescendant(node));
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode();
        node.add(child);
        child.add(childChild);
        assertTrue(child.isNodeDescendant(childChild));
        assertTrue(node.isNodeDescendant(child));
        assertTrue(node.isNodeDescendant(childChild));
        child.setParent(null);
        assertFalse(node.isNodeDescendant(child));
    }

    public void testGetSharedAncestor() throws Exception {
        assertNull(node.getSharedAncestor(null));
        assertEquals(node, node.getSharedAncestor(node));
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode();
        root.add(child);
        child.add(childChild);
        DefaultMutableTreeNode nodeRoot = new DefaultMutableTreeNode();
        nodeRoot.add(node);
        child.add(nodeRoot);
        assertEquals(child, node.getSharedAncestor(childChild));
        assertEquals(child, node.getSharedAncestor(child));
        assertEquals(child, child.getSharedAncestor(node));
        assertEquals(child, childChild.getSharedAncestor(nodeRoot));
        assertEquals(root, node.getSharedAncestor(root));
        nodeRoot.setParent(null);
        assertEquals(nodeRoot, node.getSharedAncestor(nodeRoot));
        assertNull(node.getSharedAncestor(child));
    }

    public void testIsNodeRelated() throws Exception {
        assertFalse(node.isNodeRelated(null));
        assertTrue(node.isNodeRelated(node));
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode();
        root.add(child);
        child.add(childChild);
        DefaultMutableTreeNode nodeRoot = new DefaultMutableTreeNode();
        nodeRoot.add(node);
        child.add(nodeRoot);
        assertTrue(node.isNodeRelated(childChild));
        assertTrue(node.isNodeRelated(child));
        assertTrue(child.isNodeRelated(node));
        assertTrue(childChild.isNodeRelated(nodeRoot));
        assertTrue(node.isNodeRelated(root));
        nodeRoot.setParent(null);
        assertTrue(node.isNodeRelated(nodeRoot));
        assertFalse(node.isNodeRelated(child));
    }

    public void testGetDepth() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode();
        root.add(child);
        child.add(childChild);
        child.add(node);
        node.add(new DefaultMutableTreeNode());
        assertEquals(1, node.getDepth());
        assertEquals(2, child.getDepth());
        assertEquals(3, root.getDepth());
        DefaultMutableTreeNode childChildChild = new DefaultMutableTreeNode();
        childChild.add(childChildChild);
        assertEquals(3, root.getDepth());
        DefaultMutableTreeNode childChildChildChild = new DefaultMutableTreeNode();
        childChildChild.add(childChildChildChild);
        assertEquals(4, root.getDepth());
        assertEquals(0, childChildChildChild.getDepth());
    }

    public void testGetLevel() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode();
        root.add(child);
        child.add(childChild);
        child.add(node);
        assertEquals(0, root.getLevel());
        assertEquals(2, node.getLevel());
        assertEquals(2, childChild.getLevel());
    }

    public void testGetPath() throws Exception {
        assertEquals(1, node.getPath().length);
        assertEquals(node, node.getPath()[0]);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        root.add(child);
        child.add(node);
        assertEquals(3, node.getPath().length);
        assertEquals(root, node.getPath()[0]);
        assertEquals(child, node.getPath()[1]);
        assertEquals(node, node.getPath()[2]);
        child.setParent(null);
        assertEquals(2, node.getPath().length);
        assertSame(child, node.getPath()[0]);
        assertSame(node, node.getPath()[1]);
        DefaultMutableTreeNode otherRoot = new DefaultMutableTreeNode();
        child.setParent(otherRoot);
        assertEquals(3, node.getPath().length);
        assertSame(otherRoot, node.getPath()[0]);
        assertSame(child, node.getPath()[1]);
        assertSame(node, node.getPath()[2]);
    }

    public void testGetPathToRoot() throws Exception {
        DefaultMutableTreeNode anyNode = new DefaultMutableTreeNode();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        root.add(node);
        TreeNode[] path = anyNode.getPathToRoot(root, 100);
        assertEquals(101, path.length);
        assertSame(root, path[0]);
        assertNull(path[1]);
        assertNull(path[100]);
        path = anyNode.getPathToRoot(node, 100);
        assertEquals(102, path.length);
        assertSame(root, path[0]);
        assertSame(node, path[1]);
        assertNull(path[2]);
        assertNull(path[101]);
        path = anyNode.getPathToRoot(null, 100);
        assertEquals(100, path.length);
    }

    public void testGetUserObjectPath() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode node = new DefaultMutableTreeNode("node");
        root.add(node);
        Object[] path = root.getUserObjectPath();
        assertEquals(1, path.length);
        assertSame(root.getUserObject(), path[0]);
        path = node.getUserObjectPath();
        assertEquals(2, path.length);
        assertSame(root.getUserObject(), path[0]);
        assertSame(node.getUserObject(), path[1]);
    }

    public void testGetRoot() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        root.add(child);
        child.add(node);
        assertSame(root, root.getRoot());
        assertSame(root, child.getRoot());
        assertSame(root, node.getRoot());
    }

    public void testIsRoot() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        root.add(child);
        assertFalse(child.isRoot());
        assertTrue(root.isRoot());
        assertTrue(node.isRoot());
    }

    public void testGetNextNode() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode();
        root.add(child);
        child.add(childChild);
        root.add(node);
        assertNull(node.getNextNode());
        assertSame(child, root.getNextNode());
        assertSame(childChild, child.getNextNode());
        assertSame(node, childChild.getNextNode());
    }

    public void testGetPreviousNode() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode();
        root.add(child);
        child.add(childChild);
        root.add(node);
        assertSame(childChild, node.getPreviousNode());
        assertSame(child, childChild.getPreviousNode());
        assertSame(root, child.getPreviousNode());
        assertNull(root.getPreviousNode());
    }

    public void testPreorderEnumeration() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode();
        root.add(child);
        child.add(childChild);
        root.add(node);
        Enumeration<?> preEnum = node.preorderEnumeration();
        assertSame(node, preEnum.nextElement());
        assertFalse(preEnum.hasMoreElements());
        preEnum = childChild.preorderEnumeration();
        assertSame(childChild, preEnum.nextElement());
        assertFalse(preEnum.hasMoreElements());
        preEnum = child.preorderEnumeration();
        assertSame(child, preEnum.nextElement());
        assertSame(childChild, preEnum.nextElement());
        assertFalse(preEnum.hasMoreElements());
        preEnum = root.preorderEnumeration();
        assertSame(root, preEnum.nextElement());
        assertSame(child, preEnum.nextElement());
        assertSame(childChild, preEnum.nextElement());
        assertSame(node, preEnum.nextElement());
        assertFalse(preEnum.hasMoreElements());
        preEnum = root.preorderEnumeration();
        root.remove(0);
        assertSame(root, preEnum.nextElement());
        assertSame(node, preEnum.nextElement());
        assertFalse(preEnum.hasMoreElements());
        preEnum = node.preorderEnumeration();
        root.remove(0);
        assertSame(node, preEnum.nextElement());
        assertFalse(preEnum.hasMoreElements());
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                Enumeration<?> preEnum = node.preorderEnumeration();
                preEnum.nextElement();
                preEnum.nextElement();
            }
        });
    }

    public void testPostorderEnumeration() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child = new DefaultMutableTreeNode("child");
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode("childChild");
        DefaultMutableTreeNode nodeChild = new DefaultMutableTreeNode("nodeChild");
        root.add(child);
        child.add(childChild);
        root.add(node);
        node.add(nodeChild);
        Enumeration<?> postEnum = node.postorderEnumeration();
        assertSame(nodeChild, postEnum.nextElement());
        assertSame(node, postEnum.nextElement());
        assertFalse(postEnum.hasMoreElements());
        postEnum = nodeChild.postorderEnumeration();
        assertSame(nodeChild, postEnum.nextElement());
        assertFalse(postEnum.hasMoreElements());
        postEnum = childChild.postorderEnumeration();
        assertSame(childChild, postEnum.nextElement());
        assertFalse(postEnum.hasMoreElements());
        postEnum = child.postorderEnumeration();
        assertSame(childChild, postEnum.nextElement());
        assertSame(child, postEnum.nextElement());
        assertFalse(postEnum.hasMoreElements());
        postEnum = root.postorderEnumeration();
        assertSame(childChild, postEnum.nextElement());
        assertSame(child, postEnum.nextElement());
        assertSame(nodeChild, postEnum.nextElement());
        assertSame(node, postEnum.nextElement());
        assertSame(root, postEnum.nextElement());
        assertFalse(postEnum.hasMoreElements());
        final DefaultMutableTreeNode nodeChildChild = new DefaultMutableTreeNode(
                "nodeChildChild");
        nodeChild.add(nodeChildChild);
        postEnum = root.postorderEnumeration();
        assertSame(childChild, postEnum.nextElement());
        assertSame(child, postEnum.nextElement());
        assertSame(nodeChildChild, postEnum.nextElement());
        assertSame(nodeChild, postEnum.nextElement());
        assertSame(node, postEnum.nextElement());
        assertSame(root, postEnum.nextElement());
        assertFalse(postEnum.hasMoreElements());
        DefaultMutableTreeNode childChild2 = new DefaultMutableTreeNode("childChild2");
        child.add(childChild2);
        postEnum = root.postorderEnumeration();
        assertSame(childChild, postEnum.nextElement());
        assertSame(childChild2, postEnum.nextElement());
        assertSame(child, postEnum.nextElement());
        assertSame(nodeChildChild, postEnum.nextElement());
        assertSame(nodeChild, postEnum.nextElement());
        assertSame(node, postEnum.nextElement());
        assertSame(root, postEnum.nextElement());
        assertFalse(postEnum.hasMoreElements());
        if (isHarmony()) {
            testExceptionalCase(new ExceptionalCase() {
                @Override
                public void exceptionalAction() throws Exception {
                    Enumeration<?> postEnum = nodeChildChild.postorderEnumeration();
                    postEnum.nextElement();
                    postEnum.nextElement();
                }
            });
        }
    }

    public void testDepthFirstEnumeration() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child = new DefaultMutableTreeNode("child");
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode("childChild");
        DefaultMutableTreeNode nodeChild = new DefaultMutableTreeNode("nodeChild");
        root.add(child);
        child.add(childChild);
        root.add(node);
        node.add(nodeChild);
        Enumeration<?> depthEnum = node.depthFirstEnumeration();
        assertSame(nodeChild, depthEnum.nextElement());
        assertSame(node, depthEnum.nextElement());
        assertFalse(depthEnum.hasMoreElements());
        depthEnum = nodeChild.depthFirstEnumeration();
        assertSame(nodeChild, depthEnum.nextElement());
        assertFalse(depthEnum.hasMoreElements());
        depthEnum = childChild.depthFirstEnumeration();
        assertSame(childChild, depthEnum.nextElement());
        assertFalse(depthEnum.hasMoreElements());
        depthEnum = child.depthFirstEnumeration();
        assertSame(childChild, depthEnum.nextElement());
        assertSame(child, depthEnum.nextElement());
        assertFalse(depthEnum.hasMoreElements());
        depthEnum = root.depthFirstEnumeration();
        assertSame(childChild, depthEnum.nextElement());
        assertSame(child, depthEnum.nextElement());
        assertSame(nodeChild, depthEnum.nextElement());
        assertSame(node, depthEnum.nextElement());
        assertSame(root, depthEnum.nextElement());
        assertFalse(depthEnum.hasMoreElements());
        final DefaultMutableTreeNode nodeChildChild = new DefaultMutableTreeNode(
                "nodeChildChild");
        nodeChild.add(nodeChildChild);
        depthEnum = root.depthFirstEnumeration();
        assertSame(childChild, depthEnum.nextElement());
        assertSame(child, depthEnum.nextElement());
        assertSame(nodeChildChild, depthEnum.nextElement());
        assertSame(nodeChild, depthEnum.nextElement());
        assertSame(node, depthEnum.nextElement());
        assertSame(root, depthEnum.nextElement());
        assertFalse(depthEnum.hasMoreElements());
        DefaultMutableTreeNode childChild2 = new DefaultMutableTreeNode("childChild2");
        child.add(childChild2);
        depthEnum = root.depthFirstEnumeration();
        assertSame(childChild, depthEnum.nextElement());
        assertSame(childChild2, depthEnum.nextElement());
        assertSame(child, depthEnum.nextElement());
        assertSame(nodeChildChild, depthEnum.nextElement());
        assertSame(nodeChild, depthEnum.nextElement());
        assertSame(node, depthEnum.nextElement());
        assertSame(root, depthEnum.nextElement());
        assertFalse(depthEnum.hasMoreElements());
        if (isHarmony()) {
            testExceptionalCase(new ExceptionalCase() {
                @Override
                public void exceptionalAction() throws Exception {
                    Enumeration<?> depthEnum = nodeChildChild.depthFirstEnumeration();
                    depthEnum.nextElement();
                    depthEnum.nextElement();
                }
            });
        }
    }

    public void testPathFromAncestorEnumeration() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child = new DefaultMutableTreeNode("child");
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode("childChild");
        DefaultMutableTreeNode nodeChild = new DefaultMutableTreeNode("nodeChild");
        root.add(child);
        child.add(childChild);
        root.add(node);
        node.add(nodeChild);
        Enumeration<?> pathEnum = node.pathFromAncestorEnumeration(root);
        assertSame(root, pathEnum.nextElement());
        assertSame(node, pathEnum.nextElement());
        assertFalse(pathEnum.hasMoreElements());
        pathEnum = childChild.pathFromAncestorEnumeration(root);
        assertSame(root, pathEnum.nextElement());
        assertSame(child, pathEnum.nextElement());
        assertSame(childChild, pathEnum.nextElement());
        assertFalse(pathEnum.hasMoreElements());
        pathEnum = childChild.pathFromAncestorEnumeration(childChild);
        assertSame(childChild, pathEnum.nextElement());
        assertFalse(pathEnum.hasMoreElements());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                node.pathFromAncestorEnumeration(new DefaultMutableTreeNode());
            }
        });
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                Enumeration<?> pathEnum = node.pathFromAncestorEnumeration(node);
                pathEnum.nextElement();
                pathEnum.nextElement();
            }
        });
    }

    public void testBreadthFirstEnumeration() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child = new DefaultMutableTreeNode("child");
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode("childChild");
        DefaultMutableTreeNode nodeChild = new DefaultMutableTreeNode("nodeChild");
        root.add(child);
        child.add(childChild);
        root.add(node);
        node.add(nodeChild);
        Enumeration<?> breadthEnum = node.breadthFirstEnumeration();
        assertSame(node, breadthEnum.nextElement());
        assertSame(nodeChild, breadthEnum.nextElement());
        assertFalse(breadthEnum.hasMoreElements());
        breadthEnum = nodeChild.breadthFirstEnumeration();
        assertSame(nodeChild, breadthEnum.nextElement());
        assertFalse(breadthEnum.hasMoreElements());
        breadthEnum = childChild.breadthFirstEnumeration();
        assertSame(childChild, breadthEnum.nextElement());
        assertFalse(breadthEnum.hasMoreElements());
        breadthEnum = child.breadthFirstEnumeration();
        assertSame(child, breadthEnum.nextElement());
        assertSame(childChild, breadthEnum.nextElement());
        assertFalse(breadthEnum.hasMoreElements());
        breadthEnum = root.breadthFirstEnumeration();
        assertSame(root, breadthEnum.nextElement());
        assertSame(child, breadthEnum.nextElement());
        assertSame(node, breadthEnum.nextElement());
        assertSame(childChild, breadthEnum.nextElement());
        assertSame(nodeChild, breadthEnum.nextElement());
        assertFalse(breadthEnum.hasMoreElements());
        final DefaultMutableTreeNode childChildChild = new DefaultMutableTreeNode(
                "childChildChild");
        childChild.add(childChildChild);
        breadthEnum = root.breadthFirstEnumeration();
        assertSame(root, breadthEnum.nextElement());
        assertSame(child, breadthEnum.nextElement());
        assertSame(node, breadthEnum.nextElement());
        assertSame(childChild, breadthEnum.nextElement());
        assertSame(nodeChild, breadthEnum.nextElement());
        assertSame(childChildChild, breadthEnum.nextElement());
        assertFalse(breadthEnum.hasMoreElements());
        DefaultMutableTreeNode childChild2 = new DefaultMutableTreeNode("childChild2");
        child.add(childChild2);
        breadthEnum = root.breadthFirstEnumeration();
        assertSame(root, breadthEnum.nextElement());
        assertSame(child, breadthEnum.nextElement());
        assertSame(node, breadthEnum.nextElement());
        assertSame(childChild, breadthEnum.nextElement());
        assertSame(childChild2, breadthEnum.nextElement());
        assertSame(nodeChild, breadthEnum.nextElement());
        assertSame(childChildChild, breadthEnum.nextElement());
        assertFalse(breadthEnum.hasMoreElements());
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                Enumeration<?> breadthEnum = childChildChild.breadthFirstEnumeration();
                breadthEnum.nextElement();
                breadthEnum.nextElement();
            }
        });
    }

    public void testIsNodeChild() throws Exception {
        assertFalse(node.isNodeChild(null));
        assertFalse(node.isNodeChild(node));
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        child.setParent(node);
        assertFalse(node.isNodeChild(child));
        child.setParent(null);
        node.add(child);
        assertTrue(node.isNodeChild(child));
    }

    public void testGetFirstChild() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        root.add(node);
        root.add(child);
        assertSame(node, root.getFirstChild());
        testExceptionalCase(new NoSuchElementCase() {
            @Override
            public void exceptionalAction() throws Exception {
                node.getFirstChild();
            }
        });
    }

    public void testGetLastChild() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        root.add(child);
        root.add(node);
        assertSame(node, root.getLastChild());
        testExceptionalCase(new NoSuchElementCase() {
            @Override
            public void exceptionalAction() throws Exception {
                node.getLastChild();
            }
        });
    }

    public void testGetChildAfter() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child3 = new DefaultMutableTreeNode();
        root.add(child1);
        root.add(node);
        root.add(child3);
        assertEquals(node, root.getChildAfter(child1));
        assertEquals(child3, root.getChildAfter(node));
        assertNull(root.getChildAfter(child3));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                node.getChildAfter(null);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                node.getChildAfter(new DefaultMutableTreeNode());
            }
        });
    }

    public void testGetChildBefore() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child3 = new DefaultMutableTreeNode();
        root.add(child1);
        root.add(node);
        root.add(child3);
        assertNull(root.getChildBefore(child1));
        assertEquals(child1, root.getChildBefore(node));
        assertEquals(node, root.getChildBefore(child3));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                node.getChildBefore(null);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                node.getChildBefore(new DefaultMutableTreeNode());
            }
        });
    }

    public void testIsNodeSibling() throws Exception {
        assertFalse(node.isNodeSibling(null));
        assertTrue(node.isNodeSibling(node));
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        root.add(child);
        root.add(node);
        assertTrue(node.isNodeSibling(child));
        assertTrue(child.isNodeSibling(node));
        node.setParent(null);
        assertFalse(child.isNodeSibling(node));
    }

    public void testGetSiblingCount() throws Exception {
        assertEquals(1, node.getSiblingCount());
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        root.add(node);
        assertEquals(1, node.getSiblingCount());
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        root.add(child);
        assertEquals(2, node.getSiblingCount());
        assertEquals(2, child.getSiblingCount());
    }

    public void testGetNextSibling() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        root.add(child);
        root.add(node);
        assertNull(root.getNextSibling());
        assertSame(node, child.getNextSibling());
        assertNull(node.getNextSibling());
    }

    public void testGetPreviousSibling() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        root.add(child);
        root.add(node);
        assertNull(root.getPreviousSibling());
        assertSame(child, node.getPreviousSibling());
        assertNull(child.getPreviousSibling());
        node.setParent(null);
        assertNull(node.getPreviousSibling());
    }

    public void testIsLeaf() throws Exception {
        assertTrue(node.isLeaf());
        node.add(new DefaultMutableTreeNode());
        assertFalse(node.isLeaf());
    }

    public void testGetFirstLeaf() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode();
        root.add(child);
        root.add(node);
        child.add(childChild);
        assertSame(childChild, root.getFirstLeaf());
        assertSame(childChild, child.getFirstLeaf());
        assertSame(node, node.getFirstLeaf());
    }

    public void testGetLastLeaf() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode();
        root.add(node);
        root.add(child);
        child.add(childChild);
        assertSame(node, node.getLastLeaf());
        assertSame(childChild, root.getLastLeaf());
        assertSame(childChild, child.getLastLeaf());
    }

    public void testGetNextLeaf() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child = new DefaultMutableTreeNode("child");
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode("childChild");
        DefaultMutableTreeNode nodeChild = new DefaultMutableTreeNode("nodeChild");
        root.add(node);
        root.add(child);
        child.add(childChild);
        node.add(nodeChild);
        assertNull(root.getNextLeaf());
        assertSame(childChild, node.getNextLeaf());
        assertNull(child.getNextLeaf());
        assertNull(childChild.getNextLeaf());
        assertSame(childChild, nodeChild.getNextLeaf());
    }

    public void testGetPreviousLeaf() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child = new DefaultMutableTreeNode("child");
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode("childChild");
        DefaultMutableTreeNode nodeChild = new DefaultMutableTreeNode("nodeChild");
        root.add(node);
        root.add(child);
        child.add(childChild);
        node.add(nodeChild);
        assertNull(node.getPreviousLeaf());
        assertNull(root.getPreviousLeaf());
        assertSame(nodeChild, child.getPreviousLeaf());
        assertSame(nodeChild, childChild.getPreviousLeaf());
        assertNull(nodeChild.getPreviousLeaf());
    }

    public void testGetLeafCount() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        DefaultMutableTreeNode childChild = new DefaultMutableTreeNode();
        DefaultMutableTreeNode nodeChild = new DefaultMutableTreeNode();
        root.add(node);
        root.add(child);
        child.add(childChild);
        node.add(nodeChild);
        assertEquals(2, root.getLeafCount());
        assertEquals(1, child.getLeafCount());
        assertEquals(1, nodeChild.getLeafCount());
    }

    public void testToString() throws Exception {
        assertNull(node.toString());
        node.setUserObject("user object");
        assertEquals("user object", node.toString());
    }

    public void testClone() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode();
        root.add(node);
        node.add(child);
        Object userObject = new Object();
        node.setUserObject(userObject);
        DefaultMutableTreeNode clone = (DefaultMutableTreeNode) node.clone();
        assertNull(clone.getParent());
        assertEquals(0, clone.getChildCount());
        assertSame(userObject, clone.getUserObject());
    }

    private abstract class ArrayIndexOutOfBoundsCase extends ExceptionalCase {
        @SuppressWarnings("unchecked")
        @Override
        public Class<?> expectedExceptionClass() {
            return ArrayIndexOutOfBoundsException.class;
        }
    }

    private abstract class IllegalStateCase extends ExceptionalCase {
        @SuppressWarnings("unchecked")
        @Override
        public Class<?> expectedExceptionClass() {
            return IllegalStateException.class;
        }
    }

    private abstract class NoSuchElementCase extends ExceptionalCase {
        @SuppressWarnings("unchecked")
        @Override
        public Class<?> expectedExceptionClass() {
            return NoSuchElementException.class;
        }
    }
}
