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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

/**
 * Class for importing certificates - adding to trusted certificates or
 * Certificate Signing Request (CSR) replies from certificate authorities
 * (CAs). CSR reply can be a single X.509 certificate or a PKCS#7-formatted
 * certificate chain containing X.509 certificates. X.509 v1, v2 and v3
 * certificates are supported. Certificates to import can be in binary (DER) or
 * printable (PEM) encoding format.
 */
public class CertImporter {

    /**
     * Reads an X.509 certificate or a PKCS#7 formatted certificate chain from
     * the file specified in param and puts it into the entry identified by the
     * supplied alias. If the input file is not specified, the certificates are
     * read from the standard input.
     * 
     * @param param
     * @throws KeytoolException
     * @throws IOException
     * @throws CertPathBuilderException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws FileNotFoundException
     * @throws NoSuchProviderException
     * @throws KeyStoreException
     */
    static void importCert(KeytoolParameters param)
            throws FileNotFoundException, CertificateException,
            NoSuchAlgorithmException, UnrecoverableKeyException,
            CertPathBuilderException, IOException, KeytoolException,
            NoSuchProviderException, KeyStoreException {

        String alias = param.getAlias();
        KeyStore keyStore = param.getKeyStore();
        boolean contains = keyStore.containsAlias(alias);
        String certProvider = (param.getCertProvider() != null) ? param
                .getCertProvider() : param.getProvider();

        // if the alias already exists, try to import the certificate as
        // a cert reply
        if (contains
                && keyStore.entryInstanceOf(alias,
                        KeyStore.PrivateKeyEntry.class)) {
            // read the certificates
            Collection<X509Certificate> certCollection = CertReader.readCerts(
                    param.getFileName(), false, certProvider);

            importReply(param, certCollection);
        } else if (!contains) { // import a trusted certificate
            // read the certificate
            Collection<X509Certificate> trustedCert = CertReader.readCerts(
                    param.getFileName(), true, certProvider);

            importTrusted(param, trustedCert.iterator().next());
        } else {// if the existing entry is not a private key entry
            throw new KeytoolException(
                    "Failed to import the certificate. \nAlias <" + alias
                            + "> already exists and is not a private key entry");
        }
    }

    /**
     * Imports a Certificate Signing Request (CSR) reply - single X.509
     * certificate or PKCS#7 formatted certificate chain, consisting of X.509
     * certificates.
     * 
     * @param param
     * @throws FileNotFoundException
     * @throws CertificateException
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws CertPathBuilderException
     * @throws InvalidAlgorithmParameterException
     * @throws KeytoolException
     * @throws NoSuchProviderException
     */
    private static void importReply(KeytoolParameters param,
            Collection<X509Certificate> certCollection)
            throws FileNotFoundException, CertificateException, IOException,
            KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException, CertPathBuilderException,
            KeytoolException, NoSuchProviderException {
        if (certCollection.size() == 1) {
            importSingleX509Reply(param, certCollection.iterator().next());
        } else if (certCollection.size() > 0) {
            importCertChain(param, certCollection);
        }
    }

    /**
     * Imports a single X.509 certificate Certificate Signing Request (CSR)
     * reply.
     * 
     * @param param
     * @param newCert
     * @throws CertificateException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws CertPathBuilderException
     * @throws KeytoolException
     * @throws NoSuchProviderException
     */

    private static void importSingleX509Reply(KeytoolParameters param,
            X509Certificate newCert) throws CertificateException,
            FileNotFoundException, IOException, KeyStoreException,
            NoSuchAlgorithmException, UnrecoverableKeyException,
            CertPathBuilderException, KeytoolException, NoSuchProviderException {

        String alias = param.getAlias();
        KeyStore keyStore = param.getKeyStore();

        // the certificate to be replaced with certificate reply.
        X509Certificate csrCert = (X509Certificate) keyStore
                .getCertificate(alias);
        // quit if public keys of the imported certificate and csrCert don't
        // match
        PublicKey publicKey = csrCert.getPublicKey();
        if (!Arrays.equals(publicKey.getEncoded(), newCert.getPublicKey()
                .getEncoded())) {
            throw new KeytoolException("Public keys don't match.");
        }
        // quit if the certificates are identical
        if (newCert.equals(csrCert)) {
            throw new KeytoolException("Certificate reply is identical to the "
                    + "certificate in keystore");
        }

        // save the private key to put it in a newly created entry
        PrivateKey privateKey;
        try {
            privateKey = (PrivateKey) keyStore
                    .getKey(alias, param.getKeyPass());
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException(
                    "Cannot find the algorithm to recover the key. ", e);
        }

        X509Certificate[] newChain = CertChainVerifier.buildFullCertPath(param,
                newCert);

        // changing the certificate chain //
        // remove the entry with old certificate chain
        keyStore.deleteEntry(alias);

        // set the new certificate chain
        keyStore.setKeyEntry(alias, privateKey, param.getKeyPass(), newChain);
        param.setNeedSaveKS(true);
        System.out
                .println("Certificate reply is successfully installed into the keystore.");
    }

