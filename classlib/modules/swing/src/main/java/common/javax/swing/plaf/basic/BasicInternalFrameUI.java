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
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import javax.swing.AbstractAction;
import javax.swing.DefaultDesktopManager;
import javax.swing.DesktopManager;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.InternalFrameUI;
import javax.swing.plaf.UIResource;

import org.apache.harmony.x.swing.ComponentDragImplHelper;
import org.apache.harmony.x.swing.Utilities;


public class BasicInternalFrameUI extends InternalFrameUI {

    public class InternalFrameLayout implements LayoutManager {
        public Dimension preferredLayoutSize(final Container c) {
            Dimension size = frame.getRootPane().getPreferredSize();

            if (northPane != null) {
                Dimension northPref = northPane.getPreferredSize();
                size.height += northPref.height;
                size.width = Math.max(size.width, northPref.width);
            }
            if (southPane != null) {
                Dimension southPref = southPane.getPreferredSize();
                size.height += southPref.height;
                size.width = Math.max(size.width, southPref.width);
            }
            if (westPane != null) {
                Dimension westPref = westPane.getPreferredSize();
                size.width += westPref.width;
                size.height = Math.max(size.height, westPref.height);
            }
            if (eastPane != null) {
                Dimension eastPref = eastPane.getPreferredSize();
                size.width += eastPref.width;
                size.height = Math.max(size.height, eastPref.height);
            }

            return Utilities.addInsets(size, frame.getInsets());
        }

        public Dimension minimumLayoutSize(final Container c) {
            Dimension size = new Dimension();

            if (northPane != null) {
                size.setSize(northPane.getMinimumSize());
            }

            return Utilities.addInsets(size, frame.getInsets());
        }

        public void layoutContainer(final Container c) {
            Dimension size = frame.getSize();

            Insets insets = frame.getInsets();
            int top = insets.top;
            int left = insets.left;
            int right = size.width - insets.right;
            int bottom = size.height - insets.bottom;

            if (northPane != null) {
                size = northPane.getPreferredSize();
                northPane.setBounds(left, top, right - left, size.height);
                top += northPane.getHeight();
            }

            if (southPane != null) {
                size = southPane.getPreferredSize();
                southPane.setBounds(left, bottom - size.height,
                                    right - left, size.height);
                bottom -= southPane.getHeight();
            }

            if (westPane != null) {
                size = westPane.getPreferredSize();
                westPane.setBounds(left, top, size.width, bottom - top);
                left += westPane.getWidth();
            }

            if (eastPane != null) {
                size = eastPane.getPreferredSize();
                eastPane.setBounds(right - size.width, top,
                                   size.width, bottom - top);
                right -= eastPane.getWidth();
            }

            frame.getRootPane().setBounds(left, top, right - left, bottom - top);
        }

        public void addLayoutComponent(final String name, final Component c) {
        }

        public void removeLayoutComponent(final Component c) {
        }
    }

    public class InternalFramePropertyChangeListener
            implements PropertyChangeListener {

        public void propertyChange(final PropertyChangeEvent e) {
            if (JInternalFrame.IS_ICON_PROPERTY
                    .equals(e.getPropertyName())) {
                iconPropertyChange(e.getNewValue());

            } else if (JInternalFrame.IS_MAXIMUM_PROPERTY
                    .equals(e.getPropertyName())) {
                maximumPropertyChange(e.getNewValue());

            } else if (JInternalFrame.IS_CLOSED_PROPERTY
                    .equals(e.getPropertyName())) {
                closeFrame(frame);

            } else if (JInternalFrame.IS_SELECTED_PROPERTY
                    .equals(e.getPropertyName())) {
                selectedPropertyChange(e.getNewValue());

            } else if (JInternalFrame.GLASS_PANE_PROPERTY
                    .equals(e.getPropertyName())) {
                glassPanePropertyChange(e);

            } else if ("resizable".equals(e.getPropertyName())) {
                frame.repaint();

            } else if ("ancestor".equals(e.getPropertyName())) {
                ancestorPropertyChange(e);
            }
        }

        private void maximumPropertyChange(final Object newValue) {
            if (((Boolean)newValue).booleanValue()) {
                maximizeFrame(frame);
            } else {
                minimizeFrame(frame);
            }
        }

        private void iconPropertyChange(final Object newValue) {
            if (((Boolean)newValue).booleanValue()) {
                iconifyFrame(frame);
            } else {
                deiconifyFrame(frame);
            }
        }

        private void selectedPropertyChange(final Object newValue) {
            if (((Boolean)newValue).booleanValue()) {
                activateFrame(frame);
            } else {
                deactivateFrame(frame);
            }
           frame.repaint();
        }

        private void glassPanePropertyChange(final PropertyChangeEvent e) {
            ((Component)e.getOldValue()).removeMouseListener(
                glassPaneDispatcher);
            ((Component)e.getOldValue()).removeMouseMotionListener(
                glassPaneDispatcher);

            frame.getGlassPane().addMouseListener(glassPaneDispatcher);
            frame.getGlassPane().addMouseMotionListener(glassPaneDispatcher);
        }

        private void ancestorPropertyChange(final PropertyChangeEvent e) {
            if (e.getOldValue() != null) {
                ((Container)e.getOldValue())
                        .removeComponentListener(componentListener);
            }
            if (e.getNewValue() != null) {
                ((Container)e.getNewValue()).
                    addComponentListener(componentListener);
            }
        }
    }

