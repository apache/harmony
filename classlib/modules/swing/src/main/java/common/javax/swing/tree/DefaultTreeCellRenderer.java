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
package javax.swing.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.apache.harmony.x.swing.Utilities;


public class DefaultTreeCellRenderer extends JLabel implements TreeCellRenderer {
    protected boolean selected;
    protected boolean hasFocus;
    protected transient Icon closedIcon;
    protected transient Icon leafIcon;
    protected transient Icon openIcon;
    protected Color textSelectionColor;
    protected Color textNonSelectionColor;
    protected Color backgroundSelectionColor;
    protected Color backgroundNonSelectionColor;
    protected Color borderSelectionColor;

    private Font font;
    private Color textDisabledColor;
    private boolean drawsFocusBorderAroundIcon;

    public DefaultTreeCellRenderer() {
        setHorizontalAlignment(SwingConstants.LEFT);
        closedIcon = getDefaultClosedIcon();
        leafIcon = getDefaultLeafIcon();
        openIcon = getDefaultOpenIcon();
        textSelectionColor = UIManager.getColor("Tree.selectionForeground");
        textNonSelectionColor = UIManager.getColor("Tree.textForeground");
        backgroundSelectionColor = UIManager.getColor("Tree.selectionBackground");
        backgroundNonSelectionColor = UIManager.getColor("Tree.textBackground");
        borderSelectionColor = UIManager.getColor("Tree.selectionBorderColor");
        textDisabledColor = UIManager.getColor("Label.disabledForeground");
        drawsFocusBorderAroundIcon = UIManager.getBoolean("Tree.drawsFocusBorderAroundIcon");
    }

    public Icon getDefaultOpenIcon() {
        return UIManager.getIcon("Tree.openIcon");
    }

    public Icon getDefaultClosedIcon() {
        return UIManager.getIcon("Tree.closedIcon");
    }

    public Icon getDefaultLeafIcon() {
        return UIManager.getIcon("Tree.leafIcon");
    }

    public void setOpenIcon(final Icon icon) {
        openIcon = icon;
    }

    public Icon getOpenIcon() {
        return openIcon;
    }

    public void setClosedIcon(final Icon icon) {
        closedIcon = icon;
    }

    public Icon getClosedIcon() {
        return closedIcon;
    }

    public void setLeafIcon(final Icon icon) {
        leafIcon = icon;
    }

    public Icon getLeafIcon() {
        return leafIcon;
    }

    public void setTextSelectionColor(final Color color) {
        textSelectionColor = color;
    }

    public Color getTextSelectionColor() {
        return textSelectionColor;
    }

    public void setTextNonSelectionColor(final Color color) {
        textNonSelectionColor = color;
    }

    public Color getTextNonSelectionColor() {
        return textNonSelectionColor;
    }

    public void setBackgroundSelectionColor(final Color color) {
        backgroundSelectionColor = color;
    }

    public Color getBackgroundSelectionColor() {
        return backgroundSelectionColor;
    }

    public void setBackgroundNonSelectionColor(final Color color) {
        backgroundNonSelectionColor = color;
    }

    public Color getBackgroundNonSelectionColor() {
        return backgroundNonSelectionColor;
    }

    public void setBorderSelectionColor(final Color color) {
        borderSelectionColor = color;
    }

    public Color getBorderSelectionColor() {
        return borderSelectionColor;
    }

    public void setFont(final Font font) {
        if (!Utilities.isUIResource(font)) {
            this.font = font;
        }
    }

    public Font getFont() {
        return font;
    }

    public void setBackground(final Color color) {
        if (!Utilities.isUIResource(color)) {
            super.setBackground(color);
        }
    }

    public Component getTreeCellRendererComponent(final JTree tree,
                                                  final Object value,
                                                  final boolean selected,
                                                  final boolean expanded,
                                                  final boolean leaf,
                                                  final int row,
                                                  final boolean hasFocus) {
        this.selected = selected;
        this.hasFocus = hasFocus;

        setText(tree.convertValueToText(value, selected, expanded, leaf, row, hasFocus));
        if (font == null) {
            font = tree.getFont();
        }
        setBackground(selected ? getBackgroundSelectionColor() : getBackgroundNonSelectionColor());
        if (tree.isEnabled()) {
            setForeground(selected ? getTextSelectionColor() : getTextNonSelectionColor());
        } else {
            setForeground(textDisabledColor);
        }

        if (leaf) {
            setIcon(getLeafIcon());
        } else if (expanded) {
            setIcon(getOpenIcon());
        } else {
            setIcon(getClosedIcon());
        }

        return this;
    }

    public void paint(final Graphics g) {
        Color oldColor = g.getColor();

        if (selected) {
            g.setColor(getBackgroundSelectionColor());
        } else {
            g.setColor(getBackgroundNonSelectionColor());
        }
        int textOffest = getTextOffset();
        int x = getComponentOrientation().isLeftToRight() ? textOffest : 0;
        g.fillRect(x, 0, getWidth() - textOffest, getHeight());

        super.paint(g);

        if (hasFocus) {
            g.setColor(getBorderSelectionColor());
            if (drawsFocusBorderAroundIcon) {
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            } else {
                g.drawRect(x, 0, getWidth() - textOffest - 1, getHeight() - 1);
            }
        }

        g.setColor(oldColor);
    }

    public Dimension getPreferredSize() {
        Dimension basePrefSize = super.getPreferredSize();
        return new Dimension(basePrefSize.width + 2, basePrefSize.height);
    }

    public void validate() {
    }

    public void invalidate() {
    }

    public void revalidate() {
    }

    public void repaint(final long tm, final int x, final int y, final int width, final int height) {
    }

    public void repaint(final Rectangle r) {
    }

    public void repaint() {
    }

    protected void firePropertyChange(final String propertyName,
                                      final Object oldValue,
                                      final Object newValue) {
    }

    public void firePropertyChange(final String propertyName,
                                   final byte oldValue,
                                   final byte newValue) {
    }

    public void firePropertyChange(final String propertyName,
                                   final char oldValue,
                                   final char newValue) {
    }

    public void firePropertyChange(final String propertyName,
                                   final short oldValue,
                                   final short newValue) {
    }

    public void firePropertyChange(final String propertyName,
                                   final int oldValue,
                                   final int newValue) {
    }

    public void firePropertyChange(final String propertyName,
                                   final long oldValue,
                                   final long newValue) {
    }

    public void firePropertyChange(final String propertyName,
                                   final float oldValue,
                                   final float newValue) {
    }

    public void firePropertyChange(final String propertyName,
                                   final double oldValue,
                                   final double newValue) {
    }

    public void firePropertyChange(final String propertyName,
                                   final boolean oldValue,
                                   final boolean newValue) {
    }


    private int getTextOffset() {
        return getIcon() != null ? getIcon().getIconWidth() + 2 : 0;
    }
}
