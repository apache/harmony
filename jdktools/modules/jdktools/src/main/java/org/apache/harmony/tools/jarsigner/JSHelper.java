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

package org.apache.harmony.tools.jarsigner;

import java.util.logging.Logger;

/**
 * The class prints JarSigner help message.
 */
class JSHelper {
    /**
     * Prints help message. 
     */
    static void printHelp(){
        StringBuilder buf = new StringBuilder();
        buf.append("JarSigner help.");
        buf.append("\nUsage:");
        buf.append("\tjarsigner {options} JAR-file alias");
        buf.append("\n\tjarsigner -verify {options} JAR-file");
        
        buf.append("\n\n-verify\t\t\t\t verify a signed JAR file");
        
        buf.append("\n-keystore <keystore_path>\t location of the keystore");
        
        buf.append("\n-storetype <keystore_type>\t type of the keystore");
        
        buf.append("\n-storepass <keystore_password>\t keystore password");
        
        buf.append("\n-keypass <key_password>\t\t private key password ");
        buf.append("(if differs from <keystore_password>)");
        
        buf.append("\n-signedjar <file_name>\t\t name of the signed JAR file");

        buf.append("\n-sigfile <file_name>\t\t name of .SF and .DSA files");
        
        buf.append("\n-verbose \t\t\t provide additional output");
        
        buf.append("\n-silent \t\t\t provide as few output as possible");
        
        buf.append("\n-certs \t\t\t\t display certificates ");
        buf.append("(use with -verify and -verbose)");
        
        buf.append("\n-tsa <TSA_URL>\t\t\t location of time-stamp authority");
        
        buf.append("\n-tsacert <TSA_cert_alias>\t keystore alias of the ");
        buf.append("TSA certificate");
        
        buf.append("\n-proxy <host_address>{:<port>}\t proxy server host ");
        buf.append("address and port, e.g. proxy.server.com:1234");

        buf.append("\n-proxytype <type_name>\t\t type of the proxy server (HTTP or SOCKS)");
        
        buf.append("\n-providerclass <class>\t\t class name of cryptographic ");
        buf.append("service provider");
        
        buf.append("\n-providername <name>\t\t provider name");
        
        buf.append("\n-ksproviderclass <class>\t class name of cryptographic ");
        buf.append("service provider for managing keystore");
        
        buf.append("\n-ksprovidername <name>\t\t keystore provider name");
        
        buf.append("\n-sigproviderclass <class>\t class name of cryptographic ");
        buf.append("service provider for work with signatures");
        
        buf.append("\n-sigprovidername <name>\t\t signature provider name");
        
        buf.append("\n-certproviderclass <class>\t class name of cryptographic ");
        buf.append("service provider for work with certificates");
        
        buf.append("\n-certprovidername <name>\t certificate provider name");
        
        buf.append("\n-mdproviderclass <class>\t class name of cryptographic ");
        buf.append("service provider for work with message digests");
        
        buf.append("\n-mdprovidername <name>\t\t message digest provider name\n");
        
        Logger.getLogger(JSParameters.loggerName).info(buf.toString());
    }
}

