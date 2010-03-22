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

/**
 * The main class that bundles command line parsing, interaction with the user
 * and work with keys and certificates.
 *
 * Class that implements the functionality of the key and certificate management
 * tool.
 */
public class Main {

    /**
     * Does the actual work with keys and certificates, based on the parameter
     * param. If something goes wrong an exception is thrown.
     */
    static void doWork(KeytoolParameters param) throws Exception {
        switch (param.getCommand()) {
            case EXPORT:
                CertExporter.exportCert(param);
                break;
            case LIST:
                KeyStoreCertPrinter.list(param);
                break;
            case PRINTCERT:
                KeyStoreCertPrinter.printCert(param);
                break;
            case KEYCLONE:
                EntryManager.keyClone(param);
                break;
            case DELETE:
                EntryManager.delete(param);
                break;
            case STOREPASSWD:
                KeytoolKSLoaderSaver.storePasswd(param);
                break;
            case KEYPASSWD:
                EntryManager.keyPasswd(param);
                break;
            case IMPORT:
                CertImporter.importCert(param);
                break;
            case CHECK:
                CRLManager.checkRevoked(param);
                break;
            case VERIFY:
                CertChainVerifier.verifyChain(param);
                break;
            case CERTREQ:
                CSRGenerator.certReq(param);
                break;
            case HELP:
                if (param.getHelpTopic() != null){
                    HelpPrinter.topicHelp(param.getHelpTopic());
                } else {
                    HelpPrinter.printHelp();
                }
                break;
            case GENKEY:
                KeyCertGenerator.genKey(param);
                break;
            case SELFCERT:
                KeyCertGenerator.selfCert(param);
                break;
            case CONVERT:
                KeyStoreConverter.convertKeyStore(param);
                break;
        }
    }

    /**
     * The main method to run from another program.
     * 
     * @param args -
     *            command line with options.
     */
    public static void run(String[] args) throws Exception {
        KeytoolParameters param = ArgumentsParser.parseArgs(args);

        if (param == null) {
            HelpPrinter.printHelp();
            System.exit(-1);
        }

        Command command = param.getCommand();

        // all commands except printcert and help work with a store
        if (command != Command.PRINTCERT && command != Command.HELP) {
            // all commands that work with store except list and export
            // need store password to with keystore.
            if (param.getStorePass() == null && command != Command.LIST
                    && command != Command.EXPORT) {
                throw new KeytoolException(
                        "Must specify store password to work with this command.");
            }
            // prompt for additional parameters if some of the expected
            // ones have not been specified.
            ArgumentsParser.getAdditionalParameters(param);

            // print the warning if store password is not set
            if (param.getStorePass() == null) {
                System.out.println("\nWARNING!!!\nThe integrity "
                        + "of the keystore data has NOT been checked!\n"
                        + "To check it you must provide"
                        + " your keystore password!\n");
            }
        }

        // the work is being done here
        doWork(param);

        if (param.isNeedSaveKS()) {
            // save the store
            KeytoolKSLoaderSaver.saveStore(param);
        }
    }

    /**
     * The main method to run from command line.
     * 
     * @param args -
     *            command line with options.
     */
    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception e) {
            // System.out.println("Keytool error: " + e);
            e.printStackTrace();
        }
    }
}
