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
 * @author Roman I. Chernyatchik
 */
package javax.swing.text;

import javax.swing.BasicSwingTestCase;
import junit.framework.TestCase;

public class TabStopTest extends TestCase {
    private TabStop tabStop;

    public TabStopTest(final String name) {
        super(name);
    }

    public void testEquals() throws Exception {
        TabStop tabStop1;
        TabStop tabStop2;
        tabStop1 = new TabStop(0f, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
        tabStop2 = new TabStop(0f, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
        assertEquals(tabStop1, tabStop2);
        tabStop1 = new TabStop(-12.11f, 3, -5);
        tabStop2 = new TabStop(-12.11f, 3, -5);
        assertEquals(tabStop1, tabStop2);
        tabStop1 = new TabStop(0f, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
        tabStop2 = new TabStop(1f, TabStop.ALIGN_CENTER, TabStop.LEAD_EQUALS);
        assertFalse(tabStop1.equals(tabStop2));
        tabStop1 = new TabStop(-12.11f, TabStop.ALIGN_LEFT, -5);
        tabStop2 = new TabStop(-12.1099999f, TabStop.ALIGN_LEFT, -5);
        assertEquals(tabStop1, tabStop2);
        tabStop1 = new TabStop(-12.11f, 3, -6);
        tabStop2 = new TabStop(-12.12f, 3, -5);
        assertFalse(tabStop1.equals(tabStop2));
        tabStop1 = new TabStop(-12.1f, TabStop.ALIGN_LEFT, TabStop.LEAD_EQUALS);
        assertFalse(tabStop1.equals("test"));
    }

    public void testToString() throws Exception {
        tabStop = new TabStop(1024f, TabStop.ALIGN_BAR, TabStop.LEAD_DOTS);
        assertEquals("bar tab @1024.0 (w/leaders)", tabStop.toString());
        tabStop = new TabStop(-15f, TabStop.ALIGN_CENTER, TabStop.LEAD_EQUALS);
        assertEquals("center tab @-15.0 (w/leaders)", tabStop.toString());
        tabStop = new TabStop(-5f, TabStop.ALIGN_DECIMAL, TabStop.LEAD_HYPHENS);
        assertEquals("decimal tab @-5.0 (w/leaders)", tabStop.toString());
        tabStop = new TabStop(0f, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
        assertEquals("tab @0.0", tabStop.toString());
        tabStop = new TabStop(12.11f, TabStop.ALIGN_RIGHT, TabStop.LEAD_THICKLINE);
        assertEquals("right tab @12.11 (w/leaders)", tabStop.toString());
        tabStop = new TabStop(12.11f, 20, TabStop.LEAD_UNDERLINE);
        assertEquals("tab @12.11 (w/leaders)", tabStop.toString());
        tabStop = new TabStop(12.11f);
        assertEquals("tab @12.11", tabStop.toString());
        tabStop = new TabStop(12.11f, -23, 50);
        assertEquals("tab @12.11 (w/leaders)", tabStop.toString());
    }

    public void testHashCode() throws Exception {
        assertEquals(new TabStop(3f, TabStop.ALIGN_RIGHT, TabStop.LEAD_HYPHENS).hashCode(),
                new TabStop(3f, TabStop.ALIGN_RIGHT, TabStop.LEAD_HYPHENS).hashCode());
        assertEquals(new TabStop(Float.MAX_VALUE, TabStop.ALIGN_RIGHT, TabStop.LEAD_DOTS)
                .hashCode(), new TabStop(Float.MAX_VALUE, TabStop.ALIGN_RIGHT,
                TabStop.LEAD_DOTS).hashCode());
        assertEquals(new TabStop(Float.MIN_VALUE, -3, 7).hashCode(), new TabStop(
                Float.MIN_VALUE, -3, 7).hashCode());
        int hashCode1 = new TabStop(3, TabStop.ALIGN_RIGHT, TabStop.LEAD_HYPHENS).hashCode();
        int hashCode2 = new TabStop(1, TabStop.ALIGN_RIGHT, TabStop.LEAD_HYPHENS).hashCode();
        assertTrue(hashCode1 != hashCode2);
    }

    public void testGetLeader() throws Exception {
        tabStop = new TabStop(2.11f, 0, TabStop.LEAD_EQUALS);
        assertEquals(TabStop.LEAD_EQUALS, tabStop.getLeader());
        tabStop = new TabStop(20f, 3, TabStop.LEAD_UNDERLINE);
        assertEquals(TabStop.LEAD_UNDERLINE, tabStop.getLeader());
        tabStop = new TabStop(12.11f, -23, 50);
        assertEquals(50, tabStop.getLeader());
    }

    public void testGetPosition() throws Exception {
        tabStop = new TabStop(0f, TabStop.ALIGN_CENTER, TabStop.LEAD_DOTS);
        assertEquals(0, tabStop.getPosition(), 0.0001);
        tabStop = new TabStop(-2.5f, TabStop.ALIGN_CENTER, TabStop.LEAD_DOTS);
        assertEquals(-2.5, tabStop.getPosition(), 0.0001);
        tabStop = new TabStop(13554111f, TabStop.ALIGN_CENTER, TabStop.LEAD_DOTS);
        assertEquals(13554111, tabStop.getPosition(), 0.0001);
        tabStop = new TabStop(-13554111f, TabStop.ALIGN_CENTER, TabStop.LEAD_DOTS);
        assertEquals(-13554111, tabStop.getPosition(), 0.0001);
    }

    public void testGetAlignmet() throws Exception {
        tabStop = new TabStop(2.5f, TabStop.ALIGN_DECIMAL, TabStop.LEAD_DOTS);
        assertEquals(TabStop.ALIGN_DECIMAL, tabStop.getAlignment());
        tabStop = new TabStop(0f, 3, TabStop.LEAD_DOTS);
        assertEquals(3, tabStop.getAlignment());
        tabStop = new TabStop(-1f, -3, 7);
        assertEquals(-3, tabStop.getAlignment());
    }

    public void testSerializable() throws Exception {
        tabStop = new TabStop(10f, TabStop.ALIGN_CENTER, TabStop.LEAD_HYPHENS);
        tabStop = (TabStop) BasicSwingTestCase.serializeObject(tabStop);
        assertEquals(10f, tabStop.getPosition(), 0.0001);
        assertEquals(TabStop.ALIGN_CENTER, tabStop.getAlignment());
        assertEquals(TabStop.LEAD_HYPHENS, tabStop.getLeader());
    }
}
