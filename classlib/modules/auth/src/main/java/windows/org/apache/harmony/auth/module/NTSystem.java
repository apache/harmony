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

import java.util.Map;

import org.apache.harmony.auth.NTSidGroupPrincipal;
import org.apache.harmony.auth.NTSidPrimaryGroupPrincipal;
import org.apache.harmony.auth.NTSidUserPrincipal;

/** 
 * A helper class which queries information about the current NT user.
 */
public final class NTSystem {

    // Shows whether the hyauth library was loaded or not
    private static boolean loadLibDone;

    // User's sid, domain and name
    private NTSidUserPrincipal user;

    // User's domain sid
    private String domainSid;

    // User's primary group
    /**/NTSidPrimaryGroupPrincipal mainGroup;

    // A list of groups the user belongs to
    /**/NTSidGroupPrincipal[] groups;

    // Impersonation token
    private long token;

    // May be used to trace the native library execution    
    @SuppressWarnings("unused")
    private boolean debugNative;

    /**
     * The default constructor. Loads hyauth library if necessary.
     * @throws UnsatisfiedLinkError if library hyauth not found
     */
    public NTSystem() {
        super();
        synchronized (NTSystem.class) {
            if (!loadLibDone) {
                System.loadLibrary("hyauth"); //$NON-NLS-1$
                initNatives();
                loadLibDone = true;
            }
        }
    }

    /**
     * Constructs an instance with options.
     * @param options
     */
    public NTSystem(Map<String, ?> options) {
        this();
        debugNative = "true".equalsIgnoreCase((String) options //$NON-NLS-1$
                .get("debugNative")); //$NON-NLS-1$
    }

    /**
     * Initializes internal data.
     */
    private static native void initNatives();

    /**
     * Load the security information about user.
     */
    public native void load();

    /**
     * Frees internal data stored during login().
     */
    public native void free();

    /**
     * Returns name of user's domain
     */
    public String getDomain() {
        return user.getObjectDomain();
    }

    /**
     * Returns String representation of SID of user's domain
     */
    public String getDomainSID() {
        return domainSid;
    }

    /**
     * Returns array of SIDs of groups the user belongs to
     */
    public String[] getGroupIDs() {
        if (groups == null || groups.length == 0) {
            return null;
        }
        String[] gids = new String[groups.length];
        for (int i = 0; i < groups.length; i++) {
            gids[i] = groups[i].getName();
        }
        return gids;
    }

    /**
     * Returns implementation token
     */
    public long getImpersonationToken() {
        return token;
    }

    /**
     * Returns user name
     */
    public String getName() {
        return user.getObjectName();
    }

    /**
     * Returns a SID of user's main group
     */
    public String getPrimaryGroupID() {
        return mainGroup.getSid();
    }

    /**
     * Returns user's SID
     */
    public String getUserSID() {
        return user.getSid();
    }

    /**
     * Returns a String representation of this object.
     */
    @Override
    public String toString() {
        String s = "NTSystem:\n"; //$NON-NLS-1$
        s += "   user         : " + user + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
        s += "   domainSid    : " + domainSid + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
        s += "   mainGroup    : " + mainGroup + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
        s += "   token        : " + token + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
        s += "   groups count : " + (groups == null ? 0 : groups.length); //$NON-NLS-1$
        if (groups != null) {
            s += "\n"; //$NON-NLS-1$
            for (int i = 0; i < groups.length; i++) {
                s += "      " + i + "] " + groups[i] + "\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }
        return s;
    }

    /**
     * Returns an array of groups the user belongs to
     */
    public NTSidGroupPrincipal[] getGroups() {
        if (groups == null) {
            return null;
        }
        NTSidGroupPrincipal[] tmp = new NTSidGroupPrincipal[groups.length];
        System.arraycopy(groups, 0, tmp, 0, groups.length);
        return tmp;
    }
}
