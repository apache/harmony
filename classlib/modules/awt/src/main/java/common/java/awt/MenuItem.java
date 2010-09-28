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
import java.lang.reflect.Array;
import java.util.EventListener;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;
import org.apache.harmony.awt.state.MenuItemState;

public class MenuItem extends MenuComponent implements Accessible {
    private static final long serialVersionUID = -21757335363267194L;

    private String label;

    private MenuShortcut shortcut;

    private long enabledEvents;

    private boolean enabled;

    private String actionCommand;

    private final AWTListenerList<ActionListener> actionListeners = new AWTListenerList<ActionListener>();

    protected class AccessibleAWTMenuItem extends AccessibleAWTMenuComponent implements
            AccessibleAction, AccessibleValue {
        private static final long serialVersionUID = -217847831945965825L;

        @Override
        public String getAccessibleName() {
            String aName = super.getAccessibleName();
            if (aName == null) {
                aName = getLabel();
            }
            return aName;
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU_ITEM;
        }

        @Override
        public AccessibleAction getAccessibleAction() {
            return this;
        }

        @Override
        public AccessibleValue getAccessibleValue() {
            return this;
        }

        public int getAccessibleActionCount() {
            return 1;
        }

        public boolean doAccessibleAction(int i) {
            if (i != 0) {
                return false;
            }
            itemSelected(System.currentTimeMillis(), 0);
            return true;
        }

        public String getAccessibleActionDescription(int i) {
            if (i != 0) {
                return null;
            }
            return "click"; //$NON-NLS-1$
        }

        public Number getCurrentAccessibleValue() {
            return new Integer(0);
        }

        public Number getMaximumAccessibleValue() {
            return new Integer(0);
        }

        public Number getMinimumAccessibleValue() {
            return new Integer(0);
        }

        public boolean setCurrentAccessibleValue(Number n) {
            return false;
        }
    }

    /**
     * The internal menu item's state utilized by the visual theme
     */
    class State implements MenuItemState {
        private Rectangle textBounds;

        private Rectangle shortcutBounds;

        private Rectangle itemBounds;

        public String getText() {
            return MenuItem.this.getLabel();
        }

        public Rectangle getTextBounds() {
            return textBounds;
        }

        public void setTextBounds(int x, int y, int w, int h) {
            textBounds = new Rectangle(x, y, w, h);
        }

        public boolean isMenu() {
            return MenuItem.this instanceof Menu;
        }

        public boolean isChecked() {
            return (MenuItem.this instanceof CheckboxMenuItem)
                    && ((CheckboxMenuItem) MenuItem.this).getState();
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getShortcut() {
            return (shortcut != null ? shortcut.toString() : ""); //$NON-NLS-1$
        }

        public Rectangle getShortcutBounds() {
            return shortcutBounds;
        }

        public void setShortcutBounds(int x, int y, int w, int h) {
            shortcutBounds = new Rectangle(x, y, w, h);
        }

        public boolean isCheckBox() {
            return (MenuItem.this instanceof CheckboxMenuItem);
        }

        public boolean isSeparator() {
            String label = MenuItem.this.getLabel();
            return label != null && label.equals("-"); //$NON-NLS-1$
        }

        public Dimension getMenuSize() {
            if (MenuItem.this instanceof Menu) {
                return ((Menu) MenuItem.this).getSize();
            }
            return null;
        }

        public Rectangle getItemBounds() {
            return itemBounds;
        }

        public void setItemBounds(int x, int y, int w, int h) {
            itemBounds = new Rectangle(x, y, w, h);
        }

        void reset() {
            textBounds = null;
            shortcutBounds = null;
            itemBounds = null;
        }
    }

    final State itemState = new State();

