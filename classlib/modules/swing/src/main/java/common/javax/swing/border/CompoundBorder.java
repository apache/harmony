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

import org.apache.harmony.x.swing.Utilities;


public class CompoundBorder extends AbstractBorder {

    protected Border outsideBorder;
    protected Border insideBorder;

    public CompoundBorder() {
    }

    public CompoundBorder(final Border outside, final Border inside) {
        insideBorder = inside;
        outsideBorder = outside;
    }

    public Insets getBorderInsets(final Component component, final Insets insets) {
        insets.top = 0;
        insets.left = 0;
        insets.right = 0;
        insets.bottom = 0;

        if (insideBorder != null) {
            Utilities.addInsets(insets, insideBorder.getBorderInsets(component));
        }
        if (outsideBorder != null) {
            Utilities.addInsets(insets, outsideBorder.getBorderInsets(component));
        }

        return insets;
    }

    public Insets getBorderInsets(final Component component) {
        return getBorderInsets(component, new Insets(0, 0, 0, 0));
    }

    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
        if (outsideBorder != null) {
            outsideBorder.paintBorder(c, g, x, y, width, height);
            if (insideBorder != null) {
                Insets offset = outsideBorder.getBorderInsets(c);
                insideBorder.paintBorder(c, g, x + offset.left,
                                               y + offset.top,
                                               width - offset.left - offset.right,
                                               height - offset.top - offset.bottom);
            }
        } else if (insideBorder != null) {
            insideBorder.paintBorder(c, g, x, y, width, height);
        }
    }

    public Border getOutsideBorder() {
        return outsideBorder;
    }

    public Border getInsideBorder() {
        return insideBorder;
    }

    public boolean isBorderOpaque() {
        return (insideBorder == null || insideBorder.isBorderOpaque()) &&
               (outsideBorder == null || outsideBorder.isBorderOpaque());
    }

}

