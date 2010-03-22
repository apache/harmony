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
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.apache.harmony.x.swing.Utilities;


public class MenuSelectionManager {

    protected transient ChangeEvent changeEvent;
    protected EventListenerList listenerList = new EventListenerList();

    static final MenuElement[] EMPTY_PATH = new MenuElement[0];

    private static MenuSelectionManager defaultManager;

    private MenuElement[] selectedPath;

    public static MenuSelectionManager defaultManager() {
        if (defaultManager == null) {
            defaultManager = new MenuSelectionManager();
        }
        return defaultManager;
    }

    public void addChangeListener(final ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }

    public void removeChangeListener(final ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }

    public ChangeListener[] getChangeListeners() {
        return (ChangeListener[])listenerList.getListeners(ChangeListener.class);
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

    public void setSelectedPath(final MenuElement[] path) {
        if (selectedPath == path) {
            return;
        }

        int diffStart = 0;
        if (selectedPath != null && path != null) {
            int minLength = Math.min(selectedPath.length, path.length);
            while (diffStart < minLength && selectedPath[diffStart] == path[diffStart]) {
                diffStart++;
            }
        }
        if (selectedPath != null) {
            for (int j = selectedPath.length - 1; j >= diffStart; j--) {
                selectedPath[j].menuSelectionChanged(false);
            }
        }
        if (path != null) {
            for (int j = diffStart; j < path.length; j++) {
                path[j].menuSelectionChanged(true);
            }
        }

        selectedPath = path;
        fireStateChanged();
    }

    public MenuElement[] getSelectedPath() {
        if (isPathEmpty()) {
            return EMPTY_PATH;
        }

        return (MenuElement[])selectedPath.clone();
    }

    public void clearSelectedPath() {
        setSelectedPath(null);
    }

    public Component componentForPoint(final Component c, final Point p) {
        if (isPathEmpty()) {
            return null;
        }

        for (int i = selectedPath.length - 1; i >= 0; i--) {
            Component curComponent = selectedPath[i].getComponent();
            Point convertedPoint = SwingUtilities.convertPoint(c, p, curComponent);
            if (curComponent.contains(convertedPoint)) {
                return curComponent;
            }
        }

        return null;
    }

    public boolean isComponentPartOfCurrentMenu(final Component c) {
        if (isPathEmpty()) {
            return false;
        }
        final MenuElement firstMenuElement = selectedPath[0];
        if (firstMenuElement == c) {
            return true;
        }

        if (firstMenuElement instanceof JMenuBar || firstMenuElement instanceof JPopupMenu) {
            final JComponent menu = (JComponent)firstMenuElement;
            final int numComponents = menu.getComponentCount();
            for (int i = 0; i < numComponents; i++) {
                Component curItem = menu.getComponent(i);
                if ((curItem == c) || curItem instanceof JMenu
                    && ((JMenu)curItem).isMenuComponent(c)) {
                    return true;
                }
            }
        } else if (firstMenuElement instanceof JMenu) {
            return ((JMenu)firstMenuElement).isMenuComponent(c);
        }

        return false;
    }

    public void processKeyEvent(final KeyEvent event) {
        if (isPathEmpty()) {
            return;
        }

        for (int i = selectedPath.length - 1; i >= 0; i--) {
            final MenuElement[] subElements = selectedPath[i].getSubElements();
            for (int j = 0; j < subElements.length; j++) {
                Component c = subElements[j].getComponent();
                if (!c.isVisible() || !c.isEnabled()) {
                    continue;
                }
                if (c instanceof MenuElement) {
                    final MenuElement[] curPath = Utilities.getMenuElementPath(subElements[j]);
                    ((MenuElement)c).processKeyEvent(event, curPath, this);
                    if (event.isConsumed()) {
                        return;
                    }
                }
            }
        }
    }

    public void processMouseEvent(final MouseEvent event) {   
        Component c = componentForPoint((Component) event.getSource(), event.getPoint());

        if (isPathEmpty()) {
            return;
        }

        if ((event.getID() == MouseEvent.MOUSE_DRAGGED)
                || (event.getID() == MouseEvent.MOUSE_RELEASED)) {

            if (c instanceof JMenuItem) {
                ((JMenuItem)c).processMouseEvent(event, selectedPath, this);
            }
        }
    }

    boolean isPathEmpty() {
        return Utilities.isEmptyArray(selectedPath);
    }
}

