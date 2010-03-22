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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactoryBuilder;

import junit.framework.TestCase;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockInitialContextFactoryBuilder;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContext;
import org.apache.harmony.jndi.tests.javax.naming.util.Log;

public class NamingManagerBuilderTest extends TestCase {

	/*
	 * ------------------------------------------------------------------- 
	 * Class variables
	 * -------------------------------------------------------------------
	 */

	static Log log = new Log(NamingManagerBuilderTest.class);

	/**
	 * Constructor for NamingManagerBuilderTest.
	 * 
	 * @param arg0
	 */
	public NamingManagerBuilderTest(String arg0) {
		super(arg0);
	}

	/*
	 * -------------------------------------------------------------------
	 * Methods
	 * -------------------------------------------------------------------
	 */

	private void invokeMyTestMethod(String methodName) {
		log.setMethod(methodName);
		try {
			Method m = this.getClass().getMethod(methodName, new Class[0]);
			m.invoke(this, new Object[0]);
			// log.log("Succeeded!");
		} catch (Throwable t) {
			String errMsg = t.getMessage();

			if (t instanceof InvocationTargetException) {
				errMsg = ((InvocationTargetException) t).getTargetException()
						.getMessage();
			}

			fail("Failed: " + t.getClass().getName() + " - " + errMsg);
		}
	}

	/**
	 * Test the normal condition when factory builder is properly set.
	 */
	public void myTestGetInitialContext_HasBuilder_Normal()
			throws NamingException {
		log.setMethod("myTestGetInitialContext_HasBuilder_Normal");

		Context context = NamingManager.getInitialContext(null);
		assertTrue(context instanceof MockContext);
		assertEquals(context, new MockContext(null));

		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		context = NamingManager.getInitialContext(env);
		assertTrue(context instanceof MockContext);
		assertEquals(context, new MockContext(env));
	}

	/**
	 * Test the behavior when factory builder throws NullPointerException.
	 */
	public void myTestGetInitialContext_HasBuilder_BuilderNullPointerException()
			throws NamingException {
		log
				.setMethod("myTestGetInitialContext_HasBuilder_BuilderNullPointerException");
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		NamingManagerTest.indicateNullPointerException(env, 1);
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		try {
			NamingManager.getInitialContext(env);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
			// log.log(e);
		}
	}

	/**
	 * Test the behavior when factory builder throws NamingException.
	 */
	public void myTestGetInitialContext_HasBuilder_BuilderNamingException()
			throws NamingException {
		log
				.setMethod("myTestGetInitialContext_HasBuilder_BuilderNamingException");
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		NamingManagerTest.indicateNamingException(env, 1);
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		try {
			NamingManager.getInitialContext(env);
			fail("Should throw NamingException.");
		} catch (NamingException e) {
			// log.log(e);
		}
	}

	/**
	 * Test the behavior when factory throws RuntimeException.
	 */
	public void myTestGetInitialContext_HasBuilder_FactoryRuntimeException()
			throws NamingException {
		log
				.setMethod("myTestGetInitialContext_HasBuilder_FactoryRuntimeException");
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		NamingManagerTest.indicateRuntimeException(env, 2);
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		try {
			NamingManager.getInitialContext(env);
			fail("Should throw RuntimeException.");
		} catch (RuntimeException e) {
			// log.log(e);
		}
	}

	/**
	 * Test the behavior when factory throws NamingException.
	 */
	public void myTestGetInitialContext_HasBuilder_FactoryNamingException()
			throws NamingException {
		log
				.setMethod("myTestGetInitialContext_HasBuilder_FactoryNamingException");
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		NamingManagerTest.indicateNamingException(env, 2);
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		try {
			NamingManager.getInitialContext(env);
			fail("Should throw NamingException.");
		} catch (NamingException e) {
			// log.log(e);
		}
	}

	/**
	 * Test the behavior when factory builder is set but the factory builder
	 * returns null.
	 */
	public void myTestGetInitialContext_HasBuilder_BuilderReturnNull()
			throws NamingException {
		log.setMethod("myTestGetInitialContext_HasBuilder_BuilderReturnNull");
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		NamingManagerTest.indicateReturnNull(env, 1);
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		try {
			NamingManager.getInitialContext(env);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
			// log.log(e);
		}
	}

	/**
	 * Test the behavior when factory builder is set but the factory returns
	 * null.
	 */
	public void myTestGetInitialContext_HasBuilder_FactoryReturnNull()
			throws NamingException {
		log.setMethod("myTestGetInitialContext_HasBuilder_FactoryReturnNull");
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		NamingManagerTest.indicateReturnNull(env, 2);
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		Context context = NamingManager.getInitialContext(env);
		assertNull(context);
	}

