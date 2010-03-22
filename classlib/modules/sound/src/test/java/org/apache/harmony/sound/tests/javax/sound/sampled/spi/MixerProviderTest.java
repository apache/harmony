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

package org.apache.harmony.sound.tests.javax.sound.sampled.spi;

import javax.sound.sampled.Mixer;
import javax.sound.sampled.spi.MixerProvider;

import junit.framework.TestCase;

public class MixerProviderTest extends TestCase {

    static Mixer.Info[] recorded;

    /**
     * @tests javax.sound.midi.spi.MixerProvider#isMixerSupported(Mixer.Info)
     */
    public void testIsMixerSupported() {
        MixerProvider myProvider = new MixerProvider() {

            @Override
            public Mixer getMixer(Mixer.Info info) {
                fail("what are doing here?");
                return null;
            }

            @Override
            public Mixer.Info[] getMixerInfo() {
                return recorded;
            }
        };

        recorded = new Mixer.Info[] { new Mixer.Info("1", "2", "3", "4") {} };
        assertTrue(myProvider.isMixerSupported(recorded[0]));

        Mixer.Info similar = new Mixer.Info("1", "2", "3", "4") {};
        assertFalse(myProvider.isMixerSupported(similar));

        try {
            myProvider.isMixerSupported(null);
            fail("NPE expected");
        } catch (NullPointerException e) {}

        recorded = null;
        try {
            myProvider.isMixerSupported(similar);
            fail("NPE expected");
        } catch (NullPointerException e) {}
    }
}
