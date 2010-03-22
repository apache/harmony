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

/** enumeration of commands available for execution */
public enum Command {
    // show help message
    HELP,
    // generate a key pair or a secret key
    GENKEY,
    // generate a self-signed certificate using
    // an existing key pair
    SELFCERT,
    // import a trusted certificate or a CSR reply
    IMPORT,
    // export a certificate
    EXPORT,
    // change store password
    STOREPASSWD,
    // change key password
    KEYPASSWD,
    // generate a certificate signing request (CSR)
    CERTREQ,
    // check a CRL
    CHECK,
    // convert keystore to another format
    CONVERT,
    // verify a certificate chain
    VERIFY,
    // copy a key entry into a newly created one
    KEYCLONE,
    // print out the store contents
    LIST,
    // print out a certificate which is not in keystore yet
    PRINTCERT,
    // remove an entry from the store
    DELETE
}

