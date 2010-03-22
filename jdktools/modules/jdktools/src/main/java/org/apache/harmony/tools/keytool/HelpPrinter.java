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
 * Class for printing help messages .
 */
public class HelpPrinter {
    private static StringBuffer message;

    final static String certReq = "-certreq";
    final static String checkCRL = "-checkcrl";
    final static String convert = "-convert";
    final static String delete = "-delete";
    final static String export = "-export";
    final static String genKey = "-genkey";
    final static String help = "-help";
    final static String sImport = "-import";
    final static String keyClone = "-keyclone";
    final static String keyPasswd = "-keypasswd";
    final static String list = "-list";
    final static String printCert = "-printcert";
    final static String selfCert = "-selfcert";
    final static String storePasswd = "-storepasswd";
    final static String verify = "-verify";
    
    final static String keyStore = " {-keystore <keystore_path>}";
    final static String storeType = " {-storetype <store_type>}";
    final static String keyPass = " {-keypass <key_password>}";
    final static String oldKeyPass = " {-keypass <old_key_password>}";
    final static String storePass = " {-storepass <store_password>}";
    final static String provider = " {-provider <provider_name>}";
    final static String certReqFile = " {-file <csr_file>}";
    final static String certFile = " {-file <certificate_file>}";
    final static String keyAlg = " {-keyalg <key_algorithm>}";
    final static String sigAlg = " {-sigalg <signature_algorithm>}";
    final static String keySize = " {-keysize <key_size>}";
    final static String alias = " {-alias <alias>}";
    final static String dName = " {-dname <X500_distinguished_dname>}";
    final static String validity = " {-validity <validity_period>}";
    final static String verbose = " {-v}";
    final static String verboseOrRfc = " {-rfc | -v}";
    final static String javaOption = " {-J<javaoption>}";
    final static String crlFile = " {-crlfile <crl_file>}";
    final static String convKeyStore = " {-convkeystore <result_store>}";
    final static String convStoreType = " {-convtype <result_type>}";
    final static String convStorePass = " {-convstorepass <result_store_pass>}";
    final static String convKeys = " {-convkeys}";
    final static String ca = " {-ca}";
    final static String secretKey = " {-secretkey}";
    final static String trustCAcerts = " {-trustcacerts}";
    final static String noPrompt = " {-noprompt}";
    final static String cacerts = " {-cacerts <cacerts_path>}";
    final static String cacertsPass = " {-cacertspass <cacerts_password>}";
    final static String x509version = " {-x509version <X509_version>}";
    final static String dest = " {-dest <dest_alias>}";
    final static String sNew = " {-new <new_password>}";
    final static String issuer = " {-issuer <issuer_alias>}";
    final static String issuerPass = " {-issuerpass <issuer_password>}";
    final static String serialNum = " {-certserial <cert_serial_number>}";
    final static String newLine = "\n";
    final static String doubleNewLine = "\n\n";
    final static String ksTypePassVProvCacerts = newLine + keyStore + storeType
            + newLine + storePass + verbose + provider + newLine + cacerts
            + cacertsPass + doubleNewLine;

    /**
     * Prints the help message.
     */
    static void printHelp() {
        if (message == null) {
            message = new StringBuffer();
            String tab = "\t";
            String doubleTab = "\t\t";
            String tripleTab = "\t\t\t";
            message.append("\nKeytool usage:\n");
            message.append("keytool {-<command_name>} {-<command_option>}"
                    + " {<option_value>}... -J<java_option>\n\n");
            message.append("Known commands:\n");
            message.append(tab + certReq + doubleTab
                    + "Generate certificate request\n");
            message.append(tab + checkCRL + doubleTab
                    + "Check certificates revocation status\n");
            message.append(tab + convert + doubleTab
                    + "Convert keystore to another format\n");
            message.append(tab + delete + tripleTab
                    + "Remove entry from keystore\n");
            message.append(tab + export + tripleTab
                    + "Export certificate to a file or stdout\n");
            message.append(tab + genKey + tripleTab
                    + "Secret key or key pair generation\n");
            message.append(tab + help + tripleTab
                    + "This help message or help on a command\n");
            message.append(tab + sImport + tripleTab
                    + "Import a certificate (chain) or a CSR reply\n");
            message.append(tab + keyClone + doubleTab
                    + "Duplicate a key entry\n");
            message.append(tab + keyPasswd + doubleTab
                    + "Change key password\n");
            message.append(tab + printCert + doubleTab
                    + "Print to stdout a certificate from file\n");
            message.append(tab + selfCert + doubleTab
                    + "Generate a self-signed certificate "
                    + "with existing key\n");
            message.append(tab + storePasswd + doubleTab
                    + "Change keystore password\n");
            message.append(tab + verify + tripleTab
                    + "Verify a certificate chain\n");

            message.append("\nHelp usage:\n");
            message.append("keytool -help {<command_name>}\n");
            message.append("E.g.:\t keytool -help genkey\n");
        }
        System.out.println(message);
    }

