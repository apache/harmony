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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.Enumeration;

/**
 * Trivial implementation of <code>NamingEnumeration</code> interface.
 */
public class BasicNamingEnumerator<T> implements NamingEnumeration<T> {

    private final Enumeration<T> enumeration;

    /**
     * Constructs new enumerator from existing <code>Enumeration</code>.
     * 
     * @param enumeration
     *            enumeration
     */
    public BasicNamingEnumerator(Enumeration<T> newEnum) {
        this.enumeration = newEnum;
    }

    public T next() {
        return enumeration.nextElement();
    }

    public boolean hasMore() {
        return enumeration.hasMoreElements();
    }

    public void close() throws NamingException {
    }

    public T nextElement() {
        return enumeration.nextElement();
    }

    public boolean hasMoreElements() {
        return enumeration.hasMoreElements();
    }

}
