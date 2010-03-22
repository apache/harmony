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

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Locale;

import org.apache.harmony.awt.gl.font.FontPeerImpl;
import org.apache.harmony.awt.gl.font.Glyph;
import org.apache.harmony.awt.gl.font.LineMetricsImpl;
import org.apache.harmony.awt.internal.nls.Messages;

/**
 * Windows platform font peer implementation based on GDI font object.
 */
public class WindowsFont extends FontPeerImpl{
    
    // table with loaded cached Glyphs
    private final Hashtable<Integer, WinGlyph> glyphs = new Hashtable<Integer, WinGlyph>();
    // table with loaded glyph codes
    private final Hashtable<Integer, Integer> glyphCodes = new Hashtable<Integer, Integer>();

    // Pairs of [begin, end],[..].. unicode ranges values 
    private int[] fontUnicodeRanges;

    public WindowsFont(String fontName, int fontStyle, int fontSize) {
        this.size = fontSize;
        this.style = fontStyle;
        this.name = fontName;
        this.fontFamilyName = name;
        
        pFont = NativeFont.initializeFont(this, fontName, fontStyle, fontSize);

        initWindowsFont();

    }

    public void initWindowsFont(){

        this.fontType = NativeFont.getFontType(name, style);

        // Font Metrics init
        getDefaultLineMetrics();

        this.ascent = nlm.getLogicalAscent();
        this.descent = nlm.getLogicalDescent();
        this.height = (int)nlm.getHeight();
        this.leading = nlm.getLogicalLeading();
        this.maxAdvance = nlm.getLogicalMaxCharWidth();

        this.maxCharBounds = new Rectangle2D.Float(0, -nlm.getAscent(), nlm.getMaxCharWidth(), nlm.getHeight());
        this.italicAngle = NativeFont.getItalicAngleNative(pFont);

        this.numGlyphs = 0;
        //TODO: implement NativeFont.getNumGlyphs(pFont, fontType);

        if (this.fontType == FontManager.FONT_TYPE_T1){
            this.defaultChar = 1;
        }
//      this.defaultChar = NativeFont.getDefaultCharNative(pFont);

//        addGlyphs((char) 0x20, (char) 0x7E);
    }

    /**
     * Returns true if the specified character can be displayed by this
     * WindowsFont. 
     * @param chr the specified character
     */
    @Override
    public boolean canDisplay(char chr) {
        return isGlyphExists(chr);
    }

    /**
     * Adds range of existing glyphs to this WindowsFont object
     * @param uFirst the lowest range's bound, inclusive 
     * @param uLast the highest range's bound, exclusive
     */
    
    public void addGlyphs(char uFirst, char uLast) {
        char index = uFirst;
        if (uLast < uFirst) {
            // awt.09=min range bound value is grater than max range bound
            throw new IllegalArgumentException(Messages.getString("awt.09")); //$NON-NLS-1$
        }
        while (index < uLast) {
            addGlyph(index);
            index++;
        }
    }

    /**
     * Add existing glyph to this WindowsFont object.
     * @param uChar the specified character
     * @return true if glyph of the specified character exists in this
     * WindowsFont or this character is escape sequence character.
     */
    public boolean addGlyph(char uChar) {
        boolean result = false;
        boolean isEscape = false;

        isEscape = ((uChar == '\t') || (uChar == '\n') || (uChar == '\r'));

        if (isEscape || this.isGlyphExists(uChar)) {
                glyphs.put(Integer.valueOf(uChar), new WinGlyph(this.pFont,
                        this.getSize(), uChar, NativeFont.getGlyphCodeNative(
                        this.pFont, uChar)));
                result = true;
        }

        return result;
    }
     /**
      * Checks whether given Glyph belongs to any font supported unicode range.
      * @param uIndex specified character
      * @return true if specified character in unicode ranges, false otherwise
      */
    public boolean isGlyphExists(char uIndex) {
        for (int i = 0; i < fontUnicodeRanges.length - 1; i += 2) {
            if (uIndex <= fontUnicodeRanges[i + 1]) {
                if (uIndex >= fontUnicodeRanges[i]) {
                    return true;
                }
                return false;
            }
        }

        return false;
    }
    
    /**
     *  Returns an array of unicode ranges that are supported by this WindowsFont. 
     */
    public int[] getUnicodeRanges() {
        int[] ranges = new int[fontUnicodeRanges.length];
        System.arraycopy(fontUnicodeRanges, 0, ranges, 0,
                fontUnicodeRanges.length);
        return ranges;
    }
    
