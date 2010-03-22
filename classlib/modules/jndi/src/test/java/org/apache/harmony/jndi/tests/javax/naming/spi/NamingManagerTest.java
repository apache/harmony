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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Binding;
import javax.naming.CannotProceedException;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;
import javax.naming.spi.StateFactory;

import junit.framework.TestCase;

import org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirContext;
import org.apache.harmony.jndi.tests.javax.naming.util.Log;

import com.sun.jndi.url.nntp.nntpURLContextFactory;

import dazzle.jndi.testing.spi.DazzleContext;

public class NamingManagerTest extends TestCase {

	/*
	 * ------------------------------------------------------------------- 
	 * Class variables
	 * -------------------------------------------------------------------
	 */

	static Log log = new Log(NamingManagerTest.class);

	// Name of the property indicating a RuntimeException should be simulated.
	private static final String INDICATION_RUNTIME_EXCEPTION = "indication.RuntimeException";

	// Name of the property indicating a NullPointerException should be
	// simulated.
	private static final String INDICATION_NULL_POINTER_EXCEPTION = "indication.NullPointerException";

	// Name of the property indicating a NamingException should be simulated.
	private static final String INDICATION_NAMING_EXCEPTION = "indication.NamingException";

	// Name of the property indicating a null should be returned.
	private static final String INDICATION_RETURN_NULL = "indication.returnNull";

	/*
	 * -------------------------------------------------------------------
	 * Instance variables (Should be private)
	 * -------------------------------------------------------------------
	 */
	Hashtable<String, String> props1 = new Hashtable<String, String>();

	/**
	 * Constructor for NamingManagerTest.
	 * 
	 * @param arg0
	 */
	public NamingManagerTest(String arg0) {
		super(arg0);
	}

	@Override
    protected void setUp() throws Exception {
		super.setUp();

		props1.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		props1
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockStateFactory");

		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MagicMockStateFactory");
		writeProviderResource(MockContext.class.getName(), new Hashtable<String, Object>());
		writeProviderResource(MockDirContext.class.getName(), env);
	}

	@Override
    protected void tearDown() throws Exception {
		super.tearDown();
		deleteProviderResource(MockContext.class.getName());
		deleteProviderResource(MockDirContext.class.getName());
	}

	@SuppressWarnings("unchecked")
    public static boolean actionIndicated(Hashtable<?, ?> env, String indicationName) {
		if (null == env) {
			return false;
		}
        Integer occurLevel = (Integer) env.get(indicationName);
        if (null != occurLevel) {
        	if (occurLevel.intValue() <= 1) {
        		env.remove(indicationName);
        		return true;
        	}
            ((Hashtable<Object, Object>)env).put(indicationName, new Integer(
            		occurLevel.intValue() - 1));
            return false;
        }
        return false;
	}

	public static void indicateReturnNull(Hashtable<Object, Object> env) {
		indicateReturnNull(env, 1);
	}

	public static void indicateReturnNull(Hashtable<Object, Object> env, int occurLevel) {
		env.put(INDICATION_RETURN_NULL, new Integer(occurLevel));
	}

	public static boolean returnNullIndicated(Hashtable<?, ?> env) {
		return actionIndicated(env, INDICATION_RETURN_NULL);
	}

	public static void indicateRuntimeException(Hashtable<Object, Object> env) {
		indicateRuntimeException(env, 1);
	}

	public static void indicateRuntimeException(Hashtable<Object, Object> env, int occurLevel) {
		env.put(INDICATION_RUNTIME_EXCEPTION, new Integer(occurLevel));
	}

	public static boolean runtimeExceptionIndicated(Hashtable<?, ?> env) {
		return actionIndicated(env, INDICATION_RUNTIME_EXCEPTION);
	}

	public static void indicateNullPointerException(Hashtable<?, ?> env) {
		indicateNullPointerException(env, 1);
	}

	@SuppressWarnings("unchecked")
    public static void indicateNullPointerException(Hashtable<?, ?> env,
			int occurLevel) {
		((Hashtable<Object, Object>)env).put(INDICATION_NULL_POINTER_EXCEPTION, new Integer(occurLevel));
	}

	public static boolean nullPointerExceptionIndicated(Hashtable<?, ?> env) {
		return actionIndicated(env, INDICATION_NULL_POINTER_EXCEPTION);
	}

	public static void indicateNamingException(Hashtable<?, ?> env) {
		indicateNamingException(env, 1);
	}

	@SuppressWarnings("unchecked")
    public static void indicateNamingException(Hashtable<?, ?> env, int occurLevel) {
        ((Hashtable<Object, Object>)env).put(INDICATION_NAMING_EXCEPTION, new Integer(occurLevel));
	}

	public static boolean namingExceptionIndicated(Hashtable<?, ?> env) {
		return actionIndicated(env, INDICATION_NAMING_EXCEPTION);
	}

	public static void issueIndicatedExceptions(Hashtable<?, ?> env)
			throws NamingException {
		if (nullPointerExceptionIndicated(env)) {
			throw new NullPointerException("Simulated NullPointerException.");
		} else if (namingExceptionIndicated(env)) {
			throw new NamingException("Simulated NamingException.");
		} else if (runtimeExceptionIndicated(env)) {
			throw new RuntimeException("Simulated RuntimeException.");
		}
	}

	/*
	 * -------------------------------------------------------------------
	 * Methods
	 * -------------------------------------------------------------------
	 */

	// public void testDefaultConstructor() {
	// log.setMethod("testDefaultConstructor()");
	// // for coverage only, no meaning at all!
	// try {
	// NamingManager manager = new NamingManager();
	// } catch (Throwable t) {
	// }
	// }
	/**
	 * Test the normal condition when the class name is properly set before
	 * builder is set.
	 */
	public void testGetInitialContext_NoBuilder_Normal() throws NamingException {
		log.setMethod("testGetInitialContext_NoBuilder_Normal()");
		Context context = NamingManager.getInitialContext(props1);
		assertTrue(context instanceof DazzleContext);
	}

	/**
	 * Test the behavior when the class name is null before factory builder is
	 * set.
	 */
	public void testGetInitialContext_NoBuilder_NullFactory()
			throws NamingException {
		log.setMethod("testGetInitialContext_NoBuilder_NullFactory()");
		Hashtable<String, String> envWithNoFac = new Hashtable<String, String>();
		try {
			NamingManager.getInitialContext(envWithNoFac);
			fail("Should throw NoInitialContextException.");
		} catch (NoInitialContextException e) {
		}
	}

	/**
	 * Test the behavior when the class name is empty before factory builder is
	 * set.
	 */
	public void testGetInitialContext_NoBuilder_EmptyFactory()
			throws NamingException {
		log.setMethod("testGetInitialContext_NoBuilder_EmptyFactory()");
		Hashtable<String, String> envWithEmptyFac = new Hashtable<String, String>();
		envWithEmptyFac.put(Context.INITIAL_CONTEXT_FACTORY, "");
		try {
			NamingManager.getInitialContext(envWithEmptyFac);
			fail("Should throw NoInitialContextException.");
		} catch (NoInitialContextException e) {
		}
	}

	/**
	 * Test the behavior when the class name is invalid before factory builder
	 * is set.
	 */
	public void testGetInitialContext_NoBuilder_InvalidFactory()
			throws NamingException {
		log.setMethod("testGetInitialContext_NoBuilder_InvalidFactory()");
		Hashtable<String, String> envWithInvalidFac = new Hashtable<String, String>();
		envWithInvalidFac.put(Context.INITIAL_CONTEXT_FACTORY, "junk.Factory");
		try {
            NamingManager.getInitialContext(envWithInvalidFac);
            fail("Should throw NoInitialContextException.");
        } catch (NoInitialContextException e) {
        }
	}

	/**
	 * When no factory builder is set and the fed object is Reference with a
	 * valid factory name which works properly. Should return an object
	 * successfully.
	 * 
	 * Try the same when the fed object is Referenceable.
	 */
	public void testGetObjectInstance_NoBuilder_ReferenceValidFactory()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_ReferenceValidFactory()");
		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		Reference r = new Reference(
				null,
				"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactory",
				null);
		Object obj = NamingManager.getObjectInstance(r, null, null, env);

		// if (null != obj) {
		// log.log(obj.toString());
		// } else {
		// log.log("Null object returned!");
		// }
		assertEquals(new MockObject(r, null, null, env), obj);

