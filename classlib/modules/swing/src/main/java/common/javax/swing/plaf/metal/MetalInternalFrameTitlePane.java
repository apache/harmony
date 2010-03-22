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

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Rectangle;

import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;

import org.apache.harmony.x.swing.TitlePaneInternals;

public class MetalInternalFrameTitlePane extends BasicInternalFrameTitlePane {

    private class MetalTitlePaneLayout extends TitlePaneLayout {
        public Dimension minimumLayoutSize(final Container c) {
            if (!isPalette) {
                return super.minimumLayoutSize(c);
            }

            return new Dimension(43, paletteTitleHeight);
        }

        public void layoutContainer(final Container c) {
            if (!isPalette) {
                super.layoutContainer(c);
                return;
            }

            // dimensions of the container
            Rectangle inner = SwingUtilities.calculateInnerArea(
                MetalInternalFrameTitlePane.this, null);
            int width = inner.width;
            int height = inner.height;

            // palette can have only "Close" button
            if (frame.isClosable()) {
                int buttonHeight = closeButton.getIcon().getIconHeight();
                int buttonWidth = buttonHeight;
                int x = inner.x + width - buttonWidth;
                int y = inner.y + (height - buttonHeight) / 2;
                closeButton.setBounds(x, y, buttonWidth, buttonHeight);
                inner.width = closeButton.getX() - inner.x - internals.gapX;
            }

            internals.decorationR.setBounds(inner);
        }
    }

    protected boolean isPalette;
    protected Icon paletteCloseIcon;
    protected int paletteTitleHeight;

    TitlePaneInternals internals;
    Color selectedShadowColor;
    Color notSelectedShadowColor;

    private Border commonBorder = BorderFactory.createEmptyBorder(3, 0, 4, 0);
    private Border paletteBorder = BorderFactory.createEmptyBorder(1, 3, 2, 3);

    public MetalInternalFrameTitlePane(final JInternalFrame frame) {
        super(frame);
        setBorder(commonBorder);

        installInternals();
    }

    protected void addSubComponents() {
        // overridden to add everything except menu bar,
        // but we handle the situation with menuBar == null in the ancestor
        super.addSubComponents();
    }

    protected void addSystemMenuItems(final JMenu menu) {
        // do nothing
    }

    protected void assembleSystemMenu() {
        // do nothing
    }

    protected void showSystemMenu() {
        // do nothing
    }

    protected void createButtons() {
        super.createButtons();

        setupTitlePaneButton(maxButton,
            "InternalFrameTitlePane.maximizeButtonAccessibleName");
        setupTitlePaneButton(iconButton,
            "InternalFrameTitlePane.iconifyButtonAccessibleName");
        setupTitlePaneButton(closeButton,
            "InternalFrameTitlePane.closeButtonAccessibleName");
    }

    private void setupTitlePaneButton(final JButton button,
                                      final String accessibleNameKey) {
        button.getAccessibleContext().setAccessibleName(
            UIManager.getString(accessibleNameKey));
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        // Note: just call the method from super
        return super.createPropertyChangeListener();
    }

    protected LayoutManager createLayout() {
        return new MetalTitlePaneLayout();
    }

    public void paintPalette(final Graphics g) {
        g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
        g.fillRect(0, 0, getWidth(), getHeight());
        paintDecoration(g);
        g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
        g.drawLine(0, getHeight() - 1, getWidth() - 1, getHeight() - 1);
    }

    private void paintDecoration(final Graphics g) {
        Color shadow = internals.isSelected()
                       ? selectedShadowColor
                       : notSelectedShadowColor;
        Color highlight = internals.isSelected()
                          ? MetalLookAndFeel.getPrimaryControlHighlight()
                          : MetalLookAndFeel.getControlHighlight();

        MetalBumps.paintBumps(g, internals.decorationR, shadow, highlight);
    }

    public void paintComponent(final Graphics g) {
        if (isPalette) {
            paintPalette(g);
            return;
        }

        super.paintComponent(g);
        paintDecoration(g);

        // paint the line under the titlePane
        g.setColor(internals.isSelected()
                   ? selectedShadowColor
                   : notSelectedShadowColor);
        g.drawLine(0, getHeight() - 1, getWidth() - 1, getHeight() - 1);
    }

    public void setPalette(final boolean b) {
        isPalette = b;

        closeButton.setIcon(isPalette ? paletteCloseIcon : closeIcon);

        if (isPalette) {
            setBorder(paletteBorder);
            remove(iconButton);
            remove(maxButton);
        } else {
            setBorder(commonBorder);
            if (frame.isIconifiable()) {
                add(iconButton);
            }
            if (frame.isMaximizable()) {
                add(maxButton);
            }
        }
    }

    protected void installDefaults() {
        super.installDefaults();

        paletteTitleHeight = UIManager.getInt("InternalFrame.paletteTitleHeight");
        paletteCloseIcon = UIManager.getIcon("InternalFrame.paletteCloseIcon");

        selectedShadowColor = MetalLookAndFeel.getPrimaryControlDarkShadow();
        notSelectedShadowColor = MetalLookAndFeel.getControlDarkShadow();
    }

    private void installInternals() {
        internals = (TitlePaneInternals)getClientProperty("internals");

        internals.decorationR = new Rectangle();
        internals.gapX = 6;
    }

    protected void uninstallDefaults() {
        // no need to uninstall anything because
        // the title pane is replaced while changing L&F
        super.uninstallDefaults();
    }

    public void addNotify() {
        // Note: just call the method from super
        super.addNotify();
    }
}
