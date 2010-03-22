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
 * tested class: java.lang.Class
 * tested method: GetClasses
 * 
 * @author Evgueni V. Brevnov
 */

package java.lang;

import junit.framework.TestCase;

/**
 * @tested class: java.lang.Class
 * @tested method: getClasses
 */
public class ClassTestGetClasses extends TestCase {
    
    /**
     * java.lang.Class class does not declare any public inner class.
     *
     */
    public void test1() {
        Class[] classes = Class.class.getClasses();
        assertNotNull("Unexpected null", classes);
        assertEquals("array length:", 0, classes.length);
    }
    /**
     * Only public inner class must be returned.
     *
     */
    public void test2() {
        Class[] classes = ClassTestGetClasses.class.getClasses();
        assertNotNull("Unexpected null", classes);
        assertEquals("There must be one class in the list", 1, classes.length);
        assertSame("Objects differ", Helper1.class, classes[0]);        
    }
    
    public class Helper1 {
        public class Inner {            
        }        
    }
    
    protected class Helper2 {       
    }
    
    class Helper3 {        
    }
}
