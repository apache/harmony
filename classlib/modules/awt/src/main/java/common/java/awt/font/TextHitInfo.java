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
/*
 * @author Oleg V. Khaschansky
 */

package java.awt.font;

import org.apache.harmony.misc.HashCode;


public final class TextHitInfo {
    private int charIdx; // Represents character index in the line
    private boolean isTrailing;

    private TextHitInfo(int idx, boolean isTrailing) {
        charIdx = idx;
        this.isTrailing = isTrailing;
    }

    @Override
    public String toString() {
        return new String(
                "TextHitInfo[" + charIdx + ", " + //$NON-NLS-1$ //$NON-NLS-2$
                (isTrailing?"Trailing":"Leading") + "]" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TextHitInfo) {
            return equals((TextHitInfo) obj);
        }
        return false;
    }

    public boolean equals(TextHitInfo thi) {
        return
                thi != null &&
                thi.charIdx == charIdx &&
                thi.isTrailing == isTrailing;
    }

    public TextHitInfo getOffsetHit(int offset) {
        return new TextHitInfo(charIdx + offset, isTrailing);
    }

    public TextHitInfo getOtherHit() {
        return isTrailing ?
                new TextHitInfo(charIdx+1, false) :
                new TextHitInfo(charIdx-1, true);
    }

    public boolean isLeadingEdge() {
        return !isTrailing;
    }

    @Override
    public int hashCode() {
        return HashCode.combine(charIdx, isTrailing);
    }

    public int getInsertionIndex() {
        return isTrailing ? charIdx+1 : charIdx;
    }

    public int getCharIndex() {
        return charIdx;
    }

    public static TextHitInfo trailing(int charIndex) {
        return new TextHitInfo(charIndex, true);
    }

    public static TextHitInfo leading(int charIndex) {
        return new TextHitInfo(charIndex, false);
    }

    public static TextHitInfo beforeOffset(int offset) {
        return new TextHitInfo(offset-1, true);
    }

    public static TextHitInfo afterOffset(int offset) {
        return new TextHitInfo(offset, false);
    }
}
