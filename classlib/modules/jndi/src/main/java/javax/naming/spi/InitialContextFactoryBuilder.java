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

package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.NamingException;

/**
 * An initial context factory builder creates an initial context factory, and an
 * initial context factory creates initial contexts. A variety of different
 * initial context implementations may be used by a JNDI application. The
 * application uses <code>NamingManager.setInitialContextFactoryBuilder()</code>
 * to specify its own or its preferred builder to override JNDI default
 * policies. Any such builder must implement the
 * <code>InitialContextFactoryBuilder</code> interface.
 */
public interface InitialContextFactoryBuilder {

    /**
     * Uses the environment properties in the specified <code>envmt</code>
     * parameter to create an initial context factory. If the implementation
     * needs to keep a copy of <code>envmt</code> or to change it, it will
     * clone or copy the specified object and use that instead. The
     * <code>envmt</code> parameter may be null.
     * 
     * @param envmt
     *            the context environment as a <code>Hashtable</code>
     * @return an initial context factory - cannot be null.
     * @throws NamingException
     *             if an initial context factory could not be created.
     */
    InitialContextFactory createInitialContextFactory(Hashtable<?, ?> envmt)
            throws NamingException;

}
