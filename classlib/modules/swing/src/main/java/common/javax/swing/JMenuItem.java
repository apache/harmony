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
 * @author Alexander T. Simbirtsev
 */
package javax.swing;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuDragMouseEvent;
import javax.swing.event.MenuDragMouseListener;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.swing.plaf.MenuItemUI;

import org.apache.harmony.x.swing.StringConstants;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class JMenuItem extends AbstractButton implements Accessible, MenuElement {

    // TODO implement
    protected class AccessibleJMenuItem extends AccessibleAbstractButton
            implements ChangeListener {

        AccessibleJMenuItem() {
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU_ITEM;
        }

        public void stateChanged(final ChangeEvent e) {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
    }

    private final static String UI_CLASS_ID = "MenuItemUI";

    private KeyStroke accelerator;
    private int prevMouseEventID;

    public JMenuItem() {
        this(null, null);
    }

    public JMenuItem(final Icon icon) {
        this(null, icon);
    }

    public JMenuItem(final String text) {
        this(text, null);
    }

    public JMenuItem(final String text, final int mnemonic) {
        setDefaultModelAndFocus();
        setMnemonic(mnemonic);
        init(text, null);
    }

    public JMenuItem(final String text, final Icon icon) {
        setDefaultModelAndFocus();
        init(text, icon);
    }

    public JMenuItem(final Action action) {
        setDefaultModelAndFocus();
        setAction(action);
        init(getText(), getIcon());
    }

    void configurePropertyFromAction(final Action action, final Object propertyName) {
        if (propertyName != null && propertyName.equals(Action.ACCELERATOR_KEY)) {
            setAccelerator((KeyStroke)action.getValue(Action.ACCELERATOR_KEY));
        }
        super.configurePropertyFromAction(action, propertyName);
    }

    public void setUI(final MenuItemUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
        setUI((MenuItemUI)UIManager.getUI(this));
    }

    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public void setArmed(final boolean armed) {
        model.setArmed(armed);
    }

    public boolean isArmed() {
        return model.isArmed();
    }

    public void setAccelerator(final KeyStroke keyStroke) {
        KeyStroke oldValue = accelerator;
        accelerator = keyStroke;
        firePropertyChange(StringConstants.ACCELERATOR_PROPERTY, oldValue, accelerator);
    }

    public KeyStroke getAccelerator() {
        return accelerator;
    }

    protected void fireMenuDragMouseEntered(final MenuDragMouseEvent event) {
        final MenuDragMouseListener[] listeners = getMenuDragMouseListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].menuDragMouseEntered(event);
        }
    }

    protected void fireMenuDragMouseExited(final MenuDragMouseEvent event) {
        final MenuDragMouseListener[] listeners = getMenuDragMouseListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].menuDragMouseExited(event);
        }
    }

    protected void fireMenuDragMouseDragged(final MenuDragMouseEvent event) {
        final MenuDragMouseListener[] listeners = getMenuDragMouseListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].menuDragMouseDragged(event);
        }
    }

    protected void fireMenuDragMouseReleased(final MenuDragMouseEvent event) {
        final MenuDragMouseListener[] listeners = getMenuDragMouseListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].menuDragMouseReleased(event);
        }
    }

    protected void fireMenuKeyPressed(final MenuKeyEvent event) {
        final MenuKeyListener[] listeners = getMenuKeyListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].menuKeyPressed(event);
        }
    }

    protected void fireMenuKeyReleased(final MenuKeyEvent event) {
        final MenuKeyListener[] listeners = getMenuKeyListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].menuKeyReleased(event);
        }
    }

    protected void fireMenuKeyTyped(final MenuKeyEvent event) {
        final MenuKeyListener[] listeners = getMenuKeyListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].menuKeyTyped(event);
        }
    }

    public void addMenuDragMouseListener(final MenuDragMouseListener listener) {
        listenerList.add(MenuDragMouseListener.class, listener);
    }

    public void removeMenuDragMouseListener(final MenuDragMouseListener listener) {
        listenerList.remove(MenuDragMouseListener.class, listener);
    }

    public MenuDragMouseListener[] getMenuDragMouseListeners() {
        return (MenuDragMouseListener[])getListeners(MenuDragMouseListener.class);
    }

    public void addMenuKeyListener(final MenuKeyListener listener) {
        listenerList.add(MenuKeyListener.class, listener);
    }

    public void removeMenuKeyListener(final MenuKeyListener listener) {
        listenerList.remove(MenuKeyListener.class, listener);
    }

    public MenuKeyListener[] getMenuKeyListeners() {
        return (MenuKeyListener[])getListeners(MenuKeyListener.class);
    }

    public AccessibleContext getAccessibleContext() {
        return (accessibleContext == null) ? (accessibleContext = new AccessibleJMenuItem())
                : accessibleContext;
    }

    public Component getComponent() {
        return this;
    }

    public MenuElement[] getSubElements() {
        return MenuSelectionManager.EMPTY_PATH;
    }

    public void menuSelectionChanged(final boolean isSelected) {
        setArmed(isSelected);
    }

    public void processMenuKeyEvent(final MenuKeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            fireMenuKeyPressed(e);
        } else if (e.getID() == KeyEvent.KEY_RELEASED) {
            fireMenuKeyReleased(e);
        } else if (e.getID() == KeyEvent.KEY_TYPED) {
            fireMenuKeyTyped(e);
        }
    }

    public void processKeyEvent(final KeyEvent event, final MenuElement[] path,
                                final MenuSelectionManager manager) {
        final MenuKeyEvent menuKeyEvent = new MenuKeyEvent(event.getComponent(), event.getID(),
                                             event.getWhen(), event.getModifiersEx(),
                                             event.getKeyCode(), event.getKeyChar(),
                                             path, manager);
        processMenuKeyEvent(menuKeyEvent);
        if (menuKeyEvent.isConsumed()) {
            event.consume();
            return;
        }
        if (processMnemonics(event)) {
            event.consume();
            return;
        }
    }

    public void processMenuDragMouseEvent(final MenuDragMouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_ENTERED) {
            fireMenuDragMouseEntered(e);
        } else if (e.getID() == MouseEvent.MOUSE_EXITED) {
            fireMenuDragMouseExited(e);
        } else if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
            fireMenuDragMouseDragged(e);
        } else if (e.getID() == MouseEvent.MOUSE_RELEASED &&
                prevMouseEventID == MouseEvent.MOUSE_DRAGGED) {

            fireMenuDragMouseReleased(e);
        }
        prevMouseEventID = e.getID();
    }

    public void processMouseEvent(final MouseEvent event, final MenuElement[] path,
                                  final MenuSelectionManager manager) {

        processMenuDragMouseEvent(new MenuDragMouseEvent((Component)event.getSource(),
                                                         event.getID(), event.getWhen(),
                                                         event.getModifiers(),
                                                         event.getX(), event.getY(),
                                                         event.getClickCount(),
                                                         event.isPopupTrigger(),
                                                         path, manager));
    }

    ButtonModel createDefaultModel() {
        return new DefaultButtonModel();
    }

    void setDefaultModelAndFocus() {
        setModel(createDefaultModel());
        setFocusPainted(false);
        setFocusable(false);
        setHorizontalAlignment(SwingConstants.LEADING);
    }

    static JMenuItem createJMenuItem(final Action action) {
        final JMenuItem result = new JMenuItem();
        result.configurePropertiesFromAction(action);
        result.setActionCommand(result.getText());
        return result;
    }

    boolean isMnemonicKeyStroke(final KeyStroke keyStroke) {
        return (keyStroke.getKeyCode() == getMnemonic()) &&
                !MenuSelectionManager.defaultManager().isPathEmpty();
    }
}
