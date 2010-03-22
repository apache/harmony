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
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Random;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.harmony.security.provider.cert.X509CertImpl;
import org.apache.harmony.security.utils.AlgNameMapper;
import org.apache.harmony.security.x501.Name;
import org.apache.harmony.security.x509.AlgorithmIdentifier;
import org.apache.harmony.security.x509.BasicConstraints;
import org.apache.harmony.security.x509.Extension;
import org.apache.harmony.security.x509.Extensions;
import org.apache.harmony.security.x509.SubjectPublicKeyInfo;
import org.apache.harmony.security.x509.TBSCertificate;
import org.apache.harmony.security.x509.Validity;

/**
 * Class for generating keys and key pairs, wrapping them into self-signed X.509
 * certificates.
 */
public class KeyCertGenerator {
    
    /**
     * Generates a key pair or a secret key. Key pair is composed of a private
     * and a public key. Method wraps the public key into a self-signed X.509
     * (v1, v2, v3) certificate and puts the certificate into a single-element
     * certificate chain or signs the certificate with private key from another
     * key entry and adds its chain to the newly generated certificate . After
     * that the method adds to the keystore a new entry containing the generated
     * private key and the chain. If a secret key is generated it is put into a
     * secret key entry, with null certificate chain.
     * 
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeytoolException
     * @throws IOException
     * @throws SignatureException
     * @throws NoSuchProviderException
     * @throws InvalidKeyException
     * @throws UnrecoverableKeyException
     * @throws CertificateException
     */
    static void genKey(KeytoolParameters param)
            throws NoSuchAlgorithmException, KeyStoreException,
            KeytoolException, InvalidKeyException, NoSuchProviderException,
            SignatureException, IOException, UnrecoverableKeyException,
            CertificateException {

        if (param.isSecretKey()) {
            generateSecretKey(param);
        } else {
            generatePrivateKey(param);
        }
    }

