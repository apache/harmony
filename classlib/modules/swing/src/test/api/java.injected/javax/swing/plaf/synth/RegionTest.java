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

public class RegionTest extends TestCase {

    private static final String NAME = "test"; //$NON-NLS-1$

    private static final String UI = "testUI"; //$NON-NLS-1$

    private static final Region r = new Region(NAME, UI, false);

    public static void testFields() {
        assertEquals(NAME, r.getName());
        assertFalse(r.isSubregion());
    }

    public static void testRegionLookupMethods() {
        assertSame(r, Region.getRegionFromName(NAME));
        assertSame(r, Region.getRegionFromUIID(UI));
    }
}
