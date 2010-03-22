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

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ActionMapUIResource;

import org.apache.harmony.x.swing.StringConstants;


public class BasicButtonListener implements MouseListener, MouseMotionListener, FocusListener, ChangeListener, PropertyChangeListener {

    private static final class PressButtonAction extends AbstractAction {
        public static void press(final AbstractButton button) {
            if (button.isEnabled()) {
                final ButtonModel model = button.getModel();
                model.setArmed(true);
                model.setPressed(true);
                button.requestFocusInWindow();
            }
        }

        public void actionPerformed(final ActionEvent event) {
            press((AbstractButton)event.getSource());
        }
    };

    private static final class ReleaseButtonAction extends AbstractAction {
        public static void release(final AbstractButton button) {
            if (button.isEnabled()) {
                final ButtonModel model = button.getModel();
                model.setPressed(false);
                model.setArmed(false);
            }
        }

        public void actionPerformed(final ActionEvent event) {
            release((AbstractButton)event.getSource());
        }
    };

    private static final class MnemonicAction extends AbstractAction {
        public void actionPerformed(final ActionEvent event) {
            final AbstractButton button = (AbstractButton)event.getSource();
            if (button.isEnabled()) {
                button.requestFocusInWindow();
                button.doClick();
            }
        }
    };

    private static final Action PRESS_ACTION = new PressButtonAction();
    private static final Action RELEASE_ACTION = new ReleaseButtonAction();
    private static final Action MNEMONIC_ACTION = new MnemonicAction();

    private long previousPressTime;
    private final AbstractButton button;

    public BasicButtonListener(final AbstractButton button) {
        this.button = button;
    }

    public void stateChanged(final ChangeEvent event) {
        button.repaint();
    }

    public void propertyChange(final PropertyChangeEvent event) {
        if (AbstractButton.CONTENT_AREA_FILLED_CHANGED_PROPERTY.equals(event.getPropertyName())) {
            LookAndFeel.installProperty(button, StringConstants.OPAQUE_PROPERTY, event.getNewValue());
        }

        button.revalidate();
        button.repaint();
    }

    public void uninstallKeyboardActions(final JComponent c) {
        SwingUtilities.replaceUIActionMap(c, null);
    }

    public void installKeyboardActions(final JComponent c) {
        ActionMap actionMap = new ActionMapUIResource();
        actionMap.put(StringConstants.BUTTON_PRESSED_ACTION, PRESS_ACTION);
        actionMap.put(StringConstants.BUTTON_RELEASED_ACTION, RELEASE_ACTION);
        actionMap.put(StringConstants.MNEMONIC_ACTION, MNEMONIC_ACTION);
        SwingUtilities.replaceUIActionMap(c, actionMap);
    }

    public void mouseReleased(final MouseEvent event) {
        if (event.getButton() == MouseEvent.BUTTON1) {
            ReleaseButtonAction.release(button);
        }
    }

    public void mousePressed(final MouseEvent event) {
        if ((event.getButton() == MouseEvent.BUTTON1) && mouseInside(event) && isMultiClickTimePassed(event)) {
            PressButtonAction.press(button);
        }
        previousPressTime = event.getWhen();
    }

    public void mouseExited(final MouseEvent event) {
        final ButtonModel model = button.getModel();
        if (model.isEnabled()) {
            if (button.isRolloverEnabled()) {
                model.setRollover(false);
            }
            model.setArmed(false);
        }
    }

    public void mouseEntered(final MouseEvent event) {
        final ButtonModel model = button.getModel();
        if (button.isRolloverEnabled()) {
            model.setRollover(true);
        }
        if (model.isEnabled() && model.isPressed()) {
            model.setArmed(true);
        }
    }

    public void mouseDragged(final MouseEvent event) {
    }

    public void mouseClicked(final MouseEvent event) {
    }

    public void mouseMoved(final MouseEvent event) {
    }

    protected void checkOpacity(final AbstractButton button) {
    }

    public void focusLost(final FocusEvent event) {
        if (button.isEnabled()) {
            final ButtonModel model = button.getModel();
            if (model.isEnabled()) {
                model.setArmed(false);
                model.setPressed(false);
            }
            button.repaint();
        }
    }

    public void focusGained(final FocusEvent event) {
        if (button.isEnabled()) {
            button.repaint();
        }
    }

    private boolean mouseInside(final MouseEvent event) {
        return button.contains(event.getPoint());
    }

    private boolean isMultiClickTimePassed(final MouseEvent event) {
        return (event.getWhen() - previousPressTime) > button.getMultiClickThreshhold();
    }
}

