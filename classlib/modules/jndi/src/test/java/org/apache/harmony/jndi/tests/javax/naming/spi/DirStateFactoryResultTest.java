/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.jndi.tests.javax.naming.spi;

import javax.naming.directory.BasicAttributes;
import javax.naming.spi.DirStateFactory;

import junit.framework.TestCase;
import org.apache.harmony.jndi.tests.javax.naming.util.Person;

public class DirStateFactoryResultTest extends TestCase {
    /**
     * Test Result(Object o, Attributes a) with normal values
     */
    public void testConstructor_Simple() {
        Person person = Person.getInstance();
        BasicAttributes attributes = new BasicAttributes("Anyuser", person);
        String strObj = "Harmony";
        DirStateFactory.Result result =
            new DirStateFactory.Result(strObj, attributes);
        assertEquals(strObj, result.getObject());
        assertEquals(attributes, result.getAttributes());
    }

    /**
     * Test Result(Object o, Attributes a) with the first param o as null
     */
    public void testConstructor_ObjectNull() {
        Person person = Person.getInstance();
        BasicAttributes attributes = new BasicAttributes("Anyuser", person);
        DirStateFactory.Result result =
            new DirStateFactory.Result(null, attributes);
        assertNull(result.getObject());
        assertEquals(attributes, result.getAttributes());
    }

    /**
     * Test Result(Object o, Attributes a) with the second param attributes as null
     */
    public void testConstructor_AttributesNull() {
        String strObj = "Harmony";
        DirStateFactory.Result result =
            new DirStateFactory.Result(strObj, null);
        assertEquals(strObj, result.getObject());
        assertNull(result.getAttributes());
        
    }

}
