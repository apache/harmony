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
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * Class for managing keystore entries - cloning, deleting, changing entry
 * password.
 */
public class EntryManager {
    /**
     * Copies the key and the certificate chain (if any) from the keystore entry
     * identified by given alias into a newly created one with given destination
     * alias. alias and destination alias are specified in param.
     * 
     * @param param
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeytoolException
     * @throws IOException 
     * @throws NoSuchProviderException 
     * @throws FileNotFoundException 
     * @throws CertificateException 
     */
    static void keyClone(KeytoolParameters param) throws KeyStoreException,
            NoSuchAlgorithmException, UnrecoverableKeyException,
            KeytoolException, CertificateException, FileNotFoundException,
            NoSuchProviderException, IOException {
        KeyStore keyStore = param.getKeyStore();
        String alias = param.getAlias();
        Key srcKey;
        try {
            srcKey = keyStore.getKey(alias, param.getKeyPass());
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException(
                    "Cannot find the algorithm to recover the key. ", e);
        }
        // if the entry is a not a KeyEntry
        if (srcKey == null) {
            throw new KeytoolException("The entry <" + alias + "> has no key.");
        }
        Certificate[] certChain = keyStore
                .getCertificateChain(param.getAlias());
        keyStore.setKeyEntry(param.getDestAlias(), srcKey,
                param.getNewPasswd(), certChain);
        param.setNeedSaveKS(true);
    }

    /**
     * Removes from the keystore the entry associated with alias.
     * 
     * @param param
     * @throws KeyStoreException
     * @throws IOException 
     * @throws NoSuchProviderException 
     * @throws FileNotFoundException 
     * @throws CertificateException 
     * @throws NoSuchAlgorithmException 
     */
    static void delete(KeytoolParameters param) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            FileNotFoundException, NoSuchProviderException, IOException {
        param.getKeyStore().deleteEntry(param.getAlias());
        param.setNeedSaveKS(true);
    }

    /**
     * Changes the key password to the new one.
     * 
     * @param param
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws IOException 
     * @throws NoSuchProviderException 
     * @throws FileNotFoundException 
     * @throws CertificateException 
     */
    static void keyPasswd(KeytoolParameters param) throws KeyStoreException,
            NoSuchAlgorithmException, UnrecoverableKeyException,
            CertificateException, FileNotFoundException,
            NoSuchProviderException, IOException {
        KeyStore keyStore = param.getKeyStore();
        String alias = param.getAlias();
        Key key;
        Certificate[] chain;
        try {
            key = keyStore.getKey(alias, param.getKeyPass());
            chain = keyStore.getCertificateChain(alias);
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException(
                    "Cannot find the algorithm to recover the key. ", e);
        }

        keyStore.deleteEntry(alias);
        keyStore.setKeyEntry(alias, key, param.getNewPasswd(), chain);
        param.setKeyPass(param.getNewPasswd());
        param.setNeedSaveKS(true);
    }

}

