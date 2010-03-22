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

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

/**
 * This interface is for registering and deregistering to receive events about
 * objects that are bound in a context.
 * <p>
 * Listeners register an interest in a target object or objects. The context
 * might not yet have a binding for the target, and it is optional whether a
 * context will support registering for events about an object that is not
 * bound. If the context does not support it then
 * <code>addNamingListener()</code> should throw a
 * <code>NameNotFoundException</code>. Alternatively, if this is not
 * possible, the <code>EventContext</code> should send the listener a
 * <code>NamingExceptionEvent</code> with the information. A
 * <code>NamingExceptionEvent</code> is also used to notify listeners who have
 * registered interest in a target that is subsequently deleted, if the context
 * only allows registration for currently bound objects.
 * </p>
 * <p>
 * Listeners can register for events affecting the context itself, which as
 * usual is referred to by the empty name.
 * </p>
 * <p>
 * When a listener receives a <code>NamingExceptionEvent</code> it is
 * deregistered.
 * </p>
 * <p>
 * When <code>Context</code>.closed is called on an <code>EventContext</code>,
 * all of its listeners are deregistered.
 * </p>
 * <p>
 * Listener implementations may choose to implement more than one sub-interface
 * of <code>NamingListener</code>, in order to be notified of more than one
 * type of event.
 * </p>
 * <p>
 * Event context implementations are not expected to be thread safe.
 * </p>
 */
public interface EventContext extends Context {

    /**
     * This constant indicates interest in the named object only.
     */
    public static final int OBJECT_SCOPE = 0;

    /**
     * This constant indicates interest in objects bound in the named context,
     * but not the context itself.
     */
    public static final int ONELEVEL_SCOPE = 1;

    /**
     * This constant indicates interest in the named object and its subtree.
     * <p>
     * When the named object is not a context, "subtree" here refers to the
     * subtree of the context that contains the bound object. Where the named
     * object is itself a context, "subtree" refers to the subtree of the named
     * context.
     * </p>
     */
    public static final int SUBTREE_SCOPE = 2;

    /**
     * Registers <code>namingListener</code> for events concerning
     * <code>name</code>, with scope <code>i</code>.
     * <p>
     * The scope must be one of <code>OBJECT_SCOPE</code>,
     * <code>NELEVEL_SCOPE</code>, or <code>SUBTREE_SCOPE</code>.
     * </p>
     * <p>
     * When the scope is <code>ONELEVEL_SCOPE</code>, <code>name</code>
     * must be a context. Otherwise <code>name</code> can be a context or a
     * bound object.
     * </p>
     * <p>
     * Name is relative to this context.
     * </p>
     * 
     * @param name
     *            the concerning name
     * @param i
     *            the scope
     * @param namingListener
     *            the listener to be registered
     * @throws NamingException
     *             If any exception occured.
     */
    void addNamingListener(Name name, int i, NamingListener namingListener)
            throws NamingException;

    /**
     * Registers <code>namingListener</code> for events concerning name, with
     * scope <code>i</code>.
     * 
     * @param s
     *            the concerning name string
     * @param i
     *            the scope
     * @param namingListener
     *            the listener to be registered
     * @throws NamingException
     *             If any exception occured.
     * @see #addNamingListener(Name, int, NamingListener)
     */
    void addNamingListener(String s, int i, NamingListener namingListener)
            throws NamingException;

    /**
     * Removes all registrations for <code>namingListener</code> in this
     * <code>EventContext</code>. If there are no registrations this method
     * does nothing.
     * 
     * @param namingListener
     *            the listener to be unregistered
     * @throws NamingException
     *             If any exception occured.
     */
    void removeNamingListener(NamingListener namingListener)
            throws NamingException;

    /**
     * Checks if the implementation supports registration for names that are not
     * (yet) bound in this context.
     * 
     * @return false if implementation supports this, otherwise true if the
     *         implementation does not support this.
     * @throws NamingException
     *             If the support is not known.
     */
    boolean targetMustExist() throws NamingException;

}
