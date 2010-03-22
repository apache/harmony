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
package java.awt;

import org.apache.harmony.awt.internal.nls.Messages;

public abstract class GraphicsDevice {
    private DisplayMode displayMode;

    private Window fullScreenWindow = null;

   /***************************************************************************
    *
    *  Constants
    *
    ***************************************************************************/

    public static final int TYPE_IMAGE_BUFFER = 2;

    public static final int TYPE_PRINTER = 1;

    public static final int TYPE_RASTER_SCREEN = 0;

   /***************************************************************************
    *
    *  Constructors
    *
    ***************************************************************************/

    protected GraphicsDevice() {
        displayMode = new DisplayMode(0, 0, DisplayMode.BIT_DEPTH_MULTI, DisplayMode.REFRESH_RATE_UNKNOWN);
    }


   /***************************************************************************
    *
    *  Abstract methods
    *
    ***************************************************************************/

    public abstract GraphicsConfiguration[] getConfigurations();

    public abstract GraphicsConfiguration getDefaultConfiguration();

    public abstract String getIDstring();

    public abstract int getType();



   /***************************************************************************
    *
    *  Public methods
    *
    ***************************************************************************/

    public int getAvailableAcceleratedMemory() {
        return 0;
    }

    public GraphicsConfiguration getBestConfiguration(GraphicsConfigTemplate gct) {
        return gct.getBestConfiguration(getConfigurations());
    }

    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    public DisplayMode[] getDisplayModes() {
        DisplayMode []dms = {displayMode};
        return  dms;
    }

    public Window getFullScreenWindow() {
        return fullScreenWindow;
    }

    public boolean isDisplayChangeSupported() {
        return false;
    }

    public boolean isFullScreenSupported() {
        return false;
    }

    public void setDisplayMode(DisplayMode dm) {
        if (!isDisplayChangeSupported()) {
            // awt.122=Does not support display mode changes
            throw new UnsupportedOperationException(Messages.getString("awt.122")); //$NON-NLS-1$
        }

        DisplayMode []dms = getDisplayModes();
        for (DisplayMode element : dms) {
            if (element.equals(dm)) {
                displayMode = dm;
                return;
            }
        }
        // awt.123=Unsupported display mode: {0}
        throw new IllegalArgumentException(Messages.getString("awt.123", dm)); //$NON-NLS-1$
    }

    public void setFullScreenWindow(Window w) {
        if (w == null) {
            fullScreenWindow = null;
            return;
        }

        fullScreenWindow = w;

        if (isFullScreenSupported()) {
            w.enableInputMethods(false);
        } else {
            w.setSize(displayMode.getWidth(), displayMode.getHeight());
            w.setLocation(0, 0);
        }
        w.setVisible(true);
        w.setAlwaysOnTop(true);
    }
}
