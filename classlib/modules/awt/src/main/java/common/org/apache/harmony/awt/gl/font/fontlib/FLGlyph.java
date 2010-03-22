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
package org.apache.harmony.awt.gl.font.fontlib;

import java.awt.Shape;
import java.awt.font.GlyphMetrics;

import org.apache.harmony.awt.gl.font.Glyph;

final public class FLGlyph extends Glyph { 
    
    private long glyphPointer;
    
//    private static final JavaShapeRasterizer jsr = new JavaShapeRasterizer();
    
        //this.glMetrics = new GlyphMetrics((float)Math.ceil(metrics[2]), rect, (byte)0);
        /*
            values[0] = - extents.x ; // Glyph Pixels Bounds : X
            values[1] = extents.y ; // Glyph Pixels Bounds : Y
            values[2] = extents.xOff; // Pixels AdvanceX
            values[3] = extents.yOff; // Pixels AdvanceY ?= Ascent+Descent
            values[4] = acbox.xMax-acbox.xMin;  // Glyph Pixels Bounds : width
            values[5] = acbox.yMax-acbox.yMin; // Glyph Pixels Bounds : height
    */

    FLGlyph(char c, long fontPeerPointer, int size) {
        glChar = c;
        glyphPointer = initGlyph(c, size, fontPeerPointer);
    }

    @Override
    public byte[] getBitmap() {
        /*MultiRectArea mra = jsr.rasterize(initOutline(), 0.5);
        
        Rectangle rec = mra.getBounds();
        int w = rec.width;
        int h = rec.height;
        
        System.out.println(" " + w + " " + h);
        if(w <= 0 || h <= 0) {
            return null;
        }
        
        BufferedImage bim = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);        
        
        ((Graphics2D)bim.getGraphics()).draw(mra);
        
//        bim.getRaster().
        
        
        int dbufferLength = w * h;

        DataBufferByte dbuffer = new DataBufferByte(dbufferLength);

        WritableRaster scanRaster = Raster.createInterleavedRaster(dbuffer, w, h, w, 1,new int[]{0}, null);

        /*WritableRaster scanRaster = Raster.createPackedRaster(
                dbuffer, 
                (dbufferLength / h) << 3, 
                h, 
                1, 
                null
        );*
        
        scanRaster.setRect(bim.getRaster());*/
        
//        return dbuffer.getData();
        
        return null;
    }
    
    public Shape initOutline() {
        if (glOutline == null) {
            FLPath path = new FLPath(glyphPointer);
            glOutline = path.getShape();
        }
        
        return glOutline;
    }

    @Override
    public Shape initOutline(char arg0) {
        return initOutline();
    }
    
    @Override
    public GlyphMetrics getGlyphMetrics(){
        if (glMetrics == null) {
            float[] metrics = getGlyphMetrics(glyphPointer);
            
            this.glMetrics = new GlyphMetrics(
                    true, 
                    Math.round(metrics[0]),//metrics[0], 
                    Math.round(metrics[1]),//metrics[1],
                    //new Rectangle2D.Double(initOutline().getBounds2D().getMinX(), initOutline().getBounds2D().getMinY(), initOutline().getBounds2D().getMaxX() + 5, initOutline().getBounds2D().getMaxY()),                    
                    initOutline().getBounds2D(),//new Rectangle2D.Float(metrics[2], -metrics[5]-1,metrics[4]- metrics[2] + 1, metrics[5] - metrics[3] + 1),
                    GlyphMetrics.STANDARD);
        }
        
        return glMetrics;
    }
    
    @Override
    public GlyphMetrics getGlyphPointMetrics(){ 
        return glPointMetrics = getGlyphMetrics();
    }
    
    private native float[] getGlyphMetrics(long glyphPointer);
    
    private native long initGlyph(char c, int size, long fontPeerPointer);
}
