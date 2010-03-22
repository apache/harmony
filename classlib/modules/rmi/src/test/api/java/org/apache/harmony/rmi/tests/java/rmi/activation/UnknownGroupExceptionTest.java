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

package org.apache.harmony.rmi.tests.java.rmi.activation;

import java.rmi.activation.UnknownGroupException;
import junit.framework.TestCase;

public class UnknownGroupExceptionTest extends TestCase {
    /**
     * Test method for {@link java.rmi.activation.UnknownGroupException#UnknownGroupException(java.lang.String)}.
     */
    public void testUnknownGroupException() {
        UnknownGroupException e = new UnknownGroupException("fixture");
        assertEquals("fixture", e.getMessage());
        assertNull(e.getCause());
        assertNull(e.detail);
        try {
            e.initCause(new NullPointerException());
            fail("did not throw illegal state exception");
        } catch (IllegalStateException ise) {
        }
    }
}
