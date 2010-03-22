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

package javax.swing;

import java.awt.AWTError;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.io.Serializable;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class BoxLayout implements LayoutManager2, Serializable {
    private static final long serialVersionUID = -2474455742719112368L;

    public static final int X_AXIS = 0;

    public static final int Y_AXIS = 1;

    public static final int LINE_AXIS = 2;

    public static final int PAGE_AXIS = 3;

    private final Container target;

    private final transient LayoutParameters layoutParams;

    public BoxLayout(final Container target, final int axis) {
        super();
        int alignment = axisToAlignment(target, axis);
        this.target = target;
        layoutParams = new LayoutParameters(target, alignment);
    }

    public void addLayoutComponent(final Component component, final Object constraints) {
        // Specified by LayoutManager2 but is not used
    }

    public Dimension preferredLayoutSize(final Container target) {
        checkTarget(target);
        layoutParams.calculateLayoutParameters();
        return layoutParams.preferredSize;
    }

    public Dimension minimumLayoutSize(final Container target) {
        checkTarget(target);
        layoutParams.calculateLayoutParameters();
        return layoutParams.minimumSize;
    }

    public Dimension maximumLayoutSize(final Container target) {
        checkTarget(target);
        layoutParams.calculateLayoutParameters();
        return layoutParams.maximumSize;
    }

    public void addLayoutComponent(final String name, final Component component) {
        // Specified by LayoutManager but is not used
    }

    public synchronized void invalidateLayout(final Container target) {
        checkTarget(target);
        layoutParams.invalidate();
    }

    public synchronized float getLayoutAlignmentY(final Container target) {
        checkTarget(target);
        layoutParams.calculateLayoutParameters();
        return layoutParams.alignmentY;
    }

    public synchronized float getLayoutAlignmentX(final Container target) {
        checkTarget(target);
        layoutParams.calculateLayoutParameters();
        return layoutParams.alignmentX;
    }

    public void removeLayoutComponent(final Component component) {
        // Specified by LayoutManager but is not used
    }

    public void layoutContainer(final Container target) {
        checkTarget(target);
        layoutParams.layoutTarget();
    }

    void setAxis(final int axis) {
        layoutParams.setAlignment(axisToAlignment(target, axis));
    }

    private int axisToAlignment(final Container target, final int axis) throws AWTError {
        int alignment = LayoutParameters.HORIZONTAL_ALIGNMENT;
        if (axis == X_AXIS) {
            alignment = LayoutParameters.HORIZONTAL_ALIGNMENT;
        } else if (axis == Y_AXIS) {
            alignment = LayoutParameters.VERTICAL_ALIGNMENT;
        } else if (axis == LINE_AXIS) {
            if (target != null) {
                alignment = target.getComponentOrientation().isHorizontal() ? LayoutParameters.HORIZONTAL_ALIGNMENT
                        : LayoutParameters.VERTICAL_ALIGNMENT;
            }
        } else if (axis == PAGE_AXIS) {
            if (target != null) {
                alignment = target.getComponentOrientation().isHorizontal() ? LayoutParameters.VERTICAL_ALIGNMENT
                        : LayoutParameters.HORIZONTAL_ALIGNMENT;
            }
        } else {
            throw new AWTError(Messages.getString("swing.err.04")); //$NON-NLS-1$
        }
        return alignment;
    }

    /**
     *  Checks if we want to deal with the same target that was mentioned in constructor
     *
     * @param target
     */
    private void checkTarget(final Container target) {
        if (this.target != target) {
            throw new AWTError(Messages.getString("swing.err.02")); //$NON-NLS-1$
        }
    }
}
