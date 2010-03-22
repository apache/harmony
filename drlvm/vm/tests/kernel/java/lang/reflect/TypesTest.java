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

package java.lang.reflect;

import junit.framework.TestCase;

public class TypesTest extends TestCase {

    /**
     * Regression test for HARMONY-5780  
     */
    public void testParameterizedTypeImpl() throws Exception {
        Method get = Container.class.getDeclaredMethod("get");
        Method set = Container.class.getDeclaredMethod("set", Element.class);
        Type arg = set.getGenericParameterTypes()[0];
        Type ret = get.getGenericReturnType();
        assertTrue("equals", arg.equals(ret));
        assertEquals("toString", arg.toString(),ret.toString());
    }
}

class Element<T> { 
} 

class Container { 

    protected Element<Container> el; 

    public Element<Container> get() { 
        return el; 
    } 

    public void set(Element<Container> value) { 
        this.el = value; 
    } 
} 
