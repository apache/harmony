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

package javax.swing;

import java.awt.DefaultFocusTraversalPolicy;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.KeyboardFocusManager;

public abstract class FocusManager extends DefaultKeyboardFocusManager {
    public static final String FOCUS_MANAGER_CLASS_PROPERTY = "FocusManagerClassName";

    private static boolean focusManagerEnabled = true;

    @Deprecated
    public static void disableSwingFocusManager() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().setDefaultFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
        focusManagerEnabled = false;
    }

    public static FocusManager getCurrentManager() {
        KeyboardFocusManager currentManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (currentManager instanceof FocusManager) {
            return (FocusManager)currentManager;
        }
        return null;
    }

    public static void setCurrentManager(final FocusManager focusManager) throws SecurityException {
        KeyboardFocusManager.setCurrentKeyboardFocusManager(focusManager);
    }

    @Deprecated
    public static boolean isFocusManagerEnabled() {
        return focusManagerEnabled;
    }
}
