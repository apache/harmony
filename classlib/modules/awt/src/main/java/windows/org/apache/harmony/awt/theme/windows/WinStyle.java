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
 * @author Dmitry A. Durnev
 */
package org.apache.harmony.awt.theme.windows;

import org.apache.harmony.awt.nativebridge.windows.WindowsConsts;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;

/**
 * Common functinality for native Windows visual style
 */
public class WinStyle implements WindowsConsts, WindowsDefs {

    static int getClassicButtonState(boolean enabled, boolean pressed) {
        if (!enabled) {
            return DFCS_INACTIVE;
        } else if (pressed) {
            return DFCS_PUSHED;
        }
        return 0;
    }
}
