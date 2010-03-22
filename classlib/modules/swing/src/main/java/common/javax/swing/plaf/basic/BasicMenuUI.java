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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.LookAndFeel;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;


public class BasicMenuUI extends BasicMenuItemUI {

    // this class is obsolete
    public class ChangeHandler implements ChangeListener {
        public boolean isSelected;
        public JMenu menu;
        public BasicMenuUI ui;
        public Component wasFocused;

        public ChangeHandler(final JMenu menu, final BasicMenuUI ui) {
            this.menu = menu;
            this.ui = ui;
        }

        public void stateChanged(final ChangeEvent e) {
        }
    }

    protected class MouseInputHandler implements MouseInputListener {
        public void mouseEntered(final MouseEvent e) {
            final MenuSelectionManager manager = MenuSelectionManager.defaultManager();
            if (!manager.isComponentPartOfCurrentMenu(menuItem)) {
                return;
            }
            if (Utilities.isEmptyArray(manager.getSelectedPath())) {
                return;
            }
            if (!menuItem.isEnabled()) {
                return;
            }
            if (openMenuTimer != null) {
                openMenuTimer.stop();
            }
            final JMenu menu = (JMenu)menuItem;
            MenuElement[] path = Utilities.addToPath(manager.getSelectedPath(), menu);
            if (menu.isTopLevelMenu()) {
                path = Utilities.addToPath(path, menu.getPopupMenu());
                manager.setSelectedPath(path);
                OPEN_POPUP_ACTION.openPopup(menu);
            } else {
                manager.setSelectedPath(path);
                setupPostTimer(menu);
            }
        }

        public void mousePressed(final MouseEvent e) {
            final JMenu menu = (JMenu)menuItem;
            MenuSelectionManager defaultManager = MenuSelectionManager.defaultManager();
            if (menu.isTopLevelMenu()) {
                if (Utilities.isEmptyArray(defaultManager.getSelectedPath())) {
                    defaultManager.setSelectedPath(new MenuElement[] {(JMenuBar)menu.getParent(), menu});
                    OPEN_POPUP_ACTION.openPopup(menu);
                } else {
                    defaultManager.clearSelectedPath();
                }
            }
        }

        public void mouseReleased(final MouseEvent e) {
            MenuSelectionManager defaultManager = MenuSelectionManager.defaultManager();
            if (defaultManager.componentForPoint(e.getComponent(), e.getPoint()) == null) {
                defaultManager.clearSelectedPath();
            } else {
                defaultManager.processMouseEvent(e);
            }
        }

        public void mouseClicked(final MouseEvent e) {
            MenuSelectionManager.defaultManager().processMouseEvent(e);
        }

        public void mouseDragged(final MouseEvent e) {
            MenuSelectionManager.defaultManager().processMouseEvent(e);
        }

        public void mouseMoved(final MouseEvent e) {
            MenuSelectionManager.defaultManager().processMouseEvent(e);
        }

        public void mouseExited(final MouseEvent e) {
            MenuSelectionManager.defaultManager().processMouseEvent(e);
        }
    }

    private class MenuHandler implements MenuListener, PropertyChangeListener {
        public void menuCanceled(final MenuEvent e) {
        }

        public void menuDeselected(final MenuEvent e) {
            menuItem.repaint();
        }

        public void menuSelected(final MenuEvent e) {
            menuItem.repaint();
        }

        public void propertyChange(final PropertyChangeEvent event) {
            menuItem.revalidate();
            menuItem.repaint();
        }
    }

    private static class OpenPopupAction extends AbstractAction {
        private JMenu menu;

        public void setMenu(final JMenu menu) {
            this.menu = menu;
        }

        public JMenu getMenu() {
            return menu;
        }

        public void openPopup(final JMenu menu) {
            setMenu(menu);
            doOpenPopup();
        }

        public void actionPerformed(final ActionEvent e) {
            doOpenPopup();
        }

        private void doOpenPopup() {
            menu.setPopupMenuVisible(true);
        }
    }

