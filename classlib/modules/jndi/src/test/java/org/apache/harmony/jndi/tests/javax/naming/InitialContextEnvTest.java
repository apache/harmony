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

public class InitialContextEnvTest extends TestCase {
	private static final Log log = new Log(InitialContextEnvTest.class);

	@Override
    protected void tearDown() throws Exception {
		super.tearDown();
		Log.close();
	}

	public void testConstructor_env() throws NamingException, IOException {
		log.setMethod("testConstructor_env");
		/*
		 * set properties for environment parameter
		 */
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		// C type
		env.put("java.naming.factory.control",
				"java.naming.factory.control.env");
		env.put("java.naming.factory.object", "java.naming.factory.object.env");
		env.put("java.naming.factory.state", "java.naming.factory.state.env");
		env.put("java.naming.factory.url.pkgs",
				"java.naming.factory.url.pkgs.env");
		// F
		env.put("java.naming.provider.url", "java.naming.provider.url.env");
		env.put("java.naming.authoritative", "java.naming.authoritative.env");
		env.put("java.naming.batchsize", "java.naming.batchsize.app1");
		env.put("java.naming.dns.url", "java.naming.dns.url.env");
		env.put("java.naming.language", "java.naming.language.env");
		env.put("java.naming.referral", "java.naming.referral.env");
		env.put("java.naming.security.authentication",
				"java.naming.security.authentication.env");
		env.put("java.naming.security.credentials",
				"java.naming.security.credentials.env");
		env.put("java.naming.security.principal",
				"java.naming.security.principal.env");
		env.put("java.naming.security.protocol",
				"java.naming.security.protocol.env");

		// other
		env.put("dazzle.jndi.testing.spi.env",
				"dazzle.jndi.testing.spi.env.env");
		// junk
		env.put("env.type", "env.type.env");

		/*
		 * Set System properties
		 */
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
				"org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContextFactory");
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

		InitialContext context = new InitialContext(env);

		Hashtable<?, ?> props = context.getEnvironment();
		// printHashtable(props);
		Hashtable<?, ?> expected = InitialContextLibTest.readAllProps(env);
		assertEquals(expected, props);
	}

	public void testConstructor_env_Ctype() throws NamingException, IOException {
		log.setMethod("testConstructor_env_Ctype");
		/*
		 * set properties for environment parameter
		 */
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		// C type
		env.put("java.naming.factory.control",
				"java.naming.factory.control.env");
		// env.put("java.naming.factory.object",
		// "java.naming.factory.object.env");
		// env.put("java.naming.factory.state",
		// "java.naming.factory.state.env");
		env.put("java.naming.factory.url.pkgs",
				"java.naming.factory.url.pkgs.env");
		// F
		env.put("java.naming.provider.url", "java.naming.provider.url.env");
		env.put("java.naming.authoritative", "java.naming.authoritative.env");
		env.put("java.naming.batchsize", "java.naming.batchsize.app1");
		env.put("java.naming.dns.url", "java.naming.dns.url.env");
		env.put("java.naming.language", "java.naming.language.env");
		env.put("java.naming.referral", "java.naming.referral.env");
		env.put("java.naming.security.authentication",
				"java.naming.security.authentication.env");
		env.put("java.naming.security.credentials",
				"java.naming.security.credentials.env");
		env.put("java.naming.security.principal",
				"java.naming.security.principal.env");
		env.put("java.naming.security.protocol",
				"java.naming.security.protocol.env");

		// other
		env.put("dazzle.jndi.testing.spi.env",
				"dazzle.jndi.testing.spi.env.env");
		// junk
		env.put("env.type", "env.type.env");

		/*
		 * Set System properties
		 */
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
				"org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContextFactory");
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

		InitialContext context = new InitialContext(env);

		Hashtable<?, ?> props = context.getEnvironment();
		// printHashtable(props);
		Hashtable<?, ?> expected = InitialContextLibTest.readAllProps(env);
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
