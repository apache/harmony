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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

import java.awt.event.MouseEvent;

import java.beans.PropertyChangeEvent;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.LookAndFeel;
import javax.swing.JRootPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import javax.swing.event.MouseInputAdapter;

import javax.swing.plaf.ComponentUI;

import javax.swing.plaf.basic.BasicRootPaneUI;

import org.apache.harmony.x.swing.ComponentDragImplHelper;
import org.apache.harmony.x.swing.Utilities;



public class MetalRootPaneUI extends BasicRootPaneUI {
    // keys to UIDefaults
    private static final String BORDER_KEYS[] = {
            null,
            "RootPane.frameBorder",
            "RootPane.plainDialogBorder",
            "RootPane.informationDialogBorder",
            "RootPane.errorDialogBorder",
            "RootPane.colorChooserDialogBorder",
            "RootPane.fileChooserDialogBorder",
            "RootPane.questionDialogBorder",
            "RootPane.warningDialogBorder"
    };

    /*
     * Custom layout. It is very similar to JRootPane.RootLayout,
     * but also takes custom component (MetalTitlePane) into account.
     */
    private class MetalRootLayout implements LayoutManager2 {
        private MetalRootLayout() {
        }

        public Dimension preferredLayoutSize(final Container parent) {
            return Utilities.getRootPaneLayoutSize(
                    root.getContentPane().getPreferredSize(),
                    root.getJMenuBar() != null ?
                            root.getJMenuBar().getPreferredSize() :
                            null,
                    root.isAncestorOf(titlePane) ?
                            titlePane.getPreferredSize() :
                            null,
                    root.getInsets()
                    );
        }

        public Dimension minimumLayoutSize(final Container parent) {
            return Utilities.getRootPaneLayoutSize(
                    root.getContentPane().getMinimumSize(),
                    root.getJMenuBar() != null ?
                            root.getJMenuBar().getMinimumSize() :
                            null,
                    root.isAncestorOf(titlePane) ?
                            titlePane.getMinimumSize() :
                            null,
                    root.getInsets()
                    );
        }

        public Dimension maximumLayoutSize(final Container parent) {
            return Utilities.getRootPaneLayoutSize(
                    root.getContentPane().getMaximumSize(),
                    root.getJMenuBar() != null ?
                            root.getJMenuBar().getMaximumSize() :
                            null,
                    root.isAncestorOf(titlePane) ?
                            titlePane.getMaximumSize() :
                            null,
                    root.getInsets()
                    );
        }

        public void layoutContainer(final Container parent) {
            // The glassPane fills the entire viewable area of the JRootPane (bounds - insets).
            // The layeredPane fills the entire viewable area of the JRootPane. (bounds - insets)
            // The titlePane is positioned at the upper edge of the layeredPane, if present.
            // The menuBar is positioned under titlePane, if present.
            // The contentPane fills the entire viewable area,
            //    minus the menuBar and the titlePane, if they are present.
            Rectangle r = SwingUtilities.calculateInnerArea(root, null);

            root.getGlassPane().setBounds(r);
            root.getLayeredPane().setBounds(r);

            // titlePane, menuBar, contentPane lay in layeredPane
            int top = 0;
            int height = r.height;
            if (root.isAncestorOf(titlePane)) {
                int titleHeight = titlePane.getPreferredSize().height;
                // titlePane lays in layeredPane
                titlePane.setBounds(0, top, r.width, titleHeight);
                top = titleHeight;
                height -= titleHeight;
            }

            if (root.getJMenuBar() != null) {
                int menuHeight = root.getJMenuBar().getPreferredSize().height;
                // menuBar lays in layeredPane
                root.getJMenuBar().setBounds(0, top, r.width, menuHeight);
                top += menuHeight;
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
     * This class is responsible for: the resizement/movement of the window;
     * the change of the mouse cursor when it is positioned on the border.
     */
    private class BorderListener extends MouseInputAdapter implements SwingConstants {

        /*
         * Shows direction of resizing
         */
        private int resizeDirection = ComponentDragImplHelper.RESIZE_NONE;

        private ComponentDragImplHelper helper;

        /*
         * This is the window that contains <code>JRootPane</code>
         * with the installed UI.
         */
        private Window window;

        private boolean isDynamicLayout;

        /*
         *
         */
        private BorderListener() {
            window = SwingUtilities.getWindowAncestor(root);
            helper = new ComponentDragImplHelper();
        }

        /**
         *
         */
        public void mouseClicked(final MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) &&
                    e.getClickCount() > 1 &&
                    isTitlePaneClick(e)) {

                Window window = SwingUtilities.getWindowAncestor(root);
                if (window instanceof Frame) {
                    Frame frame = (Frame)window;
                    if (Utilities.isMaximumFrame(frame)) {
                        frame.setExtendedState(Frame.NORMAL);
                    } else {
                        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                    }
                }
            }
        }

        public void mousePressed(final MouseEvent e) {
            resizeDirection = ComponentDragImplHelper.getResizeDirection(e, root);

            if (resizeDirection != ComponentDragImplHelper.RESIZE_NONE) {
                if (Utilities.isResizableWindow(window) &&
                        !Utilities.isMaximumFrame(window)) {
                    // resizing
                    helper.mousePressed(e, window, root);
                    isDynamicLayout = loadDynamicLayoutProperty();
                }
            } else if (isTitlePaneClick(e)) {
                if (!Utilities.isMaximumFrame(window)) {
                    // dragging
                    helper.mousePressed(e, window, root);
                }
            }
        }

        public void mouseExited(final MouseEvent e) {
            if (!helper.isDragging()) {
                window.setCursor(null);
            }
        }

        public void mouseDragged(final MouseEvent e) {
            if (!helper.isDragging()) {
                return;
            }

            if (resizeDirection == ComponentDragImplHelper.RESIZE_NONE) {
                // dragging the internal frame
                Rectangle newBounds = helper.mouseDragged(e);
                window.setBounds(newBounds);
                // end of draggint the internal frame
            } else {
                // resizing the internal frame
                Rectangle newBounds = helper.mouseDragged(e);
                window.setBounds(newBounds);
                if (isDynamicLayout) {
                    window.validate();
                }
                // end of resizing the internal frame
            }
        }

        public void mouseMoved(final MouseEvent e) {
            updateMouseCursor(e);
        }

        public void mouseReleased(final MouseEvent e) {
            if (!helper.isDragging()) {
                return;
            }

            helper.endDraggingOrResizing(e);
            if (resizeDirection != ComponentDragImplHelper.RESIZE_NONE
                    && !isDynamicLayout) {
                window.validate();
            }
            resizeDirection = ComponentDragImplHelper.RESIZE_NONE;

            // restore the cursor after resizing if we need
            // (if it doesn't point to the border)
            updateMouseCursor(e);
        }

        private boolean isTitlePaneClick(final MouseEvent e) {
            Point p = SwingUtilities.convertPoint(
                    e.getComponent(), e.getPoint(), titlePane);

            return titlePane.contains(p);
        }

        /*
         * Update the mouse cursor.
         */
        private void updateMouseCursor(final MouseEvent e) {
            if (!Utilities.isResizableWindow(window) ||
                    Utilities.isMaximumFrame(window)) {
                return;
            }

            window.setCursor(ComponentDragImplHelper.getUpdatedCursor(e, root));
        }

        private boolean loadDynamicLayoutProperty() {
            PrivilegedAction action = new PrivilegedAction() {
                public Object run() {
                    return System.getProperty("swing.dynamicLayout", "true");
                }
            };

            String value = (String)AccessController.doPrivileged(action);
            return Boolean.valueOf(value).booleanValue();
        }
    }