    private static class MnemonicAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            final JMenu menu = (JMenu)e.getSource();
            final MenuElement menuItem = getFirstMenuItem(menu);
            final MenuElement newSelection = (menuItem != null) ? menuItem : menu.getPopupMenu();
            final MenuElement[] path = Utilities.getMenuElementPath(newSelection);
            MenuSelectionManager.defaultManager().setSelectedPath(path);
        }

        private MenuElement getFirstMenuItem(final JMenu menu) {
            final int numElements = menu.getMenuComponentCount();
            for (int i = 0; i < numElements; i++) {
                final Component menuComponent = menu.getMenuComponent(i);
                if (menuComponent.isEnabled() && menuComponent instanceof MenuElement) {
                    return (MenuElement)menuComponent;
                }
            }
            return null;
        }
    }

    private final static OpenPopupAction OPEN_POPUP_ACTION = new OpenPopupAction();
    private final static MnemonicAction MNEMONIC_ACTION = new MnemonicAction();

    protected ChangeListener changeListener;
    protected MenuListener menuListener;
    protected PropertyChangeListener propertyChangeListener;

    private MenuHandler menuHandler;
    
    private static final String PROPERTY_PREFIX = "Menu";


    public static ComponentUI createUI(final JComponent c) {
        return new BasicMenuUI();
    }

    protected ChangeListener createChangeListener(final JComponent c) {
        return null;
    }

    protected MenuListener createMenuListener(final JComponent c) {
        return menuHandler;
    }

    protected PropertyChangeListener createPropertyChangeListener(final JComponent c) {
        return (menuHandler == null) ? new MenuHandler() : menuHandler;
    }

    protected MouseInputListener createMouseInputListener(final JComponent c) {
        return new MouseInputHandler();
    }

    protected void setupPostTimer(final JMenu menu) {
        if (!menu.isSelected()) {
            return;
        }

        if (openMenuTimer == null) {
            openMenuTimer = new Timer(menu.getDelay(), OPEN_POPUP_ACTION);
            openMenuTimer.setRepeats(false);
        } else {
            openMenuTimer.stop();
            openMenuTimer.setInitialDelay(menu.getDelay());
        }
        OPEN_POPUP_ACTION.setMenu(menu);
        openMenuTimer.start();
    }

    public Dimension getMaximumSize(final JComponent c) {
        if (!((JMenu) menuItem).isTopLevelMenu()) {
            return super.getMaximumSize(c);
        }
        Dimension result = c.getPreferredSize();
        result.height = Short.MAX_VALUE;
        return result;
    }

    protected String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

    protected void installDefaults() {
        super.installDefaults();
        LookAndFeel.installProperty(menuItem, "opaque", Boolean.FALSE);
    }

    protected void installKeyboardActions() {
        ActionMap actionMap = new ActionMapUIResource();
        actionMap.put(StringConstants.MNEMONIC_ACTION, MNEMONIC_ACTION);
        actionMap.setParent(((BasicLookAndFeel)UIManager.getLookAndFeel()).getAudioActionMap());
        SwingUtilities.replaceUIActionMap(menuItem, actionMap);
    }

    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIActionMap(menuItem, null);
    }

    protected void installListeners() {
        if (menuItem == null) {
            return;
        }

        final JMenu menu = (JMenu)menuItem;
        menuDragMouseListener = createMenuDragMouseListener(menu);
        menu.addMenuDragMouseListener(menuDragMouseListener);
        mouseInputListener = createMouseInputListener(menu);
        menu.addMouseListener(mouseInputListener);
        menu.addMouseMotionListener(mouseInputListener);
        menuKeyListener = createMenuKeyListener(menu);
        menu.addMenuKeyListener(menuKeyListener);
        propertyChangeListener = createPropertyChangeListener(menuItem);
        menu.addPropertyChangeListener(propertyChangeListener);
        changeListener = createChangeListener(menuItem);
        menu.addChangeListener(changeListener);
        menuListener = createMenuListener(menu);
        menu.addMenuListener(menuListener);
    }

    protected void uninstallListeners() {
        if (menuItem == null) {
            return;
        }

        final JMenu menu = (JMenu)menuItem;
        menu.removeMenuDragMouseListener(menuDragMouseListener);
        menuDragMouseListener = null;
        menu.removeMouseListener(mouseInputListener);
        menu.removeMouseMotionListener(mouseInputListener);
        mouseInputListener = null;
        menu.removeMenuKeyListener(menuKeyListener);
        menuKeyListener = null;
        menu.removePropertyChangeListener(propertyChangeListener);
        propertyChangeListener = null;
        menu.removeChangeListener(changeListener);
        changeListener = null;
        menu.removeMenuListener(menuListener);
        menuListener = null;
    }

    boolean isArrowToBeDrawn() {
        return (menuItem != null) ? super.isArrowToBeDrawn() && !((JMenu)menuItem).isTopLevelMenu() : false;
    }

    boolean isPaintArmed() {
        return menuItem.isArmed() || menuItem.isSelected();
    }
}
