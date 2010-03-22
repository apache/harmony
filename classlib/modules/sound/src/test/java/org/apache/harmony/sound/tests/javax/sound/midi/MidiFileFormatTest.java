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

package org.apache.harmony.sound.tests.javax.sound.midi;

import java.util.Hashtable;
import java.util.Map;

import javax.sound.midi.MidiFileFormat;

import junit.framework.TestCase;

public class MidiFileFormatTest extends TestCase {
    /**
     * test constructor(int, float, int, int, long, Map<String, Object>)
     */
    public void test_constructor() {
        Map<String, Object> table = new Hashtable<String, Object>();
        table.put("name", "harmony");
        table.put("organization", "apache");
        MidiFileFormat format = new MidiFileFormat(1, 2.0f, 3, -4, 5L, table);
        assertNull(format.getProperty("unknown"));
        assertEquals("harmony", format.getProperty("name"));
        assertEquals("apache", format.getProperty("organization"));
        
        try {
            format = new MidiFileFormat(1, 2.0f, 3, 4, 5L, null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }
    
    /**
     * test method getByteLength()
     * 
     */
    public void test_getByteLength() {
        MidiFileFormat format = new MidiFileFormat(1, 2.0f, 3, 4, 5L);
        assertEquals(4, format.getByteLength());
    }
    
    /**
     * test method getType()
     *
     */
    public void test_getType() {
        MidiFileFormat format = new MidiFileFormat(1, 2.0f, 3, 4, 5L);
        assertEquals(1, format.getType());
        
        format = new MidiFileFormat(10, 2.0f, 3, 4, 5L);
        assertEquals(10, format.getType());
    }
    
    /**
     * test method getMicrosecondLength()
     *
     */
    public void test_getMicrosecondLength() {
        MidiFileFormat format = new MidiFileFormat(1, 2.0f, 3, 4, 5L);
        assertEquals(5L, format.getMicrosecondLength());
    }
    
    /**
     * test method getProperty(String)
     *
     */
    public void test_getProperty() {
        MidiFileFormat format = new MidiFileFormat(1, 2.0f, 3, 4, 5L);
        assertNull(format.getProperty("name"));
        
        Map<String, Object> table = new Hashtable<String, Object>();
        table.put("first", "one");
        table.put("second", "two");
        format = new MidiFileFormat(1, 2.0f, 3, 4, 5L, table);
        assertNull(format.getProperty(null));
        assertNull(format.getProperty("third"));
        assertEquals("one", format.getProperty("first"));
        assertEquals("two", format.getProperty("second"));
        table.put("first", "not one");
        table.put("second", "not two");
        /*
         * values don't change!!!
         */
        assertEquals("one", format.getProperty("first"));
        assertEquals("two", format.getProperty("second"));
    }
    
    /**
     * test method properties()
     *
     */
    public void test_properties() {
        Map<String, Object> table = new Hashtable<String, Object>();
        table.put("first", "one");
        table.put("second", "two");
        MidiFileFormat format = new MidiFileFormat(1, 2.0f, 3, 4, 5L, table);
        
        table = format.properties();
        assertEquals("one", table.get("first"));
        
        try {
            table.put("first", "not one");
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {}
    }

}
