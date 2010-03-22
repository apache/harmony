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
 * @author Ilya S. Okomin
 */
package org.apache.harmony.awt.gl.font;

import java.awt.font.GlyphMetrics;

import java.awt.Rectangle;
import java.awt.Shape;

import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.awt.image.Raster;
import java.lang.Math;

import org.apache.harmony.awt.gl.Utils;
import org.apache.harmony.awt.gl.font.Glyph;
import org.apache.harmony.awt.gl.font.NativeFont;
import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;

/**
 *
 * Windows implementation of the Glyph class
 */
public class WinGlyph extends Glyph{
    
    // Win32 instance 
    private static final Win32 win32 = Win32.getInstance();

    // offset to the POINTFX array
    private static final long TTPOLYCURVE_HEADER_OFFSET = 4;
    
    // offset to the POINTFX array
    private static final long TTPOLYGONHEADER_POINTFX_OFFSET = 8;
    
    /** 
     * Constructor
     */
    public WinGlyph(long pFnt, int fntSize, char c, int glyphIndex) {
        float metrics[];
        int[] pxlMetrics;

        this.pFont = pFnt;
        this.fontSize = fntSize;

        switch (c){
            case '\t':
            case '\r':
            case '\n':
                metrics = new float[6];
                pxlMetrics = new int[6];
                break;
            default:
                metrics = NativeFont.getGlyphInfoNative(this.pFont, c, fntSize);
                pxlMetrics = NativeFont.getGlyphPxlInfoNative(this.pFont, c);
                break;

        }

        Rectangle2D.Float rect  = new Rectangle2D.Float(metrics[0],
                                                        -metrics[1],
                                                        metrics[4],
                                                        metrics[5]);
        this.glMetrics = new GlyphMetrics((float)Math.ceil(metrics[2]), rect, (byte)0);
        this.glCode = glyphIndex;
        this.glChar = c;

        Rectangle rct  = new Rectangle(pxlMetrics[0],
                                                        -pxlMetrics[1],
                                                        pxlMetrics[4],
                                                        pxlMetrics[5]);
        this.glPointMetrics = new GlyphMetrics(pxlMetrics[2], rct, (byte)1);
    }

    /**
     * Default Glyph constructor
     */
    public WinGlyph(char c, int glyphIndex) {
        float metrics[] = new float[6];
        int[] pxlMetrics = new int[6];

        this.pFont = 0;
        this.fontSize = 0;

        Rectangle2D.Float rect  = new Rectangle2D.Float(metrics[0],
                                                        -metrics[1],
                                                        metrics[4],
                                                        metrics[5]);
        this.glMetrics = new GlyphMetrics((float)Math.ceil(metrics[2]), rect, (byte)0);

        this.glCode = glyphIndex;
        this.glChar = c;

        Rectangle rct  = new Rectangle(pxlMetrics[0],
                                                        -pxlMetrics[1],
                                                        pxlMetrics[4],
                                                        pxlMetrics[5]);
        this.glPointMetrics = new GlyphMetrics(pxlMetrics[2], rct, (byte)1);
    }


    @Override
    public byte[] getBitmap(){
        if (this.bitmap == null){
            bitmap = NativeFont.NativeInitGlyphImage(this);
            if (bitmap!= null){
                this.bmp_left = 0;
                this.bmp_rows = this.getPointHeight();
                this.bmp_top = -(int) getGlyphPointMetrics().getBounds2D().getY();
                this.bmp_pitch = bitmap.length / this.bmp_rows;
                this.bmp_width = this.getPointWidth();
            }
        }
        return this.bitmap;
    }

    @Override
    public BufferedImage getImage(){
        if ((this.getWidth()==0) || (this.getHeight()==0)){
            return null;
        }

        byte[] pixels;
        int alignedWidth;
        int height;
        if (this.image == null) {
            pixels = NativeFont.NativeInitGlyphImage(this);

            DataBufferByte dataBuffer = new DataBufferByte(pixels, pixels.length);
            /* Work around:
             *
             * Because of inability to create IndexedColorModel with data, represented as DataBuffer.TYPE_INT
             * Raster with additional width is created to cover all bits, which are extending meaningful bits
             * to the DWORD-aligning. When we want to take an image of the glyph - we have to copy only rectangle
             * that encloses the Glyph from the whole raster.
             *
             * */
            height = (int)this.glPointMetrics.getBounds2D().getHeight();
            alignedWidth = (pixels.length / height) << 3;

            WritableRaster wr = Raster.createPackedRaster(dataBuffer, alignedWidth, height, 1, null);

            byte[] blackWhite = new byte[]{0, (byte)0xff};
            IndexColorModel colorModel = new IndexColorModel(1, 2, blackWhite, blackWhite, blackWhite);

            this.image = new BufferedImage(alignedWidth, height, BufferedImage.TYPE_BYTE_BINARY);
            this.image.setData(wr);
        }

        return this.image;
    }

