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
package org.apache.harmony.awt.gl.linux;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.RenderingHints;

import org.apache.harmony.awt.gl.CommonGraphics2D;
import org.apache.harmony.awt.gl.TextRenderer;
import org.apache.harmony.awt.gl.font.CommonGlyphVector;
import org.apache.harmony.awt.gl.font.CompositeFont;
import org.apache.harmony.awt.gl.font.FontManager;
import org.apache.harmony.awt.gl.font.FontPeerImpl;
import org.apache.harmony.awt.gl.font.Glyph;
import org.apache.harmony.awt.gl.font.LinuxFont;
import org.apache.harmony.awt.gl.font.LinuxNativeFont;

import org.apache.harmony.awt.nativebridge.linux.X11;

/**
 *
 *  Linux text renderer, works using XFT text rendering
 */
public class DrawableTextRenderer extends TextRenderer {
    
    /** Singleton DrawableTextRenderer instance */
    public static final DrawableTextRenderer inst = new DrawableTextRenderer();

    // X11 instance
    static final X11 x11 = X11.getInstance();

    boolean isAntialiasingHintSet(Graphics2D g){
        Object value = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        return (value == RenderingHints.VALUE_ANTIALIAS_ON);
    }

    public void drawString(Graphics2D ga, String str, float x, float y) {
        CommonGraphics2D g = (CommonGraphics2D)ga;
        AffineTransform trans = g.getTransform();
        double xOffset = x + trans.getTranslateX();
        double yOffset = y + trans.getTranslateY();

        FontPeerImpl fnt = (FontPeerImpl)g.getFont().getPeer();

        if (fnt.getClass() == CompositeFont.class){
            drawCompositeString(g, str, xOffset, yOffset);
        } else {
            drawNormalString(g, str, xOffset, yOffset);
        }

    }
    
    /**
     * Returns XColor structure corresponding to the desired Color.
     * @param color specified Color
     */
    static X11.XColor getXColor(Color color) {
        X11.XColor xcolor = x11.createXColor(false);

        xcolor.set_green((short)(color.getGreen() << 8));
        xcolor.set_blue((short)(color.getBlue() << 8));
        xcolor.set_red((short)(color.getRed() << 8));

        return xcolor;
    }

    /**
     * Method to draw string with graphics that has physical font
     *  at desired coordinates.
     * 
     * @param g CommonGraphics2D to draw onto
     * @param str String to draw
     * @param x starting X coordinate to draw at
     * @param y starting Y coordinate to draw at
     */
    public void drawNormalString(CommonGraphics2D g, String str, double x, 
            double y) {
        XGraphics2D xg2d =  (XGraphics2D)g;

        long display = xg2d.display;
        int screen = xg2d.xConfig.dev.screen;
        long colormap = x11.XDefaultColormap(display, screen);

        LinuxFont peer = (LinuxFont)g.getFont().getPeer();

        X11.XColor xcolor = getXColor(g.getColor());
        long xcolorPtr = xcolor.lock();

        /*
         * Work around for escape-subsequences.
         * We don't draw anything instead of \n,\r,\t
         *
         * */
        char[] outChars = new char[str.length()];
        char[] inChars = str.toCharArray();
        int j = 0;
        for (int i = 0; i < inChars.length; i++){
            if(peer.getFontType() == FontManager.FONT_TYPE_T1){
                if (!peer.isGlyphExists(inChars[i])){
                    continue;
                }
            }

            switch (inChars[i]){
                case '\n':
                case '\r':
                case '\t':
                    break;
                default:
                    outChars[j] = inChars[i];
                    j++;
            }
        }
        if (inChars.length != 0 ){
            LinuxNativeFont.drawStringNative(xg2d.xftDraw, display, colormap, 
                    peer.getFontHandle(isAntialiasingHintSet(g)), (int)Math.round(x), (int)Math.round(y),
                    outChars, j, xcolorPtr);
        }
        xcolor.unlock();

    }