    protected class BasicInternalFrameListener implements InternalFrameListener {
        public void internalFrameClosing(final InternalFrameEvent e) {
        }

        public void internalFrameClosed(final InternalFrameEvent e) {
        }

        public void internalFrameOpened(final InternalFrameEvent e) {
        }

        public void internalFrameIconified(final InternalFrameEvent e) {
        }

        public void internalFrameDeiconified(final InternalFrameEvent e) {
            if (frame.isMaximum()) {
                Container parent = getFrameParent();
                if (parent != null) {
                    frame.setSize(parent.getSize());
                }
            }
        }

        public void internalFrameActivated(final InternalFrameEvent e) {
            if (isKeyBindingRegistered()) {
                return;
            }

            setupMenuOpenKey();
            setupMenuCloseKey();
            setKeyBindingRegistered(true);
        }

        public void internalFrameDeactivated(final InternalFrameEvent e) {
        }
    }

    protected class BorderListener extends MouseInputAdapter
            implements SwingConstants {

        protected final int RESIZE_NONE = 0;

        private final ComponentDragImplHelper helper = new ComponentDragImplHelper();
        private int resizeDirection = RESIZE_NONE;

        public void mouseClicked(final MouseEvent e) {
            if (e.getSource() != frame && SwingUtilities.isLeftMouseButton(e)
                        && e.getClickCount() > 1) {
                if (frame.isMaximizable()) {
                    try {
                        frame.setMaximum(!frame.isMaximum());
                    } catch (final PropertyVetoException v) {
                    }
                }
            }
        }

        public void mousePressed(final MouseEvent e) {
            try {
                frame.setSelected(true);
            } catch (final PropertyVetoException e1) {
            }

            if (frame.isMaximum()) {
                return;
            }

            resizeDirection = ComponentDragImplHelper.getResizeDirection(e, frame);
            if (resizeDirection != RESIZE_NONE && frame.isResizable()) {
                getDesktopManager().beginResizingFrame(frame, resizeDirection);
                helper.beginResizing(e, frame, frame.getParent());
            } else if (isTitlePaneClick(e)) {
                getDesktopManager().beginDraggingFrame(frame);
                helper.beginDragging(e, frame, frame.getParent());
            }
        }

        public void mouseDragged(final MouseEvent e) {
            if (!helper.isDragging()) {
                return;
            }

            Rectangle newBounds = helper.mouseDragged(e);
            if (resizeDirection == RESIZE_NONE) {
                getDesktopManager().dragFrame(frame, newBounds.x, newBounds.y);
            } else {
                getDesktopManager().resizeFrame(frame,
                        newBounds.x, newBounds.y, newBounds.width, newBounds.height);
            }
        }

        public void mouseReleased(final MouseEvent e) {
            if (!helper.isDragging()) {
                return;
            }

            if (resizeDirection == RESIZE_NONE) {
                getDesktopManager().endDraggingFrame(frame);
            } else {
                getDesktopManager().endResizingFrame(frame);
            }
            helper.endDraggingOrResizing(e);

            updateMouseCursor(e);
        }

        public void mouseMoved(final MouseEvent e) {
            updateMouseCursor(e);
        }

        public void mouseExited(final MouseEvent e) {
            if (!helper.isDragging()) {
                frame.getParent().setCursor(null);
            }
        }

        private boolean isTitlePaneClick(final MouseEvent e) {
            Point p = SwingUtilities.convertPoint(
                    e.getComponent(), e.getPoint(), titlePane);

            return titlePane.contains(p);
        }

        private void updateMouseCursor(final MouseEvent e) {
            if (!frame.isResizable() || frame.isMaximum()) {
                return;
            }

            frame.getParent().setCursor(
                    ComponentDragImplHelper.getUpdatedCursor(e, frame));
        }
    }

