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

import java.io.Serializable;

import org.apache.harmony.x.swing.SizeRequirementsHelper;


public class SizeRequirements implements Serializable {

    private static final int[] EMPTY_ARRAY = new int[0];

    public int minimum;
    public int preferred;
    public int maximum;
    public float alignment;

    public SizeRequirements(final int min, final int pref,
                            final int max, final float a) {
        minimum   = min;
        preferred = pref;
        maximum   = max;
        alignment = a;
    }

    public SizeRequirements() {
        this(0, 0, 0, 0.5f);
    }

    SizeRequirements(final float a) {
        this(0, 0, 0, a);
    }

    public String toString() {
        return "[" + minimum + "," + preferred + "," + maximum + "]@"
               + alignment;
    }

    public static SizeRequirements
        getTiledSizeRequirements(final SizeRequirements[] children) {

        return SizeRequirementsHelper.getTiledSizeRequirements(children, null);
    }

    public static SizeRequirements
        getAlignedSizeRequirements(final SizeRequirements[] children) {

        return SizeRequirementsHelper.getAlignedSizeRequirements(children,
                                                                 null, false);
    }

    public static void
        calculateTiledPositions(final int allocated,
                                final SizeRequirements total,
                                final SizeRequirements[] children,
                                final int[] offsets,
                                final int[] spans,
                                final boolean normal) {

        SizeRequirements totalRequirements = SizeRequirementsHelper
                .getTiledSizeRequirements(children, total);

        SizeRequirementsHelper.calculateTiledPositions(allocated,
                                                       totalRequirements,
                                                       children,
                                                       offsets,
                                                       spans,
                                                       normal);
    }

    public static void
        calculateAlignedPositions(final int allocated,
                                  final SizeRequirements total,
                                  final SizeRequirements[] children,
                                  final int[] offsets,
                                  final int[] spans,
                                  final boolean normal) {

        SizeRequirementsHelper.calculateAlignedPositions(allocated,
                                                         total,
                                                         children,
                                                         offsets,
                                                         spans,
                                                         normal);
    }

    public static void
        calculateTiledPositions(final int allocated,
                                final SizeRequirements total,
                                final SizeRequirements[] children,
                                final int[] offsets,
                                final int[] spans) {
        calculateTiledPositions(allocated, total, children,
                                offsets, spans, true);
    }

    public static void
        calculateAlignedPositions(final int allocated,
                                  final SizeRequirements total,
                                  final SizeRequirements[] children,
                                  final int[] offsets,
                                  final int[] spans) {

        calculateAlignedPositions(allocated, total, children,
                                  offsets, spans, true);
    }

    public static int[] adjustSizes(final int delta,
                                    final SizeRequirements[] children) {
        return EMPTY_ARRAY;
    }

    void set(final int min, final int pref,
             final int max, final float a) {
        minimum   = min;
        preferred = pref;
        maximum   = max;
        alignment = a;
    }

    void reset() {
        minimum   = 0;
        preferred = 0;
        maximum   = 0;
        alignment = 0.5f;
    }

}

