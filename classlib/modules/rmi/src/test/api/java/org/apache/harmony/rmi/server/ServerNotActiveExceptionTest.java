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
import java.rmi.server.ServerNotActiveException;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

public class ServerNotActiveExceptionTest extends junit.framework.TestCase {

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
        cause = new ServerNotActiveException(causeMessage);
    }

    /**
     * @tests java.rmi.server.ServerNotActiveException#ServerNotActiveException(String)
     */
    public void test_Constructor_String() {
        ServerNotActiveException e = new ServerNotActiveException(errorMessage);
        assertEquals(errorMessage, e.getMessage());
    }

    // comparator for ServerNotActiveException objects
    private static final SerializableAssert comparator = new SerializableAssert() {
        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {

            SerializationTest.THROWABLE_COMPARATOR.assertDeserialized(initial, deserialized);

            ServerNotActiveException initEx = (ServerNotActiveException) initial;
            ServerNotActiveException desrEx = (ServerNotActiveException) deserialized;

            assertEquals(initEx.getMessage(), desrEx.getMessage());
        }
    };

    /**
     * @tests serialization/deserialization.
     */
    public void testSerializationSelf() throws Exception {

        SerializationTest.verifySelf(new ServerNotActiveException(errorMessage), comparator);
    }

    /**
     * @tests serialization/deserialization compatibility with RI.
     */
    public void testSerializationCompatibility() throws Exception {

        SerializationTest.verifyGolden(this, new ServerNotActiveException(errorMessage), comparator);
    }

}
