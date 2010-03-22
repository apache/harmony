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
 * @author Alexander T. Simbirtsev
 */
package javax.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

public class LineBorder extends AbstractBorder {

    protected int thickness = 1;
    protected Color lineColor;
    protected boolean roundedCorners;

    public LineBorder(final Color color, final int thickness, final boolean roundedCorners) {
        lineColor = color;
        this.thickness = thickness;
        this.roundedCorners = roundedCorners;
    }

    public LineBorder(final Color color, final int thickness) {
        lineColor = color;
        this.thickness = thickness;
    }

    public LineBorder(final Color color) {
        lineColor = color;
    }

    public Insets getBorderInsets(final Component component, final Insets insets) {
        if (insets != null) {
            insets.top = thickness;
            insets.left = thickness;
            insets.right = thickness;
            insets.bottom = thickness;

            return insets;
        }

        return getBorderInsets(component);
    }

    public Insets getBorderInsets(final Component component) {
        return new Insets(thickness, thickness, thickness, thickness);
    }

    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
        Color oldColor = g.getColor();
        g.setColor(lineColor);
        if (!roundedCorners) {
            for (int i = 0; i < thickness; i++) {
                g.drawRect(x + i, y + i, width - 1 - 2*i, height - 1 - 2*i);
            }
        } else {
            for (int i = 0; i < thickness; i++) {
                g.drawRoundRect(x + i, y + i, width - 1 - 2*i, height - 1 - 2*i, thickness, thickness);
            }
        }
        g.setColor(oldColor);
    }

    public Color getLineColor() {
        return lineColor;
    }

    public boolean isBorderOpaque() {
        return !roundedCorners;
    }

    public boolean getRoundedCorners() {
        return roundedCorners;
    }

    public int getThickness() {
        return thickness;
    }

    public static Border createGrayLineBorder() {
        return new LineBorder(Color.gray);
    }

    public static Border createBlackLineBorder() {
        return new LineBorder(Color.black);
    }

}

