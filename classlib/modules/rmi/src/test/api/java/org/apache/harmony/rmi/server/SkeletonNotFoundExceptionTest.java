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
import java.rmi.server.SkeletonNotFoundException;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

public class SkeletonNotFoundExceptionTest extends junit.framework.TestCase {

    private String errorMessage;

    private String causeMessage;

    private Exception cause;

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    @Override
    protected void setUp() {
        errorMessage = "Skeleton Not Found";
        causeMessage = "Caused Exception";
        cause = new SkeletonNotFoundException(causeMessage);
    }

    /**
     * @tests java.rmi.server.SkeletonNotFoundException#SkeletonNotFoundException(String)
     */
    public void test_Constructor_String() {
        SkeletonNotFoundException e = new SkeletonNotFoundException(errorMessage);
        assertTrue(e instanceof java.rmi.RemoteException);
        assertEquals(errorMessage, e.getMessage());
    }

    /**
     * @tests java.rmi.server.SkeletonNotFoundException#SkeletonNotFoundException(String,Exception)
     */
    public void test_Constructor_String_Exception() {
        SkeletonNotFoundException e = new SkeletonNotFoundException(errorMessage, cause);
        assertEquals(cause.getMessage(), e.getCause().getMessage());
    }

    // comparator for SkeletonNotFoundException objects
    private static final SerializableAssert comparator = new SerializableAssert() {
        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {

            SerializationTest.THROWABLE_COMPARATOR.assertDeserialized(initial, deserialized);

            SkeletonNotFoundException initEx = (SkeletonNotFoundException) initial;
            SkeletonNotFoundException desrEx = (SkeletonNotFoundException) deserialized;

            assertEquals(initEx.getMessage(), desrEx.getMessage());
            assertEquals(initEx.getCause().getMessage(), desrEx.getCause().getMessage());
        }
    };

    /**
     * @tests serialization/deserialization.
     */
    public void testSerializationSelf() throws Exception {

        SerializationTest.verifySelf(new SkeletonNotFoundException(errorMessage, cause), comparator);
    }

    /**
     * @tests serialization/deserialization compatibility with RI.
     */
    public void testSerializationCompatibility() throws Exception {

        SerializationTest.verifyGolden(this, new SkeletonNotFoundException(errorMessage, cause), comparator);
    }

}
