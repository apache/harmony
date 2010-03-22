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

import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

import junit.framework.TestCase;

public class FontRenderContextTest extends TestCase {
    
    FontRenderContext frc;
    AffineTransform at = AffineTransform.getRotateInstance(1);
    boolean isAA = true;
    boolean usesFractionalMetrics = false;
    
    public FontRenderContextTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frc = new FontRenderContext(at, isAA, usesFractionalMetrics);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'java.awt.font.FontRenderContext.FontRenderContext(AffineTransform, boolean, boolean)'
     */
    public final void testFontRenderContextAffineTransformBooleanBoolean() {
        
        assertNotNull(frc);
        assertEquals("AffineTransform", at, frc.getTransform());
        assertEquals("isAntialiased", isAA, frc.isAntiAliased());
        assertEquals("usesFractionalMetrics", usesFractionalMetrics, frc.usesFractionalMetrics());
        
        frc = new FontRenderContext(null, isAA, usesFractionalMetrics);
        assertEquals("AffineTransform", new AffineTransform(), frc.getTransform());
    }

    /*
     * Test method for 'java.awt.font.FontRenderContext.equals(Object)'
     */
    public final void testEqualsObject() {
        FontRenderContext frc1 = new FontRenderContext(at, isAA, usesFractionalMetrics);
        assertTrue(frc.equals((Object)frc1));
    }

    /*
     * Test method for 'java.awt.font.FontRenderContext.getTransform()'
     */
    public final void testGetTransform() {
        assertEquals(at, frc.getTransform());
    }

    /*
     * Test method for 'java.awt.font.FontRenderContext.equals(FontRenderContext)'
     */
    public final void testEqualsFontRenderContext() {
        FontRenderContext frc1 = new FontRenderContext(at, isAA, usesFractionalMetrics);
        assertTrue(frc.equals(frc1));
    }

    /*
     * Test method for 'java.awt.font.FontRenderContext.usesFractionalMetrics()'
     */
    public final void testUsesFractionalMetrics() {
        assertEquals(usesFractionalMetrics, frc.usesFractionalMetrics());
    }

    /*
     * Test method for 'java.awt.font.FontRenderContext.isAntiAliased()'
     */
    public final void testIsAntiAliased() {
        assertEquals(isAA, frc.isAntiAliased());
    }

}