	/**
	 * Before the initial context factory builder is set.
	 * 
	 */
	public void myTestSetInitialContextFactoryBuilder_NotSet() {
		log.setMethod("myTestSetInitialContextFactoryBuilder_NotSet");
		assertFalse(NamingManager.hasInitialContextFactoryBuilder());
	}

	/**
	 * Set the initial context factory builder to null.
	 * 
	 */
	public void myTestSetInitialContextFactoryBuilder_SetNull()
			throws NamingException {
		log.setMethod("myTestSetInitialContextFactoryBuilder_SetNull");
		NamingManager.setInitialContextFactoryBuilder(null);
		assertFalse(NamingManager.hasInitialContextFactoryBuilder());
	}

	/**
	 * Set the initial context factory builder to a mock instance.
	 * 
	 */
	public void myTestSetInitialContextFactoryBuilder_SetNormal()
			throws NamingException {
		log.setMethod("myTestSetInitialContextFactoryBuilder_SetNormal");
		InitialContextFactoryBuilder contextFactoryBuilder = MockInitialContextFactoryBuilder
				.getInstance();
		NamingManager.setInitialContextFactoryBuilder(contextFactoryBuilder);
		assertTrue(NamingManager.hasInitialContextFactoryBuilder());
	}

	/**
	 * Reset the initial context factory builder to another mock instance.
	 * 
	 */
	public void myTestSetInitialContextFactoryBuilder_ResetNormal()
			throws NamingException {
		log.setMethod("myTestSetInitialContextFactoryBuilder_ResetNormal");
		try {
			NamingManager
					.setInitialContextFactoryBuilder(new MockInitialContextFactoryBuilder());
			fail("Reset initialContextFactoryBuilder is forbidden!");
		} catch (IllegalStateException e) {
		}
	}

	/**
	 * Reset the initial context factory builder to the same mock instance.
	 * 
	 */
	public void myTestSetInitialContextFactoryBuilder_ResetSame()
			throws NamingException {
		log.setMethod("myTestSetInitialContextFactoryBuilder_ResetSame");
		try {
			NamingManager
					.setInitialContextFactoryBuilder(MockInitialContextFactoryBuilder
							.getInstance());
			fail("Reset initialContextFactoryBuilder is forbidden!");
		} catch (IllegalStateException e) {
		}
	}

	/**
	 * Reset the initial context factory builder to null.
	 * 
	 */
	public void myTestSetInitialContextFactoryBuilder_ResetNull()
			throws NamingException {
		log.setMethod("myTestSetInitialContextFactoryBuilder_ResetNull");
		try {
			NamingManager.setInitialContextFactoryBuilder(null);
			fail("Reset initialContextFactoryBuilder to null is forbidden!");
		} catch (IllegalStateException e) {
		}
	}

	/**
	 * Test the normal condition when factory builder is properly set.
	 */
	public void myTestGetObjectInstance_HasBuilder_Normal() throws Exception {
		log.setMethod("myTestGetObjectInstance_HasBuilder_Normal");
		Object obj = NamingManager.getObjectInstance(null, null, null, null);
		assertEquals(new NamingManagerTest.MockObject(null, null, null, null),
				obj);

		obj = NamingManager.getObjectInstance("String", null, null, null);
		assertEquals(new NamingManagerTest.MockObject("String", null, null,
				null), obj);

		Reference r = new Reference(
				null,
				"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactoryNoException",
				null);
		obj = NamingManager.getObjectInstance(r, null, null, null);
		assertEquals(new NamingManagerTest.MockObject(r, null, null, null), obj);

		obj = NamingManager.getObjectInstance(null, new CompositeName(
				"compositename"), null, null);
		assertEquals(new NamingManagerTest.MockObject(null, new CompositeName(
				"compositename"), null, null), obj);

		NamingManagerTest.MockContext cxt = new NamingManagerTest.MockContext(
				null);
		obj = NamingManager.getObjectInstance(null, null, cxt, null);
		assertEquals(new NamingManagerTest.MockObject(null, null, cxt, null),
				obj);

		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		env
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactoryNoException");
		obj = NamingManager.getObjectInstance(null, null, null, env);
		assertEquals(new NamingManagerTest.MockObject(null, null, null, env),
				obj);
	}

	/**
	 * Test the behavior when factory builder throws NullPointerException.
	 */
	public void myTestGetObjectInstance_HasBuilder_BuilderNullPointerException()
			throws Exception {
		log
				.setMethod("myTestGetObjectInstance_HasBuilder_BuilderNullPointerException");
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		NamingManagerTest.indicateNullPointerException(env, 1);
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		env
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactoryNoException");
		try {
			NamingManager.getObjectInstance(null, null, null, env);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
			// log.log(e);
		}
	}

