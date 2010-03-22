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

import org.apache.harmony.x.swing.internal.nls.Messages;


public class BevelBorder extends AbstractBorder {

    public static final int RAISED = 0;
    public static final int LOWERED = 1;

    private static final int INSETS_SIZE = 2;

    protected int bevelType;

    protected Color highlightOuter;
    protected Color highlightInner;
    protected Color shadowInner;
    protected Color shadowOuter;

    public BevelBorder(final int bevelType) {
        this.bevelType = bevelType;
    }

    public BevelBorder(final int bevelType, final Color highlightOuterColor, final Color highlightInnerColor, final Color shadowOuterColor, final Color shadowInnerColor) {
        this(bevelType);

        if (highlightOuterColor == null || highlightInnerColor == null ||
                shadowOuterColor == null || shadowInnerColor == null) {
            throw new NullPointerException(Messages.getString("swing.68")); //$NON-NLS-1$
        }
        highlightOuter = highlightOuterColor;
        highlightInner = highlightInnerColor;
        shadowOuter = shadowOuterColor;
        shadowInner = shadowInnerColor;
    }

    public BevelBorder(final int bevelType, final Color highlight, final Color shadow) {
        this(bevelType, highlight, highlight, shadow, shadow);
    }

    int getInsetSize() {
        return INSETS_SIZE;
    }

    public Insets getBorderInsets(final Component c, final Insets insets) {
        int insetSize = getInsetSize();

        insets.left = insetSize;
        insets.top = insetSize;
        insets.bottom = insetSize;
        insets.right = insetSize;

        return insets;
    }

    public Insets getBorderInsets(final Component c) {
        int insetSize = getInsetSize();
        return new Insets(insetSize, insetSize, insetSize, insetSize);
    }

    protected void paintRaisedBevel(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
        Color oldColor = g.getColor();
        Color color = getShadowOuterColor(c);
        g.setColor(color);
        g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
        g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);

        color = getShadowInnerColor(c);
        g.setColor(color);
        g.drawLine(x + 1, y + height - 2, x + width - 2, y + height - 2);
        g.drawLine(x + width - 2, y + 1, x + width - 2, y + height - 2);

        color = getHighlightOuterColor(c);
        g.setColor(color);
        g.drawLine(x + 1, y, x + width - 2, y);
        g.drawLine(x, y + 1, x, y + height - 2);

        color = getHighlightInnerColor(c);
        g.setColor(color);
        g.drawLine(x + 2, y + 1, x + width - 3, y + 1);
        g.drawLine(x + 1, y + 2, x + 1, y + height - 3);
        g.setColor(oldColor);
    }

    protected void paintLoweredBevel(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
        Color oldColor = g.getColor();
        Color color = getShadowInnerColor(c);
        g.setColor(color);
        g.drawLine(x, y, x + width - 1, y);
        g.drawLine(x, y, x, y + height - 1);

        color = getShadowOuterColor(c);
        g.setColor(color);
        g.drawLine(x + 1, y + 1, x + width - 2, y + 1);
        g.drawLine(x + 1, y + 1, x + 1, y + height - 2);

        color = getHighlightOuterColor(c);
        g.setColor(color);
        g.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
        g.drawLine(x + width - 1, y + 1, x + width - 1, y + height - 2);

        color = getHighlightInnerColor(c);
        g.setColor(color);
        g.drawLine(x + 2, y + height - 2, x + width - 2, y + height - 2);
        g.drawLine(x + width - 2, y + 2, x + width - 2, y + height - 3);
        g.setColor(oldColor);
    }

    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
        if (bevelType == BevelBorder.LOWERED) {
            paintLoweredBevel(c, g, x, y, width, height);
        } else if (bevelType == BevelBorder.RAISED){
            paintRaisedBevel(c, g, x, y, width, height);
        }
    }

    public Color getShadowOuterColor(final Component c) {
        return (shadowOuter != null) ? shadowOuter : c.getBackground().darker().darker();
    }

    public Color getShadowInnerColor(final Component c) {
        return (shadowInner != null) ? shadowInner : c.getBackground().darker();
    }

    public Color getHighlightOuterColor(final Component c) {
        return (highlightOuter != null) ? highlightOuter : c.getBackground().brighter().brighter();
    }

    public Color getHighlightInnerColor(final Component c) {
        return (highlightInner != null) ? highlightInner : c.getBackground().brighter();
    }

    public Color getShadowOuterColor() {
        return shadowOuter;
    }

    public Color getShadowInnerColor() {
        return shadowInner;
    }

    public Color getHighlightOuterColor() {
        return highlightOuter;
    }

    public Color getHighlightInnerColor() {
        return highlightInner;
    }

    public boolean isBorderOpaque() {
        return true;
    }

    public int getBevelType() {
        return bevelType;
    }

}
