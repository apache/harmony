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
import java.awt.Shape;

import javax.swing.Icon;

public class MatteBorder extends EmptyBorder {

    protected Color color;
    protected Icon tileIcon;

    public MatteBorder(final Insets insets, final Icon tileIcon) {
        super(insets);

        this.tileIcon = tileIcon;
    }

    public MatteBorder(final Insets insets, final Color color) {
        super(insets);

        this.color = color;
    }

    public MatteBorder(final Icon tileIcon) {
		// null vervfication has been added according to HARMONY-2589
		super((tileIcon == null) ? -1 : tileIcon.getIconHeight(),
				(tileIcon == null) ? -1 : tileIcon.getIconWidth());
		
		this.tileIcon=tileIcon;
	}

    public MatteBorder(final int top, final int left, final int bottom, final int right, final Icon tileIcon) {
        super(top, left, bottom, right);

        this.tileIcon = tileIcon;
    }

    public MatteBorder(final int top, final int left, final int bottom, final int right, final Color color) {
        super(top, left, bottom, right);

        this.color = color;
    }

    private void fillRectWithIcon(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
        int iconWidth = tileIcon.getIconWidth();
        int iconHeight = tileIcon.getIconHeight();
        for (int xOffset = 0; xOffset < width; xOffset += iconWidth) {
            for (int yOffset = 0; yOffset < height; yOffset += iconHeight) {
                tileIcon.paintIcon(c, g, x + xOffset, y + yOffset);
            }
        }
    }

    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
        if (tileIcon != null) {
            int iconWidth = tileIcon.getIconWidth();
            int iconHeight = tileIcon.getIconHeight();
            int topOffset = 0;
            int leftOffset = 0;
            Shape oldClip = g.getClip();
            g.setClip(x, y, width, top);
            fillRectWithIcon(c, g, x + leftOffset, y + topOffset, width, top);

            g.setClip(x, y + top, left, height - top - bottom);
            topOffset = (int)(top/iconHeight)*iconHeight;
            fillRectWithIcon(c, g, x + leftOffset, y + topOffset, left, height);


            g.setClip(x, y + height - bottom, width, bottom);
            topOffset = (int)((height-bottom)/iconHeight)*iconHeight;
            fillRectWithIcon(c, g, x, y + topOffset, width, height - topOffset);

            g.setClip(x + width - right, y + top, right, height - top - bottom);
            topOffset = (int)(top/iconHeight)*iconHeight;
            leftOffset = (int)((width - right)/iconWidth)*iconWidth;
            fillRectWithIcon(c, g, x + leftOffset, y + topOffset, width - leftOffset, height - topOffset);
            g.setClip(oldClip);
        } else if (color != null) {
            Color oldColor = g.getColor();
            g.setColor(color);
            g.fillRect(x, y, width, top);
            g.fillRect(x, y + top, left, height - top);
            g.fillRect(x + width - right, y + top, right, height - top);
            g.fillRect(x + left, y + height - bottom, width - left - right, bottom);
            g.setColor(oldColor);
        }
    }

    public Icon getTileIcon() {
        return tileIcon;
    }

    public Color getMatteColor() {
        return color;
    }

    public boolean isBorderOpaque() {
        return (tileIcon == null);
    }

}

