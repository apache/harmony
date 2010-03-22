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
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicScrollBarUI;

import org.apache.harmony.x.swing.Utilities;


public class MetalScrollBarUI extends BasicScrollBarUI {

    public static final String FREE_STANDING_PROP = "JScrollBar.isFreeStanding";

    protected MetalBumps bumps;
    protected MetalScrollButton increaseButton;
    protected MetalScrollButton decreaseButton;
    protected int scrollBarWidth;
    protected boolean isFreeStanding;
    
    // We need to hide these fields
    private Color thumbColor;
    private Color thumbHighlightColor;

    protected void paintTrack(final Graphics g, final JComponent c, final Rectangle r) {
        Color old = g.getColor();
        Utilities.draw3DRect(g, r.x, r.y, r.width, r.height - 1, Color.DARK_GRAY, Color.WHITE, false);
        g.setColor(old);
    }

    protected void paintThumb(final Graphics g, final JComponent c, final Rectangle r) {
        Color old = g.getColor();
        Utilities.draw3DRect(g, r.x + 1, r.y + 1, r.width - 2, r.height - 2, Color.DARK_GRAY, Color.WHITE, true);
        g.setColor(thumbColor);
        g.fillRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4);

        MetalBumps.paintBumps(g, r.x + 4, r.y + 4, r.width - 8, r.height - 8, thumbColor.darker(), thumbColor.brighter());

        g.setColor(old);
    }

    protected JButton createIncreaseButton(final int orient) {
        increaseButton = new MetalScrollButton(orient, scrollBarWidth, isFreeStanding);
        return increaseButton;
    }

    protected JButton createDecreaseButton(final int orient) {
        decreaseButton = new MetalScrollButton(orient, scrollBarWidth, isFreeStanding);
        return decreaseButton;
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler() {
            public void propertyChange(final PropertyChangeEvent e) {
                if (e.getPropertyName().equals(FREE_STANDING_PROP)) {
                    if (e.getNewValue() != null) {
                        isFreeStanding = ((Boolean)e.getNewValue()).booleanValue();
                    } else {
                        isFreeStanding = false;
                    }
                }
            }
        };
    }

    protected Dimension getMinimumThumbSize() {
        return super.getMinimumThumbSize();
    }

    protected void setThumbBounds(final int x, final int y, final int w, final int h) {
        super.setThumbBounds(x, y, w, h);
    }

    protected void installListeners() {
        super.installListeners();
    }

    protected void installDefaults() {
        scrollBarWidth = UIManager.getInt("ScrollBar.width");

        configureScrollBarColors();

        if ((maximumThumbSize == null) || (maximumThumbSize instanceof UIResource)) {
            maximumThumbSize = UIManager.getDimension("ScrollBar.maximumThumbSize");
        }
        if ((minimumThumbSize == null) || (minimumThumbSize instanceof UIResource)) {
            minimumThumbSize = UIManager.getDimension("ScrollBar.minimumThumbSize");
        }

        scrollbar.setLayout(this);

        LookAndFeel.installBorder(scrollbar, "ScrollBar.border");
        super.installDefaults();
    }

    protected void configureScrollBarColors() {
        LookAndFeel.installColors(scrollbar, "ScrollBar.background", "ScrollBar.foreground");

        if ((thumbColor == null) || (thumbColor instanceof UIResource)) {
            thumbColor = UIManager.getColor("ScrollBar.thumb");
        }
        if ((thumbDarkShadowColor == null) || (thumbDarkShadowColor instanceof UIResource)) {
            thumbDarkShadowColor = UIManager.getColor("ScrollBar.thumbDarkShadow");
        }
        if ((thumbHighlightColor == null) || (thumbHighlightColor instanceof UIResource)) {
            thumbHighlightColor = UIManager.getColor("ScrollBar.thumbHighlight");
        }
        if ((thumbLightShadowColor == null) || (thumbLightShadowColor instanceof UIResource)) {
            thumbLightShadowColor = UIManager.getColor("ScrollBar.thumbShadow");
        }
        if ((trackColor == null) || (trackColor instanceof UIResource)) {
            trackColor = UIManager.getColor("ScrollBar.track");
        }
        if ((trackHighlightColor == null) || (trackHighlightColor instanceof UIResource)) {
            trackHighlightColor = UIManager.getColor("ScrollBar.trackHighlight");
        }
    }

    public static ComponentUI createUI(final JComponent c) {
        return new MetalScrollBarUI();
    }
}
