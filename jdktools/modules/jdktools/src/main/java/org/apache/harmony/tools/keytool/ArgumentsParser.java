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
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * The class to interact with the user - parse the program arguments, ask for
 * confirmations, and necessary parameters which haven't been set in the command
 * line.
 */

class ArgumentsParser {
    // used to get additional data prompted
    static InputStreamReader in = new InputStreamReader(System.in);

    // buffer for the data read
    static char[] readData = new char[256];

    // number of symbols read
    static int charsRead;

    // minimum password length permitted
    static int minPwdLength = 6;

    // maximum number of attempts to set the password
    static int maxNrOfAttempts = 3;

    // length of the "\r\n" which is added to the end of the line,
    // when ENTER is pressed.
    private static int newLineLength = 2;

    // options names to compare to //
    // commands
    final static String sGenkey = "-genkey";

    final static String sSelfcert = "-selfcert";

    final static String sImport = "-import";

    final static String sExport = "-export";

    final static String sStorepasswd = "-storepasswd";

    final static String sKeypasswd = "-keypasswd";

    final static String sCertreq = "-certreq";

    final static String sCheck = "-checkcrl";

    final static String sConvert = "-convert";

    final static String sVerify = "-verify";

    final static String sPrintcert = "-printcert";

    final static String sKeyclone = "-keyclone";

    final static String sDelete = "-delete";

    final static String sList = "-list";

    final static String sHelp = "-help";

    // additional options
    final static String sKeystore = "-keystore";

    final static String sStoretype = "-storetype";

    final static String sProvider = "-provider";

    final static String sCertProvider = "-certprovider";
    
    final static String sKeyProvider = "-keyprovider";
    
    final static String sMdProvider = "-mdprovider";
    
    final static String sSigProvider = "-sigprovider";
    
    final static String sKsProvider = "-ksprovider";
    
    final static String sConvKsProvider = "-convprovider";
    
    final static String sStorepass = "-storepass";

    final static String sAlias = "-alias";

    final static String sKeyalg = "-keyalg";

    final static String sKeysize = "-keysize";

    final static String sSigalg = "-sigalg";

    final static String sDname = "-dname";

    final static String sKeypass = "-keypass";

    final static String sValidity = "-validity";

    final static String sV = "-v";

    final static String sJ = "-J";

    final static String sFile = "-file";

    final static String sNoprompt = "-noprompt";

    final static String sTrustcacerts = "-trustcacerts";

    final static String sRfc = "-rfc";

    final static String sNew = "-new";

    final static String sIssuerAlias = "-issuer";
    
    final static String sIssuerPass = "-issuerpass";
    
    final static String sSecretkey = "-secretkey";

    final static String sX509Version = "-x509version";

    final static String sCertSerial = "-certserial";

    final static String sDestAlias = "-dest";

    final static String sCRLfile = "-crlfile";
    
    final static String sCA = "-ca";
    
    final static String sConvStorePath = "-convkeystore";
    
    final static String sConvStorePass = "-convstorepass";
    
    final static String sConvStoreType = "-convtype";

    final static String sConvKeyEntries = "-convkeys";
    
    final static String sCAcertsPath = "-cacerts";
    
    final static String sCAcertsPass = "-cacertspass";

    /**
     * The method finds known options in args which is usually taken from
     * command line and sets the corresponding fields of the returned
     * KeytoolParameters object to given values.
     * 
     * @param args -
     *            String array to parse.
     * @return null if args is null or zero-sized, one of the elements of args
     *         is null or empty, an unknown option is found or an expected
     *         option value is not given or not of an expected type.
     * @throws IOException
     * @throws NumberFormatException
     * @throws KeytoolException
     */

