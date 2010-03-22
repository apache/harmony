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

package org.apache.harmony.x.swing;

import java.awt.Font;
import java.awt.FontMetrics;
import javax.swing.SwingTestCase;

public class UtilitiesTest extends SwingTestCase {
    public void testClipString() {
        FontMetrics metrics;
        String clippedStr;
        String initialString = "Long enough text for this label, can you see that it is clipped now?";
        metrics = getFontMetrics(new Font("fixed", Font.PLAIN, 12));
        clippedStr = Utilities.clipString(metrics, initialString, 350);
        assertEquals("clipped string ", "Long enough text for this ...", clippedStr);
        clippedStr = Utilities.clipString(metrics, initialString, 100);
        assertEquals("clipped string ", "Long ...", clippedStr);
        clippedStr = Utilities.clipString(metrics, initialString, 10000);
        assertEquals("clipped string ", initialString, clippedStr);
        metrics = getFontMetrics(new Font("fixed", Font.PLAIN, 60));
        clippedStr = Utilities.clipString(metrics, initialString, 1500);
        assertEquals("clipped string ", "Long enough text for t...", clippedStr);
        metrics = getFontMetrics(new Font("fixed", Font.PLAIN, 50));
        clippedStr = Utilities.clipString(metrics, initialString, 500);
        assertEquals("clipped string ", "Long en...", clippedStr);
        metrics = getFontMetrics(new Font("fixed", Font.PLAIN, 60));
        clippedStr = Utilities.clipString(metrics, initialString, 5);
        assertEquals("clipped string ", "...", clippedStr);
        metrics = getFontMetrics(new Font("fixed", Font.PLAIN, 2));
        clippedStr = Utilities.clipString(metrics, initialString, 5);
        assertEquals("clipped string ", "...", clippedStr);
        metrics = getFontMetrics(new Font("fixed", Font.PLAIN, 3));
        clippedStr = Utilities.clipString(metrics, initialString, 5);
        assertEquals("clipped string ", "...", clippedStr);
    }

    public void testIsStringEmpty() {
        assertTrue(Utilities.isEmptyString(null));
        assertTrue(Utilities.isEmptyString(""));
        assertFalse(Utilities.isEmptyString(" "));
        assertFalse(Utilities.isEmptyString("\t"));
        assertFalse(Utilities.isEmptyString("\n"));
        assertFalse(Utilities.isEmptyString("\r"));
    }

    // Regression for HARMONY-2253
    public void testSafeIntSum() {
        assertEquals(0, Utilities.safeIntSum(9, -9));
        assertEquals(0, Utilities.safeIntSum(-9, 9));
        assertEquals(-18, Utilities.safeIntSum(-9, -9));
        assertEquals(Integer.MIN_VALUE + 1, Utilities.safeIntSum(Integer.MIN_VALUE, 1));
        // assertEquals(Integer.MIN_VALUE, Utilities.safeIntSum(Integer.MIN_VALUE, -1));
        // assertEquals(Integer.MIN_VALUE, Utilities.safeIntSum(Integer.MIN_VALUE + 2, -2));
    }
}
