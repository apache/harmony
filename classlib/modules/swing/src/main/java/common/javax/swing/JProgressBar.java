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
 * @author Dennis Ushakov
 */

package javax.swing;

import java.awt.Graphics;
import java.io.Serializable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ProgressBarUI;

import org.apache.harmony.x.swing.StringConstants;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class JProgressBar extends JComponent implements SwingConstants, Accessible {
    protected int orientation = JProgressBar.HORIZONTAL;
    protected boolean paintBorder = true;;
    protected BoundedRangeModel model;
    protected String progressString;
    protected boolean paintString;
    protected transient ChangeEvent changeEvent;
    protected ChangeListener changeListener = createChangeListener();

    private boolean indeterminate;
    private static final String UI_CLASS_ID = "ProgressBarUI";
    private static final String BORDER_PAINTED_PROPERTY = "borderPainted";
    private static final String PROGRESS_STRING_PROPERTY = "string";

    protected class AccessibleJProgressBar extends AccessibleJComponent implements AccessibleValue {
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PROGRESS_BAR;
        }

        public Number getCurrentAccessibleValue() {
            return new Integer(model.getValue());
        }

        public boolean setCurrentAccessibleValue(final Number n) {
            setValue(n.intValue());
            return true;
        }

        public Number getMinimumAccessibleValue() {
            return new Integer(model.getMinimum());
        }

        public Number getMaximumAccessibleValue() {
            return new Integer(model.getMaximum());
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet stateSet = super.getAccessibleStateSet();
            if (orientation == JProgressBar.HORIZONTAL) {
                stateSet.add(AccessibleState.HORIZONTAL);
            } else {
                stateSet.add(AccessibleState.VERTICAL);
            }
            return stateSet;
        }

        public AccessibleValue getAccessibleValue() {
            return this;
        }
    }

    private class ModelChangeListener implements ChangeListener, Serializable {

        public void stateChanged(final ChangeEvent e) {
            fireStateChanged();
        }

    }

    public JProgressBar() {
        this(new DefaultBoundedRangeModel());
    }

    public JProgressBar(final int orient) {
        this(new DefaultBoundedRangeModel());
        setOrientation(orient);
    }

    public JProgressBar(final int min, final int max) {
        this(new DefaultBoundedRangeModel(min, 0, min, max));
    }

    public JProgressBar(final int orient, final int min, final int max) {
        this(new DefaultBoundedRangeModel(min, 0, min, max));
        setOrientation(orient);
    }

    public JProgressBar(final BoundedRangeModel newModel) {
        model = newModel;
        model.addChangeListener(changeListener);
        updateUI();
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(final int orientation) {
        if (orientation != JProgressBar.HORIZONTAL
            && orientation != JProgressBar.VERTICAL) {

            throw new IllegalArgumentException(Messages.getString("swing.4A", orientation)); //$NON-NLS-1$
        }
        int oldValue = this.orientation;
        this.orientation = orientation;
        firePropertyChange(StringConstants.ORIENTATION, oldValue, orientation);
    }

    public boolean isStringPainted() {
        return paintString;
    }

    public void setStringPainted(final boolean painted) {
        boolean oldValue = paintString;
        paintString = painted;
        firePropertyChange(StringConstants.PROGRESS_STRING_PAINTED_PROPERTY, oldValue, painted);
    }

    public String getString() {
        return progressString;
    }

    public void setString(final String progressString) {
        String oldValue = this.progressString;
        this.progressString = progressString;
        firePropertyChange(JProgressBar.PROGRESS_STRING_PROPERTY, oldValue, progressString);
    }

    public double getPercentComplete() {
        int min = getMinimum();
        return 1. * (getValue() - min) / (getMaximum() - min);
    }

    public boolean isBorderPainted() {
        return paintBorder;
    }

    public void setBorderPainted(final boolean painted) {
        boolean oldValue = paintBorder;
        paintBorder = painted;
        firePropertyChange(JProgressBar.BORDER_PAINTED_PROPERTY, oldValue, painted);
    }

    public ProgressBarUI getUI() {
        return (ProgressBarUI)ui;
    }

    public void setUI(final ProgressBarUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
        setUI((ProgressBarUI)UIManager.getUI(this));
    }

    public String getUIClassID() {
        return JProgressBar.UI_CLASS_ID;
    }

    protected ChangeListener createChangeListener() {
        return new ModelChangeListener();
    }

    public void addChangeListener(final ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(final ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return (ChangeListener[])getListeners(ChangeListener.class);
    }

    protected void fireStateChanged() {
        if(changeEvent == null) {
            changeEvent = new ChangeEvent(this);
        }
        ChangeListener[] listeners = getChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].stateChanged(changeEvent);
        }
    }

    public BoundedRangeModel getModel() {
        return model;
    }

    public void setModel(final BoundedRangeModel model) {
        if (this.model != null) {
            this.model.removeChangeListener(changeListener);
        }
        this.model = model;
        if (model != null) {
            model.addChangeListener(changeListener);
        }
    }

    public void setValue(final int value) {
        model.setValue(value);
    }

    public int getValue() {
        return model.getValue();
    }

    public void setMinimum(final int min) {
        model.setMinimum(min);
    }

    public int getMinimum() {
        return model.getMinimum();
    }

    public void setMaximum(final int max) {
        model.setMaximum(max);
    }

    public int getMaximum() {
        return model.getMaximum();
    }

    public void setIndeterminate(final boolean indeterminate) {
        boolean oldValue = this.indeterminate;
        this.indeterminate = indeterminate;
        firePropertyChange(StringConstants.INDETERMINATE_PROPERTY, oldValue, indeterminate);
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJProgressBar();
        }
        return accessibleContext;
    }


    protected void paintBorder(final Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }
}


