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
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ToolBarUI;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;


public class BasicToolBarUI extends ToolBarUI implements SwingConstants {
    private static final String SAVED_BORDER_PROPERTY = "JToolBar.savedBorder";
    private static final String SAVED_ROLLOVER_ENABLED_PROPERTY =
            "JToolBar.rolloverEnabled";

    public class DockingListener implements MouseInputListener {
        protected boolean isDragging;
        protected Point origin = new Point();
        protected JToolBar toolBar;

        public DockingListener(final JToolBar t) {
            toolBar = t;
        }

        public void mouseDragged(final MouseEvent e) {
            if (!isDragging) {
                isDragging = true;
                origin.setLocation(0, 0);
                SwingUtilities.convertPointToScreen(origin, toolBar);
            }
            dragTo(e.getPoint(), origin);
        }

        public void mouseReleased(final MouseEvent e) {
            if (isDragging) {
                floatAt(e.getPoint(), origin);
                isDragging = false;
                if (dragWindow != null) {
                    dragWindow.setOffset(null);
                }
            }
        }

        public void mouseClicked(final MouseEvent e) {
        }

        public void mouseEntered(final MouseEvent e) {
        }

        public void mouseExited(final MouseEvent e) {
        }

        public void mouseMoved(final MouseEvent e) {
        }

        public void mousePressed(final MouseEvent e) {
        }
    }

    protected class DragWindow extends Window {
        private Point offset;
        private int orientation;
        private Color borderColor;

        private DragWindow(final Window owner) {
            super(owner);
            setFocusableWindowState(false);
        }

        public Insets getInsets() {
            return new Insets(1, 1, 1, 1);
        }

        public void paint(final Graphics g) {
            paintDragWindow(g);
        }

        public void setBorderColor(final Color c) {
            if (borderColor != c) {
                borderColor = c;
                repaint();
            }
        }

        public Color getBorderColor() {
            return borderColor;
        }

        public void setOffset(final Point p) {
            offset = p;
        }

        public Point getOffset() {
            return offset;
        }

        public void setOrientation(final int o) {
            orientation = o;
            repaint();
        }

        private void updateBounds(final int o, final Point newLocation) {
            if (orientation != o) {
                Rectangle bounds = SwingUtilities.getLocalBounds(this);
                setOffset(new Point(offset.y, offset.x));
                bounds.setLocation((int)bounds.getCenterX() - getOffset().x,
                                   (int)bounds.getCenterY() - getOffset().y);
                bounds.setSize(getHeight(), getWidth());
                bounds.setLocation(newLocation);
                setBounds(bounds);
            } else {
                setLocation(newLocation);
            }
            setOrientation(o);
        }
    }

    protected class FrameListener extends WindowAdapter {
        public void windowClosing(final WindowEvent w) {
            setFloating(false, null);
        }
    }

