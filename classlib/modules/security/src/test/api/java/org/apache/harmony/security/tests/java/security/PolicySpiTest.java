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
package org.apache.harmony.security.tests.java.security;

import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.PolicySpi;
import java.security.ProtectionDomain;

import junit.framework.TestCase;

public class PolicySpiTest extends TestCase {

	class MockPolicySpi extends PolicySpi {
		
		@Override
		protected boolean engineImplies(ProtectionDomain domain,
				Permission permission) {
			return false;
		}
		
		public PermissionCollection testEngineGetPermissionsCodeSource() {
			return super.engineGetPermissions((CodeSource) null);
		}
		
		public PermissionCollection testEngineGetPermissionsProtectionDomain() {
			return super.engineGetPermissions((ProtectionDomain) null);
		}
        
        public void engineRefresh() {
            //do nothing.
            return;
        }

		
	}
	
	/**
	 * @tests java.security.PolicySpi#engineGetPermissions(CodeSource)
	 * @since 1.6
	 */
	public void test_engineGetPermissions_LCodeSource() {
		MockPolicySpi spi = new MockPolicySpi();
		assertSame(Policy.UNSUPPORTED_EMPTY_COLLECTION, spi
				.testEngineGetPermissionsCodeSource());
	}

	/**
	 * @tests java.security.PolicySpi#engineGetPermissions(ProtectionDomain)
	 * @since 1.6
	 */
	public void test_engineGetPermissions_LProtectionDomain() {
		MockPolicySpi spi = new MockPolicySpi();
        assertSame(Policy.UNSUPPORTED_EMPTY_COLLECTION, spi
                .testEngineGetPermissionsProtectionDomain());
	}
    
    /**
     * @tests java.security.PolicySpi#engineRefresh()
     * @since 1.6
     */
    public void test_engineRefresh()
    {
        MockPolicySpi spi = new MockPolicySpi();
        assertSame(Policy.UNSUPPORTED_EMPTY_COLLECTION, spi
                .testEngineGetPermissionsCodeSource());
        assertSame(Policy.UNSUPPORTED_EMPTY_COLLECTION, spi
                .testEngineGetPermissionsCodeSource());

        // Nothing should be done according to java doc.
        spi.engineRefresh();
        assertSame(Policy.UNSUPPORTED_EMPTY_COLLECTION, spi
                .testEngineGetPermissionsCodeSource());
        assertSame(Policy.UNSUPPORTED_EMPTY_COLLECTION, spi
                .testEngineGetPermissionsCodeSource());        
    }

}
