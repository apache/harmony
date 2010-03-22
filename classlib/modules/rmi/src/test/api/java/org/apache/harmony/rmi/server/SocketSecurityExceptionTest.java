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
import java.rmi.server.SocketSecurityException;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

public class SocketSecurityExceptionTest extends junit.framework.TestCase {

    private String errorMessage;

    private String causeMessage;

    private Exception cause;

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    @Override
    protected void setUp() {
        errorMessage = "Connectin Error";
        causeMessage = "Caused Exception";
        cause = new SocketSecurityException(causeMessage);
    }

    /**
     * @tests java.rmi.server.SocketSecurityException#SocketSecurityException(String)
     */
    public void test_Constructor_String() {
        SocketSecurityException e = new SocketSecurityException(errorMessage);
        assertTrue(e instanceof java.rmi.server.ExportException);
        assertEquals(errorMessage, e.getMessage());
    }

    /**
     * @tests java.rmi.server.SocketSecurityException#SocketSecurityException(String,Exception)
     */
    public void test_Constructor_String_Exception() {
        SocketSecurityException e = new SocketSecurityException(errorMessage, cause);
        assertEquals(cause.getMessage(), e.getCause().getMessage());
    }

    // comparator for SocketSecurityException objects
    private static final SerializableAssert comparator = new SerializableAssert() {
        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {

            SerializationTest.THROWABLE_COMPARATOR.assertDeserialized(initial, deserialized);

            SocketSecurityException initEx = (SocketSecurityException) initial;
            SocketSecurityException desrEx = (SocketSecurityException) deserialized;

            assertEquals(initEx.getMessage(), desrEx.getMessage());
            assertEquals(initEx.getCause().getMessage(), desrEx.getCause().getMessage());
        }
    };

    /**
     * @tests serialization/deserialization.
     */
    public void testSerializationSelf() throws Exception {

        SerializationTest.verifySelf(new SocketSecurityException(errorMessage, cause), comparator);
    }

    /**
     * @tests serialization/deserialization compatibility with RI.
     */
    public void testSerializationCompatibility() throws Exception {

        SerializationTest.verifyGolden(this, new SocketSecurityException(errorMessage, cause), comparator);
    }

}
