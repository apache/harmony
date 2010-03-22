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

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ScrollBarUI;
import javax.swing.plaf.UIResource;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;


public class BasicScrollBarUI extends ScrollBarUI implements LayoutManager, SwingConstants {

    protected class TrackListener extends MouseAdapter implements MouseMotionListener {
        protected transient int offset;
        protected transient int currentMouseX;
        protected transient int currentMouseY;

        private Timer trackTimer;
        private boolean inThumb;
        private int currentModelValue;

        protected TrackListener() {
            trackTimer = new Timer(150, new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    Point current = new Point(getThumbBounds().x, getThumbBounds().y);
                    Point next = new Point(currentMouseX, currentMouseY);
                    int dir = orientationStrategy.calculateDirection(current, next);
                    if (dir > 0) {
                        trackHighlight = INCREASE_HIGHLIGHT;
                    } else {
                        trackHighlight = DECREASE_HIGHLIGHT;
                    }

                    if (!getThumbBounds().contains(currentMouseX, currentMouseY)) {
                        scrollByBlock(dir);
                    }
                }
            });

            trackTimer.setInitialDelay(350);
        }

        public void mouseReleased(final MouseEvent e) {
            if (trackTimer.isRunning()) {
                trackTimer.stop();
            }
            trackHighlight = NO_HIGHLIGHT;
            scrollbar.getModel().setValueIsAdjusting(false);
            scrollbar.repaint();
        }

        public void mousePressed(final MouseEvent e) {
            currentMouseX = e.getX();
            currentMouseY = e.getY();

            currentModelValue = scrollbar.getValue();

            inThumb = getThumbBounds().contains(currentMouseX, currentMouseY);
            if (!inThumb && trackRect.contains(currentMouseX, currentMouseY)
                && SwingUtilities.isLeftMouseButton(e)) {

                Point currentPoint = new Point(getThumbBounds().x, getThumbBounds().y);
                int dir = orientationStrategy.calculateDirection(currentPoint, e.getPoint());

                if (dir > 0) {
                    trackHighlight = INCREASE_HIGHLIGHT;
                } else {
                    trackHighlight = DECREASE_HIGHLIGHT;
                }
                scrollByBlock(dir);
                trackTimer.start();
            }
        }

        public void mouseDragged(final MouseEvent e) {
            if (inThumb && SwingUtilities.isLeftMouseButton(e)) {
                scrollbar.getModel().setValueIsAdjusting(true);
                orientationStrategy.setValueOnDragging(e, currentMouseX, currentMouseY, currentModelValue);
                scrollbar.repaint();
            }

            if (trackTimer.isRunning() && getTrackBounds().contains(e.getX(), e.getY())) {
                currentMouseX = e.getX();
                currentMouseY = e.getY();
            }
        }

        public void mouseMoved(final MouseEvent e) {
        }

        public void mouseExited(final MouseEvent e) {
        }
    }

    protected class ScrollListener implements ActionListener {
        private int dir;
        private boolean block;

        public ScrollListener() {
            this(1, false);
        }

        public ScrollListener(final int dir, final boolean block) {
            this.dir = dir;
            this.block = block;
        }

        public void setDirection(final int dir) {
            this.dir = dir;
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
            scrollbar.repaint();
        }
    }

    protected class ModelListener implements ChangeListener {
        public void stateChanged(final ChangeEvent e) {
            scrollbar.revalidate();
            scrollbar.repaint();
        }
    }

    protected class ArrowButtonListener extends MouseAdapter {
        private int direction;

        public void mousePressed(final MouseEvent e) {
            if (scrollbar.getOrientation() == Adjustable.HORIZONTAL && !scrollbar.getComponentOrientation().isLeftToRight()) {
                direction = (e.getSource() == incrButton) ? -1 : 1;
            } else {
                direction = (e.getSource() == incrButton) ? 1 : - 1;
            }
            scrollListener.setDirection(direction);
            scrollByUnit(direction);
            scrollTimer.start();
        }

        public void mouseReleased(final MouseEvent e) {
            scrollTimer.stop();
        }
    }

    public class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if (StringConstants.COMPONENT_ORIENTATION.equals(propertyName)) {
                uninstallKeyboardActions();
                installKeyboardActions();
            } else if (StringConstants.MODEL_PROPERTY_CHANGED.equals(propertyName)) {
                BoundedRangeModel oldValue = (BoundedRangeModel)e.getOldValue();
                if (oldValue != null) {
                    oldValue.removeChangeListener(modelListener);
                }

                BoundedRangeModel newValue = (BoundedRangeModel)e.getNewValue();
                if (newValue != null) {
                    newValue.addChangeListener(modelListener);
                }
            }

	    if (scrollbar != null) {	
                scrollbar.revalidate();
                scrollbar.repaint();
            }
        }
    }

    protected static final int DECREASE_HIGHLIGHT = 1;
    protected static final int INCREASE_HIGHLIGHT = 2;
    protected static final int NO_HIGHLIGHT = 0;

    protected PropertyChangeListener propertyChangeListener;
    protected ArrowButtonListener buttonListener;
    protected ModelListener modelListener;
    protected ScrollListener scrollListener;
    protected TrackListener trackListener;

    protected JButton decrButton;
    protected JButton incrButton;

    protected boolean isDragging;

    protected Dimension maximumThumbSize;
    protected Dimension minimumThumbSize;

    protected JScrollBar scrollbar;
    protected Timer scrollTimer;
    protected Color trackHighlightColor;
    protected Color trackColor;
    protected Color thumbColor;
    protected Color thumbDarkShadowColor;
    protected Color thumbHighlightColor;
    protected Color thumbLightShadowColor;
    protected Rectangle thumbRect;
    protected int trackHighlight = NO_HIGHLIGHT;
    protected Rectangle trackRect;

    private int defaultButtonSize;
    private OrientationStrategy orientationStrategy;

    public static ComponentUI createUI(final JComponent c) {
        return new BasicScrollBarUI();
    }

    public void addLayoutComponent(final String s, final Component c) {
    }

    public void removeLayoutComponent(final Component c) {
    }

    public Dimension minimumLayoutSize(final Container c) {
        return getMinimumSize(scrollbar);
    }

    protected void configureScrollBarColors() {
        if (scrollbar == null)
            throw new NullPointerException();
            
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

    protected ArrowButtonListener createArrowButtonListener() {
        return new ArrowButtonListener();
    }

    protected JButton createDecreaseButton(final int orient) {
        return new BasicArrowButton(orient);
    }

    protected JButton createIncreaseButton(final int orient) {
        return new BasicArrowButton(orient);
    }

    protected ModelListener createModelListener() {
        return new ModelListener();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    protected BasicScrollBarUI.ScrollListener createScrollListener() {
        return new ScrollListener();
    }

    protected BasicScrollBarUI.TrackListener createTrackListener() {
        return new TrackListener();
    }

    public Dimension getMaximumSize(final JComponent c) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    protected Dimension getMaximumThumbSize() {
        return maximumThumbSize;
    }

    public Dimension getMinimumSize(final JComponent c) {
        return orientationStrategy.getPreferredSize(c);
    }

    protected Dimension getMinimumThumbSize() {
        return minimumThumbSize;
    }

    public Dimension getPreferredSize(final JComponent c) {
        return orientationStrategy.getPreferredSize(c);
    }

    protected Rectangle getThumbBounds() {
        return thumbRect;
    }

    protected Rectangle getTrackBounds() {
        return trackRect;
    }

    protected void installComponents() {
        incrButton = orientationStrategy.createIncreaseButton();
        scrollbar.add(incrButton);

        decrButton = orientationStrategy.createDecreaseButton();
        scrollbar.add(decrButton);
    }

    protected void installDefaults() {
        thumbRect = new Rectangle();
        trackRect = new Rectangle();
        LookAndFeel.installColors(scrollbar, "ScrollBar.background", "ScrollBar.foreground");
        LookAndFeel.installProperty(scrollbar, "opaque", Boolean.TRUE);
        configureScrollBarColors();

        defaultButtonSize = UIManager.getInt("ScrollBar.width");
        if ((maximumThumbSize == null) || (maximumThumbSize instanceof UIResource)) {
            maximumThumbSize = UIManager.getDimension("ScrollBar.maximumThumbSize");
        }
        if ((minimumThumbSize == null) || (minimumThumbSize instanceof UIResource)) {
            minimumThumbSize = UIManager.getDimension("ScrollBar.minimumThumbSize");
        }
        scrollbar.setLayout(this);
    }

    protected void installKeyboardActions() {
        Utilities.installKeyboardActions(scrollbar, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "ScrollBar.ancestorInputMap", "ScrollBar.ancestorInputMap.RightToLeft");

        scrollbar.getActionMap().put("positiveUnitIncrement", newPositiveUnitIncrementAction());
        scrollbar.getActionMap().put("positiveBlockIncrement", newPositiveBlockIncrementAction());
        scrollbar.getActionMap().put("negativeUnitIncrement", newNegativeUnitIncrementAction());
        scrollbar.getActionMap().put("negativeBlockIncrement", newNegativeBlockIncrementAction());
        scrollbar.getActionMap().put("minScroll", newMinScrollAction());
        scrollbar.getActionMap().put("maxScroll", newMaxScrollAction());
    }

    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        scrollbar.addPropertyChangeListener(propertyChangeListener);

        modelListener = createModelListener();
        scrollbar.getModel().addChangeListener(modelListener);

        trackListener = createTrackListener();
        scrollbar.addMouseListener(trackListener);
        scrollbar.addMouseMotionListener(trackListener);

        scrollListener = createScrollListener();
        scrollTimer = new Timer(150, scrollListener);
        scrollTimer.setInitialDelay(450);

        buttonListener = createArrowButtonListener();
        incrButton.addMouseListener(buttonListener);
        decrButton.addMouseListener(buttonListener);
    }

    public void installUI(final JComponent c) {
        scrollbar = (JScrollBar)c;

        if (scrollbar.getOrientation() == HORIZONTAL) {
            orientationStrategy = new HorizontalStrategy();
        } else if (scrollbar.getOrientation() == VERTICAL) {
            orientationStrategy = new VerticalStrategy();
        }

        installDefaults();
        installComponents();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(final JComponent c) {
        scrollbar = (JScrollBar)c;

        uninstallDefaults();
        uninstallListeners();
        uninstallComponents();
        uninstallKeyboardActions();
    }

    public void layoutContainer(final Container c) {
        JScrollBar bar = ((JScrollBar)c);
        BoundedRangeModel model = bar.getModel();
        orientationStrategy.layoutScrollBar(bar);
        orientationStrategy.calculateTrackBounds(model);
        orientationStrategy.calculateThumbBounds(model);
    }

    protected void layoutHScrollbar(final JScrollBar bar) {
        incrButton.setBounds(bar.getWidth() - defaultButtonSize, 0, defaultButtonSize, scrollbar.getHeight());
        decrButton.setBounds(0, 0, defaultButtonSize, scrollbar.getHeight());
    }

    protected void layoutVScrollbar(final JScrollBar bar) {
        incrButton.setBounds(0, bar.getHeight() - defaultButtonSize, scrollbar.getWidth(), defaultButtonSize);
        decrButton.setBounds(0, 0, scrollbar.getWidth(), defaultButtonSize);
    }

    public void paint(final Graphics g, final JComponent c) {
        paintTrack(g, c, getTrackBounds());
        paintThumb(g, c, getThumbBounds());
        if (trackHighlight == INCREASE_HIGHLIGHT) {
            paintIncreaseHighlight(g);
        } else if (trackHighlight == DECREASE_HIGHLIGHT) {
            paintDecreaseHighlight(g);
        }
    }

    protected void paintDecreaseHighlight(final Graphics g) {
        orientationStrategy.paintDecreaseHighlight(g);
    }

    protected void paintIncreaseHighlight(final Graphics g) {
        orientationStrategy.paintIncreaseHighlight(g);
    }

    protected void paintThumb(final Graphics g, final JComponent c, final Rectangle r) {
        Color oldColor = g.getColor();

        g.setColor(thumbColor);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(Color.WHITE);
        g.drawRect(r.x + 1, r.y + 1, r.width - 1, r.height - 1);
        g.setColor(thumbDarkShadowColor);
        g.drawPolyline(new int[] { r.x, r.x + r.width, r.x + r.width },
                       new int[] { r.y + r.height, r.y + r.height, r.y}, 3);
        g.setColor(thumbLightShadowColor);
        g.drawPolyline(new int[] { r.x + 1, r.x + r.width - 1, r.x + r.width - 1 },
                       new int[] { r.y + r.height - 1, r.y + r.height - 1, r.y + 1 }, 3);

        g.setColor(oldColor);
    }

    protected void paintTrack(final Graphics g, final JComponent c, final Rectangle r) {
        Color oldColor = g.getColor();

        g.setColor(trackColor);
        g.fillRect(r.x, r.y, r.width, r.height);

        g.setColor(oldColor);
    }

    public Dimension preferredLayoutSize(final Container c) {
        if (c instanceof JComponent) {
            return getPreferredSize((JComponent)c);
        } else {
            return new Dimension(0, 0);
        }
    }

    protected void scrollByBlock(final int dir) {
        updateScrollBarValue(scrollbar.getBlockIncrement(dir) * dir);
    }

    protected void scrollByUnit(final int dir) {
        updateScrollBarValue(scrollbar.getUnitIncrement(dir) * dir);
    }

    protected void setThumbBounds(final int x, final int y, final int w, final int h) {
        thumbRect.setBounds(x, y, w, h);
    }

    protected void uninstallComponents() {
        scrollbar.remove(incrButton);
        scrollbar.remove(decrButton);
        incrButton = null;
        decrButton = null;
    }

    protected void uninstallDefaults() {
        Utilities.uninstallColorsAndFont(scrollbar);
    }

    protected void uninstallKeyboardActions() {
        Utilities.uninstallKeyboardActions(scrollbar, JComponent.WHEN_FOCUSED);
    }

    protected void uninstallListeners() {
        scrollbar.removePropertyChangeListener(propertyChangeListener);
        propertyChangeListener = null;

        scrollbar.getModel().removeChangeListener(modelListener);
        modelListener = null;

        scrollbar.removeMouseListener(trackListener);
        scrollbar.removeMouseMotionListener(trackListener);
        trackListener = null;

        scrollListener = null;
        scrollTimer = null;

        incrButton.removeMouseListener(buttonListener);
        decrButton.removeMouseListener(buttonListener);
        buttonListener = null;
    }

    private Action newPositiveUnitIncrementAction() {
        return new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                scrollByUnit(1);
            }
        };
    }

    private Action newNegativeUnitIncrementAction() {
        return new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                scrollByUnit(-1);
            }
        };
    }

    private Action newPositiveBlockIncrementAction() {
        return new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                scrollByBlock(1);
            }
        };
    }

    private Action newNegativeBlockIncrementAction() {
        return new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                scrollByBlock(-1);
            }
        };
    }

    private Action newMaxScrollAction() {
        return new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                scrollbar.setValue(scrollbar.getModel().getMaximum());
                scrollbar.repaint();
            }
        };
    }

    private Action newMinScrollAction() {
        return new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                scrollbar.setValue(scrollbar.getModel().getMinimum());
                scrollbar.repaint();
            }
        };
    }

    private void updateScrollBarValue(final int increment) {
        scrollbar.setValue(scrollbar.getValue() + increment);
        orientationStrategy.calculateThumbBounds(scrollbar.getModel());
        scrollbar.repaint();
    }

    private abstract class OrientationStrategy {
        abstract void calculateTrackBounds(BoundedRangeModel model);
        abstract int calculateDirection(Point current, Point next);
        abstract JButton createIncreaseButton();
        abstract JButton createDecreaseButton();
        abstract void layoutScrollBar(JScrollBar bar);
        abstract Dimension getPreferredSize(JComponent c);
        abstract void paintIncreaseHighlight(Graphics g);
        abstract void paintDecreaseHighlight(Graphics g);
        abstract int getButtonSize(JButton b);
        abstract Rectangle newThumbBounds(int offset, int size);
        abstract int getOffset(MouseEvent e, int currentMouseX, int currentMouseY);
        abstract int getTrackSize();
        abstract int getThumbSize(int proposedSize);

        void calculateThumbBounds(final BoundedRangeModel model) {
            int extent = model.getExtent();
            int viewSize = model.getMaximum() - model.getMinimum();
            if (viewSize == 0) {
                setThumbBounds(0, 0, 0, 0);
                return;
            }
            int proposedThumbSize = (int)((float)getTrackSize() * extent / viewSize);
            int thumbSize = getThumbSize(proposedThumbSize);
            int availableTrackSize = getTrackSize() - thumbSize;
            if (availableTrackSize <= 0) {
                if (proposedThumbSize == thumbSize) {
                    Rectangle trackBounds = getTrackBounds();
                    setThumbBounds(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                } else {
                    setThumbBounds(0, 0, 0, 0);
                }
            } else {
                int availableScrollingSize = viewSize - extent;
                int offset = (availableScrollingSize > 0 ? availableTrackSize * model.getValue() / availableScrollingSize : 0) + getButtonSize(decrButton);
                Rectangle newThumbBounds = newThumbBounds(offset, thumbSize);
                setThumbBounds(newThumbBounds.x, newThumbBounds.y, newThumbBounds.width, newThumbBounds.height);
                trackListener.offset = offset;
            }
        }

        void setValueOnDragging(final MouseEvent e, final int currentMouseX, final int currentMouseY, final int initialModelValue) {
            BoundedRangeModel model = scrollbar.getModel();
            int extent = model.getExtent();
            int viewSize = model.getMaximum() - model.getMinimum();
            int availableScrollingSize = viewSize - extent;
            int thumbSize = getThumbSize(Math.round(getTrackSize() * extent / viewSize));
            int availableTrackSize = getTrackSize() - thumbSize;
            int offset = getOffset(e, currentMouseX, currentMouseY);
            int modelIncrement = availableTrackSize != 0 ? offset * availableScrollingSize / availableTrackSize : 0;
            model.setValue(initialModelValue + modelIncrement);
        }
    }

    private class HorizontalStrategy extends OrientationStrategy {
        void calculateTrackBounds(final BoundedRangeModel model) {
            int visible = scrollbar.getWidth() - incrButton.getWidth() - decrButton.getWidth();
            trackRect.setBounds(decrButton.getWidth(), 0, visible, scrollbar.getHeight());
        }

        int getThumbSize(final int proposedSize) {
            if (proposedSize < minimumThumbSize.width) {
                return minimumThumbSize.width;
            } else if (proposedSize > maximumThumbSize.width) {
                return maximumThumbSize.width;
            } else {
                return proposedSize;
            }
        }

        JButton createIncreaseButton() {
            return BasicScrollBarUI.this.createIncreaseButton(EAST);
        }

        JButton createDecreaseButton() {
            return BasicScrollBarUI.this.createDecreaseButton(WEST);
        }

        void layoutScrollBar(final JScrollBar bar) {
            layoutHScrollbar(bar);
        }

        int calculateDirection(final Point current, final Point next) {
            if (scrollbar.getComponentOrientation().isLeftToRight()) {
                return (next.x > current.x) ? 1 : -1;
            } else {
                return (next.x > current.x) ? -1 : 1;
            }
        }

        Dimension getPreferredSize(final JComponent c) {
            final int width = incrButton.getSize().width + decrButton.getSize().width + getThumbBounds().width;
            final int height = Math.max(incrButton.getPreferredSize().height, decrButton.getPreferredSize().height);
            return new Dimension(width, height);
        }

        void paintIncreaseHighlight(final Graphics g) {
            Color oldColor = g.getColor();

            g.setColor(trackHighlightColor);
            g.fillRect(getThumbBounds().x + getThumbBounds().width + 1, 0,
                       getTrackBounds().width - (getThumbBounds().x + getThumbBounds().width) + getButtonSize(incrButton), getTrackBounds().height - 1);

            g.setColor(oldColor);
        }

        int getTrackSize() {
            return getTrackBounds().width;
        }

        void paintDecreaseHighlight(final Graphics g) {
            Color oldColor = g.getColor();

            g.setColor(trackHighlightColor);
            g.fillRect(getTrackBounds().x, 0, getThumbBounds().x - decrButton.getWidth(), getTrackBounds().height - 1);

            g.setColor(oldColor);
        }

        int getButtonSize(final JButton b) {
            return b.getWidth();
        }

        Rectangle newThumbBounds(final int offset, final int size) {
            if (scrollbar.getComponentOrientation().isLeftToRight()) {
                return new Rectangle(offset, 0, size, scrollbar.getHeight());
            } else {
                return new Rectangle(getTrackSize() + getButtonSize(incrButton) - (offset - getButtonSize(decrButton)) - size, 0, size, scrollbar.getHeight());
            }
        }

        int getOffset(final MouseEvent e, final int currentMouseX, final int currentMouseY) {
            if (scrollbar.getComponentOrientation().isLeftToRight()) {
                return e.getX() - currentMouseX;
            } else {
                return currentMouseX - e.getX();
            }
        }
    }

    private class VerticalStrategy extends OrientationStrategy {
        void calculateTrackBounds(final BoundedRangeModel model) {
            int visible = scrollbar.getHeight() - incrButton.getHeight() - decrButton.getHeight();
            trackRect.setBounds(0, decrButton.getWidth(), scrollbar.getWidth(), visible);
        }

        int getThumbSize(final int proposedSize) {
            if (proposedSize < minimumThumbSize.height) {
                return minimumThumbSize.height;
            } else if (proposedSize > maximumThumbSize.height) {
                return maximumThumbSize.height;
            } else {
                return proposedSize;
            }
        }

        JButton createIncreaseButton() {
            return BasicScrollBarUI.this.createIncreaseButton(SOUTH);
        }

        JButton createDecreaseButton() {
            return BasicScrollBarUI.this.createDecreaseButton(NORTH);
        }

        void layoutScrollBar(final JScrollBar bar) {
            layoutVScrollbar(bar);
        }

        int calculateDirection(final Point current, final Point next) {
            return (next.y > current.y) ? 1 : -1;
        }

        public Dimension getPreferredSize(final JComponent c) {
            final int width = Math.max(incrButton.getPreferredSize().width, decrButton.getPreferredSize().width);
            final int height = incrButton.getSize().height + decrButton.getSize().height + getThumbBounds().height;
            return new Dimension(width, height);
        }

        int getTrackSize() {
            return getTrackBounds().height;
        }

        void paintIncreaseHighlight(final Graphics g) {
            Color oldColor = g.getColor();

            g.setColor(trackHighlightColor);
            g.fillRect(0, getThumbBounds().y + getThumbBounds().height + 1,
                       getTrackBounds().width, getTrackBounds().height - (getThumbBounds().y + getThumbBounds().height) + getButtonSize(incrButton));

            g.setColor(oldColor);
        }

        void paintDecreaseHighlight(final Graphics g) {
            Color oldColor = g.getColor();

            g.setColor(trackHighlightColor);
            g.fillRect(0, getTrackBounds().y, getTrackBounds().width,
                       getThumbBounds().y - decrButton.getHeight());

            g.setColor(oldColor);
        }

        int getButtonSize(final JButton b) {
            return b.getHeight();
        }

        Rectangle newThumbBounds(final int offset, final int size) {
            return new Rectangle(0, offset, scrollbar.getWidth(), size);
        }

        int getOffset(final MouseEvent e, final int currentMouseX, final int currentMouseY) {
            return e.getY() - currentMouseY;
        }
    }
}
