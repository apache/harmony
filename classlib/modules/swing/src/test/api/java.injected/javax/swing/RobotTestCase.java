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
 * @author Anton Luht, Alexander T. Simbirtsev
 */
package javax.swing;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Robot;
import java.lang.reflect.Method;
import junit.framework.TestCase;

public abstract class RobotTestCase extends TestCase {
    /**
     * Framework for running JUnit tests that consist of sequence of assertions &
     * actions. The only visible method in it is static run(Object o, String[]
     * sequence) It accepts an object and a sequence of methods that should be
     * called on that object. Methods can be of two types: - public void <method
     * name>() - action - public boolean <method name>() - assertion
     *
     * Runner checks that all methods are actions or assertions (methods are
     * obtained using Class.getMethod() method) and calls
     * those methods of the specified objects one by one in cycle in AWT dispatch thread using invokeAndWait()
     * - if method is an
     * assertion, it is called in loop max ASSERTION_RETRIES times in ASSERTION_RETRY_TIMEOUT seconds interval
     * until it returns true or max number of attempts is reached.
     *
     * - if method is an action, it is just called as is
     *
     * If methods sequence passed successfully and in the proper time frame, the
     * returned value of run(..) is true in any other case (wrong methods sequence
     * or a timeout or an unhandled exception in methods loop execution) returned
     * value is false.
     *
     * Please note that the test can hang if one of assertions or actions hangs.
     *
     * The example of call is:
     *
     * RobotRunner.run(this, new String[] {"assertion0001", "action0001",
     * "assertion0002", "action0002"});
     */
    public static class RobotRunner {
        private static class RunnableMethod implements Runnable {
            private final Object target;

            private final Method method;

            private Object result;

            public Object getResult() {
                return result;
            }

            RunnableMethod(final Object target, final Method method) {
                this.target = target;
                this.method = method;
            }

            public void run() {
                try {
                    result = method.invoke(target);
                } catch (Throwable e) {
                    e.printStackTrace();
                    result = null;
                }
            }
        }

        private static int assertionRetryTimeout = 3000;

        private static int numAssertionRetries = 5;

        private static Object testCase;

        private RobotRunner() {
        }

        public static void setTestCase(final Object test) {
            testCase = test;
        }

        public static synchronized boolean run(final String[] sequence) {
            if (sequence == null || sequence.length == 0) {
                return false;
            }
            if (testCase == null) {
                return false;
            }
            try {
                for (int i = 0; i < sequence.length; i++) {
                    Method m = testCase.getClass().getMethod(sequence[i], new Class[] {});
                    if ((m.getReturnType() != boolean.class)
                            && (m.getReturnType() != void.class)) { // assertion
                        System.err.println("unknown return type: " + m.getReturnType()
                                + " for method '" + sequence[i] + "' in test sequence");
                        return false;
                    }
                }
                //ok then - all methods are correct, we can start
                for (int i = 0; i < sequence.length; i++) {
                    Method m = testCase.getClass().getMethod(sequence[i], new Class[] {});
                    RunnableMethod rm = new RunnableMethod(testCase, m);
                    if (m.getReturnType() == boolean.class) { // assertion
                        Boolean result = Boolean.FALSE;
                        for (int j = 0; j < numAssertionRetries; j++) {
                            if (j != 0) {
                                Thread.sleep(assertionRetryTimeout);
                            }
                            EventQueue.invokeAndWait(rm);
                            result = (Boolean) rm.getResult();
                            if (result == null || result.booleanValue()) {
                                break;
                            }
                        }
                        if (result == null || !result.booleanValue()) {
                            return false;
                        }
                    } else if (m.getReturnType() == void.class) { // assertion
                        EventQueue.invokeAndWait(rm);
                    }
                }
                return true;
            } catch (Throwable e) {
                e.printStackTrace();
                return false;
            }
        }

        public static synchronized boolean run(final Object o, final String[] sequence) {
            setTestCase(o);
            return run(sequence);
        }
    }

    protected static Robot robot;

    public RobotTestCase() {
        if (robot == null) {
            try {
                robot = new Robot();
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        RobotRunner.setTestCase(this);
    }

    public void runScenario(final String[] sequence) {
        assertTrue(RobotRunner.run(sequence));
    }

    public static void setAssertionRetryTimeout(final int timeout) {
        RobotRunner.assertionRetryTimeout = timeout;
    }

    public static void setNumberOfAssertionRetries(final int numRetries) {
        RobotRunner.numAssertionRetries = numRetries;
    }
}
