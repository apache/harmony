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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import org.apache.harmony.tools.toolutils.KeyStoreLoaderSaver;

/**
 * The class encapsulates paramaters for Keytool most of which are usually given
 * in command line.
 */
public class KeytoolParameters {
    /**
     * Default location of the keystore. Used when the value is not supplied by
     * the user.
     */
    public static final String defaultKeystorePath = System
            .getProperty("user.home")
            + File.separator + ".keystore";

    // Default location of cacerts file
    private static final String defaultCacertsPath = System.getProperty("java.home")
            + File.separator + "lib" + File.separator + "security"
            + File.separator + "cacerts";
    
    // Default password for cacerts keystore
    private static final char[] defaultCacertsPass = { 'c', 'h', 'a', 'n', 'g',
            'e', 'i', 't' };

    // the keystore to work with
    private KeyStore keyStore;

    // shows should the keystore be saved or not;
    private boolean needSaveKS;

    // path to the keystore or certstore depending on a command.
    private String storePath;

    // type of the store. Default type is set in java.security file.
    private String storeType = KeyStore.getDefaultType();

    // the name of the provider to use if specific provider is not given
    private String provider;

    // the name of the provider to work with certificates  
    private String certProvider;
    
    // the name of the provider to work with keys
    private String keyProvider;
    
    // the name of the provider to work with message digests
    private String mdProvider;
    
    // the name of the provider to work with signatures
    private String sigProvider;
    
    // the name of the provider to work with keystore
    private String ksProvider;
    
    // the name of the provider to work with keystore to convert to
    private String convKsProvider;
    
    // alias to access an entry in keystore
    private String alias;

    // algorithm name to get instance of KeyPairGenerator, KeyFactory, etc.
    private String keyAlg;

    // digital signature algorithm
    private String sigAlg;

    // X.500 Distinguished Name to generate a certificate
    private String dName;

    // name of the file to import/export certificates
    private String fileName;

    // alias to access the issuer's certificate (certificate which a newly
    // generated certificate can be signed with)
    private String issuerAlias;

    // file with CRLs
    private String crlFile;

    // used in keyclone. Shows the destination alias to copy key pair to
    private String destAlias;

    // password to access the store
    private char[] storePass;

    // password to access the key entry
    private char[] keyPass;

    // new password to change the old one to
    private char[] newPasswd;

    // password to access the issuer's certificate (see issuerAlias)
    private char [] issuerPass;

    // size of the key to generate
    private int keySize = 1024;

    // validity period of the generated certificate in days from the current
    // moment
    private int validity = 90;

    // X.509 protocol version to use when generating a certificate
    private int X509version = 3;

    // certificate serial number
    private int certSerialNr;

    // should the unspecified parameters be prompted for or not
    private boolean noPrompt;

    // used in import. Should the certificates from cacerts file be considered
    // for the chain of trust or not
    private boolean trustCACerts;

    // should the certificate be printed or exported in printable or binary
    // format
    private boolean rfc;

    // should the keytool print the detailed information on the operation or not
    private boolean verbose;

    // should a secret key or a key pair be generated
    private boolean isSecretKey;
    
    // should the generated certificate ba a CA certificate or not
    private boolean isCA;

    // path to the keystore to convert the current keystore to
    private String convertedKeyStorePath;    
    
    // type of the keystore to convert the current keystore to
    private String convertedKeyStoreType;    
    
    // password to the keystore to convert the current keystore to
    private char [] convertedKeyStorePass;
    
    // should the key entries be converted or not
    private boolean convertKeyEntries;
    
    // location of cacerts file
    private String cacertsPath;
    
    // password for cacerts keystore
    private char [] cacertsPass;
    
    // cacerts keystore containing the certificates from root
    // certificate authorities (usually self-signed)
    private KeyStore cacerts;
    
    // topic to print help on
    private String helpTopic;
    
    // command to perform
    private Command command = Command.HELP;

