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
 * @author Igor A. Pyankov 
 */ 

package org.apache.harmony.x.print.ipp;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class IppResources {
    private static String IppStrings_BUNDLE_NAME;
    private static ResourceBundle IppStrings_RESOURCE_BUNDLE;

    static {
        try {
            IppStrings_BUNDLE_NAME = IppResources.class.getPackage().getName()
                    + ".resources.IppStrings";
            IppStrings_RESOURCE_BUNDLE = ResourceBundle
                    .getBundle(IppStrings_BUNDLE_NAME);
        } catch (RuntimeException e) {
            IppStrings_RESOURCE_BUNDLE = null;
        }
    }

    public static String getString(String key) {
        try {
            if (IppStrings_RESOURCE_BUNDLE != null) {
                return IppStrings_RESOURCE_BUNDLE.getString(key);
            }
        } catch (MissingResourceException e) {
            return null;
        }
        return null;
    }
}