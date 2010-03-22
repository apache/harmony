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

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.ImageCapabilities;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import org.apache.harmony.awt.gl.GLVolatileImage;
import org.apache.harmony.awt.gl.Surface;
import org.apache.harmony.awt.gl.windows.BitmapSurface;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.wtk.NativeWindow;


/**
 * Volatile image for Windows
 *
 */
public class WinVolatileImage extends GLVolatileImage {
    private final long hwnd;
    long gi;
    private final int width;
    private final int height;
    Surface surface;
    
    private WinGraphicsConfiguration gc = null;
    
   /***************************************************************************
    *
    *  Constructors
    *
    ***************************************************************************/
    public WinVolatileImage(NativeWindow nw, int width, int height) {
        if (width <= 0 || height <= 0) {
            // awt.19=Illegal size of volatile image.
            throw new IllegalArgumentException(Messages.getString("awt.19")); //$NON-NLS-1$
        }
        hwnd = nw.getId();
        this.width = width;
        this.height = height;
        if(WinGraphicsDevice.useGDI)
            gi = WinGDIGraphics2D.createCompatibleImageInfo(hwnd, width, height);
        else
            gi = WinGDIPGraphics2D.createCompatibleImageInfo(hwnd, width, height);
        surface = new BitmapSurface(gi , width, height);
    }

    public WinVolatileImage(WinGraphicsConfiguration gc, int width, int height) {
        if (width <= 0 || height <= 0) {
            // awt.19=Illegal size of volatile image.
            throw new IllegalArgumentException(Messages.getString("awt.19")); //$NON-NLS-1$
        }
        
        hwnd = 0;
        this.gc = gc;
        this.width = width;
        this.height = height;
        if(WinGraphicsDevice.useGDI)
            gi = WinGDIGraphics2D.createCompatibleImageInfo(((WinGraphicsDevice)gc.getDevice()).getIDBytes(), width, height);
        else 
            gi = WinGDIPGraphics2D.createCompatibleImageInfo(((WinGraphicsDevice)gc.getDevice()).getIDBytes(), width, height);
        surface = new BitmapSurface(gi , width, height);
    }

    @Override
    public boolean contentsLost() {
        return gi == 0;
    }

    @Override
    public Graphics2D createGraphics() {
        if (gi == 0) {
            if (hwnd != 0 && gc == null) {
                if(WinGraphicsDevice.useGDI)
                    gi = WinGDIGraphics2D.createCompatibleImageInfo(hwnd, width, height);
                else 
                    gi = WinGDIPGraphics2D.createCompatibleImageInfo(hwnd, width, height);
            } else if (hwnd == 0 && gc != null) {
                if(WinGraphicsDevice.useGDI)
                    gi = WinGDIGraphics2D.createCompatibleImageInfo(((WinGraphicsDevice)gc.getDevice()).getIDBytes(), width, height);
                else
                    gi = WinGDIPGraphics2D.createCompatibleImageInfo(((WinGraphicsDevice)gc.getDevice()).getIDBytes(), width, height);
            }
        }
        if(WinGraphicsDevice.useGDI)
            return new WinGDIGraphics2D(this, width, height);
        return new WinGDIPGraphics2D(this, width, height);
    }

    @Override
    public ImageCapabilities getCapabilities() {
        return new ImageCapabilities(false);
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public BufferedImage getSnapshot() {
        return null;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int validate(GraphicsConfiguration gc) {
        if (gi != 0) {
            return IMAGE_OK;
        }
        
        if(WinGraphicsDevice.useGDI)
            gi = WinGDIGraphics2D.createCompatibleImageInfo(((WinGraphicsDevice)gc.getDevice()).getIDBytes(), width, height);        
        else 
            gi = WinGDIPGraphics2D.createCompatibleImageInfo(((WinGraphicsDevice)gc.getDevice()).getIDBytes(), width, height);        
        return IMAGE_RESTORED;
    }

    @Override
    public Object getProperty(String name, ImageObserver observer) {
        return UndefinedProperty;
    }

    @Override
    public int getWidth(ImageObserver observer) {
        return width;
    }

    @Override
    public int getHeight(ImageObserver observer) {
        return height;
    }

    @Override
    protected void finalize() throws Throwable {
        flush();
        super.finalize();
    }

    @Override
    public Surface getImageSurface(){
        return surface;
    }

    @Override
    public void flush() {
        if (gi != 0) {
            if(WinGraphicsDevice.useGDI)
                WinGDIGraphics2D.disposeGraphicsInfo(gi);
            else 
                WinGDIPGraphics2D.disposeGraphicsInfo(gi);
            gi = 0;
        }
        if (surface != null) surface.dispose();
        super.flush();
    }
    
    long getHWND(){
        return hwnd;
    }
    
    WinGraphicsConfiguration getGraphicsConfiguration(){
        return gc;
    }
}
