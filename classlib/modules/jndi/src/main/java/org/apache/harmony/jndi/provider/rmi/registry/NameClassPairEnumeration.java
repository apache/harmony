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

import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Enumeration of {@link NameClassPair} objects, used by
 * {@link RegistryContext#list(Name)} method.
 */
class NameClassPairEnumeration implements NamingEnumeration<NameClassPair> {

    /**
     * Binding names returned from {@link Registry#list()} method.
     */
    protected final String[] names;

    /**
     * Index of the next name to return.
     */
    protected int index = 0;

    /**
     * Creates this enumeration.
     * 
     * @param names
     *            Binding names returned from {@link Registry#list()} method.
     */
    public NameClassPairEnumeration(String[] names) {
        this.names = names;
    }

    public boolean hasMore() {
        return (index < names.length);
    }

    public NameClassPair next() throws NamingException, NoSuchElementException {
        if (!hasMore()) {
            throw new NoSuchElementException();
        }

        String name = names[index++];
        NameClassPair pair = new NameClassPair(name, Object.class.getName());
        pair.setNameInNamespace(name);
        return pair;
    }

    public boolean hasMoreElements() {
        return hasMore();
    }

    public NameClassPair nextElement() {
        try {
            return next();
        } catch (NamingException e) {
            throw (NoSuchElementException) new NoSuchElementException()
                    .initCause(e);
        }
    }

    public void close() {
        index = names.length;
    }

}
