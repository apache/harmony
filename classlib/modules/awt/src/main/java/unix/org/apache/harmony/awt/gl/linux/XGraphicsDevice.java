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
 * @date: Nov 15, 2005
 */

package org.apache.harmony.awt.gl.linux;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;

import org.apache.harmony.awt.gl.GLGraphicsDevice;
import org.apache.harmony.awt.gl.Utils;
import org.apache.harmony.awt.nativebridge.Int32Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.X11Defs;
import org.apache.harmony.awt.ContextStorage;

public class XGraphicsDevice extends GLGraphicsDevice {
    private static final X11 x11 = X11.getInstance();

    long display;
    int screen;

    //private X11.Display xdisplay = null;
    private X11.Screen xscreen = null;

    private XGraphicsConfiguration configs[] = null;
    private int defaultConfigIdx;

    private DisplayMode displayMode;

    XGraphicsDevice(long display, int screen) {
        this.display = display;
        this.screen = screen;

        displayMode = new DisplayMode(
                getDisplayWidth(),
                getDisplayHeight(),
                DisplayMode.BIT_DEPTH_MULTI,
                DisplayMode.REFRESH_RATE_UNKNOWN
        );
    }

    private final X11.Screen getXscreen() {
        if (xscreen == null)
            xscreen = x11.createScreen(x11.XScreenOfDisplay(display, screen));

        return xscreen;
    }

    private final XGraphicsConfiguration[] getConfigs() {
        if (configs == null) {
            //createConfigs();

            if (true || EventQueue.isDispatchThread()) {
                createConfigs();
            } else {
                boolean wasAWTLocked = false;
                try {
                    // XXX - todo - This is a hack actually, should be discussed further
                    try {
                        ContextStorage.getSynchronizer().storeStateAndFree();
                        wasAWTLocked = true;
                    } catch (RuntimeException e) {}

                    try {
                        EventQueue.invokeAndWait(
                                new Runnable() {
                                    public void run () { createConfigs(); }
                                }
                        );
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                } finally {
                    if (wasAWTLocked)
                        ContextStorage.getSynchronizer().lockAndRestoreState();
                }
            }
        }

        return configs;
    }

    void createConfigs() {
        // First get default visual ID
        long defVisualPtr = x11.XDefaultVisual(display, screen);
        long defVisId = x11.XVisualIDFromVisual(defVisualPtr);

        // Allocate one int to get number of visual infos by ref
        Int32Pointer numVisualInfosPtr =
                NativeBridge.getInstance().createInt32Pointer(1, true);

        // Create template visual to obtain visuals for current screen only
        X11.XVisualInfo vinfo_template = x11.createXVisualInfo(true);
        vinfo_template.set_screen(screen);

        // Obtain infos
        X11.XVisualInfo infosPtr = x11.XGetVisualInfo(
                display,
                X11Defs.VisualScreenMask,
                vinfo_template,
                numVisualInfosPtr
        );
        vinfo_template.free(); // Free template data

        int numVisualInfos = numVisualInfosPtr.get(0);
        numVisualInfosPtr.free();

        // Allocate array for configurations
        configs = new XGraphicsConfiguration[numVisualInfos];

        String opengl = org.apache.harmony.awt.Utils.getSystemProperty("java2d.opengl"); //$NON-NLS-1$
        boolean useOpenGL = opengl != null && opengl.equals("true"); //$NON-NLS-1$

        for (int i=0; i<numVisualInfos; i++) {
            X11.XVisualInfo info = x11.createXVisualInfo(
                    infosPtr.getElementPointer(i*infosPtr.size())
            );
            configs[i] = useOpenGL ?
                    new GLXGraphicsConfiguration(this, info) :
                    new XGraphicsConfiguration(this, info);

            if (info.get_visualid() == defVisId)
                defaultConfigIdx = i;
        }

        if (useOpenGL) {
            //defVisId = 36;
            defVisId = GLXGraphicsConfiguration.GlxConfigsRec.getBestGLXVisualId(display, screen);
            for (int i=0; i<numVisualInfos; i++) {
                if (configs[i].info.get_visualid() == defVisId) {
                    defaultConfigIdx = i;
                    break;
                }
            }
        }
    }

    public int getType() {
        return TYPE_RASTER_SCREEN;
    }

    public GraphicsConfiguration getDefaultConfiguration() {
        return getConfigs()[defaultConfigIdx];
    }

    public GraphicsConfiguration[] getConfigurations() {
        return getConfigs();
    }

    public String getIDstring() {
        long strPtr = x11.XDisplayString(display);
        return Utils.straccess.createStringUTF(strPtr);
    }

    int getDisplayWidth() {
        return getXscreen().get_width();
    }

    int getDisplayHeight() {
        return getXscreen().get_height();
    }

    protected void finalize() {
        // Here we have to XFree all XVisualInfo's allocated by createConfigs
        // First info contains a pointer to memory allocated by XGetVisualInfo
        if (configs != null && configs.length != 0) {
            x11.XFree(configs[0].info);
        }
    }

    public int getScreen() {
        return screen;
    }

    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    public DisplayMode[] getDisplayModes() {
        DisplayMode []dms = {displayMode};
        return  dms;
    }

    public Dimension getResolution() {
        return new Dimension(
                (int) (x11.XDisplayWidth(display, screen) * 25.4 /
                       x11.XDisplayWidthMM(display,  screen)),
                (int) (x11.XDisplayHeight(display, screen) * 25.4 /
                       x11.XDisplayHeightMM(display,  screen))
        );
    }
}

