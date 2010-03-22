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


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.Rectangle;

import java.awt.event.ActionEvent;

import java.io.Serializable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

import javax.swing.plaf.RootPaneUI;

import org.apache.harmony.x.swing.Utilities;

public class JRootPane extends JComponent implements Accessible {
    public static final int NONE = 0;
    public static final int FRAME = 1;
    public static final int PLAIN_DIALOG = 2;
    public static final int INFORMATION_DIALOG = 3;
    public static final int ERROR_DIALOG = 4;
    public static final int COLOR_CHOOSER_DIALOG = 5;
    public static final int FILE_CHOOSER_DIALOG = 6;
    public static final int QUESTION_DIALOG = 7;
    public static final int WARNING_DIALOG = 8;

    protected JMenuBar menuBar;
    protected Container contentPane;
    protected JLayeredPane layeredPane;
    protected Component glassPane;
    protected JButton defaultButton;

    /**
     * @deprecated
     */
    protected DefaultAction defaultPressAction;

    /**
     * @deprecated
     */
    protected DefaultAction defaultReleaseAction;

    private int windowDecorationStyle = NONE;
    private JButton savedDefaultButton;

    public JRootPane() {
        setGlassPane(createGlassPane());
        setLayeredPane(createLayeredPane());
        setContentPane(createContentPane());
        setLayout(createRootLayout());
        setDoubleBuffered(true);

        updateUI();
    }

    /**
     * This class implements accessibility support for <code>JRootPane</code>.
     */
    protected class AccessibleJRootPane extends AccessibleJComponent {
        protected AccessibleJRootPane() {
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.ROOT_PANE;
        }

        public int getAccessibleChildrenCount() {
            // not sure
            return 1;
        }

        public Accessible getAccessibleChild(final int i) {
            // not sure
            return (Accessible)contentPane;
        }
    }

    protected  class RootLayout implements LayoutManager2, Serializable {
        protected RootLayout() {
        }

        public Dimension preferredLayoutSize(final Container parent) {
            return Utilities.getRootPaneLayoutSize(
                    getContentPane().getPreferredSize(),
                    getJMenuBar() != null ? getJMenuBar().getPreferredSize() : null,
                    null,
                    getInsets()
                    );
        }

        public Dimension minimumLayoutSize(final Container parent) {
            return Utilities.getRootPaneLayoutSize(
                    getContentPane().getMinimumSize(),
                    getJMenuBar() != null ? getJMenuBar().getMinimumSize() : null,
                    null,
                    getInsets()
                    );
        }

        public Dimension maximumLayoutSize(final Container target) {
            return Utilities.getRootPaneLayoutSize(
                    getContentPane().getMaximumSize(),
                    getJMenuBar() != null ? getJMenuBar().getMaximumSize() : null,
                    null,
                    getInsets()
                    );
        }

        public void layoutContainer(final Container parent) {
            //The glassPane fills the entire viewable area of the JRootPane (bounds - insets).
            //The layeredPane fills the entire viewable area of the JRootPane. (bounds - insets)
            //The menuBar is positioned at the upper edge of the layeredPane.
            //The contentPane fills the entire viewable area, minus the menuBar, if present.
            JRootPane root = (JRootPane)parent;
            Rectangle r = SwingUtilities.calculateInnerArea(root, null);

            root.getGlassPane().setBounds(r);
            root.getLayeredPane().setBounds(r);

            // menuBar, contentPane lay in layeredPane
            int top = 0;
            int height = r.height;
            if (root.getJMenuBar() != null) {
                int menuHeight = root.getJMenuBar().getPreferredSize().height;
                // menuBar lays in layeredPane
                root.getJMenuBar().setBounds(0, 0, r.width, menuHeight);
                top = menuHeight;
                height -= menuHeight;
            }
            // contentPane lays in layeredPane
            root.getContentPane().setBounds(0, top, r.width, height);
        }

        public void addLayoutComponent(final String name, final Component comp) {
            // this method is not used
        }

        public void removeLayoutComponent(final Component comp) {
            // this method is not used
        }

        public void addLayoutComponent(final Component comp, final Object constraints) {
            // this method is not used
        }

        public float getLayoutAlignmentX(final Container target) {
            return 0;
        }

        public float getLayoutAlignmentY(final Container target) {
            return 0;
        }

        public void invalidateLayout(final Container target) {
            // this method is not used
        }
    }

    /*
     * This is deprecate functionality and the class is private.
     * No implmenentation is required.
     */
    private class DefaultAction extends AbstractAction {
        private DefaultAction() {
        }

        public void actionPerformed(final ActionEvent e) {
        }
    }

    public void setUI(final RootPaneUI newUI) {
        super.setUI(newUI);
    }