    /**
     * The method sets the fields to default values. If there is not a default
     * value the field is set to null.
     */
    void setDefault() {
        keyStore = null;
        needSaveKS = false;
        storePath = null;
        storeType = KeyStore.getDefaultType();
        provider = null;
        certProvider = null;
        keyProvider = null;
        mdProvider = null;
        sigProvider = null;
        ksProvider = null;
        convKsProvider = null;
        helpTopic = null;
        storePass = null;
        alias = null;
        keyAlg = null;
        keySize = 1024;
        sigAlg = null;
        dName = null;
        keyPass = null;
        newPasswd = null;
        validity = 90;
        fileName = null;
        noPrompt = false;
        trustCACerts = false;
        rfc = false;
        verbose = false;
        isSecretKey = false;
        issuerAlias = null;
        issuerPass = null;
        X509version = 3;
        certSerialNr = 0;
        isCA = false;
        convertedKeyStorePath = null;
        convertedKeyStoreType = null;
        convertedKeyStorePass = null;
        convertKeyEntries = false;
        cacertsPath = null;
        cacertsPass = null;
        cacerts = null;
        crlFile = null;
        command = Command.HELP;
    }

    // getters and setters down here.
    /**
     * @return Returns the keystore to work with.
     * @throws IOException 
     * @throws NoSuchProviderException 
     * @throws KeyStoreException 
     * @throws FileNotFoundException 
     * @throws CertificateException 
     * @throws NoSuchAlgorithmException 
     */
    KeyStore getKeyStore() throws NoSuchAlgorithmException,
            CertificateException, FileNotFoundException, KeyStoreException,
            NoSuchProviderException, IOException {
        if (keyStore == null){
            KeytoolKSLoaderSaver.loadStore(this);
        }
        return keyStore;
    }

    /**
     * @param keyStore
     *            The KeyStore to set as keystore worked with.
     */
    void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * @return Returns true if keystore is to be saved, false - otherwise.
     */
    public boolean isNeedSaveKS() {
        return needSaveKS;
    }

    /**
     * @param needSaveKS -
     *            if true keystore is to be saved, if false - it is not.
     */
    public void setNeedSaveKS(boolean needSaveKS) {
        this.needSaveKS = needSaveKS;
    }

    /**
     * @return Returns the alias used to access the keystore entry.
     */
    String getAlias() {
        return alias;
    }

    /**
     * @param alias
     *            The alias to access the keystore entry.
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * @return Returns the alias to access the issuer's certificate (certificate
     *         which a newly generated certificate can be signed with)
     */
    String getIssuerAlias() {
        return issuerAlias;
    }

    /**
     * @param issuerAlias
     *            The alias to access the issuer's certificate (certificate
     *            which a newly generated certificate can be signed with)
     */
    public void setIssuerAlias(String issuerAlias) {
        this.issuerAlias = issuerAlias;
    }

    /**
     * @return Returns path to a file with CRLs.
     */
    String getCrlFile() {
        return crlFile;
    }

    /**
     * @param crlFile
     *            path to a file with CRLs.
     */
    public void setCrlFile(String crlStore) {
        this.crlFile = crlStore;
    }

    /**
     * @return Returns the destination alias to copy key pair to
     */
    String getDestAlias() {
        return destAlias;
    }

    /**
     * @param destAlias
     *            The destination alias to copy key pair to
     */
    public void setDestAlias(String destAlias) {
        this.destAlias = destAlias;
    }

    /**
     * @return Returns the certificate serial number
     */
    int getCertSerialNr() {
        return certSerialNr;
    }

    /**
     * @param certSerialNr
     *            The certificate serial number
     */
    public void setCertSerialNr(int certSerialNr) {
        this.certSerialNr = certSerialNr;
    }

    /**
     * @return Returns the command to perform
     */
    Command getCommand() {
        return command;
    }

    /**
     * @param command
     *            The command to perform
     */
    public void setCommand(Command command) {
        this.command = command;
    }