    @Override
    public Shape initOutline(char c){
        if ((this.getWidth()==0) || (this.getHeight()==0)){
            return new GeneralPath();
        }

        GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD);

        long buffer = 0; // pointer for TTPolygonHeader structure
        int bufSize = 0; // size of buffer

        /* getting size of buffer */
        bufSize = NativeFont.getGlyphOutline(this.pFont, c, buffer, bufSize);

        if (bufSize == 0){
            return gp;
        }
        buffer = Utils.memaccess.malloc(bufSize);

        /* getting filled TTPolygonHeader structure */
        int size = NativeFont.getGlyphOutline(this.pFont, c, buffer, bufSize);

        if (size == 0){
            Utils.memaccess.free(buffer);
            return gp;
        }
        /* parsing TTPolygonHeader to the set of curves*/
        Win32.TTPOLYGONHEADER polygon = win32.createTTPOLYGONHEADER(buffer);
        long ptr = polygon.lock();
        polygon.unlock();
        long offs = ptr;

        while(bufSize > Win32.TTPOLYGONHEADER.sizeof ){
            int curvesize = polygon.get_cb();
            bufSize -= curvesize;
            if (bufSize < 0){
                /* Incorrect buffer structure */
                Utils.memaccess.free(buffer);
                return gp;
            }

            if (polygon.get_dwType() != WindowsDefs.TT_POLYGON_TYPE)
            {
                /* Polygon type isn't a TT_POLYGON_TYPE */
                Utils.memaccess.free(buffer);
                return gp;
            }

            /* Set up starting point */
            float coords[] = getPoints(offs + TTPOLYGONHEADER_POINTFX_OFFSET, 1);
            float x = coords[0];
            float y = coords[1];
            gp.moveTo(x, y);
//          System.out.println("AddPoint [" + x + "," + y + "]");

            /* Obtaining first curve in a list of the polygon's curves */
            long curveOffs =  offs + curvesize;
            offs += Win32.TTPOLYGONHEADER.sizeof;

            Win32.TTPOLYCURVE curve = win32.createTTPOLYCURVE(offs);

            int count;
            while (offs < curveOffs){

                offs += TTPOLYCURVE_HEADER_OFFSET;
                count = curve.get_cpfx();
                coords = getPoints(offs, count);
                switch (curve.get_wType()){
                    /* Current curve segment is Line */
                    case WindowsDefs.TT_PRIM_LINE:
                        for (int i=0; i<count; i++){
                            float x1 = coords[i*2];
                            float y1 = coords[i*2 + 1];
                            gp.lineTo(x1, y1);
//                          System.out.println("AddPoint [" + x1 + "," + y1 + "]");
                        }
                        break;
                    /* Current curve segment is a quadratic Bezier curve */
                    case WindowsDefs.TT_PRIM_QSPLINE:
                        for (int i=0; i < count-1; i++){
                            float x1 = coords[i*2];
                            float y1 = coords[i*2+1];
                            float x2 = coords[(i+1)*2];
                            float y2 = coords[(i+1)*2+1];

                            if ( i==(curve.get_cpfx()-2) ){
                                gp.quadTo(x1, y1, x2, y2);
//                              System.out.println("AddQSpline 1[" + x1 + "," + y1 + "][" + x2 + "," + y2 + "]");
                            } else {
                                gp.quadTo(x1, y1, (x1 + x2) / 2, (y1 + y2) / 2);
//                              System.out.println("AddQSpline 2[" + x1 + "," + y1 + "][" + (x1 + x2)/2 + "," + (y1 + y2)/2 + "]");
                            }
                        }
                        break;

                    /* Current curve segment is a cubic Bezier curve */
                    case WindowsDefs.TT_PRIM_CSPLINE:
                        for (int i=0; i<count; i+=3){
                            float x1 = coords[i*2];
                            float y1 = coords[i*2+1];
                            float x2 = coords[(i+1)*2];
                            float y2 = coords[(i+1)*2+1];;
                            float x3 = coords[(i+2)*2];
                            float y3 = coords[(i+2)*2+1];;
                            gp.curveTo(x1, y1, x2, y2, x3, y3);
//                          System.out.println("AddQSpline [" + x1 + "," + y1 + "][" + x2 + "," + y2 + "][" + x3 + "," + y3 + "]");
                        }

                        break;

                    default:
                        Utils.memaccess.free(buffer);
                        return gp;

                }
                offs += count*Win32.POINTFX.sizeof;
                curve = win32.createTTPOLYCURVE(offs);
            }
            /* Closing the polygon */
            gp.lineTo(x, y);
            /* processing next polygon */
            polygon = win32.createTTPOLYGONHEADER(offs);
        }

        Utils.memaccess.free(buffer);
        gp.closePath();
        return gp;
    }

    /**
     * Returns float array from array of POINTFX elements. Method processes 
     * specified number of elements at once.
     * 
     * @param addr pointer to the memory block, where POINTFX elements stored
     * @param count the total number of elements to process
     */
    public static native float[] getPoints(long addr, int count);
}
