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
package javax.swing;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JTree.DynamicUtilTreeNode;
import javax.swing.tree.DefaultMutableTreeNode;

public class JTree_DynamicUtilTreeNodeTest extends BasicSwingTestCase {
    private JTree.DynamicUtilTreeNode node;

    public JTree_DynamicUtilTreeNodeTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        node = new DynamicUtilTreeNode("value", null);
    }

    @Override
    protected void tearDown() throws Exception {
        node = null;
    }

    public void testDynamicUtilTreeNode() throws Exception {
        assertNull(node.childValue);
        assertFalse(node.hasChildren);
        assertTrue(node.loadedChildren);
        assertEquals("value", node.getUserObject());
        assertFalse(node.getAllowsChildren());
        assertTrue(node.isLeaf());
        assertEquals(0, node.getChildCount());
        node = new DynamicUtilTreeNode("value", "children value");
        assertEquals(node.childValue, "children value");
        assertTrue(node.loadedChildren);
        node = new DynamicUtilTreeNode("value", new Object[] { "1" });
        assertFalse(node.loadedChildren);
        node = new DynamicUtilTreeNode("value", new Object[] {});
        assertFalse(node.loadedChildren);
    }

    public void testCreateChildren() throws Exception {
        DynamicUtilTreeNode.createChildren(null, "any");
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DynamicUtilTreeNode.createChildren(root, "any");
        assertEquals(0, root.getChildCount());
        assertTrue(root.isLeaf());
        int[] privitiveArrayChildren = new int[] { 1, 2, 3 };
        DynamicUtilTreeNode.createChildren(root, privitiveArrayChildren);
        assertEquals(0, root.getChildCount());
        assertTrue(root.isLeaf());
        assertTrue(root.getAllowsChildren());
        Object[] objectArrayChildren = new Object[] { "a", "b", "c" };
        DynamicUtilTreeNode.createChildren(root, objectArrayChildren);
        assertEquals(3, root.getChildCount());
        assertTrue(root.getChildAt(0) instanceof JTree.DynamicUtilTreeNode);
        assertFalse(root.isLeaf());
        assertEquals("a", ((DefaultMutableTreeNode) root.getChildAt(0)).getUserObject());
        Vector<String> vectorChildren = new Vector<String>();
        vectorChildren.add("1");
        vectorChildren.add("2");
        DynamicUtilTreeNode.createChildren(root, vectorChildren);
        assertEquals(5, root.getChildCount());
        assertTrue(root.getChildAt(4) instanceof JTree.DynamicUtilTreeNode);
        assertTrue(root.getChildAt(4).isLeaf());
        assertFalse(root.getChildAt(4).getAllowsChildren());
        assertEquals("1", ((DefaultMutableTreeNode) root.getChildAt(3)).getUserObject());
        Hashtable<String, String> hashChildren = new Hashtable<String, String>();
        hashChildren.put("key1", "value1");
        hashChildren.put("key2", "value2");
        DynamicUtilTreeNode.createChildren(root, hashChildren);
        assertEquals(7, root.getChildCount());
        assertTrue(root.getChildAt(5) instanceof JTree.DynamicUtilTreeNode);
        assertEquals(hashChildren.keys().nextElement(), ((DefaultMutableTreeNode) root
                .getChildAt(5)).getUserObject());
        assertEquals(0, root.getChildAt(5).getChildCount());
        root = new DefaultMutableTreeNode();
        Hashtable<String, String> subSubChildren = new Hashtable<String, String>();
        subSubChildren.put("221", "any");
        subSubChildren.put("222", "any");
        Vector<Serializable> subChildren = new Vector<Serializable>();
        subChildren.add("21");
        subChildren.add(subSubChildren);
        subChildren.add("23");
        Object[] complexChildren = new Object[] { "1", subChildren, "3" };
        DynamicUtilTreeNode.createChildren(root, complexChildren);
        assertEquals(3, root.getChildCount());
        DynamicUtilTreeNode child1 = (DynamicUtilTreeNode) root.getChildAt(0);
        assertFalse(child1.getAllowsChildren());
        assertEquals(0, child1.getChildCount());
        assertEquals("1", child1.getUserObject());
        assertEquals("1", child1.childValue);
        assertTrue(child1.loadedChildren);
        DynamicUtilTreeNode child2 = (DynamicUtilTreeNode) root.getChildAt(1);
        assertTrue(child2.getAllowsChildren());
        assertEquals(3, child2.getChildCount());
        assertEquals(subChildren, child2.getUserObject());
        assertSame(subChildren, child2.childValue);
        assertTrue(child2.loadedChildren);
        assertEquals(0, root.getChildAt(2).getChildCount());
        assertEquals("3", ((DefaultMutableTreeNode) root.getChildAt(2)).getUserObject());
        assertEquals(3, child2.getChildCount());
        assertEquals(0, child2.getChildAt(0).getChildCount());
        assertEquals(2, child2.getChildAt(1).getChildCount());
        assertEquals(0, child2.getChildAt(2).getChildCount());
    }

    public void testIsLeaf() throws Exception {
        DynamicUtilTreeNode node = new DynamicUtilTreeNode("value", null);
        assertFalse(node.getAllowsChildren());
        assertTrue(node.isLeaf());
        node.setAllowsChildren(true);
        assertFalse(node.isLeaf());
    }

    public void testGetChildCount() throws Exception {
        DynamicUtilTreeNode node = new DynamicUtilTreeNode("value", new Object[] { "1", "2" });
        assertFalse(node.loadedChildren);
        assertEquals(2, node.getChildCount());
        assertTrue(node.loadedChildren);
    }

    public void testGetChildAt() throws Exception {
        DynamicUtilTreeNode node = new DynamicUtilTreeNode("value", new Object[] { "1", "2" });
        assertFalse(node.loadedChildren);
        assertEquals("1", ((DynamicUtilTreeNode) node.getChildAt(0)).getUserObject());
        assertTrue(node.loadedChildren);
    }

    public void testChildren() throws Exception {
        DynamicUtilTreeNode node = new DynamicUtilTreeNode("value", new Object[] { "1", "2" });
        assertFalse(node.loadedChildren);
        Enumeration<?> children = node.children();
        assertTrue(node.loadedChildren);
        assertEquals("1", ((DefaultMutableTreeNode) children.nextElement()).getUserObject());
        assertEquals("2", ((DefaultMutableTreeNode) children.nextElement()).getUserObject());
    }

    public void testLoadChildren() throws Exception {
        Object[] children = new Object[] { "1", "2" };
        DynamicUtilTreeNode node = new DynamicUtilTreeNode("value", children);
        assertFalse(node.loadedChildren);
        assertEquals(children, node.childValue);
        assertEquals("value", node.getUserObject());
        node.loadChildren();
        assertTrue(node.loadedChildren);
        assertEquals(2, node.getChildCount());
        node.childValue = "any";
        node.loadChildren();
        assertEquals(2, node.getChildCount());
        assertEquals("value", node.getUserObject());
        node.childValue = new Object[] { "3", "4", "5" };
        node.loadChildren();
        assertTrue(node.loadedChildren);
        assertEquals(5, node.getChildCount());
        assertEquals("5", ((DefaultMutableTreeNode) node.getChildAt(4)).getUserObject());
        node.childValue = new Object[] { "6" };
        assertEquals(5, node.getChildCount());
        node.loadedChildren = false;
        assertEquals(6, node.getChildCount());
    }
}
