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

import java.awt.font.GlyphJustificationInfo;

import junit.framework.TestCase;

public class GlyphJustificationInfoTest extends TestCase {
    float weight = 120; 
    boolean growAbsorb = true;
    int growPriority = GlyphJustificationInfo.PRIORITY_KASHIDA; 
    float growLeftLimit = 20;
    float growRightLimit = 20;
    boolean shrinkAbsorb = false;
    int shrinkPriority = GlyphJustificationInfo.PRIORITY_WHITESPACE;
    float shrinkLeftLimit = 20;
    float shrinkRightLimit = 20;


    /*
     * Test method for 'java.awt.font.GlyphJustificationInfo.GlyphJustificationInfo(float, boolean, int, float, float, boolean, int, float, float)'
     */
    public final void testGlyphJustificationInfo() {
        GlyphJustificationInfo gji = new GlyphJustificationInfo(weight, growAbsorb, 
                growPriority, growLeftLimit, growRightLimit, shrinkAbsorb, 
                shrinkPriority, shrinkLeftLimit, shrinkRightLimit);
        
        assertEquals("weight", weight, gji.weight, 0F);
        assertEquals("growAbsorb", growAbsorb, gji.growAbsorb);
        assertEquals("growPriority", growPriority, gji.growPriority);
        assertEquals("growLeftLimit", growLeftLimit, gji.growLeftLimit, 0F);
        assertEquals("growRightLimit", growRightLimit, gji.growRightLimit, 0F);
        assertEquals("shrinkAbsorb", shrinkAbsorb, gji.shrinkAbsorb);
        assertEquals("shrinkPriority", shrinkPriority, gji.shrinkPriority);
        assertEquals("shrinkLeftLimit", shrinkLeftLimit, gji.shrinkLeftLimit, 0F);
        assertEquals("shrinkRightLimit", shrinkRightLimit, gji.shrinkRightLimit, 0F);
        
    }

    /*
     * Test method for 'java.awt.font.GlyphJustificationInfo.GlyphJustificationInfo(float, boolean, int, float, float, boolean, int, float, float)'
     */
    public final void testGlyphJustificationInfo_parametersCheck() {
        
        // negative weight
        try{
            new GlyphJustificationInfo(-1, growAbsorb, 
                growPriority, growLeftLimit, growRightLimit, shrinkAbsorb, 
                shrinkPriority, shrinkLeftLimit, shrinkRightLimit);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        // growPriority illegal level value
        try{
            new GlyphJustificationInfo(weight, growAbsorb, 
                5, growLeftLimit, growRightLimit, shrinkAbsorb, 
                shrinkPriority, shrinkLeftLimit, shrinkRightLimit);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try{
            new GlyphJustificationInfo(weight, growAbsorb, 
                    -1, growLeftLimit, growRightLimit, shrinkAbsorb, 
                    shrinkPriority, shrinkLeftLimit, shrinkRightLimit);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // negative growLeftLimit
        try{
            new GlyphJustificationInfo(weight, growAbsorb, 
                growPriority, -1, growRightLimit, shrinkAbsorb, 
                shrinkPriority, shrinkLeftLimit, shrinkRightLimit);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        // negative growRightLimit
        try{
            new GlyphJustificationInfo(weight, growAbsorb, 
                growPriority, growLeftLimit, -1, shrinkAbsorb, 
                shrinkPriority, shrinkLeftLimit, shrinkRightLimit);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // shrinkPriority illegal level value
        try{
            new GlyphJustificationInfo(weight, growAbsorb, 
                growPriority, growLeftLimit, growRightLimit, shrinkAbsorb, 
                5, shrinkLeftLimit, shrinkRightLimit);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try{
            new GlyphJustificationInfo(weight, growAbsorb, 
                    growPriority, growLeftLimit, growRightLimit, shrinkAbsorb, 
                    -1, shrinkLeftLimit, shrinkRightLimit);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // negative shrinkLeftLimit
        try{
            new GlyphJustificationInfo(weight, growAbsorb, 
                growPriority, growLeftLimit, growRightLimit, shrinkAbsorb, 
                shrinkPriority, -1, shrinkRightLimit);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        // negative shrinkRightLimit
        try{
            new GlyphJustificationInfo(weight, growAbsorb, 
                growPriority, growLeftLimit, growRightLimit, shrinkAbsorb, 
                shrinkPriority, -1, shrinkRightLimit);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }

    }

}
