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
 * @author Roman S. Bushmanov
 */
package java.lang;

import java.util.Map;

import junit.framework.TestCase;

public class SystemTest extends TestCase {

    /**
     * Test for String getenv(String)
     */
    public void testGetenvString() {
        assertNull("_NONEXISTENT_ENVIRONMENT_VARIABLE_", 
            System.getenv("_NONEXISTENT_ENVIRONMENT_VARIABLE_"));
        try {
            System.getenv(null);
            fail("NPE is expected");
        } catch (NullPointerException ok) {}
    }

    /**
     * test for Map getenv()
     */
    public void testGetenv() {
        Map<String, String> env = System.getenv();
        for (String key : env.keySet()) {
            assertEquals("Value of " + key, env.get(key), System.getenv(key));
        }
    }

    public void testNanoTime() {
        long start = System.nanoTime();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail("Was interruptred");
        }
        long duration = System.nanoTime() - start;
        assertTrue("Elapsed time should be >0", duration > 0);
    }
    
    public void testNanoTime1() {
        long startNano = System.nanoTime();
        long startMilli = System.currentTimeMillis();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("Was interruptred");
        }
        long durationNano = System.nanoTime() - startNano;
        long durationMillis = System.currentTimeMillis() - startMilli;
        assertTrue("Bad accuracy!", Math.abs(durationNano / 1000000 - durationMillis) < 50);
    }
    
    public void testClearProperty(){
        String name = "user.name"; 
        String initialValue = System.getProperty(name);
        String newValue = "Baba Yaga";
        System.setProperty(name, newValue);
        assertEquals("incorrect value", newValue, System.getProperty(name));
        System.clearProperty(name);
        assertNull("user.name should not be null", System.getProperty(name));
        System.setProperties(null);
        assertEquals("user.name has not been restored", initialValue, System.getProperty(name));
    }
}