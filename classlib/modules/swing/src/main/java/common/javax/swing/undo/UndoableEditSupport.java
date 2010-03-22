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

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

public class UndoableEditSupport {

    /**
     * This is a counter. BeginUpdate call increases this variable, endUpdate
     * call decreases this variable.
     */
    protected int          updateLevel;

    protected CompoundEdit compoundEdit;

    protected Vector<UndoableEditListener> listeners;

    /**
     * Source for UndoableEditEvent
     */
    protected Object       realSource;

    /**
     * Calls UndoableEditSupport(null)
     */
    public UndoableEditSupport() {
        this(null);
    }

    /**
     * If source equals null, realSource will be this
     * object(UndoableEditSupport).
     */
    public UndoableEditSupport(final Object source) {
        realSource = (source == null) ? this : source;
        listeners = new Vector<UndoableEditListener>();
        updateLevel = 0;
    }

    /**
     * Creates a clone of listeners vector. Then calls undoableEditHappened on
     * all registered listeners with new UndoableEditEvent(realSource, ue).
     */
    protected void _postEdit(final UndoableEdit ue) {
        if (listeners.isEmpty()) {
            return;
        }
        Vector duplicateVector = (Vector)listeners.clone();
        UndoableEditEvent event = new UndoableEditEvent(realSource, ue);
        for (int i = 0; i < duplicateVector.size(); i++) {
            ((UndoableEditListener)duplicateVector.elementAt(i))
                    .undoableEditHappened(event);
        }
    }

    /**
     * If updateLevel equals 0, sets compoundEdit by createCompoundEdit call.
     * Increments updateLevel. This method updates updateLevel variable, because
     * of this it is thread - safe
     */
    public synchronized void beginUpdate() {
        if (updateLevel == 0) {
            compoundEdit = createCompoundEdit();
        }
        updateLevel++;
    }

    protected CompoundEdit createCompoundEdit() {
        return new CompoundEdit();
    }

    /**
     * This method is a thread-safe method.
     */
    public synchronized void endUpdate() {
        updateLevel--;
        if (updateLevel == 0) {
            _postEdit(compoundEdit);
            compoundEdit.end();
            compoundEdit = null;
        }
    }

    public int getUpdateLevel() {
        return updateLevel;
    }

    /**
     * Thread-safe method. If updateLevel equals 0, then calls _postEdit;
     * otherwise, calls compoundEdit(ue).
     *
     */
    public synchronized void postEdit(final UndoableEdit ue) {
        if (updateLevel == 0) {
            _postEdit(ue);
        } else {
            compoundEdit.addEdit(ue);
        }

    }

    /*
     * The format of the string is based on 1.5 release behavior
     * which can be revealed using the following code:
     *
     *  Object obj = new UndoableEditSupport();
     *  System.out.println(obj.toString());
     */
    public String toString() {
        return super.toString() + " " + "updateLevel: " + updateLevel + " "
               + "listeners: " + listeners + " " + "compoundEdit: "
               + compoundEdit;
    }

    public synchronized void addUndoableEditListener(final UndoableEditListener
                                                     listener) {
        listeners.add(listener);
    }

    public synchronized void removeUndoableEditListener(
                                          final UndoableEditListener
                                           listener) {
        listeners.remove(listener);
    }

    public synchronized UndoableEditListener[] getUndoableEditListeners() {
        return listeners.toArray(new UndoableEditListener[listeners.size()]);
    }

}
