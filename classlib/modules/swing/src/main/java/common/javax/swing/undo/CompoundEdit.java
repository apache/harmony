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

import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.apache.harmony.x.swing.Utilities;

public class CompoundEdit extends AbstractUndoableEdit {

    /**
     * This field is added as performance optimization but not as
     * a guarantee of correct deserialization.
     */
    private static final long serialVersionUID = -6512679249930119683L;

    protected Vector<UndoableEdit> edits = new Vector<UndoableEdit>();

    /**
     * This is flag that indicates edits are still being added to it.
     * The method end() sets inProgress to false.
     */
    boolean inProgress = true;

    @Override
    public boolean addEdit(final UndoableEdit anEdit) {
       if (!inProgress) {
           return false;
       }

       UndoableEdit last = lastEdit();

       if (last != null && (last.addEdit(anEdit) || anEdit.replaceEdit(last))) {
            return true;
       }

       return edits.add(anEdit);
    }

    protected UndoableEdit lastEdit() {
        UndoableEdit last = null;
        try {
            last = edits.lastElement();
        } catch (final NoSuchElementException e) {
        }

        return last;
    }

    /*
     * The format of the string is based on 1.5 release behavior
     * which can be revealed using the following code:
     *
     *  CompoundEdit obj = new CompoundEdit();
     *  obj.addEdit(new CompoundEdit());
     *  System.out.println(obj.toString());
     */
    @Override
    public String toString() {
        String str = super.toString();
        str += " inProgress: " + inProgress;
        str += " edits: [";
        for (ListIterator li = edits.listIterator(); li.hasNext();) {
            str += li.next().toString() + ((li.hasNext()) ? "," : "");
        }
        str += "]";
        return str;
    }

    @Override
    public String getUndoPresentationName() {
        UndoableEdit last = lastEdit();

        if (last != null) {
            String undoName = last.getUndoPresentationName();
            if (undoName.length() != 0) {
                return undoName;
            }
        }

        return super.getUndoPresentationName();
    }

    @Override
    public String getRedoPresentationName() {
        UndoableEdit last = lastEdit();

        if (last != null) {
            String redoName = last.getRedoPresentationName();
            if (redoName.length() != 0) {
                return redoName;
            }
        }

        return super.getRedoPresentationName();
    }

    @Override
    public String getPresentationName() {
        UndoableEdit last = lastEdit();

        if (last != null) {
            String name = last.getPresentationName();
            if (!Utilities.isEmptyString(name)) {
                return name;
            }
        }

        return super.getPresentationName();
    }

    @Override
    public boolean isSignificant() {
        for (ListIterator li = edits.listIterator(); li.hasNext();) {
            if (((UndoableEdit)li.next()).isSignificant()) {
                return true;
            }
        }
        return false;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    @Override
    public boolean canUndo() {
        return !isInProgress() && super.canUndo();
    }

    @Override
    public boolean canRedo() {
        return !isInProgress() && super.canRedo();
    }

    @Override
    public void undo() {
        super.undo();
        for (ListIterator li = edits.listIterator(edits.size());
            li.hasPrevious();) {
            ((UndoableEdit)li.previous()).undo();
        }
    }

    @Override
    public void redo() {
        super.redo();
        for (ListIterator li = edits.listIterator(); li.hasNext();) {
            ((UndoableEdit)li.next()).redo();
        }
    }

    public void end() {
        inProgress = false;
    }

    @Override
    public void die() {
        super.die();
        for (ListIterator li = edits.listIterator(edits.size());
           li.hasPrevious();) {
            ((UndoableEdit)li.previous()).die();
        }
    }

}
