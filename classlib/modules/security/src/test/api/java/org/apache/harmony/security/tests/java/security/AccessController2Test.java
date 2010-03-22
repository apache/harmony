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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.security.tests.java.security;

import java.io.File;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.PrivilegedAction;

import tests.support.Support_Exec;

public class AccessController2Test extends junit.framework.TestCase {

	/**
	 * @tests java.security.AccessController#doPrivileged(java.security.PrivilegedAction)
	 */
	public void test_doPrivilegedLjava_security_PrivilegedAction() throws Exception {

         // tmp user home to avoid presence of ${user.home}/.java.policy
        String tmpUserHome = System.getProperty("java.io.tmpdir")
                + File.separatorChar + "tmpUserHomeForAccessController2Test";
        File dir = new File(tmpUserHome);
        if (!dir.exists()) {
            dir.mkdirs();
            dir.deleteOnExit();
        }
        String javaPolycy = tmpUserHome + File.separatorChar
                + ".java.policy";
        assertFalse("There should be no java policy file: " + javaPolycy,
                new File(javaPolycy).exists());

        String[] arg = new String[] { "-Duser.home=" + tmpUserHome,
                doPrivilegedLjava_security_PrivilegedActionTesting.class.getName() };

        Support_Exec.execJava(arg, null, true);
    }
        
    public static class doPrivilegedLjava_security_PrivilegedActionTesting {
        public static void main(String[] args) {

            // Pass fail flag...
            Boolean pass;

            // First test 1 argument version (TBD).

            // Then test 2 argument version.
            pass = (Boolean) AccessController.doPrivileged(
                    new PrivilegedAction() {
                        public Object run() {
                            try {
                                AccessController
                                        .checkPermission(new AllPermission());
                                return new Boolean(false);
                            } catch (SecurityException ex) {
                                return new Boolean(true);
                            }
                        }
                    }, null);
            assertTrue("Got AllPermission by passing in a null PD", pass
                    .booleanValue());

        }
    }

    /**
     * Test method tests the policy file with one of the entrie's
     * permission_class_name surrounded with quotes.
     * 
     */
    public void test_policyFileEntry_contains_Quotes1() throws Exception{
        System.setProperty("java.security.policy", "resources/policyTest2.txt");
        AccessController.checkPermission(new RuntimePermission("setSecurityManager"));
    }
    
    /**
     * Test method tests the policy file, one of the entrie's
     * permission_class_name surrounded with quotes.
     * 
     */
    public void test_policyFileEntry_contains_Quotes2() throws Exception{
        class CustomSecurityMgr extends SecurityManager{
            CustomSecurityMgr(){ }
        }

        System.setProperty("java.security.policy", "resources/policyTest2.txt");
        SecurityManager security = System.getSecurityManager();
        System.setSecurityManager(security);

        // Setting the CustomSecurity Manager
        SecurityManager customsecurity = new CustomSecurityMgr();
        System.setSecurityManager(customsecurity);
        customsecurity = System.getSecurityManager();
        System.setSecurityManager(security);
    }
}
