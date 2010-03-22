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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class TitledBorder extends AbstractBorder {

    public static final int DEFAULT_POSITION = 0;
    public static final int ABOVE_TOP = 1;
    public static final int TOP = 2;
    public static final int BELOW_TOP = 3;
    public static final int ABOVE_BOTTOM = 4;
    public static final int BOTTOM = 5;
    public static final int BELOW_BOTTOM = 6;


    public static final int DEFAULT_JUSTIFICATION = 0;
    public static final int LEFT = 1;
    public static final int CENTER = 2;
    public static final int RIGHT = 3;
    public static final int LEADING = 4;
    public static final int TRAILING = 5;


    protected static final int EDGE_SPACING = 2;
    protected static final int TEXT_SPACING = 2;
    protected static final int TEXT_INSET_H = 5;

    protected String title;
    protected int titlePosition;
    protected int titleJustification;

    protected Border border;
    protected Font titleFont;
    protected Color titleColor;

    private static final String PROPERTY_PREFIX = "TitledBorder.";

    private static Border defaultBorder;
    private static Font defaultTitleFont;
    private static Color defaultTitleColor;

    public TitledBorder(final Border border, final String title, final int titleJustification,
                        final int titlePosition, final Font titleFont, final Color titleColor) {
        setTitleJustification(titleJustification);
        setTitlePosition(titlePosition);
        this.title = title;

        setBorder(border);
        setTitleFont(titleFont);
        setTitleColor(titleColor);
    }

    public TitledBorder(final Border border, final String title, final int titleJustification,
                        final int titlePosition, final Font titleFont) {
        this(border, title, titleJustification, titlePosition, titleFont, null);
    }

    public TitledBorder(final Border border, final String title, final int titleJustification, final int titlePosition) {
        this(border, title, titleJustification, titlePosition, null, null);
    }

    public TitledBorder(final Border border, final String title) {
        this(border, title, LEADING, TOP, null, null);
    }

    public TitledBorder(final Border border) {
        this(border, "", LEADING, TOP, null, null);
    }

    public TitledBorder(final String title) {
        this(null, title, LEADING, TOP, null, null);
    }

    public Insets getBorderInsets(final Component c, final Insets insets) {
        int spacing = EDGE_SPACING + TEXT_SPACING;
        insets.set(spacing, spacing, spacing, spacing);

        Insets insideInsets = getBorder().getBorderInsets(c);
        Utilities.addInsets(insets, insideInsets);

        if (!Utilities.isEmptyString(title) && c != null) {
            FontMetrics metrics = c.getFontMetrics(getTitleFont());
            switch (titlePosition) {
            case DEFAULT_POSITION:
            case TOP:
                insets.top += metrics.getHeight() - TEXT_SPACING;
                break;
            case ABOVE_TOP:
            case BELOW_TOP:
                insets.top += metrics.getHeight();
                break;
            case ABOVE_BOTTOM:
            case BELOW_BOTTOM:
                insets.bottom += metrics.getHeight();
                break;
            case BOTTOM:
                insets.bottom += metrics.getHeight() - TEXT_SPACING;
            }
        }

        return insets;
    }

    public Insets getBorderInsets(final Component c) {
        return getBorderInsets(c,  new Insets(0, 0, 0, 0));
    }

    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
        Color oldColor = g.getColor();
        Font oldFont = g.getFont();
        Border innerBorder = getBorder();
        Insets borderInsets = innerBorder.getBorderInsets(c);

        Font font = getTitleFont();
        FontMetrics metrics = c.getFontMetrics(font);
        int stringWidth = metrics.stringWidth(title);
        int stringHeight = metrics.getHeight();
        int borderTopInset = 0;
        int borderHeight = height;
        int titleTopInset = (metrics.getAscent() - metrics.getDescent())/2;
        int titleLeftInset = 0;
        boolean titleOverBorder = false;
        Shape oldClip = g.getClip();

        if (!Utilities.isEmptyString(title)) {
            switch (convertLeadTrail(titleJustification, c)) {
            case DEFAULT_JUSTIFICATION:
            case LEFT:
                titleLeftInset = EDGE_SPACING + 2*TEXT_SPACING + borderInsets.left;
                break;
            case CENTER:
                titleLeftInset = (width - stringWidth)/2;
                break;
            case RIGHT:
                titleLeftInset = width - stringWidth - borderInsets.right -
                                 2*TEXT_SPACING - EDGE_SPACING;
                break;
            }

            switch (titlePosition) {
            case ABOVE_TOP:
                borderTopInset = stringHeight;
                titleTopInset += stringHeight/2 + EDGE_SPACING;
                borderHeight -= borderTopInset;
                break;
            case DEFAULT_POSITION:
            case TOP:
                borderTopInset = metrics.getAscent()/2;
                titleTopInset += borderTopInset + borderInsets.top/2 +
                                 EDGE_SPACING;
                borderHeight -= borderTopInset;
                titleOverBorder = (stringHeight > borderInsets.top);
                break;
            case BELOW_TOP:
                titleTopInset += borderInsets.top + stringHeight/2 +
                                 EDGE_SPACING + TEXT_SPACING;
                break;
            case ABOVE_BOTTOM:
                titleTopInset += height - borderInsets.bottom - stringHeight/2 -
                                 EDGE_SPACING - TEXT_SPACING;
                break;
            case BOTTOM:
                titleTopInset += height - borderInsets.bottom/2 - stringHeight/2 -
                                 EDGE_SPACING;
                borderHeight -= metrics.getAscent()/2 + EDGE_SPACING + 1;
                titleOverBorder = (stringHeight > borderInsets.bottom);
                break;
            case BELOW_BOTTOM:
                titleTopInset += height - stringHeight/2 - 1;
                borderHeight -= stringHeight + 1;
                break;
            }

            if (titleOverBorder) {
                Rectangle titleRect = new Rectangle(x + titleLeftInset - 1,
                                                    y - stringHeight + titleTopInset - TEXT_SPACING,
                                                    stringWidth + TEXT_SPACING,
                                                    stringHeight + 2*TEXT_SPACING);

                if (titleRect.intersects(g.getClipBounds())) {
                    Rectangle[] difference = SwingUtilities.computeDifference(g.getClipBounds(), titleRect);
                    MultiRectArea clipArea = new MultiRectArea(difference);
                    g.setClip(clipArea);
                }
            }
        }

        innerBorder.paintBorder(c, g, x + EDGE_SPACING,
                                      y + EDGE_SPACING + borderTopInset,
                                      width - 2 * EDGE_SPACING,
                                      borderHeight - 2 * EDGE_SPACING);

        g.setClip(oldClip);
        if (!Utilities.isEmptyString(title)) {
            g.setColor(getTitleColor());
            g.setFont(font);
            g.drawString(title, x + titleLeftInset, y + titleTopInset);
        }

        g.setColor(oldColor);
        g.setFont(oldFont);
    }

    public Dimension getMinimumSize(final Component c) {
        int width = 0;
        if (!Utilities.isEmptyString(title)) {
            FontMetrics metrics = c.getFontMetrics(getTitleFont());
            if (titlePosition != ABOVE_BOTTOM && titlePosition != ABOVE_TOP) {
                width += metrics.stringWidth(title);
            } else {
                width = Math.max(width, metrics.stringWidth(title));
            }
        }

        return Utilities.addInsets(new Dimension(width, 0), getBorderInsets(c));
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setBorder(final Border border) {
        this.border = border;
    }

    public Border getBorder() {
        return (border != null) ? border : getDefaultBorder();
    }

    public void setTitleFont(final Font font) {
        titleFont = font;
    }

    public Font getTitleFont() {
        return (titleFont != null) ? titleFont : getDefaultTitleFont();
    }

    public void setTitleColor(final Color color) {
        titleColor = color;
    }

    public Color getTitleColor() {
        return (titleColor != null) ? titleColor : getDefaultTitleColor();
    }

    public void setTitlePosition(final int pos) {
        if (pos < DEFAULT_POSITION || BELOW_BOTTOM < pos) {
            throw new IllegalArgumentException(Messages.getString("swing.24",pos)); //$NON-NLS-1$
        }
        titlePosition = pos;
    }

    public int getTitlePosition() {
        return titlePosition;
    }

    public void setTitleJustification(final int justification) {
        if (justification < DEFAULT_JUSTIFICATION || TRAILING < justification) {
            throw new IllegalArgumentException(Messages.getString("swing.23",justification)); //$NON-NLS-1$
        }
        titleJustification = justification;
    }

    public int getTitleJustification() {
        return titleJustification;
    }

    public boolean isBorderOpaque() {
        return false;
    }

    protected Font getFont(final Component c) {
        return getTitleFont();
    }

    private static Font getDefaultTitleFont() {
        return (defaultTitleFont != null) ? defaultTitleFont :
            UIManager.getDefaults().getFont(PROPERTY_PREFIX + "font");
    }

    private static Color getDefaultTitleColor() {
        return (defaultTitleColor != null) ? defaultTitleColor :
            UIManager.getDefaults().getColor(PROPERTY_PREFIX + "titleColor");
    }

    private Border getDefaultBorder() {
        return (defaultBorder != null) ? defaultBorder :
            UIManager.getDefaults().getBorder(PROPERTY_PREFIX + "border");
    }

    private static int convertLeadTrail(final int pos, final Component c) {
        final boolean isLTR = (c == null) || c.getComponentOrientation().isLeftToRight();
        if (pos == LEADING) {
            return isLTR ? LEFT : RIGHT;
        }
        if (pos == TRAILING) {
            return isLTR ? RIGHT : LEFT;
        }
        return pos;
    }
}
