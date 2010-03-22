/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

package org.apache.harmony.jretools.tests.keytool;

import java.io.File;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;

import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;

import junit.framework.TestCase;

import org.apache.harmony.jretools.keytool.KeytoolException;
import org.apache.harmony.jretools.keytool.Main;
import org.apache.harmony.jretools.toolutils.KeyStoreLoaderSaver;

/**
 * Tests "-genkey" option of Keytool.
 */
public class GenKeyTest extends TestCase {
    /**
     * Test method for generation of a key pair and wrapping it into a a
     * certificate (self-signed or signed with another certificate from the
     * store) with method 'KeyCertGenerator.genKey(KeytoolParameters)'
     */
    public void testGenKey_keyPair() throws Exception {
        
        // need to create keystore in a temporary directory
        String tempDir = System.getProperty("java.io.tmpdir")
                + File.separatorChar;
        String keyStorePath = tempDir + "GenKeyTestTemporaryFile";

        File keyStoreFile = new File(keyStorePath);
        // Quit if such file exists for some reason.
        if (keyStoreFile.exists()) {
            assertTrue("Cannot delete a temporary file " + keyStorePath,
                    keyStoreFile.delete());
        }
        // The file will be created by the KeyStoreLoaderSaver,
        // delete it when exiting.

        // normal parameters //
        // parameters for key pair with self-signed cerificate generation
        String[] selfSignedArgs = TestUtils.genKeySelfSignedArgs;
        String[] genKeyNoIssuerArgs = new String[selfSignedArgs.length];
        System.arraycopy(selfSignedArgs, 0, genKeyNoIssuerArgs, 0,
                selfSignedArgs.length);
        // set keystore
        genKeyNoIssuerArgs[2] = keyStorePath;
        // set distinguished name
        genKeyNoIssuerArgs[18] = "CN=selfSigned";

        // parameters to create a key pair with certificate signed by just
        // generated certificate and key pair
        // +4 will contain "-issuer" and "-issuerpass" options and their
        // values (2+2)
        String[] genKeyIssuerArgs = new String[selfSignedArgs.length + 4];
        System.arraycopy(selfSignedArgs, 0, genKeyIssuerArgs, 0,
                selfSignedArgs.length);
        // "-issuer alias -issuerpass 321321"
        genKeyIssuerArgs[genKeyIssuerArgs.length - 4] = "-issuer";
        genKeyIssuerArgs[genKeyIssuerArgs.length - 3] = genKeyNoIssuerArgs[8];
        genKeyIssuerArgs[genKeyIssuerArgs.length - 2] = "-issuerpass";
        genKeyIssuerArgs[genKeyIssuerArgs.length - 1] = TestUtils.keyPass;
        // set keystore
        genKeyIssuerArgs[2] = keyStorePath;
        // set alias
        genKeyIssuerArgs[8] = "issued";

        try {
            // Firstly generate a self-signed cert
            String[] args = genKeyNoIssuerArgs;
            String errMsgPrefix = "Self-signed cert generation: ";
            for (int p = 0; p < 2; p++) {
                if (p == 1) {
                    // secondly generate a cert, signed by an issuer
                    args = genKeyIssuerArgs;
                    errMsgPrefix = "Issued cert generation: ";
                }
                
                // current alias
                String curAlias = args[8];
    
                // run Keytool with given arguments
                Main.run(args);
    
                // read the result
                KeyStore keyStore = KeyStoreLoaderSaver.loadStore(keyStorePath,
                        args[6], TestUtils.ksPass.toCharArray(), null);
                // check the result
                assertTrue(errMsgPrefix + "alias " + curAlias
                        + " does not exist in keystore", keyStore
                        .containsAlias(curAlias));
                assertTrue(errMsgPrefix + "alias " + curAlias
                        + " is not a key entry", keyStore.isKeyEntry(curAlias));
                Key key = keyStore.getKey(curAlias, TestUtils.keyPass
                        .toCharArray());
                // check if alg = "RSA"
                assertEquals(errMsgPrefix + "unexpected key algorithm. ",
                        args[14], key.getAlgorithm().toUpperCase());
                X509Certificate cert = (X509Certificate) keyStore
                        .getCertificate(curAlias);
                X500Principal prnc = cert.getSubjectX500Principal();
                // check if the name = "CN=CN,OU=OU,O=O,L=L,ST=ST,C=C"
                assertEquals(errMsgPrefix + "unexpected name. ", args[18], prnc
                        .getName());
                X500Principal issuerPrnc = cert.getIssuerX500Principal();
                // if generated self-signed certificate
                if (p == 0) {
                    // check if the issuer and subject equal
                    assertEquals(errMsgPrefix + "unexpected principal. ", prnc,
                            issuerPrnc);
                    cert.verify(cert.getPublicKey());
                } else { // if signed with certificate chain
                    // check if issuer name is "CN=selfSigned"
                    assertEquals(errMsgPrefix + "unexpected issuer name. ",
                            genKeyNoIssuerArgs[18], issuerPrnc.getName());
                    cert.verify(keyStore.getCertificate(genKeyNoIssuerArgs[8])
                            .getPublicKey());
                }
    
                // check validity period
                // 86400000 milliseconds in one day
                long curPlusValidity = System.currentTimeMillis() + 86400000
                        * (new Integer(args[20])).intValue();
                // 300000 ms is 5 minutes
                cert.checkValidity(new Date(curPlusValidity - 300000));

                assertEquals(errMsgPrefix + "unexpected serial number. ",
                        new BigInteger(args[24]), cert.getSerialNumber());
                assertEquals(errMsgPrefix + "unexpected version. ",
                        new Integer(args[22]).intValue(), cert.getVersion());
    
                // Encrypt data with the private key and decrypt
                // it with the certificate.
                PrivateKey privateKey = (PrivateKey) keyStore.getKey(curAlias,
                        TestUtils.keyPass.toCharArray());
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, privateKey);
                byte[] clearText = "Betty Botter bought some butter".getBytes();
                byte[] cipherText = cipher.doFinal(clearText);
                cipher.init(Cipher.DECRYPT_MODE, cert);
                byte[] decrypted = cipher.doFinal(cipherText);
                assertTrue(errMsgPrefix + "unexpected decryption result. ",
                        Arrays.equals(clearText, decrypted));
            }
    
            // remove the added entries
            genKeyNoIssuerArgs[0] = "-delete";
            Main.run(genKeyNoIssuerArgs);
            genKeyNoIssuerArgs[0] = "-genkey";
    
            genKeyIssuerArgs[0] = "-delete";
            Main.run(genKeyIssuerArgs);
            genKeyIssuerArgs[0] = "-genkey";
            
            // bad parameters //
            // error message
            String excNotThrown = TestUtils.excNotThrown;
            
            // bad key size
            args = genKeyNoIssuerArgs;
            String keySize = args[12];
            args[12] = "1";
            try {
                Main.run(args);
                fail(excNotThrown);
            } catch (IllegalArgumentException ok){
            }
            // set normal key size back
            args[12] = keySize;
    
            // bad key algorithm
            String keyAlg = args[14];
            args[14] = "badKeyAlg";
            try {
                Main.run(args);
                fail(excNotThrown);
            } catch (NoSuchAlgorithmException ok){
            }
            // set normal key algorithm back
            args[14] = keyAlg;

            // bad signature algorithm
            String sigAlg = args[16];
            args[16] = "badSigAlg";
            try {
                Main.run(args);
                fail(excNotThrown);
            } catch (NoSuchAlgorithmException ok) {
            }

            // normal but incompatible signature algorithm
            args[16] = "SHA1withDSA";
            try {
                Main.run(args);
                fail(excNotThrown);
            } catch (InvalidKeyException ok) {
            }
            // set compatible signature algorithm back
            args[16] = sigAlg;

            // try to sign with issuer with bad parameters
            args = genKeyIssuerArgs;

            // bad issuer alias
            String issuerAlias = args[26];
            args[26] = "badIssuerAlias";
            try {
                Main.run(args);
                fail(excNotThrown);
            } catch (KeytoolException ok) {
            }
            // set normal issuer alias back
            args[26] = issuerAlias;
        } finally {
            keyStoreFile.delete();
        }
    }
}
