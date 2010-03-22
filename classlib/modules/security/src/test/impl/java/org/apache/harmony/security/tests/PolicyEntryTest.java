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
* @author Alexey V. Varlamov
*/

package org.apache.harmony.security.tests;

import java.net.URL;
import java.security.cert.Certificate;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Principal;
import java.security.SecurityPermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.harmony.security.PolicyEntry;
import org.apache.harmony.security.UnresolvedPrincipal;
import junit.framework.TestCase;


/**
 * TODO Put your class description here
 * 
 */

public class PolicyEntryTest extends TestCase {

    /** 
     * Tests constructor and accessors of PolicyEntry 
     */
    public void testCtor() {
        PolicyEntry pe = new PolicyEntry(null, null, null);
        assertTrue(pe.isVoid());
        assertNull(pe.getPermissions());

        pe = new PolicyEntry(new CodeSource(null, (Certificate[])null),
            new ArrayList(), new ArrayList());
        assertTrue(pe.isVoid());
        assertNull(pe.getPermissions());

        Collection perms = Arrays.asList(new Permission[] {
            new SecurityPermission("dsfg"), new AllPermission() });
        pe = new PolicyEntry(null, null, perms);
        assertFalse(pe.isVoid());
        assertEquals(perms, new ArrayList(pe.getPermissions()));
    }

