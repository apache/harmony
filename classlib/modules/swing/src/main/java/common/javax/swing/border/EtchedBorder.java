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
import java.security.InvalidParameterException;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class EtchedBorder extends AbstractBorder {

    public static final int RAISED = 0;
    public static final int LOWERED = 1;

    protected int etchType = LOWERED;;
    protected Color highlight;
    protected Color shadow;

    private static final int INSETS_SIZE = 2;
    private static final String INCORRECT_BORDER_TYPE_EXCEPTION_TEXT = Messages.getString("swing.1D"); //$NON-NLS-1$

    public EtchedBorder() {
    }

    public EtchedBorder(final Color highlightColor, final Color shadowColor) {
        highlight = highlightColor;
        shadow = shadowColor;
    }

    public EtchedBorder(final int etchType, final Color highlightColor, final Color shadowColor) {
        if (etchType != RAISED && etchType != LOWERED) {
            throw new InvalidParameterException(INCORRECT_BORDER_TYPE_EXCEPTION_TEXT);
        }
        this.etchType = etchType;
        highlight = highlightColor;
        shadow = shadowColor;
    }

    public EtchedBorder(final int etchType) {
        if (etchType != RAISED && etchType != LOWERED) {
            throw new InvalidParameterException(INCORRECT_BORDER_TYPE_EXCEPTION_TEXT);
        }
        this.etchType = etchType;
    }

    public Insets getBorderInsets(final Component c, final Insets insets) {
        insets.left = INSETS_SIZE;
        insets.top = INSETS_SIZE;
        insets.bottom = INSETS_SIZE;
        insets.right = INSETS_SIZE;

        return insets;
    }

    public Insets getBorderInsets(final Component c) {
        return new Insets(INSETS_SIZE, INSETS_SIZE, INSETS_SIZE, INSETS_SIZE);
    }

    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
        Color oldColor = g.getColor();
        Color color = (etchType == EtchedBorder.RAISED) ? getShadowColor(c) : getHighlightColor(c);
        g.setColor(color);
        g.drawRect(x, y, width - 1, height - 1);
        g.drawRect(x + 1, y + 1, width - 3, height - 3);
        color = (etchType == EtchedBorder.RAISED) ? getHighlightColor(c) : getShadowColor(c);
        g.setColor(color);
        g.drawRect(x, y, width - 2, height - 2);
        g.setColor(oldColor);
    }

    public Color getShadowColor(final Component c) {
        return (shadow != null) ? shadow : c.getBackground().darker();
    }

    public Color getHighlightColor(final Component c) {
        return (highlight != null) ? highlight : c.getBackground().brighter();
    }

    public Color getShadowColor() {
        return shadow;
    }

    public Color getHighlightColor() {
        return highlight;
    }

    public boolean isBorderOpaque() {
        return true;
    }

    public int getEtchType() {
        return etchType;
    }

}


