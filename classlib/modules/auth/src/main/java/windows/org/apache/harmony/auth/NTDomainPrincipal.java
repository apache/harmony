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
 * A Principal which stores information NT domain name 
 */
public class NTDomainPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 7574453578624887003L;

    // Domain name
    private String name;

    /**
     * Construct an instance with the given domain name.
     * @throws NullPointerException is name is null
     */
    public NTDomainPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException(Messages.getString("auth.00")); //$NON-NLS-1$
        }
        this.name = name;
    }

    /**
     * Returns domain name
     */
    public String getName() {
        return name;
    }

    /**
     * Tests two objects for equality.<br>
     * Two objects are considered equal if they both represent NTDomainPrincipal
     * and they have the same domain name.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof NTDomainPrincipal) {
            return name.equals(((NTDomainPrincipal) obj).name);
        }
        return false;
    }

    /**
     * Returns hashCode that value is equal to getName().hashCode();
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Returns String representation of this object.
     */
    @Override
    public String toString() {
        return "NTDomainPrincipal: " + name; //$NON-NLS-1$
    }
}