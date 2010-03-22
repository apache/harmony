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
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.image.BufferedImage;

import org.apache.harmony.awt.state.ScrollbarState;


/**
 * Implementation of Scrollbar's default visual style
 */
public class DefaultScrollbar extends DefaultStyle {
    static final int BUTTON_SIZE = 15;
    static final int MIN_SLIDER_SIZE = 7;

    static final int NORTH = 1;
    static final int EAST = 3;
    static final int SOUTH = 5;
    static final int WEST = 7;

    private static final Color trackHighlightColor = SystemColor.controlDkShadow;;
    private static final Color arrowColor = Color.BLACK;
    private static final Color focusColor = SystemColor.controlDkShadow;

    public static void draw(Graphics g, ScrollbarState s) {
        Dimension size = s.getSize();
        Color bkColor = s.isBackgroundSet() ? s.getBackground()
                                           : SystemColor.scrollbar;
        g.setColor(bkColor);
        g.fillRect(0, 0, size.width, size.height);

        boolean vertical = s.isVertical();

        Rectangle btn1Rect = s.getDecreaseRect();
        if (btn1Rect == null) {
            return;
        }
        Rectangle btn2Rect = s.getIncreaseRect();
        if (btn2Rect == null) {
            return;
        }
        int dir1 = (vertical ? NORTH : WEST);
        int dir2 = (vertical ? SOUTH : EAST);
        boolean btn1Pressed = s.isDecreasePressed();
        boolean btn2Pressed = s.isIncreasePressed();

        g.translate(btn1Rect.x, btn1Rect.y);
        paintArrowButton(g, dir1, btn1Rect.width, btn1Rect.height, btn1Pressed,
                         s.isEnabled());
        g.translate(-btn1Rect.x, -btn1Rect.y);
        g.translate(btn2Rect.x, btn2Rect.y);
        paintArrowButton(g, dir2, btn2Rect.width, btn2Rect.height, btn2Pressed,
                         s.isEnabled());
        g.translate(-btn2Rect.x, -btn2Rect.y);
        drawSlider(g, s);
        paintHighlight(g, s);
    }

    private static void paintFocus(Graphics g, Rectangle sliderRect) {
        sliderRect.grow(-2, -2);
        BufferedImage img = new BufferedImage(sliderRect.width, sliderRect.height,
                                              BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < sliderRect.width; x++) {
            for (int y = x & 1; y < sliderRect.height; y += 2) {
                img.setRGB(x, y, focusColor.getRGB());
            }
        }
        g.drawImage(img, sliderRect.x, sliderRect.y, null);

    }

    private static void paintHighlight(Graphics g, ScrollbarState s) {
        Color oldColor = g.getColor();
        g.setColor(trackHighlightColor);
        switch (s.getHighlight()) {
        case ScrollbarState.NO_HIGHLIGHT:
            break;
        case ScrollbarState.INCREASE_HIGHLIGHT:
            paintIncreaseHighlight(g, s);
            break;
        case ScrollbarState.DECREASE_HIGHLIGHT:
            paintDecreaseHighlight(g, s);
            break;
        }
        g.setColor(oldColor);

    }

    private static void paintDecreaseHighlight(Graphics g, ScrollbarState s) {
        Rectangle r = s.getUpperTrackBounds();
        g.fillRect(r.x, r.y, r.width, r.height);

    }

    private static void paintIncreaseHighlight(Graphics g, ScrollbarState s) {
        Rectangle r = s.getLowerTrackBounds();
        g.fillRect(r.x, r.y, r.width, r.height);

    }

    private static void drawSlider(Graphics g, ScrollbarState s) {
        if (!s.isEnabled()) {
            return;
        }
        Rectangle sliderRect = s.getSliderRect();
        if ((sliderRect == null) || sliderRect.isEmpty()) {
            return;
        }
        g.translate(sliderRect.x, sliderRect.y);

        DefaultButton.drawButtonFrame(g, sliderRect, false);
        g.translate(-sliderRect.x, -sliderRect.y);
        if (s.isFocused()) {
            paintFocus(g, new Rectangle(sliderRect));
        }

    }

