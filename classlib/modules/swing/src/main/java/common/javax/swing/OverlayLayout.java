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
 * @author Alexander T. Simbirtsev
 */
package javax.swing;

import java.awt.AWTError;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.io.Serializable;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class OverlayLayout implements LayoutManager2, Serializable {

    private final Container target;
    transient private final LayoutParameters layoutParams;

    public OverlayLayout(final Container target) {
        this.target = target;
        layoutParams = new LayoutParameters(target, LayoutParameters.MIXED_ALIGNMENT);
    }

    public void addLayoutComponent(final Component component, final Object constraints) {
        invalidateLayout(target);
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
        invalidateLayout(target);
    }

    public void invalidateLayout(final Container target) {
        checkTarget(target);
        layoutParams.invalidate();
    }

    public float getLayoutAlignmentY(final Container target) {
        checkTarget(target);
        layoutParams.calculateLayoutParameters();

        return layoutParams.alignmentY;
    }

    public float getLayoutAlignmentX(final Container target) {
        checkTarget(target);
        layoutParams.calculateLayoutParameters();

        return layoutParams.alignmentX;
    }

    public void removeLayoutComponent(final Component component) {
        invalidateLayout(target);
    }

    public void layoutContainer(final Container target) {
        checkTarget(target);
        layoutParams.layoutTarget();
    }

    /**
     *  Checks if we want to deal with the same target that was mentioned in constructor
     *
     * @param target
     */
    private void checkTarget(final Container target) {
        if (this.target != target) {
            throw new AWTError(Messages.getString("swing.err.03")); //$NON-NLS-1$
        }
    }

}


