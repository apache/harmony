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
 * @author Alexey A. Ivanov
 */
package javax.swing.text;

import junit.framework.TestCase;

public class TabSetTest extends TestCase {
    private TabSet tabSet;

    private final TabStop[] tabStops = { new TabStop(44f),
            new TabStop(56f, TabStop.ALIGN_CENTER, TabStop.LEAD_NONE),
            new TabStop(72f, TabStop.ALIGN_RIGHT, TabStop.LEAD_DOTS), new TabStop(100f) };

    public void testEquals() {
        assertTrue(tabSet.equals(tabSet));
        assertFalse(tabSet.equals(null));
        assertTrue(tabSet.equals(new TabSet(tabStops)));
        assertFalse(tabSet.equals(new TabSet(new TabStop[] { tabStops[1] })));
    }

    public void testGetTab() {
        for (int i = 0; i < tabStops.length; i++) {
            assertSame(tabStops[i], tabSet.getTab(i));
        }
        try {
            tabSet.getTab(-1);
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            tabSet.getTab(tabStops.length);
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetTabAfter() {
        assertSame(tabStops[0], tabSet.getTabAfter(0f));
        assertSame(tabStops[2], tabSet.getTabAfter(60f));
        assertNull(tabSet.getTabAfter(tabStops[3].getPosition() + 0.01f));
    }

    public void testGetTabCount() {
        assertEquals(tabStops.length, tabSet.getTabCount());
    }

    public void testGetTabIndex() {
        assertEquals(1, tabSet.getTabIndex(tabStops[1]));
        assertEquals(3, tabSet.getTabIndex(tabStops[3]));
        assertEquals(-1, tabSet.getTabIndex(null));
        assertEquals(-1, tabSet.getTabIndex(new TabStop(56f)));
    }

    public void testGetTabIndexAfter() {
        assertEquals(0, tabSet.getTabIndexAfter(0f));
        assertEquals(2, tabSet.getTabIndexAfter(60f));
        assertEquals(-1, tabSet.getTabIndexAfter(tabStops[3].getPosition() + 0.01f));
    }

    public void testHashcode() {
        assertEquals(tabSet.hashCode(), new TabSet(tabStops).hashCode());
    }

    public void testTabSet() {
        assertEquals(tabStops.length, tabSet.getTabCount());
        for (int i = 0; i < tabStops.length; i++) {
            assertSame(tabStops[i], tabSet.getTab(i));
        }
        TabStop prev = tabStops[1];
        tabStops[1] = new TabStop(60f);
        assertNotSame(tabStops[1], tabSet.getTab(1));
        assertSame(prev, tabSet.getTab(1));
    }

    public void testToString() {
        assertEquals("[ tab @44.0 - center tab @56.0 - "
                + "right tab @72.0 (w/leaders) - tab @100.0 ]", tabSet.toString());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tabSet = new TabSet(tabStops);
    }
}
