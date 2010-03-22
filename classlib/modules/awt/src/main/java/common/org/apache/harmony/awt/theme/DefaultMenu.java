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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.SystemColor;

import org.apache.harmony.awt.state.MenuItemState;
import org.apache.harmony.awt.state.MenuState;


/**
 * Implementation of Menu's default visual style
 */

public class DefaultMenu {

    static final int spacing = 3;
    static final int verticalPadding = 2;
    static final int separatorHeight = 4;
    static final int separatorMargin = 1;


    public static void drawMenu(MenuState s, Graphics gr) {

        int w = s.getWidth(), h = s.getHeight();

        gr.setColor(SystemColor.menu);
        gr.fillRect(0, 0, w, h);
        gr.setColor(SystemColor.controlShadow);
        gr.drawRect(0, 0, w-1, h-1);

        Font f = s.getFont();
        FontMetrics fm = s.getFontMetrics(f);
        int itemHeight = fm.getHeight();
        int y = spacing;

        int leftMargin = itemHeight/4;
        int rightMargin = itemHeight/4;
        int checkMarkWidth = itemHeight;

        gr.setFont(f);
        for (int index=0; index < s.getItemCount(); index++) {

            MenuItemState is = s.getItem(index);
            String label = is.getText();
            if (!label.equals("-")) { //$NON-NLS-1$
                int dy = verticalPadding * 2 + itemHeight;
                y += dy;

                if (!is.isEnabled()) {
                    gr.setColor(SystemColor.textInactiveText);
                } else if (index == s.getSelectedItemIndex()) {
                    gr.setColor(SystemColor.textHighlight);
                    gr.fillRect(spacing, y - dy, w - 2*spacing, dy);
                    gr.setColor(SystemColor.textHighlightText);

                } else {
                    gr.setColor(SystemColor.menuText);
                }

                gr.drawString(label, leftMargin + spacing + checkMarkWidth,
                        y - verticalPadding - fm.getDescent() );

                if (is.isMenu()) {
                    int sz = dy/4;
                    int base = y - dy/2 - 1;
                    int dx = w - spacing - rightMargin - sz - 2;
                    int px[] = new int[] { dx, dx + sz, dx };
                    int py[] = new int[] { base - sz, base, base + sz };
                    gr.fillPolygon(px, py, px.length);
                }
                if (is.isChecked()) {
                    int sz = dy/6;
                    int dx = spacing + sz + leftMargin;
                    int base = y - verticalPadding - fm.getDescent();

                    int px[] = new int[] { dx, dx - sz, dx - sz, dx,
                            dx + 2*sz - 1, dx + 2*sz - 1};
                    int py[] = new int[] { base, base - sz, base - 2*sz,
                            base - sz, base - 3*sz + 1, base - 2*sz + 1 };
                    gr.fillPolygon(px, py, px.length);
                }
            } else {
                // separator
                int dy = verticalPadding * 2 + separatorHeight;
                y += dy;
                int base = y - dy/2 - 1;
                int margin = separatorMargin + spacing;
                gr.setColor(SystemColor.controlShadow);
                gr.drawLine(margin, base, w - margin - 1, base);
                gr.setColor(SystemColor.controlHighlight);
                gr.drawLine(margin, base+1, w - margin - 1, base+1);
            }
        }

    }

    public static Dimension calculateSize(MenuState s) {

        Font f = s.getFont();
        FontMetrics fm = s.getFontMetrics(f);
        int itemHeight = fm.getHeight();
        int h = spacing;
        int w = 0;

        int leftMargin = itemHeight/4;
        int rightMargin = itemHeight/4;
        int checkMarkWidth = itemHeight;
        int arrowSpaceWidth = itemHeight + rightMargin;

        for (int index=0; index < s.getItemCount(); index++) {

            int dx = 0, dy = 0;
            MenuItemState is = s.getItem(index);
            String label = is.getText();
            if (!label.equals("-")) { //$NON-NLS-1$
                dy = verticalPadding * 2 + itemHeight;
                dx = fm.stringWidth(label);
            } else {
                // separator
                dy = verticalPadding * 2 + separatorHeight;
                dx = 2 * separatorMargin;
            }
            h += dy;
            w = Math.max(w, dx);
            is.setItemBounds(spacing, h - dy, w - 2 * spacing, dy);
        }
        h += spacing;
        w += spacing + checkMarkWidth + leftMargin +
                rightMargin + arrowSpaceWidth + spacing;

        for (int index=0; index < s.getItemCount(); index++) {
            MenuItemState is = s.getItem(index);
            is.getItemBounds().width = w;
        }

        return new Dimension(w, h);
    }

    public static int getItemIndex(MenuState s, Point p) {

        if (p.x < spacing || p.x >= s.getWidth() - spacing) {
            return -1;
        }

        Font f = s.getFont();
        FontMetrics fm = s.getFontMetrics(f);
        int itemHeight = fm.getHeight();
        int y = spacing;

        for (int index=0; index < s.getItemCount(); index++) {

            String label = s.getItem(index).getText();
            if (!label.equals("-")) { //$NON-NLS-1$
                int dy = verticalPadding * 2 + itemHeight;
                if ((p.y <= y + dy) && (p.y > y)) {
                    return index;
                }
                y += dy;
            } else {
                // separator
                int dy = verticalPadding * 2 + separatorHeight;
                y += dy;
            }
        }

        return -1;
    }

    public static Point getItemLocation(MenuState s, int index) {
        if (index < 0 || index >= s.getItemCount()) {
            return new Point(-1, -1);
        }

        Font f = s.getFont();
        FontMetrics fm = s.getFontMetrics(f);
        int itemHeight = fm.getHeight();
        int y = spacing;

        for (int i=0; i < index; i++) {

            String label = s.getItem(i).getText();
            if (!label.equals("-")) { //$NON-NLS-1$
                int dy = verticalPadding * 2 + itemHeight;
                y += dy;
            } else {
                // separator
                int dy = verticalPadding * 2 + separatorHeight;
                y += dy;
            }
        }

        int margin = spacing + itemHeight/4;

        Point location = s.getLocation();
        Point result = new Point(location);
        result.translate(s.getWidth() - margin, y - spacing);
        // TODO: ajust position according to sumbenu size
        return result;
    }

}
