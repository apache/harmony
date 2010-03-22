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
package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.Toolkit;
import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.AWTEventListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultFocusManager;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

import org.apache.harmony.x.swing.Utilities;


class MenuKeyBindingProcessor implements KeyEventDispatcher {

    private abstract static class GenericNavigationAction extends AbstractAction {
        protected static final int FORWARD = 1;
        protected static final int BACKWARD = -1;

        public GenericNavigationAction(final String name) {
            super(name);
        }

        protected final int getPopupIndex(final MenuElement[] path, final JPopupMenu popup) {
            int index = path.length - 1;
            while(index >= 0 && path[index] != popup) {
                index--;
            }
            return index;
        }

        protected final MenuElement[] derivePath(final MenuElement[] path,
                                                 final int length) {
            return derivePath(path, length, length);
        }

        protected final MenuElement[] derivePath(final MenuElement[] path,
                                           final int newLength,
                                           final int copyLength) {

            final MenuElement[] result = new MenuElement[newLength];
            System.arraycopy(path, 0, result, 0, copyLength);
            return result;
        }

        protected final MenuElement[] getSelectedPath() {
            return MenuSelectionManager.defaultManager().getSelectedPath();
        }

        protected final void setSelectedPath(final MenuElement[] path) {
            MenuSelectionManager.defaultManager().setSelectedPath(path);
        }
    }

    private abstract static class ChildParentAction extends GenericNavigationAction {
        private final int direction;

        public ChildParentAction(final String name, final int direction) {
            super(name);
            this.direction = direction;
        }

        protected void selectNextTopLevelMenu(final MenuElement[] path) {
            if (Utilities.isEmptyArray(path) || !(path[0] instanceof JMenuBar)) {
                return;
            }
            final JMenuBar menuBar = (JMenuBar)path[0];
            final JMenu menu = (JMenu)path[1];
            final int menuIndex = menuBar.getComponentIndex(menu);
            final JMenu nextMenu = getNextMenu(menuBar, menuIndex);
            if (nextMenu != null && nextMenu != menu) {
                setSelectedPath(new MenuElement[] {menuBar, nextMenu,
                                                   nextMenu.getPopupMenu()});
            }
        }

        private JMenu getNextMenu(final JMenuBar menuBar, final int index) {
            final int numElements = menuBar.getMenuCount();
            for (int i = 1; i <= numElements; i++) {
                int j = (direction > 0 ? index + i : numElements + index - i) % numElements;
                final JMenu menu = menuBar.getMenu(j);
                if (menu != null) {
                    return menu;
                }
            }
            return null;
        }
    }

    private static final Action SELECT_PARENT_ACTION = new ChildParentAction("selectParent", GenericNavigationAction.BACKWARD) {
        public void actionPerformed(final ActionEvent e) {
            final JPopupMenu popup = (JPopupMenu)e.getSource();
            final MenuElement[] path = getSelectedPath();
            if (path.length > 3) {
                int index = getPopupIndex(path, popup);
                JMenu menu = (JMenu)path[index - 1];
                if (!menu.isTopLevelMenu()) {
                    setSelectedPath(derivePath(path, index));
                    return;
                }
            }
            selectNextTopLevelMenu(path);
        }
    };

    private static final Action SELECT_CHILD_ACTION = new ChildParentAction("selectChild", GenericNavigationAction.FORWARD) {
        public void actionPerformed(final ActionEvent e) {
            final MenuElement[] path = getSelectedPath();
            if (path.length > 3 || path[0] instanceof JPopupMenu) {
                final MenuElement lastElement = path[path.length - 1];
                final MenuElement[] subElements = lastElement.getSubElements();
                if (!Utilities.isEmptyArray(subElements)) {
                    MenuElement[] newPath;
                    final MenuElement newSelectedItem = subElements[0];
                    final MenuElement newSelectedChild = Utilities.getFirstSelectableItem(newSelectedItem.getSubElements());
                    if (newSelectedItem instanceof JPopupMenu && newSelectedChild != null) {
                        newPath = derivePath(path, path.length + 2, path.length);
                        newPath[path.length + 1] = newSelectedChild;
                    } else {
                        newPath = derivePath(path, path.length + 1, path.length);
                    }
                    newPath[path.length] = newSelectedItem;
                    setSelectedPath(newPath);
                    return;
                }
            }
            selectNextTopLevelMenu(path);
        }
    };

