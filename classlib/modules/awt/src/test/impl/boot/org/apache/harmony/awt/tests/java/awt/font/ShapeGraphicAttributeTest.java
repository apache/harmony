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

package org.apache.harmony.awt.tests.java.awt.font;

import java.awt.Shape;
import java.awt.font.GlyphJustificationInfo;
import java.awt.font.GraphicAttribute;
import java.awt.font.ShapeGraphicAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import junit.framework.TestCase;

public class ShapeGraphicAttributeTest extends TestCase {
    ShapeGraphicAttribute sga;
    float width = 10;
    float height = 10;
    float xOrigin = 5;
    float yOrigin = 5;
    boolean stroke = ShapeGraphicAttribute.FILL;
    
    Shape shape = new Rectangle2D.Float(xOrigin, yOrigin, width, height);
    int alignment =  GraphicAttribute.ROMAN_BASELINE;

    public ShapeGraphicAttributeTest(String name) {
        super(name);
    }

    /*
     * Test method for 'java.awt.font.ShapeGraphicAttribute.getAdvance()'
     */
    public final void testGetAdvance() {

        sga = new ShapeGraphicAttribute(shape, alignment, stroke);
        assertEquals((float)shape.getBounds2D().getWidth() + xOrigin, sga.getAdvance(), 0F);
        
        Shape trShape = AffineTransform.getTranslateInstance(-20, 0).createTransformedShape(shape);
        sga = new ShapeGraphicAttribute(trShape, alignment, stroke);
        assertEquals(0, sga.getAdvance(), 0F);
    }

    /*
     * Test method for 'java.awt.font.ShapeGraphicAttribute.getAscent()'
     */
    public final void testGetAscent() {
        sga = new ShapeGraphicAttribute(shape, alignment, stroke);
        assertEquals(0, sga.getAscent(), 0F);

        Shape trShape = AffineTransform.getTranslateInstance(0, -30).createTransformedShape(shape);
        sga = new ShapeGraphicAttribute(trShape, alignment, stroke);
        assertEquals(-(float)trShape.getBounds2D().getMinY(), sga.getAscent(), 0F);

    }

    /*
     * Test method for 'java.awt.font.ShapeGraphicAttribute.getBounds()'
     */
    public final void testGetBounds() {
        sga = new ShapeGraphicAttribute(shape, alignment, ShapeGraphicAttribute.FILL);
        Rectangle2D.Float bounds = (Rectangle2D.Float)shape.getBounds2D();
        assertEquals(bounds, sga.getBounds());

    }

    /*
     * Test method for 'java.awt.font.ShapeGraphicAttribute.getDescent()'
     */
    public final void testGetDescent() {
        sga = new ShapeGraphicAttribute(shape, alignment, stroke);
        assertEquals((float)shape.getBounds2D().getMinY() + height, sga.getDescent(), 0F);

        Shape trShape = AffineTransform.getTranslateInstance(0, -30).createTransformedShape(shape);
        sga = new ShapeGraphicAttribute(trShape, alignment, stroke);
        assertEquals(0, sga.getDescent(), 0F);
    }

    /*
     * Test method for 'java.awt.font.ShapeGraphicAttribute.ShapeGraphicAttribute(Shape, int, boolean)'
     */
    public final void testShapeGraphicAttribute() {
        ShapeGraphicAttribute shAttribute = new ShapeGraphicAttribute(shape, alignment, stroke);
        assertNotNull(shAttribute);
        assertEquals(alignment, shAttribute.getAlignment());
        assertEquals((float)shape.getBounds2D().getMaxX(), shAttribute.getAdvance(), 0F);
        assertEquals(0, shAttribute.getAscent(), 0F);
        assertEquals((float)shape.getBounds2D().getMinY() + height, shAttribute.getDescent(), 0F);

        Rectangle2D.Float bounds = (Rectangle2D.Float)shape.getBounds2D();
        if (stroke == ShapeGraphicAttribute.STROKE){
            bounds.width++;
            bounds.height++;
        }
        assertEquals(bounds, shAttribute.getBounds());

        // illegal alignment value
        try {
            sga = new ShapeGraphicAttribute(shape, -3, stroke);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            sga = new ShapeGraphicAttribute(shape, 3, stroke);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /*
     * Test method for 'java.awt.font.ShapeGraphicAttribute.equals(ShapeGraphicAttribute)'
     */
    public final void testEqualsShapeGraphicAttribute() {
        sga = new ShapeGraphicAttribute(shape, alignment, stroke);
        ShapeGraphicAttribute sga1 = new ShapeGraphicAttribute(shape, alignment, stroke);
        assertTrue(sga.equals(sga1));
    }

    /*
     * Test method for 'java.awt.font.ShapeGraphicAttribute.equals(Object)'
     */
    public final void testEqualsObject() {
        sga = new ShapeGraphicAttribute(shape, alignment, stroke);
        ShapeGraphicAttribute sga1 = new ShapeGraphicAttribute(shape, alignment, stroke);
        assertTrue(sga.equals((Object)sga1));
    }

    /*
     * Test method for 'java.awt.font.GraphicAttribute.getAlignment()'
     */
    public final void testGetAlignment() {
        sga = new ShapeGraphicAttribute(shape, alignment, stroke);
        assertEquals(alignment, sga.getAlignment());
    }

    /*
     * Test method for 'java.awt.font.GraphicAttribute.getJustificationInfo()'
     */
    public final void testGetJustificationInfo() {
        sga = new ShapeGraphicAttribute(shape, alignment, stroke);
        float advance = sga.getAdvance();
        GlyphJustificationInfo gji = new GlyphJustificationInfo(
                advance,
                false,
                GlyphJustificationInfo.PRIORITY_INTERCHAR,
                advance / 3,
                advance / 3,
                false,
                GlyphJustificationInfo.PRIORITY_WHITESPACE,
                0,
                0);
        equalsGlyphJustificationInfo(gji, sga.getJustificationInfo());

    }

    private boolean equalsGlyphJustificationInfo(GlyphJustificationInfo info1, GlyphJustificationInfo info2){
        assertEquals("weight", info1.weight, info2.weight, 0F);
        assertEquals("growAbsorb", info1.growAbsorb, info2.growAbsorb);
        assertEquals("growPriority", info1.growPriority, info2.growPriority);
        assertEquals("growLeftLimit", info1.growLeftLimit, info2.growLeftLimit, 0F);
        assertEquals("growRightLimit", info1.growRightLimit, info2.growRightLimit, 0F);
        assertEquals("shrinkAbsorb", info1.shrinkAbsorb, info2.shrinkAbsorb);
        assertEquals("shrinkPriority", info1.shrinkPriority, info2.shrinkPriority);
        assertEquals("shrinkLeftLimit", info1.shrinkLeftLimit, info2.shrinkLeftLimit, 0F);
        assertEquals("shrinkRightLimit", info1.shrinkRightLimit, info2.shrinkRightLimit, 0F);
        
        return true;
    }
    

}