    /**
     * @return Returns the X.500 Distinguished Name to generate a certificate
     */
    String getDName() {
        return dName;
    }

    /**
     * @param name
     *            The X.500 Distinguished Name to generate a certificate
     */
    public void setDName(String name) {
        dName = name;
    }

    /**
     * @return Returns the name of the file to import/export certificates
     */
    String getFileName() {
        return fileName;
    }

    /**
     * @param fileName
     *            The name of the file to import/export certificates
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return Returns the algorithm name to get instance of KeyPairGenerator,
     *         KeyFactory, etc.
     */
    String getKeyAlg() {
        return keyAlg;
    }

    /**
     * @param keyAlg
     *            algorithm name to get instance of KeyPairGenerator,
     *            KeyFactory, etc.
     */
    public void setKeyAlg(String keyAlg) {
        this.keyAlg = keyAlg;
    }

    /**
     * @return Returns the password to access the key entry
     */
    char[] getKeyPass() {
        return keyPass;
    }

    /**
     * @param keyPass
     *            password to access the key entry
     */
    public void setKeyPass(char[] keyPass) {
        this.keyPass = keyPass;
    }

    /**
     * @return Returns the size of the key to generate
     */
    int getKeySize() {
        return keySize;
    }

    /**
     * @param keySize
     *            The size of the key to generate
     */
    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    /**
     * @return Returns the new password to change the old one to
     */
    char[] getNewPasswd() {
        return newPasswd;
    }

    /**
     * @param newPasswd
     *            The new password to change the old one to
     */
    public void setNewPasswd(char[] newPasswd) {
        this.newPasswd = newPasswd;
    }

    /**
     * @return password to access the issuer's certificate (certificate which a
     *         newly generated certificate can be signed with)
     */
    char[] getIssuerPass() {
        return issuerPass;
    }

    /**
     * @param issuerPass
     *            password to access the issuer's certificate (certificate which
     *            a newly generated certificate can be signed with)
     */
    public void setIssuerPass(char[] issuerPass) {
        this.issuerPass = issuerPass;
    }

    /**
     * @return Returns true if unspecified parameters should be prompted for,
     *         false - otherwise
     */
    boolean isNoPrompt() {
        return noPrompt;
    }

    /**
     * @param noPrompt
     *            Set true if unspecified parameters should be prompted for,
     *            false if not
     */
    public void setNoPrompt(boolean noPrompt) {
        this.noPrompt = noPrompt;
    }

    /**
     * @return Returns the name of the provider to use if specific provider is
     *         not given
     */
    String getProvider() {
        return provider;
    }

    /**
     * @param provider
     *            the name of the provider to use if specific provider is not
     *            given
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * @return the name of the provider to work with certificates
     */
    String getCertProvider() {
        return certProvider;
    }

    /**
     * @param certProvider
     *            the name of the provider to work with certificates
     */
    public void setCertProvider(String certProvider) {
        this.certProvider = certProvider;
    }

    /**
     * @return the name of the provider to work with keystore to convert the
     *         main keystore to
     */
    String getConvKsProvider() {
        return convKsProvider;
    }

    /**
     * @param convKsProvider
     *            the name of the provider to work with keystore to convert the
     *            main keystore to
     */
    public void setConvKsProvider(String convKsProvider) {
        this.convKsProvider = convKsProvider;
    }

    /**
     * @return the name of the provider to work with keys
     */
    String getKeyProvider() {
        return keyProvider;
    }

    /**
     * @param keyProvider
     *            the name of the provider to work with keys
     */
    public void setKeyProvider(String keyProvider) {
        this.keyProvider = keyProvider;
    }

    /**
     * @return the name of the provider to work with keystore
     */
    String getKsProvider() {
        return ksProvider;
    }

    /**
     * @param ksProvider
     *            the name of the provider to work with keystore
     */
    public void setKsProvider(String ksProvider) {
        this.ksProvider = ksProvider;
    }

