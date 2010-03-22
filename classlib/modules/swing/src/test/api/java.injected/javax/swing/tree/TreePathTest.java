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

import javax.swing.SwingTestCase;

public class TreePathTest extends SwingTestCase {
    protected TreePath treePath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.tree.TreePath.TreePath(Object)'
     */
    public void testTreePathObject() {
        Object path1 = new Object();
        Object path2 = "new Object()";
        try {
            treePath = new TreePath((Object) null);
            fail("exception hasn't been thrown");
        } catch (IllegalArgumentException e) {
        }
        treePath = new TreePath(path1);
        assertEquals(1, treePath.getPathCount());
        assertEquals(path1, treePath.getPath()[0]);
        treePath = new TreePath(path2);
        assertEquals(1, treePath.getPathCount());
        assertEquals(path2, treePath.getPath()[0]);
    }

    /*
     * Test method for 'javax.swing.tree.TreePath.TreePath()'
     */
    public void testTreePath() {
        treePath = new TreePath();
        assertEquals(1, treePath.getPathCount());
        assertNull(treePath.getPath()[0]);
    }

    /*
     * Test method for 'javax.swing.tree.TreePath.TreePath(Object[], int)'
     */
    public void testTreePathObjectArrayInt() {
        Object[] path1 = new Object[] { new Object(), "1", "2" };
        treePath = new TreePath(path1, 3);
        assertEquals(3, treePath.getPathCount());
        assertNotSame(path1, treePath.getPath());
        assertEquals(path1[0], treePath.getPath()[0]);
        assertEquals(path1[1], treePath.getPath()[1]);
        assertEquals(path1[2], treePath.getPath()[2]);
        treePath = new TreePath(path1, 1);
        assertEquals(1, treePath.getPathCount());
        assertNotSame(path1, treePath.getPath());
        assertEquals(path1[0], treePath.getPath()[0]);
        boolean thrown = false;
        try {
            treePath = new TreePath(path1, 4);
        } catch (ArrayIndexOutOfBoundsException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    /*
     * Test method for 'javax.swing.tree.TreePath.TreePath(Object[])'
     */
    public void testTreePathObjectArray() {
        Object[] path1 = new Object[] { new Object(), "1", "2" };
        Object[] path2 = new Object[] { new Object(), "11", "22", "33", "2222" };
        try {
            treePath = new TreePath((Object[]) null);
            fail("exception hasn't been thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            treePath = new TreePath(new Object[0]);
            fail("exception hasn't been thrown");
        } catch (IllegalArgumentException e) {
        }
        treePath = new TreePath(path1);
        assertEquals(3, treePath.getPathCount());
        assertNotSame(path1, treePath.getPath());
        assertEquals(path1[0], treePath.getPath()[0]);
        assertEquals(path1[1], treePath.getPath()[1]);
        assertEquals(path1[2], treePath.getPath()[2]);
        treePath = new TreePath(path2);
        assertEquals(5, treePath.getPathCount());
        assertNotSame(path2, treePath.getPath());
        assertEquals(path2[0], treePath.getPath()[0]);
        assertEquals(path2[1], treePath.getPath()[1]);
        assertEquals(path2[2], treePath.getPath()[2]);
        assertEquals(path2[3], treePath.getPath()[3]);
        assertEquals(path2[4], treePath.getPath()[4]);
    }

    /*
     * Test method for 'javax.swing.tree.TreePath.TreePath(TreePath, Object)'
     */
    public void testTreePathTreePathObject() {
        Object last1 = "3";
        Object last2 = "345";
        Object[] path1 = new Object[] { new Object(), "1", "2" };
        Object[] path2 = new Object[] { new Object(), "11", "22", "33", "2222" };
        TreePath parent = new TreePath(path1);
        treePath = new TreePath(parent, last1);
        assertEquals(4, treePath.getPathCount());
        assertNotSame(parent.getPath(), treePath.getPath());
        assertEquals(path1[0], treePath.getPath()[0]);
        assertEquals(path1[1], treePath.getPath()[1]);
        assertEquals(path1[2], treePath.getPath()[2]);
        assertEquals(last1, treePath.getPath()[3]);
        parent = new TreePath(path2);
        treePath = new TreePath(parent, last2);
        assertEquals(6, treePath.getPathCount());
        assertNotSame(parent.getPath(), treePath.getPath());
        assertEquals(path2[0], treePath.getPath()[0]);
        assertEquals(path2[1], treePath.getPath()[1]);
        assertEquals(path2[2], treePath.getPath()[2]);
        assertEquals(path2[3], treePath.getPath()[3]);
        assertEquals(path2[4], treePath.getPath()[4]);
        assertEquals(last2, treePath.getPath()[5]);
        treePath = new TreePath(null, last2);
        assertEquals(1, treePath.getPathCount());
        assertEquals(last2, treePath.getPath()[0]);
        boolean thrown = false;
        try {
            treePath = new TreePath(parent, null);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    /*
     * Test method for 'javax.swing.tree.TreePath.equals(Object)'
     */
    public void testEqualsObject() {
        Object o1 = "1";
        Object o2 = "2";
        Object o3 = "3";
        Object o4 = "4";
        TreePath path1 = new TreePath(new Object[] { o1, o2, o3, o4 });
        TreePath path2 = new TreePath(new Object[] { o1, o2, o3, o4 });
        TreePath path3 = new TreePath(new Object[] { o4, o3, o1, o2 });
        TreePath path4 = new TreePath(new Object[] { o1, o2, o3 });
        TreePath path5 = new TreePath(new Object[] { o2 });
        assertTrue(path1.equals(path2));
        assertFalse(path1.equals(path3));
        assertFalse(path4.equals(path3));
        assertFalse(path4.equals(null));
        assertFalse(path5.equals(o2));
        TreePath path51 = new TreePath(new Object[] { new String("2") });
        assertTrue(path5.equals(path51));
    }

    /*
     * Test method for 'javax.swing.tree.TreePath.getLastPathComponent()'
     */
    public void testGetLastPathComponent() {
        Object[] path1 = new Object[] { new Object(), "1", "2" };
        Object[] path2 = new Object[] { new Object(), "11", "22", "33", "2222" };
        treePath = new TreePath(path1);
        assertEquals(path1[2], treePath.getLastPathComponent());
        treePath = new TreePath(path2);
        assertEquals(path2[4], treePath.getLastPathComponent());
    }

    /*
     * Test method for 'javax.swing.tree.TreePath.getParentPath()'
     */
    public void testGetParentPath() {
        Object[] path1 = new Object[] { new Object(), "1", "2" };
        Object[] path2 = new Object[] { new Object(), "11", "22", "33", "2222" };
        treePath = new TreePath(path1);
        TreePath parent = treePath.getParentPath();
        assertEquals(2, parent.getPathCount());
        assertNotSame(parent.getPath(), treePath.getPath());
        assertEquals(path1[0], parent.getPath()[0]);
        assertEquals(path1[1], parent.getPath()[1]);
        treePath = new TreePath(path2);
        parent = treePath.getParentPath();
        assertEquals(4, parent.getPathCount());
        assertNotSame(parent.getPath(), treePath.getPath());
        assertEquals(path2[0], parent.getPath()[0]);
        assertEquals(path2[1], parent.getPath()[1]);
        assertEquals(path2[2], parent.getPath()[2]);
        assertEquals(path2[3], parent.getPath()[3]);
        treePath = new TreePath(path1[0]);
        parent = treePath.getParentPath();
        assertNull(parent);
    }

    /*
     * is being tested by constructor's tests
     */
    public void testGetPath() {
    }

    /*
     * Test method for 'javax.swing.tree.TreePath.getPathComponent(int)'
     */
    public void testGetPathComponent() {
        Object[] path1 = new Object[] { new Object(), "1", "2" };
        Object[] path2 = new Object[] { new Object(), "11", "22", "33", "2222" };
        treePath = new TreePath(path1);
        assertEquals(path1[0], treePath.getPathComponent(0));
        assertEquals(path1[1], treePath.getPathComponent(1));
        assertEquals(path1[2], treePath.getPathComponent(2));
        treePath = new TreePath(path2);
        assertEquals(path2[0], treePath.getPathComponent(0));
        assertEquals(path2[1], treePath.getPathComponent(1));
        assertEquals(path2[2], treePath.getPathComponent(2));
        assertEquals(path2[3], treePath.getPathComponent(3));
        assertEquals(path2[4], treePath.getPathComponent(4));
        boolean thrown = false;
        try {
            treePath.getPathComponent(-1);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
        thrown = false;
        try {
            treePath.getPathComponent(5);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    /*
     * is being tested by constructor's tests
     */
    public void testGetPathCount() {
    }

    /*
     * Test method for 'javax.swing.tree.TreePath.isDescendant(TreePath)'
     */
    public void testIsDescendant() {
        Object o1 = "1";
        Object o2 = "2";
        Object o3 = "3";
        Object o4 = "4";
        TreePath path1 = new TreePath(new Object[] { o1, o2, o3, o4 });
        TreePath path2 = new TreePath(new Object[] { o1, o2, o3, o4 });
        TreePath path3 = new TreePath(new Object[] { o4, o3, o1, o2 });
        TreePath path4 = new TreePath(new Object[] { o1, o2, o3 });
        TreePath path5 = new TreePath(new Object[] { o2 });
        TreePath path6 = new TreePath(new Object[] { o4 });
        assertTrue(path1.isDescendant(path2));
        assertTrue(path2.isDescendant(path1));
        assertFalse(path2.isDescendant(path3));
        assertFalse(path3.isDescendant(path1));
        assertFalse(path2.isDescendant(path4));
        assertTrue(path4.isDescendant(path2));
        assertFalse(path5.isDescendant(path1));
        assertTrue(path6.isDescendant(path3));
        assertFalse(path5.isDescendant(null));
    }

    /*
     * Test method for 'javax.swing.tree.TreePath.pathByAddingChild(Object)'
     */
    public void testPathByAddingChild() {
        Object last1 = "3";
        Object last2 = "345";
        Object[] path1 = new Object[] { new Object(), "1", "2" };
        Object[] path2 = new Object[] { new Object(), "11", "22", "33", "2222" };
        TreePath parent = new TreePath(path1);
        treePath = parent.pathByAddingChild(last1);
        assertEquals(4, treePath.getPathCount());
        assertNotSame(parent.getPath(), treePath.getPath());
        assertEquals(path1[0], treePath.getPath()[0]);
        assertEquals(path1[1], treePath.getPath()[1]);
        assertEquals(path1[2], treePath.getPath()[2]);
        assertEquals(last1, treePath.getPath()[3]);
        parent = new TreePath(path2);
        treePath = parent.pathByAddingChild(last2);
        assertEquals(6, treePath.getPathCount());
        assertNotSame(parent.getPath(), treePath.getPath());
        assertEquals(path2[0], treePath.getPath()[0]);
        assertEquals(path2[1], treePath.getPath()[1]);
        assertEquals(path2[2], treePath.getPath()[2]);
        assertEquals(path2[3], treePath.getPath()[3]);
        assertEquals(path2[4], treePath.getPath()[4]);
        assertEquals(last2, treePath.getPath()[5]);
        parent = new TreePath(last1);
        treePath = parent.pathByAddingChild(last2);
        assertEquals(2, treePath.getPathCount());
        boolean thrown = false;
        try {
            treePath = parent.pathByAddingChild(null);
        } catch (NullPointerException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    /*
     * Test method for 'javax.swing.tree.TreePath.hashCode()'
     */
    public void testHashCode() {
        Object last = "o4";
        TreePath path1 = new TreePath(new Object[] { "o1", "o2", "o3", last });
        assertEquals(path1.hashCode(), last.hashCode());
    }

    /*
     * Test method for 'javax.swing.tree.TreePath.toString()'
     */
    public void testToString() {
        TreePath path1 = new TreePath(new Object[] { "o1", "o2", "o3", "o4" });
        TreePath path2 = new TreePath(new Object[] { "o1", "o2", null });
        assertNotNull(path1.toString());
        assertNotNull(path2.toString());
    }
}
