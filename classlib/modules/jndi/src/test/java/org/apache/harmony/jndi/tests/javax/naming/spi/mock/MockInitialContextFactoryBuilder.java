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
package org.apache.harmony.jndi.tests.javax.naming.spi.mock;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

import org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest;

/*
 * ------------------------------------------- 
 * inner class for mock object
 * -------------------------------------------
 */
public class MockInitialContextFactoryBuilder implements
		InitialContextFactoryBuilder {

	private static final MockInitialContextFactoryBuilder _builder = new MockInitialContextFactoryBuilder();

	public static MockInitialContextFactoryBuilder getInstance() {
		return MockInitialContextFactoryBuilder._builder;
	}

	public InitialContextFactory createInitialContextFactory(Hashtable<?,?> envmt)
			throws NamingException {
		NamingManagerTest.issueIndicatedExceptions(envmt);
		if (NamingManagerTest.returnNullIndicated(envmt)) {
			return null;
		}
		return new MockInitialContextFactory(envmt);
	}

	public static class MockInitialContextFactory implements
			InitialContextFactory {
		public Context getInitialContext(Hashtable<?, ?> envmt)
				throws NamingException {
			NamingManagerTest.issueIndicatedExceptions(envmt);
			if (NamingManagerTest.returnNullIndicated(envmt)) {
				return null;
			}
			return new MockContext(envmt);
		}

		public MockInitialContextFactory(Hashtable<?,?> envmt) {
		}

		public MockInitialContextFactory() {
		}
	}
}