	/**
	 * Test the behavior when factory builder throws NamingException.
	 */
	public void myTestGetObjectInstance_HasBuilder_BuilderNamingException()
			throws Exception {
		log
				.setMethod("myTestGetObjectInstance_HasBuilder_BuilderNamingException");
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		NamingManagerTest.indicateNamingException(env, 1);
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		env
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactoryNoException");
		try {
			NamingManager.getObjectInstance(null, null, null, env);
			fail("Should throw NamingException.");
		} catch (NamingException e) {
			// log.log(e);
		}
	}

	/**
	 * Test the behavior when factory throws RuntimeException.
	 */
	public void myTestGetObjectInstance_HasBuilder_FactoryRuntimeException()
			throws Exception {
		log
				.setMethod("myTestGetObjectInstance_HasBuilder_FactoryRuntimeException");
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		NamingManagerTest.indicateRuntimeException(env, 2);
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		env
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactoryNoException");
		try {
			NamingManager.getObjectInstance(null, null, null, env);
			fail("Should throw RuntimeException.");
		} catch (RuntimeException e) {
			// log.log(e);
		}
	}

	/**
	 * Test the behavior when factory throws NamingException.
	 */
	public void myTestGetObjectInstance_HasBuilder_FactoryNamingException()
			throws Exception {
		log
				.setMethod("myTestGetObjectInstance_HasBuilder_FactoryNamingException");
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		NamingManagerTest.indicateNamingException(env, 2);
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		env
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactoryNoException");
		try {
			NamingManager.getObjectInstance(null, null, null, env);
			fail("Should throw NamingException.");
		} catch (NamingException e) {
			// log.log(e);
		}
	}

	/**
	 * Test the behavior when factory builder is set but the factory builder
	 * returns null.
	 */
	public void myTestGetObjectInstance_HasBuilder_BuilderReturnNull()
			throws Exception {
		log.setMethod("myTestGetObjectInstance_HasBuilder_BuilderReturnNull");
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		NamingManagerTest.indicateReturnNull(env, 1);
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		env
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactoryNoException");
		try {
			NamingManager.getObjectInstance(null, null, null, env);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
			// log.log(e);
		}
	}

	/**
	 * Test the behavior when factory builder is set but the factory returns
	 * null.
	 */
	public void myTestGetObjectInstance_HasBuilder_FactoryReturnNull()
			throws Exception {
		log.setMethod("myTestGetObjectInstance_HasBuilder_FactoryReturnNull");
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		NamingManagerTest.indicateReturnNull(env, 2);
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		env
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactoryNoException");
		Object obj = NamingManager.getObjectInstance("string", null, null, env);
		assertNull(obj);
	}

	/**
	 * Set the object factory builder to null.
	 * 
	 */
	public void myTestSetObjectFactoryBuilder_SetNull() throws NamingException {
		log.setMethod("myTestSetObjectFactoryBuilder_SetNull");
		NamingManager.setObjectFactoryBuilder(null);
	}

	/**
	 * Set the object factory builder to a mock instance.
	 * 
	 */
	public void myTestSetObjectFactoryBuilder_SetNormal()
			throws NamingException {
		log.setMethod("myTestSetInitialContextFactoryBuilder_SetNormal");
		ObjectFactoryBuilder objectFactoryBuilder = NamingManagerTest.MockObjectFactoryBuilder
				.getInstance();
		NamingManager.setObjectFactoryBuilder(objectFactoryBuilder);
	}

	/**
	 * Reset the object factory builder to another mock instance.
	 * 
	 */
	public void myTestSetObjectFactoryBuilder_ResetNormal()
			throws NamingException {
		log.setMethod("myTestSetObjectFactoryBuilder_ResetNormal");
		try {
			NamingManager
					.setObjectFactoryBuilder(new NamingManagerTest.MockObjectFactoryBuilder());
			fail("Reset ObjectFactoryBuilder is forbidden!");
		} catch (IllegalStateException e) {
		}
	}

	/**
	 * Reset the object factory builder to the same mock instance.
	 * 
	 */
	public void myTestSetObjectFactoryBuilder_ResetSame()
			throws NamingException {
		log.setMethod("myTestSetObjectFactoryBuilder_ResetSame");
		try {
			NamingManager
					.setObjectFactoryBuilder(NamingManagerTest.MockObjectFactoryBuilder
							.getInstance());
			fail("Reset ObjectFactoryBuilder is forbidden!");
		} catch (IllegalStateException e) {
		}
	}

	/**
	 * Reset the object factory builder to null.
	 * 
	 */
	public void myTestSetObjectFactoryBuilder_ResetNull()
			throws NamingException {
		log.setMethod("myTestSetObjectFactoryBuilder_ResetNull");
		try {
			NamingManager.setObjectFactoryBuilder(null);
			fail("Reset ObjectFactoryBuilder to null is forbidden!");
		} catch (IllegalStateException e) {
		}
	}

