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
import java.beans.StringPersistenceDelegate;

import junit.framework.TestCase;

/**
 * Test the internal class java.beans.StringPersistenceDelegate.
 */
public class StringPersistenceDelegateTest extends TestCase {

    private StringPersistenceDelegate pd = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pd = new StringPersistenceDelegate();
    }

    public void testMutates() {
        assertFalse(pd.mutatesTo("", null));
        assertFalse(pd.mutatesTo(null, null));
        assertFalse(pd.mutatesTo("str1", "str2"));
        assertTrue(pd.mutatesTo("str1", "str1"));
    }

    public void testInitialize() {
        pd.initialize(null, null, null, null);
    }

    public void testInstantiate_Normal() throws Exception {
        Expression exp = pd.instantiate("str", new Encoder());

        assertSame("str", exp.getValue());
        assertSame(String.class, exp.getTarget());
        assertEquals("new", exp.getMethodName());
        assertEquals(1, exp.getArguments().length);
        assertEquals("str", exp.getArguments()[0]);
    }
}
