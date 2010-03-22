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

package org.apache.harmony.drlvm.tests.regression.h3225;

/**
 * The class launches methods which contain invalid <code>jsr</code> usage and
 * should be rejected by a verifier.
 */
public class NegativeJsrTest extends junit.framework.TestCase {
    public static void main(String args[]) {
        junit.textui.TestRunner.run(org.apache.harmony.drlvm.tests.regression.h3225.NegativeJsrTest.class);
    }

    private void checkVerifyError() {
        final String testMethodName = Thread.currentThread().getStackTrace()[3].getMethodName();
        assertEquals("test", testMethodName.substring(0, 4));

        final String testClassName = "org.apache.harmony.drlvm.tests.regression.h3225."
           + testMethodName.substring(4) + "NegativeTest";
        try {
            Class.forName(testClassName).getConstructors();
        } catch (VerifyError ve) {
            return;
        } catch (ClassNotFoundException cnfe) {
            fail("Failed to load " + testClassName);
        }
        fail(testClassName + " should throw java.lang.VerifyError");
    }

    public void testMergeExecution() {
        checkVerifyError();
    }

    public void testMergeEmptyStack() {
        checkVerifyError();
    }

    public void testMergeIntFloat() {
        checkVerifyError();
    }

    public void testMergeStack() {
        checkVerifyError();
    }

    public void testRetOrder() {
        checkVerifyError();
    }
}

