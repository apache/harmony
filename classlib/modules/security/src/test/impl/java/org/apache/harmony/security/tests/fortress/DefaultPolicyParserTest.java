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
* @author Alexey V. Varlamov
*/

package org.apache.harmony.security.tests.fortress;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.security.CodeSource;
import java.security.Principal;
import java.security.SecurityPermission;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Iterator;

import org.apache.harmony.security.PolicyEntry;
import org.apache.harmony.security.fortress.DefaultPolicyParser;
import junit.framework.TestCase;


/**
 * Tests for DefaultPolicyParser
 * 
 */

public class DefaultPolicyParserTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DefaultPolicyParserTest.class);
    }

    /**
     * Tests parsing of a sample policy from temporary file, validates returned
     * PolicyEntries. 
     */
    public void testParse() throws Exception {
        File tmp = File.createTempFile("policy", null);
        try {
        FileWriter out = new FileWriter(tmp);
        out.write("grant{}KeyStore \"url2\", \"type2\" "
                + "GRANT signedby \"duke,Li\", codebase\"\", principal a.b.c \"guest\" "
                + "{permission XXX \"YYY\", SignedBy \"ZZZ\" \n \t };;;"
                + "GRANT codebase\"http://a.b.c/-\", principal * * "
                + "{permission java.security.SecurityPermission \"YYY\";}"
                + "GRANT {permission java.security.SecurityPermission \"ZZZ\";}"
                + "GRANT {permission java.security.UnresolvedPermission \"NONE\";}");
        out.flush();
        out.close();

        DefaultPolicyParser parser = new DefaultPolicyParser();
        Collection entries = parser.parse(tmp.toURI().toURL(), null);
        assertEquals(2, entries.size());
        for (Iterator iter = entries.iterator(); iter.hasNext();) {
            PolicyEntry element = (PolicyEntry)iter.next();
            if (element.getPermissions()
                .contains(new SecurityPermission("ZZZ"))) {
                assertTrue(element.impliesCodeSource(new CodeSource(null,
                    (Certificate[])null)));
                assertTrue(element.impliesPrincipals(null));
            } else if (element.getPermissions()
                .contains(new SecurityPermission("YYY"))) {
                assertFalse(element.impliesCodeSource(null));
                assertFalse(element.impliesPrincipals(null));
                assertTrue(element.impliesCodeSource(new CodeSource(new URL(
                    "http://a.b.c/-"), (Certificate[])null)));
                assertTrue(element
                    .impliesPrincipals(new Principal[] { new FakePrincipal(
                        "qqq") }));
            } else {
                fail("Extra entry parsed");
            }
        }
        } finally {
            tmp.delete();
        }
    }
}

class FakePrincipal implements Principal {

    private String name;

    public FakePrincipal(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
