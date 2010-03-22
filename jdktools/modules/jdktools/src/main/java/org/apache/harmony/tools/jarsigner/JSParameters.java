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

package org.apache.harmony.tools.jarsigner;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.jar.JarFile;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.harmony.tools.keytool.KeytoolParameters;
import org.apache.harmony.tools.toolutils.KeyStoreLoaderSaver;

/**
 * The class encapsulates paramaters for jarsigner most of which are usually
 * given in command line.
 */
class JSParameters {
    /**
     * Default location of the keystore. Used when the value is not supplied by
     * the user.
     */
    public static final String defaultKeystorePath = System
            .getProperty("user.home")
            + File.separator + ".keystore";
   
    /**
     * The name of the logger for JarSigner.
     */
    public static final String loggerName = 
            "org.apache.harmony.tools.jarsigner.JarSignerLogger";

    // the keystore to work with
    private KeyStore keyStore;
    
    // JAR file to work with
    private JarFile jarFile;
    
    // JAR file URI or path 
    private String jarURIPath;

    // alias to access an entry in keystore
    private String alias;

    // should the jar be verified (if it is false, JAR is to be signed)
    private boolean isVerify;
    
    // URL of the keystore
    private String storeURI;

    // type of the store. Default type is set in java.security file.
    private String storeType = KeyStore.getDefaultType();

    // password to access the store
    private char[] storePass;

    // password to access the key entry
    private char[] keyPass;

    // file name to be used when generating .SF and .DSA files
    private String sigFileName;
    
    // file name to be used for signed JAR
    private String signedJARName;
    
    // if used with -verify and -verbose options, makes jarsigner print
    // certificate info
    private boolean isCerts;
    
    // should the program be "verbose" or not
    private boolean isVerbose;
    
    // should the .DSA file contain .SF file in it or not
    private boolean isInternalSF;
    
    // if set to true, .SF file will not contain hash of the whole manifest file
    private boolean isSectionsOnly;
    
    // class name of the provider to use if specific provider is not given
    private String provider;
    
    // name of the provider to use if specific provider is not given
    private String providerName;

    // class name of the provider to work with certificates  
    private String certProvider;

    // name of the provider to work with certificates  
    private String certProviderName;

    // class name of the provider to work with signatures
    private String sigProvider;

    // name of the provider to work with signatures
    private String sigProviderName;

    // class name of the provider to work with keystore
    private String ksProvider;

    // name of the provider to work with keystore
    private String ksProviderName;

    // class name of the provider to work with message digests
    private String mdProvider;

    // name of the provider to work with message digests
    private String mdProviderName;

    // timestamp authority URL
    private URI tsaURI;
    
    // the alias identifying the TSA's certificate
    private String tsaCertAlias;

    // alternative signer class name
    private String altSigner;
    
    // classpath to alternative signer package 
    private String altSignerPath; 
    
    // topic to print help on
    private String helpTopic;

    // true if signature file name is processed by FileNameGenerator
    // false if the name that the user has set is unchanged.
    private boolean isSFNameProcessed; 
    
    // algorithm of the key used to sign data 
    private String keyAlg;
    
    // algorithm of the signature used
    private String sigAlg;
    
    // should the JarSigner turn off logging or not
    private boolean isSilent;
    
    // proxy server address
    private String proxy;
    
    // proxy server port
    private int proxyPort;
    
    // proxy server type
    private Proxy.Type proxyType;
    
    // set the fields of the JSParameters object to default values
    void setDefault(){
        keyStore = null;
        jarFile = null;
        jarURIPath = null;        
        alias = null;
        storeURI = null;        
        storeType = KeyStore.getDefaultType();
        storePass = null;        
        keyPass = null;
        sigFileName = null;        
        signedJARName = null;
        isVerify = false;
        isCerts = false;
        isVerbose = false;
        isInternalSF = false;
        isSectionsOnly = false;
        provider = null;        
        providerName = null;
        certProvider = null;        
        certProviderName = null;        
        sigProvider = null;
        sigProviderName = null;        
        ksProvider = null;        
        ksProviderName = null;
        mdProvider = null;        
        mdProviderName = null;
        tsaURI = null;        
        tsaCertAlias = null;        
        altSigner = null;        
        altSignerPath = null;
        helpTopic = null;        
        isSFNameProcessed = false;
        keyAlg = null;
        sigAlg = null;
        isSilent = false;
        proxy = null;
        proxyPort = 8888;
        proxyType = Proxy.Type.HTTP;
    }
    
    // Getters and setters down here
    /**
     * @param alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * @param altSigner
     */
    public void setAltSigner(String altSigner) {
        this.altSigner = altSigner;
    }

    /**
     * @param altSignerPath
     */
    public void setAltSignerPath(String altSignerPath) {
        this.altSignerPath = altSignerPath;
    }

