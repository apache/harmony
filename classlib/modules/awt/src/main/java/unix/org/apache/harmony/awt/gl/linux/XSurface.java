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
 */

package org.apache.harmony.awt.gl.linux;

import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.X11Defs;
import org.apache.harmony.misc.accessors.LockedArray;

import java.awt.image.*;
import java.awt.geom.Rectangle2D;

import org.apache.harmony.awt.gl.*;

public class XSurface extends Surface {

    XSurface(XGraphics2D g2d, int width, int height) {
        surfaceDataPtr = createSurfData(g2d.display, g2d.drawable, g2d.imageGC, g2d.xConfig.info.lock(), width, height);
        g2d.xConfig.info.unlock();
        this.width = width;
        this.height = height;
    }
    @Override
    public void dispose() {
        if (surfaceDataPtr == 0) {
            return;
        }
        
        dispose(surfaceDataPtr);
        surfaceDataPtr = 0;
    }

    @Override
    public long lock() {
        return 0;
    }

    @Override
    public void unlock() {

    }

    @Override
    public ColorModel getColorModel() {
        return null;
    }

    @Override
    public WritableRaster getRaster() {
        return null;
    }

    @Override
    public int getSurfaceType() {
        return 0;
    }

    @Override
    public Surface getImageSurface() {
        return this;
    }

    private native long createSurfData(long display, long drawable, long gc, long visual_info, int width, int height);

    private native void dispose(long structPtr);

}
