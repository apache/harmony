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
import java.util.EventListener;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;
import org.apache.harmony.awt.ScrollbarStateController;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.state.ScrollbarState;

public class Scrollbar extends Component implements Adjustable, Accessible {
    private static final long serialVersionUID = 8451667562882310543L;

    protected class AccessibleAWTScrollBar extends Component.AccessibleAWTComponent implements
            AccessibleValue {
        private static final long serialVersionUID = -344337268523697807L;

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SCROLL_BAR;
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            toolkit.lockAWT();
            try {
                AccessibleStateSet aStateSet = super.getAccessibleStateSet();
                AccessibleState aState = null;
                switch (getOrientation()) {
                    case VERTICAL:
                        aState = AccessibleState.VERTICAL;
                        break;
                    case HORIZONTAL:
                        aState = AccessibleState.HORIZONTAL;
                        break;
                }
                if (aState != null) {
                    aStateSet.add(aState);
                }
                return aStateSet;
            } finally {
                toolkit.unlockAWT();
            }
        }

        @Override
        public AccessibleValue getAccessibleValue() {
            return this;
        }

        public Number getCurrentAccessibleValue() {
            return new Integer(getValue());
        }

        public Number getMaximumAccessibleValue() {
            return new Integer(getMaximum());
        }

        public Number getMinimumAccessibleValue() {
            return new Integer(getMinimum());
        }