    /**
     * @param certProvider
     */
    public void setCertProvider(String certProvider) {
        this.certProvider = certProvider;
    }

    /**
     * @param certProviderName
     */
    public void setCertProviderName(String certProviderName) {
        this.certProviderName = certProviderName;
    }

    /**
     * @param helpTopic
     */
    public void setHelpTopic(String helpTopic) {
        this.helpTopic = helpTopic;
    }

    /**
     * @param isCerts
     */
    public void setCerts(boolean isCerts) {
        this.isCerts = isCerts;
    }

    /**
     * @param isInternalSF
     */
    public void setInternalSF(boolean isInternalSF) {
        this.isInternalSF = isInternalSF;
    }

    /**
     * @param isSectionsOnly
     */
    public void setSectionsOnly(boolean isSectionsOnly) {
        this.isSectionsOnly = isSectionsOnly;
    }

    /**
     * @param isVerbose
     */
    public void setVerbose(boolean isVerbose) {
        if (!isSilent) {
            Logger logger = Logger.getLogger(loggerName);
            Handler [] handlers = logger.getHandlers();
            for (Handler handler : handlers) {
                if (isVerbose) {
                    logger.setLevel(Level.FINE);
                    handler.setLevel(Level.FINE);
                } else {
                    logger.setLevel(Level.INFO);
                    handler.setLevel(Level.INFO);
                }
            }
        }
        this.isVerbose = isVerbose;
    }

    /**
     * @param isVerify
     */
    public void setVerify(boolean isVerify) {
        this.isVerify = isVerify;
    }

