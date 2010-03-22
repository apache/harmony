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

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.VolatileImage;

import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;

/**
 * Windows GraphicsConfiguration implementation
 *
 */
public class WinGraphicsConfiguration extends GraphicsConfiguration {
    private static final Win32 win32 = Win32.getInstance();

    private WinGraphicsDevice device;
    private ColorModel cm;
    private long flags;
    private byte pixelType;

    private int bits = -1;
    private byte redBits = -1;
    private byte redShift = -1;
    private int rmask = -1;
    private byte greenBits = -1;
    private byte greenShift = -1;
    private int gmask = -1;
    private byte blueBits = -1;
    private byte blueShift = -1;
    private int bmask = -1;
    private byte alphaBits = -1;
    private byte alphaShift = -1;
    private int amask = -1;

    private int index;

    public WinGraphicsConfiguration(WinGraphicsDevice device, int index, Win32.PIXELFORMATDESCRIPTOR pfd) {
        this.device = device;
        this.index = index;
        init(pfd);
    }

    public WinGraphicsConfiguration(long hwnd, long hdc) {
        this.device = new WinGraphicsDevice(hwnd);
        this.index = -1;

        int dci = win32.GetPixelFormat(hdc);
        dci = (dci > 0)?dci:1;
        Win32.PIXELFORMATDESCRIPTOR pfd = win32.createPIXELFORMATDESCRIPTOR(false);
        win32.DescribePixelFormat(hdc, dci, pfd.size(), pfd);
        init(pfd);
        pfd.free();
    }

    /**
     * Initializes private fileds with info from
     * native PIXELFORMATDESCRIPTOR structure.
     * 
     * @param pfd PIXELFORMATDESCRIPTOR structure.
     */
    private void init(Win32.PIXELFORMATDESCRIPTOR pfd) {
        flags = pfd.get_dwFlags();
        pixelType = pfd.get_iPixelType();
        bits = pfd.get_cColorBits();

        if (bits == 0) { 
            return; 
        }

        if ((pixelType & WindowsDefs.PFD_TYPE_COLORINDEX) != WindowsDefs.PFD_TYPE_COLORINDEX) {
            redBits = pfd.get_cRedBits();
            redShift = pfd.get_cRedShift();
            rmask = (int)(Math.pow(2,redBits)-1) << redShift;

            greenBits = pfd.get_cGreenBits();
            greenShift = pfd.get_cGreenShift();
            gmask = (int)(Math.pow(2,greenBits)-1) << greenShift;

            blueBits = pfd.get_cBlueBits();
            blueShift = pfd.get_cBlueShift();
            bmask = (int)(Math.pow(2,blueBits)-1) << blueShift;

            alphaBits = pfd.get_cAlphaBits();
            alphaShift = pfd.get_cAlphaShift();
            amask = (int)(Math.pow(2,alphaBits)-1) << alphaShift;
        }

        long hdc = win32.CreateDCW(null, device.getIDstring(), null, null);
        cm = createColorModel(hdc);
        win32.DeleteDC(hdc);

    }

    @Override
    public GraphicsDevice getDevice() {
        return device;
    }

    @Override
    public Rectangle getBounds() {
        return device.getBounds();
    }

    @Override
    public AffineTransform getDefaultTransform() {
        return new AffineTransform();
    }

    @Override
    public AffineTransform getNormalizingTransform() {
        return new AffineTransform();
    }

    @Override
    public BufferedImage createCompatibleImage(int width, int height) {
        return new BufferedImage(cm, cm.createCompatibleWritableRaster(width, height), false, null);
    }

    @Override
    public BufferedImage createCompatibleImage(int width, int height, int transparency) {
        ColorModel cmt = getColorModel(transparency);
        if (cmt == null) {
            // awt.18=Transparency is not supported.
            throw new IllegalArgumentException(Messages.getString("awt.18")); //$NON-NLS-1$
        }

        return new BufferedImage(cmt, cmt.createCompatibleWritableRaster(width, height), false, null);
    }

    @Override
    public ColorModel getColorModel() {
        return cm;
    }

    @Override
    public ColorModel getColorModel(int transparency) {
        switch(transparency){
        case Transparency.BITMASK:
            return new DirectColorModel(25, 0xFF0000, 0xFF00, 0xFF, 0x1000000);
        case Transparency.TRANSLUCENT:
            return ColorModel.getRGBdefault();
        default:
            return cm;
        }
    }

    @Override
    public VolatileImage createCompatibleVolatileImage(int width, int height) {
        return new WinVolatileImage(this, width, height);
    }

    @Override
    public VolatileImage createCompatibleVolatileImage(int width, int height, int transparency) {
        return createCompatibleVolatileImage(width, height);
    }

    public long getFlags() {
        return flags;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WinGraphicsConfiguration)) {
            return false;
        }

        WinGraphicsConfiguration gc = (WinGraphicsConfiguration)obj;

        // We do not use flags now. So GraphicsConfigurations with
        // different flags are same for us.
        //if (flags != gc.flags)
        //  return false;

        if (pixelType != gc.pixelType) {
            return false;
        }

        if (bits != gc.bits) {
            return false;
        }

        if (redBits != gc.redBits) {
            return false;
        }

        if (redShift != gc.redShift) {
            return false;
        }

        if (greenBits != gc.greenBits) {
            return false;
        }

        if (greenShift != gc.greenShift) {
            return false;
        }

        if (blueBits != gc.blueBits) {
            return false;
        }

        if (blueShift != gc.blueShift) {
            return false;
        }

        if (alphaBits != gc.alphaBits) {
            return false;
        }

        if (alphaShift != gc.alphaShift) {
            return false;
        }

        return true;
    }

    public int getIndex() {
        return index;
    }
    
    private native ColorModel createColorModel(long hdc);
}