    public static void calculate(ScrollbarState s) {
        int dim1 = BUTTON_SIZE + 1;
        int dim2 = 3 * dim1 + 2;
        Dimension scrollbarSize = (s.isVertical() ? new Dimension(dim1, dim2) :
                                                    new Dimension(dim2, dim1));
        s.setDefaultMinimumSize(scrollbarSize);
    }

    static void paintArrowButton(final Graphics g, int dir, int w, int h,
                                 boolean pressed, boolean enabled) {

        DefaultButton.drawButtonFrame(g, new Rectangle(0, 0, w, h), pressed);
        boolean vert = (dir == NORTH) || (dir == SOUTH);
        int w1 = Math.min(w, BUTTON_SIZE + 1);
        int h1 = Math.min(h, BUTTON_SIZE + 1);
        int arrowW = vert ? w1 / 2 : w1 / 3;
        int arrowH = vert ? h1 / 3 : h1 / 2;
        int x = (w - arrowW) / 2;
        int y = (h - arrowH) / 2;
        paintTriangle(g, x, y, arrowW, arrowH, dir, enabled);
    }

    static void paintTriangle(final Graphics g, final int x, final int y,
                              final int w, final int h, final int direction,
                              final boolean isEnabled) {
        Color oldColor = g.getColor();

        int xCenter = x + w / 2;
        int xMax = x + w;
        int yMax = y + h;
        int yCenter = y + h / 2;
        switch (direction) {
        case NORTH:
            paintTriangle(g, new int[] { xMax, x, xCenter },
                          new int[] { yMax, yMax, y }, isEnabled);
            break;
        case SOUTH:
            paintTriangle(g, new int[] { xMax, xCenter, x },
                          new int[] { y, yMax, y }, isEnabled);
            break;
        case WEST:
            paintTriangle(g, new int[] { xMax, xMax, x },
                          new int[] { y, yMax, yCenter }, isEnabled);
            break;
        case EAST:
            paintTriangle(g, new int[] { x, xMax, x },
                          new int[] { yMax, yCenter, y }, isEnabled);
            break;
        }

        g.setColor(oldColor);
    }

    private static void paintTriangle(final Graphics g, final int[] x, final int[] y,
                                      final boolean isEnabled) {


        g.setColor(isEnabled ? arrowColor : SystemColor.controlShadow);
        g.fillPolygon(new Polygon(x, y, x.length));
        if (!isEnabled) {
            g.setColor(SystemColor.controlHighlight);
            int x1 = x[0] + 1;
            int y1 = y[0] + 1;
            int x2 = x[1] + 1;
            int y2 = y[1] + 1;
            g.drawLine(x1, y1, x2, y2);
        }

    }

    private static int getSize(Dimension dim, boolean vert) {
        return (vert ? dim.height : dim.width);
    }

    public static void layout(ScrollbarState s) {
        Dimension size = s.getSize();
        Rectangle buttonRect = new Rectangle(new Point(0, 0), size);
        boolean vert = s.isVertical();

        if (vert) {
            buttonRect.height = Math.min(BUTTON_SIZE, size.height / 2);
        } else {
            buttonRect.width = Math.min(BUTTON_SIZE, size.width / 2);
        }

        Rectangle dnButtonRect = new Rectangle(buttonRect);
        if (vert) {
            dnButtonRect.y = size.height - dnButtonRect.height;
        } else {
            dnButtonRect.x = size.width - dnButtonRect.width;
        }
        s.setIncreaseRect(dnButtonRect);
        s.setDecreaseRect(buttonRect);
        layoutSlider(s);

    }

