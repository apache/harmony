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

package org.apache.harmony.jndi.tests.javax.naming.spi;

import java.util.Hashtable;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;

import org.apache.harmony.jndi.tests.javax.naming.spi.mock.InvokeRecord;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockInitialContextFactoryBuilder;

import junit.framework.TestCase;

// import util.Log;

public class NamingManagerExploreTest extends TestCase {
	// static Log log = new Log(NamingManagerTestExplore.class);

	public void testFactoryBuilder() throws IllegalStateException,
			SecurityException, NamingException {
		// log.setMethod("testFactoryBuilder");

		if (!NamingManager.hasInitialContextFactoryBuilder()) {
			InitialContextFactoryBuilder contextFactoryBuilder = MockInitialContextFactoryBuilder
					.getInstance();
			NamingManager
					.setInitialContextFactoryBuilder(contextFactoryBuilder);
		}

		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		env.put(Context.URL_PKG_PREFIXES, "org.apache.harmony.jndi.tests.javax.naming.spi.mock");

		MyInitialContext context = new MyInitialContext(env);
		// log.log(context.getEnvironment().toString());
		// log.log("DefaultContext:" +
		// context.getDefaultContext().getClass().getName());
		//
		Context urlContext = NamingManager.getURLContext("http", env);
		assertEquals("http", urlContext.getEnvironment().get("url.schema"));

		String name = "http://www.apache.org";
		String obj = "String object";
		context.bind(name, obj);

		assertNull(InvokeRecord.getLatestUrlSchema());
	}

	public void testFactoryBuilder_name() throws IllegalStateException,
			SecurityException, NamingException {
		// log.setMethod("testFactoryBuilder_name");

		if (!NamingManager.hasInitialContextFactoryBuilder()) {
			InitialContextFactoryBuilder contextFactoryBuilder = MockInitialContextFactoryBuilder
					.getInstance();
			NamingManager
					.setInitialContextFactoryBuilder(contextFactoryBuilder);
		}

		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		env.put(Context.URL_PKG_PREFIXES, "org.apache.harmony.jndi.tests.javax.naming.spi.mock");

		MyInitialContext context = new MyInitialContext(env);
		// log.log(context.getEnvironment().toString());
		// log.log("DefaultContext:" +
		// context.getDefaultContext().getClass().getName());
		//
		Context urlContext = NamingManager.getURLContext("http", env);
		assertEquals("http", urlContext.getEnvironment().get("url.schema"));

		Name name = new CompositeName("http://www.apache.org");
		String obj = "Name object";
		context.bind(name, obj);

		assertNull(InvokeRecord.getLatestUrlSchema());
	}

	class MyInitialContext extends InitialContext {

		public MyInitialContext(Hashtable<?, ?> environment) throws NamingException {
			super(environment);
		}

		public Context getDefaultContext() {
			return this.defaultInitCtx;
		}
	}
}
