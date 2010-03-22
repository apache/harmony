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
package org.apache.harmony.awt.gl.windows;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.apache.harmony.awt.gl.TextRenderer;
import org.apache.harmony.awt.gl.font.CommonGlyphVector;
import org.apache.harmony.awt.gl.font.CompositeFont;
import org.apache.harmony.awt.gl.font.FontPeerImpl;
import org.apache.harmony.awt.gl.font.Glyph;
import org.apache.harmony.awt.gl.font.NativeFont;
import org.apache.harmony.awt.gl.font.WindowsFont;
import org.apache.harmony.awt.internal.nls.Messages;


/**
 * @author Ilya S. Okomin
 *
 * TextRenderer that works by means of GDI+ calls.
 */

public class GDIPTextRenderer extends TextRenderer {

    // GDI Pen object handle 
    long curPen;
    
    // curPen's color 
    int curPenColor;
    
    // Print debug output or not
    protected static final boolean debugOutput = "1".equals(org.apache.harmony.awt.Utils.getSystemProperty("g2d.debug")); //$NON-NLS-1$ //$NON-NLS-2$

    public static final GDIPTextRenderer inst = new GDIPTextRenderer();

    private GDIPTextRenderer() {}

    @SuppressWarnings("deprecation")
    @Override
    public void drawGlyphVector(Graphics2D g, GlyphVector gv, float x, 
            float y) {
        FontPeerImpl fnt = (FontPeerImpl)gv.getFont().getPeer();
        if (fnt.getClass() == CompositeFont.class){
            gdipDrawCompositeGlyphVector(g, gv, x, y);
        } else {
            gdipDrawNormalGlyphVector(g, gv, x, y);
        }
    }