    static KeytoolParameters parseArgs(String[] args)
            throws NumberFormatException, KeytoolException, IOException {
        if (args == null || args.length == 0) {
            return null;
        }
        KeytoolParameters param = new KeytoolParameters();

        // look for known options and get their values.
        try {
            for (int i = 0; i < args.length; i++) {

                // commands
                if (args[i].compareToIgnoreCase(sGenkey) == 0) {
                    param.setCommand(Command.GENKEY);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sSelfcert) == 0) {
                    param.setCommand(Command.SELFCERT);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sImport) == 0) {
                    param.setCommand(Command.IMPORT);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sExport) == 0) {
                    param.setCommand(Command.EXPORT);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sStorepasswd) == 0) {
                    param.setCommand(Command.STOREPASSWD);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sKeypasswd) == 0) {
                    param.setCommand(Command.KEYPASSWD);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sCertreq) == 0) {
                    param.setCommand(Command.CERTREQ);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sCheck) == 0) {
                    param.setCommand(Command.CHECK);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sConvert) == 0) {
                    param.setCommand(Command.CONVERT);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sVerify) == 0) {
                    param.setCommand(Command.VERIFY);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sPrintcert) == 0) {
                    param.setCommand(Command.PRINTCERT);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sKeyclone) == 0) {
                    param.setCommand(Command.KEYCLONE);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sDelete) == 0) {
                    param.setCommand(Command.DELETE);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sList) == 0) {
                    param.setCommand(Command.LIST);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sHelp) == 0) {
                    param.setCommand(Command.HELP);
                    if (args.length == i + 2){
                        param.setHelpTopic(args[++i]);
                    }
                    continue;
                }

                // additional options
                if (args[i].compareToIgnoreCase(sKeystore) == 0) {
                    param.setStorePath(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sStoretype) == 0) {
                    param.setStoreType(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sProvider) == 0) {
                    param.setProvider(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sCertProvider) == 0) {
                    param.setCertProvider(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sKeyProvider) == 0) {
                    param.setKeyProvider(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sMdProvider) == 0) {
                    param.setMdProvider(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sSigProvider) == 0) {
                    param.setSigProvider(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sKsProvider) == 0) {
                    param.setKsProvider(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sConvKsProvider) == 0) {
                    param.setConvKsProvider(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sAlias) == 0) {
                    param.setAlias(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sKeyalg) == 0) {
                    param.setKeyAlg(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sSigalg) == 0) {
                    param.setSigAlg(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sDname) == 0) {
                    param.setDName(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sFile) == 0) {
                    param.setFileName(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sIssuerAlias) == 0) {
                    param.setIssuerAlias(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sStorepass) == 0) {
                    param.setStorePass(args[++i].toCharArray());
                    continue;
                }
                if (args[i].compareToIgnoreCase(sKeypass) == 0) {
                    param.setKeyPass(args[++i].toCharArray());
                    continue;
                }
                if (args[i].compareToIgnoreCase(sIssuerPass) == 0) {
                    param.setIssuerPass(args[++i].toCharArray());
                    continue;
                }
                if (args[i].compareToIgnoreCase(sCRLfile) == 0) {
                    param.setCrlFile(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sDestAlias) == 0) {
                    param.setDestAlias(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sNew) == 0) {
                    param.setNewPasswd(args[++i].toCharArray());
                    continue;
                }
                if (args[i].compareToIgnoreCase(sConvStorePath) == 0) {
                    param.setConvertedKeyStorePath(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sConvStoreType) == 0) {
                    param.setConvertedKeyStoreType(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sConvStorePass) == 0) {
                    param.setConvertedKeyStorePass(args[++i].toCharArray());
                    continue;
                }
                if (args[i].compareToIgnoreCase(sCAcertsPath) == 0) {
                    param.setCacertsPath(args[++i]);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sCAcertsPass) == 0) {
                    param.setCacertsPass(args[++i].toCharArray());
                    continue;
                }
                if (args[i].compareToIgnoreCase(sKeysize) == 0) {

                    param.setKeySize((new Integer(args[++i])).intValue());
                    if (param.getKeySize() <= 0) {
                        throw new KeytoolException("Key size"
                                + " must be more than zero.");
                    }
                    continue;
                }
                if (args[i].compareToIgnoreCase(sValidity) == 0) {
                    param.setValidity((new Integer(args[++i])).intValue());
                    if (param.getValidity() <= 0) {
                        throw new KeytoolException("Validity"
                                + " must be more than zero.");
                    }
                    continue;
                }
                if (args[i].compareToIgnoreCase(sX509Version) == 0) {
                    param.setX509version((new Integer(args[++i])).intValue());
                    if (param.getX509version() < 1
                            || param.getX509version() > 3) {
                        throw new KeytoolException(
                                "Certificate version must be " + "1, 2 or 3");
                    }
                    continue;
                }
                if (args[i].compareToIgnoreCase(sCertSerial) == 0) {
                    param.setCertSerialNr((new Integer(args[++i])).intValue());
                    if (param.getCertSerialNr() <= 0) {
                        throw new KeytoolException("Certificate serial number"
                                + " must be more than zero.");
                    }
                    continue;
                }

                // flags
                if (args[i].compareToIgnoreCase(sNoprompt) == 0) {
                    param.setNoPrompt(true);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sTrustcacerts) == 0) {
                    param.setTrustCACerts(true);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sRfc) == 0) {
                    param.setRfc(true);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sV) == 0) {
                    param.setVerbose(true);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sSecretkey) == 0) {
                    param.setSecretKey(true);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sCA) == 0) {
                    param.setCA(true);
                    continue;
                }
                if (args[i].compareToIgnoreCase(sConvKeyEntries) == 0) {
                    param.setConvertKeyEntries(true);
                    continue;
                }

                System.out.println("Illegal option: " + args[i]);
                return null;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // ignore the last option if its value is not provided
        }

        Command cmd = param.getCommand();

        // check whether -v and -rfc options are used separately with -list.
        if (cmd == Command.LIST && param.isRfc() && param.isVerbose()) {
            throw new KeytoolException("There must not be both -v and -rfc "
                    + "options specified");
        }

        // skip the store password setting if -printcert or -help commands were
        // given.
        if (cmd == Command.PRINTCERT || cmd == Command.HELP) {
            return param;
        }

        // if the store password has not been entered, prompt for it
        if (param.getStorePass() == null) {
            // get path to the store
            String storePath = (param.getStorePath() != null) ? param
                    .getStorePath() : KeytoolParameters.defaultKeystorePath;
            // get store password
            String prompt = "Enter keystore password: ";
            System.out.print(prompt);
            charsRead = in.read(readData);
            char[] storePass;
            File storeFile = new File(storePath);
            if (storeFile.isDirectory()) {
                throw new KeytoolException("The keystore path " + storePath
                        + " points to a directory.");
            }
            // Allow short passwords to unlock existing stores and
            // disallow passwords shorter than minPwdLength symbols for new
            // ones.
            // Check whether the file exists
            if (!storeFile.exists()) {
                // check of password length and additional prompts for
                // password are made here
                storePass = promptLongerPassword(prompt);
            } else {
                // if the store exists don't check the length
                storePass = new char[charsRead - newLineLength];// remove "\r\n"
                System.arraycopy(readData, 0, storePass, 0, charsRead
                        - newLineLength);
            }
            if (storePass.length != 0) {
                param.setStorePass(storePass);
            } else {
                param.setStorePass(null);
            }
        }

        return param;
    }

    /**
     * Checks if the needed values are set and, if not, prompts for them.
     * 
     * This method must be called after the keystore is loaded.
     * 
     * @param param
     * @return
     * @throws KeytoolException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchProviderException 
     * @throws CertificateException 
     */
    static void getAdditionalParameters(KeytoolParameters param)
            throws KeytoolException, IOException, KeyStoreException,
            UnrecoverableKeyException, NoSuchAlgorithmException,
            CertificateException, NoSuchProviderException {
        // this method must be called after the keystore is loaded.
        KeyStore keyStore = param.getKeyStore();

        // set the alias to "mykey" if it's not set up
        Command command = param.getCommand();
        if (param.getAlias() == null
                && (command == Command.KEYCLONE || command == Command.EXPORT
                        || command == Command.CERTREQ
                        || command == Command.GENKEY
                        || command == Command.SELFCERT
                        || command == Command.IMPORT || command == Command.KEYPASSWD)) {
            param.setAlias("mykey");
        }
        String alias = param.getAlias();
        
        // check if the alias exists
        if (command == Command.CERTREQ || command == Command.DELETE
                || command == Command.EXPORT || command == Command.KEYCLONE
                || command == Command.KEYPASSWD || command == Command.SELFCERT
                || (command == Command.LIST && param.getAlias() != null)) {
            if (!keyStore.containsAlias(param.getAlias())) {
                throw new KeytoolException("Alias <" + alias
                        + "> doesn't exist");
            }
        } else if (command == Command.GENKEY){
            if (keyStore.containsAlias(param.getAlias())) {
                throw new KeytoolException("Key(s) not generated, alias <"
                        + alias + "> already exists.");
            }
        }

        // if the key password has not been entered and the password is required
        // to get the key (it is not a password for a newly created entry)
        if (param.getKeyPass() == null
                && (command == Command.KEYCLONE || command == Command.EXPORT
                        || command == Command.CERTREQ
                        || command == Command.KEYPASSWD
                        || command == Command.SELFCERT
                // if keystore contains alias, import of a certificate reply
                // is considered, otherwise password is unnecessary.
                || (command == Command.IMPORT && keyStore.containsAlias(alias)))) {
            param.setKeyPass(tryStorePassAsKeyPass(keyStore, alias, param
                    .getStorePass()));
        }
        
        switch (command) {
            case GENKEY:
                // if the distinguished name is not specified, get the
                // necessary data
                if (param.getDName() == null && !param.isSecretKey()) {
                    param.setDName(getDistinguishedName());
                }
                // if the key password has not been entered (and can equal store
                // password)
                if (param.getKeyPass() == null) {
                    param.setKeyPass(getNewPassword(null, alias, param
                            .getStorePass()));
                }
                
                String issuerAlias = param.getIssuerAlias(); 
                // if the newly generated certificate should be signed with 
                // another certificate chain from the keystore. 
                if (issuerAlias != null && !param.isSecretKey()) {
                    // Check if the issuer password was entered. If not, try storepass.
                    // If it's not ok, prompt the user.
                    if (param.getIssuerPass() == null) {
                        param.setIssuerPass(tryStorePassAsKeyPass(keyStore, issuerAlias,
                                param.getStorePass()));
                    }
                }
                
                break;

            case KEYCLONE:
                // prompt for a destination alias, if one is not specified
                if (param.getDestAlias() == null) {
                    System.out.print("Enter destination alias name: ");
                    charsRead = in.read(readData);
                    if (charsRead <= newLineLength) {
                        throw new KeytoolException(
                                "Must specify destination alias");
                    } else {
                        param.setDestAlias(new String(readData).substring(0,
                                charsRead - newLineLength));
                    }
                }
                // if the password for a newly created entry is not specified,
                // ask for it.
                if (param.getNewPasswd() == null) {
                    param.setNewPasswd(getNewPassword(alias, param
                            .getDestAlias(), param.getKeyPass()));
                }
                break;
            case DELETE:
                // prompt for an alias to delete, if one is not specified
                if (alias == null) {
                    System.out.print("Enter alias name: ");
                    charsRead = in.read(readData);
                    if (charsRead <= newLineLength) {
                        throw new KeytoolException("Must specify alias");
                    } else {
                        param.setAlias(new String(readData).substring(0,
                                charsRead - newLineLength));
                    }
                }
                break;
            case STOREPASSWD:
            case KEYPASSWD:
                String prompt;
                String promptReenter;
                // prompt for a new password, if it is not specified
                if (command == Command.KEYPASSWD) {
                    prompt = "Enter new key password for <" + alias + ">: ";
                    promptReenter = "Re-enter new keystore password for <"
                            + alias + ">: ";
                } else { // if param.getCommand() == Command.STOREPASSWD
                    // prompt for a new store password, if it is not specified
                    prompt = "Enter new keystore password: ";
                    promptReenter = "Re-enter new keystore password: ";
                }

                // if the new password is not entered
                if (param.getNewPasswd() == null) {
                    System.out.print(prompt);
                    charsRead = in.read(readData);
                    char[] password = promptLongerPassword(prompt);
                    System.out.print(promptReenter);
                    charsRead = in.read(readData);
                    if (charsRead == password.length + newLineLength) {
                        for (int i = 0; i < password.length; i++) {
                            if (readData[i] != password[i]) {
                                throw new KeytoolException(
                                        "Passwords do not match");
                            }
                        }
                        param.setNewPasswd(password);
                    } else {
                        throw new KeytoolException("Passwords do not match");
                    }
                    // if entered a short password in the command line
                } else if (param.getNewPasswd().length < minPwdLength) {
                    throw new KeytoolException("The password must be at least "
                            + minPwdLength + " characters");
                }

                break;
            case LIST:
                if (alias != null) {
                    // This check is not where the same thing for other
                    // commands done, because (alias != null) check is
                    // necessary.
                    if (keyStore.entryInstanceOf(alias,
                            KeyStore.SecretKeyEntry.class)
                            && param.getKeyPass() == null) {
                        param.setKeyPass(tryStorePassAsKeyPass(keyStore, alias,
                                param.getStorePass()));
                    }
                }
                break;
        }// switch (param.getCommand())

    }

    /**
     * The method prompts user to enter data to initialize an X.500
     * Distinguished Name to create a new certificate. It gets the data asks if
     * it is correct, and if it is returns the String representing the
     * distinguished name, if the data entered is not correct prompts to enter
     * it again.
     * 
     * @return - String representing the distinguished names
     */
    private static String getDistinguishedName() throws IOException,
            KeytoolException {
        // X.500 principal: CN, OU, O, L, ST, C;
        String[] dnFields = { "CN=", ", OU=", ", O=", ", L=", ", ST=", ", C=" };
        // the flag is set to true, when the user confirms that
        // the data he (or she) entered is correct.
        boolean isCorrect = false;
        // X.500 Distinguished Name. It will look like:
        // "CN=user_name, OU=org_unit, O=organization, L=city, ST=state,
        // C=com"
        StringBuffer dname = new StringBuffer(256);
        // the flag is set to true when there are spaces and/or commas in
        // the fields of the distinguished name
        boolean needQuotes = false;

        // data that user enters is saved here
        StringBuffer[] dnFieldsData = new StringBuffer[] {
                new StringBuffer("Unknown"), new StringBuffer("Unknown"),
                new StringBuffer("Unknown"), new StringBuffer("Unknown"),
                new StringBuffer("Unknown"), new StringBuffer("Unknown") };

        // prompts to show to user when asking to enter some data
        String[] prompts = { "Enter your first and last name: ",
                "Enter the name of your organizational unit: ",
                "Enter the name of your organization: ",
                "Enter the name of your city or locality: ",
                "Enter the name of your state or province: ",
                "Enter the two-letter country code for the unit: ",
                "Is the information you entered correct? [no]: " };

        // do it until user confirms that the data he entered is true.
        while (!isCorrect) {
            // clear dname if it is not empty
            if (dname.length() > 0) {
                dname.delete(0, dname.length());
            }
            for (int i = 0; i < dnFieldsData.length; i++) {
                // ask the user to enter info
                System.out.println(prompts[i]);
                // print the current value of the field
                System.out.print("[" + dnFieldsData[i] + "]: ");
                dname.append(dnFields[i]);
                charsRead = in.read(readData);
                // if something was entered put the new value to
                // dnFieldsData
                // else don't change what was entered before.
                if (charsRead > newLineLength) {
                    // check whether quotes are needed.
                    needQuotes = false;
                    for (int j = 0; j < charsRead - newLineLength; j++) {
                        if (readData[j] == ',' || readData[j] == ' ') {
                            needQuotes = true;
                            break;
                        }
                    }
                    // if quotes are not needed
                    if (!needQuotes) {
                        // copy the read data into the StringBuffer.
                        // don't need the '\r' and \n' in the end
                        dnFieldsData[i].insert(0, readData, 0, charsRead
                                - newLineLength);
                        dnFieldsData[i].delete(charsRead - newLineLength,
                                dnFieldsData[i].length());
                    } else {// if quotes are needed, add them to the begin
                        // and to the end
                        dnFieldsData[i].insert(0, '\"');
                        dnFieldsData[i].insert(1, readData, 0, charsRead
                                - newLineLength);
                        dnFieldsData[i].insert(charsRead - 1, '\"');
                        dnFieldsData[i].delete(charsRead, dnFieldsData[i]
                                .length());
                    }
                }
                dname.append(dnFieldsData[i]);
            }
            // print the distinguished name with the fields filled
            System.out.println(dname);
            // confirm, if the user enters 'y' or "yes"
            // any other input results in asking the questions again
            isCorrect = getConfirmation(prompts[prompts.length - 1], true);
        }
        // save the data when got the confirmation from the user
        return new String(dname);
    }

    /**
     * The method should be called only after the password was entered and put into
     * readData. charsRead also shouldn't be changed after the password was
     * entered and before the method is called. If charsRead is less than
     * minPwdLength + newLineLength, the method just copies the password from
     * readData into a newly created char array; otherwise it prompts for a
     * longer password for maxNrOfAttempts times.
     * 
     * @param prompt
     * @return new password of length equal or longer than minPwdLength
     * @throws IOException
     * @throws KeytoolException
     */
    private static char[] promptLongerPassword(String prompt)
            throws IOException, KeytoolException {
        int cntAttempts = 0;
        while (charsRead < minPwdLength + newLineLength) {
            System.out.println("The password must be at least " + minPwdLength
                    + " characters");
            System.out.print(prompt);
            charsRead = in.read(readData);
            ++cntAttempts;
            if (cntAttempts >= maxNrOfAttempts) {
                throw new KeytoolException("Too many failures. "
                        + "Please, try again later.");
            }
        }
        char[] password = new char[charsRead - newLineLength];
        System.arraycopy(readData, 0, password, 0, charsRead - newLineLength);
        return password;
    }

    /**
     * Does all work to get from the user a password for a newly created (cloned
     * or generated) key.
     * 
     * @param -
     *            srcAlias is the alias of the entry to clone, or if it is null,
     *            the keystore password will be prompted to use.
     * @param -
     *            destAlias is the alias of the newly created entry.
     * @param -
     *            srcPass is the password to be used with a new entry if the
     *            user doesn't enter a new one.
     * 
     * @return - char array representing the password for the entry. It can be
     *         equal to the keystore password or the password of a cloned key.
     */
    private static char[] getNewPassword(String srcAlias, String destAlias,
            char[] srcPass) throws IOException, KeytoolException {
        if (destAlias == null) {
            return null;
        }
        String prompt = "Enter key password for <" + destAlias + ">: ";
        System.out.print(prompt);
        if (srcAlias == null) {
            System.out.print("(Press RETURN if same as for keystore) ");
        } else {
            System.out.print("(Press RETURN if same as for <" + srcAlias
                    + ">) ");
        }
        charsRead = in.read(readData);
        char[] destPass;
        // if RETURN was pressed
        if (charsRead <= newLineLength) {
            destPass = new char[srcPass.length];
            System.arraycopy(srcPass, 0, destPass, 0, srcPass.length);
        } else {// if some password was entered
            destPass = promptLongerPassword(prompt);
        }
        return destPass;
    }

    /**
     * Prints a promt. Reads what the user enters. If the user has entered
     * 'y'/"yes" or 'n'/"no" (case insensitively) the method returns
     * respectively true or false. Depending on acceptAnother parameter the
     * method can return false if anything except 'y' or "yes" is entered, or it
     * can prompt for a correct answer. If only ENTER is pressed false is
     * returned.
     * 
     * @param promt -
     *            text printed to ask the user for a confirmation
     * @param acceptAnother -
     *            if set to true, the method returns true if and only if the
     *            user enters 'y' or "yes"; if set to false prompts to reenter
     *            the answer from user until 'y'/"yes" or 'n'/"no" is entered.
     * @return true if the user confirms the request, false - otherwise.
     * @throws IOException
     */
    static boolean getConfirmation(String promt, boolean acceptAnother)
            throws IOException, KeytoolException {
        int counter = 0;
        while (counter++ < 100) {
            System.out.print(promt);
            charsRead = in.read(readData);
            // if pressed ENTER return the default value
            if (charsRead == newLineLength) {
                return false;
            }
            // confirm, if the user enters 'y' or "yes"
            if ((charsRead == newLineLength + 1 && (readData[0] == 'y' || readData[0] == 'Y'))
                    || (charsRead == newLineLength + 3 && "yes"
                            .equalsIgnoreCase(new String(readData).substring(0,
                                    3)))) {
                return true;
            } else if (acceptAnother) {
                return false;
            } else {
                // if entered 'n' or "no"
                if (readData[0] == 'n'
                        || readData[0] == 'N'
                        && ((charsRead == newLineLength + 1) || (charsRead == newLineLength + 2
                                && readData[0] == 'o' || readData[0] == 'O'))) {
                    return false;
                } else {
                    System.out.println("Wrong answer, please, try again");
                }
            }
        }
        throw new KeytoolException("Too many failures. ");
    }
    
    // method tries to get the key, associated with alias, using the storePass,
    // if it can be recovered using the password storePass is returned,
    // otherwise - the password is prompted for. Another attempt to recover the
    // key with entered password. If it is ok, it is returned, otherwise
    // UnrecoverableKeyException is thrown.
    private static char[] tryStorePassAsKeyPass(KeyStore keyStore,
            String alias, char[] storePass) throws KeyStoreException,
            IOException, UnrecoverableKeyException, NoSuchAlgorithmException {
        try {
            // try to get a key with keystore password
            // if succeed set key password same as that for keystore
            keyStore.getKey(alias, storePass);

            // will not come here if exception is thrown
            return storePass;
        } catch (UnrecoverableKeyException e) {
            // if key password is not equal to store password, ask for it.
            System.out.print("Enter key password for <" + alias + ">: ");
            charsRead = in.read(readData);
            char[] keyPass = new char[charsRead - newLineLength];
            System
                    .arraycopy(readData, 0, keyPass, 0, charsRead
                            - newLineLength);
            // if the new password is incorrect an exception will be thrown
            try {
                keyStore.getKey(alias, keyPass);
            } catch (NoSuchAlgorithmException nsae) {
                throw new NoSuchAlgorithmException(
                        "Cannot find the algorithm to recover the key. ", e);
            }
            return keyPass;
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException(
                    "Cannot find the algorithm to recover the key. ", e);
        }
    }

}
