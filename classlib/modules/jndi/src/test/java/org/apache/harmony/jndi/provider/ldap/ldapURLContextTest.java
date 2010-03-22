/* 
 *  Licensed to the Apache Software Foundation (ASF) under one or more 
 *  contributor license agreements.  See the NOTICE file distributed with 
 *  this work for additional information regarding copyright ownership. 
 *  The ASF licenses this file to You under the Apache License, Version 2.0 
 *  (the "License"); you may not use this file except in compliance with 
 *  the License.  You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License. 
 */

package org.apache.harmony.jndi.provider.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import javax.naming.spi.ResolveResult;

import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.jndi.provider.ldap.mock.BindResponse;
import org.apache.harmony.jndi.provider.ldap.mock.MockLdapServer;

import junit.framework.TestCase;

public class ldapURLContextTest extends TestCase {
    private MockLdapServer server;

    @Override
    public void setUp() throws Exception {
        server = new MockLdapServer();
        server.start();
    }

    @Override
    public void tearDown() {
        server.stop();
    }

    public void testGetRootURLContext() throws Exception {
        MockLdapURLContext context = new MockLdapURLContext();
        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

        ResolveResult result = context.getRootURLContext(server.getURL(), null);

        assertEquals("", result.getRemainingName().toString());
        assertTrue(result.getResolvedObj() instanceof LdapContextImpl);
    }

    public void testGetRootURLContext2() throws Exception {
        Hashtable<Object, Object> initialEnv = new Hashtable<Object, Object>();
        initialEnv.put(Context.REFERRAL, "throw");
        initialEnv.put("test.getRootURLContext", "test");

        MockLdapURLContext context = new MockLdapURLContext(initialEnv);

        Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        env.put(Context.REFERRAL, "ignore");
        env.put("test.getRootURLContext", "GetRootURLContext");

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

        ResolveResult result = context.getRootURLContext(server.getURL(), env);

        assertEquals("", result.getRemainingName().toString());
        assertTrue(result.getResolvedObj() instanceof LdapContextImpl);

        LdapContext newContext = (LdapContext) result.getResolvedObj();
        Hashtable<Object, Object> newEnv = (Hashtable<Object, Object>) newContext
                .getEnvironment();

        assertEquals("ignore", newEnv.get(Context.REFERRAL));
        assertEquals("GetRootURLContext", newEnv.get("test.getRootURLContext"));
    }

    public void testGetRootURLContext3() throws Exception {
        Hashtable<Object, Object> initialEnv = new Hashtable<Object, Object>();
        initialEnv.put(Context.REFERRAL, "throw");
        initialEnv.put("test.getRootURLContext", "test");

        MockLdapURLContext context = new MockLdapURLContext(initialEnv);

        server.setResponseSeq(new LdapMessage[] { new LdapMessage(
                LdapASN1Constant.OP_BIND_RESPONSE, new BindResponse(), null) });

        ResolveResult result = context.getRootURLContext(server.getURL(), null);

        assertEquals("", result.getRemainingName().toString());
        assertTrue(result.getResolvedObj() instanceof LdapContextImpl);

        LdapContext newContext = (LdapContext) result.getResolvedObj();
        Hashtable<Object, Object> newEnv = (Hashtable<Object, Object>) newContext
                .getEnvironment();

        assertEquals("throw", newEnv.get(Context.REFERRAL));
        assertEquals("test", newEnv.get("test.getRootURLContext"));
    }

    public static class MockLdapURLContext extends ldapURLContext {
        public MockLdapURLContext() {
            super();
        }

        public MockLdapURLContext(Hashtable<Object, Object> env) {
            super(env);
        }

        @Override
        public ResolveResult getRootURLContext(String url, Hashtable<?, ?> env)
                throws NamingException {
            return super.getRootURLContext(url, env);
        }
    }
}
