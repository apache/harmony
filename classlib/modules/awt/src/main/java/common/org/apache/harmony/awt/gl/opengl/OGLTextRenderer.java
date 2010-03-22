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

package org.apache.harmony.awt.gl.opengl;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.harmony.awt.gl.Surface;
import org.apache.harmony.awt.gl.TextRenderer;
import org.apache.harmony.awt.gl.font.CommonGlyphVector;
import org.apache.harmony.awt.gl.font.FontPeerImpl;
import org.apache.harmony.awt.gl.font.Glyph;

import org.apache.harmony.misc.HashCode;

import org.apache.harmony.awt.internal.nls.Messages;

public class OGLTextRenderer extends TextRenderer {    

    private static final TextureCache tx = TextureCache.getInstance();
    private static final GL gl = GL.getInstance();
    
    private static final Set<Character> ESCAPE = new HashSet<Character>();
    static {
        ESCAPE.add(Character.valueOf('\n'));
        ESCAPE.add(Character.valueOf('\r'));
        ESCAPE.add(Character.valueOf('\t'));
    }
    
    private static final SoftHashtable intHash2glyphHash = new SoftHashtable();   
    
    private static final Vector<Integer> toDel = new Vector<Integer>();
    
    private static final Color INVISIBLE_COLOR = new Color(0,0,0,(float)0);
    
    public static final BufferedImage getBufImg(byte[] pixels, int width, int height, Color color) {        
        if (pixels == null) {            
            return new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
        }

        WritableRaster wr = Raster.createPackedRaster(
                new DataBufferByte(pixels, pixels.length), 
                (pixels.length / height) << 3, 
                height, 
                1, 
                null
        );
        
        int[] masColors = new int[]{ 0x000000, color.getRGB()};
        
        IndexColorModel colorModel = new IndexColorModel(1, 2, masColors, 0,true,0,DataBuffer.TYPE_BYTE);  

        BufferedImage bim = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
        
        bim.setData( wr.createWritableChild(0, 0, width, height, 0, 0, null));
        
        return bim;
    }
    
    /**
     * Draws string on specified Graphics at desired position.
     * 
     * @param g specified Graphics2D object
     * @param str String object to draw
     * @param x start X position to draw
     * @param y start Y position to draw
     */
    @Override
    public void drawString(Graphics2D g, String str, float x, float y) {        
        final char[] input = str.toCharArray();
        GlyphMetrics glMetrics;
        Color col = g.getColor();
        Font font = g.getFont();        
        int length = str.length();
        @SuppressWarnings("deprecation")
        final FontPeerImpl peer = ((FontPeerImpl)font.getPeer());
        AffineTransform fontAT = (AffineTransform)font.getTransform().clone();
        Point.Float pos = new Point.Float();
        Paint paint = g.getPaint(); 
        boolean isAntialias = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING) == RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
        
        try {
            fontAT.inverseTransform(new Point.Double(x + fontAT.getTranslateX(), y + fontAT.getTranslateY()), pos);
        } catch (NoninvertibleTransformException e) {   
//          TODO determinant equals 0 => point or line
            g.fill(font.createGlyphVector(g.getFontRenderContext(), str).getOutline(x, y));
            return;
        }
        
        fontAT.translate(pos.x,pos.y);
        g.transform(fontAT);
        
        HashCode hash = new HashCode();
        hash.append(peer);
        hash.append(getFactor(g.getTransform()));
        hash.append(paint);
        hash.append(isAntialias);
        Integer intHash = Integer.valueOf(hash.hashCode());
        
        GlyphHashtable glyphHash = 
            (GlyphHashtable) (intHash2glyphHash.containsKey(intHash) ? 
                intHash2glyphHash.get(intHash) : null);
        if ( glyphHash == null) {
            glyphHash = new GlyphHashtable();
            intHash2glyphHash.put(intHash, glyphHash);
        }
        
        activateVars();
        
