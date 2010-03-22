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
* @author Alexander V. Astapchuk
*/

package org.apache.harmony.security.tests.java.security;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.security.auth.x500.X500Principal;

import org.apache.harmony.security.tests.support.TestCertUtils;

import junit.framework.TestCase;

/**
 * Unit test for CodeSource.
 * 
 */

public class CodeSource_ImplTest extends TestCase {

    /* Below are various URLs used during the testing */
    private static URL urlSite;

    static {
        try {
            String siteName = "www.intel.com";
            urlSite = new URL("http://"+siteName+"");
        } catch (MalformedURLException ex) {
            throw new Error(ex);
        }
    }

    /**
     * Test for equals(Object)<br>
     * The signer certificate may contain nulls, and equals() must not fail with NPE 
     */
    public void testEqualsObject_03() {
        Certificate cert0 = new TestCertUtils.TestCertificate();
        Certificate[] certs0 = new Certificate[] { cert0, null };
        Certificate[] certs1 = new Certificate[] { null, cert0 };
        CodeSource thiz = new CodeSource(urlSite, certs0);
        CodeSource that = new CodeSource(urlSite, certs1);
        assertTrue(thiz.equals(that));
    }

    /**
     * Test for equals(Object)<br>
     * Checks that both 'null' and not-null certs are taken into account - properly. 
     */
    public void testEqualsObject_05() {
        Certificate cert0 = new TestCertUtils.TestCertificate("cert0");
        Certificate cert1 = new TestCertUtils.TestCertificate("cert1");
        Certificate cert2 = new TestCertUtils.TestCertificate("cert2");

        Certificate[] smallSet = new Certificate[] { cert0, cert1 };
        Certificate[] bigSet = new Certificate[] { cert0, cert1, cert2 };

        CodeSource thiz = new CodeSource(urlSite, smallSet);
        CodeSource that = new CodeSource(urlSite, (Certificate[]) null);
        assertFalse(thiz.equals(that));

        that = new CodeSource(urlSite, bigSet);
        assertFalse(thiz.equals(that));

        thiz = new CodeSource(urlSite, bigSet);
        that = new CodeSource(urlSite, smallSet);
        assertFalse(thiz.equals(that));

        thiz = new CodeSource(urlSite, (Certificate[]) null);
        that = new CodeSource(urlSite, /*any set*/smallSet);
        assertFalse(thiz.equals(that));
        assertFalse(that.equals(thiz));
    }

    /**
     * getCodeSigners() must not take into account non-X509 certificates.
     */
    public void testGetCodeSigners_01() {
        Certificate[] certs = { new TestCertUtils.TestCertificate("00") };
        CodeSource cs = new CodeSource(null, certs);
        assertNull(cs.getCodeSigners());
    }

    /**
     * getCodeSigners() must return null if no X509 factory available
     */
    public void testGetCodeSigners_02() {
        ArrayList al = new ArrayList();
        boolean noMoreFactories = false;
        try {
            // remove all providers for x509
            // 'for' loop here for the sake of avoiding endless loop - well, just 
            // in case if something is wrong with add/remove machinery.
            // '100' seems reasonable big to remove all necessary providers
            // ...
            for (int i = 0; i < 100; i++) {
                try {
                    CertificateFactory f = CertificateFactory
                            .getInstance("X.509");
                    al.add(f.getProvider());
                    Security.removeProvider(f.getProvider().getName());
                } catch (CertificateException ex) {
                    noMoreFactories = true;
                    break;
                }
            }
            if (!noMoreFactories) {
                throw new Error(
                        "Unable to setup test: too many providers provide X.509");
            }
            Certificate[] certs = new Certificate[] { TestCertUtils.rootCA };
            CodeSource cs = new CodeSource(null, certs);
            assertNull(cs.getCodeSigners());
        } finally {
            // .. and restore providers back - to avoid affecting following tests
            for (int i = 0; i < al.size(); i++) {
                Security.addProvider((Provider) al.get(i));
            }
        }

    }

    /**
     * getCodeSigners() must return an array of CodeSigners. Just make sure
     * the array looks healthy.
     */
    public void testGetCodeSigners_03() {
        TestCertUtils.install_test_x509_factory();
        try {
            X500Principal[] ps = TestCertUtils.UniGen.genX500s(3);

            // 2-certs chain 
            X509Certificate rootCA = TestCertUtils.rootCA;
            X509Certificate c0 = new TestCertUtils.TestX509Certificate(ps[0],
                    rootCA.getIssuerX500Principal());
            //
            X509Certificate c1 = new TestCertUtils.TestX509Certificate(ps[1],
                    ps[1]);
            X509Certificate c2 = new TestCertUtils.TestX509Certificate(ps[2],
                    ps[2]);

            java.security.cert.Certificate [] certs = new java.security.cert.Certificate[] {
                    c0, rootCA, c1, c2 };
            CodeSource cs = new CodeSource(null, certs);
            CodeSigner[] signers = cs.getCodeSigners();

            // must get exactly 3 CodeSigner-s: one for the chain, and one 
            // for each of single certs
            assertEquals(3, signers.length);
        } finally {
            TestCertUtils.uninstall_test_x509_factory();
        }
    }

    /**
     * getCodeSigners(). Make sure, that CertException is handled properly
     */
    public void testGetCodeSigners_04() {
        try {
            TestCertUtils.install_test_x509_factory();
            X500Principal[] ps = TestCertUtils.UniGen.genX500s(1);

            // 2-certs chain 
            X509Certificate rootCA = TestCertUtils.rootCA;
            X509Certificate c0 = new TestCertUtils.TestInvalidX509Certificate(
                    ps[0], rootCA.getIssuerX500Principal());
            java.security.cert.Certificate [] certs = new java.security.cert.Certificate[] {
                    c0, rootCA };

            CodeSource cs = new CodeSource(null, certs);
            CodeSigner[] signers = cs.getCodeSigners();

            assertNull(signers);

            // Must force a check for 'factory==null' 
            cs.getCodeSigners();
        } finally {
            TestCertUtils.uninstall_test_x509_factory();
        }
    }
    

    /**
     * @tests java.security.CodeSource#toString()
     */
    public void test_toString() throws Exception {
        // Test for method java.lang.String java.security.CodeSource.toString()
        CodeSource cs = new CodeSource(new java.net.URL("file:///test"),
                (Certificate[]) null);
        assertEquals("toString did not return expected value.",
                "CodeSource, url=file:/test, <no certificates>", cs.toString());
    }

}
