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

import java.awt.font.FontRenderContext;

import org.apache.harmony.awt.gl.font.LineMetricsImpl;

/**
 *
 * Linux implementation of LineMetrics class
 */
public class LinuxLineMetrics extends LineMetricsImpl {
    
    // LinuxFont corresponding to this LineMetrics object
    private LinuxFont font = null;

    /**
     * Constructor
     */
    public LinuxLineMetrics(LinuxFont fnt, String str){

        float[] metrics = LinuxNativeFont.getNativeLineMetrics(fnt.getFontHandle(), fnt.getSize(), false, false, fnt.fontType);

        if (metrics == null){
            metrics = new float[17];
        }
        
        font = fnt;
        numChars = str.length();
        baseLineIndex = 0;

        ascent = metrics[0];    // Ascent of the font
        descent = -metrics[1];  // Descent of the font
        leading = metrics[2];  // External leading

        height = ascent + descent + leading;    // Height of the font ( == (ascent + descent + leading))
        underlineThickness = metrics[3];
        underlineOffset = -metrics[4];
        strikethroughThickness = metrics[5];
        strikethroughOffset = -metrics[6];
        maxCharWidth = metrics[7];

        //    TODO: Find out pixel metrics
        /*
         * positive metrics rounded to the smallest int that is bigger than value
         * negative metrics rounded to the smallest int that is lesser than value
         * thicknesses rounded to int ((int)round(value + 0.5))
         *
         */

        lAscent = (int)metrics[8];//(int)Math.ceil(ascent);//   // Ascent of the font
        lDescent = -(int)metrics[9];    //(int)Math.ceil(descent);// Descent of the font
        lLeading = (int)metrics[10];//(int)Math.ceil(leading);  // External leading

        lHeight = lAscent + lDescent + lLeading;    // Height of the font ( == (ascent + descent + leading))

        lUnderlineThickness = Math.round(underlineThickness);//(int)metrics[11];

        if (underlineOffset >= 0){
            lUnderlineOffset = (int)Math.ceil(underlineOffset);
        } else {
            lUnderlineOffset = (int)Math.floor(underlineOffset);
        }

        lStrikethroughThickness = Math.round(strikethroughThickness); //(int)metrics[13];

        if (strikethroughOffset >= 0){
            lStrikethroughOffset = (int)Math.ceil(strikethroughOffset);
        } else {
            lStrikethroughOffset = (int)Math.floor(strikethroughOffset);
        }

        lMaxCharWidth = (int)Math.ceil(maxCharWidth); //(int)metrics[15];
        units_per_EM = (int)metrics[16];

    }

    public float[] getBaselineOffsets() {
        // TODO: implement baseline offsets for TrueType fonts
        if (baselineOffsets == null){
            float[] baselineData = null;

            // Temporary workaround:
            // Commented out native data initialization, since it can 
            // cause failures with opening files in multithreaded applications.
            //
            // TODO: support work with truetype data in multithreaded
            // applications.

            // If font TrueType data is taken from BASE table
//            if ((this.font.getFontHandle() != 0) && (font.getFontType() == FontManager.FONT_TYPE_TT)){
//                baselineData = LinuxNativeFont.getBaselineOffsetsNative(font.getFontHandle(), font.getSize(), ascent, descent, units_per_EM);
//            }
//
//            if (baselineData == null){
                baseLineIndex = 0;
                baselineOffsets = new float[]{0, (-ascent+descent)/2, -ascent};
//            } else {
//                baseLineIndex = (int)baselineData[3];
//                baselineOffsets = new float[3];
//                System.arraycopy(baselineData, 0, baselineOffsets, 0, 3);
//            }

        }

        return baselineOffsets;
    }

    public int getBaselineIndex() {
        if (baselineOffsets == null){
            // get offsets and set correct index
            getBaselineOffsets();
        }
        return baseLineIndex;
    }

}
