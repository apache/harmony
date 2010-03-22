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
 * @author Alexander T. Simbirtsev
 */
package org.apache.harmony.x.swing;

import javax.swing.BasicSwingTestCase;
import javax.swing.SizeRequirements;

public class SizeRequirementsHelperRTest extends BasicSwingTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCalculateAlignedPositions() {
        SizeRequirements total = new SizeRequirements(100, 100, Integer.MAX_VALUE, 0.5f);
        SizeRequirements[] children = new SizeRequirements[] {
                new SizeRequirements(25, 25, 25, 0.5f), new SizeRequirements(50, 50, 50, 0.5f) };
        int[] offsets = new int[children.length];
        int[] spans = new int[children.length];
        SizeRequirementsHelper.calculateAlignedPositions(100, total, children, offsets, spans);
        assertEquals(38, offsets[0]);
        assertEquals(25, offsets[1]);
        assertEquals(25, spans[0]);
        assertEquals(50, spans[1]);
        total = new SizeRequirements(100, 1000, Integer.MAX_VALUE, 0.5f);
        children = new SizeRequirements[] { new SizeRequirements(50, 50, 250, 0.5f),
                new SizeRequirements(100, 100, 500, 0.5f) };
        offsets = new int[children.length];
        spans = new int[children.length];
        SizeRequirementsHelper.calculateAlignedPositions(200, total, children, offsets, spans);
        assertEquals(0, offsets[0]);
        assertEquals(0, offsets[1]);
        assertEquals(200, spans[0]);
        assertEquals(200, spans[1]);
    }
}