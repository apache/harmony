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
/** 
 * @author Igor A. Pyankov 
 */ 

package org.apache.harmony.x.print.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/*
 *  auxilary class FactoryLocator 
 *  for abstract class javax.print.StreamPrintServiceFactory
 */
public class FactoryLocator {    
    List factoryClasses; /* List of accessible Factories */  
             
    /*
     * Constructor for FactoryLocator
     */
    public FactoryLocator() {
        super();
        factoryClasses = new ArrayList();
    }
    
    public List getFactoryClasses(){
        return  factoryClasses;
    }
    
    public void lookupAllFactories() throws IOException{        
        Enumeration setOfFactories = null;                         
        ClassLoader classLoader = null;     // current class loader
        InputStream inputStream = null;     // stream for reader


        classLoader = (ClassLoader) AccessController
                                        .doPrivileged(new PrivilegedAction() {
                public Object run(){
                    ClassLoader cl = Thread.currentThread()
                                        .getContextClassLoader();
                    if (cl == null) {
                        cl = ClassLoader.getSystemClassLoader();
                    }
                    return cl;
                }
            }
        );

        if (classLoader == null) {
            return; //something is wrong with classloader    
        }      
        
        // get all needed resources
        try {
            setOfFactories = classLoader.getResources(
                   "META-INF/services/javax.print.StreamPrintServiceFactory");
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("IOException during resource finding");
        }
        
        try {
            while (setOfFactories.hasMoreElements()) {
                URL url = (URL) setOfFactories.nextElement();               
                inputStream = url.openStream();
                getFactoryClasses(inputStream);            
            }
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new IOException("IOException during resource reading");
        }
       
    }

    /*
     * factory class extraction method 
     */  
    private void getFactoryClasses(InputStream is) throws IOException {
        BufferedReader factoryNameReader;
        Class factoryClass;
        String name = null;        
        
        // creation reader
        try {
            factoryNameReader = new BufferedReader(new InputStreamReader(is,
                    "UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            // Let's try 7-bit encoding :(
            factoryNameReader = new BufferedReader(new InputStreamReader(is));
        }

        if (factoryNameReader == null) {            
            return; //no reader for classes
        }
        //reading class names and filling factoryClasses array         
        try {
            while(true){
                name = factoryNameReader.readLine();                
                if (name == null){
                    return;
                }
                if (name.length() > 0 && name.charAt(0) != '#'){
                    //it is not comment nor empty string
                    factoryClass = Class.forName(name);                   
                    factoryClasses.add(factoryClass.newInstance());  
                }
            }
        } catch (IOException e) {             
            throw new IOException("IOException during reading file");
        } catch (ClassNotFoundException e) {
            throw new IOException("Class" + name + " is not found");
        } catch (InstantiationException e) {
            throw new IOException("Bad instantiation of class" + name);
        } catch (IllegalAccessException e) {
            throw new IOException("Illegal access for class" + name);
        } 
    }

}