    /**
     * Method to draw string with graphics that has composite font
     *  at desired coordinates.
     * 
     * @param g CommonGraphics2D to draw onto
     * @param str String to draw
     * @param x starting X coordinate to draw at
     * @param y starting Y coordinate to draw at
     */
    public void drawCompositeString(CommonGraphics2D g, String str, double x, 
            double y) {
        XGraphics2D xg2d =  (XGraphics2D)g;

        long display = xg2d.display;
        int screen = xg2d.xConfig.dev.screen;
        long colormap = x11.XDefaultColormap(display, screen);

        X11.XColor xcolor = getXColor(g.getColor());
        long xcolorPtr = xcolor.lock();
        
        CompositeFont wcf = (CompositeFont)(g.getFont().getPeer());
        long font = 0;
        int xOffset = (int)Math.round(x);    // X offset to draw
        int yOffset = (int)Math.round(y);    // Y offset to draw
        int offs = 0;       // width of substring with the same font header
        String sChars = new String();
        char chars[];
        for (int i=0; i < str.length(); i++){
            char c = str.charAt(i);
            Glyph gl = wcf.getGlyph(c);
            if (font == 0){
                font = gl.getPFont();
            }
            int glWidth = Math.round(gl.getGlyphPointMetrics().getAdvance());
            if (glWidth ==0){
                continue;
            }
            long glPFont = gl.getPFont();
            if (font != glPFont){
                chars = sChars.toCharArray();
                LinuxNativeFont.drawStringNative(xg2d.xftDraw, display, 
                        colormap, font, xOffset, yOffset, chars, 
                        sChars.length(), xcolorPtr);

                xOffset += offs;
                offs = 0;
                sChars = String.valueOf(gl.getChar());
                font = glPFont;
            } else {
                sChars += String.valueOf(gl.getChar());
            }
            offs += glWidth;
        }
        chars = sChars.toCharArray();
        if (chars.length != 0){
            LinuxNativeFont.drawStringNative(xg2d.xftDraw, display, colormap, 
                    font, xOffset, yOffset, chars, sChars.length(), 
                    xcolorPtr);
        }
        xcolor.unlock();

    }

    public void drawGlyphVector(Graphics2D ga, GlyphVector gv, float x, 
            float y) {
        CommonGraphics2D g = (CommonGraphics2D)ga;
        AffineTransform trans = g.getTransform();
        float xOffset = x + (float)trans.getTranslateX();
        float yOffset = y + (float)trans.getTranslateY();

        FontPeerImpl fnt = (FontPeerImpl)gv.getFont().getPeer();
        if (fnt.getClass() == CompositeFont.class){
            drawCompositeGlyphVector(g, gv, xOffset, yOffset);
        } else {
            drawNormalGlyphVector(g, gv, xOffset, yOffset);
        }

    }

    /**
     * Method to draw GlyphVector created from physical font onto a 
     * specified graphics at desired coordinates.
     * 
     * @param g CommonGraphics2D to draw onto
     * @param glyphVector GlyphVector to draw
     * @param x starting X coordinate to draw at
     * @param y starting Y coordinate to draw at
     */
    public void drawNormalGlyphVector(CommonGraphics2D g, 
            GlyphVector glyphVector, float x, float y) {

        XGraphics2D xg2d =  (XGraphics2D)g;

        long display = xg2d.display;
        int screen = xg2d.xConfig.dev.screen;
        long colormap = x11.XDefaultColormap(display, screen);

        LinuxFont peer = (LinuxFont)glyphVector.getFont().getPeer();

        X11.XColor xcolor = getXColor(g.getColor());
        long xcolorPtr = xcolor.lock();

        for (int i = 0; i < glyphVector.getNumGlyphs(); i++) {

            Glyph gl = ((CommonGlyphVector)glyphVector).vector[i];
            if (gl.getPointWidth() == 0){
                continue;
            }

            Point2D pos = glyphVector.getGlyphPosition(i);

            int xBaseLine = (int)Math.round(x + pos.getX());
            int yBaseLine = (int)Math.round(y + pos.getY());
            char chars[] = {gl.getChar()};

            LinuxNativeFont.drawStringNative(xg2d.xftDraw, display, colormap, 
                    peer.getFontHandle(isAntialiasingHintSet(g)), xBaseLine, yBaseLine, chars, 1,
                    xcolorPtr);
        }
        xcolor.unlock();

    }

    /**
     * Method to draw GlyphVector created from composite font onto a 
     * specified graphics at desired coordinates.
     * 
     * @param g CommonGraphics2D to draw onto
     * @param glyphVector GlyphVector to draw
     * @param x starting X coordinate to draw at
     * @param y starting Y coordinate to draw at
     */
    public void drawCompositeGlyphVector(CommonGraphics2D g, 
            GlyphVector glyphVector, float x, float y) {

        XGraphics2D xg2d =  (XGraphics2D)g;
        long display = xg2d.display;
        int screen = xg2d.xConfig.dev.screen;
        long colormap = x11.XDefaultColormap(display, screen);

        X11.XColor xcolor = getXColor(g.getColor());
        long xcolorPtr = xcolor.lock();

        for (int i = 0; i < glyphVector.getNumGlyphs(); i++) {

            Glyph gl = ((CommonGlyphVector)glyphVector).vector[i];
            if (gl.getPointWidth() == 0){
                continue;
            }

            Point2D pos = glyphVector.getGlyphPosition(i);

            int xBaseLine = (int)Math.round(x + pos.getX());
            int yBaseLine = (int)Math.round(y + pos.getY());
            char chars[] = {gl.getChar()};

            LinuxNativeFont.drawStringNative(xg2d.xftDraw, display, colormap, 
                    gl.getPFont(), xBaseLine, yBaseLine, chars, 1, 
                    xcolorPtr);
        }
        xcolor.unlock();

    }
}