    /**
     * Imports an X.509 certificate into a trusted certificate entry.
     * 
     * @param param
     * @throws KeytoolException
     * @throws IOException
     * @throws CertPathBuilderException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     */
    private static void importTrusted(KeytoolParameters param,
            X509Certificate newCert) throws NoSuchAlgorithmException,
            CertificateException, CertPathBuilderException, IOException,
            KeytoolException, KeyStoreException, NoSuchProviderException {
        String alias = param.getAlias();
        KeyStore keyStore = param.getKeyStore();

        // should the certificate be added to the store or not
        boolean addIt = false;

        // if "-noprompt" option has been specified, don't make
        // additional checks.
        if (param.isNoPrompt()) {
            addIt = true;
        } else {
            // search for an equal certificate in the keystore
            String equalCertName = keyStore.getCertificateAlias(newCert);

            if (equalCertName != null) {
                // if an equal certificate exists in the store
                System.out.println("The certificate already exists in the "
                        + "keystore under alias <" + equalCertName + ">");
                // ask if a duplicating certificate should be added to the
                // store
                addIt = ArgumentsParser.getConfirmation(
                        "Do you still want to add it? [no] ", false);
            } else {
                try {
                    if (CertChainVerifier.buildCertPath(param, newCert) != null) {
                        addIt = true;
                    }
                } catch (Exception e) {
                    // if the certificate chain cannot be built
                    // print it and ask if it should be trusted or not.
                    String mdProvider = (param.getMdProvider() != null) ? param
                            .getMdProvider() : param.getProvider();
                    KeyStoreCertPrinter.printX509CertDetailed(newCert,
                            mdProvider);
                    addIt = ArgumentsParser.getConfirmation(
                            "Trust this certificate? [no] ", false);
                }
            }
        }
        if (addIt) {
            keyStore.setCertificateEntry(alias, newCert);
            param.setNeedSaveKS(true);
            System.out.println("The certificate is added to the keystore\n");
        } else {
            System.out
                    .println("The certificate is not added to the keystore\n");
        }
    }

    /**
     * Imports a PKCS#7-formatted certificate chain as a CSR reply. The
     * certificate chain is firstly ordered. After that top-level certificate of
     * the chain is checked to be a trusted one. If it is not a trusted
     * certificate, the user is asked if the certificate should be trusted. If
     * the certificate is considered to be trusted, old certificate chain,
     * associated with param.getAlias() is replaced with the new one.
     * Certificates can be in DER or PEM format.
     * 
     * @param param
     * @param newCerts
     * @throws Exception
     * @throws NoSuchAlgorithmException
     * @throws KeytoolException
     * @throws KeyStoreException
     * @throws IOException
     * @throws UnrecoverableKeyException
     * @throws NoSuchProviderException
     * @throws CertificateException 
     */
    private static void importCertChain(KeytoolParameters param,
            Collection<X509Certificate> newCerts)
            throws NoSuchAlgorithmException, KeytoolException,
            KeyStoreException, IOException, UnrecoverableKeyException,
            NoSuchProviderException, CertificateException {

        String alias = param.getAlias();
        KeyStore keyStore = param.getKeyStore();
        // get the public key of the certificate, associated with alias,
        // to import reply to.
        PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();
        // order the certificate chain
        X509Certificate[] orderedChain = CertChainVerifier.orderChain(newCerts,
                publicKey);
        // get the top-level certificate in the chain
        X509Certificate lastInChain = orderedChain[orderedChain.length - 1];

        // should the chain be added to the keystore or not
        boolean needAddChain;
        // try to build a chain of trust beginning with the top certificate
        needAddChain = CertChainVerifier.isTrusted(param, lastInChain);

        if (!needAddChain) {
            // If couldn't build full cert path for some reason,
            // ask user if the certificate should be trusted.
            System.out.println("Top-level certificate in the reply chain:\n");
            String mdProvider = (param.getMdProvider() != null) ? param
                    .getMdProvider() : param.getProvider();
            KeyStoreCertPrinter.printX509CertDetailed(lastInChain, mdProvider);
            needAddChain = ArgumentsParser
                    .getConfirmation(
                            "... is not trusted.\n"
                                    + "Do you still want to install the reply? [no]:  ",
                            false);

            if (!needAddChain) {
                System.out.println("The certificate reply is " + "not "
                        + "installed into the keystore.");
                return;
            }
        }

        // replacing old certificate chain with the new one
        char[] keyPassword = param.getKeyPass();
        PrivateKey privateKey = (PrivateKey) keyStore
                .getKey(alias, keyPassword);
        keyStore.deleteEntry(alias);
        keyStore.setKeyEntry(alias, privateKey, keyPassword, orderedChain);
        param.setNeedSaveKS(true);
        System.out.println("The certificate reply is " + "successfully "
                + "installed into the keystore.");
    }

}
