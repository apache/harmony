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

import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.X11Defs;
import org.apache.harmony.awt.nativebridge.Int8Pointer;

import java.awt.image.*;
import java.awt.*;

import org.apache.harmony.awt.gl.GLVolatileImage;
import org.apache.harmony.awt.gl.Surface;
import org.apache.harmony.awt.gl.Utils;
import org.apache.harmony.awt.internal.nls.Messages;

public class XVolatileImage extends GLVolatileImage {
    private static final X11 x11 = X11.getInstance();
    private static final ImageCapabilities ic = new ImageCapabilities(true);

    private long pixmap;
    private XGraphicsConfiguration xconf;
    Surface surface;

    int width, height;

    XVolatileImage(XGraphicsConfiguration xconf, int w, int h) {
        this.xconf = xconf;
        width = w;
        height = h;
        long display = xconf.dev.display;
        pixmap = x11.XCreatePixmap(
                display,
                x11.XRootWindow(display, xconf.dev.screen),
                w, h,
                xconf.info.get_depth()
        );

        surface = new PixmapSurface(display, pixmap, xconf.info.lock(), w, h);
        xconf.info.unlock();
    }

    public long getPixmap() {
        return pixmap;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public boolean contentsLost() {
        return false;
    }

    public Graphics2D createGraphics() {
        return new XGraphics2D(this, 0, 0, width, height);
    }

    public int validate(GraphicsConfiguration graphicsConfiguration) {
        if (graphicsConfiguration.equals(xconf))
            return IMAGE_OK;
        else
            return IMAGE_INCOMPATIBLE;
    }

    public ImageCapabilities getCapabilities() {
        return ic;
    }

    public BufferedImage getSnapshot() {
        long xImagePtr = 0L;
        X11.XImage xImage;

        if (x11.XImageByteOrder(xconf.dev.display) == X11Defs.LSBFirst) {
            xImagePtr = x11.XGetImage(
                    xconf.dev.display,
                    pixmap,
                    0, 0,
                    width, height,
                    ~(0L), // All bits set to 1, should be same as XAllPlanes() result
                    X11Defs.ZPixmap
            );

            if (xImagePtr == 0) // Check obtained XImage pointer
                return null;

            xImage = x11.createXImage(xImagePtr);

        } else {
            xImagePtr = x11.XGetImage(
                    xconf.dev.display,
                    pixmap,
                    0, 0,
                    1, 1,
                    ~(0L), // All bits set to 1, should be same as XAllPlanes() result
                    X11Defs.ZPixmap
            );

            if (xImagePtr == 0) // Check obtained XImage pointer
                return null;

            X11.XImage xTmpImage = x11.createXImage(xImagePtr);
            X11.Visual visual = xconf.info.get_visual();

            xImagePtr = x11.XCreateImage(
                    xconf.dev.display,
                    visual.lock(),
                    xTmpImage.get_depth(),
                    xTmpImage.get_format(),
                    xTmpImage.get_xoffset(),
                    Utils.memaccess.malloc(height*width*xTmpImage.get_bytes_per_line()),
                    width, height,
                    xTmpImage.get_bitmap_pad(),
                    0
            );
            visual.unlock();

            xImage = x11.createXImage(xImagePtr);
            xImage.set_byte_order(X11Defs.LSBFirst);

            xTmpImage.get_f().destroy_image(xTmpImage);

            xImage = x11.XGetSubImage(
                    xconf.dev.display,
                    pixmap,
                    0, 0,
                    width, height,
                    ~(0L), // All bits set to 1, should be same as XAllPlanes() result
                    X11Defs.ZPixmap,
                    xImage,
                    0, 0
            );
        }

        BufferedImage res = biFromXImage(xImage, xconf);

        // Cleanup
        xImage.get_f().destroy_image(xImage);

        return res;
    }

    public static BufferedImage biFromXImage(X11.XImage xImage, XGraphicsConfiguration xconf) {
        int width = xImage.get_width();
        int height = xImage.get_height();

        ColorModel cm = xconf.getColorModel();
        SampleModel sm;
        DataBuffer db;
        int scanlineStride, dataType;
        int bpp = xImage.get_bits_per_pixel();
        Int8Pointer dataPtr = xImage.get_data();

        switch (bpp) {
            case 32: {
                dataType = DataBuffer.TYPE_INT;
                scanlineStride = xImage.get_bytes_per_line() >> 2;
                int size = scanlineStride * height;

                // Create data buffer
                int[] data = new int[size];
                Utils.memaccess.getInt(dataPtr.lock(), data, xImage.get_xoffset(), size);
                dataPtr.unlock();
                db = new DataBufferInt(data, size);

                break;
            }
            case 16: {
                dataType = DataBuffer.TYPE_USHORT;
                scanlineStride = xImage.get_bytes_per_line() >> 1;
                int size = scanlineStride * height;

                // Create data buffer
                short[] data = new short[size];
                Utils.memaccess.getShort(dataPtr.lock(), data, xImage.get_xoffset(), size);
                dataPtr.unlock();
                db = new DataBufferShort(data, size);

                break;
            }
            case 8:
            case 4:
            case 2:
            case 1: {
                dataType = DataBuffer.TYPE_BYTE;
                scanlineStride = xImage.get_bytes_per_line();
                int size = scanlineStride * height;

                // Create data buffer
                byte[] data = new byte[size];
                Utils.memaccess.getByte(dataPtr.lock(), data, xImage.get_xoffset(), size);
                dataPtr.unlock();
                db = new DataBufferByte(data, size);

                break;
            }
            default: {
                // awt.0A=Cannot use SinglePixedPackedSampleModel for bpp = {0}
                throw new InternalError(Messages.getString(
                    "awt.0A", xImage.get_bits_per_pixel())); //$NON-NLS-1$
            }
        }

        if (cm instanceof DirectColorModel) { // TrueColor / DirectColor
            // Because XGetImage doesn't set masks we have to get them right from the visual info
            int bitMasks[] = ((DirectColorModel) cm).getMasks();

            // Now create sample model
            sm = new SinglePixelPackedSampleModel(
                    dataType,
                    width, height,
                    scanlineStride, bitMasks
            );

        } else if (
                cm instanceof IndexColorModel ||
                cm instanceof ComponentColorModel
        ) { // PseudoColor, StaticGray, etc.
            if (bpp >= 8) {
                sm = new PixelInterleavedSampleModel(
                        dataType,
                        width, height,
                        1,
                        scanlineStride,
                        new int[] {0}
                );
            } else {
                sm = new MultiPixelPackedSampleModel(
                        dataType,
                        width, height,
                        bpp,
                        scanlineStride,
                        0
                );
            }
        } else {
            // awt.0B=Wrong color model created for drawable
            throw new InternalError(Messages.getString("awt.0B")); //$NON-NLS-1$
        }

        return new BufferedImage(
                cm, Raster.createWritableRaster(sm, db, new Point(0,0)),
                false, null
        );
    }

    public Object getProperty(String name, ImageObserver observer) {
        return UndefinedProperty;
    }

    public int getWidth(ImageObserver observer) {
        return width;
    }

    public int getHeight(ImageObserver observer) {
        return height;
    }

    public void finalize() {
        surface.dispose();
        x11.XFreePixmap(xconf.dev.display, pixmap);
    }

    public Surface getImageSurface() {
        return surface;
    }
}
