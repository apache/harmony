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

import java.lang.reflect.InvocationTargetException;
import javax.swing.BasicSwingTestCase;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.SwingWaitTestCase;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class BasicMenuUI_MultithreadedTest extends BasicSwingTestCase {
    protected BasicMenuUI menuUI;

    private class ConcretePopupListener implements PopupMenuListener {
        public boolean visible;

        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            visible = true;
            synchronized (this) {
                notify();
            }
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        }

        public void popupMenuCanceled(PopupMenuEvent e) {
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        menuUI = new BasicMenuUI();
    }

    @Override
    protected void tearDown() throws Exception {
        menuUI = null;
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.plaf.basic.BasicMenuUI.setupPostTimer(JMenu)'
     */
    @SuppressWarnings("deprecation")
    public void testSetupPostTimer() throws InterruptedException, InvocationTargetException {
        final MenuSelectionManager manager = MenuSelectionManager.defaultManager();
        JFrame frame = new JFrame();
        JMenuBar menuBar = new JMenuBar();
        final JMenu menu = new JMenu("menu");
        menu.setUI(menuUI);
        ConcretePopupListener listener = new ConcretePopupListener();
        menu.getPopupMenu().addPopupMenuListener(listener);
        menu.add(new JMenuItem("item"));
        frame.setJMenuBar(menuBar);
        menuBar.add(menu);
        final JButton button = new JButton();
        frame.getContentPane().add(button);
        frame.pack();
        frame.show();
        SwingWaitTestCase.requestFocusInWindowForComponent(button);
        manager.setSelectedPath(new MenuElement[] { menuBar });
        menu.setDelay(100);
        setupTimerAntWait(menu, menuUI, listener);
        assertFalse(listener.visible);
        manager.setSelectedPath(new MenuElement[] { menuBar, menu });
        setupTimerAntWait(menu, menuUI, listener);
        assertTrue(listener.visible);
        manager.setSelectedPath(new MenuElement[] { menuBar });
        manager.setSelectedPath(new MenuElement[] { menuBar, menu });
        menu.setDelay(10000);
        setupTimerAntWait(menu, menuUI, listener);
        assertFalse(listener.visible);
    }

    private void setupTimerAntWait(final JMenu menu, final BasicMenuUI ui,
            final ConcretePopupListener listener) throws InterruptedException,
            InvocationTargetException {
        synchronized (listener) {
            listener.visible = false;
        }
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                ui.setupPostTimer(menu);
            }
        });
        synchronized (listener) {
            listener.wait(1000);
        }
    }
}
