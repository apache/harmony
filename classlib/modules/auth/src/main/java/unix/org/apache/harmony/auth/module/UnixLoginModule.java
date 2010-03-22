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

package org.apache.harmony.auth.module;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.harmony.auth.UnixNumericGroupPrincipal;
import org.apache.harmony.auth.UnixNumericUserPrincipal;
import org.apache.harmony.auth.UnixPrincipal;
import org.apache.harmony.auth.internal.nls.Messages;


/** 
 * A passive LoginModule which keeps an information about current user.
 */
public class UnixLoginModule implements LoginModule {

    private UnixSystem usys;

    private Subject subject;

    private UnixPrincipal user;

    private UnixNumericUserPrincipal uid;

    private UnixNumericGroupPrincipal gid;

    private UnixNumericGroupPrincipal[] gids;

    /** 
     * @throws NullPointerException if either subject or options is null  
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
        if (subject == null) {
            throw new NullPointerException(Messages.getString("auth.03")); //$NON-NLS-1$
        }
        if (options == null) {
            throw new NullPointerException(Messages.getString("auth.04")); //$NON-NLS-1$
        }
        this.subject = subject;
    }

    /** 
     * Performs query to UnixSystem to retrieve user's information. 
     */
    public boolean login() throws LoginException {
        if (usys == null) {
            usys = new UnixSystem();
        }
        usys.load();

        user = new UnixPrincipal(usys.getUsername());
        uid = new UnixNumericUserPrincipal(usys.getUid());
        gid = new UnixNumericGroupPrincipal(usys.getGid(), usys.getGroupName(),
                true);
        long[] gs = usys.getGroups();
        String[] gns = usys.getGroupNames();
        gids = new UnixNumericGroupPrincipal[gs.length];
        for (int i = 0; i < gids.length; i++) {
            gids[i] = new UnixNumericGroupPrincipal(gs[i],
                    i < gns.length ? gns[i] : null, false);
        }
        return true;
    }

    /**
     * Commits the login().
     */
    public boolean commit() throws LoginException {
        if (subject.isReadOnly()) {
            throw new LoginException(Messages.getString("auth.05")); //$NON-NLS-1$
        }
        Set<Principal> ps = subject.getPrincipals();

        if (!ps.contains(user)) {
            ps.add(user);
        }
        if (!ps.contains(uid)) {
            ps.add(uid);
        }
        if (!ps.contains(gid)) {
            ps.add(gid);
        }
        for (UnixNumericGroupPrincipal element : gids) {
            if (!ps.contains(element)) {
                ps.add(element);
            }
        }
        return true;
    }

    /**
     * Aborts the login() attempt and clears its information.
     */
    public boolean abort() throws LoginException {
        clear();
        return true;
    }

    /**
     * Wipes out the information stored in the Subject at the commit() stage, 
     * then clears clears an info store in its own fields.
     */
    public boolean logout() throws LoginException {
        Set<Principal> ps = subject.getPrincipals();
        ps.remove(user);
        ps.remove(uid);
        ps.remove(gid);
        if (gids != null) {
            for (UnixNumericGroupPrincipal element : gids) {
                ps.remove(element);
            }
        }
        clear();
        return true;
    }

    /**
     * Clears information stored in this object.
     */
    private void clear() {
        user = null;
        uid = null;
        gid = null;
        gids = null;
    }
}