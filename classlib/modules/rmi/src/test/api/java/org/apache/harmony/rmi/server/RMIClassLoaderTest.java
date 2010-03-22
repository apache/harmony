/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.rmi.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.server.RMIClassLoader;
import java.security.Permission;
import junit.framework.TestCase;

public class RMIClassLoaderTest extends TestCase {
    /**
     * Test for java.rmi.server.RMIclassLoader.loadClass() method testing that
     * RMI runtime does not change the order of the incoming codebase string
     * (regression test for HARMONY-1944).
     */
    public void testLoadClassCodebaseOrder() throws Exception {
        SecurityManager previous = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                /*
                 * Override checkPermission to allow everything. Specifically,
                 * we want to allow the SecurityManager to be set to null at the
                 * end of the test and we want to allow the 'testClass.jar' file
                 * to be allowed to load.
                 */
                return;
            }
        });
        try {
            URL testJarURL = getClass().getResource("testClass.jar");
            String[] paths = new String[] { testJarURL.getPath(),
            /*
             * to be sure this path will be the first after sorting
             */
            "/_fake.jar" };
            Class<?> c = RMIClassLoader.loadClass("file://" + paths[0] + " file://" + paths[1],
                    "TestClass", null);
            ClassLoader cl = c.getClassLoader();
            if (cl instanceof URLClassLoader) {
                URL[] urls = ((URLClassLoader) cl).getURLs();
                if (urls.length != 2) {
                    fail("Unexpected number of URLs: " + urls.length);
                }
                String failStr = "";
                for (int i = 0; i < urls.length; ++i) {
                    if (!urls[i].getPath().equals(paths[i])) {
                        failStr += "\nURL[" + i + "].getPath() = " + urls[i].getPath()
                                + ", expected: " + paths[i];
                    }
                }
                if (!failStr.equals("")) {
                    fail(failStr);
                }
            } else {
                fail("Class is loaded by non-URLClassLoader");
            }
        } finally {
            // reset the security manager back to null state
            System.setSecurityManager(previous);
        }
    }

    /**
      * Test for java.rmi.server.RMIClassLoader.loadProxyClass(String, String[], ClassLoader)
      * testing invalid url as a codebase.
      */
    public void testLoadProxyClassInvalidCodebase() throws Exception {
        //Regression for HARMONY-1133
        SecurityManager previous = System.getSecurityManager();
        System.setSecurityManager(null);
        
        try {
            RMIClassLoader.loadProxyClass("zzz", new String[] {}, null);
            fail("MalformedURLException expected");
        } catch (MalformedURLException e) { 
            //expected 
        } finally {
            System.setSecurityManager(previous);            
        }
    }

    /**
     * Test for java.rmi.server.RMIClassLoader.loadClass(String, String)
     * testing invalid url as a codebase.
     */
    public void testLoadClassInvalidCodebase() throws Exception {
        //Regression for HARMONY-1133
        SecurityManager previous = System.getSecurityManager();
        System.setSecurityManager(null);
        
        try { 
            RMIClassLoader.loadClass("zzz", "a123");
            fail("MalformedURLException expected");
        } catch (MalformedURLException e) { 
            //expected 
        } finally {
            System.setSecurityManager(previous);            
        } 
    }

    /**
     * Test for java.rmi.server.RMIClassLoader.getClassLoader(String)
     * testing invalid url as a codebase.
     */
    public void testGetClassLoaderInvalidCodebase() throws Exception {
        //Regression for HARMONY-1134
        SecurityManager previous = System.getSecurityManager();
        System.setSecurityManager(null);

        try { 
            RMIClassLoader.getClassLoader("zzz");
            fail("MalformedURLException expected");
        } catch (MalformedURLException e) { 
            //expected 
        } finally {
            System.setSecurityManager(previous);            
        } 
    }
}
