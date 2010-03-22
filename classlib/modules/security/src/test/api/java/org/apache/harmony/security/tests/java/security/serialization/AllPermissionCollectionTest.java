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

import java.security.AllPermission;
import java.security.PermissionCollection;
import java.util.Enumeration;

import org.apache.harmony.testframework.serialization.SerializationTest;


/**
 * Serialization tests for <code>AllPermissionCollection</code>
 * 
 */

public class AllPermissionCollectionTest extends SerializationTest {

    protected Object[] getData() {
        PermissionCollection c1 = new AllPermission().newPermissionCollection();
        PermissionCollection c2 = new AllPermission().newPermissionCollection();
        c2.add(new AllPermission());
        return new Object[] { c1, c2 };
    }
}