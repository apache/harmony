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
package org.apache.harmony.x.swing;

import javax.swing.SizeRequirements;

/**
 * Helper object which wraps functionality of javax.swing.SizeRequirements
 * so that it can be easily used by javax.swing.text.BoxView.
 *
 */
public final class SizeRequirementsHelper {
    /**
     * @see javax.swing.SizeRequirements#getTiledSizeRequirements(SizeRequirements[])
     *
     * @param sr SizeRequirements where results to be copied. If sr is null, new object is created
     *
     * @return SizeRequirements sr or newly created object
     */
    public static SizeRequirements
        getTiledSizeRequirements(final SizeRequirements[] children,
                                 final SizeRequirements sr) {

        SizeRequirements result = (sr != null) ? sr : new SizeRequirements();
        result.minimum   = 0;
        result.preferred = 0;
        result.maximum   = 0;
        result.alignment = 0.5f;

        for (int iChild = 0; iChild < children.length; iChild++) {
            result.minimum = Utilities.safeIntSum(children[iChild].minimum, result.minimum);
            result.preferred = Utilities.safeIntSum(children[iChild].preferred, result.preferred);
            result.maximum = Utilities.safeIntSum(children[iChild].maximum, result.maximum);
        }

        return result;
    }

    /**
     * @see javax.swing.SizeRequirements#getAlignedSizeRequirements(SizeRequirements[])
     *
     * @param sr SizeRequirements where results to be copied. If sr is null, new object is created
     * @param alignByPreferred boolean value representing which size (preferred or minimal) should be used for alignment calculation
     *
     * @return SizeRequirements sr or newly created object
     */
    public static SizeRequirements
        getAlignedSizeRequirements(final SizeRequirements[] children,
                                   final SizeRequirements sr,
                                   final boolean alignByPreferred) {

        int minRight = 0;
        int minLeft = 0;
        int prefRight = 0;
        int prefLeft = 0;
        int maxRight = 0;
        int maxLeft = 0;

        for (int iChild = 0; iChild < children.length; iChild++) {
            int alignedMin = (int)(children[iChild].alignment * children[iChild].minimum);
            minLeft = Math.max(minLeft, alignedMin);
            minRight = Math.max(minRight, children[iChild].minimum - alignedMin);

            int alignedPref = (int)(children[iChild].alignment * children[iChild].preferred);
            prefLeft = Math.max(prefLeft, alignedPref);
            prefRight = Math.max(prefRight, children[iChild].preferred - alignedPref);

            int alignedMax = (int)(children[iChild].alignment * children[iChild].maximum);
            maxLeft = Math.max(maxLeft, alignedMax);
            maxRight = Math.max(maxRight, children[iChild].maximum - alignedMax);
        }

        SizeRequirements result = (sr != null) ? sr : new SizeRequirements();
        result.minimum   = Utilities.safeIntSum(minRight,  minLeft);
        result.preferred = Utilities.safeIntSum(prefRight, prefLeft);
        result.maximum   = Utilities.safeIntSum(maxRight,  maxLeft);
        if (alignByPreferred) {
            result.alignment = (result.preferred != 0) ? (float)prefLeft/result.preferred : 0;
        } else {
            result.alignment = (result.minimum != 0) ? (float)minLeft/result.minimum : 0;
        }

        return result;
    }

    /**
     * @see javax.swing.SizeRequirements#calculateTiledPositions(int, SizeRequirements, SizeRequirements[], int[], int[], boolean)
     */
    public static void calculateTiledPositions(final int allocated,
                                final SizeRequirements total,
                                final SizeRequirements[] children,
                                final int[] offsets,
                                final int[] spans,
                                final boolean normal) {

        calculateTiledSpans(allocated, total, children, spans);

        if (normal) {
            int curOffset = 0;
            for (int iChild = 0; iChild < children.length; iChild++) {
                offsets[iChild] = curOffset;
                curOffset += spans[iChild];
            }
        } else {
            int curOffset = allocated;
            for (int iChild = 0; iChild < children.length; iChild++) {
                curOffset -= spans[iChild];
                offsets[iChild] = curOffset;
            }
        }
    }

