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

package javax.print;

import junit.framework.TestCase;

public class DocFlavorTest extends TestCase {
    public void testStatic() {
        String encoding = System.getProperty("file.encoding");
        assertEquals(encoding, DocFlavor.hostEncoding);
    }

    public void testHashCode() {
        DocFlavor f1 = new DocFlavor("text/plain; charset=us-ascii", "[B");
        DocFlavor f2 = new DocFlavor("text/plain; charset=us-ascii (comments)", "[B");
        assertEquals(f1.hashCode(), f1.hashCode());
        assertEquals(f1.hashCode(), f2.hashCode());
    }

    public void testMyDocFlavor() {
        tryNullPointer(null, "[B");
        tryNullPointer("text/plain", null);
    }

    public void testEqualsObject() {
        DocFlavor f1 = new DocFlavor("text/plain; charset=us-ascii", "[B");
        DocFlavor f2 = new DocFlavor("text/plain; charset=us-ascii (comments)", "[B");
        DocFlavor f3 = new DocFlavor("image/gif", "[B");
        DocFlavor f4 = new DocFlavor("text/plain", "[B");
        DocFlavor f5 = new DocFlavor("text/plain; charset=us-ascii", "[C");
        assertEquals(f1, f1);
        assertEquals(f1, f2);
        assertFalse(f1.equals(f3));
        assertFalse(f1.equals(f4));
        assertFalse(f1.equals(f5));
    }

    public void testGetMediaSubtype() {
        DocFlavor f = new DocFlavor("text/plain; charset=us-ascii", "[B");
        assertEquals("plain",f.getMediaSubtype());
    }

    public void testGetMediaType() {
        DocFlavor f = new DocFlavor("text/plain; charset=us-ascii", "[B");
        assertEquals("text", f.getMediaType());
    }

    public void testGetMimeType() {
        DocFlavor f = new DocFlavor("TEXT/plain; BBB=par1; aaa=par2 (comments)", "[B");
        assertEquals("text/plain; aaa=\"par2\"; bbb=\"par1\"", f.getMimeType());
    }

    public void testGetParameter() {
        DocFlavor f = new DocFlavor("TEXT/plain; BBB=par1; aaa=par2 (comments)", "[B");
        assertEquals("par1", f.getParameter("bbb"));
        assertNull(f.getParameter("absent"));
    }

    public void testGetRepresentationClassName() {
        DocFlavor f = new DocFlavor("TEXT/plain; BBB=par1; aaa=par2 (comments)", "[B");
        assertEquals("[B", f.getRepresentationClassName());
    }

    public void testToString() {
        DocFlavor f = new DocFlavor("TEXT/plain; BBB=par1; aaa=par2 (comments)", "[B");
        assertEquals("text/plain; aaa=\"par2\"; bbb=\"par1\"; class=\"[B\"", f.toString());
    }

    void tryNullPointer(String s1, String s2) {
        try {
            new DocFlavor(s1, s2);
            fail();
        } catch (NullPointerException e) {
            /* NullPointerException is expected here, so the test passes */
        }
    }
}