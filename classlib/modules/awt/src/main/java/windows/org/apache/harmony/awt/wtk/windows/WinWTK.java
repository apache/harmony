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
 * @author Pavel Dolgov
 */
package org.apache.harmony.awt.wtk.windows;

import java.awt.GraphicsDevice;

import org.apache.harmony.awt.wtk.*;


public class WinWTK extends WTK {

    static {
        org.apache.harmony.awt.Utils.loadLibrary("gl"); //$NON-NLS-1$
    }

    /**
     * @see org.apache.harmony.awt.wtk.WTK#getGraphicsFactory()
     */
    @Override
    public GraphicsFactory getGraphicsFactory() {
        return graphicsFactory;
    }

    /**
     * @see org.apache.harmony.awt.wtk.WTK#getNativeEventQueue()
     */
    @Override
    public NativeEventQueue getNativeEventQueue() {
        return eventQueue;
    }

    /**
     * @see org.apache.harmony.awt.wtk.WTK#getWindowFactory()
     */
    @Override
    public WindowFactory getWindowFactory() {
        return eventQueue.factory;
    }

    /**
     * @see org.apache.harmony.awt.wtk.WTK#getCursorFactory()
     */
    @Override
    public CursorFactory getCursorFactory() {
        return cursorFactory;
    }

    /**
     * @see org.apache.harmony.awt.wtk.WTK#getNativeMouseInfo()
     */
    @Override
    public NativeMouseInfo getNativeMouseInfo() {
        return mouseInfo;
    }

    /**
     * @see org.apache.harmony.awt.wtk.WTK#getSystemProperties()
     */
    @Override
    public SystemProperties getSystemProperties() {
        return systemProperties;
    }

    WinEventQueue getWinEventQueue() {
        return eventQueue;
    }

    /**
     * @see org.apache.harmony.awt.wtk.WTK#getNativeRobot(java.awt.GraphicsDevice)
     */
    @Override
    public NativeRobot getNativeRobot(GraphicsDevice screen) {
        if (robot == null) {
            robot = new WinRobot();
        }
        return robot;
    }

    @Override
    public NativeIM getNativeIM() {
        if (im == null) {
            im = new WinIM();
        }
        return im;
    }

    public native boolean getLockingState(int keyCode);

    public native void setLockingState(int keyCode, boolean on);

    private final WinSystemProperties systemProperties = new WinSystemProperties();
    private final WinEventQueue eventQueue = new WinEventQueue(systemProperties);
    private final GraphicsFactory graphicsFactory = new org.apache.harmony.awt.gl.windows.WinGraphics2DFactory();
    private final CursorFactory cursorFactory = new WinCursorFactory(eventQueue);
    private final NativeMouseInfo mouseInfo = new WinMouseInfo();
    private WinRobot robot;
    private WinIM im;
}
