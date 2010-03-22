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
package java.awt.font;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphJustificationInfo;
import java.awt.font.GlyphMetrics;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public abstract class GlyphVector implements Cloneable {

    public static final int FLAG_HAS_TRANSFORMS = 1;

    public static final int FLAG_HAS_POSITION_ADJUSTMENTS = 2;

    public static final int FLAG_RUN_RTL = 4;

    public static final int FLAG_COMPLEX_GLYPHS = 8;

    public static final int FLAG_MASK = 15; // (|) mask of other flags

    public GlyphVector() {
    }

    public Rectangle getPixelBounds(FontRenderContext frc, float x, float y) {
        // default implementation - integer Rectangle, that encloses visual 
        // bounds rectangle
        Rectangle2D visualRect = getVisualBounds();

        int minX = (int)Math.floor(visualRect.getMinX() + x);
        int minY = (int)Math.floor(visualRect.getMinY() + y);
        int width = (int)Math.ceil(visualRect.getMaxX() + x) - minX;
        int height = (int)Math.ceil(visualRect.getMaxY() + y) - minY;

        return new Rectangle(minX, minY, width, height);
    }

    public Rectangle getGlyphPixelBounds(int index, FontRenderContext frc, 
            float x, float y) {
        Rectangle2D visualRect = getGlyphVisualBounds(index).getBounds2D();

        int minX = (int)Math.floor(visualRect.getMinX() + x);
        int minY = (int)Math.floor(visualRect.getMinY() + y);
        int width = (int)Math.ceil(visualRect.getMaxX() + x) - minX;
        int height = (int)Math.ceil(visualRect.getMaxY() + y) - minY;

        return new Rectangle(minX, minY, width, height);
    }

    public abstract Rectangle2D getVisualBounds();

    public abstract Rectangle2D getLogicalBounds();

    public abstract void setGlyphPosition(int glyphIndex, Point2D newPos);

    public abstract Point2D getGlyphPosition(int glyphIndex);

    public abstract void setGlyphTransform(int glyphIndex, 
            AffineTransform trans);

    public abstract AffineTransform getGlyphTransform(int glyphIndex);

    public abstract boolean equals(GlyphVector glyphVector);

    public abstract GlyphMetrics getGlyphMetrics(int glyphIndex);

    public abstract GlyphJustificationInfo getGlyphJustificationInfo(
            int glyphIndex);

    public abstract FontRenderContext getFontRenderContext();

    public Shape getGlyphOutline(int glyphIndex, float x, float y) {
        Shape initialShape = getGlyphOutline(glyphIndex);
        AffineTransform trans = AffineTransform.getTranslateInstance(x, y);
        return trans.createTransformedShape(initialShape);
    }

    public abstract Shape getGlyphVisualBounds(int glyphIndex);

    public abstract Shape getGlyphOutline(int glyphIndex);

    public abstract Shape getGlyphLogicalBounds(int glyphIndex);

    public abstract Shape getOutline(float x, float y);

    public abstract Shape getOutline();

    public abstract Font getFont();

    public abstract int[] getGlyphCodes(int beginGlyphIndex, int numEntries, 
            int[] codeReturn);

    public int[] getGlyphCharIndices(int beginGlyphIndex, int numEntries, 
            int[] codeReturn) {
        if (codeReturn == null) {
            codeReturn = new int[numEntries];
        }

        for (int i = 0; i < numEntries; i++){
            codeReturn[i] = getGlyphCharIndex(i+beginGlyphIndex);
        }
        return codeReturn;
    }

    public abstract float[] getGlyphPositions(int beginGlyphIndex, 
            int numEntries, float[] positionReturn);

    public abstract int getGlyphCode(int glyphIndex);

    public int getGlyphCharIndex(int glyphIndex){
        // default implemetation one-to-one
        return glyphIndex;
    }

    public abstract void performDefaultLayout();

    public abstract int getNumGlyphs();

    public int getLayoutFlags(){
        // default implementation - returned value is 0
        return 0;
    }

}

