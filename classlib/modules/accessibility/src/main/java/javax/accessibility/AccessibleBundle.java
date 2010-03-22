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
 * @author Dennis Ushakov
 */

package javax.accessibility;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public abstract class AccessibleBundle {
    private static final String ACCESSIBLE_RESOURSE_BUNDLE = "javax.accessibility.AccessibleResourceBundle"; //$NON-NLS-1$
    
    protected String key;

    public String toDisplayString() {
        return displayString(ACCESSIBLE_RESOURSE_BUNDLE, null);
    }

    public String toDisplayString(final Locale locale) {
        return displayString(ACCESSIBLE_RESOURSE_BUNDLE, locale);
    }

    protected String toDisplayString(final String resourceBundleName, final Locale locale) {
        return displayString(resourceBundleName, locale);
    }

    @Override
    public String toString() {
        return toDisplayString();
    }


    private String displayString(final String bundleName, final Locale locale) {
        try {
            if (locale == null) {
                return ResourceBundle.getBundle(bundleName).getString(key);
            }
            return ResourceBundle.getBundle(bundleName, locale).getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}

