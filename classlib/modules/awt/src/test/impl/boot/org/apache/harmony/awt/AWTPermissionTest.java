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
package org.apache.harmony.awt;

import java.awt.AWTPermission;
import java.awt.Frame;
import java.awt.Window;
import java.security.Permission;

import junit.framework.TestCase;

/**
 * Test case for java.awt.AWTPermission
 */
public class AWTPermissionTest extends TestCase {

    SecurityManager originalSM = null;

    MockSecurityManager sm = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        originalSM = System.getSecurityManager();
        sm = new MockSecurityManager();
        System.setSecurityManager(sm);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        System.setSecurityManager(originalSM);
    }

    /**
     * @tests java.awt.AWTPermission#AWTPermission((String) test case for
     *        "accessEventQueue"
     */
    @SuppressWarnings("nls")
    public void test_checkAwtEventQueueAccess() {
        try {
            sm.checkAwtEventQueueAccess();
            fail("Should throw a SecurityException.");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * @tests java.awt.AWTPermission#AWTPermission((String) test case for
     *        "accessClipboard"
     */
    @SuppressWarnings("nls")
    public void test_checkSystemClipboardAccess() {
        try {
            sm.checkSystemClipboardAccess();
            fail("Should throw a SecurityException.");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * @tests java.awt.AWTPermission#AWTPermission((String) test case for
     *        "showWindowWithoutWarningBanner"
     */
    public void test_checkTopLevelWindowLjava_lang_Object() {
        assertFalse(sm.checkTopLevelWindow(new Window(new Frame())));
    }

    /**
     * Sub-SecurityManager to test some actions of AWTPermission.
     */
    class MockSecurityManager extends SecurityManager {
        /**
         * @see java.lang.SecurityManager#checkPermission(java.security.Permission)
         */
        @Override
        @SuppressWarnings("nls")
        public void checkPermission(Permission permission) {
            Permission[] denied = new Permission[] {
                    new AWTPermission("accessEventQueue"),
                    new AWTPermission("accessClipboard"),
                    new AWTPermission("showWindowWithoutWarningBanner") };
            for (Permission per : denied) {
                if (null != per && per.implies(permission)) {
                    throw new SecurityException("Denied " + permission);
                }
            }
        }
    }
}
