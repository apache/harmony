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
 * @author Dmitry A. Durnev
 */
package org.apache.harmony.awt.wtk.linux;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.WritableRaster;

import org.apache.harmony.awt.gl.Utils;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.X11Defs;
import org.apache.harmony.awt.wtk.CursorFactory;
import org.apache.harmony.awt.wtk.NativeCursor;
import org.apache.harmony.misc.accessors.AccessorFactory;
import org.apache.harmony.misc.accessors.ArrayAccessor;
import org.apache.harmony.misc.accessors.LockedArray;
import org.apache.harmony.misc.accessors.MemoryAccessor;

/**
 * Implementation of CursorFactory for Linux(X11) platform.
 */
public class LinuxCursorFactory extends CursorFactory implements X11Defs {
    private static final X11 x11 = X11.getInstance();
    private static final MemoryAccessor memAccess = AccessorFactory.getMemoryAccessor();

    private final long display;
    private final LinuxWindowFactory factory;

    LinuxCursorFactory(LinuxWindowFactory factory) {
        this.factory = factory;
        display = factory.getDisplay();
    }

    /**
     * Java to native type translation table:
     * native id of symbol in cursorfont(from cursorfont.h),
     * commented Java cursor type
     */
    static final int [] predefined = {
            XC_top_left_arrow, /*DEFAULT_CURSOR*/
            XC_cross, /*CROSSHAIR_CURSOR*/
            XC_xterm, /*TEXT_CURSOR*/
            XC_watch, /*WAIT_CURSOR*/
            XC_bottom_left_corner, /*SW_RESIZE_CURSOR*/
            XC_bottom_right_corner, /*SE_RESIZE_CURSOR*/
            XC_top_left_corner, /*NW_RESIZE_CURSOR*/
            XC_top_right_corner, /*NE_RESIZE_CURSOR*/
            XC_top_side, /*N_RESIZE_CURSOR*/
            XC_bottom_side, /*S_RESIZE_CURSOR*/
            XC_left_side, /*W_RESIZE_CURSOR*/
            XC_right_side, /*E_RESIZE_CURSOR*/
            XC_hand2, /*HAND_CURSOR*/
            XC_fleur, /*MOVE_CURSOR*/

    };
    /**
     * @see org.apache.harmony.awt.wtk.CursorFactory#createCursor(int)
     */
    public NativeCursor createCursor(int type) {
        if (type >= 0 && type < predefined.length) {
            long cursor = x11.XCreateFontCursor(display, predefined[type]);
            return new LinuxCursor(cursor, display);
        }
        return null;
    }

    /**
     * @see org.apache.harmony.awt.wtk.CursorFactory#createCustomCursor(java.awt.Image, int, int)
     */
    public NativeCursor createCustomCursor(Image img, int xHotSpot, int yHotSpot) {

        int width = img.getWidth(null);
        int height = img.getHeight(null);
        BufferedImage bufImg = Utils.getBufferedImage(img);
        if(bufImg == null) throw new NullPointerException("Cursor Image is null");

        //must convert image into TYPE_BYTE_BINARY format of depth 1
        BufferedImage bmpSrc = convertTo1Bit(bufImg);
        BufferedImage bmpMask = getMask(bufImg);
        //get pixel data from bufImg & create X11 pixmap
        byte[] bmpSrcData = ((DataBufferByte) bmpSrc.getData().getDataBuffer()).getData();
        byte[] rSrcData = convertToLSBFirst(bmpSrcData);
        byte[] bmpMaskData = ((DataBufferByte) bmpMask.getData().getDataBuffer()).getData();
        byte[] rMaskData = convertToLSBFirst(bmpMaskData);

        ArrayAccessor arrayAccess = AccessorFactory.getArrayAccessor();
        long wnd = factory.getRootWindow();
        LockedArray larr = arrayAccess.lockArrayShort(rSrcData);
        long dataPtr = larr.getAddress();
        long pixmap = x11.XCreateBitmapFromData(display, wnd,
                dataPtr, width, height);
        //System.out.println("source pixmap=" + pixmap);
        larr.release();

        larr = arrayAccess.lockArrayShort(rMaskData);
        dataPtr = larr.getAddress();
        long pixmapMask = x11.XCreateBitmapFromData(display, wnd, dataPtr,
                width, height);
        //System.out.println("mask pixmap=" + pixmap);
        larr.release();
        int fgRGB = bufImg.getRGB(0, 0);
        Color fgColor = new Color(fgRGB);
        Color bkColor = getBkColor(bufImg, fgColor);
        X11.XColor bkColorPtr = getXColor(bkColor);
        X11.XColor fgColorPtr = getXColor(fgColor);
        //then pass this pixmap to x11.XCreatePixmapCursor()
        long cursor = x11.XCreatePixmapCursor(display, pixmap, pixmapMask,
                fgColorPtr, bkColorPtr, xHotSpot, yHotSpot);

        x11.XFreePixmap(display, pixmap);
        x11.XFreePixmap(display, pixmapMask);

        return new LinuxCursor(cursor, display);
    }

    // Convert Bitmap bits to LSBFirst
    private byte[] convertToLSBFirst(byte[] src){
        int len = src.length;
        byte[] dst = new byte[len];

        for(int i = 0; i < len; i++){
            int pix = src[i] & 0xff;
            int rpix = pix & 0x1;
            for( int j = 1; j < 8; j++){
                pix >>= 1;
                rpix <<= 1;
                rpix |= (pix & 0x1) ;
            }
            dst[i] = (byte)rpix;
        }
        return dst;
    }

