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
package javax.swing.event;

import java.util.Arrays;
import javax.swing.SwingTestCase;
import javax.swing.tree.TreePath;

public class TreeModelEventTest extends SwingTestCase {
    protected TreeModelEvent event = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        event = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.event.TreeModelEvent.TreeModelEvent(Object, TreePath, int[], Object[])'
     */
    public void testTreeModelEventObjectTreePathIntArrayObjectArray() {
        Object source = "111";
        TreePath path = new TreePath("222");
        int[] indices = new int[] { 1, 2 };
        Object[] children = new Object[] { "1", "2", "3" };
        event = new TreeModelEvent(source, path, indices, children);
        assertEquals(source, event.getSource());
        assertSame(path, event.path);
        assertSame(indices, event.childIndices);
        assertSame(children, event.children);
        event = new TreeModelEvent(source, (TreePath) null, null, null);
        assertEquals(source, event.getSource());
        assertNull(event.path);
        assertNull(event.childIndices);
        assertNull(event.children);
    }

    /*
     * Test method for 'javax.swing.event.TreeModelEvent.TreeModelEvent(Object, Object[], int[], Object[])'
     */
    public void testTreeModelEventObjectObjectArrayIntArrayObjectArray() {
        Object source = "111";
        Object[] path = new Object[] { "11", "22", "33" };
        int[] indices = new int[] { 1, 2 };
        Object[] children = new Object[] { "1", "2", "3" };
        event = new TreeModelEvent(source, path, indices, children);
        assertEquals(source, event.getSource());
        assertNotNull(event.path);
        assertSame(indices, event.childIndices);
        assertEquals(indices[0], event.getChildIndices()[0]);
        assertEquals(indices[1], event.getChildIndices()[1]);
        assertSame(children, event.children);
        assertEquals(children[0], event.getChildren()[0]);
        assertEquals(children[1], event.getChildren()[1]);
        assertEquals(children[2], event.getChildren()[2]);
        event = new TreeModelEvent(source, path, null, null);
        assertEquals(source, event.getSource());
        assertNotNull(event.path);
        assertNull(event.childIndices);
        assertNull(event.children);
        event = new TreeModelEvent(source, (TreePath) null, null, null);
        assertEquals(source, event.getSource());
        assertNull(event.path);
        assertNull(event.childIndices);
        assertNull(event.children);
    }

    /*
     * Test method for 'javax.swing.event.TreeModelEvent.TreeModelEvent(Object, Object[])'
     */
    public void testTreeModelEventObjectObjectArray() {
        Object source = "111";
        Object[] path = new Object[] { "11", "22", "33" };
        event = new TreeModelEvent(source, path);
        assertEquals(source, event.getSource());
        assertNotNull(event.path);
        assertNotNull(event.childIndices);
        assertEquals(0, event.childIndices.length);
        assertNull(event.children);
    }

    /*
     * Test method for 'javax.swing.event.TreeModelEvent.TreeModelEvent(Object, TreePath)'
     */
    public void testTreeModelEventObjectTreePath() {
        Object source = "111";
        TreePath path = new TreePath("222");
        event = new TreeModelEvent(source, path);
        assertEquals(source, event.getSource());
        assertSame(path, event.path);
        assertNotNull(event.childIndices);
        assertEquals(0, event.childIndices.length);
        assertNull(event.children);
        event = new TreeModelEvent(source, (TreePath) null);
        assertEquals(source, event.getSource());
        assertNull(event.path);
        assertNotNull(event.childIndices);
        assertEquals(0, event.childIndices.length);
        assertNull(event.children);
    }

    /*
     * Test method for 'javax.swing.event.TreeModelEvent.getChildIndices()'
     */
    public void testGetChildIndices() {
        Object source = "111";
        TreePath path = new TreePath("222");
        int[] indices1 = new int[] { 1, 2 };
        int[] indices2 = new int[] { 11, 22 };
        Object[] children = new Object[] { "1", "2", "3" };
        event = new TreeModelEvent(source, path, indices1, children);
        assertNotSame(indices1, event.getChildIndices());
        assertTrue(Arrays.equals(indices1, event.getChildIndices()));
        event.childIndices = indices2;
        assertNotSame(indices2, event.getChildIndices());
        assertTrue(Arrays.equals(indices2, event.getChildIndices()));
    }

    /*
     * Test method for 'javax.swing.event.TreeModelEvent.getChildren()'
     */
    public void testGetChildren() {
        Object source = "111";
        TreePath path = new TreePath("222");
        int[] indices = new int[] { 1, 2 };
        Object[] children1 = new Object[] { "1", "2", "3" };
        Object[] children2 = new Object[] { "11", "22", "33" };
        event = new TreeModelEvent(source, path, indices, children1);
        assertNotSame(children1, event.getChildren());
        assertTrue(Arrays.equals(children1, event.getChildren()));
        event.children = children2;
        assertNotSame(children2, event.getChildren());
        assertTrue(Arrays.equals(children2, event.getChildren()));
    }

    /*
     * Test method for 'javax.swing.event.TreeModelEvent.getPath()'
     */
    public void testGetPath() {
        Object source = "111";
        Object[] array1 = new Object[] { "1", "2", "3" };
        Object[] array2 = new Object[] { "11", "22", "33" };
        TreePath path1 = new TreePath(array1);
        Object[] path2 = array2;
        TreePath path3 = new TreePath(array2);
        event = new TreeModelEvent(source, path1);
        assertNotSame(array1, event.getPath());
        assertTrue(Arrays.equals(array1, event.getPath()));
        event = new TreeModelEvent(source, path2);
        assertNotSame(array2, event.getPath());
        assertTrue(Arrays.equals(array2, event.getPath()));
        event.path = path3;
        assertNotSame(array2, event.getPath());
        assertTrue(Arrays.equals(array2, event.getPath()));
        event.path = null;
        assertNull(event.getPath());
    }

    /*
     * Test method for 'javax.swing.event.TreeModelEvent.getTreePath()'
     */
    public void testGetTreePath() {
        Object source = "111";
        Object[] array1 = new Object[] { "1", "2", "3" };
        Object[] array2 = new Object[] { "11", "22", "33" };
        TreePath path1 = new TreePath(array1);
        Object[] path2 = array2;
        TreePath path3 = new TreePath(array2);
        event = new TreeModelEvent(source, path1);
        assertSame(path1, event.getTreePath());
        event = new TreeModelEvent(source, path2);
        assertTrue(Arrays.equals(array2, event.getTreePath().getPath()));
        event.path = path3;
        assertSame(path3, event.getTreePath());
        event.path = null;
        assertNull(event.getTreePath());
    }

    /*
     * Test method for 'javax.swing.event.TreeModelEvent.toString()'
     */
    public void testToString() {
        Object source = "111";
        TreePath path = new TreePath("222");
        String str = new TreeModelEvent(source, path).toString();
        assertNotNull(str);
        assertFalse(str.equals(""));
    }
}
