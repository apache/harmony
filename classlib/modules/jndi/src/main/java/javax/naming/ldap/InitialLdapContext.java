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

package javax.naming.ldap;

import java.util.Hashtable;
import javax.naming.directory.InitialDirContext;
import javax.naming.NamingException;
import javax.naming.NotContextException;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * This is used as the starting context when the LDAPv3 extended functionality
 * provided by the <code>javax.naming.ldap</code> package is required.
 * 
 * See <code>LdapContext</code> for a description of the Request and Response
 * controls.
 * 
 * @see LdapContext
 */
public class InitialLdapContext extends InitialDirContext implements
        LdapContext {

    /*
     * This is set to the environment property java.naming.ldap.version.
     */
    private static final String LDAP_VERSION = "java.naming.ldap.version"; //$NON-NLS-1$

    /*
     * This is set to the environment property java.naming.ldap.control.connect.
     */
    private static final String CONNECT_CONTROL = "java.naming.ldap.control.connect"; //$NON-NLS-1$

    /*
     * The version of this LDAP context implementation.
     */
    private static final String THIS_LDAP_VERSION = "3"; //$NON-NLS-1$

    /**
     * Constructs an <code>InitialLdapContext</code> instance without using
     * any environment properties or connection controls.
     * 
     * @throws NamingException
     *             If an error is encountered.
     */
    public InitialLdapContext() throws NamingException {
        this(null, null);
    }

    /**
     * Constructs an <code>InitialLdapContext</code> instance using the
     * supplied environment properties and connection controls.
     * 
     * @param h
     *            the environment properties which may be null
     * @param cs
     *            the connection controls which may be null
     * @throws NamingException
     *             If an error is encountered.
     */
    @SuppressWarnings("unchecked")
    public InitialLdapContext(Hashtable<?, ?> h, Control[] cs)
            throws NamingException {
        super(true);

        /*
         * Prepare the environment properties to be inherited by the service
         * provider.
         */
        Hashtable<Object, Object> newEnvironment = null;
        if (null == h) {
            newEnvironment = new Hashtable<Object, Object>();
        } else {
            newEnvironment = (Hashtable<Object, Object>) h.clone();
        }

        // Set the environment property java.naming.ldap.control.connect
        if (null != cs) {
            Control[] cloneOfCs = new Control[cs.length];
            System.arraycopy(cs, 0, cloneOfCs, 0, cs.length);
            newEnvironment.put(CONNECT_CONTROL, cloneOfCs);
        }

        // Set the environment property java.naming.ldap.version to be 3
        newEnvironment.put(LDAP_VERSION, THIS_LDAP_VERSION);

        // Initialize the initial context
        super.init(newEnvironment);
    }

    /*
     * Gets the default initial context and verify that it's an instance of
     * LdapContext.
     */
    private LdapContext getDefaultInitLdapContext() throws NamingException {
        if (!(super.defaultInitCtx instanceof LdapContext)) {
            // jndi.1D=Expected an LdapContext object.
            throw new NotContextException(Messages.getString("jndi.1D")); //$NON-NLS-1$
        }
        return (LdapContext) super.defaultInitCtx;
    }

    public ExtendedResponse extendedOperation(ExtendedRequest e)
            throws NamingException {
        return getDefaultInitLdapContext().extendedOperation(e);
    }

    public LdapContext newInstance(Control[] ac) throws NamingException {
        return getDefaultInitLdapContext().newInstance(ac);
    }

    public void reconnect(Control[] ac) throws NamingException {
        getDefaultInitLdapContext().reconnect(ac);
    }

    public Control[] getConnectControls() throws NamingException {
        return getDefaultInitLdapContext().getConnectControls();
    }

    public void setRequestControls(Control[] ac) throws NamingException {
        getDefaultInitLdapContext().setRequestControls(ac);
    }

    public Control[] getRequestControls() throws NamingException {
        return getDefaultInitLdapContext().getRequestControls();
    }

    public Control[] getResponseControls() throws NamingException {
        return getDefaultInitLdapContext().getResponseControls();
    }

}
