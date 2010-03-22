/* 
 *  Licensed to the Apache Software Foundation (ASF) under one or more 
 *  contributor license agreements.  See the NOTICE file distributed with 
 *  this work for additional information regarding copyright ownership. 
 *  The ASF licenses this file to You under the Apache License, Version 2.0 
 *  (the "License"); you may not use this file except in compliance with 
 *  the License.  You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License. 
 */

package javax.naming.ldap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

import javax.naming.NamingException;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * TODO: JavaDoc
 */
public class StartTlsRequest implements ExtendedRequest {

    private static final long serialVersionUID = 4441679576360753397L;

    public static final String OID = "1.3.6.1.4.1.1466.20037"; //$NON-NLS-1$

    public byte[] getEncodedValue() {
        return null;
    }

    public String getID() {
        return OID;
    }

    public ExtendedResponse createExtendedResponse(String id, byte[] berValue,
            int offset, int length) throws NamingException {

        if (id != null && !OID.equals(id)) {
            throw new NamingException(Messages.getString("ldap.07") + " " + id);
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        try {
            Enumeration<URL> en = cl
                    .getResources("META-INF/services/javax.naming.ldap.StartTlsResponse"); //$NON-NLS-1$
            while (en.hasMoreElements()) {
                URL confFile = en.nextElement();
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(confFile
                            .openStream()));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // remove comments, spaces, and tabs
                        String className = line.split("#")[0].trim(); //$NON-NLS-1$

                        // try to load class
                        if (!(className.equals(""))) { //$NON-NLS-1$
                            try {
                                /*
                                 * TODO: the spec requires to return an instance
                                 * of the first class that might be successfully
                                 * instantiated, RI breaks on the first
                                 * unsuccessful attempt. We follow the spec
                                 * here.
                                 */
                                return (StartTlsResponse) Class.forName(
                                        className, true, cl).newInstance();
                            } catch (Exception ignore) {
                                // ignore
                            }
                        }
                    }
                } catch (IOException e) {
                    // ignore
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
            }
        } catch (IOException e) {
            // ignore
        }

        try {
            return  (StartTlsResponse) Class.forName(
                    "org.apache.harmony.jndi.provider.ldap.ext.StartTlsResponseImpl", true, cl).newInstance();
        } catch (Exception e) {
            // ignore
        }
        
        throw new NamingException(Messages.getString("ldap.09")); //$NON-NLS-1$
    }
}