    private static class PrevNextAction extends GenericNavigationAction {
        private final int direction;

        public PrevNextAction(final String name, final int direction) {
            super(name);
            this.direction = direction;
        }

        public void actionPerformed(final ActionEvent e) {
            final JPopupMenu popup = (JPopupMenu)e.getSource();
            final MenuElement[] path = getSelectedPath();
            int index = getPopupIndex(path, popup);
            if (popup.getComponentCount() != 0) {
                final MenuElement curSelection = (index != path.length - 1) ? path[path.length - 1] : null;
                setPath(path, index, curSelection, popup);
            } else {
                index = getNextPopupIndex(path, --index);
                if (index >= 0) {
                    setPath(path, index, path[index + 1], popup);
                }
            }
        }

        private int getNextPopupIndex(final MenuElement[] path, final int startIndex) {
            int index = startIndex;
            while(index >= 0 && !(path[index] instanceof JPopupMenu)) {
                index--;
            }
            return index;
        }

        private void setPath(final MenuElement[] path, final int index,
                             final MenuElement curSelection, final JPopupMenu menu) {
            final MenuElement nextMenuItem = getNextMenuItem(menu, curSelection);
            if (nextMenuItem != null) {
                final MenuElement[] newPath = derivePath(path, index + 2, index + 1);
                newPath[newPath.length - 1] = nextMenuItem;
                setSelectedPath(newPath);
            }
        }

        private MenuElement getNextMenuItem(final JPopupMenu menu, final MenuElement curSelection) {
            if (curSelection == null) {
                int startIndex = (direction == FORWARD) ? menu.getComponentCount() - 1 : 0;
                return getNextMenuItem(menu, startIndex);
            }
            final int startIndex = getSelectionIndex(menu, curSelection);
            if (startIndex >= 0) {
                return getNextMenuItem(menu, startIndex);
            }
            return null;
        }

        private MenuElement getNextMenuItem(final JPopupMenu menu, final int index) {
            final int numElements = menu.getComponentCount();
            for (int i = 1; i <= numElements; i++) {
                int j = ((direction > 0) ? index + i : numElements + index - i) % numElements;
                final Component menuComponent = menu.getComponent(j);
                if (menuComponent.isVisible() && menuComponent.isEnabled()
                    && menuComponent instanceof MenuElement) {

                    return (MenuElement)menuComponent;
                }
            }
            return null;
        }

        private int getSelectionIndex(final JPopupMenu menu, final MenuElement selection) {
            int numElements = menu.getComponentCount();
            for (int i = 0; i < numElements; i++) {
                if (menu.getComponent(i) == selection) {
                    return i;
                }
            }
            return -1;
        }
    }

    private static final Action SELECT_NEXT_ACTION = new PrevNextAction("selectNext", PrevNextAction.FORWARD);
    private static final Action SELECT_PREVIOUS_ACTION = new PrevNextAction("selectPrevious", PrevNextAction.BACKWARD);

    private static final class CancelEventFireStarter extends JPopupMenu {
        private static final Class[] NO_CLASSES_NO_ARGUMENTS = new Class[0];

        private Method fireCanceled = null;

        public CancelEventFireStarter() {
            try {
                fireCanceled = JPopupMenu.class.getDeclaredMethod("firePopupMenuCanceled", NO_CLASSES_NO_ARGUMENTS);
                fireCanceled.setAccessible(true);
            } catch (SecurityException e) {
            } catch (NoSuchMethodException e) {}
        }

