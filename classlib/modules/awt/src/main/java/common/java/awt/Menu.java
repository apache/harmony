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

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.state.MenuState;

public class Menu extends MenuItem implements MenuContainer, Accessible {
    private static final long serialVersionUID = -8809584163345499784L;

    final static int LAST_ELEMENT = Integer.MAX_VALUE;

    private final ArrayList<MenuItem> menuItems = new ArrayList<MenuItem>();

    private final Point location = new Point();

    private final Dimension size = new Dimension();

    private final boolean tearOff;

    final MenuPopupBox popupBox = new MenuPopupBox();

    protected class AccessibleAWTMenu extends AccessibleAWTMenuItem {
        private static final long serialVersionUID = 5228160894980069094L;

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU;
        }
    }

    /**
     * The internal menu's state utilized by the visual theme
     */
    final class State extends MenuComponent.State implements MenuState {
        @Override
        public int getWidth() {
            return Menu.this.getWidth();
        }

        @Override
        public int getHeight() {
            return Menu.this.getHeight();
        }

        @Override
        public Font getFont() {
            return Menu.this.getFont();
        }

        @Override
        public int getItemCount() {
            return Menu.this.getItemCount();
        }

        @Override
        public int getSelectedItemIndex() {
            return Menu.this.getSelectedItemIndex();
        }

        @Override
        public boolean isFontSet() {
            return Menu.this.isFontSet();
        }

        @SuppressWarnings("deprecation")
        @Override
        public FontMetrics getFontMetrics(Font f) {
            return Menu.this.toolkit.getFontMetrics(f);
        }

        @Override
        public Point getLocation() {
            return Menu.this.location;
        }
    }

    final State menuState = new State();

    public Menu(String label) throws HeadlessException {
        this(label, false);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Menu(String label, boolean tearOff) throws HeadlessException {
        super(label);
        toolkit.lockAWT();
        try {
            this.tearOff = tearOff;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Menu() throws HeadlessException {
        this("", false); //$NON-NLS-1$
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void add(String label) {
        toolkit.lockAWT();
        try {
            add(new MenuItem(label));
        } finally {
            toolkit.unlockAWT();
        }
    }

    public MenuItem add(MenuItem item) {
        toolkit.lockAWT();
        try {
            insertImpl(item, LAST_ELEMENT);
            return item;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void remove(int index) {
        toolkit.lockAWT();
        try {
            removeImpl(index);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void remove(MenuComponent item) {
        if (item == null) {
            return;
        }
        toolkit.lockAWT();
        try {
            int index = menuItems.indexOf(item);
            removeImpl(index);
        } finally {
            toolkit.unlockAWT();
        }
    }

    void removeImpl(int index) {
        MenuComponent item = menuItems.remove(index);
        item.setParent(null);
    }

    public void removeAll() {
        toolkit.lockAWT();
        try {
            while (!menuItems.isEmpty()) {
                removeImpl(menuItems.size() - 1);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void insert(String label, int index) {
        toolkit.lockAWT();
        try {
            insertImpl(new MenuItem(label), index);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void insert(MenuItem item, int index) {
        toolkit.lockAWT();
        try {
            insertImpl(item, index);
        } finally {
            toolkit.unlockAWT();
        }
    }

    void insertImpl(MenuItem item, int index) {
        if (index < 0) {
            // awt.6F=Index less than zero
            throw new IllegalArgumentException(Messages.getString("awt.6F")); //$NON-NLS-1$
        }
        if (item == null) {
            // awt.70=MenuItem is null
            throw new NullPointerException(Messages.getString("awt.70")); //$NON-NLS-1$
        }
        MenuContainer oldParent = item.getParent();
        if (oldParent != null) {
            oldParent.remove(item);
        }
        item.setParent(this);
        if (index >= menuItems.size()) {
            menuItems.add(item);
        } else {
            menuItems.add(index, item);
        }
    }

    @Override
    public String paramString() {
        toolkit.lockAWT();
        try {
            return super.paramString() + (tearOff ? ",tearOff" : ""); //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public MenuItem getItem(int index) {
        toolkit.lockAWT();
        try {
            return menuItems.get(index);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void addNotify() {
        toolkit.lockAWT();
        try {
            popupBox.addNotify();
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
    public void removeNotify() {
        toolkit.lockAWT();
        try {
            for (int i = 0; i < getItemCount(); i++) {
                getItem(i).removeNotify();
            }
            super.removeNotify();
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public int countItems() {
        toolkit.lockAWT();
        try {
            return getItemCount();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public int getItemCount() {
        toolkit.lockAWT();
        try {
            return menuItems.size();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void addSeparator() {
        toolkit.lockAWT();
        try {
            add(new MenuItem("-")); //$NON-NLS-1$
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void insertSeparator(int index) {
        toolkit.lockAWT();
        try {
            insert(new MenuItem("-"), index); //$NON-NLS-1$
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isTearOff() {
        return tearOff;
    }

    @Override
    Point getLocation() {
        return location;
    }

    Dimension getSize() {
        return size;
    }

    @Override
    int getWidth() {
        return size.width;
    }

    @Override
    int getHeight() {
        return size.height;
    }

    @Override
    void paint(Graphics gr) {
        toolkit.theme.drawMenu(menuState, gr);
    }

    /**
     * Show menu on the screen
     * @param x - screen X coordinate
     * @param y - screen Y coordinate
     */
    void show(int x, int y, boolean modal) {
        if (parent == null) {
            // awt.71=Parent is null
            throw new NullPointerException(Messages.getString("awt.71")); //$NON-NLS-1$
        }
        location.x = x;
        location.y = y;
        selectItem(-1, true);
        PopupBox parentBox = null;
        if (parent instanceof MenuComponent) {
            parentBox = ((MenuComponent) parent).getPopupBox();
        }
        size.setSize(toolkit.theme.calculateMenuSize(menuState));
        popupBox.setParent(parentBox);
        popupBox.setModal(modal);
        popupBox.show(location, size, getOwnerWindow());
    }

    Window getOwnerWindow() {
        for (MenuContainer cont = getParent(); cont != null;) {
            if (cont instanceof Component) {
                return ((Component) cont).getWindowAncestor();
            }
            if (cont instanceof Menu) {
                cont = ((Menu) cont).parent;
                continue;
            }
            if (cont instanceof MenuBar) {
                return (Window) ((MenuBar) cont).parent;
            }
        }
        return null;
    }

    @Override
    void hide() {
        super.hide();
        for (int i = 0; i < getItemCount(); i++) {
            getItem(i).hide();
        }
        popupBox.hide();
    }

    @Override
    boolean isVisible() {
        return popupBox.isVisible();
    }

    @Override
    PopupBox getPopupBox() {
        return popupBox;
    }

    @Override
    void onMouseEvent(int eventId, Point where, int mouseButton, long when, int modifiers) {
        if (eventId == MouseEvent.MOUSE_PRESSED || eventId == MouseEvent.MOUSE_MOVED
                || eventId == MouseEvent.MOUSE_DRAGGED) {
            int index = toolkit.theme.getMenuItemIndex(menuState, where);
            if (index >= 0 || getSelectedSubmenu() == null) {
                selectItem(index);
            }
        } else if (eventId == MouseEvent.MOUSE_RELEASED) {
            int index = toolkit.theme.getMenuItemIndex(menuState, where);
            selectItem(index);
            if (index >= 0) {
                fireItemAction(index, when, modifiers);
            }
        } else if (eventId == MouseEvent.MOUSE_EXITED) {
            if (getSelectedSubmenu() == null) {
                selectItem(-1);
            }
        }
    }

    @Override
    void onKeyEvent(int eventId, int vKey, long when, int modifiers) {
        if (eventId != KeyEvent.KEY_PRESSED) {
            return;
        }
        int selected = getSelectedItemIndex();
        MenuBar menuBar;
        switch (vKey) {
            case KeyEvent.VK_ESCAPE:
                hide();
                break;
            case KeyEvent.VK_RIGHT:
                Menu subMenu = getSelectedSubmenu();
                if (subMenu != null) {
                    showSubMenu(selected);
                    subMenu.selectNextItem(true, false);
                } else {
                    menuBar = getMenuBar();
                    if (menuBar != null) {
                        menuBar.onKeyEvent(eventId, vKey, when, modifiers);
                    }
                }
                break;
            case KeyEvent.VK_LEFT:
                if (parent instanceof Menu) {
                    hide();
                } else {
                    menuBar = getMenuBar();
                    if (menuBar != null) {
                        menuBar.onKeyEvent(eventId, vKey, when, modifiers);
                    }
                }
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
                selectNextItem(vKey == KeyEvent.VK_DOWN, false);
                break;
            case KeyEvent.VK_ENTER:
                if (selected >= 0) {
                    fireItemAction(selected, when, modifiers);
                } else {
                    hide();
                }
        }
    }

    @Override
    void itemSelected(long when, int modifiers) {
        // do nothing
    }

    @Override
    Rectangle getItemRect(int index) {
        return menuState.getItem(index).getItemBounds();
    }

    @Override
    Point getSubmenuLocation(int index) {
        return toolkit.theme.getMenuItemLocation(menuState, index);
    }

    @Override
    Graphics getGraphics(MultiRectArea clip) {
        return popupBox.getGraphics(clip);
    }

    @Override
    AccessibleContext createAccessibleContext() {
        return new AccessibleAWTMenu();
    }

    void collectShortcuts(HashSet<MenuShortcut> shortcuts) {
        for (int i = 0; i < menuItems.size(); i++) {
            MenuItem item = menuItems.get(i);
            if (item instanceof Menu) {
                ((Menu) item).collectShortcuts(shortcuts);
            } else {
                MenuShortcut ms = item.getShortcut();
                if (ms != null) {
                    shortcuts.add(ms);
                }
            }
        }
    }
}
