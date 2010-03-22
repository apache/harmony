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

package org.apache.harmony.security.tests.java.security.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Timestamp;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Date;

import org.apache.harmony.security.tests.support.TestCertUtils;
import org.apache.harmony.testframework.serialization.SerializationTest;


/**
 * Serialization test for CodeSource.
 * 
 */

public class CodeSourceTest extends SerializationTest {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CodeSourceTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        TestCertUtils.install_test_x509_factory();
    }

    protected void tearDown() throws Exception {
        TestCertUtils.uninstall_test_x509_factory();
        super.tearDown();
    }

    public void testGolden() throws Throwable {
        super.testGolden();
        // do special testing to cover Exceptions in the {read/write}Data
        
        //
        // test writeObject()
        //
        
        {//~
            
        Certificate[] certs = new Certificate[] { new InvalidX509Certificate_00() };
        CodeSource cs = new CodeSource(null, certs);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        try {
            oos.writeObject(cs);
            fail("must not be here");
        }
        catch( IOException ex ) {
            // ok
        }

        }//~
        
        //
        // test readObject
        //
        {//~
        Certificate[] certs = new Certificate[] { new CertificateOfUnsupportedType() };
        CodeSource cs = new CodeSource(null, certs);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        
        oos.writeObject(cs);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        
        try {
            ois.readObject();
            fail("must not pass here");
        }
        catch(ClassNotFoundException ex) {
            // ok
        }
        }//~
        
        // test readObject, force the CertFactory to throw an Exception from 
        // inside the generateCertificate
        
        {//~
            Certificate[] certs = new Certificate[] { new InvalidX509Certificate_01() };
            CodeSource cs = new CodeSource(null, certs);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            
            oos.writeObject(cs);
            
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            
            try {
                ois.readObject();
                fail("must not pass here");
            }
            catch(IOException ex) {
                // ok
            }
        }//~
    }
    
    private class InvalidX509Certificate_00 extends TestCertUtils.TestX509Certificate {
        InvalidX509Certificate_00() {
            super(TestCertUtils.rootPrincipal, TestCertUtils.rootPrincipal);
        }
        public byte[] getEncoded() throws CertificateEncodingException {
            // simply throw an exception
            throw new CertificateEncodingException("CEE");
        }
    }
    
    private class InvalidX509Certificate_01 extends TestCertUtils.TestX509Certificate {
        InvalidX509Certificate_01() {
            super(TestCertUtils.rootPrincipal, TestCertUtils.rootPrincipal);
        }
        public byte[] getEncoded() throws CertificateEncodingException {
            // this is invalid encoding for TestX509Certificate
            return new byte[] {1,2,3,4,5}; 
        }
    }
    

    private class CertificateOfUnsupportedType extends TestCertUtils.TestCertificate {
        CertificateOfUnsupportedType() {
            super(null, "this is indeed unsupported type of certificates. I do believe so.");
        }
    }
    
    protected Object[] getData() {
        URL url;
        CodeSigner[] signers = null;
        CertPath cpath = TestCertUtils.getCertPath();
        Date now = new Date();
        Timestamp ts = new Timestamp(now, cpath);

        try {
            url = new URL("http://localhost");
            signers = new CodeSigner[] { new CodeSigner(cpath, ts) };
        } catch (Exception ex) {
            throw new Error(ex);
        }
        Certificate[] x509chain = new Certificate[] {
                TestCertUtils.rootCA
        };
        
        Object[] data = new Object[] {
                new CodeSource(url, (Certificate[])null),
                new CodeSource(url, new Certificate[0]),
                new CodeSource(url, signers),
                new CodeSource(null, x509chain),
        };
        return data;
    }

    public void testSerilizationDescriptor() throws Exception {

        // Regression for HARMONY-2787
        ObjectStreamClass objectStreamClass = ObjectStreamClass
                .lookup(CodeSource.class);
        ObjectStreamField objectStreamField = objectStreamClass
                .getField("location");
        assertEquals("Ljava/net/URL;", objectStreamField.getTypeString());
    }
}