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


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;

import javax.swing.DesktopManager;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.LookAndFeel;

import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DesktopIconUI;

import org.apache.harmony.x.swing.ComponentDragImplHelper;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class BasicDesktopIconUI extends DesktopIconUI {
    private static  final int INSETS_WIDTH = 5;

    public class MouseInputHandler extends MouseInputAdapter {
        private ComponentDragImplHelper helper;
        private DesktopManager desktopManager;

        public MouseInputHandler() {
            helper = new ComponentDragImplHelper();
        }

        public void mouseDragged(final MouseEvent e) {
            Rectangle bounds = helper.mouseDragged(e);
            moveAndRepaint(desktopIcon, bounds.x, bounds.y,
                    bounds.width, bounds.height);
        }

        public void mouseMoved(final MouseEvent e) {
            return;
        }

        public void mousePressed(final MouseEvent e) {
            if (e.getClickCount() > 1 && frame.isIconifiable()) {
                deiconize();
                return;
            }

            desktopManager = ((BasicInternalFrameUI)frame.getUI()).
                    getDesktopManager();
            try {
                frame.setSelected(true);
                Component desktop = desktopIcon.getParent();
                if (desktop instanceof JLayeredPane) {
                    ((JLayeredPane)desktop).moveToFront(desktopIcon);
                }
            } catch (final PropertyVetoException e1) {
            }
            helper.beginDragging(e, desktopIcon, desktopIcon.getDesktopPane());
            desktopManager.beginDraggingFrame(desktopIcon);
        }

        public void mouseReleased(final MouseEvent e) {
            helper.endDraggingOrResizing(e);
            desktopManager.endDraggingFrame(desktopIcon);
        }

        public void moveAndRepaint(final JComponent f,
                final int newX, final int newY,
                final int newWidth, final int newHeight) {
            desktopManager.dragFrame(desktopIcon, newX, newY);
        }
    }

    public static ComponentUI createUI(final JComponent c) {
        return new BasicDesktopIconUI();
    }

    protected JInternalFrame.JDesktopIcon desktopIcon;
    protected JInternalFrame frame;
    protected JComponent iconPane;
    private MouseInputListener mouseHandler;

    public BasicDesktopIconUI() {
    }

    protected MouseInputListener createMouseInputListener() {
        if (mouseHandler == null) {
            mouseHandler = new MouseInputHandler();
        }

        return mouseHandler;
    }

    public void deiconize() {
        try {
            frame.setIcon(false);
        } catch (final PropertyVetoException e) {
        }
    }

    public Insets getInsets(final JComponent c) {
        if (c == null) {
            throw new NullPointerException(Messages.getString("swing.03","component")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        //return desktopIcon.getInsets();
        return new Insets(INSETS_WIDTH, INSETS_WIDTH,
                INSETS_WIDTH, INSETS_WIDTH);
    }

    public Dimension getMaximumSize(final JComponent c) {
        return getPreferredSize(c);
    }

    public Dimension getMinimumSize(final JComponent c) {
        //return iconPane.getLayout().minimumLayoutSize(c);
        return getPreferredSize(c);
    }

    public Dimension getPreferredSize(final JComponent c) {
        return Utilities.addInsets(iconPane.getPreferredSize(), desktopIcon.getInsets());
    }

    protected void installComponents() {
        iconPane = new BasicInternalFrameTitlePane(frame);
        desktopIcon.add(iconPane, BorderLayout.CENTER);
    }

    protected void uninstallComponents() {
        desktopIcon.remove(iconPane);
    }

    protected void installDefaults() {
        LookAndFeel.installBorder(desktopIcon, "DesktopIcon.border");
    }

    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(desktopIcon);
    }

    protected void installListeners() {
        mouseHandler = createMouseInputListener();

        desktopIcon.addMouseListener(mouseHandler);
        desktopIcon.addMouseMotionListener(mouseHandler);
    }

    protected void uninstallListeners() {
        desktopIcon.removeMouseListener(mouseHandler);
        desktopIcon.removeMouseMotionListener(mouseHandler);
    }

    public void installUI(final JComponent c) {
        desktopIcon = (JInternalFrame.JDesktopIcon) c;
        frame = desktopIcon.getInternalFrame();

        installDefaults();
        installListeners();
        installComponents();

        desktopIcon.setSize(desktopIcon.getPreferredSize());
    }

    public void uninstallUI(final JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        uninstallComponents();
    }
}
