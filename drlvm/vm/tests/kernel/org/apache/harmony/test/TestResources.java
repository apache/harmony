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
package org.apache.harmony.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Various test utilities are provided as static methods of this class.
 * 
 * @author Alexey V. Varlamov
 */
public class TestResources {

    /**
     * Name of system property specifying URL or filepath of
     * isolated bundle with test resources.
     * 
     * @see #getLoader()
     */
    public static final String RESOURCE_PATH = "test.resource.path";

    private static ClassLoader loader;

    /**
     * Certain tests may require existence of isolated test resources - 
     * i.e. some resources not available via system (caller's) loader.
     * This method is intended to support such resources. 
     * @see #RESOURCE_PATH
     * @return a classloader which is aware of location of isolated resources
     */
    public static ClassLoader getLoader() {
        if (loader == null) {
            loader = createLoader();
        }
        return loader;
    }
    
    public static ClassLoader createLoader() {
        URL url = null;
        try {
            String path = System.getProperty(RESOURCE_PATH, ".");
            File f = new File(path);
            if (f.exists()) {
                url = f.toURI().toURL();
            } else {
                url = new URL(path);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(
                    "Misconfigured path to test resources. "
                            + "Please set correct value of system property: "
                            + RESOURCE_PATH, e);
        }
        return URLClassLoader.newInstance(new URL[] { url });        
    }
}
