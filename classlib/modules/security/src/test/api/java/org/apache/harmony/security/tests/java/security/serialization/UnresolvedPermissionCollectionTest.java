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

package org.apache.harmony.security.tests.java.security.serialization;

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.UnresolvedPermission;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;

import org.apache.harmony.testframework.serialization.SerializationTest;


/**
 * Serialization tests for <code>UnresolvedPermissionCollection</code>
 * 
 */
public class UnresolvedPermissionCollectionTest extends SerializationTest {

    /**
     * Return array holding 3 collections: empty, single- and multi-element.
     */
    protected Object[] getData() {
        Permission up1 = new UnresolvedPermission("131234", null, null, null);
        Permission up2 = new UnresolvedPermission("131234", "ui23rjh", null,
                null);
        Permission up3 = new UnresolvedPermission("KUJKHVKJgyuygjhb", "xcv456",
                "26r ytf", new java.security.cert.Certificate[0]);
        PermissionCollection pc1 = up1.newPermissionCollection();
        PermissionCollection pc2 = up1.newPermissionCollection();
        pc2.add(up3);
        PermissionCollection pc3 = up1.newPermissionCollection();
        pc3.add(up1);
        pc3.add(up2);
        pc3.add(up3);
        return new Object[] { pc1, pc2, pc3 };
    }
}