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
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.DefaultFocusManager;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JRootPane;
import javax.swing.LookAndFeel;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.MenuBarUI;

import org.apache.harmony.x.swing.Utilities;


public class BasicMenuBarUI extends MenuBarUI {

    private static class FocusAction extends AbstractAction implements WindowListener, ComponentListener {
        public static final String KEY = "takeFocus";

        private final ArrayList menuBars = new ArrayList();

        public void addMenuBar(final JMenuBar menuBar) {
            if (menuBars.contains(menuBar)) {
                menuBars.remove(menuBar);
            }
            menuBars.add(menuBar);

            Window w = SwingUtilities.getWindowAncestor(menuBar);
            if (w != null) {
                w.addWindowListener(this);
                w.addComponentListener(this);
            }
        }

        public void removeMenuBar(final JMenuBar menuBar) {
            menuBars.remove(menuBar);

            Window w = SwingUtilities.getWindowAncestor(menuBar);
            if (w != null) {
                w.removeWindowListener(this);
                w.addComponentListener(this);
            }
        }

        public void actionPerformed(final ActionEvent e) {
            Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            JMenuBar menuBar = null;
            final JRootPane rootPane = SwingUtilities.getRootPane(c);
            if (rootPane != null) {
                menuBar = rootPane.getJMenuBar();
            }
            if (menuBar == null) {
                menuBar = getActiveMenuBar();
            }
            if (menuBar == null || menuBar.getMenuCount() == 0) {
                return;
            }
            menuBar.getMenu(0).doClick(0);
        }

        private JMenuBar getActiveMenuBar() {
            for (int i = menuBars.size() - 1; i >= 0; i--) {
                final JMenuBar menuBar = (JMenuBar)menuBars.get(i);
                if (menuBar.isShowing()) {
                    return menuBar;
                }
            }
            return null;
        }

        public void windowClosed(final WindowEvent e) {
            MenuSelectionManager.defaultManager().clearSelectedPath();
        }

        public void componentMoved(final ComponentEvent e) {
            MenuSelectionManager.defaultManager().clearSelectedPath();
        }

        public void componentResized(final ComponentEvent e) {
            MenuSelectionManager.defaultManager().clearSelectedPath();
        }

        public void windowIconified(final WindowEvent e) {
            MenuSelectionManager.defaultManager().clearSelectedPath();
        }

        public void windowDeactivated(final WindowEvent e) {
        }

        public void windowDeiconified(final WindowEvent e) {
        }

        public void windowOpened(final WindowEvent e) {
        }

        public void windowActivated(final WindowEvent e) {
        }

        public void windowClosing(final WindowEvent e) {
        }

        public void componentHidden(final ComponentEvent e) {
        }

        public void componentShown(final ComponentEvent e) {
        }
    }

    private class ChangeContainerHandler implements ChangeListener,
                                         ContainerListener, AncestorListener {
        public void ancestorAdded(final AncestorEvent e) {
            FOCUS_ACTION.addMenuBar((JMenuBar)e.getComponent());
            MenuKeyBindingProcessor.attach();
        }

        public void ancestorRemoved(final AncestorEvent e) {
            MenuKeyBindingProcessor.detach();
            FOCUS_ACTION.removeMenuBar((JMenuBar)e.getComponent());
        }

        public void stateChanged(final ChangeEvent e) {
        }

        public void componentAdded(final ContainerEvent e) {
        }

        public void componentRemoved(final ContainerEvent e) {
        }

        public void ancestorMoved(final AncestorEvent e) {
        }
    }

    private static final String PROPERTY_PREFIX = "MenuBar.";

    private static final FocusAction FOCUS_ACTION = new FocusAction();

    protected ChangeListener changeListener;
    protected ContainerListener containerListener;
    protected JMenuBar menuBar;

    private ChangeContainerHandler handler;
    private MenuKeyBindingProcessor menuKeyBindingProcessor;

    static {
        DefaultFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(new AcceleratorsProcessor());
    }

    public static ComponentUI createUI(final JComponent c) {
        return new BasicMenuBarUI();
    }

    public void installUI(final JComponent c) {
        menuBar = (JMenuBar)c;
        if (Utilities.isUIResource(menuBar.getLayout())) {
            menuBar.setLayout(new DefaultMenuLayout(menuBar, BoxLayout.X_AXIS));
        }
        installDefaults();
        installKeyboardActions();
        installListeners();
    }

    public void uninstallUI(final JComponent c) {
        uninstallListeners();
        uninstallKeyboardActions();
        uninstallDefaults();
        menuBar = null;
    }

    protected ChangeListener createChangeListener() {
        return (handler != null) ? handler : (handler = new ChangeContainerHandler());
    }

    protected ContainerListener createContainerListener() {
        return (handler != null) ? handler : (handler = new ChangeContainerHandler());
    }

    protected void installDefaults() {
        LookAndFeel.installColorsAndFont(menuBar, PROPERTY_PREFIX + "background",
                                         PROPERTY_PREFIX + "foreground",
                                         PROPERTY_PREFIX + "font");
        LookAndFeel.installBorder(menuBar, PROPERTY_PREFIX + "border");
        LookAndFeel.installProperty(menuBar, "opaque", Boolean.TRUE);
    }

    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(menuBar);
    }

    protected void installKeyboardActions() {
        ActionMap actionMap = new ActionMapUIResource();
        actionMap.put(FocusAction.KEY, FOCUS_ACTION);
        SwingUtilities.replaceUIActionMap(menuBar, actionMap);

        SwingUtilities.replaceUIInputMap(menuBar, JComponent.WHEN_IN_FOCUSED_WINDOW,
                                         LookAndFeel.makeComponentInputMap(menuBar, (Object[])UIManager.get(PROPERTY_PREFIX + "windowBindings")));
    }

    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIInputMap(menuBar, JComponent.WHEN_IN_FOCUSED_WINDOW, null);
        SwingUtilities.replaceUIActionMap(menuBar, null);
    }

    protected void installListeners() {
        changeListener = createChangeListener();
        containerListener = createContainerListener();
        menuBar.addContainerListener(containerListener);
        menuBar.addAncestorListener(handler);
    }

    protected void uninstallListeners() {
        menuBar.removeAncestorListener(handler);
        menuBar.removeContainerListener(containerListener);
        changeListener = null;
        containerListener = null;
    }

}

