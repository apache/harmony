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
package org.apache.harmony.awt;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import org.apache.harmony.awt.internal.nls.Messages;

/**
 * ButtonStateController.
 * Changes Component state and fires [action] events in response to
 * user input(key, mouse, focus events) and current state.
 * Repaints component when necessary.
 * Is typically used by Components such as Button, Checkbox, etc
 * as input event listener. Such components also query their state
 * properties, which are not stored in the Component-derived class,
 * for example isPressed(), with state controller.
 */
public abstract class ButtonStateController implements MouseListener, FocusListener, KeyListener {

    private static Component activeComponent;
    
    private boolean mousePressed = false;
    private boolean keyPressed = false;
    private boolean mouseInside = false;
    private boolean focused = false;
    private final Component component;
    
    /**
     * Store input event properties
     * to be able to retrieve them later
     * inside fireEvent() implementation
     * which typically uses them to 
     * fire action/item event
     */
    private long when;
    private int mod;

    public ButtonStateController(Component comp) {
        component = comp;
    }

    public boolean isPressed() {
        return ((mousePressed && mouseInside) || keyPressed);
    }

    public void mousePressed(MouseEvent me) {
        if (mousePressed || keyPressed ||
                (me.getButton() != MouseEvent.BUTTON1) || !lock()) {
            return;
        }

        mousePressed = true;
        
        if (mouseInside) {
            component.repaint();
        }

        if (component.isFocusable()) {
            component.requestFocus();
        }
    }

    public void mouseReleased(MouseEvent me) {
        if (!mousePressed || (me.getButton() != MouseEvent.BUTTON1) || !unlock()) {
            return;
        }

        mousePressed = false;
        component.repaint();
        
        if (mouseInside) {
            when = me.getWhen();
            mod = me.getModifiers();
            fireEvent();
        }
    }

    public void keyPressed(KeyEvent ke) {
        // awt.54=Key event for unfocused component
        assert focused == true : Messages.getString("awt.54"); //$NON-NLS-1$

        if (mousePressed || keyPressed ||
                (ke.getKeyCode() != KeyEvent.VK_SPACE) || !lock()) {
            return;
        }

        keyPressed = true;
        component.repaint();
    }

    public void keyReleased(KeyEvent ke) {
        // awt.54=Key event for unfocused component
        assert focused == true : Messages.getString("awt.54"); //$NON-NLS-1$

        if (!keyPressed || (ke.getKeyCode() != KeyEvent.VK_SPACE) || !unlock()) {
            return;
        }

        keyPressed = false;
        component.repaint();
        when = ke.getWhen();
        mod = ke.getModifiers();
        fireEvent();
    }

    public void mouseEntered(MouseEvent me) {
        // awt.55=Double mouse enter event for component
        assert mouseInside == false : Messages.getString("awt.55"); //$NON-NLS-1$
        mouseCrossed(true);
    }

    public void mouseExited(MouseEvent me) {
        // awt.56=Double mouse exit event for component
        assert mouseInside == true : Messages.getString("awt.56"); //$NON-NLS-1$
        mouseCrossed(false);
    }

    public void focusGained(FocusEvent fe) {
        // awt.57=Double focus gained event for component
        assert focused == false : Messages.getString("awt.57"); //$NON-NLS-1$

        focused = true;
        component.repaint();
    }

    public void focusLost(FocusEvent fe) {
        // awt.58=Double focus lost event for component
        assert focused == true : Messages.getString("awt.58"); //$NON-NLS-1$

        unlock();
        focused = false;
        keyPressed = false;
        mousePressed = false;
        component.repaint();
    }

    //Ignored
    public void keyTyped(KeyEvent ke) {
    }
    public void mouseClicked(MouseEvent me) {
    }

    private void mouseCrossed(boolean inside) {
        mouseInside = inside;
        if (mousePressed && focused) {
            component.repaint();
        }
    }

    public final long getWhen() {
        return when;
    }
    public final int getMod() {
        return mod;
    }

    protected abstract void fireEvent();
    
    /**
     * Acquires the lock for this button. If the lock is acquired no other
     * buttons could be pressed until the lock is released.
     * 
     * @return true if the lock has been successfully acquired, otherwise
     *         returns false.
     */
    private boolean lock() {
        if (activeComponent != null) {
            return false;
        }

        activeComponent = component;
        return true;
    }

    /**
     * Releases the lock.
     * 
     * @return true if the lock has been released, otherwise returns false.
     */
    private boolean unlock() {
        if (activeComponent != component) {
            return false;
        }

        activeComponent = null;
        return true;
    }
}
