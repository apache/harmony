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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.event.WindowEvent;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleStateSet;
import org.apache.harmony.x.swing.Utilities;

/**
 * <p>
 * <i>JDialog</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JDialog extends Dialog implements WindowConstants, Accessible, RootPaneContainer {
    private static final long serialVersionUID = -864070866424508218L;

    private static boolean defaultLookAndFeelDecorated;

    /**
     * This class implements accessibility support for <code>JDialog</code>.
     */
    protected class AccessibleJDialog extends AccessibleAWTDialog {
        private static final long serialVersionUID = 7312926302382808523L;

        protected AccessibleJDialog() {
        }

        @Override
        public String getAccessibleName() {
            return getTitle();
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            return super.getAccessibleStateSet();
        }
    }

    protected JRootPane rootPane;

    protected boolean rootPaneCheckingEnabled;

    protected AccessibleContext accessibleContext;

    private int defaultCloseOperation = HIDE_ON_CLOSE;

    /**
     * Constructs a modal or non-modal dialog with the specified title and
     * with the specified owner.
     * If the owner is null, the default shared owner will be used.
     *
     * @param owner - the owner
     * @param title - the title of the dialog
     * @param modal - true for a modal dialog, false for a non-modal dialog
     * @param gc - the GraphicsConfiguration of the target screen device.
     *        If gc is null, GraphicsConfiguration of the owner will be used.
     */
    public JDialog(final Frame owner, final String title, final boolean modal,
            final GraphicsConfiguration gc) {
        super(owner == null ? JFrame.getSharedOwner() : owner, title, modal, gc);
        dialogInit();
    }

    /**
     * Constructs a modal or on-modal dialog with the specified title and with
     * the specified owner.
     *
     * @param owner - the non-null owner
     * @param title - the title of the dialog
     * @param modal - true for a modal dialog, false for a non-modal dialog
     * @param gc - the GraphicsConfiguration of the target screen device.
     *        If gc is null, GraphicsConfiguration of the owner will be used.
     *
     * @throws HeadlessException - if GraphicsEnvironment.isHeadless() returns
     *         true.
     */
    public JDialog(final Dialog owner, final String title, final boolean modal,
            final GraphicsConfiguration gc) throws HeadlessException {
        super(owner, title, modal, gc);
        dialogInit();
    }

    /**
     * Constructs a modal or non-modal dialog with the specified title and with
     * the specified owner.
     * If the owner is null, the default shared owner will be used.
     *
     * @param owner - the owner
     * @param title - the title of the dialog
     * @param modal - true for a modal dialog, false for a non-modal dialog
     *
     * @throws HeadlessException - if GraphicsEnvironment.isHeadless()
     *         returns true.
     */
    public JDialog(final Frame owner, final String title, final boolean modal)
            throws HeadlessException {
        this(owner, title, modal, null);
    }

    /**
     * Constructs a non-modal dialog with the specified title and with
     * the specified owner. If the owner is null, the default shared owner
     * will be used.
     *
     * @param owner - the owner
     * @param title - the title of the dialog
     *
     * @throws HeadlessException - if GraphicsEnvironment.isHeadless()
     *         returns true.
     */
    public JDialog(final Frame owner, final String title) throws HeadlessException {
        this(owner, title, false);
    }

    /**
     * Constructs a modal or on-modal dialog with the specified title and with
     * the specified owner.
     *
     * @param owner - the non-null owner
     * @param title - the title of the dialog
     * @param modal - true for a modal dialog, false for a non-modal dialog
     *
     * @throws HeadlessException - if GraphicsEnvironment.isHeadless()
     *         returns true.
     */
    public JDialog(final Dialog owner, final String title, final boolean modal)
            throws HeadlessException {
        this(owner, title, modal, null);
    }

    /**
     * Constructs a non-modal dialog with the specified title and with
     * the specified owner.
     *
     * @param owner - the non-null owner
     * @param title - the title of the dialog
     *
     * @throws HeadlessException - if GraphicsEnvironment.isHeadless()
     *         returns true.
     */
    public JDialog(final Dialog owner, final String title) throws HeadlessException {
        this(owner, title, false);
    }

    /**
     * Constructs a non-modal dialog without a title and with
     * the specified owner.
     * If the owner is null, the default shared owner will be used.
     *
     * @param owner - the owner
     * @param modal - true for a modal dialog, false for a non-modal dialog
     *
     * @throws HeadlessException - if GraphicsEnvironment.isHeadless()
     *         returns true.
     */
    public JDialog(final Frame owner, final boolean modal) throws HeadlessException {
        this(owner, null, modal);
    }

    /**
     * Constructs a non-modal dialog without a title and with
     * the specified owner.
     * If the owner is null, the default shared owner will be used.
     *
     * @param owner - the owner
     *
     * @throws HeadlessException - if GraphicsEnvironment.isHeadless()
     *         returns true.
     */
    public JDialog(final Frame owner) throws HeadlessException {
        this(owner, null, false);
    }

    /**
     * Constructs a modal or non-modal dialog without a title and with
     * the specified owner.
     *
     * @param owner - the non-null owner
     * @param modal - true for a modal dialog, false for a non-modal dialog
     *
     * @throws HeadlessException - if GraphicsEnvironment.isHeadless()
     *         returns true.
     */
    public JDialog(final Dialog owner, final boolean modal) throws HeadlessException {
        this(owner, null, modal);
    }

    /**
     * Constructs a non-modal dialog without a title and with
     * the specified owner.
     *
     * @param owner - the non-null owner
     *
     * @throws HeadlessException - if GraphicsEnvironment.isHeadless()
     *         returns true.
     */
    public JDialog(final Dialog owner) throws HeadlessException {
        this(owner, null, false);
    }

    /**
     * Constructs a non-modal dialog without a title and without
     * a specified owner. The default shared owner will be used.
     *
     * @throws HeadlessException - if GraphicsEnvironment.isHeadless()
     *         returns true.
     */
    public JDialog() throws HeadlessException {
        this((Frame) null, null, false);
    }

    /**
     *
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
     * Set rootPane property.
     *
     * @param root - new rootPane property value
     */
    protected void setRootPane(final JRootPane root) {
        if (rootPane != null) {
            remove(rootPane);
        }
        rootPane = root;
        if (root != null) {
            super.addImpl(root, null, 0);
        }
    }

    /**
     * Get rootPane property.
     *
     * @return rootPane property
     */
    public JRootPane getRootPane() {
        return rootPane;
    }

    /**
     * Called by the constructors to create the default rootPane property.
     *
     * @return default JRootPane
     */
    protected JRootPane createRootPane() {
        return new JRootPane();
    }

    /**
     * Sets the menu bar for the frame
     *
     * @param menuBar - menu bar to be placed in the frame
     */
    public void setJMenuBar(final JMenuBar menuBar) {
        getRootPane().setJMenuBar(menuBar);
    }

    /**
     * Returns the menu bar for the frame
     *
     * @return the menu bar for the frame
     */
    public JMenuBar getJMenuBar() {
        return getRootPane().getJMenuBar();
    }

    /**
     * Sets layeredPane property.
     *
     * @param layeredPane - new layeredPane property value
     */
    public void setLayeredPane(final JLayeredPane layeredPane) {
        getRootPane().setLayeredPane(layeredPane);
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
     * Returns the accessible context for the dialog.
     *
     * @return the accessible context for the dialog
     */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJDialog();
        }
        return accessibleContext;
    }

    /**
     * Returns string representation of this dialog.
     *
     * @return string representation of this dialog
     */
    @Override
    protected String paramString() {
        String result = super.paramString();
        if (getRootPane() != null) {
            result += ",rootPane=" + getRootPane().toString();
        } else {
            result += ",rootPane=null";
        }
        return result;
    }

    /**
     * Implements actions depending on defaultCloseOperation property.
     *
     * @param e - window event
     */
    @Override
    protected void processWindowEvent(final WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
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
    }

    /**
     *
     */
    @Override
    public void setLayout(final LayoutManager layout) {
        if (isRootPaneCheckingEnabled()) {
            getContentPane().setLayout(layout);
        } else {
            super.setLayout(layout);
        }
    }

    /**
     * Just calls paint(g). This method was overridden to prevent
     * an unnecessary call to clear the background.
     *
     * @param g - the graphics context to paint
     */
    @Override
    public void update(final Graphics g) {
        paint(g);
    }

    /**
     * Sets contentPane property.
     *
     * @param contentPane - new contentPane property value
     */
    public void setContentPane(final Container contentPane) {
        getRootPane().setContentPane(contentPane);
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
     * Set glassPane property.
     *
     * @param glassPane - new glassPane property value
     */
    public void setGlassPane(final Component glassPane) {
        getRootPane().setGlassPane(glassPane);
    }

    /**
     *
     */
    @Override
    public void remove(final Component comp) {
        if (comp == getRootPane()) {
            // remove directly from JDialog
            super.remove(comp);
        } else {
            getContentPane().remove(comp);
        }
    }

    /**
     * Returns glassPane property.
     *
     * @return glassPane property
     */
    public Component getGlassPane() {
        return getRootPane().getGlassPane();
    }

    /**
     * Sets rootPaneCheckingEnabled.
     *
     * @param enabled - new rootPaneCheckingEnabled value
     */
    protected void setRootPaneCheckingEnabled(final boolean enabled) {
        rootPaneCheckingEnabled = enabled;
    }

    /**
     * Sets defaultCloseOperation property.
     *
     * @param operation - new defaultCloseOperation value
     */
    public void setDefaultCloseOperation(final int operation) {
        defaultCloseOperation = operation;
        //super.setDefaultCloseOperation(operation);
    }

    /**
     * Returns rootPaneCheckingEnabled value.
     *
     * @return value of rootPaneCheckingEnabled
     */
    protected boolean isRootPaneCheckingEnabled() {
        return rootPaneCheckingEnabled;
    }

    /**
     * Called by the constructors to init JDialog
     */
    protected void dialogInit() {
        setRootPaneCheckingEnabled(true);
        setRootPane(createRootPane());
        setLocale(JComponent.getDefaultLocale());
        // check isDefaultLookAndFeelDecorated()
        if (isDefaultLookAndFeelDecorated()) {
            setUndecorated(Utilities.lookAndFeelSupportsWindowDecorations());
            getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
        }
        /*
         * Enabling WindowEvents is required for processWindowEvent()
         * to function.
         */
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        /*
         * Enabling KeyEvents is required for events to be propagated over
         * components hierarchy.
         */
        enableEvents(AWTEvent.KEY_EVENT_MASK);
        /*
         * This class is a top level container for all Swing components. So,
         * it has to define a default focus traversal policy.
         */
        setFocusTraversalPolicy(KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .getDefaultFocusTraversalPolicy());
    }

    /**
     * Returns defaultCloseOperation value.
     *
     * @return defaultCloseOperation value
     */
    public int getDefaultCloseOperation() {
        return defaultCloseOperation;
    }

    public static void setDefaultLookAndFeelDecorated(final boolean defaultLookAndFeelDecorated) {
        JDialog.defaultLookAndFeelDecorated = defaultLookAndFeelDecorated;
    }

    public static boolean isDefaultLookAndFeelDecorated() {
        return defaultLookAndFeelDecorated;
    }
}
