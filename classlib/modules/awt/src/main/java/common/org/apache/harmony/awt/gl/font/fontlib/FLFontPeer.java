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

import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;

import org.apache.harmony.awt.gl.font.FontExtraMetrics;
import org.apache.harmony.awt.gl.font.FontManager;
import org.apache.harmony.awt.gl.font.FontPeerImpl;
import org.apache.harmony.awt.gl.font.Glyph;
import org.apache.harmony.awt.gl.font.LineMetricsImpl;

final public class FLFontPeer extends FontPeerImpl {
    private static final boolean USE_CONSTANT_METRICS = false;
    private static final Hashtable<String, FontPeerNativeHandler> handlersTable = new Hashtable<String, FontPeerNativeHandler>();
    private static final Hashtable<Integer, FLGlyph> glyphTable = new Hashtable<Integer, FLGlyph>();
    
    private static long getHandler(String name, int style) {
        String hash = name.concat(String.valueOf(style));
        
        FontPeerNativeHandler handl = handlersTable.get(hash); 
        
        if (handl == null) {
            handl = new FontPeerNativeHandler(name, style);
            
            handlersTable.put(hash, handl);
        }
        
        handl.incriment();
        
        return handl.getHandl();
    }
    
    private static void releaseHandler(String name, int style) {        
        handlersTable.get(name.concat(String.valueOf(style))).decriment();
    }
    
    private static final class FontPeerNativeHandler {
        private long pFont;
        private char count = 0;
        private String name;
        private int style;
        
        FontPeerNativeHandler(String name, int style) {
            this.name = name;
            this.style = style;
        }
        
        void incriment(){            
            if (count == 0) {
                pFont = initFLFontPeer(name, style);
                
                //System.out.println("pFont = " + pFont + "name = " + name + ", style = " + style);
            }
            
            if (pFont == 0) {
                throw new NullPointerException();
            }
            
            count ++;
        }
        
        void decriment(){
            count --;
            
            if (count == 0) {          
                //System.out.println("native dispose " + pFont);
                
                dispose(pFont);                
            }
        }
        
        long getHandl() {
            return pFont;
        }
        
    }
    private int missingGlyphCode = -1;
    
    private Glyph defGlyph;

    public FLFontPeer(String name, int style, int size) {
        super();
//        if (true) throw new  NullPointerException();        
        this.size = size;
        this.style = style;
        this.name = name;
        
        pFont = getHandler(name, style);
        
        getLineMetrics();
        
  /*      if (pFont != 0){
            this.numGlyphs = LinuxNativeFont.getNumGlyphsNative(pFont);
            this.italicAngle = LinuxNativeFont.getItalicAngleNative(pFont, this.fontType);
    }
    
    this.nlm = new LinuxLineMetrics(this, null, " "); //$NON-NLS-1$

    this.ascent = nlm.getLogicalAscent();
    this.descent = nlm.getLogicalDescent();
    this.height = nlm.getHeight();
    this.leading = nlm.getLogicalLeading();
    this.maxAdvance = nlm.getLogicalMaxCharWidth();

    if (this.fontType == FontManager.FONT_TYPE_T1){
        this.defaultChar = 1;
    } else {
        this.defaultChar = 0;
    }

    this.maxCharBounds = new Rectangle2D.Float(0, -nlm.getAscent(), nlm.getMaxCharWidth(), this.height);

*/
        
        maxCharBounds = new Rectangle2D.Float(0, -nlm.getAscent(), nlm.getMaxCharWidth(), nlm.getHeight());
        
        //if (pFont == 0) throw new  NullPointerException();    
        
        //System.out.println("create font size " + size + " style " + style + " name " + name + " pFont " + pFont);
    }

