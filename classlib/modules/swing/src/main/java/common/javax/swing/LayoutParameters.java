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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;

import org.apache.harmony.x.swing.Utilities;


/**
 * This class helps BoxLayout and OverlayLayout to layout their components
 * and calculate all target's necessary parameters. It encapsulates all necessary
 * for calculations data.
 *
 */
final class LayoutParameters {

    public float alignmentX = 0.0f;
    public float alignmentY = 0.0f;
    public final Dimension minimumSize = new Dimension();
    public final Dimension preferredSize = new Dimension();
    public final Dimension maximumSize = new Dimension();

    public final static int MIXED_ALIGNMENT = 0;
    public final static int HORIZONTAL_ALIGNMENT = 1;
    public final static int VERTICAL_ALIGNMENT = 2;

    private SizeRequirements[] horRequirements = null;
    private SizeRequirements[] vertRequirements = null;
    private final SizeRequirements totalRequirements = new SizeRequirements();
    private int[] offsetsX = null;
    private int[] spansX = null;
    private int[] offsetsY = null;
    private int[] spansY = null;

    private boolean isValid = false;
    private final Container target;
    private int alignment;

    /**
     *
     * @param target - container being laid out with this parameters
     * @param alignment - alignment of the target. It can be one of the following values:
     *      <code>MIXED_ALIGNMENT</code> - value for OverlayLayout
     *      <code>HORIZONTAL_ALIGNMENT</code> - value for BoxLayout with horizontal alignment
     *      <code>VERTICAL_ALIGNMENT</code> - value for BoxLayout with vertical alignment
     */
    public LayoutParameters(final Container target, final int alignment) {
        this.target = target;
        this.alignment = alignment;
    }
    /**
     *  This method layouts target:
     *  calculates sizes an positions for all subcomponents
     *
     */
    public void layoutTarget() {
        calculateLayoutParameters();

        int numChildren = target.getComponentCount();
        if (offsetsX == null || offsetsX.length != numChildren) {
            offsetsX = new int[numChildren];
            spansX = new int[numChildren];
            offsetsY = new int[numChildren];
            spansY = new int[numChildren];
        }
        final Insets insets = target.getInsets();
        Rectangle innerSize = Utilities.subtractInsets(target.getBounds(), insets);
        totalRequirements.alignment = alignmentX;
        if (alignment == HORIZONTAL_ALIGNMENT) {
            SizeRequirements.calculateTiledPositions(innerSize.width,
                                                     totalRequirements, horRequirements,
                                                     offsetsX, spansX);
        } else {
            SizeRequirements.calculateAlignedPositions(innerSize.width,
                                                       totalRequirements, horRequirements,
                                                       offsetsX, spansX);
        }
        totalRequirements.alignment = alignmentY;
        if (alignment == VERTICAL_ALIGNMENT) {
            SizeRequirements.calculateTiledPositions(innerSize.height,
                                                     totalRequirements, vertRequirements,
                                                     offsetsY, spansY);
        } else {
            SizeRequirements.calculateAlignedPositions(innerSize.height,
                                                       totalRequirements, vertRequirements,
                                                       offsetsY, spansY);
        }

        final boolean isLTR = target.getComponentOrientation().isLeftToRight();
        final int offsetX = isLTR ? insets.left : target.getWidth() - insets.right;
        final int offsetY = insets.top;
        for (int iChild = 0; iChild < numChildren; iChild++) {
            final Component component = target.getComponent(iChild);
            final int spanX = spansX[iChild] > 0 ? spansX[iChild] : 0;
            final int spanY = spansY[iChild] > 0 ? spansY[iChild] : 0;
            final int x = isLTR ? offsetsX[iChild] : -offsetsX[iChild] - spanX;
            component.setBounds(offsetX + x, offsetY + offsetsY[iChild], spanX, spanY);
        }
    }

    /**
     *  fills arrays with target's subcomponents data
     *  required in futher calculations
     */
    private synchronized void fillRequirementArrays() {
        int numChildren = target.getComponentCount();
        if (horRequirements == null || horRequirements.length != numChildren) {
            horRequirements = new SizeRequirements[numChildren];
            vertRequirements = new SizeRequirements[numChildren];
            for (int iChild = 0; iChild < numChildren; iChild++) {
                horRequirements[iChild] = new SizeRequirements();
                vertRequirements[iChild] = new SizeRequirements();
            }
        }

        for (int iChild = 0; iChild < numChildren; iChild++) {
            Component component = target.getComponent(iChild);
            if (component.isVisible()) {
                Dimension minSize = component.getMinimumSize();
                Dimension prefSize = component.getPreferredSize();
                Dimension maxSize = component.getMaximumSize();
                horRequirements[iChild].set(minSize.width, prefSize.width,
                        maxSize.width, component.getAlignmentX());
                vertRequirements[iChild].set(minSize.height, prefSize.height,
                        maxSize.height, component.getAlignmentY());
            } else {
                horRequirements[iChild].reset();
                vertRequirements[iChild].reset();
            }
        }
    }

    /**
     *  Calculates layout parameters:
     *  maximum, preferred and minimum sizes for container
     */
    public synchronized void calculateLayoutParameters() {
        if (isValid) {
            return;
        }
        fillRequirementArrays();

        Insets insets = target.getInsets();
        int horzInsets = insets.left + insets.right;
        int vertInsets = insets.top + insets.bottom;

        SizeRequirements resultedRequirements = null;
        if (alignment == HORIZONTAL_ALIGNMENT) {
            resultedRequirements = SizeRequirements.getTiledSizeRequirements(horRequirements);
        } else {
            resultedRequirements = SizeRequirements.getAlignedSizeRequirements(horRequirements);
        }
        alignmentX = resultedRequirements.alignment;
        minimumSize.width = Utilities.safeIntSum(resultedRequirements.minimum, horzInsets);
        preferredSize.width = Utilities.safeIntSum(resultedRequirements.preferred, horzInsets);
        maximumSize.width = Utilities.safeIntSum(resultedRequirements.maximum, horzInsets);

        if (alignment == VERTICAL_ALIGNMENT) {
            resultedRequirements = SizeRequirements.getTiledSizeRequirements(vertRequirements);
        } else {
            resultedRequirements = SizeRequirements.getAlignedSizeRequirements(vertRequirements);
        }
        alignmentY = resultedRequirements.alignment;
        minimumSize.height = Utilities.safeIntSum(resultedRequirements.minimum, vertInsets);
        preferredSize.height = Utilities.safeIntSum(resultedRequirements.preferred, vertInsets);
        maximumSize.height = Utilities.safeIntSum(resultedRequirements.maximum, vertInsets);

        isValid = true;
    }

    /**
     *  This method is to be invoked to notify LayoutParameters object, that target was changed somehow
     *  and all cached data became unreliable
     */
    public void invalidate() {
        isValid = false;
    }

    public int getAlignment() {
        return alignment;
    }

    public void setAlignment(final int alignment) {
        if (this.alignment == alignment) {
            return;
        }
        this.alignment = alignment;
        invalidate();
    }
}

