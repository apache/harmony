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
package javax.swing;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.plaf.DesktopIconUI;
import javax.swing.plaf.InternalFrameUI;
import org.apache.harmony.x.swing.BlitSupport;
import org.apache.harmony.x.swing.StringConstants;

/**
 * <p>
 * <i>JInternalFrame</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JInternalFrame extends JComponent implements Accessible, WindowConstants,
        RootPaneContainer {
    private static final long serialVersionUID = 3837984427982803247L;

    /**
     * This class represents the internal frame when it is iconified.
     */
    public static class JDesktopIcon extends JComponent implements Accessible {
        private static final long serialVersionUID = -4923863980546870453L;

        // The internal frame for this icon.
        private JInternalFrame internalFrame;

        /**
         * Creates the desktop icon for the internal frame.
         *
         * @param frame the internal frame of the created desktop icon
         */
        public JDesktopIcon(final JInternalFrame frame) {
            setLayout(new BorderLayout());
            setVisible(frame.isVisible());
            setInternalFrame(frame);
            updateUIForIcon();
        }

        /**
         * This class implements accessibility support for
         * <code>JDesktopIcon</code>.
         */
        protected class AccessibleJDesktopIcon extends AccessibleJComponent implements
                AccessibleValue {
            private static final long serialVersionUID = -7418555324455474398L;

            private Number currentAccessibleValue = new Integer(0);

            protected AccessibleJDesktopIcon() {
            }

            @Override
            public AccessibleRole getAccessibleRole() {
                return AccessibleRole.DESKTOP_ICON;
            }

            @Override
            public AccessibleValue getAccessibleValue() {
                return this;
            }

            public Number getCurrentAccessibleValue() {
                return currentAccessibleValue;
            }

            public boolean setCurrentAccessibleValue(final Number n) {
                if (n == null) {
                    return false;
                }
                if (n instanceof Integer) {
                    currentAccessibleValue = n;
                } else {
                    // XXX: 1.5 migration: replace this line with the commented line
                    currentAccessibleValue = new Integer(n.intValue());
                    //currentAccessibleValue = Integer.valueOf(n.intValue());
                }
                return true;
            }

            public Number getMinimumAccessibleValue() {
                return MIN_VALUE;
            }

            public Number getMaximumAccessibleValue() {
                return MAX_VALUE;
            }
        }

        /**
         * Sets the UI object for this component.
         *
         * @param ui
         */
        public void setUI(final DesktopIconUI ui) {
            // setUI() from super (JComponent) should always be called
            super.setUI(ui);
        }

        /**
         * Returns the UI object for this component.
         *
         * @return UI object for this component
         */
        public DesktopIconUI getUI() {
            return (DesktopIconUI) ui;
        }

        /**
         * Sets the internal frame for this desktop icon
         *
         * @param frame the internal frame for this desktop icon
         */
        public void setInternalFrame(final JInternalFrame frame) {
            internalFrame = frame;
        }

        /**
         * Returns the internal frame for this desktop icon
         *
         * @return the internal frame for this desktop icon
         */
        public JInternalFrame getInternalFrame() {
            return internalFrame;
        }

        /**
         * Returns the desktop pane that contains the desktop icon.
         *
         * @return the desktop pane that contains the desktop icon
         */
        public JDesktopPane getDesktopPane() {
            // its theoretically possible that
            // this.getInternalFrame().getDesktopIcon() != this;
            // so, we cannot write here just
            // return getInternalFrame().getDesktopPane();
            Container result = SwingUtilities.getAncestorOfClass(JDesktopPane.class, this);

            if (result == null) {
                if(getInternalFrame() != null) {
                    result = getInternalFrame().getDesktopPane();
                }
            }

            return (JDesktopPane) result;
        }

        /**
         * Returns the accessible context for the icon.
         *
         * @return the accessible context for the icon
         */
        @Override
        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleJDesktopIcon();
            }
            return accessibleContext;
        }

        /**
         * Returns the name of the L&F class that renders this component.
         *
         * @return the string "DesktopIconUI"
         */
        @Override
        public String getUIClassID() {
            return "DesktopIconUI";
        }

        /**
         * Updates <code>UI's</code> for both <code>JInternalFrame</code>
         * and <code>JDesktopIcon</code>.
         */
        @Override
        public void updateUI() {
            updateUIForIcon();
            getInternalFrame().updateUI();
        }

        void updateUIForIcon() {
            setUI((DesktopIconUI) UIManager.getUI(this));
        }
    }

    /**
     * The name of the bound property.
     */
    public static final String CONTENT_PANE_PROPERTY = "contentPane";

    /**
     * The name of the bound property.
     */
    public static final String MENU_BAR_PROPERTY = "JMenuBar";

    /**
     * The name of the bound property.
     */
    public static final String TITLE_PROPERTY = "title";

    /**
     * The name of the bound property.
     */
    public static final String LAYERED_PANE_PROPERTY = "layeredPane";

    /**
     * The name of the bound property.
     */
    public static final String ROOT_PANE_PROPERTY = "rootPane";

    /**
     * The name of the bound property.
     */
    public static final String GLASS_PANE_PROPERTY = "glassPane";

    /**
     * The name of the bound property.
     */
    public static final String FRAME_ICON_PROPERTY = "frameIcon";

    /**
     * The name of the constrained property which indicates whether
     * the internal frame is selected.
     */
    public static final String IS_SELECTED_PROPERTY = "selected";

    /**
     * The name of the constrained property which indicates whether
     * the internal frame is closed.
     */
    public static final String IS_CLOSED_PROPERTY = "closed";

    /**
     * The name of the constrained property which indicates whether
     * the internal frame is maximized.
     */
    public static final String IS_MAXIMUM_PROPERTY = "maximum";

    /**
     * The name of the constrained property which indicates whether
     * the internal frame is iconified.
     */
    public static final String IS_ICON_PROPERTY = "icon";

    private static final Integer MIN_VALUE = new Integer(Integer.MIN_VALUE);

    private static final Integer MAX_VALUE = new Integer(Integer.MAX_VALUE);

    /**
     * <code>JRootPane</code> containter of the internal frame.
     */
    protected JRootPane rootPane;

    protected boolean rootPaneCheckingEnabled;

    /**
     * The frame can be closed.
     */
    protected boolean closable;

    /**
     * The frame is closed.
     */
    protected boolean isClosed = false;

    /**
     * The frame can be maximized.
     */
    protected boolean maximizable;

    /**
     * The frame is maximized.
     */
    protected boolean isMaximum;

    /**
     * The frame can be icinified.
     */
    protected boolean iconable;

    /**
     * The frame is iconified.
     */
    protected boolean isIcon;

    /**
     * The frame is resizable.
     */
    protected boolean resizable;

    /**
     * The frame is selected now.
     */
    protected boolean isSelected;

    /**
     * The icon shown in the top-left corner of the frame.
     */
    protected Icon frameIcon;

    /**
     * The title of the frame.
     */
    protected String title = "";

    /**
     * The icon that is displayed when the frame is iconified.
     */
    protected JDesktopIcon desktopIcon;

    // normalBounds property
    private Rectangle normalBounds = new Rectangle();

    // defauld close operation
    private int defaultCloseOperation = DISPOSE_ON_CLOSE;

    // most recent focus owner
    private Component mostRecentFocusOwner;

    // shows if the internal frame is opened the first time
    private boolean firstTimeOpen = true;

    private BlitSupport blitSupport;

    /**
     * Constructs a non-resizable, non-closable, non-maximizable,
     * non-iconifiable internal frame with no title.
     */
    public JInternalFrame() {
        this("", false, false, false, false);
    }

    /**
     * Constructs a non-resizable, non-closable, non-maximizable,
     * non-iconifiable internal frame with the specified title.
     *
     * @param title the title of the internal frame
     */
    public JInternalFrame(final String title) {
        this(title, false, false, false, false);
    }

    /**
     * Constructs a non-closable, non-maximizable, non-iconifiable internal
     * frame with the specified title and resizability.
     *
     * @param title the title of the internal frame
     * @param resizable
     */
    public JInternalFrame(final String title, final boolean resizable) {
        this(title, resizable, false, false, false);
    }

    /**
     * Constructs a non-maximizable, non-iconifiable internal frame
     * with the specified title, resizability and closability.
     *
     * @param title the title of the internal frame
     * @param resizable
     * @param closable
     */
    public JInternalFrame(final String title, final boolean resizable, final boolean closable) {
        this(title, resizable, closable, false, false);
    }

    /**
     * Constructs a non-iconifiable internal frame with the specified title,
     * resizability, closability and maximizability.
     *
     * @param title the title of the internal frame
     * @param resizable
     * @param closable
     * @param maximizable
     */
    public JInternalFrame(final String title, final boolean resizable, final boolean closable,
            final boolean maximizable) {
        this(title, resizable, closable, maximizable, false);
    }

    /**
     * Constructs an internal frame with the specified title,
     * resizability, closability, maximizability and iconifiability.
     *
     * @param title the title of the internal frame
     * @param resizable
     * @param closable
     * @param maximizable
     * @param iconifiable
     */
    public JInternalFrame(final String title, final boolean resizable, final boolean closable,
            final boolean maximizable, final boolean iconifiable) {
        //super.hide();
        super.setVisible(false);
        this.title = title;
        this.resizable = resizable;
        this.closable = closable;
        this.maximizable = maximizable;
        this.iconable = iconifiable;
        // from frameInit()
        setRootPane(createRootPane());
        setLocale(JComponent.getDefaultLocale());
        // check isDefaultLookAndFeelDecorated()
        //if (isDefaultLookAndFeelDecorated()) {
        //    setUndecorated(true);
        //getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
        // }
        setBackground(getContentPane().getBackground());
        // enable events
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        enableEvents(AWTEvent.KEY_EVENT_MASK);
        setFocusTraversalPolicy(KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .getDefaultFocusTraversalPolicy());
        updateUI();
        this.desktopIcon = new JDesktopIcon(this);
        setRootPaneCheckingEnabled(true);
        // non-selected internalFrame must have visible glassPane
        getGlassPane().setVisible(true);
        // just to be sure
        super.setFocusCycleRoot(true);
    }

    /**
     * This class implements accessibility support for
     * <code>JInternalFrame</code>.
     */
    protected class AccessibleJInternalFrame extends AccessibleJComponent implements
            AccessibleValue {
        private static final long serialVersionUID = 8391910997005202445L;

        private Number currentAccessibleValue = new Integer(0);

        protected AccessibleJInternalFrame() {
        }

        @Override
        public String getAccessibleName() {
            return getTitle();
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.INTERNAL_FRAME;
        }

        @Override
        public AccessibleValue getAccessibleValue() {
            return this;
        }

        public Number getCurrentAccessibleValue() {
            return currentAccessibleValue;
        }

        public boolean setCurrentAccessibleValue(final Number n) {
            if (n == null) {
                return false;
            }
            if (n instanceof Integer) {
                currentAccessibleValue = n;
            } else {
                // XXX: 1.5 migration: replace this line with the commented line
                currentAccessibleValue = new Integer(n.intValue());
                //currentAccessibleValue = Integer.valueOf(n.intValue());
            }
            return true;
        }

        public Number getMinimumAccessibleValue() {
            return MIN_VALUE;
        }

        public Number getMaximumAccessibleValue() {
            return MAX_VALUE;
        }
    }

    /**
     * Children may not be added directly to the internal frame by default.
     * They must be added to its <code>contentPane</code>.
     *
     * @param comp - the component to be added
     * @param constraints - the constraints to be kept
     * @param index - the index
     */
    @Override
    protected void addImpl(final Component comp, final Object constraints, final int index) {
        if (isRootPaneCheckingEnabled()) {
            getContentPane().add(comp, constraints, index);
            return;
        }
        super.addImpl(comp, constraints, index);
    }

    /**
     * Sets the UI object for this component.
     *
     * @param ui the UI object to set
     */
    public void setUI(final InternalFrameUI ui) {
        // setUI() from super (JComponent) should always be called
        // strange manipulations with 'enabled', but they are necessary
        boolean enabled = isRootPaneCheckingEnabled();
        setRootPaneCheckingEnabled(false);
        super.setUI(ui);
        setRootPaneCheckingEnabled(enabled);
    }

    /**
     * Returns the UI object for this component.
     *
     * @return UI object for this component
     */
    public InternalFrameUI getUI() {
        return (InternalFrameUI) ui;
    }

    /**
     * Updates <code>UI's</code> for both <code>JInternalFrame</code>
     * and <code>JDesktopIcon</code>.
     */
    @Override
    public void updateUI() {
    	setUI((InternalFrameUI) UIManager.getUI(this));
    	
        if (getDesktopIcon() != null) {
            getDesktopIcon().updateUIForIcon();
        }
    }

    /**
     * Removes the internal frame listener.
     *
     * @param l internal frame listener to remove
     */
    public void removeInternalFrameListener(final InternalFrameListener l) {
        listenerList.remove(InternalFrameListener.class, l);
    }

    /**
     * Adds the internal frame listener.
     *
     * @param l internal frame listener to add
     */
    public void addInternalFrameListener(final InternalFrameListener l) {
        listenerList.add(InternalFrameListener.class, l);
    }

    /**
     * Returns all registered internal frame listeners.
     *
     * @return all registered internal frame listeners
     */
    public InternalFrameListener[] getInternalFrameListeners() {
        return listenerList.getListeners(InternalFrameListener.class);
    }

    /**
     * Fires the internal frame event.
     *
     * @param id identifier of the event to fire
     */
    protected void fireInternalFrameEvent(final int id) {
        Object[] listeners = listenerList.getListenerList();
        InternalFrameEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == InternalFrameListener.class) {
                if (e == null) {
                    e = new InternalFrameEvent(this, id);
                }
                InternalFrameListener l = (InternalFrameListener) listeners[i + 1];
                switch (id) {
                    case InternalFrameEvent.INTERNAL_FRAME_ACTIVATED:
                        l.internalFrameActivated(e);
                        break;
                    case InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED:
                        l.internalFrameDeactivated(e);
                        break;
                    case InternalFrameEvent.INTERNAL_FRAME_ICONIFIED:
                        l.internalFrameIconified(e);
                        break;
                    case InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED:
                        l.internalFrameDeiconified(e);
                        break;
                    case InternalFrameEvent.INTERNAL_FRAME_CLOSING:
                        l.internalFrameClosing(e);
                        break;
                    case InternalFrameEvent.INTERNAL_FRAME_OPENED:
                        l.internalFrameOpened(e);
                        break;
                    case InternalFrameEvent.INTERNAL_FRAME_CLOSED:
                        l.internalFrameClosed(e);
                        break;
                }
            }
        }
    }

    /**
     * Set rootPane property.
     *
     * @param root the new rootPane property value
     */
    protected void setRootPane(final JRootPane root) {
        JRootPane oldValue = getRootPane();
        if (rootPane != null) {
            remove(rootPane);
        }
        rootPane = root;
        if (root != null) {
            super.addImpl(root, null, 0);
        }
        firePropertyChange(ROOT_PANE_PROPERTY, oldValue, root);
    }

    /**
     * Get rootPane property.
     *
     * @return rootPane property
     */
    @Override
    public JRootPane getRootPane() {
        return rootPane;
    }

    /**
     * Called by the constructors to create the default <code>rootPane</code>
     * property.
     *
     * @return default <code>rootPane</code>
     */
    protected JRootPane createRootPane() {
        return new JRootPane();
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void setMenuBar(final JMenuBar menuBar) {
        setJMenuBar(menuBar);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public JMenuBar getMenuBar() {
        return getJMenuBar();
    }

    /**
     * Sets the menu bar for the frame.
     *
     * @param menuBar the menu bar to be placed in the frame
     */
    public void setJMenuBar(final JMenuBar menuBar) {
        JMenuBar oldValue = getJMenuBar();
        getRootPane().setJMenuBar(menuBar);
        firePropertyChange(MENU_BAR_PROPERTY, oldValue, menuBar);
    }

    /**
     * Returns the menu bar for the frame.
     *
     * @return the menu bar for the frame
     */
    public JMenuBar getJMenuBar() {
        return getRootPane().getJMenuBar();
    }

    /**
     * Sets layeredPane property. This is a bound property.
     *
     * @param layeredPane the new layeredPane property value
     */
    public void setLayeredPane(final JLayeredPane layeredPane) {
        JLayeredPane oldValue = getLayeredPane();
        getRootPane().setLayeredPane(layeredPane);
        firePropertyChange(LAYERED_PANE_PROPERTY, oldValue, layeredPane);
    }

    /**
     * Returns layeredPane property.
     *
     * @return layeredPane property
     */
    public JLayeredPane getLayeredPane() {
        return getRootPane().getLayeredPane();
    }

    /**
     * Sets the desktop icon to be used when the internal frame is iconified.
     *
     * @param icon the desktop icon to be used when the internal frame is
     *             iconified
     */
    public void setDesktopIcon(final JDesktopIcon icon) {
        desktopIcon = icon;
    }

    /**
     * Returns the desktop icon used when the internal frame is iconified.
     *
     * @return  the desktop icon used when the internal frame is iconified
     */
    public JDesktopIcon getDesktopIcon() {
        return desktopIcon;
    }

    /**
     * Returns the desktop pane that contains the internal frame
     * or <code>desktoIcon</code>.
     *
     * @return desktop pane that contains the internal frame or
     * <code>desktoIcon</code>
     */
    public JDesktopPane getDesktopPane() {
        Container result = SwingUtilities.getAncestorOfClass(JDesktopPane.class, this);
        if (result == null) {
            result = SwingUtilities.getAncestorOfClass(JDesktopPane.class, getDesktopIcon());
        }
        return (JDesktopPane) result;
    }

    /**
     * Returns the accessible context for the internal frame.
     *
     * @return the accessible context for the internal frame
     */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJInternalFrame();
        }
        return accessibleContext;
    }

    /**
     * Sets the icon to be displayed in the title bar of the internal frame.
     *
     * @param icon the icon to be displayed in the title bar of the internal
     *             frame
     */
    public void setFrameIcon(final Icon icon) {
        Icon oldValue = getFrameIcon();
        frameIcon = icon;
        firePropertyChange(FRAME_ICON_PROPERTY, oldValue, frameIcon);
    }

    /**
     * Returns the icon displayed in the title bar of the internal frame.
     *
     * @return the icon displayed in the title bar of the internal frame
     */
    public Icon getFrameIcon() {
        return frameIcon;
    }

    /**
     * Sets the title of the frame.
     * <code>title</code> can be <code>null</code>.
     * This is a bound property.
     *
     * @param title the title of the frame to be set
     */
    public void setTitle(final String title) {
        String oldValue = getTitle();
        this.title = title;
        firePropertyChange(TITLE_PROPERTY, oldValue, title);
    }

    /**
     * Returns the title of the frame.
     *
     * @return the title of the frame
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns string representation of this frame.
     *
     * @return string representation of this frame
     */
    @Override
    protected String paramString() {
        return super.paramString();
    }

    /**
     * Gets the warning string that is displayed with this internal frame.
     * Since internal frame is always fully contained within a window, this
     * method always returns <code>null</code>.
     *
     * @return <code>null</code>
     */
    public final String getWarningString() {
        return null;
    }

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string "InternalFrameUI"
     */
    @Override
    public String getUIClassID() {
        return "InternalFrameUI";
    }

    /**
     * Sets the layer attribute of the internal frame.
     *
     * @param layer the value of layer attribute to be set
     */
    public void setLayer(final Integer layer) {
        setLayer(layer.intValue());
    }

    /**
     * Sets the layer attribute of the internal frame. The method
     * <code>setLayer(Integer)</code> should be used for layer values
     * predefined in <code>JLayeredPane</code>.
     *
     * @param layer the value of layer attribute to be set
     */
    public void setLayer(final int layer) {
        //if (getDesktopPane() == null) {
        if (getParent() instanceof JLayeredPane) {
            ((JLayeredPane) getParent()).setLayer(this, layer);
        } else {
            JLayeredPane.putLayer(this, layer);
        }
    }

    /**
     * Returns the layer attribute of the internal frame.
     *
     * @return the layer attribute of the internal frame
     */
    public int getLayer() {
        return JLayeredPane.getLayer(this);
    }

    /**
     * Sets normal bounds of this internal frame, the bounds that this frame
     * will be restored to from its maximized state.
     *
     * @param rect - normal bounds value
     */
    public void setNormalBounds(final Rectangle rect) {
        normalBounds = rect;
    }

    /**
     * Returns the normal bounds of this internal frame.
     *
     * @return the normal bounds of this internal frame
     */
    public Rectangle getNormalBounds() {
        return normalBounds;
    }

    @Override
    public void setLayout(final LayoutManager layout) {
        if (isRootPaneCheckingEnabled()) {
            getContentPane().setLayout(layout);
        } else {
            super.setLayout(layout);
        }
    }

    /**
     * Is overridden to allow optimized painting when the internal
     * frame is being dragged.
     *
     * @param g the <code>Graphics</code> object to paint
     */
    @Override
    protected void paintComponent(final Graphics g) {
        if (blitSupport != null) {
            blitSupport.onPaint();
        }
        super.paintComponent(g);
    }

    /**
     * Sets contentPane property. This is a bound property.
     *
     * @param contentPane the new contentPane property value
     */
    public void setContentPane(final Container contentPane) {
        Container oldValue = getContentPane();
        getRootPane().setContentPane(contentPane);
        firePropertyChange(CONTENT_PANE_PROPERTY, oldValue, contentPane);
    }

    /**
     * Returns the contentPane property.
     *
     * @return the contentPane property
     */
    public Container getContentPane() {
        return getRootPane().getContentPane();
    }

    /**
     * Set glassPane property. This is a bound property.
     *
     * @param glassPane the new glassPane property value
     */
    public void setGlassPane(final Component glassPane) {
        Component oldValue = getGlassPane();
        getRootPane().setGlassPane(glassPane);
        firePropertyChange(GLASS_PANE_PROPERTY, oldValue, glassPane);
    }

    /**
     * Returns glassPane property.
     *
     * @return glassPane property
     */
    public Component getGlassPane() {
        return getRootPane().getGlassPane();
    }

    @Override
    public void remove(final Component comp) {
        if (comp.getParent() == this) {
            // remove directly from JInternalFrame
            super.remove(comp);
        } else {
            getContentPane().remove(comp);
        }
    }

    /*
     * Remembers the focused component when the internal frame is unselected.
     */
    private void setMostRecentFocusOwner(final Component c) {
        // we really need this 'if' before the assignment.
        // Suppose we have JInternalFrame on
        // JDesktopPane inside of some window, which has JMenu.
        // If the user selects menu, JInternalFrame becomes unselected,
        // but this method is call with c == JRootPane of the global window.
        // We must not remember this component as mostRecentFocusOwner.
        if (this.isAncestorOf(c)) {
            mostRecentFocusOwner = c;
        }
    }

    /**
     * Returns the component that will receive the focus when the
     * internal frame is selected.
     *
     * @return the component that will receive the focus when the
     *         internal frame is selected
     */
    public Component getMostRecentFocusOwner() {
        if (isSelected()) {
            return getFocusOwner();
        }
        if (mostRecentFocusOwner != null) {
            return mostRecentFocusOwner;
        }
        Component result = null;
        if (getFocusTraversalPolicy() instanceof InternalFrameFocusTraversalPolicy) {
            result = ((InternalFrameFocusTraversalPolicy) getFocusTraversalPolicy())
                    .getInitialComponent(this);
        }
        if (result == null) {
            return getFocusTraversalPolicy().getDefaultComponent(this);
        }
        return result;
    }

    /**
     * Returns the component that currently has the focus.
     *
     * @return the component that currently has the focus, if the internal
     *         frame is selected; otherwise returns <code>null</code>
     */
    public Component getFocusOwner() {
        if (!isSelected()) {
            return null;
        }
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .getFocusOwner();
        return isAncestorOf(focusOwner) ? focusOwner : null;
    }

    /**
     * Restores focus to the last subcomponent that had focus.
     */
    public void restoreSubcomponentFocus() {
        Component comp = getMostRecentFocusOwner();
        if (comp != null) {
            comp.requestFocus();
        } else {
            getContentPane().requestFocus();
        }
    }

    /**
     * Selects or deselects the internal frame. If the internal frame is selected,
     * <code>InternalFrameEvent.INTERNAL_FRAME_ACTIVATED</code> event is fired.
     * If the internal frame is deselected,
     * <code>InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED</code> event is fired.
     *
     * @param b shows whether select or deselect the internal frame
     *
     * @throws PropertyVetoException if the attempt to set the property is vetoed
     */
    public void setSelected(final boolean b) throws PropertyVetoException {
        if (b && !isShowing() && !getDesktopIcon().isShowing()) {
            return; // can't select if the internal frame is not showing
        }
        if (b == isSelected()) {
            return;
        }
        Boolean oldValue = Boolean.valueOf(isSelected());
        Boolean newValue = Boolean.valueOf(b);
        fireVetoableChange(IS_SELECTED_PROPERTY, oldValue, newValue);
        // remember the focused component for the deactivated frame;
        // recall the last focused component for the activated frame;
        setMostRecentFocusOwner(getMostRecentFocusOwner());
        isSelected = b;
        if (isSelected()) {
            isSelected = false;
            restoreSubcomponentFocus();
            isSelected = true;
        }
        firePropertyChange(IS_SELECTED_PROPERTY, oldValue, newValue);
        // fire internal frame events
        fireInternalFrameEvent(isSelected() ? InternalFrameEvent.INTERNAL_FRAME_ACTIVATED
                : InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED);
    }

    /**
     * Returns whether the internal frame is selected.
     *
     * @return whether the internal frame is selected
     */
    public boolean isSelected() {
        return isSelected;
    }

    /**
     * Sets <code>rootPaneCheckingEnabled</code> property.
     *
     * @param enabled the new <code>rootPaneCheckingEnabled</code> value
     */
    protected void setRootPaneCheckingEnabled(final boolean enabled) {
        LookAndFeel.markPropertyNotInstallable(this, "rootPaneCheckingEnabled");
        rootPaneCheckingEnabled = enabled;
    }

    /**
     * Returns <code>rootPaneCheckingEnabled</code> value.
     *
     * @return the value of <code>rootPaneCheckingEnabled</code>
     */
    protected boolean isRootPaneCheckingEnabled() {
        return rootPaneCheckingEnabled;
    }

    /**
     * Sets the <code>resizable</code> property.
     *
     * @param b new value for the <code>resizable</code> property
     */
    public void setResizable(final boolean b) {
        boolean oldValue = isResizable();
        resizable = b;
        LookAndFeel.markPropertyNotInstallable(this,
                StringConstants.INTERNAL_FRAME_RESIZABLE_PROPERTY);
        firePropertyChange(StringConstants.INTERNAL_FRAME_RESIZABLE_PROPERTY, oldValue, b);
    }

    /**
     * Get the <code>resizable</code> property value.
     *
     * @return the <code>resizable</code> property value
     */
    public boolean isResizable() {
        return resizable;
    }

    /**
     * Maximizes or restores the internal frame.
     *
     * @param b indicates whether to maximize or restore the internal frame
     *
     * @throws PropertyVetoException
     */
    public void setMaximum(final boolean b) throws PropertyVetoException {
        Boolean oldValue = Boolean.valueOf(isMaximum());
        Boolean newValue = Boolean.valueOf(b);
        fireVetoableChange(IS_MAXIMUM_PROPERTY, oldValue, newValue);
        if (b && !isMaximum()) {
            setNormalBounds(getBounds());
        }
        isMaximum = b;
        firePropertyChange(IS_MAXIMUM_PROPERTY, oldValue, newValue);
    }

    /**
     * Returns whether the internal frame is currently maximized.
     *
     * @return whether the internal frame is currently maximized
     */
    public boolean isMaximum() {
        return isMaximum;
    }

    /**
     * Sets the <code>maximizable</code> property.
     *
     * @param b the new value for the <code>maximizable</code> property
     */
    public void setMaximizable(final boolean b) {
        boolean oldValue = isMaximizable();
        maximizable = b;
        LookAndFeel.markPropertyNotInstallable(this,
                StringConstants.INTERNAL_FRAME_MAXIMIZABLE_PROPERTY);
        firePropertyChange(StringConstants.INTERNAL_FRAME_MAXIMIZABLE_PROPERTY, oldValue, b);
    }

    /**
     * Get the <code>maximizable</code> property value.
     *
     * @return the <code>maximizable</code> property value
     */
    public boolean isMaximizable() {
        return maximizable;
    }

    /**
     * Sets the <code>iconable</code> property.
     *
     * @param b new value for the <code>iconable</code> property
     */
    public void setIconifiable(final boolean b) {
        boolean oldValue = isIconifiable();
        iconable = b;
        LookAndFeel.markPropertyNotInstallable(this,
                StringConstants.INTERNAL_FRAME_ICONABLE_PROPERTY);
        firePropertyChange(StringConstants.INTERNAL_FRAME_ICONABLE_PROPERTY, oldValue, b);
    }

    /**
     * Get the <code>iconable</code> property value.
     *
     * @return the <code>iconable</code> property value
     */
    public boolean isIconifiable() {
        return iconable;
    }

    /**
     * Iconifies or deiconifies the internal frame, if the L&F supports iconification.
     * If the internal frame state is iconified, this method fires an
     * <code>INTERNAL_FRAME_ICONIFIED</code> event.
     * If the internal frame is deiconified, an <code>INTERNAL_FRAME_DEICONIFIED</code>
     * event is fired.
     *
     * @param b shows whether iconify or deiconify the internal frame
     *
     * @throws PropertyVetoException if the attempt to set the property is vetoed
     */
    public void setIcon(final boolean b) throws PropertyVetoException {
        Boolean oldValue = Boolean.valueOf(isIcon());
        Boolean newValue = Boolean.valueOf(b);
        fireVetoableChange(IS_ICON_PROPERTY, oldValue, newValue);
        isIcon = b;
        firePropertyChange(IS_ICON_PROPERTY, oldValue, newValue);
        // fire internal frame events
        if (oldValue.booleanValue() != b) {
            if (oldValue.booleanValue()) {
                fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED);
            } else {
                fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_ICONIFIED);
            }
        }
    }

    /**
     * Returns whether the frame is currently iconified.
     *
     * @return whether the frame is currently iconified
     */
    public boolean isIcon() {
        return isIcon;
    }

    /**
     * Does nothing because <code>JInternalFrame</code> must be always
     * a focus cycle root.
     *
     * @param b the value is ignored
     */
    @Override
    public final void setFocusCycleRoot(final boolean b) {
        // do nothing
    }

    /**
     * Always returns true because <code>JInternalFrame</code> is always
     * a focus cycle root.
     *
     * @return true
     */
    @Override
    public final boolean isFocusCycleRoot() {
        return true;
    }

    /**
     * Always returns null because <code>JInternalFrame</code> is always
     * a focus cycle root.
     *
     * @return null
     */
    @Override
    public final Container getFocusCycleRootAncestor() {
        return null;
    }

    /**
     * Closes the internal frame if <code>b</code> is <code>true</code>.
     *
     * @param b must be <code>true</code>
     *
     * @throws PropertyVetoException if the attempt to close the internal frame
     *         is vetoed
     */
    public void setClosed(final boolean b) throws PropertyVetoException {
        if (isClosed() || !b) {
            return;
        }
        fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSING);
        fireVetoableChange(IS_CLOSED_PROPERTY, Boolean.valueOf(isClosed()), Boolean.valueOf(b));
        dispose();
    }

    /**
     * Returns <code>true</code> if the internal frame is closed, otherwise
     * returns <code>false</code>.
     *
     * @return <code>true</code> if the internal frame is closed, otherwise
     *         returns <code>false</code>
     */
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Sets the <code>closable</code> property.
     *
     * @param b new value for the <code>closable</code> property
     */
    public void setClosable(final boolean b) {
        boolean oldValue = isClosable();
        closable = b;
        LookAndFeel.markPropertyNotInstallable(this,
                StringConstants.INTERNAL_FRAME_CLOSABLE_PROPERTY);
        firePropertyChange(StringConstants.INTERNAL_FRAME_CLOSABLE_PROPERTY, oldValue, b);
    }

    /**
     * Get the <code>closable</code> property value.
     *
     * @return the <code>closable</code> property value
     */
    public boolean isClosable() {
        return closable;
    }

    /**
     * Sets the <code>defaultCloseOperation</code> property.
     *
     * @param operation the new <code>defaultCloseOperation</code> value
     */
    public void setDefaultCloseOperation(final int operation) {
        defaultCloseOperation = operation;
    }

    /**
     * Returns <code>defaultCloseOperation</code> value.
     *
     * @return the <code>defaultCloseOperation</code> value
     */
    public int getDefaultCloseOperation() {
        return defaultCloseOperation;
    }

    /**
     * Fires an <code>INTERNAL_FRAME_CLOSING</code> event and then performs
     * the action defined by the default close operation.
     */
    public void doDefaultCloseAction() {
        fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSING);
        switch (getDefaultCloseOperation()) {
            case DISPOSE_ON_CLOSE: // dispose
                dispose();
                break;
            case HIDE_ON_CLOSE: // hide
                setVisible(false);
                break;
            case DO_NOTHING_ON_CLOSE: // do nothing
                break;
        }
    }

    /**
     * Moves the internal frame to the position 0 if its parent is
     * <code>JLayeredPane</code>
     */
    public void moveToFront() {
        if (getParent() instanceof JLayeredPane) {
            ((JLayeredPane) getParent()).setPosition(this, 0);
        }
    }

    /**
     * Moves the internal frame to the position -1 if its parent is
     * <code>JLayeredPane</code>
     */
    public void moveToBack() {
        if (getParent() instanceof JLayeredPane) {
            ((JLayeredPane) getParent()).setPosition(this, -1);
        }
    }

    /**
     * Moves the internal frame to the front.
     */
    public void toFront() {
        moveToFront();
        // Note: is there any difference between moveToFront() and toFront()
    }

    /**
     * Moves the internal frame to the back.
     */
    public void toBack() {
        moveToBack();
        // Note: is there any difference between moveToBack() and toBack()
    }

    /**
     * Subcomponents are layed out to their preferred sizes.
     */
    public void pack() {
        setSize(getPreferredSize());
        doLayout();
    }

    /**
     * If the internal frame is not visible, moves it to the front,
     * makes it visible, and attempts to select it. If the internal frame
     * is shown the first time, <code>INTERNAL_FRAME_OPENED</code> event
     * is fired. The method does nothing if the internal frame is already
     * visible.
     */
    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public void show() {
        if (isVisible()) {
            return;
        }
        if (blitSupport == null) {
            blitSupport = new BlitSupport(this);
        }
        if (getDesktopIcon() != null) {
            getDesktopIcon().setVisible(true);
        }
        moveToFront();
        // Note: how to set isVisible to true without calling of obsolete method?
        // cannot use super.setVisibile(true) - stack overflow will occur
        super.show();
        try {
            setSelected(true);
        } catch (final PropertyVetoException e) {
        }
        // fire INTERNAL_FRAME_OPENED when opening the first time
        if (firstTimeOpen) {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_OPENED);
            firstTimeOpen = false;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public void hide() {
        super.hide();
        // the desktop icon becomes visible when the internal frame becomes
        // visible; the same is correct for hiding
        if (getDesktopIcon() != null) {
            getDesktopIcon().setVisible(false);
        }
    }

    /**
     * Makes the internal frame invisible, unselected and closed. If
     * the internal fram was not already closed, <code>INTERNAL_FRAME_CLOSED</code>
     * event is fired.
     */
    public void dispose() {
        setVisible(false);
        try {
            setSelected(false);
        } catch (final PropertyVetoException e) {
        }
        boolean oldValue = isClosed();
        isClosed = true;
        firePropertyChange(IS_CLOSED_PROPERTY, oldValue, true);
        if (!oldValue) {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSED);
        }
    }

    @Override
    public void setBounds(final int x, final int y, final int w, final int h) {
        Dimension oldSize = getSize();
        super.setBounds(x, y, w, h);
        if (oldSize.width != w || oldSize.height != h) {
            validate();
            return;
        }
        if (blitSupport != null) {
            blitSupport.paint();
        }
    }
}