    /**
     * @return the name of the provider to work with message digests
     */
    String getMdProvider() {
        return mdProvider;
    }

    /**
     * @param mdProvider
     *            the name of the provider to work with message digests
     */
    public void setMdProvider(String mdProvider) {
        this.mdProvider = mdProvider;
    }

    /**
     * @return the name of the provider to work with signatures
     */
    String getSigProvider() {
        return sigProvider;
    }

    /**
     * @param sigProvider
     *            the name of the provider to work with signatures
     */
    public void setSigProvider(String sigProvider) {
        this.sigProvider = sigProvider;
    }

    /**
     * @return Returns true if the certificate should be printed or exported in
     *         printable format, false - if in binary format
     */
    boolean isRfc() {
        return rfc;
    }

    /**
     * @param rfc
     *            set true if the certificate should be printed or exported in
     *            printable format, false - if in binary format
     */
    public void setRfc(boolean rfc) {
        this.rfc = rfc;
    }

    /**
     * @return Returns true if a secret key should be generated, false - if a
     *         key pair.
     */
    boolean isSecretKey() {
        return isSecretKey;
    }

    /**
     * @param isSecretKey
     *            set true if a secret key should be generated, false - if a key
     *            pair.
     */
    public void setSecretKey(boolean secretKey) {
        this.isSecretKey = secretKey;
    }

    /**
     * @return true if the generated certificate should be a CA certificate,
     *         false - otherwise
     */
    boolean isCA() {
        return isCA;
    }

    /**
     * @param isCA
     *            set true if the generated certificate should be a CA
     *            certificate, false - otherwise
     */
    public void setCA(boolean isCA) {
        this.isCA = isCA;
    }

    /**
     * @return Returns the digital signature algorithm
     */
    String getSigAlg() {
        return sigAlg;
    }

    /**
     * @param sigAlg
     *            The digital signature algorithm to set.
     */
    public void setSigAlg(String sigAlg) {
        this.sigAlg = sigAlg;
    }

    /**
     * @return Returns the password to access the store
     */
    char[] getStorePass() {
        return storePass;
    }

    /**
     * @param storePass
     *            The password to access the store
     */
    public void setStorePass(char[] storePass) {
        this.storePass = storePass;
    }

    /**
     * @return Returns the type of the store
     */
    String getStoreType() {
        return storeType;
    }

    /**
     * @param storeType
     *            The type of the store to set.
     */
    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    /**
     * @return Returns true if the certificates from cacerts file should be
     *         considered for the chain of trust, false - if not
     */
    boolean isTrustCACerts() {
        return trustCACerts;
    }

    /**
     * @param trustCACerts
     *            set true if the certificates from cacerts file should be
     *            considered for the chain of trust, false - if not.
     */
    public void setTrustCACerts(boolean trustCACerts) {
        this.trustCACerts = trustCACerts;
    }

    /**
     * @return Returns the validity period of the generated certificate in days
     *         from the current moment
     */
    int getValidity() {
        return validity;
    }

    /**
     * @param validity
     *            The validity period of the generated certificate in days from
     *            the current moment
     */
    public void setValidity(int validity) {
        this.validity = validity;
    }

    /**
     * @return Returns true if the keytool should print the detailed information
     *         on the operation, false - if not
     */
    boolean isVerbose() {
        return verbose;
    }

    /**
     * @param verbose
     *            set true if the keytool should print the detailed information
     *            on the operation, false - if not
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * @return Returns the X.509 protocol version to use when generating a
     *         certificate
     */
    int getX509version() {
        return X509version;
    }

    /**
     * @param x509version
     *            The X.509 protocol version to use when generating a
     *            certificate
     */
    public void setX509version(int x509version) {
        X509version = x509version;
    }

    /**
     * @return Returns path to the keystore or certstore depending on a command.
     */
    String getStorePath() {
        return storePath;
    }