    /**
     * Null CodeSource of PolicyEntry implies any CodeSource; non-null
     * CodeSource should delegate to its own imply() functionality
     */
    public void testImpliesCodeSource() throws Exception {
        CodeSource cs0  = new CodeSource(null, (Certificate[]) null);

        CodeSource cs10 = new CodeSource(new URL("file:"), (Certificate[]) null);
        CodeSource cs11 = new CodeSource(new URL("file:/"), (Certificate[]) null);
        CodeSource cs12 = new CodeSource(new URL("file://"), (Certificate[]) null);
        CodeSource cs13 = new CodeSource(new URL("file:///"), (Certificate[]) null);

        CodeSource cs20 = new CodeSource(new URL("file:*"), (Certificate[]) null);
        CodeSource cs21 = new CodeSource(new URL("file:/*"), (Certificate[]) null);
        CodeSource cs22 = new CodeSource(new URL("file://*"), (Certificate[]) null);
        CodeSource cs23 = new CodeSource(new URL("file:///*"), (Certificate[]) null);

        CodeSource cs30 = new CodeSource(new URL("file:-"), (Certificate[]) null);
        CodeSource cs31 = new CodeSource(new URL("file:/-"), (Certificate[]) null);
        CodeSource cs32 = new CodeSource(new URL("file://-"), (Certificate[]) null);
        CodeSource cs33 = new CodeSource(new URL("file:///-"), (Certificate[]) null);

        PolicyEntry pe0  = new PolicyEntry(null, null, null);

        PolicyEntry pe10 = new PolicyEntry(cs10, null, null);
        PolicyEntry pe11 = new PolicyEntry(cs11, null, null);
        PolicyEntry pe12 = new PolicyEntry(cs12, null, null);
        PolicyEntry pe13 = new PolicyEntry(cs13, null, null);

        PolicyEntry pe20 = new PolicyEntry(cs20, null, null);
        PolicyEntry pe21 = new PolicyEntry(cs21, null, null);
        PolicyEntry pe22 = new PolicyEntry(cs22, null, null);
        PolicyEntry pe23 = new PolicyEntry(cs23, null, null);

        PolicyEntry pe30 = new PolicyEntry(cs30, null, null);
        PolicyEntry pe31 = new PolicyEntry(cs31, null, null);
        PolicyEntry pe32 = new PolicyEntry(cs32, null, null);
        PolicyEntry pe33 = new PolicyEntry(cs33, null, null);

        assertTrue (pe0.impliesCodeSource(null));
        assertTrue (pe0.impliesCodeSource(cs10));
        assertTrue (pe0.impliesCodeSource(cs11));
        assertTrue (pe0.impliesCodeSource(cs12));
        assertTrue (pe0.impliesCodeSource(cs13));
        assertTrue (pe0.impliesCodeSource(cs20));
        assertTrue (pe0.impliesCodeSource(cs21));
        assertTrue (pe0.impliesCodeSource(cs22));
        assertTrue (pe0.impliesCodeSource(cs23));
        assertTrue (pe0.impliesCodeSource(cs30));
        assertTrue (pe0.impliesCodeSource(cs31));
        assertTrue (pe0.impliesCodeSource(cs32));
        assertTrue (pe0.impliesCodeSource(cs33));

        assertFalse(pe10.impliesCodeSource(null));
        assertTrue (pe10.impliesCodeSource(cs10));
        assertFalse(pe10.impliesCodeSource(cs11));
        assertTrue (pe10.impliesCodeSource(cs12));
        assertFalse(pe10.impliesCodeSource(cs13));
        assertTrue (pe10.impliesCodeSource(cs20));
        assertFalse(pe10.impliesCodeSource(cs21));
        assertFalse(pe10.impliesCodeSource(cs22));
        assertFalse(pe10.impliesCodeSource(cs23));
        assertTrue (pe10.impliesCodeSource(cs30));
        assertFalse(pe10.impliesCodeSource(cs31));
        assertFalse(pe10.impliesCodeSource(cs32));
        assertFalse(pe10.impliesCodeSource(cs33));

        assertFalse(pe11.impliesCodeSource(null));
        assertFalse(pe11.impliesCodeSource(cs10));
        assertTrue (pe11.impliesCodeSource(cs11));
        assertFalse(pe11.impliesCodeSource(cs12));
        assertTrue (pe11.impliesCodeSource(cs13));
        assertFalse(pe11.impliesCodeSource(cs20));
        assertFalse(pe11.impliesCodeSource(cs21));
        assertFalse(pe11.impliesCodeSource(cs22));
        assertFalse(pe11.impliesCodeSource(cs23));
        assertFalse(pe11.impliesCodeSource(cs30));
        assertFalse(pe11.impliesCodeSource(cs31));
        assertFalse(pe11.impliesCodeSource(cs32));
        assertFalse(pe11.impliesCodeSource(cs33));

        assertFalse(pe12.impliesCodeSource(null));
        assertTrue (pe12.impliesCodeSource(cs10));
        assertFalse(pe12.impliesCodeSource(cs11));
        assertTrue (pe12.impliesCodeSource(cs12));
        assertFalse(pe12.impliesCodeSource(cs13));
        assertTrue (pe12.impliesCodeSource(cs20));
        assertFalse(pe12.impliesCodeSource(cs21));
        assertFalse(pe12.impliesCodeSource(cs22));
        assertFalse(pe12.impliesCodeSource(cs23));
        assertTrue (pe12.impliesCodeSource(cs30));
        assertFalse(pe12.impliesCodeSource(cs31));
        assertFalse(pe12.impliesCodeSource(cs32));
        assertFalse(pe12.impliesCodeSource(cs33));

        assertFalse(pe13.impliesCodeSource(null));
        assertFalse(pe13.impliesCodeSource(cs10));
        assertTrue (pe13.impliesCodeSource(cs11));
        assertFalse(pe13.impliesCodeSource(cs12));
        assertTrue (pe13.impliesCodeSource(cs13));
        assertFalse(pe13.impliesCodeSource(cs20));
        assertFalse(pe13.impliesCodeSource(cs21));
        assertFalse(pe13.impliesCodeSource(cs22));
        assertFalse(pe13.impliesCodeSource(cs23));
        assertFalse(pe13.impliesCodeSource(cs30));
        assertFalse(pe13.impliesCodeSource(cs31));
        assertFalse(pe13.impliesCodeSource(cs32));
        assertFalse(pe13.impliesCodeSource(cs33));

        assertFalse(pe20.impliesCodeSource(null));
        assertTrue (pe20.impliesCodeSource(cs10));
        assertFalse(pe20.impliesCodeSource(cs11));
        assertTrue (pe20.impliesCodeSource(cs12));
        assertFalse(pe20.impliesCodeSource(cs13));
        assertTrue (pe20.impliesCodeSource(cs20));
        assertFalse(pe20.impliesCodeSource(cs21));
        assertFalse(pe20.impliesCodeSource(cs22));
        assertFalse(pe20.impliesCodeSource(cs23));
        assertTrue (pe20.impliesCodeSource(cs30));
        assertFalse(pe20.impliesCodeSource(cs31));
        assertFalse(pe20.impliesCodeSource(cs32));
        assertFalse(pe20.impliesCodeSource(cs33));

        assertFalse(pe21.impliesCodeSource(null));
        assertFalse(pe21.impliesCodeSource(cs10));
        assertTrue (pe21.impliesCodeSource(cs11));
        assertFalse(pe21.impliesCodeSource(cs12));
        assertTrue (pe21.impliesCodeSource(cs13));
        assertFalse(pe21.impliesCodeSource(cs20));
        assertTrue (pe21.impliesCodeSource(cs21));
        assertFalse(pe21.impliesCodeSource(cs22));
        assertTrue (pe21.impliesCodeSource(cs23));
        assertFalse(pe21.impliesCodeSource(cs30));
        assertTrue (pe21.impliesCodeSource(cs31));
        assertFalse(pe21.impliesCodeSource(cs32));
        assertTrue (pe21.impliesCodeSource(cs33));

        assertFalse(pe22.impliesCodeSource(null));
        assertFalse(pe22.impliesCodeSource(cs10));
        // assertFalse(pe22.impliesCodeSource(cs11));
        assertFalse(pe22.impliesCodeSource(cs12));
        // assertFalse(pe22.impliesCodeSource(cs13));
        assertFalse(pe22.impliesCodeSource(cs20));
        assertFalse(pe22.impliesCodeSource(cs21));
        assertTrue (pe22.impliesCodeSource(cs22));
        assertFalse(pe22.impliesCodeSource(cs23));
        assertFalse(pe22.impliesCodeSource(cs30));
        assertFalse(pe22.impliesCodeSource(cs31));
        assertTrue (pe22.impliesCodeSource(cs32));
        assertFalse(pe22.impliesCodeSource(cs33));

        assertFalse(pe23.impliesCodeSource(null));
        assertFalse(pe23.impliesCodeSource(cs10));
        assertTrue (pe23.impliesCodeSource(cs11));
        assertFalse(pe23.impliesCodeSource(cs12));
        assertTrue (pe23.impliesCodeSource(cs13));
        assertFalse(pe23.impliesCodeSource(cs20));
        assertTrue (pe23.impliesCodeSource(cs21));
        assertFalse(pe23.impliesCodeSource(cs22));
        assertTrue (pe23.impliesCodeSource(cs23));
        assertFalse(pe23.impliesCodeSource(cs30));
        assertTrue (pe23.impliesCodeSource(cs31));
        assertFalse(pe23.impliesCodeSource(cs32));
        assertTrue (pe23.impliesCodeSource(cs33));

        assertFalse(pe30.impliesCodeSource(null));
        assertTrue (pe30.impliesCodeSource(cs10));
        assertFalse(pe30.impliesCodeSource(cs11));
        assertTrue (pe30.impliesCodeSource(cs12));
        assertFalse(pe30.impliesCodeSource(cs13));
        assertTrue (pe30.impliesCodeSource(cs20));
        assertFalse(pe30.impliesCodeSource(cs21));
        assertFalse(pe30.impliesCodeSource(cs22));
        assertFalse(pe30.impliesCodeSource(cs23));
        assertTrue (pe30.impliesCodeSource(cs30));
        assertFalse(pe30.impliesCodeSource(cs31));
        assertFalse(pe30.impliesCodeSource(cs32));
        assertFalse(pe30.impliesCodeSource(cs33));

        assertFalse(pe31.impliesCodeSource(null));
        assertTrue (pe31.impliesCodeSource(cs10));
        assertTrue (pe31.impliesCodeSource(cs11));
        assertTrue (pe31.impliesCodeSource(cs12));
        assertTrue (pe31.impliesCodeSource(cs13));
        assertTrue (pe31.impliesCodeSource(cs20));
        assertTrue (pe31.impliesCodeSource(cs21));
        assertFalse(pe31.impliesCodeSource(cs22));
        assertTrue (pe31.impliesCodeSource(cs23));
        assertTrue (pe31.impliesCodeSource(cs30));
        assertTrue (pe31.impliesCodeSource(cs31));
        assertFalse(pe31.impliesCodeSource(cs32));
        assertTrue (pe31.impliesCodeSource(cs33));

        assertFalse(pe32.impliesCodeSource(null));
        assertFalse(pe32.impliesCodeSource(cs10));
        assertFalse(pe32.impliesCodeSource(cs11));
        assertFalse(pe32.impliesCodeSource(cs12));
        assertFalse(pe32.impliesCodeSource(cs13));
        assertFalse(pe32.impliesCodeSource(cs20));
        assertFalse(pe32.impliesCodeSource(cs21));
        assertFalse(pe32.impliesCodeSource(cs22));
        assertFalse(pe32.impliesCodeSource(cs23));
        assertFalse(pe32.impliesCodeSource(cs30));
        assertFalse(pe32.impliesCodeSource(cs31));
        assertTrue (pe32.impliesCodeSource(cs32));
        assertFalse(pe32.impliesCodeSource(cs33));

        assertFalse(pe33.impliesCodeSource(null));
        assertTrue (pe33.impliesCodeSource(cs10));
        assertTrue (pe33.impliesCodeSource(cs11));
        assertTrue (pe33.impliesCodeSource(cs12));
        assertTrue (pe33.impliesCodeSource(cs13));
        assertTrue (pe33.impliesCodeSource(cs20));
        assertTrue (pe33.impliesCodeSource(cs21));
        assertFalse(pe33.impliesCodeSource(cs22));
        assertTrue (pe33.impliesCodeSource(cs23));
        assertTrue (pe33.impliesCodeSource(cs30));
        assertTrue (pe33.impliesCodeSource(cs31));
        assertFalse(pe33.impliesCodeSource(cs32));
        assertTrue (pe33.impliesCodeSource(cs33));
    }

    /**
     * Null or empty set of Principals of PolicyEntry implies any Principals;
     * otherwise tested set must contain all Principals of PolicyEntry.
     */
    public void testImpliesPrincipals() {
        PolicyEntry pe = new PolicyEntry(null, null, null);
        Principal[] pp1 = new Principal[] {};
        Principal[] pp2 = new Principal[] { new UnresolvedPrincipal("a.b.c",
            "XXX") };
        Principal[] pp3 = new Principal[] {
            new UnresolvedPrincipal("a.b.c", "YYY"),
            new UnresolvedPrincipal("a.b.c", "XXX"),
            new UnresolvedPrincipal("e.f.g", "ZZZ") };

        assertTrue(pe.impliesPrincipals(null));
        assertTrue(pe.impliesPrincipals(pp1));

        pe = new PolicyEntry(null, new HashSet(), null);
        assertTrue(pe.impliesPrincipals(pp3));

        pe = new PolicyEntry(null, Arrays.asList(pp2), null);
        assertFalse(pe.impliesPrincipals(null));
        assertFalse(pe.impliesPrincipals(pp1));
        assertTrue(pe.impliesPrincipals(pp3));
    }
}
