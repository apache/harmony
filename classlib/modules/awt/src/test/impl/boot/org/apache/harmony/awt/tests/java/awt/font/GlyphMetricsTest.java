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

import java.awt.font.GlyphMetrics;
import java.awt.geom.Rectangle2D;

import junit.framework.TestCase;

public class GlyphMetricsTest extends TestCase {
    float advance = 10;
    Rectangle2D.Float bounds = new Rectangle2D.Float(2, 2, 15, 15);
    byte glyphType = GlyphMetrics.COMPONENT;

    /*
     * Test method for 'java.awt.font.GlyphMetrics.GlyphMetrics(boolean, float, float, Rectangle2D, byte)'
     */
    public final void testGlyphMetricsBooleanFloatFloatRectangle2DByte() {
        boolean horizontal = false;
        float advanceX = -5;
        float advanceY = 5;
        GlyphMetrics gm = new GlyphMetrics(horizontal, advanceX, advanceY, bounds, glyphType);
        assertEquals(bounds, gm.getBounds2D());
        assertEquals(advanceY, gm.getAdvance(), 0F);
        assertEquals(glyphType, gm.getType());
        assertEquals(advanceX, gm.getAdvanceX(), 0F);
        assertEquals(advanceY, gm.getAdvanceY(), 0F);
    }

    /*
     * Test method for 'java.awt.font.GlyphMetrics.GlyphMetrics(float, Rectangle2D, byte)'
     */
    public final void testGlyphMetricsFloatRectangle2DByte() {
        GlyphMetrics gm = new GlyphMetrics(advance, bounds, glyphType);
        assertEquals(bounds, gm.getBounds2D());
        assertEquals(advance, gm.getAdvance(), 0F);
        assertEquals(glyphType, gm.getType());
    }

    /*
     * Test method for 'java.awt.font.GlyphMetrics.getBounds2D()'
     */
    public final void testGetBounds2D() {
        GlyphMetrics gm = new GlyphMetrics(advance, bounds, GlyphMetrics.LIGATURE);
        assertEquals(bounds, gm.getBounds2D());
        
    }

    /*
     * Test method for 'java.awt.font.GlyphMetrics.isWhitespace()'
     */
    public final void testIsWhitespace() {
        GlyphMetrics gm = new GlyphMetrics(advance, bounds, GlyphMetrics.WHITESPACE);
        assertTrue(gm.isWhitespace());
        
        gm = new GlyphMetrics(advance, bounds, GlyphMetrics.COMBINING);
        assertFalse(gm.isWhitespace());
    }

    /*
     * Test method for 'java.awt.font.GlyphMetrics.isStandard()'
     */
    public final void testIsStandard() {
        GlyphMetrics gm = new GlyphMetrics(advance, bounds, GlyphMetrics.STANDARD);
        assertTrue(gm.isStandard());
        
        gm = new GlyphMetrics(advance, bounds, GlyphMetrics.COMBINING);
        assertFalse(gm.isStandard());
    }

    /*
     * Test method for 'java.awt.font.GlyphMetrics.isLigature()'
     */
    public final void testIsLigature() {
        GlyphMetrics gm = new GlyphMetrics(advance, bounds, GlyphMetrics.LIGATURE);
        assertTrue(gm.isLigature());
        
        gm = new GlyphMetrics(advance, bounds, GlyphMetrics.COMBINING);
        assertFalse(gm.isLigature());
    }

    /*
     * Test method for 'java.awt.font.GlyphMetrics.isComponent()'
     */
    public final void testIsComponent() {
        GlyphMetrics gm = new GlyphMetrics(advance, bounds, GlyphMetrics.COMPONENT);
        assertTrue(gm.isComponent());
        
        gm = new GlyphMetrics(advance, bounds, GlyphMetrics.LIGATURE);
        assertFalse(gm.isComponent());
    }

    /*
     * Test method for 'java.awt.font.GlyphMetrics.isCombining()'
     */
    public final void testIsCombining() {
        GlyphMetrics gm = new GlyphMetrics(advance, bounds, GlyphMetrics.COMBINING);
        assertTrue(gm.isCombining());
        
        gm = new GlyphMetrics(advance, bounds, GlyphMetrics.LIGATURE);
        assertFalse(gm.isCombining());
    }

    /*
     * Test method for 'java.awt.font.GlyphMetrics.getType()'
     */
    public final void testGetType() {
        GlyphMetrics gm = new GlyphMetrics(advance, bounds, glyphType);
        assertEquals(glyphType, gm.getType());
    }

    /*
     * Test method for 'java.awt.font.GlyphMetrics.getRSB()'
     */
    public final void testGetRSB() {
        GlyphMetrics gm = new GlyphMetrics(advance, bounds, glyphType);
        assertEquals(bounds.x, gm.getLSB(), 0F);

        boolean horizontal = false;
        float advanceX = -5;
        float advanceY = 5;
        gm = new GlyphMetrics(horizontal, advanceX, advanceY, bounds, glyphType);
        assertEquals(bounds.y, gm.getLSB(), 0F);
    }

    /*
     * Test method for 'java.awt.font.GlyphMetrics.getLSB()'
     */
    public final void testGetLSB() {
        GlyphMetrics gm = new GlyphMetrics(advance, bounds, glyphType);
        assertEquals(advance - bounds.x - (float)bounds.getWidth(), gm.getRSB(), 0F);

        boolean horizontal = false;
        float advanceX = -5;
        float advanceY = 5;
        gm = new GlyphMetrics(horizontal, advanceX, advanceY, bounds, glyphType);
        assertEquals(advanceY - bounds.y - (float)bounds.getHeight(), gm.getRSB(), 0F);
    }

    /*
     * Test method for 'java.awt.font.GlyphMetrics.getAdvanceY()'
     */
    public final void testGetAdvanceY() {
        GlyphMetrics gm = new GlyphMetrics(advance, bounds, glyphType);
        assertEquals(0, gm.getAdvanceY(), 0F);
        
        gm = new GlyphMetrics(true, 0, advance, bounds, glyphType);
        assertEquals(advance, gm.getAdvanceY(), 0F);

    }

    /*
     * Test method for 'java.awt.font.GlyphMetrics.getAdvanceX()'
     */
    public final void testGetAdvanceX() {
        GlyphMetrics gm = new GlyphMetrics(advance, bounds, glyphType);
        assertEquals(advance, gm.getAdvanceX(), 0F);
        
        gm = new GlyphMetrics(true, advance, 0, bounds, glyphType);
        assertEquals(advance, gm.getAdvanceX(), 0F);

    }

    /*
     * Test method for 'java.awt.font.GlyphMetrics.getAdvance()'
     */
    public final void testGetAdvance() {
        GlyphMetrics gm = new GlyphMetrics(advance, bounds, glyphType);
        assertEquals(advance, gm.getAdvance(), 0F);
    }

}
