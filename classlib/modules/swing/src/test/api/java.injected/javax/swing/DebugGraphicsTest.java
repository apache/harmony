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
 * @author Dennis Ushakov
 */
package javax.swing;

import java.awt.image.BufferedImage;

public class DebugGraphicsTest extends BasicSwingTestCase {
    private DebugGraphics debugGraphics;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        debugGraphics = new DebugGraphics(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
                .getGraphics());
    }

    public void testGetSetDebugOptions() {
        assertEquals(0, debugGraphics.getDebugOptions());
        debugGraphics.setDebugOptions(DebugGraphics.BUFFERED_OPTION);
        assertTrue((debugGraphics.getDebugOptions() & DebugGraphics.BUFFERED_OPTION) != 0);
        debugGraphics.setDebugOptions(DebugGraphics.LOG_OPTION);
        assertTrue((debugGraphics.getDebugOptions() & DebugGraphics.BUFFERED_OPTION) != 0);
        assertTrue((debugGraphics.getDebugOptions() & DebugGraphics.LOG_OPTION) != 0);
        debugGraphics.setDebugOptions(DebugGraphics.NONE_OPTION);
        assertEquals(0, debugGraphics.getDebugOptions());
    }

    public void testCreate() {
        DebugGraphics result = (DebugGraphics) debugGraphics.create();
        assertEquals(debugGraphics.getDebugOptions(), result.getDebugOptions());
    }

    public void testConstructor() throws NullPointerException {
        new DebugGraphics();              
    }
}
