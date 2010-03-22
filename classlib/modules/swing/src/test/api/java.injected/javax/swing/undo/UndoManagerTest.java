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

import javax.swing.UIManager;
import javax.swing.event.UndoableEditEvent;


public class UndoManagerTest extends CompoundEditTest {
    protected UndoManager um;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        um = new UndoManager();
        ce = um;
        obj = um;
    }

    @Override
    public void testToString() {
        assertNotNull(um.toString());
    }

    @Override
    public void testCanUndo() {
        assertFalse(um.canUndo());
        TestUndoableEdit edit1 = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE);
        um.addEdit(edit1);
        //canUndo must call isSignificant
        assertFalse(um.canUndo());
        edit1.flag |= TestUndoableEdit.CAN_UNDO_FALSE;
        TestUndoableEdit edit2 = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_TRUE);
        um.addEdit(edit2);
        //if edit is significant then canUndo must be called
        assertEquals(um.canUndo(), edit2.canUndo());
        edit2.flag |= TestUndoableEdit.CAN_UNDO_FALSE;
        assertEquals(um.canUndo(), edit2.canUndo());
        edit2.flag = TestUndoableEdit.IS_SIGNIFICANT_TRUE;
        //move index to 1
        um.undo();
        //canUndo must return false because
        //there is no any significant edit before indexOfNextAdd
        assertFalse(um.canUndo());
        //back index to 2
        um.redo();
        TestUndoableEdit edit3 = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE);
        um.addEdit(edit3);
        TestUndoableEdit edit4 = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE);
        um.addEdit(edit4);
        //isSignificant must be call from the end to start
        // we must stop on significant edit and call canUndo
        edit2.flag = TestUndoableEdit.IS_SIGNIFICANT_TRUE;
        assertEquals(um.canUndo(), edit2.canUndo());
        edit2.flag |= TestUndoableEdit.CAN_UNDO_FALSE;
        assertEquals(um.canUndo(), edit2.canUndo());
        //first significant's canUndo returns false
        //second's one returns true
        edit3.flag = TestUndoableEdit.IS_SIGNIFICANT_TRUE | TestUndoableEdit.CAN_UNDO_FALSE;
        edit2.flag = TestUndoableEdit.IS_SIGNIFICANT_TRUE;
        // must look only on last significant
        assertEquals(um.canUndo(), edit3.canUndo());
        //if not inProgress
        //set inProgress to false
        um.end();
        assertFalse(um.isInProgress());
        //now um must call to super.canUndo
        // must be true if alive && hasBeenDone
        assertTrue(um.canUndo());
        //die sets alive to false
        um.die();
        assertFalse(um.canUndo());
    }

    @Override
    public void testCanRedo() {
        //empty must return false
        assertFalse(um.canRedo());
        TestUndoableEdit edit1 = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE);
        um.addEdit(edit1);
        //canRedo must call isSignificant
        assertFalse(um.canRedo());
        edit1.flag |= TestUndoableEdit.CAN_REDO_FALSE;
        TestUndoableEdit edit2 = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_TRUE);
        um.addEdit(edit2);
        //if edit is significant then canUndo must be called
        assertFalse(edit2.canRedo());
        assertEquals(um.canRedo(), edit2.canRedo());
        TestUndoableEdit edit3 = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE);
        um.addEdit(edit3);
        // false because there is no significant at indexOfNextAdd or after it
        assertFalse(um.canRedo());
        //move indexOfNextAdd to 1 and call undo for edit2 & edit 3
        um.undo();
        //canRedo must be called for significant edit at indexOfNextAdd or
        // after
        assertEquals(um.canRedo(), edit2.canRedo());
        edit2.flag |= TestUndoableEdit.CAN_REDO_FALSE;
        assertEquals(um.canRedo(), edit2.canRedo());
        edit2.flag = TestUndoableEdit.IS_SIGNIFICANT_FALSE;
        // canRedo must return false because there is no any significant edit
        assertFalse(um.canRedo());
        edit3.flag = TestUndoableEdit.IS_SIGNIFICANT_TRUE;
        assertEquals(um.canRedo(), edit3.canRedo());
        edit3.flag |= TestUndoableEdit.CAN_REDO_FALSE;
        assertEquals(um.canRedo(), edit3.canRedo());
        // set inProgress to false
        um.end();
        assertFalse(um.isInProgress());
        //now um must call to super.canRedo
        // must be true if alive && !hasBeenDone
        assertFalse(um.canRedo());
        um.undo();
        assertTrue(um.canRedo());
        //die sets alive to false
        um.die();
        assertFalse(um.canRedo());
    }

    @Override
    public void testUndo() {
        TestUndoableEdit.counter = 0;
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_TRUE));
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO));
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO
                | TestUndoableEdit.IS_SIGNIFICANT_FALSE));
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO
                | TestUndoableEdit.IS_SIGNIFICANT_FALSE));
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO
                | TestUndoableEdit.IS_SIGNIFICANT_FALSE));
        assertEquals(5, um.indexOfNextAdd);
        um.undo();
        //indexOfNextAdd must be on last significant edit (second edit)
        assertEquals(1, um.indexOfNextAdd);
        um.discardAllEdits();
        TestUndoableEdit.counter = 0;
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO));
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO));
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO));
        assertEquals(3, um.indexOfNextAdd);
        assertEquals(3, um.edits.size());
        //must be called only for last edit
        um.undo();
        assertEquals(2, TestUndoableEdit.counter);
        //index must be on last edit
        assertEquals(2, um.indexOfNextAdd);
        //set inProgress to false
        um.end();
        //and also remove last edit because it beyond indexOfNextAdd
        TestUndoableEdit.counter = 2;
        //undo must be called for remain 2 edits
        um.undo();
        assertEquals(0, TestUndoableEdit.counter);
        um = new UndoManager();
        boolean bWasException = false;
        try {
            um.undo();
        } catch (CannotUndoException e) {
            bWasException = true;
        }
        assertTrue("CannotUndoException was expected", bWasException);
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO
                | TestUndoableEdit.IS_SIGNIFICANT_FALSE));
        bWasException = false;
        try {
            um.undo();
        } catch (CannotUndoException e) {
            bWasException = true;
        }
        assertTrue("CannotUndoException was expected", bWasException);
        um = new UndoManager();
        TestUndoableEdit.counter = 0;
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO));
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO));
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO
                | TestUndoableEdit.IS_SIGNIFICANT_FALSE));
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO));
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO
                | TestUndoableEdit.IS_SIGNIFICANT_FALSE));
        assertEquals(5, um.indexOfNextAdd);
        um.undo();
        assertEquals(3, um.indexOfNextAdd);
        um.undo();
        um.undo();
    }

    @Override
    public void testRedo() {
        TestUndoableEdit.counter = 0;
        TestUndoableEdit edit1 = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE);
        um.addEdit(edit1);
        TestUndoableEdit edit2 = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_TRUE);
        um.addEdit(edit2);
        TestUndoableEdit edit3 = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_TRUE
                | TestUndoableEdit.UNDO);
        um.addEdit(edit3);
        TestUndoableEdit edit4 = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE
                | TestUndoableEdit.UNDO);
        um.addEdit(edit4);
        TestUndoableEdit edit5 = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE
                | TestUndoableEdit.UNDO);
        um.addEdit(edit5);
        //first we call undo
        um.undo();
        //index must be on 2
        // and undo was called for edit3, then for edit4 and last for edit5
        assertEquals(2, um.indexOfNextAdd);
        assertTrue(um.canRedo());
        edit3.id = 0;
        edit3.flag = TestUndoableEdit.REDO | TestUndoableEdit.IS_SIGNIFICANT_FALSE;
        edit2.id = -2;
        edit2.flag = TestUndoableEdit.REDO | TestUndoableEdit.IS_SIGNIFICANT_FALSE;
        edit1.id = -1;
        edit1.flag |= TestUndoableEdit.REDO;
        edit4.id = 1;
        edit4.flag = TestUndoableEdit.REDO | TestUndoableEdit.IS_SIGNIFICANT_FALSE;
        edit5.id = 2;
        edit5.flag = TestUndoableEdit.REDO | TestUndoableEdit.IS_SIGNIFICANT_TRUE;
        TestUndoableEdit.counter = 0;
        assertTrue(um.canRedo());
        //redo must be called first for edit3, then for edit3 & edit2
        assertEquals(2, um.indexOfNextAdd);
        // 1) edit3.isSignificant? false
        // 2) edit4.isSignificant? false
        // 3) edit5.isSignificant? true
        // 4) edit3.redo
        // 5) edit4.redo
        // 6) edit5.redo
        // find significant starting from indexOfNextAdd -> END
        // call redo starting from indexOfNextAdd -> significant
        um.redo();
        um.discardAllEdits();
        boolean bWasException = false;
        try {
            um.redo();
        } catch (CannotRedoException e) {
            bWasException = true;
        }
        assertTrue("CannotRedoException was expected", bWasException);
        //set inProgress to false
        um.end();
        // set hasBeenDone to false
        um.undo();
        // call must lead to super.redo
        // exception must not be thrown
        um.redo();
        bWasException = false;
        try {
            um.redo();
        } catch (CannotRedoException e) {
            bWasException = true;
        }
        assertTrue("CannotRedoException was expected", bWasException);
    }

    @Override
    public void testEnd() {
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.DIE));
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.DIE));
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.DIE));
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.DIE));
        um.undo();
        um.undo();
        um.undo();
        // we moved indexOfNextAdd from 4 to 1
        assertEquals(1, um.indexOfNextAdd);
        um.end();
        // must be called:
        // 1) edit3.die
        // 2) edit2.die
        // 3) edit1.die
        assertFalse(um.isInProgress());
        assertEquals(1, um.edits.size());
    }

    public void testGetLimit() {
        assertEquals(100, um.getLimit());
    }

    public void testDiscardAllEdits() {
        // call for empty manager
        // nothing should happen
        um.discardAllEdits();
        //add several edits with DISCARD flag
        final int editCount = 10;
        TestUndoableEdit[] edits = new TestUndoableEdit[editCount];
        for (int i = 0; i < editCount; i++) {
            edits[i] = new TestUndoableEdit(TestUndoableEdit.DISCARD);
            um.addEdit(edits[i]);
        }
        um.setLimit(200);
        um.discardAllEdits();
        //check that every edit was discard
        for (int i = 0; i < editCount; i++) {
            assertTrue(edits[i].isDieCalled());
        }
        assertEquals(0, um.edits.size());
        assertEquals(0, um.indexOfNextAdd);
        assertEquals(200, um.getLimit());
        um = new UndoManager();
        um.end();
        um.undo();
        assertFalse(um.isInProgress());
        assertFalse(hasBeenDone(um));
        um.discardAllEdits();
        assertFalse(um.isInProgress());
        assertFalse(hasBeenDone(um));
    }

    public void testTrimForLimit() {
        //add a lot of edits with DISCARD flag
        final int editCount = 200;
        TestUndoableEdit[] edits = new TestUndoableEdit[editCount];
        for (int i = 0; i < editCount; i++) {
            edits[i] = new TestUndoableEdit(TestUndoableEdit.DISCARD);
            // it allows to move indexOfNextAdd to limit and stay it there
            if (i < um.getLimit()) {
                um.addEdit(edits[i]);
            } else {
                um.edits.add(edits[i]);
            }
            //so indexOfNextAdd == limit
        }
        assertEquals(100, um.indexOfNextAdd);
        um.trimForLimit();
        assertEquals(um.edits.size(), um.getLimit());
        int limit = um.getLimit();
        // indexOfNextAdd must be a center
        for (int i = 0; i < editCount; i++) {
            if (i < limit / 2 || i > limit * 3 / 2 - 1) {
                assertTrue(edits[i].isDieCalled());
            } else {
                assertFalse(edits[i].isDieCalled());
            }
        }
        assertEquals(limit, um.edits.size());
    }

    public void testUndoOrRedo() {
        //must throw exception if edits is empty
        boolean wasException = false;
        try {
            um.undoOrRedo();
        } catch (CannotUndoException e) {
            wasException = true;
        }
        assertTrue("CannotUndoException was expected", wasException);
        //it makes sense to use undoOrRedo only when limit is equal to 1
        um.setLimit(1);
        TestUndoableEdit.counter = 0;
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.UNDO));
        assertEquals(1, um.indexOfNextAdd);
        //first it should call to undo
        um.undoOrRedo();
        assertEquals(0, TestUndoableEdit.counter);
        assertEquals(0, um.indexOfNextAdd);
        um.edits.set(0, new TestUndoableEdit(TestUndoableEdit.REDO));
        //then it should call to redo
        TestUndoableEdit.counter = 0;
        um.undoOrRedo();
        assertEquals(1, TestUndoableEdit.counter);
        assertEquals(1, um.indexOfNextAdd);
        TestUndoableEdit.counter = 0;
        um.edits.set(0, new TestUndoableEdit(TestUndoableEdit.UNDO));
        um.undoOrRedo();
        assertEquals(0, TestUndoableEdit.counter);
        assertEquals(0, um.indexOfNextAdd);
        um.end();
        // nothing must be call
        um.undoOrRedo();
    }

    public void testCanUndoOrRedo() {
        //must be false if edits is empty
        assertFalse(um.canUndoOrRedo());
        //it makes sense to use canUndoOrRedo only when limit is equal to 1
        um.setLimit(1);
        TestUndoableEdit edit = new TestUndoableEdit(TestUndoableEdit.UNDO);
        um.addEdit(edit);
        assertTrue(um.canUndoOrRedo());
        um.discardAllEdits();
        assertFalse(um.canUndoOrRedo());
        um = new UndoManager();
        um.addEdit(edit);
        assertTrue(um.canUndoOrRedo());
        um.end();
        // it doesn't depend from inProgress
        assertTrue(um.canUndoOrRedo());
    }

    public void testSetLimit() {
        final int editCount = 100;
        TestUndoableEdit[] edits = new TestUndoableEdit[editCount];
        TestUndoableEdit.counter = 0;
        final int newLimit = 50;
        for (int i = 0; i < editCount; i++) {
            if (i < newLimit) {
                edits[i] = new TestUndoableEdit(TestUndoableEdit.DISCARD);
            } else {
                edits[i] = new TestUndoableEdit(TestUndoableEdit.DIE);
            }
            um.edits.add(edits[i]);
        }
        um.setLimit(newLimit);
        assertEquals(newLimit, um.getLimit());
        assertEquals(newLimit, um.edits.size());
        for (int i = 0; i < editCount; i++) {
            if (i < newLimit) {
                assertFalse(edits[i].isDieCalled());
            } else {
                assertTrue(edits[i].isDieCalled());
            }
        }
        //Regression test for H2538
        um.setLimit(-5);
    }

    public void testTrimEdits() {
        //add a lot of edits with DISCARD flag
        final int editCount = 200;
        TestUndoableEdit[] edits = new TestUndoableEdit[editCount];
        for (int i = 0; i < editCount; i++) {
            edits[i] = new TestUndoableEdit(TestUndoableEdit.DISCARD);
            um.edits.add(edits[i]);
        }
        final int from = 25, to = 75;
        um.trimEdits(from, to);
        for (int i = 0; i < editCount; i++) {
            if (i < from || i > to) {
                assertFalse(edits[i].isDieCalled());
            } else {
                assertTrue(edits[i].isDieCalled());
            }
        }
    }

    /*
     * Class under test for java.lang.String getRedoPresentationName()
     */
    @Override
    public void testGetRedoPresentationName() {
        assertEquals(UIManager.getString("AbstractUndoableEdit.redoText"), um
                .getRedoPresentationName());
        TestUndoableEdit edit1 = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE);
        um.addEdit(edit1);
        TestUndoableEdit edit2 = new TestUndoableEdit(TestUndoableEdit.REDO_NAME
                | TestUndoableEdit.IS_SIGNIFICANT_TRUE);
        um.addEdit(edit2);
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE));
        um.undo();
        // returns getRedoPresentationName of significant edit
        assertEquals(edit2.getRedoPresentationName(), um.getRedoPresentationName());
        edit2.flag = TestUndoableEdit.REDO_NAME | TestUndoableEdit.IS_SIGNIFICANT_FALSE;
        assertEquals(UIManager.getString("AbstractUndoableEdit.redoText"), um
                .getRedoPresentationName());
        um.end();
        // not inProgress
        assertEquals(1, um.edits.size());
        assertFalse(um.isInProgress());
        edit1.flag |= TestUndoableEdit.REDO_NAME;
        assertEquals(edit1, um.edits.get(0));
        assertEquals(edit1.getRedoPresentationName(), um.getRedoPresentationName());
    }

    public void testGetUndoOrRedoPresentationName() {
        TestUndoableEdit.counter = 0;
        um.setLimit(1);
        TestUndoableEdit edit = new TestUndoableEdit(TestUndoableEdit.UNDO_NAME);
        um.addEdit(edit);
        //before undo function must call to edit.getUndoPresentationName
        assertEquals(edit.getUndoPresentationName(), um.getUndoOrRedoPresentationName());
        um.undoOrRedo();
        edit.flag = TestUndoableEdit.REDO_NAME;
        //before undo function must call to edit.getUndoPresentationName
        assertEquals(edit.getRedoPresentationName(), um.getUndoOrRedoPresentationName());
        //back to undo name
        um.undoOrRedo();
        edit.flag = TestUndoableEdit.UNDO_NAME;
        assertEquals(edit.getUndoPresentationName(), um.getUndoOrRedoPresentationName());
    }

    /*
     * Class under test for java.lang.String getUndoPresentationName()
     */
    @Override
    public void testGetUndoPresentationName() {
        assertEquals(UIManager.getString("AbstractUndoableEdit.undoText"), um
                .getUndoPresentationName());
        um.addEdit(new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE));
        TestUndoableEdit edit2 = new TestUndoableEdit(TestUndoableEdit.UNDO_NAME
                | TestUndoableEdit.IS_SIGNIFICANT_TRUE);
        um.addEdit(edit2);
        TestUndoableEdit edit3 = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE);
        um.addEdit(edit3);
        // returns getUndoPresentationName of significant edit
        assertEquals(edit2.getUndoPresentationName(), um.getUndoPresentationName());
        edit2.flag = TestUndoableEdit.UNDO_NAME | TestUndoableEdit.IS_SIGNIFICANT_FALSE;
        assertEquals(UIManager.getString("AbstractUndoableEdit.undoText"), um
                .getUndoPresentationName());
        um.end();
        // not inProgress
        assertEquals(3, um.edits.size());
        assertFalse(um.isInProgress());
        // last edit is edit3
        edit3.flag |= TestUndoableEdit.UNDO_NAME;
        assertEquals(edit3, um.lastEdit());
        assertEquals(edit3.getUndoPresentationName(), um.getUndoPresentationName());
    }

    public void testUndoableEditHappened() {
        UndoableEdit edit = new TestUndoableEdit();
        UndoableEditEvent event = new UndoableEditEvent(this, edit);
        um.undoableEditHappened(event);
        assertEquals(1, um.edits.size());
        assertEquals(edit, um.edits.get(0));
    }

    public void testEditToBeRedone() {
        //add a lot of edits with IS_SIGNIFICANT_FALSE flag
        //and a first with IS_SIGNIFICANT_TRUE flag
        final int editCount = 100;
        TestUndoableEdit[] edits = new TestUndoableEdit[editCount];
        edits[0] = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_TRUE);
        um.addEdit(edits[0]);
        for (int i = 1; i < editCount; i++) {
            edits[i] = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE);
            um.addEdit(edits[i]);
        }
        um.undo();
        assertEquals(0, um.indexOfNextAdd);
        assertEquals(edits[0], um.editToBeRedone());
        //when undo was called only edits[0] was undoable
        //so indexOfNextAdd is located in 0 and
        //the result must be equal to edits[0]
        edits[50].flag = TestUndoableEdit.IS_SIGNIFICANT_TRUE;
        assertEquals(edits[0], um.editToBeRedone());
        //it must be null if there are no any significant edit
        edits[0].flag = TestUndoableEdit.IS_SIGNIFICANT_FALSE;
        edits[50].flag = TestUndoableEdit.IS_SIGNIFICANT_FALSE;
        assertNull(um.editToBeRedone());
        um = new UndoManager();
        edits = new TestUndoableEdit[editCount];
        edits[0] = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_TRUE);
        um.addEdit(edits[0]);
        for (int i = 1; i < editCount; i++) {
            edits[i] = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE);
            um.addEdit(edits[i]);
        }
        edits[50].flag = TestUndoableEdit.IS_SIGNIFICANT_TRUE;
        um.undo();
        assertEquals(edits[50], um.editToBeRedone());
        um.undo();
        assertEquals(edits[0], um.editToBeRedone());
        //it must be null if there are no any significant edit
        edits[0].flag = TestUndoableEdit.IS_SIGNIFICANT_FALSE;
        edits[50].flag = TestUndoableEdit.IS_SIGNIFICANT_FALSE;
        assertNull(um.editToBeRedone());
    }

    public void testEditToBeUndone() {
        //add a lot of edits with IS_SIGNIFICANT_FALSE flag
        //and a first with IS_SIGNIFICANT_TRUE flag
        final int editCount = 100;
        TestUndoableEdit[] edits = new TestUndoableEdit[editCount];
        edits[0] = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_TRUE);
        um.addEdit(edits[0]);
        for (int i = 1; i < editCount; i++) {
            edits[i] = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE);
            um.addEdit(edits[i]);
        }
        assertEquals(edits[0], um.editToBeUndone());
        //returned edit should be first in reverse of the order they were added
        edits[50].flag = TestUndoableEdit.IS_SIGNIFICANT_TRUE;
        assertEquals(edits[50], um.editToBeUndone());
        //returned edit must be null if there are no any significant edit
        edits[0].flag = TestUndoableEdit.IS_SIGNIFICANT_FALSE;
        edits[50].flag = TestUndoableEdit.IS_SIGNIFICANT_FALSE;
        assertNull(um.editToBeUndone());
        edits[99].flag = TestUndoableEdit.IS_SIGNIFICANT_TRUE;
        assertEquals(edits[99], um.editToBeUndone());
    }

    public void testRedoTo() {
        final int editCount = 100;
        TestUndoableEdit[] edits = new TestUndoableEdit[editCount];
        TestUndoableEdit.counter = 0;
        final int start = 20, end = 50;
        for (int i = 0; i < editCount; i++) {
            if (i >= start && i < end) {
                edits[i] = new TestUndoableEdit(TestUndoableEdit.UNDO);
            } else {
                edits[i] = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE);
            }
            um.addEdit(edits[i]);
        }
        final int limit = 30;
        assertEquals(editCount, um.indexOfNextAdd);
        um.undoTo(edits[limit]);
        assertEquals(limit, um.indexOfNextAdd);
        //undo must be called only from END -> START
        //and must stop on TO
        //method undo must not be called for TO - START edits
        //indexOfNextAdd must be replaced to edits[TO]
        assertEquals(limit - start, TestUndoableEdit.counter);
        TestUndoableEdit.counter = 0;
        final int countRedo = 40;
        for (int i = limit; i < end; i++) {
            edits[i] = new TestUndoableEdit(TestUndoableEdit.REDO);
            um.edits.set(i, edits[i]);
        }
        TestUndoableEdit.counter = 0;
        um.redoTo(edits[countRedo]);
        //undo must be called only from TO -> END
        //and must stop on TO_REDO
        //method undo must not be called for TO_REDO - TO + 1 edits
        //indexOfNextAdd must be replaced to edits[TO_REDO]
        assertEquals(countRedo - limit + 1, TestUndoableEdit.counter);
        assertEquals(countRedo + 1, um.indexOfNextAdd);
        um = new UndoManager();
        UndoableEdit ed = new AbstractUndoableEdit() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean canRedo() {
                return false;
            }
        };
        um.addEdit(ed);
        // to move indexOfNextAdd from last edit to ed
        um.undoTo(ed);
        boolean bWasException = false;
        try {
            um.redoTo(ed);
        } catch (CannotRedoException e) {
            bWasException = true;
        }
        assertTrue("CannotRedoException was expected", bWasException);
    }

    public void testUndoTo_AIOOB() { // Regression test for HARMONY-2612
        UndoManager um = new UndoManager();
        um.addEdit(new AbstractUndoableEdit());
        try {
            um.undoTo(null);
            fail("CannotUndoException should have been thrown");
        } catch (CannotUndoException e) {
            // Expected
        }
    }

    public void testRedoTo_AIOOB() { // Regression test for HARMONY-2612
        UndoManager um = new UndoManager();
        um.addEdit(new AbstractUndoableEdit());
        try {
            um.redoTo(null);
            fail("CannotRedoException should have been thrown");
        } catch (CannotRedoException e) {
            // Expected
        }
    }

    public void testUndoTo() {
        final int editCount = 100;
        TestUndoableEdit[] edits = new TestUndoableEdit[editCount];
        TestUndoableEdit.counter = 0;
        for (int i = 0; i < editCount; i++) {
            if (i < 50) {
                edits[i] = new TestUndoableEdit(TestUndoableEdit.UNDO);
            } else {
                edits[i] = new TestUndoableEdit(TestUndoableEdit.IS_SIGNIFICANT_FALSE);
            }
            um.addEdit(edits[i]);
        }
        assertEquals(editCount, um.indexOfNextAdd);
        um.undoTo(edits[40]);
        assertEquals(40, TestUndoableEdit.counter);
        assertEquals(40, um.indexOfNextAdd);
        um = new UndoManager();
        UndoableEdit ed = new AbstractUndoableEdit() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean canUndo() {
                return false;
            }
        };
        um.addEdit(ed);
        boolean bWasException = false;
        try {
            um.undoTo(ed);
        } catch (CannotUndoException e) {
            bWasException = true;
        }
        assertTrue("CannotUndoException was expected", bWasException);
    }

    /*
     * Class under test for boolean addEdit(javax.swing.undo.UndoableEdit)
     */
    public void testAddEditUndoableEdit() {
        // if end was called then UndoManager acts as CompoundEdit
        um.end();
        // returns false and doesn't add anything
        assertFalse(um.addEdit(new TestUndoableEdit(TestUndoableEdit.DIE)));
        assertEquals(0, um.edits.size());
        um = new UndoManager();
        TestUndoableEdit.counter = 0;
        TestUndoableEdit edit1 = new TestUndoableEdit();
        assertTrue(um.addEdit(edit1));
        assertEquals(1, um.indexOfNextAdd);
        TestUndoableEdit edit2 = new TestUndoableEdit(TestUndoableEdit.DIE);
        assertTrue(um.addEdit(edit2));
        assertEquals(2, um.indexOfNextAdd);
        TestUndoableEdit edit3 = new TestUndoableEdit(TestUndoableEdit.DIE);
        assertTrue(um.addEdit(edit3));
        assertEquals(3, um.indexOfNextAdd);
        um.undo();
        um.undo();
        // moved indexOfNextAdd from 3 to 1
        assertEquals(1, um.indexOfNextAdd);
        TestUndoableEdit replaceEdit = new TestUndoableEdit();
        assertTrue(um.addEdit(replaceEdit));
        // must be called:
        // 1) edit3.die
        // 2) edit2.die
    }

    @Override
    public void testEditsCapacity() { // Regression for HARMONY-2649
        assertEquals(100, um.edits.capacity());
    }
}
