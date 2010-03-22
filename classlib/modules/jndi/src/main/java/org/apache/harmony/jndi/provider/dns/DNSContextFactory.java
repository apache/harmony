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
 * @author Alexei Y. Zakharov
 */

package org.apache.harmony.jndi.provider.dns;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * A factory to build DNS contexts.
 */
public class DNSContextFactory implements InitialContextFactory {

    /**
     * Empty constructor.
     */
    public DNSContextFactory() {
        super();
    }

    /**
     * Creates a new instance of DNS context.
     * 
     * @param env
     *            an environment to be used
     * @return new instance of <code>DNSContext</code>
     * @see javax.naming.spi.InitialContextFactory#getInitialContext(java.util.Hashtable)
     */
    public Context getInitialContext(Hashtable<?, ?> env)
            throws NamingException {
        return new DNSContext(env);
    }

}
