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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.Collections;

/**
 * Class for reading an X.509 certificate or an X.509 certificate chain from the
 * file or standard input.
 */
public class CertReader {
    // certificate factory to read certificates and CRLs
    private static CertificateFactory certFactory;
    // time to wait for user to input the data.
    // need this for user to have time to paste the certificate to stdin.
    private static long sleepPeriod;

    
    /**
     * Reads an X.509 certificate or a certificate chain from the file with the
     * given name or from stdin if the fileName is null and generates a
     * collection of Certificates.
     * 
     * @param fileName
     * @param readOnlyFirst
     * @param providerName
     * @return
     * @throws NoSuchProviderException 
     * @throws CertificateException 
     * @throws IOException 
     */
    static Collection readCerts(String fileName, boolean readOnlyFirst,
            String providerName) throws CertificateException,
            NoSuchProviderException, IOException {

        InputStream input = getInputStream(fileName);
        CertificateFactory factory = getCertificateFactory(providerName);
        if (input == System.in){
            System.out.println("Please, input certificate(s)...");
        }
        try {
            // let the user paste the certificates or CRLs, if read from stdin.
            // If reading from file, don't sleep.
            Thread.sleep(sleepPeriod);
        } catch (InterruptedException e) {
            // do nothing
        }

        // if the file is empty or nothing was entered
        // FIXME: remove available. Try to read and catch exception?
        if (input.available() <= 0) {
            throw new IOException("Empty input");
        }

        Collection certCollection;
        try {
            // if only the first certificate is requested, return a
            // single-element Collection
            if (readOnlyFirst) {
                certCollection = Collections.singleton(factory
                        .generateCertificate(input));
            } else {
                certCollection = factory.generateCertificates(input);
            }
            if (input != System.in) {
                input.close();
            }
            return certCollection;
        } catch (CertificateException e) {
            throw new CertificateException(
                    "Failed to generate a certificate from the input. ", e);
        }
    }

    /**
     * Reads CRLs from the file with given name and generates a collection of
     * CRLs.
     * 
     * @param fileName
     * @param providerName
     * @return
     * @throws NoSuchProviderException
     * @throws CertificateException
     * @throws IOException
     * @throws CRLException
     * 
     */
    static Collection readCRLs(String fileName, String providerName)
            throws CertificateException, NoSuchProviderException, IOException,
            CRLException {

        InputStream input = getInputStream(fileName);
        CertificateFactory factory = getCertificateFactory(providerName);
        if (input == System.in){
            System.out.println("Please, input CRL(s)...");
        }
        try {
            // let the user paste the certificates or CRLs, if read from stdin.
            // If reading from file, don't sleep.
            Thread.sleep(sleepPeriod);
        } catch (InterruptedException e) {
            // do nothing
        }

        // if the file is empty or nothing was entered
        // FIXME: remove available. Try to read and catch exception?
        if (input.available() <= 0) {
            throw new IOException("Empty input");
        }

        try {
            Collection crlCollection = factory.generateCRLs(input);
            if (input != System.in) {
                input.close();
            }
            return crlCollection;
        } catch (CRLException e) {
            throw new CRLException("Failed to generate a CRL from the input. ",
                    e);
        }
    }

    
    // Returns an input stream - FileInputStream or System.in.
    private static InputStream getInputStream(String fileName)
            throws FileNotFoundException {
        if (fileName != null) {
            sleepPeriod = 0;
            // use the file if its name is specified
            return new FileInputStream(fileName);
        } else {// if the file name is not given, use stdin
            sleepPeriod = 3000;
            return System.in;
        }
    }

    // Sets certFactory if it is still not set and returns it
    private static CertificateFactory getCertificateFactory(String providerName)
            throws CertificateException, NoSuchProviderException {
        if (certFactory == null) {
            try {
                if (providerName == null) {
                    certFactory = CertificateFactory.getInstance("X.509");
                } else {
                    certFactory = CertificateFactory.getInstance("X.509",
                            providerName);
                }
            } catch (CertificateException e) {
                throw new CertificateException(
                        "This type of certificate is not "
                                + "available from the provider. ", e);
            } catch (NoSuchProviderException e) {
                throw (NoSuchProviderException) new NoSuchProviderException(
                        "The provider " + providerName
                                + " is not found in the environment.")
                        .initCause(e);
            }
        }
        return certFactory;
    }
}
