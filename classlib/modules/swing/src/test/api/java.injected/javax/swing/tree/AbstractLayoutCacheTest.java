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

import java.awt.Rectangle;
import java.util.Enumeration;
import javax.swing.JTree;
import javax.swing.SwingTestCase;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

public class AbstractLayoutCacheTest extends SwingTestCase {
    protected class UndefaultTreeModel implements TreeModel {
        private Object root;

        public UndefaultTreeModel(Object root) {
            this.root = root;
        }

        public Object getChild(Object parent, int i) {
            return ((TreeNode) parent).getChildAt(i);
        }

        public int getChildCount(Object node) {
            return ((TreeNode) node).getChildCount();
        }

        public int getIndexOfChild(Object parent, Object child) {
            if (parent == null || child == null) {
                return -1;
            }
            TreeNode parentNode = (TreeNode) parent;
            int numChildren = parentNode.getChildCount();
            for (int i = 0; i < numChildren; i++) {
                if (child.equals(parentNode.getChildAt(i))) {
                    return i;
                }
            }
            return -1;
        }

        public Object getRoot() {
            return root;
        }

        public boolean isLeaf(Object node) {
            return false;
        }

        public void valueForPathChanged(TreePath path, Object value) {
        }

        public void addTreeModelListener(TreeModelListener l) {
        }

        public void removeTreeModelListener(TreeModelListener l) {
        }
    };

    protected class ConcreteLayoutCache extends AbstractLayoutCache {
        public class FakeNodeDimensions extends AbstractLayoutCache.NodeDimensions {
            @Override
            public Rectangle getNodeDimensions(final Object value, final int row,
                    final int depth, final boolean expanded, final Rectangle placeIn) {
                return new Rectangle(value.hashCode(), row, depth, expanded ? 1 : 0);
            }
        };

        public class NearlyFakeNodeDimensions extends AbstractLayoutCache.NodeDimensions {
            @Override
            public Rectangle getNodeDimensions(final Object value, final int row,
                    final int depth, final boolean expanded, final Rectangle placeIn) {
                return new Rectangle(10000 * depth, 100 * row, 10 * (Math.abs(row) + 1),
                        1000 * (Math.abs(row) + 1));
            }
        };

        public class RealisticNodeDimensions extends AbstractLayoutCache.NodeDimensions {
            @Override
            public Rectangle getNodeDimensions(final Object value, final int row,
                    final int depth, final boolean expanded, final Rectangle placeIn) {
                return new Rectangle(depth * 10, row * 10, 100, 10);
            }
        };

        public NodeDimensions createNodeDimensions(final int type) {
            if (type == 0) {
                return new FakeNodeDimensions();
            } else if (type == 1) {
                return new NearlyFakeNodeDimensions();
            }
            return new RealisticNodeDimensions();
        }

        @Override
        public Rectangle getBounds(TreePath path, Rectangle palceIn) {
            return null;
        }

        @Override
        public boolean getExpandedState(TreePath path) {
            return false;
        }

        @Override
        public TreePath getPathClosestTo(int x, int y) {
            return null;
        }

        @Override
        public TreePath getPathForRow(int row) {
            return null;
        }

        @Override
        public int getRowCount() {
            return 0;
        }

        @Override
        public int getRowForPath(TreePath path) {
            return 111;
        }

        @Override
        public int getVisibleChildCount(TreePath path) {
            return 0;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Enumeration getVisiblePathsFrom(TreePath path) {
            return null;
        }

        @Override
        public void invalidatePathBounds(TreePath path) {
        }

        @Override
        public void invalidateSizes() {
        }

        @Override
        public boolean isExpanded(TreePath path) {
            return false;
        }

        @Override
        public void setExpandedState(TreePath path, boolean expanded) {
        }

        @Override
        public void treeNodesChanged(TreeModelEvent e) {
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
        }
    };

    protected final DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");

    protected final DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("node1");

    protected final DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("node2");

    protected final DefaultMutableTreeNode node3 = new DefaultMutableTreeNode("node3");

    protected final DefaultMutableTreeNode node4 = new DefaultMutableTreeNode("node4");

    protected final DefaultMutableTreeNode node5 = new DefaultMutableTreeNode("node5");

    protected final DefaultMutableTreeNode node6 = new DefaultMutableTreeNode("node6");

    protected final TreePath rootPath = new TreePath(root);

    protected final TreePath path1 = new TreePath(new Object[] { root, node1 });

    protected final TreePath path2 = new TreePath(new Object[] { root, node2 });

    protected final TreePath path3 = new TreePath(new Object[] { root, node3 });

    protected final TreePath path4 = new TreePath(new Object[] { root, node4 });

    protected final TreePath path23 = new TreePath(new Object[] { root, node2, node3 });