    protected class ComponentHandler implements ComponentListener {
        public void componentResized(final ComponentEvent e) {
            if (frame.isMaximum()) {
                frame.setSize(frame.getParent().getSize());
            }
        }

        public void componentMoved(final ComponentEvent e) {
        }

        public void componentShown(final ComponentEvent e) {
        }

        public void componentHidden(final ComponentEvent e) {
        }
    }

    protected class GlassPaneDispatcher implements MouseInputListener {
        private Component pressedTarget;
        private Component moveTarget;

        public void mouseEntered(final MouseEvent e) {
            forwardMouseEvent(e);
        }

        public void mouseExited(final MouseEvent e) {
            forwardMouseEvent(e);
        }

        public void mouseClicked(final MouseEvent e) {
            forwardMouseEvent(e);
        }

        public void mouseReleased(final MouseEvent e) {
            forwardMouseEvent(e);
        }

        public void mousePressed(final MouseEvent e) {
            try {
                frame.setSelected(true);
            } catch (final PropertyVetoException e1) {
            }

            forwardMouseEvent(e);
        }

        public void mouseDragged(final MouseEvent e) {
            forwardMouseEvent(e);
        }

        public void mouseMoved(final MouseEvent e) {
            Component target = frame.getLayeredPane().findComponentAt(e.getPoint());
            if (target != moveTarget) {
                createAndDispatchMouseEvent(moveTarget, MouseEvent.MOUSE_EXITED, e);
                createAndDispatchMouseEvent(target, MouseEvent.MOUSE_ENTERED, e);
                moveTarget = target;
            }

            forwardMouseEvent(e);
        }

        private void forwardMouseEvent(final MouseEvent e) {
            // look for the target component
            Component target = frame.getLayeredPane().findComponentAt(e.getPoint());
            switch(e.getID()) {
            case MouseEvent.MOUSE_RELEASED:
            case MouseEvent.MOUSE_DRAGGED:
                target = pressedTarget;
                break;
            case MouseEvent.MOUSE_PRESSED:
                pressedTarget = target;
                break;
            case MouseEvent.MOUSE_EXITED:
                target = moveTarget;
                break;
            }

            if (target != null) {
                MouseEvent e1 = SwingUtilities.convertMouseEvent(
                        frame.getGlassPane(), e, target);
                target.dispatchEvent(e1);
            }
        }

        private void createAndDispatchMouseEvent(final Component target,
                                                 final int id,
                                                 final MouseEvent e) {
            if (target == null) {
                return;
            }

            Point p = SwingUtilities.convertPoint(frame.getGlassPane(),
                                                  e.getPoint(), target);
            MouseEvent newE =  new MouseEvent(target, id, e.getWhen(),
                                              e.getModifiers(),
                                              p.x, p.y,
                                              e.getClickCount(),
                                              e.isPopupTrigger());
            target.dispatchEvent(newE);
        }
    }

    protected JInternalFrame frame;

    @Deprecated
    protected KeyStroke openMenuKey;

    protected BasicInternalFrameTitlePane titlePane;
    protected ComponentListener componentListener;
    protected MouseInputListener glassPaneDispatcher;
    protected PropertyChangeListener propertyChangeListener;
    protected MouseInputAdapter borderListener;
    protected LayoutManager internalFrameLayout;
    protected JComponent northPane;
    protected JComponent southPane;
    protected JComponent eastPane;
    protected JComponent westPane;

    private InternalFrameListener internalFrameListener;

    /*
     * If frame is not in some JDesktopPane, this field is used
     * in getDesktopManager() to store the created desktop manager.
     */
    private DesktopManager defaultDesktopManager;

    private boolean keyBindingActive;
    private boolean keyBindingRegistered;

    public BasicInternalFrameUI(final JInternalFrame frame) {
    }

    public void setWestPane(final JComponent c) {
        westPane = c;
    }

    public JComponent getWestPane() {
        return westPane;
    }

    public void setSouthPane(final JComponent c) {
        southPane = c;
    }

