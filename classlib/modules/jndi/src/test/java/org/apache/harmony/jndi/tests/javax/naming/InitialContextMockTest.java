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

import java.util.Hashtable;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;

import junit.framework.TestCase;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.DazzleActionController;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.InvokeRecord;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockActionController;
import org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContext;
import org.apache.harmony.jndi.tests.javax.naming.util.Log;
import org.apache.harmony.jndi.tests.javax.naming.util.Person;

public class InitialContextMockTest extends TestCase {

    static Log log = new Log(InitialContextMockTest.class);

    private Context gContext;

    private final String urlSchema = "http";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContextFactory");
        env.put(Context.URL_PKG_PREFIXES,
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock");
        gContext = new InitialContext(env);

    }

    public void testAddToEnvironment() throws NamingException {
        Object value = gContext.addToEnvironment(
                Context.INITIAL_CONTEXT_FACTORY, "");

        assertEquals(
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContextFactory",
                value);
        assertEquals("", gContext.getEnvironment().get(
                Context.INITIAL_CONTEXT_FACTORY));
    }

    public void testAddToEnvironment_batchsize() throws NamingException {
        Integer batchSize = new Integer(100);
        gContext.addToEnvironment(Context.BATCHSIZE, batchSize);
        assertEquals(batchSize, gContext.getEnvironment()
                .get(Context.BATCHSIZE));
    }

    public void testAddToEnvironment_applet() throws NamingException {
        log.setMethod("testAddToEnvironment_applet");

        String applet = "java.applet.Applet";
        gContext.addToEnvironment(Context.APPLET, applet);
        assertEquals(applet, gContext.getEnvironment().get(Context.APPLET));
    }

    public void testBind() throws NamingException {
        Person person = Person.getInstance();
        Name name = new CompositeName(person.getName());
        gContext.bind(name, person);
        assertTrue(InvokeRecord.equals(null, name, person));
    }

    public void testBind_namenull() throws NamingException {
        String strObj = "bind object";
        try {
            gContext.bind((Name) null, strObj);
            fail("should throw NullPointerException!");
        } catch (NullPointerException e) {}
    }

    public void testBind_objectnull() throws NamingException {
        Name name = new CompositeName("bindname");
        gContext.bind(name, null);
        assertTrue(InvokeRecord.equals(null, name, null));
    }

    public void testBind_url() throws NamingException {
        Person person = Person.getInstance();
        Name name = new CompositeName("'http://www.apache.org/foundation'");
        gContext.bind(name, person);
        assertTrue(InvokeRecord.equals(urlSchema, name, person));
    }

    public void testBind_name_empty() throws NamingException {
        log.setMethod("testBind_name_empty");
        Person person = Person.getInstance();
        Name name = new CompositeName("");
        gContext.bind(name, person);
        assertTrue(InvokeRecord.equals(null, name, person));
    }

    public void testBind_runtimeException() throws NamingException {
        log.setMethod("testBind_runtimeException");

        MockActionController actionController = new MockActionController();
        actionController.addAction(
                DazzleActionController.THROW_RUNTIMEEXCEPTION, "1");
        MockContext.setActionController(actionController);

        Person person = Person.getInstance();
        Name name = new CompositeName(person.getName());
        try {
            gContext.bind(name, person);
            fail("Should throw RuntimeException.");
        } catch (RuntimeException e) {
            // log.log(e.getClass().getName());
            // log.log(e.toString());
        }
    }

    public void testBind_nullPointerException() throws NamingException {
        log.setMethod("testBind_nullPointerException");

        MockActionController actionController = new MockActionController();
        actionController.addAction(
                DazzleActionController.THROW_NULLPOINTEREXCEPTION, "1");
        MockContext.setActionController(actionController);

        Person person = Person.getInstance();
        Name name = new CompositeName(person.getName());
        try {
            gContext.bind(name, person);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // log.log(e.getClass().getName());
            // log.log(e.toString());
        }
    }

    public void testComposeName_name_empty() throws NamingException {
        log.setMethod("testComposeName_name_empty");
        Name name = new CompositeName("hmy");
        Name pfx = new CompositeName("");
        gContext.composeName(name, pfx);
        // assertFalse(InvokeRecord.equals(null, name, pfx));
    }

