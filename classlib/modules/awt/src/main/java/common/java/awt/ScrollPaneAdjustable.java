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

package java.awt;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.InvocationEvent;
import java.io.Serializable;
import java.util.Collection;
import org.apache.harmony.awt.ScrollbarStateController;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.state.ScrollbarState;
import org.apache.harmony.awt.wtk.NativeWindow;

public class ScrollPaneAdjustable implements Adjustable, Serializable {
    private static final long serialVersionUID = -3359745691033257079L;

    private final AWTListenerList<AdjustmentListener> adjustmentListeners = new AWTListenerList<AdjustmentListener>();

    private final int orientation;

    private final Component comp;

    private int value;

    private int visibleAmount;

    private int minimum;

    private int maximum;

    private int unitIncrement;

    private int blockIncrement;

    private boolean valueIsAdjusting;

    private final ScrollbarStateController stateController;

    private final Rectangle bounds = new Rectangle();

    private final transient State state = new State();

    private boolean callAWTListener;

    class State implements ScrollbarState {
        private final Rectangle decreaseRect = new Rectangle();

        private final Rectangle increaseRect = new Rectangle();

        private final Rectangle sliderRect = new Rectangle();

        private final Rectangle trackRect = new Rectangle();

        private final Rectangle upperTrackRect = new Rectangle();

        private final Rectangle lowerTrackRect = new Rectangle();

        private int trackSize;

        public Adjustable getAdjustable() {
            return ScrollPaneAdjustable.this;
        }

        public int getHighlight() {
            return stateController.getHighlight();
        }

        public ComponentOrientation getComponentOrientation() {
            return comp.getComponentOrientation();
        }

        public boolean isDecreasePressed() {
            return stateController.isDecreasePressed();
        }

        public boolean isIncreasePressed() {
            return stateController.isIncreasePressed();
        }

        public boolean isSliderPressed() {
            return stateController.isSliderDragged();
        }

        public int getSliderPosition() {
            return getValue();
        }

        public int getSliderSize() {
            return getVisibleAmount();
        }

        public int getScrollSize() {
            return getMaximum() - getMinimum();
        }

        public boolean isVertical() {
            return (getOrientation() == Adjustable.VERTICAL);
        }

        public Rectangle getTrackBounds() {
            return trackRect;
        }

        public void setTrackBounds(Rectangle r) {
            trackRect.setBounds(r);
        }

        public Rectangle getUpperTrackBounds() {
            return upperTrackRect;
        }

        public Rectangle getLowerTrackBounds() {
            return lowerTrackRect;
        }

        public void setUpperTrackBounds(Rectangle r) {
            upperTrackRect.setBounds(r);
        }

        public void setLowerTrackBounds(Rectangle r) {
            lowerTrackRect.setBounds(r);
        }

        public void setDecreaseRect(Rectangle r) {
            decreaseRect.setRect(r);
        }

        public void setSliderRect(Rectangle r) {
            sliderRect.setRect(r);
        }

        public void setIncreaseRect(Rectangle r) {
            increaseRect.setRect(r);
        }

        public void setTrackSize(int size) {
            trackSize = size;
        }

        public int getTrackSize() {
            return trackSize;
        }

        public Rectangle getSliderRect() {
            //don't let modify rect in any other way
            //than calling setXXXRect()
            return sliderRect;
        }

        public Rectangle getIncreaseRect() {
            return increaseRect;
        }

        public Rectangle getDecreaseRect() {
            return decreaseRect;
        }

        public boolean isEnabled() {
            return comp.isEnabled();
        }

        public boolean isVisible() {
            return comp.isVisible();
        }

        public boolean isFocused() {
            return false;
        }

        public Font getFont() {
            return null;
        }

        public boolean isFontSet() {
            return false;
        }

        public FontMetrics getFontMetrics() {
            return null;
        }

        public Color getBackground() {
            return comp.getBackground();
        }

