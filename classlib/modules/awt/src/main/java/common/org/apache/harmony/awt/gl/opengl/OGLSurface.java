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

package org.apache.harmony.awt.gl.opengl;

import org.apache.harmony.awt.gl.Surface;
import org.apache.harmony.awt.gl.ImageSurface;

import java.awt.image.*;
import java.awt.color.ColorSpace;

public class OGLSurface extends Surface {
    private static final ColorModel cm =
            new DirectColorModel(
                    ColorSpace.getInstance(ColorSpace.CS_sRGB),
                    32, 
                    0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000,
                    true, DataBuffer.TYPE_INT
            );

    OGLGraphics2D oglg;

    private ImageSurface imageSurface;
    boolean sceneUpdated = true;
    boolean cachedTopToBottom = true;
    int cachedData[];

    OGLSurface(int width, int height, OGLGraphics2D g2d) {
        super.width = width;
        super.height = height;
        oglg = g2d;
    }

    @Override
    public ColorModel getColorModel() {
        return cm;
    }

    @Override
    public WritableRaster getRaster() {
        WritableRaster res = cm.createCompatibleWritableRaster(width, height);
        DataBufferInt dbi = (DataBufferInt) res.getDataBuffer();
        oglg.readPixels(0, 0, width, height, dbi.getData(), true);
        return res;
    }

    /**
     * Clients should use the returned data only for reading 
     * @return image data
     */
    @Override
    public synchronized Object getData() {
        if (!sceneUpdated && cachedData != null && cachedTopToBottom) {
            return cachedData;
        }

        if (cachedData == null) {
            cachedData = new int[width*height];
        }

        oglg.readPixels(0, 0, width, height, cachedData, true);
        sceneUpdated = false;
        cachedTopToBottom = true;

        return cachedData;
    }

    public synchronized Object getBottomToTopData() {
        if (!sceneUpdated && cachedData != null && !cachedTopToBottom) {
            return cachedData;
        }

        if (cachedData == null) {
            cachedData = new int[width*height];
        }

        oglg.readPixels(0, 0, width, height, cachedData, false);
        sceneUpdated = false;
        cachedTopToBottom = false;

        return cachedData;
    }

    @Override
    public int getSurfaceType() {
        return BufferedImage.TYPE_INT_ARGB_PRE;
    }

    @Override
    public long lock() {
        return 0;
    }

    @Override
    public void unlock() {
    }

    @Override
    public void dispose() {
        imageSurface.dispose();
    }

    @Override
    public Surface getImageSurface() {
        if (imageSurface == null) {
            imageSurface = new ImageSurface(getColorModel(), getRaster());
        } else {
            imageSurface.setRaster(getRaster());
        }

        return imageSurface;
    }

    final void updateScene() {
        sceneUpdated = true;
        clearValidCaches();
    }
}
