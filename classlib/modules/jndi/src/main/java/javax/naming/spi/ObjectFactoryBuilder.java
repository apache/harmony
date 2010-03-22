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
 * An object factory builder creates an object factory, and an object factory
 * creates objects of a specified type. A variety of different objects may be
 * used by a JNDI application; these objects are meaningful to the application
 * and can be manipulated according to the methods available to the class of the
 * object, such as a printer object. The application uses
 * <code>NamingManager.setObjectFactoryBuilder()</code> to specify its own or
 * its preferred builder to override JNDI default policies; any such builder
 * must implement the <code>ObjectFactoryBuilder</code> interface.
 */
public interface ObjectFactoryBuilder {

    /**
     * Returns an <code>ObjectFactory</code> customized by the
     * <code>envmt</code> parameter that is capable of creating instances of
     * the object <code>o</code>.
     * 
     * @param o
     *            may be null
     * @param envmt
     *            may be null
     * @return an <code>ObjectFactory</code> customized by the
     *         <code>envmt</code> parameter that is capable of creating
     *         instances of the object <code>o</code>.
     * @throws NamingException
     *             if an object factory could not be created.
     */
    ObjectFactory createObjectFactory(Object o, Hashtable<?, ?> envmt)
            throws NamingException;

}
