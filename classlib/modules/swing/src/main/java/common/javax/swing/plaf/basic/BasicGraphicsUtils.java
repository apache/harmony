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
 * @author Vadim L. Bogdanov
 */
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.AbstractButton;

import org.apache.harmony.x.swing.ButtonCommons;
import org.apache.harmony.x.swing.Utilities;

/*
 * Appearence is taken from 1.5 release appearence.
 * Needs to be kept the same to 1.5 since client applications may use
 * this functionality in their UIs.
 */
public class BasicGraphicsUtils {
    public static void drawEtchedRect(final Graphics g,
                                      final int x,
                                      final int y,
                                      final int w,
                                      final int h,
                                      final Color shadow,
                                      final Color darkShadow,
                                      final Color highlight,
                                      final Color lightHighlight) {
        Color color = g.getColor();

        drawTwoLines(g, shadow, x, y + h - 2, x, y, x + w - 2, y);
        drawTwoLines(g, darkShadow, x + 1, y + h - 3,
                     x + 1, y + 1, x + w - 3, y + 1);
        drawTwoLines(g, lightHighlight, x, y + h - 1,
                     x + w - 1, y + h - 1, x + w - 1, y);
        drawTwoLines(g, highlight, x + 1, y + h - 2,
                     x + w - 2, y + h - 2, x + w - 2, y + 1);

        g.setColor(color);
    }

    public static Insets getEtchedInsets() {
        return new Insets(2, 2, 2, 2);
    }

    public static void drawGroove(final Graphics g,
                                  final int x,
                                  final int y,
                                  final int w,
                                  final int h,
                                  final Color shadow,
                                  final Color highlight) {
        drawLoweredBezel(g, x, y, w, h, highlight, shadow, shadow, highlight);
    }

    public static Insets getGrooveInsets() {
        return new Insets(2, 2, 2, 2);
    }

    public static void drawBezel(final Graphics g,
                                 final int x,
                                 final int y,
                                 final int w,
                                 final int h,
                                 final boolean isPressed,
                                 final boolean isDefault,
                                 final Color shadow,
                                 final Color darkShadow,
                                 final Color highlight,
                                 final Color lightHighlight) {
        if (!isDefault) {
            if (isPressed) {
                drawLoweredBezel(g, x, y, w, h,
                                 shadow, darkShadow, highlight, lightHighlight);
            } else {
                drawLoweredBezel(g, x, y, w, h,
                                 highlight, lightHighlight, shadow, darkShadow);
            }
            return;
        }

        Color color = g.getColor();
        if (isPressed) {
            g.setColor(shadow);
            g.drawRect(x + 1, y + 1, w - 3, h - 3);
        } else {
            drawLoweredBezel(g, x + 1, y + 1, w - 2, h - 2,
                             highlight, lightHighlight, shadow, darkShadow);
        }
        g.setColor(darkShadow);
        g.drawRect(x, y, w - 1, h - 1);
        g.setColor(color);
    }

    public static void drawLoweredBezel(final Graphics g,
                                        final int x,
                                        final int y,
                                        final int w,
                                        final int h,
                                        final Color shadow,
                                        final Color darkShadow,
                                        final Color highlight,
                                        final Color lightHighlight) {
        drawEtchedRect(g, x, y, w, h,
                       darkShadow, shadow, highlight, lightHighlight);
    }

    public static void drawString(final Graphics g,
                                  final String text,
                                  final int underlinedChar,
                                  final int x,
                                  final int y) {

        drawStringUnderlineCharAt(g, text,
                getDisplayedMnemonicIndex(text, underlinedChar), x, y);
    }

    public static void drawStringUnderlineCharAt(final Graphics g,
                                                 final String text,
                                                 final int underlinedIndex,
                                                 final int x,
                                                 final int y) {
        g.drawString(text, x, y);
        if (Utilities.insideString(text, underlinedIndex)) {
            final Rectangle box = getStringUnderscoreBounds(g.getFontMetrics(),
                                                            text, underlinedIndex);
            g.fillRect(x + box.x, y + box.y, box.width, box.height);
        }
    }

    public static void drawDashedRect(final Graphics g,
                                      final int x,
                                      final int y,
                                      final int width,
                                      final int height) {
        int maxX = x + width - 1;
        int maxY = y + height - 1;
        int dot;
        for (dot = x; dot < x + width; dot += 2) {
            g.drawLine(dot, y, dot, y);
            g.drawLine(dot, maxY, dot, maxY);
        }
        for (dot = y; dot < y + height; dot += 2) {
            g.drawLine(x, dot, x, dot);
            g.drawLine(maxX, dot, maxX, dot);
        }
    }

    public static Dimension getPreferredButtonSize(final AbstractButton b,
                                                   final int textIconGap) {
        return ButtonCommons.getPreferredSize(b, null, textIconGap);
    }

    private static void drawTwoLines(final Graphics g, final Color c,
                                     final int x1, final int y1,
                                     final int x2, final int y2,
                                     final int x3, final int y3) {
        int xs[] = { x1, x2, x3 };
        int ys[] = { y1, y2, y3 };
        g.setColor(c);
        g.drawPolyline(xs, ys, 3);
    }

    private static Rectangle getStringUnderscoreBounds(final FontMetrics fm,
                                                       final String str,
                                                       final int underscoreIndex) {
        final int width = fm.charWidth(str.charAt(underscoreIndex));
        final int height = 1;
        final int x = underscoreIndex > 0
                ? fm.stringWidth(str.substring(0, underscoreIndex)) : 0;
        final int y = fm.getDescent() - height;
        return new Rectangle(x, y, width, height);
    }

    private static int getDisplayedMnemonicIndex(final String text,
                                                 final int mnemonicChar) {
        if (text == null) {
            return -1;
        }

        int index = text.toUpperCase().indexOf(mnemonicChar);
        return index != -1 ? index : text.toLowerCase().indexOf(mnemonicChar);

        // TODO: uncomment after moving to 1.5
//        return text != null
//                ? text.toUpperCase().indexOf(Character.toUpperCase(mnemonicChar))
//                : -1;
    }
}

