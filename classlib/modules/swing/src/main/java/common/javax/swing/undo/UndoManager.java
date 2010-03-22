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

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

public class UndoManager extends CompoundEdit implements UndoableEditListener {
    /**
     * <b>Note:</b> The <code>serialVersionUID</code> fields are explicitly
     * declared as a performance optimization, not as a guarantee of
     * serialization compatibility.
     */
    private static final long serialVersionUID = -8731438423915672404L;

    /**
     * Index points to edit from the array of edits.
     * All new edits will be added to this place.
     * All redo, undo will look at the edit with this index.
     */
    int indexOfNextAdd;

    static final int DEFAULT_LIMIT = 100;

    /**
     * The maximum number of edits that manager will contain.
     * Default value is 100.
     */
    int limit;

    public UndoManager() {
        indexOfNextAdd = 0;
        limit          = DEFAULT_LIMIT;
        edits.ensureCapacity(DEFAULT_LIMIT);
    }

    @Override
    public synchronized boolean addEdit(final UndoableEdit anEdit) {
        if (inProgress) {
            // we need to remove edits only when indexOfNextAdd < size - 1
            if (indexOfNextAdd < edits.size()) {
                // remove all edit starting from indexOfNextAdd -> END
                // and send die in reverse order
                trimEdits(indexOfNextAdd, edits.size() - 1);
            }
            // insert new edit at indexOfNextAdd and increase index by one
            edits.insertElementAt(anEdit, indexOfNextAdd++);
            return true;
        }

        // acts as CompoundEdit
        return false;
    }

    protected void undoTo(final UndoableEdit edit) {
        int index = edits.indexOf(edit);

        if (index < 0) { // Fix for HARMONY-2612
            throw new CannotUndoException();
        }

        for (int i = indexOfNextAdd - 1; i >= index; i--) {
            UndoableEdit e = edits.get(i);
            e.undo();
        }
        indexOfNextAdd = index;
    }

    protected void redoTo(final UndoableEdit edit) {
        int index = edits.indexOf(edit);

        if (index < 0) { // Fix for HARMONY-2612
            throw new CannotRedoException();
        }

        for (int i = indexOfNextAdd; i <= index; i++) {
            UndoableEdit e = edits.get(i);
            e.redo();
        }
        indexOfNextAdd = index + 1;
    }

    protected UndoableEdit editToBeUndone() {
        // find first significant edit starting from indexOfNextAdd - 1 -> start
        for (int i = indexOfNextAdd - 1; i >= 0; i--) {
            UndoableEdit edit = edits.get(i);
            if (edit.isSignificant()) {
                return edit;
            }
        }
        return null;
    }

    protected UndoableEdit editToBeRedone() {
        // find first significant edit starting from indexOfNextAdd -> end
        for (int i = indexOfNextAdd; i < edits.size(); i++) {
            UndoableEdit edit = edits.get(i);
            if (edit.isSignificant()) {
                return edit;
            }
        }
        return null;
    }

    public void undoableEditHappened(final UndoableEditEvent e) {
        addEdit(e.getEdit());
    }

    /*
     * The format of the string is based on 1.5 release behavior
     * which can be revealed using the following code:
     *
     *  Object obj = new UndoManager();
     *  System.out.println(obj.toString());
     */
    @Override
    public String toString() {
        return super.toString()
            + " limit: " + limit
            + " indexOfNextAdd: " + indexOfNextAdd;
    }

    @Override
    public synchronized String getUndoPresentationName() {
        if (inProgress) {
            UndoableEdit undoEdit = editToBeUndone();
            if (undoEdit == null) {
                return getUndoName();
            }
            return undoEdit.getUndoPresentationName();
        } else {
           return super.getUndoPresentationName();
        }
    }

    public synchronized String getUndoOrRedoPresentationName() {
        if (indexOfNextAdd == 1) {
            return getUndoPresentationName();
        } else {
            return getRedoPresentationName();
        }
    }

    @Override
    public synchronized String getRedoPresentationName() {
        if (inProgress) {
            UndoableEdit redoEdit = editToBeRedone();
            if (redoEdit == null) {
                return getRedoName();
            }
            return redoEdit.getRedoPresentationName();
        } else {
            return super.getRedoPresentationName();
        }
    }

    protected void trimEdits(final int from, final int to) {
        // we will use this method to remove edits
        // and to call die in reverse order they were added

        // kill all edits in the given range (inclusive)
        // and remove all edits in the given range (inclusive) from edits
        // we must go from end to start (to -> from)
        for (int i = to; i >= from; i--) {
            edits.get(i).die();
            edits.remove(i);
        }
    }

    public synchronized void setLimit(final int l) {
        limit = l;
        trimForLimit();
    }

    public synchronized boolean canUndoOrRedo() {
        // checks that undo can be called if indexOfNextAdd == 1
        if (indexOfNextAdd == 1) {
            return canUndo();
        } else {
            return canRedo();
        }
    }

    @Override
    public synchronized boolean canUndo() {
        if (inProgress) {
            // find significant edit and call canUndo
            UndoableEdit edit = editToBeUndone();
            return (edit == null) ? false : edit.canUndo();
        }
        // if not in progress then call super.canUndo
        return super.canUndo();
    }

     @Override
    public synchronized boolean canRedo() {
        if (inProgress) {
            // find significant edit and call canRedo
            UndoableEdit edit = editToBeRedone();
            return (edit == null) ? false : edit.canRedo();
        }
        // if not in progress then call super.canRedo
        return super.canRedo();
    }

    public synchronized void undoOrRedo() {
        // do nothing if we are not in progress
        if (inProgress) {
            // if there is no any edit, throw exception
            if (edits.isEmpty()) {
                throw new CannotUndoException();
            }
            // flag is indexOfNextAdd
            // before undo and after redo it's equal to 1
            if (indexOfNextAdd == 1) {
                undo();
            } else {
                redo();
            }
        }
    }

    @Override
    public synchronized void undo() {
        if (inProgress) {
            // undo first significant edit before indexOfNextAdd
            UndoableEdit significantEdit = editToBeUndone();
            if (significantEdit == null) {
                // throw exception if there is no one significant edit
                throw new CannotUndoException();
            }
            undoTo(significantEdit);
        } else {
            // acts as a CompoundEdit
            super.undo();
        }
    }

    protected void trimForLimit() {
        // check that we need to trim
        if ((limit > 0) && (getLimit() < edits.size())) {
            // indexOfNextAdd is a center for trimming
            int beginning = indexOfNextAdd - limit / 2;
            if (beginning < 0) {
                beginning = 0;
            }
            // trim from 0 to beginning
            trimEdits(0, beginning - 1);
            // our array was shifted to left
            // so we need to trim edit starting from limit to end
            trimEdits(limit, edits.size() - 1);
        }
    }

    @Override
    public synchronized void redo() {
        if (inProgress) {
            // redoes last significant edit at index or later
            UndoableEdit significantEdit = editToBeRedone();
            if (significantEdit == null) {
                // throw exception if there is no significant edit
                throw new CannotRedoException();
            }
            redoTo(significantEdit);
        } else {
            // acts as CompoundEdit
            super.redo();
        }
    }

    @Override
    public synchronized void end() {
        // calls super's end
        super.end();
        // and trim from indexOfNextAdd to End
        trimEdits(indexOfNextAdd, edits.size() - 1);
    }

    public synchronized void discardAllEdits() {
        indexOfNextAdd = 0;
        for (int i = 0; i < edits.size(); i++) {
            edits.get(i).die();
        }
        edits.removeAllElements();
        return;
    }

    public synchronized int getLimit() {
        return limit;
    }

}
