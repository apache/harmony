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

import javax.naming.Name;
import javax.naming.NamingException;

/**
 * The <code>Resolver</code> interface describes an intermediate context which
 * may be used in name resolution. In some context implementations, it is
 * possible that subtypes of <code>Context</code> may not be supported; in
 * such cases, a class implementing the <code>Resolver</code> interface
 * becomes useful to obtain a context which is of a specified subtype of
 * <code>Context</code>.
 */
public interface Resolver {

    /**
     * Partially resolves the name specified by name <code>n</code> stopping
     * at the first context object which is of the <code>Context</code>
     * subtype specified by class <code>c</code>.
     * 
     * @param n
     *            a name
     * @param c
     *            a context
     * @return details of the resolved context object and the part of the name
     *         remaining to be resolved in a non-null <code>ResolveResult</code>
     *         object.
     * @throws javax.naming.NotContextException
     *             if no context of the specified subtype is found.
     * @throws NamingException
     *             if other naming errors occur
     */
    ResolveResult resolveToClass(Name n, Class<? extends javax.naming.Context> c)
            throws NamingException;

    /**
     * Partially resolves the name specified by name <code>n</code> stopping
     * at the first context object which is of the <code>Context</code>
     * subtype specified by class <code>c</code>.
     * 
     * @param n
     *            a name in string
     * @param c
     *            a context
     * @return details of the resolved context object and the part of the name
     *         remaining to be resolved in a non-null <code>ResolveResult</code>
     *         object.
     * @throws javax.naming.NotContextException
     *             if no context of the specified subtype is found.
     * @throws NamingException
     *             if other naming errors occur
     */
    ResolveResult resolveToClass(String n,
            Class<? extends javax.naming.Context> c) throws NamingException;

}
