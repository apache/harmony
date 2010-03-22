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

import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.EventListener;
import javax.swing.SwingTestCase;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.SwingPropertyChangeSupportTest.FindableListener;

public class DefaultTreeModelTest extends SwingTestCase {
    protected DefaultTreeModel model = null;

    protected DefaultMutableTreeNode root = null;

    class ConcreteTreeModelListener extends FindableListener implements TreeModelListener {
        public TreeModelEvent event = null;

        public String type = null;

        public boolean fired = false;

        private final boolean debugOutput;

        ConcreteTreeModelListener() {
            super();
            debugOutput = false;
        }

        ConcreteTreeModelListener(final boolean debugOutput) {
            super();
            this.debugOutput = debugOutput;
        }

        @Override
        public void reset() {
            event = null;
            type = null;
            fired = false;
        }

        public void treeNodesChanged(TreeModelEvent e) {
            event = e;
            fired = true;
            type = "changed";
            if (debugOutput) {
                System.out.println("changed: " + e);
            }
        }

        public void treeNodesInserted(TreeModelEvent e) {
            event = e;
            fired = true;
            type = "inserted";
            if (debugOutput) {
                System.out.println("inserted: " + e);
            }
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            event = e;
            fired = true;
            type = "removed";
            if (debugOutput) {
                System.out.println("removed: " + e);
            }
        }

        public void treeStructureChanged(TreeModelEvent e) {
            event = e;
            fired = true;
            type = "structure";
            if (debugOutput) {
                System.out.println("structure changed: " + e);
            }
        }

