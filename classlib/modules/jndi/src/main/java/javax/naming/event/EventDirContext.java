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

package javax.naming.event;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

/**
 * This interface allows registering of listeners for events concerning objects
 * bound in a directory context.
 * <p>
 * The registration methods take an RFC2254 search filter as a parameter, which
 * is used to select the objects to generate events for.
 * </p>
 * <p>
 * Sometimes objects that satisfy a search filter may be bound after a listener
 * is registered, specifying the filter. Where the directory service will not
 * support this, and generate events for objects bound after a given filter is
 * specified, then the <code>addNamingListener</code> methods will throw an
 * <code>InvalidSearchFilterException</code>.
 * </p>
 */
public interface EventDirContext extends EventContext, DirContext {

    /**
     * Registers naming listener for events concerning objects selected by the
     * given search at <code>name</code>. The <code>name</code> parameter
     * is relative to this context.
     * 
     * @param name
     *            the concerning <code>Name</code>
     * @param filter
     *            a RFC2254 search filter
     * @param filterArgs
     *            filter arguments
     * @param searchControls
     *            Further specifies the selection of objects to generate events
     *            for, and the information contained in
     *            <code>NamingEvents</code> that may be generated.
     * @param namingListener
     *            the <code>NamingListener</code> to be registered
     * @throws NamingException
     *             If any exception occured.
     */
    void addNamingListener(Name name, String filter, Object filterArgs[],
            SearchControls searchControls, NamingListener namingListener)
            throws NamingException;

    /**
     * Registers naming listener for events concerning objects selected by the
     * given search at <code>name</code>. The <code>name</code> parameter
     * is relative to this context.
     * 
     * @param name
     *            the concerning <code>Name</code>
     * @param filter
     *            a RFC2254 search filter string with no arguments
     * @param searchControls
     *            further specifies the selection of objects to generate events
     *            for, and the information contained in
     *            <code>NamingEvents</code> that may be generated
     * @param namingListener
     *            the <code>NamingListener</code> to be registered
     * @throws NamingException
     *             If any exception occured.
     * @see #addNamingListener(Name, String, Object[], SearchControls,
     *      NamingListener)
     */
    void addNamingListener(Name name, String filter,
            SearchControls searchControls, NamingListener namingListener)
            throws NamingException;

    /**
     * Registers naming listener for events concerning objects selected by the
     * given search at name string <code>name</code>. The <code>s</code>
     * parameter is relative to this context.
     * 
     * @param name
     *            the concerning <code>Name</code>
     * @param filter
     *            a RFC2254 search filter
     * @param filterArgs
     *            filter arguments
     * @param searchControls
     *            further specifies the selection of objects to generate events
     *            for, and the information contained in
     *            <code>NamingEvents</code> that may be generated
     * @param namingListener
     *            the <code>NamingListener</code> to be registered
     * @throws NamingException
     *             If any exception occured.
     * @see #addNamingListener(Name, String, Object[], SearchControls,
     *      NamingListener)
     */
    void addNamingListener(String name, String filter, Object filterArgs[],
            SearchControls searchControls, NamingListener namingListener)
            throws NamingException;

    /**
     * Registers naming listener for events concerning objects selected by the
     * given search at name string <code>name</code>. The <code>s</code>
     * parameter is relative to this context.
     * 
     * @param name
     *            the concerning <code>Name</code>
     * @param filter
     *            a RFC2254 search filter string with no arguments
     * @param searchControls
     *            further specifies the selection of objects to generate events
     *            for, and the information contained in
     *            <code>NamingEvents</code> that may be generated
     * @param namingListener
     *            the <code>NamingListener</code> to be registered
     * @throws NamingException
     *             If any exception occured.
     * @see #addNamingListener(String, String, Object[], SearchControls,
     *      NamingListener)
     */
    void addNamingListener(String name, String filter,
            SearchControls searchControls, NamingListener namingListener)
            throws NamingException;

}
