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

package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicArrowButton;

public class MetalScrollButton extends BasicArrowButton {
    private int width;
    private boolean freeStanding;
    private static Color background = UIManager.getColor("ScrollBar.background");
    private static Color shadow = UIManager.getColor("ScrollBar.shadow");
    private static Color darkShadow = Color.black;
    private static Color highlight = UIManager.getColor("ScrollBar.highlight");;

    public MetalScrollButton(final int direction, final int width, final boolean freeStanding) {
        super(direction, background, shadow, darkShadow, highlight);

        this.width = width;
        this.freeStanding = freeStanding;
    }

    public void paint(final Graphics g) {
        super.paint(g);
    }

    public Dimension getPreferredSize() {
        if (super.getPreferredSize() == null) {
            return new Dimension(0, 0);
        }
        return new Dimension(width, super.getPreferredSize().height);
    }

    public Dimension getMinimumSize() {
        if (super.getMinimumSize() == null) {
            return new Dimension(0, 0);
        }
        return super.getMinimumSize();
    }

    public Dimension getMaximumSize() {
        if (super.getMaximumSize() == null) {
            return new Dimension(0, 0);
        }
        return super.getMaximumSize();
    }

    public int getButtonWidth() {
        return width;
    }

    public void setFreeStanding(final boolean freeStanding) {
        this.freeStanding = freeStanding;
    }
}