        for (int i = 0; i - length < 0; i ++) {
            final char c = input[i];
            final Character ch = Character.valueOf(c);
            if (ESCAPE.contains(ch)) { 
                continue;
            }
            final Glyph glyph = peer.getGlyph(input[i]);
            
            if (c == ' ') {
                glMetrics = glyph.getGlyphPointMetrics();
                gl.glTranslated(
                        glMetrics.getAdvanceX(),
                        glMetrics.getAdvanceY(),
                        0
                );
                continue;
            }
            
            final DLInfo info = glyphHash.containsKey(ch) ? (DLInfo)glyphHash.get(ch) : null;
            
            if (info == null || !info.isValid()) {
                createColorGlyphDL(g, glyph, glyphHash, font, ch, col, isAntialias);
            } else {
                gl.glCallList(info.getDL());
            }            
            
            glMetrics = glyph.getGlyphPointMetrics();
            gl.glTranslated(glMetrics.getAdvanceX(), glMetrics.getAdvanceY(), 0);
        }
        deactivateVars();
        cleanLists();
    }

    /**
     * Draws GlyphVector on specified Graphics at desired position.
     * 
     * @param g specified Graphics2D object
     * @param glyphVector GlyphVector object to draw
     * @param x start X position to draw
     * @param y start Y position to draw
     */
    @Override
    public void drawGlyphVector(Graphics2D g, GlyphVector gv, float x, float y) {
        Color col = g.getColor();
        Glyph[] input = ((CommonGlyphVector)gv).vector;        
        Font font = gv.getFont();
        int length = gv.getNumGlyphs();
        @SuppressWarnings("deprecation")
        final FontPeerImpl peer = ((FontPeerImpl)font.getPeer());
        AffineTransform fontAT = (AffineTransform)font.getTransform().clone();
        Point.Float pos = new Point.Float();
        boolean isAntialias = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING) == RenderingHints.VALUE_TEXT_ANTIALIAS_ON;   
        
        try {
            fontAT.inverseTransform(new Point.Double(x + fontAT.getTranslateX(), y + fontAT.getTranslateY()), pos);
        } catch (NoninvertibleTransformException e) {
            //TODO determinant equals 0 => point or line
            g.fill(gv.getOutline(x, y));
            return;
        }
        
        fontAT.translate(pos.getX(), pos.getY());
        g.transform(fontAT);        
        
        HashCode hash = new HashCode();
        hash.append(peer);
        hash.append(getFactor(g.getTransform()));
        hash.append(g.getPaint());
        hash.append(isAntialias);        
        Integer intHash = new Integer(hash.hashCode());
        
        GlyphHashtable glyphHash = 
            (GlyphHashtable) (intHash2glyphHash.containsKey(intHash) ? 
                intHash2glyphHash.get(intHash) : null);
        if ( glyphHash == null) {
            glyphHash = new GlyphHashtable();
            intHash2glyphHash. put(intHash, glyphHash);
        }        
        
        fontAT = (AffineTransform) font.getTransform().clone();
        activateVars();
        for (int i = 0; i - length < 0; i ++) {
            final char c = input[i].getChar();
            final Character ch = Character.valueOf(c);
            
            if (c == ' ') {
                continue;
            }
            
            final DLInfo info = glyphHash.containsKey(ch) ? (DLInfo)glyphHash.get(ch) : null;
            try {
                fontAT.inverseTransform(gv.getGlyphPosition(i), pos);
            } catch (NoninvertibleTransformException e) {                
            }
            
            gl.glTranslated(pos.x, pos.y, 0);
            if (info == null || !info.isValid()) {                
                createColorGlyphDL(g, input[i], glyphHash, font, ch, col, isAntialias);
            } else {                
                gl.glCallList(info.getDL());
            }                   
            gl.glTranslated(-pos.x, -pos.y, 0);
        }     
        deactivateVars();        
        cleanLists();
    }    
    
    /**
     * @param ogl
     * @param glyph
     * @param glyphHash
     * @param font
     */
    private void createColorGlyphDL(Graphics2D g, Glyph glyph, GlyphHashtable glyphHash, Font font, Character ch, Color col, boolean isAntialias) {
        int base = gl.glGenLists(1);
        if (base == 0) {
            //awt.296 can't allocate memory on video card to create new display list
            throw new NullPointerException(Messages.getString("awt.296"));
        }        
             
        double texSize = getFactor(g.getTransform());
        
        @SuppressWarnings("deprecation")
        final Glyph newGlyph = ((FontPeerImpl)(font.deriveFont(
                (float)(font.getSize2D() * texSize))).getPeer())
                .getGlyph(ch.charValue()); 
        
        byte[] pixels = newGlyph.getBitmap();
        
        BufferedImage bim = getBufImg(
                pixels, 
                newGlyph.getPointWidth(), 
                newGlyph.getPointHeight(),
                col
                );
        
        Rectangle.Double rect = new Rectangle.Double(
                Math.round(glyph.getGlyphPointMetrics().getLSB()), 
                Math.round(-newGlyph.bmp_top  / texSize),
                glyph.getPointWidth(), 
                glyph.getPointHeight()
                );        
        
        Surface sur = Surface.getImageSurface(bim); 
        
        OGLBlitter.OGLTextureParams tp =  OGLBlitter.getInstance().blitImg2OGLTexCached(
                sur,
                bim.getWidth(), 
                bim.getHeight(),
                true
        );
        
        gl.glTexParameteri(GLDefs.GL_TEXTURE_2D, GLDefs.GL_TEXTURE_WRAP_S, GLDefs.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLDefs.GL_TEXTURE_2D, GLDefs.GL_TEXTURE_WRAP_T, GLDefs.GL_CLAMP_TO_EDGE);
        
        if (isAntialias) {
                gl.glTexParameteri(GLDefs.GL_TEXTURE_2D, GLDefs.GL_TEXTURE_MAG_FILTER, GLDefs.GL_LINEAR);
                gl.glTexParameteri(GLDefs.GL_TEXTURE_2D, GLDefs.GL_TEXTURE_MIN_FILTER, GLDefs.GL_LINEAR);
        } else {
                gl.glTexParameteri(GLDefs.GL_TEXTURE_2D, GLDefs.GL_TEXTURE_MAG_FILTER, GLDefs.GL_NEAREST);
                gl.glTexParameteri(GLDefs.GL_TEXTURE_2D, GLDefs.GL_TEXTURE_MIN_FILTER, GLDefs.GL_NEAREST);
        }
        
        double widthFactor = rect.getWidth() / tp.width * tp.p2w;
        double heightFactor = rect.getHeight() / tp.height * tp.p2h;
        
        gl.glNewList(base, GLDefs.GL_COMPILE_AND_EXECUTE);
        
        g.setColor(INVISIBLE_COLOR);        
        
        gl.glBindTexture(GLDefs.GL_TEXTURE_2D, tp.textureName);

        gl.glBegin(GLDefs.GL_QUADS);

        gl.glTexCoord2d(0.0, 0.0); 
        gl.glVertex2d(rect.getX(), rect.getY());
        gl.glTexCoord2d(0.0, rect.getHeight()/heightFactor); 
        gl.glVertex2d(rect.getX(), rect.getY() + rect.getHeight());
        gl.glTexCoord2d(rect.getWidth()/widthFactor,rect.getHeight()/heightFactor); 
        gl.glVertex2d(rect.getWidth() + rect.getX(), rect.getY() + rect.getHeight());
        gl.glTexCoord2d(rect.getWidth()/widthFactor,0.0); 
        gl.glVertex2d(rect.getWidth() + rect.getX(), rect.getY());

        gl.glEnd();
        
        g.setColor(col);        
        
        gl.glEndList();
        
        glyphHash.put(ch, new DLInfo(base, sur));        
        
//        System.out.println("create new dl - " + base);        
    }
    
    private double getFactor(AffineTransform at) {        
        return Math.max(at.getScaleX(), at.getScaleY());
    }
    
    private void cleanLists() {
        synchronized(toDel) {
            if (!toDel.isEmpty()) {            
                 for (Iterator<Integer> iter = toDel.iterator(); iter.hasNext();) {
                    int element = iter.next().intValue();                
                    if (gl.glIsList(element) == GLDefs.GL_TRUE) {    

                        gl.glDeleteLists(element, 1);    
//                        System.err.println("delete dl - " + element);
                    }
                }           
                toDel.clear();
            }
        }
    }    
    
    private void activateVars() {
//        gl.glPixelStorei(GLDefs.GL_UNPACK_ALIGNMENT, 1);      
        gl.glTexEnvf(GLDefs.GL_TEXTURE_ENV, GLDefs.GL_TEXTURE_ENV_MODE, GLDefs.GL_REPLACE);
        
        gl.glTexParameteri(GLDefs.GL_TEXTURE_2D, GLDefs.GL_TEXTURE_WRAP_S, GLDefs.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLDefs.GL_TEXTURE_2D, GLDefs.GL_TEXTURE_WRAP_T, GLDefs.GL_CLAMP_TO_EDGE);

        gl.glTexGeni(GLDefs.GL_S, GLDefs.GL_TEXTURE_GEN_MODE, GLDefs.GL_OBJECT_LINEAR);
        gl.glTexGeni(GLDefs.GL_T, GLDefs.GL_TEXTURE_GEN_MODE, GLDefs.GL_OBJECT_LINEAR);        
        
        gl.glDisable(GLDefs.GL_TEXTURE_GEN_S);
        gl.glDisable(GLDefs.GL_TEXTURE_GEN_T);

        gl.glEnable(GLDefs.GL_TEXTURE_2D);
    }
    
    private void deactivateVars() {
        gl.glDisable(GLDefs.GL_TEXTURE_2D);
    }
    
    private static final class SoftHashtable extends Hashtable<Integer, Object> { 
        private static final long serialVersionUID = 1L;

        @Override
        public Object put(Integer key, Object obj) { 
            @SuppressWarnings("unchecked")
            final SoftReference<GlyphHashtable> ref = (SoftReference<GlyphHashtable>)super.put(key, new SoftReference<GlyphHashtable>((GlyphHashtable) obj)); 
            return ref == null ? null : ref.get(); 
        }
        
        @Override
        public Object get(Object key) { 
            @SuppressWarnings("unchecked")
            final SoftReference<GlyphHashtable> ref = (SoftReference<GlyphHashtable>)super.get(key); 
            return ref == null ? null : ref.get(); 
        } 

        @Override
        public Object remove(Object key) { 
            @SuppressWarnings("unchecked")
            final SoftReference<GlyphHashtable> ref = (SoftReference<GlyphHashtable>)super.remove(key); 
            return ref == null ? null : ref.get();
        } 
    }
    
    private static final class GlyphHashtable extends Hashtable<Character, DLInfo> { 
        private static final long serialVersionUID = 1L;

        @Override
        public void finalize() throws Throwable {
            super.finalize();
            synchronized(toDel) {
                for (Iterator<DLInfo> i = this.values().iterator(); i.hasNext();) {
                    toDel.add(Integer.valueOf(i.next().getDL()));
                }
            }
        }        
    }
    
    private static final class DLInfo {
        private final int dl;
        private final Surface srf;

        DLInfo(int dl, Surface srf) {
            this.dl = dl;
            this.srf = srf;
        }

        int getDL() {
            return dl;
        }

        boolean isValid() {
            if (tx.findTexture(srf) == null) {
                synchronized (toDel) {
                    toDel.add(Integer.valueOf(dl));
                }
                return false;
            }

            return gl.glIsList(dl) == GLDefs.GL_TRUE;
        }
    }

}