    /**
     * Initializes the array of unicode ranges that are supported by this 
     * WindowsFont with the values from the specified array.
     * @param ranges the array of unicode ranges values 
     */
    public void setUnicodeRanges(int[] ranges) {
        if (ranges != null){
            fontUnicodeRanges = new int[ranges.length];
            System.arraycopy(ranges, 0, fontUnicodeRanges, 0, ranges.length);
        }
    }

    /**
     * Returns Glyph object for the specified character in this WindowsFont.
     * Default Glyph object returned if the specified character 
     * doesn't exist in the WindowsFont  
     * @param index the specified character
     */
    @Override
    public Glyph getGlyph(char index) {
        Glyph result = null;
        Integer key = Integer.valueOf(index);
        if (glyphs.containsKey(key)) {
            result = glyphs.get(key);
        } else {
            if (this.addGlyph(index)) {
                result = glyphs.get(key);
            } else {
                result = this.getDefaultGlyph();
            }
        }
        return result;
    }
    
    /**
     * Returns default Glyph object of this WindowsFont.
     */
    @Override
    public Glyph getDefaultGlyph() {
        Glyph result;
        Integer defaultKey = Integer.valueOf(defaultChar);

        if (glyphs.containsKey(defaultKey)) {
            result = glyphs.get(defaultKey);
        } else {
            if (this.fontType == FontManager.FONT_TYPE_T1){
                // XXX: !! Type1 has no default glyphs
                glyphs.put(defaultKey, new WinGlyph(defaultChar, defaultChar));
                result = glyphs.get(defaultKey);
            } else {
                glyphs.put(defaultKey, new WinGlyph(this.pFont,
                        this.getSize(), defaultChar, NativeFont.getGlyphCodeNative(this.pFont, defaultChar)));
                result = glyphs.get(defaultKey);
            }
        }

        return result;

    }

    /**
     * Returns string image, that can be blitted onto a BufferedImageGraphics2D.
     * @param str the specified string
     * @return a BufferedImage object that is the representation of the rendered
     * string
     */
    public BufferedImage getStringImage(String str) {
        // !! this method isn't used now, 
        // need to be modified with proper transparency parameters
        int height = this.ascent + this.descent;
        int width;
        int baseXOffset = 0; // X Offset of the Glyph cell along the base line
        int drawXOffset = 0; // X Offset of the Glyph image along the base line
        int drawYOffset = 0;
        Glyph[] gls = this.getGlyphs(str);

        // total width of the glyph vector is equal to the sum of their advances -
        // LSB of the first glyph - RSB of the last glyph
        width = Math.max(-(int) gls[0].getGlyphPointMetrics().getLSB(), 0);
        baseXOffset = Math.max(-(int) gls[0].getGlyphPointMetrics().getLSB(), 0);

        for (Glyph element : gls) {
            width += element.getGlyphPointMetrics().getAdvanceX();
        }

        width += Math
                .max(-(int) gls[gls.length - 1].getGlyphPointMetrics().getRSB(), 0);

        WritableRaster wr = Raster.createPackedRaster(DataBuffer.TYPE_BYTE,
                width, height, 1, 1, null);

        for (Glyph curGlyph : gls) {
            drawYOffset = this.ascent
                    + (int) Math.ceil(curGlyph.getGlyphPointMetrics().getBounds2D()
                            .getY()) - 1;
            drawXOffset = baseXOffset
                    + (int) Math.ceil(curGlyph.getGlyphPointMetrics().getLSB());

            wr.setDataElements(drawXOffset, drawYOffset, ((WinGlyph)curGlyph).getImage()
                    .getRaster());

            baseXOffset += curGlyph.getGlyphPointMetrics().getAdvanceX();
        }

        byte[] blackWhite = new byte[] { 0, (byte) 0xff };
        IndexColorModel colorModel = new IndexColorModel(1, 2, blackWhite,
                blackWhite, blackWhite);

        BufferedImage stringImage = new BufferedImage(colorModel, wr, false,
                null);
        return stringImage;
    }

    // Font Dependent methods

    /**
     * Returns locale dependent family name of this WindowsFont. 
     */
    @Override
    public String getFamily(Locale l) {
        if (this.fontType == FontManager.FONT_TYPE_T1){
            return this.name;
        }

        //TODO: implement
        return this.getFamily();
    }

