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
package org.apache.harmony.auth;

import java.security.Principal;
import java.io.Serializable;

import org.apache.harmony.auth.internal.nls.Messages;

/** 
 * A Principal class which serves as a base class for many SID-based 
 * principals. 
 */
public class NTSid implements Serializable, Principal {

    private static final long serialVersionUID = 5589132933506948177L;

    // SID
    private String sid;

    // Name of the object whose SID is kept here
    private String name;

    // Domain of the object
    private String domain;

    /**
     * A constructor which takes object's SID as its only argument. 
     * @param sid SID
     */
    public NTSid(String sid) {
        if (sid == null) {
            throw new NullPointerException(Messages.getString("auth.01")); //$NON-NLS-1$
        }
        if (sid.length() == 0) {
            throw new IllegalArgumentException(Messages.getString("auth.02")); //$NON-NLS-1$
        }
        this.sid = sid;
    }

    /**
     * A constructor which takes an extended set of information - object's SID, 
     * its name and name of its domain
     * @param sid SID
     * @param objName name of the object whose SID is stored
     * @param objDomain name of the domain the object belongs to
     */
    public NTSid(String sid, String objName, String objDomain) {
        this(sid);
        name = objName;
        domain = objDomain;
    }

    /**
     * Returns object's SID. Same as {@link #getSid()}.
     */
    public String getName() {
        return sid;
    }

    /**
     * Returns object's SID.
     */
    public String getSid() {
        return sid;
    }

    /**
     * Returns name of the object whose SID is stored in this NTSid.
     */
    public String getObjectName() {
        return name;
    }

    /**
     * Returns name of the object's domain.
     */
    public String getObjectDomain() {
        return domain;
    }

    /**
     * Returns String representation of this object.
     */
    @Override
    public String toString() {
        String str = getClass().getName();
        int dot = str.lastIndexOf('.');
        str = str.substring(dot + 1) + ": "; //$NON-NLS-1$
        str += name + '@' + domain;
        str += "; SID=" + sid; //$NON-NLS-1$
        return str;
    }

    /**
     * Tests two objects for equality.<br>
     * Two objects are considered equal if they both represent 
     * NTSid and they both have the same sid value.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof NTSid) {
            return sid.equals(((NTSid) obj).sid);
        }
        return false;
    }

    /**
     * Returns hashCode for this object.
     */
    @Override
    public int hashCode() {
        return sid.hashCode();
    }
}