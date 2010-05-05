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

package org.apache.harmony.security;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.harmony.security.DefaultPolicyScanner;
import junit.framework.TestCase;


/**
 * TODO Put your class description here
 * 
 */

public class DefaultPolicyScannerTest extends TestCase {
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(DefaultPolicyScannerTest.class);
    }

    private static String IO_ERROR = "Failed intentionally";

    private DefaultPolicyScanner scanner;

    private static StreamTokenizer getST(String sample) {
        return new StreamTokenizer(new StringReader(sample));
    }

    private static StreamTokenizer getFailingST(String sample) {
        return new StreamTokenizer(new StringReader(sample)) {

            public int nextToken() throws IOException {
                throw new IOException(IO_ERROR);
            }
        };
    }

    protected void setUp() throws Exception {
        super.setUp();
        scanner = new DefaultPolicyScanner();
    }

    /**
     * Tests tokenization of valid policy sample with all possible elements.
     */
    public void testScanStream_Complex() throws Exception {
        List grants = new ArrayList();
        List keys = new ArrayList();
        Reader r = new StringReader(
            "keystore \"url1\";grant{}KeyStore \"url2\", \"type2\""
                + "keystore \"url3\"\nGRANT signedby \"duke,Li\", "
                + "codebase\"\", principal a.b.c \"alias\"{permission XXX \"YYY\", SignedBy \"ZZZ\" \n \t };;;");
        scanner.scanStream(r, grants, keys);

        assertEquals("3 keystores expected", 3, keys.size());
        String[] urls = new String[] {
            "url1", "url2", "url3" };
        String[] types = new String[] {
            null, "type2", null };
        for (int i = 0; i < 3; i++) {
            DefaultPolicyScanner.KeystoreEntry key = (DefaultPolicyScanner.KeystoreEntry)keys
                .get(i);
            assertEquals(urls[i], key.url);
            assertEquals(types[i], key.type);
        }
        assertEquals("2 grants expected", 2, grants.size());
        DefaultPolicyScanner.GrantEntry ge = (DefaultPolicyScanner.GrantEntry)grants
            .get(0);
        assertTrue(ge.codebase == null && ge.signers == null
            && ge.principals == null && ge.permissions.size() == 0);

        ge = (DefaultPolicyScanner.GrantEntry)grants.get(1);
        assertTrue(ge.codebase.equals("") && ge.signers.equals("duke,Li")
            && ge.principals.size() == 1 && ge.permissions.size() == 1);

        DefaultPolicyScanner.PrincipalEntry pn = (DefaultPolicyScanner.PrincipalEntry)ge.principals
            .iterator().next();
        assertTrue(pn.klass.equals("a.b.c") && pn.name.equals("alias"));

        DefaultPolicyScanner.PermissionEntry pe = (DefaultPolicyScanner.PermissionEntry)ge.permissions
            .iterator().next();
        assertTrue(pe.klass.equals("XXX") && pe.name.equals("YYY")
            && pe.actions == null && pe.signers.equals("ZZZ"));
    }

    public void testScanStream_Empty() throws Exception {
        scanner.scanStream(new StringReader(""), new ArrayList(),
                           new ArrayList());
    }

    /**
     * Tests that scanStream() throws InvalidFormatException on invalid
     * keywords.
     */
    public void testScanStream_Invalid() throws Exception {
        List grants = new ArrayList();
        List keys = new ArrayList();
        try {
            scanner.scanStream(new StringReader(
                "keystore \"url1\"; granted{} grant{}"), grants, keys);
            fail("InvalidFormatException is not thrown");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {
            assertTrue(keys.size() == 1 && grants.size() == 0);

        }

        try {
            scanner.scanStream(new StringReader(
                "grant{} grant{} keyshop \"url1\"; keystore \"url1\";"),
                               grants, keys);
            fail("InvalidFormatException is not thrown 2");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {
            assertTrue(keys.size() == 1 && grants.size() == 2);

        }
    }

    /**
     * Tests that scanStream() throws IOException if stream fails.
     */
    public void testScanStream_IOException() throws Exception {
        try {
            scanner.scanStream(new Reader() {

                public void close() throws IOException {
                }

                public int read(char[] cbuf, int off, int count)
                    throws IOException {
                    throw new IOException(IO_ERROR);
                }
            }, new ArrayList(), new ArrayList());
            fail("IOException is intercepted");
        } catch (IOException ok) {
            assertEquals(IO_ERROR, ok.getMessage());
        }
    }

    /**
     * Tests that both handleUnexpectedToken() methods throw proper
     * InvalidFormatException exception.
     */
    public void testHandleUnexpectedToken() {
        try {
            scanner.handleUnexpectedToken(getST(""));
            fail("InvalidFormatException is not thrown");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }

        String message = "bla-bli-blu";
        try {
            scanner.handleUnexpectedToken(getST(""), message);
            fail("InvalidFormatException is not thrown 2");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {
            assertNotNull(ok.getMessage());
            assertTrue(ok.getMessage().indexOf(message) >= 0);
        }
    }

    /**
     * Tests readKeystoreEntry() for tokenizing all valid syntax variants.
     */
    public void testReadKeystoreEntry() throws Exception {
        DefaultPolicyScanner.KeystoreEntry ke = scanner
            .readKeystoreEntry(getST("\"URL1\""));
        assertEquals("URL1", ke.url);
        assertNull(ke.type);

        ke = scanner.readKeystoreEntry(getST("\"URL2\",\"TYPE2\""));
        assertEquals("URL2", ke.url);
        assertEquals("TYPE2", ke.type);

        ke = scanner.readKeystoreEntry(getST("\"URL3\" \"TYPE3\""));
        assertEquals("URL3", ke.url);
        assertEquals("TYPE3", ke.type);
    }

    /**
     * Tests that readKeystoreEntry() throws IOException if stream fails.
     */
    public void testReadKeystoreEntry_IOException() throws Exception {
        try {
            scanner.readKeystoreEntry(getFailingST(""));
            fail("IOException is intercepted");
        } catch (IOException ok) {
            assertEquals(IO_ERROR, ok.getMessage());
        }
    }

    /**
     * Tests that readKeystoreEntry() throws InvalidFormatException on invalid
     * input
     */
    public void testReadKeystoreEntry_Invalid() throws Exception {
        try {
            scanner.readKeystoreEntry(getST(""));
            fail("InvalidFormatException is not thrown 1");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner.readKeystoreEntry(getST(" ;"));
            fail("InvalidFormatException is not thrown 2");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner.readKeystoreEntry(getST("URL"));
            fail("InvalidFormatException is not thrown 3");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
    }

    /**
     * Tests readPrincipalEntry() for tokenizing all valid syntax variants.
     */
    public void testReadPrincipalEntry() throws Exception {
        DefaultPolicyScanner.PrincipalEntry pe = scanner
            .readPrincipalEntry(getST("\"name1\""));
        assertEquals("name1", pe.name);
        assertNull(pe.klass);

        pe = scanner.readPrincipalEntry(getST("a.b.c.d\"name 2\""));
        assertEquals("name 2", pe.name);
        assertEquals("a.b.c.d", pe.klass);

        pe = scanner.readPrincipalEntry(getST("* *"));
        assertEquals(DefaultPolicyScanner.PrincipalEntry.WILDCARD, pe.name);
        assertEquals(DefaultPolicyScanner.PrincipalEntry.WILDCARD, pe.klass);

        pe = scanner.readPrincipalEntry(getST("* \"name3\""));
        assertEquals("name3", pe.name);
        assertEquals(DefaultPolicyScanner.PrincipalEntry.WILDCARD, pe.klass);

        pe = scanner.readPrincipalEntry(getST("clazz *"));
        assertEquals(DefaultPolicyScanner.PrincipalEntry.WILDCARD, pe.name);
        assertEquals("clazz", pe.klass);

        pe = scanner.readPrincipalEntry(getST("\"a, b, c, d\""));
        assertEquals("a,b,c,d", pe.name);
        assertNull(pe.klass);
    }

    /**
     * Tests that readPrincipalEntry() throws InvalidFormatException on invalid
     * input
     */
    public void testReadPrincipalEntry_Invalid() throws Exception {
        try {
            scanner.readPrincipalEntry(getST(""));
            fail("InvalidFormatException is not thrown 1");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner.readPrincipalEntry(getST(" ;"));
            fail("InvalidFormatException is not thrown 2");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner.readPrincipalEntry(getST("class"));
            fail("InvalidFormatException is not thrown 3");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner.readPrincipalEntry(getST("class name"));
            fail("InvalidFormatException is not thrown 4");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner.readPrincipalEntry(getST("class, \"name\""));
            fail("InvalidFormatException is not thrown 5");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner.readPrincipalEntry(getST("* name"));
            fail("InvalidFormatException is not thrown 6");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
    }

    /**
     * Tests that readPrincipalEntry() throws IOException if stream fails.
     */
    public void testReadPrincipalEntry_IOException() throws Exception {
        try {
            scanner.readPrincipalEntry(getFailingST(""));
            fail("IOException is intercepted");
        } catch (IOException ok) {
            assertEquals(IO_ERROR, ok.getMessage());
        }
    }

    /**
     * Tests readPermissionEntries() for tokenizing empty list of permissions.
     */
    public void testReadPermissionEntries_Empty() throws Exception {
        Collection perms = scanner.readPermissionEntries(getST("}"));
        assertNotNull(perms);
        assertEquals(0, perms.size());
    }

    /**
     * Tests readPermissionEntries() for tokenizing all valid syntax variants,
     * for just one permission.
     */
    public void testReadPermissionEntries_Single() throws Exception {
        Collection perms = scanner
            .readPermissionEntries(getST("permission a.b.c; }"));
        assertEquals(1, perms.size());
        DefaultPolicyScanner.PermissionEntry pe = (DefaultPolicyScanner.PermissionEntry)perms
            .iterator().next();
        assertEquals("a.b.c", pe.klass);
        assertNull(pe.name);
        assertNull(pe.actions);
        assertNull(pe.signers);

        perms = scanner
            .readPermissionEntries(getST("permission a.b.c \"name1\" }"));
        assertEquals(1, perms.size());
        pe = (DefaultPolicyScanner.PermissionEntry)perms.iterator().next();
        assertEquals("a.b.c", pe.klass);
        assertEquals("name1", pe.name);
        assertNull(pe.actions);
        assertNull(pe.signers);

        perms = scanner
            .readPermissionEntries(getST("permission a.b.c \"name2\", \"action2\"}"));
        assertEquals(1, perms.size());
        pe = (DefaultPolicyScanner.PermissionEntry)perms.iterator().next();
        assertEquals("a.b.c", pe.klass);
        assertEquals("name2", pe.name);
        assertEquals("action2", pe.actions);
        assertNull(pe.signers);

        perms = scanner
            .readPermissionEntries(getST("permission a.b.c \"name3\" signedby \"\"}"));
        assertEquals(1, perms.size());
        pe = (DefaultPolicyScanner.PermissionEntry)perms.iterator().next();
        assertEquals("a.b.c", pe.klass);
        assertEquals("name3", pe.name);
        assertNull(pe.actions);
        assertEquals("", pe.signers);

        perms = scanner
            .readPermissionEntries(getST("permission a.b.c4 ,\"actions4\" SignedBy \"sig4\"}"));
        assertEquals(1, perms.size());
        pe = (DefaultPolicyScanner.PermissionEntry)perms.iterator().next();
        assertEquals("a.b.c4", pe.klass);
        assertNull(pe.name);
        assertEquals("actions4", pe.actions);
        assertEquals("sig4", pe.signers);

        perms = scanner
            .readPermissionEntries(getST("permission a.b.c5 \"name5\",\"actions5\",signedby \"sig5\";}"));
        assertEquals(1, perms.size());
        pe = (DefaultPolicyScanner.PermissionEntry)perms.iterator().next();
        assertEquals("a.b.c5", pe.klass);
        assertEquals("name5", pe.name);
        assertEquals("actions5", pe.actions);
        assertEquals("sig5", pe.signers);
    }

    /**
     * Tests readPermissionEntries() for tokenizing valid syntax for a list of
     * permissions.
     */
    public void testReadPermissionEntries_List() throws Exception {
        Collection perms = scanner
            .readPermissionEntries(getST("permission a.b.c"
                + " permission qwerty ,\"aaa\";"
                + "permission zzz signedby \"xxx\"}"));
        assertEquals(3, perms.size());

        for (Iterator it = perms.iterator(); it.hasNext();) {
            DefaultPolicyScanner.PermissionEntry pe = (DefaultPolicyScanner.PermissionEntry)it
                .next();
            if ("a.b.c".equals(pe.klass)) {
                assertNull(pe.name);
                assertNull(pe.actions);
                assertNull(pe.signers);
            } else if ("qwerty".equals(pe.klass)) {
                assertNull(pe.name);
                assertEquals("aaa", pe.actions);
                assertNull(pe.signers);
            } else if ("zzz".equals(pe.klass)) {
                assertNull(pe.name);
                assertNull(pe.actions);
                assertEquals("xxx", pe.signers);
            } else {
                fail("Unknown permission reported: " + pe.klass);
            }
        }
    }

    /**
     * Tests that readPermissionEntries() throws InvalidFormatException on
     * invalid input
     */
    public void testReadPermissionEntries_Invalid() throws Exception {
        try {
            scanner.readPermissionEntries(getST("permission a.b.c"));
            fail("InvalidFormatException is not thrown 1");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner.readPermissionEntries(getST("permission;}"));
            fail("InvalidFormatException is not thrown 2");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner.readPermissionEntries(getST("permission class name}"));
            fail("InvalidFormatException is not thrown 3");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner.readPermissionEntries(getST("permission class ,,}"));
            fail("InvalidFormatException is not thrown 4");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner
                .readPermissionEntries(getST("permission class \"name\", signedby signers}"));
            fail("InvalidFormatException is not thrown 5");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
    }

    /**
     * Tests that readPermissionEntries() throws IOException if stream fails.
     */
    public void testReadPermissionEntries_IOException() throws Exception {
        try {
            scanner.readPermissionEntries(getFailingST(""));
            fail("IOException is intercepted");
        } catch (IOException ok) {
            assertEquals(IO_ERROR, ok.getMessage());
        }
    }

    /**
     * Tests readGrantEntry() for tokenizing all valid syntax variants.
     */
    public void testReadGrantEntry_Empty() throws Exception {
        DefaultPolicyScanner.GrantEntry ge = scanner.readGrantEntry(getST(""));
        assertNull(ge.codebase);
        assertNull(ge.codebase);
        assertNull(ge.principals);
        assertTrue(ge.permissions == null || ge.permissions.isEmpty());

        ge = scanner.readGrantEntry(getST("{}"));
        assertNull(ge.codebase);
        assertNull(ge.codebase);
        assertNull(ge.principals);
        assertTrue(ge.permissions == null || ge.permissions.isEmpty());
    }

    /**
     * Tests readGrantEntry() for tokenizing all valid syntax variants.
     */
    public void testReadGrantEntry() throws Exception {
        DefaultPolicyScanner.GrantEntry ge = scanner
            .readGrantEntry(getST("codebase \"u1\" signedby \"s1\";"));
        assertEquals("u1", ge.codebase);
        assertEquals("s1", ge.signers);
        assertNull(ge.principals);

        ge = scanner.readGrantEntry(getST("signedby \"s2\" codebase \"u2\";"));
        assertEquals("u2", ge.codebase);
        assertEquals("s2", ge.signers);
        assertNull(ge.principals);

        ge = scanner.readGrantEntry(getST("signedby \"s2\",signedby \"s3\" "
            + " codebase \"u2\",codebase \"u3\";"));
        assertEquals("u3", ge.codebase);
        assertEquals("s3", ge.signers);
        assertNull(ge.principals);

        ge = scanner.readGrantEntry(getST("principal \"a1\" signedby \"s4\" "
            + "principal \"a2\", codebase \"u4\";"));
        assertEquals("u4", ge.codebase);
        assertEquals("s4", ge.signers);
        assertNotNull(ge.principals);
        assertEquals(2, ge.principals.size());

        ge = scanner.readGrantEntry(getST("principal * *, principal bbb \"b2\""
            + ", principal ccc \"c2\" codebase \"u5\";"));
        assertEquals("u5", ge.codebase);
        assertNull(ge.signers);
        assertNotNull(ge.principals);
        assertEquals(3, ge.principals.size());

        ge = scanner.readGrantEntry(getST("principal * *"
            + ", signedby \"s6\";"));
        assertNull(ge.codebase);
        assertEquals("s6", ge.signers);
        assertNotNull(ge.principals);
        assertEquals(1, ge.principals.size());
    }

    /**
     * Tests that readGrantEntry() throws InvalidFormatException on invalid
     * input
     */
    public void testReadGrantEntry_Invalid() throws Exception {
        try {
            scanner.readGrantEntry(getST("codebase"));
            fail("InvalidFormatException is not thrown 1");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner.readGrantEntry(getST("signedby"));
            fail("InvalidFormatException is not thrown 2");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner.readGrantEntry(getST("signedby *"));
            fail("InvalidFormatException is not thrown 3");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner.readGrantEntry(getST("codebase file://URL"));
            fail("InvalidFormatException is not thrown 4");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
        try {
            scanner.readGrantEntry(getST("codebase \"a2\", signedby \"a2\", "
                + "principal a \"a2\", b \"b2\" "));
            fail("InvalidFormatException is not thrown 5");
        } catch (DefaultPolicyScanner.InvalidFormatException ok) {

        }
    }

    /**
     * Tests that readGrantEntry() throws IOException if stream fails.
     */
    public void testReadGrantEntry_IOException() throws Exception {
        try {
            scanner.readGrantEntry(getFailingST(""));
            fail("IOException is intercepted");
        } catch (IOException ok) {
            assertEquals(IO_ERROR, ok.getMessage());
        }
    }
}
