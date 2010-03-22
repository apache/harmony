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

package org.apache.harmony.security.tests.java.security;

import java.io.Serializable;
import java.security.AllPermission;
import java.security.SecurityPermission;
import java.security.UnresolvedPermission;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

import tests.util.SerializationTester;

import junit.framework.TestCase;

/**
 * Tests for <code>UnresolvedPermission</code> class fields and methods
 * 
 */

public class UnresolvedPermissionTest extends TestCase {
    
    /**
     * Creates an Object with given name, type, action, certificates. 
     * Empty or null type is not allowed - exception should be thrown.
     */
    public void testCtor()
    {
        String type = "laskjhlsdk 2345346";
        String name = "^%#UHVKU^%V  887y";
        String action = "JHB ^%(*&T klj3h4";
        UnresolvedPermission up = new UnresolvedPermission(type, name, action, null);
        assertEquals(type, up.getName());
        assertEquals("", up.getActions());
        assertEquals("(unresolved " + type + " " + name + " " + action + ")", up.toString());
        
        up = new UnresolvedPermission(type, null, null, null);
        assertEquals(type, up.getName());
        assertEquals("", up.getActions());
        assertEquals("(unresolved " + type + " null null)", up.toString());
        
        up = new UnresolvedPermission(type, "", "", new java.security.cert.Certificate[0]);
        assertEquals(type, up.getName());
        assertEquals("", up.getActions());
        assertEquals("(unresolved " + type + "  )", up.toString());
        
        try {
            new UnresolvedPermission(null, name, action, null);
            fail("No expected NullPointerException");
        } catch (NullPointerException ok) {
        }

        //Regression for HARMONY-733
        up = new UnresolvedPermission("", "name", "action", null);
        assertEquals("", up.getName());
    }
    
    /** 
     * UnresolvedPermission never implies any other permission.
     */
    public void testImplies()
    {
        UnresolvedPermission up = new UnresolvedPermission("java.security.SecurityPermission", "a.b.c", null, null);
        assertFalse(up.implies(up));
        assertFalse(up.implies(new AllPermission()));
        assertFalse(up.implies(new SecurityPermission("a.b.c")));
    }
    
    public void testSerialization() throws Exception {
        UnresolvedPermission up = new UnresolvedPermission(
                "java.security.SecurityPermission", "a.b.c", "actions", null);
        assertEquals("java.security.SecurityPermission", up.getUnresolvedType());
        assertEquals("a.b.c", up.getUnresolvedName());
        assertEquals("actions", up.getUnresolvedActions());
        assertNull(up.getUnresolvedCerts());

        UnresolvedPermission deserializedUp = (UnresolvedPermission) SerializationTester
                .getDeserilizedObject(up);
        assertEquals("java.security.SecurityPermission", deserializedUp
                .getUnresolvedType());
        assertEquals("a.b.c", deserializedUp.getUnresolvedName());
        assertEquals("actions", deserializedUp.getUnresolvedActions());
        assertNull(deserializedUp.getUnresolvedCerts());
    }
    
    public void testSerialization_Compatibility() throws Exception {
        UnresolvedPermission up = new UnresolvedPermission(
                "java.security.SecurityPermission", "a.b.c", "actions", null);
        assertEquals("java.security.SecurityPermission", up.getUnresolvedType());
        assertEquals("a.b.c", up.getUnresolvedName());
        assertEquals("actions", up.getUnresolvedActions());
        assertNull(up.getUnresolvedCerts());

        SerializationTest.verifyGolden(this, up, new SerializableAssert() {
            public void assertDeserialized(Serializable orig, Serializable ser) {
                UnresolvedPermission deserializedUp = (UnresolvedPermission) ser;
                assertEquals("java.security.SecurityPermission", deserializedUp
                        .getUnresolvedType());
                assertEquals("a.b.c", deserializedUp.getUnresolvedName());
                assertEquals("actions", deserializedUp.getUnresolvedActions());
                assertNull(deserializedUp.getUnresolvedCerts());
            }
        });
    }
}