    public MenuItem() throws HeadlessException {
        this("", null); //$NON-NLS-1$
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public MenuItem(String label) throws HeadlessException {
        this(label, null);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public MenuItem(String label, MenuShortcut shortcut) throws HeadlessException {
        toolkit.lockAWT();
        try {
            this.label = label;
            this.shortcut = shortcut;
            enabled = true;
            enabledEvents = 0;
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void disable() {
        toolkit.lockAWT();
        try {
            setEnabled(false);
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void enable(boolean b) {
        toolkit.lockAWT();
        try {
            setEnabled(b);
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void enable() {
        toolkit.lockAWT();
        try {
            setEnabled(true);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        toolkit.lockAWT();
        try {
            if (ActionListener.class.isAssignableFrom(listenerType)) {
                return (T[]) getActionListeners();
            }
            return (T[]) Array.newInstance(listenerType, 0);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public String paramString() {
        /*
         * The format of paramString is based on 1.5 release behavior which can
         * be revealed using the following code:
         * 
         * MenuItem obj = new MenuItem("Label", new
         * MenuShortcut(KeyEvent.VK_A)); obj.setActionCommand("Action");
         * obj.disable(); System.out.println(obj.toString());
         */
        toolkit.lockAWT();
        try {
            String result = super.paramString() + ",label=" + label; //$NON-NLS-1$
            if (!enabled) {
                result += ",disabled"; //$NON-NLS-1$
            }
            if (actionCommand != null) {
                result += ",command=" + actionCommand; //$NON-NLS-1$
            }
            if (shortcut != null) {
                result += ",shortcut=" + shortcut.toString(); //$NON-NLS-1$
            }
            return result;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public String getActionCommand() {
        toolkit.lockAWT();
        try {
            return actionCommand != null ? actionCommand : label;
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

    public void addNotify() {
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    protected final void disableEvents(long eventsToDisable) {
        toolkit.lockAWT();
        try {
            enabledEvents &= ~eventsToDisable;
            deprecatedEventHandler = false;
        } finally {
            toolkit.unlockAWT();
        }
    }

    protected final void enableEvents(long eventsToEnable) {
        toolkit.lockAWT();
        try {
            enabledEvents |= eventsToEnable;
            deprecatedEventHandler = false;
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

    public boolean isEnabled() {
        toolkit.lockAWT();
        try {
            return enabled;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    protected void processEvent(AWTEvent event) {
        if (toolkit.eventTypeLookup.getEventMask(event) == AWTEvent.ACTION_EVENT_MASK) {
            processActionEvent((ActionEvent) event);
        } else {
            super.processEvent(event);
        }
    }

    public void setEnabled(boolean enabled) {
        toolkit.lockAWT();
        try {
            this.enabled = enabled;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void addActionListener(ActionListener listener) {
        actionListeners.addUserListener(listener);
    }

    public ActionListener[] getActionListeners() {
        return actionListeners.getUserListeners(new ActionListener[0]);
    }

    protected void processActionEvent(ActionEvent event) {
        for (ActionListener listener : actionListeners.getUserListeners()) {
            switch (event.getID()) {
                case ActionEvent.ACTION_PERFORMED:
                    listener.actionPerformed(event);
                    break;
            }
        }
    }

    public void removeActionListener(ActionListener listener) {
        actionListeners.removeUserListener(listener);
    }

    public void deleteShortcut() {
        toolkit.lockAWT();
        try {
            shortcut = null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public MenuShortcut getShortcut() {
        toolkit.lockAWT();
        try {
            return shortcut;
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

    public void setLabel(String label) {
        toolkit.lockAWT();
        try {
            this.label = label;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setShortcut(MenuShortcut shortcut) {
        toolkit.lockAWT();
        try {
            this.shortcut = shortcut;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    void itemSelected(long when, int modifiers) {
        AWTEvent event = createEvent(when, modifiers);
        toolkit.getSystemEventQueueImpl().postEvent(event);
        super.itemSelected(when, modifiers);
    }

    AWTEvent createEvent(long when, int modifiers) {
        return new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getActionCommand(), when,
                modifiers);
    }

    @Override
    AccessibleContext createAccessibleContext() {
        return new AccessibleAWTMenuItem();
    }
}
