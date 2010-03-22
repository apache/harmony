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

package org.apache.harmony.jretools.tests.keytool;

/**
 * Class to hold constant Strings used in all tests for Keytool.
 */
class TestUtils {
    // key store file name
    final static String ksFile = "bks.keystore";
    
    // keystore type
    final static String storeType = "BKS";

    // keystore password
    final static String ksPass = "123123";

    // key password
    final static String keyPass = "321321";
    
    // alias name
    final static String alias = "alias";
    
    // arguments to generate a self-signed certificate
    final static String[] genKeySelfSignedArgs = { "-genkey", 
        "-keystore", ksFile,
        "-storepass", ksPass, 
        "-storetype", storeType, 
        "-alias", alias,
        "-keypass", keyPass, 
        "-keysize", "512", 
        "-keyalg", "RSA",
        "-sigalg", "MD5withRSA", 
        "-dname", "CN=CN,OU=OU,O=O,L=L,ST=ST,C=C",
        "-validity", "365", 
        "-x509version", "1", 
        "-certserial", "1504" };

    // error message
    final static String excNotThrown = "Expected exception has not been thrown.";
}

