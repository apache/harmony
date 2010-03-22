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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Label;

import org.apache.harmony.awt.state.LabelState;


/**
 * Implementation of Label's default visual style
 */
public class DefaultLabel extends DefaultStyle {

    public static void drawBackground(Graphics g, LabelState s) {

        Dimension size = s.getSize();
        g.setColor(s.getBackground());
        g.fillRect(0, 0, size.width, size.height);
    }

    public static void drawText(Graphics g, LabelState s) {
        String text = s.getText();
        if (text == null) {
            return;
        }
        int alignment = s.getAlignment();
        g.setColor(s.getTextColor());
        g.setFont(s.getFont());

        Dimension textSize = s.getTextSize();
        Dimension size = s.getSize();

        int textCenterX = textSize.width / 2;

        int lblCenterX = size.width / 2;
        int lblCenterY = size.height / 2;

        int baseX = 0;
        int margin = (int) (REL_MARGIN * textSize.height / 3);
        switch (alignment) {
        case Label.LEFT:
            baseX = margin;
            break;
        case Label.CENTER:
            baseX = lblCenterX - textCenterX;
            break;
        case Label.RIGHT:
            baseX = size.width - textSize.width - margin;
            break;
        }
        int textCenterY = textSize.height / 2;
        int baseY = lblCenterY + textCenterY;

        if (s.isEnabled()) {
            g.drawString(text, baseX, baseY);
        } else {
            drawDisabledString(g, text, baseX, baseY);
        }
    }

    public static void calculate(LabelState s) {

        Dimension textSize = getTextSize(s);

        Dimension labelSize = new Dimension();
        labelSize.width = textSize.width + (int) (2 * REL_MARGIN * textSize.height) + 2 * ABS_MARGIN;
        labelSize.height = (int) ((2 * REL_MARGIN + 1) * textSize.height) + 2 * ABS_MARGIN;

        s.setDefaultMinimumSize(labelSize);
        s.setTextSize(textSize);
    }

}
