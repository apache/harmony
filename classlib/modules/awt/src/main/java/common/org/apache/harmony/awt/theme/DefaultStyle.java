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
 * @author Pavel Dolgov, Michael Danilov
 */
package org.apache.harmony.awt.theme;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.SystemColor;

import org.apache.harmony.awt.state.TextState;


/**
 * Common functionality for default visual style
 */
public class DefaultStyle {

    static final int ABS_MARGIN = 4;
    static final double REL_MARGIN = 1./3.;
    static final int CB_SIZE = 12;

    /**
     * Draw dotted rectangle that indicates the focused component
     *
     */
    public static void drawFocusRect(Graphics g, int x, int y, int w, int h) {
        if ((w <= 0) || (h <= 0)) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g;
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND, 1, new float[] { 1.0f, 1.0f }, 0.0f));
        //g.drawRect(x, y, w, h);
        // workaround:
        int x1 = x + w;
        int y1 = y + h;
        g.drawLine(x, y, x, y1);
        g.drawLine(x, y1, x1, y1);
        g.drawLine(x, y, x1, y);
        g.drawLine(x1, y, x1, y1);
        g2d.setStroke(oldStroke);
    }

    /**
     * Draw text for disabled standard component.
     * It's assumed that desired font is already selected in passed Graphics
     * @param g
     * @param label
     * @param baseX
     * @param baseY
     */
    public static void drawDisabledString(Graphics g, String label, int baseX, int baseY) {
        g.setColor(SystemColor.controlHighlight);
        g.drawString(label, baseX + 1, baseY + 1);
        g.setColor(SystemColor.controlShadow);
        g.drawString(label, baseX, baseY);
    }

    /**
     * Calculate the size of the text
     * @param s
     */
    public static Dimension getTextSize(TextState s) {
        Dimension textSize = new Dimension(0, 0);
        String text = s.getText();
        if (text != null) {
            FontMetrics metrics = s.getFontMetrics();
            textSize.width = metrics.stringWidth(text);
            textSize.height = metrics.getHeight();
        }
        return textSize;
    }
}
