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
 * @author Dmitry A. Durnev
 */
package java.awt;

import java.awt.event.FocusEvent;

import org.apache.harmony.awt.wtk.NativeEvent;
import org.apache.harmony.awt.wtk.NativeWindow;


/**
 * "internal" focus manager
 * Take decoded native focus events & generate
 * WINDOW_GAINED_FOCUS, WINDOW_LOST_FOCUS if necessary.
 * Move focus to focus Proxy to simulate "active" Frame/Dialog
 * when a non-activateable Window gains focus.
 * Interact with current KeyboardFocusManager: query focused, active
 * window, and post all Focus(Window)Events to EventQueue
 */

class FocusDispatcher {
    private Window nativeFocusedWindow;
    private final Toolkit toolkit;

    FocusDispatcher(Toolkit toolkit) {
        this.toolkit = toolkit;
    }

    boolean dispatch(Component src, NativeEvent event) {
        int id = event.getEventId();
        long opositeId = event.getOtherWindowId();
        long srcId = event.getWindowId();
        boolean focusGained = (id == FocusEvent.FOCUS_GAINED);

        Window focusProxyOwner = null;
        if (src == null) {
            focusProxyOwner = getFocusProxyOwner(srcId);
            if (focusProxyOwner == null) {
                return false;
            }
        }
        Component opposite = getComponentById(opositeId);
        Window oppositeFocusProxyOwner = null;
        if (opposite == null) {
            oppositeFocusProxyOwner = getFocusProxyOwner(opositeId);
        }

        dispatchFocusEvent(focusGained, src, opposite,
                          focusProxyOwner, oppositeFocusProxyOwner);
        return false;
    }

    private Window getFocusProxyOwner(long id) {
        return toolkit.getFocusProxyOwnerById(id);
    }

    private Component getComponentById(long srcId) {
        return ((srcId != 0) ? toolkit.getComponentById(srcId) : null);
    }

    boolean dispatchFocusEvent(boolean focusGained,
                               Component comp, Component oppositeComp,
                               Window focusProxyOwner,
                               Window oppositeFocusProxyOwner) {

        Component other = oppositeComp;
        Window wnd = getWindowAncestor(comp, focusProxyOwner);
        Window oppositeWnd = getWindowAncestor(other, oppositeFocusProxyOwner);
        if (focusGained) {
            nativeFocusedWindow = wnd;
        } else if (wnd == nativeFocusedWindow) {
            nativeFocusedWindow = null;
        }

        boolean isFocusProxy = (focusProxyOwner != null);
        boolean isOppositeFocusProxy = (oppositeFocusProxyOwner != null);

        if (discardFocusProxyEvent(focusGained,
                                   isFocusProxy,
                                   isOppositeFocusProxy,
                                   wnd)) {
            return true;
        }

        // if a non-Frame/Dialog gains native focus, transfer focus
        // to "focus proxy" in a nearest Frame/Dialog to make it look "active"
        if (!DefaultKeyboardFocusManager.isActivateable(wnd) &&
            (wnd != oppositeWnd) && focusGained)
        {

            activateAncestor(wnd);
        }

        return dispatchToKFM(focusGained,
                             wnd, oppositeWnd,
                             isFocusProxy, isOppositeFocusProxy, other);
    }

    private Window getWindowAncestor(Component comp, Window proxyOwner) {
        if (comp != null) {
            return comp.getWindowAncestor();
        }
        return proxyOwner;
    }

    private boolean dispatchToKFM(boolean focusGained,
                                  Window wnd, Window oppositeWnd,
                                  boolean isFocusProxy,
                                  boolean isOppositeFocusProxy,
                                  Component other) {
        KeyboardFocusManager kfm =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Component focusOwner = KeyboardFocusManager.actualFocusOwner;

        // change wnd/oppositeWnd to Java focused window
        // if focusProxy is losing focus:

        Window focusedWindow = KeyboardFocusManager.actualFocusedWindow;

        if (!focusGained && isFocusProxy &&
            (focusedWindow != null)) {
            // discard event when focus proxy
            // is losing focus to focused Window
            if (oppositeWnd == focusedWindow) {
                return true;
            }
            wnd = focusedWindow;
        }

        if (focusGained && isOppositeFocusProxy) {
            oppositeWnd = focusedWindow;
            other = focusOwner;
        }

        // if focus goes out of our app there's no requestFocus() call, so
        // deliver native event to KFM:
        if (!focusGained && (other == null)) {
            kfm.setFocus(focusOwner, focusedWindow, false, other, true, false);
        }
        if (focusGained && (wnd != oppositeWnd)) {
            // set focus to the appropriate child component:
            // contrary to focus spec KeyboardFocusManager doesn't
            // have to do it
            // [don't call behavior here to avoid endless loop]
            kfm.requestFocusInWindow(wnd, false);
        }
        return true;
    }

    /**
     * Discard some focus events where focusProxy is source or opposite
     * @param focusGained
     * @param isFocusProxy
     * @param isOppositeFocusProxy
     * @param wnd
     * @return true if event should be discarded
     */
    private boolean discardFocusProxyEvent(boolean focusGained,
                                           boolean isFocusProxy,
                                           boolean isOppositeFocusProxy,
                                           Window wnd) {
        if (!focusGained && isOppositeFocusProxy) {
            return true;
        }
        if (focusGained && isFocusProxy) {
            return true;
        }
        return false;
    }

    /**
     *  Try to activate nearest Dialog/Frame[if not already active]
     *   by setting native focus to focusProxy[dedicated child of Dialog/Frame]
     */
    private void activateAncestor(Window wnd) {

        Window decorWnd = wnd.getFrameDialogOwner();
        if ((decorWnd != null) && (decorWnd != nativeFocusedWindow)) {
            NativeWindow nativeWnd = decorWnd.getNativeWindow();
            if (nativeWnd != null) {
                NativeWindow focusProxyWnd = decorWnd.getFocusProxy();

                if ((focusProxyWnd != null)) {
                    focusProxyWnd.setFocus(true);
                }
            }
        }
    }

}
