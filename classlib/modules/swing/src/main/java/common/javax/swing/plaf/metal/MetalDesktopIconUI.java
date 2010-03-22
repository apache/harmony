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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicDesktopIconUI;

public class MetalDesktopIconUI extends BasicDesktopIconUI {

    public static ComponentUI createUI(final JComponent c) {
        return new MetalDesktopIconUI();
    }

    private class DeiconizeAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            deiconize();
        }
    }

    private class InternalFramePropertyChangeListener
        implements PropertyChangeListener {

        public void propertyChange(final PropertyChangeEvent e) {
            if (JInternalFrame.TITLE_PROPERTY.equals(e.getPropertyName())
                    || JInternalFrame.FRAME_ICON_PROPERTY.equals(e.getPropertyName())) {
                loadInternalFrameProperties();
            }
        }
    }

    /*
     * This is a small icon to the left of the button.
     */
    private class DecorationIcon implements Icon {
        public int getIconHeight() {
            return iconPane.getHeight();
        }

        public int getIconWidth() {
            return 8;
        }

        public void paintIcon(final Component c, final Graphics g,
                              final int x, final int y) {
            MetalBumps.paintBumps(g,
                SwingUtilities.calculateInnerArea(decoration, null),
                MetalLookAndFeel.getControlDarkShadow(),
                MetalLookAndFeel.getControlHighlight());
        }
    }

    private int desktopIconWidth;

    private JLabel decoration;

    private InternalFramePropertyChangeListener internalFramePropertyChangeListener;

    /**
     */
    public MetalDesktopIconUI() {
    }

    /**
     */
    public Dimension getMaximumSize(final JComponent c) {
        Dimension size = super.getMaximumSize(c);
        size.width = desktopIconWidth;
        return size;
    }

    /**
     */
    public Dimension getMinimumSize(final JComponent c) {
        Dimension size = super.getMinimumSize(c);
        size.width = desktopIconWidth;
        return size;
    }

    /**
     */
    public Dimension getPreferredSize(final JComponent c) {
        Dimension size = super.getPreferredSize(c);
        size.width = desktopIconWidth;
        return size;
    }

    /**
     */
    protected void installComponents() {
        iconPane = new JButton(new DeiconizeAction());

        loadInternalFrameProperties();
        desktopIcon.add(iconPane, BorderLayout.CENTER);

        decoration = new JLabel(new DecorationIcon());
        decoration.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 2));
        desktopIcon.add(decoration, BorderLayout.WEST);
    }

    /**
     */
    protected void uninstallComponents() {
        desktopIcon.remove(decoration);
        desktopIcon.remove(iconPane);
    }

    /**
     */
    protected void installDefaults() {
        super.installDefaults();

        LookAndFeel.installColorsAndFont(desktopIcon,
                                         "DesktopIcon.background",
                                         "DesktopIcon.foreground",
                                         "DesktopIcon.font"
                                         );
        desktopIconWidth = UIManager.getInt("DesktopIcon.width");
        LookAndFeel.installProperty(desktopIcon, "opaque", Boolean.TRUE);
    }

    /**
     */
    protected void installListeners() {
        super.installListeners();

        if (internalFramePropertyChangeListener == null) {
            internalFramePropertyChangeListener =
                new InternalFramePropertyChangeListener();
        }
        desktopIcon.getInternalFrame().
            addPropertyChangeListener(internalFramePropertyChangeListener);
    }

    /**
     */
    protected void uninstallListeners() {
        super.uninstallListeners();
        desktopIcon.getInternalFrame().
            removePropertyChangeListener(internalFramePropertyChangeListener);
    }

    private void loadInternalFrameProperties() {
        ((JButton) iconPane).setIcon(desktopIcon.getInternalFrame().
                                     getFrameIcon());
        ((JButton) iconPane).setText(desktopIcon.getInternalFrame().
                                     getTitle());
    }
}
