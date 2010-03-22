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

package javax.swing.plaf.synth;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * The class used to mark the states in coloring JComponents
 */
public class ColorType {

    public static final ColorType FOCUS = new ColorType("focus"); //$NON-NLS-1$

    public static final ColorType BACKGROUND = new ColorType("background"); //$NON-NLS-1$

    public static final ColorType FOREGROUND = new ColorType("foreground"); //$NON-NLS-1$

    public static final ColorType TEXT_BACKGROUND = new ColorType(
            "text_background"); //$NON-NLS-1$

    public static final ColorType TEXT_FOREGROUND = new ColorType(
            "text_foreground"); //$NON-NLS-1$

    /**
     * The maximum number of created color types. This field exist for
     * compatibility with RI only
     */
    public static final int MAX_COUNT; // Required by spec to be defined as a non-const value.
    
    static {
        MAX_COUNT = 5;
    }

    /**
     * The field is used for ID calculation
     */
    private static int count = 0;

    /**
     * Textual description for ColorType
     */
    private String description;

    private int id;

    protected ColorType(String description) {
        this.description = description;
        this.id = count;
        count++;
    }

    /** @return The unique number id of the current ColorType */
    public final int getID() {
        return id;
    }

    /** @return String describes ColorType */
    @Override
    public String toString() {
        return this.description;
    }

    /**
     * The method works for predefined (static fields) ColorTypes
     * 
     * @param key
     *            The ColorType name (description)
     * @return corresponding to name ColorType.
     */
    static ColorType calculateColorType(String key) {

        if (key == null) {
            return null;
        }

        key = key.toUpperCase().intern();

        if (key == "BACKGROUND") { //$NON-NLS-1$
            return BACKGROUND;
        } else if (key == "FOREGROUND") { //$NON-NLS-1$
            return FOREGROUND;
        } else if (key == "TEXT_BACKGROUND") { //$NON-NLS-1$
            return TEXT_BACKGROUND;
        } else if (key == "TEXT_FOREGROUND") { //$NON-NLS-1$
            return TEXT_FOREGROUND;
        } else if (key == "FOCUS") { //$NON-NLS-1$
            return FOCUS;
        }
        throw new IllegalStateException(Messages.getString("swing.err.1C") //$NON-NLS-1$
                + key);
    }

}
