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

import java.awt.Component;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.UIResource;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class JSlider extends JComponent implements SwingConstants, Accessible {
    protected class AccessibleJSlider extends AccessibleJComponent implements AccessibleValue {
        public AccessibleStateSet getAccessibleStateSet() {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
        public AccessibleRole getAccessibleRole() {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
        public AccessibleValue getAccessibleValue() {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
        public Number getCurrentAccessibleValue() {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
        public boolean setCurrentAccessibleValue(final Number n) {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
        public Number getMinimumAccessibleValue() {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
        public Number getMaximumAccessibleValue() {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
    }

    private class LabelUIResource extends JLabel implements UIResource {
        LabelUIResource(final String text) {
            super(text);
        }
    }

    protected BoundedRangeModel sliderModel;
    protected int majorTickSpacing;
    protected int minorTickSpacing;
    protected boolean snapToTicks;
    protected int orientation;
    protected ChangeListener changeListener;
    protected transient ChangeEvent changeEvent;

    private static final String SLIDER_UI_ID = "SliderUI";

    private static final String LABEL_TABLE = "labelTable";
    private static final String INVERTED = "inverted";
    private static final String MAJOR_TICK_SPACING = "majorTickSpacing";
    private static final String MINOR_TICK_SPACING = "minorTickSpacing";
    private static final String SNAP_TO_TICKS = "snapToTicks";
    private static final String PAINT_TICKS = "paintTicks";
    private static final String PAINT_TRACK = "paintTrack";
    private static final String ORIENTATION_PROPERTY = "orientation";
    private static final String MODEL_PROPERTY = "model";
    private static final String PAINT_LABELS = "paintLabels";

    private EventListenerList eventListenerList;

    private boolean paintTrack = true;
    private boolean inverted;
    private Dictionary labels;
    private boolean paintTicks;
    private boolean paintLabels;
    private ChangeListener modelChangeHandler;

    public JSlider() {
        this(HORIZONTAL, 0, 100, 50);
    }

    public JSlider(final int orientation) {
        this(orientation, 0, 100, 50);
    }

    public JSlider(final int min, final int max) {
        this(HORIZONTAL, min, max, (min + max) / 2);
    }

    public JSlider(final int min, final int max, final int value) {
        this(HORIZONTAL, min, max, value);
    }

    public JSlider(final int orientation, final int min, final int max, final int value) {
        this(checkOrientation(orientation),
             new DefaultBoundedRangeModel(value, 0, min, max));
    }

    public JSlider(final BoundedRangeModel brm) {
        this(HORIZONTAL, brm);
    }

    private JSlider(final int orientation, final BoundedRangeModel brm) {
        this.orientation = orientation;
        sliderModel = brm;

        modelChangeHandler = createChangeListener();
        sliderModel.addChangeListener(modelChangeHandler);

        eventListenerList = new EventListenerList();

        updateUI();
    }

    private static int checkOrientation(final int orientation) {
        if (orientation != VERTICAL && orientation != HORIZONTAL) {
            throw new IllegalArgumentException(Messages.getString("swing.28")); //$NON-NLS-1$
        }
        return orientation;
    }

    public SliderUI getUI() {
        return (SliderUI)ui;
    }

    public void setUI(final SliderUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
        setUI((SliderUI)UIManager.getUI(this));
    }

    public String getUIClassID() {
        return SLIDER_UI_ID;
    }

    protected ChangeListener createChangeListener() {
        return new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                fireStateChanged();
            }
        };
    }

    public void addChangeListener(final ChangeListener l) {
        eventListenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(final ChangeListener l) {
        eventListenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return (ChangeListener[])eventListenerList.getListeners(ChangeListener.class);
    }

    protected void fireStateChanged() {
        ChangeListener[] changeListeners = getChangeListeners();
        ChangeEvent changeEvent = new ChangeEvent(this);
        for (int i = 0; i < changeListeners.length; i++) {
            changeListeners[i].stateChanged(changeEvent);
        }
    }

    public BoundedRangeModel getModel() {
        return sliderModel;
    }

    public void setModel(final BoundedRangeModel newModel) {
        if (newModel != sliderModel) {
            BoundedRangeModel oldValue = sliderModel;
            if (oldValue != null) {
                oldValue.removeChangeListener(modelChangeHandler);
            }

            sliderModel = newModel;
            if (sliderModel != null) {
                sliderModel.addChangeListener(modelChangeHandler);
            }

            firePropertyChange(MODEL_PROPERTY, oldValue, newModel);
        }
    }

    public int getValue() {
        return sliderModel.getValue();
    }

    public void setValue(final int n) {
        sliderModel.setValue(n);
    }

    public int getMinimum() {
        return sliderModel.getMinimum();
    }

    public void setMinimum(final int minimum) {
        sliderModel.setMinimum(minimum);
    }

    public int getMaximum() {
        return sliderModel.getMaximum();
    }

    public void setMaximum(final int maximum) {
        sliderModel.setMaximum(maximum);
    }

    public boolean getValueIsAdjusting() {
        return sliderModel.getValueIsAdjusting();
    }

    public void setValueIsAdjusting(final boolean b) {
        sliderModel.setValueIsAdjusting(b);
    }

    public int getExtent() {
        return sliderModel.getExtent();
    }

    public void setExtent(final int extent) {
        sliderModel.setExtent(extent);
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(final int orientation) {
        if (orientation != VERTICAL && orientation != HORIZONTAL) {
            throw new IllegalArgumentException(Messages.getString("swing.29")); //$NON-NLS-1$
        }
        if (orientation != this.orientation) {
            int oldValue = this.orientation;
            this.orientation = orientation;
            firePropertyChange(ORIENTATION_PROPERTY, oldValue, orientation);
        }
    }

    public Dictionary getLabelTable() {
        return labels;
    }

    public void setLabelTable(final Dictionary labels) {
        if (this.labels != labels) {
            Dictionary oldValue = this.labels;
            this.labels = labels;
            firePropertyChange(LABEL_TABLE, oldValue, labels);

            Enumeration keys = this.labels.keys();
            while (keys.hasMoreElements()) {
                Object obj = this.labels.get(keys.nextElement());
                if (obj instanceof Component) {
                    Component label = ((Component)obj);
                    label.setSize(label.getPreferredSize());
                }
            }
        }
    }

    protected void updateLabelUIs() {
        if (labels == null) {
            return;
        }

        Enumeration en = labels.keys();
        while (en.hasMoreElements()) {
            JLabel l = (JLabel)en.nextElement();
            l.updateUI();
        }
    }

    public Hashtable createStandardLabels(final int increment) {
        return createStandardLabels(increment, sliderModel.getMinimum());
    }

    public Hashtable createStandardLabels(final int increment, final int start) {
        if (increment <= 0) {
            throw new IllegalArgumentException(Messages.getString("swing.2A")); //$NON-NLS-1$
        }
        if (start < sliderModel.getMinimum() || start > sliderModel.getMaximum()) {
            throw new IllegalArgumentException(Messages.getString("swing.2B")); //$NON-NLS-1$
        }

        Hashtable result = new Hashtable();
        for (int i = start; i <= sliderModel.getMaximum(); i += increment) {
            JLabel label = new LabelUIResource(Integer.toString(i));
            result.put(new Integer(i), label);
        }

        return result;
    }

    public boolean getInverted() {
        return inverted;
    }

    public void setInverted(final boolean b) {
        if (inverted != b) {
            boolean oldValue = inverted;
            inverted = b;
            firePropertyChange(INVERTED, oldValue, b);
        }
    }

    public int getMajorTickSpacing() {
        return majorTickSpacing;
    }

    public void setMajorTickSpacing(final int n) {
        if (majorTickSpacing != n) {
            int oldValue = majorTickSpacing;
            majorTickSpacing = n;
            firePropertyChange(MAJOR_TICK_SPACING, oldValue, n);
            
            if (labels == null && getPaintLabels()) {
                setLabelTable(createStandardLabels(getMajorTickSpacing()));
            }
        }
    }

    public int getMinorTickSpacing() {
        return minorTickSpacing;
    }

    public void setMinorTickSpacing(final int n) {
        if (minorTickSpacing != n) {
            int oldValue = minorTickSpacing;
            minorTickSpacing = n;
            firePropertyChange(MINOR_TICK_SPACING, oldValue, n);
        }
    }

    public boolean getSnapToTicks() {
        return snapToTicks;
    }

    public void setSnapToTicks(final boolean b) {
        if (snapToTicks != b) {
            boolean oldValue = snapToTicks;
            snapToTicks = b;
            firePropertyChange(SNAP_TO_TICKS, oldValue, b);
        }
    }

    public boolean getPaintTicks() {
        return paintTicks;
    }

    public void setPaintTicks(final boolean b) {
        if (paintTicks != b) {
            boolean oldValue = paintTicks;
            paintTicks = b;
            firePropertyChange(PAINT_TICKS, oldValue, b);
        }
    }

    public boolean getPaintTrack() {
        return paintTrack;
    }

    public void setPaintTrack(final boolean b) {
        if (paintTrack != b) {
            boolean oldValue = paintTrack;
            paintTrack = b;
            firePropertyChange(PAINT_TRACK, oldValue, b);
        }
    }

    public boolean getPaintLabels() {
        return paintLabels;
    }

    public void setPaintLabels(final boolean b) {
        if (paintLabels != b) {
            boolean oldValue = paintLabels;
            paintLabels = b;
            firePropertyChange(PAINT_LABELS, oldValue, b);

            if (labels == null && getMajorTickSpacing() != 0) {
                setLabelTable(createStandardLabels(getMajorTickSpacing()));
            }
        }
    }

    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJSlider();
        }
        return accessibleContext;
    }
}
