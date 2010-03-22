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
* @author Ivan Popov
*/

package org.apache.harmony.tools.internal.jdi;

import com.sun.jdi.VirtualMachineManager;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * This class is a proxy for JDI bootstrap implementation.
 *  
 * It looks for corresponding jars with JDI implementation, loads appropriate bootstrap class 
 * and redirects initialization to it. Details of the used JDI implementation are specified in 
 * Bootstrap.properties file.
 */
public class Bootstrap {

    /**
     * Default name for JDI properties bundle.
     */
    public static final String BUNDLE_JDI_NAME = "org.apache.harmony.tools.internal.jdi.Bootstrap";

    /**
     * Property to specify name of JDI Bootstrap class.
     */
    public static final String PROPERTY_JDI_BOOTSTRAP = "org.apache.harmony.tools.jdi.bootstrap";

    /**
     * Property to specify location of jars with JDI implementation.
     */
    public static final String PROPERTY_JDI_LOCATION = "org.apache.harmony.tools.jdi.location";

    /**
     * Property prefix to specify name of jars with JDI implementation.
     */
    public static final String PROPERTY_JDI_JAR = "org.apache.harmony.tools.jdi.jar.";
    
    /**
     * Loads JDI implementation from corresponding jars and redirects initialization to 
     * its Bootstrap class.
     *
     * @return instance of VirtualMachineManager created by JDI implementation
     * @throw RuntimeException if there is any problem in locating and loading JDI implementation
     */
    static public VirtualMachineManager virtualMachineManager() {
        if (vmm == null) {
            String bootstrapName = null;
            String baseDir = null;
            try {
                // load resource bundle with JDI implementation properties
                ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_JDI_NAME);
                
                // read JDI properties from resource bundle
                bootstrapName = bundle.getString(PROPERTY_JDI_BOOTSTRAP);
                
                try {
                    baseDir = bundle.getString(PROPERTY_JDI_LOCATION);
                } catch (MissingResourceException e) {
                    // ignore exception and use default location
                    baseDir = System.getProperty("java.home") + "/../lib";
                }
                
                ArrayList urls = new ArrayList();
                try {
                    for (int i = 0; ; i++) {
                        String key = PROPERTY_JDI_JAR + i;
                        String jar = bundle.getString(key);
                        URL url = new URL("file", null, baseDir + "/" + jar);
                        urls.add(url);
                    }
                } catch (MissingResourceException e) {
                    // ignore exception and finish reading jar entries
                }
                
                // create class loader for JDI implementation
                URL buf[] = new URL[urls.size()];
                ClassLoader loader = new URLClassLoader((URL[])(urls.toArray(buf)));

                // load and initialize JDI bootsrap
                Class cls = Class.forName(bootstrapName, true, loader);
                java.lang.reflect.Method method = cls.getMethod("virtualMachineManager", null);
                vmm = (VirtualMachineManager)method.invoke(null, null);
            } catch (Exception e) {
                throw new RuntimeException("Cannot initialize JDI bootstrap: " + bootstrapName, e);
            }
        }
        return vmm;
    }
    
    static private VirtualMachineManager vmm = null;
}
