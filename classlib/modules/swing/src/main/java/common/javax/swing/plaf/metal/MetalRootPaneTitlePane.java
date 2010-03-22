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

import java.awt.Frame;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;

import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.TitlePaneInternals;
import org.apache.harmony.x.swing.Utilities;


/**
 * This class implements the metal title pane for top level containers.
 *
 */
final class MetalRootPaneTitlePane extends MetalInternalFrameTitlePane {

    private class WindowStateHandler extends WindowAdapter {
        public void windowActivated(final WindowEvent e) {
            repaintRootPaneDecorations();
        }

        public void windowDeactivated(final WindowEvent e) {
            repaintRootPaneDecorations();
        }

        public void windowStateChanged(final WindowEvent e) {
            setButtonIcons();
            enableActions();
        }

        private void repaintRootPaneDecorations() {
            repaint();

            Insets insets = rootPane.getInsets();
            rootPane.repaint(0, 0, rootPane.getWidth(), insets.top);
            rootPane.repaint(0, insets.top, insets.left,
                             rootPane.getHeight() - insets.bottom - insets.top);
            rootPane.repaint(0, rootPane.getHeight() - insets.bottom,
                             rootPane.getWidth(), insets.bottom);
            rootPane.repaint(rootPane.getWidth() - insets.right, insets.top,
                             insets.right,
                             rootPane.getHeight() - insets.bottom - insets.top);
        }
    }