    public void testComposeName_name_validName() throws NamingException {
        log.setMethod("testComposeName_name_validName");
        Name name = new CompositeName("validname");
        Name pfx = new CompositeName("");
        gContext.composeName(name, pfx);
        // assertTrue(InvokeRecord.equals(null, name, pfx));
    }

    public void testComposeName_name_InvalidName() throws NamingException {
        log.setMethod("testComposeName_name_InvalidName");
        Name name = new CompositeName("Invalidname1");
        Name pfx = new CompositeName("InvalidName2");
        gContext.composeName(name, pfx);
        // assertTrue(InvokeRecord.equals(null, name, pfx));
    }
    
    public void testComposeName_name_pfx_null() throws NamingException {
        log.setMethod("testComposeName_name_pfx_null");
        Name name = new CompositeName("namepfxnull");
        gContext.composeName(name, null);
        // assertFalse(InvokeRecord.equals(null, name, null));
    }

    /**
     * @tests javax.naming.InitialContext#composeName(Name,Name)
     */
    public void testComposeNameLjavax_naming_NameLjavax_naming_Name()
            throws NamingException {
        log.setMethod("testComposeName_string_null"); //$NON-NLS-1$
        InitialContext initialContext = new InitialContext();

        try {
            initialContext.composeName((CompositeName) null,
                    (CompositeName) null);
            fail("Should throw NullPointerException"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
        
        try {
            initialContext.composeName(null, new CompositeName("prefix")); //$NON-NLS-1$
            fail("Should throw NullPointerException"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
        
        Name result = initialContext.composeName(
                new CompositeName("a/b/c"), (CompositeName) null); //$NON-NLS-1$
        assertEquals("a/b/c", result.toString()); //$NON-NLS-1$

        result = initialContext.composeName(
                new CompositeName("a/b/c"), new CompositeName("")); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("a/b/c", result.toString()); //$NON-NLS-1$
        
        result = initialContext.composeName(
                new CompositeName("a/b/c"), new CompositeName("prefix")); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("a/b/c", result.toString()); //$NON-NLS-1$

        result = initialContext.composeName(
                new CompositeName("testString"), new CompositeName("a/b/c/d")); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("testString", result.toString()); //$NON-NLS-1$
        
        CompositeName cn = new CompositeName("a/b/c"); //$NON-NLS-1$
        result = initialContext.composeName(cn, new CompositeName("prefix")); //$NON-NLS-1$
        cn.add("/d"); //$NON-NLS-1$
        assertEquals("a/b/c", result.toString()); //$NON-NLS-1$
    }
    
    /**
     * @tests javax.naming.InitialContext#composeName(String,String)
     */
    public void testComposeNameLjava_lang_StringLjava_lang_String()
            throws NamingException {
        log.setMethod("testComposeName_string_null"); //$NON-NLS-1$
        InitialContext initialContext = new InitialContext();

        String result = initialContext.composeName((String) null, (String) null);
        assertNull(result);

        result = initialContext.composeName((String) null, ""); //$NON-NLS-1$
        assertNull(result);

        result = initialContext.composeName("a/b/c", (String) null); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("a/b/c", result); //$NON-NLS-1$
        
        result = initialContext.composeName("a/b/c", ""); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("a/b/c", result); //$NON-NLS-1$
        
        result = initialContext.composeName("a/b/c", "prefix"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("a/b/c", result); //$NON-NLS-1$

        result = initialContext.composeName("testString", "a/b/c/d"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("testString", result); //$NON-NLS-1$
    }

    public void testComposeName_string_pfx_null() throws NamingException {
        log.setMethod("testComposeName_string_pfx_null");
        String name = "stringpfxnull";
        String pfx = null;
        gContext.composeName(name, pfx);
        // assertFalse(InvokeRecord.equals(null, name, null));
    }

    public void testComposeName_string_return() throws NamingException {
        String name = "child_str";
        String pfx = "parent_str";
        String composeName = gContext.composeName(name, pfx);
        assertNotNull(composeName);
    }

    public void testComposeName_name_return() throws NamingException {
        Name name = new CompositeName("child_name");
        Name pfx = new CompositeName("parent_name");
        Name composeName = gContext.composeName(name, pfx);
        assertNotNull(composeName);
    }

    public void testCreateSubcontext() throws NamingException {
        Name name = new CompositeName("hmy");
        gContext.createSubcontext(name);
        assertTrue(InvokeRecord.equals(null, name));
    }

    public void testCreateSubcontext_null() throws NamingException {
        log.setMethod("testCreateSubcontext_null");
        try {
            gContext.createSubcontext((Name) null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // log.log(e.toString());
        }
        // assertTrue(InvokeRecord.equals(null, null));
    }

    public void testCreateSubcontext_url() throws NamingException {
        String name = "http://www.apache.org/foundation";
        gContext.createSubcontext(name);
        assertTrue(InvokeRecord.equals(urlSchema, name));
    }

    public void testCreateSubcontext_runtimeException() throws NamingException {
        log.setMethod("testCreateSubcontext_runtimeException");

        MockActionController actionController = new MockActionController();
        actionController.addAction(
                DazzleActionController.THROW_RUNTIMEEXCEPTION, "1");
        MockContext.setActionController(actionController);

        Name name = new CompositeName("hmy");
        try {
            gContext.createSubcontext(name);
            fail("Should throw RuntimeException.");
        } catch (RuntimeException e) {
            // log.log(e.getClass().getName());
            // log.log(e.toString());
        }
    }

    public void testDestroySubcontext() throws NamingException {
        Name name = new CompositeName("hmy");
        gContext.destroySubcontext(name);
        assertTrue(InvokeRecord.equals(null, name));
    }

    public void testDestroySubcontext_null() throws NamingException {
        log.setMethod("testDestroySubcontext_null");
        try {
            gContext.destroySubcontext((Name) null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // log.log(e.toString());
        }
        // assertTrue(InvokeRecord.equals(null, null));
    }

    public void testDestroySubcontext_string_null() throws NamingException {
        log.setMethod("testDestroySubcontext_string_null");
        try {
            gContext.destroySubcontext((String) null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // log.log(e.toString());
        }
        // assertTrue(InvokeRecord.equals(null, null));
    }

    public void testDestroySubcontext_url() throws NamingException {
        String name = "http://ww.apache.org/foundation";
        gContext.destroySubcontext(name);
        assertTrue(InvokeRecord.equals(urlSchema, name));
    }

    public void testGetNameInNamespace() throws NamingException {
        MockActionController controller = new MockActionController();
        controller.addAction(DazzleActionController.RETURN_NULL, "1");
        MockContext.setActionController(controller);
        String nameSpace = gContext.getNameInNamespace();
        assertNull(nameSpace);
    }

    public void testGetNameParser() throws NamingException {
        Name name = new CompositeName("hmy");
        gContext.getNameParser(name);
        assertTrue(InvokeRecord.equals(null, name));
    }

    public void testGetNameParser_null() throws NamingException {
        log.setMethod("testGetNameParser_null");
        try {
            gContext.getNameParser((Name) null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // log.log(e.toString());
        }
        // assertTrue(InvokeRecord.equals(null, null));
    }

    public void testGetNameParser_url() throws NamingException {
        String name = "http://www.apache.org/foundation";
        gContext.getNameParser(name);
        assertTrue(InvokeRecord.equals(urlSchema, name));
    }

    public void testList() throws NamingException {
        Name name = new CompositeName("hmy");
        gContext.list(name);
        assertTrue(InvokeRecord.equals(null, name));
    }

    public void testList_null() throws NamingException {
        log.setMethod("testList_null");
        Name name = null;
        try {
            gContext.list(name);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // log.log(e.toString());
        }
        // assertTrue(InvokeRecord.equals(null, name));
    }

    public void testList_url() throws NamingException {
        Name name = new CompositeName("'http://www.apache.org/foundation'");
        gContext.list(name);
        assertTrue(InvokeRecord.equals(urlSchema, name));
    }

    public void testListBindings() throws NamingException {
        Name name = new CompositeName("hmy");
        gContext.listBindings(name);
        assertTrue(InvokeRecord.equals(null, name));
    }

    public void testListBindings_stringnull() throws NamingException {
        log.setMethod("testListBindings_stringnull");
        String name = null;
        try {
            gContext.listBindings(name);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // log.log(e.toString());
        }
        // assertTrue(InvokeRecord.equals(null, name));
    }

    public void testListBindings_url() throws NamingException {
        Name name = new CompositeName("'http://www.apache.org/foundation'");
        gContext.listBindings(name);
        assertTrue(InvokeRecord.equals(urlSchema, name));
    }

    public void testLookup() throws NamingException {
        Name name = new CompositeName("hmy");
        gContext.lookup(name);
        assertTrue(InvokeRecord.equals(null, name));
    }

    public void testLookup_null() throws NamingException {
        log.setMethod("testLookup_null");

        Name name = null;
        try {
            gContext.lookup(name);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // log.log(e.toString());
        }
        // assertTrue(InvokeRecord.equals(null, name));
    }

    public void testLookup_url() throws NamingException {
        Name name = new CompositeName("'http://www.apache.org/foundation'");
        gContext.lookup(name);
        assertTrue(InvokeRecord.equals(urlSchema, name));
    }

    public void testLookup_string_url() throws NamingException {
        log.setMethod("testLookup_string_url");

        String name = "'http://www.apache.org/foundation'";
        gContext.lookup(name);
        assertTrue(InvokeRecord.equals(null, name));
    }

    public void testLookup_string_empty() throws NamingException {
        String name = "";
        gContext.lookup(name);
        assertTrue(InvokeRecord.equals(null, name));
    }

    public void testLookupLink_url() throws NamingException {
        Name name = new CompositeName("'http://www.apache.org/foundation'");
        gContext.lookupLink(name);
        assertTrue(InvokeRecord.equals(urlSchema, name));
    }

    public void testLookupLink() throws NamingException {
        String name = "hmy";
        gContext.lookupLink(name);
        assertTrue(InvokeRecord.equals(null, name));
    }

    public void testLookupLink_null() throws NamingException {
        log.setMethod("testLookupLink_null");

        String name = null;
        try {
            gContext.lookupLink(name);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // log.log(e.toString());
        }
        // assertTrue(InvokeRecord.equals(null, name));
    }

    public void testRebind() throws NamingException {
        Name name = new CompositeName("hmy");
        Person person = Person.getInstance();
        gContext.rebind(name, person);
        assertTrue(InvokeRecord.equals(null, name, person));
    }

    public void testRebind_objectnull() throws NamingException {
        Name name = new CompositeName("hmy");
        gContext.rebind(name, null);
        assertTrue(InvokeRecord.equals(null, name, null));
    }

    public void testRebind_namenull() throws NamingException {
        log.setMethod("testRebind_namenull");
        Name name = null;
        Person person = Person.getInstance();
        try {
            gContext.rebind(name, person);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // log.log(e.toString());
        }
        // assertTrue(InvokeRecord.equals(null, name, person));
    }

    public void testRebind_url() throws NamingException {
        String name = "http://www.apache.org/foundation";
        Person person = Person.getInstance();
        gContext.rebind(name, person);
        assertTrue(InvokeRecord.equals(urlSchema, name, person));
    }

    public void testRemoveFromEnvironment() throws NamingException {
        String name = Context.INITIAL_CONTEXT_FACTORY;
        assertEquals(
                "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContextFactory",
                gContext.removeFromEnvironment(name));

    }

    public void testReName() throws NamingException {
        String oldName = "apache";
        String newName = "harmony";
        gContext.rename(oldName, newName);

        assertTrue(InvokeRecord.equals(null, oldName, newName));
    }

    public void testReName_null() throws NamingException {
        log.setMethod("testReName_null");
        String oldName = null;
        String newName = null;
        try {
            gContext.rename(oldName, newName);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // log.log(e.toString());
        }
        // assertTrue(InvokeRecord.equals(null, oldName, newName));
    }

    public void testReName_url_oldName() throws NamingException {
        Name oldName = new CompositeName("'http://www.apache.org/index.html'");
        Name newName = new CompositeName("harmony");
        gContext.rename(oldName, newName);
        assertTrue(InvokeRecord.equals(urlSchema, oldName, newName));
    }

    public void testReName_url_newName() throws NamingException {
        log.setMethod("testReName_url_newName");
        Name oldName = new CompositeName("harmony");
        Name newName = new CompositeName("'http://www.apache.org/index.html'");
        gContext.rename(oldName, newName);

        assertTrue(InvokeRecord.equals(null, oldName, newName));
    }

    public void testReName_newname_empty() throws NamingException {
        log.setMethod("testReName_newname_empty");
        Name oldName = new CompositeName("hmy");
        Name newName = new CompositeName("");
        gContext.rename(oldName, newName);
        assertTrue(InvokeRecord.equals(null, oldName, newName));
    }

    public void testReName_oldname_empty() throws NamingException {
        log.setMethod("testReName_oldname_empty");
        Name oldName = new CompositeName("");
        Name newName = new CompositeName("hmy");
        gContext.rename(oldName, newName);
        assertTrue(InvokeRecord.equals(null, oldName, newName));
    }

    public void testUnbind() throws NamingException {
        Name name = new CompositeName("hmy");
        gContext.unbind(name);
        assertTrue(InvokeRecord.equals(null, name));
    }

    public void testUnbind_null() throws NamingException {
        log.setMethod("testUnbind_null");
        String name = null;
        try {
            gContext.unbind(name);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // log.log(e.toString());
        }
        // assertTrue(InvokeRecord.equals(null, name));
    }

    public void testUnbind_url() throws NamingException {
        Name name = new CompositeName("'http://www.apache.org'");
        gContext.unbind(name);
        assertTrue(InvokeRecord.equals(urlSchema, name));
    }

    public void testGetNameParser_string() throws NamingException {
        String name1 = "sub1";
        gContext.createSubcontext(name1);

        String name = "";
        gContext.getNameParser(name);
        assertTrue(InvokeRecord.equals(null, name));

        gContext.getNameParser(name1);
        assertTrue(InvokeRecord.equals(null, name1));
    }

    public void testGetNameParser_string_url() throws NamingException {
        gContext.createSubcontext("sub1");
        String name = "http://www.apache.org";
        gContext.getNameParser(name);
        assertTrue(InvokeRecord.equals(urlSchema, name));
    }

    public void testGetNameParser_name() throws NamingException {
        Name name1 = new CompositeName("sub1");
        gContext.createSubcontext(name1);
        Name name = new CompositeName("");
        gContext.getNameParser(name);
        assertTrue(InvokeRecord.equals(null, name));

        gContext.getNameParser(name1);
        assertTrue(InvokeRecord.equals(null, name1));
    }

    public void testGetNameParser_name_url() throws NamingException {
        gContext.createSubcontext("sub1");
        Name name = new CompositeName("'http://www.apache.org'");
        gContext.getNameParser(name);
        assertTrue(InvokeRecord.equals(urlSchema, name));
    }

    public void testInvalidFactory() throws NamingException {
        log.setMethod("testInvalidFactory");

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContext");
        try {
            new InitialContext(env);
            fail("Should throw NoInitialContextException");
        } catch (NoInitialContextException e) {}

        env.put(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContext12345");
        try {
            new InitialContext(env);
            fail("Should throw NoInitialContextException");
        } catch (NoInitialContextException e) {}
    }

    public void testDefaultConstructor() throws NamingException {
        System
                .setProperty(Context.INITIAL_CONTEXT_FACTORY,
                        "org.apache.harmony.jndi.tests.javax.naming.spi.mock.MockContextFactory");
        InitialContext context = new InitialContext();
        context.bind("name", "object");
        assertTrue(InvokeRecord.equals(null, "name", "object"));
    }

    public void testClose() throws NamingException {
        gContext.close();
        assertTrue(InvokeRecord.equals(null, "close"));
        // regression test for HARMONY-1022
        new InitialContext().close();
        new InitialContext(null).close();
    }
}
