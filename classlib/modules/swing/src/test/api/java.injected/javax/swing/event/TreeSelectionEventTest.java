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

import javax.swing.SwingTestCase;
import javax.swing.tree.TreePath;

public class TreeSelectionEventTest extends SwingTestCase {
    protected TreeSelectionEvent event;

    /*
     * Test method for 'javax.swing.event.TreeSelectionEvent.TreeSelectionEvent(Object, TreePath, boolean, TreePath, TreePath)'
     */
    public void testTreeSelectionEventObjectTreePathBooleanTreePathTreePath() {
        TreePath path = new TreePath("asd");
        TreePath oldSelection = new TreePath("old");
        TreePath newSelection = new TreePath("new");
        Object source = new Object();
        boolean isNew = true;
        event = new TreeSelectionEvent(source, path, isNew, oldSelection, newSelection);
        assertEquals(source, event.getSource());
        assertEquals(path, event.getPath());
        assertEquals(isNew, event.areNew[0]);
        assertEquals(oldSelection, event.getOldLeadSelectionPath());
        assertEquals(newSelection, event.getNewLeadSelectionPath());
    }

    /*
     * Test method for 'javax.swing.event.TreeSelectionEvent.TreeSelectionEvent(Object, TreePath[], boolean[], TreePath, TreePath)'
     */
    public void testTreeSelectionEventObjectTreePathArrayBooleanArrayTreePathTreePath() {
        TreePath[] paths = new TreePath[] { new TreePath("asd"), new TreePath("qwe"),
                new TreePath("zxc") };
        boolean[] areNew = new boolean[] { true, false, true };
        TreePath oldSelection = new TreePath("old");
        TreePath newSelection = new TreePath("new");
        Object source = new Object();
        event = new TreeSelectionEvent(source, paths, areNew, oldSelection, newSelection);
        assertEquals(source, event.getSource());
        assertEquals(paths[0], event.getPaths()[0]);
        assertEquals(paths[1], event.getPaths()[1]);
        assertEquals(paths[2], event.getPaths()[2]);
        assertEquals(areNew[0], event.areNew[0]);
        assertEquals(areNew[1], event.areNew[1]);
        assertEquals(areNew[2], event.areNew[2]);
        assertEquals(oldSelection, event.getOldLeadSelectionPath());
        assertEquals(newSelection, event.getNewLeadSelectionPath());
    }

    /*
     * Test method for 'javax.swing.event.TreeSelectionEvent.cloneWithSource(Object)'
     */
    public void testCloneWithSource() {
        TreePath[] paths = new TreePath[] { new TreePath("asd"), new TreePath("qwe"),
                new TreePath("zxc") };
        boolean[] areNew = new boolean[] { true, false, true };
        TreePath oldSelection = new TreePath("old");
        TreePath newSelection = new TreePath("new");
        Object source = "source";
        Object newSource = "newSource";
        event = new TreeSelectionEvent(source, paths, areNew, oldSelection, newSelection);
        TreeSelectionEvent cloned = (TreeSelectionEvent) event.cloneWithSource(newSource);
        assertEquals(newSource, cloned.getSource());
        assertEquals(paths[0], cloned.getPaths()[0]);
        assertEquals(paths[1], cloned.getPaths()[1]);
        assertEquals(paths[2], cloned.getPaths()[2]);
        assertEquals(areNew[0], cloned.areNew[0]);
        assertEquals(areNew[1], cloned.areNew[1]);
        assertEquals(areNew[2], cloned.areNew[2]);
        assertEquals(oldSelection, cloned.getOldLeadSelectionPath());
        assertEquals(newSelection, cloned.getNewLeadSelectionPath());
    }

    /*
     * Test method for 'javax.swing.event.TreeSelectionEvent.getNewLeadSelectionPath()'
     */
    public void testGetNewLeadSelectionPath() {
        TreePath[] paths = new TreePath[] { new TreePath("asd"), new TreePath("qwe"),
                new TreePath("zxc") };
        boolean[] areNew = new boolean[] { true, false, true };
        TreePath oldSelection = new TreePath("old");
        TreePath newSelection = new TreePath("new");
        Object source = "source";
        event = new TreeSelectionEvent(source, paths, areNew, oldSelection, newSelection);
        assertEquals(newSelection, event.getNewLeadSelectionPath());
    }

    /*
     * Test method for 'javax.swing.event.TreeSelectionEvent.getOldLeadSelectionPath()'
     */
    public void testGetOldLeadSelectionPath() {
        TreePath[] paths = new TreePath[] { new TreePath("asd"), new TreePath("qwe"),
                new TreePath("zxc") };
        boolean[] areNew = new boolean[] { true, false, true };
        TreePath oldSelection = new TreePath("old");
        TreePath newSelection = new TreePath("new");
        Object source = "source";
        event = new TreeSelectionEvent(source, paths, areNew, oldSelection, newSelection);
        assertEquals(oldSelection, event.getOldLeadSelectionPath());
    }

