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
package org.apache.harmony.jndi.provider;

import java.util.Hashtable;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

import javax.naming.spi.ObjectFactory;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * Base class for URL naming context factory implementations.
 * 
 * In many cases, subclasses should only override
 * {@link #createURLContext(Hashtable)} method and provide public no-args
 * constructor.
 */
public abstract class GenericURLContextFactory implements ObjectFactory {

    /**
     * Default constructor for subclasses.
     */
    protected GenericURLContextFactory() {
        super();
    }

    /**
     * Lookups the specified object in the underlying context. Underlying
     * context instance is provided by {@link #createURLContext(Hashtable)}.
     * 
     * Follows the guidelines for URL context factories described in
     * {@link ObjectFactory#getObjectInstance(Object, Name, Context, Hashtable)}
     * specification.
     * 
     * If <code>obj</code> is <code>null</code>, just creates and returns
     * an underlying context.
     * 
     * If <code>obj</code> is a proper URL string, lookups and returns an
     * object specified by that string.
     * 
     * If <code>obj</code> is an array of URL strings, tries to lookup each of
     * them sequentially until lookup succeeds, then returns the result. If no
     * lookup succeeds, throws {@link NamingException} describing the fail of a
     * last lookup.
     * 
     * <code>name</code> and <code>nameCtx</code> parameters are ignored.
     * 
     * @param obj
     *            Object to lookup, can be <code>null</code>.
     * 
     * @param name
     *            Ignored.
     * 
     * @param nameCtx
     *            Ignored.
     * 
     * @param environment
     *            Environment to use in creating the underlying context, can be
     *            <code>null</code>.
     * 
     * @return The object created.
     * 
     * @throws ConfigurationException
     *             If <code>obj</code> is neither <code>null</code> nor a
     *             string, nor a string array, or is an empty string array.
     * 
     * @throws NamingException
     *             If lookup attempt failed.
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
            Hashtable<?, ?> environment) throws NamingException {
        Context context = createURLContext(environment);

        if (obj == null) {
            // For null object - just return context.
            return context;
        }

        try {
            if (obj instanceof String) {
                // For string object - return the object it names.
                return context.lookup((String) obj);
            }

            if (obj instanceof String[]) {
                // For string array object - search it through.
                String[] strings = (String[]) obj;

                if (strings.length < 1) {
                    // jndi.2C=obj is an empty string array
                    throw new ConfigurationException(Messages
                            .getString("jndi.2C")); //$NON-NLS-1$
                }

                NamingException exception = null;

                for (String element : strings) {
                    try {
                        // If the valid object is found - return it.
                        return context.lookup(element);
                    } catch (NamingException e) {
                        // Invalid object, store the exception
                        // to throw it later if no valid object is found.
                        exception = e;
                    }
                }

                // No valid object is found.
                throw exception;
            }

            // Unknown object type.
            // jndi.2D=obj is neither null, nor a string, nor a string array:
            // {0}
            throw new IllegalArgumentException(Messages.getString(
                    "jndi.2D", obj)); //$NON-NLS-1$
        } finally {
            context.close();
        }
    }

    /**
     * Returns new instance of the necessary context. Used by
     * {@link #getObjectInstance(Object, Name, Context, Hashtable)}.
     * 
     * Must be overridden by particular URL context factory implementations.
     * 
     * @param environment
     *            Environment.
     * 
     * @return New context instance.
     */
    protected abstract Context createURLContext(Hashtable<?, ?> environment);

}
