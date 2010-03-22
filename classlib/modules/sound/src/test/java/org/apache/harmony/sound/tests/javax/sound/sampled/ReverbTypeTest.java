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

import javax.sound.sampled.ReverbType;

import junit.framework.TestCase;

public class ReverbTypeTest extends TestCase {

    public void testReverbType() {
        ReverbType rt1 = new MyReverbType("name", 1, 2f, 3, 4f, 5);

        assertEquals(
                "name, early reflection delay 1 ns, early reflection intensity 2.0 dB, late deflection delay 3 ns, late reflection intensity 4.0 dB, decay time 5",
                rt1.toString());
        assertEquals("name", rt1.getName());
        assertEquals(1, rt1.getEarlyReflectionDelay());
        assertEquals(2f, rt1.getEarlyReflectionIntensity());
        assertEquals(3, rt1.getLateReflectionDelay());
        assertEquals(4f, rt1.getLateReflectionIntensity());
        assertEquals(5, rt1.getDecayTime());

        ReverbType rt2 = new MyReverbType("name", 1, 2f, 3, 4f, 5);
        assertFalse(rt1.equals(rt2));
        assertTrue(rt1.equals(rt1));
    }

    private class MyReverbType extends ReverbType {
        public MyReverbType(String name, int earlyReflectionDelay,
                float earlyReflectionIntensity, int lateReflectionDelay,
                float lateReflectionIntensity, int decayTime) {

            super(name, earlyReflectionDelay, earlyReflectionIntensity,
                    lateReflectionDelay, lateReflectionIntensity, decayTime);
        }
    }
}