    public JComponent getSouthPane() {
        return southPane;
    }

    public void setNorthPane(final JComponent c) {
        replacePane(getNorthPane(), c);
        northPane = c;
    }

    public JComponent getNorthPane() {
        return northPane;
    }

    public void setEastPane(final JComponent c) {
        eastPane = c;
    }

    public JComponent getEastPane() {
        return eastPane;
    }

    public Dimension getPreferredSize(final JComponent c) {
        if (c != frame) {
            return new Dimension(100, 100);
        }

        return internalFrameLayout.preferredLayoutSize(c);
    }

    public Dimension getMinimumSize(final JComponent c) {
        if (c != frame) {
            return new Dimension(0, 0);
        }

        return internalFrameLayout.minimumLayoutSize(c);
    }

    public Dimension getMaximumSize(final JComponent c) {
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    public static ComponentUI createUI(final JComponent c) {
        return new BasicInternalFrameUI((JInternalFrame)c);
    }

    public void installUI(final JComponent c) {
        frame = (JInternalFrame)c;

        installDefaults();
        installListeners();
        installComponents();
        installKeyboardActions();
    }

    public void uninstallUI(final JComponent c) {
        uninstallKeyboardActions();
        uninstallComponents();
        uninstallListeners();
        uninstallDefaults();
    }

    protected MouseInputAdapter createBorderListener(final JInternalFrame f) {
        return new BorderListener();
    }

    protected JComponent createWestPane(final JInternalFrame f) {
        return null;
    }

    protected JComponent createSouthPane(final JInternalFrame f) {
        return null;
    }

    protected JComponent createNorthPane(final JInternalFrame f) {
        titlePane = new BasicInternalFrameTitlePane(f);
        return titlePane;
    }

    protected JComponent createEastPane(final JInternalFrame f) {
        return null;
    }

    protected void replacePane(final JComponent oldPane, final JComponent newPane) {
        if (oldPane != null) {
            deinstallMouseHandlers(oldPane);
            frame.remove(oldPane);
        }

        if (newPane != null) {
            installMouseHandlers(newPane);
            frame.add(newPane);
            if (newPane instanceof BasicInternalFrameTitlePane) {
                titlePane = ((BasicInternalFrameTitlePane)newPane);
            }
        }
    }

    protected void minimizeFrame(final JInternalFrame f) {
        getDesktopManager().minimizeFrame(f);
        fireSoundAction("InternalFrame.restoreDownSound");
    }

    protected void maximizeFrame(final JInternalFrame f) {
        getDesktopManager().maximizeFrame(f);
        fireSoundAction("InternalFrame.maximizeSound");
    }

    protected void iconifyFrame(final JInternalFrame f) {
        getDesktopManager().iconifyFrame(f);
        fireSoundAction("InternalFrame.minimizeSound");
    }

    protected void deiconifyFrame(final JInternalFrame f) {
        getDesktopManager().deiconifyFrame(f);
        fireSoundAction("InternalFrame.restoreUpSound");
    }

    protected void activateFrame(final JInternalFrame f) {
        getDesktopManager().activateFrame(f);
        f.getGlassPane().setVisible(false);
    }

    protected void deactivateFrame(final JInternalFrame f) {
        getDesktopManager().deactivateFrame(f);
        f.getGlassPane().setVisible(true);
    }

    protected void closeFrame(final JInternalFrame f) {
        getDesktopManager().closeFrame(f);
        fireSoundAction("InternalFrame.closeSound");
    }

    protected void installMouseHandlers(final JComponent c) {
        c.addMouseListener(borderListener);
        c.addMouseMotionListener(borderListener);
    }

    protected void deinstallMouseHandlers(final JComponent c) {
        c.removeMouseListener(borderListener);
        c.removeMouseMotionListener(borderListener);
    }

    protected DesktopManager getDesktopManager() {
        JDesktopPane desktop = frame.getDesktopPane();
        if (desktop != null) {
            return desktop.getDesktopManager();
        }

        if (defaultDesktopManager == null) {
            defaultDesktopManager = createDesktopManager();
        }
        return defaultDesktopManager;
    }

    protected DesktopManager createDesktopManager() {
        return new DefaultDesktopManager();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        if (propertyChangeListener == null) {
            propertyChangeListener = new InternalFramePropertyChangeListener();
        }

        return propertyChangeListener;
    }

    protected MouseInputListener createGlassPaneDispatcher() {
        return new GlassPaneDispatcher();
    }

    protected ComponentListener createComponentListener() {
        return new ComponentHandler();
    }

    protected LayoutManager createLayoutManager() {
        return new InternalFrameLayout();
    }

    protected final void setKeyBindingRegistered(final boolean b) {
        keyBindingRegistered = b;
    }

    protected final boolean isKeyBindingRegistered() {
        return keyBindingRegistered;
    }

    protected final void setKeyBindingActive(final boolean b) {
        keyBindingActive = b;
    }

    public final boolean isKeyBindingActive() {
        return keyBindingActive;
    }

    protected void setupMenuOpenKey() {
        if (frame == null) {
            throw new NullPointerException();
        }

        Object[] keys = (Object[])UIManager.get("InternalFrame.windowBindings");
        if (keys == null) {
            return;
        }
        
        InputMap map = LookAndFeel.makeComponentInputMap(frame, keys);
        SwingUtilities.replaceUIInputMap(
                frame, JComponent.WHEN_IN_FOCUSED_WINDOW, map);
    }

    protected void setupMenuCloseKey() {
        // does nothing, all the work is done in setupMenuOnenKey()
    }

    protected void installListeners() {
        borderListener = createBorderListener(frame);
        installMouseHandlers(frame);

        componentListener = createComponentListener();
        if (frame.getParent() != null) {
            frame.getParent().addComponentListener(componentListener);
        }

        glassPaneDispatcher = createGlassPaneDispatcher();
        frame.getGlassPane().addMouseListener(glassPaneDispatcher);
        frame.getGlassPane().addMouseMotionListener(glassPaneDispatcher);

        propertyChangeListener = createPropertyChangeListener();
        frame.addPropertyChangeListener(propertyChangeListener);
    }

    protected void uninstallListeners() {
        frame.removePropertyChangeListener(propertyChangeListener);

        frame.getGlassPane().removeMouseListener(glassPaneDispatcher);
        frame.getGlassPane().removeMouseMotionListener(glassPaneDispatcher);

        if (frame.getParent() != null) {
            frame.getParent().removeComponentListener(componentListener);
        }

        deinstallMouseHandlers(frame);
    }

    protected void installKeyboardActions() {
        createInternalFrameListener();
        frame.addInternalFrameListener(internalFrameListener);

        ActionMapUIResource actionMap = new ActionMapUIResource();
        actionMap.put("showSystemMenu", new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                if (titlePane != null) {
                    titlePane.showSystemMenu();
                }
            }
        });

