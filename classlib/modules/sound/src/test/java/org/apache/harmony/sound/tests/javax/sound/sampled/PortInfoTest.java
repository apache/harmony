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

package org.apache.harmony.sound.tests.javax.sound.sampled;

import javax.sound.sampled.Port;

import junit.framework.TestCase;

public class PortInfoTest extends TestCase {

    public void testPortInfo() {
        assertEquals("COMPACT_DISC source port", Port.Info.COMPACT_DISC.toString());
        assertEquals("LINE_OUT target port", Port.Info.LINE_OUT.toString());

        Class<Port> cl = Port.class;
        String name = "name";
        Port.Info pi1 = new Port.Info(cl, name, true);
        assertEquals(name, pi1.getName());
        assertTrue(pi1.isSource());
        
        Port.Info pi2 = new Port.Info(cl, name, true);
        assertFalse(pi1.equals(pi2));
        assertTrue(pi1.equals(pi1));        
        assertTrue(pi1.matches(pi2));
        assertTrue(pi2.matches(pi1));

        pi2 = new Port.Info(cl, "name1", false);
        assertTrue(pi1.matches(pi1));
        assertFalse(pi1.matches(pi2));
        assertFalse(pi2.matches(pi1));
    }

}
