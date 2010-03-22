/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.tools.appletviewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

public class Main {

    static final String propertiesFileName = ".harmony.appletviewer.properties";
    static final String httpProxyHost  = "http.proxyHost";
    static final String httpProxyPort  = "http.proxyPort";
    static final String httpsProxyHost = "https.proxyHost";
    static final String httpsProxyPort = "https.proxyPort";
    static final String ftpProxyHost   = "ftp.proxyHost";
    static final String ftpProxyPort   = "ftp.proxyPort";
    static final Properties properties = new Properties();

    static File propertiesFile;

    public static void main(String argv[]) throws Exception {

        if (argv.length == 0) {
            printHelp();
            return;
        }

        ArrayList<String> propertiesList = new ArrayList<String>();
        ArrayList<String> urlsList = new ArrayList<String>();

        for(int i = 0; i < argv.length; i++){
            if(argv[i].startsWith("-D")){
                propertiesList.add(argv[i].substring(2));
            } else {
                urlsList.add(argv[i]);
            }
        }

        if (urlsList.size() == 0) {
            printHelp();
            return;
        }

        // Load stored java.properties
        String userHomeDir = System.getProperty("user.home");
        propertiesFile = new File(userHomeDir + 
            File.separator + Main.propertiesFileName);

        boolean needStore = false;
        if(propertiesFile.exists()){
            try{
                properties.load(new FileInputStream(propertiesFile));
            } catch(IOException e){
            }
        } else {
            properties.setProperty(httpProxyHost, "");
            properties.setProperty(httpProxyPort, "");
            properties.setProperty(httpsProxyHost, "");
            properties.setProperty(httpsProxyPort, "");
            properties.setProperty(ftpProxyHost, "");
            properties.setProperty(ftpProxyPort, "");
            needStore = true;
        }
        

        // Parse command line java.properties

        Iterator<String> iterator = propertiesList.iterator();
        while(iterator.hasNext()){
            String prop = iterator.next();
            if(prop != null){
                String[] pair = prop.split("=");
                String key = null;
                String val = null;

                if(pair[0] != null){
                    key = pair[0].trim();
                }

                if(pair[1] != null){
                    val = pair[1].trim();
                }

                if(key != null && key != "" && val != null && val != ""){
                    if(validatePropertyName(key)){
                        properties.setProperty(key, val);
                        needStore = true;
                    } else {
                        System.err.println("Unknown proxy property: " + key);
                        System.exit(-1);
                    }

                    if(key.endsWith("Port")){
                        try{
                            if(Integer.parseInt(val) < 0){
                                wrongPortMessage(val);
                                System.exit(-1);
                            }
                        } catch(NumberFormatException ex){
                            wrongPortMessage(key);
                            System.exit(-1);
                        }
                    }
                }
            }
        }

        if(needStore) storeProxyProperties();

        Enumeration<?> e = properties.propertyNames();

        while(e.hasMoreElements()){
            String key = (String)e.nextElement();
            String val = properties.getProperty(key);
            if(val != null && val != ""){
                System.setProperty(key, val);
            }
        }

        HTMLParser parser = new HTMLParser();
        Object []applets = parser.parse(urlsList.toArray(new String[urlsList.size()]), 0);
        
        // Start applets
        for (int i = 0; i < applets.length; i++)
            new AppletFrame((AppletInfo)applets[i]);
    }

    static void storeProxyProperties(){
        try{
            if(!propertiesFile.exists()) propertiesFile.createNewFile();
            properties.store(new FileOutputStream(propertiesFile),
                "User-specific properties for Harmony AppletViewer");
        } catch(IOException e){
        }
    }

    private static boolean validatePropertyName(String name){
        if(!name.equals(httpProxyHost) && !name.equals(httpProxyPort) &&
            !name.equals(httpsProxyHost) && !name.equals(httpsProxyPort) &&
            !name.equals(ftpProxyHost) && !name.equals(ftpProxyPort)){

            return false;
        } else {
            return true;
        }
    }

    private static void wrongPortMessage(String portName){
        System.err.println();
        System.err.println("Proxy parameter error: " + portName + " must be a positive integer value");
    }

    private static void printHelp() {
        System.err.println("AppletViewer");
        System.err.println("Usage: appletviewer [[-Dproxy.property] ... ] url(s)");
        System.err.println();
        System.err.println("Available proxy properties:");
        System.err.println();
        System.err.println("\thttp.proxyHost");
        System.err.println("\thttp.proxyPort");
        System.err.println("\thttps.proxyHost");
        System.err.println("\thttps.proxyPort");
        System.err.println("\tftp.proxyHost");
        System.err.println("\tftp.proxyPort");
    }
}

