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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.undo;

import java.util.Vector;
import javax.swing.SwingTestCase;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.PlainDocument;

public class UndoableEditSupportTest extends SwingTestCase {
    ExtUESupport ues1;

    UndoableEditSupport ues2;

    PlainDocument realSource;

    String fireOrder;

    final String PATTERN = "@[^ }]*";

    class UEListener implements UndoableEditListener {
        String name;

        UndoableEditEvent event = null;

        public UEListener(final String s) {
            name = s;
        }

        public void undoableEditHappened(final UndoableEditEvent e) {
            fireOrder += name;
            event = e;
        }
    }

    class ExtUESupport extends UndoableEditSupport {
        boolean wasCallCreate = false;

        boolean wasCallPostEdit = false;

        UndoableEdit ue = null;

        void resetDbgInfo() {
            wasCallPostEdit = false;
            wasCallCreate = false;
            ue = null;
        }

        @Override
        protected void _postEdit(final UndoableEdit e) {
            wasCallPostEdit = true;
            ue = e;
            super._postEdit(e);
        }

        @Override
        public synchronized void postEdit(final UndoableEdit a0) {
            super.postEdit(a0);
        }

        @Override
        protected CompoundEdit createCompoundEdit() {
            wasCallCreate = true;
            return super.createCompoundEdit();
        }
    }

    class ExtCompoundEdit extends CompoundEdit {
        private static final long serialVersionUID = 1L;

        boolean wasCallAddEdit = false;

        boolean wasCallEnd = false;

        public ExtCompoundEdit() {
            super();
        }

        @Override
        public boolean addEdit(final UndoableEdit anEdit) {
            wasCallAddEdit = true;
            return super.addEdit(anEdit);
        }

        @Override
        public void end() {
            wasCallEnd = true;
            super.end();
        }

        void resetDbgInfo() {
            wasCallAddEdit = false;
            wasCallEnd = false;
        }
    }

    void resetDbgInfo(final ExtCompoundEdit ce, final ExtUESupport ues) {
        ce.resetDbgInfo();
        ues.resetDbgInfo();
    }

