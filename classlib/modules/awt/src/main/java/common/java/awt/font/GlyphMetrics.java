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

import java.awt.geom.Rectangle2D;

public final class GlyphMetrics {

    // advance width of the glyph character cell
    private float advanceX;
    
    // advance height of the glyph character cell
    private float advanceY;

    // flag if the glyph horizontal
    private boolean horizontal;

    // glyph type code 
    private byte glyphType;
    
    // bounding box for outline of the glyph
    private Rectangle2D.Float bounds;

    public static final byte STANDARD = 0;

    public static final byte LIGATURE = 1;

    public static final byte COMBINING = 2;

    public static final byte COMPONENT = 3;

    public static final byte WHITESPACE = 4;

    public GlyphMetrics(boolean horizontal, float advanceX, float advanceY, 
            Rectangle2D bounds, byte glyphType) {
        this.horizontal = horizontal;
        this.advanceX = advanceX;
        this.advanceY = advanceY;

        this.bounds = new Rectangle2D.Float();
        this.bounds.setRect(bounds);

        this.glyphType = glyphType;
    }

    public GlyphMetrics(float advanceX, Rectangle2D bounds, byte glyphType) {
        this.advanceX = advanceX;
        this.advanceY = 0;

        this.horizontal = true;

        this.bounds = new Rectangle2D.Float();
        this.bounds.setRect(bounds);

        this.glyphType = glyphType;
    }

    public Rectangle2D getBounds2D() {
        return (Rectangle2D.Float) this.bounds.clone();
    }

    public boolean isWhitespace() {
        return ((this.glyphType & 4) == WHITESPACE);
    }

    public boolean isStandard() {
        return ((this.glyphType & 3) == STANDARD);
    }

    public boolean isLigature() {
        return ((this.glyphType & 3) == LIGATURE);
    }

    public boolean isComponent() {
        return ((this.glyphType & 3) == COMPONENT);
    }

    public boolean isCombining() {
        return ((this.glyphType & 3) == COMBINING);
    }

    public int getType() {
        return this.glyphType;
    }

    public float getRSB() {
        if (this.horizontal) {
            return this.advanceX - this.bounds.x - (float)this.bounds.getWidth();
        }
        return this.advanceY - this.bounds.y - (float)this.bounds.getHeight();
    }

    public float getLSB() {
        if (this.horizontal) {
            return this.bounds.x;
        }
        return this.bounds.y;
    }

    public float getAdvanceY() {
        return this.advanceY;
    }

    public float getAdvanceX() {
        return this.advanceX;
    }

    public float getAdvance() {
        if (this.horizontal) {
            return this.advanceX;
        }
        return this.advanceY;
    }

}

