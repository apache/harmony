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
 * @author Anton Avtamonov, Alexander T. Simbirtsev
 */
package javax.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
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
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.RowMapper;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.VariableHeightLayoutCache;

@SuppressWarnings("serial")
public class JTreeTest extends SwingTestCase {
    protected JTree tree;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tree = new JTree();
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public FontMetrics getFontMetrics(Font f) {
                return JTreeTest.this.getFontMetrics(f, 10, 10);
            }
        });
        propertyChangeController = new PropertyChangeController();
        tree.addPropertyChangeListener(propertyChangeController);
    }

    @Override
    protected void tearDown() throws Exception {
        tree = null;
        propertyChangeController = null;
        super.tearDown();
    }

    public void testTreeSelectionRedirector() throws Exception {
        assertNull(tree.selectionRedirector);
        DefaultTreeSelectionModel model = (DefaultTreeSelectionModel) tree.getSelectionModel();
        assertFalse(hasListener(model.getTreeSelectionListeners(),
                JTree.TreeSelectionRedirector.class));
        final Marker marker = new Marker();
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(final TreeSelectionEvent e) {
                marker.setAuxiliary(e);
            }
        });
        assertNotNull(tree.selectionRedirector);
        assertTrue(hasListener(model.getTreeSelectionListeners(),
                JTree.TreeSelectionRedirector.class));
        TreePath path = new TreePath("root");
        TreePath oldLead = new TreePath("old_lead");
        TreePath newLead = new TreePath("new_lead");
        tree.selectionRedirector.valueChanged(new TreeSelectionEvent("any_source", path, true,
                oldLead, newLead));
        assertNotNull(marker.getAuxiliary());
        TreeSelectionEvent redirectedEvent = (TreeSelectionEvent) marker.getAuxiliary();
        assertSame(tree, redirectedEvent.getSource());
        assertSame(path, redirectedEvent.getPath());
        assertTrue(redirectedEvent.isAddedPath());
        assertSame(oldLead, redirectedEvent.getOldLeadSelectionPath());
        assertSame(newLead, redirectedEvent.getNewLeadSelectionPath());
        DefaultTreeSelectionModel newModel = new DefaultTreeSelectionModel();
        tree.setSelectionModel(newModel);
        assertTrue(hasListener(newModel.getTreeSelectionListeners(),
                JTree.TreeSelectionRedirector.class));
        tree.setSelectionModel(null);
        assertTrue(hasListener(((DefaultTreeSelectionModel) tree.getSelectionModel())
                .getTreeSelectionListeners(), JTree.TreeSelectionRedirector.class));
    }

    public void testJTree() {
        assertFalse(tree.editable);
        assertFalse(tree.invokesStopCellEditing);
        assertFalse(tree.largeModel);
        assertTrue(tree.rootVisible);
        assertEquals(0, tree.rowHeight);
        assertTrue(tree.scrollsOnExpand);
        assertFalse(tree.showsRootHandles);
        assertEquals(2, tree.toggleClickCount);
        assertEquals(20, tree.visibleRowCount);
        assertNotNull(tree.selectionModel);
        assertNull(tree.selectionRedirector);
        assertNotNull(tree.cellRenderer);
        assertNull(tree.cellEditor);
        assertNotNull(tree.treeModel);
        assertNotNull(tree.treeModelListener);
    }

    public void testJTreeTreeModel() {
        DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode("root"));
        tree = new JTree(model);
        assertFalse(tree.editable);
        assertFalse(tree.invokesStopCellEditing);
        assertFalse(tree.largeModel);
        assertTrue(tree.rootVisible);
        assertEquals(0, tree.rowHeight);
        assertTrue(tree.scrollsOnExpand);
        assertFalse(tree.showsRootHandles);
        assertEquals(2, tree.toggleClickCount);
        assertEquals(20, tree.visibleRowCount);
        assertNotNull(tree.selectionModel);
        assertNull(tree.selectionRedirector);
        assertEquals(model, tree.treeModel);
        assertFalse(((DefaultTreeModel) tree.treeModel).asksAllowsChildren());
        assertNotNull(tree.treeModelListener);
        tree = new JTree((TreeModel) null);
        assertFalse(tree.editable);
        assertFalse(tree.invokesStopCellEditing);
        assertFalse(tree.largeModel);
        assertTrue(tree.rootVisible);
        assertEquals(0, tree.rowHeight);
        assertTrue(tree.scrollsOnExpand);
        assertFalse(tree.showsRootHandles);
        assertEquals(2, tree.toggleClickCount);
        assertEquals(20, tree.visibleRowCount);
        assertNotNull(tree.selectionModel);
        assertNull(tree.selectionRedirector);
        assertNull(tree.treeModel);
        if (isHarmony()) {
            assertNotNull(tree.treeModelListener);
        } else {
            assertNull(tree.treeModelListener);
        }
    }

    public void testJTreeTreeNodeBoolean() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        tree = new JTree(root, true);
        assertFalse(tree.editable);
        assertFalse(tree.invokesStopCellEditing);
        assertFalse(tree.largeModel);
        assertTrue(tree.rootVisible);
        assertEquals(0, tree.rowHeight);
        assertTrue(tree.scrollsOnExpand);
        assertFalse(tree.showsRootHandles);
        assertEquals(2, tree.toggleClickCount);
        assertEquals(20, tree.visibleRowCount);
        assertNotNull(tree.selectionModel);
        assertNull(tree.selectionRedirector);
        assertNotNull(tree.treeModel);
        assertEquals(root, tree.treeModel.getRoot());
        assertTrue(((DefaultTreeModel) tree.treeModel).asksAllowsChildren());
        assertNotNull(tree.treeModelListener);
        tree = new JTree(root, false);
        assertFalse(tree.editable);
        assertFalse(tree.invokesStopCellEditing);
        assertFalse(tree.largeModel);
        assertTrue(tree.rootVisible);
        assertEquals(0, tree.rowHeight);
        assertTrue(tree.scrollsOnExpand);
        assertFalse(tree.showsRootHandles);
        assertEquals(2, tree.toggleClickCount);
        assertEquals(20, tree.visibleRowCount);
        assertNotNull(tree.selectionModel);
        assertNull(tree.selectionRedirector);
        assertNotNull(tree.treeModel);
        assertEquals(root, tree.treeModel.getRoot());
        assertFalse(((DefaultTreeModel) tree.treeModel).asksAllowsChildren());
        assertNotNull(tree.treeModelListener);
        tree = new JTree(null, false);
        assertFalse(tree.editable);
        assertFalse(tree.invokesStopCellEditing);
        assertFalse(tree.largeModel);
        assertTrue(tree.rootVisible);
        assertEquals(0, tree.rowHeight);
        assertTrue(tree.scrollsOnExpand);
        assertFalse(tree.showsRootHandles);
        assertEquals(2, tree.toggleClickCount);
        assertEquals(20, tree.visibleRowCount);
        assertNotNull(tree.selectionModel);
        assertNull(tree.selectionRedirector);
        assertNotNull(tree.treeModel);
        assertNull(tree.treeModel.getRoot());
        assertFalse(((DefaultTreeModel) tree.treeModel).asksAllowsChildren());
        assertNotNull(tree.treeModelListener);
    }

    public void testJTreeTreeNode() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        tree = new JTree(root);
        assertFalse(tree.editable);
        assertFalse(tree.invokesStopCellEditing);
        assertFalse(tree.largeModel);
        assertTrue(tree.rootVisible);
        assertEquals(0, tree.rowHeight);
        assertTrue(tree.scrollsOnExpand);
        assertFalse(tree.showsRootHandles);
        assertEquals(2, tree.toggleClickCount);
        assertEquals(20, tree.visibleRowCount);
        assertNotNull(tree.selectionModel);
        assertNull(tree.selectionRedirector);
        assertNotNull(tree.treeModel);
        assertEquals(root, tree.treeModel.getRoot());
        assertFalse(((DefaultTreeModel) tree.treeModel).asksAllowsChildren());
        assertNotNull(tree.treeModelListener);
        tree = new JTree((TreeNode) null);
        assertFalse(tree.editable);
        assertFalse(tree.invokesStopCellEditing);
        assertFalse(tree.largeModel);
        assertTrue(tree.rootVisible);
        assertEquals(0, tree.rowHeight);
        assertTrue(tree.scrollsOnExpand);
        assertFalse(tree.showsRootHandles);
        assertEquals(2, tree.toggleClickCount);
        assertEquals(20, tree.visibleRowCount);
        assertNotNull(tree.selectionModel);
        assertNull(tree.selectionRedirector);
        assertNotNull(tree.treeModel);
        assertNull(tree.treeModel.getRoot());
        assertFalse(((DefaultTreeModel) tree.treeModel).asksAllowsChildren());
        assertNotNull(tree.treeModelListener);
    }

    public void testJTreeObjectArray() {
        Object[] nodes = new Object[] { "node1", "node2", "node3" };
        tree = new JTree(nodes);
        assertFalse(tree.editable);
        assertFalse(tree.invokesStopCellEditing);
        assertFalse(tree.largeModel);
        assertFalse(tree.rootVisible);
        assertEquals(0, tree.rowHeight);
        assertTrue(tree.scrollsOnExpand);
        assertTrue(tree.showsRootHandles);
        assertEquals(2, tree.toggleClickCount);
        assertEquals(20, tree.visibleRowCount);
        assertNotNull(tree.selectionModel);
        assertNull(tree.selectionRedirector);
        assertNotNull(tree.treeModel);
        assertNotNull(tree.treeModel.getRoot());
        assertEquals(3, tree.treeModel.getChildCount(tree.treeModel.getRoot()));
        assertFalse(((DefaultTreeModel) tree.treeModel).asksAllowsChildren());
        assertNotNull(tree.treeModelListener);
        tree = new JTree((Object[]) null);
        assertFalse(tree.editable);
        assertFalse(tree.invokesStopCellEditing);
        assertFalse(tree.largeModel);
        assertFalse(tree.rootVisible);
        assertEquals(0, tree.rowHeight);
        assertTrue(tree.scrollsOnExpand);
        assertTrue(tree.showsRootHandles);
        assertEquals(2, tree.toggleClickCount);
        assertEquals(20, tree.visibleRowCount);
        assertNotNull(tree.selectionModel);
        assertNull(tree.selectionRedirector);
        assertNotNull(tree.treeModel);
        assertNotNull(tree.treeModel.getRoot());
        assertEquals(0, tree.treeModel.getChildCount(tree.treeModel.getRoot()));
        assertFalse(((DefaultTreeModel) tree.treeModel).asksAllowsChildren());
        assertNotNull(tree.treeModelListener);
    }

    public void testJTreeVector() {
        Vector<String> nodes = new Vector<String>();
        nodes.add("node1");
        nodes.add("node2");
        nodes.add("node3");
        tree = new JTree(nodes);
        assertFalse(tree.editable);
        assertFalse(tree.invokesStopCellEditing);
        assertFalse(tree.largeModel);
        assertFalse(tree.rootVisible);
        assertEquals(0, tree.rowHeight);
        assertTrue(tree.scrollsOnExpand);
        assertTrue(tree.showsRootHandles);
        assertEquals(2, tree.toggleClickCount);
        assertEquals(20, tree.visibleRowCount);
        assertNotNull(tree.selectionModel);
        assertNull(tree.selectionRedirector);
        assertNotNull(tree.treeModel);
        assertNotNull(tree.treeModel.getRoot());
        assertEquals(3, tree.treeModel.getChildCount(tree.treeModel.getRoot()));
        assertFalse(((DefaultTreeModel) tree.treeModel).asksAllowsChildren());
        assertNotNull(tree.treeModelListener);
        tree = new JTree((Vector) null);
        assertFalse(tree.editable);
        assertFalse(tree.invokesStopCellEditing);
        assertFalse(tree.largeModel);
        assertFalse(tree.rootVisible);
        assertEquals(0, tree.rowHeight);
        assertTrue(tree.scrollsOnExpand);
        assertTrue(tree.showsRootHandles);
        assertEquals(2, tree.toggleClickCount);
        assertEquals(20, tree.visibleRowCount);
        assertNotNull(tree.selectionModel);
        assertNull(tree.selectionRedirector);
        assertNotNull(tree.treeModel);
        assertNotNull(tree.treeModel.getRoot());
        assertEquals(0, tree.treeModel.getChildCount(tree.treeModel.getRoot()));
        assertFalse(((DefaultTreeModel) tree.treeModel).asksAllowsChildren());
        assertNotNull(tree.treeModelListener);
    }

    public void testJTreeHashtable() {
        Hashtable<String, String> nodes = new Hashtable<String, String>();
        nodes.put("node1", "node1");
        nodes.put("node2", "node2");
        nodes.put("node3", "node3");
        tree = new JTree(nodes);
        assertFalse(tree.editable);
        assertFalse(tree.invokesStopCellEditing);
        assertFalse(tree.largeModel);
        assertFalse(tree.rootVisible);
        assertEquals(0, tree.rowHeight);
        assertTrue(tree.scrollsOnExpand);
        assertTrue(tree.showsRootHandles);
        assertEquals(2, tree.toggleClickCount);
        assertEquals(20, tree.visibleRowCount);
        assertNotNull(tree.selectionModel);
        assertNull(tree.selectionRedirector);
        assertNotNull(tree.treeModel);
        assertNotNull(tree.treeModel.getRoot());
        assertEquals(3, tree.treeModel.getChildCount(tree.treeModel.getRoot()));
        assertFalse(((DefaultTreeModel) tree.treeModel).asksAllowsChildren());
        assertNotNull(tree.treeModelListener);
        tree = new JTree((Hashtable) null);
        assertFalse(tree.editable);
        assertFalse(tree.invokesStopCellEditing);
        assertFalse(tree.largeModel);
        assertFalse(tree.rootVisible);
        assertEquals(0, tree.rowHeight);
        assertTrue(tree.scrollsOnExpand);
        assertTrue(tree.showsRootHandles);
        assertEquals(2, tree.toggleClickCount);
        assertEquals(20, tree.visibleRowCount);
        assertNotNull(tree.selectionModel);
        assertNull(tree.selectionRedirector);
        assertNotNull(tree.treeModel);
        assertNotNull(tree.treeModel.getRoot());
        assertEquals(0, tree.treeModel.getChildCount(tree.treeModel.getRoot()));
        assertFalse(((DefaultTreeModel) tree.treeModel).asksAllowsChildren());
        assertNotNull(tree.treeModelListener);
    }

    public void testGetAccessibleContext() {
        assertNull(tree.accessibleContext);
        assertNotNull(tree.getAccessibleContext());
        assertSame(tree.accessibleContext, tree.getAccessibleContext());
    }

    public void testGetToolTipTextMouseEvent() {
        tree.setToolTipText("tip");
        tree.cellRenderer = new DefaultTreeCellRenderer() {
            @Override
            public String getToolTipText() {
                return "renderer tip";
            }
        };
        assertEquals("renderer tip", tree.getToolTipText(new MouseEvent(tree, 0, 0, 0, 0, 0, 0,
                false)));
        assertNull(tree.getToolTipText(null));
    }

    public void testGetUIClassID() {
        assertEquals("TreeUI", tree.getUIClassID());
    }

    public void testSetAddRemoveSelectionInterval() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode("child111");
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("child2");
        DefaultMutableTreeNode child21 = new DefaultMutableTreeNode("child21");
        root.add(child1);
        child1.add(child11);
        child11.add(child111);
        root.add(child2);
        child2.add(child21);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        TreePath path2 = rootPath.pathByAddingChild(child2);
        assertEquals(0, tree.getSelectionCount());
        tree.addSelectionInterval(0, 0);
        assertEquals(1, tree.getSelectionCount());
        tree.addSelectionInterval(1, 1);
        assertEquals(2, tree.getSelectionCount());
        tree.addSelectionInterval(-20, 20);
        assertEqualsIgnoreOrder(new TreePath[] { rootPath, path1, path2 }, tree
                .getSelectionPaths());
        tree.expandPath(path1);
        assertEqualsIgnoreOrder(new TreePath[] { rootPath, path1, path2 }, tree
                .getSelectionPaths());
        tree.addSelectionInterval(1, 2);
        assertEqualsIgnoreOrder(new TreePath[] { rootPath, path1, path2, path11 }, tree
                .getSelectionPaths());
        tree.removeSelectionInterval(0, 1);
        assertEqualsIgnoreOrder(new TreePath[] { path11, path2 }, tree.getSelectionPaths());
        tree.removeSelectionInterval(2, 2);
        assertEqualsIgnoreOrder(new TreePath[] { path2 }, tree.getSelectionPaths());
        tree.removeSelectionInterval(0, 2);
        assertEqualsIgnoreOrder(new TreePath[] { path2 }, tree.getSelectionPaths());
        tree.removeSelectionInterval(-1, 10);
        assertNull(tree.getSelectionPaths());
        tree.setSelectionInterval(3, 1);
        assertEqualsIgnoreOrder(new TreePath[] { path1, path11, path2 }, tree
                .getSelectionPaths());
        tree.setSelectionInterval(2, 2);
        assertEqualsIgnoreOrder(new TreePath[] { path11 }, tree.getSelectionPaths());
    }

    public void testAddRemoveSelectionPathPathsRowRows() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode("child111");
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("child2");
        DefaultMutableTreeNode child21 = new DefaultMutableTreeNode("child21");
        root.add(child1);
        child1.add(child11);
        child11.add(child111);
        root.add(child2);
        child2.add(child21);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        TreePath path111 = path11.pathByAddingChild(child111);
        TreePath path2 = rootPath.pathByAddingChild(child2);
        TreePath path21 = path2.pathByAddingChild(child21);
        assertEquals(0, tree.getSelectionCount());
        tree.addSelectionPath(path1);
        assertEqualsIgnoreOrder(new TreePath[] { path1 }, tree.getSelectionPaths());
        tree.addSelectionPaths(new TreePath[] { rootPath, path11 });
        assertEqualsIgnoreOrder(new TreePath[] { path1, rootPath, path11 }, tree
                .getSelectionPaths());
        TreePath unexistedPath = rootPath.pathByAddingChild(new DefaultMutableTreeNode(
                "unexisted"));
        tree.addSelectionPath(unexistedPath);
        assertEqualsIgnoreOrder(new TreePath[] { path1, rootPath, path11, unexistedPath }, tree
                .getSelectionPaths());
        tree.removeSelectionPaths(new TreePath[] { rootPath, unexistedPath });
        assertEqualsIgnoreOrder(new TreePath[] { path11, path1 }, tree.getSelectionPaths());
        tree.removeSelectionPath(path11);
        assertEqualsIgnoreOrder(new TreePath[] { path1 }, tree.getSelectionPaths());
        tree.expandPath(path2);
        tree.addSelectionRows(new int[] { 3, 4 });
        assertEqualsIgnoreOrder(new TreePath[] { path1, path2, path21 }, tree
                .getSelectionPaths());
        tree.expandPath(path11);
        tree.addSelectionRows(new int[] { 0, 3 });
        assertEqualsIgnoreOrder(new TreePath[] { path1, path2, path21, rootPath, path111 },
                tree.getSelectionPaths());
        tree.removeSelectionRows(new int[] { 0, 2 });
        assertEqualsIgnoreOrder(new TreePath[] { path111, path2, path1, path21 }, tree
                .getSelectionPaths());
        tree.removeSelectionRow(3);
        assertEqualsIgnoreOrder(new TreePath[] { path2, path1, path21 }, tree
                .getSelectionPaths());
    }

    public void testAddGetRemoveTreeExpansionListener() {
        class ConcreteTreeExpansionListener implements TreeExpansionListener {
            public void treeCollapsed(TreeExpansionEvent e) {
            }

            public void treeExpanded(TreeExpansionEvent e) {
            }
        }
        ;
        TreeExpansionListener TreeExpansionListener1 = new ConcreteTreeExpansionListener();
        TreeExpansionListener TreeExpansionListener2 = new ConcreteTreeExpansionListener();
        TreeExpansionListener TreeExpansionListener3 = new ConcreteTreeExpansionListener();
        EventListener[] listenersArray = null;
        listenersArray = tree.getTreeExpansionListeners();
        int initialValue = listenersArray.length;
        tree.addTreeExpansionListener(TreeExpansionListener1);
        tree.addTreeExpansionListener(TreeExpansionListener2);
        tree.addTreeExpansionListener(TreeExpansionListener2);
        listenersArray = tree.getTreeExpansionListeners();
        assertEquals(initialValue + 3, listenersArray.length);
        tree.removeTreeExpansionListener(TreeExpansionListener1);
        tree.addTreeExpansionListener(TreeExpansionListener3);
        tree.addTreeExpansionListener(TreeExpansionListener3);
        listenersArray = tree.getTreeExpansionListeners();
        assertEquals(initialValue + 4, listenersArray.length);
        tree.removeTreeExpansionListener(TreeExpansionListener3);
        tree.removeTreeExpansionListener(TreeExpansionListener3);
        listenersArray = tree.getTreeExpansionListeners();
        assertEquals(initialValue + 2, listenersArray.length);
        tree.removeTreeExpansionListener(TreeExpansionListener2);
        tree.removeTreeExpansionListener(TreeExpansionListener2);
        listenersArray = tree.getTreeExpansionListeners();
        assertEquals(initialValue, listenersArray.length);
    }

    public void testAddGetRemoveTreeSelectionListener() {
        class ConcreteTreeSelectionListener implements TreeSelectionListener {
            public void valueChanged(TreeSelectionEvent e) {
            }
        }
        ;
        TreeSelectionListener TreeSelectionListener1 = new ConcreteTreeSelectionListener();
        TreeSelectionListener TreeSelectionListener2 = new ConcreteTreeSelectionListener();
        TreeSelectionListener TreeSelectionListener3 = new ConcreteTreeSelectionListener();
        EventListener[] listenersArray = null;
        listenersArray = tree.getTreeSelectionListeners();
        int initialValue = listenersArray.length;
        tree.addTreeSelectionListener(TreeSelectionListener1);
        tree.addTreeSelectionListener(TreeSelectionListener2);
        tree.addTreeSelectionListener(TreeSelectionListener2);
        listenersArray = tree.getTreeSelectionListeners();
        assertEquals(initialValue + 3, listenersArray.length);
        tree.removeTreeSelectionListener(TreeSelectionListener1);
        tree.addTreeSelectionListener(TreeSelectionListener3);
        tree.addTreeSelectionListener(TreeSelectionListener3);
        listenersArray = tree.getTreeSelectionListeners();
        assertEquals(initialValue + 4, listenersArray.length);
        tree.removeTreeSelectionListener(TreeSelectionListener3);
        tree.removeTreeSelectionListener(TreeSelectionListener3);
        listenersArray = tree.getTreeSelectionListeners();
        assertEquals(initialValue + 2, listenersArray.length);
        tree.removeTreeSelectionListener(TreeSelectionListener2);
        tree.removeTreeSelectionListener(TreeSelectionListener2);
        listenersArray = tree.getTreeSelectionListeners();
        assertEquals(initialValue, listenersArray.length);
    }

    public void testAddGetRemoveTreeWillExpandListener() {
        class ConcreteTreeWillExpandListener implements TreeWillExpandListener {
            public void treeWillCollapse(TreeExpansionEvent e) throws ExpandVetoException {
            }

            public void treeWillExpand(TreeExpansionEvent e) throws ExpandVetoException {
            }
        }
        ;
        TreeWillExpandListener TreeWillExpandListener1 = new ConcreteTreeWillExpandListener();
        TreeWillExpandListener TreeWillExpandListener2 = new ConcreteTreeWillExpandListener();
        TreeWillExpandListener TreeWillExpandListener3 = new ConcreteTreeWillExpandListener();
        EventListener[] listenersArray = null;
        listenersArray = tree.getTreeWillExpandListeners();
        int initialValue = listenersArray.length;
        tree.addTreeWillExpandListener(TreeWillExpandListener1);
        tree.addTreeWillExpandListener(TreeWillExpandListener2);
        tree.addTreeWillExpandListener(TreeWillExpandListener2);
        listenersArray = tree.getTreeWillExpandListeners();
        assertEquals(initialValue + 3, listenersArray.length);
        tree.removeTreeWillExpandListener(TreeWillExpandListener1);
        tree.addTreeWillExpandListener(TreeWillExpandListener3);
        tree.addTreeWillExpandListener(TreeWillExpandListener3);
        listenersArray = tree.getTreeWillExpandListeners();
        assertEquals(initialValue + 4, listenersArray.length);
        tree.removeTreeWillExpandListener(TreeWillExpandListener3);
        tree.removeTreeWillExpandListener(TreeWillExpandListener3);
        listenersArray = tree.getTreeWillExpandListeners();
        assertEquals(initialValue + 2, listenersArray.length);
        tree.removeTreeWillExpandListener(TreeWillExpandListener2);
        tree.removeTreeWillExpandListener(TreeWillExpandListener2);
        listenersArray = tree.getTreeWillExpandListeners();
        assertEquals(initialValue, listenersArray.length);
    }

    public void testClearSelection() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        root.add(child1);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        assertEquals(0, tree.getSelectionCount());
        assertEquals(0, tree.getSelectionModel().getSelectionCount());
        tree.addSelectionPaths(new TreePath[] { rootPath, path1 });
        assertEquals(2, tree.getSelectionCount());
        assertEquals(2, tree.getSelectionModel().getSelectionCount());
        tree.clearSelection();
        assertEquals(0, tree.getSelectionCount());
        assertEquals(0, tree.getSelectionModel().getSelectionCount());
    }

    public void testClearToggledPaths() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode("child111");
        root.add(child1);
        child1.add(child11);
        child11.add(child111);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        assertTrue(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        tree.clearToggledPaths();
        assertFalse(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        tree.expandPath(path1);
        tree.clearToggledPaths();
        assertFalse(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
    }

    public void testConvertValueToText() {
        assertEquals("", tree.convertValueToText(null, false, false, true, 0, false));
        assertEquals("any", tree.convertValueToText("any", false, false, true, 0, false));
        assertEquals("5", tree.convertValueToText(new Integer("5"), false, false, true, 0,
                false));
    }

    public void testCreateTreeModel() {
        Object obj1 = new Object[] { "node1", "node2", "node3" };
        Vector<String> obj2 = new Vector<String>();
        obj2.add("node1");
        obj2.add("node2");
        obj2.add("node3");
        Hashtable<String, String> obj3 = new Hashtable<String, String>();
        obj3.put("node1", "value1");
        obj3.put("node2", "value3");
        obj3.put("node3", "value3");
        Object obj4 = "object";
        List<String> obj5 = new ArrayList<String>();
        obj5.add("node1");
        obj5.add("node2");
        obj5.add("node3");
        Object obj6 = new int[] { 1, 2, 3 };
        Vector<Vector<String>> obj7 = new Vector<Vector<String>>();
        Vector<String> obj71 = new Vector<String>();
        obj71.add("node1");
        obj71.add("node2");
        obj7.add(obj71);
        TreeModel model = JTree.createTreeModel(obj1);
        assertTrue(model instanceof DefaultTreeModel);
        assertTrue(model.getRoot() instanceof DefaultMutableTreeNode);
        assertEquals("root", ((DefaultMutableTreeNode) model.getRoot()).getUserObject());
        assertEquals(3, model.getChildCount(model.getRoot()));
        assertTrue(model.getChild(model.getRoot(), 0) instanceof JTree.DynamicUtilTreeNode);
        model = JTree.createTreeModel(obj2);
        assertTrue(model instanceof DefaultTreeModel);
        assertTrue(model.getRoot() instanceof DefaultMutableTreeNode);
        assertEquals("root", ((DefaultMutableTreeNode) model.getRoot()).getUserObject());
        assertEquals(3, model.getChildCount(model.getRoot()));
        assertTrue(model.getChild(model.getRoot(), 0) instanceof JTree.DynamicUtilTreeNode);
        model = JTree.createTreeModel(obj3);
        assertTrue(model instanceof DefaultTreeModel);
        assertTrue(model.getRoot() instanceof DefaultMutableTreeNode);
        assertEquals("root", ((DefaultMutableTreeNode) model.getRoot()).getUserObject());
        assertEquals(3, model.getChildCount(model.getRoot()));
        assertTrue(model.getChild(model.getRoot(), 0) instanceof JTree.DynamicUtilTreeNode);
        assertTrue(((String) ((DefaultMutableTreeNode) ((DefaultMutableTreeNode) model
                .getRoot()).getChildAt(0)).getUserObject()).startsWith("node"));
        model = JTree.createTreeModel(obj4);
        assertTrue(model instanceof DefaultTreeModel);
        assertTrue(model.getRoot() instanceof DefaultMutableTreeNode);
        assertEquals("root", ((DefaultMutableTreeNode) model.getRoot()).getUserObject());
        assertEquals(0, model.getChildCount(model.getRoot()));
        model = JTree.createTreeModel(obj5);
        assertTrue(model instanceof DefaultTreeModel);
        assertTrue(model.getRoot() instanceof DefaultMutableTreeNode);
        assertEquals("root", ((DefaultMutableTreeNode) model.getRoot()).getUserObject());
        assertEquals(0, model.getChildCount(model.getRoot()));
        model = JTree.createTreeModel(obj6);
        assertTrue(model instanceof DefaultTreeModel);
        assertTrue(model.getRoot() instanceof DefaultMutableTreeNode);
        assertEquals("root", ((DefaultMutableTreeNode) model.getRoot()).getUserObject());
        assertEquals(0, model.getChildCount(model.getRoot()));
        model = JTree.createTreeModel(obj7);
        assertTrue(model instanceof DefaultTreeModel);
        assertTrue(model.getRoot() instanceof DefaultMutableTreeNode);
        assertEquals("root", ((DefaultMutableTreeNode) model.getRoot()).getUserObject());
        assertEquals(1, model.getChildCount(model.getRoot()));
        DefaultMutableTreeNode child = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) model
                .getRoot()).getChildAt(0);
        assertTrue(child instanceof JTree.DynamicUtilTreeNode);
        assertEquals(obj71, child.getUserObject());
        assertEquals(2, child.getChildCount());
        assertTrue(model.getChild(child, 0) instanceof JTree.DynamicUtilTreeNode);
        model = JTree.createTreeModel(null);
        assertTrue(model instanceof DefaultTreeModel);
        assertTrue(model.getRoot() instanceof DefaultMutableTreeNode);
        assertEquals("root", ((DefaultMutableTreeNode) model.getRoot()).getUserObject());
        assertEquals(0, model.getChildCount(model.getRoot()));
    }

    public void testCreateTreeModelListener() {
        TreeModelListener listener1 = tree.createTreeModelListener();
        TreeModelListener listener2 = tree.createTreeModelListener();
        assertTrue(listener1 instanceof JTree.TreeModelHandler);
        assertNotSame(listener1, listener2);
    }

    public void testTreeModelHandler() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode();
        root.add(child1);
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode();
        child1.add(child11);
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode();
        child11.add(child111);
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode();
        root.add(child2);
        DefaultMutableTreeNode child21 = new DefaultMutableTreeNode();
        child2.add(child21);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        TreePath path111 = path11.pathByAddingChild(child111);
        TreePath path2 = rootPath.pathByAddingChild(child2);
        TreePath path21 = path1.pathByAddingChild(child21);
        tree.setExpandedState(path11, true);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        assertFalse(tree.isExpanded(path2));
        assertFalse(tree.isExpanded(path21));
        tree.treeModelListener.treeNodesChanged(new TreeModelEvent(tree, path1));
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        assertFalse(tree.isExpanded(path2));
        assertFalse(tree.isExpanded(path21));
        TreePath path12 = path1.pathByAddingChild(new DefaultMutableTreeNode());
        tree.treeModelListener.treeNodesInserted(new TreeModelEvent(tree, path1,
                new int[] { 1 }, new Object[] { path12 }));
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path12));
        assertFalse(tree.isExpanded(path111));
        assertFalse(tree.isExpanded(path2));
        assertFalse(tree.isExpanded(path21));
        tree.treeModelListener.treeNodesRemoved(new TreeModelEvent(tree, path1,
                new int[] { 0 }, new Object[] { child11 }));
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        assertFalse(tree.isExpanded(path2));
        assertFalse(tree.isExpanded(path21));
        tree.setExpandedState(path11, true);
        tree.treeModelListener.treeNodesRemoved(new TreeModelEvent(tree, path1));
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        assertFalse(tree.isExpanded(path2));
        assertFalse(tree.isExpanded(path21));
        tree.setExpandedState(path2, true);
        tree.treeModelListener.treeNodesRemoved(new TreeModelEvent(tree, rootPath,
                new int[] { 0 /*index is not important*/}, new Object[] { child2 }));
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        assertFalse(tree.isExpanded(path2));
        assertFalse(tree.isExpanded(path21));
        tree.setExpandedState(path11, true);
        tree.setExpandedState(path2, true);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        assertTrue(tree.isExpanded(path2));
        assertFalse(tree.isExpanded(path21));
        tree.treeModelListener.treeStructureChanged(new TreeModelEvent(tree, rootPath));
        assertTrue(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        assertFalse(tree.isExpanded(path2));
        assertFalse(tree.isExpanded(path21));
        tree.setExpandedState(path11, true);
        tree.setExpandedState(path2, true);
        tree.treeModelListener.treeStructureChanged(new TreeModelEvent("any", rootPath,
                new int[] { 0 }, new Object[] { child1 }));
        assertTrue(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        assertFalse(tree.isExpanded(path2));
        assertFalse(tree.isExpanded(path21));
    }

    public void testExpandCollapsePathRow() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode("child111");
        root.add(child1);
        child1.add(child11);
        child11.add(child111);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        TreePath path111 = path11.pathByAddingChild(child111);
        assertTrue(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.expandPath(path11);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.collapseRow(0);
        assertFalse(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.expandPath(path111);
        assertFalse(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.expandPath(path11);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.collapsePath(path1);
        assertTrue(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.collapsePath(path11);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.collapsePath(path111);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.collapseRow(1);
        assertTrue(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.expandRow(1);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.collapseRow(2);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.expandRow(10);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.collapseRow(10);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.collapseRow(-1);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        DefaultMutableTreeNode unexisted = new DefaultMutableTreeNode("unexisted");
        TreePath unexistedPath = rootPath.pathByAddingChild(unexisted);
        tree.expandPath(unexistedPath);
        assertFalse(tree.isExpanded(unexistedPath));
        unexisted.add(new DefaultMutableTreeNode());
        tree.expandPath(unexistedPath);
        assertTrue(tree.isExpanded(unexistedPath));
    }

    public void testHasBeenExpanded() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode("child111");
        root.add(child1);
        child1.add(child11);
        child11.add(child111);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        assertTrue(tree.hasBeenExpanded(rootPath));
        assertFalse(tree.hasBeenExpanded(path1));
        assertFalse(tree.hasBeenExpanded(path11));
        tree.expandPath(path11);
        assertTrue(tree.hasBeenExpanded(rootPath));
        assertTrue(tree.hasBeenExpanded(path1));
        assertTrue(tree.hasBeenExpanded(path11));
        tree.expandPath(path1);
        assertTrue(tree.hasBeenExpanded(rootPath));
        assertTrue(tree.hasBeenExpanded(path1));
        assertTrue(tree.hasBeenExpanded(path11));
        ((DefaultTreeModel) tree.getModel()).removeNodeFromParent(child11);
        assertTrue(tree.hasBeenExpanded(rootPath));
        if (isHarmony()) {
            assertTrue(tree.hasBeenExpanded(path1));
        } else {
            assertFalse(tree.hasBeenExpanded(path1));
        }
        assertFalse(tree.hasBeenExpanded(path11));
        assertFalse(tree.hasBeenExpanded(null));
    }

    public void testFireTreeExpandedCollapsedWillExpandCollapse() throws Exception {
        final Marker expandMarker = new Marker();
        final Marker collapseMarker = new Marker();
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            public void treeExpanded(final TreeExpansionEvent event) {
                expandMarker.setOccurred();
                expandMarker.setAuxiliary(event);
            }

            public void treeCollapsed(final TreeExpansionEvent event) {
                collapseMarker.setOccurred();
                collapseMarker.setAuxiliary(event);
            }
        });
        final Marker willExpandMarker = new Marker();
        final Marker willCollapseMarker = new Marker();
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(final TreeExpansionEvent event)
                    throws ExpandVetoException {
                willExpandMarker.setOccurred();
                willExpandMarker.setAuxiliary(event);
            }

            public void treeWillCollapse(final TreeExpansionEvent event)
                    throws ExpandVetoException {
                willCollapseMarker.setOccurred();
                willCollapseMarker.setAuxiliary(event);
            }
        });
        TreePath eventPath = new TreePath(new DefaultMutableTreeNode("anyRoot"))
                .pathByAddingChild(new DefaultMutableTreeNode("anyNode"));
        tree.setExpandedState(eventPath, true);
        assertTrue(willExpandMarker.isOccurred());
        assertTrue(expandMarker.isOccurred());
        assertFalse(willCollapseMarker.isOccurred());
        assertFalse(collapseMarker.isOccurred());
        assertSame(eventPath, ((TreeExpansionEvent) willExpandMarker.getAuxiliary()).getPath());
        assertSame(eventPath, ((TreeExpansionEvent) expandMarker.getAuxiliary()).getPath());
        expandMarker.reset();
        willExpandMarker.reset();
        collapseMarker.reset();
        willCollapseMarker.reset();
        tree.setExpandedState(eventPath, false);
        assertFalse(willExpandMarker.isOccurred());
        assertFalse(expandMarker.isOccurred());
        assertTrue(willCollapseMarker.isOccurred());
        assertTrue(collapseMarker.isOccurred());
        assertSame(eventPath, ((TreeExpansionEvent) willCollapseMarker.getAuxiliary())
                .getPath());
        assertSame(eventPath, ((TreeExpansionEvent) collapseMarker.getAuxiliary()).getPath());
        expandMarker.reset();
        willExpandMarker.reset();
        collapseMarker.reset();
        willCollapseMarker.reset();
        tree.fireTreeExpanded(eventPath);
        tree.fireTreeCollapsed(eventPath);
        tree.fireTreeWillExpand(eventPath);
        tree.fireTreeWillCollapse(eventPath);
        assertTrue(willExpandMarker.isOccurred());
        assertTrue(expandMarker.isOccurred());
        assertTrue(willCollapseMarker.isOccurred());
        assertTrue(collapseMarker.isOccurred());
        assertSame(eventPath, ((TreeExpansionEvent) willExpandMarker.getAuxiliary()).getPath());
        assertSame(eventPath, ((TreeExpansionEvent) expandMarker.getAuxiliary()).getPath());
        assertSame(eventPath, ((TreeExpansionEvent) willCollapseMarker.getAuxiliary())
                .getPath());
        assertSame(eventPath, ((TreeExpansionEvent) collapseMarker.getAuxiliary()).getPath());
    }

    public void testFireValueChanged() {
        final Marker changeMarker = new Marker();
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(final TreeSelectionEvent e) {
                changeMarker.setOccurred();
                changeMarker.setAuxiliary(e);
            }
        });
        TreePath eventPath = new TreePath(new DefaultMutableTreeNode("anyRoot"))
                .pathByAddingChild(new DefaultMutableTreeNode("anyNode"));
        tree.getSelectionModel().addSelectionPath(eventPath);
        assertTrue(changeMarker.isOccurred());
        assertSame(eventPath, ((TreeSelectionEvent) changeMarker.getAuxiliary()).getPath());
        changeMarker.reset();
        tree.fireValueChanged(null);
        assertTrue(changeMarker.isOccurred());
        assertNull(changeMarker.getAuxiliary());
    }

    public void testGetSetAnchorSelectionPath() {
        TreePath path = new TreePath("unexisted");
        tree.setAnchorSelectionPath(path);
        assertSame(path, tree.getAnchorSelectionPath());
        tree.setAnchorSelectionPath(null);
        assertNull(tree.getAnchorSelectionPath());
        tree.getSelectionModel().addSelectionPath(path);
        assertSame(path, tree.getAnchorSelectionPath());
    }

    //This is part of UI functionality
    public void testGetClosestPathForLocation() {
        assertNotNull(tree.getClosestPathForLocation(1000, 1000));
    }

    //This is part of UI functionality
    public void testGetClosestRowForLocation() {
        assertEquals(0, tree.getClosestRowForLocation(-1000, -1000));
    }

    public void testGetDefaultTreeModel() {
        TreeModel model1 = JTree.getDefaultTreeModel();
        TreeModel model2 = JTree.getDefaultTreeModel();
        assertTrue(model1 instanceof DefaultTreeModel);
        assertNotSame(model1, model2);
        assertTrue(model1.getRoot() instanceof DefaultMutableTreeNode);
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model1.getRoot();
        assertEquals(3, root.getChildCount());
        assertEquals("JTree", root.getUserObject());
        assertTrue(root.getChildAt(0) instanceof DefaultMutableTreeNode);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(0);
        if (isHarmony()) {
            assertEquals("towns", node.getUserObject());
            assertEquals(4, node.getChildCount());
            assertEquals("Saint-Petersburg", ((DefaultMutableTreeNode) node.getChildAt(0))
                    .getUserObject());
            assertEquals("New-York", ((DefaultMutableTreeNode) node.getChildAt(1))
                    .getUserObject());
            assertEquals("Munchen", ((DefaultMutableTreeNode) node.getChildAt(2))
                    .getUserObject());
            assertEquals("Oslo", ((DefaultMutableTreeNode) node.getChildAt(3)).getUserObject());
        } else {
            assertEquals("colors", node.getUserObject());
            assertEquals(4, node.getChildCount());
            assertEquals("blue", ((DefaultMutableTreeNode) node.getChildAt(0)).getUserObject());
            assertEquals("violet", ((DefaultMutableTreeNode) node.getChildAt(1))
                    .getUserObject());
            assertEquals("red", ((DefaultMutableTreeNode) node.getChildAt(2)).getUserObject());
            assertEquals("yellow", ((DefaultMutableTreeNode) node.getChildAt(3))
                    .getUserObject());
        }
        assertTrue(root.getChildAt(1) instanceof DefaultMutableTreeNode);
        node = (DefaultMutableTreeNode) root.getChildAt(1);
        if (isHarmony()) {
            assertEquals("animals", node.getUserObject());
            assertEquals(4, node.getChildCount());
            assertEquals("dog", ((DefaultMutableTreeNode) node.getChildAt(0)).getUserObject());
            assertEquals("tiger", ((DefaultMutableTreeNode) node.getChildAt(1)).getUserObject());
            assertEquals("wolf", ((DefaultMutableTreeNode) node.getChildAt(2)).getUserObject());
            assertEquals("bear", ((DefaultMutableTreeNode) node.getChildAt(3)).getUserObject());
        } else {
            assertEquals("sports", node.getUserObject());
            assertEquals(4, node.getChildCount());
            assertEquals("basketball", ((DefaultMutableTreeNode) node.getChildAt(0))
                    .getUserObject());
            assertEquals("soccer", ((DefaultMutableTreeNode) node.getChildAt(1))
                    .getUserObject());
            assertEquals("football", ((DefaultMutableTreeNode) node.getChildAt(2))
                    .getUserObject());
            assertEquals("hockey", ((DefaultMutableTreeNode) node.getChildAt(3))
                    .getUserObject());
        }
        assertTrue(root.getChildAt(2) instanceof DefaultMutableTreeNode);
        node = (DefaultMutableTreeNode) root.getChildAt(2);
        if (isHarmony()) {
            assertEquals("computers", node.getUserObject());
            assertEquals(4, node.getChildCount());
            assertEquals("notebook", ((DefaultMutableTreeNode) node.getChildAt(0))
                    .getUserObject());
            assertEquals("desktop", ((DefaultMutableTreeNode) node.getChildAt(1))
                    .getUserObject());
            assertEquals("server", ((DefaultMutableTreeNode) node.getChildAt(2))
                    .getUserObject());
            assertEquals("mainframe", ((DefaultMutableTreeNode) node.getChildAt(3))
                    .getUserObject());
        } else {
            assertEquals("food", node.getUserObject());
            assertEquals(4, node.getChildCount());
            assertEquals("hot dogs", ((DefaultMutableTreeNode) node.getChildAt(0))
                    .getUserObject());
            assertEquals("pizza", ((DefaultMutableTreeNode) node.getChildAt(1)).getUserObject());
            assertEquals("ravioli", ((DefaultMutableTreeNode) node.getChildAt(2))
                    .getUserObject());
            assertEquals("bananas", ((DefaultMutableTreeNode) node.getChildAt(3))
                    .getUserObject());
        }
    }

    public void testGetDescendantToggledPaths() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode("child111");
        root.add(child1);
        child1.add(child11);
        child11.add(child111);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        assertNull(tree.getDescendantToggledPaths(null));
        checkInEnumeration(tree.getDescendantToggledPaths(rootPath), new Object[] { rootPath });
        checkInEnumeration(tree.getDescendantToggledPaths(path1), new Object[] {});
        checkInEnumeration(tree.getDescendantToggledPaths(path11), new Object[] {});
        tree.expandPath(path11);
        checkInEnumeration(tree.getDescendantToggledPaths(rootPath), new Object[] { path1,
                rootPath, path11 });
        checkInEnumeration(tree.getDescendantToggledPaths(path11), new Object[] { path11 });
        tree.collapsePath(path11);
        checkInEnumeration(tree.getDescendantToggledPaths(rootPath), new Object[] { path1,
                rootPath, path11 });
        tree.collapsePath(rootPath);
        checkInEnumeration(tree.getDescendantToggledPaths(rootPath), new Object[] { path1,
                rootPath, path11 });
    }

    public void testGetDragEnabled() {
        assertFalse(tree.getDragEnabled());
        tree.setDragEnabled(true);
        assertTrue(tree.getDragEnabled());
        assertFalse(propertyChangeController.isChanged());
    }

    public void testGetExpandedDescendants() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode("child111");
        root.add(child1);
        child1.add(child11);
        child11.add(child111);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        assertNull(tree.getExpandedDescendants(null));
        checkInEnumeration(tree.getExpandedDescendants(rootPath), new Object[] { rootPath });
        assertNull(tree.getExpandedDescendants(path1));
        assertNull(tree.getExpandedDescendants(path11));        
        tree.expandPath(path11);
        checkInEnumeration(tree.getExpandedDescendants(rootPath), new Object[] { path1,
                rootPath, path11 });
        tree.collapsePath(path11);
        checkInEnumeration(tree.getExpandedDescendants(rootPath), new Object[] { path1,
                rootPath });
        tree.collapsePath(rootPath);
        assertNull(tree.getExpandedDescendants(rootPath));
    }

    public void testGetSetExpandsSelectedPaths() {
        assertTrue(tree.getExpandsSelectedPaths());
        tree.setExpandsSelectedPaths(false);
        assertFalse(tree.getExpandsSelectedPaths());
        assertTrue(propertyChangeController.isChanged("expandsSelectedPaths"));
    }

    public void testGetSetInvokesStopCellEditing() {
        assertFalse(tree.getInvokesStopCellEditing());
        tree.setInvokesStopCellEditing(true);
        assertTrue(tree.getInvokesStopCellEditing());
        assertTrue(propertyChangeController.isChanged("invokesStopCellEditing"));
    }

    public void testGetLastSelectedPathComponent() {
        assertNull(tree.getLastSelectedPathComponent());
        tree.setSelectionRow(1);
        assertTrue(tree.getLastSelectedPathComponent() instanceof DefaultMutableTreeNode);
        if (isHarmony()) {
            assertEquals("towns", tree.getLastSelectedPathComponent().toString());
        } else {
            assertEquals("colors", tree.getLastSelectedPathComponent().toString());
        }
    }

    public void testGetLeadSelectionRow() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode("child111");
        root.add(child1);
        child1.add(child11);
        child11.add(child111);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        assertEquals(-1, tree.getLeadSelectionRow());
        tree.setSelectionRow(10);
        assertEquals(-1, tree.getLeadSelectionRow());
        tree.setExpandsSelectedPaths(false);
        tree.setSelectionPath(path11);
        assertEquals(-1, tree.getLeadSelectionRow());
        tree.setLeadSelectionPath(path11);
        assertEquals(-1, tree.getLeadSelectionRow());
        tree.setLeadSelectionPath(path1);
        assertEquals(1, tree.getLeadSelectionRow());
    }

    public void testGetMinMaxSelectionRow() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("child2");
        root.add(child1);
        child1.add(child11);
        root.add(child2);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        TreePath path2 = rootPath.pathByAddingChild(child2);
        tree.setSelectionPaths(new TreePath[] { path2, path11 });
        assertEquals(2, tree.getMinSelectionRow());
        assertEquals(3, tree.getMaxSelectionRow());
        tree.collapsePath(path1);
        assertEqualsIgnoreOrder(new TreePath[] { path2, path1 }, tree.getSelectionPaths());
        assertEquals(1, tree.getMinSelectionRow());
        assertEquals(2, tree.getMaxSelectionRow());
        tree.expandPath(path1);
        assertEqualsIgnoreOrder(new TreePath[] { path2, path1 }, tree.getSelectionPaths());
        assertEquals(1, tree.getMinSelectionRow());
        assertEquals(3, tree.getMaxSelectionRow());
    }

    public void testGetNextMatch() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("child2");
        root.add(child1);
        child1.add(child11);
        root.add(child2);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        TreePath path2 = rootPath.pathByAddingChild(child2);
        assertEquals(rootPath, tree.getNextMatch("ro", 0, Position.Bias.Forward));
        assertEquals(rootPath, tree.getNextMatch("ro", 2, Position.Bias.Forward));
        assertEquals(rootPath, tree.getNextMatch("ro", 2, Position.Bias.Backward));
        assertEquals(path1, tree.getNextMatch("ch", 0, Position.Bias.Forward));
        assertEquals(path1, tree.getNextMatch("ch", 1, Position.Bias.Forward));
        assertEquals(path1, tree.getNextMatch("ch", 1, Position.Bias.Backward));
        assertEquals(path2, tree.getNextMatch("ch", 2, Position.Bias.Forward));
        assertEquals(path1, tree.getNextMatch("child1", 2, Position.Bias.Forward));
        assertEquals(path2, tree.getNextMatch("child2", 1, Position.Bias.Backward));
        assertNull(tree.getNextMatch("child11", 1, Position.Bias.Backward));
        tree.expandRow(1);
        assertEquals(path11, tree.getNextMatch("child11", 1, Position.Bias.Backward));
        assertEquals(path1, tree.getNextMatch("child1", 0, Position.Bias.Forward));
        assertEquals(path11, tree.getNextMatch("child1", 0, Position.Bias.Backward));
        assertEquals(path11, tree.getNextMatch("child1", 0, null));
        assertEquals(path11, tree.getNextMatch("ChiLD1", 0, null));
        assertNull(tree.getNextMatch("childX", 1, Position.Bias.Forward));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                tree.getNextMatch("any", -1, Position.Bias.Forward);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                tree.getNextMatch("any", 10, Position.Bias.Forward);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                tree.getNextMatch(null, 1, Position.Bias.Forward);
            }
        });
    }

    public void testGetPathBetweenRows() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("child2");
        root.add(child1);
        child1.add(child11);
        root.add(child2);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        TreePath path2 = rootPath.pathByAddingChild(child2);
        assertEquals(new TreePath[] { path1 }, tree.getPathBetweenRows(1, 1));
        assertEquals(new TreePath[] { rootPath, path1 }, tree.getPathBetweenRows(0, 1));
        assertEquals(new TreePath[] { path1, path2 }, tree.getPathBetweenRows(1, 2));
        assertEquals(new TreePath[] { rootPath, path1, path2 }, tree.getPathBetweenRows(0, 2));
        if (isHarmony()) {
            assertEquals(new TreePath[] { rootPath, path1, path2 }, tree.getPathBetweenRows(-1,
                    3));
        } else {
            assertEquals(new TreePath[] { null, rootPath, path1, path2, null }, tree
                    .getPathBetweenRows(-1, 3));
        }
        tree.expandPath(path1);
        assertEquals(new TreePath[] { rootPath, path1, path11 }, tree.getPathBetweenRows(0, 2));
    }

    //This is UI behavior. Should not be deeply tested here
    public void testGetPathBounds() {
        Object root = tree.getModel().getRoot();
        TreePath pathToRoot = new TreePath(root);
        assertEquals(tree.getPathBounds(pathToRoot), tree.getUI().getPathBounds(tree,
                pathToRoot));
        TreePath pathToChild = pathToRoot.pathByAddingChild(tree.getModel().getChild(root, 0));
        assertEquals(tree.getPathBounds(pathToChild), tree.getUI().getPathBounds(tree,
                pathToChild));
    }

    //This is UI behavior. Should not be deeply tested here
    public void testGetRowBounds() {
        Object root = tree.getModel().getRoot();
        TreePath pathToRoot = new TreePath(root);
        assertEquals(tree.getRowBounds(0), tree.getUI().getPathBounds(tree, pathToRoot));
        TreePath pathToChild = pathToRoot.pathByAddingChild(tree.getModel().getChild(root, 0));
        assertEquals(tree.getRowBounds(1), tree.getUI().getPathBounds(tree, pathToChild));
        assertNull(tree.getRowBounds(-1));
        assertNull(tree.getRowBounds(10));
    }

    public void testGetPathForLocation() {
        assertEquals(new TreePath(tree.getModel().getRoot()), tree.getPathForLocation(5, 5));
        assertNull(tree.getPathForLocation(500, 5));
    }

    public void testGetRowForLocation() {
        assertEquals(0, tree.getRowForLocation(5, 5));
        assertEquals(-1, tree.getRowForLocation(500, 5));
    }

    //This is UI behavior. Should not be deeply tested here
    public void testGetRowForPath() {
        Object root = tree.getModel().getRoot();
        TreePath pathToRoot = new TreePath(root);
        assertEquals(0, tree.getRowForPath(pathToRoot));
        Object child = tree.getModel().getChild(root, 0);
        TreePath pathToChild = pathToRoot.pathByAddingChild(child);
        assertEquals(1, tree.getRowForPath(pathToChild));
        Object childChild = tree.getModel().getChild(child, 0);
        TreePath pathToChildChild = pathToChild.pathByAddingChild(childChild);
        assertEquals(-1, tree.getRowForPath(pathToChildChild));
    }

    //This is UI behavior. Should not be deeply tested here
    public void testGetPathForRow() {
        assertNotNull(tree.getPathForRow(0));
        assertNull(tree.getPathForRow(-1));
        assertNull(tree.getPathForRow(10));
    }

    public void testGetRowCount() {
        assertEquals(4, tree.getRowCount());
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("tree root");
        DefaultMutableTreeNode child = new DefaultMutableTreeNode("child");
        root.add(child);
        child.add(new DefaultMutableTreeNode("child_child"));
        tree.setModel(new DefaultTreeModel(root));
        assertEquals(2, tree.getRowCount());
        tree.setModel(null);
        assertEquals(0, tree.getRowCount());
    }

    public void testGetSetRowHeight() {
        assertEquals(0, tree.getRowHeight());
        tree.setRowHeight(20);
        assertEquals(20, tree.getRowHeight());
        assertTrue(propertyChangeController.isChanged("rowHeight"));
        propertyChangeController.reset();
        tree.setRowHeight(-10);
        assertEquals(-10, tree.getRowHeight());
        assertTrue(propertyChangeController.isChanged("rowHeight"));
    }

    public void testGetSetScrollsOnExpand() {
        assertTrue(tree.getScrollsOnExpand());
        tree.setScrollsOnExpand(false);
        assertFalse(tree.getScrollsOnExpand());
        assertTrue(propertyChangeController.isChanged("scrollsOnExpand"));
    }

    public void testGetSelectionCount() {
        assertEquals(0, tree.getSelectionCount());
        Object root = tree.getModel().getRoot();
        TreePath pathToRoot = new TreePath(root);
        Object child = tree.getModel().getChild(root, 0);
        TreePath pathToChild = pathToRoot.pathByAddingChild(child);
        Object childChild = tree.getModel().getChild(child, 0);
        TreePath pathToChildChild = pathToChild.pathByAddingChild(childChild);
        tree.addSelectionPath(pathToRoot);
        assertEquals(1, tree.getSelectionCount());
        tree.addSelectionPath(pathToChild);
        assertEquals(2, tree.getSelectionCount());
        tree.setExpandsSelectedPaths(false);
        tree.addSelectionPath(pathToChildChild);
        assertEquals(3, tree.getSelectionCount());
        tree.clearSelection();
        assertEquals(0, tree.getSelectionCount());
        tree.setAnchorSelectionPath(pathToChildChild);
        tree.setLeadSelectionPath(pathToRoot);
        assertEquals(0, tree.getSelectionCount());
    }

    public void testGetSelectionModel() {
        assertNotNull(tree.getSelectionModel());
        assertTrue(tree.getSelectionModel() instanceof DefaultTreeSelectionModel);
        assertNotNull(tree.getSelectionModel().getRowMapper());
    }

    public void testGetSetSelectionPaths() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode("child111");
        root.add(child1);
        child1.add(child11);
        child11.add(child111);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        TreePath path111 = path11.pathByAddingChild(child111);
        assertNull(tree.getSelectionPaths());
        tree.setSelectionPath(path1);
        tree.addSelectionPath(rootPath);
        assertEqualsIgnoreOrder(new TreePath[] { path1, rootPath }, tree.getSelectionPaths());
        tree.setSelectionPath(path11);
        assertEqualsIgnoreOrder(new TreePath[] { path11 }, tree.getSelectionPaths());
        tree.addSelectionPaths(new TreePath[] { rootPath, path111 });
        assertEqualsIgnoreOrder(new TreePath[] { path11, rootPath, path111 }, tree
                .getSelectionPaths());
        tree.collapsePath(path1);
        assertEqualsIgnoreOrder(new TreePath[] { rootPath, path1 }, tree.getSelectionPaths());
        tree.setSelectionPaths(new TreePath[] { path111 });
        assertEqualsIgnoreOrder(new TreePath[] { path111 }, tree.getSelectionPaths());
        tree.setSelectionPaths(null);
        assertNull(tree.getSelectionPaths());
    }

    public void testGetSetShowsRootHandles() {
        assertFalse(tree.getShowsRootHandles());
        tree.setShowsRootHandles(true);
        assertTrue(tree.getShowsRootHandles());
        assertTrue(propertyChangeController.isChanged("showsRootHandles"));
    }

    public void testGetToggleClickCount() {
        assertEquals(2, tree.getToggleClickCount());
        tree.setToggleClickCount(10);
        assertEquals(10, tree.getToggleClickCount());
        assertTrue(propertyChangeController.isChanged("toggleClickCount"));
    }

    public void testGetUI() {
        assertNotNull("ui is returned ", tree.getUI());
    }

    public void testIsSetEditable() {
        assertFalse(tree.isEditable());
        tree.setEditable(true);
        assertTrue(tree.isEditable());
        assertTrue(propertyChangeController.isChanged("editable"));
    }

    public void testIsExpanded() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode("child111");
        root.add(child1);
        child1.add(child11);
        child11.add(child111);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(0));
        assertFalse(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(1));
        tree.expandPath(path1);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(0));
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(2));
    }

    public void testIsFixedRowHeight() {
        assertFalse(tree.isFixedRowHeight());
        tree.setRowHeight(10);
        assertTrue(tree.isFixedRowHeight());
        tree.setRowHeight(-10);
        assertFalse(tree.isFixedRowHeight());
    }

    public void testIsLargeModel() {
        assertFalse(tree.isLargeModel());
        tree.setLargeModel(true);
        assertTrue(tree.isLargeModel());
        assertTrue(propertyChangeController.isChanged("largeModel"));
    }

    public void testIsPathEditable() {
        assertFalse(tree.isPathEditable(null));
        tree.setEditable(true);
        assertTrue(tree.isPathEditable(null));
    }

    public void testSetIsRootVisible() {
        assertTrue(tree.isRootVisible());
        tree.setRootVisible(false);
        assertFalse(tree.isRootVisible());
        assertTrue(propertyChangeController.isChanged("rootVisible"));
    }

    public void testIsPathSelected() {
        TreePath unexistedPath = new TreePath(new DefaultMutableTreeNode("any root"));
        assertFalse(tree.isPathSelected(null));
        assertFalse(tree.isPathSelected(unexistedPath));
        tree.setSelectionPath(unexistedPath);
        assertTrue(tree.isPathSelected(unexistedPath));
    }

    public void testIsRowSelected() {
        assertFalse(tree.isRowSelected(0));
        assertFalse(tree.isRowSelected(10));
        tree.setSelectionRow(0);
        assertTrue(tree.isRowSelected(0));
    }

    public void testIsSelectionEmpty() {
        assertTrue(tree.isSelectionEmpty());
        tree.setSelectionRow(0);
        assertFalse(tree.isSelectionEmpty());
        tree.clearSelection();
        assertTrue(tree.isSelectionEmpty());
    }

    public void testIsVisible() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode("child111");
        root.add(child1);
        child1.add(child11);
        child11.add(child111);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        TreePath path111 = path11.pathByAddingChild(child111);
        assertTrue(tree.isVisible(rootPath));
        assertTrue(tree.isVisible(path1));
        assertFalse(tree.isVisible(path11));
        assertFalse(tree.isVisible(path111));
        tree.expandPath(path11);
        assertTrue(tree.isVisible(rootPath));
        assertTrue(tree.isVisible(path1));
        assertTrue(tree.isVisible(path11));
        assertTrue(tree.isVisible(path111));
    }

    public void testMakeVisible() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode("child111");
        root.add(child1);
        child1.add(child11);
        child11.add(child111);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        TreePath path111 = path11.pathByAddingChild(child111);
        assertFalse(tree.isExpanded(path1));
        tree.makeVisible(path1);
        assertFalse(tree.isExpanded(path1));
        tree.makeVisible(path111);
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
    }

    public void testRemoveDescendantSelectedPaths() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode("child111");
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("child2");
        DefaultMutableTreeNode child21 = new DefaultMutableTreeNode("child21");
        root.add(child1);
        child1.add(child11);
        child11.add(child111);
        root.add(child2);
        child2.add(child21);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        TreePath path21 = rootPath.pathByAddingChild(child21);
        tree.setSelectionPaths(new TreePath[] { rootPath, path11, path21 });
        assertEqualsIgnoreOrder(new TreePath[] { rootPath, path11, path21 }, tree
                .getSelectionPaths());
        tree.removeDescendantSelectedPaths(path11, false);
        assertEqualsIgnoreOrder(new TreePath[] { rootPath, path11, path21 }, tree
                .getSelectionPaths());
        tree.removeDescendantSelectedPaths(path11, true);
        assertEqualsIgnoreOrder(new TreePath[] { rootPath, path21 }, tree.getSelectionPaths());
        tree.removeDescendantSelectedPaths(rootPath, false);
        assertEqualsIgnoreOrder(new TreePath[] { rootPath }, tree.getSelectionPaths());
        tree.removeDescendantSelectedPaths(rootPath, true);
        assertNull(tree.getSelectionPaths());
    }

    public void testRemoveDescendantToggledPaths() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("child1");
        DefaultMutableTreeNode child11 = new DefaultMutableTreeNode("child11");
        DefaultMutableTreeNode child111 = new DefaultMutableTreeNode("child111");
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("child2");
        DefaultMutableTreeNode child21 = new DefaultMutableTreeNode("child21");
        root.add(child1);
        child1.add(child11);
        child11.add(child111);
        root.add(child2);
        child2.add(child21);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(child1);
        TreePath path11 = path1.pathByAddingChild(child11);
        TreePath path2 = rootPath.pathByAddingChild(child2);
        assertTrue(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path2));
        tree.expandPath(path1);
        tree.expandPath(path2);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertTrue(tree.isExpanded(path2));
        tree.removeDescendantToggledPaths(createTestEnumeration(
                new TreePath[] { path2, path1 }));
        assertTrue(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path2));
        tree.expandPath(path11);
        tree.expandPath(path2);
        tree
                .removeDescendantToggledPaths(createTestEnumeration(new TreePath[] { path2,
                        path11 }));
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path2));
    }

    public void testSetCellEditor() {
        PropertyChangeController listener = new PropertyChangeController();
        DefaultTreeCellEditor editor1 = new DefaultTreeCellEditor(tree,
                (DefaultTreeCellRenderer) tree.getCellRenderer());
        DefaultTreeCellEditor editor2 = new DefaultTreeCellEditor(tree,
                (DefaultTreeCellRenderer) tree.getCellRenderer());
        tree.addPropertyChangeListener(listener);
        TreeCellEditor oldEditor = tree.getCellEditor();
        tree.setCellEditor(editor1);
        listener.checkPropertyFired(tree, "cellEditor", oldEditor, editor1);
        assertEquals("cellEditor", editor1, tree.getCellEditor());
        tree.setEditable(true);
        listener.reset();
        tree.setCellEditor(editor2);
        listener.checkPropertyFired(tree, "cellEditor", editor1, editor2);
        assertEquals("cellEditor", editor2, tree.getCellEditor());
        assertTrue(tree.isEditable());
        listener.reset();
        tree.setCellEditor(editor2);
        assertFalse("event's not been fired ", listener.isChanged());
        listener.reset();
        tree.setCellEditor(null);
        listener.checkPropertyFired(tree, "cellEditor", editor2, null);
        // it's being controlled by UI via listener
        assertNotNull("cellEditor", tree.getCellEditor());
        assertNotSame("cellEditor", oldEditor, tree.getCellEditor());
        assertNotSame("cellEditor", editor2, tree.getCellEditor());
        assertTrue(tree.isEditable());
        listener.reset();
    }

    public void testGetCellEditor() {
        assertNull(tree.getCellEditor());
    }

    public void testSetCellRenderer() {
        PropertyChangeController listener = new PropertyChangeController();
        TreeCellRenderer renderer1 = new DefaultTreeCellRenderer();
        TreeCellRenderer renderer2 = new DefaultTreeCellRenderer();
        tree.addPropertyChangeListener(listener);
        TreeCellRenderer oldRenderer = tree.getCellRenderer();
        tree.setCellRenderer(renderer1);
        listener.checkPropertyFired(tree, "cellRenderer", oldRenderer, renderer1);
        assertEquals("cellRenderer", renderer1, tree.getCellRenderer());
        listener.reset();
        tree.setCellRenderer(renderer2);
        listener.checkPropertyFired(tree, "cellRenderer", renderer1, renderer2);
        assertEquals("cellRenderer", renderer2, tree.getCellRenderer());
        listener.reset();
        tree.setCellRenderer(renderer2);
        assertFalse("event's not been fired ", listener.isChanged());
        listener.reset();
        tree.setCellRenderer(null);
        listener.checkPropertyFired(tree, "cellRenderer", renderer2, null);
        // it's being controlled by UI via listener
        assertNotNull("cellRenderer", tree.getCellRenderer());
        assertNotSame("cellRenderer", oldRenderer, tree.getCellRenderer());
        assertNotSame("cellRenderer", renderer2, tree.getCellRenderer());
        listener.reset();
    }

    public void testGetCellRenderer() {
        assertNotNull(tree.getCellRenderer());
        assertTrue(tree.getCellRenderer() instanceof DefaultTreeCellRenderer);
    }

    public void testSetExpandedState() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode();
        DefaultMutableTreeNode node11 = new DefaultMutableTreeNode();
        DefaultMutableTreeNode node111 = new DefaultMutableTreeNode();
        root.add(node1);
        node1.add(node11);
        node11.add(node111);
        DefaultTreeModel model = new DefaultTreeModel(root);
        tree.setModel(model);
        TreePath rootPath = new TreePath(root);
        TreePath path1 = rootPath.pathByAddingChild(node1);
        TreePath path11 = path1.pathByAddingChild(node11);
        TreePath path111 = path11.pathByAddingChild(node111);
        assertTrue(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        tree.setExpandedState(path1, true);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        tree.setExpandedState(rootPath, false);
        assertFalse(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        tree.setExpandedState(rootPath, true);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        tree.setExpandedState(rootPath, false);
        assertFalse(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        tree.setExpandedState(path1, false);
        assertTrue(tree.isExpanded(rootPath));
        assertFalse(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        tree.setExpandedState(path11, false);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertFalse(tree.isExpanded(path11));
        tree.setExpandedState(path111, true);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertTrue(tree.isExpanded(path111));
        TreePath unexistedPath = path111.pathByAddingChild(new DefaultMutableTreeNode());
        tree.setExpandedState(unexistedPath, true);
        assertTrue(tree.isExpanded(unexistedPath));
        tree.setExpandedState(unexistedPath, false);
        assertFalse(tree.isExpanded(unexistedPath));
        tree.setExpandedState(path111, false);
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                throw new ExpandVetoException(event);
            }

            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            }
        });
        tree.setExpandedState(path111, true);
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
            }

            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                throw new ExpandVetoException(event);
            }
        });
        tree.setExpandedState(path11, false);
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
        tree.setExpandedState(null, false);
        assertTrue(tree.isExpanded(rootPath));
        assertTrue(tree.isExpanded(path1));
        assertTrue(tree.isExpanded(path11));
        assertFalse(tree.isExpanded(path111));
    }

    public void testGetSetLeadSelectionPath() {
        TreePath path = new TreePath("unexisted");
        tree.setLeadSelectionPath(path);
        assertSame(path, tree.getLeadSelectionPath());
        assertNull(tree.getSelectionModel().getLeadSelectionPath());
        tree.setLeadSelectionPath(null);
        assertNull(tree.getLeadSelectionPath());
        tree.getSelectionModel().addSelectionPath(path);
        assertSame(path, tree.getSelectionModel().getLeadSelectionPath());
        assertSame(path, tree.getLeadSelectionPath());
    }

    public void testGetSetModel() {
        DefaultTreeModel oldModel = (DefaultTreeModel) tree.getModel();
        assertEquals(2, oldModel.getTreeModelListeners().length);
        DefaultTreeModel newModel = new DefaultTreeModel(
                new DefaultMutableTreeNode("some root"));
        tree.setModel(newModel);
        assertTrue(propertyChangeController.isChanged("model"));
        assertEquals(0, oldModel.getTreeModelListeners().length);
        assertEquals(2, newModel.getTreeModelListeners().length);
        tree.setModel(null);
        assertNull(tree.getModel());
    }

    public void testSetSelectionModel() {
        assertTrue(tree.getSelectionModel() instanceof DefaultTreeSelectionModel);
        RowMapper rowMapper1 = new RowMapper() {
            public int[] getRowsForPaths(TreePath[] path) {
                return null;
            }
        };
        RowMapper rowMapper2 = new RowMapper() {
            public int[] getRowsForPaths(TreePath[] path) {
                return null;
            }
        };
        TreeSelectionModel model1 = new DefaultTreeSelectionModel();
        TreeSelectionModel model2 = new DefaultTreeSelectionModel();
        RowMapper mapper = tree.getSelectionModel().getRowMapper();
        tree.setSelectionModel(model1);
        assertTrue(propertyChangeController.isChanged("selectionModel"));
        assertEquals(model1, tree.getSelectionModel());
        assertSame(mapper, tree.getSelectionModel().getRowMapper());
        tree.setSelectionModel(model2);
        assertEquals(model2, tree.getSelectionModel());
        assertSame(mapper, tree.getSelectionModel().getRowMapper());
        propertyChangeController.reset();
        TreeSelectionModel oldModel = tree.getSelectionModel();
        tree.setSelectionModel(null);
        assertNotNull(tree.getSelectionModel());
        assertTrue(tree.getSelectionModel() instanceof JTree.EmptySelectionModel);
        assertSame(mapper, tree.getSelectionModel().getRowMapper());
        assertTrue(propertyChangeController.isChanged("selectionModel"));
        propertyChangeController.checkPropertyFired(tree, "selectionModel", oldModel, tree
                .getSelectionModel());
        TreeSelectionModel emptyModel = tree.getSelectionModel();
        tree.setSelectionModel(model2);
        tree.setSelectionModel(null);
        assertSame(emptyModel, tree.getSelectionModel());
        model1.setRowMapper(rowMapper1);
        tree.setSelectionModel(model1);
        assertSame(mapper, model1.getRowMapper());
        model1.setRowMapper(null);
        model2.setRowMapper(rowMapper2);
        tree.setSelectionModel(model2);
        assertSame(mapper, model2.getRowMapper());
        assertTrue(mapper instanceof VariableHeightLayoutCache);
    }

    public void testGetSetSelectionPath() {
        assertNull(tree.getSelectionPath());
        TreePath path = new TreePath("non-existed");
        tree.setSelectionPath(path);
        assertSame(path, tree.getSelectionPath());
        assertSame(path, tree.getSelectionModel().getSelectionPath());
        assertEquals(1, tree.getSelectionCount());
        assertNull(tree.getSelectionRows());
        Object root = tree.getModel().getRoot();
        Object child = tree.getModel().getChild(root, 0);
        path = new TreePath(root).pathByAddingChild(child);
        tree.setSelectionPath(path);
        assertSame(path, tree.getSelectionPath());
        assertEquals(1, tree.getSelectionCount());
        assertEquals(1, tree.getSelectionRows().length);
        assertEquals(1, tree.getSelectionRows()[0]);
    }

    public void testGetSetSelectionRow() {
        assertNull(tree.getSelectionRows());
        tree.setSelectionRow(2);
        assertEquals(1, tree.getSelectionRows().length);
        assertEquals(2, tree.getSelectionRows()[0]);
        assertEquals(2, tree.getSelectionModel().getSelectionRows()[0]);
        tree.setSelectionRow(10000);
        assertNull(tree.getSelectionRows());
        TreePath path = new TreePath(tree.getModel().getRoot());
        tree.getSelectionModel().setSelectionPath(path);
        assertEquals(path, tree.getSelectionPath());
        assertEquals(1, tree.getSelectionRows().length);
        assertEquals(1, tree.getSelectionModel().getSelectionRows().length);
        assertEquals(0, tree.getSelectionRows()[0]);
    }

    public void testGetSetSelectionRows() {
        assertNull(tree.getSelectionRows());
        tree.setSelectionRows(null);
        assertNull(tree.getSelectionRows());
        tree.setSelectionRows(new int[] { 0, 1 });
        assertEquals(2, tree.getSelectionRows().length);
        assertEquals(2, tree.getSelectionModel().getSelectionRows().length);
        tree.setSelectionRows(new int[] { 100 });
        assertNull(tree.getSelectionRows());
        assertNull(tree.getSelectionPaths());
    }

    public void testSetUITreeUI() {
        assertNotNull(tree.getUI());
        TreeUI newUI = new TreeUI() {
            @Override
            public void cancelEditing(JTree tree) {
            }

            @Override
            public TreePath getClosestPathForLocation(JTree tree, int x, int y) {
                return null;
            }

            @Override
            public TreePath getEditingPath(JTree tree) {
                return null;
            }

            @Override
            public Rectangle getPathBounds(JTree tree, TreePath path) {
                return null;
            }

            @Override
            public TreePath getPathForRow(JTree tree, int row) {
                return null;
            }

            @Override
            public int getRowCount(JTree tree) {
                return 0;
            }

            @Override
            public int getRowForPath(JTree tree, TreePath path) {
                return 0;
            }

            @Override
            public boolean isEditing(JTree tree) {
                return false;
            }

            @Override
            public void startEditingAtPath(JTree tree, TreePath path) {
            }

            @Override
            public boolean stopEditing(JTree path) {
                return false;
            }
        };
        tree.setUI(newUI);
        assertEquals(newUI, tree.getUI());
    }

    public void testGetSetVisibleRowCount() {
        assertEquals(20, tree.getVisibleRowCount());
        tree.setVisibleRowCount(10);
        assertEquals(10, tree.getVisibleRowCount());
        assertTrue(propertyChangeController.isChanged("visibleRowCount"));
        tree.setVisibleRowCount(-5);
        assertEquals(-5, tree.getVisibleRowCount());
    }

    //UI behavior. Should not be deeply tested here
    public void testIsEditingStartEditingAtPathGetEditingPath() {
        assertFalse(tree.isEditing());
        assertFalse(tree.isEditable());
        TreePath rootPath = new TreePath(tree.getModel().getRoot());
        tree.startEditingAtPath(rootPath);
        assertFalse(tree.isEditing());
        assertNull(tree.getEditingPath());
        tree.setEditable(true);
        tree.startEditingAtPath(rootPath);
        assertTrue(tree.isEditing());
        assertSame(rootPath, tree.getEditingPath());
        assertTrue(tree.stopEditing());
        assertFalse(tree.isEditing());
        assertNull(tree.getEditingPath());
        assertFalse(tree.stopEditing());
        tree.startEditingAtPath(rootPath);
        tree.cancelEditing();
        assertFalse(tree.isEditing());
        assertNull(tree.getEditingPath());
    }

    public void testGetPreferredScrollableViewportSize() {
        tree.setPreferredSize(new Dimension(100, 200));
        tree.setVisibleRowCount(0);
        assertEquals(new Dimension(100, 0), tree.getPreferredScrollableViewportSize());
        tree.setVisibleRowCount(1);
        assertEquals(new Dimension(100, 18), tree.getPreferredScrollableViewportSize());
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(final JTree tree, final Object value,
                    final boolean selected, final boolean expanded, final boolean leaf,
                    final int row, final boolean hasFocus) {
                JComponent result = (JComponent) super.getTreeCellRendererComponent(tree,
                        value, selected, expanded, leaf, row, hasFocus);
                result.setPreferredSize(new Dimension(25, 10 * (row + 1)));
                result.setBounds(10, 20, 30, 40);
                return result;
            }
        });
        assertEquals(new Dimension(100, 10), tree.getPreferredScrollableViewportSize());
        tree.setModel(null);
        assertEquals(new Dimension(100, 16), tree.getPreferredScrollableViewportSize());
        tree.setRowHeight(20);
        tree.setVisibleRowCount(1);
        assertEquals(new Dimension(100, 20), tree.getPreferredScrollableViewportSize());
        tree.setVisibleRowCount(2);
        assertEquals(new Dimension(100, 40), tree.getPreferredScrollableViewportSize());
    }

    public void testGetScrollableBlockIncrement() {
        assertEquals(30, tree.getScrollableBlockIncrement(new Rectangle(10, 20, 30, 40),
                SwingConstants.HORIZONTAL, 1));
        assertEquals(30, tree.getScrollableBlockIncrement(new Rectangle(10, 20, 30, 40),
                SwingConstants.HORIZONTAL, -1));
        assertEquals(40, tree.getScrollableBlockIncrement(new Rectangle(10, 20, 30, 40),
                SwingConstants.VERTICAL, 1));
        assertEquals(40, tree.getScrollableBlockIncrement(new Rectangle(10, 20, 30, 40),
                SwingConstants.VERTICAL, -1));
    }

    public void testGetScrollableUnitIncrement() {
        assertEquals(4, tree.getScrollableUnitIncrement(new Rectangle(10, 20, 30, 40),
                SwingConstants.HORIZONTAL, 1));
        assertEquals(4, tree.getScrollableUnitIncrement(new Rectangle(100, 200, 300, 400),
                SwingConstants.HORIZONTAL, -1));
        assertEquals(8, tree.getScrollableUnitIncrement(new Rectangle(10, 10, 30, 40),
                SwingConstants.VERTICAL, 1));
        assertEquals(10, tree.getScrollableUnitIncrement(new Rectangle(10, 10, 30, 40),
                SwingConstants.VERTICAL, -1));
        assertEquals(16, tree.getScrollableUnitIncrement(new Rectangle(10, 20, 30, 40),
                SwingConstants.VERTICAL, 1));
        assertEquals(2, tree.getScrollableUnitIncrement(new Rectangle(10, 20, 30, 40),
                SwingConstants.VERTICAL, -1));
        assertEquals(-128, tree.getScrollableUnitIncrement(new Rectangle(10, 200, 30, 40),
                SwingConstants.VERTICAL, 1));
        assertEquals(146, tree.getScrollableUnitIncrement(new Rectangle(10, 200, 30, 40),
                SwingConstants.VERTICAL, -1));
        tree.setModel(null);
        assertEquals(0, tree.getScrollableUnitIncrement(new Rectangle(10, 200, 30, 40),
                SwingConstants.VERTICAL, 1));
        assertEquals(4, tree.getScrollableUnitIncrement(new Rectangle(10, 200, 30, 40),
                SwingConstants.HORIZONTAL, 1));
    }

    public void testGetScrollableTracksViewportHeight() {
        assertFalse(tree.getScrollableTracksViewportHeight());
        JViewport vp = new JViewport();
        vp.setView(tree);
        vp.setBounds(10, 10, 1000, tree.getPreferredSize().height);
        assertFalse(tree.getScrollableTracksViewportHeight());
        vp.setBounds(10, 10, 1000, tree.getPreferredSize().height + 1);
        assertTrue(tree.getScrollableTracksViewportHeight());
    }

    public void testGetScrollableTracksViewportWidth() {
        assertFalse(tree.getScrollableTracksViewportWidth());
        JViewport vp = new JViewport();
        vp.setView(tree);
        vp.setBounds(10, 10, tree.getPreferredSize().width, 1000);
        assertFalse(tree.getScrollableTracksViewportWidth());
        vp.setBounds(10, 10, tree.getPreferredSize().width + 1, 1000);
        assertTrue(tree.getScrollableTracksViewportWidth());
    }

    public void testScrollRowPathToVisible() {
        JViewport vp = new JViewport();
        vp.setView(tree);
        vp.setBounds(0, 0, 200, 20);
        vp.setViewPosition(new Point(0, 0));
        tree.scrollRowToVisible(3);
        assertTrue(vp.getViewRect().contains(tree.getRowBounds(3)));
        tree.scrollRowToVisible(1);
        assertTrue(vp.getViewRect().contains(tree.getRowBounds(1)));
        Object root = tree.getModel().getRoot();
        Object child0 = tree.getModel().getChild(root, 0);
        Object child2 = tree.getModel().getChild(root, 2);
        Object child00 = tree.getModel().getChild(child0, 0);
        TreePath rootPath = new TreePath(root);
        TreePath path0 = rootPath.pathByAddingChild(child0);
        TreePath path2 = rootPath.pathByAddingChild(child2);
        tree.scrollPathToVisible(path2);
        assertTrue(vp.getViewRect().contains(tree.getPathBounds(path2)));
        TreePath path00invisible = path0.pathByAddingChild(child00);
        assertFalse(tree.isVisible(path00invisible));
        tree.scrollPathToVisible(path00invisible);
        assertTrue(tree.isVisible(path00invisible));
        assertTrue(vp.getViewRect().contains(tree.getPathBounds(path00invisible)));
        tree.scrollPathToVisible(null);
        assertTrue(vp.getViewRect().contains(tree.getPathBounds(path00invisible)));
        tree.scrollRowToVisible(-10);
        assertTrue(vp.getViewRect().contains(tree.getPathBounds(path00invisible)));
    }

    private void checkInEnumeration(final Enumeration<? extends Object> e,
            final Object[] expected) {
        assertNotNull(e);
        List<Object> actual = new LinkedList<Object>();
        while (e.hasMoreElements()) {
            actual.add(e.nextElement());
        }
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) {
            if (!actual.contains(expected[i])) {
                fail("Element " + expected[i] + " doesn't exist in enumeration");
            }
        }
    }

    private void assertEqualsIgnoreOrder(final Object[] expected, final Object[] actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || actual == null) {
            fail("arrays are different");
        }
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            boolean found = false;
            for (int j = 0; j < actual.length; j++) {
                if (expected[i].equals(actual[j])) {
                    found = true;
                    continue;
                }
            }
            assertTrue("actual does not contain " + expected[i] + " element", found);
        }
    }

    private Enumeration<TreePath> createTestEnumeration(final Object[] base) {
        return new Enumeration<TreePath>() {
            private int index;

            public TreePath nextElement() {
                return (TreePath) base[index++];
            }

            public boolean hasMoreElements() {
                return index < base.length;
            }
        };
    }
}
