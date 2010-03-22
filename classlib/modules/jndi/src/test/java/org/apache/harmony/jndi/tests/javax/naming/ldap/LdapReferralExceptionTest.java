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

package org.apache.harmony.jndi.tests.javax.naming.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapReferralException;

import org.apache.harmony.jndi.tests.javax.naming.util.Log;
import junit.framework.TestCase;

public class LdapReferralExceptionTest extends TestCase {


	private static final Log log = new Log(LdapReferralExceptionTest.class);

	public void testAllCoveragePurpose() throws NamingException {
		log.setMethod("testAllCoveragePurpose()");
		LdapReferralException ex = new MockLdapReferralException();
		ex = new MockLdapReferralException("message");

		ex.getReferralContext();
		ex.getReferralContext(null);
		ex.getReferralContext(null, null);
		ex.getReferralInfo();
		ex.skipReferral();
		ex.retryReferral();
	}

	public static class MockLdapReferralException extends LdapReferralException {
        private static final long serialVersionUID = 1L;

		public MockLdapReferralException() {
			super();
		}

		public MockLdapReferralException(String s) {
			super(s);
		}

		@Override
        public Context getReferralContext() {
			return null;
		}

		@Override
        public Context getReferralContext(Hashtable<?, ?> h) {
			return null;
		}

		@Override
        public Context getReferralContext(Hashtable<?, ?> h, Control[] cs) {
			return null;
		}

		@Override
        public Object getReferralInfo() {
			return null;
		}

		@Override
        public boolean skipReferral() {
			return false;
		}

		@Override
        public void retryReferral() {

		}

	}

}