    protected class PropertyListener implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if ("JToolBar.isRollover".equals(propertyName)) {
                setRolloverBorders(((Boolean)e.getNewValue()).booleanValue());
                toolBarSizeChange();
            } else if ("orientation".equals(propertyName)
                    || "floatable".equals(propertyName)
                    || "borderPainted".equals(propertyName)
                    || "margin".equals(propertyName)) {
                toolBarSizeChange();
            }
        }

        private void toolBarSizeChange() {
            if (isFloating()) {
                toolBar.invalidate();
                ((Container)floatingWindow).setSize(
                        ((Container)floatingWindow).getPreferredSize());
                ((Container)floatingWindow).validate();
            } else {
                toolBar.revalidate();
            }
            toolBar.repaint();
        }
    }

    protected class ToolBarContListener implements ContainerListener {
        public void componentAdded(final ContainerEvent e) {
            if (isRolloverBorders()) {
                setBorderToRollover(e.getChild());
            } else {
                setBorderToNonRollover(e.getChild());
            }
            e.getChild().addFocusListener(toolBarFocusListener);
        }

        public void componentRemoved(final ContainerEvent e) {
            setBorderToNormal(e.getChild());
            e.getChild().removeFocusListener(toolBarFocusListener);
        }
    }

    protected class ToolBarFocusListener implements FocusListener {
        public void focusGained(final FocusEvent e) {
            focusedCompIndex = toolBar.getComponentIndex(e.getComponent());
        }

        public void focusLost(final FocusEvent e) {
            // does nothing
        }
    }

    private class NavigateAction extends AbstractAction {
        private int direction;

        public NavigateAction(final int direction) {
            this.direction = direction;
        }

        public void actionPerformed(final ActionEvent e) {
            navigateFocusedComp(direction);
        }
    }

    private class FloatingWindow extends JDialog {
        public FloatingWindow(final Frame owner, final String title) {
            super(owner, title, false);
        }

        public FloatingWindow(final Dialog owner, final String title) {
            super(owner, title, false);
        }
    }

    public static ComponentUI createUI(final JComponent c) {
        return new BasicToolBarUI();
    }

    protected DragWindow dragWindow;
    protected JToolBar toolBar;
    protected String constraintBeforeFloating = BorderLayout.NORTH;
    protected int focusedCompIndex = -1;

    protected Color dockingBorderColor;
    protected Color dockingColor;
    protected Color floatingBorderColor;
    protected Color floatingColor;

    protected MouseInputListener dockingListener;
    protected PropertyChangeListener propertyListener;
    protected ContainerListener toolBarContListener;
    protected FocusListener toolBarFocusListener;

    /**
     * @deprecated
     */
    protected KeyStroke downKey;
    /**
     * @deprecated
     */
    protected KeyStroke leftKey;
    /**
     * @deprecated
     */
    protected KeyStroke rightKey;
    /**
     * @deprecated
     */
    protected KeyStroke upKey;

    private boolean isRolloverBorders;
    private Border rolloverBorder;
    private Border nonRolloverBorder;
    private ActionMap actionMap;
    private RootPaneContainer floatingWindow;
    private boolean floating;
    private Container parentBeforeFloating;
    private Window defaultWindowOwner;
    private Point floatingLocation = new Point();

    private Color darkShadow;
    private Color highlight;
    private Color light;
    private Color shadow;

    private Color buttonDarkShadow;
    private Color buttonHighlight;
    private Color buttonLight;
    private Color buttonShadow;


    public void installUI(final JComponent c) {
        toolBar = (JToolBar)c;

        installDefaults();
        installComponents();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(final JComponent c) {
        if (isFloating()) {
            setFloating(false, null);
            if (dragWindow != null) {
                dragWindow.dispose();
                dragWindow = null;
            }
            if (floatingWindow != null) {
                if (floatingWindow instanceof Window) {
                    ((Window)floatingWindow).dispose();
                }
                floatingWindow = null;
            }
        }

        uninstallDefaults();
        uninstallComponents();
        uninstallListeners();
        uninstallKeyboardActions();
    }

    public boolean canDock(final Component c, final Point p) {
        String constraints = getDockingConstraints(c, p);
        if (constraints == BorderLayout.CENTER || !(c instanceof Container)) {
            return false;
        }

        if (((Container)c).getLayout() instanceof BorderLayout) {
            BorderLayout borderLayout = (BorderLayout)((Container)c).getLayout();
            if (borderLayout.getLayoutComponent(constraints) != null) {
                return false;
            }
        }
        return true;
    }

    public void setDockingColor(final Color c) {
        dockingColor = c;
    }

    public Color getDockingColor() {
        return dockingColor;
    }

    public void setFloatingColor(final Color c) {
        floatingColor = c;
    }

    public Color getFloatingColor() {
        return floatingColor;
    }

    public void setFloating(final boolean b, final Point p) {
        Container parent = getToolBarMainParent();
        boolean wasFloating = isFloating();
        floating = b;
        final int saveFocusedCompIndex = focusedCompIndex;
        if (floating) {
            Point fLocation;
            if (p == null) {
                fLocation = floatingLocation;
            } else {
                fLocation = p;
                SwingUtilities.convertPointToScreen(fLocation, parent);
            }
            if (!wasFloating) {
                if (parent.getLayout() instanceof BorderLayout) {
                    Object constraint = ((BorderLayout)parent.getLayout())
                            .getConstraints(toolBar);
                    if (constraint instanceof String) {
                        constraintBeforeFloating = (String)constraint;
                    }
                }
                if (floatingWindow == null) {
                    floatingWindow = createFloatingWindow(toolBar);
                }
                setOrientation(HORIZONTAL);
                parentBeforeFloating = toolBar.getParent();
                floatingWindow.getContentPane().add(toolBar);
                if (floatingWindow instanceof Window) {
                    ((Window)floatingWindow).pack();
                } else {
                  ((Container)floatingWindow).setSize(
                          ((Container)floatingWindow).getPreferredSize());
                  revalidateContainer((Container)floatingWindow);
                }
                revalidateContainer(parentBeforeFloating);
            }
            Point offset = SwingUtilities.convertPoint(toolBar,
                    dragWindow.getOffset(),
                    SwingUtilities.getWindowAncestor(toolBar));
            fLocation.translate(-offset.x, -offset.y);
            ((Container)floatingWindow).setLocation(fLocation.x, fLocation.y);
            ((Container)floatingWindow).setVisible(true);
        } else {
            String dockingConstraints =
                p != null
                ? getDockingConstraints(parent, p)
                : constraintBeforeFloating;
            if (wasFloating) {
                ((Container)floatingWindow).setVisible(false);
            }
            setOrientation(calculateOrientation(dockingConstraints));
            parent.add(toolBar, dockingConstraints);
            toolBar.revalidate();
            toolBar.repaint();
        }
        if (saveFocusedCompIndex != -1) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    toolBar.getComponentAtIndex(saveFocusedCompIndex).requestFocus();
                }
            });
        }
    }

    public void setFloatingLocation(final int x, final int y) {
        floatingLocation.setLocation(x, y);
    }

    public boolean isFloating() {
        return floating;
    }

    public void setOrientation(final int orientation) {
        toolBar.setOrientation(orientation);
    }

    public void setRolloverBorders(final boolean rollover) {
        isRolloverBorders = rollover;

        if (rollover) {
            installRolloverBorders(toolBar);
        } else {
            installNonRolloverBorders(toolBar);
        }
    }

    public boolean isRolloverBorders() {
        return isRolloverBorders;
    }

    protected MouseInputListener createDockingListener() {
        return new DockingListener(toolBar);
    }

    protected DragWindow createDragWindow(final JToolBar toolbar) {
        Window owner;
        if (floatingWindow == null) {
            floatingWindow = createFloatingWindow(toolBar);
        }
        if (floatingWindow instanceof Window) {
            owner = (Window)floatingWindow;
        } else {
            owner = SwingUtilities.getWindowAncestor(toolBar);
            if (owner == null) {
                if (defaultWindowOwner == null) {
                    defaultWindowOwner = new Frame();
                }
                owner = defaultWindowOwner;
            }
        }

        return new DragWindow(owner);
    }

    protected JFrame createFloatingFrame(final JToolBar toolbar) {
        JFrame floatingFrame = new JFrame(toolbar.getName());
        floatingFrame.setResizable(false);
        floatingFrame.addWindowListener(createFrameListener());
        return floatingFrame;
    }

    protected RootPaneContainer createFloatingWindow(final JToolBar toolbar) {
        Window owner = SwingUtilities.getWindowAncestor(toolBar);
        while (owner instanceof FloatingWindow) {
            owner = owner.getOwner();
        }
        JDialog floatingFrame;
        if (owner instanceof Dialog) {
            floatingFrame = new FloatingWindow((Dialog)owner, toolbar.getName());
        } else if (owner instanceof Frame) {
            floatingFrame = new FloatingWindow((Frame)owner, toolbar.getName());
        } else {
            floatingFrame = new FloatingWindow((Frame)null, toolbar.getName());
        }

        floatingFrame.setResizable(false);
        floatingFrame.addWindowListener(createFrameListener());
        return floatingFrame;
    }

    protected WindowListener createFrameListener() {
        return new FrameListener();
    }

    protected PropertyChangeListener createPropertyListener() {
        return new PropertyListener();
    }

    protected Border createRolloverBorder() {
        Border buttonBorder = new BasicBorders.RolloverButtonBorder(
                shadow, darkShadow, highlight, light);
        Border marginBorder = new BasicBorders.ToolBarButtonMarginBorder();
        return new BorderUIResource.CompoundBorderUIResource(buttonBorder,
                                                             marginBorder);
    }

    protected Border createNonRolloverBorder() {
        Border buttonBorder = new BasicBorders.ButtonBorder(
                buttonShadow, buttonDarkShadow, buttonHighlight, buttonLight);
        Border marginBorder = new BasicBorders.ToolBarButtonMarginBorder();
        return new BorderUIResource.CompoundBorderUIResource(buttonBorder,
                                                             marginBorder);
    }

    protected ContainerListener createToolBarContListener() {
        return new ToolBarContListener();
    }

    protected FocusListener createToolBarFocusListener() {
        return new ToolBarFocusListener();
    }

    protected void dragTo(final Point position, final Point origin) {
        if (!toolBar.isFloatable()) {
            return;
        }

        if (dragWindow == null) {
            dragWindow = createDragWindow(toolBar);
        }
        if (!dragWindow.isVisible()) {
            Dimension size = toolBar.getPreferredSize();
            dragWindow.setSize(size);
            if (dragWindow.getOffset() == null) {
                dragWindow.setOffset(new Point(size.width / 2, size.height / 2));
            }
            dragWindow.setOrientation(toolBar.getOrientation());
            dragWindow.setVisible(true);
        }

        Container parent = getToolBarMainParent();
        Point p = SwingUtilities.convertPoint(toolBar, position, parent);
        boolean canDock = canDock(parent, p);
        dragWindow.setBackground(canDock
                                 ? getDockingColor()
                                 : getFloatingColor());
        dragWindow.setBorderColor(canDock
                                  ? dockingBorderColor
                                  : floatingBorderColor);
        int newOrientation = calculateOrientation(getDockingConstraints(parent, p));
        SwingUtilities.convertPointToScreen(p, parent);
        p.translate(-dragWindow.getOffset().x, -dragWindow.getOffset().y);
        dragWindow.updateBounds(newOrientation, p);
    }

    protected void floatAt(final Point position, final Point origin) {
        if (!toolBar.isFloatable()) {
            return;
        }

        dragWindow.dispose();

        Container parent = getToolBarMainParent();
        Point p = SwingUtilities.convertPoint(toolBar, position, parent);
        String dockingConstraints = getDockingConstraints(parent, p);
        if (dockingConstraints == BorderLayout.CENTER) {
            SwingUtilities.convertPointToScreen(p, parent);
            setFloatingLocation(p.x, p.y);
            setFloating(true, null);
        } else {
            setFloating(false, p);
        }
    }

    protected void installComponents() {
        // does nothing
    }

    protected void uninstallComponents() {
        // does nothing
    }

    protected void installDefaults() {
        LookAndFeel.installColorsAndFont(toolBar, "ToolBar.background",
                                         "ToolBar.foreground", "ToolBar.font");
        LookAndFeel.installBorder(toolBar, "ToolBar.border");
        LookAndFeel.installProperty(toolBar, StringConstants.OPAQUE_PROPERTY,
                                    Boolean.TRUE);

        dockingBorderColor = UIManager.getColor("ToolBar.dockingForeground");
        dockingColor = UIManager.getColor("ToolBar.dockingBackground");
        floatingBorderColor = UIManager.getColor("ToolBar.floatingForeground");
        floatingColor = UIManager.getColor("ToolBar.floatingBackground");
        darkShadow = UIManager.getColor("ToolBar.darkShadow");
        highlight = UIManager.getColor("ToolBar.highlight");
        light = UIManager.getColor("ToolBar.light");
        shadow = UIManager.getColor("ToolBar.shadow");
        buttonDarkShadow = UIManager.getColor("Button.darkShadow");
        buttonHighlight = UIManager.getColor("Button.highlight");
        buttonLight = UIManager.getColor("Button.light");
        buttonShadow = UIManager.getColor("Button.shadow");

        setRolloverBorders(toolBar.isRollover());
    }

    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(toolBar);
        installNormalBorders(toolBar);
    }

    protected void installKeyboardActions() {
        SwingUtilities.replaceUIInputMap(toolBar,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                (InputMap)UIManager.get("ToolBar.ancestorInputMap"));

        SwingUtilities.replaceUIActionMap(toolBar, getUIActionMap());
    }

    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIInputMap(toolBar,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
        SwingUtilities.replaceUIActionMap(toolBar, null);
    }

    protected void installListeners() {
        if (toolBarContListener == null) {
            toolBarContListener = createToolBarContListener();
        }
        toolBar.addContainerListener(toolBarContListener);

        if (propertyListener == null) {
            propertyListener = createPropertyListener();
        }
        toolBar.addPropertyChangeListener(propertyListener);

        if (dockingListener == null) {
            dockingListener = createDockingListener();
        }
        toolBar.addMouseListener(dockingListener);
        toolBar.addMouseMotionListener(dockingListener);

        if (toolBarFocusListener == null) {
            toolBarFocusListener = createToolBarFocusListener();
        }
        for (int i = 0; i < toolBar.getComponentCount(); i++) {
            toolBar.getComponentAtIndex(i).addFocusListener(toolBarFocusListener);
        }
    }

    protected void uninstallListeners() {
        toolBar.removeContainerListener(toolBarContListener);
        toolBar.removePropertyChangeListener(propertyListener);
        toolBar.removeMouseListener(dockingListener);
        toolBar.removeMouseMotionListener(dockingListener);
        for (int i = 0; i < toolBar.getComponentCount(); i++) {
            toolBar.getComponentAtIndex(i).removeFocusListener(toolBarFocusListener);
        }
    }

    protected void installNormalBorders(final JComponent c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            setBorderToNormal(c.getComponent(i));
        }
    }

    protected void installRolloverBorders(final JComponent c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            setBorderToRollover(c.getComponent(i));
        }
    }

    protected void installNonRolloverBorders(final JComponent c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            setBorderToNonRollover(c.getComponent(i));
        }
    }

    protected void navigateFocusedComp(final int direction) {
        if (focusedCompIndex == -1) {
            return;
        }

        int delta;
        if (direction == EAST || direction == SOUTH) {
            delta = 1;
        } else {
            delta = toolBar.getComponentCount() - 1;
        }
        int indexToFocus = focusedCompIndex;
        do {
            indexToFocus = (indexToFocus + delta) % toolBar.getComponentCount();
        } while ((!toolBar.getComponentAtIndex(indexToFocus).isFocusable()
                || !toolBar.getComponentAtIndex(indexToFocus).isEnabled())
                && indexToFocus != focusedCompIndex);

        toolBar.getComponentAtIndex(indexToFocus).requestFocus();
    }

    protected void paintDragWindow(final Graphics g) {
        g.setColor(dragWindow.getBackground());
        g.fillRect(0, 0, dragWindow.getWidth(), dragWindow.getHeight());

        g.setColor(dragWindow.getBorderColor());
        g.drawRect(0, 0, dragWindow.getWidth() - 1, dragWindow.getHeight() - 1);
    }

    protected void setBorderToNonRollover(final Component c) {
        if (nonRolloverBorder == null) {
            nonRolloverBorder = createNonRolloverBorder();
        }
        setCustomBorder(c, nonRolloverBorder, false);
    }

    protected void setBorderToNormal(final Component c) {
        if (!(c instanceof AbstractButton)) {
            return;
        }

        restoreBorderAndRolloverEnabled((AbstractButton)c);
    }

    protected void setBorderToRollover(final Component c) {
        if (rolloverBorder == null) {
            rolloverBorder = createRolloverBorder();
        }
        setCustomBorder(c, rolloverBorder, true);
    }

    private void saveBorderAndRolloverEnabled(final AbstractButton b) {
        Boolean isRolloverEnabled =
                (Boolean)b.getClientProperty(SAVED_ROLLOVER_ENABLED_PROPERTY);
        if (isRolloverEnabled == null) {
            b.putClientProperty(SAVED_BORDER_PROPERTY, b.getBorder());
            b.putClientProperty(SAVED_ROLLOVER_ENABLED_PROPERTY,
                                Boolean.valueOf(b.isRolloverEnabled()));
        }
    }

    private void restoreBorderAndRolloverEnabled(final AbstractButton b) {
        Boolean isRolloverEnabled =
                (Boolean)b.getClientProperty(SAVED_ROLLOVER_ENABLED_PROPERTY);
        if (isRolloverEnabled != null) {
            LookAndFeel.installProperty(b, "rolloverEnabled", isRolloverEnabled);
            b.putClientProperty(SAVED_ROLLOVER_ENABLED_PROPERTY, null);
            if (Utilities.isUIResource(b.getBorder())) {
                b.setBorder((Border)b.getClientProperty(SAVED_BORDER_PROPERTY));
            }
            b.putClientProperty(SAVED_BORDER_PROPERTY, null);
        }
    }

    private void setCustomBorder(final Component c, final Border b,
                                 final boolean rolloverEnabled) {
        if (!(c instanceof AbstractButton)) {
            return;
        }

        AbstractButton button = (AbstractButton)c;
        if (Utilities.isUIResource(button.getBorder())) {
            saveBorderAndRolloverEnabled(button);
            button.setBorder(b);
            button.setRolloverEnabled(rolloverEnabled);
        }
    }

    private ActionMap getUIActionMap() {
        if (actionMap != null) {
            return actionMap;
        }

        actionMap = new ActionMapUIResource();
        actionMap.put("navigateRight", new NavigateAction(EAST));
        actionMap.put("navigateLeft", new NavigateAction(WEST));
        actionMap.put("navigateUp", new NavigateAction(NORTH));
        actionMap.put("navigateDown", new NavigateAction(SOUTH));
        return actionMap;
    }

    /**
     * Returns position to dock the toolbar to: NORTH, SOUTH, EAST, WEST.
     * CENTER means that the toolbar cannot be docket at this position.
     */
    private String getDockingConstraints(final Component c, final Point p) {
        Rectangle r = SwingUtilities.getLocalBounds(c);
        int delta = toolBar.getOrientation() == HORIZONTAL
                    ? toolBar.getHeight()
                    : toolBar.getWidth();

        if (!r.contains(p)) {
            return BorderLayout.CENTER;
        }

        if (p.x < delta) {
            return BorderLayout.WEST;
        } else if (p.x > r.width - delta) {
            return BorderLayout.EAST;
        } else if (p.y < delta) {
            return BorderLayout.NORTH;
        } else if (p.y > r.height - delta) {
            return BorderLayout.SOUTH;
        }
        return BorderLayout.CENTER;
    }

    private int calculateOrientation(final String constraint) {
        if (constraint == BorderLayout.EAST
                || constraint == BorderLayout.WEST) {
            return VERTICAL;
        } else if (constraint == BorderLayout.NORTH
                || constraint == BorderLayout.SOUTH
                || constraint == BorderLayout.CENTER) {
            return HORIZONTAL;
        }
        assert false : "invalid constraint";
        return HORIZONTAL;
    }

    private Container getToolBarMainParent() {
        return isFloating() ? parentBeforeFloating : toolBar.getParent();
    }

    private void revalidateContainer(final Container c) {
        if (c instanceof JComponent) {
            ((JComponent)c).revalidate();
        } else {
            c.doLayout();
        }
    }
}