    private class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent e) {
            if ("title".equals(e.getPropertyName())) {
                revalidate();
                repaint();
            } else if (StringConstants.ICON_IMAGE_PROPERTY.equals(e.getPropertyName())) {
                ImageIcon icon = frame.getIconImage() == null
                                 ? null
                                 : new ImageIcon(frame.getIconImage());
                internals.setFrameIcon(icon);
                revalidate();
                repaint();
            }
        }
    }

    private class IconifyAction extends AbstractAction {
        private IconifyAction() {
            putValue(SMALL_ICON, iconIcon);
        }

        public void actionPerformed(final ActionEvent e) {
            if (frame != null) {
                frame.setExtendedState(frame.getExtendedState()
                        | JFrame.ICONIFIED);
            }
//            if (frame.isIconifiable()) {
//                try {
//                    // removed in 1.5
//                    //if (frame.isMaximum()) {
//                    //    frame.setMaximum(false);
//                    //}
//                    frame.setIcon(!frame.isIcon());
//                } catch (PropertyVetoException v) {
//                }
//            }
        }
    }

    private class CloseAction extends AbstractAction {
        private CloseAction() {
            putValue(SMALL_ICON, closeIcon);
        }

        public void actionPerformed(final ActionEvent e) {
            window.dispatchEvent(new WindowEvent(
                    window, WindowEvent.WINDOW_CLOSING));
        }
    }

    private class MaximizeAction extends AbstractAction {
        private MaximizeAction() {
            putValue(SMALL_ICON, maxIcon);
        }

        public void actionPerformed(final ActionEvent e) {
            if (Utilities.isMaximumFrame(window)) {
                frame.setExtendedState(JFrame.NORMAL);
            } else {
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        }
    }

    private class RestoreAction extends AbstractAction {
        private RestoreAction() {
            putValue(SMALL_ICON, minIcon);
        }

        public void actionPerformed(final ActionEvent e) {
            if (frame != null) {
                frame.setExtendedState(JFrame.NORMAL);
            }
        }
    }

    private JRootPane rootPane;
    private WindowAdapter windowListener;

    /**
     * The window that contains the <code>rootPane</code>.
     */
    private Window window;

    /**
     * If the window is actually JFrame, it is also stored in this field.
     */
    private JFrame frame;

    private boolean isIconifiable;
    private boolean isClosable;
    private boolean isMaximizable;

    // system menu items text
    private String closeButtonText;
    private String maxButtonText;
    private String minButtonText;
    private String restoreButtonText;

    // mnemonics
    private int closeButtonMnemonic;
    private int minButtonMnemonic;
    private int maxButtonMnemonic;
    private int restoreButtonMnemonic;

    public MetalRootPaneTitlePane(final JRootPane root) {
        super(null);
        rootPane = root;

        window = SwingUtilities.getWindowAncestor(root);
        if (window instanceof JFrame) {
            frame = (JFrame)window;
        }
        installInternals();

        installTitlePane();
    }

    protected void installTitlePane() {
        loadWindowProperties();
        super.installTitlePane();
    }

    void uninstallTitlePane() {
        uninstallListeners();
        uninstallDefaults();
    }

    protected void installDefaults() {
        // window menu labels
        closeButtonText = UIManager.getString(
                "MetalTitlePane.closeTitle");
        maxButtonText = UIManager.getString(
                "MetalTitlePane.maximizeTitle");
        minButtonText = UIManager.getString(
                "MetalTitlePane.iconifyTitle");
        restoreButtonText = UIManager.getString(
                "MetalTitlePane.restoreTitle");

        // mnemonics
        closeButtonMnemonic = UIManager.getInt("MetalTitlePane.closeMnemonic");
        minButtonMnemonic = UIManager.getInt("MetalTitlePane.iconifyMnemonic");
        maxButtonMnemonic = UIManager.getInt("MetalTitlePane.maximizeMnemonic");
        restoreButtonMnemonic = UIManager.getInt("MetalTitlePane.restoreMnemonic");

        // icons
        closeIcon = UIManager.getIcon("InternalFrame.closeIcon");
        maxIcon = UIManager.getIcon("InternalFrame.maximizeIcon");
        minIcon = UIManager.getIcon("InternalFrame.minimizeIcon");
        iconIcon = UIManager.getIcon("InternalFrame.iconifyIcon");
        internals.setFrameIcon(UIManager.getIcon("InternalFrame.icon"));

        // colors
        installColors();

        // font
        setFont(UIManager.getFont("InternalFrame.titleFont"));
    }

    private void installColors() {
        switch (rootPane.getWindowDecorationStyle()) {
        case JRootPane.ERROR_DIALOG:
            selectedTitleColor =
                UIManager.getColor("OptionPane.errorDialog.titlePane.background");
            selectedTextColor =
                UIManager.getColor("OptionPane.errorDialog.titlePane.foreground");
            selectedShadowColor =
                UIManager.getColor("OptionPane.errorDialog.titlePane.shadow");
            break;

        case JRootPane.QUESTION_DIALOG:
        case JRootPane.COLOR_CHOOSER_DIALOG:
        case JRootPane.FILE_CHOOSER_DIALOG:
            selectedTitleColor =
                UIManager.getColor("OptionPane.questionDialog.titlePane.background");
            selectedTextColor =
                UIManager.getColor("OptionPane.questionDialog.titlePane.foreground");
            selectedShadowColor =
                UIManager.getColor("OptionPane.questionDialog.titlePane.shadow");
            break;

        case JRootPane.WARNING_DIALOG:
            selectedTitleColor =
                UIManager.getColor("OptionPane.warningDialog.titlePane.background");
            selectedTextColor =
                UIManager.getColor("OptionPane.warningDialog.titlePane.foreground");
            selectedShadowColor =
                UIManager.getColor("OptionPane.warningDialog.titlePane.shadow");
            break;

        default:
        selectedTitleColor =
            UIManager.getColor("InternalFrame.activeTitleBackground");
        selectedTextColor =
            UIManager.getColor("InternalFrame.activeTitleForeground");
        selectedShadowColor = MetalLookAndFeel.getPrimaryControlDarkShadow();
        }

        notSelectedTitleColor =
            UIManager.getColor("InternalFrame.inactiveTitleBackground");
        notSelectedTextColor =
            UIManager.getColor("InternalFrame.inactiveTitleForeground");
        notSelectedShadowColor = MetalLookAndFeel.getControlDarkShadow();
    }

    void updateTitlePaneProperties() {
        installColors();
        loadWindowProperties();
        addSubComponents();
        installListeners();
    }

    protected void uninstallDefaults() {
        // no need to uninstall anything because
        // the title pane is replaced while changing L&F
        super.uninstallDefaults();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    protected void installListeners() {
        if (propertyChangeListener == null) {
            propertyChangeListener = createPropertyChangeListener();
        }

        if (windowListener == null) {
            windowListener = createWindowStateListener();
        }

        if (window != null) {
            window.addPropertyChangeListener(propertyChangeListener);
            window.addWindowListener(windowListener);
            window.addWindowStateListener(windowListener);
        }
    }

    protected void uninstallListeners() {
        window.removePropertyChangeListener(propertyChangeListener);
        window.removeWindowListener(windowListener);
        window.removeWindowStateListener(windowListener);
    }

    private void loadWindowProperties() {
        if (rootPane.getWindowDecorationStyle() == JRootPane.FRAME) {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            isIconifiable = toolkit.isFrameStateSupported(Frame.ICONIFIED);
            isClosable = true;
            isMaximizable = toolkit.isFrameStateSupported(Frame.MAXIMIZED_BOTH);
            internals.hasMenuBar = true;
        } else {
            isIconifiable = false;
            isClosable = true;
            isMaximizable = false;
            internals.hasMenuBar = false;
        }
    }

    private void installInternals() {
        internals.setWindow(window);
    }

    protected void setButtonIcons() {
        closeButton.setAction(closeAction);
        iconButton.setAction(iconifyAction);

        if (Utilities.isMaximumFrame(window)) {
            maxButton.setAction(restoreAction);
        } else {
            maxButton.setAction(maximizeAction);
        }
    }

    private WindowStateHandler createWindowStateListener() {
        return new WindowStateHandler();
    }

    protected void assembleSystemMenu() {
        windowMenu = createSystemMenu();
        internals.setWindowMenu(windowMenu);
        addSystemMenuItems(windowMenu);

        menuBar = createSystemMenuBar();
        menuBar.add(windowMenu);
    }

    protected void addSystemMenuItems(final JMenu menu) {
        menu.add(TitlePaneInternals.createMenuItem(restoreAction,
                                                   restoreButtonText,
                                                   restoreButtonMnemonic));
        menu.add(TitlePaneInternals.createMenuItem(iconifyAction,
                                                   minButtonText,
                                                   minButtonMnemonic));
        menu.add(TitlePaneInternals.createMenuItem(maximizeAction,
                                                   maxButtonText,
                                                   maxButtonMnemonic));

        menu.addSeparator();
        menu.add(TitlePaneInternals.createMenuItem(closeAction,
                                                   closeButtonText,
                                                   closeButtonMnemonic));
    }

    protected void addSubComponents() {
        if (internals.hasMenuBar && !this.isAncestorOf(menuBar)) {
            add(menuBar);
        } else if (!internals.hasMenuBar && this.isAncestorOf(menuBar)) {
            remove(menuBar);
        }

        if (isIconifiable && !this.isAncestorOf(iconButton)) {
            add(iconButton);
        } else if (!isIconifiable && this.isAncestorOf(iconButton)) {
            remove(iconButton);
        }

        if (isMaximizable && !this.isAncestorOf(maxButton)) {
            add(maxButton);
        } else if (!isMaximizable && this.isAncestorOf(maxButton)) {
            remove(maxButton);
        }

        if (isClosable && !this.isAncestorOf(closeButton)) {
            add(closeButton);
        } else if (!isClosable && this.isAncestorOf(closeButton)) {
            remove(closeButton);
        }
    }

    protected void createActions() {
        closeAction = new CloseAction();
        iconifyAction = new IconifyAction();
        maximizeAction = new MaximizeAction();
        restoreAction = new RestoreAction();
    }

    protected void enableActions() {
        restoreAction.setEnabled(Utilities.isMaximumFrame(window));
        maximizeAction.setEnabled(!Utilities.isMaximumFrame(window));
    }
}
