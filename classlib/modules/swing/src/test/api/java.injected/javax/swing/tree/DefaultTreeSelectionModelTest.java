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
import java.util.EventListener;
import java.util.Vector;
import javax.swing.SwingTestCase;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.SwingPropertyChangeSupportTest.FindableListener;

public class DefaultTreeSelectionModelTest extends SwingTestCase {
    class ConcreteTreeSelectionListener extends FindableListener implements
            TreeSelectionListener {
        public TreeSelectionEvent event = null;

        public boolean fired = false;

        private final boolean debugOutput;

        ConcreteTreeSelectionListener() {
            super();
            debugOutput = false;
        }

        ConcreteTreeSelectionListener(final boolean debugOutput) {
            super();
            this.debugOutput = debugOutput;
        }

        public void valueChanged(TreeSelectionEvent e) {
            event = e;
            fired = true;
            if (debugOutput) {
                System.out.println(e);
            }
        }

        @Override
        public void reset() {
            event = null;
            fired = false;
        }
    }

    protected DefaultTreeSelectionModel model;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        model = new DefaultTreeSelectionModel();
    }

    @Override
    protected void tearDown() throws Exception {
        model = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.DefaultTreeSelectionModel()'
     */
    public void testDefaultTreeSelectionModel() {
        assertNotNull(model.listSelectionModel);
        assertNull(model.rowMapper);
        assertNull(model.selection);
        assertNull(model.changeSupport);
        assertNotNull(model.listenerList);
        assertEquals(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION, model.selectionMode);
        assertNull(model.leadPath);
        assertEquals(-1, model.leadIndex);
        assertEquals(-1, model.leadRow);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.addPropertyChangeListener(PropertyChangeListener)'
     */
    public void testAddPropertyChangeListener() {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        PropertyChangeListener[] listenersArray = null;
        model.addPropertyChangeListener(null);
        listenersArray = model.getPropertyChangeListeners();
        assertEquals(0, listenersArray.length);
        model.addPropertyChangeListener(changeListener1);
        listenersArray = model.getPropertyChangeListeners();
        assertEquals(1, changeListener1.findMe(listenersArray));
        assertEquals(0, model.listenerList.getListeners(PropertyChangeListener.class).length);
        model.addPropertyChangeListener(changeListener2);
        listenersArray = model.getPropertyChangeListeners();
        assertEquals(1, changeListener1.findMe(listenersArray));
        assertEquals(1, changeListener2.findMe(listenersArray));
        model.addPropertyChangeListener(changeListener2);
        listenersArray = model.getPropertyChangeListeners();
        assertEquals(1, changeListener1.findMe(listenersArray));
        assertEquals(2, changeListener2.findMe(listenersArray));
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.removePropertyChangeListener(PropertyChangeListener)'
     */
    public void testRemovePropertyChangeListener() {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        PropertyChangeListener[] listenersArray = null;
        model.addPropertyChangeListener(changeListener1);
        model.addPropertyChangeListener(changeListener1);
        model.addPropertyChangeListener(changeListener2);
        listenersArray = model.getPropertyChangeListeners();
        assertEquals(3, listenersArray.length);
        model.removePropertyChangeListener(changeListener1);
        listenersArray = model.getPropertyChangeListeners();
        assertEquals(2, listenersArray.length);
        model.removePropertyChangeListener(changeListener1);
        listenersArray = model.getPropertyChangeListeners();
        assertEquals(1, listenersArray.length);
        model.removePropertyChangeListener(changeListener2);
        listenersArray = model.getPropertyChangeListeners();
        assertEquals(0, listenersArray.length);
        model.removePropertyChangeListener(null);
        listenersArray = model.getPropertyChangeListeners();
        assertEquals(0, listenersArray.length);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.getPropertyChangeListeners()'
     */
    public void testGetPropertyChangeListeners() {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        PropertyChangeListener[] listenersArray = null;
        listenersArray = model.getPropertyChangeListeners();
        assertEquals(0, listenersArray.length);
        model.addPropertyChangeListener(changeListener1);
        listenersArray = model.getPropertyChangeListeners();
        assertEquals(1, listenersArray.length);
        assertEquals(1, changeListener1.findMe(listenersArray));
        model.addPropertyChangeListener(changeListener2);
        listenersArray = model.getPropertyChangeListeners();
        assertEquals(2, listenersArray.length);
        assertEquals(1, changeListener1.findMe(listenersArray));
        assertEquals(1, changeListener2.findMe(listenersArray));
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.addTreeSelectionListener(TreeSelectionListener)'
     */
    public void testAddTreeSelectionListener() {
        ConcreteTreeSelectionListener listener1 = new ConcreteTreeSelectionListener();
        ConcreteTreeSelectionListener listener2 = new ConcreteTreeSelectionListener();
        TreeSelectionListener[] listenersArray = null;
        model.addTreeSelectionListener(listener1);
        listenersArray = model.getTreeSelectionListeners();
        assertTrue(listenersArray.length == 1);
        assertEquals(1, model.listenerList.getListeners(TreeSelectionListener.class).length);
        assertEquals(1, model.getListeners(TreeSelectionListener.class).length);
        assertTrue(listener1.findMe(listenersArray) > 0);
        model.addTreeSelectionListener(listener2);
        listenersArray = model.getTreeSelectionListeners();
        assertEquals(2, listenersArray.length);
        assertTrue(listener1.findMe(listenersArray) > 0);
        assertTrue(listener2.findMe(listenersArray) > 0);
        model.addTreeSelectionListener(listener2);
        listenersArray = model.getTreeSelectionListeners();
        assertEquals(3, listenersArray.length);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.removeTreeSelectionListener(TreeSelectionListener)'
     */
    public void testRemoveTreeSelectionListener() {
        ConcreteTreeSelectionListener changeListener1 = new ConcreteTreeSelectionListener();
        ConcreteTreeSelectionListener changeListener2 = new ConcreteTreeSelectionListener();
        ConcreteTreeSelectionListener changeListener3 = new ConcreteTreeSelectionListener();
        TreeSelectionListener[] listenersArray = null;
        model.addTreeSelectionListener(changeListener1);
        model.addTreeSelectionListener(changeListener2);
        model.addTreeSelectionListener(changeListener3);
        listenersArray = model.getTreeSelectionListeners();
        assertEquals(3, listenersArray.length);
        assertEquals(1, changeListener1.findMe(listenersArray));
        assertEquals(1, changeListener2.findMe(listenersArray));
        assertEquals(1, changeListener3.findMe(listenersArray));
        model.removeTreeSelectionListener(changeListener2);
        listenersArray = model.getTreeSelectionListeners();
        assertEquals(2, listenersArray.length);
        assertEquals(1, changeListener1.findMe(listenersArray));
        assertEquals(0, changeListener2.findMe(listenersArray));
        assertEquals(1, changeListener3.findMe(listenersArray));
        model.removeTreeSelectionListener(changeListener1);
        listenersArray = model.getTreeSelectionListeners();
        assertEquals(1, listenersArray.length);
        assertEquals(1, changeListener3.findMe(listenersArray));
        model.removeTreeSelectionListener(changeListener3);
        listenersArray = model.getTreeSelectionListeners();
        assertEquals(0, listenersArray.length);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.getTreeSelectionListeners()'
     */
    public void testGetTreeSelectionListeners() {
        ConcreteTreeSelectionListener changeListener1 = new ConcreteTreeSelectionListener();
        ConcreteTreeSelectionListener changeListener2 = new ConcreteTreeSelectionListener();
        ConcreteTreeSelectionListener changeListener3 = new ConcreteTreeSelectionListener();
        TreeSelectionListener[] listenersArray = null;
        listenersArray = model.getTreeSelectionListeners();
        assertTrue(listenersArray != null && listenersArray.length == 0);
        model.addTreeSelectionListener(changeListener1);
        model.addTreeSelectionListener(changeListener2);
        model.addTreeSelectionListener(changeListener3);
        model.addTreeSelectionListener(changeListener2);
        listenersArray = model.getTreeSelectionListeners();
        assertTrue(listenersArray.length == 4);
        assertTrue(changeListener1.findMe(listenersArray) == 1);
        assertTrue(changeListener2.findMe(listenersArray) == 2);
        assertTrue(changeListener3.findMe(listenersArray) == 1);
    }

    public void testFireValueChange() {
        ConcreteTreeSelectionListener listener = new ConcreteTreeSelectionListener();
        TreeSelectionListener[] listenersArray = null;
        listenersArray = model.getTreeSelectionListeners();
        assertTrue(listenersArray != null && listenersArray.length == 0);
        model.addTreeSelectionListener(listener);
        TreePath path = new TreePath("asd");
        TreePath oldSelection = new TreePath("old");
        TreePath newSelection = new TreePath("new");
        Object source = new Object();
        boolean isNew = true;
        TreeSelectionEvent event = new TreeSelectionEvent(source, path, isNew, oldSelection,
                newSelection);
        model.fireValueChanged(event);
        assertSame(event, listener.event);
        listener.reset();
        model.fireValueChanged(null);
        assertTrue(listener.fired);
        assertNull(listener.event);
    }

    /*
     * Class under test for EventListener[] getListeners(Class)
     */
    public void testGetListenersClass() {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        TreeSelectionListener treeSelectionListener = new ConcreteTreeSelectionListener();
        EventListener[] listenersArray = null;
        listenersArray = model.getListeners(TreeSelectionListener.class);
        assertEquals(0, listenersArray.length);
        listenersArray = model.getListeners(PropertyChangeListener.class);
        assertEquals(0, listenersArray.length);
        model.addPropertyChangeListener(changeListener1);
        model.addTreeSelectionListener(treeSelectionListener);
        model.addPropertyChangeListener(changeListener2);
        model.addPropertyChangeListener(changeListener2);
        listenersArray = model.getListeners(PropertyChangeListener.class);
        assertEquals(0, listenersArray.length);
        listenersArray = model.getListeners(TreeSelectionListener.class);
        assertEquals(1, listenersArray.length);
        model.removeTreeSelectionListener(treeSelectionListener);
        listenersArray = model.getListeners(TreeSelectionListener.class);
        assertEquals(0, listenersArray.length);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.addSelectionPath(TreePath)'
     */
    public void testAddSelectionPath() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        ConcreteTreeSelectionListener treeSelectionListener = new ConcreteTreeSelectionListener(
                false);
        model.addTreeSelectionListener(treeSelectionListener);
        model.addSelectionPath(path1);
        assertEquals(path1, treeSelectionListener.event.getPaths()[0]);
        assertTrue(treeSelectionListener.event.isAddedPath(path1));
        treeSelectionListener.reset();
        model.addSelectionPath(path1);
        assertNull(treeSelectionListener.event);
        model.addSelectionPath(path2);
        assertEquals(1, treeSelectionListener.event.getPaths().length);
        assertEquals(path2, treeSelectionListener.event.getPaths()[0]);
        assertTrue(treeSelectionListener.event.isAddedPath(path2));
        assertNotNull(treeSelectionListener.event.getOldLeadSelectionPath());
        assertNotNull(treeSelectionListener.event.getNewLeadSelectionPath());
        treeSelectionListener.reset();
        model.addSelectionPath(null);
        assertNull(treeSelectionListener.event);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.getSelectionPath()'
     */
    public void testGetSelectionPath() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        assertNull(model.getSelectionPath());
        model.addSelectionPath(path1);
        assertEquals(path1, model.getSelectionPath());
        model.addSelectionPath(path2);
        assertEquals(path1, model.getSelectionPath());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.isPathSelected(TreePath)'
     */
    public void testIsPathSelected() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        assertFalse(model.isPathSelected(path1));
        assertFalse(model.isPathSelected(path2));
        assertFalse(model.isPathSelected(path3));
        assertFalse(model.isPathSelected(path4));
        model.addSelectionPath(path1);
        assertTrue(model.isPathSelected(path1));
        assertFalse(model.isPathSelected(path2));
        assertFalse(model.isPathSelected(path3));
        assertFalse(model.isPathSelected(path4));
        model.addSelectionPath(path2);
        assertTrue(model.isPathSelected(path1));
        assertTrue(model.isPathSelected(path2));
        assertFalse(model.isPathSelected(path3));
        assertFalse(model.isPathSelected(path4));
        model.addSelectionPaths(new TreePath[] { path3, path4 });
        assertTrue(model.isPathSelected(path1));
        assertTrue(model.isPathSelected(path2));
        assertTrue(model.isPathSelected(path3));
        assertTrue(model.isPathSelected(path4));
        assertFalse(model.isPathSelected(null));
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.addSelectionPaths(TreePath[])'
     */
    public void testAddSelectionPaths() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        ConcreteTreeSelectionListener treeSelectionListener = new ConcreteTreeSelectionListener(
                false);
        model.addTreeSelectionListener(treeSelectionListener);
        model.addSelectionPaths(new TreePath[] { path1 });
        assertEquals(path1, treeSelectionListener.event.getPaths()[0]);
        assertTrue(treeSelectionListener.event.isAddedPath(path1));
        treeSelectionListener.reset();
        model.addSelectionPaths(new TreePath[] { path1 });
        assertNull(treeSelectionListener.event);
        model.addSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertEquals(2, treeSelectionListener.event.getPaths().length);
        assertEquals(path2, treeSelectionListener.event.getPaths()[0]);
        assertEquals(path3, treeSelectionListener.event.getPaths()[1]);
        assertTrue(treeSelectionListener.event.isAddedPath(path2));
        assertTrue(treeSelectionListener.event.isAddedPath(path3));
        assertNotNull(treeSelectionListener.event.getOldLeadSelectionPath());
        assertNotNull(treeSelectionListener.event.getNewLeadSelectionPath());
    }

    public void testAddSelectionPaths_SelectionModes() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        TreePath path5 = new TreePath("5");
        TreePath path6 = new TreePath("6");
        model.clearSelection();
        model.setRowMapper(new MyRowMapper());
        model.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        model.addSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertEquals(1, model.getSelectionCount());
        model.clearSelection();
        model.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        model.addSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertEquals(3, model.getSelectionCount());
        model.clearSelection();
        model.setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        model.addSelectionPaths(new TreePath[] { path1, path2, path4 });
        assertEquals(2, model.getSelectionCount());
        assertEquals(path1, model.getSelectionPaths()[0]);
        assertEquals(path2, model.getSelectionPaths()[1]);
        model.addSelectionPaths(new TreePath[] { path3 });
        assertEquals(3, model.getSelectionCount());
        model.addSelectionPaths(new TreePath[] { path5, path6 });
        assertEquals(2, model.getSelectionCount());
        assertEquals(path5, model.getSelectionPaths()[0]);
        assertEquals(path6, model.getSelectionPaths()[1]);
        model.addSelectionPaths(new TreePath[] { path1, path2, path3, path4 });
        assertEquals(6, model.getSelectionCount());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.getSelectionPaths()'
     */
    public void testGetSelectionPaths() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        assertNull(model.getSelectionPaths());
        model.addSelectionPaths(new TreePath[] { path1 });
        assertEquals(1, model.getSelectionPaths().length);
        assertEquals(path1, model.getSelectionPaths()[0]);
        model.addSelectionPath(path4);
        assertEquals(2, model.getSelectionPaths().length);
        assertEquals(path1, model.getSelectionPaths()[0]);
        assertEquals(path4, model.getSelectionPaths()[1]);
        model.addSelectionPaths(new TreePath[] { path4, path2, path3 });
        assertEquals(4, model.getSelectionPaths().length);
        TreePath[] paths = model.getSelectionPaths();
        assertTrue(find(paths, path1));
        assertTrue(find(paths, path2));
        assertTrue(find(paths, path3));
        assertTrue(find(paths, path4));
    }

    private boolean find(final Object[] array, final Object o) {
        if (array == null) {
            return false;
        }
        for (int i = 0; i < array.length; i++) {
            if (o == null && array[i] == null || o.equals(array[i])) {
                return true;
            }
        }
        return false;
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.getSelectionCount()'
     */
    public void testGetSelectionCount() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        assertEquals(0, model.getSelectionCount());
        model.addSelectionPaths(new TreePath[] { path1 });
        assertEquals(1, model.getSelectionCount());
        model.addSelectionPaths(new TreePath[] { path1 });
        assertEquals(1, model.getSelectionCount());
        model.addSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertEquals(3, model.getSelectionCount());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.clearSelection()'
     */
    public void testClearSelection() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        ConcreteTreeSelectionListener treeSelectionListener = new ConcreteTreeSelectionListener(
                false);
        model.addTreeSelectionListener(treeSelectionListener);
        assertEquals(0, model.getSelectionCount());
        model.clearSelection();
        assertNull(treeSelectionListener.event);
        model.addSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertEquals(3, model.getSelectionCount());
        treeSelectionListener.reset();
        model.clearSelection();
        assertEquals(0, model.getSelectionCount());
        assertNull(treeSelectionListener.event.getNewLeadSelectionPath());
        assertEquals(3, treeSelectionListener.event.getPaths().length);
        assertFalse(treeSelectionListener.event.isAddedPath(0));
        assertFalse(treeSelectionListener.event.isAddedPath(1));
        assertFalse(treeSelectionListener.event.isAddedPath(2));
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.getLeadSelectionPath()'
     */
    public void testGetLeadSelectionPath() {
        TreePath path1 = new TreePath("1");
        assertNull(model.getLeadSelectionPath());
        model.leadPath = path1;
        assertEquals(path1, model.getLeadSelectionPath());
        model.leadPath = null;
        assertNull(model.getLeadSelectionPath());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.getLeadSelectionRow()'
     */
    public void testGetLeadSelectionRow() {
        TreePath path1 = new TreePath("1");
        assertNull(model.getLeadSelectionPath());
        model.leadPath = path1;
        assertEquals(-1, model.getLeadSelectionRow());
        model.leadPath = null;
        assertEquals(-1, model.getLeadSelectionRow());
        model.setRowMapper(new MyRowMapper());
        assertEquals(-1, model.getLeadSelectionRow());
        model.leadPath = path1;
        assertEquals(-1, model.getLeadSelectionRow());
        model.selection = new TreePath[] { path1 };
        model.resetRowSelection();
        assertEquals(1, model.getLeadSelectionRow());
        model.leadPath = null;
        model.resetRowSelection();
        assertEquals(-1, model.getLeadSelectionRow());
        model.leadPath = path1;
        model.leadRow = 100;
        assertEquals(100, model.getLeadSelectionRow());
        model.resetRowSelection();
        assertEquals(1, model.getLeadSelectionRow());
        assertEquals(1, model.leadRow);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.getMaxSelectionRow()'
     */
    public void testGetMaxSelectionRow() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        TreePath path5 = new TreePath("5");
        TreePath path6 = new TreePath("a");
        TreePath[] paths1 = new TreePath[] { path1, path2, path3, path4, path5 };
        TreePath[] paths3 = new TreePath[] { path2, path4, path1 };
        TreePath[] paths4 = new TreePath[] { path2, path1, path3, path5 };
        TreePath[] paths5 = new TreePath[] { path6, path4, path2 };
        TreePath[] paths6 = new TreePath[] { path6 };
        assertEquals(-1, model.getMaxSelectionRow());
        model.setSelectionPaths(paths1);
        assertEquals(-1, model.getMaxSelectionRow());
        model.setRowMapper(new MyRowMapper());
        assertEquals(5, model.getMaxSelectionRow());
        model.setSelectionPaths(paths3);
        assertEquals(4, model.getMaxSelectionRow());
        model.setSelectionPaths(paths4);
        assertEquals(5, model.getMaxSelectionRow());
        model.setSelectionPaths(paths5);
        assertEquals(4, model.getMaxSelectionRow());
        model.setSelectionPaths(paths6);
        assertEquals(-1, model.getMaxSelectionRow());
        model.setSelectionPaths(new TreePath[0]);
        assertEquals(-1, model.getMaxSelectionRow());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.getMinSelectionRow()'
     */
    public void testGetMinSelectionRow() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        TreePath path5 = new TreePath("5");
        TreePath path6 = new TreePath("a");
        TreePath[] paths1 = new TreePath[] { path1, path2, path3, path4, path5 };
        TreePath[] paths3 = new TreePath[] { path2, path4, path5 };
        TreePath[] paths4 = new TreePath[] { path2, path1, path3, path5 };
        TreePath[] paths5 = new TreePath[] { path6, path5, path2 };
        TreePath[] paths6 = new TreePath[] { path6 };
        assertEquals(-1, model.getMinSelectionRow());
        model.setSelectionPaths(paths1);
        assertEquals(-1, model.getMinSelectionRow());
        model.setRowMapper(new MyRowMapper());
        assertEquals(1, model.getMinSelectionRow());
        model.setSelectionPaths(paths3);
        assertEquals(2, model.getMinSelectionRow());
        model.setSelectionPaths(paths4);
        assertEquals(1, model.getMinSelectionRow());
        model.setSelectionPaths(paths5);
        assertEquals(2, model.getMinSelectionRow());
        model.setSelectionPaths(paths6);
        assertEquals(-1, model.getMinSelectionRow());
        model.setSelectionPaths(new TreePath[0]);
        assertEquals(-1, model.getMinSelectionRow());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.getSelectionMode()'
     */
    public void testGetSelectionMode() {
        assertEquals(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION, model.getSelectionMode());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.setSelectionMode(int)'
     */
    public void testSetSelectionMode() {
        int mode1 = TreeSelectionModel.SINGLE_TREE_SELECTION;
        int mode2 = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION;
        int mode3 = TreeSelectionModel.CONTIGUOUS_TREE_SELECTION;
        int mode4 = 100;
        PropertyChangeController listener = new PropertyChangeController();
        model.addPropertyChangeListener(listener);
        model.setSelectionMode(mode1);
        listener.checkLastPropertyFired(model, "selectionMode", new Integer(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION), new Integer(mode1));
        model.setSelectionMode(mode2);
        listener.checkLastPropertyFired(model, "selectionMode", new Integer(mode1),
                new Integer(mode2));
        model.setSelectionMode(mode3);
        listener.checkLastPropertyFired(model, "selectionMode", new Integer(mode2),
                new Integer(mode3));
        model.setSelectionMode(mode4);
        listener.checkLastPropertyFired(model, "selectionMode", new Integer(mode3),
                new Integer(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION));
        TreePath path1 = new TreePath("1");
//        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        TreePath path5 = new TreePath("5");
        model.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        model.addSelectionPaths(new TreePath[] { path1, path3, path4 });
        model.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        if (!isHarmony()) {
            model.insureRowContinuity();
        }
        assertEquals(1, model.getSelectionCount());
        assertEquals(path1, model.getSelectionPath());
        model.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        model.addSelectionPaths(new TreePath[] { path1, path3, path5 });
        model.setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        assertEquals(3, model.getSelectionCount());
        model.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        model.setRowMapper(new MyRowMapper());
        model.setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        if (!isHarmony()) {
            model.insureRowContinuity();
        }
        assertEquals(1, model.getSelectionCount());
        model.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        model.setSelectionPaths(new TreePath[] { path1, path3, path4 });
        model.setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        if (!isHarmony()) {
            model.insureRowContinuity();
        }
        assertEquals(1, model.getSelectionCount());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.getSelectionRows()'
     */
    public void testGetSelectionRows() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        TreePath path5 = new TreePath("5");
        TreePath path6 = new TreePath("a");
        TreePath[] paths1 = new TreePath[] { path1, path2, path3, path4, path5 };
        TreePath[] paths3 = new TreePath[] { path2, path4, path1 };
        TreePath[] paths4 = new TreePath[] { path2, path1, path3, path5 };
        TreePath[] paths5 = new TreePath[] { path6, path4, path2 };
        TreePath[] paths6 = new TreePath[] { path6 };
        assertNull(model.getSelectionRows());
        model.setSelectionPaths(paths1);
        assertNull(model.getSelectionRows());
        model.setRowMapper(new MyRowMapper());
        assertEquals(5, model.getSelectionRows().length);
        model.setSelectionPaths(paths3);
        assertEquals(3, model.getSelectionRows().length);
        model.setSelectionPaths(paths4);
        assertEquals(4, model.getSelectionRows().length);
        model.setSelectionPaths(paths5);
        assertEquals(2, model.getSelectionRows().length);
        model.setSelectionPaths(paths6);
        assertNull(model.getSelectionRows());
        model.setSelectionPaths(new TreePath[0]);
        assertNull(model.getSelectionRows());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.isRowSelected(int)'
     */
    public void testIsRowSelected() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        TreePath path5 = new TreePath("5");
        TreePath path6 = new TreePath("a");
        TreePath[] paths1 = new TreePath[] { path1, path2, path3, path4, path5 };
        TreePath[] paths3 = new TreePath[] { path2, path4, path5 };
        TreePath[] paths6 = new TreePath[] { path6 };
        assertFalse(model.isRowSelected(-1));
        model.setSelectionPaths(paths1);
        assertFalse(model.isRowSelected(0));
        assertFalse(model.isRowSelected(1));
        model.setRowMapper(new MyRowMapper());
        assertFalse(model.isRowSelected(0));
        assertTrue(model.isRowSelected(1));
        model.setSelectionPaths(paths3);
        assertFalse(model.isRowSelected(1));
        assertTrue(model.isRowSelected(2));
        assertFalse(model.isRowSelected(3));
        assertTrue(model.isRowSelected(4));
        assertTrue(model.isRowSelected(5));
        model.setSelectionPaths(paths6);
        assertFalse(model.isRowSelected(-1));
        assertFalse(model.isRowSelected(0));
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.isSelectionEmpty()'
     */
    public void testIsSelectionEmpty() {
        TreePath path1 = new TreePath("1");
        assertTrue(model.isSelectionEmpty());
        model.addSelectionPath(path1);
        assertFalse(model.isSelectionEmpty());
        model.removeSelectionPath(path1);
        assertTrue(model.isSelectionEmpty());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.removeSelectionPath(TreePath)'
     */
    public void testRemoveSelectionPath() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        assertEquals(0, model.getSelectionCount());
        model.addSelectionPaths(new TreePath[] { path1, path2, path3 });
        ConcreteTreeSelectionListener treeSelectionListener = new ConcreteTreeSelectionListener(
                false);
        model.addTreeSelectionListener(treeSelectionListener);
        model.removeSelectionPath(path4);
        assertNull(treeSelectionListener.event);
        model.removeSelectionPath(path2);
        assertEquals(2, model.getSelectionCount());
        assertNotNull(treeSelectionListener.event.getNewLeadSelectionPath());
        assertEquals(1, treeSelectionListener.event.getPaths().length);
        assertFalse(treeSelectionListener.event.isAddedPath(0));
        treeSelectionListener.reset();
        model.removeSelectionPath(path1);
        assertEquals(1, model.getSelectionCount());
        assertNotNull(treeSelectionListener.event.getNewLeadSelectionPath());
        assertEquals(1, treeSelectionListener.event.getPaths().length);
        assertFalse(treeSelectionListener.event.isAddedPath(0));
        treeSelectionListener.reset();
        model.removeSelectionPath(path3);
        assertEquals(0, model.getSelectionCount());
        assertNull(treeSelectionListener.event.getNewLeadSelectionPath());
        assertEquals(1, treeSelectionListener.event.getPaths().length);
        assertFalse(treeSelectionListener.event.isAddedPath(0));
        treeSelectionListener.reset();
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.removeSelectionPaths(TreePath[])'
     */
    public void testRemoveSelectionPaths() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        TreePath path5 = new TreePath("5");
        assertEquals(0, model.getSelectionCount());
        model.addSelectionPaths(new TreePath[] { path1, path2, path3, path4 });
        ConcreteTreeSelectionListener treeSelectionListener = new ConcreteTreeSelectionListener(
                false);
        model.addTreeSelectionListener(treeSelectionListener);
        model.removeSelectionPaths(new TreePath[] { path5, path5 });
        assertEquals(4, model.getSelectionCount());
        assertNull(treeSelectionListener.event);
        model.removeSelectionPaths(new TreePath[] { path1, path3, path5 });
        assertEquals(2, model.getSelectionCount());
        assertNotNull(treeSelectionListener.event.getNewLeadSelectionPath());
        assertEquals(2, treeSelectionListener.event.getPaths().length);
        assertFalse(treeSelectionListener.event.isAddedPath(0));
        assertFalse(treeSelectionListener.event.isAddedPath(1));
        treeSelectionListener.reset();
        model.removeSelectionPaths(null);
        assertEquals(2, model.getSelectionCount());
        assertNull(treeSelectionListener.event);
        model.removeSelectionPaths(new TreePath[] { path2, path4, path1, path5 });
        assertEquals(0, model.getSelectionCount());
        assertNull(treeSelectionListener.event.getNewLeadSelectionPath());
        assertEquals(2, treeSelectionListener.event.getPaths().length);
        assertFalse(treeSelectionListener.event.isAddedPath(0));
        assertFalse(treeSelectionListener.event.isAddedPath(1));
        treeSelectionListener.reset();
    }

    public void testRemoveSelectionPaths_SelectionModes() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        assertEquals(0, model.getSelectionCount());
        model.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        model.setSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertEquals(3, model.getSelectionCount());
        model.removeSelectionPaths(new TreePath[] { path2 });
        assertEquals(2, model.getSelectionCount());
        model.setSelectionPaths(new TreePath[] { path1, path2, path4 });
        assertEquals(3, model.getSelectionCount());
        model.removeSelectionPaths(new TreePath[] { path2 });
        assertEquals(2, model.getSelectionCount());
        model.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        model.setSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertEquals(1, model.getSelectionCount());
        assertEquals(path1, model.getSelectionPath());
        model.removeSelectionPaths(new TreePath[] { path1 });
        assertEquals(0, model.getSelectionCount());
        model.setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        model.setSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertEquals(3, model.getSelectionCount());
        model.removeSelectionPaths(new TreePath[] { path2 });
        assertEquals(2, model.getSelectionCount());
        model.setRowMapper(new MyRowMapper());
        model.setSelectionPaths(new TreePath[] { path1, path2, path3 });
        model.removeSelectionPaths(new TreePath[] { path1 });
        assertEquals(2, model.getSelectionCount());
        model.setSelectionPaths(new TreePath[] { path1, path2, path3 });
        model.removeSelectionPaths(new TreePath[] { path2 });
        assertEquals(0, model.getSelectionCount());
        model.setSelectionPaths(new TreePath[] { path1, path2, path3 });
        model.removeSelectionPaths(new TreePath[] { path3 });
        assertEquals(2, model.getSelectionCount());
        model.setSelectionPaths(new TreePath[] { path1, path2, path3, path4 });
        model.removeSelectionPaths(new TreePath[] { path1 });
        assertEquals(3, model.getSelectionCount());
        model.setSelectionPaths(new TreePath[] { path1, path2, path3, path4 });
        model.removeSelectionPaths(new TreePath[] { path2 });
        assertEquals(0, model.getSelectionCount());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.resetRowSelection()'
     */
    public void testResetRowSelection() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        model.setRowMapper(new MyRowMapper());
        model.leadPath = path1;
        model.selection = new TreePath[] { path1, path2, path3 };
        assertEquals(-1, model.getLeadSelectionRow());
        assertEquals(-1, model.getMaxSelectionRow());
        assertEquals(-1, model.getMinSelectionRow());
        model.resetRowSelection();
        assertEquals(1, model.getLeadSelectionRow());
        assertEquals(3, model.getMaxSelectionRow());
        assertEquals(1, model.getMinSelectionRow());
        model.leadPath = null;
        model.resetRowSelection();
        assertEquals(-1, model.getLeadSelectionRow());
        assertEquals(3, model.getMaxSelectionRow());
        assertEquals(1, model.getMinSelectionRow());
        model.leadPath = path1;
        model.leadRow = 100;
        assertEquals(100, model.getLeadSelectionRow());
        model.resetRowSelection();
        assertEquals(1, model.getLeadSelectionRow());
        assertEquals(1, model.leadRow);
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.setRowMapper(RowMapper)'
     */
    public void testSetRowMapper() {
        RowMapper mapper1 = new MyRowMapper();
        RowMapper mapper2 = new MyRowMapper();
        model.setRowMapper(mapper1);
        assertEquals(mapper1, model.getRowMapper());
        model.setRowMapper(mapper2);
        assertEquals(mapper2, model.getRowMapper());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.getRowMapper()'
     */
    public void testGetRowMapper() {
        assertNull(model.getRowMapper());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.setSelectionPaths(TreePath[])'
     */
    public void testSetSelectionPaths() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        ConcreteTreeSelectionListener treeSelectionListener = new ConcreteTreeSelectionListener(
                false);
        assertEquals(0, model.getSelectionCount());
        model.setSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertEquals(3, model.getSelectionCount());
        model.addTreeSelectionListener(treeSelectionListener);
        model.setSelectionPaths(new TreePath[] { path3, path4 });
        assertEquals(2, model.getSelectionCount());
        assertNotNull(treeSelectionListener.event.getNewLeadSelectionPath());
        assertEquals(3, treeSelectionListener.event.getPaths().length);
        assertTrue(treeSelectionListener.event.isAddedPath(0));
        assertFalse(treeSelectionListener.event.isAddedPath(1));
        assertFalse(treeSelectionListener.event.isAddedPath(2));
        treeSelectionListener.reset();
        model.setSelectionPaths(new TreePath[] { path3, path4 });
        assertNull(treeSelectionListener.event);
        model.setSelectionPaths(null);
        assertEquals(0, model.getSelectionCount());
        assertNull(treeSelectionListener.event.getNewLeadSelectionPath());
        assertEquals(2, treeSelectionListener.event.getPaths().length);
        assertFalse(treeSelectionListener.event.isAddedPath(0));
        assertFalse(treeSelectionListener.event.isAddedPath(1));
        treeSelectionListener.reset();
        model.setSelectionPaths(new TreePath[] { path4, path4 });
        assertEquals(1, model.getSelectionCount());
        assertEquals(path4, treeSelectionListener.event.getNewLeadSelectionPath());
        assertEquals(1, treeSelectionListener.event.getPaths().length);
        assertTrue(treeSelectionListener.event.isAddedPath(0));
        treeSelectionListener.reset();
    }

    public void testSetSelectionPaths_SelectionModes() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        assertEquals(0, model.getSelectionCount());
        model.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        model.setSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertEquals(3, model.getSelectionCount());
        model.setSelectionPaths(new TreePath[] { path1, path2, path4 });
        assertEquals(3, model.getSelectionCount());
        model.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        model.setSelectionPaths(new TreePath[] { path1, path3, path4 });
        assertEquals(1, model.getSelectionCount());
        assertEquals(path1, model.getSelectionPath());
        model.setSelectionPaths(new TreePath[] { path1, path2, path4 });
        assertEquals(1, model.getSelectionCount());
        assertEquals(path1, model.getSelectionPath());
        model.setSelectionPaths(new TreePath[] {});
        assertNull(model.getSelectionPath());
        model.setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        model.setSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertEquals(3, model.getSelectionCount());
        model.setRowMapper(new MyRowMapper());
        model.setSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertEquals(3, model.getSelectionCount());
        model.setSelectionPaths(new TreePath[] { path1, path3, path4 });
        assertEquals(1, model.getSelectionCount());
        assertEquals(path1, model.getSelectionPath());
        model.setSelectionPaths(new TreePath[] { path1, path2, path4 });
        assertEquals(1, model.getSelectionCount());
        assertEquals(path1, model.getSelectionPaths()[0]);
        model.setSelectionPaths(new TreePath[] {});
        assertEquals(0, model.getSelectionCount());
    }

    /*
     * Test method for 'javax.swing.tree.DefaultTreeSelectionModel.setSelectionPath(TreePath)'
     */
    public void testSetSelectionPath() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        ConcreteTreeSelectionListener treeSelectionListener = new ConcreteTreeSelectionListener(
                false);
        assertEquals(0, model.getSelectionCount());
        model.addSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertEquals(3, model.getSelectionCount());
        model.addTreeSelectionListener(treeSelectionListener);
        model.setSelectionPath(path1);
        assertEquals(1, model.getSelectionCount());
        assertEquals(path1, treeSelectionListener.event.getNewLeadSelectionPath());
        assertEquals(2, treeSelectionListener.event.getPaths().length);
        assertFalse(treeSelectionListener.event.isAddedPath(0));
        assertFalse(treeSelectionListener.event.isAddedPath(1));
        treeSelectionListener.reset();
        model.setSelectionPath(path1);
        assertNull(treeSelectionListener.event);
        model.setSelectionPath(null);
        assertEquals(0, model.getSelectionCount());
        assertNull(treeSelectionListener.event.getNewLeadSelectionPath());
        assertEquals(1, treeSelectionListener.event.getPaths().length);
        assertFalse(treeSelectionListener.event.isAddedPath(0));
        treeSelectionListener.reset();
    }

    public void testCanPathsBeAdded() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        assertTrue(model.canPathsBeAdded(null));
        model.clearSelection();
        model.setRowMapper(new MyRowMapper());
        model.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        assertTrue(model.canPathsBeAdded(new TreePath[] { path1, path2, path3 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] { path1, path3 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] {}));
        model.addSelectionPath(path4);
        assertTrue(model.canPathsBeAdded(new TreePath[] { path1, path2, path3 }));
        assertFalse(model.canPathsBeAdded(new TreePath[] { path1, path3 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] { path4 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] {}));
        model.clearSelection();
        model.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        assertTrue(model.canPathsBeAdded(new TreePath[] { path1, path2, path3 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] { path1, path3 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] { path4 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] {}));
        model.addSelectionPath(path4);
        assertTrue(model.canPathsBeAdded(new TreePath[] { path1, path2, path3 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] { path1, path3 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] { path4 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] {}));
        model.setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        model.clearSelection();
        assertTrue(model.canPathsBeAdded(new TreePath[] { path1, path2, path3 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] { path1, path3 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] { path4 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] {}));
        model.addSelectionPath(path4);
        assertTrue(model.canPathsBeAdded(new TreePath[] { path1, path2, path3 }));
        assertFalse(model.canPathsBeAdded(new TreePath[] { path1, path3 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] { path4 }));
        assertTrue(model.canPathsBeAdded(new TreePath[] {}));
    }

    public void testCanPathsBeRemoved() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        assertEquals(0, model.getSelectionCount());
        model.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        model.setSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertTrue(model.canPathsBeRemoved(new TreePath[] { path2 }));
        assertTrue(model.canPathsBeRemoved(new TreePath[] { path4 }));
        assertTrue(model.canPathsBeRemoved(new TreePath[] {}));
        model.setSelectionPaths(new TreePath[] { path1, path2, path4 });
        assertTrue(model.canPathsBeRemoved(new TreePath[] { path2 }));
        model.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        model.setSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertTrue(model.canPathsBeRemoved(new TreePath[] { path1 }));
        model.setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        model.setSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertTrue(model.canPathsBeRemoved(new TreePath[] { path2 }));
        model.setRowMapper(new MyRowMapper());
        model.setSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertTrue(model.canPathsBeRemoved(new TreePath[] { path1 }));
        assertFalse(model.canPathsBeRemoved(new TreePath[] { path2 }));
        assertTrue(model.canPathsBeRemoved(new TreePath[] { path3 }));
        model.setSelectionPaths(new TreePath[] { path1, path2, path3, path4 });
        assertTrue(model.canPathsBeRemoved(new TreePath[] { path1 }));
        assertFalse(model.canPathsBeRemoved(new TreePath[] { path2 }));
        assertFalse(model.canPathsBeRemoved(new TreePath[] { path3 }));
        assertTrue(model.canPathsBeRemoved(new TreePath[] { path4 }));
    }

    @SuppressWarnings("unchecked")
    public void testNotifyPathChange() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        Vector holders = new Vector();
        holders.add(new PathPlaceHolder(path1, true));
        holders.add(new PathPlaceHolder(path2, true));
        holders.add(new PathPlaceHolder(path3, false));
        ConcreteTreeSelectionListener treeSelectionListener = new ConcreteTreeSelectionListener(
                false);
        assertEquals(0, model.getSelectionCount());
        model.addTreeSelectionListener(treeSelectionListener);
        model.notifyPathChange(holders, path4);
        assertEquals(0, model.getSelectionCount());
        assertNotNull(treeSelectionListener.event);
        assertEquals(path4, treeSelectionListener.event.getOldLeadSelectionPath());
        assertNull(treeSelectionListener.event.getNewLeadSelectionPath());
        assertEquals(3, treeSelectionListener.event.getPaths().length);
        assertTrue(treeSelectionListener.event.isAddedPath(0));
        assertTrue(treeSelectionListener.event.isAddedPath(1));
        assertTrue(treeSelectionListener.event.isAddedPath(1));
        treeSelectionListener.reset();
    }

    public void testUpdateLeadIndex() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        TreePath path5 = new TreePath("5");
        model.addSelectionPaths(new TreePath[] { path1, path2, path3, path4 });
        assertEquals(3, model.leadIndex);
        model.leadIndex = 1000;
        model.updateLeadIndex();
        assertEquals(3, model.leadIndex);
        model.leadPath = null;
        model.updateLeadIndex();
        assertEquals(-1, model.leadIndex);
        model.leadPath = path1;
        model.updateLeadIndex();
        assertEquals(0, model.leadIndex);
        model.leadPath = path5;
        model.updateLeadIndex();
        assertEquals(-1, model.leadIndex);
    }

    class MyRowMapper implements RowMapper {
        public int[] getRowsForPaths(TreePath[] path) {
            if (path == null) {
                return new int[0];
            }
            int[] result = new int[path.length];
            for (int i = 0; i < result.length; i++) {
                String str = (String) path[i].getLastPathComponent();
                if (!str.equals("a")) {
                    result[i] = Integer.valueOf(str).intValue();
                } else {
                    result[i] = -1;
                }
            }
            return result;
        }
    };

    public void testArePathsContiguous() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        TreePath path4 = new TreePath("4");
        TreePath path5 = new TreePath("5");
        TreePath path6 = new TreePath("a");
        TreePath[] paths1 = new TreePath[] { path1, path2, path3, path4, path5 };
        TreePath[] paths2 = new TreePath[] { path2, path4, path1, path5, path3 };
        TreePath[] paths3 = new TreePath[] { path1, path4, path5 };
        TreePath[] paths4 = new TreePath[] { path2, path1, path3, path5 };
        TreePath[] paths5 = new TreePath[] { path6, path1, path2 };
        TreePath[] paths6 = new TreePath[] { path6 };
        TreePath[] paths7 = new TreePath[] { path1, path2, path3, path4, path4, path5 };
        assertTrue(model.arePathsContiguous(paths1));
        model.setRowMapper(new MyRowMapper());
        assertTrue(model.arePathsContiguous(paths1));
        assertTrue(model.arePathsContiguous(paths2));
        assertFalse(model.arePathsContiguous(paths3));
        assertFalse(model.arePathsContiguous(paths4));
        assertFalse(model.arePathsContiguous(paths5));
        assertTrue(model.arePathsContiguous(paths6));
        assertTrue(model.arePathsContiguous(paths7));
        assertTrue(model.arePathsContiguous(new TreePath[0]));
    }

    public void testToString() {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        assertNotNull(model.toString());
        model.addSelectionPaths(new TreePath[] { path1, path2, path3 });
        assertNotNull(model.toString());
    }

    public void testClone() throws CloneNotSupportedException {
        TreePath path1 = new TreePath("1");
        TreePath path2 = new TreePath("2");
        TreePath path3 = new TreePath("3");
        model.addSelectionPaths(new TreePath[] { path1, path2, path3 });
        model.addTreeSelectionListener(new ConcreteTreeSelectionListener());
        RowMapper mapper = new MyRowMapper();
        model.setRowMapper(mapper);
        model.setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        Object cloned = model.clone();
        assertTrue(cloned instanceof DefaultTreeSelectionModel);
        DefaultTreeSelectionModel clonedModel = (DefaultTreeSelectionModel) cloned;
        assertEquals(model.getSelectionCount(), clonedModel.getSelectionCount());
        assertEquals(model.getLeadSelectionPath(), clonedModel.getLeadSelectionPath());
        assertEquals(model.getSelectionPaths()[0], clonedModel.getSelectionPaths()[0]);
        assertEquals(model.getSelectionPaths()[1], clonedModel.getSelectionPaths()[1]);
        assertEquals(model.getSelectionPaths()[2], clonedModel.getSelectionPaths()[2]);
        assertEquals(model.getRowMapper(), clonedModel.getRowMapper());
        assertEquals(model.getLeadSelectionRow(), clonedModel.getLeadSelectionRow());
        assertEquals(model.getSelectionMode(), clonedModel.getSelectionMode());
        assertEquals(0, clonedModel.getTreeSelectionListeners().length);
        assertEquals(model.getMinSelectionRow(), clonedModel.getMinSelectionRow());
        assertEquals(model.getMaxSelectionRow(), clonedModel.getMaxSelectionRow());
    }
}
