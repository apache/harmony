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
 * @author Dmitry A. Durnev
 */
package java.awt;

import junit.framework.TestCase;

/**
 * A wrapper for AWT package unit tests.
 * It wraps the real test code so that
 * it is executed in the event dispatch thread.
 * Copied from SwingTestCase.
 */
public abstract class AWTTestCase extends TestCase {

    /**
     * Exception thrown from Runnable (test code).
     */
    protected Throwable exception;

    public AWTTestCase() {
        super();
    }

    public AWTTestCase(String name) {
        super(name);
    }

    // Helper method to call <code>runBare</code> of super class.
    private void runAWTTest() throws Throwable {
        super.runBare();
    }

    /**
     * @see TestCase#runBare()
     */
    @Override
    public void runBare() throws Throwable {
        // Wrap the test-method to be run in the event dispatch thread.
        EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                try {
                    runAWTTest();
                } catch (Throwable e) {
                    exception = e;
                }
            }
        });
        rethrow(exception);
    }

    final void rethrow(final Throwable exception) throws Throwable {
        // Rethrow the exception if any
        if (exception != null) {
            throw exception;
        }
    }

}
