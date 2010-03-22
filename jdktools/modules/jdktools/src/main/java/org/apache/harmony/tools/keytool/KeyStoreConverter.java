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
import java.security.cert.CertificateException;
import java.util.Enumeration;

import org.apache.harmony.tools.toolutils.KeyStoreLoaderSaver;

/**
 * Class to convert keystore to another format.
 */
public class KeyStoreConverter {
    /**
     * Converts keystore to another format.
     * 
     * @param param
     * @throws KeyStoreException
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws NoSuchProviderException
     * @throws IOException
     */
    static void convertKeyStore(KeytoolParameters param)
            throws KeyStoreException, FileNotFoundException,
            NoSuchAlgorithmException, CertificateException,
            NoSuchProviderException, IOException {

        // get the main keystore
        KeyStore mainKS = param.getKeyStore();
        String ksProvider = (param.getConvKsProvider() != null) ? param
                .getConvKsProvider() : param.getProvider();
        // creating a new keystore
        KeyStore convertedKS = KeyStoreLoaderSaver.loadStore(null, param
                .getConvertedKeyStoreType(), param.getConvertedKeyStorePass(),
                ksProvider);

        // get the aliases enumeration
        Enumeration aliases = mainKS.aliases();
        // counts converted entries
        int convertedCnt = 0;

        // if key entries should be converted just as certificate entries  
        if (param.isConvertKeyEntries()) { 
            // make a ProtectionParameter from main keystore password
            KeyStore.PasswordProtection mainKSpass = 
                new KeyStore.PasswordProtection(param.getStorePass());

            // make a ProtectionParameter from password of keystore 
            // to convert to
            KeyStore.PasswordProtection convertedKSpass = 
                new KeyStore.PasswordProtection(param.getConvertedKeyStorePass());

            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                try {
                    // if the entry is a certificate entry
                    if (mainKS.isCertificateEntry(alias)) {
                        convertedKS.setCertificateEntry(alias, mainKS
                                .getCertificate(alias));
                    } else {
                        // try to get the entry using the keystore password
                        KeyStore.Entry entry = mainKS.getEntry(alias,
                                mainKSpass);
                        convertedKS.setEntry(alias, entry, convertedKSpass);
                    }

                    // won't come here if exception is thrown
                    ++convertedCnt;
                } catch (Exception e) {
                    // Catch exception here, because program should 
                    // try to continue the work.
                    System.out.println("Failed to convert the entry <" + alias
                            + ">.");
                    System.out.println("\tReason: " + e);
                }
            } // while (aliases.hasMoreElements())...
        } else {
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                try {
                    if (mainKS.isCertificateEntry(alias)) {
                        convertedKS.setCertificateEntry(alias, mainKS
                                .getCertificate(alias));
                    }

                    // won't come here if exception is thrown
                    ++convertedCnt;
                } catch (Exception e) {
                    // Catch exception here, because program should 
                    // try to continue the work.
                    System.out.println("Failed to convert the entry <" + alias
                            + ">.");
                    System.out.println("Reason: " + e);
                }
            }
        } //if (!param.isConvertKeyEnties()) ...
        
        if (param.isVerbose()){
            System.out.println("Converted " + convertedCnt + " entries");
        }
            
        // save the converted keystore
        KeyStoreLoaderSaver.saveStore(convertedKS, param
                .getConvertedKeyStorePath(), param.getConvertedKeyStorePass(),
                param.isVerbose());
    }
}

