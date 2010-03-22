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

import java.util.Enumeration;
import java.util.Hashtable;

public class StateEdit extends AbstractUndoableEdit {

    protected static final String RCSID = "$Id: StateEdit.java,v 1.6 1997/10"
        +  "/01 20:05:51 sandipc Exp $";

    protected StateEditable object;

    protected Hashtable<Object, Object> preState;

    protected Hashtable<Object, Object> postState;

    protected String undoRedoName;

    public StateEdit(final StateEditable anObject, final String name) {
        super();
        init(anObject, name);
    }

    public StateEdit(final StateEditable anObject) {
        this(anObject, null);
    }

    public void end() {
        postState = initHashtable(postState);
        object.storeState(postState);
        removeRedundantState();
    }

    public String getPresentationName() {
        return undoRedoName;
    }

    private Hashtable<Object, Object>
                    initHashtable(final Hashtable<Object, Object> ht) {
        if (ht == null) {
            return new Hashtable<Object, Object>();
        }
        ht.clear();
        return ht;
    }

    protected void init(final StateEditable anObject, final String name) {
        object = anObject;
        undoRedoName = name;
        preState = initHashtable(preState);
        object.storeState(preState);
    }

    public void redo() {
        super.redo();
        object.restoreState(postState);
    }

    protected void removeRedundantState() {
        if (preState == null || postState == null) {
            return;
        }
        for (Enumeration keys = preState.keys(); keys.hasMoreElements();) {
            Object key = keys.nextElement();
            Object preValue = preState.get(key);
            if (!postState.containsKey(key)) {
                continue;
            }
            Object postValue = postState.get(key);
            if ((preValue == null && postValue == null)
                    || (preValue.equals(postValue))) {
                preState.remove(key);
                postState.remove(key);
            }
        }
    }

    public void undo() {
        super.undo();
        object.restoreState(preState);
    }
}
