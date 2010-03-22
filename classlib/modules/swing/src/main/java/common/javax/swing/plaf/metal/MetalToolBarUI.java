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
package javax.swing.plaf.metal;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ContainerListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarUI;

public class MetalToolBarUI extends BasicToolBarUI {
    protected class MetalDockingListener extends DockingListener {
        public MetalDockingListener(final JToolBar t) {
            super(t);
        }

        public void mousePressed(final MouseEvent e) {
            if (!isOnBumps(e.getPoint())) {
                return;
            }

            setDragOffset(e.getPoint());
            super.mousePressed(e);
        }

        public void mouseDragged(final MouseEvent e) {
            if (!isDragging && !isOnBumps(e.getPoint())) {
                return;
            }
            super.mouseDragged(e);
        }

        private boolean isOnBumps(final Point p) {
            if (p.x < 0 || p.y < 0) {
                return false;
            }
            Insets insets = toolBar.getInsets();
            if (toolBar.getOrientation() == HORIZONTAL) {
                return p.x < insets.left && p.y < toolBar.getHeight();
            } else {
                return p.x < toolBar.getWidth() && p.y < insets.top;
            }
        }
    }

    protected class MetalContainerListener extends ToolBarContListener {
        protected MetalContainerListener() {
        }
    }

    protected class MetalRolloverListener extends PropertyListener {
        protected MetalRolloverListener() {
        }
    }

    protected ContainerListener contListener;
    protected PropertyChangeListener rolloverListener;

    public static ComponentUI createUI(final JComponent c) {
        return new MetalToolBarUI();
    }

    public void installUI(final JComponent c) {
        super.installUI(c);
    }

    public void uninstallUI(final JComponent c) {
        super.uninstallUI(c);
    }

    public void update(final Graphics g, final JComponent c) {
        super.update(g, c);
    }

    protected void installListeners() {
        super.installListeners();

        contListener = createContainerListener();
        if (contListener != null) {
            toolBar.addContainerListener(contListener);
        }

        rolloverListener = createRolloverListener();
        if (rolloverListener != null) {
            toolBar.addPropertyChangeListener(rolloverListener);
        }
    }

    protected void uninstallListeners() {
        super.uninstallListeners();

        toolBar.removeContainerListener(contListener);
        toolBar.removePropertyChangeListener(rolloverListener);
    }

    protected Border createRolloverBorder() {
        Border buttonBorder = new MetalBorders.RolloverButtonBorder();
        Border marginBorder = new MetalBorders.ToolBarButtonMarginBorder();
        return new BorderUIResource.CompoundBorderUIResource(buttonBorder,
                                                                                                         marginBorder);
    }

    protected Border createNonRolloverBorder() {
        Border buttonBorder = new MetalBorders.ButtonBorder();
        Border marginBorder = new MetalBorders.ToolBarButtonMarginBorder();
        return new BorderUIResource.CompoundBorderUIResource(buttonBorder,
                                                             marginBorder);
    }

    protected void setBorderToNonRollover(final Component c) {
        super.setBorderToNonRollover(c);
    }

    protected ContainerListener createContainerListener() {
        return null;
    }

    protected PropertyChangeListener createRolloverListener() {
        return null;
    }

    protected MouseInputListener createDockingListener() {
        return new MetalDockingListener(toolBar);
    }

    protected void setDragOffset(final Point p) {
        if (dragWindow == null) {
            dragWindow = createDragWindow(toolBar);
        }
        dragWindow.setOffset(p);
    }
}
