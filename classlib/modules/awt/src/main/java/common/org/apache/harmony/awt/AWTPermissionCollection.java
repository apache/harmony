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
 * @author Evgueni V. Brevnov
 */

package org.apache.harmony.awt;

import java.awt.AWTPermission;


public interface AWTPermissionCollection {

    AWTPermission ACCESS_CLIPBOARD_PERMISSION = new AWTPermission(
        "accessClipboard"); //$NON-NLS-1$

    AWTPermission ACCESS_EVENT_QUEUE_PERMISSION = new AWTPermission(
        "accessEventQueue"); //$NON-NLS-1$

    AWTPermission CREATE_ROBOT_PERMISSION = new AWTPermission("createRobot"); //$NON-NLS-1$

    AWTPermission FULL_SCREEN_EXCLUSIVE_PERMISSION = new AWTPermission(
        "fullScreenExclusive"); //$NON-NLS-1$

    AWTPermission LISTEN_TO_ALL_AWTEVENTS_PERMISSION = new AWTPermission(
        "listenToAllAWTEvents"); //$NON-NLS-1$

    AWTPermission READ_DISPLAY_PIXELS_PERMISSION = new AWTPermission(
        "readDisplayPixels"); //$NON-NLS-1$

    AWTPermission REPLACE_KEYBOARD_FOCUS_MANAGER_PERMISSION = new AWTPermission(
        "replaceKeyboardFocusManager"); //$NON-NLS-1$

    AWTPermission SET_APPLET_STUB_PERMISSION = new AWTPermission(
        "setAppletStub"); //$NON-NLS-1$

    AWTPermission SET_WINDOW_ALWAYS_ON_TOP_PERMISSION = new AWTPermission(
        "setWindowAlwaysOnTop"); //$NON-NLS-1$

    AWTPermission SHOW_WINDOW_WITHOUT_WARNING_BANNER_PERMISSION = new AWTPermission(
        "showWindowWithoutWarningBanner"); //$NON-NLS-1$

    AWTPermission WATCH_MAOUSE_POINTER_PERMISSION = new AWTPermission(
        "watchMousePointer"); //$NON-NLS-1$
}