    protected final TreePath path24 = new TreePath(new Object[] { root, node2, node4 });

    protected final TreePath path234 = new TreePath(new Object[] { root, node2, node3, node4 });

    protected final TreePath path13 = new TreePath(new Object[] { root, node1, node3 });

    protected final TreePath path14 = new TreePath(new Object[] { root, node1, node4 });

    protected final TreePath path25 = new TreePath(new Object[] { root, node2, node5 });

    protected final TreePath path26 = new TreePath(new Object[] { root, node2, node6 });

    protected AbstractLayoutCache.NodeDimensions dimensions1;

    protected AbstractLayoutCache.NodeDimensions dimensions2;

    protected AbstractLayoutCache.NodeDimensions dimensions3;

    protected int defaultRowValue;

    protected AbstractLayoutCache cache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cache = new ConcreteLayoutCache();
        dimensions1 = ((ConcreteLayoutCache) cache).createNodeDimensions(0);
        dimensions2 = ((ConcreteLayoutCache) cache).createNodeDimensions(2);
        dimensions3 = ((ConcreteLayoutCache) cache).createNodeDimensions(1);
        defaultRowValue = cache.getRowForPath(null);
    }

    @Override
    protected void tearDown() throws Exception {
        cache = null;
        root.removeAllChildren();
        node1.removeAllChildren();
        node2.removeAllChildren();
        node3.removeAllChildren();
        node4.removeAllChildren();
        node5.removeAllChildren();
        node6.removeAllChildren();
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.AbstractLayoutCache()'
     */
    public void testLayoutCache() {
        assertFalse(cache.rootVisible);
        assertNull(cache.nodeDimensions);
        assertEquals(0, cache.rowHeight);
        assertNull(cache.treeModel);
        assertNull(cache.treeSelectionModel);
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.getModel()'
     */
    public void testGetModel() {
        TreeModel model = new DefaultTreeModel(null);
        assertNull(cache.getModel());
        cache.treeModel = model;
        assertEquals(model, cache.getModel());
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.setModel(TreeModel)'
     */
    public void testSetModel() {
        TreeModel model1 = new DefaultTreeModel(null);
        TreeModel model2 = new DefaultTreeModel(root);
        assertNull(cache.getModel());
        cache.setModel(model1);
        assertSame(model1, cache.treeModel);
        assertSame(model1, cache.getModel());
        cache.setModel(model2);
        assertSame(model2, cache.treeModel);
        assertSame(model2, cache.getModel());
        if (getClass() == AbstractLayoutCacheTest.class) {
            return;
        }
        root.add(node1);
        root.add(node2);
        node1.add(node3);
        node1.add(node4);
        node2.add(node5);
        node2.add(node6);
        cache.setRootVisible(true);
        cache.setExpandedState(path1, true);
        assertEquals(0, cache.getRowForPath(rootPath));
        assertEquals(1, cache.getRowForPath(path1));
        assertEquals(2, cache.getRowForPath(path13));
        assertEquals(3, cache.getRowForPath(path14));
        assertEquals(4, cache.getRowForPath(path2));
        assertEquals(-1, cache.getRowForPath(path25));
        assertEquals(-1, cache.getRowForPath(path26));
        cache.setModel(model1);
        assertEquals(-1, cache.getRowForPath(rootPath));
        assertEquals(-1, cache.getRowForPath(path1));
        assertEquals(-1, cache.getRowForPath(path13));
        assertEquals(-1, cache.getRowForPath(path14));
        assertEquals(-1, cache.getRowForPath(path2));
        assertEquals(-1, cache.getRowForPath(path25));
        assertEquals(-1, cache.getRowForPath(path26));
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.getSelectionModel()'
     */
    public void testGetSelectionModel() {
        TreeSelectionModel model = new DefaultTreeSelectionModel();
        assertNull(cache.getSelectionModel());
        cache.treeSelectionModel = model;
        assertEquals(model, cache.getSelectionModel());
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.setSelectionModel(TreeSelectionModel)'
     */
    public void testSetSelectionModel() {
        TreeSelectionModel model1 = new DefaultTreeSelectionModel();
        TreeSelectionModel model2 = new DefaultTreeSelectionModel();
        assertNull(cache.getSelectionModel());
        cache.setSelectionModel(model1);
        assertSame(cache, model1.getRowMapper());
        assertEquals(model1, cache.treeSelectionModel);
        assertEquals(model1, cache.getSelectionModel());
        cache.setSelectionModel(model2);
        assertSame(cache, model2.getRowMapper());
        assertNull(model1.getRowMapper());
        assertEquals(model2, cache.treeSelectionModel);
        assertEquals(model2, cache.getSelectionModel());
        cache.setSelectionModel(null);
        assertNull(cache.getSelectionModel());
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.setRootVisible(boolean)'
     */
    public void testSetRootVisible() {
        cache.setRootVisible(true);
        assertTrue(cache.rootVisible);
        assertTrue(cache.isRootVisible());
        cache.setRootVisible(false);
        assertFalse(cache.rootVisible);
        assertFalse(cache.isRootVisible());
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.isRootVisible()'
     */
    public void testIsRootVisible() {
        cache.rootVisible = true;
        assertTrue(cache.isRootVisible());
        cache.rootVisible = false;
        assertFalse(cache.isRootVisible());
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.setRowHeight(int)'
     */
    public void testSetRowHeight() {
        cache.setRowHeight(100);
        assertEquals(100, cache.rowHeight);
        assertEquals(100, cache.getRowHeight());
        cache.setRowHeight(200);
        assertEquals(200, cache.rowHeight);
        assertEquals(200, cache.getRowHeight());
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.getRowHeight()'
     */
    public void testGetRowHeight() {
        cache.rowHeight = 100;
        assertEquals(100, cache.getRowHeight());
        cache.rowHeight = 200;
        assertEquals(200, cache.getRowHeight());
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.getNodeDimensions(Object, int, int, boolean, Rectangle)'
     */
    public void testGetNodeDimensionsObjectIntIntBooleanRectangle() {
        AbstractLayoutCache.NodeDimensions renderer1 = dimensions1;
        assertNull(cache.getNodeDimensions(null, 1, 1, true, null));
        cache.setNodeDimensions(renderer1);
        assertEquals(new Rectangle("a".hashCode(), 10, 20, 1), cache.getNodeDimensions("a", 10,
                20, true, null));
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.getNodeDimensions()'
     */
    public void testGetNodeDimensions() {
        AbstractLayoutCache.NodeDimensions renderer1 = dimensions1;
        AbstractLayoutCache.NodeDimensions renderer2 = dimensions2;
        cache.nodeDimensions = renderer1;
        assertEquals(renderer1, cache.getNodeDimensions());
        cache.nodeDimensions = renderer2;
        assertEquals(renderer2, cache.getNodeDimensions());
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.setNodeDimensions(NodeDimensions)'
     */
    public void testSetNodeDimensions() {
        AbstractLayoutCache.NodeDimensions renderer1 = dimensions1;
        AbstractLayoutCache.NodeDimensions renderer2 = dimensions2;
        cache.setNodeDimensions(renderer1);
        assertSame(renderer1, cache.nodeDimensions);
        assertSame(renderer1, cache.getNodeDimensions());
        cache.setNodeDimensions(renderer2);
        assertSame(renderer2, cache.nodeDimensions);
        assertSame(renderer2, cache.getNodeDimensions());
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.getPreferredHeight()'
     */
    public void testGetPreferredHeight() {
        assertEquals(0, cache.getPreferredHeight());
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.getPreferredWidth(Rectangle)'
     */
    public void testGetPreferredWidth() {
        assertEquals(0, cache.getPreferredWidth(null));
        assertEquals(0, cache.getPreferredWidth(new Rectangle(0, 0, 20, 20)));
        JTree tree = new JTree();
        cache.setModel(tree.getModel());
        cache.setRootVisible(true);
        cache.setRowHeight(10);
        cache.setNodeDimensions(dimensions1);
        assertEquals(0, cache.getPreferredWidth(null));
        assertEquals(0, cache.getPreferredWidth(new Rectangle(0, 0, 20, 20)));
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.getRowsForPaths(TreePath[])'
     */
    public void testGetRowsForPaths() {
        if (!isHarmony()) {
            assertNull(cache.getRowsForPaths(null));
        } else {
            assertEquals(0, cache.getRowsForPaths(null).length);
        }
        assertEquals(0, cache.getRowsForPaths(new TreePath[0]).length);
        TreePath[] paths = new TreePath[] { new TreePath("1"), new TreePath("2"),
                new TreePath(new Object[] { "1", "2" }) };
        assertEquals(3, cache.getRowsForPaths(paths).length);
        assertEquals(defaultRowValue, cache.getRowsForPaths(paths)[0]);
        assertEquals(defaultRowValue, cache.getRowsForPaths(paths)[1]);
        assertEquals(defaultRowValue, cache.getRowsForPaths(paths)[2]);
    }

    /*
     * Test method for 'javax.swing.tree.AbstractLayoutCache.isFixedRowHeight()'
     */
    public void testIsFixedRowHeight() {
        assertFalse(cache.isFixedRowHeight());
        cache.setRowHeight(1);
        assertTrue(cache.isFixedRowHeight());
        cache.setRowHeight(0);
        assertFalse(cache.isFixedRowHeight());
        cache.setRowHeight(-10);
        assertFalse(cache.isFixedRowHeight());
    }

    @SuppressWarnings("unchecked")
    protected boolean checkEnumeration(final Enumeration e, final Object[] array) {
        int size = (array != null) ? array.length : 0;
        boolean enumEmpty = (e == null) || (!e.hasMoreElements());
        boolean arrayEmpty = (size == 0);
        if (enumEmpty || arrayEmpty) {
            //            if (enumEmpty != arrayEmpty) {
            //                System.out.println("sizes: " + array.length);
            //            }
            return (enumEmpty == arrayEmpty);
        }
        int i = 0;
        while (e.hasMoreElements()) {
            Object element = e.nextElement();
            if (i >= size || !element.equals(array[i++])) {
                return false;
            }
        }
        return (i == size);
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.setExpandedState(TreePath, boolean)'
     */
    public void testSetExpandedState() {
        if (cache instanceof ConcreteLayoutCache) {
            return;
        }
        root.add(node1);
        root.add(node2);
        node2.add(node3);
        node3.add(node4);
        TreeModel model = new DefaultTreeModel(root);
        cache.setModel(model);
        cache.setExpandedState(rootPath, false);
        assertFalse(cache.getExpandedState(rootPath));
        assertFalse(cache.getExpandedState(path1));
        assertFalse(cache.getExpandedState(path2));
        assertFalse(cache.getExpandedState(path23));
        cache.setExpandedState(path1, true);
        assertTrue(cache.getExpandedState(rootPath));
        assertFalse(cache.getExpandedState(path1));
        assertFalse(cache.getExpandedState(path2));
        assertFalse(cache.getExpandedState(path23));
        cache.setExpandedState(rootPath, false);
        cache.setExpandedState(path2, true);
        assertTrue(cache.getExpandedState(rootPath));
        assertFalse(cache.getExpandedState(path1));
        assertTrue(cache.getExpandedState(path2));
        assertFalse(cache.getExpandedState(path23));
        cache.setExpandedState(rootPath, false);
        assertFalse(cache.getExpandedState(rootPath));
        assertFalse(cache.getExpandedState(path1));
        assertFalse(cache.getExpandedState(path2));
        assertFalse(cache.getExpandedState(path23));
        cache.setExpandedState(rootPath, true);
        assertTrue(cache.getExpandedState(rootPath));
        assertFalse(cache.getExpandedState(path1));
        assertTrue(cache.getExpandedState(path2));
        assertFalse(cache.getExpandedState(path23));
        cache.setExpandedState(rootPath, false);
        cache.setExpandedState(path2, false);
        cache.setExpandedState(path234, true);
        assertTrue(cache.getExpandedState(rootPath));
        assertFalse(cache.getExpandedState(path1));
        assertTrue(cache.getExpandedState(path2));
        assertTrue(cache.getExpandedState(path23));
        assertFalse(cache.getExpandedState(path234));
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.getExpandedState(TreePath)'
     */
    public void testGetExpandedState() {
        if (cache instanceof ConcreteLayoutCache) {
            return;
        }
        root.add(node1);
        root.add(node2);
        node2.add(node3);
        assertFalse(cache.getExpandedState(rootPath));
        TreeModel model = new DefaultTreeModel(root);
        cache.setModel(model);
        assertTrue(cache.getExpandedState(rootPath));
        assertFalse(cache.getExpandedState(path1));
        assertFalse(cache.getExpandedState(path2));
        cache.setExpandedState(path1, true);
        assertTrue(cache.getExpandedState(rootPath));
        assertFalse(cache.getExpandedState(path1));
        assertFalse(cache.getExpandedState(path2));
        cache.setExpandedState(path2, true);
        assertTrue(cache.getExpandedState(rootPath));
        assertFalse(cache.getExpandedState(path1));
        assertTrue(cache.getExpandedState(path2));
        cache.setRootVisible(false);
        assertTrue(cache.getExpandedState(rootPath));
        assertFalse(cache.getExpandedState(path1));
        assertTrue(cache.getExpandedState(path2));
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.isExpanded(TreePath)'
     */
    public void testIsExpanded() {
        if (cache instanceof ConcreteLayoutCache) {
            return;
        }
        root.add(node1);
        root.add(node2);
        node2.add(node3);
        assertFalse(cache.isExpanded(rootPath));
        TreeModel model = new DefaultTreeModel(root);
        cache.setModel(model);
        assertTrue(cache.isExpanded(rootPath));
        assertFalse(cache.isExpanded(path1));
        assertFalse(cache.isExpanded(path2));
        cache.setExpandedState(path1, true);
        assertTrue(cache.isExpanded(rootPath));
        assertFalse(cache.isExpanded(path1));
        assertFalse(cache.isExpanded(path2));
        cache.setExpandedState(path2, true);
        assertTrue(cache.isExpanded(rootPath));
        assertFalse(cache.isExpanded(path1));
        assertTrue(cache.isExpanded(path2));
        cache.setExpandedState(rootPath, false);
        assertFalse(cache.isExpanded(rootPath));
        assertFalse(cache.isExpanded(path1));
        assertFalse(cache.isExpanded(path2));
        cache.setExpandedState(path1, true);
        assertTrue(cache.isExpanded(rootPath));
        assertFalse(cache.isExpanded(path1));
        assertTrue(cache.isExpanded(path2));
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.getPathForRow(int)'
     */
    public void testGetPathForRow() {
        if (cache instanceof ConcreteLayoutCache) {
            return;
        }
        root.add(node1);
        root.add(node2);
        node1.add(node3);
        node1.add(node4);
        node2.add(node5);
        node2.add(node6);
        assertNull(cache.getPathForRow(0));
        cache.setRootVisible(true);
        assertNull(cache.getPathForRow(0));
        TreeModel model = new UndefaultTreeModel(root);
        cache.setModel(model);
        cache.setRootVisible(false);
        assertEquals(path1, cache.getPathForRow(0));
        assertEquals(path2, cache.getPathForRow(1));
        assertNull(cache.getPathForRow(2));
        cache.setRootVisible(true);
        assertEquals(rootPath, cache.getPathForRow(0));
        assertEquals(path1, cache.getPathForRow(1));
        assertEquals(path2, cache.getPathForRow(2));
        assertNull(cache.getPathForRow(3));
        cache.setExpandedState(path1, true);
        assertEquals(rootPath, cache.getPathForRow(0));
        assertEquals(path1, cache.getPathForRow(1));
        assertEquals(path13, cache.getPathForRow(2));
        assertEquals(path14, cache.getPathForRow(3));
        assertEquals(path2, cache.getPathForRow(4));
        assertNull(cache.getPathForRow(5));
        cache.setExpandedState(path1, false);
        cache.setExpandedState(path2, true);
        assertEquals(rootPath, cache.getPathForRow(0));
        assertEquals(path1, cache.getPathForRow(1));
        assertEquals(path2, cache.getPathForRow(2));
        assertEquals(path25, cache.getPathForRow(3));
        assertEquals(path26, cache.getPathForRow(4));
        assertNull(cache.getPathForRow(5));
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.getRowCount()'
     */
    public void testGetRowCount() {
        if (cache instanceof ConcreteLayoutCache) {
            return;
        }
        root.add(node1);
        root.add(node2);
        TreeModel model = new DefaultTreeModel(root);
        assertEquals(0, cache.getRowCount());
        cache.setModel(model);
        assertEquals(2, cache.getRowCount());
        cache.setRootVisible(true);
        assertEquals(3, cache.getRowCount());
        cache.setExpandedState(rootPath, false);
        assertEquals(1, cache.getRowCount());
        cache.setRootVisible(false);
        assertEquals(0, cache.getRowCount());
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.getRowForPath(TreePath)'
     */
    public void testGetRowForPath() {
        if (cache instanceof ConcreteLayoutCache) {
            return;
        }
        root.add(node1);
        root.add(node2);
        node1.add(node3);
        node1.add(node4);
        node2.add(node5);
        node2.add(node6);
        assertEquals(-1, cache.getRowForPath(null));
        assertEquals(-1, cache.getRowForPath(rootPath));
        cache.setRootVisible(true);
        assertEquals(-1, cache.getRowForPath(rootPath));
        TreeModel model = new UndefaultTreeModel(root);
        cache.setModel(model);
        cache.setRootVisible(false);
        assertEquals(-1, cache.getRowForPath(rootPath));
        assertEquals(0, cache.getRowForPath(path1));
        assertEquals(1, cache.getRowForPath(path2));
        assertEquals(-1, cache.getRowForPath(path13));
        assertEquals(-1, cache.getRowForPath(path14));
        assertEquals(-1, cache.getRowForPath(path25));
        assertEquals(-1, cache.getRowForPath(path26));
        cache.setRootVisible(true);
        assertEquals(0, cache.getRowForPath(rootPath));
        assertEquals(1, cache.getRowForPath(path1));
        assertEquals(2, cache.getRowForPath(path2));
        assertEquals(-1, cache.getRowForPath(path13));
        assertEquals(-1, cache.getRowForPath(path14));
        assertEquals(-1, cache.getRowForPath(path25));
        assertEquals(-1, cache.getRowForPath(path26));
        cache.setExpandedState(path1, true);
        assertEquals(0, cache.getRowForPath(rootPath));
        assertEquals(1, cache.getRowForPath(path1));
        assertEquals(2, cache.getRowForPath(path13));
        assertEquals(3, cache.getRowForPath(path14));
        assertEquals(4, cache.getRowForPath(path2));
        assertEquals(-1, cache.getRowForPath(path25));
        assertEquals(-1, cache.getRowForPath(path26));
        cache.setExpandedState(path1, false);
        cache.setExpandedState(path2, true);
        assertEquals(0, cache.getRowForPath(rootPath));
        assertEquals(1, cache.getRowForPath(path1));
        assertEquals(-1, cache.getRowForPath(path13));
        assertEquals(-1, cache.getRowForPath(path14));
        assertEquals(2, cache.getRowForPath(path2));
        assertEquals(3, cache.getRowForPath(path25));
        assertEquals(4, cache.getRowForPath(path26));
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.getVisibleChildCount(TreePath)'
     */
    public void testGetVisibleChildCount() {
        if (cache instanceof ConcreteLayoutCache) {
            return;
        }
        root.add(node1);
        root.add(node2);
        node2.add(node3);
        node2.add(node4);
        TreeModel model = new DefaultTreeModel(root);
        assertEquals(0, cache.getVisibleChildCount(rootPath));
        cache.setModel(model);
        assertEquals(2, cache.getVisibleChildCount(rootPath));
        assertEquals(0, cache.getVisibleChildCount(path1));
        assertEquals(0, cache.getVisibleChildCount(path2));
        cache.setExpandedState(rootPath, false);
        assertEquals(0, cache.getVisibleChildCount(rootPath));
        assertEquals(0, cache.getVisibleChildCount(path1));
        assertEquals(0, cache.getVisibleChildCount(path2));
        cache.setExpandedState(path2, true);
        assertEquals(4, cache.getVisibleChildCount(rootPath));
        assertEquals(0, cache.getVisibleChildCount(path1));
        assertEquals(2, cache.getVisibleChildCount(path2));
        cache.setExpandedState(rootPath, false);
        assertEquals(0, cache.getVisibleChildCount(rootPath));
        assertEquals(0, cache.getVisibleChildCount(path1));
        assertEquals(0, cache.getVisibleChildCount(path2));
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.getVisiblePathsFrom(TreePath)'
     */
    public void testGetVisiblePathsFrom() {
        if (cache instanceof ConcreteLayoutCache) {
            return;
        }
        final TreePath path5 = new TreePath(new Object[] { root, node5 });
        root.add(node1);
        root.add(node2);
        node2.add(node3);
        node2.add(node4);
        root.add(node5);
        assertTrue(checkEnumeration(cache.getVisiblePathsFrom(rootPath), new Object[] {}));
        TreeModel model = new DefaultTreeModel(root);
        cache.setModel(model);
        assertTrue(checkEnumeration(cache.getVisiblePathsFrom(rootPath), new Object[] {
                rootPath, path1, path2, path5 }));
        assertTrue(checkEnumeration(cache.getVisiblePathsFrom(path1), new Object[] { path1,
                path2, path5 }));
        assertTrue(checkEnumeration(cache.getVisiblePathsFrom(path2), new Object[] { path2,
                path5 }));
        cache.setExpandedState(rootPath, false);
        assertTrue(checkEnumeration(cache.getVisiblePathsFrom(rootPath),
                new Object[] { rootPath }));
        assertNull(cache.getVisiblePathsFrom(path1));
        assertNull(cache.getVisiblePathsFrom(path2));
        cache.setRootVisible(false);
        assertTrue(checkEnumeration(cache.getVisiblePathsFrom(rootPath),
                new Object[] { rootPath }));
        assertNull(cache.getVisiblePathsFrom(path1));
        assertNull(cache.getVisiblePathsFrom(path2));
        cache.setRootVisible(true);
        cache.setExpandedState(path2, true);
        assertTrue(checkEnumeration(cache.getVisiblePathsFrom(rootPath), new Object[] {
                rootPath, path1, path2, path23, path24, path5 }));
        assertTrue(checkEnumeration(cache.getVisiblePathsFrom(path1), new Object[] { path1,
                path2, path23, path24, path5 }));
        assertTrue(checkEnumeration(cache.getVisiblePathsFrom(path2), new Object[] { path2,
                path23, path24, path5 }));
        assertTrue(checkEnumeration(cache.getVisiblePathsFrom(path23), new Object[] { path23,
                path24, path5 }));
        cache.setExpandedState(rootPath, false);
        assertTrue(checkEnumeration(cache.getVisiblePathsFrom(rootPath),
                new Object[] { rootPath }));
        assertNull(cache.getVisiblePathsFrom(path1));
        assertNull(cache.getVisiblePathsFrom(path2));
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.treeNodesChanged(TreeModelEvent)'
     */
    public void testTreeNodesChanged() {
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.treeNodesInserted(TreeModelEvent)'
     */
    public void testTreeNodesInserted() {
        if (cache instanceof ConcreteLayoutCache) {
            return;
        }
        root.add(node1);
        TreeModel model = new UndefaultTreeModel(root);
        cache.setModel(model);
        assertEquals(0, cache.getRowForPath(path1));
        assertEquals(-1, cache.getRowForPath(path2));
        root.insert(node2, 0);
        cache.treeNodesInserted(new TreeModelEvent(model, rootPath, new int[] { 0 },
                new Object[] { node2 }));
        assertEquals(0, cache.getRowForPath(path2));
        assertEquals(-1, cache.getRowForPath(path25));
        assertEquals(-1, cache.getRowForPath(path26));
        assertEquals(1, cache.getRowForPath(path1));
        assertEquals(-1, cache.getRowForPath(path13));
        assertEquals(-1, cache.getRowForPath(path14));
        node2.add(node6);
        cache.treeNodesInserted(new TreeModelEvent(model, path2, new int[] { 0 }, null));
        assertEquals(0, cache.getRowForPath(path2));
        assertEquals(-1, cache.getRowForPath(path25));
        assertEquals(-1, cache.getRowForPath(path26));
        assertEquals(1, cache.getRowForPath(path1));
        assertEquals(-1, cache.getRowForPath(path13));
        assertEquals(-1, cache.getRowForPath(path14));
        cache.setExpandedState(path2, true);
        node2.insert(node5, 0);
        cache.treeNodesInserted(new TreeModelEvent(model, path2, new int[] { 0 }, null));
        assertEquals(0, cache.getRowForPath(path2));
        assertEquals(1, cache.getRowForPath(path25));
        assertEquals(2, cache.getRowForPath(path26));
        assertEquals(3, cache.getRowForPath(path1));
        assertEquals(-1, cache.getRowForPath(path13));
        assertEquals(-1, cache.getRowForPath(path14));
        node1.add(node3);
        cache.treeNodesInserted(new TreeModelEvent(model, path1, new int[] { 0 },
                new Object[] { node3 }));
        assertEquals(0, cache.getRowForPath(path2));
        assertEquals(1, cache.getRowForPath(path25));
        assertEquals(2, cache.getRowForPath(path26));
        assertEquals(3, cache.getRowForPath(path1));
        assertEquals(-1, cache.getRowForPath(path13));
        assertEquals(-1, cache.getRowForPath(path14));
        cache.setExpandedState(path1, true);
        node1.add(node4);
        cache.treeNodesInserted(new TreeModelEvent(model, path1, new int[] { 1 },
                new Object[] { node4 }));
        assertEquals(0, cache.getRowForPath(path2));
        assertEquals(1, cache.getRowForPath(path25));
        assertEquals(2, cache.getRowForPath(path26));
        assertEquals(3, cache.getRowForPath(path1));
        assertEquals(4, cache.getRowForPath(path13));
        assertEquals(5, cache.getRowForPath(path14));
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.treeNodesRemoved(TreeModelEvent)'
     */
    public void testTreeNodesRemoved() {
        if (cache instanceof ConcreteLayoutCache) {
            return;
        }
        TreeModel model = new UndefaultTreeModel(root);
        root.add(node1);
        root.add(node2);
        node1.add(node3);
        node1.add(node4);
        node2.add(node5);
        node2.add(node6);
        cache.setModel(model);
        cache.setExpandedState(path1, true);
        cache.setExpandedState(path2, true);
        assertEquals(0, cache.getRowForPath(path1));
        assertEquals(1, cache.getRowForPath(path13));
        assertEquals(2, cache.getRowForPath(path14));
        assertEquals(3, cache.getRowForPath(path2));
        assertEquals(4, cache.getRowForPath(path25));
        assertEquals(5, cache.getRowForPath(path26));
        node1.remove(0);
        cache.treeNodesRemoved(new TreeModelEvent(model, path1, new int[] { 0 },
                new Object[] { node1 }));
        assertEquals(0, cache.getRowForPath(path1));
        //        assertEquals(-1, cache.getRowForPath(path13));
        assertEquals(1, cache.getRowForPath(path14));
        assertEquals(2, cache.getRowForPath(path2));
        assertEquals(3, cache.getRowForPath(path25));
        assertEquals(4, cache.getRowForPath(path26));
        assertTrue(cache.getExpandedState(path1));
        node1.remove(0);
        cache.treeNodesRemoved(new TreeModelEvent(model, path1, new int[] { 0 },
                new Object[] { node4 }));
        assertEquals(0, cache.getRowForPath(path1));
        //        assertEquals(-1, cache.getRowForPath(path13));
        //        assertEquals(-1, cache.getRowForPath(path14));
        assertEquals(1, cache.getRowForPath(path2));
        assertEquals(2, cache.getRowForPath(path25));
        assertEquals(3, cache.getRowForPath(path26));
        assertTrue(cache.getExpandedState(path1));
        root.remove(0);
        cache.treeNodesRemoved(new TreeModelEvent(model, rootPath, new int[] { 0 },
                new Object[] { node1 }));
        assertEquals(-1, cache.getRowForPath(path1));
        assertEquals(-1, cache.getRowForPath(path13));
        assertEquals(-1, cache.getRowForPath(path14));
        assertEquals(0, cache.getRowForPath(path2));
        assertEquals(1, cache.getRowForPath(path25));
        assertEquals(2, cache.getRowForPath(path26));
        if (isHarmony()) {
            assertFalse(cache.getExpandedState(path1));
        }
        root.remove(0);
        cache.treeNodesRemoved(new TreeModelEvent(model, rootPath, new int[] { 0 },
                new Object[] { node2 }));
        if (isHarmony()) {
            assertEquals(-1, cache.getRowForPath(path1));
        }
        assertEquals(-1, cache.getRowForPath(path13));
        assertEquals(-1, cache.getRowForPath(path14));
        if (isHarmony()) {
            assertEquals(-1, cache.getRowForPath(path2));
        }
        assertEquals(-1, cache.getRowForPath(path25));
        assertEquals(-1, cache.getRowForPath(path26));
        if (isHarmony()) {
            assertFalse(cache.getExpandedState(path2));
        }
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.treeStructureChanged(TreeModelEvent)'
     */
    public void testTreeStructureChanged() {
        if (cache instanceof ConcreteLayoutCache) {
            return;
        }
        TreeModel model = new UndefaultTreeModel(root);
        root.add(node1);
        root.add(node2);
        node2.add(node5);
        cache.setModel(model);
        assertEquals(0, cache.getRowForPath(path1));
        assertEquals(1, cache.getRowForPath(path2));
        assertEquals(-1, cache.getRowForPath(path25));
        assertEquals(-1, cache.getRowForPath(path26));
        root.add(node3);
        root.add(node4);
        cache.treeStructureChanged(new TreeModelEvent(model, path1));
        assertEquals(0, cache.getRowForPath(path1));
        assertEquals(1, cache.getRowForPath(path2));
        assertEquals(-1, cache.getRowForPath(path25));
        assertEquals(-1, cache.getRowForPath(path26));
        cache.treeStructureChanged(new TreeModelEvent(model, rootPath));
        assertEquals(0, cache.getRowForPath(path1));
        assertEquals(1, cache.getRowForPath(path2));
        assertEquals(2, cache.getRowForPath(path3));
        assertEquals(3, cache.getRowForPath(path4));
        assertEquals(-1, cache.getRowForPath(path25));
        assertEquals(-1, cache.getRowForPath(path26));
        cache.setExpandedState(path2, true);
        node2.add(node6);
        assertEquals(0, cache.getRowForPath(path1));
        assertEquals(1, cache.getRowForPath(path2));
        assertEquals(2, cache.getRowForPath(path25));
        if (!isHarmony()) {
            assertEquals(3, cache.getRowForPath(path3));
            assertEquals(4, cache.getRowForPath(path4));
        }
        cache.treeStructureChanged(new TreeModelEvent(model, path2));
        assertEquals(0, cache.getRowForPath(path1));
        assertEquals(1, cache.getRowForPath(path2));
        assertEquals(2, cache.getRowForPath(path25));
        assertEquals(3, cache.getRowForPath(path26));
        assertEquals(4, cache.getRowForPath(path3));
        assertEquals(5, cache.getRowForPath(path4));
        cache.treeStructureChanged(new TreeModelEvent(model, rootPath));
        assertEquals(0, cache.getRowForPath(path1));
        assertEquals(1, cache.getRowForPath(path2));
        assertEquals(-1, cache.getRowForPath(path25));
        assertEquals(-1, cache.getRowForPath(path26));
        assertEquals(2, cache.getRowForPath(path3));
        assertEquals(3, cache.getRowForPath(path4));
    }

    public void testInvalidateSizes() {
        if (cache instanceof ConcreteLayoutCache) {
            return;
        }
        root.add(node1);
        root.add(node2);
        node2.add(node3);
        node2.add(node4);
        TreeModel model = new DefaultTreeModel(root);
        cache.setModel(model);
        cache.setExpandedState(path2, true);
        cache.invalidateSizes();
    }
}