	public void testSetInitialContextFactoryBuilder_AfterSet() {
		// not set builder yet
		// myTestSetInitialContextFactoryBuilder_NotSet();
		invokeMyTestMethod("myTestSetInitialContextFactoryBuilder_NotSet");

		// set builder as mock builder instance
		invokeMyTestMethod("myTestSetInitialContextFactoryBuilder_SetNull");
		// myTestSetInitialContextFactoryBuilder_SetNormal();
		invokeMyTestMethod("myTestSetInitialContextFactoryBuilder_SetNormal");

		// myTestGetInitialContext_HasBuilder_Normal();
		invokeMyTestMethod("myTestGetInitialContext_HasBuilder_Normal");
		// myTestGetInitialContext_HasBuilder_BuilderNullPointerException();
		// invokeMyTestMethod("myTestGetInitialContext_HasBuilder_BuilderNullPointerException");
		// myTestGetInitialContext_HasBuilder_BuilderNamingException();
		// invokeMyTestMethod("myTestGetInitialContext_HasBuilder_BuilderNamingException");
		// myTestGetInitialContext_HasBuilder_FactoryRuntimeException();
		// invokeMyTestMethod("myTestGetInitialContext_HasBuilder_FactoryRuntimeException");
		// myTestGetInitialContext_HasBuilder_FactoryNamingException();
		// invokeMyTestMethod("myTestGetInitialContext_HasBuilder_FactoryNamingException");
		// myTestGetInitialContext_HasBuilder_BuilderReturnNull();
		// invokeMyTestMethod("myTestGetInitialContext_HasBuilder_BuilderReturnNull");
		// myTestGetInitialContext_HasBuilder_FactoryReturnNull();
		// invokeMyTestMethod("myTestGetInitialContext_HasBuilder_FactoryReturnNull");

		// try to reset builder to another instance
		// myTestSetInitialContextFactoryBuilder_ResetNormal();
		// invokeMyTestMethod("myTestSetInitialContextFactoryBuilder_ResetNormal");

		// try to reset builder to the same instance as before
		// myTestSetInitialContextFactoryBuilder_ResetSame();
		// invokeMyTestMethod("myTestSetInitialContextFactoryBuilder_ResetSame");

		// try to reset to null
		// myTestSetInitialContextFactoryBuilder_ResetNull();
		// invokeMyTestMethod("myTestSetInitialContextFactoryBuilder_ResetNull");
	}

	public void testSetObjectFactoryBuilder_AfterSet() {
		// set builder as mock builder instance
		invokeMyTestMethod("myTestSetObjectFactoryBuilder_SetNull");
		// myTestSetObjectFactoryBuilder_SetNormal();
		invokeMyTestMethod("myTestSetObjectFactoryBuilder_SetNormal");

		// myTestGetObjectInstance_HasBuilder_Normal();
		invokeMyTestMethod("myTestGetObjectInstance_HasBuilder_Normal");
		// myTestGetObjectInstance_HasBuilder_BuilderNullPointerException();
		invokeMyTestMethod("myTestGetObjectInstance_HasBuilder_BuilderNullPointerException");
		// myTestGetObjectInstance_HasBuilder_BuilderNamingException();
		invokeMyTestMethod("myTestGetObjectInstance_HasBuilder_BuilderNamingException");
		// myTestGetObjectInstance_HasBuilder_FactoryRuntimeException();
		invokeMyTestMethod("myTestGetObjectInstance_HasBuilder_FactoryRuntimeException");
		// myTestGetObjectInstance_HasBuilder_FactoryNamingException();
		invokeMyTestMethod("myTestGetObjectInstance_HasBuilder_FactoryNamingException");
		// myTestGetObjectInstance_HasBuilder_BuilderReturnNull();
		invokeMyTestMethod("myTestGetObjectInstance_HasBuilder_BuilderReturnNull");
		// myTestGetObjectInstance_HasBuilder_FactoryReturnNull();
		invokeMyTestMethod("myTestGetObjectInstance_HasBuilder_FactoryReturnNull");

		// try to reset builder to another instance
		// myTestSetObjectFactoryBuilder_ResetNormal();
		invokeMyTestMethod("myTestSetObjectFactoryBuilder_ResetNormal");

		// try to reset builder to the same instance as before
		// myTestSetObjectFactoryBuilder_ResetSame();
		invokeMyTestMethod("myTestSetObjectFactoryBuilder_ResetSame");

		// try to reset to null
		// myTestSetObjectFactoryBuilder_ResetNull();
		invokeMyTestMethod("myTestSetObjectFactoryBuilder_ResetNull");
	}

}