    /**
     * @see javax.swing.SizeRequirements#calculateAlignedPositions(int, SizeRequirements, SizeRequirements[], int[], int[], boolean)
     */
    public static void calculateAlignedPositions(final int allocated,
                                  final SizeRequirements total,
                                  final SizeRequirements[] children,
                                  final int[] offsets,
                                  final int[] spans,
                                  final boolean normal) {

        final int alignedAllocated = (int)(allocated * total.alignment);
        for (int iChild = 0; iChild < children.length; iChild++) {
            spans[iChild] = children[iChild].maximum;
            offsets[iChild] = alignedAllocated
                - (int)(children[iChild].maximum * children[iChild].alignment);

            if (offsets[iChild] < 0) {
                spans[iChild] = spans[iChild] + offsets[iChild];
                offsets[iChild] = 0;
            }
            if (offsets[iChild] + spans[iChild] > allocated) {
                spans[iChild] = allocated - offsets[iChild];
            }
        }
        if (!normal) {
            for (int iChild = 0; iChild < children.length; iChild++) {
                offsets[iChild] = allocated - offsets[iChild] - spans[iChild];
            }
        }
    }

    /**
     * @see javax.swing.SizeRequirements#calculateAlignedPositions(int, SizeRequirements, SizeRequirements[], int[], int[])
     */
    public static void calculateAlignedPositions(final int allocated,
                              final SizeRequirements total,
                              final SizeRequirements[] children,
                              final int[] offsets,
                              final int[] spans) {

        int baseLineOffset = (int)(allocated * total.alignment);
        int childMinRequirement;

        for (int iChild = 0; iChild < children.length; iChild++) {
            childMinRequirement = children[iChild].minimum;

            if (children[iChild].alignment == 0) {
                offsets[iChild] = baseLineOffset;
                if (allocated - baseLineOffset > childMinRequirement) {
                    spans[iChild] = Math.min(children[iChild].maximum,
                                             allocated - baseLineOffset);
                } else {
                    spans[iChild] = childMinRequirement;
                }
                continue;
            }

            if (children[iChild].alignment == 1) {
                if (baseLineOffset < childMinRequirement) {
                    spans[iChild] = childMinRequirement;
                } else {
                    spans[iChild] = Math.min(children[iChild].maximum,
                                             baseLineOffset);
                }
                offsets[iChild] = baseLineOffset - spans[iChild];
                continue;
            }

            int upperSpan = (int)(baseLineOffset / children[iChild].alignment);
            int bottomSpan = (int)((allocated - baseLineOffset) / (1 - children[iChild].alignment));

            spans[iChild] = childMinRequirement;
            if (childMinRequirement <= upperSpan
                && childMinRequirement <= bottomSpan) {
                spans[iChild] = Math.min(children[iChild].maximum, Math
                    .min(upperSpan, bottomSpan));
            }
            offsets[iChild] = baseLineOffset
                              - (int)(spans[iChild] * children[iChild].alignment);
        }
    }

    private static void calculateTiledSpans(final int allocated,
                                            final SizeRequirements total,
                                            final SizeRequirements[] children,
                                            final int[] spans) {

        if (total.preferred <= allocated) {
            if (total.maximum <= allocated) {
                for (int iChild = 0; iChild < children.length; iChild++) {
                    spans[iChild] = children[iChild].maximum;
                }
            } else {
                int allocatedDelta = allocated - total.preferred;
                long maxDelta = 0;
                for (int iChild = 0; iChild < children.length; iChild++) {
                    maxDelta += children[iChild].maximum - children[iChild].preferred;
                }
                for (int iChild = 0; iChild < children.length; iChild++) {
                    spans[iChild] = children[iChild].preferred
                        + safeIntRatio(allocatedDelta,
                                       children[iChild].maximum
                                       - children[iChild].preferred,
                                       maxDelta);
                }
            }
        } else {
            if (total.minimum < allocated) {
                int allocatedDelta = allocated - total.minimum;
                long prefDelta = 0;
                for (int iChild = 0; iChild < children.length; iChild++) {
                    prefDelta += children[iChild].preferred - children[iChild].minimum;
                }
                for (int iChild = 0; iChild < children.length; iChild++) {
                    spans[iChild] = children[iChild].minimum
                        + safeIntRatio(allocatedDelta,
                                       children[iChild].preferred
                                       - children[iChild].minimum, prefDelta);
                }
            } else {
                for (int iChild = 0; iChild < children.length; iChild++) {
                    spans[iChild] = children[iChild].minimum;
                }
            }
        }
    }

    /**
     * Returns ratio of two integers multiplied by the third one.
     * Avoid "division by zero" problem if the divider is 0.
     * This function prevents wrong results when arguments are close
     * to Integer.MAX_VALUE
     *
     * @param item1 the first multiplier
     * @param item2 the second multiplier
     * @param item3 divisor
     * @return the ratio of (item1 * item2 / item3)
     */
    private static int safeIntRatio(final long item1, final long item2, final long item3) {
        return (item3 != 0) ? (int)((item1 * item2) / item3) : 0;
    }
}