    /**
     * @param storePath
     *            The path to the keystore or certstore (depending on a
     *            command).
     */
    public void setStorePath(String storePath) {
        this.storePath = storePath;
    }

    /**
     * @return password for the keystore to convert the current keystore to
     */
    char [] getConvertedKeyStorePass() {
        return convertedKeyStorePass;
    }

    /**
     * @param convertedKeyStorePass
     *            password for the keystore to convert the current keystore to
     */
    public void setConvertedKeyStorePass(char [] convertedKeyStorePass) {
        this.convertedKeyStorePass = convertedKeyStorePass;
    }

    /**
     * @return path to the keystore to convert the current keystore to
     */
    String getConvertedKeyStorePath() {
        return convertedKeyStorePath;
    }

    /**
     * @param convertedKeyStorePath
     *            path to the keystore to convert the current keystore to
     */
    public void setConvertedKeyStorePath(String convertedKeyStorePath) {
        this.convertedKeyStorePath = convertedKeyStorePath;
    }

    /**
     * @return type of the keystore to convert the current keystore to
     */
    String getConvertedKeyStoreType() {
        return convertedKeyStoreType;
    }

    /**
     * @param convertedKeyStoreType
     *            type of the keystore to convert the current keystore to
     */
    public void setConvertedKeyStoreType(String convertedKeyStoreType) {
        this.convertedKeyStoreType = convertedKeyStoreType;
    }

    /**
     * @return true if key entries should be converted, false - if not
     */
    boolean isConvertKeyEntries() {
        return convertKeyEntries;
    }

    /**
     * @param convertKeyEnties
     *            set true if key entries should be converted, false - if not
     */
    public void setConvertKeyEntries(boolean convertKeyEnties) {
        this.convertKeyEntries = convertKeyEnties;
    }

    /**
     * @return Returns the location of cacerts file, containing the certificates
     *         from root certificate authorities (usually self-signed).
     */
    String getCacertsPath() {
        if (cacertsPath != null) {
            return cacertsPath;
        } else {
            return defaultCacertsPath;
        }
    }

    /**
     * @param cacertsPath
     *            the location of cacerts file, containing the certificates from
     *            root certificate authorities (usually self-signed).
     */
    public void setCacertsPath(String cacertsPath) {
        this.cacertsPath = cacertsPath;
    }

    /**
     * @return password for cacerts keystore
     */
    char[] getCacertsPass() {
        if (cacertsPass != null) {
            return cacertsPass;
        } else {
            return defaultCacertsPass;
        }
    }

    /**
     * @param cacertsPass
     *            password for cacerts keystore
     */
    public void setCacertsPass(char[] cacertsPass) {
        this.cacertsPass = cacertsPass;
    }

    /**
     * @return cacerts keystore containing the certificates from root
     *         certificate authorities (usually self-signed)
     * @throws IOException 
     * @throws NoSuchProviderException 
     * @throws CertificateException 
     * @throws NoSuchAlgorithmException 
     * @throws KeyStoreException 
     * @throws FileNotFoundException 
     */
    KeyStore getCacerts() throws FileNotFoundException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            NoSuchProviderException, IOException {
        if (cacerts == null) {
            String keyStoreProv = (ksProvider != null) ? ksProvider : provider;
            cacerts = KeyStoreLoaderSaver.loadStore(getCacertsPath(),
                    storeType, getCacertsPass(), keyStoreProv);
        }
        return cacerts;
    }

    /**
     * @param cacerts
     *            keystore containing the certificates from root certificate
     *            authorities (usually self-signed)
     */
    void setCacerts(KeyStore cacerts) {
        this.cacerts = cacerts;
    }

    /**
     * @return topic to print help on
     */
    String getHelpTopic() {
        return helpTopic;
}
    /**
     * @param helpTopic
     *            topic to print help on
     */
    public void setHelpTopic(String helpTopic) {
        this.helpTopic = helpTopic;
    }
}
