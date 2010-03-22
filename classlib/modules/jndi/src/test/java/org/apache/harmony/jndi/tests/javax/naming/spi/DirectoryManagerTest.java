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
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.spi.DirStateFactory;
import javax.naming.spi.DirectoryManager;

import junit.framework.TestCase;

import org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest.MockRefAddr;
import org.apache.harmony.jndi.tests.javax.naming.spi.NamingManagerTest.MockReferenceable;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContext;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirContext2;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirContext3;

import com.sun.jndi.url.dir2.dir2URLContextFactory;


public class DirectoryManagerTest extends TestCase {



    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.STATE_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory");
        // env.put(
        // Context.INITIAL_CONTEXT_FACTORY,
        // "dazzle.jndi.testing.spi.DazzleContextFactory");
        NamingManagerTest.writeProviderResource(
                MockDirContext2.class.getName(), env);
    }


    /*
     * -------------------------------------------------------------------
     * Methods
     * -------------------------------------------------------------------
     */
    // public void testDefaultConstructor() {
    // 
    // // for coverage only, no meaning!
    // try {
    // DirectoryManager manager = new DirectoryManager();
    // } catch (Throwable t) {
    // }
    // }
    /*
     * ------------------------------------------------------- test
     * getObjectInstance -------------------------------------------------------
     */

    /**
     * When no factory builder is set and the fed object is Reference with a
     * valid factory name which works properly. Should return an object
     * successfully.
     * 
     * Try the same when the fed object is Referenceable.
     */
    public void testGetObjectInstance_NoBuilder_ReferenceValidFactory()
            throws NamingException {
        
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "dazzle.jndi.testing.spi.DazzleContextFactory");
        Reference r = new Reference(null,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirObjectFactory", null);

        Attributes a = new BasicAttributes();
        assertGetObjectResult(r, null, null, env, a);

        // test Referenceable
        MockReferenceable mr = new MockReferenceable(r);
        assertGetObjectResult(mr, null, null, env, a);
    }

    /**
     * @param r
     * @param object
     * @param object2
     * @param env
     * @param a
     * @return
     */
    private void assertGetObjectResult(Object o, Name n, Context c,
            Hashtable<String, String> h, Attributes a) throws NamingException {
        Object obj = null;
        try {
            obj = DirectoryManager.getObjectInstance(o, n, c, h, a);
        } catch (Exception e) {
            
            fail();
        }

        Hashtable<?, ?> t = (Hashtable<?, ?>) obj;
        if (o instanceof Referenceable) {
            assertSame(t.get("o"), ((Referenceable) o).getReference());
        } else {
            assertSame(t.get("o"), o);
        }
        assertSame(t.get("n"), n);
        assertSame(t.get("c"), c);
        assertSame(t.get("h"), h);
        assertSame(t.get("a"), a);

    }

    /**
     * When no factory builder is set and the fed object is Reference with an
     * invalid factory name. Should return the original object.
     * 
     * Try the same when the fed object is Referenceable.
     */
    public void testGetObjectInstance_NoBuilder_ReferenceInvalidFactory()
            throws Exception {
        
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "dazzle.jndi.testing.spi.DazzleContextFactory");
        Reference r = new Reference(null, "junk.factory", null);
        Attributes a = new BasicAttributes();
        Object obj = DirectoryManager.getObjectInstance(r, null, null, env, a);
        assertSame(r, obj);

        // test Referenceable
        MockReferenceable mr = new MockReferenceable(r);
        obj = DirectoryManager.getObjectInstance(mr, null, null, env, a);
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
        
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "dazzle.jndi.testing.spi.DazzleContextFactory");
        Reference r = new Reference(null,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirObjectFactory", null);
        NamingManagerTest.indicateNullPointerException(env);
        Attributes a = new BasicAttributes();
        try {
            DirectoryManager.getObjectInstance(r, null, null, env,
                    a);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // 
        }

        // test Referenceable
        MockReferenceable mr = new MockReferenceable(r);
        NamingManagerTest.indicateNamingException(env);
        try {
            DirectoryManager.getObjectInstance(mr, null, null,
                    env, a);
            fail("Should throw NamingException.");
        } catch (NamingException e) {
            // 
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
        
        Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "dazzle.jndi.testing.spi.DazzleContextFactory");
        Reference r = new Reference(null,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirObjectFactory", null);
        NamingManagerTest.indicateReturnNull(env);
        Attributes a = new BasicAttributes();
        Object obj = DirectoryManager.getObjectInstance(r, null, null, env, a);
        assertNull(obj);

        // test Referenceable
        MockReferenceable mr = new MockReferenceable(r);
        NamingManagerTest.indicateReturnNull(env);
        obj = DirectoryManager.getObjectInstance(mr, null, null, env, a);
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
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "dazzle.jndi.testing.spi.DazzleContextFactory");
        env.put(Context.URL_PKG_PREFIXES, "org.apache.harmony.jndi.tests.javax.naming.spi.mock");
        Reference r = new Reference(null);
        MockRefAddr mockAddr = new MockRefAddr("URL", "dire://www.apache.org/");
        r.add(mockAddr);
        mockAddr = new MockRefAddr(null, "dire://www.apache.org/");
        r.add(mockAddr);
        Attributes a = new BasicAttributes();

        Object obj = DirectoryManager.getObjectInstance(r, new CompositeName(
                "compositename"), new MockContext(new Hashtable<String, String>()), env, a);
        assertSame(obj, r);
    }

    /**
     * When no factory builder is set and the fed object is Reference with no
     * factory name, and there is a StringRefAddr whose type is null. Should
     * throw NullPointerException.
     */
    public void testGetObjectInstance_NoBuilder_ReferenceNullTypedStrAddr()
            throws Exception {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "dazzle.jndi.testing.spi.DazzleContextFactory");
        env.put(Context.URL_PKG_PREFIXES, "org.apache.harmony.jndi.tests.javax.naming.spi.mock");
        Reference r = new Reference(null);
        StringRefAddr nullTypeAddr = new StringRefAddr(null,
                "dire://www.apache.org/");
        r.add(nullTypeAddr);
        Attributes a = new BasicAttributes();

        try {
            DirectoryManager.getObjectInstance(r,
                    new CompositeName("compositename"), new MockContext(
                            new Hashtable<String, String>()), env, a);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // 
        }

        // test Referenceable
        MockReferenceable mr = new MockReferenceable(r);
        try {
            DirectoryManager.getObjectInstance(mr,
                    new CompositeName("compositename"), new MockContext(
                            new Hashtable<String, String>()), env, a);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // 
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
        // invalid StringRefAddr with wrong type
        StringRefAddr notUrlTypeAddr = new StringRefAddr("uurl",
                "dire://www.apache.org/");
        r.add(notUrlTypeAddr);
        // invalid StringRefAddr without scheme
        StringRefAddr noSchemeAddr = new StringRefAddr("URL", "www.apache.org");
        r.add(noSchemeAddr);
        // invalid StringRefAddr without relevant ObjectFactory
        StringRefAddr noFactorySchemeAddr = new StringRefAddr(url,
                "httpss://www.apache.org/");
        r.add(noFactorySchemeAddr);
        // invalid StringRefAddr without ObjectFactory which getObjectInstance()
        // method return null
        StringRefAddr returnNullFactoryAddr = new StringRefAddr(url,
                "news://www.apache.org/");
        r.add(returnNullFactoryAddr);
        // valid StringRefAddr, should call its related
        // DirObjectFactory.getObjectInstance(Object o,Name n,Context
        // c,Hashtable h,Attributes a)
        // the class of DirObjectFactory should be
        // javax.naming.spi.mock.dire.direURLContextFactory
        StringRefAddr validFactoryAddr = new StringRefAddr(url,
                "dire://www.apache.org/");
        r.add(validFactoryAddr);
        Attributes a = new BasicAttributes();
        Hashtable<String, String> temp = new Hashtable<String, String>();
        temp.put("mockkey", "mockobj");
        MockContext c = new MockContext(temp);
        Name n = new CompositeName("compositename");

        MockDirContext3 ctx = (MockDirContext3) DirectoryManager
                .getObjectInstance(r, n, c, env, a);

        boolean equals = ctx.parameterEquals(validFactoryAddr.getContent(), n,
                c, env, null); // it's NOT a!!
        assertTrue(equals);

        // test Referenceable
        MockReferenceable mr = new MockReferenceable(r);
        ctx = (MockDirContext3) DirectoryManager.getObjectInstance(mr, n, c,
                env, a);

        assertTrue(ctx.parameterEquals(validFactoryAddr.getContent(), n, c,
                env, null)); // it's NOT a!!
    }

    /*
     * pls. refer to comments of
     * myTestGetObjectInstance_NoBuilder_ReferenceValidURL(String)
     */
    public void testGetObjectInstance_NoBuilder_ReferenceValidURL_URL()
            throws Exception {
        myTestGetObjectInstance_NoBuilder_ReferenceValidURL("URL");
    }

    public void testGetObjectInstance_NoBuilder_ReferenceValidURL_url()
            throws Exception {
        myTestGetObjectInstance_NoBuilder_ReferenceValidURL("url");
    }

    /**
     * Test the default URL context factory: com.sun.jndi.url
     */
    public void testGetObjectInstance_NoBuilder_ReferenceDefaultURL()
            throws Exception {
        
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "dazzle.jndi.testing.spi.DazzleContextFactory");
        env.put(Context.URL_PKG_PREFIXES, "org.apache.harmony.jndi.tests.javax.naming.spi.mock");

        Reference r = new Reference(null);
        StringRefAddr addr = new StringRefAddr("url", "dir2://www.apache.org/");
        r.add(addr);
        Attributes a = new BasicAttributes();

        dir2URLContextFactory.MockObject obj = (dir2URLContextFactory.MockObject) DirectoryManager
                .getObjectInstance(r, new CompositeName("compositename"), null, env, a);

        assertEquals(obj,
                new dir2URLContextFactory.MockObject(addr.getContent(),
                        new CompositeName("compositename"), null, env, a));

        // test Referenceable
        MockReferenceable mr = new MockReferenceable(r);
        obj = (dir2URLContextFactory.MockObject) DirectoryManager
                .getObjectInstance(mr.getReference(), new CompositeName(
                        "compositename"), null, env, a);

        assertEquals(obj,
                new dir2URLContextFactory.MockObject(addr.getContent(),
                        new CompositeName("compositename"), null, env, a));
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
            Hashtable<String, String> env = new Hashtable<String, String>();

            Reference r = new Reference(null);
            StringRefAddr exceptionalFactoryAddr = new StringRefAddr(url,
                    "http://www.apache.org/");
            r.add(exceptionalFactoryAddr);
            StringRefAddr validFactoryAddr = new StringRefAddr(url,
                    "dire://www.apache.org/");
            r.add(validFactoryAddr);

            Hashtable<String, String> ctxEnv = new Hashtable<String, String>();
            /*
             * ctxEnv.put( Context.INITIAL_CONTEXT_FACTORY,
             * "dazzle.jndi.testing.spi.DazzleContextFactory");
             */
            // ctxEnv.put(Context.URL_PKG_PREFIXES,
            // "tests.api.javax.naming.spi.mock");
            Attributes a = new BasicAttributes();
            env
                    .put(Context.URL_PKG_PREFIXES,
                            "org.apache.harmony.jndi.tests.javax.naming.spi.mock");
            // NamingManagerTest.writeProviderResource(
            // "tests.api.javax.naming.spi.dummy",
            // ctxEnv);
            try {
                NamingManagerTest.indicateNullPointerException(env);
                DirectoryManager.getObjectInstance(r,
                        new CompositeName("compositename"), new MockContext(
                                ctxEnv), env, a);
                fail("Should throw NamingException.");
            } catch (NamingException e) {
                assertTrue(e.getRootCause() instanceof NullPointerException);
            }

            // test Referenceable
            MockReferenceable mr = new MockReferenceable(r);
            try {
                NamingManagerTest.indicateNamingException(env);
                DirectoryManager
                        .getObjectInstance(mr, new CompositeName(
                                "compositename"), new MockContext(ctxEnv), env,
                                a);

                fail("Should throw NamingException.");
            } catch (NamingException e) {
                assertNull(e.getRootCause());
            }
        } finally {
            NamingManagerTest.writeProviderResource(
                    "org.apache.harmony.jndi.tests.javax.naming.spi.dummy", new Hashtable<Object, Object>());
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
                    "dire://www.apache.org/");
            r.add(validFactoryAddr);

            Hashtable<String, String> ctxEnv = new Hashtable<String, String>();

            ctxEnv.put(Context.INITIAL_CONTEXT_FACTORY,
                    "dazzle.jndi.testing.spi.DazzleContextFactory");

            ctxEnv.put(Context.URL_PKG_PREFIXES,
                    "org.apache.harmony.jndi.tests.javax.naming.spi.mock");

            NamingManagerTest.writeProviderResource(
                    "org.apache.harmony.jndi.tests.javax.naming.spi.dummy", ctxEnv);
            Attributes a = new BasicAttributes();
            try {
                NamingManagerTest.indicateNullPointerException(ctxEnv);
                DirectoryManager.getObjectInstance(r,
                        new CompositeName("compositename"), new MockContext(
                                ctxEnv), ctxEnv, a);
                fail("Should throw NamingException.");
            } catch (NamingException e) {
                assertTrue(e.getRootCause() instanceof NullPointerException);
            }

            // test Referenceable
            MockReferenceable mr = new MockReferenceable(r);
            try {
                NamingManagerTest.indicateNamingException(ctxEnv);
                DirectoryManager
                        .getObjectInstance(mr, new CompositeName(
                                "compositename"), new MockContext(ctxEnv),
                                ctxEnv, a);
                fail("Should throw NamingException.");
            } catch (NamingException e) {
                assertNull(e.getRootCause());
            }
        } finally {
            NamingManagerTest.writeProviderResource(
                    "org.apache.harmony.jndi.tests.javax.naming.spi.dummy", new Hashtable<Object, Object>());
        }
    }

    public void testGetObjectInstance_NoBuilder_ReferenceExceptionalURL_URL()
            throws Exception {

        myTestGetObjectInstance_NoBuilder_ReferenceExceptionalURL("URL");
    }

    public void testGetObjectInstance_NoBuilder_ReferenceExceptionalURL_URL2()
            throws Exception {
        myTestGetObjectInstance_NoBuilder_ReferenceExceptionalURL2("URL");
    }

    public void testGetObjectInstance_NoBuilder_ReferenceExceptionalURL_url()
            throws Exception {
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
            throws NamingException, Throwable {
        
        try {

            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY,
                    "dazzle.jndi.testing.spi.DazzleContextFactory");
            env.put(Context.OBJECT_FACTORIES,
                    "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirObjectFactory");

            Hashtable<String, String> ctxEnv = new Hashtable<String, String>();

            ctxEnv
                    .put(Context.OBJECT_FACTORIES,
                            "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirObjectFactoryNoException");
            ctxEnv.put(Context.URL_PKG_PREFIXES,
                    "org.apache.harmony.jndi.tests.javax.naming.spi.mock");
            NamingManagerTest.writeProviderResource(
                    "org.apache.harmony.jndi.tests.javax.naming.spi.dummy", ctxEnv);
            Context ctx = new MockContext(ctxEnv);
            Attributes a = new BasicAttributes();
            assertGetObjectResult("Junk", new CompositeName("compositeName"),
                    ctx, env, a);
        } finally {
            NamingManagerTest.writeProviderResource(
                    "org.apache.harmony.jndi.tests.javax.naming.spi.dummy", new Hashtable<Object, Object>());
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
            throws Throwable {

        try {

            Hashtable<String, String> ctxEnv = new Hashtable<String, String>();
            ctxEnv
                    .put(
                            Context.OBJECT_FACTORIES,
                            ":org.apache.harmony.jndi.tests.javax.naming.spi.mock.news.newsURLContextFactory:"
                                    + "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirObjectFactory");
            ctxEnv.put(Context.URL_PKG_PREFIXES,
                    "org.apache.harmony.jndi.tests.javax.naming.spi.mock");
            NamingManagerTest.writeProviderResource(
                    "org.apache.harmony.jndi.tests.javax.naming.spi.dummy", ctxEnv);
            Attributes a = new BasicAttributes();
            Context ctx = new MockContext(ctxEnv);

            Hashtable<String, String> env = new Hashtable<String, String>();
            /*
             * env.put( Context.INITIAL_CONTEXT_FACTORY,
             * "dazzle.jndi.testing.spi.DazzleContextFactory");
             */
            env
                    .put(Context.URL_PKG_PREFIXES,
                            "org.apache.harmony.jndi.tests.javax.naming.spi.mock");

            Reference r = new Reference("");
            MockRefAddr mockAddr = new MockRefAddr("URL",
                    "dire://www.apache.org/");
            r.add(mockAddr);
            StringRefAddr notUrlTypeAddr = new StringRefAddr("uurl",
                    "dire://www.apache.org/");
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

            assertSame(r, DirectoryManager.getObjectInstance(r,
                    new CompositeName("compositename"), ctx, env, a));
            // assertGetObjectResult(
            // r,
            // new CompositeName("compositename"),
            // ctx,
            // env,
            // a);
        } finally {
            NamingManagerTest.writeProviderResource(
                    "org.apache.harmony.jndi.tests.javax.naming.spi.dummy", new Hashtable<Object, Object>());
        }
    }

    public void testGetObjectInstance_NoBuilder_NotRef_ValidFactoryWithNull_1()
            throws Exception {
        try {
            Hashtable<String, String> ctxEnv = new Hashtable<String, String>();
            NamingManagerTest.writeProviderResource(
                    "org.apache.harmony.jndi.tests.javax.naming.spi.dummy", ctxEnv);

            Context ctx = new MockContext(ctxEnv);

            Hashtable<String, String> env = new Hashtable<String, String>();
            env
                    .put(Context.URL_PKG_PREFIXES,
                            "org.apache.harmony.jndi.tests.javax.naming.spi.mock");
            env.put(Context.OBJECT_FACTORIES,
                    ":org.apache.harmony.jndi.tests.javax.naming.spi.mock.news.newsURLContextFactory");

            Reference r = new Reference("");
            MockRefAddr mockAddr = new MockRefAddr("URL",
                    "dire://www.apache.org/");
            r.add(mockAddr);
            StringRefAddr notUrlTypeAddr = new StringRefAddr("uurl",
                    "dire://www.apache.org/");
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
            Attributes a = new BasicAttributes();

            Object obj = DirectoryManager.getObjectInstance(r,
                    new CompositeName("compositename"), ctx, env, a);
            assertEquals(r, obj);
        } finally {
            NamingManagerTest.writeProviderResource(
                    "org.apache.harmony.jndi.tests.javax.naming.spi.dummy", new Hashtable<Object, Object>());
        }
    }

    public void testGetObjectInstance_NoBuilder_NotRef_ValidFactoryWithNull_2()
            throws InvalidNameException, Throwable {
        try {
            Hashtable<?, ?> ctxEnv = new Hashtable<Object, Object>();
            NamingManagerTest.writeProviderResource(
                    "org.apache.harmony.jndi.tests.javax.naming.spi.dummy", ctxEnv);

            DirContext ctx = new MockDirContext2(ctxEnv);

            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY,
                    "dazzle.jndi.testing.spi.DazzleContextFactory");
            env
                    .put(Context.URL_PKG_PREFIXES,
                            "org.apache.harmony.jndi.tests.javax.naming.spi.mock");
            env
                    .put(
                            Context.OBJECT_FACTORIES,
                            ":org.apache.harmony.jndi.tests.javax.naming.spi.mock.news.newsURLContextFactory"
                                    + ":org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirObjectFactory");

            Reference r = new Reference("");
            MockRefAddr mockAddr = new MockRefAddr("URL",
                    "dire://www.apache.org/");
            r.add(mockAddr);
            StringRefAddr notUrlTypeAddr = new StringRefAddr("uurl",
                    "dire://www.apache.org/");
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
            Attributes a = new BasicAttributes();
            assertGetObjectResult(r, new CompositeName("compositename"), ctx,
                    env, a);
        } finally {
            NamingManagerTest.writeProviderResource(
                    "org.apache.harmony.jndi.tests.javax.naming.spi.dummy", new Hashtable<Object, Object>());
        }
    }

    /**
     * When no factory builder is set, and all fed parameters are null. Should
     * return the original object.
     */
    public void testGetObjectInstance_NoBuilder_AllNull() throws Exception {
        
        Object obj = DirectoryManager.getObjectInstance(null, null, null, null,
                null);
        assertNull(obj);

        Object originalObject = "original object";
        obj = DirectoryManager.getObjectInstance(originalObject, null, null,
                null, null);
        assertSame(obj, originalObject);
    }

    /**
     * When no factory builder is set, and all fed parameters are null except
     * the original object and the environment properties. The environment
     * properties contains an invalid factory name, and a valid factory name
     * that follows. Should return an object created by the valid factory.
     */
    public void testGetObjectInstance_NoBuilder_NotRef_InvalidFactory()
            throws Throwable {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "dazzle.jndi.testing.spi.DazzleContextFactory");
        env.put(Context.OBJECT_FACTORIES, "junk.factory:"
                + "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirObjectFactory");

        assertGetObjectResult(null, null, null, env, null);

        assertGetObjectResult("abc", null, null, env, null);
    }

    /**
     * When no factory builder is set, and all fed parameters are null except
     * the original object and the environment properties. The environment
     * properties contains a valid factory that throws an exception, and a valid
     * factory name that follows. Should throw an exception.
     */
    public void testGetObjectInstance_NoBuilder_NotRef_ExceptionalFactory()
            throws Exception {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "dazzle.jndi.testing.spi.DazzleContextFactory");
        env
                .put(
                        Context.OBJECT_FACTORIES,
                        "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirObjectFactory:"
                                + "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirObjectFactoryNoException");
        Attributes a = new BasicAttributes();

        try {
            NamingManagerTest.indicateNullPointerException(env);
            DirectoryManager.getObjectInstance(null, null, null,
                    env, a);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        try {
            NamingManagerTest.indicateNamingException(env);
            DirectoryManager.getObjectInstance(null, null, null,
                    env, a);
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
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "dazzle.jndi.testing.spi.DazzleContextFactory");
        env.put(Context.OBJECT_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.news.newsURLContextFactory");
        Attributes a = new BasicAttributes();

        Object obj = DirectoryManager.getObjectInstance(null, null, null, env,
                a);
        assertNull(obj);

        Object obj2 = new Object();
        obj = DirectoryManager.getObjectInstance(obj2, null, null, env, a);
        assertSame(obj2, obj);
    }

    /*
     * ------------------------------------------------------- test
     * getStateToBind -------------------------------------------------------
     */

    public void testGetStateToBind_null_null_null_null_null()
            throws NamingException {
        
        Object o = null;
        Name n = null;
        Context c = null;
        Hashtable<?, ?> h = null;
        Attributes a = null;
        DirStateFactory.Result r = DirectoryManager.getStateToBind(o, n, c, h,
                a);
        assertNull(r.getObject());
        assertNull(r.getAttributes());
    }

    public void testGetStateToBind_null_null_null_null_attr()
            throws NamingException {
        
        Object o = null;
        Name n = null;
        Context c = null;
        Hashtable<?, ?> h = null;
        Attributes a = new BasicAttributes();
        DirStateFactory.Result r = DirectoryManager.getStateToBind(o, n, c, h,
                a);
        assertNull(r.getObject());
        assertSame(a, r.getAttributes());
    }

    public void testGetStateToBind_null_null_null_hash_null() {
        
        Object o = null;
        Name n = null;
        Context c = null;
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();
        Attributes a = null;
        h.put(Context.STATE_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory");
        try {
            assertGetStateResults(o, n, c, h, a);
        } catch (Throwable e) {
            fail();
        }
    }

    public void testGetStateToBind_null_null_null_hash_attr() {
        
        Object o = null;
        Name n = null;
        Context c = null;
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();
        Attributes a = new BasicAttributes();
        h.put(Context.STATE_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory");
        try {
            assertGetStateResults(o, n, c, h, a);
        } catch (Throwable e) {
            fail();
        }
    }

    private void assertGetStateResults(Object o, Name n, Context c,
            Hashtable<Object, Object> h, Attributes a) throws NamingException {
        DirStateFactory.Result r = DirectoryManager.getStateToBind(o, n, c, h,
                a);
        Hashtable<?, ?> t = (Hashtable<?, ?>) r.getObject();
        assertEquals(a, r.getAttributes());
        assertEquals(t.get("o"), o);
        assertEquals(t.get("n"), n);
        assertEquals(t.get("c"), c);
        assertEquals(t.get("h"), h);
        assertEquals(t.get("a"), a);
    }

    // public void testGetStateToBind_null_null_ctx_null_null() {
    // 
    // Object o = null;
    // Name n = null;
    // Context c = new MockDirContext2(new Hashtable<Object, Object>());
    // Hashtable h = null;
    // Attributes a = null;
    // try {
    // assertGetStateResults(o, n, c, h, a);
    // } catch (NamingException e) {
    // fail("NamingException occured");
    // }
    // }

    // public void testGetStateToBind_null_null_ctx_null_attr() {
    // 
    // Object o = null;
    // Name n = null;
    // Context c = new MockDirContext2(new Hashtable<Object, Object>());
    // Hashtable h = null;
    // Attributes a = new BasicAttributes();
    // try {
    // assertGetStateResults(o, n, c, h, a);
    // } catch (NamingException e) {
    // fail("NamingException occured");
    // }
    // }

    // public void testGetStateToBind_null_null_ctx_empty_null() {
    // 
    // Object o = null;
    // Name n = null;
    // Context c = new MockDirContext2(new Hashtable<Object, Object>());
    // // lead to state factory
    // Hashtable h = new Hashtable<Object, Object>();
    // Attributes a = null;
    //
    // try {
    // assertGetStateResults(o, n, c, h, a);
    // } catch (NamingException e) {
    // fail("NamingException occured");
    // }
    // }

    // public void testGetStateToBind_null_null_ctx_empty_attr() {
    // 
    // Object o = null;
    // Name n = null;
    // Context c = new MockDirContext2(new Hashtable<Object, Object>());
    // // lead to state factory
    // Hashtable h = new Hashtable<Object, Object>();
    // Attributes a = new BasicAttributes();
    //
    // try {
    // assertGetStateResults(o, n, c, h, a);
    // } catch (NamingException e) {
    // fail("NamingException occured");
    // }
    // }

    public void testGetStateToBind_null_name_ctx_hash_null() {
        
        Object o = null;
        Name n = new CompositeName();
        Context c = new MockDirContext2(new Hashtable<Object, Object>());
        // lead to state factory
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();
        Attributes a = null;
        h.put(Context.STATE_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory");
        try {
            assertGetStateResults(o, n, c, h, a);
        } catch (NamingException e) {
            fail("NamingException occured");
        }
    }

    public void testGetStateToBind_null_name_ctx_hash_attr() {
        
        Object o = null;
        Name n = new CompositeName();
        Context c = new MockDirContext2(new Hashtable<Object, Object>());
        // lead to state factory
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();
        Attributes a = new BasicAttributes();
        h.put(Context.STATE_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory");
        try {
            assertGetStateResults(o, n, c, h, a);
        } catch (NamingException e) {
            fail("NamingException occured");
        }
    }

    // public void testGetStateToBind_obj_name_ctx_empty_null() {
    // 
    // Object o = "object";
    // Name n = new CompositeName();
    // Context c = new MockDirContext2(new Hashtable<Object, Object>());
    // // lead to state factory
    // Hashtable h = new Hashtable<Object, Object>();
    // Attributes a = null;
    //
    // try {
    // assertGetStateResults(o, n, c, h, a);
    // } catch (NamingException e) {
    // fail("NamingException occured");
    // }
    // }

    // public void testGetStateToBind_obj_name_ctx_empty_attr() {
    // 
    // Object o = "object";
    // Name n = new CompositeName();
    // Context c = new MockDirContext2(new Hashtable<Object, Object>());
    // // lead to state factory
    // Hashtable h = new Hashtable<Object, Object>();
    // Attributes a = new BasicAttributes();
    //
    // try {
    // assertGetStateResults(o, n, c, h, a);
    // } catch (NamingException e) {
    // fail("NamingException occured");
    // }
    // }

    // public void testGetStateToBind_obj_name_ctx_empty2_null() {
    // 
    // Object o = "object";
    // Name n = new CompositeName();
    // Context c = new MockContext(new Hashtable<Object, Object>()); // no state factory
    // Hashtable h = new Hashtable<Object, Object>();
    // Attributes a = null;
    //
    // try {
    // assertGetStateResults(o, n, c, h, a);
    // } catch (NamingException e) {
    // fail("NamingException occured");
    // }
    // }

    // public void testGetStateToBind_obj_name_ctx_empty2_attr() {
    // 
    // Object o = "object";
    // Name n = new CompositeName();
    // Context c = new MockContext(new Hashtable<Object, Object>()); // no state factory
    // Hashtable h = new Hashtable<Object, Object>();
    // Attributes a = new BasicAttributes();
    //
    // try {
    // assertGetStateResults(o, n, c, h, a);
    // } catch (NamingException e) {
    // fail("NamingException occured");
    // }
    // }

    public void testGetStateToBind_obj_name_ctx_hash_null() {
        
        Object o = "object";
        Name n = new CompositeName();
        Context c = new MockDirContext2(new Hashtable<Object, Object>());
        // lead to state factory
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();
        Attributes a = null;
        h.put(Context.STATE_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory");

        try {
            assertGetStateResults(o, n, c, h, a);
        } catch (NamingException e) {
            fail("NamingException occured");
        }
    }

    public void testGetStateToBind_obj_name_ctx_hash_attr() {
        
        Object o = "object";
        Name n = new CompositeName();
        Context c = new MockDirContext2(new Hashtable<Object, Object>());
        // lead to state factory
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();
        Attributes a = new BasicAttributes();
        h.put(Context.STATE_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory");

        try {
            assertGetStateResults(o, n, c, h, a);
        } catch (NamingException e) {
            fail("NamingException occured");
        }
    }

    public void testGetStateToBind_f1BadClassName_Success() {
        
        Object o = "object";
        Name n = new CompositeName();
        Context c = new MockContext(new Hashtable<String, String>()); // no state factory
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();
        Attributes a = null;
        h.put(Context.STATE_FACTORIES, "bad.class.Name" + ":"
                + "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory");

        try {
            assertGetStateResults(o, n, c, h, a);
        } catch (NamingException e) {
            fail("NamingException occured");
        }
    }

    // public void testGetStateToBind_f2Success() {
    // 
    // Object o = "object";
    // Name n = new CompositeName();
    // Context c = new MockDirContext2(new Hashtable<Object, Object>());
    // // lead to state factory
    // Hashtable h = new Hashtable<Object, Object>();
    // Attributes a = null;
    // try {
    // Object ro = DirectoryManager.getStateToBind(o, n, c, h, a);
    // assertGetStateResults(o, n, c, h, a);
    // } catch (NamingException e) {
    // fail("NamingException occured");
    // }
    // }

    // public void testGetStateToBind_f1BadClassName_f2Success() {
    // 
    // Object o = "object";
    // Name n = new CompositeName();
    // Context c = new MockDirContext2(new Hashtable<Object, Object>());
    // // lead to state factory
    // Hashtable h = new Hashtable<Object, Object>();
    // h.put(Context.STATE_FACTORIES, "bad.class.Name");
    // Attributes a = null;
    // try {
    // Object ro = DirectoryManager.getStateToBind(o, n, c, h, a);
    // assertGetStateResults(o, n, c, h, a);
    // } catch (NamingException e) {
    // fail("NamingException occured");
    // }
    // }

    public void testGetStateToBind_f1NamingException_f2Success() {
        
        Object o = "object";
        Name n = new CompositeName();
        Context c = new MockDirContext2(new Hashtable<Object, Object>());
        // lead to state factory
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();
        Attributes a = null;
        h.put(Context.STATE_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory");
        NamingManagerTest.indicateNamingException(h);
        try {
            assertGetStateResults(o, n, c, h, a);
            fail();
        } catch (NamingException e) {
            assertTrue(e.getMessage().indexOf("Simulated") >= 0);
        }
    }

    public void testGetStateToBind_f1RuntimeException_f2Success() {
        
        Object o = "object";
        Name n = new CompositeName();
        Context c = new MockDirContext2(new Hashtable<Object, Object>());
        // lead to state factory
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();
        Attributes a = null;
        h.put(Context.STATE_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory");
        NamingManagerTest.indicateRuntimeException(h);

        try {
            assertGetStateResults(o, n, c, h, a);
            fail();
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().indexOf("Simulated") >= 0);
        } catch (NamingException e) {
            fail("should throw runtime exception");
        }
    }

    public void testGetStateToBind_f1ReturnNull_Success() {
        
        Object o = "object";
        Name n = new CompositeName();
        Context c = new MockContext(new Hashtable<String, String>()); // no state factory
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();
        Attributes a = null;
        h
                .put(
                        Context.STATE_FACTORIES,
                        "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory"
                                + ":"
                                + "org.apache.harmony.jndi.tests.javax.naming.spi.mock.SuccessMockDirStateFactory");
        NamingManagerTest.indicateReturnNull(h);
        try {
            assertGetStateResults(o, n, c, h, a);
        } catch (NamingException e) {
            fail("NamingException occured");
        }
    }

    public void testGetStateToBind_f1ReturnNull_f2Success() {
        
        Object o = "object";
        Name n = new CompositeName();
        Context c = new MockDirContext2(new Hashtable<Object, Object>());
        // lead to state factory
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();
        Attributes a = null;
        h.put(Context.STATE_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory");
        NamingManagerTest.indicateReturnNull(h);

        try {
            DirectoryManager.getStateToBind(o, n, c, h, a);
            assertGetStateResults(o, n, c, h, a);
        } catch (NamingException e) {
            fail("NamingException occured");
        }
    }

    public void testGetStateToBind_f1Success_f2Success() {
        
        Object o = "object";
        Name n = new CompositeName();
        Context c = new MockDirContext2(new Hashtable<Object, Object>());
        // lead to state factory
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();
        Attributes a = null;
        h.put(Context.STATE_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.SuccessMockDirStateFactory");
        NamingManagerTest.indicateRuntimeException(h);

        try {
            assertGetStateResults(o, n, c, h, a);
        } catch (NamingException e) {
            fail("NamingException occured");
        }
    }

    public void testGetStateToBind_f1ReturnNull() throws NamingException {
        
        Object o = "object";
        Name n = new CompositeName();
        Context c = new MockContext(new Hashtable<String, String>()); // no state factory
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();
        Attributes a = null;
        h.put(Context.STATE_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory");
        NamingManagerTest.indicateReturnNull(h);
        DirectoryManager.getStateToBind(o, n,
                c, h, a);
        assertGetStateResults(o, n, c, h, a);
    }

    // public void testGetStateToBind_f1BadClassName() {
    // 
    // Object o = "object";
    // Name n = new CompositeName();
    // Context c = new MockContext(new Hashtable<Object, Object>()); // no state factory
    // Hashtable h = new Hashtable<Object, Object>();
    // Attributes a = null;
    // h.put(Context.STATE_FACTORIES, "bad.class.Name");
    //
    // try {
    // assertGetStateResults(o, n, c, h, a);
    // } catch (NamingException e) {
    // fail("NamingException occured");
    // }
    // }

    public void testGetStateToBind_f1NamingException() {
        
        Object o = "object";
        Name n = new CompositeName();
        Context c = new MockContext(new Hashtable<String, String>()); // no state factory
        Hashtable<String, String> h = new Hashtable<String, String>();
        Attributes a = null;
        h.put(Context.STATE_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory");
        NamingManagerTest.indicateNamingException(h);

        try {
            DirectoryManager.getStateToBind(o, n, c, h, a);
            fail();
        } catch (NamingException e) {
            assertTrue(e.getMessage().indexOf("Simulated") >= 0);
        }
    }

    public void testGetStateToBind_f1RuntimeException() {
        
        Object o = "object";
        Name n = new CompositeName();
        Context c = new MockContext(new Hashtable<String, String>()); // no state factory
        Hashtable<Object, Object> h = new Hashtable<Object, Object>();
        Attributes a = null;
        h.put(Context.STATE_FACTORIES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockDirStateFactory");
        NamingManagerTest.indicateRuntimeException(h);

        try {
            DirectoryManager.getStateToBind(o, n, c, h, a);
            fail("should throw RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().indexOf("Simulated") >= 0);
        } catch (Throwable e) {
            fail("should throw RuntimeException");
        }
    }

    // public void testGetContinuationContext_MockDirContext2_null_null_null() {
    // log.setMethod(
    // "testGetContinuationContext_MockDirContext2_null_null_null()");
    //
    // CannotProceedException cpe = new CannotProceedException();
    // Object resolvedObj = new MockDirContext2(new Hashtable<Object, Object>());
    // cpe.setResolvedObj(resolvedObj);
    //
    // try {
    // DirContext r = DirectoryManager.getContinuationDirContext(cpe);
    // log.log(r.toString());
    // assertSame(cpe, cpe.getEnvironment().get(DirectoryManager.CPE));
    // assertSame(resolvedObj, r);
    // } catch (Throwable t) {
    // logThrowable(t);
    // fail();
    // }
    // }
    //
    // public void testGetContinuationContext_null_null_null_h() {
    // 
    // Hashtable h = new Hashtable<Object, Object>();
    // CannotProceedException cpe = new CannotProceedException();
    // h.put(
    // Context.OBJECT_FACTORIES,
    // "tests.api.javax.naming.mock.MockDirContextObjectFactory");
    // cpe.setEnvironment(h);
    // try {
    // DirContext r = DirectoryManager.getContinuationDirContext(cpe);
    // log.log(r.toString());
    // assertSame(cpe, cpe.getEnvironment().get(DirectoryManager.CPE));
    // assertSame(r, MockDirContextObjectFactory.DIR_CONTEXT);
    // } catch (NamingException t) {
    // logThrowable(t);
    // }
    // }
    //
    // public void testGetContinuationContext_null_null_null_null() {
    // 
    // CannotProceedException cpe = new CannotProceedException();
    // try {
    // DirContext r = DirectoryManager.getContinuationDirContext(cpe);
    // log.log(r.toString());
    // } catch (NamingException t) {
    // logThrowable(t);
    // fail();
    // }
    // }
    //
    // public void testGetContinuationContext_null_null_null_wrongh() {
    // 
    // CannotProceedException cpe = new CannotProceedException();
    // Hashtable h = new Hashtable<Object, Object>();
    // h.put(
    // Context.OBJECT_FACTORIES,
    // "tests.api.javax.naming.spi.NamingManagerTest$MockObjectFactory");
    // cpe.setEnvironment(h);
    // try {
    // Context r = DirectoryManager.getContinuationDirContext(cpe);
    // fail();
    // } catch (NamingException e) {
    // logThrowable(e);
    // }
    // }
    //
    // public void testGetContinuationContext_null_null_null_badnameh() {
    // 
    // CannotProceedException cpe = new CannotProceedException();
    // Hashtable h = new Hashtable<Object, Object>();
    // h.put(Context.OBJECT_FACTORIES, "bad name: javax.naming.spi.Test");
    // cpe.setEnvironment(h);
    // try {
    // Context r = DirectoryManager.getContinuationDirContext(cpe);
    // fail();
    // } catch (NamingException e) {
    // logThrowable(e);
    // }
    // }

}
