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
package org.apache.harmony.jndi.tests.javax.naming;

import javax.naming.RefAddr;

import junit.framework.TestCase;

public class RefAddrTest extends TestCase {
    public static class MockRefAddr extends RefAddr {

        private String contents;

        public MockRefAddr(String type, String address) {
            super(type);
            this.contents = address;
        }

        @Override
        public Object getContent() {
            return contents;
        }

    }

    public void testToString_Simple() {
        MockRefAddr addr = new MockRefAddr("type", "contents");

        assertNotNull(addr.toString());
        assertEquals("Type: " + addr.getType() + "\nContent: "
                + addr.getContent() + "\n", addr.toString());
    }

    public void testToString_AddressNull() {
        MockRefAddr addr = new MockRefAddr("type", null);

        assertNotNull(addr.toString());
        assertEquals("Type: " + addr.getType() + "\nContent: null\n", addr
                .toString());
    }

    public void testToString_TypeNull() {
        MockRefAddr addr = new MockRefAddr(null, "contents");

        assertNotNull(addr.toString());
        assertEquals("Type: null\nContent: " + addr.getContent() + "\n", addr
                .toString());
    }
}
