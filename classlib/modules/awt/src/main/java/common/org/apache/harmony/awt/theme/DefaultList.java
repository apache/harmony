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
 * @author Dmitry A. Durnev
 */
package org.apache.harmony.awt.theme;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.SystemColor;

import org.apache.harmony.awt.state.ListState;


/**
 * DefaultList
 */
public class DefaultList extends DefaultStyle {
    public static void drawBackground(Graphics g, ListState s, boolean flat) {

        Dimension size = s.getSize();
        g.setColor(s.isBackgroundSet() ? s.getBackground() :
            SystemColor.text);
        Rectangle client = s.getClient();
        g.fillRect(client.x, client.y, client.width, client.height);
        int x = client.x + client.width;
        int y = client.y + client.height;
        int w = size.width - x;
        int h = size.height - y;
        // fill areas outside of client area &&
        // scrollbars
        g.fillRect(x, y, w, h);        
        g.fillRect(0, size.height - 2, size.width, 2);
        g.fillRect(size.width - 2, 0, 2, size.height);
        if (flat) {
            g.setColor(Color.black);
            g.drawRect(0, 0, size.width - 1, size.height - 1);
        } else {
            //draw pressed button frame:
            DefaultButton.drawButtonFrame(g, new Rectangle(new Point(), size),
                                          true);
        }
    }
    public static void drawItems(Graphics g, ListState s) {
        // draw items:
        Shape oldClip = g.getClip();
        Rectangle client = s.getClient();
        g.clipRect(client.x, client.y, client.width, client.height);
        g.setColor(s.getTextColor());
        Font oldFont = g.getFont();
        g.setFont(s.getFont());
        int firstItemIdx = 0;
        int lastItemIdx = 0;
        if (s.getItemCount() > 0) {
            Rectangle itemRect = s.getItemBounds(0);
            int itemHeight = itemRect.height;
            firstItemIdx = (client.y - itemRect.y) / itemHeight;
            lastItemIdx = firstItemIdx + (client.height / itemHeight) + 2;
            lastItemIdx = Math.min(lastItemIdx, s.getItemCount());
        }
        for (int i = firstItemIdx; i < lastItemIdx; i++) {
            drawItem(i, g, s);
        }
        if (s.isFocused()) {
            paintFocus(g, s);
        }
        g.setFont(oldFont);
        g.setClip(oldClip);
    }

    private static void drawItem(int idx, Graphics g, ListState s) {        
        Rectangle itemRect = s.getItemBounds(idx);
        itemRect.width = s.getClient().width - itemRect.x + 1;
        int itemHeight = itemRect.height;        
        if (s.isSelected(idx)) {
            g.setColor(SystemColor.textHighlight);
            ((Graphics2D)g).fill(itemRect);
            g.setColor(SystemColor.textHighlightText);
        }
        String item = s.getItem(idx);
        FontMetrics fm = s.getFontMetrics();
        int baseLineOffset = itemHeight - fm.getDescent() - 1;
        g.drawString(item, itemRect.x + 1, baseLineOffset + itemRect.y);

        if (s.isSelected(idx)) {
            g.setColor(s.getTextColor());
        }
    }

    private static void paintFocus(Graphics g, ListState s) {
        int curIdx = s.getCurrentIndex();
        if ((curIdx >= 0) && (curIdx < s.getItemCount())) {
            Rectangle curRect = s.getItemBounds(curIdx);
            Color oldColor = g.getColor();
            g.setColor(s.isSelected(curIdx) ? Color.YELLOW : s.getTextColor());
            int w = s.getClient().width - curRect.x - 1;
            drawFocusRect(g, curRect.x + 1, curRect.y + 1, w,
                          curRect.height - 3);
            g.setColor(oldColor);
        }
    }
}