    /**
     * Method to draw GlyphVector created from physical font onto a 
     * specified graphics at desired coordinates.
     * 
     * @param g Graphics to draw onto
     * @param gv GlyphVector to draw
     * @param x starting X coordinate to draw at
     * @param y starting Y coordinate to draw at
     */
    @SuppressWarnings("deprecation")
    public void gdipDrawNormalGlyphVector(Graphics2D g, GlyphVector gv, 
            float x, float y) {
        int len = gv.getNumGlyphs();
        int status = 0;
        long graphicsInfo = ((WinGDIPGraphics2D)g).getGraphicsInfo();
        int flags = gv.getLayoutFlags();
        
        if ((flags & 
                (GlyphVector.FLAG_HAS_POSITION_ADJUSTMENTS |
                        GlyphVector.FLAG_HAS_TRANSFORMS )) == 0){
            gdipDrawNormalChars(g, gv.getFont(), 
                    ((CommonGlyphVector)gv).charVector, len, x, y);
            return;
        } 

        WindowsFont wf = (WindowsFont)gv.getFont().getPeer();
        long font = wf.getFontHandle();
        AffineTransform fontAT = gv.getFont().getTransform();

        double matrix[] = new double[6];
        double fontMatrix[] = new double[6];
        char chars[] = new char[len];
        double positions[] = new double[len*2];
        int count = 0;
        
        AffineTransform at = new AffineTransform(fontAT);
        at.getMatrix(fontMatrix);
        at.preConcatenate(AffineTransform.getTranslateInstance(x, y));

        if (((gv.getLayoutFlags() & GlyphVector.FLAG_HAS_TRANSFORMS) != 0)){

            for (int i=0; i < gv.getNumGlyphs(); i++){

                Glyph gl = ((CommonGlyphVector)gv).vector[i];

                if (gl.getPointWidth()==0) {
                    continue;
                }

                char chr = gl.getChar();

                AffineTransform glyphAT = gv.getGlyphTransform(i);
                if ((glyphAT == null) || glyphAT.isIdentity()){
                    chars[count] = chr;
                    int index = i * 2;
                    int ind = count * 2;
                    positions[ind] = ((CommonGlyphVector)gv).visualPositions[index];
                    positions[ind + 1] = ((CommonGlyphVector)gv).visualPositions[index+1];
                    count++;
                    continue;
                }
                
                if (glyphAT.getType() == AffineTransform.TYPE_TRANSLATION){
                    chars[count] = chr;
                    int index = i * 2;
                    int ind = count * 2;
                    positions[ind] = ((CommonGlyphVector)gv).visualPositions[index] + 
                                glyphAT.getTranslateX();
                    positions[ind + 1] = ((CommonGlyphVector)gv).visualPositions[index+1] +
                                glyphAT.getTranslateY();
                    count++;
                    continue;
                }
                
                at.transform(positions, 0, positions, 0, count);
                
                status = NativeFont.gdiPlusDrawDriverChars(graphicsInfo,
                        chars,
                        count,
                        font,
                        positions,
                        NativeFont.DriverStringOptionsCmapLookup,
                        fontMatrix);
                if (status != 0 && debugOutput){
                    // awt.err.02=GDIPlus DrawDriverString error status = {0}
                    System.err.println(Messages.getString("awt.err.02", status));  //$NON-NLS-1$
                }

                count = 0;
                
                AffineTransform at1 = new AffineTransform(glyphAT);

                at1.concatenate(fontAT);
                at1.getMatrix(matrix);

                Point2D pos = gv.getGlyphPosition(i);
                
                status = NativeFont.gdiPlusDrawDriverChar(graphicsInfo,
                        chr,
                        font,
                        (float)(x + pos.getX()),
                        (float)(y + pos.getY()),
                        NativeFont.DriverStringOptionsCmapLookup,
                        matrix);
                if (status != 0 && debugOutput){
                    // awt.err.02=GDIPlus DrawDriverString error status = {0}
                    System.err.println(Messages.getString("awt.err.02", status)); //$NON-NLS-1$
                }

            }
            if (count != 0){
                
                at.transform(positions, 0, positions, 0, count);

                status = NativeFont.gdiPlusDrawDriverChars(graphicsInfo,
                        chars,
                        count,
                        font,
                        positions,
                        NativeFont.DriverStringOptionsCmapLookup,
                        fontMatrix);
                if (status != 0 && debugOutput){
                    // awt.err.02=GDIPlus DrawDriverString error status = {0}
                    System.err.println(Messages.getString("awt.err.02", status));  //$NON-NLS-1$
                }

            }
        } else {
            for (int i=0; i < len ; i++){
                Glyph gl = ((CommonGlyphVector)gv).vector[i];
                
                if (gl.getPointWidth()==0) {
                    continue;
                }

                chars[count] = gl.getChar();
                int index = i * 2;
                int ind = count * 2;
                positions[ind] = ((CommonGlyphVector)gv).visualPositions[index];
                positions[ind + 1] = ((CommonGlyphVector)gv).visualPositions[index+1];
                count++;
            }

            at.transform(positions, 0, positions, 0, count);

            status = NativeFont.gdiPlusDrawDriverChars(graphicsInfo,
                    chars,
                    count,
                    font,
                    positions,
                    NativeFont.DriverStringOptionsCmapLookup,
                    fontMatrix);
        }

        if (status != 0 && debugOutput){
            // awt.err.02=GDIPlus DrawDriverString error status = {0}
            System.err.println(Messages.getString("awt.err.02", status)); //$NON-NLS-1$
        }
    }

