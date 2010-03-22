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

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapReferralException;

import org.apache.harmony.jndi.provider.ldap.parser.LdapUrlParser;

public class ReferralExceptionImpl extends LdapReferralException {

    private static final long serialVersionUID = -485662331078312979L;

    private String targetDN;

    private String[] referrals;

    private Hashtable<Object, Object> env;

    private int index = 0;

    public ReferralExceptionImpl(String targetDN, String[] referrals,
            Hashtable<Object, Object> env) {
        this.targetDN = targetDN;
        this.referrals = referrals;
        if (env == null) {
            this.env = new Hashtable<Object, Object>();
        } else {
            this.env = (Hashtable<Object, Object>) env.clone();
        }
    }

    @Override
    public Context getReferralContext() throws NamingException {
        if (index >= referrals.length) {
            return null;
        }

        LdapUrlParser parser = LdapUtils.parserURL(referrals[index], false);

        String host = parser.getHost();
        int port = parser.getPort();

        LdapClient client = LdapClient.newInstance(host, port, env, LdapUtils
                .isLdapsURL(referrals[index]));

        LdapContextImpl context = new LdapContextImpl(client,
                (Hashtable<Object, Object>) env, targetDN);
        return context;

    }

    @Override
    public Context getReferralContext(Hashtable<?, ?> h) throws NamingException {
        if (index >= referrals.length) {
            return null;
        }

        if (h == null) {
            return getReferralContext();
        }

        Hashtable<Object, Object> myEnv = (Hashtable<Object, Object>) h.clone();
        LdapUrlParser parser = LdapUtils.parserURL(referrals[index], true);

        String host = parser.getHost();
        int port = parser.getPort();

        LdapClient client = LdapClient.newInstance(host, port, myEnv, LdapUtils
                .isLdapsURL(referrals[index]));

        LdapContextImpl context = new LdapContextImpl(client, myEnv, targetDN);

        return context;
    }

    @Override
    public Object getReferralInfo() {
        if (index >= referrals.length) {
            return null;
        }
        return referrals[index];
    }

    @Override
    public void retryReferral() {
        // TODO what should we do?
        // do nothing
    }

    @Override
    public boolean skipReferral() {
        index++;
        return index < referrals.length;
    }

    @Override
    public Context getReferralContext(Hashtable<?, ?> h, Control[] cs)
            throws NamingException {
        Hashtable<Object, Object> myEnv = (Hashtable<Object, Object>) h;
        myEnv.put("java.naming.ldap.control.connect", cs); //$NON-NLS-1$
        return getReferralContext(myEnv);
    }

}
