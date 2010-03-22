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

package javax.swing.plaf.synth;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;

import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.apache.harmony.x.swing.ButtonCommons;
import org.apache.harmony.x.swing.Utilities;

public class SynthGraphicsUtils {

    /**
     * Calculate string width with given parameters
     * 
     * @param context
     *            SynthContext to specify component
     * @param font
     *            Font used to draw the string (if null font calculates from the
     *            SynthContext)
     * @param metrics
     *            FontMetrics used to draw the string (if null metrics
     *            calculates from the SynthContext)
     * @param text
     *            String to draw
     */
    public int computeStringWidth(SynthContext context, Font font,
            FontMetrics metrics, String text) {
        metrics = computeFontMetrics(context, font, metrics);

        return metrics.stringWidth(text);
    }

    /**
     * Calculate string height from Context
     * 
     * @param context
     *            SynthContext to specify component
     */
    public int getMaximumCharHeight(SynthContext context) {
        Rectangle2D maxBounds = context.getStyle().getFont(context)
                .getMaxCharBounds(new FontRenderContext(null, true, true));

        return (int) Math.ceil(maxBounds.getHeight());
    }

    /**
     * Draws a line
     */
    @SuppressWarnings("unused")
    public void drawLine(SynthContext context, Object paintKey, Graphics g,
            int x1, int y1, int x2, int y2) {
        g.setColor(context.getStyle().getColor(context, ColorType.FOREGROUND));
        g.drawLine(x1, y1, x2, y2);
    }

    /**
     * Calculates the maximum size for the component with given parameters
     */
    @SuppressWarnings("unused")
    public Dimension getPreferredSize(SynthContext ss, Font font, String text,
            Icon icon, int hAlign, int vAlign, int hTextPosition,
            int vTextPosition, int iconTextGap, int mnemonicIndex) {

        final FontMetrics fm = computeFontMetrics(ss, font, null);

        Dimension size = getCompoundLabelSize(fm, text, icon, vTextPosition,
                hTextPosition, iconTextGap);

        return Utilities.addInsets(size, ss.getStyle().getInsets(ss, null));
    }

    /**
     * Calculates the minimum size for the component with given parameters
     */
    public Dimension getMinimumSize(SynthContext ss, Font font, String text,
            Icon icon, int hAlign, int vAlign, int hTextPosition,
            int vTextPosition, int iconTextGap, int mnemonicIndex) {

        return getPreferredSize(ss, font, text, icon, hAlign, vAlign,
                hTextPosition, vTextPosition, iconTextGap, mnemonicIndex);
    }

    /**
     * Calculates the preferred size for the component with given parameters
     */
    public Dimension getMaximumSize(SynthContext ss, Font font, String text,
            Icon icon, int hAlign, int vAlign, int hTextPosition,
            int vTextPosition, int iconTextGap, int mnemonicIndex) {

        return getPreferredSize(ss, font, text, icon, hAlign, vAlign,
                hTextPosition, vTextPosition, iconTextGap, mnemonicIndex);
    }

    /**
     * Layouts text and Icon in the complex JComponent
     */
    public String layoutText(SynthContext ss, FontMetrics fm, String text,
            Icon icon, int hAlign, int vAlign, int hTextPosition,
            int vTextPosition, Rectangle viewR, Rectangle iconR,
            Rectangle textR, int iconTextGap) {

        fm = computeFontMetrics(ss, fm);

        SwingUtilities.layoutCompoundLabel(fm, text, icon, vAlign, hAlign,
                vTextPosition, hTextPosition, viewR, iconR, textR, iconTextGap);

        return text;
    }

    public void paintText(SynthContext ss, Graphics g, String text, Icon icon,
            int hAlign, int vAlign, int hTextPosition, int vTextPosition,
            int iconTextGap, int mnemonicIndex, int textOffset) {

        FontMetrics metrics = computeFontMetrics(ss, null);

        Color color = ss.getStyle().getColor(ss, ColorType.TEXT_FOREGROUND);

        Rectangle textR = getTextRect(text, metrics);
        Rectangle iconR = getIconRect(icon);

        Insets insets = ss.getStyle().getInsets(ss, null);
        Rectangle viewR = Utilities.subtractInsets(g.getClipBounds(), insets);

        layoutText(ss, metrics, text, icon, hAlign, vAlign, hTextPosition,
                vTextPosition, viewR, iconR, textR, iconTextGap);

        textR.translate(textOffset, textOffset);

        ButtonCommons.paintText(g, metrics, text, mnemonicIndex, textR, text,
                color);

    }

    public void paintText(SynthContext ss, Graphics g, String text, int x,
            int y, int mnemonicIndex) {

        Color color = ss.getStyle().getColor(ss, ColorType.TEXT_FOREGROUND);
        FontMetrics metrics = computeFontMetrics(ss, null);
        Rectangle textR = getTextRect(text, metrics);
        textR.x = x;
        textR.y = y;
        ButtonCommons.paintText(g, metrics, text, mnemonicIndex, textR, text,
                color);
    }

    public void paintText(SynthContext ss, Graphics g, String text,
            Rectangle bounds, int mnemonicIndex) {

        Color color = ss.getStyle().getColor(ss, ColorType.TEXT_FOREGROUND);
        FontMetrics metrics = computeFontMetrics(ss, null);
        ButtonCommons.paintText(g, metrics, text, mnemonicIndex, bounds, text,
                color);
    }

    /**
     * This method calculates Font metrics from context
     */
    private FontMetrics computeFontMetrics(SynthContext context, Font font,
            FontMetrics metrics) {

        if (metrics == null) {

            if (font == null) {
                font = context.getStyle().getFont(context);
            }

            metrics = context.getComponent().getFontMetrics(font);
        }

        return metrics;
    }

    /**
     * This method calculates Font metrics from context
     */
    private FontMetrics computeFontMetrics(SynthContext context,
            FontMetrics metrics) {

        if (metrics == null) {

            metrics = context.getComponent().getFontMetrics(
                    context.getStyle().getFont(context));
        }

        return metrics;
    }

    /**
     * @param text
     *            text to be outlined
     * @param fm
     *            the FontMetrics of the text
     * @return outline Rectangle
     */
    private Rectangle getTextRect(String text, FontMetrics fm) {

        if (!Utilities.isEmptyString(text)) {
            Dimension stringDim = Utilities.getStringSize(text, fm);
            Dimension size = new Dimension(stringDim.width, Utilities.getTextY(
                    fm, new Rectangle(stringDim)));

            return new Rectangle(size);
        }

        return new Rectangle();
    }

    private Dimension getCompoundLabelSize(FontMetrics fm, final String text,
            final Icon icon, final int verticalTextPosition,
            final int horizontalTextPosition, final int iconTextGap) {

        final Dimension result = new Dimension();
        Rectangle viewR = new Rectangle(Short.MAX_VALUE, Short.MAX_VALUE);
        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();

        SwingUtilities.layoutCompoundLabel(fm, text, icon, SwingConstants.TOP,
                SwingConstants.LEFT, verticalTextPosition,
                horizontalTextPosition, viewR, iconR, textR, iconTextGap);

        result.width = Math.max(iconR.x + iconR.width, textR.x + textR.width);
        result.height = Math
                .max(iconR.y + iconR.height, textR.y + textR.height);

        return result;
    }

    /**
     * @param icon
     *            Icon to be outlined
     * @return outline Rectangle
     */
    private Rectangle getIconRect(Icon icon) {

        if (icon != null) {
            return new Rectangle(icon.getIconWidth(), icon.getIconHeight());
        }

        return new Rectangle();
    }

}
