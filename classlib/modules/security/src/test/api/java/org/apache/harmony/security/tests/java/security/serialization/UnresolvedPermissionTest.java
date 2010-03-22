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

import java.io.ByteArrayInputStream;
import java.security.UnresolvedPermission;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import org.apache.harmony.security.tests.support.cert.TestUtils;
import org.apache.harmony.testframework.serialization.SerializationTest;



/**
 * Serialization tests for <code>UnresolvedPermission</code>
 * 
 */

public class UnresolvedPermissionTest extends SerializationTest {

    /**
     * @see com.intel.drl.test.SerializationTest#getData()
     */
    protected Object[] getData() {
        // test with real certificates ?
        return new Object[] {
                new UnresolvedPermission("type", "name", "actions", null),
                new UnresolvedPermission("type", null, null, new Certificate[0]) };
    }
    
    public void testSerializationWithCertificates() throws Exception {

        // Regression for HARMONY-2762
        CertificateFactory certificateFactory = CertificateFactory
                .getInstance("X.509");
        Certificate certificate = certificateFactory
                .generateCertificate(new ByteArrayInputStream(TestUtils
                        .getEncodedX509Certificate()));

        UnresolvedPermission unresolvedPermission = new UnresolvedPermission(
                "java.security.SecurityPermission", "a.b.c", "action",
                new Certificate[] { certificate });
        SerializationTest.verifySelf(unresolvedPermission);
        SerializationTest.verifyGolden(this, unresolvedPermission);
    }
}
