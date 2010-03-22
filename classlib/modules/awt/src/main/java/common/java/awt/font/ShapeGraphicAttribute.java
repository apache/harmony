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

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.apache.harmony.misc.HashCode;


public final class ShapeGraphicAttribute extends GraphicAttribute {

    // shape to render
    private Shape fShape;
    
    // flag, if the shape should be stroked (true) or filled (false)
    private boolean fStroke;

    // bounds of the shape
    private Rectangle2D fBounds;
    
    // X coordinate of the origin point
    private float fOriginX;
    
    // Y coordinate of the origin point
    private float fOriginY;

    // width of the shape
    private float fShapeWidth;
    
    // height of the shape
    private float fShapeHeight;

    public static final boolean STROKE = true;

    public static final boolean FILL = false;

    public ShapeGraphicAttribute(Shape shape, int alignment, boolean stroke) {
        super(alignment);

        this.fShape = shape;
        this.fStroke = stroke;

        this.fBounds  = fShape.getBounds2D();

        this.fOriginX = (float)fBounds.getMinX();
        this.fOriginY = (float)fBounds.getMinY();

        this.fShapeWidth = (float)fBounds.getWidth();
        this.fShapeHeight = (float)fBounds.getHeight();
    }

    @Override
    public int hashCode() {
        HashCode hash = new HashCode();

        hash.append(fShape.hashCode());
        hash.append(getAlignment());
        return hash.hashCode();
    }

    public boolean equals(ShapeGraphicAttribute sga) {
        if (sga == null) {
            return false;
        }

        if (sga == this) {
            return true;
        }

        return ( fStroke == sga.fStroke &&
                getAlignment() == sga.getAlignment() &&
                fShape.equals(sga.fShape));

    }

    @Override
    public boolean equals(Object obj) {
        try {
            return equals((ShapeGraphicAttribute) obj);
        }
        catch(ClassCastException e) {
            return false;
        }
    }

    @Override
    public void draw(Graphics2D g2, float x, float y) {
        AffineTransform at = AffineTransform.getTranslateInstance(x, y);
        if (fStroke == STROKE){
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke());
            g2.draw(at.createTransformedShape(fShape));
            g2.setStroke(oldStroke);
        } else {
            g2.fill(at.createTransformedShape(fShape));
        }

    }

    @Override
    public float getAdvance() {
        return Math.max(0, fShapeWidth + fOriginX);
    }

    @Override
    public float getAscent() {
        return Math.max(0, -fOriginY);
    }

    @Override
    public Rectangle2D getBounds() {
        return (Rectangle2D)fBounds.clone();
    }

    @Override
    public float getDescent() {
        return Math.max(0, fShapeHeight + fOriginY);
    }

}

