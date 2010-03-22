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

import javax.sound.sampled.Mixer;

import junit.framework.TestCase;

public class MixerTest extends TestCase {

    public void testMixer() {

        Mixer.Info info = new MyMixerInfo("NAME", "VENDOR", "DESCRIPTION",
                "VERSION");
        assertEquals("DESCRIPTION", info.getDescription());
        assertEquals("NAME", info.getName());
        assertEquals("VENDOR", info.getVendor());
        assertEquals("VERSION", info.getVersion());
        assertEquals("NAME, version VERSION", info.toString());
    }
    
    public void testEquals() {
        Mixer.Info mi1 = new MyMixerInfo("1", "2", "3", "4");
        Mixer.Info mi2 = new MyMixerInfo("1", "2", "3", "4");
        assertFalse(mi1.equals(mi2));
        assertTrue(mi1.equals(mi1));
    }

    private class MyMixerInfo extends Mixer.Info {
        public MyMixerInfo(String name, String vendor, String description,
                String version) {
            super(name, vendor, description, version);
        }
    }
}