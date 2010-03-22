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

package org.apache.harmony.tools.keytool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import org.apache.harmony.tools.toolutils.KeyStoreLoaderSaver;

/**
 * Class for loading the main keystore, saving ang changing its password.
 */
public class KeytoolKSLoaderSaver {
    /**
     * Creates an instance of class KeyStore and loads a keystore to it.
     * param.getStorePass() is used to check the integrity of the keystore. If
     * the password is not set in param, the integrity is not checked. If the
     * path to the store is not defined an empty keystore is created.
     * 
     * @param param -
     *            KeytoolParameters object which is used to get path to the
     *            store and password to unlock it or check its integrity.
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     */
    static void loadStore(KeytoolParameters param)
            throws NoSuchAlgorithmException, CertificateException,
            FileNotFoundException, IOException, KeyStoreException,
            NoSuchProviderException {

        // If the path to the store is not specified, try to open
        // the store using the default path.
        if (param.getStorePath() == null) {
            param.setStorePath(KeytoolParameters.defaultKeystorePath);
        }
        String ksProvider = (param.getKsProvider() != null) ? param
                .getKsProvider() : param.getProvider();
        KeyStore keyStore;
        File ksFile;
        URI uri;
        try {
            uri = new URI(param.getStorePath());
            ksFile = new File(uri);
        } catch (URISyntaxException e) {
            ksFile = new File(param.getStorePath());
        } catch (IllegalArgumentException e){
            ksFile = new File(param.getStorePath());
        }
        if (ksFile.exists()) {
            // load an existing store
            keyStore = KeyStoreLoaderSaver.loadStore(param.getStorePath(),
                    param.getStoreType(), param.getStorePass(), ksProvider);
        } else {
            // create a new store if it doesn't exist
            keyStore = KeyStoreLoaderSaver.loadStore(null,
                    param.getStoreType(), param.getStorePass(), ksProvider);
            param.setNeedSaveKS(true);
        }
        param.setKeyStore(keyStore);
    }

    /**
     * Saves the main keystore to the file and protects its integrity with
     * password.
     * 
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     * @throws NoSuchProviderException
     */
    static void saveStore(KeytoolParameters param) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException,
            NoSuchProviderException {
        KeyStoreLoaderSaver.saveStore(param.getKeyStore(),
                param.getStorePath(), param.getStorePass(), param.isVerbose());
    }

    /**
     * Changes the keystore password to the new one.
     * 
     * @param param
     */
    static void storePasswd(KeytoolParameters param) {
        param.setStorePass(param.getNewPasswd());
    }
}

