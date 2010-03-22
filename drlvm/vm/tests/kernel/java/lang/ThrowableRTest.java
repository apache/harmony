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
 * @author Elena Semukhina
 */

package java.lang;

import junit.framework.TestCase;

public class ThrowableRTest extends TestCase {

    private class initCauseExc extends RuntimeException {
        
        private static final long serialVersionUID = 0L;

        initCauseExc(Throwable cause){
            super(cause);
        }
        
        public Throwable initCause(Throwable cause) {
            return cause;
        }
    }

    /**
	 * Tests the Throwable(Throwable) constructor when the initCause() method
     * is overloaded
	 */
	public void testThrowableThrowableInitCause() {
        NullPointerException nPE = new NullPointerException();
        initCauseExc iC = new initCauseExc(nPE);
        assertTrue("Assert 0: The cause has not been set",
                   iC.getCause() != null); 
        assertTrue("Assert 1: The invalid cause has been set", 
                   iC.getCause() == nPE);        
    }

    /**
     * A regression test for HARMONY-1431 I-3 issue.
     * Tests that getStackTrace() contains info about sources
     * and does not contain empty parentheses
     */
    public void testStackTraceFileName() {
        try {
            Class<?> a = Class.forName("SomeClass");
            fail("ClassNotFoundException should be thrown");
        } catch (ClassNotFoundException e) {
            StackTraceElement ste[] = e.getStackTrace();
            for (int i = 0; i < ste.length; i++) {
                String element = ste[i].toString();
                if (element.indexOf("()") != -1) {
                    fail("Empty parentheses are published in stack trace: " + element);
                    break;
                }
            }
        }
    }

    /**
     * A regression test for HARMONY-1431 I-3 issue.
     * Tests that getStackTrace() contains info about the "main" method
     */
    public void testStackTraceMathodMain() {
        try {
            Class<?> a = Class.forName("SomeClass");
            fail("ClassNotFoundException should be thrown");
        } catch (ClassNotFoundException e) {
            StackTraceElement ste[] = e.getStackTrace();
            String mainFrame = ste[ste.length - 1].toString();
            if (mainFrame.indexOf("TestRunner.main") == -1) {
                fail("Method \"main\" is not published in stack trace");
            }
        }
    }
}