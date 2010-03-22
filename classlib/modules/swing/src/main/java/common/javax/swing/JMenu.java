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

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.apache.harmony.luni.util.NotImplementedException;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>JMenu</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JMenu extends JMenuItem implements Accessible, MenuElement {
    private static final long serialVersionUID = 6344812061970456262L;

    // TODO implement accessibility
    protected class AccessibleJMenu extends AccessibleJMenuItem implements AccessibleSelection {
        private static final long serialVersionUID = -7871723353224195081L;

        public void addAccessibleSelection(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void clearAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public int getAccessibleChildrenCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public Accessible getAccessibleChild(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU;
        }

        @Override
        public AccessibleSelection getAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Accessible getAccessibleSelection(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleSelectionCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public boolean isAccessibleChildSelected(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void removeAccessibleSelection(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void selectAllAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }
    }

    protected class WinListener extends WindowAdapter implements Serializable {
        private static final long serialVersionUID = 1L;

        private final JPopupMenu popup;

        public WinListener(JPopupMenu p) {
            popup = p;
        }

        @Override
        public void windowClosing(WindowEvent e) {
            setSelected(false);
        }
    }

    protected WinListener popupListener;

    private static final String UI_CLASS_ID = "MenuUI";

    private static final Object ALL_ACTION_PROPERTIES = new Object() { //$NON-LOCK-1$
        @Override
        public boolean equals(Object o) {
            return !Action.ACCELERATOR_KEY.equals(o);
        }
    };

    private int delay = 200;

    private JPopupMenu popup;

    private transient MenuEvent menuEvent;

    private transient int[] uiMnemonicModifiers;

    private transient boolean crossMenuMnemonic;

    public JMenu() {
        super();
    }

    public JMenu(String text) {
        super(text);
    }

    public JMenu(String text, boolean b) {
        super(text);
    }

    public JMenu(Action action) {
        setDefaultModelAndFocus();
        setAction(action);
        init(getText(), getIcon());
    }

    @Override
    void configurePropertyFromAction(Action action, Object propertyName) {
        if (propertyName == null || propertyName.equals(Action.ACCELERATOR_KEY)) {
            return;
        }
        super.configurePropertyFromAction(action, propertyName);
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        return (accessibleContext == null) ? (accessibleContext = new AccessibleJMenu())
                : accessibleContext;
    }

    protected PropertyChangeListener createActionChangeListener(JMenuItem item) {
        return item.createActionPropertyChangeListener(getAction());
    }

    protected JMenuItem createActionComponent(Action action) {
        return JMenuItem.createJMenuItem(action);
    }

    protected WinListener createWinListener(JPopupMenu popup) {
        return new WinListener(popup);
    }

    public void addMenuListener(MenuListener listener) {
        listenerList.add(MenuListener.class, listener);
    }

    public void removeMenuListener(MenuListener listener) {
        listenerList.remove(MenuListener.class, listener);
    }

    public MenuListener[] getMenuListeners() {
        return getListeners(MenuListener.class);
    }

    protected void fireMenuCanceled() {
        final MenuListener[] listeners = getMenuListeners();
        if (listeners.length == 0) {
            return;
        }
        if (menuEvent == null) {
            menuEvent = new MenuEvent(this);
        }
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].menuCanceled(menuEvent);
        }
    }

    protected void fireMenuDeselected() {
        final MenuListener[] listeners = getMenuListeners();
        if (listeners.length == 0) {
            return;
        }
        if (menuEvent == null) {
            menuEvent = new MenuEvent(this);
        }
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].menuDeselected(menuEvent);
        }
    }

    protected void fireMenuSelected() {
        final MenuListener[] listeners = getMenuListeners();
        if (listeners.length == 0) {
            return;
        }
        if (menuEvent == null) {
            menuEvent = new MenuEvent(this);
        }
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].menuSelected(menuEvent);
        }
    }

    @Override
    public void doClick(int time) {
        final MenuElement[] path = Utilities.getMenuElementPath(getPopupMenu());
        MenuSelectionManager.defaultManager().setSelectedPath(path);
    }

    public JPopupMenu getPopupMenu() {
        if (popup == null) {
            popup = new JPopupMenu();
            popup.setInvoker(this);
        }
        return popup;
    }

    protected Point getPopupMenuOrigin() {
        final boolean leftToRight = getComponentOrientation().isLeftToRight();
        Point result = Utilities.getPopupLocation(getBounds(), getPopupMenu()
                .getPreferredSize(), leftToRight, !isTopLevelMenu());
        String prefix = isTopLevelMenu() ? "Menu.menuPopupOffset" : "Menu.submenuPopupOffset";
        int xOffset = UIManager.getInt(prefix + "X");
        int yOffset = UIManager.getInt(prefix + "Y");
        if (!leftToRight) {
            xOffset = -xOffset;
        }
        result.translate(xOffset - getX(), yOffset - getY());
        return result;
    }

    public boolean isPopupMenuVisible() {
        return popup != null ? popup.isVisible() : false;
    }

    public void setPopupMenuVisible(boolean visible) {
        if (visible == isPopupMenuVisible()) {
            return;
        }
        popup = getPopupMenu();
        if (visible) {
            if (isShowing()) {
                Point origin = getPopupMenuOrigin();
                popup.show(this, origin.x, origin.y);
            }
        } else {
            popup.setVisible(visible);
        }
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    @Override
    public void setSelected(boolean selected) {
        if (selected != isSelected()) {
            super.setSelected(selected);
            if (selected) {
                fireMenuSelected();
            } else {
                fireMenuDeselected();
            }
        }
    }

    public boolean isTearOff() {
        throw new Error(Messages.getString("swing.err.0A")); //$NON-NLS-1$
    }

    public boolean isTopLevelMenu() {
        return (getParent() instanceof JMenuBar);
    }

    @Override
    public void menuSelectionChanged(boolean b) {
        setSelected(b);
    }

    @Override
    public void setAccelerator(KeyStroke keyStroke) {
        throw new Error(Messages.getString("swing.err.0B","setAccelerator()","setMnemonic()"));//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public void setComponentOrientation(ComponentOrientation orientation) {
        super.setComponentOrientation(orientation);
        if (popup != null) {
            popup.setComponentOrientation(orientation);
        }
    }

    @Override
    public void applyComponentOrientation(ComponentOrientation orientation) {
        super.applyComponentOrientation(orientation);
        if (popup != null) {
            popup.applyComponentOrientation(orientation);
        }
    }

    public void setDelay(int delay) {
        if (delay < 0) {
            throw new IllegalArgumentException(Messages.getString("swing.1C")); //$NON-NLS-1$
        }
        this.delay = delay;
    }

    public int getDelay() {
        return delay;
    }

    public void setMenuLocation(int x, int y) {
        if (popup != null) {
            popup.setLocation(x, y);
        }
    }

    @Override
    public Component add(Component c) {
        return getPopupMenu().add(c);
    }

    public JMenuItem add(Action action) {
        return getPopupMenu().add(action);
    }

    @Override
    public Component add(Component c, int index) {
        return getPopupMenu().add(c, index);
    }

    public JMenuItem add(String text) {
        return getPopupMenu().add(text);
    }

    public JMenuItem add(JMenuItem item) {
        return getPopupMenu().add(item);
    }

    public void addSeparator() {
        getPopupMenu().addSeparator();
    }

    public void insert(String text, int index) {
        JMenuItem item = new JMenuItem(text);
        getPopupMenu().insert(item, getValidIndex(index));
    }

    public JMenuItem insert(JMenuItem item, int index) {
        getPopupMenu().insert(item, index);
        return item;
    }

    public JMenuItem insert(Action action, int index) {
        JMenuItem item = createActionComponent(action);
        getPopupMenu().insert(item, getValidIndex(index));
        return item;
    }

    public void insertSeparator(int index) {
        getPopupMenu().insert(new JPopupMenu.Separator(), getValidIndex(index));
    }

    public Component getMenuComponent(int index) {
        if (popup == null || index < 0 || index >= getMenuComponentCount()) {
            return null;
        }
        return popup.getComponent(index);
    }

    public JMenuItem getItem(int index) {
        if (popup == null || index < 0 || index >= getItemCount()) {
            return null;
        }
        Component c = popup.getComponent(index);
        return (c instanceof JMenuItem) ? (JMenuItem) c : null;
    }

    public int getItemCount() {
        return getMenuComponentCount();
    }

    public int getMenuComponentCount() {
        return (popup != null) ? popup.getComponentCount() : 0;
    }

    public Component[] getMenuComponents() {
        return (popup != null) ? popup.getComponents() : new Component[0];
    }

    @Override
    public MenuElement[] getSubElements() {
        return new MenuElement[] { getPopupMenu() };
    }

    @Override
    public void remove(Component c) {
        if (popup == null) {
            return;
        }
        Component[] subComponents = getMenuComponents();
        for (int i = 0; i < subComponents.length; i++) {
            if (subComponents[i] == c) {
                popup.remove(i);
                break;
            }
        }
    }

    @Override
    public void remove(int i) {
        if (popup != null) {
            popup.remove(i);
        }
    }

    public void remove(JMenuItem item) {
        remove((Component) item);
    }

    @Override
    public void removeAll() {
        if (popup != null) {
            popup.removeAll();
        }
    }

    public boolean isMenuComponent(Component c) {
        if (c == null) {
            return false;
        }
        if (c == this) {
            return true;
        }
        Component[] subComponents = getMenuComponents();
        for (int i = 0; i < subComponents.length; i++) {
            if (subComponents[i] == c) {
                return true;
            }
        }
        for (int i = 0; i < subComponents.length; i++) {
            if (subComponents[i] instanceof JMenu
                    && ((JMenu) subComponents[i]).isMenuComponent(c)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        uiMnemonicModifiers = (int[]) UIManager.get("Menu.shortcutKeys");
        crossMenuMnemonic = UIManager.getBoolean("Menu.crossMenuMnemonic");
    }

    @Override
    void setDefaultModelAndFocus() {
        setModel(createDefaultModel());
        setFocusPainted(false);
        setHorizontalAlignment(SwingConstants.LEADING);
    }

    @Override
    Object getActionPropertiesFilter() {
        return ALL_ACTION_PROPERTIES;
    }

    @Override
    boolean isMnemonicKeyStroke(KeyStroke keyStroke) {
        if (keyStroke.getKeyCode() != getMnemonic()) {
            return false;
        }
        final int modifiers = keyStroke.getModifiers();
        if (isTopLevelMenu()) {
            final MenuSelectionManager defaultManager = MenuSelectionManager.defaultManager();
            final boolean pathEmpty = defaultManager.isPathEmpty();
            if (!pathEmpty && !crossMenuMnemonic) {
                return false;
            }
            boolean enableStandardModifiers = defaultManager.isComponentPartOfCurrentMenu(this);
            return isMnemonicModifiers(modifiers, enableStandardModifiers);
        }
        return isStandardModifiers(modifiers);
    }

    private boolean isMnemonicModifiers(int modifiers, boolean forceStandardCheck) {
        if (forceStandardCheck && isStandardModifiers(modifiers)) {
            return true;
        }
        if (Utilities.isEmptyArray(uiMnemonicModifiers)) {
            return false;
        }
        for (int i = 0; i < uiMnemonicModifiers.length; i++) {
            if ((modifiers & uiMnemonicModifiers[i]) != 0) {
                return true;
            }
        }
        return false;
    }

    private boolean isStandardModifiers(int modifiers) {
        return (modifiers == 0) || (modifiers & InputEvent.ALT_DOWN_MASK) != 0;
    }

    private int getValidIndex(int index) {
        return index < getItemCount() ? index : getItemCount();
    }
}
