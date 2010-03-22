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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.harmony.luni.util.Base64;

/**
 * Class for exporting the certificates to a file or stdout in DER or PEM
 * formats.
 */
public class CertExporter {

    /**
     * Reads an X.509 certificate associated with alias and prints it into the
     * given file. alias and the file name are supplied in param. if The file
     * name is not given, the certificate is printed to stdout.
     * 
     * @param param
     * @throws KeyStoreException
     * @throws IOException
     * @throws KeytoolException
     * @throws NoSuchProviderException 
     * @throws CertificateException 
     * @throws NoSuchAlgorithmException 
     */
    static void exportCert(KeytoolParameters param) throws KeyStoreException,
            IOException, KeytoolException, NoSuchAlgorithmException,
            CertificateException, NoSuchProviderException {
        KeyStore keyStore = param.getKeyStore();
        String alias = param.getAlias();
        if (keyStore.entryInstanceOf(alias, KeyStore.SecretKeyEntry.class)) {
            throw new KeytoolException("The alias <" + alias
                    + "> points to a secret key entry.\n"
                    + "It has no certificates.");
        }

        X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
        byte[] encodedCert;
        try {
            encodedCert = cert.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new CertificateEncodingException(
                    "Failed to encode the certificate", e);
        }

        OutputStream output;
        String fileName = param.getFileName();
        // if no file name is given, output to System.out
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

        if (param.isRfc()) {
            output.write("-----BEGIN CERTIFICATE-----\n".getBytes());
            output.write(Base64.encode(encodedCert, "ISO-8859-1").getBytes());
            output.write("\n-----END CERTIFICATE-----\n".getBytes());
        } else {
            output.write(encodedCert);
        }
        output.flush();
        if (output != System.out) {
            output.close();

            if (param.isVerbose()) {
                System.out.println("The certificate is stored in file <"
                        + fileName + ">.");
            }
        }
    }

}

