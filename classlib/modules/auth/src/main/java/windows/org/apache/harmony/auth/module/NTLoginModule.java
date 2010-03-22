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

/**
 * @author Alexander V. Astapchuk
 */
package org.apache.harmony.auth.module;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.harmony.auth.NTDomainPrincipal;
import org.apache.harmony.auth.NTNumericCredential;
import org.apache.harmony.auth.NTSidDomainPrincipal;
import org.apache.harmony.auth.NTSidGroupPrincipal;
import org.apache.harmony.auth.NTSidPrimaryGroupPrincipal;
import org.apache.harmony.auth.NTSidUserPrincipal;
import org.apache.harmony.auth.NTUserPrincipal;
import org.apache.harmony.auth.internal.nls.Messages;


/** 
 * A passive LoginModule which keeps an NT user's information.
 */
public class NTLoginModule implements LoginModule {

    private Subject subject;

    private Map<String, ?> options;

    private NTSystem sys;

    private NTUserPrincipal user;

    private NTDomainPrincipal domain;

    private NTSidUserPrincipal userSid;

    private NTSidDomainPrincipal domainSid;

    private NTSidPrimaryGroupPrincipal mainGroupSid;

    private NTSidGroupPrincipal[] groupsSids;

    private NTNumericCredential credential;

    public void initialize(Subject subject, CallbackHandler cbHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
        if (subject == null) {
            throw new NullPointerException(Messages.getString("auth.03")); //$NON-NLS-1$
        }
        if (options == null) {
            throw new NullPointerException(Messages.getString("auth.04")); //$NON-NLS-1$
        }
        this.subject = subject;
        //cbHandler - unused in this version
        //sharedState - unused
        this.options = options;
    }

    /**
     * Clears information stored in this object.
     */
    private void clear() {
        if (sys != null) {
            sys.free();
        }
        user = null;
        domain = null;
        userSid = null;
        domainSid = null;
        mainGroupSid = null;
        groupsSids = null;
        credential = null;
    }

    /**
     * Aborts the login() attempt and clears its information.
     */
    public boolean abort() throws LoginException {
        clear();
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

        if (!ps.contains(domain)) {
            ps.add(domain);
        }

        if (!ps.contains(userSid)) {
            ps.add(userSid);
        }

        if (!ps.contains(domainSid)) {
            ps.add(domainSid);
        }

        if (!ps.contains(mainGroupSid)) {
            ps.add(mainGroupSid);
        }

        for (NTSidGroupPrincipal element : groupsSids) {
            if (!ps.contains(element)) {
                ps.add(element);
            }
        }
        Set<Object> creds = subject.getPrivateCredentials();
        if (!creds.contains(credential)) {
            creds.add(credential);
        }
        return true;
    }

    /** 
     * Performs query to NTSystem to retrieve user's security information. 
     */
    public boolean login() throws LoginException {
        if (sys != null) {
            sys.free();
        } else {
            sys = new NTSystem(options);
        }
        sys.load();

        user = new NTUserPrincipal(sys.getName());
        domain = new NTDomainPrincipal(sys.getDomain());
        domainSid = new NTSidDomainPrincipal(sys.getDomainSID());
        userSid = new NTSidUserPrincipal(sys.getUserSID());

        mainGroupSid = sys.mainGroup;
        groupsSids = sys.groups;
        credential = new NTNumericCredential(sys.getImpersonationToken());

        return true;
    }

    /**
     * Wipes out the information stored in the Subject at the commit() stage, 
     * then clears clears an info store in its own fields.
     */
    public boolean logout() throws LoginException {
        if (subject.isReadOnly()) {
            throw new LoginException(Messages.getString("auth.05")); //$NON-NLS-1$
        }
        Set<Principal> ps = subject.getPrincipals();

        if (user != null) {
            ps.remove(user);
        }
        if (domain != null) {
            ps.remove(domain);
        }
        if (userSid != null) {
            ps.remove(userSid);
        }
        if (domainSid != null) {
            ps.remove(domainSid);
        }
        if (mainGroupSid != null) {
            ps.remove(mainGroupSid);
        }
        if (groupsSids != null) {
            for (NTSidGroupPrincipal element : groupsSids) {
                ps.remove(element);
            }
        }

        if (credential != null) {
            Set<Object> creds = subject.getPrivateCredentials();
            creds.remove(credential);
        }

        clear();
        return true;
    }
}