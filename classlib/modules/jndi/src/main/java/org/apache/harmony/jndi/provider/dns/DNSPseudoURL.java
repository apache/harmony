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

import java.util.StringTokenizer;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * Represents a DNS pseudo URL.
 */
public class DNSPseudoURL {

    private String host = "localhost"; //$NON-NLS-1$

    private int port = ProviderConstants.DEFAULT_DNS_PORT;

    private String domain = "."; //$NON-NLS-1$

    private boolean hostIpWasGiven = false;

    /**
     * Parses given argument and constructs new <code>DNSPseudoURL</code>
     * object. The format of the argument is:<br>
     * <code>dns:[//host[:port]][/domain]</code><br>
     * If no host information was given then <code>localhost</code> will be
     * used. If no port was given then the standard DNS server port
     * <code>53</code> will be used. If no domain was given then the root
     * domain will be used. All domain here are treated as absolute domains.
     * 
     * @param DNS
     *            string representation of DNS URL
     * @throws IllegalArgumentException
     *             if the argument is not in acceptable format
     * @throws NullPointerException
     *             if the argument is null
     */
    public DNSPseudoURL(String strForm) throws IllegalArgumentException {
        StringTokenizer st = null;
        StringTokenizer st2 = null;
        String token;

        if (strForm == null) {
            // jndi.67=strForm is null
            throw new NullPointerException(Messages.getString("jndi.67")); //$NON-NLS-1$
        }
        st = new StringTokenizer(strForm, "/", true); //$NON-NLS-1$
        if (!st.hasMoreTokens()) {
            // jndi.68=Empty URL
            throw new IllegalArgumentException(Messages.getString("jndi.68")); //$NON-NLS-1$
        }
        // scheme
        token = st.nextToken();
        if (!token.equals("dns:")) { //$NON-NLS-1$
            // jndi.69=Specified scheme is not dns
            throw new IllegalArgumentException(Messages.getString("jndi.69")); //$NON-NLS-1$
        }
        // host
        if (st.hasMoreTokens()) {
            token = st.nextToken();
            if (!token.equals("/") || !st.hasMoreTokens()) { //$NON-NLS-1$
                // jndi.6A=Bad URL syntax
                throw new IllegalArgumentException(Messages
                        .getString("jndi.6A")); //$NON-NLS-1$
            }
            token = st.nextToken();
            if (token.equals("/")) { //$NON-NLS-1$
                // host[:port] was given
                if (!st.hasMoreElements()) {
                    // jndi.6A=Bad URL syntax
                    throw new IllegalArgumentException(Messages
                            .getString("jndi.6A")); //$NON-NLS-1$
                }
                token = st.nextToken();
                st2 = new StringTokenizer(token, ":"); //$NON-NLS-1$
                host = st2.nextToken();
                try {
                    ProviderMgr.parseIpStr(host);
                    hostIpWasGiven = true;
                } catch (IllegalArgumentException e) {
                    hostIpWasGiven = false;
                }
                // port
                if (st2.hasMoreTokens()) {
                    port = Integer.parseInt(st2.nextToken());
                }
                // domain
                if (st.hasMoreTokens()) {
                    token = st.nextToken();
                    if (!token.equals("/") || !st.hasMoreTokens()) { //$NON-NLS-1$
                        // jndi.6A=Bad URL syntax
                        throw new IllegalArgumentException(Messages
                                .getString("jndi.6A")); //$NON-NLS-1$
                    }
                    domain = ProviderMgr.normalizeName(st.nextToken());
                }
            } else {
                // domain
                domain = ProviderMgr.normalizeName(token);
            }
            // extra
            if (st.hasMoreTokens()) {
                // jndi.66=Extra characters encountered at the end of the URL
                throw new IllegalArgumentException(Messages
                        .getString("jndi.66")); //$NON-NLS-1$
            }
        }
    }

    /**
     * @return Returns the domain.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return Returns the host.
     */
    public String getHost() {
        return host;
    }

    /**
     * @return Returns the hostIpWasGiven.
     */
    public boolean isHostIpGiven() {
        return hostIpWasGiven;
    }

    /**
     * @return Returns the port.
     */
    public int getPort() {
        return port;
    }

}
