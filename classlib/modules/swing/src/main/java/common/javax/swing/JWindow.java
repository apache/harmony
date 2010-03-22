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

package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Window;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * <code>JWindow</code> implements a top-level container without
 * any decorations.
 *
 */
public class JWindow extends Window implements Accessible, RootPaneContainer {

    protected JRootPane rootPane;
    protected boolean rootPaneCheckingEnabled;
    protected AccessibleContext accessibleContext;

    /**
     * Constructs a new window in the specified GraphicsConfiguration with
     * the specified owner.
     *
     * @param window - owner of the window
     * @param gc - the GraphicsConfiguration to construct the frame;
     * if gc is null, the system default GraphicsConfiguration is assumed
     */
    public JWindow(final Window window, final GraphicsConfiguration gc) {
        super(window == null ? JFrame.getSharedOwner() : window, gc);
        windowInit();
    }

    /**
     * Constructs a new window (initially invisible) with the specified owner.
     * If owner is null, the shared owner will be used
     *
     * @param window - owner of the window
     */
    public JWindow(final Window window) {
        // It seems that this constructor is not equivalent to
        // JWindow(window, null).
        // In this constructor GraphicsConfiguration is taken from the owner;
        // in JWindow(window, null) GraphicsConfiguration is system default.
        super(window == null ? JFrame.getSharedOwner() : window);
        windowInit();
    }

    /**
     * Constructs a new window in the specified GraphicsConfiguration.
     *
     * @param gc - the GraphicsConfiguration to construct the frame;
     * if gc is null, the system default GraphicsConfiguration is assumed
     */
    public JWindow(final GraphicsConfiguration gc) {
        this(null, gc);
    }

    /**
     * Constructs a new window (initially invisible) with the specified owner.
     * If owner is null, the shared owner will be used
     *
     * @param frame - owner of the window
     */
    public JWindow(final Frame frame) {
        super(frame == null ? JFrame.getSharedOwner() : frame);
        windowInit();
    }

    /**
     * Constructs a new window with no specified owner.
     */
    public JWindow() {
        this((Frame)null);
    }

    /**
     * This class implements accessibility support for <code>JWindow</code>.
     */
    protected class AccessibleJWindow extends AccessibleAWTWindow {
        protected AccessibleJWindow() {
            super();
        }
    }

    /**
     *
     */
    protected void addImpl(final Component comp, final Object constraints,
                           final int index) {
        if (isRootPaneCheckingEnabled()) {
            getContentPane().add(comp, constraints, index);
            return;
        }

        super.addImpl(comp, constraints, index);
    }

    /**
     * Set rootPane property.
     *
     * @param root
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
     * Returns the accessible context for the window.
     *
     * @return the accessible context for the window
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJWindow();
        }

        return accessibleContext;
    }

    /**
     * Returns string representation of this window.
     *
     * @return string representation of this window
     */
    protected String paramString() {
        return super.paramString();
    }

    /**
     *
     */
    public void setLayout(final LayoutManager layout) {
        if (isRootPaneCheckingEnabled()) {
            getContentPane().setLayout(layout);
        } else {
            super.setLayout(layout);
        }
    }

    /**
     * Just calls paint(g). This method was overridden to prevent an
     * unnecessary call to clear the background.
     *
     * @param g - the graphics context to paint
     */
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
    public void remove(final Component comp) {
        if (comp == getRootPane()) {
            // remove directly from JWindow
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
     * Returns rootPaneCheckingEnabled value.
     *
     * @return value of rootPaneCheckingEnabled
     */
    protected boolean isRootPaneCheckingEnabled() {
        return rootPaneCheckingEnabled;
    }

    /**
     * Called by the constructors to init JWindow
     */
    protected void windowInit() {
        setRootPaneCheckingEnabled(true);

        setRootPane(createRootPane());

        setLocale(JComponent.getDefaultLocale());

        setFocusTraversalPolicy(KeyboardFocusManager.
                getCurrentKeyboardFocusManager().
                getDefaultFocusTraversalPolicy());

        // background shouldn't be initialized
    }
}