    // Generates a key pair composed of a private and a public key, wraps the
    // public key into a self-signed X.509 (v1, v2, v3) certificate, puts the
    // certificate into a single-element certificate chain and adds to the
    // keystore a new entry containing the private key and the chain.
    private static void generatePrivateKey(KeytoolParameters param)
            throws NoSuchAlgorithmException, KeyStoreException,
            NoSuchProviderException, InvalidKeyException, SignatureException,
            IOException, KeytoolException, UnrecoverableKeyException,
            CertificateException {

        KeyStore keyStore = param.getKeyStore();

        PrivateKey issuerPrivateKey = null;
        Certificate[] issuerCertChain = null;
        String issuerDN = null;
        // if the generated certificate shouldn't be self-signed
        // but should be signed with a chain.
        boolean selfSigned = (param.getIssuerAlias() == null);
        if (!selfSigned) {
            String issuerAlias = param.getIssuerAlias();
            if (!keyStore.containsAlias(issuerAlias)) {
                throw new KeytoolException("Certificate issuer alias <"
                        + issuerAlias + "> does not exist.");
            }

            if (!keyStore.entryInstanceOf(issuerAlias,
                    KeyStore.PrivateKeyEntry.class)) {
                throw new KeytoolException("Issuer alias <" + issuerAlias
                        + "> is not a private key entry. ");
            }
            issuerCertChain = keyStore.getCertificateChain(issuerAlias);
            issuerPrivateKey = (PrivateKey) keyStore.getKey(issuerAlias, param
                    .getIssuerPass());
            issuerDN = ((X509Certificate) issuerCertChain[0])
                    .getSubjectX500Principal().getName();
        }

        KeyPairGenerator kpg = null;
        String keyAlg = param.getKeyAlg();
        // key algorithm is DSA by default
        if (keyAlg == null) {
            keyAlg = "DSA";
        }

        String sigAlgName = null;

        // if signature algorithm is not set, use a default
        if (param.getSigAlg() != null) {
            sigAlgName = param.getSigAlg();
        } else if (selfSigned) {
            if (keyAlg.equalsIgnoreCase("DSA")) {
                sigAlgName = "SHA1withDSA";
            } else if (keyAlg.equalsIgnoreCase("RSA")) {
                sigAlgName = "MD5withRSA";
            }else {
                sigAlgName = keyAlg;
            }
        } else {
            String issuerKeyAlg = issuerPrivateKey.getAlgorithm();
            if (issuerKeyAlg.equalsIgnoreCase("DSA")) {
                sigAlgName = "SHA1withDSA";
            } else if (issuerKeyAlg.equalsIgnoreCase("RSA")) {
                sigAlgName = "MD5withRSA";
            } else {
                sigAlgName = issuerKeyAlg;
            }
        }
        // set the certificate validity period
        // 90 days by default
        long validity = (param.getValidity() != 0) ? param.getValidity() : 90;

        // set the X.509 version to use with the certificate
        int version = (param.getX509version() != 0) ?
        // TBSCertificate needs 0, 1 or 2 as version in constructor (not 1,2,3);
                // X.509 v.3 certificates by default
                param.getX509version() - 1 : 2;

        // set certificate serial number
        BigInteger serialNr;
        if (param.getCertSerialNr() != 0) {
            serialNr = BigInteger.valueOf(param.getCertSerialNr());
        } else {
            int randomInt = new Random().nextInt();
            if (randomInt < 0) {
                randomInt = -randomInt;
            }
            serialNr = BigInteger.valueOf(randomInt);
        }

        int keySize = param.getKeySize();
        if (param.isVerbose()) {
            StringBuffer strBuf = new StringBuffer("Generating a " + keyAlg
                    + " key pair, key length " + keySize + " bit \nand a ");
            if (selfSigned) {
                strBuf.append("self-signed ");
            }
            strBuf.append("certificate (signature algorithm is " + sigAlgName
                    + ")\n for " + param.getDName());
            System.out.println(strBuf);
        }

        // generate a pair of keys
        String provider = param.getProvider();
        String keyProvider = (param.getKeyProvider() != null) ? param
                .getKeyProvider() : provider;
        try {
            if (keyProvider == null) {
                kpg = KeyPairGenerator.getInstance(keyAlg);
            } else {
                kpg = KeyPairGenerator.getInstance(keyAlg, keyProvider);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException("The algorithm " + keyAlg
                    + " is not available in current environment.");
        } catch (NoSuchProviderException e) {
            throw (NoSuchProviderException) new NoSuchProviderException(
                    "The provider " + keyProvider
                            + " is not found in the environment.").initCause(e);
        }
        // initialize the KeyPairGenerator with the key size
        kpg.initialize(keySize);

        KeyPair keyPair = kpg.genKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();

        String subjectDN = param.getDName();
        String sigProvider = (param.getSigProvider() != null) ? param
                .getSigProvider() : provider;

        if (selfSigned) {
            // generate the certificate
            X509CertImpl x509cert = genX509CertImpl(sigAlgName, version,
                    serialNr, subjectDN, subjectDN, validity, keyPair
                            .getPublic(), privateKey, sigProvider,
                    param.isCA());

            // put the key pair with the newly created cert into the keystore
            keyStore.setKeyEntry(param.getAlias(), privateKey, param
                    .getKeyPass(), new X509Certificate[] { x509cert });

        } else {
            // generate the certificate
            X509CertImpl x509cert = genX509CertImpl(sigAlgName, version,
                    serialNr, subjectDN, issuerDN, validity, keyPair
                            .getPublic(), issuerPrivateKey,
                    sigProvider, param.isCA());

            // construct the certificate chain
            int issuerChainLength = issuerCertChain.length;
            X509Certificate[] certChain = new X509Certificate[issuerChainLength + 1];
            certChain[0] = x509cert;
            System.arraycopy(issuerCertChain, 0, certChain, 1,
                    issuerChainLength);

            // put the key pair with the newly created cert into the keystore
            keyStore.setKeyEntry(param.getAlias(), privateKey, param
                    .getKeyPass(), certChain);

        }

        param.setNeedSaveKS(true);
    }

    /**
     * Generates an X.509 (v1, v2, v3) self-signed certificate using a key pair
     * associated with alias defined in param. If X.500 Distinguished Name is
     * supplied in param it is used as both subject and issuer of the
     * certificate. Otherwise the distinguished name associated with alias is
     * used. Signature algorithm, validity period and certificate serial number
     * are taken from param if defined there or from the keystore entry
     * identified by alias.
     * 
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws UnrecoverableKeyException
     * @throws IOException
     * @throws NoSuchProviderException
     * @throws SignatureException
     * @throws InvalidKeyException
     * @throws KeytoolException
     * @throws CertificateException 
     */
    static void selfCert(KeytoolParameters param)
            throws NoSuchAlgorithmException, KeyStoreException,
            UnrecoverableKeyException, InvalidKeyException, SignatureException,
            NoSuchProviderException, IOException, KeytoolException,
            CertificateException {

        String alias = param.getAlias();
        KeyStore keyStore = param.getKeyStore();

        if (!keyStore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
            throw new KeytoolException("Failed to generate a certificate. "
                    + "Entry <" + alias + "> is not a private key entry");
        }

        // get the keys and the certificate from the keystore
        PrivateKey privateKey;
        try {
            privateKey = (PrivateKey) keyStore
                    .getKey(alias, param.getKeyPass());
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException(
                    "Cannot find the algorithm to recover the key. ", e);
        }

        // get the certificate currently associated with the alias
        X509Certificate existing = (X509Certificate) keyStore
                .getCertificate(alias);

        // setting certificate attributes
        // signature algorithm name
        String sigAlgName = (param.getSigAlg() != null) ? param.getSigAlg()
                : existing.getSigAlgName();

        // X.500 distinguished name
        String distinguishedName = (param.getDName() != null) ? param
                .getDName() : existing.getSubjectDN().getName();

        // validity period. It is 90 days by default
        long validity = (param.getValidity() != 0) ? param.getValidity() : 90;

        // set the X.509 version to use with the certificate
        int version = (param.getX509version() != 0) ?
        // TBSCertificate needs 0, 1 or 2 as version in constructor (not 1,2,3);
                param.getX509version() - 1 : 2; // X.509 v.3 certificates by default
        

        // set certificate serial number
        int randomInt = new Random().nextInt();
        if (randomInt < 0) {
            randomInt = -randomInt;
        }
        BigInteger serialNr = (param.getCertSerialNr() != 0) ? BigInteger
                .valueOf(param.getCertSerialNr()) : BigInteger
                .valueOf(randomInt);

        // generate a new certificate
        String sigProvider = (param.getSigProvider() != null) ? param
                .getSigProvider() : param.getProvider();
        X509CertImpl x509cert = genX509CertImpl(sigAlgName, version, serialNr,
                distinguishedName, distinguishedName, validity, existing
                        .getPublicKey(), privateKey, sigProvider, param.isCA());

        if (param.isVerbose()) {
            System.out.println("New self-signed certificate: ");
            System.out.println("Version: v" + x509cert.getVersion());
            System.out.println("Owner: " + x509cert.getSubjectX500Principal());
            System.out.println("Issuer: " + x509cert.getIssuerX500Principal());
            System.out.println("Public key: " + x509cert.getPublicKey());
            System.out
                    .println("Signature algorithm: OID."
                            + x509cert.getSigAlgOID() + ", "
                            + x509cert.getSigAlgName());
            System.out
                    .println("Serial number: "
                            // String.format("%x", x509cert.getSerialNumber()));
                            // TODO: print with String.format(..) when the
                            // method is
                            // implemented, and remove Integer.toHexString(..).
                            + Integer.toHexString(x509cert.getSerialNumber()
                                    .intValue()));

            System.out.println("Validity: \n    From: "
                    + x509cert.getNotBefore() + "\n      To: "
                    + x509cert.getNotAfter());
        }

        // put the new certificate into the entry, associated with the alias
        keyStore.setKeyEntry(alias, privateKey, param.getKeyPass(),
                new X509Certificate[] { x509cert });
        param.setNeedSaveKS(true);
    }

    // Generates an X.509 certificate (instance of X509CertImpl class) based
    // on given paramaters.
    // 
    // @param sigAlgName
    // the name of the signature algorithm to use
    // @param version
    // version of X.509 protocol to use. May be one of 0 (v1), 1 (v2)
    // or 2 (v3)
    // @param serialNr
    // certificate serial number
    // @param strSubjectDN
    // X.500 Distinguished Name to use as subject
    // @param strIssuerDN
    // X.500 Distinguished Name to use as issuer
    // @param validity
    // certificate validity period in days after the current moment
    // @param publicKey
    // public key to wrap in certificate
    // @param privateKey
    // private key to sign the certificate
    // @param provider
    // provider name to use when generating a signature
    // @return X.509 certificate
    private static X509CertImpl genX509CertImpl(String sigAlgName, int version,
            BigInteger serialNr, String strSubjectDN, String strIssuerDN,
            long validity, PublicKey publicKey, PrivateKey privateKey,
            String provider, boolean isCA) throws InvalidKeyException, SignatureException,
            NoSuchAlgorithmException, IOException, NoSuchProviderException {

        String[] sigAlgNameAndOID = getAlgNameAndOID(sigAlgName);
        if (sigAlgNameAndOID[0] == null || sigAlgNameAndOID[1] == null) {
            throw new NoSuchAlgorithmException("The algorithm " + sigAlgName
                    + " is not found in the environment.");
        }
        sigAlgName = sigAlgNameAndOID[0];
        String sigAlgOID = sigAlgNameAndOID[1];

        AlgorithmIdentifier algId = new AlgorithmIdentifier(sigAlgOID);

        // generate a distinguished name using the string
        Name subjectDName = null;
        Name issuerDName = null;
        try {
            subjectDName = new Name(strSubjectDN);

            if (strSubjectDN.equals(strIssuerDN)) {
                issuerDName = subjectDName;
            } else {
                issuerDName = new Name(strIssuerDN);
            }
        } catch (IOException e) {
            throw (IOException) new IOException(
                    "Failed to generate a distinguished name. ").initCause(e);
        }

        // generate a SubjectPublicKeyInfo
        SubjectPublicKeyInfo subjectPublicKeyInfo = null;
        try {
            subjectPublicKeyInfo = (SubjectPublicKeyInfo) SubjectPublicKeyInfo.ASN1
                    .decode(publicKey.getEncoded());
        } catch (IOException e) {
            throw (IOException) new IOException(
                    "Failed to decode SubjectPublicKeyInfo. ").initCause(e);
        }
        
        Extensions extensions = null;
        
        if (version == 1 || version == 2) {
            // generate extensions
            extensions = new Extensions(Collections
                    .singletonList(new Extension("2.5.29.19", false,
                            new BasicConstraints(isCA, Integer.MAX_VALUE))));
        }       
        // generate the TBSCertificate to put it into the X.509 cert
        TBSCertificate tbsCertificate = new TBSCertificate(
        // version
                version,
                // serial number
                serialNr,
                // signature algorithm identifier
                algId,
                // issuer
                issuerDName,
                // validity
                new Validity(new Date(System.currentTimeMillis()), // notBefore
                        new Date(System.currentTimeMillis()
                        // 86400000 milliseconds in a day
                                + validity * 86400000)), // notAfter
                // subject
                subjectDName,
                // subjectPublicKeyInfo
                subjectPublicKeyInfo,
                // issuerUniqueID
                null,
                // subjectUniqueID
                null, 
                // basic constraints
                extensions);
        // get the TBSCertificate encoding
        byte[] tbsCertEncoding = tbsCertificate.getEncoded();

        // generate the signature
        Signature sig = null;
        try {
            sig = (provider == null) ? Signature.getInstance(sigAlgName)
                    : Signature.getInstance(sigAlgName, provider);
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException("The algorithm " + sigAlgName
                    + " is not found in the environment.", e);
        } catch (NoSuchProviderException e) {
            throw (NoSuchProviderException) new NoSuchProviderException(
                    "The provider " + provider
                            + " is not found in the environment.").initCause(e);
        }

        try {
            sig.initSign(privateKey);
        } catch (InvalidKeyException e) {
            throw new InvalidKeyException(
                    "The private key used to generate the signature is invalid.",
                    e);
        }

        byte[] signatureValue = null;
        try {
            sig.update(tbsCertEncoding, 0, tbsCertEncoding.length);
            signatureValue = sig.sign();
        } catch (SignatureException e) {
            throw new SignatureException("Failed to sign the certificate. ", e);
        }

        // actual X.509 certificate generation
        org.apache.harmony.security.x509.Certificate cert;
        cert = new org.apache.harmony.security.x509.Certificate(tbsCertificate,
                algId, signatureValue);
        return new X509CertImpl(cert);
    }

    // generates a secret key and puts it into a newly created secret key entry.
    private static void generateSecretKey(KeytoolParameters param)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            KeyStoreException, CertificateException, FileNotFoundException,
            IOException {

        String keyAlg = param.getKeyAlg();
        int keySize = param.getKeySize();

        if (keyAlg == null) {
            keyAlg = "DES";
        } else {
            keyAlg = getAlgNameAndOID(keyAlg)[0];
        }
        if (param.isVerbose()) {
            System.out.println("Generating a new secret key: ");
            System.out.println("Algorithm: " + keyAlg);
            System.out.println("Key size: " + keySize);
        }
        KeyGenerator keyGen;
        String keyProvider = (param.getKeyProvider() != null) ? param
                .getKeyProvider() : param.getProvider();
        try {

            if (keyProvider == null) {
                keyGen = KeyGenerator.getInstance(keyAlg);
            } else {
                keyGen = KeyGenerator.getInstance(keyAlg, keyProvider);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException("The algorithm " + keyAlg
                    + " is not found in the environment.", e);
        } catch (NoSuchProviderException e) {
            throw (NoSuchProviderException) new NoSuchProviderException(
                    "The provider " + keyProvider
                            + " is not found in the environment.").initCause(e);
        }

        keyGen.init(keySize);
        SecretKey key = keyGen.generateKey();

        KeyStore keyStore = param.getKeyStore();

        // keyStore.setKeyEntry(param.getAlias(), key, param.getKeyPass(),
        // null);

        keyStore.setEntry(param.getAlias(), new KeyStore.SecretKeyEntry(key),
                new KeyStore.PasswordProtection(param.getKeyPass()));

        param.setNeedSaveKS(true);
    }

    // Method gets algorithm name (it can be an algorithm alias, OID or standard
    // algorithm name) and returns String array, which has standard name as the
    // first element and OID as the second.
    // If algName is OID and the mapping is not found in providers available,
    // first element can be null.
    // If algName is a standard format (not an OID) and the mapping is not found
    // second element of the returned array can be null. If the algorithm name
    // itself is not found algName is returned as the first element.
    private static String[] getAlgNameAndOID(String algName)
            throws NoSuchAlgorithmException {
        String algOID = null;
        String standardName = null;
        if (AlgNameMapper.isOID(algName.toUpperCase())) {
            // if algName is OID, remove the leading "OID." and
            // copy it to algOID
            algOID = AlgNameMapper.normalize(algName.toUpperCase());
            // convert OID to a normal algorithm name.
            standardName = AlgNameMapper.map2AlgName(algOID);
        } else {
            // if algName is not an OID, convert it into a standard name and
            // get its OID and put to algOID
            standardName = AlgNameMapper.getStandardName(algName);
            if (standardName == null) {
                return new String[] { algName, null };
            }
            algOID = AlgNameMapper.map2OID(standardName);
        }
        return new String[] { standardName, algOID };
    }
}

