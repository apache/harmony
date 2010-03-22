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
 * @author Dmitry A. Durnev, Pavel Dolgov
 */
package org.apache.harmony.awt.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.SystemColor;

import org.apache.harmony.awt.state.CheckboxState;
import org.apache.harmony.awt.state.TextState;


/**
 * Implementation of Checkbox' default visual style
 */
public class DefaultCheckbox extends DefaultStyle {
    static Color pressedColor = new Color(236, 233, 216);

    public static void drawBackground(Graphics g, CheckboxState s, Rectangle r) {

        Dimension size = s.getSize();
        boolean group = s.isInGroup();

        g.setColor(s.getBackground());
        g.fillRect(0, 0, size.width, size.height);
        Rectangle checkRect = drawCheckBox(g, s);
        g.setColor(s.getTextColor());
        if (s.isChecked()) {
            if (group) {
                drawRoundCheckMark(g, checkRect);
            } else {
                drawCheckMark(g, checkRect);
            }
        }

        if (s.isFocused()) {
            r.grow(1, 1);
            drawFocusRect(g, r.x, r.y, r.width, r.height);
        }
    }

    public static Rectangle getTextRect(CheckboxState s) {

        if (s.getText() == null) {
            return s.getBounds();
        }

        Dimension size = s.getSize();
        Dimension textSize = getTextSize(s);

        int centerY = size.height / 2;
        int textCenterY = textSize.height / 2;

        int margin = ABS_MARGIN + CB_SIZE + (int) (REL_MARGIN * textSize.height / 2);
        int baseX = margin; // + textSize.width;

        int baseY = centerY + textCenterY;

        return new Rectangle(baseX, baseY - textSize.height,
                             textSize.width, textSize.height);

    }

    public static void drawText(Graphics g, TextState s, Rectangle r) {
        String text = s.getText();
        if (text == null){
            return;
        }
        int baseX = r.x;
        int h = getTextSize(s).height;
        int baseY = r.y + r.height - h / 5;
        g.setFont(s.getFont());
        g.setColor(s.getTextColor());
        if (s.isEnabled()) {
            g.drawString(text, baseX, baseY);
        } else {
            drawDisabledString(g, text, baseX, baseY);
        }
    }

    public static void calculate(CheckboxState s) {        
        Dimension textSize = getTextSize(s);

        Dimension cbSize = new Dimension();
        cbSize.width = textSize.width + (int) (2 * REL_MARGIN * textSize.height) + 2 * ABS_MARGIN;
        cbSize.width += CB_SIZE;
        cbSize.height = (int) ((2 * REL_MARGIN + 1) * textSize.height) + 2 * ABS_MARGIN;
        s.setDefaultMinimumSize(cbSize);
        s.setTextSize(textSize);
    }

    private static void drawRoundCheckMark(Graphics g, Rectangle rect) {
        int dx = rect.width / 3;
        int dy = rect.height / 3;
        g.fillOval(rect.x + dx, rect.y + dy, dx + 1, dy + 1);
    }

    private static void drawCheckMark(Graphics g, Rectangle rect) {

        Graphics2D g2d = (Graphics2D) g;
        Stroke stroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(3));        
        int w = rect.width;
        int h = rect.height;
        Point p2 = new Point(rect.x + w / 2 - 1, rect.y + h - 3);
        int x0 = p2.x;
        int y0 = p2.y;
        Point p1 = new Point(x0 - w / 4 + 1, y0 - h / 4 + 1);
        Point p3 = new Point(x0 + w / 3, y0 - h / 3);
        g.drawLine(p1.x, p1.y, x0, y0);
        g.drawLine(x0, y0, p3.x, p3.y);

        g2d.setStroke(stroke);
    }

    private static Rectangle drawCheckBox(Graphics g, CheckboxState s) {
        Dimension size = s.getSize();
        boolean group = s.isInGroup();
        boolean pressed = s.isPressed();
        int x = ABS_MARGIN;
        int y = size.height / 2 - CB_SIZE / 2;
        Rectangle r = new Rectangle(x, y, CB_SIZE, CB_SIZE);
        if (!group) {
            drawPressedRect(g, r, pressed);
        } else {
            drawPressedCircle(g, r, pressed);
        }

        return r;
    }

    private static void drawPressedRect(Graphics g, Rectangle rect,
                                        boolean pressed) {
        int w = rect.width;
        int h = rect.height;
        int x = rect.x;
        int y = rect.y;
        g.setColor(SystemColor.controlHighlight);

        g.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
        g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
        g.setColor(SystemColor.controlShadow);
        g.drawLine(x + 1, y + 1, x + w - 3, y + 1);
        g.drawLine(x + 1, y + 1, x + 1, y + h - 3);
        g.setColor(SystemColor.controlDkShadow);
        g.drawLine(x, y, x + w - 2, y);
        g.drawLine(x, y, x, y + h - 2);
        g.setColor(pressed ? pressedColor : SystemColor.window);
        g.fillRect(x+2, y+2, CB_SIZE - 2, CB_SIZE - 2);
    }

    private static void drawPressedCircle(Graphics g, Rectangle rect,
                                          boolean pressed) {
        int w = rect.width;
        int h = rect.height;
        int x = rect.x;
        int y = rect.y;

        g.setColor(SystemColor.controlShadow);
        int startAngle = 45;
        int angle = 180;
        int endAngle = startAngle + angle;
        g.drawArc(x, y, w, h, startAngle, angle);
        g.setColor(SystemColor.controlDkShadow);
        g.drawArc(x + 1, y + 1, w - 1, h - 1,  startAngle, angle);

        g.setColor(SystemColor.controlHighlight);
        g.drawArc(x, y, w, h, endAngle, angle);
        g.setColor(SystemColor.controlLtHighlight);
        g.drawArc(x + 1, y + 1, w - 1, h - 1, endAngle, angle);
        g.setColor(pressed ? pressedColor : SystemColor.window);
        g.fillOval(x+2, y+2, CB_SIZE - 2, CB_SIZE - 2);

    }

}
