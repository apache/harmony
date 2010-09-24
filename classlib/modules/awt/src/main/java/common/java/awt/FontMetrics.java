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
package java.awt;

import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.text.CharacterIterator;

import org.apache.harmony.awt.internal.nls.Messages;

public abstract class FontMetrics implements Serializable {
    private static final long serialVersionUID = 1681126225205050147L;

    protected Font font;

    protected FontMetrics(Font fnt) {
        this.font = fnt;
    }

    @Override
    public String toString() {
        return this.getClass().getName() +
                "[font=" + this.getFont() + //$NON-NLS-1$
                "ascent=" + this.getAscent() + //$NON-NLS-1$
                ", descent=" + this.getDescent() + //$NON-NLS-1$
                ", height=" + this.getHeight() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public Font getFont() {
        return font;
    }

    public int getHeight() {
        return this.getAscent() + this.getDescent() + this.getLeading();
    }

    public int getAscent() {
        return 0;
    }

    public int getDescent() {
        return 0;
    }

    public int getLeading() {
        return 0;
    }

    public LineMetrics getLineMetrics(CharacterIterator ci, int beginIndex,
                                        int limit, Graphics context) {
        return font.getLineMetrics(ci, beginIndex, limit, 
                this.getFRCFromGraphics(context));
    }

    public LineMetrics getLineMetrics(String str, Graphics context) {
        return font.getLineMetrics(str, this.getFRCFromGraphics(context));
    }

    public LineMetrics getLineMetrics(char[] chars, int beginIndex, int limit,
                                        Graphics context) {
        return font.getLineMetrics(chars, beginIndex, limit, 
                this.getFRCFromGraphics(context));
    }

    public LineMetrics getLineMetrics(String str, int beginIndex, int limit,
                                        Graphics context) {
        return font.getLineMetrics(str, beginIndex, limit, 
                this.getFRCFromGraphics(context));
    }

    public Rectangle2D getMaxCharBounds(Graphics context) {
        return this.font.getMaxCharBounds(this.getFRCFromGraphics(context));
    }

    public Rectangle2D getStringBounds(CharacterIterator ci, int beginIndex,
            int limit, Graphics context) {
        return font.getStringBounds(ci, beginIndex, limit, 
                this.getFRCFromGraphics(context));
    }

    public Rectangle2D getStringBounds(String str, int beginIndex, int limit,
            Graphics context) {
        return font.getStringBounds(str, beginIndex, limit, 
                this.getFRCFromGraphics(context));
    }


    public Rectangle2D getStringBounds(char[] chars, int beginIndex, int limit,
            Graphics context) {
        return font.getStringBounds(chars, beginIndex, limit, 
                this.getFRCFromGraphics(context));
    }

    public Rectangle2D getStringBounds(String str, Graphics context) {
        return font.getStringBounds(str, this.getFRCFromGraphics(context));
    }

    public boolean hasUniformLineMetrics() {
        return this.font.hasUniformLineMetrics();
    }

    public int bytesWidth(byte[] data, int off, int len) {
        int width = 0;
        if ((off >= data.length) || (off < 0)){
            // awt.13B=offset off is out of range
            throw new IllegalArgumentException(Messages.getString("awt.13B")); //$NON-NLS-1$
        }

        if ((off+len > data.length)){
            // awt.13C=number of elements len is out of range
            throw new IllegalArgumentException(Messages.getString("awt.13C")); //$NON-NLS-1$
        }

        for (int i = off; i < off+len; i++){
            width += charWidth(data[i]);
        }

        return width;
    }

    public int charsWidth(char[] data, int off , int len){
        int width = 0;
        if ((off >= data.length) || (off < 0)){
            // awt.13B=offset off is out of range
            throw new IllegalArgumentException(Messages.getString("awt.13B")); //$NON-NLS-1$
        }

        if ((off+len > data.length)){
            // awt.13C=number of elements len is out of range
            throw new IllegalArgumentException(Messages.getString("awt.13C")); //$NON-NLS-1$
        }

        for (int i = off; i < off+len; i++){
            width += charWidth(data[i]);
        }

        return width;
    }

    public int charWidth(int ch) {
        return 0;
    }

    public int charWidth(char ch) {
        return 0;
    }

    public int getMaxAdvance() {
        return 0;
    }

    public int getMaxAscent() {
        return 0;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public int getMaxDecent() {
        return 0;
    }

    public int getMaxDescent() {
        return 0;
    }

    public int[] getWidths() {
        return null;
    }

    public int stringWidth(String str) {
        return 0;
    }
    
    /**
     * Returns FontRenderContext instance of the Graphics context specified.
     * @param context the specified Graphics context
     * 
     * @return a FontRenderContext of the specified Graphics context.
     */
    private FontRenderContext getFRCFromGraphics(Graphics context){
        FontRenderContext frc;
        if (context instanceof Graphics2D) {
            frc = ((Graphics2D)context).getFontRenderContext();
        } else {
            frc = new FontRenderContext(null, false, false);
        }

        return frc;

    }
}

