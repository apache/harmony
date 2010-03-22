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
 * @author Pavel Dolgov
 */
package org.apache.harmony.awt.theme;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemColor;

import org.apache.harmony.awt.state.MenuBarState;
import org.apache.harmony.awt.state.MenuItemState;


/**
 * Implementation of MenuBar's default visual style
 */

public class DefaultMenuBar {

    static final int vSpacing = 2;
    static final int hSpacing = 5;


    public static void layoutMenuBar(MenuBarState s, int width) {

        Font f = s.getFont();
        FontMetrics fm = s.getFontMetrics(f);
        int lineHeight = getItemHeight(fm);
        int lines = 0;
        int lineWidth = 0;
        for (int i=0; i<s.getItemCount(); i++) {
            MenuItemState is = s.getItem(i);
            int itemWidth = getItemWidth(fm, is);
            if (itemWidth >= width) {
                lines += (lineWidth == 0 ? 1 : 2);
                lineWidth = 0;
                is.setItemBounds(0, lines * lineHeight, itemWidth, lineHeight);
                continue;
            }
            if (lineWidth + itemWidth > width) {
                lines ++;
                lineWidth = itemWidth;
                is.setItemBounds(0, lines * lineHeight, itemWidth, lineHeight);
                continue;
            }
            is.setItemBounds(lineWidth, lines * lineHeight, itemWidth, lineHeight);
            lineWidth += itemWidth;
        }
        if (lineWidth > 0) {
            lines ++;
        }

        s.setSize(width, lines * lineHeight + 1);
    }

    public static int getPreferredWidth(MenuBarState s) {
        int width = 0;
        Font f = s.getFont();
        FontMetrics fm = s.getFontMetrics(f);
        for (int i=0; i<s.getItemCount(); i++) {
            width += getItemWidth(fm, s.getItem(i));
        }
        return width;
    }

    public static void drawMenuBar(MenuBarState s, Graphics g) {
        int width = s.getWidth(), height = s.getHeight();
        g.setColor(SystemColor.control);
        g.fillRect(0, 0, width, height);
        g.setColor(SystemColor.menu);
        g.drawLine(0, height-1, width-1, height-1);

        g.setFont(s.getFont());
        g.setColor(SystemColor.menuText);
        int selected = s.getSelectedItemIndex();
        for (int i=0; i<s.getItemCount(); i++) {
            MenuItemState is = s.getItem(i);
            Rectangle item = is.getItemBounds();
            Rectangle text = is.getTextBounds();

            if (!is.isEnabled()) {
                g.setColor(SystemColor.textInactiveText);
            } else if (i == selected) {
                g.setColor(SystemColor.textHighlight);
                g.fillRect(item.x, item.y, item.width, item.height);
                g.setColor(SystemColor.textHighlightText);
            } else {
                g.setColor(SystemColor.menuText);
            }
            g.drawString(is.getText(), item.x + text.x, item.y + text.y);
        }
    }

    public static int getItemIndex(MenuBarState s, Point p) {

        for (int i=0; i<s.getItemCount(); i++) {
            MenuItemState is = s.getItem(i);
            Rectangle bounds = is.getItemBounds();
            if (bounds.contains(p)) {
                return i;
            }
        }

        return -1;
    }

    public static Point getItemLocation(MenuBarState s, int index) {
        if (index < 0 || index >= s.getItemCount()) {
            return new Point(-1, -1);
        }

        MenuItemState is = s.getItem(index);
        Rectangle bounds = is.getItemBounds();
        Point where = new Point(bounds.x, bounds.y + bounds.height);
        Point screenPos = s.getLocationOnScreen();
        where.translate(screenPos.x, screenPos.y);
        return where;
    }


    private static int getItemWidth(FontMetrics fm, MenuItemState is) {
        Rectangle r = is.getTextBounds();
        if (r == null) {
            int h = getItemHeight(fm);
            is.setTextBounds(hSpacing, h + - vSpacing - fm.getDescent(),
                    fm.stringWidth(is.getText()) + 2 * hSpacing, h);
            r = is.getTextBounds();
        }
        return r.width;
    }

    private static int getItemHeight(FontMetrics fm) {
        return fm.getHeight() + 2 * vSpacing;
    }
}