    public RootPaneUI getUI() {
        return (RootPaneUI)ui;
    }

    public void updateUI() {
        setUI((RootPaneUI)UIManager.getUI(this));
    }

    public String getUIClassID() {
        return "RootPaneUI";
    }

    public int getWindowDecorationStyle() {
        return windowDecorationStyle;
    }

    public void setWindowDecorationStyle(final int newWindowDecorationStyle) {
        switch (newWindowDecorationStyle) {
        case NONE: case FRAME: case PLAIN_DIALOG: case INFORMATION_DIALOG:
        case ERROR_DIALOG: case COLOR_CHOOSER_DIALOG: case FILE_CHOOSER_DIALOG:
        case QUESTION_DIALOG: case WARNING_DIALOG:
            int oldWindowDecorationStyle = windowDecorationStyle;
            windowDecorationStyle = newWindowDecorationStyle;
            firePropertyChange("windowDecorationStyle", oldWindowDecorationStyle, newWindowDecorationStyle);
            return;
        }
        throw new IllegalArgumentException();
    }

    /**
     * @deprecated
     */
    public void setMenuBar(final JMenuBar menu) {
        setJMenuBar(menu);
    }

    public void setJMenuBar(final JMenuBar menu) {
        if (getJMenuBar() != null) {
            layeredPane.remove(getJMenuBar());
        }
        if (menu != null) {
            layeredPane.add(menu, JLayeredPane.FRAME_CONTENT_LAYER);
        }
        menuBar = menu;
    }

    /**
     * @deprecated
     */
    public JMenuBar getMenuBar() {
        return getJMenuBar();
    }

    public JMenuBar getJMenuBar() {
        return menuBar;
    }

    protected JLayeredPane createLayeredPane() {
        JLayeredPane panel = new JLayeredPane();
        panel.setName("layeredPane");
        return panel;
    }

    protected Container createContentPane() {
        //return super.createContentPane();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setName("contentPane");
        panel.setOpaque(true);
        return panel;
    }

    protected Component createGlassPane() {
        JPanel panel = new JPanel(false); // double buffering is set to false
        panel.setName("glassPane");
        panel.setVisible(false);
        panel.setOpaque(false);
        return panel;
    }

    public Container getContentPane() {
        return contentPane;
    }

    public void setContentPane(final Container content) {
        if (content == null) {
            throw new IllegalComponentStateException();
        }
        if (getContentPane() != null) {
            layeredPane.remove(getContentPane());
        }
        layeredPane.add(content, JLayeredPane.FRAME_CONTENT_LAYER);
        contentPane = content;
    }

    public void setLayeredPane(final JLayeredPane layered) {
        if (layered == null) {
            throw new IllegalComponentStateException();
        }
        if (getLayeredPane() != null) {
            remove(getLayeredPane());
        }
        add(layered);
        layeredPane = layered;
    }

    public JLayeredPane getLayeredPane() {
        return layeredPane;
    }

    public void setGlassPane(final Component glass) {
        if (glass == null) {
            throw new NullPointerException();
        }
        if (getGlassPane() != null) {
            remove(getGlassPane());
        }
        glassPane = glass;
        add(glass);
    }

    public Component getGlassPane() {
        return glassPane;
    }

    protected void addImpl(final Component comp, final Object constraints, final int index) {
        if (comp == getGlassPane()) {
//          glassPane should always has a position 0
            super.addImpl(comp, constraints, 0);
        } else if (index == 0) {
//          not a glass pane, index cannot be 0
            super.addImpl(comp, constraints, 1);
        } else {
            super.addImpl(comp, constraints, index);
        }
    }

    public boolean isValidateRoot() {
        return true;
    }

    public boolean isOptimizedDrawingEnabled() {
        return !getGlassPane().isVisible();
    }

    protected String paramString() {
        return super.paramString();
    }

    protected LayoutManager createRootLayout() {
        return new RootLayout();
    }

    public void setDefaultButton(final JButton button) {
        JButton oldButton = defaultButton;
        defaultButton = button;
        savedDefaultButton = button;
        firePropertyChange("defaultButton", oldButton, button);
    }

    public JButton getDefaultButton() {
        return defaultButton;
    }

    /**
     * Returns the accessible context for the root pane.
     *
     * @return the accessible context for the root pane
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJRootPane();
        }

        return accessibleContext;
    }

    public void removeNotify() {
        JButton lastDefaultButton = getDefaultButton();
        super.removeNotify();
        savedDefaultButton = lastDefaultButton;
    }

    public void addNotify() {
        super.addNotify();
        if (getDefaultButton() == null && savedDefaultButton != null
                && savedDefaultButton.getRootPane() == this) {
            setDefaultButton(savedDefaultButton);
        }
    }
}
