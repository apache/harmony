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
 * @author Ilya S. Okomin
 */
package java.awt.font;

import org.apache.harmony.awt.internal.nls.Messages;

public final class GlyphJustificationInfo {

    public static final int PRIORITY_KASHIDA = 0;

    public static final int PRIORITY_WHITESPACE = 1;

    public static final int PRIORITY_INTERCHAR = 2;

    public static final int PRIORITY_NONE = 3;

    public final boolean growAbsorb;

    public final float growLeftLimit;

    public final float growRightLimit;

    public final int growPriority;

    public final boolean shrinkAbsorb;

    public final float shrinkLeftLimit;

    public final float shrinkRightLimit;

    public final int shrinkPriority;

    public final float weight;

    public GlyphJustificationInfo(float weight, boolean growAbsorb, int growPriority,
            float growLeftLimit, float growRightLimit, boolean shrinkAbsorb,
            int shrinkPriority, float shrinkLeftLimit, float shrinkRightLimit) {

        if (weight < 0) {
            // awt.19C=weight must be a positive number
            throw new IllegalArgumentException(Messages.getString("awt.19C")); //$NON-NLS-1$
        }
        this.weight = weight;

        if (growLeftLimit < 0) {
            // awt.19D=growLeftLimit must be a positive number
            throw new IllegalArgumentException(Messages.getString("awt.19D")); //$NON-NLS-1$
        }
        this.growLeftLimit = growLeftLimit;

        if (growRightLimit < 0) {
            // awt.19E=growRightLimit must be a positive number
            throw new IllegalArgumentException(Messages.getString("awt.19E")); //$NON-NLS-1$
        }
        this.growRightLimit = growRightLimit;

        if ((shrinkPriority < 0) || (shrinkPriority > PRIORITY_NONE)) {
            // awt.19F=incorrect value for shrinkPriority, more than PRIORITY_NONE or less than PRIORITY_KASHIDA value
            throw new IllegalArgumentException(Messages.getString("awt.19F")); //$NON-NLS-1$
        }
        this.shrinkPriority = shrinkPriority;

        if ((growPriority < 0) || (growPriority > PRIORITY_NONE)) {
            // awt.200=incorrect value for growPriority, more than PRIORITY_NONE or less than PRIORITY_KASHIDA value
            throw new IllegalArgumentException(Messages.getString("awt.200")); //$NON-NLS-1$
        }
        this.growPriority = growPriority;

        if (shrinkLeftLimit < 0) {
            // awt.201=shrinkLeftLimit must be a positive number
            throw new IllegalArgumentException(Messages.getString("awt.201")); //$NON-NLS-1$
        }
        this.shrinkLeftLimit = shrinkLeftLimit;

        if (shrinkRightLimit < 0) {
            // awt.202=shrinkRightLimit must be a positive number
            throw new IllegalArgumentException(Messages.getString("awt.202")); //$NON-NLS-1$
        }
        this.shrinkRightLimit = shrinkRightLimit;

        this.shrinkAbsorb = shrinkAbsorb;
        this.growAbsorb = growAbsorb;
    }
}
