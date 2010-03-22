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
 * @author Michael Danilov
 */
package org.apache.harmony.awt.wtk.linux;

import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.nativebridge.linux.X11;

class XServerConnection {

    private long display;

    private int screen;

    private final X11 x11;

    private static XServerConnection instance = new XServerConnection();

    private XServerConnection() {
        this.x11 = X11.getInstance();;
        display = x11.XOpenDisplay(0); //0 - we use default display only
        if (display == 0) {
            String name = org.apache.harmony.awt.Utils.getSystemProperty("DISPLAY"); //$NON-NLS-1$
            // awt.0F=Cannot open display '{0}'
            throw new InternalError(Messages.getString("awt.0F", //$NON-NLS-1$ 
                    (name != null ? name : ""))); //$NON-NLS-1$
        }

        screen = x11.XDefaultScreen(display);

        org.apache.harmony.awt.Utils.loadLibrary("gl");
        init(display, screen);
    }

    public static XServerConnection getInstance(){
        return instance;
    }

    public void close() {
        x11.XCloseDisplay(display);
    }

    public long getDisplay() {
        return display;
    }

    public int getScreen() {
        return screen;
    }

    private native void init(long display, int screen);
}
