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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.Serializable;

public abstract class AbstractBorder implements Border, Serializable {

    public Insets getBorderInsets(final Component component, final Insets insets) {
        if (insets != null) {
            insets.top = 0;
            insets.left = 0;
            insets.right = 0;
            insets.bottom = 0;

            return insets;
        } else {
            return new Insets(0, 0, 0, 0);
        }
    }

    public Rectangle getInteriorRectangle(final Component c, final int x, final int y, final int width, final int height) {
        return AbstractBorder.getInteriorRectangle(c, this, x, y, width, height);
    }

    public Insets getBorderInsets(final Component component) {
        return new Insets(0, 0, 0, 0);
    }

    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
    }

    public boolean isBorderOpaque() {
        return false;
    }

    public static Rectangle getInteriorRectangle(final Component component, final Border border, final int x, final int y, final int width, final int height) {
        Rectangle result = new Rectangle(x, y, width, height);
        if (border != null) {
            Insets insets = border.getBorderInsets(component);
            result.x += insets.left;
            result.y += insets.top;
            result.width -= insets.left + insets.right;
            result.height -= insets.top + insets.bottom;
        }
        return result;
    }
}

