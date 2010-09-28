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
 * @author Michael Danilov, Dmitry A. Durnev
 */
package java.awt;

import java.io.Serializable;
import java.util.*;

public final class ComponentOrientation implements Serializable {
    private static final long serialVersionUID = -4113291392143563828L;

    public static final ComponentOrientation LEFT_TO_RIGHT = new ComponentOrientation(true, true);

    public static final ComponentOrientation RIGHT_TO_LEFT = new ComponentOrientation(true, false);

    public static final ComponentOrientation UNKNOWN = new ComponentOrientation(true, true);

    private static final Set<String> rlLangs = new HashSet<String>(); //RIGHT_TO_LEFT languages

    private final boolean horizontal;

    private final boolean left2right;

    static {
        rlLangs.add("ar"); //$NON-NLS-1$
        rlLangs.add("fa"); //$NON-NLS-1$
        rlLangs.add("iw"); //$NON-NLS-1$
        rlLangs.add("ur"); //$NON-NLS-1$
    }

    /**
     * @deprecated
     */
    @Deprecated
    public static ComponentOrientation getOrientation(ResourceBundle bdl) {
        Object obj = null;
        try {
            obj = bdl.getObject("Orientation"); //$NON-NLS-1$
        }
        catch (MissingResourceException mre) {
            obj = null;
        }
        if (obj instanceof ComponentOrientation) {
            return (ComponentOrientation) obj;
        }
        Locale locale = bdl.getLocale();
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return getOrientation(locale);
    }

    public static ComponentOrientation getOrientation(Locale locale) {
        String lang = locale.getLanguage();
        return rlLangs.contains(lang) ? RIGHT_TO_LEFT : LEFT_TO_RIGHT;
    }

    private ComponentOrientation(boolean hor, boolean l2r) {
        horizontal = hor;
        left2right = l2r;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    public boolean isLeftToRight() {
        return left2right;
    }

}