    /**
     * Method to draw GlyphVector created from composite font onto a 
     * specified graphics at desired coordinates.
     * 
     * @param g Graphics to draw onto
     * @param gv GlyphVector to draw
     * @param x starting X coordinate to draw at
     * @param y starting Y coordinate to draw at
     */
    @SuppressWarnings("deprecation")
    public void gdipDrawCompositeGlyphVector(Graphics2D g, GlyphVector gv, 
            float x, float y) {
        int status = 0;

        long graphicsInfo = ((WinGDIPGraphics2D)g).getGraphicsInfo();
        Font fnt =  gv.getFont();
        int len = gv.getNumGlyphs();
        CompositeFont wcf = (CompositeFont)(fnt.getPeer());
        int charFontIndex = wcf.getCharFontIndex(((CommonGlyphVector)gv).charVector[0], 0);
        int fontIndex = charFontIndex;

        int size = len;
        double positions[] = new double[size*2];
        char chars[] = new char[size];
        int count = 0;
        WindowsFont physFont = (WindowsFont)wcf.fPhysicalFonts[charFontIndex];
        
        long font = physFont.getFontHandle();

        AffineTransform fontAT = fnt.getTransform();

        double matrix[] = new double[6];
        double fontMatrix[] = new double[6];

        AffineTransform at = new AffineTransform(fontAT);
        at.getMatrix(fontMatrix);
        at.preConcatenate(AffineTransform.getTranslateInstance(x, y));
        
        for (int i=0; i < len; i++){
            Glyph gl = ((CommonGlyphVector)gv).vector[i];

            if (gl.getPointWidth() ==0){
                continue;
            }
            
            char c = gl.getChar();
            AffineTransform glyphAT = gv.getGlyphTransform(i);
            Point2D pos = gv.getGlyphPosition(i);
            
            fontIndex = wcf.getCharFontIndex(c, 0);

            if ((glyphAT == null) || 
                    glyphAT.isIdentity() ||
                    glyphAT.getType() == AffineTransform.TYPE_TRANSLATION){
                if (fontIndex != charFontIndex) {
                    charFontIndex = fontIndex;

                    at.transform(positions, 0, positions, 0, count);

                    status = NativeFont.gdiPlusDrawDriverChars(graphicsInfo,
                            chars,
                            count,
                            font,
                            positions,
                            NativeFont.DriverStringOptionsCmapLookup,
                            fontMatrix);
                    if (status != 0 && debugOutput){
                        // awt.err.03=gdipDrawCompositeGlyphVector: GDIPlus DrawDriverString error status = {0}
                        System.err.println(Messages.getString("awt.err.03", status));  //$NON-NLS-1$
                    }

                    count = 0;
                    font = gl.getPFont();
                    physFont = (WindowsFont)wcf.fPhysicalFonts[charFontIndex];
                }else {
                    chars[count] = c;
                    int ind = count * 2;
                    positions[ind] = pos.getX();
                    positions[ind + 1] = pos.getY();
                    count++;
                }
                continue;
            } 

            charFontIndex = fontIndex;
            if (count > 0){

                at.transform(positions, 0, positions, 0, count);

                status = NativeFont.gdiPlusDrawDriverChars(graphicsInfo,
                        chars,
                        count,
                        font,
                        positions,
                        NativeFont.DriverStringOptionsCmapLookup,
                        fontMatrix);
                if (status != 0 && debugOutput){
                    // awt.err.04=gdipDrawCompositeGlyphVector: GDIPlus DrawDriverString error status = {0}
                    System.err.println(Messages.getString("awt.err.04", status)); //$NON-NLS-1$
                }
                count = 0;
            }
            
            font = gl.getPFont();

            AffineTransform at1 = new AffineTransform(glyphAT);
            at1.concatenate(fontAT);
            at1.getMatrix(matrix);

            status = NativeFont.gdiPlusDrawDriverChar(graphicsInfo,
                    c,
                    font,
                    (float)(x + pos.getX()),
                    (float)(y + pos.getY()),
                    NativeFont.DriverStringOptionsCmapLookup,
                    matrix);

            if (status != 0 && debugOutput){
                // awt.err.04=gdipDrawCompositeGlyphVector: GDIPlus DrawDriverString error status = {0}
                System.err.println(Messages.getString("awt.err.04", status)); //$NON-NLS-1$
            }
                
        }
        if (count > 0){

            at.transform(positions, 0, positions, 0, count);

            status = NativeFont.gdiPlusDrawDriverChars(graphicsInfo,
                chars,
                count,
                font,
                positions,
                NativeFont.DriverStringOptionsCmapLookup,
                fontMatrix);
        }

    }

