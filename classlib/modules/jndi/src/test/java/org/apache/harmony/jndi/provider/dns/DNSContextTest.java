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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Alexei Y. Zakharov
 */
package org.apache.harmony.jndi.provider.dns;

import java.util.Hashtable;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * <code>org.apache.harmony.jndi.provider.dns.DNSContext</class> class unit test. 
 * @author Alexei Zakharov
 */
public class DNSContextTest extends TestCase {

    private DNSNameParser nameParser = null;
    
    @Override
    protected void setUp() {
        nameParser = new DNSNameParser();
    }
    
    public void testGetNameInNamespace() throws NamingException {
        Context ctx;
        String propStr1 = "dns://localhost/example.com";
        String propStr2 = "dns://localhost";
        Hashtable<String, String> env = new Hashtable<String, String>();

        env.put(Context.PROVIDER_URL, propStr1);
        ctx = new DNSContextFactory().getInitialContext(env);
        assertEquals("example.com.", ctx.getNameInNamespace());
        env.put(Context.PROVIDER_URL, propStr2);
        ctx = new DNSContextFactory().getInitialContext(env);
        assertEquals(".", ctx.getNameInNamespace());
    }
    
    public void testComposeName() throws NamingException {
        Name name = null;
        Name prefix = null;
        Name result = null;
        String resultStr = null;
        String propStr1 = "dns://localhost/example.com";
        Hashtable<String, String> env = new Hashtable<String, String>();
        Context ctx;

        env.put(Context.PROVIDER_URL, propStr1);
        ctx = new DNSContextFactory().getInitialContext(env);
        // #composeName(Name, Name)
        // NULL & NULL
        try {
            ctx.composeName(name, prefix);
            fail("NullPointerException has not been thrown");
        } catch (NullPointerException e) {}
        // CompositeName & CompositeName
        name = new CompositeName("host1/file1.html");
        prefix = new CompositeName("/example.com");
        result = ctx.composeName(name, prefix);
        assertEquals("/host1.example.com/file1.html", result.toString());
        // DNSName & CompositeName
        name = nameParser.parse("host1.mysubdomain");
        prefix = new CompositeName("schema2:/example.com");
        result = ctx.composeName(name, prefix);
        assertEquals("schema2:/host1.mysubdomain.example.com",
                result.toString());
        // CompositeName & DNSName
        name = new CompositeName("host1/file1.html");
        prefix = nameParser.parse("subdomain.example.com."); 
        result = ctx.composeName(name, prefix);    
        assertEquals("host1.subdomain.example.com./file1.html",
                result.toString());
        // DNSName & DNSName
        name = nameParser.parse("host1.subdomain1");
        prefix = nameParser.parse("subdomain2.example.com."); 
        result = ctx.composeName(name, prefix);    
        assertEquals("host1.subdomain1.subdomain2.example.com.",
                result.toString());

        // error
        name = ProviderConstants.ROOT_ZONE_NAME_OBJ;
        prefix = new CompositeName("schema33:/domain.com");
        try {
            result = ctx.composeName(name, prefix);
            fail("NamingException should be thrown");
        } catch (NamingException e) {}

        // string form
        resultStr = ctx.composeName("host1/file1.html", "/example.com");
        assertEquals("/host1.example.com/file1.html", resultStr);
        resultStr = ctx.composeName("host1", "example.com");
        assertEquals("host1.example.com", resultStr);
        resultStr = ctx.composeName("host1/mamba", "example.com");
        assertEquals("host1.example.com/mamba", resultStr);
        resultStr = ctx.composeName("host1.subdomain", "schema17:/example.com");
        assertEquals("schema17:/host1.subdomain.example.com", resultStr);
        // error
        try {
            ctx.composeName(".", "schema17:/example.com");
            fail("NamingException should be thrown");
        } catch (NamingException e) {}
    }

    public void testConstructor() throws NoSuchFieldException,
            IllegalArgumentException, SecurityException, NamingException
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        DNSContext context = null;
        env.put(Context.AUTHORITATIVE, "true");
        env.put(DNSContext.LOOKUP_ATTR, "IN A");
        env.put(DNSContext.RECURSION, "true");
        env.put(DNSContext.TIMEOUT_INITIAL, "5000");
        env.put(DNSContext.TIMEOUT_RETRIES, "5");
        env.put(DNSContext.THREADS_MAX, "17");
        env.put(Context.PROVIDER_URL, "dns://superdns.com/intel.com");
        context = (DNSContext)new DNSContextFactory().getInitialContext(env);
        assertTrue(TestMgr.getBoolField(context, "authoritative"));
        assertEquals(ProviderConstants.A_TYPE,
                TestMgr.getIntField(context, "lookupAttrType"));
        assertEquals(ProviderConstants.IN_CLASS,
                TestMgr.getIntField(context, "lookupAttrClass"));
        assertTrue(TestMgr.getBoolField(context, "recursion"));
        assertEquals(5000, TestMgr.getIntField(context, "timeoutInitial"));
        assertEquals(5, TestMgr.getIntField(context, "timeoutRetries"));
        assertEquals(17, TestMgr.getIntField(context, "maxThreads"));

