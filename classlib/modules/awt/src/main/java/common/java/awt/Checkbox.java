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

package java.awt;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.EventListener;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;
import org.apache.harmony.awt.ButtonStateController;
import org.apache.harmony.awt.FieldsAccessor;
import org.apache.harmony.awt.state.CheckboxState;

public class Checkbox extends Component implements ItemSelectable, Accessible {
    private static final long serialVersionUID = 7270714317450821763L;

    protected class AccessibleAWTCheckbox extends Component.AccessibleAWTComponent implements
            ItemListener, AccessibleAction, AccessibleValue {
        private static final long serialVersionUID = 7881579233144754107L;

        public AccessibleAWTCheckbox() {
            super();
            // define default constructor explicitly just to make it public
            // add listener to be able to fire property changes:
            addItemListener(this);
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.CHECK_BOX;
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet aStateSet = super.getAccessibleStateSet();
            if (getState()) {
                aStateSet.add(AccessibleState.CHECKED);
            }
            return aStateSet;
        }

        @Override
        public AccessibleAction getAccessibleAction() {
            return this;
        }

        @Override
        public AccessibleValue getAccessibleValue() {
            return this;
        }

        public void itemStateChanged(ItemEvent e) {
            // fire property change event
            final AccessibleState checkedState = AccessibleState.CHECKED;
            AccessibleState oldValue = null;
            AccessibleState newValue = null;
            switch (e.getStateChange()) {
                case ItemEvent.SELECTED:
                    newValue = checkedState;
                    break;
                case ItemEvent.DESELECTED:
                    oldValue = checkedState;
                    break;
            }
            firePropertyChange(AccessibleContext.ACCESSIBLE_STATE_PROPERTY, oldValue, newValue);
        }

        public int getAccessibleActionCount() {
            return 0; // no actions
        }

        public boolean doAccessibleAction(int i) {
            return false;
        }

        public String getAccessibleActionDescription(int i) {
            return null;
        }

        public Number getCurrentAccessibleValue() {
            return null;
        }

        public Number getMaximumAccessibleValue() {
            return null;
        }

        public Number getMinimumAccessibleValue() {
            return null;
        }

        public boolean setCurrentAccessibleValue(Number arg0) {
            return false;
        }
    }

    private final class State extends Component.ComponentState implements CheckboxState {
        private final Dimension textSize = new Dimension();

        State() {
            super();
        }

        public boolean isChecked() {
            return checked;
        }

        public String getText() {
            return label;
        }

        public Dimension getTextSize() {
            return textSize;
        }

        public void setTextSize(Dimension size) {
            textSize.width = size.width;
            textSize.height = size.height;
        }

        public boolean isInGroup() {
            return group != null;
        }

        public boolean isPressed() {
            return stateController.isPressed();
        }

        @Override
        public void calculate() {
            toolkit.theme.calculateCheckbox(state);
        }
    }

    private final AWTListenerList<ItemListener> itemListeners = new AWTListenerList<ItemListener>(
            this);

    private String label;

    private CheckboxGroup group;

    private boolean checked;

    private final transient State state = new State();

    private final transient ButtonStateController stateController;

    public Checkbox() throws HeadlessException {
        this(new String(), null, false);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Checkbox(String label, CheckboxGroup group, boolean state) throws HeadlessException {
        toolkit.lockAWT();
        try {
            this.label = label;
            this.group = group;
            setState(state);
            stateController = createStateController();
            addAWTMouseListener(stateController);
            addAWTKeyListener(stateController);
            addAWTFocusListener(stateController);
        } finally {
            toolkit.unlockAWT();
        }
    }

    private void generateEvent() {
        setState(!getState());
        int stateChange = (checked ? ItemEvent.SELECTED : ItemEvent.DESELECTED);
        postEvent(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this, stateChange));
    }