    /**
     * Select one of bufImg pixel colors as background color
     * when foreground color is fgColor.
     * @param bufImg
     * @param fgColor
     * @return background color
     */
    private Color getBkColor(BufferedImage bufImg, Color fgColor) {
        int w = bufImg.getWidth(), h = bufImg.getHeight();
        float maxError = 0, error = 0;
        Color color, bkColor = fgColor;
        ColorSpace diffSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int rgb = bufImg.getRGB(x, y);
                color = new Color(rgb);
                error = getColorDistance(color, fgColor, diffSpace);
                if (error > maxError) {
                    maxError = error;
                    bkColor = color;
                }
            }
        }
        return bkColor;
    }

    /**
     * Returns square of "distance" between 2 colors in
     * color space cspace
     * @param color1
     * @param color2
     * @param cspace color space where to compare 2 colors
     * @return
     */
    private float getColorDistance(Color color1, Color color2, ColorSpace cspace) {
        float[] c1 = color1.getRGBColorComponents(/*cspace, */null);
        float[] c2 = color2.getRGBColorComponents(/*cspace, */null);

        float sum = 0;
        for (int i=0; i < Math.min(c1.length, c2.length); i++) {
            float diff = c1[i] - c2[i];
            sum += diff * diff;
        }
        return sum;
    }

    /**
     * @param fgRGB
     * @return
     */
    static X11.XColor getXColor(Color color) {
        X11.XColor xcolor = x11.createXColor(false);
        //shift to set MSB for 8-bit R,G,B values
        xcolor.set_green((short)(color.getGreen() << 8));
        xcolor.set_blue((short)(color.getBlue() << 8));
        xcolor.set_red((short)(color.getRed() << 8));

        return xcolor;
    }

    /**
     * Create a mask as 1-bit bitmap from bufImg with alpha.
     * Mask has the same size as bufImg. Mask pixel is 0 for
     * completely transparent pixel in bufImg (alpha=0) and 1 otherwise.
     * @param bufImg
     * @return new BufferedImage containing mask
     */
    static BufferedImage getMask(BufferedImage bufImg) {
        int w = bufImg.getWidth(), h = bufImg.getHeight();
        //WritableRaster alphaRaster = bufImg.getAlphaRaster();
        BufferedImage dstImg = new BufferedImage(w, h,
                BufferedImage.TYPE_BYTE_BINARY);
        WritableRaster raster = dstImg.getRaster();
       //dstImg.setData(alphaRaster);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int rgb = bufImg.getRGB(x, y);
                //dstImg.setRGB(x, y, (rgb & 0xFF000000) != 0 ? 0xFFFFFF : 0x0);
                raster.setPixel(x, y, (rgb & 0xFF000000) != 0 ? new int[] {0x1} : new int[] {0x0});
            }
        }
        return dstImg;
    }

    /**
     * we have to explicitly free system("predefined") cursors on X11
     * because actually they're not "system", but just normal cursors
     */
    protected void finalize() {
        //System.out.println("finalizing system cursors on X11!");
        for (int i = 0; i < systemCursors.length; i++) {
            LinuxCursor cursor = (LinuxCursor) systemCursors[i];
            if (cursor != null) {
                cursor.destroy();
            }
        }
    }

    /**
     * @see org.apache.harmony.awt.wtk.CursorFactory#getBestCursorSize(int, int)
     */
    public Dimension getBestCursorSize(int prefWidth, int prefHeight) {
        long rwidthPtr = memAccess.malloc(4);
        long rheightPtr = memAccess.malloc(4);
        int status = x11.XQueryBestCursor(display, factory.getRootWindow(),
                prefWidth, prefHeight, rwidthPtr, rheightPtr);
        int rwidth = 0, rheight = 0;
        if (status == 1) {
            rwidth = memAccess.getInt(rwidthPtr);
            rheight = memAccess.getInt(rheightPtr);
        }
        memAccess.free(rwidthPtr);
        memAccess.free(rheightPtr);
        return new Dimension(rwidth, rheight);
    }

    /**
     * @see org.apache.harmony.awt.wtk.CursorFactory#getMaximumCursorColors()
     */
    public int getMaximumCursorColors() {
        // TODO query XServer if RENDER extension is supported
        // and return (& use for custom cursors) more than 2 colors in this case
        return 2;
    }

    X11.XColor getXColorByName(String strColor) {
        X11.XColor color = x11.createXColor(false);
        int status = x11.XParseColor(display,
                x11.XDefaultColormap(display, factory.getScreen()),
                strColor, color);
        if (status == 0) {
            // awt.13=Cannot allocate color named '{0}'
            throw new RuntimeException(Messages.getString("awt.13", strColor)); //$NON-NLS-1$
        }
        return color;
    }
    /**
     * Convert buffered image src to 1-bit BYTE_BINARY buffered image.
     * @param src Image to convert
     * @return new Buffered image containing 1-bit bitmap
     */
    static BufferedImage convertTo1Bit(BufferedImage src) {
        if(src.getType() == BufferedImage.TYPE_BYTE_BINARY){
            MultiPixelPackedSampleModel mppsm = (MultiPixelPackedSampleModel) src.getRaster().getSampleModel();
            if(mppsm.getPixelBitStride() == 1) return src;
        }
        int w = src.getWidth(null);
        int h = src.getHeight(null);

        BufferedImage dstImg = new BufferedImage(w, h,
                BufferedImage.TYPE_BYTE_BINARY);
        dstImg.getGraphics().drawImage(src, 0, 0, null);
        return dstImg;

    }
}
