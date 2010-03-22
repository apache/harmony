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

package org.apache.harmony.tests.java.lang.instrument;

import java.lang.instrument.ClassDefinition;

import junit.framework.TestCase;

public class ClassDefinitionTest extends TestCase {

    /**
     * @tests java.lang.instrument.ClassDefinition#ClassDefinition(Class<?>, byte[])
     */
    public void test_ConstructorLClass$B() {
        try{
            new ClassDefinition(null,null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try{
            new ClassDefinition(null,new byte[0]);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try{
            new ClassDefinition(this.getClass(),null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * @tests java.lang.instrument.ClassDefinition#getDefinitionClass()
     */
    public void test_getDefinitionClass() {
        ClassDefinition cd = new ClassDefinition(this.getClass(),new byte[0]);
        assertSame(this.getClass(),cd.getDefinitionClass());
    }

    /**
     * @tests java.lang.instrument.ClassDefinition#getDefinitionClassFile()
     */
    public void test_getDefinitionClassFile() {
        byte[] emptyByteArray = new byte[0];
        byte[] someByteArray = new byte[1024];
        ClassDefinition cd = new ClassDefinition(this.getClass(),emptyByteArray);
        assertSame(emptyByteArray,cd.getDefinitionClassFile());        
        cd = new ClassDefinition(this.getClass(),someByteArray);
        assertSame(someByteArray,cd.getDefinitionClassFile());        
    }
}
