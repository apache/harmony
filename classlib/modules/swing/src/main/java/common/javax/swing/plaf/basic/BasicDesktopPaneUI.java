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
 * @author Vadim L. Bogdanov
 */

package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultDesktopManager;
import javax.swing.DesktopManager;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.KeyStroke;
import javax.swing.SortingFocusTraversalPolicy;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DesktopPaneUI;
import javax.swing.plaf.UIResource;

import org.apache.harmony.x.swing.Utilities;


public class BasicDesktopPaneUI extends DesktopPaneUI {

    public static ComponentUI createUI(final JComponent c) {
        return new BasicDesktopPaneUI();
    }

    protected class CloseAction extends AbstractAction {
        protected CloseAction() {
        }

        public void actionPerformed(final ActionEvent e) {
            try {
                desktop.getSelectedFrame().setClosed(true);
            } catch (PropertyVetoException e1) {
            }
        }

        public boolean isEnabled() {
            return desktop.getSelectedFrame() != null
                && desktop.getSelectedFrame().isClosable();
        }
    }

    protected class MaximizeAction extends AbstractAction {
        protected MaximizeAction() {
        }

        public void actionPerformed(final ActionEvent e) {
            try {
                desktop.getSelectedFrame().setMaximum(true);
            } catch (PropertyVetoException e1) {
            }
        }

        public boolean isEnabled() {
            return desktop.getSelectedFrame() != null
                && desktop.getSelectedFrame().isMaximizable();
        }
    }

    protected class MinimizeAction extends AbstractAction {
        protected MinimizeAction() {
        }

        public void actionPerformed(final ActionEvent e) {
            try {
                desktop.getSelectedFrame().setIcon(true);
            } catch (PropertyVetoException e1) {
            }
        }

        public boolean isEnabled() {
            return desktop.getSelectedFrame() != null
                && desktop.getSelectedFrame().isIconifiable();
        }
    }

    /**
     * Implements <code>"navigateNext", "navigatePrevious"</code> actions.
     */
    protected class NavigateAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            Container ancestor = desktop.getFocusCycleRootAncestor();
            if (ancestor == null) {
                return;
            }

            FocusTraversalPolicy policy = ancestor.getFocusTraversalPolicy();
            if (!(policy instanceof SortingFocusTraversalPolicy)) {
                return;
            }

            SortingFocusTraversalPolicy sortingPolicy = (SortingFocusTraversalPolicy)policy;

            boolean implicitEnabled = sortingPolicy.getImplicitDownCycleTraversal();
            sortingPolicy.setImplicitDownCycleTraversal(false);

            Component result = null;
            String action = (String)getValue(NAME);
            if ("navigateNext".equals(action)) {
                result = policy.getComponentAfter(ancestor, desktop);
            } else if ("navigatePrevious".equals(action)) {
                result = policy.getComponentBefore(ancestor, desktop);
            }

            sortingPolicy.setImplicitDownCycleTraversal(implicitEnabled);

