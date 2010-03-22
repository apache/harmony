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
* @author Maxim V. Makarov
*/

package org.apache.harmony.auth.tests.javax.security.auth.kerberos.serialization;

import java.security.Permission;
import java.security.PermissionCollection;
import javax.security.auth.kerberos.ServicePermission;
import org.apache.harmony.testframework.serialization.SerializationTest;

/**
 * Serialization test for KrbServicePermissionCollection class
 */
public class KrbServicePermissionCollectionTest extends SerializationTest {

    @Override
    protected Object[] getData() {
        Permission p1 = new ServicePermission("AAA", "accept");
        Permission p2 = new ServicePermission("BBB", "initiate");
        Permission p3 = new ServicePermission("CCC", "initiate, accept");
        PermissionCollection pc1 = p1.newPermissionCollection();
        PermissionCollection pc2 = p1.newPermissionCollection();
        pc2.add(p3);
        PermissionCollection pc3 = p1.newPermissionCollection();
        pc3.add(p1);
        pc3.add(p2);
        pc3.add(p3);
        return new Object[] { pc1, pc2, pc3 };
    }

}
