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
 * @author Evgueni V. Brevnov, Roman S. Bushmanov
 */
package java.lang;

import junit.framework.TestCase;

/**
 * tested class: java.lang.Class
 * tested method: getInterfaces
 */
public class ClassTestGetInterfaces extends TestCase {

    /**
     * Class.getInterfaces() method must return all interfaces declared in the
     * implements clause and must not return super interfaces these interfaces.
     *  
     */
    public void test1() {
        Class[] interfaces = c2.class.getInterfaces();
        assertEquals("array length:", interfaces.length, 1);
        assertEquals("names differ:", interfaces[0].getName(), i3.class.getName());
    }

    private interface i1 {
        int i = 0;

        int j = 0;
    }

    private interface i2 {
        int i = 1;

        int k = 1;
    }

    private interface i3 extends i2, i1 {
        int k = 2;
    }

    private class c1 implements i1 {
        int i = 3;
    }

    private class c2 extends c1 implements i3 {
        public int j = 4;
    }
}