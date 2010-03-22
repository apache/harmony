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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Alexei Y. Zakharov
 */

package org.apache.harmony.jndi.provider.dns;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test class to test 
 * <code>org.apache.harmony.jndi.provider.dns.DNSNameParser</code> class. 
 * @author Alexei Zakharov
 */
public class DNSNameParserTest extends TestCase {

    /**
     * Tests a <code>parse()</code> method.
     * @see org.apache.harmony.jndi.provider.dns.DNSNameParser#parse(String)
     */
    public void testParse() throws NamingException {
        String nameStr1 = "www.intel.com";
        String nameStr2 = "mydomain.com.";
        String nameStr3 = "myhost.mysubdomain.mydomain.com.";
        String nameStr4 = "mydomain.com..";
        String nameStr5 = "ddddddddddddddddddddddddddddddddddddddddddddddd" +
                "dddddddddddddddddddddddddddddddddddddddddddddddddd" +
                "dddddddddddddddddddddddddddddddddddddddddddddddddd.a.com";
        String nameStr6 = "aaaaaaaaaa.aaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaa." +
                "aaaaaaaaaa.aaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaa." +
                "aaaaaaaaaa.aaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaa." +
                "aaaaaaaaaa.aaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaa." +
                "aaaaaaaaaa.aaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaa." +
                "aaaaaaaaaa.aaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaa." +
                "aaaaaaaaaa.aaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaa." +
                "aaaaaaaaaa.aaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaa." +
                "aaaaaaaaaa.aaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaa." +
                "aaaaaaaaaa.aaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaa." +
                "aaaaaaaaaa.aaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaa." +
                "aaaaaaaaaa.aaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaa." +
                "aaaaaaaaaa.aaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaa";
        Name name;
        
        DNSNameParser parser = new DNSNameParser();
        name = parser.parse(nameStr1);
        assertEquals("The size of the name is wrong", 3, name.size());
        assertEquals("Wrong label", "com", name.get(0));
        assertEquals("Wrong label", "intel", name.get(1));
        assertEquals("Wrong label", "www", name.get(2));
        name = parser.parse(nameStr2);
        assertEquals("The size of the name is wrong", 3, name.size());
        assertEquals("Wrong label", "", name.get(0));
        assertEquals("Wrong label", "com", name.get(1));
        assertEquals("Wrong label", "mydomain", name.get(2));
        name = parser.parse(nameStr3);
        assertEquals("The size of the name is wrong", 5, name.size());
        assertEquals("Wrong label", "", name.get(0));
        assertEquals("Wrong label", "com", name.get(1));
        assertEquals("Wrong label", "mydomain", name.get(2));
        assertEquals("Wrong label", "mysubdomain", name.get(3));
        assertEquals("Wrong label", "myhost", name.get(4));
        try {
            name = parser.parse(nameStr4);
            fail("DNS name contains two null labels but " +
                    "InvalidNameException has not been thrown while parsing");
        } catch (InvalidNameException e) {}
        try {
            name = parser.parse(nameStr5);
            fail("Too long label was given but " +
                    "InvalidNameException was not thrown while parsing");
        } catch (InvalidNameException e) {}
        try {
            name = parser.parse(nameStr6);
            fail("Too long name was given but " +
                    "InvalidNameException was not thrown while parsing");
        } catch (InvalidNameException e) {}            
    }

    public static Test suite() {
        return new TestSuite(DNSNameParserTest.class);
    }

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

}