        public boolean isBackgroundSet() {
            return comp.isBackgroundSet();
        }

        public Color getTextColor() {
            return null;
        }

        public Rectangle getBounds() {
            if (bounds != null) {
                return bounds.getBounds();
            }
            return null;
        }

        public Dimension getSize() {
            if (bounds != null) {
                return bounds.getSize();
            }
            return null;
        }

        public Dimension getDefaultMinimumSize() {
            return null;
        }

        public void setDefaultMinimumSize(Dimension size) {
        }

        public long getWindowId() {
            NativeWindow win = comp.getNativeWindow();
            return (win != null) ? win.getId() : 0;
        }

        public Point getLocation() {
            return bounds.getLocation();
        }

        public void setValue(int type, int value) {
            ScrollPaneAdjustable.this.setValue(type, value);
        }

        public boolean isTextColorSet() {
            return false;
        }
    }

    ScrollPaneAdjustable(Component comp, int orientation) {
        this.comp = comp;
        this.orientation = orientation;
        unitIncrement = 1;
        blockIncrement = 1;
        stateController = new ScrollbarStateController(state) {
            @Override
            protected void fireEvent() {
                generateEvent();
            }

            @Override
            protected void repaint(Rectangle r) {
                doRepaint(r);
            }

            @Override
            protected void repaint() {
                doRepaint();
            }

            @Override
            protected void requestFocus() {
                // just don't do it
            }
        };
        comp.addAWTMouseListener(stateController);
        comp.addAWTMouseMotionListener(stateController);
        comp.addAWTComponentListener(stateController);
    }

