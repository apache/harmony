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
 * @author Sergey Burlak
 */
package javax.swing;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.LinkedList;

import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class ToolTipManager extends MouseAdapter implements MouseMotionListener {
    protected class insideTimerAction implements ActionListener {
        public void actionPerformed(final ActionEvent e) {
            insideTimer.stop();
            component = enteredComponent;
            tipText = newTipText;
            showPopup();
        }
    }

    protected class outsideTimerAction implements ActionListener {
        public void actionPerformed(final ActionEvent e) {
            outsideTimer.stop();
        }
    }

    protected class stillInsideTimerAction implements ActionListener {
        public void actionPerformed(final ActionEvent e) {
            hidePopup();
        }
    }

    protected boolean lightWeightPopupEnabled = true;
    protected boolean heavyWeightPopupEnabled;

    //There is no API to retrieve actual cursor size. An approximate
    //'default' cursor height is taken.
    private static final int MOUSE_CURSOR_HEIGHT = 20;
    private static ToolTipManager manager;

    private int reshowDelay = 500;
    private int dismissDelay = 4000;
    private int initialDelay = 750;

    private Timer insideTimer;
    private Timer outsideTimer;
    private Timer stillInsideTimer;

    private LinkedList componentsList;

    private boolean enabled = true;
    private JComponent component;
    private JComponent enteredComponent;
    private Popup popup;
    private String tipText;
    private String newTipText;

    private ToolTipManager() {
        insideTimer = new Timer(initialDelay, new insideTimerAction());
        outsideTimer = new Timer(reshowDelay, new outsideTimerAction());
        stillInsideTimer = new Timer(dismissDelay, new stillInsideTimerAction());

        componentsList = new LinkedList();
    }

    public void setEnabled(final boolean flag) {
        enabled = flag;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setLightWeightPopupEnabled(final boolean flag) {
        lightWeightPopupEnabled = flag;
    }

    public boolean isLightWeightPopupEnabled() {
        return lightWeightPopupEnabled;
    }

    public void setInitialDelay(final int milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException(Messages.getString("swing.65", milliseconds)); //$NON-NLS-1$
        }
        initialDelay = milliseconds;
    }

    public int getInitialDelay() {
        return initialDelay ;
    }

    public void setDismissDelay(final int milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException(Messages.getString("swing.66", milliseconds)); //$NON-NLS-1$
        }
        dismissDelay = milliseconds;
    }

    public int getDismissDelay() {
        return dismissDelay;
    }

    public void setReshowDelay(final int milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException(Messages.getString("swing.67", milliseconds)); //$NON-NLS-1$
        }
        reshowDelay = milliseconds;
    }

    public int getReshowDelay() {
        return reshowDelay;
    }

    public static ToolTipManager sharedInstance() {
        if (manager == null) {
            manager = new ToolTipManager();
        }

        return manager;
    }

    public void registerComponent(final JComponent c) {
        if (!componentsList.contains(c)) {
            c.addMouseListener(this);
            c.addMouseMotionListener(this);
            componentsList.add(c);
        }
    }

    public void unregisterComponent(final JComponent c) {
        componentsList.remove(c);
        c.removeMouseListener(this);
        c.removeMouseMotionListener(this);
    }

    public void mouseEntered(final MouseEvent e) {
        if (!isEnabled() || SwingUtilities.isLeftMouseButton(e)) {
            return;
        }
        enteredComponent = (JComponent)e.getComponent();
        newTipText = enteredComponent.getToolTipText(e);
        if (outsideTimer.isRunning()) {
            tipText = newTipText;
            outsideTimer.stop();
            component = enteredComponent;
            showPopup();
        } else {
            insideTimer.restart();
        }
    }

    public void mouseExited(final MouseEvent e) {
        boolean isPopupShown = stillInsideTimer.isRunning();
        hidePopup();
        if (isPopupShown) {
            outsideTimer.restart();
        }
        insideTimer.stop();
    }

    public void mousePressed(final MouseEvent e) {
        hidePopup();
        insideTimer.stop();
    }

    public void mouseDragged(final MouseEvent e) {
    }

    public void mouseMoved(final MouseEvent e) {
        newTipText = ((JComponent)e.getComponent()).getToolTipText(e);

        if (component != e.getComponent()) {
            return;
        }
        if (!toolTipTextChanged(newTipText)) {
            return;
        }

        if (stillInsideTimer.isRunning()) {
            tipText = newTipText;
            showPopup();
        } else {
            mouseEntered(e);
        }
    }

    private boolean toolTipTextChanged(final String newToolTipText) {
        return (!Utilities.isEmptyString(newToolTipText) || !Utilities.isEmptyString(tipText))
                && (tipText == null || !tipText.equals(newToolTipText));
    }

    private void hidePopup() {
        if (popup != null) {
            popup.hide();
            popup = null;
            insideTimer.stop();
            stillInsideTimer.stop();
            outsideTimer.stop();
        }
    }

    private void showPopup() {
        if (!isEnabled()) {
            return;
        }

        if (Utilities.isEmptyString(tipText) || !component.isShowing()) {
            hidePopup();
            return;
        }
        JToolTip t = component.createToolTip();
        t.setTipText(tipText);

        if (popup != null) {
            popup.hide();
        }
        PopupFactory factory = PopupFactory.getSharedInstance();
        Point toolTipPoint = MouseInfo.getPointerInfo().getLocation();
        factory.setLWPopupsEnabled(isLightWeightPopupEnabled());
        popup = factory.getPopup(component, t, toolTipPoint.x, toolTipPoint.y + MOUSE_CURSOR_HEIGHT);
        factory.setLWPopupsEnabled(false);
        popup.show();
        stillInsideTimer.restart();
    }
}
