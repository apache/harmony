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
 * @author Evgeny S. Sidorenko
 */

package org.apache.harmony.sound.tests.javax.sound.midi;

import javax.sound.midi.Soundbank;
import javax.sound.midi.SoundbankResource;

import junit.framework.TestCase;

public class SoundbankResourceTest extends TestCase
{
    /**
     * @tests javax.sound.midi.Soundbank#getName()
     */
    public void test_getName()
    {
        SoundbankResource1 sound = new SoundbankResource1( null, null, null );
        assertNull(sound.getName());
        
        SoundbankResource1 sound1 = new SoundbankResource1( null, "Test", null );
        assertEquals("Test", sound1.getName());
    }
    
    /**
     * @tests javax.sound.midi.Soundbank#getDataClass()
     */
    public void test_getDataClass()
    {
        SoundbankResource1 sound = new SoundbankResource1( null, null, null );
        assertNull(sound.getDataClass());
    }
    
    /**
     * Subsidiary class in order to use constructor
     * of class Instrument, because it declared as protected
     */
    static class SoundbankResource1 extends SoundbankResource
    {
        SoundbankResource1( Soundbank soundbank, String name, Class<?> dataClass )
        {
            super( soundbank, name, dataClass );
        }
        
        @Override
        public Object getData()
        {
            return null;
        }
    }
}
