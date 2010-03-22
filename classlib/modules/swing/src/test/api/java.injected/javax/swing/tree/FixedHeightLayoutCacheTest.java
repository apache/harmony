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

public class FixedHeightLayoutCacheTest extends AbstractLayoutCacheTest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cache = new FixedHeightLayoutCache();
        defaultRowValue = cache.getRowForPath(null);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    public void testLayoutCache() {
        assertFalse(cache.rootVisible);
        assertNull(cache.nodeDimensions);
        assertEquals(1, cache.rowHeight);
        assertNull(cache.treeModel);
        assertNull(cache.treeSelectionModel);
    }

    @Override
    public void testSetRootVisible() {
        super.testSetRootVisible();
        int height = 1111;
        cache.setRowHeight(height);
        TreeModel model = new UndefaultTreeModel(root);
        root.add(node1);
        root.add(node2);
        cache.setModel(model);
        cache.setNodeDimensions(dimensions1);
        assertEquals(new Rectangle(root.hashCode(), -1 * height, 0, height), cache.getBounds(
                rootPath, null));
        assertEquals(new Rectangle(node1.hashCode(), 0 * height, 1, height), cache.getBounds(
                path1, null));
        assertEquals(new Rectangle(node2.hashCode(), 1 * height, 1, height), cache.getBounds(
                path2, null));
        cache.setRootVisible(true);
        assertEquals(new Rectangle(root.hashCode(), 0 * height, 0, height), cache.getBounds(
                rootPath, null));
        assertEquals(new Rectangle(node1.hashCode(), 1 * height, 1, height), cache.getBounds(
                path1, null));
        assertEquals(new Rectangle(node2.hashCode(), 2 * height, 1, height), cache.getBounds(
                path2, null));
    }

    @Override
    public void testIsFixedRowHeight() {
        assertTrue(cache.isFixedRowHeight());
    }

    @Override
    public void testSetRowHeight() {
        cache.setRowHeight(100);
        assertEquals(100, cache.getRowHeight());
        cache.setRowHeight(200);
        assertEquals(200, cache.getRowHeight());
        try {
            cache.setRowHeight(0);
            fail("no exception has been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.getBounds(TreePath, Rectangle)'
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
        cache.setExpandedState(path1, true);
        cache.setExpandedState(path2, true);
        assertNull(cache.getBounds(path1, new Rectangle()));
        assertNull(cache.getBounds(path1, null));
        cache.setNodeDimensions(dimensions1);
        assertEquals(new Rectangle(root.hashCode(), -1 * height, 0, height), cache.getBounds(
                rootPath, null));
        assertEquals(new Rectangle(node1.hashCode(), 0 * height, 1, height), cache.getBounds(
                path1, null));
        assertEquals(new Rectangle(node2.hashCode(), 3 * height, 1, height), cache.getBounds(
                path2, null));
        assertEquals(new Rectangle(node3.hashCode(), 1 * height, 2, height), cache.getBounds(
                path13, null));
        assertEquals(new Rectangle(node4.hashCode(), 2 * height, 2, height), cache.getBounds(
                path14, null));
        assertEquals(new Rectangle(node5.hashCode(), 4 * height, 2, height), cache.getBounds(
                path25, null));
        assertEquals(new Rectangle(node6.hashCode(), 5 * height, 2, height), cache.getBounds(
                path26, null));
        cache.setExpandedState(rootPath, false);
        assertNull(cache.getBounds(path25, null));
        cache.setModel(null);
        assertNull(cache.getBounds(path1, null));
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.getPathClosestTo(int, int)'
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
        cache.setNodeDimensions(dimensions2);
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
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.invalidatePathBounds(TreePath)'
     * due to the spec this method does nothing
     */
    public void testInvalidatePathBounds() {
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.invalidateSizes()'
     * this method seems to do nothing also
     */
    @Override
    public void testInvalidateSizes() {
    }

    /*
     * Test method for 'javax.swing.tree.FixedHeightLayoutCache.treeNodesChanged(TreeModelEvent)'
     */
    @Override
    public void testTreeNodesChanged() {
        super.testTreeNodesChanged();
    }

    @Override
    public void testGetPreferredHeight() {
        int height = 1111;
        TreeModel model = new UndefaultTreeModel(root);
        root.add(node1);
        root.add(node2);
        cache.setRowHeight(height);
        assertEquals(0, cache.getPreferredHeight());
        cache.setModel(model);
        if (isHarmony()) {
            assertEquals(2222, cache.getPreferredHeight());
        } else {
            assertEquals(0, cache.getPreferredHeight());
        }
        cache.setRowHeight(height);
        cache.setNodeDimensions(dimensions3);
        assertEquals(2222, cache.getPreferredHeight());
        cache.setRootVisible(true);
        assertEquals(3333, cache.getPreferredHeight());
        cache.setRootVisible(false);
        assertEquals(2222, cache.getPreferredHeight());
        cache.setModel(model);
        assertEquals(2222, cache.getPreferredHeight());
        cache.setNodeDimensions(null);
        if (isHarmony()) {
            assertEquals(2222, cache.getPreferredHeight());
        } else {
            assertEquals(0, cache.getPreferredHeight());
        }
    }

    @Override
    public void testGetPreferredWidth() {
        int height = 20;
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
        assertEquals(0, cache.getPreferredWidth(new Rectangle(100, 100, 10, 10)));
        cache.setNodeDimensions(dimensions2);
        assertEquals(110, cache.getPreferredWidth(new Rectangle(100, 100, 10, 10)));
        cache.setRootVisible(true);
        assertEquals(110, cache.getPreferredWidth(new Rectangle(0, 0, 10, 100)));
        cache.setExpandedState(path1, true);
        assertEquals(100, cache.getPreferredWidth(new Rectangle(0, 0, 0, 0)));
        assertEquals(100, cache.getPreferredWidth(new Rectangle(10, 10, 10, 10)));
        assertEquals(110, cache.getPreferredWidth(new Rectangle(20, 20, 10, 10)));
        assertEquals(110, cache.getPreferredWidth(new Rectangle(30, 30, 10, 10)));
        assertEquals(120, cache.getPreferredWidth(new Rectangle(40, 40, 10, 10)));
        assertEquals(120, cache.getPreferredWidth(new Rectangle(50, 50, 10, 10)));
        assertEquals(120, cache.getPreferredWidth(new Rectangle(60, 60, 10, 10)));
        assertEquals(120, cache.getPreferredWidth(new Rectangle(70, 70, 10, 10)));
        assertEquals(120, cache.getPreferredWidth(null));
    }
}