    /*
     * Test method for 'javax.swing.event.TreeSelectionEvent.getPath()'
     */
    public void testGetPath() {
        TreePath[] paths = new TreePath[] { new TreePath("asd"), new TreePath("qwe"),
                new TreePath("zxc") };
        boolean[] areNew = new boolean[] { true, false, true };
        TreePath oldSelection = new TreePath("old");
        TreePath newSelection = new TreePath("new");
        Object source = new Object();
        event = new TreeSelectionEvent(source, paths, areNew, oldSelection, newSelection);
        assertEquals(paths[0], event.getPath());
        areNew = new boolean[] { false, false, true };
        event = new TreeSelectionEvent(source, paths, areNew, oldSelection, newSelection);
        assertEquals(paths[0], event.getPath());
        event = new TreeSelectionEvent(source, paths[0], true, oldSelection, newSelection);
        assertEquals(paths[0], event.getPath());
    }

    /*
     * Test method for 'javax.swing.event.TreeSelectionEvent.getPaths()'
     */
    public void testGetPaths() {
        TreePath[] paths = new TreePath[] { new TreePath("asd"), new TreePath("qwe"),
                new TreePath("zxc") };
        boolean[] areNew = new boolean[] { true, false, true };
        TreePath oldSelection = new TreePath("old");
        TreePath newSelection = new TreePath("new");
        Object source = new Object();
        event = new TreeSelectionEvent(source, paths, areNew, oldSelection, newSelection);
        assertNotSame(paths, event.getPaths());
        assertEquals(paths[0], event.getPaths()[0]);
        assertEquals(paths[1], event.getPaths()[1]);
        assertEquals(paths[2], event.getPaths()[2]);
    }

    /*
     * Test method for 'javax.swing.event.TreeSelectionEvent.isAddedPath(int)'
     */
    public void testIsAddedPathInt() {
        TreePath path1 = new TreePath("asd");
        TreePath path2 = new TreePath("dsa");
        TreePath path3 = new TreePath("qwe");
        TreePath oldSelection = new TreePath("old");
        TreePath newSelection = new TreePath("new");
        Object source = new Object();
        event = new TreeSelectionEvent(source, path1, true, oldSelection, newSelection);
        assertTrue(event.isAddedPath(0));
        event = new TreeSelectionEvent(source, path1, false, oldSelection, newSelection);
        assertFalse(event.isAddedPath(0));
        event = new TreeSelectionEvent(source, path1, true, oldSelection, newSelection);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                event.isAddedPath(1);
            }
        });
        TreePath[] paths = new TreePath[] { path1, path2, path3 };
        boolean[] areNew = new boolean[] { true, false, true };
        event = new TreeSelectionEvent(source, paths, areNew, oldSelection, newSelection);
        assertTrue(event.isAddedPath(0));
        assertFalse(event.isAddedPath(1));
        assertTrue(event.isAddedPath(2));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                event.isAddedPath(3);
            }
        });
    }

    /*
     * Test method for 'javax.swing.event.TreeSelectionEvent.isAddedPath()'
     */
    public void testIsAddedPath() {
        TreePath path1 = new TreePath("asd");
        TreePath path2 = new TreePath("dsa");
        TreePath path3 = new TreePath("qwe");
        TreePath oldSelection = new TreePath("old");
        TreePath newSelection = new TreePath("new");
        Object source = new Object();
        event = new TreeSelectionEvent(source, path1, true, oldSelection, newSelection);
        assertTrue(event.isAddedPath());
        event = new TreeSelectionEvent(source, path1, false, oldSelection, newSelection);
        assertFalse(event.isAddedPath());
        event = new TreeSelectionEvent(source, null, true, null, null);
        assertTrue(event.isAddedPath());
        event = new TreeSelectionEvent(source, null, false, null, null);
        assertFalse(event.isAddedPath());
        TreePath[] paths = new TreePath[] { path1, path2, path3 };
        boolean[] areNew = new boolean[] { true, false, true };
        event = new TreeSelectionEvent(source, paths, areNew, oldSelection, newSelection);
        assertTrue(event.isAddedPath());
        areNew = new boolean[] { false, false, true };
        event = new TreeSelectionEvent(source, paths, areNew, oldSelection, newSelection);
        assertFalse(event.isAddedPath());
    }

    /*
     * Test method for 'javax.swing.event.TreeSelectionEvent.isAddedPath(TreePath)'
     */
    public void testIsAddedPathTreePath() {
        TreePath path1 = new TreePath("asd");
        final TreePath path2 = new TreePath("dsa");
        TreePath path3 = new TreePath("qwe");
        final TreePath path4 = new TreePath("ewq");
        TreePath oldSelection = new TreePath("old");
        TreePath newSelection = new TreePath("new");
        Object source = new Object();
        event = new TreeSelectionEvent(source, path1, true, oldSelection, newSelection);
        assertTrue(event.isAddedPath(path1));
        event = new TreeSelectionEvent(source, path1, false, oldSelection, newSelection);
        assertFalse(event.isAddedPath(path1));
        event = new TreeSelectionEvent(source, path1, true, oldSelection, newSelection);
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                event.isAddedPath(path2);
            }
        });
        TreePath[] paths = new TreePath[] { path1, path2, path3 };
        boolean[] areNew = new boolean[] { true, false, true };
        event = new TreeSelectionEvent(source, paths, areNew, oldSelection, newSelection);
        assertTrue(event.isAddedPath(path1));
        assertFalse(event.isAddedPath(path2));
        assertTrue(event.isAddedPath(path3));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                event.isAddedPath(path4);
            }
        });
        event = new TreeSelectionEvent(source, null, null, oldSelection, newSelection);
    }
}
