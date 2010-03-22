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

package javax.swing;

import java.awt.Adjustable;
import java.awt.Dimension;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.ScrollBarUI;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class JScrollBar extends JComponent implements Adjustable, Accessible {

    protected class AccessibleJScrollBar extends JComponent.AccessibleJComponent implements AccessibleValue {
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet result = new AccessibleStateSet();

            if (isEnabled()) {
                result.add(AccessibleState.ENABLED);
            }
            if (isFocusable()) {
                result.add(AccessibleState.FOCUSABLE);
            }
            if (isVisible()) {
                result.add(AccessibleState.VISIBLE);
            }
            if (isOpaque()) {
                result.add(AccessibleState.OPAQUE);
            }
            if (getOrientation() == HORIZONTAL) {
                result.add(AccessibleState.HORIZONTAL);
            } else {
                result.add(AccessibleState.VERTICAL);
            }

            return result;
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SCROLL_BAR;
        }

        public AccessibleValue getAccessibleValue() {
            return this;
        }

        public Number getCurrentAccessibleValue() {
            return new Integer(getValue());
        }

        public boolean setCurrentAccessibleValue(final Number n) {
            boolean result;
            try {
                setValue(n.intValue());
                result = true;
            } catch (final Exception e) {
                result = false;
            }
            return result;
        }

        public Number getMinimumAccessibleValue() {
            return new Integer(getMinimum());
        }

        public Number getMaximumAccessibleValue() {
            return new Integer(getMaximum() - getVisibleAmount());
        }
    }

    protected BoundedRangeModel model;
    protected int blockIncrement = 10;
    protected int unitIncrement = 1;
    protected int orientation = VERTICAL;
    private static final String classUIID = "ScrollBarUI";

    private ChangeListener modelChangeHandler;
    private static final String BLOCK_INCREMENT = "blockIncrement";
    private static final String UNIT_INCREMENT = "unitIncrement";
    private static final String ORIENTATION_PROPERTY = "orientation";
    private static final String MODEL_PROPERTY = "model";

    public JScrollBar() {
        this(VERTICAL, 0, 10, 0, 100);
    }

    public JScrollBar(final int orientation) {
        this(orientation, 0, 10, 0, 100);
    }

    public JScrollBar(final int orientation, final int value, final int extent, final int min, final int max) {
        model = new DefaultBoundedRangeModel(value, extent, min, max);

        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException(Messages.getString("swing.28")); //$NON-NLS-1$
        } 

        this.orientation = orientation;
        blockIncrement = extent;

        modelChangeHandler = new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                fireAdjustmentValueChanged(AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED, AdjustmentEvent.TRACK, getValue());
            }
        };

        model.addChangeListener(modelChangeHandler);
        listenerList = new EventListenerList();

        updateUI();
    }

    public void setUI(final ScrollBarUI ui) {
        super.setUI(ui);
    }

    public ScrollBarUI getUI() {
        return (ScrollBarUI)ui;
    }

    public void updateUI() {
        setUI((ScrollBarUI)UIManager.getUI(this));
    }

    public String getUIClassID() {
        return classUIID;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(final int orientation) {
        if (orientation != VERTICAL && orientation != HORIZONTAL) {
            throw new IllegalArgumentException(Messages.getString("swing.28")); //$NON-NLS-1$
        }
        if (orientation != this.orientation) {
            int oldValue = this.orientation;
            this.orientation = orientation;
            firePropertyChange(ORIENTATION_PROPERTY, oldValue, orientation);
        }
    }

    public BoundedRangeModel getModel() {
        return model;
    }

    public void setModel(final BoundedRangeModel model) {
        if (model != this.model) {
            BoundedRangeModel oldValue = this.model;
            if (oldValue != null) {
                oldValue.removeChangeListener(modelChangeHandler);
            }

            this.model = model;
            if (model != null) {
                model.addChangeListener(modelChangeHandler);
            }

            firePropertyChange(MODEL_PROPERTY, oldValue, model);
        }
    }

    public int getUnitIncrement(final int direction) {
        return unitIncrement;
    }

    public void setUnitIncrement(final int unitIncrement) {
        if (this.unitIncrement != unitIncrement) {
            int oldValue = this.unitIncrement;
            this.unitIncrement = unitIncrement;
            firePropertyChange(UNIT_INCREMENT, oldValue, unitIncrement);
        }
    }

    public int getBlockIncrement(final int direction) {
        return blockIncrement;
    }

    public void setBlockIncrement(final int blockIncrement) {
        if (this.blockIncrement != blockIncrement) {
            int oldValue = this.blockIncrement;
            this.blockIncrement = blockIncrement;
            firePropertyChange(BLOCK_INCREMENT, oldValue, blockIncrement);
        }
    }

    public int getUnitIncrement() {
        return unitIncrement;
    }

    public int getBlockIncrement() {
        return blockIncrement;
    }

    public int getValue() {
        return getModel().getValue();
    }

    public void setValue(final int value) {
        getModel().setValue(value);
    }

    public int getVisibleAmount() {
        return getModel().getExtent();
    }

    public void setVisibleAmount(final int extent) {
        getModel().setExtent(extent);
    }

    public int getMinimum() {
        return getModel().getMinimum();
    }

    public void setMinimum(final int minimum) {
        getModel().setMinimum(minimum);
    }

    public int getMaximum() {
        return getModel().getMaximum();
    }

    public void setMaximum(final int maximum) {
        getModel().setMaximum(maximum);
    }

    public boolean getValueIsAdjusting() {
        return getModel().getValueIsAdjusting();
    }

    public void setValueIsAdjusting(final boolean b) {
        getModel().setValueIsAdjusting(b);
    }

    public void setValues(final int newValue, final int newExtent, final int newMin, final int newMax) {
        getModel().setRangeProperties(newValue, newExtent, newMin, newMax, getModel().getValueIsAdjusting());
    }

    public void addAdjustmentListener(final AdjustmentListener l) {
        listenerList.add(AdjustmentListener.class, l);
    }

    public void removeAdjustmentListener(final AdjustmentListener l) {
        listenerList.remove(AdjustmentListener.class, l);
    }

    public AdjustmentListener[] getAdjustmentListeners() {
        return (AdjustmentListener[])listenerList.getListeners(AdjustmentListener.class);
    }

    protected void fireAdjustmentValueChanged(final int id, final int type, final int value) {
        AdjustmentListener[] listeners = getAdjustmentListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].adjustmentValueChanged(new AdjustmentEvent(this, id, type, value));
        }
    }

    public void setEnabled(final boolean b) {
        super.setEnabled(b);
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJScrollBar();
        }
        return accessibleContext;
    }
}