        public void checkEvent(Object source, Object[] path, int[] childIndices,
                Object[] children) {
            assertNotNull("event", event);
            assertEquals("source", source, event.getSource());
            assertTrue("path", Arrays.equals(path, event.getPath()));
            assertTrue("childIndices", Arrays.equals(childIndices, event.getChildIndices()));
            assertTrue("children", Arrays.equals(children, event.getChildren()));
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = new DefaultMutableTreeNode("root");
        model = new DefaultTreeModel(root);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.DefaultTreeModel(TreeNode)'
     */
    public void testDefaultTreeModelTreeNode() {
        TreeNode node = new DefaultMutableTreeNode(null);
        model = new DefaultTreeModel(node);
        assertEquals(node, model.root);
        assertFalse(model.asksAllowsChildren);
        assertNotNull(model.listenerList);
        model = new DefaultTreeModel(null);
        assertNull(model.root);
        assertFalse(model.asksAllowsChildren);
        assertNotNull(model.listenerList);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.DefaultTreeModel(TreeNode, boolean)'
     */
    public void testDefaultTreeModelTreeNodeBoolean() {
        TreeNode node = new DefaultMutableTreeNode(null);
        model = new DefaultTreeModel(node, true);
        assertEquals(node, model.root);
        assertTrue(model.asksAllowsChildren);
        assertNotNull(model.listenerList);
        model = new DefaultTreeModel(null, false);
        assertNull(model.root);
        assertFalse(model.asksAllowsChildren);
        assertNotNull(model.listenerList);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.setAsksAllowsChildren(boolean)'
     */
    public void testSetAsksAllowsChildren() {
        TreeNode node = new DefaultMutableTreeNode(null);
        model = new DefaultTreeModel(node);
        model.setAsksAllowsChildren(true);
        assertTrue(model.asksAllowsChildren);
        model.setAsksAllowsChildren(false);
        assertFalse(model.asksAllowsChildren);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.asksAllowsChildren()'
     */
    public void testSetGetAsksAllowsChildren() {
        TreeNode node = new DefaultMutableTreeNode(null);
        model = new DefaultTreeModel(node);
        model.asksAllowsChildren = true;
        assertTrue(model.asksAllowsChildren());
        model.asksAllowsChildren = false;
        assertFalse(model.asksAllowsChildren());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.addTreeModelListener(TreeModelListener)'
     */
    public void testAddTreeModelListener() {
        ConcreteTreeModelListener listener1 = new ConcreteTreeModelListener();
        ConcreteTreeModelListener listener2 = new ConcreteTreeModelListener();
        TreeModelListener[] listenersArray = null;
        model.addTreeModelListener(listener1);
        listenersArray = model.getTreeModelListeners();
        assertTrue(listenersArray.length == 1);
        assertEquals(1, model.listenerList.getListeners(TreeModelListener.class).length);
        assertEquals(1, model.getListeners(TreeModelListener.class).length);
        assertTrue(listener1.findMe(listenersArray) > 0);
        model.addTreeModelListener(listener2);
        listenersArray = model.getTreeModelListeners();
        assertEquals(2, listenersArray.length);
        assertTrue(listener1.findMe(listenersArray) > 0);
        assertTrue(listener2.findMe(listenersArray) > 0);
        model.addTreeModelListener(listener2);
        listenersArray = model.getTreeModelListeners();
        assertEquals(3, listenersArray.length);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.removeTreeModelListener(TreeModelListener)'
     */
    public void testRemoveTreeModelListener() {
        ConcreteTreeModelListener changeListener1 = new ConcreteTreeModelListener();
        ConcreteTreeModelListener changeListener2 = new ConcreteTreeModelListener();
        ConcreteTreeModelListener changeListener3 = new ConcreteTreeModelListener();
        TreeModelListener[] listenersArray = null;
        model.addTreeModelListener(changeListener1);
        model.addTreeModelListener(changeListener2);
        model.addTreeModelListener(changeListener3);
        listenersArray = model.getTreeModelListeners();
        assertEquals(3, listenersArray.length);
        assertEquals(1, changeListener1.findMe(listenersArray));
        assertEquals(1, changeListener2.findMe(listenersArray));
        assertEquals(1, changeListener3.findMe(listenersArray));
        model.removeTreeModelListener(changeListener2);
        listenersArray = model.getTreeModelListeners();
        assertEquals(2, listenersArray.length);
        assertEquals(1, changeListener1.findMe(listenersArray));
        assertEquals(0, changeListener2.findMe(listenersArray));
        assertEquals(1, changeListener3.findMe(listenersArray));
        model.removeTreeModelListener(changeListener1);
        listenersArray = model.getTreeModelListeners();
        assertEquals(1, listenersArray.length);
        assertEquals(1, changeListener3.findMe(listenersArray));
        model.removeTreeModelListener(changeListener3);
        listenersArray = model.getTreeModelListeners();
        assertEquals(0, listenersArray.length);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.getTreeModelListeners()'
     */
    public void testGetTreeModelListeners() {
        ConcreteTreeModelListener changeListener1 = new ConcreteTreeModelListener();
        ConcreteTreeModelListener changeListener2 = new ConcreteTreeModelListener();
        ConcreteTreeModelListener changeListener3 = new ConcreteTreeModelListener();
        TreeModelListener[] listenersArray = null;
        listenersArray = model.getTreeModelListeners();
        assertTrue(listenersArray != null && listenersArray.length == 0);
        model.addTreeModelListener(changeListener1);
        model.addTreeModelListener(changeListener2);
        model.addTreeModelListener(changeListener3);
        model.addTreeModelListener(changeListener2);
        listenersArray = model.getTreeModelListeners();
        assertTrue(listenersArray.length == 4);
        assertTrue(changeListener1.findMe(listenersArray) == 1);
        assertTrue(changeListener2.findMe(listenersArray) == 2);
        assertTrue(changeListener3.findMe(listenersArray) == 1);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.getListeners(EventListener)'
     */
    public void testGetListeners() {
        TreeModelListener listener1 = new ConcreteTreeModelListener();
        TreeModelListener listener2 = new ConcreteTreeModelListener();
        EventListener[] listenersArray = null;
        listenersArray = model.getListeners(TreeModelListener.class);
        assertEquals(0, listenersArray.length);
        model.addTreeModelListener(listener1);
        model.addTreeModelListener(listener2);
        listenersArray = model.getListeners(PropertyChangeListener.class);
        assertEquals(0, listenersArray.length);
        listenersArray = model.getListeners(TreeModelListener.class);
        assertEquals(2, listenersArray.length);
        model.removeTreeModelListener(listener1);
        listenersArray = model.getListeners(TreeModelListener.class);
        assertEquals(1, listenersArray.length);
        model.addTreeModelListener(listener2);
        listenersArray = model.getListeners(TreeModelListener.class);
        assertEquals(2, listenersArray.length);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.getChild(Object, int)'
     */
    public void testGetChild() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("1");
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("2");
        DefaultMutableTreeNode node3 = new DefaultMutableTreeNode("3");
        DefaultMutableTreeNode node4 = new DefaultMutableTreeNode("4");
        DefaultMutableTreeNode node5 = new DefaultMutableTreeNode("5");
        node1.add(node2);
        node1.add(node5);
        node2.add(node3);
        node2.add(node4);
        assertEquals(node2, model.getChild(node1, 0));
        assertEquals(node5, model.getChild(node1, 1));
        assertEquals(node3, model.getChild(node2, 0));
        assertEquals(node4, model.getChild(node2, 1));
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.getChildCount(Object)'
     */
    public void testGetChildCount() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("1");
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("2");
        DefaultMutableTreeNode node3 = new DefaultMutableTreeNode("3");
        DefaultMutableTreeNode node4 = new DefaultMutableTreeNode("4");
        DefaultMutableTreeNode node5 = new DefaultMutableTreeNode("5");
        node1.add(node2);
        node1.add(node5);
        node2.add(node3);
        node2.add(node4);
        assertEquals(2, model.getChildCount(node1));
        assertEquals(2, model.getChildCount(node2));
        assertEquals(0, model.getChildCount(node3));
        assertEquals(0, model.getChildCount(node4));
        assertEquals(0, model.getChildCount(node5));
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.getIndexOfChild(Object, Object)'
     */
    public void testGetIndexOfChild() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode(null);
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode(null);
        DefaultMutableTreeNode node3 = new DefaultMutableTreeNode(null);
        DefaultMutableTreeNode node4 = new DefaultMutableTreeNode(null);
        node1.add(node2);
        node1.add(node3);
        node3.add(node4);
        assertEquals(-1, model.getIndexOfChild(null, node1));
        assertEquals(-1, model.getIndexOfChild(node1, null));
        assertEquals(0, model.getIndexOfChild(node1, node2));
        assertEquals(1, model.getIndexOfChild(node1, node3));
        assertEquals(-1, model.getIndexOfChild(node1, node4));
        assertEquals(0, model.getIndexOfChild(node3, node4));
        assertEquals(-1, model.getIndexOfChild(node4, node3));
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.setRoot(TreeNode)'
     */
    public void testSetRoot() {
        TreeNode root1 = new DefaultMutableTreeNode(null);
        TreeNode root2 = new DefaultMutableTreeNode(null);
        TreeNode root3 = null;
        ConcreteTreeModelListener listener = new ConcreteTreeModelListener(false);
        model.addTreeModelListener(listener);
        model.setRoot(root1);
        assertEquals(root1, model.root);
        assertNotNull(listener.event);
        assertEquals("structure", listener.type);
        listener.checkEvent(model, new Object[] { root1 }, null, null);
        listener.reset();
        model.setRoot(root2);
        assertEquals(root2, model.root);
        assertNotNull(listener.event);
        assertEquals("structure", listener.type);
        listener.checkEvent(model, new Object[] { root2 }, null, null);
        listener.reset();
        model.setRoot(root2);
        assertEquals(root2, model.root);
        assertNotNull(listener.event);
        assertEquals("structure", listener.type);
        listener.checkEvent(model, new Object[] { root2 }, null, null);
        listener.reset();
        model.setRoot(root3);
        assertEquals(root3, model.root);
        assertNotNull(listener.event);
        assertEquals("structure", listener.type);
        listener.checkEvent(model, null, new int[0], null);
        listener.reset();
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.getRoot()'
     */
    public void testGetRoot() {
        TreeNode root1 = new DefaultMutableTreeNode(null);
        TreeNode root2 = new DefaultMutableTreeNode(null);
        TreeNode root3 = null;
        model.root = root1;
        assertEquals(root1, model.getRoot());
        model.root = root2;
        assertEquals(root2, model.getRoot());
        model.root = root3;
        assertEquals(root3, model.getRoot());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.isLeaf(Object)'
     */
    public void testIsLeaf() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode(null, true);
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode(null, false);
        DefaultMutableTreeNode node3 = new DefaultMutableTreeNode(null);
        model.setAsksAllowsChildren(true);
        assertFalse(model.isLeaf(node1));
        assertTrue(model.isLeaf(node2));
        model.setAsksAllowsChildren(false);
        assertTrue(model.isLeaf(node1));
        assertTrue(model.isLeaf(node2));
        node1.add(node3);
        model.setAsksAllowsChildren(true);
        assertFalse(model.isLeaf(node1));
        model.setAsksAllowsChildren(false);
        assertFalse(model.isLeaf(node1));
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.valueForPathChanged(TreePath, Object)'
     */
    public void testValueForPathChanged() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("1");
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("2");
        DefaultMutableTreeNode node3 = new DefaultMutableTreeNode("3");
        DefaultMutableTreeNode node4 = new DefaultMutableTreeNode("4");
        DefaultMutableTreeNode node5 = new DefaultMutableTreeNode("5");
        node1.add(node2);
        node1.add(node5);
        node2.add(node3);
        node2.add(node4);
        model.valueForPathChanged(new TreePath(node1), "11");
        assertEquals("11", node1.getUserObject());
        model.valueForPathChanged(new TreePath(new Object[] { node1, node2 }), "22");
        assertEquals("22", node2.getUserObject());
        assertEquals("11", node1.getUserObject());
        model.valueForPathChanged(new TreePath(new Object[] { node2 }), "222");
        assertEquals("222", node2.getUserObject());
        assertEquals("11", node1.getUserObject());
        model.valueForPathChanged(new TreePath(new Object[] { node2, node4 }), "222444");
        assertEquals("222", node2.getUserObject());
        assertEquals("222444", node4.getUserObject());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.reload()'
     */
    public void testReload() {
        DefaultMutableTreeNode node1 = root;
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("2");
        DefaultMutableTreeNode node3 = new DefaultMutableTreeNode("3");
        DefaultMutableTreeNode node4 = new DefaultMutableTreeNode("4");
        DefaultMutableTreeNode node5 = new DefaultMutableTreeNode("5");
        node1.add(node2);
        node1.add(node5);
        node2.add(node3);
        node2.add(node4);
        ConcreteTreeModelListener listener = new ConcreteTreeModelListener();
        model.addTreeModelListener(listener);
        model.setRoot(null);
        model.reload();
        assertNotNull(listener.event);
        assertEquals("structure", listener.type);
        listener.checkEvent(model, null, new int[0], null);
        listener.reset();
        model.setRoot(root);
        model.reload();
        assertNotNull(listener.event);
        listener.checkEvent(model, new Object[] { root }, null, null);
        listener.reset();
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.reload(TreeNode)'
     */
    public void testReloadTreeNode() {
        DefaultMutableTreeNode node1 = root;
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("2");
        DefaultMutableTreeNode node3 = new DefaultMutableTreeNode("3");
        DefaultMutableTreeNode node4 = new DefaultMutableTreeNode("4");
        DefaultMutableTreeNode node5 = new DefaultMutableTreeNode("5");
        node1.add(node2);
        node1.add(node5);
        node2.add(node3);
        node2.add(node4);
        ConcreteTreeModelListener listener = new ConcreteTreeModelListener();
        model.addTreeModelListener(listener);
        model.reload(null);
        assertNull(listener.event);
        model.setRoot(null);
        model.reload(null);
        assertNotNull(listener.event);
        assertEquals("structure", listener.type);
        listener.checkEvent(model, null, new int[0], null);
        listener.reset();
        model.setRoot(root);
        model.reload(root);
        assertNotNull(listener.event);
        listener.checkEvent(model, new Object[] { root }, null, null);
        listener.reset();
        model.reload(node2);
        assertNotNull(listener.event);
        listener.checkEvent(model, new Object[] { root, node2 }, null, null);
        listener.reset();
        model.reload(node4);
        assertNotNull(listener.event);
        listener.checkEvent(model, new Object[] { root, node2, node4 }, null, null);
        listener.reset();
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.insertNodeInto(MutableTreeNode, MutableTreeNode, int)'
     */
    public void testInsertNodeInto() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("1");
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("2");
        DefaultMutableTreeNode node3 = new DefaultMutableTreeNode("3");
        ConcreteTreeModelListener listener = new ConcreteTreeModelListener();
        model.addTreeModelListener(listener);
        try {
            model.insertNodeInto(null, node1, 0);
            fail("no exception's been thrown");
        } catch (IllegalArgumentException e) {
        }
        model.insertNodeInto(node1, node2, 0);
        assertNotNull(listener.event);
        listener.checkEvent(model, new Object[] { node2 }, new int[] { 0 },
                new Object[] { node1 });
        assertEquals(node1, node2.getChildAt(0));
        listener.reset();
        model.insertNodeInto(node3, node2, 0);
        listener.checkEvent(model, new Object[] { node2 }, new int[] { 0 },
                new Object[] { node3 });
        assertEquals(node3, node2.getChildAt(0));
        assertEquals(node1, node2.getChildAt(1));
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.removeNodeFromParent(MutableTreeNode)'
     */
    public void testRemoveNodeFromParent() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("1");
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("2");
        DefaultMutableTreeNode node3 = new DefaultMutableTreeNode("3");
        node1.add(node2);
        node1.add(node3);
        ConcreteTreeModelListener listener = new ConcreteTreeModelListener();
        model.addTreeModelListener(listener);
        try {
            model.removeNodeFromParent(node1);
            fail("no exception's been thrown");
        } catch (IllegalArgumentException e) {
        }
        model.removeNodeFromParent(node3);
        assertNotNull(listener.event);
        listener.checkEvent(model, new Object[] { node1 }, new int[] { 1 },
                new Object[] { node3 });
        assertEquals(1, node1.getChildCount());
        listener.reset();
        model.removeNodeFromParent(node2);
        assertNotNull(listener.event);
        listener.checkEvent(model, new Object[] { node1 }, new int[] { 0 },
                new Object[] { node2 });
        assertEquals(0, node1.getChildCount());
        listener.reset();
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.nodeChanged(TreeNode)'
     */
    public void testNodeChanged() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("1");
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("2");
        DefaultMutableTreeNode node3 = new DefaultMutableTreeNode("3");
        DefaultMutableTreeNode node4 = new DefaultMutableTreeNode("4");
        DefaultMutableTreeNode node5 = new DefaultMutableTreeNode("4");
        node1.add(node2);
        node1.add(node5);
        node2.add(node3);
        node2.add(node4);
        ConcreteTreeModelListener listener = new ConcreteTreeModelListener();
        model.addTreeModelListener(listener);
        model.nodeChanged(null);
        assertNull(listener.event);
        model.nodeChanged(node1);
        assertNull(listener.event);
        model.nodeChanged(root);
        assertNotNull(listener.event);
        assertEquals("changed", listener.type);
        listener.checkEvent(model, new Object[] { root }, null, null);
        listener.reset();
        model.nodeChanged(node2);
        assertNotNull(listener.event);
        assertEquals("changed", listener.type);
        listener.checkEvent(model, new Object[] { node1 }, new int[] { 0 },
                new Object[] { node2 });
        listener.reset();
        model.nodeChanged(node3);
        assertNotNull(listener.event);
        assertEquals("changed", listener.type);
        listener.checkEvent(model, new Object[] { node1, node2 }, new int[] { 0 },
                new Object[] { node3 });
        listener.reset();
        model.nodeChanged(node4);
        assertNotNull(listener.event);
        assertEquals("changed", listener.type);
        listener.checkEvent(model, new Object[] { node1, node2 }, new int[] { 1 },
                new Object[] { node4 });
        listener.reset();
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.nodesWereInserted(TreeNode, int[])'
     */
    public void testNodesWereInserted() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("1");
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("2");
        DefaultMutableTreeNode node3 = new DefaultMutableTreeNode("3");
        DefaultMutableTreeNode node4 = new DefaultMutableTreeNode("4");
        DefaultMutableTreeNode node5 = new DefaultMutableTreeNode("5");
        node1.add(node2);
        node1.add(node5);
        node2.add(node3);
        node2.add(node4);
        ConcreteTreeModelListener listener = new ConcreteTreeModelListener();
        model.addTreeModelListener(listener);
        model.nodesWereInserted(null, null);
        assertNull(listener.event);
        model.nodesWereInserted(node1, null);
        assertNull(listener.event);
        model.nodesWereInserted(node1, new int[0]);
        assertNull(listener.event);
        model.nodesWereInserted(node1, new int[] { 1, 0 });
        assertNotNull(listener.event);
        assertEquals("inserted", listener.type);
        listener.checkEvent(model, new Object[] { node1 }, new int[] { 1, 0 }, new Object[] {
                node5, node2 });
        listener.reset();
        model.nodesWereInserted(node2, new int[] { 0, 1 });
        assertNotNull(listener.event);
        assertEquals("inserted", listener.type);
        listener.checkEvent(model, new Object[] { node1, node2 }, new int[] { 0, 1 },
                new Object[] { node3, node4 });
        listener.reset();
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.nodesWereRemoved(TreeNode, int[], Object[])'
     */
    public void testNodesWereRemoved() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("1");
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("2");
        DefaultMutableTreeNode node3 = new DefaultMutableTreeNode("3");
        DefaultMutableTreeNode node4 = new DefaultMutableTreeNode("4");
        DefaultMutableTreeNode node5 = new DefaultMutableTreeNode("5");
        node1.add(node2);
        node1.add(node5);
        node2.add(node3);
        node2.add(node4);
        ConcreteTreeModelListener listener = new ConcreteTreeModelListener();
        model.addTreeModelListener(listener);
        model.nodesWereRemoved(null, null, null);
        assertNull(listener.event);
        model.nodesWereRemoved(node1, null, null);
        assertNull(listener.event);
        model.nodesWereRemoved(node1, new int[] { 1, 0 }, new Object[] { node3, node4 });
        assertNotNull(listener.event);
        assertEquals("removed", listener.type);
        listener.checkEvent(model, new Object[] { node1 }, new int[] { 1, 0 }, new Object[] {
                node3, node4 });
        listener.reset();
        model.nodesWereRemoved(node4, new int[] { 1 }, new Object[] { node3, node5 });
        assertNotNull(listener.event);
        assertEquals("removed", listener.type);
        listener.checkEvent(model, new Object[] { node1, node2, node4 }, new int[] { 1 },
                new Object[] { node3, node5 });
        listener.reset();
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.nodesChanged(TreeNode, int[])'
     */
    public void testNodesChanged() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("1");
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("2");
        DefaultMutableTreeNode node3 = new DefaultMutableTreeNode("3");
        DefaultMutableTreeNode node4 = new DefaultMutableTreeNode("4");
        DefaultMutableTreeNode node5 = new DefaultMutableTreeNode("5");
        DefaultMutableTreeNode node6 = new DefaultMutableTreeNode("6");
        node1.add(node2);
        node1.add(node5);
        node2.add(node3);
        node2.add(node4);
        root.add(node6);
        ConcreteTreeModelListener listener = new ConcreteTreeModelListener();
        model.addTreeModelListener(listener);
        model.nodesChanged(null, null);
        assertNull(listener.event);
        model.nodesChanged(node1, null);
        assertNull(listener.event);
        model.nodesChanged(root, null);
        assertNotNull(listener.event);
        assertEquals("changed", listener.type);
        listener.checkEvent(model, new Object[] { root }, null, null);
        listener.reset();
        model.nodesChanged(root, new int[] { 0 });
        assertNotNull(listener.event);
        assertEquals("changed", listener.type);
        listener.checkEvent(model, new Object[] { root }, new int[] { 0 },
                new Object[] { node6 });
        listener.reset();
        model.nodesChanged(node1, new int[0]);
        assertNull(listener.event);
        model.nodesChanged(null, new int[] { 1 });
        assertNull(listener.event);
        model.nodesChanged(node2, new int[] { 1 });
        assertNotNull(listener.event);
        assertEquals("changed", listener.type);
        listener.checkEvent(model, new Object[] { node1, node2 }, new int[] { 1 },
                new Object[] { node4 });
        listener.reset();
        model.nodesChanged(node1, new int[] { 0 });
        assertNotNull(listener.event);
        assertEquals("changed", listener.type);
        listener.checkEvent(model, new Object[] { node1 }, new int[] { 0 },
                new Object[] { node2 });
        listener.reset();
        model.nodesChanged(node1, new int[] { 0, 1 });
        assertNotNull(listener.event);
        assertEquals("changed", listener.type);
        listener.checkEvent(model, new Object[] { node1 }, new int[] { 0, 1 }, new Object[] {
                node2, node5 });
        listener.reset();
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.nodeStructureChanged(TreeNode)'
     */
    public void testNodeStructureChanged() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("1");
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("2");
        DefaultMutableTreeNode node3 = new DefaultMutableTreeNode("3");
        DefaultMutableTreeNode node4 = new DefaultMutableTreeNode("4");
        DefaultMutableTreeNode node5 = new DefaultMutableTreeNode("5");
        node1.add(node2);
        node1.add(node5);
        node2.add(node3);
        node2.add(node4);
        ConcreteTreeModelListener listener = new ConcreteTreeModelListener();
        model.addTreeModelListener(listener);
        model.nodeStructureChanged(null);
        assertNull(listener.event);
        model.setRoot(null);
        listener.reset();
        model.nodeStructureChanged(null);
        assertNull(listener.event);
        model.nodeStructureChanged(node1);
        assertNotNull(listener.event);
        assertEquals("structure", listener.type);
        listener.checkEvent(model, new Object[] { node1 }, null, null);
        listener.reset();
        model.nodeStructureChanged(node2);
        assertNotNull(listener.event);
        listener.checkEvent(model, new Object[] { node1, node2 }, null, null);
        listener.reset();
        model.nodeStructureChanged(node4);
        assertNotNull(listener.event);
        listener.checkEvent(model, new Object[] { node1, node2, node4 }, null, null);
        listener.reset();
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.getPathToRoot(TreeNode)'
     */
    public void testGetPathToRootTreeNode() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("1");
        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("2");
        DefaultMutableTreeNode node3 = root;
        DefaultMutableTreeNode node4 = new DefaultMutableTreeNode("4");
        node1.add(node2);
        node2.add(node3);
        node3.add(node4);
        if (isHarmony()) {
            assertEquals(0, model.getPathToRoot(null).length);
        } else {
            assertNull(model.getPathToRoot(null));
        }
        assertTrue(Arrays.equals(new TreeNode[] { node1 }, model.getPathToRoot(node1)));
        assertTrue(Arrays.equals(new TreeNode[] { node1, node2 }, model.getPathToRoot(node2)));
        assertTrue(Arrays.equals(new TreeNode[] { node3 }, model.getPathToRoot(node3)));
        assertTrue(Arrays.equals(new TreeNode[] { node3, node4 }, model.getPathToRoot(node4)));
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.fireTreeNodesChanged(Object, Object[], int[], Object[])'
     */
    public void testFireTreeNodesChanged() {
        Object source1 = "source1";
        Object[] paths1 = new Object[] { "1", "2" };
        int[] indices1 = new int[] { 100, 200 };
        Object[] children1 = new Object[] { "10", "20" };
        ConcreteTreeModelListener listener1 = new ConcreteTreeModelListener();
        ConcreteTreeModelListener listener2 = new ConcreteTreeModelListener();
        model.addTreeModelListener(listener1);
        model.addTreeModelListener(listener2);
        model.fireTreeNodesChanged(source1, paths1, indices1, children1);
        assertNotNull(listener1.event);
        assertNotNull(listener2.event);
        assertEquals("changed", listener1.type);
        assertEquals("changed", listener2.type);
        listener1.checkEvent(source1, paths1, indices1, children1);
        listener2.checkEvent(source1, paths1, indices1, children1);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.fireTreeNodesInserted(Object, Object[], int[], Object[])'
     */
    public void testFireTreeNodesInserted() {
        Object source1 = "source1";
        Object[] paths1 = new Object[] { "1", "2" };
        int[] indices1 = new int[] { 100, 200 };
        Object[] children1 = new Object[] { "10", "20" };
        ConcreteTreeModelListener listener1 = new ConcreteTreeModelListener();
        ConcreteTreeModelListener listener2 = new ConcreteTreeModelListener();
        model.addTreeModelListener(listener1);
        model.addTreeModelListener(listener2);
        model.fireTreeNodesInserted(source1, paths1, indices1, children1);
        assertNotNull(listener1.event);
        assertNotNull(listener2.event);
        assertEquals("inserted", listener1.type);
        assertEquals("inserted", listener2.type);
        listener1.checkEvent(source1, paths1, indices1, children1);
        listener2.checkEvent(source1, paths1, indices1, children1);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.fireTreeNodesRemoved(Object, Object[], int[], Object[])'
     */
    public void testFireTreeNodesRemoved() {
        Object source1 = "source1";
        Object[] paths1 = new Object[] { "1", "2" };
        int[] indices1 = new int[] { 100, 200 };
        Object[] children1 = new Object[] { "10", "20" };
        ConcreteTreeModelListener listener1 = new ConcreteTreeModelListener();
        ConcreteTreeModelListener listener2 = new ConcreteTreeModelListener();
        model.addTreeModelListener(listener1);
        model.addTreeModelListener(listener2);
        model.fireTreeNodesRemoved(source1, paths1, indices1, children1);
        assertNotNull(listener1.event);
        assertNotNull(listener2.event);
        assertEquals("removed", listener1.type);
        assertEquals("removed", listener2.type);
        listener1.checkEvent(source1, paths1, indices1, children1);
        listener2.checkEvent(source1, paths1, indices1, children1);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeModel.fireTreeStructureChanged(Object, Object[], int[], Object[])'
     */
    public void testFireTreeStructureChanged() {
        Object source1 = "source1";
        Object[] paths1 = new Object[] { "1", "2" };
        int[] indices1 = new int[] { 100, 200 };
        Object[] children1 = new Object[] { "10", "20" };
        ConcreteTreeModelListener listener1 = new ConcreteTreeModelListener();
        ConcreteTreeModelListener listener2 = new ConcreteTreeModelListener();
        model.addTreeModelListener(listener1);
        model.addTreeModelListener(listener2);
        model.fireTreeStructureChanged(source1, paths1, indices1, children1);
        assertNotNull(listener1.event);
        assertNotNull(listener2.event);
        assertEquals("structure", listener1.type);
        assertEquals("structure", listener2.type);
        listener1.checkEvent(source1, paths1, indices1, children1);
        listener2.checkEvent(source1, paths1, indices1, children1);
    }
}
