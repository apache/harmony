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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.SystemColor;

import org.apache.harmony.awt.state.ChoiceState;


/**
 * DefaultChoice
 */
public class DefaultChoice extends DefaultStyle {
    private final static int BORDER_SIZE = 2;

    public static Rectangle drawButton(Graphics g, ChoiceState s) {
        Dimension size = s.getSize();
        Rectangle buttonRect = getButtonRect(size);
        int dx = buttonRect.x;
        int dy = buttonRect.y;
        g.translate(dx, dy);
        g.setColor(SystemColor.control);
        int buttonWidth = buttonRect.width;
        int buttonHeight = buttonRect.height;
        g.fillRect(0, 0, buttonWidth, buttonHeight);
        DefaultScrollbar.paintArrowButton(g, DefaultScrollbar.SOUTH,
                                          buttonWidth,
                                          buttonHeight,
                                          s.isPressed(), s.isEnabled());
        g.translate(-dx, -dy);
        return buttonRect;
    }

    private static Rectangle getButtonRect(Dimension size) {
        Rectangle buttonRect = new Rectangle();
        int gap = 2 * BORDER_SIZE;
        buttonRect.width = Math.min(size.width - gap,
                                   DefaultScrollbar.BUTTON_SIZE + 2);
        buttonRect.height = size.height - gap;
        buttonRect.x = size.width - buttonRect.width - BORDER_SIZE;
        buttonRect.y = BORDER_SIZE;
        return buttonRect;
    }

    public static Rectangle drawBackground(Graphics g, ChoiceState s) {
        Dimension size = s.getSize();
        g.setColor(s.getBackground());
        g.fillRect(0, 0, size.width, size.height);
        DefaultButton.drawButtonFrame(g, new Rectangle(new Point(),
                                                       size), true);
        Rectangle buttonRect = drawButton(g, s);
        drawFocus(g, s);
        return buttonRect;
    }

    private static void drawFocus(Graphics g, ChoiceState s) {
        if (s.isFocused()) {
            Rectangle r = new Rectangle(new Point(), s.getSize());
            r.width -= DefaultScrollbar.BUTTON_SIZE + 2;
            r.grow(- 2 * BORDER_SIZE, -2 * BORDER_SIZE);
            drawFocusRect(g, r.x, r.y, r.width, r.height);
            r.grow(-1, -1);
            g.setColor(SystemColor.textHighlight);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }

    public static void drawText(Graphics g, ChoiceState s) {
        String text = s.getText();
        if (text == null) {
            return;
        }

        Rectangle r = s.getTextBounds();

        Shape oldClip = g.getClip();
        g.clipRect(r.x, r.y, r.width, r.height);

        g.setFont(s.getFont());
        g.setColor(s.isFocused() ? SystemColor.textHighlightText
                                 : s.getTextColor());
        int baseX = r.x;
        int baseY = r.y + r.height - s.getFontMetrics().getDescent();
        if (s.isEnabled()) {
            g.drawString(text, baseX, baseY);
        } else {
            drawDisabledString(g, text, baseX, baseY);
        }
        g.setClip(oldClip);
    }
}