    static void topicHelp(String topic) {
        StringBuffer topicMsg = new StringBuffer();
        if (topic.equalsIgnoreCase("certreq")) {
            topicMsg.append(" Generates a Certificate Signing Request "
                    + "(CSR). The request is generated\n");
            topicMsg.append(" based on data taken from keystore entry "
                    + "associated with alias given.\n");
            topicMsg.append(" The certificate request "
                    + "is printed to a file, if its name is supplied\n");
            topicMsg.append(" or otherwise printed to stdout.\n");
            topicMsg.append("\ncertreq Usage:\n");
            topicMsg.append(certReq + alias + certReqFile + newLine + sigAlg
                    + keyPass + ksTypePassVProvCacerts);

        } else if (topic.equalsIgnoreCase("checkcrl")) {
            topicMsg.append(" Checks if the certificate given in the file "
                    + "is contained in the CRL which\n");
            topicMsg.append(" is stored in the CRL file. If the file "
                    + "name is not given, stdin is used.\n");
            topicMsg.append("\ncheckcrl Usage:\n");
            topicMsg.append(checkCRL + certFile + crlFile
                    + ksTypePassVProvCacerts);
        } else if (topic.equalsIgnoreCase("convert")) {
            topicMsg.append(" Converts keystore to another format.\n"
                    + " If \"-convkeys\" option has been specified, "
                    + "an attempt to convert\n key entries is performed."
                    + " Only entries with password equal to \n"
                    + " keystore password are converted.\n");
            topicMsg.append("\nconvert Usage:\n");
            topicMsg.append(convert + convStoreType + convKeyStore + newLine
                    + convStorePass + convKeys + ksTypePassVProvCacerts);

        } else if (topic.equalsIgnoreCase("delete")) {
            topicMsg.append(" Removes from the keystore the entry "
                    + "associated with alias.\n");
            topicMsg.append("\ndelete Usage:\n");
            topicMsg.append(delete + alias + ksTypePassVProvCacerts);

        } else if (topic.equalsIgnoreCase("export")) {
            topicMsg.append(" Reads an X.509 certificate associated with "
                    + "alias and prints it into the\n");
            topicMsg.append(" given file. If The file");
            topicMsg.append(" name is not given, the certificate is printed\n"
                    + " to stdout.\n");
            topicMsg.append("\nexport Usage:\n");
            topicMsg.append(export + verboseOrRfc + alias + certFile
                    + ksTypePassVProvCacerts);

        } else if (topic.equalsIgnoreCase("genkey")) {
            topicMsg.append(" Generates a key pair or a secret key."
                    + " Key pair is composed of a private\n");
            topicMsg.append(" and a public key. Wraps the public key "
                    + "into a self-signed X.509\n");
            topicMsg.append(" (v1, v2, v3) certificate and puts the "
                    + "certificate into a single-element\n");
            topicMsg.append(" certificate chain or signs the certificate "
                    + "with private key from another\n");
            topicMsg.append(" key entry and adds its chain to the newly "
                    + "generated certificate . After\n");
            topicMsg.append(" that adds to the keystore a new "
                    + "entry containing the generated\n");
            topicMsg.append(" private key and the chain. If a secret key is "
                    + "generated it is put into a\n");
            topicMsg
                    .append(" secret key entry, with null certificate chain.\n");
            topicMsg
                    .append(" If \"-ca\" option is specified, generated certificate\n");
            topicMsg
                    .append(" will can be used for signing another certificates.\n");
            topicMsg
                    .append(" If \"-secretkey\" option is specified, a secret key will.\n");
            topicMsg
                    .append(" be generated instead of key pair and a certificate which\n");
            topicMsg.append(" are generated by default. \n");

            topicMsg.append("\ngenkey usage\n");
            topicMsg.append(genKey + alias + keyAlg + newLine + keySize
                    + sigAlg + newLine + validity + dName + newLine
                    + x509version + ca + serialNum + newLine + secretKey
                    + keyPass + newLine + issuer + issuerPass
                    + ksTypePassVProvCacerts);
        } else if (topic.equalsIgnoreCase("help")) {
            printHelp();
        } else if (topic.equalsIgnoreCase("import")) {
            topicMsg.append(" Reads an X.509 certificate or a PKCS#7 "
                    + "formatted certificate chain from\n");
            topicMsg.append(" the file specified in param and puts it "
                    + "into the entry identified by the\n");
            topicMsg.append(" supplied alias. If the input file is "
                    + "not specified, the certificates are\n");
            topicMsg.append(" read from the standard input.\n");
            topicMsg.append("\nimport Usage:\n");
            topicMsg.append(sImport + alias + certFile + newLine + noPrompt
                    + trustCAcerts + newLine + keyPass + cacerts + newLine
                    + cacertsPass + ksTypePassVProvCacerts);

        } else if (topic.equalsIgnoreCase("keyclone")) {
            topicMsg.append(" Copies the key and the certificate "
                    + "chain (if any) from the keystore entry\n");
            topicMsg.append(" identified by given alias into a newly "
                    + "created one with given destination.\n");
            topicMsg.append("\nkeyclone Usage:\n");
            topicMsg.append(keyClone + alias + dest + newLine + sNew + keyPass
                    + ksTypePassVProvCacerts);

        } else if (topic.equalsIgnoreCase("keypasswd")) {
            topicMsg.append(" Changes the key password to the new one.\n");
            topicMsg.append("\nkeypasswd Usage:\n");
            topicMsg.append(keyPasswd + alias + oldKeyPass + newLine + sNew
                    + ksTypePassVProvCacerts);

        } else if (topic.equalsIgnoreCase("list")) {
            topicMsg.append(" Prints the contents of the entry associated "
                    + "with the alias given. \n");
            topicMsg.append(" If no alias is specified, the contents of "
                    + "the entire keystore are printed.\n");
            topicMsg.append("\nlist Usage:\n");
            topicMsg.append(list + verboseOrRfc + alias
                    + ksTypePassVProvCacerts);

        } else if (topic.equalsIgnoreCase("printcert")) {
            topicMsg.append(" Prints the detailed description of a "
                    + "certificate in a human-readable\n");
            topicMsg.append(" format: its owner and issuer, serial number, "
                    + "validity period and\n");
            topicMsg.append(" fingerprints.\n");
            topicMsg.append("\nprintcert Usage:\n");
            topicMsg.append(printCert + verbose + certFile + doubleNewLine);

        } else if (topic.equalsIgnoreCase("selfcert")) {
            topicMsg.append(" Generates an X.509 (v1, v2, v3) self-signed "
                    + "certificate using a key pair\n");
            topicMsg.append(" associated with alias. "
                    + "If X.500 Distinguished Name is supplied it is \n");
            topicMsg.append(" used as both subject and issuer of the"
                    + "certificate. Otherwise the\n");
            topicMsg.append(" distinguished name associated with alias is"
                    + " used. Signature algorithm,\n");
            topicMsg.append(" validity period and certificate serial"
                    + " number are taken from command line if \n");
            topicMsg.append(" defined there or "
                    + "from the keystore entry identified by alias.\n");
            topicMsg
                    .append(" If \"-ca\" option is specified, generated certificate\n");
            topicMsg
                    .append(" will can be used for signing another certificates.\n");
            topicMsg
                    .append(" If \"-secretkey\" option is specified, a secret key will.\n");
            topicMsg
                    .append(" be generated instead of key pair and a certificate which\n");
            topicMsg.append(" are generated by default. \n");
            topicMsg.append("\nselfcert Usage:\n");
            topicMsg.append(selfCert + alias + dName + newLine + validity
                    + sigAlg + newLine + keyPass + ca + serialNum
                    + ksTypePassVProvCacerts);

        } else if (topic.equalsIgnoreCase("storepasswd")) {
            topicMsg.append(" Changes the keystore password to the new one.\n");
            topicMsg.append("\nstorepasswd Usage:\n");
            topicMsg.append(storePasswd + sNew + ksTypePassVProvCacerts);

        } else if (topic.equalsIgnoreCase("verify")) {
            topicMsg.append(" A cerificate chain is built by looking up "
                    + "the certificate of the issuer\n");
            topicMsg.append(" of the current certificate. If a sertificate "
                    + "is self-signed it is assumed\n");
            topicMsg.append(" to be the root CA. After that the certificates "
                    + "are searched in the lists\n");
            topicMsg.append(" of revoked certificates. Certificate signatures "
                    + "are checked and\n");
            topicMsg.append(" certificate path is built in the same way as in "
                    + "import operation. If an\n");
            topicMsg.append(" error occurs the flow is not stopped but an "
                    + "attempt to continue is made.\n");
            topicMsg.append(" The results of the verification are"
                    + " printed to stdout.\n");
            topicMsg.append("\nverify Usage:\n");
            topicMsg.append(verify + certFile + crlFile + newLine
                    + trustCAcerts + cacerts + newLine + cacertsPass
                    + ksTypePassVProvCacerts);

        } else {
            System.out.println("The option with name <" + topic
                    + "> is unknown.");
            printHelp();
            return;
        }
        System.out.println(topicMsg);
    }

}
