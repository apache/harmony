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


package org.apache.harmony.jndi.internal.parser;

import java.util.List;

import javax.naming.directory.Attribute;

import junit.framework.TestCase;

public class LdapRdnParserTest extends TestCase {
    public void test_getList() throws Exception {
        LdapRdnParser parser1 = new LdapRdnParser("cn=test");
        LdapRdnParser parser2 = new LdapRdnParser("o=harmony+sn=test");

        List list = parser1.getList();

        assertEquals(1, list.size());
        assertEquals("cn", ((Attribute) list.get(0)).getID()); 
        assertEquals("test", ((Attribute) list.get(0)).get());
    }
}