    private static void layoutSlider(ScrollbarState s) {
        boolean vertical = s.isVertical();
        Dimension btnSize = s.getIncreaseRect().getSize();
        int buttonSize = getSize(btnSize, vertical);
        int size = getSize(s.getSize(), vertical);
        float scrollSize = calculateTrackBounds(s);
        if ((int) scrollSize < MIN_SLIDER_SIZE) {
            // don't draw slider if there's not enough space
            s.setSliderRect(new Rectangle());
            return;
        }
        float scale =  scrollSize / s.getScrollSize();
        int sliderSize = Math.max(MIN_SLIDER_SIZE,
                                  Math.round(s.getSliderSize() * scale));
        int sliderPos = buttonSize + Math.round(s.getSliderPosition() * scale);
        sliderPos = Math.min(sliderPos, size - buttonSize - MIN_SLIDER_SIZE);
        int sliderPosX = 0, sliderPosY = 0;
        if (vertical) {
            sliderPosY = sliderPos;
        } else {
            sliderPosX = sliderPos;
        }
        int w = vertical ? btnSize.width : sliderSize;
        int h = vertical ? sliderSize : btnSize.height;

        Rectangle sliderRect = new Rectangle(sliderPosX, sliderPosY, w, h);
        s.setSliderRect(sliderRect);
        setUpperTrack(s);
        setLowerTrack(s);

    }

    private static void setLowerTrack(ScrollbarState s) {
        boolean v = s.isVertical();
        Rectangle sliderRect = s.getSliderRect();
        Rectangle trackRect = s.getTrackBounds();
        int trackWidth = trackRect.width;
        int trackHeight = trackRect.height;
        int sliderPosX = sliderRect.x;
        int sliderPosY = sliderRect.y;
        int thumbRight = sliderPosX + sliderRect.width;
        int thumbBottom = sliderPosY + sliderRect.height;

        Rectangle incrRect = s.getIncreaseRect();
        int x = (v ? 0 : thumbRight + 1);
        int y = (v ? thumbBottom + 1 : 0);
        int w = (v ? trackWidth : trackWidth - thumbRight + incrRect.width);
        int h = (v ? trackHeight - thumbBottom + incrRect.height : trackHeight - 1);
        s.setLowerTrackBounds(new Rectangle(x, y, w, h));

    }

    private static void setUpperTrack(ScrollbarState s) {
        Rectangle trackBounds = s.getTrackBounds();
        int trackWidth = trackBounds.width;
        int trackHeight = trackBounds.height;
        int decrButtonWidth = s.getDecreaseRect().width;
        int decrButtonHeight = s.getDecreaseRect().height;
        Rectangle sliderRect = s.getSliderRect();
        int sliderPosX = sliderRect.x;
        int sliderPosY = sliderRect.y;
        boolean v = s.isVertical();
        int x = (v ? 0 : trackBounds.x);
        int y = (v ? trackBounds.y : 0);
        int w = (v ? trackWidth : sliderPosX - decrButtonWidth);
        int h = (v ? sliderPosY - decrButtonHeight : trackHeight - 1);
        s.setUpperTrackBounds(new Rectangle(x, y, w, h));

    }

    private static float calculateTrackBounds(ScrollbarState s) {
        Dimension scrollbarSize = s.getSize();
        Dimension btnSize = s.getIncreaseRect().getSize();
        boolean vertical = s.isVertical();
        int size = getSize(scrollbarSize, vertical);
        int buttonSize = getSize(btnSize, vertical);
        double scrollSize = size - 2. * buttonSize;
        s.setTrackSize((int) scrollSize);
        int btnWidth = btnSize.width;
        int btnHeight = btnSize.height;
        int trackX = vertical ? 0 : buttonSize;
        int trackY = vertical ? buttonSize : 0;
        int trackW = vertical ? btnWidth : (int) scrollSize;
        int trackH = vertical ? (int) scrollSize : btnHeight;
        s.setTrackBounds(new Rectangle(trackX, trackY, trackW, trackH));
        return (float) scrollSize;
    }
}
