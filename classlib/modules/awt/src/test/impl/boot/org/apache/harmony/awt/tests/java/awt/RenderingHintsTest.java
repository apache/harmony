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

package org.apache.harmony.awt.tests.java.awt;

import java.awt.RenderingHints;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class RenderingHintsTest extends TestCase {
    
    public void testConstructor_Null_Map() {
        // Regression test HARMONY-2517
        Map<RenderingHints.Key,?> localMap = new dummyMap(); 
        RenderingHints rh = new RenderingHints(localMap); 
        assertTrue(rh.isEmpty()); 
    }
    
    private class dummyMap implements Map<RenderingHints.Key, Object> { 
        public void clear() { 
            return; 
        } 

        public boolean containsKey(Object p0) { 
            return false; 
        } 

        public boolean containsValue(Object p0) { 
            return false; 
        } 

        public Set<Map.Entry<RenderingHints.Key, Object>> entrySet() { 
            return null; 
        } 

        public boolean equals(Object p0) { 
            return false; 
        } 

        public Object get(Object p0) { 
            return null; 
        } 

        public int hashCode() { 
            return 0; 
        } 

        public boolean isEmpty() { 
            return false; 
        } 

        public Set<RenderingHints.Key> keySet() { 
            return null; 
        } 

        public Object put(RenderingHints.Key p0, Object p1) { 
            return null; 
        } 

        public void putAll(Map p0) { 
            return; 
        } 

        public Object remove(Object p0) { 
            return null; 
        } 

        public int size() { 
            return 0; 
        } 

        public Collection<Object> values() { 
            return null; 
        } 
    }

} 

