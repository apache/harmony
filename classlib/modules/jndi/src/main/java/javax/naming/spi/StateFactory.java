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
import javax.naming.Name;
import javax.naming.Context;
import javax.naming.NamingException;

/**
 * The <code>StateFactory</code> interface describes a factory used to get the
 * state of an object to be bound. Using a <code>lookup()</code> on a suitable
 * context, an class may be found representing, say, a printer; an
 * <code>ObjectFactory</code> may be used to create an instance of the printer
 * object class and methods defined by the printer class may be used to inspect
 * and manipulate the printer. There is a reverse mechanism in which a
 * <code>StateFactory</code> is used by the service provider to obtain the
 * state of the (in this case) printer for storing in the naming system. In
 * addition to implementing this interface, a <code>StateFactory</code> class
 * must be public and have a public constructor taking no parameters.
 * <p>
 * Note that while <code>StateFactory</code> is meant to be used in
 * <code>Context</code> service providers, whereas
 * <code>DirStateFactory</code> is meant to be used in <code>DirContext</code>
 * service providers.
 * </p>
 */
public interface StateFactory {

    /**
     * Returns a new instance of the specified object <code>o</code>
     * containing the state of the object to be bound, customized by the
     * specified <code>envmt</code> parameter. The name and context parameters
     * optionally specify the name of the object being created.
     * 
     * Object and state factories are specified via environment properties from
     * several sources including provider properties files (see the
     * specification of the <code>Context</code> interface) and may comprise a
     * list of factories. When the service provider looks to obtain the list of
     * state factories, the environment and provider properties files are search
     * for the property name specified by constant
     * <code>Context.STATE_FACTORIES</code>. Each state factory in the
     * resulting list is used by <code>NamingManager.getStateToBind()</code>
     * which invokes this method on each of them until a non-null result is
     * achieved or until the list is exhausted. If a <code>StateFactory</code>
     * throws an exception, it should be passed back to the code that invoked
     * <code>NamingManager.getStateToBind()</code> and no further state
     * factories in the list are examined. An exception should only be thrown by
     * a state factory if it is intended that no other state factories be
     * examined. Usually, if an state factory is unable to return the state of
     * an object, then null should be returned.
     * 
     * @param o
     *            may be null, or specifies the returning instance
     * @param n
     *            may be null, or specifies the name relative to the specified
     *            context <code>c</code>. The implementation may clone or
     *            copy this object, but will not modify the original.
     * @param c
     *            the context to which the name parameter is relative, or may be
     *            null when using a name relative the the default initial
     *            context. If a factory uses this context object,
     *            synchronization should be used as context objects are not
     *            thread-safe.
     * @param envmt
     *            may be null; the implementation may clone or copy this object,
     *            but will not modify the original.
     * @return either a new instance of the specified object <code>o</code>
     *         containing the state of the object to be bound, customized by the
     *         specified <code>envmt</code> parameter. Or null if no object
     *         could be created.
     * @throws NamingException
     *             if it is intended that no other state factories be examined.
     */
    Object getStateToBind(Object o, Name n, Context c, Hashtable<?, ?> envmt)
            throws NamingException;

}