    @Override
    public String toString() {
        return (getClass().getName() + "[" + paramString() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public int getValue() {
        return value;
    }

    public void setValue(int v) {
        setValue(AdjustmentEvent.TRACK, v);
    }

    public int getBlockIncrement() {
        return blockIncrement;
    }

    public int getMaximum() {
        return maximum;
    }

    public int getMinimum() {
        return minimum;
    }

    public int getOrientation() {
        return orientation;
    }

    public int getUnitIncrement() {
        return unitIncrement;
    }

    public boolean getValueIsAdjusting() {
        return valueIsAdjusting;
    }

    public int getVisibleAmount() {
        return visibleAmount;
    }

    public String paramString() {
        /* The format is based on 1.5 release behavior 
         * which can be revealed by the following code:
         * 
         * System.out.println(new ScrollPane().getVAdjustable());
         * System.out.println(new ScrollPane().getHAdjustable());
         */
        String orientStr = ""; //$NON-NLS-1$
        switch (orientation) {
            case Adjustable.HORIZONTAL:
                orientStr = "horizontal"; //$NON-NLS-1$
                break;
            case Adjustable.VERTICAL:
                orientStr = "vertical"; //$NON-NLS-1$
                break;
            case Adjustable.NO_ORIENTATION:
                orientStr = "no orientation"; //$NON-NLS-1$
                break;
        }
        return (orientStr + ",val=" + value + ",vis=" + visibleAmount + ",[" + minimum + ".." //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                + maximum + "]" + ",unit=" + unitIncrement + ",block=" + blockIncrement //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + ",isAdjusting=" + valueIsAdjusting); //$NON-NLS-1$
    }

    public void setBlockIncrement(int b) {
        blockIncrement = b;
    }

    public void setMaximum(int max) {
        // awt.144=Can be set by scrollpane only
        throw new AWTError(Messages.getString("awt.144")); //$NON-NLS-1$
    }

    public void setMinimum(int min) {
        // awt.144=Can be set by scrollpane only
        throw new AWTError(Messages.getString("awt.144")); //$NON-NLS-1$
    }

    public void setUnitIncrement(int u) {
        unitIncrement = u;
    }

    public void setValueIsAdjusting(boolean b) {
        valueIsAdjusting = b;
    }

    public void setVisibleAmount(int vis) {
        // awt.144=Can be set by scrollpane only
        throw new AWTError(Messages.getString("awt.144")); //$NON-NLS-1$
    }

    public void addAdjustmentListener(AdjustmentListener l) {
        adjustmentListeners.addUserListener(l);
    }

    public void removeAdjustmentListener(AdjustmentListener l) {
        adjustmentListeners.removeUserListener(l);
    }

    public AdjustmentListener[] getAdjustmentListeners() {
        return adjustmentListeners.getUserListeners(new AdjustmentListener[0]);
    }

    final Rectangle getBounds() {
        return bounds.getBounds();
    }

    final void setBounds(Rectangle bounds) {
        Rectangle oldBounds = this.bounds.getBounds();
        this.bounds.setBounds(bounds);
        if (!oldBounds.equals(bounds)) {
            doRepaint();
        }
    }

    void doRepaint() {
        doRepaint(new Rectangle(new Point(), bounds.getSize()));
    }

    void doRepaint(Rectangle r) {
        if (comp.isDisplayable()) {
            comp.toolkit.theme.layoutScrollbar(state);
            comp.invalidate();
            Rectangle paintRect = new Rectangle(r);
            paintRect.translate(bounds.x, bounds.y);
            repaintComponent(paintRect);
        }
    }

    void repaintComponent(Rectangle r) {
        if (comp.isShowing()) {
            comp.repaint(r.x, r.y, r.width, r.height);
        }
    }

    void prepaint(Graphics g) {
        if ((bounds == null) || bounds.isEmpty()) {
            return;
        }
        g.translate(bounds.x, bounds.y);
        comp.toolkit.theme.drawScrollbar(g, state);
        g.translate(-bounds.x, -bounds.y);
    }

    void generateEvent() {
        setValueIsAdjusting(stateController.isSliderDragged());
    }

    private void postAdjustmentEvent(final int type) {
        // create and post invocation event here:
        comp.postEvent(new InvocationEvent(ScrollPaneAdjustable.this, new Runnable() {
            public void run() {
                AdjustmentEvent event = new AdjustmentEvent(ScrollPaneAdjustable.this,
                        AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED, type, value,
                        getValueIsAdjusting());
                if (callAWTListener) {
                    comp.toolkit.lockAWT();
                    try {
                        processAdjustmentEvent(event, adjustmentListeners.getSystemListeners());
                    } finally {
                        comp.toolkit.unlockAWT();
                    }
                }
                processAdjustmentEvent(event, adjustmentListeners.getUserListeners());
            }
        }));
    }

    /**
     * Set parameters which cannot be set by public api methods
     */
    void setSizes(int vis, int min, int max) {
        boolean repaint = false;
        if (vis != visibleAmount) {
            visibleAmount = vis;
            repaint = true;
        }
        if (min != minimum) {
            minimum = min;
            repaint = true;
        }
        if (max != maximum) {
            maximum = max;
            repaint = true;
        }
        if (repaint) {
            doRepaint();
        }
    }

    void processAdjustmentEvent(AdjustmentEvent e, Collection<AdjustmentListener> c) {
        for (AdjustmentListener listener : c) {
            switch (e.getID()) {
                case AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED:
                    listener.adjustmentValueChanged(e);
                    break;
            }
        }
    }

    void setValue(int type, int val) {
        int oldVal = value;
        value = Math.max(Math.min(val, maximum - visibleAmount), minimum);
        if (oldVal != value) {
            Rectangle oldRect = new Rectangle(state.getSliderRect());
            comp.toolkit.theme.layoutScrollbar(state); //TODO: FIXME
            Rectangle paintRect = oldRect.union(state.getSliderRect());
            paintRect.grow(0, 1);
            postAdjustmentEvent(type);
            doRepaint(paintRect);
        }
    }

    void addAWTAdjustmentListener(AdjustmentListener l) {
        adjustmentListeners.addSystemListener(l);
        callAWTListener = true;
    }

    ScrollbarStateController getStateController() {
        return stateController;
    }
}
