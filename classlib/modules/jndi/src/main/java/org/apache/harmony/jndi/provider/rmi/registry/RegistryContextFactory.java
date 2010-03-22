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
 * @author  Vasily Zakharov
 */

package org.apache.harmony.jndi.provider.rmi.registry;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;

import org.apache.harmony.jndi.internal.nls.Messages;
import org.apache.harmony.jndi.provider.rmi.rmiURLContextFactory;

/**
 * Initial context and object factory for {@link RegistryContext}.
 */
public class RegistryContextFactory implements InitialContextFactory,
        ObjectFactory {

    /**
     * Default constructor.
     */
    public RegistryContextFactory() {
    }

    /**
     * {@inheritDoc}
     */
    public Context getInitialContext(Hashtable<?, ?> environment)
            throws NamingException {
        String url = null;

        if (environment != null) {
            url = (String) environment.get(Context.PROVIDER_URL);
        }

        if (url == null) {
            url = RegistryContext.RMI_URL_PREFIX;
        }

        Object obj = new rmiURLContextFactory().getObjectInstance(url, null,
                null, environment);

        if (obj instanceof Context) {
            return (Context) obj;
        }
        // jndi.76=Object instantiated using the URL specified in environment is
        // not a context: {0}
        throw new NotContextException(Messages.getString("jndi.76", url)); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
            Hashtable<?, ?> environment) throws Exception {
        if (!(obj instanceof Reference)) {
            return null;
        }
        Reference reference = (Reference) obj;

        if (!reference.getFactoryClassName().equals(
                RegistryContextFactory.class.getName())) {
            return null;
        }
        int size = reference.size();

        if (size < 1) {
            // jndi.77=Reference is empty
            throw new ConfigurationException(Messages.getString("jndi.77")); //$NON-NLS-1$
        }
        Vector<Object> urls = new Vector<Object>(size);

        for (Enumeration<RefAddr> e = reference.getAll(); e.hasMoreElements();) {
            RefAddr refAddr = e.nextElement();

            if ((refAddr instanceof StringRefAddr)
                    && refAddr.getType().equals(RegistryContext.ADDRESS_TYPE)) {
                urls.add(refAddr.getContent());
            }
        }
        size = urls.size();

        if (size < 1) {
            // jndi.78=Reference contains no valid addresses
            throw new ConfigurationException(Messages.getString("jndi.78")); //$NON-NLS-1$
        }
        Object ret = new rmiURLContextFactory().getObjectInstance(urls
                .toArray(new String[size]), name, nameCtx, environment);

        if (ret instanceof RegistryContext) {
            ((RegistryContext) ret).setReference(reference);
        }
        return ret;
    }

}
