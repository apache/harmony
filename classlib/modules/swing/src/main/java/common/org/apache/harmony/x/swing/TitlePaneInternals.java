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
 *
 */

package org.apache.harmony.x.swing;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.plaf.UIResource;

/**
 * This class contains properties that are common for all title panes,
 * for example: <code>BasicInternalFrameTitlePane, MetalInternalFrameTitlePane,
 * MetalRootPaneTitlePane</code>.
 *
 */
public final class TitlePaneInternals {
    private JInternalFrame internalFrame;
    private Window window;
    private JFrame frame;
    private JDialog dialog;

    private Icon frameIcon;
    private Icon uiFrameIcon;
    private JMenu windowMenu;

    /**
     * The gap between title pane components (hardcoded).
     * This value of the gap is for BasicInternalFrameTitlePane.
     */
    public int gapX = 2;

    /**
     * Rectangle which represents the bounds of the title decoration area.
     */
    public Rectangle decorationR;

    /**
     * Keeps if the associated frame has menubar or not.
     */
    public boolean hasMenuBar = true;

    /**
     * Consctucts TitlePaneInternals and connects it to the specified frame.
     *
     * @param internalFrame internals are connected to
     */
    public TitlePaneInternals(final JInternalFrame internalFrame) {
        this.internalFrame = internalFrame;
        loadInternalFrameProperties();
    }

    /**
     * Retrieves title information of the connected frame.
     *
     * @return Frame title
     */
    public String getWindowTitle() {
        if (internalFrame != null) {
            return internalFrame.getTitle();
        } else if (frame != null) {
            return frame.getTitle();
        } else if (dialog != null) {
            return dialog.getTitle();
        }
        return "";
    }

    /**
     * Stores frame icon. For the user-defined icon its sizes are scaled to the
     * frame icon sizes.
     *
     * @param icon Icon to be used for the frame
     */
    public void setFrameIcon(final Icon icon) {
        if (icon instanceof UIResource) {
            uiFrameIcon = icon;
            frameIcon = uiFrameIcon;
        } else {
            frameIcon = getScaledIcon(icon);
        }
        updateWindowMenuIcon();
    }

    /**
     * Gets frame icon.
     *
     * @return Icon used for the connected frame
     */
    public Icon getFrameIcon() {
        if (frameIcon != null) {
            return frameIcon;
        } else if (windowMenu != null) {
            return uiFrameIcon;
        }

        return null;
    }

    /**
     * Retrieves icon set to the frame by its L&F.
     *
     * @return Icon installed by L&F
     */
    public Icon getUIFrameIcon() {
        return uiFrameIcon;
    }

    /**
     * Determines is the frame is active (activated if the frame is top-level or selected for internal frames).
     *
     * @return <code>true</code> if the connected frame is active; <code>false</code> otherwise
     */
    public boolean isSelected() {
        if (internalFrame != null) {
            return internalFrame.isSelected();
        }
        return window.isActive();
    }

    /**
     * Sets the connected window (for top-level windows only).
     *
     * @param window Window internals to be connected to
     */
    public void setWindow(final Window window) {
        this.window = window;
        if (window instanceof JFrame) {
            frame = (JFrame)window;
        } else if (window instanceof JDialog) {
            dialog = (JDialog)window;
        }
        internalFrame = null;
    }

    /**
     * Sets system menu used for the connected frame.
     *
     * @param menu JMenu to be used for the connected frame
     */
    public void setWindowMenu(final JMenu menu) {
        windowMenu = menu;
        updateWindowMenuIcon();
    }

    /**
     * Creates the menu item for the specified system menu.
     *
     * @param action Action of the creating item
     * @param text String representing item label
     * @param mnemonic int value representing item mnemonic or -1 if no mnemonic should be used
     *
     * @return JMenuItem created for the frame system menu
     */
    public static JMenuItem createMenuItem(final Action action,
                                           final String text,
                                           final int mnemonic) {
        JMenuItem item = new JMenuItem(action);
        item.setText(text);
        item.setToolTipText(null);
        item.setIcon(null);
        item.setMnemonic(mnemonic);
        return item;
    }


    private void updateWindowMenuIcon() {
        if (windowMenu != null) {
            windowMenu.setIcon(getFrameIcon());
        }
    }

    private Icon getScaledIcon(final Icon icon) {
        if (icon == null || uiFrameIcon == null
            || icon.getIconWidth() == uiFrameIcon.getIconWidth()
            && icon.getIconHeight() == uiFrameIcon.getIconHeight()) {
            return icon;
        }
        int w = uiFrameIcon.getIconWidth();
        int h = uiFrameIcon.getIconHeight();

        if (icon instanceof ImageIcon) {
            Image image = ((ImageIcon)icon).getImage();
            if (image != null) {
                image = image.getScaledInstance(w, h, Image.SCALE_FAST);
                ImageIcon newIcon = new ImageIcon(image);
                return newIcon;
            }
        }
        return icon;
    }


    private void loadInternalFrameProperties() {
        if (internalFrame != null) {
            setFrameIcon(internalFrame.getFrameIcon());
        }
    }
}
