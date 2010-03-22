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

package org.apache.harmony.jndi.tests.javax.naming;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.TestCase;
import org.apache.harmony.jndi.tests.javax.naming.util.Log;

public class InitialContextSysTest extends TestCase {
	private final Log log = new Log(InitialContextSysTest.class);

	public void testConstructor_sys() throws NamingException, IOException {
		log.setMethod("testConstructor_sys");
		/*
		 * Set System properties
		 */
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		// C
		System.setProperty("java.naming.factory.control",
				"java.naming.factory.control.sys");
		System.setProperty("java.naming.factory.object",
				"java.naming.factory.object.sys");
		System.setProperty("java.naming.factory.state",
				"java.naming.factory.state.sys");
		System.setProperty("java.naming.factory.url.pkgs",
				"java.naming.factory.url.pkgs.sys");
		// F
		System.setProperty("java.naming.provider.url",
				"java.naming.provider.url.sys");
		System.setProperty("java.naming.authoritative",
				"java.naming.authoritative.sys");
		System.setProperty("java.naming.batchsize",
				"java.naming.batchsize.app1");
		System.setProperty("java.naming.dns.url", "java.naming.dns.url.sys");
		System.setProperty("java.naming.language", "java.naming.language.sys");
		System.setProperty("java.naming.referral", "java.naming.referral.sys");
		System.setProperty("java.naming.security.authentication",
				"java.naming.security.authentication.sys");
		System.setProperty("java.naming.security.credentials",
				"java.naming.security.credentials.sys");
		System.setProperty("java.naming.security.principal",
				"java.naming.security.principal.sys");
		System.setProperty("java.naming.security.protocol",
				"java.naming.security.protocol.sys");

		// other
		System.setProperty("dazzle.jndi.testing.spi.sys",
				"dazzle.jndi.testing.spi.sys.sys");
		// junk
		System.setProperty("sys.type", "sys.type.sys");

		InitialContext context = new InitialContext();
		Hashtable<?, ?> props = context.getEnvironment();
		// printHashtable(props);
		Hashtable<?, ?> expected = InitialContextLibTest.readAllProps(null);
		assertEquals(expected, props);
	}

	void printHashtable(Hashtable<?, ?> env) {
		// TO DO: Need to remove
		Enumeration<?> keys = env.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			log.log(key + "=" + env.get(key));
		}
	}
}
