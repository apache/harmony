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
 * @author Roman I. Chernyatchik
 */
package javax.swing.text;

import java.io.Serializable;

import org.apache.harmony.misc.HashCode;


public class TabStop implements Serializable {
    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_RIGHT = 1;
    public static final int ALIGN_CENTER = 2;
    public static final int ALIGN_DECIMAL = 4;
    public static final int ALIGN_BAR = 5;

    public static final int LEAD_NONE = 0;
    public static final int LEAD_DOTS = 1;
    public static final int LEAD_HYPHENS = 2;
    public static final int LEAD_UNDERLINE = 3;
    public static final int LEAD_THICKLINE = 4;
    public static final int LEAD_EQUALS = 5;

    private final int alignment;
    private final int leader;
    private final float position;

    public TabStop(final float position) {
        this(position, ALIGN_LEFT, LEAD_NONE);
    }

    public TabStop(final float position, final int align, final int leader) {
        this.position = position;
        this.alignment = align;
        this.leader = leader;
    }

    public int getAlignment() {
        return alignment;
    }

    public int getLeader() {
        return leader;
    }

    public float getPosition() {
        return position;
    }

    public boolean equals(final Object other) {
        if (!(other instanceof TabStop)) {
            return false;
        }

        final TabStop tabStop = (TabStop)other;
        return tabStop.alignment == alignment
               && tabStop.leader == leader
               && tabStop.position == position;
    }

    public int hashCode() {
        HashCode hash = new HashCode();

        hash.append(position);
        hash.append(alignment);
        hash.append(leader);

        return hash.hashCode();
    }

    public String toString() {
        final StringBuilder result = new StringBuilder();
        switch (alignment) {
            case ALIGN_RIGHT:
                result.append("right ");
                break;
            case ALIGN_CENTER:
                result.append("center ");
                break;
            case ALIGN_DECIMAL:
                result.append("decimal ");
                break;
            case ALIGN_BAR:
                result.append("bar ");
                break;
        }
        result.append("tab @").append(position);

        if (leader != LEAD_NONE) {
            result.append(" (w/leaders)");
        }

        return result.toString();
    }
}
