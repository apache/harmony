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

public class SoftBevelBorder extends BevelBorder {

    private static final int INSETS_SIZE = 3;

    public SoftBevelBorder(final int bevelType, final Color highlightOuterColor, final Color highlightInnerColor, final Color shadowOuterColor, final Color shadowInnerColor) {
        super(bevelType, highlightOuterColor, highlightInnerColor, shadowOuterColor, shadowInnerColor);
    }

    public SoftBevelBorder(final int bevelType, final Color highlight, final Color shadow) {
        super(bevelType, highlight, shadow);
    }

    public SoftBevelBorder(final int bevelType) {
        super(bevelType);
    }

    int getInsetSize() {
        return INSETS_SIZE;
    }

    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
        Color oldColor = g.getColor();
        Color color = (bevelType == BevelBorder.RAISED) ? getShadowOuterColor(c) :
                                                          getHighlightOuterColor(c);
        g.setColor(color);
        g.drawLine(x + 2, y + height - 1, x + width - 1, y + height - 1);
        g.drawLine(x + width - 1, y + 2, x + width - 1, y + height - 1);

        color = (bevelType == BevelBorder.RAISED) ? getShadowInnerColor(c) :
                                                    getHighlightInnerColor(c);
        g.setColor(color);
        g.drawLine(x + width - 2, y + height - 2, x + width - 2, y + height - 2);

        color = (bevelType == BevelBorder.RAISED) ? getHighlightInnerColor(c) :
                                                    getShadowInnerColor(c);
        g.setColor(color);
        g.drawLine(x + 2, y + 1, x + width - 2, y + 1);
        g.drawLine(x + 1, y + 2, x + 1, y + height - 2);
        g.drawLine(x, y, x + width - 1, y);
        g.drawLine(x, y, x, y + height - 1);
        g.drawLine(x + 2, y + 2, x + 2, y + 2);

        color = (bevelType == BevelBorder.RAISED) ? getHighlightOuterColor(c) :
                                                    getShadowOuterColor(c);
        g.setColor(color);
        g.drawLine(x, y, x + width - 2, y);
        g.drawLine(x, y, x, y + height - 3);
        g.drawLine(x + 1, y + 1, x + 1, y + 1);
        g.setColor(oldColor);
    }

    public boolean isBorderOpaque() {
        return false;
    }
}

