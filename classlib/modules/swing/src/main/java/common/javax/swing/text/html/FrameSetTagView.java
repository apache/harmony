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
 * @author Vadim L. Bogdanov
 */
package javax.swing.text.html;

import javax.swing.text.BoxView;
import javax.swing.text.Element;
import javax.swing.text.View;

class FrameSetTagView extends BoxView {

    public FrameSetTagView(final Element elem) {
        super(elem, calculateMajorAxis(elem));
    }

    protected void layoutMajorAxis(final int targetSpan, final int axis,
                                   final int[] offsets, final int[] spans) {
        if (!hasNoFramesView()) {
            layoutAxisImpl(targetSpan, axis, offsets, spans);
            return;
        }

        View noFramesView = getView(getViewCount() - 1);
        int noFramesSpan = (int)noFramesView.getMinimumSpan(axis);
        int newTargetSpan = targetSpan - noFramesSpan;

        layoutAxisImpl(newTargetSpan, axis, offsets, spans);

        offsets[getViewCount() - 1] = newTargetSpan;
        spans[getViewCount() - 1] = noFramesSpan;
    }

    protected void layoutMinorAxis(final int targetSpan, final int axis,
                                   final int[] offsets, final int[] spans) {
        layoutAxisImpl(targetSpan, axis, offsets, spans);

        if (hasNoFramesView()) {
            offsets[getViewCount() - 1] = 0;
            spans[getViewCount() - 1] = targetSpan;
        }
    }

    private void layoutAxisImpl(final int targetSpan, final int axis,
                                   final int[] offsets, final int[] spans) {
        Object attr;
        if (axis == X_AXIS) {
            attr = HTML.Attribute.COLS;
        } else {
            attr = HTML.Attribute.ROWS;
        }

        String[] strLengths = parseLengths(
            (String)getAttributes().getAttribute(attr));
        int lineLength = strLengths.length;

        int[] lengths = calculateLengths(strLengths, targetSpan);

        int x = 0;
        int y = 0;
        int curOffset = 0;
        int viewsToLayout = hasNoFramesView() ? getViewCount() - 1 : getViewCount();
        for (int i = 0; i < viewsToLayout; i++) {
            offsets[i] = curOffset;
            spans[i] = lengths[x];

            if (axis == X_AXIS) {
                curOffset += spans[i];
                x++;
                if (x >= lineLength) {
                    x = 0;
                    y++;
                    curOffset = 0;
                }
            } else {
                y++;
                if (y >= offsets.length / lineLength) {
                    x++;
                    y = 0;
                    curOffset += spans[i];
                }
            }
        }
    }

    private int[] calculateLengths(final String[] strLengths,
                                   final int targetLength) {
        int[] lengths = new int[strLengths.length];

        int remainingLength = targetLength;
        int starsCount = 0;
        for (int i = 0; i < lengths.length; i++) {
            String strLen = strLengths[i];
            if (strLen.endsWith("%")) {
                lengths[i] = targetLength * cutTailAndParseInt(strLen) / 100;
            } else if (strLen.endsWith("*")) {
                // we'll just count '*' during this pass
                lengths[i] = 0;
                starsCount += cutTailAndParseInt(strLen);
            } else {
                lengths[i] = Integer.parseInt(strLen);
            }
            remainingLength -= lengths[i];
        }

        // allocate space for '*' frames
        int starsLength = Math.max(remainingLength, 0);
        if (starsCount > 0) {
            for (int i = 0; i < lengths.length; i++) {
                String strLen = strLengths[i];
                if (strLen.endsWith("*")) {
                    lengths[i] = starsLength
                        * cutTailAndParseInt(strLen) / starsCount;
                    remainingLength -= lengths[i];
                }
            }
        }

        normalizeLengths(lengths, targetLength, targetLength - remainingLength);
        return lengths;
    }

    private int cutTailAndParseInt(final String str) {
        if ("*".equals(str)) {
            return 1;
        }
        return Integer.parseInt(str.substring(0, str.length() - 1));
    }

    private void normalizeLengths(final int[] lengths, final int targetLength,
                                  final int allocatedLength) {
        if (targetLength == allocatedLength || allocatedLength == 0) {
            return;
        } else if (Math.abs(targetLength - allocatedLength) < lengths.length) {
            lengths[lengths.length - 1] += targetLength - allocatedLength;
            return;
        }

        int usedLength = 0;
        for (int i = 0; i < lengths.length; i++) {
            lengths[i] = lengths[i] * targetLength / allocatedLength;
            usedLength += lengths[i];
        }

        lengths[lengths.length - 1] += targetLength - usedLength;
    }

    private static String[] parseLengths(final String lengths) {
        if (lengths == null) {
            return new String[] {"100%"};
        }

        return lengths.split(", *");
    }

    private static int calculateMajorAxis(final Element elem) {
        return elem.getAttributes().isDefined(HTML.Attribute.ROWS) ? Y_AXIS : X_AXIS;
    }

    private boolean hasNoFramesView() {
        if (getViewCount() == 0) {
            return false;
        }
        return getView(getViewCount() - 1) instanceof NoFramesTagView;
    }
}
