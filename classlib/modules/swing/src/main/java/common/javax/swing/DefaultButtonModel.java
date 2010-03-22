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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serializable;
import java.util.EventListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * <p>
 * <i>DefaultButtonModel</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class DefaultButtonModel implements ButtonModel, Serializable {
    private static final long serialVersionUID = -8004185980087291435L;

    public static final int ARMED = 1;

    public static final int SELECTED = 2;

    public static final int PRESSED = 4;

    public static final int ENABLED = 8;

    public static final int ROLLOVER = 16;

    protected int stateMask = ENABLED;

    protected String actionCommand;

    protected ButtonGroup group;

    protected int mnemonic;

    protected transient ChangeEvent changeEvent;

    protected EventListenerList listenerList = new EventListenerList();

    public <T extends EventListener> T[] getListeners(Class<T> listenersClass) {
        return listenerList.getListeners(listenersClass);
    }

    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    public void addItemListener(ItemListener listener) {
        listenerList.add(ItemListener.class, listener);
    }

    public void removeItemListener(ItemListener listener) {
        listenerList.remove(ItemListener.class, listener);
    }

    public ItemListener[] getItemListeners() {
        return listenerList.getListeners(ItemListener.class);
    }

    public void addActionListener(ActionListener listener) {
        listenerList.add(ActionListener.class, listener);
    }

    public void removeActionListener(ActionListener listener) {
        listenerList.remove(ActionListener.class, listener);
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    public void setGroup(ButtonGroup group) {
        this.group = group;
    }

    public ButtonGroup getGroup() {
        return group;
    }

    public void setActionCommand(String command) {
        actionCommand = command;
    }

    public String getActionCommand() {
        return actionCommand;
    }

    public Object[] getSelectedObjects() {
        return null;
    }

    public void setSelected(boolean selected) {
        if (isSelected() != selected) {
            toggleState(SELECTED);
            int state = selected ? ItemEvent.SELECTED : ItemEvent.DESELECTED;
            ItemEvent event = new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this, state);
            fireItemStateChanged(event);
        }
    }

    public boolean isSelected() {
        return isStateSet(SELECTED);
    }

    public void setRollover(boolean rollover) {
        if (isEnabled() && isRollover() != rollover) {
            toggleState(ROLLOVER);
        }
    }

    public boolean isRollover() {
        return isStateSet(ROLLOVER);
    }

    public void setPressed(boolean pressed) {
        if (isEnabled() && isPressed() != pressed) {
            toggleState(PRESSED);
            if (!pressed && isArmed()) {
                fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                        actionCommand, System.currentTimeMillis(), 0));
            }
        }
    }

    public boolean isPressed() {
        return isStateSet(PRESSED);
    }

    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            stateMask = isSelected() ? SELECTED : 0;
            if (enabled) {
                stateMask |= ENABLED;
            }
            fireStateChanged();
        }
    }

    public boolean isEnabled() {
        return isStateSet(ENABLED);
    }

    public void setArmed(boolean armed) {
        if (isEnabled() && isArmed() != armed) {
            toggleState(ARMED);
        }
    }

    public boolean isArmed() {
        return isStateSet(ARMED);
    }

    public void setMnemonic(int mnemonic) {
        if (this.mnemonic != mnemonic) {
            this.mnemonic = mnemonic;
            fireStateChanged();
        }
    }

    public int getMnemonic() {
        return mnemonic;
    }

    protected void fireStateChanged() {
        ChangeListener[] listeners = getChangeListeners();
        if (listeners.length == 0) {
            return;
        }
        if (changeEvent == null) {
            changeEvent = new ChangeEvent(this);
        }
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].stateChanged(changeEvent);
        }
    }

    protected void fireItemStateChanged(ItemEvent event) {
        ItemListener[] listeners = getItemListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].itemStateChanged(event);
        }
    }

    protected void fireActionPerformed(ActionEvent event) {
        ActionListener[] listeners = getActionListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].actionPerformed(event);
        }
    }
    
    void toggleState(int stateFlag) {
        // visibility is changed from private to default because according to
        // HARMONY-4658 patch the method is needed by ToggleButtonModel
        stateMask ^= stateFlag;
        fireStateChanged();
    }

    private boolean isStateSet(int stateFlag) {
        return (stateMask & stateFlag) != 0;
    }
}
