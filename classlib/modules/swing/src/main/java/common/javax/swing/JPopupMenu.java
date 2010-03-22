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
package javax.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Iterator;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.PopupMenuUI;

import org.apache.harmony.awt.ComponentInternals;
import org.apache.harmony.awt.MouseEventPreprocessor;
import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class JPopupMenu extends JComponent implements Accessible, MenuElement {

    public static class Separator extends JSeparator {
        private final static String UI_CLASS_ID = "PopupMenuSeparatorUI";

        public String getUIClassID() {
            return UI_CLASS_ID;
        }
    }

    // TODO implement accessibility
    protected class AccessibleJPopupMenu extends AccessibleJComponent implements PropertyChangeListener {
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.POPUP_MENU;
        }

        public void propertyChange(final PropertyChangeEvent event) {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
    }

    private static class PopupMouseEventPreprocessor implements MouseEventPreprocessor {
        private final HashSet openedPopups = new HashSet();

        public boolean preprocess(final MouseEvent event) {
            if (event.getID() != MouseEvent.MOUSE_PRESSED || openedPopups.isEmpty()) {
                return true;
            }

            boolean inside = false;
            final Iterator i = openedPopups.iterator();
            while (i.hasNext() && !inside) {
                final JPopupMenu popup = (JPopupMenu)i.next();
                Point localPoint = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), popup);
                inside = popup.contains(localPoint);

                final Component invoker = popup.getInvoker();
                if (invoker != null && Boolean.TRUE.equals(popup.getClientProperty(StringConstants.HIDE_ON_INVOKER_PRESSED_PROPERTY))) {
                    localPoint = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), invoker);
                    inside |= invoker.contains(localPoint);
                }
            }
            if (inside) {
                return true;
            }

            final MenuElement[] selectedPath = MenuSelectionManager.defaultManager().getSelectedPath();
            for (int j = 1; j < selectedPath.length; j++) {
                Component c = selectedPath[j].getComponent();
                if (c != null) {
                    Point localPoint = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), c);
                    inside = c.contains(localPoint);
                    if (inside) {
                        return true;
                    }
                }
            }
            MenuSelectionManager.defaultManager().clearSelectedPath();
            return true;
        }

        public void registerPopup(final JPopupMenu popup) {
            openedPopups.add(popup);
        }

        public void unregisterPopup(final JPopupMenu popup) {
            openedPopups.remove(popup);
        }
    }

    private static int numHWPopups;
    private static Runnable cancelGrabRunnable;

    private final static String UI_CLASS_ID = "PopupMenuUI";

    private static boolean defaultLightWeightPopupEnabled = true;
    private static final PopupMouseEventPreprocessor MOUSE_EVENTS_HANDLER = new PopupMouseEventPreprocessor();

    private SingleSelectionModel selectionModel = new DefaultSingleSelectionModel();
    private String label;
    private Component invoker;
    private boolean borderPainted = true;
    private boolean lightWeightPopupEnabled = getDefaultLightWeightPopupEnabled();
    private Popup popup;
    private final Point location = new Point();

    static {
        ComponentInternals.getComponentInternals().setMouseEventPreprocessor(MOUSE_EVENTS_HANDLER);
    }

    public JPopupMenu() {
        super.setVisible(false);
        updateUI();
    }

    public JPopupMenu(final String text) {
        label = text;
        super.setVisible(false);
        updateUI();
    }

    public AccessibleContext getAccessibleContext() {
        return (accessibleContext == null) ? (accessibleContext = new AccessibleJPopupMenu())
                : accessibleContext;
    }

    protected PropertyChangeListener createActionChangeListener(final JMenuItem item) {
        return (item != null) ? item.createActionPropertyChangeListener(item.getAction()) : null;
    }

    public void addPopupMenuListener(final PopupMenuListener listener) {
        listenerList.add(PopupMenuListener.class, listener);
    }

    public void removePopupMenuListener(final PopupMenuListener listener) {
        listenerList.remove(PopupMenuListener.class, listener);
    }

    public PopupMenuListener[] getPopupMenuListeners() {
        return (PopupMenuListener[])getListeners(PopupMenuListener.class);
    }

    protected void firePopupMenuCanceled() {
        final PopupMenuListener[] listeners = getPopupMenuListeners();
        if (listeners.length == 0){
            return;
        }

        final PopupMenuEvent event = new PopupMenuEvent(this);
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].popupMenuCanceled(event);
        }
    }

    protected void firePopupMenuWillBecomeInvisible() {
        final PopupMenuListener[] listeners = getPopupMenuListeners();
        if (listeners.length == 0){
            return;
        }

        final PopupMenuEvent event = new PopupMenuEvent(this);
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].popupMenuWillBecomeInvisible(event);
        }
    }

    protected void firePopupMenuWillBecomeVisible() {
        final PopupMenuListener[] listeners = getPopupMenuListeners();
        if (listeners.length == 0){
            return;
        }

        final PopupMenuEvent event = new PopupMenuEvent(this);
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].popupMenuWillBecomeVisible(event);
        }
    }

    public JMenuItem add(final Action action) {
        final JMenuItem result = add(createActionComponent(action));
        result.setAction(action);
        return result;
    }

    public JMenuItem add(final String text) {
        return add(new JMenuItem(text));
    }

    public JMenuItem add(final JMenuItem item) {
        return (JMenuItem)super.add(item);
    }

    public void addSeparator() {
        super.add(new JPopupMenu.Separator());
    }

    public void insert(final Component c, final int i) {
        if (i < 0) {
            throw new IllegalArgumentException(Messages.getString("swing.21")); //$NON-NLS-1$
        }
        if (i <= getComponentCount()) {
            super.add(c, i);
        }
    }

    public void insert(final Action action, final int i) {
        if (i < 0) {
            throw new IllegalArgumentException(Messages.getString("swing.21")); //$NON-NLS-1$
        }
        insert(createActionComponent(action), i < getComponentCount() ? i : getComponentCount());
    }

    public void remove(final int i) {
        if (i < 0) {
            throw new IllegalArgumentException(Messages.getString("swing.21")); //$NON-NLS-1$
        }
        if (i >= getComponentCount()) {
            throw new IllegalArgumentException(Messages.getString("swing.22")); //$NON-NLS-1$
        }

        super.remove(i);
    }

    /**
     * @deprecated
     */
    public Component getComponentAtIndex(final int i) {
        return super.getComponent(i);
    }

    public int getComponentIndex(final Component c) {
        for (int i = 0; i < getComponentCount(); i++) {
            if (getComponent(i) == c) {
                return i;
            }
        }
        return -1;
    }

    public MenuElement[] getSubElements() {
        return Utilities.getSubElements(this);
    }

    protected JMenuItem createActionComponent(final Action action) {
        return JMenuItem.createJMenuItem(action);
    }

    public static boolean getDefaultLightWeightPopupEnabled() {
        return defaultLightWeightPopupEnabled;
    }

    public static void setDefaultLightWeightPopupEnabled(final boolean lightWeightPopupEnabled) {
        JPopupMenu.defaultLightWeightPopupEnabled = lightWeightPopupEnabled;
    }

    public boolean isLightWeightPopupEnabled() {
        return lightWeightPopupEnabled;
    }

    public void setLightWeightPopupEnabled(final boolean lightWeightPopupEnabled) {
        this.lightWeightPopupEnabled = lightWeightPopupEnabled;
    }

    public void setInvoker(final Component invoker) {
        this.invoker = invoker;
    }

    public Component getInvoker() {
        return invoker;
    }

    public Component getComponent() {
        return this;
    }

    public void setLabel(final String label) {
        String oldValue = this.label;
        this.label = label;
        firePropertyChange(StringConstants.LABEL_PROPERTY_CHANGED, oldValue, label);
    }

    public String getLabel() {
        return label;
    }

    public Insets getMargin() {
        return new Insets(0, 0, 0, 0);
    }

    public void setBorderPainted(final boolean painted) {
        borderPainted = painted;
    }

    public boolean isBorderPainted() {
        return borderPainted;
    }

    protected void paintBorder(final Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }

    public boolean isPopupTrigger(final MouseEvent event) {
        return getUI().isPopupTrigger(event);
    }

    public void menuSelectionChanged(final boolean isIncluded) {
        if (isIncluded == isVisible()) {
            return;
        }

        if (isIncluded) {
            final Component c = getInvoker();
            if (c instanceof JMenu) {
                ((JMenu)c).setPopupMenuVisible(true);
            }
        } else {
            setVisible(false);
        }
    }

    public void pack() {
        final LayoutManager layout = getLayout();
        if (layout != null) {
            layout.layoutContainer(this);
        }
    }

    public void processKeyEvent(final KeyEvent event,
                                final MenuElement[] path,
                                final MenuSelectionManager msm) {
        // seems like it does no useful work
    }

    public void processMouseEvent(final MouseEvent event,
                                  final MenuElement[] path,
                                  final MenuSelectionManager msm) {
    }

    public void setSelected(final Component selection) {
        if (selectionModel != null) {
            selectionModel.setSelectedIndex(getComponentIndex(selection));
        }
    }

    public void setSelectionModel(final SingleSelectionModel model) {
        selectionModel = model;
    }

    public SingleSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setVisible(final boolean visible) {
        boolean oldValue = isVisible();

        if (visible) {
            super.show();
            MOUSE_EVENTS_HANDLER.registerPopup(this);
        } else {
            super.hide();
            MOUSE_EVENTS_HANDLER.unregisterPopup(this);
        }

        if (getUI() != null) {
            if (visible) {
                popup = getUI().getPopup(this, location.x, location.y);
                firePopupMenuWillBecomeVisible();
                popup.show();
                startMouseGrab();
            } else if (popup != null){
                firePopupMenuWillBecomeInvisible();
                popup.hide();
                endMouseGrab();
            }
        }

        updateSelectionManager(visible);
        firePropertyChange(StringConstants.VISIBLE_PROPERTY_CHANGED, oldValue, visible);
    }

    public void show(final Component invoker, final int x, final int y) {
        setInvoker(invoker);
        Point p = new Point(x, y);
        if (invoker != null) {
            p = adjustPopupLocation(invoker, p);
        }
        setLocation(p.x, p.y);
        setVisible(true);
    }

    public void setLocation(final int x, final int y) {
        location.move(x, y);

        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) {
            w.setLocation(x, y);
        }
    }

    public void setPopupSize(final int width, final int height) {
        setPreferredSize(new Dimension(width, height));
    }

    public void setPopupSize(final Dimension size) {
        setPreferredSize(size);
    }

    public void updateUI() {
        setUI(UIManager.getUI(this));
    }

    public void setUI(final PopupMenuUI ui) {
        super.setUI(ui);
    }

    public PopupMenuUI getUI() {
        return (PopupMenuUI)ui;
    }

    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    private void updateSelectionManager(final boolean visible) {
        final MenuSelectionManager manager = MenuSelectionManager.defaultManager();
        final MenuElement[] oldPath = manager.getSelectedPath();
        MenuElement[] newPath = null;
        if (visible) {
            MenuElement selectableChild = Utilities.getFirstSelectableItem(getSubElements());
            if (Utilities.isEmptyArray(oldPath) && selectableChild != null) {
                newPath = new MenuElement[] {this, selectableChild};
            } else {
                newPath = Utilities.addToPath(oldPath, this);
            }
            manager.setSelectedPath(newPath);
        }
    }

    private void endMouseGrab() {
        if (numHWPopups > 0) {
            numHWPopups--;
        }
        if (numHWPopups == 0) {
            ComponentInternals.getComponentInternals().endMouseGrab();
        }
    }

    private void startMouseGrab() {
        if (numHWPopups == 0) {
            if (cancelGrabRunnable == null) {
                cancelGrabRunnable = new Runnable() {
                    public void run() {
                        if (isVisible()) {
                            MenuSelectionManager.defaultManager().clearSelectedPath();
                        }
                    }
                };
            }
            final Window window = SwingUtilities.getWindowAncestor(this);
            ComponentInternals.getComponentInternals().startMouseGrab(window, cancelGrabRunnable);
        }
        numHWPopups++;
    }

    private Point adjustPopupLocation(final Component invoker, Point p) {
        Point invokerScreenLocation = invoker.getLocationOnScreen();
        p.translate(invokerScreenLocation.x, invokerScreenLocation.y);
        Rectangle bounds = new Rectangle(invokerScreenLocation, invoker.getSize());
        boolean horizontal = (invoker instanceof JMenu) ? !((JMenu)(invoker)).isTopLevelMenu() : false;
        p = Utilities.adjustPopupLocation(p, getPreferredSize(),
                                          bounds, horizontal,
                                          invoker.getGraphicsConfiguration());
        return p;
    }

}
