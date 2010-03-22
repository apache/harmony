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
 * @author Alexey A. Ivanov, Roman I. Chernyatchik
 */
package javax.swing.text;

import java.util.Vector;

import javax.swing.undo.AbstractUndoableEdit;

abstract class AbstractContentUndoableEdit extends AbstractUndoableEdit {

    /*
     * TODO check if it is to be taken from UIDefaults
     */
    private static final String addition = "addition";

    /*
     * TODO check if it is to be taken from UIDefaults
     */
    private static final String deletion = "deletion";

    protected final boolean    inserted;
    protected final int        len;
    protected final int        pos;
    protected String     text;

    // Information to undo position changes.
    private Vector     undoPos;

    public AbstractContentUndoableEdit(final int where, final String chars,
            final boolean isInsertCommand)
    throws BadLocationException {

        pos      = where;
        text     = chars;
        len      = text.length();
        inserted = isInsertCommand;
        if (!inserted) {
            undoPos = getPositionsInRange(null, pos, len);
        } else {
            undoPos = new Vector();
        }
    }

    public void die() {
        super.die();

        text = null;
        undoPos = null;
    }

    public String getPresentationName() {
        return inserted ? addition : deletion;
    }

    public void redo() {
        super.redo();

        if (inserted) {
            insertText();
        } else {
            removeText();
        }
    }

    public void undo() {
        super.undo();

        if (inserted) {
            removeText();
        } else {
            insertText();
        }
    }

    protected abstract Vector getPositionsInRange(final Vector positions,
                                                  final int where,
                                                  final int length);

    protected abstract void insertItems(final int where, final String chars);

    protected abstract void removeItems(final int where, final int length);

    protected abstract void updateUndoPositions(final Vector undoPositions);

    private void insertText() {
        insertItems(pos, text);
        updateUndoPositions(undoPos);
        undoPos.clear();
    }

    private void removeText() {
        getPositionsInRange(undoPos, pos, len);
        removeItems(pos, len);
    }
}
