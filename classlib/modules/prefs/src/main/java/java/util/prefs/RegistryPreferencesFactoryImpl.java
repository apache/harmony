/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.util.prefs;

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Default implementation of {@code PreferencesFactory} for windows
 * platform, using windows Registry as back end.
 * 
 * @since 1.4
 */
class RegistryPreferencesFactoryImpl implements PreferencesFactory {

    // user root preferences
    private static final Preferences USER_ROOT = new RegistryPreferencesImpl(
            true);

    // system root preferences
    private static final Preferences SYSTEM_ROOT = new RegistryPreferencesImpl(
            false);

    public RegistryPreferencesFactoryImpl() {
        super();
    }

    public Preferences userRoot() {
        return USER_ROOT;
    }

    public Preferences systemRoot() {
        return SYSTEM_ROOT;
    }
}
