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
 * @author  Vasily Zakharov
 */
package org.apache.harmony.jndi.provider.rmi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;

import org.apache.harmony.jndi.provider.GenericURLContextFactory;

/**
 * URL context factory for {@link rmiURLContext}.
 */
public class rmiURLContextFactory extends GenericURLContextFactory {

    /**
     * Default constructor.
     */
    public rmiURLContextFactory() {
        super();
    }

    /**
     * Returns new {@link rmiURLContext}. Used by
     * {@link GenericURLContextFactory#getObjectInstance(Object, Name, Context, Hashtable)}.
     * 
     * @param environment
     *            Environment.
     * 
     * @return New {@link rmiURLContext}.
     */
    @Override
    protected Context createURLContext(Hashtable<?, ?> environment) {
        return new rmiURLContext(environment);
    }

}
