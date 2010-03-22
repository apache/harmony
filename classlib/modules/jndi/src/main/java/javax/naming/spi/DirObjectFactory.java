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
import javax.naming.directory.Attributes;

/**
 * An <code>DirObjectFactory</code> creates objects of a specified type.
 * <code>DirObjectFactory</code> is a specific version of
 * <code>ObjectFactory</code> for <code>DirectoryManager</code>.
 * 
 * @see ObjectFactory
 * @see DirectoryManager
 */
public interface DirObjectFactory extends ObjectFactory {

    /**
     * Similar to <code>ObjectFactory.getObjectInstance</code>, with an
     * additional <code>attributes</code> parameter.
     * 
     * @param o
     *            an object
     * @param n
     *            a name
     * @param c
     *            a context
     * @param envmt
     *            a context environment
     * @param a
     *            some attributes
     * @return the created object
     * @throws Exception
     *             if an exception occurs
     * @see ObjectFactory#getObjectInstance(Object, Name, Context, Hashtable)
     */
    Object getObjectInstance(Object o, Name n, Context c,
            Hashtable<?, ?> envmt, Attributes a) throws Exception;

}
