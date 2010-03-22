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
 * @date: Nov 22, 2005
 */

package org.apache.harmony.awt.gl.linux;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.image.IndexColorModel;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.apache.harmony.awt.gl.CommonGraphics2D;
import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.gl.Surface;
import org.apache.harmony.awt.gl.Utils;
import org.apache.harmony.awt.gl.XORComposite;
import org.apache.harmony.awt.gl.font.FontManager;
import org.apache.harmony.awt.gl.font.LinuxNativeFont;
import org.apache.harmony.awt.wtk.NativeWindow;
import org.apache.harmony.awt.nativebridge.Int8Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.Xft;
import org.apache.harmony.awt.nativebridge.linux.X11Defs;

public class XGraphics2D extends CommonGraphics2D {
    private static final X11 x11 = X11.getInstance();
    private static final Xft xft = Xft.getInstance();

    long drawable; // X11 window or pixmap
    long display;

    long xftDraw;

    // Context related
    long gc; // X11 GC for basic drawing
    long imageGC; // X11 GC for image operations
    int argb;

    XGraphicsConfiguration xConfig;

    boolean nativeLines = true;
    boolean nativePaint = true;
    boolean transparentColor = false;
    boolean simpleComposite = true;
    boolean xor_mode = false;

    boolean indexModel = false;

    public XGraphics2D(long drawable, int tx, int ty, MultiRectArea clip) {
        super(tx, ty, clip);
        this.drawable = drawable;
        xConfig = (XGraphicsConfiguration) getDeviceConfiguration();
        display = xConfig.dev.display;
        gc = createGC(display, drawable);

        X11.Visual visual = xConfig.info.get_visual();
        xftDraw = createXftDraw(display, drawable, visual.lock());
        visual.unlock();

        imageGC = createGC(display, drawable);

        //xSetForeground(argb); // Set default foregroung to black

        blitter = XBlitter.getInstance();
        Rectangle bounds = clip.getBounds();
        dstSurf = new XSurface(this, bounds.width, bounds.height);
        if (!FontManager.IS_FONTLIB) {
            jtr = DrawableTextRenderer.inst;
        }

        //setTransformedClip(clip);
        setClip(clip);

        if (xConfig.getColorModel() instanceof IndexColorModel) {
            indexModel = true;
        }
    }

    public XGraphics2D(XVolatileImage image, int tx, int ty, int width, int height) {
        this(image, tx, ty, new MultiRectArea(new Rectangle(width, height)));
    }

    public XGraphics2D(XVolatileImage image, int tx, int ty, MultiRectArea clip) {
        super(tx, ty, clip);
        drawable = image.getPixmap();
        xConfig = (XGraphicsConfiguration) getDeviceConfiguration();
        display = xConfig.dev.display;
        gc = createGC(display, drawable);

        X11.Visual visual = xConfig.info.get_visual();
        xftDraw = createXftDraw(display, drawable, visual.lock());
        visual.unlock();

        imageGC = createGC(display, drawable);

        //xSetForeground(argb); // Set default foregroung to black

        blitter = XBlitter.getInstance();
        Rectangle bounds = clip.getBounds();
        dstSurf = image.getImageSurface();

        if (!FontManager.IS_FONTLIB) {
            jtr = DrawableTextRenderer.inst;
        }

        //setTransformedClip(clip);
        setClip(clip);

        if (xConfig.getColorModel() instanceof IndexColorModel) {
            indexModel = true;
        }
    }

    public XGraphics2D(long drawable, int tx, int ty, int width, int height) {
        this(drawable, tx, ty, new MultiRectArea(new Rectangle(width, height)));
    }

    public XGraphics2D(NativeWindow nwin, int tx, int ty, MultiRectArea clip) {
        this(nwin.getId(), tx, ty, clip);
    }

    public XGraphics2D(NativeWindow nwin, int tx, int ty, int width, int height) {
        this(nwin.getId(), tx, ty, new MultiRectArea(new Rectangle(width, height)));
    }

    public Graphics create() {
        XGraphics2D res = new XGraphics2D(
                drawable,
                origPoint.x, origPoint.y,
                dstSurf.getWidth(), dstSurf.getHeight()
        );
        copyInternalFields(res);
        return res;
    }

