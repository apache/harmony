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
import java.awt.KeyEventPostProcessor;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.apache.harmony.x.swing.StringConstants;


class AcceleratorsProcessor implements KeyEventPostProcessor {

    public boolean postProcessKeyEvent(final KeyEvent event) {
        if (event.isConsumed()) {
            return false;
        }

        final Component source = (Component)event.getSource();
        final KeyStroke ks = KeyStroke.getKeyStrokeForEvent(event);

        JRootPane parent = SwingUtilities.getRootPane(source);
        while (parent != null) {
            final JMenuBar menu = parent.getJMenuBar();
            if (menu != null && processMenuAccelerator(menu, ks, event)) {
                return true;
            }
            parent = (JRootPane)SwingUtilities.getAncestorOfClass(JRootPane.class, parent);
        }

        return false;
    }

    private boolean processMenuAccelerator(final JMenuBar menu, final KeyStroke ks, final KeyEvent event) {
        final int menuCount = menu.getMenuCount();
        for (int i = 0; i < menuCount; i++) {
            final JMenu child = menu.getMenu(i);
            if (child != null && processMenuAccelerator(child, ks, event)) {
                return true;
            }
        }
        return false;
    }

    private boolean processMenuAccelerator(final JMenu menu, final KeyStroke ks, final KeyEvent event) {
        final int menuCount = menu.getMenuComponentCount();
        for (int i = 0; i < menuCount; i++) {
            final Component child = menu.getMenuComponent(i);
            if (child instanceof JMenu) {
                if (processMenuAccelerator((JMenu)child, ks, event) ){
                    return true;
                }
            } else if (child instanceof JMenuItem) {
                final JMenuItem menuItem = (JMenuItem)child;
                if (ks.equals(menuItem.getAccelerator())) {
                    final Action action = menuItem.getActionMap().get(StringConstants.MNEMONIC_ACTION);
                    if (action != null) {
                        SwingUtilities.notifyAction(action, ks, event, menuItem, event.getModifiersEx());
                        return true;
                    }
                }
            }
        }
        return false;
    }

}

