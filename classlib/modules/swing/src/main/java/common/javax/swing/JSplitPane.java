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
 * @author Anton Avtamonov
 */

package javax.swing;

import java.awt.Component;
import java.awt.Graphics;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;
import javax.swing.plaf.SplitPaneUI;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class JSplitPane extends JComponent implements Accessible {
    public static final String BOTTOM = "bottom";
    public static final String TOP = "top";
    public static final String DIVIDER = "divider";
    public static final String LEFT = "left";
    public static final String RIGHT = "right";

    public static final String CONTINUOUS_LAYOUT_PROPERTY = "continuousLayout";
    public static final String DIVIDER_LOCATION_PROPERTY = "dividerLocation";
    public static final String DIVIDER_SIZE_PROPERTY = "dividerSize";
    public static final String LAST_DIVIDER_LOCATION_PROPERTY = "lastDividerLocation";
    public static final String ONE_TOUCH_EXPANDABLE_PROPERTY = "oneTouchExpandable";
    public static final String ORIENTATION_PROPERTY = "orientation";
    public static final String RESIZE_WEIGHT_PROPERTY = "resizeWeight";

    public static final int VERTICAL_SPLIT = 0;
    public static final int HORIZONTAL_SPLIT = 1;

    protected class AccessibleJSplitPane extends AccessibleJComponent implements AccessibleValue {
        public AccessibleStateSet getAccessibleStateSet() {
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

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SPLIT_PANE;
        }
    }

    protected boolean continuousLayout;
    protected int dividerSize;
    protected int lastDividerLocation;
    protected boolean oneTouchExpandable;
    protected int orientation;

    protected Component leftComponent;
    protected Component rightComponent;

    private static final String UI_CLASS_ID = "SplitPaneUI";
    private double resizeWeight;
    private Component divider;
    private int dividerLocation = -1;

    public JSplitPane() {
        this(HORIZONTAL_SPLIT, false, new JButton("left button"),
             new JButton("right button"));
    }

    public JSplitPane(final int orientation) {
        this(orientation, false, null, null);
    }

    public JSplitPane(final int orientation, final boolean continuousLayout) {
        this(orientation, continuousLayout, null, null);
    }

    public JSplitPane(final int orientation,
                      final Component leftComponent,
                      final Component rightComponent) {

        this(orientation, false, leftComponent, rightComponent);
    }

    public JSplitPane(final int orientation,
                      final boolean continuesLayout,
                      final Component leftComponent,
                      final Component rightComponent) {

        checkOrientation(orientation);
        this.orientation = orientation;
        this.continuousLayout = continuesLayout;

        setLayout(null);

        setLeftComponent(leftComponent);
        setRightComponent(rightComponent);

        updateUI();
    }

    public void setUI(final SplitPaneUI ui) {
        super.setUI(ui);
    }

    public SplitPaneUI getUI() {
        return (SplitPaneUI)ui;
    }

    public void updateUI() {
        setUI((SplitPaneUI)UIManager.getUI(this));
    }

    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public void setDividerSize(final int size) {
        if (dividerSize != size) {
            int oldValue = dividerSize;
            dividerSize = size;
            firePropertyChange(DIVIDER_SIZE_PROPERTY, oldValue, size);
        }
    }

    public int getDividerSize() {
        return dividerSize;
    }

    public void setLeftComponent(final Component c) {
        add(c, LEFT);
    }

    public Component getLeftComponent() {
        return leftComponent;
    }

    public void setTopComponent(final Component c) {
        add(c, TOP);
    }

    public Component getTopComponent() {
        return leftComponent;
    }

    public void setRightComponent(final Component c) {
        add(c, RIGHT);
    }

    public Component getRightComponent() {
        return rightComponent;
    }

    public void setBottomComponent(final Component c) {
        add(c, BOTTOM);
    }

    public Component getBottomComponent() {
        return rightComponent;
    }

    public void setOneTouchExpandable(final boolean expandable) {
        if (oneTouchExpandable != expandable) {
            oneTouchExpandable = expandable;
            firePropertyChange(ONE_TOUCH_EXPANDABLE_PROPERTY, !expandable, expandable);
        }
    }

    public boolean isOneTouchExpandable() {
        return oneTouchExpandable;
    }

    public void setLastDividerLocation(final int lastLocation) {
        if (lastDividerLocation != lastLocation) {
            int oldValue = lastDividerLocation;
            lastDividerLocation = lastLocation;
            firePropertyChange(LAST_DIVIDER_LOCATION_PROPERTY, oldValue, lastLocation);
        }
    }

    public int getLastDividerLocation() {
        return lastDividerLocation;
    }

    public void setOrientation(final int orientation) {
        checkOrientation(orientation);
        if (this.orientation != orientation) {
            int oldValue = this.orientation;
            this.orientation = orientation;
            firePropertyChange(ORIENTATION_PROPERTY, oldValue, orientation);
        }
    }

    public int getOrientation() {
        return orientation;
    }

    public void setContinuousLayout(final boolean continuousLayout) {
        if (this.continuousLayout != continuousLayout) {
            this.continuousLayout = continuousLayout;
            firePropertyChange(CONTINUOUS_LAYOUT_PROPERTY, !continuousLayout, continuousLayout);
        }
    }

    public boolean isContinuousLayout() {
        return continuousLayout;
    }

    public void setResizeWeight(final double weight) {
        if (weight < 0 || weight > 1) {
            throw new IllegalArgumentException(Messages.getString("swing.32")); //$NON-NLS-1$
        }
        if (resizeWeight != weight) {
            double oldValue = resizeWeight;
            resizeWeight = weight;
            firePropertyChange(RESIZE_WEIGHT_PROPERTY, oldValue, weight);
        }
    }

    public double getResizeWeight() {
        return resizeWeight;
    }

    public void resetToPreferredSizes() {
        getUI().resetToPreferredSizes(this);
    }

    public void setDividerLocation(final double proportionalLocation) {
        if (proportionalLocation < 0 || proportionalLocation > 1) {
            throw new IllegalArgumentException(Messages.getString("swing.33")); //$NON-NLS-1$
        }

        int size;
        if (orientation == HORIZONTAL_SPLIT) {
            size = getWidth();
        } else {
            size = getHeight();
        }
        int location = (int)((size - dividerSize) * proportionalLocation);
        setDividerLocation(location);
    }

    public void setDividerLocation(final int location) {
        int oldValue = dividerLocation;
        if (oldValue != location) {
            dividerLocation = location;
            getUI().setDividerLocation(this, location);
            firePropertyChange(DIVIDER_LOCATION_PROPERTY, oldValue, location);
        }
        setLastDividerLocation(oldValue);
    }

    public int getDividerLocation() {
        return dividerLocation;
    }

    public int getMinimumDividerLocation() {
        return getUI().getMinimumDividerLocation(this);
    }

    public int getMaximumDividerLocation() {
        return getUI().getMaximumDividerLocation(this);
    }

    public void remove(final Component component) {
        clearComponentField(component);
        super.remove(component);
    }

    public void remove(final int index) {
        Component component = getComponent(index);
        clearComponentField(component);
        super.remove(index);
    }

    public void removeAll() {
        leftComponent = null;
        rightComponent = null;
        divider = null;
        super.removeAll();
    }

    public boolean isValidateRoot() {
        return true;
    }

    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJSplitPane();
        }

        return accessibleContext;
    }


    protected void addImpl(final Component c, final Object constraints, final int index) {
        Object updatedConstraints = constraints;
        Component oldComponent = null;
        if (LEFT.equals(constraints) || TOP.equals(constraints)) {
            oldComponent = leftComponent;
            leftComponent = c;
        } else if (RIGHT.equals(constraints) || BOTTOM.equals(constraints)) {
            oldComponent = rightComponent;
            rightComponent = c;
        } else if (DIVIDER.equals(constraints)) {
            oldComponent = divider;
            divider = c;
        } else {
            if (leftComponent == null) {
                leftComponent = c;
                updatedConstraints = LEFT;
            } else if (rightComponent == null) {
                rightComponent = c;
                updatedConstraints = RIGHT;
            } else {
                updatedConstraints = null;
            }
        }

        if (oldComponent != null) {
            super.remove(oldComponent);
        }
        if (c != null) {
            super.addImpl(c, updatedConstraints, -1);
        }
    }

    protected void paintChildren(final Graphics g) {
        super.paintChildren(g);
        getUI().finishedPaintingChildren(this, g);
    }


    private void clearComponentField(final Component component) {
        if (leftComponent == component) {
            leftComponent = null;
        } else if (rightComponent == component) {
            rightComponent = null;
        } else if (divider == component) {
            divider = null;
        }
    }

    private void checkOrientation(final int orientation) {
        if (orientation != HORIZONTAL_SPLIT && orientation != VERTICAL_SPLIT) {
            throw new IllegalArgumentException(Messages.getString("swing.1B")); //$NON-NLS-1$
        }
    }


}