    public long createXftDraw(long display, long drawable, long visual){
        long draw = LinuxNativeFont.createXftDrawNative(display, drawable, visual);
        LinuxNativeFont.xftDrawSetSubwindowModeNative(draw, X11Defs.IncludeInferiors);
        return draw;
    }

    private final long createGC(long display, long win) {
        return createGC(display, win, 0L, 0L);
    }

    public GraphicsConfiguration getDeviceConfiguration() {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        return env.getDefaultScreenDevice().getDefaultConfiguration();
    }

    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        x += transform.getTranslateX();
        y += transform.getTranslateY();

        copyArea(display, drawable, drawable, gc, x, y, width, height, dx+x, dy+y);
    }

    // Caller should free native pointer to rects after using it
    private static final X11.XRectangle createXRects(int[] vertices) {
        int rectsSize = (vertices[0]-1) << 1; // sizeof(XRectangle) = 8

        Int8Pointer rects = NativeBridge.getInstance().createInt8Pointer(rectsSize, true);
        int idx = 0;
        for (int i = 1; i < vertices[0]; i+=4) {
            X11.XRectangle r = x11.createXRectangle(rects.getElementPointer(idx));
            r.set_x((short) vertices[i]);
            r.set_y((short) vertices[i+1]);
            r.set_width((short) (vertices[i+2]-vertices[i]+1));
            r.set_height((short) (vertices[i+3]-vertices[i+1]+1));
            idx += r.size();
        }

        return x11.createXRectangle(rects);
    }

    public void setPaint(Paint paint) {
        if (paint == null)
            return;
            
        if (paint instanceof Color) {
            setColor((Color)paint);
            nativePaint = true;
        } else {
            super.setPaint(paint);
            nativePaint = false;
        }
    }

    public void setColor(Color color) {
        if (color == null)
            return;
        super.setColor(color);

        // Get values for XColor
        int argb_val = color.getRGB();
        if (argb_val != argb) {
            // Check if it is a transparent color
            if ((argb_val & 0xFF000000) != 0xFF000000)
                transparentColor = true;
            else
                transparentColor = false;

            xSetForeground(argb_val);
        }
    }

    private void xSetForeground(int argb_val) {
        // XAllocColor doesn't match closest color,
        // get the exact value from ColorModel
        if (indexModel) {
            IndexColorModel icm = (IndexColorModel) xConfig.getColorModel();
            int pixel = ((int[]) icm.getDataElements(argb_val, new int[]{0}))[0];
            argb_val = icm.getRGB(pixel);
        }

        setForeground(display, gc, xConfig.xcolormap, argb_val);

    }

    public void dispose() {
        super.dispose();

        if (xftDraw != 0) {
            LinuxNativeFont.freeXftDrawNative(this.xftDraw);
            xftDraw = 0;
        }

        if(dstSurf instanceof XSurface) 
            dstSurf.dispose();

        if (gc != 0) {
            freeGC(display, gc);
            gc = 0;
        }
        if (imageGC != 0) {
            freeGC(display, imageGC);
            imageGC = 0;
        }
    }

    void setXClip(MultiRectArea mra, long gc) {
        if (mra == null) {
            resetXClip(gc);
        } else {
            int vertices[] = mra.rect;
            int numVert = vertices[0] - 1;
            setClipRectangles(display, gc, 0, 0, vertices, numVert);
        }
    }

    void resetXClip(long gc) {
        setClipMask(display, gc, X11Defs.None);
    }

    void setXftClip(MultiRectArea mra) {
        if (mra == null){
            resetXftClip();
        } else {

            X11.XRectangle clipXRects = createXRects(mra.rect);
            xft.XftDrawSetClipRectangles(xftDraw, 0, 0, clipXRects, mra.getRectCount());
            clipXRects.free();
        }
    }

    void resetXftClip() {
        xft.XftDrawSetClip(xftDraw, 0);
    }

    protected void setTransformedClip(MultiRectArea clip) {
        super.setTransformedClip(clip);
        if (xftDraw != 0) {
            setXftClip(clip);
        }
        if (gc != 0) {
            setXClip(clip, gc);
        }
    }

    void setGCFunction(int func) {
        setFunction(display, gc, func);
    }

    void setImageGCFunction(int func) { // Note: works with imageGC
        setFunction(display, imageGC, func);
    }

    Surface getSurface() {
        return dstSurf;
    }

    public void setStroke(Stroke s) {
        super.setStroke(s);
        if (s instanceof BasicStroke) {
            BasicStroke bs = (BasicStroke) s;
            if (bs.getMiterLimit() != 10.f) { // Check if it is same as in xlib
                nativeLines = false;
                return;
            }

            int line_width = (int)(bs.getLineWidth() + 0.5f);
            int join_style = bs.getLineJoin();
            int cap_style = bs.getEndCap()+1;
            int dash_offset = (int)(bs.getDashPhase() + 0.5f);

            float fdashes[] = bs.getDashArray();

            int len = 0;
            byte bdashes[] = null;

            if(fdashes != null){
                len = fdashes.length;
                bdashes = new byte[len];

                for(int i = 0; i < len; i++){
                    bdashes[i] = (byte)(fdashes[i] + 0.5f);
                }
            }

            setStroke(display, gc, line_width, join_style, cap_style, dash_offset, bdashes, len);

            nativeLines = true;
        } else {
            nativeLines = false;
        }
    }

    public void setTransform(AffineTransform transform) {
        super.setTransform(transform);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        if (
                nativeLines && nativePaint &&
                !transparentColor && simpleComposite
        ) {
            int type = transform.getType();
            if (type < 2) {
              
                int tx = (int) transform.getTranslateX();
                int ty = (int) transform.getTranslateY();

                x1 += tx;
                y1 += ty;
                x2 += tx;
                y2 += ty;

                drawLine(display, drawable, gc, x1, y1, x2, y2);       

                if (xor_mode) {
                    XORComposite xor = (XORComposite)composite;
                    Color xorcolor = xor.getXORColor();
                    xSetForeground(xorcolor.getRGB());
                    drawLine(display, drawable, gc, x1, y1, x2, y2);       
                    xSetForeground(fgColor.getRGB());
                }
            } else {

                float points[] = new float[]{x1, y1, x2, y2};
                transform.transform(points, 0, points, 0, 2);

                x1 = (int)points[0];
                y1 = (int)points[1];
                x2 = (int)points[2];
                y2 = (int)points[3];

                drawLine(display, drawable, gc, x1, y1, x2, y2);       

                if (xor_mode) {
                    XORComposite xor = (XORComposite)composite;
                    Color xorcolor = xor.getXORColor();
                    xSetForeground(xorcolor.getRGB());
                    drawLine(display, drawable, gc, x1, y1, x2, y2);       
                    xSetForeground(fgColor.getRGB());
                }
            }
        } else {
            super.drawLine(x1, y1, x2, y2);
        }
    }

    @Override
    public void drawPolyline(int[] xpoints, int[] ypoints, int npoints) {
        if (
                nativeLines && nativePaint &&
                !transparentColor && simpleComposite
        ) {

            short points[] = new short[npoints << 1];

            int type = transform.getType();
            if (type < 2) {
              
                int tx = (int) transform.getTranslateX();
                int ty = (int) transform.getTranslateY();

                for (int idx = 0, i = 0; i < npoints; i++){
                    points[idx++] = (short)(xpoints[i] + tx);
                    points[idx++] = (short)(ypoints[i] + ty);
                }

                drawLines(display, drawable, gc, points, points.length);
            } else {

                float fpoints[] = new float[npoints << 1];

                for (int idx = 0, i = 0; i < npoints; i++){
                    fpoints[idx++] = xpoints[i];
                    fpoints[idx++] = ypoints[i];
                }

                transform.transform(fpoints, 0, fpoints, 0, npoints);
                for (int i = 0; i < fpoints.length; i++)
                    points[i] = (short)(fpoints[i] + 0.5f);

                drawLines(display, drawable, gc, points, points.length);
            }

            if (xor_mode) {
                XORComposite xor = (XORComposite)composite;
                Color xorcolor = xor.getXORColor();
                xSetForeground(xorcolor.getRGB());
                drawLines(display, drawable, gc, points, points.length);
                xSetForeground(fgColor.getRGB());
            }
        } else {
            super.drawPolyline(xpoints, ypoints, npoints);
        }
    }

    @Override
    public void drawPolygon(int[] xpoints, int[] ypoints, int npoints) {
        if (
                nativeLines && nativePaint &&
                !transparentColor && simpleComposite
        ) {

            short points[] = new short[(npoints << 1) + 2];

            int type = transform.getType();
            if (type < 2) {
              
                int tx = (int) transform.getTranslateX();
                int ty = (int) transform.getTranslateY();

                int idx = 0;
                for (int i = 0; i < npoints; i++){
                    points[idx++] = (short)(xpoints[i] + tx);
                    points[idx++] = (short)(ypoints[i] + ty);
                }
                points[idx++] = (short)(xpoints[0] + tx);
                points[idx++] = (short)(ypoints[0] + ty);

                drawLines(display, drawable, gc, points, points.length);
            } else {

                float fpoints[] = new float[npoints << 1];

                for (int idx = 0, i = 0; i < npoints; i++){
                    fpoints[idx++] = xpoints[i];
                    fpoints[idx++] = ypoints[i];
                }

                transform.transform(fpoints, 0, fpoints, 0, npoints);
                int i = 0;
                for (; i < fpoints.length; i++)
                    points[i] = (short)(fpoints[i] + 0.5f);
                points[i++] = (short)(fpoints[0] + 0.5f);
                points[i++] = (short)(fpoints[1] + 0.5f);

                drawLines(display, drawable, gc, points, points.length);
            }

            if (xor_mode) {
                XORComposite xor = (XORComposite)composite;
                Color xorcolor = xor.getXORColor();
                xSetForeground(xorcolor.getRGB());
                drawLines(display, drawable, gc, points, points.length);
                xSetForeground(fgColor.getRGB());
            }
        } else {
            super.drawPolygon(xpoints, ypoints, npoints);
        }
    }

    @Override
    public void drawPolygon(Polygon polygon) {
        drawPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        if (
                nativeLines && nativePaint &&
                !transparentColor && simpleComposite
        ) {
            int type = transform.getType();
            if (type < 2) {
                x += (int)transform.getTranslateX();
                y += (int)transform.getTranslateY();
                drawRectangle(display, drawable, gc, x, y, width, height);

                if (xor_mode) {
                    XORComposite xor = (XORComposite)composite;
                    Color xorcolor = xor.getXORColor();
                    xSetForeground(xorcolor.getRGB());
                    drawRectangle(display, drawable, gc, x, y, width, height);
                    xSetForeground(fgColor.getRGB());
                }

            } else if (type < 7) {
                float points[] = new float[]{x, y, x + width - 1, y + height - 1};
                transform.transform(points, 0, points, 0, 2);

                if (points[0] < points[2]){
                    x = (int)points[0];
                    width = (int)(points[2] - points[0]) + 1;
                } else {
                    x = (int)points[2];
                    width = (int)(points[0] - points[2]) + 1;
                }

                if (points[1] < points[3]){
                    y = (int)points[1];
                    height = (int)(points[3] - points[1]) + 1;
                } else {
                    y = (int)points[3];
                    height = (int)(points[1] - points[3]) + 1;
                }

                drawRectangle(display, drawable, gc, x, y, width, height);

                if (xor_mode) {
                    XORComposite xor = (XORComposite)composite;
                    Color xorcolor = xor.getXORColor();
                    xSetForeground(xorcolor.getRGB());
                    drawRectangle(display, drawable, gc, x, y, width, height);
                    xSetForeground(fgColor.getRGB());
                }
            } else {
                float fpoints[] = new float[]{x, y, x + width - 1, y, x + width - 1, y + height - 1, x, y + height - 1};
                transform.transform(fpoints, 0, fpoints, 0, 4);

                short points[] = new short[fpoints.length + 2];

                int i = 0;
                for (; i < fpoints.length; i++)
                    points[i] = (short)(fpoints[i] + 0.5f);
                points[i++] = (short)(fpoints[0] + 0.5f);
                points[i++] = (short)(fpoints[1] + 0.5f);

                drawLines(display, drawable, gc, points, points.length);

                if (xor_mode) {
                    XORComposite xor = (XORComposite)composite;
                    Color xorcolor = xor.getXORColor();
                    xSetForeground(xorcolor.getRGB());
                    drawLines(display, drawable, gc, points, points.length);
                    xSetForeground(fgColor.getRGB());
                }
            }
        } else {
            super.drawRect(x, y, width, height);
        }
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int sa, int ea) {
        if (
                nativeLines && nativePaint &&
                !transparentColor && simpleComposite &&
                transform.getType() < 2
        ) {
            x += (int)transform.getTranslateX();
            y += (int)transform.getTranslateY();
            drawArc(
                    display,
                    drawable,
                    gc,
                    x, y,
                    width, height,
                    sa << 6, ea << 6
            );

            if (xor_mode) {
                XORComposite xor = (XORComposite)composite;
                Color xorcolor = xor.getXORColor();
                xSetForeground(xorcolor.getRGB());
                drawArc(
                        display,
                        drawable,
                        gc,
                        x, y,
                        width, height,
                        sa << 6, ea << 6
                );
                xSetForeground(fgColor.getRGB());
            }
        } else {
            super.drawArc(x, y, width, height, sa, ea);
        }
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        drawArc(x, y, width, height, 0, 360);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        if (
                nativeLines && nativePaint &&
                !transparentColor && simpleComposite
        ) {
            int type = transform.getType();
            if (type < 2) {

                x += (int)transform.getTranslateX();
                y += (int)transform.getTranslateY();
                fillRectangle(display, drawable, gc, x, y, width, height);

                if (xor_mode) {
                    XORComposite xor = (XORComposite)composite;
                    Color xorcolor = xor.getXORColor();
                    xSetForeground(xorcolor.getRGB());
                    fillRectangle(display, drawable, gc, x, y, width, height);
                    xSetForeground(fgColor.getRGB());
                }

            } else if (type < 7) {
                float points[] = new float[]{x, y, x + width - 1, y + height - 1};
                transform.transform(points, 0, points, 0, 2);

                if (points[0] < points[2]){
                    x = (int)points[0];
                    width = (int)(points[2] - points[0]) + 1;
                } else {
                    x = (int)points[2];
                    width = (int)(points[0] - points[2]) + 1;
                }

                if (points[1] < points[3]){
                    y = (int)points[1];
                    height = (int)(points[3] - points[1]) + 1;
                } else {
                    y = (int)points[3];
                    height = (int)(points[1] - points[3]) + 1;
                }

                fillRectangle(display, drawable, gc, x, y, width, height);

                if (xor_mode) {
                    XORComposite xor = (XORComposite)composite;
                    Color xorcolor = xor.getXORColor();
                    xSetForeground(xorcolor.getRGB());
                    fillRectangle(display, drawable, gc, x, y, width, height);
                    xSetForeground(fgColor.getRGB());
                }
            } else {
                float points[] = new float[]{x, y, x + width - 1, y, x + width - 1, y + height - 1, x, y + height - 1};
                transform.transform(points, 0, points, 0, 4);

                short spoints[] = new short[points.length];
                for (int i = 0; i < points.length; i++)
                    spoints[i] = (short)(points[i] + 0.5f);
                fillPolygon(display, drawable, gc, spoints, spoints.length);

                if (xor_mode) {
                    XORComposite xor = (XORComposite)composite;
                    Color xorcolor = xor.getXORColor();
                    xSetForeground(xorcolor.getRGB());
                    fillPolygon(display, drawable, gc, spoints, spoints.length);
                    xSetForeground(fgColor.getRGB());
                }
            }
        } else {
            super.fill(new Rectangle(x, y, width, height));
        }
    }

    protected void fillMultiRectAreaColor(MultiRectArea mra) {
        if (
                nativeLines && nativePaint && 
                !transparentColor && simpleComposite
        ) {
            int vertices[] = mra.rect;
            int numVert = vertices[0] - 1;
            fillRectangles(display, drawable, gc, vertices, numVert);

            if (xor_mode) {
                XORComposite xor = (XORComposite)composite;
                Color xorcolor = xor.getXORColor();
                xSetForeground(xorcolor.getRGB());
                fillRectangles(display, drawable, gc, vertices, numVert);
                xSetForeground(fgColor.getRGB());
            }
        } else {
            super.fillMultiRectAreaColor(mra);
        }
    }

    @Override
    public void fillPolygon(Polygon p) {
        fillPolygon(p.xpoints, p.ypoints, p.npoints);
    }

    @Override
    public void fillPolygon(int[] xpoints, int[] ypoints, int npoints ) {
        if (
                nativeLines && nativePaint &&
                !transparentColor && simpleComposite
        ) {

            short points[] = new short[npoints << 1];

            int type = transform.getType();
            if (type < 2) {
              
                int tx = (int) transform.getTranslateX();
                int ty = (int) transform.getTranslateY();

                for (int idx = 0, i = 0; i < npoints; i++){
                    points[idx++] = (short)(xpoints[i] + tx);
                    points[idx++] = (short)(ypoints[i] + ty);
                }

                fillPolygon(display, drawable, gc, points, points.length);
            } else {

                float fpoints[] = new float[npoints << 1];

                for (int idx = 0, i = 0; i < npoints; i++){
                    fpoints[idx++] = xpoints[i];
                    fpoints[idx++] = ypoints[i];

                }
                transform.transform(fpoints, 0, fpoints, 0, npoints);

                for (int i = 0; i < fpoints.length; i++)
                    points[i] = (short)(fpoints[i] + 0.5f);

                fillPolygon(display, drawable, gc, points, points.length);
            }

            if (xor_mode) {
                XORComposite xor = (XORComposite)composite;
                Color xorcolor = xor.getXORColor();
                xSetForeground(xorcolor.getRGB());
                fillPolygon(display, drawable, gc, points, points.length);
                xSetForeground(fgColor.getRGB());
            }
        } else {
            super.fillPolygon(xpoints, ypoints, npoints);
        }
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int sa, int ea) {
        if (
                nativeLines && nativePaint &&
                !transparentColor && simpleComposite &&
                transform.getType() < 2
        ) {
            x += (int)transform.getTranslateX();
            y += (int)transform.getTranslateY();
            fillArc(
                    display,
                    drawable,
                    gc,
                    x, y,
                    width, height,
                    sa << 6, ea << 6
            );

            if (xor_mode) {
                XORComposite xor = (XORComposite)composite;
                Color xorcolor = xor.getXORColor();
                xSetForeground(xorcolor.getRGB());
                fillArc(
                        display,
                        drawable,
                        gc,
                        x, y,
                        width, height,
                        sa << 6, ea << 6
                );
                xSetForeground(fgColor.getRGB());
            }
        } else {
            super.fillArc(x, y, width, height, sa, ea);
        }
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        fillArc(x, y, width, height, 0, 360);
    }

    @Override
    public void setXORMode(Color color) {
        super.setXORMode(color);
        setFunction(display, gc, X11Defs.GXxor);
        xor_mode = true;
        simpleComposite = true;
    }

    @Override
    public void setPaintMode() {
        setComposite(AlphaComposite.SrcOver);
    }

    public void setComposite(Composite composite) {
        super.setComposite(composite);
        xor_mode = false;
        if (composite instanceof AlphaComposite) {
            AlphaComposite acomp = (AlphaComposite) composite;
            int rule = acomp.getRule();
            float srca = acomp.getAlpha();

            switch(rule){
                case AlphaComposite.CLEAR:
                case AlphaComposite.SRC_OUT:
                    setFunction(display, gc, X11Defs.GXclear);                
                    simpleComposite = true;
                    break;

                case AlphaComposite.SRC:
                case AlphaComposite.SRC_IN:
                    if(srca == 0.0f) setFunction(display, gc, X11Defs.GXclear);
                    else setFunction(display, gc, X11Defs.GXcopy);
                    simpleComposite = true;
                    break;

                case AlphaComposite.DST:
                case AlphaComposite.DST_OVER:
                    setFunction(display, gc, X11Defs.GXnoop);                
                    simpleComposite = true;
                    break;

                case AlphaComposite.SRC_ATOP:
                case AlphaComposite.SRC_OVER:
                    setFunction(display, gc, X11Defs.GXcopy);                
                    if(srca == 1.0f){
                        simpleComposite = true;
                    }else{
                        simpleComposite = false;
                    }
                    break;

                case AlphaComposite.DST_IN:
                case AlphaComposite.DST_ATOP:
                    if(srca != 0.0f){
                        setFunction(display, gc, X11Defs.GXnoop);                
                    } else {
                        setFunction(display, gc, X11Defs.GXclear);                
                    }
                    simpleComposite = true;
                    break;

                case AlphaComposite.DST_OUT:
                case AlphaComposite.XOR:
                    if(srca != 1.0f){
                        setFunction(display, gc, X11Defs.GXnoop);                
                    } else {
                        setFunction(display, gc, X11Defs.GXclear);                
                    }
                    simpleComposite = true;
                    break;
            }
        } else {
            simpleComposite = false;
        }
    }

    @Override
    public void drawString(String str, float x, float y) {
        AffineTransform at = (AffineTransform)this.getTransform().clone();
        AffineTransform fontTransform = font.getTransform();
        at.concatenate(fontTransform);

        if (!at.isIdentity()){
            // TYPE_TRANSLATION
            if (at.getType() == AffineTransform.TYPE_TRANSLATION){
                jtr.drawString(this, str,
                        (float)(x+fontTransform.getTranslateX()),
                        (float)(y+fontTransform.getTranslateY()));
                return;
            }
            // TODO: we use slow type of drawing strings when Font object
            // in Graphics has transforms, we just fill outlines. New textrenderer
            // is to be implemented.
            Shape sh = font.createGlyphVector(this.getFontRenderContext(), str).getOutline(x, y);
            fill(sh);
        } else {
            jtr.drawString(this, str, x, y);
        }
    }

    @Override
    public void drawGlyphVector(GlyphVector gv, float x, float y) {

        AffineTransform at = gv.getFont().getTransform();

        double[] matrix = new double[6];
        if ((at != null) && (!at.isIdentity())){

            int atType = at.getType();
            at.getMatrix(matrix);

            // TYPE_TRANSLATION
            if ((atType == AffineTransform.TYPE_TRANSLATION) &&
                ((gv.getLayoutFlags() & GlyphVector.FLAG_HAS_TRANSFORMS) == 0)){
                jtr.drawGlyphVector(this, gv, (int)(x+matrix[4]), (int)(y+matrix[5]));
                return;
            }
        } else {
            if (((gv.getLayoutFlags() & GlyphVector.FLAG_HAS_TRANSFORMS) == 0)){
                jtr.drawGlyphVector(this, gv, x, y);
                return;
            }
        }

        // TODO: we use slow type of drawing strings when Font object
        // in Graphics has transforms, we just fill outlines. New textrenderer
        // is to be implemented.

        Shape sh = gv.getOutline(x, y);
        this.fill(sh);

    }

    @Override
    public void flush(){
        flush(display);
    }

    // Native methods

    // GC methods
    // Creating and Releasing
    private native long createGC(long display, long drawable, long valuemask, long values);
    private native int freeGC(long display, long gc);

    // Setting GC function
    private native int setFunction(long display, long gc, int func);

    // Stroke (line attributes)
    private native int setStroke(long display, long gc, int line_width, int join_style, int cap_style, int dash_offset, byte dashes[], int len);

    // Foreground
    private native int setForeground(long display, long gc, long colormap, int argb_val);

    // Clipping
    private native int setClipMask(long display, long gc, long pixmap);
    private native int setClipRectangles(long display, long gc, int clip_x_origin, int clip_y_origin, int clip_rects[], int num_rects);

    // Drawing methods

    private native int drawArc(long display, long drawable, long gc, int x, int y, int width, int height, int startAngle, int angle);
    private native int drawLine(long display, long drawable, long gc, int x1, int y1, int x2, int y2);       
    private native int drawLines(long display, long drawable, long gc, short points[], int numPoints);
    private native int drawRectangle(long display, long drawable, long gc, int x, int y, int width, int height);

    // Filling methods

    private native int fillRectangles(long display, long drawable, long gc, int vertices[], int numVert);
    private native int fillRectangle(long display, long drawable, long gc, int x, int y, int width, int height);
    private native int fillPolygon(long display, long drawable, long gc, short points[], int numPoints);
    private native int fillArc(long display, long drawable, long gc, int x, int y, int width, int height, int startAngle, int angle);

    private native int copyArea(long display, long src, long dst, long gc, int src_x, int src_y, int width, int height, int dst_x, int dst_y);

    // Send all queued requests to the server

    private native void flush(long display);

}
