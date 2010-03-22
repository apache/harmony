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

package org.apache.harmony.security.tests.java.security;

import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;

public class CodeSource2Test extends junit.framework.TestCase {

	/**
	 * @throws Exception
	 * @tests java.security.CodeSource#CodeSource(java.net.URL,
	 *        java.security.cert.Certificate[])
	 */
	public void test_ConstructorLjava_net_URL$Ljava_security_cert_Certificate() throws Exception {
		// Test for method java.security.CodeSource(java.net.URL,
		// java.security.cert.Certificate [])
		new CodeSource(new java.net.URL("file:///test"),
				(Certificate[]) null);
	}

	/**
	 * @tests java.security.CodeSource#equals(java.lang.Object)
	 */
	public void test_equalsLjava_lang_Object() throws Exception {
		// Test for method boolean
		// java.security.CodeSource.equals(java.lang.Object)
		CodeSource cs1 = new CodeSource(new java.net.URL("file:///test"),
				(Certificate[]) null);
		CodeSource cs2 = new CodeSource(new java.net.URL("file:///test"),
				(Certificate[]) null);
		assertTrue("Identical objects were not equal()!", cs1.equals(cs2));
	}

	/**
	 * @tests java.security.CodeSource#hashCode()
	 */
	public void test_hashCode() throws Exception {
        URL url = new java.net.URL("file:///test");
        CodeSource cs = new CodeSource(url, (Certificate[]) null);
        assertEquals("Did not get expected hashCode!", cs.hashCode(), url
                .hashCode());
    }

	/**
	 * @tests java.security.CodeSource#getCertificates()
	 */
	public void test_getCertificates() throws Exception {
        CodeSource cs = new CodeSource(new java.net.URL("file:///test"),
                (Certificate[]) null);
        assertNull("Should have gotten null certificate list.", cs
                .getCertificates());
    }

	/**
	 * @tests java.security.CodeSource#getLocation()
	 */
	public void test_getLocation() throws Exception {
		// Test for method java.net.URL java.security.CodeSource.getLocation()
        CodeSource cs = new CodeSource(new java.net.URL("file:///test"),
                (Certificate[]) null);
        assertEquals("Did not get expected location!", "file:/test", cs
                .getLocation().toString());
        assertNotNull("Host should not be null", cs.getLocation().getHost());
    }

	/**
	 * @tests java.security.CodeSource#implies(java.security.CodeSource)
	 */
	public void test_impliesLjava_security_CodeSource() throws Exception {
		// Test for method boolean
        // java.security.CodeSource.implies(java.security.CodeSource)
        CodeSource cs1 = new CodeSource(new URL("file:/d:/somedir"),
                (Certificate[]) null);
        CodeSource cs2 = new CodeSource(new URL("file:/d:/somedir/"),
                (Certificate[]) null);
        assertTrue("Does not add /", cs1.implies(cs2));

        cs1 = new CodeSource(new URL("file", null, -1, "/d:/somedir/"),
                (Certificate[]) null);
        cs2 = new CodeSource(new URL("file:/d:/somedir/"), (Certificate[]) null);
        assertTrue("null host should imply host", cs1.implies(cs2));
        assertFalse("host should not imply null host", cs2.implies(cs1));
	}

}