    @SuppressWarnings("deprecation")
    @Override
    public void drawString(Graphics2D g, String str, float x, float y) {
        int len = str.length();
        if (len == 0){
            return;
        }

        FontPeerImpl fnt = (FontPeerImpl)g.getFont().getPeer();
        if (fnt.getClass() == CompositeFont.class){
            gdipDrawCompositeString(g, str, x, y);
        } else {
            gdipDrawNormalChars(g, g.getFont(), str.toCharArray(), len, x, y);
        }
    }
    
    /**
     * Method to draw string with graphics that has physical font
     *  at desired coordinates.
     * 
     * @param g Graphics to draw onto
     * @param str String to draw
     * @param x starting X coordinate to draw at
     * @param y starting Y coordinate to draw at
     */
    @SuppressWarnings("deprecation")
    public void gdipDrawNormalString(Graphics2D g, String str, float x, float y) {
        long graphicsInfo = ((WinGDIPGraphics2D)g).getGraphicsInfo();

        WindowsFont wf = (WindowsFont)(g.getFont().getPeer());
        long font = wf.getFontHandle();
        int len = str.length();
        
        char[] chars = new char[len];
        double positions[] = new double[len*2];
        double matrix[] = new double[6];
        AffineTransform at = new AffineTransform(g.getFont().getTransform());
        at.getMatrix(matrix);

        float xPos = 0;
        float yPos = 0;
       
        int count = 0;
        for (int i=0; i < len; i++){
            char c = str.charAt(i);
            Glyph gl = wf.getGlyph(c);

            // We compare Advances because Width
            // of a char can be 0 (e.g. "Space" char)
            float advance = gl.getGlyphPointMetrics().getAdvance(); 
            if ( advance != 0){
                chars[count] = gl.getChar();
                positions[count * 2] = xPos;
                positions[count * 2 + 1] = yPos;
                xPos += advance;
                count++;
            }
        }

        at.preConcatenate(AffineTransform.getTranslateInstance(x, y));
        at.transform(positions, 0, positions, 0, count);

        int status = NativeFont.gdiPlusDrawDriverChars(graphicsInfo,
                chars,
                count,
                font,
                positions,
                NativeFont.DriverStringOptionsCmapLookup,
                matrix);

        if (status != 0 && debugOutput){
            // awt.err.02=GDIPlus DrawDriverString error status = {0}
            System.err.println(Messages.getString("awt.err.02", status));  //$NON-NLS-1$
        }
    }

    /**
     * Method for fast drawing strings at the desired coordinates with Graphics 
     * or Font object has transforms.
     * 
     * @param g Graphics to draw onto
     * @param fnt Font class that is used to draw string
     * @param str String to draw
     * @param len length of the String parameter
     * @param x starting X coordinate to draw at
     * @param y starting Y coordinate to draw at
     */
    @SuppressWarnings("deprecation")
    public void gdipDrawNormalChars(Graphics2D g, Font fnt, char str[], 
            int len, float x, float y) {
        long graphicsInfo = ((WinGDIPGraphics2D)g).getGraphicsInfo();

        WindowsFont wf = (WindowsFont)(fnt.getPeer());
        long font = wf.getFontHandle();
        
        char[] chars = new char[len];
        double positions[] = new double[len*2];
        double matrix[] = new double[6];
        
        AffineTransform at = new AffineTransform(fnt.getTransform());
        at.getMatrix(matrix);
        at.preConcatenate(AffineTransform.getTranslateInstance(x, y));

        float xPos = 0;
        float yPos = 0;
       
        int count = 0;
        for (int i=0; i < len; i++){
            char c = str[i];
            Glyph gl = wf.getGlyph(c);

            // We compare advances because width
            // of a char can be 0 (e.g. "Space" char)
            float advance = gl.getGlyphPointMetrics().getAdvance(); 
            if ( advance != 0){
                chars[count] = gl.getChar();

                positions[count * 2] = xPos;
                positions[count * 2 + 1] = yPos;

                xPos += advance;
                count++;
            }
        }
        at.transform(positions, 0, positions, 0, count);
        
        int status = NativeFont.gdiPlusDrawDriverChars(graphicsInfo,
                chars,
                count,
                font,
                positions,
                NativeFont.DriverStringOptionsCmapLookup,
                matrix);

        if (status != 0 && debugOutput){
            // awt.err.02=GDIPlus DrawDriverString error status = {0}
            System.err.println(Messages.getString("awt.err.02", status));  //$NON-NLS-1$
        }

    }

