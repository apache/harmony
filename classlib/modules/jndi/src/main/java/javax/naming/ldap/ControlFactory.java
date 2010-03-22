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
import javax.naming.Context;
import javax.naming.NamingException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.harmony.jndi.internal.EnvironmentReader;
import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * This abstract class is used for factories which create controls as used in
 * LDAPv3. These factories are used by service providers to obtain control
 * instances when they receive a response control.
 * 
 * @see Control
 */
public abstract class ControlFactory {

    /**
     * Constructs a <code>ControlFactory</code> instance with no parameters.
     */
    protected ControlFactory() {
        super();
    }

    /**
     * Uses this control factory to create a particular type of <code>Control
     * </code>
     * based on the supplied control. It is likely that the supplied control
     * contains data encoded in BER format as received from an LDAP server.
     * Returns <code>null</code> if the factory cannot create a
     * <code>Control</code> else it returns the type of <code>Control</code>
     * created by the factory.
     * 
     * 
     * @param c
     *            the supplied control
     * @throws NamingException
     *             If an error is encountered.
     * @return the control
     */
    public abstract Control getControlInstance(Control c)
            throws NamingException;

    /**
     * Creates a particular type of control based on the supplied control c. It
     * is likely that the supplied control contains data encoded in BER format
     * as received from an LDAP server.
     * <p>
     * This method tries the factories in LdapContext.CONTROL_FACTORIES, first
     * from the supplied <code>Hashtable</code> then from the resource
     * provider files of the supplied <code>Context</code>.
     * </p>
     * <p>
     * It returns the supplied control if no factories are loaded or a control
     * cannot be created. Otherwise, a new <code>Control</code> instance is
     * returned.
     * 
     * @param c
     *            the supplied <code>Control</code> instance
     * @param ctx
     *            the supplied <code>Context</code> instance
     * @param h
     *            the supplier JNDI environment properties
     * @return the supplied control if no factories are loaded or a control
     *         cannot be created, otherwise a new <code>Control</code>
     *         instance
     * @throws NamingException
     *             If an error is encountered.
     */
    public static Control getControlInstance(Control c, Context ctx,
            Hashtable<?, ?> h) throws NamingException {

        // obtain control factories from hashtable and provider resource file
        String fnames[] = EnvironmentReader
                .getFactoryNamesFromEnvironmentAndProviderResource(h, ctx,
                        LdapContext.CONTROL_FACTORIES);

        for (String element : fnames) {
            // new factory instance by its class name
            ControlFactory factory = null;
            try {
                factory = (ControlFactory) classForName(element).newInstance();
            } catch (Exception e) {
                continue;
            }
            // try obtaining a Control using the factory
            Control control = factory.getControlInstance(c);
            // if a Control is obtained successfully, return it
            if (null != control) {
                return control;
            }
        }

        // all factories failed, return the input argument c
        return c;
    }

    /*
     * Use the context class loader or the system class loader to load the
     * specified class, in a privileged manner.
     */
    private static Class<?> classForName(final String className)
            throws ClassNotFoundException {

        Class<?> cls = AccessController
                .doPrivileged(new PrivilegedAction<Class<?>>() {
                    public Class<?> run() {
                        // try thread context class loader first
                        try {
                            return Class.forName(className, true, Thread
                                    .currentThread().getContextClassLoader());
                        } catch (ClassNotFoundException e) {
                            // Ignored
                        }
                        // try system class loader second
                        try {
                            return Class.forName(className, true, ClassLoader
                                    .getSystemClassLoader());
                        } catch (ClassNotFoundException e) {
                            // Ignored
                        }
                        // return null, if fail to load class
                        return null;
                    }
                });

        if (cls == null) {
            // jndi.1C=class {0} not found
            throw new ClassNotFoundException(Messages.getString(
                    "jndi.1C", className)); //$NON-NLS-1$
        }

        return cls;
    }

}
