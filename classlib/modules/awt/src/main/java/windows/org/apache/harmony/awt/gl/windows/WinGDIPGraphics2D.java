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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import org.apache.harmony.awt.gl.CommonGraphics2D;
import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.gl.XORComposite;
import org.apache.harmony.awt.gl.font.FontManager;
import org.apache.harmony.awt.gl.font.NativeFont;
import org.apache.harmony.awt.gl.font.fontlib.FLTextRenderer;
import org.apache.harmony.awt.wtk.NativeWindow;


/**
 * Graphics2D implementation for Windows GDI+ library
 *
 */
public class WinGDIPGraphics2D extends CommonGraphics2D {
    private NativeWindow nw = null;
    private long hdc = 0;
    private long gi = 0;
    private char pageUnit = 1;

    private final Dimension size;

    GraphicsConfiguration config = null;

    private WinVolatileImage img = null;

    // These two flags shows are current Stroke and
    // Paint transferred to native objects or not.
    private boolean nativePen = false;
    private boolean nativeBrush = false;

    // This array is used for passing Path data to
    // native code.
    // It is not thread safe.
    // But WTK guys think that Graphics should not
    // be called from different threads
    private float []pathArray = null;
    private float []pathPoints = null;

    private static final long gdipToken;

    static {
        org.apache.harmony.awt.Utils.loadLibrary("gl"); //$NON-NLS-1$

        // GDI+ startup
        gdipToken = gdiPlusStartup();

        // Prepare GDI+ shutdown
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                Runtime.getRuntime().addShutdownHook(new GDIPShutdown());
                return null;
            }
        });
    }

    public WinGDIPGraphics2D(NativeWindow nw, int tx, int ty, MultiRectArea clip) {
        super(tx, ty, clip);
        this.nw = nw;

        Rectangle b = clip.getBounds();
        size = new Dimension(b.width, b.height);

        gi = createGraphicsInfo(this.nw.getId(), tx, ty, b.width, b.height);
        if (!FontManager.IS_FONTLIB) {
            jtr = GDIPTextRenderer.inst;
        }
        dstSurf = new GDISurface(gi);
        blitter = GDIBlitter.getInstance();
        setTransform(getTransform());
    }

    public WinGDIPGraphics2D(NativeWindow nw, int tx, int ty, int width, int height) {
        super(tx, ty);
        this.nw = nw;

        size = new Dimension(width, height);

        gi = createGraphicsInfo(this.nw.getId(), tx, ty, width, height);
        if (!FontManager.IS_FONTLIB) {
            jtr = GDIPTextRenderer.inst;
        }
        dstSurf = new GDISurface(gi);
        blitter = GDIBlitter.getInstance();
        if (debugOutput) {
            System.err.println("WinGDIPGraphics2D("+nw+", "+tx+", "+ty+", "+width+", "+height+")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        }
        setTransform(getTransform());

    }

    public WinGDIPGraphics2D(WinVolatileImage img, int width, int height) {
        this(img, 0, width, height);
    }

    public WinGDIPGraphics2D(WinVolatileImage img, long ogi, int width, int height) {
        super();
        size = new Dimension(width, height);
        this.img = img;
        if (ogi != 0) {
            this.gi = copyImageInfo(ogi);
        } else {
            this.gi = copyImageInfo(img.gi);
        }
        dstSurf = img.getImageSurface();
        blitter = GDIBlitter.getInstance();
        if (!FontManager.IS_FONTLIB) {
            jtr = GDIPTextRenderer.inst;
        }        
        setTransform(getTransform());
    }

    /**
     * Create G2D associated with the specified device context.
     * 
     * @param hdc pointer to DC handle
     * @param pageUnit one of Gdiplus::Unit specifying how to convert Graphics
     *        coordinates to DC coordinates
     * @param width Graphics width
     * @param height Graphics height
     */
    public WinGDIPGraphics2D(final long hdc, final char pageUnit,
                    final int width, final int height) {
        this.hdc = hdc;
        this.pageUnit = pageUnit;
        size = new Dimension(width, height);
        gi = createGraphicsInfoFor(hdc, pageUnit);

        if (!FontManager.IS_FONTLIB) {
            jtr = GDIPTextRenderer.inst;
        }

        dstSurf = new GDISurface(gi);
        blitter = GDIBlitter.getInstance();
    }

    @Override
    public void addRenderingHints(Map<?,?> hints) {
        super.addRenderingHints(hints);
        if (!FontManager.IS_FONTLIB) {
            Object value = this.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            if (value == RenderingHints.VALUE_ANTIALIAS_ON) {
                NativeFont.setAntialiasing(gi,true);
            } else {
                NativeFont.setAntialiasing(gi,false);
            }
        }
    }
    
    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        copyArea(gi, x, y, width, height, dx, dy);
    }

    @Override
    public Graphics create() {
        if (debugOutput) {
            System.err.println("WinGDIPGraphics2D.create()"); //$NON-NLS-1$
        }

        final WinGDIPGraphics2D res;

        if (img != null) {
            res = new WinGDIPGraphics2D(img, gi, size.width, size.height);
        } else if (nw != null) {
            res = new WinGDIPGraphics2D(nw, origPoint.x, origPoint.y,
                            size.width, size.height);
        } else {
            res = new WinGDIPGraphics2D(getDC(), pageUnit, size.width,
                            size.height);
        }

        copyInternalFields(res);
        return res;
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        if (config == null) {
            if (nw != null) {
                config = new WinGraphicsConfiguration(nw.getId(), getDC());
            } else if (img != null) {
                final long hwnd = img.getHWND();
                
                if(hwnd != 0){
                    config = new WinGraphicsConfiguration(hwnd, getDC());
                }else{
                    config = img.getGraphicsConfiguration();
                }
            }
        }

        return config;
    }

    @Override
    protected void fillMultiRectAreaPaint(MultiRectArea mra) {
        if (nativeBrush && composite == AlphaComposite.SrcOver) {
            fillRects(gi, mra.rect, mra.rect[0]-1);
        } else {
            super.fillMultiRectAreaPaint(mra);
        }
    }




    /***************************************************************************
     *
     *  Overriden methods
     *
     ***************************************************************************/

    @Override
    public void setColor(Color color) {
        if (color == null) {
            return;
        }
        super.setColor(color);
        setSolidBrush(gi, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        nativeBrush = true;
        setStroke(getStroke());
    }


    //REMARK: It seems that transform affects paints too
    //REMARK: Think how to implement this
    @Override
    public void setPaint(Paint paint) {
        if (paint == null)
            return;
            
        if (paint instanceof Color) {
            setColor((Color)paint);
        } else {
            this.paint = paint;
            nativeBrush = false;
            if (paint instanceof GradientPaint) {
                GradientPaint p = (GradientPaint)paint;
                if (!p.isCyclic()) {
                    return;
                }
                Color c1 = p.getColor1();
                Color c2 = p.getColor2();
                Point2D p1 = transform.transform(p.getPoint1(), null);
                Point2D p2 = transform.transform(p.getPoint2(), null);
                setLinearGradientBrush(gi, (int)Math.round(p1.getX()), (int)Math.round(p1.getY()), c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha(),
                                           (int)Math.round(p2.getX()), (int)Math.round(p2.getY()), c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha(), p.isCyclic());
                nativeBrush = true;
            }
            setStroke(getStroke());
        }
    }

    @Override
    public void dispose() {
        if (gi == 0) {
            return;
        }
        if (dstSurf instanceof GDISurface) {
            dstSurf.dispose();
        }
        disposeGraphicsInfo(gi);
        gi = 0;
        super.dispose();
        if (debugOutput) {
            System.err.println("WinGDIPGraphics2D.dispose()"); //$NON-NLS-1$
        }
    }

    @Override
    public void drawGlyphVector(GlyphVector gv, float x, float y) {
        jtr.drawGlyphVector(this, gv, x, y);
    }

    @Override
    public void drawString(String str, float x, float y) {
        jtr.drawString(this, str, x, y);
    }

    @Override
    public void setStroke(Stroke stroke) {
        super.setStroke(stroke);
        nativePen = nativeBrush && stroke instanceof BasicStroke;
        if (!nativePen) {
            deletePen(gi);
            return;
        }

        BasicStroke bs = (BasicStroke)stroke;
        float []dash = bs.getDashArray();
        if (dash != null && dash.length % 2 == 1) {
            // If dash len is odd then we need to double the array           
            float []newDash = new float[dash.length*2];
            System.arraycopy(dash, 0, newDash, 0, dash.length);
            System.arraycopy(dash, 0, newDash, dash.length, dash.length);
            dash = newDash;
        }
        setPen(gi, bs.getLineWidth(), bs.getEndCap(), bs.getLineJoin(), bs.getMiterLimit(),
                dash, (dash != null)?dash.length:0, bs.getDashPhase());
    }

    @Override
    public void draw(Shape s) {
        if (!nativePen || composite != AlphaComposite.SrcOver) {
            super.draw(s);
            return;
        }

        PathIterator pi = s.getPathIterator(transform, 0.5);
        int len = getPathArray(pi);
        drawShape(gi, pathArray, len, pi.getWindingRule());
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        if (!nativePen || composite != AlphaComposite.SrcOver) {
            super.drawLine(x1, y1, x2, y2);
            return;
        }

        drawLine(gi, x1, y1, x2, y2);
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        if (!nativePen || composite != AlphaComposite.SrcOver) {
            super.drawRect(x, y, width, height);
            return;
        }

        drawRect(gi, x, y, width, height);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        if (!nativePen || composite != AlphaComposite.SrcOver) {
            super.drawOval(x, y, width, height);
            return;
        }

        drawOval(gi, x, y, width, height);
    }

    @Override
    public void fill(Shape s) {
        if (!nativeBrush || composite != AlphaComposite.SrcOver) {
            super.fill(s);
            return;
        }

        PathIterator pi = s.getPathIterator(transform, 0.5);
        int len = getPathArray(pi);
        fillShape(gi, pathArray, len, pi.getWindingRule());
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        if (!nativeBrush || composite != AlphaComposite.SrcOver) {
            super.fillRect(x, y, width, height);
            return;
        }

        fillRect(gi, x, y, width, height);
    }

    /**
     * Sets native clip to specified area
     * 
     * @param clip Transformed clip to set
     */
    @Override
    protected void setTransformedClip(MultiRectArea clip) {
        super.setTransformedClip(clip);
        if (gi == 0) {
            return;
        }
        if (clip == null) {
            resetClip(gi);
        } else {
            setClip(gi, clip.rect, clip.rect[0]-1);
        }
    }

    /***************************************************************************
    *
    *  Transformation methods
    *
    ***************************************************************************/

    @Override
    public void setTransform(AffineTransform transform) {
        super.setTransform(transform);
        if (gi == 0) {
            return;
        }

        setNativeTransform(gi, matrix);
    }

    @Override
    public void rotate(double theta) {
        super.rotate(theta);

        setNativeTransform(gi, matrix);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        super.rotate(theta, x, y);

        setNativeTransform(gi, matrix);
    }

    @Override
    public void scale(double sx, double sy) {
        super.scale(sx, sy);

        setNativeTransform(gi, matrix);
    }

    @Override
    public void shear(double shx, double shy) {
        super.shear(shx, shy);

        setNativeTransform(gi, matrix);
    }

    @Override
    public void transform(AffineTransform at) {
        super.transform(at);

        setNativeTransform(gi, matrix);
    }

    @Override
    public void translate(double tx, double ty) {
        super.translate(tx, ty);

        setNativeTransform(gi, matrix);
    }

    @Override
    public void translate(int tx, int ty) {
        super.translate(tx, ty);

        setNativeTransform(gi, matrix);
    }

    /***************************************************************************
    *
    *  Class specific methods
    *
    ***************************************************************************/

    /**
     * Returns handle to underlying device context
     */
    public long getDC() {
        if (hdc == 0) {
            hdc = getDC(gi);
        }
        return hdc;
    }

    /**
     * Returns pointer to underlying native GraphicsInfo structure
     *  
     * @return Pointer to GraphicsInfo structure
     */
    public long getGraphicsInfo() {
        return gi;
    }

   /***************************************************************************
    *
    *  Private methods
    *
    ***************************************************************************/
    /**
     * Converts PathIterator into array of int values. This array is
     * stored in pathArray field.
     * Array then used to pass Shape to native drawing routines
     * 
     * @param pi PathIterator recieved from Shape
     * @return Number of result array elements.
     */
    private int getPathArray(PathIterator pi) {
        if (pathArray == null) {
            pathArray = new float[8192];
            pathPoints = new float[6];
        }

        int i = 0;

        while (!pi.isDone()) {
            int seg = pi.currentSegment(pathPoints);
            pathArray[i++] = seg;
            switch (seg) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    pathArray[i++] = pathPoints[0];
                    pathArray[i++] = pathPoints[1];
                    break;
                case PathIterator.SEG_CLOSE:
                    break;
            }
            pi.next();
        }
        return i;
    }

    /***************************************************************************
     *
     *  Native methods
     *
     ***************************************************************************/

    // GDI+ system startup/shutdown methods
    private native static long gdiPlusStartup();
    private native static void gdiPlusShutdown(long token);

    // Creates native GraphicsInfo structure
    private native long createGraphicsInfo(long hwnd, int x, int y, int width, int height);
    private native long createGraphicsInfoFor(long hdc, char pageUnit);
    static native long createCompatibleImageInfo(long hwnd, int width, int height);
    static native long createCompatibleImageInfo(byte[] bytes, int width, int height);
    private native long copyImageInfo(long gi);

    // Releases GraphicsInfo structure
    static native void disposeGraphicsInfo(long gi);

    private native void copyArea(long gi, int x, int y, int width, int height, int dx, int dy);
    
    // Methods to set solid and gradient brushes
    private native void setSolidBrush(long gi, int r, int g, int b, int a);
    private native void setLinearGradientBrush(long gi, int x1, int y1, int r1, int g1, int b1, int a1, int x2, int y2, int r2, int g2, int b2, int a2, boolean cyclic);
    
    // Fills specified rectangles by native brush
    private native void fillRects(long gi, int []vertices, int len);

    private native long getDC(long gi);

    //Pen manipulation routins
    private native boolean setPen(long gi, float lineWidth, int endCap, int lineJoin, float miterLimit, float[] dashArray, int dashLen, float dashPhase);
    private native void deletePen(long gi);

    // Draw/Fill Shape/GraphicsPath
    private native void drawShape(long gi, float []path, int len, int winding);
    private native void fillShape(long gi, float []path, int len, int winding);

    // Draw native primitives
    private native void drawLine(long gi, int x1, int y1, int x2, int y2);
    private native void drawRect(long gi, int x, int y, int width, int height);
    private native void drawOval(long gi, int x, int y, int width, int height);

    // Fill native primitives
    private native void fillRect(long gi, int x, int y, int width, int height);

    @Override
    public void setRenderingHint(RenderingHints.Key key, Object value) {
        super.setRenderingHint(key,value);
        if (!FontManager.IS_FONTLIB) {
            Object val = this.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            if (val == RenderingHints.VALUE_ANTIALIAS_ON) {
                NativeFont.setAntialiasing(gi,true);
            } else {
                NativeFont.setAntialiasing(gi,false);
            }
        }
    }

    @Override
    public void setRenderingHints(Map<?,?> hints) {
        super.setRenderingHints(hints);
        if (!FontManager.IS_FONTLIB) {
            Object value = this.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            if (value == RenderingHints.VALUE_ANTIALIAS_ON) {
                NativeFont.setAntialiasing(gi,true);
            } else {
                NativeFont.setAntialiasing(gi,false);
            }
        }
    }


    // Set native clip
    private native void setClip(long gi, int[] vertices, int len);
    private native void resetClip(long gi);

    // Update native affine transform matrix
    private native void setNativeTransform(long gi, double[] matrix);



    /***************************************************************************
     *
     *  Shutdown class
     *
     ***************************************************************************/
    /**
     * We need to shutdown GDI+ before exit.
     */
    private static class GDIPShutdown extends Thread {
        @Override
        public void run() {
            WinGDIPGraphics2D.gdiPlusShutdown(WinGDIPGraphics2D.gdipToken);
        }
    }
}