            if (result != null) {
                result.requestFocus();
            }
        }

        public boolean isEnabled() {
            return true;
        }
    }

    /**
     * Restores the selected frame from iconified or maximized states
     * if it is not <code>null</code>.
     */
    protected class OpenAction extends AbstractAction {
        protected OpenAction() {
        }

        public void actionPerformed(final ActionEvent e) {
            try {
                if (desktop.getSelectedFrame().isMaximum()
                        && desktop.getSelectedFrame().isMaximizable()) {
                    desktop.getSelectedFrame().setMaximum(false);
                } else if (desktop.getSelectedFrame().isIcon()
                        && desktop.getSelectedFrame().isIconifiable()) {
                    desktop.getSelectedFrame().setIcon(false);
                }
            } catch (PropertyVetoException e1) {
            }
        }

        public boolean isEnabled() {
            return desktop.getSelectedFrame() != null
                /*&& desktop.getSelectedFrame().isMaximizable()*/;
        }
    }

    /**
     * Implements drag/move operations of the desktop icon.
     */
    private final class BoundsChangeAction extends AbstractAction {
        private static final int BOUNDS_STEP = 10;

        private String command;

        public BoundsChangeAction(final String command) {
            this.command = command;
        }

        public void actionPerformed(final ActionEvent e) {
            if (desktop.getSelectedFrame() == null) {
                return;
            }

            if ("move".equals(command)) {
                frameOperation = DRAGGING;
                moveFocusToFrame();

            } else if ("escape".equals(command)) {
                frameOperation = NONE;
                restoreFocusOwner();

            } else if ("resize".equals(command)) {
                if (desktop.getSelectedFrame().isResizable()) {
                    frameOperation = RESIZING;
                    moveFocusToFrame();
                }

            } else if (frameOperation != NONE) {
                doBoundsChange();
            }
        }

        public boolean isEnabled() {
            return desktop.getSelectedFrame() != null
                   && !desktop.getSelectedFrame().isMaximum();
        }

        private JComponent getComponentForChangingBounds() {
            JComponent comp;
            if (desktop.getSelectedFrame().isIcon()) {
                comp = desktop.getSelectedFrame().getDesktopIcon();
            } else {
                comp = desktop.getSelectedFrame();
            }
            return comp;
        }

        private void doBoundsChange() {
            Rectangle delta = new Rectangle();

            JComponent comp = getComponentForChangingBounds();
            Dimension minSize = comp.getMinimumSize();
            int shrinkX = Math.min(BOUNDS_STEP,
                                   comp.getWidth() - minSize.width);
            int shrinkY = Math.min(BOUNDS_STEP,
                                   comp.getHeight() - minSize.height);

            if ("right".equals(command)) {
                if (frameOperation == DRAGGING) {
                    delta.x = BOUNDS_STEP;
                } else {
                    delta.width = BOUNDS_STEP;
                }
            } else if ("shrinkRight".equals(command)) {
                if (frameOperation == RESIZING) {
                    delta.x = shrinkX;
                    delta.width = -shrinkX;
                }
            } else if ("left".equals(command)) {
                if (frameOperation == RESIZING) {
                    delta.width = BOUNDS_STEP;
                }
                delta.x = -BOUNDS_STEP;
            } else if ("shrinkLeft".equals(command)) {
                if (frameOperation == RESIZING) {
                    delta.width = -shrinkX;
                }
            } else if ("up".equals(command)) {
                delta.y = -BOUNDS_STEP;
                if (frameOperation == RESIZING) {
                    delta.height = BOUNDS_STEP;
                }
            } else if ("shrinkUp".equals(command)) {
                if (frameOperation == RESIZING) {
                    delta.height = -shrinkY;
                }
            } else if ("down".equals(command)) {
                if (frameOperation == RESIZING) {
                    delta.height = BOUNDS_STEP;
                } else {
                    delta.y = BOUNDS_STEP;
                }
            } else if ("shrinkDown".equals(command)) {
                if (frameOperation == RESIZING) {
                    delta.y = shrinkY;
                    delta.height = -shrinkY;
                }
            }

            changeSelectedFrameBounds(comp, delta);
        }

        private void changeSelectedFrameBounds(final JComponent comp,
                                               final Rectangle delta) {
            comp.setBounds(comp.getX() + delta.x,
                           comp.getY() + delta.y,
                           comp.getWidth() + delta.width,
                           comp.getHeight() + delta.height);
        }

        private void moveFocusToFrame() {
            saveFocusOwner();
            desktop.getSelectedFrame().requestFocusInWindow();
        }

        private void saveFocusOwner() {
            savedFocusOwner = KeyboardFocusManager
                    .getCurrentKeyboardFocusManager().getFocusOwner();
        }

        private void restoreFocusOwner() {
            if (savedFocusOwner != null
                    && desktop.getSelectedFrame().isAncestorOf(savedFocusOwner)) {
                savedFocusOwner.requestFocusInWindow();
            }
        }
    }

    private class SelectFrameAction extends AbstractAction {
        private Comparator comparator;

        public SelectFrameAction() {
            comparator = new Comparator() {
                public int compare(final Object o1, final Object o2) {
                    return System.identityHashCode(o2)
                        - System.identityHashCode(o1);
                }
            };
        }

        public void actionPerformed(final ActionEvent e) {
            String action = (String)getValue(NAME);
            JInternalFrame[] frames = getInternalFramesSorted();
            int index = Arrays.binarySearch(frames,
                                            desktop.getSelectedFrame(),
                                            comparator);

            if ("selectNextFrame".equals(action)) {
                index = (index + 1) % frames.length;
            } else if ("selectPreviousFrame".equals(action)) {
                index = (index + frames.length - 1) % frames.length;
            }

            try {
                frames[index].setSelected(true);
            } catch (PropertyVetoException e1) {
            }
        }

        private JInternalFrame[] getInternalFramesSorted() {
            JInternalFrame[] frames = desktop.getAllFrames();
            Arrays.sort(frames, comparator);
            return frames;
        }

        public boolean isEnabled() {
            return true;
        }
    }

    private class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent e) {
            if ("desktopManager".equals(e.getPropertyName())) {
                if (desktop.getDesktopManager() == null) {
                    installDesktopManager();
                }
            }
        }
    }

    /**
     * @deprecated
     */
    protected KeyStroke closeKey;

    protected JDesktopPane desktop;

    protected DesktopManager desktopManager;

    /**
     * @deprecated
     */
    protected KeyStroke maximizeKey;

    /**
     * @deprecated
     */
    protected KeyStroke minimizeKey;

    /**
     * @deprecated
     */
    protected KeyStroke navigateKey;

    /**
     * @deprecated
     */
    protected KeyStroke navigateKey2;

    private ActionMap actionMap;
    private DesktopManager oldDesktopManager;

    // these fields are also used in BasicInternalFrameTitlePane
    static final int NONE = 0;
    static final int DRAGGING = 1;
    static final int RESIZING = 2;
    int frameOperation = NONE;

    private Component savedFocusOwner;

    private PropertyChangeListener propertyChangeListener;

    public Dimension getMaximumSize(final JComponent c) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public Dimension getMinimumSize(final JComponent c) {
        return new Dimension(0, 0);
    }

    public Dimension getPreferredSize(final JComponent c) {
        return null;
    }

    protected void installDefaults() {
        if (Utilities.isUIResource(desktop.getBackground())) {
            desktop.setBackground(UIManager.getColor("Desktop.background"));
        }
    }

    protected void uninstallDefaults() {
        if (desktop.getBackground() instanceof UIResource) {
            desktop.setBackground(null);
        }
    }

    protected void installDesktopManager() {
        oldDesktopManager = desktop.getDesktopManager();
        if (desktop.getDesktopManager() != null) {
            desktopManager = oldDesktopManager;
        } else {
            if (desktopManager == null) {
                desktopManager = new DefaultDesktopManager();
            }
            desktop.setDesktopManager(desktopManager);
        }
    }

    protected void uninstallDesktopManager() {
        if (desktop.getDesktopManager() == desktopManager
                || desktop.getDesktopManager() == null) {
            desktop.setDesktopManager(oldDesktopManager);
        }
    }

    private ActionMap getActionMap() {
        if (actionMap == null) {
            actionMap = new ActionMapUIResource();
            actionMap.put("restore", new OpenAction());           // ctrl F5
            actionMap.put("close", new CloseAction());            // ctrl F4
            actionMap.put("move", new BoundsChangeAction("move")); // ctrl F7
            actionMap.put("resize",
                          new BoundsChangeAction("resize"));      // ctrl F8
            actionMap.put("right",
                          new BoundsChangeAction("right"));       // "RIGHT"
            actionMap.put("shrinkRight",
                          new BoundsChangeAction("shrinkRight")); // shift RIGHT
            actionMap.put("left",
                          new BoundsChangeAction("left"));        // LEFT
            actionMap.put("shrinkLeft",
                          new BoundsChangeAction("shrinkLeft"));  // shift LEFT
            actionMap.put("up", new BoundsChangeAction("up"));    // UP
            actionMap.put("shrinkUp",
                          new BoundsChangeAction("shrinkUp"));    // shift UP
            actionMap.put("down",
                          new BoundsChangeAction("down"));        // DOWN
            actionMap.put("shrinkDown",
                          new BoundsChangeAction("shrinkDown"));  // shift DOWN
            actionMap.put("escape",
                          new BoundsChangeAction("escape"));      // ESCAPE
            actionMap.put("minimize", new MinimizeAction());      // ctrl F9
            actionMap.put("maximize", new MaximizeAction());      // ctrl F10

            putActionToActionMap("selectNextFrame",
                                 new SelectFrameAction());  // ctrl F6
            putActionToActionMap("selectPreviousFrame",
                                 new SelectFrameAction());  // shift ctrl alt F6
            putActionToActionMap("navigateNext",
                                 new NavigateAction());  // ctrl F12
            putActionToActionMap("navigatePrevious",
                                 new NavigateAction());  // shift ctrl F12
        }

        return actionMap;
    }

    private void putActionToActionMap(final String actionName,
                                      final AbstractAction action) {
        action.putValue(AbstractAction.NAME, actionName);
        actionMap.put(actionName, action);
    }

    protected void installKeyboardActions() {
        SwingUtilities.replaceUIActionMap(desktop, getActionMap());

        SwingUtilities.replaceUIInputMap(desktop,
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
            (InputMap)UIManager.get("Desktop.ancestorInputMap"));
    }

    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIActionMap(desktop, null);

        SwingUtilities.replaceUIInputMap(desktop,
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
            null);
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        if (propertyChangeListener != null) {
            desktop.addPropertyChangeListener(propertyChangeListener);
        }
    }

    protected void uninstallListeners() {
        if (propertyChangeListener != null) {
            desktop.removePropertyChangeListener(propertyChangeListener);
        }
    }

    public void installUI(final JComponent c) {
        desktop = (JDesktopPane) c;

        installDefaults();
        installListeners();
        installKeyboardActions();
        installDesktopManager();
    }

    public void uninstallUI(final JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();
        uninstallDesktopManager();
    }

    public void paint(final Graphics g, final JComponent c) {
        // no need to paint the background because
        // it's already painted in ComponentUI.update()
    }

    protected void registerKeyboardActions() {
        // nothing to do
    }

    protected void unregisterKeyboardActions() {
        // nothing to do
    }
}