    /**
     * Method to draw string with graphics that has composite font
     *  at desired coordinates.
     * 
     * @param g Graphics to draw onto
     * @param str String to draw
     * @param x starting X coordinate to draw at
     * @param y starting Y coordinate to draw at
     */
    @SuppressWarnings("deprecation")
    public void gdipDrawCompositeString(Graphics2D g, String str, float x, 
            float y) {
        int len = str.length();

        long graphicsInfo = ((WinGDIPGraphics2D)g).getGraphicsInfo();

        double matrix[] = new double[6];
        AffineTransform at = new AffineTransform(g.getFont().getTransform());
        at.getMatrix(matrix);
        at.preConcatenate(AffineTransform.getTranslateInstance(x, y));

        int size = len;
        double positions[] = new double[size*2];
        char chars[] = new char[size];
        
        CompositeFont wcf = (CompositeFont)(g.getFont().getPeer());
        int charFontIndex = wcf.getCharFontIndex(str.charAt(0), 0);
        int fontIndex = charFontIndex;

        WindowsFont physFont = (WindowsFont)wcf.fPhysicalFonts[charFontIndex];
        
        long font = physFont.getFontHandle();
        float yPos = 0;    // Y position to draw (y - font.ascent)
        float xPos = 0;    // X position to draw
        int count = 0;

        
        for (int i=0; i < len; i++){
            Glyph gl = wcf.getGlyph(str.charAt(i));

            int advance = (int)(gl.getGlyphPointMetrics().getAdvance());
            if (advance ==0){
                continue;
            }
            char c = gl.getChar();

            fontIndex = wcf.getCharFontIndex(c, 0);
            
            if (fontIndex != charFontIndex) {
                charFontIndex = fontIndex;
                
                at.transform(positions, 0, positions, 0, count);

                int status = NativeFont.gdiPlusDrawDriverChars(graphicsInfo,
                        chars,
                        count,
                        font,
                        positions,
                        NativeFont.DriverStringOptionsCmapLookup,
                        matrix);
                if (status != 0 && debugOutput){
                    // awt.err.02=GDIPlus DrawDriverString error status = {0}
                    System.err.println(Messages.getString("awt.err.02", status));  //$NON-NLS-1$
                }

                count = 0;
                font = gl.getPFont();
            } 
            chars[count] = c;
            positions[count*2] = xPos;
            positions[count*2 + 1] = yPos;

            count++;
            xPos += advance;
        }

        at.transform(positions, 0, positions, 0, count);

        int status = NativeFont.gdiPlusDrawDriverChars(graphicsInfo,
              chars,
              count,
              font,
              positions,
              NativeFont.DriverStringOptionsCmapLookup,
              matrix);
      if (status != 0 && debugOutput){
          // awt.err.02=GDIPlus DrawDriverString error status = {0}
          System.err.println(Messages.getString("awt.err.02", status)); //$NON-NLS-1$
      }

    }

}