    @Override
    protected void setUp() throws Exception {
        ues1 = new ExtUESupport();
        realSource = new PlainDocument();
        ues2 = new UndoableEditSupport(realSource);
        fireOrder = "";
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testUndoableEditSupport() {
        assertEquals(0, ues1.updateLevel);
        assertEquals(0, ues2.updateLevel);
        assertEquals(0, ues1.getUpdateLevel());
        assertEquals(0, ues2.getUpdateLevel());
        assertEquals(ues1, ues1.realSource);
        assertEquals(realSource, ues2.realSource);
        assertNull(ues1.compoundEdit);
        assertNull(ues2.compoundEdit);
        assertEquals(0, ues1.getUndoableEditListeners().length);
        assertEquals(0, ues2.getUndoableEditListeners().length);
        UndoableEditSupport ues = new UndoableEditSupport(null);
        assertEquals(ues, ues.realSource);
    }

    void checkEvent(final UndoableEditEvent e1, final UndoableEditEvent e2) {
        assertEquals(e1.getSource(), e2.getSource());
        assertEquals(e1.getEdit(), e2.getEdit());
    }

    public void test_postEdit() {
        UEListener listener1 = new UEListener("1");
        UEListener listener2 = new UEListener("2");
        UEListener listener3 = new UEListener("3");
        ues1.addUndoableEditListener(listener1);
        ues1.addUndoableEditListener(listener2);
        ues1.addUndoableEditListener(listener3);
        UndoableEdit ue = new CompoundEdit();
        UndoableEditEvent uee = new UndoableEditEvent(ues1.realSource, ue);
        ues1._postEdit(ue);
        assertEquals("123", fireOrder);
        checkEvent(uee, listener1.event);
        checkEvent(uee, listener2.event);
        checkEvent(uee, listener3.event);
    }

    public void testBeginUpdate() {
        ues1.wasCallCreate = false;
        for (int i = 0; i < 100; i++) {
            ues1.resetDbgInfo();
            ues1.beginUpdate();
            if (i == 0) {
                assertTrue(ues1.wasCallCreate);
            } else {
                assertFalse(ues1.wasCallCreate);
                assertEquals(i + 1, ues1.getUpdateLevel());
            }
        }
    }

    void checkCompoundEdit(final CompoundEdit ce, final int size) {
        assertNotNull(ce);
        assertEquals(size, ce.edits.size());
    }

    public void testCreateCompoundEdit() {
        checkCompoundEdit(ues1.createCompoundEdit(), 0);
        checkCompoundEdit(ues2.createCompoundEdit(), 0);
    }

    public void testEndUpdate() {
        UEListener listener = new UEListener("1");
        ues1.addUndoableEditListener(listener);
        ExtCompoundEdit ce = new ExtCompoundEdit();
        ues1.compoundEdit = ce;
        for (int i = 0; i < 5; i++) {
            ues1.beginUpdate();
            assertEquals(0, ues1.compoundEdit.edits.size());
        }
        ues1.compoundEdit = ce;
        for (int i = 0; i < 10; i++) {
            CompoundEdit ce1 = ues1.compoundEdit;
            UndoableEditEvent uee = new UndoableEditEvent(ues1.realSource, ce1);
            resetDbgInfo(ce, ues1);
            ues1.endUpdate();
            assertEquals(4 - i, ues1.updateLevel);
            if (ues1.updateLevel == 0) {
                assertTrue(ues1.wasCallPostEdit);
                checkEvent(uee, listener.event);
                assertEquals(ce1, ues1.ue);
                assertTrue(ce.wasCallEnd);
            } else {
                assertFalse(ues1.wasCallPostEdit);
                assertFalse(ce.wasCallEnd);
            }
            if (ues1.updateLevel > 0) {
                assertEquals(0, ues1.compoundEdit.edits.size());
            } else {
                assertNull(ues1.compoundEdit);
            }
        }
        for (int i = 0; i < 20; i++) {
            ues1.wasCallCreate = false;
            ues1.beginUpdate();
            if (ues1.updateLevel == 1) {
                assertTrue(ues1.wasCallCreate);
            } else {
                assertFalse(ues1.wasCallCreate);
            }
        }
    }

    void checkUpdateLevel(final int count, final UndoableEditSupport ues) {
        assertEquals(count, ues.updateLevel);
        assertEquals(count, ues.getUpdateLevel());
    }

    public void testGetUpdateLevel() {
        checkUpdateLevel(0, ues1);
        for (int i = 3; i < 10; i++) {
            int count = ues1.getUpdateLevel();
            for (int i1 = 0; i1 < i; i1++) {
                ues1.beginUpdate();
                count++;
                checkUpdateLevel(count, ues1);
            }
            for (int i1 = 0; i1 < i / 2; i1++) {
                ues1.endUpdate();
                count--;
                checkUpdateLevel(count, ues1);
            }
            for (int i1 = i / 2; i1 < i; i1++) {
                ues1.beginUpdate();
                count++;
                checkUpdateLevel(count, ues1);
            }
            for (int i1 = count; i1 > 0; i1--) {
                ues1.endUpdate();
                count--;
                checkUpdateLevel(count, ues1);
            }
            for (int i1 = 0; i1 > -3; i1--) {
                ues1.endUpdate();
                count--;
                checkUpdateLevel(count, ues1);
            }
        }
    }

    public void testPostEdit() {
        ues1.postEdit(null);
        assertTrue(true);
        assertNull(ues1.ue);
        UndoableEdit ue = new CompoundEdit();
        ues1.postEdit(ue);
        assertEquals(ue, ues1.ue);
    }

    String getString(final UndoableEditSupport ues) {
        return ues.getClass().getName() + " " + "updateLevel: " + ues.updateLevel + " "
                + "listeners: " + ues.listeners.toString() + " " + "compoundEdit: "
                + ues.compoundEdit;
    }

    void checkToString(final UndoableEditSupport ues) {
        assertEquals(getString(ues), ues2.toString().replaceFirst(PATTERN, ""));
    }

    public void testToString() {
        checkToString(ues2);
        ues2.beginUpdate();
        checkToString(ues2);
    }

    @SuppressWarnings("unchecked")
    void checkListeners(final UndoableEditSupport ues, final Vector v) {
        UndoableEditListener[] listeners = ues.getUndoableEditListeners();
        int length = v.size();
        assertEquals(length, listeners.length);
        for (int i = 0; i < length; i++) {
            assertEquals(v.get(i), listeners[i]);
        }
        assertEquals(v, ues.listeners);
    }

    @SuppressWarnings("unchecked")
    public void testAddRemoveGetUndoableEditListener() {
        UEListener listener1 = new UEListener("1");
        UEListener listener2 = new UEListener("2");
        UEListener listener3 = new UEListener("3");
        Vector listenersVector = new Vector();
        ues1.addUndoableEditListener(listener1);
        listenersVector.add(listener1);
        checkListeners(ues1, listenersVector);
        ues1.addUndoableEditListener(listener2);
        listenersVector.add(listener2);
        checkListeners(ues1, listenersVector);
        ues1.addUndoableEditListener(listener3);
        listenersVector.add(listener3);
        checkListeners(ues1, listenersVector);
        ues1.addUndoableEditListener(listener1);
        listenersVector.add(listener1);
        checkListeners(ues1, listenersVector);
        ues1.removeUndoableEditListener(listener2);
        listenersVector.remove(listener2);
        checkListeners(ues1, listenersVector);
        ues1.removeUndoableEditListener(listener1);
        listenersVector.remove(listener1);
        checkListeners(ues1, listenersVector);
        ues1.removeUndoableEditListener(listener3);
        listenersVector.remove(listener3);
        checkListeners(ues1, listenersVector);
    }

    public void testPostEdit2() {
        ExtCompoundEdit ce = new ExtCompoundEdit();
        ues1.compoundEdit = ce;
        resetDbgInfo(ce, ues1);
        UndoableEdit ue = new CompoundEdit();
        assertEquals(0, ues1.updateLevel);
        ues1.postEdit(ue);
        assertTrue(ues1.wasCallPostEdit);
        assertFalse(ce.wasCallAddEdit);
        ues1.updateLevel = 3;
        resetDbgInfo(ce, ues1);
        assertEquals(ce, ues1.compoundEdit);
        assertEquals(3, ues1.updateLevel);
        ues1.postEdit(ue);
        assertFalse(ues1.wasCallPostEdit);
        assertTrue(ce.wasCallAddEdit);
    }
}