        public void firePopupMenuCanceled(final JPopupMenu popup) {
            if (fireCanceled == null) {
                return;
            }
            try {
                fireCanceled.invoke(popup, (Object[])NO_CLASSES_NO_ARGUMENTS);
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {}
        }
    }

    private static final Action CANCEL_ACTION = new GenericNavigationAction("cancel") {
        private final CancelEventFireStarter cancelEventFireStarter = new CancelEventFireStarter();

        public void actionPerformed(final ActionEvent e) {
            final JPopupMenu popup = (JPopupMenu)e.getSource();
            cancelEventFireStarter.firePopupMenuCanceled(popup);

            final MenuElement[] path = getSelectedPath();
            if (path.length > 4) {
                setSelectedPath(derivePath(path, path.length - 2));
            } else {
                setSelectedPath(null);
            }
        }
    };

    private static final Action RETURN_ACTION = new GenericNavigationAction("return") {
        public void actionPerformed(final ActionEvent e) {
            final MenuElement[] path = getSelectedPath();
            if (Utilities.isEmptyArray(path)) {
                return;
            }
            final MenuElement selection = path[path.length - 1];
            if (selection instanceof JMenuItem) {
                MenuSelectionManager.defaultManager().clearSelectedPath();
                final JMenuItem menuItem = (JMenuItem)selection;
                menuItem.doClick(0);
            }
        }
    };

    private final HashMap actionMap = new HashMap();

    private static int numInstallations;
    private static MenuKeyBindingProcessor sharedInstance;

    private MenuKeyBindingProcessor() {
        installActions();
    }

    public static void attach() {
        if (sharedInstance == null) {
            sharedInstance = new MenuKeyBindingProcessor();
        }
        if (numInstallations == 0) {
            DefaultFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(sharedInstance);
        }
        numInstallations++;
    }

    public static void detach() {
        if (numInstallations == 1) {
            DefaultFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(sharedInstance);
        }
        if (numInstallations > 0) {
            numInstallations--;
        }
    }

    public boolean dispatchKeyEvent(final KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_PRESSED || e.isConsumed()) {
            return false;
        }

        // dispatch event to user listeners
        for (AWTEventListener listener :
                Toolkit.getDefaultToolkit().getAWTEventListeners(
                    AWTEvent.KEY_EVENT_MASK)) {
            listener.eventDispatched(e);
        }
        
        if (e.isConsumed()) {
            // consumed by user listener
            return true;
        }
        
        final JPopupMenu activePopupMenu = getActivePopupMenu();
        if (activePopupMenu == null) {
            return false;
        }
        final KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);
        final Object actionKey = activePopupMenu.getInputMap().get(ks);
        final Action action = (Action)actionMap.get(actionKey);
        if (action == null) {
            return false;
        }
        
        SwingUtilities.notifyAction(action, ks, e, activePopupMenu, e.getModifiersEx());
        return true;
    }

    private JPopupMenu getActivePopupMenu() {
        final MenuElement[] path = MenuSelectionManager.defaultManager().getSelectedPath();
        if (Utilities.isEmptyArray(path)) {
            return null;
        }

        if (!Utilities.isValidFirstPathElement(path[0])) {
            return null;
        }

        for (int i = path.length - 1; i >= 0; i--) {
            if (path[i] instanceof JPopupMenu) {
                return (JPopupMenu)path[i];
            }
        }

        return null;
    }

    private void installActions() {
        addAction(actionMap, SELECT_CHILD_ACTION);
        addAction(actionMap, SELECT_PARENT_ACTION);
        addAction(actionMap, SELECT_PREVIOUS_ACTION);
        addAction(actionMap, SELECT_NEXT_ACTION);
        addAction(actionMap, RETURN_ACTION);
        addAction(actionMap, CANCEL_ACTION);
    }

    private void addAction(final HashMap actionMap, final Action action) {
        actionMap.put(action.getValue(Action.NAME), action);
    }

}