		// test Referenceable
		MockReferenceable mr = new MockReferenceable(r);
		obj = NamingManager.getObjectInstance(mr, null, null, env);
		assertEquals(new MockObject(r, null, null, env), obj);
	}

	/**
	 * When no factory builder is set and the fed object is Reference with an
	 * invalid factory name. Should return the original object.
	 * 
	 * Try the same when the fed object is Referenceable.
	 */
	public void testGetObjectInstance_NoBuilder_ReferenceInvalidFactory()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_ReferenceInvalidFactory()");
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		Reference r = new Reference(null, "junk.factory", null);
		Object obj = NamingManager.getObjectInstance(r, null, null, env);
		assertSame(r, obj);

		// test Referenceable
		MockReferenceable mr = new MockReferenceable(r);
		obj = NamingManager.getObjectInstance(mr, null, null, env);
		assertSame(mr, obj);
	}

	/**
	 * When no factory builder is set and the fed object is Reference with a
	 * valid factory name but the factory fails to create an object. Should
	 * throw the exception.
	 * 
	 * Try the same when the fed object is Referenceable.
	 */
	public void testGetObjectInstance_NoBuilder_ReferenceException()
			throws Exception {
		log.setMethod("testGetObjectInstance_NoBuilder_ReferenceException()");
		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		Reference r = new Reference(
				null,
				"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactory",
				null);
		indicateNullPointerException(env);
		try {
			NamingManager.getObjectInstance(r, null, null, env);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
			// log.log(e);
		}

		// test Referenceable
		MockReferenceable mr = new MockReferenceable(r);
		indicateNamingException(env);
		try {
			NamingManager.getObjectInstance(mr, null, null, env);
			fail("Should throw NamingException.");
		} catch (NamingException e) {
			// log.log(e);
		}
	}

	/**
	 * When no factory builder is set and the fed object is Reference with a
	 * valid factory name but the factory returns null. Should return null.
	 * 
	 * Try the same when the fed object is Referenceable.
	 */
	public void testGetObjectInstance_NoBuilder_ReferenceReturnNull()
			throws Exception {
		log.setMethod("testGetObjectInstance_NoBuilder_ReferenceReturnNull()");
		Hashtable<Object, Object> env = new Hashtable<Object, Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		Reference r = new Reference(
				null,
				"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactory",
				null);
		indicateReturnNull(env);
		Object obj = NamingManager.getObjectInstance(r, null, null, env);
		assertNull(obj);

		// test Referenceable
		MockReferenceable mr = new MockReferenceable(r);
		indicateReturnNull(env);
		obj = NamingManager.getObjectInstance(mr, null, null, env);
		assertNull(obj);
	}

	/**
	 * When no factory builder is set and the fed object is Reference with no
	 * factory name, and there are one MockRefAddr which contains a valid URL
	 * and another MockRefAddr whose type is null. Should return the original
	 * object o.
	 */
	public void testGetObjectInstance_NoBuilder_ReferenceNullTypedNonStrAddr()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_ReferenceNullTypedNonStrAddr()");
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		env.put(Context.URL_PKG_PREFIXES, "org.apache.harmony.jndi.tests.javax.naming.spi.mock");
		Reference r = new Reference(null);
		MockRefAddr mockAddr = new MockRefAddr("URL", "ftp://www.apache.org/");
		r.add(mockAddr);
		mockAddr = new MockRefAddr(null, "ftp://www.apache.org/");
		r.add(mockAddr);

		Object obj = NamingManager.getObjectInstance(r, new CompositeName(
				"compositename"), new MockContext(new Hashtable<String, Object>()), env);
		assertSame(obj, r);
	}

	/**
	 * When no factory builder is set and the fed object is Reference with no
	 * factory name, and there is a StringRefAddr whose type is null. Should
	 * throw NullPointerException.
	 */
	public void testGetObjectInstance_NoBuilder_ReferenceNullTypedStrAddr()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_ReferenceNullTypedStrAddr()");
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		env.put(Context.URL_PKG_PREFIXES, "org.apache.harmony.jndi.tests.javax.naming.spi.mock");
		Reference r = new Reference(null);
		StringRefAddr nullTypeAddr = new StringRefAddr(null,
				"ftp://www.apache.org/");
		r.add(nullTypeAddr);

		try {
			NamingManager.getObjectInstance(r, new CompositeName(
					"compositename"), new MockContext(new Hashtable<String, Object>()), env);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
			// log.log(e);
		}

		// test Referenceable
		MockReferenceable mr = new MockReferenceable(r);
		try {
			NamingManager.getObjectInstance(mr, new CompositeName(
					"compositename"), new MockContext(new Hashtable<String, Object>()), env);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
			// log.log(e);
		}
	}

	/**
	 * When no factory builder is set and the fed object is Reference with no
	 * factory name, and there is a StringRefAddr with a valid URL which can be
	 * used to create the object successfully. Before this URL, there are
	 * several "invalid" URLs: one without scheme, and next without corresponding
	 * factory, and a third one corresponding to a factory that returns null.
	 * The types of these StringRefAddr is "URL". Before all these
	 * StringRefAddr, there is a StringRefAddr whose type is neither "URL" nor
	 * "url" but contains a valid URL. Should return an object corresponding to
	 * the URL mentioned in the beginning successfully.
	 * 
	 * Try the same when the fed object is Referenceable. Replace the address
	 * type "URL" with "url" and try again.
	 * 
	 * URL_PKG_PREFIXES is contained in the fed environment properties.
	 */
	private void myTestGetObjectInstance_NoBuilder_ReferenceValidURL(String url)
			throws Exception {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		env.put(Context.URL_PKG_PREFIXES, "org.apache.harmony.jndi.tests.javax.naming.spi.mock");
		Reference r = new Reference(null);
		StringRefAddr notUrlTypeAddr = new StringRefAddr("uurl",
				"ftp://www.apache.org/");
		r.add(notUrlTypeAddr);
		StringRefAddr noSchemeAddr = new StringRefAddr("URL", "www.apache.org");
		r.add(noSchemeAddr);
		StringRefAddr noFactorySchemeAddr = new StringRefAddr(url,
				"httpss://www.apache.org/");
		r.add(noFactorySchemeAddr);
		StringRefAddr returnNullFactoryAddr = new StringRefAddr(url,
				"news://www.apache.org/");
		r.add(returnNullFactoryAddr);
		StringRefAddr validFactoryAddr = new StringRefAddr(url,
				"ftp://www.apache.org/");
		r.add(validFactoryAddr);

		MockContext ctx = (MockContext) NamingManager.getObjectInstance(r,
				new CompositeName("compositename"), new MockContext(
						new Hashtable<String, Object>()), env);
		// if (null != ctx) {
		// log.log(ctx.getEnvironment().toString());
		// } else {
		// log.log("Null object returned!");
		// }
		assertTrue(ctx.parameterEquals(validFactoryAddr.getContent(),
				new CompositeName("compositename"), new MockContext(
						new Hashtable<String, Object>()), env));

		// test Referenceable
		MockReferenceable mr = new MockReferenceable(r);
		ctx = (MockContext) NamingManager.getObjectInstance(mr,
				new CompositeName("compositename"), new MockContext(
						new Hashtable<String, Object>()), env);
		assertTrue(ctx.parameterEquals(validFactoryAddr.getContent(),
				new CompositeName("compositename"), new MockContext(
						new Hashtable<String, Object>()), env));
	}

	public void testGetObjectInstance_NoBuilder_ReferenceValidURL_URL()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_ReferenceValidURL_URL()");
		myTestGetObjectInstance_NoBuilder_ReferenceValidURL("URL");
	}

	public void testGetObjectInstance_NoBuilder_ReferenceValidURL_url()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_ReferenceValidURL_url()");
		myTestGetObjectInstance_NoBuilder_ReferenceValidURL("url");
	}

	/**
	 * Test the default URL context factory: com.sun.jndi.url
	 */
	public void testGetObjectInstance_NoBuilder_ReferenceDefaultURL()
			throws Exception {
		log.setMethod("testGetObjectInstance_NoBuilder_ReferenceDefaultURL()");
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		env.put(Context.URL_PKG_PREFIXES, "org.apache.harmony.jndi.tests.javax.naming.spi.mock");

		Reference r = new Reference(null);
		StringRefAddr addr = new StringRefAddr("url", "nntp://www.apache.org/");
		r.add(addr);

		nntpURLContextFactory.MockObject obj = (nntpURLContextFactory.MockObject) NamingManager
				.getObjectInstance(r, new CompositeName("compositename"), null,
						env);

		assertEquals(obj, new nntpURLContextFactory.MockObject(addr
				.getContent(), new CompositeName("compositename"), null, env));

		// test Referenceable
		MockReferenceable mr = new MockReferenceable(r);
		obj = (nntpURLContextFactory.MockObject) NamingManager
				.getObjectInstance(mr.getReference(), new CompositeName(
						"compositename"), null, env);

		assertEquals(obj, new nntpURLContextFactory.MockObject(addr
				.getContent(), new CompositeName("compositename"), null, env));
	}

	/**
	 * When no factory builder is set and the fed object is Reference with no
	 * factory name, and there is a StringRefAddr with a valid URL which results
	 * in a NullPointerException when creating an object. After this URL there
	 * is another URL which can be used to create the object successfully. The
	 * types of these StringRefAddr is "URL". Should throw a
	 * NullPointerException.
	 * 
	 * Try the same when the fed object is Referenceable. Replace the address
	 * type "URL" with "url" and try again.
	 * 
	 * URL_PKG_PREFIXES is contained in the fed context's provider resource
	 * file.
	 * 
	 */
	private void myTestGetObjectInstance_NoBuilder_ReferenceExceptionalURL(
			String url) throws Exception {
		try {
			// testGetStateToBind_obj_name_ctx_empty();
			Hashtable<String, Object> env = new Hashtable<String, Object>();

			Reference r = new Reference(null);
			StringRefAddr exceptionalFactoryAddr = new StringRefAddr(url,
					"http://www.apache.org/");
			r.add(exceptionalFactoryAddr);
			StringRefAddr validFactoryAddr = new StringRefAddr(url,
					"ftp://www.apache.org/");
			r.add(validFactoryAddr);

			Hashtable<String, Object> ctxEnv = new Hashtable<String, Object>();
			/*
			 * ctxEnv.put( Context.INITIAL_CONTEXT_FACTORY,
			 * "dazzle.jndi.testing.spi.DazzleContextFactory");
			 */
			// ctxEnv.put(Context.URL_PKG_PREFIXES,
			// "org.apache.harmony.jndi.tests.javax.naming.spi.mock");
			env
					.put(Context.URL_PKG_PREFIXES,
							"org.apache.harmony.jndi.tests.javax.naming.spi.mock");

			// writeProviderResource("org.apache.harmony.jndi.tests.javax.naming.spi.dummy",
			// ctxEnv);
			try {
				indicateNullPointerException(env);
				Object ctx = NamingManager.getObjectInstance(r,
						new CompositeName("compositename"), new MockContext(
								ctxEnv), env);
				log.log(null == ctx ? "null object returned" : ctx.toString());
				fail("Should throw NamingException with root cause - null pointer.");
			} catch (NamingException e) {
				assertTrue(e.getRootCause() instanceof NullPointerException);
			}

			// test Referenceable
			MockReferenceable mr = new MockReferenceable(r);
			try {
				indicateNamingException(env);
				MockContext ctx = (MockContext) NamingManager
						.getObjectInstance(mr, new CompositeName(
								"compositename"), new MockContext(ctxEnv), env);
				log.log(null == ctx ? "null object returned" : ctx.toString());
				fail("Should throw NamingException.");
			} catch (NamingException e) {
				assertNull(e.getRootCause());
			}
		} finally {
			writeProviderResource("org.apache.harmony.jndi.tests.javax.naming.spi.dummy",
					new Hashtable<String, Object>());
		}
	}

	private void myTestGetObjectInstance_NoBuilder_ReferenceExceptionalURL2(
			String url) throws Exception {
		try {

			Reference r = new Reference(null);
			StringRefAddr exceptionalFactoryAddr = new StringRefAddr(url,
					"http://www.apache.org/");
			r.add(exceptionalFactoryAddr);
			StringRefAddr validFactoryAddr = new StringRefAddr(url,
					"ftp://www.apache.org/");
			r.add(validFactoryAddr);

			Hashtable<String, Object> ctxEnv = new Hashtable<String, Object>();

			ctxEnv.put(Context.INITIAL_CONTEXT_FACTORY,
					"dazzle.jndi.testing.spi.DazzleContextFactory");

			ctxEnv.put(Context.URL_PKG_PREFIXES,
					"org.apache.harmony.jndi.tests.javax.naming.spi.mock");

			writeProviderResource("org.apache.harmony.jndi.tests.javax.naming.spi.dummy", ctxEnv);
			try {
				indicateNullPointerException(ctxEnv);
				NamingManager.getObjectInstance(r,
						new CompositeName("compositename"), new MockContext(
								ctxEnv), ctxEnv);
				fail("Should throw NamingException with root cause - null pointer.");
			} catch (NamingException e) {
				assertTrue(e.getRootCause() instanceof NullPointerException);
			}

			// test Referenceable
			MockReferenceable mr = new MockReferenceable(r);
			try {
				indicateNamingException(ctxEnv);
				NamingManager
						.getObjectInstance(mr, new CompositeName(
								"compositename"), new MockContext(ctxEnv),
								ctxEnv);
				fail("Should throw NamingException.");
			} catch (NamingException e) {
				assertNull(e.getRootCause());
			}
		} finally {
			writeProviderResource("org.apache.harmony.jndi.tests.javax.naming.spi.dummy",
					new Hashtable<String, Object>());
		}
	}

	public void testGetObjectInstance_NoBuilder_ReferenceExceptionalURL_URL()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_ReferenceExceptionalURL_URL()");
		myTestGetObjectInstance_NoBuilder_ReferenceExceptionalURL("URL");
	}

	public void testGetObjectInstance_NoBuilder_ReferenceExceptionalURL_URL2()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_ReferenceExceptionalURL_URL2()");
		myTestGetObjectInstance_NoBuilder_ReferenceExceptionalURL2("URL");
	}

	public void testGetObjectInstance_NoBuilder_ReferenceExceptionalURL_url()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_ReferenceExceptionalURL_url()");
		myTestGetObjectInstance_NoBuilder_ReferenceExceptionalURL("url");
	}

	/**
	 * When no factory builder is set and the fed object is neither Reference
	 * nor Referenceable (e.g., String), and the environment properties contains
	 * a valid factory name, while the fed context's provider resource file also
	 * contains a valid factory name. Should return an object created by the
	 * factory specified by the fed environment properties.
	 */
	public void testGetObjectInstance_NoBuilder_NotRef_ValidFactory()
			throws Exception {
		log.setMethod("testGetObjectInstance_NoBuilder_NotRef_ValidFactory()");
		try {
			log
					.setMethod("testGetObjectInstance_NoBuilder_NotRef_ValidFactory");
			Hashtable<String, Object> env = new Hashtable<String, Object>();
			env.put(Context.INITIAL_CONTEXT_FACTORY,
					"dazzle.jndi.testing.spi.DazzleContextFactory");
			env
					.put(Context.OBJECT_FACTORIES,
							"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactory");

			Hashtable<String, Object> ctxEnv = new Hashtable<String, Object>();
			ctxEnv
					.put(Context.OBJECT_FACTORIES,
							"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactoryNoException");
			ctxEnv.put(Context.URL_PKG_PREFIXES,
					"org.apache.harmony.jndi.tests.javax.naming.spi.mock");
			writeProviderResource("org.apache.harmony.jndi.tests.javax.naming.spi.dummy", ctxEnv);
			Context ctx = new MockContext(ctxEnv);

			Object obj = NamingManager.getObjectInstance("Junk",
					new CompositeName("compositename"), ctx, env);
			assertEquals(new MockObject("Junk", new CompositeName(
					"compositename"), ctx, env), obj);
		} finally {
			writeProviderResource("org.apache.harmony.jndi.tests.javax.naming.spi.dummy",
					new Hashtable<String, Object>());
		}
	}

	/**
	 * When no factory builder is set and the fed object is Reference with no
	 * factory name but contains several "invalid" URLs: one without scheme, and
	 * next without corresponding factory, and a third one corresponding to a
	 * factory that returns null. The types of these StringRefAddr is "url".
	 * Before all these StringRefAddr, there is is a MockRefAddr whose type is
	 * also "url" and contains a valid URL which can be used to create an object
	 * successfully, and a StringRefAddr whose type is neither "URL" nor "url"
	 * but contains a valid URL. And fed context's provider resource file does
	 * contain a valid factory name following another factory that returns null.
	 * Should return an object created by the factory specified by the fed
	 * context's environment properties.
	 * 
	 */
	public void testGetObjectInstance_NoBuilder_NotRef_ValidFactoryWithNull()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_NotRef_ValidFactoryWithNull()");
		try {
			log
					.setMethod("testGetObjectInstance_NoBuilder_NotRef_ValidFactoryWithNull");
			Hashtable<String, Object> ctxEnv = new Hashtable<String, Object>();
			ctxEnv
					.put(
							Context.OBJECT_FACTORIES,
							":org.apache.harmony.jndi.tests.javax.naming.spi.news.newsURLContextFactory:"
									+ "org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactory");
			// ctxEnv.put(Context.URL_PKG_PREFIXES,
			// "org.apache.harmony.jndi.tests.javax.naming.spi.mock");
			// writeProviderResource("org.apache.harmony.jndi.tests.javax.naming.spi.dummy",
			// ctxEnv);

			Hashtable<String, Object> env = new Hashtable<String, Object>();
			env
					.put(
							Context.OBJECT_FACTORIES,
							":org.apache.harmony.jndi.tests.javax.naming.spi.news.newsURLContextFactory:"
									+ "org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactory");

			Context ctx = new MockContext(ctxEnv);

			/*
			 * env.put( Context.INITIAL_CONTEXT_FACTORY,
			 * "dazzle.jndi.testing.spi.DazzleContextFactory");
			 */
			env
					.put(Context.URL_PKG_PREFIXES,
							"org.apache.harmony.jndi.tests.javax.naming.spi.mock");

			Reference r = new Reference("");
			MockRefAddr mockAddr = new MockRefAddr("URL",
					"ftp://www.apache.org/");
			r.add(mockAddr);
			StringRefAddr notUrlTypeAddr = new StringRefAddr("uurl",
					"ftp://www.apache.org/");
			r.add(notUrlTypeAddr);
			StringRefAddr noSchemeAddr = new StringRefAddr("URL",
					"www.apache.org");
			r.add(noSchemeAddr);
			StringRefAddr noFactorySchemeAddr = new StringRefAddr("url",
					"httpss://www.apache.org/");
			r.add(noFactorySchemeAddr);
			StringRefAddr returnNullFactoryAddr = new StringRefAddr("url",
					"news://www.apache.org/");
			r.add(returnNullFactoryAddr);

			Object obj = NamingManager.getObjectInstance(r, new CompositeName(
					"compositename"), ctx, env);
			// if (null != obj) {
			// log.log(obj.toString());
			// } else {
			// log.log("Null object returned!");
			// }
			assertEquals(new MockObject(r, new CompositeName("compositename"),
					ctx, env), obj);
		} finally {
			writeProviderResource("org.apache.harmony.jndi.tests.javax.naming.spi.dummy",
					new Hashtable<String, Object>());
		}
	}

	public void testGetObjectInstance_NoBuilder_NotRef_ValidFactoryWithNull_1()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_NotRef_ValidFactoryWithNull_1()");
		try {
			log
					.setMethod("testGetObjectInstance_NoBuilder_NotRef_ValidFactoryWithNull_1");
			Hashtable<String, Object> ctxEnv = new Hashtable<String, Object>();
			writeProviderResource("org.apache.harmony.jndi.tests.javax.naming.spi.dummy", ctxEnv);

			Context ctx = new MockContext(ctxEnv);

			Hashtable<String, String> env = new Hashtable<String, String>();
			env
					.put(Context.URL_PKG_PREFIXES,
							"org.apache.harmony.jndi.tests.javax.naming.spi.mock");
			env.put(Context.OBJECT_FACTORIES,
					":org.apache.harmony.jndi.tests.javax.naming.spi.news.newsURLContextFactory");

			Reference r = new Reference("");
			MockRefAddr mockAddr = new MockRefAddr("URL",
					"ftp://www.apache.org/");
			r.add(mockAddr);
			StringRefAddr notUrlTypeAddr = new StringRefAddr("uurl",
					"ftp://www.apache.org/");
			r.add(notUrlTypeAddr);
			StringRefAddr noSchemeAddr = new StringRefAddr("URL",
					"www.apache.org");
			r.add(noSchemeAddr);
			StringRefAddr noFactorySchemeAddr = new StringRefAddr("url",
					"httpss://www.apache.org/");
			r.add(noFactorySchemeAddr);
			StringRefAddr returnNullFactoryAddr = new StringRefAddr("url",
					"news://www.apache.org/");
			r.add(returnNullFactoryAddr);

			Object obj = NamingManager.getObjectInstance(r, new CompositeName(
					"compositename"), ctx, env);
			// if (null != obj) {
			// log.log(obj.toString());
			// } else {
			// log.log("Null object returned!");
			// }
			assertEquals(r, obj);
		} finally {
			writeProviderResource("org.apache.harmony.jndi.tests.javax.naming.spi.dummy",
					new Hashtable<String, Object>());
		}
	}

	public void testGetObjectInstance_NoBuilder_NotRef_ValidFactoryWithNull_2()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_NotRef_ValidFactoryWithNull_2()");
		try {
			log
					.setMethod("testGetObjectInstance_NoBuilder_NotRef_ValidFactoryWithNull_2");
			Hashtable<String, Object> ctxEnv = new Hashtable<String, Object>();
			writeProviderResource("org.apache.harmony.jndi.tests.javax.naming.spi.dummy", ctxEnv);

			Context ctx = new MockContext(ctxEnv);

			Hashtable<String, Object> env = new Hashtable<String, Object>();
			env.put(Context.INITIAL_CONTEXT_FACTORY,
					"dazzle.jndi.testing.spi.DazzleContextFactory");
			env
					.put(Context.URL_PKG_PREFIXES,
							"org.apache.harmony.jndi.tests.javax.naming.spi.mock");
			env
					.put(
							Context.OBJECT_FACTORIES,
							":org.apache.harmony.jndi.tests.javax.naming.spi.news.newsURLContextFactory"
									+ ":org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactory");

			Reference r = new Reference("");
			MockRefAddr mockAddr = new MockRefAddr("URL",
					"ftp://www.apache.org/");
			r.add(mockAddr);
			StringRefAddr notUrlTypeAddr = new StringRefAddr("uurl",
					"ftp://www.apache.org/");
			r.add(notUrlTypeAddr);
			StringRefAddr noSchemeAddr = new StringRefAddr("URL",
					"www.apache.org");
			r.add(noSchemeAddr);
			StringRefAddr noFactorySchemeAddr = new StringRefAddr("url",
					"httpss://www.apache.org/");
			r.add(noFactorySchemeAddr);
			StringRefAddr returnNullFactoryAddr = new StringRefAddr("url",
					"news://www.apache.org/");
			r.add(returnNullFactoryAddr);

			Object obj = NamingManager.getObjectInstance(r, new CompositeName(
					"compositename"), ctx, env);
			// if (null != obj) {
			// log.log(obj.toString());
			// } else {
			// log.log("Null object returned!");
			// }
			assertEquals(new MockObject(r, new CompositeName("compositename"),
					ctx, env), obj);
		} finally {
			writeProviderResource("org.apache.harmony.jndi.tests.javax.naming.spi.dummy",
					new Hashtable<String, Object>());
		}
	}

	/**
	 * When no factory builder is set, and all fed parameters are null. Should
	 * return the original object.
	 */
	public void testGetObjectInstance_NoBuilder_AllNull() throws Exception {
		log.setMethod("testGetObjectInstance_NoBuilder_AllNull()");
		Object obj = NamingManager.getObjectInstance(null, null, null, null);
		assertNull(obj);

		Object originalObject = new MockObject(null, null, null, null);
		obj = NamingManager.getObjectInstance(originalObject, null, null, null);
		assertSame(obj, originalObject);
	}

	/**
	 * When no factory builder is set, and all fed parameters are null except
	 * the original object and the environment properties. The environment
	 * properties contains an invalid factory name, and a valid factory name
	 * that follows. Should return an object created by the valid factory.
	 */
	public void testGetObjectInstance_NoBuilder_NotRef_InvalidFactory()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_NotRef_InvalidFactory()");
		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		env
				.put(
						Context.OBJECT_FACTORIES,
						"junk.factory:"
								+ "org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactory");

		Object obj = NamingManager.getObjectInstance(null, null, null, env);
		assertEquals(new MockObject(null, null, null, env), obj);

		obj = NamingManager.getObjectInstance(env, null, null, env);
		assertEquals(new MockObject(env, null, null, env), obj);
	}

	/**
	 * When no factory builder is set, and all fed parameters are null except
	 * the original object and the environment properties. The environment
	 * properties contains a valid factory that throws an exception, and a valid
	 * factory name that follows. Should throw an exception.
	 */
	public void testGetObjectInstance_NoBuilder_NotRef_ExceptionalFactory()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_NotRef_ExceptionalFactory()");
		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		env
				.put(
						Context.OBJECT_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactory:"
								+ "org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactoryNoException");

		try {
			indicateNullPointerException(env);
			NamingManager.getObjectInstance(null, null, null, env);
			fail("Should throw NullPointerException.");
		} catch (NullPointerException e) {
		}

		try {
			indicateNamingException(env);
			NamingManager.getObjectInstance(null, null, null, env);
			fail("Should throw NamingException.");
		} catch (NamingException e) {
		}

	}

	/**
	 * When no factory builder is set, and all fed parameters are null except
	 * the original object and the environment properties. The environment
	 * properties contains a valid factory that returns null. Should return the
	 * original object.
	 */
	public void testGetObjectInstance_NoBuilder_NotRef_FactoryWithNull()
			throws Exception {
		log
				.setMethod("testGetObjectInstance_NoBuilder_NotRef_FactoryWithNull()");
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"dazzle.jndi.testing.spi.DazzleContextFactory");
		env.put(Context.OBJECT_FACTORIES,
				"org.apache.harmony.jndi.tests.javax.naming.spi.news.newsURLContextFactory");

		Object obj = NamingManager.getObjectInstance(null, null, null, env);
		assertNull(obj);

		obj = NamingManager.getObjectInstance(env, null, null, env);
		assertSame(env, obj);
	}

	public void testGetStateToBind_null_null_null_null() {
		log.setMethod("testGetStateToBind_null_null_null_null()");
		Object o = null;
		Name n = null;
		Context c = null;
		Hashtable<?, ?> h = null;

		try {
			Object r = NamingManager.getStateToBind(o, n, c, h);
			assertNull(r);
		} catch (NamingException e) {
			fail("should throw NullPointerException");
		}
	}

	public void testGetStateToBind_null_null_null_hash() {
		log.setMethod("testGetStateToBind_null_null_null_hash()");
		Object o = null;
		Name n = null;
		Context c = null;
		Hashtable<String, String> h = new Hashtable<String, String>();
		h
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MagicMockStateFactory");

		try {
			Hashtable<?, ?> r = (Hashtable<?, ?>) NamingManager.getStateToBind(o, n, c, h);
			assertEquals(r.get("o"), o);
			assertEquals(r.get("n"), n);
			assertEquals(r.get("c"), c);
			assertEquals(r.get("h"), h);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	// public void testGetStateToBind_null_null_ctx_null() {
	// log.setMethod("testGetStateToBind_null_null_ctx_null()");
	// Object o = null;
	// Name n = null;
	// Context c = new MockDirContext(new Hashtable());
	// // lead to state factory
	// Hashtable h = null;
	//
	// try {
	// Hashtable r = (Hashtable) NamingManager.getStateToBind(o, n, c, h);
	// assertEquals(r.get("o"), o);
	// assertEquals(r.get("n"), n);
	// assertEquals(r.get("c"), c);
	// assertEquals(r.get("h"), h);
	// } catch (Throwable e) {
	// log.log(e);
	// fail();
	// }
	// }

	// public void testGetStateToBind_null_null_ctx_empty() {
	// log.setMethod("testGetStateToBind_null_null_ctx_empty()");
	// Object o = null;
	// Name n = null;
	// Context c = new MockDirContext(new Hashtable());
	// // lead to state factory
	// Hashtable h = new Hashtable();
	//
	// try {
	// Hashtable r = (Hashtable) NamingManager.getStateToBind(o, n, c, h);
	// assertEquals(r.get("o"), o);
	// assertEquals(r.get("n"), n);
	// assertEquals(r.get("c"), c);
	// assertEquals(r.get("h"), h);
	// } catch (Throwable e) {
	// log.log(e);
	// fail();
	// }
	// }

	public void testGetStateToBind_null_null_ctx_empty2() {
		log.setMethod("testGetStateToBind_null_null_ctx_empty2()");
		Object o = null;
		Name n = null;
		Context c = new MockContext(new Hashtable<String, Object>()); // no state factory
		Hashtable<?, ?> h = new Hashtable<Object, Object>();

		try {
			Object r = NamingManager.getStateToBind(o, n, c, h);
			assertNull(r);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetStateToBind_null_name_ctx_hash() {
		log.setMethod("testGetStateToBind_null_name_ctx_hash()");
		Object o = null;
		Name n = new CompositeName();
		Context c = new MockDirContext(new Hashtable<Object, Object>());
		// lead to state factory
		Hashtable<String, String> h = new Hashtable<String, String>();
		h
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MagicMockStateFactory");

		try {
			Hashtable<?, ?> r = (Hashtable<?, ?>) NamingManager.getStateToBind(o, n, c, h);
			assertEquals(r.get("o"), o);
			assertEquals(r.get("n"), n);
			assertEquals(r.get("c"), c);
			assertEquals(r.get("h"), h);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	// public void testGetStateToBind_obj_name_ctx_empty() {
	// log.setMethod("testGetStateToBind_obj_name_ctx_empty()");
	// Object o = "object";
	// Name n = new CompositeName();
	// Context c = new MockDirContext(new Hashtable());
	// // lead to state factory
	// Hashtable h = new Hashtable();
	//
	// try {
	// Hashtable r = (Hashtable) NamingManager.getStateToBind(o, n, c, h);
	// assertEquals(r.get("o"), o);
	// assertEquals(r.get("n"), n);
	// assertEquals(r.get("c"), c);
	// assertEquals(r.get("h"), h);
	// } catch (Throwable e) {
	// log.log(e);
	// fail();
	// }
	// }

	public void testGetStateToBind_obj_name_ctx_empty2() {
		log.setMethod("testGetStateToBind_obj_name_ctx_empty2()");
		Object o = "object";
		Name n = new CompositeName();
		Context c = new MockContext(new Hashtable<String, Object>()); // no state factory
		Hashtable<Object, Object> h = new Hashtable<Object, Object>();

		try {
			Object r = NamingManager.getStateToBind(o, n, c, h);
			assertSame(o, r);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetStateToBind_obj_name_ctx_hash() {
		log.setMethod("testGetStateToBind_obj_name_ctx_hash()");
		Object o = "object";
		Name n = new CompositeName();
		Context c = new MockDirContext(new Hashtable<Object, Object>());
		// lead to state factory
		Hashtable<String, String> h = new Hashtable<String, String>();
		h
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MagicMockStateFactory");

		try {
			Hashtable<?, ?> r = (Hashtable<?, ?>) NamingManager.getStateToBind(o, n, c, h);
			assertEquals(r.get("o"), o);
			assertEquals(r.get("n"), n);
			assertEquals(r.get("c"), c);
			assertEquals(r.get("h"), h);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetStateToBind_f1BadClassName_Success() {
		log.setMethod("testGetStateToBind_f1BadClassName_Success()");
		Object o = "object";
		Name n = new CompositeName();
		Context c = new MockContext(new Hashtable<String, Object>()); // no state factory
		Hashtable<Object, Object> h = new Hashtable<Object, Object>();
		h.put(Context.STATE_FACTORIES,"bad.class.Name:org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MagicMockStateFactory");

        try {
			Hashtable<?, ?> r = (Hashtable<?, ?>) NamingManager.getStateToBind(o, n, c, h);
			assertEquals(r.get("o"), o);
			assertEquals(r.get("n"), n);
			assertEquals(r.get("c"), c);
			assertEquals(r.get("h"), h);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	// public void testGetStateToBind_f2Success() {
	// log.setMethod("testGetStateToBind_f2Success()");
	// Object o = "object";
	// Name n = new CompositeName();
	// Context c = new MockDirContext(new Hashtable());
	// // lead to state factory
	// Hashtable h = new Hashtable();
	//
	// try {
	// Object ro = NamingManager.getStateToBind(o, n, c, h);
	// Hashtable r = (Hashtable) ro;
	// assertEquals(r.get("o"), o);
	// assertEquals(r.get("n"), n);
	// assertEquals(r.get("c"), c);
	// assertEquals(r.get("h"), h);
	// } catch (Throwable e) {
	// log.log(e);
	// fail();
	// }
	// }

	// public void testGetStateToBind_f1BadClassName_f2Success() {
	// log.setMethod("testGetStateToBind_f1BadClassName_f2Success()");
	// Object o = "object";
	// Name n = new CompositeName();
	// Context c = new MockDirContext(new Hashtable());
	// // lead to state factory
	// Hashtable h = new Hashtable();
	// h.put(Context.STATE_FACTORIES, "bad.class.Name");
	//
	// try {
	// Object ro = NamingManager.getStateToBind(o, n, c, h);
	// Hashtable r = (Hashtable) ro;
	// assertEquals(r.get("o"), o);
	// assertEquals(r.get("n"), n);
	// assertEquals(r.get("c"), c);
	// assertEquals(r.get("h"), h);
	// } catch (Throwable e) {
	// log.log(e);
	// fail();
	// }
	// }

	public void testGetStateToBind_f1NamingException_f2Success() {
		log.setMethod("testGetStateToBind_f1NamingException_f2Success()");
		Object o = "object";
		Name n = new CompositeName();
		Context c = new MockDirContext(new Hashtable<Object, Object>());
		// lead to state factory
		Hashtable<String, Object> h = new Hashtable<String, Object>();
		h
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MagicMockStateFactory");
		indicateNamingException(h);

		try {
			NamingManager.getStateToBind(o, n, c, h);
		} catch (NamingException e) {
			assertTrue(e.getMessage().indexOf("Simulated") >= 0);
		} catch (Throwable e) {
			fail("should throw NamingException");
		}
	}

	public void testGetStateToBind_f1RuntimeException_f2Success() {
		log.setMethod("testGetStateToBind_f1RuntimeException_f2Success()");
		Object o = "object";
		Name n = new CompositeName();
		Context c = new MockDirContext(new Hashtable<Object, Object>());
		// lead to state factory
		Hashtable<Object, Object> h = new Hashtable<Object, Object>();
		h
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MagicMockStateFactory");
		indicateRuntimeException(h);

		try {
			NamingManager.getStateToBind(o, n, c, h);
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().indexOf("Simulated") >= 0);
		} catch (Throwable e) {
			fail("should throw RuntimeException");
		}
	}

	@SuppressWarnings("unchecked")
    public void testGetStateToBind_f1ReturnNull_Success() {
		log.setMethod("testGetStateToBind_f1ReturnNull_Success()");
		Object o = "object";
		Name n = new CompositeName();
		Context c = new MockContext(new Hashtable<String, Object>()); // no state factory
		Hashtable<Object, Object> h = new Hashtable<Object, Object>();
		h
				.put(
						Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MagicMockStateFactory"
								+ ":"
								+ "org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MagicSuccessMockStateFactory");
		indicateReturnNull(h);

		try {
			Hashtable<Object, Object> r = (Hashtable<Object, Object>) NamingManager.getStateToBind(o, n, c, h);
			assertEquals(r.get("o"), o);
			assertEquals(r.get("n"), n);
			assertEquals(r.get("c"), c);
			assertEquals(r.get("h"), h);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	// public void testGetStateToBind_f1ReturnNull_f2Success() {
	// log.setMethod("testGetStateToBind_f1ReturnNull_f2Success()");
	// Object o = "object";
	// Name n = new CompositeName();
	// Context c = new MockDirContext(new Hashtable());
	// // lead to state factory
	// Hashtable h = new Hashtable();
	// h
	// .put(Context.STATE_FACTORIES,
	// "org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MagicMockStateFactory");
	// indicateReturnNull(h);
	//
	// try {
	// Object ro = NamingManager.getStateToBind(o, n, c, h);
	// Hashtable r = (Hashtable) ro;
	// assertEquals(r.get("o"), o);
	// assertEquals(r.get("n"), n);
	// assertEquals(r.get("c"), c);
	// assertEquals(r.get("h"), h);
	// } catch (Throwable e) {
	// log.log(e);
	// fail();
	// }
	// }

	public void testGetStateToBind_f1Success_f2Success() {
		log.setMethod("testGetStateToBind_f1Success_f2Success()");
		Object o = "object";
		Name n = new CompositeName();
		Context c = new MockDirContext(new Hashtable<Object, Object>());
		// lead to state factory
		Hashtable<Object, Object> h = new Hashtable<Object, Object>();
		h
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MagicSuccessMockStateFactory");
		indicateRuntimeException(h);

		try {
            Hashtable<?, ?> r = (Hashtable<?, ?>) NamingManager.getStateToBind(o, n, c, h);
			assertEquals(r.get("o"), o);
			assertEquals(r.get("n"), n);
			assertEquals(r.get("c"), c);
			assertEquals(r.get("h"), h);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetStateToBind_f1ReturnNull() {
		log.setMethod("testGetStateToBind_f1ReturnNull()");
		Object o = "object";
		Name n = new CompositeName();
		Context c = new MockContext(new Hashtable<String, Object>()); // no state factory
		Hashtable<Object, Object> h = new Hashtable<Object, Object>();
		h
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MagicMockStateFactory");
		indicateReturnNull(h);

		try {
			Object result = NamingManager.getStateToBind(o, n, c, h);
			assertEquals(o, result);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetStateToBind_f1BadClassName() {
		log.setMethod("testGetStateToBind_f1BadClassName()");
		Object o = "object";
		Name n = new CompositeName();
		Context c = new MockContext(new Hashtable<String, Object>()); // no state factory
		Hashtable<String, String> h = new Hashtable<String, String>();
		h.put(Context.STATE_FACTORIES, "bad.class.Name");

		try {
			Object result = NamingManager.getStateToBind(o, n, c, h);
			assertEquals(o, result);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetStateToBind_f1NamingException() {
		log.setMethod("testGetStateToBind_f1NamingException()");
		Object o = "object";
		Name n = new CompositeName();
		Context c = new MockContext(new Hashtable<String, Object>()); // no state factory
		Hashtable<String, Object> h = new Hashtable<String, Object>();
		h
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MagicMockStateFactory");
		indicateNamingException(h);

		try {
			NamingManager.getStateToBind(o, n, c, h);
		} catch (NamingException e) {
			assertTrue(e.getMessage().indexOf("Simulated") >= 0);
		} catch (Throwable e) {
			fail("should throw NamingException");
		}
	}

	public void testGetStateToBind_f1RuntimeException() {
		log.setMethod("testGetStateToBind_f1RuntimeException()");
		Object o = "object";
		Name n = new CompositeName();
		Context c = new MockContext(new Hashtable<String, Object>()); // no state factory
		Hashtable<Object, Object> h = new Hashtable<Object, Object>();
		h
				.put(Context.STATE_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MagicMockStateFactory");
		indicateRuntimeException(h);

		try {
			NamingManager.getStateToBind(o, n, c, h);
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().indexOf("Simulated") >= 0);
		} catch (Throwable e) {
			fail("should throw RuntimeException");
		}
	}

	public void testGetURLContext_null_null() {
		log.setMethod("testGetURLContext_null_null()");
		String schema = null;
        Hashtable<Object, Object> h = null;

		try {
			Context ctx = NamingManager.getURLContext(schema, h);
			assertNull(ctx);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetURLContext_empty_null() {
		log.setMethod("testGetURLContext_empty_null()");
		String schema = "";
        Hashtable<Object, Object> h = null;

		try {
			Context ctx = NamingManager.getURLContext(schema, h);
			assertNull(ctx);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetURLContext_http_null() {
		log.setMethod("testGetURLContext_http_null()");
		String schema = "http";
        Hashtable<Object, Object> h = null;

		try {
			Context ctx = NamingManager.getURLContext(schema, h);
			assertNull(ctx);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetURLContext_http_empty() {
		log.setMethod("testGetURLContext_http_empty()");
		String schema = "http";
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();

		try {
			Context ctx = NamingManager.getURLContext(schema, h);
			assertNull(ctx);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetURLContext_http_f1Success() {
		log.setMethod("testGetURLContext_http_f1Success()");
		String schema = "http";
		Hashtable<String, String> h = new Hashtable<String, String>();
		h.put(Context.URL_PKG_PREFIXES,
				"org.apache.harmony.jndi.tests.javax.naming.spi.mocksuccess");

		try {
			Context ctx = NamingManager.getURLContext(schema, h);
			assertNull(ctx.getEnvironment().get("o"));
			assertNull(ctx.getEnvironment().get("n"));
			assertNull(ctx.getEnvironment().get("c"));
			assertSame(h, ctx.getEnvironment().get("h"));
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetURLContext_http_f1BadClassName_f2Success() {
		log.setMethod("testGetURLContext_http_f1BadClassName_f2Success()");
		String schema = "http";
		Hashtable<String, String> h = new Hashtable<String, String>();
		h.put(Context.URL_PKG_PREFIXES,
				"bad.class.name:org.apache.harmony.jndi.tests.javax.naming.spi.mocksuccess");

		try {
			Context ctx = NamingManager.getURLContext(schema, h);
			assertNull(ctx.getEnvironment().get("o"));
			assertNull(ctx.getEnvironment().get("n"));
			assertNull(ctx.getEnvironment().get("c"));
			assertSame(h, ctx.getEnvironment().get("h"));
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetURLContext_http_f1NamingException_f2Success() {
		log.setMethod("testGetURLContext_http_f1NamingException_f2Success()");
		String schema = "http";
		Hashtable<String, Object> h = new Hashtable<String, Object>();
		h
				.put(Context.URL_PKG_PREFIXES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.mock:org.apache.harmony.jndi.tests.javax.naming.spi.mocksuccess");
		indicateNamingException(h);

		try {
			NamingManager.getURLContext(schema, h);
			fail("NamingException expected");
		} catch (NamingException e) {
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetURLContext_http_f1RuntimeException_f2Success() {
		log.setMethod("testGetURLContext_http_f1RuntimeException_f2Success()");
		String schema = "http";
		Hashtable<Object, Object> h = new Hashtable<Object, Object>();
		h
				.put(Context.URL_PKG_PREFIXES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.mock:org.apache.harmony.jndi.tests.javax.naming.spi.mocksuccess");
		indicateRuntimeException(h);

		try {
			NamingManager.getURLContext(schema, h);
			fail("NamingException expected");
		} catch (NamingException e) {
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetURLContext_http_f1ReturnNull_f2Success() {
		log.setMethod("testGetURLContext_http_f1ReturnNull_f2Success()");
		String schema = "http";
		Hashtable<Object, Object> h = new Hashtable<Object, Object>();
		h
				.put(Context.URL_PKG_PREFIXES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.mock:org.apache.harmony.jndi.tests.javax.naming.spi.mocksuccess");
		indicateReturnNull(h);

		try {
			Context ctx = NamingManager.getURLContext(schema, h);
			assertNull(ctx);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetURLContext_http_f1Success_f2RuntimeException() {
		log.setMethod("testGetURLContext_http_f1Success_f2RuntimeException()");
		String schema = "http";
		Hashtable<Object, Object> h = new Hashtable<Object, Object>();
		h
				.put(Context.URL_PKG_PREFIXES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.mocksuccess:org.apache.harmony.jndi.tests.javax.naming.spi.mock");
		indicateRuntimeException(h);

		try {
			Context ctx = NamingManager.getURLContext(schema, h);
			assertNull(ctx.getEnvironment().get("o"));
			assertNull(ctx.getEnvironment().get("n"));
			assertNull(ctx.getEnvironment().get("c"));
			assertSame(h, ctx.getEnvironment().get("h"));
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetURLContext_http_f1ReturnNull_f2ReturnNull() {
		log.setMethod("testGetURLContext_http_f1ReturnNull_f2ReturnNull()");
		String schema = "http";
		Hashtable<Object, Object> h = new Hashtable<Object, Object>();
		h
				.put(Context.URL_PKG_PREFIXES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.mock:org.apache.harmony.jndi.tests.javax.naming.spi.mock");
		indicateReturnNull(h);

		try {
			Context ctx = NamingManager.getURLContext(schema, h);
			assertNull(ctx);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetURLContext_http_f1NamingException_f2NamingException() {
		log
				.setMethod("testGetURLContext_http_f1NamingException_f2NamingException()");
		String schema = "http";
		Hashtable<String, Object> h = new Hashtable<String, Object>();
		h
				.put(Context.URL_PKG_PREFIXES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.mock:org.apache.harmony.jndi.tests.javax.naming.spi.mock");
		indicateNamingException(h);

		try {
			NamingManager.getURLContext(schema, h);
			fail("NamingException expected");
		} catch (NamingException e) {
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetURLContext_http_f1RuntimeException_f2RuntimeException() {
		log
				.setMethod("testGetURLContext_http_f1RuntimeException_f2RuntimeException()");
		String schema = "http";
		Hashtable<Object, Object> h = new Hashtable<Object, Object>();
		h
				.put(Context.URL_PKG_PREFIXES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.mock:org.apache.harmony.jndi.tests.javax.naming.spi.mock");
		indicateRuntimeException(h);

		try {
			NamingManager.getURLContext(schema, h);
			fail("NamingException expected");
		} catch (NamingException e) {
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetURLContext_http_f1BadClassName_f2BadClassName() {
		log.setMethod("testGetURLContext_http_f1BadClassName_f2BadClassName()");
		String schema = "http";
		Hashtable<String, String> h = new Hashtable<String, String>();
		h.put(Context.URL_PKG_PREFIXES, "bad.class.name:bad.class.name");

		try {
			Context ctx = NamingManager.getURLContext(schema, h);
			assertNull(ctx);
		} catch (Throwable e) {
			log.log(e);
			fail();
		}
	}

	public void testGetContinuationContext_MockContext_null_null_null()
			throws NamingException {
		log
				.setMethod("testGetContinuationContext_MockContext_null_null_null()");
		CannotProceedException cpe = new CannotProceedException();
		Object resolvedObj = new MockContext(new Hashtable<String, Object>());
		cpe.setResolvedObj(resolvedObj);
		Context r = NamingManager.getContinuationContext(cpe);
		assertSame(resolvedObj, r);
		assertSame(cpe, cpe.getEnvironment().get(NamingManager.CPE));
	}

	public void testGetContinuationContext_OBJ_name_context_h()
			throws NamingException {
		log.setMethod("testGetContinuationContext_OBJ_name_context_h()");
		CannotProceedException cpe = new CannotProceedException();
		cpe.setResolvedObj("resolved object");
		cpe.setAltName(new CompositeName("abc/abc"));
		cpe.setAltNameCtx(new MockContext(new Hashtable<String, Object>()));
		Hashtable<String, String> h = new Hashtable<String, String>();
		h
				.put(Context.OBJECT_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockContextObjectFactory");
		cpe.setEnvironment(h);
		Context r = NamingManager.getContinuationContext(cpe);
		assertTrue(r instanceof MockContext);
	}

	public void testGetContinuationContext_OBJ_name_context_badnameh()
			throws NamingException {
		log.setMethod("testGetContinuationContext_OBJ_name_context_badnameh()");
		CannotProceedException cpe = new CannotProceedException();
		Object obj = "resolved object";
		cpe.setResolvedObj(obj);
		CompositeName altName = new CompositeName("abc/abc");
		cpe.setAltName(altName);
		MockContext context = new MockContext(new Hashtable<String, Object>());
		cpe.setAltNameCtx(context);
		Hashtable<String, String> h = new Hashtable<String, String>();
		h.put(Context.OBJECT_FACTORIES, "bad name:asdfa");
		cpe.setEnvironment(h);
		try {
			NamingManager.getContinuationContext(cpe);
			fail();
		} catch (CannotProceedException e) {
			assertCPE(cpe, altName, context, h, e, obj);
		}
	}

	private void assertCPE(CannotProceedException cpe, CompositeName altName,
			MockContext context, Hashtable<String, String> h, CannotProceedException e,
			Object obj) {
		assertNull(e.getExplanation());
		assertNull(e.getMessage());
		assertNull(e.getRootCause());
		assertNull(e.getRemainingName());
		assertNull(e.getRemainingNewName());
		assertNull(e.getResolvedName());
		assertSame(e.getAltName(), altName);
		assertSame(e.getAltNameCtx(), context);
		assertEquals(e.getResolvedObj(), obj);
		assertSame(e, cpe);
		if (h != null) {
			assertSame(e.getEnvironment(), h);
		} else {
			assertSame(e.getEnvironment().get(NamingManager.CPE), e);
		}
	}

	public void testGetContinuationContext_OBJ_name_context_wrongh()
			throws NamingException {
		log.setMethod("testGetContinuationContext_OBJ_name_context_wrongh()");
		CannotProceedException cpe = new CannotProceedException();
		Object obj = "resolved object";
		cpe.setResolvedObj(obj);

		CompositeName altName = new CompositeName("abc/abc");
		cpe.setAltName(altName);
		MockContext context = new MockContext(new Hashtable<String, Object>());
		cpe.setAltNameCtx(context);
		Hashtable<String, String> h = new Hashtable<String, String>();
		h
				.put(Context.OBJECT_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockObjectFactory");
		cpe.setEnvironment(h);
		try {
			NamingManager.getContinuationContext(cpe);
			fail();
		} catch (CannotProceedException e) {
			assertCPE(cpe, altName, context, h, e, obj);
		}
	}

	public void testGetContinuationContext_null_name_context_h()
			throws NamingException {
		log.setMethod("testGetContinuationContext_null_name_context_h()");
		CannotProceedException cpe = new CannotProceedException();
		CompositeName altName = new CompositeName("abc/abc");
		cpe.setAltName(altName);
		MockContext context = new MockContext(new Hashtable<String, Object>());
		cpe.setAltNameCtx(context);
		Hashtable<String, String> h = new Hashtable<String, String>();
		h.put(Context.OBJECT_FACTORIES,"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockContextObjectFactory");
        cpe.setEnvironment(h);
		try {
		    NamingManager.getContinuationContext(cpe);
			fail();
		} catch (CannotProceedException e) {
			assertCPE(cpe, altName, context, h, e, null);
		}
	}

	public void testGetContinuationContext_OBJ_null_ctx_h()
			throws NamingException {
		log.setMethod("testGetContinuationContext_OBJ_null_ctx_h()");
		CannotProceedException cpe = new CannotProceedException();
		cpe.setResolvedObj("resolved object");
		cpe.setAltNameCtx(new MockContext(new Hashtable<String, Object>()));
		Hashtable<String, String> h = new Hashtable<String, String>();
		h
				.put(Context.OBJECT_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockContextObjectFactory");
		cpe.setEnvironment(h);
		Context r = NamingManager.getContinuationContext(cpe);
		assertTrue(r instanceof MockContext);
	}

	public void testGetContinuationContext_OBJ_name_null_h()
			throws NamingException {
		log.setMethod("testGetContinuationContext_OBJ_name_null_h()");
		CannotProceedException cpe = new CannotProceedException();
		cpe.setResolvedObj("resolved object");
		cpe.setAltName(new CompositeName("abc/abc"));
		Hashtable<String, String> h = new Hashtable<String, String>();
		h
				.put(Context.OBJECT_FACTORIES,
						"org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest$MockContextObjectFactory");
		cpe.setEnvironment(h);
		Context r = NamingManager.getContinuationContext(cpe);
		assertTrue(r instanceof MockContext);
	}

	public void testGetContinuationContext_OBJ_name_context_null()
			throws NamingException {
		log.setMethod("testGetContinuationContext_OBJ_name_context_null()");
		CannotProceedException cpe = new CannotProceedException();
		Object obj = "resolved object";
		cpe.setResolvedObj(obj);
		CompositeName altName = new CompositeName("abc/abc");
		cpe.setAltName(altName);
		MockContext context = new MockContext(new Hashtable<String, Object>());
		cpe.setAltNameCtx(context);
		try {
			NamingManager.getContinuationContext(cpe);
			fail();
		} catch (CannotProceedException e) {
			assertCPE(cpe, altName, context, null, e, obj);
		}
	}

	public static void writeProviderResource(String ctxClassName, Hashtable<?, ?> res) {
		try {
			int dot = ctxClassName.lastIndexOf('.');
			String pkg = dot >= 0 ? ctxClassName.substring(0, dot) : "";
			File f = new File(System.getProperty("harmony.tests.cp1"), pkg
					.replace('.', '/')
					+ "/jndiprovider.properties");
            f.createNewFile();
            f.deleteOnExit();

			PrintStream out = new PrintStream(new FileOutputStream(f));
			for (Map.Entry<?, ?> e : res.entrySet()) {
				out.println(e.getKey() + "=" + e.getValue());
			}
			out.close();
		} catch (Throwable e) {
			log.log("Failed in writeProviderResource!!");
			log.log(e);
		}
	}

	public static void deleteProviderResource(String ctxClassName) {
		try {
			int dot = ctxClassName.lastIndexOf('.');
			String pkg = dot >= 0 ? ctxClassName.substring(0, dot) : "";
			File f = new File(System.getProperty("harmony.tests.cp1"), pkg
					.replace('.', '/')
					+ "/jndiprovider.properties");
			f.delete();
		} catch (Throwable e) {
			log.log("Failed in deleteProviderResource!!");
			log.log(e);
			e.printStackTrace();
		}
	}

	public static class MockObjectFactoryBuilder implements
			ObjectFactoryBuilder {

		private static final MockObjectFactoryBuilder _builder = new MockObjectFactoryBuilder();

		public static MockObjectFactoryBuilder getInstance() {
			return _builder;
		}

		public ObjectFactory createObjectFactory(Object o, Hashtable<?, ?> envmt)
				throws NamingException {
			issueIndicatedExceptions(envmt);
			if (returnNullIndicated(envmt)) {
				return null;
			}
			return new MockObjectFactory(envmt);
		}
	}

	public static class MockObjectFactory implements ObjectFactory {
		Hashtable<?, ?> ht = null;

		public Object getObjectInstance(Object o, Name n, Context c,
				Hashtable<?, ?> envmt) throws Exception {
			issueIndicatedExceptions(envmt);
			if (returnNullIndicated(envmt)) {
				return null;
			}
			return new MockObject(o, n, c, envmt);
		}

		public MockObjectFactory(Hashtable<?, ?> envmt) {
			ht = envmt;
		}

		public MockObjectFactory() {
		}
	}

	public static class MockObject {
		private Object o;

		private Name n;

		private Context c;

		private Hashtable<?, ?> envmt;

		public MockObject(Object o, Name n, Context c, Hashtable<?, ?> envmt) {
			this.o = o;
			this.n = n;
			this.c = c;
			this.envmt = envmt;
		}

		@Override
        public String toString() {
			String s = "MockObject {";

			s += "Object= " + o + "\n";
			s += "Name= " + n + "\n";
			s += "Context= " + c + "\n";
			s += "Env= " + envmt;

			s += "}";

			return s;
		}

		@Override
        public boolean equals(Object obj) {
			if (obj instanceof MockObject) {
				MockObject theOther = (MockObject) obj;
				if (o != theOther.o) {
					return false;
				}

				boolean nameEqual = (null == n ? null == theOther.n : n
						.equals(theOther.n));
				if (!nameEqual) {
					return false;
				}

				if (c != theOther.c) {
					return false;
				}

				boolean envmtEqual = (null == envmt ? null == theOther.envmt
						: envmt.equals(theOther.envmt));
				if (!envmtEqual) {
					return false;
				}

				return true;
			}
            return false;
		}
	}

	public static class MockObjectFactoryNoException implements ObjectFactory {

		public Object getObjectInstance(Object o, Name n, Context c,
				Hashtable<?, ?> envmt) throws Exception {
			return "MockObjectNoException";
		}
	}

	public static class MockContext implements Context {
		Hashtable<Object, Object> prop = null;

		@SuppressWarnings("unchecked")
        public MockContext(Hashtable<?, ?> prop) {
			this.prop = (Hashtable<Object, Object>)prop;
		}

		@Override
        public boolean equals(Object obj) {
			if (obj instanceof MockContext) {
				MockContext theOther = (MockContext) obj;
				boolean envmtEqual = (null == prop ? null == theOther.prop
						: prop.equals(theOther.prop));
				if (!envmtEqual) {
					return false;
				}

				return true;
			}
            return false;
		}

		public boolean parameterEquals(Object o, Name n, Context c, Hashtable<?, ?> h) {
			Hashtable<String, Object> r = new Hashtable<String, Object>();
			if (null != o) {
				r.put("o", o);
			}
			if (null != n) {
				r.put("n", n);
			}
			if (null != c) {
				r.put("c", c);
			}
			if (null != h) {
				r.put("h", h);
			}
			return r.equals(this.prop);
		}

		public Object addToEnvironment(String s, Object o)
				throws NamingException {
			return prop.put(s, o);
		}

		public void bind(Name n, Object o) throws NamingException {
		}

		public void bind(String s, Object o) throws NamingException {
		}

		public void close() throws NamingException {
		}

		public Name composeName(Name n, Name pfx) throws NamingException {
			return null;
		}

		public String composeName(String s, String pfx) throws NamingException {
			return null;
		}

		public Context createSubcontext(Name n) throws NamingException {
			return null;
		}

		public Context createSubcontext(String s) throws NamingException {
			return null;
		}

		public void destroySubcontext(Name n) throws NamingException {
		}

		public void destroySubcontext(String s) throws NamingException {
		}

		public Hashtable<?, ?> getEnvironment() throws NamingException {
			return prop;
		}

		public String getNameInNamespace() throws NamingException {
			return "MockNameSpace";
		}

		public NameParser getNameParser(Name n) throws NamingException {
			return null;
		}

		public NameParser getNameParser(String s) throws NamingException {
			return null;
		}

		public NamingEnumeration<NameClassPair> list(Name n) throws NamingException {
			return null;
		}

		public NamingEnumeration<NameClassPair> list(String s) throws NamingException {
			return null;
		}

		public NamingEnumeration<Binding> listBindings(Name n) throws NamingException {
			return null;
		}

		public NamingEnumeration<Binding> listBindings(String s) throws NamingException {
			return null;
		}

		public Object lookup(Name n) throws NamingException {
			return null;
		}

		public Object lookup(String s) throws NamingException {
			return null;
		}

		public Object lookupLink(Name n) throws NamingException {
			return null;
		}

		public Object lookupLink(String s) throws NamingException {
			return null;
		}

		public void rebind(Name n, Object o) throws NamingException {
		}

		public void rebind(String s, Object o) throws NamingException {
		}

		public Object removeFromEnvironment(String s) throws NamingException {
			return null;
		}

		public void rename(Name nOld, Name nNew) throws NamingException {
		}

		public void rename(String sOld, String sNew) throws NamingException {
		}

		public void unbind(Name n) throws NamingException {
		}

		public void unbind(String s) throws NamingException {
		}
	}

	public static class MagicMockStateFactory implements StateFactory {

		public Object getStateToBind(Object o, Name n, Context c, Hashtable<?, ?> h)
				throws NamingException {

			issueIndicatedExceptions(h);
			if (returnNullIndicated(h)) {
				return null;
			}

			Hashtable<String, Object> r = new Hashtable<String, Object>();
			if (null != o) {
				r.put("o", o);
			}
			if (null != n) {
				r.put("n", n);
			}
			if (null != c) {
				r.put("c", c);
			}
			if (null != h) {
				r.put("h", h);
			}
			return r;
		}
	}

	public static class MagicSuccessMockStateFactory implements StateFactory {

		public Object getStateToBind(Object o, Name n, Context c, Hashtable<?, ?> h)
				throws NamingException {

			Hashtable<String, Object> r = new Hashtable<String, Object>();
			if (null != o) {
				r.put("o", o);
			}
			if (null != n) {
				r.put("n", n);
			}
			if (null != c) {
				r.put("c", c);
			}
			if (null != h) {
				r.put("h", h);
			}
			return r;
		}
	}

	public static class MockReferenceable implements Referenceable {
		private Reference ref;

		public MockReferenceable() {
		}

		public MockReferenceable(Reference ref) {
			this.ref = ref;
		}

		public Reference getReference() {
			return ref;
		}
	}

	public static class MockRefAddr extends RefAddr {
        private static final long serialVersionUID = 1L;
        private String content;

		public MockRefAddr(String type, String content) {
			super(type);
			this.content = content;
		}

		@Override
        public Object getContent() {
			return content;
		}
	}

	public static class MockContextObjectFactory extends MockObjectFactory {
		Hashtable<?, ?> ht = null;

		@Override
        public Object getObjectInstance(Object o, Name n, Context c,
				Hashtable<?, ?> envmt) throws Exception {
			return new MockContext(envmt);
		}

		public MockContextObjectFactory(Hashtable<?, ?> envmt) {
			ht = envmt;
		}

		public MockContextObjectFactory() {
		}
	}
}

