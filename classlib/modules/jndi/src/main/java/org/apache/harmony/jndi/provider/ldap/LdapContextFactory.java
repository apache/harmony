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

package org.apache.harmony.jndi.provider.ldap;

import java.util.Hashtable;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.apache.harmony.jndi.provider.ldap.parser.LdapUrlParser;

public class LdapContextFactory implements InitialContextFactory {

    public Context getInitialContext(Hashtable<?, ?> envmt)
            throws NamingException {
        Hashtable<Object, Object> myEnv = null;
        if (envmt == null) {
            myEnv = new Hashtable<Object, Object>();
        } else {
            myEnv = (Hashtable<Object, Object>) envmt.clone();
        }
        String url = (String) myEnv.get(Context.PROVIDER_URL);
        LdapUrlParser parser = null;
        try {
            parser = LdapUtils.parserURL(url, false);
        } catch (InvalidNameException e) {
            throw new ConfigurationException(e.getMessage());
        }

        String host = parser.getHost();
        int port = parser.getPort();
        String dn = parser.getBaseObject();

        LdapClient client = LdapClient.newInstance(host, port, myEnv, LdapUtils
                .isLdapsURL(url));

        LdapContextImpl context = new LdapContextImpl(client, myEnv, dn);

        return context;

    }
}
