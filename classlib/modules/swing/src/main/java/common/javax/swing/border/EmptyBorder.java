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
import java.io.Serializable;

public class EmptyBorder extends AbstractBorder implements Serializable {

    protected int left;
    protected int right;
    protected int top;
    protected int bottom;

    public EmptyBorder(final Insets insets) {
        top = insets.top;
        left = insets.left;
        right = insets.right;
        bottom = insets.bottom;
    }

    public EmptyBorder(final int top, final int left, final int bottom, final int right) {
        this.top = top;
        this.left = left;
        this.right = right;
        this.bottom = bottom;
    }

    /**
     *  This constructor initialize vertical and horizontal insets
     *  with the same value
     *
     * @param topBottom - specifies vertical (top and bottom) insets
     * @param leftRight - specifies horizontal (left and right) insets
     *
     */
    EmptyBorder(final int topBottom, final int leftRight) {
        top = topBottom;
        bottom = topBottom;

        left = leftRight;
        right = leftRight;
    }

    public Insets getBorderInsets(final Component component, final Insets insets) {
        if (insets != null) {
            insets.top = top;
            insets.left = left;
            insets.right = right;
            insets.bottom = bottom;

            return insets;
        }

        return getBorderInsets(component);
    }

    public Insets getBorderInsets(final Component component) {
        return new Insets(top, left, bottom, right);
    }

    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
    }

    public Insets getBorderInsets() {
        return getBorderInsets(null);
    }

    public boolean isBorderOpaque() {
        return false;
    }

}

