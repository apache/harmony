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

package org.apache.harmony.print.tests.javax.print;

import javax.print.PrintException;
import junit.framework.TestCase;

public class PrintExceptionTest extends TestCase {
    /**
     * Test method for {@link javax.print.PrintException#PrintException()}.
     */
    public void testPrintException() {
        PrintException pe = new PrintException();
        assertNull(pe.getMessage());
        assertNull(pe.getCause());
    }

    /**
     * Test method for {@link javax.print.PrintException#PrintException(java.lang.String)}.
     */
    public void testPrintExceptionString() {
        PrintException pe = new PrintException("message");
        assertEquals("message", pe.getMessage());
        assertNull(pe.getCause());
    }

    /**
     * Test method for {@link javax.print.PrintException#PrintException(java.lang.Exception)}.
     */
    public void testPrintExceptionException() {
        NullPointerException npe = new NullPointerException("npe");
        PrintException pe = new PrintException(npe);
        assertNotNull(pe.getMessage());
        assertSame(npe, pe.getCause());
        
        pe = new PrintException((Exception)null);
        assertNull(pe.getMessage());
        assertNull(pe.getCause());
    }

    /**
     * Test method for {@link javax.print.PrintException#PrintException(java.lang.String, java.lang.Exception)}.
     */
    public void testPrintExceptionStringException() {
        NullPointerException npe = new NullPointerException("npe");
        PrintException pe = new PrintException("message", npe);
        assertEquals("message", pe.getMessage());
        assertSame(npe, pe.getCause());
    }
}
