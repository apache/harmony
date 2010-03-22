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
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SliderUI;

import org.apache.harmony.luni.util.NotImplementedException;
import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <b>Note:</b> <code>serialVersionUID</code> fields are added as
 * a performance optimization only but not as a guarantee of correct
 * deserialization.
 */
public class BasicSliderUI extends SliderUI {

    /**
     * This class isn't used since 1.3 and not implemented here (see
     * HARMONY-4523 for details - the methods doesn't throw new
     * NotImplementedException according to backward compatibility)
     */
    @SuppressWarnings("unused")
    public class ActionScroller extends AbstractAction {
        private static final long serialVersionUID = -3454576988589353120L;

        public ActionScroller(JSlider slider, int dir, boolean block)
                throws NotImplementedException {
            // Not implemented.
        }

        public void actionPerformed(ActionEvent e)
                throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }
    }

    public class ChangeHandler implements ChangeListener {
        public void stateChanged(final ChangeEvent e) {
            calculateThumbLocation();
            setThumbLocation(thumbRect.x, thumbRect.y);
            Rectangle repaintRect = new Rectangle();
            if (slider.getOrientation() == JSlider.HORIZONTAL) {
                repaintRect.setBounds(contentRect.x, trackRect.y, contentRect.width, trackRect.height);
            } else {
                repaintRect.setBounds(trackRect.x, contentRect.y, trackRect.width, contentRect.height);
            }
            slider.repaint(repaintRect);
        }
    }

    public class ComponentHandler extends ComponentAdapter {
        @Override
        public void componentResized(final ComponentEvent e) {
            calculateGeometry();
            slider.repaint();
        }
    }

    public class FocusHandler implements FocusListener {
        public void focusGained(final FocusEvent e) {
            slider.repaint();
        }

        public void focusLost(final FocusEvent e) {
            slider.repaint();
        }
    }

    public class PropertyChangeHandler implements PropertyChangeListener {
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

    public class ScrollListener implements ActionListener {
        private int dir;
        private boolean block;

        public ScrollListener() {
            this(POSITIVE_SCROLL, false);
        }

        public ScrollListener(final int dir, final boolean block) {
            this.dir = dir;
            this.block = block;
        }

        public void setDirection(final int direction) {
            this.dir = direction;
        }

        public void setScrollByBlock(final boolean block) {
            this.block = block;
        }

        public void actionPerformed(final ActionEvent e) {
            if (block) {
                scrollByBlock(dir);
            } else {
                scrollByUnit(dir);
            }
            slider.repaint();
        }
    }

    public class TrackListener extends MouseInputAdapter {
        protected transient int offset;
        protected transient int currentMouseX;
        protected transient int currentMouseY;

        private Timer trackTimer;
        private boolean inThumb;
        private int diffX;
        private int diffY;
        private Point mousePoint = new Point();

        public TrackListener() {
            
            trackTimer = new Timer(150, new ActionListener() {
            
                public void actionPerformed(final ActionEvent e) {
                
                    Point current = new Point(thumbRect.x, thumbRect.y);
                    Point next = new Point(currentMouseX, currentMouseY);
                    int dir = calculateDirection(current, next);
                    //Changed in H-4480
                    if (shouldScroll(dir)) {
                        scrollDueToClickInTrack(dir);
                    }
                    
                }
            });
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            if (trackTimer.isRunning()) {
                trackTimer.stop();
            }
            offset = 0;

            if (slider.getSnapToTicks() && isDragging) {
                isDragging = false;

                int value = getNearestVisibleValue();
                slider.setValue(value);

                if (slider.getOrientation() == JSlider.HORIZONTAL) {
                    setThumbLocation(xPositionForValue(value), thumbRect.y);
                } else {
                    setThumbLocation(thumbRect.x, yPositionForValue(value));
                }
                calculateThumbLocation();
            }

            slider.setValueIsAdjusting(false);
            isDragging = false;
            slider.repaint();
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            if (!slider.isEnabled()) {
                return;
            }
 
            if (!slider.isFocusOwner()) {
                slider.requestFocus();
            }
            currentMouseX = e.getX();
            currentMouseY = e.getY();
            diffX = currentMouseX - thumbRect.x;
            diffY = currentMouseY - thumbRect.y;
            mousePoint = e.getPoint();

            inThumb = thumbRect.contains(currentMouseX, currentMouseY);
            if (!inThumb && SwingUtilities.isLeftMouseButton(e)) {
                Point currentPoint = new Point(thumbRect.x, thumbRect.y);
                scrollDueToClickInTrack(calculateDirection(currentPoint, e.getPoint()));
                trackTimer.start();

                slider.getModel().setValueIsAdjusting(true);
            }
        }

        public boolean shouldScroll(final int direction) {
            // The class has been unused in TrackListener before H4480
            // Now the behaviour has been changed and this method used in timer
            if (slider.getOrientation() == JSlider.HORIZONTAL) {
              
                if (direction == POSITIVE_SCROLL) {
                
                    return mousePoint.x
                            - (thumbRect.x + computeIncrement() + getThumbSize().width) > 1;
                }
                
                if (direction == NEGATIVE_SCROLL) {
                
                    return mousePoint.x - (thumbRect.x - computeIncrement()) < -1;
                }
                
            } else {
               
                if (direction == POSITIVE_SCROLL) {
                
                    return mousePoint.y
                            - (thumbRect.y + computeIncrement() + getThumbSize().height / 2) > 1;
                }
                
                if (direction == NEGATIVE_SCROLL) {
                
                    return mousePoint.y
                            - (thumbRect.y + computeIncrement() + getThumbSize().height / 2) < -1;
                }
            }
            return false;
        }
        
        @Override
        public void mouseDragged(final MouseEvent e) {
            mousePoint = e.getPoint();
            if (inThumb && SwingUtilities.isLeftMouseButton(e)) {
                isDragging = true;
                slider.getModel().setValueIsAdjusting(true);

                Rectangle repaintRect = new Rectangle();
                if (slider.getOrientation() == JSlider.HORIZONTAL) {
                    int newX = e.getX() - diffX;
                    newX = checkXEdgeCondition(newX);
                    setThumbLocation(newX, thumbRect.y);
                    slider.setValue(valueForXPosition(newX + getThumbSize().width / 2));

                    repaintRect.setBounds(contentRect.x, trackRect.y, contentRect.width, trackRect.height);
                    offset = newX - (currentMouseX - diffX);
                } else {
                    int newY = e.getY() - diffY;
                    newY = checkYEdgeCondition(newY);
                    setThumbLocation(thumbRect.x, newY);
                    slider.setValue(valueForYPosition(newY + getThumbSize().height / 2));

                    repaintRect.setBounds(trackRect.x, contentRect.y, trackRect.width, contentRect.height);
                    offset = newY - (currentMouseY - diffY);
                }
                slider.repaint(repaintRect);
            }

            if (trackTimer.isRunning() && trackRect.contains(e.getX(), e.getY())) {
                currentMouseX = e.getX();
                currentMouseY = e.getY();
            }
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
        }

        private int checkXEdgeCondition(final int newX) {
            int result = newX;
            if (!drawInverted() ^ !slider.getComponentOrientation().isLeftToRight()) {
                if (newX + getThumbSize().width / 2 < xPositionForValue(slider.getMinimum())) {
                    result = xPositionForValue(slider.getMinimum()) - getThumbSize().width / 2;
                }
                if (newX + getThumbSize().width / 2 > xPositionForValue(slider.getMaximum())) {
                    result = xPositionForValue(slider.getMaximum()) - getThumbSize().width / 2;
                }
            } else {
                if (newX + getThumbSize().width / 2 > xPositionForValue(slider.getMinimum())) {
                    result = xPositionForValue(slider.getMinimum()) - getThumbSize().width / 2;
                }
                if (newX + getThumbSize().width / 2 < xPositionForValue(slider.getMaximum())) {
                    result = xPositionForValue(slider.getMaximum()) - getThumbSize().width / 2;
                }
            }
            return result;
        }

        private int checkYEdgeCondition(final int newY) {
            int result = newY;
            if (!drawInverted() ^ (!slider.getComponentOrientation().isLeftToRight() && slider.getOrientation() == JSlider.HORIZONTAL)) {
                if (newY + getThumbSize().height / 2 > yPositionForValue(slider.getMinimum())) {
                    result = yPositionForValue(slider.getMinimum()) - getThumbSize().height / 2;
                }
                if (newY + getThumbSize().height / 2 < yPositionForValue(slider.getMaximum())) {
                    result = yPositionForValue(slider.getMaximum()) - getThumbSize().height / 2;
                }
            } else {
                if (newY + getThumbSize().height / 2 < yPositionForValue(slider.getMinimum())) {
                    result = yPositionForValue(slider.getMinimum()) - getThumbSize().height / 2;
                }
                if (newY + getThumbSize().height / 2 > yPositionForValue(slider.getMaximum())) {
                    result = yPositionForValue(slider.getMaximum()) - getThumbSize().height / 2;
                }
            }
            return result;
        }

        private int calculateDirection(final Point current, final Point next) {
            if (slider.getOrientation() == JSlider.HORIZONTAL) {
                if (slider.getComponentOrientation().isLeftToRight()) {
                    return (next.x > current.x) ? 1 : -1;
                } else {
                    return (next.x > current.x) ? -1 : 1;
                }
            } else {
                return (next.y < current.y) ? 1 : -1;
            }
        }

        private int getNearestVisibleValue() {
            int tickSpacing = calculateTickSpacing();
            int value = slider.getValue();
            int result = slider.getMinimum();
            while (result < value) {
                result += tickSpacing;
            }
            result = (result - value < value - (result - tickSpacing)) ? result : result - tickSpacing;

            return result;
        }
    }

    public static final int POSITIVE_SCROLL = 1;
    public static final int NEGATIVE_SCROLL = -1;
    public static final int MIN_SCROLL = -2;
    public static final int MAX_SCROLL = 2;

    protected Insets focusInsets = new Insets(0, 0, 0, 0);
    protected Timer scrollTimer;
    protected JSlider slider;
    protected Insets insetCache;
    protected boolean leftToRightCache;
    protected Rectangle focusRect;
    protected Rectangle contentRect;
    protected Rectangle labelRect;
    protected Rectangle tickRect;
    protected Rectangle trackRect;
    protected Rectangle thumbRect;
    protected int trackBuffer;
    protected TrackListener trackListener;
    protected ChangeListener changeListener;
    protected ComponentListener componentListener;
    protected FocusListener focusListener;
    protected ScrollListener scrollListener;
    protected PropertyChangeListener propertyChangeListener;

    private Color shadowColor;
    private Color highlightColor;
    private Color focusColor;

    private boolean isDragging;

    private ChangeListener changeHandler;
    private ComponentListener componentHandler;
    private FocusListener focusHandler;
    private PropertyChangeListener propertyChangeHandler;

    private static final int THUMB_WIDTH = 11;
    private static final int THUMB_HEIGHT = 20;
    private static final int TRACK_SIZE = 2;
    private static final int DEFAULT_SLIDER_SIZE = 200;
    private static final int DEFAULT_SLIDER_MIN_SIZE = 36;
    private static final int TICK_LENGTH = 8;
    private static final int UNIT_INCREMENT = 1;

    public BasicSliderUI(final JSlider slider) {
        focusRect = new Rectangle();
        contentRect = new Rectangle();
        labelRect = new Rectangle();
        tickRect = new Rectangle();
        trackRect = new Rectangle();
        thumbRect = new Rectangle();
    }

    protected Color getShadowColor() {
        return shadowColor;
    }

    protected Color getHighlightColor() {
        return highlightColor;
    }

    protected Color getFocusColor() {
        return focusColor;
    }

    protected boolean isDragging() {
        return isDragging;
    }

    public static ComponentUI createUI(final JComponent c) {
        return new BasicSliderUI((JSlider)c);
    }

    @Override
    public void installUI(final JComponent c) {
        slider = (JSlider)c;

        installDefaults(slider);
        installListeners(slider);
        installKeyboardActions(slider);

        calculateGeometry();
    }

    @Override
    public void uninstallUI(final JComponent c) {
        if (c != slider) {
            throw new IllegalComponentStateException(Messages.getString("swing.0E",new Object[]{this, c, slider})); //$NON-NLS-1$
        }
        uninstallListeners(slider);
        uninstallKeyboardActions(slider);
    }

    protected void installDefaults(final JSlider slider) {
        LookAndFeel.installColors(slider, "Slider.background", "Slider.foreground");

        shadowColor = UIManager.getColor("Slider.shadow");
        highlightColor = UIManager.getColor("Slider.highlight");
        focusColor = UIManager.getColor("Slider.focus");

        focusInsets = UIManager.getInsets("Slider.focusInsets");
    }

    protected TrackListener createTrackListener(final JSlider slider) {
        return new TrackListener();
    }

    protected ChangeListener createChangeListener(final JSlider slider) {
        if (changeHandler == null) {
            changeHandler = new ChangeHandler();
        }
        return changeHandler;
    }

    protected ComponentListener createComponentListener(final JSlider slider) {
        if (componentHandler == null) {
            componentHandler = new ComponentHandler();
        }
        return componentHandler;
    }

    protected FocusListener createFocusListener(final JSlider slider) {
        if (focusHandler == null) {
            focusHandler = new FocusHandler();
        }
        return focusHandler;
    }

    protected ScrollListener createScrollListener(final JSlider slider) {
        return slider != null
               ? new ScrollListener(slider.getOrientation(), slider.getSnapToTicks())
               : new ScrollListener();
    }

    protected PropertyChangeListener createPropertyChangeListener(final JSlider slider) {
        if (propertyChangeHandler == null) {
            propertyChangeHandler = new PropertyChangeHandler();
        }
        return propertyChangeHandler;
    }

    protected void installListeners(final JSlider slider) {
        changeListener = new ChangeHandler();
        slider.getModel().addChangeListener(changeListener);

        componentListener = createComponentListener(slider);
        slider.addComponentListener(componentListener);

        focusListener = createFocusListener(slider);
        slider.addFocusListener(focusListener);

        propertyChangeListener = createPropertyChangeListener(slider);
        slider.addPropertyChangeListener(propertyChangeListener);

        trackListener = createTrackListener(slider);
        slider.addMouseListener(trackListener);
        slider.addMouseMotionListener(trackListener);

        scrollListener = createScrollListener(slider);
        scrollTimer = new Timer(150, scrollListener);
    }

    protected void uninstallListeners(final JSlider slider) {
        slider.getModel().removeChangeListener(changeListener);
        changeListener = null;

        slider.removeComponentListener(componentListener);
        componentListener = null;

        slider.removeFocusListener(focusListener);
        focusListener = null;

        slider.removePropertyChangeListener(propertyChangeListener);
        propertyChangeListener = null;

        slider.removeMouseListener(trackListener);
        slider.removeMouseMotionListener(trackListener);
        trackListener = null;

        scrollListener = null;
        scrollTimer = null;
    }

    protected void installKeyboardActions(final JSlider slider) {
        Utilities.installKeyboardActions(slider, JComponent.WHEN_FOCUSED, "Slider.focusInputMap", "Slider.focusInputMap.RightToLeft");

        slider.getActionMap().put("positiveUnitIncrement", newPositiveUnitIncrementAction());
        slider.getActionMap().put("positiveBlockIncrement", newPositiveBlockIncrementAction());
        slider.getActionMap().put("negativeUnitIncrement", newNegativeUnitIncrementAction());
        slider.getActionMap().put("negativeBlockIncrement", newNegativeBlockIncrementAction());
        slider.getActionMap().put("minScroll", newMinScrollAction());
        slider.getActionMap().put("maxScroll", newMaxScrollAction());
    }

    protected void uninstallKeyboardActions(final JSlider slider) {
        Utilities.uninstallKeyboardActions(slider, JComponent.WHEN_FOCUSED);
    }

    public Dimension getPreferredHorizontalSize() {
        Insets insets = slider.getInsets();
        int trackHeight = trackRect.height > 0 ? trackRect.height : 0;
        int tickHeight = tickRect.height > 0 ? tickRect.height : 0;
        int labelHeight = labelRect.height > 0 ? labelRect.height : 0;
        int height = trackHeight + tickHeight + labelHeight + insets.top + insets.bottom + focusInsets.top + focusInsets.bottom;
        return new Dimension(DEFAULT_SLIDER_SIZE, height);
    }

    public Dimension getPreferredVerticalSize() {
        Insets insets = slider.getInsets();
        int trackWidth = trackRect.width > 0 ? trackRect.width : 0;
        int tickWidth = tickRect.width > 0 ? tickRect.width : 0;
        int labelWidth = labelRect.width > 0 ? labelRect.width : 0;
        int width = trackWidth + tickWidth + labelWidth + insets.left + insets.right + focusInsets.left + focusInsets.right;
        return new Dimension(width, DEFAULT_SLIDER_SIZE);
    }

    public Dimension getMinimumHorizontalSize() {
        Insets insets = slider.getInsets();
        int trackHeight = trackRect.height > 0 ? trackRect.height : 0;
        int tickHeight = tickRect.height > 0 ? tickRect.height : 0;
        int labelHeight = labelRect.height > 0 ? labelRect.height : 0;
        int height = trackHeight + tickHeight + labelHeight + insets.top + insets.bottom + focusInsets.top + focusInsets.bottom;
        return new Dimension(DEFAULT_SLIDER_MIN_SIZE, height);
    }

    public Dimension getMinimumVerticalSize() {
        Insets insets = slider.getInsets();
        int trackWidth = trackRect.width > 0 ? trackRect.width : 0;
        int tickWidth = tickRect.width > 0 ? tickRect.width : 0;
        int labelWidth = labelRect.width > 0 ? labelRect.width : 0;
        int width = trackWidth + tickWidth + labelWidth + insets.left + insets.right + focusInsets.left + focusInsets.right;
        return new Dimension(width, DEFAULT_SLIDER_MIN_SIZE);
    }

    @Override
    public Dimension getPreferredSize(final JComponent c) {
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            return getPreferredHorizontalSize();
        } else {
            return getPreferredVerticalSize();
        }
    }

    @Override
    public Dimension getMinimumSize(final JComponent c) {
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            return getMinimumHorizontalSize();
        } else {
            return getMinimumVerticalSize();
        }
    }

    @Override
    public Dimension getMaximumSize(final JComponent c) {
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            return new Dimension(Short.MAX_VALUE, getPreferredHorizontalSize().height);
        } else {
            return new Dimension(getPreferredHorizontalSize().width, Short.MAX_VALUE);
        }
    }

    protected void calculateGeometry() {
        calculateFocusRect();
        calculateContentRect();
        calculateThumbSize();
        calculateTrackBuffer();
        calculateTrackRect();
        calculateTickRect();
        calculateLabelRect();
        calculateThumbLocation();
    }

    protected void calculateFocusRect() {
        Insets insets = slider.getInsets();
        int x = insets.left;
        int y = insets.top;
        int width = slider.getWidth() - insets.left - insets.right;
        int height = slider.getHeight() - insets.top - insets.bottom;

        focusRect.setBounds(x, y, width, height);
    }

    protected void calculateThumbSize() {
        int width = (slider.getOrientation() == JSlider.HORIZONTAL) ? THUMB_WIDTH : THUMB_HEIGHT;
        int height = (slider.getOrientation() == JSlider.HORIZONTAL) ? THUMB_HEIGHT : THUMB_WIDTH;
        thumbRect.setSize(width, height);
    }

    protected void calculateContentRect() {
        int x = focusRect.x + focusInsets.left;
        int y = focusRect.y + focusInsets.top;
        int width = focusRect.width - focusInsets.left - focusInsets.right;
        int height = focusRect.height - focusInsets.top - focusInsets.bottom;

        contentRect.setBounds(x, y, width, height);
    }

    protected void calculateThumbLocation() {
        if (isDragging) {
            return;
        }
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            int x = xPositionForValue(slider.getValue());
            int y = trackRect.y;
            x -= getThumbSize().width / 2;
            thumbRect.setLocation(x, y);
        } else {
            int x = trackRect.x;
            int y = yPositionForValue(slider.getValue());
            y -= getThumbSize().height / 2;
            thumbRect.setLocation(x, y);
        }
    }

    protected void calculateTrackBuffer() {
        if (slider.getPaintLabels()) {
            if ((slider.getOrientation() == JSlider.HORIZONTAL)) {
                int widthOfHighValueLabel = getWidthOfHighValueLabel();
                int widthOfLowValueLabel = getWidthOfLowValueLabel();
                trackBuffer = widthOfHighValueLabel > widthOfLowValueLabel 
                                                ? widthOfHighValueLabel / 2
                                                : widthOfLowValueLabel / 2;
            } else {
                int heightOfHighValueLabel = getHeightOfHighValueLabel();
                int heightOfLowValueLabel = getHeightOfLowValueLabel();
                trackBuffer = heightOfHighValueLabel > heightOfLowValueLabel
                                                ? heightOfHighValueLabel / 2
                                                : heightOfLowValueLabel / 2;
            }
        } else {
            trackBuffer = 0;
        }
    }

    protected void calculateTrackRect() {

        if (slider.getOrientation() == JSlider.HORIZONTAL) {            
            int width = contentRect.width - trackBuffer * 2
                          - getThumbSize().width;
            int x = contentRect.x + trackBuffer + getThumbSize().width / 2;
            int y = contentRect.y + (contentRect.height - getThumbSize().height
                          + labelRect.height + tickRect.height) / 2;
            
            trackRect.setBounds(x, y, width, getThumbSize().height);
            
        } else {            
            int height = contentRect.height - trackBuffer * 2
                          - getThumbSize().height;
            int sizeToCenter = (getThumbSize().width + labelRect.width + tickRect.width) / 2;
            int y = contentRect.y + trackBuffer + getThumbSize().height / 2;
            int x;
            
            if (slider.getComponentOrientation().isLeftToRight()) {
                x = contentRect.x + contentRect.width / 2 - sizeToCenter;
                
            } else {
                x = contentRect.x + contentRect.width / 2 + sizeToCenter
                          - getThumbSize().width;
            }
            
            trackRect.setBounds(x, y, getThumbSize().width, height);
        }
    }

    protected int getTickLength() {
        return slider.getPaintTicks() ? TICK_LENGTH : 0;
    }

    protected void calculateTickRect() {
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            int x = trackRect.x;
            int y = trackRect.y + trackRect.height;
            int width = trackRect.width;
            int height = getTickLength();
            tickRect.setBounds(x, y, width, height);
        } else {
            int x;
            if (slider.getComponentOrientation().isLeftToRight()) {
                x = trackRect.x + trackRect.width;
            } else {
                x = trackRect.x - getTickLength();
            }
            int y = trackRect.y;
            int width = getTickLength();
            int height = trackRect.height;
            tickRect.setBounds(x, y, width, height);
        }
    }

    protected void calculateLabelRect() {
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            int x = contentRect.x;
            int y = tickRect.y + tickRect.height;
            int width = contentRect.width;
            int height = slider.getPaintLabels() ? getHeightOfTallestLabel() : 0;
            labelRect.setBounds(x, y, width, height);
        } else {
            int x;
            int width = slider.getPaintLabels() ? getWidthOfWidestLabel() : 0;
            int height = contentRect.height;
            if (slider.getComponentOrientation().isLeftToRight()) {
                x = tickRect.x + tickRect.width;
            } else {
                x = tickRect.x - width;
            }
            int y = contentRect.y;
            labelRect.setBounds(x, y, width, height);
        }
    }

    protected Dimension getThumbSize() {
        return thumbRect.getSize();
    }

    protected int getWidthOfWidestLabel() {
        Dictionary table = slider.getLabelTable();
        if (table == null) {
            return 0;
        }
        Enumeration keys = table.keys();
        int result = ((Component)table.get(keys.nextElement())).getWidth();
        while (keys.hasMoreElements()) {
            Component label = ((Component)table.get(keys.nextElement()));
            if (label.getWidth() > result) {
                result = label.getWidth();
            }
        }

        return result;
    }

    protected int getHeightOfTallestLabel() {
        Dictionary table = slider.getLabelTable();
        if (table == null) {
            return 0;
        }
        Enumeration keys = table.keys();
        int result = ((Component)table.get(keys.nextElement())).getHeight();
        while (keys.hasMoreElements()) {
            Component label = ((Component)table.get(keys.nextElement()));
            if (label.getHeight() > result) {
                result = label.getHeight();
            }
        }

        return result;
    }

    protected int getWidthOfHighValueLabel() {
        Component label = getHighestValueLabel();

        return label == null? 0: label.getWidth();
    }

    protected int getWidthOfLowValueLabel() {        
        Component label = getLowestValueLabel();

        return label == null? 0: label.getWidth();
    }

    protected int getHeightOfHighValueLabel() {
        Component label = getHighestValueLabel();

        return label == null? 0: label.getHeight();
    }

    protected int getHeightOfLowValueLabel() {        
        Component label = getLowestValueLabel();

        return label == null? 0: label.getHeight();
    }

    protected Component getLowestValueLabel() {
        Dictionary table = slider.getLabelTable();
        if (table == null) {
            return null;
        }
        Enumeration keys = table.keys();
        Integer lastKey = (Integer)keys.nextElement();
        Component result = (Component)table.get(lastKey);
        while (keys.hasMoreElements()) {
            Integer el = (Integer)keys.nextElement();
            if (el.intValue() < lastKey.intValue()) {
                lastKey = el;
                result = (Component)table.get(lastKey);
            }
        }

        return result;
    }

    protected Component getHighestValueLabel() {
        Dictionary table = slider.getLabelTable();
        if (table == null) {
            return null;
        }
        Enumeration keys = table.keys();
        Integer lastKey = (Integer)keys.nextElement();
        Component result = (Component)table.get(lastKey);
        while (keys.hasMoreElements()) {
            Integer el = (Integer)keys.nextElement();
            if (el.intValue() > lastKey.intValue()) {
                lastKey = el;
                result = (Component)table.get(lastKey);
            }
        }

        return result;
    }

    @Override
    public void paint(final Graphics g, final JComponent c) {
        Color oldColor = g.getColor();

        g.setColor(slider.getBackground());
        g.fillRect(0, 0, slider.getWidth(), slider.getHeight());

        if (slider.isFocusOwner()) {
            paintFocus(g);
        }
        if (slider.getPaintTrack()) {
            paintTrack(g);
        }
        if (slider.getPaintTicks()) {
            paintTicks(g);
        }
        if (slider.getPaintLabels()) {
            paintLabels(g);
        }
        paintThumb(g);

        g.setColor(oldColor);
    }

    protected void recalculateIfInsetsChanged() {
        calculateGeometry();
    }

    protected void recalculateIfOrientationChanged() {
        calculateGeometry();
        uninstallKeyboardActions(slider);
        installKeyboardActions(slider);
    }

    protected boolean drawInverted() {
        return slider.getInverted();
    }

    public void paintFocus(final Graphics g) {
        Color oldColor = g.getColor();
        g.setColor(focusColor);
        g.drawRect(focusRect.x, focusRect.y, focusRect.width - 1, focusRect.height - 1);
        g.setColor(oldColor);
    }

    public void paintTrack(final Graphics g) {
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            Utilities.draw3DRect(g, trackRect.x, trackRect.y  + (trackRect.height - TRACK_SIZE) / 2,
                    trackRect.width, TRACK_SIZE, Color.DARK_GRAY, Color.WHITE, false);
        } else {
            Utilities.draw3DRect(g, trackRect.x + (trackRect.width - TRACK_SIZE) / 2, trackRect.y,
                    TRACK_SIZE, trackRect.height, Color.DARK_GRAY, Color.WHITE, false);
        }
    }

    public void paintTicks(final Graphics g) {
        Color oldColor = g.getColor();
        g.setColor(Color.BLACK);
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            if (slider.getMajorTickSpacing() != 0) {
                int value = slider.getMinimum();
                while (value <= slider.getMaximum()) {
                    paintMajorTickForHorizSlider(g, tickRect, xPositionForValue(value));
                    value += slider.getMajorTickSpacing();
                }
            }
            if (slider.getMinorTickSpacing() != 0) {
                int value = slider.getMinimum();
                while (value <= slider.getMaximum()) {
                    paintMinorTickForHorizSlider(g, tickRect, xPositionForValue(value));
                    value += slider.getMinorTickSpacing();
                }
            }
        } else {
            if (slider.getMajorTickSpacing() != 0) {
                int value = slider.getMinimum();
                while (value <= slider.getMaximum()) {
                    paintMajorTickForVertSlider(g, tickRect, yPositionForValue(value));
                    value += slider.getMajorTickSpacing();
                }
            }
            if (slider.getMinorTickSpacing() != 0) {
                int value = slider.getMinimum();
                while (value <= slider.getMaximum()) {
                    paintMinorTickForVertSlider(g, tickRect, yPositionForValue(value));
                    value += slider.getMinorTickSpacing();
                }
            }
        }
        g.setColor(oldColor);
    }

    protected void paintMinorTickForHorizSlider(final Graphics g, final Rectangle tickBounds, final int x) {
        g.drawLine(x, tickBounds.y, x, tickBounds.y + tickBounds.height / 2);
    }

    protected void paintMajorTickForHorizSlider(final Graphics g, final Rectangle tickBounds, final int x) {
        g.drawLine(x, tickBounds.y, x, tickBounds.y + tickBounds.height);
    }

    protected void paintMinorTickForVertSlider(final Graphics g, final Rectangle tickBounds, final int y) {
        int diff = slider.getComponentOrientation().isLeftToRight() ? 0 : tickBounds.width / 2;
        g.drawLine(tickBounds.x + diff, y, tickBounds.x + tickBounds.width / 2 + diff, y);
    }

    protected void paintMajorTickForVertSlider(final Graphics g, final Rectangle tickBounds, final int y) {
        g.drawLine(tickBounds.x, y, tickBounds.x + tickBounds.width, y);
    }

    public void paintLabels(final Graphics g) {
        if (slider.getMajorTickSpacing() == 0) {
            return;
        }
        Dictionary labelTable = slider.getLabelTable();
        if (labelTable == null) {
            return;
        }
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            int value = slider.getMinimum();
            while (value <= slider.getMaximum()) {
                Component label = (Component)labelTable.get(new Integer(value));
                if (label != null) {
                    paintHorizontalLabel(g, value, label);
                }
                value += slider.getMajorTickSpacing();
            }
        } else {
            int value = slider.getMinimum();
            while (value <= slider.getMaximum()) {
                Component label = (Component)labelTable.get(new Integer(value));
                if (label != null) {
                    paintVerticalLabel(g, value, label);
                }
                value += slider.getMajorTickSpacing();
            }
        }
    }

    protected void paintHorizontalLabel(final Graphics g, final int value, final Component label) {
        int x = xPositionForValue(value) - label.getWidth() / 2;;
        int y = labelRect.y;
        g.translate(x, y);

        label.paint(g);

        g.translate(-x, -y);
    }

    protected void paintVerticalLabel(final Graphics g, final int value, final Component label) {
        int x;
        if (slider.getComponentOrientation().isLeftToRight()) {
            x = labelRect.x;
        } else {
            x = labelRect.x + labelRect.width - label.getWidth();
        }
        int y = yPositionForValue(value) - label.getHeight() / 2;
        g.translate(x, y);

        label.paint(g);

        g.translate(-x, -y);
    }

    public void paintThumb(final Graphics g) {
        if (slider.getPaintTicks()) {
            Color oldColor = g.getColor();
            g.setColor(slider.getBackground());
            g.fillRect(thumbRect.x, thumbRect.y, getThumbSize().width, getThumbSize().height);
            g.setColor(oldColor);
            paintThumbWithPointer(g);
        } else {
            Color oldColor = g.getColor();
            g.setColor(slider.getBackground());
            g.fillRect(thumbRect.x, thumbRect.y, getThumbSize().width, getThumbSize().height);
            g.setColor(oldColor);
            g.fillRect(thumbRect.x, thumbRect.y, getThumbSize().width, getThumbSize().height);
            Utilities.draw3DRect(g, thumbRect.x, thumbRect.y, getThumbSize().width, getThumbSize().height,
                                 Color.GRAY, Color.WHITE, true);
        }
    }

    public void setThumbLocation(final int x, final int y) {
        thumbRect.setLocation(x, y);
    }

    int computeIncrement() {
        
        if (slider.getMajorTickSpacing() != 0) {
            
            return slider.getMajorTickSpacing();
            
        } else {
            
            int increment = (slider.getMaximum() - slider.getMinimum()) / 10;
            if (increment <= 0) {
                increment = 1;
            }
            return increment;
        }
    }

    public void scrollByBlock(final int direction) {
        //Changed in H-4480
        scrollByIncrement(direction, computeIncrement());
    }

    public void scrollByUnit(final int direction) {
        scrollByIncrement(direction, UNIT_INCREMENT);
    }

    protected void scrollDueToClickInTrack(final int dir) {
        scrollByBlock(dir);
    }

    protected int xPositionForValue(final int value) {
        // Changed according to JIRA 4445
        double valueToSizeRatio = (double) (value - slider.getMinimum())
                / (double) (slider.getMaximum() - slider.getMinimum());

        if ((drawInverted() ^ !slider.getComponentOrientation().isLeftToRight())) {
            return (int) (trackRect.x + trackRect.width - (trackRect.width * valueToSizeRatio));
        } else {
            return (int) (trackRect.x + trackRect.width * valueToSizeRatio);
        }

    }

    protected int yPositionForValue(final int value) {
        // Changed according to JIRA 4445
        double valueToSizeRatio = (double) (value - slider.getMinimum())
                / (double) (slider.getMaximum() - slider.getMinimum());

        if ((drawInverted() ^ (!slider.getComponentOrientation()
                .isLeftToRight() && slider.getOrientation() == JSlider.HORIZONTAL))) {
            return (int) (trackRect.y + trackRect.height * valueToSizeRatio);
        } else {
            return (int) (trackRect.y + trackRect.height - (trackRect.height * valueToSizeRatio));
        }
    }

    public int valueForYPosition(final int yPos) {
        int size = slider.getMaximum() - slider.getMinimum();
        int intervalSize = trackRect.height / size;

        int result = drawInverted() ^ (!slider.getComponentOrientation().isLeftToRight() && slider.getOrientation() == JSlider.HORIZONTAL)
                        ? slider.getMinimum() + (yPos - trackRect.y + intervalSize / 2) * size / trackRect.height
                        : slider.getMinimum() + (trackRect.y + trackRect.height - yPos + intervalSize / 2) * size / trackRect.height;
        result = (result < slider.getMinimum()) ? slider.getMinimum() : result;
        result = (result > slider.getMaximum()) ? slider.getMaximum() : result;

        return result;
    }

    public int valueForXPosition(final int xPos)  {
        int size = slider.getMaximum() - slider.getMinimum();
        int intervalSize = trackRect.width / size;

        int result = drawInverted() ^ !slider.getComponentOrientation().isLeftToRight()
                        ? slider.getMinimum() + (trackRect.x + trackRect.width - xPos + intervalSize / 2) * size / trackRect.width
                        : slider.getMinimum() + (xPos - trackRect.x + intervalSize / 2) * size / trackRect.width;
        result = (result < slider.getMinimum()) ? slider.getMinimum() : result;
        result = (result > slider.getMaximum()) ? slider.getMaximum() : result;

        return result;
    }

    private Action newMaxScrollAction() {
        return new AbstractAction() {
            private static final long serialVersionUID = -3822301141065864044L;

            public void actionPerformed(final ActionEvent e) {
                if (drawInverted()) {
                    slider.setValue(slider.getMinimum());
                } else {
                    slider.setValue(slider.getMaximum());
                }
                slider.repaint();
            }
        };
    }

    private Action newMinScrollAction() {
        return new AbstractAction() {
            private static final long serialVersionUID = 703565386507416752L;

            public void actionPerformed(final ActionEvent e) {
                if (drawInverted()) {
                    slider.setValue(slider.getMaximum());
                } else {
                    slider.setValue(slider.getMinimum());
                }
                slider.repaint();
            }
        };
    }

    private Action newNegativeBlockIncrementAction() {
        return new AbstractAction() {
            private static final long serialVersionUID = 7818668169396841520L;

            public void actionPerformed(final ActionEvent e) {
                scrollByBlock(NEGATIVE_SCROLL);
            }
        };
    }

    private Action newNegativeUnitIncrementAction() {
        return new AbstractAction() {
            private static final long serialVersionUID = -4366059737366026435L;

            public void actionPerformed(final ActionEvent e) {
                scrollByUnit(NEGATIVE_SCROLL);
            }
        };
    }

    private Action newPositiveBlockIncrementAction() {
        return new AbstractAction() {
            private static final long serialVersionUID = -5999323396935662487L;

            public void actionPerformed(final ActionEvent e) {
                scrollByBlock(POSITIVE_SCROLL);
            }
        };
    }

    private Action newPositiveUnitIncrementAction() {
        return new AbstractAction() {
            private static final long serialVersionUID = 5166413389559469128L;

            public void actionPerformed(final ActionEvent e) {
                scrollByUnit(POSITIVE_SCROLL);
            }
        };
    }

    private void scrollByIncrement(final int direction, final int increment) {
        int dir = direction;
        if (drawInverted()) {
            dir = dir * (-1);
        }
        if (dir >= POSITIVE_SCROLL) {
            slider.setValue(slider.getValue() + increment);
        } else {
            slider.setValue(slider.getValue() - increment);
        }
    }

    private void paintThumbWithPointer(final Graphics g) {
        int x = thumbRect.x;
        int y = thumbRect.y;
        Color shadow = Color.gray;
        Color highlight = Color.white;

        final Color oldColor = g.getColor();
        final Color topLeft = highlight;
        final Color bottomRight = shadow;
        final int bottom = y + getThumbSize().height - 1;
        final int right = x + getThumbSize().width - 1;
        g.setColor(topLeft);
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            g.drawLine(x, y, right, y);
            g.drawLine(right, y, right, bottom - getThumbSize().height / 4);
            g.drawLine(right, bottom - getThumbSize().height / 4, x + getThumbSize().width / 2, bottom);
        } else {
            g.drawLine(x, y, x, bottom);
            g.drawLine(x, y, right - getThumbSize().width / 4, y);
            g.drawLine(right - getThumbSize().width / 4, y, right, y + getThumbSize().height / 2);
        }
        g.setColor(bottomRight);
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            g.drawLine(x, y, x, bottom - getThumbSize().height / 4);
            g.drawLine(x, bottom - getThumbSize().height / 4, x + getThumbSize().width / 2, bottom);
        } else {
            g.drawLine(x, bottom, right - getThumbSize().width / 4, bottom);
            g.drawLine(right - getThumbSize().width / 4, bottom, right, y + getThumbSize().height / 2);
        }
        g.setColor(oldColor);
    }

    private int calculateTickSpacing() {
        if (slider.getMinorTickSpacing() != 0 && slider.getMajorTickSpacing() != 0) {
            return slider.getMinorTickSpacing();
        } else if (slider.getMajorTickSpacing() != 0) {
            return slider.getMajorTickSpacing();
        } else {
            return 1;
        }
    }
}
