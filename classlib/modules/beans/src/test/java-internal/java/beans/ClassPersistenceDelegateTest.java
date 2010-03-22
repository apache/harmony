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

package java.beans;

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.ClassPersistenceDelegate;

import junit.framework.TestCase;

/**
 * Test the internal class java.beans.ClassPersistenceDelegate.
 */
public class ClassPersistenceDelegateTest extends TestCase {

    private ClassPersistenceDelegate pd = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pd = new ClassPersistenceDelegate();
    }

    public void testMutates() {
        assertFalse(pd.mutatesTo(Class.class, null));
        assertFalse(pd.mutatesTo(null, null));
        assertFalse(pd.mutatesTo(Class.class, String.class));
        assertTrue(pd.mutatesTo(String.class, String.class));
    }

    public void testInitialize() {
        pd.initialize(null, null, null, null);
    }

    public void testInstantiate_Normal() throws Exception {
        Expression exp = pd.instantiate(Integer.class, new Encoder());

        assertSame(Integer.class, exp.getValue());
        assertTrue(exp.getTarget() instanceof Class);
        assertEquals("forName", exp.getMethodName());
        assertEquals(1, exp.getArguments().length);
        assertEquals("java.lang.Integer", exp.getArguments()[0]);
    }

    public void testInstantiate_Primitive() throws Exception {
        Expression exp = pd.instantiate(Integer.TYPE, new Encoder());

        assertSame(Integer.TYPE, exp.getValue());
        assertTrue(exp.getTarget() instanceof java.lang.reflect.Field);
        assertEquals("get", exp.getMethodName());
        assertEquals(1, exp.getArguments().length);
        assertNull(exp.getArguments()[0]);
    }

    public void testInstantiate_Class() throws Exception {
        Expression exp = pd.instantiate(Class.class, new Encoder());

        assertSame(Class.class, exp.getValue());
        assertTrue(exp.getTarget() instanceof Class);
        assertEquals("forName", exp.getMethodName());
        assertEquals(1, exp.getArguments().length);
        assertEquals("java.lang.Class", exp.getArguments()[0]);
    }
}
