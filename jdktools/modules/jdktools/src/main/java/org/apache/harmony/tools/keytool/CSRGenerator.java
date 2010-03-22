/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.harmony.tools.keytool;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Vector;

import org.apache.harmony.luni.util.Base64;
import org.apache.harmony.security.pkcs10.CertificationRequest;
import org.apache.harmony.security.pkcs10.CertificationRequestInfo;
import org.apache.harmony.security.x501.Name;
import org.apache.harmony.security.x509.AlgorithmIdentifier;
import org.apache.harmony.security.x509.SubjectPublicKeyInfo;

/**
 * Class for generating X.509 Certificate Signing Requests (CSRs). The generated
 * certificate request is printed to a file, if its name is supplied in param,
 * or otherwise printed to stdout.
 */
public class CSRGenerator {
    /**
     * Generates a Certificate Signing Request (CSR). The request is generated
     * based on data taken from keystore entry associated with alias given in
     * parameter param. The certificate request is printed to a file, if its
     * name is supplied in param, or otherwise printed to stdout.
     * 
     * @param param
     * @throws KeyStoreException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws InvalidKeyException
     * @throws SignatureException
     * @throws NoSuchProviderException
     * @throws KeytoolException 
     * @throws CertificateException 
     */
    static void certReq(KeytoolParameters param) throws KeyStoreException,
            NoSuchAlgorithmException, UnrecoverableKeyException, IOException,
            InvalidKeyException, SignatureException, NoSuchProviderException,
            KeytoolException, CertificateException {

        KeyStore keyStore = param.getKeyStore();
        String alias = param.getAlias();

        if (!keyStore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
            throw new KeytoolException(
                    "Failed to generate a certificate request. \n" + "Entry <"
                            + alias + "> is not a private key entry. ");
        }

        // get the existing certificate and keys associated with the alias
        X509Certificate cert = (X509Certificate) keyStore.getCertificate(param
                .getAlias());
        PrivateKey privateKey;
        try {
            privateKey = (PrivateKey) keyStore.getKey(param.getAlias(), param
                    .getKeyPass());
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException(
                    "Cannot find the algorithm to recover the key. ", e);
        }
        PublicKey publicKey = cert.getPublicKey();

        Name distinguishedName;
        try {
            distinguishedName = new Name(cert.getSubjectDN().getName());
        } catch (IOException e) {
            throw (IOException) new IOException(
                    "Failed to generate a distinguished name. ").initCause(e);
        }

        SubjectPublicKeyInfo subjectPublicKeyInfo = null;
        try {
            subjectPublicKeyInfo = (SubjectPublicKeyInfo) SubjectPublicKeyInfo.ASN1
                    .decode(publicKey.getEncoded());
        } catch (IOException e) {
            throw (IOException) new IOException(
                    "Failed to decode SubjectPublicKeyInfo. ").initCause(e);
        }

        // generate CertificationRequestInfo based on data taken from
        // the existing certificate.
        CertificationRequestInfo certReqInfo = new CertificationRequestInfo(
                cert.getVersion(), distinguishedName, subjectPublicKeyInfo,
                // attributes
                new Vector());
        byte[] infoEncoding = certReqInfo.getEncoded();

        // generate the signature
        String sigAlgName = (param.getSigAlg() != null) ? param.getSigAlg()
                : cert.getSigAlgName();

        Signature sig;
        String sigProvider = (param.getSigProvider() != null) ? param
                .getSigProvider() : param.getProvider();
        try {
            sig = (sigProvider != null) ? Signature.getInstance(sigAlgName,
                    sigProvider) : Signature.getInstance(sigAlgName);
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException("The algorithm " + sigAlgName
                    + " is not found in the environment.", e);
        } catch (NoSuchProviderException e) {
            throw (NoSuchProviderException) new NoSuchProviderException(
                    "The provider " + sigProvider
                            + " is not found in the environment.").initCause(e);
        }

        try {
            sig.initSign(privateKey);
        } catch (InvalidKeyException e) {
            throw new InvalidKeyException(
                    "The private key used to generate the signature is invalid.",
                    e);
        }

        byte[] signatureValue;
        try {
            sig.update(infoEncoding, 0, infoEncoding.length);
            signatureValue = sig.sign();
        } catch (SignatureException e) {
            throw new SignatureException("Failed to sign the certificate. ", e);
        }

        // generating the request
        CertificationRequest certReq = new CertificationRequest(certReqInfo,
                new AlgorithmIdentifier(cert.getSigAlgOID()), signatureValue);
        byte[] certReqEncoding = certReq.getEncoded();

        OutputStream output;
        // if no file name is given, output to System.out
        String fileName = param.getFileName();
        if (fileName == null) {
            output = System.out;
        } else { // output to a file if the name is supplied
            File file = new File(fileName);
            // the file will be created if it doesn't already exist.
            // If it already exists and is not a file, then an IOException will
            // be thrown.
            file.createNewFile();

            output = new BufferedOutputStream(new FileOutputStream(file));
        }

        output.write("-----BEGIN NEW CERTIFICATE REQUEST-----\n".getBytes());
        output.write(Base64.encode(certReqEncoding, "ISO-8859-1").getBytes());
        output.write("\n-----END NEW CERTIFICATE REQUEST-----\n".getBytes());
        output.flush();

        if (param.isVerbose() && fileName != null) {
            System.out.println("The certificate request is stored in file <"
                    + fileName + ">.");
        }
    }
}

