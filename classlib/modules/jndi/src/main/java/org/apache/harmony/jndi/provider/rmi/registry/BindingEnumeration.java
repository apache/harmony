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

import java.rmi.registry.Registry;

import java.util.NoSuchElementException;

import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Enumeration of {@link Binding} objects, used by
 * {@link RegistryContext#listBindings(Name)} method.
 */
class BindingEnumeration implements NamingEnumeration<Binding> {

    /**
     * Binding names returned from {@link Registry#list()} method.
     */
    protected final String[] names;

    /**
     * Index of the next name to return.
     */
    protected int index = 0;

    /**
     * Registry context.
     */
    protected RegistryContext context;

    /**
     * Creates this enumeration.
     * 
     * @param names
     *            Binding names returned from {@link Registry#list()} method.
     * 
     * @param context
     *            RegistryContext to extract bindings from.
     */
    public BindingEnumeration(String[] names, RegistryContext context) {
        super();
        this.names = names;
        this.context = context.cloneContext();
    }

    public boolean hasMore() {
        if (index < names.length) {
            return true;
        }
        close();
        return false;
    }

    public boolean hasMoreElements() {
        return hasMore();
    }

    public Binding next() throws NoSuchElementException, NamingException {
        if (!hasMore()) {
            throw new NoSuchElementException();
        }

        String name = names[index++];
        Binding binding = new Binding(name, context.lookup(name));
        binding.setNameInNamespace(name);
        return binding;
    }

    public Binding nextElement() {
        try {
            return next();
        } catch (NamingException e) {
            throw (NoSuchElementException) new NoSuchElementException()
                    .initCause(e);
        }
    }

    public void close() {
        index = names.length;
        context.close();
    }

}
