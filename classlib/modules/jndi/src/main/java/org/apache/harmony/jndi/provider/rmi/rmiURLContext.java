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

import javax.naming.CompositeName;
import javax.naming.NamingException;

import javax.naming.spi.ResolveResult;

import org.apache.harmony.jndi.internal.nls.Messages;
import org.apache.harmony.jndi.provider.GenericURLContext;

import org.apache.harmony.jndi.provider.rmi.registry.RegistryContext;

/**
 * RMI URL context implementation.
 */
public class rmiURLContext extends GenericURLContext {

    /**
     * Creates instance of this context with empty environment.
     */
    public rmiURLContext() {
        super(null);
    }

    /**
     * Creates instance of this context with specified environment.
     * 
     * @param environment
     *            Environment to copy.
     */
    public rmiURLContext(Hashtable<?, ?> environment) {
        super(environment);
    }

    /**
     * Determines the proper {@link RegistryContext} from the specified URL and
     * returns the {@link ResolveResult} object with that context as resolved
     * object and the rest of the URL as remaining name.
     * 
     * @param url
     *            URL.
     * 
     * @param environment
     *            Environment.
     * 
     * @return {@link ResolveResult} object with resolved context as resolved
     *         object the rest of the URL as remaining name.
     * 
     * @throws NamingException
     *             If some naming error occurs.
     */
    @Override
    protected ResolveResult getRootURLContext(String url,
            Hashtable<?, ?> environment) throws NamingException {
        if (!url.startsWith(RegistryContext.RMI_URL_PREFIX)) {
            // jndi.74=Not an RMI URL, incorrect prefix: {0}
            throw new IllegalArgumentException(Messages.getString(
                    "jndi.74", url)); //$NON-NLS-1$
        }
        int length = url.length();
        int start = RegistryContext.RMI_URL_PREFIX.length();
        String hostName = null;
        int port = 0;

        if ((start < length) && (url.charAt(start) == '/')) {
            start++;

            if ((start < length) && (url.charAt(start) == '/')) {
                start++;

                // end marks either first slash or end of URL.
                int end = url.indexOf('/', start);
                if (end < 0) {
                    end = length;
                }

                // hostEnd marks either end of hostname or end of URL.
                int hostEnd = url.indexOf(':', start);
                if ((hostEnd < 0) || (hostEnd > end)) {
                    hostEnd = end;
                }

                // Extracting host name.
                if (start < hostEnd) {
                    hostName = url.substring(start, hostEnd);
                }

                // Extracting port number.
                int portStart = hostEnd + 1;
                if (portStart < end) {
                    try {
                        port = Integer.parseInt(url.substring(portStart, end));
                    } catch (NumberFormatException e) {
                        // jndi.75=Invalid port number in URL: {0}
                        throw (IllegalArgumentException) new IllegalArgumentException(
                                Messages.getString("jndi.75", //$NON-NLS-1$
                                        url)).initCause(e);
                    }
                }

                // Point start to suffix string.
                start = ((end < length) ? (end + 1) : length);
            }
        }

        // Create remaining name.
        CompositeName name = new CompositeName();
        if (start < length) {
            name.add(url.substring(start));
        }

        return new ResolveResult(new RegistryContext(hostName, port,
                environment), name);
    }

}
