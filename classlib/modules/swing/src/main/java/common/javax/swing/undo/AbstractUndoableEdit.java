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

import java.io.Serializable;
import javax.swing.UIManager;

import org.apache.harmony.x.swing.Utilities;

public class AbstractUndoableEdit implements UndoableEdit, Serializable {

    private static final long serialVersionUID = 580150227676302096L;

    protected static final String UndoName = "Undo";

    protected static final String RedoName = "Redo";

    private boolean alive;

    private boolean hasBeenDone;

    public AbstractUndoableEdit() {
        alive = true;
        hasBeenDone = true;
    }

    public boolean replaceEdit(final UndoableEdit anEdit) {
        return false;
    }

    public boolean addEdit(final UndoableEdit anEdit) {
        return false;
    }

    /*
     * The format of the string is based on 1.5 release behavior
     * which can be revealed using the following code:
     *
     *  Object obj = new AbstractUndoableEdit() {};
     *  System.out.println(obj.toString());
     */
    @Override
    public String toString() {
        return super.toString() + " alive: " + alive
           +  " hasBeenDone: " + hasBeenDone;
    }

    public String getUndoPresentationName() {
        return getOperationPresentationName(getUndoName());
    }

    public String getRedoPresentationName() {
        return getOperationPresentationName(getRedoName());
    }

    public String getPresentationName() {
        return "";
    }

    public boolean isSignificant() {
        return true;
    }

    public boolean canUndo() {
        return alive && hasBeenDone;
    }

    public boolean canRedo() {
        return alive && !hasBeenDone;
    }

    public void undo() {
        if (!canUndo()) {
            throw new CannotUndoException();
        }

        hasBeenDone = false;
    }

    public void redo() {
        if (!canRedo()) {
            throw new CannotRedoException();
        }

        hasBeenDone = true;
    }

    public void die() {
        alive = false;
    }

    final String getUndoName() {
        return UIManager.getString("AbstractUndoableEdit.undoText");
    }

    final String getRedoName() {
        return UIManager.getString("AbstractUndoableEdit.redoText");
    }

    private String getOperationPresentationName(final String operationName) {
        final String presentationName = getPresentationName();
        return Utilities.isEmptyString(presentationName)
               ? operationName
               : operationName + " " + presentationName;
    }
}