        actionMap.setParent(((BasicLookAndFeel)UIManager.getLookAndFeel())
                            .getAudioActionMap());
        SwingUtilities.replaceUIActionMap(frame, actionMap);
    }

    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIActionMap(frame, null);
        SwingUtilities.replaceUIInputMap(frame,
                JComponent.WHEN_IN_FOCUSED_WINDOW, null);

        frame.removeInternalFrameListener(internalFrameListener);
    }

    protected void installDefaults() {
        if (Utilities.isUIResource(frame.getBackground())) {
            frame.setBackground(UIManager.getColor(
                "InternalFrame.inactiveTitleBackground"));
        }

        LookAndFeel.installBorder(frame, "InternalFrame.border");

        if (Utilities.isUIResource(frame.getFrameIcon())) {
            frame.setFrameIcon(UIManager.getIcon("InternalFrame.icon"));
        }

        internalFrameLayout = createLayoutManager();
        frame.setLayout(internalFrameLayout);

        LookAndFeel.installProperty(frame, "opaque", Boolean.TRUE);
    }

    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(frame);

        if (frame.getFrameIcon() instanceof UIResource) {
            frame.setFrameIcon(null);
        }

        internalFrameLayout = null;
        frame.setLayout(internalFrameLayout);
    }

    protected void installComponents() {
        setNorthPane(createNorthPane(frame));
    }

    protected void uninstallComponents() {
        setNorthPane(null);
    }

    protected void createInternalFrameListener() {
        if (internalFrameListener == null) {
            internalFrameListener = new BasicInternalFrameListener();
        }
    }

    private Container getFrameParent() {
        Container parent = frame.getParent();
        if (parent == null) {
            parent = frame.getDesktopIcon().getParent();
        }
        return parent;
    }

    private void fireSoundAction(final String name) {
        ((BasicLookAndFeel)UIManager.getLookAndFeel()).fireSoundAction(frame, name);
    }
}
