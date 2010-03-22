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
 * @author Sergey Burlak
 */

package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.apache.harmony.x.swing.Utilities;


public class BasicArrowButton extends JButton implements SwingConstants {
    protected int direction;

    private static Color shadow = UIManager.getColor("ScrollBar.shadow");
    private static Color darkShadow = UIManager.getColor("ScrollBar.darkShadow");

    public BasicArrowButton(final int direction) {
        this.direction = direction;
    }

    public BasicArrowButton(final int direction, final Color background, final Color shadow,
                            final Color darkShadow, final Color highlight) {
        BasicArrowButton.shadow = shadow;
        BasicArrowButton.darkShadow = darkShadow;
        this.direction = direction;
        setBackground(background);
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(final int dir) {
        direction = dir;
    }

    public void paint(final Graphics g) {
        super.paint(g);
        int arrowSize = Math.min(getSize().width, getSize().height) / 2;
        int x = (getSize().width - arrowSize) / 2 + 1;
        int y = (getSize().height - arrowSize) / 2 + 1;
        paintTriangle(g, x, y, arrowSize, getDirection(), isEnabled());
    }

    public Dimension getPreferredSize() {
        return new Dimension(16, 16);
    }

    public Dimension getMinimumSize() {
        return new Dimension(5, 5);
    }

    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public boolean isFocusTraversable() {
        return false;
    }

    public void paintTriangle(final Graphics g, final int x, final int y, final int size, final int direction, final boolean isEnabled) {
        Utilities.fillArrow(g, x, y, direction, size, false, isEnabled ? darkShadow : shadow);
    }
}
