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
package javax.swing.undo;

import java.util.Hashtable;
import javax.swing.SwingTestCase;

@SuppressWarnings("unchecked")
public class StateEditTest extends SwingTestCase {
    StateEdit se1;

    StateEdit se2;

    boolean bWasException;

    SimpleEditable obj;

    class SimpleEditable implements StateEditable {
        boolean wasCallStore = false;

        boolean wasCallRestore = false;

        Hashtable state = null;

        public void storeState(final Hashtable ht) {
            ht.put("store", "state");
            ht.put("into", "this table");
            wasCallStore = true;
            state = ht;
        }

        public void restoreState(final Hashtable ht) {
            wasCallRestore = true;
            state = ht;
        }
    }

    class SimpleStateEdit extends StateEdit {
        private static final long serialVersionUID = 1L;

        boolean wasCallRemoveRedudant = false;

        Hashtable state1 = null;

        Hashtable state2 = null;

        void resetDbgInfo() {
            wasCallRemoveRedudant = false;
            state1 = null;
            state2 = null;
        }

        public SimpleStateEdit(final StateEditable s) {
            super(s);
        }

        public SimpleStateEdit(final StateEditable s, final String name) {
            super(s, name);
        }

        @Override
        protected void removeRedundantState() {
            wasCallRemoveRedudant = true;
            state1 = preState;
            state2 = postState;
            super.removeRedundantState();
        }
    }

    @Override
    protected void setUp() throws Exception {
        bWasException = false;
        se1 = new SimpleStateEdit(new SimpleEditable());
        obj = (SimpleEditable) se1.object;
        se2 = new StateEdit(new SimpleEditable(), "presentationName");
        super.setUp();
    }

    public void testGetPresentationName() {
        assertNull(se1.getPresentationName());
        assertEquals("presentationName", se2.getPresentationName());
    }

    public void testUndo() {
        se1.preState.put("1", new Integer(1));
        se1.preState.put("2", new Integer(2));
        se1.postState = new Hashtable();
        se1.postState.put("3", new Integer(3));
        se1.postState.put("4", new Integer(4));
        Hashtable oldPreState = se1.preState;
        Hashtable oldPostState = se1.postState;
        se1.undo();
        assertTrue(obj.wasCallRestore);
        assertEquals(obj.state, se1.preState);
        assertEquals(oldPreState, se1.preState);
        assertEquals(oldPostState, se1.postState);
    }

    public void testRedo() {
        try {
            se1.redo();
        } catch (CannotRedoException e) {
            bWasException = true;
        }
        assertTrue("ExpectedException", bWasException);
        se1.undo();
        obj.wasCallRestore = false;
        se1.preState.put("1", new Integer(1));
        se1.preState.put("2", new Integer(2));
        se1.postState = new Hashtable();
        se1.postState.put("3", new Integer(3));
        se1.postState.put("4", new Integer(4));
        Hashtable oldPreState = se1.preState;
        Hashtable oldPostState = se1.postState;
        se1.redo();
        assertTrue(obj.wasCallRestore);
        assertEquals(se1.postState, obj.state);
        assertEquals(oldPreState, se1.preState);
        assertEquals(oldPostState, se1.postState);
    }

    public void testStateEditStateEditableString() {
    }

    public void testStateEditStateEditable() {
    }

    public void testEnd() {
        SimpleStateEdit stEdit = (SimpleStateEdit) se1;
        se1.preState.put("1", new Integer(1));
        se1.preState.put("2", new Integer(2));
        se1.postState = new Hashtable();
        se1.postState.put("3", new Integer(3));
        se1.postState.put("4", new Integer(4));
        Hashtable oldPreState = se1.preState;
        stEdit.resetDbgInfo();
        se1.end();
        assertTrue(obj.wasCallStore);
        assertEquals(stEdit.state2, obj.state);
        assertEquals(stEdit.state1, oldPreState);
        assertEquals(stEdit.state2, obj.state);
        assertTrue(stEdit.wasCallRemoveRedudant);
    }

    public void testRemoveRedundantState() {
        assertNotNull(se1.preState);
        assertNull(se1.postState);
        assertEquals(2, se1.preState.size());
        se1.preState.remove("store");
        se1.preState.remove("into");
        se1.postState = new Hashtable();
        se1.preState.put("1", new Integer(1));
        se1.preState.put("2", new Integer(2));
        se1.preState.put("3", new Integer(3));
        se1.preState.put("4", new Integer(4));
        se1.preState.put("5", new Integer(5));
        se1.postState.put("1", new Integer(44));
        se1.postState.put("2x", new Integer(2));
        se1.postState.put("3x", new Integer(3));
        se1.postState.put("4x", new Integer(4));
        se1.postState.put("5", new Integer(5));
        se1.removeRedundantState();
        Hashtable preState = se1.preState;
        Hashtable postState = se1.postState;
        assertEquals(4, se1.preState.size());
        assertEquals(new Integer(1), preState.get("1"));
        assertEquals(new Integer(2), preState.get("2"));
        assertEquals(new Integer(3), preState.get("3"));
        assertEquals(new Integer(4), preState.get("4"));
        assertEquals(4, se1.postState.size());
        assertEquals(new Integer(44), postState.get("1"));
        assertEquals(new Integer(2), postState.get("2x"));
        assertEquals(new Integer(3), postState.get("3x"));
        assertEquals(new Integer(4), postState.get("4x"));
    }

    Hashtable getState(final StateEditable editable) {
        Hashtable ht = new Hashtable();
        editable.storeState(ht);
        return ht;
    }

    public void testInit() {
        SimpleEditable newObj = new SimpleEditable();
        obj.wasCallStore = false;
        obj.state = null;
        assertNull(se1.undoRedoName);
        assertEquals("presentationName", se2.undoRedoName);
        se1.init(newObj, "name");
        assertEquals(newObj, se1.object);
        assertEquals("name", se1.getPresentationName());
        assertTrue(newObj.wasCallStore);
        assertEquals(newObj.state, se1.preState);
        assertNull(se1.postState);
        assertEquals(getState(newObj), se1.preState);
        assertEquals("name", se1.undoRedoName);

        try { // Regression test for HARMONY-2536
            new StateEdit(null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        try { // Regression test for HARMONY-2536
            new StateEdit(null, "str");
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    // Regression test for HARMONY-2844
    public void testInitNull() {
        StateEdit se = new StateEdit(new SimpleEditable());
        try {
            se.init(null, "test");
            fail("NullPointerException is expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testConstants() {
        assertEquals("$Id: StateEdit.java,v 1.6 1997/10" + "/01 20:05:51 sandipc Exp $",
                StateEdit.RCSID);
        assertEquals("$Id: StateEditable.java,v 1.2 1997/09" + "/08 19:39:08 marklin Exp $",
                StateEditable.RCSID);
    }
}