        public boolean setCurrentAccessibleValue(Number n) {
            setValue(n.intValue());
            return true;
        }
    }

    public static final int HORIZONTAL = 0;

    public static final int VERTICAL = 1;

    final static int MAX = Integer.MAX_VALUE;

    private final AWTListenerList<AdjustmentListener> adjustmentListeners = new AWTListenerList<AdjustmentListener>(
            this);

    private int blockIncrement;

    private int unitIncrement;

    private int maximum;

    private int minimum;

    private int orientation;

    private int value;

    private transient boolean valueIsAdjusting;

    private int visibleAmount;

    final transient State state = new State();

    private final transient ScrollbarStateController stateController;

    public Scrollbar() throws HeadlessException {
        this(VERTICAL);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Scrollbar(int orientation) throws HeadlessException {
        this(orientation, 0, 10, 0, 100);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Scrollbar(int orientation, int value, int visible, int min, int max)
            throws HeadlessException {
        toolkit.lockAWT();
        try {
            setOrientation(orientation);
            setValues(value, visible, min, max);
            setUnitIncrement(1);
            setBlockIncrement(10);
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
                    if (isFocusable()) {
                        Scrollbar.this.requestFocus(false);
                    }
                }
            };
            addAWTMouseListener(stateController);
            addAWTKeyListener(stateController);
            addAWTFocusListener(stateController);
            addAWTMouseMotionListener(stateController);
            addAWTComponentListener(stateController);
        } finally {
            toolkit.unlockAWT();
        }
    }

    class State extends Component.ComponentState implements ScrollbarState {
        private final Rectangle decreaseRect = new Rectangle();

        private final Rectangle increaseRect = new Rectangle();

        private final Rectangle sliderRect = new Rectangle();

        private final Rectangle trackRect = new Rectangle();

        private final Rectangle upperTrackRect = new Rectangle();

        private final Rectangle lowerTrackRect = new Rectangle();

        private int trackSize;

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
            return value - minimum;
        }

        public boolean isVertical() {
            return (orientation == VERTICAL);
        }

        public int getSliderSize() {
            return visibleAmount;
        }

        public int getScrollSize() {
            return maximum - minimum;
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

        public void setSliderRect(Rectangle r) {
            sliderRect.setRect(r);
        }

        public void setIncreaseRect(Rectangle r) {
            increaseRect.setRect(r);
        }

        public void setDecreaseRect(Rectangle r) {
            decreaseRect.setRect(r);
        }

        public int getTrackSize() {
            return trackSize;
        }

        public void setTrackSize(int size) {
            trackSize = size;
        }

        public Adjustable getAdjustable() {
            return Scrollbar.this;
        }

        public int getHighlight() {
            return stateController.getHighlight();
        }

        public Rectangle getTrackBounds() {
            return trackRect;
        }

        public void setTrackBounds(Rectangle r) {
            trackRect.setBounds(r);
        }

        public ComponentOrientation getComponentOrientation() {
            return Scrollbar.this.getComponentOrientation();
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

        public Point getLocation() {
            return new Point(0, 0);
        }

        public void setValue(int type, int value) {
            if (type == AdjustmentEvent.TRACK) {
                setValuesImpl(value, visibleAmount, minimum, maximum, false);
                return;
            }
            Adjustable adj = getAdjustable();
            if (adj != null) {
                adj.setValue(value);
            }
        }

        @Override
        public void calculate() {
            toolkit.theme.calculateScrollbar(state);
        }
    }

    public int getValue() {
        toolkit.lockAWT();
        try {
            return value;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setValue(int val) {
        toolkit.lockAWT();
        try {
            setValues(val, visibleAmount, minimum, maximum);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void addNotify() {
        toolkit.lockAWT();
        try {
            super.addNotify();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        toolkit.lockAWT();
        try {
            return super.getAccessibleContext();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getBlockIncrement() {
        toolkit.lockAWT();
        try {
            return getPageIncrement();
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public int getLineIncrement() {
        toolkit.lockAWT();
        try {
            return unitIncrement;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getMaximum() {
        toolkit.lockAWT();
        try {
            return maximum;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getMinimum() {
        toolkit.lockAWT();
        try {
            return minimum;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getOrientation() {
        toolkit.lockAWT();
        try {
            return orientation;
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public int getPageIncrement() {
        toolkit.lockAWT();
        try {
            return blockIncrement;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getUnitIncrement() {
        toolkit.lockAWT();
        try {
            return getLineIncrement();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean getValueIsAdjusting() {
        toolkit.lockAWT();
        try {
            return valueIsAdjusting;
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public int getVisible() {
        toolkit.lockAWT();
        try {
            return visibleAmount;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getVisibleAmount() {
        toolkit.lockAWT();
        try {
            return getVisible();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    protected String paramString() {
        /* The format is based on 1.5 release behavior 
         * which can be revealed by the following code:
         * System.out.println(new Scrollbar());
         * System.out.println(new Scrollbar(Scrollbar.HORIZONTAL));
         */
        toolkit.lockAWT();
        try {
            return (super.paramString() + ",val=" + getValue() + ",vis=" + getVisibleAmount() //$NON-NLS-1$ //$NON-NLS-2$
                    + ",min=" + getMinimum() + ",max=" + getMaximum() //$NON-NLS-1$ //$NON-NLS-2$
                    + (getOrientation() == HORIZONTAL ? ",horz" : ",vert") + ",isAdjusting=" + getValueIsAdjusting()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setBlockIncrement(int v) {
        toolkit.lockAWT();
        try {
            blockIncrement = Math.max(1, v);
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void setLineIncrement(int v) {
        toolkit.lockAWT();
        try {
            setUnitIncrement(v);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setMaximum(int max) {
        toolkit.lockAWT();
        try {
            int oldMin = minimum;
            int oldMax = maximum;
            maximum = Math.max(Integer.MIN_VALUE + 1, max);
            minimum = Math.min(maximum - 1, minimum);
            setValues(value, visibleAmount, minimum, maximum);
            if ((maximum != oldMax) || (minimum != oldMin)) {
                doRepaint();
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setMinimum(int min) {
        toolkit.lockAWT();
        try {
            setValues(value, visibleAmount, min, maximum);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setOrientation(int orientation) {
        toolkit.lockAWT();
        try {
            if ((orientation != HORIZONTAL) && (orientation != VERTICAL)) {
                // awt.113=illegal scrollbar orientation
                throw new IllegalArgumentException(Messages.getString("awt.113")); //$NON-NLS-1$
            }
            this.orientation = orientation;
            doRepaint();
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void setPageIncrement(int v) {
        toolkit.lockAWT();
        try {
            setBlockIncrement(v);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setUnitIncrement(int v) {
        toolkit.lockAWT();
        try {
            unitIncrement = Math.max(1, v);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setValueIsAdjusting(boolean b) {
        toolkit.lockAWT();
        try {
            valueIsAdjusting = b;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setValues(int value, int visible, int min, int max) {
        toolkit.lockAWT();
        try {
            setValuesImpl(value, visible, min, max, true);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setVisibleAmount(int newAmount) {
        toolkit.lockAWT();
        try {
            setValues(value, newAmount, minimum, maximum);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (AdjustmentListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getAdjustmentListeners();
        }
        return super.getListeners(listenerType);
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

    @Override
    protected void processEvent(AWTEvent e) {
        if (toolkit.eventTypeLookup.getEventMask(e) == AWTEvent.ADJUSTMENT_EVENT_MASK) {
            processAdjustmentEvent((AdjustmentEvent) e);
        } else {
            super.processEvent(e);
        }
    }

    protected void processAdjustmentEvent(AdjustmentEvent e) {
        for (AdjustmentListener listener : adjustmentListeners.getUserListeners()) {
            switch (e.getID()) {
                case AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED:
                    listener.adjustmentValueChanged(e);
                    break;
            }
        }
    }

    @Override
    String autoName() {
        return ("scrollbar" + toolkit.autoNumber.nextScrollbar++); //$NON-NLS-1$
    }

    @Override
    void prepaint(Graphics g) {
        toolkit.theme.drawScrollbar(g, state);
    }

    @Override
    boolean isPrepainter() {
        return true;
    }

    private void doRepaint(Rectangle r) {
        if (isDisplayable()) {
            if (isShowing()) {
                repaint(r.x, r.y, r.width, r.height);
            }
        }
    }

    private void doRepaint() {
        toolkit.theme.layoutScrollbar(state);
        invalidate();
        doRepaint(new Rectangle(new Point(), getSize()));
    }

    @Override
    void setEnabledImpl(boolean value) {
        super.setEnabledImpl(value);
        doRepaint();
    }

    @Override
    ComponentBehavior createBehavior() {
        return new HWBehavior(this);
    }

    @Override
    void validateImpl() {
        super.validateImpl();
        toolkit.theme.calculateScrollbar(state);
    }

    void generateEvent() {
        int type = stateController.getType();
        if (type < 0) {
            return;
        }
        setValueIsAdjusting(stateController.isSliderDragged());
        postEvent(new AdjustmentEvent(this, AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED, type,
                value, getValueIsAdjusting()));
    }

    @Override
    Dimension getDefaultMinimumSize() {
        return state.getDefaultMinimumSize();
    }

    @Override
    void resetDefaultSize() {
        state.reset();
    }

    void setValuesImpl(int value, int visible, int min, int max, boolean repaint) {
        int oldValue = this.value;
        int oldMin = minimum;
        minimum = Math.min(min, MAX - 1);
        maximum = Math.max(minimum + 1, max);
        if (maximum - minimum < 0) {
            maximum = minimum + MAX;
        }
        visibleAmount = Math.max(1, visible);
        visibleAmount = Math.min(maximum - minimum, visibleAmount);
        this.value = Math.max(minimum, value);
        this.value = Math.min(this.value, maximum - visibleAmount);
        repaint &= (oldValue != this.value) || (oldMin != minimum);
        if (repaint) {
            doRepaint();
        }
    }

    @Override
    AccessibleContext createAccessibleContext() {
        return new AccessibleAWTScrollBar();
    }
}
