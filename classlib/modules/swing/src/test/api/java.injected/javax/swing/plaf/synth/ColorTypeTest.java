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

package javax.swing.plaf.synth;

import junit.framework.TestCase;

public class ColorTypeTest extends TestCase {

    private final static String NAME_1 = "test1"; //$NON-NLS-1$

    private final static String NAME_2 = "test2"; //$NON-NLS-1$

    public static void testColorTypes() {
        assertEquals(ColorType.MAX_COUNT, 5);
        assertNotSame(ColorType.BACKGROUND, ColorType.FOREGROUND);

        ColorType testCT = new ColorType(NAME_1) {
            // Empty class - all is inherited
        };

        ColorType testCT2 = new ColorType(NAME_2) {
            // Empty class - all is inherited
        };
        assertEquals(NAME_1, testCT.toString());
        assertEquals(NAME_2, testCT2.toString());
        assertFalse(ColorType.BACKGROUND.getID() == testCT.getID());
        assertFalse(testCT2.getID() == testCT.getID());
        assertTrue(testCT.getID() == testCT.getID());
        assertSame(ColorType.calculateColorType("BACKGROUND"), //$NON-NLS-1$
                ColorType.BACKGROUND);
        assertSame(ColorType.calculateColorType("FOREGROUND"), //$NON-NLS-1$
                ColorType.FOREGROUND);
        assertSame(ColorType.calculateColorType("FOCUS"), ColorType.FOCUS); //$NON-NLS-1$
        assertSame(ColorType.calculateColorType("TEXT_BACKGROUND"), //$NON-NLS-1$
                ColorType.TEXT_BACKGROUND);
        assertSame(ColorType.calculateColorType("TEXT_FOREGROUND"), //$NON-NLS-1$
                ColorType.TEXT_FOREGROUND);
        assertEquals(ColorType.MAX_COUNT, 5);
    }
}
