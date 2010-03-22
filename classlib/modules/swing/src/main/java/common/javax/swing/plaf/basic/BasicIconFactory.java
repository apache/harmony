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
 * @author Sergey Burlak
 */

package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.plaf.UIResource;

import org.apache.harmony.x.swing.Utilities;


public class BasicIconFactory implements Serializable {
    private static RadioButtonMenuItemIcon radioButtonMenuItemIcon;
    private static RadioButtonIcon radioButtonIcon;
    private static MenuItemCheckIcon menuItemCheckIcon;
    private static MenuItemArrowIcon menuItemArrowIcon;
    private static MenuArrowIcon menuArrowIcon;
    private static CheckBoxMenuItemIcon checkBoxMenuItemIcon;
    private static CheckBoxIcon checkBoxIcon;
    private static EmptyFrameIcon emptyFrameIcon;

    public static Icon getRadioButtonMenuItemIcon() {
        if (radioButtonMenuItemIcon == null) {
            radioButtonMenuItemIcon = new RadioButtonMenuItemIcon();
        }
        return radioButtonMenuItemIcon;
    }

    public static Icon getRadioButtonIcon() {
        if (radioButtonIcon == null) {
            radioButtonIcon = new RadioButtonIcon();
        }
        return radioButtonIcon;
    }

    public static Icon getMenuItemCheckIcon() {
        if (menuItemCheckIcon == null) {
            menuItemCheckIcon = new MenuItemCheckIcon();
        }
        return menuItemCheckIcon;
    }

    public static Icon getMenuItemArrowIcon() {
        if (menuItemArrowIcon == null) {
            menuItemArrowIcon = new MenuItemArrowIcon();
        }
        return menuItemArrowIcon;
    }

    public static Icon getMenuArrowIcon() {
        if (menuArrowIcon == null) {
            menuArrowIcon = new MenuArrowIcon();
        }
        return menuArrowIcon;
    }

    public static Icon getCheckBoxMenuItemIcon() {
        if (checkBoxMenuItemIcon == null) {
            checkBoxMenuItemIcon = new CheckBoxMenuItemIcon();
        }
        return checkBoxMenuItemIcon;
    }

    public static Icon getCheckBoxIcon() {
        if (checkBoxIcon == null) {
            checkBoxIcon = new CheckBoxIcon();
        }
        return checkBoxIcon;
    }

    public static Icon createEmptyFrameIcon() {
        if (emptyFrameIcon == null) {
            emptyFrameIcon = new EmptyFrameIcon();
        }
        return emptyFrameIcon;
    }


    private static class RadioButtonIcon implements Icon {
        public int getIconHeight() {
            return 13;
        }

        public int getIconWidth() {
            return 13;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            // this method paints nothing
        }
    }

    private static class RadioButtonMenuItemIcon implements Icon {
        private static final int SIZE = 6;

        public int getIconHeight() {
            return SIZE;
        }

        public int getIconWidth() {
            return SIZE;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            AbstractButton b = (AbstractButton)c;
            if (b.isSelected()) {
                g.setColor(b.getForeground());
                g.fillRoundRect(x, y + 1, SIZE, SIZE, SIZE / 2, SIZE / 2);
            }
        }
    }

    private static class CheckBoxIcon implements Icon {
        public int getIconHeight() {
            return 13;
        }

        public int getIconWidth() {
            return 13;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            // this method paints nothing
        }
    }

    private static class CheckBoxMenuItemIcon implements Icon {
        private static final int SIZE = 9;

        public int getIconHeight() {
            return SIZE;
        }

        public int getIconWidth() {
            return SIZE;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            AbstractButton b = (AbstractButton)c;
            if (b.isSelected()) {
                g.setColor(b.getForeground());
                g.drawLine(x + 3, y + SIZE / 2, x + 3, y + SIZE - 2);
                g.drawLine(x + 4, y + SIZE / 2, x + 4, y + SIZE - 2);
                int lineLength = SIZE - SIZE / 2 - 2;
                g.drawLine(x + 4, y + SIZE - 3, x + 4 + lineLength, y + SIZE - 3 - lineLength);
            }
        }
    }

    private static class MenuArrowIcon implements Icon {
        private int iconHeight = 8;
        private int iconWidth = 4;

        public int getIconHeight() {
            return iconHeight;
        }

        public int getIconWidth() {
            return iconWidth;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            final boolean leftToRight = c.getComponentOrientation().isLeftToRight();
            final int direction = leftToRight ? SwingConstants.EAST : SwingConstants.WEST;
            final Color color = c.isEnabled() ? c.getForeground() : c.getBackground().darker();
            Utilities.fillArrow(g, x, y + 1, direction, 8, true, color);
        }
    }

    private static class MenuItemCheckIcon implements Icon {
        public int getIconHeight() {
            return 9;
        }

        public int getIconWidth() {
            return 9;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
        }
    }

    private static class MenuItemArrowIcon implements Icon {
        public int getIconHeight() {
            return 8;
        }

        public int getIconWidth() {
            return 4;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
        }
    }

    private static class EmptyFrameIcon implements Icon, UIResource {
        public int getIconHeight() {
            return 16;
        }

        public int getIconWidth() {
            return 14;
        }

        public void paintIcon(final Component c, final Graphics g,
                              final int x, final int y) {
            // does nothing
        }
    }
}