    /**
     * Returns locale dependent face name of this WindowsFont.
     */
    @Override
    public String getFontName(Locale l) {
        if (this.fontType == FontManager.FONT_TYPE_T1){
            return this.name;
        }

       return this.getFontName();
    }
    
    /**
     * Returns a clone of LineMetrics object that contains metrics of this 
     * WindowsFont.
     */
    private LineMetricsImpl getDefaultLineMetrics(){
        // TODO: implement baseline offsets for TrueType fonts
        if (nlm != null){
            return (LineMetricsImpl)nlm.clone();
        }
        float[] metrics = NativeFont.getNativeLineMetrics(this.getFontHandle(), this.getSize(), false, false, this.getFontType());
        int _numChars = 0;

        nlm = new LineMetricsImpl(_numChars, metrics, null);
        return (LineMetricsImpl)nlm.clone();
    }

    /**
     * Returns a LineMetrics object that contains text metrics of this 
     * WindowsFont.
     */
    @Override
    public LineMetrics getLineMetrics(String str, FontRenderContext frc, AffineTransform at) {
        AffineTransform frcAt = null;
        LineMetricsImpl lm = getDefaultLineMetrics();
        lm.setNumChars(str.length());
        
        if (frc != null)
            frcAt = frc.getTransform();

        if ((at != null) && (!at.isIdentity())){
            if (frcAt != null) 
                at.concatenate(frcAt);
            lm.scale((float)at.getScaleX(), (float)at.getScaleY());
        } else if ((frcAt != null) && (!frcAt.isIdentity())){
            lm.scale((float)frcAt.getScaleX(), (float)frcAt.getScaleY());
        }

        return lm;
    }

    /**
     * Return Font object if it was successfully embedded in the system.
     */
    public static Font embedFont(String absolutePath) throws IOException {
        return NativeFont.embedFont(absolutePath);
    }

    /**
     * Dispose all native resources and deleting temporary font file
     * if this WindowsFont object was created from stream.
     */
    @Override
    public void dispose(){
        if (pFont != 0){
            NativeFont.pFontFree(pFont);
            pFont = 0;

            if (isCreatedFromStream()) {
                NativeFont.RemoveFontResource(getTempFontFileName());
            }
            
        }
    }

    /**
     * Returns postscript name of this WindowsFont.
     */
    @Override
    public String getPSName(){
        if (psName == null){
            // TODO: implement method
            psName = getFontName();
        }
        return psName;
    }

    @Override
    public int getMissingGlyphCode(){
        return getDefaultGlyph().getGlyphCode();
    }

    /**
     * Returns the advance width of the specified char of this WindowsFont. 
     * @param ind the char which width is to be returned
     * @return the advance width of the specified char of this WindowsFont 
     */
    @Override
    public int charWidth(int ind) {
        return charWidth((char)ind);
    }

    /**
     * Returns face name of this WindowsFont.
     */
    @Override
    public String getFontName() {
        if (faceName == null){
            if (this.fontType == FontManager.FONT_TYPE_T1) {
                faceName = getFamily();
            } else {
                faceName = NativeFont.getFontNameNative(this.pFont);
            }
        }

        return faceName;
    }

    /**
     * Returns unicode by index from the 'cmap' table of this font.
     */
    @Override
    public char getUnicodeByIndex(int glyphCode) {
        char result;

        if (glyphCodes.isEmpty()) {
            for (int i = 0; i < fontUnicodeRanges.length - 1; i += 2) {
                for (int j = fontUnicodeRanges[i]; j <= fontUnicodeRanges[i + 1]; j++)
                {
                    int code = NativeFont.getGlyphCodeNative(pFont, (char) j);
                    glyphCodes.put(code, j);
                }
            }
        }

        if (glyphCodes.containsKey(glyphCode)) {
            result = (char) glyphCodes.get(glyphCode).intValue();
        } else {
            result = defaultChar;
        }

        return result;
    }

    /**
     * Returns initiated FontExtraMetrics instance of this WindowsFont.
     */
    @Override
    public FontExtraMetrics getExtraMetrics(){
        if (extraMetrix == null){

            //!! for Type1 fonts 'x' char width used as average char width
            float[] metrics = NativeFont.getExtraMetricsNative(this.getFontHandle(), this.size, this.fontType);
            if (fontType == FontManager.FONT_TYPE_T1){
                metrics[0] = charWidth('x');
            }
            extraMetrix = new FontExtraMetrics(metrics);
        }
        return extraMetrix;
    }   

}