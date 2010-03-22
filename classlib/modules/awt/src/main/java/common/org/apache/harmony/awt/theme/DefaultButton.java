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
 * @author Michael Danilov, Pavel Dolgov
 */
package org.apache.harmony.awt.theme;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.SystemColor;

import org.apache.harmony.awt.state.*;


/**
 * Implementation of Button's default visual style
 */
public class DefaultButton extends DefaultStyle {

    public static void drawBackground(Graphics g, ButtonState s) {
        Rectangle rect = s.getBounds();
        Color bkColor = s.isBackgroundSet() ? s.getBackground() :
                                              SystemColor.control;
        g.setColor(bkColor);
        g.fillRect(0, 0, rect.width, rect.height);

        drawButtonFrame(g, s.getBounds(), s.isPressed());

        if (s.isFocused()) {
            Color foreColor = s.isTextColorSet() ?
                    s.getTextColor() :
                    SystemColor.controlText;
            g.setColor(foreColor);

            drawFocusRect(g, 3, 3, rect.width - 7, rect.height - 7);
        }
    }

    public static void drawButtonFrame(Graphics g, Rectangle rect, boolean pressed) {
        if (pressed) {
            g.setColor(SystemColor.controlHighlight);
            g.drawLine(rect.width - 1, 0, rect.width - 1, rect.height - 1);
            g.drawLine(0, rect.height - 1, rect.width - 1, rect.height - 1);
            g.setColor(SystemColor.controlShadow);
            g.drawLine(1, 1, rect.width - 3, 1);
            g.drawLine(1, 1, 1, rect.height - 3);
            g.setColor(SystemColor.controlDkShadow);
            g.drawLine(0, 0, rect.width - 2, 0);
            g.drawLine(0, 0, 0, rect.height - 2);
        } else {
            g.setColor(SystemColor.controlHighlight);
            g.drawLine(0, 0, rect.width - 1, 0);
            g.drawLine(0, 0, 0, rect.height - 1);
            g.setColor(SystemColor.controlShadow);
            g.drawLine(rect.width - 2, 1, rect.width - 2, rect.height - 2);
            g.drawLine(1, rect.height - 2, rect.width - 2, rect.height - 2);
            g.setColor(SystemColor.controlDkShadow);
            g.drawLine(rect.width - 1, 0, rect.width - 1, rect.height - 1);
            g.drawLine(0, rect.height - 1, rect.width - 1, rect.height - 1);
        }
    }

    public static void drawText(Graphics g, ButtonState s) {
        String text = s.getText();
        boolean pressed = s.isPressed();
        Color foreColor = s.isTextColorSet() ?
                s.getTextColor() :
                SystemColor.controlText;

        if (text != null) {
            int labelHeight = s.getTextSize().height;
            Dimension prefSize = s.getDefaultMinimumSize();
            Dimension realSize = s.getSize();
            int baseX = ABS_MARGIN + (int) (REL_MARGIN * labelHeight) +
                    (realSize.width - prefSize.width) / 2;
            int baseY = ABS_MARGIN + (int) ((REL_MARGIN + 1) * labelHeight) +
                    (realSize.height - prefSize.height) / 2;

            g.setFont(s.getFont());
            if (s.isEnabled()) {
                g.setColor(foreColor);
                if (pressed) {
                    g.drawString(text, baseX + 1, baseY + 1);
                } else {
                    g.drawString(text, baseX, baseY);
                }
            } else {
                drawDisabledString(g, text, baseX, baseY);
            }
        }

    }


    public static void calculate(ButtonState s) {
        FontMetrics metrics = s.getFontMetrics();
        Dimension textSize = getTextSize(s);

        textSize.height = metrics.getAscent();

        Dimension buttonSize = new Dimension();
        buttonSize.width = textSize.width + (int) (2 * REL_MARGIN * textSize.height) + 2 * ABS_MARGIN;
        buttonSize.height = (int) ((2 * REL_MARGIN + 1) * textSize.height) + 2 * ABS_MARGIN;

        s.setDefaultMinimumSize(buttonSize);
        s.setTextSize(textSize);
    }

}
