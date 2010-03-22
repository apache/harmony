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

import javax.swing.BasicSwingTestCase;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

public class CompoundEditTest extends AbstractUndoableEditTest {
    protected CompoundEdit ce;

    /*
     * @see AbstractUndoableEditTest#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ce = new CompoundEdit();
        obj = ce;
    }

    /*
     * Class under test for java.lang.String toString()
     */
    @Override
    public void testToString() {
        assertNotNull(ce.toString());
    }

    @Override
    public void testIsSignificant() {
        assertFalse(ce.isSignificant());
        ce.addEdit(new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE));
        assertFalse(ce.isSignificant());
        ce.addEdit(new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_TRUE));
        assertTrue(ce.isSignificant());
    }

    @Override
    public void testCanUndo() {
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(!ce.isInProgress() && (isAlive(ce) && hasBeenDone(ce)), ce.canUndo());
        }
        assertFalse(ce.canUndo());
        ce.end();
        assertTrue(ce.canUndo());
        ce.die();
        assertFalse(ce.canUndo());
    }

    @Override
    public void testCanRedo() {
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(!ce.isInProgress() && (isAlive(ce) && !hasBeenDone(ce)), ce.canRedo());
        }
        assertFalse(ce.canRedo());
        ce.end();
        ce.undo();
        assertTrue(ce.canRedo());
        ce.die();
        assertFalse(ce.canRedo());
    }

    /**
     * Behaviour of class depends from the flag.
     * UNDO, DIE: Checks that undo and die were called in the reverse of the
     * order they were created.
     * REDO: Checks that redo were called in the order they were created.
     * NAME: getPresentationName returns string with id.
     * UNDO_NAME: getUndoPresentationName returns string with id.
     * REDO_NAME: getRedoPresentationName returns string with id.
     * ADD_EDIT_TRUE: addEdit returns true.
     * ADD_EDIT_FALSE: addEdit returns false.
     * REPLACE_EDIT_FALSE: replaceEdit returns false.
     * REPLACE_EDIT_TRUE: replaceEdit returns true.
     * IS_SIGNIFICANT_TRUE: isSignificant returns true.
     * IS_SIGNIFICANT_FALSE: isSignificant returns false.
     * DISCARD: Sets flag dieCalled to true.
     * UNDO_THROW_EXCEPTION: Undo throws CannotUndoException.
     * CAN_UNDO_FALSE: canUndo returns false.
     * CAN_REDO_FALSE: canRedo returns false.
     */
    protected static class TestUndoableEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;

        public static int counter = 0;

        int id;

        public static final int DIE = 1 << 0;

        public static final int UNDO = 1 << 1;

        public static final int REDO = 1 << 2;

        public static final int NAME = 1 << 3;

        public static final int UNDO_NAME = 1 << 4;

        public static final int REDO_NAME = 1 << 5;

        public static final int ADD_EDIT_TRUE = 1 << 6;

        public static final int ADD_EDIT_FALSE = 1 << 7;

        public static final int REPLACE_EDIT_TRUE = 1 << 8;

        public static final int REPLACE_EDIT_FALSE = 1 << 9;

        public static final int IS_SIGNIFICANT_TRUE = 1 << 10;

        public static final int IS_SIGNIFICANT_FALSE = 1 << 11;

        public static final int DISCARD = 1 << 12;

        public static final int UNDO_THROW_EXCEPTION = 1 << 13;

        public static final int CAN_UNDO_FALSE = 1 << 14;

        public static final int CAN_REDO_FALSE = 1 << 15;

        /**
         * If the method die is called and flag is set to DISCARD
         * then it equals to true.
         */
        protected boolean dieCalled = false;

        public int flag;

        /**
         * If the method replaceEdit was called it equals to true.
         */
        public boolean isReplaceEditCalled = false;

        /**
         * Contains all registered change listeners.
         */
        private EventListenerList listeners;

        public TestUndoableEdit() {
            id = 0;
            flag = 0;
        }

        public TestUndoableEdit(final int flag) {
            if ((flag & DIE) != 0 || (flag & UNDO) != 0 || (flag & REDO) != 0
                    || (flag & NAME) != 0 || (flag & REDO_NAME) != 0 || (flag & UNDO_NAME) != 0) {
                id = counter++;
            } else {
                id = 0;
            }
            this.flag = flag;
            listeners = new EventListenerList();
        }

        public void addChangeListener(final ChangeListener listener) {
            listeners.add(ChangeListener.class, listener);
        }

        @Override
        public void die() {
            //System.out.println("die " + id);
            super.die();
            if ((flag & DIE) != 0) {
                assertEquals(--counter, id);
                dieCalled = true;
            }
            if ((flag & DISCARD) != 0) {
                dieCalled = true;
            }
        }

        /**
         * Returns the value of flag dieCalled.
         *
         */
        public boolean isDieCalled() {
            return dieCalled;
        }

        @Override
        public void undo() {
            //System.out.println("undo " + id);
            super.undo();
            if ((flag & UNDO) != 0) {
                assertEquals(--counter, id);
            } else if ((flag & UNDO_THROW_EXCEPTION) != 0) {
                throw new CannotUndoException();
            }
        }

        @Override
        public void redo() {
            //System.out.println("redo " + id);
            super.redo();
            if ((flag & REDO) != 0) {
                assertEquals(counter++, id);
            }
        }

        @Override
        public String getPresentationName() {
            if ((flag & NAME) != 0) {
                return String.valueOf(id);
            }
            return "";
        }

        @Override
        public String getRedoPresentationName() {
            if ((flag & REDO_NAME) != 0) {
                return String.valueOf(id);
            }
            return "";
        }

        @Override
        public String getUndoPresentationName() {
            if ((flag & UNDO_NAME) != 0) {
                return String.valueOf(id);
            }
            return "";
        }

        @Override
        public boolean addEdit(final UndoableEdit edit) {
            return ((flag & ADD_EDIT_TRUE) != 0);
        }

        @Override
        public boolean replaceEdit(final UndoableEdit edit) {
            isReplaceEditCalled = true;
            return ((flag & REPLACE_EDIT_TRUE) != 0);
        }

        @Override
        public boolean isSignificant() {
            //System.out.println("isSignificant " + id);
            return (flag & IS_SIGNIFICANT_FALSE) == 0;
        }

        @Override
        public boolean canUndo() {
            //System.out.println("canUndo " + id);
            //TODO: remove super.canUndo and add UNDO flag where it necessary
            return (flag & CAN_UNDO_FALSE) == 0 && ((flag & UNDO) != 0 || super.canUndo());
        }

        @Override
        public boolean canRedo() {
            //System.out.println("canRedo " + id);
            //TODO: remove super.canRedo and add REDO flag where it necessary
            return (flag & CAN_REDO_FALSE) == 0 && ((flag & REDO) != 0 || super.canRedo());
        }
    }

    @Override
    public void testUndo() {
        boolean bWasException = false;
        try {
            ce.undo();
        } catch (CannotUndoException e) {
            bWasException = true;
        }
        assertTrue("CannotUndoException must be thrown", bWasException);
        TestUndoableEdit.counter = 0;
        for (int i = 0; i < 10; i++) {
            ce.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO));
        }
        ce.end();
        ce.undo();
    }

    @Override
    public void testRedo() {
        boolean bWasException = false;
        try {
            ce.redo();
        } catch (CannotRedoException e) {
            bWasException = true;
        }
        assertTrue("CannotRedoException must be thrown", bWasException);
        TestUndoableEdit.counter = 0;
        for (int i = 0; i < 10; i++) {
            ce.addEdit(new TestUndoableEdit(TestUndoableEdit.REDO));
        }
        ce.end();
        ce.undo();
        TestUndoableEdit.counter = 0;
        ce.redo();
    }

    @Override
    public void testDie() {
        ce.die();
        TestUndoableEdit.counter = 0;
        final int count = 10;
        TestUndoableEdit[] edits = new TestUndoableEdit[count];
        for (int i = 0; i < count; i++) {
            edits[i] = new TestUndoableEdit(TestUndoableEdit.DIE);
            ce.addEdit(edits[i]);
        }
        ce.die();
        for (int i = 0; i < count; i++) {
            assertTrue(edits[i].isDieCalled());
        }
    }

    public void testCompoundEdit() {
        assertTrue(ce.isInProgress());
    }

    public void testEnd() {
        ce.end();
        assertFalse(ce.isInProgress());
    }

    public void testIsInProgress() {
        assertTrue(ce.isInProgress());
        ce.end();
        assertFalse(ce.isInProgress());
    }

    /**
     * Original test.
     */
    public void testGetPresentationName01() {
        assertEquals("", ce.getPresentationName());
        TestUndoableEdit.counter = 0;
        final int n = 10;
        for (int i = 0; i < n; i++) {
            ce.addEdit(new TestUndoableEdit(TestUndoableEdit.NAME));
        }
        assertEquals(String.valueOf(n - 1), ce.getPresentationName());
    }

    /**
     * Additional test.
     * Tests that whatever getPresentationName() of the last edit returns -
     * empty string in particular - the result doesn't change.
     */
    public void testGetPresentationName02() {
        assertEquals("", ce.getPresentationName());
        TestUndoableEdit.counter = 1;
        ce.addEdit(new TestUndoableEdit(TestUndoableEdit.NAME));
        assertEquals(String.valueOf(1), ce.getPresentationName());
        ce.addEdit(new TestUndoableEdit());
        assertEquals("", ce.getPresentationName());
    }

    /**
     * Tests that if <code>getPresentationName</code> of the last edit returns
     * <code>null</code>, the result is empty string.
     * 
     */
    // Regression for HARMONY-2603
    public void testGetPresentationName03() {
        ce.addEdit(new TestUndoableEdit() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getPresentationName() {
                return null;
            }
        });
        assertEquals("", ce.getPresentationName());
    }

    @Override
    public void testGetRedoPresentationName() {
        assertEquals(UIManager.getString("AbstractUndoableEdit.redoText"), ce
                .getRedoPresentationName());
        TestUndoableEdit.counter = 0;
        final int n = 10;
        for (int i = 0; i < n; i++) {
            ce.addEdit(new TestUndoableEdit(TestUndoableEdit.REDO_NAME));
        }
        assertEquals(String.valueOf(n - 1), ce.getRedoPresentationName());
    }

    @Override
    public void testGetUndoPresentationName() {
        assertEquals(UIManager.getString("AbstractUndoableEdit.undoText"), ce
                .getUndoPresentationName());
        TestUndoableEdit.counter = 0;
        int n = 10;
        for (int i = 0; i < n; i++) {
            ce.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO_NAME));
        }
        assertEquals(String.valueOf(n - 1), ce.getUndoPresentationName());
    }

    public void testLastEdit() {
        assertNull(ce.lastEdit());
        TestUndoableEdit.counter = 0;
        final int n = 10;
        for (int i = 0; i < n; i++) {
            ce.addEdit(new TestUndoableEdit(TestUndoableEdit.DIE));
        }
        assertEquals(n - 1, ((TestUndoableEdit) ce.lastEdit()).id);
    }

    @Override
    public void testAddEdit() {
        TestUndoableEdit ue = new TestUndoableEdit(TestUndoableEdit.ADD_EDIT_FALSE
                | TestUndoableEdit.UNDO | TestUndoableEdit.REDO);
        assertTrue(ce.addEdit(ue));
        assertEquals(ue, ce.edits.elementAt(0));
        ce.end();
        assertEquals(ue.canUndo(), ce.canUndo());
        assertEquals(ue.canRedo(), ue.canRedo());
        ue.flag = TestUndoableEdit.UNDO;
        assertEquals(ue.canUndo(), ce.canUndo());
        assertEquals(ue.canRedo(), ue.canRedo());
        ce = new CompoundEdit();
        ue = new TestUndoableEdit(TestUndoableEdit.ADD_EDIT_TRUE);
        assertTrue(ce.addEdit(ue));
        assertEquals(ue, ce.edits.elementAt(0));
        ue = new TestUndoableEdit(TestUndoableEdit.REPLACE_EDIT_FALSE);
        assertTrue(ce.addEdit(ue));
        assertTrue(ce.edits.size() == 1);
        ((TestUndoableEdit) ce.lastEdit()).flag = TestUndoableEdit.ADD_EDIT_FALSE;
        ue = new TestUndoableEdit(TestUndoableEdit.REPLACE_EDIT_FALSE);
        assertTrue(ce.addEdit(ue));
        assertEquals(ue, ce.edits.elementAt(1));
        ue = new TestUndoableEdit(TestUndoableEdit.REPLACE_EDIT_TRUE);
        assertTrue(ce.addEdit(ue));
        assertTrue(ce.edits.size() == 2);
        assertTrue(ue.isReplaceEditCalled);
        ce.end();
        assertTrue(ce.canUndo());
        ce.undo();
        assertTrue(ce.canRedo());
    }

    public void testEditsCapacity() { // Regression for HARMONY-2649
        assertEquals(10, ce.edits.capacity());
    }
}