    @Override
    public FontExtraMetrics getExtraMetrics() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LineMetrics getLineMetrics(String str, FontRenderContext frc, AffineTransform at) {
        /*
     * metrics[0] - ascent<p>
     * metrics[1] - descent<p>
     * metrics[2] - external leading<p>
     * metrics[3] - underline thickness<p>
     * -metrics[4] - underline offset<p>
     * metrics[5] - strikethrough thickness<p>
     * -metrics[6] - strikethrough offset<p>
     * metrics[7] - maximum char width<p>*/        
        
        
        /*System.out.println("LineMetrics length " + metrics.length + " Font " + pFont);
        for (int i = 0; i < metrics.length; i ++) {
            System.out.println(metrics[i]);
        }//*/
        
        /**
         * Creates LineMetricsImpl object from specified parameters. If baseline data parameter
         * is null than {0, (-ascent+descent)/2, -ascent} values are used for baseline offsets.
         *  
         * @param _numChars number of chars 
         * @param _baseLineIndex index of the baseline offset
         * @param _baselineOffsets an array of baseline offsets
         * @param _underlineThickness underline thickness
         * @param _underlineOffset underline offset
         * @param _strikethroughThickness strikethrough thickness
         * @param _strikethroughOffset strinkethrough offset
         * @param _leading leading of the font
         * @param _height font height
         * @param _ascent ascent of the font
         * @param _descent descent of the font
         * @param _maxCharWidth max char width
         *
        public LineMetricsImpl(int _numChars, int _baseLineIndex,
                float[] _baselineOffsets, float _underlineThickness,
                float _underlineOffset, float _strikethroughThickness,
                float _strikethroughOffset, float _leading, float _height,
                float _ascent, float _descent, float _maxCharWidth) {*/

//        System.out.println("LineMetricsImpl");
        
         
        LineMetricsImpl lm;
        if (USE_CONSTANT_METRICS) { 
            float height = size;
            float ascent = canDisplay('H') ? 
                    getGlyph('H').getHeight() : 
                        (height *3) /4;
            float descent = canDisplay('p') ? 
                    (float) getGlyph('p').getGlyphMetrics().getBounds2D().getMaxY() :
                        height / 4;
                        
            lm = new LineMetricsImpl(
                    str.length(), //_numChars number of chars 
                    0, //_baseLineIndex index of the baseline offset
                    new float[]{0, (-ascent+descent)/2, -ascent}, //_baselineOffsets an array of baseline offsets
                    ascent/13, //_underlineThickness underline thickness
                    -descent/2, //_underlineOffset underline offset
                    ascent/13, //_strikethroughThickness strikethrough thickness
                    ascent/2, //_strikethroughOffset strinkethrough offset
                    height - ascent- descent, //_leading leading of the font
                    height, //_height font height
                    ascent, //_ascent ascent of the font
                    descent, //_descent descent of the font
                    canDisplay('W') ? getGlyph('W').getWidth() : getGlyph(' ').getWidth()); //_maxCharWidth max char width
            
        } else {
            float[] metrics = getLineMetrics(pFont);
            lm = new LineMetricsImpl(
                    str.length(), //_numChars number of chars 
                    0, //_baseLineIndex index of the baseline offset
                    new float[]{0, (-metrics[0]+metrics[1])*size/2, -metrics[0]*size}, //_baselineOffsets an array of baseline offsets
                    metrics[3]*size, //_underlineThickness underline thickness
                    metrics[4]*size, //_underlineOffset underline offset
                    metrics[5]*size, //_strikethroughThickness strikethrough thickness
                    metrics[6]*size, //_strikethroughOffset strinkethrough offset
                    metrics[2]*size, //_leading leading of the font
                    (metrics[0] + metrics[1] + metrics[2])*size, //_height font height
                    metrics[0]*size, //_ascent ascent of the font
                    metrics[1]*size, //_descent descent of the font
                    metrics[7]*size); //_maxCharWidth max char width
        }
        
                   
  
        if ((at != null) && (!at.isIdentity())){
            lm.scale((float)at.getScaleX(), (float)at.getScaleY());
        }        
        
        return lm;
    }

    @Override
    public String getPSName() {
        psName = getPSName(pFont);
        return psName;
    }

    @Override
    public int getMissingGlyphCode() {   
        if (missingGlyphCode == -1) {
            missingGlyphCode = getMissingGlyphCode(pFont);
        }
        return missingGlyphCode;
    }

    @Override
    public Glyph getGlyph(char ch) {
        Integer id = new Integer((size << 8) + ch); 
        
        if (!glyphTable.containsKey(id)) {
            //System.out.println("size = " + size + ", char " + ch + ", id = " + id);
            glyphTable.put(id, new FLGlyph(ch, pFont, size));
        }
        
        return glyphTable.get(id);
    }

    @Override
    public void dispose() {
        ((FLFontManager) FontManager.getInstance()).removeFontFromHash(this);
        
        //System.out.println("dispose " + pFont);
        
        releaseHandler(name, style);
        
        //dispose(pFont);        
    }

    @Override
    public Glyph getDefaultGlyph() {
        if (defGlyph == null) {
            defGlyph = getGlyph((char)0);
        }
        return defGlyph;
    }

    @Override
    public boolean canDisplay(char c) {        
        return canDisplay(c, pFont);
    }
    
    @Override
    public char getUnicodeByIndex(int glyphCode) {
        return getUnicodeByIndex(glyphCode, pFont);
    }
    
    private static native long initFLFontPeer(String family, int style);
    
    private static native float[] getLineMetrics(long fontPeerPointer);

    private static native String getPSName(long fontPeerPointer);

    private static native int getMissingGlyphCode(long fontPeerPointer);

    private static native void dispose(long fontPeerPointer);    

    private static native boolean canDisplay(char c, long fontPeerPointer);
    
    private static native char getUnicodeByIndex(int glyphCode, long fontPeerPointer);
}
