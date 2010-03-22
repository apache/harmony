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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.harmony.x.swing.Utilities;


class RootPaneFocusHandler implements ChangeListener {
    private static RootPaneFocusHandler sharedInstance;
    private static int numInstallations;
    private static Component previousFocusOwner;
    private static Component focusedRootPane;

    private KeyListener rootPaneKeyListener;

    private RootPaneFocusHandler() {
    }

    public static void attach() {
        if (sharedInstance == null) {
            sharedInstance = new RootPaneFocusHandler();
        }
        if (numInstallations == 0) {
            MenuSelectionManager.defaultManager().addChangeListener(sharedInstance);
        }
        numInstallations++;
    }

    public static void detach() {
        if (numInstallations == 1) {
            MenuSelectionManager.defaultManager().removeChangeListener(sharedInstance);
        }
        if (numInstallations > 0) {
            numInstallations--;
        }
    }

    public void stateChanged(final ChangeEvent e) {
        final MenuElement[] path = MenuSelectionManager.defaultManager().getSelectedPath();
        if (Utilities.isEmptyArray(path)) {
            if (focusedRootPane != null) {
                returnFocus();
            }
        } else {
            if (focusedRootPane == null && Utilities.isValidFirstPathElement(path[0])) {
                grabFocus(path);
            }
        }
    }

    private void grabFocus(final MenuElement[] path) {
        for (int i = 0; i < path.length; i++) {
            final MenuElement item = path[i];
            if (i > 0 && !(item instanceof Component)) {
                continue;
            }
            final JRootPane pane = SwingUtilities.getRootPane(getRootPaneChild(item));
            if (pane != null) {
                Component focusOwner = getFocusManager().getFocusOwner();
                if (pane.requestFocus(true)) {
                    previousFocusOwner = focusOwner;
                    focusedRootPane = pane;
                    MenuKeyBindingProcessor.attach();
                    if (pane.getJMenuBar() == null) {
                        pane.addKeyListener(createRootPaneKeyListener());
                    }
                }
                break;
            }
        }
    }

    private KeyListener createRootPaneKeyListener() {
        return (rootPaneKeyListener != null) ? rootPaneKeyListener : (rootPaneKeyListener = new KeyListener() {
            public void keyPressed(KeyEvent event) {
                MenuSelectionManager.defaultManager().processKeyEvent(event);
            }

            public void keyReleased(KeyEvent event) {
                MenuSelectionManager.defaultManager().processKeyEvent(event);
            }

            public void keyTyped(KeyEvent event) {
                MenuSelectionManager.defaultManager().processKeyEvent(event);
            }
        });
    }

    private Component getRootPaneChild(final MenuElement item) {
        return (item instanceof JPopupMenu) ? ((JPopupMenu)item).getInvoker() : (Component)item;
    }

    private void returnFocus() {
        if (isValidPreviusFocusOwner()) {
            previousFocusOwner.requestFocusInWindow();
        } else {
            getFocusManager().focusNextComponent();
        }
        focusedRootPane.removeKeyListener(rootPaneKeyListener);
        focusedRootPane = null;
        MenuKeyBindingProcessor.detach();
    }

    private KeyboardFocusManager getFocusManager() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager();
    }

    private boolean isValidPreviusFocusOwner() {
        return previousFocusOwner != null && previousFocusOwner.isFocusable() && previousFocusOwner.isVisible() && previousFocusOwner.isEnabled();
    }
}
