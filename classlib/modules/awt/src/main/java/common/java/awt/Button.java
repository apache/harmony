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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.EventListener;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;
import org.apache.harmony.awt.ButtonStateController;
import org.apache.harmony.awt.FieldsAccessor;
import org.apache.harmony.awt.state.ButtonState;

public class Button extends Component implements Accessible {
    private static final long serialVersionUID = -8774683716313001058L;

    protected class AccessibleAWTButton extends AccessibleAWTComponent implements
            AccessibleAction, AccessibleValue {
        private static final long serialVersionUID = -5932203980244017102L;

        protected AccessibleAWTButton() {
            super();
        }

        public int getAccessibleActionCount() {
            return 1;
        }

        public String getAccessibleActionDescription(int i) {
            if (i == 0) {
                return "click"; //$NON-NLS-1$
            }
            return null;
        }

        public boolean doAccessibleAction(int i) {
            if (i != 0) {
                return false;
            }
            generateEvent(0l, 0);
            return true;
        }

        public Number getCurrentAccessibleValue() {
            return Integer.valueOf(0);
        }

        public boolean setCurrentAccessibleValue(Number n) {
            return false;
        }

        public Number getMinimumAccessibleValue() {
            return Integer.valueOf(0);
        }

        public Number getMaximumAccessibleValue() {
            return Integer.valueOf(0);
        }

        @Override
        public AccessibleAction getAccessibleAction() {
            return this;
        }

        @Override
        public String getAccessibleName() {
            return getLabel();
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PUSH_BUTTON;
        }

        @Override
        public AccessibleValue getAccessibleValue() {
            return this;
        }
    }

    private class State extends Component.ComponentState implements ButtonState {
        State() {
            super();
        }

        public String getText() {
            return label;
        }

        public Dimension getTextSize() {
            return labelSize;
        }

        public void setTextSize(Dimension size) {
            labelSize.width = size.width;
            labelSize.height = size.height;
        }

        public boolean isPressed() {
            return stateController.isPressed();
        }

        @Override
        public void calculate() {
            toolkit.theme.calculateButton(state);
        }
    }

    private String actionCommand;

    private String label;

    private final Dimension labelSize;

    private final transient ButtonStateController stateController;

    private final AWTListenerList<ActionListener> actionListeners;

    private final transient State state = new State();

    public Button(String label) throws HeadlessException {
        toolkit.lockAWT();
        try {
            this.label = label;
            labelSize = new Dimension();
            actionCommand = null;
            actionListeners = new AWTListenerList<ActionListener>(this);
            stateController = createStateController();
            addAWTMouseListener(stateController);
            addAWTKeyListener(stateController);
            addAWTFocusListener(stateController);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Button() throws HeadlessException {
        this(""); //$NON-NLS-1$
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setLabel(String label) {
        toolkit.lockAWT();
        try {
            if (!(this.label == label ||
                    (label != null && label.equals(this.label)))) {
                // set new label only if it differs from the old one
                // to avoid dead loop in repaint()
                this.label = label;
                if (isDisplayable()) {
                    invalidate();
                    if (isShowing()) {
                        repaint();
                    }
                }
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    void setFontImpl(Font f) {
        super.setFontImpl(f);
        if (isDisplayable()) {
            invalidate();
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

    @Override
    public void addNotify() {
        toolkit.lockAWT();
        try {
            super.addNotify();
            invalidate();
            state.calculate();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setActionCommand(String command) {
        toolkit.lockAWT();
        try {
            actionCommand = command;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public String getActionCommand() {
        toolkit.lockAWT();
        try {
            return (actionCommand == null ? label : actionCommand);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (ActionListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getActionListeners();
        }
        return super.getListeners(listenerType);
    }

    public void addActionListener(ActionListener l) {
        actionListeners.addUserListener(l);
    }

    public void removeActionListener(ActionListener l) {
        actionListeners.removeUserListener(l);
    }

    public ActionListener[] getActionListeners() {
        return actionListeners.getUserListeners(new ActionListener[0]);
    }

    @Override
    protected void processEvent(AWTEvent e) {
        if (toolkit.eventTypeLookup.getEventMask(e) == AWTEvent.ACTION_EVENT_MASK) {
            processActionEvent((ActionEvent) e);
        } else {
            super.processEvent(e);
        }
    }

    protected void processActionEvent(ActionEvent e) {
        for (ActionListener listener : actionListeners.getUserListeners()) {
            switch (e.getID()) {
                case ActionEvent.ACTION_PERFORMED:
                    listener.actionPerformed(e);
                    break;
            }
        }
    }

    @Override
    protected String paramString() {
        toolkit.lockAWT();
        try {
            return (super.paramString() + ",label=" + label + "," + (stateController //$NON-NLS-1$ //$NON-NLS-2$
                    .isPressed() ? "pressed" : "unpressed")); //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            toolkit.unlockAWT();
        }
    }

    Dimension getLabelSize() {
        return labelSize;
    }

    @Override
    void prepaint(Graphics g) {
        toolkit.theme.drawButton(g, state);
    }

    @Override
    boolean isPrepainter() {
        return true;
    }

    boolean isPressed() {
        return stateController.isPressed();
    }

    @Override
    void setEnabledImpl(boolean value) {
        if (isEnabled() != value) { // to avoid dead loop in repaint()
            super.setEnabledImpl(value);
            repaint();
        }
    }

    void generateEvent(long timestamp, int modifiers) {
        postEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getActionCommand(),
                timestamp, modifiers));
    }

    @Override
    String autoName() {
        return ("button" + Integer.toString(toolkit.autoNumber.nextButton++)); //$NON-NLS-1$
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
    Dimension getDefaultMinimumSize() {
        if (getFont() == null) {
            return new Dimension(0, 0);
        }
        if (state.getDefaultMinimumSize() == null) {
            toolkit.theme.calculateButton(state);
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
                generateEvent(getWhen(), getMod());
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
        return new AccessibleAWTButton();
    }
}