    /**
     * @param jarFile
     */
    public void setJarFile(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    /**
     * @param jarURIPath
     */
    public void setJarURIorPath(String jarURIPath) {
        this.jarURIPath = jarURIPath;
    }

    /**
     * @param keyAlg
     */
    void setKeyAlg(String keyAlg) {
        this.keyAlg = keyAlg;
    }

    /**
     * @param keyPass
     */
    public void setKeyPass(char[] keyPass) {
        this.keyPass = keyPass;
    }

    /**
     * @param keyStore
     */
    void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * @param ksProvider
     */
    public void setKsProvider(String ksProvider) {
        this.ksProvider = ksProvider;
    }

    /**
     * @param ksProviderName
     */
    public void setKsProviderName(String ksProviderName) {
        this.ksProviderName = ksProviderName;
    }

    /**
     * @param mdProvider
     */
    public void setMdProvider(String mdProvider) {
        this.mdProvider = mdProvider;
    }

    /**
     * @param mdProviderName
     */
    public void setMdProviderName(String mdProviderName) {
        this.mdProviderName = mdProviderName;
    }

    /**
     * @param provider
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * @param providerName
     */
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    /**
     * @param proxy 
     */
    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    /**
     * @param proxyPort 
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * @param proxyType 
     */
    public void setProxyType(Proxy.Type proxyType) {
        this.proxyType = proxyType;
    }

    /**
     * @param sigAlg
     */
    void setSigAlg(String sigAlg) {
        this.sigAlg = sigAlg;
    }

    /**
     * @param sigFileName
     */
    public void setSigFileName(String sigFileName) {
        this.sigFileName = sigFileName;
        isSFNameProcessed = false;
    }

    /**
     * @param signedJARName
     */
    public void setSignedJARName(String signedJARName) {
        this.signedJARName = signedJARName;
    }

    /**
     * @param sigProvider
     */
    public void setSigProvider(String sigProvider) {
        this.sigProvider = sigProvider;
    }

    /**
     * @param sigProviderName
     */
    public void setSigProviderName(String sigProviderName) {
        this.sigProviderName = sigProviderName;
    }

    /**
     * @param isSilent
     */
    public void setSilent(boolean isSilent) {
        Logger logger = Logger.getLogger(loggerName);
        Handler[] handlers = logger.getHandlers();
        for (Handler handler : handlers) {
            if (isSilent) {
                logger.setLevel(Level.OFF);
            } else {
                if (isVerbose) {
                    logger.setLevel(Level.FINE);
                    handler.setLevel(Level.FINE);
                } else {
                    logger.setLevel(Level.INFO);
                    handler.setLevel(Level.INFO);
                }
            }
        }
        this.isSilent = isSilent;
    }

    /**
     * @param storePass
     */
    public void setStorePass(char[] storePass) {
        this.storePass = storePass;
    }

    /**
     * @param storeType
     */
    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    /**
     * @param storeURI
     */
    public void setStoreURI(String storeURI) {
        this.storeURI = storeURI;
    }

    /**
     * @param tsaCertAlias
     */
    public void setTsaCertAlias(String tsaCertAlias) {
        this.tsaCertAlias = tsaCertAlias;
    }

    /**
     * @param tsaURI
     */
    public void setTsaURI(URI tsaURI) {
        this.tsaURI = tsaURI;
    }

    /**
     * @return
     */
    String getAlias() {
        return alias;
    }

    /**
     * @return
     */
    String getAltSigner() {
        return altSigner;
    }

    /**
     * @return
     */
    String getAltSignerPath() {
        return altSignerPath;
    }

    /**
     * @return
     */
    String getCertProvider() {
        return certProvider;
    }

    /**
     * @return
     */
    String getCertProviderName() {
        return certProviderName;
    }

    /**
     * @return
     */
    String getHelpTopic() {
        return helpTopic;
    }

    /**
     * @return
     */
    boolean isCerts() {
        return isCerts;
    }

    /**
     * @return
     */
    boolean isInternalSF() {
        return isInternalSF;
    }

    /**
     * @return
     */
    boolean isSectionsOnly() {
        return isSectionsOnly;
    }

    /**
     * @return
     */
    boolean isSilent() {
        return isSilent;
    }

    /**
     * @return
     */
    boolean isVerbose() {
        return isVerbose;
    }

    /**
     * @return
     */
    boolean isVerify() {
        return isVerify;
    }

    /**
     * @return
     * @throws IOException 
     */
    JarFile getJarFile() throws IOException {
        if (jarFile == null) {
            try {
                File file;
                try {
                    // try to open the file as if jarURIPath is an URI
                    URI jarURI = new URI(jarURIPath);
                    file = new File(jarURI);
                } catch (URISyntaxException e) {
                    // try to open the file as if jarURIPath is a path
                    file = new File(jarURIPath);
                } catch (IllegalArgumentException e) {
                    file = new File(jarURIPath);
                }
                jarFile = new JarFile(file, isVerify);
            } catch (IOException e) {
                throw (IOException) new IOException("Failed to load JAR file "
                        + jarURIPath).initCause(e);
            }
        }
        return jarFile;
    }

    /**
     * @return
     */
    String getJarURIorPath() {
        return jarURIPath;
    }

    /**
     * @return
     */
    String getKeyAlg() {
        return keyAlg;
    }

    /**
     * @return
     */
    char[] getKeyPass() {
        return keyPass;
    }

    /**
     * @return
     * @throws JarSignerException
     */
    KeyStore getKeyStore() throws JarSignerException {
        if (keyStore == null) {
            String ksProvName = (ksProviderName != null) ? ksProviderName
                    : providerName;
            // If the path to the store is not specified, try to open
            // the store using the default path.
            if (storeURI == null) {
                storeURI = KeytoolParameters.defaultKeystorePath;
            }
            try {
                keyStore = KeyStoreLoaderSaver.loadStore(storeURI, storeType,
                        storePass, ksProvName);
            } catch (Exception e) {
                throw new JarSignerException("Cannot load the keystore "
                        + storeURI, e);
            }
        }
        return keyStore;
    }

    /**
     * @return
     */
    String getKsProvider() {
        return ksProvider;
    }

    /**
     * @return
     */
    String getKsProviderName() {
        return ksProviderName;
    }

    /**
     * @return
     */
    String getMdProvider() {
        return mdProvider;
    }

    /**
     * @return
     */
    String getMdProviderName() {
        return mdProviderName;
    }

    /**
     * @return
     */
    String getProvider() {
        return provider;
    }

    /**
     * @return
     */
    String getProviderName() {
        return providerName;
    }

    /**
     * @return 
     */
    String getProxy() {
        return proxy;
    }

    /**
     * @return 
     */
    int getProxyPort() {
        return proxyPort;
    }

    /**
     * @return 
     */
    Proxy.Type getProxyType() {
        return proxyType;
    }

    /**
     * @return
     */
    String getSigAlg() {
        return sigAlg;
    }

    /**
     * @return
     */
    String getSigFileName() {
        // If the file name is not processed by FileNameGenerator.
        if (!isSFNameProcessed) {
            sigFileName = FileNameGenerator
                    .generateFileName(sigFileName, alias);
            isSFNameProcessed = true;
        }
        return sigFileName;
    }

    /**
     * @return
     */
    String getSignedJARName() {
        return signedJARName;
    }

    /**
     * @return
     */
    String getSigProvider() {
        return sigProvider;
    }

    /**
     * @return
     */
    String getSigProviderName() {
        return sigProviderName;
    }

    /**
     * @return
     */
    char[] getStorePass() {
        return storePass;
    }

    /**
     * @return
     */
    String getStoreType() {
        if (storeType == null){
            storeType = KeyStore.getDefaultType();
        }
        return storeType;
    }

    /**
     * @return
     */
    String getStoreURI() {
        return storeURI;
    }

    /**
     * @return
     */
    String getTsaCertAlias() {
        return tsaCertAlias;
    }

    /**
     * @return
     */
    URI getTsaURI() {
        return tsaURI;
    }
}

