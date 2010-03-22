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

package org.apache.harmony.rmi.server;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.SkeletonMismatchException;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

public class SkeletonMismatchExceptionTest extends junit.framework.TestCase {

    private String errorMessage = "SkeletonMismatch Exception!";
    private SkeletonMismatchException cause = new SkeletonMismatchException("cause");

    /**
     * @tests java.rmi.server.SkeletonMismatchException#SkeletonMismatchException(String)
     */
    public void test_Constructor_String() {
        SkeletonMismatchException e = new SkeletonMismatchException(errorMessage);
        assertTrue(e instanceof java.rmi.RemoteException);
        assertTrue(e instanceof RemoteException);
        assertEquals(errorMessage, e.getMessage());
        assertNull(e.detail);
    }

    // comparator for SkeletonMismatchException objects
    private static final SerializableAssert comparator = new SerializableAssert() {
        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {

            SerializationTest.THROWABLE_COMPARATOR.assertDeserialized(initial, deserialized);

            SkeletonMismatchException initEx = (SkeletonMismatchException) initial;
            SkeletonMismatchException desrEx = (SkeletonMismatchException) deserialized;
            assertEquals(initEx.getMessage(), desrEx.getMessage());
            assertEquals(initEx.detail, desrEx.detail);
        }
    };

    /**
     * @tests serialization/deserialization.
     */
    public void testSerializationSelf() throws Exception {

        SerializationTest.verifySelf(new SkeletonMismatchException(errorMessage), comparator);
    }

    /**
     * @tests serialization/deserialization compatibility with RI.
     */
    public void testSerializationCompatibility() throws Exception {

        SerializationTest.verifyGolden(this, new SkeletonMismatchException(errorMessage), comparator);
    }

}
