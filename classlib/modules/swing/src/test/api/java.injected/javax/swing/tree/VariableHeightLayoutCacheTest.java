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

public class VariableHeightLayoutCacheTest extends AbstractLayoutCacheTest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cache = new VariableHeightLayoutCache();
        defaultRowValue = cache.getRowForPath(null);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.tree.VariableHeightLayoutCache.VariableHeightLayoutCache()'
     */
    @Override
    public void testLayoutCache() {
        assertFalse(cache.rootVisible);
        assertNull(cache.nodeDimensions);
        assertEquals(0, cache.rowHeight);
        assertNull(cache.treeModel);
        assertNull(cache.treeSelectionModel);
    }

    /*
     * Test method for 'javax.swing.tree.VariableHeightLayoutCache.setRootVisible(boolean)'
     */
    @Override
    public void testSetRootVisible() {
        super.testSetRootVisible();
        int height = 1111;
        cache.setRowHeight(height);
        TreeModel model = new UndefaultTreeModel(root);
        root.add(node1);
        root.add(node2);
        cache.setModel(model);
        cache.setNodeDimensions(dimensions2);
        if (isHarmony()) {
            assertEquals(new Rectangle(0, -1111, 100, 1111), cache.getBounds(rootPath, null));
        } else {
            assertEquals(new Rectangle(0, -1, 100, 1111), cache.getBounds(rootPath, null));
        }
        assertEquals(new Rectangle(10, 0 * height, 100, height), cache.getBounds(path1, null));
        assertEquals(new Rectangle(10, 1 * height, 100, height), cache.getBounds(path2, null));
        cache.setRootVisible(true);
        assertEquals(new Rectangle(0, 0, 100, 1111), cache.getBounds(rootPath, null));
        assertEquals(new Rectangle(10, 1 * height, 100, height), cache.getBounds(path1, null));
        assertEquals(new Rectangle(10, 2 * height, 100, height), cache.getBounds(path2, null));
    }

    /*
     * Test method for 'javax.swing.tree.VariableHeightLayoutCache.setRowHeight(int)'
     */
    @Override
    public void testSetRowHeight() {
        cache.setRowHeight(100);
        assertEquals(100, cache.getRowHeight());
        cache.setRowHeight(200);
        assertEquals(200, cache.getRowHeight());
        cache.setRowHeight(-1);
        assertEquals(-1, cache.getRowHeight());
    }

    /*
     * Test method for 'javax.swing.tree.VariableHeightLayoutCache.getPreferredHeight()'
     */
    @Override
    public void testGetPreferredHeight() {
        int height = 1111;
        TreeModel model = new UndefaultTreeModel(root);
        root.add(node1);
        root.add(node2);
        cache.setRowHeight(height);
        assertEquals(0, cache.getPreferredHeight());
        cache.setModel(model);
        assertEquals(2222, cache.getPreferredHeight());
        cache.setRowHeight(-100);
        assertEquals(0, cache.getPreferredHeight());
        cache.setRowHeight(height);
        cache.setNodeDimensions(dimensions3);
        assertEquals(2222, cache.getPreferredHeight());
        cache.setRootVisible(true);
        assertEquals(3333, cache.getPreferredHeight());
        cache.setRowHeight(-100);
        assertEquals(6000, cache.getPreferredHeight());
        cache.setRootVisible(false);
        if (isHarmony()) {
            assertEquals(3000, cache.getPreferredHeight());
        } else {
            assertEquals(5000, cache.getPreferredHeight());
        }
    }

    /*
     * Test method for 'javax.swing.tree.VariableHeightLayoutCache.getPreferredWidth(Rectangle)'
     */
    @Override
    public void testGetPreferredWidth() {
        int height = 1111;
        TreeModel model = new DefaultTreeModel(root);
        root.add(node1);
        root.add(node2);
        node1.add(node3);
        node1.add(node4);
        node2.add(node5);
        node2.add(node6);
        cache.setRowHeight(height);
        assertEquals(0, cache.getPreferredWidth(null));
        cache.setModel(model);
        assertEquals(0, cache.getPreferredWidth(null));
        cache.setRowHeight(-100);
        assertEquals(0, cache.getPreferredWidth(null));
        cache.setNodeDimensions(dimensions2);
        assertEquals(110, cache.getPreferredWidth(new Rectangle(0, 0, 0, 0)));
        assertEquals(20, cache.getPreferredHeight());
        assertEquals(110, cache.getPreferredWidth(new Rectangle(0, 3010, 10, 10)));
        cache.setRootVisible(true);
        assertEquals(110, cache.getPreferredWidth(new Rectangle(0, 0, 50, 100)));
        cache.setExpandedState(path1, true);
        if (!isHarmony()) {
            assertEquals(120, cache.getPreferredWidth(new Rectangle(0, 0, 0, 0)));
        } else {
            assertEquals(100, cache.getPreferredWidth(new Rectangle(0, 0, 0, 0)));
        }
        assertEquals(120, cache.getPreferredWidth(null));
        cache.setExpandedState(path1, false);
        cache.setExpandedState(path2, true);
        if (!isHarmony()) {
            assertEquals(120, cache.getPreferredWidth(new Rectangle(0, 0, 0, 0)));
        } else {
            assertEquals(100, cache.getPreferredWidth(new Rectangle(0, 0, 0, 0)));
        }
        assertEquals(120, cache.getPreferredWidth(null));
    }

    /*
     * Test method for 'javax.swing.tree.VariableHeightLayoutCache.getBounds(TreePath, Rectangle)'
     */
    public void testGetBounds() {
        int height = 1111;
        cache.setRowHeight(height);
        TreeModel model = new UndefaultTreeModel(root);
        root.add(node1);
        root.add(node2);
        node1.add(node3);
        node1.add(node4);
        node2.add(node5);
        node2.add(node6);
        cache.setModel(model);
        cache.setExpandedState(rootPath, false);
        assertNull(cache.getBounds(path1, new Rectangle()));
        cache.setExpandedState(path1, true);
        cache.setExpandedState(path2, true);
        cache.setRowHeight(10);
        if (isHarmony()) {
            assertEquals(new Rectangle(0, -10, 0, 10), cache.getBounds(rootPath, null));
        } else {
            assertEquals(new Rectangle(0, -1, 0, 10), cache.getBounds(rootPath, null));
        }
        assertEquals(new Rectangle(0, 0, 0, 10), cache.getBounds(path1, null));
        assertEquals(new Rectangle(0, 30, 0, 10), cache.getBounds(path2, null));
        assertEquals(new Rectangle(0, 50, 0, 10), cache.getBounds(path26, null));
        cache.setRowHeight(-10);
        assertEquals(new Rectangle(0, 0, 0, 0), cache.getBounds(path1, null));
        assertEquals(new Rectangle(0, 0, 0, 0), cache.getBounds(path2, null));
        assertEquals(new Rectangle(0, 0, 0, 0), cache.getBounds(path26, null));
        cache.setNodeDimensions(dimensions1);
        assertEquals(new Rectangle(root.hashCode(), 0, 0, 1), cache.getBounds(rootPath, null));
        assertEquals(new Rectangle(node1.hashCode(), 0, 1, 1), cache.getBounds(path1, null));
        assertEquals(new Rectangle(node2.hashCode(), 1, 1, 1), cache.getBounds(path2, null));
        //        assertEquals(new Rectangle(0, 1, 0, 0), cache.getBounds(path13, null));
        //        assertEquals(new Rectangle(0, 1, 0, 0), cache.getBounds(path14, null));
        //        assertEquals(new Rectangle(0, 2, 0, 0), cache.getBounds(path25, null));
        //        assertEquals(new Rectangle(0, 2, 0, 0), cache.getBounds(path26, null));
        cache.setRowHeight(-110);
        cache.setNodeDimensions(dimensions3);
        assertEquals(new Rectangle(0, 0, 20, 2000), cache.getBounds(rootPath, null));
        assertEquals(new Rectangle(10000, 0, 10, 1000), cache.getBounds(path1, null));
        assertEquals(new Rectangle(10000, 6000, 40, 4000), cache.getBounds(path2, null));
        assertEquals(new Rectangle(20000, 1000, 20, 2000), cache.getBounds(path13, null));
        assertEquals(new Rectangle(20000, 3000, 30, 3000), cache.getBounds(path14, null));
        assertEquals(new Rectangle(20000, 10000, 50, 5000), cache.getBounds(path25, null));
        assertEquals(new Rectangle(20000, 15000, 60, 6000), cache.getBounds(path26, null));
        cache.setModel(null);
        assertNull(cache.getBounds(path1, null));
    }

    /*
     * Test method for 'javax.swing.tree.VariableHeightLayoutCache.getPathClosestTo(int, int)'
     */
    public void testGetPathClosestTo() {
        int height = 10;
        cache.setRowHeight(height);
        TreeModel model = new UndefaultTreeModel(root);
        root.add(node1);
        root.add(node2);
        node1.add(node3);
        node1.add(node4);
        node2.add(node5);
        node2.add(node6);
        assertNull(cache.getPathClosestTo(-100, -100));
        assertNull(cache.getPathClosestTo(0, 0));
        assertNull(cache.getPathClosestTo(100, 100));
        cache.setModel(model);
        cache.setExpandedState(rootPath, false);
        assertNull(cache.getPathClosestTo(-100, -100));
        assertNull(cache.getPathClosestTo(0, 0));
        assertNull(cache.getPathClosestTo(100, 100));
        cache.setRootVisible(true);
        assertEquals(rootPath, cache.getPathClosestTo(-100, -100));
        assertEquals(rootPath, cache.getPathClosestTo(0, 0));
        assertEquals(rootPath, cache.getPathClosestTo(100, 100));
        cache.setExpandedState(rootPath, true);
        assertEquals(rootPath, cache.getPathClosestTo(-100, -100));
        assertEquals(rootPath, cache.getPathClosestTo(0, 0));
        assertEquals(path2, cache.getPathClosestTo(100, 100));
        cache.setExpandedState(path1, true);
        cache.setExpandedState(path2, true);
        assertEquals(rootPath, cache.getPathClosestTo(-100, -100));
        assertEquals(rootPath, cache.getPathClosestTo(0, 0));
        assertEquals(path13, cache.getPathClosestTo(0, 20));
        assertEquals(path2, cache.getPathClosestTo(0, 40));
        assertEquals(path26, cache.getPathClosestTo(0, 60));
        assertEquals(path26, cache.getPathClosestTo(0, 80));
        assertEquals(rootPath, cache.getPathClosestTo(120, 0));
        assertEquals(path13, cache.getPathClosestTo(120, 20));
        assertEquals(path2, cache.getPathClosestTo(120, 40));
        assertEquals(path26, cache.getPathClosestTo(120, 60));
        assertEquals(path26, cache.getPathClosestTo(120, 80));
        cache.setRowHeight(-100);
        cache.setNodeDimensions(dimensions2);
        assertEquals(rootPath, cache.getPathClosestTo(-100, -100));
        assertEquals(rootPath, cache.getPathClosestTo(0, 0));
        assertEquals(path26, cache.getPathClosestTo(100, 100));
        cache.setModel(model);
        cache.setRootVisible(false);
        cache.setExpandedState(rootPath, false);
        assertNull(cache.getPathClosestTo(-100, -100));
        assertNull(cache.getPathClosestTo(0, 0));
        assertNull(cache.getPathClosestTo(100, 100));
        cache.setRootVisible(true);
        assertEquals(rootPath, cache.getPathClosestTo(-100, -100));
        assertEquals(rootPath, cache.getPathClosestTo(0, 0));
        assertEquals(rootPath, cache.getPathClosestTo(100, 100));
        cache.setExpandedState(rootPath, true);
        assertEquals(rootPath, cache.getPathClosestTo(-100, -100));
        assertEquals(rootPath, cache.getPathClosestTo(0, 0));
        assertEquals(path2, cache.getPathClosestTo(100, 100));
        cache.setExpandedState(path1, true);
        cache.setExpandedState(path2, true);
        assertEquals(rootPath, cache.getPathClosestTo(-100, -100));
        assertEquals(rootPath, cache.getPathClosestTo(0, 0));
        assertEquals(path13, cache.getPathClosestTo(0, 20));
        assertEquals(path2, cache.getPathClosestTo(0, 40));
        assertEquals(path26, cache.getPathClosestTo(0, 60));
        assertEquals(path26, cache.getPathClosestTo(0, 80));
        assertEquals(rootPath, cache.getPathClosestTo(120, 0));
        assertEquals(path13, cache.getPathClosestTo(120, 20));
        assertEquals(path2, cache.getPathClosestTo(120, 40));
        assertEquals(path26, cache.getPathClosestTo(120, 60));
        assertEquals(path26, cache.getPathClosestTo(120, 80));
        cache.setModel(null);
        assertNull(cache.getPathClosestTo(0, 0));
    }

    /*
     * Test method for 'javax.swing.tree.VariableHeightLayoutCache.invalidatePathBounds(TreePath)'
     */
    public void testInvalidatePathBounds() {
    }

    /*
     * Test method for 'javax.swing.tree.VariableHeightLayoutCache.invalidateSizes()'
     */
    @Override
    public void testInvalidateSizes() {
        //        super.testInvalidateSizes();
    }

    /*
     * Test method for 'javax.swing.tree.VariableHeightLayoutCache.treeNodesChanged(TreeModelEvent)'
     */
    @Override
    public void testTreeNodesChanged() {
        super.testTreeNodesChanged();
    }

    /*
     * Test method for 'javax.swing.tree.VariableHeightLayoutCache.setNodeDimensions(NodeDimensions)'
     */
    public void testSetNodeDimensionsNodeDimensions() {
    }

    @Override
    public void testIsFixedRowHeight() {
        assertFalse(cache.isFixedRowHeight());
    }
}
