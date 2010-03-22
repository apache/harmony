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

package javax.swing;

import java.io.Serializable;
import java.util.EventObject;

import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;

public abstract class AbstractCellEditor implements CellEditor, Serializable {
    protected EventListenerList listenerList = new EventListenerList();
    protected transient ChangeEvent changeEvent;

    public boolean isCellEditable(final EventObject e) {
        return true;
    }

    public boolean shouldSelectCell(final EventObject event) {
        return true;
    }

    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }

    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    public void addCellEditorListener(final CellEditorListener l) {
        listenerList.add(CellEditorListener.class, l);
    }

    public void removeCellEditorListener(final CellEditorListener l) {
        listenerList.remove(CellEditorListener.class, l);
    }

    public CellEditorListener[] getCellEditorListeners() {
        return listenerList.getListeners(CellEditorListener.class);
    }


    protected void fireEditingStopped() {
        CellEditorListener[] listeners = getCellEditorListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].editingStopped(getChangeEvent());
        }
    }

    protected void fireEditingCanceled() {
        CellEditorListener[] listeners = getCellEditorListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].editingCanceled(getChangeEvent());
        }
    }


    private ChangeEvent getChangeEvent() {
        if (changeEvent == null) {
            changeEvent = new ChangeEvent(this);
        }

        return changeEvent;
    }
}