        env.put(Context.AUTHORITATIVE, "blah blah blah");
        env.put(DNSContext.LOOKUP_ATTR, "MX");
        env.put(DNSContext.RECURSION, "trueee");
        env.remove(DNSContext.THREADS_MAX);
        context = (DNSContext)new DNSContextFactory().getInitialContext(env);
        assertFalse(TestMgr.getBoolField(context, "authoritative"));
        assertEquals(ProviderConstants.MX_TYPE,
                TestMgr.getIntField(context, "lookupAttrType"));
        assertEquals(ProviderConstants.IN_CLASS,
                TestMgr.getIntField(context, "lookupAttrClass"));
        assertFalse(TestMgr.getBoolField(context, "recursion"));
        assertEquals(ProviderConstants.DEFAULT_MAX_THREADS,
                TestMgr.getIntField(context, "maxThreads"));

        env.put(DNSContext.LOOKUP_ATTR, "IN ZZZZZZZ");
        try {
            context = (DNSContext)new DNSContextFactory().getInitialContext(env);
            fail("NamingException has not been thrown");
        } catch (NamingException e) {}

        env.put(DNSContext.LOOKUP_ATTR, "ZZZZZZZ");
        try {
            context = (DNSContext)new DNSContextFactory().getInitialContext(env);
            fail("NamingException has not been thrown");
        } catch (NamingException e) {}
        env.put(DNSContext.LOOKUP_ATTR, "TXT");

        env.put(DNSContext.TIMEOUT_INITIAL, "q");
        try {
            context = (DNSContext)new DNSContextFactory().getInitialContext(env);
            fail("NumberFormatException has not been thrown");
        } catch (NumberFormatException e) {}
        env.put(DNSContext.TIMEOUT_INITIAL, "5000");

        env.put(DNSContext.TIMEOUT_RETRIES, "q");
        try {
            context = (DNSContext)new DNSContextFactory().getInitialContext(env);
            fail("NumberFormatException has not been thrown");
        } catch (NumberFormatException e) {}
        env.put(DNSContext.TIMEOUT_RETRIES, "5");

        env.put(Context.PROVIDER_URL,
                "dns://dnsserver1.com/super.zone.ru. " +
                "dns://123.456.78.90/super.zone.ru");
        context = (DNSContext)new DNSContextFactory().getInitialContext(env);
        /*
	slist = SList.getInstance();
        serv = slist.getServerByName("super.zone.ru", "dnsserver1.com", 53);
        if (serv == null) {
            fail("DNS server has not been added");
        }
        serv = slist.getServerByIP("super.zone.ru.", "123.456.78.90", 53);
        if (serv == null) {
            fail("DNS server has not been added");
        }
        */
        env.put(Context.PROVIDER_URL, "file:/etc/passwd");
        try {
            context = (DNSContext)new DNSContextFactory().getInitialContext(env);
            fail("NamingException has not been thrown");
        }
        catch (NamingException e) {}
        
    }
    
    /**
     * Tests <code>addToEnvironment(), getEnvironment()</code> and
     * <code>removeFromEnvironment()</code> methods.
     */
    public void testEnvironment() throws NamingException {
        DNSContext context = null;
        Hashtable<String, String> env = new Hashtable<String, String>();
        Hashtable<?, ?> env2 = null;

        // no side effect
        env.put(DNSContext.TIMEOUT_INITIAL, "2000");
        context = (DNSContext)new DNSContextFactory().getInitialContext(env);
        env.put(DNSContext.TIMEOUT_INITIAL, "2001");
        env2 = context.getEnvironment();
        assertEquals("2000", env2.get(DNSContext.TIMEOUT_INITIAL));
        
        // add to environment
        context.addToEnvironment(DNSContext.TIMEOUT_RETRIES, "15");
        env2 = context.getEnvironment();
        assertEquals("15", env2.get(DNSContext.TIMEOUT_RETRIES));

        // replace with new value
        context.addToEnvironment(DNSContext.TIMEOUT_RETRIES, "16");
        env2 = context.getEnvironment();
        assertEquals("16", env2.get(DNSContext.TIMEOUT_RETRIES));
        
        // remove from environment
        context.removeFromEnvironment(DNSContext.TIMEOUT_INITIAL);
        env2 = context.getEnvironment();
        assertNull(env2.get(DNSContext.TIMEOUT_INITIAL));        
    }
    
//    public void testConstructCannotProceedException() {
//        // TODO
//    }

    @Override
    protected void tearDown() {
        nameParser = null;
    }
    
    public static Test suite() {
        return new TestSuite(DNSContextTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DNSContextTest.class);
    }

}
