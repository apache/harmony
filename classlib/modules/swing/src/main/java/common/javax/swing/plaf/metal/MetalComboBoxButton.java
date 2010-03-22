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
 * @author Anton Avtamonov
 */

package javax.swing.plaf.metal;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.CellRendererPane;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.UIManager;

import org.apache.harmony.x.swing.ButtonCommons;


public class MetalComboBoxButton extends JButton {
    protected JComboBox comboBox;
    protected Icon comboIcon;
    protected boolean iconOnly;
    protected JList listBox;
    protected CellRendererPane rendererPane;

    private Insets cachedInsets = new Insets(0, 0, 0, 0);


    private class MetalComboBoxButtonModel extends DefaultButtonModel {
        public void setArmed(final boolean isArmed) {
            super.setArmed(isArmed || isPressed());
        }
    }


    public MetalComboBoxButton(final JComboBox comboBox, final Icon icon, final CellRendererPane rendererPane, final JList list) {
        this(comboBox, icon, false, rendererPane, list);
    }

    public MetalComboBoxButton(final JComboBox comboBox, final Icon icon, final boolean iconOnly, final CellRendererPane rendererPane, final JList list) {
        setComboBox(comboBox);
        setComboIcon(icon);
        setIconOnly(iconOnly);
        this.rendererPane = rendererPane;
        this.listBox = list;
        setEnabled(comboBox.isEnabled());
        setModel(new MetalComboBoxButtonModel());
    }

    public final JComboBox getComboBox() {
        return comboBox;
    }

    public final void setComboBox(final JComboBox comboBox) {
        this.comboBox = comboBox;
    }

    public final Icon getComboIcon() {
        return comboIcon;
    }

    public final void setComboIcon(final Icon icon) {
        this.comboIcon = icon;
    }

    public final boolean isIconOnly() {
        return iconOnly;
    }

    public final void setIconOnly(final boolean isIconOnly) {
        this.iconOnly = isIconOnly;
    }

    public boolean isFocusTraversable() {
        return false;
    }

    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            setForeground(comboBox.getForeground());
            setBackground(comboBox.getBackground());
        } else {
            setForeground(UIManager.getColor("ComboBox.disabledForeground"));
            setBackground(UIManager.getColor("ComboBox.disabledBackground"));
        }
    }

    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        Insets insets = getInsets(cachedInsets);
        int viewX = insets.left;
        int viewY = insets.top;
        int viewWidth = getWidth() - insets.left - insets.right;
        int viewHeight = getHeight() - insets.top - insets.bottom;

        if (iconOnly) {
            if (comboIcon != null) {
                int iconX = viewX + (viewWidth - comboIcon.getIconWidth()) / 2 + 1;
                int iconY = viewY + (viewHeight - comboIcon.getIconHeight()) / 2 + 1;
                paintIcon(g, iconX, iconY);
            }
        } else {
            if (comboIcon != null) {
                if (comboBox.getComponentOrientation().isLeftToRight()) {
                    paintItem(g, viewX, viewY, viewWidth - comboIcon.getIconWidth() - 1, viewHeight);

                    int iconX = viewX + viewWidth - comboIcon.getIconWidth() - 1;
                    int iconY = viewY + (viewHeight - comboIcon.getIconHeight()) / 2;
                    paintIcon(g, iconX, iconY);
                } else {
                    int iconX = viewX + 1;
                    int iconY = viewY + (viewHeight - comboIcon.getIconHeight()) / 2;
                    paintIcon(g, iconX, iconY);

                    paintItem(g, iconX + comboIcon.getIconWidth() + 1, viewY, viewWidth - comboIcon.getIconWidth() - 1, viewHeight);
                }
            } else {
                paintItem(g, viewX, viewY, viewWidth, viewHeight);
            }
        }

        if (comboBox.isFocusOwner() && !comboBox.isEditable()) {
            ButtonCommons.paintFocus(g, new Rectangle(viewX, viewY, viewWidth + 2, viewHeight),
                    ((MetalButtonUI)getUI()).getFocusColor());
        }
    }

    private void paintItem(final Graphics g, final int x, final int y, final int w, final int h) {
        if (listBox == null) {
            return;
        }
        Component renderer = comboBox.getRenderer().getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
        if (renderer instanceof JComponent) {
            ((JComponent)renderer).setOpaque(false);
            if (comboBox.isEnabled()) {
                renderer.setForeground(comboBox.getForeground());
            } else {
                renderer.setForeground(UIManager.getColor("ComboBox.disabledForeground"));
            }
        }

        rendererPane.paintComponent(g, renderer, this, x, y, w, h);
    }

    private void paintIcon(final Graphics g, final int x, final int y) {
        comboIcon.paintIcon(this, g, x, y);
    }
}
