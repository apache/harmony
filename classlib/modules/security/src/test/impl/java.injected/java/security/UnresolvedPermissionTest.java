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

package java.security;

import junit.framework.TestCase;

/**
 * Tests for <code>UnresolvedPermission</code> class fields and methods
 * 
 */

public class UnresolvedPermissionTest extends TestCase {

    /**
     * newPermissionCollection() should return new BasicPermissionCollection on every invocation
     */
    public void testCollection()
    {
        UnresolvedPermission up = new UnresolvedPermission("a.b.c", null, null, null);
        PermissionCollection pc1 = up.newPermissionCollection();
        PermissionCollection pc2 = up.newPermissionCollection();
        assertTrue((pc1 instanceof UnresolvedPermissionCollection) && (pc2 instanceof UnresolvedPermissionCollection));
        assertNotSame(pc1, pc2);
    }
}
