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
import java.awt.FontMetrics;
import java.awt.geom.AffineTransform;

/**
 *  FontMetrics implementation
 */

public class FontMetricsImpl extends FontMetrics  {

    private static final long serialVersionUID = 844695615201925138L;

    // ascent of the font
    private int ascent;
    
    //descent of the font
    private int descent;
    
    // leading of the font
    private int leading;
    
    // maximum ascent of the font
    private int maxAscent;
    
    // maximum descent of the font
    private int maxDescent;
    
    // maximum advance of the font
    private int maxAdvance;
    
    // array of char advance widths
    private int[] widths = new int[256];
    
    // font peer corresponding to this FontPeerImpl 
    private transient FontPeerImpl peer;
    
    // X scale parameter of the font transform
    private float scaleX = 1;

    // Y scale parameter of the font transform
    // private float scaleY = 1;

    /**
     * Creates new FontMericsImpl object described by the specified Font.
     * 
     * @param fnt the specified Font object
     */
    public FontMetricsImpl(Font fnt) {
        super(fnt);
        peer = getFontPeer();
        AffineTransform at = fnt.getTransform();
        if (!at.isIdentity()){
            scaleX = (float)at.getScaleX();
            // scaleY = (float)at.getScaleY();
        }

        LineMetricsImpl lm = (LineMetricsImpl)peer.getLineMetrics("", null, at); //$NON-NLS-1$
        
        this.ascent = lm.getLogicalAscent();
        this.descent = lm.getLogicalDescent();
        this.leading = lm.getLogicalLeading();
        this.maxAscent = ascent;
        this.maxDescent = descent;
        this.maxAdvance = lm.getLogicalMaxCharWidth();
    }

    /**
     * Returns the ascent of the Font describing this FontMetricsImpl object.
     */
    @Override
    public int getAscent() {
        return this.ascent;
    }

    /**
     * Returns the descent of the Font describing this FontMetricsImpl object.
     */
    @Override
    public int getDescent() {
        return this.descent;
    }

    /**
     * Returns the leading of the Font describing this FontMetricsImpl object.
     */
    @Override
    public int getLeading() {
        return this.leading;
    }

    /**
     * Returns the advance width of the specified char of the Font 
     * describing this FontMetricsImpl object.
     * 
     * @param ch the char which width is to be returned
     * @return the advance width of the specified char of the Font 
     * describing this FontMetricsImpl object
     */
    @Override
    public int charWidth(int ch) {
//        if (ch < 256){
//            return widths[ch];
//        }

        return getFontPeer().charWidth((char)ch);
    }

    /**
     * Returns the advance width of the specified char of the Font 
     * describing this FontMetricsImpl object.
     * 
     * @param ch the char which width is to be returned
     * @return the advance width of the specified char of the Font 
     * describing this FontMetricsImpl object
     */
    @Override
    public int charWidth(char ch) {
//        if (ch < 256){
//            return widths[ch];
//        }

        return (int)(getFontPeer().charWidth(ch)*scaleX);
    }

    /**
     * Returns the maximum advance of the Font describing this 
     * FontMetricsImpl object.
     */
    @Override
    public int getMaxAdvance() {
        return this.maxAdvance;
    }

    /**
     * Returns the maximum ascent of the Font describing this 
     * FontMetricsImpl object.
     */
    @Override
    public int getMaxAscent() {
        return this.maxAscent;
    }

    /**
     * Returns the maximum descent of the Font describing this 
     * FontMetricsImpl object.
     */
    @Deprecated
    @Override
    public int getMaxDecent() {
        return this.maxDescent;
    }

    /**
     * Returns the maximum descent of the Font describing this 
     * FontMetricsImpl object.
     */
    @Override
    public int getMaxDescent() {
        return this.maxDescent;
    }

    /**
     * Returns the advance widths of the first 256 characters in the Font 
     * describing this FontMetricsImpl object.
     */
    @Override
    public int[] getWidths() {
        this.widths = new int[256];
        for (int chr=0; chr < 256; chr++){
            widths[chr] = (int)(getFontPeer().charWidth((char)chr)*scaleX);
        }
        return this.widths;
    }

    /**
     * Returns the total advance width of the specified string in the metrics
     * of the Font describing this FontMetricsImpl object.
     * 
     * @param str the String which width is to be measured
     * @return the total advance width of the specified string in the metrics 
     * of the Font describing this FontMetricsImpl object
     */
    @Override
    public int stringWidth(String str) {
        int width = 0;
        char chr;

        for (int i = 0; i < str.length(); i++){
            chr = str.charAt(i);
            width += charWidth(chr);
        }

        return width;


    }
    
    /**
     * Returns FontPeer implementation of the Font describing this 
     * FontMetricsImpl object. 
     *  
     * @return a FontPeer object, that is the platform dependent FontPeer 
     * implementation for the Font describing this FontMetricsImpl object.
     */
    @SuppressWarnings("deprecation")
    public FontPeerImpl getFontPeer(){
        if (peer == null){
            peer = (FontPeerImpl)font.getPeer();
        }
        return peer;
    }
}
