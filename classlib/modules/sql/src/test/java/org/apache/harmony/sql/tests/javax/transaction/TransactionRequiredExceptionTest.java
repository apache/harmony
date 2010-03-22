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

package org.apache.harmony.sql.tests.javax.transaction;

import javax.transaction.TransactionRequiredException;

import junit.framework.TestCase;

public class TransactionRequiredExceptionTest extends TestCase {

    /*
     * ConstructorTest
     */
    public void testTransactionRequiredExceptionString() {

        String[] init1 = { "a", "1", "valid1", "----", "&valid*", null, "", "\0" };

        String[] theFinalStates1 = init1;

        Exception[] theExceptions = { null, null, null, null, null, null, null,
                null };

        TransactionRequiredException aTransactionRequiredException;
        int loopCount = init1.length;
        for (int i = 0; i < loopCount; i++) {
            try {
                aTransactionRequiredException = new TransactionRequiredException(
                        init1[i]);
                if (theExceptions[i] != null) {
                    fail();
                }
                assertEquals(i + "  Final state mismatch",
                        aTransactionRequiredException.getMessage(),
                        theFinalStates1[i]);

            } catch (Exception e) {
                if (theExceptions[i] == null) {
                    fail(i + "Unexpected exception");
                }
                assertEquals(i + "Exception mismatch", e.getClass(),
                        theExceptions[i].getClass());
                assertEquals(i + "Exception mismatch", e.getMessage(),
                        theExceptions[i].getMessage());
            } // end try
        } // end for

    } // end method testTransactionRequiredExceptionString

    /*
     * ConstructorTest
     */
    public void testTransactionRequiredException() {

        String[] theFinalStates1 = { null };

        Exception[] theExceptions = { null };

        TransactionRequiredException aTransactionRequiredException;
        int loopCount = 1;
        for (int i = 0; i < loopCount; i++) {
            try {
                aTransactionRequiredException = new TransactionRequiredException();
                if (theExceptions[i] != null) {
                    fail();
                }
                assertEquals(i + "  Final state mismatch",
                        aTransactionRequiredException.getMessage(),
                        theFinalStates1[i]);

            } catch (Exception e) {
                if (theExceptions[i] == null) {
                    fail(i + "Unexpected exception");
                }
                assertEquals(i + "Exception mismatch", e.getClass(),
                        theExceptions[i].getClass());
                assertEquals(i + "Exception mismatch", e.getMessage(),
                        theExceptions[i].getMessage());
            } // end try
        } // end for

    } // end method testTransactionRequiredException

} // end class TransactionRequiredExceptionTest
