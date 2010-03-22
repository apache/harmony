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

import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;
import org.apache.harmony.awt.nativebridge.windows.Win32;

/**
 *
 * TextRenderer that works by means of GDI calls.
 */

public class GDITextRenderer extends TextRenderer {
    
    // GDI Pen object handle 
    long curPen;

    // curPen's color 
    int curPenColor;
    
    // GDI clipped region 
    long hOldGDIRgn = 0;
    
    // Win32 instance
    private final Win32 win32 = Win32.getInstance();
    
    /** GDITextRenderer singleton instance */
    public static final GDITextRenderer inst = new GDITextRenderer();

    private GDITextRenderer() {}

    // Convert from aarrggbb to 00bbggrr
    private int getGDIColor(int argbPix) {
        return ((argbPix & 0x00FF0000) >> 16) |
        (argbPix & 0x0000FF00) | ((argbPix & 0x000000FF) << 16);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void drawGlyphVector(Graphics2D g, GlyphVector gv, float x, 
            float y) {
        FontPeerImpl fnt = (FontPeerImpl)gv.getFont().getPeer();
        if (fnt.getClass() == CompositeFont.class){
            drawCompositeGlyphVector(g, gv, x, y);
        } else {
            drawNormalGlyphVector(g, gv, x, y);
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
    public void drawNormalGlyphVector(Graphics2D g, GlyphVector gv, float x, 
            float y) {
        AffineTransform trans = ((WinGDIGraphics2D)g).getTransform();
        long hdc = ((WinGDIGraphics2D)g).getDC();

        x += trans.getTranslateX();
        y += trans.getTranslateY();

        win32.SetTextColor(hdc, getGDIColor(g.getColor().getRGB()));
        win32.SetBkMode(hdc, WindowsDefs.TRANSPARENT);
        
        WindowsFont wf = (WindowsFont)gv.getFont().getPeer();

        int ascent = wf.getAscent();
        long font = wf.getFontHandle();
        long oldFont = win32.SelectObject(hdc, font);

        for (int i=0; i < gv.getNumGlyphs(); i++){
            Glyph gl = ((CommonGlyphVector)gv).vector[i];
            char chr = gl.getChar();

            if (gl.getPointWidth()==0) {
                continue;
            }

            String sChar = String.valueOf(chr);

            Point2D pos = gv.getGlyphPosition(i);
            win32.TextOutW(hdc, (int)Math.round(x+pos.getX()), 
                    (int)Math.round(y + pos.getY() - ascent), sChar, 1);
        }

        win32.SelectObject(hdc, oldFont);
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
    public void drawCompositeGlyphVector(Graphics2D g, GlyphVector gv, float x, 
            float y) {
        AffineTransform trans = ((WinGDIGraphics2D)g).getTransform();
        long hdc = ((WinGDIGraphics2D)g).getDC();

        x += (int)Math.round(trans.getTranslateX());
        y += (int)Math.round(trans.getTranslateY());

        win32.SetTextColor(hdc, getGDIColor(g.getColor().getRGB()));
        win32.SetBkMode(hdc, WindowsDefs.TRANSPARENT);

        CompositeFont cf = (CompositeFont)gv.getFont().getPeer();

        int ascent = cf.getAscent();

        long font = 0;

        for (int i=0; i < gv.getNumGlyphs(); i++){
            Glyph gl = ((CommonGlyphVector)gv).vector[i];
            char chr = gl.getChar();

            if (gl.getPointWidth()==0) {
                continue;
            }

            String sChar = String.valueOf(chr);

            long glPFont = gl.getPFont();
            if (font != glPFont){
                font = glPFont;
                win32.SelectObject(hdc, font);
            }

            Point2D pos = gv.getGlyphPosition(i);
            win32.TextOutW(hdc, (int)Math.round(x+pos.getX()), 
                    (int)Math.round(y + pos.getY() - ascent), sChar, 1);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void drawString(Graphics2D g, String str, float x, float y) {
        FontPeerImpl fnt = (FontPeerImpl)g.getFont().getPeer();
        if (fnt.getClass() == CompositeFont.class){
            drawCompositeString(g, str, Math.round(x), Math.round(y));
        } else {
            drawNormalString(g, str, Math.round(x), Math.round(y));
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
    public void drawNormalString(Graphics2D g, String str, int x, int y) {
        AffineTransform trans = ((WinGDIGraphics2D)g).getTransform();

        x += (int)Math.round(trans.getTranslateX());
        y += (int)Math.round(trans.getTranslateY());

        WindowsFont wf = (WindowsFont)(g.getFont().getPeer());
        long font = wf.getFontHandle();
        
        long gi = ((WinGDIGraphics2D)g).getGraphicsInfo();
        long hdc = ((WinGDIGraphics2D)g).getDC();

        win32.SelectObject(hdc, font);

        win32.SetTextColor(hdc, getGDIColor(g.getColor().getRGB()));
        win32.SetBkMode(hdc, WindowsDefs.TRANSPARENT);

        /*
         * Work around for escape-subsequences. If e.g. we draw
         * string "\n\uFFFF" - instead of default glyph for \uFFFF
         * GDI draws small vertical rectangle. For this reason we draw
         * chars one by one to avoid this situation.
         *
         * GDI draws all glyphs starting from 0. First 32 glyphs are taken from
         * another font, hence we have to check if the glyph exists in chosen 
         * font and if success draw it, otherwise we draw default glyph, 
         * except esc-subsequence chars.
         * */
        char[] chars = new char[str.length()];
        int j = 0;
        for (int i=0; i < str.length(); i++){
            char c = str.charAt(i);
            Glyph gl = wf.getGlyph(c);

            // We compare Advances because Width
            // of a char can be 0 (e.g. "Space" char)
            if (gl.getGlyphMetrics().getAdvance() != 0){
                chars[j] = gl.getChar();
                j++;
            }
        }
        String out = new String(chars, 0, j);

        win32.TextOutW(hdc, x, y - wf.getAscent(), out, j);

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
    public void drawCompositeString(Graphics2D g, String str, int x, int y) {

        int len = str.length();

        if (len == 0){
            return;
        }

        AffineTransform trans = ((WinGDIGraphics2D)g).getTransform();

        long gi = ((WinGDIGraphics2D)g).getGraphicsInfo();
        long hdc = ((WinGDIGraphics2D)g).getDC();
        
        x += (int)Math.round(trans.getTranslateX());
        y += (int)Math.round(trans.getTranslateY());
        
        win32.SetTextColor(hdc, getGDIColor(g.getColor().getRGB()));
        win32.SetBkMode(hdc, WindowsDefs.TRANSPARENT);

        CompositeFont wcf = (CompositeFont)(g.getFont().getPeer());
        int charFontIndex = wcf.getCharFontIndex(str.charAt(0), 0);
        int fontIndex = charFontIndex;

        WindowsFont physFont = (WindowsFont)wcf.fPhysicalFonts[charFontIndex];
        long font = physFont.getFontHandle();

        win32.SelectObject(hdc, font);

        int ascent = physFont.getAscent(); // Font ascent
        int offs = 0;       // width of substring with the same font header
        int yOffset = y - ascent; // Y offset to draw (y - font.ascent)
        int xOffset = x;    // X offset to draw
        int start = 0;
        int count = 0;
        
        for (int i=0; i < len; i++){
            char c = str.charAt(i);
            Glyph gl = wcf.getGlyph(c);

            int glWidth = Math.round(gl.getGlyphPointMetrics().getAdvance());
            if (glWidth ==0){
                continue;
            }

            fontIndex = wcf.getCharFontIndex(c, 0);
            
            if (fontIndex != charFontIndex){
                charFontIndex = fontIndex;
                win32.TextOutW(hdc, xOffset, yOffset, 
                        str.substring(start, start + count), count);

                xOffset += offs;
                offs = 0;
                count = 1;
                start = i;

                physFont = (WindowsFont)wcf.fPhysicalFonts[charFontIndex];
                font = physFont.getFontHandle();
                win32.SelectObject(hdc, font);
                yOffset = y - physFont.getAscent();
            } else {
                count++;
            }

            offs += glWidth;
        }

        win32.TextOutW(hdc, xOffset, yOffset, 
                str.substring(start, start + count), count);
    }

}