    /*
     *  The custom layout manager.
     */
    private LayoutManager layout;

    /*
     *  When the custom layout manager is set, the old layout manager
     *  is stored here.
     */
    private LayoutManager saveLayout;

    /*
     *  The custom component (title pane).
     */
    private MetalRootPaneTitlePane titlePane;

    private JRootPane root;

    private MouseInputAdapter borderListener;

    public void installUI(final JComponent c) {
        super.installUI(c);

        root = (JRootPane)c;
        if (root.getWindowDecorationStyle() != JRootPane.NONE) {
            installWindowDecorations();
        }
    }

    public void uninstallUI(final JComponent c) {
        if (root.getWindowDecorationStyle() != JRootPane.NONE) {
            uninstallWindowDecorations();
        }

        super.uninstallUI(c);
    }

    public void propertyChange(final PropertyChangeEvent e) {
        // we are interested here only in windowDecorationStyle property
        if ("windowDecorationStyle".equals(e.getPropertyName())) {
            //JRootPane root = (JRootPane)e.getSource();
            if (((Integer)e.getOldValue()).intValue() != JRootPane.NONE) {
                uninstallWindowDecorations();
            }
            if (((Integer)e.getNewValue()).intValue() != JRootPane.NONE) {
                installWindowDecorations();
            }
        } else {
            super.propertyChange(e);
        }
    }

    public static ComponentUI createUI(final JComponent c) {
        return new MetalRootPaneUI();
    }

    /*
     * Installs titlePane, border and custom layout. This function
     * is invoked only if root.getWindowDecorationStyle() != NONE.
     */
    private void installWindowDecorations() {
        if (!Utilities.lookAndFeelSupportsWindowDecorations()) {
            return;
        }
        // install titlePane
        if (titlePane == null) {
            titlePane = createTitlePane(root);
        } else {
            titlePane.updateTitlePaneProperties();
        }
        // titlePane cannot be already installed;
        // for example: if windowDecorationStyle is clanged
        // from FRAME to ERROR_DIALOG, windowDecorations has to be
        // uninstalled first;
        root.getLayeredPane().add(titlePane, JLayeredPane.FRAME_CONTENT_LAYER, -1);

        // install border
        String key = BORDER_KEYS[root.getWindowDecorationStyle()];
        LookAndFeel.installBorder(root, key);

        // install custom layout
        if (layout == null) {
            layout = createLayout();
        }
        saveLayout = root.getLayout();
        root.setLayout(layout);

        if (borderListener == null) {
            borderListener = createBorderListener();
        }
        root.addMouseListener(borderListener);
        root.addMouseMotionListener(borderListener);
    }

    /*
     * Uninstalls titlePane, border and custom layout. This function
     * is invoked if root.getWindowDecorationStyle() is changed to NONE
     * or when L&F is uninstalled.
     */
    private void uninstallWindowDecorations() {
        if (!Utilities.lookAndFeelSupportsWindowDecorations()) {
            return;
        }
        // uninstall titlePane
        root.getLayeredPane().remove(titlePane);
        titlePane.uninstallTitlePane();

        // uninstall border
        LookAndFeel.uninstallBorder(root);

        // uninstall custom layout
        root.setLayout(saveLayout);

        root.removeMouseListener(borderListener);
        root.removeMouseMotionListener(borderListener);
    }

    /*
     * Creates the new instance of title pane.
     */
    private MetalRootPaneTitlePane createTitlePane(final JRootPane root) {
        return new MetalRootPaneTitlePane(root);
    }

    /*
     * Creates the new instance of custom layout manager.
     */
    private LayoutManager createLayout() {
        return new MetalRootLayout();
    }

    private MouseInputAdapter createBorderListener() {
        return new BorderListener();
    }
}
