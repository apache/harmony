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
package javax.swing.text;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.text.Position.Bias;

import org.apache.harmony.x.swing.SizeRequirementsHelper;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class BoxView extends CompositeView {
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final SizeRequirements[] EMPTY_REQUIREMENTS_ARRAY =
                                                    new SizeRequirements[0];

    private int boxHeight;
    private int boxWidth;

    private int majorAxis;

    private boolean majorLayoutValid;
    private int[] majorOffsets = EMPTY_INT_ARRAY;
    private SizeRequirements[] majorRequirements = EMPTY_REQUIREMENTS_ARRAY;
    private boolean majorRequirementsValid;
    private int[] majorSpans = EMPTY_INT_ARRAY;
    private final SizeRequirements majorTotalRequirements =
                                        new SizeRequirements();
    private boolean majorTotalRequirementsValid;

    private boolean minorLayoutValid;
    private int[] minorOffsets = EMPTY_INT_ARRAY;
    private SizeRequirements[] minorRequirements = EMPTY_REQUIREMENTS_ARRAY;
    private boolean minorRequirementsValid;
    private int[] minorSpans = EMPTY_INT_ARRAY;
    private final SizeRequirements minorTotalRequirements =
                                        new SizeRequirements();
    private boolean minorTotalRequirementsValid;

    public BoxView(final Element element, final int axis) {
        super(element);
        majorAxis = axis;
    }

    @Override
    public float getAlignment(final int axis) {
        isAxisValid(axis);

        return getTotalRequirements(axis).alignment;
    }

    public int getAxis() {
        return majorAxis;
    }

    @Override
    public Shape getChildAllocation(final int index, final Shape shape) {
        if (shape == null || !isLayoutValid()) {
            return null;
        }
        return super.getChildAllocation(index, shape);
    }

    @Override
    public float getMinimumSpan(final int axis) {
        isAxisValid(axis);

        return getTotalRequirements(axis).minimum + getSideInset(axis);
    }

    @Override
    public float getPreferredSpan(final int axis)  {
        isAxisValid(axis);

        return getTotalRequirements(axis).preferred + getSideInset(axis);
    }

    @Override
    public float getMaximumSpan(final int axis) {
        isAxisValid(axis);

        return Utilities.safeIntSum(getTotalRequirements(axis).maximum,
                                    getSideInset(axis));
    }

    @Override
    public int getResizeWeight(final int axis) {
        isAxisValid(axis);

        final SizeRequirements sr = getTotalRequirements(axis);
        return sr.minimum == sr.maximum ? 0 : 1;
    }

    public void layoutChanged(final int axis) {
        if (isMajor(axis)) {
            majorLayoutValid = false;
        } else {
            minorLayoutValid = false;
        }
    }

    @Override
    public void paint(final Graphics g, final Shape shape) {
        final Rectangle insideAlloc = getInsideAllocation(shape);
        final Rectangle allocation = new Rectangle();
        final Rectangle clipBounds = g.getClipBounds();

        for (int i = 0; i < getViewCount(); i++) {
            allocation.setBounds(insideAlloc);
            childAllocation(i, allocation);
            if (allocation.intersects(clipBounds)) {
                paintChild(g, allocation, i);
            }
        }
    }

    @Override
    public void preferenceChanged(final View child,
                                  final boolean width,
                                  final boolean height) {
        invalidateLayout(width, height);

        super.preferenceChanged(child, width, height);
    }

    @Override
    public void replace(final int index, final int length, final View[] elems) {
        super.replace(index, length, elems);

        int toAdd = elems != null ? elems.length : 0;
        minorOffsets = resizeArray(minorOffsets, index, length, toAdd);
        majorOffsets = resizeArray(majorOffsets, index, length, toAdd);
        minorSpans   = resizeArray(minorSpans, index, length, toAdd);
        majorSpans   = resizeArray(majorSpans, index, length, toAdd);

        minorRequirements = resizeArray(minorRequirements, index,
                                        length, toAdd);
        majorRequirements = resizeArray(majorRequirements, index,
                                        length, toAdd);

        invalidateLayout(true, true);
    }

    public void setAxis(final int axis) {
        majorAxis = axis;
        invalidateLayout(true, true);
    }

    @Override
    public void setSize(final float width, final float height) {
        layout((int)(width - getSideInset(X_AXIS)),
               (int)(height - getSideInset(Y_AXIS)));
    }

    public int getWidth() {
        return boxWidth;
    }

    public int getHeight() {
        return boxHeight;
    }

    @Override
    public Shape modelToView(final int pos, final Shape shape,
                             final Bias bias) throws BadLocationException {
        final Rectangle bounds = shape.getBounds();
        setSize(bounds.width, bounds.height);
        return super.modelToView(pos, shape, bias);
    }

    @Override
    public int viewToModel(final float x, final float y,
                           final Shape shape, final Bias[] bias) {
        final Rectangle bounds = shape.getBounds();
        setSize(bounds.width, bounds.height);
        return super.viewToModel(x, y, shape, bias);
    }

    protected void baselineLayout(final int targetSpan,
                                  final int axis,
                                  final int[] offsets,
                                  final int[] spans) {
        SizeRequirements[] srs = getRequirements(axis);
        SizeRequirementsHelper.calculateAlignedPositions(targetSpan,
                getTotalRequirements(axis), srs, offsets, spans);
    }

    protected SizeRequirements baselineRequirements(final int axis,
                                                    final SizeRequirements r) {
        final SizeRequirements[] sr = getRequirements(axis);
        return SizeRequirementsHelper.getAlignedSizeRequirements(sr, r, true);
    }

    protected SizeRequirements calculateMajorAxisRequirements(final int axis,
            final SizeRequirements r) {
        final SizeRequirements[] sr = getRequirements(axis);
        return SizeRequirementsHelper.getTiledSizeRequirements(sr, r);
    }

    protected SizeRequirements calculateMinorAxisRequirements(final int axis,
            final SizeRequirements r)  {
        final SizeRequirements result = r != null ? r : new SizeRequirements();
        final SizeRequirements[] children = getRequirements(axis);

        int min  = 0;
        int pref = 0;

        final int count = getViewCount();
        for (int i = 0; i < count; i++) {
            min  = Math.max(min, children[i].minimum);
            pref = Math.max(pref, children[i].preferred);
        }

        result.minimum   = min;
        result.preferred = pref;
        result.maximum   = Integer.MAX_VALUE;
        result.alignment = ALIGN_CENTER;

        return result;
    }

    @Override
    protected void childAllocation(final int index, final Rectangle alloc) {
        if (isLayoutValid()) {
            alloc.x += getOffset(X_AXIS, index);
            alloc.y += getOffset(Y_AXIS, index);
            alloc.width  = getSpan(X_AXIS, index);
            alloc.height = getSpan(Y_AXIS, index);
        } else {
            alloc.width  = 0;
            alloc.height = 0;
        }
    }

    @Override
    protected boolean flipEastAndWestAtEnds(final int position,
                                            final Bias bias) {
        if (isMajor(X_AXIS)) {
            return false;
        }

        View child = getView(getViewIndexAtPosition(bias == Bias.Backward
                                                    ? position - 1
                                                    : position));
        return child instanceof CompositeView
               && ((CompositeView)child).flipEastAndWestAtEnds(position, bias);
    }

    @Override
    protected void forwardUpdate(final ElementChange change,
                                 final DocumentEvent event,
                                 final Shape shape,
                                 final ViewFactory factory) {
        boolean allocValid = isLayoutValid(majorAxis);

        super.forwardUpdate(change, event, shape, factory);

        if (allocValid && !isLayoutValid(majorAxis)) {
            Component component = getComponent();
            if (component != null) {
                int index = getViewIndexAtPosition(event.getOffset());

                Rectangle rect = getInsideAllocation(shape);
                int viewOffset = getOffset(majorAxis, index);
                if (majorAxis == Y_AXIS) {
                    rect.y      += viewOffset;
                    rect.height -= viewOffset;
                } else {
                    rect.x      += viewOffset;
                    rect.width  -= viewOffset;
                }
                component.repaint(rect.x, rect.y, rect.width, rect.height);
            }
        }
    }

    protected int getOffset(final int axis, final int childIndex) {
        return isMajor(axis) ? majorOffsets[childIndex]
                             : minorOffsets[childIndex];
    }

    protected int getSpan(final int axis, final int childIndex) {
        return isMajor(axis) ? majorSpans[childIndex]
                             : minorSpans[childIndex];
    }

    @Override
    protected View getViewAtPoint(final int x, final int y,
                                  final Rectangle alloc) {
        final int location = isMajor(Y_AXIS) ? y - alloc.y : x - alloc.x;

        if (location < 0) {
            return getViewWithAllocation(0, alloc);
        }

        final int lastIndex = getViewCount() - 1;
        if (location >= getOffset(majorAxis, lastIndex)) {
            return getViewWithAllocation(lastIndex, alloc);
        }

        for (int i = 0; i <= lastIndex; i++) {
            if (getOffset(majorAxis, i) <= location
                && location < getOffset(majorAxis, i) + getSpan(majorAxis, i)) {

                return getViewWithAllocation(i, alloc);
            }
        }
        return null;
    }

    @Override
    protected boolean isBefore(final int x, final int y,
                               final Rectangle innerAlloc) {
        return isMajor(X_AXIS) ? x < innerAlloc.x : y < innerAlloc.y;
    }

    @Override
    protected boolean isAfter(final int x, final int y,
                              final Rectangle innerAlloc) {
        return isMajor(X_AXIS) ? x > (innerAlloc.x + innerAlloc.width)
                               : y > (innerAlloc.y + innerAlloc.height);
    }

    protected boolean isAllocationValid() {
        return isLayoutValid();
    }

    protected boolean isLayoutValid(final int axis) {
        return isMajor(axis) ? majorLayoutValid : minorLayoutValid;
    }

    /**
     * This method may cause stack overflow if upon each layout try a child
     * changes its preferences, i.e. <code>preferenceChanged</code> is called.
     */
    protected void layout(final int width, final int height) {
        final boolean layoutX = !isLayoutValid(X_AXIS) || width != boxWidth;
        final boolean layoutY = !isLayoutValid(Y_AXIS) || height != boxHeight;

        boxWidth  = width;
        boxHeight = height;

        final boolean isMajorX = isMajor(X_AXIS);

        if (layoutX) {
            if (isMajorX) {
                layoutMajorAxis(width,  X_AXIS, majorOffsets, majorSpans);
            } else {
                layoutMinorAxis(width, X_AXIS, minorOffsets, minorSpans);
            }
        }

        if (layoutY) {
            if (!isMajorX) {
                layoutMajorAxis(height,  Y_AXIS, majorOffsets, majorSpans);
            } else {
                layoutMinorAxis(height, Y_AXIS, minorOffsets, minorSpans);
            }
        }

        majorLayoutValid = true;
        minorLayoutValid = true;

        if (layoutX || layoutY) {
            for (int i = 0; i < getViewCount(); i++) {
                getView(i).setSize(getSpan(X_AXIS, i), getSpan(Y_AXIS, i));
            }
        }

        // The following may cause stack overflow
        if (!isLayoutValid()) {
            layout(width, height);
        }
    }

    protected void layoutMajorAxis(final int targetSpan, final int axis,
                                   final int[] offsets, final int[] spans) {
        SizeRequirementsHelper.calculateTiledPositions(
                targetSpan,
                getTotalRequirements(axis), majorRequirements,
                offsets, spans, true);
    }

    protected void layoutMinorAxis(final int targetSpan, final int axis,
                                   final int[] offsets, final int[] spans) {
        final SizeRequirements[] sr = getRequirements(axis);

        final int count = getViewCount();
        for (int i = 0; i < count; i++) {
            if (targetSpan >= sr[i].maximum) {
                spans[i] = sr[i].maximum;
            } else if (targetSpan >= sr[i].minimum) {
                spans[i] = targetSpan;
            } else {
                spans[i] = sr[i].minimum;
            }

            offsets[i] = (int)(sr[i].alignment * (targetSpan - spans[i]));
            if (offsets[i] < 0) {
                offsets[i] = 0;
            }
        }

    }

    protected void paintChild(final Graphics g,
                              final Rectangle alloc,
                              final int index) {
        getView(index).paint(g, alloc);
    }

    private void fillRequirements(final SizeRequirements[] requirements,
                                  final int axis) {
        for (int i = 0; i < getViewCount(); i++) {
            View child = getView(i);

            if (requirements[i] == null) {
                requirements[i] = new SizeRequirements();
            }

            requirements[i].minimum   = (int)child.getMinimumSpan(axis);
            requirements[i].preferred = (int)child.getPreferredSpan(axis);
            requirements[i].maximum   = (int)child.getMaximumSpan(axis);
            requirements[i].alignment = child.getAlignment(axis);
        }
    }

    private SizeRequirements[] getRequirements(final int axis) {
        if (isMajor(axis)) {
            if (!majorRequirementsValid) {
                fillRequirements(majorRequirements, axis);
                majorRequirementsValid = true;
            }
            return majorRequirements;
        } else {
            if (!minorRequirementsValid) {
                fillRequirements(minorRequirements, axis);
                minorRequirementsValid = true;
            }
            return minorRequirements;
        }
    }

    private SizeRequirements getTotalRequirements(final int axis) {
        if (isMajor(axis)) {
            if (!majorTotalRequirementsValid) {
                calculateMajorAxisRequirements(axis, majorTotalRequirements);
                majorTotalRequirementsValid = true;
            }
            return majorTotalRequirements;
        } else {
            if (!minorTotalRequirementsValid) {
                calculateMinorAxisRequirements(axis, minorTotalRequirements);
                minorTotalRequirementsValid = true;
            }
            return minorTotalRequirements;
        }
    }

    private View getViewWithAllocation(final int index, final Rectangle alloc) {
        childAllocation(index, alloc);
        return getView(index);
    }

    private void isAxisValid(final int axis) {
        if (axis != X_AXIS && axis != Y_AXIS) {
            throw new IllegalArgumentException(Messages.getString("swing.81")); //$NON-NLS-1$
        }
    }

    private boolean isMajor(final int axis) {
        return majorAxis == axis;
    }

    private boolean isLayoutValid() {
        return isLayoutValid(X_AXIS) && isLayoutValid(Y_AXIS);
    }

    private void invalidateLayout(final boolean width, final boolean height) {
        final boolean majorX = isMajor(X_AXIS);

        if ((majorX && width) || (!majorX && height)) {
            majorLayoutValid            = false;
            majorRequirementsValid      = false;
            majorTotalRequirementsValid = false;
        }

        if ((majorX && height) || (!majorX && width)) {
            minorLayoutValid            = false;
            minorRequirementsValid      = false;
            minorTotalRequirementsValid = false;
        }
    }

    /**
     * Resizes an array of <code>int</code>s.
     *
     * @param array the original array
     * @param index index where elements will be removed and added
     * @param toRemove the number of items to remove
     * @param toAdd the number of items to add
     * @return array with the new size,
     *         may be the same as <code>array</code> parameter
     */
    private int[] resizeArray(final int[] array,
                              final int index,
                              final int toRemove,
                              final int toAdd) {
        if (toRemove == toAdd) {
            return array;
        }

        int size = array.length - toRemove + toAdd;
        if (size == 0) {
            return EMPTY_INT_ARRAY;
        }

        int[] result = new int[size];
        System.arraycopy(array, 0, result, 0, index);
        System.arraycopy(array, index + toRemove, result, index + toAdd,
                         array.length - (index + toRemove));
        return result;
    }

    /**
     * Resizes an array of size requirements.
     * <small>(Probably may be optimized to not copy old array contents)</small>
     *
     * @param array the array to resize
     * @param index index where remove and add operations occur
     * @param toRemove the number of items to remove
     * @param toAdd the number of items to add
     * @return array with the new size
     */
    private SizeRequirements[] resizeArray(final SizeRequirements[] array,
                                           final int index,
                                           final int toRemove,
                                           final int toAdd) {
         if (toRemove == toAdd) {
             return array;
         }

         int size = array.length - toRemove + toAdd;
         if (size == 0) {
             return EMPTY_REQUIREMENTS_ARRAY;
         }

         SizeRequirements[] newArray = new SizeRequirements[size];
         System.arraycopy(array, 0, newArray, 0, index);
         System.arraycopy(array, index + toRemove, newArray, index + toAdd,
                          array.length - (index + toRemove));
         return newArray;
     }

    private int getSideInset(final int axis) {
        if (axis == X_AXIS) {
            return getLeftInset() + getRightInset();
        }
        return getTopInset() + getBottomInset();
    }
}
