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
 * @author Oleg V. Khaschansky
 *
 * @date: Nov 14, 2005
 */

package org.apache.harmony.awt.gl.linux;

import java.awt.GraphicsDevice;
import java.awt.HeadlessException;

import org.apache.harmony.awt.gl.CommonGraphicsEnvironment;
import org.apache.harmony.awt.wtk.linux.LinuxWindowFactory;
import org.apache.harmony.awt.nativebridge.linux.X11;

public class XGraphicsEnvironment extends CommonGraphicsEnvironment {
    private static final X11 x11 = X11.getInstance();

    private long display;
    private XGraphicsDevice devices[];
    private int defaultDeviceNum;

    public XGraphicsEnvironment(LinuxWindowFactory winFactory) {
        display = winFactory.getDisplay();
        // LinuxWindowFactory is able to use only default screen
        // but there can be several screens actually
        int nScreens = x11.XScreenCount(display);
        devices = new XGraphicsDevice[nScreens];
        for (int i=0; i<nScreens; i++) {
            devices[i] = new XGraphicsDevice(display, i);
        }
        defaultDeviceNum = x11.XDefaultScreen(display);
    }

    public GraphicsDevice getDefaultScreenDevice() throws HeadlessException {
        return devices[defaultDeviceNum];
    }

    public GraphicsDevice[] getScreenDevices() throws HeadlessException {
        XGraphicsDevice res[] = new XGraphicsDevice[devices.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = devices[i];
        }

        return res;
    }

}
