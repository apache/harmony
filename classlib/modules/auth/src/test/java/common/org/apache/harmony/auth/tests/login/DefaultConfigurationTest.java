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
* @author Maxim V. Makarov
*/

package org.apache.harmony.auth.tests.login;

import java.io.File;
import java.io.IOException;
import java.security.Permission;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.AuthPermission;
import javax.security.auth.login.AppConfigurationEntry;

import junit.framework.TestCase;

import org.apache.harmony.auth.login.DefaultConfiguration;
import org.apache.harmony.auth.tests.support.TestUtils;

import tests.support.resource.Support_Resources;

/**
 * Tests default configuration implementation.  
 */

public class DefaultConfigurationTest extends TestCase {

    private static final String LOGIN_CONFIG = "java.security.auth.login.config";
    
    static String otherConfFile = Support_Resources
            .getAbsoluteResourcePath("auth.conf");

    static AppConfigurationEntry[] ents;

    SecurityManager old = System.getSecurityManager();

    @Override
    public void tearDown() throws Exception {
        System.setSecurityManager(old);
    }

    /**
     * loading a config file specified on the system property
     * using -Djava.security.auth.login.config    
     */
    public void testLoadConfigFile_1() throws IOException {
        
        String oldp = System.getProperty(LOGIN_CONFIG); 
        try {
        System.setProperty(LOGIN_CONFIG, 
                new File(otherConfFile).getCanonicalPath());
        DefaultConfiguration dc = new DefaultConfiguration();
        assertNotNull(dc);
        
        ents = dc.getAppConfigurationEntry("Login2");
        assertNotNull(ents);
        ents = dc.getAppConfigurationEntry("other");
        assertNotNull(ents);
        ents = dc.getAppConfigurationEntry("Login1");
        assertNotNull(ents);
        Map<String, String> m = new HashMap<String, String>();
        for (AppConfigurationEntry element : ents) {
            assertEquals("com.intel.security.auth.module.LoginModule1",
                    element.getLoginModuleName());
            m.clear();
            m.put("debug1", "true");
            m.put("test1", "false");
            assertEquals(m, element.getOptions());
            assertEquals("LoginModuleControlFlag: required", element
                    .getControlFlag().toString());
        }
        
        ents = dc.getAppConfigurationEntry("Login7");
        assertNotNull(ents);

        
        assertEquals("com.intel.security.auth.module.LoginModule1", ents[0].getLoginModuleName());
        assertEquals("com.intel.security.auth.module.LoginModule2", ents[1].getLoginModuleName());
        assertEquals("com.intel.security.auth.module.LoginModule3", ents[2].getLoginModuleName());
        assertEquals("com.intel.security.auth.module.LoginModule4",  ents[3].getLoginModuleName());
        
        assertEquals("LoginModuleControlFlag: required", ents[0].getControlFlag().toString());
        assertEquals("LoginModuleControlFlag: optional", ents[1].getControlFlag().toString());
        assertEquals("LoginModuleControlFlag: sufficient", ents[2].getControlFlag().toString());
        assertEquals("LoginModuleControlFlag: requisite", ents[3].getControlFlag().toString());
        
        m.clear();
        m.put("AAAA", "true");
        m.put("BBB", "false");
        assertEquals(m, ents[0].getOptions());
        m.clear();
        m.put("debug2", "true");
        assertEquals(m, ents[1].getOptions());
        m.clear();
        m.put("debug2", "false");
        assertEquals(m, ents[2].getOptions());
        m.clear();
        m.put("ticketCache", System.getProperty("user.home")+ File.separator+"tickets");
        m.put("useTicketCache", "true");
        assertEquals(m, ents[3].getOptions());

        } finally {
            TestUtils.setSystemProperty(LOGIN_CONFIG, oldp);
        }
    }
    /**
     * test of the refresh method
     */
    public void testRefresh() throws IOException {
        
        String oldp = System.getProperty(LOGIN_CONFIG);
        try {
            System.setProperty(LOGIN_CONFIG, 
                    new File(otherConfFile).getCanonicalPath());

        DefaultConfiguration dc = new DefaultConfiguration();
        MySecurityManager checker = new MySecurityManager(new AuthPermission(
                "refreshLoginConfiguration"), true);
        System.setSecurityManager(checker);
        dc.refresh();
        assertTrue(checker.checkAsserted);
        checker.reset();
        checker.enableAccess = false;
        try {
            dc.refresh();
            fail("No expected SecurityException");
        } catch (SecurityException ex) {
        }
        } finally {
            TestUtils.setSystemProperty(LOGIN_CONFIG, oldp);
        }

    }

    /**
     * Easy the SecurityManager class
     * 
     */

    class MySecurityManager extends SecurityManager {

        public boolean enableAccess;

        public Permission checkTarget;

        public boolean checkAsserted;

        public MySecurityManager(Permission target, boolean enable) {
            checkAsserted = false;
            checkTarget = target;
            enableAccess = enable;
        }

        @Override
        public void checkPermission(Permission p) {
            if (p instanceof AuthPermission && checkTarget.equals(p)) {
                checkAsserted = true;
                if (!enableAccess) {
                    throw new SecurityException();
                }
            }
        }

        public MySecurityManager reset() {
            checkAsserted = false;
            return this;
        }
    }
    
}