    public Checkbox(String label) throws HeadlessException {
        this(label, null, false);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Checkbox(String label, boolean state) throws HeadlessException {
        this(label, null, state);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Checkbox(String label, boolean state, CheckboxGroup group) throws HeadlessException {
        this(label, group, state);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean getState() {
        toolkit.lockAWT();
        try {
            return checked;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void addNotify() {
        toolkit.lockAWT();
        try {
            super.addNotify();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        toolkit.lockAWT();
        try {
            return super.getAccessibleContext();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    protected String paramString() {
        /* The format is based on 1.5 release behavior 
         * which can be revealed by the following code:
         * System.out.println(new Checkbox());
         */
        toolkit.lockAWT();
        try {
            return super.paramString() + ",label=" + label + ",state=" + checked; //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            toolkit.unlockAWT();
        }
    }

    public CheckboxGroup getCheckboxGroup() {
        toolkit.lockAWT();
        try {
            return group;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public String getLabel() {
        toolkit.lockAWT();
        try {
            return label;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Object[] getSelectedObjects() {
        toolkit.lockAWT();
        try {
            if (checked) {
                return new Object[] { label };
            }
            return null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setCheckboxGroup(CheckboxGroup group) {
        toolkit.lockAWT();
        try {
            CheckboxGroup oldGroup = this.group;
            this.group = group;
            if (checked) {
                if ((oldGroup != null) && (oldGroup.getSelectedCheckbox() == this)) {
                    oldGroup.setSelectedCheckbox(null);
                }
                if (group != null) {
                    Checkbox selected = group.getSelectedCheckbox();
                    if (selected != null) {
                        checked = false;
                    } else {
                        group.setSelectedCheckbox(this);
                    }
                }
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setLabel(String label) {
        toolkit.lockAWT();
        try {
            this.label = label;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setState(boolean state) {
        toolkit.lockAWT();
        try {
            setChecked(state);
            updateGroup();
        } finally {
            toolkit.unlockAWT();
        }
    }

    private void updateGroup() {
        if (group != null) {
            boolean wasSelected = (group.getSelectedCheckbox() == this);
            if (checked) {
                group.setSelectedCheckbox(this);
            } else if (wasSelected) {
                setChecked(true);
            }
        }
    }

    void setChecked(boolean checked) {
        if (checked != this.checked) { // avoid dead loop in repaint()
            this.checked = checked;
            repaint();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        toolkit.lockAWT();
        try {
            if (ItemListener.class.isAssignableFrom(listenerType)) {
                return (T[]) getItemListeners();
            }
            return super.getListeners(listenerType);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void addItemListener(ItemListener l) {
        toolkit.lockAWT();
        try {
            itemListeners.addUserListener(l);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void removeItemListener(ItemListener l) {
        toolkit.lockAWT();
        try {
            itemListeners.removeUserListener(l);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public ItemListener[] getItemListeners() {
        toolkit.lockAWT();
        try {
            return itemListeners.getUserListeners(new ItemListener[0]);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    protected void processEvent(AWTEvent e) {
        toolkit.lockAWT();
        try {
            if (toolkit.eventTypeLookup.getEventMask(e) == AWTEvent.ITEM_EVENT_MASK) {
                processItemEvent((ItemEvent) e);
            } else {
                super.processEvent(e);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    protected void processItemEvent(ItemEvent e) {
        toolkit.lockAWT();
        try {
            for (ItemListener listener : itemListeners.getUserListeners()) {
                switch (e.getID()) {
                    case ItemEvent.ITEM_STATE_CHANGED:
                        listener.itemStateChanged(e);
                        break;
                }
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    boolean isPrepainter() {
        return true;
    }

    @Override
    void prepaint(Graphics g) {
        toolkit.theme.drawCheckbox(g, state);
    }

    @Override
    void setEnabledImpl(boolean value) {
        if (value != isEnabled()) { // avoid dead loop in repaint()
            super.setEnabledImpl(value);
            repaint();
        }    
    }

    @Override
    String autoName() {
        return ("checkbox" + toolkit.autoNumber.nextCheckBox++); //$NON-NLS-1$
    }

    @Override
    Dimension getDefaultMinimumSize() {
        if (getFont() == null) {
            return new Dimension(0, 0);
        }
        return state.getDefaultMinimumSize();
    }

    @Override
    void resetDefaultSize() {
        state.reset();
    }

    @Override
    ComponentBehavior createBehavior() {
        return new HWBehavior(this);
    }

    ButtonStateController createStateController() {
        return new ButtonStateController(this) {
            @Override
            protected void fireEvent() {
                generateEvent();
            }
        };
    }

    private void readObject(ObjectInputStream stream) throws IOException,
            ClassNotFoundException {
        stream.defaultReadObject();
        FieldsAccessor accessor = new FieldsAccessor(Button.class, this);
        accessor.set("stateController", createStateController()); //$NON-NLS-1$
        accessor.set("state", new State()); //$NON-NLS-1$
    }

    @Override
    AccessibleContext createAccessibleContext() {
        return new AccessibleAWTCheckbox();
    }
}
