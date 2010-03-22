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

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSliderUI;

import org.apache.harmony.x.swing.StringConstants;


public class MetalSliderUI extends BasicSliderUI {
    protected class MetalPropertyListener extends PropertyChangeHandler {
        public void propertyChange(final PropertyChangeEvent e) {
            String changedProperty = e.getPropertyName();
            if (StringConstants.COMPONENT_ORIENTATION.equals(changedProperty)) {
                recalculateIfOrientationChanged();
            } else if (StringConstants.BORDER_PROPERTY_CHANGED.equals(changedProperty)) {
                recalculateIfInsetsChanged();
            }
            calculateGeometry();
            slider.revalidate();
            slider.repaint();
        }
    }

    protected final int TICK_BUFFER = 4;
    protected final String SLIDER_FILL = "JSlider.isFilled";
    protected boolean filledSlider;
    protected static Color thumbColor;
    protected static Color highlightColor;
    protected static Color darkShadowColor;
    protected static int trackWidth;
    protected static int tickLength;
    protected static Icon horizThumbIcon;
    protected static Icon vertThumbIcon;

    public MetalSliderUI() {
        super(null);
    }

    public static ComponentUI createUI(final JComponent c) {
        return new MetalSliderUI();
    }

    public void installUI(final JComponent c) {
        slider = (JSlider)c;
        horizThumbIcon = UIManager.getIcon("Slider.horizontalThumbIcon");
        vertThumbIcon = UIManager.getIcon("Slider.verticalThumbIcon");
        thumbRect.setSize(getThumbSize());

        super.installUI(c);

        trackWidth = UIManager.getInt("Slider.trackWidth");
        tickLength = UIManager.getInt("Slider.majorTickLength");

        thumbColor = UIManager.getColor("Slider.foreground");
        highlightColor = UIManager.getColor("Slider.highlight");
        darkShadowColor = UIManager.getColor("Slider.shadow");
    }

    protected PropertyChangeListener createPropertyChangeListener(final JSlider slider) {
        return new MetalPropertyListener();
    }

    public void paintThumb(final Graphics g) {
        Color oldColor = g.getColor();
        if (slider.isFocusOwner()) {
            g.setColor(thumbColor);
        } else {
            g.setColor(Color.LIGHT_GRAY);
        }
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            horizThumbIcon.paintIcon(slider, g, thumbRect.x, thumbRect.y);
        } else {
            vertThumbIcon.paintIcon(slider, g, thumbRect.x, thumbRect.y);
        }
        Color shadow = Color.GRAY;
        Color highlight = Color.WHITE;
        Rectangle thumbBounds;
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            thumbBounds = new Rectangle(thumbRect.x + 1, thumbRect.y, thumbRect.width, thumbRect.height / 2);
        } else {
            thumbBounds = new Rectangle(thumbRect.x, thumbRect.y + 1, thumbRect.width / 2, thumbRect.height);
        }
        MetalBumps.paintBumps(g, thumbBounds, shadow, highlight);
        g.setColor(oldColor);
    }

    public void paintTrack(final Graphics g) {
        Color oldColor = g.getColor();
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            int x = trackRect.x;
            int y = thumbRect.y  + getThumbSize().height - getThumbOverhang() - trackWidth;
            int width = getTrackLength();
            int height = trackWidth;
            paintTrack(g, x, y, width, height);
        } else {
            int x = thumbRect.x  + getThumbSize().width - getThumbOverhang() - trackWidth;
            int y = trackRect.y;
            int width = trackWidth;
            int height = getTrackLength();
            paintTrack(g, x, y, width, height);
        }
        g.setColor(oldColor);
    }

    public void paintFocus(final Graphics g) {
    }

    protected Dimension getThumbSize() {
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            return new Dimension(horizThumbIcon.getIconWidth(), horizThumbIcon.getIconHeight());
        } else {
            return new Dimension(vertThumbIcon.getIconWidth(), vertThumbIcon.getIconHeight());
        }
    }

    public int getTickLength() {
        return slider.getPaintTicks() ? tickLength : 0;
    }

    protected int getTrackWidth() {
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            return trackRect.height;
        } else {
            return trackRect.width;
        }
    }

    protected int getTrackLength() {
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            return trackRect.width;
        } else {
            return trackRect.height;
        }
    }

    protected int getThumbOverhang() {
        return 4;
    }

    protected void scrollDueToClickInTrack(final int dir) {
        scrollByUnit(dir);
    }

    protected void paintMinorTickForHorizSlider(final Graphics g, final Rectangle tickBounds, final int x) {
        g.setColor(slider.getForeground());
        super.paintMinorTickForHorizSlider(g, tickBounds, x);
    }

    protected void paintMajorTickForHorizSlider(final Graphics g, final Rectangle tickBounds, final int x) {
        g.setColor(slider.getForeground());
        super.paintMajorTickForHorizSlider(g, tickBounds, x);
    }

    protected void paintMinorTickForVertSlider(final Graphics g, final Rectangle tickBounds, final int y) {
        g.setColor(slider.getForeground());
        super.paintMinorTickForVertSlider(g, tickBounds, y);
    }

    protected void paintMajorTickForVertSlider(final Graphics g, final Rectangle tickBounds, final int y) {
        g.setColor(slider.getForeground());
        super.paintMajorTickForVertSlider(g, tickBounds, y);
    }

    private void paintTrack(final Graphics g, final int x, final int y, final int width, final int height) {
        g.setColor(getShadowColor().brighter());
        g.drawRect(x + 1, y + 1, width, height);
        g.setColor(darkShadowColor);
        g.drawRect(x, y, width, height);
        g.setColor(highlightColor);
        g.drawLine(x + 1, y + 1 + height, x + 1 + width, y + 1 + height);
        g.drawLine(x + 1 + width, y + 1, x + 1 + width, y + 1 + height);
    }
}
