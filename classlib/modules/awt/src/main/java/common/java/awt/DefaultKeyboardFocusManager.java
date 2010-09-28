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

import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.Set;

public class DefaultKeyboardFocusManager extends KeyboardFocusManager {

    private static Window prevFocusedWindow;
    private static Component prevFocusOwner;
    private static Window prevActiveWindow;
    private static boolean consumeKeyTyped;

    private final Toolkit toolkit = Toolkit.getDefaultToolkit();

    static boolean isActivateable(Window w) {
        // return true if activeWindow can be set to w
        return ( (w == null) || w.isActivateable());
    }

    @Override
    protected void dequeueKeyEvents(long a0, Component a1) {
        toolkit.lockAWT();
        try {
            // currently do nothing, this method
            // is never called by AWT implementation
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    protected void discardKeyEvents(Component a0) {
        toolkit.lockAWT();
        try {
            // currently do nothing, this method
            // is never called by AWT implementation
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public boolean dispatchEvent(AWTEvent e) {
        if (e instanceof KeyEvent) {
            KeyEvent ke = (KeyEvent) e;
            return (preProcessKeyEvent(ke) || dispatchKeyEvent(ke));
        } else if (e instanceof FocusEvent) {
            FocusEvent fe = (FocusEvent) e;
            return dispatchFocusEvent(fe);
        } else if (e instanceof WindowEvent) {
            WindowEvent we = (WindowEvent) e;
            return dispatchWindowEvent(we);
        } else if (e == null) {
            throw new NullPointerException();
        }
        return false;
    }

    private boolean preProcessKeyEvent(KeyEvent ke) {
        // first pass event to every key event dispatcher:
        for (KeyEventDispatcher ked : getKeyEventDispatchers()) {
            if (ked.dispatchKeyEvent(ke)) {
                return true;
            }
        }

        return false;
    }

    private boolean dispatchFocusEvent(FocusEvent fe) {
        boolean doRedispatch = false;
        Component comp = fe.getComponent();
        toolkit.lockAWT();
        try {
            Component newFocusOwner = focusOwner;
            switch (fe.getID()) {
            case FocusEvent.FOCUS_GAINED:
                if ((focusedWindow != null) &&
                    (comp.getWindowAncestor() == focusedWindow)) {

                    newFocusOwner = comp;
                }
                break;
            case FocusEvent.FOCUS_LOST:
                // Focus changes in which a Component
                // loses focus to itself must be discarded
                if (comp == fe.getOppositeComponent()) {
                    return true;
                }
                if (focusOwner != null) {
                    Container hwContainer = ((comp instanceof Container) ?
                                             (Container)comp :
                                              null);

                    if ((hwContainer != null) &&
                        hwContainer.isAncestorOf(focusOwner)) {

                        comp = focusOwner;
                        fe.setSource(comp);
                    }

                    if (comp == focusOwner) {
                        newFocusOwner = null;
                        prevFocusOwner = focusOwner;
                    }
                }
                break;
            default:
                return false;
            }
            if (newFocusOwner == focusOwner) {
                return true;
            }
            boolean temp = fe.isTemporary();
            setFocusOwner(newFocusOwner, temp);

            // rejection recovery [if (newFocusOwner != focusOwner)]
            boolean success = (newFocusOwner == getFocusOwner());

            if (success) {
                doRedispatch = true;
            } else {
                recoverFocusOwner(temp);
            }
        } finally {
            toolkit.unlockAWT();
        }
        if (doRedispatch) {
            redispatchEvent(comp, fe);
        }
        return true;
    }

    private boolean recoverFocusOwner(boolean temp) {
        if (focusOwner == null) {
            return true;
        }

        // focus owner will be reset to the Component
        // which was previously the focus owner
        boolean success = prevFocusOwner.requestFocusImpl(temp, true, true);

        // If that is not possible,
        // then it will be reset to the next Component
        // in the focus traversal cycle after the previous focus owner
        if (!success) {
            Container root = prevFocusOwner.getFocusCycleRootAncestor();
            Component newFocusOwner =
                root.getFocusTraversalPolicy().getComponentAfter(root,
                                                                 focusOwner);
            success = ((newFocusOwner != null) &&
                        newFocusOwner.requestFocusImpl(temp, true, true));
        }

        // If that is also not possible,
        // then the KeyboardFocusManager will clear the global focus owner
        if (!success) {
            clearGlobalFocusOwner();
        }
        return success;
    }

    private void setFocusOwner(Component newFocusOwner, boolean temp) {
        if (temp) {
            setGlobalFocusOwner(newFocusOwner);
        } else {
            setGlobalPermanentFocusOwner(newFocusOwner);
        }
    }

    private boolean dispatchWindowEvent(WindowEvent we) {
        Window win = we.getWindow();
        Window opposite = we.getOppositeWindow();
        int id = we.getID();
        switch (id) {
        case WindowEvent.WINDOW_ACTIVATED:
        case WindowEvent.WINDOW_DEACTIVATED:
            processWindowActivation(we);
            return true;

        case WindowEvent.WINDOW_GAINED_FOCUS:


            boolean doRedispatch = false;
            toolkit.lockAWT();
            try {
                // skip events in illegal state(to preserve their order),
                // don't reorder them [as opposed to spec]
                if (win.getFrameDialogOwner() != getGlobalActiveWindow()) {
                    return true;
                }
                if ( ((opposite != null) && (opposite == win)) ||
                     (win == focusedWindow) ) {
                    return true;
                }

                setGlobalFocusedWindow(win);

                // rejection recovery before redispatching
                doRedispatch = (getFocusedWindow() == win);
                if (!doRedispatch) {
                    recoverWindow(prevFocusedWindow);
                }
            } finally {
                toolkit.unlockAWT();
            }
            if (doRedispatch) {
                redispatchEvent(win, we);
            }
            return true;

        case WindowEvent.WINDOW_LOST_FOCUS:
            if ( (focusedWindow != null) && (opposite == focusedWindow)) {
                return true;
            }

            // Events posted by the peer layer claiming
            // that the active Window has lost focus to
            // the focused Window must be discarded
            if ((win == activeWindow) && (focusedWindow != null) &&
                (opposite == focusedWindow)) {
                return true;
            }

            dispatchWindowLostFocus(we, win, opposite);
            return true;

        }
        return false;
    }

    private void dispatchWindowLostFocus(WindowEvent we,
                                         Window win,
                                         Window opposite) {

        boolean doRedispatch = false;
        toolkit.lockAWT();
        try {
            if ((focusedWindow != null) && (win != focusedWindow)) {
                win = focusedWindow;
            }
            we.setSource(win);
            if (win == focusedWindow) {
                // remember last focused window to request focus back if
                // needed
                prevFocusedWindow = focusedWindow;
                setGlobalFocusedWindow(null);

                doRedispatch = true;
            }
        } finally {
            toolkit.unlockAWT();
        }

        if (doRedispatch) {
            redispatchEvent(win, we);
        }
    }

    private boolean recoverWindow(Window prevWindow) {
        // focused Window will be reset to the Window
        // which was previously the focused Window

        if (prevWindow != null) {
            // do the same thing as when native "focus gained" event comes on
            // prevFocusedWindow, but call behavior
            // to generate native event & activate ancestor Frame
            requestFocusInWindow(prevWindow, true);
        } else {
            // If there is no such Window,
            // then the KeyboardFocusManager will clear the global focus owner.
            clearGlobalFocusOwner();
            return false;
        }
        return true;
    }

    private void processWindowActivation(WindowEvent we) {
        boolean active = (we.getID() == WindowEvent.WINDOW_ACTIVATED);
        Window win = we.getWindow();
        win = (isActivateable(win) ? win : win.getFrameDialogOwner());

        // ignore activating already active window
        // & deactivating any not active window
        if (active == (activeWindow == win)) {
            return;
        }

        Window newActiveWindow = (active ? win : null);
        Window oldActiveWindow = activeWindow;
        setGlobalActiveWindow(newActiveWindow);
        if (getGlobalActiveWindow() == newActiveWindow) {
            if (!active) {
                prevActiveWindow = oldActiveWindow;
            }
            we.setSource(win);
            redispatchEvent(win, we);
        } else if (active) {
            // initiate activeWindow recovery if the change was
            // rejected by a vetoable change listener
            // recover only after WINDOW_ACTIVATED

            Window winToRecover = null;
            if ((prevFocusedWindow != null) &&
                (prevFocusedWindow.getFrameDialogOwner() == prevActiveWindow)) {

                winToRecover = prevFocusedWindow;
            }
            recoverWindow(winToRecover);
        }

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        boolean doRedispatch = false;
        toolkit.lockAWT();
        try {
            if ((focusOwner != null) && focusOwner.isKeyEnabled()) {
                doRedispatch = !e.isConsumed();
            }
        } finally {
            toolkit.unlockAWT();
        }

        if (doRedispatch) {
            e.setSource(focusOwner);
            redispatchEvent(focusOwner, e);
        }
        postProcessKeyEvent(e);
        return true; // no further dispatching
    }

    @Override
    public void downFocusCycle(Container aContainer) {
        toolkit.lockAWT();
        try {
            if (aContainer != null) {
                aContainer.transferFocusDownCycle();
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    protected void enqueueKeyEvents(long a0, Component a1) {
        toolkit.lockAWT();
        try {
            // currently do nothing,
            // this method is never called by AWT implementation
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void focusNextComponent(Component aComponent) {
        toolkit.lockAWT();
        try {
            if (aComponent != null) {
                aComponent.transferFocus();
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void focusPreviousComponent(Component aComponent) {
        toolkit.lockAWT();
        try {
            if (aComponent != null) {
                aComponent.transferFocusBackward();
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public boolean postProcessKeyEvent(KeyEvent ke) {
        // pass event to every key event postprocessor:
        for (KeyEventPostProcessor kep : getKeyEventPostProcessors()) {
            if (kep.postProcessKeyEvent(ke)) {
                return true;
            }
        }
        
        // postprocess the event if no KeyEventPostProcessor dispatched it
        if (!ke.isConsumed()) {
            handleShortcut(ke);
        }
        return true;// discard KeyEvents if there's no focus owner

    }

    private void handleShortcut(KeyEvent ke) {
        toolkit.lockAWT();
        try {
            if (MenuShortcut.isShortcut(ke) && (activeWindow instanceof Frame)) {
                MenuBar mb = ((Frame) activeWindow).getMenuBar();
                if (mb != null) {
                    mb.handleShortcut(ke);
                }
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void processKeyEvent(Component focusedComponent, KeyEvent e) {
        toolkit.lockAWT();
        try {
            AWTKeyStroke ks = ((e.getID() == KeyEvent.KEY_TYPED) ?
                                null :
                                AWTKeyStroke.getAWTKeyStrokeForEvent(e));
            Container container = ((focusedComponent instanceof Container) ?
                                   (Container)focusedComponent :
                                   null);
            Set<?> back = focusedComponent.getFocusTraversalKeys(BACKWARD_TRAVERSAL_KEYS);
            Set<?> forward = focusedComponent.getFocusTraversalKeys(FORWARD_TRAVERSAL_KEYS);
            Set<?> up = focusedComponent.getFocusTraversalKeys(UP_CYCLE_TRAVERSAL_KEYS);
            Set<?> down = (((container != null) && container.isFocusCycleRoot()) ?
                        container.getFocusTraversalKeys(DOWN_CYCLE_TRAVERSAL_KEYS) :
                        null);

            // all KeyEvents related to the focus traversal key, including the
            // associated KEY_TYPED event,
            // will be consumed, and will not be dispatched to any Component

            Set<?>[] sets = { back, forward, up, down };
            consume(e, sets);

            if (back.contains(ks)) {
                focusPreviousComponent(focusedComponent);
            } else if (forward.contains(ks)) {
                focusNextComponent(focusedComponent);
            } else if (up.contains(ks)) {
                upFocusCycle(focusedComponent);
            } else if ((down != null) &&
                       (container != null) &&
                        down.contains(ks)) {
                downFocusCycle(container);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @param sets
     * @param e
     * Consumes key event e if any set of focus-traversal keystrokes
     * (i. e. not of type KEY_TYPED) from sets contains a keystroke
     * with same code (or char) & modifiers as e
     */
    private void consume(KeyEvent e, Set<?>[] sets) {
        int keyCode = e.getKeyCode();
        int mod = (e.getModifiersEx() | e.getModifiers());
        boolean codeDefined = (keyCode != KeyEvent.VK_UNDEFINED);
        if (!codeDefined) {
            // consume any KEY_TYPED event after
            // consumed KEY_PRESSED and before
            // any unconsumed KEY_PRESSED or any
            // KEY_RELEASED
            if (consumeKeyTyped) {
                e.consume();
            }
            return;
        }
        for (Set<?> s : sets) {
            if (s != null) {
                AWTKeyStroke[] keys = s.toArray(new AWTKeyStroke[0]);
                for (AWTKeyStroke key : keys) {
                    if ( (key.getKeyCode() == keyCode) &&
                         (key.getModifiers() == mod) ) {
                        e.consume();
                        if (e.getID() == KeyEvent.KEY_PRESSED) {
                            // consume next KEY_TYPED event
                            consumeKeyTyped = true;
                        } else {
                            consumeKeyTyped = false;
                        }

                        return;
                    }
                }
            }
        }
        consumeKeyTyped = false;
    }

    @Override
    public void upFocusCycle(Component aComponent) {
        toolkit.lockAWT();
        try {
            if (aComponent != null) {
                aComponent.transferFocusUpCycle();
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

}


