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
import javax.naming.Context;
import javax.naming.NamingException;

/**
 * An implementation of <code>InitialContextFactory</code> creates an initial
 * context so that the JNDI application can begin to invoke naming operations on
 * that context. The class implementing this interface should be public and
 * should also provide a public constructor taking no arguments.
 */
public interface InitialContextFactory {

    /**
     * Returns a non-null initial context object on which naming operations can
     * be invoked. The specified <code>envmt</code> parameter may be null or
     * may be used to customize the requested <code>Context</code> object. The
     * implementation may clone or copy the <code>envmt</code> object, but
     * will not modify the original object.
     * 
     * @param envmt
     *            the context environment as a <code>Hashtable</code>
     * @return a non-null initial context object
     * @throws NamingException
     *             if a naming exception occurs
     */
    Context getInitialContext(Hashtable<?, ?> envmt) throws NamingException;

}
