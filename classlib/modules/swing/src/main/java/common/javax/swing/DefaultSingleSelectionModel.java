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
import java.util.EventListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * <p>
 * <i>DefaultSingleSelectionModel</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class DefaultSingleSelectionModel implements Serializable, SingleSelectionModel {
    private static final long serialVersionUID = -8733648221309386264L;

    protected ChangeEvent changeEvent;

    protected EventListenerList listenerList = new EventListenerList();

    private int selectedIndex = -1;

    public void clearSelection() {
        selectedIndex = -1;
        fireStateChanged();
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
        fireStateChanged();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public boolean isSelected() {
        return selectedIndex > -1;
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    public ChangeListener[] getChangeListeners() {
        return getListeners(ChangeListener.class);
    }

    protected void fireStateChanged() {
        ChangeListener[] changeListeners = getChangeListeners();
        if (changeListeners.length > 0) {
            ChangeEvent event = getChangeEvent();
            for (int i = 0; i < changeListeners.length; i++) {
                changeListeners[i].stateChanged(event);
            }
        }
    }

    private ChangeEvent getChangeEvent() {
        if (changeEvent == null) {
            changeEvent = new ChangeEvent(this);
        }
        return changeEvent;
    }
}
