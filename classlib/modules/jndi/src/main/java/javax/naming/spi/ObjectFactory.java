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

/**
 * An <code>ObjectFactory</code> creates objects of a specified type. A
 * variety of different objects may be used by a JNDI application; these objects
 * are meaningful to the application and can be manipulated according to the
 * methods available to the class of the object, such as a printer object. The
 * class implementing this interface should be public, should also provide a
 * public constructor taking no arguments, and should be thread-safe. Where URL
 * is mentioned below, this refers to RFC 1738 and related RFCs.
 */
public interface ObjectFactory {

    /**
     * Creates an object of the type specified by parameter <code>o</code>,
     * including any reference or location details, customized by the specified
     * <code>envmt</code> parameter.
     * <p>
     * Object factories are specified via environment properties from several
     * sources including provider properties files (see the specification of the
     * <code>Context</code> interface) and may comprise a list of factories.
     * Each object factory in the list is used by
     * <code>NamingManager.getObjectInstance()</code> which invokes this
     * method on each of them until a non-null result is achieved or until the
     * list is exhausted. If an <code>ObjectFactory</code> throws an
     * exception, it should be passed back to the code that invoked
     * <code>NamingManager.getObjectInstance()</code> and no further object
     * factories in the list are examined. An exception should only be thrown by
     * an object factory if it is intended that no other object factories be
     * examined. Usually, if an <code>ObjectFactory</code> is unable to create
     * an object, then null should be returned.
     * </p>
     * <p>
     * A special case of <code>ObjectFactory</code> is a URL context factory
     * which is used either to create an object whose location is specified by a
     * URL passed as the object <code>o</code> or creates contexts for
     * resolving URLs. The <code>getObjectInstance()</code> method of a URL
     * context factory must obey these rules:
     * </p>
     * <p>
     * 1. When <code>Object o</code> is null, return a new Context object
     * suitable for resolving any URL of the scheme supported by this factory.
     * </p>
     * <p>
     * 2. When <code>Object o</code> is a URL string, return a new object
     * (usually a context) identified by the URL, so that names relatively lower
     * than that context may be resolved.
     * </p>
     * <p>
     * 3. When <code>Object o</code> is an <code>Array</code> object with
     * more than one URL string (order is not important), return a new object
     * (usually a context) identified by the URL as in rule 2, above. The URLs
     * in the array are considered to be equivalent in relation to the
     * associated context, but the object (context) factory can choose whether
     * or not to verify that they are truly equivalent.
     * </p>
     * <p>
     * 4. Otherwise, the behaviour of this method depends on the context factory
     * implementation.
     * </p>
     * 
     * @param o
     *            may be null or may contain location or reference details
     * @param n
     *            the name relative to the context <code>c</code> of the
     *            object being created and may be null; the implementation may
     *            clone or copy this object, but will not modify the original.
     * @param c
     *            may be null when the name is relative to the default initial
     *            context, or specifies the context to which the name is
     *            relative (if a factory uses this context object,
     *            synchronization should be used as Context objects are not
     *            thread-safe).
     * @param envmt
     *            may be null; the implementation may clone or copy this object,
     *            but will not modify the original.
     * @return either an object of the specified type or null if no object could
     *         be created.
     * @throws Exception
     *             causes no further object factory will be tried
     */
    Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> envmt)
            throws Exception;

}
