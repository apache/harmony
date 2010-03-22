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
 * @author Dmitry A. Durnev
 */
package java.awt;

import java.awt.List.AccessibleAWTList;
import java.beans.PropertyChangeEvent;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleState;
import junit.framework.TestCase;

/**
 * AccessibleAWTListTest
 */
public class AccessibleAWTListTest extends TestCase {
    List list;
    AccessibleContext ac;
    protected PropertyChangeEvent lastPropEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        list = new List();
        ac = list.getAccessibleContext();
        assertNotNull(ac);
    }

    public final void testGetAccessibleChildrenCount() {
        assertEquals(0, ac.getAccessibleChildrenCount());
        list.add("item1");
        list.add("item2");
        assertEquals(2, ac.getAccessibleChildrenCount());
    }

    public final void testGetAccessibleChild() {
        assertNull(ac.getAccessibleChild(0));
        list.add("item");
        Accessible aChild = ac.getAccessibleChild(0);
        assertNotNull(aChild);
        assertTrue(aChild instanceof AccessibleAWTList.AccessibleAWTListChild);
        assertNotNull(aChild = ac.getAccessibleChild(-2));
        assertTrue(aChild instanceof AccessibleAWTList.AccessibleAWTListChild);
        assertNull(ac.getAccessibleChild(1));
    }

    public final void testGetAccessibleRole() {
        assertSame(AccessibleRole.LIST, ac.getAccessibleRole());
    }

    public final void testGetAccessibleStateSet() {
        final AccessibleState STATE = AccessibleState.MULTISELECTABLE;
        assertFalse(ac.getAccessibleStateSet().contains(STATE));
        list.setMultipleMode(true);
        assertTrue(ac.getAccessibleStateSet().contains(STATE));
        list.setMultipleMode(false);
        assertFalse(ac.getAccessibleStateSet().contains(STATE));

    }

    public final void testGetAccessibleAt() {
        list.add("item1");
        Point p = new Point();
        assertNull(ac.getAccessibleComponent().getAccessibleAt(p));
    }

    public final void testAccessibleAWTList() {
        AccessibleAWTList aList = list.new AccessibleAWTList();
        assertNotNull(aList);
        // constructor has side-effect: listeners are added:
        assertSame(aList, list.getItemListeners()[1]);
        assertSame(aList, list.getActionListeners()[1]);
    }

    public final void testGetAccessibleSelectionCount() {
        list.add("item1");
        list.add("item2");
        AccessibleSelection as = ac.getAccessibleSelection();
        assertEquals(0, as.getAccessibleSelectionCount());
        list.select(0);
        assertEquals(1, as.getAccessibleSelectionCount());
        list.select(1);
        assertEquals(1, as.getAccessibleSelectionCount());
        list.setMultipleMode(true);
        list.select(0);
        assertEquals(2, as.getAccessibleSelectionCount());
    }

    public final void testClearAccessibleSelection() {
        list.add("item1");
        list.add("item2");
        list.setMultipleMode(true);
        AccessibleSelection as = ac.getAccessibleSelection();
        list.select(0);
        list.select(1);
        assertEquals(2, list.getSelectedItems().length);
        as.clearAccessibleSelection();
        assertEquals(0, list.getSelectedItems().length);
        assertEquals(0, as.getAccessibleSelectionCount());

    }

    public final void testSelectAllAccessibleSelection() {
        AccessibleSelection as = ac.getAccessibleSelection();
        list.add("item1");
        list.add("item2");
        assertEquals(0, list.getSelectedItems().length);
        as.selectAllAccessibleSelection();
        assertEquals(1, list.getSelectedItems().length);
        assertEquals("item1", list.getSelectedItem());
        as.selectAllAccessibleSelection();
        assertEquals(1, list.getSelectedItems().length);
        assertEquals("item1", list.getSelectedItem());
        list.setMultipleMode(true);
        as.selectAllAccessibleSelection();
        assertEquals(2, list.getSelectedItems().length);


    }

    public final void testAddAccessibleSelection() {
        AccessibleSelection as = ac.getAccessibleSelection();
        list.add("item1");
        list.add("item2");
        assertEquals(0, list.getSelectedItems().length);
        as.addAccessibleSelection(1);
        assertEquals(1, list.getSelectedItems().length);
        assertEquals("item2", list.getSelectedItem());
        as.addAccessibleSelection(0);
        assertEquals(1, list.getSelectedItems().length);
        assertEquals("item1", list.getSelectedItem());
        list.setMultipleMode(true);
        as.addAccessibleSelection(1);
        assertEquals(2, list.getSelectedItems().length);
    }

    public final void testRemoveAccessibleSelection() {
        AccessibleSelection as = ac.getAccessibleSelection();
        list.add("item1");
        list.add("item2");
        assertEquals(0, list.getSelectedItems().length);
        as.removeAccessibleSelection(0);
        as.removeAccessibleSelection(1);
        assertEquals(0, list.getSelectedItems().length);
        list.select(0);
        assertEquals(1, list.getSelectedItems().length);
        as.removeAccessibleSelection(1);
        assertEquals(1, list.getSelectedItems().length);
        as.removeAccessibleSelection(0);
        assertEquals(0, list.getSelectedItems().length);
    }

    public final void testIsAccessibleChildSelected() {
        AccessibleSelection as = ac.getAccessibleSelection();
        list.add("item1");
        list.add("item2");
        assertFalse(as.isAccessibleChildSelected(0));
        assertFalse(as.isAccessibleChildSelected(1));
        list.select(1);
        assertFalse(as.isAccessibleChildSelected(0));
        assertTrue(as.isAccessibleChildSelected(1));
        list.select(0);
        assertFalse(as.isAccessibleChildSelected(1));
        assertTrue(as.isAccessibleChildSelected(0));
    }

    /*
     * Class under test for javax.accessibility.Accessible getAccessibleSelection(int)
     */
    public final void testGetAccessibleSelectionint() {
        AccessibleSelection as = ac.getAccessibleSelection();
        list.add("item1");
        list.add("item2");
        assertNull(as.getAccessibleSelection(-1));
        assertNull(as.getAccessibleSelection(0));
        assertNull(as.getAccessibleSelection(1));
        list.select(1);
        assertNull(as.getAccessibleSelection(-1));
        assertTrue(as.getAccessibleSelection(0)
                   instanceof AccessibleAWTList.AccessibleAWTListChild);
        assertNull(as.getAccessibleSelection(1));
    }

    public final void testGetAccessibleSelection() {
        assertSame(ac, ac.getAccessibleSelection());
    }

    public final void testItemStateChanged() {
        // not implemented
    }

    public final void testActionPerformed() {
        // not implemented
    }

}
