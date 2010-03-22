/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.rmi;

import java.rmi.AccessException;

import junit.framework.TestCase;

public class AccessExceptionTest extends TestCase {

    /**
     * {@link java.rmi.AccessException#AccessException(java.lang.String, java.lang.Exception)}.
     */
    public void testAccessExceptionStringException() {
        AccessException e = new AccessException("fixture", null);
        assertEquals("fixture", e.getMessage());
        assertNull(e.getCause());
        assertNull(e.detail);
        
        NullPointerException npe = new NullPointerException();
        e = new AccessException("fixture", npe);
        assertTrue(e.getMessage().indexOf("fixture") > -1);
        assertSame(npe, e.getCause());
        assertSame(npe, e.detail);
    }

    /**
     * {@link java.rmi.AccessException#AccessException(java.lang.String)}.
     */
    public void testAccessExceptionString() {
        AccessException e = new AccessException("fixture");
        assertEquals("fixture", e.getMessage());
        assertNull(e.getCause());
        assertNull(e.detail);
    }

}
