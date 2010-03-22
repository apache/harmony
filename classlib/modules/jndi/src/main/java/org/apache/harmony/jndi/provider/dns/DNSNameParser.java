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
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameParser;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * DNS name parser
 */
public class DNSNameParser implements NameParser {

    /**
     * Constructs a new name parser.
     */
    public DNSNameParser() {
    }

    /**
     * Parses string representation of DNS name. Following situations will be
     * treated as an error:
     * <ul>
     * <li>The length of the whole name is longer than 255 characters</li>
     * <li>The length of each label is more than 63 characters</li>
     * <li>more than one null label encountered or null label is not the least
     * specific label (the rightmost)</li>
     * </ul>
     * 
     * @param name
     *            string representation of DNS name
     * @return new instance of <code>DNSName</code> class
     * @throws InvalidNameException
     *             if given string is not a correct DNS name
     * @see javax.naming.NameParser#parse(java.lang.String)
     * @see RFC 1034
     */
    public Name parse(String name) throws InvalidNameException {
        StringTokenizer st;
        boolean lastTokenWasDilim = false;
        DNSName dnsName = new DNSName();

        if (name == null) {
            // jndi.2E=The name is null
            throw new InvalidNameException(Messages.getString("jndi.2E")); //$NON-NLS-1$
        }
        if (name.length() > 255) {
            // jndi.54=The length of the name is more than 255 characters
            throw new InvalidNameException(Messages.getString("jndi.54")); //$NON-NLS-1$
        }
        st = new StringTokenizer(name, ".", true); //$NON-NLS-1$
        while (st.hasMoreTokens()) {
            String comp = st.nextToken();

            if (comp.equals(".")) { //$NON-NLS-1$
                if (lastTokenWasDilim) {
                    // two delimiters one after another
                    // jndi.55=Null label is not the rightmost one
                    throw new InvalidNameException(Messages
                            .getString("jndi.55")); //$NON-NLS-1$
                }
                lastTokenWasDilim = true;
                if (dnsName.size() == 0 && st.hasMoreTokens()) {
                    // jndi.56=DNS name shouldn't start with a dot
                    throw new InvalidNameException(Messages
                            .getString("jndi.56")); //$NON-NLS-1$
                }
            } else {
                if (comp.length() > 63) {
                    // jndi.57=The length of {0} label is more than 63
                    // characters
                    throw new InvalidNameException(Messages.getString(
                            "jndi.57", comp)); //$NON-NLS-1$
                }
                dnsName.add(0, comp);
                lastTokenWasDilim = false;
            }
        }
        if (lastTokenWasDilim) {
            dnsName.add(0, ""); //$NON-NLS-1$
        }
        return dnsName;
    }

    /**
     * @param obj
     *            the object to compare with
     * @return <code>true</code> if and only if the given object is instance
     *         of class <code>DNSParser</code>; otherwise returns
     *         <code>false</code>.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof DNSNameParser) {
            return true;
        }
        return false;
    }

    /**
     * Return the hashcode of the receiver.
     */
    @Override
    public int hashCode() {
        return 1;
    }
}
