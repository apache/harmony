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

package org.apache.harmony.jndi.provider.ldap.event;

import org.apache.harmony.jndi.provider.ldap.LdapSearchResult;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;

/**
 * Search result for persisten search.
 */
abstract public class PersistentSearchResult extends LdapSearchResult {

    private String dn;

    @Override
    protected void decodeRef(Object value) {
        /*
         * TODO test ri behavior, how to deal with referrals in persistent
         * search or maybe referral would be never received
         */
    }

    @Override
    protected void decodeEntry(Object value) {
        Object[] values = (Object[]) value;
        dn = Utils.getString((byte[]) values[0]);
        // TODO is attributes useful for persistent search?
    }

    /**
     * This is a callback method which would be invoked when ldap client receive
     * notifaction from server.
     * 
     * @param object
     *            Received notification from server, one of
     *            <code>ECNotificationControl</code> or
     *            <code>LdapResult</code>
     */
    abstract public void receiveNotificationHook(Object object);

    /**
     * Get DN of changed entry
     * 
     * @return DN of changed entry
     */
    public String getDn() {
        return dn;
    }

}
