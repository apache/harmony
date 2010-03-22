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
* @author Ivan Popov
*/

package org.eclipse.osgi.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** 
 * This stub class replaces org.eclipse.osgi.util.NLS from Eclipse RCP bundle and provides 
 * only NLS functionality required for JDI implementation.
 */
public class NLS {

    /**
     * Fills given field with corresponding resource string from given resource bundle.
     * If bundle is null or corresponding resource string is not found, then 
     * field name will be assigned to the field.
     * 
     * @param field - static field to assign resource string to
     * @param bundle - resource bundle where to find resource string or null
     */
    private static void fillFieldValue(Field field, ResourceBundle bundle) {
        String name = field.getName();
        String value = null;
        if (bundle != null) {
            try {
                value = bundle.getString(name);
            } catch (MissingResourceException e) {
                // ignore
                e.printStackTrace(); // TODO: remove
            }
        }
        if (value == null) {
            value = name;
        }
        try {
            field.set(null, value);
        } catch (Exception e) {
            // ignore
            e.printStackTrace(); // TODO: remove
        }
    }
    
    /**
     * Loads localized messages from given bundle and assigns them to corresponding
     * static fields of given class.
     * 
     * @param bundleName - name of resource bundle to load messages from
     * @param cls - Class instance whose static fields will be filled with appropriate messages
     */
    public static void initializeMessages(String bundleName, Class cls) {
        ClassLoader loader = cls.getClassLoader();
        Field[] fields = cls.getDeclaredFields();

        // load bundle for specified class
        ResourceBundle bundle = null;
        try {
            Locale locale = Locale.getDefault();
            bundle = ResourceBundle.getBundle(bundleName, locale, loader);
        } catch (MissingResourceException e) {
            // ignore
            e.printStackTrace(); // TODO: remove
        }
        
        // fill appropriate class fields with strings from resource bundle
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            // only fields declared in given class
            if (field.getDeclaringClass() == cls) {
                // only public static and not final fields
                int modifiers = field.getModifiers();
                if ((modifiers & Modifier.FINAL) == 0 
                        && (modifiers & Modifier.STATIC) != 0
                            && (modifiers & Modifier.PUBLIC) != 0) {
                    fillFieldValue(fields[i], bundle);
                }
            }
        }
    }
}
