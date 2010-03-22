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

import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.nativebridge.*;
import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.X11Defs;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;


public class XGraphicsConfiguration extends GraphicsConfiguration {
    // X11 atom, required for getting the default colormap
    private static final long XA_RGB_DEFAULT_MAP = 27;

    protected static final X11 x11 = X11.getInstance();

    XGraphicsDevice dev;
    X11.XVisualInfo info;
    ColorModel cm = null;

    long xcolormap;

    XGraphicsConfiguration(XGraphicsDevice dev, X11.XVisualInfo info) {
        this.dev = dev;
        this.info = info;
        xcolormap = obtainRGBColorMap();
    }

    public long getXColormap() {
        return xcolormap;
    }

    public int getDepth() {
        return info.get_depth();
    }

    public X11.Visual getVisual() {
        return info.get_visual();
    }

    private long obtainRGBColorMap() {
        X11.Visual defVisual = x11.createVisual(x11.XDefaultVisual(dev.display, dev.screen));
        if (info.get_visualid() == defVisual.get_visualid()) {
            return x11.XDefaultColormap(dev.display, dev.screen);
        }

        X11.Visual vis = info.get_visual();
        long rootWindow = x11.XRootWindow(dev.display, dev.screen);

        int status = x11.XmuLookupStandardColormap(
                dev.display, dev.screen,
                info.get_visualid(),
                info.get_depth(),
                               XA_RGB_DEFAULT_MAP,
                               X11Defs.False,
                X11Defs.True
        );

        if (status == 1) {
            Int32Pointer nCmaps = NativeBridge.getInstance().createInt32Pointer(1, true);
            PointerPointer stdCmaps = NativeBridge.getInstance().createPointerPointer(1, true);

            status = x11.XGetRGBColormaps(
                    dev.display,
                    rootWindow,
                    stdCmaps,
                                   nCmaps,
                    XA_RGB_DEFAULT_MAP
            );

            int numCmaps = nCmaps.get(0);
            VoidPointer ptr = stdCmaps.get(0);
            nCmaps.free();
            stdCmaps.free();

            if (status == 1) {
                for (int i = 0; i < numCmaps; i++) {

                    X11.XStandardColormap stdCmap = x11.createXStandardColormap(
                            ptr.byteBase.getElementPointer(i*X11.XStandardColormap.sizeof)
                    );

                    if (stdCmap.get_visualid() == info.get_visualid()) {
                        long cmap = stdCmap.get_colormap();
                        x11.XFree(ptr);
                        return cmap;
                    }
                }

                x11.XFree(ptr);
            }
        }

        long cmap = x11.XCreateColormap(
                dev.display,
                rootWindow,
                vis.lock(),
                X11Defs.AllocNone
        );
        vis.unlock();
        return cmap;
    }

    public GraphicsDevice getDevice() {
        return dev;
    }

    public Rectangle getBounds() {
        return new Rectangle(dev.getDisplayWidth(), dev.getDisplayHeight());
    }

    public AffineTransform getDefaultTransform() {
        return new AffineTransform();
    }

    public AffineTransform getNormalizingTransform() {
        return new AffineTransform(); // XXX - todo
    }

    public BufferedImage createCompatibleImage(int width, int height) {
        return new BufferedImage(
                getColorModel(),
                getColorModel().createCompatibleWritableRaster(width, height),
                false,
                null
        );
    }

    public BufferedImage createCompatibleImage(int width, int height, int transparency) {
        return new BufferedImage(
                getColorModel(transparency),
                getColorModel(transparency).createCompatibleWritableRaster(width, height),
                false,
                null
        );
    }

    public ColorModel getColorModel() {
        if (cm == null) {
            switch (info.get_class()) {
                case X11Defs.TrueColor:
                case X11Defs.DirectColor:
                    cm = new DirectColorModel(
                            info.get_depth(),
                            (int) info.get_red_mask(),
                            (int) info.get_green_mask(),
                            (int) info.get_blue_mask()
                    );
                    break;
                case X11Defs.StaticGray:
                    // looks like native colormap differs from the colors given
                    // by the gray ICC profile
                    /*
                    // This should be enough
                    cm = new ComponentColorModel(
                            ColorSpace.getInstance(ColorSpace.CS_GRAY),
                            new int[]{info.get_depth()},
                            false,
                            false,
                            Transparency.OPAQUE,
                            DataBuffer.TYPE_BYTE
                    );
                    break;
                    */
                case X11Defs.PseudoColor:
                case X11Defs.GrayScale:
                case X11Defs.StaticColor: {
                    // Always use default colormap to create ColorModel.
                    // This should be sufficient for the first time.
                    int defCmapSize =
                            ((XGraphicsConfiguration)
                            dev.getDefaultConfiguration()).info.get_colormap_size();

                    // Create array of XColor and fill pixel values
                    Int8Pointer xColors =
                            NativeBridge.getInstance().createInt8Pointer(
                                    X11.XColor.sizeof * defCmapSize,
                                    true
                            );
                    int xColorsIdx = 0;
                    for (int i = 0; i < defCmapSize; i++, xColorsIdx += X11.XColor.sizeof) {
                        X11.XColor color = x11.createXColor(xColors.getElementPointer(xColorsIdx));
                        color.set_pixel(i);
                    }

                    // Get the rgb values from the default colormap and create cm
                    x11.XQueryColors(dev.display, xcolormap, xColors.lock(), defCmapSize);
                    xColors.unlock();

                    byte redVals[] = new byte[defCmapSize];
                    byte greenVals[] = new byte[defCmapSize];
                    byte blueVals[] = new byte[defCmapSize];

                    xColorsIdx = 0;
                    for (int i = 0; i < defCmapSize; i++, xColorsIdx += X11.XColor.sizeof) {
                        X11.XColor color = x11.createXColor(xColors.getElementPointer(xColorsIdx));
                        redVals[i] = (byte) ((color.get_red() >> 8) & 0xFF);
                        greenVals[i] = (byte) ((color.get_green() >> 8) & 0xFF);
                        blueVals[i] = (byte) ((color.get_blue() >> 8) & 0xFF);
                    }

                    cm = new IndexColorModel(
                            info.get_depth(),
                            defCmapSize,
                            redVals, greenVals, blueVals,
                            -1
                    );

                    xColors.free();
                    break;
                }
                default:
                    // awt.0C=Unknown visual class
                    throw new InternalError(Messages.getString("awt.0C")); //$NON-NLS-1$
            }
        }

        return cm;
    }

    public ColorModel getColorModel(int transparency) {
        switch (transparency) {
            case Transparency.OPAQUE:
                return getColorModel();
            case Transparency.TRANSLUCENT:
                return ColorModel.getRGBdefault();
            case Transparency.BITMASK:
                return new DirectColorModel(25, 0xFF0000, 0xFF00, 0xFF, 0x1000000);
            default:
                // awt.0D=Invalid transparency
                throw new IllegalArgumentException(Messages.getString("awt.0D")); //$NON-NLS-1$
        }
    }

    public VolatileImage createCompatibleVolatileImage(int width, int height) {
        if (width <= 0 || height <= 0)
            // awt.0E=Dimensions of the image should be positive
            throw new IllegalArgumentException(Messages.getString("awt.0E")); //$NON-NLS-1$
            

        return new XVolatileImage(this, width, height);
    }

    public VolatileImage createCompatibleVolatileImage(
            int width, int height,
            int transparency
    ) {
        // XXX - todo - implement transparency
        return createCompatibleVolatileImage(width, height);
    }
}
