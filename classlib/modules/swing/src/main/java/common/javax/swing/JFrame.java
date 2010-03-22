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
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.event.WindowEvent;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleStateSet;
import org.apache.harmony.x.swing.internal.nls.Messages;
import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

/**
 * <p>
 * <i>JFrame</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JFrame extends Frame implements WindowConstants, Accessible, RootPaneContainer {
    private static final long serialVersionUID = -1026528232454752719L;

    public static final int EXIT_ON_CLOSE = 3;

    private static boolean defaultLookAndFeelDecorated;

    private static Frame sharedFrame;

    /**
     * This class implements accessibility support for <code>JFrame</code>.
     */
    protected class AccessibleJFrame extends AccessibleAWTFrame {
        private static final long serialVersionUID = -6604775962178425920L;

        protected AccessibleJFrame() {
            super();
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
     * Constructs a new frame in the specified GraphicsConfiguration and
     * the specified title.
     *
     * @param title - title of the frame; if title is null, an empty title
     *        is assumed
     * @param gc - the GraphicsConfiguration to construct the frame;
     *        if gc is null, the system default GraphicsConfiguration is assumed
     */
    public JFrame(final String title, final GraphicsConfiguration gc) {
        super(title, gc);
        frameInit();
    }

    /**
     * Constructs a new frame with the specified title which is initially
     * invisible.
     *
     * @param title - title of the frame; if title is null, an empty title
     *        is assumed
     *
     * @throws HeadlessException - when GraphicsEnvironment.isHeadless()
     *         returns true
     */
    public JFrame(final String title) throws HeadlessException {
        super(title);
        frameInit();
    }

    /**
     * Constructs a new frame in the specified GraphicsConfiguration and
     * an empty title.
     *
     * @param gc - the GraphicsConfiguration to construct the frame;
     * if gc is null, the system default GraphicsConfiguration is assumed
     */
    public JFrame(final GraphicsConfiguration gc) {
        super(gc);
        frameInit();
    }

    /**
     * Constructs a new frame with an empty title which is initially invisible.
     *
     * @throws HeadlessException - when GraphicsEnvironment.isHeadless()
     *         returns true
     */
    public JFrame() throws HeadlessException {
        frameInit();
    }

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
     * Sets the menu bar for the frame.
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
     * Returns the accessible context for the frame.
     *
     * @return the accessible context for the frame
     */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJFrame();
        }
        return accessibleContext;
    }

    /**
     * Returns string representation of this frame.
     *
     * @return string representation of this frame
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
                case EXIT_ON_CLOSE: // exit
                    System.exit(0);
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
        // Note: differs from JInternalFrame's behavior,
        // how titlePane is removed?
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
     * Sets defaultCloseOperation property. This is a bound property.
     *
     * @param operation - new defaultCloseOperation value
     */
    public void setDefaultCloseOperation(final int operation) {
        int oldOperation = getDefaultCloseOperation();
        switch (operation) {
            case DO_NOTHING_ON_CLOSE:
            case HIDE_ON_CLOSE:
            case DISPOSE_ON_CLOSE:
                defaultCloseOperation = operation;
                break;
            case EXIT_ON_CLOSE:
                SecurityManager security = System.getSecurityManager();
                if (security != null) {
                    security.checkExit(0);
                }
                defaultCloseOperation = operation;
                break;
            default:
                throw new IllegalArgumentException(
                    Messages.getString("swing.B2","defaultCloseOperation", //$NON-NLS-1$ //$NON-NLS-2$
                        "DO_NOTHING_ON_CLOSE, HIDE_ON_CLOSE, DISPOSE_ON_CLOSE, EXIT_ON_CLOSE")); //$NON-NLS-1$ 
        }
        firePropertyChange("defaultCloseOperation", oldOperation, operation);
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
     * Called by the constructors to init JFrame
     */
    protected void frameInit() {
        setRootPaneCheckingEnabled(true);
        setRootPane(createRootPane());
        setLocale(JComponent.getDefaultLocale());
        // check isDefaultLookAndFeelDecorated()
        if (isDefaultLookAndFeelDecorated()) {
            setUndecorated(Utilities.lookAndFeelSupportsWindowDecorations());
            getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
        }
        setBackground(getContentPane().getBackground());
        // enable events
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        enableEvents(AWTEvent.KEY_EVENT_MASK);
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

    @Override
    public void setIconImage(final Image image) {
        Image oldValue = getIconImage();
        super.setIconImage(image);
        firePropertyChange(StringConstants.ICON_IMAGE_PROPERTY, oldValue, image);
    }

    public static void setDefaultLookAndFeelDecorated(final boolean defaultLookAndFeelDecorated) {
        JFrame.defaultLookAndFeelDecorated = defaultLookAndFeelDecorated;
    }

    public static boolean isDefaultLookAndFeelDecorated() {
        return defaultLookAndFeelDecorated;
    }

    /**
     * Returns the frame that is used as a default shared owner for
     * <code>JDialog</code> and <code>JWindow</code>.
     */
    static Frame getSharedOwner() {
        if (sharedFrame == null) {
            sharedFrame = new Frame();
        }
        return sharedFrame;
    }
}
