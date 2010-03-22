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
 * @author Alexey A. Petrenko
 */
package org.apache.harmony.awt.gl.windows;

import java.awt.GraphicsDevice;
import java.awt.HeadlessException;

import org.apache.harmony.awt.gl.CommonGraphicsEnvironment;
import org.apache.harmony.awt.wtk.WindowFactory;
import org.apache.harmony.awt.Utils;

/**
 * Windows GraphicsEnvironment implementation
 *
 */
public class WinGraphicsEnvironment extends CommonGraphicsEnvironment {
    WinGraphicsDevice defaultDevice = null;
    WinGraphicsDevice []devices = null;

    static {
        Utils.loadLibrary("gl"); //$NON-NLS-1$
    }

    public WinGraphicsEnvironment(WindowFactory wf) {
    }

    @Override
    public GraphicsDevice getDefaultScreenDevice() throws HeadlessException {
        if (defaultDevice == null) {
            WinGraphicsDevice []dvcs = (WinGraphicsDevice [])getScreenDevices();
            for (WinGraphicsDevice element : dvcs) {
                if (element.isDefaultDevice()) {
                    defaultDevice = element;
                    break;
                }
            }
        }

        return defaultDevice;
    }

    @Override
    public GraphicsDevice[] getScreenDevices() throws HeadlessException {
        if (devices == null) {
            devices = enumerateDisplayDevices();
        }

        return devices;
    }

    /**
     * Enumerates system displays
     * 
     * @return Array of WinGraphicsDevice objects representing system displays
     */
    private native WinGraphicsDevice []enumerateDisplayDevices();
}
